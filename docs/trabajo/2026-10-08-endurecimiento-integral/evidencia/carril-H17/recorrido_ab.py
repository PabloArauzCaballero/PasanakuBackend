#!/usr/bin/env python3
"""Recorrido A/B LOCAL contra servicios reales (procesos del host), Kafka real y PostgreSQL real.

A (organizador sintetico) crea un grupo e invita a B por ENLACE; B consulta y acepta. Las comprobaciones de estado se
hacen contra la base real. Las credenciales salen de /tmp/claude-0/e2e/cuentas.env (fuera del repo) y NO se imprimen.
Doble declarado: el segundo factor es el de desarrollo del perfil `local` (000000); NO es el de TEST ni produccion.
"""
import json, os, subprocess, sys, time, urllib.request, urllib.error, uuid, datetime

ID = "http://127.0.0.1:18081"; GR = "http://127.0.0.1:18082"; NU = "http://127.0.0.1:18084"
SALIDA = []

def cargar(p):
    for l in open(p):
        if "=" in l:
            k, v = l.strip().split("=", 1); os.environ[k] = v
cargar("/tmp/claude-0/e2e/cuentas.env")

def sql(q):
    r = subprocess.run(["docker", "exec", "-i", "aportaya-pg", "psql", "-X", "-tA", "-U", "pasanaku", "-d", "pasanaku", "-c", q],
                       capture_output=True, text=True)
    return r.stdout.strip()

def http(metodo, url, cuerpo=None, token=None, clave=True):
    h = {"Content-Type": "application/json", "Accept": "application/json"}
    if token: h["Authorization"] = "Bearer " + token
    if clave and metodo == "POST": h["Idempotency-Key"] = clave if isinstance(clave, str) else str(uuid.uuid4())
    req = urllib.request.Request(url, method=metodo, headers=h, data=json.dumps(cuerpo).encode() if cuerpo is not None else None)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            t = r.read().decode(); return r.status, (json.loads(t) if t else {})
    except urllib.error.HTTPError as e:
        t = e.read().decode()
        try: return e.code, json.loads(t)
        except Exception: return e.code, {"crudo": t[:200]}

def paso(nombre, ok, detalle=""):
    SALIDA.append((nombre, ok, detalle)); print(("PASS " if ok else "FAIL ") + nombre + (" · " + detalle if detalle else ""))
    return ok

def login(tel, clave_env, huella):
    c = os.environ[clave_env]
    cuerpo = {"telefonoE164": tel, "credencial": c, "huellaDispositivo": huella, "plataforma": "WEB"}
    s1, r1 = http("POST", ID + "/sesiones", cuerpo, clave=False)
    s2, r2 = http("POST", ID + "/sesiones", {**cuerpo, "factor": {"tipo": "OTP", "valor": "000000"}}, clave=False)
    return s1, r1.get("codigo"), s2, r2.get("tokenAcceso")

def esperar(q, esperado, tope=60):
    t0 = time.time()
    while time.time() - t0 < tope:
        v = sql(q)
        if v == esperado: return v, round(time.time() - t0, 1)
        time.sleep(1)
    return sql(q), round(time.time() - t0, 1)

a1, a1c, a2, ta = login("+59170009101", "CUENTA_A_CLAVE", "h17-a")
paso("login A en dos pasos (credencial -> pide factor -> sesion)", a1 == 422 and a1c == "AP-CU04-03" and a2 == 200 and bool(ta), f"http={a1}/{a1c} luego {a2}")
b1, b1c, b2, tb = login("+59170009102", "CUENTA_B_CLAVE", "h17-b")
paso("login B en dos pasos", b1 == 422 and b1c == "AP-CU04-03" and b2 == 200 and bool(tb), f"http={b1}/{b1c} luego {b2}")
if not (ta and tb): sys.exit(2)

