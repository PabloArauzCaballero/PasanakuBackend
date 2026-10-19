#!/usr/bin/env python3
"""
Genera el VOLUMEN sintetico de dev clonando el escenario demo coherente.

    python3 -m venv .venv-volumen && .venv-volumen/bin/pip install -r scripts/requirements-volumen.txt
    .venv-volumen/bin/python scripts/generar_volumen_dev.py          (desde la raiz)
    python3 scripts/generar_semillas.py                              (despues: baja el JSON a sql/61_dev)

Por que clonar y no inventar tabla por tabla: el escenario de `seeders/dev/02..14` ya es
coherente de punta a punta —claves foraneas, CHECK, asientos que cuadran, saldos de billetera
que salen del libro— y eso costo varios carriles conseguirlo. Generar 60 tablas desde cero con
Faker repetiria ese trabajo y lo haria mal. Clonar hereda la coherencia; lo unico que cambia en
cada clon es:

  1. Las CLAVES NATURALES (columnas de `conflicto` y columnas UNIQUE del esquema): se reemplazan
     en TODO el texto del clon —tambien dentro de las subconsultas `$sql`— por una version con el
     numero de clon, asi las referencias entre archivos siguen apuntando a la fila correcta.
  2. Los DATOS DE PERSONA (nombres, apellidos, correo, telefono, nacimiento): Faker con semilla
     fija. Son SINTETICOS y se declaran asi (`origen: SINTETICO_FAKER` en cada archivo, regla 97.6).
  3. Las FECHAS: cada clon se corre `desplazamiento` dias hacia atras, para repartir el volumen
     en los ultimos 12 meses (los tableros miden por rango).

Lo que NO se toca: referencias a catalogos de `minimos/` (no son claves definidas en los archivos
clonados), las cuentas de sistema y las filas compartidas (personal interno, custodia).

Determinista: misma semilla, mismo JSON byte a byte (es la prueba de H2.S2.M1). Solo dev: cada
archivo declara `entorno: dev` y `generar_semillas.py` se niega a mezclarlo con `minimos/`.
"""
import copy
import json
import pathlib
import re
import sys
import zlib

try:
    from faker import Faker
except ImportError:  # pragma: no cover — se instala con scripts/requirements-volumen.txt
    sys.exit("Falta Faker: pip install -r scripts/requirements-volumen.txt")

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

R = pathlib.Path(__file__).resolve().parent.parent
DEV = R / "seeders/dev"
VOLUMEN = R / "seeders/volumen"
SQL = R / "sql"

SEMILLA = 20261016
CLONES = 12
# Los archivos del escenario demo que forman UNA cadena coherente (usuario -> grupo -> aporte ->
# entrega -> asiento). 09 (mora) y 12 (retiro) entran solo en algunos clones: asi los tableros ven
# grupos al dia, con mora y con retiros, no doce copias identicas.
BASE = ["02", "03", "04", "06", "07", "08", "11", "14"]
CON_MORA = "09"
CON_RETIRO = "12"
# Bloques SQL que tocan filas COMPARTIDAS: en un clon repetirian el efecto sobre lo mismo.
SQL_COMPARTIDO = re.compile(r"cuenta_custodia", re.I)
CAMPOS_PERSONA = {"nombres", "apellidos", "correo", "fecha_nacimiento"}
PREFIJO = "volumen-"


