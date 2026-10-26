# Franja D — Sistemas (`apps/backoffice/src/app/rutas/sistemas/**`)

> **AVANCE: 22 / 22 — 100 %** de microtareas de la franja en `HECHO`. Peldaño: `VERIFIED` contra dobles
> (simulado OpenAPI sin estado en 4010 + `page.route`); **nada persistido** (el simulado no guarda). La integración
> con servicios reales y la revisión independiente (H7) quedan fuera de esta franja.

- Fecha: 2026-10-26 · Worktree `/home/user/wt-d` · Rama `claude/franja-d` (desde `c9ac3966`) · Servidor `127.0.0.1:4304`
- Plan raíz: `../PLAN.md` (H4). Guía: `../GUIA-DE-FRANJA.md`.
- Resultado observable: un operador con rol `PLATAFORMA` entra a cada vista de Sistemas y ve cabecera con ícono propio,
  «Actualizar», cifras arriba y cada colección como listado: buscador multicampo, filtros, CSV/JSON de lo filtrado,
  acciones de fila ícono + nombre y paginación completa; las mutaciones del contrato (usuarios internos, elementos de
  catálogo) van por diálogos accesibles que conservan lo escrito ante un fallo.

## Hallazgos del descubrimiento (hechos)

- D-F1 · Las 9 vistas «de puerto» (servicios, despliegues, base, respaldos, proveedores, outbox, webhooks, accesos,
  incidentes) no tienen contrato. Sin `<meta name="aportaya-demo" content="true">` (el `index.html` de desarrollo no la
  trae), `detectarModoFuentesDeSistemas()` = `produccion` y muestran el error «todavía no existe un origen de datos real»;
  con la etiqueta, `proveedor-fuentes.demo.ts` sirve `@aportaya/simulado/backoffice-sistemas`. Se probaron los dos modos.
- D-F2 · Contratos reales: usuarios internos (`identidad.yaml`: GET/POST `/usuarios-internos`, GET/PATCH `/{id}`,
  PUT `/{id}/roles`, POST `/{id}/desbloquear`), roles (`GET /roles`) y permisos (`GET /permisos`) de solo lectura,
  bitácora (`auditoria.yaml`: cursor `antesDe`/`antesDeId`, `limite` ≤ 100; append-only) y catálogos (GET lista, ficha,
  elementos; `PATCH /elementos/{id}` solo `{activo, motivo}`).
- D-F3 · No hay `DELETE` en ninguno de esos contratos → **no se dibuja ningún «Eliminar»** en Sistemas.
- D-F4 · Botones que «no funcionaban» (todos corregidos):
  - B1 Ficha de usuario: «Suspender/Reactivar» y «Desbloquear ahora» usaban `prompt()` y `subscribe(next)` sin `error`:
    un rechazo no mostraba nada.
  - B2 Alta, asignar roles y cambiar estado de elemento leían `err.error?.mensaje`, pero `errores.interceptor.ts`
    entrega `ErrorTraducido` (`err.mensaje`): siempre salía el genérico, sin código de seguimiento.
  - B3 Alta: `rolCodigo` arrancaba fijo en `'OPERATIONS_ANALYST'` aunque el catálogo no lo trajera.
  - B4 Asignar roles: selección inicial con `setTimeout`.
  - B5 Los tres modales eran `div role=dialog` sin foco atrapado ni devuelto, con un botón «✕» de solo glifo.
  - B6 `resource()` envuelve todo rechazo que no es `Error` en `ResourceWrappedError` (original en `.cause`): en todas
    las vistas el mensaje traducido y el código de seguimiento se perdían. `compartido/carga.ts` lo desenvuelve.
  - B7 Íconos de cabecera repetidos (`candado` ×5, `baseDeDatos` ×3) y «+» como texto.

## Alcance

