"""Pruebas del aliado simulado: estado persistente, tiempo, concurrencia, firma y HTTP real local.

Los valores esperados de interes salen de calculos hechos a mano (principal x tasa x dias / base),
no de la funcion que se prueba. Todos los datos son sinteticos.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import sqlite3
import tempfile
import threading
import unittest
from concurrent.futures import ThreadPoolExecutor
from contextlib import closing
from datetime import datetime, timezone
from http.client import RemoteDisconnected
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from uuid import uuid4

from .modelo import Aliado, ErrorAliado
from .servidor import crear_servidor

# Lunes 12 de octubre de 2026, 08:00 en La Paz (12:00 UTC): dia habil, antes del corte de las 15:00.
LUNES_MANANA = datetime(2026, 10, 12, 12, 0, tzinfo=timezone.utc)
SECRETO = b"firma-ficticia-solo-para-esta-prueba-0001"


class AliadoBase(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.path = Path(self.tmp.name) / "aliado.sqlite"
        self.aliado = Aliado(self.path, SECRETO, inicio=LUNES_MANANA)
        self.titular = str(uuid4())

    def suscribir(self, producto: str, monto: str, escenario: str = "AUTO") -> dict:
        ref = str(uuid4())
        entrada = {"referencia": ref, "producto": producto, "monto": monto, "moneda": "BOB", "titularRef": self.titular, "escenario": escenario}
        return self.aliado.suscribir(entrada, ref)[0]

    def rescatar(self, posicion: str, cuotas: str | None = None, escenario: str = "AUTO") -> dict:
        ref = str(uuid4())
        entrada = {"referencia": ref, "posicion": posicion, "escenario": escenario}
        if cuotas is not None:
            entrada["cuotas"] = cuotas
        return self.aliado.rescatar(entrada, ref)[0]

    def valor(self, fecha: str, valor: str) -> None:
        self.aliado.publicar_valor_cuota("FONDO-DEMO-ABIERTO", fecha, valor)


class DpfTest(AliadoBase):
    def test_suscripcion_dpf_confirma_con_posicion_y_vencimiento(self) -> None:
        op = self.suscribir("DPF-DEMO-180", "10000.00")
        self.assertEqual(op["estado"], "CONFIRMADO")
        pos = self.aliado.posicion(op["posicionExterna"])
        self.assertEqual((pos["principal"], pos["fechaConstitucion"], pos["fechaVencimiento"]), ("10000.00", "2026-10-12", "2027-04-10"))

    def test_minimo_y_producto(self) -> None:
        with self.assertRaisesRegex(ErrorAliado, "MONTO_BAJO_EL_MINIMO"):
            self.suscribir("DPF-DEMO-180", "499.99")
        with self.assertRaisesRegex(ErrorAliado, "PRODUCTO_INEXISTENTE"):
            self.suscribir("NO-EXISTE", "1000.00")

    def test_vencimiento_calculo_hecho_a_mano(self) -> None:
        # 10000 x 4% x 180 / 365 = 197.2602... -> 197.26 ; retencion sintetica 10% = 19.726 -> 19.73 (half-even)
        op = self.suscribir("DPF-DEMO-180", "10000.00")
        self.aliado.avanzar(180 * 86400)
        r = self.rescatar(op["posicionExterna"])
        self.assertEqual(r["estado"], "CONFIRMADO")
        d = r["detalle"]
        self.assertEqual((d["modo"], d["interesBruto"], d["retencion"], d["netoAcreditar"]), ("VENCIMIENTO", "197.26", "19.73", "10177.53"))
        self.assertEqual(self.aliado.posicion(op["posicionExterna"])["estado"], "CERRADA")

    def test_base_360_a_plazo_completo(self) -> None:
        # 10000 x 4,5% x 360 / 360 = 450.00 ; retencion 45.00 ; neto 10405.00
        op = self.suscribir("DPF-DEMO-360-ANTICIPABLE", "10000.00")
        self.aliado.avanzar(360 * 86400)
        d = self.rescatar(op["posicionExterna"])["detalle"]
        self.assertEqual((d["interesBruto"], d["retencion"], d["netoAcreditar"]), ("450.00", "45.00", "10405.00"))

    def test_rescate_anticipado_no_permitido(self) -> None:
        op = self.suscribir("DPF-DEMO-180", "10000.00")
        self.aliado.avanzar(10 * 86400)
        with self.assertRaisesRegex(ErrorAliado, "RESCATE_ANTICIPADO_NO_PERMITIDO"):
            self.rescatar(op["posicionExterna"])
        self.assertEqual(self.aliado.posicion(op["posicionExterna"])["estado"], "VIGENTE")

    def test_rescate_anticipado_permitido_con_penalizacion(self) -> None:
        # 90 dias: 10000 x 4,5% x 90 / 360 = 112.50 ; penalizacion sintetica 50% -> 56.25 ; retencion 5.625 -> 5.62 ; neto 10050.63
        op = self.suscribir("DPF-DEMO-360-ANTICIPABLE", "10000.00")
        self.aliado.avanzar(90 * 86400)
        d = self.rescatar(op["posicionExterna"])["detalle"]
        self.assertEqual((d["modo"], d["interesBruto"], d["retencion"], d["netoAcreditar"]), ("ANTICIPADO", "56.25", "5.62", "10050.63"))

    def test_dpf_no_se_rescata_dos_veces(self) -> None:
        op = self.suscribir("DPF-DEMO-180", "10000.00")
        self.aliado.avanzar(181 * 86400)
        self.rescatar(op["posicionExterna"])
        with self.assertRaisesRegex(ErrorAliado, "POSICION_NO_VIGENTE"):
            self.rescatar(op["posicionExterna"])


class FondoTest(AliadoBase):
    def test_cuotas_se_truncan_a_seis_decimales(self) -> None:
        self.valor("2026-10-12", "100.500000")
        op = self.suscribir("FONDO-DEMO-ABIERTO", "1000.00")
        self.assertEqual((op["estado"], op["cuotas"], op["fechaValor"]), ("CONFIRMADO", "9.950248", "2026-10-12"))

    def test_orden_despues_del_corte_toma_el_proximo_dia_habil(self) -> None:
        self.aliado.avanzar(8 * 3600)  # 16:00 en La Paz, pasado el corte de las 15:00
        op = self.suscribir("FONDO-DEMO-ABIERTO", "1000.00")
        self.assertEqual((op["estado"], op["fechaValor"]), ("PENDIENTE", "2026-10-13"))

    def test_viernes_tarde_y_feriado_saltan_al_martes(self) -> None:
        self.aliado.avanzar(4 * 86400 + 8 * 3600)  # viernes 16:00
        self.aliado.marcar_no_habil("2026-10-19")  # el lunes siguiente es feriado de prueba
        op = self.suscribir("FONDO-DEMO-ABIERTO", "1000.00")
        self.assertEqual(op["fechaValor"], "2026-10-20")

    def test_sin_valor_publicado_queda_pendiente_y_se_asienta_al_publicarlo(self) -> None:
        op = self.suscribir("FONDO-DEMO-ABIERTO", "1000.00")
        self.assertEqual(op["estado"], "PENDIENTE")
        self.valor("2026-10-12", "100.000000")
        confirmada = self.aliado.consultar(op["referencia"])
        self.assertEqual((confirmada["estado"], confirmada["cuotas"]), ("CONFIRMADO", "10.000000"))

    def test_valor_publicado_no_se_reescribe(self) -> None:
        self.valor("2026-10-12", "100.000000")
        self.valor("2026-10-12", "100.000000")  # idempotente
        with self.assertRaisesRegex(ErrorAliado, "VALOR_YA_PUBLICADO"):
            self.valor("2026-10-12", "101.000000")

    def test_rescate_demorado_y_valor_aplicado(self) -> None:
        self.valor("2026-10-12", "100.000000")
        op = self.suscribir("FONDO-DEMO-ABIERTO", "1000.00")
        self.aliado.avanzar(86400)  # martes 08:00 La Paz
        self.valor("2026-10-13", "102.000000")
        r = self.rescatar(op["posicionExterna"], "4.000000")
        self.assertEqual((r["estado"], r["fechaValor"]), ("PENDIENTE", "2026-10-13"))
        self.assertTrue(r["liquidaEn"].startswith("2026-10-14T13:00"))  # miercoles 09:00 La Paz
        self.aliado.avanzar(86400)  # miercoles 08:00 La Paz: aun falta una hora para la liquidacion
        self.assertEqual(self.aliado.consultar(r["referencia"])["estado"], "PENDIENTE")
        self.aliado.avanzar(3600)
        ok = self.aliado.consultar(r["referencia"])
        self.assertEqual((ok["estado"], ok["monto"]), ("CONFIRMADO", "408.00"))  # 4 x 102
        self.assertEqual(self.aliado.posicion(op["posicionExterna"])["cuotas"], "6.000000")

    def test_doble_disponibilidad_se_rechaza(self) -> None:
        self.valor("2026-10-12", "100.000000")
        op = self.suscribir("FONDO-DEMO-ABIERTO", "1000.00")  # 10 cuotas
        self.rescatar(op["posicionExterna"], "7.000000")
        with self.assertRaisesRegex(ErrorAliado, "DOBLE_DISPONIBILIDAD"):
            self.rescatar(op["posicionExterna"], "4.000000")
        self.assertEqual(self.aliado.posicion(op["posicionExterna"])["cuotasEnRescate"], "7.000000")
        self.rescatar(op["posicionExterna"], "3.000000")  # lo que queda libre si se puede

    def test_rescate_rechazado_libera_las_cuotas(self) -> None:
        self.valor("2026-10-12", "100.000000")
        op = self.suscribir("FONDO-DEMO-ABIERTO", "1000.00")
        r = self.rescatar(op["posicionExterna"], "10.000000", escenario="PENDIENTE")
        self.aliado.resolver(r["referencia"], "RECHAZADO")
        self.assertEqual(self.aliado.posicion(op["posicionExterna"])["cuotasEnRescate"], "0")
        self.rescatar(op["posicionExterna"], "10.000000")


class ContratoTest(AliadoBase):
    def test_idempotencia_mismo_contenido_y_contenido_distinto(self) -> None:
        ref = str(uuid4())
        entrada = {"referencia": ref, "producto": "DPF-DEMO-180", "monto": "1000.00", "moneda": "BOB", "titularRef": self.titular}
        a, _ = self.aliado.suscribir(entrada, ref)
        b, _ = self.aliado.suscribir(entrada, ref)
        self.assertEqual(a["transaccionProveedor"], b["transaccionProveedor"])
        with self.assertRaisesRegex(ErrorAliado, "IDEMPOTENCIA"):
            self.aliado.suscribir(dict(entrada, monto="2000.00"), ref)
        with closing(self.aliado.conectar()) as db:
            self.assertEqual(db.execute("SELECT count(*) FROM posiciones").fetchone()[0], 1)

    def test_concurrencia_una_sola_operacion_y_una_sola_posicion(self) -> None:
        ref = str(uuid4())
        entrada = {"referencia": ref, "producto": "DPF-DEMO-180", "monto": "1000.00", "moneda": "BOB", "titularRef": self.titular}
        with ThreadPoolExecutor(max_workers=6) as pool:
            res = list(pool.map(lambda _: self.aliado.suscribir(entrada, ref)[0], range(12)))
        self.assertEqual(len({r["transaccionProveedor"] for r in res}), 1)
        with closing(self.aliado.conectar()) as db:
            self.assertEqual(db.execute("SELECT count(*) FROM operaciones").fetchone()[0], 1)
            self.assertEqual(db.execute("SELECT count(*) FROM posiciones").fetchone()[0], 1)

    def test_persistencia_tras_reabrir(self) -> None:
        op = self.suscribir("DPF-DEMO-180", "1000.00")
        otro = Aliado(self.path, SECRETO)
        self.assertEqual(otro.consultar(op["referencia"])["estado"], "CONFIRMADO")

    def test_respuesta_perdida_no_pierde_la_operacion(self) -> None:
        ref = str(uuid4())
        entrada = {"referencia": ref, "producto": "DPF-DEMO-180", "monto": "1000.00", "moneda": "BOB", "titularRef": self.titular, "escenario": "RESPUESTA_PERDIDA"}
        _, perdida = self.aliado.suscribir(entrada, ref)
        self.assertTrue(perdida)
        self.assertEqual(self.aliado.consultar(ref)["estado"], "CONFIRMADO")

    def test_firma_cubre_los_bytes_originales(self) -> None:
        op = self.suscribir("DPF-DEMO-180", "1000.00")
        recibo = self.aliado.recibo(op["referencia"])
        raw = base64.b64decode(recibo["payload"])
        self.assertEqual(hmac.new(SECRETO, raw, hashlib.sha256).hexdigest(), recibo["firma"])
        self.assertNotEqual(hmac.new(SECRETO, raw.replace(b"1000.00", b"9000.00"), hashlib.sha256).hexdigest(), recibo["firma"])

    def test_entradas_invalidas_no_producen_error_interno(self) -> None:
        ref = str(uuid4())
        base = {"referencia": ref, "producto": "DPF-DEMO-180", "monto": "1000.00", "moneda": "BOB", "titularRef": self.titular}
        invalidos = {
            "monto": [1000, 1000.0, "NaN", "-1.00", "0.00", "01.00", "1.001", None, {}],
            "moneda": ["USD", None, []],
            "escenario": [[], {}, None, 1, "OTRO"],
            "titularRef": ["x", None, 7],
        }
        for campo, valores in invalidos.items():
            for v in valores:
                with self.subTest(campo=campo, valor=v), self.assertRaises(ErrorAliado):
                    self.aliado.suscribir(dict(base, **{campo: v}), ref)
        with self.assertRaises(ErrorAliado):
            self.aliado.suscribir(dict(base, extra="x"), ref)
        with closing(self.aliado.conectar()) as db:
            self.assertEqual(db.execute("SELECT count(*) FROM operaciones").fetchone()[0], 0)

    def test_historial_append_only(self) -> None:
        self.suscribir("DPF-DEMO-180", "1000.00")
        with closing(self.aliado.conectar()) as db, self.assertRaisesRegex(sqlite3.IntegrityError, "APPEND_ONLY"):
            db.execute("DELETE FROM cambios")

    def test_el_catalogo_se_declara_sintetico(self) -> None:
        cat = self.aliado.productos()
        self.assertTrue(cat["simulado"])
        for p in cat["productos"]:
            self.assertEqual(p["origenDatos"], "SINTETICO")
            self.assertIn("SINTETICO", p["fuente"])
            self.assertIsNotNone(p["fechaCotizacion"])


class HttpTest(AliadoBase):
    def setUp(self) -> None:
        super().setUp()
        self.api = "api-prueba-" + "a" * 32
        self.control = "control-prueba-" + "b" * 32
        self.servidor = crear_servidor("127.0.0.1", 0, self.aliado, self.api, self.control)
        self.worker = threading.Thread(target=self.servidor.serve_forever, daemon=True)
        self.worker.start()
        self.base = f"http://127.0.0.1:{self.servidor.server_port}"
        self.addCleanup(self.parar)

    def parar(self) -> None:
        self.servidor.shutdown()
        self.servidor.server_close()
        self.worker.join(timeout=3)
        self.assertFalse(self.worker.is_alive())

    def post(self, ruta: str, cuerpo: dict, token: str | None = None, clave: str | None = None):
        cab = {"Content-Type": "application/json", "Authorization": "Bearer " + (token or self.api)}
        if clave:
            cab["Idempotency-Key"] = clave
        return urlopen(Request(self.base + ruta, data=json.dumps(cuerpo).encode(), headers=cab), timeout=3)

    def test_clave_operativa_no_alcanza_los_controles(self) -> None:
        with self.assertRaises(HTTPError) as e:
            self.post("/control/reloj", {"segundos": 5})
        self.assertEqual(e.exception.code, 401)
        e.exception.close()

    def test_respuesta_perdida_por_http_y_recuperacion_por_consulta(self) -> None:
        ref = str(uuid4())
        cuerpo = {"referencia": ref, "producto": "DPF-DEMO-180", "monto": "1000.00", "moneda": "BOB", "titularRef": self.titular, "escenario": "RESPUESTA_PERDIDA"}
        with self.assertRaises((RemoteDisconnected, ConnectionError, OSError)):
            self.post("/v1/suscripciones", cuerpo, clave=ref)
        consulta = urlopen(Request(self.base + f"/v1/operaciones/{ref}", headers={"Authorization": "Bearer " + self.api}), timeout=3)
        payload = json.loads(base64.b64decode(json.loads(consulta.read())["payload"]))
        self.assertEqual(payload["estado"], "CONFIRMADO")

    def test_indisponible_devuelve_503_y_no_persiste(self) -> None:
        ref = str(uuid4())
        cuerpo = {"referencia": ref, "producto": "DPF-DEMO-180", "monto": "1000.00", "moneda": "BOB", "titularRef": self.titular, "escenario": "INDISPONIBLE"}
        with self.assertRaises(HTTPError) as e:
            self.post("/v1/suscripciones", cuerpo, clave=ref)
        self.assertEqual(e.exception.code, 503)
        e.exception.close()
        with closing(self.aliado.conectar()) as db:
            self.assertEqual(db.execute("SELECT count(*) FROM operaciones").fetchone()[0], 0)
        cuerpo["escenario"] = "AUTO"
        self.assertEqual(self.post("/v1/suscripciones", cuerpo, clave=ref).status, 201)  # el reintento ya entra

    def test_catalogo_y_valor_de_cuota_firmados(self) -> None:
        self.valor("2026-10-12", "100.250000")
        for ruta in ("/v1/productos", "/v1/productos/FONDO-DEMO-ABIERTO/valor-cuota?fecha=2026-10-12"):
            r = json.loads(urlopen(Request(self.base + ruta, headers={"Authorization": "Bearer " + self.api}), timeout=3).read())
            raw = base64.b64decode(r["payload"])
            self.assertEqual(hmac.new(SECRETO, raw, hashlib.sha256).hexdigest(), r["firma"])
            self.assertTrue(json.loads(raw)["simulado"])


if __name__ == "__main__":
    unittest.main()
