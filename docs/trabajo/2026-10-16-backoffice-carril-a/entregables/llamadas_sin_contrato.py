import re, pathlib, yaml, sys
R = pathlib.Path(sys.argv[1])
llamadas = set()
for ts in (R / "apps/backoffice/src/app").rglob("*.ts"):
    if ts.name.endswith(".spec.ts"):
        continue
    t = ts.read_text(encoding="utf-8")
    for m in re.finditer(r"(get|post|put|patch|delete|httpResource)\s*(?:<[^()]*?>)?\s*\(\s*(?:\(\)\s*=>\s*)?`\$\{gateway\}(/[^`?]*)", t):
        verbo = "GET" if m.group(1) in ("get", "httpResource") else m.group(1).upper()
        ruta = re.sub(r"\$\{[^}]+\}", "{x}", m.group(2)).rstrip("/")
        llamadas.add((verbo, ruta, ts.relative_to(R).as_posix()))
contrato = {}
for y in (R / "servicios").glob("*/src/main/resources/openapi/*.yaml"):
    d = yaml.safe_load(y.read_text(encoding="utf-8"))
    for camino, ops in (d.get("paths") or {}).items():
        for v in ops:
            if v in ("get", "post", "put", "patch", "delete"):
                contrato[(v.upper(), re.sub(r"\{[^}]+\}", "{x}", camino))] = (y.parent.parent.parent.parent.parent.name, ops[v].get("operationId"))
faltan = sorted({(v, r, f) for v, r, f in llamadas if (v, r) not in contrato})
print(f"llamadas de la UI: {len({(v,r) for v,r,_ in llamadas})} · sin operacion en ningun contrato: {len({(v,r) for v,r,_ in faltan})}")
for v, r, f in faltan:
    print(f"  {v:6} {r:55} {f}")
