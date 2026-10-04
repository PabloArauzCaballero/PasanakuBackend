"""H11 · CU-64 positivo + CU-63 — traspaso del cupo de USR1 en GRP-DEMO-02 a USR2 (participante ACTIVO), con el acuerdo votado por los OTROS participantes."""
from campania_lib import llamar, login, sql

uno = lambda q: (sql(q).splitlines() or [""])[0]
G2 = uno("select id from grupos.grupo where codigo_publico='GRP-DEMO-02'")
CUPO1 = uno(f"select c.id from grupos.cupo c join grupos.participante p on p.id=c.participante_id join identidad.usuario u on u.id=p.usuario_id where c.grupo_id='{G2}' and u.codigo_publico='USR000001'")
U2 = uno("select id from identidad.usuario where codigo_publico='USR000002'")
t1, t2, t90 = login("USR1"), login("USR2"), login("USR90")
titular = lambda: sql(f"select estado||' titular='||coalesce((select u.codigo_publico from grupos.participante p join identidad.usuario u on u.id=p.usuario_id where p.id=c.participante_id),'-') from grupos.cupo c where id='{CUPO1}'")
print("SQL antes:", titular())
print("-- negativo: USR2 (sin GRUPO_ADMINISTRAR) intenta traspasar")
llamar("POST", f"/grupos/{G2}/cupos/{CUPO1}/traspasos", t2, idem=True, cuerpo={"usuarioEntranteId": U2, "motivo": "ajeno ajeno ajeno"})
print("-- sin acuerdo: el grupo exige votacion")
llamar("POST", f"/grupos/{G2}/cupos/{CUPO1}/traspasos", t1, idem=True, cuerpo={"usuarioEntranteId": U2, "motivo": "Traspaso sintetico de la campania"})
print("-- CU-63: USR1 propone el acuerdo; votan los tres participantes (B38: ningun voto cierra la votacion mientras el quorum sea alcanzable)")
e, ac = llamar("POST", "/acuerdos", t1, idem=True, cuerpo={"grupoId": G2, "tipo": "TRASPASO_CUPO", "descripcion": "Traspaso sintetico del cupo de USR1 a USR2 para la campania de pruebas",
                                                         "referenciaAfectadaId": CUPO1, "diasVotacion": 1})
if not isinstance(ac, dict) or "acuerdoId" not in ac:
    # El recorrido anterior (h3_traspaso_g2) ya dejo UN acuerdo de traspaso ABIERTO para este cupo: con B38 ya no se cierra al
    # primer voto. Se continua esa misma votacion (los votos repetidos dan 422 y se ignoran).
    print("-- ya hay un acuerdo ABIERTO de ese tipo: se continua esa votacion")
    ac = {"acuerdoId": uno(f"select id from grupos.acuerdo where grupo_id='{G2}' and tipo='ADMISION_REEMPLAZO' and estado='ABIERTO' limit 1")}
aid = ac["acuerdoId"]
for etiqueta, tok in (("USR90", t90), ("USR2", t2), ("USR1", t1)):
    ev, vt = llamar("POST", f"/acuerdos/{aid}/votos", tok, idem=True, cuerpo={"voto": "A_FAVOR"}); print(f"  voto de {etiqueta}:", vt)
print("SQL acuerdo:", sql(f"select estado||' a_favor='||votos_a_favor||' en_contra='||votos_en_contra from grupos.acuerdo where id='{aid}'"))
print("-- USR1 traspasa con el acuerdo aprobado")
K = __import__("uuid").uuid4()
e, r = llamar("POST", f"/grupos/{G2}/cupos/{CUPO1}/traspasos", t1, idem=str(K), cuerpo={"usuarioEntranteId": U2, "motivo": "Traspaso sintetico de la campania", "acuerdoId": aid})
print("-- repetir con la misma clave")
llamar("POST", f"/grupos/{G2}/cupos/{CUPO1}/traspasos", t1, idem=str(K), cuerpo={"usuarioEntranteId": U2, "motivo": "Traspaso sintetico de la campania", "acuerdoId": aid})
print("SQL despues:", titular())
print("SQL traspasos:", sql("select motivo||' acuerdo='||(aprobado_por_acuerdo_id is not null)::text||' x'||count(*) from grupos.traspaso_cupo group by motivo, aprobado_por_acuerdo_id is not null"))
