"""Driver de la campaña E2E: HTTP con la librería estándar (equivalente a curl, sin sumar dependencias).

- La clave sale de la variable de entorno CLAVE_DEV; nunca se escribe en un archivo.
- Los teléfonos se enmascaran en la salida (regla 90.2.3). Los tokens no se imprimen.
- Cada llamada imprime una línea: método, ruta, estado HTTP y, si falla, el código de error del contrato.
"""
import base64
import json
import os
import urllib.error
import urllib.request
import uuid

BASE = os.environ.get("BASE_API", "http://localhost/api/v1")
HUELLA = os.environ.get("HUELLA", "c4a1c4a1c4a1c4a1c4a1c4a1c4a1c4a1")
CUENTAS = {  # cuentas sembradas en seeders/dev (sintéticas)
    "USR1": "+59171000001", "USR2": "+59171000002", "USR3": "+59171000003",
    "USR4": "+59171000004", "USR5": "+59171000005", "USR6": "+59171000006",
    "USR7": "+59171000007", "USR8": "+59171000008", "USR9": "+59171000009",
    "USR90": "+59171000090", "USR91": "+59171000091",
}


def mascara(tel):
    return tel[:4] + "***" + tel[-2:]


def llamar(metodo, ruta, token=None, cuerpo=None, idem=None, publico=False, mostrar=True):
    """Devuelve (estado, json|texto). `idem=True` genera una Idempotency-Key nueva; un str la reutiliza."""
    cabeceras = {"Content-Type": "application/json", "Accept": "application/json"}
    if token and not publico:
        cabeceras["Authorization"] = "Bearer " + token
    if idem is True:
        idem = str(uuid.uuid4())
    if idem:
        cabeceras["Idempotency-Key"] = idem
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(BASE + ruta, data=datos, method=metodo, headers=cabeceras)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            estado, texto = r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        estado, texto = e.code, e.read().decode()
    try:
        cuerpo_resp = json.loads(texto) if texto else None
    except ValueError:
        cuerpo_resp = texto
    if mostrar:
        codigo = ""
        if estado >= 400 and isinstance(cuerpo_resp, dict):
            codigo = " " + str(cuerpo_resp.get("codigo") or cuerpo_resp.get("code") or "")
        print(f"{metodo:5} {ruta[:80]:80} -> {estado}{codigo}", flush=True)
    return estado, cuerpo_resp


def claims(token):
    """Payload del JWT (sin verificar firma: solo para mostrar rol y permisos, no son PII)."""
    p = token.split(".")[1]
    p += "=" * (-len(p) % 4)
    return json.loads(base64.urlsafe_b64decode(p))


def login(clave_cuenta, factor=None):
    clave = os.environ["CLAVE_DEV"]
    cuerpo = {"telefonoE164": CUENTAS[clave_cuenta], "credencial": clave,
              "huellaDispositivo": HUELLA, "plataforma": "ANDROID"}
    if factor:
        cuerpo["factor"] = {"tipo": "TOTP", "valor": factor}
    est, r = llamar("POST", "/sesiones", cuerpo=cuerpo, publico=True, mostrar=False)
    tok = r.get("tokenAcceso") if isinstance(r, dict) else None
    print(f"LOGIN {clave_cuenta} ({mascara(CUENTAS[clave_cuenta])}) -> {est} "
          f"requiereFactor={r.get('requiereFactorAdicional') if isinstance(r, dict) else '?'} "
          f"token={'si' if tok else 'no'}", flush=True)
    return tok


def sql(consulta):
    """SELECT de verificación contra la base local (solo lectura); devuelve las filas como texto."""
    import subprocess
    r = subprocess.run(["docker", "exec", "aportaya-postgres", "psql", "-U", "pasanaku", "-d", "pasanaku", "-At", "-F", " | ", "-c", consulta],
                       capture_output=True, text=True, encoding="utf-8")
    return (r.stdout + r.stderr).strip()
