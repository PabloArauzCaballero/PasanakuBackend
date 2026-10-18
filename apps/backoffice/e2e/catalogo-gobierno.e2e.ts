import { expect, test, type Page } from '@playwright/test'
import { capturar, todas } from './apoyo/capturas-f2'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'
import { CATALOGO, servir } from './apoyo/tableros-sinteticos'

/**
 * CU-130 · el catálogo de datos con la ficha de gobierno de cada tabla, como el portal de Atlas: cifras, filtros por
 * estado, chips de estado y revisión, y la edición desde la ficha. **Las respuestas de auditoria son un DOBLE
 * declarado**: prueba la pantalla y su apariencia; la integración con el backend real se verifica en TEST (REPORTE).
 */
const P = 'cu130'
const RETENCION = '00000000-0000-4000-8000-0000000000c1'

type Tabla = { tabla: string; esquema: string; gobierno?: unknown }
const conGobierno = (estado: string, revision: string, responsable?: string) => ({
  estado,
  estadoRevision: revision,
  criticaParaAuditoria: estado === 'ACTIVA',
  version: 1,
  ...(responsable ? { responsable, politicaRetencionId: RETENCION } : {}),
})
const FICHAS: Record<string, ReturnType<typeof conGobierno>> = {
  'aportes.obligacion_aporte': conGobierno('ACTIVA', 'APROBADA', 'Tesorería'),
  'aportes.pago': conGobierno('ACTIVA', 'NECESITA_REVISION', 'Tesorería'),
  'auditoria.ticket_soporte': conGobierno('CANDIDATA_A_RETIRO', 'APROBADA', 'Sistemas'),
  'catalogo.umbral_operativo': conGobierno('RETIRADA', 'RECHAZADA'),
}
const CON_GOBIERNO = {
  ...CATALOGO,
  tablas: (CATALOGO.tablas as Tabla[]).map((t) => ({ ...t, gobierno: FICHAS[`${t.esquema}.${t.tabla}`] })),
  politicasDeRetencion: [{ id: RETENCION, entidad: 'registros contables', mesesRetencionActiva: 120, accionAlVencer: 'ARCHIVAR' }],
}

async function abrir(page: Page, permisos: string[], ruta: string) {
  await servir(page, '/indicadores/catalogo-de-datos', { cuerpo: CON_GOBIERNO })
  await iniciarSesionDePrueba(page, permisos)
  await navegarSinRecargar(page, ruta)
}

test('lista: cifras del inventario, chips de estado y revisión, y filtro por estado desde la URL', async ({ page }) => {
  await abrir(page, ['TABLERO_SISTEMAS_VER'], '/tablero/catalogo-de-datos')
  await expect(page.getByRole('heading', { level: 1, name: 'Catálogo de datos' })).toBeVisible()
  await expect(page.getByText('Hacia el retiro')).toBeVisible()
  await expect(page.locator('ap-chip-estado', { hasText: 'Aprobada' }).first()).toBeVisible()
  await todas(page, P, 'lista')

  await page.getByLabel('Estado').selectOption('CANDIDATA_A_RETIRO')
  await expect(page).toHaveURL(/estado=CANDIDATA_A_RETIRO/)
  await expect(page.getByText(/^1 de \d+ tablas$/)).toBeVisible()
  await expect(page.getByRole('link', { name: 'ticket_soporte', exact: true })).toBeVisible()
  await capturar(page, P, 'filtrada', 'escritorio', 'light')
})

test('ficha: quien tiene permiso edita el gobierno y guarda con la versión leída', async ({ page }) => {
  let cuerpo: unknown
  await page.route(
    (url) => url.pathname.endsWith('/api/v1/indicadores/catalogo-de-datos/aportes/pago/gobierno'),
    async (route) => {
      cuerpo = route.request().postDataJSON()
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ ...FICHAS['aportes.pago'], estadoRevision: 'APROBADA', version: 2 }) })
    },
  )
  await abrir(page, ['TABLERO_SISTEMAS_VER', 'CATALOGO_DATOS_EDITAR'], '/tablero/catalogo-de-datos/pago')
  await expect(page.getByRole('heading', { level: 2, name: 'Gobierno de la tabla' })).toBeVisible()
  await todas(page, P, 'ficha')

  await page.getByLabel('Revisión', { exact: true }).selectOption('APROBADA')
  await page.getByRole('button', { name: 'Guardar ficha' }).click()
  await expect(page.getByText('Ficha guardada. El cambio quedó en la bitácora.')).toBeVisible()
  expect(cuerpo).toMatchObject({ estadoRevision: 'APROBADA', version: 1, responsable: 'Tesorería' })
  await capturar(page, P, 'ficha-guardada', 'movil', 'light')
})

test('ficha sin permiso de edición: se lee, no se edita', async ({ page }) => {
  await abrir(page, ['TABLERO_SISTEMAS_VER'], '/tablero/catalogo-de-datos/pago')
  await expect(page.getByText('Para editar esta ficha hace falta el permiso')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Guardar ficha' })).toHaveCount(0)
  await capturar(page, P, 'ficha-lectura', 'escritorio', 'dark')
})
