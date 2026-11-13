# Sistema de software con inteligencia artificial para tamizaje no invasivo de anemia infantil

Semillero de Investigación AXIS EPIS — Ingeniería de Sistemas, Universidad Nacional del Altiplano (Puno).

Prototipo Android **offline** que apoya al personal y promotores de salud en el tamizaje de anemia
en niños de 6 a 59 meses a partir de fotos de la **conjuntiva palpebral**, la **palma de la mano**
y el **lecho ungueal**, e interpreta la hemoglobina con **corrección por altitud**.

> Herramienta de apoyo. No reemplaza el diagnóstico clínico ni la medición oficial de hemoglobina.

## Estado: demo 0.3

- Registro del niño con código anónimo (sin nombres ni DNI), edad (con equivalencia en años),
  sexo, altitud (atajos Puno/Juliaca) y Hb opcional con **vista previa de la Hb corregida**.
- Captura guiada de las 3 fotos con la cámara (CameraX): indicador de pasos, recuadro guía con
  el resto de la imagen oscurecido y linterna. Las fotos quedan solo en el almacenamiento privado de la app.
- **Tres modelos provisionales** (TensorFlow Lite, MobileNetV3Small) entrenados con datasets
  públicos de niños de Ghana, evaluados con participantes no vistos: conjuntiva AUC 0,88,
  palma 0,80, uña 0,68. Se combinan con un promedio ponderado por calidad. Detalles y
  limitaciones en [ml/README.md](ml/README.md). Sin modelos en `assets/modelo/`, la app usa un
  clasificador simulado marcado como DEMO.
- Resultado con **semáforo de riesgo** (bajo < 30 % ≤ intermedio < 50 % ≤ alto, cortes provisionales),
  detalle por zona con miniaturas y explicación de la Hb corregida.
- Hb observada → Hb corregida por altitud → grado de anemia (pendiente de validar con la
  NTS N° 213-MINSA/DGIESP-2024).
- Inicio con resumen (tamizajes, riesgo alto, con Hb), historial local con Room (SQLite) y botón
  **Exportar** (ZIP con `tamizajes.csv` y fotos) para armar el dataset propio. La app no pide
  permiso de Internet.

## Cómo abrirlo

1. Instalar Android Studio y abrir esta carpeta (`File > Open`). Si avisa que Gradle 8.13 no
   es compatible con su Java, elegir **Use JVM 21**.
2. Esperar la sincronización de Gradle y ejecutar en un emulador o celular con depuración USB.

Desde la terminal (con JDK 17 o 21, por ejemplo el que descarga Android Studio en `~/.jdks`):

```bash
./gradlew assembleDebug testDebugUnitTest
```

## Estructura

```
app/src/main/java/pe/edu/unap/tamizajeanemia/
├── clinico/   Corrección de Hb por altitud y puntos de corte por edad
├── datos/     Entidad Tamizaje, Room y repositorio (fotos en almacenamiento privado)
├── ia/        Interfaz ClasificadorAnemia, implementación TFLite y simulada
├── camara/    Vista previa y captura con CameraX
└── ui/        Pantallas Compose: inicio, datos, captura, resultado, tema y componentes
app/src/main/assets/modelo/   <modalidad>.tflite + <modalidad>.json (versión, recorte, AUC)
ml/                           Descarga de datos y entrenamiento en Python
```

## Integrantes

Flores Flores Emerson Aldair · Llanos Ticona Blanca Rosario · Monroy Quispe Maricarmen ·
Ramos Vilca Francy Jimena · Suaña Hancco Guido · Vargas Urpi Albino
