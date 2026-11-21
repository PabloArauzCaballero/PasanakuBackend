# Anexo L5 — Correcciones por hallazgo

Este anexo convierte en microtareas los hallazgos de `AUDITORIA-transversal-y-apoyo.md` (lote L5, `origin/test` 07fc6971). El informe tiene 88 fichas `### TYA-*` y las 88 están trazadas al final.

Decisiones que aplica (de `2026-11-20-auditoria-consolidado/DECISIONES.md`):
- **Comisión.** Es una comisión fija por servicio, transparente e igual para todos. P-17: no hay descuentos ni exenciones. Su cobro lo confirma un **evento de pago** (el QR confirmado por webhook), nunca una persona.
- **Fondo y aval.** P-01/P-02 adoptan el modelo A: fondo del grupo en cuenta segregada, con subrogación. P-04 elimina el respaldo de la empresa.
- **Reembolsos y reversos.** P-13: «cuatro ojos» reales, con dos personas distintas tomadas del token.
- **Parámetros financieros.** Son configurables y obligatorios, sin valor por defecto.
- **Calificación (tanda 5, pieza D).** Se calcula solo con hechos, sin calificaciones entre personas. La reputación negativa solo nace de un expediente. Se construye sobre `transparencia`.
- **Comprobante manual.** P-07 lo elimina.
- **Billetera.** P-06: la billetera recargada por QR cuenta como pago por QR.
- **Docker.** P-19: corre en la máquina de Pablo.
- **Textos.** P-16: los redacta Claude y los aprueba Pablo.

Todo hallazgo de origen está en el peldaño `DISCOVERED`. La única excepción es TYA-DINERO-001, que se reprodujo. Ninguna microtarea de este anexo se ejecutó.

## Resumen

**Microtareas por fase** (82 en total)

| Fase | Microtareas |
|---|---:|
| F0 | 5 |
| F1 | 26 |
| F2 | 4 |
| F3 | 9 |
| F4 | 38 |

**Hallazgos por fase de su microtarea principal** (la primera que figura en la trazabilidad)

| Fase | S1 | S2 | S3 | S4 | Total |
|---|---:|---:|---:|---:|---:|
| F0 | 0 | 1 | 4 | 0 | 5 |
| F1 | 4 | 17 | 6 | 0 | 27 |
| F2 | 1 | 2 | 1 | 2 | 6 |
| F3 | 0 | 5 | 5 | 0 | 10 |
| F4 | 0 | 9 | 25 | 6 | 40 |
| **Total** | **5** | **34** | **41** | **8** | **88** |

**Hallazgos que se resuelven con otra microtarea.** Cada uno conserva su fila en la trazabilidad.

| Hallazgo | Se resuelve con | Motivo |
|---|---|---|
| TYA-DEFECTO-007 | F1.L5.M14 | P-17: los segmentos de descuento se retiran |
| TYA-DEFECTO-009 | F2.L5.M35 | Tanda 5: las reseñas entre personas se retiran |

**Hallazgos sin acción:** ninguno.

**Ambigüedades abiertas**

| ID | Pregunta | Supuesto que se toma | Afecta a | A quién confirmar |
|---|---|---|---|---|
| D-L5-1 | «Comisión fija por servicio», ¿es un importe fijo por grupo o periodo, o un porcentaje único igual para todos? | Ninguno: es un parámetro obligatorio y su forma (`IMPORTE` o `FRACCION`) la declara la configuración | F3.L5.M36 | Pablo y Cumplimiento |
| D-L5-2 | ¿Quién paga la comisión y en qué momento (con cada cuota, al cobrar el turno o aparte)? | Se cobra como orden de cobro QR propia; no se descuenta del pozo (ADR-051 D-04) | F3.L5.M37 | Pablo |
| D-L5-3 | ¿El tarifario sigue necesitando la aprobación de un comité (AP-06), ahora que la comisión es única? | Se conserva hasta que Cumplimiento diga lo contrario | F1.L5.M28 | Cumplimiento |

---

## F0 — Preparación

### F0.L5.M01 — ADR de comisión fija y cobro por evento, que reemplaza ADR-051 D-02, D-03, D-04 y D-15
- **Hallazgos:** TYA-COHERENCIA-001 (S3).
- **Causa de raíz:** se tomaron decisiones sucesivas sin retirar el material anterior. ADR-051, las plantillas, las cuentas 9.x y la regla `BOLSA_COMPLETA` describen la cobertura empresarial, el fondo mutual heredado y una comisión «no implementada».
- **Corrección de raíz:**
  - Escribir un ADR nuevo en `docs/Arquitectura/` que fije cuatro puntos:
    - (1) la comisión es fija, igual para todos, sin descuentos ni exenciones, y se cobra por orden QR propia;
    - (2) el cobro se confirma solo por el evento de pago;
    - (3) el respaldo de la empresa se elimina;
    - (4) el faltante lo cubre el modelo A, referido al ADR del lote L2.
  - Marcar en ADR-051 las decisiones D-02, D-03, D-04 y D-15 como superadas.
  - Reescribir en su fuente la regla operativa `BOLSA_COMPLETA` (`sql/60_semillas/09-reglas-operativas.sql:6`) y `scripts/generar_catalogo_operaciones.py:30`.
- **CA:** dado el repo, cuando se busca «reserva de respaldo» o «cubierta por el fondo» en los ADR vigentes y en las semillas de reglas, entonces solo aparece en secciones marcadas como superadas.
- **DoD:**
  - `grep` revisado;
  - `python scripts/validar_flujos.py` en verde;
  - revisión de Pablo.
- **Depende de:** F0.L3.M01 y el ADR del modelo A (lote L2).
- **Esfuerzo:** medio.

### F0.L5.M02 — Suite de integración ejecutable en la máquina de Pablo, con informes guardados
- **Hallazgos:** TYA-PRUEBAS-002 (S3).
- **Causa de raíz:** la integración requiere Docker y ninguna sesión lo tiene; 0 de 393 clases de prueba se ejecutaron.
- **Corrección de raíz:**
  - Un `docker compose` de integración (PostgreSQL, Kafka y MinIO) corre en la máquina de Pablo por Remote Control (P-19), con permiso en cada caso.
  - Un script en `scripts/` corre `./gradlew integrationTest` servicio por servicio, uno a la vez (regla 70.1.4), y guarda los informes JUnit en `docs/trabajo/<fecha>/evidencia/`.
- **CA:** dado el entorno de Pablo, cuando se ejecuta el script, entonces cada servicio deja su informe y el resumen cuenta las clases ejecutadas por servicio (más de 0).
- **DoD:** salida pegada del script y un informe por servicio.
- **Depende de:** P-19.
- **Esfuerzo:** medio.

### F0.L5.M03 — Las pruebas de integración corren con el rol del servicio y con las semillas reales
- **Hallazgos:** TYA-PRUEBAS-001 (S2).
- **Causa de raíz:** `plataforma/comun-pruebas` conecta como dueño del esquema, y las fixtures codifican los errores (`'es'`, `COBRADO`). Con eso, los permisos, el append-only y las lecturas cruzadas pasan en las pruebas y fallan en producción.
- **Corrección de raíz:**
  - La base de pruebas de `comun-pruebas` abre la conexión con `svc_<servicio>`.
  - Las fixtures (`FixturaDeNotificaciones`, `FixturaDeFacturacion`, `CU80Test`) usan las semillas reales en lugar de valores armados.
  - `VerificarControllerWebTest` deja de mockear CU75.
- **CA:** dado `AppendOnlyRepositorioTest`, cuando corre con el rol de servicio, entonces el `UPDATE` sobre una tabla sellada falla. Dada la prueba de ERP que lee otro esquema, entonces falla y expone TYA-ACOPLE-001.
- **DoD:** corrida en el entorno de F0.L5.M02, con la lista de las pruebas que pasan a rojo. Esa lista alimenta las microtareas F4.
- **Depende de:** F0.L5.M02.
- **Esfuerzo:** medio.

### F0.L5.M04 — Verificación de deriva entre el modelo, la base y las entidades
- **Hallazgos:** TYA-DATOS-007 (S3).
- **Causa de raíz:**
  - ADR-032 no tiene `migraciones/`.
  - `CREATE TABLE IF NOT EXISTS` y los 811 `DROP NOT NULL` de `sql/15_infra/nulabilidad.sql` hacen que una base existente nunca reciba los endurecimientos, y nada lo detecta (regla 97.2).
- **Corrección de raíz:**
  - Un verificador en `sql/50_verificacion/` (o un script en `scripts/`) compara el modelo con `information_schema` y con jOOQ. Revisa tabla ausente, columna ausente, nulabilidad, `CHECK` y FK.
  - Se ejecuta al aplicar el esquema y en el gate.
  - Se retira `nulabilidad.sql` en favor de declarar la nulabilidad en el modelo.
- **CA:** dada una base aplicada y un `CHECK` nuevo en el modelo, cuando corre el verificador, entonces informa la deriva y sale con código distinto de 0.
- **DoD:** prueba de mutación (agregar un `CHECK` sin aplicar, ver el rojo) con la salida pegada.
- **Depende de:** F0.L5.M02.
- **Esfuerzo:** medio.

### F0.L5.M05 — Pactos verificados del lado productor y campos de destino de dinero obligatorios
- **Hallazgos:** TYA-CONTRATO-005 (S3).
- **Causa de raíz:** el formato propio de pacto no tiene verificador del productor. Además, `aportes-nucleo-financiero-aporte-cobrado.json` (`tipo`) y `nucleo-financiero-garantia-aporte-aplicado.json` (`destino`) son opcionales y su valor por omisión manda el dinero al pozo, y el patrón de importe admite negativos.
- **Corrección de raíz:**
  - Hacer obligatorios los campos `tipo` y `destino`.
  - Cambiar el patrón de importe a `^\d+\.\d{2}$`.
  - Agregar prueba de productor en `grupos`, `entregas` y `nucleo-financiero` para los pactos que la tienen pendiente.
  - Alinear `inversiones-nucleo-financiero.json` con `nucleo-financiero.yaml:1501`.
- **CA:**
  - Dado un evento `aporte_cobrado` sin `tipo`, cuando lo valida el productor, entonces falla.
  - Un importe `-1.00` no valida.
- **DoD:** pruebas de productor en verde, más una prueba de mutación por pacto.
- **Depende de:** F0.L3.M02.
- **Esfuerzo:** medio.

---

## F1 — Seguridad e integridad del mayor

### F1.L5.M06 — El mayor rechaza líneas negativas, ambiguas y tardías
- **Hallazgos:** TYA-DINERO-001 (S1, reproducido).
- **Causa de raíz:** la invariante 91.2.1 se valida solo en el trigger de la cabecera (`tg_asiento_cuadrado`), no en el de la línea, y el signo quedó en manos de la aplicación.
- **Corrección de raíz:**
  - En el modelo de `movimiento_contable` (`sql/10_tablas/03_aportes_pagos_qr/movimiento_contable.sql:10-11`) agregar `CHECK (debe >= 0 AND haber >= 0 AND (debe = 0) <> (haber = 0))`.
  - Agregar un trigger `DEFERRABLE INITIALLY DEFERRED` sobre `movimiento_contable` que revalide el cuadre del asiento de cada línea y rechace las líneas de asientos no creados en la misma transacción.
  - Agregar a `sql/50_verificacion/verificaciones.sql` la consulta de cuadre por asiento.
  - Para lo ya asentado, si hay asientos descuadrados en algún entorno, se corrigen con contra-asiento, nunca con `UPDATE`.
