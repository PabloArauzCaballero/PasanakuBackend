import { expect, test, type Page } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * GU-09/GU-11 · prueba visual de «Personal y accesos» (usuarios internos, revisión de accesos y el diálogo de acto con
 * segundo factor) en 3 viewports × 2 temas. **Backend simulado, declarado**: los endpoints se responden con `page.route`
 * y datos SINTÉTICOS con la forma del contrato de identidad (#516). Prueba diseño, estados y consola, no al servicio.
 * Capturas en `.playwright/backoffice/visual/personal-y-accesos/` (gitignored).
 */
const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const
const TEMAS = ['light', 'dark'] as const
const PERMISOS = ['USUARIOS_INTERNOS_GESTIONAR', 'ACCESOS_ADMINISTRAR']

const usuario = (n: number, extra: Record<string, unknown> = {}) => ({
  id: `0b10c0e0-0000-4000-8000-0000000000${String(n).padStart(2, '0')}`, usuarioId: `0b10c0e0-0000-4000-8000-0000000001${String(n).padStart(2, '0')}`,
  codigoPublico: `USR-00${n}`, nombres: `Persona${n}`, apellidos: 'Sintética', correo: `persona${n}@ejemplo.test`, telefono: '+59170000000',
  departamento: 'OPERACIONES', cargo: 'Analista', estado: 'ACTIVO', debeCambiarContrasena: false, tieneSegundoFactor: true, roles: ['SOPORTE'],
  creadoEn: `2026-11-0${n}T10:00:00Z`, ...extra,
})
const USUARIOS = [usuario(1), usuario(2, { estado: 'SUSPENDIDO', roles: [] }), usuario(3, { motivoBloqueo: 'ORDEN_ADMIN', bloqueadoHasta: null }), usuario(4, { estado: 'BAJA_LABORAL', roles: [], tieneSegundoFactor: false })]
const fila = (n: number, extra: Record<string, unknown> = {}) => ({
  asignacionId: `0b10c0e0-0000-4000-8000-0000000002${String(n).padStart(2, '0')}`, usuarioId: `0b10c0e0-0000-4000-8000-0000000001${String(n).padStart(2, '0')}`,
  nombre: `Persona${n} Sintética`, estadoCuenta: 'ACTIVO', rolCodigo: 'SOPORTE', ambito: 'GLOBAL', otorgadaPor: '0b10c0e0-0000-4000-8000-000000000999', otorgadaEn: '2026-11-01T10:00:00Z', vigenteHasta: null, totpConfirmado: true, ...extra,
})
const INFORME = { seccion: 'VIVOS', generadoEn: '2026-11-25T12:00:00Z', items: [fila(1), fila(2, { vigenteHasta: '2026-12-01T00:00:00Z' }), fila(3, { totpConfirmado: false })], pagina: 1, limite: 25, total: 3, totalPaginas: 1 }

const json = (page: Page, patron: RegExp, status: number, cuerpo: unknown) => page.route(patron, (r) => r.fulfill({ status, contentType: 'application/json', body: JSON.stringify(cuerpo) }))
const LISTA = /\/usuarios-internos(\?|$)/
const ESTADOS = [
  { nombre: 'datos', status: 200, cuerpo: USUARIOS, espera: /Persona1 Sintética/ },
  { nombre: 'vacio', status: 200, cuerpo: [], espera: /Todavía no hay usuarios|No hay usuarios|Dá de alta/i },
  { nombre: 'error', status: 500, cuerpo: { codigo: 'AP-X', trazaId: 'traza-visual' }, espera: /No pudimos cargar|no está disponible|salió mal/i },
] as const

async function abrir(page: Page, viewport: (typeof VIEWPORTS)[number], tema: (typeof TEMAS)[number], ruta: string): Promise<string[]> {
  const errores: string[] = []
  page.on('console', (m) => {
    if (m.type() === 'error' && !/500|Failed to load resource/.test(m.text())) errores.push(m.text())
  })
  await page.setViewportSize({ width: viewport.width, height: viewport.height })
  await page.emulateMedia({ colorScheme: tema })
  await iniciarSesionDePrueba(page, PERMISOS)
  await navegarSinRecargar(page, ruta)
  return errores
}

for (const viewport of VIEWPORTS) {
  for (const tema of TEMAS) {
    for (const estado of ESTADOS) {
      test(`usuarios internos ${estado.nombre} — ${viewport.nombre} × ${tema}`, async ({ page }) => {
        await json(page, LISTA, estado.status, estado.cuerpo)
        const errores = await abrir(page, viewport, tema, '/sistemas/usuarios-internos')
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
        await expect(page.getByText(estado.espera).first()).toBeVisible()
        await page.screenshot({ path: `../../.playwright/backoffice/visual/personal-y-accesos/usuarios-${estado.nombre}-${viewport.nombre}-${tema}.png`, animations: 'disabled' })
        expect(errores).toEqual([])
      })
    }

    test(`revisión de accesos datos — ${viewport.nombre} × ${tema}`, async ({ page }) => {
      await json(page, /\/usuarios-internos\/accesos\/informe/, 200, INFORME)
      const errores = await abrir(page, viewport, tema, '/sistemas/accesos')
      await expect(page.getByText(/Persona1 Sintética/).first()).toBeVisible()
      await page.screenshot({ path: `../../.playwright/backoffice/visual/personal-y-accesos/accesos-${viewport.nombre}-${tema}.png`, animations: 'disabled' })
      expect(errores).toEqual([])
    })

    test(`diálogo de suspensión con segundo factor — ${viewport.nombre} × ${tema}`, async ({ page }) => {
      await json(page, LISTA, 200, USUARIOS)
      const errores = await abrir(page, viewport, tema, '/sistemas/usuarios-internos')
      await expect(page.getByText(/Persona1 Sintética/).first()).toBeVisible()
      const mas = page.locator('summary[aria-label^="Más acciones para Persona1"]')
      // Se deja a la vista antes de pulsar: el menú se cierra con el desplazamiento de la ventana (decisión de la pantalla).
      await mas.scrollIntoViewIfNeeded()
      await mas.click()
      await page.getByRole('button', { name: /Suspender a Persona1/ }).click()
      const dialogo = page.getByRole('dialog')
      await expect(dialogo.getByText(/Suspender revoca todos sus roles/)).toBeVisible()
      await page.screenshot({ path: `../../.playwright/backoffice/visual/personal-y-accesos/dialogo-suspender-${viewport.nombre}-${tema}.png`, animations: 'disabled' })
      await page.keyboard.press('Escape')
      await expect(dialogo).toBeHidden()
      expect(errores).toEqual([])
    })
  }
}
