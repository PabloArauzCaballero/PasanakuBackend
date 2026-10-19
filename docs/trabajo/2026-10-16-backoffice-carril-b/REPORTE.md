# Reporte — Backoffice financiero · Carril B (experiencia Angular)

> **AVANCE: 28 / 28 — 100,0 %.** Todas las microtareas con su DoD en verde (unitarias 770/770, a11y 65/65, e2e 137/137, visual 30/30), pero el **hito H1 queda A MEDIAS**: todo está probado contra dobles, nunca contra los servicios reales, y B4-políticas, auditoría, roles y fondeo siguen `BLOQUEADO` por falta de contrato o decisión. El carril B completo (B0–B6) **no** está terminado.

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md) · Rama: `codex/backoffice-ui-20261015` (worktree
  `.claude/worktrees/backoffice-ui`), sin push. Lotes integrados por merge: `codex/backoffice-ui-l1-operacion`,
  `-l2-cumplimiento`, `-l3-contabilidad`, `-l4-tablero`.
- Peldaño de evidencia alcanzado: **`TESTED`** en todo el frontend (unitarias con `HttpTestingController` contra dobles con la
  forma de los YAML) + **`VERIFIED_FUNCTIONAL_ONLY` visual contra el mock Prism** (ejemplos SINTÉTICOS del contrato).
  **No `VERIFIED`**: ninguna pantalla se ejercitó contra los servicios reales (carril A tampoco los levantó: MinIO 401 en quay.io).
- Alcance cumplido del plan estratégico §6: B1 (bandejas por cursor, acciones con `Idempotency-Key`), B2 (tablero honesto),
  B3 (operación y tesorería), B4 parte 1 (cumplimiento, riesgos, seguridad) y B5 parcial (contabilidad, publicidad,
  configuración). **No** cumplido: B4 parte 2 (políticas/motor/aprobaciones: sin contrato, `DECISION_REQUIRED`), explorador de
  auditoría de B5 (sin contrato de bitácora para la UI), roles/accesos (33 permisos sin catalogar).

## Completado

| ID | Qué se logró (observable) | Comando | Resultado |
|---|---|---|---|
| H1.S0.M1 | `nucleo/tabla/cursor.ts`: toda bandeja `{items, proximo…}` pagina en `TablaDeDatosVirtualizada` sin inventar total | `test:front` cursor.spec | PASS 6/6 · `evidencia/H1.S0.M1-cursor.txt` |
| H1.S0.M2 | 4 lotes integrados sin conflicto | `git merge` + `typecheck` | PASS exit 0 · `evidencia/H1.S0.M4-typecheck.txt` |
| H1.S0.M3 | Menú/secciones: `configuracion` nueva; `ver:operacion` +TESORERIA/CONTABILIDAD; `ver:cumplimiento` +roles oficial/riesgos/seguridad/auditor | `secciones.spec`, `matriz-de-roles.spec` | PASS (770/770) |
| H1.S0.M5 | 5 pantallas × 3 viewports × 2 temas, capturas miradas, consola sin errores | `playwright test e2e/visual-carril-b.e2e.ts` | PASS 30/30 · `evidencia/visual/*.png` |
| H1.S0.M6 | Lint sin errores (parámetro sin uso preexistente) | `yarn workspace @aportaya/backoffice lint` | «All files pass linting» · `evidencia/H1.S0.M4-lint.txt` |
| H1.S0.M7 | Tabla alineada en tablet (envoltorio CDK 1059→528 px) | medición DOM + visual | PASS · `evidencia/H1.S0.M7-ancho-tabla.txt` |
| H1.S0.M8 | Detalles `casos/:id`, `alertas/:id`, `actas/:id` ya no los tapa la guarda del listado | `cumplimiento.routes.spec.ts` | rojo primero 3 fallas → verde · `evidencia/H1.S0.M8-rojo-primero.txt` |
| H1.S0.M9 | Dobles e2e alineados al contrato (cobro, pago a proveedor, anunciantes) | 5 archivos e2e | PASS 41/41 |
| H1.S1.M1–M6 | Retiros (bandeja + `retiros/:ordenId`), reclamos (bandeja + respuesta real, sin diálogo simulado), custodia, cierres diarios, reversos, desembolsos | specs de `rutas/operacion` | PASS · `l1-operacion/evidencia/` |
| H1.S2.M1–M5 | Alertas, casos (abrir/decidir), UIF (preparar / aprobar y registrar envío), requerimientos, continuidad, actas, riesgo, incidentes | specs de `rutas/cumplimiento` | PASS · `l2-cumplimiento/evidencia/10…15` |
| H1.S3.M1–M3 | Período vigente sin UUID demo, ejercicios y períodos, 5 listados ERP por cursor, publicidad con página del contrato | specs de `rutas/contabilidad`, `rutas/publicidad` | PASS · `l3-contabilidad/evidencia/01…06` |
| H1.S4.M1–M5 | Tablero «Requiere tu acción» sin números inventados; tarifario, mensajería, reportes; sistemas honesto en producción | specs de `rutas/tablero`, `configuracion`, `sistemas` | PASS · `l4-tablero/evidencia/M1…M5` |

