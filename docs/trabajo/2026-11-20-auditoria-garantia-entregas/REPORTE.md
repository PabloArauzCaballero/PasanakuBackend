# Reporte — Auditoría de solo lectura del módulo garantia-entregas

> **AVANCE: 8 / 9 — 88,9 %.** (M9, la entrega, se cierra al verificar el push; ver «A medias».)

- Fecha: 2026-11-20 · Plan: [PLAN.md](./PLAN.md) · Rama: `auditoria/garantia-entregas-2026-11-20` (desde `origin/test`)
- Entregable: [AUDITORIA-garantia-entregas.md](./AUDITORIA-garantia-entregas.md)
- Commit auditado: `origin/test` = `07fc69719287b0dadd152d0293a054948df69ead` (sin diferencias con `origin/dev` en el módulo)
- Peldaño de evidencia alcanzado: **`DISCOVERED`** para todo lo auditado (lectura de código). Los verificadores Python del
  repo llegaron a `RUNS`. Las suites Gradle quedaron **`BLOCKED`** por entorno. Ningún hallazgo se ejecutó.

**Resultado en una línea:** 68 hallazgos (13 S1 · 25 S2 · 24 S3 · 6 S4). El respaldo empresarial está vivo de punta a
punta y contradice R-NUEVA-2. La entrega avanza por cinco actos humanos y no por el cobro QR, lo que contradice
R-NUEVA-1. Además hay defectos graves de seguridad y dinero que no dependen del flujo nuevo.

## Completado

| ID | Qué se logró (observable) | Comando de verificación | Resultado |
|---|---|---|---|
| H1.S1.M1 | Rama creada desde `origin/test`; commit, historial completo y diferencia con `dev` anotados | `git rev-parse HEAD origin/test; git rev-parse --is-shallow-repository` | PASS — `evidencia/00-preparacion.txt`, `evidencia/09-dod.txt` |
| H1.S1.M2 | Huella y mapa de conexiones con los saltos de la operación de dinero | `grep -n '^## ' AUDITORIA-…md` (§3 en la línea 83) | PASS — `evidencia/09-dod.txt` |
| H1.S1.M3 | Anexo de cobertura: 275 archivos de la huella (184 completos, 8 parciales, 83 no leídos con motivo) más las conexiones | `grep -c '^| \`' AUDITORIA-…md` → 325 | PASS — `evidencia/09-dod.txt` |
| H1.S1.M4 | Inventarios: 18 aprobaciones, 12 puntos de respaldo, A-1 a A-4 y ramas del faltante | `grep -n '^### 4' AUDITORIA-…md` → 4.a, 4.b, 4.c, 4.d | PASS — `evidencia/09-dod.txt` |
| H1.S1.M5 | Vista de conjunto: DUP-X, REESCRITURA, FRAGIL y PISADA registrados; fila por clase en §8 | lectura de §8 (32 filas, ninguna vacía) | PASS |
| H1.S1.M6 | Matriz de contratos de 41 operaciones y de los eventos; verificadores del repo ejecutados | `ls evidencia/` → `05-verificadores.txt`, `05b-…`, `05c-gradle-…` | PASS. Gradle `BLOCKED` (ver Evidencia) |
| H1.S1.M7 | Cada hallazgo con característica ISO/IEC 25010, estándares y regla | `grep -c 'Estándares y normas' AUDITORIA-…md` → 68 | PASS — `evidencia/09-dod.txt` |
| H1.S1.M8 | Refutación: los 13 S1 y los S2 reabiertos por el agente principal; 4 bajas de severidad y 2 renombres | `evidencia/07-refutacion.md` | PASS |

## A medias

### H1.S1.M9 — Entrega (commit y push)
- **Qué anda:** el informe, el plan, la evidencia y este reporte están escritos. Hay cuatro commits intermedios ya
  pusheados a `origin/auditoria/garantia-entregas-2026-11-20`, uno por tanda de hallazgos.
- **Qué no anda:** al escribir este reporte, el commit final todavía no estaba verificado en el remoto.
- **Qué falta exactamente:** `git push` del commit final y `git log origin/auditoria/garantia-entregas-2026-11-20 -1`
  mostrando ese commit. Después, pasar M9 a `HECHO` en `PLAN.md` y en este reporte.
- **Dónde quedó:** rama `auditoria/garantia-entregas-2026-11-20`. Solo se tocaron archivos en
  `docs/trabajo/2026-11-20-auditoria-garantia-entregas/`.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Ejecución de las suites Gradle de garantía y entregas, `webTest`, `integrationTest` y barridos | BLOQUEADO (entorno: Maven Central respondió HTTP 429) | Una sesión con acceso a las dependencias o con caché de Gradle. No depende de una persona. Es la verificación del módulo, no una tarea de esta auditoría |
| `yarn lint` / `yarn typecheck` / `flutter analyze` | no intentados | Los cubre el lote L6 (supuesto declarado) |
| Decisiones A-1 a A-4 | DECISION_REQUIRED | Pablo, y Cumplimiento para A-1 y para R-SEG-04 |

## Evidencia

```text
$ ./gradlew :servicios:garantia:test --console=plain -q
… Could not GET 'https://repo.maven.apache.org/maven2/org/junit/junit-bom/5.14.3/junit-bom-5.14.3.pom'. Received status code 429 from server: Too Many Requests
BUILD FAILED in 1m 2s
exit=1
```

