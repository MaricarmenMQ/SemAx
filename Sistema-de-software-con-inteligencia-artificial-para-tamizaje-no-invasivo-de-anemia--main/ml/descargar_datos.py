"""Descarga los datasets públicos de niños de Ghana (Mendeley Data, CC BY 4.0).

- Conjuntiva: Appiahene P., Asare J. W., Donkoh E. (2022). "Application of Machine Learning in
  Detecting Iron Deficiency Anemia Using Conjunctiva image Dataset from Ghana". doi:10.17632/nt7r8hv2pz.1
- Palma: Asare J. W. et al. (2022). "Anemia Detection using Palpable Palm Image Datasets from
  Ghana". doi:10.17632/ccr8cm22vz.1
- Uña: Asare J. W., Appiahene P., Donkoh E. (2022). "Detection of Anemia using Colour of the
  Fingernails Image Datasets from Ghana". doi:10.17632/2xx4j3kjg2.1

Uso:  python descargar_datos.py [conjuntiva] [palma] [una]   (sin argumentos: todos)
Deja las imágenes en ml/datos/ghana_<modalidad>/ (carpeta ignorada por git). Los .rar se
extraen con `tar` (bsdtar, incluido en Windows 10+).
"""

import hashlib
import json
import subprocess
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

DATASETS = {"conjuntiva": "nt7r8hv2pz", "palma": "ccr8cm22vz", "una": "2xx4j3kjg2"}
DATOS = Path(__file__).parent / "datos"
LOTE = 1000


def leer_json(url: str):
    peticion = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(peticion, timeout=60) as respuesta:
        return json.load(respuesta)


def listar_archivos(dataset: str) -> list[dict]:
    api = f"https://data.mendeley.com/public-api/datasets/{dataset}/files?folder_id=root&version=1"
    archivos, inicio = [], 0
    while True:
        lote = leer_json(f"{api}&$start={inicio}&$limit={LOTE}")
        archivos += lote
        if len(lote) < LOTE:
            return archivos
        inicio += LOTE


def sha256(ruta: Path) -> str:
    h = hashlib.sha256()
    with ruta.open("rb") as f:
        for bloque in iter(lambda: f.read(1 << 20), b""):
            h.update(bloque)
    return h.hexdigest()


def descargar(archivo: dict, destino: Path) -> Path:
    detalle = archivo["content_details"]
    ruta = destino / archivo["filename"]
    if ruta.exists() and sha256(ruta) == detalle["sha256_hash"]:
        return ruta
    peticion = urllib.request.Request(detalle["download_url"], headers={"User-Agent": "Mozilla/5.0"})
    for _ in range(4):
        try:
            with urllib.request.urlopen(peticion, timeout=300) as respuesta:
                contenido = respuesta.read()
        except OSError:
            continue
        if hashlib.sha256(contenido).hexdigest() == detalle["sha256_hash"]:
            ruta.write_bytes(contenido)
            return ruta
    raise ValueError(f"No se pudo descargar bien {ruta.name}")


def descargar_dataset(nombre: str) -> None:
    destino = DATOS / f"ghana_{nombre}"
    destino.mkdir(parents=True, exist_ok=True)
    archivos = listar_archivos(DATASETS[nombre])
    print(f"{nombre}: {len(archivos)} archivos")
    with ThreadPoolExecutor(max_workers=16) as hilos:
        rutas = list(hilos.map(lambda a: descargar(a, destino), archivos))
    for ruta in rutas:
        if ruta.suffix.lower() == ".rar":
            # Se extrae en una subcarpeta propia; el .rar queda para no volver a descargarlo.
            carpeta = destino / ruta.stem
            if not carpeta.exists():
                carpeta.mkdir()
                subprocess.run(["tar", "-xf", str(ruta), "-C", str(carpeta)], check=True)
    print(f"Listo: {destino}")


def main() -> None:
    for nombre in sys.argv[1:] or DATASETS:
        descargar_dataset(nombre)


if __name__ == "__main__":
    main()
