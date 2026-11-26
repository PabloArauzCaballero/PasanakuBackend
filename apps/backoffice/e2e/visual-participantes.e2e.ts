import { expect, test, type Page } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * CU-134 · prueba visual del directorio y la ficha del participante en 3 viewports × 2 temas.
 * **Backend simulado, declarado**: `busquedas` y `ficha` se responden con `page.route` usando datos SINTÉTICOS con la
 * forma de `PaginaDeParticipantes` y `FichaDeParticipante` (identidad.yaml). Prueba diseño, estados y consola; no el
 * comportamiento del servicio. Capturas en `.playwright/backoffice/visual/participantes/` (gitignored).
 */
const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const
const TEMAS = ['light', 'dark'] as const
const USUARIO = '0c134000-0000-4000-8000-000000000001'
const FILA = { usuarioId: USUARIO, codigoPublico: 'AP-SINT-0001', nombreAbreviado: 'Persona S.', estado: 'ACTIVO', nivelKyc: 'BASICO', fechaRegistro: '2026-01-10T12:00:00Z', tieneRestriccion: false, tieneBloqueo: false, tieneMedidaEnCurso: true }
const PAGINA = { filas: [FILA, { ...FILA, usuarioId: '0c134000-0000-4000-8000-000000000002', codigoPublico: 'AP-SINT-0002', nombreAbreviado: 'Otra P.', estado: 'SUSPENDIDO', tieneMedidaEnCurso: false, tieneBloqueo: true }] }
const FICHA = {
  ...FILA,
  nombreCompleto: 'Persona Sintética',
  telefonoEnmascarado: '+591 7•••••12',
  correoEnmascarado: 'p•••@ejemplo.test',
  medidas: [
    {
      expedienteId: '0c134000-0000-4000-8000-0000000000e1', usuarioId: USUARIO, causal: 'CUENTA_COMPROMETIDA', medida: 'SUSPENSION_TEMPORAL', cautelar: true,
      estado: 'EN_DESCARGO', estadoCuenta: 'SUSPENDIDA', propuestaEn: '2026-11-20T10:00:00Z', notificadaEn: '2026-11-20T10:05:00Z',
      fechaLimiteDescargo: '2026-11-27T23:59:59Z', prescribeEn: '2027-11-20T10:00:00Z',
      lineaDeTiempo: [{ estado: 'PROPUESTA', ocurridoEn: '2026-11-20T10:00:00Z' }, { estadoAnterior: 'PROPUESTA', estado: 'EN_DESCARGO', ocurridoEn: '2026-11-20T10:05:00Z' }],
    },
  ],
}
const DIRECTORIO = [
  { nombre: 'datos', status: 200, cuerpo: PAGINA, espera: /AP-SINT-0002/ },
  { nombre: 'vacio', status: 200, cuerpo: { filas: [] }, espera: /Ningún participante coincide/ },
  { nombre: 'error', status: 503, cuerpo: { codigo: 'AP-0000', mensaje: 'x', trazaId: 'traza-visual' }, espera: /Volver a intentar/ },
] as const

async function preparar(page: Page, viewport: (typeof VIEWPORTS)[number], tema: (typeof TEMAS)[number], errores: string[], permisos = ['PARTICIPANTES_LEER']): Promise<void> {
  page.on('console', (m) => {
    if (m.type() === 'error' && !/503/.test(m.text())) errores.push(m.text())
  })
  await page.setViewportSize({ width: viewport.width, height: viewport.height })
  await page.emulateMedia({ colorScheme: tema })
  await iniciarSesionDePrueba(page, permisos)
}

const DIR = '../../.playwright/backoffice/visual/participantes'

