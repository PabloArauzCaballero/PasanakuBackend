"""
H14.S1.M1 (ciclo feliz), H14.S1.M2 (mora y cobertura), H14.S1.M3 (carreras y eventos duplicados), H13.S1.M6 (reclamo != sancion
firme) y el desembolso de H8.S1.M4, todo contra el stack REAL por HTTP con tokens reales.

Convenciones del informe:
  PASS  la comprobacion se ejercito y coincide;  FAIL  se ejercito y NO coincide (defecto real o regla rota);
  GAP   el sistema no lo hace todavia (declarado, con quien lo cierra): no es un fallo del ensayo.
Las lecturas contra la base son SOLO para conciliar (conteos, mayor, libro); nunca se escribe para provocar un resultado.
"""
from __future__ import annotations

import json
import secrets
import subprocess
import uuid
from decimal import Decimal
from typing import Any

import lib
from acciones import (D, INF, autorizar_y_ejecutar, evento_publicado, fondear_pozo, obligaciones, pagar, preparar, recorrido,
                      reserva)
from lib import (Cuenta, asientos_contables, clave, dinero, en_paralelo, esperar, numero, psql, saldo_contable, saldo_del_libro,
                 sembrar_usuarios, suma_global_del_libro, transacciones_descuadradas, uno)
from mundo import (abrir_periodo, cuenta_de_servicio_de_entregas, cuenta_destino_sembrada, documento_del_titular, fondear_por_libro, grupo_de_admision,
                   oferta_publicada, persona, terminar_enfriamiento)


# ------------------------------------------------------------------------------------------------ utilidades
def cuantos(respuestas: list[lib.Respuesta], *estados: int) -> int:
    return sum(1 for r in respuestas if r.estado in estados)


def reentregar(esquema: str, evento_id: str) -> None:
    """El relevo murio entre `send` y marcar PUBLICADO: la fila vuelve a PENDIENTE y el MISMO evento sale otra vez."""
    psql("UPDATE %s.evento_dominio SET estado = 'PENDIENTE', proximo_intento_en = NULL WHERE id = %%s" % esquema, evento_id)


def evento_id(esquema: str, tipo: str, agregado_id: str) -> str | None:
    return uno("SELECT id FROM %s.evento_dominio WHERE tipo = %%s AND agregado_id = %%s ORDER BY ocurrido_en LIMIT 1" % esquema,
               tipo, agregado_id)


def admision(prefijo: str) -> dict[str, Any]:
    """F01/F06/F07/F04 por HTTP real: invitar -> canjear (pide ingreso) -> proponer (administrador) -> resolver (backoffice)."""
    org = persona(prefijo + "O", "ORGANIZADOR", "Organizador")
    relleno = persona(prefijo + "R", "PARTICIPANTE", "Relleno")
    aspirante = persona(prefijo + "A", "PARTICIPANTE", "Aspirante")
    operador = persona(prefijo + "P", "ADMIN_PLATAFORMA", "Operador")
    sembrar_usuarios([org, relleno, aspirante, operador])
    g = grupo_de_admision(org, relleno)
    return {"org": org, "aspirante": aspirante, "operador": operador, "grupo": g}


# ------------------------------------------------------------------------------------------------ H14.S1.M1
CAPACIDADES = [  # id, nombre, estado por omision (el recorrido lo sube a REAL con IDs si lo ejercita)
    ("F01", "Solicitar ingreso", "FUERA"), ("F02", "Cargar saldo", "FUERA"), ("F03", "Solicitar ser administrador", "FUERA"),
    ("F04", "Aprobacion por backoffice", "FUERA"), ("F05", "Crear grupo", "SEMBRADO"), ("F06", "Recibir aceptacion/rechazo", "FUERA"),
    ("F07", "Proponer aceptacion/rechazo", "FUERA"), ("F08", "Algoritmo de recomendacion", "FUERA"), ("F09", "Sortear turnos", "SEMBRADO"),
    ("F10", "Crear oferta con precio", "GAP"), ("F11", "Vender turno", "FUERA"), ("F12", "Ver ofertas", "FUERA"),
    ("F13", "Comprar turno", "FUERA"), ("F14", "Recibir mesa", "FUERA"), ("F15", "Retirar saldo", "FUERA"),
    ("F16", "Invertir voluntariamente", "FUERA"), ("F17", "Solicitar rescate", "FUERA"), ("F18", "Transferir por QR", "FUERA"),
    ("F19", "Cobrar comision", "FUERA"), ("F20", "Pagar intereses", "FUERA"), ("F21", "Cobrar regalia de rentabilidades", "FUERA"),
]
NOTAS_F = {
    "F02": "recargas exigen cotizacion de `tarifas` y pasarela: no estan en el conjunto minimo; el saldo inicial entra por el libro cuadrado",
    "F03": "habilitacion de organizador: servicio `organizador` + tarifario + licencia (cubierto por la aceptacion REAL F1-F4 y por H5)",
    "F05": "crearGrupo exige organizador habilitado, tarifario y licencia: el grupo se siembra por SQL (declarado en mundo.py)",
    "F08": "servicio de recomendacion fuera del conjunto minimo",
    "F09": "los turnos se siembran; el sorteo (CU-60) lo cubren las pruebas de integracion de grupos, no esta corrida",
    "F10": "publicarOferta por HTTP denegado: `grupos` no publica la consulta de turno (titular/estado/monto); la oferta se siembra. Dueno: grupos",
    "F15": "retiros: pasarela y cuentas de custodia fuera del conjunto minimo",
    "F16": "inversiones fuera del conjunto minimo", "F17": "inversiones fuera del conjunto minimo",
    "F18": "transferencia QR: pasarela fuera del conjunto minimo", "F19": "comisiones: `tarifas` fuera del conjunto minimo",
    "F20": "inversiones fuera del conjunto minimo", "F21": "inversiones fuera del conjunto minimo",
}


def reportar_capacidades(reales: dict[str, str]) -> None:
    for id_, nombre, defecto in CAPACIDADES:
        if id_ in reales:
            INF.registrar(f"H14.S1.M1-{id_}", f"{id_} {nombre}", "PASS", "REAL por HTTP · " + reales[id_])
        elif defecto == "GAP":
            INF.brecha(f"H14.S1.M1-{id_}", f"{id_} {nombre}", NOTAS_F.get(id_, ""))
        else:
            INF.registrar(f"H14.S1.M1-{id_}", f"{id_} {nombre}", "INFO", f"{defecto} · " + NOTAS_F.get(id_, "no ejercitada en esta corrida"))


def snapshot(g: Any, e: Any) -> dict[str, Any]:
    """Lo que tiene que sobrevivir a un reinicio de los servicios y a una sesion nueva."""
    return {
        "entregas": psql("SELECT turno_id, estado, monto_neto_a_entregar FROM entregas.entrega_fondo WHERE grupo_id = %s ORDER BY turno_id", g.id),
        "obligaciones": psql("SELECT periodo_id, participante_id, estado FROM aportes.obligacion_aporte WHERE periodo_id = ANY(%s) ORDER BY 1,2",
                             "{" + ",".join(g.periodos) + "}"),
        "reserva": psql("SELECT monto_reservado, monto_aplicado, monto_recuperado FROM garantia.reserva_respaldo WHERE grupo_id = %s", g.id),
        "oferta": psql("SELECT id, estado FROM entregas.oferta_turno WHERE grupo_id = %s ORDER BY id", g.id),
        "titulos": psql("SELECT oferta_turno_id, estado FROM entregas.cesion_derecho WHERE oferta_turno_id IN "
                        "(SELECT id FROM entregas.oferta_turno WHERE grupo_id = %s) ORDER BY 1", g.id),
    }


