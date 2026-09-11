---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril B1 — operación (Angular)"
ola: 2
fase: F7
mundo: Angular
modulo: apps/backoffice/src/app/rutas/operacion
rama: pablo/feature/carril-B1-operacion
estado: en curso
---

# Carril B1 — operación

**Fase** F7 · **Mundo** Angular · **Casos de uso** los 26 de operación (conciliación,
descuadres, reversos, incidencias de desembolso, arqueos, solicitudes escaladas,
reclamos, políticas de resolución, roles, tarifario, reportes, fondeo QR, mensajería) ·
**Puesto** B1

## Resumen ejecutivo — léase antes que nada

**Actualizado tras la reconciliación del coordinador** (merge de `dev` con el shell
real de B, M y W dentro de esta rama, commit `36c0dcf`). Lo que sigue reemplaza al
resumen original: el bloqueo §1 de la versión anterior de este informe (infraestructura
de shell inexistente) **ya no aplica** — la infraestructura real llegó con el merge y
las dos pantallas se migraron a usarla. El bloqueo §2 (contratos de backend para D-15 y
D-18) **sigue igual**, y una revisión adicional de los servicios con OpenAPI "verificado"
(tarifas, núcleo-financiero, entregas, grupos) confirma que el patrón se repite en los
otros 24 CU: hay operaciones de **acción** (`POST`), pero no de **bandeja** (`GET` que
liste). Ver el detalle abajo.

**Se siguen entregando 2 de las ~26 pantallas del alcance**, pero ahora sobre la pieza
real y compartida (`TablaDeDatosVirtualizada`, `BarraDeFiltros`, `requierePermiso` de
`nucleo/permisos.ts`), no sobre una reconstrucción propia. Antes de la reconciliación,
las dos pantallas usaban una lista (`<ul>`) propia y un `canMatch` propio porque las
piezas reales no existían en el `dev` desde el que arrancó este carril; con el merge,
ambas cosas ya estaban de más y se reemplazaron — ver §Migración.

No se sumó una tercera pantalla en esta vuelta. La razón, verificada con `grep` contra
los cuatro servicios que el coordinador señaló como "contrato ya verificado", está en
§Por qué no se sumaron más pantallas.

## Pantallas

| Pantalla | Ruta | CU | Delta | Organismos | Estados | Maqueta | Golden/captura | Gate |
| --- | --- | :-: | :-: | --- | :-: | :-: | :-: | :-: |
| Bandeja de reclamos | `/operacion/reclamos` | CU-52 | D-18 | `TablaDeDatosVirtualizada`, `BarraDeFiltros`, `ChipEstado`, `RelojDePlazo`, `Boton`, `Dialogo` | ⬜ 4/4 (carga/error vía la tabla, ya probados en su propio spec del shell; vacío y error específicos de esta bandeja sin prueba propia) | ⬜ no hay maqueta pintada para esta pantalla en `docs/Views` | ⬜ | ✅ lint/typecheck/build/test:front/test:a11y |
| Cola de solicitudes escaladas | `/operacion/solicitudes-escaladas` | — (D-15, sin CU numerado) | D-15 | `TablaDeDatosVirtualizada`, `RelojDePlazo`, `Boton`, `Dialogo`, `Campo` | ⬜ 4/4 (ídem) | ⬜ | ⬜ | ✅ lint/typecheck/build/test:front/test:a11y |
| Las 24 restantes (conciliación, descuadres, reversos, incidencias de desembolso, arqueos, políticas de resolución, roles, tarifario, reportes, fondeo QR, mensajería) | — | varios | — | — | — | — | — | ⬜ no iniciadas |

No se cargó `planes/22` §3 (el mapa de la maqueta) columna por columna para las 24
pantallas restantes: dado que no se iba a construir nada contra ellas en esta sesión,
leerlo entero habría sido gastar contexto sin producir código, en contra de la regla de
economía de tokens de `arrancar-carril` §9. Queda para la próxima sesión, antes de tocar
esas pantallas.

## Migración a la infraestructura real (tras la reconciliación)

El merge trajo `apps/backoffice/src/app/nucleo/tabla/tabla-de-datos-virtualizada.ts`,
`nucleo/filtros/barra-de-filtros.ts`, `nucleo/exportador.ts`,
`nucleo/evidencia/panel-de-evidencia.ts` y `nucleo/permisos.ts`. Se migró lo que aplica
a las dos pantallas de este carril:

