# Reporte — Auditoría de solo lectura: identidad-cumplimiento (L4)

> **AVANCE: 9 / 9 — 100,0 %.** (auditoría de solo lectura: los hallazgos están en peldaño DISCOVERED; ninguna prueba del módulo corrió)

- Fecha: 2026-11-20 · Plan: [PLAN.md](./PLAN.md) · Rama: `auditoria/identidad-cumplimiento-2026-11-20` (creada desde `origin/test`)
- Entregable: [AUDITORIA-identidad-cumplimiento.md](./AUDITORIA-identidad-cumplimiento.md)
- Commit auditado: `origin/test` `07fc69719287b0dadd152d0293a054948df69ead` (`origin/dev` `850e0f51` tiene el mismo árbol)
- Peldaño de evidencia alcanzado: `DISCOVERED` en todas las áreas del módulo. Solo se ejecutaron los verificadores estáticos de `scripts/`. Ninguna prueba del módulo corrió (Gradle, yarn y Flutter no disponibles en la nube; ver «No cubierto»).

## Completado

| ID | Qué se logró (observable) | Comando de verificación | Resultado |
|---|---|---|---|
| H1.M0 | Rama creada desde `origin/test`, commit anotado, historial completo | `git rev-parse origin/test` | PASS → `07fc6971…` (`evidencia/commit-auditado.txt`) |
| H1.M1 | Huella y mapa de conexiones (§3) | `grep -c '^## 3' AUDITORIA-*.md` | PASS → 1 (`evidencia/dod-plan.txt`) |
| H1.M2 | Anexo de cobertura de archivos (§12) | conteo de filas del anexo | PASS → 566 filas |
| H1.M3 | Inventarios de aprobaciones (17 filas) y respaldo (3) y tabla A-1 a A-4 (§4) | `grep -c '^| APR-'` / `'^| RSP-'` | PASS → 17 / 3 |
| H1.M4 | Vista de conjunto y cobertura por clase (§8, 32 clases, ninguna vacía) | conteo de filas de §8 | PASS |
| H1.M5 | Matriz de contratos (§6) y verificadores ejecutados | `ls evidencia/` | PASS → 23 archivos; Gradle/yarn como BLOCKED de entorno |
| H1.M6 | Estándares en cada ficha | `grep -c '^- Estándares y normas:'` | PASS → 176 de 176 |
| H1.M8 | Entrega: commit y push a la rama de entrega, sin PR ni merge | `git push` | PASS → `b7218c97..59987f2b  auditoria/identidad-cumplimiento-2026-11-20 -> auditoria/identidad-cumplimiento-2026-11-20` |
| H1.M7 | Refutación de S1 y S2 | ver «Evidencia» | PASS → 0 hallazgos caídos; 14 corregidos; 1 bajado a S3; 39 líneas relocalizadas |

## A medias

ninguna

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| — | ninguna | — |

## Evidencia

Índice de `evidencia/`:
- `commit-auditado.txt`: `origin/test` y `origin/dev`.
- Verificadores (`python3 scripts/<x>.py`, salida literal): `verificar_seguridad.txt` (exit 0), `verificar_boveda.txt` (exit 0), `verificar_carriles.txt` (exit 0), `verificar_frontend.txt` (exit 0), `verificar_rutas_frontend.txt` (**exit 1**, falla ajena de grupos), `verificar_pruebas_web.txt`, `verificar_contratos_limites.txt`, `validar_flujos.txt`, `verificar_criterios.txt`, `cobertura.txt` («Sin informes»), `idempotencia.txt` (sin salida), `auditar_backend.txt`, `listar_topicos.txt`, `inventario_endpoints.txt` (`--check`, exit 0).
- `gradle-identidad-testBarrido.txt`: `BUILD FAILED` por 429 de Maven Central.
- `yarn-backoffice-typecheck.txt`: Corepack no habilitado.
- `fase7-verificador-A.md`, `fase7-verificador-B.md`: refutación independiente de 33 S2.
- `fase7-relocalizacion.md`, `fase7-chequeo-de-lineas.txt`, `chequear_lineas.py`: chequeo mecánico de las 677 referencias `ruta:línea` (resultado final: 0 fuera de rango; 7 nombres ambiguos revisados a mano).
- `dod-plan.txt`: salidas de los DoD del plan.

```text
$ python3 scripts/validar_flujos.py
  AVISO · recarga-y-retiro.yaml: paso `instruir-pago` SIN CABLEAR — H8-01 · Ningún trabajo, endpoint ni consumidor llama a RetirosConProveedor.despachar ni a CU11RetirarSaldo.instruirPago en producción; …
$ timeout 600 ./gradlew :servicios:identidad:testBarrido --console=plain
  > Could not GET 'https://repo.maven.apache.org/maven2/…'. Received status code 429 from server: Too Many Requests
  BUILD FAILED in 1m 1s
$ python3 chequear_lineas.py AUDITORIA-identidad-cumplimiento.md   (después de la relocalización)
  referencias revisadas: 677 · fuera de rango o sin resolver: 7   (los 7 son nombres de archivo ambiguos, ninguno fuera de rango)
```

