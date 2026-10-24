# Plan — Backend real levantado en el contenedor de la nube y recorrido del backoffice contra él

- Fecha: 2026-10-24 · Repos afectados: PasanakuBackend · Predecesor: `docs/trabajo/2026-10-24-backoffice-correccion-y-atlas/` (H1 y H2 de ese plan: «recorrido contra API real», que estaba bloqueado porque el entorno no alcanza TEST)
- Resultado observable: en este contenedor corren PostgreSQL, Kafka, Redis, el gateway y los servicios que el backoffice llama; un operador de backoffice inicia sesión por la API real (con segundo factor) y cada pantalla del backoffice se abre contra ese backend, con captura, consola y red revisadas.
- Kill-test: `curl` al gateway local en `/api/v1/sesiones` sin credenciales no devuelve 401/400 del servicio real (devuelve conexión rechazada o 502), o el inicio de sesión del operador falla.

## Alcance
- IN: levantar la infraestructura local sin Docker (no hay demonio), compilar y arrancar los servicios, sembrar esquema + catálogos + cuentas de desarrollo en una base **de este contenedor**, servir el backoffice con `ng serve` contra el gateway local, recorrer las pantallas, registrar defectos.
- OUT: TEST (no alcanzable desde aquí), producción, cualquier corrección de código de otros carriles, el paquete `diseno_flutter`. Los defectos que aparezcan se **registran** y, si son del backoffice o del backend de los carriles de este trabajo, se agregan al plan como microtarea.
- Secretos: la contraseña que Justin usa en TEST **no se usa ni se escribe**. Las claves locales (pimienta, firma, cuenta de operador) son aleatorias, viven en variables de entorno y en un archivo del scratchpad fuera del repositorio.
- Ambigüedades registradas:
  - H3.S2: `modelo.py` dice que el SELECT de las bitácoras compartidas es solo de `rol_auditor`; el servicio `auditoria` las consulta con `svc_auditoria`. Supuesto tomado: se otorga SELECT sobre `comun.bitacora_evento` (solo esa tabla) a `svc_auditoria`, porque es el servicio que existe para consultarla. Confirmar con Pablo / Seguridad: es un cambio de permisos (regla 90.5).
  - Sin Docker: se usa Postgres, Redis y Kafka nativos y los servicios con `java -jar`. Supuesto: el comportamiento es el mismo que en contenedores salvo PgBouncer (los servicios hablan directo a Postgres) y MinIO (sin almacén de archivos; las pantallas que suben archivos quedan fuera del recorrido y se declaran). Confirmar con Pablo si exige pasar por PgBouncer.

## H1 — Infraestructura y servicios arriba, en la nube (HECHO: 15 servicios + gateway en `UP`, ver evidencia/H1S2M4-salud-de-todos.txt)
**CA:** Dado este contenedor, cuando se consulta `/actuator/health/readiness` de cada servicio y del gateway, entonces responden `UP` (o se documenta cuál no y por qué).
**DoD:** salida literal de los health de cada uno, `ps` con los procesos, y declarar qué quedó sin MinIO/PgBouncer.
**Estado:** TODO

### H1.S1 — Datos y mensajería
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Base `pasanaku` recreada desde cero (esquema + catálogos + dev) | `aplicar.sql` y `sembrar.sql` salen 0; `sembrar_dev.sql` dos veces igual | `psql` conteos: tablas, usuarios dev | HECHO |
| H1.S1.M2 | Kafka KRaft nativo en 9092 | un tópico de prueba produce y consume un mensaje | consola del tópico | HECHO |
| H1.S1.M3 | Redis nativo en 6379 | `PING` → `PONG` | `redis-cli ping` | HECHO |

### H1.S2 — Servicios
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Compilar los jars del gateway y de los servicios | `bootJar` de cada módulo sale 0 | `ls build/libs` | HECHO (16 jars; espejo local con reintentos por el 404/429 aleatorio del proxy) |
| H1.S2.M2 | Nombres de servicio resueltos a loopback propio (`/etc/hosts`) y secretos locales generados fuera del repo | cada nombre resuelve a su IP | `getent hosts` | HECHO |
| H1.S2.M3 | Arrancar identidad y esperar readiness | `UP` y JWKS publicado | `curl` al JWKS | HECHO |
| H1.S2.M4 | Arrancar el resto de servicios y el gateway | readiness `UP` en cada uno | health literal | HECHO |

## H2 — Sesión real y recorrido
**CA:** Dado el backend local, cuando el operador de backoffice (`+59171000091`, clave local aleatoria) inicia sesión y recorre las pantallas, entonces cada una muestra datos, vacío o error accionable, sin errores de consola ni 5xx inesperados, o queda registrado el defecto.
**DoD:** por pantalla: ruta, estado observado, petición de red y respuesta, captura inspeccionada; resumen en `REPORTE.md`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Inicio de sesión del operador por la API real, con segundo factor | token de acceso válido | `curl` literal (token enmascarado) | HECHO |
| H2.S1.M2 | `ng serve` del backoffice con proxy al gateway local | `GET /` y `GET /api/v1/...` responden desde el navegador | Playwright | TODO |
| H2.S2.M1 | Inventario de rutas del backoffice (de `rutas.ts`) y su servicio | lista completa con la ruta de API de cada una | tabla en evidencia | TODO |
| H2.S2.M2 | Recorrido de cada pantalla (se desglosa por grupo al llegar) | ver CA de H2 | evidencia por pantalla | TODO |

