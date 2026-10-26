# Franja F — Publicidad, Tablero y Admisiones

> **AVANCE: 17 / 17 microtareas HECHO.** Peldaño de la franja: `VERIFIED` contra el doble. Las mutaciones se verificaron contra el simulado Prism y contra rutas interceptadas: se vieron la UI, el pedido y la respuesta, pero el simulado no guarda nada. Falta la regresión de la suite completa del backoffice, que hace el agente principal en H7.

- Fecha: 2026-10-26 · Rama: `claude/franja-f` (worktree `/home/user/wt-f`) · Base: `c9ac3966`
- Resultado observable: en Publicidad, Tablero y Admisiones cada listado tiene buscador multicampo, filtros, descarga CSV/JSON de lo filtrado, filas por página, «Ir a», Anterior/Siguiente y acciones de fila con ícono y nombre. Los botones que no funcionaban ahora funcionan o se quitaron.
- Kill-test: en Anunciantes, un texto sin coincidencias muestra «Quitar filtros» y el CSV baja solo el encabezado → PASS (spec + e2e).

## Alcance
- IN: `apps/backoffice/src/app/rutas/{publicidad,tablero,admisiones}/**` (sin `*.routes.ts`), sus specs y los e2e `publicidad-anunciantes`, `tableros-por-rol`, `catalogo-gobierno`, `admisiones` y `tablero-y-permisos`.
- OUT: `packages/ui`, `nucleo`, `layout`, rutas, contratos.

## Microtareas

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Lotes por cursor para publicidad (`dominio/lotes-por-cursor.ts`, `barra-de-lotes.ts`) | 100 por lote; «Cargar 100 más» manda `despuesDeFecha`+`despuesDeId`; el estado viaja al servidor; una respuesta vieja no pisa la vigente | spec anunciantes «Cargar 100 más» | HECHO |
| H6.S1.M2 | Anunciantes con `ap-listado` + cifras + acciones de fila Desempeño/Liquidar | 25 filas → 3 páginas; búsqueda; filtros estado/tipo/moneda; CSV/JSON | `ng test publicidad` (10 casos) + e2e kit | HECHO |
| H6.S1.M3 | Alta de anunciante (botón roto: mandaba SOCIO_COMERCIAL sin `socioComercialId`, R-PUB-01) | POST con tipo + id correcto; un error queda en el diálogo con lo escrito | spec «alta…» + «alta rechazada…» | HECHO |
| H6.S1.M4 | Campañas con `ap-listado`; «Todos» pide los 6 estados; «Revisar» con la campaña en el estado de la navegación | GET con `estados=…`; navegación correcta | spec campañas (8 casos) | HECHO |
| H6.S1.M5 | Crear campaña (botón roto: cuenta `00000000-…` y `conjuntos: []`, rechazado por `minItems: 1`) | cuenta elegida entre los anunciantes + 1 conjunto; POST 201 en el navegador | spec + navegador (cuerpo del POST abajo) | HECHO |
| H6.S1.M6 | Panel de aprobación: muestra el resultado, el error y el detalle si viene de la cola | aprobar → alerta y no vuelve a ofrecer la decisión; rechazo fallido conserva el motivo | spec panel (7 casos) | HECHO |
| H6.S1.M7 | Moderación con `ap-listado`, Ver pieza/Aprobar/Rechazar con ícono, estado en el servidor | la fila pasa a «Aprobada» solo después del 2xx; R-PUB-05 traducido | spec moderación (7) + navegador POST 201 | HECHO |
| H6.S1.M8 | Desempeño y Liquidación: elegir la cuenta por anunciante, `?cuenta=`, sin «BOB» inventado, factura con su moneda y error en el diálogo; arreglo de `value()` en error | consulta sola con `?cuenta=`; liquidar → factura; 422 en el diálogo | specs nuevos (3 + 3) + navegador | HECHO |
| H6.S2.M1 | Tablero inicio: íconos en bandejas, accesos y monitoreo; cada «Abrir bandeja» lleva a su ruta | clic en las 5 tarjetas y en los 13 accesos | navegador (abajo) + spec | HECHO |
| H6.S2.M2 | Acceso «Sistemas» solo con el rol que pide su guardia (antes rebotaba a Operación) | sin PLATAFORMA/SEGURIDAD no aparece | spec tablero | HECHO |
| H6.S2.M3 | Tableros por rol: `ap-grupo-de-indicadores` con ícono por grupo | sin datos / suprimido siguen sin dibujarse como cifra | spec + e2e `tableros-por-rol` | HECHO |
| H6.S2.M4 | Endpoints del día con `ap-listado` (orden numérico, filtros, CSV/JSON, error con reintento) | 2 filas → la más llamada primero; filtro servicio → 1 fila | spec (7) + navegador CSV | HECHO |
| H6.S2.M5 | Catálogo (paginado en el servidor): descarga de la página, 100 por página, Ver ficha/Editar (CU-130), íconos en ficha, gobierno y revisión | `limite=100` llega al servidor; Editar solo con `CATALOGO_DATOS_EDITAR` | spec (2 nuevos) + navegador | HECHO |
| H6.S3.M1 | Admisiones: entrada con pestañas, pendientes de un grupo (`listarSolicitudesDeIngreso`) y «Resolver» | `?grupo=` pide la cola; Resolver abre el expediente | spec entrada (4) + navegador | HECHO |
| H6.S3.M2 | Expediente: centrado, «Volver», historial con CSV/JSON, íconos | descarga del historial; resolver → POST con `revisionEsperada` | navegador + e2e `admisiones` (8) | HECHO |
| H6.S4.M1 | e2e: anunciantes recorre el kit; admisiones elige la pestaña nueva (mismo requisito) | 42/42 en verde contra :4306 | `playwright test` (5 archivos) | HECHO |
| H6.S4.M2 | Layout medido a 375/768/1280 + oscuro: sin desborde horizontal y texto largo en su propia columna | `scrollWidth ≤ ancho` en las 12 vistas × 4 | `capturas.mjs` sin «DESBORDE» + capturas revisadas | HECHO |

