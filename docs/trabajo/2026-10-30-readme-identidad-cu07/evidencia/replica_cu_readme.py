"""Replica de DocumentacionVivaContratoTest.elReadmeDiceLosCuQueElServicioTiene (solo lectura)."""
import pathlib, re, sys
raiz = pathlib.Path(sys.argv[1])
CODIGO = re.compile(r"\bCU(\d{2})[A-Z]")
README = re.compile(r"\bCU-(\d{2})\b")
malos = []
for srv in sorted(p for p in (raiz / "servicios").iterdir() if p.is_dir()):
    java = srv / "src/main/java"
    en_codigo = set()
    if java.is_dir():
        for f in java.rglob("*.java"):
            if re.match(r"CU\d{2}.*\.java$", f.name) and (m := CODIGO.search(f.name)):
                en_codigo.add(m.group(1))
    rd = srv / "README.md"
    en_readme = set(README.findall(rd.read_text(encoding="utf-8"))) if rd.is_file() else set()
    falta = sorted(en_codigo - en_readme)
    if falta:
        malos.append(f"{srv.name}: implementa {falta} y su README no los nombra")
print("DIVERGENCIAS:", malos if malos else "[]")
sys.exit(1 if malos else 0)
