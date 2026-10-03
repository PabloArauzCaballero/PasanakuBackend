"""H3.S9 CU-64 + CU-63 — traspaso del cupo 1 de GRP-DEMO-02 a USR90, con el acuerdo votado que el grupo exige."""
from campania_lib import llamar, login, sql

uno = lambda q: (sql(q).splitlines() or [""])[0]
G2 = uno("select id from grupos.grupo where codigo_publico='GRP-DEMO-02'")
CUPO1 = uno(f"select id from grupos.cupo where grupo_id='{G2}' and numero=1")
U90 = uno("select id from identidad.usuario where codigo_publico='USR000090'")
t1, t2 = login("USR1"), login("USR2")
titular = lambda: sql(f"select estado||' titular='||coalesce(participante_id::text,'-') from grupos.cupo where id='{CUPO1}'")
print("SQL antes:", titular())
print("-- negativo: USR2 (sin GRUPO_ADMINISTRAR) intenta traspasar")
llamar("POST", f"/grupos/{G2}/cupos/{CUPO1}/traspasos", t2, idem=True, cuerpo={"usuarioEntranteId": U90, "motivo": "ajeno"})
print("-- sin acuerdo: el grupo exige votación")
llamar("POST", f"/grupos/{G2}/cupos/{CUPO1}/traspasos", t1, idem=True, cuerpo={"usuarioEntranteId": U90, "motivo": "Campania sintetica"})
print("-- CU-63: USR1 propone un acuerdo de traspaso")
e, ac = llamar("POST", "/acuerdos", t1, idem=True, cuerpo={"grupoId": G2, "tipo": "TRASPASO_CUPO", "descripcion": "Traspaso sintetico del cupo 1 a USR90 para la campania de pruebas",
                                                         "referenciaAfectadaId": CUPO1, "diasVotacion": 1})
print("respuesta:", ac)
aid = (ac or {}).get("acuerdoId") if isinstance(ac, dict) else None
if aid:
    print("-- USR1 vota A_FAVOR (único participante: alcanza el quórum)")
    ev, vt = llamar("POST", f"/acuerdos/{aid}/votos", t1, idem=True, cuerpo={"voto": "A_FAVOR"})
    print("respuesta:", vt)
    print("SQL acuerdo:", sql(f"select estado from grupos.acuerdo_grupo where id='{aid}'") or sql("select estado from grupos.acuerdo limit 1"))
    print("-- USR1 traspasa con el acuerdo")
    e, r = llamar("POST", f"/grupos/{G2}/cupos/{CUPO1}/traspasos", t1, idem=True, cuerpo={"usuarioEntranteId": U90, "motivo": "Campania sintetica", "acuerdoId": aid})
    print("respuesta:", r)
print("SQL después:", titular())
print("SQL traspasos:", sql("select count(*) from grupos.traspaso_cupo"))
