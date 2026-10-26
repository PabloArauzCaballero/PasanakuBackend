# Franja B — Operación (`rutas/operacion/**`)

> **AVANCE: 14 / 14 — 100 %** de las microtareas de la franja. Peldaño: **VERIFIED contra el doble** (simulado OpenAPI sin estado + `page.route`), no `REGRESSION_VERIFIED`: la regresión del backoffice entero y los e2e del repo son de H7.

- Fecha: 2026-10-26 · Rama: `claude/franja-b` (worktree `/home/user/wt-b`, base `c9ac3966`) · Plan raíz: [../PLAN.md](../PLAN.md)
- Resultado observable: cada listado de Operación tiene buscador multicampo, filtros, CSV/JSON de lo filtrado, acciones de fila con ícono + nombre y paginación (filas por página, Ir a N, Anterior/Siguiente), y resuelve cargando / vacío / vacío por filtro / error con reintento / sin permiso; ningún botón queda muerto.
- Kill-test: en `/operacion/reversos`, elegir «Rechazado» pide `GET /contabilidad/reversos?estados=RECHAZADO`; «Descargar CSV» con una búsqueda sin coincidencias baja solo el encabezado; `/operacion/solicitudes-escaladas` no pide ningún endpoint. **Los tres pasan** (`evidencia/franja-b/recorrido-reversos.txt`, `recorrido-otras.txt`).

## Decisiones

- **D-B1 · Listados por cursor.** Los seis listados de Operación paginan por cursor en el servidor y su contrato no acepta búsqueda. Se compuso la anatomía del kit a mano (`comun/listado-por-cursor.ts` + `comun/listado-por-cursor-vista.ts`, con `ap-barra-de-filtros`, `ap-exportar-listado`, `ap-tabla-de-datos`, `ap-paginacion` y los estados del kit). Al servidor van: filas por página (`limite`), cursor (Anterior/Siguiente/Ir a, reutilizando `nucleo/tabla/cursor.ts`: nunca se calcula un cursor) y el **estado** donde el contrato lo acepta (reclamos, reversos, desembolsos). En el navegador, sobre **la página cargada** (y así lo dice la ayuda del buscador): búsqueda multicampo, filtros secundarios y CSV/JSON. El contrato no da total: la paginación conoce las páginas vistas + 1 si hay próxima; el resumen dice «N registros en la página P».
- **D-B2 · Solicitudes escaladas sin contrato.** `servicios/grupos/.../grupos.yaml` y `packages/simulado/generado/prism/todos.yaml` **no** publican una cola de escaladas (solo `listarSolicitudesDeIngreso` por grupo, `proponerAdmision`, `resolverAdmision`, `historialAdmision`). La vista deja de pedir `GET /grupos/solicitudes-escaladas` (404), quita Aceptar/Rechazar (no enviaban nada), lo dice, y ofrece abrir la solicitud en Admisiones por su identificador (`/admisiones/:solicitudId`, que sí resuelve con `resolverAdmision`). Se borraron `dominio/d15-solicitudes-escaladas.ts` y su spec: modelaban un endpoint inventado; el requisito vigente lo prueba el spec nuevo de la cola.
- **D-B3 · Sin Editar ni Eliminar** en retiros, reversos, desembolsos, cierres y conciliaciones (registros financieros, regla 91.1.3), y ningún contrato de esas colecciones tiene `PUT/PATCH/DELETE`. Acciones de fila de dominio con ícono + nombre: «Revisar y decidir» (retiros, `aprobarRetiro`) y «Responder» (reclamos, `responderReclamo`). Reversos, desembolsos, cierres y conciliaciones no tienen acción de fila (sin endpoint detrás).
- **D-B4 · Importes.** Siempre `ap-monto` con su moneda. Donde el contrato no trae moneda (A-7: reclamos, cierres, reversos) se mantiene `ap-importe-sin-moneda` («4,44 · moneda no registrada»), sin suponer BOB. En CSV/JSON el importe sale como se ve: «Bs 4,44» o «4,44 (moneda no registrada)» (`comun/como-se-ve.ts`); nunca `toFixed` ni número.
- **D-B5 · Formularios** (conciliación, cierre) en una tarjeta a lo ancho que se abre con un botón de la cabecera («Registrar conciliación», «Cerrar un día»), en dos columnas desde 48 rem; antes estaban siempre abiertos encima del historial con un ancho de 24–32 rem.