def columnas_unicas():
    """tabla -> conjuntos de columnas UNIQUE, leidos del esquema generado (una sola fuente de verdad)."""
    unicas = {}
    patron = re.compile(
        r"CREATE UNIQUE INDEX[^\n]*?\n?\s*ON\s+(?:\w+\.)?(\w+)\s*\(([^)]*)\)|"
        r"ALTER TABLE\s+(?:\w+\.)?(\w+)[^;]*?UNIQUE\s*\(([^)]*)\)|"
        r"CONSTRAINT\s+uq_(\w+?)_\w+\s+UNIQUE\s*\(([^)]*)\)",
        re.I)
    for archivo in list(SQL.glob("30_indices/*.sql")) + list(SQL.glob("40_reglas/*.sql")) + list(
            SQL.glob("10_tablas/*/*.sql")):
        texto = archivo.read_text(encoding="utf-8")
        for m in patron.finditer(texto):
            tabla = m.group(1) or m.group(3) or m.group(5)
            cols = m.group(2) or m.group(4) or m.group(6)
            limpias = [c.strip() for c in cols.split(",") if re.fullmatch(r"\w+", c.strip())]
            if limpias:
                unicas.setdefault(tabla, set()).update(limpias)
    return unicas


def tokens_de(documentos, unicas):
    """Los literales que son CLAVE en las filas de los archivos clonados: lo que hay que renombrar."""
    tokens = set()
    for doc in documentos:
        for bloque in doc["bloques"]:
            tabla = bloque.get("tabla")
            if not tabla:
                continue
            claves = set(c for c in bloque.get("conflicto", []) if re.fullmatch(r"\w+", c))
            claves |= unicas.get(tabla, set())
            claves -= CAMPOS_PERSONA  # esos los pone Faker, no se renombran
            for fila in bloque.get("filas", []):
                for col in claves:
                    v = fila.get(col)
                    if isinstance(v, str) and len(v) >= 4 and not re.fullmatch(r"[A-Z_]+", v):
                        tokens.add(v)
    # Y lo que se usa para BUSCAR una fila definida en estos mismos archivos (`$ref` por una columna sin
    # UNIQUE, como `extracto_bancario.archivo_url`): si no se renombra, el clon encuentra dos filas.
    definidas = {}
    for doc in documentos:
        for bloque in doc["bloques"]:
            for fila in bloque.get("filas", []):
                for col, v in fila.items():
                    if isinstance(v, str):
                        definidas.setdefault((bloque.get("tabla"), col), set()).add(v)

    todos = set().union(*definidas.values()) if definidas else set()

    def refs(valor):
        if isinstance(valor, dict):
            if "$sql" in valor:  # WHERE archivo_url = '...': el literal busca una fila clonada
                for literal in re.findall(r"'([^']{4,})'", valor["$sql"]):
                    if literal in todos and not re.fullmatch(r"[A-Z_]+", literal):
                        tokens.add(literal)
            if "$ref" in valor:
                for k, v in valor.items():
                    if k != "$ref" and isinstance(v, str) and v in definidas.get((valor["$ref"], k), set()):
                        if len(v) >= 4 and not re.fullmatch(r"[A-Z_]+", v):
                            tokens.add(v)
            for v in valor.values():
                refs(v)
        elif isinstance(valor, list):
            for v in valor:
                refs(v)

    for doc in documentos:
        refs(doc["bloques"])
    return sorted(tokens, key=len, reverse=True)


def renombrar(token, n):
    """La clave del clon `n`. Conserva el largo siempre que puede: las columnas tienen tope y el
    validador de semillas lo controla. Ninguna forma resultante puede coincidir con un original
    (llevan `V`/`v` o un prefijo `8` que los originales no tienen)."""
    if "DEMO" in token:
        return token.replace("DEMO", f"V{n:02d}", 1)
    if "demo" in token:
        return token.replace("demo", f"v{n:02d}", 1)
    if token.startswith("+591") and len(token) == 12:
        # telefonos E.164: rango +59179NN.... que ningun seed usa (los de dev van por +59170/+59171)
        return f"+59179{n:02d}{token[-4:]}"
    m = re.fullmatch(r"([A-Z]+-?)(0*)(\d+)", token)
    if m and len(m.group(2)) >= 3:  # USR000001 -> USRV01001 · BOB-0000001 -> BOB-V010001
        cuerpo = m.group(2) + m.group(3)
        return m.group(1) + f"V{n:02d}" + cuerpo[3:]
    if token.isdigit():
        return f"8{n:02d}" + token[3:]
    return f"{token}-V{n:02d}"


