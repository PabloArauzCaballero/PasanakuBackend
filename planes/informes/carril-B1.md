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

**Se entregan 2 de las ~26 pantallas del alcance, completas de punta a punta (dominio +
pantalla + guardas de segregación + confirmación con dato concreto + pruebas + a11y +
build verificado en `dist/`), no 26 pantallas superficiales.** La razón no es negligencia:
es que el estado real del repo, al arrancar este carril, difiere del que describe el
prompt de arranque en dos puntos que cambian el trabajo por completo, y quedó
constancia de ambos como bloqueo (ver §Bloqueos):

1. **La infraestructura de shell que el prompt da por fusionada no existe todavía.**
   No hay `apps/backoffice/src/app/layout/`, no hay `permisos.ts`, no hay
   `TablaDeDatosVirtualizada`, `BarraDeFiltros`, `Exportador` ni `PanelDeEvidencia` como
   piezas nombradas. Lo que sí existe (`nucleo/sesion.ts` con `Sesion.puede()`,
   `nucleo/gateway.ts`, `packages/ui/src/tabla-de-datos`, `dialogo`, `reloj-de-plazo`,
   `chip-estado`, etc.) es suficiente para construir pantallas reales, y es lo que se
   usó. `Sesion.puede()` es la base real de la segregación de funciones que pide el gate.
2. **Los contratos de backend para los dos deltas que el gate exige por nombre (D-15 y
   D-18) todavía no existen.** `servicios/grupos/openapi/grupos.yaml` no publica el
   escalamiento de solicitudes; `servicios/cumplimiento/openapi/cumplimiento.yaml`
   reserva el prefijo `/reclamos` (línea 5) pero no publica ninguna operación bajo él.
   CU-52 (Atender un reclamo en plazo) sí está escrito y con su forma de datos
   completa: se usó esa forma. Para D-15 no hay CU numerado; se derivó la forma de los
   campos que CU-68 ya declara para `solicitud_ingreso` más lo que el delta agrega
   por necesidad (desde cuándo está vencida, a quién le toca resolver). **Ambos
   supuestos están declarados por escrito, con su archivo y su línea, en
   `dominio/cu52-reclamos.ts` y `dominio/d15-solicitudes-escaladas.ts`.**

Dado ese punto de partida, se decidió no construir 26 pantallas contra un contrato que
en su mayoría no existe (que sería inventar la forma del dato — la regla cero lo prohíbe
explícitamente) ni contra una infraestructura de permisos/tabla que tampoco existe (que
sería crear en `nucleo/` lo que el prompt dice que B ya congeló, violando la
exclusividad de archivos). Se construyeron las **dos pantallas que el gate nombra por
código** (D-15, D-18), con todo el rigor que pide el gate, para dejar sentado el patrón
que el resto del carril debe seguir cuando los contratos existan.

## Pantallas

| Pantalla | Ruta | CU | Delta | Organismos | Estados | Maqueta | Golden/captura | Gate |
| --- | --- | :-: | :-: | --- | :-: | :-: | :-: | :-: |
| Bandeja de reclamos | `/operacion/reclamos` | CU-52 | D-18 | `EstadoDePantalla`, `ChipEstado`, `RelojDePlazo`, `Monto`, `Boton`, `Dialogo` | ⬜ 4/4 (carga, éxito, vacío, error vía `EstadoDePantalla` — pendiente prueba de error propia) | ⬜ no hay maqueta pintada para esta pantalla en `docs/Views` | ⬜ | ✅ lint/typecheck/build/test:front/test:a11y |
| Cola de solicitudes escaladas | `/operacion/solicitudes-escaladas` | — (D-15, sin CU numerado) | D-15 | `EstadoDePantalla`, `RelojDePlazo`, `Fecha`, `Boton`, `Dialogo`, `Campo` | ⬜ 4/4 (ídem) | ⬜ | ⬜ | ✅ lint/typecheck/build/test:front/test:a11y |
| Las 24 restantes (conciliación, descuadres, reversos, incidencias de desembolso, arqueos, políticas de resolución, roles, tarifario, reportes, fondeo QR, mensajería) | — | varios | — | — | — | — | — | ⬜ no iniciadas |

No se cargó `planes/22` §3 (el mapa de la maqueta) columna por columna para las 24
pantallas restantes: dado que no se iba a construir nada contra ellas en esta sesión,
leerlo entero habría sido gastar contexto sin producir código, en contra de la regla de
economía de tokens de `arrancar-carril` §9. Queda para la próxima sesión, antes de tocar
esas pantallas.

