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
  { path: '', pathMatch: 'full', redirectTo: 'billetera/11111111-1111-4111-8111-111111111111' },
  {
    path: 'billetera/:cuentaId',
    loadComponent: () => import('./billetera/pantalla-de-billetera').then((m) => m.PantallaDeBilletera),
    title: 'Billetera · AportaYa',
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
  // `estado/` es de F6 (ficha F6, estructura congelada): la ficha se lo asigna al shell
  // aunque viva dentro del dominio `operacion` que B1 posee. Es la única entrada de esta
  // lista que F6 agrega; el resto de `operacion.routes.ts` lo llena B1.
  {
    path: 'estado',
    loadComponent: () => import('./estado/pantalla-de-estado').then((m) => m.PantallaDeEstado),
    title: 'Arquitectura y estado · AportaYa',
  },
]
