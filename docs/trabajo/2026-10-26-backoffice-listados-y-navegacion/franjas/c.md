# Franja C — Cumplimiento (`rutas/cumplimiento/**`)

> **AVANCE: 14 / 14 — 100 %.** Peldaño: `VERIFIED` contra dobles (simulado sin estado + `page.route`), con prueba visual mirada. No es `REGRESSION_VERIFIED`: falta la integración H7 (agente principal) y el e2e `caso-de-cumplimiento.e2e.ts` no se corrió.

- Rama: `claude/franja-c` · worktree `/home/user/wt-c` · servidor `127.0.0.1:4303` (bajado al cerrar).
- Hallazgos del descubrimiento:
  - `cumplimiento.yaml` **no tiene ninguna operación PUT/PATCH/DELETE**: en toda la sección no hay Editar ni Eliminar posibles. Las acciones de fila son de dominio (Abrir caso, Ver, Decidir, Aprobar y registrar envío, Responder), siempre ícono + nombre.
  - Las 8 bandejas paginan por cursor en el servidor (`limite` ≤ 100, sin total, **sin parámetro de búsqueda**).
  - Navegación duplicada: `ap-pestanas-de-seccion` (shell) y `ap-navegacion-de-cumplimiento` (marco) con las mismas 9 entradas.
  - Botón muerto: «Confirmar decisión» en `casos/:casoId` solo borraba el borrador local.
  - Bug: `pantalla-de-expedientes` leía `cola.value()` en estado de error → `ResourceValueError` (lo destapó el spec nuevo de error 500).

## Microtareas

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | `crearBandeja` por cursor (reemplaza `observarBandeja`) | filtros y filas por página viajan al servidor; 403 → denegado; error con traza; la última carga gana | `bandeja-por-cursor.spec.ts` 5/5 | HECHO |
| H3.S1.M2 | `ap-listado-de-bandeja` (reemplaza `ap-estado-de-bandeja` + tabla virtualizada) | barra + CSV/JSON + tabla + paginación + 5 estados | specs de bandejas + a11y | HECHO |
| H3.S2.M1 | Alertas de monitoreo | buscador, estado/severidad al servidor, CSV/JSON, selección y acción de fila «Abrir caso» | spec 11 casos + recorrido | HECHO |
| H3.S2.M2 | Casos de investigación | «Ver» → ficha; «Decidir» solo en no cerrado/sin decisión | spec + recorrido (POST decisión) | HECHO |
| H3.S2.M3 | Reportes UIF | «Aprobar y registrar envío» solo en GENERADO; sin Eliminar | spec + recorrido (POST preparar) | HECHO |
| H3.S2.M4 | Requerimientos de autoridad | «Responder» solo si no respondido/archivado | spec + recorrido (POST respuesta) | HECHO |
| H3.S2.M5 | Colas de lectura (continuidad, actas, eventos, incidentes) | kit en las 4; «Registrar una prueba» abre su diálogo | spec + recorrido | HECHO |
| H3.S3.M1 | Verificación de identidad con `crearListado` | buscador nombre/teléfono/correo/documento, filtros estado/documento/fotos, paginación, CSV/JSON tal como se ve | spec 8 casos + recorrido | HECHO |
| H3.S4.M1 | Caso: botón muerto | «Confirmar decisión» abre `decidirCasoLft` con la conclusión prellenada; borrador se borra tras el 2xx | spec + recorrido | HECHO |
| H3.S4.M2 | Íconos y layout en fichas (habilitación, indisponible, caso, expediente) | cada botón con ícono + texto; ficha en una tarjeta a todo el ancho | capturas | HECHO |
| H3.S4.M3 | Marco sin navegación duplicada, a todo el ancho, eyebrow, ícono por vista | una sola navegación; sin scroll horizontal de página | `ancho-de-pagina.txt` | HECHO |
| H3.S5.M1 | Recorrido en navegador (aceptado/límite/error + descargas + requests) | 156 verificaciones PASS | `recorrido-*.txt` | HECHO |
| H3.S5.M2 | Capturas 1280/768/375 claro + 1280 oscuro, miradas | sin desbordes ni defectos | 60 PNG en `evidencia/franja-c/` | HECHO |
| H3.S5.M3 | Comandos de cierre | tsc, lint, verificar_frontend, tests verdes | `cierre-estatico.txt`, `test-*.txt` | HECHO |

