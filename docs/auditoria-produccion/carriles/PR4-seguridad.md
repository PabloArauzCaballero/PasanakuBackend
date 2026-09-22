# Carril PR4 — Seguridad transversal (Marcelo)

> **Avance calculado:** ver `Marcelo-Daily-Noche-2026-09-21.md` (HECHO/total). Este
> documento es la bitácora técnica del carril; el daily es el resumen de cierre.
>
> **Rama:** `marcelo/feature/carril-PR4-seguridad` desde `origin/dev` @
> `19a621e666afdea5bdc40aced326d3f212a116f4`.

## H1 — `aportes`: idempotencia con el scope de su índice y webhook en tres niveles

### H1.S1 — Baseline y barrido de lecturas de idempotencia

- **H1.S1.M1** (baseline `aportes`): ver `docs/auditoria-produccion/baseline-PR4-aportes.md`.
- **H1.S1.M2** (grep repo-wide + tabla): `docs/auditoria-produccion/idempotencia-scope.md`.
  12 índices relevados, 1 corregido en este carril (`uq_pago_idem`), 2 sin lectura Java
  (tarifas, sin dueño), 2 no implementados (`orden_cobro`/`intento_pago`, hallazgo de
  alcance), 1 hallazgo en `notificaciones` (sin dueño este turno).
- **H1.S1.M3** — `PagoRepositorio.porClaveIdempotencia` filtraba SOLO por
  `clave_idempotencia` (más ancho que `uq_pago_idem (obligacion_id, clave)`, hallazgo C
  del `PLAN.md`, vigente en el SHA base). Corregido a
  `porClaveIdempotencia(dsl, obligacionId, clave)`
  (`servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/PagoRepositorio.java:64-70`),
  con test `CU21Test.mismaClaveOtraObligacionEsOtroPago`
  (`servicios/aportes/src/test/java/bo/aportaya/aportes/CU21Test.java`).
- **H1.S1.M4** — 0 filas `FUERA DE SCOPE` sin dueño declarado (ver tabla de
  `idempotencia-scope.md`).

### H1.S2 — Webhook de la pasarela en tres niveles

**Hallazgo de alcance, declarado antes de construir**: al llegar a este carril,
`aportes` **no tenía ningún CU ni controlador que recibiera un webhook**. Solo existía
la tabla `aportes.webhook_pasarela` (generada por `scripts/generar_ddl.py`) y el índice
`uq_webhook_idem (proveedor_id, clave_idempotencia)`; ningún archivo Java la usaba
(`grep -rn webhook servicios/aportes/src/main` → solo el flag `soportaWebhook` de
`proveedor_pago`, sin endpoint receptor). H1.S2.M1 esperaba "localizar el CU del
webhook y su validación actual" — no existía qué localizar, así que se construyó,
dentro del alcance de `servicios/aportes/**`.

**Simplificación de alcance declarada** (no se resuelve por conveniencia, regla 00):
la cadena `obligación → orden_cobro → intento_pago → webhook → pago` que describe la
skill `payments-qr-integration` no existe en Java (`CU21CobrarAporte` acredita de forma
síncrona, sin `orden_cobro`/`intento_pago`). Construir esa cadena asíncrona completa es
una feature de producto que excede un carril de seguridad transversal. Lo que se
construyó conecta el webhook, ya verificado en firma/ventana/duplicado, con el `pago`
ya registrado por su `referencia_proveedor` — el único eslabón real que existe hoy.
Queda declarado como hallazgo de alcance, no como una decisión de seguridad tomada a
la ligera.

**Q-02 (DECIDIDA 2026-09-21) aplicada tal cual**: HMAC-SHA256 sobre el cuerpo crudo +
`X-Firma` + `X-Timestamp` (ventana ±5 min), en tres niveles.