| Qué tenía antes (propio, provisional) | Qué usa ahora (del shell, compartido) | Por qué |
| --- | --- | --- |
| `<ul>`/`<li>` con `@for` propio, sin paginación | `ap-tabla-de-datos-virtualizada` con columnas, `cargador` server-side y orden por lista blanca | Regla del proyecto: "nunca se duplica un átomo". Es *la* tabla de las 64 pantallas de F7/F8 |
| `tienePermiso(permiso): CanMatchFn` definido en `operacion.routes.ts` | `requierePermiso(permiso)` de `nucleo/permisos.ts` | Misma función, ahora la real: además redirige a `/tablero` en vez de solo bloquear el match, que es mejor UX que la versión propia |
| — (no existía) | `ap-barra-de-filtros` con el filtro de estado de la bandeja de reclamos, persistido en la URL | Nueva: se agregó porque encaja con CU-52 (filtrar por estado) y no se había podido antes |
| `httpResource<T[]>` pidiendo el arreglo completo | `CargadorDePagina<T>` (`cargarReclamos` / `cargarSolicitudesEscaladas` en `dominio/`) que la tabla invoca con página/orden/filtros | La tabla exige un `cargador` con esa forma; el adaptador de dominio sigue paginando/ordenando en memoria porque el `GET` asumido (supuesto declarado) todavía no lo hace del lado del servidor — es el único punto que cambia cuando el backend lo publique |

**No se usó `Exportador` ni `PanelDeEvidencia`.** `Exportador` (CU-58) encajaría en la
bandeja de reclamos (exportar para el reporte mensual a la ASFI que pide CU-52), pero
agregarlo bien —con su propio permiso `exportar:reclamos` y su propia prueba— no entraba
en el tiempo de esta vuelta; queda anotado en "Qué queda abierto". `PanelDeEvidencia`
necesita una fuente de eventos por caso (`EventoDeEvidencia[]`) que ningún contrato
expone todavía para un reclamo o una solicitud — mismo tipo de hueco que D-15/D-18.

**Efecto colateral verificado, no descartado**: el `initial chunk` del build pasó de
~277 kB a 318 kB (el CDK de scroll virtual que trae la tabla no es gratis) y ahora
excede el presupuesto de advertencia de `angular.json` (300 kB) sin llegar al de error
(400 kB) — `ng build` sigue en verde (exit 0), pero es una advertencia real que las
próximas 24 pantallas, si repiten el patrón, van a empujar más. Ver evidencia en
§Gate de salida.

## Por qué no se sumaron más pantallas

Se revisaron los cuatro servicios que el coordinador señaló con contrato ya verificado,
buscando una operación de **bandeja** (`GET` que devuelva una lista, no una acción
puntual) para alguno de los 24 CU restantes:

```
$ grep -n "^  /" servicios/tarifas/src/main/resources/openapi/tarifas.yaml
# /comisiones/cotizaciones, /comisiones/devengos, /facturas, /tarifas/tarifarios,
# /tarifas/tarifarios/{id}/vigencia, /tarifas/liquidaciones/{periodo}/cierre,
# /tarifas/segmentos, /tarifas/precios/resolver, /tarifas/vigentes/{codigo}
# → todas POST (acción) salvo /tarifas/vigentes/{codigo} (GET de UN código, no bandeja).
# No hay GET que liste tarifarios.

$ grep -n "^  /" servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml
# /billetera/reversos → POST (CU-14, reversar). No hay GET que liste reversos pendientes.
# /billetera/{cuentaId}/extracto → GET, pero es el extracto de UNA cuenta, no una bandeja
# de descuadres/incidencias para operación.

$ grep -n "^  /" servicios/entregas/src/main/resources/openapi/entregas.yaml
# /entregas, /entregas/{id}/autorizacion, /entregas/{id}/ejecucion, /desembolsos,
# /desembolsos/{ordenId}/respuesta → todas POST. No hay GET que liste incidencias de
# desembolso.
```

