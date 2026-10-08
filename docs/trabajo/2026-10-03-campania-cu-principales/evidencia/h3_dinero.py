"""H3.S5 CU-10 recargar + H3.S6 CU-12 transferir — API + SQL (la app aporta la captura en otro paso)."""
import uuid
from campania_lib import llamar, login, sql

cuenta = lambda n: sql(f"select id from nucleo_financiero.cuenta_billetera where numero_cuenta='{n}'")
C90 = cuenta("BOB-0000090")
C2 = cuenta("BOB-0000002")
saldo = lambda n: sql(f"select numero_cuenta||' disponible='||saldo_disponible from nucleo_financiero.cuenta_billetera where numero_cuenta='{n}'")

t90, t2, t8 = login("USR90"), login("USR2"), login("USR8", "000000")
print("\n== CU-10 · saldo antes:", saldo("BOB-0000090"))
est, orden = llamar("POST", "/billetera/recargas", t90, idem=True, cuerpo={
    "cuentaBilleteraId": C90, "monto": {"monto": "200.00", "moneda": "BOB"}, "medio": "QR_INTEROPERABLE"})
print("orden:", {k: orden.get(k) for k in ("ordenRecargaId", "estado", "acreditara")} if isinstance(orden, dict) else orden)
oid = orden.get("ordenRecargaId") if isinstance(orden, dict) else None
if oid:
    print("-- negativo: USR2 (participante ajeno) intenta acreditar la orden de USR90")
    llamar("POST", f"/billetera/recargas/{oid}/acreditacion", t2, idem=True)
    print("-- negativo: USR90, TITULAR de la orden, intenta acreditarsela a si mismo (B20)")
    llamar("POST", f"/billetera/recargas/{oid}/acreditacion", t90, idem=True)
    print("-- acreditar (TESORERIA, USR8) con Idempotency-Key K")
    K = str(uuid.uuid4())  # el contrato exige UUID
    e1, r1 = llamar("POST", f"/billetera/recargas/{oid}/acreditacion", t8, idem=K)
    print("   respuesta 1:", r1)
    print("-- repetir la MISMA clave K")
    e2, r2 = llamar("POST", f"/billetera/recargas/{oid}/acreditacion", t8, idem=K)
    print("   respuesta 2:", r2)
    print("-- repetir con clave NUEVA (la orden ya esta acreditada)")
    llamar("POST", f"/billetera/recargas/{oid}/acreditacion", t8, idem=True)
    print("SQL orden:", sql(f"select estado||' bruto='||monto_bruto||' costo='||costo_proveedor||' acreditado='||monto_acreditado from nucleo_financiero.orden_recarga where id='{oid}'"))
    tx = sql(f"select transaccion_id from nucleo_financiero.orden_recarga where id='{oid}'")
    print("SQL transacciones con esa orden:", sql(f"select count(*) from nucleo_financiero.transaccion_billetera where origen_id='{oid}'"))
    print("SQL patas del libro (sentido, monto):", sql(f"select sentido||' '||monto from nucleo_financiero.movimiento_billetera where transaccion_id='{tx}' order by orden"))
    print("SQL cuadre de la transacción (suma por sentido):", sql(f"select sentido||'='||sum(monto) from nucleo_financiero.movimiento_billetera where transaccion_id='{tx}' group by sentido order by sentido"))
    print("SQL asiento contable enlazado:", sql(f"select coalesce(asiento_contable_id::text,'NULL') from nucleo_financiero.transaccion_billetera where id='{tx}'"))
print("== CU-10 · saldo después:", saldo("BOB-0000090"))

print("\n== CU-12 · saldos antes:", saldo("BOB-0000090"), "·", saldo("BOB-0000002"))
K2 = str(uuid.uuid4())
e, tr = llamar("POST", "/billetera/transferencias", t90, idem=K2, cuerpo={
    "cuentaOrigenId": C90, "destino": {"tipo": "ALIAS", "valor": "Juan"},
    "monto": {"monto": "50.00", "moneda": "BOB"}, "concepto": "Campania CU-12 sintetica"})
print("transferencia:", tr)
print("-- repetir la misma clave"); e2, tr2 = llamar("POST", "/billetera/transferencias", t90, idem=K2, cuerpo={
    "cuentaOrigenId": C90, "destino": {"tipo": "ALIAS", "valor": "Juan"},
    "monto": {"monto": "50.00", "moneda": "BOB"}, "concepto": "Campania CU-12 sintetica"})
print("   respuesta 2:", tr2)
print("-- negativo: USR2 transfiere desde la cuenta de USR90")
llamar("POST", "/billetera/transferencias", t2, idem=True, cuerpo={
    "cuentaOrigenId": C90, "destino": {"tipo": "ALIAS", "valor": "Juan"},
    "monto": {"monto": "1.00", "moneda": "BOB"}, "concepto": "ajena"})
print("-- negativo: saldo insuficiente")
llamar("POST", "/billetera/transferencias", t90, idem=True, cuerpo={
    "cuentaOrigenId": C90, "destino": {"tipo": "ALIAS", "valor": "Juan"},
    "monto": {"monto": "99999.00", "moneda": "BOB"}, "concepto": "sin fondos"})
print("== CU-12 · saldos después:", saldo("BOB-0000090"), "·", saldo("BOB-0000002"))
print("SQL transacciones TRANSFERENCIA por la clave:", sql(f"select count(*) from nucleo_financiero.transaccion_billetera where clave_idempotencia = '{K2}'"))
