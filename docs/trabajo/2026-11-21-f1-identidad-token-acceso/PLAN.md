# Plan — F1: la identidad sale del token y el acceso es real

- Fecha: 2026-11-21 · Repo: `PabloArauzCaballero/PasanakuBackend` · Rama: `correcciones/f1-identidad-token-acceso-2026-11-21` (desde `origin/dev` 850e0f51)
- Predecesor: plan de correcciones `docs/trabajo/2026-11-21-plan-correcciones/` (rama `claude/nice-fermat-essf9e`), anexo L4 y `DECISIONES.md`.
- Resultado observable: (1) existe un único componente en `comun-web` que da el actor del token y exige que la segunda firma venga de **otra persona en otra sesión**; (2) una cuenta suspendida, bloqueada o dada de baja no obtiene ni renueva token; (3) un operador entra con un TOTP real y `000000` no entra en ningún entorno.
- Kill-test: `grep -rn '"000000"' servicios/identidad/src/main` devuelve algo · una prueba que suspende una cuenta y renueva su refresh obtiene token · el componente de cuatro ojos acepta el mismo `sub` con otro `sesion_id`.

## Alcance
- IN: `plataforma/comun-web`, `plataforma/comun-dominio` (solo si lo exige el actor), `servicios/identidad` (código, contrato `openapi/identidad.yaml`, pruebas), y este directorio de trabajo.
- OUT: `sql/`, `docs/entidades`, `docker-compose*`, `apps/*`, los demás servicios. La migración de los endpoints que toman aprobadores del cuerpo es de la próxima tanda; acá solo queda la lista (REPORTE).
- Ambigüedades registradas:
  - A1. PLAN maestro F1.S2.M1 pide **422** para un cuerpo con aprobador; la plataforma ya rechaza campos no declarados con **400 AP-VAL-02** (`CuerposEstrictos`) y el anexo L4 (F1.L4.M05) también dice 400. Supuesto: se conserva 400 (contrato vigente). Confirmar con Pablo.
  - A2. Estados de `usuario.estado` que pueden autenticarse. Supuesto (lista blanca, falla cerrada): `ACTIVO`, `PRE_REGISTRO`, `INCOMPLETO_KYC`, `PENDIENTE_VERIFICACION` entran (hoy entran y el alta/KYC depende de eso); `SUSPENDIDO`, `BLOQUEADO_SEGURIDAD`, `BAJA_VOLUNTARIA`, `ELIMINADO` y cualquier estado desconocido no. Confirmar con Pablo.
  - A3. El secreto TOTP no puede guardarse como hash (verificar TOTP exige el secreto): se guarda **cifrado AES-256-GCM con versión de llave**, que es lo que ya modela `factor_mfa(secreto_cifrado, version_llave)`.

## H1 — F1.S2.M1 · Actor del token y cuatro ojos reales en `comun-web`
**CA:** Dado un acto registrado por A en su sesión, cuando la segunda firma llega con el token de A (misma u otra sesión) o sin token, entonces se rechaza; cuando llega con el token de B en otra sesión, entonces se acepta y se registran los dos actores tomados de token.
**DoD:** `./gradlew :plataforma:comun-web:test :plataforma:comun-web:webTest` en verde con las pruebas nuevas.
**Estado:** HECHO (peldaño TESTED; ver REPORTE)

### H1.S1 — Componente y pruebas en comun-web
**CA:** el del hito H1. **DoD:** el del hito H1. **Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `ActorAutenticado` (usuario + sesión del token) y `CuatroOjos` en `comun-web/seguridad`, reusando `SesionDeLaPeticion` | Regla pura: mismo usuario → rechazo; misma sesión → rechazo; sistema → rechazo; distinto → par de actores | `./gradlew :plataforma:comun-web:test --tests '*CuatroOjos*'` verde | HECHO |
| H1.S1.M2 | Prueba web: endpoint de ensayo con primera firma persistida y segunda firma por token; cuerpo con `aprobadoPor` → 400 AP-VAL-02 | Matriz: mismo token, mismo usuario otra sesión, sin token, cuerpo con aprobador, otro usuario | `./gradlew :plataforma:comun-web:webTest --tests '*CuatroOjos*'` verde | HECHO |
| H1.S1.M3 | Documentación de uso (`plataforma/comun-web/docs/cuatro-ojos.md`) | Explica cómo persistir el primer actor y pedir el segundo | Archivo existe y enlaza las clases | HECHO |