## Microtareas

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Estado compartido `crearListadoPorCursor` | primera página `limite=10` sin cursor; Siguiente con el cursor del servidor; Ir a 3 recorre; filtro de servidor vuelve a la 1; respuesta vieja descartada; 403 ⇒ denegado | `ng test comun/listado-por-cursor.spec.ts` (8 casos) | HECHO |
| H2.S1.M2 | Vista `ap-listado-por-cursor` | barra + resumen + descarga + tabla + paginación + estados; sin desborde (columna `minmax(0,1fr)`) | specs de las 6 bandejas + recorrido (scrollWidth ≤ ancho) | HECHO |
| H2.S2.M1 | Retiros en revisión | ver tabla de vistas | `bandeja-de-retiros.spec.ts` + `recorrido-retiros.txt` (25 PASS) | HECHO |
| H2.S2.M2 | Aprobación de retiro (tarjetas, volver en cabecera, íconos) | 500 conserva motivo y deja reintentar; 2xx muestra el resultado; Volver navega | `pantalla-de-aprobacion-de-retiro*.spec.ts` + `recorrido-formularios.txt` | HECHO |
| H2.S2.M3 | Reclamos (+ respuesta) | estado al servidor; Responder pide la ficha; 500 al responder conserva el texto | `bandeja-de-reclamos.spec.ts` + `recorrido-reclamos.txt` (27) + formularios | HECHO |
| H2.S2.M4 | Solicitudes escaladas sin endpoint inventado | 0 peticiones; abre `/admisiones/:id`; id inválido marcado | `cola-de-solicitudes-escaladas*.spec.ts` + `recorrido-otras.txt` | HECHO |
| H2.S2.M5 | Conciliación de custodia (listado + formulario + recibo) | columna de cuenta; formulario en tarjeta; POST con el contrato e Idempotency-Key; 500 conserva lo escrito | specs custodia + `recorrido-custodia.txt` (24) + formularios | HECHO |
| H2.S2.M6 | Errores de campo que se limpian al corregir | corregir la cuenta saca solo su error | `formulario-de-conciliacion.spec.ts` (caso nuevo) | HECHO |
| H2.S2.M7 | Cierres diarios (listado + formulario + comprobante) | POST solo `{fecha}`; 500 conserva el día; comprobante tras 2xx | specs cierres + `recorrido-cierres.txt` (24) + formularios | HECHO |
| H2.S2.M8 | Reversos | estado al servidor en una clave; tipo local; texto largo a 375 | `listado-de-reversos.spec.ts` + `recorrido-reversos.txt` (26) | HECHO |
| H2.S2.M9 | Desembolsos | estado al servidor (`explode: true`); sin estado ⇒ excepciones | `bandeja-de-desembolsos.spec.ts` + `recorrido-desembolsos.txt` (26) | HECHO |
| H2.S2.M10 | Billetera: Actualizar saldo y Consultar otra cuenta | Actualizar re-pide; otra cuenta navega y pide; id inválido marcado | `pantalla-de-billetera.spec.ts` (3 casos nuevos) + `recorrido-otras.txt` | HECHO |
| H2.S2.M11 | Arquitectura y estado con `ap-listado`, KPI y `?filtro=` | nivel en la URL; filtros nivel/salud; CSV/JSON; 500/403 explicados | `pantalla-de-estado.spec.ts` (2 casos nuevos) + `recorrido-otras.txt` | HECHO |
| H2.S3.M1 | Quitar `bandejaPorCursor` y `EstadoDeBandejaVista` | sin consumidores (grep: cumplimiento usa su propia copia en `cumplimiento/compartido/`) | `tsc` app + spec en 0 | HECHO |

### Vista → qué cambió → niveles

