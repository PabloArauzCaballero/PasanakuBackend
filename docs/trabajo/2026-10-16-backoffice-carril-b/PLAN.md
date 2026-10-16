# Plan — Backoffice financiero · Carril B (experiencia Angular)

- Fecha: 2026-10-16 · Repos afectados: PasanakuBackend (`apps/backoffice`) · Rama: `codex/backoffice-ui-20261015`
  (worktree `.claude/worktrees/backoffice-ui`) · Predecesor: carril A `codex/backoffice-dominio-20261015` @ `6b178fb3`
  (traído por fast-forward: B no tenía commits propios desde `e3e11457`).
- Plan estratégico: `docs/trabajo/2026-10-15-backoffice-financiero/PLAN.md` §6 (B0–B6). Contratos: los OpenAPI de
  `servicios/*/src/main/resources/openapi/` + `docs/trabajo/2026-10-16-backoffice-carril-a/entregables/fichas-de-contrato.md`.
- Resultado observable: un operador con el permiso correspondiente abre cada bandeja que A publicó (retiros, reclamos,
  conciliación, cierres, reversos, desembolsos, alertas, casos, UIF, requerimientos, continuidad, actas, riesgo, incidentes,
  ERP, publicidad, tarifario, mensajería, reportes), pagina por cursor, abre la ficha y ejecuta la acción que el contrato
  publica con `Idempotency-Key`, sin UUID de demostración ni diálogos que simulan enviar.
- Kill-test: `grep -rn "11111111-1111-4111-8111-111111111111\|sinEnviar" apps/backoffice/src` no vacío, o una pantalla que
  llama a una ruta que no está en ningún OpenAPI (`entregables/llamadas_sin_contrato.py`).

## Alcance
- IN: `apps/backoffice/src/**`, `packages/simulado/ejemplos/**` (ejemplos sintéticos de las operaciones nuevas, generados por
  `yarn workspace @aportaya/simulado build`), este directorio de trabajo.
- OUT: `servicios/**`, `sql/**` (carril A), `packages/ui/**` salvo pedido registrado, `apps/movil`, `clientes/**` (generados,
  ignorados por git).
- Ambigüedades registradas:
  - **AMB-B1** Concurrencia de agentes: la regla 70.1.2 fija 1 subagente simultáneo salvo que el `CLAUDE.md` del proyecto
    autorice más; no hay `CLAUDE.md` de proyecto. El dueño del repositorio pidió explícitamente en la sesión «lanzá agentes en
    paralelo». Supuesto: se lanzan 4 agentes con lotes **disjuntos en archivos** (70.4.9), cada uno en su worktree, con
    pruebas dirigidas (no suites completas) y sin navegador; la regresión completa y la prueba visual las corre el agente
    principal en serie. Confirmar con Pablo si se fija en un `CLAUDE.md`.
  - **AMB-B2** Políticas/motor de decisión (B4 parte 2), roles/catálogo de permisos (A1), fondeo, solicitudes escaladas
    (D-15) y registro de accesos: A los dejó `DECISION_REQUIRED` sin contrato. No se inventa endpoint: pantallas fuera de
    este carril hasta que exista ficha (regla 65: lo que falta es la decisión de negocio, no un contrato nombrable).
  - **AMB-B4** Secciones por rol: `AUDITORIA_LEER` lo tienen casi todos los roles; usarlo para abrir Operación/Cumplimiento/
    Configuración les abría secciones que el plan §3 no les da (p. ej. Cumplimiento a Tesorería). Supuesto: las secciones se abren
    por el código de ROL del plan §3; las rutas internas aceptan `AUDITORIA_LEER` como el servidor. Consecuencia: AUDITOR_INTERNO no
    ve Operación por `AUDITORIA_LEER` sino por sus permisos ya sembrados. Confirmar matriz con Seguridad de la información.
  - **AMB-B3** Tablero (B2): no hay endpoint de indicadores/contadores. Supuesto: el tablero muestra «requiere tu acción» a
    partir de las bandejas reales que el permiso habilita (primera página: hay/no hay pendientes, nunca un total inventado) y
    declara «indicador no disponible» donde falta proyección. Confirmar con negocio qué KPI quiere y que A publique proyección.

## Reparto (lotes disjuntos en archivos)

| Lote | Dueño | Archivos que puede tocar |
| --- | --- | --- |
| L0 núcleo e integración | agente principal | `nucleo/**`, `app.routes.ts`, `layout/**`, `nucleo/secciones.ts`, `rutas/ayuda/**`, docs |
| L1 operación y tesorería | subagente 1 | `rutas/operacion/**` |
| L2 cumplimiento | subagente 2 | `rutas/cumplimiento/**` |
| L3 contabilidad y publicidad | subagente 3 | `rutas/contabilidad/**`, `rutas/publicidad/**` |
| L4 tablero y configuración | subagente 4 | `rutas/tablero/**`, `rutas/configuracion/**` (nueva), `rutas/sistemas/**` |

