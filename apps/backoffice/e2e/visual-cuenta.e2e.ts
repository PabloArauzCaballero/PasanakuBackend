import { expect, test, type Page } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * GU-C · prueba visual de `/acceso/*` en 3 viewports × 2 temas, con animaciones desactivadas.
 * **Backend simulado, declarado**: las respuestas de `/cuenta/*` se sirven con `page.route` y datos SINTÉTICOS con la forma
 * de identidad.yaml. Prueba diseño, estados y consola; no el comportamiento del servicio (eso es de las pruebas de
 * integración del servicio). Capturas en `.playwright/backoffice/visual/cuenta/` (gitignored).
 */
const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const
const TEMAS = ['light', 'dark'] as const
const DIR = '../../.playwright/backoffice/visual/cuenta'
const json = (status: number, cuerpo: unknown) => ({ status, contentType: 'application/json', body: JSON.stringify(cuerpo) })
const ERROR_GENERICO = { codigo: 'AP-CU09-04', mensaje: 'x', trazaId: 'traza-visual' }
const SOLICITUD = {
  id: '0c900000-0000-4000-8000-000000000001', usuarioId: '0c900000-0000-4000-8000-0000000000a1', nombres: 'Persona', apellidos: 'Sintética',
  alcance: 'CLAVE', estado: 'PENDIENTE', solicitadaEn: '2026-11-26T10:00:00Z', motivo: 'Olvidé mi clave (dato sintético)',
}

async function preparar(page: Page, v: (typeof VIEWPORTS)[number], tema: (typeof TEMAS)[number], errores: string[]): Promise<void> {
  page.on('console', (m) => {
    if (m.type() === 'error' && !/40[0-9]|50[0-9]/.test(m.text())) errores.push(m.text())
  })
  await page.setViewportSize({ width: v.width, height: v.height })
  await page.emulateMedia({ colorScheme: tema, reducedMotion: 'reduce' })
}

async function foto(page: Page, nombre: string, v: (typeof VIEWPORTS)[number], tema: string): Promise<void> {
  await page.screenshot({ path: `${DIR}/${nombre}.${v.nombre}.${tema}.png`, fullPage: true, animations: 'disabled' })
}

for (const v of VIEWPORTS) {
  for (const tema of TEMAS) {
    test(`invitación datos — ${v.nombre} × ${tema}`, async ({ page }) => {
      const errores: string[] = []
      await page.route(/\/cuenta\/invitacion\/consultar$/, (r) => r.fulfill(json(200, { alcance: 'ALTA', nombres: 'Persona', venceEn: '2026-11-29T10:00:00Z', requiereClaveActual: false, requiereFactor: true })))
      await preparar(page, v, tema, errores)
      await page.goto('/acceso/invitacion#token=token-sintetico-de-prueba')
      await expect(page.getByText(/Hola, Persona/)).toBeVisible()
      await foto(page, 'invitacion-datos', v, tema)
      expect(errores, 'consola sin errores inesperados').toEqual([])
    })

    test(`invitación enlace inválido — ${v.nombre} × ${tema}`, async ({ page }) => {
      const errores: string[] = []
      await page.route(/\/cuenta\/invitacion\/consultar$/, (r) => r.fulfill(json(400, ERROR_GENERICO)))
      await preparar(page, v, tema, errores)
      await page.goto('/acceso/invitacion#token=token-sintetico-de-prueba')
      await expect(page.getByRole('heading').first()).toBeVisible()
      await foto(page, 'invitacion-invalida', v, tema)
      expect(errores, 'consola sin errores inesperados').toEqual([])
    })

    test(`no puedo entrar — ${v.nombre} × ${tema}`, async ({ page }) => {
      const errores: string[] = []
      await page.route(/\/cuenta\/recuperaciones$/, (r) => r.fulfill(json(202, { recibido: true })))
      await preparar(page, v, tema, errores)
      await page.goto('/acceso/recuperar')
      await foto(page, 'recuperar-formulario', v, tema)
      await page.getByLabel('Correo institucional').fill('operador@ejemplo.test')
      await page.getByRole('button', { name: /Enviar|Pedir/ }).click()
      await expect(page.getByText('Pedido recibido')).toBeVisible()
      await foto(page, 'recuperar-enviado', v, tema)
      expect(errores, 'consola sin errores inesperados').toEqual([])
    })

    for (const estado of [
      { nombre: 'datos', status: 200, cuerpo: { items: [SOLICITUD, { ...SOLICITUD, id: '0c900000-0000-4000-8000-000000000002', alcance: 'FACTOR', estado: 'APROBADA', enfriamientoHasta: '2026-11-27T10:00:00Z' }] }, espera: /Olvidé mi clave/ },
      { nombre: 'vacio', status: 200, cuerpo: { items: [] }, espera: /Ningún registro de esta página coincide/ },
      { nombre: 'error', status: 503, cuerpo: { codigo: 'AP-0000', mensaje: 'x', trazaId: 'traza-visual' }, espera: /Volver a intentar|intentar/ },
    ] as const) {
      test(`recuperaciones ${estado.nombre} — ${v.nombre} × ${tema}`, async ({ page }) => {
        const errores: string[] = []
        await page.route(/\/cuenta\/recuperaciones(\?.*)?$/, (r) => r.fulfill(json(estado.status, estado.cuerpo)))
        await preparar(page, v, tema, errores)
        await iniciarSesionDePrueba(page, ['SEGURIDAD_ACCESO_RESTABLECER'])
        await navegarSinRecargar(page, '/acceso/recuperaciones')
        await expect(page.getByText(estado.espera).first()).toBeVisible()
        await foto(page, `recuperaciones-${estado.nombre}`, v, tema)
        expect(errores, 'consola sin errores inesperados').toEqual([])
      })
    }

    test(`cambiar mi clave — ${v.nombre} × ${tema}`, async ({ page }) => {
      const errores: string[] = []
      await preparar(page, v, tema, errores)
      await iniciarSesionDePrueba(page, [])
      await navegarSinRecargar(page, '/acceso/cambiar-clave')
      await expect(page.getByRole('heading', { name: 'Cambiar mi clave' })).toBeVisible()
      await foto(page, 'cambiar-clave', v, tema)
      expect(errores, 'consola sin errores inesperados').toEqual([])
    })
  }
}
