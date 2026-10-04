"""H2.S1.M6 — sondas de autorización: ¿qué rol y qué permisos lleva realmente cada token, y qué endpoints bloquea?"""
import uuid
from campania_lib import claims, llamar, login

FALSO = str(uuid.uuid4())
print("== 1. Tokens: rol y permisos reales")
tokens = {}
for cuenta, factor in [("USR90", None), ("USR1", None), ("USR2", None), ("USR91", "000000"), ("USR8", "000000"), ("USR9", "000000")]:
    t = login(cuenta, factor)
    tokens[cuenta] = t
    if t:
        c = claims(t)
        perms = c.get("permisos") or c.get("perms") or c.get("scope")
        print(f"   {cuenta}: rol={c.get('rol')} permisos={perms}")

print("\n== 2. Sondas (cuerpos mínimos: el guardia de permisos responde antes de validar el cuerpo)")
sondas = [
    ("USR1", "GET", "/licencia/alcance?servicio=GRUPO_PASANAKU", None, "CU-20: consulta de licencia que hace el servicio grupos"),
    ("USR90", "GET", "/licencia/alcance?servicio=GRUPO_PASANAKU", None, "mismo, con token de participante"),
    ("USR91", "GET", "/licencia/alcance?servicio=GRUPO_PASANAKU", None, "mismo, con token de backoffice"),
    ("USR91", "POST", f"/organizadores/postulaciones/{FALSO}/aprobacion", {}, "CU-90 aprobar (ADMIN_PLATAFORMA)"),
    ("USR91", "POST", f"/organizadores/{FALSO}/habilitacion", {}, "CU-90 habilitar (ADMIN_PLATAFORMA)"),
    ("USR91", "POST", "/reputacion/insignias/evaluacion", {}, "CU-74 evaluar insignias (SOPORTE)"),
    ("USR90", "POST", "/organizadores/postulaciones", {}, "CU-90 postular (PARTICIPANTE) — debe pasar el guardia"),
    ("USR1", "POST", "/grupos", {}, "CU-20 crear grupo (GRUPO_CREAR) — debe pasar el guardia"),
]
for cuenta, metodo, ruta, cuerpo, que in sondas:
    t = tokens.get(cuenta)
    print(f"-- {que}  [actor {cuenta}]")
    if not t:
        print("   (sin token: no se pudo iniciar sesión)")
        continue
    llamar(metodo, ruta, token=t, cuerpo=cuerpo, idem=True if metodo == "POST" else None)
