# Higiene de dependencias

> H6.S2.M4 del carril PR4-seguridad (Marcelo). Sin upgrades majors — regla del
> carril. Este documento es el inventario y las decisiones; los parches/minors con
> CVE aplicables en `aportes` se hacen uno por commit (H6.S2.M5).

## Estado: HECHO (árbol real corrido, con JDK 21 nativo)

`./gradlew :servicios:aportes:dependencies --configuration runtimeClasspath`
corrido de verdad (`EXIT=0`, árbol completo de 406 líneas). Las dos hipótesis
de esta corrida se **confirmaron**, no quedaron como candidatos:

```text
$ grep -rn "resilience4j\|@CircuitBreaker\|@Retry\|@RateLimiter\|@Bulkhead\|@TimeLimiter" \
    servicios/aportes/src/main --include=*.java
(sin resultados)

$ grep -rn "SchedulerLock\|shedlock\|LockProvider\|@Scheduled" \
    servicios/aportes/src/main --include=*.java
(sin resultados)
```

## Catálogo de versiones (`gradle/libs.versions.toml`) — lo que `aportes` usa

`aportes` depende, vía `plataforma/comun-{dominio,datos,web,mensajeria}` y
directamente, de (según `servicios/aportes/build.gradle.kts`):

| Dependencia | Para qué | Nota |
|---|---|---|
| `spring.boot.web` | MVC, el servidor embebido | Sin uso detectado fuera de lo esperado |
| `spring.boot.actuator` | Salud/métricas | — |
| `spring.boot.validation` | Bean Validation en el contrato generado | — |
| `jooq` | Acceso a datos | Versión fijada junto con el plugin generador (`aportaya.libreria.gradle.kts` exige que coincidan) |
| `kafka` | Cliente para el outbox | `aportes` no tiene consumidor propio, solo productor vía `Outbox` |
| `shedlock` | Locks distribuidos para tareas programadas | **HALLAZGO CONFIRMADO: declarada, CERO uso.** Ni `@SchedulerLock`, ni `LockProvider`, ni siquiera un `@Scheduled` en todo `servicios/aportes/src/main` — no hay ningún job programado en este servicio hoy, así que la dependencia no tiene nada que proteger todavía. |
| `resilience4j` | Circuit breaker / retry | **HALLAZGO CONFIRMADO: declarada, CERO uso.** `aportes` no tiene clientes HTTP salientes propios (ver `security-matrix.md` §API7) y no hay una sola anotación `@CircuitBreaker`/`@Retry`/`@RateLimiter`/`@Bulkhead`/`@TimeLimiter` en el módulo. |
| `micrometer` | Métricas | — |
| `bundles.pruebas` (test) | JUnit 5, AssertJ, Testcontainers, ArchUnit | — |

**Nueva dependencia de este carril**: `com.fasterxml.jackson.databind` (ya transitiva
de `spring-boot-starter-web`, usada explícitamente en
`CU100RecibirWebhookPasarela` para parsear el payload del webhook DESPUÉS de
verificar la firma). No es una dependencia nueva en el catálogo — no se tocó
`libs.versions.toml` para esto.

## Sin uso / obsoletas / duplicadas / transitivas innecesarias

**Confirmado con el árbol real y con grep del código fuente** (no inferencia):
`shedlock` y `resilience4j` están declaradas en `servicios/aportes/build.gradle.kts`
pero **ninguna de las dos tiene un solo punto de uso** en `servicios/aportes/src/main`.

**No se retiran en este carril** (regla 00, no se resuelve por conveniencia):
quitar una dependencia de otro servicio o del propio `aportes` sin que el dueño
del servicio lo confirme es un cambio de superficie, no de higiene — ambas
podrían estar ahí a propósito, para un trabajo programado a corto plazo
(`shedlock`) o como defensa preventiva declarada pero no cableada todavía
(`resilience4j`). Queda como hallazgo con la evidencia exacta (comando + cero
resultados), para que quien tenga el contexto de producto decida si se
retiran o se activan.

No se detectaron duplicados de versión ni transitivas conflictivas en el árbol
de `runtimeClasspath` de `aportes` (sin líneas `-> ` de resolución forzada por
conflicto salvo las ya conocidas y gestionadas por el BOM de Spring Boot).

## CVE con parche/minor disponible

**TODO** — exige acceso a OSV/una base de vulnerabilidades real contra las
versiones fijadas en `gradle/libs.versions.toml`. El script real de esto es
responsabilidad de Pablo (`ci(security): add OSV scanner`, H6.S2 del plan madre,
fuera de este carril salvo aplicar el parche puntual una vez que exista el reporte).

## Cómo se corrió (para quien quiera reproducirlo)

```bash
export BD_URL_ADMIN="jdbc:postgresql://localhost:5543/pasanaku"
export BD_USUARIO_ADMIN="pasanaku"
export BD_CLAVE_ADMIN="pasanaku"
./gradlew :servicios:aportes:dependencies --configuration runtimeClasspath
```

(con JDK 21 nativo en el host — ya no hace falta Docker-fuera-de-Docker para
esto, ver `evidencia/H1-entorno-docker.md` §5 para el contexto de por qué antes
sí)
