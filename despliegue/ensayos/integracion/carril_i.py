"""
Carril I (quinta vuelta): lo que se rompe A PROPOSITO contra el stack real para cerrar las brechas de dinero y de identidad.

    cd despliegue/ensayos/integracion && python recorridos.py i-cu18 i-aportes-caida i-cesion-orden

  i-cu18          CU-18: el mecanismo de verificacion de titularidad (credencial de SERVICIO_ENTREGAS) y su matriz negativa por el gateway.
  i-aportes-caida regla 98.8.4: se APAGA nucleo-financiero, se paga un aporte, se vuelve a encender y la plata se mueve UNA vez; traza
                  con el correlationId entre aportes y nucleo-financiero.
  i-cesion-orden  regla 98.8.4 para grupos: se apaga grupos, se vende un turno, se vuelve a encender y el cupo cambia de titular UNA vez.

Cada recorrido arma SU elenco y SU grupo. Un recorrido que se cae no oculta a los demas (ver `acciones.recorrido`).
"""
from __future__ import annotations

import secrets
import subprocess
from typing import Any

from acciones import D, INF, obligaciones, pagar, preparar, recorrido
from h14 import evento_id
from lib import clave, esperar, http, numero, psql, saldo_del_libro, uno
from mundo import armar_elenco, cuenta_de_servicio_de_entregas, documento_del_titular, fondear_por_libro, oferta_publicada


def contenedor(servicio: str) -> str:
    p = subprocess.run(["docker", "ps", "-a", "--format", "{{.Names}}"], capture_output=True, text=True)
    nombres = [n for n in p.stdout.split() if f"-{servicio}-" in n and "-infra-" not in n]
    assert len(nombres) == 1, f"se esperaba UN contenedor de {servicio}: {nombres}"
    return nombres[0]


def sano(nombre: str) -> bool:
    r = subprocess.run(["docker", "inspect", "-f", "{{.State.Health.Status}}", nombre], capture_output=True, text=True)
    return r.stdout.strip() == "healthy"


def apagar(servicio: str) -> str:
    nombre = contenedor(servicio)
    subprocess.run(["docker", "stop", nombre], capture_output=True, text=True, check=True)
    return nombre


def encender(nombre: str) -> None:
    subprocess.run(["docker", "start", nombre], capture_output=True, text=True, check=True)
    esperar(lambda: sano(nombre), 240, 4, f"{nombre} vuelve a estar healthy")


# ------------------------------------------------------------------------------------------------ CU-18
@recorrido("I.CU18")
def cu18_mecanismo() -> None:
    """La verificacion de titularidad contesta si/no a la credencial de SERVICIO_ENTREGAS y a nadie mas."""
    e = armar_elenco(2)
    servicio = cuenta_de_servicio_de_entregas()
    p1, p2 = e.participantes
    ci = "".join(str(secrets.randbelow(10)) for _ in range(7))  # CI SINTETICA, solo en memoria
    documento_del_titular(p1.id, ci)
    nombre = f"{p1.nombre} Sintetico"
    ruta = f"/usuarios/{p1.id}/titularidad/verificacion"

    bien = http("POST", ruta, {"nombreCompleto": nombre, "documento": ci}, token=servicio.token(), clave=clave())
    INF.comprobar("I.CU18-servicio-si", "la credencial de SERVICIO_ENTREGAS verifica el titular: coincide=true y SOLO eso en la respuesta",
                  bien.estado == 200 and bien.cuerpo == {"coincide": True} and ci not in bien.texto and nombre not in bien.texto, f"{bien.estado} {bien.texto[:120]}")
    mal = http("POST", ruta, {"nombreCompleto": nombre, "documento": "".join(str(secrets.randbelow(10)) for _ in range(7))},
               token=servicio.token(), clave=clave())
    INF.comprobar("I.CU18-servicio-no", "otro documento: coincide=false (no es un 403 ni un error)", mal.estado == 200 and mal.cuerpo == {"coincide": False}, f"{mal.estado} {mal.texto[:100]}")
    otro_nombre = http("POST", ruta, {"nombreCompleto": "Otra Persona", "documento": ci}, token=servicio.token(), clave=clave())
    INF.comprobar("I.CU18-servicio-otro-nombre", "el documento correcto con el nombre de OTRA persona: coincide=false",
                  otro_nombre.estado == 200 and otro_nombre.cuerpo == {"coincide": False}, f"{otro_nombre.estado}")
    inexistente = http("POST", "/usuarios/00000000-0000-4000-8000-0000000000ff/titularidad/verificacion", {"nombreCompleto": nombre, "documento": ci},
                       token=servicio.token(), clave=clave())
    INF.comprobar("I.CU18-servicio-inexistente", "un usuario que no existe: coincide=false (no revela si existe)",
                  inexistente.estado == 200 and inexistente.cuerpo == {"coincide": False}, f"{inexistente.estado}")

    # La matriz NEGATIVA: nadie mas pasa.
    cuerpo = {"nombreCompleto": nombre, "documento": ci}
    for etiqueta, quien in (("participante-titular", p1), ("otro-participante", p2), ("tesoreria", e.tesoreria), ("riesgos", e.riesgos)):
        r = http("POST", ruta, cuerpo, token=quien.token(), clave=clave())
        INF.comprobar(f"I.CU18-403-{etiqueta}", f"{etiqueta}: 403 (la titularidad no la verifica una persona sin DATOS_SENSIBLES_LEER ni el servicio de entregas)",
                      r.estado == 403 and ci not in r.texto, f"{r.estado} {r.texto[:80]}")
    sin = http("POST", ruta, cuerpo, clave=clave())
    INF.comprobar("I.CU18-401-sin-token", "sin token: 401", sin.estado == 401, str(sin.estado))

    # Extremo a extremo: el participante registra SU cuenta (antes: 422 «no coincide») y la de otra persona se rechaza.
    cuenta = {"tipoCuenta": "AHORRO", "entidadFinanciera": "BANCO SINTETICO", "numeroCuenta": "10" + ci + "99",
              "numeroCifrado": "enc:v1:int-sintetico", "titularNombre": nombre, "titularDocumento": ci, "moneda": "BOB"}
    propia = p1.post("/cuentas-bancarias", cuenta)
    INF.comprobar("I.CU18-registra", "el participante registra SU cuenta de destino (201, numero enmascarado)",
                  propia.estado == 201 and "numeroCuenta" not in propia.cuerpo and ci not in propia.texto, f"{propia.estado} {propia.texto[:140]}".replace(ci, "***"))
    ajena = p1.post("/cuentas-bancarias", {**cuenta, "titularNombre": "Otra Persona", "numeroCuenta": "10" + ci + "98"})
    INF.comprobar("I.CU18-rechaza-ajena", "una cuenta a nombre de OTRA persona sigue rechazada (422, R-SEG-02)", ajena.estado == 422, f"{ajena.estado} {ajena.texto[:100]}")