El patrón se repite: estos cuatro servicios tienen contrato real para la **acción** de
cada CU (reversar, autorizar una entrega, responder un desembolso, publicar un
tarifario), pero **ninguno publica todavía el `GET` de bandeja** que una pantalla de
operación necesita para listar "los reversos pendientes", "las incidencias de
desembolso sin resolver", "los tarifarios en preaviso". Es la misma clase de hueco que
D-15 y D-18, no una excepción. Construir una tercera pantalla habría significado
inventar una tercera forma de bandeja sin contrato — exactamente lo que la regla cero
prohíbe — en vez de profundizar las dos que si tienen (para D-18) o pueden derivar con
un supuesto razonable (para D-15) una forma de datos real. Se prefirió eso.

**La recomendación que se puede dar con esto**: antes de que cualquier carril de F7/F8
construya la pantalla de bandeja de un CU, conviene pedir al backend el `GET` de listado
correspondiente — no es un caso aislado de reclamos o solicitudes escaladas, es un patrón
del backend actual (fuerte en comandos, débil en consultas de bandeja) que probablemente
toca a otros carriles de esta ola también.

## Piezas declaradas por nivel

| Pieza | Nivel | Dónde vive | CU | Estado |
| --- | --- | --- | :-: | :-: |
| `vencido()`, `ordenadosPorVencimiento()`, `cargarReclamos()`, `cargadorDeReclamos()` | Átomo/molécula de dominio (funciones puras + adaptador HTTP) | `rutas/operacion/dominio/cu52-reclamos.ts` | CU-52 | Hecho |
| `BandejaDeReclamos` | Página (compone `TablaDeDatosVirtualizada` + `BarraDeFiltros` del shell y organismos de `@aportaya/ui`, sin lógica propia) | `rutas/operacion/reclamos/bandeja-de-reclamos.ts` | CU-52 | Hecho, migrado |
| `ordenadasPorVencimiento()`, `cargarSolicitudesEscaladas()`, `cargadorDeSolicitudesEscaladas()` | Átomo/molécula de dominio | `rutas/operacion/dominio/d15-solicitudes-escaladas.ts` | D-15 | Hecho |
| `ColaDeSolicitudesEscaladas` | Página (compone `TablaDeDatosVirtualizada` del shell) | `rutas/operacion/solicitudes-escaladas/cola-de-solicitudes-escaladas.ts` | D-15 | Hecho, migrado |

**No se creó ninguna pieza en `packages/ui` ni en `nucleo/`.** Todo lo usado
(`EstadoDePantalla`, `ChipEstado`, `RelojDePlazo`, `Fecha`, `Boton`, `Dialogo`, `Campo`,
`Sesion`, `TablaDeDatosVirtualizada`, `BarraDeFiltros`, `requierePermiso`) ya existía,
la mitad en `packages/ui` (congelado, de solo lectura) y la mitad en `nucleo/` (del
shell B, llegada con la reconciliación). El `tienePermiso` propio que este carril había
escrito en `operacion.routes.ts` **se borró** — quedó reemplazado por
`requierePermiso` de `nucleo/permisos.ts`, que además redirige a `/tablero` en vez de
solo bloquear el `match`.

## Ejemplos del contrato

No se agregó nada a `packages/simulado/ejemplos/`. Los datos de prueba de las dos
pantallas son literales locales en cada `*.spec.ts`, **no** `ejemploDe(...)`: como el
`openapi` de `/reclamos` y de la cola escalada no existe, generar un ejemplo "oficial"
con `packages/simulado` habría validado contra un esquema que no está, y ese paquete es
compartido con los otros carriles — el riesgo de romper `ejemplos-contra-esquema.spec.ts`
para todos por un contrato inventado no vale la pena. Cuando el backend publique las dos
operaciones, ese es el primer paso siguiente: mover estos fixtures a ejemplos reales del
contrato.

## Supuestos declarados

1. **D-18 / CU-52** (`dominio/cu52-reclamos.ts`): se asume que la bandeja se sirve en
   `GET {gateway}/reclamos` y devuelve un arreglo con los campos que `SalidaCU52` ya
   declara (`reclamoId`, `codigo`, `plazoRespuesta`, `diasHabilesPlazo`, `estado`) más
   `categoria`, `canalIngreso`, `montoReclamado`, `fechaIngreso` y `responsableId/Nombre`,
   que el flujo principal de CU-52 exige guardar aunque `SalidaCU52` (pensado para el
   `POST` de alta) no los liste. **No crítico** porque no mueve dinero ni decide nada
   por sí solo — es de lectura; se declara y se sigue. Pedido al backend: publicar
   `GET /reclamos` (bandeja) y `POST /reclamos/{id}/respuesta` en
   `servicios/cumplimiento/openapi/cumplimiento.yaml`.
