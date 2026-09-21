# Plan — PasanakuBackend (AportaYa): `dev` → production ready, técnicamente demostrable

- Fecha: 2026-09-21 · Repos afectados: `PabloArauzCaballero/PasanakuBackend` (rama `dev`), este repo (solo `docs/trabajo/`) · Predecesor: `planes/informes/auditoria-backend.md` y `docs/Auditoria-Robustez.md` del backend (auditorías previas, no verificadas contra el SHA actual hasta H0)
- **SHA inicial de `origin/dev`: `19a621e666afdea5bdc40aced326d3f212a116f4`** (commit del 2026-09-21 15:22 -0400, "fix: los fronts no construian — el paquete de tutoriales no entraba en la imagen"). `main` = `519747a5…`, `test` = mismo SHA que `dev`.
- Resultado observable: quien despliega `dev` en un entorno con perfil `production` obtiene catorce servicios que **no arrancan sin clave de firma JWT ni con el bypass de MFA**, que publican de verdad al broker cada evento del outbox, que rechazan un retiro sin step-up MFA, que exigen un segundo aprobador distinto del solicitante por encima del umbral, que devuelven la misma respuesta ante un reintento con la misma clave y `409` ante la misma clave con otro cuerpo, y un CI en verde en el SHA final con escaneo real de dependencias, SBOM e imagen. Todo eso con salida literal pegada en `docs/auditoria-produccion/`.
- Kill-test: levantar `base.yml --profile base` + `nucleo-financiero`, ejecutar una transferencia (CU-12) y consultar `SELECT estado FROM nucleo_financiero.evento_dominio` 10 s después. Hoy queda `PENDIENTE` para siempre (no existe bean de `Relevo`, ver H2). Si al cerrar sigue `PENDIENTE`, esto NO está hecho.

## 0. Cómo se ejecuta este plan

| Tema | Regla para este trabajo |
|---|---|
| Dónde vive | Se redacta acá (`docs/trabajo/2026-09-21-backend-production-ready/`). En **H0.S1.M4** se copia a `PasanakuBackend/docs/auditoria-produccion/PLAN.md` y desde ahí se actualiza en el momento; `PROGRESS.md` (metaprompt §94) es el espejo de estados de este plan, no un documento aparte. Al cierre, `REPORTE.md` acá (regla 40) + `FINAL_REPORT.md` en el backend (metaprompt §86), con el mismo contenido de avance. |
| Evidencia | Canónica en `PasanakuBackend/docs/auditoria-produccion/evidencia/<ID>-<slug>.txt` (salida literal, recortada, **sin datos de personas ni secretos**). Este repo enlaza, no duplica. |
| Estados | Exactamente los seis de la regla 20. `HECHO` solo con la salida del DoD pegada. Avance = `microtareas HECHO / total`. |
| Orden | El de la regla 5/93 del metaprompt: H0 → H1 → H2 → H3 → H4 → H5 → H6 → H7 → H8 → H9 → H10 → H11 → H12. Ningún refactor cosmético antes de cerrar H1–H5. |
| Una microtarea a la vez | Una sola en `EN CURSO`. Un commit atómico por microtarea o por subtarea coherente (§61), con el prefijo indicado en cada fila. |
| Por cada fix (§60/§91) | Reproducir → test que falla → corregir → test verde → integración → módulo → suite → documentar. En el reporte: problema, causa raíz, solución, test, evidencia, riesgo residual. |
| Fail closed (§89) | Si algo crítico no se puede implementar por falta de infraestructura: **no se hace bypass**. Se aísla el contrato y se simula en tres niveles (regla 65) declarando el doble. |
| No tocar `main` (§62) | Todo va a `dev` (o a una rama `pablo/feature/produccion-ready` que se mergea a `dev`; decisión en H0.S1.M5). El merge `dev → main` no se ejecuta: se recomienda. |
| Recursos (regla 70) | Un build/suite a la vez; `integrationTest` por módulo (`:servicios:<svc>:integrationTest`) antes que la raíz; Testcontainers necesita Docker Desktop sano. |

## 1. Alcance

- **IN:**
  - Todo lo enumerado en los hitos H0–H12 sobre `servicios/`, `plataforma/`, `sql/` (vía generador), `scripts/`, `buildSrc/`, `despliegue/`, `.github/`, `docs/auditoria-produccion/`, `docs/operacion/`, `docs/Arquitectura/ADR-0xx`.
  - Los hallazgos previos A–J del metaprompt §87, revalidados en el SHA actual (tabla §2.4).
  - Defectos nuevos que aparezcan en el camino: **se agregan al plan** como microtarea con CA y DoD antes de tocarlos.
- **OUT (aunque se vea roto):**
  - `main` y `test` (ramas). `apps/`, `packages/`, `landing/` (frontends) salvo que un cambio de contrato OpenAPI obligue a regenerar clientes (`generateOpenApiClients`, eso sí entra).
  - Refactors de nombres, reformateos masivos fuera de `spotlessApply`, upgrades mayores de dependencias (§53: no majors masivos).
  - Lógica de negocio del pasanaku (grupos, sorteo, garantía) que no toque dinero/identidad/mensajería/seguridad.
  - Decisiones de negocio y regulatorias (umbrales UIF, RPO/RTO, proveedor real de pagos/MFA): se registran como `DECISION_REQUIRED`, no se inventan.
  - Aplicar la protección de ramas en GitHub: se genera el documento y los comandos exactos; **ejecutarlos exige confirmación explícita de Pablo** (acción sobre algo compartido).

## 2. Descubrimiento factual (Fase 1) — contra el SHA `19a621e6`

Clonado en solo lectura con `git clone --depth 1 --branch dev` (hizo falta `core.longpaths=true` en Windows: el checkout fallaba por rutas largas en `docs/Modelos/…`).

### 2.1 Hechos (con ruta)

**Stack y build**
- Gradle wrapper 9.7.1 (`gradle/wrapper/gradle-wrapper.properties`), toolchain Java 21 por foojay (`settings.gradle.kts:15`). `rootProject.name = "aportaya"`.
- Catálogo `gradle/libs.versions.toml`: Spring Boot 3.5.6, jOOQ 3.20.5, ShedLock 6.9.0, resilience4j 2.3.0, Argon2 2.12, ArchUnit 1.4.0, jqwik 1.9.3, Spotless 7.0.4, openapi-generator 7.14.0, Testcontainers (postgres + **kafka** declarado, `testcontainers-kafka`), micrometer prometheus + tracing brave.
- 14 servicios por barrido de `servicios/` (`settings.gradle.kts:45-48`): aportes, auditoria, cumplimiento, entregas, erp, garantia, grupos, identidad, notificaciones, nucleo-financiero, organizador, publicidad, tarifas, transparencia. Más `plataforma/gateway` (Spring Cloud Gateway) y `plataforma/comun-{dominio,datos,web,mensajeria,archivos,pruebas}`.
- Cada servicio declara `actuator`, `kafka`, `shedlock`, `resilience4j`, `micrometer` (`servicios/nucleo-financiero/build.gradle.kts:29-36`, `servicios/identidad/build.gradle.kts:29-39`).
- Tareas raíz agregadoras (`build.gradle.kts:23-37`): `spotlessCheck`, `spotlessApply`, `check`, `test`, `webTest`, `integrationTest`, `contractTest`, `sagaTest`, `e2eTest`, `generateJooq`, `generateOpenApiClients`, `erroresCatalogo`, `verificar`. `testBarrido` existe por módulo (`buildSrc/src/main/kotlin/aportaya.base.gradle.kts:336`) y el CI lo invoca en raíz (`ci.yml:72`).
- Patrones de corredores (`aportaya.base.gradle.kts:181-212`): `integrationTest` = `**/CU*Test`, `**/*RepositorioTest`, `**/Aislamiento*Test`, `**/Arranque*Test` (Testcontainers, 120 s por prueba); `webTest` = `**/*WebTest` (MockMvc, sin contenedor); `contractTest` = `**/*ContratoTest`; `sagaTest` = `**/*SagaTest`; `e2eTest` = `**/*E2ETest` (300 s); `testBarrido` = `**/*BarridoTest`.
- `-Werror` activo (`aportaya.base.gradle.kts:18-20`); Spotless; JaCoCo con piso; guarda `sinJpa` (`aportaya.servicio.gradle.kts:36-61`).
- **El esquema no son migraciones**: `sql/aplicar.sql` es el esquema entero, generado por `scripts/generar_ddl.py` desde los `.puml` + `scripts/modelo.py` (`README.md` §"De la bóveda al código"; cabecera de cada `sql/10_tablas/**.sql`: "Generado por scripts/generar_ddl.py — no editar a mano"). El CI falla si `generar_ddl.py` deja diff (`ci.yml:90-91`). **Todo cambio de esquema de este plan va por el generador.**
- Dockerfile único multietapa (`despliegue/Dockerfile`): `eclipse-temurin:21-jre-noble`, usuario `app` no root (`:80,:92`), `HEALTHCHECK` sobre `/actuator/health/readiness` (`:96-97`), capas de Spring Boot. Instala `wget` en runtime (`:82`).

**CI (`.github/workflows/ci.yml`)**
- Jobs: `codigo` (spotlessCheck → check -x test → testBarrido → compileJava → erroresCatalogo), `boveda`, `base` (aplicar.sql en limpio, semillas ×2, semillas dev ×2, rechazo de semillas dev en producción, permisos `sql/50_verificacion/verificaciones.sql`), `contratos` (generateOpenApiClients), `frontend`, `pruebas` (test, webTest, integrationTest, contractTest, sagaTest), `imagenes` (bootJar + docker build + verifica `User == app`), `e2e` (**solo en `main`**, `ci.yml` `if: github.ref == 'refs/heads/main'`), `seguridad` (gitleaks `ci.yml:391`; "19c dependencias vulnerables" = **solo** `./gradlew :<m>:dependencies --configuration runtimeClasspath`, `ci.yml:396-407`).
- **Estado real en `dev` @ `19a621e6`** (`gh run list`/`gh run view 35644455765`): workflow `CI` en **failure**, job "Formato, reglas propias y compilacion" falla en el paso **"2 · formato"** (Spotless); `pruebas`, `imagenes`, `contratos`, `frontend` quedan `skipped`. El workflow "Bóveda, esquema, semillas y despliegue" pasa. Las dos corridas anteriores en `dev` (2026-09-21 18:12 y 2026-09-17 21:19) también fallaron en `CI`.
- No existe `.github/CODEOWNERS`, ni Dependabot, ni OSV/Trivy/Grype/CycloneDX (grep vacío en `.github/`).
- Protección de ramas (`gh api …/branches/{dev,main}/protection` → `404 Branch not protected`; `…/rulesets` → `[]`). **`gh` está autenticado como el dueño del repo**, así que aplicar protección es posible pero es acción compartida (ver Alcance OUT).

**Idempotencia**
- Tabla `nucleo_financiero.respuesta_idempotente` (`sql/10_tablas/10_billetera_custodia/respuesta_idempotente.sql`): `usuario_id`, `operacion`, `clave_idempotencia`, `hash_solicitud`, `codigo_http`, `cuerpo_respuesta`, `registrada_en`, `expira_en`. Índice único **`(usuario_id, clave_idempotencia, operacion)`** (`sql/30_indices/10_billetera_custodia.sql:148-149`). Solo existe en el esquema `nucleo_financiero` (único `CREATE TABLE` encontrado).
- `plataforma/comun-web/.../idempotencia/Idempotencia.java`: `exigirNueva` hace `SELECT … WHERE clave_idempotencia = ? AND operacion = ?` **sin `usuario_id`** (`:43-47`); nunca compara `hash_solicitud` con el almacenado (lanza `OperacionRepetida` con la respuesta previa ante cualquier coincidencia, `:49-51`); `guardarRespuesta` hace `UPDATE … WHERE clave_idempotencia = ?` **solo por clave** (`:85-90`). Se cablea como bean (`ConfiguracionComunWeb.java:32-36`) pero **ningún caso de uso llama a `exigirNueva`**: aparece solo en comentarios de esqueleto de 7 CU (`grupos/CU59,60,62,63,65,68`, `identidad/CU09`).
- Índices únicos del dinero (`sql/40_reglas/restricciones.sql:236-263`): `uq_tx_idem` sobre `transaccion_billetera (COALESCE(iniciada_por,…), origen_tipo, clave_idempotencia)`; `uq_recarga_idem (cuenta_billetera_id, clave)`; `uq_retiro_idem (cuenta_billetera_id, clave)`; `uq_pago_idem (obligacion_id, clave)`; `uq_orden_cobro_idem`, `uq_intento_pago_idem`, `uq_webhook_idem (proveedor_id, clave)`, `uq_cotizacion_idem`, `uq_token_verificacion_idem`, `uq_devengo_idem`.
- Lecturas Java **más amplias que su índice** (todas `WHERE clave_idempotencia = ?` a secas): `LibroDeBilletera.porClaveIdempotencia` (`:100-105`, usado por `CU12TransferirSaldo:72`); `OrdenRetiroRepositorio.porClaveIdempotencia` (`:87-92`, usado por `CU11RetirarSaldo:85`); `OrdenRecargaRepositorio.porClaveIdempotencia` (`:70-75`, usado por `CU10RecargarSaldo:82`); `aportes/PagoRepositorio.porClaveIdempotencia` (`:53`, usado por `CU21CobrarAporte:63`). Consecuencia: el usuario B que reutiliza la clave de A recibe la orden/transacción de A como "replay".
- `Idempotency-Key` es cabecera obligatoria UUID en toda operación con efecto (`SabanaDeSeguridadWeb.java:139-157`, `ManejadorGlobalDeErroresWebTest:101-120`).

**Outbox / Kafka**
- `sql/15_infra/mensajeria.sql`: por esquema, `evento_dominio` (`id, tipo, version, agregado, agregado_id, payload, metadatos, correlation_id, causation_id, ocurrido_en, publicado_en, estado CHECK IN ('PENDIENTE','PUBLICADO','FALLIDO'), intentos`), `evento_consumido (id_evento, consumidor)` y `shedlock`. Permisos: `GRANT UPDATE (publicado_en, estado, intentos)` únicamente (`sql/00_base/03_permisos.sql:139-141`).
- `Outbox.emitir` escribe en la misma transacción (`Outbox.java:31-58`); `Consumidos.registrar` dedupe con `ON CONFLICT DO NOTHING` (`Consumidos.java:25-32`).
- **`Relevo.java` no es bean**: sin `@Component`, sin `new Relevo(` ni `@Bean` en todo el repo; solo `cumplimiento` tiene `@EnableScheduling` (`servicios/cumplimiento/.../Aplicacion.java:20`); **no existe `LockProvider` ni `@EnableSchedulerLock`** en ningún archivo (grep). `aportaya.outbox.intervalo` está en los `application.yml` pero nadie lo consume. → El outbox **nunca publica**.
- `Relevo.relevar` (`:48-69`): `@Scheduled` + `@SchedulerLock` + `@Transactional`; `SELECT … FOR UPDATE SKIP LOCKED` y luego `kafka.send(...).get()` **dentro de la transacción** (`:76-80`); ante excepción loguea y deja `PENDIENTE` sin backoff (`:85-89`); `marcar` incrementa `intentos` solo al publicar. Métrica `aportaya.outbox.edad_mas_viejo_segundos` (`:45`).
- **No hay ningún `@KafkaListener`, `KafkaContainer` ni `EmbeddedKafka`** en el repo (grep). Kafka existe en `despliegue/compose/base.yml:149-171` (cp-kafka 7.7.1, KRaft) y en `docker-compose.coolify.yml`.

**MFA de retiro**
- Puerto `SegundoFactor` (`nucleo-financiero/dominio/puertos/SegundoFactor.java:20-28`); adaptador `SegundoFactorLocal` (`infraestructura/SegundoFactorLocal.java:22-40`): `@Component` **sin `@Profile`**; con `aportaya.mfa.exigido=true` (valor por omisión y en `application.yml:22-25`) devuelve **siempre `false`** → todo retiro cae en `MFA_REQUERIDO` (`CondicionesDeRetiro` vía `CU11:103-116`); con `exigido=false` acepta **cualquier string no vacío** (`:33-38`). `BilleteraController.solicitarRetiro` lo invoca fuera de la transacción (`:186`).
- `identidad` ya tiene MFA en el login: `ExigeSegundoFactor`, `CU04Autenticar:142-149`, esquema `FactorPresentado { tipo: OTP|BIOMETRIA|TOTP }` en `servicios/identidad/src/main/resources/openapi/identidad.yaml:384-415`, tablas `factor_mfa`, `dispositivo`, `token_verificacion`, `politica_token`, `intento_validacion_token` (`sql/10_tablas/01_identidad_usuarios/`). **No hay operación de challenge/verificación de factor con propósito** (grep `step|desafio` en el contrato: solo el login). Patrón de confinamiento por perfil ya presente: `DesafioDeDesarrollo` `@Profile("local")` (`identidad/infraestructura/DesafioDeDesarrollo.java:32`).
- JWT actual no lleva `acr/amr` ni nada de segundo factor (`EmisorDeAcceso.java:79-88`: `sub, jti, iat, exp, rol, permisos, nivel_diligencia, dispositivo`).