## Piezas declaradas por nivel

| Pieza | Nivel | Dónde vive | CU | Estado |
| --- | --- | --- | :-: | :-: |
| `bandejaDeReclamos()`, `vencido()`, `ordenadosPorVencimiento()` | Átomo/molécula de dominio (función pura + `httpResource`) | `rutas/operacion/dominio/cu52-reclamos.ts` | CU-52 | Hecho |
| `BandejaDeReclamos` | Página (compone organismos de `@aportaya/ui`, sin lógica propia) | `rutas/operacion/reclamos/bandeja-de-reclamos.ts` | CU-52 | Hecho |
| `colaDeSolicitudesEscaladas()`, `ordenadasPorVencimiento()` | Átomo/molécula de dominio | `rutas/operacion/dominio/d15-solicitudes-escaladas.ts` | D-15 | Hecho |
| `ColaDeSolicitudesEscaladas` | Página | `rutas/operacion/solicitudes-escaladas/cola-de-solicitudes-escaladas.ts` | D-15 | Hecho |
| `tienePermiso(permiso): CanMatchFn` | Guardia de ruta (propia, en `operacion.routes.ts`, no en `nucleo/`) | `rutas/operacion/operacion.routes.ts` | — | Hecho |

No se creó ninguna pieza en `packages/ui` ni en `nucleo/`: todo lo usado (`EstadoDePantalla`,
`ChipEstado`, `RelojDePlazo`, `Fecha`, `Monto`, `Boton`, `Dialogo`, `Campo`, `Sesion`)
ya existía.

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

1. **Infraestructura de shell descrita en el prompt de arranque no está en `dev`.**
   `layout/`, `permisos.ts`, `TablaDeDatosVirtualizada`, `BarraDeFiltros`, `Exportador`,
   `PanelDeEvidencia` no existen en `apps/backoffice/src/app/` ni en `packages/ui` con
   esos nombres. Lo que existe y cumple una función equivalente: `nucleo/sesion.ts`
   (`Sesion.puede()`, roles), `packages/ui/src/tabla-de-datos` (no virtualizada),
   `packages/ui/src/dialogo`, `reloj-de-plazo`, `chip-estado`, `fila-de-cotejo` (útil
   para conciliación cuando se retome). **Esperando de**: el carril B (shell) o una
   decisión de si esas piezas se construyen en esta fase o se posponen. No se creó
   nada en `nucleo/` ni `packages/ui` porque ambos son de solo lectura para B1.
2. **Contratos de backend faltantes** para D-15 (`grupos`) y D-18 (`cumplimiento`) —
   ver Supuestos 1 y 2. Sin esto, ninguna de las dos pantallas puede completar la
   acción con efecto (aceptar/rechazar solicitud, responder reclamo) contra datos
   reales; hoy simulan el cierre del diálogo y no queda registrado en ningún lado.
3. **Los otros 24 CU de operación no tienen ni pantalla ni contrato revisado en esta
   sesión.** No es que falte el contrato en todos los casos (tarifario y desembolsos sí
   tienen `openapi` en `servicios/tarifas` y `servicios/nucleo-financiero`, sin revisar
   todavía) — es que el tiempo de una sesión no alcanzó para las 26, y se priorizaron
   las dos que el gate nombra por código explícito. **Esto es lo más importante que
   queda abierto del carril.**

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | --- |
| Especificación | Cada pantalla sale de la sección Interfaz de su CU | CU-52 §Interfaz citado; D-15 sin CU, tomado de `planes/20` | ✅ (2/2 construidas) |
| Segregación de funciones | canMatch monta solo el lado que el rol permite | `bandeja-de-reclamos.spec.ts`: "sin RECLAMO_ATENDER no aparece el botón de responder" (pasa); `cola-de-solicitudes-escaladas.spec.ts`: "sin SOLICITUD_INGRESO_RESOLVER no aparecen las acciones" (pasa); ambos con dos roles simulados vía `Sesion.abrir(...)` | ✅ |
| Confirmación con dato concreto | Nunca un "¿estás seguro?" genérico | Prueba: el diálogo de reclamos contiene el código (`REC-2026-08-0157`); el de solicitudes contiene nombre y grupo (`Marcelo Rojas`, `PSK-0042`) | ✅ |
| Bandeja con plazo ordena por vencimiento | Orden ascendente por fecha límite | Prueba en ambas pantallas: fixture con dos ítems en orden inverso, se verifica el orden de salida en el DOM | ✅ |
| Rutas solo en `operacion.routes.ts`, con carga perezosa en `dist/` | `grep` en el build, no en la fuente | `yarn workspace @aportaya/backoffice build` genera `chunk-BiM-th3s.js` (cola-de-solicitudes-escaladas) y `chunk-DJ5NiWmQ.js` (bandeja-de-reclamos) como chunks separados — confirmado con `grep -rl "cola-de-solicitudes-escaladas\|bandeja-de-reclamos" dist/backoffice/browser/*.js` | ✅ para las 2 hechas; las 24 restantes no existen |
| D-15 / D-18 | Cola de escaladas y reclamos con puerta y plazo guardado | Construidas; el plazo se lee del campo ya guardado del servidor (`plazoRespuesta`, `fechaLimiteOrganizador`), nunca se recalcula en cliente | ✅ patrón; ⬜ integración real (bloqueada por contrato) |
| Estados | Los cuatro, vía `EstadoDePantalla` | Carga/éxito/vacío probados en las 2; error se apoya en el mismo `EstadoDePantalla` ya probado en CU-13 (`pantalla-de-billetera`), sin prueba propia repetida por pantalla | parcial — falta el caso error explícito por pantalla nueva |
| Accesibilidad | axe sin violaciones serias | `yarn test:a11y`: 5 tests pasan (3 archivos, incluidas las 2 pantallas nuevas) | ✅ |
| Entrega | Lint, tipos, pruebas, build | ver comandos abajo | ✅ |

