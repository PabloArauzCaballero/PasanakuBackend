"""
El "mundo" sintetico del ensayo: personas con rol, un grupo con sus cupos, periodos y turnos, billeteras con saldo.

Lo que se siembra por SQL es SOLO lo que ningun servicio crea hoy (y cada brecha queda declarada en el informe):
  - la billetera de cada persona (no hay consumidor de identidad.usuario_registrado en nucleo-financiero);
  - la estructura del grupo (grupos expone crearGrupo, pero exige un organizador habilitado en el servicio
    `organizador`, tarifario vigente y licencia: tres servicios que este conjunto no levanta);
  - el saldo inicial de las billeteras (la recarga exige cotizacion de `tarifas`, que este conjunto no levanta);
  - la capacidad de respaldo empresarial (dato de gobierno: tope SINTETICO, apto_produccion=false por diseno).
Todo lo demas se ejercita por HTTP con tokens reales.
"""
from __future__ import annotations

import hashlib
import secrets
import uuid
from dataclasses import dataclass, field
from decimal import Decimal

from lib import (E, ID_PUENTE, Cuenta, abrir_billetera, lit, psql, sembrar_usuarios, uno)


def telefono_nuevo() -> str:
    """+5917 + 7 digitos aleatorios: sintetico y distinto en cada corrida (uq_usuario_telefono_e164)."""
    return "+5917" + "".join(str(secrets.randbelow(10)) for _ in range(7))


def persona(prefijo: str, rol: str, nombre: str) -> Cuenta:
    sufijo = uuid.uuid4().hex[:6].upper()
    return Cuenta(f"{prefijo}{sufijo}", rol, nombre, telefono_nuevo())


@dataclass
class Elenco:
    """Las personas de un recorrido. Cada recorrido arma el suyo: no comparten nada y se pueden correr en cualquier orden."""

    admin: Cuenta
    tesoreria: Cuenta
    riesgos: Cuenta
    participantes: list[Cuenta]  # P1..Pn; P1 es el primer receptor
    comprador: Cuenta
    billeteras: dict[str, str] = field(default_factory=dict)  # codigo -> cuenta_billetera_id


def tipo_de_cambio_sintetico() -> None:
    """R-UIF-04: sin tipo de cambio del dia el libro rechaza TODA transaccion (no se puede expresar en dolares para el umbral UIF).
    El stack del ensayo no carga la serie de `sql/61_dev` (fuente MANUAL), asi que se siembra la misma serie SINTETICA: la tasa es
    una constante de prueba, no una cotizacion; en produccion la carga el BCB/tesoreria y nada de esto aplica."""
    psql("""INSERT INTO catalogo.tipo_cambio (moneda_origen, moneda_destino, fecha, tipo_cambio, fuente, cargado_en)
            SELECT 'BOB', 'USD', d::date, 0.143678, 'MANUAL', now()
              FROM generate_series(current_date - 60, current_date + 30, interval '1 day') AS d
             WHERE NOT EXISTS (SELECT 1 FROM catalogo.tipo_cambio WHERE moneda_origen = 'BOB' AND moneda_destino = 'USD' AND fecha = d::date)""")


def armar_elenco(participantes: int = 3) -> Elenco:
    tipo_de_cambio_sintetico()
    e = Elenco(
        admin=persona("IAD", "ADMIN_PLATAFORMA", "Admin"),
        tesoreria=persona("ITE", "TESORERIA", "Tesorero"),
        riesgos=persona("IRI", "RESPONSABLE_RIESGOS", "Riesgos"),
        participantes=[persona("IPA", "PARTICIPANTE", f"Participante{i + 1}") for i in range(participantes)],
        comprador=persona("ICO", "PARTICIPANTE", "Comprador"),
    )
    sembrar_usuarios([e.admin, e.tesoreria, e.riesgos, *e.participantes, e.comprador])
    return e