**Doble aprobación de retiros**
- `orden_retiro` (`sql/10_tablas/10_billetera_custodia/orden_retiro.sql`): `solicitada_por NOT NULL`, `aprobada_por NULL`, `requiere_doble_aprobacion`, `mfa_verificado`, `estado CHECK IN ('AUTORIZADA','BORRADOR','EN_PROCESO','EN_REVISION','PAGADA','PENDIENTE','RECHAZADA','REVERSADA')` (`:28`), montos `NUMERIC(16,2)` + `moneda CHAR(3)`.
- `ck_retiro_doble_aprobacion` (`restricciones.sql:1363-1368`): `NOT requiere OR estado IN ('BORRADOR','PENDIENTE','RECHAZADA') OR (aprobada_por IS NOT NULL AND aprobada_por <> solicitada_por)`.
- Umbral `aportaya.retiro.doble-aprobacion-desde: 5000.00` (`nucleo-financiero/application.yml`); el controller lo compara (`BilleteraController:187`).
- `CU11RetirarSaldo.confirmarPago` hace `pasarA(PENDIENTE → PAGADA)` **directo** (`:181`); no existe `aprobar` en `nucleo-financiero` (grep vacío); el contrato `openapi/nucleo-financiero.yaml` no tiene ruta de aprobación (paths `:32-340`); `SalidaRetiro.estado` enum `[PENDIENTE, EN_REVISION, AUTORIZADA, PAGADA, RECHAZADA]` (`:503`). Permisos existentes: `BILLETERA_OPERAR`, `BILLETERA_VER`, `BILLETERA_VER_TERCEROS`, `REVERSO_AUTORIZAR`; catálogo en `sql/60_semillas/10-roles-y-permisos.sql:26,98,100`.
- `confirmarPago`/`rechazar` existen en el CU (`:173-253`) pero **ningún endpoint los expone** en el contrato.

**JWT / identidad**
- `EmisorDeAcceso.java:52-66`: si `aportaya.jwt.clave-firma` está vacía **genera RSA en memoria y solo `WARN`**; `identidad/application.yml`: `clave-firma: ${JWT_CLAVE_FIRMA:}` (default vacío). Emite RS256 con `kid`, vigencia 15 min. **Sin `iss` ni `aud`.**
- Decoder común (`comun-web/seguridad/ConfiguracionDelDecodificador.java:27-28`): `NimbusJwtDecoder.withJwkSetUri(jwksUri).build()` → **sin validador de issuer/audience**, skew por omisión.
- Pimienta obligatoria sin default (`identidad/application.yml`: `pimienta: ${SEGURIDAD_PIMIENTA}`); Argon2 por catálogo; lockout `acceso.intentos-maximos: 5`.
- Reuso de refresh: trigger en base R-SEG-09 (`restricciones.sql` ≈`:1370+`, "reusarlo revoca la familia entera"); en Java solo `CorteDeCredencial.java` menciona refresco. Sin test de integración del caso A→B→A encontrado.

**HTTP / seguridad / observabilidad**
- Gateway (`plataforma/gateway/src/main/resources/application.yml`): timeouts `connect 2000ms / response 10s`, `DedupeResponseHeader=Access-Control-Allow-Origin`; **sin rate limiting ni configuración CORS** (grep vacío en gateway y `despliegue/nginx`). NGINX (`despliegue/nginx/aportaya.conf:14-16`): `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`; sin HSTS ni `Cache-Control` sensibles.
- Probes habilitados (`management.endpoint.health.probes.enabled: true` en los `application.yml`); gateway expone `health,info,prometheus`.
- Existe `@Permiso`/`@Publico` con guardia al arranque (`TodoEndpointDecideSuAcceso`, citado en `ArranqueTest`), `SabanaDeSeguridadWeb` (401/403/`Idempotency-Key`), `Traza` (correlación) y `ManejadorGlobalDeErrores`.
- Un solo `application.yml` por servicio; **no hay `application-{local,test,staging,production}.yml`** en identidad, nucleo-financiero ni gateway.
- Scripts propios ya presentes: `scripts/auditar_backend.py`, `verificar_seguridad.py`, `verificar_criterios.py`, `verificar_boveda.py`, `generar_compose.py`, `generar_gateway.py`, `generar_k8s.py`, `generar_ddl.py`.

**Pruebas existentes en `nucleo-financiero`** (`src/test/java/bo/aportaya/nucleofinanciero/`): `ArquitecturaTest` (ArchUnit), `ArranqueTest`, `BarridoTest`, `BilleteraControllerWebTest`, `SeguridadWebTest`, `CU10…CU17`, `CU24`, `CU40`, `CU50`, `CU51` (+ `*RechazosTest`), `CU10ConcurrenciaTest`, `CuadrarPartidasTest`, `CuadrarPartidasPropiedadTest` (jqwik), fixture `BaseDeBilletera`; `comun-pruebas`: `BaseDePrueba.contenedor()` (PostgreSQL Testcontainers singleton), `AislamientoEsquemaTest`, `Barrido`, `SinUmbralLiteral`.

**Máquina donde se planificó** (Windows 11): Docker 29.6.2 OK; Python 3.14.2 OK; `gh` 2.97 autenticado; **`java` y `psql` no están en el PATH**. Gradle necesita un JDK para arrancar aunque foojay resuelva el toolchain → H0.S1.

### 2.2 Desconocidos (se resuelven en H0 con comando y salida)
- Resultado real de `./gradlew verificar`, `integrationTest`, `contractTest`, `sagaTest`, `e2eTest` en el SHA (el CI solo llegó a Spotless).
- Qué falla exactamente en Spotless (archivos).
- Si `sql/aplicar.sql` + `sembrar.sql` ×2 + `verificaciones.sql` pasan localmente (el CI dice que sí en `base`; hay que reproducirlo).
- Cobertura de `AislamientoEsquemaTest` respecto a RLS/`FORCE ROW LEVEL SECURITY`/rol auditor.
- Si existe adaptador de proveedor de retiros (quién llama a `confirmarPago`/`rechazar`) y cómo se registran `referencia_proveedor`/timeouts.
- Formato actual de `ManejadorGlobalDeErrores` (campos del error) y de los logs (JSON o texto).
- Qué HTTP clients existen (`*PorHttp`) y si usan resilience4j.
- Si `identidad` valida/rota refresh tokens en Java además del trigger.

### 2.3 Hipótesis (nunca presentadas como hecho)
- H-1: El `FALLIDO` del outbox no se usa en ningún lado (no se encontró código que lo escriba).
- H-2: `confirmarPago`/`rechazar` están pensados para una saga/consumidor del proveedor que aún no existe.
- H-3: Los `application.yml` sin perfiles implican que "producción" hoy es el mismo YAML con variables de entorno; no hay guarda que distinga entornos salvo la de semillas dev en base.
- H-4: Spotless falla por archivos del último commit de frontends/tutoriales, no por Java (a confirmar en H0.S3.M1).

### 2.4 Revalidación de los hallazgos previos (§87) en `19a621e6`

| # | Hallazgo previo | Estado en el SHA | Evidencia | Hito |
|---|---|---|---|---|
| A | `Idempotencia.java` lee sin `usuario_id`, actualiza solo por clave, no compara hash | **VIGENTE**, además **sin uso real** (latente) | `Idempotencia.java:43-47, 49-51, 85-90`; grep `exigirNueva` | H1.S1 |
| B | `porClaveIdempotencia` del ledger más amplio que `uq_tx_idem` | **VIGENTE** | `LibroDeBilletera.java:100-105` vs `restricciones.sql:236-238` | H1.S2 |
| C | Lookup de retiro sin scope de cuenta | **VIGENTE**, y también recarga y pago de aportes | `OrdenRetiroRepositorio:87-92`, `OrdenRecargaRepositorio:70-75`, `aportes/PagoRepositorio:53` | H1.S3, H1.S4 |
| D | `Relevo` sin bean/configuración activa | **VIGENTE y peor**: sin bean, sin `@EnableScheduling` (salvo cumplimiento), sin `LockProvider`, sin consumidores | grep; `Relevo.java`; `Aplicacion.java` ×14 | H2 |
| E | MFA local: exigido→false; no exigido→cualquier string | **VIGENTE** | `SegundoFactorLocal.java:31-40` | H3 |
| F | DB exige four-eyes; app sin caso de uso/endpoint de aprobación | **VIGENTE** | `CU11:181`; contrato sin ruta; grep `aprobar` vacío | H4 |
| G | RSA efímera si falta configuración | **VIGENTE** (solo WARN) | `EmisorDeAcceso.java:52-66`; `application.yml` default vacío | H5.S1 |
| H | CI rojo en Spotless | **VIGENTE** | `gh run view 35644455765` → paso "2 · formato" failure | H6.S1 |
| I | SCA falsa (solo `dependencies`) | **VIGENTE** | `ci.yml:396-407` | H6.S2 |
| J | `dev` y `main` sin protección | **VIGENTE** | `gh api` → 404, rulesets `[]` | H6.S5 |

## 3. Ambigüedades registradas

| # | Pregunta abierta | Supuesto tomado para avanzar | A quién confirmar |
|---|---|---|---|
| AMB-1 | ¿Dónde vive el plan/reporte del trabajo: acá o en el backend? | Se redacta acá; se copia y se mantiene en `PasanakuBackend/docs/auditoria-produccion/PLAN.md` (que también cumple el rol de `PROGRESS.md`). `REPORTE.md` acá + `FINAL_REPORT.md` allá. | Pablo |
| AMB-2 | ¿Cómo valida `nucleo-financiero` la evidencia step-up sin leer la base de `identidad` (invariante 11) y sin llamada de red dentro de la transacción (invariante 6)? | `identidad` emite un **JWT step-up RS256 corto** (`proposito=RETIRO`, `challenge_id`, `jti`, `acr`, `exp ≤ 5 min`, `aud=nucleo-financiero`) firmado con su misma clave; `nucleo-financiero` lo valida con el JWKS que ya usa, fuera de la transacción, y consume el `jti` una sola vez en una tabla propia `nucleo_financiero.evidencia_mfa_consumida` (vía generador DDL). | Pablo (arquitectura) |
| AMB-3 | ¿Qué factor real verifica `identidad` en el challenge de retiro? | Los ya modelados en `FactorPresentado` (`OTP`, `TOTP`, `BIOMETRIA`) con el proveedor de OTP como puerto; en `local/test`, doble con **tres niveles** (correcto / límite: OTP al borde de expiración / inválido: OTP ajeno, expirado, ya usado). El proveedor real de OTP es `DECISION_REQUIRED`. | Pablo · negocio |
| AMB-4 | Nombre y permiso del endpoint de aprobación | `POST /billetera/retiros/{ordenId}/aprobacion` (mismo estilo que `/recargas/{ordenId}/acreditacion` y `/retenciones/{id}/cierre`), permiso nuevo **`RETIRO_APROBAR`** asignado a un rol de backoffice (no a `PARTICIPANTE` ni `ORGANIZADOR`). Rechazo por el mismo endpoint con `desenlace: RECHAZADA`. | Pablo |
| AMB-5 | ¿Un retiro por debajo del umbral pasa `PENDIENTE → EN_PROCESO → PAGADA` o `PENDIENTE → PAGADA`? | Siempre `PENDIENTE → AUTORIZADA → EN_PROCESO → PAGADA`; para monto < umbral la autorización es automática en la misma transacción de creación (`aprobada_por = NULL`, `requiere_doble_aprobacion = false`). Estados existentes del CHECK, ninguno nuevo. | Pablo |
| AMB-6 | ¿Rate limiting distribuido exige agregar Redis al stack? | Sí: Spring Cloud Gateway `RequestRateLimiter` necesita Redis reactivo. Se agrega Redis a `despliegue/compose/base.yml` y a Coolify con ADR. Alternativa si se rechaza: límite en NGINX (`limit_req`) por réplica, declarado como local. | Pablo (infra) |
| AMB-7 | ¿Estrategia de migración `N-1 → N` (§48)? El repo aplica el esquema completo e idempotente (`aplicar.sql`), no migraciones versionadas. | Se documenta que **no existe** estrategia de migración de datos en vivo y que `aplicar.sql` es `CREATE IF NOT EXISTS`/`DROP+ADD CONSTRAINT`. Se verifica `empty → latest` y `latest → latest` (re-aplicación sin pérdida). Introducir Flyway/Liquibase es `DECISION_REQUIRED`. | Pablo (arquitectura) |
| AMB-8 | RPO/RTO (§50) | Se documentan como **"a definir por operación"**, no como compromiso. | Pablo · operación |
| AMB-9 | Severidad que hace fallar el SCA (§32) | `HIGH` y `CRITICAL` bloquean; `MEDIUM` reporta. Excepciones en `.github/osv-ignore.toml` con motivo y fecha. | Pablo |
| AMB-10 | ¿Qué consumidor real de Kafka usar para la prueba de dedupe si no existe ninguno? | Un consumidor **de prueba** en `comun-mensajeria` (`src/e2eTest`) que usa `Consumidos.registrar`; ningún consumidor productivo nuevo (fuera de alcance). | Pablo |
| AMB-11 | ¿Rama de trabajo? | `pablo/feature/produccion-ready` desde `dev`, PRs pequeños hacia `dev`. Si Pablo prefiere commits directos a `dev`, se cambia en H0.S1.M5. | Pablo |
| AMB-12 | ¿"producción" = perfil Spring `production`? Hoy no hay perfiles. | Se introduce `spring.profiles.active ∈ {local, test, staging, production}` con `application-<perfil>.yml`; la guarda de arranque trata **todo lo que no sea `local`/`test`** como productivo (fail closed). | Pablo |

### 3.1 Decisiones tomadas el 2026-09-21 (cierran las ambigüedades de arriba)

- **AMB-R1** — `test` es espejo fast-forward de `dev` (`git push origin origin/dev:test` tras cada merge); el ruleset mínimo `proteccion-minima` (JSON y comando en el daily del equipo §2, lo aplica Pablo con una línea) bloquea force-push y borrado en `dev`, `test` y `main`
- **AMB-1** — el plan vive en este repo y su copia en `PasanakuBackend/docs/auditoria-produccion/PLAN.md` ya está en `dev` y `test`; el estado vivo va en `carriles/PR<n>-*.md`
- **AMB-2** — JWT step-up RS256 emitido por `identidad`, validado localmente por `nucleo-financiero` con el JWKS; `jti` consumido una sola vez en `nucleo_financiero.evidencia_mfa_consumida`; contrato ya en `dev`: `docs/auditoria-produccion/contratos/step-up-jwt.md`
- **AMB-3** — el factor real de producción es **TOTP (RFC 6238)** verificado dentro de `identidad` desde `factor_mfa.secreto_cifrado` (tipo `TOTP` ya existe en el CHECK): **sin proveedor externo**; SMS/WhatsApp (`factor_mfa.tipo` `SMS`/`WHATSAPP`) salen por el servicio `notificaciones` como segundo canal, con doble en tres niveles solo en `local/test`. El desafío **reutiliza `token_verificacion`** (`tipo_token='OTP'`, `proposito='MFA_RETIRO'`, `politica_id`, `intentos_fallidos`/`max_intentos`, `uso_unico=true`): sin tabla nueva
- **AMB-4** — `POST /billetera/retiros/{ordenId}/aprobacion` con `desenlace ∈ {AUTORIZADA, RECHAZADA}`; permiso nuevo `RETIRO_APROBAR` asignado al rol **`TESORERIA`** del seed (`sql/60_semillas/10-roles-y-permisos.sql`), nunca a `PARTICIPANTE`/`ORGANIZADOR`
- **AMB-5** — siempre `PENDIENTE → AUTORIZADA → EN_PROCESO → PAGADA`; por debajo del umbral la autorización es automática en la misma transacción de creación (`aprobada_por = NULL`); nunca `PENDIENTE → PAGADA`
- **AMB-6** — **Redis entra al stack** (`despliegue/compose/base.yml`, `infra.yml`, Coolify) para `RequestRateLimiter` del gateway; con Redis caído las rutas sensibles deniegan (fail closed); ADR-050 lo registra
- **AMB-7** — este turno sigue con `sql/aplicar.sql` generado (idempotente) y **solo cambios aditivos** al esquema (sin `DROP COLUMN`/`DROP TABLE`/cambio de tipo); se verifica `empty→latest` y `latest→latest` con datos; la adopción de Flyway/Liquibase queda como ADR posterior a la promoción, no se hace ahora
- **AMB-8** — se implementa PITR (base + WAL) y el restore se ejecuta de verdad; el **RPO y el RTO se miden** en ese restore y se reportan como capacidad medida (no como compromiso comercial, que sigue siendo de negocio)
- **AMB-9** — `HIGH` y `CRITICAL` bloquean en OSV y Trivy; `MEDIUM` reporta; toda excepción lleva motivo y fecha de revisión (≤ 30 días)
- **AMB-10** — consumidor Kafka **de prueba** en `comun-mensajeria/src/e2eTest`; ningún consumidor productivo este turno; contrato del envelope ya en `dev`: `docs/auditoria-produccion/contratos/evento-kafka.md`
- **AMB-11** — rama `<persona>/feature/carril-PR<n>-<slug>` desde `dev`, PRs pequeños a `dev` mergeados con `--rebase`, espejo a `test`
- **AMB-12** — perfiles `local`, `test`, `staging`, `production`; `aportaya.entorno.productivo = true` para todo perfil que no sea `local`/`test` (fail closed)
- **Protección de ramas** — durante el turno `dev` **no exige PR ni aprobaciones** (los cinco mergean solos con su gate local); queda **listo** el ruleset mínimo `proteccion-minima` (bloquea force-push y borrado en `dev`, `test` y `main`; el agente no tiene permiso para crearlo, Pablo lo aplica con el comando del daily §2); el ruleset **completo** (PR obligatorio, checks requeridos, CODEOWNERS, 2 aprobaciones en `main`) se activa en la promoción `dev → main` (Pablo H2.S5.M2 lo deja escrito)