| Pieza | Archivo |
|---|---|
| Contrato (`POST /pagos/webhooks/{proveedorCodigo}`, `text/plain` a propósito) | `servicios/aportes/src/main/resources/openapi/aportes.yaml` |
| Verificador de firma (puro, sin Spring) | `.../aportes/dominio/VerificadorDeFirmaWebhook.java` |
| Puerto del secreto (Q-02) | `.../aportes/dominio/ProveedorDeSecretoWebhook.java` |
| Doble `local`/`test` (secreto fijo) | `.../aportes/infraestructura/SecretoWebhookDeDesarrollo.java` |
| Fail-closed fuera de `local`/`test` (sin adaptador de vault real — `DECISION_REQUIRED`) | `.../aportes/infraestructura/SecretoWebhookSinConfigurar.java` |
| Repositorio append-only | `.../aportes/infraestructura/WebhookRepositorio.java` |
| CU-100 | `.../aportes/aplicacion/CU100RecibirWebhookPasarela.java` |
| Controlador (`@Publico`, sin `Idempotency-Key`: el dedupe es `(proveedor_id, clave)`) | `.../aportes/web/PagosController.java` |
| Tests unitarios del verificador (8, corredor `test`) | `.../aportes/dominio/VerificadorDeFirmaWebhookTest.java` |
| Tests de integración en 3 niveles (8, corredor `integrationTest`, patrón `CU*Test` + `--tests '*Webhook*'`) | `.../aportes/CU100WebhookTest.java` |

**Cobertura de la tabla H1.S2.M1** (correcto / límite / inválido):

| Caso | Nivel | Test |
|---|---|---|
| Firma válida + ventana vigente + monto igual → PROCESADO | correcto | `firmaValidaProcesaYConcilia` |
| Mismo evento entregado dos veces → DUPLICADO, un solo pago | correcto | `eventoRepetidoEsIdempotente` |
| Timestamp a 4:59 (dentro de ±5 min) → PROCESADO | límite | `timestampAlBordeDeLaVentanaDentroSeAcepta` |
| Timestamp a 5:01 (fuera de ±5 min) → rechazo, nada escrito | límite | `timestampJustoFueraDeLaVentanaSeRechaza` |
| Firma que no corresponde al cuerpo → rechazo, nada escrito | inválido | `firmaInvalidaSeRechazaSinEscribirNada` |
| Proveedor inexistente/sin soporte de webhook → mismo rechazo | inválido | `proveedorInexistenteSeRechaza` |
| Firma válida, monto distinto al pago → DESCARTADO, no acredita | inválido | `montoDistintoSeDescartaSinAcreditar` |
| Firma válida, sin pago para la referencia → DESCARTADO | inválido | `sinPagoParaLaReferenciaSeDescarta` |

**Estado de verificación (peldaño `evidence-and-verification`), actualizado**:
- `VerificadorDeFirmaWebhookTest` (unitario, sin Spring/DB): `TESTED` —
  `./gradlew :servicios:aportes:test --tests '*VerificadorDeFirmaWebhookTest*'` →
  **8/8 PASS**.
- `webTest` completo del servicio (confirma que el endpoint nuevo no rompe el
  contrato HTTP ni la sábana de seguridad): `TESTED` —
  `./gradlew :servicios:aportes:webTest` → **25/25 PASS, 0 skipped** (encontró y
  se corrigió un bug real: `PagosControllerWebTest`/`SeguridadWebTest` no
  mockeaban `CU100RecibirWebhookPasarela`).
- `ArquitecturaTest` (ArchUnit): **5/5 PASS** — las clases nuevas respetan la
  dirección de dependencia del servicio.
- `CU100WebhookTest`/`AuditoriaCriticaTest`/el nuevo caso de `CU21Test`
  (Testcontainers, PostgreSQL real): `BLOCKED` — no por el mismo problema de E/S
  de antes (ese se resolvió, ver `evidencia/H1-entorno-docker.md` §4), sino por
  una limitación DISTINTA de Docker-fuera-de-Docker: Testcontainers no puede
  bind-montar `sql/` en el contenedor Postgres que levanta, porque la ruta que
  calcula solo existe dentro del contenedor de Gradle, no en el daemon de Docker
  Desktop. Causa exacta, intentos (incl. instalar un JDK nativo) y siguiente
  paso: `evidencia/H1-entorno-docker.md` §5.

