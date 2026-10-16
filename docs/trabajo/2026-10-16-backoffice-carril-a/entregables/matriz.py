#!/usr/bin/env python3
"""
Matrices del carril A del backoffice (A0), GENERADAS desde las fuentes del repo.

    python3 docs/trabajo/2026-10-16-backoffice-carril-a/entregables/matriz.py            # escribe
    python3 docs/trabajo/2026-10-16-backoffice-carril-a/entregables/matriz.py --verificar # compara

Fuentes (nada se escribe a mano):
  - pantallas y CU:  docs/Flujo de pantallas · backoffice administrador.md (### y filas de tabla)
  - servicio del CU: cabecera "## Contrato · `openapi/<servicio>.yaml`" de cada CU
  - operaciones:     servicios/<servicio>/src/main/resources/openapi/<servicio>.yaml, operationId con
                     comentario "# CU-NN" (verbo HTTP y ruta leidos del YAML)
  - permisos:        columna «Exige» de cada CU y @Permiso de los controladores
  - catalogo:        sql/60_semillas/10-roles-y-permisos.sql (permiso y rol_permiso)

Con --verificar regenera en memoria y falla (exit 1) si los .md versionados difieren: la matriz
no se puede quedar vieja sin que alguien lo note.
"""
import pathlib
import re
import sys

import yaml

R = pathlib.Path(__file__).resolve().parents[4]
AQUI = pathlib.Path(__file__).resolve().parent
FLUJO = R / "docs" / "Flujo de pantallas · backoffice administrador.md"
CU_DIR = R / "docs" / "CasosDeUso"
SEMILLA = R / "sql" / "60_semillas" / "10-roles-y-permisos.sql"

# Pantallas que existen en apps/backoffice pero no tienen fila propia en el flujo de pantallas.
# Cada una con su motivo: no se agrega ninguna "porque parece".
PANTALLAS_EXTRA = [
    ("operacion/retiros", [11], "ruta real en apps/backoffice/src/app/rutas/operacion/operacion.routes.ts; ADR-049"),
]

RE_CU = re.compile(r"\[\[CU-(\d+)[^\]]*\]\]")
RE_RUTA = re.compile(r"`([a-z\-]+/[a-z\-/]+)`")
RE_CONTRATO = re.compile(r"^## Contrato · `openapi/([\w-]+)\.yaml`", re.M)
METODOS = ("get", "post", "put", "patch", "delete")


def pantallas():
    filas = []
    for linea in FLUJO.read_text(encoding="utf-8").splitlines():
        if not (linea.startswith("### ") or linea.startswith("| `")):
            continue
        ruta = RE_RUTA.search(linea)
        cus = [int(n) for n in RE_CU.findall(linea)]
        if ruta and ruta.group(1).split("/")[0] in ("acceso", "operacion", "cumplimiento", "contabilidad", "publicidad"):
            filas.append((ruta.group(1), cus, "Flujo de pantallas"))
    for ruta, cus, motivo in PANTALLAS_EXTRA:
        filas.append((ruta, cus, motivo))
    return filas


def doc_cu(numero):
    candidatos = sorted(CU_DIR.glob(f"CU-{numero:02d} *.md")) + sorted(CU_DIR.glob(f"CU-{numero} *.md"))
    return candidatos[0] if candidatos else None


def exige(texto):
    """Codigos de la seccion «Eventos, trabajos y permisos» del CU.

    MISMA definicion que scripts/verificar_seguridad.py (bloque 6, S-8): todo codigo en mayusculas
    de la seccion, de seis o mas caracteres. Si cada herramienta contara a su manera, la matriz y el
    gate de seguridad dirian numeros distintos y nadie sabria cual creer.
    """
    m = re.search(r"## Eventos, trabajos y permisos(.*?)(\n## )", texto, re.S)
    return sorted(set(re.findall(r"`([A-Z][A-Z0-9_]{5,})`", m.group(1)))) if m else []


_OPENAPI = {}