Cada decisión es reversible por ADR; ninguna se tomó por conveniencia de implementación: todas eligen la opción que preserva fail closed, invariante 11 (nadie lee la base ajena) y el conflicto cero entre carriles.

## 4. Trazabilidad metaprompt § → hito

| § del metaprompt | Hito/subtarea |
|---|---|
| 2, 6, 87, 88 | H0 |
| 7, 8, 43 (idempotencia), 70 (códigos) | H1 |
| 9, 10, 11, 74, 76, 78 | H2 |
| 12, 89 | H3 |
| 13, 41 (estados), 43 (aprobación) | H4 |
| 17, 18, 19, 51, 52, 71 | H5 |
| 31, 32, 33, 34, 35, 36, 83, 84 | H6 |
| 20–30, 42, 69, 70, 72, 79 | H7 |
| 14, 15, 16, 44, 45 | H8 |
| 38, 39, 40, 41, 68, 73, 75, 77 | H9 |
| 43, 46, 47, 48, 49, 58, 80, 81 | H10 |
| 53, 54, 55, 56 | H11 |
| 50, 57, 64, 65, 66, 67, 82, 85, 86, 90, 92, 95, 96, 97 | H12 |
| 3, 4, 5, 59, 60, 61, 62, 63, 90, 93, 94, 98 | Transversales (§0 de este plan) |

## 5. Hitos

Convención de las tablas: **DoD** = comando literal → salida esperada; la evidencia se guarda como `evidencia/<ID>-<slug>.txt`. Prefijo de commit sugerido entre paréntesis en el nombre de la subtarea.

---

## H0 — Baseline real y reproducible: sé exactamente qué anda y qué no en `19a621e6`

**CA:** Dado el SHA inicial de `origin/dev`, cuando alguien lee `docs/auditoria-produccion/baseline.md`, entonces encuentra versiones, cada comando del gate con su salida literal y su veredicto, la base aplicada desde cero con semillas dos veces, y la tabla A–J revalidada con ruta y línea.
**DoD:** `baseline.md` existe con los 9 comandos del §6.2 ejecutados (o `BLOCKED` con causa), `git rev-parse HEAD` = `19a621e6…` registrado, `python3 scripts/auditar_backend.py --json` guardado como línea base.
**Estado:** TODO

### H0.S1 — Entorno de ejecución y rama de trabajo (`chore(baseline)`)
**CA:** Dado una máquina limpia, cuando se siguen los pasos de `baseline.md` §"Entorno", entonces `./gradlew --version` reporta Gradle 9.7.1 y JVM 21, Docker responde y el repo está en la rama de trabajo.
**DoD:** salidas de M1–M5 pegadas.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H0.S1.M1 | Instalar/apuntar JDK 21 (Temurin) en la máquina de ejecución | `java -version` muestra 21.x | `java -version` → `openjdk version "21…"` | TODO |
| H0.S1.M2 | Clonar `PasanakuBackend` completo (no shallow) con `core.longpaths=true` en `Entrypoint-GitHUb/Pasanaku/PasanakuBackend` | `git status` limpio en `dev` | `git -C … rev-parse HEAD` → `19a621e666afdea5bdc40aced326d3f212a116f4` | TODO |
| H0.S1.M3 | Verificar Docker y PostgreSQL de compose | contenedor `aportaya-postgres` healthy | `docker compose -f despliegue/compose/base.yml --profile base up -d --wait` → exit 0 | TODO |
| H0.S1.M4 | Copiar este plan a `docs/auditoria-produccion/PLAN.md` y crear `evidencia/` | archivo existe en el backend | `test -f docs/auditoria-produccion/PLAN.md && echo OK` → `OK` | TODO |
| H0.S1.M5 | Crear rama `pablo/feature/produccion-ready` desde `dev` (AMB-11) | rama apunta al SHA inicial | `git rev-parse --abbrev-ref HEAD` → nombre de la rama; `git merge-base HEAD origin/dev` → SHA inicial | TODO |

### H0.S2 — Registro del estado inicial (`docs(baseline)`)
**CA:** Dado `baseline.md`, cuando se lee la sección "Estado", entonces están branch, SHA, fecha, Java, Gradle, Docker, PostgreSQL y versiones del catálogo.
**DoD:** `baseline.md` §Estado con las salidas de `git log -1`, `./gradlew --version`, `docker --version`, `docker exec aportaya-postgres postgres --version`, `grep -E '^(spring-boot|jooq|shedlock|resilience4j) ' gradle/libs.versions.toml`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H0.S2.M1 | Escribir `docs/auditoria-produccion/baseline.md` §Estado | contiene las 8 líneas de versiones | `grep -c "" docs/auditoria-produccion/baseline.md` > 0 y revisión visual | TODO |
| H0.S2.M2 | Guardar línea base de la auditoría automática del repo | JSON guardado | `python3 scripts/auditar_backend.py --json > docs/auditoria-produccion/evidencia/H0-auditar-backend-inicial.json` → exit 0 | TODO |

### H0.S3 — Gates del §6.2 ejecutados uno por uno (`docs(baseline)`)
**CA:** Dado cada comando, cuando se ejecuta en serie (regla 70), entonces su salida literal (cola recortada) y su exit code quedan en `evidencia/` y en `baseline.md` con veredicto PASS/FAIL/BLOCKED.
**DoD:** nueve archivos `H0-S3-M*.txt`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H0.S3.M1 | `spotlessCheck` con lista de archivos que fallan | veredicto + lista | `./gradlew spotlessCheck; echo exit=$?` → salida pegada (se espera FAIL; confirma H-4) | TODO |
| H0.S3.M2 | `check -x test` (estático, ArchUnit, sinJpa) | veredicto | `./gradlew check -x test -x jacocoTestCoverageVerification -x jacocoTestReport; echo exit=$?` | TODO |
| H0.S3.M3 | `testBarrido` | veredicto | `./gradlew testBarrido; echo exit=$?` | TODO |
| H0.S3.M4 | `test` (átomos) | veredicto + conteo | `./gradlew test; echo exit=$?` | TODO |
| H0.S3.M5 | `webTest` | veredicto | `./gradlew webTest; echo exit=$?` | TODO |
| H0.S3.M6 | `integrationTest` (Testcontainers), **módulo por módulo** para aislar fallos | veredicto por módulo | `for s in $(ls servicios); do ./gradlew :servicios:$s:integrationTest; done` con exit por módulo | TODO |
| H0.S3.M7 | `contractTest` y `sagaTest` | veredicto | `./gradlew contractTest; ./gradlew sagaTest` | TODO |
| H0.S3.M8 | `e2eTest` sobre `compose --profile todo` (o `BLOCKED` con causa si no hay imágenes) | veredicto | `./gradlew e2eTest; echo exit=$?` | TODO |
| H0.S3.M9 | Clasificar cada rojo (PRODUCT_BUG / TEST_BUG / ENVIRONMENT / DATA / EXTERNAL) con evidencia (regla 80.4) y agregar al plan la microtarea que lo corrige si es de alcance | tabla en `baseline.md` | revisión: cada FAIL tiene clase + hipótesis + microtarea o "fuera de alcance, anotado" | TODO |

### H0.S4 — Base desde cero, semillas dos veces, permisos, jOOQ y OpenAPI (`docs(baseline)`)
**CA:** Dado PostgreSQL vacío, cuando se aplica `sql/aplicar.sql`, se siembra dos veces y se corren las verificaciones, entonces todo termina en exit 0 y la segunda siembra no inserta filas.
**DoD:** salidas de M1–M6.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H0.S4.M1 | Aplicar esquema en base vacía | exit 0, 305 tablas | `docker exec -i aportaya-postgres psql -v ON_ERROR_STOP=1 -U pasanaku -d pasanaku -f - < sql/aplicar.sql; echo exit=$?` y `SELECT count(*) FROM information_schema.tables WHERE table_schema NOT IN ('pg_catalog','information_schema')` | TODO |
| H0.S4.M2 | Semillas mínimas ×2 | segunda corrida: mismo conteo | conteo `SELECT sum(n_live_tup) FROM pg_stat_user_tables` antes/después de la 2.ª corrida → iguales | TODO |
| H0.S4.M3 | Guarda de semillas dev: base sin marcar rechaza; marcada acepta ×2 | primer intento falla; luego 2 corridas iguales | reproducir pasos `ci.yml` job `base` 9–10 con salida | TODO |
| H0.S4.M4 | Permisos por `svc_*` | `verificaciones.sql` sin FALLA | `psql -f sql/50_verificacion/verificaciones.sql` → 0 líneas `FALLA` | TODO |
| H0.S4.M5 | Prueba de humo y concurrencia SQL | 0 FALLA | `psql -f sql/50_verificacion/prueba_humo.sql` y `prueba_concurrencia.sql` | TODO |
| H0.S4.M6 | jOOQ y clientes OpenAPI generan | exit 0 | `./gradlew generateJooq` y `./gradlew generateOpenApiClients --no-parallel --no-build-cache` | TODO |

### H0.S5 — Tabla A–J revalidada y defectos nuevos (`docs(baseline)`)
**CA:** Dado `baseline.md`, cuando se lee §"Hallazgos", entonces la tabla §2.4 de este plan está copiada con cualquier corrección que la ejecución haya revelado, más los defectos nuevos hallados con ruta.
**DoD:** sección escrita; cada fila cita archivo:línea.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H0.S5.M1 | Copiar y ajustar la tabla A–J a `baseline.md` | 10 filas con evidencia | revisión | TODO |
| H0.S5.M2 | Resolver los desconocidos §2.2 con comando y pegar en `baseline.md` §"Desconocidos resueltos" | cada uno con salida | revisión: 8 ítems resueltos o `BLOCKED` | TODO |
| H0.S5.M3 | Checkpoint de cierre de H0 en `PROGRESS`/plan | avance calculado | `python .claude/hooks/plan_status.py` (en este repo) → línea de avance | TODO |

---

## H1 — Idempotencia con exactamente el scope del índice único

**CA:** Dado dos usuarios U1 y U2 y una misma clave K, cuando cada uno envía su operación con K, entonces cada uno obtiene su propio resultado; cuando U1 reenvía K con el mismo cuerpo obtiene la misma respuesta sin efecto nuevo; cuando U1 reenvía K con otro cuerpo obtiene `409` con código estable; y 50 reenvíos simultáneos producen un solo efecto financiero.
**DoD:** `./gradlew :plataforma:comun-web:integrationTest :servicios:nucleo-financiero:integrationTest :servicios:aportes:integrationTest` en verde con los tests nuevos listados; ADR escrito; `security-guardrails` y `data-privacy-financial` revisados (la respuesta almacenada no cruza usuarios).
**Estado:** TODO

### H1.S1 — Helper genérico `Idempotencia` (`fix(idempotency): scope keys by user and operation`)
**CA:** Dado `(U,O,K,H)`, cuando no existe → reserva y ejecuta; existe con `H` igual → devuelve la respuesta previa; existe con `H` distinto → `409`; expirado → se trata como nuevo (registro previo se reemplaza).
**DoD:** `./gradlew :plataforma:comun-web:integrationTest --tests '*IdempotenciaRepositorioTest*'` → 10 tests PASS.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Escribir `IdempotenciaRepositorioTest` (integrationTest, Testcontainers, esquema `nucleo_financiero`) con los 10 casos **en rojo**: clave nueva; replay mismo hash; misma clave/cuerpo distinto → 409; misma clave otro usuario; misma clave otra operación; 50 hilos simultáneos → 1 reserva; expirada → nueva; rollback no deja reserva; fallo después de reservar → reintento posible; reintento tras error transitorio | 10 tests existen y fallan por la causa correcta | `./gradlew :plataforma:comun-web:integrationTest --tests '*IdempotenciaRepositorioTest*'` → FAIL con aserciones, no con errores de compilación | TODO |
| H1.S1.M2 | `exigirNueva`: `SELECT` por `(usuario_id, operacion, clave_idempotencia)` | test "otro usuario" y "otra operación" pasan | mismo comando → esos 2 PASS | TODO |
| H1.S1.M3 | Comparar `hash_solicitud`: igual → `OperacionRepetida` con respuesta; distinto → `ErrorDeNegocio` con código nuevo `IDEMPOTENCIA_CONFLICTO` (nombre según convención `CodigoError.de(...)` + catálogo `erroresCatalogo`) mapeado a `409` en `ManejadorGlobalDeErrores` | test "cuerpo distinto" pasa con 409 | mismo comando → PASS; `./gradlew :plataforma:comun-web:webTest --tests '*ManejadorGlobalDeErroresWebTest*'` PASS | TODO |
| H1.S1.M4 | `guardarRespuesta(dsl, ctx, operacion, clave, …)` actualiza por la identidad completa | test "replay" devuelve exactamente la respuesta guardada por ese usuario | mismo comando → PASS | TODO |
| H1.S1.M5 | Expiración: registro con `expira_en < now()` se elimina/reemplaza al reservar; `VIGENCIA` configurable (`aportaya.idempotencia.vigencia`, default PT24H) sin literal | test "expirada" pasa | mismo comando → PASS; `./gradlew :plataforma:comun-web:testBarrido` (SinUmbralLiteral) PASS | TODO |
| H1.S1.M6 | Reserva en estado `202` + respuesta `{}` no se sirve como replay: si la fila está "en proceso" (sin respuesta final) → `409 EN_PROCESO` (comportamiento actual `:77-81` se conserva y se testea) | test concurrencia: 1 efecto, 49 × 409 | mismo comando → PASS | TODO |
| H1.S1.M7 | Tabla `respuesta_idempotente` disponible en los esquemas que usen el helper: agregar al generador (`scripts/modelo.py` / `.puml`) para los esquemas donde se adopte en H1.S5, regenerar `sql/` con `generar_ddl.py`, grants | `git diff` de `sql/` sale del generador | `python3 scripts/generar_ddl.py && git diff --stat sql/`; `./gradlew erroresCatalogo` PASS | TODO |

### H1.S2 — Ledger: lookup con la identidad de `uq_tx_idem` (`fix(wallet): scope ledger idempotency lookup`)
**CA:** Dado dos usuarios que inician una transferencia con la misma clave, cuando ambos la envían, entonces se registran dos transacciones distintas y cada uno recibe la suya.
**DoD:** `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*CU12*'` PASS incluyendo el caso nuevo.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Test rojo en `CU12Test`: misma clave, dos `ContextoSesion` distintos → dos transacciones | falla hoy (devuelve la de U1) | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*CU12Test*'` → 1 FAIL esperado | TODO |
| H1.S2.M2 | `LibroDeBilletera.porClaveIdempotencia(dsl, iniciadaPor, origenTipo, clave)` con `COALESCE` idéntico al índice; actualizar `CU12` y `CU14` | test verde | mismo comando → PASS | TODO |
| H1.S2.M3 | Test replay legítimo: mismo usuario, misma clave → misma `transaccionId`, saldo sin cambio | PASS | mismo comando | TODO |

### H1.S3 — Retiro y recarga: lookup por `(cuenta_billetera_id, clave)` (`fix(wallet): scope withdrawal/topup idempotency`)
**CA:** Dado una orden de retiro de la cuenta A con clave K, cuando la cuenta B pide un retiro con K, entonces se crea una orden nueva para B y A no se expone.
**DoD:** `--tests '*CU11*'` y `'*CU10*'` PASS con casos nuevos.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | Tests rojos en `CU11Test` y `CU10Test`: misma clave, otra cuenta → orden distinta; misma clave, misma cuenta → misma orden | 2 FAIL hoy | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*CU11Test*' --tests '*CU10Test*'` | TODO |
| H1.S3.M2 | `OrdenRetiroRepositorio.porClaveIdempotencia(dsl, cuentaId, clave)` y `OrdenRecargaRepositorio` ídem; adaptar `CU11:85`, `CU10:82` | PASS | mismo comando | TODO |
| H1.S3.M3 | Verificar que el replay de retiro devuelve el `costo` **almacenado** (`monto_neto`/`costo_retiro`) y no el `entrada.costo()` recotizado (`CU11:88-93`) | test: replay con costo distinto en la entrada devuelve el costo original | mismo comando | TODO |

### H1.S4 — Barrido repo-wide de lecturas de idempotencia (`fix(idempotency): align every lookup with its unique index`)
**CA:** Dado cada índice único que incluye `clave_idempotencia` (§2.1), cuando se busca su lectura Java, entonces la lectura filtra por las mismas columnas o no existe lectura (solo `ON CONFLICT`).
**DoD:** tabla `docs/auditoria-produccion/idempotencia-scope.md` (índice → clase:línea → veredicto) y test de barrido que falla ante `where(DSL.field("clave_idempotencia").eq(` sin otra condición.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S4.M1 | Grep repo-wide (`clave_idempotencia`, `Idempotency-Key`, `porClaveIdempotencia`, `idempoten`) y tabla índice→lectura | tabla completa (≥ 12 índices) | archivo escrito + `grep -rn "porClaveIdempotencia" --include=*.java servicios plataforma` pegado | TODO |
| H1.S4.M2 | `aportes/PagoRepositorio.porClaveIdempotencia` por `(obligacion_id, clave)` (`uq_pago_idem`) + test rojo→verde en `CU21` | PASS | `./gradlew :servicios:aportes:integrationTest --tests '*CU21*'` | TODO |
| H1.S4.M3 | Cualquier otro caso hallado en M1 → microtarea nueva `H1.S4.Mx` con su test (se agregan al plan al descubrirlos) | 0 lecturas fuera de scope | tabla sin filas `FUERA DE SCOPE` | TODO |
| H1.S4.M4 | Regla de barrido en `comun-pruebas/Barrido` (o `ReglasPropiasTest`): `.where(DSL.field("clave_idempotencia")` debe ir acompañado de otra condición en la misma cadena | `testBarrido` falla si se reintroduce | `./gradlew testBarrido` PASS; prueba negativa temporal (introducir y revertir) pegada | TODO |