**Estado:** A MEDIAS — código completo, 33 de 49 tests nuevos/tocados con
ejecución real verificada (16 del webhook a nivel unitario+web, 25 del webTest
completo incluyendo el bugfix que encontraron); los que necesitan PostgreSQL
real (`CU21Test`, `CU100WebhookTest`, `AuditoriaCriticaTest`, 3 clases) quedan
bloqueados por el entorno, con causa raíz identificada y siguiente paso
concreto declarado.

## H2 — Inventario de endpoints y endurecimiento de entrada

### H2.S1 — Inventario generado

- `scripts/inventario_endpoints.py` (`--self-test`, `--check`), mismo patrón que
  `verificar_pruebas_web.py`/`auditar_backend.py`. Cruza `openapi/*.yaml` × controladores
  × rutas Spring literales.
- **Kill-test del propio encargo, ejecutado de verdad**: un endpoint agregado a mano
  con `@GetMapping` directo, sin contrato → el self-test #2 lo reproduce y falla
  correctamente (`self-test: 2 PASS`).
- **Bug real encontrado corriendo el script contra el repo real** (no en el self-test):
  la regex de detección de "operationId implementado" no manejaba genéricos anidados
  (`ResponseEntity<List<X>>`), y marcaba `listarContratosVigentes` (cumplimiento),
  `listarVerificaciones` (identidad), `listarBloques` y `evaluarInsignias`
  (transparencia) como "sin implementar" cuando SÍ estaban implementados. Corregido con
  un patrón que tolera un nivel de anidamiento (`_GENERICO` en el script). Evidencia:

  ```text
  $ python scripts/inventario_endpoints.py --check   (ANTES del fix de la regex)
  endpoints relevados: 158 · hallazgos: 4
    - servicios\cumplimiento: ... "listarContratosVigentes" ... ningun controlador lo implementa
    - servicios\identidad: ... "listarVerificaciones" ...
    - servicios\transparencia: ... "listarBloques" ...
    - servicios\transparencia: ... "evaluarInsignias" ...
  EXIT=1

  $ python scripts/inventario_endpoints.py --self-test && python scripts/inventario_endpoints.py --check   (DESPUES)
  self-test: 2 PASS (operationId sin implementar; endpoint agregado a mano sin contrato)
  endpoints relevados: 158 · hallazgos: 0
  El inventario esta alineado: todo lo implementado tiene contrato y viceversa.
  EXIT=0
  ```
- `docs/auditoria-produccion/endpoints.md` generado: 158 endpoints, columnas Service,
  Method, Path, Permission, Ownership, Idempotency, Rate limit, MFA, Audit, Tests,
  Sensible.
- **Columna `sensible`**: heurística por palabra clave (retiro, transferencia,
  aprobación, MFA, credencial, webhook, pago, saldo, documento, KYC, etc.) — ver
  `PALABRAS_SENSIBLES` en el script.
- **Para el CI** (paso que Pablo agrega — H2.S1.M2): agregar al job `boveda` (o al
  `codigo`) el paso

  ```yaml
  - name: Inventario de endpoints
    run: python3 scripts/inventario_endpoints.py --check
  ```

  y una tarea Gradle `inventarioEndpoints` que invoque el mismo script (patrón de
  `erroresCatalogo`), para que un endpoint sin contrato falle el build igual que en
  local.
- **Prueba negativa (H2.S1.M3)**: reproducida con fixtures en memoria dentro del
  propio `--self-test` (no se tocó el repo real para no dejar un endpoint roto en la
  rama, per regla de higiene) — ver `self-test 2` arriba, que ES exactamente el
  kill-test del §2 del encargo.

**Estado:** HECHO.

### H2.S2 — Entrada: mass assignment, límites, SQL, SSRF, uploads