- **CA:** dado un asiento confirmado, cuando se inserta una línea en otra transacción, o una línea con `debe` negativo, o con `debe` y `haber` a la vez, entonces la base lo rechaza y el saldo no cambia.
- **DoD:**
  - repetir el experimento reproducido de la auditoría contra la base real y pegar el rechazo;
  - correr `verificaciones.sql` con cuadre en 0 filas;
  - prueba de integración con el rol `svc_nucleo_financiero`.
- **Depende de:** F0.L5.M04.
- **Esfuerzo:** medio.

### F1.L5.M07 — Unicidad de origen en cada tabla de dinero
- **Hallazgos:** TYA-DINERO-010 (S2).
- **Causa de raíz:** la idempotencia se resolvió en la mensajería (`evento_consumido`) y no en la escritura (91.2.4). Un reproceso duplica el asiento o el movimiento.
- **Corrección de raíz:** en el modelo, `UNIQUE (origen_tipo, origen_id)` en `asiento_contable`, y `clave_idempotencia UNIQUE` en las tablas que siguen vivas tras F2:
  - `movimiento_fondo`
  - `abono_recuperacion`
  - `deduccion_entrega`
  - `reembolso`
  - `retencion_saldo`
  - `devolucion_comision`

  `ejecucion_aval` cae con el retiro del aval (lote L2). Los consumidores capturan `unique_violation` y responden idempotente.
- **CA:** dado el mismo evento de dinero procesado dos veces, después de borrar `evento_consumido`, entonces existe un solo asiento.
- **DoD:**
  - prueba de duplicado de mensaje por consumidor contra la base real;
  - verificación de deriva.
- **Depende de:** F1.L5.M06 y F0.L5.M04.
- **Esfuerzo:** medio.

### F1.L5.M08 — La idempotencia de plataforma reproduce siempre y condiciona la reutilización
- **Hallazgos:** TYA-DINERO-003 (S2).
- **Causa de raíz:** `Idempotencia.java:133-148` tomó «vence la reserva» como «vence el derecho a reproducir», y reutiliza la fila vencida sin precondición. La usan 63 archivos.
- **Corrección de raíz:**
  - Reproducir siempre las respuestas 2xx y 4xx guardadas.
  - Solo una reserva en curso vencida, sin respuesta, se reutiliza, con `UPDATE … WHERE codigo_http IS NULL AND expira_en = :previa` y control de filas afectadas = 1.
  - La vigencia (`ConfiguracionComunWeb.java:54`) pasa a ser un parámetro explícito.
- **CA:** dada una clave con respuesta guardada hace más de 24 h, cuando se reintenta, entonces se devuelve la respuesta original sin ejecutar. Dos reintentos concurrentes sobre una reserva vencida ejecutan una sola vez.
- **DoD:** prueba de concurrencia real con dos conexiones y prueba con reloj inyectado.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F1.L5.M09 — `Idempotency-Key` cableada en todas las mutaciones de los servicios de apoyo
- **Hallazgos:** TYA-DINERO-004 (S2).
- **Causa de raíz:** el contrato exige la cabecera, pero `transparencia`, `notificaciones`, `auditoria`, `tarifas`, `erp` y `publicidad` no usan `Idempotencia`. Además, deduplican con SELECT seguido de INSERT.
- **Corrección de raíz:**
  - Usar el componente de F1.L5.M08 en cada mutación de los controladores citados en la ficha.
  - Donde la operación no deba tener la cabecera, quitarla del contrato con versión.
  - Los `CU30:76-87` y `CU31:76-81` capturan `unique_violation`.
- **CA:** dada cualquier mutación de esos servicios repetida con la misma clave, entonces no hay un segundo evento de outbox ni un 500.
- **DoD:** prueba de idempotencia por endpoint y conteo de outbox.
- **Depende de:** F1.L5.M08.
- **Esfuerzo:** grande.

### F1.L5.M10 — Los consumidores Kafka no descartan: van a DLT con alarma
- **Hallazgos:** TYA-RESILIENCIA-001 (S2).
- **Causa de raíz:** `ConfiguracionMensajeria.java:55-65` reintenta unos 10 s y comitea el offset, sin destino para el fallo final. Así se pierde, por ejemplo, un `aporte_cobrado`.
- **Corrección de raíz:**
  - `DeadLetterPublishingRecoverer` hacia `<tema>.DLT`.
  - Backoff exponencial más largo para errores transitorios de base.
  - Métrica y alarma de mensajes en DLT.
  - Procedimiento de reproceso idempotente, que se apoya en F1.L5.M07.
- **CA:** dada la base caída durante 60 s, cuando llega un `aporte_cobrado`, entonces se aplica al volver la base o queda en la DLT con alarma. Nunca se pierde.
- **DoD:** integración con la base detenida a propósito (98.8.4) y prueba de mensaje envenenado que llega a la DLT.
- **Depende de:** F0.L5.M02.
- **Esfuerzo:** medio.

### F1.L5.M11 — Sello append-only para la evidencia del pago, las deducciones y la reputación
- **Hallazgos:** TYA-DATOS-008 (S2).
- **Causa de raíz:** la lista de `sql/35_append_only/append_only.sql` es manual, y en ella faltan `aportes.pago` (sus campos de evidencia), `aportes.webhook_pasarela`, `entregas.deduccion_entrega`, `transparencia.snapshot_reputacion` y `transparencia.puntaje_reputacion`.
- **Corrección de raíz:**
  - Declarar en el modelo cuáles tablas son append-only y generar el trigger y el `REVOKE UPDATE, DELETE` desde esa declaración.
  - Para `pago`, solo se sellan los campos de evidencia: el estado avanza por transición validada.
- **CA:** dado el rol de cada servicio, cuando intenta `UPDATE` o `DELETE` sobre esas tablas, entonces la base lo rechaza.
- **DoD:** prueba por tabla con el rol real (F0.L5.M03) y verificación de deriva.
- **Depende de:** F0.L5.M03 y F0.L5.M04.
- **Esfuerzo:** chico.

### F1.L5.M12 — Las deducciones de una entrega se suman bajo candado
- **Hallazgos:** TYA-DINERO-008 (S2). La parte append-only va en F1.L5.M11.
- **Causa de raíz:** `restricciones.sql:1846-1864` suma `total_deducciones` sin bloquear la fila de `entrega_fondo`, a diferencia del patrón que usa la billetera (`:325-342`). El control 91.2.3 se evalúa contra un total perdido.
- **Corrección de raíz:**
  - `SELECT … FOR UPDATE` de `entrega_fondo` antes del `SUM` en la función de la regla.
  - La reversa de una deducción se hace con una fila compensatoria.
- **CA:** dadas dos deducciones concurrentes que juntas superan la bolsa bruta, entonces una se rechaza y `total_deducciones` es exacto.
- **DoD:** prueba de concurrencia real contra PostgreSQL.
- **Depende de:** F1.L5.M11.
- **Esfuerzo:** chico.

### F1.L5.M13 — Devolución de comisión idempotente, con candado y cuatro ojos tomados del token
- **Hallazgos:** TYA-DINERO-002 (S1).
- **Causa de raíz:** `CU33DevolverComision` no usa `Idempotencia`, y el tope vive en un trigger sin serialización. El autorizante llega en el cuerpo (`ComisionesController.java:162-176`). Es un reverso sujeto a P-13.
- **Corrección de raíz:**
  - Clave de idempotencia con `UNIQUE` en `devolucion_comision`.
  - `devengos.bloquear` antes de leer lo cobrado.
  - Flujo en dos pasos: quien solicita (`ctx.usuarioId()`) y quien autoriza (`ctx.usuarioId()` de otra petición, distinto del primero, con permiso `REVERSO_AUTORIZAR`). Nada sale del cuerpo.
  - Agregar columna `moneda`.
  - El asiento del reverso es un contra-asiento.
- **CA:**
  - Dada la misma devolución enviada dos veces o en paralelo, entonces se registra una sola y nunca se supera lo cobrado.
  - Si quien autoriza es quien solicitó, la respuesta es 403.
- **DoD:**
  - prueba de concurrencia;
  - prueba de idempotencia;
  - autorización negativa (misma persona, sin permiso, sin token);
  - evidencia 91.6: asiento, idempotencia, cuadre, reversa y redondeo.
- **Depende de:** F1.L5.M08.
- **Esfuerzo:** medio.

### F1.L5.M14 — Comisión sin descuentos, segmentos ni exenciones, y con el obligado derivado en el servidor
- **Hallazgos:** TYA-SEG-001 (S1). Resuelve TYA-DEFECTO-007.
- **Causa de raíz:** `ComisionesController.java:50-66,112-128` y `CU31DevengarComision.java:104-115` toman del cuerpo el descuento, la exención y `usuarioObligadoId` (90.1.7). P-17 elimina los descuentos y las exenciones.
- **Corrección de raíz:**
  - Retirar del contrato `tarifas.yaml` los campos `descuento`, `exentoTotal` y segmento, con versión.
  - Retirar de `CalculoDeComision.java:83-88` y `SegmentoAplicable` la lógica de descuento.
  - Retirar del modelo las tablas o columnas de segmentos y descuentos.
  - El obligado se deriva de la operación (grupo y participante) que llega por evento.
  - `/devengos` deja de existir como ruta de usuario y pasa a ser consumo de evento (F3.L5.M37).
- **CA:** dado cualquier cliente, cuando intenta devengar una comisión con descuento, exención u otro obligado, entonces la ruta no existe para usuarios y el contrato no admite esos campos.
- **DoD:**
  - autorización negativa: un participante llama a `/devengos` y recibe 403 o 404;
  - prueba de contrato;
  - `grep -rn 'exentoTotal\|descuento' servicios/tarifas` vacío.
- **Depende de:** F0.L5.M01.
- **Esfuerzo:** medio.

### F1.L5.M15 — Retirar el marcado manual de comisión cobrada o fallida
- **Hallazgos:** TYA-APROB-001 (S1), parte del cierre del agujero. La confirmación por evento va en F3.L5.M37.
- **Causa de raíz:** una operación pensada como callback entre servicios quedó publicada en el gateway (`rutas.yml:146`) con el permiso de usuario final `BILLETERA_OPERAR`.
- **Corrección de raíz:**
  - Retirar `POST /comisiones/devengos/{id}/cobros` del controlador, del contrato (con versión) y del gateway.
  - Retirar el permiso de `10-roles-y-permisos.sql:154,156`.
  - `CU31DevengarComision.java:150-245` queda sin entrada HTTP. Su lógica de cobro o incobrable solo se invoca desde el consumidor de F3.L5.M37.
