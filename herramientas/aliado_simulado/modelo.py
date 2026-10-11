"""Aliado de inversion SIMULADO: DPF y fondo abierto con estado persistente.

Todo lo de aca es ficticio. Las tasas, plazos, minimos, horas de corte, retenciones y
penalizaciones de los productos sembrados son SINTETICOS: no los publico ningun banco ni
ninguna SAFI y no sirven como cotizacion. Existen para que el servicio `inversiones` pueda
ejercitar su logica (devengo, valoracion, rescate demorado) contra un colaborador con estado.

El calculo del DPF esta escrito aca con `Decimal` y por separado del que hay en Java: el
servicio compara contra este resultado, no contra una copia de su propia formula (H10.S1.M4).
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import re
import sqlite3
from contextlib import closing
from datetime import date, datetime, time, timedelta, timezone
from decimal import ROUND_DOWN, ROUND_HALF_EVEN, Decimal
from pathlib import Path
from typing import Any
from uuid import UUID, uuid4

ESCENARIOS = frozenset({"AUTO", "PENDIENTE", "RECHAZADO", "RESPUESTA_PERDIDA", "INDISPONIBLE"})
LA_PAZ = timezone(timedelta(hours=-4), "America/La_Paz")  # sin horario de verano
CENTAVO = Decimal("0.01")
SEIS = Decimal("0.000001")

# Productos SINTETICOS. Ningun numero de esta tabla tiene fuente: son valores de prueba.
PRODUCTOS_SINTETICOS = (
    # codigo, tipo, nombre, plazo, tasa, base, anticipado, penaliz., dias_rescate, corte, minimo, retencion, comision de exito
    ("DPF-DEMO-180", "DPF", "DPF de demostracion a 180 dias (SINTETICO)", 180, "0.040000", 365, 0, "0.000000", None, None, "500.00", "0.100000", None),
    ("DPF-DEMO-360-ANTICIPABLE", "DPF", "DPF de demostracion a 360 dias, cancelable (SINTETICO)", 360, "0.045000", 360, 1, "0.500000", None, None, "500.00", "0.100000", None),
    ("FONDO-DEMO-ABIERTO", "FONDO", "Fondo abierto de demostracion (SINTETICO)", None, None, None, 0, "0.000000", 1, "15:00", "100.00", "0.000000", "0.100000"),
)
FUENTE_SINTETICA = "aliado_simulado/SINTETICO: valor de prueba, no es una cotizacion"


class ErrorAliado(ValueError):
    """Error seguro: un codigo estable, sin reproducir contenido financiero ni credenciales."""

    def __init__(self, codigo: str, status: int = 400) -> None:
        super().__init__(codigo)
        self.codigo = codigo
        self.status = status


def serializar(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False, allow_nan=False).encode("utf-8")


def uuid_canonico(value: Any, codigo: str = "REFERENCIA_INVALIDA") -> str:
    try:
        if not isinstance(value, str) or str(UUID(value)) != value:
            raise ValueError
        return value
    except (ValueError, TypeError, AttributeError) as exc:
        raise ErrorAliado(codigo) from exc


def decimal_texto(value: Any, escala: int, codigo: str, positivo: bool = True) -> Decimal:
    """Un decimal escrito como texto con EXACTAMENTE `escala` decimales; nunca un numero JSON."""
    patron = r"(?:0|[1-9][0-9]{0,11})\.[0-9]{%d}" % escala
    if not isinstance(value, str) or not re.fullmatch(patron, value):
        raise ErrorAliado(codigo)
    d = Decimal(value)
    if positivo and d <= 0:
        raise ErrorAliado(codigo)
    return d


def cent(x: Decimal) -> Decimal:
    return x.quantize(CENTAVO, rounding=ROUND_HALF_EVEN)


def interes_simple(principal: Decimal, tasa: Decimal, dias: int, base: int) -> Decimal:
    """principal x tasa nominal x dias reales / base, a centavos (half-even)."""
    return cent(principal * tasa * Decimal(dias) / Decimal(base))


def instante(value: str) -> datetime:
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if parsed.tzinfo is None:
            raise ValueError
        return parsed.astimezone(timezone.utc)
    except (ValueError, TypeError, AttributeError) as exc:
        raise ErrorAliado("INSTANTE_INVALIDO") from exc


class Aliado:
    """Su SQLite es del aliado ficticio: no es una copia del mayor de Pasanaku."""

    def __init__(self, archivo: Path, secreto: bytes, inicio: datetime | None = None) -> None:
        if len(secreto) < 32:
            raise ErrorAliado("SECRETO_DE_PRUEBA_DEMASIADO_CORTO")
        self.archivo = archivo.resolve()
        self.secreto = secreto
        self.archivo.parent.mkdir(parents=True, exist_ok=True)
        with closing(self.conectar()) as db, db:
            db.executescript("""
                PRAGMA journal_mode=WAL;
                CREATE TABLE IF NOT EXISTS reloj (id INTEGER PRIMARY KEY CHECK(id=1), instante TEXT NOT NULL);
                CREATE TABLE IF NOT EXISTS no_habiles (fecha TEXT PRIMARY KEY);
                CREATE TABLE IF NOT EXISTS productos (
                    codigo TEXT PRIMARY KEY, tipo TEXT NOT NULL CHECK(tipo IN ('DPF','FONDO')), nombre TEXT NOT NULL,
                    plazo_dias INTEGER, tasa_nominal TEXT, base_dias INTEGER, permite_anticipado INTEGER NOT NULL,
                    penalizacion TEXT NOT NULL, dias_rescate INTEGER, hora_corte TEXT, minimo TEXT NOT NULL,
                    tasa_retencion TEXT NOT NULL, tasa_comision_exito TEXT, fuente TEXT NOT NULL, fecha_cotizacion TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS valores_cuota (
                    producto TEXT NOT NULL, fecha TEXT NOT NULL, valor TEXT NOT NULL, publicado TEXT NOT NULL,
                    PRIMARY KEY (producto, fecha)
                );
                CREATE TABLE IF NOT EXISTS operaciones (
                    referencia TEXT PRIMARY KEY, clave TEXT NOT NULL UNIQUE, huella TEXT NOT NULL,
                    tipo TEXT NOT NULL CHECK(tipo IN ('SUSCRIPCION','RESCATE')),
                    producto TEXT NOT NULL, titular_ref TEXT NOT NULL, monto TEXT, cuotas TEXT, posicion TEXT,
                    escenario TEXT NOT NULL,
                    estado TEXT NOT NULL CHECK(estado IN ('PENDIENTE','CONFIRMADO','RECHAZADO')),
                    transaccion TEXT NOT NULL UNIQUE, creada TEXT NOT NULL, liquidada TEXT, fecha_valor TEXT,
                    liquida_en TEXT, version INTEGER NOT NULL DEFAULT 1, detalle TEXT NOT NULL DEFAULT '{}'
                );
                CREATE TABLE IF NOT EXISTS posiciones (
                    id TEXT PRIMARY KEY, producto TEXT NOT NULL, titular_ref TEXT NOT NULL, principal TEXT NOT NULL,
                    cuotas TEXT, valor_entrada TEXT, fecha_constitucion TEXT NOT NULL, fecha_vencimiento TEXT,
                    estado TEXT NOT NULL CHECK(estado IN ('VIGENTE','CERRADA')), orden_ref TEXT NOT NULL UNIQUE
                );
                CREATE TABLE IF NOT EXISTS cambios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, referencia TEXT NOT NULL, version INTEGER NOT NULL,
                    estado TEXT NOT NULL, instante TEXT NOT NULL, UNIQUE(referencia,version)
                );
                CREATE TRIGGER IF NOT EXISTS cambios_no_update BEFORE UPDATE ON cambios BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
                CREATE TRIGGER IF NOT EXISTS cambios_no_delete BEFORE DELETE ON cambios BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
            """)
            db.execute("INSERT OR IGNORE INTO reloj VALUES(1,?)", ((inicio or datetime.now(timezone.utc)).isoformat(),))
            ahora = db.execute("SELECT instante FROM reloj WHERE id=1").fetchone()[0]
            for p in PRODUCTOS_SINTETICOS:
                db.execute("INSERT OR IGNORE INTO productos VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", (*p, FUENTE_SINTETICA, ahora))

    def conectar(self) -> sqlite3.Connection:
        db = sqlite3.connect(self.archivo, timeout=5)
        db.row_factory = sqlite3.Row
        return db

    # ------------------------------------------------------------------ reloj y calendario
    @staticmethod
    def _ahora(db: sqlite3.Connection) -> datetime:
        return instante(db.execute("SELECT instante FROM reloj WHERE id=1").fetchone()[0])

    @staticmethod
    def _habil(db: sqlite3.Connection, dia: date) -> bool:
        return dia.weekday() < 5 and db.execute("SELECT 1 FROM no_habiles WHERE fecha=?", (dia.isoformat(),)).fetchone() is None

    def _habil_n(self, db: sqlite3.Connection, desde: date, n: int) -> date:
        """El n-esimo dia habil posterior a `desde`."""
        dia = desde
        while n > 0:
            dia += timedelta(days=1)
            if self._habil(db, dia):
                n -= 1
        return dia

    def _fecha_valor(self, db: sqlite3.Connection, producto: sqlite3.Row, ahora: datetime) -> date:
        """Una orden que llega despues del corte (o un dia no habil) toma el valor del proximo dia habil."""
        local = ahora.astimezone(LA_PAZ)
        corte = time.fromisoformat(producto["hora_corte"])
        if self._habil(db, local.date()) and local.time() < corte:
            return local.date()
        return self._habil_n(db, local.date(), 1)

    def avanzar(self, segundos: int) -> str:
        if type(segundos) is not int or not 0 <= segundos <= 366 * 86400:
            raise ErrorAliado("AVANCE_INVALIDO")
        with closing(self.conectar()) as db, db:
            db.execute("BEGIN IMMEDIATE")
            nuevo = (self._ahora(db) + timedelta(seconds=segundos)).isoformat()
            db.execute("UPDATE reloj SET instante=? WHERE id=1", (nuevo,))
        return nuevo

    def marcar_no_habil(self, fecha: Any) -> str:
        try:
            dia = date.fromisoformat(fecha)
        except (TypeError, ValueError) as exc:
            raise ErrorAliado("FECHA_INVALIDA") from exc
        with closing(self.conectar()) as db, db:
            db.execute("INSERT OR IGNORE INTO no_habiles VALUES(?)", (dia.isoformat(),))
        return dia.isoformat()

    def publicar_valor_cuota(self, producto: Any, fecha: Any, valor: Any) -> dict[str, str]:
        v = decimal_texto(valor, 6, "VALOR_CUOTA_INVALIDO")
        try:
            dia = date.fromisoformat(fecha)
        except (TypeError, ValueError) as exc:
            raise ErrorAliado("FECHA_INVALIDA") from exc
        with closing(self.conectar()) as db, db:
            db.execute("BEGIN IMMEDIATE")
            p = db.execute("SELECT tipo FROM productos WHERE codigo=?", (producto,)).fetchone()
            if p is None or p["tipo"] != "FONDO":
                raise ErrorAliado("PRODUCTO_NO_ES_FONDO", 404)
            previo = db.execute("SELECT valor FROM valores_cuota WHERE producto=? AND fecha=?", (producto, dia.isoformat())).fetchone()
            if previo is not None and previo["valor"] != valor:
                raise ErrorAliado("VALOR_YA_PUBLICADO", 409)  # un valor publicado no se reescribe
            db.execute("INSERT OR IGNORE INTO valores_cuota VALUES(?,?,?,?)", (producto, dia.isoformat(), valor, self._ahora(db).isoformat()))
            self._asentar_todo(db)
        return {"producto": producto, "fecha": dia.isoformat(), "valor": str(v)}

    # ------------------------------------------------------------------ catalogo y valores
    def productos(self) -> dict[str, Any]:
        with closing(self.conectar()) as db:
            filas = db.execute("SELECT * FROM productos ORDER BY codigo").fetchall()
            ahora = self._ahora(db).isoformat()
        salida = [{
            "codigo": f["codigo"], "tipo": f["tipo"], "nombre": f["nombre"], "plazoDias": f["plazo_dias"],
            "tasaNominalAnual": f["tasa_nominal"], "baseDias": f["base_dias"], "permiteRescateAnticipado": bool(f["permite_anticipado"]),
            "penalizacionAnticipo": f["penalizacion"], "diasRescate": f["dias_rescate"], "horaCorte": f["hora_corte"],
            "montoMinimo": f["minimo"], "tasaRetencion": f["tasa_retencion"], "tasaComisionExito": f["tasa_comision_exito"], "moneda": "BOB",
            "fuente": f["fuente"], "fechaCotizacion": f["fecha_cotizacion"], "origenDatos": "SINTETICO",
        } for f in filas]
        return {"productos": salida, "emitidoEn": ahora, "simulado": True}

    def valor_cuota(self, codigo: str, fecha: str | None = None) -> dict[str, Any]:
        with closing(self.conectar()) as db:
            if fecha is None:
                f = db.execute("SELECT * FROM valores_cuota WHERE producto=? ORDER BY fecha DESC LIMIT 1", (codigo,)).fetchone()
            else:
                f = db.execute("SELECT * FROM valores_cuota WHERE producto=? AND fecha=?", (codigo, fecha)).fetchone()
        if f is None:
            raise ErrorAliado("VALOR_NO_PUBLICADO", 404)
        return {"producto": codigo, "fecha": f["fecha"], "valor": f["valor"], "publicadoEn": f["publicado"], "moneda": "BOB", "simulado": True}

    # ------------------------------------------------------------------ operaciones
    def suscribir(self, entrada: dict[str, Any], clave: str) -> tuple[dict[str, Any], bool]:
        claves = {"referencia", "producto", "monto", "moneda", "titularRef", "escenario"}
        if not isinstance(entrada, dict) or set(entrada) - claves:
            raise ErrorAliado("CONTRATO_INVALIDO")
        ref = uuid_canonico(entrada.get("referencia"))
        uuid_canonico(clave)
        titular = uuid_canonico(entrada.get("titularRef"), "TITULAR_INVALIDO")
        importe = decimal_texto(entrada.get("monto"), 2, "MONTO_DECIMAL_INVALIDO")
        escenario = entrada.get("escenario", "AUTO")
        if entrada.get("moneda") != "BOB" or not isinstance(escenario, str) or escenario not in ESCENARIOS:
            raise ErrorAliado("MONEDA_O_ESCENARIO_INVALIDO")
        codigo = entrada.get("producto")
        if escenario == "INDISPONIBLE":
            raise ErrorAliado("ALIADO_NO_DISPONIBLE", 503)  # no persiste nada: el llamador reintenta
        normal = {"referencia": ref, "producto": codigo, "monto": entrada["monto"], "moneda": "BOB", "titularRef": titular, "escenario": escenario}
        return self._crear(normal, "SUSCRIPCION", clave, importe, None)

    def rescatar(self, entrada: dict[str, Any], clave: str) -> tuple[dict[str, Any], bool]:
        claves = {"referencia", "posicion", "cuotas", "escenario"}
        if not isinstance(entrada, dict) or set(entrada) - claves:
            raise ErrorAliado("CONTRATO_INVALIDO")
        ref = uuid_canonico(entrada.get("referencia"))
        uuid_canonico(clave)
        escenario = entrada.get("escenario", "AUTO")
        if not isinstance(escenario, str) or escenario not in ESCENARIOS:
            raise ErrorAliado("MONEDA_O_ESCENARIO_INVALIDO")
        cuotas = entrada.get("cuotas")
        if cuotas is not None:
            decimal_texto(cuotas, 6, "CUOTAS_INVALIDAS")
        if escenario == "INDISPONIBLE":
            raise ErrorAliado("ALIADO_NO_DISPONIBLE", 503)
        normal = {"referencia": ref, "posicion": entrada.get("posicion"), "cuotas": cuotas, "escenario": escenario}
        return self._crear(normal, "RESCATE", clave, None, cuotas)

    def _crear(self, normal: dict[str, Any], tipo: str, clave: str, importe: Decimal | None, cuotas: str | None) -> tuple[dict[str, Any], bool]:
        ref = normal["referencia"]
        huella = hashlib.sha256(serializar(normal)).hexdigest()
        with closing(self.conectar()) as db, db:
            db.execute("BEGIN IMMEDIATE")
            anterior = db.execute("SELECT * FROM operaciones WHERE referencia=? OR clave=?", (ref, clave)).fetchone()
            if anterior:
                if anterior["huella"] != huella or anterior["clave"] != clave:
                    raise ErrorAliado("IDEMPOTENCIA_CON_CONTENIDO_DISTINTO", 409)
                self._asentar_todo(db)
                return self._salida(db, ref), False
            ahora = self._ahora(db)
            if tipo == "SUSCRIPCION":
                producto = db.execute("SELECT * FROM productos WHERE codigo=?", (normal["producto"],)).fetchone()
                if producto is None:
                    raise ErrorAliado("PRODUCTO_INEXISTENTE", 404)
                if importe < Decimal(producto["minimo"]):
                    raise ErrorAliado("MONTO_BAJO_EL_MINIMO", 422)
                titular, posicion = normal["titularRef"], None
                fecha_valor = self._fecha_valor(db, producto, ahora).isoformat() if producto["tipo"] == "FONDO" else None
                liquida, monto_op, cuotas_op, detalle = None, str(importe), None, {}
            else:
                pos = db.execute("SELECT * FROM posiciones WHERE id=?", (normal["posicion"],)).fetchone()
                if pos is None:
                    raise ErrorAliado("POSICION_INEXISTENTE", 404)
                if pos["estado"] != "VIGENTE":
                    raise ErrorAliado("POSICION_NO_VIGENTE", 409)
                producto = db.execute("SELECT * FROM productos WHERE codigo=?", (pos["producto"],)).fetchone()
                titular, posicion = pos["titular_ref"], pos["id"]
                monto_op, fecha_valor, liquida, cuotas_op, detalle = self._preparar_rescate(db, producto, pos, ahora, cuotas)
            estado = {"RECHAZADO": "RECHAZADO", "PENDIENTE": "PENDIENTE"}.get(normal["escenario"], "PENDIENTE")
            db.execute(
                "INSERT INTO operaciones(referencia,clave,huella,tipo,producto,titular_ref,monto,cuotas,posicion,escenario,estado,transaccion,creada,fecha_valor,liquida_en,detalle) "
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                (ref, clave, huella, tipo, producto["codigo"], titular, monto_op, cuotas_op, posicion, normal["escenario"], estado, str(uuid4()),
                 ahora.isoformat(), fecha_valor, liquida, serializar(detalle).decode("utf-8")))
            db.execute("INSERT INTO cambios(referencia,version,estado,instante) VALUES(?,1,?,?)", (ref, estado, ahora.isoformat()))
            self._asentar_todo(db)
            salida = self._salida(db, ref)
        return salida, normal["escenario"] == "RESPUESTA_PERDIDA"

    def _preparar_rescate(self, db: sqlite3.Connection, producto: sqlite3.Row, pos: sqlite3.Row, ahora: datetime, cuotas: str | None):
        if producto["tipo"] == "DPF":
            hoy = ahora.astimezone(LA_PAZ).date()
            venc = date.fromisoformat(pos["fecha_vencimiento"])
            constitucion = date.fromisoformat(pos["fecha_constitucion"])
            principal, tasa, base = Decimal(pos["principal"]), Decimal(producto["tasa_nominal"]), producto["base_dias"]
            if hoy >= venc:
                dias, modo = producto["plazo_dias"], "VENCIMIENTO"
                bruto = interes_simple(principal, tasa, dias, base)
            elif producto["permite_anticipado"]:
                dias, modo = (hoy - constitucion).days, "ANTICIPADO"
                bruto = cent(interes_simple(principal, tasa, dias, base) * (Decimal(1) - Decimal(producto["penalizacion"])))
            else:
                raise ErrorAliado("RESCATE_ANTICIPADO_NO_PERMITIDO", 409)
            retencion = cent(bruto * Decimal(producto["tasa_retencion"]))
            neto = principal + bruto - retencion
            detalle = {"modo": modo, "diasDevengados": dias, "principal": str(principal), "interesBruto": str(bruto),
                       "retencion": str(retencion), "netoAcreditar": str(neto), "baseDias": base, "tasaNominalAnual": producto["tasa_nominal"]}
            return str(neto), None, None, None, detalle
        # FONDO
        libres = Decimal(pos["cuotas"]) - self._cuotas_en_rescate(db, pos["id"])
        pedido = Decimal(cuotas) if cuotas is not None else libres
        if pedido <= 0 or pedido > libres:
            raise ErrorAliado("DOBLE_DISPONIBILIDAD", 409)  # esas cuotas ya estan comprometidas en otro rescate
        fecha_valor = self._fecha_valor(db, producto, ahora)
        liquida = datetime.combine(self._habil_n(db, fecha_valor, producto["dias_rescate"]), time(9, 0), tzinfo=LA_PAZ).astimezone(timezone.utc)
        return None, fecha_valor.isoformat(), liquida.isoformat(), str(pedido.quantize(SEIS)), {"modo": "RESCATE_FONDO"}

    @staticmethod
    def _cuotas_en_rescate(db: sqlite3.Connection, posicion: str) -> Decimal:
        filas = db.execute("SELECT cuotas FROM operaciones WHERE tipo='RESCATE' AND posicion=? AND estado='PENDIENTE'", (posicion,)).fetchall()
        return sum((Decimal(f["cuotas"]) for f in filas), Decimal(0))

    # ------------------------------------------------------------------ asentamiento (el tiempo y los valores resuelven pendientes)
    def _asentar_todo(self, db: sqlite3.Connection) -> None:
        for f in db.execute("SELECT referencia FROM operaciones WHERE estado='PENDIENTE' AND escenario IN ('AUTO','RESPUESTA_PERDIDA') ORDER BY creada, referencia").fetchall():
            self._asentar(db, f["referencia"])

    def _asentar(self, db: sqlite3.Connection, ref: str) -> None:
        op = db.execute("SELECT * FROM operaciones WHERE referencia=?", (ref,)).fetchone()
        if op["estado"] != "PENDIENTE" or op["escenario"] not in ("AUTO", "RESPUESTA_PERDIDA"):
            return
        ahora = self._ahora(db)
        producto = db.execute("SELECT * FROM productos WHERE codigo=?", (op["producto"],)).fetchone()
        if op["tipo"] == "SUSCRIPCION":
            self._confirmar_suscripcion(db, op, producto, ahora)
        elif producto["tipo"] == "DPF":
            self._confirmar_rescate(db, op, producto, ahora, None)
        elif op["liquida_en"] <= ahora.isoformat():
            self._confirmar_rescate(db, op, producto, ahora, db.execute("SELECT valor FROM valores_cuota WHERE producto=? AND fecha=?", (producto["codigo"], op["fecha_valor"])).fetchone())

    def _confirmar_suscripcion(self, db, op, producto, ahora) -> None:
        importe = Decimal(op["monto"])
        pos_id = "POS-" + op["referencia"]
        local = ahora.astimezone(LA_PAZ).date()
        if producto["tipo"] == "DPF":
            venc = (local + timedelta(days=producto["plazo_dias"])).isoformat()
            db.execute("INSERT INTO posiciones VALUES(?,?,?,?,NULL,NULL,?,?,'VIGENTE',?)", (pos_id, producto["codigo"], op["titular_ref"], str(importe), local.isoformat(), venc, op["referencia"]))
            detalle = {"tipo": "DPF", "vencimiento": venc, "constitucion": local.isoformat()}
            cuotas = None
        else:
            v = db.execute("SELECT valor FROM valores_cuota WHERE producto=? AND fecha=?", (producto["codigo"], op["fecha_valor"])).fetchone()
            if v is None:
                return  # el valor del dia de la orden todavia no se publico: sigue PENDIENTE
            cuotas = (importe / Decimal(v["valor"])).quantize(SEIS, rounding=ROUND_DOWN)  # el resto de cuota se queda en el fondo
            db.execute("INSERT INTO posiciones VALUES(?,?,?,?,?,?,?,NULL,'VIGENTE',?)", (pos_id, producto["codigo"], op["titular_ref"], str(importe), str(cuotas), v["valor"], local.isoformat(), op["referencia"]))
            detalle = {"tipo": "FONDO", "valorCuota": v["valor"], "cuotas": str(cuotas), "constitucion": local.isoformat()}
        self._cerrar(db, op["referencia"], "CONFIRMADO", ahora, op["version"], {"posicion": pos_id, "cuotas": None if cuotas is None else str(cuotas), "detalle": detalle})

    def _confirmar_rescate(self, db, op, producto, ahora, valor) -> None:
        pos = db.execute("SELECT * FROM posiciones WHERE id=?", (op["posicion"],)).fetchone()
        detalle = json.loads(op["detalle"])
        if producto["tipo"] == "DPF":
            db.execute("UPDATE posiciones SET estado='CERRADA' WHERE id=?", (pos["id"],))
        else:
            if valor is None:
                return  # el valor aplicable aun no se publico
            cuotas = Decimal(op["cuotas"])
            bruto = cent(cuotas * Decimal(valor["valor"]))
            restantes = Decimal(pos["cuotas"]) - cuotas
            db.execute("UPDATE posiciones SET cuotas=?, estado=? WHERE id=?", (str(restantes), "CERRADA" if restantes == 0 else "VIGENTE", pos["id"]))
            detalle.update({"valorCuota": valor["valor"], "cuotas": str(cuotas), "netoAcreditar": str(bruto)})
            db.execute("UPDATE operaciones SET monto=? WHERE referencia=?", (str(bruto), op["referencia"]))
        self._cerrar(db, op["referencia"], "CONFIRMADO", ahora, op["version"], {"detalle": detalle})

    def _cerrar(self, db, ref: str, estado: str, ahora: datetime, version: int, extra: dict[str, Any]) -> None:
        sets = {"posicion": extra.get("posicion"), "cuotas": extra.get("cuotas")}
        actual = db.execute("SELECT detalle, posicion, cuotas FROM operaciones WHERE referencia=?", (ref,)).fetchone()
        detalle = json.loads(actual["detalle"])
        detalle.update(extra.get("detalle", {}))
        db.execute(
            "UPDATE operaciones SET estado=?, liquidada=?, version=version+1, posicion=COALESCE(?,posicion), cuotas=COALESCE(?,cuotas), detalle=? WHERE referencia=? AND estado='PENDIENTE'",
            (estado, ahora.isoformat() if estado == "CONFIRMADO" else None, sets["posicion"], sets["cuotas"], serializar(detalle).decode("utf-8"), ref))
        db.execute("INSERT INTO cambios(referencia,version,estado,instante) VALUES(?,?,?,?)", (ref, version + 1, estado, ahora.isoformat()))

    def resolver(self, ref: str, estado: str) -> dict[str, Any]:
        """Control del escenario PENDIENTE: el aliado decide a mano."""
        uuid_canonico(ref)
        if not isinstance(estado, str) or estado not in {"CONFIRMADO", "RECHAZADO"}:
            raise ErrorAliado("TRANSICION_INVALIDA", 409)
        with closing(self.conectar()) as db, db:
            db.execute("BEGIN IMMEDIATE")
            op = db.execute("SELECT * FROM operaciones WHERE referencia=?", (ref,)).fetchone()
            if op is None:
                raise ErrorAliado("OPERACION_NO_ENCONTRADA", 404)
            if op["estado"] == estado:
                return self._salida(db, ref)
            if op["estado"] != "PENDIENTE":
                raise ErrorAliado("ESTADO_TERMINAL_NO_REVERSIBLE", 409)
            ahora = self._ahora(db)
            producto = db.execute("SELECT * FROM productos WHERE codigo=?", (op["producto"],)).fetchone()
            if estado == "RECHAZADO":
                self._cerrar(db, ref, "RECHAZADO", ahora, op["version"], {})
            else:
                db.execute("UPDATE operaciones SET escenario='AUTO' WHERE referencia=?", (ref,))  # a partir de ahora lo asienta el tiempo
                self._asentar(db, ref)
            return self._salida(db, ref)

    def consultar(self, ref: str) -> dict[str, Any]:
        uuid_canonico(ref)
        with closing(self.conectar()) as db, db:
            db.execute("BEGIN IMMEDIATE")
            self._asentar_todo(db)
            return self._salida(db, ref)

    def posicion(self, posicion_id: str) -> dict[str, Any]:
        with closing(self.conectar()) as db, db:
            db.execute("BEGIN IMMEDIATE")
            self._asentar_todo(db)
            p = db.execute("SELECT * FROM posiciones WHERE id=?", (posicion_id,)).fetchone()
            if p is None:
                raise ErrorAliado("POSICION_INEXISTENTE", 404)
            hoy = self._ahora(db).astimezone(LA_PAZ).date()
            en_rescate = self._cuotas_en_rescate(db, p["id"]) if p["cuotas"] is not None else Decimal(0)
        return {"posicion": p["id"], "producto": p["producto"], "titularRef": p["titular_ref"], "principal": p["principal"],
                "cuotas": p["cuotas"], "cuotasEnRescate": str(en_rescate), "valorEntrada": p["valor_entrada"], "fechaConstitucion": p["fecha_constitucion"],
                "fechaVencimiento": p["fecha_vencimiento"], "estado": p["estado"], "hoy": hoy.isoformat(), "moneda": "BOB", "simulado": True}

    def _salida(self, db: sqlite3.Connection, ref: str) -> dict[str, Any]:
        f = db.execute("SELECT * FROM operaciones WHERE referencia=?", (ref,)).fetchone()
        if f is None:
            raise ErrorAliado("OPERACION_NO_ENCONTRADA", 404)
        return {"referencia": f["referencia"], "tipo": f["tipo"], "producto": f["producto"], "estado": f["estado"], "monto": f["monto"],
                "moneda": "BOB", "cuotas": f["cuotas"], "posicionExterna": f["posicion"], "transaccionProveedor": f["transaccion"],
                "creadaEn": f["creada"], "liquidadaEn": f["liquidada"], "fechaValor": f["fecha_valor"], "liquidaEn": f["liquida_en"],
                "version": f["version"], "detalle": json.loads(f["detalle"]), "simulado": True}

    # ------------------------------------------------------------------ firma
    def firmar(self, payload: dict[str, Any]) -> dict[str, str]:
        payload = dict(payload, emitidoEn=datetime.now(timezone.utc).isoformat())
        raw = serializar(payload)
        return {"payload": base64.b64encode(raw).decode("ascii"), "firma": hmac.new(self.secreto, raw, hashlib.sha256).hexdigest()}

    def recibo(self, ref: str) -> dict[str, str]:
        return self.firmar(self.consultar(ref))
