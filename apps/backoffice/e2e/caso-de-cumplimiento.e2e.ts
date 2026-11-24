import { expect, test, type Page } from '@playwright/test'
import { prepararArranque } from './apoyo/arranque-e2e'

/**
 * PR6-SmartPresentational.Frontend, H4 — E2E dirigido de la pantalla piloto
 * (`cumplimiento/casos/pantalla-de-caso.ts`) tras separarla en contenedor
 * (`PantallaDeCaso`) y presentación pura (`FormularioDeCaso`), más la comparación
 * visual que pide el cierre del carril: 3 viewports × 2 temas, estado inicial y con
 * causal elegida.
 *
 * **Este worktree (`PasanakuBackend-richard-pr6`) NO tiene el `AuthBootstrap`/
 * `RefrescoDeSesion` de `PR11`** (son ramas distintas del mismo `origin/dev`): la sesión
 * vive solo en memoria y no sobrevive a una recarga real de página (el mismo hallazgo que
 * documenta `tablero-y-permisos.e2e.ts` del carril F12). Por eso:
 *
 * 1. El login se hace por el formulario real (`POST /sesiones`), interceptado para
 *    devolver un token con las claims reales que la pantalla necesita — el mock de Prism
 *    siempre manda el literal `"tokenAcceso"` (sin claims, confirmado en
 *    `packages/simulado/ejemplos/identidad/autenticar.json`), así que con el mock puro no
 *    hay forma de entrar con un permiso real.
 * 2. Para llegar a `/cumplimiento/casos/:id` sin perder la sesión en memoria, la
 *    navegación es **client-side**: `history.pushState` + `popstate` (lo que Angular's
 *    `PathLocationStrategy` escucha para sincronizar el Router con back/forward del
 *    navegador) — un `page.goto` a otra ruta recarga la página entera y tira el token,
 *    exactamente el defecto que documenta el hallazgo de F12.
 */
function tokenConClaims(claims: Record<string, unknown>): string {
  const base64url = (s: string) => Buffer.from(s).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  return `${base64url(JSON.stringify({ alg: 'none' }))}.${base64url(JSON.stringify(claims))}.firma-e2e`
}

/** Un caso con forma de UUID: el simulado sirve la ficha de `obtenerCasoLft` (escenario `ok`, datos sintéticos). */
const CASO = '66947425-f599-4f17-86cf-204ce5c2e033'
const CONTRATO = '../../docs/trabajo/2026-11-23-continuar-traspaso/evidencia/contrato'

const TOKEN_CUMPLIMIENTO = tokenConClaims({ sub: 'e2e-oficial', permisos: ['ver:cumplimiento'], rol: 'oficial' })

async function entrarConPermiso(page: Page): Promise<void> {
  // F4 (TEST_BUG heredado de la integración con el AuthBootstrap): el arranque restaura la sesión con `POST /sesion/refrescar`, que Prism no
  // tiene; sin este doble el 404 ensuciaba la consola. `prepararArranque` lo responde 401 limpio, como un navegador sin cookie.
  await prepararArranque(page)
  await page.route('**/sesiones', (route) =>
    route.fulfill({ status: 200, json: { requiereFactorAdicional: false, dispositivoConfiable: true, tokenAcceso: TOKEN_CUMPLIMIENTO } }),
  )
  await page.goto('/ingreso')
  await page.getByRole('textbox', { name: 'Correo o teléfono' }).fill('71234567')
  await page.getByRole('textbox', { name: 'Contraseña' }).fill('una-contrasena-larga')
  await page.getByRole('button', { name: 'Continuar' }).click()
  await expect(page).toHaveURL(/\/tablero$/)

  // Navegación client-side: un `page.goto` a otra ruta recarga la SPA entera y pierde el
  // token en memoria (no hay bootstrap que lo restaure en este worktree).
  await page.evaluate((ruta: string) => {
    history.pushState(null, '', ruta)
    window.dispatchEvent(new PopStateEvent('popstate'))
  }, `/cumplimiento/casos/${CASO}`)
  await expect(page).toHaveURL(new RegExp(`/cumplimiento/casos/${CASO}`))
  // La URL cambia con el `pushState` antes de que el Router pinte la ruta: sin esperar el título del caso, las
  // capturas «inicial» salían del tablero (TEST_BUG visto en las capturas del 2026-11-24).
  await expect(page.locator('h1')).toHaveText('Caso de cumplimiento')
}