hoy = datetime.date.today() + datetime.timedelta(days=30)
cuerpo = {"nombre": "Grupo ensayo H17", "montoAporte": {"monto": "100.00", "moneda": "BOB"}, "periodicidad": "MENSUAL", "cupos": 3,
          "diaCobro": 5, "modalidadTurnos": "SORTEO_ALEATORIO", "fechaDeInicio": hoy.isoformat()}
s, r = http("POST", GR + "/grupos", cuerpo, ta)
gid = r.get("grupoId")
paso("A crea el grupo (202)", s == 202 and bool(gid), f"http={s} codigo={r.get('codigo')}")
if not gid: print(json.dumps(r)[:300]); sys.exit(3)

est0 = sql(f"select estado from grupos.grupo where id='{gid}'")
paso("el grupo nace BORRADOR o ya abierto (la cadena es asincrona)", est0 in ("BORRADOR", "ABIERTO_A_INSCRIPCION"), f"estado={est0}")
v, seg = esperar(f"select estado from grupos.grupo where id='{gid}'", "ABIERTO_A_INSCRIPCION")
paso("P·H1: grupo_creado -> tarifas congela -> tarifa_congelada -> grupos abre (Kafka real)", v == "ABIERTO_A_INSCRIPCION", f"estado={v} en {seg}s")
n = sql(f"select count(*) from tarifas.tarifa_congelada_grupo where grupo_id='{gid}'")
paso("tarifas dejo una sola tarifa congelada con hash", n == "1" and len(sql(f"select hash_snapshot from tarifas.tarifa_congelada_grupo where grupo_id='{gid}'")) == 64, f"filas={n}")
hist = sql(f"select count(*) from grupos.historial_estado_grupo where grupo_id='{gid}' and estado_nuevo='ABIERTO_A_INSCRIPCION'")
paso("el historial de estado registra la apertura una vez", hist == "1", f"filas={hist}")
cb, seg2 = esperar(f"select count(*) from nucleo_financiero.cuenta_billetera where grupo_id='{gid}'", "1")
paso("Q·DD-8: nucleo abre la billetera del grupo al consumir grupo_creado (Kafka real)", cb == "1", f"filas={cb} en {seg2}s")

