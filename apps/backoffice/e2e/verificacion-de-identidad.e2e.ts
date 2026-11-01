import { expect, test, type Page } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * CU-02 · la cola de verificación de identidad en tabla, la ficha del expediente y «Plataforma» fija en el menú
 * (trabajo `docs/trabajo/2026-10-28-verificacion-tabla-ficha-y-menu`).
 *
 * **Contra un doble declarado (regla 65)**: el ejemplo de Prism para `listarVerificaciones` trae un solo expediente
 * con textos de relleno (`nombreCompleto: "nombreCompleto"`), que no deja ver ni el caso límite ni el inválido. Se
 * intercepta `GET /identidad/verificaciones` con expedientes **sintéticos** con la forma exacta del contrato, y las
 * fotos con un SVG que dice qué cara es. La decisión (`POST …/decision`) también se responde acá: no hay backend de
 * identidad en este sandbox. Lo que esto NO prueba: la integración con el servicio real.
 */
const CINCO = ['ANVERSO', 'REVERSO', 'SELFIE', 'PERFIL_IZQUIERDO', 'PERFIL_DERECHO']
const EXPEDIENTES = [
  { verificacionId: '0f6199ea-0000-4000-8000-000000000001', usuarioId: '0f6199ea-0000-4000-8000-0000000000a1', nombreCompleto: 'Juana Sintética Quispe', telefono: '+59170000101', correo: 'juana@ejemplo.invalid', documento: 'CI SC', estado: 'EN_REVISION', iniciadaEn: '2026-10-20T13:10:00Z', fechaExpiracionDocumento: '2031-08-08', documentoVigente: true, fotos: CINCO },
  { verificacionId: '0f6199ea-0000-4000-8000-000000000002', usuarioId: '0f6199ea-0000-4000-8000-0000000000a2', nombreCompleto: 'Mario Ejemplo Choque', telefono: '+59170000102', documento: 'CI LP', estado: 'EN_REVISION', iniciadaEn: '2026-10-22T09:00:00Z', fechaExpiracionDocumento: '2024-01-31', documentoVigente: false, fotos: ['ANVERSO', 'REVERSO', 'SELFIE'] },
  { verificacionId: '0f6199ea-0000-4000-8000-000000000003', usuarioId: '0f6199ea-0000-4000-8000-0000000000a3', nombreCompleto: 'Lucía Prueba Vargas', telefono: '+59170000103', correo: 'lucia@ejemplo.invalid', documento: 'PASAPORTE', estado: 'PENDIENTE', iniciadaEn: '2026-10-27T18:30:00Z', documentoVigente: false, fotos: [] },
  { verificacionId: '0f6199ea-0000-4000-8000-000000000004', usuarioId: '0f6199ea-0000-4000-8000-0000000000a4', nombreCompleto: 'Pedro Ficticio Rojas', telefono: '+59170000104', correo: 'pedro@ejemplo.invalid', documento: 'CI CB', estado: 'APROBADA', iniciadaEn: '2026-10-10T10:00:00Z', resueltaEn: '2026-10-11T15:20:00Z', fechaExpiracionDocumento: '2029-02-01', documentoVigente: true, fotos: CINCO },
  { verificacionId: '0f6199ea-0000-4000-8000-000000000005', usuarioId: '0f6199ea-0000-4000-8000-0000000000a5', nombreCompleto: 'Ana Inventada Flores', telefono: '+59170000105', documento: 'CI OR', estado: 'RECHAZADA', iniciadaEn: '2026-10-09T10:00:00Z', resueltaEn: '2026-10-09T16:00:00Z', motivoRechazo: 'La selfie no coincide con la foto del anverso.', fechaExpiracionDocumento: '2030-06-15', documentoVigente: true, fotos: CINCO },
]
const PERMISOS = ['ver:cumplimiento', 'VERIFICACION_RESOLVER', 'exportar:expedientes-de-identidad', 'ver:sistemas', 'ver:configuracion', 'AUDITORIA_LEER']
const DIR = '../../docs/trabajo/2026-10-28-verificacion-tabla-ficha-y-menu/evidencia/capturas'