## A medias

### H1 — bandejas y acciones contra los contratos reales (hito)
- Qué anda: 28/28 microtareas con su DoD; ~25 pantallas nuevas o reescritas consumen los contratos de A con cursor, errores `AP-…`,
  `Idempotency-Key`, estados carga/vacío/error/denegado; gates del frontend en verde (ver Evidencia).
- Qué no anda / no se probó: ningún recorrido contra los servicios levantados (G2); el escenario F5 de aceptación nunca se vio en
  verde (la suite de aceptación falla igual en la base por entorno).
- Qué falta exactamente: (1) destrabar `./gradlew bd:levantar` (MinIO 401) y correr los recorridos lista → ficha → acción → recibo
  contra servicios reales con dos sesiones; (2) arreglar el entorno de la suite de aceptación y ver F5 en verde; (3) las pantallas
  bloqueadas de «Pendiente» cuando A publique contrato.
- Dónde quedó: rama `codex/backoffice-ui-20261015` (worktree `.claude/worktrees/backoffice-ui`), compila, todas las suites en
  verde, sin push.

## Pendiente

| ID / bloque | Estado | Qué lo destraba |
|---|---|---|
| B4 · políticas, simulaciones, aprobaciones con MFA, rollback | `BLOQUEADO` · `DECISION_REQUIRED` | aprobar `carril-a/entregables/ADR-propuesto-motor-de-decisiones.md` (A-2) y que A publique contrato |
| B5 · explorador de auditoría (actor/recurso/intervalo) | `BLOQUEADO` · sin contrato | A publica listado de bitácora para la UI |
| Roles/accesos (A1) | `BLOQUEADO` · `DECISION_REQUIRED` | Seguridad de la información cataloga los 33 permisos S-8 |
| Solicitudes escaladas (D-15), registro de accesos | `BLOQUEADO` · `DECISION_REQUIRED` | ver `carril-a/REPORTE.md` §Pendiente |
| Fondeo | `BLOQUEADO` · sin contrato ni CU | negocio define el flujo |
| Gates G1–G4 (integración real) | `TODO` | levantar el stack (`./gradlew bd:levantar`, hoy roto por MinIO 401) y repetir los recorridos contra servicios reales |

## Evidencia

