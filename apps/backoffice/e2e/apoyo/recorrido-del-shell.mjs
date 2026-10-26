// Recorrido del shell en un navegador real (DOBLE DE SESIÓN, como `sonda-de-navegacion.mjs`): ingreso (aceptado,
// límite, error), cada botón de la cabecera y del menú a 1280 y a 375 px, y las capturas del shell. Las respuestas de
// error (401, 500, colección vacía) se simulan con `page.route`; el resto va al simulado OpenAPI (sin estado).
// Uso: `PERMISOS_ARCHIVO=permisos.txt node e2e/apoyo/recorrido-del-shell.mjs [baseURL] [carpeta-de-evidencia]`.
// No es una prueba de la suite: es el recorrido que deja la evidencia de la franja A.
import { chromium } from 'playwright'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'

const base = process.argv[2] ?? 'http://127.0.0.1:4300'
const carpeta = process.argv[3] ?? 'recorrido'
mkdirSync(carpeta, { recursive: true })
const permisos = (process.env.PERMISOS_ARCHIVO ? readFileSync(process.env.PERMISOS_ARCHIVO, 'utf8') : '').split(/\s+/).filter(Boolean)
const token = (rol) => `r.${Buffer.from(JSON.stringify({ permisos, rol, sub: 'recorrido' })).toString('base64url')}.f`

const resultados = []
const errores = []
function verificar(nombre, ok, detalle = '') {
  resultados.push({ nombre, veredicto: ok ? 'PASS' : 'FAIL', detalle })
  console.log(`${ok ? 'PASS' : 'FAIL'} ${nombre}${detalle ? ' · ' + detalle : ''}`)
}
async function intento(nombre, fn) {
  try {
    await fn()
  } catch (e) {
    verificar(nombre, false, String(e).split('\n')[0].slice(0, 200))
  }
}

const navegador = await chromium.launch({ args: ['--disable-features=LocalNetworkAccessChecks'] })

async function pagina({ ancho = 1280, alto = 900, oscuro = false, rol = 'PLATAFORMA', entrar = true } = {}) {
  const contexto = await navegador.newContext({ viewport: { width: ancho, height: alto }, colorScheme: oscuro ? 'dark' : 'light', acceptDownloads: true })
  const page = await contexto.newPage()
  page.on('console', (m) => m.type() === 'error' && errores.push({ url: page.url().replace(base, ''), tipo: 'consola', texto: m.text().slice(0, 300) }))
  page.on('pageerror', (e) => errores.push({ url: page.url().replace(base, ''), tipo: 'excepcion', texto: String(e).slice(0, 300) }))
  page.on('response', (r) => r.status() >= 500 && r.url().includes('/api/') && !page.__errorEsperado && errores.push({ url: page.url().replace(base, ''), tipo: 'red', texto: `${r.status()} ${r.url().replace(base, '')}` }))
  await page.route('**/ingreso', async (route) => {
    const r = await route.fetch()
    await route.fulfill({ response: r, body: (await r.text()).replace('</head>', '<meta name="aportaya-gateway" content="/api/v1"></head>') })
  }, { times: 1 })
  if (entrar) await ingresar(page, rol)
  return page
}

async function ingresar(page, rol) {
  await page.route('**/api/v1/sesiones', async (route) => {
    if (!route.request().postDataJSON()?.factor) return route.continue()
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ sesionId: 's', expiraEn: new Date(Date.now() + 3.6e6).toISOString(), requiereFactorAdicional: false, dispositivoConfiable: true, tokenAcceso: token(rol) }) })
  })
  await page.goto(base + '/ingreso')
  await page.getByLabel('Correo o teléfono').fill('71234567')
  await page.getByLabel('Contraseña', { exact: true }).fill('contrasena-e2e')
  await page.getByRole('button', { name: 'Continuar' }).click()
  await page.getByLabel('Código de verificación').fill('123456')
  await page.waitForURL(/\/tablero$/, { timeout: 30000 })
  await page.unroute('**/api/v1/sesiones')
}

/** Espera a que terminen las animaciones (una captura a mitad de una transición no prueba nada). */
const quieto = (page) => page.waitForFunction(() => document.getAnimations().every((a) => a.playState !== 'running'), null, { timeout: 5000 }).catch(() => {})

