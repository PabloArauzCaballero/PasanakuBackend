#!/usr/bin/env python3
"""Presta la camara del Mac al simulador de iOS.

**El problema.** El simulador de iOS no tiene camara y no puede usar la del Mac: no
expone ningun `AVCaptureDevice`, asi que `image_picker` con `ImageSource.camera`
devuelve nada. Es una limitacion de Apple, no del codigo de la app. Sin camara no se
puede recorrer el alta —documento y prueba de vida— sin un telefono fisico.

**La solucion.** Un servicio chiquito en la maquina que saca una foto con la camara
del Mac y la devuelve por HTTP. La app, SOLO en desarrollo, pide la foto aca en vez
de abrir una camara que no existe. La foto es real, sale de la camara de verdad, y el
resto del flujo —subida, MinIO, expediente, backoffice— es exactamente el mismo.

    python3 scripts/camara_del_mac.py            # queda escuchando en 8899
    curl http://localhost:8899/foto -o prueba.jpg

Y la app se compila apuntando aca:

    flutter run --dart-define=CAMARA_DEV=http://localhost:8899/foto

**Permiso de camara.** La primera vez macOS pregunta si la terminal puede usar la
camara. Hay que aceptar una vez; si no, la captura se queda esperando para siempre.
Para provocar el dialogo a mano:

    ffmpeg -f avfoundation -framerate 30 -i "0" -frames:v 1 -y /tmp/prueba.jpg

Esto es una herramienta de desarrollo y nada mas: no se despliega, no la usa la app
de produccion, y el adaptador que la consume solo existe cuando se pasa CAMARA_DEV.
"""

from __future__ import annotations

import argparse
import http.server
import subprocess
import sys
import tempfile
from pathlib import Path

# El indice de la camara en la lista de avfoundation. `0` suele ser la integrada;
# `ffmpeg -f avfoundation -list_devices true -i ""` las muestra todas.
CAMARA_POR_OMISION = "0"

# Cuanto se espera a la captura. Si macOS todavia no dio el permiso, ffmpeg se queda
# esperando el dialogo: es mejor cortar y decirlo que colgarse.
ESPERA_SEGUNDOS = 20


def capturar(dispositivo: str, ancho: int, alto: int) -> bytes:
    """Un cuadro de la camara, en JPEG."""
    with tempfile.TemporaryDirectory() as carpeta:
        salida = Path(carpeta) / "foto.jpg"
        orden = [
            "ffmpeg",
            "-hide_banner",
            "-v", "error",
            "-f", "avfoundation",
            "-framerate", "30",
            "-video_size", f"{ancho}x{alto}",
            "-i", dispositivo,
            # Los primeros cuadros salen oscuros: la camara todavia esta midiendo la
            # luz. Se descartan y se guarda el siguiente.
            "-frames:v", "1",
            "-ss", "0.8",
            "-y", str(salida),
        ]
        try:
            subprocess.run(orden, check=True, capture_output=True, timeout=ESPERA_SEGUNDOS)
        except subprocess.TimeoutExpired as e:
            raise RuntimeError(
                "La captura se quedo esperando. Casi siempre es el permiso de camara "
                "de macOS sin conceder: corre una vez\n"
                '  ffmpeg -f avfoundation -framerate 30 -i "0" -frames:v 1 -y /tmp/p.jpg\n'
                "y acepta el dialogo."
            ) from e
        except subprocess.CalledProcessError as e:
            raise RuntimeError(e.stderr.decode(errors="replace").strip()) from e
        return salida.read_bytes()


def servidor(dispositivo: str, ancho: int, alto: int) -> type[http.server.BaseHTTPRequestHandler]:
    class Manejador(http.server.BaseHTTPRequestHandler):
        def do_GET(self) -> None:  # noqa: N802 — lo define la biblioteca
            if self.path.split("?")[0] != "/foto":
                self.send_error(404, "Solo /foto")
                return
            try:
                imagen = capturar(dispositivo, ancho, alto)
            except RuntimeError as fallo:
                self.send_error(503, str(fallo))
                return
            self.send_response(200)
            self.send_header("Content-Type", "image/jpeg")
            self.send_header("Content-Length", str(len(imagen)))
            # El simulador es otro origen; sin esto el navegador de Flutter Web lo
            # bloquearia, y no cuesta nada dejarlo abierto en una herramienta local.
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(imagen)

        def log_message(self, formato: str, *args: object) -> None:
            print(f"camara: {formato % args}", file=sys.stderr)

    return Manejador


def main() -> int:
    opciones = argparse.ArgumentParser(description=__doc__)
    opciones.add_argument("--puerto", type=int, default=8899)
    opciones.add_argument("--dispositivo", default=CAMARA_POR_OMISION)
    opciones.add_argument("--ancho", type=int, default=1280)
    opciones.add_argument("--alto", type=int, default=720)
    args = opciones.parse_args()

    manejador = servidor(args.dispositivo, args.ancho, args.alto)
    with http.server.ThreadingHTTPServer(("127.0.0.1", args.puerto), manejador) as srv:
        print(
            f"Camara del Mac en http://localhost:{args.puerto}/foto "
            f"(dispositivo {args.dispositivo}). Ctrl-C para cortar."
        )
        try:
            srv.serve_forever()
        except KeyboardInterrupt:
            print("\nlisto")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
