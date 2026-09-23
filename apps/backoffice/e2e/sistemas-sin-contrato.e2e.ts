import { expect, test } from '@playwright/test'
import { abrirSesionDePruebaYNavegar } from './apoyo/sesion-de-prueba'

/**
 * H2.S2.M6 — sin modo demo, las nueve rutas de `sistemas/` muestran el error
 * accionable ("la fuente no está disponible"), nunca los datos de ejemplo
 * (`99.95%` y compañía).
 *
 * ACTUALIZACIÓN sobre el hallazgo heredado del carril F12: el comentario original de
 * este archivo decía "no existe ninguna pantalla de login" y que las nueve rutas
 * redirigían a `/tablero` sin sesión. Corrida de verdad contra el código de este
 * checkout, ninguna de las dos cosas es cierta hoy:
 *   - `2025a43 feat: el backoffice tiene por donde entrar` agregó `/ingreso`
 *     (`PantallaDeIngreso`, CU-04) después de que F12 escribiera su hallazgo.
 *   - `requiereSesion()` (`nucleo/permisos.ts`) redirige sin sesión a `/ingreso`, no a
 *     `/tablero`; y `soloRolesDeSistemas` (`rutas/sistemas/guardia-rol-sistemas.ts`),
 *     si el rol no es PLATAFORMA/SEGURIDAD, redirige a `/operacion`, no a `/tablero`.
 *
 * Sigue sin existir un `storageState` inyectable: `Sesion` guarda el token SOLO en
 * memoria a propósito (ver `nucleo/sesion.ts`). En vez de eso, y en vez de ejercitar
 * `/ingreso` contra las credenciales de ejemplo del mock de Prism (fuera de esta
 * pasada), este archivo abre sesión con un DOBLE DE PRUEBA documentado en
 * `e2e/apoyo/sesion-de-prueba.ts`: toma la instancia ya montada de `PantallaDeIngreso`
 * vía la API de depuración que Angular expone en builds no-production y llama el
 * método público `Sesion.abrir(...)` que `nucleo/sesion.ts` ya define. No se edita
 * `nucleo/sesion*`, `nucleo/permisos.ts` ni `app.config.ts` (fuera de mi reserva).
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

// ACCESOS_ADMINISTRAR abre `ver:sistemas` (nucleo/secciones.ts); PLATAFORMA pasa
// `soloRolesDeSistemas`. Las dos barreras de interfaz, satisfechas por el doble.
const PERMISOS_DE_SISTEMAS = ['ACCESOS_ADMINISTRAR']
const ROL_DE_SISTEMAS = 'PLATAFORMA'

test.describe('sistemas — sin contrato real, nunca datos de ejemplo (con sesión de prueba)', () => {
  for (const { ruta } of RUTAS) {
    test(`/sistemas/${ruta}: con sesión ve el error accionable, nunca "99.95" en pantalla`, async ({ page }) => {
      await abrirSesionDePruebaYNavegar(page, PERMISOS_DE_SISTEMAS, ROL_DE_SISTEMAS, `/sistemas/${ruta}`)

      // No hay servicio "sistemas" real ni mock que lo cubra en este sandbox: el
      // estado esperado es el de error accionable, nunca el de éxito con datos de
      // ejemplo — eso es lo que este caso prueba.
      await expect(page.getByRole('alert')).toBeVisible()
      await expect(page.locator('body')).not.toContainText('99.95')
    })
  }

  test('sin sesión, /sistemas/servicios redirige a /ingreso (requiereSesion en el shell)', async ({ page }) => {
    const r = await page.goto('/sistemas/servicios')
    expect(r?.status()).toBe(200)
    await expect(page).toHaveURL(/\/ingreso$/)
  })

  test('con sesión pero sin rol PLATAFORMA/SEGURIDAD, /sistemas/servicios nunca monta (soloRolesDeSistemas)', async ({ page }) => {
    // `soloRolesDeSistemas` manda a /operacion, pero un rol CONTABILIDAD tampoco tiene
    // ver:operacion (nucleo/secciones.ts), así que ESE canMatch reenvía otra vez, a
    // /tablero. Lo que este caso prueba es la barrera de rol en sí (nunca llega a
    // /sistemas), no la cadena completa de reenvíos, que depende de qué otro permiso
    // tenga la sesión.
    await abrirSesionDePruebaYNavegar(page, PERMISOS_DE_SISTEMAS, 'CONTABILIDAD', '/sistemas/servicios')
    await expect(page).not.toHaveURL(/\/sistemas\//)
  })
})
