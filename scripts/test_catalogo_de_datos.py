#!/usr/bin/env python3
"""
Pruebas del generador del catálogo de datos (la semilla que sirve `auditoria`).

    python3 -I scripts/test_catalogo_de_datos.py      (desde la raíz del repo)

No usan dobles: corren contra el modelo y el DDL reales del repo, que son lo que el catálogo tiene que describir.
"""

import pathlib
import re
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

import catalogo_de_datos as cat  # noqa: E402


class CatalogoDeDatosTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalogo = cat.construir()
        cls.por_nombre = {(t["esquema"], t["tabla"]): t for t in cls.catalogo["tablas"]}

    def test_esquemas_son_objetos_con_sus_cifras(self):
        detalle = {e["nombre"]: e for e in self.catalogo["detalleDeEsquemas"]}
        self.assertEqual(sorted(detalle), self.catalogo["esquemas"])
        aportes = detalle["aportes"]
        self.assertEqual(aportes["servicio"], "aportes")
        self.assertTrue(aportes["descripcion"])
        self.assertEqual(aportes["tablas"], sum(1 for t in self.catalogo["tablas"] if t["esquema"] == "aportes"))
        self.assertNotIn("servicio", detalle["catalogo"], "el esquema compartido no tiene servicio dueño")
        self.assertEqual(sum(e["tablas"] for e in detalle.values()), len(self.catalogo["tablas"]))

    def test_relaciones_coinciden_con_el_ddl(self):
        texto = "".join(f.read_text(encoding="utf-8") for f in sorted((cat.SQL / "20_claves").glob("*.sql")))
        en_ddl = {m[3]: m[8] for m in cat.FK_DDL.finditer(texto)}
        salientes = [r for t in self.catalogo["tablas"] for r in t["relaciones"]["salientes"]]
        self.assertEqual(len(salientes), len(en_ddl), "cada FK del modelo es una restricción del DDL y viceversa")
        for r in salientes:
            self.assertEqual(en_ddl[r["restriccion"]], r["alBorrar"], r["restriccion"])
            self.assertEqual(r["columnaDestino"], "id")
            self.assertIn(r["cardinalidad"], ("N:1", "1:1"))

    def test_al_borrar_sigue_la_nulabilidad(self):
        for t in self.catalogo["tablas"]:
            for r in t["relaciones"]["salientes"]:
                self.assertEqual(r["alBorrar"], "RESTRICT" if r["obligatoria"] else "SET NULL", r["restriccion"])

    def test_entrante_es_la_saliente_vista_desde_el_otro_lado(self):
        pago = self.por_nombre[("aportes", "pago")]
        entrante = next(r for r in pago["relaciones"]["entrantes"] if r["tabla"] == "constancia_pago")
        constancia = self.por_nombre[("aportes", "constancia_pago")]
        saliente = next(r for r in constancia["relaciones"]["salientes"] if r["tabla"] == "pago")
        self.assertEqual(entrante["restriccion"], saliente["restriccion"])
        self.assertEqual(entrante["cardinalidad"], saliente["cardinalidad"])

    def test_columna_unica_vuelve_la_relacion_uno_a_uno(self):
        unicas = cat.unicas_del_ddl()
        for t in self.catalogo["tablas"]:
            for r in t["relaciones"]["salientes"]:
                es_unica = (t["esquema"], t["tabla"], r["columna"]) in unicas
                self.assertEqual(r["cardinalidad"], "1:1" if es_unica else "N:1", r["restriccion"])

    def test_columna_fk_trae_el_esquema_destino(self):
        pago = self.por_nombre[("aportes", "pago")]
        obligacion = next(c for c in pago["columnas"] if c["nombre"] == "obligacion_id")
        self.assertEqual((obligacion["referencia"], obligacion["esquemaReferencia"]), ("obligacion_aporte", "aportes"))

    def test_clasificacion_por_tabla_cuenta_las_columnas_sensibles(self):
        for t in self.catalogo["tablas"]:
            esperado = sum(1 for c in t["columnas"] if c["sensibilidad"] == "FINANCIERO")
            self.assertEqual(t["clasificacion"]["financieros"], esperado)
        self.assertGreater(self.por_nombre[("aportes", "pago")]["clasificacion"]["financieros"], 0)

    def test_tablas_de_infraestructura_estan_y_se_distinguen(self):
        texto = cat.INFRA_SQL.read_text(encoding="utf-8")
        en_ddl = set(re.findall(r"CREATE TABLE IF NOT EXISTS (\w+)\.(\w+) \(", texto))
        infra = {(t["esquema"], t["tabla"]) for t in self.catalogo["tablas"] if t["tipo"] == "INFRAESTRUCTURA"}
        self.assertEqual(infra, en_ddl)
        outbox = self.por_nombre[("aportes", "evento_dominio")]
        self.assertTrue(outbox["negocio"]["queEs"])
        self.assertIn("tipo", [c["nombre"] for c in outbox["columnas"]])
        consumo = self.por_nombre[("aportes", "evento_consumido")]
        self.assertEqual([c["nombre"] for c in consumo["columnas"] if c["pk"]], ["id_evento", "consumidor"])

    def test_vineta_con_varias_columnas_explica_a_cada_una(self):
        ficha = cat._ficha("### `X` / `x`\n- `montoA` y `montoB`: lo que se explica.\n- `esManual` + `registradoPor`: otra.\n")
        self.assertEqual(ficha["columnas"]["montoA"], "lo que se explica.")
        self.assertEqual(ficha["columnas"]["montoB"], "lo que se explica.")
        self.assertEqual(ficha["columnas"]["registradoPor"], "otra.")

    def test_orden_determinista(self):
        claves = [(t["esquema"], t["tabla"]) for t in self.catalogo["tablas"]]
        self.assertEqual(claves, sorted(claves))
        self.assertEqual(len(claves), len(set(claves)))


if __name__ == "__main__":
    unittest.main()
