#!/usr/bin/env python3
"""
Inyección de Metadata de Esquemas, Tablas y Columnas en AportaYa.
Genera comentarios SQL idempotentes (COMMENT ON SCHEMA / TABLE / COLUMN)
y enriquece el catálogo de datos para que el servidor lo sirva con
razón de negocio y razón de sistema completas.
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import modelo  # noqa: E402
import catalogo_de_datos  # noqa: E402

RAIZ = pathlib.Path(__file__).resolve().parent.parent

ESQUEMAS_METADATA = {
    "aportes": {
        "nombre": "Aportes y Recaudación",
        "modulo": "02",
        "negocio": "Gestión integral de recaudación de aportes de cada ciclo, seguimiento de obligaciones de pago, control de mora y cálculo de recargos del pasanaku.",
        "sistema": "Esquema del microservicio aportes, operado exclusivamente por el rol de base svc_aportes. Contiene tablas de obligaciones, pagos y pasarelas de pago.",
    },
    "auditoria": {
        "nombre": "Auditoría, Reportes y Cumplimiento",
        "modulo": "09",
        "negocio": "Registro inmutable de trazabilidad, cumplimiento regulatorio, catálogo de metadatos, gobierno de datos, reportes periódicos a supervisores y bitácora de eventos del sistema.",
        "sistema": "Esquema del microservicio auditoria, operado por svc_auditoria. Almacena bitácoras de eventos, políticas de retención, fichas de gobierno de datos y revisiones.",
    },
    "cumplimiento": {
        "nombre": "Cumplimiento Regulatorio, UIF y ASFI",
        "modulo": "12",
        "negocio": "Prevención de legitimación de ganancias ilícitas y financiamiento del terrorismo (PLA/FT), debida diligencia, listas de sanción, límites transaccionales y reportes UIF.",
        "sistema": "Esquema del microservicio cumplimiento, operado por svc_cumplimiento. Aplica RLS estricto para expedientes confidenciales y auditoría de decisiones de comités.",
    },
    "entregas": {
        "nombre": "Entregas, Turnos y Desembolsos",
        "modulo": "04",
        "negocio": "Adjudicación y desembolso del fondo acumulado a los beneficiarios de cada turno según el calendario o sorteo del grupo pasanaku.",
        "sistema": "Esquema del microservicio entregas, operado por svc_entregas. Coordina con nucleo_financiero mediante sagas orquestadas para transferencias seguras.",
    },
    "erp": {
        "nombre": "Contabilidad Institucional y ERP",
        "modulo": "13",
        "negocio": "Gestión administrativa interna, plan de cuentas institucional, facturación electrónica, presupuesto operativo, compras y activos de la empresa.",
        "sistema": "Esquema del microservicio erp, operado por svc_erp. Separa la contabilidad corporativa de los libros de custodia de fondos de los participantes.",
    },
    "garantia": {
        "nombre": "Fondo de Garantía, Mora y Cobranza",
        "modulo": "08",
        "negocio": "Fondo de protección colectiva, gestión de garantías prendarias y líquidas, subrogación de deuda ante incumplimientos y procesos de cobranza judicial y extrajudicial.",
        "sistema": "Esquema del microservicio garantia, operado por svc_garantia. Asegura la solvencia del grupo mediante coberturas automáticas registradas en libro mayor.",
    },
    "grupos": {
        "nombre": "Grupos, Rondas y Turnos",
        "modulo": "03",
        "negocio": "Administración del ciclo de vida de los grupos pasanaku: creación, miembros, cupos, turnos, reglas de convivencia, votaciones y disolución.",
        "sistema": "Esquema del microservicio grupos, operado por svc_grupos. Centraliza el estado de los círculos de ahorro y valida cupos y turnos.",
    },
    "identidad": {
        "nombre": "Identidad, Usuarios y Autenticación",
        "modulo": "01",
        "negocio": "Registro de usuarios, verificación de identidad documental y biométrica (KYC), autenticación multifactor (MFA, PIN, contraseñas) y gestión de roles y permisos.",
        "sistema": "Esquema del microservicio identidad, operado por svc_identidad. Almacena credenciales hasheadas con Argon2id, tokens revocables y sesiones con contexto RLS.",
    },
    "inversiones": {
        "nombre": "Inversiones y Rendimientos",
        "modulo": "15",
        "negocio": "Rentabilización de excedentes de fondos de reserva en instrumentos autorizados (DPF, bonos soberanos), cálculo de rendimientos y reinversión.",
        "sistema": "Esquema del microservicio inversiones, operado por svc_inversiones. Restricción de doble firma y segregación de fondos propios vs fondos en custodia.",
    },
    "notificaciones": {
        "nombre": "Notificaciones y Mensajería",
        "modulo": "05",
        "negocio": "Envío omnicanal (WhatsApp, push, SMS, correo) de avisos de vencimiento, confirmación de pagos, alertas de seguridad y recordatorios de turnos.",
        "sistema": "Esquema del microservicio notificaciones, operado por svc_notificaciones. Emplea cola de reintentos con backoff exponencial y plantillas versionadas.",
    },
    "nucleo_financiero": {
        "nombre": "Núcleo Financiero, Billeteras y Custodia",
        "modulo": "10",
        "negocio": "El corazón transaccional de AportaYa: billeteras electrónicas, libro mayor de partida doble inmutable, custodia de dinero de los participantes y movimientos contables.",
        "sistema": "Esquema del microservicio nucleo_financiero, operado por svc_nucleo_financiero. Tablas append-only sin UPDATE/DELETE permitidos. Garantía de balance cero.",
    },
    "organizador": {
        "nombre": "Organizadores y Automatización",
        "modulo": "07",
        "negocio": "Habilitación, capacitación, evaluación de reputación, liquidación de comisiones y control de conducta de los organizadores comunitarios de pasanaku.",
        "sistema": "Esquema del microservicio organizador, operado por svc_organizador. Registra contratos de mandato, certificaciones y límites de grupos simultáneos.",
    },
    "publicidad": {
        "nombre": "Publicidad, Campañas y Fidelización",
        "modulo": "14",
        "negocio": "Campañas de difusión, incentivos por referidos, programas de fidelidad y métricas de adquisición de nuevos participantes.",
        "sistema": "Esquema del microservicio publicidad, operado por svc_publicidad. Aislado de datos financieros y personales sensibles; opera con agregados anónimos.",
    },
    "tarifas": {
        "nombre": "Tarifas, Comisiones e Impuestos",
        "modulo": "11",
        "negocio": "Tarifario oficial de comisiones por servicio, costos de pasarela, impuestos aplicables e historial de modificaciones comerciales.",
        "sistema": "Esquema del microservicio tarifas, operado por svc_tarifas. Los cálculos de comisiones se congelan en la cotización; tablas versionadas temporalmente.",
    },
    "transparencia": {
        "nombre": "Transparencia, Sorteos y Reputación",
        "modulo": "06",
        "negocio": "Mecanismo verificable de confianza colectiva: actas de sorteo criptográfico (commit-reveal), hashes encadenados inmutables y verificación pública de probidad.",
        "sistema": "Esquema del microservicio transparencia, operado por svc_transparencia. Genera árboles de Merkle y pruebas criptográficas SHA-256 reproducibles por terceros.",
    },
    "catalogo": {
        "nombre": "Catálogos y Datos Maestros Compartidos",
        "modulo": "99",
        "negocio": "Datos maestros y tablas de referencia estables: monedas, países, tipos de documento, feriados bancarios y motivos operativos.",
        "sistema": "Esquema compartido de solo lectura para todos los servicios; solo el servicio asignado o los scripts de semillas pueden escribir en él.",
    },
    "comun": {
        "nombre": "Infraestructura Común y Trazabilidad",
        "modulo": "00",
        "negocio": "Infraestructura transversal compartida de observabilidad, auditoría técnica de sesiones y rastreo entre servicios.",
        "sistema": "Esquema compartido; todo servicio puede insertar su rastro de eventos/auditoría pero ningún servicio puede leer ni modificar el de otro salvo svc_auditoria.",
    },
}

MAPA_COLUMNAS_EXACTAS = {
    "id": "Identificador estable y único de la fila (UUID). Es lo único que se loguea o viaja entre servicios.",
    "moneda": "Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).",
    "clave_idempotencia": "Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).",
    "vigente_desde": "Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.",
    "vigente_hasta": "Hasta cuándo rigió esta versión; vacía mientras sigue vigente.",
    "estado": "Estado operativo o fase del ciclo de vida en que se encuentra el registro.",
    "tipo": "Subtipo o modalidad específica que determina las reglas operativas aplicables al registro.",
    "codigo": "Código mnemotécnico o identificador legible para operadores y personas usuarias.",
    "codigo_publico": "Identificador público visible en interfaz y comunicaciones que no expone la clave primaria UUID.",
    "descripcion": "Detalle descriptivo que explica el contenido, propósito o contexto de la entidad.",
    "nombre": "Nombre descriptivo de la entidad para visualización y referencia en el sistema.",
    "motivo": "Justificación declarada o causal que originó la creación, rechazo o transición del registro.",
    "observacion": "Anotación adicional o detalle relevante registrado por el operador o sistema.",
    "monto": "Importe monetario principal de la transacción o concepto financiero.",
    "saldo": "Monto neto disponible o remanente tras procesar las operaciones de la entidad.",
    "version": "Número de versión para control de concurrencia optimista y auditoría de modificaciones.",
    "activo": "Indicador booleano que determina si el registro se encuentra actualmente operativo.",
    "activa": "Indicador booleano que determina si la entidad se encuentra actualmente operativa.",
    "creado_en": "Marca temporal UTC en que el registro fue creado en la base de datos.",
    "creada_en": "Marca temporal UTC en que la entidad fue creada en la base de datos.",
    "actualizado_en": "Marca temporal UTC de la última modificación aplicada sobre este registro.",
    "actualizada_en": "Marca temporal UTC de la última modificación aplicada sobre esta entidad.",
    "creado_por": "Identificador del usuario u operador que dio de alta el registro.",
    "aprobado_por": "Identificador del usuario con facultades que autorizó la operación.",
    "revisado_por": "Identificador del usuario u operador que realizó la revisión técnica o de gobierno.",
    "usuario_id": "Referencia al usuario titular de la cuenta o actor principal de la operación.",
    "grupo_id": "Referencia al grupo de pasanaku en el que se enmarca esta operación.",
    "participante_id": "Referencia al miembro participante en el círculo de ahorro pasanaku.",
    "cuenta_billetera_id": "Referencia a la billetera donde se debita o acredita el movimiento de fondos.",
    "asiento_contable_id": "Referencia al asiento de partida doble que respalda el movimiento financiero.",
    "ip_origen": "Dirección IP desde la cual se ejecutó la solicitud, registrada para auditoría de seguridad.",
    "dispositivo_id": "Identificador del dispositivo enrolado desde el que se autenticó la operación.",
    "fecha": "Fecha calendario correspondiente al devengo o ejecución de la operación.",
    "resultado": "Desenlace, dictamen o veredicto de la evaluación o proceso ejecutado.",
    "severidad": "Nivel de gravedad o criticidad asignado al incidente, alerta o evento.",
}


def explicar_columna_negocio(tabla: str, col_nombre: str, col_tipo: str, fk_target: str | None, ficha_col: str | None) -> str:
    if ficha_col:
        return ficha_col
    n = col_nombre
    t = tabla
    if n in MAPA_COLUMNAS_EXACTAS:
        return MAPA_COLUMNAS_EXACTAS[n]
    if fk_target:
        return f"Referencia por clave foránea a la entidad `{fk_target}`."
    if n.startswith("monto_") or n.startswith("total_") or n.endswith("_monto") or n.endswith("_importe"):
        concepto = n.replace("monto_", "").replace("total_", "").replace("_monto", "").replace("_importe", "").replace("_", " ")
        return f"Importe monetario correspondiente a {concepto} en `{t}`."
    if n.startswith("fecha_") or n.endswith("_en") or n.endswith("_fecha"):
        concepto = n.replace("fecha_", "").replace("_en", "").replace("_fecha", "").replace("_", " ")
        return f"Marca temporal o fecha de {concepto} en `{t}`."
    if n.startswith("es_") or n.startswith("tiene_") or n.startswith("esta_"):
        concepto = n.replace("es_", "").replace("tiene_", "").replace("esta_", "").replace("_", " ")
        return f"Indicador booleano que señala si {concepto} en `{t}`."
    if n.endswith("_id"):
        ent = n[:-3].replace("_", " ")
        return f"Identificador de referencia para el concepto de {ent} en `{t}`."
    if "hash" in n:
        concepto = n.replace("hash_", "").replace("_hash", "").replace("_", " ")
        return f"Resumen criptográfico SHA-256 para verificación inmutable de {concepto}."
    if n.startswith("tasa_") or n.startswith("porcentaje_") or n.endswith("_porcentaje"):
        concepto = n.replace("tasa_", "").replace("porcentaje_", "").replace("_porcentaje", "").replace("_", " ")
        return f"Tasa porcentual o alícuota aplicable a {concepto}."
    if n.startswith("numero_") or n.startswith("nro_") or n.endswith("_numero"):
        concepto = n.replace("numero_", "").replace("nro_", "").replace("_numero", "").replace("_", " ")
        return f"Numeración secuencial o identificador numérico de {concepto}."
    if n.startswith("codigo_") or n.endswith("_codigo"):
        concepto = n.replace("codigo_", "").replace("_codigo", "").replace("_", " ")
        return f"Código normalizado o mnemotécnico de {concepto}."
    if n.startswith("url_") or n.endswith("_url") or n.endswith("_uri"):
        concepto = n.replace("url_", "").replace("_url", "").replace("_uri", "").replace("_", " ")
        return f"Localizador uniforme de recursos (URL/URI) correspondiente a {concepto}."

    limpio = n.replace("_", " ")
    return f"Atributo que representa {limpio} en el modelo de `{t}`."


def explicar_columna_sistema(c: dict, fk_target: str | None, sensibilidad: str) -> str:
    partes = [c["tipo"].lower()]
    partes.append("opcional" if c["nulo"] else "obligatoria (NOT NULL)")
    if c["pk"]:
        partes.append("clave primaria (PK)")
    if fk_target:
        partes.append(f"clave foránea (FK) hacia `{fk_target}`")
    if c.get("uq"):
        partes.append("restricción UNIQUE")
    if c.get("idx"):
        partes.append("con índice B-tree")
    if c.get("generated"):
        partes.append("calculada por la base de datos")
    if sensibilidad != "NINGUNA":
        partes.append(f"sensibilidad {sensibilidad}")
    return ", ".join(partes) + "."


def esc(s: str) -> str:
    return s.replace("'", "''")


def generar():
    mods, registro, _ = modelo.cargar()
    narrativas = catalogo_de_datos.fichas()
    reglas = catalogo_de_datos.reglas_por_tabla()

    sql_lineas = [
        "-- ===========================================================================",
        "-- Inyección de Metadata en la Base de Datos (Esquemas, Tablas y Columnas)",
        "-- Generado automáticamente por scripts/inyectar_metadata.py",
        "-- Idempotente: COMMENT ON en PostgreSQL actualiza los metadatos sin fallar",
        "-- ===========================================================================",
        "",
        "SET search_path TO aportes, auditoria, cumplimiento, entregas, erp, garantia, grupos, identidad, inversiones, notificaciones, nucleo_financiero, organizador, publicidad, tarifas, transparencia, catalogo, comun, public;",
        "",
    ]

    # 1. Comentarios de esquemas
    sql_lineas.append("-- ---------------------------------------------------------------------------")
    sql_lineas.append("-- 1. METADATA DE ESQUEMAS")
    sql_lineas.append("-- ---------------------------------------------------------------------------")
    for esq, meta in sorted(ESQUEMAS_METADATA.items()):
        texto = f"[NEGOCIO] {meta['negocio']} [SISTEMA] {meta['sistema']}"
        sql_lineas.append(f"COMMENT ON SCHEMA {esq} IS '{esc(texto)}';")
    sql_lineas.append("")

    tablas_dict = {}
    total_columnas = 0

    for k, d in sorted(mods.items()):
        sql_lineas.append(f"-- ===========================================================================")
        sql_lineas.append(f"-- MÓDULO {k} — {d['nombre'].upper()}")
        sql_lineas.append(f"-- ===========================================================================")
        for alias in d["orden"]:
            e = d["entidades"][alias]
            tabla = e["tabla"]
            esquema = modelo.esquema_de(tabla, k)
            f_tabla = narrativas.get(tabla, {})
            f_cols = f_tabla.get("columnas", {})

            # Explicación de tabla
            que_es = f_tabla.get("queEs", f"Entidad del módulo {k}")
            negocio_t = f_tabla.get("negocio") or f_tabla.get("porQue") or f"Entidad de negocio {alias}"
            tabla_negocio = f"[NEGOCIO] {que_es}. {negocio_t}".strip().replace("'", "''")
            
            # Sistema de tabla
            sistema_partes = [f"Esquema `{esquema}`"]
            if tabla in modelo.APPEND_ONLY:
                sistema_partes.append("tabla inmutable append-only (UPDATE/DELETE prohibidos)")
            if esquema not in (modelo.ESQUEMA_COMUN, modelo.ESQUEMA_CATALOGO):
                sistema_partes.append(f"servicio dueño `{modelo.servicio_de(esquema)}`")
            tabla_sistema = f"[SISTEMA] {', '.join(sistema_partes)}.".replace("'", "''")

            sql_lineas.append(f"COMMENT ON TABLE {esquema}.{tabla} IS '{tabla_negocio} {tabla_sistema}';")

            cols_procesadas = []
            for c in e["cols"]:
                n = c["nombre"]
                fk = modelo.resolver_fk(c, k, registro) if c["fk"] else None
                nota = f_cols.get(modelo.a_camel(n)) or f_cols.get(n) or catalogo_de_datos.CONVENCIONES.get(n)
                exp_negocio = explicar_columna_negocio(tabla, n, c["tipo"], fk, nota)
                sens = catalogo_de_datos.sensibilidad(n)
                exp_sistema = explicar_columna_sistema(c, fk, sens)

                col_comentario = f"[NEGOCIO] {exp_negocio} [SISTEMA] {exp_sistema}".replace("'", "''")
                sql_lineas.append(f"COMMENT ON COLUMN {esquema}.{tabla}.{n} IS '{col_comentario}';")
                total_columnas += 1

                cols_procesadas.append({
                    "nombre": n,
                    "tipo": c["tipo"],
                    "nulable": c["nulo"],
                    "pk": c["pk"],
                    "referencia": fk,
                    "negocio": exp_negocio,
                    "origenNegocio": "ficha" if nota else "sistema",
                    "sistema": exp_sistema,
                    "sensibilidad": sens,
                })

            tablas_dict[tabla] = {
                "tabla": tabla,
                "esquema": esquema,
                "modulo": k,
                "nombreModulo": d["nombre"],
                "servicio": None if esquema in (modelo.ESQUEMA_COMUN, modelo.ESQUEMA_CATALOGO) else modelo.servicio_de(esquema),
                "appendOnly": tabla in modelo.APPEND_ONLY,
                "negocio": {
                    "queEs": que_es,
                    "negocio": f_tabla.get("negocio"),
                    "porQue": f_tabla.get("porQue"),
                },
                "notaDeSistema": f_tabla.get("sistema"),
                "reglas": [r["nombre"] for r in reglas.get(tabla, [])],
                "columnas": cols_procesadas,
            }
        sql_lineas.append("")

    # Escribir SQL de semillas
    archivo_sql = RAIZ / "sql" / "60_semillas" / "24-metadata-comentarios.sql"
    archivo_sql.write_text("\n".join(sql_lineas), encoding="utf-8")
    print(f"Generado: {archivo_sql} ({len(sql_lineas)} líneas, {len(tablas_dict)} tablas, {total_columnas} columnas)")

    # Actualizar sembrar.sql si no tiene 24-metadata-comentarios.sql
    sembrar_sql = RAIZ / "sql" / "60_semillas" / "sembrar.sql"
    contenido_sembrar = sembrar_sql.read_text(encoding="utf-8")
    if "24-metadata-comentarios.sql" not in contenido_sembrar:
        contenido_sembrar = contenido_sembrar.replace(
            "\\ir 23-indicadores-tableros.sql",
            "\\ir 23-indicadores-tableros.sql\n\\ir 24-metadata-comentarios.sql"
        )
        sembrar_sql.write_text(contenido_sembrar, encoding="utf-8")
        print("Actualizado: sql/60_semillas/sembrar.sql con \\ir 24-metadata-comentarios.sql")

    # Guardar archivo de enriquecimiento para catalogo_de_datos.py
    archivo_enriquecido = RAIZ / "scripts" / "metadata_enriquecida.json"
    archivo_enriquecido.write_text(
        json.dumps({
            "esquemas": ESQUEMAS_METADATA,
            "columnas_explicadas": total_columnas,
        }, ensure_ascii=False, indent=2),
        encoding="utf-8"
    )
    print(f"Guardado mapa de esquemas: {archivo_enriquecido}")


if __name__ == "__main__":
    generar()