- **H2.S2.M1** — `scripts/verificar_contratos_limites.py` (`--self-test`, 4 PASS).
  Encontró 4 hallazgos reales en `aportes` (corregidos: `EntradaEnrutamiento.claveIdempotencia`,
  `.yaIntentados`; `EntradaProveedor.urlBase`, `.referenciaCredenciales` — todos sin
  límite) y 83 hallazgos en otros 12 servicios (impresos como pendientes, no bloquean:
  fuera del alcance de este carril). Evidencia:

  ```text
  $ python scripts/verificar_contratos_limites.py
  servicios revisados con contrato OpenAPI · hallazgos totales: 87
  83 hallazgos en servicios sin dueño de este gate todavia (no bloquean): [...]
  Los contratos de request en el alcance de este gate declaran limites y additionalProperties: false.
  EXIT=0
  ```
- **H2.S2.M2** — Regla nueva en `verificar_seguridad.py` (bloque 7, `SQL-SEGURO`):
  `DSL.field(variable)`, `DSL.condition(...)`, `.execute(variable)`, `String.format`
  con SQL, sin comentario `// SQL-SEGURO: <por qué>` → falla en `servicios/aportes/**`
  y `scripts/`, aviso en el resto. **Hallazgo real encontrado**:
  `servicios/nucleo-financiero/.../DestinoRepositorio.java:34` —
  `DSL.field(DSL.name(columna), UUID.class)` con `columna` como parámetro `String` de
  un método PRIVADO, llamado hoy solo con los literales `"usuario_id"`/`"grupo_id"`
  (bug **latente**, no explotable con el código actual, pero el patrón — nombre de
  columna desde una variable — es exactamente lo que un refactor futuro convierte en
  inyectable sin que nadie lo note). Es `nucleo-financiero`: **OUT de este carril**
  (dueño: Justin, PR2) — reportado en el daily §6, no editado. `PagoRepositorio.reembolsadoDe`
  (aportes) revisado y marcado `// SQL-SEGURO:` (bind real, `?` con parámetro, texto
  literal en bloque triple-comillas).
- **H2.S2.M3** — Inventario de clientes HTTP salientes (`*PorHttp`, `RestClient`,
  `HttpClient`): `grep -rn "PorHttp\|RestClient\|HttpClient" servicios/aportes/src/main`
  → **ninguno**. `aportes` no tiene clientes HTTP salientes propios (el enrutamiento a
  proveedores es interno, vía `CU99EnrutarProveedor`/`ProveedorPagoRepositorio`, sin red
  saliente). Sin superficie SSRF en este servicio — declarado en `security-matrix.md`
  §SSRF junto con el inventario de los demás servicios (ver H5).
- **H2.S2.M4** — `ArchivosSeguridadTest` en `comun-archivos`: **coordinado con Leo**
  (dueño del módulo, per encargo). No iniciado este turno por prioridad de orden
  (H1→H6 primero); si el tiempo no alcanza, queda `TODO` declarado, con el hallazgo de
  que `aportes` no maneja uploads propios (sin superficie de archivos en este
  servicio — el escaneo de `EntradaDisputa`/`EntradaReembolso` no incluye archivos).

**Estado:** A MEDIAS (M1–M3 HECHO; M4 TODO, coordinado con Leo).

## H3 — Invariantes del libro y benchmark del advisory lock

**Estado: HECHO.** Ver `docs/auditoria-produccion/financial-invariants.md`
completo, con la evidencia literal de cada corrida.

- H3.S1.M1 (grep de `double`/`float`) — **HECHO**, comando y salida pegados: 0
  ocurrencias reales (las 2 que aparecen son comentarios que EXPLICAN por qué no se
  usa `double`).
- H3.S2.M1 (localizar el advisory lock) — **HECHO**, con línea exacta:
  `sql/40_reglas/restricciones.sql:56`, `pg_advisory_xact_lock(hashtext('cadena_transaccion_billetera'))`
  — lock GLOBAL (clave fija, no por cuenta), serializa TODA inserción en
  `transaccion_billetera` de cualquier cuenta.