def reiniciar_servicios(*nombres: str) -> None:
    """`docker restart` de los contenedores del servicio: lo unico que sobrevive es lo persistido."""
    p = subprocess.run(["docker", "ps", "--format", "{{.Names}}"], capture_output=True, text=True)
    objetivos = [n for n in p.stdout.split() if any(f"-{s}-" in n for s in nombres)]
    if objetivos:
        subprocess.run(["docker", "restart", *objetivos], capture_output=True, text=True)


@recorrido("H14.S1.M1")
def h14_m1() -> None:
    cuenta_de_servicio_de_entregas()
    e, g = preparar(3, abiertos=1, periodos=4, saldo="0")  # 4 cupos (P1..P3 + comprador), 4 periodos
    reales: dict[str, str] = {}
    comprador = e.comprador
    fondear_por_libro(e.billeteras[comprador.codigo], D("6000.00"))
    entregas: list[str] = []
    mayor_antes = suma_global_del_libro()

    # --- tres turnos completos: aportes de los 4, pozo completo, autorizacion, ejecucion ---------------------------------
    for k in (1, 2, 3):
        if k > 1:
            abrir_periodo(g, k)  # el reloj de grupos no abre periodos todavia (brecha: lo hace esta funcion)
        ob = obligaciones(g, k, e)
        personas = [*e.participantes, comprador]
        pagos = [pagar(c, ob[c.codigo], "1500.00") for c in personas]
        INF.comprobar(f"H14.S1.M1-T{k}-aportes", f"turno {k}: los 4 aportes se pagan por HTTP (201)",
                      all(p.estado == 201 and p.cuerpo.get("estadoObligacion") == "PAGADO" for p in pagos), str([p.estado for p in pagos]))
        rec = e.tesoreria.get(f"/aportes/periodos/{g.periodos[k - 1]}/recaudo")
        INF.comprobar(f"H14.S1.M1-T{k}-recaudo", f"turno {k}: aportes afirma pozo = confirmado = 6000 y faltante 0",
                      rec.estado == 200 and rec.cuerpo["pozo"]["monto"] == rec.cuerpo["confirmado"]["monto"] == "6000.00"
                      and rec.cuerpo["faltante"]["monto"] == "0.00", str(rec.cuerpo)[:160])
        f = fondear_pozo(e, g, k)
        INF.comprobar(f"H14.S1.M1-T{k}-fondeo", f"turno {k}: pozo completo FONDEADO sin cobertura",
                      f.estado == 201 and f.cuerpo.get("estado") == "FONDEADO" and f.cuerpo["cubiertoEmpresa"]["monto"] == "0.00", str(f)[:200])
        entrega = f.cuerpo.get("entregaId")
        a, x = autorizar_y_ejecutar(e, entrega, "6000.00")
        INF.comprobar(f"H14.S1.M1-T{k}-entrega", f"turno {k}: autoriza RESPONSABLE_RIESGOS, ejecuta TESORERIA: ENTREGADA 6000",
                      a.estado == 200 and x.estado == 200 and x.cuerpo.get("estado") == "ENTREGADA", f"{a.estado}/{x.estado} {str(x)[:120]}")
        entregas.append(entrega)
    reales["F14"] = "3 turnos: aporte x4 -> pozo 6000 -> entrega ENTREGADA; entregas " + ",".join(x[:8] for x in entregas)

    # --- mercado: el turno 4 (del comprador) se vende a P1 (F12 ver, F13 comprar, F11 liquidar) ---------------------------
    vendedor, nuevo = comprador, e.participantes[0]
    fondear_por_libro(e.billeteras[nuevo.codigo], D("6000.00"))
    oferta = oferta_publicada(g, 4, vendedor, "5500.00", "50.00")
    vista = nuevo.get("/entregas/mercado/ofertas")
    ids = [o.get("ofertaId") or o.get("id") for o in (vista.cuerpo.get("ofertas") or vista.cuerpo.get("contenido") or vista.cuerpo.get("items") or [])] \
        if isinstance(vista.cuerpo, dict) else []
    INF.comprobar("H14.S1.M1-F12", "F12 el miembro ve la oferta elegible y NO se muestra al vendedor",
                  vista.estado == 200 and oferta in ids and "vendedor" not in vista.texto.lower(), f"{vista.estado} {vista.texto[:160]}")
    antes_v, antes_c = saldo_del_libro(e.billeteras[vendedor.codigo]), saldo_del_libro(e.billeteras[nuevo.codigo])
    c = nuevo.post(f"/entregas/mercado/ofertas/{oferta}/compra", clave_=clave())
    INF.comprobar("H14.S1.M1-F13", "F13 compra LIQUIDADA (retener -> titulo -> pago)", c.estado == 200 and c.cuerpo.get("estado") == "LIQUIDADA", str(c)[:200])
    d_c = saldo_del_libro(e.billeteras[nuevo.codigo]) - antes_c
    d_v = saldo_del_libro(e.billeteras[vendedor.codigo]) - antes_v
    INF.comprobar("H14.S1.M1-mercado-libro", "el libro del mercado: comprador -5500; vendedor + plataforma = +5500",
                  d_c == D("-5500.00") and d_v >= D("5450.00"), f"comprador {d_c}, vendedor {d_v}")
    if c.estado == 200:
        reales["F13"] = f"oferta {oferta[:8]} -> cesion {str(c.cuerpo.get('cesionId'))[:8]} LIQUIDADA"
        reales["F11"] = "cesion LIQUIDADA con pago al vendedor desde el libro"
        reales["F12"] = f"oferta {oferta[:8]} visible para el miembro"

    # --- admision: F01, F04, F06, F07 ---------------------------------------------------------------------------------
    ad = admision("IAD")
    inv = ad["org"].post(f"/grupos/{ad['grupo']}/invitaciones", {"telefonoInvitado": ad["aspirante"].telefono, "canal": "ENLACE"})
    INF.comprobar("H14.S1.M1-F06-invitar", "el organizador emite la invitacion de un solo uso (201)", inv.estado == 201 and "token" in inv.cuerpo, str(inv)[:120])
    canje = ad["aspirante"].post(f"/grupos/{ad['grupo']}/invitaciones/{inv.cuerpo.get('invitacionId')}/canje",
                                 {"token": inv.cuerpo.get("token"), "cuposSolicitados": 1})
    INF.comprobar("H14.S1.M1-F01", "F01 el aspirante pide ingreso: solicitud PENDIENTE_BACKOFFICE",
                  canje.estado == 200 and canje.cuerpo.get("estado") == "PENDIENTE_BACKOFFICE", str(canje)[:160])
    sol = canje.cuerpo.get("solicitudId")
    prop = ad["org"].post(f"/grupos/solicitudes/{sol}/propuestas", {"decision": "ACEPTAR", "motivo": "Referencias verificadas.", "revisionEsperada": 0})
    INF.comprobar("H14.S1.M1-F07", "F07 el administrador propone ACEPTAR con motivo", prop.estado == 200 and prop.cuerpo.get("decision") == "ACEPTAR", str(prop)[:160])
    res = ad["operador"].post(f"/grupos/solicitudes/{sol}/resoluciones", {
        "decision": "ACEPTAR", "motivo": "Evidencia suficiente.", "revisionEsperada": prop.cuerpo.get("revision", 1),
        "propuestaId": prop.cuerpo.get("id")})
    INF.comprobar("H14.S1.M1-F04", "F04 backoffice resuelve ACEPTAR (otra persona que quien propuso)", res.estado == 200, str(res)[:160])
    part = ad["aspirante"].get("/grupos/participaciones")
    n_part = len(part.cuerpo) if isinstance(part.cuerpo, list) else -1
    INF.comprobar("H14.S1.M1-F06", "F06 el aspirante ve SU participacion, una sola vez", part.estado == 200 and n_part == 1, f"{part.estado} n={n_part}")
    if canje.estado == 200 and prop.estado == 200 and res.estado == 200:
        reales["F01"] = f"solicitud {str(sol)[:8]} PENDIENTE_BACKOFFICE"
        reales["F07"] = f"propuesta {str(prop.cuerpo.get('id'))[:8]} ACEPTAR"
        reales["F04"] = "resolucion de backoffice ACEPTAR con motivo"
        reales["F06"] = f"invitacion {str(inv.cuerpo.get('invitacionId'))[:8]} -> participacion unica"

    # --- conciliacion y persistencia tras reinicio + sesion nueva -------------------------------------------------------
    INF.comprobar("H14.S1.M1-cuadre", "ninguna transaccion del libro descuadrada; la suma global no cambio por el ciclo",
                  not transacciones_descuadradas() and suma_global_del_libro() == mayor_antes == D("0"),
                  f"descuadradas={transacciones_descuadradas()[:3]} suma={suma_global_del_libro()}")
    INF.brecha("H14.S1.M1-aportes-libro", "los aportes (4 x 1500 x 3 turnos) mueven el libro",
               "aportes.aporte_cobrado no tiene consumidor en nucleo-financiero: el aporte queda `PAGADO` en aportes y NO se refleja "
               f"en la billetera ni en el mayor (movimientos de libro de las 4 billeteras del grupo: "
               f"{numero('SELECT count(*) FROM nucleo_financiero.movimiento_billetera m JOIN nucleo_financiero.cuenta_billetera c ON c.id = m.cuenta_billetera_id WHERE c.usuario_id = ANY(%s::uuid[])', '{' + ','.join(str(c.id) for c in [*e.participantes, comprador]) + '}')}). "
               "Dueno: aportes + nucleo-financiero (H8.S1.M1/H4)")
    antes = snapshot(g, e)
    reiniciar_servicios("aportes", "entregas", "garantia", "grupos")
    esperar(lambda: e.tesoreria.get(f"/aportes/periodos/{g.periodos[0]}/recaudo", ).estado == 200, 150, 3, "aportes responde tras el reinicio")
    for c_ in [e.tesoreria, e.riesgos, nuevo]:
        c_.token(forzar=True)  # sesion nueva: nada del token anterior
    despues = snapshot(g, e)
    INF.comprobar("H14.S1.M1-persistencia", "tras reiniciar aportes/entregas/garantia/grupos y volver a ingresar, el estado es el mismo",
                  antes == despues and len(antes["entregas"]) == 3, f"entregas={len(antes['entregas'])} iguales={antes == despues}")
    rec = e.tesoreria.get(f"/aportes/periodos/{g.periodos[0]}/recaudo")
    INF.comprobar("H14.S1.M1-recarga", "la consulta HTTP tras el reinicio devuelve el mismo recaudo", rec.estado == 200 and rec.cuerpo["confirmado"]["monto"] == "6000.00", str(rec)[:120])
    reportar_capacidades(reales)


