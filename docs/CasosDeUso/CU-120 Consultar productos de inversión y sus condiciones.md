---
tags:
  - caso-uso
  - modulo/15-inversiones-voluntarias
codigo: CU-120
criticidad: media
actores: [Usuario, Operaciones, Sistema]
normas: [Consentimiento versionado, Prohibido inferir parámetros financieros]
---

# CU-120 — Consultar productos de inversión y sus condiciones

> **Objetivo.** Que quien quiera invertir vea qué se le ofrece, de quién, con qué
> condiciones y **de cuándo es el dato**, sin ninguna promesa de rentabilidad; y que
> el catálogo del aliado se sincronice sin pisar jamás lo que alguien ya aceptó.

> [!warning] Dos límites que valen para todo el módulo 15 (CU-120 a CU-125)
> 1. **La inversión de dinero de grupos está DESACTIVADA.** Un saldo afectado a un
>    aporte o a un pozo no se invierte (se hace cumplir en [[CU-121 Aceptar las condiciones y ordenar una inversión]]).
>    Esta función es una inversión **voluntaria del titular** con su propio saldo libre.
> 2. **Todo parámetro comercial o fiscal es SINTÉTICO** (tasas, plazos, base de días,
>    mínimos, costos, retención, comisión de éxito, riesgo, titularidad) hasta que
>    exista una decisión de negocio con fuente. Un dato sintético sale marcado
>    `origenDatos: SINTETICO` y `aptoProduccion: false`, y nunca se ofrece en
>    producción. Las decisiones abiertas están en el `README` del servicio
>    (`servicios/inversiones`) y en `evidencia/carril-D/ADR-inversiones.md`.

## Actores y disparador

- **Actor principal:** el usuario titular, que consulta el catálogo (`BILLETERA_VER`).
- **Actor secundario:** Operaciones, que dispara la sincronización contra el aliado
  (`ADMIN_PLATAFORMA`). No hay hoy un trabajo programado que lo haga solo.
- **Tercero externo:** el aliado (banco o SAFI), visto únicamente por el puerto
  `AliadoDeInversion`. Qué aliado, con qué facultades y quién contrata con el cliente
  es una **decisión abierta (DR-INV-01)**. Hoy existe un solo adaptador y es **simulado**.
- **Disparador:** el usuario abre el catálogo (`GET /inversiones/productos`) u
  Operaciones pide `POST /inversiones/productos/sincronizacion`.

## Precondiciones

1. Sesión válida; el servicio no confía en el gateway: valida el token y el permiso.
2. Para sincronizar, el aliado está habilitado en este entorno
   (`aportaya.inversiones.aliado.modo`). En entornos productivos el único modo que
   arranca es `deshabilitado`: `GuardiaDeProduccion` corta el arranque si se configura
   el simulador, así que ahí no hay catálogo que sincronizar.
3. La respuesta del aliado se acepta solo con **firma HMAC y frescura verificadas**;
   una respuesta que no se puede verificar no confirma nada.

## Flujo principal

**Consultar**

1. Se leen los productos `ACTIVO` y, de cada uno, su **versión de condiciones vigente**
   (la de número más alto).
2. Si el entorno es productivo (`aportaya.entorno.productivo`, por omisión `true`: se
   falla cerrado), se descarta toda versión que no sea `aptoProduccion`. Como hoy todo
   dato es sintético, en producción la lista sale vacía.
3. Cada producto se devuelve con: código, tipo (`DPF` o `FONDO`), nombre, emisor,
   moneda, nivel de riesgo, titularidad y sus condiciones: versión (id y número),
   **texto completo y su hash SHA-256**, fuente, **fecha de cotización**, `origenDatos`,
   `aptoProduccion`, monto mínimo y costos; en un DPF, plazo, base de días, tasa
   nominal anual, retención y política de cancelación anticipada; en un fondo, días de
   rescate, hora de corte (hora de La Paz) y comisión por resultado si la tiene. Nunca
   se informa una rentabilidad: el rendimiento de un fondo puede ser negativo y el de
   un DPF es el que fija su contrato.
4. La respuesta incluye la advertencia: invertir puede generar pérdidas, el
   rendimiento no está garantizado ni por Pasanaku ni por el aliado, y la garantía del
   pozo de un grupo **no** alcanza a esta inversión.

**Sincronizar**

1. Operaciones pide la sincronización. El efecto es idempotente **por contenido** (la
   cabecera `Idempotency-Key` el contrato la exige, pero ninguna operación de
   sincronización la usa para decidir nada).
2. Se pide el catálogo al aliado. Si no responde, devuelve error o la respuesta no se
   puede verificar: **AP-CU120-02** y no cambia nada.
3. Por cada producto, en una transacción propia: se asegura el producto (alta si no
   existía, con emisor `Aliado de demostración (SINTETICO)` y riesgo `BAJO` para un DPF
   y `MEDIO` para un fondo, ambos supuestos sintéticos), se bloquea la fila y se
   redacta el texto de condiciones de forma determinista.
4. Si el hash del texto nuevo es **igual** al de la versión vigente, no se inserta
   nada. Si cambió, se inserta una versión nueva (`numero + 1`); las anteriores no se
   tocan, porque son lo que alguien ya aceptó.
