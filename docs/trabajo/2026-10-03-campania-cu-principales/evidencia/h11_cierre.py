"""H11 — verificacion en runtime de lo corregido en esta ronda: CU-68 (aceptar/rechazar), B24 (recien llegado), B32 (aprobar activa),
B37 (evidencia step-up firmada), B36 (mis participaciones). Todo con cuentas sinteticas de desarrollo."""
import base64
import json
import uuid

from campania_lib import claims, llamar, login, sql
from h7_helpers import jwks, rs256_ok

uno = lambda q: (sql(q).splitlines() or [""])[0]
G3 = uno("select id from grupos.grupo where codigo_publico='GRP-DEMO-03'")
t1, t2, t90, t91 = login("USR1"), login("USR2"), login("USR90"), login("USR91", "000000")
U90 = uno("select id from identidad.usuario where codigo_publico='USR000090'")
U4 = uno("select id from identidad.usuario where codigo_publico='USR000004'")
t4 = login("USR4")

print("\n===== CU-68 · dos recien llegados (sin historial) postulan a GRP-DEMO-03 (B24)")
e, s90 = llamar("POST", f"/grupos/{G3}/postulaciones", t90, idem=True, cuerpo={"cuposSolicitados": 1, "mensaje": "quiero entrar al grupo"})
e, s4 = llamar("POST", f"/grupos/{G3}/postulaciones", t4, idem=True, cuerpo={"cuposSolicitados": 1, "mensaje": "yo tambien quiero"})
print("SQL pendientes:", sql(f"select count(*) from grupos.solicitud_ingreso where grupo_id='{G3}' and estado='PENDIENTE'"))

print("\n===== CU-68 · la cola del organizador")
e, cola = llamar("GET", f"/grupos/{G3}/solicitudes", t1)
print("-- negativo: un participante sin GRUPO_ADMINISTRAR no ve la cola")
llamar("GET", f"/grupos/{G3}/solicitudes", t2)

ID90, ID4 = s90["solicitudId"], s4["solicitudId"]
print("\n===== CU-68 · decidir")
print("-- negativo: USR2 intenta aceptar (sin permiso)")
llamar("POST", f"/grupos/solicitudes/{ID90}/decision", t2, idem=True, cuerpo={"decision": "ACEPTAR"})
print("-- el organizador (USR1) acepta a USR90")
k = str(uuid.uuid4())
e, ok = llamar("POST", f"/grupos/solicitudes/{ID90}/decision", t1, idem=k, cuerpo={"decision": "ACEPTAR"})
print("-- repetir la MISMA decision: devuelve lo ya resuelto")
e, ok2 = llamar("POST", f"/grupos/solicitudes/{ID90}/decision", t1, idem=k, cuerpo={"decision": "ACEPTAR"})
print("mismo participante:", ok.get("participanteId") == ok2.get("participanteId"))
print("-- decidir lo CONTRARIO de lo ya resuelto")
llamar("POST", f"/grupos/solicitudes/{ID90}/decision", t1, idem=True, cuerpo={"decision": "RECHAZAR", "motivo": "ahora no"})
print("-- rechazar sin motivo")
llamar("POST", f"/grupos/solicitudes/{ID4}/decision", t1, idem=True, cuerpo={"decision": "RECHAZAR"})
print("-- rechazar con motivo")
llamar("POST", f"/grupos/solicitudes/{ID4}/decision", t1, idem=True, cuerpo={"decision": "RECHAZAR", "motivo": "no cumple el perfil del grupo"})
print("SQL solicitudes:", sql(f"select estado||'='||count(*) from grupos.solicitud_ingreso where grupo_id='{G3}' group by estado").replace("\n", " ; "))
print("SQL participante aceptado:", sql(f"select estado from grupos.participante where grupo_id='{G3}' and usuario_id='{U90}'"))
print("SQL cupos:", sql(f"select estado||'='||count(*) from grupos.cupo where grupo_id='{G3}' group by estado").replace("\n", " ; "), "· cupos_ocupados:", sql(f"select cupos_ocupados from grupos.grupo where id='{G3}'"))
print("SQL revisor y fecha:", sql(f"select (revisada_por is not null)::text||' '||(fecha_resolucion is not null)::text from grupos.solicitud_ingreso where id='{ID90}'"))
print("SQL eventos:", sql(f"select tipo||'='||count(*) from grupos.evento_dominio where agregado_id in ('{ID90}','{ID4}') group by tipo").replace("\n", " ; "))

print("\n===== B36 · mis participaciones (lo que usa la pestaña Grupos de la app)")
e, mias = llamar("GET", "/grupos/participaciones", t90)
print("USR90 ve su participacion:", [(m['estado']) for m in mias] if isinstance(mias, list) else mias)
e, otras = llamar("GET", "/grupos/participaciones", t1)
print("USR1 ve las suyas:", len(otras) if isinstance(otras, list) else otras)

print("\n===== B37 · evidencia step-up (identidad firma, JWKS publico verifica)")
e, d = llamar("POST", "/sesiones/desafios", t1, idem=True, cuerpo={"proposito": "RETIRO"})
e, ev = llamar("POST", f"/sesiones/desafios/{d['desafioId']}/verificacion", t1, cuerpo={"factor": {"tipo": "TOTP", "valor": "000000"}}, mostrar=False)
print("estado:", e, "· claves de la respuesta:", sorted(ev.keys()) if isinstance(ev, dict) else ev)
if isinstance(ev, dict) and "evidencia" in ev:
    c = claims(ev["evidencia"])
    print("claims (sin el token):", {k: c[k] for k in ("iss", "aud", "proposito", "acr", "amr") if k in c}, "· exp-iat =", c["exp"] - c["iat"], "s")
    print("sub es USR1:", c["sub"] == uno("select id from identidad.usuario where codigo_publico='USR000001'"))
    print("firma RS256 verifica con el JWKS publico de identidad:", rs256_ok(ev["evidencia"], jwks()))
print("-- el mismo desafio, segunda vez")
llamar("POST", f"/sesiones/desafios/{d['desafioId']}/verificacion", t1, cuerpo={"factor": {"tipo": "TOTP", "valor": "000000"}})
print("-- el desafio de USR1 presentado por USR2")
e, d2 = llamar("POST", "/sesiones/desafios", t1, idem=True, cuerpo={"proposito": "RETIRO"}, mostrar=False)
llamar("POST", f"/sesiones/desafios/{d2['desafioId']}/verificacion", t2, cuerpo={"factor": {"tipo": "OTP", "valor": "000000"}})
print("-- sin sesion")
llamar("POST", "/sesiones/desafios", None, idem=True, cuerpo={"proposito": "RETIRO"}, publico=True)
