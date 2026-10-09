"""Pruebas de persistencia, concurrencia, autenticidad y servidor HTTP real local."""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import tempfile
import threading
import unittest
from concurrent.futures import ThreadPoolExecutor
from contextlib import closing
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from uuid import uuid4

from .modelo import ErrorProveedor, Proveedor
from .servidor import crear_servidor


class ProveedorTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name) / "proveedor.sqlite"
        self.secret = b"firma-ficticia-solo-para-esta-prueba-0001"
        self.proveedor = Proveedor(self.path, self.secret)
        self.ref = str(uuid4())
        self.entrada = {"referencia": self.ref, "tipo": "RECARGA", "monto": "500.00", "moneda": "BOB"}

    def test_no_confirma_al_solicitar(self) -> None:
        salida, perdida = self.proveedor.crear(self.entrada, self.ref)
        self.assertEqual(salida["estado"], "PENDIENTE")
        self.assertIsNone(salida["liquidadaEn"])
        self.assertFalse(perdida)

    def test_persistencia_tras_reabrir(self) -> None:
        self.proveedor.crear(self.entrada, self.ref)
        self.proveedor.resolver(self.ref, "CONFIRMADO")
        otro = Proveedor(self.path, self.secret)
        self.assertEqual(otro.consultar(self.ref)["estado"], "CONFIRMADO")

    def test_concurrencia_tiene_una_operacion(self) -> None:
        with ThreadPoolExecutor(max_workers=4) as pool:
            resultados = list(pool.map(lambda _: self.proveedor.crear(self.entrada, self.ref), range(8)))
        self.assertEqual(len({r[0]["transaccionProveedor"] for r in resultados}), 1)
        with closing(self.proveedor.conectar()) as db:
            self.assertEqual(db.execute("SELECT count(*) FROM operaciones").fetchone()[0], 1)
            self.assertEqual(db.execute("SELECT count(*) FROM cambios").fetchone()[0], 1)

    def test_idempotencia_no_acepta_otro_monto(self) -> None:
        self.proveedor.crear(self.entrada, self.ref)
        with self.assertRaisesRegex(ErrorProveedor, "IDEMPOTENCIA"):
            self.proveedor.crear(dict(self.entrada, monto="999.00"), self.ref)
        self.assertEqual(self.proveedor.consultar(self.ref)["monto"], "500.00")

    def test_firma_cubre_bytes_originales(self) -> None:
        self.proveedor.crear(self.entrada, self.ref)
        recibo = self.proveedor.recibo(self.ref)
        raw = base64.b64decode(recibo["payload"])
        self.assertEqual(hmac.new(self.secret, raw, hashlib.sha256).hexdigest(), recibo["firma"])
        alterado = raw.replace(b"500.00", b"900.00")
        self.assertNotEqual(hmac.new(self.secret, alterado, hashlib.sha256).hexdigest(), recibo["firma"])

    def test_estado_terminal_no_retrocede(self) -> None:
        self.proveedor.crear(self.entrada, self.ref)
        original = self.proveedor.resolver(self.ref, "CONFIRMADO")
        repetida = self.proveedor.resolver(self.ref, "CONFIRMADO")
        self.assertEqual(original, repetida)
        with self.assertRaisesRegex(ErrorProveedor, "TERMINAL"):
            self.proveedor.resolver(self.ref, "RECHAZADO")

    def test_respuesta_perdida_no_pierde_liquidacion(self) -> None:
        self.entrada["escenario"] = "RESPUESTA_PERDIDA"
        _, perdida = self.proveedor.crear(self.entrada, self.ref)
        self.assertTrue(perdida)
        consultada = self.proveedor.consultar(self.ref)
        reintento, perdida = self.proveedor.crear(self.entrada, self.ref)
        self.assertFalse(perdida)
        self.assertEqual(consultada, reintento)
        self.assertEqual(reintento["estado"], "CONFIRMADO")

    def test_montos_invalidos(self) -> None:
        for valor in [500, 1.1, "NaN", "Infinity", "-1.00", "0.00", "01.00", "1.001", None, {}]:
            with self.subTest(valor=valor), self.assertRaises(ErrorProveedor):
                self.proveedor.crear(dict(self.entrada, monto=valor), self.ref)

    def test_tipos_compuestos_no_producen_error_interno(self) -> None:
        for campo in ("tipo", "escenario"):
            for valor in ([], {}, None, True, 1):
                with self.subTest(campo=campo, valor=valor), self.assertRaises(ErrorProveedor):
                    self.proveedor.crear(dict(self.entrada, **{campo: valor}), self.ref)
        self.proveedor.crear(self.entrada, self.ref)
        for valor in ([], {}, None, True):
            with self.subTest(estado=valor), self.assertRaises(ErrorProveedor):
                self.proveedor.resolver(self.ref, valor)
        self.assertEqual(self.proveedor.consultar(self.ref)["estado"], "PENDIENTE")

    def test_control_reloj_sin_retroceso(self) -> None:
        self.proveedor.crear(self.entrada, self.ref)
        futuro = self.proveedor.avanzar(86400)
        self.assertEqual(self.proveedor.resolver(self.ref, "CONFIRMADO")["liquidadaEn"], futuro)
        for valor in [-1, True, "3600"]:
            with self.assertRaises(ErrorProveedor):
                self.proveedor.avanzar(valor)

    def test_historial_append_only(self) -> None:
        self.proveedor.crear(self.entrada, self.ref)
        import sqlite3
        with closing(self.proveedor.conectar()) as db:
            with self.assertRaisesRegex(sqlite3.IntegrityError, "APPEND_ONLY"):
                db.execute("DELETE FROM cambios")

    def test_http_separa_clave_operativa_y_control(self) -> None:
        api = "api-prueba-" + "a" * 32
        control = "control-prueba-" + "b" * 32
        servidor = crear_servidor("127.0.0.1", 0, self.proveedor, api, control)
        worker = threading.Thread(target=servidor.serve_forever, daemon=True)
        worker.start()
        base = f"http://127.0.0.1:{servidor.server_port}"
        try:
            req = Request(base + "/v1/operaciones", data=json.dumps(self.entrada).encode(), headers={"Content-Type": "application/json", "Authorization": "Bearer " + api, "Idempotency-Key": self.ref})
            with urlopen(req, timeout=3) as response:
                self.assertEqual(response.status, 201)
            req = Request(base + "/control/resolver", data=json.dumps({"referencia": self.ref, "estado": "CONFIRMADO"}).encode(), headers={"Content-Type": "application/json", "Authorization": "Bearer " + api})
            with self.assertRaises(HTTPError) as error:
                urlopen(req, timeout=3)
            self.assertEqual(error.exception.code, 401)
            error.exception.close()
            self.assertEqual(self.proveedor.consultar(self.ref)["estado"], "PENDIENTE")
        finally:
            servidor.shutdown()
            servidor.server_close()
            worker.join(timeout=3)
            self.assertFalse(worker.is_alive())


if __name__ == "__main__":
    unittest.main()
