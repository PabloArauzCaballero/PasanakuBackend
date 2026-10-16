import { Routes } from '@angular/router'
import { requierePermiso } from '../../nucleo/permisos'

/**
 * Las rutas del dominio operación. **Este archivo lo posee el carril B1**; el shell
 * solo lo carga por `loadChildren`. Agregar una pantalla es agregar una carpeta acá al
 * lado y una entrada en esta lista: nada de `app.routes.ts`, `nucleo/` ni `layout/` cambia.
 *
 * `requierePermiso` (de `nucleo/permisos.ts`, del shell) es la segregación de funciones
 * del lado del cliente: monta solo la ruta que el permiso del operador habilita, y
 * redirige al tablero si no — nunca una pantalla vacía sin explicación. El 403 real del
 * backend respalda esto, nunca al revés — cada pantalla vuelve a chequear el permiso
 * antes de montar sus acciones (ver `puedeAtender` / `puedeResolver` en cada componente),
 * porque el guard evita la navegación pero no protege un enlace pegado directo si el
 * permiso cambió después de montar.
 */
export const rutasOperacion: Routes = [
  // H12.S1.M1 · antes redirigía a la billetera de una cuenta FIJA (un UUID de ejemplo) a cualquier operador que
  // entrara a la sección: la puerta de entrada de operación es la pantalla que no depende de ninguna cuenta ni
  // permiso fino. La billetera de un tercero se abre por enlace, con su cuenta, y la gobierna su permiso.
  { path: '', pathMatch: 'full', redirectTo: 'estado' },
  {
    path: 'billetera/:cuentaId',
    loadComponent: () => import('./billetera/pantalla-de-billetera').then((m) => m.PantallaDeBilletera),
    title: 'Billetera · AportaYa',
    // H12.S1.M1 · ver la billetera de OTRA persona exige su permiso (`BILLETERA_VER_TERCEROS`, con segundo factor):
    // pertenecer a la sección no alcanza (quien solo atiende reclamos no mira saldos ajenos). El servidor vuelve a decidir.
    canMatch: [requierePermiso('BILLETERA_VER_TERCEROS')],
  },
  {
    path: 'reclamos',
    loadComponent: () => import('./reclamos/bandeja-de-reclamos').then((m) => m.BandejaDeReclamos),
    title: 'Reclamos · AportaYa',
    canMatch: [requierePermiso('RECLAMO_VER')],
  },
  {
    path: 'solicitudes-escaladas',
    loadComponent: () => import('./solicitudes-escaladas/cola-de-solicitudes-escaladas').then((m) => m.ColaDeSolicitudesEscaladas),
    title: 'Solicitudes escaladas · AportaYa',
    canMatch: [requierePermiso('SOLICITUD_INGRESO_VER')],
  },
  {
    // CU-11 · bandeja `listarRetirosEnRevision` (exige RETIRO_APROBAR, el mismo permiso que `aprobarRetiro`).
    path: 'retiros',
    pathMatch: 'full',
    loadComponent: () => import('./retiros/bandeja-de-retiros').then((m) => m.BandejaDeRetiros),
    title: 'Retiros en revisión · AportaYa',
    canMatch: [requierePermiso('RETIRO_APROBAR')],
  },
  {
    // CU-11 · segunda firma de una orden (fila de la bandeja o enlace profundo por id).
    path: 'retiros/:ordenId',
    loadComponent: () => import('./retiros/pantalla-de-aprobacion-de-retiro').then((m) => m.PantallaDeAprobacionDeRetiro),
    title: 'Aprobación de retiros · AportaYa',
    canMatch: [requierePermiso('RETIRO_APROBAR')],
  },
  {
    // CU-50 · `listarConciliaciones` exige AUDITORIA_LEER; registrar (`conciliarCustodia`) exige el rol TESORERIA y la
    // pantalla lo vuelve a chequear antes de montar el formulario.
    path: 'custodia',
    loadComponent: () => import('./custodia/pantalla-de-conciliaciones').then((m) => m.PantallaDeConciliaciones),
    title: 'Conciliación de custodia · AportaYa',
    canMatch: [requierePermiso('AUDITORIA_LEER')],
  },
  // `estado/` es de F6 (ficha F6, estructura congelada): la ficha se lo asigna al shell
  // aunque viva dentro del dominio `operacion` que B1 posee. Es la única entrada de esta
  // lista que F6 agrega; el resto de `operacion.routes.ts` lo llena B1.
  {
    path: 'estado',
    loadComponent: () => import('./estado/pantalla-de-estado').then((m) => m.PantallaDeEstado),
    title: 'Arquitectura y estado · AportaYa',
  },
]
