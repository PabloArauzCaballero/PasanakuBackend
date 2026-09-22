# Evidencia de entorno — inestabilidad de Docker Desktop compartido (H1/H2/H3/H6)

> Declarado por `evidence-and-verification` (regla 30, peldaño de evidencia): un
> bloqueo de infraestructura se documenta con lo que se intentó y lo que lo
> destraba, no se esconde ni se convierte en un PASS.

## 1. Síntoma

`./gradlew` corriendo dentro de un contenedor `eclipse-temurin:21-jdk` con el
repositorio montado como bind mount de Windows (`-v
"C:/.../PasanakuBackend:/work"`) falla de forma reproducible, siempre en el mismo
punto (`:buildSrc:generateExternalPluginSpecBuilders`, escribiendo el lock file de
su propia caché), con:

```text
Caused by: java.io.IOException: Input/output error
	at java.base/java.io.RandomAccessFile.writeBytes0(Native Method)
	...
	at org.gradle.cache.internal.filelock.LockFileAccess.markDirty(LockFileAccess.java:68)
```

Ocurrió 3 veces seguidas, incluso después de:

- Recrear el volumen nombrado `pasanaku-gradle-cache` desde cero (descartando
  cualquier corrupción de caché).
- Confirmar que el volumen en sí acepta escrituras normales (`touch`/`rm` dentro de
  un contenedor de prueba, exitoso).

## 2. Causa real, confirmada

**No es el volumen de caché: es el bind mount de Windows** (`-v
"C:\...:/work"`, gRPC-FUSE/VirtioFS de Docker Desktop) el que no sostiene de forma
confiable el locking POSIX que Gradle usa internamente para su propia caché
(`RandomAccessFile` + bloqueo de archivo) — un problema documentado en general para
builds sobre sistemas de archivos en red/virtualizados, no específico de este repo.

**Segundo factor, encontrado en el camino**: la máquina es COMPARTIDA por varios
carriles corriendo en paralelo (`.claude/worktrees/agent-*` dentro del propio
repositorio confirma al menos otro agente activo), y **algunos archivos de
`buildSrc/.gradle/` estaban con un lock de Windows activo de otro proceso** en el
momento de intentar copiarlos (`docker cp` falló ahí puntualmente con "the process
cannot access the file because another process has locked a portion of the file" —
evidencia de que alguien más, probablemente Leo en su carril de `buildSrc`, tenía
una build corriendo sobre el mismo árbol al mismo tiempo). Eso, sumado a CPU/disco
compartidos con dos procesos ajenos al repo (`mantra-redesa-mongodb-1` al 119% CPU,
`mantra-redesa-opensearch-1` al 69% CPU, de OTRO proyecto en la misma máquina, sin
relación con Pasanaku) explica los tiempos de 25+ minutos observados para una simple
copia de archivos.

## 3. Mitigación aplicada, y su resultado

| Intento | Resultado |
|---|---|
| Bind mount directo del repo completo | `Input/output error` reproducible en `buildSrc` |
| Recrear el volumen de caché de Gradle | Mismo error — descarta corrupción de caché |
| `cp -r` de TODO el repo (incl. `.git`, `apps/`, `packages/`, `landing/`, `node_modules` si los hay, el propio `.claude/worktrees/` de otro agente) a un filesystem local del contenedor | 27+ minutos solo para la copia, sin terminar — descartado por lento, no por incorrecto |
| `cp -r` acotado a los directorios que Gradle necesita (`servicios plataforma buildSrc gradle sql scripts` + archivos raíz) vía bind mount de solo lectura | Igual de lento (varios minutos solo para `servicios/`) — el bind mount de LECTURA también sufre el mismo cuello de botella bajo esta carga |
| **`docker cp` (API de Docker, no bind mount) hacia un volumen nombrado** (`pasanaku-src`), directorio por directorio | **Funciona**: 438 archivos de `plataforma/`, 1399 de `servicios/`, 384 de `sql/`, 32 de `scripts/` — verificados con conteo exacto contra el árbol real. Los tiempos por directorio (ver abajo) confirman que el cuello de botella real es specific a locks/timing, no un límite físico de disco |
| `buildSrc/` completo (incluyendo `.gradle/`, `.kotlin/`, `build/`) | Falló a mitad de camino: **lock de Windows activo de otro proceso** sobre `buildSrc/.gradle/9.7.1/executionHistory/executionHistory.lock` |
| `buildSrc/src` + `buildSrc/build.gradle.kts` únicamente (sin `.gradle/`/`.kotlin/`/`build/`, que son caché local, no fuente) | **Funciona**, sin conflicto de lock: 6 archivos `.kts`/`.kt` fuente, verificados 1:1 contra el árbol real |

Tiempos reales de `docker cp` (segundo intento, ya con el volumen `pasanaku-src`):

```text
== plataforma == real 0m12.6s (con el error de buildSrc en paralelo en otro paso)
== gradle ==     real 0m7.5s
== sql ==        real 0m28.0s
== scripts ==    real 0m7.7s
```

## 4. Receta que queda documentada para el resto del turno y para quien retome

```bash
# Una sola vez (o cuando cambien las fuentes):
docker volume create pasanaku-src
docker run -d --name src-holder -v pasanaku-src:/work alpine:3 sleep 3600
cd PasanakuBackend
for d in servicios plataforma gradle sql scripts; do docker cp "$d" src-holder:/work/; done
docker cp buildSrc/src src-holder:/work/buildSrc/src   # NUNCA buildSrc/.gradle ni .kotlin
docker cp buildSrc/build.gradle.kts src-holder:/work/buildSrc/build.gradle.kts
docker cp gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties src-holder:/work/

# Cada build:
docker run --rm -v pasanaku-src:/work -v pasanaku-gradle-cache:/root/.gradle \
  -w /work eclipse-temurin:21-jdk bash -lc "./gradlew <tareas>"
```

Si se editaron archivos después de la copia, hay que repetir el `docker cp` de la
carpeta tocada antes del siguiente build — el volumen no se sincroniza solo.

## 5. Qué queda con evidencia real pese a esto

Ningún script de Python se vio afectado (corren directo sobre el host, sin Docker):
`verificar_seguridad.py`, `inventario_endpoints.py`, `verificar_contratos_limites.py`
— los tres con salida literal y exit code reales, pegados en sus documentos
correspondientes. Lo que quedó bloqueado por este entorno es específicamente la
ejecución de la suite Java (`webTest`/`integrationTest`/`pitest`) contra
PostgreSQL real vía Testcontainers.
