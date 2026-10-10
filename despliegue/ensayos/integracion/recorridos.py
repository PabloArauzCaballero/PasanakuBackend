#!/usr/bin/env python3
"""
Recorridos de integracion REAL entre servicios (carril G): H8.S1.M3, H8.S1.M4, H9.S1.M5, H14.S1.M1, H14.S1.M2, H14.S1.M3.

    python despliegue/ensayos/integracion/recorridos.py [h8m3 h8m4 h9m5 h14m1 h14m2 h14m3 reclamos]   (sin argumentos: todos)

Requiere el stack de `bash despliegue/ensayos/integracion/correr.sh` (identidad, grupos, aportes, entregas,
nucleo-financiero, garantia, cumplimiento + gateway, Postgres y Kafka). Sale con 1 si ALGUNA comprobacion falla; una
brecha declarada (lo que el sistema no hace todavia, con quien lo cierra) no es un fallo del ensayo.

Cada recorrido arma SU elenco y SU grupo: no comparten nada y corren en cualquier orden.
"""
from __future__ import annotations

import sys
from decimal import Decimal

import lib
from acciones import (D, INF, autorizar_y_ejecutar, evento_publicado, fondear_pozo, obligaciones, pagar, preparar,
                      recorrido, reserva)
from lib import (asientos_contables, clave, dinero, esperar, numero, psql, saldo_contable, saldo_del_libro,
                 suma_global_del_libro, transacciones_descuadradas, uno)
from h14 import evento_id, logs_de, reentregar
from mundo import cuenta_de_servicio_de_entregas, fondear_por_libro, oferta_publicada


# ---------------------------------------------------------------------------------- 0 · la infraestructura de eventos
@recorrido("INFRA")
def infraestructura() -> None:
    p = lib.subprocess.run(["docker", "exec", lib.KAFKA, "kafka-topics", "--bootstrap-server", "kafka:9092", "--list"],
                           capture_output=True, text=True, env={**lib.os.environ, "KAFKA_HEAP_OPTS": "-Xmx64m"})
    topicos = [t for t in p.stdout.split() if t.startswith("aportaya.")]
    INF.comprobar("INFRA-1", "los topicos aportaya.* existen (el broker no autocrea)", len(topicos) >= 200, f"{len(topicos)} topicos")
    pendientes = numero("SELECT count(*) FROM garantia.evento_dominio WHERE estado = 'PENDIENTE' AND intentos = 0 "
                        "AND ocurrido_en < now() - interval '2 minutes'")
    INF.comprobar("INFRA-2", "el relevo del outbox publica: ningun evento de garantia quedo PENDIENTE sin intentos hace >2 min",
                  pendientes == 0, f"{pendientes} pendientes sin intentar")


