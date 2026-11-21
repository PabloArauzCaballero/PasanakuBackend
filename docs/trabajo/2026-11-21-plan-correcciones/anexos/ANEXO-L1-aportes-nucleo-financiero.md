# Anexo L1 — Correcciones por hallazgo

- Informe de origen: `aportes-nucleo-financiero` (auditoría de `origin/test` @ `07fc6971`), 45 fichas.
- Decisiones aplicadas: `docs/trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md` (P-01…P-20 y tandas 2 a 5).
- Prefijo de los hallazgos: en todo el anexo `ANF-` abrevia `aportes-nucleo-financiero-` (por ejemplo, `ANF-FLUJO-QR-001` = `aportes-nucleo-financiero-FLUJO-QR-001`).
- Las microtareas de F0 que este anexo cita las definen otros lotes; acá se nombran por su contenido: **F0·DOCKER** (entorno de integración en la máquina de Pablo, P-19), **F0·ADR-051** / **F0·ADR-049** / **F0·ADR-054** (ADR que reemplazan a cada uno), **F0·PACTOS** (contratos y pactos base), **F0·PUERTOS** (puerto de pasarela QR y de proveedor de desembolso con su doble firmado en tres niveles, P-15 y regla 65).
- Estado de todas las microtareas: `TODO`.

## Resumen

**Microtareas: 44** (F0: 0 · F1: 13 · F2: 4 · F3: 20 · F4: 7 · F5: 0).

Hallazgos por fase (asignación principal) y severidad:

| Fase | S1 | S2 | S3 | S4 | Total |
|---|---:|---:|---:|---:|---:|
| F1 Seguridad e integridad del mayor | 2 | 6 | 5 | 0 | 13 |
| F2 Retiro de lo que contradice el flujo nuevo | 1 | 2 | 1 | 0 | 4 |
| F3 Flujo nuevo | 5 | 10 | 6 | 0 | 21 |
| F4 Calidad y deuda | 0 | 2 | 4 | 1 | 7 |
| F5 Piloto «Poder de compra» | 0 | 0 | 0 | 0 | 0 |
| **Total** | **8** | **20** | **16** | **1** | **45** |

Sobre los S1 que caen en F3: P-14 confirma que hoy no se mueve dinero real. Los cinco S1 de F3 (FLUJO-QR-001…004 y UX-A11Y-001) son el rediseño del cobro, que P-18 ubica en la fase 3. La parte de FLUJO-QR-001 que se explota hoy (el cliente elige canal, monto y referencia) se cierra antes, en **F1.L1.M11**.

**Hallazgos «se resuelve con»:**
- `ANF-SEG-004` (S3) → se resuelve con **F3.L1.M17** (la pantalla nueva lee monto y moneda de la orden de cobro en el servidor; desaparece el parámetro `monto` de la URL).

**Hallazgos «sin acción»:** ninguno. Ningún hallazgo de este informe queda convertido en correcto por las decisiones de Pablo; las decisiones definen *cómo* se corrige (P-04, P-05, P-06, P-07, P-09, P-12, P-13, P-15).

**Hallazgos con dos microtareas:** `ANF-FLUJO-QR-001` (corte de la confianza en el cuerpo en F1.L1.M11; corrección de raíz del cobro en F3.L1.M01).

**Ambigüedades que este anexo registra (no resueltas por conveniencia):**
- AMB-L1-1 — ¿El recargo por mora es una sanción que exige plazo de descargo *antes* de devengar (regla 91.4) o basta con el incumplimiento declarado por el sistema (P-12) y descargo posterior con contra-asiento? Supuesto: se devenga solo sobre incumplimiento declarado y queda sujeto a descargo; si el descargo prospera, contra-asiento. Confirmar con Pablo y Cumplimiento (F3.L1.M15).
- AMB-L1-2 — ¿Existen transferencias P2P hacia la billetera de un grupo en el flujo nuevo? Supuesto: no; al grupo solo entra dinero por el pago de una cuota (QR o saldo recargado por QR, P-06). Confirmar con Pablo (F3.L1.M20).
- AMB-L1-3 — ¿El bloqueo judicial de saldo (AP-11) sigue siendo una acción humana? Supuesto: sí, porque responde a una orden de autoridad externa, con dos personas distintas para bloquear y levantar. Confirmar con Cumplimiento (F1.L1.M10).

---

## F1 — Seguridad e integridad del mayor

### F1.L1.M01 — Importe nunca negativo, del contrato a la base
- Hallazgos: `ANF-DINERO-001` (S1)
- Causa de raíz: un solo tipo `Dinero` representa a la vez importes (siempre ≥ 0) y saldos con signo; el contrato acepta `^-?\d+\.\d{2}$`, el dominio solo valida escala y la base no tiene la invariante.
- Corrección de raíz: (1) en el OpenAPI de `aportes` y `nucleo-financiero`, separar el esquema `Importe` (`^\d+\.\d{2}$`, mayor que cero donde aplique) del esquema de saldo con signo, y usar `Importe` en todo cuerpo de entrada (`aportes.yaml:788-790` y equivalentes); (2) en `plataforma/comun-dominio`, un tipo `Importe` (o fábrica `Dinero.importe(...)`) que rechace negativos en el borde, usado por `MapeoDeAportes` y por `CU19ReembolsarPago`; (3) en `sql/40_reglas/restricciones.sql` (o la fuente del modelo según regla 97.1): `CHECK (monto > 0)` en `aportes.reembolso`, `CHECK (monto_pagado >= 0)` en `obligacion_aporte`, `CHECK (... >= 0)` en `pago.monto_neto_acreditado`, `pago.monto_comision_proveedor` y `disputa_pago.monto_disputado`. Resuelve la causa porque la invariante 91.2.2 queda en la base y en el tipo, no en un `if`.
- CA: Dado un pago acreditado de 100.00 BOB, cuando alguien solicita un reembolso de -50.00, entonces el contrato responde 400 con el error de validación del campo `monto`, no se inserta ninguna fila y un `INSERT` directo con `monto = -50.00` en `aportes.reembolso` falla por `CHECK`.
- DoD: prueba web de `PagosController` con monto negativo → 400; prueba unitaria del tipo `Importe` (negativo, cero, límite `0.01`); prueba de integración contra PostgreSQL real (F0·DOCKER) que intenta los `INSERT`/`UPDATE` negativos en cada tabla y espera violación de `CHECK`; `scripts/verificar_contratos_limites.py` en verde.
- Depende de: F0·DOCKER.
- Esfuerzo: chico

### F1.L1.M02 — Cuadre del asiento y signo de cada línea garantizados en la base
- Hallazgos: `ANF-DINERO-004` (S2)
- Causa de raíz: la invariante de cuadre se ata al encabezado (`tg_asiento_cuadrado ON asiento_contable`) y no a las líneas; un asiento sin líneas cuadra como 0 = 0 y `movimiento_contable` no tiene `CHECK` de signo.
- Corrección de raíz: en `sql/40_reglas/restricciones.sql`, agregar un constraint trigger diferido `AFTER INSERT ON movimiento_contable` que reevalúe el cuadre de su `asiento_id`; la función exige al menos dos líneas y `SUM(debe) = SUM(haber)`; en `movimiento_contable.sql`, `CHECK (debe >= 0 AND haber >= 0 AND ((debe = 0) <> (haber = 0)))`. Así el `INSERT` tardío sobre un asiento confirmado deja de escapar al control.
- CA: Dado un asiento `CONFIRMADO` cuadrado, cuando una transacción posterior inserta una línea más en `movimiento_contable`, entonces el commit falla; y una línea con `debe = -10` o con debe y haber a la vez se rechaza al insertarla.
- DoD: prueba de integración contra PostgreSQL real con cuatro casos (línea tardía, asiento sin líneas, línea negativa, línea con debe y haber) que esperan error; prueba de que los asientos que hoy proponen `ConsumoDeAportes` y `CU24RegistrarAsiento` siguen confirmándose.
- Depende de: F0·DOCKER.
- Esfuerzo: chico

