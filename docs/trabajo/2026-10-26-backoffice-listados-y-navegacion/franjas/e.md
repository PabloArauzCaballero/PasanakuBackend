# Franja E — Contabilidad y Configuración (H5)

> **AVANCE: 19 / 19 — 100 %.** Todas las microtareas con DoD ejecutado y salida pegada. Peldaño del área: `VERIFIED`
> **contra dobles** (Prism sin estado + `page.route` con datos sintéticos), con prueba visual. No es `REGRESSION_VERIFIED`:
> no se corrió la suite completa del backoffice (eso es H7, del agente principal).

- Fecha: 2026-10-26 · Worktree `/home/user/wt-e` · Rama `claude/franja-e` (desde `c9ac3966`) · Último commit de código: `4820a578`
- Plan raíz: [`../PLAN.md`](../PLAN.md) · Guía: [`../GUIA-DE-FRANJA.md`](../GUIA-DE-FRANJA.md) · Evidencia: [`../evidencia/franja-e/`](../evidencia/franja-e/)
- Resultado observable: en cada vista de `/contabilidad/**` y `/configuracion/**` hay cabecera con ícono y eyebrow, cifras
  arriba donde corresponde, y cada listado tiene buscador multicampo, filtros, CSV/JSON de lo filtrado, paginación (filas
  por página, ir a la página N, Anterior/Siguiente) y acciones de fila con ícono + nombre. Cada botón dispara la petición
  del contrato o no se dibuja.

## Hallazgos (descubrimiento)

- **E-F1 · Todos los listados de la franja paginan por cursor en el servidor** (`erp.yaml` A6, `tarifas.yaml`,
  `notificaciones.yaml`, `auditoria.yaml`): `limite` 1–100, filtro `estados`, sin total, sin búsqueda de texto y sin orden
  por columna. Por eso **no** se usa `ap-listado` + `crearListado` (que pagina en el navegador), sino la composición manual
  que pide la guía: `contabilidad/listado/listado-por-cursor.ts` (estado) + `tabla-por-cursor.ts` (presentación, misma
  anatomía que `ap-listado`). El `estado` viaja como `estados`; el buscador y los demás filtros actúan sobre **la página
  cargada**, y la ayuda del buscador lo dice. CSV/JSON bajan lo filtrado de esa página.
- **E-F2 · No hay Editar ni Eliminar en ningún contrato de la franja.** `erp.yaml` no tiene PUT/PATCH/DELETE; `tarifas.yaml`
  y `notificaciones.yaml` tampoco para tarifarios ni plantillas. Además asientos, facturas, cobros, pagos y períodos son
  append-only (regla 91.1.3). Las acciones de fila son de dominio, con ícono + nombre, solo donde el contrato tiene la
  operación y el estado la permite: **Aprobar** orden en `BORRADOR` (`aprobarOrdenDeCompra`), **Registrar el pago**
  (`pagarFacturaDeProveedor`), **Registrar el cobro** (`registrarCobro`), **Depreciar en el período** (`depreciarActivo`).
  Configuración queda de solo lectura (carril L4).
- **E-F3 · Botones que «no funcionaban», corregidos:**
  1. Períodos · «Asentar en este período» solo guardaba una señal: ningún contrato publica el alta de un asiento → **botón
     muerto**. Reemplazado por «Admite asientos: está abierto.» (el requisito «un período cerrado no ofrece asentar y dice
     por qué» se conserva y sigue probado).
  2. Estados financieros · «Descargar el documento» traía el blob y **nunca lo entregaba** al navegador. Ahora lo guarda con
     el nombre que lleva el hash (`estados/guardar-archivo.ts`).
  3. Facturas / Cobros / Activos · tras pagar, cobrar o depreciar con éxito **la lista no se recargaba ni avisaba**. Ahora
     recarga y muestra un aviso de éxito.
  4. Órdenes · `crearAprobacionDeOrden` existía en el dominio **sin botón**: una orden en borrador no se podía aprobar.
