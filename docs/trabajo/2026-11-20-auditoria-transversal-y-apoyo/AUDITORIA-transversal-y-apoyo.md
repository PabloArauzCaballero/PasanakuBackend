# AUDITORÍA — `transversal-y-apoyo` (lote L5) · Pasanaku / AportaYa

Documento de solo lectura. No se modificó código, SQL, contratos, pruebas ni documentación del repo. Ningún dato de participante, secreto ni importe real aparece aquí; los importes de ejemplo y de los experimentos son **sintéticos**.

## 1. Ficha

| Campo | Valor |
|---|---|
| Módulo | `transversal-y-apoyo` (L5): `plataforma/*`, `servicios/{auditoria,transparencia,notificaciones,tarifas,publicidad,inversiones,erp}`, `contratos/`, `sql/` como fuente del esquema |
| Responsable | Pablo (dueño del producto) |
| Fecha | 2026-11-20 |
| Commit auditado `origin/test` | `07fc69719287b0dadd152d0293a054948df69ead` |
| Commit `origin/dev` | `850e0f51192d03094304630a61e44e5bd2a97efe` — **sin diferencias** en el alcance (`git diff --shortstat origin/test origin/dev -- <alcance>` vacío) |
| Historial | completo (`git fetch --unshallow`; `git rev-parse --is-shallow-repository` → `false`; 1 878 commits en `origin/test`) |
| Archivos en la huella | 1 282 versionados · 279 181 líneas (de ellas 131 888 son dos catálogos JSON generados de `servicios/auditoria`) |
| Leídos completos | 395 archivos · 31 388 líneas. El resto: parcial, por búsqueda/script, inventario o solo ejecutado (anexo §12) |
| Base local | PostgreSQL 16 efímero en `127.0.0.1:5433`, creado y destruido en esta sesión; `sql/aplicar.sql` + `sembrar.sql` ×3 + `verificaciones.sql` + `prueba_humo.sql` ejecutados |
| Lecturas delegadas | 4 subagentes de solo lectura en lotes disjuntos (plataforma · auditoria/transparencia/notificaciones · tarifas/publicidad/inversiones/erp · sql/contratos). Todo S1 y S2 marcado *confirmado* fue reabierto o ejecutado por la sesión principal (regla 70.4.8) |

## 2. Resumen

El código transversal (`plataforma/`) está bien encaminado en lo estructural (outbox en la misma transacción, `Dinero` con `BigDecimal` y moneda, deny-by-default, RLS por contexto de transacción) pero tiene defectos de fondo que afectan a todos los servicios: consumidores Kafka que **descartan** mensajes tras ~10 s, una idempotencia que **re-ejecuta** al vencer, un cliente HTTP que convierte "denegado" en "no hay dato", un sorteo cuyo orden depende solo de la semilla del servidor y un limitador de tasa evadible. El esquema (`sql/`) se aplica en limpio y su prueba de humo pasa 165/165, pero **el mayor contable acepta líneas añadidas a un asiento ya confirmado y con importes negativos** (reproducido) y **ningún control de `verificaciones.sql` lo detecta**; además el esquema permite acreditar pagos por efectivo al organizador, tarjeta o registro manual, sin vínculo con un webhook de firma válida. En los servicios de apoyo, `tarifas` expone a participantes operaciones que fijan su propio descuento, marcan su comisión como cobrada y crean deudas a terceros; `erp`, `publicidad` y los reportes de `auditoria` leen esquemas ajenos y **fallan con su propio rol** (ejecutado); `notificaciones` no envía nada porque el despacho no tiene punto de entrada; `transparencia` deja que soporte escriba la reputación y que un participante firme su propio certificado. Ninguno de los siete servicios de apoyo tiene aprobaciones dentro del circuito del pasanaku salvo la anotación manual de cobro de comisión; el respaldo empresarial aparece en este lote como **cuentas contables sembradas también para producción, mensajes activos de "cobertura" y rutas internas publicadas en el gateway**.

### Hallazgos por clase y severidad

| Clase | S1 | S2 | S3 | S4 | Total |
|---|---:|---:|---:|---:|---:|
| DINERO | 2 | 8 | 1 | 1 | 12 |
| FLUJO-QR | 1 | 0 | 0 | 0 | 1 |
| APROB | 1 | 0 | 1 | 0 | 2 |
| RESPALDO | 0 | 1 | 1 | 0 | 2 |
| SEG | 1 | 9 | 5 | 1 | 16 |
| ACOPLE | 0 | 1 | 1 | 0 | 2 |
| DEBIDO-PROCESO | 0 | 1 | 1 | 0 | 2 |
| HUECO | 0 | 3 | 2 | 0 | 5 |
| DEFECTO | 0 | 4 | 4 | 1 | 9 |
| RESILIENCIA | 0 | 1 | 3 | 1 | 5 |
| CONTRATO | 0 | 0 | 5 | 0 | 5 |
| TRAZA | 0 | 0 | 1 | 0 | 1 |
| OBSERV | 0 | 0 | 1 | 1 | 2 |
| DATOS | 0 | 5 | 5 | 0 | 10 |
| RENDIMIENTO | 0 | 0 | 1 | 0 | 1 |
| FRAGIL | 0 | 0 | 2 | 0 | 2 |
| DUP-X | 0 | 0 | 1 | 0 | 1 |
| ARQ | 0 | 0 | 1 | 1 | 2 |
| CLEAN | 0 | 0 | 0 | 1 | 1 |
| CONFIG | 0 | 0 | 2 | 0 | 2 |
| PRUEBAS | 0 | 1 | 1 | 0 | 2 |
| DOC | 0 | 0 | 0 | 1 | 1 |
| REG | 0 | 0 | 1 | 0 | 1 |
| COHERENCIA | 0 | 0 | 1 | 0 | 1 |
| **Total** | **5** | **34** | **41** | **8** | **88** |

Clases sin hallazgo propio (ver §8): FLUJO-FALTANTE, DUP-INT (se reporta dentro de TYA-DUP-X-001 y TYA-CLEAN-001), REESCRITURA, PISADA, PARCHE, UX-A11Y, OTRO.

### Los diez más graves

1. **TYA-DINERO-001** (S1) — El mayor acepta líneas tardías y negativas en asientos confirmados; reproducido y no detectado por `verificaciones.sql`.
2. **TYA-FLUJO-QR-001** (S1) — El esquema permite acreditar un pago sin QR ni webhook con firma válida (efectivo al organizador, tarjeta, manual).
3. **TYA-APROB-001** (S1) — Un participante marca su comisión como cobrada, o fallida a nombre de otro, por HTTP.
4. **TYA-SEG-001** (S1) — Un participante fija su descuento, su exención y a quién se cobra la comisión.
5. **TYA-DINERO-002** (S1) — Devolución de comisión sin idempotencia ni candado; autorizante del cuerpo.
6. **TYA-SEG-002** (S2) — 63 funciones `SECURITY DEFINER` ejecutables por cualquier servicio (ejecutado con `svc_publicidad`).
7. **TYA-RESILIENCIA-001** (S2) — Consumidores Kafka descartan el mensaje tras ~10 s; `aporte_cobrado` se puede perder.
8. **TYA-ACOPLE-001** (S2) — ERP, publicidad y reportes leen esquemas ajenos y fallan con su propio rol (ejecutado).
9. **TYA-HUECO-001** (S2) — Notificaciones no despacha nada: sin recordatorios de cobro en un flujo que solo avanza por pago.
10. **TYA-SEG-005** (S2) — El orden del sorteo depende solo de la semilla del servidor.

### Peldaño de evidencia por área (regla 30)

| Área | Peldaño | Respaldo |
|---|---|---|
| `sql/` (esquema y semillas) | `VERIFIED` para los hallazgos ejecutados (DINERO-001, SEG-002, ACOPLE-001, SEG-010, DATOS-008, idempotencia de semillas); `DISCOVERED` para el resto | `evidencia/db-*.txt` |
| `plataforma/` | `TESTED` (suites de los 6 módulos en verde: 186 pruebas) para lo que cubren; hallazgos en `DISCOVERED` | `evidencia/g-resumen-plataforma.txt` |
| 7 servicios de apoyo | `TESTED` para unitarios/web/contrato/barrido (503 pruebas en verde, 0 fallos); integración `BLOCKED` (Docker); hallazgos en `DISCOVERED` salvo los ejecutados en base | `evidencia/g-resumen-servicios.txt` |
| `contratos/` | `DISCOVERED` | lectura de los 10 pactos |
| Pantallas | `UNKNOWN` (no auditadas; ver §11) | — |

## 3. Huella y mapa de conexiones

### Huella

| Parte | Archivos | Líneas | Notas |
|---|---:|---:|---|
| `plataforma/` (gateway, comun-archivos, comun-datos, comun-dominio, comun-mensajeria, comun-pruebas, comun-web) | 201 | 15 868 | sin `build/` versionado; ningún archivo > 300 líneas |
| `servicios/auditoria` | 125 | 146 195 | 131 888 líneas en `catalogo-datos.json` y `catalogo-operaciones.json` (generados por `scripts/catalogo_de_datos.py`, `scripts/generar_catalogo_operaciones.py`) |
| `servicios/transparencia` | 95 | 13 801 | |
| `servicios/notificaciones` | 69 | 7 601 | |
| `servicios/tarifas` | 85 | 11 183 | |
| `servicios/publicidad` | 57 | 6 764 | cliente Angular generado en `clientes/angular/publicidad` |
| `servicios/inversiones` | 115 | 15 478 | sin `gradle.lockfile` (TYA-CONFIG-001) |
| `servicios/erp` | 76 | 11 214 | |
| `contratos/pactos` | 10 | 573 | formato propio (no Pact) |
| `sql/` | 449 | 50 497 | 351 `CREATE TABLE`, 687 FK, 47 tablas append-only según humo; 447 relaciones y 103 con RLS en la base aplicada |

Frontera tomada (supuesto, ver §11): las pantallas de `apps/*` que consumen estos servicios no se auditaron (son del lote L6 o de cada módulo); los clientes generados existen solo para `publicidad` (Angular).

### Mapa de conexiones

| Desde → hacia | Mecanismo | Punto exacto | Observación |
|---|---|---|---|
| Internet → 15 servicios | HTTP vía gateway | `plataforma/gateway/src/main/resources/rutas.yml:65-154` | prefijos completos; rate limit solo en `/sesiones/**`, `/usuarios/**`, `/billetera/retiros/**`, `/billetera/transferencias/**` (TYA-SEG-003, TYA-ARQ-001) |
| Todos los servicios → `plataforma/comun-*` | biblioteca | usos en main: `Datos` 251, `Reloj` 199, `Dinero` 196, `Outbox` 152, `Idempotencia` 63, `SesionDeLaPeticion` 63, `ClienteDeServicio` 19, `Consumidos` 13, `SorteoVerificable` 2, `CifradorSimetrico` 1 | `comun-archivos` solo en identidad |
| nucleo-financiero → tarifas | HTTP | `servicios/nucleo-financiero/.../CotizadorPorHttp.java` → `/comisiones/cotizaciones`, `/aceptacion` | timeout, reintento y breaker |
| inversiones → nucleo-financiero | HTTP | `servicios/inversiones/.../LibroPorHttp.java` → `/billetera/retenciones`, `/inversiones/debitos`, `/acreditaciones` | timeout PT3S + breaker; sin correlación (TYA-TRAZA-001); pacto `inversiones-nucleo-financiero.json` |
| transparencia → grupos | HTTP | `/grupos/{g}/integrantes/{u}/comparte`, `/grupos/sorteos/{id}/paquete` vía `ClienteDeServicio` | pacto `transparencia-grupos-pertenencia.json` sin prueba de productor |
| grupos → transparencia | HTTP | `servicios/grupos/.../HechosPorHttp.java:180-240` (`/reputacion/{u}/puntaje`, `/etapas/de-personas`) | TYA-CONTRATO-004 |
| auditoria → 14 servicios | HTTP | `/actuator/health/readiness` (2 s) y Prometheus (PT3S) | |
| grupos → tarifas | Kafka | `aportaya.grupos.grupo_creado` → `CongelarTarifaDelGrupo` | idempotente (`Consumidos` en la misma transacción) |
| tarifas → grupos | Kafka | `aportaya.tarifas.tarifa_congelada` | único evento de tarifas con consumidor |
| todos → auditoria | Kafka | patrón `aportaya\.[a-z_]+\.indicador_calculado` | idempotente; pacto `servicios-auditoria-indicador-calculado.json` |
| tarifas, erp, publicidad, transparencia, notificaciones, auditoria → nadie | Kafka (outbox) | `tarifas.comision_*`, `erp.*`, `publicidad.*`, `transparencia.*`, `notificaciones.*`, `auditoria.*` | sin consumidor (TYA-HUECO-002, -003, -005) |
| garantia → nucleo-financiero | Kafka | `aportaya.garantia.respaldo_reservado|ampliado|liberado|recuperado` | la reserva de respaldo deja asientos (inventario 4b) |
| erp → nucleo_financiero; publicidad → organizador; auditoria (reportes) → 12 tablas ajenas | SQL directo | TYA-ACOPLE-001 | **falla con el rol propio** |
| todos los servicios → `aportes.fn_*` | funciones SQL | `sql/00_base/03_permisos.sql:262-284` | TYA-SEG-002 |
| entregas → garantia | HTTP | `servicios/entregas/.../RespaldoPorHttp.java:55` → `POST /garantia/respaldo/coberturas` | ruta también publicada en el gateway (TYA-ARQ-001) |

Saltos de la operación de dinero del lote (98.8.1): **comisión de entrega** — nucleo-financiero cotiza en tarifas (HTTP) → acepta (HTTP) → tarifas devenga (`/comisiones/devengos`, HTTP de usuario) → cobro anotado (`/cobros`, HTTP de usuario, TYA-APROB-001) → evento `tarifas.comision_cobrada` → **ningún consumidor**: el mayor no recibe asiento (TYA-HUECO-002).

## 4. Distancia al flujo nuevo

### 4a. Inventario de aprobaciones (módulo y conexiones directas)

Búsqueda: `grep -rIil "aprobar\|aprobaci\|APROBAD\|doble_aprobacion\|comprobante\|postulaci\|habilit\|confirmar\|PENDIENTE_"` sobre el alcance, más inventario del esquema.

| ID | Dónde | Qué aprueba y quién | Estado · endpoint · permiso · evento · tabla | Clasificación | Qué se rompe si se quita |
|---|---|---|---|---|---|
| AP-01 | `servicios/tarifas/.../web/ComisionesController.java:141-160`, `CU31DevengarComision.java:150-245` | Un usuario afirma que la comisión se cobró o falló | `POST /comisiones/devengos/{id}/cobros` · `BILLETERA_OPERAR` · `tarifas.comision_cobrada`/`comision_incobrable` · `cargo_comision`, `cuenta_por_cobrar_comision` | **ELIMINABLE** (debe reemplazarse por evento de pago confirmado) — TYA-APROB-001 | CU-32 (facturación) y CU-35 dependen del estado cobrado |
| AP-02 | `sql/10_tablas/03_aportes_pagos_qr/comprobante_manual.sql:9-15` | Revisión manual de comprobante con segunda revisión | sin servicio que la use · tabla `comprobante_manual` | **ELIMINABLE** — TYA-APROB-002 | nada en código (tabla huérfana) |
| AP-03 | `sql/10_tablas/03_aportes_pagos_qr/pago.sql:23-27` | Pago `es_manual` registrado por una persona; canal `EFECTIVO_AL_ORGANIZADOR` | `pago` (aportes, L1) | **ELIMINABLE** — TYA-FLUJO-QR-001 | semillas dev/volumen que usan canales no QR |
| AP-04 | `servicios/notificaciones/.../dominio/IntencionEntrante.java:20,88`; `notificaciones.yaml:410` | El chatbot pide comprobante ante "ya pagué"; el contrato admite `PAGO_CONFIRMADO` | `POST /notificaciones/webhooks/{proveedor}` · público con firma · `notificaciones.respuesta_procesada` | **ELIMINABLE** | ningún consumidor actúa sobre esa acción |
| AP-05 | `servicios/tarifas/.../CU33DevolverComision.java` | Devolución autorizada por operador | `REVERSO_AUTORIZAR` · `devolucion_comision` | **CONSERVAR** (fuera del circuito del pasanaku: reverso de un cobro de la empresa) | — |
| AP-06 | `servicios/tarifas/.../CU34PublicarTarifario.java` | Comité aprueba tarifario (`actaComite`, `aprobadoPor`) | `TARIFARIO_PUBLICAR` | **DECISIÓN REQUERIDA (A-1)**: gobierno de tarifas puede ser exigencia regulatoria; norma a identificar por Cumplimiento | preaviso de cambio de tarifas |
| AP-07 | `servicios/erp` CU101/CU102/CU103 (`/presupuestos/{id}/aprobacion`, `/ordenes-de-compra/{id}/aprobacion`, `aprobar` de factura, cuatro ojos en pago) | Aprobaciones contables internas | `CONTABILIDAD_ERP_*` | **CONSERVAR** (control interno de la empresa, fuera del circuito) | — |
| AP-08 | `servicios/publicidad` CU110/CU111/CU112 | Verificación de socio, aprobación de campaña, moderación de creatividad | `PUBLICIDAD_*` | **CONSERVAR** (fuera del circuito) | — |
| AP-09 | `servicios/transparencia/.../CU76PublicarResena.java` (`moderar` → `APROBAR`) | Moderación de reseñas | `MODERADOR_CONTENIDO` | **CONSERVAR** (fuera del circuito) | — |
| AP-10 | `servicios/inversiones` CU122 | Confirmación de posición por el aliado (no humana) | — | **CONSERVAR** | — |
| AP-11 | `sql/60_semillas/15-eventos-y-plantillas.sql:35,142-144,342-344` | Mensaje "Aprobamos tu plan de regularización" | evento `PLAN_REGULARIZACION_APROBADO` | **DECISIÓN REQUERIDA (A-4)** (cobranza; depende de cómo se trate la mora) | plantillas |
| AP-12 | Esquema de módulos ajenos (inventario, sin auditar su interior): `solicitud_ingreso`, `decision_ingreso`, `solicitud_permuta`, `participante` (`POSTULANTE`), `entrega_fondo` (`autorizada_por`), `validacion_pre_entrega`, `solicitud_organizador`, `decision_habilitacion`, `tarea_automatizada` (`REQUIERE_APROBACION`), `cobertura_incumplimiento` (`aprobada_por`), `politica_cobertura.requiere_aprobacion_manual_desde`, `orden_retiro.requiere_doble_aprobacion` | Varios (organizador, operador, comité, doble aprobación de retiros) | tablas en `sql/10_tablas/02_*`, `04_*`, `07_*`, `08_*`, `10_*` | **Lo clasifica cada lote dueño** (L1–L4); `orden_retiro` es **DECISIÓN REQUERIDA (A-1)** (ADR-049) | — |

### 4b. Inventario de respaldo y cobertura (módulo y conexiones directas)

Búsqueda: `grep -rIil "respaldo\|cobertura\|pozo completo\|te cubrimos\|faltante"` sobre el alcance, inventario del esquema y de consumidores Kafka.

| ID | Dónde | Qué pone la empresa y cuándo | Asiento en el mayor | Clasificación | Qué depende |
|---|---|---|---|---|---|
| RS-01 | `sql/60_semillas/22-cuentas-de-sistema.sql:4,17-32` | Cuentas 9.1.01/9.2.01 "Caja de la empresa" y 9.1.02/9.2.02 "Reserva de respaldo" (BOB y USD), sembradas por `sembrar.sql` (también producción) | son las cuentas contrapartida de los asientos de reserva | **CONTRADICE R-NUEVA-2** — TYA-RESPALDO-002 | nucleo-financiero (configuración del mapeo lógico→código) |
| RS-02 | consumidor en nucleo-financiero de `aportaya.garantia.respaldo_reservado|ampliado|liberado|recuperado` | Reserva/ampliación/liberación/recuperación del respaldo empresarial; se dispara solo al recibir el evento | sí: asiento por evento (el detalle del asiento lo audita L1/L2) | **CONTRADICE R-NUEVA-2** | garantia (L2) |
| RS-03 | `plataforma/gateway/src/main/resources/rutas.yml:95-100` → `POST /garantia/respaldo/coberturas` | La empresa cubre el faltante del corte; lo invoca entregas (`RespaldoPorHttp.java:55`) con el token del usuario y también queda alcanzable desde internet | el que deje CU23CubrirConRespaldo (L2) | **CONTRADICE R-NUEVA-2** — TYA-ARQ-001 | entregas, garantia |
| RS-04 | `sql/10_tablas/08_garantia_incumplimiento/{capacidad_respaldo,reserva_respaldo,movimiento_reserva,cobertura_respaldo,cobertura_respaldo_linea,recuperacion_respaldo,plan_contingencia}.sql`; `sql/40_reglas/restricciones.sql:2411-2533` (R-GAR-08..13); `04_entregas_fondo/fondeo_entrega.sql:15`; `asiento_contable.origen_tipo 'COBERTURA'` | Estructura completa del respaldo empresarial | `movimiento_reserva` append-only | **CONTRADICE R-NUEVA-2** / **DECISIÓN REQUERIDA (A-3)** sobre qué hacer con lo construido | servicios garantia/entregas (L2), semillas, pruebas |
| RS-05 | `sql/60_semillas/02-politicas.sql:27-28` (`politica_cobertura` 5 %, topes, aprobación manual desde un monto) | Política del fondo de garantía (mutual) | — | **DECISIÓN REQUERIDA (A-3)**: fondo de participantes, no de la empresa | garantia |
| RS-06 | `sql/60_semillas/15-eventos-y-plantillas.sql:33,74,136-138,200,332-334`; `servicios/garantia/.../CU23CubrirIncumplimiento.java:167` | Mensajes "el fondo de garantía cubrió tu aporte" (activos, hoy no despachados) | — | **DECISIÓN REQUERIDA (A-3)** — TYA-RESPALDO-001 | notificaciones |
| RS-07 | `servicios/transparencia/.../dominio/CriterioDeInsignia.java:35-38`; `transparencia.yaml:775,781,891` | Insignia "sin cobertura del fondo"; tipos de evento `COBERTURA_*` | — | **DECISIÓN REQUERIDA (A-3)** | transparencia |
| RS-08 | `sql/60_semillas/19-reportes-y-retencion.sql:7,9,11` | Reportes "cubierto por garantía", "cobertura del fondo", `MOVIMIENTO_FONDO_GARANTIA` | — | **DECISIÓN REQUERIDA (A-3)** | auditoria (CU-58) |
| RS-09 | `sql/60_semillas/09-reglas-operativas.sql:6` (`BOLSA_COMPLETA` "o cubierta por el fondo"); `scripts/generar_catalogo_operaciones.py:30` | Regla operativa y texto del catálogo que prometen pozo completo | — | **CONTRADICE R-NUEVA-2** si "fondo" se lee como empresa; **DECISIÓN REQUERIDA (A-2)** | entregas, auditoria |
| RS-10 | `sql/10_tablas/03_aportes_pagos_qr/pago.sql:27` (canal `FONDO_GARANTIA`), `obligacion_aporte.monto_cubierto_garantia`, deducción `REPOSICION_FONDO_GARANTIA` | Cobertura del aporte con el fondo mutual | asiento según garantia/nucleo | **NO APLICA** a la empresa (fondo de participantes) salvo que Pablo lo extienda — **DECISIÓN REQUERIDA (A-3)** | aportes, entregas |
| RS-11 | `servicios/tarifas`, `servicios/erp`, `servicios/publicidad`, `servicios/inversiones` | Ninguno. `grep -i "respaldo\|cobertura\|garant\|reserva"` solo encuentra el aviso legal de inversiones (`TextoDeCondiciones.java:20-21`: la garantía del pozo **no** alcanza a la inversión) | — | ninguno encontrado | — |

### 4c. Ambigüedades

| ID | Qué muestra el código hoy (en este lote) | Supuesto tomado | A quién confirmar |
|---|---|---|---|
| A-1 | Aprobaciones regulatorias en el lote: tarifario por comité (AP-06); datos UIF expuestos por funciones SQL (TYA-SEG-002); doble aprobación de retiros solo como columna en el esquema (`orden_retiro`, L1). | No se asume que se eliminan; se listan como DECISIÓN REQUERIDA. | Pablo; Cumplimiento |
| A-2 | Este lote no implementa la entrega del turno; lo que toca es la regla `BOLSA_COMPLETA` sembrada (RS-09) y la comisión como cargo separado (ADR-051 D-04). No hay rama de "falta plata" en el lote. | No se inventa la regla. | Pablo |
| A-3 | Lo construido sobre el respaldo vive en el esquema y en semillas que se aplican en producción (RS-01..RS-04) y los mensajes de cobertura del fondo mutual siguen activos (RS-06). | Respaldo empresarial = contradice R-NUEVA-2; fondo mutual y aval = no aplica salvo decisión. | Pablo |
| A-4 | Mora: política sembrada sin procedencia (2 %, tope; `02-politicas.sql:18-19`); mensajes de mora y plan de regularización activos. | Sin cambio. | Pablo; Finanzas |

## 5. Hallazgos

Prefijo de identificador: `TYA` (= módulo `transversal-y-apoyo`). Confianza según §10: *confirmado* (abrí cada ubicación o lo ejecuté), *probable* (falta la comprobación que se nombra), *sospecha*. Toda ruta es relativa a la raíz del repo en `origin/test` 07fc6971. Los importes de los experimentos son **sintéticos**.

### 5.1 DINERO

### TYA-DINERO-001 — El mayor acepta líneas tardías y negativas en asientos ya confirmados, y ningún control lo detecta
- Clase / Severidad / Confianza: DINERO / **S1** / confirmado (ejecutado)
- Ubicaciones: `sql/10_tablas/03_aportes_pagos_qr/movimiento_contable.sql:10-11`; `sql/40_reglas/restricciones.sql` (función `fn_aud_asiento_cuadrado`, trigger `tg_asiento_cuadrado` sobre `asiento_contable`); `sql/50_verificacion/verificaciones.sql` (sin consulta de cuadre por asiento)
- Evidencia: `CREATE CONSTRAINT TRIGGER tg_asiento_cuadrado AFTER INSERT OR UPDATE ON nucleo_financiero.asiento_contable ... WHEN (new.estado IN ('CONFIRMADO','REVERSADO'))`; `movimiento_contable` no tiene ningún `CHECK` (consulta a `pg_constraint` vacía). Experimento en base local efímera (`evidencia/db-experimento-mayor.txt`): E1 asiento CONFIRMADO 100/100 → COMMIT; E2 en otra transacción `INSERT` de una línea debe 50 al mismo asiento → **COMMIT aceptado**, resultado `debe=150.00 haber=100.00`; E3 asiento con `debe=-100`/`haber=-100` → **aceptado**; E4 (control) descuadre en una sola transacción → rechazado `R-AUD-05`. Después, `verificaciones.sql` devuelve `(0 rows)` en sus 16 consultas (`evidencia/db-verificaciones-tras-experimento.txt`).
- Cómo se comprobó: apliqué `sql/aplicar.sql` + `sembrar.sql` en PostgreSQL 16 local efímero y ejecuté los cuatro casos; leí la definición del trigger con `pg_get_triggerdef`.
- Impacto: cualquier camino (bug de un servicio, consumidor duplicado que reinserta líneas, script) puede descuadrar el mayor de forma permanente: la tabla es append-only, así que no se puede ni corregir con `UPDATE`; el saldo cacheado (`fn_ctb_recalcular_saldo`) absorbe la línea. Afecta a todo el que lea saldos o estados del mayor (transparencia, ERP, conciliación).
- Causa de fondo: la invariante 91.2.1 se valida solo en el evento de la cabecera, no en el de la línea; el signo se dejó a la aplicación.
- Solución de fondo: `CHECK (debe >= 0 AND haber >= 0 AND (debe = 0) <> (haber = 0))`; trigger diferido también sobre `movimiento_contable` que revalide el asiento de cada línea insertada y rechace líneas de asientos no creados en la misma transacción; consulta de cuadre por asiento en `verificaciones.sql`.
- Relación con el flujo nuevo: FLUJO-QR (es el mayor donde aterriza el aporte cobrado)
- Conexiones y módulos afectados: nucleo-financiero (dueño, L1), erp, transparencia, auditoria.
- Estándares y normas: ISO/IEC 25010 Integridad (Seguridad) y Corrección funcional; regla 91.2.1, 91.2.2, 97.2.
- Esfuerzo estimado: chico

### TYA-DINERO-002 — Devolución de comisión sin idempotencia, sin candado y con el autorizante tomado del cuerpo
- Clase / Severidad / Confianza: DINERO / **S1** / confirmado (lectura); la carrera concurrente es probable (no ejecutada)
- Ubicaciones: `servicios/tarifas/src/main/java/bo/aportaya/tarifas/web/ComisionesController.java:162-176`; `.../aplicacion/CU33DevolverComision.java:63-75,107`; `sql/40_reglas/restricciones.sql:835-853`; `sql/10_tablas/11_tarifas_comisiones/devolucion_comision.sql`
- Evidencia: el controlador recibe `UUID idempotencyKey` y no lo pasa a `cu33.devolver(...)`; `cuerpo.getAutorizadaPor()` va directo al registro; CU33 lee con `devengos.ver(dsl, ...)` (sin `FOR UPDATE`, a diferencia de `anotarCobro` que usa `devengos.bloquear`); el trigger `fn_tar_validar_devolucion` hace `SUM` sin bloquear. Columnas reales de `devolucion_comision` (consulta a `information_schema`): sin `moneda`.
- Cómo se comprobó: lectura de las tres ubicaciones y consulta de columnas en la base local.
- Impacto: un reintento de red o un doble clic del operador registra dos devoluciones; dos en paralelo pasan ambas el tope bajo READ COMMITTED. La auditoría del actor es falsificable. La empresa devuelve dinero de más.
- Causa de fondo: no se usa la `Idempotencia` de plataforma; el tope vive en un trigger sin serialización.
- Solución de fondo: clave de idempotencia con `UNIQUE`; `devengos.bloquear` antes de leer lo cobrado; actor desde `ctx.usuarioId()`; columna `moneda`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: tarifas; nucleo-financiero (si algún día ejecuta el reembolso).
- Estándares y normas: ISO/IEC 25010 Integridad; CWE-362, CWE-639; reglas 91.1.2, 91.1.4, 96.3.
- Esfuerzo estimado: chico

