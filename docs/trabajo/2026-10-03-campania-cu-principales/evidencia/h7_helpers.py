"""Verificacion RS256 con la biblioteca estandar (sin dependencias): modulo y exponente publicos del JWKS."""
import base64
import hashlib
import json
import urllib.request

BASE_JWKS = "http://localhost/.well-known/jwks.json"
_PREFIJO_SHA256 = bytes.fromhex("3031300d060960864801650304020105000420")


def _b64(s):
    return base64.urlsafe_b64decode(s + "=" * (-len(s) % 4))


def jwks():
    """El JWKS lo sirve identidad por la red interna (no sale por el gateway)."""
    import subprocess

    r = subprocess.run(["docker", "exec", "aportaya-identidad", "curl", "-s", "http://localhost:8080/.well-known/jwks.json"],
                       capture_output=True, text=True, timeout=30)
    return json.loads(r.stdout)


def rs256_ok(jwt, claves):
    cab, carga, firma = jwt.split(".")
    kid = json.loads(_b64(cab)).get("kid")
    k = next((x for x in claves["keys"] if x.get("kid") == kid), None)
    if k is None:
        return False
    n, e = int.from_bytes(_b64(k["n"]), "big"), int.from_bytes(_b64(k["e"]), "big")
    tam = (n.bit_length() + 7) // 8
    em = pow(int.from_bytes(_b64(firma), "big"), e, n).to_bytes(tam, "big")
    digest = hashlib.sha256(f"{cab}.{carga}".encode()).digest()
    esperado = b"\x00\x01" + b"\xff" * (tam - len(_PREFIJO_SHA256) - len(digest) - 3) + b"\x00" + _PREFIJO_SHA256 + digest
    return em == esperado
