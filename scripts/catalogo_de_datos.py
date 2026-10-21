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
        parrafo = _narrativa(cuerpo[fin:hasta])
        if parrafo and clave not in ficha:
            ficha[clave] = parrafo
    # Una viñeta explica una columna o varias a la vez: «- `montoComisionProveedor` y `montoNetoAcreditado`: …».
    # Antes solo se reconocía la de un nombre y las de varios se perdían enteras.
    for m in re.finditer(r"^\s*[-*]\s+((?:`\w+`\s*(?:,|/|\+|\by\b|\be\b|\bo\b)?\s*)+)(?:\([^)]*\))?\s*[:—–-]\s*"
                         r"(.+(?:\n\s{2,}\S.*)*)", cuerpo, re.M):
        for nombre in re.findall(r"`(\w+)`", m.group(1)):
            ficha["columnas"].setdefault(nombre, _limpio(m.group(2)))
    return ficha


def _narrativa(texto):
    """Un párrafo de la ficha sin perder su forma: los párrafos se separan con una línea en blanco y cada viñeta
    queda en su renglón (`- …` o `1. …`). Antes se aplanaba todo en un solo párrafo y las listas se leían como texto
    corrido. Las líneas cortadas a mano y las continuaciones sangradas de una viñeta se juntan."""
    texto = re.sub(r"```.*?```", "", texto, flags=re.S)
    renglones = []
    for linea in texto.splitlines():
        # Una línea en blanco o un separador de markdown (`---`) corta el párrafo; el separador no es texto.
        if not linea.strip() or re.fullmatch(r"\s*([-*_])(\s*\1){2,}\s*", linea):
            renglones.append(None)
        elif v := re.match(r"^\s*([-*]|\d+\.)\s+(.*)$", linea):
            renglones.append(["-" if v[1] in "-*" else v[1], v[2]])
        elif renglones and renglones[-1] is not None:
            renglones[-1][1] += " " + linea
        else:
            renglones.append(["", linea])
    salida, blanco = "", False
    for r in renglones:
        if r is None:
            blanco = bool(salida)
            continue
        renglon = (f"{r[0]} " if r[0] else "") + re.sub(r"\s+", " ", r[1]).strip()
        salida += (("\n\n" if blanco else "\n") + renglon) if salida else renglon
        blanco = False
    return salida


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
    claves, unicas = claves_foraneas_del_ddl(), unicas_del_ddl()
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
                columnas.append(_columna(c, destino, narrativas.get(tabla, {}), (esquema, tabla, c["nombre"]) in unicas))
            tablas[tabla] = {
                "tabla": tabla, "esquema": esquema, "tipo": "NEGOCIO", "modulo": k, "nombreModulo": d["nombre"],
                "servicio": None if esquema in (modelo.ESQUEMA_COMUN, modelo.ESQUEMA_CATALOGO) else modelo.servicio_de(esquema),
                "appendOnly": tabla in modelo.APPEND_ONLY,
                "negocio": {c: narrativas.get(tabla, {}).get(c) for c in ("queEs", "negocio", "porQue")},
                "notaDeSistema": narrativas.get(tabla, {}).get("sistema"),
                "reglas": [r["nombre"] for r in reglas.get(tabla, [])],
                "columnas": columnas,
            }
    for c in (c for t in tablas.values() for c in t["columnas"] if c["referencia"]):
        c["esquemaReferencia"] = tablas[c["referencia"]]["esquema"]
    for tabla, t in tablas.items():
        salientes = [_relacion(t["esquema"], tabla, c["nombre"], c["referencia"], tablas[c["referencia"]]["esquema"],
                               not c["nulable"], c.get("unica", False), claves) for c in t["columnas"] if c["referencia"]]
        llegan = []
        for r in entrantes.get(tabla, []):
            origen = tablas[r["tabla"]]
            col = next(c for c in origen["columnas"] if c["nombre"] == r["columna"])
            llegan.append(_relacion(origen["esquema"], r["tabla"], r["columna"], r["tabla"], origen["esquema"],
                                    not col["nulable"], col.get("unica", False), claves))
        t["relaciones"] = {"salientes": salientes, "entrantes": llegan}
        t["sistema"] = _sistema(t)
        t["clasificacion"] = _clasificacion(t["columnas"])
    operaciones = operaciones_por_tabla(set(tablas))
    for tabla, t in tablas.items():
        t["operaciones"] = operaciones.get(tabla, [])
    for t in tablas_de_infraestructura():
        tablas[f"{t['esquema']}.{t['tabla']}"] = {**t, "operaciones": []}
    ordenadas = [tablas[n] for n in sorted(tablas, key=lambda n: (tablas[n]["esquema"], tablas[n]["tabla"]))]
    return {"esquemas": sorted({t["esquema"] for t in ordenadas}),
            "detalleDeEsquemas": _esquemas(ordenadas),
            "tablas": ordenadas}