test.describe('pantalla de caso — E2E dirigido', () => {
  test('carga con la escalera de etapas, el botón deshabilitado sin causal, y el título real', async ({ page }) => {
    await entrarConPermiso(page)
    await expect(page.locator('h1')).toHaveText('Caso de cumplimiento')
    await expect(page.getByRole('button', { name: 'Confirmar decisión' })).toBeDisabled()
    await expect(page.getByText('Elegí una causal del catálogo para habilitar la confirmación.')).toBeVisible()
  })

  test('elegir una causal habilita "Confirmar decisión" — el contenedor y la presentación se comunican de verdad', async ({ page }) => {
    await entrarConPermiso(page)
    await page.getByRole('radio').first().check()
    await expect(page.getByRole('button', { name: 'Confirmar decisión' })).toBeEnabled()
  })

  test('escribir en la narrativa no se pierde al re-renderizar (el contenedor sigue siendo la fuente de verdad)', async ({ page }) => {
    await entrarConPermiso(page)
    const area = page.getByLabel('Narrativa')
    await area.fill('La contraparte fraccionó cinco depósitos el mismo día.')
    await page.getByRole('radio').first().check()
    await expect(area).toHaveValue('La contraparte fraccionó cinco depósitos el mismo día.')
  })

  test('el resumen trae del contrato la alerta de origen y el titular enmascarado, sin el aviso de «no publicado»', async ({ page }) => {
    await entrarConPermiso(page)
    const resumen = page.getByRole('region', { name: /Caso/ }).first()
    await expect(resumen).toContainText('RM-FRACCIONAMIENTO')
    await expect(resumen).toContainText('Fraccionamiento de operaciones')
    await expect(resumen).toContainText('****7c2e')
    await expect(resumen).toContainText('LFT-2026-0042')
    await expect(page.getByText('todavía no los publica')).toHaveCount(0)
    for (const [vp, tema] of [[VIEWPORTS[0], 'claro'], [VIEWPORTS[2], 'claro'], [VIEWPORTS[0], 'oscuro'], [VIEWPORTS[2], 'oscuro']] as const) {
      await page.setViewportSize({ width: vp.width, height: vp.height })
      await page.emulateMedia({ colorScheme: tema === 'oscuro' ? 'dark' : 'light' })
      await expect(resumen).toContainText('****7c2e')
      await resumen.screenshot({ path: `${CONTRATO}/caso-resumen-alerta-titular.${vp.nombre}.${tema}.png`, animations: 'disabled' })
    }
  })

  test('consola y red sin errores nuevos al cargar', async ({ page }) => {
    const erroresDeConsola: string[] = []
    page.on('console', (msg) => {
      // El único «Failed to load resource» esperado es el 401 del refresco de sesión que responde `prepararArranque` a propósito.
      if (msg.type() === 'error' && !/status of 401/.test(msg.text())) erroresDeConsola.push(msg.text())
    })
    const respuestasMalas: number[] = []
    page.on('response', (r) => {
      if (r.status() >= 400 && !r.url().includes('/sesiones') && !r.url().includes('/sesion/refrescar')) respuestasMalas.push(r.status())
    })
    await entrarConPermiso(page)
    await page.waitForLoadState('networkidle')
    expect(erroresDeConsola, `errores de consola: ${erroresDeConsola.join(' | ')}`).toHaveLength(0)
    expect(respuestasMalas, `respuestas 4xx/5xx inesperadas: ${respuestasMalas.join(', ')}`).toHaveLength(0)
  })
})

const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 834, height: 1194 },
  { nombre: 'escritorio', width: 1440, height: 900 },
] as const

test.describe('pantalla de caso — disposición', () => {
  test('móvil: la causal queda en el primer pliegue y la escalera se despliega a pedido', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await entrarConPermiso(page)
    await expect(page.getByRole('radiogroup', { name: 'Causal del catálogo' })).toBeVisible()
    const primera = page.getByRole('radio').first()
    await expect(primera).toBeInViewport()
    const desplegar = page.getByText('Ver las 6 etapas del debido proceso')
    await desplegar.click()
    await expect(page.getByRole('list', { name: 'Etapa 1 de 6: Causal' })).toBeVisible()
    await page.screenshot({ path: '../../evidencia/visual-caso-movil-claro-etapas-desplegadas.png', fullPage: true, animations: 'disabled' })
  })

  test('escritorio: la escalera es un riel lateral junto al formulario', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await entrarConPermiso(page)
    const riel = page.getByRole('complementary', { name: 'Debido proceso' })
    await expect(riel).toBeVisible()
    await expect(page.getByText('Ver las 6 etapas del debido proceso')).toBeHidden()
    const cajaRiel = await riel.boundingBox()
    const cajaCausal = await page.getByRole('radiogroup', { name: 'Causal del catálogo' }).boundingBox()
    expect(cajaRiel && cajaCausal && cajaRiel.x + cajaRiel.width <= cajaCausal.x).toBe(true)
  })
})

test.describe('pantalla de caso — comparación visual (3 viewports × 2 temas)', () => {
  for (const vp of VIEWPORTS) {
    for (const tema of ['claro', 'oscuro'] as const) {
      test(`${vp.nombre} · ${tema} · estado inicial (sin causal)`, async ({ page }) => {
        await page.setViewportSize({ width: vp.width, height: vp.height })
        await page.emulateMedia({ colorScheme: tema === 'oscuro' ? 'dark' : 'light' })
        await entrarConPermiso(page)
        await expect(page.locator('h1')).toBeVisible()
        await page.screenshot({ path: `../../evidencia/visual-caso-${vp.nombre}-${tema}-inicial.png` })
      })

      test(`${vp.nombre} · ${tema} · con causal elegida`, async ({ page }) => {
        await page.setViewportSize({ width: vp.width, height: vp.height })
        await page.emulateMedia({ colorScheme: tema === 'oscuro' ? 'dark' : 'light' })
        await entrarConPermiso(page)
        await page.getByRole('radio').first().check()
        await expect(page.getByRole('button', { name: 'Confirmar decisión' })).toBeEnabled()
        await page.screenshot({ path: `../../evidencia/visual-caso-${vp.nombre}-${tema}-con-causal.png`, fullPage: true, animations: 'disabled' })
      })
    }
  }
})
