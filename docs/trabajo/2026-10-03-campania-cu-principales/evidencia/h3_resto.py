"""H3 — CU-90 (cuerpo corregido), CU-61 verificar sorteo, CU-62 permuta, CU-22 entrega, CU-11 retiro (API + SQL)."""
import datetime
import uuid
from campania_lib import llamar, login, sql

uno = lambda q: (sql(q).splitlines() or [""])[0]
G1 = uno("select id from grupos.grupo where codigo_publico='GRP-DEMO-01'")
KYC90 = uno("select v.id from identidad.verificacion_kyc v join identidad.usuario u on u.id=v.usuario_id where u.codigo_publico='USR000090' limit 1")
SORTEO = uno("select s.id from grupos.sorteo_turnos s join grupos.grupo g on g.id=s.grupo_id where g.codigo_publico='GRP-DEMO-01'")
part = lambda u: uno(f"select p.id from grupos.participante p join grupos.grupo g on g.id=p.grupo_id join identidad.usuario x on x.id=p.usuario_id where g.codigo_publico='GRP-DEMO-01' and x.codigo_publico='{u}'")
turno = lambda n: uno(f"select t.id from grupos.turno t join grupos.cupo c on c.id=t.cupo_id where t.grupo_id='{G1}' and c.numero={n}")
cupo = lambda n: uno(f"select id from grupos.cupo where grupo_id='{G1}' and numero={n}")
PER3 = uno(f"select id from grupos.periodo where grupo_id='{G1}' and numero=3")
C1 = uno("select id from nucleo_financiero.cuenta_billetera where numero_cuenta='BOB-0000001'")
INSTR1 = uno("select i.id from nucleo_financiero.instrumento_fondeo i join identidad.usuario u on u.id=i.usuario_id where u.codigo_publico='USR000001' and i.estado_verificacion='VERIFICADO' limit 1")

t90, t1, t2, t8, t9 = login("USR90"), login("USR1"), login("USR2"), login("USR8", "000000"), login("USR9", "000000")

print("\n===== CU-90 · USR90 postula (cuerpo corregido: reputacion con formato decimal)")
e, r = llamar("POST", "/organizadores/postulaciones", t90, idem=True, cuerpo={
    "motivacion": "Campania sintetica", "experienciaDeclarada": "Sin experiencia previa (dato sintetico)",
    "kycReforzadoId": KYC90, "reputacion": "0.00", "medidos": {}})
print("respuesta:", r)
print("SQL solicitudes de organizador:", sql("select estado||'='||count(*) from organizador.solicitud_organizador group by estado").replace("\n", " ; "))

print("\n===== CU-61 · verificación pública del sorteo sembrado de GRP-DEMO-01 (sin sesión)")
e, v = llamar("GET", f"/publico/sorteos/{SORTEO}/verificacion", publico=True)
print("respuesta:", v)

print("\n===== CU-62 · USR2 solicita permuta de su turno con el de USR3")
e, pm = llamar("POST", "/turnos/permutas", t2, idem=True, cuerpo={
    "turnoOrigenId": turno(2), "turnoDestinoId": turno(3), "contraparteId": part("USR000003"), "motivo": "Campania sintetica"})
print("respuesta:", pm)
print("-- negativo: USR1 solicita permuta de un turno que no es suyo (cupo 2)")
llamar("POST", "/turnos/permutas", t1, idem=True, cuerpo={
    "turnoOrigenId": turno(2), "turnoDestinoId": turno(3), "contraparteId": part("USR000003"), "motivo": "ajeno"})
print("SQL permutas:", sql("select count(*) from grupos.solicitud_permuta"))

print("\n===== CU-22 · entrega del fondo: liquidar (USR8) -> autorizar (USR9) -> ejecutar (USR8)")
fecha = (datetime.date.today() + datetime.timedelta(days=3)).isoformat()
liq = {"grupoId": G1, "periodoId": PER3, "turnoId": turno(2), "cupoId": cupo(2), "beneficiarioId": part("USR000002"),
       "bruto": {"monto": "3000.00", "moneda": "BOB"}, "recaudado": {"monto": "3000.00", "moneda": "BOB"},
       "deducciones": [], "metodoDesembolso": "BILLETERA_MOVIL", "fechaProgramada": fecha}
print("-- negativo: USR9 (solo autoriza) intenta liquidar")
llamar("POST", "/entregas", t9, idem=True, cuerpo=liq)
print("-- negativo: bruto mayor que lo recaudado")
llamar("POST", "/entregas", t8, idem=True, cuerpo={**liq, "bruto": {"monto": "3500.00", "moneda": "BOB"}})
K = str(uuid.uuid4())
e, en = llamar("POST", "/entregas", t8, idem=K, cuerpo=liq)
print("liquidar:", en)
print("-- repetir la MISMA clave")
e2, en2 = llamar("POST", "/entregas", t8, idem=K, cuerpo=liq)
print("liquidar (2.ª):", en2)
eid = (en or {}).get("entregaId") if isinstance(en, dict) else None
if eid:
    print("-- negativo: USR8 (ejecutor) intenta autorizar")
    llamar("POST", f"/entregas/{eid}/autorizacion", t8, idem=True, cuerpo={})
    print("-- USR9 autoriza")
    ea, au = llamar("POST", f"/entregas/{eid}/autorizacion", t9, idem=True, cuerpo={})
    print("autorizar:", au)
    print("-- USR8 ejecuta")
    ee, ej = llamar("POST", f"/entregas/{eid}/ejecucion", t8, idem=True, cuerpo={"montoEntregado": {"monto": "2989.50", "moneda": "BOB"}})
    print("ejecutar:", ej)
    print("SQL entrega:", sql(f"select estado||' bruto='||monto_bolsa_bruto||' neto='||monto_neto_a_entregar||' entregado='||coalesce(monto_efectivamente_entregado::text,'-') from entregas.entrega_fondo where id='{eid}'"))
print("SQL saldo del beneficiario (no se acredita: el servicio no mueve saldo):", sql("select saldo_disponible from nucleo_financiero.cuenta_billetera where numero_cuenta='BOB-0000002'"))

print("\n===== CU-11 · USR1 solicita un retiro de 100.00 a su instrumento verificado")
K = str(uuid.uuid4())
cuerpo = {"cuentaBilleteraId": C1, "monto": {"monto": "100.00", "moneda": "BOB"}, "instrumentoDestinoId": INSTR1}
e, rt = llamar("POST", "/billetera/retiros", t1, idem=K, cuerpo=cuerpo)
print("respuesta:", rt)
print("-- negativo: USR2 retira de la cuenta de USR1")
llamar("POST", "/billetera/retiros", t2, idem=True, cuerpo=cuerpo)
print("SQL órdenes de retiro:", sql("select estado||'='||count(*) from nucleo_financiero.orden_retiro group by estado").replace("\n", " ; "))
print("SQL saldo USR1:", sql("select saldo_disponible||' retenido='||saldo_retenido from nucleo_financiero.cuenta_billetera where numero_cuenta='BOB-0000001'"))
