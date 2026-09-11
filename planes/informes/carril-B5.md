---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril B5 — sistemas (Angular)"
ola: F3
fase: F8.D
mundo: Angular
modulo: apps/backoffice/src/app/rutas/sistemas
rama: pablo/feature/carril-B5-sistemas
estado: en curso
---

# Carril B5 — sistemas (backoffice)

**Fase** F8.D · **Mundo** Angular · **Producto** backoffice de sistemas (aparte del
financiero) · **Rama** `pablo/feature/carril-B5-sistemas`

## Desvío frente al contrato del carril, declarado de entrada

El prompt de arranque describe una `dev` con `layout/` (incluido `layout/sistemas/`
vacío), `TablaDeDatosVirtualizada` sobre CDK, `BarraDeFiltros`, `Exportador`,
`PanelDeEvidencia`, `ServicioSesion` y `permisos.ts`. **Ninguna de esas piezas existe en
`origin/dev` real** (verificado con `find apps/backoffice/src/app -maxdepth 3` tras
`git checkout -b pablo/feature/carril-B5-sistemas origin/dev`). Lo que sí existe:

- `apps/backoffice/src/app/rutas/{operacion,cumplimiento,sistemas,contabilidad,publicidad,tablero}`,
  cada uno con su `<dominio>.routes.ts` — `sistemas.routes.ts` estaba vacío, igual que
  describe la ficha, solo que **no hay `layout/`** en absoluto.
- `nucleo/` con `sesion.ts` (clase `Sesion`, no `ServicioSesion`), `gateway.ts`,
  `errores.ts` + `errores.interceptor.ts`, `traza.interceptor.ts`, `idempotencia.
  interceptor.ts`.
- `packages/ui` con `tabla-de-datos` (sin CDK ni virtualización), `interruptor`,
  `chip-estado`, `alerta`, `dialogo`, `campo`, `tarjeta-kpi`, `estado-de-pantalla`,
  `estado-vacio`, etc. — no hay `BarraDeFiltros`, `Exportador` ni `PanelDeEvidencia`.
- No hay ningún guardián `canMatch`/`permisos.ts` en todo el repo (`grep -rl canMatch
  apps/ packages/` vacío antes de este carril).

**Supuesto declarado (no crítico, no toca dinero/seguridad/dato regulado):** trabajo
sobre la `dev` real, no sobre la descrita. Como la ficha me da `rutas/sistemas/` en
exclusiva y **no** un `layout/` que exista para mí, construyo el menú propio de
sistemas **dentro de mi carpeta** (`rutas/sistemas/menu/shell-sistemas.ts`), sin crear
ni tocar ningún `layout/` — sigue cumpliendo la regla no negociable "nunca comparte
menú con el financiero" porque no hay ningún menú compartido que pudiera tocar.

## Hueco crítico declarado: no existe contrato de "indicadores"/"tablero" para sistemas

Busqué en `servicios/erp/src/main/resources/openapi/erp.yaml` y en
`docs/CasosDeUso/CU-98*.md`. `erp.yaml` no expone estado de servicios, SLO, despliegues,
interruptores, base de datos, respaldos, proveedores, outbox, webhooks ni accesos como
API — es el ERP de aportes/pasarelas, no de plataforma. Los CU-98x hablan de
indicadores de **negocio** (mora, cobertura), no de plataforma técnica. No hay ningún
servicio "observabilidad" ni "plataforma" asignado en `planes/16` ni en `planes/07`.

**Esto es un hueco, no una decisión de implementación**: no se inventó una forma de
servicio. Las 8 pantallas de este carril se armaron con datos de ejemplo fijos en
`rutas/sistemas/dominio/datos-simulados.ts` (documentado en el propio archivo), el
mismo criterio que usaría `ejemploDe(...)` contra un contrato — pero sin `openapi/` del
que generarlo, porque no existe. Cuando exista un servicio de plataforma con su
`openapi/`, cada pantalla cambia una función (de datos fijos a `httpResource`, igual que
`cu13-consultar-saldo.ts` de operación) sin tocar el resto. **Pedido:** un contrato de
"indicadores de plataforma" al carril que asuma un futuro servicio de observabilidad —
no asignado hoy a nadie en `planes/16`.

## Pantallas

