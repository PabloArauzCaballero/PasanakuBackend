import { expect, test, type Page } from '@playwright/test'
import { capturar, todas } from './apoyo/capturas-f2'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'
import { CATALOGO, servirCatalogo } from './apoyo/tableros-sinteticos'

/**
 * CU-130 y CU-131 · el catálogo de datos como el portal de Atlas: la lista con cifras y filtros, la ficha con
 * pestañas en la URL, «Configurar tabla» con confirmación, y la revisión de cada columna desde un cajón.
 * **Las respuestas de auditoria son un DOBLE declarado**: prueba la pantalla y su apariencia; la integración con
 * el backend real se verifica en TEST (REPORTE).
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
  await servirCatalogo(page, CON_GOBIERNO)
  await iniciarSesionDePrueba(page, permisos)
  await navegarSinRecargar(page, ruta)
}

/** Captura el cuerpo del PUT que llegue a `sufijo` y responde `respuesta`. */
async function capturarPut(page: Page, sufijo: RegExp, respuesta: unknown): Promise<() => unknown> {
  let cuerpo: unknown
  await page.route(
    (url) => sufijo.test(url.pathname),
    async (route) => {
      cuerpo = route.request().postDataJSON()
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(respuesta) })
    },
  )
  return () => cuerpo
}

test('lista: cifras del inventario, chips de estado y revisión, y filtro por estado desde la URL', async ({ page }) => {
  await abrir(page, ['TABLERO_SISTEMAS_VER'], '/tablero/catalogo-de-datos')
  await expect(page.getByRole('heading', { level: 1, name: 'Catálogo de datos' })).toBeVisible()
  await expect(page.getByText('Por revisar')).toBeVisible()
  await expect(page.getByText('2 fichas aprobadas')).toBeVisible()
  await expect(page.locator('ap-chip-estado', { hasText: 'Aprobada' }).first()).toBeVisible()
  await todas(page, P, 'lista')

  await page.getByRole('combobox', { name: 'Estado' }).selectOption('CANDIDATA_A_RETIRO')
  await expect(page).toHaveURL(/estado=CANDIDATA_A_RETIRO/)
  await expect(page.locator('ap-tabla-de-datos .total')).toHaveText('1')
  await expect(page.getByRole('link', { name: 'ticket_soporte', exact: true })).toBeVisible()
  await capturar(page, P, 'filtrada', 'escritorio', 'light')
})

