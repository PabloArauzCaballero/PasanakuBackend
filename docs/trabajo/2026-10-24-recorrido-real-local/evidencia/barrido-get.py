"""Barrido de todas las rutas GET sin parametro de ruta de los contratos OpenAPI, contra el gateway
local y con el token del operador (el token sale de un archivo fuera del repositorio).
Clasifica: 2xx ok | 4xx esperado (401/403/404 de datos, 400 validacion) | 422 | 5xx (defecto) | 404 de ruta."""
import glob, json, os, sys, urllib.request, urllib.error, yaml, collections
TOKEN = open(sys.argv[1]).read().strip()
BASE = "http://127.0.0.1:8080/api/v1"
res = []
for f in sorted(glob.glob("servicios/*/src/main/resources/openapi/*.yaml")):
    srv = f.split("/")[1]
    d = yaml.safe_load(open(f))
    for path, ops in (d.get("paths") or {}).items():
        g = ops.get("get")
        if not g or "{" in path: continue
        # parametros de consulta requeridos: se omiten el barrido (se espera 400)
        req = [p for p in (g.get("parameters") or []) if isinstance(p, dict) and p.get("required") and p.get("in") == "query"]
        r = urllib.request.Request(BASE + path, headers={"Authorization": "Bearer " + TOKEN})
        try:
            with urllib.request.urlopen(r, timeout=30) as x: code, body = x.status, x.read()[:300]
        except urllib.error.HTTPError as e: code, body = e.code, (e.read() or b"")[:300]
        except Exception as e: code, body = 0, str(e).encode()
        try: cod = json.loads(body).get("codigo", "") if body[:1] == b"{" else ""
        except Exception: cod = ""
        res.append({"servicio": srv, "ruta": path, "estado": code, "codigo": cod, "consulta_requerida": [p.get("name") for p in req]})
json.dump(res, open(sys.argv[2], "w"), indent=1, ensure_ascii=False)
c = collections.Counter(r["estado"] for r in res)
print(len(res), "rutas GET:", dict(sorted(c.items())))
for r in res:
    if r["estado"] >= 500 or r["estado"] == 0 or r["codigo"] == "AP-DOM-01" or (r["estado"] == 404 and r["codigo"] == "AP-VAL-05"):
        print(f'  {r["estado"]} {r["codigo"]:<11} {r["servicio"]:<18} {r["ruta"]}')
