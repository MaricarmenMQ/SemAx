"""Entrena los modelos PROVISIONALES por modalidad y los exporta a TFLite para la app.

- Datos: ml/datos/ghana_<modalidad>/ (ver descargar_datos.py). Niños < 6 años, Ghana.
- Partición por PARTICIPANTE (no por imagen): los datasets traen copias aumentadas de cada
  foto y separarlas por imagen filtraría información entre entrenamiento y prueba.
- Sin aumentos de color/brillo: la palidez es la señal a aprender.
- Modelo: MobileNetV3Small (ImageNet) + cabeza sigmoide. Entrada 224x224x3 RGB en 0–1,
  salida [1, 1] = probabilidad de anemia (el formato que espera ClasificadorTflite).

Uso:  .venv/Scripts/python entrenar.py [conjuntiva] [palma] [una]   (sin argumentos: todas)
Salidas: ml/salidas/<modalidad>/ (métricas y modelo) y
         app/src/main/assets/modelo/<modalidad>.tflite + <modalidad>.json
"""

import hashlib
import json
import re
import shutil
import sys
from datetime import date
from pathlib import Path

import numpy as np
import tensorflow as tf
from sklearn.metrics import confusion_matrix, f1_score, roc_auc_score, roc_curve
from sklearn.model_selection import StratifiedGroupKFold

SEMILLA = 42
TAM = 224
RAIZ = Path(__file__).parent
SALIDAS = RAIZ / "salidas"
ASSETS = RAIZ.parent / "app" / "src" / "main" / "assets" / "modelo"

# recorte: zona central (ancho, alto como fracción de la foto) que la app recorta antes de
# clasificar; corresponde al recuadro guía de la cámara (Modalidad.guia en la app).
MODALIDADES = {
    "conjuntiva": {
        "datos": "ghana_conjuntiva",
        "recorte": [0.55, 0.22],
        "cita": "Appiahene, Asare & Donkoh (2022), Mendeley Data, doi:10.17632/nt7r8hv2pz.1 (CC BY 4.0)",
    },
    "palma": {
        "datos": "ghana_palma",
        "recorte": [0.45, 0.30],
        "cita": "Asare et al. (2022), Mendeley Data, doi:10.17632/ccr8cm22vz.1 (CC BY 4.0)",
    },
    "una": {
        "datos": "ghana_una",
        "recorte": [0.25, 0.14],
        "cita": "Asare, Appiahene & Donkoh (2022), Mendeley Data, doi:10.17632/2xx4j3kjg2.1 (CC BY 4.0)",
    },
}


def nombre_base(ruta: Path) -> str:
    """'Anemic-015 (6).png', 'Anemic-196 - Copy.png', 'Non-anemic-Fin-002 4.png' → 'anemic-015', ..."""
    base = re.sub(r"( \(\d+\)| - Copy| \d+)+$", "", ruta.stem)
    return re.sub(r"png$", "", base).lower()


def cargar_indice(carpeta: Path):
    """Lista (ruta, etiqueta, participante) sin duplicados exactos.

    Los nombres de los datasets son irregulares (y con erratas como 'non-anrmic') y algunas
    imágenes idénticas aparecen con nombres distintos; se unen en un mismo grupo para que
    nunca queden a ambos lados de la partición.
    """
    padre: dict[str, str] = {}

    def raiz(x):
        while padre.setdefault(x, x) != x:
            x = padre[x]
        return x

    por_huella: dict[str, tuple[Path, str]] = {}
    for ruta in sorted(carpeta.rglob("*.png")):
        base = nombre_base(ruta)
        huella = hashlib.sha256(ruta.read_bytes()).hexdigest()
        if huella in por_huella:
            padre[raiz(base)] = raiz(por_huella[huella][1])
        else:
            por_huella[huella] = (ruta, base)

    rutas, etiquetas, grupos = [], [], []
    for ruta, base in por_huella.values():
        rutas.append(str(ruta))
        etiquetas.append(0 if base.startswith("non") else 1)
        grupos.append(raiz(base))
    return np.array(rutas), np.array(etiquetas), np.array(grupos)