- **CA:** dado un participante con `BILLETERA_OPERAR`, cuando llama a la ruta, entonces recibe 404, y ninguna comisión cambia de estado.
- **DoD:** autorización negativa por el gateway real y `grep` de la ruta vacío.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F1.L5.M16 — El gateway no publica las operaciones internas
- **Hallazgos:** TYA-ARQ-001 (S3).
- **Causa de raíz:** `rutas.yml:65-154` enruta prefijos completos (`/api/v1/garantia/**`, `/api/v1/comisiones/**`). Así queda alcanzable desde internet lo que es entre servicios (98.6.4).
- **Corrección de raíz:**
  - Una lista de rutas internas excluidas del borde, con una prueba que la compare contra los `x-interno` de los contratos.
  - Las llamadas entre servicios usan una credencial de servicio con alcance acotado.
  - `POST /garantia/respaldo/coberturas` desaparece con F2 (lote L2).
- **CA:** dada cualquier ruta marcada como interna en un contrato, cuando se llama por el gateway, entonces la respuesta es 404.
- **DoD:** prueba del gateway que recorre los contratos.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

### F1.L5.M17 — Las funciones `SECURITY DEFINER` no se ejecutan desde PUBLIC
- **Hallazgos:** TYA-SEG-002 (S2).
- **Causa de raíz:** `SECURITY DEFINER` se aplicó en bloque y no se revocó `EXECUTE` de PUBLIC (`sql/00_base/03_permisos.sql:262-284`).
- **Corrección de raíz:**
  - `REVOKE ALL ON ALL FUNCTIONS IN SCHEMA aportes FROM PUBLIC`.
  - `SECURITY DEFINER` solo en las funciones de trigger.
  - `GRANT EXECUTE` explícito por rol a las que un servicio necesite. Por ejemplo, `fn_uif_registrar_operacion` solo para `svc_cumplimiento`.
- **CA:** dado el rol `svc_tarifas`, cuando ejecuta `fn_uif_titular`, entonces la base niega el permiso.
- **DoD:** script de prueba por rol contra la base real, con la salida pegada.
- **Depende de:** F0.L5.M03.
- **Esfuerzo:** chico.

### F1.L5.M18 — Limitador de tasa con clave que el cliente no controla
- **Hallazgos:** TYA-SEG-003 (S2).
- **Causa de raíz:** `LimitadorDeTasa.java:125-136` arma la clave con `Authorization`, así que un `Bearer` basura distinto abre un bucket nuevo.
- **Corrección de raíz:**
  - En las rutas sin sesión, la clave es solo la IP real: NGINX con `$remote_addr` y confianza explícita en el proxy.
  - Un bucket por identificador dentro de `identidad` para login y OTP.
- **CA:** dadas 100 peticiones de login con `Bearer` distintos desde la misma IP, entonces se aplica el límite.
- **DoD:** prueba del gateway con cabeceras variables.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F1.L5.M19 — `ClienteDeServicio` con errores tipados, sin cuerpo ajeno y sin log del cuerpo
- **Hallazgos:** TYA-SEG-004 (S2).
- **Causa de raíz:** hay un solo tipo de excepción para errores de cliente, de dependencia y de dominio. El 5xx del dependido sale como 422 con su cuerpo. El 403 se lee como «no hay dato». El cuerpo, que puede llevar importes, va al log.
- **Corrección de raíz:**
  - `DependenciaNoDisponible` devuelve 503 sin cuerpo.
  - `Optional.empty()` se usa solo para 404.
  - Resultado tipado para 403, 409 y 422.
  - Nunca se loguea el cuerpo, solo el código y el `correlationId`.
  - Se revisan los 19 usos.
- **CA:** dado un dependido que responde 500 con cuerpo, entonces el cliente final recibe 503 sin ese cuerpo y el log no lo contiene. Un 403 no se trata como vacío.
- **DoD:** prueba unitaria con doble HTTP en tres niveles (2xx, 403 y 500 o timeout) y captura del log.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

### F1.L5.M20 — El sorteo usa las entropías de los participantes reveladas tras el compromiso
- **Hallazgos:** TYA-SEG-005 (S2).
- **Causa de raíz:** `SorteoVerificable.java:66-102` ata las entropías en el compromiso pero no las usa al barajar. Quien opera el servidor conoce el orden de antemano y puede regenerar la semilla.
- **Corrección de raíz:**
  - La semilla efectiva pasa a ser `SHA-256(semilla || entropías reveladas tras el compromiso)`.
  - La versión del algoritmo se publica en el protocolo de CU-61.
  - El barajado se aplica **dentro de cada nivel de puntualidad** (F3.L3.M18).
- **CA:** dada la misma semilla de servidor con entropías distintas, entonces el orden cambia. Cualquiera puede recalcular el orden desde lo publicado.
- **DoD:** pruebas de propiedad del algoritmo y prueba de verificación pública de punta a punta.
- **Depende de:** ninguna. Coordina con F3.L3.M18.
- **Esfuerzo:** medio.

### F1.L5.M21 — El certificado de reputación se arma con datos del servidor
- **Hallazgos:** TYA-SEG-006 (S2).
- **Causa de raíz:** `CU75EmitirCertificado.java:76-95` firma con la clave de la plataforma un contenido que llega del cuerpo (`MapeoDeReputacion.java:96-103`), con la verificación de identidad como booleano del cliente.
- **Corrección de raíz:**
  - El contenido se carga del `snapshot_reputacion` del propio `ctx.usuarioId()`.
  - El estado de identidad se consulta a `identidad`, con timeout y fallo cerrado.
  - El cuerpo queda solo para elegir qué se incluye.
- **CA:** dado un pedido con nivel o puntaje inventado, entonces el certificado emitido muestra los valores reales.
- **DoD:** prueba web, más doble de `identidad` en tres niveles.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F1.L5.M22 — El sello de la cadena de transparencia tiene permiso propio y hechos calculados en el servidor
- **Hallazgos:** TYA-SEG-008 (S2).
- **Causa de raíz:** el sellado se diseñó como operación manual que recibe hechos entrantes, con un permiso de lectura (`ReputacionController.java:139-145`). Así se puede sellar cualquier hecho y 0 excepciones (91.1.5).
- **Corrección de raíz:**
  - Permiso de escritura propio.
  - Los hechos se calculan en el servidor desde el mayor y la conciliación, por API de `nucleo-financiero` y `aportes`.
  - El sellado lo dispara un trabajo programado, no una persona.
- **CA:** dado un usuario con solo permiso de lectura, cuando intenta sellar, entonces recibe 403. El bloque sellado coincide con el saldo del mayor.
- **DoD:** autorización negativa, más prueba de contrato con `nucleo-financiero` y prueba con ese servicio caído (no sella).
- **Depende de:** F4.L5.M52 (API de saldos del mayor).
- **Esfuerzo:** medio.

### F1.L5.M23 — Webhook de mensajería: todo se deriva de la carga firmada, con ventana de tiempo
- **Hallazgos:** TYA-SEG-009 (S2).
- **Causa de raíz:** la firma cubre solo `cargaUtil` y el remitente y el contenido viajan aparte (`CU82ProcesarRespuesta.java:69-117`). Además no hay protección contra repetición.
- **Corrección de raíz:**
  - Remitente, contenido y timestamp se leen de la carga firmada.
  - Se rechaza fuera de una ventana configurable.
  - Se guarda el identificador del mensaje con `UNIQUE` contra repeticiones.
- **CA:**
  - Dado un par (carga, firma) capturado y reenviado con otro remitente, entonces se rechaza.
  - El mismo mensaje dos veces se procesa una sola vez.
- **DoD:** doble del proveedor con firma real en tres niveles (válido, repetido o en el borde de la ventana, firma inválida).
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F1.L5.M24 — RLS por titular declarada en el modelo
- **Hallazgos:** TYA-SEG-010 (S2).
- **Causa de raíz:** la política RLS se aplica por heurística del nombre de columna (`restricciones.sql:1306-1500`). Por eso `aportes.pago`, `webhook_pasarela`, `qr_cobro`, `obligacion_aporte`, `entregas.entrega_fondo` y otras quedan sin RLS.
- **Corrección de raíz:** el modelo declara por tabla la columna de titular, o «sin titular, justificado», y el generador emite las políticas a partir de esa declaración.
- **CA:** dado el rol del servicio con el contexto del usuario A, cuando lee pagos o entregas, entonces no ve filas del usuario B.
- **DoD:** prueba por tabla con dos usuarios contra la base real, más verificación de deriva.
- **Depende de:** F0.L5.M03 y F0.L5.M04.
- **Esfuerzo:** medio.

### F1.L5.M25 — JWT con `iss` y `aud` validados, y JWKS con timeout
- **Hallazgos:** TYA-SEG-011 (S3).
- **Causa de raíz:** `ConfiguracionDelDecodificador.java:26-29` usa la configuración por omisión, sin validar `iss` ni `aud`. La evidencia de step-up no entra como sesión solo por accidente.
- **Corrección de raíz:**
  - `EmisorDeAcceso` emite `iss` y `aud` por tipo de token.
  - El decodificador los valida.
  - Timeouts explícitos para el JWKS.
- **CA:** dado un token de step-up con la misma clave, cuando se usa como sesión, entonces recibe 401 por `aud`.
- **DoD:** prueba de seguridad con los dos tipos de token, más JWKS caído (falla cerrado dentro del timeout).
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F1.L5.M26 — Avisos internos filtrados por destinatario
- **Hallazgos:** TYA-SEG-012 (S3).
- **Causa de raíz:** `AvisoInternoRepositorio.java:54-65` no comprueba el destinatario (BOLA entre operadores).
- **Corrección de raíz:** filtrar y marcar con `WHERE destinatario = ctx.usuarioId()`, más una política RLS por destinatario.
- **CA:** dado el operador B, cuando lee o marca un aviso de A, entonces recibe 404.
- **DoD:** autorización negativa.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F1.L5.M27 — Retirar la consulta de supresión que lleva el contacto en la URL
- **Hallazgos:** TYA-SEG-013 (S3).
- **Causa de raíz:** se mantuvo por compatibilidad el `GET` deprecado (`NotificacionesController.java:92-98`), que lleva el teléfono o el correo en el path (90.2.2).
- **Corrección de raíz:** retirar el `GET` con fecha declarada en el contrato. `grupos` ya usa el `POST`.
- **CA:** dado el contrato, entonces no hay ninguna operación con el contacto en el path o en la query.
- **DoD:** prueba de contrato y `grep` de consumidores vacío.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F1.L5.M28 — Publicación de tarifario: actor de la sesión y tipo de cambio derivado
- **Hallazgos:** TYA-SEG-014 (S3).
- **Causa de raíz:** `CU34PublicarTarifario.java:79,94` toma `aprobadoPor` y el tipo de cambio del cuerpo. Un aumento etiquetado como `REDUCCION` evita el preaviso.
- **Corrección de raíz:**
  - El actor sale de `ctx.usuarioId()`.
  - El tipo se deriva comparando la comisión vigente con la nueva (`EntradaEnVigencia.exigePreaviso`).
  - Si se conserva la aprobación por comité (D-L5-3), quien aprueba es otra persona, tomada de su propio token.
- **CA:** dado un aumento declarado como reducción, entonces el sistema lo clasifica como aumento y exige preaviso.
- **DoD:** prueba unitaria y prueba web.
- **Depende de:** D-L5-3 (solo para el segundo firmante).
- **Esfuerzo:** chico.