- **E-F4 · `cargadorPorCursor` (`nucleo/tabla/cursor.ts`) calcula mal el total al volver atrás** después de conocer la
  última página: `total = vistas` con las filas vistas *hasta la página pedida*. Efecto: de la 3 a la 2 se mostraba «Página 2
  de 2» con «Siguiente» deshabilitado. Reproducido en el navegador y en una prueba unitaria. Compensado en mi capa (ver
  «Pedidos a otras franjas»).
- **E-F5 · `ap-monto` lanza error con importes de 4 decimales** (`^-?\d+\.\d{2}$`), y el costo de un envío viaja con 4
  (`0.0125`). En Mensajería el costo se muestra como cadena exacta con su moneda, sin `ap-monto` (que crashearía).
- **E-F6 · La defensa anti-fórmulas del CSV** (`packages/ui/exportar-listado`) antepone `'` a un texto que empieza con `-`.
  Una desviación negativa salía como `'-359.90 BOB`. El importe exportado ahora es `BOB -359.90` (moneda primero).
- **E-F7 · Consumidores externos:** `contabilidad/listado/estado-de-listado.ts` y `listado.apoyo.ts` los usa también
  Publicidad (franja F): **no se borraron**; sus 50 pruebas siguen verdes. `configuracion/aviso-de-listado.ts` y
  `filtro-de-estado.ts` solo los usaba Configuración: **borrados**.

## Microtareas

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H5.S0.M1 | Listado por cursor compartido (`listado-por-cursor.ts` + `tabla-por-cursor.ts`) | 25 filas por cursor: «Ir a 3» → 5 filas; buscar filtra la página sin pedir al servidor; CSV con lo filtrado; vacío / error con reintento; volver atrás no apaga «Siguiente» | `ng test --include=…/listado/tabla-por-cursor.spec.ts` → 13/13 | HECHO |
| H5.S1.M1 | Presupuesto con el listado + KPI de conteos | spec 3 niveles + recorrido | spec 8 + a11y; recorrido 15/15 PASS | HECHO |
| H5.S1.M2 | Órdenes con listado + «Aprobar» (BORRADOR) | POST `/aprobacion` con Idempotency-Key, recarga, error no traba | `tablas-de-compras.spec.ts`; recorrido PASS | HECHO |
| H5.S1.M3 | Facturas con listado + «Registrar el pago» que recarga y avisa | POST `/pagos` con `"monto":"100.50"`, recarga, «Página 1 de 2» | ídem | HECHO |
| H5.S1.M4 | Cobros con listado + «Registrar el cobro» que recarga y avisa | POST `/cobros`; con 500 conserva lo escrito y no traba | `pantalla-de-cobros.spec.ts`; recorrido PASS | HECHO |
| H5.S1.M5 | Activos con listado + «Depreciar» que recarga y avisa | POST `/depreciaciones` con el período de la URL | `pantalla-de-activos.spec.ts`; recorrido PASS | HECHO |
| H5.S1.M6 | Períodos: layout, íconos, sin botón muerto | abierto dice «Admite asientos» sin botón; cerrar → POST `/cierre` | `periodo/**` 25 pruebas; recorrido PASS | HECHO |
| H5.S1.M7 | Estados financieros: layout + la descarga entrega el archivo | descarga con hash en el nombre; hash discrepante → no se entrega | `estados/**` 9 pruebas; recorrido PASS (2 pasos) | HECHO |
| H5.S2.M1 | Tarifario con listado | spec 3 niveles | `tarifario/**`; recorrido PASS | HECHO |
| H5.S2.M2 | Mensajería: plantillas y envíos en pestañas | spec 3 niveles; `?filtro=` abre Envíos filtrado | `mensajeria/**`; recorrido PASS (2 listados) | HECHO |
| H5.S2.M3 | Reportes: catálogo e historial en pestañas (convive con `Exportador` CU-58, sin tocarlo) | spec 3 niveles | `reportes/**`; recorrido PASS (2 listados) | HECHO |
| H5.S2.M4 | Inicio de Configuración con íconos | 3 accesos con ícono que navegan | `configuracion.a11y.spec.ts`; recorrido PASS | HECHO |
| H5.S3.M1 | Recorrido en navegador (aceptado/límite/error) con red verificada y descargas | 138 pasos PASS, red/consola solo con los 22 errores provocados | `node evidencia/franja-e/recorrido.mjs` | HECHO |
| H5.S3.M2 | Capturas 1280/768/375 claro + 1280 oscuro, miradas | 52 PNG en `evidencia/franja-e/` | Read de cada PNG (ver «Inspección visual») | HECHO |
| H5.S3.M3 | Comandos de cierre | tsc, lint, verificador, tests y a11y en 0 | `cierre-*.txt` | HECHO |
| H5.S3.M4 | *(descubierta)* Total del cursor que baja al volver atrás o recargar (E-F4) | «2 de 3» tras 3→2; «1 de 2» tras recargar | 2 pruebas nuevas en `tabla-por-cursor.spec.ts` (fallaban antes) | HECHO |
| H5.S3.M5 | *(descubierta)* Tablas anchas estiraban la página (scrollWidth 1457 a 1280) | `scrollWidth ≤ ancho` en todas las vistas | recorrido: paso de capturas PASS en las 13 vistas | HECHO |
| H5.S3.M6 | *(descubierta)* Importe negativo con apóstrofo en el CSV (E-F6) | CSV trae `BOB -359.90` | prueba nueva + recorrido (`exportar: 'BOB -359.90'`) | HECHO |
| H5.S3.M7 | *(descubierta)* Factura `REGISTRADA` decía «no tiene saldo» | dice «todavía no está aprobada» | prueba nueva en `cu103-facturas.spec.ts` | HECHO |

