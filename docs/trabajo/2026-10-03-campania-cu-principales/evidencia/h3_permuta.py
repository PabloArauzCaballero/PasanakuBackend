"""H3.S10 CU-62 — permuta entre turnos PROGRAMADO (cupo 3 de USR3 y cupo 4 de USR4)."""
from campania_lib import llamar, login, sql

uno = lambda q: (sql(q).splitlines() or [""])[0]
G1 = uno("select id from grupos.grupo where codigo_publico='GRP-DEMO-01'")
turno = lambda n: uno(f"select t.id from grupos.turno t join grupos.cupo c on c.id=t.cupo_id where t.grupo_id='{G1}' and c.numero={n}")
part = lambda u: uno(f"select p.id from grupos.participante p join grupos.grupo g on g.id=p.grupo_id join identidad.usuario x on x.id=p.usuario_id where g.codigo_publico='GRP-DEMO-01' and x.codigo_publico='{u}'")
t3, t5 = login("USR3"), login("USR5")
cuerpo = {"turnoOrigenId": turno(3), "turnoDestinoId": turno(4), "contraparteId": part("USR000004"), "motivo": "Campania sintetica"}
print("-- negativo: USR5 (dueño del turno 5) intenta permutar los turnos 3 y 4 que no son suyos")
llamar("POST", "/turnos/permutas", t5, idem=True, cuerpo=cuerpo)
print("-- USR3 solicita la permuta de su turno 3 con el turno 4 de USR4")
e, r = llamar("POST", "/turnos/permutas", t3, idem=True, cuerpo=cuerpo)
print("respuesta:", r)
print("SQL permutas:", sql("select count(*) from grupos.solicitud_permuta"))
