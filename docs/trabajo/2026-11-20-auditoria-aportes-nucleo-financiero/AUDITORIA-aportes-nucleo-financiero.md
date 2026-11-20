# Auditoría — aportes-nucleo-financiero (lote L1, solo lectura)

> Documento en construcción incremental (indicación de Pablo, 2026-11-20): cada hallazgo se cataloga al identificarse y se sube a la rama tras cada tanda. Las secciones vacías se completan en las tandas siguientes.

## 1. Ficha
- Módulo: `aportes-nucleo-financiero` — `servicios/aportes`, `servicios/nucleo-financiero`, sus tablas, pactos y pantallas de cobro/billetera.
- Responsable: Pablo (dueño del producto) · Fecha: 2026-11-20
- Commit auditado: `origin/test` @ `07fc69719287b0dadd152d0293a054948df69ead`. `origin/dev` @ `850e0f51` **no difiere** en `servicios/aportes` ni `servicios/nucleo-financiero` (`git diff --stat origin/test origin/dev -- servicios/aportes servicios/nucleo-financiero` → vacío).
- Historial: completo (`git fetch --unshallow`; `--is-shallow-repository` → `false`).
- Archivos/líneas cubiertos: _se completa al integrar la lectura (anexo 12)_.

## 2. Resumen
_Se completa al cierre._

## 3. Huella y mapa de conexiones
_En curso._

## 4. Distancia al flujo nuevo
_En curso._

## 5. Hallazgos

