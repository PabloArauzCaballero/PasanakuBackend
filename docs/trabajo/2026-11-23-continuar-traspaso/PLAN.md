# Plan — Continuar traspaso Pasanaku (PR #503–#505 → dev → test)

- Fecha: 2026-11-23 · Repos afectados: PasanakuBackend (PasanakuFrontend solo lectura de estado) · Predecesor: traspaso del 2026-11-23 (ramas `codex/claude-handoff-*`)
- Resultado observable: las PR #503, #504 y #505 quedan con CI completo en verde e integradas a `dev`; `test` recibe solo ese conjunto validado.
- Kill-test: cualquier check requerido en rojo sobre el head de una PR, o `integrationTest` de `servicios/grupos` en rojo en #504.

## Alcance
- IN: corregir los fallos reales de CI de #503 y #504, vigilar #505, merge a `dev` de PR verdes, promoción `dev → test` si todo el conjunto está verde.
- OUT: `main` (sin autorización de Pablo); checkpoints M08 y M6 (incompletos, no se integran); PR a `main` #468/#495–#497; Frontend `c28f69bb`.
- Ambigüedades registradas:
  - M08 TTL: ¿24 meses estricto o "casi 25" por partición mensual? Supuesto: no se decide en este trabajo (DECISION_REQUIRED, confirmar con Pablo).
  - El traspaso atribuía el rojo de #504 a un lock de JaCoCo; el log de CI muestra otra causa (ver hallazgos). Se sigue la evidencia del log.

## Hallazgos (descubrimiento, con evidencia en `evidencia/`)
- #503 "Formato, reglas propias y compilación" (job 138033735520): Maven Central respondió **429** al resolver `kotlin-stdlib:2.4.0` → clase `ENVIRONMENT`, no `spotlessCheck`.
- #503 "Frontend ADR-044" (job 138035184830): `yarn typecheck` del backoffice falla con TS4111 en `pantalla-de-parametros-financieros.spec.ts` líneas 98, 99, 145, 146 → `TEST_BUG`.
- #504 "Los cinco corredores" (job 138026360778): `servicios:grupos:integrationTest` 337 tests, 10 fallan con `IllegalStateException: El turno … no tiene titular` en `TurnosAsignadosRepositorio.delGrupo:30` → `PRODUCT_BUG`.
- Corrección: #504 se clasifica `TEST_BUG`/`DATA` (dato de prueba irreal), no `PRODUCT_BUG`.
- #505: head actual `ed9bb51`; "Formato" ya en verde; "Los cinco corredores" en curso al abrir este plan.

## H1 — PR #503–#505 en verde e integradas a dev
**CA:** Dado el head de cada PR, cuando CI termina, entonces todos los checks no omitidos están en `success` y la PR se integra a `dev`.
**DoD:** check-runs del head en success (salida de `gh api …/check-runs` pegada) · merge registrado.
**Estado:** EN CURSO