## Evidencia (`evidencia/franja-c/`)

```text
$ npx tsc -p tsconfig.app.json --noEmit   → exit 0
$ npx tsc -p tsconfig.spec.json --noEmit  → exit 0
$ npx ng lint                             → All files pass linting.
$ python3 ../../scripts/verificar_frontend.py backoffice → TODO OK
$ npx ng test --include='src/app/rutas/cumplimiento/**/*.spec.ts' --exclude='src/**/*.a11y.spec.ts'
 Test Files  25 passed (25)
      Tests  142 passed (142)
$ npx ng test --include='src/app/rutas/cumplimiento/**/*.a11y.spec.ts'
 Test Files  8 passed (8)
      Tests  14 passed (14)
$ node scripts/recorrido.mjs   → {"pass":131,"fail":0,"errores":20}   (8 bandejas × aceptado/límite/error)
$ node scripts/recorrido-2.mjs → {"pass":25,"fail":0,"errores":2}     (mutaciones, expedientes, caso, habilitación)
$ node scripts/ancho.mjs       → scrollWidth = viewport en 1280/768/375 en las 9 vistas con listado
```

Los «errores» de consola de los recorridos son **todos** los 500/403 simulados a propósito (`Failed to load resource: 500/403`); en el recorrido 1 son 10+10 porque los widgets del tablero piden `alertas?limite=1` y `casos?limite=1` con el mismo patrón mientras el doble respondía error. Ninguno es un error nuevo.

Requests observadas (extracto literal):
```text
GET /cumplimiento/alertas?limite=25
GET /cumplimiento/alertas?limite=25&despuesDeFecha=2026-01-01T00:00:00Z&despuesDeId=cursor-1   (Siguiente)
GET /cumplimiento/alertas?limite=10                                                         (Filas por página)
GET /cumplimiento/alertas?limite=10&estados=ESCALADA                                        (filtro → página 1)
POST /cumplimiento/casos/{id}/decision 200 {"decision":"DESCARTAR","conclusion":"…"}  + Idempotency-Key
POST /cumplimiento/casos 201 {"alertaIds":["0b9b2a61-…-000000000001"],"analistaId":"7c1d9e40-…","resumen":"…"}
POST /uif/reportes 200 {"periodo":"2026-09","catalogoCodigo":"UIF-ROG","formulario":"ROG-01"}
POST /uif/requerimientos/{id}/respuesta 200 {"respuestaUrl":"https://docs.ejemplo.test/respuesta-of-1.pdf"}
POST /identidad/verificaciones/{id}/decision 200 {"decision":"RECHAZAR","motivo":"…"}
GET /identidad/verificaciones?estado=APROBADA
```
Todas las mutaciones fueron contra el simulado sin estado: **verificadas contra el doble, no persistidas**. Datos de todas las corridas: sintéticos.

Capturas (miradas con Read): `<vista>-{1280,768,375}.png` y `<vista>-1280-oscuro.png` para alertas, alertas-vacio, alertas-error, casos, casos-decision-error, caso, reportes-uif, requerimientos, continuidad, actas, eventos, incidentes, verificaciones, habilitacion, indisponible. Correcciones hechas al mirarlas: (1) la tabla con una tipología larga ensanchaba la página (columna implícita `auto`) → `grid-template-columns: minmax(0, 1fr)` en el marco y el listado; (2) el título de error salía «alertas de monitoreo lft» → «No se pudo cargar la bandeja «…».». Dos filas grises en la captura oscura de reportes eran el hover del puntero (re-capturado con el puntero fuera: limpio).