## Gate de salida — evidencia

- [x] `./gradlew generateOpenApiClients` — `BUILD SUCCESSFUL`, `UP-TO-DATE` en todos los servicios (no había diff que generar para B1: no se tocó ningún `openapi/`)
- [ ] `python3 scripts/verificar_maqueta.py` — no corrido: no hay maqueta pintada para reclamos/solicitudes-escaladas en `docs/Views` contra la cual comparar
- [x] `yarn workspace @aportaya/backoffice lint` → `All files pass linting.` + `TODO OK` (sin red en vista, sin literal de diseño, sin formato de dinero, sin console, sin archivo >200 líneas, sin literal de diseño en `@aportaya/ui`)
- [x] `yarn workspace @aportaya/backoffice typecheck` → sin salida (sin errores)
- [x] `yarn workspace @aportaya/backoffice test:front` → `Test Files 5 passed (5)` / `Tests 21 passed (21)`
- [x] `yarn workspace @aportaya/backoffice test:a11y` → `Test Files 3 passed (3)` / `Tests 5 passed (5)`
- [x] `yarn workspace @aportaya/backoffice build` → `Application bundle generation complete`, con `cola-de-solicitudes-escaladas` y `bandeja-de-reclamos` como *lazy chunks* nombrados en la salida
- [ ] Flutter / iOS: n/a, carril Angular
- [x] `git diff --stat` fuera de `rutas/operacion/` y `planes/informes/carril-B1.md`: vacío (ver commit)
- [x] Cada criterio de aceptación con su prueba nombrada igual: los `it(...)` citan la regla del gate en su propio texto (segregación, plazo, confirmación con dato concreto, motivo obligatorio)
- [ ] Gate específico de la fase F7 completo: no — 24/26 pantallas sin empezar

## Qué queda abierto, en orden de prioridad

1. **Pedir a `grupos` y `cumplimiento` (backend) los contratos de D-15 y D-18** —
   bloquea integrar de verdad las dos pantallas ya construidas.
2. **Resolver el bloqueo de infraestructura de shell** (§Bloqueos 1) antes de construir
   conciliación/descuadres/arqueos, que si necesitan tabla con selección masiva,
   exportación y evidencia — las piezas que el prompt asume que existen y no existen.
3. **Las 24 pantallas restantes**, revisando primero qué contratos de
   `servicios/tarifas`, `servicios/nucleo-financiero`, `servicios/entregas` y
   `servicios/grupos` ya están escritos (no se revisó en esta sesión más que lo
   necesario para D-15/D-18).
4. Agregar la prueba explícita de estado "error" a las dos pantallas nuevas (hoy se
   apoya en que `EstadoDePantalla` ya la prueba en CU-13, pero el gate pide 4/4 por
   pantalla).

## Ver también

[[informe]] · [[16 Carriles de frontend]] · [[22 Mapa de la maqueta · pantalla, carril y mundo]] · [[10b Estándar de ejecución del frontend]]