### TYA-DINERO-003 — `Idempotencia` de plataforma re-ejecuta la operación al vencer la clave y no pone la precondición en la escritura
- Clase / Severidad / Confianza: DINERO / S2 / confirmado (lectura)
- Ubicaciones: `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/idempotencia/Idempotencia.java:133-148`; vigencia por omisión en `.../ConfiguracionComunWeb.java:54`
- Evidencia: `boolean expirada = previa.get(3, ...).isBefore(ahora); ... if (expirada || transitoria) { dsl.update(tabla)...where(identidad(ctx, operacion, clave)).execute(); return; }` — devuelve como operación nueva.
- Cómo se comprobó: lectura del método completo y de su único camino de reutilización.
- Impacto: un reintento con la misma clave pasado el vencimiento (24 h) vuelve a ejecutar en vez de devolver la respuesta guardada; dos reintentos simultáneos sobre una fila vencida ejecutan ambos. 63 archivos de servicios la usan.
- Causa de fondo: se modeló "expira la reserva" como "expira el derecho a reproducir".
- Solución de fondo: reproducir siempre respuestas 2xx/4xx; reutilizar solo con `WHERE codigo_http = :previo AND expira_en = :previa` y comprobar filas afectadas = 1.
- Relación con el flujo nuevo: FLUJO-QR (orden de cobro y webhook usan claves)
- Conexiones y módulos afectados: todos los servicios que la usan.
- Estándares y normas: ISO/IEC 25010 Fiabilidad; CWE-362; reglas 91.1.4, 96.3.2.
- Esfuerzo estimado: chico

### TYA-DINERO-004 — `idempotencyKey` aceptada e ignorada en las mutaciones de los servicios de apoyo; dedupe por SELECT-luego-INSERT
- Clase / Severidad / Confianza: DINERO / S2 / confirmado en tarifas (`ComisionesController.java:164`) y transparencia (permisos y firmas en `ReputacionController.java`); probable en el resto (informado por lectura delegada, no reabierto línea a línea)
- Ubicaciones: transparencia `web/ReputacionController.java:87,106,140,158,178,205,226`; notificaciones `web/NotificacionesController.java:122`; auditoria `web/AuditoriaController.java:141`, `web/ReportesController.java:92,123`; tarifas `ComisionesController.java:164`; erp `web/ErpController.java:239`; publicidad `web/PublicidadController.java` (todas las operaciones); tarifas `CU30:76-87`, `CU31:76-81`
- Evidencia: los parámetros `UUID idempotencyKey` no se usan en el cuerpo del método; CU30/CU31 consultan la clave antes del `INSERT` y no capturan la violación de `uq_cotizacion_idem`/`uq_devengo_idem`.
- Cómo se comprobó: lectura de `ComisionesController` y firmas de `ReputacionController`; para los demás, lectura delegada.
- Impacto: reintentos duplican eventos de outbox (con id nuevo), cobros de ERP y facturación publicitaria; la carrera sobre clave única termina en 500 en lugar de reproducción.
- Causa de fondo: el contrato OpenAPI exige la cabecera pero la implementación no la cablea; `Idempotencia` de plataforma no se usa en ninguno de los 7 servicios de apoyo.
- Solución de fondo: usar `Idempotencia` en cada mutación (o quitar la cabecera del contrato con versión); capturar `unique_violation` y reproducir.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: los 7 servicios del lote.
- Estándares y normas: ISO/IEC 25010 Fiabilidad; reglas 91.1.4, 96.3.3, 80.2.5.
- Esfuerzo estimado: medio

### TYA-DINERO-005 — ERP suma movimientos sin separar moneda ni estado; el pago ignora la moneda de la factura
- Clase / Severidad / Confianza: DINERO / S2 / probable (falta abrir `CU103.pagar:185-200` y ejecutar con dos monedas)
- Ubicaciones: `servicios/erp/src/main/java/bo/aportaya/erp/infraestructura/PeriodoRepositorio.java` (`totalesDe`), `PresupuestoRepositorio.java` (`saldosDelPeriodo`); `ComprasRepositorio.java:172-198`; `aplicacion/CU103FacturaDeProveedor.java:185-200`; `aplicacion/CU104*`
- Evidencia (lectura delegada): `SUM(debe)`/`SUM(haber)` sin `GROUP BY moneda` ni filtro de `estado`; `facturaBloqueada` no carga la moneda; `entrada.monto()` se suma sin comparar `entrada.moneda()`.
- Cómo se comprobó: informe del subagente de lectura; no reabierto.
- Impacto: un pago de 100 USD salda una factura de 100 BOB; cierres de periodo con totales mezclados.
- Causa de fondo: obsesión por primitivos (importe sin `Dinero` en la consulta).
- Solución de fondo: agrupar por moneda; `Dinero` con validación de moneda en `pagar`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: erp, nucleo-financiero.
- Estándares y normas: ISO/IEC 25010 Corrección funcional; regla 91.1.2.
- Esfuerzo estimado: chico

### TYA-DINERO-006 — Publicidad factura clics y entregas sin límite, sin dueño de cuenta y con tope mensual que nunca se mueve
- Clase / Severidad / Confianza: DINERO / S2 / probable (lectura delegada; no reabierto)
- Ubicaciones: `servicios/publicidad/src/main/java/bo/aportaya/publicidad/aplicacion/CU113EntregarAnuncio.java:96-118`; `CU111*.java:39-43`; `infraestructura/AnuncianteRepositorio.java:131-138`; `sql/30_indices/14_*.sql:73`; `sql/40_reglas/restricciones.sql:3111-3115`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Evidencia (delegada): `registrarClic` cobra por llamada sin `UNIQUE (impresion_id)` en `clic_anuncio`; `crearCampana` acepta cualquier `cuentaPublicitariaId`; `sumarConsumo` sin llamadores.
- Impacto: un anunciante puede agotar el presupuesto de otro; facturación inflada; el desborde termina en 500.
- Causa de fondo: autorización por rol sin propiedad; contadores no cableados.
- Solución de fondo: propiedad de la cuenta en cada operación; `UNIQUE` por impresión; cablear `sumarConsumo` con precondición en el `UPDATE`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: publicidad.
- Estándares y normas: ISO/IEC 25010 Integridad; OWASP API1:2023 (BOLA); reglas 90.1.2, 91.1.4.
- Esfuerzo estimado: medio

### TYA-DINERO-007 — Impuestos de la comisión: se ignora el tipo de cálculo del catálogo; NIT por omisión; simulador del SIN sin guardia de producción
- Clase / Severidad / Confianza: DINERO / S2 / confirmado parcial: `grep -rn tipo_calculo servicios/tarifas/src/main/java` → 0 coincidencias; semilla `sql/60_semillas/05-impuestos.sql:5-6` declara `IVA INCLUIDO_EN_PRECIO` e `IT SOBRE_PRECIO`. El resto, probable.
- Ubicaciones: `servicios/tarifas/.../dominio/CalculoDeComision.java:96-103`; `.../infraestructura/TarifarioRepositorio.java:137-147`; `.../consumo/ServicioDeImpuestosSimulado.java:22`; `servicios/tarifas/src/main/resources/application.yml` (`SIN_NIT_EMISOR` con valor por omisión)
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Evidencia: el catálogo distingue dos tipos de cálculo y el código no lee la columna.
- Impacto: importes fiscales de cada comisión mal calculados; en producción se emitirían facturas "VALIDADAS" que nunca pasaron por el SIN.
- Causa de fondo: el modelo del impuesto se simplificó en código sin seguir al catálogo.
- Solución de fondo: leer `tipo_calculo`; quitar el valor por omisión del NIT (90.3.4); guardia de producción como `inversiones/GuardiaDeProduccion`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: tarifas; cumplimiento fiscal.
- Estándares y normas: ISO/IEC 25010 Corrección funcional; reglas 90.3.4, 97.5.4. Norma tributaria: a identificar por Cumplimiento.
- Esfuerzo estimado: medio

### TYA-DINERO-008 — Deducciones de la entrega: suma sin candado y tabla no append-only
- Clase / Severidad / Confianza: DINERO / S2 / confirmado que `entregas.deduccion_entrega` no tiene trigger append-only (consulta a `pg_trigger` = 0, `evidencia/db-verificacion-hallazgos-3.txt`); la carrera es probable
- Ubicaciones: `sql/40_reglas/restricciones.sql:1846-1864`; `sql/10_tablas/04_entregas_fondo/deduccion_entrega.sql:14`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Evidencia: `SELECT COALESCE(SUM(monto),0) INTO v_total FROM deduccion_entrega WHERE entrega_id = v_entrega AND revertida_en IS NULL; UPDATE entrega_fondo SET total_deducciones = v_total` sin `FOR NO KEY UPDATE` previo.
- Impacto: dos deducciones concurrentes dejan `total_deducciones` desactualizado y el control "deducciones ≤ bolsa bruta" (91.2.3) se evalúa contra un total perdido; una deducción se puede "revertir" por `UPDATE`.
- Causa de fondo: el patrón de "bloquear antes de leer" que sí usa la billetera (`restricciones.sql:325-342`) no se replicó.
- Solución de fondo: candado de la fila de `entrega_fondo` antes del `SUM`; `deduccion_entrega` append-only con reversa por fila compensatoria.
- Relación con el flujo nuevo: ninguna directa (entrega del pozo, L2)
- Conexiones y módulos afectados: entregas (dueño, L2); tarifas (comisión como cargo separado, ADR-051 D-04).
- Estándares y normas: ISO/IEC 25010 Integridad; CWE-362; reglas 91.1.3, 91.2.3.
- Esfuerzo estimado: chico

### TYA-DINERO-009 — 80 de 347 tablas llevan importe sin columna `moneda`
- Clase / Severidad / Confianza: DINERO / S2 / probable (recuento por script del subagente sobre `sql/10_tablas`; muestra confirmada: `devolucion_comision` sin `moneda`)
- Ubicaciones (muestra): `entregas.deduccion_entrega`, `garantia.movimiento_fondo`, `garantia.cobertura_respaldo_linea`, `aportes.reembolso`, `aportes.conciliacion`, `nucleo_financiero.movimiento_billetera`, `nucleo_financiero.reverso_transaccion`, `tarifas.cuenta_por_cobrar_comision`, `tarifas.devolucion_comision`, `erp.cierre_periodo_contable`, `transparencia` (`monto_total_aportado`)
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Evidencia: solo 72 de 347 tablas tienen `moneda`; varias la heredan del padre, pero la regla 91.1.2 la pide por columna, DTO y evento.
- Impacto: sumas y reportes que mezclan monedas sin aviso cuando aparezca USD (ya hay cuentas USD en `22-cuentas-de-sistema.sql`).
- Causa de fondo: modelo heredado anterior a la regla.
- Solución de fondo: agregar `moneda CHAR(3)` en el modelo `.puml` (fuente de verdad) y regenerar.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los dueños de esas tablas.
- Estándares y normas: ISO 4217; regla 91.1.2.
- Esfuerzo estimado: grande

### TYA-DINERO-010 — Tablas de dinero sin clave de idempotencia ni unicidad natural
- Clase / Severidad / Confianza: DINERO / S2 / probable (inventario del subagente; no reabierto tabla por tabla)
- Ubicaciones: `nucleo_financiero.asiento_contable` (solo `UNIQUE numero`; nada sobre `(origen_tipo, origen_id)`), `garantia.movimiento_fondo`, `garantia.abono_recuperacion`, `entregas.deduccion_entrega`, `aportes.reembolso`, `nucleo_financiero.retencion_saldo`, `garantia.ejecucion_aval`, `tarifas.devolucion_comision`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Evidencia: la base local muestra en `asiento_contable` solo las columnas `origen_tipo`/`origen_id` sin índice único (consulta de columnas).
- Impacto: un consumidor que reprocesa un evento (ver TYA-RESILIENCIA-001/002) duplica el asiento; el único dique es `evento_consumido`.
- Causa de fondo: la idempotencia se resolvió en la mensajería y no en la escritura (91.2.4).
- Solución de fondo: `UNIQUE (origen_tipo, origen_id)` o `clave_idempotencia UNIQUE` en cada tabla de dinero.
- Relación con el flujo nuevo: FLUJO-QR (el aporte cobrado genera asiento)
- Conexiones y módulos afectados: nucleo-financiero, garantia, entregas, aportes, tarifas.
- Estándares y normas: reglas 91.1.4, 91.2.4.
- Esfuerzo estimado: medio

### TYA-DINERO-011 — Los tramos del tarifario se pueden modificar después de publicado y el precio "congelado" lee tramos vivos
- Clase / Severidad / Confianza: DINERO / S3 / probable (lectura delegada)
- Ubicaciones: `sql/40_reglas/restricciones.sql:738-752` (inmutabilidad solo de `concepto_tarifa`), `:3483-3487` (REVOKE a `rol_aplicacion`); `sql/00_base/03_permisos.sql:72` (GRANT directo a `svc_tarifas`); `servicios/tarifas/.../TarifarioRepositorio.java` (`regla`)
- Evidencia: `sql/40_reglas/restricciones.sql:738` → `CREATE OR REPLACE FUNCTION fn_tar_tarifario_inmutable() RETURNS trigger AS $$` · `DECLARE v_estado TEXT;` · `BEGIN`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: una comisión ya prometida a un grupo cambia sin preaviso (R-TAR-07).
- Solución de fondo: inmutabilidad de `regla_tarifa` y lectura del snapshot congelado.
- Causa de fondo: el REVOKE apunta a un rol que ya no es el de la aplicación (ADR-017).
- Relación con el flujo nuevo: ninguna · Conexiones: tarifas, grupos · Estándares: regla 91.1.3, 97.5.4 · Esfuerzo: chico

### TYA-DINERO-012 — Precisiones de dinero fuera de (14,2)/(16,2)
- Clase / Severidad / Confianza: DINERO / S4 / probable
- Ubicaciones: `pago.monto_comision_proveedor NUMERIC(10,2)`; `tarifas.*` `(12,2)`; `cuenta_custodia`, `movimiento_custodia`, `conciliacion_custodia` `(18,2)`; costos `(10,4)` en `envio_notificacion`, `proveedor_mensajeria`, `clic_anuncio`, `impresion_anuncio`; variables `NUMERIC(12,2)` en `fn_tar_validar_devolucion` (`restricciones.sql:836`)
- Evidencia: sin fragmento extraíble automáticamente de las ubicaciones citadas (ubicación de carpeta o patrón); ver descripción en Impacto.
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: desbordes o truncamientos en acumulados; costos sub-centavo sin regla de redondeo declarada.
- Causa de fondo: tipos elegidos tabla por tabla. Solución: unificar en el modelo. Relación: ninguna. Conexiones: varios. Estándares: regla 91.1.1. Esfuerzo: chico

### 5.2 FLUJO-QR, APROB y RESPALDO

### TYA-FLUJO-QR-001 — El esquema permite acreditar un pago sin QR ni webhook con firma válida
- Clase / Severidad / Confianza: FLUJO-QR / **S1** / confirmado (catálogo de la base local)
- Ubicaciones: `sql/10_tablas/03_aportes_pagos_qr/pago.sql:23-27`; `intento_pago.sql:17`; `webhook_pasarela.sql:10-20`; `sql/60_semillas/24-metadata-comentarios.sql:896`
- Evidencia: `ck_pago_canal CHECK (canal IN ('BILLETERA_MOVIL','COMPENSACION_INTERNA','EFECTIVO_AL_ORGANIZADOR','FONDO_GARANTIA','QR_INTEROPERABLE','TARJETA','TRANSFERENCIA_BANCARIA'))`; `pago` tiene `es_manual` y `registrado_por` y ninguna FK ni `CHECK` que exija un `webhook_pasarela` con `firma_valida`; el único `CHECK` de `webhook_pasarela` es el de `estado` (consulta a `pg_constraint`, `evidencia/db-verificacion-hallazgos-1.txt`). `grep -rn firma_valida sql/40_reglas sql/50_verificacion` → 0.
- Cómo se comprobó: `pg_get_constraintdef` sobre la base aplicada; grep.
- Impacto: la base, último dique (91.2), no impide un pago `ACREDITADO` por efectivo al organizador, tarjeta o registro manual. Contradice R-NUEVA-1, la regla 91.3.1 (solo webhook verificado), 91.5.2 (el organizador no es caja) y 90.2.10 (tarjeta fuera de alcance PCI declarado).
- Causa de fondo: el modelo de pagos es anterior a la decisión "solo QR".
- Solución de fondo: depende de una decisión de negocio (¿se mantienen canales internos como `COMPENSACION_INTERNA`?). Como mínimo: retirar `EFECTIVO_AL_ORGANIZADOR` y `TARJETA`; `pago.webhook_id` con FK y `CHECK (estado <> 'ACREDITADO' OR canal <> 'QR_INTEROPERABLE' OR webhook_id IS NOT NULL)`; `CHECK (pago_id IS NULL OR firma_valida)` en el webhook.
- Relación con el flujo nuevo: FLUJO-QR, APROB
- Conexiones y módulos afectados: aportes (dueño, L1).
- Estándares y normas: ISO/IEC 25010 Integridad; reglas 91.3.1, 91.3.2, 91.5.2, 90.2.10.
- Esfuerzo estimado: medio

### TYA-APROB-001 — Cualquier participante puede marcar una comisión como cobrada (o fallida) por HTTP, sin webhook ni propiedad
- Clase / Severidad / Confianza: APROB / **S1** / confirmado (lectura + base)
- Ubicaciones: `servicios/tarifas/.../web/ComisionesController.java:141-160`; `.../aplicacion/CU31DevengarComision.java:150-245`; `sql/60_semillas/10-roles-y-permisos.sql:154,156`; `plataforma/gateway/src/main/resources/rutas.yml:146`
- Evidencia: `@Permiso("BILLETERA_OPERAR") anotarCobroDeComision(...)` → `Boolean.TRUE.equals(cuerpo.getExitoso())`; CU31 solo exige que el devengo exista (`devengos.bloquear(...).orElseThrow`); `BILLETERA_OPERAR` está asignado a `PARTICIPANTE` y `ORGANIZADOR`; `tarifas.devengo_comision` y `cargo_comision` no tienen RLS (`relrowsecurity = f`).
- Cómo se comprobó: lectura y consulta de RLS en la base local.
- Impacto: un participante marca su comisión como pagada sin pagar, o con tres `exitoso=false` abre una cuenta por cobrar y emite `comision_incobrable` contra **otra** persona. Un "pago" que no viene de la pasarela confirmada.
- Causa de fondo: una operación pensada como callback entre servicios quedó publicada en el gateway con un permiso de usuario final.
- Solución de fondo: confirmación desde un evento de nucleo-financiero/pasarela deduplicado por `message_id`; retirar la ruta del gateway; si se mantiene, credencial de servicio con alcance.
- Relación con el flujo nuevo: APROB, FLUJO-QR
- Conexiones y módulos afectados: tarifas, nucleo-financiero, gateway.
- Estándares y normas: OWASP API5:2023 (BFLA), API1:2023 (BOLA); CWE-285; reglas 91.3.1, 98.6.4.
- Esfuerzo estimado: medio

### TYA-APROB-002 — Revisión manual de comprobantes que acredita pagos sigue en el esquema (tabla huérfana)
- Clase / Severidad / Confianza: APROB / S3 / probable (sin referencias en `servicios/*/src/main` según grep del subagente)
- Ubicaciones: `sql/10_tablas/03_aportes_pagos_qr/comprobante_manual.sql:9-15`; `servicios/notificaciones/.../dominio/IntencionEntrante.java:20,88` (acción `COMPROBANTE_SOLICITADO`); `servicios/notificaciones/src/main/resources/openapi/notificaciones.yaml:410` (enum `PAGO_CONFIRMADO`)
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Evidencia: estados `APROBADO/OBSERVADO/PENDIENTE/RECHAZADO`, `revisado_por`, `segunda_revision_por`; el chatbot clasifica "ya pagué" y pide comprobante; el contrato admite una acción `PAGO_CONFIRMADO` que el código no emite.
- Impacto: hoy no mueve dinero, pero deja abierta una vía de confirmación de pago distinta del webhook (R-NUEVA-1, 91.3.1).
- Causa de fondo: modelo anterior a "solo QR". Solución: retirar tabla, acción y valor de enum con versión de contrato.
- Relación con el flujo nuevo: APROB, FLUJO-QR · Conexiones: aportes, notificaciones · Estándares: regla 91.3.1, 98.1.3 · Esfuerzo: chico

### TYA-RESPALDO-001 — Mensajes activos que anuncian "el fondo de garantía cubrió tu aporte"
- Clase / Severidad / Confianza: RESPALDO / S3 / confirmado
- Ubicaciones: `sql/60_semillas/15-eventos-y-plantillas.sql:33,74,136-138,200,332-334`; `servicios/garantia/.../aplicacion/CU23CubrirIncumplimiento.java:167` (publica `COBERTURA_APLICADA`); `servicios/transparencia/.../dominio/CriterioDeInsignia.java:35-38` (insignia "sin cobertura del fondo"); `sql/60_semillas/19-reportes-y-retencion.sql:7,9,11`; `servicios/transparencia/src/main/resources/openapi/transparencia.yaml:775,781,891`
- Evidencia: evento `('COBERTURA_APLICADA','El fondo de garantía cubrió tu aporte y quedó una deuda a tu nombre',...)`; plantillas IN_APP/PUSH/CORREO en `APROBADA`, activas.
- Cómo se comprobó: grep y lectura de las semillas; grep del publicador.
- Impacto: hoy no se despacha porque el despachador no tiene entrada (TYA-HUECO-001); en cuanto se cablee, comunica una cobertura. El fondo de garantía es mutual (dinero de participantes, no de la empresa): si R-NUEVA-2 también lo alcanza es DECISIÓN REQUERIDA (A-3).
- Causa de fondo: catálogo de mensajes construido sobre el flujo de cobertura.
- Solución de fondo: decidir A-3; después retirar o reescribir evento, plantillas, insignia y reportes en el mismo cambio.
- Relación con el flujo nuevo: RESPALDO · Conexiones: garantia (L2), notificaciones, transparencia, auditoria · Estándares: regla 97.4.5 · Esfuerzo: chico

### TYA-RESPALDO-002 — Cuentas contables "Caja de la empresa" y "Reserva de respaldo" en las semillas que también se aplican en producción
- Clase / Severidad / Confianza: RESPALDO / S2 / confirmado
- Ubicaciones: `sql/60_semillas/22-cuentas-de-sistema.sql:4,17-32`; `sql/60_semillas/sembrar.sql:1`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `-- SINTETICO · apto_produccion=false. Arbol 9 ...`; `sembrar.sql`: `-- Catálogos mínimos — también se aplican en producción`. nucleo-financiero consume `aportaya.garantia.respaldo_reservado|ampliado|liberado|recuperado` (grep de `@KafkaListener`), así que la reserva de respaldo **sí** deja asientos en el mayor.
- Impacto: datos declarados sintéticos llegan a producción (97.4.1, 97.6.3) y son la contrapartida contable del camino que R-NUEVA-2 prohíbe.
- Causa de fondo: una sola semilla para producción y simulación.
- Solución de fondo: separar semillas de simulación; decidir A-3 sobre la reserva de respaldo.
- Relación con el flujo nuevo: RESPALDO · Conexiones: nucleo-financiero, garantia · Estándares: reglas 97.4.1, 97.6.3, 91.5 · Esfuerzo: chico

### 5.3 SEG

### TYA-SEG-001 — Un participante elige su propio descuento, su exención total y a quién se le cobra la comisión
- Clase / Severidad / Confianza: SEG / **S1** / confirmado (lectura + base)
- Ubicaciones: `servicios/tarifas/.../web/ComisionesController.java:50-66,112-128`; `.../aplicacion/CU31DevengarComision.java:104-115`; `.../dominio/CalculoDeComision.java:83-88`
- Evidencia: `@Permiso("BILLETERA_OPERAR") devengarComision(...)` → `cuerpo.getUsuarioObligadoId(), ..., MapeoDeTarifas.ceroSiFalta(cuerpo.getDescuento(), cuerpo.getDescuento()), Boolean.TRUE.equals(cuerpo.getExentoTotal())`; CU31: `String estadoInicial = entrada.exentoTotal() ? EstadoDelDevengo.EXONERADO : EstadoDelDevengo.DEVENGADO;`. Sin RLS en `devengo_comision`.
- Cómo se comprobó: lectura; permisos en `sql/60_semillas/10-roles-y-permisos.sql:156`; `relrowsecurity` en la base local.
- Impacto: comisión de la plataforma condonada a voluntad; deuda creada a nombre de un tercero (asignación masiva).
- Causa de fondo: el precio y el obligado se toman del cliente (90.1.7).
- Solución de fondo: descuento, segmento y exención resueltos en el servidor; obligado derivado de la cotización/operación; `/devengos` solo para credencial de servicio.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: tarifas; nucleo-financiero (llama a `/comisiones/cotizaciones`).
- Estándares y normas: OWASP API3:2023 (mass assignment), API1:2023; CWE-639, CWE-915; reglas 90.1.2, 90.1.5, 90.1.7.
- Esfuerzo estimado: medio

### TYA-SEG-002 — 63 funciones `SECURITY DEFINER` del esquema `aportes` ejecutables por cualquier rol de servicio
- Clase / Severidad / Confianza: SEG / S2 / confirmado (ejecutado)
- Ubicaciones: `sql/00_base/03_permisos.sql:262-284`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal y ejecución en base local o Gradle (ver `evidencia/`)
- Evidencia: `IF f.nombre !~ '^fn_seg_' THEN EXECUTE format('ALTER FUNCTION %s SECURITY DEFINER', f.fn);`. Base local: 63 de 63 funciones `prosecdef` con `proacl` nulo (EXECUTE por PUBLIC). `SET ROLE svc_publicidad; SELECT aportes.fn_uif_titular(gen_random_uuid())` → ejecuta sin error (`evidencia/db-verificacion-hallazgos-2.txt`).
- Impacto: un servicio comprometido (o un bug) obtiene el titular de cualquier transacción de billetera (`fn_uif_titular`), inserta registros en `cumplimiento.registro_operacion_relevante` (`fn_uif_registrar_operacion`) o recalcula saldos, saltando la frontera de GRANT por esquema (ADR-017).
- Causa de fondo: se aplicó `SECURITY DEFINER` en bloque para que los triggers vean tablas ajenas, sin revocar `EXECUTE` de PUBLIC.
- Solución de fondo: `REVOKE ALL ON ALL FUNCTIONS IN SCHEMA aportes FROM PUBLIC`; `SECURITY DEFINER` solo en funciones de trigger.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los servicios; cumplimiento.
- Estándares y normas: CWE-250, CWE-269; reglas 98.1.1, 98.6.4, 90.1.4.
- Esfuerzo estimado: chico

### TYA-SEG-003 — Rate limiting del borde evadible variando la cabecera `Authorization`
- Clase / Severidad / Confianza: SEG / S2 / confirmado para `Authorization`; probable para `X-Forwarded-For` (comportamiento de librería no ejecutado)
- Ubicaciones: `plataforma/gateway/src/main/java/bo/aportaya/gateway/LimitadorDeTasa.java:125-136`; `plataforma/gateway/src/main/resources/application.yml:48`; `despliegue/nginx/aportaya.conf:72`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Evidencia: `return (autorizacion == null || autorizacion.isBlank()) ? ip : ip + ":" + autorizacion.hashCode();` (el token no se valida antes); `forward-headers-strategy: framework` y NGINX `X-Forwarded-For $proxy_add_x_forwarded_for` (agrega, no reemplaza).
- Impacto: un `Bearer` basura distinto por pedido abre un bucket nuevo en `/api/v1/sesiones/**` y `/usuarios/**`: fuerza bruta de login/OTP limitada solo por el bloqueo de cuenta.
- Causa de fondo: clave del limitador controlada por el cliente.
- Solución de fondo: clave solo por IP en rutas sin sesión; NGINX con `$remote_addr`; bucket por identificador dentro de identidad.
- Relación con el flujo nuevo: ninguna · Conexiones: identidad, gateway · Estándares: OWASP API4:2023; CWE-307; regla 90.1.14 · Esfuerzo: chico

### TYA-SEG-004 — `ClienteDeServicio` devuelve 422 con el cuerpo interno del otro servicio y trata 401/403/409/422 como "no hay dato"
- Clase / Severidad / Confianza: SEG / S2 / confirmado
- Ubicaciones: `plataforma/comun-web/.../clientes/ClienteDeServicio.java:68-91`; `.../errores/ManejadorGlobalDeErrores.java:253-256`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `if (...is4xxClientError()) { BITACORA.warn("... {}: {}", destino, ..., respondioMal.getResponseBodyAsString()); return Optional.empty(); } throw new ErrorDeDominio("El servicio %s respondio %s: %s".formatted(...getResponseBodyAsString()));` y `reglaDelDominio(ErrorDeDominio e) → unprocessableEntity()... e.getMessage()`.
- Impacto: (a) un 5xx del dependido sale al cliente final como 422 con nombre del servicio y cuerpo crudo (fuga de arquitectura y de datos); (b) "denegado" se confunde con "no existe" — p. ej. grupos recibe 403 de transparencia y lo toma como "sin historial" (TYA-CONTRATO-004); (c) el cuerpo, que puede llevar importes de `ErrorDeNegocio`, se escribe en log (90.2.1). 19 archivos main en 9 servicios lo usan.
- Causa de fondo: un solo tipo de excepción para errores de cliente, de dependencia y de dominio.
- Solución de fondo: `DependenciaNoDisponible` → 503 sin cuerpo; `Optional.empty()` solo para 404; resultado tipado para 403/409/422; nunca loguear el cuerpo.
- Relación con el flujo nuevo: ninguna · Conexiones: 9 servicios · Estándares: RFC 9110 (semántica de 502/503); CWE-209, CWE-532; reglas 90.1.13, 96.4.4, 98.2.4 · Esfuerzo: medio

### TYA-SEG-005 — El orden del sorteo depende solo de la semilla del servidor; la entropía de los participantes no lo afecta
- Clase / Severidad / Confianza: SEG / S2 / confirmado (lectura)
- Ubicaciones: `plataforma/comun-dominio/.../SorteoVerificable.java:66-102`; `servicios/grupos/.../aplicacion/CU60Sortear.java:80,105-121`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `barajarDeterminista(String semilla, List<T> cupos)` usa `indiceDelPaso(semilla, i, i + 1)`; las entropías solo entran en `hashDelCompromiso`. `CU60Sortear.java:80`: `String semilla = ids.nuevo().toString() + ids.nuevo();`, sellada en la base en la fase 1.
- Impacto: quien opera el servidor o lee la base conoce el orden de cobro antes de revelarlo y puede regenerar la semilla hasta un orden favorable antes de comprometer; el "sorteo verificable" no protege contra el operador. Área de máxima sensibilidad (90.5).
- Causa de fondo: el compromiso ata las entropías, pero el barajado no las usa.
- Solución de fondo: semilla efectiva `SHA-256(semilla || entropías reveladas tras el compromiso)`, con versión del algoritmo publicada en el protocolo de CU-61.
- Relación con el flujo nuevo: ninguna (el sorteo sigue sin aprobación humana) · Conexiones: grupos (L3), transparencia · Estándares: CWE-330; regla 90.5 · Esfuerzo: medio

