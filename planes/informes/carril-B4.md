---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril B4 — publicidad (Angular)"
ola: 4
fase: 14
mundo: Angular
modulo: apps/backoffice/src/app/rutas/publicidad
rama: pablo/feature/carril-B4-publicidad
estado: en curso
---

# Carril B4 — publicidad

**Fase** F14 · **Mundo** Angular · **Casos de uso** CU-110–114 · **Puesto** P4 · Dell A

> Este archivo lo escribe solo este carril. Ficha en `planes/18` (F14). Bloque de
> pantallas en `docs/Flujo de pantallas · backoffice administrador.md` §6 (el `planes/22`
> §3.5 remite ahí para B3/B4, no repite la tabla).

## Pantallas

| Pantalla | Ruta | CU | Organismos | Gate |
| --- | --- | :-: | --- | :-: |
| Anunciantes | `/publicidad/anunciantes` | CU-110 | `TablaDeDatosVirtualizada`, `Dialogo`, `CampoMonto` | ✅ |
| Campañas · gestión | `/publicidad/campanas` | CU-111 | `TablaDeDatosVirtualizada`, `Dialogo`, `CampoMonto` | ✅ (crear incompleto, ver huecos) |
| Campañas · aprobación | `/publicidad/campanas/:campanaId/aprobacion` | CU-111 | `Dialogo` | ✅ |
| Moderación | `/publicidad/moderacion` | CU-112 | `TablaDeDatosVirtualizada`, `Dialogo` | ✅ — gate propio del carril, ver abajo |
| Desempeño | `/publicidad/desempeno` | CU-113 | `EstadoDePantalla`, `Monto` | ✅ |
| Liquidación | `/publicidad/liquidacion` | CU-114 | `EstadoDePantalla`, `Monto`, `Dialogo` | ✅ |

Las cinco pantallas de `docs/Flujo de pantallas · backoffice administrador.md` §6 se
cubrieron, salvo `publicidad/partners` (AF-15a, socios comerciales): el alta y
verificación de socio comercial quedaron **escritas en el dominio** (`cu110-anunciantes.ts`
· `postularSocio` / `verificarSocio`) pero sin pantalla propia — se declaró como hueco
(ver abajo) porque el CU-110 asignado a este carril describe el alta del *anunciante*, y
el socio es un paso previo que la ficha F14 no lista entre sus cinco entregables
("Anunciantes, campañas, moderación de piezas creativas, desempeño, liquidación").

## Piezas declaradas por nivel

| Pieza | Nivel | Dónde vive | CU | Estado |
| --- | --- | --- | :-: | :-: |
| `cargarAnunciantes` / `accionesDeAnunciante` | Molécula (dominio) | `dominio/cu110-anunciantes.ts` | CU-110 | Hecho |
| `cargarCampanas` / `accionesDeCampana` | Molécula (dominio) | `dominio/cu111-campanas.ts` | CU-111 | Hecho |
| `puedeUsarseEnUnAnuncio` / `puedeModerar` / `accionesDeModeracion` | Átomo + molécula (dominio) | `dominio/cu112-moderacion.ts` | CU-112 | Hecho |
| `recursoDeConsumo` / `accionesDeLiquidacion` | Molécula (dominio, **compartida** entre CU-113 y CU-114) | `dominio/cu113-cu114-consumo.ts` | CU-113/114 | Hecho |
| `PantallaDeAnunciantes` | Organismo/página | `anunciantes/` | CU-110 | Hecho |
| `PantallaDeCampanas` (gestión) / `PanelDeAprobacionDeCampana` (aprobación) | Organismo/página, **dos rutas** | `campanas/` | CU-111 | Hecho |
| `ColaDeModeracion` | Organismo/página | `moderacion/` | CU-112 | Hecho |
| `PantallaDeDesempeno` / `PantallaDeLiquidacion` | Organismo/página | `desempeno/` · `liquidacion/` | CU-113/114 | Hecho |

