"""Replica de las dos reglas de Barrido (tamano-archivo y sin-umbral-literal) para un servicio. Solo lectura."""
import re, sys, pathlib
raiz = pathlib.Path(sys.argv[1]) / "src"
CONST = re.compile(r"(?i)\b(UMBRAL|LIMITE|TOPE|TARIFA|COMISION|MONTO|MINIMO|MAXIMO|PORCENTAJE|TASA)[A-Za-z_]*\s*=\s*-?\d")
BIGD = re.compile(r'(new\s+BigDecimal\s*\(|BigDecimal\.valueOf\s*\()\s*"?-?\d')
def fuentes(base):
    return sorted(p for p in base.rglob("*.java") if "/build/" not in p.as_posix())
mal = 0
for p in fuentes(raiz):
    n = len(p.read_text().split("\n")) - (1 if p.read_text().endswith("\n") else 0)
    if n >= 300:
        print(f"tamano-archivo  {p.relative_to(raiz.parent)}  {n} lineas"); mal += 1
principal = raiz / "main/java"
for p in fuentes(principal if principal.is_dir() else raiz):
    if "/src/test/" in p.as_posix(): continue
    for i, l in enumerate(p.read_text().split("\n"), 1):
        s = l.strip()
        if s.startswith(("//", "*", "/*")): continue
        if CONST.search(l) or BIGD.search(l):
            print(f"sin-umbral-literal  {p.relative_to(raiz.parent)}:{i}  {s}"); mal += 1
print(f"HALLAZGOS: {mal}")
sys.exit(1 if mal else 0)
