from campania_lib import llamar, login, sql
u3 = sql("select id from identidad.usuario where codigo_publico='USR000003'").splitlines()[0]
t = login("USR3")
e, r = llamar("GET", f"/reputacion/{u3}/puntaje", t)
print("puntaje:", r)
print("min grupo:", sql("select reputacion_minima||' kyc='||requiere_kyc_minimo from grupos.grupo where codigo_publico='GRP-DEMO-02'"))