### F1.L5.M29 — Publicidad: propiedad de la cuenta, impresión única y tope aplicado
- **Hallazgos:** TYA-DINERO-006 (S2).
- **Causa de raíz:** la autorización es solo por rol, sin propiedad. Los contadores no están cableados (`CU113EntregarAnuncio.java:96-118`, `AnuncianteRepositorio.java:131-138`).
- **Corrección de raíz:**
  - Verificar la propiedad de la cuenta en cada operación.
  - `UNIQUE` por impresión y clic.
  - `sumarConsumo` con precondición en el `UPDATE` (`consumo + x <= tope`).
- **CA:**
  - Dado el anunciante B, cuando opera sobre la campaña de A, entonces recibe 403.
  - Un consumo que supera el tope se rechaza con 422, no con 500.
- **DoD:** autorización negativa, concurrencia sobre el tope y prueba de duplicado de impresión.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

### F1.L5.M30 — Semillas de desarrollo sintéticas y rotación de las credenciales expuestas
- **Hallazgos:** TYA-DATOS-001 (S2).
- **Causa de raíz:** por comodidad de desarrollo se versionaron datos personales de una persona real y hashes con sal fija. Están en `sql/61_dev/15-usuarios-dev.sql` y en `seeders/dev/15-usuarios-dev.json`, y hay hashes en `61_dev/05`, `61_dev/14` y `62_volumen`.
- **Corrección de raíz:**
  - Reemplazar por datos sintéticos declarados.
  - Rotar en TEST la contraseña de esa cuenta y de cualquier cuenta con un hash publicado (90.3.5).
  - Pablo decide si se purga el historial.
  - Agregar un chequeo en el gate que rechace semillas con documentos o correos no sintéticos (dominio reservado).
- **CA:** dado el repo, cuando se busca el patrón de datos reales de la ficha, entonces no hay coincidencias. El hash viejo ya no autentica en TEST.
- **DoD:** `grep` revisado, intento de login con la credencial vieja (falla) y chequeo del gate.
- **Depende de:** P-19 (acceso a TEST).
- **Esfuerzo:** chico.

### F1.L5.M31 — Borrador de alta sin PII como clave, con el documento protegido y RLS
- **Hallazgos:** TYA-DATOS-002 (S2).
- **Causa de raíz:** `sql/15_infra/borradores.sql` se escribió a mano fuera del modelo. Usa el correo como clave primaria y guarda el documento en claro, en contra de R-SEG-01. Lo mismo pasa con `pago.pagador_documento`.
- **Corrección de raíz:**
  - Pasar la tabla al modelo con una llave sustituta.
  - El documento se guarda como hash con pimienta más cifrado, y el correo, cifrado.
  - RLS por titular.
  - En `pago`, el documento del pagador se guarda cifrado o no se guarda.
- **CA:** dado el borrador, cuando se consulta la tabla como dueño, entonces no aparecen ni el documento ni el correo en claro.
- **DoD:** verificación de deriva, prueba de cifrado e ida y vuelta, y RLS con dos usuarios.
- **Depende de:** F0.L5.M04 y F1.L5.M24.
- **Esfuerzo:** chico.

---

## F2 — Retiro de lo que contradice el flujo nuevo

### F2.L5.M32 — Retirar los canales de pago que no vienen de un QR confirmado, con el control en la base
- **Hallazgos:** TYA-FLUJO-QR-001 (S1) y TYA-SEG-016 (S4).
- **Causa de raíz:** el modelo de pagos es anterior a la decisión «solo QR». Admite `EFECTIVO_AL_ORGANIZADOR`, `TARJETA`, `es_manual` e instrumentos de fondeo con tarjeta y efectivo (`instrumento_fondeo.sql:23`, `cobro_cuenta_por_cobrar.sql:16`, `14-proveedores-externos.sql`). Además, la base no exige el webhook firmado.
- **Corrección de raíz:**
  - Dejar en el modelo solo tres canales:
    - `QR_INTEROPERABLE`, que exige `webhook_id` con FK y `CHECK (estado <> 'ACREDITADO' OR webhook_id IS NOT NULL)`;
    - `BILLETERA` (P-06: su saldo entró por un QR confirmado);
    - `FONDO_GRUPO` (cobertura del modelo A, con asiento propio).
  - En `webhook_pasarela`, `CHECK (pago_id IS NULL OR firma_valida)`.
  - Retirar `es_manual`, `TARJETA`, `EFECTIVO_*` y `TARJETA_ADQUIRENCIA`.
  - Las semillas de desarrollo y volumen que usan esos canales pasan a QR.
- **CA:** dado cualquier `INSERT` o `UPDATE` que deje un pago `ACREDITADO` sin webhook firmado y fuera de esos tres canales, entonces la base lo rechaza.
- **DoD:**
  - prueba que intenta violar cada `CHECK` con el rol `svc_aportes`;
  - verificación de deriva;
  - semillas aplicadas dos veces (idempotencia).
- **Depende de:** F0.L5.M04. Coordina con el lote L1 (retiro del cobro declarado por el cliente).
- **Esfuerzo:** medio.

### F2.L5.M33 — Retirar el comprobante manual del esquema y del chatbot
- **Hallazgos:** TYA-APROB-002 (S3).
- **Causa de raíz:** el modelo es anterior a «solo QR». Quedan la tabla `comprobante_manual`, la acción `COMPROBANTE_SOLICITADO` (`IntencionEntrante.java:20,88`) y el valor `PAGO_CONFIRMADO` en `notificaciones.yaml:410`, que P-07 elimina.
- **Corrección de raíz:**
  - Retirar la tabla del modelo.
  - Retirar la acción del dominio.
  - Retirar el valor del enum, con versión del contrato.
  - Ante «ya pagué», el chatbot responde con el estado real del pago consultado a `aportes`.
- **CA:** dado el mensaje «ya pagué», cuando lo procesa `notificaciones`, entonces no pide un comprobante ni confirma un pago.
- **DoD:** prueba de dominio, prueba de contrato y verificación de deriva.
- **Depende de:** F0.L5.M04.
- **Esfuerzo:** chico.

### F2.L5.M34 — Retirar las cuentas 9.x de la empresa y separar las semillas de simulación
- **Hallazgos:** TYA-RESPALDO-002 (S2).
- **Causa de raíz:** una sola semilla sirve a producción y a simulación. Por eso `22-cuentas-de-sistema.sql` siembra en todos los entornos «Caja de la empresa» y «Reserva de respaldo», que son la contrapartida de un camino que P-04 elimina.
- **Corrección de raíz:**
  - Retirar del catálogo las cuentas 9.1.01, 9.1.02, 9.2.01 y 9.2.02 y su mapeo lógico en `nucleo-financiero`.
  - Agregar las cuentas segregadas del fondo del grupo según el ADR del modelo A, con procedencia.
  - Dividir `sembrar.sql` en semillas de producción (solo catálogos con procedencia) y de simulación (`61_dev`, `62_volumen`).
  - Si hay asientos en las 9.x en algún entorno, se revierten con contra-asiento antes de retirar la cuenta.
- **CA:** dada una base de producción sembrada, entonces no existen las cuentas 9.x ni ningún dato sintético.
- **DoD:**
  - `sembrar.sql` de producción aplicado dos veces sin inserciones en la segunda;
  - consulta de cuentas;
  - verificación de deriva.
- **Depende de:** F0.L5.M01 y el retiro del respaldo en `garantia` y `nucleo-financiero` (lotes L1 y L2).
- **Esfuerzo:** medio.

### F2.L5.M35 — Retirar las reseñas entre personas como insumo de la reputación
- **Hallazgos:** TYA-SEG-007 (S2). Resuelve TYA-DEFECTO-009.
- **Causa de raíz:** CU-76 deja que un participante califique a otro. El autor, el ciclo y los antecedentes vienen del cuerpo. La tanda 5 fija que la calificación se calcula solo con hechos, sin calificaciones entre personas.
- **Corrección de raíz:**
  - Retirar `CU76PublicarResena`, su moderación (AP-09), sus rutas y contrato (con versión), sus tablas y la regla `restricciones.sql:2198-2212`.
  - El peso de las reseñas deja de existir en el cálculo de la reputación.
- **CA:** dado el contrato de `transparencia`, entonces no hay ninguna operación de reseña, y el puntaje no depende de ninguna entrada escrita por otra persona.
- **DoD:** `grep -rn 'Resena' servicios/transparencia` vacío, más tests y deriva en verde.
- **Depende de:** F3.L5.M39.
- **Esfuerzo:** chico.

---

## F3 — Flujo nuevo

### F3.L5.M36 — Comisión fija igual para todos, como parámetro obligatorio con unidad explícita
- **Hallazgos:** TYA-DEFECTO-002 (S2).
- **Causa de raíz:** la semilla y el código interpretan distinto la unidad del porcentaje (30 % frente a 0,30 %). Los tramos dejan huecos y el valor no tiene procedencia (97.5.4). La prueba cubre el código, no la semilla.
- **Corrección de raíz:**
  - Reemplazar el tarifario por tramos (`04-tarifario.sql`) por **un solo** parámetro de comisión. Su forma (`IMPORTE` o `FRACCION`, ver D-L5-1) y su valor se declaran en la configuración, con moneda y procedencia.
  - El servicio no arranca si el parámetro falta: no hay valor por defecto.
  - `CalculoDeComision` aplica una sola fórmula con `BigDecimal` y redondeo declarado (`HALF_EVEN` a 2 decimales).
- **CA:**
  - Dados dos participantes cualesquiera con la misma operación, entonces pagan la misma comisión.
  - Sin el parámetro, el servicio no arranca.
- **DoD:**
  - prueba que lee la configuración y la semilla reales;
  - prueba de arranque sin el parámetro (falla);
  - redondeo declarado en el reporte (91.6.5).
- **Depende de:** F0.L5.M01, F1.L5.M14 y D-L5-1.
- **Esfuerzo:** medio.

### F3.L5.M37 — El cobro de la comisión se confirma solo por el evento de pago y llega al mayor
- **Hallazgos:** TYA-APROB-001 (S1, parte de reemplazo) y TYA-HUECO-002 (S2, parte de `tarifas`).
- **Causa de raíz:** el lado productor se escribió sin consumidor, y el cobro se marcaba a mano. Las comisiones cobradas nunca llegan a la partida doble (91.1.5).
- **Corrección de raíz:**
  - Cada comisión genera su orden de cobro QR en `aportes` y `nucleo-financiero`, con `clave_idempotencia` (D-L5-2).
  - El webhook firmado confirma el pago y `nucleo-financiero` asienta el ingreso por comisión.
  - Se publica `pago_comision_confirmado`, y `tarifas` lo consume con deduplicación por `message_id` y transición con precondición (`PENDIENTE → COBRADA`).
  - La rama incobrable la decide el vencimiento según la regla de incumplimiento del sistema, no una persona.
  - El consumidor de `tarifas.comision_cobrada` en `nucleo-financiero` usa `UNIQUE (origen_tipo, origen_id)`.
- **CA:** dada una comisión pagada por QR, cuando llega el webhook (incluso duplicado), entonces la comisión queda `COBRADA` una vez y hay un asiento que cuadra. Sin webhook, nunca queda cobrada.
- **DoD:**
  - doble de pasarela con firma real en tres niveles (válido, duplicado o en el borde de la ventana, firma inválida o monto distinto);
  - evidencia 91.6 completa;
  - pactos `nucleo-financiero`↔`tarifas`;
  - prueba con `tarifas` caído (el evento espera y no se pierde).
