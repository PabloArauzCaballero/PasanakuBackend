# Higiene de dependencias

> H6.S2.M4 del carril PR4-seguridad (Marcelo). Sin upgrades majors — regla del
> carril. Este documento es el inventario y las decisiones; los parches/minors con
> CVE aplicables en `aportes` se hacen uno por commit (H6.S2.M5).

## Estado de esta corrida

**`./gradlew :aportes:dependencies` no se ejecutó todavía en esta sesión**: el
entorno Docker Desktop compartido con los demás carriles estuvo severamente
inestable durante buena parte del turno (ver
`docs/auditoria-produccion/evidencia/H1-entorno-docker.md` — errores de E/S
reproducibles del propio Gradle al escribir su caché, y una copia de ~200 MB del
repositorio a través del bind mount de Windows tardando más de 25 minutos por
contención de disco con otros procesos del mismo host). Se priorizó dejar H1
(idempotencia + webhook) y H2 (inventario + contratos) con evidencia real de
ejecución antes de abrir un análisis de dependencias que necesita el mismo build
completo.

Lo que SÍ se relevó sin necesitar el build (lectura directa del catálogo):

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
| `shedlock` | Locks distribuidos para tareas programadas | `CU21CobrarAporte.generarRecargos` es el candidato natural — no confirmado si ya tiene `@SchedulerLock` (fuera del foco de este carril) |
| `resilience4j` | Circuit breaker / retry | `aportes` no tiene clientes HTTP salientes (ver `security-matrix.md` §API7): dependencia declarada, uso real no confirmado — **candidato a hallazgo de "declarada pero sin uso"**, pendiente de `./gradlew :aportes:dependencies` para confirmarlo con el árbol real |
| `micrometer` | Métricas | — |
| `bundles.pruebas` (test) | JUnit 5, AssertJ, Testcontainers, ArchUnit | — |

**Nueva dependencia de este carril**: `com.fasterxml.jackson.databind` (ya transitiva
de `spring-boot-starter-web`, usada explícitamente en
`CU100RecibirWebhookPasarela` para parsear el payload del webhook DESPUÉS de
verificar la firma). No es una dependencia nueva en el catálogo — no se tocó
`libs.versions.toml` para esto.

## Sin uso / obsoletas / duplicadas / transitivas innecesarias

**TODO** — exige `./gradlew :servicios:aportes:dependencies --configuration
runtimeClasspath` real, no inferencia. Candidato principal a investigar primero:
`resilience4j` en `aportes` (ver tabla de arriba).

## CVE con parche/minor disponible

**TODO** — exige acceso a OSV/una base de vulnerabilidades real contra las
versiones fijadas en `gradle/libs.versions.toml`. El script real de esto es
responsabilidad de Pablo (`ci(security): add OSV scanner`, H6.S2 del plan madre,
fuera de este carril salvo aplicar el parche puntual una vez que exista el reporte).

## Siguiente paso concreto para quien retome

```bash
MSYS_NO_PATHCONV=1 docker run --rm \
  -v pasanaku-src:/work -v pasanaku-gradle-cache:/root/.gradle \
  -w /work eclipse-temurin:21-jdk \
  bash -lc "./gradlew :servicios:aportes:dependencies --configuration runtimeClasspath > /work/deps-aportes.txt; cat /work/deps-aportes.txt"
```

(el volumen `pasanaku-src` ya tiene una copia de las fuentes hecha con `docker cp`
en vez de bind mount — mucho más estable en este host bajo carga; ver
`evidencia/H1-entorno-docker.md` §3 para por qué)
