"""H3 — CU-90 postular, CU-69 invitar/aceptar, CU-68 postular, CU-60 sorteo, CU-21 aportar, CU-64 traspasar (API + SQL).

Cada paso imprime estado HTTP y código de error del contrato. Los pasos no se detienen ante un fallo:
la campaña clasifica cada resultado; no lo oculta.
"""
import uuid
from campania_lib import llamar, login, sql

uno = lambda q: sql(q).splitlines()[0] if sql(q) else ""
G1 = uno("select id from grupos.grupo where codigo_publico='GRP-DEMO-01'")
G2 = uno("select id from grupos.grupo where codigo_publico='GRP-DEMO-02'")
KYC90 = uno("select v.id from identidad.verificacion_kyc v join identidad.usuario u on u.id=v.usuario_id where u.codigo_publico='USR000090' limit 1")
U90 = uno("select id from identidad.usuario where codigo_publico='USR000090'")
CUPO6 = uno("select c.id from grupos.cupo c join grupos.grupo g on g.id=c.grupo_id where g.codigo_publico='GRP-DEMO-01' and c.numero=6")
OBL = uno("select id from aportes.obligacion_aporte where estado='PENDIENTE'")
HASH2 = uno("select hash_contenido from grupos.reglamento_grupo r join grupos.grupo g on g.id=r.grupo_id where g.codigo_publico='GRP-DEMO-02'")

t90, t1, t2, t3, t91 = login("USR90"), login("USR1"), login("USR2"), login("USR3"), login("USR91", "000000")

print("\n===== CU-90 · USR90 postula a organizador")
e, r = llamar("POST", "/organizadores/postulaciones", t90, idem=True, cuerpo={
    "motivacion": "Campania sintetica", "experienciaDeclarada": "Sin experiencia previa (dato sintetico)",
    "kycReforzadoId": KYC90, "reputacion": "NINGUNA", "medidos": {}})
print("respuesta:", r)
print("SQL solicitudes:", sql("select count(*) from organizador.solicitud_organizador"))

print("\n===== CU-69 · USR1 invita a USR90 a GRP-DEMO-02 (ENLACE)")
e, inv = llamar("POST", f"/grupos/{G2}/invitaciones", t1, idem=True, cuerpo={"telefonoInvitado": "+59171000090", "canal": "ENLACE"})
print("respuesta:", {k: (v if k != "token" else "<oculto>") for k, v in inv.items()} if isinstance(inv, dict) else inv)
tok_id = (inv or {}).get("tokenId") if isinstance(inv, dict) else None
tok = (inv or {}).get("token") if isinstance(inv, dict) else None
print("SQL invitaciones:", sql("select count(*) from grupos.invitacion_grupo"))
if tok_id and tok:
    print("-- USR90 consulta la invitación")
    llamar("POST", "/grupos/invitaciones/enlace/consultar", t90, cuerpo={"tokenId": tok_id, "token": tok})
    print("-- USR2 intenta aceptar la invitación de USR90 (negativo: token de otro)")
    llamar("POST", "/grupos/invitaciones/enlace/aceptar", t2, idem=True, cuerpo={"tokenId": tok_id, "token": tok, "hashReglamento": HASH2, "aceptaReglamento": True})
    print("-- USR90 acepta")
    e, ac = llamar("POST", "/grupos/invitaciones/enlace/aceptar", t90, idem=True, cuerpo={"tokenId": tok_id, "token": tok, "hashReglamento": HASH2, "aceptaReglamento": True})
    print("respuesta:", ac)
else:
    print("(sin token de enlace en la respuesta: no se puede continuar con aceptar)")
print("SQL cupos GRP-DEMO-02:", sql(f"select estado||'='||count(*) from grupos.cupo where grupo_id='{G2}' group by estado order by estado").replace("\n", " ; "))
print("SQL participantes GRP-DEMO-02:", sql(f"select count(*) from grupos.participante where grupo_id='{G2}'"))

print("\n===== CU-68 · USR3 postula a GRP-DEMO-02")
llamar("POST", f"/grupos/{G2}/postulaciones", t3, idem=True, cuerpo={"cuposSolicitados": 1, "mensaje": "Campania sintetica"})
print("SQL solicitudes de ingreso:", sql("select estado||'='||count(*) from grupos.solicitud_ingreso group by estado").replace("\n", " ; "))

print("\n===== CU-60 · USR1 compromete el sorteo de GRP-DEMO-02 (grupo con cupos libres: se espera rechazo)")
llamar("POST", f"/grupos/{G2}/sorteo", t1, idem=True, cuerpo={})
print("-- negativo: USR2 (sin GRUPO_ADMINISTRAR) intenta sortear")
llamar("POST", f"/grupos/{G2}/sorteo", t2, idem=True, cuerpo={})
print("SQL sorteos GRP-DEMO-02:", sql(f"select count(*) from grupos.sorteo_turnos where grupo_id='{G2}'"))

print("\n===== CU-21 · USR2 paga la obligación PENDIENTE")
cuerpo = {"monto": {"monto": "500.00", "moneda": "BOB"}, "canal": "BILLETERA_MOVIL", "referenciaProveedor": "campania-sintetica-0001"}
K = str(uuid.uuid4())
print("-- negativo (ANTES de pagar): monto mayor al pendiente")
llamar("POST", f"/aportes/obligaciones/{OBL}/pagos", t2, idem=True, cuerpo={**cuerpo, "monto": {"monto": "9999.00", "moneda": "BOB"}})
print("-- negativo (ANTES de pagar): USR3 paga la obligación de USR2")
llamar("POST", f"/aportes/obligaciones/{OBL}/pagos", t3, idem=True, cuerpo=cuerpo)
print("SQL pagos tras los negativos (debe seguir en 0):", sql(f"select count(*) from aportes.pago where obligacion_id='{OBL}'"))
e, pago = llamar("POST", f"/aportes/obligaciones/{OBL}/pagos", t2, idem=K, cuerpo=cuerpo)
print("respuesta:", pago)
print("-- repetir la MISMA clave")
e2, pago2 = llamar("POST", f"/aportes/obligaciones/{OBL}/pagos", t2, idem=K, cuerpo=cuerpo)
print("respuesta 2:", pago2)
print("SQL obligación:", sql(f"select estado||' pagado='||monto_pagado from aportes.obligacion_aporte where id='{OBL}'"))
print("SQL pagos de la obligación:", sql(f"select count(*) from aportes.pago where obligacion_id='{OBL}'"))
print("SQL saldo USR2 (no debe moverse: el servicio no debita):", sql("select saldo_disponible from nucleo_financiero.cuenta_billetera where numero_cuenta='BOB-0000002'"))

print("\n===== CU-64 · USR1 traspasa el cupo 6 de GRP-DEMO-01 a USR90")
e, tr = llamar("POST", f"/grupos/{G1}/cupos/{CUPO6}/traspasos", t1, idem=True, cuerpo={"usuarioEntranteId": U90, "motivo": "Campania sintetica"})
print("respuesta:", tr)
print("-- negativo: USR2 intenta traspasar")
llamar("POST", f"/grupos/{G1}/cupos/{CUPO6}/traspasos", t2, idem=True, cuerpo={"usuarioEntranteId": U90, "motivo": "ajeno"})
print("SQL traspasos:", sql("select count(*) from grupos.traspaso_cupo"))

print("\n===== CU-74 · evaluar insignias con token de backoffice (se espera 403 por SOPORTE)")
llamar("POST", "/reputacion/insignias/evaluacion", t91, idem=True, cuerpo={})