s, inv = http("POST", GR + f"/grupos/{gid}/invitaciones", {"canal": "ENLACE", "telefonoInvitado": "+59170009102", "nombreSugerido": "B"}, ta)
paso("P·H2: invitar por ENLACE responde 201 (antes 500 AP-INT-01)", s == 201 and bool(inv.get("token")), f"http={s} codigo={inv.get('codigo')}")
if s != 201: print(json.dumps(inv)[:400]); sys.exit(4)
print("   respuesta de la invitacion (claves):", sorted(inv.keys()), "| enlace:", str(inv.get("enlace"))[:70].replace(inv["token"], "<token>"))
import re
m = re.search(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", inv.get("enlace", "") + " " + str(inv.get("tokenId", "")))
tid = inv.get("tokenId") or (m.group(0) if m else None)
tok = inv["token"]
s, det = http("POST", GR + "/grupos/invitaciones/enlace/consultar", {"tokenId": tid, "token": tok}, tb, clave=False)
paso("B consulta el enlace (200, antes 422 AP-CU69-05 por BORRADOR)", s == 200 and det.get("grupoId") == gid, f"http={s} codigo={det.get('codigo')}")
hr = det.get("hashReglamento")
clave_aceptar = str(uuid.uuid4())
cuerpo_ac = {"tokenId": tid, "token": tok, "hashReglamento": hr, "aceptaReglamento": True}
s, ac = http("POST", GR + "/grupos/invitaciones/enlace/aceptar", cuerpo_ac, tb, clave=clave_aceptar)
paso("B acepta el enlace (201) con participanteId", s == 201 and ac.get("grupoId") == gid and bool(ac.get("participanteId")), f"http={s} codigo={ac.get('codigo')}")
part = sql(f"select count(*) from grupos.participante where grupo_id='{gid}' and id='{ac.get('participanteId')}'")
paso("la participacion existe en la base (persistencia real)", part == "1", f"filas={part}")
# Reintento del MISMO pedido (misma Idempotency-Key): misma respuesta, sin duplicar
s2, ac2 = http("POST", GR + "/grupos/invitaciones/enlace/aceptar", cuerpo_ac, tb, clave=clave_aceptar)
n2 = sql(f"select count(*) from grupos.participante where grupo_id='{gid}' and usuario_id=(select id from identidad.usuario where codigo_publico='SINT-E2E-B')")
paso("reintento con la misma clave: mismo participanteId y una sola participacion", s2 == 201 and ac2.get("participanteId") == ac.get("participanteId") and n2 == "1", f"http={s2} participaciones={n2}")
# Otro pedido (otra clave) sobre el enlace ya usado: el enlace es de un solo uso
s4, ac4 = http("POST", GR + "/grupos/invitaciones/enlace/aceptar", cuerpo_ac, tb)
n4 = sql(f"select count(*) from grupos.participante where grupo_id='{gid}' and usuario_id=(select id from identidad.usuario where codigo_publico='SINT-E2E-B')")
paso("rechaza: otro pedido (otra clave) sobre el enlace ya usado no suma otra participacion", s4 == 422 and n4 == "1", f"http={s4} codigo={ac4.get('codigo')} participaciones={n4}")
# Un tercero no usa el enlace de B
s3, r3 = http("POST", GR + "/grupos/invitaciones/enlace/consultar", {"tokenId": tid, "token": tok}, ta, clave=False)
paso("rechaza: el organizador (otro actor) no consulta el enlace de B", s3 in (403, 422), f"http={s3} codigo={r3.get('codigo')}")

# ---- P·H3: un canje que no puede completarse localmente NO quema el enlace en identidad --------------------------------
cuerpo2 = {**cuerpo, "nombre": "Grupo canje H17"}
s, r = http("POST", GR + "/grupos", cuerpo2, ta); g2 = r.get("grupoId")
esperar(f"select estado from grupos.grupo where id='{g2}'", "ABIERTO_A_INSCRIPCION")
s, po = http("POST", GR + f"/grupos/{g2}/postulaciones", {"cuposSolicitados": 1, "mensaje": "Quiero entrar"}, tb)
paso("B se postula al segundo grupo (queda una postulacion pendiente)", s in (200, 201, 202), f"http={s} codigo={po.get('codigo')}")
s, inv2 = http("POST", GR + f"/grupos/{g2}/invitaciones", {"canal": "ENLACE", "telefonoInvitado": "+59170009102", "nombreSugerido": "B"}, ta)
inv_id = inv2.get("invitacionId"); tok_id = sql(f"select token_id from grupos.invitacion where id='{inv_id}'")
s, cj = http("POST", GR + f"/grupos/{g2}/invitaciones/{inv_id}/canje", {"token": inv2.get("token"), "cuposSolicitados": 1, "mensaje": "canje"}, tb)
paso("rechaza: canje con una postulacion pendiente -> AP-CU68-06", s == 422 and cj.get("codigo") == "AP-CU68-06", f"http={s} codigo={cj.get('codigo')}")
est_tok = sql(f"select estado from identidad.token_verificacion where id='{tok_id}'")
est_inv = sql(f"select estado from grupos.invitacion where id='{inv_id}'")
paso("el enlace NO quedo consumido en identidad y la invitacion sigue ENVIADA (no se quemo)", est_tok != "CONSUMIDO" and est_inv == "ENVIADA", f"token={est_tok} invitacion={est_inv}")
s, rv = http("POST", GR + f"/grupos/{g2}/invitaciones/{inv_id}/revocacion", None, ta)
paso("el organizador todavia puede revocar la invitacion (204)", s == 204, f"http={s} codigo={rv.get('codigo')}")
print(json.dumps({"grupo": gid, "resultado": [(n_, o) for n_, o, _ in SALIDA]}))
sys.exit(0 if all(o for _, o, _ in SALIDA) else 1)
