# Guía común de franja — backoffice AportaYa (2026-10-26)

Leela entera antes de tocar nada. El plan raíz es `PLAN.md` de esta carpeta (en `/home/user/PasanakuFrontend/docs/trabajo/2026-10-26-backoffice-listados-y-navegacion/`).

## Entorno (ya preparado)

- Tu worktree: `/home/user/wt-<letra>`, rama `claude/franja-<letra>`, que parte del commit `c9ac3966` (la base H0). Trabajá **solo** ahí; nunca en `/home/user/PasanakuFrontend` ni en otro `wt-*`.
- Node: `export PATH=/opt/node24/bin:$PATH` en **cada** comando (Angular CLI exige ≥ 22.22.3; el `node` por omisión es 22.22.0 y falla).
- Dependencias, tokens (`packages/tokens/generado`), clientes OpenAPI (`clientes/angular`, ignorados por git) y el simulado ya están en tu worktree. No corras `gradlew`.
- **El simulado OpenAPI ya corre en `127.0.0.1:4010`** y es compartido: no levantes otro. Es *sin estado* (Prism/Scalar): un POST/PUT/DELETE responde el ejemplo del contrato pero **no persiste**. Toda mutación verificada contra él se declara «verificada contra el doble» (regla 65), nunca «persistida».
- Tu servidor de desarrollo: `cd /home/user/wt-<letra> && export PATH=/opt/node24/bin:$PATH && yarn workspace @aportaya/backoffice start --port <puerto> --host 127.0.0.1 --proxy-config proxy.e2e.json` en segundo plano. Puertos: a=4301, b=4302, c=4303, d=4304, e=4305, f=4306. Bajalo al terminar.
- Navegador: Chromium y Playwright están instalados (`import { chromium } from 'playwright'` desde `apps/backoffice`). **Un navegador por agente**, nunca `waitForTimeout` ni `networkidle`: esperá por condición.
- Sesión en el navegador: copiá el ingreso de `apps/backoffice/e2e/apoyo/sonda-de-navegacion.mjs` (doble de sesión: intercepta el 2.º `POST /sesiones` y sirve un token con permisos). Los permisos de todas las secciones están en `/tmp/claude-0/-home-user/bfffe767-c9ff-52f5-8524-aca65b5504e0/scratchpad/permisos.txt`. Para **Sistemas** el token tiene que traer `rol: 'PLATAFORMA'` (lo exige `rutas/sistemas/guardia-rol-sistemas.ts`). La sesión vive en memoria: navegá con clics o `history.pushState` + `popstate`, no con `page.goto` después de entrar.
- Tus scripts y capturas temporales: `/tmp/claude-0/-home-user/bfffe767-c9ff-52f5-8524-aca65b5504e0/scratchpad/franja-<letra>/`. Las capturas que sean evidencia van a `docs/trabajo/2026-10-26-backoffice-listados-y-navegacion/evidencia/franja-<letra>/` de **tu** worktree.

## La disciplina que pidió Pablo, en cada listado

Todo listado (tabla, bandeja, cola, lista de tarjetas que es una colección) usa el kit de `@aportaya/ui`:

```ts
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos, type CargaDeListado } from '@aportaya/ui/listado/listado'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'

readonly filas = computed(() => (this.estado().tipo === 'lista' ? this.estado().filas : []))
readonly listado = crearListado<Retiro>({
  filas: this.filas,
  camposDeBusqueda: ['codigo', 'titular', (r) => r.estado],   // buscador multicampo
  filtros: [{ nombre: 'estado', etiqueta: 'Estado', valor: (r) => r.estado, texto: (v) => ETIQUETAS[v] }],
  porPagina: 10,                                              // opciones 10/20/50/100 por omisión
})
```

```html
<ap-listado titulo="Retiros en revisión" [columnas]="columnas" [listado]="listado" [carga]="carga()"
            ayudaDeBusqueda="Busca por código, titular o estado." [exportables]="exportables" (reintentar)="recargar()">
  <ap-boton accionesDeListado icono="mas" variante="primario" (pulsado)="nuevo()">Nuevo retiro</ap-boton>
  <ng-template #celda let-r let-c="columna">…celdas con formato (ap-monto, ap-fecha, ap-chip-estado)…</ng-template>
  <ng-template #acciones let-r>
    <ap-acciones-de-fila [fila]="r.codigo" [ver]="true" [editar]="puedeEditar()" [eliminar]="puedeEliminar()" [ocupada]="eliminando() === r.id"
                         (verPulsado)="ver(r)" (editarPulsado)="editar(r)" (eliminarConfirmado)="eliminar(r)" />
  </ng-template>
</ap-listado>
```

Lo que da el kit: buscador multicampo sin acentos, un selector por filtro con «Todos» y «Limpiar filtros», «Descargar CSV / JSON» de **todo lo filtrado** (con el conteo), tabla con orden, paginación (filas por página, «Ir a» la página N, Anterior/Siguiente) y los estados cargando / error con reintento e ID de petición / vacío sin datos / vacío por filtro. Ver `packages/ui/src/listado/*.ts` y su spec.

