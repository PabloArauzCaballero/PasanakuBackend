---
titulo: "Matriz de evaluación de propuestas de proveedores externos"
estado: borrador para aprobación de Pablo — pesos DECISION_REQUIRED
fecha: 2026-11-21
origen: "PX0.M10 y sección «Proceso común de contratación» de PLAN-PROVEEDORES.md"
tags: [proveedores, rfp, evaluacion]
---

# Matriz de evaluación de propuestas

Se usa junto con la [plantilla de RFP](./00-plantilla-rfp.md). Es la misma matriz para todos los
proveedores; cada RFP puede agregar criterios propios **solo como eliminatorios**, nunca cambiar los pesos.

> **Pesos: `DECISION_REQUIRED` de Pablo.** Esta matriz no propone números. Hasta que Pablo los fije, la
> columna «Peso» queda vacía y ninguna propuesta se puede puntuar. Se pide fijarlos **antes** de abrir las
> propuestas, para que no se ajusten al resultado.

## 1. Paso 1 — Requisitos eliminatorios (cumple / no cumple)

Una propuesta que no cumple uno de estos queda fuera, sin puntuar.

| ID | Eliminatorio | Aplica a | Origen |
|---|---|---|---|
| E-01 | Autorización o supervisión vigente para prestar el servicio en Bolivia, según lo que determine Cumplimiento | Todos los que mueven dinero o datos regulados | Criterio «Regulación» |
| E-02 | Confirmaciones por **webhook firmado** con marca de tiempo e id de evento | PX-1, PX-2, PX-7 (estado de entrega), PX-8 | RM-04 |
| E-03 | **Idempotencia con nuestra clave** | PX-1, PX-2, PX-7, PX-8 | RM-03 |
| E-04 | **Consulta de estado** por nuestra clave | PX-1, PX-2 | RM-06, RM-07 |
| E-05 | Entorno de pruebas donde se pueden provocar los casos obligatorios | Todos | RM-17 |
| E-06 | Acepta las cláusulas de confidencialidad, auditoría y continuidad | Todos | RM-11 |
| E-07 | Declara el país de procesamiento de cada dato y sus suboperadores | Todos los que acceden a datos personales | Criterio «Datos» |
| E-08 | No usa nuestros datos para fines propios | Todos | DAT-04 |
| E-09 | Acepta que operemos con un segundo proveedor del mismo servicio | PX-1, PX-2 (segundo proveedor obligatorio, decisión de Pablo, tanda 8) | RM-19 |
| E-10 | No devuelve ni retiene imágenes o biometría más allá de lo declarado, o lo declara con plazo y país | PX-4 | RF-PX4-06 |
| E-11 | No es un canal de WhatsApp ni lo exige como requisito | PX-7 (decisión de Pablo, tanda 8) | — |

## 2. Paso 2 — Puntuación por criterio

Los siete criterios comunes salen del plan de proveedores («Criterios de evaluación comunes»).

| # | Criterio | Qué se pregunta | Evidencia que se pide | Peso |
|---|---|---|---|---|
| C1 | Regulación | ¿Está autorizado o supervisado por la autoridad que corresponde para ese servicio en Bolivia? (Cumplimiento lo determina) | Número de autorización, autoridad emisora, alcance (CON-07) | `DECISION_REQUIRED` |
| C2 | Capacidades técnicas | Webhooks firmados, consulta de estado, idempotencia por clave nuestra, sandbox, contrato de API publicado y versionado | Documentación de la API, ejemplos de webhook, sandbox funcionando (RM-01 a RM-08, RM-17, RM-18) | `DECISION_REQUIRED` |
| C3 | Seguridad | Certificaciones (por ejemplo, ISO 27001 o PCI DSS si aplica), mTLS, rotación de llaves, respuesta a incidentes y notificación de brechas | Certificados y alcance, resumen del último pentest, procedimiento de incidentes (SEG-01 a SEG-07, RM-12) | `DECISION_REQUIRED` |
| C4 | Datos | Qué datos pide, dónde los procesa (`pais_procesamiento`), retención, borrado y suboperadores | Lista de datos por operación, mapa de países y suboperadores, política de retención (DAT-01 a DAT-06, RM-13) | `DECISION_REQUIRED` |
| C5 | Disponibilidad | SLA, ventanas de mantenimiento, historial de incidentes y soporte | SLA firmado, historial de 12 meses, matriz de soporte (CON-01 a CON-04, RM-09) | `DECISION_REQUIRED` |
| C6 | Costo | Costo por operación, mínimos y costo de salida | Tabla de precios completa (CON-05, RM-14) | `DECISION_REQUIRED` |
| C7 | Salida | Exportación de datos, plazo de aviso y cooperación en la migración | Cláusula de salida, formato de exportación (CON-06, RM-15) | `DECISION_REQUIRED` |
| | | | **Suma de pesos** | **100 % cuando Pablo los fije** |

