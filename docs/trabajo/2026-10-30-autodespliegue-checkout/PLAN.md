# Plan — El autodespliegue de TEST no puede decir «listo» si no cambió de commit

- Fecha: 2026-10-30 · Repos: PasanakuBackend (`despliegue/host/reconstruir-si-cambio.sh`) · Predecesor: ninguno
- Resultado observable: si el host no puede pasar al commit nuevo de `test`, el log dice `FALLA` y no se despliega; si puede, el árbol queda exactamente en ese commit aunque la corrida anterior haya dejado clientes generados modificados.
- Kill-test: un clon con un archivo versionado modificado que el commit nuevo también cambia → el script viejo escribe «listo» y queda en el commit anterior.

## Hallazgo (2026-10-30, TEST, solo lectura)
- `/opt/aportaya/repo` quedó en `9f642eb` con 4 archivos de `clientes/dart/*/lib/*.dart` modificados por la generación de clientes. `git checkout -q "$REMOTO"` (línea 59) falla porque el commit nuevo también los cambia; el script corre con `set -uo pipefail` (sin `-e`), sigue, encola en Coolify el `HEAD` viejo (`disparar-despliegue.sh` usa `git rev-parse HEAD`) y escribe «listo en c3fd625». TEST no desplegó nada nuevo desde `9f642eb`.
- Destrabado a mano por Pablo: `git checkout -- clientes/dart` + `ultimo-construido = 9f642eb`.

## Alcance
- IN: el paso de checkout de `despliegue/host/reconstruir-si-cambio.sh`.
- OUT: la generación de clientes, qué parte de `clientes/dart` se versiona, instalar el guion en el host (es manual, ver `despliegue/host/README.md`).

## H1 — Checkout que no miente
**CA:** Dado un clon con clientes generados modificados, cuando `test` avanza, entonces el árbol queda en el commit nuevo; y si el checkout igual falla, el log dice `FALLA` y no hay «listo».
**DoD:** prueba en un clon descartable (abajo) · `bash -n` · `shellcheck` si está.
**Estado:** A MEDIAS (falta el PR y la instalación manual en el host)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | `git checkout -q -f "$REMOTO"` y verificar `HEAD = REMOTO`; si no, `marca "FALLA …"` + `exit 1` | el clon sucio termina en el commit nuevo; con checkout imposible sale 1 con FALLA | `evidencia/prueba-checkout.txt` | HECHO |
| H1.M2 | PR a `test` y gemelo a `dev` | dos PRs | `gh pr view` | TODO |

## Decisiones
- `-f` descarta cambios locales en archivos VERSIONADOS del clon de despliegue. Ese clon no es un lugar de trabajo: lo único que escribe ahí es la generación de clientes, que se rehace en cada corrida.