- IN: `rutas/sistemas/**` salvo `sistemas.routes.ts`; specs y a11y.
- OUT: `packages/ui/**`, `nucleo/**`, `layout/**`, `*.routes.ts`, contratos, simulado.
- Ambigüedades (a confirmar con Pablo):
  - AD1 «Eliminar usuario interno»: sin DELETE en el contrato. Supuesto: «Suspender» (PATCH `estado=SUSPENDIDO` + motivo)
    y «Reactivar». Una baja definitiva exigiría contrato nuevo.
  - AD2 «Editar elemento de catálogo»: el PATCH solo admite `activo` + `motivo` → acción «Activar/Desactivar», sin «Editar».
  - AD3 Bitácora por cursor: «Ir a N» recorre las páginas intermedias pidiendo cada cursor; el buscador y CSV/JSON
    trabajan sobre la página cargada (el contrato no tiene búsqueda libre) y la ayuda del buscador lo dice.

## Microtareas

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | `compartido/`: `cargaDe`, `filasDe`, `mensajeDeError`, `ap-cifras-de-sistemas` | error con `.cause` → mensaje + idPeticion | `compartido/carga.spec.ts` 3/3 | HECHO |
| H4.S2.M1 | Servicios y SLO | 25 filas → «Página 1 de 3»; buscar/filtrar | spec 5 + 4 previas | HECHO |
| H4.S2.M2 | Despliegues (listado) + interruptores | doble confirmación intacta; listado pagina | spec 9 | HECHO |
| H4.S2.M3 | Base y migraciones | filtro Estado | spec 5 | HECHO |
| H4.S2.M4 | Respaldos (VENCIDA filtrable y contada) | filtro Restauración | spec 5 + navegador | HECHO |
| H4.S2.M5 | Proveedores (monto + moneda; no suma entre monedas) | CSV con moneda | spec 5 | HECHO |
| H4.S2.M6 | Outbox y descartados (dos listados, sin Editar/Eliminar) | motivo visible; sin Eliminar | spec 6 + a11y | HECHO |
| H4.S2.M7 | Webhooks | filtros Dirección/Último estado | spec 5 | HECHO |
| H4.S2.M8 | Accesos | filtros Rol/Ámbito | spec 5 | HECHO |
| H4.S2.M9 | Incidentes | filtros Severidad/Estado | spec 5 | HECHO |
| H4.S3.M1 | Usuarios internos: Ver/Editar/Roles/Suspender/Desbloquear | PATCH/PUT/POST con cuerpo correcto; fallo conserva lo escrito | `…mutaciones.spec.ts` 8 + navegador | HECHO |
| H4.S3.M2 | Alta en `ap-dialogo` (B2, B3, B5) | rol inicial = 1.º del catálogo; 422 conserva lo escrito | `modal-alta…spec.ts` 3 | HECHO |
| H4.S3.M3 | Asignar roles en `ap-dialogo` (B2, B4, B5) | PUT con selección + motivo | mutaciones.spec + navegador | HECHO |
| H4.S3.M4 | Ficha de usuario sin `prompt()` (B1) | Suspender → diálogo → PATCH | detalle.spec + navegador | HECHO |
| H4.S4.M1 | Roles internos (Ver permisos en cajón) | filtros Depto/Ámbito/Origen; sin Editar/Eliminar | spec 4 + navegador | HECHO |
| H4.S4.M2 | Permisos internos | filtros Riesgo/Recurso/Motivo/MFA | spec 4 + navegador | HECHO |
| H4.S5.M1 | Bitácora: cursor, Ir a N, cajón, sin Eliminar | Siguiente manda `antesDeId`; Ir a 3 recorre | `paginador-por-cursor.spec.ts` 5 + `…cursor.spec.ts` 5 + navegador | HECHO |
| H4.S6.M1 | Catálogos operativos | filtros Estado/Dueño; Ver → ficha | spec 6 + navegador | HECHO |
| H4.S6.M2 | Ficha de catálogo + Activar/Desactivar en `ap-dialogo` | PATCH `{activo, motivo}`; 403 conserva el motivo | `…estado.spec.ts` 2 + navegador | HECHO |
| H4.S7.M1 | Recorrido en navegador (aceptado/límite/error) + descargas | requests y archivos verificados | `node evidencia/franja-d/recorrido.mjs --pruebas` 36/36 | HECHO |
| H4.S7.M2 | Capturas 1280/768/375 claro + 1280 oscuro, miradas, sin desborde | 64/64 sin desborde horizontal | `recorrido.mjs --capturas` | HECHO |
| H4.S7.M3 | Comandos de cierre | todos en 0 | salida literal abajo | HECHO |