def operaciones(servicio, numero):
    if servicio not in _OPENAPI:
        ruta = R / "servicios" / servicio / "src" / "main" / "resources" / "openapi" / f"{servicio}.yaml"
        texto = ruta.read_text(encoding="utf-8")
        doc = yaml.safe_load(texto)
        por_id = {}
        for camino, metodos in (doc.get("paths") or {}).items():
            for metodo, op in metodos.items():
                if metodo in METODOS and isinstance(op, dict) and "operationId" in op:
                    por_id[op["operationId"]] = (metodo.upper(), camino)
        comentarios = {}
        for linea in texto.splitlines():
            m = re.search(r"operationId:\s*(\w+)\s*#\s*(.*)$", linea)
            if m:
                comentarios[m.group(1)] = m.group(2)
        _OPENAPI[servicio] = (por_id, comentarios)
    por_id, comentarios = _OPENAPI[servicio]
    marcas, implementadas = controladores(servicio)
    patron = re.compile(rf"CU-0*{numero}\b")
    ids = {op for op, comentario in comentarios.items() if patron.search(comentario)}
    ids |= {op for op, cus in marcas.items() if numero in cus}
    return sorted((op, *por_id[op], op in implementadas) for op in ids if op in por_id)


_CONTROLADORES = {}
RE_METODO = re.compile(r"public\s+ResponseEntity<[^(]*?>\s+(\w+)\s*\(")
RE_MARCA = re.compile(r'marcarCasoDeUso\(\s*"CU-0*(\d+)"')


def controladores(servicio):
    """operationId -> CU que marca su metodo (Traza.marcarCasoDeUso), y cuales estan implementados.

    Un operationId del contrato sin metodo que lo sobrescriba responde el EJEMPLO del generador:
    no esta implementado aunque el contrato lo publique.
    """
    if servicio not in _CONTROLADORES:
        marcas, implementadas = {}, set()
        for java in (R / "servicios" / servicio / "src" / "main" / "java").rglob("*.java"):
            if "/web/" not in java.as_posix():
                continue
            texto = java.read_text(encoding="utf-8")
            metodos = list(RE_METODO.finditer(texto))
            for i, m in enumerate(metodos):
                previo = texto[max(0, m.start() - 400) : m.start()]
                if "@Override" not in previo:
                    continue
                fin = metodos[i + 1].start() if i + 1 < len(metodos) else len(texto)
                cuerpo = texto[m.end() : fin]
                implementadas.add(m.group(1))
                marcas.setdefault(m.group(1), set()).update(int(n) for n in RE_MARCA.findall(cuerpo))
        _CONTROLADORES[servicio] = (marcas, implementadas)
    return _CONTROLADORES[servicio]


def catalogo():
    texto = SEMILLA.read_text(encoding="utf-8")
    bloque = texto.split("INSERT INTO permiso", 1)[1].split("ON CONFLICT", 1)[0]
    permisos = {}
    for m in re.finditer(r"\('([A-Z_]+)',\s*'((?:[^']|'')*)',\s*'([^']*)',\s*'([^']*)',\s*(TRUE|FALSE)\)", bloque):
        permisos[m.group(1)] = (m.group(2).replace("''", "'"), m.group(3), m.group(4), m.group(5) == "TRUE")
    roles_de = {}
    asign = texto.split("INSERT INTO rol_permiso", 1)[1].split("ON CONFLICT", 1)[0]
    for rol, permiso in re.findall(r"codigo = '([A-Z_]+)'\), \(SELECT id FROM permiso WHERE codigo = '([A-Z_]+)'\)", asign):
        roles_de.setdefault(permiso, []).append(rol)
    bloque_rol = texto.split("INSERT INTO rol ", 1)[1].split("ON CONFLICT", 1)[0]
    roles = re.findall(r"^\s*\('([A-Z_]+)',\s*'[^']*',\s*'[A-Z_]+',\s*(?:TRUE|FALSE)\)", bloque_rol, re.M)
    return permisos, roles_de, roles