## H3 — Defectos reales que el recorrido contra el backend vivo destapó
**CA:** Dado el backend local, cuando el operador abre Usuarios internos, Roles internos, Permisos internos y Bitácora, entonces cada pantalla carga datos reales (o vacío) sin 422/500.
**DoD:** `curl` por el gateway con el token del operador devuelve 200 en las cuatro rutas; el barrido GET del contrato queda sin 422 `AP-DOM-01` ni 5xx; tests dirigidos del servicio en verde; deriva del esquema en verde si cambia el generador (regla 97).
**Estado:** HECHO salvo hallazgo 3 (rutas de grupos, fuera de este plan) — barrido GET tras las correcciones: 31×200, 2×400, 30×403 por permisos, 0×422, 0×5xx (evidencia/barrido-get-despues.txt)
**Hallazgos (evidencia/barrido-get.txt y recorrido/escritorio-1440.log):**
1. `GET /usuarios-internos`, `/roles`, `/permisos` → 422 `AP-DOM-01` «conContexto exige una transacción abierta»: `GestionarUsuariosInternos.listar/consultar/listarRoles/listarPermisos` llaman a `Datos.conContexto` sin `@Transactional`.
2. `GET /auditoria/bitacora` → 500: `svc_auditoria` solo tiene INSERT sobre `comun.*` y `ConsultarBitacora` hace SELECT sobre `comun.bitacora_evento` (SQLSTATE 42501). El comentario de `scripts/modelo.py` dice que el SELECT es solo de `rol_auditor`: el servicio que existe para consultar la bitácora quedó sin permiso. Decisión de seguridad registrada (ver Ambigüedades).
3. `GET /grupos/solicitudes` y `GET /grupos/solicitudes-escaladas` → 404: rutas sin servicio (H4.B del plan predecesor, no se repite aquí).

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H3.S1.M1 | `@Transactional(readOnly = true)` en los cuatro métodos de lectura de `GestionarUsuariosInternos` | las tres rutas devuelven 200 por el gateway | `curl` literal tras recompilar y reiniciar identidad | HECHO (evidencia/H3S1M1-identidad-lecturas.txt) |
| H3.S1.M2 | Prueba dirigida que habría atrapado M1 | falla sin el arreglo y pasa con él | test de identidad en verde + mutación | HECHO: nueva regla `con-contexto-sin-transaccion` (comun-pruebas) + 4 pruebas dirigidas + activada en identidad; mutación probada (evidencia/H3S1M2-mutacion.txt) |
| H3.S2.M1 | `svc_auditoria` puede leer `comun.bitacora_evento` (excepción enumerada en `modelo.py`, generada en `03_permisos.sql`) | `GET /auditoria/bitacora` → 200 | `curl` + `generar_ddl.py` sin diff extra + verificaciones.sql | HECHO (evidencia/H3S2M1-bitacora.txt: GET /auditoria/bitacora 200; svc_identidad sigue sin SELECT) |
| H3.S2.M2 | Catálogo de datos y docs regenerados coherentes con la excepción | `catalogo_de_datos.py` y `verificar_narrativas.py` en verde | salida literal | HECHO (evidencia/H3S2M2-*.txt: generar_ddl idempotente, verificar_boveda/seguridad/narrativas OK, base vacía + verificaciones.sql exit 0, auditoria test 45 / webTest 74 / testBarrido 2) |

### H3.S3 — Alta de usuario interno (hallazgo 4, destapado al probar el botón «Alta» por la API real)
**CA:** Dado el operador con permiso de gestión de usuarios internos, cuando da de alta a una persona con un rol, entonces recibe 201 con su ficha, el reintento con la misma `Idempotency-Key` no duplica, y la persona aparece en la lista.
**DoD:** `POST /usuarios-internos` → 201 por el gateway; mismo POST repetido → misma ficha; `GET /usuarios-internos` la lista; prueba de integración escrita para CI.
**Estado:** EN CURSO
**Hallazgos:** `UsuariosInternosRepositorio.crear` (a) inserta `nivel_kyc = 'ESTANDAR'`, valor que `ck_usuario_nivel_kyc` rechaza (BASICO, COMPLETO, INTERMEDIO, NINGUNO) → 422 `AP-BD-03`; (b) no informa `credencial_acceso.parametros_kdf` (NOT NULL) y escribe el algoritmo a mano; (c) NO CORREGIDO, fuera de alcance: inserta `fecha_nacimiento = 1990-01-01` inventada para personal real (regla 00 §2) — se registra como riesgo.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S3.M1 | `nivel_kyc = 'NINGUNO'` (el valor inicial que ya usa `RegistroRepositorio`) y parámetros KDF desde `HasheadorDeCredencial.parametros()` | el alta devuelve 201 | recompilar, reiniciar identidad, `curl` | HECHO: `POST /usuarios-internos` → 201 con la ficha y 9 permisos de CONTABILIDAD (evidencia/H3S3-alta-usuario-interno.txt) |
| H3.S3.M2 | Reintento con la misma `Idempotency-Key` no duplica | misma respuesta, una sola fila | `curl` ×2 + `select count(*)` | **NO CUMPLE**: el reintento con la misma clave devuelve 422 `AP-CU01-02` (no duplica: una sola fila) en vez de repetir el 201. `identidad` no tiene `respuesta_idempotente` (solo `nucleo_financiero`); implementarlo toca el generador de esquema → H3.S4 |
| H3.S3.M3 | Prueba de integración del repositorio (alta y lectura contra Postgres real) | pasa en CI | **solo escrita: aquí no hay Docker, no se puede correr** (peldaño WRITTEN) | **A MEDIAS (peldaño RUNS, no TESTED)**: `UsuariosInternosRepositorioTest` escrita, compila y pasa spotless; NO ejecutada — necesita Testcontainers y aquí no hay Docker. Falta que la corra el CI (`integrationTest`) |

