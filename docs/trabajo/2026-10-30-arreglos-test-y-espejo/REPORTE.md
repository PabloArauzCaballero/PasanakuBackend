# Reporte — Clave expuesta en el log de TEST, construcción de grupos y espejo desviado

> **AVANCE: 6 / 7 — 85,7 %.** Falta 1 `BLOQUEADO`: cambiar la clave expuesta. El clasificador de permisos negó escribir en los almacenes de secretos, y la clave vieja **sigue siendo válida** hasta que se cambie.

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/arreglos-test-20261030` · PRs #331 (test) y #332 (dev)
- Peldaño alcanzado: `VERIFIED` para que la clave deje de filtrarse (se observó en TEST: una tanda real construyó con el secreto y no la imprimió). Para la rotación, `WRITTEN` (el script existe y no se ejecutó).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `despliegue/Dockerfile` lee la clave de un secreto de BuildKit y, si no hay secreto, del ARG | tres builds de `identidad` contra una base descartable | PASS: con secreto construye y la clave no aparece en la salida ni en `docker history`; por ARG (camino de CI) construye; sin ninguna de las dos falla la autenticación (control) · `evidencia/H1-dockerfile-secreto.txt` |
| H1.S1.M2 | `/opt/aportaya/bin/construir.sh` pasa `--secret id=bd_clave_admin,env=POSTGRES_PASSWORD` (respaldo en `construir.sh.bak-20261030`) | tanda real de TEST desde las 13:57 | PASS: 9 servicios con `BUILD SUCCESSFUL`, `listo en f4fa141`, clave en el log nuevo: 0 · `evidencia/H1-construir-con-secreto-en-test.txt` |
| H1.S1.M3 | La clave quedó tachada en los 6 logs de `/opt/aportaya/logs` | `grep -rlF <clave> /opt/aportaya/logs \| wc -l` | 0 |
| H2.S1.M1 | El `test` del espejo tiene el árbol de `origin/test` (merge `bf7bfbb3` con los dos padres, sin force) | `git diff --name-only origin/test frontend-mirror/test` | 0 archivos · `evidencia/H2-espejo.txt` |
| H2.S1.M2 | Ídem para `dev` (merge `3c948ae8`) | ídem | 0 archivos |
| H3.S1.M1 | Dockerfile mergeado en `test` (#331) y `dev` (#332); TEST desplegó | log del host | `listo en f4fa141` |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S2.M1 — cambiar la clave del rol `pasanaku` | `BLOQUEADO` | Pablo: dar permiso o correrlo él. Es una acción sobre algo compartido, así que no se simula (regla 65 §1). |

### Qué hay que hacer para cambiar la clave (lo hace el script de la sesión, que no se dejó en el host)
1. Respaldar `/opt/aportaya/postgres/.env`, `/opt/aportaya/infra/.env` y `/data/coolify/applications/n3wymuuo076w9prwo5i293bx/.env`.
2. Generar la clave nueva: `openssl rand -hex 24`.
3. Cambiarla en el rol: `ALTER ROLE pasanaku PASSWORD …`, por stdin, dentro de una transacción con `SET LOCAL log_statement='none'`.
4. Reemplazar el valor en los tres `.env`: en `postgres` es `POSTGRES_PASSWORD`, en `infra` es `PG_PASSWORD` y en el de Coolify es `BD_CLAVE_ADMIN`.
5. Recrear pgbouncer con `cd /opt/aportaya/infra && docker compose up -d pgbouncer`. Es `auth_user=pasanaku` y arma su `userlist.txt` a partir del entorno: hasta recrearlo, las conexiones nuevas de los servicios fallan.
6. En Coolify, actualizar `EnvironmentVariable` 1139 y 1140 (`BD_CLAVE_ADMIN`, aplicación 13). De ahí sale el entorno del job de esquema que reusa `reconstruir-si-cambio.sh`.
7. Desplegar la aplicación una vez, para que el contenedor `esquema-…` se cree con la clave nueva.
8. Verificar: la clave vieja da `password authentication failed`, la nueva conecta, y los servicios y el gateway están sanos.

## Evidencia
```text
H1.S1.M1 (base descartable con clave SINTÉTICA claveprueba-fix)
  con --secret          → generateJooq · BUILD SUCCESSFUL · apariciones de la clave: 0 · docker history: 0
  con --build-arg (CI)  → generateJooq · BUILD SUCCESSFUL
  sin nada (control)    → FATAL: password authentication failed for user "pasanaku" · BUILD FAILED
H1.S1.M2 (TEST)         → 9 × BUILD SUCCESSFUL · [2026-10-30T14:01:09] listo en f4fa141 · clave en el log nuevo: 0
H1.S1.M3                → archivos de log que aún la contienen: 0
H2                      → test: 0 archivos de diferencia · dev: 0 archivos de diferencia
```
Índice: `evidencia/H1-dockerfile-secreto.txt`, `evidencia/H1-construir-con-secreto-en-test.txt`, `evidencia/H2-espejo.txt`.

## No cubierto
- La clave sigue en el entorno de los contenedores en ejecución (`docker inspect` de los 16 servicios, pgbouncer y postgres), en los `.env` del host y en Coolify. Ahí tiene que estar; deja de importar cuando se cambie.
- La clave también se vio completa en la salida de esta sesión de Claude Code (un `tail` del log, antes de saber que estaba ahí). Es una razón más para cambiarla.
- El historial de las imágenes de servicio construidas antes del cambio: la etapa `construccion` no se publica, pero puede seguir en la caché de BuildKit del host.

## Desvíos del plan
- **Falla de `grupos`:** no hizo falta tocar nada. La tanda de las 01:33 compiló sin el esquema al día; la columna `grupos.acuerdo.entrante_participante_id` ya existe y las tandas de las 02:05, 10:34 y 14:01 terminaron en `listo`. La causa más probable es que esa tanda compiló antes de aplicar el esquema; no la reproduje.
- La primera búsqueda en Coolify leyó la clave vacía (el archivo no era legible para el usuario de PHP) y devolvió coincidencias falsas. Se repitió con un control de longitud. No se cambió nada en esa corrida.

## Riesgos residuales
- 🔴 **Hasta que se cambie, la clave expuesta sigue siendo válida** sobre el rol `pasanaku`, que es superusuario de la base de TEST.
- El espejo vuelve a desviarse si se mergean PRs directamente en PasanakuFrontend. El salto de dependabot a Spring Boot 4.1 (PR #48 del espejo) quedó en el historial, pero no en el árbol.

## Decisiones y ambigüedades (confirmar con Pablo)
1. El contenido propio del espejo se trató como viejo (son versiones anteriores de archivos que `origin` reescribió). Si querés Spring Boot 4.1, es un trabajo aparte con su regresión.
2. La clave nueva tendría la misma forma que la vieja (48 caracteres hexadecimales).