### Escala anclada (igual para los siete criterios)

| Puntos | Significa |
|---|---|
| 0 | No responde, o la respuesta contradice un requisito |
| 1 | Cumple solo con desarrollo y sin plazo comprometido |
| 2 | Cumple con desarrollo comprometido por escrito con plazo |
| 3 | Cumple hoy, con documentación |
| 4 | Cumple hoy, con documentación **y** lo demostró en el entorno de pruebas |

Puntaje de la propuesta = Σ (puntos del criterio × peso del criterio) / 4. Va de 0 a 100.

## 3. Paso 3 — Planilla por propuesta

Una planilla por oferente. Los evaluadores son al menos dos personas distintas, y la de Cumplimiento firma
C1 y C4. Cada puntaje lleva su evidencia: un número sin evidencia no cuenta.

| Criterio | Puntos (0–4) | Evidencia (documento y página) | Evaluador |
|---|---|---|---|
| C1 Regulación | | | |
| C2 Capacidades técnicas | | | |
| C3 Seguridad | | | |
| C4 Datos | | | |
| C5 Disponibilidad | | | |
| C6 Costo | | | |
| C7 Salida | | | |
| **Puntaje** | | | |

Eliminatorios: E-01 ☐ · E-02 ☐ · E-03 ☐ · E-04 ☐ · E-05 ☐ · E-06 ☐ · E-07 ☐ · E-08 ☐ · E-09 ☐ · E-10 ☐ · E-11 ☐
(marcar solo los que aplican al servicio).

## 4. Del resultado al registro

| Resultado | Dónde queda |
|---|---|
| Oferente elegido y contrato firmado | `contrato_tercero` (`es_critico`, `accede_a_datos_personales`, `pais_procesamiento`, `clausula_confidencialidad`, `clausula_auditoria`, `clausula_continuidad`, `acuerdo_nivel_servicio`, `comunicado_al_organismo`, `estado = VIGENTE`) |
| Planillas de evaluación de la selección | Archivo firmado, referenciado desde `contrato_tercero.evaluacion_riesgo_url` |
| Desempeño posterior, por periodo | `evaluacion_tercero` (`cumplimiento_sla`, `incidentes_atribuibles`, `resultado`) |

> **Ambigüedad registrada.** El plan de proveedores dice «evaluación ponderada (`evaluacion_tercero`)», pero
> esa tabla, tal como está en `docs/entidades/12_cumplimiento_asfi.puml`, mide el **desempeño periódico** de
> un contrato ya firmado (una fila por periodo, con SLA e incidentes) y no tiene dónde guardar la puntuación de
> una selección. Este documento no cambia el esquema: deja la planilla como archivo referenciado desde
> `evaluacion_riesgo_url`. Si Pablo quiere la selección en la base, es una microtarea nueva sobre la fuente
> `.puml` (regla 97.1), fuera de este carril.

## 5. Desempate y conflictos de interés

- Empate de puntaje: gana la propuesta con mejor C3 (seguridad); si persiste, la de menor costo de salida (C7).
  **`DECISION_REQUIRED` de Pablo**: este orden es una propuesta, no una decisión tomada.
- Quien evalúa declara por escrito que no tiene relación con ningún oferente.