Microtarea agregada en el camino (descubrimiento visual, regla 20.6.6) dentro de H4.S7.M2: la tabla ensanchaba la página
(hasta 1357 px de desborde a 768) y la columna de acciones apilaba los botones. Causa demostrada en el navegador:
`ap-tabla-de-datos` es ítem de la grilla de `ap-listado`, cuyas pistas son `auto` y el ítem tiene `min-width: auto`
(tabla 1060 px dentro de un listado de 960 px). Contenido desde mis vistas con `grid-template-columns: minmax(0, 1fr)` en
`.pantalla` y en el host `ap-listado`, y acciones de fila sin envolver; después: 64/64 sin desborde.

## Evidencia (salidas literales recortadas)

```text
$ npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit; echo "tsc exit $?"
tsc exit 0
$ npx ng lint
Linting "backoffice"...
All files pass linting.
$ python3 ../../scripts/verificar_frontend.py backoffice
  OK    · ningún archivo de más de 200 líneas
  OK    · sin literal de diseño en @aportaya/ui
TODO OK
$ npx ng test --include='src/app/rutas/sistemas/**/*.spec.ts' --exclude='src/**/*.a11y.spec.ts'
 Test Files  29 passed (29)
      Tests  149 passed (149)
$ npx ng test --include='src/app/rutas/sistemas/**/*.a11y.spec.ts'
 Test Files  12 passed (12)
      Tests  18 passed (18)
$ node recorrido.mjs --pruebas
{"casos":36,"pass":36,"fail":0,"consola":4,"red4xx5xx":4}
$ node recorrido.mjs --capturas
{"casos":64,"pass":64,"fail":0,"consola":0,"red4xx5xx":0}
```

Los 4 errores de consola/red del recorrido son exactamente los cuatro inyectados a propósito (PATCH usuario 500, GET
usuarios 403, PATCH elemento 403, GET bitácora 500); no apareció ningún otro (`evidencia/franja-d/recorrido-pruebas.json`).

Índice de `evidencia/franja-d/`:
- `recorrido.mjs` (el script), `recorrido-salida.txt` (36 líneas PASS con el detalle de cada request), `recorrido-pruebas.json`, `recorrido-capturas.json`.
- `descarga-usuarios-busqueda.csv` (búsqueda «jose»: encabezado + 1 fila), `descarga-usuarios-suspendidos.json` (5 objetos), `descarga-bitacora-pagina.csv`.
- `capturas/` (68 PNG): `<vista>-1280|768|375|1280-oscuro.png` para 16 vistas, más `usuarios-error-suspender-1280`, `servicios-sin-fuente-1280`, `ficha-usuario-1280`, `ficha-catalogo-1280`.
- `triage-despliegues-timeout.txt`: un timeout de 110 s en una corrida conjunta con carga 36 sobre 4 CPU; aislado 9/9 PASS → `ENVIRONMENT`.

