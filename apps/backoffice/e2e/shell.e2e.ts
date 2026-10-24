import { expect, test } from '@playwright/test'
import { capturar, todas } from './apoyo/capturas-f2'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'
import { SISTEMAS, servir } from './apoyo/tableros-sinteticos'

/**
 * El shell con la anatomía del portal de Atlas (plan backoffice-como-atlas, H2): menú en acordeón con
 * las pantallas de la sección activa, migas y título en la cabecera, buscador global, menú de quien
 * opera y, en el celular, el cajón que abre la hamburguesa. Login real de CU-04 contra Prism; las
 * pantallas de abajo son las reales (la de outbox lee del mock).
 */
const P = 'h2'
const PERMISOS = ['TABLERO_SISTEMAS_VER', 'ACCESOS_ADMINISTRAR', 'VERIFICACION_RESOLVER', 'RETIRO_APROBAR', 'CONTABILIDAD_ERP_CERRAR']

test('escritorio: grupos, pantallas de la sección activa, migas y título; capturas en 3 anchos × 2 temas', async ({ page }) => {
  await servir(page, '/indicadores', { cuerpo: SISTEMAS })
  await iniciarSesionDePrueba(page, PERMISOS, 'PLATAFORMA')
  await navegarSinRecargar(page, '/sistemas/outbox')
  const menu = page.getByRole('navigation', { name: 'Secciones del backoffice' })
  // Acordeón (3d4344e3): la sección de la ruta activa arranca desplegada y su pantalla marca la página actual.
  const sistemas = menu.getByRole('button', { name: /pantallas de Sistemas/ })
  await expect(sistemas).toHaveAttribute('aria-expanded', 'true')
  await expect(menu.getByRole('link', { name: 'Outbox y descartados' })).toHaveAttribute('aria-current', 'page')
  const migas = page.getByRole('navigation', { name: 'Migas de pan' })
  await expect(migas).toContainText('Inicio')
  await expect(migas).toContainText('Sistemas')
  await expect(migas.locator('[aria-current="page"]')).toHaveText('Outbox y descartados')
  await todas(page, P, 'shell-outbox')

  // Plegar la sección oculta sus pantallas (y las saca del orden de tabulación); volver a abrirla las devuelve.
  await page.setViewportSize({ width: 1280, height: 900 })
  await sistemas.click()
  await expect(sistemas).toHaveAttribute('aria-expanded', 'false')
  await expect(menu.getByRole('link', { name: 'Outbox y descartados' })).toBeHidden()
  await sistemas.click()
  await expect(sistemas).toHaveAttribute('aria-expanded', 'true')
  await expect(menu.getByRole('link', { name: 'Outbox y descartados' })).toBeVisible()
})

test('cabecera: el menú de usuario muestra el rol y ofrece salir; el buscador abre una pantalla con el teclado', async ({ page }) => {
  await iniciarSesionDePrueba(page, PERMISOS, 'PLATAFORMA')
  await navegarSinRecargar(page, '/tablero')
  // La cabecera muestra el rol del token en texto legible (el guard de Sistemas exige PLATAFORMA o SEGURIDAD).
  await page.locator('[data-tutorial-id="cabecera-rol"]').click()
  await expect(page.getByRole('button', { name: /Plataforma/, expanded: true })).toBeVisible()
  await expect(page.getByRole('menu', { name: 'Cuenta' })).toBeVisible()
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
