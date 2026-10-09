"""Operaciones externas persistentes y recibos autenticados para desarrollo local."""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import re
import sqlite3
from contextlib import closing
from datetime import datetime, timedelta, timezone
from decimal import Decimal
from pathlib import Path
from typing import Any
from uuid import UUID, uuid4

TIPOS = frozenset({"RECARGA", "RETIRO", "SUSCRIPCION_DPF", "SUSCRIPCION_FONDO", "RESCATE"})
ESCENARIOS = frozenset({"PENDIENTE", "CONFIRMADO", "RECHAZADO", "RESPUESTA_PERDIDA"})
# Fallos de transporte que el control puede armar para las proximas creaciones:
# PERDER_ANTES corta la conexion sin registrar nada; PERDER_DESPUES registra la
# operacion (y la liquida si su escenario lo hace) y corta sin responder.
# LIQUIDAR_Y_PERDER es el caso que mas duele: la operacion se liquida (aunque el pedido
# no lo haya pedido) y la respuesta se pierde. Desde afuera es indistinguible de PERDER_ANTES.
FALLOS = frozenset({"PERDER_ANTES", "PERDER_DESPUES", "LIQUIDAR_Y_PERDER"})


class ErrorProveedor(ValueError):
    """Error seguro, sin reproducir contenido financiero o credenciales."""

    def __init__(self, codigo: str, status: int = 400) -> None:
        super().__init__(codigo)
        self.codigo = codigo
        self.status = status


def serializar(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False, allow_nan=False).encode("utf-8")


def referencia(value: Any) -> str:
    try:
        if not isinstance(value, str) or str(UUID(value)) != value:
            raise ValueError
        return value
    except (ValueError, TypeError, AttributeError) as exc:
        raise ErrorProveedor("REFERENCIA_INVALIDA") from exc


def monto(value: Any) -> str:
    if not isinstance(value, str) or not re.fullmatch(r"(?:0|[1-9][0-9]{0,11})\.[0-9]{2}", value):
        raise ErrorProveedor("MONTO_DECIMAL_INVALIDO")
    if Decimal(value) <= 0:
        raise ErrorProveedor("MONTO_DEBE_SER_POSITIVO")
    return value


def instante(value: str) -> datetime:
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if parsed.tzinfo is None:
            raise ValueError
        return parsed.astimezone(timezone.utc)
    except (ValueError, TypeError, AttributeError) as exc:
        raise ErrorProveedor("INSTANTE_INVALIDO") from exc


