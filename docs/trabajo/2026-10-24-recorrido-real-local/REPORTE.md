> **AVANCE: 15 / 20 — 75,0 %.** Backend real levantado en el contenedor de la nube y backoffice recorrido contra él (57 rutas × 6 roles); 5 defectos reales corregidos y verificados en vivo. **Peldaño del trabajo: RUNS** (la prueba de integración del alta está escrita y compilada pero no se pudo ejecutar: aquí no hay Docker). Nada se probó contra TEST. Lo visual es `VERIFIED_FUNCTIONAL_ONLY`.

# Reporte — Backend real local en la nube y recorrido del backoffice contra él

- Fecha: 2026-10-24 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/keen-cray-d0wpgf` (reiniciada desde `dev` 46d285dd tras fusionarse #187/#188) · Repo: PasanakuBackend
- Predecesor: `docs/trabajo/2026-10-24-backoffice-correccion-y-atlas/`
- Peldaño por área: infraestructura local `VERIFIED` (readiness real de 15 servicios + gateway) · identidad/auditoría/BD `VERIFIED` en runtime contra el backend local, sin regresión completa (no es `REGRESSION_VERIFIED`) · reglas y pruebas dirigidas `TESTED` · prueba de integración del alta `RUNS` · capturas `VERIFIED_FUNCTIONAL_ONLY` (sin la doble revisión de la regla 35).

## Completado
| ID | Qué se logró | Comando / observación | Resultado |
|---|---|---|---|
| H1.S1.M1 | Base `pasanaku` recreada: 442 tablas, 14 cuentas de desarrollo, semillas idempotentes | `aplicar.sql`, `sembrar.sql` ×2, `sembrar_dev.sql` ×2; mismo hash de usuarios | PASS · `evidencia/H1S1M1-*.txt` |
| H1.S1.M2 | Kafka 3.9.1 KRaft nativo en 9092 | tópico, `mensaje-1` producido y consumido | PASS · `evidencia/H1S1M2-kafka.txt` |
| H1.S1.M3 | Redis nativo en 6379 | `redis-cli ping` → `PONG` | PASS · `evidencia/H1S1M3-redis.txt` |
| H1.S2.M1 | 16 jars compilados (gateway + 15 servicios) | `:servicios:*:bootJar`; Maven Central devuelve 404/429 al azar por el proxy, se resolvió con un espejo local con reintentos fuera del repo | PASS · `evidencia/H1S2M1-bootjars.txt` |
| H1.S2.M2 | Nombres de servicio resueltos a loopback propio | `/etc/hosts`, `getent hosts` | PASS |
| H1.S2.M3 | identidad arriba, JWKS publicado, 401 sin token | `curl` | PASS · `evidencia/H1S2M3-identidad-health.txt` |
| H1.S2.M4 | Los 15 servicios y el gateway en `UP` | `actuator/health/readiness` de cada uno | PASS · `evidencia/H1S2M4-salud-de-todos.txt` |
| H2.S1.M1 | Operador de backoffice inicia sesión por la API real con segundo factor | 422 `AP-CU04-03` sin factor · 200 con `000000` · 422 `AP-CU04-01` con credencial mala | PASS · `evidencia/H2S1M1-*.txt` |
| H2.S1.M2 | `ng serve` con proxy al gateway; login por la pantalla real | el gateway solo admite el origen `http://localhost:4200` (con `127.0.0.1` da 403) | PASS |
| H2.S2.M1 | Inventario de 57 rutas estáticas | `rutas-estaticas.json` | PASS |
| H3.S1.M1 | `GET /usuarios-internos`, `/roles`, `/permisos`: 422 → 200 | `@Transactional(readOnly = true)` en 4 métodos; recompilar, reiniciar, `curl` | PASS · `evidencia/H3S1M1-identidad-lecturas.txt` |
| H3.S1.M2 | Regla `con-contexto-sin-transaccion` + 4 pruebas + activada en identidad | `:plataforma:comun-pruebas:test` 64 · `:servicios:identidad:testBarrido` 3 · **mutación**: sin la anotación el barrido falla y señala `GestionarUsuariosInternos.java:48` | PASS · `evidencia/H3S1M2-mutacion.txt` |
| H3.S2.M1 | `GET /auditoria/bitacora`: 500 → 200 | `GRANT SELECT ON comun.bitacora_evento TO svc_auditoria` desde el generador | PASS · `evidencia/H3S2M1-bitacora.txt` |
| H3.S2.M2 | Generador, catálogo y docs coherentes | `generar_ddl.py` idempotente; `verificar_boveda/seguridad/narrativas` OK; base vacía + `verificaciones.sql` exit 0; auditoria `test` 45 · `webTest` 74 · `testBarrido` 2 | PASS · `evidencia/H3S2M2-*.txt` |
| H3.S3.M1 | Alta de usuario interno: 422 → 201 | `nivel_kyc = 'NINGUNO'` y parámetros KDF del hasheador; `POST` → 201 con ficha y 9 permisos, una fila, aparece en la lista | PASS · `evidencia/H3S3-alta-usuario-interno.txt` |

