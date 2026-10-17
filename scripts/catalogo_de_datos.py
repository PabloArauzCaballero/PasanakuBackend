#!/usr/bin/env python3
"""
Catálogo de datos: cada esquema, tabla y columna con su explicación de negocio y de sistema.

    python3 -I scripts/catalogo_de_datos.py --tabla obligacion_aporte   (una ficha, para leerla)
    python3 -I scripts/catalogo_de_datos.py --escribir                  (docs/catalogo-de-datos.md + JSON)

No hay una fuente nueva de narrativa. La de NEGOCIO ya está escrita a mano en las fichas de
`docs/entidades/*.md` («Qué es», «Para qué sirve (negocio)», «Por qué debe existir»); ponerla
además en el `.puml` dejaría dos verdades (regla 97.1.1). La de SISTEMA no se escribe: se deriva
del modelo (servicio dueño, append-only, claves, reglas nombradas), así no puede quedar vieja.

Columnas: la nota de la ficha cuando existe; si no, la convención del proyecto para los nombres
que se repiten en todo el modelo; y siempre la descripción estructural. La cobertura de columnas
con explicación de negocio se mide y no puede bajar (`verificar_narrativas.py`).
"""

import argparse
import json
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

import modelo  # noqa: E402
from catalogo_reglas import reglas_por_tabla  # noqa: E402

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

DOCS = pathlib.Path("docs")
SALIDA_MD = DOCS / "catalogo-de-datos.md"
# No va en `apps/backoffice/public/`: un archivo estático se sirve sin sesión y publicaría el esquema completo.
# Lo sirve `auditoria` en GET /catalogo-de-datos, con TABLERO_SISTEMAS_VER (D14 del trabajo de tableros).
SALIDA_JSON = pathlib.Path("servicios/auditoria/src/main/resources/catalogo/catalogo-datos.json")

# Las fichas escriben la etiqueta con matices («**Por qué debe existir, y por qué no es lo mismo…**:»):
# se reconoce el comienzo y se tolera el resto hasta el cierre del negrita.
ETIQUETAS = {
    "queEs": r"Qué (?:es|son)",
    "negocio": r"Para qué sirven? \(negocio\)",
    "porQue": r"Por qué (?:deben?|son)\b",
    "sistema": r"A nivel de sistema",
}

# Convenciones del proyecto para columnas que se repiten en todo el modelo. Cada una cita la regla
# que la fija; un nombre que significa cosas distintas según la tabla (`version`, `tipo`, `estado`)
# NO va acá: su explicación es la de su ficha o la estructural.
CONVENCIONES = {
    "id": "Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.",
    "moneda": "Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).",
    "clave_idempotencia": "Clave que vuelve idempotente la operación: un reintento con la misma clave no "
    "repite el efecto (regla 91.1.4).",
    "vigente_desde": "Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.",
    "vigente_hasta": "Hasta cuándo rigió esta versión; vacía mientras sigue vigente.",
}

# Vocabulario de `SalidasSensibles.java` (comun-pruebas): el mismo que impide que esos datos salgan
# en logs y URLs decide acá qué columna es dato personal.
PERSONAL = re.compile(r"telefono|celular|correo|email|documento|apellido|direccion|nombre_completo|"
                      r"fecha_nacimiento|nit|iban|numero_cuenta|url_anverso|url_reverso|selfie")
SECRETO = re.compile(r"password|contrasena|secreto|secret|pimienta|otp|token|clave(?!_idempotencia)|cvv")
FINANCIERO = re.compile(r"monto|saldo|importe|bolsa|deuda")


def fichas():
    """{tabla: {queEs, negocio, porQue, sistema, columnas: {nombre_o_camel: texto}}} desde los .md.

    Dos formas de ficha conviven. La de la mayoría de los módulos: un `### `Clase` / `tabla`` por tabla. La
    de los módulos 13 y 14: un `##` por grupo de tablas, con una fila `| `tabla` | Qué es |` por tabla y los
    párrafos del grupo («Para qué sirve», «Por qué debe existir», «A nivel de sistema») que valen para todas.
    La ficha propia de la tabla gana sobre la del grupo.
    """
    resultado, de_grupo = {}, {}
    for md in sorted((DOCS / "entidades").glob("[0-9][0-9]_*.md")):
        texto = md.read_text(encoding="utf-8")
        for grupo in re.split(r"^(?=## )", texto, flags=re.M):
            partes = re.split(r"^(?=### )", grupo, flags=re.M)
            for seccion in partes[1:]:
                tablas = re.findall(r"`([a-z][a-z0-9_]*)`", seccion.split("\n", 1)[0])
                ficha = _ficha(seccion)
                for t in tablas:
                    resultado.setdefault(t, ficha)
            _fichas_de_grupo(partes[0], de_grupo)
    for t, ficha in de_grupo.items():
        resultado.setdefault(t, ficha)
    return resultado