### F1.L1.M03 — Reverso de billetera con cuatro ojos reales (solicitar y autorizar, cada uno desde su token)
- Hallazgos: `ANF-SEG-005` (S1)
- Causa de raíz: la segunda firma se modela como un dato de entrada (`autorizadaPor` del cuerpo, `MapeoDeBilletera.java:148-155`), no como una acción autenticada de otra persona.
- Corrección de raíz (P-13): partir `POST /billetera/reversos` en dos pasos: `POST /billetera/reversos` crea una solicitud (`reverso_transaccion` en estado `SOLICITADO`, solicitante = `ctx.usuarioId()`), y `POST /billetera/reversos/{id}/autorizacion` la ejecuta con el autorizador tomado del token, exigiendo que sea distinto del solicitante, con la precondición `estado = 'SOLICITADO'` en el `UPDATE`/transición (regla 96.3.2). Quitar `autorizadaPor` del contrato (`nucleo-financiero.yaml:1035-1040`) y del `EntradaReverso`. `CU14ReversarTransaccion` solo ejecuta desde el paso de autorización. El pacto `nucleo-financiero-garantia-transaccion-reversada.json` no cambia de forma.
- CA: Dada una solicitud de reverso creada por la persona A, cuando A intenta autorizarla, entonces recibe 403/422 y no se mueve saldo; cuando B (otra persona con `REVERSO_AUTORIZAR`) la autoriza, entonces se ejecuta una sola vez y la auditoría registra a A como solicitante y a B como autorizador, ambos desde sus tokens.
- DoD: pruebas web con matriz negativa (mismo usuario, sin permiso, sin token, solicitud ya ejecutada, autorizar dos veces → segundo intento sin efecto); prueba de integración contra base real del flujo completo; verificación del pacto con `garantia`; búsqueda en el contrato de que `autorizadaPor` ya no existe.
- Depende de: F0·DOCKER, F0·PACTOS.
- Esfuerzo: medio

### F1.L1.M04 — `nucleo-financiero` arranca con perfil productivo sin dobles de desarrollo
- Hallazgos: `ANF-HUECO-005` (S2)
- Causa de raíz: el único `ProveedorDeRetiro` es `@Profile({"local","test"})`; sin él el servicio no arranca, y por eso TEST corre con `PERFIL_SPRING=local`, que arrastra los dobles de desarrollo (origen de SEG-001). La prueba de arranque lo esconde con `@MockitoBean`.
- Corrección de raíz: implementar el adaptador del puerto de desembolso definido en F0·PUERTOS de forma que el servicio arranque fuera de `local`/`test`: mientras no haya proveedor contratado (P-15), un adaptador *fail-closed* explícito que rechaza toda instrucción con un código de error estable (`PROVEEDOR_DE_DESEMBOLSO_NO_CONFIGURADO`) y lo reporta en el health; el doble con firma real se activa solo por configuración explícita de entorno no productivo. `ArranqueProduccionTest` deja de mockear el puerto. `despliegue/TEST.md` pasa TEST a un perfil no-`local`.
- CA: Dado el servicio con perfil productivo y sin proveedor configurado, cuando arranca, entonces queda sano, y cualquier intento de instruir un retiro devuelve `PROVEEDOR_DE_DESEMBOLSO_NO_CONFIGURADO` sin mover saldo.
- DoD: `ArranqueProduccionTest` sin `@MockitoBean` del puerto, en verde; arranque real del contenedor con perfil productivo en F0·DOCKER (salida del health pegada); prueba del adaptador fail-closed.
- Depende de: F0·PUERTOS, F0·DOCKER.
- Esfuerzo: medio

### F1.L1.M05 — Secreto del webhook de aportes obligatorio por entorno, sin valor por omisión
- Hallazgos: `ANF-SEG-001` (S2)
- Causa de raíz: el secreto de prueba es un literal por omisión en `SecretoWebhookDeDesarrollo.java` confinado por perfil, y TEST corre con el perfil de desarrollo.
- Corrección de raíz: el secreto se lee de una variable obligatoria (`aportaya.aportes.webhook.secreto` sin `:default`) validada al arrancar (regla 90.3.4: falla si falta o es corta); `docker-compose.coolify.yml` la inyecta para `aportes`; `SecretoWebhookDeDesarrollo` se limita a las pruebas automáticas (`src/test`) y desaparece de `src/main`. Con F1.L1.M04, TEST deja de correr en `local`.
- CA: Dado `aportes` con perfil productivo, cuando arranca sin la variable del secreto, entonces el arranque falla con un mensaje que nombra la variable; y ningún archivo de `src/main` contiene un secreto literal.
- DoD: prueba de arranque sin la variable → falla; con la variable → arranca; `scripts/auditar_backend.py` sin el aviso «secreto con solo valor por omisión»; grep de `secreto-de-prueba` en `src/main` vacío.
- Depende de: F1.L1.M04.
- Esfuerzo: chico

### F1.L1.M06 — La firma del webhook cubre el timestamp
- Hallazgos: `ANF-SEG-003` (S3)
- Causa de raíz: esquema de firma propio que firma solo el cuerpo; la ventana de ±5 minutos se evalúa sobre una cabecera no firmada.
- Corrección de raíz: `VerificadorDeFirmaWebhook` firma `timestamp + "." + cuerpoCrudo` (el mismo esquema que fija el doble de F0·PUERTOS y que adoptará la pasarela contratada), comparación en tiempo constante, y la ventana se evalúa sobre el timestamp firmado. El mismo verificador se reutiliza para el webhook de recargas (F3.L1.M06) y de desembolsos.
- CA: Dado un webhook válido capturado, cuando se reenvía con un `X-Timestamp` nuevo, entonces la firma no verifica y se responde 401 sin registrar el evento.
- DoD: prueba unitaria en tres niveles (firma válida; límite: timestamp justo en el borde de la ventana y mismo `idEvento` dos veces; inválido: timestamp alterado, cuerpo alterado, firma ausente); prueba web de `CU100`.
- Depende de: F0·PUERTOS.
- Esfuerzo: chico

### F1.L1.M07 — Verificación de propiedad del recurso en los endpoints de `aportes` (BOLA)
- Hallazgos: `ANF-SEG-002` (S2)
- Causa de raíz: autorización solo por rol; ningún endpoint salvo el cobro compara el participante con el usuario de la sesión, y las tablas de `aportes` no tienen RLS por titular.
- Corrección de raíz: un componente único de pertenencia (participante → usuario, resuelto contra `grupos` por el mismo camino que ya usa el cobro en `AportesController.java:133-141`) aplicado en `consultarEstadoDelParticipante`, `contarMorosos`, `consultarPrimeraCuota`, `solicitarReembolso` (verifica que el pago sea del solicitante); `consultarRecaudoDelPeriodo` queda restringido a identidad de servicio (`entregas`), no a usuarios. Respuesta 404 uniforme ante recurso ajeno para no confirmar existencia.
- CA: Dado el participante A, cuando consulta el estado o pide el reembolso de un pago del participante B cambiando el id, entonces recibe 404 y no se crea ninguna solicitud ni se emite `reembolso_solicitado`.
- DoD: matriz de autorización negativa en las pruebas web de `AportesController` y `PagosController` (otro usuario, otro grupo, rol insuficiente, sin token, token de servicio donde corresponde); prueba de integración contra base real del reembolso ajeno.
- Depende de: ninguna.
- Esfuerzo: medio

### F1.L1.M08 — Hechos de dinero de `aportes` inmutables y rol sin `UPDATE`/`DELETE`
- Hallazgos: `ANF-DATOS-001` (S2)
- Causa de raíz: los estados de `pago` y `webhook_pasarela` se implementan con `UPDATE` sobre el hecho, y ninguna tabla de `aportes` figura en `sql/35_append_only/`.
- Corrección de raíz: separar el hecho inmutable (`pago`, `webhook_pasarela`, `reembolso`) de su estado, llevado en tablas de transición append-only (`pago_transicion`, `webhook_transicion`, `reembolso_transicion`); revocar `UPDATE`/`DELETE` sobre los hechos y las transiciones en `sql/35_append_only/` para `svc_aportes`; `PagoRepositorio.java:107-116` y `WebhookRepositorio.java:64-75` insertan transiciones. El cambio va por el pipeline del modelo (regla 97.1). Esto prepara el modelo `orden_cobro`/`intento_pago` de F3.
- CA: Dado el rol `svc_aportes`, cuando ejecuta `UPDATE aportes.pago SET estado = ...` o `DELETE` sobre un hecho de dinero, entonces la base lo rechaza por permisos; y el estado vigente de un pago se obtiene de su última transición.
- DoD: prueba de integración contra PostgreSQL real con el rol de la aplicación que intenta `UPDATE`/`DELETE` y espera `permission denied`; verificación de deriva modelo ↔ base ↔ jOOQ (regla 97.2) en verde; pruebas de `CU21`, `CU100` y `CU19` adaptadas en verde.
- Depende de: F0·DOCKER.
- Esfuerzo: medio