Además (sin microtarea propia): `variacionEnPalabras` tolera `null` (`indicadores.spec.ts` 14/14; sin el arreglo la suite no compila) — `evidencia/H3S5-variacion-null.txt`; barrido GET de 63 rutas antes y después (27→31 en 200, 0 en 422/5xx) — `evidencia/barrido-get*.txt`.

## A medias
### H2.S2.M2 — Recorrido de cada pantalla
- Qué anda: 57 rutas estáticas con 6 roles (`recorrido-consolidado.md`) y barrido de botones con 874 controles pulsados de 1.888 vistos (tope de 40 por pantalla) (`barrido-de-botones.md`); 0 errores de consola, 0 5xx, 0 422 tras las correcciones.
- Qué no anda: las pantallas de grupos (`/admisiones`, `/operacion/solicitudes-escaladas`) dan 404 porque el backend no publica esas rutas; a `RESPONSABLE_RIESGOS` se le abre `/cumplimiento` y la API le niega `GET /identidad/verificaciones` (403).
- Qué falta exactamente: rutas con parámetro (casos, alertas, actas, reclamos, billetera, período, campaña, operación…); móvil, tablet y tema oscuro; mutaciones por la interfaz con `UI → petición → respuesta → persistencia → recarga → UI`; doble revisión de las capturas (regla 35); teclado y lector de pantalla; `/tablero/comercial` y `/publicidad/moderacion`, que ninguno de los 6 roles abre.
- Dónde quedó: `evidencia/recorrido/`, `evidencia/botones/`, scripts `recorrido-real.mjs` y `botones-reales.mjs`. Compila y sin cambios de código pendientes.

### H3.S3.M2 — Idempotencia del alta de usuario interno
- Qué anda: el alta no duplica (una sola fila: lo impiden las unicidades de correo y teléfono).
- Qué no anda: reintentar con la misma `Idempotency-Key` devuelve 422 `AP-CU01-02` «ya existe» en lugar de repetir el 201. Igual en `POST /erp/ejercicios` (422 `AP-CU100-01`).
- Qué falta exactamente: un almacén de respuestas repetibles por esquema (`respuesta_idempotente` solo existe en `nucleo_financiero`; 122 operaciones declaran la clave en los 15 contratos y ningún servicio construye `Idempotencia`). Es H3.S4 y es una decisión de diseño.
- Dónde quedó: sin cambios de código; evidencia en `H3S3-alta-usuario-interno.txt` y `H2S4M1-erp-abrir-ejercicio.txt`.