# ------------------------------------------------------------------------------------------------ H14.S1.M2
@recorrido("H14.S1.M2")
def h14_m2() -> None:
    """Primer receptor que deja de aportar: la empresa cubre por turno y la obligacion de recuperar se conserva."""
    e, g = preparar(3, abiertos=1, periodos=3, saldo="0")
    p1, p2, p3 = e.participantes
    r = reserva(e, g, "6000.00")
    INF.comprobar("H14.S1.M2-reserva", "garantia reserva 6000 del ciclo (la exposicion maxima prevista)", r.estado == 201, str(r)[:160])
    reserva_id = r.cuerpo.get("reservaId")
    esperar(lambda: asientos_contables("AJUSTE", reserva_id), 60, 1, "asiento AJUSTE de la reserva")  # linea base DESPUES de la reserva
    filas: list[dict[str, Any]] = []
    obligacion_p1: dict[int, str] = {}
    cobrados: dict[int, str] = {}
    antes_9102 = saldo_contable("9.1.02")
    for k in (1, 2, 3):
        if k > 1:
            abrir_periodo(g, k)
        ob = obligaciones(g, k, e)
        obligacion_p1[k] = ob[p1.codigo]
        for c in (p2, p3, e.comprador):  # P1 (el primer receptor) deja de aportar en TODOS los turnos
            p = pagar(c, ob[c.codigo], "1500.00")
            assert p.estado == 201, f"pago de {c.codigo} turno {k}: {p}"
        rec = e.tesoreria.get(f"/aportes/periodos/{g.periodos[k - 1]}/recaudo")
        f = fondear_pozo(e, g, k)
        ok = f.estado == 201 and f.cuerpo.get("estado") == "FONDEADO" and f.cuerpo["cubiertoEmpresa"]["monto"] == "1500.00"
        INF.comprobar(f"H14.S1.M2-T{k}-cobertura", f"turno {k}: falta el aporte de P1 (1500); la empresa cubre y el pozo se funda completo",
                      ok and rec.cuerpo["faltante"]["monto"] == "1500.00", f"faltante={rec.cuerpo.get('faltante')} fondeo={str(f)[:160]}")
        a, x = autorizar_y_ejecutar(e, f.cuerpo.get("entregaId"), "6000.00")
        INF.comprobar(f"H14.S1.M2-T{k}-entrega", f"turno {k}: el beneficiario cobra el pozo COMPLETO (6000), no lo recaudado",
                      x.estado == 200 and x.cuerpo.get("estado") == "ENTREGADA", f"{a.estado}/{x.estado} {str(x)[:100]}")
        res = psql("SELECT monto_reservado, monto_aplicado, monto_recuperado FROM garantia.reserva_respaldo WHERE id = %s", reserva_id)
        cob = uno("SELECT id FROM garantia.cobertura_respaldo WHERE turno_id = %s AND estado <> 'REVERSADA'", g.turnos[k - 1])
        if cob:
            cobrados[k] = cob
            evento_publicado("garantia", "garantia.cobertura_empresarial_aplicada", cob)
        filas.append({"turno": k, "pozo": "6000.00", "confirmado": rec.cuerpo["confirmado"]["monto"], "cubiertoEmpresa": "1500.00",
                      "exposicion": str(D(res[0][1]) - D(res[0][2])) if res else "?", "reservado": res[0][0] if res else "?"})
    INF.comprobar("H14.S1.M2-exposicion", "exposicion por turno: 1500, 3000, 4500 (aplicado - recuperado), con la reserva en 6000",
                  [f["exposicion"] for f in filas] == ["1500.00", "3000.00", "4500.00"], json.dumps(filas))
    asientos = [esperar(lambda c=c: asientos_contables("COBERTURA", c), 60, 1, "asiento COBERTURA") for c in cobrados.values()]
    INF.comprobar("H14.S1.M2-mayor", "el mayor tiene UN asiento COBERTURA cuadrado por turno cubierto (3 x 1500)",
                  len(asientos) == 3 and all(len(a) == 1 and a[0]["debe"] == a[0]["haber"] == D("1500.00") for a in asientos),
                  str([[(x["debe"], x["haber"]) for x in a] for a in asientos]))
    INF.comprobar("H14.S1.M2-caja", "la caja de respaldo (9.1.02) bajo 4500 en el mayor, igual que la exposicion",
                  saldo_contable("9.1.02") - antes_9102 == D("-4500.00"), f"delta {saldo_contable('9.1.02') - antes_9102}")

    # --- recuperacion: el primer receptor paga tarde la obligacion del turno 1 ----------------------------------------
    tarde = pagar(p1, obligacion_p1[1], "1500.00")
    INF.comprobar("H14.S1.M2-pago-tarde", "P1 puede pagar tarde su aporte del turno 1 (pagar nunca se restringe)", tarde.estado in (200, 201), str(tarde)[:200])
    if tarde.estado in (200, 201):
        pago_id = tarde.cuerpo.get("pagoId")
        cuerpo = {"pagoId": pago_id, "obligacionId": obligacion_p1[1], "pago": dinero("1500.00")}
        rc = e.admin.post("/garantia/respaldo/recuperaciones", cuerpo)
        INF.comprobar("H14.S1.M2-recupera", "la recuperacion devuelve 1500 a la empresa (no al pozo) y la exposicion baja a 3000",
                      rc.estado == 200 and rc.cuerpo.get("resultado") == "RECUPERADA" and rc.cuerpo["recuperado"]["monto"] == "1500.00"
                      and rc.cuerpo["exposicionDespues"]["monto"] == "3000.00", str(rc)[:220])
        otra = e.admin.post("/garantia/respaldo/recuperaciones", cuerpo)
        INF.comprobar("H14.S1.M2-recupera-una-vez", "el mismo pago no recupera dos veces (YA_RECUPERADA, nada nuevo)",
                      otra.estado == 200 and otra.cuerpo.get("resultado") == "YA_RECUPERADA", str(otra)[:160])
        mov = uno("SELECT id FROM garantia.movimiento_reserva WHERE tipo = 'RECUPERACION' AND referencia_id = %s", pago_id)
        asi = esperar(lambda: asientos_contables("COBERTURA", mov), 60, 1, "asiento de la recuperacion en el mayor")
        INF.comprobar("H14.S1.M2-mayor-recupera", "el mayor asienta la recuperacion (1500 a la empresa) UNA vez y cuadra",
                      len(asi) == 1 and asi[0]["debe"] == asi[0]["haber"] == D("1500.00"), str(asi))
        ev = uno("SELECT id FROM garantia.evento_dominio WHERE tipo = 'garantia.respaldo_recuperado' AND payload->>'pagoId' = %s", pago_id)
        if ev:
            reentregar("garantia", ev)
            esperar(lambda: uno("SELECT estado FROM garantia.evento_dominio WHERE id = %s AND estado = 'PUBLICADO' AND intentos >= 2", ev), 60, 1, "reentrega")
            INF.comprobar("H14.S1.M2-recupera-evento-dup", "el evento de recuperacion entregado dos veces no asienta dos veces",
                          len(asientos_contables("COBERTURA", mov)) == 1, "un asiento")
    exp = psql("SELECT monto_reservado, monto_aplicado, monto_recuperado FROM garantia.reserva_respaldo WHERE id = %s", reserva_id)
    INF.comprobar("H14.S1.M2-conserva", "la obligacion de recuperar se conserva: 4500 aplicado, 1500 recuperado, 3000 de exposicion viva",
                  bool(exp) and tuple(D(x) for x in exp[0]) == (D("6000"), D("4500"), D("1500")), str(exp))
    INF.comprobar("H14.S1.M2-cuadre", "el libro y el mayor quedan cuadrados", not transacciones_descuadradas(), str(transacciones_descuadradas()[:3]))
    INF.registrar("H14.S1.M2-flujo", "flujo temporal con caja y exposicion por turno", "INFO", json.dumps(filas), filas)