MATRIZ = DOCS / "catalogo-de-flujos" / "matriz.md"
CASOS = DOCS / "CasosDeUso"
FILA_MATRIZ = re.compile(r"^\| ([\w-]+) \| (GET|POST|PUT|PATCH|DELETE) \| `([^`]+)` \| `(\w+)` \| [^|]+\| (CU-\d+) \|", re.M)


def operaciones_por_tabla(tablas_de_negocio):
    """Qué casos de uso nombran cada tabla y con qué endpoints se ejecutan (la pestaña «Operaciones» de Atlas).

    No se infiere qué endpoint escribe qué tabla: se encadenan dos fuentes que ya existen. Tabla → caso de uso: el
    `[[tabla]]` que cada `docs/CasosDeUso/CU-*.md` enlaza. Caso de uso → endpoint: la columna CU de
    `docs/catalogo-de-flujos/matriz.md` (generada por `matriz_de_pruebas.py` desde el contrato y la traza).
    """
    endpoints = {}
    for m in FILA_MATRIZ.finditer(MATRIZ.read_text(encoding="utf-8")):
        endpoints.setdefault(int(m[5].split("-")[1]), []).append(
            {"servicio": m[1], "metodo": m[2], "ruta": m[3], "operationId": m[4]})
    por_tabla = {}
    for archivo in sorted(CASOS.glob("CU-*.md"), key=lambda a: int(a.stem.split()[0].split("-")[1])):
        codigo, titulo = archivo.stem.split(" ", 1)
        nombradas = {e.strip() for e in re.findall(r"\[\[([^\]|#]+)", archivo.read_text(encoding="utf-8"))}
        for tabla in sorted(nombradas & tablas_de_negocio):
            por_tabla.setdefault(tabla, []).append(
                {"casoDeUso": codigo, "titulo": titulo, "endpoints": endpoints.get(int(codigo.split("-")[1]), [])})
    return por_tabla


def _columna(c, destino, ficha, unica=False):
    n = c["nombre"]
    nota = ficha.get("columnas", {}).get(modelo.a_camel(n)) or ficha.get("columnas", {}).get(n)
    origen = "ficha" if nota else "convencion" if n in CONVENCIONES else None
    partes = [c["tipo"], "opcional" if c["nulo"] else "obligatoria"]
    if c["pk"]:
        partes.append("clave primaria")
    if unica:
        partes.append("única")
    if destino:
        partes.append(f"referencia a `{destino}`")
    if c["generated"]:
        partes.append("calculada por la base, no se escribe")
    columna = {
        "nombre": n, "tipo": c["tipo"], "nulable": c["nulo"], "pk": c["pk"], "referencia": destino,
        "negocio": nota or CONVENCIONES.get(n), "origenNegocio": origen,
        "sistema": ", ".join(partes) + ".", "sensibilidad": sensibilidad(n),
    }
    if unica:
        columna["unica"] = True
    return columna