# ------------------------------------------------------------------------------------------------ aportes: caida de nucleo
@recorrido("I.APORTES-CAIDA")
def aportes_con_nucleo_caido() -> None:
    """98.8.4: con nucleo-financiero APAGADO el aporte se paga igual (aportes no lo necesita) y la plata se mueve UNA vez al volver."""
    e, g = preparar(3, abiertos=1, periodos=1, saldo="1500.00")
    ob = obligaciones(g, 1, e)
    pagador = e.participantes[0]
    cuenta = e.billeteras[pagador.codigo]
    nucleo = apagar("nucleo-financiero")
    try:
        p = pagar(pagador, ob[pagador.codigo], "1500.00")
        INF.comprobar("I.APORTES-CAIDA-paga", "con nucleo-financiero APAGADO el aporte se acredita en aportes (201, PAGADO)",
                      p.estado == 201 and p.cuerpo.get("estadoObligacion") == "PAGADO", f"{p.estado} {str(p)[:100]}")
        ev = esperar(lambda: uno("SELECT id FROM aportes.evento_dominio WHERE tipo = 'aportes.aporte_cobrado' AND agregado_id = %s AND estado = 'PUBLICADO'",
                                 ob[pagador.codigo]), 60, 2, "aporte_cobrado PUBLICADO en Kafka con nucleo caido")
        sin_efecto = numero("SELECT count(*) FROM nucleo_financiero.transaccion_billetera WHERE tipo = 'APORTE_A_GRUPO' AND origen_id = %s",
                            ob[pagador.codigo]) == 0 and saldo_del_libro(cuenta) == D("1500.00")
        INF.comprobar("I.APORTES-CAIDA-inconsistencia", "mientras nucleo esta caido el aporte figura PAGADO y la billetera NO se movio (inconsistencia visible, acotada al tiempo de la caida)",
                      sin_efecto, f"libro={saldo_del_libro(cuenta)}")
    finally:
        encender(nucleo)
    n = esperar(lambda: numero("SELECT count(*) FROM nucleo_financiero.transaccion_billetera WHERE tipo = 'APORTE_A_GRUPO' AND origen_id = %s",
                               ob[pagador.codigo]) == 1, 120, 3, "el aporte se aplica al volver nucleo")
    asi = psql("SELECT a.id, COALESCE(SUM(m.debe),0), COALESCE(SUM(m.haber),0) FROM nucleo_financiero.transaccion_billetera t "
               "JOIN nucleo_financiero.asiento_contable a ON a.id = t.asiento_contable_id JOIN nucleo_financiero.movimiento_contable m ON m.asiento_id = a.id "
               "WHERE t.tipo = 'APORTE_A_GRUPO' AND t.origen_id = %s GROUP BY a.id", ob[pagador.codigo])
    INF.comprobar("I.APORTES-CAIDA-recupera", "al volver nucleo el aporte se aplica UNA vez: billetera 1500 -> 0, grupo +1500, UN asiento cuadrado",
                  bool(n) and saldo_del_libro(cuenta) == D("0.00") and saldo_del_libro(g.billetera) == D("1500.00") and len(asi) == 1
                  and D(asi[0][1]) == D(asi[0][2]) == D("1500.00"), f"libro={saldo_del_libro(cuenta)} grupo={saldo_del_libro(g.billetera)} asientos={asi}")
    # Traza: el MISMO correlationId en el evento de aportes y en el de nucleo.
    c_aportes = uno("SELECT correlation_id FROM aportes.evento_dominio WHERE id = %s", ev)
    c_nucleo = esperar(lambda: uno("SELECT correlation_id FROM nucleo_financiero.evento_dominio WHERE tipo = 'nucleo_financiero.aporte_aplicado' "
                                   "AND agregado_id = %s", ob[pagador.codigo]), 60, 2, "aporte_aplicado en el outbox de nucleo")
    INF.comprobar("I.APORTES-CAIDA-traza", "el correlationId cruza aportes -> nucleo-financiero (aporte_cobrado y aporte_aplicado comparten traza)",
                  bool(c_aportes) and c_aportes == c_nucleo, f"aportes={c_aportes} nucleo={c_nucleo}")