### F1.L1.M09 — Idempotencia real en reembolso, su aprobación y el alta de proveedor
- Hallazgos: `ANF-RESILIENCIA-001` (S3)
- Causa de raíz: el contrato exige `Idempotency-Key` pero `PagosController` la recibe y la descarta; el caso de uso no la modela.
- Corrección de raíz: `clave_idempotencia` en `reembolso`, en la transición de aprobación y en `proveedor_pago`, con índice `UNIQUE` nombrado; `CU19ReembolsarPago` y `CU99EnrutarProveedor` reciben la clave y, ante violación de unicidad, devuelven la respuesta original (mismo `id`).
- CA: Dada una solicitud de reembolso con clave K, cuando se envía dos veces (en serie y en paralelo), entonces existe una sola solicitud y ambas respuestas llevan el mismo `id`.
- DoD: prueba de idempotencia contra base real con dos peticiones concurrentes (salida pegada, regla 91.6.2); ídem para la aprobación y el alta de proveedor.
- Depende de: F1.L1.M08 (las transiciones de reembolso), F0·DOCKER.
- Esfuerzo: chico

### F1.L1.M10 — Bloqueo judicial de saldo con permiso propio de escritura y segregación
- Hallazgos: `ANF-SEG-006` (S3)
- Causa de raíz: el permiso se eligió por afinidad temática (`DATOS_SENSIBLES_LEER`) y no por la acción que autoriza.
- Corrección de raíz: permisos nuevos `BLOQUEO_AUTORIDAD_REGISTRAR` y `BLOQUEO_AUTORIDAD_LEVANTAR` en el catálogo de permisos y en `BilleteraController.bloquearPorAutoridad` / levantamiento; quien levanta un bloqueo es una persona distinta de quien lo registró (ambos desde el token); el levantamiento libera la retención asociada en la misma transacción (`CU17BloquearPorAutoridad.java:143`). Ver AMB-L1-3.
- CA: Dada una persona con solo `DATOS_SENSIBLES_LEER`, cuando llama `POST /billetera/bloqueos`, entonces recibe 403; y cuando quien registró un bloqueo intenta levantarlo, recibe 403; cuando lo levanta otra persona con el permiso, el saldo retenido vuelve a estar disponible.
- DoD: matriz de autorización negativa en las pruebas web; prueba de integración contra base real de bloqueo → levantamiento → saldo disponible.
- Depende de: AMB-L1-3 (confirmación de Cumplimiento).
- Esfuerzo: chico

### F1.L1.M11 — El cobro deja de aceptar canal, monto, referencia, recargo y proveedor del cuerpo
- Hallazgos: `ANF-DEFECTO-001` (S2); `ANF-FLUJO-QR-001` (S1, parte de confianza en el cuerpo)
- Causa de raíz: `POST /aportes/obligaciones/{id}/pagos` toma del cliente `canal`, `monto`, `referenciaProveedor`, `proveedorId`, `esManual` y `recargo` (este último mapeado como comisión del proveedor), y el servidor los persiste como verdad.
- Corrección de raíz: el contrato de cobro de la persona queda sin esos campos: el monto es el saldo pendiente de la obligación leído por el servidor, el único canal que admite este endpoint es el pago con saldo de billetera (P-06) y lo fija el servidor; `referenciaProveedor` y la comisión del proveedor solo los escribe el procesamiento del webhook (F3.L1.M02); el recargo es su propia obligación `RECARGO_MORA` y no viaja en el cobro de la cuota. Se borran `esManual` y `registrado_por` como dato del cliente (P-07). Cambios en `aportes.yaml:596-608`, `AportesController.java:126-169`, `CU21CobrarAporte.EntradaCobro`. El cobro por QR entra por la orden de cobro de F3.L1.M01.
- CA: Dado un participante con una cuota pendiente de 100.00 BOB, cuando envía un cuerpo con `canal=QR_INTEROPERABLE`, `monto=1.00` o `recargo=50.00`, entonces el contrato lo rechaza por campo no declarado (400) y la cuota no cambia.
- DoD: prueba web con cada campo prohibido → 400 (lista blanca, regla 96.2.2); prueba de integración contra base real de que el pago con saldo registra el monto de la obligación; cliente Dart regenerado y la app compila.
- Depende de: ninguna (la parte QR queda en F3.L1.M01).
- Esfuerzo: medio

### F1.L1.M12 — Moneda en reembolsos y tablas de importe de `aportes`
- Hallazgos: `ANF-DINERO-002` (S3)
- Causa de raíz: `reembolso` (y otras tablas de importe del esquema) no tienen `moneda` al lado del importe; el repositorio fija `Moneda.BOB` (`PagoRepositorio.java:198`).
- Corrección de raíz: columna `moneda CHAR(3) NOT NULL` con `CHECK` contra el catálogo ISO-4217 del proyecto en `reembolso`, `disputa_pago`, `conciliacion`, `excepcion_conciliacion` y `extracto_bancario` (verificando cada una en el DDL; `plan_regularizacion` se elimina en F2 junto con el comprobante manual); la moneda del reembolso se copia del pago; los repositorios leen la moneda de la fila. Va por el pipeline del modelo.
- CA: Dado un pago en USD, cuando se solicita y aprueba su reembolso, entonces el reembolso se registra y se emite en USD, y un reembolso en moneda distinta de la del pago se rechaza.
- DoD: prueba de integración contra base real con un pago USD; verificación de deriva en verde; grep de `Moneda.BOB` en los repositorios de `aportes` sin coincidencias de mapeo fijo.
- Depende de: F1.L1.M08.
- Esfuerzo: chico

### F1.L1.M13 — El sello del cierre diario incluye los saldos, la moneda y el sello anterior
- Hallazgos: `ANF-DINERO-006` (S3)
- Causa de raíz: el material del hash (`cuentaId + "|" + fecha`, `CU51EjecutarCierreDiario.java:172-176`) omite el contenido que dice proteger.
- Corrección de raíz: material = `cuentaId | fecha | moneda | saldo_disponible | saldo_retenido | sello del día anterior` con formato canónico de decimales; agregar la verificación que recalcula la cadena y reporta la primera ruptura.
- CA: Dado un cierre sellado, cuando se altera el saldo de un día anterior, entonces la verificación de la cadena falla señalando esa cuenta y esa fecha.
- DoD: prueba unitaria (alterar saldo, moneda o sello previo → cambia el sello); prueba de integración contra base real de dos cierres encadenados y su verificación.
- Depende de: ninguna.
- Esfuerzo: chico

---

## F2 — Retiro de lo que contradice el flujo nuevo

### F2.L1.M01 — Retirar el consumidor de asientos de cobertura empresarial y respaldo
- Hallazgos: `ANF-RESPALDO-003` (S1)
- Causa de raíz: implementación de ADR-051 («la empresa aporta el faltante»), encendida por defecto en `docker-compose.coolify.yml:603-611`.
- Corrección de raíz (P-04): borrar `ConsumidorDeAsientosKafka` (rama `garantia.cobertura_empresarial_*` y `garantia.respaldo_*`), la parte de `ConsumoDeAsientos` que los asienta, las cuentas lógicas `CAJA_EMPRESA`, `RESERVA_RESPALDO`, `POZO_GRUPO` de `application.yml:88-112`, las variables `CONSUMO_DE_ASIENTOS_HABILITADO` y `CUENTA_LOGICA_*` del compose, sus pruebas y la documentación del flujo. Si el consumidor también recibe asientos de `entregas` que siguen vigentes, esos tópicos se mantienen con su pacto y el resto se borra. Coordinado con el retiro del productor en `garantia` (lote L2).
- CA: Dado el despliegue, cuando `garantia` (o un productor de prueba) publica `cobertura_empresarial_aplicada` o `respaldo_reservado`, entonces ningún asiento se registra en el mayor y el tópico no tiene consumidor en `nucleo-financiero`.
- DoD: grep de `cobertura_empresarial\|respaldo_` y `CAJA_EMPRESA\|RESERVA_RESPALDO` en `servicios/nucleo-financiero/src/main` y en el compose vacío; prueba de integración en F0·DOCKER que publica el evento y verifica que el mayor no cambia; `scripts/validar_flujos.py` en verde.
- Depende de: F0·ADR-051.
- Esfuerzo: medio

