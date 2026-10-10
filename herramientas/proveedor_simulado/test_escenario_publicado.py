"""H3.S1.M3 · el escenario publicado en README.md («Escenario reproducible con semilla y cronología»), ejecutado paso a paso.

Misma semilla -> mismas referencias y claves; el reloj de negocio arranca en T0 (el instante en que se abre el archivo)
y avanza solo cuando el escenario lo dice, así que la cronología se afirma en instantes RELATIVOS a T0. Lo único que no
es determinista es `transaccionProveedor` (un uuid4 por operación): se afirma que NO cambia entre consultas, no su valor.
"""

from __future__ import annotations

import json
import tempfile
import threading
import unittest
from datetime import timedelta
from pathlib import Path
from urllib.error import URLError
from urllib.request import Request, urlopen
from uuid import NAMESPACE_URL, uuid5

from .modelo import Proveedor, instante
from .servidor import crear_servidor

SEMILLA = "pasanaku-escenario-h3-s1-m3-v1"
API = "api-escenario-" + "a" * 32
CONTROL = "control-escenario-" + "b" * 32
SECRETO = b"firma-ficticia-del-escenario-publicado-0001"


def ref(nombre: str) -> str:
    """La referencia de cada operación del escenario: derivada de la semilla, igual en cada corrida."""
    return str(uuid5(NAMESPACE_URL, f"{SEMILLA}/{nombre}"))


class EscenarioPublicadoTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.archivo = Path(self.tmp.name) / "escenario.sqlite"
        self._abrir()

    def _abrir(self) -> None:
        self.proveedor = Proveedor(self.archivo, SECRETO)
        self.servidor = crear_servidor("127.0.0.1", 0, self.proveedor, API, CONTROL)
        hilo = threading.Thread(target=self.servidor.serve_forever, daemon=True)
        hilo.start()
        self.base = f"http://127.0.0.1:{self.servidor.server_port}"

    def _cerrar(self) -> None:
        self.servidor.shutdown()
        self.servidor.server_close()

    def tearDown(self) -> None:
        self._cerrar()

    def _post(self, ruta: str, cuerpo: dict, clave_http: str, idempotencia: str | None = None) -> tuple[int, dict]:
        cabeceras = {"Content-Type": "application/json", "Authorization": "Bearer " + clave_http}
        if idempotencia:
            cabeceras["Idempotency-Key"] = idempotencia
        pedido = Request(self.base + ruta, data=json.dumps(cuerpo).encode(), headers=cabeceras)
        with urlopen(pedido, timeout=3) as r:
            return r.status, json.loads(r.read())

    def _crear(self, nombre: str, **extra) -> tuple[int, dict]:
        cuerpo = {"referencia": ref(nombre), "tipo": "RECARGA", "monto": "250.00", "moneda": "BOB", **extra}
        return self._post("/v1/operaciones", cuerpo, API, ref(nombre))

    def test_cronologia_publicada(self) -> None:
        # t=0 · se pide una recarga: queda PENDIENTE, nada se acredita
        estado, salida = self._crear("recarga-A")
        payload_a = salida if "estado" in salida else json.loads(__import__("base64").b64decode(salida["payload"]))
        self.assertEqual((estado, payload_a["estado"]), (201, "PENDIENTE"))
        self.assertEqual(self.proveedor.resumen(), {"operaciones": 1, "confirmadas": 0, "pendientes": 1, "rechazadas": 0})

        # t=0 · el mismo pedido con la MISMA clave devuelve la misma operación (no abre otra)
        self._crear("recarga-A")
        self.assertEqual(self.proveedor.resumen()["operaciones"], 1)

        # t=0 · otro contenido con la misma clave es 409
        with self.assertRaises(Exception) as caido:
            self._post(
                "/v1/operaciones",
                {"referencia": ref("recarga-A"), "tipo": "RECARGA", "monto": "999.00", "moneda": "BOB"},
                API,
                ref("recarga-A"),
            )
        self.assertIn("409", str(caido.exception))

        # t=0 · RESPUESTA_PERDIDA: el proveedor liquida y corta la respuesta de la primera creación
        with self.assertRaises((URLError, ConnectionError, OSError)):
            self._crear("recarga-B", escenario="RESPUESTA_PERDIDA")
        self.assertEqual(self.proveedor.resumen()["confirmadas"], 1)

        # t=+10 s · el backend consulta la B y ve la liquidación ORIGINAL; consultarla otra vez da lo mismo
        self.proveedor.avanzar(10)
        primera = self.proveedor.consultar(ref("recarga-B"))
        segunda = self.proveedor.consultar(ref("recarga-B"))
        self.assertEqual(primera["estado"], "CONFIRMADO")
        self.assertEqual(primera, segunda)
        liquidada_en_b = primera["liquidadaEn"]

        # t=+10 s · reinicio del proceso con el mismo archivo: nada se pierde ni se duplica
        self._cerrar()
        self._abrir()
        self.assertEqual(self.proveedor.consultar(ref("recarga-B"))["liquidadaEn"], liquidada_en_b)
        self.assertEqual(self.proveedor.resumen(), {"operaciones": 2, "confirmadas": 1, "pendientes": 1, "rechazadas": 0})

        # t=+3600 s · la A se confirma a mano: su liquidación lleva el instante del reloj de negocio, 3600 s despues de la B (liquidada en t=0)
        self.proveedor.avanzar(3590)
        confirmada = self.proveedor.resolver(ref("recarga-A"), "CONFIRMADO")
        self.assertEqual(confirmada["estado"], "CONFIRMADO")
        self.assertEqual(instante(confirmada["liquidadaEn"]) - instante(liquidada_en_b), timedelta(seconds=3600))

        # t=+3600 s · estado terminal: no retrocede, y repetir la misma resolución devuelve lo mismo
        self.assertEqual(self.proveedor.resolver(ref("recarga-A"), "CONFIRMADO"), confirmada)
        with self.assertRaisesRegex(Exception, "TERMINAL"):
            self.proveedor.resolver(ref("recarga-A"), "RECHAZADO")
        self.assertEqual(self.proveedor.resumen(), {"operaciones": 2, "confirmadas": 2, "pendientes": 0, "rechazadas": 0})

    def test_misma_semilla_mismas_referencias(self) -> None:
        self.assertEqual(ref("recarga-A"), str(uuid5(NAMESPACE_URL, f"{SEMILLA}/recarga-A")))
        self.assertNotEqual(ref("recarga-A"), ref("recarga-B"))


if __name__ == "__main__":
    unittest.main()