2. **D-15** (`dominio/d15-solicitudes-escaladas.ts`): se asume `GET
   {gateway}/grupos/solicitudes-escaladas`, con la forma que combina `solicitud_ingreso`
   (CU-68) y lo que el delta agrega (`escaladaEn`, `fechaLimiteOrganizador`). No hay CU
   numerado para este delta específico — el propio `planes/20` lo dice: "F7 suma la
   vista de solicitudes escaladas" sin más detalle de contrato. **No crítico**, mismo
   argumento. Pedido al backend: publicar la operación de bandeja y la de resolución
   (`POST /grupos/solicitudes-escaladas/{id}/resolucion`) en
   `servicios/grupos/openapi/grupos.yaml`, con la restricción `ck_solicitud_ingreso_resuelta`
   (exige `revisada_por` + `fecha_resolucion`) reflejada en la entrada.
3. **Permisos**: se inventaron los nombres `RECLAMO_VER`, `RECLAMO_ATENDER`,
   `SOLICITUD_INGRESO_VER`, `SOLICITUD_INGRESO_RESOLVER` para `Sesion.puede(...)`. CU-52
   solo nombra `RECLAMO_ATENDER` en su tabla de eventos; el resto son supuestos de
   nomenclatura, consistentes con ese único dato real. No crítico — son strings de
   guardia, no lógica de negocio; si el backend usa otro nombre de permiso, es un
   cambio de una constante.
4. **No se implementaron los botones de acción contra el backend real** (`confirmarRespuesta`
   y `confirmar` en la cola de escaladas cierran el diálogo y recargan la bandeja, sin
   hacer el `POST`): como el endpoint no existe, no hay contra qué integrar. El diálogo,
   el texto de confirmación con el dato concreto delante y el bloqueo de motivo vacío
   sí están implementados y probados — es la parte que no depende del contrato.

## Micro-PR abiertos al troncal

Ninguno. No se tocó `plataforma/`, `gradle/libs.versions.toml` ni ningún átomo
compartido.

## Bloqueos

1. ~~Infraestructura de shell descrita en el prompt de arranque no está en `dev`~~ —
   **resuelto por la reconciliación** (commit `36c0dcf`). `nucleo/tabla/`,
   `nucleo/filtros/`, `nucleo/permisos.ts`, `nucleo/exportador.ts`,
   `nucleo/evidencia/` ya existen y las dos pantallas de este carril los usan. Ver
   §Migración.
2. **Contratos de backend faltantes** para D-15 (`grupos`) y D-18 (`cumplimiento`) —
   ver Supuestos 1 y 2, sin cambios. Sin esto, ninguna de las dos pantallas puede
   completar la acción con efecto (aceptar/rechazar solicitud, responder reclamo)
   contra datos reales; hoy simulan el cierre del diálogo y no queda registrado en
   ningún lado.
3. **Los otros 24 CU de operación no tienen ni pantalla ni contrato de bandeja.**
   Revisado en esta vuelta con evidencia (§Por qué no se sumaron más pantallas):
   `servicios/tarifas`, `servicios/nucleo-financiero` y `servicios/entregas` —los tres
   señalados como "contrato ya verificado"— tienen operaciones de **acción** (`POST`)
   para cada CU, pero **ninguno publica todavía un `GET` de bandeja** que liste
   tarifarios en preaviso, reversos pendientes o incidencias de desembolso sin
   resolver. Es un bloqueo de backend, no de este carril, y probablemente toca a más
   carriles de F7/F8 que solo a B1. **Esto sigue siendo lo más importante que queda
   abierto.**