### F2.L1.M02 — Retirar del esquema y de las semillas las cuentas 9.x y los tipos de cobertura empresarial
- Hallazgos: `ANF-RESPALDO-005` (S3)
- Causa de raíz: el mapeo «sintético» de cuentas de la empresa se sembró en las semillas base para encender el consumidor de F2.L1.M01.
- Corrección de raíz (P-04): eliminar las filas `9.1.01`…`9.1.0x` (BOB y USD) de `sql/60_semillas/22-cuentas-de-sistema.sql`, los orígenes y tipos `COBERTURA_*` empresariales de `transaccion_billetera.sql:28,31` y `asiento_contable.sql:19`, y la semilla de desarrollo `sql/61_dev/09-mora-cobertura-y-cobranza.sql` en lo que siembra el fondo empresarial pagando al grupo. Lo que el modelo A necesite (cuenta del fondo del grupo) lo crea F3 con su propio código, nunca bajo 9.x. Si TEST tiene asientos ya registrados contra 9.x, el retiro va por contra-asiento documentado, no por `DELETE` (regla 91.1.3).
- CA: Dada una base reconstruida desde cero, cuando se aplican esquema y semillas, entonces no existe ninguna cuenta 9.x ni ningún tipo `COBERTURA_EMPRESARIAL`/`RESPALDO_*` en los catálogos.
- DoD: reconstrucción completa de la base en F0·DOCKER (salida pegada); consulta de verificación de cero filas; segunda corrida de semillas sin inserciones (regla 97.4.3); verificación de deriva en verde.
- Depende de: F2.L1.M01.
- Esfuerzo: chico

### F2.L1.M03 — El catálogo de canales de cobro solo admite los canales del flujo nuevo
- Hallazgos: `ANF-RESPALDO-001` (S2)
- Causa de raíz: el enum público y el `CHECK ck_pago_canal` replican el catálogo interno, con `FONDO_GARANTIA` (respaldo), `EFECTIVO_AL_ORGANIZADOR` (organizador, contrario a 91.5) y canales manuales (`TARJETA`, `TRANSFERENCIA_BANCARIA`, `COMPENSACION_INTERNA`).
- Corrección de raíz (P-04, P-07, P-11): catálogo codificado de canales con solo `QR_INTEROPERABLE` y `BILLETERA_MOVIL` (saldo recargado por QR, P-06); `CHECK ck_pago_canal` acorde; los demás canales salen del contrato, del DDL, de los textos de la app y de `packages/simulado`. Si F3 modela la cobertura del fondo del grupo como pago de la cuota, entra por una operación de sistema con su propio origen, nunca por el contrato público. (El contrato público de cobro ya no lleva `canal` desde F1.L1.M11; esta microtarea cierra el esquema y los catálogos.)
- CA: Dado el esquema reconstruido, cuando se inserta un `pago` con canal `FONDO_GARANTIA` o `EFECTIVO_AL_ORGANIZADOR`, entonces la base lo rechaza; y ningún cliente versionado ofrece esos canales.
- DoD: prueba de integración contra base real con los canales retirados → violación de `CHECK`; grep de `EFECTIVO_AL_ORGANIZADOR\|FONDO_GARANTIA` en `servicios/aportes`, `apps/` y `packages/simulado` vacío; verificación de deriva en verde.
- Depende de: F1.L1.M11, decisión de retiro del organizador (P-11).
- Esfuerzo: chico

### F2.L1.M04 — Las cuotas de cada periodo nacen solas al abrirse el periodo
- Hallazgos: `ANF-HUECO-001` (S2)
- Causa de raíz: no hay disparador de apertura de periodo; el único llamador de `calendario.generar` es un endpoint `ADMIN_PLATAFORMA` (AP-02, ELIMINABLE).
- Corrección de raíz: consumidor idempotente del evento de apertura de periodo de `grupos` (o, si `grupos` no lo publica, trabajo con ShedLock que detecta periodos abiertos sin cuotas) que llama a `calendario.generar` con identidad de sistema; la idempotencia la garantiza `uq_obligacion_periodo_cupo` con captura de la violación. Retirar el endpoint `POST /aportes/periodos/{id}/obligaciones` y su paso `generar-obligaciones` del catálogo `aporte-mora-cobertura.yaml:15-17`. Pacto nuevo `grupos-aportes-periodo-abierto.json`.
- CA: Dado un grupo iniciado, cuando se abre el periodo 2, entonces existe una cuota por cupo sin intervención humana, y si el evento llega dos veces sigue existiendo una sola cuota por cupo.
- DoD: prueba de integración en F0·DOCKER con el evento real por Kafka; prueba de duplicado (mismo mensaje dos veces → una cuota por cupo, regla 98.8.3); prueba de contrato del lado productor y consumidor; grep del endpoint retirado vacío.
- Depende de: F0·PACTOS, F0·ADR-054 (inicio automático y calendario).
- Esfuerzo: medio

---

## F3 — Flujo nuevo

### F3.L1.M01 — El cobro por QR crea una orden de cobro; nada se acredita por declaración
- Hallazgos: `ANF-FLUJO-QR-001` (S1)
- Causa de raíz: falta la cadena `orden_cobro → intento_pago → webhook` (las tablas existen sin código); `CU21CobrarAporte` acredita de forma síncrona lo que el cliente declara.
- Corrección de raíz: nuevo caso de uso «emitir orden de cobro» en `aportes`: crea `orden_cobro` `PENDIENTE` con el saldo pendiente de la obligación, moneda de la obligación, vencimiento y `clave_idempotencia`, pide el QR dinámico al puerto de pasarela (F0·PUERTOS) y devuelve el QR. El pago `ACREDITADO`, el incremento de `monto_pagado` y `aporte_cobrado` dejan de existir en este camino (solo ocurren en F3.L1.M02). Se elimina la rama de acreditación síncrona de `CU21CobrarAporte` para canales distintos de saldo.
- CA: Dada una cuota pendiente, cuando la persona pide pagar por QR, entonces recibe un QR con el monto de la obligación y la cuota sigue `PENDIENTE` hasta que llega el webhook verificado.
- DoD: prueba de integración contra base real con el doble de pasarela en tres niveles (correcto: QR emitido; límite: segunda emisión con la misma clave devuelve la misma orden; inválido: pasarela responde 500 o excede el timeout → error de contrato y ninguna orden huérfana `PENDIENTE` sin QR); prueba web de autorización negativa (cuota ajena).
- Depende de: F0·PUERTOS, F1.L1.M08, F1.L1.M11.
- Esfuerzo: grande

### F3.L1.M02 — El webhook verificado es el único disparador del cobro
- Hallazgos: `ANF-FLUJO-QR-002` (S1)
- Causa de raíz: `CU100RecibirWebhookPasarela` se construyó como control transversal sobre un pago ya acreditado y solo archiva; no distingue tipo de evento.
- Corrección de raíz: en la misma transacción, el webhook con firma verificada (F1.L1.M06) hace `orden_cobro PENDIENTE → PAGADA`, registra el `pago` con la referencia y la comisión que informa la pasarela, sube `monto_pagado` y emite `aporte_cobrado` por outbox; los eventos de pago fallido, reversado o contracargo generan la transición inversa y un evento que dispara el contra-asiento. Monto o moneda distintos de la orden → excepción persistida y alerta, sin acreditar. Deduplicación por `idEvento` con índice único (ya existe) y por orden.
- CA: Dada una orden pendiente de 100.00 BOB, cuando llega un webhook firmado de pago confirmado por 100.00 BOB, entonces la cuota queda pagada y se emite un solo `aporte_cobrado`; cuando llega el mismo webhook otra vez, no cambia nada; cuando llega un webhook de reverso, la cuota vuelve a pendiente y se emite el evento de reverso.
- DoD: pruebas de integración en F0·DOCKER con el doble firmado en tres niveles (correcto; límite: duplicado y timestamp en el borde; inválido: firma mala, monto distinto, orden inexistente, orden ya pagada); prueba de duplicado del mensaje saliente (outbox) con consumidor idempotente; prueba del pacto `aportes-nucleo-financiero-aporte-cobrado.json`.
- Depende de: F3.L1.M01, F1.L1.M06.
- Esfuerzo: grande

### F3.L1.M03 — El cobro por QR asienta en el mayor y acredita la cuenta del grupo
- Hallazgos: `ANF-FLUJO-QR-004` (S1)
- Causa de raíz: la contrapartida contable del QR (custodia/pasarela → cuenta del grupo) no existe; `ConsumoDeAportes` devuelve `CANAL_SIN_BILLETERA` para todo canal distinto de billetera.
- Corrección de raíz: `ConsumoDeAportes` trata `QR_INTEROPERABLE` con el asiento `DEBE custodia/pasarela · HABER SALDO_GRUPOS` y la acreditación de la billetera del grupo, con clave de idempotencia derivada del evento de la pasarela; la comisión fija del servicio (decisión de ingreso) va en su propia línea con su cuenta. Se reemplaza la prueba `CUConsumoDeAportesRechazosTest.java:219` que fija `CANAL_SIN_BILLETERA` por la que exige el asiento.
- CA: Dado un `aporte_cobrado` por QR de 100.00 BOB, cuando `nucleo-financiero` lo consume, entonces existe un asiento cuadrado `custodia → SALDO_GRUPOS` y el saldo del grupo en el mayor sube 100.00; con el mismo evento dos veces, existe un solo asiento.
- DoD: prueba de integración contra base real con el asiento pegado (débitos, créditos y suma, regla 91.6.1); prueba de duplicado del mensaje; prueba de cuadre: saldo del grupo desde el mayor = suma de aportes cobrados (consulta pegada, 91.6.3); caso de reverso con su contra-asiento (91.6.4).
- Depende de: F3.L1.M02, F1.L1.M02.
- Esfuerzo: grande