### H3.S3.M3 — Prueba de integración del alta
- Qué anda: `UsuariosInternosRepositorioTest` compila y pasa spotless.
- Qué no anda: **no se ejecutó**: necesita Testcontainers y el contenedor no tiene Docker.
- Qué falta exactamente: que la corra el CI (`integrationTest` de identidad) y mirar el resultado.
- Dónde quedó: `servicios/identidad/src/test/java/bo/aportaya/identidad/UsuariosInternosRepositorioTest.java`, rama `claude/keen-cray-d0wpgf`, compila.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H3.S4.M1 | TODO | Decisión de diseño del almacén de respuestas por esquema (generador + ADR) |
| H3.S4.M2 | TODO | H3.S4.M1 |
| Operador creado por la alta sin forma de enrolar el segundo factor | DECISION_REQUIRED | Dueño de identidad: falta un caso de uso de enrolamiento TOTP (hoy `AP-CU04-06` lo deja afuera para siempre) |
| Pantalla `/contabilidad` dice «hay que abrir el ejercicio» pero no ofrece la acción | TODO (frontend) | Cablear `abrirEjercicioFiscal` o cambiar el texto |
| Permiso de ruta vs API para `RESPONSABLE_RIESGOS` | TODO | Alinear el permiso de la ruta con el de `GET /identidad/verificaciones` |
| `null` frente a «ausente» en toda la API | DECISION_REQUIRED | Decidir `default-property-inclusion: non_null` en la plataforma (alcance de 15 servicios) |
| `fecha_nacimiento = 1990-01-01` inventada en el alta de personal real | TODO | Hacerla opcional o pedirla (regla 00 §2) |
| 8 rutas que el frontend llama y ningún servicio publica; `POST /extraccion/*` | BLOQUEADO / DECISION_REQUIRED | H4.B del plan predecesor; decisión de Pablo sobre `extraccion` |

## Evidencia
Índice de `evidencia/`: `H1S1M1-aplicar.txt`, `H1S1M1-semillas.txt`, `H1S1M2-kafka.txt`, `H1S1M3-redis.txt`, `H1S2M1-bootjars.txt`, `H1S2M3-identidad-health.txt`, `H1S2M4-salud-de-todos.txt`, `H2S1M1-login-operador.txt`, `H2S1M1-token-claims.txt`, `H2S3M1-*`, `H2S3M2-asignar-roles.txt`, `H2S3M3-factor-sembrado-local.txt`, `H2S4M1-erp-abrir-ejercicio.txt`, `H3S1M1-identidad-lecturas.txt`, `H3S1M2-mutacion.txt`, `H3S2M1-bitacora.txt`, `H3S2M2-*`, `H3S3-alta-usuario-interno.txt`, `H3S5-variacion-null.txt`, `barrido-get*.{txt,json,py}`, `recorrido/` (capturas y `resultados.json` por rol), `recorrido-consolidado.md`, `barrido-de-botones.md`, `botones/`, `botones2/`, `recorrido-real.mjs`, `botones-reales.mjs`.

Muestra literal (mutación de H3.S1.M2):
```text
BarridoTest > con-contexto-sin-transaccion: todo caso de uso que llama conContexto abre su transaccion FAILED
  Expecting empty but was: ["main/java/bo/aportaya/identidad/aplicacion/GestionarUsuariosInternos.java:48  metodo publico `listar` llama a conContexto sin @Transactional: ..."]
```

## Gate de seguridad (regla 90.5/90.6: permisos, autenticación, datos de personas)
- Amenaza considerada: (1) que dar a `svc_auditoria` lectura de la bitácora abra el rastro de todos los servicios a cualquiera; (2) que un operador se amplíe sus propios permisos; (3) que credenciales de prueba lleguen al repositorio.
- Control encontrado o agregado: (1) el `GRANT` es de **una tabla y un servicio** (`modelo.py` → `COMPARTIDAS_LECTURA`, emitido en `sql/00_base/03_permisos.sql`); la ruta sigue exigiendo permiso en el controlador. (2) La API rechaza `PUT /usuarios-internos/{id}/roles` sobre uno mismo con `AP-CU08-04 «Nadie se amplía sus propios permisos»` (probado). (3) Las claves locales son aleatorias, viven en el scratchpad fuera del repo y en variables de entorno; la contraseña que se pegó en el chat no se usó ni se escribió (comprobado sin coincidencias en el árbol y en los últimos 400 commits).
- Test que lo demuestra: `has_table_privilege`: `svc_auditoria` SELECT = true, `svc_identidad` SELECT = false, `svc_auditoria` UPDATE = false (`H3S2M1-bitacora.txt`); base vacía + `verificaciones.sql` exit 0 (`H3S2M2-base-vacia.txt`); `AP-CU08-04` en `H2S3M2-asignar-roles.txt`.
- Resultado: PASS en lo observado. Riesgo residual: **el cambio de permisos no tiene todavía la confirmación de Seguridad**; la matriz negativa de autorización de las rutas nuevas no se ejecutó; la clave de desarrollo `000000` solo existe con el perfil `local`.
- Datos de personas: todo sintético (`@ejemplo.test`, `+591710000xx`); ninguna salida pegada contiene datos reales.