/** Navega dentro de la SPA sin recargar (la sesión vive en memoria). */
async function ir(page, ruta) {
  await page.evaluate((r) => { history.pushState({}, '', r); dispatchEvent(new PopStateEvent('popstate')) }, ruta)
  await page.waitForURL((u) => u.pathname.startsWith(ruta.split('?')[0]))
}

// ── Ingreso (CU-04) ──────────────────────────────────────────────────────────────────────────────────────────────
{
  const page = await pagina({ entrar: false })
  await page.goto(base + '/ingreso')
  let pedidos = 0
  await page.route('**/api/v1/sesiones', (r) => { pedidos++; return r.fallback() })
  await intento('ingreso · límite · campos vacíos', async () => {
    await page.getByRole('button', { name: 'Continuar' }).click()
    await page.locator('[aria-invalid="true"]').nth(1).waitFor()
    verificar('ingreso · límite · campos vacíos marcan 2 campos y no piden nada', (await page.locator('[aria-invalid="true"]').count()) === 2 && pedidos === 0)
  })
  await page.screenshot({ path: `${carpeta}/ingreso-limite-vacios-1280.png` })
  await page.getByLabel('Correo o teléfono').fill('71234567')
  await page.getByLabel('Contraseña', { exact: true }).fill('contrasena-e2e')
  for (const [estado, cuerpo, espera] of [[401, { codigo: 'AP-CU04-01' }, 'no coinciden'], [401, {}, 'no coinciden'], [500, { codigo: 'X' }, 'Probá de nuevo']]) {
    await intento(`ingreso · error ${estado}`, async () => {
      page.__errorEsperado = true
      await page.route('**/api/v1/sesiones', (r) => r.fulfill({ status: estado, contentType: 'application/json', body: JSON.stringify(cuerpo) }), { times: 1 })
      await page.getByRole('button', { name: 'Continuar' }).click()
      const alerta = page.locator('ap-alerta')
      await alerta.filter({ hasText: espera }).waitFor({ timeout: 10000 })
      const habilitado = await page.getByRole('button', { name: 'Continuar' }).isEnabled()
      const conserva = (await page.getByLabel('Correo o teléfono').inputValue()) === '71234567'
      verificar(`ingreso · error ${estado} ${cuerpo.codigo ?? 'sin código'} → «${espera}», conserva lo escrito, botón habilitado`, habilitado && conserva, (await alerta.innerText()).slice(0, 120))
      page.__errorEsperado = false
    })
  }
  await page.screenshot({ path: `${carpeta}/ingreso-error-500-1280.png` })
  await intento('ingreso · sin doble envío', async () => {
    pedidos = 0
    let liberar
    const retenida = new Promise((r) => (liberar = r))
    await page.route('**/api/v1/sesiones', async (r) => { pedidos++; await retenida; await r.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ codigo: 'AP-CU04-03' }) }) }, { times: 1 })
    await page.getByRole('button', { name: 'Continuar' }).click()
    await page.getByRole('button', { name: /Continuar|Procesando/ }).click({ force: true }).catch(() => {})
    await page.locator('form').evaluate((f) => f.requestSubmit())
    liberar()
    await page.getByLabel('Código de verificación').waitFor()
    verificar('ingreso · límite · tres envíos mientras espera = 1 pedido', pedidos === 1, `pedidos=${pedidos}`)
  })
  await intento('ingreso · código incompleto', async () => {
    await page.getByLabel('Código de verificación').fill('123')
    await page.getByRole('button', { name: 'Entrar' }).click()
    await page.locator('[aria-invalid="true"]').first().waitFor()
    verificar('ingreso · límite · código de 3 dígitos se marca en el campo', (await page.getByText('El código tiene 6 dígitos').count()) > 0)
  })
  await page.screenshot({ path: `${carpeta}/ingreso-codigo-incompleto-1280.png` })
  await intento('ingreso · aceptado', async () => {
    await page.route('**/api/v1/sesiones', (r) => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ sesionId: 's', expiraEn: new Date(Date.now() + 3.6e6).toISOString(), requiereFactorAdicional: false, dispositivoConfiable: true, tokenAcceso: token('PLATAFORMA') }) }), { times: 1 })
    await page.getByLabel('Código de verificación').fill('123456')
    await page.waitForURL(/\/tablero$/, { timeout: 15000 })
    verificar('ingreso · aceptado · código completo → /tablero', true)
  })
  await page.context().close()
}