### H1.S5 — Concurrencia real y adopción del helper (`test(idempotency): 50 concurrent replays`)
**CA:** Dado 50 peticiones simultáneas HTTP con la misma `Idempotency-Key` sobre `POST /billetera/transferencias`, cuando terminan, entonces hay exactamente 1 `transaccion_billetera` nueva y las demás respuestas son `201` idénticas o `409 EN_PROCESO`.
**DoD:** `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*CU12ConcurrenciaTest*'` PASS con conteo pegado.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S5.M1 | `CU12ConcurrenciaTest` (patrón de `CU10ConcurrenciaTest`): 50 hilos, misma clave, misma cuenta | 1 transacción, saldo total preservado | comando → PASS + salida con conteos | TODO |
| H1.S5.M2 | Decidir y registrar si `Idempotencia` (helper) se adopta en los 7 CU que lo comentan (`grupos`, `identidad/CU09`) o si se elimina el código muerto: **decisión** = adoptar solo donde la operación tiene efecto financiero o irreversible; el resto se anota en H11 | ADR o nota en `idempotencia-scope.md` | archivo actualizado | TODO |
| H1.S5.M3 | `docs/Arquitectura/ADR-046 Alcance de la idempotencia.md` (contexto, decisión, alternativas, consecuencias, riesgos) + enlace en `_Arquitectura.md`; `python3 scripts/verificar_boveda.py` sigue en verde | ADR existe | `python3 scripts/verificar_boveda.py` → exit 0 | TODO |
| H1.S5.M4 | Regresión del módulo | verde | `./gradlew :servicios:nucleo-financiero:webTest :servicios:nucleo-financiero:integrationTest` PASS; `spotlessCheck` PASS | TODO |

---

## H2 — El outbox publica de verdad en Kafka, al menos una vez, sin perder ni duplicar

**CA:** Dado un caso de uso que emite un evento por outbox, cuando la transacción hace commit, entonces en menos de 5 s el evento está en el tema `aportaya.<tipo>` con su `event_id`, la fila queda `PUBLICADO`, y si el mismo mensaje se entrega dos veces el consumidor produce el efecto una sola vez; con Kafka apagado el evento queda `PENDIENTE` y se publica al volver.
**DoD:** `./gradlew :plataforma:comun-mensajeria:integrationTest` (Testcontainers PostgreSQL + Kafka) y `:plataforma:comun-mensajeria:e2eTest` en verde; kill-test del plan en verde; métricas `aportaya.outbox.*` visibles en `/actuator/prometheus` de un servicio levantado; ADR escrito.
**Estado:** TODO

### H2.S1 — `Relevo` existe en el contexto de cada productor (`fix(outbox): register relay, scheduling and lock provider`)
**CA:** Dado cualquier servicio con `aportaya.esquema` y Kafka configurado, cuando arranca, entonces existe un bean `Relevo`, el scheduler está habilitado y ShedLock tiene `LockProvider` sobre `<esquema>.shedlock`.
**DoD:** `./gradlew integrationTest --tests '*ArranqueTest*'` en los 14 servicios PASS con aserción nueva `contexto.getBean(Relevo.class)`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Test rojo `RelevoConfiguracionTest` (comun-mensajeria, `@SpringBootTest` mínimo con PostgreSQL Testcontainers y `KafkaTemplate` de prueba): exige bean `Relevo`, `LockProvider`, y que `@Scheduled` esté activo (`ScheduledAnnotationBeanPostProcessor` presente) | 3 aserciones FAIL hoy | `./gradlew :plataforma:comun-mensajeria:integrationTest --tests '*RelevoConfiguracionTest*'` → FAIL | TODO |
| H2.S1.M2 | `ConfiguracionMensajeria` (`@Configuration`, `@EnableScheduling`, `@EnableSchedulerLock(defaultLockAtMostFor="PT30S")`) en `comun-mensajeria`, registrada vía `ConfiguracionComunWeb` (`@Import`) o `AutoConfiguration.imports`, siguiendo el patrón `@ConditionalOnMissingBean` de `ConfiguracionComunWeb:26-77`; bean `LockProvider` = `JdbcTemplateLockProvider` con `withTableName("<esquema>.shedlock")` (verificar API de ShedLock 6.9.0 en su README antes de escribir) | test M1 PASS | mismo comando → PASS | TODO |
| H2.S1.M3 | Bean `Relevo` condicionado a `aportaya.outbox.habilitado` (default `true`) y a la presencia de `KafkaTemplate`; en `webTest`/`test` no se levanta | `ArranqueTest` de los 14 servicios PASS; `webTest` sin Kafka PASS | `./gradlew integrationTest --tests '*ArranqueTest*'`; `./gradlew webTest` | TODO |
| H2.S1.M4 | Quitar el `@EnableScheduling` duplicado de `cumplimiento/Aplicacion.java:20` solo si la configuración común lo cubre (test de CU-54 sigue verde) | CU-54 programado sigue activo | `./gradlew :servicios:cumplimiento:integrationTest` PASS | TODO |
| H2.S1.M5 | Permisos: `svc_*` puede `INSERT/UPDATE/DELETE` sobre su `shedlock` (verificar en `sql/00_base/03_permisos.sql`; si falta, agregar por generador) | ShedLock adquiere el lock con rol `svc_nucleo_financiero` | `psql -U svc_nucleo_financiero -c "INSERT INTO nucleo_financiero.shedlock …"` → 1 fila (o test de integración con ese rol) | TODO |

### H2.S2 — Prueba de punta a punta: caso de uso → outbox → Kafka → consumidor → `PUBLICADO` (`test(outbox): end-to-end publish and consume`)
**CA:** Dado `CU12TransferirSaldo` ejecutado, cuando pasan ≤ 5 s, entonces un consumidor de prueba recibe en `aportaya.nucleo_financiero.transferencia_realizada` (nombre real según `EventoDominio.tema()`) un mensaje cuyo `event_id` = `evento_dominio.id`, la fila está `PUBLICADO` con `publicado_en` no nulo, y una segunda entrega del mismo `event_id` no repite el efecto.
**DoD:** `./gradlew :servicios:nucleo-financiero:e2eTest --tests '*OutboxE2ETest*'` PASS (Testcontainers `KafkaContainer` + PostgreSQL), salida pegada.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S2.M1 | Fixture Kafka en `comun-pruebas` (`BaseDePrueba.kafka()` singleton con `org.testcontainers.kafka.KafkaContainer`, imagen `apache/kafka` fijada; verificar el nombre de clase en la versión del BOM antes de usarla) | contenedor levanta una vez por JVM | `./gradlew :plataforma:comun-pruebas:test` PASS | TODO |
| H2.S2.M2 | `OutboxE2ETest` en rojo: ejecuta CU-12, espera con `Awaitility` (o polling con `Thread.onSpinWait` acotado, sin `sleep` fijo) a `estado='PUBLICADO'`, consume con `KafkaConsumer` y compara `event_id` | FAIL hoy (queda PENDIENTE) | `./gradlew :servicios:nucleo-financiero:e2eTest --tests '*OutboxE2ETest*'` → FAIL por timeout | TODO |
| H2.S2.M3 | Cabeceras Kafka del envelope (§10): `event_id`, `type`, `version`, `occurred_at`, `producer`, `correlation_id`, `causation_id`, `trace_id` puestas por `Relevo` desde las columnas de `evento_dominio`/`metadatos` | test verifica las 8 cabeceras | mismo comando → PASS | TODO |
| H2.S2.M4 | Consumidor de prueba (AMB-10) que aplica `Consumidos.registrar` y cuenta efectos; test entrega el mismo mensaje dos veces → 1 efecto | PASS | mismo comando + `./gradlew :plataforma:comun-mensajeria:integrationTest --tests '*ConsumidosTest*'` | TODO |
| H2.S2.M5 | Test de que ninguno de los 14 esquemas queda sin relevo: `OutboxE2ETest` parametrizado o `ArranqueTest` de cada servicio exige `Relevo` (ya en H2.S1.M3) | 14 PASS | `./gradlew integrationTest --tests '*ArranqueTest*'` | TODO |

### H2.S3 — Menos bloqueo: tomar, publicar fuera de la transacción, marcar (`fix(outbox): claim-publish-mark with backoff`)
**CA:** Dado 100 eventos pendientes y Kafka lento (latencia inyectada 2 s), cuando el relevo corre, entonces ninguna transacción de PostgreSQL permanece abierta durante la llamada a Kafka; ante fallo, `intentos` sube, `ultimo_error` y `proximo_intento_en` se registran con backoff exponencial + jitter, y tras `N` intentos el evento pasa a `FALLIDO` (DLQ lógica) con métrica.
**DoD:** `RelevoTest` (integrationTest) PASS con los 6 escenarios; `pg_stat_activity` durante la latencia inyectada sin transacción `idle in transaction` del relevo (consulta pegada).
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S3.M1 | Columnas nuevas en `evento_dominio` vía generador: `tomado_en TIMESTAMPTZ`, `tomado_por VARCHAR(80)`, `ultimo_error TEXT`, `proximo_intento_en TIMESTAMPTZ`; estado `TOMADO` en el CHECK; `GRANT UPDATE` ampliado a esas columnas en `03_permisos.sql` (generado) | `generar_ddl.py` sin diff residual; `aplicar.sql` en limpio | `python3 scripts/generar_ddl.py && git diff --exit-code sql/ || true` (diff = solo lo generado); `psql -f sql/aplicar.sql` exit 0 | TODO |
| H2.S3.M2 | `RelevoTest` rojo con 6 escenarios: publica OK → PUBLICADO; Kafka falla → PENDIENTE con `intentos=1`, `proximo_intento_en > now()`; respeta `proximo_intento_en` (no reintenta antes); `N` fallos → FALLIDO; `TOMADO` huérfano (> `lockAtMostFor`) se recupera; dos relevos concurrentes no publican dos veces (`SKIP LOCKED`) | 6 FAIL hoy | `./gradlew :plataforma:comun-mensajeria:integrationTest --tests '*RelevoTest*'` | TODO |
| H2.S3.M3 | Refactor de `Relevo.relevar`: tx1 `UPDATE … SET estado='TOMADO', tomado_en, tomado_por WHERE id IN (SELECT … FOR UPDATE SKIP LOCKED)` y commit; publicación fuera de tx con `send().get(timeout)`; tx2 `marcar(PUBLICADO)` o `marcar(fallo, backoff)`; sin `@Transactional` en el método externo | 6 PASS | mismo comando → PASS | TODO |
| H2.S3.M4 | Backoff configurable sin literales: `aportaya.outbox.{intentos-maximos, backoff-base, backoff-tope, timeout-publicacion}` en `application.yml` de los 14 (vía plantilla `scripts/nuevo_servicio.py` para que no diverjan) | `SinUmbralLiteral` PASS | `./gradlew testBarrido` PASS | TODO |
| H2.S3.M5 | Verificar en vivo que no hay `idle in transaction` del relevo con latencia inyectada (Toxiproxy de Testcontainers o `KafkaTemplate` decorado con `Thread.sleep` en test) | consulta `SELECT state, query FROM pg_stat_activity WHERE state LIKE 'idle in transaction%'` vacía para el relevo | salida pegada | TODO |

### H2.S4 — Resiliencia del outbox: Kafka caído y reinicio (`test(outbox): kafka down and restart scenarios`)
**CA:** Dado Kafka apagado, cuando se ejecuta una operación financiera, entonces el commit ocurre, el evento queda `PENDIENTE` y se publica al reencender Kafka; dado un reinicio del servicio entre "publicado" y "marcado", cuando vuelve, entonces el evento se republica y el consumidor no duplica.
**DoD:** `OutboxResilienciaE2ETest` PASS (3 escenarios) con salida.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S4.M1 | Escenario Kafka caído (`kafka.stop()` del contenedor): CU-12 termina 201, fila PENDIENTE, readiness del servicio = DOWN, liveness = UP | 4 aserciones PASS | `./gradlew :servicios:nucleo-financiero:e2eTest --tests '*OutboxResilienciaE2ETest*'` | TODO |
| H2.S4.M2 | Escenario Kafka vuelve (`kafka.start()`): evento PUBLICADO ≤ backoff máximo | PASS | mismo comando | TODO |
| H2.S4.M3 | Escenario reinicio entre publicar y marcar (simulado: publicar manualmente y dejar `TOMADO`): al siguiente relevo se republica; consumidor con `Consumidos` → 1 efecto | PASS | mismo comando | TODO |
| H2.S4.M4 | Métricas (§38/§68): `aportaya.outbox.pendientes`, `aportaya.outbox.fallidos`, `aportaya.outbox.publicaciones_total{resultado}`, además de `edad_mas_viejo_segundos` | aparecen en `/actuator/prometheus` | `curl -s localhost:8080/actuator/prometheus | grep aportaya_outbox` (servicio levantado con compose) → 4 métricas | TODO |
| H2.S4.M5 | `docs/Arquitectura/ADR-047 Semántica de entrega del outbox.md` (at-least-once, envelope, TOMADO, backoff, FALLIDO) y actualización de ADR-018/027 con enlace | bóveda verde | `python3 scripts/verificar_boveda.py` exit 0 | TODO |

---

## H3 — MFA real (step-up) para retirar: sin bypass posible en producción

**CA:** Dado un titular autenticado, cuando pide un retiro sin evidencia step-up, con evidencia inválida, expirada, de otro usuario, de otro propósito o ya consumida, entonces recibe `MFA_REQUERIDO`/`MFA_INVALIDO` y no se crea orden; cuando presenta evidencia válida emitida por `identidad` para `RETIRO`, entonces la orden se crea con `mfa_verificado=true`. En un proceso con perfil distinto de `local`/`test`, el adaptador local **no existe** y sin adaptador real el servicio no arranca.
**DoD:** `./gradlew :servicios:identidad:integrationTest :servicios:identidad:webTest :servicios:nucleo-financiero:integrationTest :servicios:nucleo-financiero:webTest` PASS con los tests de H3; `ArranqueProduccionTest` demuestra el fallo de arranque; gate `security-guardrails` + `authn-identity` revisado; ADR escrito.
**Estado:** TODO

### H3.S1 — Confinar el bypass a `local`/`test` y fallar cerrado (`fix(wallet): confine local MFA adapter to local/test profiles`)
**CA:** Dado el perfil `production`, cuando arranca `nucleo-financiero` sin un `SegundoFactor` real, entonces el proceso no levanta y el error nombra la propiedad faltante; dado el perfil `local`, el adaptador local sigue disponible pero **con `exigido=true` deniega** (sin cambio).
**DoD:** `ArranqueProduccionTest` (perfil `production`, sin adaptador) → contexto falla con mensaje; `ArranqueTest` (perfil `test`) PASS.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Introducir perfiles: `application-local.yml`, `application-test.yml` (usados por `BaseDePrueba`/`ArranqueTest`), `application-production.yml` (vacío salvo lo que endurece) en `nucleo-financiero` e `identidad`; el resto en H5.S5 | `ArranqueTest` activa `test` | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*ArranqueTest*'` PASS | TODO |
| H3.S1.M2 | `@Profile({"local","test"})` en `SegundoFactorLocal` (patrón `DesafioDeDesarrollo:32`) | bean ausente en `production` | `ArranqueProduccionTest` (nuevo, `@ActiveProfiles("production")`) falla con `NoSuchBeanDefinitionException: SegundoFactor` **hasta** H3.S4 | TODO |
| H3.S1.M3 | Eliminar `aportaya.mfa.exigido` como interruptor de bypass: el adaptador local deniega siempre salvo `aportaya.mfa.doble-local=true` **solo leído bajo perfil local/test**; test de que la propiedad no tiene efecto en `production` | test PASS | `./gradlew :servicios:nucleo-financiero:webTest --tests '*BilleteraControllerWebTest*'` | TODO |
| H3.S1.M4 | Regla en `scripts/verificar_seguridad.py`: prohibido `@Component` sin `@Profile` en clases `*Local` de `infraestructura/` | script falla si se reintroduce | `python3 scripts/verificar_seguridad.py` exit 0; prueba negativa pegada | TODO |