```text
$ python3 scripts/inventario_endpoints.py --check
endpoints relevados: 350 · hallazgos: 0
$ python3 scripts/matriz_de_pruebas.py --servicio entregas
| entregas | 21 | 16 | 11 | 11 | 5 |
Sin prueba web (5): GET/POST /entregas/subastas, GET /entregas/subastas/{subastaId}, POST …/cancelacion, POST …/pujas
$ python3 scripts/validar_flujos.py
  OK    · aporte-mora-cobertura.yaml: 12 pasos, 6 validos, 7 limite, 7 invalidos   ← valida el flujo viejo (DOC-002)
$ python3 scripts/verificar_seguridad.py
  AVISO · servicios/entregas/…/SubastaRepositorio.java:45 — DSL.field(variable)
  AVISO · servicios/entregas/…/PujaRepositorio.java:34 — DSL.field(variable)
```

Índice de `evidencia/`:
- `00-preparacion.txt`: commits, historial y diferencia entre `test` y `dev`.
- `05-verificadores.txt` y `05b-verificadores-filtrado-modulo.txt`: los 15 verificadores del repo.
- `05c-gradle-garantia-test.txt`: el bloqueo de Gradle.
- `07-refutacion.md`: la Fase 7.
- `09-dod.txt`: los DoD de M1 a M8.

## No cubierto

- Ningún comportamiento ejercitado en runtime. Por eso todos los S1 y S2 están en `DISCOVERED`: «confirmado» quiere
  decir leído y reabierto, no ejecutado.
- Unas 14 000 líneas de pruebas solo se relevaron por `@DisplayName`.
- No se leyeron `docs/entidades/*.puml` (la fuente real del esquema) ni el interior de nucleo-financiero, aportes,
  grupos y cumplimiento más allá de su punto de contacto.
- Modo oscuro, movimiento reducido y accesibilidad con teclado o lector de pantalla no se ejercitaron.
- No se comprobó si el enmascarador de PII de los logs cubre los mensajes de OBSERV-001.
- No se ejecutaron las hipótesis «probable» y «sospecha». Cada una nombra en su ficha la comprobación que le falta:
  RLS con el rol real (DEFECTO-002), doble cobertura por turno (DINERO-006), respuesta 500 de `ejecutarReemplazo`
  (DEFECTO-001) y el trigger de disolución con `svc_garantia` (ACOPLE-001).

## Desvíos del plan

- **Hallazgos por tandas.** Se catalogaron y pushearon por tanda (cuatro commits), por una indicación de Pablo que
  retransmitió la sesión coordinadora. Al final se reordenaron por clase y severidad.
- **Agrupamiento de fichas.** Las fichas S3 y S4 se escribieron primero en forma compacta. Antes de cerrar se
  completaron todos sus campos; el DoD de M7 se volvió a correr después de normalizar la etiqueta «Estándares y normas»
  (primero dio 31, después 68).
- **Números de línea.** Los subagentes reportaron algunas líneas desplazadas. El agente principal reubicó todas las de
  los S1 y S2 confirmados.

## Riesgos residuales

- Los S1 de seguridad (SEG-001 a SEG-007, DINERO-001 y DINERO-003) afectan dinero aunque se mantenga el flujo viejo.
  Esto es relevante para el dossier H15 de habilitación de dinero real.
- Al retirar el respaldo fallarán por diseño alrededor de 15 clases de prueba que lo asumen (PRUEBAS-001). Eso no es
  una regresión: es la prueba de que el respaldo dejó de existir.
- `scripts/validar_flujos.py` seguirá dando OK al flujo con respaldo hasta que se actualice el catálogo.

## Decisiones y ambigüedades

| Ambigüedad | Supuesto tomado | A quién confirmar |
|---|---|---|
| A-1: ¿R-NUEVA-1 alcanza KYC, UIF/ASFI, la doble aprobación de retiros, los cuatro ojos de la entrega (R-SEG-04) y la verificación de cuenta? | No se asume que se eliminan; se dejan como DECISIÓN REQUERIDA | Pablo y Cumplimiento |
| A-2: entrega con faltante | No se inventa la regla. Hoy la entrega queda bloqueada con el pozo completo como principal | Pablo |
| A-3: respaldo, fondo mutual y aval | El fondo mutual y el aval no son «la empresa» salvo que Pablo diga otra cosa | Pablo |
| A-4: mora y recargo sin cobertura | Sin decidir. El catálogo mezcla fondo mutual y reserva de la empresa | Pablo; Cumplimiento para sanciones |
| Severidad de los textos públicos que prometen cobertura | S2 (no mueven dinero) | Pablo |
| Dueño de la pantalla de traspaso por votación | Lote L3 (grupos); acá solo se registra | Coordinador de lotes |

**Conflicto con un candado.** El hook `blocker_gate.py` (regla 65) bloquea el cierre de turno por microtareas
`BLOQUEADO` o `EN CURSO` de seis planes de octubre ajenos a este trabajo. Esta auditoría es de solo lectura y no puede
tocarlos, así que no se tocaron (ver §10 del informe).

**Procesos en background.** Ninguno queda corriendo. Los cuatro subagentes terminaron y el daemon de Gradle se detuvo
con `./gradlew --stop`.