def cuenta_de_servicio_de_entregas() -> Cuenta:
    """La cuenta con que ENTREGAS entra a identidad (rol SERVICIO_ENTREGAS). Telefono y clave salen del entorno del ensayo
    (ENTREGAS_SERVICIO_*, generados fuera del repo): el servicio las recibe por variable y se renueva el token solo."""
    c = Cuenta("SVCENT01", "SERVICIO_ENTREGAS", "ServicioEntregas", E["ENTREGAS_SERVICIO_TELEFONO"])
    c.clave = E["ENTREGAS_SERVICIO_CLAVE"]
    sembrar_usuarios([c])
    return c


def fondear_por_libro(cuenta_id: str, monto: Decimal) -> str:
    """Saldo inicial: una transaccion CUADRADA contra el puente de custodia, como lo hacen las fixturas de las pruebas.

    Ojo (hallado en la primera corrida real): una RECARGA por encima de USD 1.000 (umbral UIF PCC-01 inc. i, acumulado en 3 dias)
    exige declaracion de origen de fondos y el libro la RECHAZA (ck_operelev_declaracion). Por eso el saldo inicial es < USD 1.000
    (~6.960 BOB a la tasa sintetica): en el producto real esa declaracion la pide el flujo de recarga, que este conjunto no levanta."""
    assert monto <= Decimal("6900.00"), "el saldo inicial sintetico no puede superar el umbral UIF de carga de billetera"
    transaccion = str(uuid.uuid4())
    psql(f"""BEGIN;
INSERT INTO nucleo_financiero.transaccion_billetera
  (id, tipo, estado, moneda, monto_total, origen_tipo, origen_id, canal, clave_idempotencia, hash_registro, ocurrida_en, registrada_en)
VALUES ({lit(transaccion)}, 'RECARGA', 'APLICADA', 'BOB', {lit(monto)}, 'ORDEN_RECARGA', gen_random_uuid(), 'API',
        {lit('semilla-' + transaccion)}, repeat('a', 64), now(), now());
INSERT INTO nucleo_financiero.movimiento_billetera (id, transaccion_id, cuenta_billetera_id, orden, sentido, monto, saldo_disponible_posterior, saldo_retenido_posterior, glosa, registrado_en)
VALUES (gen_random_uuid(), {lit(transaccion)}, {lit(ID_PUENTE)}, 1, 'DEBITO', {lit(monto)}, 0, 0, 'Ingreso desde custodia', now());
INSERT INTO nucleo_financiero.movimiento_billetera (id, transaccion_id, cuenta_billetera_id, orden, sentido, monto, saldo_disponible_posterior, saldo_retenido_posterior, glosa, registrado_en)
VALUES (gen_random_uuid(), {lit(transaccion)}, {lit(cuenta_id)}, 2, 'CREDITO', {lit(monto)}, {lit(monto)}, 0, 'Saldo inicial sintetico', now());
COMMIT;""")
    return transaccion


def con_billeteras(e: Elenco, saldo: str = "0") -> None:
    for c in [*e.participantes, e.comprador]:
        assert c.id, c.codigo
        e.billeteras[c.codigo] = abrir_billetera(c.id)
    if Decimal(saldo) > 0:
        for c in [*e.participantes, e.comprador]:
            fondear_por_libro(e.billeteras[c.codigo], Decimal(saldo))


@dataclass
class Grupo:
    id: str
    aporte: Decimal
    periodos: list[str]  # periodo_id por numero (indice 0 = periodo 1)
    turnos: list[str]  # turno_id por periodo
    participantes: list[str]  # participante_id, mismo orden que Elenco.participantes (+ comprador al final)
    cupos: list[str]  # cupo_id, mismo orden


