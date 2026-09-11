---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril B — F6 shell backoffice (Angular)"
ola: F1
fase: F6
mundo: Angular
modulo: apps/backoffice/src/app/{nucleo,layout}
rama: pablo/feature/carril-B-shell-backoffice
estado: en curso
---

# Carril B — F6 shell backoffice

**Fase** F6 · **Mundo** Angular · **Casos de uso** ninguno de negocio (shell) · **Puesto** P3 · Legion

> Ficha en [[18 Fichas de carril · las 38 unidades de trabajo]] · `F6`. Documento de
> fase [[13 Fases F6 a F8 · Backoffice]]. Posee `apps/backoffice/src/app/{nucleo,layout}/`,
> `app.routes.ts`, `app.config.ts`, `rutas/tablero/`, `rutas/operacion/estado`, y se
> congela al cerrar. Punto de partida: el andamiaje de `F0-B` (`nucleo/` con los cuatro
> interceptores, `Sesion`, `GATEWAY`, `app.routes.ts` con el enchufe por dominio,
> `enchufe-de-rutas.spec.ts`, la pantalla de billetera de F0) — **no se reescribió**,
> se completó.

## Qué está hecho, con la salida que lo prueba

| Entregable | Evidencia | Estado |
| --- | --- | :-: |
| **Shell financiero** (`layout/shell-financiero.ts`): menú por dominio, cabecera con rol, `router-outlet` | montado en `app.routes.ts` como componente raíz con `children` | ✅ |
| `layout/sistemas/` dejado **vacío**, congelado para `B5` | `layout/sistemas/README.md`, sin componentes | ✅ |
| `ServicioSesion` con rol y permisos | Ya existía en `nucleo/sesion.ts` (F0-B); se reutilizó tal cual, sin tocarlo | ✅ |
| Interceptores con **registro de acceso** | `nucleo/registro-de-acceso.interceptor.ts` + `ACCESO_A_DATOS` (`HttpContextToken`), agregado a la cadena en `app.config.ts` | ✅ |
| Guardias `canMatch` por permiso | `nucleo/permisos.ts` (`requierePermiso`), aplicado a los cinco dominios en `app.routes.ts` | ✅ 2 pruebas |
| **`TablaDeDatosVirtualizada`** (CDK) | `nucleo/tabla/tabla-de-datos-virtualizada.ts` + `tipos.ts` + `navegacion-por-teclado.ts`: columnas configurables, paginación server-side, orden con lista blanca, selección múltiple, `cdk-virtual-scroll-viewport` | ✅ 6 pruebas + 5 de navegación |
| `BarraDeFiltros` con estado en la URL | `nucleo/filtros/barra-de-filtros.ts`, sobre `ChipsDeFiltro`/`CampoBusqueda` de `@aportaya/ui`, `?filtro=…&q=…`, resetea `pagina` | ✅ 2 pruebas |
| `Exportador` (CU-58) | `nucleo/exportador.ts`: `puedeExportar()` por permiso, `solicitar()` llama al gateway — **nunca arma el archivo en el cliente** | ✅ |
| `PanelDeEvidencia` | `nucleo/evidencia/panel-de-evidencia.ts` | ✅ (hueco documentado abajo) |
| Borrador local (`ServicioBorrador`) | `nucleo/borrador.ts`, `IndexedDB`, no `localStorage` | ✅ (supuesto abajo) |
| `rutas/tablero/` | `Tablero`: accesos por permiso, **sin KPI inventado** (regla cero) | ✅ |
| `rutas/operacion/estado` | `PantallaDeEstado`: arquitectura y nivel de criticidad de los 14 servicios (mismo contenido que la maqueta), ejercitando `TablaDeDatosVirtualizada` + `BarraDeFiltros` + `Exportador` juntos | ✅ 3 + 1 a11y |
| Acceso administrativo (ADR-038) | **No se hizo** | ⬜ hueco, ver abajo |

### Evidencia, con el comando y su resultado

