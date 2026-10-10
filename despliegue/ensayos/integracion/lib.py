"""
Biblioteca del ensayo de INTEGRACION REAL entre servicios (carril G).

Habla con el stack compose de despliegue/ensayos/arranque_completo.sh por HTTP, a traves del gateway, con tokens
REALES emitidos por identidad. La base se toca para DOS cosas y nada mas, y las dos quedan dichas en el informe:

  * precondiciones que ningun servicio sabe crear todavia (la billetera de una persona, la estructura de un grupo
    sintetico, la capacidad de respaldo empresarial: dato de gobierno), igual que lo hacen las fixturas de las pruebas;
  * LECTURAS para conciliar contra el mayor (movimiento_billetera, movimiento_contable) y para contar filas.

Nunca se escribe en el libro ni en una tabla de un servicio para provocar el resultado de lo que se prueba.

Todo lo que se genera es sintetico: telefonos +5917xxxxxxx, claves aleatorias (en memoria, nunca al repo), correos
@ejemplo.invalid. Sin datos personales reales.
"""
from __future__ import annotations

import json
import os
import re
import secrets
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
from typing import Any, Callable

D3 = os.environ.get("D3_DIR") or os.path.join(os.environ.get("TEMP", "/tmp"), "aportaya-d3")
GATEWAY = os.environ.get("INTEGRACION_GATEWAY", f"http://127.0.0.1:{os.environ.get('D3_PUERTO_GATEWAY', '18080')}/api/v1")
POSTGRES = os.environ.get("INTEGRACION_POSTGRES", "aportaya-d3-infra-postgres-1")
KAFKA = os.environ.get("INTEGRACION_KAFKA", "aportaya-d3-infra-kafka-1")


# --------------------------------------------------------------------------------------------- entorno y base
def entorno() -> dict[str, str]:
    """El entorno sintetico del ensayo (fuera del repo). Nunca se imprime."""
    datos: dict[str, str] = {}
    for archivo in ("entorno.env",):
        ruta = os.path.join(D3, archivo)
        for linea in open(ruta, encoding="utf-8"):
            if "=" in linea and not linea.startswith("#"):
                k, v = linea.rstrip("\n").split("=", 1)
                datos[k] = v
    return datos


E = entorno() if os.path.exists(os.path.join(D3, "entorno.env")) else {}


def lit(valor: Any) -> str:
    """Literal SQL para valores que genera ESTE script (uuid, numeros, textos propios). No es para entrada ajena."""
    if valor is None:
        return "NULL"
    if isinstance(valor, bool):
        return "TRUE" if valor else "FALSE"
    if isinstance(valor, (int, Decimal)):
        return str(valor)
    return "'" + str(valor).replace("'", "''") + "'"


def sql(consulta: str, *params: Any) -> str:
    return consulta % tuple(lit(p) for p in params) if params else consulta


def psql(consulta: str, *params: Any, comprobar: bool = True) -> list[list[str]]:
    """Ejecuta SQL contra la base del ensayo y devuelve las filas (columnas separadas por '|')."""
    p = subprocess.run(
        ["docker", "exec", "-i", "-e", "PGPASSWORD=" + E["BD_CLAVE_ADMIN"], POSTGRES, "psql", "-v", "ON_ERROR_STOP=1",
         "-At", "-F", "\x1f", "-U", "pasanaku", "-d", "pasanaku"],
        input=sql(consulta, *params).encode(), capture_output=True)
    if p.returncode != 0 and comprobar:
        raise RuntimeError("psql: " + p.stderr.decode(errors="replace")[:600])
    salida = p.stdout.decode(errors="replace").strip()
    return [fila.split("\x1f") for fila in salida.splitlines() if fila and not fila.startswith(("INSERT ", "UPDATE ", "DELETE ", "BEGIN", "COMMIT", "SET"))]


def uno(consulta: str, *params: Any) -> str | None:
    filas = psql(consulta, *params)
    return filas[0][0] if filas and filas[0] else None


def numero(consulta: str, *params: Any) -> Decimal:
    v = uno(consulta, *params)
    return Decimal(v) if v not in (None, "") else Decimal("0")


