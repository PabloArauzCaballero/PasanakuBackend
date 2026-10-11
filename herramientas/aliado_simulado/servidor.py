"""Servidor HTTP local del aliado de inversion ficticio; interfaz interna, no API de un banco ni de una SAFI."""

from __future__ import annotations

import argparse
import hmac
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import parse_qs, urlsplit

from .modelo import Aliado, ErrorAliado, instante, serializar


def objeto_unico(pares: list[tuple[str, Any]]) -> dict[str, Any]:
    salida: dict[str, Any] = {}
    for clave, valor in pares:
        if clave in salida:
            raise ErrorAliado("CLAVE_JSON_DUPLICADA")
        salida[clave] = valor
    return salida


def rechazar_constante(_: str) -> None:
    raise ErrorAliado("JSON_NO_FINITO")


def crear_servidor(host: str, puerto: int, aliado: Aliado, api_key: str, control_key: str) -> ThreadingHTTPServer:
    if host not in {"127.0.0.1", "localhost"} or min(len(api_key), len(control_key)) < 32 or api_key == control_key:
        raise ErrorAliado("CONFIGURACION_LOCAL_INVALIDA")

    class Handler(BaseHTTPRequestHandler):
        server_version = "AliadoFicticio/1"

        def log_message(self, formato: str, *args: Any) -> None:
            return  # sin URLs, credenciales ni importes en los logs del servidor de pruebas

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
            try:
                tamano = int(self.headers.get("Content-Length", "0"))
                if not 0 < tamano <= 16_384 or self.headers.get_content_type() != "application/json":
                    raise ErrorAliado("CUERPO_INVALIDO")
                valor = json.loads(self.rfile.read(tamano), object_pairs_hook=objeto_unico, parse_constant=rechazar_constante)
                if not isinstance(valor, dict):
                    raise ErrorAliado("OBJETO_JSON_REQUERIDO")
                return valor
            except (json.JSONDecodeError, UnicodeDecodeError, ValueError) as exc:
                if isinstance(exc, ErrorAliado):
                    raise
                raise ErrorAliado("JSON_INVALIDO") from exc

        def autorizar(self, control: bool = False) -> None:
            esperada = "Bearer " + (control_key if control else api_key)
            if not hmac.compare_digest(self.headers.get("Authorization", "").encode(), esperada.encode()):
                raise ErrorAliado("NO_AUTORIZADO", 401)

        def do_GET(self) -> None:
            try:
                url = urlsplit(self.path)
                if url.path == "/health":
                    self.responder(200, {"status": "UP", "simulado": True})
                    return
                self.autorizar()
                partes = url.path.strip("/").split("/")
                if partes == ["v1", "productos"]:
                    self.responder(200, aliado.firmar(aliado.productos()))
                elif len(partes) == 4 and partes[:2] == ["v1", "productos"] and partes[3] == "valor-cuota":
                    fecha = parse_qs(url.query).get("fecha", [None])[0]
                    self.responder(200, aliado.firmar(aliado.valor_cuota(partes[2], fecha)))
                elif len(partes) == 3 and partes[:2] == ["v1", "operaciones"]:
                    self.responder(200, aliado.recibo(partes[2]))
                elif len(partes) == 3 and partes[:2] == ["v1", "posiciones"]:
                    self.responder(200, aliado.firmar(aliado.posicion(partes[2])))
                else:
                    raise ErrorAliado("RUTA_NO_ENCONTRADA", 404)
            except ErrorAliado as exc:
                self.responder(exc.status, {"codigo": exc.codigo})

        def do_POST(self) -> None:
            try:
                path = urlsplit(self.path).path
                self.autorizar(path.startswith("/control/"))
                entrada = self.cuerpo()
                clave = self.headers.get("Idempotency-Key", "")
                if path in ("/v1/suscripciones", "/v1/rescates"):
                    crear = aliado.suscribir if path == "/v1/suscripciones" else aliado.rescatar
                    operacion, perdida = crear(entrada, clave)
                    if perdida:
                        self.close_connection = True  # la operacion quedo hecha; la respuesta, no
                        return
                    self.responder(201, aliado.firmar(operacion))
                elif path == "/control/resolver":
                    aliado.resolver(entrada.get("referencia"), entrada.get("estado"))
                    self.responder(200, aliado.recibo(entrada["referencia"]))
                elif path == "/control/reloj":
                    self.responder(200, {"instante": aliado.avanzar(entrada.get("segundos"))})
                elif path == "/control/valor-cuota":
                    self.responder(200, aliado.publicar_valor_cuota(entrada.get("producto"), entrada.get("fecha"), entrada.get("valor")))
                elif path == "/control/no-habil":
                    self.responder(200, {"fecha": aliado.marcar_no_habil(entrada.get("fecha"))})
                else:
                    raise ErrorAliado("RUTA_NO_ENCONTRADA", 404)
            except ErrorAliado as exc:
                self.responder(exc.status, {"codigo": exc.codigo})

    servidor = ThreadingHTTPServer((host, puerto), Handler)
    servidor.daemon_threads = True
    return servidor


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Aliado de inversion simulado y persistente, solo local.")
    parser.add_argument("--database", type=Path, required=True)
    parser.add_argument("--port", type=int, default=4030)
    parser.add_argument("--inicio", help="instante inicial del reloj de negocio (ISO 8601); solo rige si la base es nueva")
    args = parser.parse_args(argv)
    if os.environ.get("PASANAKU_AMBIENTE") != "simulado":
        parser.error("Solo funciona con PASANAKU_AMBIENTE=simulado")
    try:
        aliado = Aliado(args.database, os.environ.get("ALIADO_FIRMA_SECRETO", "").encode(), instante(args.inicio) if args.inicio else None)
        servidor = crear_servidor("127.0.0.1", args.port, aliado, os.environ.get("ALIADO_API_KEY", ""), os.environ.get("ALIADO_CONTROL_KEY", ""))
    except ErrorAliado as exc:
        parser.error(exc.codigo)
    print(f"Aliado de inversion SIMULADO en 127.0.0.1:{servidor.server_port}; sin dinero real.", flush=True)
    try:
        servidor.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        servidor.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