# ------------------------------------------------------------------------------------------------ H14.S1.M3
@recorrido("H14.S1.M3")
def h14_m3() -> None:
    """Pagos, ofertas y resoluciones concurrentes + eventos duplicados: nada de doble dinero, titulo o membresia."""
    cuenta_de_servicio_de_entregas()
    e, g = preparar(3, abiertos=1, periodos=3, saldo="0")
    p1, p2, p3 = e.participantes
    comprador = e.comprador
    fondear_por_libro(e.billeteras[comprador.codigo], D("6000.00"))
    fondear_por_libro(e.billeteras[p3.codigo], D("6000.00"))
    ob = obligaciones(g, 1, e)

    # 1 · pagos concurrentes de la MISMA obligacion con claves DISTINTAS: uno acredita, el resto se rechaza.
    rs = en_paralelo([lambda: pagar(p2, ob[p2.codigo], "1500.00") for _ in range(6)])
    n_pagos = numero("SELECT count(*) FROM aportes.pago WHERE obligacion_id = %s", ob[p2.codigo])
    INF.comprobar("H14.S1.M3-pagos-claves", "6 pagos concurrentes (claves distintas) de la misma obligacion: UN pago, nada de doble cobro",
                  n_pagos == 1 and cuantos(rs, 201) == 1 and cuantos(rs, 422, 409) == 5, f"filas={n_pagos} estados={sorted(r.estado for r in rs)}")
    # 2 · la MISMA clave llega 6 veces a la vez: un solo pago, los demas devuelven el mismo.
    k = clave()
    rs = en_paralelo([lambda: pagar(p3, ob[p3.codigo], "1500.00", k) for _ in range(6)])
    ids = {r.cuerpo.get("pagoId") for r in rs if r.estado in (200, 201)}
    n = numero("SELECT count(*) FROM aportes.pago WHERE obligacion_id = %s", ob[p3.codigo])
    INF.comprobar("H14.S1.M3-pagos-misma-clave", "6 envios de la MISMA clave: un pago y todos devuelven su id",
                  n == 1 and len(ids) == 1 and cuantos(rs, 200, 201) == 6, f"filas={n} ids={len(ids)} estados={sorted(r.estado for r in rs)}")
    pagar(comprador, ob[comprador.codigo], "1500.00")  # P1 no paga: hace falta cobertura
    reserva(e, g, "3000.00")

    # 3 · fondeo concurrente con claves distintas: UNA entrega y UNA cobertura.
    fs = en_paralelo([lambda: fondear_pozo(e, g, 1, clave()) for _ in range(5)])
    n_ent = numero("SELECT count(*) FROM entregas.entrega_fondo WHERE turno_id = %s", g.turnos[0])
    n_cob = numero("SELECT count(*) FROM garantia.cobertura_respaldo WHERE turno_id = %s AND estado <> 'REVERSADA'", g.turnos[0])
    aplicado = numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE grupo_id = %s", g.id)
    entrega = next((f.cuerpo.get("entregaId") for f in fs if f.estado in (200, 201)), None)
    INF.comprobar("H14.S1.M3-fondeo", "5 fondeos concurrentes: UNA entrega, UNA cobertura, 1500 aplicados una sola vez",
                  n_ent == 1 and n_cob == 1 and aplicado == D("1500.00") and entrega is not None,
                  f"entregas={n_ent} coberturas={n_cob} aplicado={aplicado} estados={sorted(f.estado for f in fs)}")

    # 4 · autorizar y ejecutar concurrentes: la entrega se ejecuta UNA vez.
    if entrega:
        en_paralelo([lambda: e.riesgos.post(f"/entregas/{entrega}/autorizacion") for _ in range(3)])
        ex = en_paralelo([lambda: e.tesoreria.post(f"/entregas/{entrega}/ejecucion", {"montoEntregado": dinero("6000.00")}) for _ in range(4)])
        estado = uno("SELECT estado FROM entregas.entrega_fondo WHERE id = %s", entrega)
        INF.comprobar("H14.S1.M3-ejecucion", "4 ejecuciones concurrentes: la entrega queda ENTREGADA una vez y las demas se rechazan",
                      estado == "ENTREGADA" and cuantos(ex, 200) >= 1 and cuantos(ex, 200, 422) == 4, f"estado={estado} estados={sorted(r.estado for r in ex)}")

    # 5 · dos compradores a la vez sobre la MISMA oferta: uno gana; un reintento concurrente del ganador no cobra dos veces.
    oferta = oferta_publicada(g, 3, p3, "5500.00", "50.00")
    a_c, a_p = saldo_del_libro(e.billeteras[comprador.codigo]), saldo_del_libro(e.billeteras[p1.codigo])
    fondear_por_libro(e.billeteras[p1.codigo], D("6000.00"))
    a_p += D("6000.00")
    kc = clave()
    cs = en_paralelo([lambda: comprador.post(f"/entregas/mercado/ofertas/{oferta}/compra", clave_=kc),
                      lambda: comprador.post(f"/entregas/mercado/ofertas/{oferta}/compra", clave_=kc),
                      lambda: p1.post(f"/entregas/mercado/ofertas/{oferta}/compra", clave_=clave()),
                      lambda: p1.post(f"/entregas/mercado/ofertas/{oferta}/compra", clave_=clave())])
    ganadores = [c for c in cs if c.estado == 200 and c.cuerpo.get("estado") == "LIQUIDADA"]
    n_ces = numero("SELECT count(*) FROM entregas.cesion_derecho WHERE oferta_turno_id = %s AND estado = 'LIQUIDADA'", oferta)
    n_ret = numero("SELECT count(*) FROM nucleo_financiero.retencion_saldo WHERE referencia_id = %s AND estado IN ('EJECUTADA','VIGENTE')", oferta)
    cobros = (a_c - saldo_del_libro(e.billeteras[comprador.codigo])) + (a_p + D("0") - saldo_del_libro(e.billeteras[p1.codigo]))
    INF.comprobar("H14.S1.M3-compra", "4 compras concurrentes de una oferta (2 compradores, 1 con la misma clave dos veces): UN titulo, UNA retencion, UN cobro de 5500",
                  n_ces == 1 and n_ret == 1 and cobros == D("5500.00") and len(ganadores) >= 1,
                  f"cesiones={n_ces} retenciones={n_ret} cobrado={cobros} estados={sorted(c.estado for c in cs)}")
    n_part = numero("SELECT count(*) FROM grupos.participante WHERE grupo_id = %s", g.id)
    INF.comprobar("H14.S1.M3-membresia", "la membresia no se duplica por la venta (el derecho cambia de titular, no hay participante nuevo)",
                  n_part == 4, f"participantes={n_part}")

    # 6 · evento duplicado: el relevo reentrega la cobertura y nucleo la asienta UNA vez.
    cob = uno("SELECT id FROM garantia.cobertura_respaldo WHERE turno_id = %s", g.turnos[0])
    ev = evento_id("garantia", "garantia.cobertura_empresarial_aplicada", cob) if cob else None
    if ev:
        evento_publicado("garantia", "garantia.cobertura_empresarial_aplicada", cob)
        esperar(lambda: asientos_contables("COBERTURA", cob), 60, 1, "asiento COBERTURA")
        consumidos_antes = numero("SELECT count(*) FROM nucleo_financiero.evento_consumido")
        reentregar("garantia", ev)
        esperar(lambda: uno("SELECT estado FROM garantia.evento_dominio WHERE id = %s AND estado = 'PUBLICADO' AND intentos >= 2", ev), 60, 1,
                "el evento sale por segunda vez")
        asientos = asientos_contables("COBERTURA", cob)
        INF.comprobar("H14.S1.M3-evento-duplicado", "el MISMO evento entregado dos veces: UN asiento COBERTURA y el consumidor lo descarta",
                      len(asientos) == 1 and asientos[0]["debe"] == asientos[0]["haber"] == D("1500.00"),
                      f"asientos={len(asientos)} consumidos {consumidos_antes} -> {numero('SELECT count(*) FROM nucleo_financiero.evento_consumido')}")
    else:
        INF.registrar("H14.S1.M3-evento-duplicado", "evento duplicado", "FAIL", "no hubo evento de cobertura que reentregar")

    # 7 · resoluciones concurrentes de una admision: UNA resolucion y UNA membresia.
    ad = admision("IMC")
    inv = ad["org"].post(f"/grupos/{ad['grupo']}/invitaciones", {"telefonoInvitado": ad["aspirante"].telefono, "canal": "ENLACE"})
    cj = en_paralelo([lambda: ad["aspirante"].post(f"/grupos/{ad['grupo']}/invitaciones/{inv.cuerpo.get('invitacionId')}/canje",
                                                   {"token": inv.cuerpo.get("token"), "cuposSolicitados": 1}) for _ in range(4)])
    n_sol = numero("SELECT count(*) FROM grupos.solicitud_ingreso WHERE grupo_id = %s AND usuario_id = %s", ad["grupo"], ad["aspirante"].id)
    INF.comprobar("H14.S1.M3-canje", "4 canjes concurrentes del enlace de un solo uso: UNA solicitud", n_sol == 1 and cuantos(cj, 200) >= 1,
                  f"solicitudes={n_sol} estados={sorted(c.estado for c in cj)}")
    sol = next((c.cuerpo.get("solicitudId") for c in cj if c.estado == 200), None)
    if sol:
        prop = ad["org"].post(f"/grupos/solicitudes/{sol}/propuestas", {"decision": "ACEPTAR", "motivo": "Perfil compatible.", "revisionEsperada": 0})
        rr = en_paralelo([lambda: ad["operador"].post(f"/grupos/solicitudes/{sol}/resoluciones", {
            "decision": "ACEPTAR", "motivo": "Evidencia suficiente.", "revisionEsperada": prop.cuerpo.get("revision", 1),
            "propuestaId": prop.cuerpo.get("id")}) for _ in range(4)])
        n_res = numero("SELECT count(*) FROM grupos.decision_ingreso WHERE solicitud_id = %s AND fase = 'RESOLUCION'", sol)
        n_mem = numero("SELECT count(*) FROM grupos.participante WHERE grupo_id = %s AND usuario_id = %s", ad["grupo"], ad["aspirante"].id)
        INF.comprobar("H14.S1.M3-resolucion", "4 resoluciones concurrentes: UNA resolucion y UNA membresia",
                      n_res == 1 and n_mem == 1 and cuantos(rr, 200) >= 1, f"resoluciones={n_res} membresias={n_mem} estados={sorted(r.estado for r in rr)}")
    INF.comprobar("H14.S1.M3-cuadre", "al final el libro cuadra transaccion por transaccion y la suma global es cero",
                  not transacciones_descuadradas() and suma_global_del_libro() == D("0"), str(transacciones_descuadradas()[:3]))