# ------------------------------------------------------------------------------------------------ H8.S1.M3
@recorrido("H8.S1.M3")
def h8_m3() -> dict:
    """Aplicar cobertura empresarial: reserva financiada, cubre, mueve fondos corporativos y conserva exposicion y responsable."""
    e, g = preparar()
    ob = obligaciones(g, 1, e)
    pagan = [c for c in [*e.participantes[1:], e.comprador]]  # P1 (el primer receptor) no aporta
    for c in pagan:
        r = pagar(c, ob[c.codigo], "1500.00")
        INF.comprobar("H8.S1.M3-pago", f"{c.codigo} paga su aporte por HTTP (obligacion {ob[c.codigo][:8]})", r.estado == 201 and r.cuerpo.get("estadoObligacion") == "PAGADO", str(r)[:120])
    rec = e.tesoreria.get(f"/aportes/periodos/{g.periodos[0]}/recaudo")
    INF.comprobar("H8.S1.M3-recaudo", "aportes afirma el pozo 6000, confirmado 4500 y faltante 1500 (no lo afirma el cliente)",
                  rec.estado == 200 and rec.cuerpo["pozo"]["monto"] == "6000.00" and rec.cuerpo["confirmado"]["monto"] == "4500.00"
                  and rec.cuerpo["faltante"]["monto"] == "1500.00", str(rec.cuerpo)[:200])

    r = reserva(e, g, "3000.00")
    INF.comprobar("H8.S1.M3-reserva", "garantia reserva 3000 de la capacidad empresarial (201)", r.estado == 201 and r.cuerpo.get("esNueva") is True, str(r)[:200])
    reserva_id = r.cuerpo.get("reservaId")
    # La reserva tiene SU asiento (AJUSTE: reserva debe / caja empresa haber) y llega por el relevo: se espera ANTES de medir la
    # linea base, o el delta de la cobertura se mezcla con el de la reserva (hallado en la primera corrida real).
    aj = esperar(lambda: asientos_contables("AJUSTE", reserva_id), 60, 1, "asiento AJUSTE de la reserva")
    INF.comprobar("H8.S1.M3-mayor-reserva", "el mayor asienta la reserva (3000) que cuadra, antes de cualquier cobertura",
                  len(aj) == 1 and aj[0]["debe"] == aj[0]["haber"] == D("3000.00"), str(aj))
    antes_reserva = saldo_contable("9.1.02")
    antes_pozo = saldo_contable("9.1.03")

    f = fondear_pozo(e, g, 1)
    ok = f.estado == 201 and f.cuerpo.get("estado") == "FONDEADO" and f.cuerpo["cubiertoEmpresa"]["monto"] == "1500.00"
    INF.comprobar("H8.S1.M3-cobertura", "entregas -> aportes -> garantia: el pozo queda FONDEADO y la empresa cubre 1500", ok, str(f)[:300])
    cobertura = uno("SELECT id FROM garantia.cobertura_respaldo WHERE turno_id = %s", g.turnos[0])
    INF.comprobar("H8.S1.M3-cobertura-fila", "hay UNA cobertura viva del turno, con responsable y solicitante", cobertura is not None and
                  uno("SELECT responsable_id FROM garantia.cobertura_respaldo WHERE id = %s", cobertura) == e.admin.id, f"cobertura={cobertura}")
    fila = psql("SELECT monto_reservado, monto_aplicado, monto_recuperado FROM garantia.reserva_respaldo WHERE id = %s", reserva_id)
    INF.comprobar("H8.S1.M3-exposicion", "la reserva conserva la exposicion: reservado 3000, aplicado 1500, recuperado 0",
                  bool(fila) and tuple(D(x) for x in fila[0]) == (D("3000"), D("1500"), D("0")), str(fila))

    # El mayor: el evento viaja por el relevo y Kafka y nucleo-financiero lo asienta UNA vez.
    evento_publicado("garantia", "garantia.cobertura_empresarial_aplicada", cobertura)
    asientos = esperar(lambda: asientos_contables("COBERTURA", cobertura), 60, 1, "asiento COBERTURA en el mayor")
    cuadra = len(asientos) == 1 and asientos[0]["debe"] == asientos[0]["haber"] == D("1500.00")
    INF.comprobar("H8.S1.M3-mayor", "el mayor de nucleo tiene UN asiento COBERTURA que cuadra (debe = haber = 1500)", cuadra, str(asientos))
    INF.comprobar("H8.S1.M3-cuentas", "cuentas logicas -> plan: POZO_GRUPO (9.1.03) +1500 al debe, RESERVA_RESPALDO (9.1.02) -1500",
                  saldo_contable("9.1.03") - antes_pozo == D("1500.00") and saldo_contable("9.1.02") - antes_reserva == D("-1500.00"),
                  f"pozo {saldo_contable('9.1.03') - antes_pozo}, reserva {saldo_contable('9.1.02') - antes_reserva}")

    # Idempotencia y carrera por turno: repetir con la misma clave y con OTRA no abre otra cobertura ni otra entrega.
    f2 = fondear_pozo(e, g, 1, clave())
    n_cob = numero("SELECT count(*) FROM garantia.cobertura_respaldo WHERE turno_id = %s AND estado <> 'REVERSADA'", g.turnos[0])
    n_ent = numero("SELECT count(*) FROM entregas.entrega_fondo WHERE turno_id = %s", g.turnos[0])
    INF.comprobar("H8.S1.M3-idempotencia", "repetir el fondeo (otra clave): misma entrega, UNA cobertura y UNA entrega",
                  f2.estado in (200, 201) and f2.cuerpo.get("entregaId") == f.cuerpo.get("entregaId") and n_cob == 1 and n_ent == 1,
                  f"entregas={n_ent} coberturas={n_cob} -> {f2}"[:200])
    return {"e": e, "g": g, "cobertura": cobertura, "entrega": f.cuerpo.get("entregaId"), "reserva": reserva_id, "ob": ob}