def compartidos_con_demo(no_clonados):
    """Literales con `demo` que pertenecen a filas que NO se clonan (custodia, personal interno): esos
    se dejan como estan, porque el clon tiene que seguir apuntando a la fila compartida."""
    vistos = set()

    def juntar(valor):
        if isinstance(valor, dict):
            for v in valor.values():
                juntar(v)
        elif isinstance(valor, list):
            for v in valor:
                juntar(v)
        elif isinstance(valor, str):
            for literal in [valor] + re.findall(r"'([^']+)'", valor):
                if re.search(r"demo", literal, re.I):
                    vistos.add(literal)

    for doc in no_clonados:
        juntar(doc["bloques"])
    return sorted(vistos, key=len, reverse=True)


def renombrar_demo(texto, n, protegidos):
    """`demo`/`DEMO` -> `vNN`/`VNN` en todo literal propio del escenario (hashes de digest, claves de
    idempotencia, URLs de extracto...). Un literal compartido se protege y queda intacto."""
    guardados = {}
    for i, literal in enumerate(protegidos):
        if literal in texto:
            marca = f"\x00{i}\x00"
            guardados[marca] = literal
            texto = texto.replace(literal, marca)
    texto = texto.replace("DEMO", f"V{n:02d}").replace("demo", f"v{n:02d}").replace("Demo", f"V{n:02d}")
    for marca, literal in guardados.items():
        texto = texto.replace(marca, literal)
    return texto


def reemplazar(valor, mapa, desplazamiento):
    if isinstance(valor, dict):
        if "$fecha" in valor:
            m = re.fullmatch(r"\s*([+-]?\d+)\s+days?\s*", valor["$fecha"])
            if m:
                return {"$fecha": f"{int(m.group(1)) - desplazamiento} days"}
            return valor
        if "$sql" in valor:
            texto = valor["$sql"]
            for viejo, nuevo in mapa:
                texto = texto.replace(viejo, nuevo)
            texto = texto.replace("now()", f"(now() - interval '{desplazamiento} days')")
            texto = re.sub(r"\bcurrent_date\b", f"(current_date - {desplazamiento})", texto)
            return {"$sql": texto}
        return {k: reemplazar(v, mapa, desplazamiento) for k, v in valor.items()}
    if isinstance(valor, list):
        return [reemplazar(v, mapa, desplazamiento) for v in valor]
    if isinstance(valor, str):
        for viejo, nuevo in mapa:
            if viejo in valor:
                valor = valor.replace(viejo, nuevo)
        if re.fullmatch(r"\d{4}-\d{2}-\d{2}", valor):
            return valor  # fechas fijas (nacimiento): las pone Faker en las filas de persona
        return valor
    return valor


def personas(fila, faker, n):
    """Datos de persona sinteticos. El correo y el telefono llevan el clon: unicos por construccion."""
    if "nombres" in fila:
        fila["nombres"] = faker.first_name()
    if "apellidos" in fila:
        fila["apellidos"] = f"{faker.last_name()} {faker.last_name()}"
    if "correo" in fila and isinstance(fila["correo"], str):
        usuario = re.sub(r"[^a-z]", "", faker.user_name().lower())[:12] or "persona"
        fila["correo"] = f"{usuario}.v{n:02d}{zlib.crc32(fila['correo'].encode()) % 1000:03d}@volumen.pasanaku.test"
    if "fecha_nacimiento" in fila and isinstance(fila["fecha_nacimiento"], str):
        fila["fecha_nacimiento"] = faker.date_of_birth(minimum_age=21, maximum_age=65).isoformat()