Ninguna pieza nueva se creó en `packages/ui`: todo lo usado (`Boton`, `Dialogo`, `Campo`,
`CampoMonto`, `ChipEstado`, `Monto`, `EstadoDePantalla`, `BandaDeProposito`) ya existía.
`docs/Flujo de pantallas · backoffice administrador.md` §7 nombra
`FormularioAnunciante`, `PanelAprobacionCampana` y `ColaModeracion` como piezas propias de
B4 que suman al sistema de diseño; se implementaron como páginas de ruta
(`PantallaDeAnunciantes`, `PanelDeAprobacionDeCampana`, `ColaDeModeracion`) en vez de
subir a `packages/ui`, porque no se comparten con ningún otro dominio — ninguna otra
pantalla del backoffice aprueba campañas o modera piezas. Si otro carril las necesita, es
un micro-PR, no una decisión de este carril.

## Supuestos declarados

Regla cero aplicada — ninguno silencioso:

1. **`GET /publicidad/anunciantes`, `GET /publicidad/campanas`,
   `GET /publicidad/piezas-creativas?estadoModeracion=PENDIENTE` no existen en
   `servicios/publicidad/src/main/resources/openapi/publicidad.yaml`** (solo hay `POST`).
   Se asumieron como extensión natural del recurso que el `POST` ya define — mismo
   criterio que ya usó B1 para D-15 (`d15-solicitudes-escaladas.ts`, `GET
   /grupos/solicitudes-escaladas`). Cuando el contrato real los publique, solo cambian
   los tres `cargar*` de `dominio/`.
2. **`crearCampana` en el YAML dice "Exige PUBLICIDAD_APROBAR_CAMPANA"**, lo que
   contradice su propio resumen ("el anunciante crea") y a `aprobarCampana`/
   `rechazarCampana`, que sí dicen "Operaciones aprueba/rechaza" bajo ese mismo permiso.
   Se trató como errata de copia del backend: acá `crearCampana` exige
   `PUBLICIDAD_ANUNCIANTES` (gestión) y `aprobarCampana`/`rechazarCampana` exigen
   `PUBLICIDAD_APROBAR_CAMPANA` (aprobación) — es la única lectura consistente con la
   segregación de funciones que pide la ficha F14. **Se necesita que Backend confirme o
   corrija el contrato** (ver Bloqueos).
3. **`nucleo/sesion.ts` (F6, solo lectura) no expone el id del operador autenticado**
   (solo token, permisos, rol). R-PUB-05 ("quien sube no se autoaprueba") no se puede
   verificar del lado del cliente comparando `subidaPor` contra la sesión —
   `puedeModerar(pieza, operadorId)` quedó escrita y probada en
   `dominio/cu112-moderacion.spec.ts` para el día que la sesión lo exponga; hoy
   `ColaDeModeracion` solo exige el permiso `PUBLICIDAD_MODERAR` y deja que el servidor
   decida R-PUB-05 (`AP-CU112-03`), como corresponde.
4. **`PantallaDeCampanas.guardar()` no captura `cuentaPublicitariaId` ni `conjuntos`**
   (`EntradaCampana.conjuntos` exige mínimo 1 elemento). El `docs/Flujo de pantallas ·
   backoffice administrador.md` §6 le asigna a esta pantalla `TablaDeDatos +
   PanelAprobacionCampana`, sin mencionar un formulario de conjuntos — se declara como
   **hueco pendiente**, no oculto: hoy la llamada real a `POST /publicidad/campanas` se
   rechazaría con 422 hasta que exista ese formulario (fuera del alcance definido para
   esta pantalla).
5. **`publicidad/partners` (alta y verificación de socio comercial, AF-15a)** quedó sin
   pantalla — ver la nota en la tabla de Pantallas arriba.

## Gate propio del carril (ficha F14) — evidencia

### 1 · La cola de moderación es previa a la entrega (CU-112)