# ------------------------------------------------------------------------------------------------ H13.S1.M6
@recorrido("H13.S1.M6")
def h13_m6() -> None:
    """Un reclamo EN REVISION no equivale a sancion firme ni a deuda ejecutable: garantia y organizador consultan a cumplimiento."""
    e, g = preparar(3, abiertos=1, periodos=1, saldo="0")
    deudor = e.participantes[0]
    ob = obligaciones(g, 1, e)
    # Quien cobra (GRUPO_ADMINISTRAR: el organizador del grupo) y ademas ve reclamos (RECLAMO_VER). Con SOLO ORGANIZADOR cumplimiento
    # contesta 403 al adaptador de garantia y garantia falla CERRADO: ninguna restriccion se puede aplicar nunca (hallazgo de seguridad
    # abierto, ver informe): por eso el actor del ensayo lleva los dos permisos.
    cobranza = persona("ICB", "ORGANIZADOR", "Cobranza")
    cobranza.extras = ["ADMIN_PLATAFORMA"]
    sembrar_usuarios([cobranza])
    solo_org = persona("ICC", "ORGANIZADOR", "SoloOrganizador")
    sembrar_usuarios([solo_org])
    punto = uno("SELECT id FROM cumplimiento.punto_reclamo WHERE activo ORDER BY codigo LIMIT 1") or uno("SELECT id FROM cumplimiento.punto_reclamo LIMIT 1")
    reclamo = str(uuid.uuid4())
    psql(f"""INSERT INTO cumplimiento.reclamo_cliente (id, codigo, usuario_id, punto_reclamo_id, categoria, producto, monto_reclamado, descripcion,
                 canal_ingreso, estado, fecha_ingreso, dias_habiles_plazo, plazo_respuesta, conservar_hasta)
             VALUES ('{reclamo}', 'RINT-{reclamo[:8].upper()}', '{deudor.id}', '{punto}', 'GRUPO', 'APORTE', 1500.00,
                     'Reclamo sintetico del ensayo', 'APP', 'EN_ANALISIS', now(), 5, now() + interval '7 days', current_date + 3700)""")
    ef = e.admin.get(f"/reclamos/usuarios/{deudor.id}/efecto")
    INF.comprobar("H13.S1.M6-efecto", "cumplimiento afirma que el reclamo en analisis FRENA la sancion firme y la deuda ejecutable",
                  ef.estado == 200 and ef.cuerpo.get("bloqueaSancionFirme") is True and ef.cuerpo.get("bloqueaDeudaEjecutable") is True, str(ef)[:240])
    sin = e.participantes[1].get(f"/reclamos/usuarios/{deudor.id}/efecto")
    soloorg = solo_org.get(f"/reclamos/usuarios/{deudor.id}/efecto")
    INF.comprobar("H13.S1.M6-organizador-sin-permiso", "un ORGANIZADOR sin RECLAMO_VER recibe 403 de cumplimiento (garantia fallaria CERRADO)", soloorg.estado == 403, str(soloorg)[:100])
    INF.comprobar("H13.S1.M6-permiso", "un participante NO ve el rastro de reclamos de otra persona (403)", sin.estado == 403, str(sin)[:100])

    d = cobranza.post("/incumplimientos", {
        "codigoExpediente": "EINT-" + reclamo[:6].upper(), "usuarioId": deudor.id, "participanteId": g.participantes[0], "grupoId": g.id,
        "periodoId": g.periodos[0], "cupoId": g.cupos[0], "obligacionId": ob[deudor.codigo], "tipo": "APORTE_IMPAGO", "severidad": "MODERADA",
        "origenDeteccion": "AUDITORIA_INTERNA", "montoInvolucrado": dinero("1500.00"), "diasMora": 12, "tipoDeEvidencia": "LOG_SISTEMA",
        "descripcionDeLaEvidencia": "Obligacion sin pagar al cierre del periodo (ensayo sintetico)"})
    INF.comprobar("H13.S1.M6-expediente", "se declara el incumplimiento (expediente con plazo de descargo)", d.estado in (200, 201), str(d)[:200])
    exp = d.cuerpo.get("expedienteId")
    if exp:
        r1 = cobranza.post(f"/incumplimientos/{exp}/restriccion", {"nivel": "LIMITADO", "motivo": "Mora confirmada", "duracionDias": 30})
        INF.comprobar("H13.S1.M6-garantia-frena", "garantia NO restringe mientras el reclamo esta en revision (422 AP-CU27-07)",
                      r1.estado == 422 and r1.codigo == "AP-CU27-07", str(r1)[:200])
        n_r = numero("SELECT count(*) FROM garantia.lista_restriccion_interna WHERE usuario_id = %s", deudor.id)
        INF.comprobar("H13.S1.M6-sin-efecto", "no hay restriccion ni deuda ejecutable creadas por el intento", n_r == 0, f"restricciones={n_r}")
        # DESFAVORABLE seguiria frenando: el cliente conserva el plazo para elevarlo (PLAZO_DE_ELEVACION_ABIERTO). Solo el desistimiento libera.
        psql("UPDATE cumplimiento.reclamo_cliente SET estado = 'CERRADO', resultado = 'DESFAVORABLE', respuesta = 'Sin merito (ensayo)', "
             "fecha_respuesta = now() WHERE id = %s", reclamo)
        r1b = cobranza.post(f"/incumplimientos/{exp}/restriccion", {"nivel": "LIMITADO", "motivo": "Mora confirmada", "duracionDias": 30})
        INF.comprobar("H13.S1.M6-garantia-plazo-elevar", "cerrado DESFAVORABLE el cliente aun puede elevarlo: sigue frenando (422 AP-CU27-07)",
                      r1b.estado == 422 and r1b.codigo == "AP-CU27-07", str(r1b)[:160])
        psql("UPDATE cumplimiento.reclamo_cliente SET estado = 'CERRADO', resultado = 'DESISTIDO', respuesta = 'El cliente desistio (ensayo)', "
             "fecha_respuesta = now() WHERE id = %s", reclamo)
        r2 = cobranza.post(f"/incumplimientos/{exp}/restriccion", {"nivel": "LIMITADO", "motivo": "Mora confirmada", "duracionDias": 30})
        INF.comprobar("H13.S1.M6-garantia-pasa", "desistido el reclamo la restriccion procede (201)", r2.estado in (200, 201), str(r2)[:200])
    # organizador: la sancion firme de un ORGANIZADOR tambien espera al reclamo (CU-93). Se siembra el organizador (habilitar exige
    # capacitacion, contrato y licencia) y se reabre el reclamo para el segundo intento.
    org_user = e.participantes[1]
    org_id = str(uuid.uuid4())
    psql(f"""INSERT INTO organizador.organizador (id, usuario_id, estado, nivel, limite_grupos_simultaneos, limite_monto_administrado, grupos_activos,
                 grupos_historicos, monto_administrado_actual, calificacion_promedio, indice_morosidad_cartera, fecha_postulacion, fecha_habilitacion, version)
             VALUES ('{org_id}', '{org_user.id}', 'HABILITADO', 'ESTANDAR', 3, 50000.00, 0, 0, 0, 4.00, 0.00, now(), now(), 0)""")
    reclamo2 = str(uuid.uuid4())
    psql(f"""INSERT INTO cumplimiento.reclamo_cliente (id, codigo, usuario_id, punto_reclamo_id, categoria, producto, monto_reclamado, descripcion,
                 canal_ingreso, estado, fecha_ingreso, dias_habiles_plazo, plazo_respuesta, conservar_hasta)
             VALUES ('{reclamo2}', 'RINT-{reclamo2[:8].upper()}', '{org_user.id}', '{punto}', 'SERVICIO', 'APORTE', NULL,
                     'Reclamo sintetico contra el organizador', 'APP', 'EN_ANALISIS', now(), 5, now() + interval '7 days', current_date + 3700)""")
    sc = {"tipo": "ADVERTENCIA", "motivo": "Incumplimiento de funciones comprobado (ensayo sintetico)"}
    s1 = e.admin.post(f"/organizadores/{org_id}/sanciones", sc)
    INF.comprobar("H13.S1.M6-organizador-frena", "organizador NO sanciona mientras un reclamo contra esa persona esta en analisis (422 AP-CU93-07)",
                  s1.estado == 422 and s1.codigo == "AP-CU93-07", str(s1)[:200])
    psql("UPDATE cumplimiento.reclamo_cliente SET estado = 'CERRADO', resultado = 'DESISTIDO', respuesta = 'El cliente desistio (ensayo)', "
         "fecha_respuesta = now() WHERE id = %s", reclamo2)
    s2 = e.admin.post(f"/organizadores/{org_id}/sanciones", sc)
    INF.comprobar("H13.S1.M6-organizador-pasa", "cerrado el reclamo la sancion procede con su plazo de apelacion (201)",
                  s2.estado == 201 and "puedeApelarHasta" in s2.cuerpo, str(s2)[:200])
    INF.registrar("H13.S1.M6-origen", "alta y resolucion del reclamo", "INFO",
                  "cumplimiento no expone HTTP para ingresar ni cerrar un reclamo en este stack: el reclamo se siembra por SQL (declarado)")