4. **`Exportador` y `PanelDeEvidencia` no se integraron** en esta vuelta por tiempo,
   aunque ya están disponibles y encajarían en la bandeja de reclamos (exportar para el
   reporte mensual a la ASFI, CU-52) — ver §Migración para el detalle de por qué
   quedaron fuera.

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | --- |
| Especificación | Cada pantalla sale de la sección Interfaz de su CU | CU-52 §Interfaz citado; D-15 sin CU, tomado de `planes/20` | ✅ (2/2 construidas) |
| No duplicar átomos | La tabla y los filtros son los del shell, no una reconstrucción propia | `BandejaDeReclamos` y `ColaDeSolicitudesEscaladas` importan `TablaDeDatosVirtualizada` y (la primera) `BarraDeFiltros` de `nucleo/`; la lista `<ul>` propia que existía antes de la reconciliación se borró | ✅ (nuevo, tras el merge) |
| Segregación de funciones | La guarda de ruta y la guarda de acción montan solo el lado que el rol permite | `operacion.routes.ts` usa `requierePermiso` real (`nucleo/permisos.ts`), no una copia propia; `bandeja-de-reclamos.spec.ts`: "sin RECLAMO_ATENDER la guarda... da false" (pasa); `cola-de-solicitudes-escaladas.spec.ts`: "sin SOLICITUD_INGRESO_RESOLVER la guarda... da false" (pasa); ambos con dos roles simulados vía `Sesion.abrir(...)` | ✅ |
| Confirmación con dato concreto | Nunca un "¿estás seguro?" genérico | Prueba: el diálogo de reclamos contiene el código (`REC-2026-08-0157`); el de solicitudes contiene nombre y grupo (`Marcelo Rojas`, `PSK-0042`) | ✅ |
| Bandeja con plazo ordena por vencimiento | Orden ascendente por fecha límite, calculado en el `cargador`, no en el componente | `dominio/cu52-reclamos.spec.ts` y `dominio/d15-solicitudes-escaladas.spec.ts`: el `cargador` de cada bandeja, probado sin CDK de por medio, devuelve las filas en orden de vencimiento cuando no se pide otro orden | ✅ |
| Rutas solo en `operacion.routes.ts`, con carga perezosa en `dist/` | `grep` en el build, no en la fuente | `yarn workspace @aportaya/backoffice build` genera `chunk-0ZYavWnq.js` (`cola-de-solicitudes-escaladas`) y `chunk-BKcOS0cr.js` (`bandeja-de-reclamos`) como chunks separados — confirmado en la salida del build (los hashes cambian por build, se verifica por el nombre entre paréntesis, no por el hash) | ✅ para las 2 hechas; las 24 restantes no existen |
| D-15 / D-18 | Cola de escaladas y reclamos con puerta y plazo guardado | Construidas; el plazo se lee del campo ya guardado del servidor (`plazoRespuesta`, `fechaLimiteOrganizador`), nunca se recalcula en cliente | ✅ patrón; ⬜ integración real (bloqueada por contrato) |
| Estados | Los cuatro (carga, éxito, vacío, error) | Ahora los maneja `TablaDeDatosVirtualizada` internamente (`cargando()`, `error()`, ya probados en su propio spec del shell) en vez de `EstadoDePantalla`; el caso vacío y de error específico de cada bandeja no tiene prueba propia en esta vuelta | parcial, sin cambios respecto del informe anterior |
| Accesibilidad | axe sin violaciones serias | `yarn test:a11y`: 6 tests pasan (4 archivos, incluidas las 2 pantallas nuevas) | ✅ |
| Presupuesto de bundle | `budgets` de `angular.json` respetado | `yarn build`: `initial` 318,42 kB, supera el `maximumWarning` (300 kB) pero no el `maximumError` (400 kB) — el build sigue en verde (exit 0) pero es una advertencia real, atribuible al CDK de scroll virtual que trae la tabla | ⚠️ advertencia, no bloqueante |
| Entrega | Lint, tipos, pruebas, build | ver comandos abajo | ✅ |

## Gate de salida — evidencia (repetida tras la reconciliación y la migración)