## Evidencia

Comandos de cierre (salida literal en `evidencia/franja-e/cierre-tsc.txt`, `cierre-lint.txt`, `cierre-tests.txt`):

```text
$ npx tsc -p tsconfig.app.json --noEmit
exit=0
$ npx tsc -p tsconfig.spec.json --noEmit
exit=0
$ npx ng lint
All files pass linting.
exit=0
$ python3 ../../scripts/verificar_frontend.py backoffice
  OK    · sin red en vista / sin literal de diseño / sin formato de dinero / sin console / ningún archivo de más de 200 líneas
TODO OK
exit=0
$ npx ng test --include='src/app/rutas/contabilidad/**/*.spec.ts' --exclude='src/**/*.a11y.spec.ts'
 Test Files  18 passed (18)
      Tests  185 passed (185)
$ npx ng test --include='src/app/rutas/contabilidad/**/*.a11y.spec.ts'
 Test Files  6 passed (6)
      Tests  9 passed (9)
$ npx ng test --include='src/app/rutas/configuracion/**/*.spec.ts' --exclude='src/**/*.a11y.spec.ts'
 Test Files  7 passed (7)
      Tests  29 passed (29)
$ npx ng test --include='src/app/rutas/configuracion/**/*.a11y.spec.ts'
 Test Files  1 passed (1)
      Tests  4 passed (4)
$ npx ng test --include=src/app/rutas/publicidad/**/*.spec.ts   (consumidor de estado-de-listado y listado.apoyo)
 Test Files  11 passed (11)
      Tests  50 passed (50)
```

Recorrido en navegador (`evidencia/franja-e/recorrido.log` + `recorrido.json`; script `recorrido.mjs`, datos `datos.mjs`):