def sembrar_grupo(e: Elenco, aporte: str = "1500.00", periodos: int = 3, abiertos: int = 1) -> Grupo:
    """Un grupo ACTIVO con un cupo por persona (incluido el comprador), `periodos` periodos y un turno por periodo.

    El turno k es del cupo k (P1 cobra primero). Solo los primeros `abiertos` periodos quedan ABIERTOS."""
    personas = [*e.participantes, e.comprador]
    n = len(personas)
    g = str(uuid.uuid4())
    sql_ = [f"""
INSERT INTO grupos.grupo (id, codigo_publico, nombre, monto_aporte, moneda, periodicidad, dia_cobro, num_periodos, cupos_totales, cupos_ocupados,
   fecha_inicio, fecha_fin_estimada, estado, tipo_conformacion, modalidad_turnos, visibilidad, es_autogestionado, requiere_kyc_minimo,
   reputacion_minima, dias_gracia, aplica_recargo_mora, usa_fondo_garantia, porcentaje_fondo_garantia, quorum_decisiones)
VALUES ({lit(g)}, {lit('GI' + g[:8].upper())}, 'Grupo de integracion', {lit(aporte)}, 'BOB', 'MENSUAL', 5, {max(periodos, 3)}, {n}, {n},
   current_date, current_date + 150, 'ACTIVO', 'MANUAL_POR_INVITACION', 'SORTEO_ALEATORIO', 'PRIVADO', true, 'BASICO', 0, 3, true, false, 0.00, 0.600);"""]
    parts, cupos = [], []
    for i, c in enumerate(personas, 1):
        p, cu = str(uuid.uuid4()), str(uuid.uuid4())
        parts.append(p)
        cupos.append(cu)
        sql_.append(f"""
INSERT INTO grupos.participante (id, grupo_id, usuario_id, estado, es_organizador, fecha_ingreso, reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
VALUES ({lit(p)}, {lit(g)}, {lit(c.id)}, 'ACTIVO', false, now(), 50, 0, 0);
INSERT INTO grupos.cupo (id, grupo_id, numero, participante_id, estado, fraccion, asignado_en)
VALUES ({lit(cu)}, {lit(g)}, {i}, {lit(p)}, 'OCUPADO', 1.0, now());""")
    per_ids, tur_ids = [], []
    monto_pozo = Decimal(aporte) * n
    for k in range(1, periodos + 1):
        per, tur = str(uuid.uuid4()), str(uuid.uuid4())
        per_ids.append(per)
        tur_ids.append(tur)
        estado = "ABIERTO" if k <= abiertos else "PROGRAMADO"
        # El primer periodo vence hoy: la fecha de liquidacion de aportes corre al siguiente dia habil (CU-59).
        sql_.append(f"""
INSERT INTO grupos.periodo (id, grupo_id, numero, fecha_inicio, fecha_limite_pago, fecha_fin_gracia, fecha_entrega_prevista, estado, monto_objetivo, monto_recaudado, cupos_morosos)
VALUES ({lit(per)}, {lit(g)}, {k}, current_date - 30 + {(k - 1) * 30}, current_date + {(k - 1) * 30}, current_date + 3 + {(k - 1) * 30},
        current_date + 5 + {(k - 1) * 30}, {lit(estado)}, {lit(monto_pozo)}, 0, 0);
INSERT INTO grupos.turno (id, grupo_id, periodo_id, cupo_id, orden_asignado, estado, criterio_asignacion, monto_estimado_cobro)
VALUES ({lit(tur)}, {lit(g)}, {lit(per)}, {lit(cupos[k - 1])}, {k}, 'PROGRAMADO', 'SORTEO', {lit(monto_pozo)});""")
    psql("BEGIN;\n" + "\n".join(sql_) + "\nCOMMIT;")
    return Grupo(g, Decimal(aporte), per_ids, tur_ids, parts, cupos)


def abrir_periodo(grupo: Grupo, k: int) -> None:
    """Pasa el periodo k (1-based) a ABIERTO: lo que haria el reloj de grupos al llegar su fecha (no hay trabajo que lo haga hoy)."""
    psql("UPDATE grupos.periodo SET estado = 'ABIERTO' WHERE id = %s", grupo.periodos[k - 1])