// ── Shell a 1280 ─────────────────────────────────────────────────────────────────────────────────────────────────
{
  const page = await pagina()
  await intento('menú 1280', async () => {
    const entradas = await page.locator('ap-menu-lateral a').evaluateAll((as) => as.map((a) => ({ texto: a.textContent.trim(), href: a.getAttribute('href'), icono: !!a.querySelector('ap-icono-de-seccion svg') })))
    verificar('menú · 7 entradas con ícono, rol PLATAFORMA', entradas.length === 7 && entradas.every((e) => e.icono), entradas.map((e) => `${e.texto}→${e.href}`).join(' | '))
    await page.locator('ap-menu-lateral a', { hasText: 'Sistemas' }).click()
    await page.waitForURL(/\/sistemas\/servicios/)
    verificar('menú · Sistemas abre /sistemas/servicios (F1)', true)
    const pestanas = await page.locator('ap-pestanas-de-seccion nav').first().locator('a').evaluateAll((as) => as.map((a) => ({ t: a.textContent.trim(), i: !!a.querySelector('ap-icono svg') })))
    verificar('pestañas · Sistemas ≤ 8 y todas con ícono', pestanas.length <= 8 && pestanas.every((p) => p.i), pestanas.map((p) => p.t).join(' | '))
    await page.locator('ap-pestanas-de-seccion a', { hasText: 'Integraciones' }).click()
    await page.waitForURL(/\/sistemas\/proveedores/)
    await page.locator('ap-pestanas-de-seccion nav.sub a', { hasText: 'Outbox' }).click()
    await page.waitForURL(/\/sistemas\/outbox/)
    verificar('pestañas · grupo Integraciones → subpestaña Outbox', (await page.locator('ap-pestanas-de-seccion a[aria-current="page"]').innerText()).includes('Outbox'))
  })
  await page.screenshot({ path: `${carpeta}/shell-sistemas-outbox-1280-claro.png` })
  {
    // Nada de la cabecera queda fuera de la pantalla (el menú de usuario es lo último a la derecha).
    const derecha = await page.locator('ap-menu-de-usuario').evaluate((e) => e.getBoundingClientRect().right)
    verificar('1280 · la cabecera entra entera (menú de usuario visible)', derecha <= 1280, `borde derecho=${Math.round(derecha)}px`)
  }

  await intento('buscador', async () => {
    const caja = page.getByRole('combobox', { name: 'Buscar en el backoffice' })
    await caja.fill('retir')
    await page.getByRole('option', { name: /Retiros en revisión/ }).waitFor()
    await page.screenshot({ path: `${carpeta}/buscador-resultados-1280.png` })
    await caja.press('Enter')
    await page.waitForURL(/\/operacion\/retiros/)
    verificar('buscador · aceptado · «retir» → Enter abre /operacion/retiros', true)
    await caja.fill('zzzzqq')
    await page.getByText('Nada coincide con «zzzzqq»').waitFor()
    verificar('buscador · límite · sin coincidencias lo dice', true)
    await caja.press('Escape')
    const cerro = await page.getByRole('listbox').waitFor({ state: 'detached', timeout: 3000 }).then(() => true, () => false)
    verificar('buscador · Escape cierra la lista', cerro)
  })

  await intento('campana', async () => {
    const campana = page.locator('ap-campana-de-notificaciones .disparador')
    await campana.click()
    await page.locator('.panel .titulo-aviso, .panel .sin-avisos').first().waitFor()
    verificar('campana · con datos del simulado lista avisos', (await page.locator('.panel .item-aviso').count()) > 0)
    await campana.click()
    await page.route('**/api/v1/notificaciones/avisos-internos**', (r) => r.fulfill({ status: 200, contentType: 'application/json', body: '{"totalNoLeidos":0,"elementos":[]}' }), { times: 1 })
    await campana.click()
    await page.locator('.panel .sin-avisos').waitFor()
    verificar('campana · vacía orienta', (await page.locator('.panel .sin-avisos').innerText()).includes('No tenés avisos'))
    await campana.click()
    page.__errorEsperado = true
    await page.route('**/api/v1/notificaciones/avisos-internos**', (r) => r.fulfill({ status: 500, contentType: 'application/json', body: '{}' }), { times: 1 })
    await campana.click()
    await page.locator('.panel [role="alert"]').waitFor()
    await page.screenshot({ path: `${carpeta}/campana-error-500-1280.png` })
    await page.getByRole('button', { name: 'Volver a intentar' }).click()
    await page.locator('.panel .item-aviso').first().waitFor()
    verificar('campana · error 500 → mensaje + «Volver a intentar» recupera', true)
    page.__errorEsperado = false
    await page.keyboard.press('Escape')
  })

  await intento('menú de usuario', async () => {
    const disparador = page.locator('ap-menu-de-usuario .disparador')
    await disparador.click()
    await page.getByRole('menuitem').first().waitFor()
    const opciones = await page.getByRole('menuitem').allInnerTexts()
    const conIcono = await page.locator('ap-menu-de-usuario [role="menuitem"]').evaluateAll((os) => os.every((o) => o.querySelector('svg')))
    const enfocada = await page.evaluate(() => document.activeElement?.textContent?.trim())
    verificar('menú de usuario · Centro de ayuda y Salir, con ícono; foco en la primera opción', opciones.join('|') === 'Centro de ayuda|Salir' && conIcono && enfocada === 'Centro de ayuda', `${opciones.join('|')} · icono=${conIcono} · foco=${enfocada}`)
    await page.screenshot({ path: `${carpeta}/menu-de-usuario-1280.png` })
    await page.keyboard.press('ArrowDown')
    const segundo = await page.evaluate(() => document.activeElement?.textContent?.trim())
    await page.keyboard.press('Escape')
    const foco = await page.evaluate(() => document.activeElement?.classList.contains('disparador'))
    const cerro = await page.getByRole('menuitem').first().waitFor({ state: 'detached', timeout: 3000 }).then(() => true, () => false)
    verificar('menú de usuario · flecha recorre; Escape cierra y devuelve el foco', segundo === 'Salir' && foco && cerro, `segundo=${segundo} · foco=${foco} · cerró=${cerro}`)
    await disparador.click()
    await page.getByRole('menuitem', { name: 'Centro de ayuda' }).click()
    await page.waitForURL(/\/ayuda$/)
    verificar('menú de usuario · Centro de ayuda abre /ayuda', true)
  })

  await intento('ayuda', async () => {
    await page.locator('main#contenido ap-boton button, main#contenido button').first().waitFor()
    await page.screenshot({ path: `${carpeta}/ayuda-1280.png`, fullPage: true })
    const botones = await page.locator('main#contenido button:visible').allInnerTexts()
    verificar('ayuda · hay tarjetas con botón', botones.length > 0, botones.slice(0, 8).join(' | '))
    // Cada botón «Comenzar» abre su tutorial: el anfitrión aparece y lleva a la ruta del tutorial.
    const n = await page.locator('main#contenido button', { hasText: /Comenzar|Repetir|Continuar/ }).count()
    let abren = 0
    for (let i = 0; i < n; i++) {
      await ir(page, '/ayuda')
      const b = page.locator('main#contenido button', { hasText: /Comenzar|Repetir|Continuar/ }).nth(i)
      await b.waitFor()
      const antes = page.url()
      await b.click()
      const ok = await page.locator('ap-anfitrion-de-tutorial [role="dialog"], ap-anfitrion-de-tutorial .globo, [data-tutorial-paso]').first().waitFor({ timeout: 5000 }).then(() => true, () => page.url() !== antes)
      if (ok) abren++
      await page.keyboard.press('Escape')
    }
    verificar('ayuda · cada «Comenzar» abre su tutorial', abren === n, `${abren}/${n}`)
  })

  await intento('lanzador', async () => {
    await ir(page, '/operacion/reclamos')
    const lanzador = page.locator('ap-lanzador-de-tutorial button')
    if ((await lanzador.count()) === 0) return verificar('lanzador · en /operacion/reclamos', false, 'no aparece')
    await lanzador.click()
    const ok = await page.locator('ap-anfitrion-de-tutorial').locator('*').first().waitFor({ timeout: 5000 }).then(() => true, () => false)
    verificar('lanzador · abre el tutorial de la vista', ok)
    await page.keyboard.press('Escape')
  })

  await intento('salir 200', async () => {
    await page.locator('aside button.salir').click()
    await page.waitForURL(/\/ingreso/)
    verificar('Salir · 200 → /ingreso', true)
  })
  await intento('salir 500', async () => {
    await ingresar(page, 'PLATAFORMA')
    page.__errorEsperado = true
    await page.route('**/api/v1/sesion', (r) => (r.request().method() === 'DELETE' ? r.fulfill({ status: 500, body: '{}' }) : r.fallback()), { times: 1 })
    await page.locator('ap-menu-de-usuario .disparador').click()
    await page.getByRole('menuitem', { name: 'Salir' }).click()
    await page.waitForURL(/\/ingreso/)
    verificar('Salir · 500 del servidor → igual /ingreso', true)
    page.__errorEsperado = false
  })
  await page.context().close()
}

