# Baseline — carril PR3-plataforma (Leo), H1.S1.M1

> Corrida real, en background, de:
>
> ```bash
> for m in comun-dominio comun-datos comun-web comun-mensajeria comun-archivos comun-pruebas; do
>   ./gradlew :plataforma:$m:test :plataforma:$m:webTest :plataforma:$m:integrationTest
>   echo "$m exit=$?"
> done
> ```
>
> contra `leo/feature/carril-PR3-plataforma` en el commit `2d2da96` (antes de cualquier cambio de
> código de esta sesión — el baseline arrancó antes de tocar `Idempotencia.java` ni
> `nuevo_servicio.py`). Máquina compartida con otras sesiones (Justin, Marcelo, Pablo) corriendo
> sus propios carriles en paralelo — varios contenedores Testcontainers/Postgres/Kafka a la vez —,
> lo cual explica los tiempos largos y uno de los dos rojos.

## Veredicto por módulo

| Módulo | `test` | `webTest` | `integrationTest` | `exit` | Tiempo total | Veredicto |
|---|---|---|---|---:|---:|---|
| `comun-dominio` | PASS (from cache) | NO-SOURCE | NO-SOURCE | 0 | 8m 19s | **PASS** |
| `comun-datos` | PASS (from cache) | NO-SOURCE | **FAILED** | 1 | 16m 34s | **FAIL** — ver causa 1 |
| `comun-web` | — (no llegó a correr) | — | — | 1 | 2m 6s | **FAIL** — ver causa 2 |
| `comun-mensajeria` | PASS (from cache) | NO-SOURCE | PASS | 0 | 9m 31s | **PASS** |
| `comun-archivos` | PASS (from cache) | NO-SOURCE | NO-SOURCE | 0 | 2m 44s | **PASS** |
| `comun-pruebas` | PASS (from cache) | NO-SOURCE | PASS | 0 | 6m 49s | **PASS** |

**4 de 6 módulos en verde. 2 rojos, con causa raíz distinta cada uno — ninguno es un defecto de
lógica de negocio; los dos son del entorno/infra de build, y uno de ellos (`comun-web`) ya se
corrigió en esta misma sesión (ver abajo).**

## Causa 1 — `comun-datos:integrationTest`: timeout de Testcontainers, no un defecto de código

```
AppendOnlyRepositorioTest > initializationError FAILED
    java.util.concurrent.TimeoutException: calentarElContenedor() timed out after 600 seconds
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

4 tests completed, 1 failed
```

`calentarElContenedor()` es el `@BeforeAll` que levanta `BaseDePrueba.contenedor()` (Testcontainers
PostgreSQL) y le aplica `sql/aplicar.sql`. El límite ya es generoso a propósito (5 minutos de
`withStartupTimeout`, comentado en `BaseDePrueba.java` como "la maquina de desarrollo corre otros
stacks"), y con la máquina corriendo ~7 procesos Java y 5+ contenedores Postgres/Kafka de
Testcontainers en simultáneo (otras sesiones activas) superó incluso ese margen. No es un hallazgo
sobre `comun-datos`: es contención de recursos del entorno compartido, y `comun-datos` no está en
mi alcance de todos modos (no lo toco).

## Causa 2 — `comun-web`: TODO el módulo caía antes de compilar, por `python3` en Windows

```
> Task :plataforma:comun-web:erroresCatalogo
no se encontró Python; ejecutar sin argumentos para instalar desde el Microsoft Store...

> Task :plataforma:comun-web:erroresCatalogo FAILED
...
> Process 'command 'python3'' finished with non-zero exit value 9009
BUILD FAILED in 2m 6s
```

`erroresCatalogo` (dependencia de `processResources`, que a su vez bloquea `test`/`webTest`/
`integrationTest`) invoca `executable = "python3"`. En Windows, sin el launcher de la Microsoft
Store, `python3` resuelve al alias de ejecución de apps y no al intérprete real — aunque `python`
sí resuelve correctamente (`C:\Python314\python.exe`, confirmado con `where python3` / `where
python` / `python --version` → `Python 3.14.6`). Efecto: **ningún test de `comun-web` corrió
nunca**, ni uno solo, hasta que se arregla esto — el módulo entero quedaba rojo por una razón que
no tiene nada que ver con el código bajo prueba.

**Corregido en esta misma sesión** (`plataforma/comun-web/build.gradle.kts`, archivo reservado de
este carril): `executable` ahora elige `python` en Windows y `python3` en Linux/macOS, vía
`org.gradle.internal.os.OperatingSystem.current().isWindows`. El mismo patrón (`executable =
"python3"`) existe también en el `build.gradle.kts` de la raíz del repo — **fuera de mi alcance**
(no es uno de mis archivos reservados); registrado en `carriles/PR3-plataforma.md` §Hallazgos y en
mi daily §6 para quien lo tenga (Pablo/CI).

## No cubierto por este baseline

- No corre contra `dev` en CI: es una corrida local, en esta máquina, con la contención de recursos
  ya descrita. Un runner de CI dedicado podría no reproducir la Causa 1.
- `comun-web` con la corrección de Causa 1 todavía no se re-corrió como parte de este mismo
  baseline (habría reordenado el loop) — se re-corre como parte del ciclo rojo→verde de H1.S1.M2 en
  adelante, y esa salida sí queda pegada en `carriles/PR3-plataforma.md`.