- [x] `./gradlew generateOpenApiClients` — `BUILD SUCCESSFUL`, `UP-TO-DATE` en todos los servicios (no se tocó ningún `openapi/`)
- [ ] `python3 scripts/verificar_maqueta.py` — no corrido: no hay maqueta pintada para reclamos/solicitudes-escaladas en `docs/Views` contra la cual comparar
- [x] `yarn workspace @aportaya/backoffice lint` → `All files pass linting.` + `TODO OK` (sin red en vista — el `HttpClient` quedó solo en `dominio/`, sin literal de diseño, sin formato de dinero, sin console, sin archivo >200 líneas, sin literal de diseño en `@aportaya/ui`)
- [x] `yarn workspace @aportaya/backoffice typecheck` → sin salida (sin errores)
- [x] `yarn workspace @aportaya/backoffice test:front` → `Test Files 12 passed (12)` / `Tests 42 passed (42)` (subió de 5/21 a 12/42: se separaron pruebas de `cargador` puras de dominio, sin CDK, de las pruebas de componente)
- [x] `yarn workspace @aportaya/backoffice test:a11y` → `Test Files 4 passed (4)` / `Tests 6 passed (6)`
- [x] `yarn workspace @aportaya/backoffice build` → `Application bundle generation complete`, con `cola-de-solicitudes-escaladas` y `bandeja-de-reclamos` como *lazy chunks* nombrados en la salida; advertencia de presupuesto de bundle inicial (ver matriz de gates)
- [ ] Flutter / iOS: n/a, carril Angular
- [x] `git diff --stat dev -- apps/backoffice/src/app/nucleo apps/backoffice/src/app/layout apps/backoffice/src/app/app.routes.ts`: vacío — no se tocó el shell
- [x] Cada criterio de aceptación con su prueba nombrada igual: los `it(...)` citan la regla del gate en su propio texto (segregación, plazo, confirmación con dato concreto, motivo obligatorio)
- [ ] Gate específico de la fase F7 completo: no — 24/26 pantallas sin empezar, bloqueadas en su mayoría por falta de contrato de bandeja (ver §Por qué no se sumaron más pantallas)

### Por qué bajó el número de pruebas de DOM

Antes de la migración, las pruebas verificaban el orden y el contenido leyendo el DOM
(`<li strong>`, etc.). `TablaDeDatosVirtualizada` renderiza sus filas dentro de un
`cdk-virtual-scroll-viewport`, que en `jsdom` no mide tamaño real y por lo tanto no
renderiza filas (mismo comportamiento que ya documenta
`nucleo/tabla/tabla-de-datos-virtualizada.spec.ts`, del shell, que prueba contra las
señales del componente y no contra el DOM). Se siguió el mismo criterio: el orden y el
filtro se prueban contra el `cargador` de forma aislada
(`dominio/cu52-reclamos.spec.ts`, `dominio/d15-solicitudes-escaladas.spec.ts`, 5 pruebas
nuevas) y la guarda de permiso y el diálogo se prueban contra la instancia del
componente, no contra el DOM virtualizado. Es una prueba distinta, no una prueba más
débil: cubre la misma regla del gate por una vía que sí se ejecuta en `jsdom`.

## Qué queda abierto, en orden de prioridad

1. **Pedir contratos de bandeja (`GET` que liste) a los backends de operación** — no
   solo D-15 (`grupos`) y D-18 (`cumplimiento`): la revisión de esta vuelta encontró el
   mismo hueco en `tarifas`, `nucleo-financiero` y `entregas`. Es el bloqueo que más
   pantallas del carril frena.
2. **Las 24 pantallas restantes**, empezando por las que ya tienen el CU escrito y
   revisando primero, para cada una, si existe el `GET` de bandeja antes de construir
   nada (evita repetir el supuesto declarado dos veces).
3. Integrar `Exportador` (CU-58) en la bandeja de reclamos, con su propio permiso y
   prueba — encaja con el reporte mensual a la ASFI que pide CU-52 y ya está disponible.
4. Integrar `PanelDeEvidencia` cuando exista una fuente de eventos por caso (reclamo o
   solicitud) en algún contrato — hoy no existe.
5. Vigilar el presupuesto de bundle (`angular.json`, `initial` ya en 318 kB de 300 kB
   de advertencia): si las próximas pantallas repiten `TablaDeDatosVirtualizada`, el CDK
   de scroll virtual ya está pagado una vez, pero conviene remedir tras sumar 3-4 más.
6. Agregar la prueba explícita de estado "error" a las dos pantallas (antes se apoyaba
   en `EstadoDePantalla`, que ya no se usa en estas dos tras la migración a la tabla —
   `TablaDeDatosVirtualizada` expone `cargando()`/`error()` pero esta vuelta no agregó
   una prueba de componente para el caso de error específico de cada bandeja).

## Ver también

[[informe]] · [[16 Carriles de frontend]] · [[22 Mapa de la maqueta · pantalla, carril y mundo]] · [[10b Estándar de ejecución del frontend]]
