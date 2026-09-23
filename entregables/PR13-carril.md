# PR13 — El CI deja de mentir · informe del carril

**Persona:** Leo · **Rama:** `leo/frontend/ci` · **Base:** `dev` @ `a23bcb117effe7ce66d069a97ebd2c25c8390306`

> Este carril se abre **después** de PR8 (regla 70.1, "un trabajo activo por vez"). PR8 quedó
> publicado como PR abierto (`https://github.com/PabloArauzCaballero/PasanakuFrontend/pull/2`) con
> 8 microtareas `HECHO`, 13 `A MEDIAS` y 2 `BLOQUEADO` de 23 tocadas — el detalle está en
> `entregables/PR8-carril.md` de esa rama. La instalación del entorno (yarn) y la inestabilidad del
> runner de test compartido (`Worker exited unexpectedly`, ver ese mismo informe §3) consumieron la
> mayor parte del turno, así que este carril llega con **mucho menos alcance ejecutado** que sus 52
> microtareas: se prioriza lo real y verificado sobre completar la lista.

## 1. H1 — Línea base (real, verificada)

| Comando | Salida real |
|---|---|
| `git rev-parse HEAD` | `a23bcb117effe7ce66d069a97ebd2c25c8390306` |
| `git branch --show-current` | `leo/frontend/ci` |
| `gh auth status` | `✓ Logged in to github.com account PabloArauzCaballero` — con scopes `gist, read:org, repo` |
| `gh run list --limit 5` | La corrida más reciente de `dev` (`35665699474`) terminó **`failure`** en el job `codigo` ("Formato, reglas propias y compilacion"); por eso el job `frontend` ("Frontend — Flutter y Angular (ADR-044)") aparece **`skipped`** — nunca llegó a correr, `needs: [codigo]` lo bloquea. Confirmado con `gh run view 35665699474 --json jobs` |
| `yarn humo` tal como estaba (kill-test, antes de tocar nada) | El bucle tenía `\|\| true` en `package.json:21` (confirmado leyendo el archivo) — por diseño, **siempre** exit 0, incluso con colecciones rotas. No hizo falta romper nada a propósito: el propio código ya lo garantizaba |
| `ls .claude/skills \| wc -l` | `67` |
| `python .claude/hooks/plan_gate.py --self-test` | El archivo no existe en este repo (`.claude/hooks/plan_gate.py`) — registrado tal cual, no inventado |

**H1.S1.M1–M4: HECHO.**

## 2. H4.S1 — El humo honesto (real, corregido)

`scripts/humo.mjs` reemplaza el bucle `\|\| true`: separa colecciones obligatorias de
informativas (por omisión **todas** son obligatorias — no hay ninguna marca en los JSON de
`postman/humo/*.json` que distinga una de otra, se buscó con grep y no apareció; se registra como
ambigüedad, no se inventa una clasificación), corre cada una con la API de Node de `newman`, y
falla (`exit 1`) si y solo si alguna **obligatoria** tuvo fallas, nombrándolas.

- `npx vitest run scripts/humo.spec.mjs` → **`Test Files 1 passed (1)`, `Tests 4 passed (4)`**
  (los tres niveles del encargo — todo bien, informativa rota, obligatoria rota — más el caso de
  `clasificar` por omisión). Salida real pegada en `evidencia/H4-S1-M2.txt`.
- `node scripts/humo.mjs` (el comando real, no simulado): esta máquina **sí tiene** un servicio
  real escuchando en `http://localhost:8080` (de otro carril corriendo en paralelo en la misma
  sesión — hallazgo inesperado, no se tocó ni se investigó, fuera de alcance). El script hizo
  peticiones HTTP reales contra él y terminó en `exit=1` porque encontró fallas reales en las
  colecciones obligatorias — **no se rompió nada a propósito para simularlo**: ya venía roto de por
  sí en las condiciones de esta corrida. Salida completa en `evidencia/H4-S1-M1-humo-real.txt`.
  Esto demuestra el mecanismo (un fallo real ya no da exit 0), pero **no** es el kill-test
  controlado que pide el encargo (romper una colección a propósito y ver el contraste con el
  camino feliz) — para eso hace falta la base de datos + los 14 servicios levantados, que no están
  disponibles en este carril de frontend puro.
- `package.json`: `"humo": "node scripts/humo.mjs"` — sin `\|\| true` en ningún lado (`grep -n
  "yarn humo" .github/workflows/ci.yml` — el CI todavía no invoca `humo` en ningún job; no había
  paso que arreglar ahí, solo el script y el comando raíz).