# --- relaciones: lo que la base aplica de verdad ------------------------------
# La relación sale del modelo (resolver_fk), pero su restricción y su acción al borrar se leen del DDL que se
# aplica (`sql/20_claves`, generado por generar_ddl.py): así el catálogo dice lo que la base hace, no lo que se
# supone. Una FK del modelo sin su restricción en el DDL es deriva (regla 97.2) y corta la generación.
SQL = pathlib.Path("sql")
FK_DDL = re.compile(r"ALTER TABLE (\w+)\.(\w+)\s+ADD CONSTRAINT (\w+)\s+FOREIGN KEY \((\w+)\) REFERENCES (\w+)\.(\w+) "
                    r"\((\w+)\) ON DELETE (RESTRICT|SET NULL|CASCADE|NO ACTION)")
UNICO_DDL = re.compile(r"CREATE UNIQUE INDEX IF NOT EXISTS \w+\s+ON (\w+)\.(\w+) \((\w+)\);")


def claves_foraneas_del_ddl():
    """{(esquema, tabla, columna): {restriccion, esquemaDestino, tablaDestino, columnaDestino, alBorrar}}."""
    claves = {}
    for f in sorted((SQL / "20_claves").glob("*.sql")):
        for m in FK_DDL.finditer(f.read_text(encoding="utf-8")):
            claves[(m[1], m[2], m[4])] = {"restriccion": m[3], "esquemaDestino": m[5], "tablaDestino": m[6],
                                          "columnaDestino": m[7], "alBorrar": m[8]}
    return claves


def unicas_del_ddl():
    """Columnas con un índice UNIQUE de una sola columna: la FK que vive en ellas es 1:1, no N:1."""
    return {(m[1], m[2], m[3]) for f in sorted((SQL / "30_indices").glob("*.sql"))
            for m in UNICO_DDL.finditer(f.read_text(encoding="utf-8"))}


def _relacion(esquema_origen, tabla_origen, columna, tabla, esquema, obligatoria, unica, claves):
    """Una relación vista desde la tabla de la ficha. `tabla`/`esquema` son el otro extremo; la FK siempre vive en
    `tabla_origen.columna`, así que la restricción se busca ahí en las dos direcciones."""
    fk = claves.get((esquema_origen, tabla_origen, columna))
    if fk is None:
        raise SystemExit(f"Deriva: la FK {esquema_origen}.{tabla_origen}.{columna} del modelo no está en sql/20_claves "
                         "(correr scripts/generar_ddl.py)")
    return {"columna": columna, "tabla": tabla, "esquema": esquema, "columnaDestino": fk["columnaDestino"],
            "restriccion": fk["restriccion"], "alBorrar": fk["alBorrar"],
            "cardinalidad": "1:1" if unica else "N:1", "obligatoria": obligatoria}


def _clasificacion(columnas):
    """Cuántas columnas de cada sensibilidad tiene la tabla: es lo que el catálogo filtra y cuenta sin abrirla."""
    cuenta = {"personales": "PERSONAL", "financieros": "FINANCIERO", "secretos": "SECRETO", "derivados": "DERIVADO"}
    return {k: sum(1 for c in columnas if c["sensibilidad"] == v) for k, v in cuenta.items()}