## H1 — Bandejas y acciones del backoffice contra los contratos reales

**CA:** Dado un operador con permiso, cuando abre cualquier bandeja publicada por A, entonces ve la página que devuelve el
servidor (cursor), los estados carga/vacío/error/denegado, y las acciones envían el cuerpo exacto del contrato con
`Idempotency-Key`; la UI no dice «aprobado/enviado» hasta el 2xx.
**DoD:** `yarn workspace @aportaya/backoffice typecheck` 0 · `test:front` verde · `lint` verde · cruce
`llamadas_sin_contrato.py` sin llamadas nuevas sin contrato · capturas de 3 viewports en 2 temas de una bandeja por lote.
**Estado:** HECHO (microtareas) · hito en `TESTED`, falta G2 con servicios reales

### H1.S0 — Núcleo (L0)
**CA:** Toda bandeja pagina por cursor reutilizando `TablaDeDatosVirtualizada` sin cambiar su API.
**DoD:** spec dirigido en verde.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S0.M1 | `nucleo/tabla/cursor.ts`: adaptador `{items, proximo…}` → `CargadorDePagina` | página N reusa el cursor de N-1; filtros nuevos reinician | `ng test --include=src/app/nucleo/tabla/cursor.spec.ts` | HECHO — `evidencia/H1.S0.M1-cursor.txt` (485/485) |
| H1.S0.M2 | Integrar lotes L1–L4 (merge) | sin conflictos fuera de los lotes | `git merge` + typecheck 0 | HECHO — merges de L1–L4 sin conflictos; typecheck 0 (`evidencia/H1.S0.M4-typecheck.txt`) |
| H1.S0.M3 | Secciones/menú: permisos de las rutas nuevas | cada ruta nueva se abre con su permiso y ninguna sin él | `ng test --include=src/app/nucleo/secciones*.spec.ts` | HECHO — `secciones.spec` + `matriz-de-roles.spec` en verde (770/770) |
| H1.S0.M4 | Regresión completa | suite completa verde | `test:front` + `test:a11y` + `lint` + e2e | HECHO — 770/770 · a11y 65/65 · ESLint limpio · e2e 137/137 (`evidencia/H1.S0.M4-*`) |
| H1.S0.M5 | Prueba visual | 3 viewports × 2 temas de una bandeja por lote, inspeccionadas | capturas en `evidencia/` | HECHO — 30 capturas inspeccionadas (`evidencia/visual/`, `H1.S0.M5-visual.txt`); halló el desfase de la tabla en tablet → H1.S0.M7 |
| H1.S0.M6 | Lint: parámetro sin uso preexistente en `pantalla-de-expedientes.ts:151` (bloqueaba el gate) | `ng lint` sin errores | `yarn lint` → «All files pass linting» | HECHO — `evidencia/H1.S0.M4-lint.txt` |
| H1.S0.M7 | Tabla del núcleo: filas desalineadas de la cabecera en tablet (envoltorio CDK a max-content) | envoltorio = ancho del viewport | medición DOM 1059→528 px + visual 30/30 | HECHO — `evidencia/H1.S0.M7-ancho-tabla.txt` |
| H1.S0.M8 | Rutas: el `canMatch` de un listado tapaba su detalle (`casos/:id`, `alertas/:id`, `actas/:id`) | el detalle se abre sin el permiso del listado | `cumplimiento.routes.spec.ts` (rojo primero) | HECHO — `evidencia/H1.S0.M8-rojo-primero.txt` |
| H1.S0.M9 | E2E con dobles fuera de contrato (cobro, pago a proveedor, anunciantes) y F5 por enlace profundo | dobles con la forma del YAML, sin debilitar aserciones | 5 archivos 41/41 | HECHO — `evidencia/H1.S0.M4-e2e-regresion-final.txt` |
### H1.S1 — Operación y tesorería (L1) · CU-11, CU-52, CU-50, CU-51, CU-14, CU-28
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Bandeja de retiros en revisión (reemplaza ingreso de UUID; ficha por id se conserva) | lista por cursor; `puedeDecidir=false` sin acciones | spec dirigido | HECHO (TESTED, contra doble) — `l1-operacion/evidencia` |
| H1.S1.M2 | Reclamos: bandeja paginada + ficha + respuesta real | `POST /reclamos/{id}/respuesta` con cuerpo del contrato; sin `sinEnviar` | spec dirigido | HECHO (TESTED, contra doble) — `l1-operacion/evidencia` |
| H1.S1.M3 | Conciliación de custodia: listado + conciliar | cuerpo del contrato, recibo con id | spec dirigido | HECHO (TESTED, contra doble) — `l1-operacion/evidencia` |
| H1.S1.M4 | Cierres diarios: listado + ejecutar | ídem | spec dirigido | HECHO (TESTED, contra doble) — `l1-operacion/evidencia` |
| H1.S1.M5 | Reversos: listado | lectura paginada | spec dirigido | HECHO (TESTED, contra doble) — `l1-operacion/evidencia` |
| H1.S1.M6 | Desembolsos: bandeja de excepciones | lectura paginada, sin cuenta destino | spec dirigido | HECHO (TESTED, contra doble) — `l1-operacion/evidencia` |
### H1.S2 — Cumplimiento (L2) · CU-44, CU-43, CU-45, CU-54, CU-55, CU-56, CU-94
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Bandeja de alertas real (sin datos de ejemplo en contenedor) | cursor + filtros estado/severidad | spec dirigido | HECHO (TESTED, contra doble) — `l2-cumplimiento/evidencia/10-M1-alertas.txt` |
| H1.S2.M2 | Bandeja de casos + abrir caso + decidir caso | revisor = sesión, sin «avisar al cliente» | spec dirigido | HECHO (TESTED, contra doble) — `l2-cumplimiento/evidencia/11-M2-casos.txt` |
| H1.S2.M3 | UIF: reportes regulatorios (listar/generar/aprobar y enviar) | aprobación ≠ preparación, recibo | spec dirigido | HECHO (TESTED, contra doble) — `l2-cumplimiento/evidencia/12-M3-reportes-uif.txt` |
| H1.S2.M4 | Requerimientos de autoridad (listar/registrar/responder) | cuerpo del contrato | spec dirigido | HECHO (TESTED, contra doble) — `l2-cumplimiento/evidencia/13-M4-requerimientos.txt` |
| H1.S2.M5 | Continuidad, actas de comité, eventos de riesgo, incidentes (colas) | lectura paginada | spec dirigido | HECHO (TESTED, contra doble) — `l2-cumplimiento/evidencia/14-M5-colas.txt` |
### H1.S3 — Contabilidad y publicidad (L3) · CU-100…106, CU-110…112
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | Período vigente + selector de ejercicios/períodos (sin UUID demo) | 422 `AP-CU100-04` se dice, no se elige otro | spec dirigido | HECHO (TESTED, contra doble) — `l3-contabilidad/evidencia` |
| H1.S3.M2 | Listados ERP (partidas, órdenes, facturas, cuentas por cobrar, activos) por cursor | forma `{items, proximo…}` | spec dirigido | HECHO (TESTED, contra doble) — `l3-contabilidad/evidencia` |
| H1.S3.M3 | Publicidad: anunciantes, campañas, piezas con página del contrato | sin `anuncianteNombre` inventado | spec dirigido | HECHO (TESTED, contra doble) — `l3-contabilidad/evidencia` |
### H1.S4 — Tablero y configuración (L4) · CU-34, CU-80, CU-83, CU-58
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S4.M1 | Tablero «requiere tu acción» desde bandejas reales por permiso | sin números inventados; «no disponible» honesto | spec dirigido | HECHO (TESTED, contra doble) — `l4-tablero/evidencia/M1-tablero-specs.txt` |
| H1.S4.M2 | Historial de tarifarios | lectura paginada | spec dirigido | HECHO (TESTED, contra doble) — `l4-tablero/evidencia/M2-tarifario-specs.txt` |
| H1.S4.M3 | Mensajería: plantillas y envíos (auditoría) | sin datos personales | spec dirigido | HECHO (TESTED, contra doble) — `l4-tablero/evidencia/M3-mensajeria-specs.txt` |
| H1.S4.M4 | Reportes: catálogo e historial de ejecuciones | lectura paginada | spec dirigido | HECHO (TESTED, contra doble) — `l4-tablero/evidencia/M4-reportes-y-rutas-specs.txt` |
| H1.S4.M5 | Sistemas: datos de ejemplo solo en desarrollo, indisponibilidad honesta en producción | banner/adaptador verificados | spec dirigido | HECHO (TESTED, contra doble) — `l4-tablero/evidencia/M5-sistemas-specs.txt` |
## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin backend levantado (A quedó en `TESTED`; MinIO 401 en quay.io) | No se llega a `VERIFIED` de punta a punta | Pruebas contra `HttpTestingController` + ejemplos sintéticos del contrato (doble declarado, regla 65); E2E real queda como pendiente G2 |
| Cuatro agentes en paralelo | Contención de CPU, conflictos | Lotes disjuntos, worktrees separados, solo specs dirigidos, sin navegador |
| Permisos no catalogados (33 S-8) | Rutas sin permiso real | No se inventa código de permiso: se usa el que declara el OpenAPI (`x-permiso` / descripción) |