- **Depende de:** F1.L5.M15, F3.L5.M36, F1.L5.M07, F1.L5.M10 y la cadena QR → webhook del lote L1.
- **Esfuerzo:** grande.

### F3.L5.M38 — Parámetros financieros con procedencia, obligatorios y sin valor por defecto
- **Hallazgos:** TYA-DATOS-003 (S2).
- **Causa de raíz:** las semillas se escribieron para simular. Hay mora del 2 %, cobertura del 5 %, topes y comisiones de proveedores sin fuente (`02-politicas.sql:18-19,27-28`, `14-proveedores-externos.sql`, `09-reglas-operativas.sql`, `12-calendario-habil.sql`), y un `hash_documento = repeat('0',64)` falso.
- **Corrección de raíz:**
  - Agregar las columnas de procedencia que pide la regla 97.4.2 (fuente, referencia, fecha, responsable) y el estado `PROVISIONAL` o `VIGENTE` en las tablas de parámetros.
  - Los parámetros del modelo A y D se cargan por configuración obligatoria (porcentaje del fondo, días de gracia, tasa de recargo y su destino al perjudicado, y comisión). Los servicios no arrancan si un parámetro `VIGENTE` falta.
  - Retirar de las semillas de producción los valores sin fuente.
  - El documento del tarifario lleva un hash real o no lleva ninguno.
- **CA:**
  - Dado un entorno sin el porcentaje del fondo configurado, cuando arranca `garantia`, entonces falla con un mensaje claro.
  - Ningún parámetro financiero sembrado en producción carece de procedencia.
- **DoD:** prueba de arranque por servicio, consulta de procedencia nula = 0 y semilla idempotente.
- **Depende de:** F2.L5.M34 y la confirmación de valores por Pablo y Cumplimiento.
- **Esfuerzo:** medio.

### F3.L5.M39 — Reputación calculada solo con hechos y expedientes firmes, con historial append-only
- **Hallazgos:** TYA-DEBIDO-PROCESO-001 (S2) y TYA-DATOS-005 (S3, parte de `transparencia`).
- **Causa de raíz:** no hay integración por evento con el expediente. Soporte escribe eventos penalizantes y `monto_total_aportado` (sin moneda) desde el cuerpo (`ReputacionController.java:85-167`, `ReputacionRepositorio.java:189-201,234`), y el puntaje anterior se borra.
- **Corrección de raíz:**
  - `transparencia` consume `aporte_cobrado`, que viene del webhook del QR, con su fecha frente al vencimiento. Ese es el único insumo positivo.
  - Los eventos negativos solo llegan con la referencia a un expediente `FIRME` de `garantia` y se verifican con su dueño.
  - El puntaje y el nivel (pieza D: niveles, rachas, insignias, «Grupo perfecto») se derivan de `evento_reputacion`.
  - El historial de puntajes es append-only.
  - El aportado se lee del mayor, con moneda.
  - Se retiran las rutas de escritura manual de soporte.
- **CA:**
  - Dado un participante, su puntaje se reproduce íntegro recalculando desde los eventos.
  - Un evento penalizante sin expediente `FIRME` se rechaza.
  - Ningún puntaje anterior se puede borrar.
- **DoD:**
  - prueba de duplicado del consumidor;
  - doble de `garantia` en tres niveles (firme, en descargo, servicio caído);
  - prueba de recálculo determinista;
  - append-only con el rol real.
- **Depende de:** F1.L5.M11 y la microtarea de incumplimiento declarado por el sistema (lote L2).
- **Esfuerzo:** grande.

### F3.L5.M40 — El perfil en el grupo muestra solo hechos firmes y su lectura queda registrada
- **Hallazgos:** TYA-DEBIDO-PROCESO-002 (S3).
- **Causa de raíz:** la vista se diseñó mostrando presunciones (`RutasDePerfilEnGrupo.java:58`) y sin bitácora de lectura.
- **Corrección de raíz:**
  - El perfil muestra el nivel, la racha, las insignias y solo los incumplimientos `FIRME`.
  - Cada lectura de un perfil ajeno se registra en `comun.registro_acceso_datos`.
- **CA:** dado un incumplimiento en descargo, cuando un par mira el perfil, entonces no lo ve. La lectura queda registrada con quién y qué.
- **DoD:** prueba web y consulta de la bitácora.
- **Depende de:** F3.L5.M39.
- **Esfuerzo:** chico.

### F3.L5.M41 — `grupos` obtiene el nivel de puntualidad por contrato y propaga el rechazo
- **Hallazgos:** TYA-CONTRATO-004 (S3).
- **Causa de raíz:** el contrato cambió sin versión. `HechosPorHttp.java:192-197` pide el puntaje ajeno y lee el 403 como «sin historial» (puntaje cero).
- **Corrección de raíz:**
  - `grupos` usa `/etapas/de-personas`, que devuelve el nivel y no el puntaje, con versión y pacto.
  - El 403 y el 503 se propagan como error (F1.L5.M19).
  - Este es el insumo del orden de turnos por nivel (F3.L3.M18).
- **CA:** dado `transparencia` negando o caído, cuando `grupos` compromete el sorteo, entonces no ordena con un nivel falso: reintenta.
- **DoD:** pacto `grupos`↔`transparencia` y doble en tres niveles.
- **Depende de:** F3.L5.M39 y F1.L5.M19.
- **Esfuerzo:** chico.

### F3.L5.M42 — La verificación pública del sorteo tiene un contrato público y un estado explícito
- **Hallazgos:** TYA-CONTRATO-003 (S3).
- **Causa de raíz:** `PublicoController.java:79-117` depende de una ruta autenticada de `grupos` y, si falla, responde 200 con «no verifica».
- **Corrección de raíz:**
  - `grupos` publica por evento el paquete del sorteo (compromiso, entropías, semilla, niveles y orden) y `transparencia` lo guarda.
  - La verificación pública lee ese paquete.
  - Si no está disponible, responde un estado explícito (503 o el campo `disponible=false`).
- **CA:** dado un sorteo revelado, cuando cualquiera lo verifica, entonces obtiene «verifica» o un motivo explícito, nunca un falso «no verifica».
- **DoD:** prueba pública de punta a punta y pacto del evento.
- **Depende de:** F1.L5.M20 y F3.L3.M18.
- **Esfuerzo:** medio.

### F3.L5.M43 — Las notificaciones salen: consumidores de eventos, trabajo programado e idioma `es-BO`
- **Hallazgos:** TYA-HUECO-001 (S2).
- **Causa de raíz:** CU80, CU81 y CU83 se escribieron y probaron aislados, sin punto de entrada y sin carpeta `trabajos/`. La fixture inserta `'es'`.
- **Corrección de raíz:**
  - Consumidores idempotentes de los eventos de negocio: cuota por vencer, mora, cuota cubierta por el fondo del grupo, entrega acreditada, incumplimiento declarado y comisión pendiente.
  - Trabajo programado con ShedLock para recordatorios y lote.
  - Plantillas en `es-BO`.
  - Los textos nuevos los aprueba Pablo (P-16).
- **CA:** dada una cuota que vence mañana, cuando corre el trabajo, entonces sale un recordatorio con el enlace del QR, una sola vez.
- **DoD:**
  - prueba de duplicado;
  - doble del proveedor de mensajería en tres niveles;
  - prueba de lock con dos instancias.
- **Depende de:** F1.L5.M10 y F3.L5.M44.
- **Esfuerzo:** grande.

### F3.L5.M44 — Mensajes, insignias y reportes de cobertura reescritos para el fondo del grupo
- **Hallazgos:** TYA-RESPALDO-001 (S3).
- **Causa de raíz:** el catálogo de mensajes se construyó sobre la cobertura heredada (`15-eventos-y-plantillas.sql:33,74,136-138,200,332-334`, `CriterioDeInsignia.java:35-38`, `19-reportes-y-retencion.sql:7,9,11`, `transparencia.yaml:775,781,891`).
- **Corrección de raíz:**
  - Con el modelo A decidido, reescribir en el mismo cambio:
    - el evento, con el fondo **del grupo** como origen;
    - la plantilla para el deudor: «el fondo de tu grupo cubrió tu cuota; quedás debiendo al fondo»;
    - la plantilla para el grupo;
    - la insignia, como logro de puntualidad de la pieza D;
    - los reportes.
  - Los textos los redacta Claude y los aprueba Pablo.
  - Retirar toda mención de cobertura de la empresa.
- **CA:** dado un `cuota_cubierta_por_fondo`, entonces los mensajes nombran al fondo del grupo y la deuda del participante. Ninguno menciona a la empresa.
- **DoD:** prueba de plantillas por evento y aprobación de Pablo en el PR.
- **Depende de:** P-16 y el ADR del modelo A.
- **Esfuerzo:** chico.

---

## F4 — Calidad y deuda

Cada microtarea de F4 es un cambio y un PR.

### F4.L5.M45 — Moneda y precisión de dinero uniformes en el modelo
- **Hallazgos:** TYA-DINERO-009 (S2) y TYA-DINERO-012 (S4).
- **Causa de raíz:** los tipos se eligieron tabla por tabla en un modelo heredado anterior a la regla 91.1. Hay 80 tablas sin `moneda` y precisiones (10,2), (12,2), (18,2) y (10,4) sin regla de redondeo.
- **Corrección de raíz:**
  - Un tipo de dominio «Dinero» en el `.puml` que genera `DECIMAL(14,2)` (o `16,2` para acumulados) junto a `moneda CHAR(3)`.
  - Los costos sub-centavo usan un tipo propio con redondeo declarado.
  - Se regenera el esquema y las tablas que caen en F2 no se migran.
- **CA:** dada cualquier tabla con importe, entonces tiene `moneda` y una precisión del catálogo.
- **DoD:** script de inventario con 0 excepciones y verificación de deriva.
- **Depende de:** F0.L5.M04 y las microtareas de F2.
- **Esfuerzo:** grande.

### F4.L5.M46 — ERP separa por moneda y valida la moneda al pagar
- **Hallazgos:** TYA-DINERO-005 (S2).
- **Causa de raíz:** obsesión por primitivos en `PeriodoRepositorio.totalesDe`, `PresupuestoRepositorio.saldosDelPeriodo` y `CU103FacturaDeProveedor.java:185-200`.
- **Corrección de raíz:** agrupar por moneda y usar `Dinero` con validación de moneda en `pagar`.
- **CA:** dado un pago en USD contra una factura en BOB, entonces se rechaza.
- **DoD:** prueba con dos monedas contra la base real.
- **Depende de:** F4.L5.M45.
- **Esfuerzo:** chico.

### F4.L5.M47 — Impuestos de la comisión según el catálogo, sin NIT por defecto y con guardia de producción
- **Hallazgos:** TYA-DINERO-007 (S2).
- **Causa de raíz:** el modelo del impuesto se simplificó y no lee `tipo_calculo`. `SIN_NIT_EMISOR` tiene un valor por defecto. El simulador del SIN no tiene guardia.
- **Corrección de raíz:**
  - `CalculoDeComision` aplica `INCLUIDO_EN_PRECIO` o `SOBRE_PRECIO` según el catálogo.
  - Sin `SIN_NIT_EMISOR`, el servicio no arranca (90.3.4).
  - `GuardiaDeProduccion` impide el simulador en producción.