```text
{"pasos":138,"fallos":0,"erroresDeRed":22,"provocados":22,"consola":0}
PASS · compras-ordenes · aceptado · «Aprobar» OC-0005 → POST /aprobacion con Idempotency-Key y recarga — POST /api/v1/erp/ordenes-de-compra/…0005/aprobacion · Idempotency-Key 1232b46a…
PASS · compras-facturas · aceptado · «Registrar el pago» F-0001 → POST /pagos con monto exacto y recarga — …/pagos · {"monto":"100.50","moneda":"BOB","formaPago":"TRANSFERENCIA"}
PASS · cobros · error · cobro con 500 → conserva lo escrito y el botón no queda trabado
PASS · cobros · aceptado · reintento del cobro → POST /cobros con monto 50.00 y recarga — …/cobros · {"monto":"50.00","formaCobro":"TRANSFERENCIA"} · clave d6c1f936…
PASS · activos · aceptado · «Depreciar» AF-001 → POST /depreciaciones con el período y recarga — {"periodoContableId":"b6ab0151-…"}
PASS · periodo · aceptado · período de hoy, ejercicios, meses; sin «Asentar»; cerrar → POST /cierre — {"glosa":"Cierre de enero 1"}
PASS · estados · error · Prism repite un hash vacío junto al documento → NO se entrega el archivo y se dice por qué
PASS · estados · aceptado · generar → POST; descargar → GET documento (doble con el hash generado) y el navegador recibe el archivo
PASS · presupuesto · aceptado · CSV y JSON de lo filtrado — …csv (2 líneas: «Marketing,Operación Ñandú,abril 2026,BOB 1000.00,BOB 640.10,BOB -359.90,Borrador»)
```

Por cada uno de los 10 listados el recorrido ejercita: lista 10 de 25 con `limite=10`; buscar (sin pedir al servidor);
filtro local; filtro del servidor (`estados=…` en la URL pedida); filas por página 20 (`limite=20`); Siguiente con cursor,
«Ir a 3» (5 filas), Anterior → «Página 2 de 3» con Siguiente habilitado → «Página 1 de 3»; CSV y JSON de lo filtrado
(contenido leído del archivo descargado, `evidencia/franja-e/descargas/`); capturas sin scroll horizontal; **límite**:
búsqueda sin coincidencias → «Quitar filtros», colección vacía → vacío orientado sin paginación, una fila → «Página 1 de 1»;
**error**: 500 y 403 → mensaje traducido con código de seguimiento y sin el cuerpo crudo, «Volver a intentar» repite el pedido.

Capturas (`evidencia/franja-e/<vista>-{1280,768,375}-claro.png` y `-1280-oscuro.png`): presupuesto, compras-ordenes,
compras-facturas, cobros, activos, periodo, estados, configuracion, tarifario, mensajeria-plantillas, mensajeria-envios,
reportes-catalogo, reportes-ejecuciones.

### Inspección visual (lo que se vio y se corrigió)