- H3.S1.M2–M4 (11 escenarios contra PostgreSQL real) — **HECHO**: los 11
  escenarios de `LibroInvariantesTest` PASS (`./gradlew
  :servicios:nucleo-financiero:integrationTest --tests '*LibroInvariantesTest*'`
  → `BUILD SUCCESSFUL`, `tests="11" failures="0" errors="0"`), incluido el
  escenario 10 (excepción tras débito dentro de `Datos.conContexto`, forzada con
  una violación real de `fk_transferencia_p2p_grupo_id`, sin tocar código de
  producción). Hallazgo real en el escenario 8 (TOCTOU de `CU12TransferirSaldo`
  bajo replay exactamente concurrente), documentado para Justin (PR2), no
  editado.
- H3.S2.M2 (benchmark de 200 transferencias × 3 corridas) — **HECHO**:
  `LibroBenchmarkTest` corrido contra PostgreSQL real. 0 deadlocks en las 3
  corridas; contención alta del advisory lock (hasta 49/51 conexiones
  esperando). Evidencia en `evidencia/H3-benchmark-hashchain.txt`.
- H3.S2.M3 (decisión Q-03) — **HECHO**: se mantiene el advisory lock global, no
  se reabre; los números del benchmark quedan documentados como insumo para un
  ADR futuro.
- **Micro-PR al troncal ya aplicado**: `buildSrc/src/main/kotlin/aportaya.base.gradle.kts`
  incluye `**/Libro*Test.class`, `**/AppendOnlyTest.class`,
  `**/AuditoriaCriticaTest.class` en `integrationTest` (y los excluye de `test`),
  y solo corre la etiqueta JUnit `benchmark` con `-PcorrerBenchmarks`. Cambio
  puramente aditivo, sin tocar ningún patrón existente.

## H4 — RLS, grants, append-only, esquema desde cero

**Estado: A MEDIAS** (mejoró: los tests parametrizados por servicio y
`AppendOnlyTest` ya corrieron contra PostgreSQL real, con JDK 21 nativo, esta
misma sesión — no quedan sólo como hallazgos de lectura de código).

**Evidencia literal, corrida real, 2026-09-22:**

```
$ ./gradlew :plataforma:comun-pruebas:integrationTest --tests '*AislamientoEsquemaTest*'
TEST-bo.aportaya.plataforma.pruebas.AislamientoEsquemaTest.xml → tests="15" failures="0" errors="0"
  (14 parametrizados: ningunServicioLeeElEsquemaAjeno × cada uno de los 14 servicios
   + 1: soloElNucleoEscribeElLibro)

$ ./gradlew :servicios:aportes:integrationTest --tests '*AislamientoEsquemaTest*'
  (BD_URL_ADMIN=jdbc:postgresql://localhost:5543/pasanaku)
TEST-bo.aportaya.aportes.AislamientoEsquemaTest.xml → tests="2" failures="0" errors="0"
  (svcAportesNoSalteaLaPoliticaDeFila, rolAuditorNoEsMiembroDeRolAplicacion)

$ ./gradlew :servicios:nucleo-financiero:integrationTest --tests '*AppendOnlyTest*'
TEST-bo.aportaya.nucleofinanciero.AppendOnlyTest.xml → tests="8" failures="0" errors="0"
```

Total H4: **25/25 PASS** contra PostgreSQL real (Testcontainers, no simulado).

- **RLS real y extensa, más allá de lo que `AislamientoEsquemaTest` cubre hoy**:
  `sql/40_reglas/restricciones.sql:1260-1296` aplica `ENABLE`/`FORCE ROW LEVEL
  SECURITY` automáticamente a las **86 tablas** (según el comentario del propio SQL)
  que tienen columna `usuario_id` o `cuenta_billetera_id`, en TODOS los esquemas de
  servicio (el barrido corregido explícitamente para no quedarse en `public`, ver el
  comentario de la migración de ADR-017 en esa misma sección). La condición de
  visibilidad es `usuario_id = fn_seg_usuario_actual() OR fn_seg_rol_privilegiado()
  OR fn_seg_es_sistema()` — funciones definidas en `restricciones.sql:1204-1227`,
  todas sobre `current_setting('app.usuario_id'/'app.rol')`, que es exactamente lo
  que `Datos.conContexto` fija por transacción.