```
$ yarn workspace @aportaya/backoffice typecheck
(sin salida — 0 errores)

$ yarn workspace @aportaya/backoffice lint
All files pass linting.
=== apps/backoffice (Angular) ===
  OK · sin red en vista
  OK · sin literal de diseño
  OK · sin formato de dinero
  OK · sin console
  OK · ningún archivo de más de 200 líneas
  OK · sin literal de diseño en @aportaya/ui
TODO OK

$ yarn workspace @aportaya/backoffice build
Initial total         | 319.99 kB | 87.50 kB (transferido)
Lazy: pantalla-de-estado 55.89 kB · pantalla-de-billetera 7.17 kB · tablero 2.60 kB ·
      operacion-routes 395 B · cumplimiento/contabilidad/publicidad/sistemas-routes < 40 B c/u
⚠ bundle initial excede el presupuesto por 19,99 kB (300 kB warning / 400 kB error) — no bloquea

$ yarn workspace @aportaya/backoffice test:front
Test Files  8 passed (8)
     Tests  30 passed (30)

$ yarn workspace @aportaya/backoffice test:a11y
Test Files  2 passed (2)
     Tests  4 passed (4)

$ grep noindex apps/backoffice/dist/backoffice/browser/index.html
<meta name="robots" content="noindex, nofollow, noarchive">
# apps/backoffice/docker/nginx.conf ya trae: add_header X-Robots-Tag "noindex, nofollow, noarchive" always;
# (heredado de F0-B; no se corrió `docker build`+`curl` contra un contenedor real — ver huecos)
```

También corridos, filtrados por Turborepo, con el mismo resultado:
`yarn lint --filter=@aportaya/backoffice`, `yarn typecheck --filter=@aportaya/backoffice`,
`yarn test:front --filter=@aportaya/backoffice`, `yarn test:a11y --filter=@aportaya/backoffice`.

## Piezas declaradas por nivel

| Pieza | Nivel | Dónde vive | Estado |
| --- | --- | --- | :-: |
| `ShellFinanciero` | organismo (layout) | `layout/shell-financiero.ts` | ✅ |
| `TablaDeDatosVirtualizada` | organismo | `nucleo/tabla/tabla-de-datos-virtualizada.ts` | ✅ |
| `accionParaTecla`, `enfocarFilaPorId` | átomo (función pura + utilidad DOM) | `nucleo/tabla/navegacion-por-teclado.ts` | ✅ |
| `BarraDeFiltros` | molécula (compone `ChipsDeFiltro` + `CampoBusqueda` de `@aportaya/ui`) | `nucleo/filtros/barra-de-filtros.ts` | ✅ |
| `PanelDeEvidencia` | organismo | `nucleo/evidencia/panel-de-evidencia.ts` | ✅ (con hueco de `LineaDeTiempo`) |
| `Exportador` | servicio (no componente) | `nucleo/exportador.ts` | ✅ |
| `ServicioBorrador` | servicio | `nucleo/borrador.ts` | ✅ |
| `requierePermiso` | átomo funcional (`canMatch`) | `nucleo/permisos.ts` | ✅ |
| `registroDeAccesoInterceptor` | interceptor | `nucleo/registro-de-acceso.interceptor.ts` | ✅ |

## Comparación contra la maqueta

| Pantalla | Escenario | Igual | Distinto (y por qué) | Corregido en la maqueta |
| --- | :-: | --- | --- | --- |
| `operacion/estado` («Arquitectura y estado del proyecto») | único | Mismas 14 filas (servicio, nivel, réplicas, por qué), mismo criterio ADR-037 | La maqueta usa una tabla estática con paneles de resumen por nivel; acá se usa `TablaDeDatosVirtualizada` para ejercitar el organismo del shell — el contenido de negocio es el mismo, la pieza de UI es la del sistema de diseño real, no HTML de maqueta | No |
| `tablero` | único | Es el punto de entrada por rol | **Distinto a propósito**: la maqueta muestra el tablero con KPI del día en `operacion/tablero` (carril `B1`/F7 — «10 segundos para saber si hay algo que atender»). Este `tablero` raíz es el landing de F6 y no tiene ningún CU ni contrato de indicadores propio; mostrar números ahí sería inventar (regla cero) | No — son pantallas distintas del mapa |

## Ejemplos del contrato

Ninguno: F6 es shell, sin CU de negocio propio. `PantallaDeEstado` usa una constante de
arquitectura (`rutas/operacion/estado/dominio/estado-del-sistema.ts`), no un ejemplo de
`packages/simulado/`, porque no existe contrato `/auditoria/estado` todavía (ver
supuesto declarado). No se creó ningún archivo en `packages/simulado/ejemplos/` en este
carril.

## Supuestos declarados