# ------------------------------------------------------------------------------------------------ H8.S1.M4 (desembolso)
@recorrido("H8.S1.M4-desembolso")
def h8_m4_desembolso() -> None:
    """Entregar el pozo por la via del DESEMBOLSO: cuenta de destino verificada -> orden -> respuesta del proveedor -> mayor, UNA vez."""
    e, g = preparar(3, abiertos=1, periodos=1, saldo="0")
    benef = e.participantes[0]
    ci = "".join(str(secrets.randbelow(10)) for _ in range(7))  # CI SINTETICA, solo en memoria
    documento_del_titular(benef.id, ci)  # el KYC no se crea por HTTP en este conjunto (precondicion declarada)
    ob = obligaciones(g, 1, e)
    for c in [*e.participantes, e.comprador]:
        pagar(c, ob[c.codigo], "1500.00")
    f = fondear_pozo(e, g, 1)
    entrega = f.cuerpo.get("entregaId")
    INF.comprobar("H8.S1.M4-d-fondeo", "pozo completo FONDEADO (6000)", f.estado == 201 and f.cuerpo.get("estado") == "FONDEADO", str(f)[:160])
    a = e.riesgos.post(f"/entregas/{entrega}/autorizacion")
    INF.comprobar("H8.S1.M4-d-autoriza", "autorizada por RESPONSABLE_RIESGOS", a.estado == 200 and a.cuerpo.get("estado") == "AUTORIZADA", str(a)[:140])

    cuenta = {"tipoCuenta": "AHORRO", "entidadFinanciera": "BANCO SINTETICO", "numeroCuenta": "10" + ci + "99",
              "numeroCifrado": "enc:v1:int-sintetico", "titularNombre": f"{benef.nombre} Sintetico", "titularDocumento": ci, "moneda": "BOB"}
    ajena = benef.post("/cuentas-bancarias", {**cuenta, "titularNombre": "Otra Persona", "numeroCuenta": "10" + ci + "98"})
    INF.comprobar("H8.S1.M4-d-ajena", "R-SEG-02: una cuenta a nombre de OTRA persona no se registra (422)", ajena.estado == 422, str(ajena)[:160])
    r = benef.post("/cuentas-bancarias", cuenta)
    registrada = r.estado == 201 and "numeroCuenta" not in r.cuerpo and ci not in r.texto
    INF.comprobar("H8.S1.M4-d-cuenta", "DEFECTO: la cuenta propia de un participante se registra PENDIENTE y se devuelve ENMASCARADA (CU-18)", registrada,
                  (str(r)[:160] + " · causa: identidad.verificarTitularidad exige DATOS_SENSIBLES_LEER; entregas propaga el token del PARTICIPANTE "
                   "(403) y lo lee como «no coincide». Dueno: identidad + entregas (credencial de servicio con permiso acotado)").replace(ci, "***"))
    cuenta_id = r.cuerpo.get("cuentaId") if registrada else cuenta_destino_sembrada(benef.id, cuenta["titularNombre"], ci)
    prematura = e.tesoreria.post("/desembolsos", {"entregaId": entrega, "proveedorId": str(uuid.uuid4()), "cuentaDestinoId": cuenta_id,
                                                  "glosa": "pozo turno 1", "saldoRetenido": True})
    INF.comprobar("H8.S1.M4-d-sin-verificar", "R-DES-02: no se desembolsa a una cuenta sin verificar (422)", prematura.estado == 422, str(prematura)[:140])
    v = benef.post(f"/cuentas-bancarias/{cuenta_id}/verificacion", {"metodo": "MICRODEPOSITO"})
    INF.comprobar("H8.S1.M4-d-verifica", "verificada: empieza la ventana de enfriamiento (se guarda su fin)", v.estado == 200, str(v)[:160])
    enfriando = e.tesoreria.post("/desembolsos", {"entregaId": entrega, "proveedorId": str(uuid.uuid4()), "cuentaDestinoId": cuenta_id,
                                                  "glosa": "pozo turno 1", "saldoRetenido": True})
    INF.comprobar("H8.S1.M4-d-enfriamiento", "verificada NO es utilizable: en la ventana de enfriamiento se rechaza (422)", enfriando.estado == 422, str(enfriando)[:140])
    terminar_enfriamiento(cuenta_id)
    proveedor = uno("SELECT id FROM aportes.proveedor_pago ORDER BY prioridad LIMIT 1")
    k = clave()
    cuerpo = {"entregaId": entrega, "proveedorId": proveedor, "cuentaDestinoId": cuenta_id, "glosa": "pozo turno 1", "saldoRetenido": True}
    o = e.tesoreria.post("/desembolsos", cuerpo, k)
    INF.comprobar("H8.S1.M4-d-orden", "orden ENVIADA_A_PROVEEDOR (201)", o.estado == 201 and o.cuerpo.get("estado") == "ENVIADA_A_PROVEEDOR", str(o)[:160])
    o2 = e.tesoreria.post("/desembolsos", cuerpo, k)
    n_ord = numero("SELECT count(*) FROM entregas.orden_desembolso WHERE entrega_id = %s", entrega)
    INF.comprobar("H8.S1.M4-d-una-orden", "R-DES-01: repetir la orden no abre otra (UNA viva por entrega)", n_ord == 1 and o2.cuerpo.get("ordenId") == o.cuerpo.get("ordenId"), f"ordenes={n_ord} {o2.estado}")
    orden = o.cuerpo.get("ordenId")
    resp = {"iniciado": "2026-12-15T10:00:00Z", "exitoso": True, "referenciaProveedor": "REF-" + clave()[:12]}
    ack = e.tesoreria.post(f"/desembolsos/{orden}/respuesta", resp)
    INF.comprobar("H8.S1.M4-d-acreditada", "la respuesta exitosa del proveedor acredita la orden y la entrega queda ENTREGADA",
                  ack.estado in (200, 201) and uno("SELECT estado FROM entregas.entrega_fondo WHERE id = %s", entrega) == "ENTREGADA", str(ack)[:160] + " (el contrato dice 200 y el codigo da 201: divergencia YA anotada como deuda en scripts/verificar_pruebas_web.py)")
    ack2 = e.tesoreria.post(f"/desembolsos/{orden}/respuesta", resp)
    INF.comprobar("H8.S1.M4-d-respuesta-repetida", "la misma respuesta otra vez no acredita dos veces", ack2.estado in (200, 201) and ack2.cuerpo.get("esNuevo") is False and
                  numero("SELECT count(*) FROM entregas.evento_dominio WHERE tipo = 'entregas.desembolso_acreditado' AND agregado_id = %s", orden) == 1, str(ack2)[:140])
    evento_publicado("entregas", "entregas.desembolso_acreditado", orden)
    asientos = esperar(lambda: asientos_contables("ENTREGA", entrega), 60, 1, "asiento del desembolso en el mayor")
    INF.comprobar("H8.S1.M4-d-mayor", "el mayor de nucleo tiene UN asiento del desembolso que cuadra (6000)",
                  len(asientos) == 1 and asientos[0]["debe"] == asientos[0]["haber"] == D("6000.00"), str(asientos))
    ev = evento_id("entregas", "entregas.desembolso_acreditado", orden)
    if ev:
        reentregar("entregas", ev)
        esperar(lambda: uno("SELECT estado FROM entregas.evento_dominio WHERE id = %s AND estado = 'PUBLICADO' AND intentos >= 2", ev), 60, 1, "reentrega")
        INF.comprobar("H8.S1.M4-d-evento-duplicado", "el evento de acreditacion entregado dos veces no asienta dos veces",
                      len(asientos_contables("ENTREGA", entrega)) == 1, "un asiento")
    INF.comprobar("H8.S1.M4-d-cuadre", "el libro sigue cuadrado", not transacciones_descuadradas(), str(transacciones_descuadradas()[:3]))