- **Listado paginado en el servidor** (cursor o `pagina`/`limite` en el contrato): no lo bajes entero para paginar en el navegador. Mandá búsqueda y filtros en la consulta si el contrato los acepta, y componé `ap-barra-de-filtros` + `ap-tabla-de-datos` + `ap-paginacion` + `ap-exportar-listado` a mano con la misma anatomía. Si el contrato no acepta búsqueda, filtrá la página cargada y decilo en la ayuda del buscador.
- **Acciones de fila** = `ap-acciones-de-fila`, siempre ícono + nombre. **Editar y Eliminar solo si el contrato tiene la operación** (buscá en `servicios/*/src/main/resources/openapi/*.yaml` o `packages/simulado/generado/prism/todos.yaml`). Nunca Eliminar sobre asientos, movimientos, bitácoras, reportes enviados a la UIF ni nada append-only (regla 91.1.3). Una bandeja de trabajo conserva su acción de dominio («Revisar», «Aprobar», «Responder») proyectada dentro de `ap-acciones-de-fila` con `<button apAccionDeFila>` + `ap-icono`. Un botón que no hace nada es exactamente la queja: o tiene endpoint y se prueba, o no se dibuja.
- **Íconos en todo**: cada botón lleva ícono + texto (`<ap-boton icono="guardar">Guardar</ap-boton>`), cada cabecera de página su `icono`, cada tarjeta KPI su ícono. Íconos disponibles: `packages/ui/src/icono/icono.ts` (≈ 100). Si falta uno, usá el más cercano y anotalo en tu informe; **no edites `packages/ui`**.
- **Layout**: `ap-cabecera-de-pagina` con ícono, eyebrow y descripción; cifras arriba (`ap-tarjeta-kpi`) cuando la vista las tiene; el listado a todo el ancho; formularios y fichas en una tarjeta, centrados, sin columna vacía. Tokens siempre (`var(--s4)`, `var(--r-lg)`…), nunca literales. Funciona de 375 px a 1440 px y en tema claro y oscuro.

## Lo que no se toca

- `packages/ui/**`, `apps/backoffice/src/app/nucleo/**`, `apps/backoffice/src/app/layout/**`, `app.routes.ts`, `app.config.ts`, **todos los `*.routes.ts`** (son de la franja A) y los directorios de otras franjas. Si necesitás un cambio ahí, escribilo en la sección «Pedidos a otras franjas» de tu informe.
- Contratos, backend, `apps/web`, `apps/movil`.
- No borres ni debilites pruebas. Un spec que se rompe porque cambió la anatomía se actualiza para afirmar **el mismo requisito** con la anatomía nueva; si el requisito cambió, decilo.

## Reglas del repo que el lint hace cumplir

- `python3 scripts/verificar_frontend.py backoffice`: archivo de pantalla/componente ≤ 200 líneas (partí en componentes), sin literales de diseño, sin `HttpClient`/`fetch` fuera de `dominio/` y `nucleo/`, sin `console.*`.
- Señales, `OnPush`, componentes standalone; comentarios en español rioplatense, con la densidad de los archivos vecinos. Antes de escribir, mirá 2–3 vecinos del mismo tipo y copiá su forma.

## Definition of Done de cada vista (los tres niveles)

1. **Aceptado** — con datos del simulado: se ve la lista; buscar por cada campo declarado encuentra; cada filtro filtra; cambiar filas por página, «Ir a», Anterior y Siguiente funcionan; CSV y JSON bajan lo filtrado (verificá el contenido del archivo descargado con `page.waitForEvent('download')`); cada botón de la vista hace lo que dice (navega, abre diálogo, dispara la petición correcta — mirá la request en la red).
2. **Límite** — vacío sin datos (simulá con `page.route` devolviendo la colección vacía), búsqueda sin coincidencias → «Quitar filtros», una sola fila, última página parcial, textos largos sin romper el layout a 375 px.
3. **Error** — 500 y 403 simulados con `page.route`: mensaje accionable, «Volver a intentar» repite la petición, nada de error crudo; una mutación que falla conserva lo escrito y no deja el botón trabado.

Por cada vista: spec unitario de la vista (los tres niveles, contra dobles de dominio como hacen los specs vecinos) + recorrido en navegador + capturas a 1280 / 768 / 375 en claro, y 1280 en oscuro (`colorScheme: 'dark'`). **Mirá cada captura** (Read sobre el PNG) y corregí lo que veas.

Comandos de cierre (pegá la salida literal en tu informe):

```bash
cd /home/user/wt-<letra>/apps/backoffice && export PATH=/opt/node24/bin:$PATH
npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit
npx ng lint && python3 ../../scripts/verificar_frontend.py backoffice
npx ng test --include='src/app/rutas/<tu-seccion>/**/*.spec.ts' --exclude='src/**/*.a11y.spec.ts'
npx ng test --include='src/app/rutas/<tu-seccion>/**/*.a11y.spec.ts'
```

## Entrega

1. Commits chicos en tu rama, uno por vista o por grupo coherente, con mensaje en español y el pie:
   ```
   Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
   Claude-Session: https://claude.ai/code/session_01T6SZ7JDzrktGwq6ChgXChm
   ```
   No hagas push, no abras PR, no hagas merge: eso lo integra el agente principal.
2. `docs/trabajo/2026-10-26-backoffice-listados-y-navegacion/franjas/<letra>.md` en tu worktree: microtareas (ID `H<n>.S<k>.M<j>`, CA binario, DoD con comando, estado de los seis permitidos), la evidencia (salidas literales recortadas, rutas de capturas), «No cubierto», «Pedidos a otras franjas» y «Hallazgos fuera de alcance».
3. Tu respuesta final al agente principal: tabla vista → qué cambió → aceptado/límite/error (PASS/FAIL/BLOCKED) → evidencia; lo que quedó A MEDIAS con qué falta exactamente; hash del último commit. Sin adornos: si algo no se verificó, decilo.