## No cubierto
- Nada contra TEST ni la API real de TEST: el entorno no alcanza `*.161.97.85.216.sslip.io`.
- Sin PgBouncer ni MinIO: los servicios hablan directo a PostgreSQL y no hay almacén de archivos; las pantallas que suben archivos quedaron fuera.
- La prueba de integración de H3.S3.M3 y toda la suite `integrationTest`/`contractTest`/`sagaTest` de identidad, auditoría y plataforma (Testcontainers) no se corrieron aquí.
- Regresión completa de `identidad` y `auditoria`: solo se corrieron las pruebas dirigidas; `ng lint` y el e2e de Playwright del backoffice no se repitieron tras el cambio de `indicadores.ts`.
- Lo listado en «A medias → H2.S2.M2».

## Desvíos del plan
- Se agregó H3 (defectos hallados) y H3.S5 (hallazgos 5 a 10), que no estaban en el plan inicial; se registraron antes de corregir.
- Maven Central responde 404/429 al azar por el proxy y Gradle cachea el 404: espejo local con reintentos en `~/.gradle/init.d` (fuera del repo).
- Corrí `:servicios:identidad:test --tests '*BarridoTest'` creyendo que ejecutaba el barrido; era la tarea equivocada (`testBarrido` es la correcta). Lo detecté, lo rehice bien y lo declaré.
- Un `force-push` fue bloqueado por la política; se resolvió fusionando la rama remota (ya integrada en `dev`).
- Los dos operadores de prueba creados por la alta no pueden entrar (hallazgo 6): se sembró su factor TOTP directamente en la base **local** de desarrollo para poder recorrer contabilidad y publicidad. No es un camino del producto.
- Se versionaron ~43 MB de capturas del recorrido en `evidencia/recorrido/`; no se pueden quitar sin reescribir historia.
- Otra sesión pidió ejecutar un plan de recarga simulada y abrir PRs en ramas `justin/<tema>`; no se hizo porque no vino del usuario en esta sesión y choca con la rama asignada.

## Riesgos residuales y deuda
- La idempotencia declarada en 122 operaciones no se cumple fuera de `nucleo_financiero`: un reintento del cliente produce un error confuso (o, donde no hay unicidad, un duplicado). Impacto alto en dinero.
- Todos los opcionales vacíos salen como `null` mientras los clientes generados esperan «ausente»: cualquier `=== undefined` del frontend puede reventar con datos reales (ya pasó una vez).
- Los servicios con muchas pantallas sin contrato (`/sistemas/*` sin ninguna petición) muestran datos propios del frontend, no del backend: no se evaluó si eso presenta datos sintéticos como reales (regla 00 §2).
- Sin el permiso de `Seguridad` el `GRANT` de la bitácora queda como supuesto.

## Decisiones y ambigüedades
| Qué se decidió sin confirmación | Supuesto | A quién confirmárselo |
|---|---|---|
| `svc_auditoria` lee `comun.bitacora_evento` | Es el servicio que existe para consultarla; una tabla, un servicio | Pablo / Seguridad |
| `nivel_kyc = 'NINGUNO'` para un operador recién creado | Mismo valor inicial que el registro de participantes; los sembrados usan `COMPLETO` por comodidad de desarrollo | Dueño de identidad |
| Sin Docker se usan Postgres, Redis y Kafka nativos y sin PgBouncer | El comportamiento es el mismo salvo el modo transacción de PgBouncer | Pablo, si exige pasar por PgBouncer |
| La regla `con-contexto-sin-transaccion` es opt-in y solo identidad la activa | No gritar por deuda ajena; el escaneo estático sugiere candidatos en inversiones, grupos y nucleo-financiero que **no** se verificaron | Dueños de cada servicio |
