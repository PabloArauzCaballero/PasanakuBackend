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


import subprocess as sp
L = "docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-H17/levantar_local.sh"
def servicio(accion, nombre):
    if accion == "parar":
        pid = open(f"/tmp/claude-0/e2e/{nombre}.pid").read().strip(); sp.run(["kill", pid]); time.sleep(4)
    else:
        sp.run(["bash", L, "reiniciar", nombre], capture_output=True)
a1, a1c, a2, ta = login("+59170009101", "CUENTA_A_CLAVE", "h17-a")
b1, b1c, b2, tb = login("+59170009102", "CUENTA_B_CLAVE", "h17-b")
hoy = datetime.date.today() + datetime.timedelta(days=30)
cuerpo = {"nombre": "Grupo caida tarifas", "montoAporte": {"monto": "100.00", "moneda": "BOB"}, "periodicidad": "MENSUAL", "cupos": 3,
          "diaCobro": 5, "modalidadTurnos": "SORTEO_ALEATORIO", "fechaDeInicio": hoy.isoformat()}

# 1) tarifas CAIDO: crear el grupo falla CERRADO (AP-CU20-01) y no deja un grupo a medias
servicio("parar", "tarifas")
antes = sql("select count(*) from grupos.grupo")
s, r = http("POST", GR + "/grupos", cuerpo, ta)
despues = sql("select count(*) from grupos.grupo")
paso("rechaza: con tarifas apagado crear el grupo falla cerrado (422 AP-CU20-01) y no escribe nada", s == 422 and r.get("codigo") == "AP-CU20-01" and antes == despues, f"http={s} codigo={r.get('codigo')} grupos {antes}->{despues}")
servicio("reiniciar", "tarifas")
time.sleep(5)

# 2) grupos MUERE justo despues de crear (kill -9, antes de que su relevo publique el outbox): el evento no se pierde
s, r = http("POST", GR + "/grupos", {**cuerpo, "nombre": "Grupo crash outbox"}, ta); g = r.get("grupoId")
pid = open("/tmp/claude-0/e2e/grupos.pid").read().strip(); sp.run(["kill", "-9", pid]); time.sleep(3)
pend = sql(f"select estado from grupos.evento_dominio where tipo='grupos.grupo_creado' and agregado_id='{g}'")
est = sql(f"select estado from grupos.grupo where id='{g}'")
paso("grupos muerto con el alta ya confirmada: el evento queda en el outbox y el grupo en BORRADOR", s == 202 and pend in ("PENDIENTE", "TOMADO") and est == "BORRADOR", f"http={s} outbox={pend} grupo={est}")
servicio("reiniciar", "grupos")
v, seg = esperar(f"select estado from grupos.grupo where id='{g}'", "ABIERTO_A_INSCRIPCION", 90)
paso("al volver grupos el outbox publica, tarifas congela y el grupo se abre solo (nada se perdio)", v == "ABIERTO_A_INSCRIPCION", f"estado={v} a los {seg}s de volver")
n = sql(f"select count(*) from tarifas.tarifa_congelada_grupo where grupo_id='{g}'")
cb = sql(f"select count(*) from nucleo_financiero.cuenta_billetera where grupo_id='{g}'")
paso("y queda una sola tarifa congelada y una sola billetera de grupo", n == "1" and cb == "1", f"tarifas={n} billeteras={cb}")

# 3) garantia CAIDO: la postulacion falla CERRADA (no se sabe si tiene restriccion => se asume que si)
s, r2 = http("POST", GR + "/grupos", {**cuerpo, "nombre": "Grupo caida garantia"}, ta); g2 = r2.get("grupoId")
esperar(f"select estado from grupos.grupo where id='{g2}'", "ABIERTO_A_INSCRIPCION", 60)
servicio("parar", "garantia")
s, p = http("POST", GR + f"/grupos/{g2}/postulaciones", {"cuposSolicitados": 1, "mensaje": "x"}, tb)
paso("rechaza: con garantia apagado la postulacion falla cerrada (AP-CU68-01), no deja pasar a ciegas", s == 422 and p.get("codigo") == "AP-CU68-01", f"http={s} codigo={p.get('codigo')}")
cnt = sql(f"select count(*) from grupos.solicitud_ingreso where grupo_id='{g2}'")
paso("y no quedo ninguna solicitud escrita", cnt == "0", f"filas={cnt}")
servicio("reiniciar", "garantia")
time.sleep(8)
s, p = http("POST", GR + f"/grupos/{g2}/postulaciones", {"cuposSolicitados": 1, "mensaje": "x"}, tb)
paso("al volver garantia la misma postulacion entra (201)", s in (200, 201, 202), f"http={s} codigo={p.get('codigo')}")
sys.exit(0 if all(o for _, o, _ in SALIDA) else 1)
