"""B32 en runtime: la cuenta desechable (+59171000195) se aprueba con la API del backoffice y pasa a ACTIVO; rechazar no la cambia."""
import subprocess
import sys

from campania_lib import llamar, login, sql

uno = lambda q: (sql(q).splitlines() or [""])[0]
if not uno("select id from identidad.usuario where telefono_e164='+59171000195'"):
    subprocess.run([sys.executable, "h7_cuenta_desechable.py"], check=False)
U = uno("select id from identidad.usuario where telefono_e164='+59171000195'")
V = uno(f"select id from identidad.verificacion_kyc where usuario_id='{U}' order by iniciada_en desc limit 1")
print("ANTES  usuario:", sql(f"select estado||' nivel='||nivel_kyc from identidad.usuario where id='{U}'"), "· expediente:", sql(f"select estado from identidad.verificacion_kyc where id='{V}'"))
t91 = login("USR91", "000000")
e, r = llamar("POST", f"/identidad/verificaciones/{V}/decision", t91, idem=True, cuerpo={"decision": "APROBAR"})
print("DESPUES usuario:", sql(f"select estado||' nivel='||nivel_kyc from identidad.usuario where id='{U}'"), "· expediente:", sql(f"select estado||' revisor='||(revisada_por is not null)::text from identidad.verificacion_kyc where id='{V}'"))