# --- infraestructura de mensajería (ADR-027/028) -------------------------------
# generar_ddl.py inyecta en cada esquema de servicio el outbox, la marca de consumo, el candado de trabajos y,
# en los orquestadores, el estado de saga. No están en los .puml (son plantilla), pero son tablas reales de la base:
# un catálogo que no las lista dice que el esquema tiene menos de lo que tiene. Se leen del DDL que se aplica.
INFRA_SQL = SQL / "15_infra" / "mensajeria.sql"
BORRADORES_SQL = SQL / "15_infra" / "borradores.sql"
INFRA_ARCHIVOS = (INFRA_SQL, BORRADORES_SQL)
# Cómo se presenta cada archivo de infraestructura: el módulo y quién la escribe.
INFRA_ORIGEN = {
    INFRA_SQL: ("Infraestructura de mensajería", "Infraestructura del servicio `{s}`, generada por plantilla en cada "
                "esquema (ADR-027): la escribe y la lee solo ese servicio."),
    BORRADORES_SQL: ("Borradores de onboarding", "Tabla propia del servicio `{s}` fuera del modelo de entidades: la "
                     "genera `generar_ddl.py` desde el contrato `BorradorOnboarding` y el repositorio que la usa."),
}
INFRA_NEGOCIO = {
    "evento_dominio": "Garantiza que ningún hecho del servicio se pierda entre la base y la mensajería: el evento se "
    "guarda en la misma transacción que el cambio y un relevo lo publica después (regla 98.3.3).",
    "evento_consumido": "Hace idempotente el consumo: la entrega es al menos una vez y un mismo evento no puede "
    "producir dos efectos (regla 98.3.4).",
    "shedlock": "Evita que dos réplicas del servicio ejecuten el mismo trabajo programado a la vez (ADR-018).",
    "estado_saga": "Persiste el paso de una saga que cruza servicios para poder reanudarla o compensarla si algo "
    "falla en el medio (regla 98.3.2, ADR-028).",
    "borrador_onboarding": "Permite retomar un alta interrumpida sin volver a cargar todo: guarda lo que la persona "
    "llenó después de confirmar su correo, vence a los siete días y se borra al completar el alta. Guarda datos "
    "personales en claro mientras el alta no termina.",
}
COLUMNA_DDL = re.compile(r"^\s+(\w+)\s+(UUID|VARCHAR\(\d+\)|JSONB|TIMESTAMPTZ|DATE|SMALLINT|INTEGER|BIGINT|TEXT|BOOLEAN)(?![\w(])"
                         r"(.*)$")


def tablas_de_infraestructura():
    return [t for archivo in INFRA_ARCHIVOS for t in _tablas_de_infraestructura(archivo)]


def _tablas_de_infraestructura(archivo):
    texto = archivo.read_text(encoding="utf-8")
    modulo, sistema = INFRA_ORIGEN[archivo]
    comentarios = {(e, t): c for e, t, c in re.findall(r"COMMENT ON TABLE (\w+)\.(\w+) IS '([^']*)'", texto)}
    de_columna = {(e, t, c): v for e, t, c, v in re.findall(r"COMMENT ON COLUMN (\w+)\.(\w+)\.(\w+) IS '([^']*)'", texto)}
    tablas = []
    for m in re.finditer(r"CREATE TABLE IF NOT EXISTS (\w+)\.(\w+) \((.*?)\n\);", texto, re.S):
        esquema, tabla, cuerpo = m[1], m[2], m[3]
        pk_compuesta = re.search(r"PRIMARY KEY \(([^)]*)\)", cuerpo)
        en_pk = {n.strip() for n in pk_compuesta[1].split(",")} if pk_compuesta else set()
        columnas = []
        for linea in cuerpo.splitlines():
            c = COLUMNA_DDL.match(linea)
            if not c:
                continue
            pk = "PRIMARY KEY" in c[3] or c[1] in en_pk
            nulable = not pk and "NOT NULL" not in c[3]
            columnas.append({
                "nombre": c[1], "tipo": c[2], "nulable": nulable, "pk": pk, "referencia": None,
                "negocio": de_columna.get((esquema, tabla, c[1])) or CONVENCIONES.get(c[1]),
                "origenNegocio": "ddl" if (esquema, tabla, c[1]) in de_columna else ("convencion" if c[1] in CONVENCIONES else None),
                "sistema": ", ".join([c[2], "opcional" if nulable else "obligatoria"] + (["clave primaria"] if pk else []))
                + ".", "sensibilidad": sensibilidad(c[1]),
            })
        tablas.append({
            "tabla": tabla, "esquema": esquema, "tipo": "INFRAESTRUCTURA", "modulo": "00",
            "nombreModulo": modulo, "servicio": modelo.servicio_de(esquema),
            "appendOnly": False,
            "negocio": {"queEs": comentarios.get((esquema, tabla)), "negocio": INFRA_NEGOCIO.get(tabla), "porQue": None},
            "notaDeSistema": None, "reglas": [], "columnas": columnas,
            "relaciones": {"salientes": [], "entrantes": []},
            "sistema": sistema.format(s=modelo.servicio_de(esquema)),
            "clasificacion": _clasificacion(columnas),
        })
    return tablas