class Proveedor:
    """Su SQLite es del proveedor ficticio, no una copia del mayor de la app."""

    def __init__(self, archivo: Path, secreto: bytes) -> None:
        if len(secreto) < 32:
            raise ErrorProveedor("SECRETO_DE_PRUEBA_DEMASIADO_CORTO")
        self.archivo = archivo.resolve()
        self.secreto = secreto
        self.archivo.parent.mkdir(parents=True, exist_ok=True)
        with closing(self.conectar()) as db, db:
            db.executescript("""
                PRAGMA journal_mode=WAL;
                CREATE TABLE IF NOT EXISTS reloj (
                    id INTEGER PRIMARY KEY CHECK(id=1), instante TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS fallos (
                    id INTEGER PRIMARY KEY CHECK(id=1),
                    modo TEXT,
                    restantes INTEGER NOT NULL DEFAULT 0 CHECK(restantes BETWEEN 0 AND 10)
                );
                CREATE TABLE IF NOT EXISTS operaciones (
                    referencia TEXT PRIMARY KEY,
                    clave TEXT NOT NULL UNIQUE,
                    huella TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    monto TEXT NOT NULL,
                    moneda TEXT NOT NULL CHECK(moneda='BOB'),
                    estado TEXT NOT NULL CHECK(estado IN ('PENDIENTE','CONFIRMADO','RECHAZADO')),
                    transaccion TEXT NOT NULL UNIQUE,
                    creada TEXT NOT NULL,
                    liquidada TEXT,
                    version INTEGER NOT NULL DEFAULT 1,
                    condiciones TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS cambios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    referencia TEXT NOT NULL,
                    version INTEGER NOT NULL,
                    estado TEXT NOT NULL,
                    instante TEXT NOT NULL,
                    UNIQUE(referencia,version)
                );
                CREATE TRIGGER IF NOT EXISTS cambios_no_update
                BEFORE UPDATE ON cambios BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
                CREATE TRIGGER IF NOT EXISTS cambios_no_delete
                BEFORE DELETE ON cambios BEGIN SELECT RAISE(ABORT,'APPEND_ONLY'); END;
            """)
            db.execute("INSERT OR IGNORE INTO reloj VALUES(1,?)", (datetime.now(timezone.utc).isoformat(),))
            db.execute("INSERT OR IGNORE INTO fallos(id,modo,restantes) VALUES(1,NULL,0)")

    def conectar(self) -> sqlite3.Connection:
        db = sqlite3.connect(self.archivo, timeout=5)
        db.row_factory = sqlite3.Row
        return db

    def crear(self, entrada: dict[str, Any], clave: str, escenario_forzado: str | None = None) -> tuple[dict[str, Any], bool]:
        if not isinstance(entrada, dict) or set(entrada) - {"referencia", "tipo", "monto", "moneda", "escenario", "condiciones"}:
            raise ErrorProveedor("CONTRATO_INVALIDO")
        ref = referencia(entrada.get("referencia"))
        referencia(clave)
        importe = monto(entrada.get("monto"))
        tipo = entrada.get("tipo")
        escenario = entrada.get("escenario", "PENDIENTE")
        condiciones = entrada.get("condiciones", {})
        if not isinstance(tipo, str) or tipo not in TIPOS or entrada.get("moneda") != "BOB" or not isinstance(escenario, str) or escenario not in ESCENARIOS:
            raise ErrorProveedor("TIPO_MONEDA_O_ESCENARIO_INVALIDO")
        if not isinstance(condiciones, dict) or len(serializar(condiciones)) > 4096:
            raise ErrorProveedor("CONDICIONES_INVALIDAS")
        normalizada = {"referencia": ref, "tipo": tipo, "monto": importe, "moneda": "BOB", "escenario": escenario, "condiciones": condiciones}
        huella = hashlib.sha256(serializar(normalizada)).hexdigest()
        perdida = False
        with closing(self.conectar()) as db, db:
            db.execute("BEGIN IMMEDIATE")
            anterior = db.execute("SELECT * FROM operaciones WHERE referencia=? OR clave=?", (ref, clave)).fetchone()
            if anterior:
                if anterior["huella"] != huella or anterior["clave"] != clave:
                    raise ErrorProveedor("IDEMPOTENCIA_CON_CONTENIDO_DISTINTO", 409)
                return self._salida(anterior), False
            ahora = db.execute("SELECT instante FROM reloj WHERE id=1").fetchone()[0]
            # El escenario forzado decide el estado, no la huella: un reintento identico sigue siendo identico.
            efectivo = escenario_forzado or escenario
            estado = "CONFIRMADO" if efectivo == "RESPUESTA_PERDIDA" else efectivo
            liquidada = ahora if estado == "CONFIRMADO" else None
            db.execute("INSERT INTO operaciones VALUES(?,?,?,?,?,?,?,?,?,?,1,?)", (ref, clave, huella, tipo, importe, "BOB", estado, str(uuid4()), ahora, liquidada, serializar(condiciones).decode("utf-8")))
            db.execute("INSERT INTO cambios(referencia,version,estado,instante) VALUES(?,1,?,?)", (ref, estado, ahora))
            fila = db.execute("SELECT * FROM operaciones WHERE referencia=?", (ref,)).fetchone()
            perdida = efectivo == "RESPUESTA_PERDIDA"
        return self._salida(fila), perdida

    def consultar(self, ref: str) -> dict[str, Any]:
        referencia(ref)
        with closing(self.conectar()) as db:
            fila = db.execute("SELECT * FROM operaciones WHERE referencia=?", (ref,)).fetchone()
        if fila is None:
            raise ErrorProveedor("OPERACION_NO_ENCONTRADA", 404)
        return self._salida(fila)

    @staticmethod
    def _salida(fila: sqlite3.Row) -> dict[str, Any]:
        return {"referencia": fila["referencia"], "tipo": fila["tipo"], "monto": fila["monto"], "moneda": fila["moneda"], "estado": fila["estado"], "transaccionProveedor": fila["transaccion"], "creadaEn": fila["creada"], "liquidadaEn": fila["liquidada"], "version": fila["version"], "condiciones": json.loads(fila["condiciones"]), "simulado": True}

    def resolver(self, ref: str, estado: str) -> dict[str, Any]:
        referencia(ref)
        if not isinstance(estado, str) or estado not in {"CONFIRMADO", "RECHAZADO"}:
            raise ErrorProveedor("TRANSICION_INVALIDA", 409)
        with closing(self.conectar()) as db, db:
            db.execute("BEGIN IMMEDIATE")
            fila = db.execute("SELECT * FROM operaciones WHERE referencia=?", (ref,)).fetchone()
            if fila is None:
                raise ErrorProveedor("OPERACION_NO_ENCONTRADA", 404)
            if fila["estado"] == estado:
                return self._salida(fila)
            if fila["estado"] != "PENDIENTE":
                raise ErrorProveedor("ESTADO_TERMINAL_NO_REVERSIBLE", 409)
            ahora = db.execute("SELECT instante FROM reloj WHERE id=1").fetchone()[0]
            db.execute("UPDATE operaciones SET estado=?,liquidada=?,version=version+1 WHERE referencia=? AND estado='PENDIENTE'", (estado, ahora if estado == "CONFIRMADO" else None, ref))
            db.execute("INSERT INTO cambios(referencia,version,estado,instante) VALUES(?,?,?,?)", (ref, fila["version"] + 1, estado, ahora))
            actual = db.execute("SELECT * FROM operaciones WHERE referencia=?", (ref,)).fetchone()
        return self._salida(actual)

    def resumen(self) -> dict[str, Any]:
        """Conteos para que una prueba afirme «una sola liquidacion» sin leer el archivo."""
        with closing(self.conectar()) as db:
            fila = db.execute(
                "SELECT count(*) AS total,"
                " coalesce(sum(estado='CONFIRMADO'),0) AS confirmadas,"
                " coalesce(sum(estado='PENDIENTE'),0) AS pendientes,"
                " coalesce(sum(estado='RECHAZADO'),0) AS rechazadas FROM operaciones"
            ).fetchone()
        return {"operaciones": fila["total"], "confirmadas": fila["confirmadas"], "pendientes": fila["pendientes"], "rechazadas": fila["rechazadas"]}

    def armar_fallo(self, modo: Any, cantidad: Any) -> dict[str, Any]:
        """Programa cuantas creaciones siguientes sufren un corte de transporte; sobrevive al reinicio."""
        if not isinstance(modo, str) or modo not in FALLOS or type(cantidad) is not int or not 1 <= cantidad <= 10:
            raise ErrorProveedor("FALLO_INVALIDO")
        with closing(self.conectar()) as db, db:
            db.execute("BEGIN IMMEDIATE")
            db.execute("UPDATE fallos SET modo=?,restantes=? WHERE id=1", (modo, cantidad))
        return {"modo": modo, "restantes": cantidad}

    def consumir_fallo(self) -> str | None:
        """Descuenta atomicamente un fallo armado; None cuando no hay ninguno."""
        with closing(self.conectar()) as db, db:
            db.execute("BEGIN IMMEDIATE")
            fila = db.execute("SELECT modo,restantes FROM fallos WHERE id=1").fetchone()
            if fila is None or fila["modo"] is None or fila["restantes"] <= 0:
                return None
            db.execute("UPDATE fallos SET restantes=restantes-1 WHERE id=1")
            return fila["modo"]

    def avanzar(self, segundos: int) -> str:
        if type(segundos) is not int or not 0 <= segundos <= 366 * 86400:
            raise ErrorProveedor("AVANCE_INVALIDO")
        with closing(self.conectar()) as db, db:
            db.execute("BEGIN IMMEDIATE")
            actual = instante(db.execute("SELECT instante FROM reloj WHERE id=1").fetchone()[0])
            nuevo = (actual + timedelta(seconds=segundos)).isoformat()
            db.execute("UPDATE reloj SET instante=? WHERE id=1", (nuevo,))
        return nuevo

    def recibo(self, ref: str) -> dict[str, str]:
        payload = self.consultar(ref)
        payload["emitidoEn"] = datetime.now(timezone.utc).isoformat()
        raw = serializar(payload)
        return {"payload": base64.b64encode(raw).decode("ascii"), "firma": hmac.new(self.secreto, raw, hashlib.sha256).hexdigest()}
