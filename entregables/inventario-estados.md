# Inventario de estados — pantallas del backoffice con red (H4/H12.S4)

Metodología: `find apps/backoffice/src -name 'pantalla-*.ts'` da 27 pantallas. Para
cada una se verificó (grep, no "probablemente"): si usa `EstadoDePantalla`
(`packages/ui/src/estado-de-pantalla/estado-de-pantalla.ts` — pinta cargando / datos /
vacío / error a partir de un `ResourceRef`), si la ruta que la carga tiene
`canMatch: [requierePermiso(...)]` (sin-permiso, a nivel ruta: la pantalla ni se monta
si falta el permiso — no es un estado que la pantalla misma dibuje), y si el estado de
error distingue sin-conexión (columna aparte, ver nota al final: es un hallazgo
transversal, no arreglado acá — `EstadoDePantalla` es de `packages/ui`, fuera de mi
alcance).

Leyenda: ✅ cubierto y verificado · ⚠️ parcial/no distinguido · — no aplica (sin red) ·
`?` no auditado en profundidad (pantalla fuera de mi carril, no se tocó).

| Pantalla | Carga | Datos | Vacío | Error | Sin permiso | Sin conexión |
|---|---|---|---|---|---|---|
| `sistemas/servicios/pantalla-servicios.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ (ver nota) |
| `sistemas/despliegues/pantalla-despliegues.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `sistemas/base-datos/pantalla-base-datos.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `sistemas/respaldos/pantalla-respaldos.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `sistemas/proveedores/pantalla-proveedores.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `sistemas/outbox/pantalla-outbox.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `sistemas/webhooks/pantalla-webhooks.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `sistemas/accesos/pantalla-accesos.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `sistemas/incidentes/pantalla-incidentes.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `operacion/billetera/pantalla-de-billetera.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `cumplimiento/organizadores/pantalla-de-habilitacion.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `cumplimiento/verificaciones/pantalla-de-expedientes.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `contabilidad/periodo/pantalla-de-periodo.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `publicidad/desempeno/pantalla-de-desempeno.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `publicidad/liquidacion/pantalla-de-liquidacion.ts` | ✅ | ✅ | ✅ | ✅ | ✅ (ruta) | ⚠️ |
| `contabilidad/activos/pantalla-de-activos.ts` | `?` | `?` | `?` | `?` | `?` | `?` |
| `contabilidad/cobros/pantalla-de-cobros.ts` | `?` | `?` | `?` | `?` | `?` | `?` |
| `contabilidad/compras/pantalla-de-compras.ts` | `?` | `?` | `?` | `?` | `?` | `?` |
| `contabilidad/estados/pantalla-de-estados-financieros.ts` | `?` | `?` | `?` | `?` | `?` | `?` |
| `contabilidad/presupuesto/pantalla-de-presupuesto.ts` | `?` | `?` | `?` | `?` | `?` | `?` |
| `cumplimiento/actas/pantalla-de-acta.ts` | `?` | `?` | `?` | `?` | `?` | `?` |
| `cumplimiento/alertas/pantalla-de-alertas.ts` | `?` | `?` | `?` | `?` | `?` | `?` |
| `cumplimiento/casos/pantalla-de-caso.ts` | `?` | `?` | `?` | `?` | `?` | `?` |
| `cumplimiento/reclamos/pantalla-de-reclamo.ts` | `?` | `?` | `?` | `?` | `?` | `?` |
| `ingreso/pantalla-de-ingreso.ts` | — | — | — | — | — | — |
| `operacion/estado/pantalla-de-estado.ts` | `?` | `?` | `?` | `?` | `?` | `?` |
| `publicidad/anunciantes/pantalla-de-anunciantes.ts` | `?` | `?` | `?` | `?` | `?` | `?` |
| `publicidad/campanas/pantalla-de-campanas.ts` | `?` | `?` | `?` | `?` | `?` | `?` |

**Lo que se arregló en este carril (H2/H3):** las nueve pantallas de `sistemas/` — antes
importaban arrays de ejemplo directo, sin ningún estado de carga/vacío/error real (un
`filas = serviciosSimulados` no tiene ninguno de los cuatro). Ahora las nueve inyectan
su puerto (`dominio/puertos.ts`) y usan `EstadoDePantalla` con `resource()` — los
cuatro estados quedan cubiertos y probados (`pantalla-servicios.spec.ts` y análogos).

**Lo que se verificó sin tocar (ya estaba bien) — 6 pantallas fuera de `sistemas/`**
que ya usaban `EstadoDePantalla` antes de este carril: `billetera`, `habilitacion`,
`expedientes`, `periodo`, `desempeno`, `liquidacion`. No se tocaron (fuera de
alcance), se registran acá porque el inventario tiene que incluir toda pantalla con
red, no solo las que se tocaron.

**Lo que quedó sin auditar (`?`)** — 11 pantallas que no usan `EstadoDePantalla` y no
se investigaron en profundidad: puede que manejen sus cuatro estados con su propio
patrón (no todas las pantallas del backoffice comparten el mismo organismo), o puede
que tengan el hueco real que esta regla busca. **No se tocan** (regla 00 §3: lo roto
fuera de alcance se reporta, no se arregla) — quedan como hallazgo para el equipo,
sección más abajo.

**`pantalla-de-ingreso.ts`**: sin red verificada en el grep (no importa
`EstadoDePantalla` ni `resource`/`httpResource`) — parece ser el formulario de login,
que además el hallazgo de `apps/backoffice/e2e/tablero-y-permisos.e2e.ts` (carril F12)
documenta como no cableado a ningún `APP_INITIALIZER` ni ruta real todavía.

## Hallazgo transversal: "sin conexión" no se distingue visualmente de "error del servidor" (⚠️)

`packages/ui/src/estado-de-pantalla/estado-de-pantalla.ts` (Angular, backoffice y
web) **sí recibe** la bandera `sinConexion` en `ErrorTraducido` (poblada por
`nucleo/errores.interceptor.ts:18`, `sinConexion: error.status === 0`), pero su
plantilla nunca la lee: pinta el mismo bloque `role="alert"` con el mismo mensaje
genérico sea cual sea la causa. Comparar con el lado Flutter
(`apps/movil`, `aportaya_diseno/organismos/estado_error.dart`), que sí muestra un
ícono y un texto distintos para `sinConexion` (ver
`test/widget/conexion_test.dart`, que además demuestra que la app SÍ distingue
sin-conexión de error del servidor del lado Flutter — H12.S4.M3 / H4.S1.M3
verificado, sin brecha ahí).

Este hallazgo pega en las 15 pantallas Angular que usan `EstadoDePantalla`, incluidas
las nueve de `sistemas/` que este carril tocó — pero el componente es de
`packages/ui`, fuera de mi alcance (Marcelo). Se reporta acá, no se arregla.