def main():
    faker = Faker("es_MX")  # no hay locale de Bolivia; se declara en el archivo
    faker.seed_instance(SEMILLA)
    unicas = columnas_unicas()

    fuentes = {}
    for archivo in sorted(DEV.glob("*.json")):
        prefijo = archivo.name[:2]
        if prefijo in BASE + [CON_MORA, CON_RETIRO]:
            fuentes[prefijo] = json.loads(archivo.read_text(encoding="utf-8"))
    tokens = tokens_de(fuentes.values(), unicas)
    no_clonados = [json.loads(p.read_text(encoding="utf-8")) for p in sorted(DEV.glob("*.json"))
                   if p.name != "manifiesto.json" and p.name[:2] not in fuentes]
    protegidos = compartidos_con_demo(no_clonados)

    VOLUMEN.mkdir(exist_ok=True)
    for viejo in VOLUMEN.glob(f"{PREFIJO}*.json"):
        viejo.unlink()

    escritos = []
    for n in range(1, CLONES + 1):
        desplazamiento = 30 * (n - 1) + 5  # un clon por mes, el mas reciente hace 5 dias
        # El escenario va COMPLETO en cada clon: los asientos de 11 apuntan a las transacciones de 09 y 12,
        # y un clon sin ellos dejaria asientos huerfanos. La variedad la dan las fechas y las personas.
        incluye = BASE + [CON_MORA, CON_RETIRO]
        mapa = [(t, renombrar(t, n)) for t in tokens]
        bloques = []
        if n == 1:
            # La misma serie SINTETICA de dev/01 (0.143678, fuente MANUAL), extendida a lo que cubre el volumen:
            # sin tipo de cambio del dia, R-UIF-04 rechaza la operacion al confirmar (fn_fx_a_usd).
            bloques.append({
                "comentario": "Tipo de cambio sintetico para las fechas del volumen (hasta 400 dias atras).",
                "sql": "INSERT INTO tipo_cambio (moneda_origen, moneda_destino, fecha, tipo_cambio, fuente, cargado_en)\n"
                       "SELECT 'BOB', 'USD', d::date, 0.143678, 'MANUAL', now()\n"
                       "  FROM generate_series(current_date - 400, current_date + 30, interval '1 day') AS d\n"
                       " ON CONFLICT DO NOTHING;",
            })
        for prefijo in sorted(incluye):
            doc = copy.deepcopy(fuentes[prefijo])
            for bloque in doc["bloques"]:
                if "sql" in bloque and SQL_COMPARTIDO.search(bloque["sql"]):
                    continue
                nuevo = reemplazar(bloque, mapa, desplazamiento)
                nuevo = json.loads(renombrar_demo(json.dumps(nuevo, ensure_ascii=False), n, protegidos))
                for fila in nuevo.get("filas", []):
                    personas(fila, faker, n)
                bloques.append(nuevo)
        salida = {
            "descripcion": f"VOLUMEN SINTETICO, clon {n:02d} ({'+'.join(sorted(incluye))}; "
                           f"{desplazamiento} dias atras). Generado por scripts/generar_volumen_dev.py con "
                           f"semilla {SEMILLA}; no editar a mano.",
            "entorno": "dev",
            "origen": "SINTETICO_FAKER",
            "nota": "Personas inventadas con Faker (locale es_MX, no hay es_BO). Ningun dato es real.",
            "bloques": bloques,
        }
        destino = VOLUMEN / f"{PREFIJO}{n:02d}.json"
        destino.write_text(json.dumps(salida, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        escritos.append(destino.name)
    manifiesto = {
        "entorno": "dev",
        "descripcion": "Volumen SINTETICO para los tableros por rol: clones del escenario demo de dev/, con "
                       "personas de Faker. NO aplicar en produccion: la guarda de entorno es la misma de dev/.",
        "marca": {
            "tabla": "usuario",
            "donde": f"codigo_publico = '{renombrar('USR000001', 1)}'",
            "por_que": "El primer usuario del clon 01. Si existe, el volumen ya se sembro y no se repite.",
        },
        "orden": escritos,
    }
    (VOLUMEN / "manifiesto.json").write_text(
        json.dumps(manifiesto, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{len(escritos)} archivos · {CLONES} clones · {len(tokens)} claves renombradas por clon")


if __name__ == "__main__":
    main()