- **CA:** dado el IVA incluido, entonces el desglose coincide con la semilla. En el perfil de producción, con el simulador activo, el servicio no arranca.
- **DoD:** prueba unitaria por tipo y prueba de arranque.
- **Depende de:** F3.L5.M36.
- **Esfuerzo:** chico.

### F4.L5.M48 — La comisión publicada es inmutable y se lee del snapshot
- **Hallazgos:** TYA-DINERO-011 (S3).
- **Causa de raíz:** el `REVOKE` apunta a un rol que ya no es el de la aplicación (`restricciones.sql:3483-3487` frente a `03_permisos.sql:72`), y el precio «congelado» lee tramos vivos.
- **Corrección de raíz:** se revoca el permiso de modificación a `svc_tarifas` sobre la comisión publicada, y el grupo guarda el snapshot de la comisión vigente al iniciar.
- **CA:** dado un parámetro publicado, cuando `svc_tarifas` intenta un `UPDATE`, entonces la base lo rechaza.
- **DoD:** prueba con el rol real.
- **Depende de:** F3.L5.M36.
- **Esfuerzo:** chico.

### F4.L5.M49 — La facturación de comisiones deriva el estado del cargo cobrado
- **Hallazgos:** TYA-DEFECTO-003 (S2).
- **Causa de raíz:** `CU32EmitirFactura.java:110` no usa `EstadoDelDevengo.derivar`.
- **Corrección de raíz:** derivar el estado de las filas de `cargo_comision`, que quedan cobradas por F3.L5.M37.
- **CA:** dada una comisión cobrada por evento, entonces CU-32 emite su factura.
- **DoD:** integración sin la fixture `COBRADO` (F0.L5.M03).
- **Depende de:** F3.L5.M37.
- **Esfuerzo:** chico.

### F4.L5.M50 — Una sola factura por devengo
- **Hallazgos:** TYA-DEFECTO-008 (S3).
- **Causa de raíz:** TOCTOU: el chequeo ocurre antes del candado y el índice no es único (`sql/30_indices/11_*.sql:163`).
- **Corrección de raíz:** `UNIQUE (devengo_id)` y el chequeo bajo candado.
- **CA:** dadas dos emisiones concurrentes, entonces hay una factura.
- **DoD:** prueba de concurrencia.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L5.M51 — El balance general incluye el resultado del ejercicio
- **Hallazgos:** TYA-DEFECTO-004 (S2).
- **Causa de raíz:** `CU106GenerarEstadoFinanciero.java:70-83` exige ACTIVO = PASIVO + PATRIMONIO sin el resultado y con movimientos del periodo.
- **Corrección de raíz:** usar saldos acumulados más el resultado del ejercicio en `CuadreContable`.
- **CA:** dado un periodo con ingresos y gastos, entonces el balance se genera y cuadra.
- **DoD:** prueba con datos sintéticos declarados.
- **Depende de:** F4.L5.M52.
- **Esfuerzo:** chico.

### F4.L5.M52 — ERP, publicidad y los reportes dejan de leer esquemas ajenos
- **Hallazgos:** TYA-ACOPLE-001 (S2).
- **Causa de raíz:** leen por SQL datos ajenos (`PeriodoRepositorio.java:149-174`, `PresupuestoRepositorio`, `CU110AltaDeAnunciante.java:158,177`, `CreativaRepositorio.java:89-97` y `19-reportes-y-retencion.sql:6-14`) en lugar de pedirlos por API o recibirlos por evento (98.1.1).
- **Corrección de raíz:**
  - API de lectura de saldos del mayor en `nucleo-financiero`, con versión y pacto.
  - Proyecciones propias de `auditoria` para los reportes, alimentadas por eventos.
  - `publicidad` consulta a `grupos` o `organizador` por contrato. La parte de organizador cae con F2.L3.M12.
- **CA:** dado el rol `svc_erp`, cuando se genera el cierre, entonces funciona sin `GRANT` sobre otros esquemas.
- **DoD:** integración con el rol real, pactos y prueba con `nucleo-financiero` caído (503 explícito).
- **Depende de:** F0.L5.M03.
- **Esfuerzo:** grande.

### F4.L5.M53 — Los eventos de dinero de ERP y publicidad llegan al mayor
- **Hallazgos:** TYA-HUECO-002 (S2, parte de ERP y publicidad). La parte de `tarifas` va en F3.L5.M37.
- **Causa de raíz:** el productor se escribió sin consumidor.
- **Corrección de raíz:**
  - Consumidores idempotentes en `nucleo-financiero`, con asiento por evento y `UNIQUE (origen_tipo, origen_id)`.
  - `factura_proveedor.asiento_contable_id` se reemplaza por la relación desde el asiento, porque esa tabla tiene `UPDATE` revocado.
- **CA:** dado un pago a proveedor, entonces hay un asiento que cuadra, una sola vez.
- **DoD:** prueba de duplicado y evidencia 91.6.
- **Depende de:** F1.L5.M07 y F1.L5.M10.
- **Esfuerzo:** medio.

### F4.L5.M54 — Corte de las claves foráneas entre esquemas, por pares
- **Hallazgos:** TYA-ACOPLE-002 (S3).
- **Causa de raíz:** el monolito se partió en esquemas sin cortar las claves (`02_esquemas.sql:4-5`). Hay 359 FK cruzadas.
- **Corrección de raíz:**
  - Un plan de corte por pares en el modelo, de expansión a contracción (por ejemplo, `cumplimiento→identidad`, `garantia→identidad` y `garantia→grupos`).
  - La validación pasa a hacerse por contrato del dueño.
  - Coordina con F4.L3.M23.
- **CA:** al cerrar cada par, `information_schema` no muestra FK de ese par.
- **DoD:** consulta por par y verificación de deriva.
- **Depende de:** F0.L5.M04.
- **Esfuerzo:** grande.

### F4.L5.M55 — Derechos del titular ejecutados de verdad
- **Hallazgos:** TYA-HUECO-003 (S2).
- **Causa de raíz:** `CU07EjercerDerechos.java:113,121-144` registra el plan como si fuera la ejecución y responde «se eliminaron», colgado de un permiso de lectura.
- **Corrección de raíz:**
  - Estado `EN_PROCESO` hasta que cada servicio dueño confirme por evento.
  - Un ejecutor de supresión o anonimización por servicio, que respeta las retenciones legales de dinero y UIF.
  - Un permiso de escritura propio.
- **CA:** dada una solicitud de supresión, entonces la respuesta es `EN_PROCESO` hasta que llegan todas las confirmaciones, y los datos ya no son legibles.
- **DoD:** integración con dos servicios y uno caído, que queda pendiente y con alerta.
- **Depende de:** revisión de Cumplimiento (F4.L5.M81).
- **Esfuerzo:** grande.

### F4.L5.M56 — Los reportes regulatorios se entregan y su ejecutor es de solo lectura en la base
- **Hallazgos:** TYA-HUECO-004 (S3).
- **Causa de raíz:** la implementación está incompleta: no escribe el archivo y valida el SQL por texto (`EjecutorDeConsulta.java:47`).
- **Corrección de raíz:** escribir en almacenamiento y ejecutar con un rol de base de solo lectura.
- **CA:** dado un CTE con `DELETE`, entonces la base lo rechaza. La descarga devuelve el archivo.
- **DoD:** prueba con el rol y prueba de descarga.
- **Depende de:** F4.L5.M52.
- **Esfuerzo:** medio.

### F4.L5.M57 — `auditoria` crea incidentes por cadena rota y por sorteo no verificado
- **Hallazgos:** TYA-HUECO-005 (S3).
- **Causa de raíz:** hay productor sin consumidor (`CU61*:99-101`, `CU73*:111-128`).
- **Corrección de raíz:** un consumidor idempotente en `auditoria` que crea el incidente y avisa.
- **CA:** dado `cadena_rota`, entonces existe un incidente abierto.
- **DoD:** prueba de duplicado.
- **Depende de:** F1.L5.M10.
- **Esfuerzo:** chico.

### F4.L5.M58 — La verificación pública de certificados reconoce un certificado vigente
- **Hallazgos:** TYA-DEFECTO-001 (S2).
- **Causa de raíz:** el contrato interno es ambiguo: mapa vacío frente a nulo (`VerificarController.java:41`, `CU75EmitirCertificado.java:194-199`).
- **Corrección de raíz:** separar la «vigencia» del «cotejo de contenido».
- **CA:** dado un certificado vigente, entonces `valido=true`.
- **DoD:** prueba web sin mockear CU75.
- **Depende de:** F1.L5.M21.
- **Esfuerzo:** chico.

### F4.L5.M59 — La ventana de envío y los recordatorios se calculan en `America/La_Paz`
- **Hallazgos:** TYA-DEFECTO-005 (S3).
- **Causa de raíz:** se usa `OffsetDateTime` en UTC en `VentanaDeEnvio`, CU80 y CU81, y la prueba codifica el error.
- **Corrección de raíz:** usar `Reloj.hoyEnLaPaz()` y el `ZoneId` del calendario (compartido con F4.L3.M27 a).
- **CA:** dadas las 20:30 locales, entonces el recordatorio cae en el día local correcto.
- **DoD:** prueba con reloj fijo y la prueba de `CU80Test.java:135` corregida.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L5.M60 — Una métrica sin umbral no dispara alerta
- **Hallazgos:** TYA-DEFECTO-006 (S3).
- **Causa de raíz:** se mapea null a cero (`MapeoDeTransparencia.java:51`).
- **Corrección de raíz:** conservar null y que `SenalDeRiesgo` no evalúe sin umbral.
- **CA:** dada una métrica sin umbral, entonces `en_alerta=false`.
- **DoD:** prueba unitaria.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L5.M61 — El relevo del outbox tiene tope de rescates, productor idempotente y orden por agregado
- **Hallazgos:** TYA-RESILIENCIA-002 (S3), TYA-RESILIENCIA-003 (S3) y TYA-CONTRATO-002 (S3).
- **Causa de raíz:** son tres defectos del mismo componente (`Relevo.java`), cada uno con su causa:
  - (a) rescata los `FALLIDOS` para siempre;
  - (b) usa la configuración por omisión del productor;
  - (c) no respeta el orden por agregado con un evento en backoff.
- **Corrección de raíz:** subtareas separadas, una por PR.
  - (a) Tope de rescates y umbral por último fallo.
  - (b) `max.block.ms` menor que `lockAtMostFor` y `enable.idempotence=true`.
  - (c) No tomar un evento si hay otro anterior pendiente del mismo agregado.
- **CA:** (a) un evento venenoso queda `FALLIDO` tras N rescates y la métrica lo muestra. (b) Con Kafka caído, el relevo no supera el lock. (c) El evento 2 nunca sale antes que el 1.
- **DoD:** una prueba de integración por subtarea, con Kafka detenido a propósito en (b).
- **Depende de:** F0.L5.M02.
- **Esfuerzo:** medio.