Resultado de la auditoría: **176 hallazgos: S1 3 · S2 61 · S3 81 · S4 31.** Detalle y fichas en el entregable.

## No cubierto

- Ninguna prueba Java, Dart ni TypeScript del módulo corrió (Gradle 429; jOOQ necesita PostgreSQL con el esquema aplicado y el metaprompt prohíbe migraciones; yarn sin Corepack; Flutter no instalado). Las afirmaciones del informe son de lectura (`DISCOVERED`).
- Ninguna evidencia de runtime de las reglas 91.6 / 98.8 / 90.6: asientos, idempotencia ejercitada, caída del dependido y traza con `correlationId` quedan NO COMPROBADO (§7 del informe).
- Pruebas leídas solo en parte: las de autorización, step-up, UIF y PEP completas; el resto inventariado por nombre.
- El historial está completo pero no se recorrió commit por commit buscando pisadas.
- Clientes generados (`clientes/dart/*/lib/src`, `clientes/angular/*`) no versionados: el contraste se hizo contra los OpenAPI.
- Interceptores y timeouts del backoffice (`apps/backoffice/src/app/nucleo/`) no leídos.

## Desvíos del plan

- Lectura repartida en 5 subagentes de solo lectura con lotes disjuntos, en dos olas: 4 simultáneos y después el quinto, dentro del máximo de 4 que autoriza `CLAUDE.md`. La refutación la hicieron 2 verificadores distintos de quienes encontraron los hallazgos (regla 70.4.8), más un relocalizador de líneas.
- Por indicación retransmitida de Pablo, el informe se escribió y subió por tandas (8 commits) en vez de un único commit final. El metaprompt pedía un commit; el cambio no afecta qué se escribió ni dónde.
- El candado `blocker_gate.py` (regla 65) bloqueó el cierre de turno por 9 microtareas BLOQUEADO/EN CURSO de **otros** trabajos (`docs/trabajo/2026-10-03-…`, `2026-10-14-…`, `2026-10-16-…`, `2026-10-27-…`, `2026-10-28-…`, `2026-10-30-…`). Esta auditoría solo puede escribir en su carpeta (metaprompt §3), así que no se tocaron esos planes. Queda registrado para que sus dueños los cierren.
- Un heredoc de shell sin comillas intentó interpretar fragmentos entre comillas invertidas al escribir la §6. No se ejecutó nada: los scripts no tienen permiso de ejecución y el resto no eran comandos. `git status` limpio fuera de esta carpeta. La sección se reescribió desde un archivo.

## Riesgos residuales

- Los subagentes de las tandas 3 y 5 citaron números de línea inexistentes en 39 referencias. Se relocalizaron todas y el chequeo mecánico no deja ninguna fuera de rango. Igual, una referencia dentro de rango puede apuntar a una línea vecina: las fichas S1 y S2 se reabrieron y las S3/S4 se muestrearon.
- La severidad de los hallazgos regulatorios (reportes UIF en cero, oficios sin bloqueo) queda en S2 porque el criterio S1 del metaprompt no cubre lo regulatorio. Su peso real lo determina Cumplimiento.

## Decisiones y ambigüedades

- A-1 (¿«nada de aprobación» incluye KYC, UIF/ASFI y la doble aprobación de retiros?): **no resuelta**. Inventario §4.a: 6 filas DECISIÓN REQUERIDA (KYC manual, organizador, retiros, compuerta de la app, permisos de aprobación de otros módulos, habilitación de organizador). La vía del sospechoso UIF y la diligencia reforzada figuran como CONSERVAR POR NORMA, con fuente `docs/Cumplimiento.md:183-185` y §2.3. Confirmar con Pablo y Cumplimiento.
- A-2, A-3, A-4: el módulo no tiene ramas de faltante, mora ni respaldo propias. Sí hay una promesa de cobertura en la app (RSP-01) y el permiso `FONDO_ADMINISTRAR` sembrado (RSP-02). Confirmar con Pablo.
- Supuesto de frontera: el resto de `nucleo-financiero` es del lote L1, las pantallas de grupos/admisión del L3 y `plataforma/*` del L5. La recarga acreditada a mano por TESORERÍA se dejó en «Observado fuera de alcance».
- Severidad: el orquestador bajó 7 S1 propuestos a S2 y movió uno a §10; cada caso tiene su «Nota del orquestador».
