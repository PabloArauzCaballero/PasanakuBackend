# Reporte — Auditoría frontends-compartidos (L6)

> **AVANCE: 8 / 9 — 88,9 %.** H1.S1.M2 (cobertura por archivo) queda A MEDIAS: se leyó completo ≈54 % de la huella, y lo demás se declaró como escaneado o muestreado.

- Fecha: 2026-11-20 · Plan: [PLAN.md](./PLAN.md) · Informe: [AUDITORIA-frontends-compartidos.md](./AUDITORIA-frontends-compartidos.md)
- Rama: `auditoria/frontends-compartidos-2026-11-20` (desde `origin/test` @ `07fc69719287b0dadd152d0293a054948df69ead`; `origin/dev` sin diferencias en el alcance)
- Peldaño de evidencia: `DISCOVERED` (lectura) en todas las áreas. Hay `RUNS` en los verificadores del repo y dos ejecuciones puntuales (port Python de `CampoMonto.aCadenaDelContrato`; `new URL` en node).
- Resultado: 42 hallazgos verificados (**1 S1, 14 S2, 24 S3, 3 S4**) + 40 candidatos de lectores como *sospecha*. Inventario: 8 aprobaciones (3 DECISIÓN REQUERIDA, A-1) y 7 textos de respaldo en el alcance, más mock, clientes y tour (6 contradicen R-NUEVA-2; 1 DECISIÓN REQUERIDA, A-3).

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M0 | Rama y commit auditado fijados, historial completo | `git rev-parse HEAD` · `git rev-parse --is-shallow-repository` | `07fc6971…` · `false` |
| H1.S1.M1 | Huella (1 391 archivos, ~98 900 líneas) y mapa de conexiones | `git ls-files … \| wc -l` | §3 del informe |
| H1.S1.M3 | Inventarios de aprobación y respaldo | `git grep -n -i -E "aprob\|…"` y `"respaldo\|cobertura\|cubr\|faltante\|…"` | §4a, §4b, §4c |
| H1.S1.M4 | Vista de conjunto (duplicación, reescrituras, fragilidad) | `diff`, `git grep -l` de referencias | DUP-X-001/002, REESCRITURA-001/002, FRAGIL-001 |
| H1.S1.M5 | Matriz de contratos y verificadores | 13 scripts de `scripts/` | 12 exit 0, `verificar_rutas_frontend.py` exit 1; salidas en `evidencia/verificadores/` |
| H1.S1.M6 | Estándares por hallazgo | inspección | cada ficha con ISO/IEC 25010 y regla de la casa |
| H1.S1.M7 | Refutación de S1/S2 | re-lectura de ubicaciones | S1 resiste; 4 severidades ajustadas (§11) |
| H1.S1.M8 | Entrega | `git push -u origin auditoria/frontends-compartidos-2026-11-20` | exit 0 (commits por tanda) |

## A medias

### H1.S1.M2 — Lectura con registro de cobertura
- Qué anda: el anexo (§12) tiene la tabla por archivo de los cuatro lotes (≈1 230 filas). Se leyeron completos unos 750 archivos, incluido todo el código de `diseno_flutter`, `tokens`, `dominio-cliente` y el shell móvil.
- Qué no anda: el CA pide una fila por archivo de la huella y faltan filas individuales para 76 pruebas de `apps/movil/test/{unidad,widget}`, que están listadas en conjunto. Unos 640 archivos (specs de Angular, ejemplos JSON del mock, componentes de `packages/ui` que no tocan dinero) se escanearon o muestrearon, sin lectura completa.
- Qué falta exactamente: leer y agregar fila por fila las 76 pruebas de la app; leer completos los 187 archivos marcados como escaneados en el lote C y los 265 ejemplos muestreados del lote B; re-verificar uno por uno los 40 candidatos del §5-bis.
- Dónde quedó: `AUDITORIA-frontends-compartidos.md` §12 y `evidencia/lotes/`; rama `auditoria/frontends-compartidos-2026-11-20`. Solo documentación, sin código tocado.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Lint, typecheck y pruebas de front (`yarn lint`, `yarn typecheck`, `test:front`, `test:a11y`) | BLOQUEADO (entorno) | `corepack yarn` → `ENOTFOUND repo.yarnpkg.com`; correr en una máquina con yarn 4.18 |
| Análisis, pruebas y goldens Flutter (`apps/movil`, `packages/diseno_flutter`) | BLOQUEADO (entorno) | No hay SDK Flutter/Dart en la nube; correr en el runner canónico |
| Decisiones A-1 a A-4 | DECISION_REQUIRED | Pablo (A-1 también Cumplimiento) |

## Evidencia
- `evidencia/verificadores/*.txt`: salida literal de los 13 verificadores.
- `evidencia/lotes/{A,B,C,D}-*.md`: informes de los lectores (cobertura por archivo y candidatos).
- Ejecuciones puntuales pegadas en las fichas DINERO-002 y SEG-001 del informe.

```text
$ python3 scripts/verificar_rutas_frontend.py      → exit 1
rutas distintas que usa el frontend: 104 · con contrato: 98 · sin contrato: 6
FALLA · la lista de conocidas se pudrió: … GET /grupos/solicitudes-escaladas
$ python3 scripts/verificar_frontend.py todo        → exit 0 · TODO OK
$ corepack yarn --version                          → ENOTFOUND repo.yarnpkg.com
```

## No cubierto
- Ningún comportamiento se ejercitó en ejecución (ni app, ni backoffice, ni web). Todos los hallazgos están en `DISCOVERED`.
- No se comprobó el efecto real de DINERO-002 en un dispositivo, ni la carrera de DEFECTO-003, ni las cabeceras en el borde de Coolify (SEG-002).
- Los 40 candidatos del §5-bis no fueron re-verificados por el auditor.

## Desvíos del plan
- La sesión coordinadora retransmitió una indicación de Pablo: escribir cada hallazgo al identificarlo y hacer commit y push por tanda. Se hizo en 7 commits en lugar de uno solo al final. El mensaje de commit de las tandas no es literalmente el del metaprompt, pero mantiene su prefijo `docs: auditoria de…`.
- Las salidas de los verificadores y los informes de los lectores se versionaron en `evidencia/`, dentro de la carpeta permitida.

## Riesgos residuales
- DINERO-002 (S1) sigue en el código: cualquier importe escrito con punto decimal en la app se multiplica por 100.
- Los textos públicos siguen prometiendo cobertura del faltante y pago en efectivo mientras no se tome A-2.

## Decisiones y ambigüedades
- A-1 a A-4: registradas en §4c del informe, sin resolver. A quién: Pablo; Cumplimiento para A-1.
- Supuesto de frontera del «shell y compartido» (§11): `dominio/cu10..cu15_*.dart` de la app se auditaron acá aunque son de billetera (L1).
- RESPALDO-001/003 se clasifican «CONTRADICE R-NUEVA-2» suponiendo que el fondo vivo es la reserva empresarial (ADR-051 D-15). Si se conserva un fondo mutual de participantes, pasan a DECISIÓN REQUERIDA.
- El candado `blocker_gate.py` señala microtareas abiertas de 6 planes ajenos. Una auditoría de solo lectura no los modifica; quedan en «Observado fuera de alcance».
- Procesos en background: ninguno. Los 4 lectores terminaron y no se levantaron servidores.