def _fichas_de_grupo(seccion, destino):
    comun = _ficha(seccion)
    # La definición de cada tabla del grupo: una fila `| `tabla` | Qué es |` o una viñeta `- **`tabla`** — …`.
    filas = re.finditer(r"^\|\s*((?:`[a-z][a-z0-9_]*`\s*/?\s*)+)\|\s*(.+?)\s*\|\s*$", seccion, re.M)
    vinetas = re.finditer(r"^[-*]\s+\*\*((?:`[a-z][a-z0-9_]*`\s*/?\s*)+)\*\*\s*[—–-]\s*(.+(?:\n\s{2,}\S.*)*)",
                          seccion, re.M)
    for fila in [*filas, *vinetas]:
        for t in re.findall(r"`([a-z][a-z0-9_]*)`", fila.group(1)):
            destino.setdefault(t, {**comun, "queEs": _limpio(fila.group(2)), "columnas": comun["columnas"]})


def _ficha(seccion):
    cuerpo = seccion.split("\n", 1)[1] if "\n" in seccion else ""
    ficha = {"columnas": {}}
    marcas = [(m.start(), m.end(), clave) for clave, rx in ETIQUETAS.items()
              for m in re.finditer(r"^\*\*" + rx + r"[^*]*\*\*:?", cuerpo, re.M)]
    marcas.sort()
    for i, (_, fin, clave) in enumerate(marcas):
        hasta = marcas[i + 1][0] if i + 1 < len(marcas) else len(cuerpo)
        parrafo = _limpio(cuerpo[fin:hasta])
        if parrafo and clave not in ficha:
            ficha[clave] = parrafo
    for m in re.finditer(r"^\s*[-*]\s+`(\w+)`\s*(?:\([^)]*\))?\s*[:—–-]\s*(.+(?:\n\s{2,}\S.*)*)", cuerpo, re.M):
        ficha["columnas"].setdefault(m.group(1), _limpio(m.group(2)))
    return ficha


def _limpio(texto):
    texto = re.sub(r"```.*?```", "", texto, flags=re.S)
    return re.sub(r"\s+", " ", texto).strip()


def sensibilidad(columna):
    n = columna.lower()
    if n.startswith("hash_") or n.endswith("_hash"):
        return "DERIVADO"
    if SECRETO.search(n):
        return "SECRETO"
    if PERSONAL.search(n):
        return "PERSONAL"
    if FINANCIERO.search(n):
        return "FINANCIERO"
    return "NINGUNA"


def construir():
    mods, registro, _ = modelo.cargar()
    narrativas, reglas = fichas(), reglas_por_tabla()
    tablas, entrantes = {}, {}
    for k, d in mods.items():
        for alias in d["orden"]:
            e = d["entidades"][alias]
            tabla = e["tabla"]
            esquema = modelo.esquema_de(tabla, k)
            columnas = []
            for c in e["cols"]:
                destino = modelo.resolver_fk(c, k, registro) if c["fk"] else None
                if destino:
                    entrantes.setdefault(destino, []).append({"tabla": tabla, "columna": c["nombre"]})
                columnas.append(_columna(c, destino, narrativas.get(tabla, {})))
            tablas[tabla] = {
                "tabla": tabla, "esquema": esquema, "modulo": k, "nombreModulo": d["nombre"],
                "servicio": None if esquema in (modelo.ESQUEMA_COMUN, modelo.ESQUEMA_CATALOGO) else modelo.servicio_de(esquema),
                "appendOnly": tabla in modelo.APPEND_ONLY,
                "negocio": {c: narrativas.get(tabla, {}).get(c) for c in ("queEs", "negocio", "porQue")},
                "notaDeSistema": narrativas.get(tabla, {}).get("sistema"),
                "reglas": [r["nombre"] for r in reglas.get(tabla, [])],
                "columnas": columnas,
            }
    for tabla, t in tablas.items():
        salientes = [{"columna": c["nombre"], "tabla": c["referencia"], "esquema": tablas[c["referencia"]]["esquema"]}
                     for c in t["columnas"] if c["referencia"]]
        llegan = [{**r, "esquema": tablas[r["tabla"]]["esquema"]} for r in entrantes.get(tabla, [])]
        t["relaciones"] = {"salientes": salientes, "entrantes": llegan}
        t["sistema"] = _sistema(t)
    return {"esquemas": sorted({t["esquema"] for t in tablas.values()}),
            "tablas": [tablas[n] for n in sorted(tablas, key=lambda n: (tablas[n]["esquema"], n))]}