function foto(cara: string): string {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="320" height="200"><rect width="320" height="200" fill="#dfe7e2"/><text x="160" y="96" font-size="18" text-anchor="middle" fill="#1d3b2f">${cara}</text><text x="160" y="124" font-size="12" text-anchor="middle" fill="#1d3b2f">foto sintética</text></svg>`
}

/** El doble del servicio identidad, y un registro de lo que la consola y la red dicen mientras tanto. */
async function prepararDoble(page: Page): Promise<{ problemas: string[] }> {
  const problemas: string[] = []
  // Una copia por prueba: decidir en una prueba no puede cambiar los datos de la siguiente.
  const datos = structuredClone(EXPEDIENTES)
  page.on('console', (m) => { if (m.type() === 'error') problemas.push(`consola: ${m.text()}`) })
  page.on('response', (r) => { if (r.status() >= 400 && r.url().includes('/api/v1/identidad/verificaciones')) problemas.push(`red: ${r.status()} ${r.url()}`) })
  await page.route((u) => /\/api\/v1\/identidad\/verificaciones/.test(u.pathname), async (route) => {
    const req = route.request()
    const ruta = new URL(req.url()).pathname
    const cara = /\/fotos\/([A-Z_]+)\/contenido$/.exec(ruta)?.[1]
    if (cara) return route.fulfill({ status: 200, contentType: 'image/svg+xml', body: foto(cara) })
    if (req.method() === 'POST' && ruta.endsWith('/decision')) {
      const id = ruta.split('/').at(-2)
      const e = datos.find((x) => x.verificacionId === id)!
      const { decision, motivo } = req.postDataJSON() as { decision: string; motivo?: string }
      Object.assign(e, { estado: decision === 'APROBAR' ? 'APROBADA' : 'RECHAZADA', resueltaEn: new Date().toISOString(), motivoRechazo: motivo })
      return route.fulfill({ status: 200, json: e })
    }
    const estado = new URL(req.url()).searchParams.get('estado')
    return route.fulfill({ status: 200, json: estado ? datos.filter((e) => e.estado === estado) : datos })
  })
  return { problemas }
}

test('correcto: la cola es una tabla; «Revisar» abre la ficha con sus pestañas; aprobar resuelve y la cola se actualiza', async ({ page }) => {
  const { problemas } = await prepararDoble(page)
  await iniciarSesionDePrueba(page, PERMISOS, 'PLATAFORMA')
  await navegarSinRecargar(page, '/cumplimiento/verificaciones')
  const tabla = page.getByRole('table', { name: 'Verificación de identidad' })
  await expect(tabla.getByRole('row')).toHaveCount(4) // cabecera + los tres que esperan a una persona
  await expect(tabla).toContainText('Cédula de identidad · Santa Cruz')
  await expect(tabla).toContainText('3 de 5')
  await tabla.getByRole('link', { name: 'Revisar el expediente de Juana Sintética Quispe' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Juana Sintética Quispe' })).toBeVisible()
  await expect(page.getByRole('tab')).toHaveText(['Resumen', /Documento y fotos/, 'Decisión pendiente', 'Cómo funciona'])
  await page.getByRole('tab', { name: /Documento y fotos/ }).click()
  await page.getByRole('button', { name: 'Ver las fotos' }).click()
  await expect(page.locator('ap-tira-de-fotos img')).toHaveCount(5)
  await page.getByRole('tab', { name: 'Decisión pendiente' }).click()
  await page.getByRole('button', { name: 'Aprobar' }).click()
  await expect(page.getByText('Resultado: Aprobada')).toBeVisible()
  await page.getByRole('link', { name: 'Volver a la cola' }).click()
  await expect(page.getByRole('table', { name: 'Verificación de identidad' }).getByRole('row')).toHaveCount(3)
  expect(problemas).toEqual([])
})

test('límite e inválido: el menú fija «Plataforma»; un expediente sin fotos ni fecha explica por qué no se aprueba; un id inexistente lo dice', async ({ page }) => {
  const { problemas } = await prepararDoble(page)
  await iniciarSesionDePrueba(page, PERMISOS, 'PLATAFORMA')
  const menu = page.getByRole('navigation', { name: 'Secciones del backoffice' })
  await expect(menu.getByRole('link', { name: 'Plataforma' })).toHaveAttribute('href', '/sistemas/servicios')
  await expect(menu.locator('[aria-expanded]')).toHaveCount(0)
  await navegarSinRecargar(page, '/cumplimiento/verificaciones/0f6199ea-0000-4000-8000-000000000003?vista=decision')
  await expect(page.getByText('Por qué no se puede aprobar todavía')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Aprobar' })).toBeDisabled()
  await navegarSinRecargar(page, '/cumplimiento/verificaciones/no-existe')
  await expect(page.getByText('Este expediente no está en la cola de verificación.')).toBeVisible()
  expect(problemas).toEqual([])
})

const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const

for (const v of VIEWPORTS) {
  for (const tema of ['light', 'dark'] as const) {
    test(`capturas — ${v.nombre} × ${tema}`, async ({ page }) => {
      await page.setViewportSize({ width: v.width, height: v.height })
      await page.emulateMedia({ colorScheme: tema })
      await prepararDoble(page)
      await iniciarSesionDePrueba(page, PERMISOS, 'PLATAFORMA')
      await navegarSinRecargar(page, '/cumplimiento/verificaciones')
      await expect(page.getByRole('table', { name: 'Verificación de identidad' })).toBeVisible()
      await page.screenshot({ path: `${DIR}/bandeja.${v.nombre}.${tema}.png`, fullPage: true })
      const base = '/cumplimiento/verificaciones/0f6199ea-0000-4000-8000-000000000002'
      for (const vista of ['resumen', 'fotos', 'decision', 'como-funciona']) {
        await navegarSinRecargar(page, `${base}?vista=${vista}`)
        await expect(page.getByRole('tab', { selected: true })).toBeVisible()
        if (vista === 'fotos') {
          await page.getByRole('button', { name: 'Ver las fotos' }).click()
          await expect(page.locator('ap-tira-de-fotos img')).toHaveCount(3)
        }
        await page.screenshot({ path: `${DIR}/ficha-${vista}.${v.nombre}.${tema}.png`, fullPage: true })
      }
    })
  }
}
