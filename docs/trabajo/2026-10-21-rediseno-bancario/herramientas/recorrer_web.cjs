// Recorrido REAL de la app web (mecanismo 2 del PLAN §0.1): la app compilada con
// `flutter build web --profile --no-web-resources-cdn --dart-define=API=http://localhost:4010/api/v1`
// servida en :8080, contra el simulado (`yarn workspace @aportaya/simulado mock`, :4010).
// Navega como una persona (por rol y texto, regla 80.3), captura cada paso, graba el video
// y FALLA si hay errores de consola o respuestas 4xx/5xx inesperadas.
// Uso: node recorrer_web.cjs <fase> [ancho] [alto]
const { chromium } = require('/opt/node-tools/node_modules/playwright')
const { mkdirSync, renameSync } = require('node:fs')
const { join } = require('node:path')

const fase = process.argv[2] ?? 'antes'
const ancho = Number(process.argv[3] ?? 412)
const alto = Number(process.argv[4] ?? 915)
const salida = join(__dirname, '..', 'evidencia', 'web', fase)
mkdirSync(salida, { recursive: true })

;(async () => {
  const navegador = await chromium.launch()
  const contexto = await navegador.newContext({
    viewport: { width: ancho, height: alto },
    // Sin idioma, Flutter web no arranca («Incorrect locale information provided»):
    // hallazgo de la versión web, registrado en revision-antes.md.
    locale: 'es-BO',
    recordVideo: { dir: salida, size: { width: ancho, height: alto } },
  })
  const p = await contexto.newPage()
  const problemas = []
  p.on('pageerror', (e) => problemas.push(`pageerror: ${e}`))
  p.on('console', (m) => {
    // El navegador loguea el 404 conocido como «Failed to load resource» sin la URL.
    if (m.type() === 'error' && !(m.text().includes('404') && m.location().url.endsWith('/api/v1/version'))) {
      problemas.push(`console: ${m.text()} (${m.location().url})`)
    }
  })
  // Fallas YA conocidas del antes (registradas en revision-antes.md); cualquier otra falla.
  const conocidas = [/\/api\/v1\/version$/]
  const conocida = (texto) => conocidas.some((re) => re.test(texto))
  p.on('response', (r) => r.status() >= 400 && !conocida(r.url()) && problemas.push(`${r.status()} ${r.url()}`))

  let n = 0
  const foto = async (nombre) => {
    await p.waitForTimeout(700) // deja asentar la transición (solo para la foto)
    await p.screenshot({ path: join(salida, `${String(++n).padStart(2, '0')}-${nombre}__${ancho}.png`) })
  }

  await p.goto('http://127.0.0.1:8080/')
  await p.waitForSelector('flutter-view', { state: 'attached', timeout: 60000 })
  // La semántica puede venir ya prendida; si Flutter ofrece el botón para prenderla, se toca.
  const placeholder = p.locator('flt-semantics-placeholder')
  await placeholder.waitFor({ state: 'attached', timeout: 5000 }).catch(() => {})
  if (await placeholder.count()) await placeholder.dispatchEvent('click')
  await p.getByRole('button', { name: 'Ya tengo cuenta' }).first().waitFor()
  await foto('portada')

  await p.getByRole('button', { name: 'Ya tengo cuenta' }).first().click()
  // Los campos del ingreso exponen el placeholder como nombre accesible, no la etiqueta
  // (hallazgo de a11y en revision-antes.md): se ubican por tipo hasta que H8 lo corrija.
  const celular = p.locator('input[type="tel"]').first()
  await celular.waitFor({ state: 'attached' })
  await foto('ingreso')
  // Se teclea como una persona: `fill` cambia el input sin que Flutter reciba los eventos.
  await celular.click()
  await p.keyboard.type('71234567')
  await p.locator('input[type="password"]').first().click()
  await p.keyboard.type('clave-sintetica-123')
  await foto('ingreso-lleno')
  await p.getByText('Ingresar', { exact: true }).first().click()
  // El simulado pide el segundo factor: se escriben los 6 dígitos y se confirma.
  await p.getByText('Verificación en dos pasos').first().waitFor({ timeout: 20000 })
  await foto('segundo-factor')
  // CampoOTP dibuja las celdas; su campo de texto se habilita al tocar una celda.
  const celda = await p.locator('input[aria-label^="Código de tu segundo factor"]').first().boundingBox()
  await p.mouse.click(celda.x + 20, celda.y + celda.height / 2)
  await p.keyboard.type('123456')
  // Con los 6 dígitos el campo envía solo; si «Confirmar» sigue a la vista, se toca.
  await p.getByText('Confirmar', { exact: true }).first().click({ timeout: 3000 }).catch(() => {})
  await p.getByText('Grupos', { exact: true }).first().waitFor({ timeout: 20000 })
  await foto('inicio')
  // La primera vez arranca el tutorial guiado: se fotografía y se deja.
  const dejar = p.getByText('Dejar el tutorial', { exact: true }).first()
  if (await dejar.isVisible().catch(() => false)) {
    await foto('tutorial')
    await dejar.click()
    await foto('inicio-sin-tutorial')
  }

  for (const pestana of ['Grupos', 'Movimientos', 'Perfil', 'Inicio']) {
    await p.getByText(pestana, { exact: true }).last().click()
    await foto(pestana.toLowerCase())
  }

  const video = await p.video().path()
  await contexto.close()
  renameSync(video, join(salida, `recorrido__${ancho}.webm`))
  await navegador.close()
  console.log(`${n} capturas + video en ${salida}`)
  if (problemas.length) {
    console.log('PROBLEMAS:\n' + problemas.join('\n'))
    process.exitCode = 1
  }
})()