### F4.L5.M62 — El despacho por lote llama al proveedor después del commit
- **Hallazgos:** TYA-RESILIENCIA-004 (S3).
- **Causa de raíz:** una transacción larga con `FOR UPDATE SKIP LOCKED` llama al proveedor adentro (`CU83DespacharLote.java:83-149`).
- **Corrección de raíz:**
  - Reservar en la transacción, enviar fuera y registrar el resultado (96.3.4).
  - Circuit breaker hacia el proveedor de mensajería.
- **CA:** dado un error tardío, entonces los mensajes ya enviados no se reenvían.
- **DoD:** prueba con el proveedor que falla a mitad del lote.
- **Depende de:** F3.L5.M43.
- **Esfuerzo:** medio.

### F4.L5.M63 — Timeouts explícitos en el cliente de MinIO
- **Hallazgos:** TYA-RESILIENCIA-005 (S4).
- **Causa de raíz:** se omitieron (`AlmacenS3Adaptador.java:38-41`).
- **Corrección de raíz:** timeouts de conexión y de lectura configurables y obligatorios.
- **CA:** con MinIO colgado, la operación falla dentro del timeout.
- **DoD:** prueba con el contenedor pausado.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L5.M64 — Semántica HTTP de los errores y formato registrado en un ADR
- **Hallazgos:** TYA-CONTRATO-001 (S3).
- **Causa de raíz:** hay un solo tipo `ErrorDeDominio` que se convierte en 422 (`ManejadorGlobalDeErrores.java:253-262`), y `ResponseStatusException` termina en 500.
- **Corrección de raíz:**
  - Subclases de infraestructura que responden 500 o 503.
  - Un handler de `ErrorResponse`.
  - Un ADR que elija `ErrorApi` o `problem+json`.
- **CA:** con MinIO caído, la respuesta es 503, y un `ResponseStatusException(422)` responde 422.
- **DoD:** pruebas web.
- **Depende de:** F1.L5.M19.
- **Esfuerzo:** medio.

### F4.L5.M65 — Trazabilidad de punta a punta
- **Hallazgos:** TYA-TRAZA-001 (S3).
- **Causa de raíz:** las piezas se probaron aisladas. El interceptor Kafka no tiene prueba de cableado, `causation_id` nunca se escribe, la versión es fija y `LibroPorHttp` no envía la correlación.
- **Corrección de raíz:**
  - Prueba de contexto con un listener real.
  - Escribir `causation_id`.
  - La versión sale del build.
  - La cabecera de correlación se propaga en todos los clientes HTTP.
- **CA:** dado un aporte, su `correlationId` aparece en los logs de `aportes`, `nucleo-financiero` y `garantia`.
- **DoD:** traza capturada en la integración (98.8.5).
- **Depende de:** F0.L5.M02.
- **Esfuerzo:** medio.

### F4.L5.M66 — Logs estructurados con máscara de PII y los campos de la regla 98.5.2
- **Hallazgos:** TYA-OBSERV-001 (S3) y TYA-OBSERV-002 (S4).
- **Causa de raíz:** el formato es artesanal: JSON a mano en `logback-spring.xml:19`, máscara solo sobre el mensaje y faltan campos. OBSERV-002 reúne detalles del mismo origen: reloj no inyectado y `/sesion/refrescar` sin límite de tasa.
- **Corrección de raíz:** subtareas separadas, una por PR.
  - (a) Logging estructurado de Spring Boot, con máscara sobre la excepción y el MDC y con `correlationId`, `service`, `version` y `actorId`.
  - (b) Los puntos de OBSERV-002, incluido el límite de tasa en `/sesion/refrescar`.
- **CA:** (a) una excepción con un documento no aparece en claro en el log. (b) `/sesion/refrescar` aplica el límite.
- **DoD:** captura de logs y prueba del gateway.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

### F4.L5.M67 — `auditoria` lee los catálogos de su dueño y se borra el JSON paralelo
- **Hallazgos:** TYA-DATOS-004 (S2).
- **Causa de raíz:** el catálogo está duplicado (97.4.6), los cambios viven en memoria por réplica y los errores se tragan.
- **Corrección de raíz:** borrar `catalogos-operativos.json`, leer por API del servicio dueño (umbrales UIF de `cumplimiento`) y propagar los errores.
- **CA:** el umbral mostrado coincide con el de `cumplimiento`, y un cambio se registra solo si lo persistió el dueño.
- **DoD:** pacto y prueba con el dueño caído.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

### F4.L5.M68 — CUF de la nota de crédito tomado del SIN
- **Hallazgos:** TYA-DATOS-005 (S3, parte de `tarifas`). La parte de `transparencia` va en F3.L5.M39.
- **Causa de raíz:** se usa `hashCode` (`ConfiguracionDeTarifas.java:27-31`).
- **Corrección de raíz:** el CUF que devuelve el SIN, o el doble declarado fuera de producción.
- **CA:** dos notas de crédito no comparten CUF.
- **DoD:** prueba unitaria con el doble del SIN.
- **Depende de:** F4.L5.M47.
- **Esfuerzo:** chico.

### F4.L5.M69 — La semilla de impuestos es idempotente entre días
- **Hallazgos:** TYA-DATOS-006 (S3).
- **Causa de raíz:** usa una fecha dinámica (`05-impuestos.sql:5-7`).
- **Corrección de raíz:** fecha fija con procedencia y `ON CONFLICT` por identificador estable.
- **CA:** corrida en dos días distintos, sin una segunda fila vigente.
- **DoD:** dos corridas con reloj distinto y conteo.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L5.M70 — Claves foráneas dentro del mismo esquema
- **Hallazgos:** TYA-DATOS-009 (S3).
- **Causa de raíz:** se omitieron en el modelo. Hay unas 55 columnas `_id` sin FK.
- **Corrección de raíz:** agregar las FK en el `.puml`. Las de `organizador` y `cobertura_respaldo_id` caen con F2.
- **CA:** el inventario de `_id` sin FK queda vacío o con cada caso justificado.
- **DoD:** consulta de huérfanos y deriva.
- **Depende de:** las microtareas de F2.
- **Esfuerzo:** chico.

### F4.L5.M71 — Tablas sin uso: retirar o implementar, servicio por servicio
- **Hallazgos:** TYA-DATOS-010 (S3).
- **Causa de raíz:** el modelo se adelantó al código.
- **Corrección de raíz:**
  - Confirmar por servicio.
  - `comprobante_manual` cae en F2.L5.M33.
  - `qr_cobro` pasa a usarse en la cadena QR del lote L1.
  - El resto se retira del modelo, o se registra con su microtarea si alguna decisión lo pide (por ejemplo, `plan_regularizacion`).
- **CA:** el inventario de tablas sin referencia queda vacío o justificado.
- **DoD:** script de inventario con la salida pegada.
- **Depende de:** F2.L5.M33 y la cadena QR del lote L1.
- **Esfuerzo:** medio.

### F4.L5.M72 — Saldo incremental con verificación periódica
- **Hallazgos:** TYA-RENDIMIENTO-001 (S3).
- **Causa de raíz:** por simplicidad, `fn_ctb_recalcular_saldo` suma toda la historia en cada movimiento.
- **Corrección de raíz:** saldo incremental bajo el mismo candado más un trabajo de verificación contra la suma completa que alerta si difieren.
- **CA:** 10 000 movimientos sin degradación lineal y el verificador en 0 diferencias.
- **DoD:** prueba de carga corta y verificador.
- **Depende de:** F1.L5.M06.
- **Esfuerzo:** medio.

### F4.L5.M73 — `Traza` normaliza el identificador a UUID
- **Hallazgos:** TYA-FRAGIL-001 (S3).
- **Causa de raíz:** el identificador se tipa como `String` y hay 255 sitios con `UUID.fromString`.
- **Corrección de raíz:** `Traza` genera o normaliza un UUID en el borde.
- **CA:** una traza «sin-traza» no provoca 500.
- **DoD:** prueba web sin NGINX.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L5.M74 — Detección de duplicados por SQLSTATE y clasificación de los 4xx
- **Hallazgos:** TYA-FRAGIL-002 (S3).
- **Causa de raíz:** se toman atajos con el mensaje de la excepción (`CU103FacturaDeProveedor.java:123-130`, `RescateRepositorio.java:65-70`) y se reintenta todo lo que no es 2xx (`LibroPorHttp.java:228-237`).
- **Corrección de raíz:** usar SQLSTATE `23505` con el nombre de la restricción y reintentar solo los errores reintentables.
- **CA:** un FK roto no se informa como duplicado, y un 409 no se reintenta.
- **DoD:** pruebas unitarias.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L5.M75 — Una sola implementación de circuito, idempotencia, `Importe` y hash en `plataforma`
- **Hallazgos:** TYA-DUP-X-001 (S3).
- **Causa de raíz:** agentes trabajando en paralelo dejaron copias. Hay `ClienteConCircuito` copiado y `RegistroDeIdempotencia` con otra semántica sobre la misma tabla. Además, `record Importe` está en 7 clientes y el hex de SHA-256 en unos 12 archivos.
- **Corrección de raíz:**
  - El circuito se extrae con F4.L3.M24.
  - `nucleo-financiero` usa `Idempotencia` de plataforma.
  - `Importe` y el hash pasan a `comun-dominio`.
- **CA:** existe una sola clase de cada una en el repo.
- **DoD:** `grep` y `./gradlew test` de los servicios afectados.
- **Depende de:** F4.L3.M24 y F1.L5.M08.
- **Esfuerzo:** medio.

### F4.L5.M76 — Capas limpias en transparencia, notificaciones, auditoria, tarifas y publicidad
- **Hallazgos:** TYA-ARQ-002 (S4).
- **Causa de raíz:** atajos que no siguen ADR-033.
- **Corrección de raíz:** mover a los adaptadores los imports de aplicación en el dominio y el jOOQ y los modelos web en la aplicación.
- **CA:** una prueba de arquitectura (ArchUnit) sin violaciones.
- **DoD:** la prueba de arquitectura.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

### F4.L5.M77 — Excepciones, literales y valores por omisión
- **Hallazgos:** TYA-CLEAN-001 (S4).
- **Causa de raíz:** trabajo en paralelo sin un patrón común.
- **Corrección de raíz:**
  - Propagar las excepciones tragadas.
  - Los umbrales se configuran.
  - Los 34 `@Value(...:valor)` pasan a obligatorios cuando son secretos o parámetros (90.3.4).
  - El JSON se arma con Jackson.
  - Se borra el código muerto.
- **CA:** la sección «Cableado» de `scripts/auditar_backend.py` queda sin valores por omisión en secretos.
- **DoD:** salida del script.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

### F4.L5.M78 — `gradle.lockfile` de `inversiones`
- **Hallazgos:** TYA-CONFIG-001 (S3).
- **Causa de raíz:** omisión (90.4.1).
- **Corrección de raíz:** generar y versionar el lockfile, y que el gate falle si un servicio no lo tiene.
- **CA:** los 15 servicios tienen lockfile.
- **DoD:** `./gradlew :servicios:inversiones:dependencies --write-locks` y el chequeo del gate.
- **Depende de:** ninguna.
- **Esfuerzo:** chico.

