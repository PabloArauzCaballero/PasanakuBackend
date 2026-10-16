"""H7 — cuenta DESECHABLE para recorrer la app: alta por API (CU-01) + 3 fotos sinteticas (CU-02). Se aprueba en el backoffice.

Todo es sintetico y rotulado: teléfono +59171000195, documento 9000195, fotos PNG generadas aqui (bandas de color, sin
ninguna persona ni documento real). La clave sale de CLAVE_DEV y no se imprime. No toca ninguna cuenta sembrada.
"""
import json
import os
import struct
import urllib.request
import uuid
import zlib

from campania_lib import BASE, llamar, sql

TEL = "+59171000195"


def png(ancho, alto, colores):
    """PNG RGB de bandas horizontales; sin dependencias."""
    filas = bytearray()
    for y in range(alto):
        r, g, b = colores[(y * len(colores)) // alto]
        filas += b"\x00" + bytes([r, g, b]) * ancho

    def trozo(tipo, datos):
        c = struct.pack(">I", len(datos)) + tipo + datos
        return c + struct.pack(">I", zlib.crc32(tipo + datos) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + trozo(b"IHDR", struct.pack(">IIBBBBB", ancho, alto, 8, 2, 0, 0, 0))
            + trozo(b"IDAT", zlib.compress(bytes(filas), 6)) + trozo(b"IEND", b""))


def subir(usuario_id, cara, contenido, nombre):
    limite = "----campania" + uuid.uuid4().hex
    cuerpo = (f"--{limite}\r\nContent-Disposition: form-data; name=\"cara\"\r\n\r\n{cara}\r\n"
              f"--{limite}\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\"{nombre}\"\r\n"
              f"Content-Type: image/png\r\n\r\n").encode() + contenido + f"\r\n--{limite}--\r\n".encode()
    req = urllib.request.Request(f"{BASE}/usuarios/{usuario_id}/documentos", data=cuerpo, method="POST", headers={
        "Content-Type": f"multipart/form-data; boundary={limite}", "Idempotency-Key": str(uuid.uuid4()), "Accept": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            est, txt = r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        est, txt = e.code, e.read().decode()
    print(f"POST  /usuarios/<id>/documentos ({cara})  -> {est}")
    return est


print("== contratos vigentes (los que hay que aceptar)")
est, contratos = llamar("GET", "/cumplimiento/contratos/vigentes", publico=True)
ids = [c["id"] if "id" in c else c.get("contratoId") for c in contratos] if isinstance(contratos, list) else []
print("   contratos:", len(ids))

print("== alta (CU-01) de la cuenta desechable")
existente = sql(f"select id from identidad.usuario where telefono_e164 = '{TEL}'")
if existente:
    print("   ya existe (se reutiliza):", existente.splitlines()[0][:8] + "…")
    uid = existente.splitlines()[0]
else:
    est, r = llamar("POST", "/usuarios", publico=True, idem=True, cuerpo={
        "telefonoE164": TEL, "nombres": "Prueba", "apellidos": "Desechable", "fechaNacimiento": "1990-05-15",
        "contrasena": os.environ["CLAVE_DEV"], "canalVerificacion": "SMS",
        "documento": {"tipo": "CI", "numero": "9000195", "lugarExpedicion": "SC"},
        "aceptaContratos": ids})
    print("   respuesta:", {k: v for k, v in r.items() if k in ("estado", "nivelDiligencia", "codigo", "mensaje")} if isinstance(r, dict) else r)
    uid = r.get("usuarioId") if isinstance(r, dict) else None

if uid:
    print("== expediente: 3 fotos sinteticas")
    subir(uid, "ANVERSO", png(1600, 1009, [(30, 90, 60), (240, 170, 70), (30, 90, 60)]), "carnet-sintetico-anverso.png")
    subir(uid, "REVERSO", png(1600, 1009, [(60, 60, 90), (200, 200, 220), (60, 60, 90)]), "carnet-sintetico-reverso.png")
    subir(uid, "SELFIE", png(1200, 1600, [(120, 80, 60), (230, 200, 170), (120, 80, 60)]), "selfie-sintetica.png")
    print("SQL verificacion:", sql(f"select estado||' · selfie='||(url_selfie is not null)::text from identidad.verificacion_kyc where usuario_id='{uid}' order by iniciada_en desc limit 1"))