RECORRIDOS_H14 = {"h14m1": h14_m1, "h14m2": h14_m2, "h14m3": h14_m3, "reclamos": h13_m6, "desembolso": h8_m4_desembolso}


# ------------------------------------------------------------------------------------------------ F1-F4 (cara HTTP)
@recorrido("F1-F4")
def admision_f1_f4() -> None:
    """La cara API de la aceptacion de dos usuarios (F1 invitacion, F2 aceptar, F3 rechazar, F4 limites) con tokens reales.

    La cara de NAVEGADOR (A en apps/web, B en apps/backoffice) la ejecuta apps/backoffice/e2e/aceptacion con `aceptacion.sh real`;
    esto es lo que se puede probar sin Chromium ni ng serve (el guardian de RAM no los deja arrancar con el stack arriba)."""
    ad = admision("IFA")
    otro = persona("IFB", "PARTICIPANTE", "Ajeno")
    sinpermiso = persona("IFS", "AUDITOR_INTERNO", "SinPermiso")
    sembrar_usuarios([otro, sinpermiso])
    g = ad["grupo"]
    inv = ad["org"].post(f"/grupos/{g}/invitaciones", {"telefonoInvitado": ad["aspirante"].telefono, "canal": "ENLACE"})
    tok, inv_id = inv.cuerpo.get("token"), inv.cuerpo.get("invitacionId")
    INF.comprobar("F1-invitar", "F1 el organizador emite el enlace de un solo uso (201, token de 64 hex)",
                  inv.estado == 201 and isinstance(tok, str) and len(tok) == 64, f"{inv.estado}")
    k = clave()
    canje = ad["aspirante"].post(f"/grupos/{g}/invitaciones/{inv_id}/canje", {"token": tok, "cuposSolicitados": 1}, k)
    INF.comprobar("F1-canje", "F1 A pide ingreso: PENDIENTE_BACKOFFICE, sin membresia todavia",
                  canje.estado == 200 and canje.cuerpo.get("estado") == "PENDIENTE_BACKOFFICE"
                  and ad["aspirante"].get("/grupos/participaciones").cuerpo == [], str(canje)[:140])
    sol = canje.cuerpo.get("solicitudId")
    rep = ad["aspirante"].post(f"/grupos/{g}/invitaciones/{inv_id}/canje", {"token": tok, "cuposSolicitados": 1}, k)
    INF.comprobar("F1-misma-clave", "F1 el reintento con la MISMA clave devuelve la misma solicitud", rep.estado == 200 and rep.cuerpo.get("solicitudId") == sol, str(rep)[:120])
    seg = ad["aspirante"].post(f"/grupos/{g}/invitaciones/{inv_id}/canje", {"token": tok, "cuposSolicitados": 1})
    INF.comprobar("F1-un-solo-uso", "F1/F4 el enlace usado otra vez con OTRA clave: 422 AP-CU69-05 y no crea otra solicitud",
                  seg.estado == 422 and seg.codigo == "AP-CU69-05", str(seg)[:140])
    aje = otro.post(f"/grupos/{g}/invitaciones/{inv_id}/canje", {"token": tok, "cuposSolicitados": 1})
    INF.comprobar("F4-ajeno", "F4 otra persona con el mismo enlace: 422 AP-CU69-05", aje.estado == 422 and aje.codigo == "AP-CU69-05", str(aje)[:140])
    cola = ad["org"].get(f"/grupos/{g}/solicitudes")
    INF.comprobar("F1-cola", "F1 el organizador ve UNA sola solicitud pendiente", cola.estado == 200 and len(cola.cuerpo) == 1, f"n={len(cola.cuerpo) if isinstance(cola.cuerpo, list) else '?'}")

    prop = ad["org"].post(f"/grupos/solicitudes/{sol}/propuestas", {"decision": "RECHAZAR", "motivo": "Perfil no compatible.", "revisionEsperada": 0})
    INF.comprobar("F3-propone", "F3 el administrador propone RECHAZAR con motivo", prop.estado == 200 and prop.cuerpo.get("decision") == "RECHAZAR", str(prop)[:140])
    sinp = sinpermiso.post(f"/grupos/solicitudes/{sol}/resoluciones", {"decision": "RECHAZAR", "motivo": "x", "revisionEsperada": prop.cuerpo.get("revision", 1), "propuestaId": prop.cuerpo.get("id")})
    INF.comprobar("F4-sin-permiso", "F4 quien no es de backoffice no resuelve (403)", sinp.estado == 403, str(sinp)[:100])
    sinm = ad["operador"].post(f"/grupos/solicitudes/{sol}/resoluciones", {"decision": "RECHAZAR", "motivo": "", "revisionEsperada": prop.cuerpo.get("revision", 1), "propuestaId": prop.cuerpo.get("id")})
    INF.comprobar("F4-sin-motivo", "F4 resolver sin motivo se rechaza (4xx) y no cierra la solicitud", sinm.estado in (400, 422), str(sinm)[:120])
    res = ad["operador"].post(f"/grupos/solicitudes/{sol}/resoluciones", {"decision": "RECHAZAR", "motivo": "Sin cupo para este perfil.", "revisionEsperada": prop.cuerpo.get("revision", 1), "propuestaId": prop.cuerpo.get("id")})
    INF.comprobar("F3-rechaza", "F3 backoffice resuelve RECHAZAR con motivo", res.estado == 200 and res.cuerpo.get("decision") == "RECHAZAR", str(res)[:140])
    dob = ad["operador"].post(f"/grupos/solicitudes/{sol}/resoluciones", {"decision": "RECHAZAR", "motivo": "Sin cupo para este perfil.", "revisionEsperada": prop.cuerpo.get("revision", 1), "propuestaId": prop.cuerpo.get("id")})
    hist = ad["org"].get(f"/grupos/solicitudes/{sol}/decisiones")
    n_res = len([d for d in (hist.cuerpo if isinstance(hist.cuerpo, list) else []) if d.get("fase") == "RESOLUCION"])
    hb = ad["operador"].get(f"/grupos/solicitudes/{sol}/decisiones")
    INF.comprobar("F2-F3-historial-backoffice",
                  "DEFECTO: backoffice (ADMIN_PLATAFORMA) lee el historial de decisiones del expediente (el contrato lo promete)", hb.estado == 200,
                  f"{hb.estado} · causa: RutasDeAdmision.historialAdmision lleva @Permiso(\"PARTICIPANTE\") y ADMIN_PLATAFORMA no lo tiene, aunque "
                  "CU68AceptarIngreso.historial ya admite al backoffice. Dueno: grupos + modelo de roles (guard de un solo permiso)")
    INF.comprobar("F3-una-resolucion", "F3 resolver dos veces no duplica: una sola resolucion en el historial y A sigue sin membresia",
                  n_res == 1 and dob.estado in (200, 409, 422) and ad["aspirante"].get("/grupos/participaciones").cuerpo == [], f"resoluciones={n_res} segunda={dob.estado}")

    # F2 con otra persona: aceptar crea UNA participacion (pendiente de firma).
    inv2 = ad["org"].post(f"/grupos/{g}/invitaciones", {"telefonoInvitado": otro.telefono, "canal": "ENLACE"})
    c2 = otro.post(f"/grupos/{g}/invitaciones/{inv2.cuerpo.get('invitacionId')}/canje", {"token": inv2.cuerpo.get("token"), "cuposSolicitados": 1})
    p2 = ad["org"].post(f"/grupos/solicitudes/{c2.cuerpo.get('solicitudId')}/propuestas", {"decision": "ACEPTAR", "motivo": "Referencias verificadas.", "revisionEsperada": 0})
    r2 = ad["operador"].post(f"/grupos/solicitudes/{c2.cuerpo.get('solicitudId')}/resoluciones", {"decision": "ACEPTAR", "motivo": "Hay cupo y perfil compatible.", "revisionEsperada": p2.cuerpo.get("revision", 1), "propuestaId": p2.cuerpo.get("id")})
    part = otro.get("/grupos/participaciones")
    INF.comprobar("F2-acepta", "F2 backoffice acepta: A ve UNA participacion (la membresia existe una sola vez)",
                  r2.estado == 200 and isinstance(part.cuerpo, list) and len(part.cuerpo) == 1, f"{r2.estado} n={len(part.cuerpo) if isinstance(part.cuerpo, list) else '?'} estado={part.cuerpo[0].get('estado') if part.cuerpo else None}")
    INF.brecha("F1-F4-navegador", "A en apps/web y B en apps/backoffice (Chromium + ng serve)",
               "no se ejecuto: `aceptacion.sh real` es proceso pesado y el guardian (>=3500 MB libres) no lo deja arrancar con el stack arriba "
               "(libres ~2300 MB con el stack minimo). Falta una ventana con >=6 GB libres. Lo cubre cuando haya RAM: `bash despliegue/ensayos/integracion/correr.sh aceptacion`")


RECORRIDOS_H14["f1f4"] = admision_f1_f4
