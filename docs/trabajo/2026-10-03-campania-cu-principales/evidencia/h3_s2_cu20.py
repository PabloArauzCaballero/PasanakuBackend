"""H3.S2 — CU-20 crear grupo con el sandbox regulado ACTIVO: ¿responde 201 o AP-CU20-04?"""
import datetime
from campania_lib import llamar, login, sql

ORG = (sql("select o.id from organizador.organizador o join identidad.usuario u on u.id=o.usuario_id where u.codigo_publico='USR000001'").splitlines() or [''])[0]
t = login("USR1")
cuerpo = {
    "nombre": "Campania CU-20 sintetico", "montoAporte": {"monto": "100.00", "moneda": "BOB"},
    "periodicidad": "MENSUAL", "cupos": 3, "diaCobro": 5, "modalidadTurnos": "SORTEO_ALEATORIO",
    "fechaDeInicio": (datetime.date.today() + datetime.timedelta(days=15)).isoformat(),
    "organizadorId": ORG, "permitePermutaDeTurnos": True,
}
est, r = llamar("POST", "/grupos", token=t, cuerpo=cuerpo, idem=True)
print("respuesta:", r)