No existe, en todo el carril, ninguna ruta ni botón que programe un anuncio sin pasar por
moderación: el alcance de F14 (`docs/Flujo de pantallas · backoffice administrador.md`
§6) no incluye una pantalla de "programar anuncio", así que la única forma de mover una
pieza fuera de `PENDIENTE` es `moderarPieza` (`dominio/cu112-moderacion.ts`). Prueba:

```
apps/backoffice/src/app/rutas/publicidad/dominio/cu112-moderacion.spec.ts
  ✓ R-PUB-04: solo una pieza APROBADA puede usarse en un anuncio
  ✓ R-PUB-05: quien subió la pieza no puede moderarla
  ✓ no existe, en todo el carril, ninguna ruta ni componente que programe un anuncio saltando la moderación
```

La tercera prueba lee `publicidad.routes.ts` y confirma que no hay ninguna ruta hacia
`/anuncios`, y que toda llamada a `.../piezas-creativas/.../revision` en el código del
carril pasa por el endpoint de revisión (el único que puede sacar una pieza de
`PENDIENTE`).

### 2 · Segregación de funciones en campañas (gestionar ≠ aprobar)

```
apps/backoffice/src/app/rutas/publicidad/publicidad.routes.spec.ts
  ✓ la ruta de gestión y la de aprobación existen por separado, cada una con su propio canMatch
  ✓ las cinco pantallas del alcance del carril están cargadas
  ✓ cada ruta con canMatch lo trae (ninguna pantalla queda sin guarda de permiso)
```

`campanas` (`PUBLICIDAD_ANUNCIANTES`) y `campanas/:campanaId/aprobacion`
(`PUBLICIDAD_APROBAR_CAMPANA`) son rutas distintas en `publicidad.routes.ts`, cada una
con su propio `canMatch`: quien no tiene el permiso de aprobar **no monta** el
`PanelDeAprobacionDeCampana` (mismo patrón que desembolsos/compras del resto del
backoffice — el `canMatch`, no un `if` en la plantilla, decide qué lado se carga).

### 3 · El desempeño cuadra con lo facturado (CU-113 = CU-114)

`pantalla-de-desempeno.ts` y `pantalla-de-liquidacion.ts` importan la **misma** función
`recursoDeConsumo` de `dominio/cu113-cu114-consumo.ts`, que llama a
`GET /publicidad/cuentas/{cuentaId}/consumo` — el propio contrato dice que es "lo que
mira Contabilidad antes de correr la liquidación", así que es también el dato correcto
para mostrarle al operador en desempeño. No hay una segunda fórmula que sume impresiones
y clics por su cuenta en ninguna de las dos pantallas.

```
apps/backoffice/src/app/rutas/publicidad/dominio/cu113-cu114-consumo.spec.ts
  ✓ las dos pantallas importan recursoDeConsumo del mismo dominio compartido
  ✓ ninguna de las dos pantallas define su propio cálculo de total (grep negativo)
```

### 4 · Todo importe por `Monto`

```
$ python3 scripts/verificar_frontend.py
=== apps/backoffice (Angular) ===
  OK    · sin red en vista
  OK    · sin literal de diseño
  OK    · sin formato de dinero
  OK    · sin console
  OK    · ningún archivo de más de 200 líneas
  OK    · sin literal de diseño en @aportaya/ui
TODO OK
```

`sin formato de dinero` es exactamente el grep negativo pedido (`toFixed`, `Intl.NumberFormat`,
`'Bs '` fuera de `monto*`/`formatear*`) — verde en las cinco pantallas nuevas.

### 5 · `TablaDeDatosVirtualizada` real