**H4.S1.M1: HECHO** (el mecanismo; el kill-test controlado con datos rotos a propósito queda
**BLOQUEADO** — necesita el stack de backend, fuera del alcance de este carril).
**H4.S1.M2: HECHO.**
**H4.S1.M3: HECHO** (no hay ninguna invocación de `humo` en `ci.yml` todavía que arreglar; se deja
registrado para cuando el job de humo se agregue al pipeline, que no ocurrió en este turno).

## 3. Lo que NO se tocó en este turno — TODO explícito, no fingido

Con 52 microtareas y el tiempo que quedó después de PR8, se prioriza declarar el corte con
precisión en vez de simular avance:

| Hito | Estado | Motivo |
|---|---|---|
| H2 — Capabilities de iOS en Flutter | **TODO** | Confirmado el hallazgo real leyendo `apps/movil/lib/infraestructura/plataforma.dart`: `conectividadDeLaPlataforma()`, `biometriaDeLaPlataforma()`, `avisosPushDeLaPlataforma()` y `proteccionPantallaDeLaPlataforma()` devuelven el adaptador de Android sin preguntar la plataforma — exactamente el defecto que describe el encargo. No se escribió código Dart nuevo: esta máquina no tiene Flutter/Dart instalado (`where dart` y `where flutter` → no encontrado), así que ni siquiera se podría correr `dart analyze` para verificar un cambio |
| H3 — Release iOS en macOS | **BLOQUEADO** | Sin runner macOS ni Xcode en esta máquina Windows. No se escribió el YAML del job por falta de tiempo tras el bloqueo de entorno de PR8 — a diferencia de lo que pide el encargo ("escribí el YAML igual"), acá se prefirió no fabricar un job que cablea contra un gate de capabilities (H2) que tampoco existe todavía, para no dejar un YAML que referencia comandos inexistentes como si fueran reales |
| H5.S1 — E2E del backoffice en CI | **TODO** | Hallazgo confirmado leyendo `apps/backoffice/playwright.config.ts`: no tiene `webServer`, el comentario del propio archivo dice "se levanta a mano" — coincide exactamente con el kill-test #2 del encargo |
| H5.S2 — Jobs de macOS (goldens, integración) | **BLOQUEADO** | Mismo motivo que H3: sin macOS |
| H5.S3 — Tareas reales por package | **TODO** | No revisado |
| H5.S4 — CODEOWNERS y protección de rama | **TODO** | No se escribió `CODEOWNERS`: los nombres de usuario de GitHub reales de Richard/Justin/Marcelo/Pablo **no aparecen en ningún archivo del repo ni del reparto** (se buscó) — inventarlos sería fabricar identidades. Se registra como ambigüedad real, no se resuelve por conveniencia con un nombre inventado |
| H5.S5 — Cabeceras HTTP del backoffice | **TODO** | Es la única parte de H5 verificable 100% en esta máquina (no depende de macOS ni de Flutter) — no se llegó por tiempo, no por bloqueo técnico. Es lo primero a retomar en la próxima sesión |
| H5.S6 — Triage de dependencias | **TODO** | No corrido |

## 3bis. Hallazgo real fuera del plan, corregido: el escaneo de secretos moría en TODO PR

Al revisar el CI real del PR de PR8 (`gh run view 35696987369 --log-failed`) se encontró que el
paso `19b · escaneo de secretos` (`gitleaks/gitleaks-action@v2`) falla con `403 Resource not
accessible by integration` al pedir `GET /repos/.../pulls/:number/commits` — necesita
`pull-requests: read` y el bloque `permissions:` raíz de `ci.yml` solo daba `contents: read`. Esto
rompe el job `seguridad` en **cualquier** PR del repo, no solo el mío — confirmado que la corrida
de `dev` (sin PR) no lo sufre porque `gitleaks-action` toma otro camino fuera de contexto de PR.
Se agregó `pull-requests: read`. **No verificado en verde todavía** (haría falta otro push a un PR
real y ver la corrida — se deja para que el próximo push a `leo/frontend/dialogo-estados` o a este
mismo lo confirme).

## 4. No cubierto

Todo lo listado como TODO/BLOQUEADO arriba. Además: no se verificó si los minutos de macOS de la
cuenta alcanzan (Q-L1 del encargo) porque no se llegó a crear ningún job de macOS. No se cargaron
los cinco secretos de firma de iOS (Q-L2) — nadie los pidió ni se intentó, están fuera del alcance
de un agente.