### H3.S2 — `identidad`: challenge de propósito `RETIRO` (`feat(identity): purpose-bound MFA challenge`)
**CA:** Dado un usuario autenticado con factor enrolado, cuando pide `POST /sesiones/desafios` con `proposito=RETIRO`, entonces recibe `desafioId` + `expiraEn` (≤ 5 min) y el OTP sale por su canal; sin factor enrolado → `FACTOR_NO_ENROLADO`; más de N intentos → `DEMASIADOS_INTENTOS`.
**DoD:** `CU04DesafioTest` (integrationTest) 5 casos PASS; `SesionesControllerWebTest` con 401/403/400 PASS; contrato validado y clientes regenerados.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S2.M1 | Contrato OpenAPI `identidad.yaml`: `POST /sesiones/desafios` (crear), `POST /sesiones/desafios/{desafioId}/verificacion` (verificar → evidencia), esquemas `EntradaDesafio{proposito enum [RETIRO, CAMBIO_CUENTA, ADMIN]}`, `SalidaDesafio`, `EntradaVerificacion{factor: FactorPresentado}`, `SalidaEvidencia{evidencia, expiraEn, jti}`; errores 401/403/409/422; `Idempotency-Key` en ambos | `generateOpenApiClients` exit 0 | `./gradlew :servicios:identidad:generarClientes` (o `generateOpenApiClients`) exit 0 | TODO |
| H3.S2.M2 | Persistencia: reutilizar `token_verificacion` (ya tiene `usuario_id`, `clave_idempotencia`, hash con pimienta) con `proposito` nuevo, o tabla `desafio_mfa` si el modelo lo separa — **decidir leyendo `docs/Modelos/Entidades/01…/token_verificacion.md` y `politica_token`**; cambio por generador | `generar_ddl.py` sin diff residual | `python3 scripts/generar_ddl.py; python3 scripts/verificar_boveda.py` exit 0 | TODO |
| H3.S2.M3 | `CU04CrearDesafio` (aplicación): valida factor enrolado, crea token hasheado con pimienta, TTL desde `politica_token`, límite de intentos, envía OTP por el puerto existente de notificación; **doble local en tres niveles** para el proveedor OTP (correcto / límite / inválido) declarado `@Profile({"local","test"})` | tests rojos → verdes | `./gradlew :servicios:identidad:integrationTest --tests '*CU04DesafioTest*'` | TODO |
| H3.S2.M4 | Controlador + `@Permiso`/autenticado + `SabanaDeSeguridadWeb` cubre las 2 rutas | 401/403/400 PASS | `./gradlew :servicios:identidad:webTest` PASS | TODO |
| H3.S2.M5 | Auditoría append-only del challenge (creado, verificado, fallido) sin OTP en logs (`data-privacy-financial`) | bitácora con `usuario_id`, `desafio_id`, `resultado`; grep de logs sin el OTP | test + `grep -rn "otp\|factor" build/…/logs` vacío de valores | TODO |

### H3.S3 — `identidad` emite evidencia step-up y `nucleo-financiero` la valida localmente (`feat(identity): issue step-up evidence token`)
**CA:** Dado un desafío verificado, cuando `identidad` emite la evidencia, entonces es un JWT RS256 con `sub`, `jti`, `iat`, `exp ≤ 5 min`, `proposito`, `desafio_id`, `acr=mfa`, `aud=nucleo-financiero`, `iss` y `kid`; dado ese JWT, cuando `nucleo-financiero` lo valida con su JWKS, entonces acepta solo si `sub` = usuario de la sesión, `proposito=RETIRO`, no vencido, `aud` correcto y `jti` no consumido.
**DoD:** `EvidenciaStepUpTest` (identidad, 4 casos) + `SegundoFactorStepUpTest` (nucleo, 7 casos) PASS.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S3.M1 | `EmisorDeEvidencia` en identidad (reusa la `RSAKey` de `EmisorDeAcceso`, misma `kid`), claims según CA; `Clock` inyectable (`Reloj`) en vez de `Instant.now()` (§71) | 4 tests PASS (claims, exp, aud, firma) | `./gradlew :servicios:identidad:integrationTest --tests '*EvidenciaStepUpTest*'` | TODO |
| H3.S3.M2 | Tabla `nucleo_financiero.evidencia_mfa_consumida (jti PK, usuario_id, proposito, consumida_en)` por generador; grants `INSERT` a `svc_nucleo_financiero`; sin `UPDATE/DELETE` | `aplicar.sql` en limpio | `psql -f sql/aplicar.sql` exit 0; `verificaciones.sql` sin FALLA | TODO |
| H3.S3.M3 | `SegundoFactorStepUp implements SegundoFactor` (perfil ≠ local/test por omisión, también activable en test) usando `JwtDecoder` común + validadores (`aud`, `proposito`, `sub`, `acr`); consumo one-shot del `jti` con `INSERT … ON CONFLICT DO NOTHING` (patrón `Consumidos`) **dentro** de la transacción del CU-11 para que el consumo revierta con la orden | 7 tests: sin evidencia, inválida, expirada, de otro usuario, otro propósito, ya consumida → rechazo; válida → acepta | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*SegundoFactorStepUpTest*'` | TODO |
| H3.S3.M4 | `BilleteraController.solicitarRetiro`: `EntradaRetiro.factorMfa` pasa a llevar la evidencia (renombrar en contrato a `evidenciaMfa`, compatibilidad: aceptar ambos hasta retiro declarado, §63/96.4); regenerar clientes | contrato válido; `webTest` PASS | `./gradlew generateOpenApiClients`; `./gradlew :servicios:nucleo-financiero:webTest` | TODO |
| H3.S3.M5 | Códigos estables: `MFA_REQUERIDO` (ya `CodigoError.de(11,2)`), `MFA_INVALIDO` nuevo; catálogo `erroresCatalogo` y `ManejadorGlobalDeErrores` → 403/422 según convención existente | `erroresCatalogo` PASS | `./gradlew erroresCatalogo` | TODO |

### H3.S4 — Arranque y cierre del hito (`feat(identity): add withdrawal step-up MFA`)
**CA:** Dado perfil `production` con `SegundoFactorStepUp` disponible, cuando arranca, entonces levanta; dado perfil `production` con `aportaya.mfa.doble-local=true`, entonces **no** levanta (guarda §52).
**DoD:** `ArranqueProduccionTest` PASS (2 casos); regresión de identidad y núcleo verde; ADR.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S4.M1 | `ArranqueProduccionTest` en nucleo-financiero: (a) `production` + step-up → UP; (b) `production` + `doble-local=true` → falla con mensaje | 2 PASS | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*ArranqueProduccionTest*'` | TODO |
| H3.S4.M2 | E2E de retiro completo con evidencia (compose `todo`): challenge → verificación (doble OTP `local`) → retiro 201 con `mfa_verificado=true` | PASS | `./gradlew :servicios:nucleo-financiero:e2eTest --tests '*RetiroConMfaE2ETest*'` | TODO |
| H3.S4.M3 | `ADR-048 MFA step-up para operaciones sensibles.md` + actualización de `docs/Seguridad.md` (control, comando que lo comprueba) | bóveda y seguridad verdes | `python3 scripts/verificar_boveda.py; python3 scripts/verificar_seguridad.py` | TODO |
| H3.S4.M4 | Regresión de los dos módulos | verde | `./gradlew :servicios:identidad:webTest :servicios:identidad:integrationTest :servicios:nucleo-financiero:webTest :servicios:nucleo-financiero:integrationTest` | TODO |

---

## H4 — Doble aprobación de retiros en la aplicación (four-eyes), no solo en la base

**CA:** Dado un retiro con monto ≥ umbral, cuando se crea, entonces queda `EN_REVISION`; cuando el solicitante intenta aprobarlo → `RETIRO_AUTOAPROBACION_PROHIBIDA`; cuando alguien sin `RETIRO_APROBAR` intenta → `403`; cuando un aprobador distinto aprueba → `AUTORIZADA` con `aprobada_por`; cuando dos aprobadores aprueban a la vez → una sola transición; cuando se intenta pagar una orden que requiere aprobación y no está `AUTORIZADA` → `RETIRO_APROBACION_REQUERIDA`. Dado monto < umbral → autorización automática (AMB-5).
**DoD:** `CU11AprobacionTest` (11 casos del §13) + `CU11AprobacionConcurrenciaTest` PASS; `BilleteraControllerWebTest` con el endpoint nuevo (201/403/409/404) PASS; ADR.
**Estado:** TODO

### H4.S1 — Máquina de estados explícita (`feat(wallet): explicit withdrawal state machine`)
**CA:** Dado los estados del CHECK (`orden_retiro.sql:28`), cuando se modela `EstadoDeRetiro` en dominio, entonces las únicas transiciones válidas son `PENDIENTE→EN_REVISION|AUTORIZADA|RECHAZADA`, `EN_REVISION→AUTORIZADA|RECHAZADA`, `AUTORIZADA→EN_PROCESO|RECHAZADA`, `EN_PROCESO→PAGADA|RECHAZADA`, `PAGADA→REVERSADA`; toda otra → error de dominio.
**DoD:** `EstadoDeRetiroTest` (átomo, `test`) PASS con tabla de transiciones; `pasarA` sigue condicionando por estado previo.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | `EstadoDeRetiro` (enum + `puedePasarA`) en `nucleo-financiero/dominio` con test de todas las transiciones (válidas e inválidas) | test PASS | `./gradlew :servicios:nucleo-financiero:test --tests '*EstadoDeRetiroTest*'` | TODO |
| H4.S1.M2 | `CU11.solicitar`: estado inicial `EN_REVISION` si `requiereDobleAprobacion`, si no `AUTORIZADA` (AMB-5); `SalidaRetiro.estado` refleja | `CU11Test` casos: umbral−0.01 → AUTORIZADA; umbral exacto → EN_REVISION; umbral+1 → EN_REVISION | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*CU11Test*'` | TODO |
| H4.S1.M3 | `confirmarPago` exige `EN_PROCESO` (o `AUTORIZADA` → `EN_PROCESO` al instruir al proveedor) y nunca desde `PENDIENTE`/`EN_REVISION`; el `UPDATE` lleva la precondición (regla 96.3.2) | test: pagar `EN_REVISION` → `RETIRO_APROBACION_REQUERIDA`; base rechaza además por `ck_retiro_doble_aprobacion` | mismo comando | TODO |

### H4.S2 — Caso de uso y endpoint de aprobación (`feat(wallet): enforce four-eyes withdrawal approval`)
**CA:** Dado `POST /billetera/retiros/{ordenId}/aprobacion` con `desenlace ∈ {AUTORIZADA, RECHAZADA}` y `Idempotency-Key`, cuando lo llama un usuario con `RETIRO_APROBAR` distinto del solicitante, entonces la orden transiciona y se audita; solicitante → `409 RETIRO_AUTOAPROBACION_PROHIBIDA`; sin permiso → `403`; orden inexistente → `404`; ya resuelta → `409`.
**DoD:** `CU11AprobacionTest` 8 casos + `BilleteraControllerWebTest` 5 casos PASS.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.S2.M1 | Permiso `RETIRO_APROBAR` en `sql/60_semillas/10-roles-y-permisos.sql` (vía su generador/seed idempotente) asignado a rol de backoffice existente (localizar rol en el seed; **no** a PARTICIPANTE/ORGANIZADOR) | seed ×2 idéntico | `psql -f sql/60_semillas/sembrar.sql` ×2 → mismo conteo | TODO |
| H4.S2.M2 | Contrato: ruta `/billetera/retiros/{ordenId}/aprobacion`, `EntradaAprobacionRetiro{desenlace, motivo?}`, `SalidaAprobacionRetiro{ordenRetiroId, estado, aprobadaPor}`; errores 401/403/404/409; ejemplo | clientes generan | `./gradlew generateOpenApiClients` exit 0 | TODO |
| H4.S2.M3 | `CU11.aprobar/rechazarRevision` (tests rojos primero): bloquea la orden, verifica `aprobador != solicitada_por`, permiso vía `@Permiso("RETIRO_APROBAR")` en el controller **y** ownership/segregación en el CU; `UPDATE … WHERE estado='EN_REVISION'`; outbox `retiro_autorizado`/`retiro_rechazado`; auditoría con actor, target, correlación | 8 PASS | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*CU11AprobacionTest*'` | TODO |
| H4.S2.M4 | Controller + `SabanaDeSeguridadWeb` + `BilleteraControllerWebTest` (201, 403 sin permiso, 401, 409 auto-aprobación, 400 sin `Idempotency-Key`) | 5 PASS | `./gradlew :servicios:nucleo-financiero:webTest` | TODO |
| H4.S2.M5 | Códigos estables `RETIRO_APROBACION_REQUERIDA`, `RETIRO_AUTOAPROBACION_PROHIBIDA` en el catálogo | `erroresCatalogo` PASS | `./gradlew erroresCatalogo` | TODO |

### H4.S3 — Concurrencia y fallo del proveedor (`test(wallet): concurrent approvers and provider failure`)
**CA:** Dado dos aprobadores distintos que aprueban simultáneamente, cuando terminan, entonces exactamente uno obtiene `AUTORIZADA` y el otro `409`; dado approve/reject concurrentes → uno gana; dado un reintento del mismo aprobador con la misma clave → misma respuesta; dado el proveedor que falla después de `AUTORIZADA` → `RECHAZADA` con retención liberada y motivo, sin movimiento en el libro.
**DoD:** `CU11AprobacionConcurrenciaTest` PASS (4 escenarios) con conteos pegados.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.S3.M1 | Escenarios 1–3 (race aprobadores, approve/reject, reintento idempotente) | 3 PASS | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*CU11AprobacionConcurrenciaTest*'` | TODO |
| H4.S3.M2 | Escenario 4: `AUTORIZADA → EN_PROCESO`, proveedor (doble, tres niveles) responde fallo → `rechazar` libera retención; `movimiento_billetera` sin filas nuevas | PASS + consulta `SELECT count(*) FROM movimiento_billetera WHERE …` = 0 | mismo comando | TODO |
| H4.S3.M3 | `ADR-049 Doble aprobación de retiros.md` + `docs/Seguridad.md` control R-BIL-xx con su comando | bóveda verde | `python3 scripts/verificar_boveda.py` | TODO |
| H4.S3.M4 | Regresión del módulo + `spotlessCheck` | verde | `./gradlew :servicios:nucleo-financiero:webTest :servicios:nucleo-financiero:integrationTest spotlessCheck` | TODO |

---

## H5 — Identidad y JWT endurecidos: producción no arranca con defaults inseguros

**CA:** Dado perfil `production` sin `JWT_CLAVE_FIRMA` (o con `SEGURIDAD_PIMIENTA` placeholder, CORS `*`, MFA doble, URL de proveedor `http://`), cuando arranca `identidad` o cualquier servicio, entonces el proceso termina con exit ≠ 0 y el log nombra la propiedad; dado un token con `iss`/`aud` incorrectos, firma ajena o vencido, cuando llega a cualquier servicio, entonces `401`; dado un refresh reutilizado tras rotar, entonces se rechaza, la familia queda invalidada y hay evento de auditoría.
**DoD:** `GuardiaDeProduccionTest` (comun-web) + `ArranqueProduccionTest` (identidad) + `DecodificadorTest` + `RefrescoReusoTest` PASS; regresión de identidad verde; ADR de rotación.
**Estado:** TODO

### H5.S1 — Sin RSA efímera fuera de `local`/`test` (`fix(identity): fail startup without signing key outside local/test`)
**CA:** Dado `aportaya.jwt.clave-firma` vacía y perfil ≠ local/test, cuando arranca `identidad`, entonces falla con `IllegalStateException("aportaya.jwt.clave-firma es obligatoria en <perfil>")`; en `local`/`test` genera en memoria y avisa (comportamiento actual).
**DoD:** `ArranqueProduccionTest` (identidad) 2 casos PASS.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | Test rojo: `@ActiveProfiles("production")` + clave vacía → contexto falla; `test` + vacía → levanta con WARN | 1 FAIL hoy | `./gradlew :servicios:identidad:integrationTest --tests '*ArranqueProduccionTest*'` | TODO |
| H5.S1.M2 | `EmisorDeAcceso`: inyectar `Environment`/perfil; `generar()` solo si perfil ∈ {local,test}; si no, lanzar | PASS | mismo comando | TODO |
| H5.S1.M3 | `application-production.yml` de identidad: `aportaya.jwt.clave-firma: ${JWT_CLAVE_FIRMA}` **sin default** (falla por `PlaceholderResolutionException` incluso antes de la guarda) | PASS | mismo comando | TODO |

### H5.S2 — `iss`/`aud`/`kid`/skew en emisor y decodificador (`feat(security): validate issuer and audience on every service`)
**CA:** Dado un token emitido por `identidad`, cuando lo valida un servicio, entonces exige `iss = aportaya.jwt.emisor`, `aud` contiene el nombre del servicio (o `aportaya` global, decisión registrada), `exp` con skew ≤ 60 s, `kid` presente; dado un JWKS con dos claves (rotación), ambos tokens validan durante el solapamiento.
**DoD:** `DecodificadorTest` (comun-web, 6 casos: iss malo, aud malo, vencido, firma ajena, kid ausente, skew) PASS; `EmisorDeAccesoTest` claims PASS; `ArranqueTest` de los 14 PASS.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.S2.M1 | Tests rojos `DecodificadorTest` (con JWKS servido por `MockWebServer`/`WireMock` ya disponible o `HttpServer` del JDK) | 6 FAIL | `./gradlew :plataforma:comun-web:integrationTest --tests '*DecodificadorTest*'` | TODO |
| H5.S2.M2 | `EmisorDeAcceso.emitir` agrega `iss` (`aportaya.jwt.emisor`) y `aud` (`aportaya.jwt.audiencia`, lista) — también en `EmisorDeEvidencia` (H3) | `EmisorDeAccesoTest` PASS | `./gradlew :servicios:identidad:test --tests '*EmisorDeAcceso*'` | TODO |
| H5.S2.M3 | `ConfiguracionDelDecodificador`: `JwtValidators.createDefaultWithIssuer(...)` + `JwtClaimValidator<List<String>>("aud", …)` + `JwtTimestampValidator(Duration.ofSeconds(skew))`; propiedades `aportaya.jwt.{emisor, audiencia, tolerancia}` en la plantilla de `application.yml` de los 14 (`scripts/nuevo_servicio.py`) | 6 PASS; 14 `ArranqueTest` PASS | comandos M1 + `./gradlew integrationTest --tests '*ArranqueTest*'` | TODO |
| H5.S2.M4 | JWKS con solapamiento: `aportaya.jwt.claves-anteriores` (lista de JWK públicas) publicadas junto con la vigente; `kid` distinto; test de rotación (token firmado con clave anterior valida) | PASS | `./gradlew :servicios:identidad:integrationTest --tests '*JwksRotacionTest*'` | TODO |
| H5.S2.M5 | `docs/operacion/jwt-key-rotation.md` (runbook §67): generar JWK, cargar en secretos, desplegar identidad con ambas, esperar `VIGENCIA` × 2, retirar; verificación con `curl /.well-known/jwks.json` | archivo con las 7 secciones del §67 | revisión | TODO |