### H1.S1 — #503 CU-132
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Corregir TS4111 en el spec (acceso con corchetes) | typecheck del backoffice sin errores TS4111 | `yarn typecheck` en apps/backoffice → exit 0 | HECHO — 31234da (TS4111) + 28cb588b (plantilla estricta: variante, `?? false`, `?? null`); CI de 28cb588b ya compiló el bundle |
| H1.S1.M2 | Re-ejecutar CI de #503 (429 era de entorno) | Formato y Frontend en success | check-runs del head nuevo | HECHO — CI de bcfc6274: 12 success / 0 fallos |
| H1.S1.M3 | Confirmar doble aprobación por persona distinta con `TARIFARIO_ADMINISTRAR` | existe test/control que rechaza aprobación del mismo proponente | `grep` del control + test pasando en CI | HECHO — `CU132RegistroDeParametros.java:168` + `@Permiso("TARIFARIO_ADMINISTRAR")` (TarifasController:245-265) + test `mismaPersona` |
### H1.S2 — #504 turnos asignados
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Reproducir y localizar causa de "turno sin titular" | causa enunciada explica los 10 fallos | lectura de código + reproducción | HECHO — fixture `cuposOcupados` sin titular (y luego sin firma del reglamento); producción siempre fija ambos |
| H1.S2.M2 | Corregir la causa en producto | los 10 tests pasan sin debilitar aserciones | `./gradlew :servicios:grupos:integrationTest` local o CI verde | HECHO — 319e7b3 + 29dc2b22; CI de 29dc2b22: 12 success / 0 fallos |
### H1.S3 — #505 CU-44
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | Esperar "Los cinco corredores" y triagear si falla | todos los checks en success | check-runs del head | HECHO — CI de ed9bb51: 12 success / 0 fallos |
### H1.S4 — Integración
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S4.M1 | Merge a `dev` de cada PR verde | `origin/dev` contiene los heads | `git branch -r --contains <head>` | HECHO — #505 (64da8b97), #504 (98b03005), #503 (d62225d9) en `origin/dev` |
## H2 — Promoción dev → test
**CA:** Dado `dev` con #503–#505 y CI verde, cuando se promueve, entonces `test` apunta a ese contenido y el health responde.
**DoD:** CI de `dev` verde · promoción por la ruta acordada · `estado` ejecutado por Pablo en su host (fuera de este entorno: SSH bloqueado).
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Revisar diff `test..dev` y CI de `dev` | sin cambios inesperados, CI verde | `git diff --stat origin/test origin/dev` + check-runs | HECHO — 78 archivos, +1851/−402, sólo #499–#505; sin M08/M6 |
| H2.S1.M2 | Promover `dev → test` | `origin/test` contiene `dev` | `git branch -r --contains` | HECHO — #511 `dev → test` mergeada (d75ac069): 28 success / 7 skipped / 1 failure preexistente (Release iOS, deep_link_test 02, igual que en dev antes de este trabajo); sin M08/M6 |
| H2.S1.M3 | Health en TEST | `estado` responde OK | salida provista por Pablo | TODO |

## H3 — App móvil (apps/movil): corregir los defectos abiertos
Pedido por Pablo el 2026-11-24. Inventario verificado contra HEAD por un agente de solo lectura,
fuente: auditoría `origin/auditoria/frontends-compartidos-2026-11-20` y `ANEXO-L6`. Ya corregidos
(no se rehacen): DINERO-002, DEFECTO-002, CU-21 `pagarCuotaConSaldo` en `pantalla_aportar.dart`.

**CA:** Dado la app móvil en `dev`, cuando una persona usa enlaces, sesión, pagos y pantallas
protegidas, entonces ningún defecto abierto del inventario se reproduce y la suite móvil completa
(incluidos `test/billetera` y `test/navegacion`) corre en CI.
**DoD:** CI "Frontend — Flutter y Angular (ADR-044)" en success con `test:front` ampliado ·
`dart format` y `dart analyze` limpios en lo tocado · PR a `dev` integrada.
**Estado:** TODO
**Rama:** `claude/movil-correcciones-20261124` (desde `dev`), una PR a `dev` cuando haya cupo (máx. 3 abiertas).

