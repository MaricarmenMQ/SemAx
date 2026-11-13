# Modelos de IA

Scripts para entrenar los modelos y exportarlos a TensorFlow Lite para la app.

## Modelos provisionales (conjuntiva, palma y uña)

Sirven para que la demo use modelos reales mientras se recolecta el dataset propio del altiplano.
**No están validados para uso clínico ni para niños de Puno.**

Datos: tres datasets públicos del mismo grupo de Ghana, niños menores de 6 años, licencia CC BY 4.0:

| Modalidad | Referencia |
|---|---|
| Conjuntiva | Appiahene P., Asare J. W., Donkoh E. (2022). Mendeley Data. [doi:10.17632/nt7r8hv2pz.1](https://doi.org/10.17632/nt7r8hv2pz.1) |
| Palma | Asare J. W. et al. (2022). Mendeley Data. [doi:10.17632/ccr8cm22vz.1](https://doi.org/10.17632/ccr8cm22vz.1) |
| Uña | Asare J. W., Appiahene P., Donkoh E. (2022). Mendeley Data. [doi:10.17632/2xx4j3kjg2.1](https://doi.org/10.17632/2xx4j3kjg2.1) |

Las imágenes vienen **ya recortadas** (conjuntiva y uña segmentadas sobre fondo transparente;
palma como recorte de piel). La versión pública de CP-AnemiC (doi:10.17632/m53vz6b7fx) fue
retirada por sus autores. No se encontraron datasets públicos de Perú ni de la sierra: el de la
UNSM (adultos jóvenes, Tarapoto) aún no habilita la descarga y el de la UPCH (niños) no es público.

```bash
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements.txt
.venv/Scripts/python descargar_datos.py   # ~330 MB en ml/datos/ (no se sube a git); usa `tar` para los .rar
.venv/Scripts/python entrenar.py          # o: entrenar.py palma   (una sola modalidad)
```

`entrenar.py` deja `<modalidad>.tflite` y `<modalidad>.json` en `app/src/main/assets/modelo/`
y las métricas en `salidas/<modalidad>/metricas.json`.

Decisiones:

- **Partición por participante.** Los datasets traen copias aumentadas de cada foto (y algunas
  idénticas con nombres distintos o con erratas); se agrupan para que ningún niño quede a la
  vez en entrenamiento y prueba. Separar por imagen infla las métricas (ver abajo).
- **Sin aumentos de color o brillo**, porque la palidez es justamente la señal.
- **Ajuste fino de toda la red.** Con solo las últimas capas la conjuntiva daba AUC 0,58.
- **Recorte central en la app.** Como los modelos aprendieron con recortes, la app muestra un
  recuadro guía por modalidad y recorta esa zona antes de clasificar (`recorte_central` en el
  .json). Aun así la foto incluye piel y fondo que los modelos no vieron: hay que esperar un
  rendimiento menor que el de prueba.
- **Fusión ponderada.** La app combina las tres probabilidades con peso AUC − 0,5
  (conjuntiva ≈ 44 %, palma ≈ 35 %, uña ≈ 21 %). No se pudo validar la fusión: los tres
  datasets no permiten emparejar fotos del mismo niño.

### Resultados (29/10/2026, prueba con participantes no vistos, umbral 0,5)

| Modelo (MobileNetV3Small) | Imágenes de prueba | AUC-ROC | Sensibilidad | Especificidad | F1 |
|---|---|---|---|---|---|
| Conjuntiva | 289 | **0,88** | 0,86 | 0,76 | 0,84 |
| Palma | 296 | **0,80** | 0,60 | 0,79 | 0,68 |
| Uña | 299 | **0,68** | 0,67 | 0,55 | 0,67 |

Comparaciones en conjuntiva:

| Variante | AUC-ROC |
|---|---|
| MobileNetV3Small, solo últimas capas | 0,58 |
| Línea base: estadísticas de color + gradient boosting | 0,74 |
| Misma línea base partiendo **por imagen** (con fuga) | 0,98 |

La última fila muestra cuánto se inflan las métricas si se parte por imagen: es un punto a
discutir al comparar con trabajos publicados. Los `.tflite` exportados se verificaron con el
mismo preprocesamiento de la app (conjuntiva 0,879; palma 0,796; uña 0,666).

## Pendiente para el estudio

- Entrenar y validar con el dataset propio del altiplano (botón «Exportar» de la app).
- Modelo multimodal entrenado con fotos emparejadas del mismo niño.
- Comparar etiquetas con Hb observada vs. Hb corregida por altitud.
- Segmentación automática de la zona en el celular (en lugar del recorte fijo).
- Fotos de uña con más resolución: las del dataset de Ghana miden ~70 px.
