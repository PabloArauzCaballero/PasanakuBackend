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

### Reconciliación posterior (mismo día)

El coordinador fusionó el `dev` correcto (con el carril B — shell backoffice, F6 —
fusionado) dentro de esta rama (commit `8bec722 merge: dev (M/B/W ya fusionados) para
reconciliar contra la infraestructura real`), sin conflicto de archivo. Con la base
correcta aparecieron piezas reales que no existían cuando arranqué:

- `apps/backoffice/src/app/layout/shell-financiero.ts` — el shell financiero real, con
  su propio menú por `Sesion.puede(permiso)`.
- `apps/backoffice/src/app/layout/sistemas/` — **vacío a propósito, con un
  `README.md`** que decía textualmente: *"el shell de sistemas (menú y cabecera para
  PLATAFORMA/SEGURIDAD) se dibuja ahí, no en F6 (...) Mientras tanto, la ruta `sistemas`
  reutiliza `ShellFinanciero` desde `app.routes.ts`."* Es decir: F6 sí me había dejado el
  lugar correcto, tal como decía la ficha original — mi supuesto de arriba quedó
  superado por la base real.
- `apps/backoffice/src/app/nucleo/permisos.ts` con `requierePermiso(permiso): CanMatchFn`
  (patrón `canMatch` genérico por permiso, ya usado por `app.routes.ts` para las otras
  rutas) y su prueba `permisos.spec.ts`.
- `apps/backoffice/src/app/app.routes.ts` (frozen, de F6) ya envuelve la ruta `sistemas`
  con `canMatch: [requierePermiso('ver:sistemas')]` — una primera barrera de permiso que
  no tenía cuando escribí el `sistemas.routes.ts` original.
- `apps/backoffice/src/app/nucleo/tabla/tabla-de-datos-virtualizada.ts` (paginación de
  **servidor**, vía `CargadorDePagina`), `nucleo/filtros/barra-de-filtros.ts`,
  `nucleo/evidencia/panel-de-evidencia.ts`, `nucleo/exportador.ts`.

**Corrección aplicada:** moví `rutas/sistemas/menu/shell-sistemas.ts` a
`layout/sistemas/shell-sistemas.ts` (el lugar real, `git mv`), borré el
`layout/sistemas/README.md` que marcaba el hueco (ya lo llené) y ajusté el único import
que cambiaba (`sistemas.routes.ts` ahora carga el shell desde `../../layout/sistemas/
shell-sistemas`). `rutas/sistemas/` volvió a ser solo pantallas + rutas + dominio, como
corresponde.

**Sobre el guard de roles:** mantuve `soloRolesDeSistemas` (mi `canMatch` propio, ya
probado) como **segunda barrera** además de la que ya pone `app.routes.ts` con
`requierePermiso('ver:sistemas')`. Revisé `nucleo/permisos.ts`: `requierePermiso` es
genérico por **permiso** (`Sesion.puede(permiso)`), no por **rol** — no cubre por sí
solo la exigencia de la ficha de que `layout/sistemas/` sea "propio de roles PLATAFORMA
y SEGURIDAD" (un permiso `ver:sistemas` mal otorgado a otro rol pasaría igual). Por eso
NO reemplacé mi guard por `requierePermiso`: lo dejé como una barrera adicional, más
estricta, sobre `Sesion.rol()` — que es exactamente la misma clase `Sesion` real (sin
diff contra la que ya usaba: `git diff b5d5d5e 8bec722 -- .../nucleo/sesion.ts` vacío).
`TablaDeDatosVirtualizada` no se adoptó: pagina contra un `CargadorDePagina` de
servidor, y mis 9 pantallas no tienen servidor (hueco de contrato, ver abajo) — queda
como decisión abierta para cuando el contrato exista.

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

Las 9 pantallas comparten `layout/sistemas/shell-sistemas.ts` como enchufe propio (nav +
`router-outlet`) — el lugar que F6 dejó preparado para B5, no una carpeta dentro de
`rutas/`. `sistemas.routes.ts` es la única ruta padre y lleva `canMatch: [
soloRolesDeSistemas]` como **segunda** barrera, detrás de la primera que ya pone
`app.routes.ts` (`canMatch: [requierePermiso('ver:sistemas')]`, frozen, de F6). Así que
**ninguna** de las 9 carga su chunk si el rol no es PLATAFORMA ni SEGURIDAD, ni si falta
el permiso `ver:sistemas`.

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
| `ShellSistemas` | organismo (shell propio) | `layout/sistemas/shell-sistemas.ts` | hecho, migrado a su lugar real |
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
| Menú propio, sin compartir con financiero | `layout/sistemas/shell-sistemas.ts` no importa nada de `layout/shell-financiero.ts` ni viceversa | imports listados arriba, ambos limpios | ✅ |
| Entrega | lint, tipos, pruebas, build | salida citada abajo | ✅ |

