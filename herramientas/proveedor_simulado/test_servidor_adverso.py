"""Borde HTTP real del proveedor ficticio: toda peticion hostil recibe una respuesta controlada."""

from __future__ import annotations

import base64
import http.client
import json
import socket
import tempfile
import threading
import unittest
from contextlib import closing
from pathlib import Path
from uuid import uuid4

from .modelo import Proveedor
from .servidor import crear_servidor

API = "api-prueba-" + "a" * 32
CONTROL = "control-prueba-" + "b" * 32
SECRETO = b"firma-ficticia-solo-para-esta-prueba-0001"
CORTES = (http.client.RemoteDisconnected, ConnectionError, http.client.BadStatusLine)


class ServidorAdversoTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name) / "proveedor.sqlite"
        self.proveedor = Proveedor(self.path, SECRETO)
        self.ref = str(uuid4())
        self.entrada = {"referencia": self.ref, "tipo": "RECARGA", "monto": "500.00", "moneda": "BOB"}
        self.servidor = crear_servidor("127.0.0.1", 0, self.proveedor, API, CONTROL, segundos_por_conexion=1)
        self.worker = threading.Thread(target=self.servidor.serve_forever, daemon=True)
        self.worker.start()
        self.detenido = False
        self.addCleanup(self.detener)

    def detener(self) -> None:
        if self.detenido:
            return
        self.detenido = True
        self.servidor.shutdown()
        self.servidor.server_close()
        self.worker.join(timeout=3)

    def pedir(self, metodo: str, ruta: str, cuerpo: bytes | None = None, cabeceras: dict[str, str] | None = None) -> tuple[int, dict]:
        conexion = http.client.HTTPConnection("127.0.0.1", self.servidor.server_port, timeout=5)
        try:
            conexion.request(metodo, ruta, body=cuerpo, headers=cabeceras or {})
            respuesta = conexion.getresponse()
            return respuesta.status, json.loads(respuesta.read() or b"{}")
        finally:
            conexion.close()

    def crear(self, cuerpo: bytes, extra: dict[str, str] | None = None) -> tuple[int, dict]:
        cabeceras = {"Content-Type": "application/json", "Authorization": "Bearer " + API, "Idempotency-Key": self.ref}
        cabeceras.update(extra or {})
        return self.pedir("POST", "/v1/operaciones", cuerpo, cabeceras)

    def cantidad_de_operaciones(self) -> int:
        with closing(self.proveedor.conectar()) as db:
            return db.execute("SELECT count(*) FROM operaciones").fetchone()[0]

    def con_content_length(self, valor: str) -> int:
        """Envia solo cabeceras (sin cuerpo) con el Content-Length dado; devuelve el estado."""
        conexion = http.client.HTTPConnection("127.0.0.1", self.servidor.server_port, timeout=5)
        try:
            conexion.putrequest("POST", "/v1/operaciones")
            for clave, cabecera in {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + API,
                "Idempotency-Key": self.ref,
                "Content-Length": valor,
            }.items():
                conexion.putheader(clave, cabecera)
            conexion.endheaders()
            respuesta = conexion.getresponse()
            respuesta.read()
            return respuesta.status
        finally:
            conexion.close()

    def armar(self, modo: str, cantidad: int = 1) -> None:
        cuerpo = json.dumps({"modo": modo, "cantidad": cantidad}).encode()
        estado, _ = self.pedir("POST", "/control/fallo", cuerpo, {"Content-Type": "application/json", "Authorization": "Bearer " + CONTROL})
        self.assertEqual(estado, 200)

    def test_cuerpo_desmesurado_se_rechaza_con_413(self) -> None:
        estado, cuerpo = self.crear(b"{" + b" " * 20_000 + b"}")
        self.assertEqual((estado, cuerpo["codigo"]), (413, "CUERPO_DEMASIADO_GRANDE"))
        self.assertEqual(self.cantidad_de_operaciones(), 0)

    def test_content_length_enorme_sin_enviar_el_cuerpo_responde_413_sin_esperarlo(self) -> None:
        self.assertEqual(self.con_content_length(str(10**9)), 413)
        self.assertEqual(self.cantidad_de_operaciones(), 0)

    def test_content_length_invalido_o_ausente_responde_400(self) -> None:
        for valor in ("abc", "-5", "0", "", "1.5"):
            with self.subTest(content_length=valor):
                self.assertEqual(self.con_content_length(valor), 400)
        self.assertEqual(self.cantidad_de_operaciones(), 0)

    def test_tipo_de_contenido_distinto_de_json_responde_415(self) -> None:
        estado, cuerpo = self.crear(json.dumps(self.entrada).encode(), {"Content-Type": "text/plain"})
        self.assertEqual((estado, cuerpo["codigo"]), (415, "CONTENT_TYPE_INVALIDO"))

    def test_json_hostil_recibe_400_y_el_servidor_sigue_vivo(self) -> None:
        hostiles = [b"[" * 9_000, b'{"a":1,"a":2}', b'{"monto":NaN}', b"\xff\xfe{}", b"[]", b'"texto"', b"{"]
        for crudo in hostiles:
            with self.subTest(cuerpo=crudo[:20]):
                estado, cuerpo = self.crear(crudo)
                self.assertEqual(estado, 400, cuerpo)
        estado, _ = self.crear(json.dumps(self.entrada).encode())
        self.assertEqual(estado, 201)

    def test_campos_no_textuales_por_http_nunca_producen_error_interno(self) -> None:
        for campo in ("tipo", "escenario", "moneda", "condiciones", "monto", "referencia"):
            for valor in ([], {}, None, True, 1, ["RECARGA"], 1.5):
                with self.subTest(campo=campo, valor=valor):
                    estado, cuerpo = self.crear(json.dumps(dict(self.entrada, **{campo: valor})).encode())
                    self.assertNotEqual(estado, 500, cuerpo)
                    self.assertNotEqual(cuerpo.get("codigo"), "ERROR_INTERNO_CONTROLADO")
        self.assertLessEqual(self.cantidad_de_operaciones(), 1)

    def test_referencias_invalidas_e_inexistentes(self) -> None:
        estado, cuerpo = self.crear(json.dumps(self.entrada).encode(), {"Idempotency-Key": "no-es-uuid"})
        self.assertEqual((estado, cuerpo["codigo"]), (400, "REFERENCIA_INVALIDA"))
        estado, cuerpo = self.pedir("GET", "/v1/operaciones/no-es-uuid", None, {"Authorization": "Bearer " + API})
        self.assertEqual((estado, cuerpo["codigo"]), (400, "REFERENCIA_INVALIDA"))
        estado, cuerpo = self.pedir("GET", "/v1/operaciones/" + str(uuid4()), None, {"Authorization": "Bearer " + API})
        self.assertEqual((estado, cuerpo["codigo"]), (404, "OPERACION_NO_ENCONTRADA"))

    def test_sin_credencial_no_se_evalua_el_cuerpo(self) -> None:
        estado, cuerpo = self.pedir("POST", "/v1/operaciones", b"x" * 20_000, {"Content-Type": "application/json"})
        self.assertEqual((estado, cuerpo["codigo"]), (401, "NO_AUTORIZADO"))
        estado, _ = self.pedir("POST", "/control/fallo", b"{}", {"Content-Type": "application/json", "Authorization": "Bearer " + API})
        self.assertEqual(estado, 401)

    def test_conexion_lenta_no_retiene_el_hilo(self) -> None:
        with socket.create_connection(("127.0.0.1", self.servidor.server_port), timeout=5) as lenta:
            lenta.sendall(
                (
                    "POST /v1/operaciones HTTP/1.1\r\nHost: x\r\nContent-Type: application/json\r\n"
                    f"Authorization: Bearer {API}\r\nIdempotency-Key: {self.ref}\r\nContent-Length: 100\r\n\r\n{{"
                ).encode()
            )
            lenta.settimeout(4)
            try:
                lenta.recv(1024)
            except socket.timeout:
                self.fail("el servidor retuvo la conexion lenta mas alla de su limite")
        estado, _ = self.crear(json.dumps(self.entrada).encode())
        self.assertEqual(estado, 201)

    def test_control_de_fallos_rechaza_valores_invalidos(self) -> None:
        invalidos = [
            {"modo": "OTRO", "cantidad": 1},
            {"modo": "PERDER_ANTES", "cantidad": 0},
            {"modo": "PERDER_ANTES", "cantidad": 99},
            {"modo": [], "cantidad": 1},
            {"modo": "PERDER_ANTES", "cantidad": True},
            {"modo": "PERDER_ANTES", "cantidad": "1"},
        ]
        for cuerpo in invalidos:
            with self.subTest(cuerpo=cuerpo):
                estado, respuesta = self.pedir("POST", "/control/fallo", json.dumps(cuerpo).encode(), {"Content-Type": "application/json", "Authorization": "Bearer " + CONTROL})
                self.assertEqual((estado, respuesta["codigo"]), (400, "FALLO_INVALIDO"))

    def test_corte_antes_de_registrar_no_deja_operacion_y_el_reenvio_crea_una(self) -> None:
        self.armar("PERDER_ANTES")
        with self.assertRaises(CORTES):
            self.crear(json.dumps(self.entrada).encode())
        self.assertEqual(self.cantidad_de_operaciones(), 0)
        estado, _ = self.crear(json.dumps(self.entrada).encode())
        self.assertEqual(estado, 201)
        self.assertEqual(self.cantidad_de_operaciones(), 1)

    def test_corte_despues_de_registrar_conserva_la_operacion_tras_reiniciar_el_proceso(self) -> None:
        entrada = dict(self.entrada, escenario="CONFIRMADO")
        self.armar("PERDER_DESPUES")
        with self.assertRaises(CORTES):
            self.crear(json.dumps(entrada).encode())
        self.detener()
        reabierto = Proveedor(self.path, SECRETO)
        consultada = reabierto.consultar(self.ref)
        self.assertEqual(consultada["estado"], "CONFIRMADO")
        servidor = crear_servidor("127.0.0.1", 0, reabierto, API, CONTROL)
        worker = threading.Thread(target=servidor.serve_forever, daemon=True)
        worker.start()
        try:
            conexion = http.client.HTTPConnection("127.0.0.1", servidor.server_port, timeout=5)
            cabeceras = {"Content-Type": "application/json", "Authorization": "Bearer " + API, "Idempotency-Key": self.ref}
            conexion.request("POST", "/v1/operaciones", json.dumps(entrada).encode(), cabeceras)
            respuesta = conexion.getresponse()
            sobre = json.loads(respuesta.read())
            conexion.close()
            self.assertEqual(respuesta.status, 201)
            self.assertEqual(json.loads(base64.b64decode(sobre["payload"]))["transaccionProveedor"], consultada["transaccionProveedor"])
        finally:
            servidor.shutdown()
            servidor.server_close()
            worker.join(timeout=3)
        with closing(reabierto.conectar()) as db:
            self.assertEqual(db.execute("SELECT count(*) FROM operaciones").fetchone()[0], 1)

    def test_liquidar_y_perder_deja_la_operacion_confirmada_y_el_reenvio_identico_devuelve_lo_mismo(self) -> None:
        self.armar("LIQUIDAR_Y_PERDER")
        with self.assertRaises(CORTES):
            self.crear(json.dumps(self.entrada).encode())
        self.assertEqual(self.proveedor.consultar(self.ref)["estado"], "CONFIRMADO")
        estado, sobre = self.crear(json.dumps(self.entrada).encode())
        self.assertEqual(estado, 201)
        self.assertEqual(json.loads(base64.b64decode(sobre["payload"]))["estado"], "CONFIRMADO")
        self.assertEqual(self.cantidad_de_operaciones(), 1)

    def test_el_resumen_de_control_cuenta_operaciones_y_exige_la_clave_de_control(self) -> None:
        estado, _ = self.pedir("GET", "/control/estado", None, {"Authorization": "Bearer " + API})
        self.assertEqual(estado, 401)
        self.assertEqual(self.crear(json.dumps(self.entrada).encode())[0], 201)
        self.assertEqual(self.crear(json.dumps(self.entrada).encode())[0], 201)
        estado, resumen = self.pedir("GET", "/control/estado", None, {"Authorization": "Bearer " + CONTROL})
        self.assertEqual((estado, resumen), (200, {"operaciones": 1, "confirmadas": 0, "pendientes": 1, "rechazadas": 0}))

    def test_el_fallo_armado_sobrevive_al_reinicio_y_se_consume_una_vez_por_corte(self) -> None:
        self.armar("PERDER_ANTES", 2)
        otro = Proveedor(self.path, SECRETO)
        self.assertEqual([otro.consumir_fallo(), otro.consumir_fallo(), otro.consumir_fallo()], ["PERDER_ANTES", "PERDER_ANTES", None])


if __name__ == "__main__":
    unittest.main()
