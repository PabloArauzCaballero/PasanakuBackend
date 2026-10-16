"""
Piezas comunes de los recorridos de integracion REAL (carril G): lo que varios recorridos hacen igual, por HTTP.

Cada funcion es una accion de negocio con el token REAL de la persona que la hace (nunca un atajo por la base); lo que se
siembra por SQL vive en `mundo.py` y esta declarado alla.
"""
from __future__ import annotations

import traceback
from decimal import Decimal
from typing import Callable

import lib
from lib import Cuenta, Informe, clave, dinero, esperar, psql, uno
from mundo import Elenco, Grupo, armar_elenco, capacidad_de_respaldo, con_billeteras, sembrar_grupo

D = Decimal
INF = Informe("integracion-real")


def obligaciones(g: Grupo, k: int, e: Elenco) -> dict[str, str]:
    """Genera el calendario del periodo k por HTTP (aportes pregunta a grupos) y devuelve {codigo de persona: obligacionId}."""
    r = e.admin.post(f"/aportes/periodos/{g.periodos[k - 1]}/obligaciones")
    assert r.estado in (200, 201), f"generar obligaciones: {r}"
    filas = psql("""SELECT i.codigo_publico, o.id FROM aportes.obligacion_aporte o
                      JOIN grupos.participante p ON p.id = o.participante_id JOIN identidad.usuario i ON i.id = p.usuario_id
                     WHERE o.periodo_id = %s AND o.tipo = 'APORTE_PERIODICO'""", g.periodos[k - 1])
    return {f[0]: f[1] for f in filas}


def pagar(c: Cuenta, obligacion_id: str, monto: str, clave_: str | None = None) -> lib.Respuesta:
    return c.post(f"/aportes/obligaciones/{obligacion_id}/pagos",
                  {"monto": dinero(monto), "canal": "BILLETERA_MOVIL", "referenciaProveedor": "int-" + clave()[:8]}, clave_)


def fondear_pozo(e: Elenco, g: Grupo, k: int, clave_: str | None = None) -> lib.Respuesta:
    return e.tesoreria.post("/entregas/pozo-completo", {
        "grupoId": g.id, "periodoId": g.periodos[k - 1], "turnoId": g.turnos[k - 1], "cupoId": g.cupos[k - 1],
        "beneficiarioId": g.participantes[k - 1], "metodoDesembolso": "BILLETERA_MOVIL", "fechaProgramada": "2026-12-15"}, clave_)


def autorizar_y_ejecutar(e: Elenco, entrega: str, monto: str) -> tuple[lib.Respuesta, lib.Respuesta]:
    """Dos personas distintas (R-SEG-04): RESPONSABLE_RIESGOS autoriza y TESORERIA ejecuta."""
    a = e.riesgos.post(f"/entregas/{entrega}/autorizacion")
    x = e.tesoreria.post(f"/entregas/{entrega}/ejecucion", {"montoEntregado": dinero(monto)})
    return a, x


def evento_publicado(esquema: str, tipo: str, agregado_id: str, tope: float = 45) -> str:
    return esperar(lambda: uno("SELECT estado FROM %s.evento_dominio WHERE tipo = %%s AND agregado_id = %%s AND estado = 'PUBLICADO' LIMIT 1"
                               % esquema, tipo, agregado_id), tope, 1.0, f"evento {tipo} PUBLICADO")


def reserva(e: Elenco, g: Grupo, monto: str, ciclo: int = 1) -> lib.Respuesta:
    return e.admin.post("/garantia/respaldo/reservas", {"grupoId": g.id, "ciclo": ciclo, "monto": dinero(monto), "responsableId": e.admin.id})


def preparar(participantes: int = 3, abiertos: int = 1, periodos: int = 1, saldo: str = "1500.00") -> tuple[Elenco, Grupo]:
    """`saldo`: lo que cada persona tiene en su billetera al empezar. Por omision UN aporte (1500): desde el carril I el aporte cobrado
    DEBITA la billetera (nucleo consume aportes.aporte_cobrado) y un aporte sin fondos se rechaza, como en la realidad."""
    e = armar_elenco(participantes)
    con_billeteras(e, saldo)
    g = sembrar_grupo(e, "1500.00", periodos, abiertos)
    capacidad_de_respaldo(e.admin.id)
    return e, g


def recorrido(id_: str) -> Callable:
    def deco(f: Callable) -> Callable:
        def envuelto() -> None:
            try:
                f()
            except Exception as ex:  # un recorrido que se cae no oculta a los demas
                INF.registrar(id_, f"{f.__name__} se interrumpio", "FAIL", f"{type(ex).__name__}: {str(ex)[:400]}",
                              traceback.format_exc()[-1500:])
        envuelto.__name__ = f.__name__
        return envuelto
    return deco