### TYA-SEG-006 — Certificado de reputación firmado por la plataforma con contenido elegido por el participante
- Clase / Severidad / Confianza: SEG / S2 / confirmado (lectura)
- Ubicaciones: `servicios/transparencia/.../aplicacion/CU75EmitirCertificado.java:76-95`; `.../web/MapeoDeReputacion.java:96-103`; `.../web/ReputacionController.java:177-181`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `new LinkedHashMap<>(cuerpo.getDisponibles())`, `Boolean.TRUE.equals(cuerpo.getIdentidadVerificada())`; CU75: `var contenido = CertificadoVerificable.contenido(entrada.disponibles(), entrada.incluir()); ... firmar(hashContenido, claveDeFirma)`. Solo se exige `usuarioId == ctx.usuarioId()` (línea 76); `snapshotId` no se cruza con el contenido.
- Impacto: certificados "verificables" con puntaje y nivel inventados, firmados con la clave HMAC de la plataforma; la verificación de identidad se salta con un booleano.
- Causa de fondo: el contenido no se lee de `snapshot_reputacion` en el servidor.
- Solución de fondo: cargar el contenido desde la foto del propio usuario y el estado de identidad desde identidad.
- Relación con el flujo nuevo: ninguna · Conexiones: transparencia, identidad · Estándares: CWE-345, CWE-602; regla 90.1.7 · Esfuerzo: chico

### TYA-SEG-007 — Reseñas con autor, ciclo cerrado y antecedentes tomados del cuerpo
- Clase / Severidad / Confianza: SEG / S2 / confirmado (lectura)
- Ubicaciones: `servicios/transparencia/.../aplicacion/CU76PublicarResena.java:75,97,110,203-214`; `.../web/MapeoDeReputacion.java:106-114`; `sql/40_reglas/restricciones.sql:2198-2212`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `cuerpo.getAutorParticipanteId()`, `Boolean.TRUE.equals(cuerpo.getCicloCerrado())`; CU76 no compara el autor con `ctx`; el trigger `fn_rep_validar_resena` solo impide la autorreseña y exige que el reseñado sea del grupo.
- Impacto: un participante reseña en nombre de otro y salta la regla de ciclo cerrado (AP-CU76-02) y la atenuación de peso. Afecta reputación ajena.
- Causa de fondo: hechos que deben venir de grupos llegan del cliente.
- Solución de fondo: autor desde la sesión; ciclo, expulsión y antecedentes pedidos a grupos.
- Relación con el flujo nuevo: ninguna · Conexiones: transparencia, grupos · Estándares: OWASP API1:2023; CWE-639; regla 90.1.2 · Esfuerzo: chico

### TYA-SEG-008 — La cadena pública de transparencia se sella con un permiso de lectura y con hechos enviados por el llamador
- Clase / Severidad / Confianza: SEG / S2 / confirmado (lectura)
- Ubicaciones: `servicios/transparencia/.../web/ReputacionController.java:139-145`; `.../web/MapeoDeReputacion.java:84-93`; `.../aplicacion/CU72SellarBloque.java:62-67`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `@Permiso("AUDITORIA_LEER") sellarBloque(...)`; `cuerpo.getHechos().stream()...`, `cuerpo.getExcepcionesDeConciliacionAbiertas()`.
- Impacto: quien solo puede leer auditoría sella "hechos" arbitrarios en la prueba pública y puede declarar 0 excepciones abiertas (R-BIL-12). Lo sellado no se deriva del mayor (91.1.5).
- Causa de fondo: el sellado se diseñó como operación manual con datos entrantes.
- Solución de fondo: permiso propio de escritura; hechos calculados en el servidor desde el mayor y la conciliación (por API).
- Relación con el flujo nuevo: ninguna · Conexiones: transparencia, nucleo-financiero, aportes · Estándares: OWASP API5:2023; regla 91.1.5 · Esfuerzo: medio

### TYA-SEG-009 — Webhook de mensajería: solo se firma `cargaUtil`; remitente y contenido viajan sin firma
- Clase / Severidad / Confianza: SEG / S2 / confirmado (lectura)
- Ubicaciones: `servicios/notificaciones/.../aplicacion/CU82ProcesarRespuesta.java:69,80,85,100,117`; `.../web/NotificacionesController.java:146-158`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `FirmaDeWebhook.verifica(entrada.cargaUtil(), entrada.firma(), secretoWebhook)`; luego `respuestas.porIdentificador(dsl, entrada.remitente(), ...)`, `IntencionEntrante.clasificar(entrada.contenido())`, `supresiones.suprimir(dsl, entrada.remitente(), canal, "SOLICITUD_LEGAL", "TODAS", ...)`.
- Impacto: con un par (cargaUtil, firma) capturado se suprime cualquier contacto verificado (silenciando sus recordatorios de pago) o se abren reclamos falsos. Sin ventana de tiempo ni anti-repetición.
- Causa de fondo: la firma cubre un campo, no la petición.
- Solución de fondo: derivar todos los campos de la carga firmada; timestamp con ventana.
- Relación con el flujo nuevo: ninguna · Conexiones: notificaciones, proveedor de mensajería · Estándares: CWE-345, CWE-294 · Esfuerzo: chico

### TYA-SEG-010 — RLS ausente en tablas con datos personales y financieros de participantes
- Clase / Severidad / Confianza: SEG / S2 / confirmado (base local)
- Ubicaciones: `sql/40_reglas/restricciones.sql:1306-1500` (cobertura por nombre de columna `usuario_id`/`cuenta_billetera_id`); tablas `aportes.pago` (`pagador_nombre`, `pagador_documento`, `pago.sql:19-20`), `aportes.webhook_pasarela`, `aportes.qr_cobro`, `aportes.obligacion_aporte`, `entregas.entrega_fondo`, `identidad.borrador_onboarding`, `erp.tercero_comercial`, `publicidad.socio_comercial`, `tarifas.devengo_comision`, `tarifas.cargo_comision`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal y ejecución en base local o Gradle (ver `evidencia/`)
- Evidencia: `relrowsecurity = f` en las ocho primeras (`evidencia/db-verificacion-hallazgos-3.txt`) y en las dos de tarifas; 103 de 447 relaciones con RLS.
- Impacto: la defensa en profundidad de ADR-007/021 no cubre las tablas de pago ni de entrega; un error de autorización en el servicio expone documentos y pagos de otros.
- Causa de fondo: la política se aplica por heurística de nombre de columna.
- Solución de fondo: declarar en el modelo qué tablas son por titular y con qué columna.
- Relación con el flujo nuevo: FLUJO-QR · Conexiones: aportes, entregas, identidad, erp, publicidad, tarifas · Estándares: OWASP ASVS (control de acceso); reglas 90.1.3, 96.5.4 · Esfuerzo: medio

### TYA-SEG-011 — Validación del JWT sin `iss` ni `aud`; JWKS sin timeout
- Clase / Severidad / Confianza: SEG / S3 / probable (lectura delegada)
- Ubicaciones: `plataforma/comun-web/.../seguridad/ConfiguracionDelDecodificador.java:26-29`; `servicios/identidad/.../EmisorDeAcceso.java:144-162`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Evidencia: `NimbusJwtDecoder.withJwkSetUri(jwksUri).build();`
- Impacto: la evidencia de step-up (misma clave) no entra como sesión solo porque le falta el reclamo `rol`: protección accidental.
- Solución: emitir y validar `iss`/`aud`; timeouts del JWKS. Causa: configuración por omisión. Relación: ninguna. Conexiones: todos. Estándares: RFC 7519 §4.1.1/§4.1.3; CWE-287. Esfuerzo: chico

### TYA-SEG-012 — Avisos internos legibles y marcables por cualquier operador con el permiso
- Clase / Severidad / Confianza: SEG / S3 / probable (lectura delegada)
- Ubicaciones: `servicios/notificaciones/.../web/NotificacionesController.java:258-267`; `.../infraestructura/AvisoInternoRepositorio.java:54-65`; `sql/40_reglas/restricciones.sql:1326-1328`
- Evidencia: `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/web/NotificacionesController.java:258` → `@Override` · `@Permiso("AUDITORIA_LEER")` · `@PermisoAlternativo({"ADMIN_PLATAFORMA"})`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: BOLA entre operadores del backoffice. Causa: sin chequeo de destinatario. Solución: filtrar por destinatario. Relación: ninguna. Conexiones: notificaciones. Estándares: OWASP API1:2023. Esfuerzo: chico

### TYA-SEG-013 — Identificador de contacto en la URL de la consulta de supresión (operación deprecada pero viva)
- Clase / Severidad / Confianza: SEG / S3 / probable
- Ubicaciones: `servicios/notificaciones/.../web/NotificacionesController.java:92-98`
- Evidencia: `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/web/NotificacionesController.java:92` → `@Deprecated` · `@Override` · `@Permiso("GRUPO_ADMINISTRAR")`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: teléfono/correo en logs de acceso del gateway y de cada salto (90.2.2). Solución: retirar la GET con fecha declarada. Causa: compatibilidad. Relación: ninguna. Conexiones: grupos ya migró a POST. Estándares: CWE-598; regla 90.2.2. Esfuerzo: chico

### TYA-SEG-014 — Actor y tipo de cambio de tarifario tomados del cuerpo
- Clase / Severidad / Confianza: SEG / S3 / probable
- Ubicaciones: `servicios/tarifas/.../aplicacion/CU34PublicarTarifario.java:79,94`; `.../dominio/EntradaEnVigencia.java` (`exigePreaviso`)
- Evidencia: `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/CU34PublicarTarifario.java:62` → `boolean requierePreaviso = EntradaEnVigencia.exigePreaviso(entrada.tipoCambio());`; líneas 74-75 y 95: `entrada.aprobadoPor(), entrada.tipoCambio()` (ambos del cuerpo).
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: `aprobadoPor` falsificable; un aumento etiquetado como `REDUCCION` evita el preaviso obligatorio. Solución: actor de la sesión y tipo derivado de comparar tarifas. Causa: datos del cliente. Relación: ninguna. Conexiones: tarifas, notificaciones. Estándares: regla 90.1.7. Esfuerzo: chico

### TYA-SEG-015 — `CifradorSimetrico` sin derivación de clave, sin identificador de clave y sin AAD
- Clase / Severidad / Confianza: SEG / S3 / probable
- Ubicaciones: `plataforma/comun-dominio/.../CifradorSimetrico.java:121-129`; uso en `servicios/entregas/.../CU18RegistrarCuentaDestino.java:107`
- Evidencia: `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/CifradorSimetrico.java:121` → `private static SecretKey derivarClave(String secreto) {` · `try {` · `MessageDigest sha256 = MessageDigest.getInstance("SHA-256");`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: la clave del número de cuenta bancaria no se puede rotar; un cifrado se puede trasplantar entre filas. Solución: HKDF + kid + AAD con el id de fila. Causa: diseño mínimo. Relación: ninguna. Conexiones: entregas. Estándares: CWE-321/CWE-327 (según implementación). Esfuerzo: medio

### TYA-SEG-016 — Tarjeta y efectivo admitidos en instrumentos de fondeo y cobros sin alcance PCI declarado
- Clase / Severidad / Confianza: SEG / S4 / probable
- Ubicaciones: `sql/10_tablas/10_billetera_custodia/instrumento_fondeo.sql:23`; `cobro_cuenta_por_cobrar.sql:16`; `sql/60_semillas/14-proveedores-externos.sql` (`TARJETA_ADQUIRENCIA`)
- Evidencia: `sql/10_tablas/10_billetera_custodia/instrumento_fondeo.sql:23` → `CONSTRAINT ck_instrumento_fondeo_tipo CHECK (tipo IN ('CUENTA_BANCARIA', 'QR_BANCARIO', 'TARJETA')),` · `CONSTRAINT ck_instrumento_fondeo_estado_verificacion CHECK (estado_verificacion IN ('PENDIENTE', 'RECHAZADO', 'VENCIDO', 'VERIFICADO'))` · `);`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: contradice 90.2.10 y R-NUEVA-1. Solución: retirar o declarar. Causa: modelo heredado. Relación: FLUJO-QR. Conexiones: nucleo-financiero. Estándares: regla 90.2.10. Esfuerzo: chico

### 5.4 ACOPLE, DEBIDO-PROCESO, HUECO, DEFECTO

### TYA-ACOPLE-001 — ERP, publicidad y los reportes de auditoría leen esquemas de otros servicios; con su propio rol fallan
- Clase / Severidad / Confianza: ACOPLE / S2 / confirmado (ejecutado)
- Ubicaciones: `servicios/erp/.../infraestructura/PeriodoRepositorio.java:149-174`; `.../PresupuestoRepositorio.java:113-115,129-160,172,193`; `servicios/publicidad/.../aplicacion/CU110AltaDeAnunciante.java:158,177`; `.../infraestructura/CreativaRepositorio.java:89-97`; `sql/60_semillas/19-reportes-y-retencion.sql:6-14` (SQL de reportes que ejecuta `servicios/auditoria/.../EjecutorDeConsulta.java`)
- Evidencia: base local: `SET ROLE svc_erp; SELECT count(*) FROM nucleo_financiero.asiento_contable` → `ERROR: permission denied for schema nucleo_financiero`; `SET ROLE svc_publicidad; ... organizador.organizador` → `permission denied for schema organizador`; `has_schema_privilege('svc_auditoria','nucleo_financiero','USAGE') = f` (`evidencia/db-verificacion-hallazgos-1.txt`, `-2.txt`). `scripts/auditar_backend.py` lo lista como "divergencias declaradas" (10).
- Cómo se comprobó: ejecución con los roles reales sembrados por `00_base`.
- Impacto: en producción fallan el cierre de periodo (CU-100), los estados financieros (CU-106), la ejecución presupuestaria, el alta de anunciante organizador y los reportes regulatorios que cruzan esquemas. Las pruebas no lo ven porque corren como dueño (TYA-PRUEBAS-001). Además viola 98.1.1.
- Causa de fondo: datos ajenos leídos por SQL en vez de pedidos por API o recibidos por evento.
- Solución de fondo: API de lectura de saldos del mayor en nucleo-financiero; proyecciones propias alimentadas por eventos para reportes.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: erp, publicidad, auditoria, nucleo-financiero (L1), organizador (L3).
- Estándares y normas: ISO/IEC 25010 Modularidad; reglas 98.1.1, 98.1.2; ADR-017.
- Esfuerzo estimado: grande

### TYA-ACOPLE-002 — Base compartida de hecho: 359 de 687 claves foráneas cruzan esquemas
- Clase / Severidad / Confianza: ACOPLE / S3 / probable (recuento por script del subagente)
- Ubicaciones: `sql/20_claves/*.sql`; `sql/00_base/02_esquemas.sql:4-5` ("Las claves foraneas entre esquemas se conservan")
- Evidencia: sin fragmento extraíble automáticamente de las ubicaciones citadas (ubicación de carpeta o patrón); ver descripción en Impacto.
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: ningún servicio puede migrar su base por separado; borrar en un servicio falla por restricciones de otro. Causa: el monolito se partió en esquemas sin cortar las claves. Solución: plan de corte por pares (cumplimiento→identidad 59, garantia→identidad 33, garantia→grupos 33, ...). Relación: ninguna. Conexiones: todos. Estándares: regla 98.1.2. Esfuerzo: grande

### TYA-DEBIDO-PROCESO-001 — La reputación la escribe soporte con datos del cuerpo y el puntaje anterior se borra
- Clase / Severidad / Confianza: DEBIDO-PROCESO / S2 / confirmado (permisos y `DELETE`); probable (cálculo desde el cuerpo, `CU71RecalcularPuntaje.java:87-96` no reabierto)
- Ubicaciones: `servicios/transparencia/.../web/ReputacionController.java:85,96,105,127,156,167`; `.../infraestructura/ReputacionRepositorio.java:189-201`; `.../aplicacion/CU71RecalcularPuntaje.java:87-96`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Evidencia: `@Permiso("SOPORTE")` en eventos, compensación, puntajes, snapshots e insignias; `dsl.deleteFrom(DSL.table(DSL.name("transparencia", "componente_score")))`, `deleteFrom(... "puntaje_reputacion")`. transparencia no tiene ningún `@KafkaListener` (grep de `topics =`).
- Impacto: una persona puede quedar con `FRAUDE_CONFIRMADO` o `SANCION_APLICADA` sin expediente `FIRME`; el historial que necesitaría una apelación desaparece. Contradice 91.4.2 ("la reputación es consecuencia del expediente").
- Causa de fondo: no hay integración por evento con el expediente de incumplimiento (garantia).
- Solución de fondo: eventos penalizantes solo con referencia a un expediente FIRME verificado con su dueño; puntaje derivado de `evento_reputacion`; puntaje histórico append-only.
- Relación con el flujo nuevo: ninguna (R-NUEVA-1 no elimina el debido proceso)
- Conexiones y módulos afectados: transparencia, garantia (L2), grupos.
- Estándares y normas: regla 91.4; ISO/IEC 25010 Responsabilidad (Accountability).
- Esfuerzo estimado: grande

### TYA-DEBIDO-PROCESO-002 — Los pares del grupo ven incumplimientos no firmes; no hay bitácora de lectura de perfiles ajenos
- Clase / Severidad / Confianza: DEBIDO-PROCESO / S3 / probable
- Ubicaciones: `servicios/transparencia/.../web/RutasDePerfilEnGrupo.java:58`; `.../aplicacion/ConsultarPerfilEnGrupo*`; `servicios/auditoria/.../aplicacion/ConsultarBitacora.java`
- Evidencia: `servicios/transparencia/src/main/java/bo/aportaya/transparencia/web/RutasDePerfilEnGrupo.java:58` → `.incumplimientosAbiertos(f.incumplimientosAbiertos())` · `.antiguedadMeses(f.antiguedadMeses()))` · `.orElseGet(() -> new bo.aportaya.transparencia.web.generado.modelo.PerfilDeReputacionEnGrupo(`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: exposición de presunciones como hechos; lecturas de datos financieros sin rastro (90.2.7). Solución: mostrar solo FIRME; registrar lecturas en `comun.registro_acceso_datos`. Causa: diseño de vista. Relación: ninguna. Conexiones: transparencia, auditoria. Estándares: reglas 91.4.1, 90.2.7. Esfuerzo: chico

### TYA-HUECO-001 — Notificaciones no envía nada: el despacho, los recordatorios y el lote no tienen punto de entrada
- Clase / Severidad / Confianza: HUECO / S2 / confirmado (grep)
- Ubicaciones: `servicios/notificaciones/.../aplicacion/CU80DespacharNotificacion.java:38,106`; `CU81ProgramarRecordatorios.java`; `CU83DespacharLote.java`; carpeta `trabajos/` inexistente; `servicios/notificaciones/descriptor.yml`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `grep -rn "CU80Despachar\|CU81Programar\|CU83Despachar"` fuera de los propios archivos → solo `infraestructura/simulado/MensajeriaSimulada.java` (importa un tipo); sin `@KafkaListener` ni `@Scheduled` en el servicio. `IDIOMA_POR_OMISION = "es"` mientras las 122 versiones sembradas usan `'es-BO'` (`15-eventos-y-plantillas.sql`).
- Impacto: ningún recordatorio de aporte por vencer, mora, entrega acreditada ni aviso de sanción llega a nadie. En el flujo nuevo sin aprobaciones, el recordatorio es el único empujón al pago por QR. El descriptor afirma "una caída se vuelve atraso, no aviso perdido", que hoy es falso. Aun cableado, CU80 no encontraría plantilla por el idioma.
- Causa de fondo: casos de uso escritos y probados en aislamiento, sin integrar (los tests pasan porque la fixture inserta `'es'`).
- Solución de fondo: consumidores de los eventos de negocio + trabajo programado con ShedLock; idioma `es-BO`.
- Relación con el flujo nuevo: FLUJO-QR (recordatorios de cobro)
- Conexiones y módulos afectados: notificaciones; productores aportes, entregas, garantia, grupos.
- Estándares y normas: ISO/IEC 25010 Completitud funcional; reglas 96.7, 00 §6.2.
- Esfuerzo estimado: medio

### TYA-HUECO-002 — Los eventos de dinero de tarifas, ERP y publicidad no los consume nadie: nunca llegan al mayor
- Clase / Severidad / Confianza: HUECO / S2 / confirmado (grep)
- Ubicaciones: publicadores en `servicios/tarifas`, `servicios/erp`, `servicios/publicidad` (outbox); consumidores existentes listados por `grep -rhoE "topics *= *..." servicios/*/src/main/java`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: los temas consumidos en todo el repo son `aportes.aporte_cobrado`, `entregas.derecho_cedido`, `grupos.grupo_creado` (×2), `grupos.grupo_iniciado`, `identidad.usuario_registrado`, `nucleo_financiero.aporte_aplicado|aporte_no_aplicado|transaccion_reversada`, `organizador.habilitacion_revocada`, `tarifas.tarifa_congelada`, `garantia.respaldo_*` y el patrón `*.indicador_calculado`. Ninguno de `tarifas.comision_*`, `erp.*`, `publicidad.*`.
- Impacto: comisiones cobradas, pagos a proveedores y facturación publicitaria quedan fuera de la partida doble (91.1.5); `factura_proveedor.asiento_contable_id` nunca se completa (y tiene `UPDATE` revocado); CU-35 compara contra un saldo que nunca incluye comisiones.
- Causa de fondo: el lado productor se escribió sin el consumidor.
- Solución de fondo: consumidores idempotentes en nucleo-financiero con asiento por evento y `UNIQUE (origen_tipo, origen_id)`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: tarifas, erp, publicidad, nucleo-financiero.
- Estándares y normas: reglas 91.1.5, 98.3.4.
- Esfuerzo estimado: medio

### TYA-HUECO-003 — Derechos del titular: se responde "se eliminaron todos sus datos" sin eliminar nada
- Clase / Severidad / Confianza: HUECO / S2 / confirmado (lectura + grep)
- Ubicaciones: `servicios/auditoria/.../aplicacion/CU07EjercerDerechos.java:113,121-144`; `.../web/AuditoriaController.java:139-141`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `UUID procesoId = anonimizaciones.planificar(...)`; `String estado = ... ? "ATENDIDA" : "PARCIAL"`; `return "Se eliminaron todos sus datos: ninguna retencion legal seguia vigente.";`. `grep anonimizacion_planificada` fuera de auditoria → sin consumidores.
- Impacto: respuesta falsa a una solicitud de supresión de datos personales; riesgo legal. La operación cuelga de un permiso de lectura (`DATOS_SENSIBLES_LEER`).
- Causa de fondo: el plan se registró como si fuera la ejecución.
- Solución de fondo: estado `EN_PROCESO` hasta que cada servicio confirme; ejecutores por servicio; permiso de escritura propio.
- Relación con el flujo nuevo: ninguna · Conexiones: todos los dueños de datos personales · Estándares: regla 90.2; norma de protección de datos a identificar por Cumplimiento · Esfuerzo: grande

### TYA-HUECO-004 — Reportes regulatorios (CU-58) nunca se entregan; el ejecutor "solo lectura" acepta un CTE con DELETE
- Clase / Severidad / Confianza: HUECO / S3 / probable
- Ubicaciones: `servicios/auditoria/.../aplicacion/CU58EjecutarReporte.java:153-177`; `.../web/ReportesController.java:121-127`; `.../infraestructura/EjecutorDeConsulta.java:47`
- Evidencia: `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/CU58EjecutarReporte.java:153` → `List<List<String>> filas = resultado.filas();` · `String huella = HuellaDeResultado.de(filas);` · `reportes.cerrarEjecucion(dsl, ejecucionId, COMPLETADA, filas.size(), resultado.duracionMs(), huella, ahora);`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: exportación que apunta a un archivo que no existe; descarga 204. Solución: escribir el archivo en almacenamiento; validar SQL con rol de solo lectura en la base, no por texto. Causa: implementación incompleta. Relación: ninguna. Conexiones: auditoria. Estándares: CWE-89 (defensa). Esfuerzo: medio

### TYA-HUECO-005 — Incidentes prometidos por `cadena_rota` y `sorteo_verificacion_fallida` nunca se crean
- Clase / Severidad / Confianza: HUECO / S3 / probable
- Ubicaciones: `servicios/transparencia/.../aplicacion/CU61*.java:99-101`; `CU73*.java:111-128`; auditoria consume solo `*.indicador_calculado`
- Evidencia: `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU61VerificarSorteo.java:99` → `outbox.emitir(`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: una cadena de transparencia rota no alerta a nadie. Solución: consumidor en auditoria. Causa: productor sin consumidor. Relación: ninguna. Conexiones: transparencia, auditoria. Estándares: regla 98.3. Esfuerzo: chico

### TYA-DEFECTO-001 — La verificación pública de certificados declara inválido todo certificado vigente
- Clase / Severidad / Confianza: DEFECTO / S2 / confirmado (lectura)
- Ubicaciones: `servicios/transparencia/.../web/VerificarController.java:41`; `.../aplicacion/CU75EmitirCertificado.java:194-199`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `cu75.verificarPublico(codigo, Map.of(), contexto())`; `contenidoPublicado == null ? null : CertificadoVerificable.hash(contenidoPublicado)` → con `Map.of()` (no nulo) se compara el hash de `{}` contra el del certificado.
- Impacto: `valido=false` con `estado=VIGENTE` en la página pública: los certificados no sirven. Las pruebas no lo ven: `CU75Test` pasa `null` y `VerificarControllerWebTest` mockea CU75.
- Causa de fondo: contrato interno ambiguo (mapa vacío vs. nulo).
- Solución de fondo: pasar `null` o separar "vigencia" de "cotejo de contenido".
- Relación con el flujo nuevo: ninguna · Conexiones: transparencia, apps · Estándares: ISO/IEC 25010 Corrección funcional · Esfuerzo: chico

### TYA-DEFECTO-002 — La comisión sembrada cobra 30 % en lugar de 0,30 %
- Clase / Severidad / Confianza: DEFECTO / S2 / confirmado (base local + lectura)
- Ubicaciones: `sql/60_semillas/04-tarifario.sql:16,46-48`; `servicios/tarifas/.../dominio/CalculoDeComision.java:118`; `servicios/tarifas/src/test/.../CU30Test.java:56`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal y ejecución en base local o Gradle (ver `evidencia/`)
- Evidencia: base sembrada: `COM_ENTREGA|PORCENTUAL|0.3000|10.00|50.00`, tramos `0.3500/0.3000/0.2500`; código `base.monto().multiply(concepto.valorPorcentual())`; la prueba usa `"0.0030"`.
- Impacto: con cualquier bolsa mayor a ~Bs 167 (sintético) se cobra el techo; los tramos tienen huecos en 3000,00 y 10000,00 exactos (`hasta` exclusivo y el siguiente desde `.01`). Además el parámetro no tiene procedencia (97.5.4).
- Causa de fondo: semilla y código interpretan distinto la unidad del porcentaje; la prueba cubre el código, no la semilla.
- Solución de fondo: unidad explícita en el catálogo (fracción) y prueba que lea la semilla real; tramos contiguos.
- Relación con el flujo nuevo: ninguna (la comisión es un cargo aparte, ADR-051 D-04)
- Conexiones y módulos afectados: tarifas, entregas.
- Estándares y normas: reglas 97.5.4, 91.1; ISO/IEC 25010 Corrección funcional.
- Esfuerzo estimado: chico

### TYA-DEFECTO-003 — CU-32 no puede facturar ninguna comisión real
- Clase / Severidad / Confianza: DEFECTO / S2 / confirmado (lectura)
- Ubicaciones: `servicios/tarifas/.../aplicacion/CU32EmitirFactura.java:110`; `.../dominio/EstadoDelDevengo.java:7-20`; `src/test/.../FixturaDeFacturacion.java:86`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `if (!"COBRADO".equals(devengo.estado()))`; el devengo es append-only y nace `DEVENGADO` o `EXONERADO`; solo la fixture inserta `COBRADO`.
- Impacto: ninguna factura electrónica de comisión se emite fuera de las pruebas.
- Causa de fondo: el estado derivado (`EstadoDelDevengo.derivar`) no se usa.
- Solución de fondo: derivar el estado de las filas de `cargo_comision`.
- Relación con el flujo nuevo: ninguna · Conexiones: tarifas · Estándares: ISO/IEC 25010 Corrección funcional; regla 80.2.2 · Esfuerzo: chico

### TYA-DEFECTO-004 — El balance general de CU-106 se rechaza siempre que el periodo tenga ingresos o gastos
- Clase / Severidad / Confianza: DEFECTO / S2 / probable (lectura delegada; falta ejecutar)
- Ubicaciones: `servicios/erp/.../aplicacion/CU106GenerarEstadoFinanciero.java:70-83`; `.../dominio/CuadreContable.java`
- Evidencia: `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/CU106GenerarEstadoFinanciero.java:70` → `var saldos = presupuestos.saldosDelPeriodo(dsl, periodoId).stream()` · `.map(s -> new CuadreContable.SaldoDeCuenta(s.codigo(), s.tipo(), s.saldo()))` · `.toList();`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: estados financieros imposibles de generar en un periodo real. Causa: exige ACTIVO = PASIVO + PATRIMONIO con movimientos del periodo, sin el resultado. Solución: incluir resultado del ejercicio y saldos acumulados. Relación: ninguna. Conexiones: erp. Estándares: ISO/IEC 25010 Corrección funcional. Esfuerzo: chico

### TYA-DEFECTO-005 — Ventana de envío y recordatorios en UTC, no en `America/La_Paz`
- Clase / Severidad / Confianza: DEFECTO / S3 / probable
- Ubicaciones: `servicios/notificaciones/.../dominio/VentanaDeEnvio.java` (`decidir`); `CU80DespacharNotificacion.java:75`; `CU81ProgramarRecordatorios.java:51`; `src/test/.../CU80Test.java:135`
- Evidencia: `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/VentanaDeEnvio.java:40` → `LocalTime hora = ahora.toLocalTime();`; `CU81ProgramarRecordatorios.java:51` → `LocalDate hoy = reloj.ahora().atOffset(ZoneOffset.UTC).toLocalDate();`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: la ventana 08:00–21:00 queda en 04:00–17:00 locales; recordatorios corridos un día tras las 20:00. La prueba codifica el error. Solución: `ZoneId` del calendario. Causa: `OffsetDateTime` UTC. Relación: FLUJO-QR (recordatorios). Conexiones: notificaciones. Estándares: ISO 8601. Esfuerzo: chico

### TYA-DEFECTO-006 — Métricas de riesgo sin umbral disparan alerta
- Clase / Severidad / Confianza: DEFECTO / S3 / probable
- Ubicaciones: `servicios/transparencia/.../web/MapeoDeTransparencia.java:51`; `.../dominio/SenalDeRiesgo.java:31-39`
- Evidencia: `servicios/transparencia/src/main/java/bo/aportaya/transparencia/web/MapeoDeTransparencia.java:51` → `m.getUmbral() == null ? BigDecimal.ZERO : new BigDecimal(m.getUmbral()),` · `Boolean.TRUE.equals(m.getMayorEsPeor())))` · `.toList();`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: alertas falsas persistidas (`en_alerta=true`). Solución: no mapear null a cero. Causa: mapeo. Relación: ninguna. Conexiones: transparencia. Estándares: ISO/IEC 25010 Corrección. Esfuerzo: chico

