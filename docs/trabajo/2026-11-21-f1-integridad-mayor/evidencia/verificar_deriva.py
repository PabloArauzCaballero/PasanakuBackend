#!/usr/bin/env python3
"""Verificacion de deriva (regla 97.2) para las tablas que toca F1.S1.

Modelo -> sql/: la regeneracion no deja diff (se corre aparte: generar_ddl.py + git status).
sql/ -> base: para cada tabla tocada compara, contra la base viva,
  - tablas y columnas declaradas en sql/10_tablas/**/<tabla>.sql (ausente / sobrante),
  - nulabilidad de cada columna (NOT NULL en el DDL vs attnotnull en la base),
  - toda CONSTRAINT con nombre declarada en sql/ (tablas + 40_reglas) para esa tabla,
  - todo trigger declarado en sql/40_reglas y sql/35_append_only para esa tabla.
Base -> jOOQ: se demuestra compilando los servicios (generateJooq introspecta la base).

Uso: python3 -I verificar_deriva.py <raiz-repo> <dsn psql>
Sale con 1 si encuentra deriva.
"""
import pathlib, re, subprocess, sys

raiz = pathlib.Path(sys.argv[1]); dsn = sys.argv[2]
TABLAS = {"asiento_contable": "nucleo_financiero", "movimiento_contable": "nucleo_financiero",
          "obligacion_aporte": "aportes", "orden_cobro": "aportes", "pago": "aportes",
          "reembolso": "aportes", "disputa_pago": "aportes"}

def psql(q):
    r = subprocess.run(["psql", dsn, "-X", "-A", "-t", "-F", "|", "-c", q], capture_output=True, text=True, check=True)
    return [l.split("|") for l in r.stdout.splitlines() if l.strip()]

reglas = (raiz / "sql/40_reglas/restricciones.sql").read_text(encoding="utf-8") + \
         (raiz / "sql/35_append_only/append_only.sql").read_text(encoding="utf-8")
fallas = 0
for tabla, esquema in TABLAS.items():
    ddl = next(raiz.glob(f"sql/10_tablas/*/{tabla}.sql")).read_text(encoding="utf-8")
    cuerpo = ddl[ddl.index("("):ddl.index("\n);")]
    cols = {}
    for linea in cuerpo.splitlines():
        m = re.match(r"\s+([a-z_][a-z0-9_]*)\s+(\S.*?),?$", linea)
        if m and m.group(1) not in ("constraint",) and not linea.strip().upper().startswith("CONSTRAINT"):
            cols[m.group(1)] = "NOT NULL" in m.group(2).upper()
    base = {c: n == "t" for c, n in psql(
        f"SELECT attname, attnotnull FROM pg_attribute WHERE attrelid = '{esquema}.{tabla}'::regclass "
        "AND attnum > 0 AND NOT attisdropped")}
    nombres_ck = set(re.findall(r"CONSTRAINT\s+([a-z0-9_]+)", ddl))
    # restricciones agregadas por ALTER TABLE <tabla> en 40_reglas (nombre simple o calificado)
    for m in re.finditer(r"ALTER TABLE\s+(?:[a-z_]+\.)?" + tabla + r"\s+ADD CONSTRAINT\s+([a-z0-9_]+)", reglas + ddl):
        nombres_ck.add(m.group(1))
    en_base_ck = {r[0] for r in psql(f"SELECT conname FROM pg_constraint WHERE conrelid = '{esquema}.{tabla}'::regclass")}
    trg = set(re.findall(r"CREATE (?:CONSTRAINT )?TRIGGER\s+([a-z0-9_]+)\s+(?:[^;]*?)\s+ON\s+(?:[a-z_]+\.)?" + tabla + r"\b", reglas, re.S))
    en_base_trg = {r[0] for r in psql(f"SELECT tgname FROM pg_trigger WHERE tgrelid = '{esquema}.{tabla}'::regclass AND NOT tgisinternal")}
    problemas = []
    problemas += [f"columna ausente en la base: {c}" for c in cols if c not in base]
    problemas += [f"columna de la base fuera del modelo: {c}" for c in base if c not in cols]
    problemas += [f"nulabilidad divergente: {c} (modelo NOT NULL={cols[c]}, base={base[c]})" for c in cols if c in base and cols[c] != base[c]]
    problemas += [f"restriccion ausente en la base: {n}" for n in sorted(nombres_ck - en_base_ck)]
    problemas += [f"trigger ausente en la base: {n}" for n in sorted(trg - en_base_trg)]
    estado = "OK " if not problemas else "DERIVA"
    print(f"{estado} {esquema}.{tabla}: {len(cols)} columnas, {len(nombres_ck)} restricciones con nombre, {len(trg)} triggers del modelo")
    for p in problemas:
        print("     -", p)
    fallas += len(problemas)
print(f"deriva: {fallas} problema(s)")
sys.exit(1 if fallas else 0)