## Evidencia

Comandos de cierre (salida literal en `evidencia/franja-f/cierre.txt`):

```text
$ npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit
exit 0
$ npx ng lint
All files pass linting.
exit 0
$ python3 ../../scripts/verificar_frontend.py backoffice
TODO OK
$ ng test publicidad     → Test Files 10 passed · Tests 62 passed     · a11y 3/3
$ ng test tablero        → Test Files 12 passed · Tests 84 passed     · a11y 5/5
$ ng test admisiones     → Test Files 4 passed  · Tests 28 passed     · a11y 5/5
$ npx playwright test (publicidad-anunciantes, tableros-por-rol, catalogo-gobierno, admisiones, tablero-y-permisos) contra :4306
  42 passed (2.4m)
```

Recorrido en navegador (Chromium, sesión de prueba sustituida por un doble como en `sonda-de-navegacion.mjs`, simulado en :4010). La consola y la red quedaron sin errores en todas las corridas (`errores: []`):

```text
· Todos -> GET /api/v1/publicidad/campanas?limite=100&estados=BORRADOR,EN_REVISION,ACTIVA,PAUSADA,RECHAZADA,FINALIZADA
· POST cuerpo: {"cuentaPublicitariaId":"1c2d00f1-…","nombre":"Campaña sintética de prueba","objetivo":"VISIBILIDAD_MARCA","presupuestoTotal":"500.00","moneda":"BOB","fechaInicio":"2026-11-01T04:00:00.000Z","conjuntos":[{…,"presupuestoDiario":"20.00","pujaMaxima":"1.50","modeloPuja":"CPM"}]}  → 201 · alerta creada OK
· moderar: /publicidad/piezas-creativas/…/revision {"decision":"APROBADA"} -> 201 · fila marcada Aprobada
· desempeño: /publicidad/cuentas/1c2d…/consumo?periodo=2026-09 200 · liquidar: {"periodo":"2026-09"} -> 201 · factura mostrada
· anunciantes (25 sintéticos): Página 1 de 3 · «Ir a» 3 → 5 filas · CSV 24 filas · JSON 24 filas · vacío por filtro OK
· bandeja Retiros/Reclamos/Alertas/Casos/Desembolsos -> /operacion/retiros, /operacion/reclamos, /cumplimiento/alertas, /cumplimiento/casos, /operacion/desembolsos
· Actualizar vuelve a pedir: /billetera/retiros?limite=1 · 13 accesos → su sección (Sistemas rebotaba: corregido en H6.S2.M2)
· endpoints csv: Servicio,Método,Ruta,Solicitudes,Errores,p95 · catálogo csv: 20 filas · 100 por página -> …/tablas?pagina=1&limite=100
· ver ficha -> /tablero/catalogo-de-datos/aportes/comprobante_manual · editar ficha -> …/ficha
· admisiones: GET /grupos/7a1c…/solicitudes (url ?grupo=…) · Resolver -> /admisiones/d9fc… · historial CSV
· POST resoluciones: {"decision":"ACEPTAR","motivo":"…","revisionEsperada":1,"propuestaId":"a1b2…"} -> 200
```