# ------------------------------------------------------------------------------------------------ cesion: caida de grupos
@recorrido("I.CESION-ORDEN")
def cesion_con_grupos_caido() -> None:
    """98.8.4: con grupos APAGADO la venta se liquida igual en entregas; al volver, el cupo cambia de titular UNA vez."""
    cuenta_de_servicio_de_entregas()
    e, g = preparar(3, abiertos=1, periodos=2, saldo="0")
    fondear_por_libro(e.billeteras[e.comprador.codigo], D("6000.00"))
    vendedor = e.participantes[1]
    oferta = oferta_publicada(g, 2, vendedor, "5500.00", "50.00")
    grupos = apagar("grupos")
    try:
        # La compra necesita a grupos para la membresia del comprador: con grupos caido entregas deniega y NO retiene (falla cerrado).
        c = e.comprador.post(f"/entregas/mercado/ofertas/{oferta}/compra", clave_=clave())
        INF.comprobar("I.CESION-ORDEN-cerrada", "con grupos APAGADO la compra no avanza (entregas deniega por omision: no se puede saber si es miembro)",
                      c.estado != 200 or c.cuerpo.get("estado") != "LIQUIDADA", f"{c.estado} {str(c)[:120]}")
    finally:
        encender(grupos)
    c = e.comprador.post(f"/entregas/mercado/ofertas/{oferta}/compra", clave_=clave())
    INF.comprobar("I.CESION-ORDEN-liquida", "con grupos de vuelta la compra se liquida", c.estado == 200 and c.cuerpo.get("estado") == "LIQUIDADA", str(c)[:160])
    cesion = c.cuerpo.get("cesionId")
    comprador_participante = g.participantes[3]
    esperar(lambda: uno("SELECT participante_id FROM grupos.cupo WHERE id = %s", g.cupos[1]) == comprador_participante, 90, 2, "cupo del comprador")
    INF.comprobar("I.CESION-ORDEN-titular", "el cupo es del comprador y hay UN traspaso VENTA",
                  numero("SELECT count(*) FROM grupos.traspaso_cupo WHERE cupo_id = %s", g.cupos[1]) == 1, "un traspaso")
    ev = evento_id("entregas", "entregas.derecho_cedido", cesion)
    INF.comprobar("I.CESION-ORDEN-traza", "el correlationId del derecho_cedido llega a grupos (grupos.cupo_traspasado comparte traza)",
                  bool(ev) and uno("SELECT correlation_id FROM entregas.evento_dominio WHERE id = %s", ev)
                  == esperar(lambda: uno("SELECT correlation_id FROM grupos.evento_dominio WHERE tipo = 'grupos.cupo_traspasado' AND payload->>'cupoId' = %s", g.cupos[1]), 60, 2, "evento de grupos"),
                  "mismo correlationId")


RECORRIDOS_I: dict[str, Any] = {"i-cu18": cu18_mecanismo, "i-aportes-caida": aportes_con_nucleo_caido, "i-cesion-orden": cesion_con_grupos_caido}