# ------------------------------------------------------------------------------------------------------- HTTP
class Respuesta:
    def __init__(self, estado: int, cuerpo: Any, texto: str, cabeceras: dict[str, str]):
        self.estado, self.cuerpo, self.texto, self.cabeceras = estado, cuerpo, texto, cabeceras

    def __repr__(self) -> str:
        return f"<{self.estado} {self.texto[:160]}>"

    @property
    def codigo(self) -> str | None:
        return self.cuerpo.get("codigo") if isinstance(self.cuerpo, dict) else None


def http(metodo: str, ruta: str, cuerpo: Any = None, token: str | None = None, clave: str | None = None,
         consulta: dict[str, str] | None = None, reintentos_429: int = 20) -> Respuesta:
    url = GATEWAY + ruta
    if consulta:
        url += "?" + "&".join(f"{k}={v}" for k, v in consulta.items())
    cab = {"Accept": "application/json"}
    if cuerpo is not None:
        cab["Content-Type"] = "application/json"
    if token:
        cab["Authorization"] = "Bearer " + token
    if clave:
        cab["Idempotency-Key"] = clave
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    for intento in range(reintentos_429 + 1):
        pedido = urllib.request.Request(url, data=datos, headers=cab, method=metodo)
        try:
            r = urllib.request.urlopen(pedido, timeout=90)
            estado, texto, cabeceras = r.status, r.read().decode(errors="replace"), dict(r.headers)
        except urllib.error.HTTPError as e:
            estado, texto, cabeceras = e.code, e.read().decode(errors="replace"), dict(e.headers)
        except (urllib.error.URLError, TimeoutError) as e:
            return Respuesta(0, {}, f"sin respuesta: {e}", {})
        if estado == 429 and intento < reintentos_429:
            time.sleep(min(float(cabeceras.get("Retry-After", "1")), 5) + 0.2)
            continue
        try:
            js = json.loads(texto) if texto else {}
        except ValueError:
            js = {"_texto": texto}
        return Respuesta(estado, js, texto, cabeceras)
    raise AssertionError("inalcanzable")


def clave() -> str:
    return str(uuid.uuid4())


def dinero(monto: str | Decimal, moneda: str = "BOB") -> dict[str, str]:
    return {"monto": f"{Decimal(str(monto)):.2f}", "moneda": moneda}


# --------------------------------------------------------------------------------------------- cuentas y tokens
class Cuenta:
    """Una persona sintetica con credencial REAL (hash Argon2id con la pimienta del entorno) sembrada en identidad."""

    def __init__(self, codigo: str, rol: str, nombre: str, telefono: str):
        self.codigo, self.rol, self.nombre, self.telefono = codigo, rol, nombre, telefono
        self.clave = secrets.token_urlsafe(18)
        self.extras: list[str] = []  # roles globales ADICIONALES (p. ej. quien cobra y ademas ve reclamos)
        self.id: str | None = None
        self._token: str | None = None
        self._vence = 0.0
        self._cerrojo = threading.Lock()

    def token(self, forzar: bool = False) -> str:
        with self._cerrojo:
            if self._token and not forzar and time.time() < self._vence:
                return self._token
            base = {"telefonoE164": self.telefono, "credencial": self.clave, "huellaDispositivo": "int-" + uuid.uuid4().hex[:8],
                    "plataforma": "WEB"}
            r = http("POST", "/sesiones", base)
            if r.estado == 422 and r.codigo == "AP-CU04-03":
                base["factor"] = {"tipo": "OTP", "valor": "000000"}
                r = http("POST", "/sesiones", base)
            if r.estado != 200 or "tokenAcceso" not in r.cuerpo:
                raise RuntimeError(f"no pude abrir sesion de {self.codigo} ({r.estado}): {r.texto[:200]}")
            self._token = r.cuerpo["tokenAcceso"]
            self._vence = time.time() + 600  # el token vive 15 min: se renueva antes
            return self._token

    # atajos HTTP con el token de esta persona
    def get(self, ruta: str, **kw: Any) -> Respuesta:
        return http("GET", ruta, token=self.token(), **kw)

    def post(self, ruta: str, cuerpo: Any = None, clave_: str | None = None, **kw: Any) -> Respuesta:
        return http("POST", ruta, cuerpo, token=self.token(), clave=clave_ if clave_ is not None else clave(), **kw)