### F3.L1.M04 — Lo «confirmado» para la entrega se lee del mayor
- Hallazgos: `ANF-FLUJO-QR-003` (S1)
- Causa de raíz: dos fuentes de verdad (obligación de `aportes` y mayor de `nucleo-financiero`) y la entrega decide con la que no es el dueño del dinero.
- Corrección de raíz: el importe con el que `entregas` decide una entrega se lee del saldo de la cuenta del grupo en el mayor de `nucleo-financiero` (endpoint de servicio con pacto, regla 98.4.2); `GET /aportes/periodos/{id}/recaudo` queda solo como detalle por cupo (quién pagó) y deja de exponer `confirmado`, `cubiertoMutual` y `faltante` como importes de decisión (RS-01). La cobertura por el fondo del grupo (modelo A) aparece como asiento propio en el mayor, no como `cubiertoMutual` calculado.
- CA: Dado un periodo con una cuota marcada pagada en `aportes` sin asiento en el mayor, cuando `entregas` consulta lo disponible para la entrega, entonces ese importe no se cuenta.
- DoD: prueba de contrato productor (`nucleo-financiero`) y consumidor (`entregas`); prueba de integración en F0·DOCKER del caso divergente; prueba de caída del dependido (nucleo apagado → la entrega no se ejecuta y queda reintentable, regla 98.8.4).
- Depende de: F3.L1.M03, F0·PACTOS, microtarea de entrega automática de L2.
- Esfuerzo: medio

### F3.L1.M05 — El pago con saldo de billetera se confirma en el mayor antes de marcar la cuota pagada
- Hallazgos: `ANF-HUECO-002` (S2)
- Causa de raíz: la cuota se marca pagada antes de que `nucleo-financiero` debite; la saga no tiene estado persistido y la rama `NO_COMPENSABLE` solo deja un `warn`.
- Corrección de raíz (P-06): el pago con saldo crea una orden de cobro con canal `BILLETERA_MOVIL`; `nucleo-financiero` debita la billetera, asienta y publica `aporte_aplicado`; `aportes` marca la cuota pagada solo al consumir `aporte_aplicado` (o `aporte_no_aplicado` cierra la orden como rechazada). Se elimina `CU21CompensarAporte` y la rama `NO_COMPENSABLE`. La orden tiene vencimiento; un trabajo marca vencidas las que no recibieron respuesta y alerta.
- CA: Dada una persona sin saldo suficiente, cuando paga su cuota con saldo, entonces la cuota nunca aparece pagada y la persona ve el rechazo; con saldo suficiente, la cuota queda pagada una sola vez aunque `aporte_aplicado` llegue dos veces.
- DoD: prueba de integración en F0·DOCKER por Kafka real de los dos caminos; prueba de duplicado; prueba de caída del dependido (nucleo apagado → la orden queda pendiente y vence con alerta, sin cuota pagada); grep de `CU21CompensarAporte` vacío.
- Depende de: F3.L1.M01, F0·PACTOS.
- Esfuerzo: medio

### F3.L1.M06 — La recarga de billetera se acredita por webhook firmado, sin Tesorería
- Hallazgos: `ANF-FLUJO-QR-005` (S2)
- Causa de raíz: no existe webhook de recargas; la acreditación la dispara una persona de Tesorería (`BilleteraController.java:142-149`, AP-10, ELIMINABLE).
- Corrección de raíz: webhook de recargas en `nucleo-financiero` con el mismo verificador (F1.L1.M06) que acredita la orden de recarga con identidad de sistema, idempotente por evento de la pasarela; se retira `POST /billetera/recargas/{id}/acreditacion` y el permiso `TESORERIA` de ese camino.
- CA: Dada una orden de recarga pendiente, cuando llega el webhook firmado de pago confirmado, entonces el saldo se acredita una sola vez sin intervención humana; y el endpoint de acreditación manual ya no existe.
- DoD: pruebas de integración con el doble firmado en tres niveles (correcto; límite: duplicado y borde de ventana; inválido: firma, monto, orden inexistente); grep del endpoint retirado vacío.
- Depende de: F1.L1.M06, F0·PUERTOS.
- Esfuerzo: medio

### F3.L1.M07 — Los límites de recarga se evalúan al pedir la orden, no al acreditar
- Hallazgos: `ANF-DEFECTO-004` (S2)
- Causa de raíz: el control preventivo de límites está en el paso de confirmación (`CU10RecargarSaldo.java:226-232`), cuando el dinero ya fue cobrado.
- Corrección de raíz: evaluar límites al crear la orden de recarga (con el importe pedido); al confirmar, el dinero cobrado siempre se acredita y, si un límite se excedió en el intervalo, se abre una devolución con su asiento y su orden al proveedor de desembolso, sin revertir la acreditación.
- CA: Dada una persona cerca de su límite, cuando pide una recarga que lo excede, entonces la orden se rechaza antes de emitir el QR; y si el webhook confirma una recarga que en ese momento excede el límite, entonces el saldo se acredita y se crea una devolución trazable.
- DoD: prueba de integración contra base real de ambos caminos con asientos pegados.
- Depende de: F3.L1.M06, F3.L1.M10 (despacho al proveedor para la devolución).
- Esfuerzo: chico

### F3.L1.M08 — Toda operación de billetera asienta en el mayor en la misma transacción
- Hallazgos: `ANF-DINERO-003` (S2)
- Causa de raíz: el mayor se agregó después del libro de billetera y solo se conectó a dos consumidores; `CU10`, `CU12` y `CU14` no asientan.
- Corrección de raíz: `CU10RecargarSaldo`, `CU12TransferirSaldo`, `CU14ReversarTransaccion` y los retiros llaman a `CU24RegistrarAsiento` en la misma transacción que el movimiento de billetera; el reverso usa el contra-asiento del asiento vinculado. Se agrega una verificación de cuadre entre libro de billetera y mayor por cuenta (se ejecuta en el cierre diario).
- CA: Dada cualquier recarga, transferencia, reverso o retiro, cuando se confirma, entonces existe su asiento cuadrado y el saldo de la cuenta en el mayor es igual al del libro de billetera.
- DoD: prueba de integración contra base real por cada operación con el asiento pegado; prueba de cuadre libro ↔ mayor (consulta pegada); README de `nucleo-financiero` sin la nota «2.1.01 queda negativo».
- Depende de: F1.L1.M02, F1.L1.M03.
- Esfuerzo: grande

### F3.L1.M09 — Un solo puerto y un solo despachador de retiros
- Hallazgos: `ANF-DUP-INT-001` (S3)
- Causa de raíz: dos carriles construyeron la misma pieza (`ProveedorDeRetiro` y `ProveedorDeRetiros`; `CU11RetirarSaldo.instruirPago` y `CU11InstruirRetiro` + `RetirosConProveedor`) con reglas que divergen.
- Corrección de raíz: conservar un único puerto (el de F0·PUERTOS) y un único despachador que revalida titularidad, cuenta verificada y enfriamiento antes de instruir; borrar el otro camino y sus pruebas duplicadas.
- CA: Dado el código de `nucleo-financiero`, cuando se buscan implementaciones del puerto de desembolso, entonces existe un solo puerto y un solo despachador, y el despachador rechaza una cuenta no verificada.
- DoD: grep de `ProveedorDeRetiros\b` y del camino borrado vacío; prueba unitaria del despachador con los rechazos.
- Depende de: F0·PUERTOS, F1.L1.M04.
- Esfuerzo: medio