### TYA-DEFECTO-007 — Segmentos: la evaluación se detiene en el primer candidato sin dato
- Clase / Severidad / Confianza: DEFECTO / S3 / probable
- Ubicaciones: `servicios/tarifas/.../dominio/SegmentoAplicable.java:35-42`
- Evidencia: `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/SegmentoAplicable.java:35` → `List<Candidato> ordenados = candidatos.stream()` · `.sorted(Comparator.comparingInt(Candidato::prioridad))` · `.toList();`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: segmentos de menor prioridad nunca se evalúan. Solución: continuar con el siguiente. Causa: retorno temprano. Relación: ninguna. Conexiones: tarifas. Estándares: —. Esfuerzo: chico

### TYA-DEFECTO-008 — Puede emitirse más de una factura por devengo
- Clase / Severidad / Confianza: DEFECTO / S3 / probable
- Ubicaciones: `servicios/tarifas/.../aplicacion/CU32EmitirFactura.java` (chequeo antes del candado); `infraestructura/FacturaRepositorio.java:61`; `sql/30_indices/11_*.sql:163` (índice no único)
- Evidencia: `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/CU32EmitirFactura.java:106` → `if (facturas.facturaDe(dsl, devengo.id()).isPresent()) {` (chequeo previo; el candado se toma después según lectura delegada).
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: doble factura fiscal. Solución: `UNIQUE (devengo_id)` y chequeo bajo candado. Causa: TOCTOU. Relación: ninguna. Conexiones: tarifas. Estándares: CWE-367. Esfuerzo: chico

### TYA-DEFECTO-009 — Moderación de reseña informa el peso base, no el atenuado
- Clase / Severidad / Confianza: DEFECTO / S4 / confirmado
- Ubicaciones: `servicios/transparencia/.../aplicacion/CU76PublicarResena.java:176-190`
- Evidencia: `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU76PublicarResena.java:187-190` → `"pesoEnReputacion", pesoBaseDeResena.toPlainString()` · `return new SalidaResena(entrada.resenaId(), estado, pesoBaseDeResena, ...)`, mientras la publicación calcula `ModeracionDeResena.peso(pesoBaseDeResena, ...)` (línea 90).
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: dato informado incorrecto. Solución: devolver el peso efectivo. Causa: variable equivocada. Relación: ninguna. Conexiones: transparencia. Estándares: —. Esfuerzo: chico

### 5.5 RESILIENCIA, CONTRATO, TRAZA, OBSERV

### TYA-RESILIENCIA-001 — Consumidores Kafka: tras ~10 s de reintentos el mensaje se descarta (sin DLT ni buzón muerto)
- Clase / Severidad / Confianza: RESILIENCIA / S2 / confirmado (lectura + grep)
- Ubicaciones: `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/ConfiguracionMensajeria.java:55-65`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `var backoff = new ExponentialBackOff(1000L, 2.0); backoff.setMaxElapsedTime(10000L); return new DefaultErrorHandler(backoff);` — sin recoverer. El comentario dice "Evita que un fallo transitorio descarte eventos". `grep -rln "DeadLetterPublishingRecoverer\|CommonErrorHandler"` → solo este archivo.
- Impacto: una caída de base de más de 10 s hace que un consumidor comitee el offset y pierda el evento: `aporte_cobrado` sin aplicar al pozo, `aporte_no_aplicado` sin compensar. El comentario miente.
- Causa de fondo: se configuró reintento sin decidir el destino del fallo final.
- Solución de fondo: `DeadLetterPublishingRecoverer` a `<tema>.DLT` (o tabla de buzón) con alarma; backoff más largo para errores transitorios de base.
- Relación con el flujo nuevo: FLUJO-QR (el aporte cobrado viaja por Kafka)
- Conexiones y módulos afectados: todos los consumidores (aportes, nucleo-financiero, garantia, tarifas, grupos, auditoria...).
- Estándares y normas: ISO/IEC 25010 Tolerancia a fallos; reglas 98.3.3, 98.3.4, 96.7.
- Esfuerzo estimado: chico

### TYA-RESILIENCIA-002 — El relevo del outbox resucita los eventos FALLIDOS para siempre
- Clase / Severidad / Confianza: RESILIENCIA / S3 / probable (lectura delegada; sin prueba de `rescatarFallidos`)
- Ubicaciones: `plataforma/comun-mensajeria/.../Relevo.java:110-126,203`
- Evidencia: `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/Relevo.java:110` → `@Scheduled(fixedDelayString = "${aportaya.outbox.rescate-intervalo:PT5M}")` · `@SchedulerLock(name = "outbox.relevo.rescate", lockAtMostFor = "PT1M")` · `public void rescatarFallidos() {`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: un evento venenoso vuelve a PENDIENTE con `intentos=0` cada 5 min; la métrica de fallidos oscila a 0. Solución: tope de rescates y umbral por último fallo; prueba. Causa: diseño. Relación: ninguna. Conexiones: todos. Estándares: regla 98.3.3. Esfuerzo: chico

### TYA-RESILIENCIA-003 — Publicación Kafka sin `max.block.ms` ni idempotencia del productor
- Clase / Severidad / Confianza: RESILIENCIA / S3 / sospecha (comportamiento de librería no ejecutado)
- Ubicaciones: `plataforma/comun-mensajeria/.../Relevo.java:177-178`
- Evidencia: `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/Relevo.java:177` → `kafka.send(EnvelopeDeEvento.construir(evento, esquema))` · `.get(timeoutPublicacion.toMillis(), TimeUnit.MILLISECONDS);` · `marcarPublicado(id);`
- Cómo se comprobó: indicio por lectura delegada; falta ejecutar el comportamiento de la librería o confirmar referencias
- Impacto: con Kafka caído, `send()` puede bloquear 60 s por evento y superar `lockAtMostFor`. Solución: fijar `max.block.ms` y `enable.idempotence=true`. Causa: configuración por omisión. Relación: ninguna. Conexiones: todos. Estándares: regla 98.2.1. Esfuerzo: chico

### TYA-RESILIENCIA-004 — Despacho por lote llama al proveedor dentro de la transacción con `FOR UPDATE SKIP LOCKED`
- Clase / Severidad / Confianza: RESILIENCIA / S3 / probable
- Ubicaciones: `servicios/notificaciones/.../aplicacion/CU83DespacharLote.java:83-149`
- Evidencia: `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/aplicacion/CU83DespacharLote.java:83` → `return datos.conContexto(ctx, dsl -> {` · `List<EnCola> lote = envios.tomar(dsl, entrada.particion(), ahora, entrada.cuantos());` · `List<Detalle> detalles = new ArrayList<>();`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: un error tardío revierte mensajes ya enviados y se reenvían; `intentados` siempre vacío. Sin circuit breaker en los 7 servicios de apoyo salvo inversiones. Solución: efecto después del commit (96.3.4). Causa: transacción larga. Relación: ninguna. Conexiones: notificaciones. Estándares: reglas 96.3.4, 98.2.3. Esfuerzo: medio

### TYA-RESILIENCIA-005 — Cliente de MinIO sin timeouts
- Clase / Severidad / Confianza: RESILIENCIA / S4 / probable
- Ubicaciones: `plataforma/comun-archivos/.../AlmacenS3Adaptador.java:38-41`
- Evidencia: `plataforma/comun-archivos/src/main/java/bo/aportaya/plataforma/archivos/AlmacenS3Adaptador.java:38` → `public AlmacenS3Adaptador(String url, String usuario, String clave, String bucket) {` · `this.cliente =` · `MinioClient.builder().endpoint(url).credentials(usuario, clave).build();`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: hilos colgados en identidad. Solución: timeouts explícitos. Causa: omisión. Relación: ninguna. Conexiones: identidad. Estándares: regla 98.2.1. Esfuerzo: chico

### TYA-CONTRATO-001 — Errores: todo `ErrorDeDominio` sale como 422; `ResponseStatusException` como 500; formato no RFC 9457
- Clase / Severidad / Confianza: CONTRATO / S3 / confirmado (lectura de `ManejadorGlobalDeErrores.java:253-262`)
- Ubicaciones: `plataforma/comun-web/.../errores/ManejadorGlobalDeErrores.java:253-262`; `servicios/auditoria/.../web/AuditoriaController.java:88`; `plataforma/comun-datos/.../SinTransaccion.java:17`; `plataforma/comun-archivos/.../AlmacenS3Adaptador.java:59-130`
- Evidencia: `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/ManejadorGlobalDeErrores.java:253` → `@ExceptionHandler(ErrorDeDominio.class)` · `public ResponseEntity<ErrorApi> reglaDelDominio(ErrorDeDominio e) {` · `return ResponseEntity.unprocessableEntity().body(ErrorApi.de("AP-DOM-01", e.getMessage(), Traza.actual()));`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Impacto: bugs de servidor e infraestructura (MinIO caído, `SET LOCAL` sin transacción) llegan como 422 con mensaje interno; `ResponseStatusException(422)` llega como 500. Los clientes no pueden distinguir. Solución: subclases de infraestructura → 500/503; handler de `ErrorResponse`; registrar en ADR el formato (`ErrorApi` vs problem+json). Causa: un solo tipo. Relación: ninguna. Conexiones: todos. Estándares: RFC 9110, RFC 9457; regla 96.4.4. Esfuerzo: medio

### TYA-CONTRATO-002 — Orden por agregado prometido en el sobre del evento y no garantizado con fallos
- Clase / Severidad / Confianza: CONTRATO / S3 / probable
- Ubicaciones: `plataforma/comun-mensajeria/.../EnvelopeDeEvento.java:40-41`; `Relevo.java:101,228-233`
- Evidencia: `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/EnvelopeDeEvento.java:40` → `.setHeader(KafkaHeaders.KEY, agregadoId.toString())` · `.setHeader("event_id", id.toString())` · `.setHeader("type", tipo)`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: un evento 2 de un agregado se publica antes que el 1 en backoff. Solución: no tomar eventos con anterior pendiente del mismo agregado, o versionar. Causa: diseño del relevo. Relación: FLUJO-QR. Conexiones: todos los consumidores. Estándares: regla 98.3. Esfuerzo: chico

### TYA-CONTRATO-003 — Verificación pública del sorteo siempre "no verifica" con HTTP 200
- Clase / Severidad / Confianza: CONTRATO / S3 / probable
- Ubicaciones: `servicios/transparencia/.../web/PublicoController.java:79-117`; `.../dominio/puertos/PaquetesDeSorteo.java:13-19`
- Evidencia: `servicios/transparencia/src/main/java/bo/aportaya/transparencia/web/PublicoController.java:79` → `var paquete = paquetes.de(sorteoId);` · `if (paquete.isEmpty()) {` · `return ResponseEntity.ok(sinPaquete());`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: lectura pública que sugiere sorteo no limpio; la ruta de grupos exige sesión. Solución: estado explícito "no disponible" (503 o campo) y contrato público del paquete. Causa: dependencia autenticada. Relación: ninguna. Conexiones: grupos (L3). Estándares: RFC 9110. Esfuerzo: medio

### TYA-CONTRATO-004 — grupos recibe "sin historial" cuando transparencia le niega el puntaje ajeno
- Clase / Severidad / Confianza: CONTRATO / S3 / probable
- Ubicaciones: `servicios/grupos/.../HechosPorHttp.java:192-197` (contacto); consecuencia de TYA-SEG-004
- Evidencia: `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/HechosPorHttp.java:192` → `public Reputacion reputacion(UUID usuarioId) {` · `return transparencia` · `.consultar("/reputacion/" + usuarioId + "/puntaje", Puntaje.class)`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: decisiones de grupos con reputación falsa en cero. Solución: usar `/etapas/de-personas` (ya existe) y propagar 403. Causa: contrato cambiado sin versión. Relación: ninguna. Conexiones: grupos (L3), transparencia. Estándares: regla 98.1.3. Esfuerzo: chico

### TYA-CONTRATO-005 — Pactos: lado productor sin prueba en tres casos, campos opcionales con valor por omisión de dinero y patrón que admite negativos
- Clase / Severidad / Confianza: CONTRATO / S3 / probable (lectura delegada de los 10 pactos)
- Ubicaciones: `contratos/pactos/identidad-grupos-pertenencia.json`, `transparencia-grupos-pertenencia.json` (sin prueba en grupos); `nucleo-financiero-entregas.json` (sin prueba en entregas); `aportes-nucleo-financiero-aporte-cobrado.json` (`tipo` opcional: "sin él, va al pozo"); `nucleo-financiero-garantia-aporte-aplicado.json` (`destino` opcional → GRUPO); `inversiones-nucleo-financiero.json` vs `nucleo-financiero.yaml:1501`; patrón `^-?\d+\.\d{2}$` en `entregas.yaml:1017`, `aportes.yaml:566-568`, `auditoria.yaml`
- Evidencia: `contratos/pactos/` contiene 10 pactos en formato propio (`productor`, `consumidor`, `campos`, `interacciones`); para `identidad-grupos-pertenencia.json`, `transparencia-grupos-pertenencia.json` y `nucleo-financiero-entregas.json` no hay prueba que los cite en el `src/test` del productor (búsqueda delegada).
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: cambios del productor que rompen al consumidor sin que CI falle (98.1.4); un campo omitido manda dinero al pozo por defecto. Los servicios del lote no son productores en ningún pacto. Solución: pruebas de productor; campos de destino de dinero obligatorios; patrón sin signo. Causa: formato de pacto propio sin verificador del productor. Relación: FLUJO-QR. Conexiones: aportes, nucleo, garantia, entregas, grupos, inversiones. Estándares: regla 98.1.4; OpenAPI 3. Esfuerzo: medio

### TYA-TRAZA-001 — Trazabilidad incompleta: interceptor Kafka probablemente sin cablear, `causation_id` nunca escrito, versión fija
- Clase / Severidad / Confianza: TRAZA / S3 / sospecha (cableado del interceptor no ejecutado); confirmado `version` fija según lectura delegada
- Ubicaciones: `plataforma/comun-mensajeria/.../ConfiguracionMensajeria.java:48-52`; `Outbox.java:41-66,57`; `EnvelopeDeEvento.java:44`; `servicios/inversiones/.../LibroPorHttp.java:166-173` (sin cabecera de correlación)
- Evidencia: `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/ConfiguracionMensajeria.java:48` → `@Bean` · `@ConditionalOnMissingBean` · `public InterceptorDeTrazaKafka interceptorDeTrazaKafka() {`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal y ejecución en base local o Gradle (ver `evidencia/`)
- Impacto: la traza de un aporte se corta entre servicios (98.5.1). Solución: prueba de contexto con listener real; `causation_id`; propagar correlación en clientes HTTP. Causa: piezas probadas aisladas. Relación: FLUJO-QR. Conexiones: todos. Estándares: regla 98.5. Esfuerzo: medio

### TYA-OBSERV-001 — Logs: JSON armado a mano, excepciones sin máscara de PII, sin `correlationId`/`version`/`actorId`
- Clase / Severidad / Confianza: OBSERV / S3 / probable
- Ubicaciones: `plataforma/comun-web/src/main/resources/logback-spring.xml:19`; `.../observabilidad/EnmascaradorPii.java:20-28`; `.../traza/Traza.java:62-69`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Evidencia (delegada): `"mensaje":"%maskPii(%msg)","excepcion":"%replace(%xEx){'\n','\t'}"`; patrones de CI de 10+ dígitos (el CI boliviano tiene 7-8).
- Impacto: registros inválidos, datos personales en trazas de excepción, sin campos exigidos por 98.5.2. Solución: logging estructurado de Spring Boot y máscara sobre excepción y MDC. Causa: formato artesanal. Relación: ninguna. Conexiones: todos. Estándares: reglas 90.2.1, 98.5.2. Esfuerzo: medio

### TYA-OBSERV-002 — Detalles de observabilidad y configuración menores
- Clase / Severidad / Confianza: OBSERV / S4 / probable
- Ubicaciones: `plataforma/comun-mensajeria/.../PublicacionDeIndicadores.java:102-107`; `plataforma/comun-datos/.../SaludDeLaBase.java:28`; `plataforma/comun-archivos/.../DestinoDeObjeto.java:71`; `plataforma/gateway/src/main/resources/application.yml:23-35`; `plataforma/gateway/src/main/resources/rutas.yml:107-112` (`/sesion/refrescar` sin rate limit)
- Evidencia: `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/PublicacionDeIndicadores.java:102` → `} catch (RuntimeException fallo) {` · `LOG.warn(` · `"indicadores del periodo {} no publicados: {}",`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: diagnóstico pobre, reloj no inyectado. Solución: ordenar. Causa: deuda. Relación: ninguna. Conexiones: varios. Estándares: reglas 96.8, 90.1.14. Esfuerzo: chico

### 5.6 DATOS

### TYA-DATOS-001 — Datos personales de una persona real y hashes de contraseña crackeables en una semilla versionada
- Clase / Severidad / Confianza: DATOS / S2 / confirmado (lectura; valores no reproducidos)
- Ubicaciones: `sql/61_dev/15-usuarios-dev.sql:1,8-9,14-20` (y su fuente `seeders/dev/15-usuarios-dev.json`); hashes también en `sql/61_dev/05-*.sql:20-22`, `sql/61_dev/14-*.sql:107-108`, `sql/62_volumen/volumen-*.sql` (líneas ~1372-1379)
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: comentario "Las dos cuentas de desarrollo con contraseña real"; nombre, apellidos, fecha de nacimiento y correo de una persona identificable (no se copian aquí); "Argon2id real ... Sal fija a propósito"; una de las cuentas entra al backoffice.
- Impacto: dato personal real en el repositorio (97.6.1, 00 §2.3); el hash con sal fija se puede atacar offline y, si la contraseña se reutiliza en TEST, da acceso de plataforma.
- Causa de fondo: comodidad de desarrollo.
- Solución de fondo: datos sintéticos; rotar la contraseña; evaluar purga del historial (90.3.5).
- Relación con el flujo nuevo: ninguna · Conexiones: identidad · Estándares: regla 97.6.1, 90.3.1; CWE-359, CWE-916 · Esfuerzo: chico

### TYA-DATOS-002 — Borrador de alta con correo como clave primaria y número de documento en claro, sin RLS
- Clase / Severidad / Confianza: DATOS / S2 / confirmado (lectura + base)
- Ubicaciones: `sql/15_infra/borradores.sql:5-16,51`; `sql/10_tablas/03_aportes_pagos_qr/pago.sql:20` (`pagador_documento`)
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal y ejecución en base local o Gradle (ver `evidencia/`)
- Evidencia: `correo VARCHAR(150) PRIMARY KEY, ... numero_documento VARCHAR(30)`; `relrowsecurity = f`; `monto_mensual_estimado TEXT` (dinero como texto, sin moneda).
- Impacto: contradice R-SEG-01 (hash con pimienta + cifrado); PII como identificador. Solución: llave sustituta, documento cifrado y hasheado, RLS. Causa: tabla de infraestructura escrita a mano fuera del modelo. Relación: ninguna. Conexiones: identidad (L4). Estándares: regla 90.2.9, 97.1.3. Esfuerzo: chico

### TYA-DATOS-003 — Parámetros financieros sembrados sin procedencia y hash de documento falso
- Clase / Severidad / Confianza: DATOS / S2 / probable (las líneas de tarifario confirmadas en la base; el resto por lectura delegada)
- Ubicaciones: `sql/60_semillas/02-politicas.sql:18-19,27-28`; `04-tarifario.sql:16` (`hash_documento = repeat('0',64)`); `14-proveedores-externos.sql:5-11`; `09-reglas-operativas.sql`; `12-calendario-habil.sql`
- Evidencia: `sql/60_semillas/02-politicas.sql:18` → `IF NOT EXISTS (SELECT 1 FROM politica_mora) THEN` · `INSERT INTO politica_mora (dias_gracia, tipo_recargo, valor_recargo, tope_recargo, dias_para_mora_grave, dias_para_incumplimiento, aplica_au` · `(3, 'PORCENTUAL', 2.0, 100.0, 15, 30, TRUE, now());`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: mora 2 %, cobertura 5 %, topes y comisiones de proveedores presentados como catálogo sin fuente, fecha ni responsable (97.4.2, 97.5.4). Solución: columnas de procedencia y estado `PROVISIONAL`. Causa: semillas escritas para simular. Relación: RESPALDO (política de cobertura). Conexiones: aportes, garantia, tarifas. Estándares: reglas 97.4.2, 97.5.4. Esfuerzo: medio

### TYA-DATOS-004 — Catálogo operativo paralelo en `auditoria`: valores divergentes, cambios solo en memoria por réplica, errores tragados
- Clase / Severidad / Confianza: DATOS / S2 / confirmado (mapa en memoria y `catch`); probable (divergencia de valores)
- Ubicaciones: `servicios/auditoria/src/main/resources/catalogo/catalogos-operativos.json`; `servicios/auditoria/.../infraestructura/CatalogosOperativosRepositorio.java:40,83,111,181-229`; `.../aplicacion/GestionarCatalogosOperativos.java:47-49`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Evidencia: `new ConcurrentHashMap<>()` como almacén; `} catch (Exception e) {` en la carga; JSON de bitácora concatenado a mano.
- Impacto: un operador "desactiva" un elemento: el cambio vive en una réplica hasta reiniciar, pero la bitácora lo registra como real; umbrales UIF mostrados distintos de `seeders/minimos/06-umbrales-uif.json`.
- Causa de fondo: catálogo duplicado en vez de leído de su dueño (97.4.6).
- Solución de fondo: leer los catálogos de sus servicios dueños por API; borrar el JSON.
- Relación con el flujo nuevo: ninguna · Conexiones: auditoria, cumplimiento · Estándares: reglas 97.4.1, 97.4.6 · Esfuerzo: medio

### TYA-DATOS-005 — Columnas de dinero tomadas del cuerpo y sin moneda en transparencia; CUF de nota de crédito con `hashCode`
- Clase / Severidad / Confianza: DATOS / S3 / probable
- Ubicaciones: `servicios/transparencia/.../infraestructura/ReputacionRepositorio.java:234`; `servicios/tarifas/.../ConfiguracionDeTarifas.java:27-31`
- Evidencia: `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/ReputacionRepositorio.java:234` → `.set(DSL.field("monto_total_aportado", BigDecimal.class), indicadores.montoAportado())` · `.set(DSL.field("grupos_completados", Short.class), (short) indicadores.gruposCompletados())` · `.set(DSL.field("grupos_abandonados", Short.class), (short) indicadores.gruposAbandonados())`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: `monto_total_aportado` sin moneda y escrito por soporte; CUF no único. Solución: derivar del mayor; CUF del SIN. Causa: atajos. Relación: ninguna. Conexiones: transparencia, tarifas. Estándares: regla 91.1.2. Esfuerzo: chico

### TYA-DATOS-006 — Semilla de impuestos no idempotente entre días
- Clase / Severidad / Confianza: DATOS / S3 / confirmado (lectura)
- Ubicaciones: `sql/60_semillas/05-impuestos.sql:5-7`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Evidencia: `vigente_desde = current_date` con `ON CONFLICT (codigo, vigente_desde) DO NOTHING`.
- Impacto: correr la semilla otro día agrega una segunda fila vigente de IVA e IT. Mi prueba de idempotencia (misma fecha) pasó y por eso no lo detecta. Solución: fecha fija con procedencia. Causa: fecha dinámica. Relación: ninguna. Conexiones: tarifas. Estándares: regla 97.4.3. Esfuerzo: chico

### TYA-DATOS-007 — Sin verificación de deriva; el esquema solo se puede aflojar
- Clase / Severidad / Confianza: DATOS / S3 / confirmado (grep: las únicas coincidencias de "deriv" en `sql/50_verificacion` son de saldos derivados)
- Ubicaciones: `sql/50_verificacion/`; `sql/15_infra/nulabilidad.sql` (811 `DROP NOT NULL`); `CREATE TABLE IF NOT EXISTS` en `10_tablas`
- Evidencia: `grep -rniE "deriva|drift|information_schema.columns" sql/50_verificacion` → 6 coincidencias, todas sobre saldos *derivados* (`prueba_humo.sql:80,185,187`, `verificaciones.sql:103,107,109`); ninguna compara esquema contra modelo.
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Impacto: una base existente no recibe endurecimientos ni nuevos `CHECK`; nadie lo detecta (97.2 exige la verificación). Solución: verificación de deriva contra el modelo en CI y al aplicar. Causa: mecanismo de ADR-032 aún sin `migraciones/`. Relación: ninguna. Conexiones: todos. Estándares: regla 97.2. Esfuerzo: medio

### TYA-DATOS-008 — Tablas de evidencia de dinero y reputación sin sello append-only
- Clase / Severidad / Confianza: DATOS / S2 / confirmado (base: `pg_trigger` sin `*_append_only` en `aportes.pago`, `aportes.webhook_pasarela`, `entregas.deduccion_entrega`, `transparencia.snapshot_reputacion`, `transparencia.puntaje_reputacion`)
- Ubicaciones: `sql/35_append_only/append_only.sql`; `sql/00_base/03_permisos.sql:118-187`
- Evidencia: `sql/00_base/03_permisos.sql:118` → `REVOKE UPDATE, DELETE ON comun.bitacora_evento FROM svc_identidad;` · `REVOKE UPDATE, DELETE ON comun.bitacora_evento FROM svc_inversiones;` · `REVOKE UPDATE, DELETE ON comun.bitacora_evento FROM svc_notificaciones;`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal y ejecución en base local o Gradle (ver `evidencia/`)
- Impacto: la evidencia del pago (payload y firma del webhook) y las deducciones se pueden editar; el puntaje se borra (TYA-DEBIDO-PROCESO-001). Solución: sellar o justificar en el modelo. Causa: lista manual. Relación: FLUJO-QR. Conexiones: aportes, entregas, transparencia. Estándares: regla 91.1.3. Esfuerzo: chico

### TYA-DATOS-009 — Unas 55 columnas `_id` sin clave foránea dentro del mismo esquema
- Clase / Severidad / Confianza: DATOS / S3 / probable (script del subagente)
- Ubicaciones: `grupos.alta_grupo.grupo_id/creador_id`, `grupos.decision_ingreso.solicitud_id`, `organizador.decision_habilitacion.*`, `entregas.fondeo_entrega.cobertura_respaldo_id`, entre otras
- Evidencia: sin fragmento extraíble automáticamente de las ubicaciones citadas (ubicación de carpeta o patrón); ver descripción en Impacto.
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: huérfanos posibles. Solución: FKs en el modelo. Causa: omisión. Relación: ninguna. Conexiones: grupos, organizador, entregas. Estándares: regla 97.5.1. Esfuerzo: chico

### TYA-DATOS-010 — Unas 55 tablas sin uso en ningún servicio, incluida la del payload del QR
- Clase / Severidad / Confianza: DATOS / S3 / sospecha (búsqueda heurística por nombre)
- Ubicaciones: `aportes.qr_cobro`, `comprobante_manual`, `constancia_pago`, `enlace_pago_rapido`, `plan_regularizacion`, `castigo_deuda`, `acuerdo_quita`, `promesa_pago`, `reporte_operacion_sospechosa`, `alerta_cumplimiento`, `regla_antifraude`, `expediente_cliente`, `matriz_riesgo_lft`
- Evidencia: sin fragmento extraíble automáticamente de las ubicaciones citadas (ubicación de carpeta o patrón); ver descripción en Impacto.
- Cómo se comprobó: indicio por lectura delegada; falta ejecutar el comportamiento de la librería o confirmar referencias
- Impacto: esquema que promete capacidades inexistentes (QR sin tabla en uso). Solución: confirmar por servicio y retirar o implementar. Causa: modelo adelantado al código. Relación: FLUJO-QR. Conexiones: aportes, cumplimiento, garantia. Estándares: regla 97.1. Esfuerzo: medio

### TYA-RENDIMIENTO-001 — El saldo de cada cuenta se recalcula sumando toda su historia en cada movimiento
- Clase / Severidad / Confianza: RENDIMIENTO / S3 / probable
- Ubicaciones: `sql/40_reglas/restricciones.sql:3003-3016` (`fn_ctb_recalcular_saldo`); `fn_bil_recalcular_saldos`
- Evidencia: `sql/40_reglas/restricciones.sql:3003` → `CREATE OR REPLACE FUNCTION fn_ctb_recalcular_saldo(p_cuenta UUID) RETURNS VOID AS $$` · `DECLARE v_saldo NUMERIC(16,2); v_naturaleza TEXT;` · `BEGIN`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: O(n) por movimiento bajo candado en cuentas calientes (pozo, custodia). Solución: saldo incremental con verificación periódica. Causa: simplicidad. Relación: FLUJO-QR. Conexiones: nucleo-financiero. Estándares: ISO/IEC 25010 Eficiencia. Esfuerzo: medio

### 5.7 FRAGIL, DUP, ARQ, CLEAN, CONFIG, PRUEBAS, DOC, REG, COHERENCIA

### TYA-FRAGIL-001 — Una traza no-UUID tumba con 500 cualquier caso de uso que la convierta
- Clase / Severidad / Confianza: FRAGIL / S3 / probable (el conteo de 255 sitios es del subagente; mitigado hoy por NGINX)
- Ubicaciones: `plataforma/comun-web/.../traza/Traza.java:43-57`; usos `UUID.fromString(ctx.traza().id())` (p. ej. `servicios/transparencia/.../CU76PublicarResena.java:148,188`); `despliegue/nginx/aportaya.conf:69`
- Evidencia: `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/traza/Traza.java:43` → `private static String normalizar(String valor) {` · `if (valor == null || valor.isBlank()) {` · `return UUID.randomUUID().toString();`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: rutas públicas (CU-61, CU-73) responden 500 si alguien llega sin pasar por NGINX o con `sin-traza`. Si cambia NGINX, se rompe todo lejos y ninguna prueba lo avisa. Solución: normalizar a UUID en `Traza`. Causa: tipo `String` para un identificador. Relación: ninguna. Conexiones: todos. Estándares: CWE-20. Esfuerzo: chico

### TYA-FRAGIL-002 — Detección de duplicados por mensaje de excepción o por cualquier violación de integridad
- Clase / Severidad / Confianza: FRAGIL / S3 / probable
- Ubicaciones: `servicios/erp/.../aplicacion/CU103FacturaDeProveedor.java:123-130`; `servicios/inversiones/.../RescateRepositorio.java:65-70`; `servicios/inversiones/.../LibroPorHttp.java:228-237` (todo no-2xx → reintento)
- Evidencia: `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/CU103FacturaDeProveedor.java:123` → `} catch (org.jooq.exception.IntegrityConstraintViolationException` · `| org.springframework.dao.DataIntegrityViolationException e) {`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: un FK roto se informa como "factura ya registrada"; un cambio de texto del driver rompe el rescate; 400/403/409 se reintentan 12 veces. Solución: SQLSTATE y nombre de restricción; clasificar 4xx. Causa: atajos. Relación: ninguna. Conexiones: erp, inversiones, nucleo. Estándares: regla 96.3.5. Esfuerzo: chico