def _esquemas(tablas):
    """Cada esquema con su servicio dueño, de qué trata y sus cifras: lo que el catálogo muestra antes de las tablas."""
    modulo_de = {v: k for k, v in modelo.ESQUEMA.items()}
    resultado = []
    for nombre in sorted({t["esquema"] for t in tablas}):
        propias = [t for t in tablas if t["esquema"] == nombre]
        if nombre == modelo.ESQUEMA_CATALOGO:
            servicio, descripcion = None, ("Catálogos compartidos: todos los servicios los consultan y solo su "
                                           "dueño declarado los escribe (ADR-029).")
        elif nombre == modelo.ESQUEMA_COMUN:
            servicio, descripcion = None, ("Bitácoras transversales append-only: todo servicio inserta su rastro y "
                                           "solo el rol auditor las lee.")
        else:
            k = modulo_de[nombre]
            servicio, descripcion = modelo.servicio_de(nombre), f"{modelo.MODULOS[k][0]}. {modelo.FOCO[k]}."
        esquema = {
            "nombre": nombre, "descripcion": descripcion,
            "tablas": len(propias),
            "tablasDeNegocio": sum(1 for t in propias if t["tipo"] == "NEGOCIO"),
            "columnas": sum(len(t["columnas"]) for t in propias),
            "relaciones": sum(len(t["relaciones"]["salientes"]) for t in propias),
            "appendOnly": sum(1 for t in propias if t["appendOnly"]),
            "conDatosPersonales": sum(1 for t in propias if t["clasificacion"]["personales"]),
        }
        if servicio:
            esquema["servicio"] = servicio
        resultado.append(esquema)
    return resultado


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
            if t["operaciones"]:
                L += ["**Casos de uso que la nombran.** " + " · ".join(
                    f"{o['casoDeUso']} {o['titulo']}" + (f" ({', '.join('`' + e['operationId'] + '`' for e in o['endpoints'])})"
                                                         if o["endpoints"] else "") for o in t["operaciones"]), ""]
            L += [f"**Sistema.** {t['sistema']}", "", "| Columna | Tipo | Negocio | Sistema | Sensibilidad |",
                  "|---|---|---|---|---|"]
            for c in t["columnas"]:
                negocio = (c["negocio"] or "—").replace("|", "\\|")
                L.append(f"| `{c['nombre']}` | {c['tipo']} | {negocio} | {c['sistema']} | {c['sensibilidad']} |")
            L.append("")
    return "\n".join(L)


def _sin_nulos(valor):
    """El contrato (`CatalogoDeDatos`, auditoria.yaml) declara opcional lo que puede faltar: se omite, no viaja null.

    Y viaja en texto plano: la pantalla no interpreta Markdown, así que las marcas de las fichas (`**`, comillas
    invertidas) se verían en crudo (prueba visual de H9). El `.md` del catálogo sí las conserva.
    """
    if isinstance(valor, dict):
        return {k: _sin_nulos(v) for k, v in valor.items() if v is not None}
    if isinstance(valor, list):
        return [_sin_nulos(v) for v in valor]
    if isinstance(valor, str):
        return re.sub(r"\*\*|`", "", valor)
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