for (const viewport of VIEWPORTS) {
  for (const tema of TEMAS) {
    for (const estado of DIRECTORIO) {
      test(`participantes ${estado.nombre} — ${viewport.nombre} × ${tema}`, async ({ page }) => {
        const errores: string[] = []
        await page.route(/\/identidad\/participantes\/busquedas$/, (r) => r.fulfill({ status: estado.status, contentType: 'application/json', body: JSON.stringify(estado.cuerpo) }))
        await preparar(page, viewport, tema, errores)
        await navegarSinRecargar(page, '/cumplimiento/participantes')
        await page.getByLabel('Por qué consultas').fill('Ticket SOP-0001 sintético')
        await page.getByRole('button', { name: 'Buscar' }).click()
        await expect(page.getByText(estado.espera).first()).toBeVisible()
        await page.screenshot({ path: `${DIR}/directorio-${estado.nombre}.${viewport.nombre}.${tema}.png`, fullPage: true })
        expect(errores, 'consola sin errores inesperados').toEqual([])
      })
    }

    test(`participantes ficha — ${viewport.nombre} × ${tema}`, async ({ page }) => {
      const errores: string[] = []
      await page.route(/\/identidad\/participantes\/[^/]+\/ficha$/, (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(FICHA) }))
      await preparar(page, viewport, tema, errores)
      await navegarSinRecargar(page, `/cumplimiento/participantes/${USUARIO}`)
      await page.getByLabel('Por qué consultas').fill('Ticket SOP-0001 sintético')
      await page.getByRole('button', { name: 'Abrir ficha' }).click()
      await expect(page.getByText('+591 7•••••12')).toBeVisible()
      await page.screenshot({ path: `${DIR}/ficha.${viewport.nombre}.${tema}.png`, fullPage: true })
      expect(errores, 'consola sin errores inesperados').toEqual([])
    })
  }
}

/** H3.S6.M3 · los actos con segundo factor: la ficha con sus botones y el diálogo de confirmar, en 3 viewports × 2 temas. */
const ACTOS = ['PARTICIPANTES_LEER', 'PARTICIPANTES_MEDIDA_DECIDIR', 'PARTICIPANTES_MEDIDA_PROPONER', 'DATOS_SENSIBLES_LEER']
for (const viewport of VIEWPORTS) {
  for (const tema of TEMAS) {
    test(`participantes actos — ${viewport.nombre} × ${tema}`, async ({ page }) => {
      const errores: string[] = []
      await page.route(/\/identidad\/participantes\/[^/]+\/ficha$/, (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(FICHA) }))
      await preparar(page, viewport, tema, errores, ACTOS)
      await navegarSinRecargar(page, `/cumplimiento/participantes/${USUARIO}`)
      await page.getByLabel('Por qué consultas').fill('Ticket SOP-0001 sintético')
      await page.getByRole('button', { name: 'Abrir ficha' }).click()
      await expect(page.getByRole('button', { name: 'Confirmar medida' })).toBeVisible()
      await expect(page.getByRole('button', { name: 'Revelar teléfono' })).toBeVisible()
      await page.screenshot({ path: `${DIR}/actos-ficha.${viewport.nombre}.${tema}.png`, fullPage: true })
      await page.getByRole('button', { name: 'Confirmar medida' }).click()
      const dialogo = page.getByRole('dialog', { name: 'Confirmar la medida' })
      await expect(dialogo).toBeVisible()
      await dialogo.getByLabel('Fundamento de la decisión').fill('La evidencia del sistema y el descargo sostienen la medida propuesta.')
      await page.screenshot({ path: `${DIR}/actos-confirmar.${viewport.nombre}.${tema}.png` })
      await dialogo.getByRole('button', { name: 'Cancelar' }).click()
      await page.getByRole('button', { name: 'Proponer medida' }).click()
      await expect(page.getByRole('dialog', { name: 'Proponer una medida sobre la cuenta' })).toBeVisible()
      await page.screenshot({ path: `${DIR}/actos-proponer.${viewport.nombre}.${tema}.png` })
      expect(errores, 'consola sin errores inesperados').toEqual([])
    })
  }
}