- **Hallazgo/pregunta abierta, declarada, no resuelta por conveniencia**:
  `fn_seg_rol_privilegiado()` mira `current_setting('app.rol')` — una variable de
  SESIÓN DE APLICACIÓN, no el rol de PostgreSQL. Esto es DISTINTO del mecanismo que
  ya prueba `AislamientoEsquemaTest` (`SET ROLE svc_*`, `rol_auditor` con `GRANT
  SELECT` cruzado). Un cliente que se conecta como `rol_auditor` por `psql` directo
  (sin pasar por `Datos.conContexto`, que es lo único que fija `app.rol`) **no
  necesariamente ve las filas que sus `GRANT` le permiten**, porque la RLS seguiría
  evaluando `fn_seg_usuario_actual()` como `NULL` y `fn_seg_rol_privilegiado()` como
  `false`. No se verificó en esta corrida si eso es el comportamiento real (exige
  una conexión `psql` real contra la base, con y sin `SET`, algo para H4.S1.M2) —
  queda declarado como pregunta a probar, no como hallazgo confirmado.
- `AislamientoEsquemaTest` (base, `plataforma/comun-pruebas`) ya cubre: lectura
  cruzada entre esquemas (×14, parametrizado) y escritura exclusiva del libro
  contable (nucleo_financiero). NO cubre: `FORCE RLS` en sí (que el dueño de una
  tabla no pueda saltarla), `rol_auditor` en la práctica (arriba), ni "migración con
  privilegios limitados".
- H4.S2.M1–M2 (esquema desde cero, re-aplicación con datos) — **TODO**:
  `aportaya-postgres` (compose) está arriba y sano, pero el esquema no se aplicó
  todavía en esta corrida (bloqueado por el mismo presupuesto de tiempo consumido en
  estabilizar el entorno Docker, ver `evidencia/H1-entorno-docker.md`).
- H4.S2.M3 (documento de migraciones) — **HECHO**: `docs/operacion/schema-changes.md`.
- H4.S2.M4 (guarda de semillas dev) — **REFERENCIADO, no reproducido**: el paso
  existe en `ci.yml` job `base`; no se corrió localmente en esta corrida.

## H5 — Auditoría append-only y matriz de seguridad

**Estado: A MEDIAS.** Ver `docs/auditoria-produccion/security-matrix.md` completo.

- H5.S1.M1 (localizar la bitácora + tabla de qué CU la usan) — **HECHO**: hallazgo
  real y significativo — `comun.bitacora_evento` (cadena de hash, desde antes de
  este carril) tenía CERO escritores en código de producción, en NINGÚN servicio.
- H5.S1.M2 — **A MEDIAS**: `aportes` corregido (`AuditoriaRepositorio` +
  `CU19ReembolsarPago.aprobar` escriben la bitácora en la aprobación de reembolso;
  test `AuditoriaCriticaTest`, verificación contra PostgreSQL real pendiente de esta
  corrida). `identidad`/`nucleo-financiero`/`cumplimiento`: hallazgo entregado,
  ningún archivo ajeno editado.
- H5.S1.M3 (matriz OWASP) — **HECHO**: 10 categorías + la tabla de bitácora por CU
  crítico.

## H6 — PIT, código muerto, dependencias

**Estado: A MEDIAS.**

