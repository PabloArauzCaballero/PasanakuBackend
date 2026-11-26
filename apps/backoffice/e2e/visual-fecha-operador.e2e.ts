import { expect, test, type Page } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * GU-17 · prueba visual de la fecha de nacimiento del personal interno en 3 viewports × 2 temas, animaciones off:
 * el aviso «Falta la fecha de nacimiento» del detalle, el diálogo para completarla y el alta con una persona menor de 18.
 * **Backend simulado, declarado**: las respuestas se sirven con `page.route` y datos SINTÉTICOS con la forma de
 * identidad.yaml. Prueba diseño, estados y consola; no el comportamiento del servicio.
 */
const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const
const TEMAS = ['light', 'dark'] as const
const ID = '0b10c0e0-0000-4000-8000-000000000101'
const DETALLE = {
  id: ID, usuarioId: ID, codigoPublico: 'USR-001', nombres: 'Persona', apellidos: 'Sintética', correo: 'persona@ejemplo.test', telefono: '+59170000000',
  departamento: 'OPERACIONES', cargo: 'Analista', estado: 'ACTIVO', debeCambiarContrasena: false, tieneSegundoFactor: true, roles: ['SOPORTE'],
  permisosEfectivos: ['USUARIOS_INTERNOS_LEER'], creadoEn: '2026-11-01T10:00:00Z', fechaNacimientoPendiente: true,
}
const DIR = '../../.playwright/backoffice/visual/fecha-operador'
const json = (page: Page, patron: RegExp, cuerpo: unknown) => page.route(patron, (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(cuerpo) }))

async function abrir(page: Page, v: (typeof VIEWPORTS)[number], tema: (typeof TEMAS)[number], ruta: string): Promise<string[]> {
  const errores: string[] = []
  page.on('console', (m) => {
    if (m.type() === 'error' && !/40[0-9]|50[0-9]|Failed to load resource/.test(m.text())) errores.push(m.text())
  })
  await page.setViewportSize({ width: v.width, height: v.height })
  await page.emulateMedia({ colorScheme: tema, reducedMotion: 'reduce' })
  await iniciarSesionDePrueba(page, ['USUARIOS_INTERNOS_GESTIONAR', 'ACCESOS_ADMINISTRAR'])
  await navegarSinRecargar(page, ruta)
  return errores
}

for (const v of VIEWPORTS) {
  for (const tema of TEMAS) {
    test(`detalle sin dato de fecha — ${v.nombre} × ${tema}`, async ({ page }) => {
      await json(page, new RegExp(`/usuarios-internos/${ID}$`), DETALLE)
      const errores = await abrir(page, v, tema, `/sistemas/usuarios-internos/${ID}`)
      await expect(page.getByText('Falta la fecha de nacimiento')).toBeVisible()
      await page.screenshot({ path: `${DIR}/detalle-sin-dato.${v.nombre}.${tema}.png`, fullPage: true, animations: 'disabled' })
      expect(errores, 'consola sin errores inesperados').toEqual([])
    })

    test(`completar la fecha — ${v.nombre} × ${tema}`, async ({ page }) => {
      await json(page, new RegExp(`/usuarios-internos/${ID}$`), DETALLE)
      const errores = await abrir(page, v, tema, `/sistemas/usuarios-internos/${ID}`)
      await page.getByRole('button', { name: 'Completar la fecha' }).click()
      const dialogo = page.getByRole('dialog')
      await dialogo.getByLabel('Fecha de nacimiento').fill('2999-01-01')
      await expect(dialogo.getByText('no puede ser futura')).toBeVisible()
      await page.screenshot({ path: `${DIR}/completar-fecha-error.${v.nombre}.${tema}.png`, animations: 'disabled' })
      expect(errores, 'consola sin errores inesperados').toEqual([])
    })

    test(`alta con menor de 18 — ${v.nombre} × ${tema}`, async ({ page }) => {
      await json(page, /\/usuarios-internos(\?|$)/, [])
      const errores = await abrir(page, v, tema, '/sistemas/usuarios-internos')
      await page.getByRole('button', { name: /Nuevo usuario|Dar de alta|Alta/ }).first().click()
      const dialogo = page.getByRole('dialog')
      await dialogo.getByLabel('Fecha de nacimiento').fill(`${new Date().getFullYear() - 17}-06-15`)
      await expect(dialogo.getByText('mayor de 18 años')).toBeVisible()
      await page.screenshot({ path: `${DIR}/alta-menor-de-edad.${v.nombre}.${tema}.png`, animations: 'disabled' })
      expect(errores, 'consola sin errores inesperados').toEqual([])
    })
  }
}