| Vista | Qué cambió | Aceptado | Límite | Error |
|---|---|---|---|---|
| Retiros en revisión | Listado por cursor; filtros Segundo factor / Quién decide; acción «Revisar y decidir» con ícono; CSV con «Bs 4,44» | PASS | PASS (25 filas: 10 / Ir a 3 ⇒ 5 y Siguiente deshabilitado; 1 fila; vacío) | PASS (500 con traza y reintento; 403) |
| Aprobación de retiro | Tarjetas resumen \| decisión; «Volver a la bandeja» en la cabecera; íconos | PASS (2xx contra el doble) | — | PASS (500 conserva motivo, botón usable) |
| Reclamos | Estado al servidor; Categoría/Canal/Plazo locales; «Responder» con ícono; plazo como fecha + chip; nota de la norma | PASS | PASS (+ texto largo a 375) | PASS |
| Respuesta a reclamo | Sin cambios de layout (diálogo del kit) | PASS (POST con el cuerpo del contrato) | — | PASS (500 conserva la respuesta) |
| Solicitudes escaladas | Sin endpoint inventado; abrir en Admisiones | PASS (0 peticiones; navega) | PASS (sin acceso a Admisiones) | PASS (id inválido) |
| Conciliación de custodia | Listado por cursor + columna de cuenta; formulario en tarjeta a dos columnas; recibo con ícono | PASS (201 contra el doble ⇒ recibo y recarga) | PASS | PASS (500 conserva lo escrito) |
| Cierres diarios | Listado por cursor; formulario en tarjeta (requisitos \| día); comprobante con ícono | PASS (2xx ⇒ comprobante) | PASS | PASS (500 conserva el día) |
| Reversos | Listado por cursor; estado al servidor, tipo local | PASS | PASS (+ texto largo a 375) | PASS |
| Desembolsos | Listado por cursor; estado al servidor, resultado local | PASS | PASS (+ texto largo a 375) | PASS |
| Billetera (CU-13) | Tarjetas saldo \| otra cuenta; Actualizar saldo | PASS | PASS (saldo en cero) | PASS (500; 403) |
| Arquitectura y estado | `ap-listado` del kit; KPI de salud; `?filtro=` conservado | PASS | PASS (sin coincidencias ⇒ Quitar filtros) | PASS (500; 403) |

## Evidencia

Todo en `evidencia/franja-b/`. Scripts del recorrido en `evidencia/franja-b/scripts/` (doble de sesión copiado de `e2e/apoyo/sonda-de-navegacion.mjs`; un navegador por vez, sin `waitForTimeout` ni `networkidle`).

```text
$ npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit
exit=0
$ npx ng lint
All files pass linting.
exit=0
$ python3 ../../scripts/verificar_frontend.py backoffice
  OK    · sin red en vista · sin literal de diseño · sin formato de dinero · sin console · ningún archivo de más de 200 líneas
TODO OK
exit=0
$ npx ng test --include='src/app/rutas/operacion/**/*.spec.ts' --exclude='src/**/*.a11y.spec.ts'
 Test Files  23 passed (23)
      Tests  135 passed (135)
$ npx ng test --include='src/app/rutas/operacion/**/*.a11y.spec.ts'
 Test Files  6 passed (6)
      Tests  26 passed (26)
```

Recorridos en navegador (Chromium, `ng serve` en 4302, simulado en 4010): 195 líneas PASS, 0 FALLO — `recorrido-{retiros,reclamos,custodia,cierres,reversos,desembolsos,formularios,otras}.txt`. Extracto (reversos):

```text
PASS reversos: primera página GET /contabilidad/reversos?limite=10 sin cursor
PASS reversos: Siguiente pide con el cursor (antesDeId=32fe0d80…)
PASS reversos: filtro Estado=Rechazado viaja como estados=RECHAZADO
PASS reversos: CSV sin coincidencias baja solo el encabezado (reversos-2026-10-26.csv)
PASS reversos: CSV de lo filtrado con el importe como se ve («4,44 (moneda no registrada)»)
PASS reversos: límite · Ir a 3 ⇒ última página parcial (5 filas) y Siguiente deshabilitado
PASS reversos: error · 500 ⇒ mensaje accionable con código de seguimiento, sin el crudo
PASS reversos: 375px light sin desborde horizontal (scrollWidth=375)
PASS reversos: consola sin errores (salvo los 403/500 provocados) []
```

