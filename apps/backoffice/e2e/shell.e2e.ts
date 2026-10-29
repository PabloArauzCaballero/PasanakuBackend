import { expect, test } from '@playwright/test'
import { capturar, todas } from './apoyo/capturas-f2'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'
import { SISTEMAS, servir } from './apoyo/tableros-sinteticos'

/**
 * El shell con la anatomía del portal de Atlas (plan backoffice-como-atlas, H2): menú por grupos con
 * una entrada por sección (siete desde el recorte del 2026-10-26), pestañas con las pantallas de la sección activa (2026-10-21), migas y título en la cabecera, buscador global, menú de quien
 * opera y, en el celular, el cajón que abre la hamburguesa. Login real de CU-04 contra Prism; las
 * pantallas de abajo son las reales (la de outbox lee del mock).
 */
const P = 'h2'
const PERMISOS = ['TABLERO_SISTEMAS_VER', 'ACCESOS_ADMINISTRAR', 'VERIFICACION_RESOLVER', 'RETIRO_APROBAR', 'CONTABILIDAD_ERP_CERRAR']

test('escritorio: grupos, pestañas de la sección activa, migas y título; capturas en 3 anchos × 2 temas', async ({ page }) => {
  await servir(page, '/indicadores', { cuerpo: SISTEMAS })
  await iniciarSesionDePrueba(page, PERMISOS, 'PLATAFORMA')
  await navegarSinRecargar(page, '/sistemas/outbox')
  const menu = page.getByRole('navigation', { name: 'Secciones del backoffice' })
  // «Plataforma» es un rótulo fijo (2026-10-28): un enlace con ícono, nada que plegar.
  await expect(menu.getByRole('link', { name: 'Plataforma' })).toHaveAttribute('href', '/sistemas/servicios')
  await expect(menu.locator('[aria-expanded]')).toHaveCount(0)
  // Las pantallas de la sección están fijas debajo de ella (2026-10-28): la actual se marca con aria-current.
  await expect(menu.getByRole('link', { name: 'Sistemas' })).toHaveClass(/activo/)
  await expect(menu.getByRole('link', { name: 'Outbox y descartados' })).toHaveAttribute('aria-current', 'page')
  // Sin pestañas de sección (2026-10-28, `5d0f8d99`): Outbox se lista en el menú dentro de su subgrupo «Integraciones».
  await expect(menu.getByRole('list', { name: 'Integraciones' }).getByRole('link', { name: 'Outbox y descartados' })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('navigation', { name: 'Pantallas de Sistemas' })).toHaveCount(0)
  // Una sola línea de migas: sección › pantalla actual. «Inicio» está en el menú lateral y el grupo en las pestañas (2026-10-27).
  const migas = page.getByRole('navigation', { name: 'Migas de pan' })
  await expect(migas).not.toContainText('Inicio')
  await expect(migas).toContainText('Sistemas')
  await expect(migas).not.toContainText('Integraciones')
  await expect(migas.locator('[aria-current="page"]')).toHaveText('Outbox y descartados')
  await expect(page.locator('.titulo-vista')).toHaveText('Outbox y descartados')
  await todas(page, P, 'shell-outbox')

  // El rótulo no se pliega y abre su primera sección; sus entradas siguen a la vista desde cualquier sección.
  await page.setViewportSize({ width: 1280, height: 900 })
  // Sin «Inicio» en el menú: al inicio se vuelve con el logo.
  await page.getByRole('link', { name: /AportaYa/ }).first().click()
  await expect(page).toHaveURL(/\/tablero$/)
  await expect(menu.getByRole('link', { name: 'Sistemas' })).toBeVisible()
  await menu.getByRole('link', { name: 'Plataforma' }).click()
  await expect(page).toHaveURL(/\/sistemas\/servicios$/)
  await expect(menu.getByRole('link', { name: 'Servicios y SLO' })).toHaveAttribute('aria-current', 'page')
})

test('cabecera: el menú de usuario muestra el rol y ofrece salir; el buscador abre una pantalla con el teclado', async ({ page }) => {
  await iniciarSesionDePrueba(page, PERMISOS, 'PLATAFORMA')
  await navegarSinRecargar(page, '/tablero')
  // La cabecera muestra el rol del token en texto legible (el guard de Sistemas exige PLATAFORMA o SEGURIDAD).
  await page.locator('[data-tutorial-id="cabecera-rol"]').click()
  // El disparador del menú de cuenta, no el grupo «Plataforma» del menú lateral (mismo texto).
  await expect(page.getByRole('banner').getByRole('button', { name: /Plataforma/, expanded: true })).toBeVisible()
  await expect(page.getByRole('menu', { name: 'Cuenta' })).toBeVisible()
  // Ayuda vive acá desde el recorte del menú lateral; cada opción con ícono y texto.
  await expect(page.getByRole('menuitem')).toHaveText(['Centro de ayuda', 'Salir'])
  await capturar(page, P, 'menu-de-usuario', 'escritorio', 'light')
  await page.keyboard.press('Escape')
  await expect(page.getByRole('menu', { name: 'Cuenta' })).toBeHidden()

  await page.keyboard.press('ControlOrMeta+K')
  const buscador = page.getByRole('combobox', { name: 'Buscar en el backoffice' })
  await expect(buscador).toBeFocused()
  await buscador.fill('outb')
  await expect(page.getByRole('option', { name: /Outbox y descartados/ })).toBeVisible()
  await capturar(page, P, 'buscador', 'escritorio', 'dark')
  await page.keyboard.press('Enter')
  await expect(page).toHaveURL(/\/sistemas\/outbox$/)
})

test('celular: la hamburguesa abre el cajón con el menú y navegar lo cierra', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 740 })
  await iniciarSesionDePrueba(page, PERMISOS, 'PLATAFORMA')
  await navegarSinRecargar(page, '/tablero')
  await expect(page.getByRole('navigation', { name: 'Secciones del backoffice' })).toHaveCount(0)
  await page.getByRole('button', { name: 'Abrir el menú' }).click()
  const cajon = page.getByRole('dialog', { name: 'Secciones' })
  await expect(cajon).toBeVisible()
  await capturar(page, P, 'cajon-menu', 'movil', 'light')
  await cajon.getByRole('link', { name: 'Sistemas' }).click()
  await expect(page).toHaveURL(/\/sistemas/)
  await expect(cajon).toBeHidden()
})
