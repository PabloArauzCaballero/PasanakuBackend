"""H3.S6 CU-12 — USR1 (comparte GRP-DEMO-01 con USR2) transfiere por alias."""
import uuid
from campania_lib import llamar, login, sql

cuenta = lambda n: sql(f"select id from nucleo_financiero.cuenta_billetera where numero_cuenta='{n}'")
saldo = lambda n: sql(f"select numero_cuenta||' disponible='||saldo_disponible from nucleo_financiero.cuenta_billetera where numero_cuenta='{n}'")
C1 = cuenta("BOB-0000001")
t1, t2 = login("USR1"), login("USR2")
print("saldos antes:", saldo("BOB-0000001"), "·", saldo("BOB-0000002"))
print("-- resolver alias «Juan» con el token de USR1")
llamar("GET", "/grupos/alias/Juan", t1)
K = str(uuid.uuid4())
cuerpo = {"cuentaOrigenId": C1, "destino": {"tipo": "ALIAS", "valor": "Juan"}, "monto": {"monto": "50.00", "moneda": "BOB"}, "concepto": "Campania CU-12 sintetica"}
e, r = llamar("POST", "/billetera/transferencias", t1, idem=K, cuerpo=cuerpo)
print("respuesta:", r)
print("saldos después:", saldo("BOB-0000001"), "·", saldo("BOB-0000002"))