### TYA-DUP-X-001 — Circuit breaker copiado en cinco servicios e idempotencia reimplementada, mientras plataforma no los ofrece o no se usan
- Clase / Severidad / Confianza: DUP-X / S3 / confirmado (diff ejecutado) para el circuito; probable para el resto
- Ubicaciones: `servicios/{aportes/.../infraestructura/clientes,entregas/.../infraestructura,garantia/.../infraestructura,organizador/.../infraestructura}/ClienteConCircuito.java`, variante en `servicios/grupos`; `servicios/nucleo-financiero/.../RegistroDeIdempotencia.java` (misma tabla `respuesta_idempotente`, otra semántica); `record Importe(String monto, String moneda)` en 7 clientes HTTP; hex de SHA-256 en ~12 archivos de los servicios del lote
- Evidencia: `wc -l` → 128 líneas en `servicios/{aportes/.../infraestructura/clientes,entregas/.../infraestructura,garantia/.../infraestructura,organizador/.../infraestructura}/ClienteConCircuito.java` y 141 en `grupos`; `diff` ignorando `package` e imports propios → 0 líneas distintas entre las cuatro primeras, 13 con la de grupos (ejecutado por la sesión principal).
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: un arreglo en una copia no llega a las otras; dos semánticas de idempotencia sobre la misma tabla. Versión única: `plataforma/comun-web` (`ClienteDeServicio` con circuito; `Idempotencia`), `plataforma/comun-dominio` (hash). Causa: agentes en paralelo. Relación: ninguna. Conexiones: 6 servicios. Estándares: ISO/IEC 25010 Mantenibilidad (Reusabilidad). Esfuerzo: medio

### TYA-ARQ-001 — El gateway publica a internet operaciones pensadas entre servicios (incluida la cobertura con respaldo)
- Clase / Severidad / Confianza: ARQ / S3 / confirmado (`rutas.yml` enruta prefijos completos: `/api/v1/garantia/**`, `/api/v1/comisiones/**`)
- Ubicaciones: `plataforma/gateway/src/main/resources/rutas.yml:65-154` (garantia 95-100, tarifas 146); contacto `servicios/entregas/.../RespaldoPorHttp.java:55`, `servicios/garantia/.../GarantiaController.java:172`
- Evidencia: `plataforma/gateway/src/main/resources/rutas.yml:65` → `- id: aportes` · `uri: http://aportes:8080` · `predicates:`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Impacto: `POST /garantia/respaldo/coberturas` (la empresa cubre el faltante) y `/comisiones/devengos/{id}/cobros` (TYA-APROB-001) quedan alcanzables desde afuera, protegidos solo por RBAC de usuario (98.6.4). Solución: lista de rutas internas excluidas del borde y credencial de servicio con alcance. Causa: enrutado por prefijo. Relación: RESPALDO, APROB. Conexiones: garantia (L2), entregas, tarifas. Estándares: regla 98.6.4; OWASP API9:2023. Esfuerzo: chico

### TYA-ARQ-002 — Capas cruzadas: dominio que importa aplicación, aplicación con jOOQ/SQL y modelos web
- Clase / Severidad / Confianza: ARQ / S4 / probable
- Ubicaciones: `servicios/transparencia/.../dominio/puertos/PaquetesDeSorteo.java:3`; `servicios/notificaciones/.../aplicacion/GestionarAvisosInternos*`; `servicios/auditoria/.../aplicacion/ConsultarBitacora.java:5`, `GestionarCatalogosOperativos.java:4-6`; `servicios/tarifas/.../aplicacion/CU34PublicarTarifario.java:175-180`; `servicios/publicidad/.../aplicacion/CU110AltaDeAnunciante.java:177`
- Evidencia: `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/puertos/PaquetesDeSorteo.java:3` → `import bo.aportaya.transparencia.aplicacion.CU61VerificarSorteo.PaqueteDeSorteo;` · `import java.util.Optional;` · `import java.util.UUID;`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: cada cambio de modelo web o esquema toca la aplicación. Solución: mover a adaptadores (ADR-033). Causa: atajos. Relación: ninguna. Conexiones: 4 servicios. Estándares: ADR-033. Esfuerzo: medio

### TYA-CLEAN-001 — Patrón: excepciones tragadas, umbrales literales, valores por omisión silenciosos, JSON a mano, código muerto
- Clase / Severidad / Confianza: CLEAN / S4 / probable (patrón; ubicaciones de lectura delegada, muestra confirmada en `CatalogosOperativosRepositorio.java:111`)
- Ubicaciones: tragadas: `servicios/notificaciones/.../GestionarAvisosInternos.java:81`, `servicios/auditoria/.../ConsultarBitacora.java:100`, `CatalogosOperativosRepositorio.java:111`; literales: `servicios/transparencia/.../CriterioDeInsignia.java:33-89`, `CU70*:79`, `servicios/notificaciones/.../CU81*:129`; `@Value(...:valor)` en los 7 servicios (34 casos según `scripts/auditar_backend.py`, sección Cableado); JSON a mano sin escape completo: `servicios/tarifas/.../CU30*:191-211`, `CU36*:108-123`, `CatalogosOperativosRepositorio.java:208-211`; muerto: `plataforma/comun-web/.../CatalogoDeErrores.java`, `plataforma/comun-dominio/.../Prorrateo.java` (0 usos), `AmbitoArchivo.REPORTE`, `servicios/notificaciones/.../EnvioRepositorio` (varios métodos), `servicios/tarifas/.../DevengoRepositorio.registrarImpuesto` (`calculo_impuesto` nunca se escribe), `servicios/auditoria/src/main/resources/catalogo/catalogo-operaciones.json` (1,6 MB sin lector Java)
- Evidencia: `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/aplicacion/GestionarAvisosInternos.java:81` → `} catch (Exception e) {` · `tipo = AvisoInterno.TipoEnum.INFO;` · `}`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: errores ocultos y deuda. Solución: ordenar por servicio. Causa: trabajo en paralelo. Relación: ninguna. Conexiones: varios. Estándares: ISO/IEC 25010 Analizabilidad; regla 90.3.4. Esfuerzo: medio

### TYA-CONFIG-001 — `servicios/inversiones` no tiene `gradle.lockfile`
- Clase / Severidad / Confianza: CONFIG / S3 / confirmado
- Ubicaciones: `servicios/inversiones/` (14 de 15 servicios tienen lockfile; `buildSrc/src/main/kotlin/aportaya.servicio.gradle.kts:19-20` declara `lockAllConfigurations()`)
- Evidencia: `buildSrc/src/main/kotlin/aportaya.servicio.gradle.kts:19` → `dependencyLocking {` · `lockAllConfigurations()` · `}`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Impacto: dependencias del servicio que mueve inversiones sin fijar (90.4.1). Solución: generar y commitear. Causa: omisión. Relación: ninguna. Conexiones: inversiones. Estándares: regla 90.4.1. Esfuerzo: chico

### TYA-CONFIG-002 — Nivel de criticidad de transparencia menor que el de su dependiente síncrono
- Clase / Severidad / Confianza: CONFIG / S3 / probable
- Ubicaciones: `servicios/transparencia/descriptor.yml` (`nivel: N3`); grupos (N2) la llama síncronamente y falla cerrado
- Evidencia: `servicios/transparencia/descriptor.yml:9-10` → `nivel: N3` · `nivel_porque: "Reputación y certificados: nadie afuera nota diez minutos"`; `servicios/grupos/descriptor.yml:9` → `nivel: N2`.
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: despliegue con menor disponibilidad que la que exige su consumidor (ADR-037). Solución: subir a N2 o desacoplar. Causa: descriptor desactualizado. Relación: ninguna. Conexiones: grupos. Estándares: ADR-037. Esfuerzo: chico

### TYA-PRUEBAS-001 — Pruebas que no pueden ver los defectos: corren como dueño de la base y con fixtures que codifican el error
- Clase / Severidad / Confianza: PRUEBAS / S2 / confirmado parcial (ejecución: el rol de servicio falla donde la prueba pasa como dueño, TYA-ACOPLE-001; fixtures leídas: `FixturaDeNotificaciones.java:27,89` con `'es'`, `FixturaDeFacturacion.java:86` con `COBRADO`, `CU80Test.java:135`)
- Ubicaciones: `plataforma/comun-pruebas` (base de pruebas), `plataforma/comun-datos/src/test/.../AppendOnlyRepositorioTest.java`; fixtures citadas; `servicios/transparencia/src/test/.../CU75Test.java:73-120`, `VerificarControllerWebTest` (mockea CU75)
- Evidencia: `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU75Test.java:73` → `t -> certificadoCU.verificarPublico("AY-ZZZZ-ZZZZ-ZZZZ-ZZZZ", null, contextoDeSistema()));` · `var revocado = transaccion.execute(` · `t -> certificadoCU.verificarPublico(emitido.codigoVerificacion(), null, contextoDeSistema()));`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Impacto: integración, permisos, append-only en el rol real, cierre de ERP, facturación y despacho de notificaciones fallan en producción con la suite en verde. `rescatarFallidos` y el orden por agregado no tienen prueba.
- Causa de fondo: las pruebas de integración se escriben contra el dueño del esquema y contra datos armados a mano.
- Solución de fondo: conexión de pruebas con el rol `svc_<servicio>`; fixtures que usen las semillas reales.
- Relación con el flujo nuevo: ninguna · Conexiones: los 7 servicios y plataforma · Estándares: reglas 80.2.4, 80.5.4 · Esfuerzo: medio

### TYA-PRUEBAS-002 — La suite de integración no corre sin Docker y ninguna prueba de los servicios del lote se ejecutó antes de esta auditoría
- Clase / Severidad / Confianza: PRUEBAS / S3 / confirmado (ejecutado)
- Ubicaciones: `evidencia/g-resumen-servicios.txt`; `scripts/auditar_backend.py` ("0/393 clases de prueba corrieron y pasaron")
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal y ejecución en base local o Gradle (ver `evidencia/`)
- Evidencia: en esta sesión: unitarios, web, contrato y barrido en verde en los 7 servicios (p. ej. auditoria `webTest` 82/82); integración 23+12+18+14+136+23+16 fallos, todos por `Could not find a valid Docker environment` o el contexto que no levanta por esa causa.
- Impacto: la capa que prueba persistencia y permisos no tiene evidencia de ejecución. Solución: correr la integración en CI y guardar los informes. Causa: ENVIRONMENT. Relación: ninguna. Conexiones: todos. Estándares: regla 80.1. Esfuerzo: chico

### TYA-DOC-001 — Documentación que describe lo que el código no hace
- Clase / Severidad / Confianza: DOC / S4 / probable (muestra confirmada: `aplicar.sql:11` "Los 307 nombres de tabla son unicos", y `evento_dominio`, `shedlock` existen en cada esquema)
- Ubicaciones: `sql/README.md:58-72,101-109,161` (274 tablas, 565 FKs, 2 políticas RLS, nombres de archivo inexistentes; la base tiene 447 relaciones y 103 con RLS); `sql/aplicar.sql:11`; `plataforma/gateway/src/main/resources/application-production.yml:3-6` (clases inexistentes); `plataforma/comun-dominio/.../Indicador.java:8-9` (otro producto); `servicios/erp/README.md` (asientos "se completan cuando nucleo responde"); `servicios/notificaciones/descriptor.yml` (promesa de outbox); `ConfiguracionMensajeria.java:55-56` (comentario que contradice el código)
- Evidencia: `sql/README.md:58` → `psql -d aportaya -f sql/60_semillas/99_desarrollo.sql   # solo entorno local` · `'''` · `| Archivo | Qué carga | Estado |`
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: decisiones tomadas sobre documentación falsa. Solución: regenerar desde el modelo y corregir. Causa: documentación no generada. Relación: ninguna. Conexiones: varios. Estándares: ISO/IEC 25010 Analizabilidad. Esfuerzo: chico

### TYA-REG-001 — Riesgos regulatorios abiertos por el lote
- Clase / Severidad / Confianza: REG / S3 / probable
- Ubicaciones: TYA-HUECO-003 (supresión de datos falsamente informada); TYA-DINERO-007 (facturación electrónica simulada sin guardia y CUF local); TYA-SEG-002 (registro UIF invocable por cualquier servicio); TYA-DATOS-003 (umbrales UIF divergentes entre catálogo de auditoria y semilla); habilitación de dinero real condicionada al dossier H15 (`docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-F/H15-dossier-habilitacion-real.md`)
- Evidencia: sin fragmento extraíble automáticamente de las ubicaciones citadas (ubicación de carpeta o patrón); ver descripción en Impacto.
- Cómo se comprobó: lectura delegada a un subagente de solo lectura; el fragmento de Evidencia lo extrajo la sesión principal del archivo citado. Falta reabrir el resto de ubicaciones y, si aplica, ejecutar
- Impacto: evidencia faltante ante ASFI/UIF/SIN y en protección de datos. No se declara conformidad ni no conformidad. Solución: revisión de Cumplimiento con estos hallazgos; normas a identificar por Cumplimiento. Causa: módulos de apoyo sin dueño regulatorio. Relación: ninguna. Conexiones: cumplimiento (L4). Estándares: `docs/Cumplimiento.md`, `docs/Restricciones.md`. Esfuerzo: medio

### TYA-COHERENCIA-001 — Tres fuentes que se contradicen sobre la cobertura y la comisión en el lote
- Clase / Severidad / Confianza: COHERENCIA / S3 / confirmado (lectura)
- Ubicaciones: `docs/Arquitectura/ADR-051 Garantía del pozo, comisión y autogestión.md` (D-02/D-03 reserva empresarial, D-15 fondo mutual legado; D-04 comisión como cargo separado "no implementado todavía"); `sql/60_semillas/15-eventos-y-plantillas.sql:33` (anuncia cobertura del fondo mutual, que ADR-051 declara legado); `sql/60_semillas/22-cuentas-de-sistema.sql` (cuentas de reserva empresarial); `sql/60_semillas/09-reglas-operativas.sql:6` (`BOLSA_COMPLETA` "o cubierta por el fondo"); `servicios/tarifas` (comisión `COM_ENTREGA` sobre la entrega, coherente con D-04 en tarifas pero sin consumidor en el mayor, TYA-HUECO-002)
- Evidencia: `sql/60_semillas/15-eventos-y-plantillas.sql:33` → `('COBERTURA_APLICADA', 'El fondo de garantía cubrió tu aporte y quedó una deuda a tu nombre', 'COBRANZA', TRUE, 'ALTA', TRUE, FALSE, 0, 'IN_` · `('PROMESA_DE_PAGO_POR_VENCER', 'Vence la fecha que comprometiste para regularizar', 'COBRANZA', FALSE, 'NORMAL', FALSE, TRUE, 1440, 'IN_APP,` · `('PLAN_REGULARIZACION_APROBADO', 'Se aprobó tu plan de regularización con su cronograma', 'COBRANZA', TRUE, 'NORMAL', FALSE, FALSE, 0, 'IN_A`
- Cómo se comprobó: lectura de las ubicaciones por la sesión principal
- Impacto: con R-NUEVA-2 las tres fuentes quedan desactualizadas a la vez. Por la prioridad de evidencia (regla 00 §8), manda la regla de negocio explícita del dueño (R-NUEVA-2, 2026-11-20) sobre ADR-051; lo que sigue vivo en el código es el comportamiento real a corregir. Solución: ADR que reemplace a ADR-051 D-02/D-03/D-15 tras decidir A-2/A-3/A-4. Causa: decisiones sucesivas sin retiro del material anterior. Relación: RESPALDO. Conexiones: garantia, entregas, notificaciones, nucleo. Estándares: regla 00 §8. Esfuerzo: medio

## 6. Matriz de contratos

Una fila por operación HTTP de los 7 servicios del lote, generada desde sus OpenAPI (`servicios/<s>/src/main/resources/openapi/*.yaml`). Columnas:

- **Implementación**: `scripts/inventario_endpoints.py --check` → `endpoints relevados: 350 · hallazgos: 0` (contrato ↔ controlador alineados en los 15 servicios).
- **Pacto**: ninguno de los servicios del lote es productor de ningún pacto en `contratos/pactos/`.
- **Cliente generado**: búsqueda del `operationId` en `clientes/` (solo existe `clientes/angular/publicidad`).
- **Uso en front**: búsqueda heurística del prefijo de ruta en `apps/backoffice/src`, `apps/web/src`, `apps/movil/lib`, `packages`. "No encontrado" no prueba que nadie la llame.
- **Esquema en `sql/`**, **dinero y moneda** y **paginación** no se compararon fila a fila (ver §11). El dinero viaja como cadena + moneda en los esquemas leídos.
- **Errores**: formato `ErrorApi` propio, no RFC 9457 (TYA-CONTRATO-001).

Columna adicional *Idempotency-Key en contrato*: "sí" significa que el contrato declara la cabecera. TYA-DINERO-004 muestra que en varias operaciones se declara y no se usa.

| Servicio | Método y ruta | operationId | Implementación (`inventario_endpoints --check`) | Pacto | Cliente generado | Uso en front (ruta) | Idempotency-Key en contrato | Discrepancias / hallazgo |
|---|---|---|---|---|---|---|---|---|
| auditoria | `POST /auditoria/derechos` | `ejercerDerechos` | alineada | ninguno | no | sí | sí | Informa supresión no ejecutada (TYA-HUECO-003) |
| auditoria | `GET /auditoria/bitacora` | `consultarBitacora` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /auditoria/bitacora/{id}` | `consultarEventoBitacora` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /auditoria/catalogos` | `listarCatalogosOperativos` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /auditoria/catalogos/{codigo}` | `consultarCatalogoOperativo` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /auditoria/catalogos/{codigo}/elementos` | `listarElementosCatalogo` | alineada | ninguno | no | sí | no |  |
| auditoria | `PATCH /auditoria/catalogos/{codigo}/elementos/{id}` | `actualizarEstadoElementoCatalogo` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /reportes` | `listarDefinicionesDeReporte` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /reportes/ejecuciones` | `listarEjecucionesDeReporte` | alineada | ninguno | no | sí | no |  |
| auditoria | `POST /reportes/{definicionId}/ejecuciones` | `ejecutarReporte` | alineada | ninguno | no | sí | sí | SQL sobre esquemas ajenos (TYA-ACOPLE-001) |
| auditoria | `POST /reportes/exportaciones/{exportacionId}/descargas` | `descargarExportacion` | alineada | ninguno | no | sí | sí | 204 sin archivo (TYA-HUECO-004) |
| auditoria | `GET /indicadores` | `publicarTablero` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /indicadores/operacion/endpoints` | `listarEndpointsDelDia` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /indicadores/operacion/servicios` | `consultarSaludDeServicios` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /indicadores/operacion/outbox` | `consultarOutboxPorServicio` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /indicadores/operacion/incidentes` | `listarIncidentesOperativos` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /indicadores/catalogo-de-datos` | `obtenerCatalogoDeDatos` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /indicadores/catalogo-de-datos/resumen` | `obtenerResumenDelCatalogo` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /indicadores/catalogo-de-datos/tablas` | `listarTablasDelCatalogo` | alineada | ninguno | no | sí | no |  |
| auditoria | `GET /indicadores/catalogo-de-datos/tablas/{esquema}/{tabla}` | `obtenerTablaDelCatalogo` | alineada | ninguno | no | sí | no |  |
| auditoria | `PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/gobierno` | `guardarFichaDeGobierno` | alineada | ninguno | no | sí | no |  |
| auditoria | `PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/columnas/{columna}/revision` | `revisarColumna` | alineada | ninguno | no | sí | no |  |
| transparencia | `GET /publico/sorteos/{sorteoId}/verificacion` | `verificarSorteo` | alineada | ninguno | no | sí | no |  |
| transparencia | `POST /reputacion/eventos` | `registrarEventoReputacion` | alineada | ninguno | no | sí | sí |  |
| transparencia | `POST /reputacion/eventos/{eventoId}/compensacion` | `compensarEventoReputacion` | alineada | ninguno | no | sí | no |  |
| transparencia | `POST /reputacion/puntajes` | `recalcularPuntaje` | alineada | ninguno | no | sí | sí |  |
| transparencia | `POST /reputacion/snapshots` | `tomarSnapshotReputacion` | alineada | ninguno | no | sí | no |  |
| transparencia | `POST /reputacion/bloques` | `sellarBloque` | alineada | ninguno | no | sí | sí | Permiso de lectura escribe (TYA-SEG-008) |
| transparencia | `GET /publico/grupos/{grupoId}/verificacion` | `verificarCadena` | alineada | ninguno | no | sí | no |  |
| transparencia | `GET /publico/grupos/{grupoId}/bloques` | `listarBloques` | alineada | ninguno | no | sí | no |  |
| transparencia | `POST /reputacion/insignias/evaluacion` | `evaluarInsignias` | alineada | ninguno | no | sí | sí |  |
| transparencia | `POST /reputacion/insignias/{otorgadaId}/revocacion` | `revocarInsignia` | alineada | ninguno | no | sí | no |  |
| transparencia | `POST /reputacion/certificados` | `emitirCertificado` | alineada | ninguno | no | sí | sí | Contenido del cuerpo (TYA-SEG-006); clave ignorada |
| transparencia | `POST /reputacion/certificados/{codigo}/revocacion` | `revocarCertificado` | alineada | ninguno | no | sí | no |  |
| transparencia | `GET /verificar/{codigo}` | `verificarCertificado` | alineada | ninguno | no | sí | no | Siempre valido=false (TYA-DEFECTO-001) |
| transparencia | `POST /reputacion/resenas` | `publicarResena` | alineada | ninguno | no | sí | sí | Autor del cuerpo (TYA-SEG-007) |
| transparencia | `POST /reputacion/resenas/{resenaId}/moderacion` | `moderarResena` | alineada | ninguno | no | sí | no |  |
| transparencia | `POST /reputacion/riesgo/evaluacion` | `evaluarRiesgo` | alineada | ninguno | no | sí | sí |  |
| transparencia | `POST /reputacion/riesgo/alertas/{alertaId}/cierre` | `cerrarAlertaRiesgo` | alineada | ninguno | no | sí | no |  |
| transparencia | `GET /reputacion/{usuarioId}/perfil-en-grupo/{grupoId}` | `consultarPerfilEnGrupo` | alineada | ninguno | no | sí | no |  |
| transparencia | `GET /reputacion/etapas` | `listarEtapas` | alineada | ninguno | no | sí | no |  |
| transparencia | `GET /reputacion/etapas/de-personas` | `consultarEtapasDePersonas` | alineada | ninguno | no | sí | no |  |
| transparencia | `GET /reputacion/{usuarioId}/puntaje` | `consultarPuntaje` | alineada | ninguno | no | sí | no |  |
| notificaciones | `POST /notificaciones` | `emitirNotificacion` | alineada | ninguno | no | sí | sí | Clave ignorada; no hay despacho (TYA-HUECO-001) |
| notificaciones | `POST /notificaciones/webhooks/{proveedor}` | `procesarRespuestaEntrante` | alineada | ninguno | no | sí | no | Solo cargaUtil firmada (TYA-SEG-009); enum PAGO_CONFIRMADO sin emisor (TYA-APROB-002) |
| notificaciones | `GET /notificaciones/proveedores` | `listarProveedoresDeMensajeria` | alineada | ninguno | no | sí | no |  |
| notificaciones | `GET /notificaciones/webhooks-recibidos` | `listarEventosDeEntregaRecibidos` | alineada | ninguno | no | sí | sí |  |
| notificaciones | `GET /notificaciones/plantillas` | `listarPlantillas` | alineada | ninguno | no | sí | no |  |
| notificaciones | `GET /notificaciones/envios` | `listarEnvios` | alineada | ninguno | no | sí | no |  |
| notificaciones | `POST /notificaciones/supresion/consultas` | `verificarSupresion` | alineada | ninguno | no | sí | sí |  |
| notificaciones | `GET /notificaciones/supresion` | `consultarSupresion` | alineada | ninguno | no | sí | no | Contacto en la URL (TYA-SEG-013) |
| notificaciones | `GET /notificaciones/avisos-internos` | `consultarAvisosInternos` | alineada | ninguno | no | sí | no |  |
| notificaciones | `POST /notificaciones/avisos-internos` | `emitirAvisoInterno` | alineada | ninguno | no | sí | no |  |
| notificaciones | `POST /notificaciones/avisos-internos/{id}/leido` | `marcarAvisoLeido` | alineada | ninguno | no | sí | no | Sin chequeo de destinatario (TYA-SEG-012) |
| notificaciones | `POST /notificaciones/avisos-internos/marcar-todos-leidos` | `marcarTodosAvisosLeidos` | alineada | ninguno | no | sí | no |  |
| tarifas | `POST /comisiones/cotizaciones` | `cotizarComision` | alineada | ninguno | no | sí | sí | Descuento/segmento del cliente (TYA-SEG-001); clave ignorada en replay incompleto (TYA-DINERO-004) |
| tarifas | `POST /comisiones/cotizaciones/{cotizacionId}/aceptacion` | `aceptarCotizacion` | alineada | ninguno | no | sí | no |  |
| tarifas | `POST /comisiones/devengos` | `devengarComision` | alineada | ninguno | no | sí | sí | Obligado, descuento y exención del cuerpo (TYA-SEG-001) |
| tarifas | `POST /comisiones/devengos/{devengoId}/cobros` | `anotarCobroDeComision` | alineada | ninguno | no | sí | no | Cobro por aserción del participante (TYA-APROB-001); sin clave de idempotencia |
| tarifas | `POST /facturas` | `emitirFactura` | alineada | ninguno | no | sí | no | Nunca encuentra devengo COBRADO (TYA-DEFECTO-003) |
| tarifas | `POST /comisiones/{devengoId}/devoluciones` | `devolverComision` | alineada | ninguno | no | sí | sí | Clave aceptada e ignorada; actor del cuerpo (TYA-DINERO-002) |
| tarifas | `GET /tarifas/tarifarios` | `listarTarifarios` | alineada | ninguno | no | sí | no |  |
| tarifas | `POST /tarifas/tarifarios` | `publicarTarifario` | alineada | ninguno | no | sí | no |  |
| tarifas | `POST /tarifas/tarifarios/{tarifarioId}/vigencia` | `ponerTarifarioVigente` | alineada | ninguno | no | sí | no |  |
| tarifas | `POST /tarifas/liquidaciones/{periodo}/cierre` | `cerrarLiquidacion` | alineada | ninguno | no | sí | no | Saldo del mayor del cuerpo (lectura delegada) |
| tarifas | `GET /tarifas/costos-de-proveedor` | `listarCostosDeProveedor` | alineada | ninguno | no | sí | no |  |
| tarifas | `POST /tarifas/segmentos` | `crearSegmento` | alineada | ninguno | no | sí | no |  |
| tarifas | `POST /tarifas/precios/resolver` | `resolverPrecio` | alineada | ninguno | no | sí | no |  |
| tarifas | `GET /tarifas/vigentes/{codigo}` | `consultarTarifarioVigente` | alineada | ninguno | no | sí | no |  |
| publicidad | `POST /publicidad/socios-comerciales` | `postularSocioComercial` | alineada | ninguno | no | sí | sí |  |
| publicidad | `POST /publicidad/socios-comerciales/{socioId}/verificacion` | `verificarSocioComercial` | alineada | ninguno | no | sí | sí |  |
| publicidad | `GET /publicidad/anunciantes` | `listarAnunciantes` | alineada | ninguno | no | sí | no |  |
| publicidad | `POST /publicidad/anunciantes` | `darDeAltaAnunciante` | alineada | ninguno | no | sí | sí | Lee organizador con rol sin permiso (TYA-ACOPLE-001) |
| publicidad | `GET /publicidad/campanas` | `listarCampanas` | alineada | ninguno | no | sí | no |  |
| publicidad | `POST /publicidad/campanas` | `crearCampana` | alineada | ninguno | no | sí | sí | Sin propiedad de la cuenta (TYA-DINERO-006) |
| publicidad | `POST /publicidad/campanas/{campanaId}/aprobacion` | `aprobarCampana` | alineada | ninguno | no | sí | sí |  |
| publicidad | `POST /publicidad/campanas/{campanaId}/rechazo` | `rechazarCampana` | alineada | ninguno | no | sí | sí |  |
| publicidad | `GET /publicidad/piezas-creativas` | `listarPiezasCreativas` | alineada | ninguno | no | sí | no |  |
| publicidad | `POST /publicidad/piezas-creativas` | `subirPiezaCreativa` | alineada | ninguno | no | sí | sí |  |
| publicidad | `POST /publicidad/piezas-creativas/{piezaId}/revision` | `moderarPiezaCreativa` | alineada | ninguno | no | sí | sí |  |
| publicidad | `POST /publicidad/anuncios` | `programarAnuncio` | alineada | ninguno | no | sí | sí |  |
| publicidad | `POST /publicidad/espacios/{espacioId}/entrega` | `entregarAnuncio` | alineada | ninguno | no | sí | sí | Cada llamada factura (TYA-DINERO-006) |
| publicidad | `POST /publicidad/impresiones/{impresionId}/clic` | `registrarClic` | alineada | ninguno | no | sí | sí | Clic facturable repetible (TYA-DINERO-006) |
| publicidad | `POST /publicidad/conversiones` | `registrarConversion` | alineada | ninguno | no | sí | sí |  |
| publicidad | `GET /publicidad/cuentas/{cuentaId}/consumo` | `consumoDelPeriodo` | alineada | ninguno | no | sí | no |  |
| publicidad | `POST /publicidad/cuentas/{cuentaId}/liquidaciones` | `liquidarPeriodo` | alineada | ninguno | no | sí | sí |  |
| inversiones | `GET /inversiones/productos` | `listarProductosDeInversion` | alineada | ninguno | no | sí | no |  |
| inversiones | `POST /inversiones/productos/sincronizacion` | `sincronizarCatalogoDeInversion` | alineada | ninguno | no | sí | sí |  |
| inversiones | `POST /inversiones/valores-cuota/sincronizacion` | `sincronizarValoresDeCuota` | alineada | ninguno | no | sí | sí |  |
| inversiones | `POST /inversiones/ordenes` | `crearOrdenDeInversion` | alineada | ninguno | no | sí | sí |  |
| inversiones | `GET /inversiones/ordenes/{ordenId}` | `verOrdenDeInversion` | alineada | ninguno | no | sí | no |  |
| inversiones | `POST /inversiones/ordenes/{ordenId}/sincronizacion` | `sincronizarOrdenDeInversion` | alineada | ninguno | no | sí | sí |  |
| inversiones | `GET /inversiones/posiciones` | `listarPosicionesDeInversion` | alineada | ninguno | no | sí | no |  |
| inversiones | `GET /inversiones/posiciones/{posicionId}` | `verPosicionDeInversion` | alineada | ninguno | no | sí | no |  |
| inversiones | `GET /inversiones/posiciones/{posicionId}/comprobantes` | `listarComprobantesDeInversion` | alineada | ninguno | no | sí | no |  |
| inversiones | `POST /inversiones/posiciones/{posicionId}/rescates` | `solicitarRescateDeInversion` | alineada | ninguno | no | sí | sí |  |
| inversiones | `POST /inversiones/rescates/{rescateId}/sincronizacion` | `sincronizarRescateDeInversion` | alineada | ninguno | no | sí | sí |  |
| inversiones | `POST /inversiones/devengos` | `correrDevengoDeDpf` | alineada | ninguno | no | sí | sí |  |
| inversiones | `GET /inversiones/productos/{codigo}` | `verProductoDeInversion` | alineada | ninguno | no | sí | no |  |
| inversiones | `GET /inversiones/posiciones/{posicionId}/distribuciones` | `listarDistribucionesDeInteres` | alineada | ninguno | no | sí | no |  |
| inversiones | `GET /inversiones/posiciones/{posicionId}/comision` | `estimarComisionDeExito` | alineada | ninguno | no | sí | no |  |
| inversiones | `GET /inversiones/reportes/fiscal` | `reporteFiscalDeInversiones` | alineada | ninguno | no | sí | no |  |
| inversiones | `GET /inversiones/reportes/fiscal/exportacion` | `exportarReporteFiscalDeInversiones` | alineada | ninguno | no | sí | no |  |
| inversiones | `POST /inversiones/simulaciones/caja` | `simularCajaDeInversion` | alineada | ninguno | no | sí | no |  |
| inversiones | `POST /inversiones/instrucciones/reintento` | `reintentarInstruccionesAlLibro` | alineada | ninguno | no | sí | sí |  |
| inversiones | `GET /inversiones/corridas` | `listarCorridasDeInversiones` | alineada | ninguno | no | sí | no |  |
| erp | `GET /erp/ejercicios` | `listarEjerciciosFiscales` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/ejercicios` | `abrirEjercicioFiscal` | alineada | ninguno | no | sí | sí |  |
| erp | `GET /erp/ejercicios/{ejercicioId}/periodos` | `listarPeriodosDelEjercicio` | alineada | ninguno | no | sí | no |  |
| erp | `GET /erp/periodos/vigente` | `consultarPeriodoVigente` | alineada | ninguno | no | sí | no |  |
| erp | `POST /erp/periodos/{periodoId}/cierre` | `cerrarPeriodoContable` | alineada | ninguno | no | sí | sí | Lee nucleo_financiero con rol sin permiso (TYA-ACOPLE-001) |
| erp | `POST /erp/presupuestos` | `crearPresupuesto` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/presupuestos/{presupuestoId}/aprobacion` | `aprobarPresupuesto` | alineada | ninguno | no | sí | sí |  |
| erp | `GET /erp/partidas-presupuestarias` | `listarPartidasPresupuestarias` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/terceros` | `darDeAltaTercero` | alineada | ninguno | no | sí | sí |  |
| erp | `GET /erp/ordenes-de-compra` | `listarOrdenesDeCompra` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/ordenes-de-compra` | `crearOrdenDeCompra` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/ordenes-de-compra/{ordenId}/aprobacion` | `aprobarOrdenDeCompra` | alineada | ninguno | no | sí | sí |  |
| erp | `GET /erp/facturas-de-proveedor` | `listarFacturasDeProveedor` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/facturas-de-proveedor` | `registrarFacturaDeProveedor` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/facturas-de-proveedor/{facturaId}/pagos` | `pagarFacturaDeProveedor` | alineada | ninguno | no | sí | sí |  |
| erp | `GET /erp/cuentas-por-cobrar` | `listarCuentasPorCobrar` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/cuentas-por-cobrar` | `abrirCuentaPorCobrar` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/cuentas-por-cobrar/{cuentaId}/cobros` | `registrarCobro` | alineada | ninguno | no | sí | sí |  |
| erp | `GET /erp/activos` | `listarActivosFijos` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/activos/{activoId}/depreciaciones` | `depreciarActivo` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/periodos/{periodoId}/depreciaciones` | `correrDepreciacionDelPeriodo` | alineada | ninguno | no | sí | sí |  |
| erp | `POST /erp/periodos/{periodoId}/estados-financieros` | `generarEstadoFinanciero` | alineada | ninguno | no | sí | sí | Balance inalcanzable (TYA-DEFECTO-004); lee nucleo_financiero (TYA-ACOPLE-001) |
| erp | `GET /erp/estados-financieros/{estadoFinancieroId}/documento` | `descargarEstadoFinanciero` | alineada | ninguno | no | sí | no |  |
| erp | `GET /erp/plantillas-de-asiento/{plantillaId}/validacion` | `validarPlantillaDeAsiento` | alineada | ninguno | no | sí | no |  |