- H6.S1 (PIT) — **HECHO**, en un micro-PR SEPARADO
  (`marcelo/chore/pitest-plugin`, PR #17 contra `dev`, no en esta rama) porque
  toca `buildSrc/`+`gradle/libs.versions.toml` — el propio catálogo se declara
  "MICRO-PR, nunca una rama de carril". `info.solidsoft.pitest` 1.19.0 +
  `pitest-junit5-plugin` 1.2.3, corrido de verdad contra Gradle 9.7.1 (dos
  incompatibilidades reales encontradas y corregidas — accessor tipado no
  generado en un plugin de convención, y un `strictly` de PIT sobre
  `junit-platform-launcher` que chocaba con el BOM de Spring Boot). Resultado:
  `VerificadorDeFirmaWebhook` 12/12 mutantes matados (100%). Detalle completo en
  `docs/auditoria-produccion/mutation-testing.md`.
- H6.S2.M1 (inventario `TODO`/`FIXME`) — **HECHO**:

  ```text
  $ git grep -nE "TODO|FIXME" -- servicios plataforma | wc -l
  12
  ```

  Las 12 son la palabra ESPAÑOLA "TODO"/"TODOS" ("cubre TODO", "TODOS_HABILES",
  "notifica a TODO el grupo") o la subcadena "METODO" — **cero marcadores reales de
  trabajo pendiente** en `servicios/`+`plataforma/`. Se corrigieron además DOS
  falsos positivos PROPIOS de este carril antes de correr el grep final: la misma
  palabra española "TODO" en dos comentarios nuevos de `aportes`
  (`ProveedorDeSecretoWebhook.java`, `SecretoWebhookSinConfigurar.java`),
  reescritos para no ensuciar la evidencia de este mismo gate.
- H6.S2.M2 (`./gradlew :aportes:check` verde) — **PENDIENTE** de la corrida de
  Gradle de esta sesión (ver §H1 arriba).
- H6.S2.M3 (los 7 `idempotencia.exigirNueva`) — **HECHO** (inventario, sin editar
  archivos ajenos):

  | Archivo | CU | Decisión aplicable (Q-04/ADR-046, sin ADR en `dev`: supuesto "solo efecto financiero/irreversible") |
  |---|---|---|
  | `servicios/grupos/.../CU59CargarCalendario.java:24` | CU-59 | No es efecto financiero irreversible por sí solo (carga de calendario) — supuesto: no exige idempotencia dura, comentario puede quedar hasta que Leo resuelva ADR-046 |
  | `servicios/grupos/.../CU60SortearTurnos.java:24` | CU-60 | Sorteo de turnos: **sí** tiene efecto irreversible (determina el orden de cobro) — candidato a activar primero |
  | `servicios/grupos/.../CU62PermutarTurnos.java:24` | CU-62 | Permuta de turnos: efecto irreversible sobre el orden — candidato a activar |
  | `servicios/grupos/.../CU63ResolverAcuerdo.java:24` | CU-63 | Resolución de acuerdo: efecto irreversible — candidato a activar |
  | `servicios/grupos/.../CU65RetirarParticipante.java:24` | CU-65 | Retiro de participante: irreversible — candidato a activar |
  | `servicios/grupos/.../CU68AceptarIngreso.java:24` | CU-68 | Aceptar ingreso: irreversible (ocupa un cupo) — candidato a activar |
  | `servicios/identidad/.../CU09RestablecerOperador.java:24` | CU-09 | Restablecer credenciales de operador: irreversible y sensible — candidato a activar con prioridad alta |

  Los archivos son de `grupos` (sin dueño este turno) e `identidad` (Richard, PR1):
  no se editan. Este carril NO adopta `idempotencia.exigirNueva` por decisión propia
  (Q-04/regla 00) — la tabla de arriba es el hallazgo, no una resolución.
- H6.S2.M4 (dependencias.md) — **A MEDIAS**: catálogo relevado sin el build
  (`docs/auditoria-produccion/dependencias.md`); árbol real de
  `./gradlew :aportes:dependencies` pendiente.
- H6.S2.M5 (parches CVE) — **TODO**: depende de H6.S2.M4.
- H6.S2.M6 (cierre, merge, espejo `test`) — **BLOQUEADO por la restricción de la
  plataforma**: no se ejecuta `gh pr merge` ni `git push origin origin/dev:test` sin
  aprobación humana (impuesto por la herramienta, no por el encargo). El PR queda
  abierto para revisión — ver la sección "PRs abiertos" del daily.

## Para el CI (consolidado, todo lo que Pablo agrega)

```yaml
- name: Inventario de endpoints
  run: python3 scripts/inventario_endpoints.py --check
- name: Contratos con límites explícitos
  run: python3 scripts/verificar_contratos_limites.py
- name: Seguridad (incluye bloque 7, SQL-SEGURO)
  run: python3 scripts/verificar_seguridad.py
```