### H3.S1 — Defectos de código móvil (sin decisión pendiente)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | HUECO-001: `aportaya://aporte` y el push llevan a una ruta existente (`enlaces_profundos.dart:16`, `ruta_del_aviso.dart`) | abrir el enlace no termina en ruta inexistente | test de `enlaces_profundos` nuevo/ajustado en verde | HECHO — aefc77de; rojo 3 fallos → verde 37/37 |
| H3.S1.M2 | DEFECTO-003: un solo `Refrescador` compartido (`cliente.dart:90`, `renovar_acceso.dart:16`, `habilitacion_basica.dart:101`) | dos 401 simultáneos producen UN refresh | test de concurrencia del refresco en verde | HECHO — 7a40ff3e; rojo 2 refrescos → verde 6/6 |
| H3.S1.M3 | PRUEBAS-001: `test:front` incluye `test/billetera` y `test/navegacion` (`package.json:12`) | CI ejecuta esos 5 archivos | log de CI con esos tests | HECHO — 275fa64f; 929 verdes con las 7 carpetas |
| H3.S1.M4 | DEFECTO-004: FLAG_SECURE con contador (`proteccion_de_pantalla_dinero.dart:11-28`) | cerrar una protegida apilada deja la de abajo protegida | test unitario del contador en verde | HECHO — 3667951c; rojo → verde 3/3 + 24 de protección |
| H3.S1.M5 | UX-A11Y-001: guardia de identidad cubre retiro, traspaso y permuta (`acceso_a_rutas.dart:17-22`) | sin KYC esas rutas redirigen antes del 403 | test de `acceso_a_rutas` en verde | HECHO — d8402050; rojo 4 → verde 10/10; suite 950 |
| H3.S1.M6 | PARCHE-001: el vigilante no traga errores de programación (`vigilante_de_aprobacion.dart:85-86`) | un `Error` se propaga; uno de red se ignora | test del vigilante en verde | HECHO — 987b5aea; rojo → verde 5/5 |
| H3.S1.M7 | DOC-001: al cerrar sesión se borra el progreso de tutoriales (`sesion.dart:143-147`) | tras cerrar sesión el almacén de progreso queda vacío | test de sesión en verde | HECHO — e0ae3721; rojo → verde 3/3; 199 de sesión/tutoriales |
| H3.S1.M8 | DUP-INT-001: una sola configuración de idioma (`localizacion.dart` vs `app_real.dart:20`) | queda una sola fuente | `dart analyze` + test de arranque en verde | HECHO — a0ce40c1; analyze limpio; suite 954 |
| H3.S1.M9 | REESCRITURA-001: retirar código muerto que parece control (`guardias.dart`, `conexion.dart`, `validacion.dart`, `dispositivo.dart`, `main_h6_s3_m8_profile.dart`) | ningún import los usa y se borran | `grep` sin referencias + `dart analyze` limpio | HECHO — 0f478e94; 0 importadores; analyze limpio; suite 954 |
| H3.S1.M10 | Fixture de CU-21: `pantalla_aportar_test.dart:35` usa `pagarCuotaConSaldo` | el test simula la operación real | test en verde | HECHO — 275fa64f; 4/4 |
| H3.S1.M11 | CONFIG-001: quitar TODO de plantilla y firma debug del release Android (`android/app/build.gradle.kts:21,26-27,38-40`) | release no firma con clave debug; sin TODO de plantilla | `grep` + build de Gradle Android en CI | BLOQUEADO — DECISION_REQUIRED + acción sobre algo compartido: hace falta una clave de release que solo Pablo puede crear/custodiar, y verificar implica correr apk-de-prueba.yml, que publica un prerelease. Destraba: Pablo crea la keystore de release y su secreto en Actions |
### H3.S2 — Requieren app + backend
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S2.M1 | FLUJO-QR-001: «Simular pago» solo con `--dart-define` y el backend falla al arrancar en producción con modo=prueba | build sin define no muestra el botón | test widget + test de arranque del backend | HECHO — #506 CI 17 success / 2 skipped (incl. guardia backend compilada y probada) → merge e6751478 |
| H3.S2.M2 | SEG-005: secreto de invitación en el fragmento, no en el path (`rutas.dart:122`, `enlaces_profundos.dart:32`, `lectura_de_qr.dart:40`) | la URL compartida no lleva el secreto en el path | tests de enlaces + contrato | HECHO — e7698bc9: enlace `/g#/g/<codigo>`, manifest `path=/g`, nginx 302; tests de enlaces verdes (suite 957) |
| H3.S2.M3 | `cobrarAporte` marcado `deprecated: true` en `aportes.yaml:70` (se conserva por la app desplegada) | el OpenAPI lo marca deprecado, sin quitarlo | verificador de contratos en verde | HECHO — 19d7c756; validate sin problemas; postman/catálogo sin deriva; app sin issues |
### H3.S3 — Decisión de negocio (DECISION_REQUIRED, Pablo)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S3.M1 | RESPALDO-003: qué se promete del fondo de garantía en el tour (`identidad/textos.dart:42-46`) | texto aprobado por Pablo aplicado | test de textos | BLOQUEADO — DECISION_REQUIRED: redacción regulatoria |
| H3.S3.M2 | APROB-001: retirar o no los textos de aprobación humana `AP-CU20-02`, `AP-CU69-10` (`errores.dart:57,76`) | decisión aplicada | test de errores | BLOQUEADO — DECISION_REQUIRED |
| H3.S3.M3 | PENDIENTES.md: grupos/detalle sin ruta (H8.S4.M4, H10.S3.M1) | decisión registrada | — | BLOQUEADO — DECISION_REQUIRED |