| Pantalla | Ruta | Entregable de la ficha | Organismos | Estados | Datos | Gate |
| --- | --- | --- | --- | :-: | :-: | :-: |
| Servicios y SLO | `/sistemas/servicios` | Estado de servicios, SLO con presupuesto de error | `TablaDeDatos`, `ChipEstado` | estático (sin red, ver hueco) | simulados | ✅ |
| Despliegues e interruptores | `/sistemas/despliegues` | Despliegues, interruptores | `TablaDeDatos`, `Interruptor`, `Dialogo`, `Alerta`, `Campo` | estático | simulados | ✅ |
| Base y migraciones | `/sistemas/base-de-datos` | Base y migraciones | `TablaDeDatos`, `ChipEstado` | estático | simulados | ✅ |
| Respaldos | `/sistemas/respaldos` | Respaldos con última restauración probada | `TablaDeDatos`, `ChipEstado` | estático | simulados | ✅ |
| Proveedores | `/sistemas/proveedores` | Proveedores con costo real | `TablaDeDatos`, `Monto` | estático | simulados | ✅ |
| Outbox y descartados | `/sistemas/outbox` | Outbox, descartados | `TablaDeDatos`, `ChipEstado` | estático | simulados | ✅ |
| Webhooks | `/sistemas/webhooks` | Webhooks | `TablaDeDatos`, `ChipEstado` | estático | simulados | ✅ |
| Accesos | `/sistemas/accesos` | Accesos (venía de F7/F8, dominio de sistemas) | `TablaDeDatos` | estático | simulados | ✅ |
| Incidentes | `/sistemas/incidentes` | Incidentes (ídem) | `TablaDeDatos`, `ChipEstado` | estático | simulados | ✅ |

Las 9 pantallas comparten `menu/shell-sistemas.ts` como enchufe propio (nav + `router-
outlet`); `sistemas.routes.ts` es la única ruta padre y lleva `canMatch: [
soloRolesDeSistemas]`, así que **ninguna** de las 9 carga su chunk si el rol no es
PLATAFORMA ni SEGURIDAD.

**Columna "Estados" en n/4:** con datos simulados y sin `httpResource` no hay un estado
de red que probar por pantalla (no hay `isLoading`/`error` reales — ese es justamente el
hueco de contrato de arriba). Se declara explícitamente: cuando el contrato de
indicadores exista, cada pantalla necesita su propio `*.spec.ts` con los cuatro estados,
igual que `pantalla-de-billetera.spec.ts` de operación. **Esto queda abierto.**

## Piezas declaradas por nivel

| Pieza | Nivel | Dónde vive | Estado |
| --- | --- | --- | --- |
| `soloRolesDeSistemas` (canMatch) | molécula (guardia de ruta) | `rutas/sistemas/guardia-rol-sistemas.ts` | hecho, probado |
| `restauracionVencida`, `puedeConfirmar` | átomo (función pura) | `rutas/sistemas/dominio/datos-simulados.ts` | hecho, probado |
| `ShellSistemas` | organismo (shell propio) | `rutas/sistemas/menu/shell-sistemas.ts` | hecho |
| 9 `Pantalla*` | página | `rutas/sistemas/<area>/pantalla-*.ts` | hecho |
| Datos simulados de plataforma | dato de arranque, no contrato | `rutas/sistemas/dominio/datos-simulados.ts` | hecho, declarado como hueco |

No se usó ninguna pieza de `packages/ui` que no existiera ya: `tabla-de-datos`,
`chip-estado`, `interruptor`, `alerta`, `dialogo`, `campo`, `boton`, `banda-de-
proposito`, `monto`. No hizo falta ningún micro-PR al paquete de diseño.

## Supuestos declarados

1. Se trabajó contra la `dev` real (sin `layout/`), no contra la descrita en el arranque
   — ver sección de desvío arriba.
2. No existe correo del operador en el token de sesión (`Sesion` solo guarda `acceso`,
   `permisos`, `rol`). El flujo de doble confirmación de `pantalla-despliegues.ts` usa un
   `SOLICITANTE_ACTUAL` fijo con un `TODO` explícito: hace falta que el contrato de
   autenticación devuelva el correo/identificador de quien pide el cambio para que la
   "primera persona" sea real y no una constante. No es crítico para demostrar el gate
   (que exige "la interfaz lo impide"), pero sí lo es para producción.
3. Sin contrato de indicadores de plataforma (hueco crítico de arriba), las 9 pantallas
   no llaman a ningún endpoint — no hay una URL de gateway que simular con Prism para
   ellas. El 403 de "doble barrera" que pide el gate se demuestra hoy solo por la
   barrera de interfaz (`canMatch`); la barrera de servidor queda pendiente hasta que el
   endpoint exista (ver Bloqueos).
4. Redirección del guard: un rol sin acceso se manda a `/operacion` (ruta que sí existe
   en el shell). No hay una pantalla de "acceso denegado" propia porque no la pide la
   ficha ni existe un patrón ya armado en el repo para copiar.

## Bloqueos

- **El contrato de indicadores de plataforma no existe.** Pedido declarado arriba; sin
  dueño asignado en `planes/16`. Mientras no exista, la "doble barrera" (interfaz +
  servidor 403) de este carril queda con la mitad probada: `canMatch` bloquea el
  `matching` de la ruta (probado, 4 pruebas en `guardia-rol-sistemas.spec.ts`), pero no
  hay un endpoint real que devuelva 403 para probar la otra mitad con `HttpTestingController`
  como hace `pantalla-de-billetera.spec.ts` en operación.