def hash_clave(texto: str) -> str:
    sys.path.insert(0, os.path.join(D3, "pylib"))
    from argon2 import PasswordHasher  # type: ignore

    # Los mismos parametros que identidad, con la pimienta del entorno: identidad hashea `credencial + pimienta`.
    return PasswordHasher(time_cost=3, memory_cost=65536, parallelism=4).hash(texto + E["SEGURIDAD_PIMIENTA"])


def sembrar_usuarios(cuentas: list[Cuenta]) -> None:
    """Crea (idempotente por codigo_publico) usuarios sinteticos con rol global, credencial y factor enrolado."""
    partes = ["""
INSERT INTO identidad.usuario (codigo_publico, nombres, apellidos, telefono_e164, correo, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
VALUES ('USRINT00', 'Otorgante', 'Sintetico', '+59172999000', 'usrint00@ejemplo.invalid', '1990-01-01', 'ACTIVO', 'COMPLETO', 'es-BO', 'America/La_Paz', now())
ON CONFLICT (codigo_publico) DO NOTHING;"""]
    for c in cuentas:
        tipo = "SMS" if all(r in ("PARTICIPANTE", "ORGANIZADOR") for r in [c.rol, *c.extras]) else "TOTP"
        roles_sql = "".join(f"""
INSERT INTO identidad.asignacion_rol (usuario_id, rol_id, ambito, ambito_id, otorgada_por, otorgada_en)
SELECT u.id, r.id, 'GLOBAL', NULL, (SELECT id FROM identidad.usuario WHERE codigo_publico = 'USRINT00'), now()
  FROM identidad.usuario u, identidad.rol r
 WHERE u.codigo_publico = {lit(c.codigo)} AND r.codigo = {lit(rol)}
   AND NOT EXISTS (SELECT 1 FROM identidad.asignacion_rol a WHERE a.usuario_id = u.id AND a.rol_id = r.id);""" for rol in [c.rol, *c.extras])
        partes.append(f"""
INSERT INTO identidad.usuario (codigo_publico, nombres, apellidos, telefono_e164, correo, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
VALUES ({lit(c.codigo)}, {lit(c.nombre)}, 'Sintetico', {lit(c.telefono)}, {lit(c.codigo.lower() + '@ejemplo.invalid')}, '1990-01-01', 'ACTIVO', 'COMPLETO', 'es-BO', 'America/La_Paz', now())
ON CONFLICT (codigo_publico) DO NOTHING;
INSERT INTO identidad.credencial_acceso (usuario_id, hash_contrasena, algoritmo, parametros_kdf, requiere_cambio, cambiada_en)
SELECT id, {lit(hash_clave(c.clave))}, 'argon2id', '{{"m": 65536, "t": 3, "p": 4}}'::jsonb, FALSE, now() FROM identidad.usuario WHERE codigo_publico = {lit(c.codigo)}
ON CONFLICT (usuario_id) DO UPDATE SET hash_contrasena = EXCLUDED.hash_contrasena;
{roles_sql}
INSERT INTO identidad.factor_mfa (usuario_id, tipo, secreto_cifrado, activo, es_principal, confirmado_en, version_llave)
SELECT id, {lit(tipo)}, 'enc:v1:int-sintetico', TRUE, TRUE, now(), 1 FROM identidad.usuario WHERE codigo_publico = {lit(c.codigo)}
   AND NOT EXISTS (SELECT 1 FROM identidad.factor_mfa f WHERE f.usuario_id = identidad.usuario.id);""")
    psql("BEGIN;\n" + "\n".join(partes) + "\nCOMMIT;")
    for c in cuentas:
        c.id = uno("SELECT id FROM identidad.usuario WHERE codigo_publico = %s", c.codigo)