### H5.S3 — Refresh tokens: reuso detectado y familia invalidada (`test(identity): refresh token reuse detection`)
**CA:** Dado refresh A rotado a B, cuando A se presenta de nuevo, entonces `401`, B y toda la familia quedan invalidados, se registra evento de seguridad en auditoría y el usuario debe volver a autenticarse.
**DoD:** `RefrescoReusoTest` (integrationTest, sobre el trigger R-SEG-09 real) PASS; `SesionesControllerWebTest` 401 PASS.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.S3.M1 | Localizar el CU de refresco (grep `refresc`, `CorteDeCredencial`) y documentar el flujo actual con ruta:línea en `baseline.md` | flujo documentado | revisión | TODO |
| H5.S3.M2 | `RefrescoReusoTest`: A→B→A; aserciones sobre `sesion`/`token` (estado INVALIDADO), respuesta 401 y fila en bitácora | PASS (o FAIL → PRODUCT_BUG → microtarea nueva) | `./gradlew :servicios:identidad:integrationTest --tests '*RefrescoReusoTest*'` | TODO |
| H5.S3.M3 | Si el trigger lo cubre pero la app no emite evento/auditoría → agregarlo en el CU (outbox `sesion.familia_revocada`) | PASS | mismo comando | TODO |

### H5.S4 — Contraseñas, tokens y logs (`chore(identity): verify argon2 params and secret hygiene`)
**CA:** Dado el hasher, cuando se inspeccionan sus parámetros, entonces cumplen OWASP (Argon2id, m ≥ 19 MiB, t ≥ 2, p = 1 o justificado) y la pimienta viene de entorno sin default; dado cualquier log, no contiene contraseña, OTP, token, JWT ni JWK privada.
**DoD:** `ArgonParametrosTest` PASS; `python3 scripts/verificar_seguridad.py` con regla nueva de logs PASS; grep de logs de una corrida `integrationTest` sin valores sensibles.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.S4.M1 | Test de parámetros Argon2id contra la clase real (localizar `Argon2`/`Hasher` en identidad) y comparación timing-safe | PASS | `./gradlew :servicios:identidad:test --tests '*ArgonParametrosTest*'` | TODO |
| H5.S4.M2 | Regla en `verificar_seguridad.py`: prohibido `BITACORA.*(…token…|…otp…|…clave…)` con el valor; whitelist explícita | exit 0 | `python3 scripts/verificar_seguridad.py` | TODO |
| H5.S4.M3 | Reset de contraseña: test de expiración del token, un solo uso e invalidación de sesiones tras el reset (CU-09) | PASS o `A MEDIAS` con causa | `./gradlew :servicios:identidad:integrationTest --tests '*CU09*'` | TODO |

### H5.S5 — Perfiles por entorno y guarda de arranque en producción (`feat(platform): production startup guards`)
**CA:** Dado perfil ∉ {local,test}, cuando arranca cualquier servicio, entonces `GuardiaDeProduccion` (comun-web, `SmartInitializingSingleton`/`ApplicationRunner` que corre antes de aceptar tráfico) falla ante: clave de firma ausente (identidad), pimienta ausente o con valor de ejemplo (`pimienta-de-prueba`, `cambiar`, `example`), `BD_URL`/`KAFKA_URL` ausentes, orígenes CORS con `*`, `aportaya.mfa.doble-local=true`, URL de proveedor sin `https://`; en `local`/`test` no interviene.
**DoD:** `GuardiaDeProduccionTest` (7 casos) PASS; `ArranqueProduccionTest` en identidad y nucleo PASS; `docs/Arquitectura/Entornos y despliegue.md` actualizado.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.S5.M1 | Tests rojos `GuardiaDeProduccionTest` (7 casos, `ApplicationContextRunner`) | 7 FAIL | `./gradlew :plataforma:comun-web:test --tests '*GuardiaDeProduccionTest*'` | TODO |
| H5.S5.M2 | `GuardiaDeProduccion` + propiedades `aportaya.entorno.productivo` derivada del perfil; lista de placeholders prohibidos configurable | 7 PASS | mismo comando | TODO |
| H5.S5.M3 | `application-{local,test,staging,production}.yml` en los 14 servicios y gateway, generados por `scripts/nuevo_servicio.py` (plantilla) para no divergir; `docker-compose.coolify.yml` y `generar_compose.py` fijan `SPRING_PROFILES_ACTIVE` | `generar_compose.py` sin diff residual; `ArranqueTest` ×14 PASS | `python3 scripts/generar_compose.py && git diff --stat`; `./gradlew integrationTest --tests '*ArranqueTest*'` | TODO |
| H5.S5.M4 | Documentar en `docs/Arquitectura/Entornos y despliegue.md` qué exige cada perfil (tabla variable → obligatoria en) | tabla presente | `python3 scripts/verificar_boveda.py` exit 0 | TODO |

---

## H6 — CI real y cadena de suministro: verde en el SHA final sin trampas

**CA:** Dado un push a `dev`, cuando corre el CI, entonces todos los jobs terminan `success` sin `skipTests`, `ignoreFailures` ni `|| true`; existen escaneo real de dependencias (falla ante HIGH/CRITICAL), SBOM CycloneDX como artifact, escaneo de imagen, CODEOWNERS y un documento con la protección de ramas exacta; un desarrollador puede correr `./gradlew verificarProduccion` localmente.
**DoD:** `gh run list --branch <rama> --limit 1` → `success` en todos los jobs; artifacts `sbom-*.json` presentes; `docs/operacion/branch-protection.md` escrito.
**Estado:** TODO

### H6.S1 — Spotless en verde (`style: apply spotless across the repo`)
**CA:** Dado el repo, cuando corre `spotlessCheck`, entonces exit 0.
**DoD:** `./gradlew spotlessCheck` exit 0 local + job `codigo` verde en CI.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S1.M1 | `./gradlew spotlessApply` y revisar el diff (solo formato; ningún cambio semántico) | diff solo whitespace/orden de imports | `git diff --stat` + `./gradlew spotlessCheck` exit 0 | TODO |
| H6.S1.M2 | Commit propio y push; CI job `codigo` verde | `gh run view` success | `gh run list --branch <rama> --limit 1` | TODO |

### H6.S2 — SCA real (`ci(security): add OSV scanner with severity policy`)
**CA:** Dado el catálogo de dependencias resuelto, cuando corre el CI, entonces `osv-scanner` analiza los lockfiles/árbol de Gradle y falla si hay HIGH/CRITICAL no exceptuados (AMB-9).
**DoD:** job `seguridad` con paso OSV verde y reporte SARIF subido; prueba negativa documentada (dependencia vulnerable temporal → rojo → revertida).
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S2.M1 | Generar entrada para OSV: `./gradlew dependencies` no alcanza → usar `google/osv-scanner-action` sobre `gradle/libs.versions.toml` + lockfiles de Gradle (`./gradlew dependencies --write-locks` en los módulos desplegables; commitear `gradle.lockfile`) — verificar en la doc de OSV qué formatos de Gradle soporta antes de elegir | lockfiles commiteados | `ls servicios/*/gradle.lockfile plataforma/gateway/gradle.lockfile` | TODO |
| H6.S2.M2 | Paso en `ci.yml` job `seguridad` reemplazando "19c" con `osv-scanner` + `--config .github/osv-scanner.toml` (severidad, excepciones con motivo/fecha) | job verde | `gh run view --job <id>` | TODO |
| H6.S2.M3 | Prueba negativa: rama temporal con una dependencia con CVE alta conocida → job rojo; revertir; pegar salida | rojo demostrado | evidencia `H6-S2-M3-osv-negativo.txt` | TODO |
| H6.S2.M4 | Dependabot (`.github/dependabot.yml`, gradle + github-actions, semanal) | archivo válido | `gh api repos/…/dependabot/…` o revisión de sintaxis | TODO |

### H6.S3 — SBOM CycloneDX (`ci(supply-chain): generate CycloneDX SBOM per service`)
**CA:** Dado un build de CI, cuando termina, entonces hay un `bom.json` CycloneDX por módulo desplegable como artifact.
**DoD:** `./gradlew cyclonedxBom` exit 0 local; artifact `sbom` en la corrida.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S3.M1 | Plugin `org.cyclonedx.bom` en `aportaya.servicio.gradle.kts` (verificar versión y API en la doc del plugin; agregar al catálogo como micro-PR) | `bom.json` generado | `./gradlew :servicios:identidad:cyclonedxBom && ls servicios/identidad/build/reports/bom.json` | TODO |
| H6.S3.M2 | Paso CI + `upload-artifact` `sbom-<sha>` | artifact visible | `gh run view <id>` lista el artifact | TODO |

### H6.S4 — Imagen: escaneo y endurecimiento (`ci(security): scan container image and harden runtime`)
**CA:** Dado la imagen de un servicio, cuando se escanea con Trivy, entonces no hay CRITICAL sin excepción; el contenedor corre como `app`, con filesystem de solo lectura salvo `/tmp`, sin `wget` si el healthcheck puede hacerse con Java, y base con digest fijado.
**DoD:** paso Trivy verde; `docker inspect` muestra `User=app`; compose con `read_only: true` + `tmpfs`; `HEALTHCHECK` funciona.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S4.M1 | Paso `aquasecurity/trivy-action` sobre `aportaya/identidad:ci` con `severity: CRITICAL,HIGH`, `exit-code: 1`, `ignore-unfixed: true`; `.trivyignore` con motivo/fecha | job verde | `gh run view --job <id>` | TODO |
| H6.S4.M2 | Fijar base por digest (`eclipse-temurin:21-jre-noble@sha256:…`) y `21-jdk@sha256` en construcción | Dockerfile con digest | `grep -n "@sha256" despliegue/Dockerfile` → 2 líneas | TODO |
| H6.S4.M3 | `read_only: true` + `tmpfs: /tmp` en `generar_compose.py`/Coolify; verificar que Spring arranca (jOOQ/tmp) | servicio healthy | `docker compose … up -d --wait` exit 0 | TODO |
| H6.S4.M4 | Reemplazar `wget` del healthcheck por `java -cp … HealthCheck` o `curl` ya presente en la base (evaluar; si no hay alternativa, dejar `wget` y documentar) | decisión escrita | `docker image inspect` sin `wget` o nota en ADR-025 | TODO |

### H6.S5 — Gobernanza: CODEOWNERS y protección de ramas (`docs(ops): branch protection and CODEOWNERS`)
**CA:** Dado `.github/CODEOWNERS`, cuando se toca `/sql`, `/servicios/nucleo-financiero`, `/servicios/identidad`, `/servicios/cumplimiento`, `/plataforma/comun-web`, `/plataforma/comun-mensajeria`, `/.github/workflows`, entonces exige revisión del dueño; `docs/operacion/branch-protection.md` contiene la configuración exacta (ruleset JSON + comandos `gh api`) para `dev` y `main`.
**DoD:** archivos escritos; `gh api repos/…/codeowners/errors` sin errores; **aplicación de la protección solo con confirmación de Pablo** (fila M3 queda `BLOQUEADO: DECISION_REQUIRED` hasta entonces, sin simular: es acción destructiva/compartida).
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S5.M1 | `.github/CODEOWNERS` con los 7 paths | sin errores de sintaxis | `gh api repos/PabloArauzCaballero/PasanakuBackend/codeowners/errors` → `{"errors":[]}` | TODO |
| H6.S5.M2 | `docs/operacion/branch-protection.md`: PR obligatorio, 1 aprobación (`dev`) / 2 (`main`), CODEOWNERS, checks requeridos = nombres exactos de los jobs de `ci.yml` y `boveda-y-esquema.yml`, up-to-date, sin force push, sin borrado, commits firmados (opcional, decisión), + ruleset JSON y comando `gh api --method POST repos/…/rulesets --input ruleset-dev.json` | documento completo | revisión | TODO |
| H6.S5.M3 | Aplicar el ruleset completo (PR + checks + CODEOWNERS) en la promoción `dev → main`; el mínimo (sin force-push ni borrado en `dev`/`test`/`main`) queda listo en `ruleset-minimo.json` para que Pablo lo aplique con un comando | `gh api …/rulesets` lista ambos | `gh api repos/PabloArauzCaballero/PasanakuBackend/rulesets --jq '.[].name'` | TODO |

### H6.S6 — E2E financiero antes de promoción y agregador local (`ci: run financial e2e on dev and add verificarProduccion`)
**CA:** Dado un push a `dev` (o `workflow_dispatch`), cuando corre el CI, entonces el job `e2e` ejecuta al menos `OutboxE2ETest`, `RetiroConMfaE2ETest` y el E2E de four-eyes; dado un desarrollador local, `./gradlew verificarProduccion` corre formato, estático, tests, integración, contrato, saga, verificaciones de base y `verificar_seguridad.py` sin exigir secretos.
**DoD:** job `e2e` verde en `dev`; `./gradlew verificarProduccion` exit 0 local.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S6.M1 | Job `e2e-financiero` en `ci.yml` con `services` postgres + kafka (imagen `apache/kafka` fijada) y `if: github.ref == 'refs/heads/dev' || workflow_dispatch`, corriendo `./gradlew :servicios:nucleo-financiero:e2eTest` | job verde | `gh run view` | TODO |
| H6.S6.M2 | Tarea raíz `verificarProduccion` (dependsOn `verificar`, `testBarrido`, `erroresCatalogo`, `cyclonedxBom`, `Exec` de `verificar_seguridad.py` y `verificar_boveda.py`) sin scanners que requieran token | exit 0 | `./gradlew verificarProduccion` | TODO |
| H6.S6.M3 | Revisar que ningún paso use `-x test`, `ignoreFailures`, `|| true` para tapar fallos (el `-x test` del job `codigo` es legítimo porque `pruebas` los corre; documentarlo) | grep con justificación | `grep -n "|| true\|ignoreFailures\|skipTests" .github/workflows/*.yml buildSrc -r` → vacío o justificado | TODO |
| H6.S6.M4 | Corrida completa del CI en la rama con **todos** los jobs verdes | success ×N | `gh run view <id> --json jobs --jq '.jobs[].conclusion'` → solo `success` | TODO |

---

## H7 — Seguridad HTTP/API demostrada endpoint por endpoint

**CA:** Dado el inventario de endpoints, cuando se lee `endpoints.md`, entonces cada uno tiene permiso, regla de propiedad, idempotencia, rate limit, MFA, auditoría y test; dado un usuario A, cuando altera un ID de B en billetera/retiros/extracto, entonces `403`/`404` sin filtrar datos; dado 20 intentos de login en un minuto, entonces `429`; dado un error, la respuesta tiene `codigo`, `mensaje`, `correlationId`, `timestamp` y nunca stacktrace/SQL.
**DoD:** `docs/auditoria-produccion/endpoints.md` y `security-matrix.md` escritos y generados por script; tests IDOR, rate limit, mass assignment y errores PASS; gate `api-pentest`/`authz-access-control` revisado.
**Estado:** TODO

### H7.S1 — Inventario de endpoints generado (`docs(security): endpoint inventory`)
**CA:** Dado los 15 contratos OpenAPI y las anotaciones `@Permiso`/`@Publico`, cuando corre `scripts/inventario_endpoints.py`, entonces produce `endpoints.md` con Service, Method, Path, Permission, Ownership, Idempotency, Rate limit, MFA, Audit, Tests, y falla si un endpoint implementado no está en OpenAPI o viceversa (§37).
**DoD:** `python3 scripts/inventario_endpoints.py --check` exit 0; archivo generado.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H7.S1.M1 | Script (patrón de `verificar_pruebas_web.py`/`auditar_backend.py`): cruza `openapi/*.yaml` × controladores × `*WebTest` | tabla completa | comando | TODO |
| H7.S1.M2 | Paso en CI (`boveda`) y tarea Gradle `inventarioEndpoints` | verde | `gh run view` | TODO |
| H7.S1.M3 | Marcar en la tabla los endpoints sensibles (§90.5) → alimentan H7.S2–S6 | columna `sensible` | revisión | TODO |

### H7.S2 — IDOR/BOLA con matriz negativa (`test(security): negative authorization matrix on wallet`)
**CA:** Dado A y B con billetera, cuando A pide `/billetera/{cuentaDeB}/saldo`, `/extracto`, `retiros/{ordenDeB}/aprobacion`, `retenciones/{deB}/cierre`, entonces `403` (o `404` según convención existente) y el cuerpo no contiene datos de B.
**DoD:** `AutorizacionNegativaTest` (integrationTest con dos sesiones) PASS en nucleo-financiero; réplica en `identidad` (sesiones/dispositivos) y `aportes`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H7.S2.M1 | Test rojo/verde por cada endpoint con recurso identificado en nucleo-financiero (≥ 6) | PASS | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*AutorizacionNegativaTest*'` | TODO |
| H7.S2.M2 | Corregir cada ownership faltante en el CU (nunca en el controller) | PASS | mismo comando | TODO |
| H7.S2.M3 | Misma matriz en `identidad` y `aportes` | PASS | `./gradlew :servicios:identidad:integrationTest :servicios:aportes:integrationTest --tests '*AutorizacionNegativaTest*'` | TODO |