### aportes-nucleo-financiero-FLUJO-QR-001 — El aporte se da por cobrado por declaración del cliente, sin confirmación de la pasarela
- Clase / Severidad / Confianza: FLUJO-QR (también DINERO, SEG) / **S1** / confirmado
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/web/AportesController.java:126-169`; `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CobrarAporte.java:58-172`; `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/PagoRepositorio.java:42`; `servicios/aportes/src/main/resources/openapi/aportes.yaml:596-608`
- Evidencia:
  ```java
  // AportesController.java:143-156 — el cuerpo del cliente decide canal, monto, referencia y esManual
  var salida = cu21.acreditar(new CU21CobrarAporte.EntradaCobro(idempotencyKey.toString(), obligacionId,
          MapeoDeAportes.dinero(cuerpo.getMonto()), …, cuerpo.getCanal().getValue(),
          cuerpo.getReferenciaProveedor(), Optional.ofNullable(cuerpo.getProveedorId()),
          Boolean.TRUE.equals(cuerpo.getEsManual()), admisibilidad.periodoAbierto()), ctx);
  // PagoRepositorio.java:42 — el pago nace acreditado
  .set(DSL.field("estado", String.class), "ACREDITADO")
  // CU21CobrarAporte.java:127-128,134-137 — la obligación sube monto_pagado y se emite aporte_cobrado
  obligaciones.acreditar(dsl, obligacion.id(), entrada.monto(), estadoNuevo, …);
  outbox.emitir(dsl, new EventoDominio("aportes.aporte_cobrado", …
  ```
  El contrato acepta `canal` ∈ `[BILLETERA_MOVIL, COMPENSACION_INTERNA, EFECTIVO_AL_ORGANIZADOR, FONDO_GARANTIA, QR_INTEROPERABLE, TARJETA, TRANSFERENCIA_BANCARIA]` (`aportes.yaml:603-606`) del propio cuerpo de la petición (permiso `BILLETERA_OPERAR`, titular verificado vía `grupos`).
- Cómo se comprobó: lectura completa de `CU21CobrarAporte.java`, del método `cobrarAporte` del controlador y de `CU100RecibirWebhookPasarela.java`; `grep -n '"estado"' PagoRepositorio.java`. El propio javadoc de CU100 lo declara: «`CU21CobrarAporte` acredita de forma síncrona» (`CU100RecibirWebhookPasarela.java:43-49`). No ejecutado (Gradle bloqueado, ver Límites).
- Impacto: un participante (o un cliente modificado) que envía `canal=QR_INTEROPERABLE` con una referencia cualquiera deja su cuota `PAGADO` sin que exista dinero. Esa cuota cuenta como «confirmado» para la entrega del turno (ver FLUJO-QR-003). El beneficiario y el grupo reciben un pozo que no existe en caja; la empresa queda con un descalce que, con el respaldo vigente, cubriría ella (R-NUEVA-2).
- Causa de fondo: falta la cadena `orden_cobro → intento_pago → webhook` (tablas existen en `sql/10_tablas/03_aportes_pagos_qr/orden_cobro.sql`, `intento_pago.sql`; sin código que las use, ya señalado en `docs/auditoria-produccion/idempotencia-scope.md` como «NO IMPLEMENTADO» y **sigue vigente** en este commit).
- Solución de fondo: el endpoint de cobro solo crea una `orden_cobro` (estado pendiente, QR emitido); el paso a `ACREDITADO`, el incremento de `monto_pagado` y la emisión de `aporte_cobrado` ocurren **únicamente** en el procesamiento del webhook con firma verificada. El canal lo fija el servidor, nunca el cliente. Para `BILLETERA_MOVIL` el débito lo confirma `nucleo-financiero` antes de marcar pagado.
- Relación con el flujo nuevo: FLUJO-QR (contradice R-NUEVA-1 y regla 91.3.1)
- Conexiones y módulos afectados: `nucleo-financiero` (consumidor `aporte_cobrado`), `entregas`/`garantia` (consultan recaudo), `grupos` (sorteo exige primeras cuotas `ACREDITADO`, ADR-054 D3), app móvil.
- Estándares y normas: ISO/IEC 25010 Seguridad (integridad, no repudio) y Adecuación funcional (corrección); CWE-345 (verificación insuficiente de la autenticidad de los datos); OWASP API Security Top 10 (lógica de negocio). Reglas 91.3.1, 91.1.5, 96.2.
- Esfuerzo estimado: grande

### aportes-nucleo-financiero-FLUJO-QR-002 — El webhook de la pasarela no confirma nada: solo archiva
- Clase / Severidad / Confianza: FLUJO-QR (también HUECO) / **S1** / confirmado
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU100RecibirWebhookPasarela.java:147-166`
- Evidencia:
  ```java
  var pago = pagoOpt.get();
  Dinero montoDelEvento = montoDe(evento);
  if (montoDelEvento == null || !montoDelEvento.equals(pago.monto())) { … "DESCARTADO" … }
  webhooks.marcarProcesado(dsl, webhookId, "PROCESADO", null, pago.id(), ahora);
  return Resultado.procesado(webhookId, pago.id());
  ```
- Cómo se comprobó: lectura completa del caso de uso (224 líneas). No hay transición del `pago`, ni de la `obligacion_aporte`, ni evento: el único efecto es la fila en `webhook_pasarela`. Tampoco se distingue el tipo de evento (pago fallido, reversado, contracargo): `tipoEvento` solo se guarda (`:141`).
- Impacto: el único mecanismo que según R-NUEVA-1 debe mover el flujo no mueve nada; un webhook de pago **rechazado** o **reversado** que coincida en monto se marca `PROCESADO` igual. Un pago que la pasarela nunca confirma queda `ACREDITADO` para siempre.
- Causa de fondo: el CU se construyó como control de seguridad transversal sobre un `pago` ya acreditado (javadoc `:43-49`, «simplificación de alcance, declarada»), no como el disparador del cobro.
- Solución de fondo: el webhook verificado es la transición `orden_cobro PENDIENTE → PAGADA` y dispara (en la misma transacción, por outbox) el registro del `pago` y `aporte_cobrado`; los eventos de fallo/reverso generan su transición inversa con contra-asiento.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: los mismos de FLUJO-QR-001.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (completitud); regla 91.3.1, 98.3.3.
- Esfuerzo estimado: grande

### aportes-nucleo-financiero-FLUJO-QR-003 — El «confirmado» que usa la entrega del pozo sale de `monto_pagado`, no del mayor ni de la pasarela
- Clase / Severidad / Confianza: FLUJO-QR (también DINERO 91.1.5) / **S1** / confirmado
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/web/AportesController.java:207-229`; `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/RecaudoRepositorio.java:16-18,33,58`; `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/RecaudoDelPeriodo.java:47-72`
- Evidencia: `RecaudoRepositorio.java:16-18` (javadoc): «`monto_pagado` solo lo … pago: por eso es la fuente de lo «confirmado». Contrastarlo contra el mayor es trabajo …»; el endpoint `consultarRecaudoDelPeriodo` (permiso `ENTREGA_EJECUTAR`) devuelve `confirmado`, `cubiertoMutual` y `faltante` calculados desde ese campo.
- Cómo se comprobó: `grep -rn "confirmado\|monto_pagado"` en `servicios/aportes/src/main` y lectura de las líneas citadas.
- Impacto: con FLUJO-QR-001, un aporte declarado por QR y nunca pagado se suma a la caja del pozo que `entregas` liquida. Con canal `BILLETERA_MOVIL` que luego falla por saldo (`aporte_no_aplicado`), hay una ventana hasta la compensación en la que el pozo aparece más lleno de lo real.
- Causa de fondo: dos fuentes de verdad (obligación de `aportes` vs. mayor de `nucleo-financiero`) sin conciliación en el camino de la entrega.
- Solución de fondo: el importe con el que se decide una entrega se lee del dueño del dinero (saldo de la cuenta del grupo en el mayor de `nucleo-financiero`, regla 98.4.2), o `monto_pagado` solo sube tras la aplicación confirmada en el mayor.
- Relación con el flujo nuevo: FLUJO-QR; alimenta FLUJO-FALTANTE (el `faltante` que dispara el respaldo).
- Conexiones y módulos afectados: `entregas` (lote L2), `garantia`.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección); reglas 91.1.5, 98.4.2.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-FLUJO-QR-004 — Con canal distinto de billetera, `nucleo-financiero` no mueve dinero pero la cuota ya quedó pagada
- Clase / Severidad / Confianza: FLUJO-QR (también HUECO) / **S1** / confirmado
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ConsumoDeAportes.java:61-63,78,139-140`
- Evidencia:
  ```java
  // :61 «Alcance declarado: solo el canal BILLETERA_MOVIL sale de una billetera. Los demas canales (QR, …»
  if (!CANAL_BILLETERA.equals(aporte.canal())) {
      return Salida.sinEfecto(Resultado.CANAL_SIN_BILLETERA);
  }
  ```
- Cómo se comprobó: `grep -n` sobre el consumidor y lectura de las líneas.
- Impacto: un aporte por QR (el único canal que R-NUEVA-1 admite) no deja asiento en el mayor ni acredita la cuenta del grupo; `aportes` lo da por pagado. La caja del grupo en el mayor y el «confirmado» de `aportes` divergen por construcción.
- Causa de fondo: la contrapartida contable del QR (cuenta puente de custodia/pasarela → cuenta del grupo) no está implementada; el javadoc la remite a «conciliación y su propio asiento», que no existe en este flujo.
- Solución de fondo: el webhook verificado del QR genera el asiento `DEBE custodia/pasarela · HABER SALDO_GRUPOS` con clave de idempotencia derivada del evento de la pasarela.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: `aportes`, `entregas`.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (completitud); reglas 91.1.5, 91.6.1.
- Esfuerzo estimado: grande

### aportes-nucleo-financiero-SEG-001 — En TEST el secreto del webhook de aportes es un literal público del repositorio
- Clase / Severidad / Confianza: SEG (también CONFIG) / **S2** / probable (falta confirmar el valor de `PERFIL_SPRING` en Coolify; el repo dice `local`)
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/SecretoWebhookDeDesarrollo.java:18-26`; `docker-compose.coolify.yml:83-112` (servicio `aportes` sin variable del secreto); `despliegue/TEST.md:84-90,455-458`
- Evidencia: `@Profile({"local", "test"})` y `@Value("${aportaya.aportes.webhook.secreto-de-prueba:<literal por omisión>}")` (valor omitido aquí); `TEST.md:458`: «Se queda en `PERFIL_SPRING=local`».
- Cómo se comprobó: lectura del bean; `grep -rn "WEBHOOK_SECRETO"` (solo `notificaciones` lo recibe, `docker-compose.coolify.yml:555`); `auditar_backend.py` lo marca: «`aportaya.aportes.webhook.secreto-de-prueba` es un secreto y solo tiene valor por omision».
- Impacto: en el servidor de TEST cualquiera que lea el repo puede firmar webhooks válidos. Hoy el efecto es acotado porque el webhook no acredita (FLUJO-QR-002), pero en cuanto el webhook pase a ser el disparador del cobro, esto es acreditación sin pago. Regla 91.3.2: «La firma del webhook se valida siempre, incluso en desarrollo».
- Causa de fondo: el doble de la pasarela se confina por perfil, y TEST corre con el perfil de desarrollo.
- Solución de fondo: secreto obligatorio por entorno, inyectado por variable y sin valor por omisión (falla al arrancar si falta, regla 90.3.4); el doble con secreto fijo solo en pruebas automáticas.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: despliegue TEST (transversal, L5).
- Estándares y normas: ISO/IEC 25010 Seguridad (autenticidad); CWE-798 (credenciales embebidas); reglas 90.3.4, 91.3.2.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-RESPALDO-001 — Canales `FONDO_GARANTIA` y `EFECTIVO_AL_ORGANIZADOR` aceptados para cobrar un aporte
- Clase / Severidad / Confianza: RESPALDO (también DINERO RN-18) / **S2** / probable (falta ver si algún cliente los envía; lo revisa el lote de frontends)
- Ubicaciones: `servicios/aportes/src/main/resources/openapi/aportes.yaml:603-606`; `sql/10_tablas/03_aportes_pagos_qr/pago.sql:27`
- Evidencia: `CONSTRAINT ck_pago_canal CHECK (canal IN ('BILLETERA_MOVIL', 'COMPENSACION_INTERNA', 'EFECTIVO_AL_ORGANIZADOR', 'FONDO_GARANTIA', 'QR_INTEROPERABLE', 'TARJETA', 'TRANSFERENCIA_BANCARIA'))`
- Cómo se comprobó: lectura del contrato y del DDL.
- Impacto: un participante con `BILLETERA_OPERAR` puede declarar su cuota pagada «por el fondo de garantía» o «en efectivo al organizador». Lo primero contradice R-NUEVA-2; lo segundo, la regla 91.5 (el dinero del grupo no pasa por el organizador).
- Causa de fondo: el enum del contrato público replica el catálogo interno de canales del esquema.
- Solución de fondo: el contrato público solo admite el canal que el servidor emite (QR); los canales internos, si se conservan, solo por operaciones de sistema. La continuidad de `FONDO_GARANTIA` es DECISIÓN REQUERIDA (A-3).
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: `garantia` (L2), `organizador` (L3).
- Estándares y normas: ISO/IEC 25010 Seguridad (integridad); reglas 91.5, 90.1.5.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-CONTRATO-001 — `referenciaProveedor` admite 100 caracteres en el contrato y 80 en la base
- Clase / Severidad / Confianza: CONTRATO (también DATOS) / **S3** / confirmado por lectura (sin ejecución)
- Ubicaciones: `servicios/aportes/src/main/resources/openapi/aportes.yaml:607`; `sql/10_tablas/03_aportes_pagos_qr/pago.sql:18`
- Evidencia: `referenciaProveedor: { type: string, maxLength: 100 }` frente a `referencia_proveedor VARCHAR(80) NOT NULL`.
- Cómo se comprobó: lectura de ambas líneas.
- Impacto: una referencia de 81 a 100 caracteres pasa la validación y falla en el `INSERT` (error de base, probable 500 en lugar del error de contrato, regla 96.2.4).
- Causa de fondo: contrato y esquema mantenidos a mano por separado.
- Solución de fondo: alinear el límite desde la fuente de verdad del esquema y verificarlo en `verificar_contratos_limites.py`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: clientes generados.
- Estándares y normas: ISO/IEC 25010 Fiabilidad (tolerancia a fallos); RFC 9457; regla 96.2.4.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-DEFECTO-001 — El `recargo` del cuerpo se registra como comisión del proveedor y reduce el neto
- Clase / Severidad / Confianza: DEFECTO (también DDD) / **S2** / confirmado por lectura
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/web/AportesController.java:147-150`; `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CobrarAporte.java:107-109`; `sql/10_tablas/03_aportes_pagos_qr/pago.sql:12-13`
- Evidencia: el controlador pasa `cuerpo.getRecargo()` como `comision` de `EntradaCobro`; CU21 lo persiste en `monto_comision_proveedor` y calcula `entrada.monto().menos(entrada.comision())` como `monto_neto_acreditado`.
- Cómo se comprobó: lectura de ambas ubicaciones.
- Impacto: un cliente que manda un recargo reduce el neto acreditado del pago en ese importe, y el recargo de mora queda contabilizado como comisión de la pasarela; `monto_comision_proveedor` es `NUMERIC(10,2)` y el recargo viene sin tope de contrato. El campo lo controla el cliente.
- Causa de fondo: concepto del lenguaje ubicuo («recargo» vs. «comisión del proveedor») mapeado al campo equivocado.
- Solución de fondo: la comisión del proveedor la informa la pasarela en el webhook; el recargo es su propia obligación `RECARGO_MORA` y no viaja en el cobro de la cuota.
- Relación con el flujo nuevo: ninguna directa (A-4)
- Conexiones y módulos afectados: conciliación, reportes.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección); regla 91.3.4.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-OBSERV-001 — Importes atribuibles a una persona en logs de nivel INFO
- Clase / Severidad / Confianza: OBSERV (también SEG) / **S3** / confirmado por lectura
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CobrarAporte.java:170`
- Evidencia: `BITACORA.info("CU-21 · Aporte cobrado registrado: pago={}, obligacion={}, monto={}, estado={}", pagoId, obligacion.id(), entrada.monto(), estadoNuevo)`
- Cómo se comprobó: lectura. No se comprobó si el enmascarador de PII de la configuración de logs cubre el campo `monto` (pendiente).
- Impacto: la regla 90.2.1 pide loguear identificadores, no importes atribuibles a una persona.
- Causa de fondo: log de depuración convertido en log de negocio.
- Solución de fondo: quitar el importe del log; si hace falta, que lo registre la auditoría con acceso controlado.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Seguridad (confidencialidad); regla 90.2.1.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-DINERO-001 — Un reembolso con importe negativo pasa todas las validaciones y, al aprobarse, sube lo pagado de la cuota
- Clase / Severidad / Confianza: DINERO (también DATOS) / **S1** / probable (lectura completa del camino; falta ejecutarlo contra PostgreSQL, bloqueado por entorno)
- Ubicaciones: `servicios/aportes/src/main/resources/openapi/aportes.yaml:788-790`; `servicios/aportes/src/main/java/bo/aportaya/aportes/web/MapeoDeAportes.java:20-24`; `plataforma/comun-dominio/.../Dinero.java:36-45`; `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU19ReembolsarPago.java:88-99,141-148`; `sql/10_tablas/03_aportes_pagos_qr/reembolso.sql:8,17-18`; `sql/10_tablas/03_aportes_pagos_qr/obligacion_aporte.sql:17,30`
- Evidencia: `pattern: '^-?\d+\.\d{2}$'` (el esquema `Dinero` admite signo); `Dinero.de` solo valida escala, no signo; CU19: `if (entrada.monto().esMayorQue(disponible))` es falso para un negativo; `reembolso.monto NUMERIC(14,2) NOT NULL` sin `CHECK`; al aprobar, `obligacion.saldo().pagado().menos(reembolso.monto())` y `revertirPago` hace `monto_pagado - (monto)`; `monto_pagado` no tiene `CHECK` (solo `monto_esperado > 0`).
- Cómo se comprobó: lectura de cada línea citada; `grep -rn "signum\|negativ"` en `servicios/aportes/src/main/java` (solo `CU21AbrirPrimeraCuota.java:109`); `grep` de `reembolso` y `monto_pagado` en `sql/20_claves` y `sql/40_reglas` sin `CHECK` aplicable.
- Impacto: con una solicitud negativa (cualquier `BILLETERA_OPERAR`, ver SEG-002) y una aprobación de un segundo usuario con `REVERSO_AUTORIZAR`, la cuota queda con más `monto_pagado` del que entró y puede pasar a `PAGADO` sin dinero; `reembolso_ejecutado` viaja con importe negativo a sus consumidores. Viola 91.2.2 («nunca es negativo; el signo lo da la cuenta»).
- Causa de fondo: el tipo `Dinero` del contrato y del dominio no distingue importe (≥0) de saldo con signo, y la base no tiene la invariante.
- Solución de fondo: esquema de contrato sin signo para importes, `Dinero` (o un `Importe`) que rechace negativos en el borde, y `CHECK (monto > 0)` en `reembolso`, `CHECK (monto_pagado >= 0)` y equivalentes en `monto_neto_acreditado`, `monto_comision_proveedor`, `disputa_pago.monto_disputado`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: consumidores de `aportes.reembolso_ejecutado` (nucleo-financiero), `entregas` vía recaudo.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección), Seguridad (integridad); CWE-20 (validación de entrada), CWE-839 (comparación numérica sin mínimo); reglas 91.2.2, 96.2.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-SEG-002 — Endpoints de aportes sin verificación de propiedad del recurso (BOLA)
- Clase / Severidad / Confianza: SEG / **S2** / confirmado por lectura (sin ejecución)
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/web/AportesController.java:80-93` (`consultarEstadoDelParticipante`, `BILLETERA_VER`), `:95-103` (`contarMorosos`), `:108-124` (`consultarPrimeraCuota`), `:207-229` (`consultarRecaudoDelPeriodo`, `ENTREGA_EJECUTAR`); `servicios/aportes/src/main/java/bo/aportaya/aportes/web/PagosController.java:56-72` → `CU19ReembolsarPago.java:76-99` (`solicitarReembolso`, `BILLETERA_OPERAR`); `sql/40_reglas/restricciones.sql:1410-1418` (RLS solo para tablas con `usuario_id`/`cuenta_billetera_id`)
- Evidencia: `@Permiso("BILLETERA_VER") public ResponseEntity<EstadoDelParticipante> consultarEstadoDelParticipante(UUID participanteId) { … estados.ejecutar(participanteId, sesion.actual()) …`; `ConsultarEstadoDelParticipante.java` no contiene comparación con el usuario de la sesión (`grep -n "usuarioId\|titular"` vacío); CU19 `solicitar` solo verifica que el pago exista (`:78`) antes de insertar.
- Cómo se comprobó: lectura de los métodos y de los casos de uso; `grep` de verificaciones de titularidad. El único endpoint que verifica propiedad es el cobro (`AportesController.java:133-141`, vía `grupos`).
- Impacto: cualquier participante con `BILLETERA_VER` lee deuda, total aportado y estado de mora de otro participante cambiando el id; cualquiera con `BILLETERA_OPERAR` solicita el reembolso de un pago ajeno (lo frena la aprobación de cuatro ojos, pero queda la solicitud y el evento `reembolso_solicitado`).
- Causa de fondo: autorización por rol sin chequeo a nivel de objeto; las tablas de `aportes` no tienen RLS por titular.
- Solución de fondo: verificación de pertenencia en cada endpoint (o RLS por `participante_id` resuelto desde la sesión) y matriz de autorización negativa en los tests web.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `grupos` (resolución participante→usuario), app móvil.
- Estándares y normas: ISO/IEC 25010 Seguridad (confidencialidad, integridad); OWASP API Security Top 10 API1 (BOLA); CWE-639; reglas 90.1.3, 96.5.2, 80.2.3.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-DEFECTO-002 — La política de mora `DIARIO_COMPUESTO` que admite la base no existe en Java y el fallo se traga
- Clase / Severidad / Confianza: DEFECTO (también CONTRATO, RESILIENCIA) / **S2** / confirmado por lectura
- Ubicaciones: `sql/10_tablas/03_aportes_pagos_qr/politica_mora.sql:17`; `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/RecargoDeMora.java:21-25`; `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CobrarAporte.java:217-219`; `servicios/aportes/src/main/java/bo/aportaya/aportes/trabajos/RecargosDeMoraDiarios.java:54-56`
- Evidencia: base `CHECK (tipo_recargo IN ('DIARIO_COMPUESTO', 'FIJO', 'PORCENTUAL'))`; Java `enum Tipo { FIJO, PORCENTUAL, PORCENTUAL_DIARIO }`; `RecargoDeMora.Tipo.valueOf(p.tipoRecargo())`; el trabajo `catch (RuntimeException fallo) { LOG.warn(…`.
- Cómo se comprobó: lectura de las cuatro ubicaciones.
- Impacto: una sola política `DIARIO_COMPUESTO` hace fallar `valueOf` dentro de la transacción del trabajo diario: se revierte la corrida entera (ningún grupo recibe recargo) y solo queda un `warn`. `PORCENTUAL_DIARIO` es inalcanzable desde la base. Además `CU21CobrarAporte.java:218` pasa `0` como días de gracia de la política (ignora `politica_mora.dias_gracia`).
- Causa de fondo: catálogo cerrado modelado como enum del lenguaje y como `CHECK` por separado (regla 97.4.7).
- Solución de fondo: un único catálogo codificado; el trabajo aísla la falla por obligación y alerta.
- Relación con el flujo nuevo: ninguna directa (A-4)
- Conexiones y módulos afectados: `nucleo-financiero` (el recargo va al fondo de garantía, ver RESPALDO-002).
- Estándares y normas: ISO/IEC 25010 Fiabilidad (tolerancia a fallos), Adecuación funcional; reglas 97.4.7, 96.7.4.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-HUECO-001 — Las cuotas de los periodos 2..N solo nacen si un administrador las dispara a mano
- Clase / Severidad / Confianza: HUECO (también APROB) / **S2** / confirmado
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/web/AportesController.java:236-242`; `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml:15-17`
- Evidencia: `@Permiso("ADMIN_PLATAFORMA") … generarObligacionesDelPeriodo(…) { … calendario.generar(periodoId, sesion.actual())`; catálogo `id: generar-obligaciones · actor: ADMIN_PLATAFORMA`.
- Cómo se comprobó: `grep -rn "calendario.generar\|\.generar("` en `servicios/aportes` → único llamador `AportesController.java:242`. La primera cuota sí es automática (`ConsumidorDeGrupoIniciadoKafka` → `CU21AbrirPrimeraCuota`).
- Impacto: sin un humano que llame el endpoint cada periodo, no hay obligaciones, no hay cobro y no hay entrega. Es una habilitación manual en el circuito (R-NUEVA-1).
- Causa de fondo: falta un disparador por evento de apertura de periodo o por trabajo programado.
- Solución de fondo: consumidor de apertura de periodo (o trabajo con ShedLock) idempotente por `(periodo, cupo)`, que ya garantiza `uq_obligacion_periodo_cupo`.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: `grupos` (L3).
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (completitud); regla 96.7.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-HUECO-002 — Si `nucleo-financiero` no pudo aplicar el aporte y la compensación no procede, la cuota queda pagada sin dinero y sin alerta
- Clase / Severidad / Confianza: HUECO (también RESILIENCIA) / **S2** / confirmado por lectura
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CompensarAporte.java:95-124`; `servicios/aportes/src/main/java/bo/aportaya/aportes/consumo/ConsumidorDeAportesNoAplicadosKafka.java` (rama `NO_COMPENSABLE`)
- Evidencia: `return sinEfecto(Resultado.NO_COMPENSABLE);` y el consumidor solo registra un `warn`; el evento queda consumido.
- Cómo se comprobó: lectura del caso de uso y del consumidor.
- Impacto: el estado «pagado» de la obligación y el mayor quedan divergentes para siempre, sin tarea, expediente ni alerta; el «confirmado» de la entrega (FLUJO-QR-003) lo cuenta.
- Causa de fondo: la saga de cobro no tiene estado persistido ni paso de escalamiento.
- Solución de fondo: con el cobro disparado solo por confirmación (FLUJO-QR-001/002) desaparece la necesidad de compensar; mientras tanto, persistir la excepción y alertar.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: `nucleo-financiero`.
- Estándares y normas: ISO/IEC 25010 Fiabilidad (recuperabilidad); reglas 98.3.2, 96.7.4.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-DATOS-001 — Ninguna tabla de `aportes` es append-only y el rol de la aplicación puede modificarlas
- Clase / Severidad / Confianza: DATOS (también TRAZA) / **S2** / confirmado por lectura
- Ubicaciones: `sql/35_append_only/` (sin entradas de `aportes.*`); `sql/00_base/02_esquemas.sql:79-81`; `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/WebhookRepositorio.java:64-75` (UPDATE de estado del webhook, cuyo comentario `:12-20` lo llama append-only); `PagoRepositorio.java:107-116` (UPDATE a `REVERSADO`)
- Evidencia: `grep -n "aportes\." sql/35_append_only/*.sql` → vacío; `GRANT USAGE ON SCHEMA aportes TO svc_aportes; ALTER DEFAULT PRIVILEGES IN SCHEMA aportes …`.
- Cómo se comprobó: grep y lectura.
- Impacto: pagos, webhooks y reembolsos son registros de dinero que la aplicación (o un script con su rol) puede reescribir sin rastro; la regla 91.1.3/91.2.5 exige `UPDATE`/`DELETE` revocados en el rol.
- Causa de fondo: el modelo de estados de `pago` y `webhook_pasarela` se implementa con `UPDATE` en lugar de eventos de estado append-only.
- Solución de fondo: separar el hecho inmutable (pago recibido, webhook recibido) de su estado (tabla de transiciones append-only) y revocar `UPDATE/DELETE` sobre los hechos.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: auditoría.
- Estándares y normas: ISO/IEC 25010 Seguridad (no repudio, responsabilidad); reglas 91.1.3, 91.2.5.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-SEG-003 — La firma del webhook no cubre el timestamp
- Clase / Severidad / Confianza: SEG / **S3** / confirmado por lectura
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/VerificadorDeFirmaWebhook.java:30-36`; `CU100RecibirWebhookPasarela.java:98-114`
- Evidencia: `byte[] firma = mac.doFinal(cuerpoCrudo.getBytes(StandardCharsets.UTF_8));` — la ventana de ±5 min se evalúa sobre `X-Timestamp`, que no está firmado.
- Cómo se comprobó: lectura.
- Impacto: un cuerpo capturado se puede reenviar con un timestamp nuevo; hoy lo mitiga la deduplicación por `idEvento`, pero la ventana no aporta protección.
- Causa de fondo: esquema de firma propio, sin cubrir la cabecera temporal.
- Solución de fondo: firmar `timestamp + "." + cuerpo` (o el esquema que publique la pasarela contratada).
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Seguridad (autenticidad); CWE-294 (replay).
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-DINERO-002 — Reembolsos sin moneda: se leen siempre como BOB
- Clase / Severidad / Confianza: DINERO / **S3** / confirmado por lectura
- Ubicaciones: `sql/10_tablas/03_aportes_pagos_qr/reembolso.sql` (sin columna `moneda`); `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/PagoRepositorio.java:198`
- Evidencia: `Dinero.de(f.get("monto", BigDecimal.class), Moneda.BOB)`.
- Cómo se comprobó: grep de `moneda` en el DDL (vacío) y lectura.
- Impacto: el reembolso de un pago en USD se trata como BOB; la aprobación falla al operar monedas distintas o, peor, devuelve en otra moneda. Regla 91.1.2.
- Causa de fondo: columna de importe sin moneda al lado.
- Solución de fondo: `moneda CHAR(3)` en `reembolso` (y `disputa_pago`, `conciliacion`, `excepcion_conciliacion`, `plan_regularizacion`, `extracto_bancario`, según el lote A — pendiente de verificar cada una).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional; ISO 4217; regla 91.1.2.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-UX-A11Y-001 — La app ofrece declarar un aporte por QR, transferencia o tarjeta escribiendo la referencia de un comprobante
- Clase / Severidad / Confianza: UX-A11Y (causa: FLUJO-QR, APROB) / **S1** / confirmado (servidor: FLUJO-QR-001)
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/pantalla_aportar.dart:23-36,110-161`; `apps/movil/lib/pantallas/pasanaku/textos_de_conversion.dart:41-43`
- Evidencia: canales `BILLETERA_MOVIL`, `QR_INTEROPERABLE`, `TRANSFERENCIA_BANCARIA`, `TARJETA` elegibles por la persona; campo «referencia del pago» con ayuda «El número de operación de tu comprobante.».
- Cómo se comprobó: lectura de la pantalla; el servidor acredita sin confirmación (FLUJO-QR-001).
- Impacto: el cliente oficial del producto implementa la autodeclaración del pago que R-NUEVA-1 prohíbe.
- Causa de fondo: CU-21 diseñado como «aplicar pago informado».
- Solución de fondo: la pantalla de aporte genera la orden y muestra el QR dinámico, y espera la confirmación del servidor; sin campo de referencia.
- Relación con el flujo nuevo: FLUJO-QR / APROB
- Conexiones y módulos afectados: `aportes`.
- Estándares y normas: ISO/IEC 25010 Seguridad (integridad); regla 91.3.1.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-HUECO-003 — No hay en la app un camino navegable para pagar un aporte
- Clase / Severidad / Confianza: HUECO / **S2** / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart:94-100` (única definición de `pasanaku.aportar`); `apps/movil/lib/pantallas/billetera/acciones_de_saldo.dart:22-26`; `apps/movil/lib/navegacion/enlaces_profundos.dart:16`
- Evidencia: `grep -rn "pasanaku.aportar\|'aportar'" apps/movil/lib` → solo `rutas_operacion.dart:96`; la acción «Aportar» navega a `pasanaku.miEstado`; el enlace profundo `aporte` apunta a `/billetera/aportes`.
- Cómo se comprobó: grep (subagente D) y re-ejecución propia del grep.
- Impacto: en producción la persona no tiene cómo aportar desde la app; el flujo cobrado-por-QR no tiene superficie.
- Causa de fondo: pantalla construida y nunca enlazada.
- Solución de fondo: con la pantalla de cobro por QR (UX-A11Y-001), enlazarla desde «Mi estado» y desde el aviso.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: navegación de la app (L6).
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (completitud).
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-SEG-004 — El importe a cobrar viaja en la URL de la pantalla de aporte
- Clase / Severidad / Confianza: SEG (también DEFECTO) / **S3** / confirmado por lectura
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart:98-99`; `apps/movil/lib/pantallas/pasanaku/pantalla_aportar.dart:137-142`
- Evidencia: `montoDelPeriodo: state.uri.queryParameters['monto'] ?? '0.00'`; moneda fija `'BOB'`.
- Cómo se comprobó: lectura.
- Impacto: importe atribuible en la URL (regla 90.2.2); el monto lo decide el cliente; sin parámetro se cobra `0.00`.
- Causa de fondo: la pantalla no consulta la obligación al servidor.
- Solución de fondo: leer monto y moneda de la obligación por id.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `aportes`.
- Estándares y normas: ISO/IEC 25010 Seguridad (confidencialidad); CWE-598; regla 90.2.2.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-RESPALDO-002 — La web pública promete que «el fondo cubre la entrega» si alguien no aporta
- Clase / Severidad / Confianza: RESPALDO (también DOC) / **S3** / confirmado
- Ubicaciones: `apps/web/src/app/paginas/inicio/secciones/producto.ts:75`; `apps/web/src/app/app.routes.ts:117`; además `cierre.ts:71`, `transparencia.ts:65`, `hero.ts:31` (citadas por el subagente D, no re-abiertas una por una)
- Evidencia: «Si alguien no aporta, el fondo cubre la entrega de quien tenía el turno.»; «Un fondo de garantía cubre esa falta para que el resto del grupo no se vea afectado».
- Cómo se comprobó: `grep -n "cubre"` sobre los dos primeros archivos.
- Impacto: promesa comercial contraria a R-NUEVA-2; riesgo de reclamo del consumidor si se mantiene tras el cambio.
- Causa de fondo: textos escritos para ADR-051.
- Solución de fondo: reescribir según la decisión A-2 (qué pasa con la entrega cuando falta plata).
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: marketing, L6.
- Estándares y normas: ISO/IEC 25010 Usabilidad (reconocibilidad adecuada).
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-UX-A11Y-002 — Comprobantes con título fijo que no refleja el estado devuelto
- Clase / Severidad / Confianza: UX-A11Y (también COHERENCIA) / **S3** / confirmado por lectura
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/aporte_registrado.dart:19-29`; `apps/movil/lib/pantallas/billetera/comprobante_de_retiro.dart:22-26`
- Evidencia: el aporte muestra siempre `TextosPasanaku.aporteConfirmado` con `Tono.ok` (el chip sí muestra el estado de la obligación); el retiro muestra siempre `retiroInstruido`, aunque el estado sea `EN_REVISION` o `RECHAZADA`.
- Cómo se comprobó: lectura.
- Impacto: la persona lee «confirmado»/«instruido» cuando la cuota sigue pendiente o el retiro espera revisión.
- Causa de fondo: el título no se deriva del estado del servidor.
- Solución de fondo: título y tono según el estado devuelto; mostrar el pendiente.
- Relación con el flujo nuevo: APROB (retiro en revisión)
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Usabilidad; regla 95.2.
- Esfuerzo estimado: chico

## 6. Matriz de contratos
_En curso._

## 7. Evidencia obligatoria del módulo
_En curso; ver Límites para lo NO COMPROBADO por entorno._

## 8. Cobertura por clase
_Se completa al cierre._

## 9. Lo que está bien
- `CU100RecibirWebhookPasarela` valida la firma HMAC sobre el cuerpo crudo antes de leerlo, aplica una ventana de ±5 min, deduplica por `(proveedor_id, clave)` con índice único, y nunca acredita «lo que vino»: compara monto y moneda contra el pago registrado (`:90-163`).
- `CU21CobrarAporte` bloquea la obligación, revalida la idempotencia tras el candado y usa versión optimista (`:65-131`); el evento sale por outbox en la misma transacción (`:134`).
- `Dinero` (plataforma) usa `BigDecimal` con escala fija, exige regla de redondeo explícita y compara por `compareTo` (`Dinero.java:36-44,72-115,129-131`).

## 10. Observado fuera de alcance
- `despliegue/TEST.md:458` — `nucleo-financiero` no arranca fuera de `local`/`test` porque no existe `ProveedorDeRetiro` productivo (se audita en este lote en la tanda siguiente).
- `servicios/cumplimiento/.../LimiteRepositorio.java:67,99` y `servicios/erp/.../PeriodoRepositorio.java:149-150`, `PresupuestoRepositorio.java:113-115,172` leen tablas del esquema `nucleo_financiero` (`auditar_backend.py`, «divergencias declaradas»): acople entrante hacia este módulo; se analiza en ACOPLE.

## 11. Límites de esta auditoría
- **Gradle bloqueado por entorno.** `./gradlew --no-daemon :servicios:aportes:test :servicios:aportes:webTest :servicios:nucleo-financiero:test :servicios:nucleo-financiero:webTest --continue` → `Could not resolve org.postgresql:postgresql:42.7.12 … Received status code 429 from server: Too Many Requests` · `BUILD FAILED in 38s`. Sin daemon de Docker (`failed to connect to the docker API at unix:///var/run/docker.sock`): `integrationTest` imposible.
- **Front bloqueado por entorno.** `yarn typecheck` → `This project's package.json defines "packageManager": "yarn@4.18.0". However the current global version of Yarn is 1.22.22.`; sin `node_modules`. Flutter no instalado.
- `scripts/idempotencia.py` no es un verificador sino una biblioteca del generador de DDL: no tiene salida que reportar.
- Supuesto: TEST corre con `PERFIL_SPRING=local` según `despliegue/TEST.md:458`; no hay acceso a Coolify para confirmarlo.

## 12. Anexo de cobertura de archivos
_En curso._
