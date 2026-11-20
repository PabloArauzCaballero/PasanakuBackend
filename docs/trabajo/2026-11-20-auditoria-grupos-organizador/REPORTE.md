# Reporte — Auditoría de solo lectura grupos-organizador (L3)

> **AVANCE: 9 / 9 — 100,0 %.**

- Fecha: 2026-11-20 · Plan: [PLAN.md](./PLAN.md) · Entregable: [AUDITORIA-grupos-organizador.md](./AUDITORIA-grupos-organizador.md) · Rama: `auditoria/grupos-organizador-2026-11-20` (desde `origin/test` `07fc6971`)
- Peldaño de evidencia: el **informe** está escrito y subido (entregable de documentación). Las **afirmaciones sobre el código** auditado están en `DISCOVERED` (lectura con ruta y línea); solo los verificadores Python del repo llegaron a `RUNS`. Nada del módulo se ejecutó: Gradle y yarn no corrieron por el entorno.
- Resultado: 55 hallazgos — S1: 2 · S2: 25 · S3: 25 · S4: 3. Aprobaciones inventariadas: 6 (APR-01..06); puntos de respaldo: 6 (RSP-01..06).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Rama desde `origin/test`, historial completo | `git rev-parse HEAD; git rev-parse --is-shallow-repository` | PASS — `07fc6971…`, `false` |
| H1.S1.M2 | Huella y mapa de conexiones | sección 3 del informe | PASS |
| H1.S1.M3 | Anexo de cobertura por archivo | sección 12 (521 filas) | PASS — cobertura parcial declarada |
| H1.S1.M4 | Inventarios de aprobaciones y respaldo, ambigüedades | `grep -n 'APR-0\|RSP-0' AUDITORIA-*.md` | PASS |
| H1.S1.M5 | Vista de conjunto (duplicación, reescritura, fragilidad) | sección 5 y 8 | PASS |
| H1.S1.M6 | Matriz de contratos y verificadores | `ls evidencia/` (16 salidas) | PASS con límites de entorno declarados |
| H1.S1.M7 | Estándares por hallazgo | `grep -c 'Estándares y normas'` = 55 | PASS |
| H1.S1.M8 | Refutación de S1/S2 | tabla en sección 11 | PASS — 1 baja de S1 a S2, 1 baja de confianza |
| H1.S1.M9 | Commit y push solo de esta carpeta | `git push`; `git diff --name-only origin/test..HEAD` fuera de la carpeta → 0 | PASS |

## A medias
ninguna.

## Pendiente
ninguna dentro del alcance. Decisiones de negocio abiertas (DECISION_REQUIRED, para Pablo; Cumplimiento en lo regulatorio): A-1 (qué aprobaciones sobreviven: KYC, habilitación profesional, R-ORG-06, votaciones), A-2 (entrega con faltante), A-3 (destino de lo construido sobre respaldo), A-4 (mora y recargo sin cobertura).

## Evidencia
Salidas literales en `evidencia/`:
```text
$ python3 scripts/inventario_endpoints.py --check
endpoints relevados: 350 · hallazgos: 0
$ python3 scripts/verificar_rutas_frontend.py
FALLA · la lista de conocidas se pudrió: … GET /grupos/solicitudes-escaladas
[exit 1]
$ python3 scripts/matriz_de_pruebas.py --servicio grupos
| grupos | 44 | 31 | 18 | 14 | 0 |   (13 operaciones sin prueba web)
$ python3 scripts/matriz_de_pruebas.py --servicio organizador
| organizador | 25 | 18 | 19 | 18 | 0 |  (7 sin prueba web)
$ ./gradlew :servicios:grupos:test --console=plain
… Received status code 429 from server: Too Many Requests … BUILD FAILED
$ yarn typecheck
error This project's package.json defines "packageManager": "yarn@4.18.0". However the current global version of Yarn is 1.22.22.
```
Además: `validar_flujos`, `verificar_criterios`, `verificar_seguridad`, `verificar_contratos_limites`, `idempotencia`, `cobertura`, `listar_topicos`, `verificar_frontend`, `verificar_pruebas_web`, `verificar_boveda`, `verificar_carriles`, `auditar_backend`, barrido manual de tamaño.

## No cubierto
- Ninguna prueba Java, Angular ni Flutter se ejecutó; ninguna base de datos se levantó. Toda la sección 7 del informe (evidencia de dinero y microservicios) queda NO COMPROBADO o FAIL por ausencia.
- 84 clases de prueba de `grupos` (14 419 líneas) no se leyeron línea a línea; `grupos.yaml` al ~65 %; `restricciones.sql` solo en rangos del módulo.
- Hallazgos S3/S4 con ubicaciones aportadas por subagentes y verificadas por muestra (rotulados en cada ficha).
- Valor real de `ADMISION_HUMANA_OBLIGATORIA` en producción; enmascarador de PII de logs.

## Desvíos del plan
- Se agregó una indicación del dueño (retransmitida por la sesión que creó esta): catalogar cada hallazgo al identificarlo y subir por tandas. Hubo 7 commits a la rama de entrega en lugar de uno solo; todos con el mismo prefijo de mensaje y solo esta carpeta.
- El candado `blocker_gate.py` (regla 65) bloqueó el cierre de turno por microtareas `BLOQUEADO`/`EN CURSO` de **otros** trabajos (`docs/trabajo/2026-10-03-…`, `2026-10-14-…`, `2026-10-16-…`, `2026-10-27-…`, `2026-10-28-…`, `2026-10-30-…`). No se tocaron: están fuera del alcance de solo lectura (regla 00 §3). Conviene que su dueño los cierre o declare la simulación.

## Riesgos residuales
- Los dos S1 (DINERO-002 alias, SEG-001 retiro) están en «probable»: falta ejercitarlos contra una base con RLS y ver la confirmación del destinatario en CU-12 (L1).
- Las conclusiones sobre interruptores dependen de los valores por omisión del repo; el entorno real puede diferir.

## Decisiones y ambigüedades
- A-1 a A-4 registradas en la sección 4c del informe, sin resolver; a confirmar con Pablo (y Cumplimiento para KYC/UIF).
- Supuesto: retiro, acuerdos, permutas y traspasos se auditaron del lado de `servicios/grupos`; sus pantallas de app quedan para el lote de esos flujos.
- Supuesto: la admisión humana y el inicio/sorteo manual se clasificaron ELIMINABLE porque el propio repo los declara decisión pendiente (`application.yml:74`) o ya resolvió automatizarlos (ADR-054 D3). La habilitación profesional y R-ORG-06 quedaron DECISIÓN REQUERIDA.
- Procesos en segundo plano: ninguno. Los 4 subagentes terminaron; el demonio de Gradle se detuvo.