## Gate propio — salida real de cada comando (post-reconciliación, commit de merge `8bec722` ya incorporado)

### `yarn workspace @aportaya/backoffice build`
```
Application bundle generation complete. [1.563 seconds]
Initial total 321.16 kB | 87.68 kB transferido
Lazy chunks incluyen: pantalla-de-estado, pantalla-despliegues, pantalla-de-billetera,
tablero, shell-sistemas, pantalla-outbox, pantalla-respaldos, pantalla-servicios,
sistemas-routes, pantalla-proveedores, pantalla-webhooks, y 9 más.

⚠ WARNING: bundle initial exceeded maximum budget. Budget 300.00 kB was not met by
21.16 kB con un total de 321.16 kB.
Output location: apps/backoffice/dist/backoffice
```
El build **compila y termina en verde**; el único aviso es un *warning* de presupuesto
de bundle inicial (no un error), y es preexistente a este carril — `shell-sistemas` es
un *lazy chunk* de 2.56 kB, no entra en el bundle inicial; el crecimiento viene de lo ya
fusionado de B1/B2/F6/M/W. No lo puedo resolver desde `rutas/sistemas/` ni
`layout/sistemas/` sin tocar código de otro carril.

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
Sin salida (sin errores).

### `yarn workspace @aportaya/backoffice test:front`
```
 Test Files  11 passed (11)
      Tests  45 passed (45)
```
Incluye los míos (`guardia-rol-sistemas.spec.ts` 4, `datos-simulados.spec.ts` 7,
`pantalla-despliegues.spec.ts` 4 — 15 en total) más los ya fusionados de B1/B2/F6 tras
la reconciliación (`permisos.spec.ts`, `enchufe-de-rutas.spec.ts`, `pantalla-de-
billetera.spec.ts`, `tabla-de-datos-virtualizada.spec.ts`, `navegacion-por-
teclado.spec.ts`, `barra-de-filtros.spec.ts`, etc.).

### `yarn workspace @aportaya/backoffice test:a11y`
```
 Test Files  5 passed (5)
      Tests  7 passed (7)
```
Incluye `pantalla-servicios.a11y.spec.ts`, `pantalla-respaldos.a11y.spec.ts`,
`pantalla-outbox.a11y.spec.ts` (mías, sin violaciones serias vía axe) más el
preexistente de `pantalla-de-billetera` y uno nuevo llegado con la reconciliación.

**Nota honesta:** sigue sin haber `.a11y.spec.ts` para las 6 pantallas restantes
(despliegues, base-de-datos, proveedores, webhooks, accesos, incidentes) — mismo patrón
de componente que las 3 cubiertas, riesgo bajo, pero **queda abierto**. Ver "Qué queda
abierto".

### Verificación de aislamiento del menú (delta D-2), contra el `layout/` real

```
$ grep -n "^import" apps/backoffice/src/app/layout/shell-financiero.ts
1:import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
2:import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router'
3:import { Sesion } from '../nucleo/sesion'

$ grep -n "^import" apps/backoffice/src/app/layout/sistemas/shell-sistemas.ts
1:import { ChangeDetectionStrategy, Component } from '@angular/core'
2:import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router'
3:import { textosSistemas } from '../../rutas/sistemas/textos'
```
`shell-financiero.ts` no importa nada de `layout/sistemas/`; `shell-sistemas.ts` no
importa nada de `shell-financiero.ts` ni de ningún dominio financiero — cada uno importa
solo `@angular/core`, `@angular/router` y su propio dominio (`Sesion` es núcleo
compartido de infraestructura, no menú). Verificado también en texto libre (comentarios,
strings) por si hubiera una referencia oculta:
```
$ grep -rn "shell-financiero\|financiero" apps/backoffice/src/app/layout/sistemas apps/backoffice/src/app/rutas/sistemas
(solo apariciones en comentarios/strings propios que EXPLICAN el aislamiento — ver
arriba — ninguna es un import)
```

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
6. `nucleo/tabla/tabla-de-datos-virtualizada.ts` (paginación de servidor vía
   `CargadorDePagina`, virtualización CDK) llegó con la reconciliación y no se adoptó:
   mis 9 pantallas no tienen un servidor del que paginar (mismo hueco de contrato). Con
   `TablaDeDatos` simple alcanza para las filas de ejemplo actuales. Cuando exista el
   contrato de indicadores, conviene revisar si outbox/webhooks/accesos —que en
   producción pueden crecer mucho— deberían pasar a la versión virtualizada.
7. El warning de presupuesto de bundle inicial (`build`, +21.16 kB sobre 300 kB) es
   preexistente a este carril (ver Gate propio) — no se puede resolver sin tocar código
   fuera de `rutas/sistemas/` y `layout/sistemas/`.