## No cubierto

- `e2e/caso-de-cumplimiento.e2e.ts` (Playwright con su propio servidor) no se corrió; el h1 «Caso de cumplimiento» y el gate de causal se conservaron y los cubren el spec y el recorrido.
- Persistencia real: ninguna mutación se probó contra `cumplimiento`/`identidad` corriendo (no hay backend en esta franja).
- «Ir a la página N» en bandejas por cursor solo ofrece hasta «páginas vistas + 1» (no hay total en el contrato); se recorre con cursores, nunca se inventa uno.
- La descarga de una bandeja por cursor baja **la página cargada** filtrada, no todo el servidor (lo dice el nombre del archivo `…-pagina-N-…` y la ayuda del buscador).
- Lector de pantalla real: no se probó; solo axe + recorrido por roles.
- Pantallas no ruteadas `actas/pantalla-de-acta.ts`, `reclamos/pantalla-de-reclamo.ts`, `alertas/pantalla-de-alertas.ts`: solo se les agregó ícono; sus botones siguen sin operación (no se llega a ellas por ninguna ruta).

## Desvíos y decisiones

- **Filtros de una sola opción por criterio** (el kit usa un `select` por filtro): antes estado/severidad eran chips de selección múltiple. La serialización múltiple sigue probada en el dominio (`paginas.spec.ts`, explode true y false). Si Pablo quiere elegir varios estados a la vez, es un cambio del kit (`packages/ui`).
- Se quitó `ap-navegacion-de-cumplimiento` del marco (duplicaba las pestañas del shell; A1). `navegacion.ts` y `ENTRADAS_DE_CUMPLIMIENTO` quedan (los usa `acceso.spec.ts`) — ver pedido a franja A.
- `compartido/bandeja.ts` y `compartido/estado-de-bandeja.ts` se borraron: solo los consumían bandejas de cumplimiento (grep sin consumidores fuera).
- Specs actualizados para afirmar el mismo requisito con la anatomía nueva (filas en `ap-tabla-de-datos`, reintento con «Volver a intentar», acción de fila en vez de botón en celda). Requisito cambiado: filtro de estado de una opción (arriba). Ningún test borrado ni debilitado; se agregaron 25 casos.
- `casoParaDecidir` en la ficha usa el `casoId` como código (la ficha no tiene GET del caso en el contrato).

## Pedidos a otras franjas

- **Franja A**: las pestañas de sección (`layout/pestanas-de-seccion.ts` + `nucleo/menu.ts`) muestran las 9 pantallas de cumplimiento sin mirar permisos; la navegación interna que filtraba por rol/permiso se quitó por duplicada. Pedido: filtrar las pestañas con los mismos códigos de `ENTRADAS_DE_CUMPLIMIENTO` (o mover esos códigos a `nucleo/menu.ts`).

## Hallazgos fuera de alcance

- **A3 / regla 90.2 — descarga de datos de identidad**: «Verificación de identidad» descarga CSV/JSON con nombre, teléfono, correo y documento **tal como se ven** (la vista no enmascara). Se hizo como pidió la guía; **Pablo decide** si esta vista debe exportar solo por CU-58 (con huella y tope), enmascarar, o no exportar. Lo mismo, en menor grado, para requerimientos de autoridad (no viaja la persona) y alertas/casos (deber de reserva: no viajan persona ni monto).
- `@aportaya/ui` exportar: un teléfono `+591…` sale como `'+591…` en el CSV (defensa anti-fórmula correcta, pero el número queda con apóstrofo visible en la planilla).
- Prism devuelve siempre un cursor: contra el simulado puro toda bandeja dice «hay más» y «Siguiente» nunca se apaga (por eso el recorrido usa `page.route`).
- `eventos`: la categoría sale en código (`FRAUDE_EXTERNO`): no hay catálogo de textos para categorías en el contrato.
- F5 del plan (openapi-generator) sigue igual; no se tocó.