# ------------------------------------------------------------------------------------------ billeteras y libro
ID_PUENTE = "00000000-0000-0000-0000-0000000000c0"
ID_INGRESOS = "00000000-0000-0000-0000-0000000000c1"


def abrir_billetera(usuario_id: str, moneda: str = "BOB") -> str:
    """La billetera de una persona. Ningun servicio la abre hoy (no hay consumidor de identidad.usuario_registrado)."""
    existente = uno("SELECT id FROM nucleo_financiero.cuenta_billetera WHERE usuario_id = %s AND moneda = %s AND tipo = 'USUARIO'",
                    usuario_id, moneda)
    if existente:
        return existente
    cuenta = str(uuid.uuid4())
    psql("""INSERT INTO nucleo_financiero.cuenta_billetera
              (id, numero_cuenta, tipo, usuario_id, politica_billetera_id, moneda, estado, nivel_debida_diligencia,
               saldo_disponible, saldo_retenido, permite_saldo_negativo, fecha_apertura, version)
            SELECT %s, %s, 'USUARIO', %s, (SELECT id FROM nucleo_financiero.politica_billetera WHERE moneda = %s ORDER BY vigente_desde LIMIT 1),
                   %s, 'ACTIVA', 'ESTANDAR', 0, 0, false, now(), 0""",
         cuenta, "INT-" + cuenta[:12], usuario_id, moneda, moneda)
    return cuenta


def saldo_del_libro(cuenta_id: str) -> Decimal:
    """Disponible segun el LIBRO (movimientos), sin descontar retenciones."""
    return numero("""SELECT COALESCE(SUM(CASE WHEN sentido = 'CREDITO' THEN monto ELSE -monto END), 0)
                       FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id = %s""", cuenta_id)


def retenido_vigente(cuenta_id: str) -> Decimal:
    return numero("SELECT COALESCE(SUM(monto), 0) FROM nucleo_financiero.retencion_saldo WHERE cuenta_billetera_id = %s AND estado = 'VIGENTE'",
                  cuenta_id)


def transacciones_descuadradas() -> list[str]:
    """Transacciones cuyo debito != credito (R-BIL: toda transaccion cuadra)."""
    filas = psql("""SELECT transaccion_id FROM nucleo_financiero.movimiento_billetera GROUP BY transaccion_id
                     HAVING SUM(CASE WHEN sentido = 'DEBITO' THEN monto ELSE 0 END)
                         <> SUM(CASE WHEN sentido = 'CREDITO' THEN monto ELSE 0 END)""")
    return [f[0] for f in filas]


def suma_global_del_libro() -> Decimal:
    """Un sistema cerrado: la suma de todos los movimientos (creditos - debitos) de TODAS las cuentas es cero."""
    return numero("""SELECT COALESCE(SUM(CASE WHEN sentido = 'CREDITO' THEN monto ELSE -monto END), 0)
                       FROM nucleo_financiero.movimiento_billetera""")


def asientos_contables(origen_tipo: str, origen_id: str) -> list[dict[str, Any]]:
    filas = psql("""SELECT a.id, a.estado, a.asiento_reversa_id,
                           COALESCE((SELECT SUM(debe) FROM nucleo_financiero.movimiento_contable m WHERE m.asiento_id = a.id), 0),
                           COALESCE((SELECT SUM(haber) FROM nucleo_financiero.movimiento_contable m WHERE m.asiento_id = a.id), 0)
                      FROM nucleo_financiero.asiento_contable a WHERE a.origen_tipo = %s AND a.origen_id = %s ORDER BY a.numero""",
                  origen_tipo, origen_id)
    return [{"id": f[0], "estado": f[1], "reversa": f[2] or None, "debe": Decimal(f[3]), "haber": Decimal(f[4])} for f in filas]


def saldo_contable(codigo_cuenta: str) -> Decimal:
    """debe - haber de una cuenta del plan, desde los movimientos del mayor."""
    return numero("""SELECT COALESCE(SUM(m.debe - m.haber), 0) FROM nucleo_financiero.movimiento_contable m
                       JOIN nucleo_financiero.cuenta_contable c ON c.id = m.cuenta_id WHERE c.codigo = %s""", codigo_cuenta)


