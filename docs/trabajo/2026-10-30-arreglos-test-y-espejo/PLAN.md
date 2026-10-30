# Plan — Clave expuesta en el log de TEST, construcción de grupos y espejo desviado

- Fecha: 2026-10-30 · Repos: Pasanaku (`origin` y `frontend-mirror`) · Rama: `claude/arreglos-test-20261030` sobre `origin/test` (`58f3aa15`)
- Predecesor: [`2026-10-29-sistemas-pendientes`](../2026-10-29-sistemas-pendientes/REPORTE.md) (ahí se detectaron los tres problemas)
- Resultado observable: en TEST, el log de despliegue y el historial de la imagen ya no muestran la clave de la base, y la clave expuesta ya no sirve; `test` y `dev` del espejo quedan con el mismo árbol que en `origin`.
- Kill-test: `grep -c <clave vieja> /opt/aportaya/logs/autodespliegue.log` > 0, o una conexión con la clave vieja que funciona, o `git diff origin/test frontend-mirror/test` no vacío.

## Hallazgos
- `despliegue/Dockerfile:60-66`: la clave llega como `ARG BD_CLAVE_ADMIN` y se usa en `RUN`. BuildKit imprime el `RUN` con el valor ya expandido, y así quedó en el log.
- `/opt/aportaya/bin/construir.sh` (en el host, no versionado) lee `/opt/aportaya/postgres/.env` y la pasa con `--build-arg`.
- Otros que llaman al Dockerfile: CI (`ci.yml:850`, con la clave de desarrollo `pasanaku`, que no es secreta) y el compose local (`generar_compose.py:307`, usa el valor por omisión). Los dos tienen que seguir andando.
- La falla de `grupos` de las 01:33 (`AcuerdoRepositorio.java`: `cannot find symbol`) ya no se repite: las tandas de las 02:05 y las 10:34 terminaron en `listo`, y la columna `grupos.acuerdo.entrante_participante_id` existe en la base de TEST.
- Espejo: el `test` tiene 34 commits propios y el `dev` 6. Las diferencias de contenido son versiones viejas de archivos que `origin` ya reescribió (menú, cumplimiento) y un salto de dependabot a Spring Boot 4.1 en `gradle/libs.versions.toml`.

## Alcance
- IN: `despliegue/Dockerfile`; `construir.sh` en el host; tachar la clave en el log del host; cambiar la clave del rol `pasanaku` en TEST y en cada lugar del host que la guarda; sincronizar el `test` y el `dev` del espejo.
- OUT: el salto a Spring Boot 4 (actualización mayor no pedida: queda en el historial del espejo, no en el árbol); las claves de los roles de cada servicio (`ROLES_CLAVE`), que no se filtraron.
- Ambigüedades:
  1. Si el espejo tiene trabajo que `origin` no tiene. Supuesto: es material viejo, porque los commits del espejo son merges y ports de `origin` y su contenido distinto es una versión anterior. Lo resuelvo como en 2026-10-15 (memoria `pasanaku-dos-remotos`): un merge con el árbol de `origin` y los dos padres, sin force. Confirmar con Pablo si quiere el salto a Spring Boot 4.

## H1 — La clave deja de filtrarse
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | El Dockerfile lee la clave de un secreto de BuildKit (`--mount=type=secret`) y, si no hay secreto, usa el ARG | con el secreto, la salida del build no tiene la clave; sin el secreto (CI y compose), construye igual | build local de `identidad` en las dos variantes, con `grep` de la clave en la salida | HECHO (evidencia/H1-dockerfile-secreto.txt) |
| H1.S1.M2 | `construir.sh` del host pasa `--secret id=bd_clave_admin,env=BD_CLAVE_ADMIN` y deja de pasar el `--build-arg` | la próxima construcción en TEST no imprime la clave | `grep -c` de la clave en el log nuevo = 0 | HECHO (evidencia/H1-construir-con-secreto-en-test.txt) |
| H1.S1.M3 | Tachar la clave en el log existente (y en sus rotaciones) | `grep -c` = 0 | comando pegado | HECHO (evidencia/H1-construir-con-secreto-en-test.txt) |
| H1.S2.M1 | Cambiar la clave del rol `pasanaku` en TEST y actualizar cada lugar del host que la guarda | con la clave vieja la conexión falla; con la nueva funciona; los servicios siguen sanos | `psql` con las dos claves + healthchecks | BLOQUEADO: el clasificador de permisos negó escribir en los almacenes de secretos (.env del host y Coolify). Script listo, no ejecutado; lo destraba Pablo (permiso o correrlo él). Acción sobre algo compartido, no se simula (regla 65 §1) |

## H2 — El espejo queda igual que origin
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | `test` del espejo: merge con el árbol de `origin/test` y los dos padres, push normal | `git diff origin/test frontend-mirror/test` vacío | comando pegado | HECHO (evidencia/H2-espejo.txt) |
| H2.S1.M2 | `dev` del espejo: ídem con `origin/dev` | `git diff origin/dev frontend-mirror/dev` vacío | comando pegado | HECHO (evidencia/H2-espejo.txt) |

## H3 — Cierre
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | PR del Dockerfile a `test` y a `dev`, mergeados; TEST despliega en `listo` | `listo en <commit>` sin `FALLA` | líneas del log | HECHO: #331 (test) y #332 (dev) mergeados; «listo en f4fa141» 14:01:09 (evidencia/H1-construir-con-secreto-en-test.txt) |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Cambiar la clave rompe algo que la guarda y no encontré | TEST cae | Buscar el valor en todo `/opt/aportaya` y en Coolify antes de cambiarla; verificar la salud de los servicios después; la clave vieja queda en un archivo 600 del host hasta verificar |
| `--mount=type=secret,env=` no está soportado por el frontend del Dockerfile | El build falla | Montarlo como archivo (`/run/secrets/…`), que existe desde dockerfile 1.2; el Dockerfile declara 1.7 |