# ------------------------------------------------------------------------------------------------ H8.S1.M4
@recorrido("H8.S1.M4")
def h8_m4() -> None:
    """Entregar el pozo completo UNA sola vez: fondeo -> autorizacion -> ejecucion -> desembolso con confirmacion externa."""
    e, g = preparar(3)
    ob = obligaciones(g, 1, e)
    for c in [*e.participantes, e.comprador]:
        pagar(c, ob[c.codigo], "1500.00")
    r = fondear_pozo(e, g, 1)
    INF.comprobar("H8.S1.M4-fondeo", "pozo completo (6000, sin cobertura) FONDEADO con pozo = confirmado + cubierto",
                  r.estado == 201 and r.cuerpo.get("estado") == "FONDEADO" and r.cuerpo["pozo"]["monto"] == "6000.00"
                  and r.cuerpo["cubiertoEmpresa"]["monto"] == "0.00" and r.cuerpo["pendiente"]["monto"] == "0.00", str(r)[:300])
    entrega = r.cuerpo.get("entregaId")
    a = e.riesgos.post(f"/entregas/{entrega}/autorizacion")
    INF.comprobar("H8.S1.M4-autoriza", "quien autoriza (RESPONSABLE_RIESGOS) autoriza", a.estado == 200 and a.cuerpo.get("estado") == "AUTORIZADA", str(a)[:200])
    mismo = e.riesgos.post(f"/entregas/{entrega}/ejecucion", {"montoEntregado": dinero("6000.00")})
    INF.comprobar("H8.S1.M4-segregacion", "quien autoriza NO ejecuta (R-SEG-04): el permiso lo frena antes (403) o la regla (422)",
                  mismo.estado in (403, 422), str(mismo)[:160])
    x = e.tesoreria.post(f"/entregas/{entrega}/ejecucion", {"montoEntregado": dinero("6000.00")})
    INF.comprobar("H8.S1.M4-ejecuta", "otra persona (TESORERIA) la ejecuta: ENTREGADA por 6000.00",
                  x.estado == 200 and x.cuerpo.get("estado") == "ENTREGADA" and x.cuerpo["montoEntregado"]["monto"] == "6000.00", str(x)[:200])
    otra = e.tesoreria.post(f"/entregas/{entrega}/ejecucion", {"montoEntregado": dinero("6000.00")})
    INF.comprobar("H8.S1.M4-una-vez", "ejecutar de nuevo se rechaza (422): el beneficiario no cobra dos veces",
                  otra.estado == 422, str(otra)[:160])
    n = numero("SELECT count(*) FROM entregas.entrega_fondo WHERE turno_id = %s", g.turnos[0])
    INF.comprobar("H8.S1.M4-unica", "existe UNA sola entrega del turno (R-GRP-01)", n == 1, f"entregas={n}")
    INF.registrar("H8.S1.M4-desembolso", "desembolso con confirmacion externa y comprobante contra el mayor", "INFO",
                  "se ejercita en el recorrido `desembolso` (cuenta verificada -> orden -> respuesta -> asiento ENTREGA en el mayor)")