| Supuesto | Por qué | Quién lo puede revisar |
| --- | --- | --- |
| `operacion/estado` lee una **constante estática**, no un `httpResource` | No hay contrato `/auditoria/estado` en ningún `openapi/*.yaml`: los 14 servicios no tienen código de aplicación todavía (la propia maqueta lo dice: «Código de los 14 servicios: 0 de 14»). Inventar un contrato JSON en `packages/simulado/ejemplos/auditoria/` sin un `openapi` real habría sido peor que declarar el supuesto | Carril que implemente `auditoria` — el cambio es una función, no la pantalla |
| `Tablero` no muestra ningún KPI | Ningún CU ni la ficha F6 le da a este carril un contrato de indicadores de tablero; el tablero con cifras es `operacion/tablero` y es de `B1`/F7 | `B1` al implementar F7 |
| `ServicioBorrador` no cifra el valor antes de guardarlo en `IndexedDB` | No hay una clave de cifrado de aplicación segura para guardar en el cliente sin exponerla igual; se confía en el cifrado de disco del sistema operativo del operador, como el resto del backoffice | Quien lo use en F8 (ROS, actas) puede pedir cifrado adicional si lo necesita |
| El permiso de cada dominio (`ver:operacion`, `exportar:estado-plataforma`, etc.) es un **nombre inventado por este carril**, sin catálogo de permisos todavía | No existe (a la fecha) un catálogo de permisos publicado por `identidad` en la bóveda al que este carril tenga acceso de solo lectura verificado; se usó una convención `<verbo>:<recurso>` legible, documentada en el código, fácil de renombrar en masa | `identidad`/`B1` cuando el catálogo real exista |
| `TablaDeDatosVirtualizada` pagina por **página completa** (server-side) y virtualiza el **renderizado** de esa página con el CDK, en vez de "scroll infinito con huecos" | La ficha pide "paginación server-side" + "virtualización" a la vez; con `tamanoDePagina` grande (p. ej. 500-1000) el CDK evita que el DOM tenga esa cantidad de filas montadas, y el servidor sigue paginando el total. Es una interpretación razonable de dos requisitos que, tomados literalmente juntos ("paginar" y "virtualizar todo"), son parcialmente redundantes | Quien la consuma en F7/F8 puede pedir el modo "ventana con huecos" si un caso concreto lo necesita |

## Huecos encontrados, y de quién son

| Hueco | De quién es | Por qué no se resolvió acá |
| --- | --- | --- |
| `LineaDeTiempo` (organismo que la ficha F6 pide para componer `PanelDeEvidencia`) no existe en `packages/ui/src/` | `F1-W` (o quien suceda a ese carril) | `packages/ui` es de solo lectura para este carril. `PanelDeEvidencia` se entregó igual, con una cronología semántica propia (`<ol>`) sobre los tokens del sistema, para no bloquear a `F7`/`F8`; cuando `LineaDeTiempo` exista, es un cambio interno de `panel-de-evidencia.ts` |
| **Acceso administrativo de ADR-038** (ingreso, desafío TOTP, enrolamiento, recuperación asistida) | Este carril (F6), no resuelto por falta de tiempo | Es la pieza más grande que quedó afuera. Tocar autenticación de verdad (TOTP, recuperación) sin poder validar contra `identidad` real ni contra el ADR completo en el tiempo disponible se evaluó como más riesgoso que declararlo hueco — es justamente el tipo de superficie («seguridad») donde la instrucción pide no inventar bajo presión de tiempo |
| `X-Robots-Tag` y el `meta noindex` **no se verificaron con `curl` contra un contenedor NGINX real** | Este carril | Se verificó el `meta` en el `index.html` compilado y el `add_header` en `apps/backoffice/docker/nginx.conf` (heredado de F0-B, no tocado), pero no se corrió `docker build` + contenedor + `curl` por el tiempo disponible en esta sesión |
| Catálogo real de permisos (`ver:operacion`, etc.) | `identidad`/quien defina el modelo de roles del backoffice | Ver supuesto declarado arriba |
| Presupuesto de bundle inicial (300 kB) superado por ~20 kB | Este carril, no resuelto | El excedente es de Angular Router + CDK scrolling + los componentes de `@aportaya/ui` que usa el shell. No se investigó `lazy`-cargar el propio `ShellFinanciero` ni dividir `RouterModule` por falta de tiempo; queda para quien cierre el gate de rendimiento en F7 |
| Prueba con **100 000 filas reales en un navegador** (Playwright, con medición de que el hilo principal no se traba) | Este carril | Se probó el mecanismo (paginación + virtualización) con `vitest`/`jsdom` sobre un cargador de 100 000 filas en memoria (`tabla-de-datos-virtualizada.spec.ts`), verificando que solo se pide la página actual y que cambiar de página vuelve a pedir al "servidor" — pero no hay medición de *frame budget* en un navegador real, porque esta sesión no ejecutó Playwright |

