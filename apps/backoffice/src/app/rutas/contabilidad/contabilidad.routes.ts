import { Routes } from '@angular/router'
import { requierePermiso } from '../../nucleo/permisos'

/**
 * Las rutas del dominio contabilidad (ERP, CU-100 a CU-106). **Este archivo lo posee el
 * carril B3**; el shell solo lo carga por `loadChildren` desde `app.routes.ts`, que no
 * cambia. Las seis rutas son las que fija
 * `docs/Flujo de pantallas · backoffice administrador` §5, con el mismo patrón que ya
 * usan `operacion` y `cumplimiento`: una carpeta por pantalla al lado de este archivo y
 * una entrada acá.
 *
 * `requierePermiso` (de `nucleo/permisos.ts`, del shell) monta solo la ruta que el
 * permiso del operador habilita y redirige al tablero si no. Los nombres de permiso son
 * los que declara cada CU en su tabla «Eventos, trabajos y permisos»
 * (`CONTABILIDAD_ERP_*`). El guard evita la navegación, pero **no** reemplaza al 403 del
 * gateway ni a la comprobación que cada pantalla vuelve a hacer antes de montar sus
 * acciones: `puedePagar`, `puedeCobrar` y `puedeDepreciar` se consultan en el componente,
 * porque el permiso puede cambiar después de montada la ruta.
 *
 * **Supuesto declarado sobre los parámetros de la URL.** `erp.yaml` no publica ninguna
 * operación de listado (ver `dominio/contrato-erp.ts`), y tampoco una que diga cuál es el
 * ejercicio o el período vigente. Las tres rutas que lo necesitan lo llevan en la URL
 * (`:ejercicioId`, `:periodoId`), que es además lo que permite pegar el enlace de un mes
 * concreto en un expediente — mismo criterio que `cumplimiento`. La redirección de `''`
 * apunta al ejercicio de demostración, igual que hace `operacion` con su cuenta.
 */
export const rutasContabilidad: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'periodo/11111111-1111-4111-8111-111111111111' },
  {
    path: 'periodo/:ejercicioId',
    loadComponent: () => import('./periodo/pantalla-de-periodo').then((m) => m.PantallaDePeriodo),
    title: 'Períodos contables · AportaYa',
    canMatch: [requierePermiso('CONTABILIDAD_ERP_CERRAR')],
  },
  {
    path: 'presupuesto',
    loadComponent: () => import('./presupuesto/pantalla-de-presupuesto').then((m) => m.PantallaDePresupuesto),
    title: 'Presupuesto por centro de costo · AportaYa',
    canMatch: [requierePermiso('CONTABILIDAD_ERP_PRESUPUESTO')],
  },
  {
    path: 'compras',
    loadComponent: () => import('./compras/pantalla-de-compras').then((m) => m.PantallaDeCompras),
    title: 'Compras y cuentas por pagar · AportaYa',
    canMatch: [requierePermiso('CONTABILIDAD_ERP_COMPRAS')],
  },
  {
    path: 'cobros',
    loadComponent: () => import('./cobros/pantalla-de-cobros').then((m) => m.PantallaDeCobros),
    title: 'Cuentas por cobrar · AportaYa',
    canMatch: [requierePermiso('CONTABILIDAD_ERP_COBRAR')],
  },
  {
    path: 'activos/:periodoVigenteId',
    loadComponent: () => import('./activos/pantalla-de-activos').then((m) => m.PantallaDeActivos),
    title: 'Activos fijos · AportaYa',
    canMatch: [requierePermiso('CONTABILIDAD_ERP_ACTIVOS_FIJOS')],
  },
  {
    path: 'estados/:periodoId',
    loadComponent: () => import('./estados/pantalla-de-estados-financieros').then((m) => m.PantallaDeEstadosFinancieros),
    title: 'Estados financieros · AportaYa',
    canMatch: [requierePermiso('CONTABILIDAD_ERP_REPORTES')],
  },
]