### F3.L1.M10 — Los retiros se instruyen solos, con controles, y se confirman por webhook
- Hallazgos: `ANF-HUECO-004` (S2)
- Causa de raíz: falta el disparador del despacho (decisión A9 abierta en su carril) y la doble aprobación humana de ADR-049.
- Corrección de raíz (P-09): al solicitar un retiro con segundo factor válido, cuenta verificada, enfriamiento cumplido y sin alerta UIF, la orden pasa a autorizada por el sistema y el despachador (F3.L1.M09) la instruye por outbox; el proveedor confirma por webhook firmado y recién ahí se asienta la salida; si dispara una alerta, queda en revisión de Cumplimiento. Se retira la doble aprobación (`ResolucionDeRetiro`, `ck_retiro_doble_aprobacion`, `application.yml:61`, backoffice `operacion/retiros/*`) según F0·ADR-049.
- CA: Dada una persona con cuenta verificada y sin alertas, cuando pide un retiro con segundo factor válido, entonces se instruye al proveedor sin intervención humana y su saldo baja solo cuando llega la confirmación firmada; un cambio de cuenta reciente o una alerta UIF lo detienen.
- DoD: pruebas de integración en F0·DOCKER con el doble de desembolso en tres niveles (correcto; límite: webhook duplicado, enfriamiento justo en el borde; inválido: firma mala, proveedor caído → orden reintentable sin doble salida); prueba de idempotencia del despacho.
- Depende de: F0·ADR-049, F3.L1.M09, F3.L1.M11, controles de L4 (UIF, segundo factor).
- Esfuerzo: grande

### F3.L1.M11 — Sin conciliación de custodia vigente, el encaje es desconocido y los retiros se detienen
- Hallazgos: `ANF-DEFECTO-005` (S3)
- Causa de raíz: la ausencia de dato se trata como cumplimiento (`OrdenRetiroRepositorio.java:264-277` devuelve verdadero con cero filas).
- Corrección de raíz: el control devuelve tres estados (cumple, no cumple, desconocido) por moneda; «desconocido» (sin conciliación dentro de la antigüedad máxima configurada, sin valor por omisión) frena los retiros automáticos y alerta a Tesorería.
- CA: Dada una moneda sin ninguna conciliación de custodia vigente, cuando se pide un retiro en esa moneda, entonces el retiro no se instruye y queda registrado el motivo «encaje desconocido».
- DoD: prueba de integración contra base real con cero conciliaciones, conciliación vieja, conciliación que no cumple y conciliación que cumple.
- Depende de: ninguna.
- Esfuerzo: chico

### F3.L1.M12 — La conciliación tripartita corre todos los días como control posterior del QR
- Hallazgos: `ANF-REESCRITURA-001` (S3)
- Causa de raíz: `ConciliarLoteTripartito` se construyó sin disparador; además corta los días en UTC.
- Corrección de raíz: trabajo diario con ShedLock que concilia libro (`pago`/orden), proveedor (webhooks recibidos) y banco (extracto), con el día cortado en `America/La_Paz`, y que abre excepciones de conciliación con alerta; se integra al cierre diario de `nucleo-financiero`.
- CA: Dado un día con un webhook confirmado sin línea de extracto, cuando corre la conciliación, entonces se abre una excepción para esa orden y los pagos conciliados pasan a conciliados.
- DoD: prueba de integración contra base real con un caso conciliado, uno faltante en banco y uno en el borde de medianoche de La Paz; prueba de que una segunda corrida no duplica excepciones.
- Depende de: F3.L1.M02.
- Esfuerzo: medio

### F3.L1.M13 — El sistema pasa la cuota a mora al vencer el plazo de gracia
- Hallazgos: `ANF-DEFECTO-003` (S2)
- Causa de raíz: el estado de la obligación es un campo escrito solo en eventos de pago, no una función de la fecha.
- Corrección de raíz (P-12): trabajo diario idempotente que pasa `PENDIENTE → EN_MORA` cuando vence el plazo de gracia de la política (parámetro obligatorio sin valor por omisión, fecha en `America/La_Paz`), con la precondición en el `UPDATE`/transición, y emite el evento de incumplimiento declarado por el sistema que consume `garantia` (cobertura del fondo del grupo, modelo A) y el expediente de descargo.
- CA: Dada una cuota no pagada cuyo plazo de gracia venció ayer, cuando corre el trabajo, entonces la cuota está `EN_MORA`, el estado del participante muestra la deuda y se emite un solo evento de incumplimiento aunque el trabajo corra dos veces.
- DoD: prueba de integración contra base real con reloj fijo (día anterior, día de vencimiento, día siguiente); prueba de duplicado; pacto del evento con `garantia`.
- Depende de: F3.L1.M15 (parámetros), microtareas de L2 para el fondo del grupo y el expediente.
- Esfuerzo: medio

### F3.L1.M14 — El recargo por mora va al perjudicado
- Hallazgos: `ANF-RESPALDO-004` (S2)
- Causa de raíz: ADR-051 fijó como destino del recargo la cuenta de reserva de respaldo de la empresa (`DestinoDelCobro.java:17-19,43`).
- Corrección de raíz (P-05): `DestinoDelCobro` acredita el recargo a quien tuvo que esperar por la mora (el beneficiario del turno afectado, o en partes si son varios, con redondeo declarado y residuo asignado), con asiento `SALDO_USUARIOS (moroso) → SALDO_USUARIOS (perjudicado)`; se borra el destino `RESERVA_RESPALDO`; se actualiza `aporte-mora-cobertura.yaml:81-87` y `CUConsumoDeRecargoTest`. El reparto de parte de los recargos a los puntuales (pieza D) se agrega cuando su regla esté definida.
- CA: Dado un recargo de 10.00 BOB pagado por una persona en mora que retrasó el turno de otra, cuando se aplica, entonces la persona perjudicada recibe 10.00 BOB y ninguna cuenta de la empresa cambia.
- DoD: prueba de integración contra base real con asiento pegado, caso de varios perjudicados con residuo, prueba de idempotencia y caso de reverso.
- Depende de: F2.L1.M01, F3.L1.M13.
- Esfuerzo: chico

### F3.L1.M15 — Política de recargo con parámetros obligatorios y devengo solo sobre incumplimiento declarado
- Hallazgos: `ANF-DEBIDO-PROCESO-001` (S2)
- Causa de raíz: la política está modelada en la base (`aplica_automatico`, `vigente_desde`, `dias_gracia`) y el código la ignora: devenga siempre y con gracia 0 (`CU21CobrarAporte.java:218`).
- Corrección de raíz: los parámetros financieros (tasa, días de gracia, tope) se leen de la configuración obligatoria sin valor por omisión (el servicio no arranca sin ellos, decisión de parámetros) y de la política vigente por fecha; el recargo se devenga solo sobre una cuota en incumplimiento declarado (F3.L1.M13) y respeta `aplica_automatico`; si el descargo resuelto por Soporte/Riesgos prospera (P-12), se genera el contra-asiento. Ver AMB-L1-1.
- CA: Dado un grupo cuya política tiene `aplica_automatico = false`, cuando una cuota entra en mora, entonces no se devenga recargo; y con el servicio sin la tasa configurada, el arranque falla.
- DoD: prueba de arranque sin parámetros → falla; prueba de integración contra base real con política automática, no automática, fuera de vigencia y descargo aceptado (contra-asiento pegado).
- Depende de: F3.L1.M13, AMB-L1-1.
- Esfuerzo: medio

### F3.L1.M16 — Tipo de recargo como catálogo codificado y falla aislada por obligación
- Hallazgos: `ANF-DEFECTO-002` (S2)
- Causa de raíz: catálogo cerrado modelado dos veces (enum Java `FIJO, PORCENTUAL, PORCENTUAL_DIARIO` y `CHECK` `DIARIO_COMPUESTO, FIJO, PORCENTUAL`), y el trabajo diario corre todo en una transacción y se traga el fallo.
- Corrección de raíz: un único catálogo codificado de tipos de recargo (regla 97.4.7) del que leen la base y el código; el trabajo procesa cada obligación en su propia transacción, registra la falla persistida y alerta, sin revertir las demás.
- CA: Dada una política con un tipo de recargo desconocido, cuando corre el trabajo diario, entonces las demás obligaciones reciben su recargo y queda una falla registrada y alertada para esa política.
- DoD: prueba de integración contra base real con una política inválida mezclada con válidas; verificación de que no existe enum Java paralelo al catálogo.
- Depende de: F3.L1.M15.
- Esfuerzo: chico

