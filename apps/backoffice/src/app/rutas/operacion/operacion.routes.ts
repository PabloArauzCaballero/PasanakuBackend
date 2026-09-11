import { Routes } from '@angular/router'

/**
 * Las rutas del dominio operación. **Este archivo lo posee el carril B1**; el shell
 * solo lo carga por `loadChildren`. Agregar una pantalla es agregar una carpeta acá al
 * lado y una entrada en esta lista: nada de `app.routes.ts`, `nucleo/` ni `layout/` cambia.
 */
export const rutasOperacion: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'billetera/11111111-1111-4111-8111-111111111111' },
  {
    path: 'billetera/:cuentaId',
    loadComponent: () => import('./billetera/pantalla-de-billetera').then((m) => m.PantallaDeBilletera),
    title: 'Billetera · AportaYa',
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
