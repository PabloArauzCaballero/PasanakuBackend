from campania_lib import llamar, login, sql
ORG = (sql("select o.id from organizador.organizador o join identidad.usuario u on u.id=o.usuario_id where u.codigo_publico='USR000001'").splitlines() or [''])[0]
t = login("USR1")
est, r = llamar("GET", f"/organizadores/{ORG}/habilitacion", token=t)
print("habilitacion:", r)