### H7.S3 — Mass assignment, validación de entrada, SQL, SSRF, uploads (`test(security): input hardening`)
**CA:** Dado un cuerpo con campos no declarados, cuando llega, entonces `400` (contratos con `additionalProperties: false`); strings, arrays, paginación, montos, fechas y teléfonos tienen límites en el contrato; no hay `DSL.field(String)`/`DSL.condition(String)`/`fetchOne(String)` con entrada de usuario interpolada; URLs de terceros configuradas, nunca del usuario; uploads con tamaño, MIME real y nombre aleatorio.
**DoD:** `verificar_seguridad.py` con reglas nuevas exit 0; `ContratosLimitesTest` PASS; `ArchivosTest` PASS.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H7.S3.M1 | Script: todo `schema` de request tiene `additionalProperties: false` y `maxLength`/`maximum`/`maxItems`; paginación con `maximum` | exit 0 | `python3 scripts/verificar_contratos_limites.py` | TODO |
| H7.S3.M2 | Grep + regla: `DSL.field("` con nombre de columna literal está permitido; `DSL.field(variable)`, `DSL.condition(String)`, `execute(String)`, `String.format` con SQL → prohibido salvo whitelist con comentario `SQL-SEGURO:` | exit 0 | `python3 scripts/verificar_seguridad.py` | TODO |
| H7.S3.M3 | Inventario de clientes HTTP (`*PorHttp`, `RestClient`, `HttpClient`) y URLs: todas de configuración; sin fetch de URL provista por usuario (si existe → whitelist de esquemas, bloqueo de rangos privados/metadata, timeouts, redirects ≤ 3) | tabla en `security-matrix.md` | revisión + tests si aplica | TODO |
| H7.S3.M4 | `comun-archivos`: test de tamaño máximo, magic bytes vs MIME declarado, nombre aleatorio, objeto privado, URL firmada corta | PASS | `./gradlew :plataforma:comun-archivos:integrationTest` | TODO |

### H7.S4 — Rate limiting en el borde (`feat(gateway): distributed rate limiting for sensitive routes`)
**CA:** Dado el gateway con Redis (AMB-6), cuando un mismo cliente supera N req/min en login, refresh, reset, MFA, retiro, transferencia, registro y OTP, entonces `429` con `Retry-After`; con Redis caído el gateway **deniega** (fail closed) en esas rutas y lo mide.
**DoD:** `RateLimitGatewayTest` PASS (Testcontainers Redis); ADR; compose y Coolify actualizados.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H7.S4.M1 | `ADR-050 Rate limiting distribuido en el borde.md` (Redis vs NGINX; decisión AMB-6) | ADR | `verificar_boveda.py` | TODO |
| H7.S4.M2 | Redis en `despliegue/compose/base.yml` (imagen fijada) y `docker-compose.coolify.yml`; dependencia `spring-boot-starter-data-redis-reactive` en catálogo (micro-PR) | contenedor healthy | `docker compose … up -d --wait` | TODO |
| H7.S4.M3 | Filtro `RequestRateLimiter` con `KeyResolver` por IP+usuario en las rutas generadas por `generar_gateway.py` (marcar rutas sensibles en `PREFIJOS`/modelo) | test PASS | `./gradlew :plataforma:gateway:integrationTest --tests '*RateLimitGatewayTest*'` | TODO |
| H7.S4.M4 | Comportamiento con Redis caído = denegar en rutas sensibles + métrica | test PASS | mismo comando | TODO |

### H7.S5 — CORS, cabeceras y errores (`feat(gateway): explicit CORS per environment and security headers`)
**CA:** Dado perfil `production`, cuando un origen no listado hace preflight, entonces se rechaza; `*` está prohibido con credenciales; NGINX envía HSTS solo si termina TLS; respuestas sensibles llevan `Cache-Control: no-store`; los errores siguen el formato `{codigo, mensaje, correlationId, timestamp}`.
**DoD:** `CorsGatewayTest` PASS; `ManejadorGlobalDeErroresWebTest` con `correlationId` y `timestamp` PASS; `curl -I` en compose con cabeceras pegadas.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H7.S5.M1 | CORS en gateway por perfil (`aportaya.cors.origenes`, métodos y cabeceras mínimos, `allowCredentials` solo si se usa); `GuardiaDeProduccion` rechaza `*` | test PASS | `./gradlew :plataforma:gateway:integrationTest --tests '*CorsGatewayTest*'` | TODO |
| H7.S5.M2 | NGINX: `Strict-Transport-Security` si TLS termina ahí (verificar en `despliegue/nginx/aportaya.conf` y Coolify); `Cache-Control: no-store` para `/api/`; `Permissions-Policy` si aplica | cabeceras presentes | `curl -sI http://localhost/api/v1/...` pegado | TODO |
| H7.S5.M3 | Formato de error unificado con `correlationId` (de `Traza`) y `timestamp`; sin stacktrace/SQL/nombres de tabla (revisar `TraduccionDeRestricciones` para que el mensaje no exponga `constraint_name`) | test PASS | `./gradlew :plataforma:comun-web:webTest` | TODO |

### H7.S6 — Auditoría de operaciones críticas y matriz de seguridad (`feat(audit): critical operations leave append-only trail`)
**CA:** Dado login, login fallido, challenge MFA, cambio de rol/permiso, transferencia, retiro, aprobación, rechazo, cambio de configuración sensible, cuando ocurren, entonces existe una fila append-only con actor, acción, target, timestamp, correlationId, IP/dispositivo y estado anterior/nuevo cuando aplica, sin secretos.
**DoD:** `AuditoriaCriticaTest` por servicio PASS; `security-matrix.md` con OWASP API Top 10:2023 cubierto fila por fila (Componente, Amenaza, Control, Test, Evidencia, Estado, Riesgo residual).
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H7.S6.M1 | Localizar el mecanismo de bitácora encadenada (`auditoria`, `Traza`, `bitacora_*`) y qué CU ya lo usan; tabla en `security-matrix.md` | tabla | revisión | TODO |
| H7.S6.M2 | Agregar auditoría donde falte (tests rojos → verdes), sin datos de personas ni tokens | PASS | `./gradlew integrationTest --tests '*AuditoriaCriticaTest*'` | TODO |
| H7.S6.M3 | `docs/auditoria-produccion/security-matrix.md` completo | 10 categorías OWASP API + filas de §90.5 | revisión | TODO |

---

## H8 — Consistencia monetaria y ledger demostrados con invariantes

**CA:** Dado cualquier operación que calcule, mueva o reporte dinero, cuando se inspecciona su código y sus tests, entonces no usa `double`/`float`, lleva moneda, redondea con modo declarado, `SUM(debe)=SUM(haber)` por transacción se demuestra en 11 escenarios, la cadena de hash se preserva y existe un benchmark del advisory lock con p50/p95/p99.
**DoD:** `testBarrido` con regla de dinero PASS; `LibroInvariantesTest` (11 escenarios) PASS; `CuadrarPartidasPropiedadTest` ampliado PASS; `docs/auditoria-produccion/financial-invariants.md` + `evidencia/H8-benchmark-hashchain.txt`.
**Estado:** TODO

### H8.S1 — Sin `double`/`float` en dinero, con moneda y redondeo declarados (`test(money): forbid floating point money`)
**CA:** Dado el repo, cuando corre el barrido, entonces ningún archivo de `servicios/**` ni `plataforma/**` usa `double`/`float`/`Double`/`Float` en un tipo cuyo nombre contenga `monto|saldo|importe|comision|tarifa|aporte|retiro|transferencia|Dinero`, ni `BigDecimal` sin escala/`RoundingMode` explícitos en división.
**DoD:** `./gradlew testBarrido` PASS; prueba negativa pegada.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H8.S1.M1 | Grep de las 12 palabras del §14 + `double|float` → tabla de ocurrencias con veredicto en `financial-invariants.md` | tabla | `grep -rnE "\b(double|float|Double|Float)\b" --include=*.java servicios plataforma` pegado | TODO |
| H8.S1.M2 | Regla en `comun-pruebas/Barrido`: patrón anterior + `divide(` sin `RoundingMode` | PASS; negativo demostrado | `./gradlew testBarrido` | TODO |
| H8.S1.M3 | `Dinero` (comun-dominio): tests de escala (2), moneda distinta → error, negativo → error, cero permitido donde corresponde, serialización JSON como string con 2 decimales, `NUMERIC(16,2)` compatible | PASS | `./gradlew :plataforma:comun-dominio:test --tests '*DineroTest*'` | TODO |
| H8.S1.M4 | Redondeo declarado en `CostoDeOperacion`/tarifas: modo y destino del residuo documentados y testeados (§91.6.5) | PASS | `./gradlew :servicios:nucleo-financiero:test --tests '*CostoDeOperacionTest*'`; sección en `financial-invariants.md` | TODO |

### H8.S2 — Invariantes del ledger en 11 escenarios (`test(ledger): double-entry invariants`)
**CA:** Dado cada escenario del §15 (transferencia OK, saldo insuficiente, moneda distinta, cuenta bloqueada, P2P no permitido, concurrencia, dos opuestas simultáneas, replay, rollback, excepción tras débito, resistencia a deadlock), cuando se ejecuta, entonces `SUM(debe)=SUM(haber)` por transacción y la suma de saldos del sistema es invariante.
**DoD:** `LibroInvariantesTest` PASS con consulta de cuadre pegada por escenario.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H8.S2.M1 | Escenarios 1–5 (funcionales) reutilizando `BaseDeBilletera`; cuadre por `SELECT transaccion_id, SUM(CASE sentido…) FROM movimiento_billetera GROUP BY …` = 0 | 5 PASS | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*LibroInvariantesTest*'` | TODO |
| H8.S2.M2 | Escenarios 6–8 (concurrencia, opuestas, replay) — 100 hilos sobre una cuenta (§43): sin saldo negativo, sin pérdida, sin duplicación, suma preservada | 3 PASS + conteos | mismo comando | TODO |
| H8.S2.M3 | Escenarios 9–11 (rollback, excepción tras débito con `Datos.conContexto`, deadlock: transferencias cruzadas A→B y B→A ×50 con orden de bloqueo de `CU12:78-80`) | 3 PASS; `pg_stat_database.deadlocks` sin incremento | mismo comando + `SELECT deadlocks FROM pg_stat_database WHERE datname=current_database()` | TODO |
| H8.S2.M4 | Property test (jqwik, ampliar `CuadrarPartidasPropiedadTest`): para cualquier secuencia de transferencias internas válidas, `saldo_total_antes == saldo_total_despues` y ningún asiento confirmado cambia (`hash_registro` estable) | PASS con ≥ 1000 tries | `./gradlew :servicios:nucleo-financiero:test --tests '*PropiedadTest*'` | TODO |

### H8.S3 — Hash chain: medir antes de tocar (`perf(ledger): benchmark hash-chain advisory lock`)
**CA:** Dado 200 transferencias concurrentes contra PostgreSQL real, cuando corre el benchmark, entonces se registran throughput, p50/p95/p99, tiempo esperando el advisory lock (`pg_stat_activity.wait_event`), conexiones, deadlocks; **no se cambia el mecanismo** salvo que sea cuello de botella demostrado y la alternativa preserve la integridad criptográfica.
**DoD:** `evidencia/H8-benchmark-hashchain.txt` con las 6 métricas; decisión escrita en `financial-invariants.md`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H8.S3.M1 | Localizar el advisory lock en `sql/40_reglas/restricciones.sql` (trigger de `transaccion_billetera`) y documentarlo con línea | ruta:línea | revisión | TODO |
| H8.S3.M2 | Benchmark reproducible: `LibroBenchmarkTest` (etiquetado `@Tag("benchmark")`, excluido del CI) o script k6 contra compose; 200 hilos, 3 corridas | métricas pegadas | comando del test/script | TODO |
| H8.S3.M3 | Decisión: mantener (default) o proponer alternativa (p. ej. cadena por cuenta con hash de raíz diario) **solo con** medición que muestre el lock > X % del tiempo; si se propone, ADR y `DECISION_REQUIRED` antes de implementar | sección en `financial-invariants.md` | revisión | TODO |
| H8.S3.M4 | `docs/auditoria-produccion/financial-invariants.md`: balances, ledger, transfer, withdrawal, fees, guarantees (si aplica), settlements, reconciliation, redondeo, idempotencia — cada uno con invariante, dónde vive (SQL/Java) y test que lo demuestra | documento completo | revisión | TODO |

---

## H9 — Observabilidad, health y resiliencia reales

**CA:** Dado un servicio corriendo en compose, cuando se consulta `/actuator/prometheus`, entonces expone requests, errores, latencia p50/p95/p99, JVM, pool, Kafka, outbox y las 9 métricas de negocio del §68 sin IDs de usuario; los logs son JSON con `timestamp, level, service, traceId, correlationId, event, message`; el `correlationId` cruza HTTP → CU → outbox → Kafka → consumidor; `liveness` sigue UP con PostgreSQL/Kafka caídos y `readiness` cae; todo cliente HTTP tiene timeouts, retry solo si idempotente, circuit breaker y métricas; el retiro con proveedor tiene estados y reconciliación ante timeout.
**DoD:** `curl` de métricas pegado; `LogsEstructuradosTest`; `TrazaPropagadaE2ETest`; `ProbesTest` (Kafka y PostgreSQL apagados); `ClientesResilientesTest`; `docs/auditoria-produccion/proveedores.md` (fichas §75).
**Estado:** TODO

### H9.S1 — Métricas técnicas y de negocio (`feat(observability): business and outbox metrics`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H9.S1.M1 | Verificar exposición de `prometheus` en los 14 (`management.endpoints.web.exposure.include`) y que el endpoint **no** es público sin red interna (NGINX/gateway no lo enruta) | `/actuator/prometheus` responde en red interna, 404 desde el gateway | `curl` ×2 pegados | TODO |
| H9.S1.M2 | Contadores `wallet_transfer_{success,failure}_total`, `withdrawal_{requested,approved,failed}_total`, `auth_login_failures_total`, `mfa_failures_total`, `outbox_publish_failures_total`, `outbox_pending` (gauge) — nombres en snake_case Prometheus, sin etiquetas de usuario; emitidos desde los CU vía `MeterRegistry` | métricas aparecen tras ejercitar cada CU | `curl -s :8080/actuator/prometheus | grep -E "wallet_|withdrawal_|auth_login|mfa_|outbox_"` → 9 líneas | TODO |
| H9.S1.M3 | Test de cardinalidad: regla en barrido que prohíbe `.tag("usuario"|"cuenta"|"id", …)` en métricas | PASS | `./gradlew testBarrido` | TODO |

### H9.S2 — Logs estructurados y trazabilidad (`feat(observability): structured logs and end-to-end correlation`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H9.S2.M1 | `logging.structured.format.console=ecs` (o `logstash`) de Spring Boot 3.5 en perfiles ≠ local (verificar propiedad en la doc de Boot 3.5.6); campos `service`, `traceId`, `correlationId`, `event` vía MDC de `Traza` | log JSON con los 7 campos | `LogsEstructuradosTest` PASS + línea de log pegada (sin datos de persona) | TODO |
| H9.S2.M2 | Propagar `correlationId`/`traceId` a cabeceras Kafka (H2.S2.M3) y restaurarlos en el consumidor de prueba; `TrazaPropagadaE2ETest` cruza HTTP → CU → outbox → Kafka → consumidor → cliente HTTP saliente (`CotizadorPorHttp` envía la cabecera) | mismo id en los 5 saltos | `./gradlew :servicios:nucleo-financiero:e2eTest --tests '*TrazaPropagadaE2ETest*'` | TODO |
| H9.S2.M3 | Regla de barrido: ningún `BITACORA` con `cuerpo`, `payload`, `factor`, `token`, `documento`, `cuenta_bancaria` como valor | PASS | `./gradlew testBarrido` | TODO |

### H9.S3 — Liveness/readiness y caídas (`test(resilience): probes under dependency outage`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H9.S3.M1 | Verificar `management.endpoint.health.group.readiness.include=db,kafka` (o el equivalente real) y que `liveness` excluye ambos | config + test | `ProbesTest` PASS | TODO |
| H9.S3.M2 | Kafka apagado (`KafkaContainer.stop()`): liveness UP, readiness DOWN, operación financiera commitea (H2.S4.M1) | PASS | `./gradlew :servicios:nucleo-financiero:e2eTest --tests '*ProbesE2ETest*'` | TODO |
| H9.S3.M3 | PostgreSQL apagado: errores `503` controlados (no 500 con stacktrace), readiness DOWN, liveness UP, pool Hikari se reconecta al volver, sin loop agresivo (contar intentos en log ≤ N/min) | PASS + conteo | mismo comando | TODO |