### F3.L1.M17 — Pantalla de aporte por QR que espera la confirmación del servidor
- Hallazgos: `ANF-UX-A11Y-001` (S1); resuelve también `ANF-SEG-004` (S3)
- Causa de raíz: CU-21 se diseñó como «aplicar pago informado» y la pantalla replica esa autodeclaración (canales elegibles y campo de referencia de comprobante); el monto viaja por la URL.
- Corrección de raíz (P-07): reescribir `apps/movil/lib/pantallas/pasanaku/pantalla_aportar.dart`: recibe solo el id de la obligación, pide la orden de cobro (F3.L1.M01), muestra el QR dinámico con monto y moneda del servidor y la opción de pagar con saldo (P-06), y espera la confirmación (consulta del estado de la orden) con los estados cargando, pendiente, confirmado, rechazado y error. Sin campo de referencia, sin canales manuales, sin `monto` en la ruta (`rutas_operacion.dart:98-99`). Textos nuevos redactados por Claude y aprobados por Pablo (P-16).
- CA: Dada una cuota pendiente, cuando la persona abre «Aportar», entonces ve un QR con el monto que informa el servidor, no puede escribir ninguna referencia, y la pantalla muestra «confirmado» solo después de que el servidor confirme el pago.
- DoD: prueba de widget por estado; prueba de integración Flutter contra el gateway en F0·DOCKER con el doble de pasarela; capturas en móvil estrecho, tablet y escritorio, tema claro y oscuro (regla 95.7), revisadas; grep de `queryParameters['monto']` vacío.
- Depende de: F3.L1.M01, F3.L1.M02, F3.L1.M05.
- Esfuerzo: medio

### F3.L1.M18 — «Aportar» enlazado desde «Mi estado», la billetera y el aviso
- Hallazgos: `ANF-HUECO-003` (S2)
- Causa de raíz: la pantalla de aporte se construyó y nunca se enlazó; la acción «Aportar» lleva a `pasanaku.miEstado` y el enlace profundo apunta a otra ruta.
- Corrección de raíz: enlazar la ruta de F3.L1.M17 desde la tarjeta de cuota en «Mi estado», desde `acciones_de_saldo.dart:22-26` y desde el enlace profundo `aporte` (`enlaces_profundos.dart:16`), pasando solo el id de la obligación.
- CA: Dada una persona con una cuota pendiente, cuando toca «Aportar» en la billetera, en «Mi estado» o en el aviso, entonces llega a la pantalla de aporte de esa cuota.
- DoD: prueba de navegación Flutter de los tres caminos; captura de cada camino revisada.
- Depende de: F3.L1.M17.
- Esfuerzo: chico

### F3.L1.M19 — La web pública deja de prometer que la empresa cubre la entrega
- Hallazgos: `ANF-RESPALDO-002` (S3)
- Causa de raíz: textos escritos para ADR-051.
- Corrección de raíz (P-01/P-02, P-16): reescribir los textos de `apps/web/src/app/paginas/inicio/secciones/producto.ts:75`, `app.routes.ts:117`, `cierre.ts:71`, `transparencia.ts:65` y `hero.ts:31` según el modelo A «Tu garantía vuelve» (fondo del grupo en cuenta segregada, nunca de la empresa; devolución al cierre) y la pieza D; los textos los redacta Claude y los aprueba Pablo antes de subirlos.
- CA: Dada la web pública, cuando se lee cualquier sección, entonces ningún texto dice que la empresa o «el fondo de garantía» de la empresa cubre la entrega, y el texto aprobado por Pablo figura tal cual.
- DoD: grep de «cubre la entrega» y «fondo de garantía cubre» en `apps/web/src` vacío; constancia de aprobación de Pablo enlazada en el PR; capturas en tres viewports revisadas.
- Depende de: F0·ADR-051, aprobación de textos (P-16).
- Esfuerzo: chico

### F3.L1.M20 — Al grupo solo entra dinero por el pago de una cuota
- Hallazgos: `ANF-HUECO-006` (S3)
- Causa de raíz: integración a medio construir: `CU12TransferirSaldo` permite transferir a la billetera de un grupo (`ResolverDestino`) y publica `obligacion_pagada`, que nadie consume.
- Corrección de raíz: según el supuesto de AMB-L1-2, `ResolverDestino` rechaza destinos de tipo grupo y se elimina la publicación de `obligacion_pagada`; el único camino de dinero hacia el grupo es la orden de cobro (F3.L1.M01/M05).
- CA: Dada una persona con saldo, cuando intenta transferir a la billetera de un grupo, entonces recibe un error de contrato y ningún saldo se mueve.
- DoD: prueba de integración contra base real de la transferencia rechazada; grep de `obligacion_pagada` en `servicios` y `plataforma` vacío.
- Depende de: AMB-L1-2 (confirmación de Pablo).
- Esfuerzo: chico

---

## F4 — Calidad y deuda

### F4.L1.M01 — Límite de `referenciaProveedor` derivado del esquema
- Hallazgos: `ANF-CONTRATO-001` (S3)
- Causa de raíz: contrato (100) y esquema (80) mantenidos a mano por separado.
- Corrección de raíz: tras F1.L1.M11 la referencia la escribe solo el webhook: fijar el límite en la fuente del esquema y que el contrato del webhook y las validaciones del caso de uso lo tomen de ahí; agregar el par `pago.referencia_proveedor` a `scripts/verificar_contratos_limites.py`. Una referencia más larga que el límite produce el error de contrato (RFC 9457), nunca un 500.
- CA: Dado un webhook con una referencia de 81 caracteres, cuando se procesa, entonces se rechaza con un error de contrato registrado y no hay error de base.
- DoD: `scripts/verificar_contratos_limites.py` en verde con el nuevo par; prueba de integración del caso límite (80 y 81).
- Depende de: F1.L1.M11, F3.L1.M02.
- Esfuerzo: chico

### F4.L1.M02 — Sin importes atribuibles a personas en los logs
- Hallazgos: `ANF-OBSERV-001` (S3)
- Causa de raíz: log de depuración convertido en log de negocio (`CU21CobrarAporte.java:170`).
- Corrección de raíz: quitar `monto` (y cualquier importe) de los logs de `aportes` y `nucleo-financiero`, dejando identificadores y `correlationId`; agregar una regla de análisis estático (o prueba de arquitectura) que falle ante parámetros de tipo `Dinero`/`Importe` en llamadas de log.
- CA: Dado un cobro registrado, cuando se revisa el log, entonces aparecen el id del pago, de la obligación y el `correlationId`, y ningún importe.
- DoD: prueba de arquitectura en verde en ambos servicios; grep de `monto={}` en logs de `src/main` vacío.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L1.M03 — Extracto y certificado de saldo en la moneda de la cuenta
- Hallazgos: `ANF-DINERO-005` (S3)
- Causa de raíz: moneda fija `Moneda.BOB` en el mapeo (`ExtractoRepositorio.java:31-32,64`).
- Corrección de raíz: leer la moneda de la cuenta en cada mapeo del extracto y del certificado; prueba de arquitectura que prohíba `Moneda.BOB` literal en repositorios de lectura.
- CA: Dada una cuenta en USD, cuando se emite su extracto o su certificado de saldo, entonces todos los importes figuran en USD.
- DoD: prueba de integración contra base real con una cuenta USD.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L1.M04 — Sin claves foráneas entre esquemas ni lecturas ajenas del mayor
- Hallazgos: `ANF-ACOPLE-001` (S2)
- Causa de raíz: el esquema se modeló como base única antes de partir en servicios; `erp` y `cumplimiento` leen tablas del mayor.
- Corrección de raíz: quitar de `sql/20_claves/03_aportes_pagos_qr.sql:15-36` las FK hacia `grupos`, `identidad` y `erp` (la existencia se valida por evento o API al escribir); exponer a `erp` y `cumplimiento`, por endpoint de servicio con pacto, lo que hoy leen por SQL (`LimiteRepositorio.java:67,99`, `PeriodoRepositorio.java:149-150`, `PresupuestoRepositorio.java:113-115,172`); quitar la «divergencia declarada» del invariante 11 de `scripts/auditar_backend.py`.
- CA: Dada la base, cuando se consultan las FK que salen del esquema del mayor hacia otro esquema, entonces no hay ninguna; y `erp`/`cumplimiento` obtienen esos datos solo por API.
- DoD: consulta a `information_schema` pegada con cero FK entre esquemas; `scripts/auditar_backend.py` sin divergencias en el invariante 11; pruebas de contrato productor/consumidor de los endpoints nuevos.
- Depende de: F0·PACTOS.
- Esfuerzo: medio