Muestras del recorrido (de `recorrido-salida.txt`):
- `Editar → PATCH {"departamento":"RIESGOS","cargo":"Jefa de riesgos","motivo":"Ascenso aprobado por RRHH"} → 200 (doble sin estado: no persiste)`
- `PUT {"roles":["RISK_ANALYST","codigo"],"motivo":"Rota a auditoría por seis meses"} → 200`
- `POST rolCodigo=codigo · Idempotency-Key presente → 201`
- `Suspender con 500: error visible con traza; foco vuelve a «Suspender a Nombre3 Prueba»`
- `Bitácora: 1ª: limite=25 · Siguiente: antesDeId=c-25 · Ir a 3: antesDeId=c-50`
- `403 al listar: «No se pudo cargar usuarios internos. No tenés acceso a esto. Código de seguimiento: traza-…» · reintento → 200`

Capturas miradas: hojas de contacto por vista de las cuatro variantes para usuarios, bitácora, ficha de usuario, ficha de
catálogo, despliegues y outbox; las diez restantes a 375 y a 1280 oscuro en hojas combinadas. Corregido tras mirarlas:
desborde de la tabla, acciones apiladas, ficha capturada cargando (espera por su `h1`), cifras de a una a 375 (ahora dos
columnas), pista larga en «Restauración vencida».

## No cubierto

- **Persistencia**: el simulado de 4010 no guarda; ninguna mutación se verificó persistida (UI → request → response sí;
  persistencia → recarga → UI no). Pendiente contra los servicios `identidad` y `auditoria` reales.
- Las 9 vistas de puerto no tienen contrato: se verificaron contra el doble del puerto (specs) y el modo demo (navegador);
  el estado «sin fuente» se vio en navegador solo en Servicios (el resto comparte el mismo puerto y se cubre en specs).
- Volúmenes grandes en navegador se sirvieron con `page.route` (doble declarado); en modo demo hay 2–4 filas por vista,
  así que la paginación de las vistas de puerto se verificó solo en specs.
- Lector de pantalla real no se usó; a11y = axe en 12 archivos + teclado (Escape y devolución de foco en un diálogo).
- Capturas a 375 de los diálogos (alta, roles) no se tomaron.

## Pedidos a otras franjas

- **H0 / `packages/ui`**:
  - `ap-listado` debería declarar `grid-template-columns: minmax(0, 1fr)` (o `min-width: 0` en sus hijos): hoy la tabla
    ensancha la página; lo contengo desde cada vista de Sistemas, pero el resto de las franjas lo va a sufrir igual.
  - `ap-estado-de-pantalla.error()` no desenvuelve `ResourceWrappedError.cause`: en toda la app se pierde el mensaje
    traducido y el código de seguimiento de los `resource()`/`httpResource()` (B6).
  - `ap-acciones-de-fila` proyecta el contenido antes de Ver/Editar: las acciones de dominio quedan delante de «Ver».
  - Modo tarjeta de `ap-tabla-de-datos` a 375: el título de la tabla parte palabra por palabra y un valor mono largo se corta.
- **Franja A**: el menú todavía no oculta «Sistemas» a quien no tiene rol PLATAFORMA/SEGURIDAD (F1 del plan); el recorrido
  usó un token con `rol: 'PLATAFORMA'`.

## Hallazgos fuera de alcance

- `despliegues/pantalla-despliegues.ts`: los interruptores sin dinero cambian solo en memoria (no hay contrato) y el
  solicitante es un correo fijo con un `TODO` preexistente (`SOLICITANTE_ACTUAL`). No se tocó.
- El `index.html` de desarrollo no trae `aportaya-demo`: en `ng serve` las 9 vistas de puerto muestran el error, no los
  datos de ejemplo, aunque la configuración `development` reemplace el proveedor.

## Desvíos del plan

- Se agregaron `compartido/pruebas.apoyo.ts` y `pruebas-http.apoyo.ts` (suites de tres niveles reutilizadas por las vistas)
  y `bitacora/paginador-por-cursor.ts` + `columnas-de-bitacora.ts` (límite de 200 líneas).
- Se agregó `compartido/vistas-de-puerto.a11y.spec.ts` para las seis vistas que no tenían a11y.