5. En la misma transacción se escribe en el outbox `inversiones.condiciones_publicadas`.
6. Se responde cuántas versiones eran nuevas.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 2a | El aliado está caído, deshabilitado o su respuesta no se puede verificar | `AP-CU120-02`; no se crea ni se modifica nada |
| 3a | Entorno productivo y producto sintético | No se ofrece: la lista excluye toda versión no apta |
| 4a | El aliado publica las mismas condiciones | No se inserta nada: sincronizar dos veces no duplica versiones |
| 4b | El aliado publica condiciones distintas | Nace una versión **nueva**; la anterior queda intacta |
| 4c | Un dato sintético marcado apto para producción, o un fondo con tasa fija, o un DPF sin plazo | La base lo rechaza (`R-INV-01`) |
| — | Sin sesión o sin permiso | 401 sin token, 403 con rol insuficiente |

## Postcondiciones

- Quien consulta ve la versión vigente con su fecha y su fuente, y sabe si el dato es
  sintético.
- Una versión de condiciones publicada es inmutable: lo que aceptó una persona se
  puede reconstruir byte a byte con su hash.
- Ningún dato sintético se ofrece en producción.

## Contrato · `openapi/inversiones.yaml`

```ts
export const EntradaCU120 = z.object({
  claveIdempotencia: z.string().uuid(), // exigida por el contrato; la sincronización es idempotente por contenido
}).strict()

export const SalidaCU120 = z.object({
  productos: z.number().int(),
  nuevos: z.number().int(), // versiones de condiciones que no existían
}).strict()

export const ErroresCU120 = {
  PRODUCTO_INEXISTENTE: 'AP-CU120-01',
  ALIADO_NO_DISPONIBLE: 'AP-CU120-02',
  RESPUESTA_DEL_ALIADO_NO_VERIFICABLE: 'AP-CU120-03',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `PRODUCTO_INEXISTENTE` | Declarado en el contrato; hoy ningún camino de CU-120 lo emite (un producto inexistente se reporta al ordenar, ver CU-121) |
| `ALIADO_NO_DISPONIBLE` | El aliado no respondió, está deshabilitado o su respuesta no se pudo verificar: no se sabe qué hay, y no se inventa |
| `RESPUESTA_DEL_ALIADO_NO_VERIFICABLE` | Declarado en el contrato; hoy una respuesta sin firma válida se trata como aliado no disponible |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `TextoDeCondiciones` | Redacta el texto de condiciones de forma determinista y calcula su hash |
| Átomo | `Condiciones` | Condiciones inmutables; un dato sintético no puede ser apto para producción |
| Molécula | `CatalogoRepositorio` | Productos y versiones de condiciones, con bloqueo y numeración |
| Molécula | `AliadoSimuladoHttp` | Adaptador firmado del puerto `AliadoDeInversion`: timeout, circuit breaker y firma |
| Molécula | `GuardiaDeProduccion` | Impide arrancar un proceso productivo con el aliado simulado |
| Organismo | `CU120Catalogo` | Sincroniza y lista; una transacción por producto |
| Página | `GET /inversiones/productos` · `POST /inversiones/productos/sincronizacion` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `inversiones.condiciones_publicadas` | Nada todavía (es un hecho publicado para quien lo consuma) | `BILLETERA_VER` para listar · `ADMIN_PLATAFORMA` para sincronizar |

No hay trabajo programado: la sincronización la dispara Operaciones.

## Interfaz

- **App:** pantalla del catálogo de inversión (pendiente de especificar en el
  frontend). El contrato ya entrega lo que debe mostrar: fecha de cotización, fuente,
  `origenDatos` y la advertencia, diferenciando DPF de fondo.
- **Backoffice:** acción de Operaciones «Sincronizar catálogo» (pendiente de
  especificar).

## Restricciones aplicables

`R-INV-01`

## Evidencia que deja

`producto_inversion` · `version_condiciones` · `evento_dominio` (outbox del esquema
`inversiones`). El módulo 15 todavía no tiene notas de entidad en `docs/Modelos`: su
modelo vive en `docs/entidades/15_inversiones.puml`.

## Criterios de aceptación

```gherkin
Dado un DPF y un fondo del aliado
Cuando se consulta el catalogo
Entonces identifica riesgo, plazo, costos, titularidad, fuente y fecha de cotizacion

Dado un producto cuyos parametros no tienen fuente aprobada
Cuando se sincroniza el catalogo
Entonces sale marcado SINTETICO y no apto para produccion

Dado un entorno productivo y un producto sintetico
Cuando se consulta el catalogo
Entonces no se ofrece ningun producto sintetico

Dado un producto con condiciones vigentes
Cuando el aliado publica condiciones distintas
Entonces nace una version NUEVA y la anterior queda intacta

Dado el aliado caido
Cuando se sincroniza el catalogo
Entonces falla con AP-CU120-02 y no cambia nada
```

Además, las pruebas de reintento (sincronizar dos veces no inserta nada) y de rechazo
de la base por `R-INV-01` (dato sintético marcado apto para producción; fondo con tasa
fija o DPF sin plazo).

## Ver también

[[CU-121 Aceptar las condiciones y ordenar una inversión]] · [[CU-122 Confirmar la posición sin duplicar el saldo]] · [[CU-123 Devengar y valorar una posición de inversión]] · [[CU-124 Solicitar el rescate de una inversión]] · [[CU-125 Liquidar el rescate y acreditar al titular]]