### H3.S4 — Verificación que necesita dispositivo/credenciales reales
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S4.M1 | Push real FCM, rostros reales, goldens canónicos macOS (PENDIENTES H10.S2.M1–M6, H6.S2.M3, H10.S1.M6/M7) | ejecutado en AVD/macOS con cuentas de ensayo | evidencia de la corrida | BLOQUEADO — requiere dispositivo y cuentas fuera de este entorno; la lógica sí se cubre con dobles en H3.S1 |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Rate-limit 429 de Maven Central en CI | rojo espurio | re-ejecutar el job; si persiste, registrar como ENVIRONMENT |
| Sin Java/Gradle/Postgres local | no se reproduce local | usar CI como verificación, declarado |
| SSH a TEST bloqueado | no se observa el health | Pablo ejecuta `estado` (acción en su host, no simulable) |

## H4 — Rediseño visual de las pantallas mostradas (pedido de Pablo, 2026-11-24)
**CA:** Dado el pedido de mejorar sustancialmente los visuales, cuando Pablo abre la recarga QR (app), el caso de cumplimiento y los parámetros financieros (backoffice), entonces ve jerarquía clara (lo que hay que hacer primero), estado vivo y legible, y el mismo comportamiento y textos de negocio, en claro y oscuro y en 3 anchos.
**DoD:** suites de la app y del backoffice en verde · analyze/lint/format · E2E visuales re-generados · capturas antes/después inspeccionadas · PR a `dev` con CI verde.
**Estado:** EN CURSO
- Rama: `claude/visuales-20261124` (desde 2915db44, cabeza de #506). Sólo tokens del sistema de diseño, componentes existentes primero, sin librerías nuevas, sin cambiar contratos ni reglas de dinero.

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H4.S1.M1 | App: recarga QR con monto como protagonista, estado «esperando confirmación» visible y saldo secundario | capturas 3 anchos × 2 temas × 4 estados muestran la nueva jerarquía; tests de recarga verdes | `flutter test test/billetera` + capturas | HECHO — 11ee8082 (`claude/visuales-movil-20261124`); verificado por la sesión principal: analyze sin issues, format 0 cambios, 44/44 dirigidas; agente: test:front 957; 24 capturas antes/después |
| H4.S2.M1 | Backoffice: caso de cumplimiento con cabecera de resumen y escalera compacta en móvil / lateral en escritorio | E2E `caso-de-cumplimiento` 16/16 + capturas nuevas | `yarn playwright test e2e/caso-de-cumplimiento.e2e.ts` | HECHO — ae441d9f; verificado: E2E caso 18/18, specs del caso + grupo-radio verdes, typecheck 0 |
| H4.S2.M2 | Backoffice: parámetros financieros con vigente / propuesta pendiente / historial jerarquizados | spec + E2E visual verdes | `ng test` del componente + `yarn playwright test e2e/visual-parametros-financieros.e2e.ts` | HECHO — ae441d9f + d74707f7 (captura sin barra fija a mitad); E2E parámetros 10/10 |
| H4.S3.M1 | PR de H4 a `dev` con CI verde y merge | `origin/dev` contiene la rama | check-runs + `git branch -r --contains` | HECHO — #507 → dev (cc8ad163), #508 → dev (842f8e26; CI 9 success / 8 skipped) |

## H5 — Contrato: datos que las pantallas rediseñadas declaran faltantes (pedido de Pablo, 2026-11-24)
**CA:** Dado un caso de cumplimiento o un parámetro con versiones, cuando un operador abre su pantalla en el backoffice, entonces ve la alerta de origen y el titular (enmascarado) del caso, y quién propuso y quién aprobó cada versión del parámetro, servidos por el contrato real (cambio aditivo y compatible) y no inventados en el cliente.
**DoD:** OpenAPI + implementación + tests del servicio en CI verde · clientes regenerados · pantallas y E2E actualizados · autorización negativa del endpoint intacta · sin PII en claro · PR a `dev` con CI verde y merge.
**Estado:** HECHO
- Rama: `claude/contrato-caso-parametros-20261124`, encima de `claude/visuales-backoffice-20261124` (#507).

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H5.S1.M1 | CU-132: el contrato de versión de parámetro publica quién propuso y quién aprobó | respuesta incluye ambos campos (aprobado nulo si pendiente) | test web de tarifas en CI + `openapi-generator validate` | HECHO — 576770ad; CI de #510: compilación con -Werror y `servicios:tarifas:test` en verde |
| H5.S1.M2 | CU-44: el contrato del caso publica alerta de origen y titular enmascarado | respuesta incluye ambos, titular nunca en claro | test web de cumplimiento en CI + validate | HECHO — 424db313 + e6d1ab11 (criterio en la bóveda); CI: `servicios:cumplimiento:test` y verificar_criterios en verde |
| H5.S2.M1 | Backoffice muestra esos datos en lugar del aviso de faltante | specs + E2E caso y parámetros verdes con el simulado regenerado | `yarn playwright test e2e/caso-de-cumplimiento.e2e.ts e2e/visual-parametros-financieros.e2e.ts` | HECHO — b2d6f20c; typecheck/lint OK, test:front 1361, a11y 98, E2E caso 19/19 y parámetros 12/12; capturas revisadas por la sesión principal |
| H5.S3.M1 | PR de H5 a `dev` con CI verde y merge | `origin/dev` contiene la rama | check-runs + merge | HECHO — #510 → dev (cb651d9f); CI 12 success / 6 skipped |
| H6.S1.M1 | Ninguna PR abierta en PasanakuBackend ni PasanakuFrontend | `pulls?state=open` vacío en ambos | `gh api repos/.../pulls?state=open` → [] | HECHO — `pulls?state=open` → 0 en PasanakuBackend y 0 en PasanakuFrontend (2026-11-24) |

## H7 — Nombre del operador en vez del UUID (pedido de Pablo, 2026-11-24)
**CA:** Dado un parámetro con versiones, cuando un operador abre «Parámetros financieros», entonces «Propuso» y «Aprobó» muestran el nombre del operador interno (y, si no se puede resolver, un texto que lo dice, nunca un UUID suelto ni un error crudo).
**DoD:** consulta mínima de nombres en identidad (solo id + nombre, solo usuarios internos, límite de cantidad, autorización negativa probada) en CI verde · backoffice resuelve y muestra nombres con E2E y capturas · PR a `dev` mergeada · promoción a `test`.
**Estado:** EN CURSO
- Rama: `claude/nombre-operador-20261124` desde `origin/dev`.

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H7.S1.M1 | Identidad publica nombres de operadores internos por lote | respuesta con solo `usuarioId` + nombre; ids no internos se omiten; sin permiso → 403 | test web de identidad en CI + `openapi-generator validate` | HECHO — cb4ad819 + 4fbd17c5 (pruebas separadas por el barrido de 300 líneas); CI de #512 12 success |
| H7.S2.M1 | Backoffice muestra el nombre en Propuso/Aprobó | E2E parámetros verde con nombres y con fallo de resolución | `yarn playwright test e2e/visual-parametros-financieros.e2e.ts` | HECHO — e196ad49; E2E parámetros 16/16; capturas revisadas |
| H7.S3.M1 | PR a `dev` con CI verde, merge y promoción a `test` | `origin/test` contiene la rama | check-runs + merges | HECHO — #512 → dev (6729629d); promoción #513 (24 success, 11 skipped) → test f957167b |