Capturas (`evidencia/franja-f/<vista>-<ancho>-<tema>.png`, 12 vistas × {1280, 768, 375 claro; 1280 oscuro}) de `pub-anunciantes`, `pub-campanas`, `pub-moderacion`, `pub-desempeno`, `pub-liquidacion`, `pub-aprobacion`, `tab-inicio`, `tab-financiero`, `tab-sistemas`, `tab-catalogo`, `adm-entrada` y `adm-expediente`. Las miré todas. Lo que corregí al mirarlas: desborde horizontal (grilla sin `minmax(0, 1fr)`), columnas tapadas por la columna fija de acciones, y texto partido letra por letra en la vista de tarjetas de 375.

## No cubierto
- **Persistencia real**: el simulado no guarda estado. Las altas, aprobaciones, moderaciones, liquidaciones y resoluciones están verificadas contra el doble (UI → pedido → respuesta), no contra una base.
- Un 403 real de `listarSolicitudesDeIngreso` para backoffice: solo se simuló.
- «Cargar 100 más» contra un servidor con más de 100 filas reales: el simulado siempre devuelve un cursor siguiente, así que se ejercitó en el spec y en el navegador, pero no en el fin real de la lista.
- Lector de pantalla: no se probó. Lo que sí está verificado es axe en a11y y el nombre accesible por fila.
- Cuentas publicitarias más allá de los primeros 100 anunciantes en el selector de Desempeño, Liquidación y Crear campaña.

## Decisiones y ambigüedades (a confirmar con Pablo)
- **A-F1** Publicidad pagina por cursor, sin total y sin búsqueda en el contrato. Decidí pedir lotes de 100 (el máximo del contrato) y que el kit busque, filtre, pagine y exporte **lo cargado**, con «Cargar 100 más». El estado viaja al servidor. La ayuda del buscador dice «en los … cargados».
- **A-F2** El contrato de publicidad no tiene editar, pausar ni eliminar anunciante o campaña. Por eso no hay Editar ni Eliminar. Las acciones de fila son las que sí existen: Desempeño y Liquidar la cuenta, Revisar, Ver pieza, Aprobar y Rechazar.
- **A-F3** `listarSolicitudesDeIngreso` se describe como «la cola del organizador». La uso desde backoffice; si el servidor responde 403, la pantalla lo dice. También puede quedar como cola solo del organizador: hay que confirmarlo.
- **A-F4** `SalidaConsumo` no trae moneda. Muestro el total sin inventar «BOB» y lo aclaro en la vista (regla 91.1.2).
- **A-F5** Crear campaña pide los identificadores de segmento y de espacio publicitario a mano: el contrato no publica un listado de ellos.
- **A-F6** En Catálogo (paginado en el servidor) la descarga baja **la página filtrada**, no todo lo filtrado, y la vista lo dice. Para bajar más, se sube a 100 por página.

## Pedidos a otras franjas
- **Dueño del kit (`packages/ui`)**: el `:host` de `ap-listado` es `display: grid` sin `grid-template-columns: minmax(0, 1fr)`, así que una tabla ancha lo ensancha y desborda la página. Lo compenso en cada pantalla con `ap-listado { grid-template-columns: minmax(0, 1fr) }`. Conviene arreglarlo en el kit.
- **Kit**: `ap-dialogo` no acepta ícono en sus botones (Confirmar y Cancelar quedan sin ícono).
- **Franja D/A**: exportar `ROLES_PERMITIDOS` de `rutas/sistemas/guardia-rol-sistemas.ts`. El Tablero repite `['PLATAFORMA', 'SEGURIDAD']` para no ofrecer un acceso que rebota.
- **Franja A**: el menú tiene el mismo problema con «Sistemas» (F1 del plan).

## Hallazgos fuera de alcance
- `e2e/admisiones.e2e.ts` y `tableros-por-rol.e2e.ts` reescriben con `capturar()` capturas versionadas de `docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/`. Cada corrida e2e ensucia el árbol (las restauré).
- En el Tablero de sistemas, el ejemplo del simulado trae `p95Ms: "p95Ms"`: se muestra «p95Ms ms». Es un dato del simulado, no de la pantalla.
- Las capturas `fullPage` dibujan la barra lateral fija cortada. Es un artefacto de la captura del shell (franja A), no un defecto de la vista.

## Riesgos residuales
- Las mutaciones no se probaron contra el backend real (ver «No cubierto»).
- La exportación es de lo cargado en el navegador. No reemplaza la exportación CU-58 con huella del servidor (A3 del plan).