| Archivo | Qué prueba |
|---|---|
| `evidencia/B-00-linea-base.txt` | línea base antes de tocar: 72 archivos / 479 pruebas en verde |
| `evidencia/H1.S0.M1-cursor.txt` | adaptador de cursor |
| `evidencia/H1.S0.M4-typecheck.txt` · `-test-front.txt` · `-test-a11y.txt` · `-lint.txt` | gates finales del árbol integrado |
| `evidencia/H1.S0.M4-e2e-regresion-1.txt` | primera regresión e2e: 117 PASS / 40 FAIL (clasificadas abajo) |
| `evidencia/H1.S0.M4-e2e-linea-base-6b178fb3.txt` | los mismos archivos sobre el commit previo a B: los 17 de `aceptacion/*` ya fallaban |
| `evidencia/H1.S0.M4-aceptacion-B.txt` | suite de aceptación con su configuración: 12 FAIL / 9 PASS, **idéntico** a la línea base |
| `evidencia/H1.S0.M4-e2e-regresion-final.txt` | regresión e2e final (sin `aceptacion/`) |
| `evidencia/H1.S0.M5-visual.txt` · `evidencia/visual/` | 30 capturas del carril B |
| `evidencia/H1.S0.M7-ancho-tabla.txt` · `H1.S0.M8-rojo-primero.txt` | defectos hallados y su prueba |
| `l1-operacion/` · `l2-cumplimiento/` · `l3-contabilidad/` · `l4-tablero/` `evidencia/` | salidas literales de cada lote |

```text
$ corepack yarn workspace @aportaya/backoffice test:front --watch=false
 Test Files  116 passed (116)
      Tests  770 passed (770)
$ corepack yarn workspace @aportaya/backoffice test:a11y --watch=false
 Test Files  32 passed (32)
      Tests  65 passed (65)
$ corepack yarn workspace @aportaya/backoffice typecheck
typecheck exit=0
$ corepack yarn workspace @aportaya/backoffice lint
All files pass linting.
  FALLA · ningún archivo de más de 200 líneas ['apps/backoffice/src/app/rutas/cumplimiento/verificaciones/pantalla-de-expedientes.ts (209)']
$ playwright test e2e/visual-carril-b.e2e.ts
  30 passed
$ playwright test (todos los e2e del backoffice salvo aceptacion/ y produccion)
  137 passed (1.3m)
```

Clasificación de las 40 fallas de la primera regresión e2e (regla 80.4):

| Archivo | Fallas | Clase | Acción |
|---|---|---|---|
| `aceptacion/*` (f1–f5) | 17 | `ENVIRONMENT` preexistente | fallan igual en `6b178fb3`; con su propia configuración, 12/21 fallan igual en base y en B |
| `caso-de-cumplimiento` | 9 | `PRODUCT_BUG` (B, L2) | guarda del listado tapaba el detalle → H1.S0.M8 |
| `f2-red-y-teclado` · `h17-pago-a-proveedor` | 5 + 1 | `TEST_BUG` | doble con arreglo en vez de la página del contrato → H1.S0.M9 |
| `publicidad-anunciantes` · `visual-tabla-de-datos` | 2 + 6 | `TEST_BUG` | premisa vieja («Prism da 405 porque el contrato no publica el listado»); el error se fuerza ahora con 503 interceptado, sin quitar la aserción de error |

## No cubierto

- **Integración real** con `nucleo-financiero`, `cumplimiento`, `entregas`, `erp`, `publicidad`, `tarifas`, `notificaciones`,
  `auditoria`: todo es contra dobles (`HttpTestingController`, ejemplos sintéticos de Prism). Segregación real con dos sesiones
  (AP-CU11-10, AP-CU43-02, AP-CU44-02) solo verificada como traducción del código.
- **F5 (retiro con doble aprobación)**: reescrito para entrar por `retiros/:ordenId`; la suite de aceptación falla igual en la base
  (entorno), así que el escenario nuevo **no se vio en verde**.
- Celdas virtuales en jsdom: las pruebas unitarias verifican `filas()` y métodos, no el DOM pintado (sí lo cubren las capturas).
- Prueba visual: una pantalla por lote (5), no las ~25 nuevas; estados vacío/error vistos en specs, no capturados.
- Lector de pantalla manual: no se hizo (solo axe en `test:a11y`).

## Desvíos del plan