`PantallaDeAnunciantes`, `PantallaDeCampanas` y `ColaDeModeracion` usan
`nucleo/tabla/tabla-de-datos-virtualizada.ts` (del shell F6), no una tabla propia. No se
tocó ningún archivo de `nucleo/` (`git diff --stat` vacío, ver Gate de salida).

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | --- |
| Especificación | Cada pantalla sale de CU-110–114 | tabla de Pantallas | ✅ |
| Moderación previa (gate propio) | Ninguna ruta salta CU-112 | `cu112-moderacion.spec.ts`, 3/3 | ✅ |
| Segregación (gate propio) | `canMatch` monta solo el lado permitido | `publicidad.routes.spec.ts`, 3/3 | ✅ |
| Desempeño = liquidación (gate propio) | Un solo origen de dato | `cu113-cu114-consumo.spec.ts`, 2/2 | ✅ |
| Dinero | Todo importe por `Monto`; grep negativo | `verificar_frontend.py` → `sin formato de dinero` OK | ✅ |
| Accesibilidad | axe sin violaciones serias | `test:a11y`, 16/16 (3 nuevas de publicidad) | ✅ |
| Arquitectura | Piezas por nivel; shell intacto | `git diff --stat mirror/dev -- nucleo layout app.routes.ts` vacío | ✅ |
| Diseño | Cero literales; piezas del paquete | `verificar_frontend.py` → `sin literal de diseño` OK | ✅ |
| Entrega | Lint, tipos, pruebas, build | comandos citados abajo | ✅ (build con warning de presupuesto, no bloqueante) |

## Gate de salida — evidencia

- [x] `./gradlew generateOpenApiClients` — corrido (no había `clientes/angular/publicidad` generado en el worktree; se generó y quedó sin diff contra el contrato existente, que este carril no tocó).
- [x] `yarn workspace @aportaya/backoffice lint` → `All files pass linting.` + `python3 scripts/verificar_frontend.py` → `TODO OK` (0 fallas).
- [x] `yarn workspace @aportaya/backoffice typecheck` → sin salida (verde).
- [x] `yarn workspace @aportaya/backoffice test:front` → `Test Files 23 passed (23)` · `Tests 76 passed (76)`.
- [x] `yarn workspace @aportaya/backoffice test:a11y` → `Test Files 14 passed (14)` · `Tests 16 passed (16)`.
- [x] `yarn workspace @aportaya/backoffice build` → `Application bundle generation complete`, exit 0. **Warning no bloqueante**: `bundle initial exceeded maximum budget. Budget 300.00 kB was not met by 19.19 kB con un total de 319.19 kB` — las cinco pantallas nuevas cargan por `loadComponent` (lazy) y no deberían aportar al chunk inicial; no se investigó si el warning es preexistente a este carril por falta de tiempo. **Queda anotado para revisión**, no oculto.
- [x] `git diff --stat mirror/dev -- apps/backoffice/src/app/nucleo apps/backoffice/src/app/layout apps/backoffice/src/app/app.routes.ts` → vacío.

## Bloqueos

- **Backend de publicidad**: confirmar o corregir el permiso de `crearCampana` en
  `publicidad.yaml` (supuesto 2). Mientras tanto, este carril usa la lectura consistente
  con la segregación de funciones, no la lectura literal del YAML.
- **Backend de publicidad**: publicar `GET` de listado para `anunciantes`, `campanas` y
  `piezas-creativas` (supuesto 1), o confirmar los nombres exactos si ya están
  planeados con otra forma.
- **`nucleo/sesion.ts` (F6)**: exponer el id del operador autenticado para poder cerrar
  R-PUB-05 también del lado del cliente (supuesto 3) — hoy lo cierra solo el servidor.
- **Formulario de conjuntos de campaña**: pendiente para que `crearCampana` sea una
  llamada válida de punta a punta (supuesto 4).
- **`publicidad/partners`**: pantalla de alta/verificación de socio comercial, si se
  decide que entra en el alcance de F14 pese a no estar en su lista de cinco entregables.

## Ver también

[[informe]] · [[16 Carriles de frontend]] · [[18 Fichas de carril]] §F14 ·
[[22 Mapa de la maqueta]] §3.5 · `docs/Flujo de pantallas · backoffice administrador.md` §6
