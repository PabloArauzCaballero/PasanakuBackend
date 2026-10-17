import { readFileSync } from 'node:fs'
import { ESPERADOS_COMERCIAL, ESPERADOS_FINANCIERO, ESPERADOS_SISTEMAS, type Esperado } from '../../src/app/rutas/tablero/dominio/catalogo-de-tableros'
import type { Cuenta } from './apoyo/api'
import { capturarEstado } from './apoyo/capturas'
import { MODO } from './apoyo/entorno'
import { expect, ingresarBackoffice, irA, test } from './apoyo/escenario'

/**
 * Tableros por rol (H4.S2.M3, H5.S2.M2, H6.S3.M4) contra el STACK REAL del ensayo D3 (`arranque_completo.sh`): ingreso
 * real con segundo factor, `auditoria` real con los indicadores que publicaron los calculadores de cada servicio sobre
 * la base sembrada (`sql/61_dev`). Cada tarjeta con valor en la respuesta real tiene que verse en la pantalla con ese
 * mismo número: pantalla = API. Que la API = el cuadre del mayor lo prueba H4.S1.M8 con SQL.
 *
 * Las cuentas salen de `sembrar_usuarios.sh` (archivo FUERA del repositorio, ruta en CREDENCIALES_TABLEROS).
 */
test.skip(MODO !== 'real', 'Solo contra el stack real (ACEPTACION_MODO=real)')

function cuenta(rol: string): Cuenta {
  const archivo = process.env['CREDENCIALES_TABLEROS']
  if (!archivo) throw new Error('Falta CREDENCIALES_TABLEROS (la salida de sembrar_usuarios.sh)')
  const linea = readFileSync(archivo, 'utf8').split('\n').find((l) => l.startsWith(`${rol} `))
  const telefono = /telefono=(\S+)/.exec(linea ?? '')?.[1]
  const clave = /credencial=(\S+)/.exec(linea ?? '')?.[1]
  if (!telefono || !clave) throw new Error(`Sin credencial sembrada para ${rol}`)
  // Perfil `local`: DesafioDeDesarrollo acepta 000000 como segundo factor (solo ahí).
  return { telefono, clave, rol, factor: '000000' }
}

/** Mismo formato que `decimal()` de dominio/indicadores.ts (coma decimal, punto de miles, sin ceros de cola). */
function decimal(valor: string): string {
  const [entero, fraccion] = valor.split('.')
  const conMiles = entero!.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  const recortada = (fraccion ?? '').replace(/0+$/, '')
  return recortada ? `${conMiles},${recortada}` : conMiles
}

type Indicador = { codigo: string; valor?: string; suprimidoPorPrivacidad: boolean }

const TABLEROS = [
  { tipo: 'financiero', rol: 'TESORERIA', titulo: 'Tablero financiero', esperados: ESPERADOS_FINANCIERO },
  { tipo: 'comercial', rol: 'COMERCIAL', titulo: 'Tablero comercial', esperados: ESPERADOS_COMERCIAL },
  { tipo: 'sistemas', rol: 'ADMIN_PLATAFORMA', titulo: 'Tablero de sistemas', esperados: ESPERADOS_SISTEMAS },
] as const

for (const t of TABLEROS) {
  test(`tablero ${t.tipo} contra el backend real: cada valor de la API se ve en la pantalla`, async ({ b }) => {
    await ingresarBackoffice(b.page, cuenta(t.rol))
    const respuesta = b.page.waitForResponse((r) => new URL(r.url()).pathname.endsWith('/api/v1/indicadores'))
    await irA(b.page, `/tablero/${t.tipo}`)
    const r = await respuesta
    expect(r.status(), 'GET /indicadores del stack real').toBe(200)
    const cuerpo = (await r.json()) as { periodo: string; indicadores: Indicador[] }
    await expect(b.page.getByRole('heading', { level: 1, name: t.titulo })).toBeVisible()

    const delTablero = new Set(t.esperados.map((e: Esperado) => e.codigo))
    const conValor = cuerpo.indicadores.filter((i) => delTablero.has(i.codigo) && !i.suprimidoPorPrivacidad && i.valor !== undefined)
    console.log(`[${t.tipo}] periodo=${cuerpo.periodo} indicadores=${cuerpo.indicadores.length} con valor=${conValor.length}`)
    expect(conValor.length, `el período ${cuerpo.periodo} trae valores de la base sembrada`).toBeGreaterThan(0)
    for (const i of conValor) {
      const nombre = t.esperados.find((e: Esperado) => e.codigo === i.codigo)!.nombre
      // La parte entera con miles alcanza para distinguir: `ap-monto` agrega moneda y decimales a su modo.
      const entero = decimal(i.valor!).split(',')[0]!
      const tarjeta = b.page.locator('article, li, section').filter({ hasText: nombre }).last()
      await expect(tarjeta, `${i.codigo} = ${i.valor}`).toContainText(entero)
    }

    await capturarEstado(b.page, 'tableros-reales', t.tipo)
    expect(b.obs.inesperadas(), 'sin 4xx/5xx inesperados').toEqual([])
    expect(b.obs.consolaInesperada(), 'consola sin errores').toEqual([])
  })
}

test('catálogo de datos contra el backend real: las 337 tablas y una ficha', async ({ b }) => {
  await ingresarBackoffice(b.page, cuenta('ADMIN_PLATAFORMA'))
  const respuesta = b.page.waitForResponse((r) => new URL(r.url()).pathname.endsWith('/api/v1/indicadores/catalogo-de-datos'))
  await irA(b.page, '/tablero/catalogo-de-datos')
  const r = await respuesta
  expect(r.status()).toBe(200)
  const total = ((await r.json()) as { tablas: unknown[] }).tablas.length
  await expect(b.page.getByText(`${total} tablas`).first()).toBeVisible()
  await b.page.getByRole('searchbox').fill('obligación aporte')
  await b.page.getByRole('link', { name: 'obligacion_aporte', exact: true }).click()
  await expect(b.page.getByRole('heading', { level: 1 })).toContainText('obligacion_aporte')
  await capturarEstado(b.page, 'tableros-reales', 'catalogo-ficha', { viewports: ['movil', 'escritorio'] })
  expect(b.obs.inesperadas()).toEqual([])
  expect(b.obs.consolaInesperada()).toEqual([])
})
