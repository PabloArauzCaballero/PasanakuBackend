"""Servidor HTTP local de proveedor ficticio; interfaz interna, no API de un banco."""

from __future__ import annotations

import argparse
import hmac
import json
import os
import sqlite3
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import urlsplit

from .modelo import ErrorProveedor, Proveedor, serializar


LIMITE_CUERPO = 16_384
LIMITE_DRENAJE = 1_048_576
SEGUNDOS_POR_CONEXION = 10


def objeto_unico(pares: list[tuple[str, Any]]) -> dict[str, Any]:
    salida: dict[str, Any] = {}
    for clave, valor in pares:
        if clave in salida:
            raise ErrorProveedor("CLAVE_JSON_DUPLICADA")
        salida[clave] = valor
    return salida


def rechazar_constante(_: str) -> None:
    raise ErrorProveedor("JSON_NO_FINITO")


def crear_servidor(
    host: str,
    puerto: int,
    proveedor: Proveedor,
    api_key: str,
    control_key: str,
    segundos_por_conexion: int = SEGUNDOS_POR_CONEXION,
) -> ThreadingHTTPServer:
    if host not in {"127.0.0.1", "localhost"} or min(len(api_key), len(control_key)) < 32 or api_key == control_key:
        raise ErrorProveedor("CONFIGURACION_LOCAL_INVALIDA")

    class Handler(BaseHTTPRequestHandler):
        server_version = "ProveedorFicticio/1"
        # Una conexion que no termina de enviar su cuerpo no retiene al hilo para siempre.
        timeout = segundos_por_conexion

        def log_message(self, formato: str, *args: Any) -> None:
            # Evita URLs, credenciales y payloads en logs del servidor de pruebas.
            return

        def responder(self, status: int, cuerpo: Any) -> None:
            raw = serializar(cuerpo)
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Pasanaku-Simulado", "true")
            self.send_header("Content-Length", str(len(raw)))
            self.end_headers()
            self.wfile.write(raw)

        def cuerpo(self) -> dict[str, Any]:
            if self.headers.get_content_type() != "application/json":
                raise ErrorProveedor("CONTENT_TYPE_INVALIDO", 415)
            try:
                tamano = int(self.headers.get("Content-Length", ""))
            except ValueError as exc:
                raise ErrorProveedor("CONTENT_LENGTH_INVALIDO") from exc
            if tamano <= 0:
                raise ErrorProveedor("CUERPO_INVALIDO")
            if tamano > LIMITE_CUERPO:
                self.close_connection = True
                raise ErrorProveedor("CUERPO_DEMASIADO_GRANDE", 413)
            self.cuerpo_consumido = True
            crudo = self.rfile.read(tamano)
            if len(crudo) != tamano:
                raise ErrorProveedor("CUERPO_INCOMPLETO")
            try:
                valor = json.loads(crudo, object_pairs_hook=objeto_unico, parse_constant=rechazar_constante)
            except ErrorProveedor:
                raise
            except (json.JSONDecodeError, UnicodeDecodeError, ValueError, RecursionError) as exc:
                raise ErrorProveedor("JSON_INVALIDO") from exc
            if not isinstance(valor, dict):
                raise ErrorProveedor("OBJETO_JSON_REQUERIDO")
            return valor

        def drenar(self) -> None:
            """Descarta el cuerpo declarado que no se leyo antes de contestar un error.

            Cerrar con bytes sin leer hace que el sistema operativo resetee la conexion y el cliente
            pierda la respuesta (se vio en Windows: WinError 10053, una de cada varias corridas). Se
            descarta en trozos, sin acumular; lo que supera el limite de drenaje no se lee, y el
            tiempo maximo de la conexion sigue rigiendo.
            """
            if getattr(self, "cuerpo_consumido", False):
                return
            self.cuerpo_consumido = True
            try:
                pendiente = int(self.headers.get("Content-Length", "0"))
            except ValueError:
                return
            if not 0 < pendiente <= LIMITE_DRENAJE:
                return
            try:
                while pendiente > 0:
                    leido = self.rfile.read(min(pendiente, 65_536))
                    if not leido:
                        break
                    pendiente -= len(leido)
            except OSError:
                self.close_connection = True

        def fallar(self, exc: Exception) -> None:
            """Traduce cualquier excepcion a una respuesta controlada, sin contenido de la peticion."""
            self.drenar()
            if isinstance(exc, ErrorProveedor):
                estado, codigo = exc.status, exc.codigo
            elif isinstance(exc, sqlite3.Error):
                estado, codigo = 503, "PROVEEDOR_NO_DISPONIBLE"
            else:
                estado, codigo = 500, "ERROR_INTERNO_CONTROLADO"
            try:
                self.responder(estado, {"codigo": codigo})
            except OSError:
                self.close_connection = True

        def autorizar(self, control: bool = False) -> None:
            esperada = "Bearer " + (control_key if control else api_key)
            if not hmac.compare_digest(self.headers.get("Authorization", "").encode(), esperada.encode()):
                raise ErrorProveedor("NO_AUTORIZADO", 401)

        def do_GET(self) -> None:
            try:
                path = urlsplit(self.path).path
                if path == "/health":
                    self.responder(200, {"status": "UP", "simulado": True})
                    return
                if path == "/control/estado":
                    self.autorizar(True)
                    self.responder(200, proveedor.resumen())
                    return
                self.autorizar()
                partes = path.strip("/").split("/")
                if len(partes) == 3 and partes[:2] == ["v1", "operaciones"]:
                    self.responder(200, proveedor.recibo(partes[2]))
                    return
                raise ErrorProveedor("RUTA_NO_ENCONTRADA", 404)
            except Exception as exc:  # noqa: BLE001 - toda falla se traduce, nunca queda sin respuesta
                self.fallar(exc)

        def do_POST(self) -> None:
            try:
                path = urlsplit(self.path).path
                self.autorizar(path.startswith("/control/"))
                entrada = self.cuerpo()
                if path == "/v1/operaciones":
                    fallo = proveedor.consumir_fallo()
                    if fallo == "PERDER_ANTES":
                        self.close_connection = True
                        return
                    forzado = "RESPUESTA_PERDIDA" if fallo == "LIQUIDAR_Y_PERDER" else None
                    operacion, perdida = proveedor.crear(entrada, self.headers.get("Idempotency-Key", ""), forzado)
                    if perdida or fallo == "PERDER_DESPUES":
                        self.close_connection = True
                        return
                    self.responder(201, proveedor.recibo(operacion["referencia"]))
                    return
                if path == "/control/resolver":
                    proveedor.resolver(entrada.get("referencia"), entrada.get("estado"))
                    self.responder(200, proveedor.recibo(entrada["referencia"]))
                    return
                if path == "/control/fallo":
                    self.responder(200, proveedor.armar_fallo(entrada.get("modo"), entrada.get("cantidad")))
                    return
                if path == "/control/reloj":
                    self.responder(200, {"instante": proveedor.avanzar(entrada.get("segundos"))})
                    return
                raise ErrorProveedor("RUTA_NO_ENCONTRADA", 404)
            except Exception as exc:  # noqa: BLE001 - toda falla se traduce, nunca queda sin respuesta
                self.fallar(exc)

    servidor = ThreadingHTTPServer((host, puerto), Handler)
    servidor.daemon_threads = True
    return servidor


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Proveedor externo simulado y persistente, solo local.")
    parser.add_argument("--database", type=Path, required=True)
    parser.add_argument("--port", type=int, default=4020)
    args = parser.parse_args(argv)
    if os.environ.get("PASANAKU_AMBIENTE") != "simulado":
        parser.error("Solo funciona con PASANAKU_AMBIENTE=simulado")
    try:
        proveedor = Proveedor(args.database, os.environ.get("PROVEEDOR_FIRMA_SECRETO", "").encode())
        servidor = crear_servidor("127.0.0.1", args.port, proveedor, os.environ.get("PROVEEDOR_API_KEY", ""), os.environ.get("PROVEEDOR_CONTROL_KEY", ""))
    except ErrorProveedor as exc:
        parser.error(exc.codigo)
    print(f"Proveedor SIMULADO en 127.0.0.1:{servidor.server_port}; sin dinero real.", flush=True)
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        servidor.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