def _columna(c, destino, ficha):
    n = c["nombre"]
    nota = ficha.get("columnas", {}).get(modelo.a_camel(n)) or ficha.get("columnas", {}).get(n)
    origen = "ficha" if nota else "convencion" if n in CONVENCIONES else None
    partes = [c["tipo"], "opcional" if c["nulo"] else "obligatoria"]
    if c["pk"]:
        partes.append("clave primaria")
    if destino:
        partes.append(f"referencia a `{destino}`")
    if c["generated"]:
        partes.append("calculada por la base, no se escribe")
    return {
        "nombre": n, "tipo": c["tipo"], "nulable": c["nulo"], "pk": c["pk"], "referencia": destino,
        "negocio": nota or CONVENCIONES.get(n), "origenNegocio": origen,
        "sistema": ", ".join(partes) + ".", "sensibilidad": sensibilidad(n),
    }


def _sistema(t):
    if t["esquema"] == modelo.ESQUEMA_COMUN:
        frases = ["Vive en el esquema `comun`: todo servicio INSERTA su rastro y ninguno lee ni edita el de otro; "
                  "el SELECT es solo de `rol_auditor` (excepción enumerada en `COMPARTIDAS_ESCRITURA`)."]
    elif t["esquema"] == modelo.ESQUEMA_CATALOGO:
        escritor = modelo.CATALOGO_ESCRITOR.get(t["tabla"])
        frases = ["Vive en el esquema `catalogo`: todos los servicios la consultan; "
                  + (f"solo `{escritor}` la escribe." if escritor else "se carga con las semillas.")]
    else:
        frases = [f"Vive en el esquema `{t['esquema']}`; la escribe y la lee solo el servicio `{t['servicio']}` "
                  "(regla 98.1.1): otro servicio la conoce por su API o por sus eventos."]
    if t["appendOnly"]:
        frases.append("Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva.")
    # ADR-017: las FK entre esquemas existen (un solo cluster); el esquema se nombra cuando es otro.
    def nombre(r):
        return f"`{r['tabla']}`" if r["esquema"] == t["esquema"] else f"`{r['esquema']}.{r['tabla']}`"
    if t["relaciones"]["salientes"]:
        frases.append("Depende de " + ", ".join(sorted({nombre(r) for r in t["relaciones"]["salientes"]})) + ".")
    if t["relaciones"]["entrantes"]:
        frases.append("La referencian " + ", ".join(sorted({nombre(r) for r in t["relaciones"]["entrantes"]})) + ".")
    if t["reglas"]:
        frases.append("Reglas nombradas: " + ", ".join(f"`{r}`" for r in t["reglas"]) + ".")
    return " ".join(frases)


def markdown(catalogo):
    L = ["# Catálogo de datos", "",
         "> Generado por `scripts/catalogo_de_datos.py --escribir`. No se edita a mano: la narrativa de negocio",
         "> se corrige en `docs/entidades/*.md` y la estructura en los `.puml`.", ""]
    for esquema in catalogo["esquemas"]:
        L += [f"## Esquema `{esquema}`", ""]
        for t in (t for t in catalogo["tablas"] if t["esquema"] == esquema):
            L += [f"### `{t['tabla']}`", ""]
            for clave, titulo in (("queEs", "Qué es"), ("negocio", "Para qué sirve"), ("porQue", "Por qué existe")):
                if t["negocio"][clave]:
                    L.append(f"**{titulo}.** {t['negocio'][clave]}")
                    L.append("")
            L += [f"**Sistema.** {t['sistema']}", "", "| Columna | Tipo | Negocio | Sistema | Sensibilidad |",
                  "|---|---|---|---|---|"]
            for c in t["columnas"]:
                negocio = (c["negocio"] or "—").replace("|", "\\|")
                L.append(f"| `{c['nombre']}` | {c['tipo']} | {negocio} | {c['sistema']} | {c['sensibilidad']} |")
            L.append("")
    return "\n".join(L)


def _sin_nulos(valor):
    """El contrato (`CatalogoDeDatos`, auditoria.yaml) declara opcional lo que puede faltar: se omite, no viaja null."""
    if isinstance(valor, dict):
        return {k: _sin_nulos(v) for k, v in valor.items() if v is not None}
    if isinstance(valor, list):
        return [_sin_nulos(v) for v in valor]
    return valor


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--tabla")
    p.add_argument("--escribir", action="store_true")
    a = p.parse_args()
    catalogo = construir()
    if a.tabla:
        t = next((t for t in catalogo["tablas"] if t["tabla"] == a.tabla), None)
        if t is None:
            raise SystemExit(f"No existe la tabla {a.tabla} en el modelo")
        print(json.dumps(t, ensure_ascii=False, indent=2))
    if a.escribir:
        SALIDA_JSON.parent.mkdir(parents=True, exist_ok=True)
        SALIDA_MD.write_text(markdown(catalogo) + "\n", encoding="utf-8")
        SALIDA_JSON.write_text(json.dumps(_sin_nulos(catalogo), ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        print(f"{len(catalogo['tablas'])} tablas → {SALIDA_MD} y {SALIDA_JSON}")


if __name__ == "__main__":
    main()