1. **Cuatro subagentes en paralelo** (AMB-B1): pedido explícito del dueño en la sesión; la regla 70.1.2 fija 1 sin `CLAUDE.md`
   de proyecto. Lotes disjuntos en worktrees propios; la regresión y la prueba visual las corrió el agente principal en serie.
2. Los subagentes **no pudieron escribir sus `REPORTE.md`** (el harness lo bloquea); su contenido está consolidado acá y su
   evidencia está commiteada en cada `lN-*/evidencia/`.
3. `secciones.ts`: no se agregó `AUDITORIA_LEER` a ninguna sección aunque los lotes lo pidieron (AMB-B4).
4. `nucleo/errores.ts`: se agregaron 30 códigos (L1+L3), cada uno verificado contra `servicios/`. Los de L2 siguen en
   `rutas/cumplimiento/compartido/errores-de-cumplimiento.ts` (deuda: unificar).
5. Se corrigieron fuera de los lotes: un error de lint preexistente (M6) y la tabla del núcleo (M7).

## Riesgos residuales y deuda

- **Piezas duplicadas entre lotes** (hecho en paralelo, cada uno sin tocar `nucleo/`): estado de listado/bandeja
  (`operacion/dominio/bandeja-por-cursor.ts`, `cumplimiento/compartido/observarBandeja`, `contabilidad/listado/estado-de-listado.ts`,
  `configuracion/aviso-de-listado.ts`), guardas «alguno de» (`requiereAlguno` en operación, cumplimiento y configuración) y
  `traducirError` en tablero/configuración. Causa raíz: `TablaDeDatosVirtualizada.cargar()` no atrapa el rechazo del cargador ni
  dibuja vacío/error. Conviene subirlo a `nucleo/tabla` en un trabajo propio.
- `casos/:casoId` (`PantallaDeCaso`) sigue mostrando un caso `demo` cuyo botón no envía; la decisión real se hace desde la
  bandeja. Retirarla exige reescribir `e2e/caso-de-cumplimiento.e2e.ts`.
- `pantalla-de-campanas.ts` crea campañas con `cuentaPublicitariaId` cero y `conjuntos: []` (preexistente; el servidor lo rechazará).
- Montos sin moneda en contratos de A (A-7): `montoReclamado`, totales sellados del período, `montoReversado`, `montoTotal`
  UIF, `perdidaCuantificada`: la UI no supone BOB. Hay que corregirlo en los YAML (carril A).
- Campos que se tipean a mano por falta de listado: cuenta de custodia (UUID), `analistaId` al abrir caso, `catalogoCodigo` al
  aprobar reporte UIF.
- `pantalla-de-expedientes.ts` (209 líneas) mantiene en rojo el control de 200 líneas de `verificar_frontend.py` desde antes de B.
- `cierres`: «hoy en La Paz» se calcula con UTC−4 fijo como aviso previo; decide el servidor.

## Decisiones y ambigüedades

| ID | Decisión / supuesto | Confirmar con |
|---|---|---|
| AMB-B1 | 4 subagentes en paralelo por pedido explícito | Pablo: fijarlo en un `CLAUDE.md` si se repite |
| AMB-B2 | Sin pantallas para políticas, roles, fondeo, D-15 (no hay contrato) | negocio / Legal / carril A |
| AMB-B3 | Tablero: «hay pendientes / al día» desde `limite=1`, sin totales; monitoreo «no disponible» | negocio (qué KPI) + carril A (proyección) |
| AMB-B4 | Secciones por rol del plan §3, no por `AUDITORIA_LEER` | Seguridad de la información |
| A-carril | `listarCampanas` exige `PUBLICIDAD_APROBAR_CAMPANA` pero la gestión de campañas usa `PUBLICIDAD_ANUNCIANTES`; `crearCampana` dice exigir el de aprobar | Producto + carril A |
| A-carril | `abrirCasoLft` responde 201 también al reintento (no distingue «ya abierto») | carril A |