### F4.L1.M05 — Pruebas que fijan el requisito nuevo y se ejecutan con evidencia
- Hallazgos: `ANF-PRUEBAS-001` (S2)
- Causa de raíz: las pruebas describen la implementación vigente y nunca se ejecutaron en CI (0/393 clases con evidencia).
- Corrección de raíz: cada microtarea de este anexo reescribe sus propias pruebas; esta microtarea cubre lo que no pertenece a ninguna: renombrar o rehacer `CU21Test:189` (concurrencia real con dos hilos contra base real) y `CU21Test:207` (cuadre contra el mayor), y dejar la suite de `aportes` y `nucleo-financiero` ejecutándose en F0·DOCKER con reporte guardado, para que `scripts/auditar_backend.py` deje de informar 0 clases corridas.
- CA: Dada la suite de ambos servicios, cuando se ejecuta en el entorno de integración, entonces todas las clases corren, el reporte queda guardado y la prueba llamada «concurrencia» ejecuta peticiones simultáneas.
- DoD: salida de `./gradlew :servicios:aportes:test :servicios:nucleo-financiero:test` (un build a la vez) pegada; `scripts/auditar_backend.py` con el conteo real de clases corridas.
- Depende de: F0·DOCKER y el resto de microtareas de F1 a F3 de este anexo.
- Esfuerzo: medio

### F4.L1.M06 — Tamaño del pool con una sola fuente
- Hallazgos: `ANF-CONFIG-001` (S4)
- Causa de raíz: dos fuentes para el mismo número (`descriptor.yml` `hikari_por_replica: 10` y `application.yml` `maximum-pool-size: 20`).
- Corrección de raíz: `application.yml` toma `maximum-pool-size` de una variable que el despliegue genera desde `descriptor.yml`, y un verificador compara ambos en CI; aplicar en `aportes` y `nucleo-financiero`.
- CA: Dado cualquiera de los dos servicios, cuando se compara el descriptor con la configuración efectiva, entonces el tamaño del pool coincide.
- DoD: verificador en verde para los dos servicios; prueba de que falla si se cambia solo uno.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L1.M07 — Comprobantes de aporte y retiro con título y tono según el estado
- Hallazgos: `ANF-UX-A11Y-002` (S3)
- Causa de raíz: el título no se deriva del estado que devuelve el servidor (`aporte_registrado.dart:19-29`, `comprobante_de_retiro.dart:22-26`).
- Corrección de raíz: título, tono y texto se calculan del estado devuelto (pendiente, confirmado, rechazado para el aporte; solicitado, instruido, pagado, en revisión por alerta, rechazado para el retiro), con estado de error accionable; textos aprobados por Pablo (P-16).
- CA: Dado un retiro en revisión por alerta, cuando la persona ve su comprobante, entonces el título dice que está en revisión y no «instruido»; dado un aporte pendiente, el título no dice «confirmado».
- DoD: pruebas de widget por estado; capturas por estado en tres viewports y dos temas, revisadas.
- Depende de: F3.L1.M17, F3.L1.M10.
- Esfuerzo: chico

---

## F5 — Piloto «Poder de compra»

Ninguna microtarea: ningún hallazgo de este informe toca algo reutilizable por el piloto.

---

## Riesgos del orden propuesto
| Riesgo | Impacto | Mitigación |
|---|---|---|
| F1.L1.M11 rechaza los canales QR, transferencia y tarjeta antes de que exista la pantalla de F3.L1.M17 | La app actual recibe 400 si alguien elige esos canales | P-14: no hay dinero real; la pantalla ni siquiera está enlazada (HUECO-003); se declara en el PR |
| F2.L1.M01 se mergea antes de que L2 retire el productor en `garantia` | Eventos de respaldo sin consumidor en el tópico | Coordinar ambos PR en el mismo ciclo de revisión de Pablo |
| Asientos ya registrados contra cuentas 9.x en TEST | No se pueden borrar (append-only) | Contra-asiento documentado en F2.L1.M02 |

## Trazabilidad «Hallazgo → Microtarea»

| # | Hallazgo | Sev. | Microtarea |
|---:|---|---|---|
| 1 | aportes-nucleo-financiero-FLUJO-QR-001 | S1 | F3.L1.M01 (raíz) · F1.L1.M11 (confianza en el cuerpo) |
| 2 | aportes-nucleo-financiero-FLUJO-QR-002 | S1 | F3.L1.M02 |
| 3 | aportes-nucleo-financiero-FLUJO-QR-003 | S1 | F3.L1.M04 |
| 4 | aportes-nucleo-financiero-FLUJO-QR-004 | S1 | F3.L1.M03 |
| 5 | aportes-nucleo-financiero-SEG-001 | S2 | F1.L1.M05 |
| 6 | aportes-nucleo-financiero-RESPALDO-001 | S2 | F2.L1.M03 |
| 7 | aportes-nucleo-financiero-CONTRATO-001 | S3 | F4.L1.M01 |
| 8 | aportes-nucleo-financiero-DEFECTO-001 | S2 | F1.L1.M11 |
| 9 | aportes-nucleo-financiero-OBSERV-001 | S3 | F4.L1.M02 |
| 10 | aportes-nucleo-financiero-DINERO-001 | S1 | F1.L1.M01 |
| 11 | aportes-nucleo-financiero-SEG-002 | S2 | F1.L1.M07 |
| 12 | aportes-nucleo-financiero-DEFECTO-002 | S2 | F3.L1.M16 |
| 13 | aportes-nucleo-financiero-HUECO-001 | S2 | F2.L1.M04 |
| 14 | aportes-nucleo-financiero-HUECO-002 | S2 | F3.L1.M05 |
| 15 | aportes-nucleo-financiero-DATOS-001 | S2 | F1.L1.M08 |
| 16 | aportes-nucleo-financiero-SEG-003 | S3 | F1.L1.M06 |
| 17 | aportes-nucleo-financiero-DINERO-002 | S3 | F1.L1.M12 |
| 18 | aportes-nucleo-financiero-UX-A11Y-001 | S1 | F3.L1.M17 |
| 19 | aportes-nucleo-financiero-HUECO-003 | S2 | F3.L1.M18 |
| 20 | aportes-nucleo-financiero-SEG-004 | S3 | se resuelve con F3.L1.M17 |
| 21 | aportes-nucleo-financiero-RESPALDO-002 | S3 | F3.L1.M19 |
| 22 | aportes-nucleo-financiero-UX-A11Y-002 | S3 | F4.L1.M07 |
| 23 | aportes-nucleo-financiero-SEG-005 | S1 | F1.L1.M03 |
| 24 | aportes-nucleo-financiero-RESPALDO-003 | S1 | F2.L1.M01 |
| 25 | aportes-nucleo-financiero-DINERO-003 | S2 | F3.L1.M08 |
| 26 | aportes-nucleo-financiero-DINERO-004 | S2 | F1.L1.M02 |
| 27 | aportes-nucleo-financiero-HUECO-004 | S2 | F3.L1.M10 |
| 28 | aportes-nucleo-financiero-HUECO-005 | S2 | F1.L1.M04 |
| 29 | aportes-nucleo-financiero-ACOPLE-001 | S2 | F4.L1.M04 |
| 30 | aportes-nucleo-financiero-RESPALDO-004 | S2 | F3.L1.M14 |
| 31 | aportes-nucleo-financiero-RESPALDO-005 | S3 | F2.L1.M02 |
| 32 | aportes-nucleo-financiero-FLUJO-QR-005 | S2 | F3.L1.M06 |
| 33 | aportes-nucleo-financiero-DUP-INT-001 | S3 | F3.L1.M09 |
| 34 | aportes-nucleo-financiero-DINERO-005 | S3 | F4.L1.M03 |
| 35 | aportes-nucleo-financiero-DINERO-006 | S3 | F1.L1.M13 |
| 36 | aportes-nucleo-financiero-HUECO-006 | S3 | F3.L1.M20 |
| 37 | aportes-nucleo-financiero-PRUEBAS-001 | S2 | F4.L1.M05 |
| 38 | aportes-nucleo-financiero-CONFIG-001 | S4 | F4.L1.M06 |
| 39 | aportes-nucleo-financiero-DEFECTO-003 | S2 | F3.L1.M13 |
| 40 | aportes-nucleo-financiero-DEBIDO-PROCESO-001 | S2 | F3.L1.M15 |
| 41 | aportes-nucleo-financiero-DEFECTO-004 | S2 | F3.L1.M07 |
| 42 | aportes-nucleo-financiero-RESILIENCIA-001 | S3 | F1.L1.M09 |
| 43 | aportes-nucleo-financiero-REESCRITURA-001 | S3 | F3.L1.M12 |
| 44 | aportes-nucleo-financiero-DEFECTO-005 | S3 | F3.L1.M11 |
| 45 | aportes-nucleo-financiero-SEG-006 | S3 | F1.L1.M10 |

Verificación: 45 fichas en el informe (`grep -c '^### aportes-nucleo-financiero-'` → 45) y 45 filas en esta tabla; por severidad 8 S1, 20 S2, 16 S3 y 1 S4, igual que la tabla de totales del informe.
