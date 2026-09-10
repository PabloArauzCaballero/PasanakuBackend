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
]
