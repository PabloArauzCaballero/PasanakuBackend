"""H5 — completa GRP-DEMO-02 (invita y acepta a USR2), conforma el grupo y ejecuta el sorteo (CU-60) y su verificacion publica (CU-61)."""
from campania_lib import llamar, login, sql

uno = lambda q: (sql(q).splitlines() or [""])[0]
G2 = uno("select id from grupos.grupo where codigo_publico='GRP-DEMO-02'")
HASH2 = uno("select hash_contenido from grupos.reglamento_grupo r join grupos.grupo g on g.id=r.grupo_id where g.codigo_publico='GRP-DEMO-02'")
t1, t2 = login("USR1"), login("USR2")
print("estado del grupo antes:", sql(f"select estado||' ocupados='||cupos_ocupados||'/'||cupos_totales from grupos.grupo where id='{G2}'"))

print("\n== invitar y aceptar al ultimo participante (USR2)")
e, inv = llamar("POST", f"/grupos/{G2}/invitaciones", t1, idem=True, cuerpo={"telefonoInvitado": "+59171000002", "canal": "ENLACE"})
tok_id, tok = inv["enlace"].split("unirse/", 1)[1].split(".", 1)
e, ac = llamar("POST", "/grupos/invitaciones/enlace/aceptar", t2, idem=True,
               cuerpo={"tokenId": tok_id, "token": tok, "hashReglamento": HASH2, "aceptaReglamento": True})
print("aceptar:", ac)
print("estado del grupo despues:", sql(f"select estado||' ocupados='||cupos_ocupados||'/'||cupos_totales from grupos.grupo where id='{G2}'"))
print("cupos:", sql(f"select estado||'='||count(*) from grupos.cupo where grupo_id='{G2}' group by estado").replace("\n", " ; "))

print("\n== CU-60 comprometer el sorteo")
print("-- negativo: USR2 (sin GRUPO_ADMINISTRAR)")
llamar("POST", f"/grupos/{G2}/sorteo", t2, idem=True, cuerpo={})
e, comp = llamar("POST", f"/grupos/{G2}/sorteo", t1, idem=True, cuerpo={})
print("respuesta:", {k: (v if k != "semilla" else "<oculta>") for k, v in comp.items()} if isinstance(comp, dict) else comp)
print("SQL sorteo:", sql(f"select estado from grupos.sorteo_turnos where grupo_id='{G2}'"))
if isinstance(comp, dict) and comp.get("sorteoId"):
    sid = comp["sorteoId"]
    sem = comp["semilla"]
    print("\n== CU-60 revelar el sorteo")
    e, rev = llamar("POST", f"/grupos/{G2}/sorteo/revelacion", t1, idem=True, cuerpo={"sorteoId": sid, "semilla": sem})
    print("respuesta:", rev)
    print("\n== CU-61 verificacion publica (sin sesion)")
    e, ver = llamar("GET", f"/publico/sorteos/{sid}/verificacion", publico=True)
    print("respuesta:", {k: v for k, v in ver.items() if k != "paquete"} if isinstance(ver, dict) else ver)
    print("turnos:", sql(f"select orden_asignado||':'||estado from grupos.turno where grupo_id='{G2}' order by orden_asignado").replace("\n", " "))