def capacidad_de_respaldo(responsable_usuario_id: str, tope: str = "100000.00") -> None:
    """Tope SINTETICO de la capacidad empresarial GENERAL/BOB. Es un dato de gobierno; sin el se deniega por omision."""
    existe = uno("SELECT id FROM garantia.capacidad_respaldo WHERE ambito = 'GENERAL' AND moneda = 'BOB'")
    if existe:
        psql("UPDATE garantia.capacidad_respaldo SET monto_tope = GREATEST(monto_tope, monto_comprometido + %s) WHERE id = %s", tope, existe)
        return
    psql("""INSERT INTO garantia.capacidad_respaldo (id, ambito, moneda, monto_tope, monto_comprometido, responsable_id, vigente_desde, version)
            VALUES (gen_random_uuid(), 'GENERAL', 'BOB', %s, 0, %s, now(), 0)""", tope, responsable_usuario_id)


def oferta_publicada(g: Grupo, k: int, vendedor: Cuenta, precio: str, cargos: str) -> str:
    """Una oferta PUBLICADA sobre el turno k, como la dejaria `publicarOferta`.

    Publicar por HTTP exige que `grupos` publique la consulta del turno (titular, estado, monto del derecho, cierre), que NO
    existe todavia: el adaptador de entregas contesta «no se pudo saber» y deniega (brecha declarada)."""
    oferta = str(uuid.uuid4())
    derecho = g.aporte * len(g.participantes)
    psql(f"""INSERT INTO entregas.oferta_turno
      (id, grupo_id, turno_id, cupo_id, participante_origen_id, vendedor_usuario_id, moneda, monto_derecho, monto_precio, monto_cargos,
       estado, vigente_hasta, publicada_en, clave_idempotencia, version)
      VALUES ({lit(oferta)}, {lit(g.id)}, {lit(g.turnos[k - 1])}, {lit(g.cupos[k - 1])}, {lit(g.participantes[k - 1])}, {lit(vendedor.id)}, 'BOB',
              {lit(derecho)}, {lit(precio)}, {lit(cargos)}, 'PUBLICADA', now() + interval '2 days', now(), {lit('int-' + oferta)}, 0)""")
    return oferta


def grupo_de_admision(organizador: Cuenta, relleno: Cuenta, aporte: str = "500.00") -> str:
    """Un grupo CONFORMADO con un cupo libre (el del recorrido de admision: invitar, canjear, proponer, resolver).

    Igual que la siembra de la aceptacion REAL: crearGrupo exige organizador habilitado, tarifario y licencia."""
    g, p_org, p_rel = str(uuid.uuid4()), str(uuid.uuid4()), str(uuid.uuid4())
    psql(f"""BEGIN;
INSERT INTO grupos.grupo (id, codigo_publico, nombre, monto_aporte, moneda, periodicidad, dia_cobro, num_periodos, cupos_totales, cupos_ocupados,
   fecha_inicio, fecha_fin_estimada, estado, tipo_conformacion, modalidad_turnos, visibilidad, es_autogestionado, requiere_kyc_minimo,
   reputacion_minima, dias_gracia, aplica_recargo_mora, usa_fondo_garantia, porcentaje_fondo_garantia, quorum_decisiones)
VALUES ({lit(g)}, {lit('GA' + g[:8].upper())}, 'Grupo de admision', {lit(aporte)}, 'BOB', 'MENSUAL', 5, 3, 3, 2,
   current_date + 7, current_date + 100, 'ABIERTO_A_INSCRIPCION', 'MANUAL_POR_INVITACION', 'SORTEO_ALEATORIO', 'PRIVADO', true, 'BASICO', 0, 3, true, false, 0.00, 0.600);
INSERT INTO grupos.participante (id, grupo_id, usuario_id, estado, es_organizador, fecha_ingreso, reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
VALUES ({lit(p_org)}, {lit(g)}, {lit(organizador.id)}, 'ACTIVO', true, now(), 50, 0, 0),
       ({lit(p_rel)}, {lit(g)}, {lit(relleno.id)}, 'ACTIVO', false, now(), 50, 0, 0);
INSERT INTO grupos.cupo (id, grupo_id, numero, participante_id, estado, fraccion, asignado_en) VALUES
  (gen_random_uuid(), {lit(g)}, 1, {lit(p_org)}, 'OCUPADO', 1.0, now()),
  (gen_random_uuid(), {lit(g)}, 2, {lit(p_rel)}, 'OCUPADO', 1.0, now()),
  (gen_random_uuid(), {lit(g)}, 3, NULL, 'LIBRE', 1.0, NULL);
INSERT INTO grupos.configuracion_grupo (grupo_id, permite_cupos_multiples, max_cupos_por_persona, hora_limite_pago, tolerancia_monto_parcial)
VALUES ({lit(g)}, false, 1, '18:00', 0);
INSERT INTO grupos.reglamento_grupo (grupo_id, version, contenido, hash_contenido, clausulas_mora, clausulas_abandono, vigente_desde, redactado_por)
VALUES ({lit(g)}, 1, 'Reglamento de integracion', encode(digest('Reglamento de integracion', 'sha256'), 'hex'), 'Mora', 'Abandono', now(), {lit(organizador.id)});
COMMIT;""")
    return g