- Envíos e Historial a 1280: la tabla estiraba la página (scrollWidth 1457). → pistas `minmax(0, 1fr)` en las grillas (H5.S3.M5).
- Cobros/Facturas a 1280: la columna fija de acciones tapaba el **saldo** («Bs 30(»). → los textos largos se parten en la
  celda y el motivo se acota; los importes quedan visibles.
- Órdenes: la columna «Facturable» con motivo largo dejaba filas muy altas → el motivo pasa a la celda de acciones.
- Activos a 768: los tres importes quedaban fuera del marco → «Valor en libros» va antes de la categoría.
- Compras: el aviso general decía «Esta cuenta aprobó esta factura» (texto de una fila) → regla general.
- Configuración: «Solo lectura» repetido en título y cuerpo del aviso; tarjetas de inicio de alto distinto → corregido.
- Oscuro: primeras capturas a mitad de la transición de color (filas grises) → se esperan las animaciones; recapturadas.
- Períodos y Estados a 375/768/1280 claro y 1280 oscuro: correctos.

## No cubierto

- **Todo lo verificado con mutación es contra dobles**: Prism (sin estado) responde el ejemplo del contrato. Ninguna
  aprobación, pago, cobro, depreciación ni cierre se **persistió**; la recarga posterior la sirve el doble de `page.route`.
  Falta verificarlo contra el ERP real (asientos generados, idempotencia del lado servidor, cuadre: regla 91.6).
- Los listados del recorrido usan datos **sintéticos** de `page.route` (25 filas), porque Prism devuelve siempre 1 fila con
  un cursor infinito. Contra el simulado crudo solo se ejercitaron período vigente, ejercicios, cierre, generación y descarga.
- La descarga del estado financiero en el camino aceptado usa un doble del documento con el hash generado: Prism repite
  `X-Hash-Contenido` vacío (que la pantalla rechaza, como corresponde).
- A 1280 con el menú abierto, el chip de **Estado** queda debajo de la columna fija de acciones en Facturas, Cobros y
  Órdenes y se ve desplazando la tabla. No es un importe, y el motivo de la fila está escrito al lado.
- Estados financieros fuera del período de hoy: el nombre y si está cerrado vienen de las entradas de la ruta, que hoy nadie
  pasa (`estados/:periodoId`): ese caso muestra «Período» y «Provisorio».
- Navegación por teclado y lector de pantalla: solo axe (`*.a11y.spec.ts`), sin prueba manual.
- No se corrió la suite completa del backoffice ni los e2e de Playwright del repo.

## Pedidos a otras franjas

- **Dueño de `nucleo/tabla/cursor.ts` (agente principal / H7):** `cargadorPorCursor` devuelve `total = vistas` cuando `fin`
  ya se conoce y la página pedida no es la última (líneas 42–45). Debería ser `max(fin * tamano + filasDeLaUltima, …)` o
  recordar el total final. Mi capa lo compensa (`conocido` por consulta + recarga = consulta nueva con la marca `__recarga`
  en `filtros`, que ningún adaptador envía). Cuando se corrija en `nucleo`, esa compensación sobra.
- **`packages/ui` (H0):** (1) `ap-monto` no admite importes con más de 2 decimales (costo de envío `0.0125` en
  `notificaciones.yaml`); (2) en modo tarjetas a 375, el título de `ap-tabla-de-datos` se parte en una palabra por línea
  («Órdenes / de / compra»); (3) `exportar.ts` antepone `'` a un texto que empieza con `-` aunque sea un importe con moneda.
  Lo esquivé poniendo la moneda primero.
- **Franja A (`contabilidad.routes.ts`):** la ruta `estados/:periodoId` no pasa `periodoNombre` ni `periodoCerrado`. Para
  un período que no es el de hoy, la pantalla no sabe si es definitivo. Pedido: un resolver de la ruta o un
  `GET /erp/periodos/{id}` en el contrato.

## Hallazgos fuera de alcance

- `configuracion/dominio/listado.ts` conserva la clase `EstadoDeListado`, que ya no usa ninguna pantalla. Tiene prueba
  propia (`cu34-tarifarios.spec.ts`), así que no la borré.
- `contabilidad/dominio/contrato-erp.ts` conserva `paginarEnElAdaptador` y `filtroPorEstado`, sin consumidores en pantalla
  (ya era así antes).
- Prism devuelve datos incoherentes para los períodos (un período `ABIERTO` con `cerradoEn` y totales sellados): la
  pantalla los muestra tal cual.

## Desvíos del plan

- Composición manual por cursor en lugar de `ap-listado` (lo pide la guía para listados paginados en el servidor).
- Mensajería y Reportes pasan de dos tablas apiladas a **una tarjeta con pestañas** (un solo listado montado: una barra de
  filtros y una paginación en el DOM). Un `?filtro=` en la URL abre directamente Envíos o Historial.
- `configuracion/**` importa el listado compartido de `contabilidad/listado/`: no hay carpeta común en `rutas/`.
- Cuatro microtareas descubiertas en el camino (H5.S3.M4–M7), registradas arriba.

## Decisiones y ambigüedades

- A3 (plan raíz): CSV/JSON = lo filtrado **de la página cargada**, porque el servidor no busca por texto ni da total. El
  contador del botón («10 filas») y la ayuda del buscador lo dicen. A confirmar con Pablo si quiere exportar todas las
  páginas (habría que recorrer el cursor completo).
- Importe exportado = `MONEDA monto` (cadena exacta del contrato), no columnas separadas.
- Depreciar sin diálogo de confirmación (como estaba): la corrida es idempotente por período en el servidor.
- Las cifras de arriba son **conteos de la página**, nunca sumas de importes (regla 91.1.5).
