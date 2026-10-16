// Capturas del backoffice contra el stack real (gateway :80 vía proxy de ng serve en :4300).
// Login REAL (USR000091, MFA de desarrollo 000000); la sesión vive en memoria, así que se navega sin recargar.
// Un solo navegador, un viewport a la vez (regla 70). Los teléfonos no se imprimen ni se capturan completos.
import { chromium } from '@playwright/test'
import { execSync } from 'node:child_process'
import { mkdirSync } from 'node:fs'

const SALIDA = process.argv[2]
const CLAVE = process.env.CLAVE_DEV
const base = 'http://localhost:4300'
const sql = (q) => execSync(`docker exec aportaya-postgres psql -U pasanaku -d pasanaku -Atc "${q}"`).toString().trim()
const ORG = sql("select o.id from organizador.organizador o join identidad.usuario u on u.id=o.usuario_id where u.codigo_publico='USR000001'")
const CUENTA = sql("select id from nucleo_financiero.cuenta_billetera where numero_cuenta='BOB-0000002'")
mkdirSync(SALIDA, { recursive: true })

const VIEWPORTS = [
  { nombre: 'movil', width: 390, height: 844 },
  { nombre: 'tablet', width: 820, height: 1180 },
  { nombre: 'escritorio', width: 1440, height: 900 },
]
const TEL = process.env.BO_TEL ?? '71000091'
const SOLO = (process.env.BO_SOLO ?? '').split(',').filter(Boolean)
const PREFIJO = process.env.BO_PREFIJO ?? 'bo'
const TODAS = [
  { id: 'organizador-habilitacion', ruta: `/cumplimiento/organizadores/${ORG}/habilitacion`, accion: 'habilitar' },
  { id: 'billetera-saldo', ruta: `/operacion/billetera/${CUENTA}` },
  { id: 'solicitudes-escaladas', ruta: '/operacion/solicitudes-escaladas' },
  { id: 'cola-verificaciones', ruta: '/cumplimiento/verificaciones' },
]
const PANTALLAS = SOLO.length ? TODAS.filter((p) => SOLO.includes(p.id)) : TODAS

const navegar = (page, ruta) => page.evaluate((r) => { history.pushState({}, '', r); window.dispatchEvent(new PopStateEvent('popstate')) }, ruta)

const navegador = await chromium.launch()
for (const vp of VIEWPORTS) {
  for (const tema of ['light', 'dark']) {
    const ctx = await navegador.newContext({ viewport: { width: vp.width, height: vp.height }, colorScheme: tema })
    const page = await ctx.newPage()
    const consola = []
    const red = []
    page.on('console', (m) => { if (m.type() === 'error') consola.push(m.text().slice(0, 160)) })
    page.on('response', (r) => { if (r.url().includes('/api/v1') && r.status() >= 400) red.push(`${r.status()} ${new URL(r.url()).pathname.replace(/[0-9a-f-]{36}/g, '<id>')}`) })
    await page.route('**/ingreso', async (route) => {
      const resp = await route.fetch()
      await route.fulfill({ response: resp, body: (await resp.text()).replace('</head>', '<meta name="aportaya-gateway" content="/api/v1"></head>') })
    }, { times: 1 })
    await page.goto(`${base}/ingreso`)
    await page.getByLabel('Teléfono').fill(TEL)
    await page.getByLabel('Contraseña', { exact: true }).fill(CLAVE)
    await page.getByRole('button', { name: 'Continuar' }).click()
    await page.getByLabel('Código de verificación').fill('000000')
    await page.waitForURL(/\/tablero$/, { timeout: 20000 })
    for (const p of PANTALLAS) {
      await navegar(page, p.ruta)
      await page.waitForTimeout(1500)
      if (p.accion === 'habilitar') {
        const boton = page.getByRole('button', { name: /habilitar/i }).first()
        if (await boton.isVisible().catch(() => false)) { await boton.click().catch(() => {}); await page.waitForTimeout(1500) }
      }
      await page.screenshot({ path: `${SALIDA}/${PREFIJO}-${p.id}-${vp.nombre}-${tema}.png`, fullPage: false })
    }
    console.log(`${vp.nombre}/${tema}: errores de consola=${consola.length} · respuestas 4xx/5xx=${JSON.stringify([...new Set(red)])}`)
    await ctx.close()
  }
}
await navegador.close()
