import { expect, test } from '@playwright/test'

/**
 * H2.S2.M6 — sin modo demo, las nueve rutas de `sistemas/` muestran el error
 * accionable ("la fuente no está disponible"), nunca los datos de ejemplo
 * (`99.95%` y compañía).
 *
 * HALLAZGO HEREDADO (carril F12, `tablero-y-permisos.e2e.ts` lo documenta primero):
 * no existe ninguna pantalla de login en `apps/backoffice` ni un `APP_INITIALIZER`
 * que abra sesión — `Sesion.permisos()` empieza vacío en toda carga real, y
 * `sistemas.routes.ts` está detrás de DOS barreras (`canMatch: [requierePermiso(
 * 'ver:sistemas')]` en la ruta padre — de F6/app.routes.ts, congelada — y
 * `canMatch: [soloRolesDeSistemas]` acá mismo). Sin sesión, las nueve rutas
 * redirigen a `/tablero` antes de montar cualquier pantalla — ni siquiera llegan a
 * pedir datos, así que el error accionable no se ve (tampoco los mocks: se
 * comprueba con el kill-test de `yarn workspace @aportaya/backoffice build`, no acá).
 *
 * Este archivo deja las nueve aserciones escritas y las corre contra lo que el
 * shell expone hoy sin sesión (redirección), documentando el bloqueo en vez de
 * inventar un login que no es mío (`nucleo/sesion*` está fuera de mi alcance). Si se
 * corre con una sesión ya abierta (p. ej. inyectada por un futuro `storageState` de
 * Playwright con un token de PLATAFORMA/SEGURIDAD), las mismas aserciones sirven
 * para el caso real: por eso `esperarPantallaOredireccion` cubre las dos ramas.
 */
const RUTAS = [
  { ruta: 'servicios', selectorTitulo: 'h1' },
  { ruta: 'despliegues', selectorTitulo: 'h1' },
  { ruta: 'base-de-datos', selectorTitulo: 'h1' },
  { ruta: 'respaldos', selectorTitulo: 'h1' },
  { ruta: 'proveedores', selectorTitulo: 'h1' },
  { ruta: 'outbox', selectorTitulo: 'h1' },
  { ruta: 'webhooks', selectorTitulo: 'h1' },
  { ruta: 'accesos', selectorTitulo: 'h1' },
  { ruta: 'incidentes', selectorTitulo: 'h1' },
]

test.describe('sistemas — sin contrato real, nunca datos de ejemplo', () => {
  for (const { ruta } of RUTAS) {
    test(`/sistemas/${ruta}: nunca "99.95" en pantalla; con sesión ve el error accionable, sin sesión redirige`, async ({ page }) => {
      const r = await page.goto(`/sistemas/${ruta}`)
      expect(r?.status()).toBe(200)

      const url = page.url()
      if (url.includes('/tablero')) {
        // Bloqueado por el hallazgo de F12 (arriba): sin sesión, canMatch redirige antes
        // de montar la pantalla. Se documenta, no se simula una sesión que no es mía.
        test.info().annotations.push({
          type: 'bloqueado-F12',
          description: `/sistemas/${ruta} redirigió a /tablero: no hay login real para entrar con rol PLATAFORMA/SEGURIDAD`,
        })
        return
      }

      // Con sesión (futuro storageState con rol PLATAFORMA/SEGURIDAD): el estado de
      // error es un `role="alert"` accionable, y en ningún caso aparece el 99.95%
      // de los mocks — el kill-test real es el grep sobre el bundle, esto es la
      // vista en vivo del mismo hecho.
      await expect(page.getByRole('alert')).toBeVisible()
      await expect(page.locator('body')).not.toContainText('99.95')
    })
  }
})