Capturas (52, todas miradas): por vista `-1280-claro`, `-768-claro`, `-375-claro`, `-1280-oscuro`; además `-1280-error`, `-375-texto-largo` (reclamos, reversos, desembolsos), `custodia-formulario`, `custodia-recibo`, `cierres-comprobante`, `reclamos-respuesta-error`, `retiros-aprobacion-error`.

## No cubierto

- **Persistencia real**: el simulado no tiene estado. Todo POST (conciliación, cierre, respuesta a reclamo, aprobación de retiro) está verificado **contra el doble**: cuerpo del contrato, `Idempotency-Key`, respuesta 2xx y UI; no la persistencia ni la recarga con el dato nuevo.
- E2E del repo (`e2e/operacion-estado.e2e.ts`, `e2e/visual-carril-b.e2e.ts`, `shell.e2e.ts`) **no se corrieron** (son de H7). Riesgo: `operacion-estado.e2e.ts` espera `table thead th` = 6 (sigue siendo 6) y ordenar por «Servicio» (sigue ordenable).
- El cambio de lint en `formulario-de-conciliacion.ts` (`Object.fromEntries` en lugar de desestructurar) se hizo después del último recorrido de formularios; lo cubren los specs de cierre (corridos después), no el navegador.
- Teclado y lector de pantalla manuales: no se hicieron; solo axe (26 casos) y nombres accesibles por rol en el recorrido.
- Tema oscuro solo a 1280 (lo pedido); 768/375 oscuro no se capturaron.
- Respuesta a reclamo, aprobación de retiro y formularios: sin capturas a 768/375.

## Cambios de requisito (declarados)

- Reclamos, reversos y desembolsos: el estado se elegía con chips **múltiples** guardados en `?filtro=A,B`; ahora es un selector **simple** del kit y no queda en la URL. El servidor sigue recibiendo `estados`. En Arquitectura y estado el nivel **sí** sigue en la URL (`?filtro=N1`), pero de a uno: `?filtro=N1,N2` aplica solo N1.
- Conciliación y cierre: el formulario ya no está siempre abierto; se abre con el botón de la cabecera (los specs lo afirman así).

## Pedidos a otras franjas

- **H0 / `packages/ui` · `ap-paginacion`**: el `<select [value]="porPagina()">` se asigna antes de que existan las `<option>`, así que el selector muestra la **primera** opción aunque `porPagina` sea otra (visto: 14 filas visibles con «10» en el selector). Esquivado eligiendo como tamaño inicial la primera opción. Arreglo sugerido: `[selected]="n === porPagina()"` en cada `<option>`.
- **H0 · `ap-listado`**: `:host { display: grid }` sin `grid-template-columns: minmax(0, 1fr)` ⇒ una tabla ancha empuja la página (Arquitectura y estado: scrollWidth 985 a 768 px). Esquivado desde la pantalla con `ap-listado { grid-template-columns: minmax(0, 1fr) }`.
- **H0 · `ap-tabla-de-datos`**: `mono: true` en una columna pone en monoespaciado también la etiqueta de la tarjeta móvil. No se usa `mono` en Operación.
- **H0 · `ap-dialogo`**: los botones Confirmar/Cancelar no aceptan ícono (pedido de Pablo: ícono en todo botón).
- **Franja A**: ninguno (no se tocó `operacion.routes.ts`).

## Hallazgos fuera de alcance

- El simulado devuelve `estado: AUTORIZADA` a una aprobación pedida como `RECHAZADA` (ejemplo fijo, sin estado): la pantalla muestra lo que dice el servidor; no es un defecto del front.
- Los ejemplos del simulado traen datos incoherentes entre sí (conciliación `CUADRADA` con diferencia `4.44`; `ratioCobertura: "ratioCobertura"`; la salud de servicios no nombra ninguno de los 14 de la arquitectura): la UI los muestra tal cual.
- `GET /grupos/solicitudes-escaladas` no existe: si negocio quiere la cola de D-15, falta la operación en `grupos.yaml` (issue al repo del servicio de grupos).
- El tablero (`/tablero`) pide `?limite=1` a las mismas bandejas para sus avisos: no se tocó.