**Eventos del módulo**

| Evento | Publica | Consume | Consumidor idempotente | Pacto | Discrepancia |
|---|---|---|---|---|---|
| `aportaya.*.indicador_calculado` | todos (`plataforma/comun-mensajeria` `PublicacionDeIndicadores`) | auditoria (`RegistroDeIndicadores.java:70`) | sí (`Consumidos` por `event_id` + `onConflictDoNothing`) | `servicios-auditoria-indicador-calculado.json` | el propio pacto declara que se cerró contra un doble |
| `aportaya.grupos.grupo_creado` | grupos | tarifas (`CongelarTarifaDelGrupo`) | sí | ninguno | sin pacto (98.1.4) |
| `aportaya.tarifas.tarifa_congelada` | tarifas | grupos | — (L3) | ninguno | sin pacto |
| `aportaya.tarifas.comision_devengada / comision_cobrada / comision_devuelta / comision_incobrable` y 10 temas más de tarifas | tarifas | **nadie** | — | ninguno | TYA-HUECO-002 |
| `aportaya.erp.pago_a_proveedor_registrado`, `erp.cobro_registrado`, … | erp | **nadie** | — | ninguno | TYA-HUECO-002 |
| `aportaya.publicidad.factura_publicidad_generada`, … | publicidad | **nadie** | — | ninguno | TYA-HUECO-002 |
| `aportaya.transparencia.*` (14 tipos, incl. `cadena_rota`, `sorteo_verificacion_fallida`) | transparencia | **nadie** | — | ninguno | TYA-HUECO-005 |
| `aportaya.notificaciones.aviso_solicitado`, `notificacion_*`, `recordatorio_debido`, `respuesta_procesada`, `remitente_desconocido` | notificaciones | **nadie** | — | ninguno | TYA-HUECO-001 |
| `aportaya.auditoria.reporte_*`, `derecho_solicitado`, `anonimizacion_planificada` | auditoria | **nadie** | — | ninguno | TYA-HUECO-003 |
| `aportaya.inversiones.*` | inversiones | **nadie** (solo claves de configuración) | — | ninguno | — |
| `aportaya.garantia.respaldo_*` (conexión) | garantia | nucleo-financiero | lo audita L1/L2 | ninguno | inventario RS-02 |

**Pactos (`contratos/pactos/`)**

| Pacto | Productor → consumidor | Prueba productor | Prueba consumidor | Discrepancia |
|---|---|---|---|---|
| aportes-nucleo-financiero-aporte-cobrado | aportes → nucleo | `CU21EventoDeAporteTest` | `ConsumidorDeAportesKafkaTest` | `tipo` opcional con destino por omisión al pozo; ejemplo con canal `BILLETERA_MOVIL` (R-NUEVA-1) |
| aportes-nucleo-financiero | nucleo → aportes | `CUAporteNoAplicadoTest` | `ConsumidorDeAportesNoAplicadosKafkaTest` | el consumidor no exige `moneda` |
| grupos-aportes-grupo-iniciado | grupos → aportes | `CU69EventoDeInicioTest` | `PactoGrupoIniciadoTest` | ninguna encontrada |
| identidad-grupos-pertenencia | grupos → identidad | **ninguna** | `GruposPorHttpPactoContratoTest` | coincide con `grupos.yaml:1031` |
| transparencia-grupos-pertenencia | grupos → transparencia | **ninguna** | `GruposPorHttpPactoContratoTest` | igual |
| inversiones-nucleo-financiero | nucleo → inversiones | `CU13PactoInversionesTest` | `LibroPorHttpPactoContratoTest` | el pacto exige `titularId` y `comprometidoEnPozos` que `nucleo-financiero.yaml:1501` deja opcionales |
| nucleo-financiero-entregas | entregas → nucleo | **ninguna en entregas** | `PactoConEntregasContratoTest` | patrón de dinero admite negativos (`entregas.yaml:1017`) |
| nucleo-financiero-garantia-aporte-aplicado | nucleo → garantia | `CUPactosDelRecargoTest` | `ConsumidorDeAportesAplicadosKafkaTest` | `destino` opcional → GRUPO |
| nucleo-financiero-garantia-transaccion-reversada | nucleo → garantia | `CUPactosDelRecargoTest` | `ConsumidorDeReversosKafkaTest` | ninguna encontrada |
| servicios-auditoria-indicador-calculado | todos → auditoria | `PublicadorDeIndicadoresTest` | `ConsumidorDeIndicadoresKafkaTest` | cerrado contra un doble |

Ejecución: `contractTest` en verde en transparencia (4) e inversiones (23) (`evidencia/g-resumen-servicios.txt`); los otros cinco servicios del lote no tienen pruebas en ese corredor.

## 7. Evidencia obligatoria del módulo (reglas 91.6, 98.8, 90.6)

| Evidencia | Estado | Detalle |
|---|---|---|
| Asientos que genera cada operación de dinero del lote | **FAIL** | Comisión cobrada, pago a proveedor, cobro ERP y factura publicitaria **no generan asiento**: sus eventos no tienen consumidor (TYA-HUECO-002). |
| Invariante debe = haber por asiento en la base | **FAIL** | Reproducido: asiento CONFIRMADO con debe 150,00 / haber 100,00 aceptado (`evidencia/db-experimento-mayor.txt`). El control dentro de una sola transacción sí funciona (E4 rechazado). |
| Idempotencia ejercitada (misma operación dos veces) | **PASS** solo para semillas: corridas 2 y 3 de `sembrar.sql` con conteos idénticos en todas las tablas (`evidencia/db-diff-seed.txt`, 0 líneas de diferencia; 1 082 filas). **NO COMPROBADO** para endpoints de dinero: requiere Docker o servicios levantados. |
| Cuadre del mayor contra total esperado | **NO COMPROBADO** con datos reales. `verificaciones.sql` no tiene consulta de cuadre por asiento (TYA-DINERO-001). |
| Caso de reversa | **NO COMPROBADO** (devolución de comisión sin asiento; ver TYA-DINERO-002). |
| Redondeo declarado | **PASS (lectura)**: `PoliticaDeRedondeo` como dato de catálogo en tarifas; residuo asignado a la última cuota en `erp` (depreciación) y en `inversiones` (rescate). No ejecutado. |
| Prueba de contrato | **PASS** parcial: `contractTest` 27/27 en verde (transparencia e inversiones). Productor sin prueba en 3 pactos. |
| Prueba de duplicado de mensaje | **NO COMPROBADO** en ejecución (requiere Kafka). Por lectura: el consumidor de auditoria y el de tarifas deduplican por `event_id`. |
| Dependido apagado | **NO COMPROBADO** (sin Docker). Por lectura: `ClienteDeServicio` convierte 5xx en 422 (TYA-SEG-004); fail-closed en transparencia→grupos. |
| Traza con `correlationId` | **NO COMPROBADO**; por lectura, interceptor Kafka posiblemente sin cablear (TYA-TRAZA-001). |
| Matriz de autorización negativa | **PASS (existencia)**: `scripts/verificar_pruebas_web.py` → "controladores: 42 · con matriz propia: 42 · en deuda: 0"; `webTest` en verde en los 7 servicios. **Pero** las matrices no detectan TYA-SEG-001/APROB-001: el permiso es correcto, falta la propiedad del recurso. |
| Prueba de humo del esquema | **PASS**: 165 líneas `OK`, 0 `FALLA` (`evidencia/db-humo.txt`). |
| Aplicación del esquema en limpio | **PASS**: `psql -v ON_ERROR_STOP=1 -f sql/aplicar.sql` → `exit=0` (`evidencia/db-aplicar.txt`). |

## 8. Cobertura por clase

| Clase | Qué se revisó | Método | Hallazgos |
|---|---|---|---|
| APROB | Alcance completo + esquema de los demás lotes | grep de términos de §6, lectura de controladores de tarifas, notificaciones y tablas | 2 (+ inventario 4a, 12 filas) |
| RESPALDO | Alcance + consumidores Kafka + semillas | grep, lectura de semillas, `@KafkaListener` | 2 (+ inventario 4b, 11 filas) |
| FLUJO-QR | Esquema de pago/webhook, mensajería, recordatorios | `pg_get_constraintdef`, lectura | 1 (y relación en 12 más) |
| FLUJO-FALTANTE | Regla `BOLSA_COMPLETA`, comisión como cargo separado | lectura de semillas y de tarifas | 0 — el lote no contiene ramas de faltante; la entrega es de L2 (ver A-2) |
| COHERENCIA | ADR-051, ADR-054, ADR-049, semillas, catálogo de flujos | lectura | 1 |
| DUP-INT | Hash SHA-256 hex, `raizDe`, `comoJson`, `ClienteDeServicio` | lectura delegada | 0 propios (incluidos en TYA-CLEAN-001 y TYA-DUP-X-001) |
| DUP-X | Circuit breaker, idempotencia, `Importe` | diff delegado | 1 |
| REESCRITURA | Búsqueda de `v2`, `legacy`, `old`, dos CU para lo mismo en el lote | grep | 0 en el lote (los legados de ADR-051 están en L2) |
| PISADA | Historial completo disponible; comentarios que contradicen código | lectura | 0 como clase (comentario contradictorio en TYA-DOC-001) |
| PARCHE | Casos especiales, `catch` que traga, reintentos sin causa | lectura | 0 como clase (en TYA-CLEAN-001, TYA-FRAGIL-002) |
| FRAGIL | Cadenas que rompen lejos | lectura | 2 |
| DEFECTO | Lógica de los CU de los 7 servicios | lectura + base | 9 |
| HUECO | Productores sin consumidor, CU sin entrada | grep de `@KafkaListener`, `@Scheduled` | 5 |
| RESILIENCIA | Kafka, outbox, clientes HTTP, MinIO | lectura | 5 |
| DINERO | 91.1 y 91.2 en código y esquema | lectura + experimento en base | 12 |
| DEBIDO-PROCESO | Reputación y su origen | lectura | 2 |
| DDD | Obsesión por primitivos (trazas, importes en SQL), entidades devueltas | lectura | 0 como clase (TYA-FRAGIL-001, TYA-DINERO-005) |
| ARQ | Capas, gateway | lectura | 2 |
| CLEAN | Patrones repetidos | lectura delegada; `scripts/auditar_backend.py` | 1 (patrón) |
| ACOPLE | SQL entre esquemas, FKs cruzadas | ejecución con roles reales | 2 |
| CONTRATO | OpenAPI, pactos, errores | `inventario_endpoints --check`, lectura | 5 |
| DATOS | Esquema, semillas, procedencia, deriva | base local + lectura | 10 |
| RENDIMIENTO | Recalculo de saldos, paginación | lectura | 1 |
| PRUEBAS | Ejecución de suites, fixtures | Gradle (ejecutado) | 2 |
| UX-A11Y | — | **no revisado** (pantallas fuera de esta sesión, §11) | 0 — no miré |
| OBSERV | Logback, máscara de PII | lectura | 2 |
| CONFIG | Lockfiles, valores por omisión, descriptores | `ls`, `scripts/auditar_backend.py` | 2 |
| DOC | READMEs, comentarios, descriptores | lectura | 1 |
| SEG | Autorización por objeto y función, firmas, RLS, funciones SQL | lectura + base | 16 |
| TRAZA | correlación, bitácora de lectura | lectura | 1 (+ DEBIDO-PROCESO-002) |
| REG | Riesgo regulatorio agregado | consolidación | 1 |
| OTRO | — | — | 0 |

## 9. Lo que está bien

- **Mayor de billetera**: cuadre por transacción verificado al COMMIT con trigger diferido y por moneda (R-BIL-01/20), con "bloquear antes de leer" documentado (`sql/40_reglas/restricciones.sql:325-342`). La prueba de humo lo ejercita (pasa).
- **Esquema reproducible**: `aplicar.sql` aplica en limpio en ~3,5 s; 165/165 casos de humo; semillas idempotentes entre corridas del mismo día; 47 tablas selladas con REVOKE + trigger.
- **Un esquema y un rol por servicio**: la frontera es el GRANT (comprobado: los roles ajenos reciben `permission denied`).
- **`Dinero`** con `BigDecimal`, escala 2, rechazo de mezcla de monedas y JSON como cadena; ningún `float`/`double` de dinero en el alcance (Java, SQL).
- **Outbox** en la misma transacción y relevo en tres pasos con `SKIP LOCKED`, sin red dentro de la transacción; consumidores con `Consumidos` por PK.
- **Deny-by-default** con `@Publico` única fuente; el arranque falla si un endpoint no decide; `jwks-uri` obligatorio; CORS y rate limiting fail-closed.
- **Cadena de transparencia** con árbol de Merkle, una sola clase para sellar y verificar; compensación de reputación por contra-evento.
- **inversiones**: guardia de producción que impide arrancar con el aliado simulado; timeouts y breaker; claves de instrucción deterministas.
- **ERP**: cuatro ojos con actor del token; residuo asignado a la última cuota de depreciación.
- **Gates del repo**: `verificar_seguridad.py` "TODO OK · 7 aviso(s)"; `verificar_pruebas_web.py` 42/42 controladores con matriz; `inventario_endpoints.py --check` 350/350 alineados.

## 10. Observado fuera de alcance

- `scripts/verificar_rutas_frontend.py` falla: la lista de conocidas incluye `GET /grupos/solicitudes-escaladas`, que ya tiene contrato (`evidencia/v-verificar_rutas_frontend.txt`).
- `docs/catalogo-de-flujos/recarga-y-retiro.yaml`: paso `instruir-pago` sin cablear (H8-01), según `scripts/validar_flujos.py`.
- `scripts/auditar_backend.py`: operaciones declaradas sin implementar en cumplimiento (5) y grupos (7+), y `aportes.webhook.secreto-de-prueba` con solo valor por omisión (`servicios/aportes/.../SecretoWebhookDeDesarrollo.java`).
- `servicios/cumplimiento/.../LimiteRepositorio.java:67,99` lee `nucleo_financiero` (mismo patrón que TYA-ACOPLE-001).
- `servicios/grupos/.../CU60Sortear.java:80` (semilla) — consumidor de TYA-SEG-005.
- `servicios/garantia/.../GarantiaController.java:172` (`POST /garantia/respaldo/coberturas` con `ENTREGA_EJECUTAR`).
- `servicios/entregas/.../CU18RegistrarCuentaDestino.java:107` (variable `pimienta` usada como secreto de cifrado; verificar reuso).
- `scripts/verificar_seguridad.py` avisa `DSL.field(variable)` en `servicios/entregas` (2) y `servicios/nucleo-financiero` (3), además de `servicios/inversiones/.../Sql.java:20`.

## 11. Límites de esta auditoría

- **No leído completo**: pruebas de los servicios (250 archivos, solo búsqueda; sí ejecutadas); ~60 % del código main de `inversiones`, CU101/CU102/CU105 de `erp`, CU112 de `publicidad` y varios repositorios de `tarifas`; los OpenAPI fuera de rutas y esquemas citados; ~2 800 de 3 526 líneas de `restricciones.sql`; ~320 de 351 archivos de tabla (solo por script); `24-metadata-comentarios.sql`; `62_volumen` (inventario y muestra); los dos catálogos JSON generados (inventario y búsqueda de secretos y datos personales: sin hallazgos).
- **Pantallas**: no se auditaron las pantallas de `apps/movil`, `apps/backoffice` ni `apps/web` que consumen estos servicios (supuesto: son de L6 o del lote del módulo de negocio). Por eso UX-A11Y queda sin revisar.
- **No ejecutado / BLOCKED**: pruebas de integración y `ArranqueTest` de los 7 servicios (Testcontainers sin Docker: `Could not find a valid Docker environment`; no se arregló el entorno por instrucción del metaprompt); Kafka, MinIO y Redis; `yarn lint` y `yarn typecheck` (frontend fuera del alcance de este lote); `scripts/cobertura.py` ("Sin informes. Corré primero ./gradlew test ... jacocoTestReport"); el test del gateway (no corre en `:test`).
- **Verificadores**: `scripts/idempotencia.py` terminó con `exit 0` y salida vacía (no se sabe qué verificó); `scripts/matriz_de_pruebas.py` se corrió con `--servicio` para no escribir `docs/`; `scripts/inventario_endpoints.py` con `--check` por la misma razón.
- **Gradle**: la primera corrida de `plataforma` falló por `429 Too Many Requests` de Maven Central; se reintentó una vez y pasó (`evidencia/g-plataforma-test.txt`, `-2.txt`).
- **Confianza delegada**: los hallazgos marcados *probable* vienen de lectura delegada y no se reabrieron línea por línea; los S1 y S2 marcados *confirmado* sí.
- **Supuestos**: el `.puml` y `docs/Restricciones.md` son la fuente del `sql/` generado (ADR-032) y no se auditaron. Las rutas `...` abreviadas en las fichas son `src/main/java/bo/aportaya/<servicio>/`. Prefijo de hallazgos `TYA`.
- **Base local**: los experimentos se hicieron como superusuario sobre una base efímera. Los triggers aplican igual a cualquier rol. Las pruebas de permisos se hicieron con `SET ROLE svc_*`.

### Refutación (Fase 7)

- TYA-DINERO-001: busqué un trigger sobre `movimiento_contable` que revalide el asiento. Los que existen son `cuenta_de_movimiento`, `sincroniza_saldo`, `moneda` y `append_only`. Ninguno revalida, y el experimento lo confirma. **Resiste.**
- TYA-SEG-001 / APROB-001: busqué RLS que limite por titular. No existe en `devengo_comision` ni en `cargo_comision` (`relrowsecurity = f`). Busqué un chequeo de propiedad en CU31 y no lo hay. **Resisten.**
- TYA-ACOPLE-001: hipótesis "el rol de producción tiene GRANT". Refutada con `has_schema_privilege` = `f` y `permission denied` real. **Resiste.**
- TYA-SEG-002: hipótesis "EXECUTE revocado en otro archivo". `proacl` es nulo en las 63 funciones y la invocación como `svc_publicidad` funciona. **Resiste.**
- TYA-RESILIENCIA-001: hipótesis "otro `CommonErrorHandler` en un servicio". El grep en servicios y plataforma solo encuentra este. **Resiste.**
- TYA-HUECO-001: hipótesis "lo invoca una tarea programada o un consumidor". No hay `@Scheduled`, `@KafkaListener` ni carpeta `trabajos/` en notificaciones. **Resiste.**
- TYA-DEFECTO-002: hipótesis "el código divide por 100". `brutoSegunMetodo` multiplica directo y la prueba usa `0.0030`. **Resiste.**
- TYA-DATOS-001: se mantiene en S2 y no en S1. Es data de desarrollo, protegida por `app.entorno='dev'`, y aparentemente de la propia persona responsable. Igual incumple la regla 97.6.1.
- Bajados a *probable* o *sospecha*, sin reapertura propia: TYA-SEG-003 (parte `X-Forwarded-For`), TYA-RESILIENCIA-003 y TYA-TRAZA-001 (comportamiento de librería no ejecutado), TYA-DATOS-010 (heurística).

## 12. Anexo de cobertura de archivos

Generado con `git ls-files` sobre el alcance y `wc -l`. La columna "Qué es" describe la carpeta, no cada archivo. Hacerlo archivo por archivo en 1 282 filas excedía la sesión, y se declara así.

| Estado | Archivos | Líneas |
|---|---:|---:|
| sí | 395 | 31 388 |
| parcial / grep (código de servicio) | 157 | 26 548 |
| parcial (pruebas de plataforma, por nombres y aserciones) | 73 | 7 504 |
| no (grep); ejecutada (pruebas de servicios) | 250 | 36 822 |
| grep por script (DDL de tablas, aplicado) | 347 | 10 061 |
| parseo por script (claves e índices, aplicado) | 30 | 6 043 |
| parcial (OpenAPI) | 7 | 6 958 |
| parcial (`restricciones.sql`) | 1 | 3 526 |
| inventario + búsqueda (catálogos generados) | 2 | 131 888 |
| inventario + muestra (`62_volumen`) | 13 | 17 014 |
| inventario / no (lockfiles) | 7 | 1 429 |
| **Total** | **1 282** | **279 181** |