def particionar(etiquetas, grupos):
    """~70 % entrenamiento, ~15 % validación, ~15 % prueba, estratificado y por participante."""
    pliegues = list(StratifiedGroupKFold(n_splits=7, shuffle=True, random_state=SEMILLA)
                    .split(np.zeros(len(etiquetas)), etiquetas, grupos))
    prueba = pliegues[0][1]
    validacion = pliegues[1][1]
    resto = np.setdiff1d(np.arange(len(etiquetas)), np.concatenate([prueba, validacion]))
    return resto, validacion, prueba


def leer_imagen(ruta, etiqueta):
    # Imágenes RGBA ya recortadas; donde hay fondo transparente queda negro.
    rgba = tf.cast(tf.io.decode_png(tf.io.read_file(ruta), channels=4), tf.float32)
    imagen = rgba[..., :3] * (rgba[..., 3:] / 255.0)
    # Igual que la app: escala directa (sin recorte) y píxeles en 0–1.
    imagen = tf.image.resize(imagen, (TAM, TAM)) / 255.0
    return imagen, tf.cast(etiqueta, tf.float32)


def lote(rutas, etiquetas, entrenamiento):
    ds = tf.data.Dataset.from_tensor_slices((rutas, etiquetas))
    if entrenamiento:
        ds = ds.shuffle(len(rutas), seed=SEMILLA)
    ds = ds.map(leer_imagen, num_parallel_calls=tf.data.AUTOTUNE)
    return ds.batch(32).prefetch(tf.data.AUTOTUNE)


def construir_modelo():
    entrada = tf.keras.Input((TAM, TAM, 3), name="imagen")
    x = tf.keras.Sequential([
        tf.keras.layers.RandomFlip("horizontal"),
        tf.keras.layers.RandomRotation(0.05),
        tf.keras.layers.RandomZoom(0.1),
    ], name="aumento")(entrada)
    x = tf.keras.layers.Rescaling(255.0)(x)  # MobileNetV3 normaliza internamente desde 0–255
    base = tf.keras.applications.MobileNetV3Small(
        input_shape=(TAM, TAM, 3), include_top=False, weights="imagenet", pooling="avg",
    )
    base.trainable = False
    x = base(x, training=False)
    x = tf.keras.layers.Dropout(0.3)(x)
    salida = tf.keras.layers.Dense(1, activation="sigmoid", name="prob_anemia")(x)
    return tf.keras.Model(entrada, salida), base


def metricas(y, prob, umbral):
    pred = (prob >= umbral).astype(int)
    tn, fp, fn, tp = confusion_matrix(y, pred, labels=[0, 1]).ravel()
    return {
        "umbral": round(float(umbral), 3),
        "auc_roc": round(float(roc_auc_score(y, prob)), 3),
        "sensibilidad": round(tp / (tp + fn), 3),
        "especificidad": round(tn / (tn + fp), 3),
        "f1": round(float(f1_score(y, pred)), 3),
        "exactitud": round((tp + tn) / len(y), 3),
        "matriz_confusion": {"vp": int(tp), "fn": int(fn), "vn": int(tn), "fp": int(fp)},
    }