// ── Rol oficial: Sistemas no aparece (F1) ───────────────────────────────────────────────────────────────────────
{
  const page = await pagina({ rol: 'oficial' })
  const entradas = await page.locator('ap-menu-lateral a').allInnerTexts()
  verificar('F1 · rol oficial con todos los permisos: Sistemas no aparece', !entradas.some((t) => t.includes('Sistemas')), entradas.join(' | '))
  await page.locator('ap-menu-lateral a', { hasText: 'Configuración' }).click()
  await page.waitForURL(/\/configuracion\//)
  const pestanas = await page.locator('ap-pestanas-de-seccion a').allInnerTexts()
  verificar('F1 · rol oficial: Configuración sin «Catálogos operativos» (exige el rol de sistemas)', !pestanas.some((t) => t.includes('Catálogos operativos')), pestanas.join(' | '))
  await page.context().close()
}

// ── Shell a 375: hamburguesa, cajón, navegación y Salir ─────────────────────────────────────────────────────────
{
  const page = await pagina({ ancho: 375, alto: 812 })
  await intento('375', async () => {
    await page.screenshot({ path: `${carpeta}/shell-tablero-375-claro.png` })
    await page.getByRole('button', { name: 'Abrir el menú' }).click()
    await page.locator('ap-cajon-lateral ap-menu-lateral a').first().waitFor()
    await quieto(page)
    await page.screenshot({ path: `${carpeta}/cajon-375-claro.png` })
    await page.locator('ap-cajon-lateral ap-menu-lateral a', { hasText: 'Cumplimiento' }).click()
    await page.waitForURL(/\/cumplimiento\//)
    const abierto = await page.locator('ap-cajon-lateral ap-menu-lateral').count()
    verificar('375 · hamburguesa abre el cajón; una entrada navega y lo cierra', abierto === 0)
    const desborde = await page.evaluate(() => document.documentElement.scrollWidth - innerWidth)
    verificar('375 · sin desplazamiento horizontal de la página', desborde <= 0, `desborde=${desborde}px`)
    await ir(page, '/sistemas/outbox')
    await page.locator('ap-pestanas-de-seccion nav.sub').waitFor()
    await page.screenshot({ path: `${carpeta}/shell-sistemas-outbox-375-claro.png` })
    await page.getByRole('button', { name: 'Abrir el menú' }).click()
    await page.locator('ap-cajon-lateral button.salir').click()
    await page.waitForURL(/\/ingreso/)
    verificar('375 · Salir desde el cajón → /ingreso', true)
  })
  await page.context().close()
}

// ── Capturas restantes: 768 claro y 1280 oscuro ─────────────────────────────────────────────────────────────────
for (const [ancho, oscuro, nombre] of [[768, false, '768-claro'], [1280, true, '1280-oscuro'], [375, true, '375-oscuro']]) {
  const page = await pagina({ ancho, oscuro, alto: ancho === 375 ? 812 : 900 })
  await ir(page, '/sistemas/outbox')
  await page.locator('ap-pestanas-de-seccion nav.sub').waitFor()
  await quieto(page)
  await page.screenshot({ path: `${carpeta}/shell-sistemas-outbox-${nombre}.png` })
  await ir(page, '/cumplimiento/seguridad/incidentes')
  await page.locator('ap-pestanas-de-seccion nav.sub a[aria-current="page"]', { hasText: 'Incidentes de seguridad' }).waitFor()
  await page.locator('main#contenido h1, main#contenido h2', { hasText: /ncidentes/ }).first().waitFor().catch(() => {})
  await quieto(page)
  await page.screenshot({ path: `${carpeta}/shell-cumplimiento-incidentes-${nombre}.png` })
  await page.context().close()
}

writeFileSync(`${carpeta}/recorrido-shell.json`, JSON.stringify({ resultados, errores }, null, 1))
console.log(JSON.stringify({ pass: resultados.filter((r) => r.veredicto === 'PASS').length, fail: resultados.filter((r) => r.veredicto === 'FAIL').length, errores: errores.length }))
await navegador.close()