| Ruta | Líneas | Leído completo | Qué es |
|---|---:|---|---|
| `contratos/pactos/aportes-nucleo-financiero-aporte-cobrado.json` | 49 | sí | pacto entre servicios |
| `contratos/pactos/aportes-nucleo-financiero.json` | 34 | sí | pacto entre servicios |
| `contratos/pactos/grupos-aportes-grupo-iniciado.json` | 51 | sí | pacto entre servicios |
| `contratos/pactos/identidad-grupos-pertenencia.json` | 46 | sí | pacto entre servicios |
| `contratos/pactos/inversiones-nucleo-financiero.json` | 190 | sí | pacto entre servicios |
| `contratos/pactos/nucleo-financiero-entregas.json` | 37 | sí | pacto entre servicios |
| `contratos/pactos/nucleo-financiero-garantia-aporte-aplicado.json` | 43 | sí | pacto entre servicios |
| `contratos/pactos/nucleo-financiero-garantia-transaccion-reversada.json` | 31 | sí | pacto entre servicios |
| `contratos/pactos/servicios-auditoria-indicador-calculado.json` | 46 | sí | pacto entre servicios |
| `contratos/pactos/transparencia-grupos-pertenencia.json` | 46 | sí | pacto entre servicios |
| `plataforma/comun-archivos/build.gradle.kts` | 21 | sí | código transversal |
| `plataforma/comun-archivos/src/main/java/bo/aportaya/plataforma/archivos/AlmacenDeArchivos.java` | 44 | sí | código transversal |
| `plataforma/comun-archivos/src/main/java/bo/aportaya/plataforma/archivos/AlmacenS3Adaptador.java` | 180 | sí | código transversal |
| `plataforma/comun-archivos/src/main/java/bo/aportaya/plataforma/archivos/AmbitoArchivo.java` | 64 | sí | código transversal |
| `plataforma/comun-archivos/src/main/java/bo/aportaya/plataforma/archivos/ArchivoGuardado.java` | 10 | sí | código transversal |
| `plataforma/comun-archivos/src/main/java/bo/aportaya/plataforma/archivos/ClaveObjeto.java` | 53 | sí | código transversal |
| `plataforma/comun-archivos/src/main/java/bo/aportaya/plataforma/archivos/ConfiguracionDeArchivos.java` | 34 | sí | código transversal |
| `plataforma/comun-archivos/src/main/java/bo/aportaya/plataforma/archivos/ContenidoAlmacenado.java` | 6 | sí | código transversal |
| `plataforma/comun-archivos/src/main/java/bo/aportaya/plataforma/archivos/ContenidoEntrante.java` | 16 | sí | código transversal |
| `plataforma/comun-archivos/src/main/java/bo/aportaya/plataforma/archivos/DestinoDeObjeto.java` | 92 | sí | código transversal |
| `plataforma/comun-archivos/src/main/java/bo/aportaya/plataforma/archivos/TipoPorContenido.java` | 46 | sí | código transversal |
| `plataforma/comun-archivos/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 1 | sí | código transversal |
| `plataforma/comun-archivos/src/test/java/bo/aportaya/plataforma/archivos/DestinoDeObjetoTest.java` | 88 | sí | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-datos/build.gradle.kts` | 23 | sí | código transversal |
| `plataforma/comun-datos/src/main/java/bo/aportaya/plataforma/datos/CalendarioDelCatalogo.java` | 44 | sí | código transversal |
| `plataforma/comun-datos/src/main/java/bo/aportaya/plataforma/datos/ConfiguracionDeDatos.java` | 64 | sí | código transversal |
| `plataforma/comun-datos/src/main/java/bo/aportaya/plataforma/datos/Datos.java` | 95 | sí | código transversal |
| `plataforma/comun-datos/src/main/java/bo/aportaya/plataforma/datos/SinTransaccion.java` | 19 | sí | código transversal |
| `plataforma/comun-datos/src/main/java/bo/aportaya/plataforma/datos/TransaccionAparte.java` | 45 | sí | código transversal |
| `plataforma/comun-datos/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 1 | sí | código transversal |
| `plataforma/comun-datos/src/test/java/bo/aportaya/plataforma/datos/AppendOnlyRepositorioTest.java` | 117 | sí | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-datos/src/test/java/bo/aportaya/plataforma/datos/ComoSistemaRepositorioTest.java` | 105 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-datos/src/test/java/bo/aportaya/plataforma/datos/ContextoDeFilaRepositorioTest.java` | 118 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-datos/src/test/java/bo/aportaya/plataforma/datos/DatosTest.java` | 32 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/build.gradle.kts` | 19 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/CalendarioHabil.java` | 17 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/CifradorSimetrico.java` | 130 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/ClaveIdempotencia.java` | 38 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/CodigoError.java` | 38 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/ContextoSesion.java` | 42 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/Dinero.java` | 143 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/ErrorDeDominio.java` | 25 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/ErrorDeNegocio.java` | 41 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/Ids.java` | 33 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/Indicador.java` | 147 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/Moneda.java` | 11 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/Periodo.java` | 42 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/PeriodoDeTablero.java` | 87 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/PlazoHabil.java` | 64 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/Prorrateo.java` | 60 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/Reloj.java` | 44 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/SinContextoDeSesion.java` | 17 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/SorteoVerificable.java` | 126 | sí | código transversal |
| `plataforma/comun-dominio/src/main/java/bo/aportaya/plataforma/dominio/Traza.java` | 30 | sí | código transversal |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/CifradorSimetricoTest.java` | 72 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/CodigoErrorTest.java` | 37 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/ContextoSesionTest.java` | 50 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/DineroCuadrePropiedadTest.java` | 64 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/DineroCuadreTest.java` | 44 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/DineroTest.java` | 119 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/ErrorDeNegocioTest.java` | 46 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/IdentificadoresTest.java` | 84 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/IndicadorTest.java` | 124 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/PeriodoDeTableroTest.java` | 63 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/PeriodoTest.java` | 57 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/PlazoHabilTest.java` | 67 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/ProrrateoTest.java` | 65 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-dominio/src/test/java/bo/aportaya/plataforma/dominio/SorteoVerificableTest.java` | 149 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/build.gradle.kts` | 37 | sí | código transversal |
| `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/CalculadorDeIndicadores.java` | 20 | sí | código transversal |
| `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/ConfiguracionMensajeria.java` | 128 | sí | código transversal |
| `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/Consumidos.java` | 33 | sí | código transversal |
| `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/EnvelopeDeEvento.java` | 73 | sí | código transversal |
| `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/EventoDominio.java` | 38 | sí | código transversal |
| `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/InterceptorDeTrazaKafka.java` | 58 | sí | código transversal |
| `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/Outbox.java` | 76 | sí | código transversal |
| `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/PublicacionDeIndicadores.java` | 112 | sí | código transversal |
| `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/PublicadorDeIndicadores.java` | 88 | sí | código transversal |
| `plataforma/comun-mensajeria/src/main/java/bo/aportaya/plataforma/mensajeria/Relevo.java` | 274 | sí | código transversal |
| `plataforma/comun-mensajeria/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 1 | sí | código transversal |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/BarridoTest.java` | 31 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/BaseDePruebaMensajeria.java` | 89 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/ConsumidosRepositorioTest.java` | 77 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/EventoDominioTest.java` | 42 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/InterceptorDeTrazaKafkaTest.java` | 58 | sí | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/OutboxE2ETest.java` | 122 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/OutboxRepositorioTest.java` | 112 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/PublicacionDeIndicadoresRepositorioTest.java` | 132 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/PublicadorDeIndicadoresTest.java` | 135 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/RelevoAutoConfiguracionTest.java` | 87 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/RelevoConfiguracionTest.java` | 89 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-mensajeria/src/test/java/bo/aportaya/plataforma/mensajeria/RelevoRepositorioTest.java` | 161 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/build.gradle.kts` | 24 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/BaseDePrueba.java` | 134 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/DatosSinteticos.java` | 96 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/DependidoDeEnsayo.java` | 107 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/EfectoDeReclamosDeEnsayo.java` | 188 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/Espera.java` | 42 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/SinRelevoEnPruebas.java` | 32 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/barrido/Barrido.java` | 86 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/barrido/ClaveIdempotenciaSuelta.java` | 81 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/barrido/ConContextoSinTransaccion.java` | 99 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/barrido/Hallazgo.java` | 16 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/barrido/SinUmbralLiteral.java` | 67 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/barrido/TamanoDeArchivo.java` | 83 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/seguridad/Decision.java` | 38 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/seguridad/FuenteJava.java` | 188 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/seguridad/MapaDeSeguridad.java` | 240 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/seguridad/Operacion.java` | 40 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/seguridad/SalidasSensibles.java` | 216 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/resources/META-INF/spring.factories` | 1 | sí | código transversal |
| `plataforma/comun-pruebas/src/main/resources/archunit.properties` | 9 | sí | código transversal |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/AislamientoEsquemaTest.java` | 143 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/DatosSinteticosTest.java` | 69 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/DependidoDeEnsayoTest.java` | 76 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/DocumentacionVivaContratoTest.java` | 108 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/EfectoDeReclamosDeEnsayoTest.java` | 146 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/EsperaTest.java` | 77 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/PrefijoSinDosDuenosContratoTest.java` | 99 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/RutaEnPrefijoContratoTest.java` | 120 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/SinRelevoEnPruebasTest.java` | 46 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/barrido/PlataformaBarridoTest.java` | 26 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/barrido/ReglasPropiasTest.java` | 204 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/seguridad/AlarmasContratoTest.java` | 250 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/seguridad/MapaDeSeguridadDeteccionTest.java` | 170 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/seguridad/MatrizDeAutorizacionContratoTest.java` | 227 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/seguridad/ModelosSinCamposAbiertosTest.java` | 91 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/seguridad/PoliticaDeAcceso.java` | 217 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/seguridad/RotacionDeSecretosContratoTest.java` | 75 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/seguridad/SalidasSensiblesBarridoTest.java` | 94 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/seguridad/SalidasSensiblesDeteccionTest.java` | 117 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/seguridad/SecretosSinDefaultBarridoTest.java` | 121 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/java/bo/aportaya/plataforma/pruebas/seguridad/SpringSinSuperficieVulnerableBarridoTest.java` | 118 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-pruebas/src/test/resources/seguridad/salidas-sensibles-conocidas.txt` | 8 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/build.gradle.kts` | 84 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/CatalogoDeErrores.java` | 55 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/ConfiguracionComunWeb.java` | 98 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/CuerposEstrictos.java` | 38 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/clientes/ClienteDeServicio.java` | 134 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/clientes/CodigoTotp.java` | 55 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/clientes/ConfiguracionDeClientes.java` | 32 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/clientes/CredencialDeServicio.java` | 29 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/clientes/CredencialDeServicioConfigurada.java` | 53 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/clientes/CredencialPorLogin.java` | 155 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/dinero/ConfiguracionDeDinero.java` | 24 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/dinero/DeserializadorDeDinero.java` | 33 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/dinero/SerializadorDeDinero.java` | 27 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/ErrorApi.java` | 19 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/FallaDeLaBase.java` | 90 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/LimiteDeUsoExcedido.java` | 25 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/ManejadorGlobalDeErrores.java` | 277 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/RastroSeguro.java` | 67 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/errores/TraduccionDeRestricciones.java` | 65 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/idempotencia/Idempotencia.java` | 198 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/idempotencia/IdempotenciaConflicto.java` | 29 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/idempotencia/IdempotenciaEnProceso.java` | 27 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/idempotencia/OperacionRepetida.java` | 32 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/observabilidad/DefaultsDeObservabilidad.java` | 45 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/observabilidad/EnmascaradorPii.java` | 58 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/salud/SaludDeLaBase.java` | 35 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/ConfiguracionDeSeguridad.java` | 78 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/ConfiguracionDelDecodificador.java` | 30 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/GuardiaDePermiso.java` | 95 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/LoQueEstaAbierto.java` | 119 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/Permiso.java` | 20 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/PermisoAlternativo.java` | 26 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/Publico.java` | 21 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/RegistroDeGuardias.java` | 29 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/SesionDeLaPeticion.java` | 126 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/TodoEndpointDecideSuAcceso.java` | 68 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/traza/FiltroDeTraza.java` | 37 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/traza/Traza.java` | 76 | sí | código transversal |
| `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/traza/TrazaDeLaPeticion.java` | 17 | sí | código transversal |
| `plataforma/comun-web/src/main/resources/META-INF/spring.factories` | 1 | sí | código transversal |
| `plataforma/comun-web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 2 | sí | código transversal |
| `plataforma/comun-web/src/main/resources/logback-spring.xml` | 47 | sí | código transversal |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/AplicacionDeEnsayo.java` | 18 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/BarridoTest.java` | 38 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/CatalogoDeErroresTest.java` | 30 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/clientes/CodigoTotpTest.java` | 40 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/clientes/CredencialDeServicioConfiguradaTest.java` | 47 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/clientes/CredencialPorLoginContratoTest.java` | 262 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/clientes/UsuarioDeLaSesionTest.java` | 44 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/errores/BitacoraDeErroresSinContenidoWebTest.java` | 127 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/errores/ControladorDeEnsayo.java` | 183 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/errores/ManejadorGlobalDeErroresWebTest.java` | 272 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/errores/RechazosDeJooqWebTest.java` | 84 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/idempotencia/BaseIdempotenciaRepositorioTest.java` | 110 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/idempotencia/IdempotenciaConcurrenciaRepositorioTest.java` | 132 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/idempotencia/IdempotenciaRepositorioTest.java` | 135 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/observabilidad/DefaultsDeObservabilidadTest.java` | 58 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/observabilidad/EnmascaradorPiiTest.java` | 52 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/test/java/bo/aportaya/plataforma/web/seguridad/GuardiaDePermisoWebTest.java` | 147 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/testFixtures/java/bo/aportaya/plataforma/pruebas/web/ContextoWebDePrueba.java` | 97 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/testFixtures/java/bo/aportaya/plataforma/pruebas/web/Endpoint.java` | 172 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/testFixtures/java/bo/aportaya/plataforma/pruebas/web/PruebaWeb.java` | 70 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/testFixtures/java/bo/aportaya/plataforma/pruebas/web/SabanaDeSeguridadWeb.java` | 201 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/comun-web/src/testFixtures/java/bo/aportaya/plataforma/pruebas/web/Sesiones.java` | 84 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `plataforma/gateway/build.gradle.kts` | 50 | sí | código transversal |
| `plataforma/gateway/gradle.lockfile` | 193 | inventario | dependencias fijadas |
| `plataforma/gateway/src/main/java/bo/aportaya/gateway/Aplicacion.java` | 20 | sí | código transversal |
| `plataforma/gateway/src/main/java/bo/aportaya/gateway/ConfiguracionCors.java` | 93 | sí | código transversal |
| `plataforma/gateway/src/main/java/bo/aportaya/gateway/LimitadorDeTasa.java` | 137 | sí | código transversal |
| `plataforma/gateway/src/main/java/bo/aportaya/gateway/Version.java` | 38 | sí | código transversal |
| `plataforma/gateway/src/main/resources/application-local.yml` | 11 | sí | código transversal |
| `plataforma/gateway/src/main/resources/application-production.yml` | 10 | sí | código transversal |
| `plataforma/gateway/src/main/resources/application-staging.yml` | 15 | sí | código transversal |
| `plataforma/gateway/src/main/resources/application-test.yml` | 5 | sí | código transversal |
| `plataforma/gateway/src/main/resources/application.yml` | 62 | sí | código transversal |
| `plataforma/gateway/src/main/resources/rutas.yml` | 154 | sí | código transversal |
| `plataforma/gateway/src/test/java/bo/aportaya/gateway/ArranqueRateLimitGatewayTest.java` | 126 | parcial (nombres y aserciones) | prueba de plataforma (ejecutada en esta sesión) |
| `servicios/auditoria/.jqwik-database` | 1 | sí | código del servicio |
| `servicios/auditoria/README.md` | 95 | sí | código del servicio |
| `servicios/auditoria/build.gradle.kts` | 46 | sí | código del servicio |
| `servicios/auditoria/descriptor.yml` | 30 | sí | código del servicio |
| `servicios/auditoria/gradle.lockfile` | 206 | no | dependencias fijadas |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/Aplicacion.java` | 21 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/.gitkeep` | 0 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/CU07EjercerDerechos.java` | 176 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/CU130GobernarFichaDeTabla.java` | 146 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/CU131RevisarColumna.java` | 133 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/CU58DescargarExportacion.java` | 110 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/CU58EjecutarReporte.java` | 279 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/CU98PublicarTablero.java` | 204 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/CatalogoDeDatosVigente.java` | 39 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/ConsultarBitacora.java` | 104 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/ConsultarCatalogoDeDatos.java` | 296 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/ConsultarHistorialDeReportes.java` | 118 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/ConsultarIncidentesOperativos.java` | 45 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/ConsultasDeTrafico.java` | 44 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/EndpointsDelDia.java` | 102 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/GestionarCatalogosOperativos.java` | 56 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/IndicadoresDeOperacion.java` | 96 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/OutboxPorServicio.java` | 102 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/RegistroDeDisponibilidad.java` | 61 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/RegistroDeIndicadores.java` | 105 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/SaludDeServicios.java` | 76 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/aplicacion/TablaIndexada.java` | 75 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/consumo/ConsumidorDeIndicadoresKafka.java` | 96 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/.gitkeep` | 0 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/DisponibilidadDelServicio.java` | 58 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/EstrategiaDeAnonimizacion.java` | 11 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/FamiliasVisibles.java` | 35 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/FichaDeGobierno.java` | 46 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/HuellaDeResultado.java` | 52 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/IncidenteOperativo.java` | 20 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/OpcionDeRetencion.java` | 6 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/OutboxDeServicio.java` | 15 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/ParametrosDeReporte.java` | 66 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/RetencionLegal.java` | 63 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/RevisionDeColumna.java` | 29 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/SaludDeServicio.java` | 19 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/SentidoDeMeta.java` | 24 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/TipoDeSolicitud.java` | 40 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/dominio/Variacion.java` | 48 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/.gitkeep` | 0 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/AnonimizacionRepositorio.java` | 64 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/BitacoraRepositorio.java` | 175 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/CatalogosOperativosRepositorio.java` | 230 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/ConsultaPrometheus.java` | 90 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/DisponibilidadRepositorio.java` | 88 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/EjecutorDeConsulta.java` | 134 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/FichaGobiernoRepositorio.java` | 111 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/HistorialDeReportesRepositorio.java` | 118 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/IncidentesOperativosRepositorio.java` | 72 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/IndicadorRepositorio.java` | 202 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/PoliticaRetencionRepositorio.java` | 65 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/ReporteRepositorio.java` | 284 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/RevisionColumnaRepositorio.java` | 104 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/SolicitudDatosRepositorio.java` | 65 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/infraestructura/SondaDeSalud.java` | 89 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/trabajos/.gitkeep` | 0 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/trabajos/MuestreoDeDisponibilidad.java` | 37 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/web/.gitkeep` | 0 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/web/AuditoriaController.java` | 164 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/web/IndicadoresController.java` | 254 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/web/MapeoDeCatalogo.java` | 208 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/web/MapeoDeReportes.java` | 40 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/web/MapeoDeSistemas.java` | 114 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/web/MapeoDeTablero.java` | 66 | sí | código del servicio |
| `servicios/auditoria/src/main/java/bo/aportaya/auditoria/web/ReportesController.java` | 128 | sí | código del servicio |
| `servicios/auditoria/src/main/resources/application.yml` | 72 | sí | código del servicio |
| `servicios/auditoria/src/main/resources/catalogo/catalogo-datos.json` | 86568 | inventario + grep | catálogo generado |
| `servicios/auditoria/src/main/resources/catalogo/catalogo-operaciones.json` | 45320 | inventario + grep | catálogo generado |
| `servicios/auditoria/src/main/resources/catalogo/catalogos-operativos.json` | 343 | sí | código del servicio |
| `servicios/auditoria/src/main/resources/openapi/auditoria.yaml` | 1501 | parcial (rutas y esquemas citados) | contrato OpenAPI |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/ArquitecturaTest.java` | 60 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/ArranqueTest.java` | 64 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/AutorizacionNegativaTest.java` | 102 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/BarridoTest.java` | 28 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/BaseDeAuditoria.java` | 169 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CU07RechazosTest.java` | 94 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CU07Test.java` | 99 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CU130Test.java` | 157 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CU131Test.java` | 144 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CU58HistorialTest.java` | 196 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CU58RechazosTest.java` | 171 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CU58SemillasTest.java` | 141 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CU58Test.java` | 194 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CU98RechazosTest.java` | 135 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CU98Test.java` | 102 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CURegistroDeIndicadoresTest.java` | 109 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CalendarioVacio.java` | 23 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/CatalogosOperativosRepositorioTest.java` | 189 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/DisponibilidadRepositorioTest.java` | 102 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/FixturaDeAuditoria.java` | 209 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/FixturaDeReportes.java` | 124 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/IncidentesOperativosRepositorioTest.java` | 109 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/aplicacion/CatalogoDeDatosVigenteTest.java` | 34 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/aplicacion/ConsultarCatalogoDeDatosTest.java` | 228 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/aplicacion/OutboxPorServicioTest.java` | 109 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/aplicacion/SaludDeServiciosTest.java` | 90 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/consumo/ConsumidorDeIndicadoresKafkaTest.java` | 87 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/dominio/DisponibilidadDelServicioTest.java` | 69 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/dominio/FamiliasVisiblesTest.java` | 38 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/dominio/SentidoDeMetaTest.java` | 47 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/dominio/VariacionTest.java` | 68 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/infraestructura/PrometheusDobleTest.java` | 168 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/infraestructura/SondaDeSaludTest.java` | 112 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/trabajos/MuestreoDeDisponibilidadTest.java` | 49 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/BitacoraControllerWebTest.java` | 131 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/CatalogoDeDatosWebTest.java` | 117 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/CatalogoPorPartesWebTest.java` | 240 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/CatalogosOperativosControllerWebTest.java` | 226 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/CuerpoEstrictoWebTest.java` | 59 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/DerechosControllerWebTest.java` | 140 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/FichaDeGobiernoWebTest.java` | 175 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/IncidentesOperativosWebTest.java` | 133 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/IndicadoresControllerWebTest.java` | 280 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/OutboxPorServicioWebTest.java` | 97 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/ReportesControllerWebTest.java` | 171 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/ReportesHistorialWebTest.java` | 150 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/RevisionDeColumnaWebTest.java` | 146 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/SaludDeServiciosWebTest.java` | 139 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/java/bo/aportaya/auditoria/web/SeguridadWebTest.java` | 73 | no (grep); ejecutada | prueba del servicio |
| `servicios/auditoria/src/test/resources/fixtures/.gitkeep` | 0 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/.jqwik-database` | 1 | sí | configuración/descriptor |
| `servicios/erp/README.md` | 69 | sí | configuración/descriptor |
| `servicios/erp/build.gradle.kts` | 45 | sí | configuración/descriptor |
| `servicios/erp/descriptor.yml` | 30 | sí | configuración/descriptor |
| `servicios/erp/gradle.lockfile` | 206 | no | dependencias fijadas |
| `servicios/erp/src/main/java/bo/aportaya/erp/Aplicacion.java` | 21 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/CU100AbrirCerrarPeriodo.java` | 262 | sí | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/CU101Presupuestar.java` | 183 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/CU102AltaDeTercero.java` | 151 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/CU103FacturaDeProveedor.java` | 287 | sí | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/CU104CobrarCuenta.java` | 140 | sí | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/CU105DepreciarActivo.java` | 205 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/CU106DescargarEstadoFinanciero.java` | 77 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/CU106GenerarEstadoFinanciero.java` | 173 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/ConsultarListadosDeErp.java` | 219 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/FilasDeErp.java` | 85 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/ReintentoDePago.java` | 52 | sí | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/dominio/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/erp/src/main/java/bo/aportaya/erp/dominio/CuadreContable.java` | 81 | sí | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/dominio/Depreciacion.java` | 53 | sí | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/dominio/DocumentoDeEstadoFinanciero.java` | 57 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/infraestructura/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/erp/src/main/java/bo/aportaya/erp/infraestructura/ActivosRepositorio.java` | 97 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/infraestructura/CierresDelEjercicioRepositorio.java` | 62 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/infraestructura/CobranzasRepositorio.java` | 108 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/infraestructura/ComprasRepositorio.java` | 276 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/infraestructura/EstadosFinancierosRepositorio.java` | 30 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/infraestructura/ListadosDeErpRepositorio.java` | 142 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/infraestructura/ListadosDeTercerosRepositorio.java` | 182 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/infraestructura/PeriodoRepositorio.java` | 279 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/infraestructura/PresupuestoRepositorio.java` | 220 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/trabajos/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/erp/src/main/java/bo/aportaya/erp/web/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/erp/src/main/java/bo/aportaya/erp/web/ErpController.java` | 298 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/web/ListadosDeErpController.java` | 123 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/web/MapeoDeErp.java` | 298 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/web/MapeoDeListados.java` | 101 | parcial / grep | código del servicio |
| `servicios/erp/src/main/java/bo/aportaya/erp/web/PermisosDeContabilidad.java` | 22 | parcial / grep | código del servicio |
| `servicios/erp/src/main/resources/application.yml` | 32 | sí | configuración/descriptor |
| `servicios/erp/src/main/resources/openapi/erp.yaml` | 1187 | parcial (rutas y esquemas citados) | contrato OpenAPI |
| `servicios/erp/src/test/java/bo/aportaya/erp/ArquitecturaTest.java` | 60 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/ArranqueTest.java` | 60 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/BarridoTest.java` | 28 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/BaseDeErp.java` | 119 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU100PeriodosTest.java` | 137 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU100RechazosTest.java` | 128 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU100Test.java` | 200 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU101ListadoTest.java` | 145 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU101RechazosTest.java` | 99 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU101Test.java` | 243 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU102ListadoTest.java` | 112 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU102RechazosTest.java` | 62 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU102Test.java` | 193 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU103IdempotenciaTest.java` | 224 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU103ListadoTest.java` | 115 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU103RechazosTest.java` | 207 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU103Test.java` | 252 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU104ListadoTest.java` | 133 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU104RechazosTest.java` | 128 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU104Test.java` | 216 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU105ListadoTest.java` | 149 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU105RechazosTest.java` | 129 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU105Test.java` | 200 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU106DescargaTest.java` | 102 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU106RechazosTest.java` | 134 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/CU106Test.java` | 211 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/EscenarioDeFactura.java` | 83 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/FixturaDeErp.java` | 246 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/web/ErpControllerWebTest.java` | 249 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/web/ErpPeriodosWebTest.java` | 236 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/web/ListadosDeErpWebTest.java` | 284 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/web/PagoDeFacturaWebTest.java` | 252 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/web/RegistroDeFacturaWebTest.java` | 170 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/java/bo/aportaya/erp/web/SeguridadWebTest.java` | 55 | no (grep); ejecutada | prueba del servicio |
| `servicios/erp/src/test/resources/fixtures/.gitkeep` | 0 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/README.md` | 146 | sí | configuración/descriptor |
| `servicios/inversiones/build.gradle.kts` | 37 | sí | configuración/descriptor |
| `servicios/inversiones/descriptor.yml` | 30 | sí | configuración/descriptor |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/Aplicacion.java` | 27 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/AplicadorDeInstrucciones.java` | 230 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CU120Catalogo.java` | 204 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CU121OrdenarInversion.java` | 245 | sí | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CU122ConfirmarPosicion.java` | 299 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CU123Devengo.java` | 82 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CU123Posiciones.java` | 279 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CU124SolicitarRescate.java` | 201 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CU125LiquidarRescate.java` | 268 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CU126DistribuirIntereses.java` | 142 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CU127ComisionPorResultado.java` | 104 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CU128ReporteFiscal.java` | 259 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CU129SimularCaja.java` | 106 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/CalculoDeLiquidacion.java` | 141 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/ConfirmacionDeSuscripcion.java` | 41 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/EjecucionDeCorridas.java` | 158 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/Errores.java` | 19 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/ExportacionFiscal.java` | 84 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/IndicadoresDeInversion.java` | 36 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/OperacionesProgramables.java` | 203 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/Transaccionar.java` | 33 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/VistaComprobante.java` | 9 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/VistaCorrida.java` | 32 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/VistaDistribucion.java` | 43 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/VistaOrden.java` | 49 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/aplicacion/VistaRescate.java` | 52 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/ComisionDeExito.java` | 43 | sí | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/Condiciones.java` | 51 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/CorteDeOrden.java` | 42 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/Descomposicion.java` | 89 | sí | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/DevengoDeDpf.java` | 39 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/OrigenDatos.java` | 14 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/PoliticaDeReintento.java` | 50 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/SimulacionDeCaja.java` | 193 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/TextoDeCondiciones.java` | 83 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/TipoProducto.java` | 6 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/ValoracionDeCuotas.java` | 50 | sí | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/puertos/AliadoDeInversion.java` | 111 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/dominio/puertos/LibroDelTitular.java` | 70 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/CatalogoRepositorio.java` | 245 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/ComprobanteRepositorio.java` | 215 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/CorridaRepositorio.java` | 155 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/DistribucionRepositorio.java` | 101 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/GuardiaDeProduccion.java` | 37 | sí | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/IndicadoresDeInversionRepositorio.java` | 31 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/InstruccionRepositorio.java` | 157 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/OrdenRepositorio.java` | 207 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/PosicionRepositorio.java` | 215 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/RescateRepositorio.java` | 167 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/Sql.java` | 22 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/clientes/AliadoSimuladoHttp.java` | 261 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/clientes/ConfiguracionDeCredencialDeServicio.java` | 31 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/clientes/LecturaDeRespuestas.java` | 93 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/infraestructura/clientes/LibroPorHttp.java` | 251 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/trabajos/DevengoDiario.java` | 55 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/trabajos/ReintentoDeInstrucciones.java` | 65 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/trabajos/SincronizacionDelAliado.java` | 65 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/web/AnalisisDeLaInversion.java` | 184 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/web/InversionesController.java` | 261 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/web/MapeoDeInversiones.java` | 190 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/java/bo/aportaya/inversiones/web/OperacionesDeLaInversion.java` | 89 | parcial / grep | código del servicio |
| `servicios/inversiones/src/main/resources/application-local.yml` | 7 | sí | configuración/descriptor |
| `servicios/inversiones/src/main/resources/application-production.yml` | 7 | sí | configuración/descriptor |
| `servicios/inversiones/src/main/resources/application-staging.yml` | 7 | sí | configuración/descriptor |
| `servicios/inversiones/src/main/resources/application-test.yml` | 7 | sí | configuración/descriptor |
| `servicios/inversiones/src/main/resources/application.yml` | 99 | sí | configuración/descriptor |
| `servicios/inversiones/src/main/resources/openapi/inversiones.yaml` | 882 | parcial (rutas y esquemas citados) | contrato OpenAPI |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/AliadoDoble.java` | 272 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/AliadoSimuladoContratoTest.java` | 145 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/AliadoSimuladoFlujosContratoTest.java` | 96 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/ArquitecturaTest.java` | 60 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/ArranqueTest.java` | 166 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/BarridoTest.java` | 28 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/BaseDeAnalisis.java` | 75 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/BaseDeInversiones.java` | 262 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/BaseDeLiquidacion.java` | 52 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/BaseDeRescates.java` | 33 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/BaseDelLibroHttp.java` | 88 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU120OperacionesTest.java` | 104 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU120Test.java` | 195 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU121ConcurrenciaTest.java` | 91 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU121Test.java` | 249 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU122ReintentoTest.java` | 272 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU122Test.java` | 260 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU123Test.java` | 278 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU123TrabajosTest.java` | 152 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU124FondoTest.java` | 228 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU124Test.java` | 122 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU125ComisionTest.java` | 138 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU125LibroTest.java` | 227 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU125Test.java` | 128 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU126Test.java` | 206 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU127Test.java` | 177 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU128Test.java` | 214 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CU129Test.java` | 190 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/CUIndicadoresDeInversionTest.java` | 71 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/LibroDoble.java` | 199 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/LibroPorHttpContratoTest.java` | 172 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/LibroPorHttpCredencialContratoTest.java` | 83 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/LibroPorHttpPactoContratoTest.java` | 235 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/OrdenSagaTest.java` | 211 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/ProductosDePrueba.java` | 85 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/ReintentoDelRelojSagaTest.java` | 100 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/ServidorDelAliadoSimulado.java` | 123 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/dominio/CalculosDeDpfTest.java` | 120 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/dominio/CalculosDeFondoTest.java` | 266 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/infraestructura/ConfiguracionDelAliadoTest.java` | 88 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/web/BaseWebInversiones.java` | 91 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/web/InversionesAnalisisWebTest.java` | 284 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/web/InversionesControllerWebTest.java` | 276 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/java/bo/aportaya/inversiones/web/SeguridadWebTest.java` | 60 | no (grep); ejecutada | prueba del servicio |
| `servicios/inversiones/src/test/resources/fixtures/.gitkeep` | 0 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/.jqwik-database` | 1 | sí | código del servicio |
| `servicios/notificaciones/README.md` | 62 | sí | código del servicio |
| `servicios/notificaciones/build.gradle.kts` | 44 | sí | código del servicio |
| `servicios/notificaciones/descriptor.yml` | 30 | sí | código del servicio |
| `servicios/notificaciones/gradle.lockfile` | 206 | no | dependencias fijadas |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/Aplicacion.java` | 21 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/ConfiguracionDeNotificaciones.java` | 40 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/aplicacion/.gitkeep` | 0 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/aplicacion/CU80DespacharNotificacion.java` | 205 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/aplicacion/CU81ProgramarRecordatorios.java` | 159 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/aplicacion/CU82ProcesarRespuesta.java` | 146 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/aplicacion/CU83DespacharLote.java` | 160 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/aplicacion/ConsultarMensajeria.java` | 190 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/aplicacion/ConsultarSupresion.java` | 37 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/aplicacion/GestionarAvisosInternos.java` | 95 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/aplicacion/SolicitarAviso.java` | 77 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/.gitkeep` | 0 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/Canal.java` | 38 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/EleccionDeProveedor.java` | 60 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/EscaleraDeRecordatorios.java` | 75 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/EsperaDeReintento.java` | 63 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/FirmaDeWebhook.java` | 50 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/IntencionEntrante.java` | 101 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/PlantillaRenderizada.java` | 73 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/PlataformaDeMensajeria.java` | 27 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/dominio/VentanaDeEnvio.java` | 50 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/.gitkeep` | 0 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/AuditoriaDeMensajeriaRepositorio.java` | 159 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/AvisoInternoRepositorio.java` | 96 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/EnvioRepositorio.java` | 192 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/NotificacionRepositorio.java` | 128 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/PlantillaRepositorio.java` | 54 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/PlataformaDeMensajeriaRepositorio.java` | 56 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/ProgramacionRepositorio.java` | 50 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/ProveedorRepositorio.java` | 64 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/RespuestaRepositorio.java` | 58 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/SupresionRepositorio.java` | 53 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/infraestructura/simulado/MensajeriaSimulada.java` | 32 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/trabajos/.gitkeep` | 0 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/web/.gitkeep` | 0 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/web/LimiteDeConsultas.java` | 61 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/web/MapeoDeMensajeria.java` | 81 | sí | código del servicio |
| `servicios/notificaciones/src/main/java/bo/aportaya/notificaciones/web/NotificacionesController.java` | 282 | sí | código del servicio |
| `servicios/notificaciones/src/main/resources/application.yml` | 54 | sí | código del servicio |
| `servicios/notificaciones/src/main/resources/openapi/notificaciones.yaml` | 605 | parcial (rutas y esquemas citados) | contrato OpenAPI |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/ArquitecturaTest.java` | 60 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/ArranqueTest.java` | 67 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/AvisoInternoRepositorioTest.java` | 148 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/BarridoTest.java` | 28 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/BaseDeNotificaciones.java` | 164 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/CU80PlantillasTest.java` | 139 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/CU80RechazosTest.java` | 102 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/CU80Test.java` | 248 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/CU81Test.java` | 285 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/CU82RechazosTest.java` | 103 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/CU82Test.java` | 231 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/CU83EnviosTest.java` | 216 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/CU83RechazosTest.java` | 95 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/CU83Test.java` | 280 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/FixturaDeNotificaciones.java` | 143 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/PlataformaDeMensajeriaRepositorioTest.java` | 140 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/web/AlarmasOperativasWebTest.java` | 147 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/web/AutorizacionNegativaWebTest.java` | 190 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/web/AvisosInternosWebTest.java` | 189 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/web/MensajeriaAuditoriaWebTest.java` | 205 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/web/NotificacionesControllerWebTest.java` | 260 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/web/PlataformaDeMensajeriaWebTest.java` | 88 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/java/bo/aportaya/notificaciones/web/SeguridadWebTest.java` | 39 | no (grep); ejecutada | prueba del servicio |
| `servicios/notificaciones/src/test/resources/fixtures/.gitkeep` | 0 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/.jqwik-database` | 1 | sí | configuración/descriptor |
| `servicios/publicidad/README.md` | 69 | sí | configuración/descriptor |
| `servicios/publicidad/build.gradle.kts` | 44 | sí | configuración/descriptor |
| `servicios/publicidad/descriptor.yml` | 30 | sí | configuración/descriptor |
| `servicios/publicidad/gradle.lockfile` | 206 | no | dependencias fijadas |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/Aplicacion.java` | 21 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/aplicacion/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/aplicacion/CU110AltaDeAnunciante.java` | 198 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/aplicacion/CU111CrearCampana.java` | 210 | sí | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/aplicacion/CU112ModerarPieza.java` | 131 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/aplicacion/CU113EntregarAnuncio.java` | 195 | sí | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/aplicacion/CU114LiquidarPublicidad.java` | 128 | sí | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/aplicacion/ConsultarListadosDePublicidad.java` | 161 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/dominio/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/dominio/ConsumoDelPeriodo.java` | 32 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/dominio/DecisionDeModeracion.java` | 32 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/dominio/LimiteDeGasto.java` | 33 | sí | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/dominio/ReferenciaDelAnunciante.java` | 56 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/dominio/SubastaDelEspacio.java` | 85 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/infraestructura/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/infraestructura/AnuncianteRepositorio.java` | 149 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/infraestructura/CampanaRepositorio.java` | 189 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/infraestructura/CreativaRepositorio.java` | 101 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/infraestructura/EntregaRepositorio.java` | 249 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/infraestructura/FacturacionRepositorio.java` | 116 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/infraestructura/ListadosDePublicidadRepositorio.java` | 168 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/trabajos/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/web/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/web/ListadosDePublicidadWeb.java` | 120 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/web/MapeoDePublicidad.java` | 103 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/java/bo/aportaya/publicidad/web/PublicidadController.java` | 298 | parcial / grep | código del servicio |
| `servicios/publicidad/src/main/resources/application.yml` | 32 | sí | configuración/descriptor |
| `servicios/publicidad/src/main/resources/openapi/publicidad.yaml` | 816 | parcial (rutas y esquemas citados) | contrato OpenAPI |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/ArquitecturaTest.java` | 60 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/ArranqueTest.java` | 65 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/BarridoTest.java` | 28 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/BaseDePublicidad.java` | 102 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU110ListadoTest.java` | 53 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU110RechazosTest.java` | 99 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU110Test.java` | 228 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU111ListadoTest.java` | 60 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU111RechazosTest.java` | 73 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU111Test.java` | 196 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU112ListadoTest.java` | 63 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU112RechazosTest.java` | 128 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU112Test.java` | 206 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU113RechazosTest.java` | 77 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU113Test.java` | 226 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU114RechazosTest.java` | 78 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/CU114Test.java` | 214 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/EscenarioDeCampana.java` | 90 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/EscenarioDeListados.java` | 51 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/FixturaDePublicidad.java` | 199 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/web/PublicidadControllerWebTest.java` | 250 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/web/PublicidadListadosWebTest.java` | 202 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/java/bo/aportaya/publicidad/web/SeguridadWebTest.java` | 44 | no (grep); ejecutada | prueba del servicio |
| `servicios/publicidad/src/test/resources/fixtures/.gitkeep` | 0 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/.jqwik-database` | 1 | sí | configuración/descriptor |
| `servicios/tarifas/README.md` | 70 | sí | configuración/descriptor |
| `servicios/tarifas/build.gradle.kts` | 45 | sí | configuración/descriptor |
| `servicios/tarifas/descriptor.yml` | 30 | sí | configuración/descriptor |
| `servicios/tarifas/gradle.lockfile` | 206 | no | dependencias fijadas |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/Aplicacion.java` | 21 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/ConfiguracionDeTarifas.java` | 30 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/CU30CotizarComision.java` | 233 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/CU31DevengarComision.java` | 257 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/CU32EmitirFactura.java` | 230 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/CU33DevolverComision.java` | 175 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/CU34PublicarTarifario.java` | 207 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/CU35CerrarLiquidacion.java` | 151 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/CU36ResolverPrecio.java` | 127 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/CongelarTarifaDelGrupo.java` | 103 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/ConsultarCostosDeProveedor.java` | 27 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/ConsultarTarifarioVigente.java` | 39 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/aplicacion/ConsultarTarifarios.java` | 120 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/consumo/ConsumidorDeGruposCreadosKafka.java` | 82 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/CalculoDeComision.java` | 154 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/CodigoUnicoDeFactura.java` | 38 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/CostoDeProveedor.java` | 8 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/EntradaEnVigencia.java` | 26 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/EstadoDelDevengo.java` | 62 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/MaximoDevolvible.java` | 21 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/MetodoDeCalculo.java` | 54 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/PeriodoContable.java` | 38 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/PlazoDeContingencia.java` | 27 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/PoliticaDeRedondeo.java` | 59 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/SegmentoAplicable.java` | 63 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/VigenciaDeCotizacion.java` | 23 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/dominio/puertos/ServicioDeImpuestos.java` | 47 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/CambioTarifarioRepositorio.java` | 185 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/CostosDeProveedorRepositorio.java` | 32 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/CotizacionRepositorio.java` | 131 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/DevengoRepositorio.java` | 222 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/DevolucionRepositorio.java` | 107 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/FacturaRepositorio.java` | 188 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/HistorialDeTarifariosRepositorio.java` | 112 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/LiquidacionRepositorio.java` | 131 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/SegmentoRepositorio.java` | 83 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/TarifaCongeladaRepositorio.java` | 88 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/TarifarioRepositorio.java` | 231 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/infraestructura/simulado/ServicioDeImpuestosSimulado.java` | 61 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/trabajos/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/web/.gitkeep` | 0 | sí | configuración/descriptor |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/web/ComisionesController.java` | 186 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/web/FacturasController.java` | 54 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/web/MapeoDeTarifarios.java` | 54 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/web/MapeoDeTarifas.java` | 41 | sí | código del servicio |
| `servicios/tarifas/src/main/java/bo/aportaya/tarifas/web/TarifasController.java` | 231 | parcial / grep | código del servicio |
| `servicios/tarifas/src/main/resources/application.yml` | 56 | sí | configuración/descriptor |
| `servicios/tarifas/src/main/resources/openapi/tarifas.yaml` | 785 | parcial (rutas y esquemas citados) | contrato OpenAPI |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/ArquitecturaTest.java` | 60 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/ArranqueTest.java` | 64 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/BarridoTest.java` | 28 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/BaseDeTarifas.java` | 156 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU20CongelarTarifaTest.java` | 218 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU30RechazosTest.java` | 195 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU30Test.java` | 248 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU31RechazosTest.java` | 242 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU31Test.java` | 267 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU32RechazosTest.java` | 153 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU32Test.java` | 225 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU33RechazosTest.java` | 233 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU33Test.java` | 285 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU34RechazosTest.java` | 213 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU34TarifariosTest.java` | 181 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU34Test.java` | 249 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU35RechazosTest.java` | 114 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU35Test.java` | 225 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU36RechazosTest.java` | 165 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CU36Test.java` | 258 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/CostosDeProveedorRepositorioTest.java` | 88 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/FixturaDeFacturacion.java` | 202 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/FixturaDeGrupo.java` | 143 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/FixturaDeTarifas.java` | 234 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/consumo/ConsumidorDeGruposCreadosKafkaTest.java` | 76 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/web/ComisionesControllerWebTest.java` | 201 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/web/CostosDeProveedorWebTest.java` | 87 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/web/FacturasControllerWebTest.java` | 135 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/web/SeguridadWebTest.java` | 60 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/web/TarifariosListadoWebTest.java` | 178 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/java/bo/aportaya/tarifas/web/TarifasControllerWebTest.java` | 249 | no (grep); ejecutada | prueba del servicio |
| `servicios/tarifas/src/test/resources/fixtures/.gitkeep` | 0 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/.jqwik-database` | 1 | sí | código del servicio |
| `servicios/transparencia/README.md` | 81 | sí | código del servicio |
| `servicios/transparencia/build.gradle.kts` | 44 | sí | código del servicio |
| `servicios/transparencia/descriptor.yml` | 30 | sí | código del servicio |
| `servicios/transparencia/gradle.lockfile` | 206 | no | dependencias fijadas |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/Aplicacion.java` | 21 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/ConfiguracionDeTransparencia.java` | 73 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU61VerificarSorteo.java` | 169 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU70RegistrarEventoReputacion.java` | 186 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU71RecalcularPuntaje.java` | 178 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU72SellarBloque.java` | 219 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU73VerificarCadena.java` | 146 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU74EvaluarInsignias.java` | 158 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU75EmitirCertificado.java` | 277 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU76PublicarResena.java` | 224 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/CU97EvaluarRiesgo.java` | 228 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/ConsultarEtapasDePersonas.java` | 111 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/ConsultarPerfilEnGrupo.java` | 38 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/ConsultarPuntaje.java` | 123 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/ListarBloques.java` | 56 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/aplicacion/ListarEtapas.java` | 50 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/CadenaDeBloques.java` | 159 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/CertificadoVerificable.java` | 129 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/ContenidoCanonico.java` | 102 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/CriterioDeInsignia.java` | 147 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/EtapasDeReputacion.java` | 81 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/IndicadoresDeReputacion.java` | 21 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/ModeracionDeResena.java` | 75 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/PerfilDeReputacionEnGrupo.java` | 49 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/PuntajeDeReputacion.java` | 179 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/SenalDeRiesgo.java` | 170 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/puertos/PaquetesDeSorteo.java` | 25 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/CadenaRepositorio.java` | 209 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/CertificadoRepositorio.java` | 117 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/EtapaRepositorio.java` | 71 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/InsigniaRepositorio.java` | 69 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/ModeloRepositorio.java` | 104 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/PerfilEnGrupoRepositorio.java` | 66 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/ReputacionRepositorio.java` | 279 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/ResenaRepositorio.java` | 69 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/RiesgoRepositorio.java` | 105 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/SnapshotRepositorio.java` | 44 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/clientes/GruposPorHttp.java` | 40 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/infraestructura/clientes/PaquetePorHttp.java` | 49 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/trabajos/.gitkeep` | 0 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/web/.gitkeep` | 0 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/web/MapeoDeReputacion.java` | 262 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/web/MapeoDeTransparencia.java` | 65 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/web/PublicoController.java` | 161 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/web/ReputacionController.java` | 250 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/web/RutasDeEtapas.java` | 119 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/web/RutasDePerfilEnGrupo.java` | 63 | sí | código del servicio |
| `servicios/transparencia/src/main/java/bo/aportaya/transparencia/web/VerificarController.java` | 54 | sí | código del servicio |
| `servicios/transparencia/src/main/resources/application.yml` | 67 | sí | código del servicio |
| `servicios/transparencia/src/main/resources/openapi/transparencia.yaml` | 1182 | parcial (rutas y esquemas citados) | contrato OpenAPI |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/ArquitecturaTest.java` | 60 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/ArranqueTest.java` | 67 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/BarridoTest.java` | 28 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/BaseDeEtapas.java` | 45 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/BaseDeTransparencia.java` | 220 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU61RechazosTest.java` | 107 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU61Test.java` | 218 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU70RechazosTest.java` | 128 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU70Test.java` | 228 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU71RechazosTest.java` | 150 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU71Test.java` | 277 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU72RechazosTest.java` | 119 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU72Test.java` | 286 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU73RechazosTest.java` | 181 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU73Test.java` | 241 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU74RechazosTest.java` | 149 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU74Test.java` | 247 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU75RechazosTest.java` | 170 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU75Test.java` | 259 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU76RechazosTest.java` | 205 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU76Test.java` | 283 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU97RechazosTest.java` | 203 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/CU97Test.java` | 291 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/ContenidoCanonicoTest.java` | 45 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/EscenarioDeRiesgo.java` | 40 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/EtapaCatalogoRepositorioTest.java` | 228 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/EtapaRepositorioTest.java` | 179 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/EtapasDePersonasRepositorioTest.java` | 115 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/EtapasDeReputacionTest.java` | 119 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/FixturaDeTransparencia.java` | 145 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/PerfilEnGrupoRepositorioTest.java` | 170 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/infraestructura/clientes/GruposPorHttpPactoContratoTest.java` | 164 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/web/EtapasWebTest.java` | 156 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/web/PerfilEnGrupoWebTest.java` | 142 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/web/PrivacidadDelPuntajeWebTest.java` | 221 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/web/PublicoControllerWebTest.java` | 241 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/web/ReputacionControllerWebTest.java` | 284 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/web/SeguridadWebTest.java` | 78 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/java/bo/aportaya/transparencia/web/VerificarControllerWebTest.java` | 112 | no (grep); ejecutada | prueba del servicio |
| `servicios/transparencia/src/test/resources/fixtures/.gitkeep` | 0 | no (grep); ejecutada | prueba del servicio |
| `sql/00_base/00_extensiones.sql` | 5 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/00_base/01_roles.sql` | 21 | sí | base/semilla/infra |
| `sql/00_base/02_esquemas.sql` | 595 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/00_base/03_permisos.sql` | 284 | sí | base/semilla/infra |
| `sql/10_tablas/01_identidad_usuarios/alcance_invitacion.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/asignacion_rol.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/bloqueo_cuenta.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/consentimiento.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/credencial_acceso.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/direccion_usuario.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/dispositivo.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/documento_identidad.sql` | 39 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/factor_mfa.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/historial_credencial.sql` | 15 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/intento_autenticacion.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/intento_validacion_token.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/perfil_financiero.sql` | 19 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/permiso.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/politica_token.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/preferencia_notificacion.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/referencia_personal.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/reputacion_usuario.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/restriccion_usuario.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/rol.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/rol_permiso.sql` | 12 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/sesion.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/solicitud_baja.sql` | 18 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/token_verificacion.sql` | 63 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/usuario.sql` | 40 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/usuario_interno.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/01_identidad_usuarios/verificacion_kyc.sql` | 42 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/aceptacion_reglamento.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/acuerdo.sql` | 35 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/alta_grupo.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/configuracion_grupo.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/criterio_emparejamiento.sql` | 19 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/cupo.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/decision_ingreso.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/dia_no_habil.sql` | 19 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/enlace_grupo.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/grupo.sql` | 56 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/historial_estado_grupo.sql` | 19 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/invitacion.sql` | 40 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/participante.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/periodo.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/postulacion_emparejamiento.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/propuesta_grupo.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/propuesta_postulacion.sql` | 15 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/reglamento_grupo.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/snapshot_sorteo.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/solicitud_ingreso.sql` | 36 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/solicitud_permuta.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/solicitud_retiro.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/sorteo_turnos.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/sustitucion_administrador.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/traspaso_cupo.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/turno.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/02_grupos_turnos/voto_participante.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/asiento_contable.sql` | 33 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/cierre_diario.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/comprobante_manual.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/conciliacion.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/constancia_pago.sql` | 19 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/cuenta_contable.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/disputa_pago.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/enlace_pago_rapido.sql` | 19 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/excepcion_conciliacion.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/extracto_bancario.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/intento_pago.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/movimiento_bancario.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/movimiento_contable.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/obligacion_aporte.sql` | 53 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/orden_cobro.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/pago.sql` | 50 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/plan_regularizacion.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/politica_mora.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/proveedor_pago.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/qr_cobro.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/reembolso.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/tipo_cambio.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/03_aportes_pagos_qr/webhook_pasarela.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/cesion_derecho.sql` | 42 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/confirmacion_recepcion.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/cuenta_bancaria_beneficiario.sql` | 35 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/deduccion_entrega.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/entrega_fondo.sql` | 55 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/fondeo_entrega.sql` | 45 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/historial_estado_entrega.sql` | 19 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/incidencia_entrega.sql` | 36 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/intento_desembolso.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/oferta_turno.sql` | 40 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/orden_desembolso.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/puja_subasta.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/regla_entrega.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/subasta_cupo.sql` | 47 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/04_entregas_fondo/validacion_pre_entrega.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/aviso_interno.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/bandeja_entrada.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/canal_vinculado.sql` | 33 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/cola_envio.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/cola_muerta.sql` | 18 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/enlace_pago_notificado.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/envio_notificacion.sql` | 48 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/evento_entrega_mensaje.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/evento_notificable.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/lista_supresion.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/notificacion.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/plantilla_mensaje.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/programacion_recordatorio.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/proveedor_mensajeria.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/respuesta_entrante.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/05_notificaciones/version_plantilla.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/alerta_riesgo.sql` | 30 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/bloque_transparencia.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/certificado_reputacion.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/componente_score.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/desbloqueo_etapa.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/etapa.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/evento_reputacion.sql` | 36 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/insignia_logro.sql` | 17 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/insignia_otorgada.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/metrica_grupo.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/modelo_scoring.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/peso_factor.sql` | 19 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/puntaje_reputacion.sql` | 36 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/registro_sellado.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/regla_impacto_evento.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/resena_participante.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/snapshot_reputacion.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/06_transparencia_reputacion/verificacion_publica.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/apelacion_sancion_org.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/capacitacion_organizador.sql` | 19 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/contrato_organizador.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/decision_habilitacion.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/ejecucion_tarea.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/evaluacion_desempeno.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/metrica_organizador.sql` | 18 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/organizador.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/regla_automatizacion.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/requisito_habilitacion.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/sancion_organizador.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/solicitud_organizador.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/07_organizador_automatizacion/tarea_automatizada.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/abono_recuperacion.sql` | 35 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/accion_cobranza.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/acuerdo_quita.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/alerta_temprana.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/apelacion_sancion.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/aval_participante.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/candidato_reemplazo.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/capacidad_respaldo.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/castigo_deuda.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/cobertura_incumplimiento.sql` | 37 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/cobertura_respaldo.sql` | 47 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/cobertura_respaldo_linea.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/descargo_participante.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/deuda_participante.sql` | 41 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/devolucion_fondo.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/disolucion_anticipada.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/ejecucion_aval.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/estrategia_cobranza.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/evidencia_incumplimiento.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/fondo_garantia.sql` | 34 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/gestion_cobranza.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/historial_estado_incumplimiento.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/historial_incumplimiento_usuario.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/liquidacion_participante.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/lista_restriccion_interna.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/matriz_sancion.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/movimiento_fondo.sql` | 33 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/movimiento_reserva.sql` | 38 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/plan_contingencia.sql` | 30 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/politica_cobertura.sql` | 34 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/politica_sancion.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/promesa_pago.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/recuperacion_respaldo.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/reemplazo_participante.sql` | 33 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/registro_incumplimiento.sql` | 65 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/reserva_respaldo.sql` | 40 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/sancion.sql` | 42 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/score_riesgo_incumplimiento.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/08_garantia_incumplimiento/subrogacion.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/alerta_cumplimiento.sql` | 38 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/bitacora_evento.sql` | 69 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/coincidencia_lista.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/definicion_indicador.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/definicion_reporte.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/disponibilidad_servicio_dia.sql` | 19 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/ejecucion_reporte.sql` | 30 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/exportacion_reporte.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/ficha_gobierno_tabla.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/incidente_operativo.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/indicador_kpi.sql` | 33 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/lista_restrictiva_externa.sql` | 16 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/politica_retencion.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/proceso_anonimizacion.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/programacion_reporte.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/registro_acceso_datos.sql` | 45 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/regla_cumplimiento.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/reporte_operacion_sospechosa.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/revision_columna.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/solicitud_datos_personales.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/ticket_soporte.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/09_auditoria_reportes/umbral_operativo.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/bloqueo_saldo.sql` | 39 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/certificado_saldo.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/conciliacion_custodia.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/consumo_limite.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/cuenta_billetera.sql` | 41 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/cuenta_custodia.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/descuadre_custodia.sql` | 34 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/discrepancia_proveedor.sql` | 30 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/estado_cuenta_billetera.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/evaluacion_antifraude.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/evidencia_mfa_consumida.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/instrumento_fondeo.sql` | 37 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/limite_operativo_billetera.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/movimiento_billetera.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/movimiento_custodia.sql` | 33 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/orden_recarga.sql` | 49 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/orden_retiro.sql` | 48 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/politica_billetera.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/qr_transferencia.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/regla_antifraude.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/respuesta_idempotente.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/retencion_saldo.sql` | 35 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/reverso_transaccion.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/saldo_diario_billetera.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/solicitud_cierre_billetera.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/transaccion_billetera.sql` | 57 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/10_billetera_custodia/transferencia_p2p.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/aplicacion_promocion.sql` | 17 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/asignacion_tarifario.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/calculo_impuesto.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/cambio_tarifario.sql` | 30 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/campana_promocional.sql` | 30 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/cargo_comision.sql` | 33 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/catalogo_hecho_generador.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/concepto_tarifa.sql` | 52 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/costo_proveedor_operacion.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/cotizacion_comision.sql` | 34 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/cuenta_por_cobrar_comision.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/datos_facturacion.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/devengo_comision.sql` | 49 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/devolucion_comision.sql` | 35 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/evento_significativo_sin.sql` | 33 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/exencion_comision.sql` | 39 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/factura_electronica.sql` | 53 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/impuesto.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/liquidacion_ingresos.sql` | 34 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/lote_envio_sin.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/nota_credito_debito.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/politica_redondeo.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/regla_tarifa.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/segmento_comercial.sql` | 17 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/simulacion_tarifa.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/tarifa_congelada_grupo.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/11_tarifas_comisiones/tarifario.sql` | 36 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/aceptacion_contrato.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/acta_comite.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/activo_informacion.sql` | 35 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/alerta_monitoreo_lft.sql` | 37 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/beneficiario_final.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/calificacion_riesgo_cliente.sql` | 34 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/capacitacion_cumplimiento.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/caso_investigacion_lft.sql` | 41 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/catalogo_reporte_regulatorio.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/comite_gobierno.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/contrato_adhesion.sql` | 33 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/contrato_tercero.sql` | 39 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/control_interno.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/debida_diligencia.sql` | 36 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/declaracion_origen_fondos.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/declaracion_pep.sql` | 33 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/designacion_regulatoria.sql` | 30 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/desvio_perfil.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/documento_publicado.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/entorno_prueba_regulado.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/envio_regulatorio.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/evaluacion_riesgo_producto.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/evaluacion_tercero.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/evento_riesgo_operativo.sql` | 44 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/expediente_cliente.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/factor_riesgo_evaluado.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/hallazgo_auditoria.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/incidente_seguridad.sql` | 47 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/instancia_reclamo.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/licencia_regulatoria.sql` | 39 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/matriz_riesgo_lft.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/observacion_regulatoria.sql` | 36 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/oficial_cumplimiento.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/perfil_transaccional.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/plan_accion_riesgo.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/plan_continuidad.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/politica_interna.sql` | 34 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/prueba_continuidad.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/prueba_control.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/punto_reclamo.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/reclamo_cliente.sql` | 59 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/registro_operacion_relevante.sql` | 55 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/regla_monitoreo_lft.sql` | 34 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/reporte_regulatorio.sql` | 35 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/requerimiento_autoridad.sql` | 35 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/revision_periodica_kyc.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/12_cumplimiento_asfi/umbral_reporte_uif.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/activo_fijo.sql` | 33 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/asiento_plantilla.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/categoria_activo_fijo.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/centro_costo.sql` | 18 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/cierre_periodo_contable.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/cobro_cuenta_por_cobrar.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/cuenta_por_cobrar.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/depreciacion_activo.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/ejercicio_fiscal.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/estado_financiero_generado.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/factura_proveedor.sql` | 36 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/linea_plantilla_asiento.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/orden_compra.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/pago_a_proveedor.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/partida_presupuestaria.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/periodo_contable.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/presupuesto.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/13_contabilidad_erp/tercero_comercial.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/anunciante.sql` | 23 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/anuncio.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/campana_publicitaria.sql` | 31 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/clic_anuncio.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/conjunto_anuncios.sql` | 32 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/conversion_anuncio.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/cuenta_publicitaria.sql` | 22 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/espacio_publicitario.sql` | 19 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/factura_publicidad.sql` | 28 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/impresion_anuncio.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/pieza_creativa.sql` | 24 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/revision_creativa.sql` | 21 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/segmento_audiencia.sql` | 17 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/14_publicidad_campanas/socio_comercial.sql` | 25 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/comision_exito.sql` | 45 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/comprobante_inversion.sql` | 50 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/conciliacion_interes.sql` | 35 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/consentimiento_inversion.sql` | 20 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/corrida_trabajo.sql` | 29 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/devengo_dpf.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/distribucion_interes.sql` | 45 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/instruccion_libro.sql` | 38 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/orden_inversion.sql` | 40 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/posicion_inversion.sql` | 46 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/producto_inversion.sql` | 27 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/rescate_inversion.sql` | 38 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/valor_cuota.sql` | 26 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/10_tablas/15_inversiones/version_condiciones.sql` | 54 | grep por script (~30 leídas) | DDL de tabla (aplicado en base local) |
| `sql/15_infra/borradores.sql` | 51 | sí | base/semilla/infra |
| `sql/15_infra/columnas_agregadas.sql` | 9 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/15_infra/mensajeria.sql` | 1129 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/15_infra/nulabilidad.sql` | 828 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/20_claves/01_identidad_usuarios.sql` | 180 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/02_grupos_turnos.sql` | 270 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/03_aportes_pagos_qr.sql` | 250 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/04_entregas_fondo.sql` | 200 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/05_notificaciones.sql` | 120 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/06_transparencia_reputacion.sql` | 130 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/07_organizador_automatizacion.sql` | 95 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/08_garantia_incumplimiento.sql` | 595 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/09_auditoria_reportes.sql` | 150 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/10_billetera_custodia.sql` | 330 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/11_tarifas_comisiones.sql` | 335 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/12_cumplimiento_asfi.sql` | 520 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/13_contabilidad_erp.sql` | 205 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/14_publicidad_campanas.sql` | 130 | parseo por script | claves/índices (aplicado en base local) |
| `sql/20_claves/15_inversiones.sql` | 100 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/01_identidad_usuarios.sql` | 125 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/02_grupos_turnos.sql` | 128 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/03_aportes_pagos_qr.sql` | 161 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/04_entregas_fondo.sql` | 134 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/05_notificaciones.sql` | 122 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/06_transparencia_reputacion.sql` | 119 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/07_organizador_automatizacion.sql` | 59 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/08_garantia_incumplimiento.sql` | 275 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/09_auditoria_reportes.sql` | 146 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/10_billetera_custodia.sql` | 215 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/11_tarifas_comisiones.sql` | 191 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/12_cumplimiento_asfi.sql` | 407 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/13_contabilidad_erp.sql` | 113 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/14_publicidad_campanas.sql` | 95 | parseo por script | claves/índices (aplicado en base local) |
| `sql/30_indices/15_inversiones.sql` | 143 | parseo por script | claves/índices (aplicado en base local) |
| `sql/35_append_only/append_only.sql` | 244 | sí | base/semilla/infra |
| `sql/40_reglas/restricciones.sql` | 3526 | parcial (~700 de 3526 líneas) | reglas, triggers y RLS (aplicado y ejecutado) |
| `sql/50_verificacion/prueba_concurrencia.sql` | 121 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/50_verificacion/prueba_humo.sql` | 824 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/50_verificacion/verificaciones.sql` | 145 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/01-plan-de-cuentas.sql` | 149 | sí | base/semilla/infra |
| `sql/60_semillas/02-politicas.sql` | 30 | sí | base/semilla/infra |
| `sql/60_semillas/03-limites-operativos.sql` | 45 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/04-tarifario.sql` | 60 | sí | base/semilla/infra |
| `sql/60_semillas/05-impuestos.sql` | 7 | sí | base/semilla/infra |
| `sql/60_semillas/06-umbrales-uif.sql` | 28 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/07-reportes-regulatorios.sql` | 15 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/08-gobierno-y-licencia.sql` | 43 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/09-reglas-operativas.sql` | 21 | sí | base/semilla/infra |
| `sql/60_semillas/10-roles-y-permisos.sql` | 275 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/11-contratos-de-adhesion.sql` | 9 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/12-calendario-habil.sql` | 47 | sí | base/semilla/infra |
| `sql/60_semillas/13-politicas-de-token.sql` | 39 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/14-proveedores-externos.sql` | 21 | sí | base/semilla/infra |
| `sql/60_semillas/15-eventos-y-plantillas.sql` | 409 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/16-reputacion-y-scoring.sql` | 81 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/17-organizador-y-emparejamiento.sql` | 41 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/18-sanciones-y-cobranza.sql` | 56 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/19-reportes-y-retencion.sql` | 101 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/20-control-interno-y-continuidad.sql` | 81 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/21-contabilidad-y-publicidad.sql` | 23 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/22-cuentas-de-sistema.sql` | 50 | sí | base/semilla/infra |
| `sql/60_semillas/23-indicadores-tableros.sql` | 41 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/24-metadata-comentarios.sql` | 5056 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/60_semillas/sembrar.sql` | 39 | sí | base/semilla/infra |
| `sql/61_dev/01-entorno-tecnico.sql` | 45 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/02-usuarios-y-billeteras.sql` | 158 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/03-grupo-demo.sql` | 208 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/04-fondo-y-cuenta-del-grupo.sql` | 21 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/05-personal-interno-y-gobierno.sql` | 108 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/06-instrumentos-y-recargas.sql` | 124 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/07-aportes-y-entrega.sql` | 234 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/08-cobros-qr-y-conciliacion.sql` | 154 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/09-mora-cobertura-y-cobranza.sql` | 128 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/10-notificaciones-cierre-y-reclamos.sql` | 108 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/11-contabilidad-y-saldos.sql` | 158 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/12-retiro-y-controles.sql` | 103 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/13-cumplimiento-uif.sql` | 120 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/14-identidad-y-sesiones.sql` | 148 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/15-usuarios-dev.sql` | 158 | sí | base/semilla/infra |
| `sql/61_dev/16-campania-cu-principales.sql` | 104 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/17-usuarios-e2e-sinteticos.sql` | 67 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/18-licencia-sandbox-test.sql` | 12 | parcial / grep | esquema o semilla (aplicado en base local) |
| `sql/61_dev/sembrar_dev.sql` | 58 | sí | base/semilla/infra |
| `sql/62_volumen/sembrar_volumen.sql` | 52 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-01.sql` | 1419 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-02.sql` | 1413 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-03.sql` | 1413 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-04.sql` | 1413 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-05.sql` | 1413 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-06.sql` | 1413 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-07.sql` | 1413 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-08.sql` | 1413 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-09.sql` | 1413 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-10.sql` | 1413 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-11.sql` | 1413 | inventario + muestra | datos de volumen sintéticos |
| `sql/62_volumen/volumen-12.sql` | 1413 | inventario + muestra | datos de volumen sintéticos |
| `sql/README.md` | 166 | sí | base/semilla/infra |
| `sql/aplicar.sql` | 448 | parcial / grep | esquema o semilla (aplicado en base local) |