test('ficha: pestañas en la URL, y «Configurar tabla» confirma antes de guardar con la versión leída', async ({ page }) => {
  const cuerpo = await capturarPut(page, /\/catalogo-de-datos\/aportes\/pago\/gobierno$/, { ...FICHAS['aportes.pago'], estadoRevision: 'APROBADA', version: 2 })
  await abrir(page, ['TABLERO_SISTEMAS_VER', 'CATALOGO_DATOS_EDITAR'], '/tablero/catalogo-de-datos/aportes/pago')
  await expect(page.getByRole('heading', { level: 1, name: 'aportes.pago' })).toBeVisible()
  await expect(page.getByRole('tab', { name: 'Vista general' })).toHaveAttribute('aria-selected', 'true')
  await todas(page, P, 'ficha')

  await page.getByRole('tab', { name: 'Relaciones' }).click()
  await expect(page).toHaveURL(/vista=relaciones/)
  await capturar(page, P, 'ficha-relaciones', 'escritorio', 'dark')

  await page.getByRole('button', { name: 'Editar ficha' }).click()
  await expect(page).toHaveURL(/\/catalogo-de-datos\/aportes\/pago\/ficha$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Configurar tabla' })).toBeVisible()
  await todas(page, P, 'configurar')

  await page.getByRole('combobox', { name: 'Revisión' }).selectOption('APROBADA')
  await page.getByRole('button', { name: 'Guardar ficha' }).click()
  const dialogo = page.getByRole('dialog', { name: 'Guardar ficha de la tabla' })
  await expect(dialogo).toContainText('aportes.pago en el catálogo de datos (1 campo)')
  await capturar(page, P, 'configurar-confirmar', 'movil', 'light')
  await dialogo.getByRole('button', { name: 'Guardar ficha' }).click()

  await expect(page).toHaveURL(/\/catalogo-de-datos\/aportes\/pago\?vista=gobierno$/)
  expect(cuerpo()).toMatchObject({ estadoRevision: 'APROBADA', version: 1, responsable: 'Tesorería' })
  await capturar(page, P, 'ficha-gobierno', 'tablet', 'light')
})

test('ficha: la narrativa de negocio conserva sus viñetas', async ({ page }) => {
  await abrir(page, ['TABLERO_SISTEMAS_VER'], '/tablero/catalogo-de-datos/aportes/pago')
  const contexto = page.getByRole('region', { name: 'Contexto de negocio' })
  await expect(contexto.getByRole('listitem').filter({ hasText: 'montoComisionProveedor' })).toBeVisible()
  await todas(page, P, 'ficha-narrativa')
})

test('ficha: «Operaciones» conserva las directas y lista los casos de uso que nombran la tabla con sus endpoints', async ({ page }) => {
  await abrir(page, ['TABLERO_SISTEMAS_VER'], '/tablero/catalogo-de-datos/aportes/pago')
  await page.getByRole('tab', { name: /Operaciones/ }).click()
  await expect(page).toHaveURL(/vista=operaciones/)
  // Las dos fuentes, una debajo de la otra: el catálogo de operaciones (directas) y los casos de uso de la bóveda.
  await expect(page.getByRole('region', { name: 'Operaciones que usan la tabla' })).toBeVisible()
  const cu21 = page.getByRole('list', { name: 'Casos de uso que la nombran' }).getByRole('listitem').filter({ has: page.getByRole('heading', { name: /CU-21/ }) })
  const cobrar = cu21.getByRole('list', { name: 'Endpoints de CU-21' }).getByRole('listitem').filter({ hasText: 'cobrarAporte' })
  await expect(cobrar).toContainText('/aportes/obligaciones/{obligacionId}/pagos')
  await expect(cobrar.getByText('POST', { exact: true })).toBeVisible()
  await todas(page, P, 'ficha-operaciones')
})

test('ficha: «Operaciones directas» con filas: la tabla se ve sin desborde (tabla SINTÉTICA declarada, nombrada como la nombra el inventario empaquetado)', async ({ page }) => {
  // DOBLE declarado: el inventario de operaciones empaquetado solo nombra una tabla, `outbox_evento`, que no existe en el
  // catálogo real; se agrega una copia sintética de `evento_dominio` con ese nombre para ver la sección con datos.
  const base = (CATALOGO.tablas as Tabla[]).find((t) => t.tabla === 'evento_dominio')!
  await servirCatalogo(page, { ...CON_GOBIERNO, tablas: [...(CON_GOBIERNO.tablas as Tabla[]), { ...base, tabla: 'outbox_evento', operaciones: [] }] })
  await iniciarSesionDePrueba(page, ['TABLERO_SISTEMAS_VER'])
  await navegarSinRecargar(page, '/tablero/catalogo-de-datos/aportes/outbox_evento?vista=operaciones')
  const directas = page.getByRole('region', { name: 'Operaciones que usan la tabla' })
  await expect(directas.getByRole('table')).toBeVisible()
  await expect(directas.getByRole('row').nth(1)).toBeVisible()
  await expect(directas.getByText('Sin operaciones directas registradas')).toHaveCount(0)
  await todas(page, P, 'ficha-operaciones-directas')
})

test('ficha de infraestructura: «Operaciones» muestra los dos vacíos y los dos dicen por qué y qué hacer', async ({ page }) => {
  await abrir(page, ['TABLERO_SISTEMAS_VER'], '/tablero/catalogo-de-datos/aportes/evento_dominio')
  await page.getByRole('tab', { name: /Operaciones/ }).click()
  await expect(page).toHaveURL(/vista=operaciones/)
  const directas = page.getByRole('region', { name: 'Operaciones que usan la tabla' })
  await expect(directas.getByText('Sin operaciones directas registradas')).toBeVisible()
  await expect(directas.getByText(/inventario parcial/)).toBeVisible()
  await expect(page.getByText('Ningún caso de uso la nombra')).toBeVisible()
  await expect(page.getByText(/falta enlazarla en el caso de uso que la toca/)).toBeVisible()
  await todas(page, P, 'ficha-operaciones-infra')
})

test('columnas: revisar desde el cajón exige motivo al rechazar y guarda con la versión leída', async ({ page }) => {
  const cuerpo = await capturarPut(page, /\/catalogo-de-datos\/aportes\/pago\/columnas\/id\/revision$/, { decision: 'RECHAZADA', motivo: 'No es un identificador de negocio', version: 1 })
  await abrir(page, ['TABLERO_SISTEMAS_VER', 'CATALOGO_DATOS_EDITAR'], '/tablero/catalogo-de-datos/aportes/pago?vista=columnas')
  await expect(page.getByRole('tab', { name: 'Columnas' })).toHaveAttribute('aria-selected', 'true')
  await expect(page.getByRole('cell', { name: 'Sin revisar' }).first()).toBeVisible()
  await page.getByRole('button', { name: 'Revisar' }).first().click()
  const cajon = page.getByRole('dialog', { name: 'Revisar columna id' })
  await expect(cajon).toBeVisible()
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished)))
  await todas(page, P, 'revision')

  await cajon.getByRole('combobox', { name: 'Decisión' }).selectOption('RECHAZADA')
  await cajon.getByRole('button', { name: 'Guardar revisión' }).click()
  await expect(cajon.getByText('Para rechazar hay que decir por qué.')).toBeVisible()
  expect(cuerpo()).toBeUndefined()
  await capturar(page, P, 'revision-sin-motivo', 'movil', 'dark')

  await cajon.getByRole('textbox', { name: 'Motivo' }).fill('No es un identificador de negocio')
  await cajon.getByRole('button', { name: 'Guardar revisión' }).click()
  await expect(cajon).toBeHidden()
  expect(cuerpo()).toEqual({ decision: 'RECHAZADA', motivo: 'No es un identificador de negocio', version: 0 })
})

test('sin permiso de edición: se lee, no se edita, y la revisión de una columna solo se mira', async ({ page }) => {
  await abrir(page, ['TABLERO_SISTEMAS_VER'], '/tablero/catalogo-de-datos/aportes/pago?vista=columnas')
  await expect(page.getByRole('button', { name: 'Editar ficha' })).toHaveCount(0)
  await page.getByRole('button', { name: 'Ver revisión' }).first().click()
  const cajon = page.getByRole('dialog', { name: 'Revisar columna id' })
  await expect(cajon.getByText('hace falta el permiso de edición')).toBeVisible()
  await expect(cajon.getByRole('button', { name: 'Guardar revisión' })).toHaveCount(0)
  await capturar(page, P, 'revision-lectura', 'escritorio', 'dark')
})