# ------------------------------------------------------------------------------------------------ H9.S1.M5
@recorrido("H9.S1.M5")
def h9_m5() -> None:
    """Liquidar el pago y cambiar de titular: entregas (saga) <-> nucleo-financiero reales, token SERVICIO_ENTREGAS real."""
    cuenta_de_servicio_de_entregas()  # la identidad de servicio con que entregas le paga al vendedor
    e, g = preparar(3, abiertos=1, periodos=2, saldo="0")
    fondear_por_libro(e.billeteras[e.comprador.codigo], D("6000.00"))
    vendedor = e.participantes[1]  # P2 cobra en el turno 2
    precio, cargos = "5500.00", "50.00"
    oferta = oferta_publicada(g, 2, vendedor, precio, cargos)
    antes_comprador = saldo_del_libro(e.billeteras[e.comprador.codigo])
    antes_vendedor = saldo_del_libro(e.billeteras[vendedor.codigo])
    antes_plataforma = saldo_del_libro(lib.ID_INGRESOS)

    # Ya existe el mecanismo: la billetera del comprador sale de SU sesion.
    cu = e.comprador.get("/billetera/cuentas/propia", consulta={"moneda": "BOB"})
    INF.comprobar("H9.S1.M5-cuenta", "nucleo entrega la billetera de la sesion (GET /billetera/cuentas/propia)",
                  cu.estado == 200 and cu.cuerpo.get("cuentaId") == e.billeteras[e.comprador.codigo], str(cu)[:160])

    c = e.comprador.post(f"/entregas/mercado/ofertas/{oferta}/compra", clave_=clave())
    INF.comprobar("H9.S1.M5-compra", "la compra termina LIQUIDADA (200): retener -> titulo -> pago-a-tercero con token de SERVICIO",
                  c.estado == 200 and c.cuerpo.get("estado") == "LIQUIDADA" and c.cuerpo.get("liquidada") is True, str(c)[:240])
    cesion = c.cuerpo.get("cesionId")
    ret = psql("SELECT id, estado, monto FROM nucleo_financiero.retencion_saldo WHERE referencia_id = %s", oferta)
    INF.comprobar("H9.S1.M5-retencion", "una sola retencion COMPRA_EN_MERCADO EJECUTADA por el precio", len(ret) == 1 and ret[0][1] == "EJECUTADA" and D(ret[0][2]) == D(precio), str(ret))
    d_comprador = saldo_del_libro(e.billeteras[e.comprador.codigo]) - antes_comprador
    d_vendedor = saldo_del_libro(e.billeteras[vendedor.codigo]) - antes_vendedor
    d_plataforma = saldo_del_libro(lib.ID_INGRESOS) - antes_plataforma
    INF.comprobar("H9.S1.M5-mayor", "el libro cuadra: comprador -5500, y vendedor + plataforma = +5500 (sistema cerrado)",
                  d_comprador == D("-5500.00") and d_vendedor + d_plataforma == D("5500.00"),
                  f"comprador {d_comprador}, vendedor {d_vendedor}, plataforma {d_plataforma}")
    if d_vendedor == D(precio) and d_plataforma == D("0.00"):
        INF.brecha("H9.S1.M5-cargos", "reparto de los cargos de la oferta (DECISION_REQUIRED, ambiguedad A9)",
                   f"entregas declara que el vendedor cobra precio-cargos ({D(precio) - D(cargos)}) y la plataforma {cargos}; nucleo le pago "
                   f"al vendedor {d_vendedor} y a la plataforma {d_plataforma}: dos servicios, dos lecturas. La OfertaParaPago es minima A "
                   "PROPOSITO (no lleva cargos) y un test lo exige: decidirlo es de Direccion comercial/tarifas, no se resuelve por conveniencia")
    else:
        INF.comprobar("H9.S1.M5-cargos", "reparto de los cargos coincide con entregas", d_vendedor == D(precio) - D(cargos) and d_plataforma == D(cargos),
                      f"vendedor {d_vendedor}, plataforma {d_plataforma}")
    estados = psql("SELECT o.estado, c.estado FROM entregas.oferta_turno o JOIN entregas.cesion_derecho c ON c.oferta_turno_id = o.id WHERE o.id = %s", oferta)
    INF.comprobar("H9.S1.M5-estado", "oferta VENDIDA y cesion LIQUIDADA", estados and estados[0] == ["VENDIDA", "LIQUIDADA"], str(estados))
    evento_publicado("entregas", "entregas.derecho_cedido", cesion)
    INF.comprobar("H9.S1.M5-evento", "entregas publico entregas.derecho_cedido (PUBLICADO por el relevo)", True)
    # Carril I: grupos consume entregas.derecho_cedido y el cupo cambia de titular (antes: «el cupo sigue de P2»).
    comprador_participante = g.participantes[3]
    try:
        titular = esperar(lambda: uno("SELECT participante_id FROM grupos.cupo WHERE id = %s", g.cupos[1]) == comprador_participante, 90, 2,
                          "grupos asigna el cupo al comprador")
    except TimeoutError as ex:
        titular = False
        INF.registrar("H9.S1.M5-titular-espera", "grupos consume entregas.derecho_cedido", "FAIL", str(ex))
    traspasos = psql("SELECT motivo, deuda_transferida, derecho_cobro_transferido FROM grupos.traspaso_cupo WHERE cupo_id = %s", g.cupos[1])
    INF.comprobar("H9.S1.M5-titular", "el cupo del turno 2 pasa de P2 (vendedor) al comprador, con UN traspaso VENTA (deuda 0, derecho de cobro transferido)",
                  bool(titular) and len(traspasos) == 1 and traspasos[0][0] == "VENTA" and D(traspasos[0][1]) == D("0") and traspasos[0][2] in ("t", "true", "True"),
                  f"cupo={uno('SELECT participante_id FROM grupos.cupo WHERE id = %s', g.cupos[1])} traspasos={traspasos}")
    ev = evento_id("entregas", "entregas.derecho_cedido", cesion)
    if ev:
        reentregar("entregas", ev)
        esperar(lambda: uno("SELECT estado FROM entregas.evento_dominio WHERE id = %s AND estado = 'PUBLICADO' AND intentos >= 2", ev), 60, 1,
                "el derecho_cedido sale por segunda vez")
        descartado = esperar(lambda: any(ev in linea and "DUPLICADO" in linea for linea in logs_de("grupos")), 60, 2,
                             "grupos descarta el derecho_cedido repetido")
        INF.comprobar("H9.S1.M5-titular-duplicado", "el MISMO derecho_cedido entregado dos veces: grupos lo descarta y no escribe otro traspaso",
                      bool(descartado) and numero("SELECT count(*) FROM grupos.traspaso_cupo WHERE cupo_id = %s", g.cupos[1]) == 1,
                      f"traspasos={numero('SELECT count(*) FROM grupos.traspaso_cupo WHERE cupo_id = %s', g.cupos[1])}")
    # Reintento: la misma clave de compra no cobra dos veces.
    c2 = e.comprador.post(f"/entregas/mercado/ofertas/{oferta}/compra", clave_=clave())
    INF.comprobar("H9.S1.M5-no-dos-veces", "comprar de nuevo la oferta ya vendida se rechaza y el libro no se mueve",
                  c2.estado == 422 and saldo_del_libro(e.billeteras[e.comprador.codigo]) - antes_comprador == D("-5500.00"), str(c2)[:160])
    INF.comprobar("H9.S1.M5-cuadre", "ninguna transaccion del libro quedo descuadrada y la suma global sigue en cero",
                  not transacciones_descuadradas() and suma_global_del_libro() == D("0"), f"{transacciones_descuadradas()[:3]}")


TODOS = {"h8m3": h8_m3, "h8m4": h8_m4, "h9m5": h9_m5}


def main(argv: list[str]) -> int:
    from carril_i import RECORRIDOS_I  # los que apagan un servicio a proposito (98.8.4): no entran en la corrida por omision
    from h14 import RECORRIDOS_H14  # despues de los de dinero: usan el mismo informe

    todos = {**TODOS, **RECORRIDOS_H14}
    quiero = argv[1:] or ["infra", *todos]
    todos = {**todos, **RECORRIDOS_I}
    if "infra" in quiero:
        infraestructura()
    for nombre in quiero:
        if nombre in todos:
            todos[nombre]()
    return INF.resumen()


if __name__ == "__main__":
    sys.exit(main(sys.argv))
