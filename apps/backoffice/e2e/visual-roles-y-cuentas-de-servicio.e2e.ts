import { expect, test, type Page } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * GU-16/GU-18 · prueba visual de «Roles internos» (catálogo, solicitudes a cuatro ojos y el formulario con segundo
 * factor) y «Cuentas de servicio» en 3 viewports × 2 temas. **Backend simulado, declarado**: los endpoints se responden con
 * `page.route` y datos SINTÉTICOS con la forma del contrato de identidad. Prueba diseño, estados y consola, no al servicio.
 * Capturas en `.playwright/backoffice/visual/roles-y-cuentas/` (gitignored).
 */
const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const
const TEMAS = ['light', 'dark'] as const
const PERMISOS = ['ACCESOS_ADMINISTRAR', 'ROLES_INTERNOS_GESTIONAR', 'USUARIOS_INTERNOS_GESTIONAR']

const rol = (n: number, extra: Record<string, unknown> = {}) => ({
  id: `0b10c0e0-0000-4000-8000-00000000a0${n}`, codigo: `ROL_SINTETICO_${n}`, nombre: `Rol sintético ${n}`, ambito: 'GLOBAL', departamento: 'OPERACIONES',
  descripcion: 'Rol de prueba', esSistema: n === 1, permisos: ['TABLERO_VER'], ...extra,
})
const ROLES = [rol(1), rol(2), rol(3, { permisos: ['TABLERO_VER', 'RETIRO_APROBAR'] })]
const SOLICITUDES = [
  { id: '0b10c0e0-0000-4000-8000-00000000b001', tipo: 'CREAR', codigo: 'ROL_PENDIENTE', nombre: 'Rol pendiente de aprobación', permisos: ['RETIRO_APROBAR'], motivo: 'Nuevo puesto aprobado', propuestaPor: '0b10c0e0-0000-4000-8000-000000000999', propuestaEn: '2026-11-25T10:00:00Z' },
]
const permiso = (codigo: string, riesgo: string) => ({ id: `p-${codigo}`, codigo, descripcion: 'Permiso de prueba', recurso: 'x', accion: 'y', nivelRiesgo: riesgo, exigeMotivo: false, requiereMfa: false })
const PERMISOS_CATALOGO = [permiso('TABLERO_VER', 'BAJO'), permiso('RETIRO_APROBAR', 'ALTO'), permiso('ENTREGA_AUTORIZAR', 'CRITICO')]
const cuenta = (n: number, extra: Record<string, unknown> = {}) => ({
  id: `0b10c0e0-0000-4000-8000-00000000c0${n}`, usuarioId: `0b10c0e0-0000-4000-8000-00000000d0${n}`, rolCodigo: 'SERVICIO_ENTREGAS', nombre: `Servicio sintético ${n}`,
  estado: 'ACTIVA', venceEn: '2027-01-15T10:30:00Z', creadaEn: '2026-11-01T10:00:00Z', credencialRotadaEn: '2026-11-10T10:00:00Z', ...extra,
})
const CUENTAS = [cuenta(1), cuenta(2, { estado: 'REVOCADA', revocadaEn: '2026-11-20T10:00:00Z' })]

const json = (page: Page, patron: RegExp, status: number, cuerpo: unknown) => page.route(patron, (r) => r.fulfill({ status, contentType: 'application/json', body: JSON.stringify(cuerpo) }))

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
const captura = (page: Page, nombre: string, viewport: (typeof VIEWPORTS)[number], tema: string) =>
  page.screenshot({ path: `../../.playwright/backoffice/visual/roles-y-cuentas/${nombre}-${viewport.nombre}-${tema}.png`, animations: 'disabled' })

async function montarRoles(page: Page) {
  await json(page, /\/roles\?asignablesAPersonalInterno/, 200, ROLES)
  await json(page, /\/roles\/solicitudes/, 200, SOLICITUDES)
  await json(page, /\/permisos(\?|$)/, 200, PERMISOS_CATALOGO)
}

for (const viewport of VIEWPORTS) {
  for (const tema of TEMAS) {
    test(`roles internos con solicitudes — ${viewport.nombre} × ${tema}`, async ({ page }) => {
      await montarRoles(page)
      const errores = await abrir(page, viewport, tema, '/sistemas/roles-internos')
      await expect(page.getByText(/Rol sintético 2/).first()).toBeVisible()
      await expect(page.getByText(/Rol pendiente de aprobación/).first()).toBeVisible()
      await captura(page, 'roles-datos', viewport, tema)
      expect(errores).toEqual([])
    })

    test(`roles internos formulario de nuevo rol — ${viewport.nombre} × ${tema}`, async ({ page }) => {
      await montarRoles(page)
      const errores = await abrir(page, viewport, tema, '/sistemas/roles-internos')
      await page.getByRole('button', { name: 'Nuevo rol' }).click()
      await expect(page.getByRole('dialog')).toBeVisible()
      await expect(page.getByText(/RETIRO_APROBAR/).first()).toBeVisible()
      await captura(page, 'roles-formulario', viewport, tema)
      expect(errores).toEqual([])
    })

    for (const estado of [
      { nombre: 'datos', status: 200, cuerpo: CUENTAS, espera: /Servicio sintético 1/ },
      { nombre: 'vacio', status: 200, cuerpo: [], espera: /Todavía no hay cuentas de servicio/ },
      { nombre: 'error', status: 500, cuerpo: { codigo: 'AP-X', trazaId: 'traza-visual' }, espera: /No pudimos|salió mal|no está disponible/i },
    ] as const) {
      test(`cuentas de servicio ${estado.nombre} — ${viewport.nombre} × ${tema}`, async ({ page }) => {
        await json(page, /\/cuentas-servicio/, estado.status, estado.cuerpo)
        const errores = await abrir(page, viewport, tema, '/sistemas/cuentas-de-servicio')
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
        await expect(page.getByText(estado.espera).first()).toBeVisible()
        await captura(page, `cuentas-${estado.nombre}`, viewport, tema)
        expect(errores).toEqual([])
      })
    }
  }
}
