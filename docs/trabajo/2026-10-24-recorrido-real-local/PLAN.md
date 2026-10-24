# Plan — Backend real levantado en el contenedor de la nube y recorrido del backoffice contra él

- Fecha: 2026-10-24 · Repos afectados: PasanakuBackend · Predecesor: `docs/trabajo/2026-10-24-backoffice-correccion-y-atlas/` (H1 y H2 de ese plan: «recorrido contra API real», que estaba bloqueado porque el entorno no alcanza TEST)
- Resultado observable: en este contenedor corren PostgreSQL, Kafka, Redis, el gateway y los servicios que el backoffice llama; un operador de backoffice inicia sesión por la API real (con segundo factor) y cada pantalla del backoffice se abre contra ese backend, con captura, consola y red revisadas.
- Kill-test: `curl` al gateway local en `/api/v1/sesiones` sin credenciales no devuelve 401/400 del servicio real (devuelve conexión rechazada o 502), o el inicio de sesión del operador falla.

## Alcance
- IN: levantar la infraestructura local sin Docker (no hay demonio), compilar y arrancar los servicios, sembrar esquema + catálogos + cuentas de desarrollo en una base **de este contenedor**, servir el backoffice con `ng serve` contra el gateway local, recorrer las pantallas, registrar defectos.
- OUT: TEST (no alcanzable desde aquí), producción, cualquier corrección de código de otros carriles, el paquete `diseno_flutter`. Los defectos que aparezcan se **registran** y, si son del backoffice o del backend de los carriles de este trabajo, se agregan al plan como microtarea.
- Secretos: la contraseña que Justin usa en TEST **no se usa ni se escribe**. Las claves locales (pimienta, firma, cuenta de operador) son aleatorias, viven en variables de entorno y en un archivo del scratchpad fuera del repositorio.
- Ambigüedades registradas:
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

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| 15 JVM en 16 GB y 4 núcleos | arranque lento o OOM | heap acotado por servicio; arrancar por grupos; solo los que el backoffice llama primero |
| jOOQ necesita la base viva al compilar | `compileJava` falla | la base se levanta y siembra **antes** de compilar |
| Maven Central responde 429 por el proxy | compilación lenta | `--max-workers=1` y reintentos (ya probado) |
| Rutas sin servicio (8 conocidas) | pantallas con error | se espera el error accionable; es el hallazgo H4.B del plan predecesor |
