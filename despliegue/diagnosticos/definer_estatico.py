"""Analisis ESTATICO (sin base) de las funciones que sql/00_base/03_permisos.sql vuelve SECURITY DEFINER.

    python despliegue/diagnosticos/definer_estatico.py

Lee solo los archivos que aplica sql/aplicar.sql (no 50_verificacion). Es una heuristica por palabra: la lista de
'candidatas' es el punto de partida de una revision humana. El diagnostico contra una base real esta en
security_definer.sql / diagnostico_definer.sh.
"""
import pathlib
import re
import sys

RAIZ = pathlib.Path(__file__).resolve().parent.parent.parent / "sql"
CREA = re.compile(r"CREATE\s+(?:OR\s+REPLACE\s+)?FUNCTION\s+([a-zA-Z_][\w.]*)\s*\(([^)]*)\)\s*RETURNS\s+([\w\[\]]+)", re.I)
funciones = {}
APLICADOS = ("00_base", "10_tablas", "15_infra", "20_claves", "30_indices", "35_append_only", "40_reglas")
def aplicado(f):
    return f.relative_to(RAIZ).parts[0] in APLICADOS
for f in sorted(p for p in RAIZ.rglob("*.sql") if aplicado(p)):
    t = f.read_text(encoding="utf-8")
    for m in CREA.finditer(t):
        nombre = m.group(1)
        if "." in nombre:  # calificada: no cae en `aportes` por search_path
            continue
        funciones.setdefault(nombre, (m.group(3).lower(), f.relative_to(RAIZ).as_posix()))
tablas = {}
for f in RAIZ.glob("10_tablas/*/*.sql"):
    esquema = None
    for m in re.finditer(r"CREATE TABLE IF NOT EXISTS ([a-z_]+)\.([a-z_]+)", f.read_text(encoding="utf-8")):
        tablas[m.group(2)] = m.group(1)

total = len(funciones)
seg = [n for n in funciones if n.startswith("fn_seg_")]
definer = [n for n in funciones if not n.startswith("fn_seg_")]
trig = [n for n in definer if funciones[n][0] == "trigger"]
no_trig = [n for n in definer if funciones[n][0] != "trigger"]
print(f"funciones sin esquema (caen en `aportes`): {total}")
print(f"  fn_seg_* (quedan INVOKER): {len(seg)}")
print(f"  DEFINER (todas las demas): {len(definer)}  -> triggers: {len(trig)} · invocables por SQL (no trigger): {len(no_trig)}")
print("no-trigger invocables:")
for n in sorted(no_trig):
    print(f"   {n} -> {funciones[n][0]}  ({funciones[n][1]})")

# Cruce de esquemas: tablas que menciona el cuerpo (por palabra) y esquemas distintos entre si
cuerpos = {}
for f in sorted(p for p in RAIZ.rglob("*.sql") if aplicado(p)):
    t = f.read_text(encoding="utf-8")
    for m in re.finditer(r"CREATE\s+(?:OR\s+REPLACE\s+)?FUNCTION\s+([a-zA-Z_][\w]*)\s*\([^)]*\)\s*RETURNS[\s\S]*?(\$\$|\$[a-z]+\$)([\s\S]*?)\2", t, re.I):
        cuerpos.setdefault(m.group(1), m.group(3))
cruzan = []
for n in definer:
    cuerpo = cuerpos.get(n, "")
    esquemas = {tablas[t] for t in tablas if re.search(r"\b" + re.escape(t) + r"\b", cuerpo)}
    if len(esquemas) > 1:
        cruzan.append((n, sorted(esquemas)))
print(f"\nDEFINER cuyo cuerpo menciona tablas de MAS DE UN esquema (candidatas a conservar): {len(cruzan)}")
for n, e in sorted(cruzan):
    print(f"   {n}: {', '.join(e)}")
print(f"DEFINER sin cruce aparente (candidatas a INVOKER): {len(definer) - len(cruzan)}")