def documento_del_titular(usuario_id: str, numero: str) -> None:
    """El KYC aprobado de una persona SINTETICA (numero inventado): identidad lo guarda como hash con pimienta y entregas
    lo cotejara al registrar una cuenta de destino (R-SEG-02). Ningun servicio lo crea por HTTP en este conjunto."""
    hash_ = hashlib.sha256((numero + E["SEGURIDAD_PIMIENTA"]).encode()).hexdigest()
    psql(f"""INSERT INTO identidad.documento_identidad (usuario_id, tipo, numero_cifrado, version_llave, hash_numero, pais_emision, estado)
             SELECT {lit(usuario_id)}, 'CI', 'enc:v1:int-sintetico', 1, {lit(hash_)}, 'BO', 'APROBADA'
              WHERE NOT EXISTS (SELECT 1 FROM identidad.documento_identidad WHERE usuario_id = {lit(usuario_id)})""")


def terminar_enfriamiento(cuenta_id: str) -> None:
    """Hace pasar la ventana de enfriamiento de una cuenta de destino (no hay reloj inyectable en el stack real)."""
    psql("UPDATE entregas.cuenta_bancaria_beneficiario SET bloqueada_hasta = now() - interval '1 minute' WHERE id = %s", cuenta_id)


def cuenta_destino_sembrada(usuario_id: str, nombre: str, documento: str) -> str:
    """Una cuenta de destino PENDIENTE a nombre del beneficiario, sembrada por SQL porque registrarla por HTTP es hoy IMPOSIBLE para
    un participante (entregas -> identidad `verificarTitularidad` exige DATOS_SENSIBLES_LEER y entregas propaga el token del
    participante: 403 -> «no coincide»). La verificacion, la ventana de enfriamiento y el desembolso si se ejercitan por HTTP."""
    cuenta = str(uuid.uuid4())
    numero_cuenta = "10" + documento + "99"
    psql(f"""INSERT INTO entregas.cuenta_bancaria_beneficiario
              (id, usuario_id, tipo_cuenta, entidad_financiera, numero_cuenta_cifrado, version_llave, hash_numero_cuenta, numero_enmascarado,
               titular_nombre, titular_documento, moneda, es_principal, estado_verificacion)
             VALUES ({lit(cuenta)}, {lit(usuario_id)}, 'AHORRO', 'BANCO SINTETICO', 'enc:v1:int-sintetico',
                     1, {lit(hashlib.sha256(numero_cuenta.encode()).hexdigest())}, {lit('****' + numero_cuenta[-4:])},
                     {lit(nombre)}, {lit(documento)}, 'BOB', true, 'PENDIENTE')""")
    return cuenta