# ---------------------------------------------------------------------------------------------- esperas
def esperar(condicion: Callable[[], Any], tope_s: float = 60, cada_s: float = 1.0, que: str = "condicion") -> Any:
    """Espera POR CONDICION (nunca un sleep fijo) y devuelve lo que la condicion devolvio."""
    fin = time.time() + tope_s
    ultimo = None
    while time.time() < fin:
        ultimo = condicion()
        if ultimo:
            return ultimo
        time.sleep(cada_s)
    raise TimeoutError(f"se agoto la espera de: {que} (ultimo valor: {ultimo!r})")


def en_paralelo(tareas: list[Callable[[], Any]], hilos: int | None = None) -> list[Any]:
    """Dispara las tareas casi a la vez (barrera) y devuelve sus resultados en orden."""
    barrera = threading.Barrier(len(tareas))

    def correr(tarea: Callable[[], Any]) -> Any:
        barrera.wait(timeout=30)
        return tarea()

    with ThreadPoolExecutor(max_workers=hilos or len(tareas)) as pool:
        return [f.result() for f in [pool.submit(correr, t) for t in tareas]]


# ------------------------------------------------------------------------------------------------ informe
_SECRETOS = [
    (re.compile(r"[0-9a-f]{64}"), "<hex64>"),  # tokens de invitacion, hashes
    (re.compile(r"eyJ[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+"), "<jwt>"),
    (re.compile(r'("(?:token|tokenAcceso|tokenRefresco|credencial|clave|secreto)"\s*:\s*")[^"]+'), r"\g<1><enmascarado>"),
    (re.compile(r"\+591\d{8}"), "+591<tel>"),
]


def enmascarar(texto: str) -> str:
    """Nada de tokens, claves ni telefonos en la evidencia (regla 90.2): aunque sean sinteticos, el habito es el que importa."""
    for patron, reemplazo in _SECRETOS:
        texto = patron.sub(reemplazo, texto)
    return texto


class Informe:
    """Resultados por comprobacion. Un recorrido que falla NO oculta el siguiente: se registra y se sigue."""

    def __init__(self, nombre: str):
        self.nombre = nombre
        self.filas: list[dict[str, Any]] = []
        self.salida = os.environ.get("INTEGRACION_SALIDA")

    def registrar(self, id_: str, titulo: str, resultado: str, detalle: str = "", evidencia: Any = None) -> None:
        detalle = enmascarar(detalle)
        if isinstance(evidencia, str):
            evidencia = enmascarar(evidencia)
        self.filas.append({"id": id_, "titulo": titulo, "resultado": resultado, "detalle": detalle, "evidencia": evidencia})
        marca = {"PASS": "PASS ", "FAIL": "FAIL ", "GAP": "GAP  ", "INFO": "INFO "}.get(resultado, resultado)
        print(f"{marca} {id_} {titulo}" + (f"  -> {detalle}" if detalle else ""), flush=True)

    def comprobar(self, id_: str, titulo: str, condicion: bool, detalle: str = "", evidencia: Any = None) -> bool:
        self.registrar(id_, titulo, "PASS" if condicion else "FAIL", detalle, evidencia)
        return bool(condicion)

    def brecha(self, id_: str, titulo: str, detalle: str) -> None:
        """Algo que el sistema NO hace todavia (declarado, con quien lo cierra): no es un fallo del ensayo."""
        self.registrar(id_, titulo, "GAP", detalle)

    def resumen(self) -> int:
        cuenta = {k: sum(1 for f in self.filas if f["resultado"] == k) for k in ("PASS", "FAIL", "GAP", "INFO")}
        print(f"\nRESUMEN {self.nombre}: {cuenta['PASS']} PASS, {cuenta['FAIL']} FAIL, {cuenta['GAP']} GAP (brechas declaradas)", flush=True)
        if self.salida:
            with open(self.salida, "w", encoding="utf-8") as f:
                json.dump({"nombre": self.nombre, "resumen": cuenta, "filas": self.filas}, f, indent=2, ensure_ascii=False, default=str)
        return 1 if cuenta["FAIL"] else 0