def entrenar(nombre: str) -> dict:
    config = MODALIDADES[nombre]
    tf.keras.utils.set_random_seed(SEMILLA)
    rutas, etiquetas, grupos = cargar_indice(RAIZ / "datos" / config["datos"])
    ent, val, pru = particionar(etiquetas, grupos)
    for parte, idx in [("entrenamiento", ent), ("validacion", val), ("prueba", pru)]:
        print(f"[{nombre}] {parte}: {len(idx)} imágenes, {len(set(grupos[idx]))} participantes, "
              f"{etiquetas[idx].mean():.0%} anemia")
    assert not set(grupos[ent]) & set(grupos[pru]), "Participantes repetidos entre particiones"

    ds_ent = lote(rutas[ent], etiquetas[ent], True)
    ds_val = lote(rutas[val], etiquetas[val], False)
    ds_pru = lote(rutas[pru], etiquetas[pru], False)

    pesos = {c: len(ent) / (2 * np.sum(etiquetas[ent] == c)) for c in (0, 1)}
    modelo, base = construir_modelo()

    # Fase 1: solo la cabeza.
    modelo.compile(tf.keras.optimizers.Adam(1e-3), "binary_crossentropy",
                   metrics=[tf.keras.metrics.AUC(name="auc")])
    modelo.fit(ds_ent, validation_data=ds_val, epochs=25, class_weight=pesos, callbacks=[
        tf.keras.callbacks.EarlyStopping(monitor="val_auc", mode="max", patience=6, restore_best_weights=True),
    ])

    # Fase 2: ajuste fino de toda la base (BatchNorm congelado). Con solo las últimas capas
    # la red no capta bien la señal de color (conjuntiva: AUC prueba ~0.58).
    base.trainable = True
    for capa in base.layers:
        if isinstance(capa, tf.keras.layers.BatchNormalization):
            capa.trainable = False
    modelo.compile(tf.keras.optimizers.Adam(1e-4), "binary_crossentropy",
                   metrics=[tf.keras.metrics.AUC(name="auc")])
    modelo.fit(ds_ent, validation_data=ds_val, epochs=40, class_weight=pesos, callbacks=[
        tf.keras.callbacks.EarlyStopping(monitor="val_auc", mode="max", patience=8, restore_best_weights=True),
    ])

    prob_val = modelo.predict(ds_val, verbose=0).ravel()
    fpr, tpr, umbrales = roc_curve(etiquetas[val], prob_val)
    umbral_youden = float(np.clip(umbrales[np.argmax(tpr - fpr)], 0, 1))
    prob_pru = modelo.predict(ds_pru, verbose=0).ravel()

    salida = SALIDAS / nombre
    salida.mkdir(parents=True, exist_ok=True)
    reporte = {
        "modalidad": nombre,
        "fecha": date.today().isoformat(),
        "modelo": "MobileNetV3Small + sigmoide, entrada 224x224 RGB 0-1",
        "dataset": config["cita"],
        "imagenes_unicas": int(len(rutas)),
        "participantes": int(len(set(grupos))),
        "particion": {"entrenamiento": int(len(ent)), "validacion": int(len(val)), "prueba": int(len(pru))},
        "prueba_umbral_0_5": metricas(etiquetas[pru], prob_pru, 0.5),
        "prueba_umbral_youden_val": metricas(etiquetas[pru], prob_pru, umbral_youden),
    }
    (salida / "metricas.json").write_text(json.dumps(reporte, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(reporte, indent=2, ensure_ascii=False))

    tflite = tf.lite.TFLiteConverter.from_keras_model(modelo).convert()
    (salida / f"{nombre}.tflite").write_bytes(tflite)
    ASSETS.mkdir(parents=True, exist_ok=True)
    shutil.copy(salida / f"{nombre}.tflite", ASSETS / f"{nombre}.tflite")
    (ASSETS / f"{nombre}.json").write_text(json.dumps({
        "modalidad": nombre.upper(),
        "recorte_central": config["recorte"],
        "version": f"{nombre}-ghana-{date.today().isoformat()}",
        "auc_prueba": reporte["prueba_umbral_0_5"]["auc_roc"],
    }, indent=2), encoding="utf-8")
    print(f"Modelo exportado: {ASSETS / f'{nombre}.tflite'} ({len(tflite) / 1e6:.1f} MB)")
    return reporte


def main():
    for nombre in sys.argv[1:] or MODALIDADES:
        entrenar(nombre)


if __name__ == "__main__":
    main()