### H3.S4 — Idempotencia real en el alta de usuario interno (pendiente, abierta por M2)
**CA:** Dado el mismo `POST /usuarios-internos` con la misma `Idempotency-Key`, entonces la segunda respuesta es la primera (201, misma ficha), no un 422.
**DoD:** `respuesta_idempotente` en el esquema `identidad` por el generador (regla 97), `Idempotencia` aplicada en el controlador, prueba de idempotencia, deriva en verde.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S4.M1 | Inyectar `respuesta_idempotente` en `identidad` desde `generar_ddl.py` | tabla creada por `aplicar.sql` | `aplicar.sql` + deriva | TODO |
| H3.S4.M2 | Aplicar `Idempotencia` en `crearUsuarioInterno` | mismo POST dos veces → misma respuesta | `curl` ×2 | TODO |

### H3.S5 — Hallazgos del recorrido por roles y del barrido de botones
| # | Hallazgo (evidencia) | Tratamiento |
|---|---|---|
| 5 | Tablero de sistemas: `TypeError ... reading 'startsWith'` en `variacionEnPalabras`: el backend real manda `variacionPeriodoAnterior: null` (2 de 3 indicadores) y el contrato dice «ausente» | **HECHO** en el frontend: tolera `null` + prueba (evidencia/H3S5-variacion-null.txt). Causa de fondo, SIN corregir: Spring serializa los opcionales vacíos como `null` en todos los servicios mientras los clientes generados esperan «ausente» → hallazgo sistémico, decisión de diseño (`default-property-inclusion: non_null` en la plataforma tiene mucho alcance) |
| 6 | Un operador creado por la alta NO puede iniciar sesión jamás: R-SEG-10 exige TOTP enrolado y no existe ningún endpoint de enrolamiento (`AP-CU04-06`). Solo los operadores sembrados tienen factor | **DECISION_REQUIRED** (falta un caso de uso). Para el recorrido se sembró el factor en la base local (evidencia/H2S3M3-factor-sembrado-local.txt) |
| 7 | `Idempotency-Key`: 122 operaciones la declaran en los 15 contratos; solo `nucleo_financiero` tiene `respuesta_idempotente` y ningún servicio construye `Idempotencia`. Reintentar con la misma clave da 422 «ya existe» (comprobado en `POST /usuarios-internos` y `POST /erp/ejercicios`) en vez de repetir la respuesta | **ABIERTO** (H3.S4 ampliada): decisión de diseño del almacén de respuestas por esquema (generador + ADR) |
| 8 | `/contabilidad` dice «hay que abrir el ejercicio» pero la pantalla no ofrece la acción, aunque la API `abrirEjercicioFiscal` existe; la base de desarrollo no trae ejercicios | **ABIERTO** (frontend). Probado por la API: 201 con 12 períodos y `GET /erp/periodos/vigente` 200 (evidencia/H2S4M1-erp-abrir-ejercicio.txt) |
| 9 | `RESPONSABLE_RIESGOS` en `/cumplimiento`: la pantalla llama `GET /identidad/verificaciones` y el rol no tiene permiso (403) aunque la ruta se le abre | **ABIERTO**: alinear permiso de ruta y de API |
| 10 | Rutas `/tablero/comercial` y `/publicidad/moderacion` no las abre ninguno de los 6 roles probados | **ABIERTO**: probar con el rol que corresponda (¿EXECUTIVE_READONLY?, ¿moderación?) |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| 15 JVM en 16 GB y 4 núcleos | arranque lento o OOM | heap acotado por servicio; arrancar por grupos; solo los que el backoffice llama primero |
| jOOQ necesita la base viva al compilar | `compileJava` falla | la base se levanta y siembra **antes** de compilar |
| Maven Central responde 429 por el proxy | compilación lenta | `--max-workers=1` y reintentos (ya probado) |
| Rutas sin servicio (8 conocidas) | pantallas con error | se espera el error accionable; es el hallazgo H4.B del plan predecesor |