## Bloqueos

Ninguno bloqueante para lo entregado. El acceso administrativo (ADR-038) y el catálogo
de permisos real son las dos piezas que, si se retoman, conviene coordinarlas con
`identidad` antes de escribir código, no adivinar de nuevo la forma del contrato.

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | :-: |
| Router | Enchufe por dominio, `canMatch` por permiso | `app.routes.ts` + `nucleo/permisos.spec.ts` (2/2) | ✅ |
| Router | Ruta nueva en un dominio no toca `app.routes.ts`/`nucleo`/`layout` | `enchufe-de-rutas.spec.ts` (2/2, ya existía, sigue en verde) | ✅ |
| Tabla | Dataset grande paginado del servidor, sin bloquear | `tabla-de-datos-virtualizada.spec.ts`: 100 000 filas simuladas, solo se piden 50 por vez (6/6) | ✅ (mecanismo); ⬜ (medición en navegador real, ver huecos) |
| Tabla | Navegación completa por teclado, fila a fila | `navegacion-por-teclado.spec.ts` (5/5) + `onKeydown` probado en el componente | ✅ |
| Tabla | Orden fuera de la lista blanca se rechaza visible, no se ignora | `tabla-de-datos-virtualizada.spec.ts` (caso `idSecreto`) | ✅ |
| Filtros | Estado en la URL, compartible | `barra-de-filtros.spec.ts` (2/2): `?filtro=`, `?q=`, resetea `pagina` | ✅ |
| Filtros | Sobrevive a "recargar"/pegar la URL | `pantalla-de-estado.spec.ts` (URL con `?filtro=N1` filtra al crear el arnés de ruta de cero) | ✅ |
| Exportación | Respeta el permiso: sin él, no aparece el botón | `pantalla-de-estado.spec.ts` (2/3) | ✅ |
| Exportación | Nunca se arma el archivo en el cliente | `Exportador.solicitar()` solo hace `POST` al gateway; sin `Blob`/`CSV.stringify` en el repo (`grep` manual) | ✅ |
| Acceso a datos | Se registra cuando la pantalla lo marca | `registro-de-acceso.interceptor.ts` + `ACCESO_A_DATOS`; **sin prueba de interceptor dedicada** (falta) | ⬜ |
| Diseño | Cero literales, piezas de `@aportaya/ui` | `lint` (`sin literal de diseño`) | ✅ |
| Accesibilidad | axe sin violaciones serias | `test:a11y` (4/4) | ✅ |
| Arquitectura | Ningún archivo de más de 200 líneas · sin red en componentes | `lint` (`verificar_frontend.py backoffice`) | ✅ |
| Entrega | Lint, tipos, pruebas, build | ver comandos arriba | ✅ |
| noindex | `meta` + `X-Robots-Tag` | verificado en `index.html` y `nginx.conf`; no verificado contra contenedor real | ✅ (parcial) |
| `dist/` | Sin clientes de servicios no usados por la ruta cargada | Chunks separados por pantalla en el build (`pantalla-de-estado`, `pantalla-de-billetera`, `tablero`, y un chunk de ~40-400 B por cada `*-routes` vacío); no se hizo un análisis exhaustivo de qué símbolos entran a cada chunk | ✅ (razonable, no exhaustivo) |
| Acceso administrativo (ADR-038) | — | no implementado | ⬜ |

### Frases prohibidas sin evidencia

No se declara nada de esto como «listo» sin la corrida de arriba. Lo que falta está en
la fila correspondiente de la matriz y en «Huecos», no escondido en prosa.

## Qué queda abierto

1. **ADR-038** (acceso administrativo: TOTP, enrolamiento, recuperación asistida) —
   sin empezar.
2. Prueba de rendimiento de la tabla en un navegador real (Playwright) con 100 000 filas
   y medición de *frame budget* — el mecanismo está probado con `vitest`, no la
   experiencia real de scroll.
3. `docker build` + `curl` contra el NGINX real para cerrar el gate de `noindex` con
   evidencia de contenedor, no solo de archivo fuente.
4. Prueba dedicada del interceptor `registroDeAccesoInterceptor` (hoy solo se verifica
   por lectura de código y por el uso que hace `pantalla-de-estado`, que no marca
   ningún `ACCESO_A_DATOS` porque no muestra datos personales).
5. Catálogo de permisos real, coordinado con `identidad`, para reemplazar la
   convención `<verbo>:<recurso>` de este carril.
6. Ajustar el presupuesto de bundle inicial (300 kB), hoy en ~320 kB.