- El correo/identidad de quien pide un cambio de interruptor no viene de `Sesion` (ver
  supuesto 2). Cuando el contrato de autenticación lo agregue, se reemplaza la constante.

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | --- |
| Rol financiero bloqueado (interfaz) | `soloRolesDeSistemas` corta `canMatch` para un rol financiero | `guardia-rol-sistemas.spec.ts` — 4/4 pruebas verdes (ver Gate propio) | ✅ |
| Rol financiero bloqueado (servidor) | 403 del endpoint simulado | **no aplica todavía**: no hay endpoint (hueco de contrato) | ⬜ bloqueado |
| Restauración vencida > 30 días | marcada VENCIDA con datos de ejemplo | `datos-simulados.spec.ts` (4 pruebas) + `pantalla-respaldos.a11y.spec.ts` | ✅ |
| Interruptor de dinero, doble persona | la interfaz impide confirmarlo solo/a | `pantalla-despliegues.spec.ts` (4 pruebas) + `puedeConfirmar` (3 pruebas) | ✅ |
| Cola de descartados visible con motivo | columna "Motivo" en la tabla, sin detalle escondido | `pantalla-outbox.ts` + `pantalla-outbox.a11y.spec.ts` | ✅ |
| Menú propio, sin compartir con financiero | `shell-sistemas.ts` no importa nada de otro dominio; ningún otro dominio importa `rutas/sistemas` | `grep -rl "shell-sistemas\|rutas/sistemas" rutas/{operacion,cumplimiento,contabilidad,publicidad,tablero}` → vacío | ✅ |
| Entrega | lint, tipos, pruebas, build | salida citada abajo | ✅ |

## Gate propio — salida real de cada comando

### `yarn workspace @aportaya/backoffice build`
```
Application bundle generation complete. [2.043 seconds]
Initial total 294.41 kB | 80.72 kB transferido
Lazy chunks: pantalla-despliegues, shell-sistemas, pantalla-outbox, pantalla-respaldos,
pantalla-servicios, sistemas-routes, pantalla-proveedores, pantalla-webhooks,
pantalla-incidentes, pantalla-base-datos, y 6 más.
Output location: apps/backoffice/dist/backoffice
```

### `yarn workspace @aportaya/backoffice lint`
```
Linting "backoffice"...
All files pass linting.
=== apps/backoffice (Angular) ===
  OK    · sin red en vista
  OK    · sin literal de diseño
  OK    · sin formato de dinero
  OK    · sin console
  OK    · ningún archivo de más de 200 líneas
  OK    · sin literal de diseño en @aportaya/ui
TODO OK
```

### `yarn workspace @aportaya/backoffice typecheck`
Sin salida (sin errores) — verificado también filtrando por `sistemas`: cero
coincidencias.

### `yarn workspace @aportaya/backoffice test:front`
```
 Test Files  6 passed (6)
      Tests  27 passed (27)
```
Incluye: `guardia-rol-sistemas.spec.ts` (4), `datos-simulados.spec.ts` (7),
`pantalla-despliegues.spec.ts` (4), más los 12 preexistentes de operación (`enchufe-de-
rutas`, `pantalla-de-billetera`).

### `yarn workspace @aportaya/backoffice test:a11y`
```
 Test Files  4 passed (4)
      Tests  6 passed (6)
```
Incluye `pantalla-servicios.a11y.spec.ts`, `pantalla-respaldos.a11y.spec.ts`,
`pantalla-outbox.a11y.spec.ts` sin violaciones serias (axe), más el preexistente de
`pantalla-de-billetera`.

**Nota honesta:** no hay una prueba `.a11y.spec.ts` para las 6 pantallas restantes
(despliegues, base-de-datos, proveedores, webhooks, accesos, incidentes) — mismo patrón
de componente que las 3 cubiertas, riesgo bajo, pero **queda abierto** por presupuesto
de esta sesión. Ver "Qué queda abierto".

### Verificación de aislamiento del menú (delta D-2)
```
$ grep -rl "shell-sistemas\|rutas/sistemas" apps/backoffice/src/app/rutas/{operacion,cumplimiento,contabilidad,publicidad,tablero}
(sin salida)
```
Ningún dominio financiero importa nada de sistemas, y `shell-sistemas.ts` no importa
nada de `layout/` ni de otro dominio (no existe `layout/` en este repo, ver desvío).

## Qué queda abierto

1. **Contrato de indicadores de plataforma** (bloqueo principal, ver arriba) — sin él,
   las 9 pantallas siguen siendo datos de ejemplo fijos, no `httpResource` contra un
   gateway real, y la barrera de servidor (403) no se puede probar.
2. `.a11y.spec.ts` para 6 de las 9 pantallas (ver nota arriba).
3. El correo de quien pide un cambio de interruptor es una constante (`SOLICITANTE_ACTUAL`
   con `TODO`), no viene de `Sesion` — falta que el contrato de autenticación lo exponga.
4. No hay paginación ni filtro en `TablaDeDatos` de estas pantallas (el componente los
   soporta a medias — orden sí, paginación la maneja quien la usa) porque los datos de
   ejemplo son pocos; con datos reales de outbox/webhooks esto va a hacer falta.
5. `packages/simulado/ejemplos/<servicio>/` no se usó (no hay servicio real del que
   generar ejemplos) — se declaró en su lugar `dominio/datos-simulados.ts`, documentado
   con la razón en el propio archivo.
