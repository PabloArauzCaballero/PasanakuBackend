import { expect, test } from '@playwright/test'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'

/**
 * F5 · estado de las dos pantallas que el encargo pedía «SOLO si existen endpoints y pantallas reales en apps/web y apps/backoffice».
 *
 *  - RETIRO CON DOBLE APROBACIÓN: ya tiene pantalla en `apps/backoffice` (`operacion/retiros`, permiso RETIRO_APROBAR) y su escenario de dos
 *    personas vive en `f5-retiro.aceptacion.e2e.ts`. Acá se fija que NO aparezca en `apps/web` (el sitio no tiene sesión de participantes ni de
 *    operadores) y que la ruta de backoffice exista con su permiso: si alguien la quita, esto falla.
 *  - MERCADO DE TURNOS (permuta / cesión de cupo): SIGUE PENDIENTE y documentado, no omitido. Los endpoints existen
 *    (`POST /turnos/permutas`, `POST /grupos/{g}/cupos/{c}/traspasos`, `/entregas/mercado/ofertas*`), pero todos son de la persona participante:
 *    `listarOfertas` solo devuelve ofertas «de grupos donde quien mira es miembro activo» y `publicarOferta`/`comprarOferta` exigen ser el titular o un
 *    miembro. Un operador de backoffice no es ninguna de las dos cosas, y el sitio web no tiene sesión de participante. La pantalla de participante
 *    existe solo en `apps/movil` (`pantalla_permuta.dart`), que no se puede ejecutar en esta máquina (Flutter no está en el guardián de RAM).
 *    Falta, para una pantalla de backoffice, un endpoint de SUPERVISIÓN (listar y cancelar ofertas como operador) que el contrato no tiene.
 *
 * Este archivo NO es un «skip»: ejecuta comprobaciones reales y FALLA el día que aparezca una pantalla o una llamada de mercado en las apps web o
 * backoffice, para obligar a escribir el escenario de aceptación/rechazo que hoy no se puede escribir.
 */
const RAIZ = path.resolve(__dirname, '../../../..')
const APPS = ['apps/web/src', 'apps/backoffice/src']
const PATRON_RUTA_MERCADO = /\b(permuta|traspaso|cesi[oó]n|oferta-de-turno|mercado)/i
const PATRON_ENDPOINT_MERCADO = /(turnos\/permutas|\/traspasos|entregas\/mercado)/

function archivos(dir: string): string[] {
  return readdirSync(dir).flatMap((n) => {
    const p = path.join(dir, n)
    if (statSync(p).isDirectory()) return n === 'node_modules' ? [] : archivos(p)
    return /\.ts$/.test(n) && !/\.spec\.ts$/.test(n) ? [p] : []
  })
}
const lineas = (f: string) => readFileSync(f, 'utf8').split('\n').map((l, i) => ({ f, i: i + 1, l }))

test.describe('F5 · mercado de turnos pendiente documentado; retiro con pantalla solo en backoffice', () => {
  for (const app of APPS) {
    test(`${app}: no hay rutas de pantalla ni llamadas a los endpoints de permuta, cesión o mercado de turnos`, () => {
      const todos = archivos(path.join(RAIZ, app))
      const rutas = todos
        .filter((f) => /(\.routes\.ts|app\.routes\.ts)$/.test(f))
        .flatMap(lineas)
        .filter(({ l }) => /path:\s*['"]/.test(l) && PATRON_RUTA_MERCADO.test(l))
      const llamadas = todos.flatMap(lineas).filter(({ l }) => PATRON_ENDPOINT_MERCADO.test(l) && /(http|gateway|\$\{)/i.test(l))
      const hallazgos = [...rutas, ...llamadas].map(({ f, i, l }) => `${path.relative(RAIZ, f)}:${i}: ${l.trim().slice(0, 120)}`)
      expect(hallazgos, 'Apareció una pantalla o una llamada de mercado de turnos: escribí el escenario de aceptación/rechazo de dos usuarios y quitá este pendiente').toEqual([])
    })
  }

  test('el retiro con doble aprobación tiene su ruta en backoffice, con su permiso, y ninguna en el sitio web', () => {
    const rutasBo = readFileSync(path.join(RAIZ, 'apps/backoffice/src/app/rutas/operacion/operacion.routes.ts'), 'utf8')
    expect(rutasBo).toMatch(/path:\s*'retiros'[\s\S]{0,400}requierePermiso\('RETIRO_APROBAR'\)/)
    const llamadasWeb = archivos(path.join(RAIZ, 'apps/web/src'))
      .flatMap(lineas)
      .filter(({ l }) => /billetera\/retiros/.test(l) && /(http|gateway|\$\{)/i.test(l))
    expect(llamadasWeb, 'el sitio web no tiene sesión: no debe llamar al retiro').toEqual([])
  })

  test('queda registrado qué falta para cubrir el mercado de turnos', () => {
    test.info().annotations.push(
      { type: 'PENDIENTE', description: 'Mercado de turnos: aceptar/rechazar una permuta o cesión es de la persona participante; apps/web no tiene sesión de participante y apps/movil no se ejecuta acá.' },
      { type: 'PENDIENTE', description: 'Para una pantalla de backoffice del mercado hace falta un endpoint de supervisión (listar y cancelar ofertas como operador) que el contrato de entregas no publica.' },
      { type: 'DECISION', description: 'Quién prioriza ese endpoint y esa pantalla es una decisión de producto (dueño de backoffice / app del participante).' },
    )
  })
})