## H2 — F1.S3.M1 (F1.L4.M01) · El acceso mira `usuario.estado`
**CA:** Dada una cuenta suspendida/bloqueada/de baja, cuando intenta entrar o refrescar, entonces no obtiene token y su sesión queda revocada; dado un interno desbloqueado, cuando entra, entonces entra.
**DoD:** unitarias en `test` verdes; integración escrita (NO COMPROBADO sin Docker) con comando listo; SQL ejercitado con `psql` contra la base local.
**Estado:** HECHO (peldaño TESTED; ver REPORTE)

### H2.S1 — Login y refresco con estado de cuenta
**CA:** el del hito H2. **DoD:** el del hito H2. **Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Átomo `PuedeAutenticarse` (lista blanca de estados) | Cada estado de `ck_usuario_estado` tiene su veredicto; desconocido → no | `./gradlew :servicios:identidad:test --tests '*PuedeAutenticarse*'` | HECHO |
| H2.S1.M2 | `CU04Autenticar` rechaza cuenta no habilitada (después de la credencial, sin enumerar) | Suspendido con clave correcta → AP-CU04-02, sin sesión | integración `CU04CuentaNoHabilitadaTest` (Docker) | HECHO |
| H2.S1.M3 | `RenovarSesion` con cuenta no habilitada revoca la sesión y rechaza | Refresh de suspendido → rechazado y `sesion.revocada_en` no nulo | integración (Docker) + `psql` de la sentencia | HECHO |
| H2.S1.M4 | Desbloquear un interno libera `bloqueo_cuenta` (el modelo que lee el login) | Tras desbloquear, `tieneBloqueoVigente` = false | integración (Docker) + `psql` | HECHO |

## H3 — F1.S3.M2 (F1.L4.M02, parte de M03) · TOTP real y fin del `000000`
**CA:** Dado un operador con TOTP enrolado, cuando presenta `000000` u otro inválido o uno ya usado, entonces se rechaza; con el vigente, entra. Ningún perfil acepta un código fijo.
**DoD:** vectores RFC 6238 en verde; pruebas del verificador y del enrolamiento; arranque sin llave → falla.
**Estado:** HECHO (peldaño TESTED; ver REPORTE)

### H3.S1 — TOTP real y enrolamiento
**CA:** el del hito H3. **DoD:** el del hito H3. **Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Verificador TOTP (RFC 6238, ventana ±1 paso, anti-reuso) reusando `CodigoTotp` de comun-web | Vectores RFC; código usado o fuera de ventana → falso | `./gradlew :servicios:identidad:test --tests '*Totp*'` | HECHO |
| H3.S1.M2 | Llaves de factor obligatorias (`aportaya.mfa.llaves`, versión vigente), cifrado AES-GCM existente | Sin llave → el contexto no arranca | prueba de configuración en `test` | HECHO |
| H3.S1.M3 | Borrar `DesafioDeDesarrollo`; el TOTP se valida contra `factor_mfa`, el correo sigue en `DesafioLocal` | `grep '"000000"' servicios/identidad/src/main` → 0 | grep + `CableadoDelSegundoFactorTest` | HECHO |
| H3.S1.M4 | Enrolamiento del titular: iniciar (secreto cifrado, inactivo) y confirmar con código | Confirmar con código válido activa; inválido no | unitaria del caso de uso + integración (Docker) | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin `000000`, los operadores de TEST sin TOTP real no entran al backoffice | TEST inaccesible para operadores hasta enrolar | Enrolar como participante antes del rol, o F1.L4.M04 (invitación); avisado en el PR |
| `aportaya.mfa.llaves` obligatoria | El despliegue sin la variable no arranca | Avisado en el PR; compose fuera de alcance |
| Integración sin Docker | No se puede subir a VERIFIED acá | NO COMPROBADO con comando listo (regla 65: doble no aplica, es la base real) |

## Cambios al plan durante la ejecución
- H2: el DoD de integración pasó de «NO COMPROBADO» a corrida real: `BaseDeCU04` admite
  `APORTAYA_TEST_DB_URL`, y las 9 clases que la extienden corrieron contra la Postgres local.
- H3.S1.M4: el DoD «integración (Docker)» se cumplió con `CU04TotpTest` contra la Postgres local.
- Un solo PR con un commit por microtarea del plan maestro (ver Desvíos en el REPORTE).
