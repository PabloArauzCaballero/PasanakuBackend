// Mide el ancho del documento contra el viewport en cada vista (detecta scroll horizontal de página).
import { abrir, ir } from './sesion.mjs'
const { navegador, page } = await abrir()
const fila = (i) => ({ alertaId: `a-${i}`, reglaCodigo: `R-${i}`, tipologia: 'Fraccionamiento con un nombre de tipología muy largo que no debería romper la grilla a 375 px', severidad: 'ALTA', estado: 'ABIERTA', detectadaEn: '2026-10-01T14:00:00Z', casoId: null })
await page.route('**/api/v1/cumplimiento/alertas?*', (r) => r.fulfill({ json: { items: [1, 2].map(fila) } }))
for (const ruta of ['/cumplimiento/alertas', '/cumplimiento/casos', '/cumplimiento/uif/reportes', '/cumplimiento/uif/requerimientos', '/cumplimiento/continuidad', '/cumplimiento/actas', '/cumplimiento/riesgos/eventos', '/cumplimiento/seguridad/incidentes', '/cumplimiento/verificaciones']) {
  await ir(page, ruta)
  await page.locator('main#contenido table, main#contenido ap-tarjeta-de-expediente, main#contenido ap-estado-vacio').first().waitFor({ timeout: 30000 })
  const medidas = []
  for (const w of [1280, 768, 375]) {
    await page.setViewportSize({ width: w, height: 900 })
    await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(r))))
    medidas.push(`${w}→${await page.evaluate(() => document.documentElement.scrollWidth)}`)
  }
  await page.setViewportSize({ width: 1280, height: 900 })
  console.log(ruta, medidas.join(' '))
}
await navegador.close()