### F4.L5.M79 — Criticidad de `transparencia` alineada con su consumidor síncrono
- **Hallazgos:** TYA-CONFIG-002 (S3).
- **Causa de raíz:** el descriptor está desactualizado. `grupos` (N2) depende de `transparencia` (N3) para el nivel de puntualidad del sorteo.
- **Corrección de raíz:** subir a N2, o desacoplar con la foto de niveles guardada por `grupos` al comprometer (F3.L3.M18).
- **CA:** el descriptor es coherente con ADR-037.
- **DoD:** el validador de descriptores.
- **Depende de:** F3.L5.M41.
- **Esfuerzo:** chico.

### F4.L5.M80 — Documentación regenerada desde el modelo
- **Hallazgos:** TYA-DOC-001 (S4).
- **Causa de raíz:** la documentación no se genera. `sql/README.md`, `aplicar.sql:11`, `application-production.yml:3-6`, `Indicador.java:8-9` y los README de `erp` y `notificaciones` describen lo que el código no hace.
- **Corrección de raíz:** generar los conteos del README desde la base aplicada y corregir el resto de los textos.
- **CA:** los números del README coinciden con la consulta.
- **DoD:** el script generador.
- **Depende de:** F0.L5.M04.
- **Esfuerzo:** chico.

### F4.L5.M81 — Paquete de evidencia para Cumplimiento sobre los riesgos regulatorios del lote
- **Hallazgos:** TYA-REG-001 (S3).
- **Causa de raíz:** los módulos de apoyo no tienen un dueño regulatorio.
- **Corrección de raíz:**
  - Reunir en un documento para Cumplimiento la evidencia de cuatro microtareas: F4.L5.M55 (supresión), F4.L5.M47 (facturación), F1.L5.M17 (registro UIF) y F4.L5.M67 (umbrales UIF).
  - Agregar las preguntas de DECISIONES («Para validar con Cumplimiento»).
  - No se declara conformidad.
- **CA:** Cumplimiento recibe el paquete y registra su respuesta.
- **DoD:** el documento en `docs/trabajo/` y el acuse de Cumplimiento.
- **Depende de:** las microtareas citadas.
- **Esfuerzo:** medio.

### F4.L5.M82 — `CifradorSimetrico` con derivación de clave, identificador de clave y AAD
- **Hallazgos:** TYA-SEG-015 (S3).
- **Causa de raíz:** diseño mínimo (`CifradorSimetrico.java:121-129`). La clave de la cuenta bancaria no rota y el cifrado se puede trasplantar entre filas.
- **Corrección de raíz:**
  - HKDF por propósito, `kid` en el texto cifrado y AAD con el id de fila.
  - Recifrado de lo existente (`CU18RegistrarCuentaDestino`).
- **CA:** un cifrado copiado a otra fila no descifra, y una rotación de clave mantiene la lectura.
- **DoD:** pruebas unitarias y de recifrado.
- **Depende de:** ninguna.
- **Esfuerzo:** medio.

---

## Trazabilidad hallazgo → microtarea (88 de 88)

| # | Hallazgo | Sev. | Microtarea | Tipo |
|---:|---|---|---|---|
| 1 | TYA-DINERO-001 | S1 | F1.L5.M06 | corrección |
| 2 | TYA-DINERO-002 | S1 | F1.L5.M13 | corrección |
| 3 | TYA-DINERO-003 | S2 | F1.L5.M08 | corrección |
| 4 | TYA-DINERO-004 | S2 | F1.L5.M09 | corrección |
| 5 | TYA-DINERO-005 | S2 | F4.L5.M46 | corrección |
| 6 | TYA-DINERO-006 | S2 | F1.L5.M29 | corrección |
| 7 | TYA-DINERO-007 | S2 | F4.L5.M47 | corrección |
| 8 | TYA-DINERO-008 | S2 | F1.L5.M12 (+F1.L5.M11) | corrección |
| 9 | TYA-DINERO-009 | S2 | F4.L5.M45 | corrección |
| 10 | TYA-DINERO-010 | S2 | F1.L5.M07 | corrección |
| 11 | TYA-DINERO-011 | S3 | F4.L5.M48 | corrección |
| 12 | TYA-DINERO-012 | S4 | F4.L5.M45 | corrección |
| 13 | TYA-FLUJO-QR-001 | S1 | F2.L5.M32 | corrección |
| 14 | TYA-APROB-001 | S1 | F1.L5.M15 + F3.L5.M37 | corrección |
| 15 | TYA-APROB-002 | S3 | F2.L5.M33 | corrección |
| 16 | TYA-RESPALDO-001 | S3 | F3.L5.M44 | corrección |
| 17 | TYA-RESPALDO-002 | S2 | F2.L5.M34 | corrección |
| 18 | TYA-SEG-001 | S1 | F1.L5.M14 | corrección |
| 19 | TYA-SEG-002 | S2 | F1.L5.M17 | corrección |
| 20 | TYA-SEG-003 | S2 | F1.L5.M18 | corrección |
| 21 | TYA-SEG-004 | S2 | F1.L5.M19 | corrección |
| 22 | TYA-SEG-005 | S2 | F1.L5.M20 | corrección |
| 23 | TYA-SEG-006 | S2 | F1.L5.M21 | corrección |
| 24 | TYA-SEG-007 | S2 | F2.L5.M35 | corrección |
| 25 | TYA-SEG-008 | S2 | F1.L5.M22 | corrección |
| 26 | TYA-SEG-009 | S2 | F1.L5.M23 | corrección |
| 27 | TYA-SEG-010 | S2 | F1.L5.M24 | corrección |
| 28 | TYA-SEG-011 | S3 | F1.L5.M25 | corrección |
| 29 | TYA-SEG-012 | S3 | F1.L5.M26 | corrección |
| 30 | TYA-SEG-013 | S3 | F1.L5.M27 | corrección |
| 31 | TYA-SEG-014 | S3 | F1.L5.M28 | corrección |
| 32 | TYA-SEG-015 | S3 | F4.L5.M82 | corrección |
| 33 | TYA-SEG-016 | S4 | F2.L5.M32 | corrección |
| 34 | TYA-ACOPLE-001 | S2 | F4.L5.M52 | corrección |
| 35 | TYA-ACOPLE-002 | S3 | F4.L5.M54 | corrección |
| 36 | TYA-DEBIDO-PROCESO-001 | S2 | F3.L5.M39 | corrección |
| 37 | TYA-DEBIDO-PROCESO-002 | S3 | F3.L5.M40 | corrección |
| 38 | TYA-HUECO-001 | S2 | F3.L5.M43 | corrección |
| 39 | TYA-HUECO-002 | S2 | F3.L5.M37 + F4.L5.M53 | corrección |
| 40 | TYA-HUECO-003 | S2 | F4.L5.M55 | corrección |
| 41 | TYA-HUECO-004 | S3 | F4.L5.M56 | corrección |
| 42 | TYA-HUECO-005 | S3 | F4.L5.M57 | corrección |
| 43 | TYA-DEFECTO-001 | S2 | F4.L5.M58 | corrección |
| 44 | TYA-DEFECTO-002 | S2 | F3.L5.M36 | corrección |
| 45 | TYA-DEFECTO-003 | S2 | F4.L5.M49 | corrección |
| 46 | TYA-DEFECTO-004 | S2 | F4.L5.M51 | corrección |
| 47 | TYA-DEFECTO-005 | S3 | F4.L5.M59 | corrección |
| 48 | TYA-DEFECTO-006 | S3 | F4.L5.M60 | corrección |
| 49 | TYA-DEFECTO-007 | S3 | F1.L5.M14 | se resuelve con |
| 50 | TYA-DEFECTO-008 | S3 | F4.L5.M50 | corrección |
| 51 | TYA-DEFECTO-009 | S4 | F2.L5.M35 | se resuelve con |
| 52 | TYA-RESILIENCIA-001 | S2 | F1.L5.M10 | corrección |
| 53 | TYA-RESILIENCIA-002 | S3 | F4.L5.M61 (a) | corrección |
| 54 | TYA-RESILIENCIA-003 | S3 | F4.L5.M61 (b) | corrección |
| 55 | TYA-RESILIENCIA-004 | S3 | F4.L5.M62 | corrección |
| 56 | TYA-RESILIENCIA-005 | S4 | F4.L5.M63 | corrección |
| 57 | TYA-CONTRATO-001 | S3 | F4.L5.M64 | corrección |
| 58 | TYA-CONTRATO-002 | S3 | F4.L5.M61 (c) | corrección |
| 59 | TYA-CONTRATO-003 | S3 | F3.L5.M42 | corrección |
| 60 | TYA-CONTRATO-004 | S3 | F3.L5.M41 | corrección |
| 61 | TYA-CONTRATO-005 | S3 | F0.L5.M05 | corrección |
| 62 | TYA-TRAZA-001 | S3 | F4.L5.M65 | corrección |
| 63 | TYA-OBSERV-001 | S3 | F4.L5.M66 (a) | corrección |
| 64 | TYA-OBSERV-002 | S4 | F4.L5.M66 (b) | corrección |
| 65 | TYA-DATOS-001 | S2 | F1.L5.M30 | corrección |
| 66 | TYA-DATOS-002 | S2 | F1.L5.M31 | corrección |
| 67 | TYA-DATOS-003 | S2 | F3.L5.M38 | corrección |
| 68 | TYA-DATOS-004 | S2 | F4.L5.M67 | corrección |
| 69 | TYA-DATOS-005 | S3 | F3.L5.M39 + F4.L5.M68 | corrección |
| 70 | TYA-DATOS-006 | S3 | F4.L5.M69 | corrección |
| 71 | TYA-DATOS-007 | S3 | F0.L5.M04 | corrección |
| 72 | TYA-DATOS-008 | S2 | F1.L5.M11 | corrección |
| 73 | TYA-DATOS-009 | S3 | F4.L5.M70 | corrección |
| 74 | TYA-DATOS-010 | S3 | F4.L5.M71 | corrección |
| 75 | TYA-RENDIMIENTO-001 | S3 | F4.L5.M72 | corrección |
| 76 | TYA-FRAGIL-001 | S3 | F4.L5.M73 | corrección |
| 77 | TYA-FRAGIL-002 | S3 | F4.L5.M74 | corrección |
| 78 | TYA-DUP-X-001 | S3 | F4.L5.M75 | corrección |
| 79 | TYA-ARQ-001 | S3 | F1.L5.M16 | corrección |
| 80 | TYA-ARQ-002 | S4 | F4.L5.M76 | corrección |
| 81 | TYA-CLEAN-001 | S4 | F4.L5.M77 | corrección |
| 82 | TYA-CONFIG-001 | S3 | F4.L5.M78 | corrección |
| 83 | TYA-CONFIG-002 | S3 | F4.L5.M79 | corrección |
| 84 | TYA-PRUEBAS-001 | S2 | F0.L5.M03 | corrección |
| 85 | TYA-PRUEBAS-002 | S3 | F0.L5.M02 | corrección |
| 86 | TYA-DOC-001 | S4 | F4.L5.M80 | corrección |
| 87 | TYA-REG-001 | S3 | F4.L5.M81 | corrección |
| 88 | TYA-COHERENCIA-001 | S3 | F0.L5.M01 | corrección |

Verificación: las 88 filas coinciden con las 88 fichas `### TYA-*` del informe. Por severidad: 5 S1, 34 S2, 41 S3 y 8 S4, igual que la fila «Total» del informe.