### H9.S4 — Clientes externos y proveedor de retiros (`feat(resilience): timeouts, circuit breakers and provider reconciliation`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H9.S4.M1 | Inventario de clientes HTTP salientes (`*PorHttp`, gateway) → `docs/auditoria-produccion/proveedores.md` con ficha §75 (provider, operation, timeout, idempotency, retry, CB, reconciliation, credentials, sandbox, observability) | ≥ 1 ficha por cliente | revisión | TODO |
| H9.S4.M2 | resilience4j (ya en catálogo) en cada cliente: `TimeLimiter`/timeouts connect+read, `Retry` **solo** en GET/idempotentes con `Idempotency-Key`, `CircuitBreaker` con fallback declarado (denegar por omisión en dinero), métricas | `ClientesResilientesTest` (WireMock/`MockWebServer`: timeout, 500, CB abierto) PASS | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*ClientesResilientesTest*'` | TODO |
| H9.S4.M3 | Retiro con proveedor (§41): puerto `ProveedorDeRetiro` (instruir, consultar estado), estados `AUTORIZADA → EN_PROCESO → PAGADA|RECHAZADA`, **timeout ≠ fallo**: queda `EN_PROCESO` con `referencia_proveedor` y job de reconciliación (ShedLock) que consulta; doble en tres niveles (`@Profile local/test`): acepta / responde tarde (límite) / rechaza-timeout (inválido) | `CU11ProveedorTest` 3 niveles PASS | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*CU11ProveedorTest*'` | TODO |
| H9.S4.M4 | Webhooks (§42): si existe `webhook_pasarela` en `aportes`, verificar firma, ventana temporal, replay (`uq_webhook_idem`), versión de esquema, auditoría; tests en tres niveles | PASS | `./gradlew :servicios:aportes:integrationTest --tests '*Webhook*'` | TODO |
| H9.S4.M5 | `docs/operacion/provider-timeout.md` y `docs/operacion/withdrawal-reconciliation.md` (runbooks §67) | 7 secciones cada uno | revisión | TODO |

---

## H10 — Concurrencia, base de datos y carga con números reales

**CA:** Dado PostgreSQL real, cuando se ejecutan los tests de RLS/grants desde la conexión de cada `svc_*`, entonces cada servicio solo ve su esquema, `FORCE ROW LEVEL SECURITY` no se puede saltear, el rol auditor es de solo lectura; `empty → latest` y `latest → latest` aplican sin pérdida; las semillas ×2 son idempotentes y producción las rechaza; existe un escenario k6 reproducible con baseline de throughput/latencias; PIT evaluado sobre dinero, idempotencia, retiro, aprobación y MFA.
**DoD:** `AislamientoEsquemaTest` ampliado PASS en los 14; `k6/` en repo con `evidencia/H10-carga-baseline.txt`; `docs/auditoria-produccion/mutation-testing.md`.
**Estado:** TODO

### H10.S1 — RLS y grants desde cada servicio (`test(db): per-service RLS and grants`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H10.S1.M1 | Leer `AislamientoEsquemaTest` y `verificaciones.sql`; tabla de cobertura vs §47 (leer/escribir permitido, otros esquemas, RLS aplica, `FORCE RLS`, auditor solo lectura, migración con privilegios limitados) | tabla | revisión | TODO |
| H10.S1.M2 | Completar casos faltantes como tests parametrizados por servicio (conexión con `svc_<x>` real del contenedor) | PASS ×14 | `./gradlew integrationTest --tests '*AislamientoEsquemaTest*'` | TODO |
| H10.S1.M3 | Append-only demostrado: `UPDATE`/`DELETE` sobre `transaccion_billetera`, `movimiento_billetera`, bitácoras y `evento_consumido` con rol `svc_*` → error de permiso/trigger | PASS | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*AppendOnlyTest*'` | TODO |

### H10.S2 — Esquema y semillas (`test(db): schema from zero and reapply`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H10.S2.M1 | `empty → latest` (CI ya lo hace; reproducir local con salida) | exit 0 | H0.S4.M1 reutilizado | TODO |
| H10.S2.M2 | `latest → latest` con datos: aplicar, sembrar dev, insertar filas de prueba, **re-aplicar** `aplicar.sql`, verificar que las filas siguen (AMB-7) | conteos iguales | consulta antes/después pegada | TODO |
| H10.S2.M3 | Documentar en `docs/operacion/schema-changes.md` que no hay migraciones versionadas, cómo se expande/contrae hoy y el `DECISION_REQUIRED` de Flyway/Liquibase | documento | revisión | TODO |
| H10.S2.M4 | Guarda de semillas dev en producción: test automatizado (ya en CI job `base` paso 10) referenciado como evidencia; sin trabajo nuevo salvo que falle | evidencia enlazada | `gh run view` | TODO |

### H10.S3 — Carga reproducible (`perf: k6 scenarios and baseline`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H10.S3.M1 | `carga/k6/` con escenarios: login, consulta de saldo, transferencia, replay idempotente, retiro (proveedor doble), y lectura de `outbox_pending`; datos sintéticos deterministas (`scripts/clave_dev.py` para cuentas de prueba) | scripts ejecutan contra compose | `k6 run carga/k6/transferencia.js` exit 0 | TODO |
| H10.S3.M2 | Corrida baseline (3 repeticiones) registrando throughput, p50/p95/p99, errores, `pg_locks`, CPU, heap, lag de Kafka; **sin objetivos inventados** (§58): se reporta baseline | evidencia con tabla | `evidencia/H10-carga-baseline.txt` | TODO |
| H10.S3.M3 | Límites de recursos (§73): `hikari.maximum-pool-size` × 14 ≤ `max_connections` (verificar en compose/PgBouncer), `server.max-http-request-header-size`, `spring.servlet.multipart.max-*`, Kafka `buffer.memory`, pool del `RestClient`; tabla en `proveedores.md`/`financial-invariants.md` | tabla con valores y fuente | revisión | TODO |

### H10.S4 — Mutation testing evaluado (`test: PIT evaluation on financial core`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H10.S4.M1 | Plugin PIT (`info.solidsoft.pitest`, verificar versión compatible con Gradle 9.7 y JUnit 5) solo en `nucleo-financiero`, `comun-web` (idempotencia), `identidad` (MFA); target classes: `Dinero`, `CostoDeOperacion`, `CondicionesDeRetiro`, `EstadoDeRetiro`, `Idempotencia`, `SegundoFactorStepUp`, `EmisorDeEvidencia` | reporte generado | `./gradlew :servicios:nucleo-financiero:pitest` exit 0 | TODO |
| H10.S4.M2 | `docs/auditoria-produccion/mutation-testing.md` con score por clase, mutantes sobrevivientes relevantes y qué test se agregó (o por qué no) | documento | revisión | TODO |

---

## H11 — Arquitectura, código muerto y dependencias (sin mega-refactor)

**CA:** Dado el repo, cuando corren las reglas ArchUnit, entonces fallan ante controller → repositorio directo, dominio → Spring/jOOQ, ciclos, servicio A importando `bo.aportaya.<B>.infraestructura|aplicacion`, y JPA; no quedan adapters sin uso, mocks/dobles en perfiles productivos, `TODO` críticos ni flags inseguros; el informe de dependencias lista sin uso/obsoletas/duplicadas sin aplicar majors.
**DoD:** `./gradlew check` PASS con reglas nuevas; `docs/auditoria-produccion/dependencias.md`; `git grep -n "TODO" servicios plataforma` sin `TODO` en P0.
**Estado:** TODO

### H11.S1 — Reglas de arquitectura (`test(arch): enforce layering across services`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H11.S1.M1 | Leer `ArquitecturaTest` de nucleo-financiero y las reglas de `comun-pruebas`; tabla de cobertura vs §55 | tabla | revisión | TODO |
| H11.S1.M2 | Completar reglas faltantes en una clase compartida de `comun-pruebas` (`ReglasDeArquitectura`) usada por los 14 `ArquitecturaTest` | PASS ×14; negativo demostrado | `./gradlew test --tests '*ArquitecturaTest*'` | TODO |

### H11.S2 — Código muerto y flags inseguros (`chore: remove dead adapters and unsafe flags`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H11.S2.M1 | Inventario: beans/adapters sin uso (verificar wiring por reflexión/Spring antes de borrar), configs duplicadas, endpoints abandonados, `TODO`/`FIXME`, dobles sin `@Profile` | tabla con decisión por ítem | `git grep -nE "TODO|FIXME" -- servicios plataforma` pegado | TODO |
| H11.S2.M2 | Eliminar lo confirmado muerto (una microtarea por ítem si tiene riesgo; se agregan al plan) | `check` verde | `./gradlew check` | TODO |
| H11.S2.M3 | Los 7 comentarios de esqueleto `idempotencia.exigirNueva` (H1.S5.M2): adoptar o borrar según decisión | 0 comentarios huérfanos | `git grep -n "idempotencia.exigirNueva" servicios` → solo usos reales | TODO |

### H11.S3 — Higiene de dependencias (`docs(deps): dependency hygiene report`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H11.S3.M1 | `./gradlew dependencies` por módulo + OSV + revisión manual: sin uso, obsoletas, duplicadas, transitivas innecesarias; **sin** upgrades majors | `dependencias.md` | revisión | TODO |
| H11.S3.M2 | Aplicar solo parches/minors con CVE (uno por commit, `verificar` en verde después de cada uno) | verde | `./gradlew verificar` | TODO |

---

## H12 — Operación documentada, gate de promoción y cierre demostrable

**CA:** Dado un ingeniero nuevo, cuando lee `README` + `docs/operacion/` + `docs/auditoria-produccion/`, entonces puede levantar el entorno, correr los tests, entender eventos, servicios y permisos, desplegar staging, responder a un incidente con los 7 runbooks y saber exactamente qué falta para promover `dev → main`; el `FINAL_REPORT.md` abre con el estado (`READY` / `READY WITH NON-BLOCKING RISKS` / `NOT READY`) sustentado por evidencia.
**DoD:** 7 runbooks + `backup-recovery.md` + 4 ADR + `promotion-gate.md` (solo con checks que tengan evidencia) + `FINAL_REPORT.md` + `REPORTE.md` (regla 40) + inspección final del diff + validación en checkout limpio.
**Estado:** TODO

### H12.S1 — Runbooks y respaldo (`docs(ops): runbooks and backup/recovery`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H12.S1.M1 | `docs/operacion/outbox-backlog.md`, `kafka-down.md`, `postgres-down.md`, `secret-rotation.md` (los otros 3 salen de H5.S2.M5 y H9.S4.M5), cada uno con síntomas, dashboards/consultas, diagnóstico, acciones seguras, recuperación, validación, escalamiento | 7 archivos × 7 secciones | `ls docs/operacion/*.md | wc -l` ≥ 9; revisión | TODO |
| H12.S1.M2 | `docs/operacion/backup-recovery.md`: backup PostgreSQL (`pg_dump`/base + WAL/PITR según Coolify/ADR-013), **RPO/RTO = "a definir por operación"** (AMB-8), prueba de restore ejecutada en local con salida, consideraciones Kafka (retención, no es fuente de verdad), respaldo de secretos, runbook | restore probado | `evidencia/H12-restore.txt` con `pg_restore` exit 0 + conteo de tablas | TODO |
| H12.S1.M3 | Actualizar `README.md`, `docs/Arquitectura/Entornos y despliegue.md`, `docs/Seguridad.md`, `docs/Pruebas.md` con lo nuevo (perfiles, step-up, aprobación, relevo, rate limit, CI) | bóveda y seguridad verdes | `python3 scripts/verificar_boveda.py; python3 scripts/verificar_seguridad.py` | TODO |

### H12.S2 — Matrices, ADR y gate de promoción (`docs(audit): matrices, ADRs and promotion gate`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H12.S2.M1 | Verificar que existen y enlazan: `ADR-046` idempotencia, `ADR-047` outbox, `ADR-048` step-up, `ADR-049` four-eyes, `ADR-050` rate limit; `endpoints.md`, `security-matrix.md`, `financial-invariants.md`, `proveedores.md`, `dependencias.md`, `mutation-testing.md` | 11 archivos | `ls` pegado | TODO |
| H12.S2.M2 | `docs/auditoria-produccion/promotion-gate.md` con los 17 checks del §85 + los de este plan; **cada check marcado solo con enlace a evidencia** | ningún `[x]` sin enlace | revisión línea por línea | TODO |

### H12.S3 — Inspección final y checkout limpio (`chore: final audit of the diff`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H12.S3.M1 | `git diff origin/dev...HEAD` revisado como auditor independiente (regla 70.4.8: revisor distinto del autor → subagente de solo lectura con contrato: buscar debug, secretos, `TODO`, mocks en prod, bypasses, imports muertos, logs sensibles, SQL peligroso, tests desactivados) | informe con 0 hallazgos bloqueantes o microtareas nuevas | `evidencia/H12-revision-diff.md` | TODO |
| H12.S3.M2 | `gitleaks detect --source . --no-git` y `python3 scripts/verificar_seguridad.py` en el SHA final | exit 0 ×2 | salidas pegadas | TODO |
| H12.S3.M3 | Worktree limpio (`git worktree add ../pr-limpio <sha-final>`): setup → `./gradlew verificarProduccion` → base desde cero → `e2eTest` → build de imagen → Trivy local | todo exit 0 | `evidencia/H12-checkout-limpio.txt` | TODO |
| H12.S3.M4 | CI verde en el SHA final (todos los jobs, incluido `e2e-financiero`) | success | `gh run view <id> --json jobs --jq '.jobs[].conclusion'` | TODO |

### H12.S4 — Reportes (`docs(audit): final report`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H12.S4.M1 | `docs/auditoria-produccion/FINAL_REPORT.md` con las 16 secciones del §86; estado `READY` solo si H1–H6 y todos los P0 están `HECHO` con evidencia; SHA inicial y final; cambios manuales externos pendientes (rulesets, secretos, Redis en Coolify, decisiones abiertas) | 16 secciones | revisión contra §86 | TODO |
| H12.S4.M2 | `REPORTE.md` en este repo (regla 40): avance en la primera línea, Completado/A medias/Pendiente, evidencia, no cubierto, desvíos, riesgos, decisiones | `report_gate.py` no bloquea | `python .claude/hooks/report_gate.py --self-test`; `python .claude/hooks/plan_status.py` | TODO |
| H12.S4.M3 | Recomendación `dev → main` (sin ejecutar el merge): PR de promoción con checklist del gate, orden de despliegue (identidad con claves, Redis, variables por perfil), rollback | sección en `FINAL_REPORT.md` | revisión | TODO |

## 6. Riesgos y bloqueos previstos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| La máquina de ejecución no tiene JDK ni `psql` en PATH (esta Windows) | H0 no arranca | H0.S1.M1; `psql` se usa dentro del contenedor (`docker exec`) |
| Docker Desktop degradado (la auditoría previa reporta `docker ps` colgado en otra máquina) | Testcontainers falla → ENVIRONMENT | Verificar `docker info` antes de cada suite; no paralelizar módulos (regla 70); documentar como `BLOCKED` con evidencia si ocurre |
| `integrationTest` de los 14 en serie tarda decenas de minutos (cada uno aplica 305 tablas) | Ciclo lento | Correr por módulo el afectado; suite completa solo al cerrar cada hito |
| Cambios de esquema deben pasar por `generar_ddl.py`/`.puml` y los 4 verificadores de bóveda | Un `ALTER` a mano rompe el CI | Toda microtarea de esquema termina con `generar_ddl.py` + `verificar_boveda.py` |
| Contrato OpenAPI cambia (`evidenciaMfa`, endpoint de aprobación) y los frontends (`apps/`) consumen clientes generados | Rotura fuera de alcance | Compatibilidad hacia atrás (aceptar campo viejo con fecha de retiro), `generateOpenApiClients` en verde; frontends anotados en el reporte, no tocados |
| Step-up MFA depende de un proveedor de OTP real que no existe | No se puede "verificar" un factor real | Regla 65: doble en tres niveles bajo `local/test`, producción falla cerrado sin adaptador; `DECISION_REQUIRED` registrada |
| Redis nuevo en el stack (rate limiting) | Infra adicional en Coolify | ADR-050 + AMB-6; alternativa NGINX documentada |
| `-Werror` + `spotless` convierten cualquier warning en rojo | Iteraciones lentas | `spotlessApply` antes de cada commit; compilar el módulo tocado antes de la suite |
| Branch protection requiere acción del dueño | J queda abierto | Documento + comandos listos; `BLOQUEADO — DECISION_REQUIRED` explícito |
| Hallazgos nuevos durante H0 (5 fallos previos del gate citados por la auditoría: organizador CU-92/93, núcleo CU-12, tarifas CU-31) | Alcance crece | Se clasifican (regla 80.4) y entran al plan como microtareas o se anotan fuera de alcance; el % baja y se dice |
| `KafkaContainer` API cambió entre versiones de Testcontainers | Test no compila | Verificar la clase en la versión del BOM antes de escribir (regla 00.1.3) |

## 7. Orden de ejecución y dependencias

```
H0 ──► H1 ──► H2 ──► H3 ──► H4 ──► H5 ──► H6 ──► H7 ──► H8 ──► H9 ──► H10 ──► H11 ──► H12
        │             │      │              │
        │             │      └─ H4.S2 usa el permiso y el contrato; H3.S3 usa el JWKS que H5.S2 endurece (se hace en H3 con `iss/aud` provisional y se cierra en H5)
        │             └─ H2.S2 necesita el fixture Kafka (H2.S2.M1) que reutilizan H9.S2/S3
        └─ H1.S1.M7 (tabla por generador) es el primer cambio de esquema: valida el flujo `.puml → generar_ddl → CI` que reutilizan H2.S3.M1, H3.S2.M2, H3.S3.M2
H6.S1 (Spotless) puede adelantarse a H1 si H0 muestra que el rojo es solo formato: se registra como desvío del plan.
```

Total de microtareas al escribir el plan: **H0 = 25 · H1 = 21 · H2 = 20 · H3 = 18 · H4 = 12 · H5 = 18 · H6 = 19 · H7 = 20 · H8 = 12 · H9 = 14 · H10 = 12 · H11 = 7 · H12 = 12 → 210**. Avance actual: `0 / 210 — 0 %` (el conteo exacto lo calcula `python .claude/hooks/plan_status.py`; si difiere, manda el script).