def matriz_trazabilidad():
    permisos, _, _ = catalogo()
    lineas = [
        "# Matriz de trazabilidad — backoffice (carril A · A0)",
        "",
        "> GENERADA por `matriz.py` desde el flujo de pantallas, los CU, los OpenAPI y la semilla de permisos.",
        "> No se edita a mano: `python3 .../entregables/matriz.py` la reescribe y `--verificar` falla si quedó vieja.",
        "",
        "Estados: **existe** = hay operaciones del CU y al menos un `GET` · **sin lectura atribuida** = hay",
        "operaciones del CU pero ningún `GET` atribuido a él (puede existir un `GET` sin CU: ver la última",
        "sección) · **ausente** = ninguna operación atribuida al CU ·",
        "**sin contrato** = el CU no declara servicio. Permiso **no catalogado** = la columna «Exige» del CU",
        "nombra un código que la semilla no tiene (S-8: lo decide Seguridad, no se inventa).",
        "Una operación se atribuye a un CU por su comentario `# CU-NN` en el YAML o por el",
        "`Traza.marcarCasoDeUso(\"CU-NN\")` del método que la implementa. **SIN IMPLEMENTAR** = el contrato",
        "la publica pero ningún controlador la sobrescribe (responde el ejemplo del generador).",
        "",
        "| Pantalla | Fuente | CU | Servicio | Operaciones (verbo ruta · operationId) | Exige (CU) | Estado |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    resumen = {}
    for ruta, cus, fuente in pantallas():
        if not cus:
            lineas.append(f"| `{ruta}` | {fuente} | — | — | — | — | **sin CU en el flujo** |")
            resumen["sin CU en el flujo"] = resumen.get("sin CU en el flujo", 0) + 1
            continue
        for numero in cus:
            doc = doc_cu(numero)
            texto = doc.read_text(encoding="utf-8") if doc else ""
            m = RE_CONTRATO.search(texto)
            servicio = m.group(1) if m else None
            ops = operaciones(servicio, numero) if servicio else []
            codigos = exige(texto)
            exige_txt = ", ".join(
                f"`{c}`" if c in permisos else f"`{c}` (no catalogado)" for c in codigos
            ) or "—"
            if not servicio:
                estado = "sin contrato"
            elif not ops:
                estado = "ausente"
            elif not any(v == "GET" for _, v, _, _ in ops):
                estado = "sin lectura atribuida"
            else:
                estado = "existe"
            resumen[estado] = resumen.get(estado, 0) + 1
            ops_txt = "<br>".join(f"`{v} {c}` · {o}{'' if impl else ' (SIN IMPLEMENTAR)'}" for o, v, c, impl in ops) or "—"
            lineas.append(
                f"| `{ruta}` | {fuente} | CU-{numero:02d} | {servicio or '—'} | {ops_txt} | {exige_txt} | **{estado}** |"
            )
    lineas += ["", "## Resumen", ""]
    for estado in sorted(resumen):
        lineas.append(f"- {estado}: {resumen[estado]}")
    lineas += [
        "",
        "## Lecturas (`GET`) que no están atribuidas a ningún CU",
        "",
        "Existen en el contrato pero ni su comentario ni su controlador dicen a qué CU sirven; pueden",
        "alimentar una pantalla de arriba. Atribuirlas es tarea del dueño de cada contrato, no de esta matriz.",
        "",
        "| Servicio | Operación |",
        "| --- | --- |",
    ]
    for servicio in sorted(_OPENAPI):
        por_id, comentarios = _OPENAPI[servicio]
        marcas, implementadas = controladores(servicio)
        for op, (verbo, camino) in sorted(por_id.items()):
            atribuida = bool(re.search(r"CU-\d+", comentarios.get(op, ""))) or bool(marcas.get(op))
            if verbo == "GET" and not atribuida:
                impl = "" if op in implementadas else " (SIN IMPLEMENTAR)"
                lineas.append(f"| {servicio} | `GET {camino}` · {op}{impl} |")
    return "\n".join(lineas) + "\n"


def matriz_autorizacion():
    permisos, roles_de, roles = catalogo()
    exigidos = {}
    for doc in sorted(CU_DIR.glob("CU-*.md")):
        for codigo in exige(doc.read_text(encoding="utf-8")):
            exigidos.setdefault(codigo, []).append(doc.name.split(" ")[0])
    lineas = [
        "# Matriz de autorización — backoffice (carril A · A0)",
        "",
        "> GENERADA por `matriz.py` desde `sql/60_semillas/10-roles-y-permisos.sql` y la columna «Exige» de los CU.",
        "",
        "## Permisos sembrados",
        "",
        "| Permiso | Recurso | Acción | MFA al usar | Roles que lo tienen | CU que lo exigen |",
        "| --- | --- | --- | --- | --- | --- |",
    ]
    for codigo in sorted(permisos):
        _, recurso, accion, mfa = permisos[codigo]
        roles_txt = ", ".join(sorted(roles_de.get(codigo, []))) or "— (ningún rol)"
        cus = ", ".join(exigidos.get(codigo, [])) or "—"
        lineas.append(f"| `{codigo}` | {recurso} | {accion} | {'sí' if mfa else 'no'} | {roles_txt} | {cus} |")
    faltan = {c: v for c, v in exigidos.items() if c not in permisos and c not in roles}
    lineas += [
        "",
        "## Exigidos por un CU y no catalogados (S-8) — `DECISION_REQUIRED`",
        "",
        "Cada uno necesita recurso, acción, `requiere_mfa` y rol, y eso lo decide Seguridad de la",
        "información (Flujo de pantallas §9.6). Hasta entonces, toda operación que los necesite queda",
        "cerrada (denegar por omisión). Los que coinciden con un **rol** sembrado no se listan acá.",
        "",
        "| Código | CU que lo exigen |",
        "| --- | --- |",
    ]
    for codigo in sorted(faltan):
        lineas.append(f"| `{codigo}` | {', '.join(faltan[codigo])} |")
    lineas += ["", f"Total no catalogados: {len(faltan)}", ""]
    lineas += ["## Roles sembrados", ""] + [f"- `{r}`" for r in roles] + [""]
    return "\n".join(lineas)


RE_CITA = re.compile(r"`((?:servicios|plataforma|sql|docs|apps|scripts|seeders)/[^`:]+):(\d+)`")


def citas():
    """Cada `ruta:linea` de los entregables .md existe: archivo presente y con esa linea."""
    rotas, total = [], 0
    for md in sorted(AQUI.glob("*.md")):
        for ruta, linea in RE_CITA.findall(md.read_text(encoding="utf-8")):
            total += 1
            archivo = R / ruta
            if not archivo.is_file() or len(archivo.read_text(encoding="utf-8").splitlines()) < int(linea):
                rotas.append(f"{md.name}: {ruta}:{linea}")
    for r in rotas:
        print("CITA ROTA ·", r)
    print(f"{total - len(rotas)}/{total} citas resuelven")
    return 1 if rotas else 0


def main():
    if "--citas" in sys.argv:
        return citas()
    salidas = {
        AQUI / "matriz-trazabilidad.md": matriz_trazabilidad(),
        AQUI / "matriz-autorizacion.md": matriz_autorizacion(),
    }
    if "--verificar" in sys.argv:
        viejas = [p.name for p, contenido in salidas.items() if not p.exists() or p.read_text(encoding="utf-8") != contenido]
        if viejas:
            print("DESACTUALIZADAS:", ", ".join(viejas))
            return 1
        print("OK · las matrices coinciden con las fuentes")
        return 0
    for p, contenido in salidas.items():
        p.write_text(contenido, encoding="utf-8")
        print("escrita", p.name)
    return 0


if __name__ == "__main__":
    sys.exit(main())
