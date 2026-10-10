import { expect, test } from '@playwright/test'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'

/**
 * F5 · MERCADO DE TURNOS (permuta / cesión de cupo) y RETIRO CON DOBLE APROBACIÓN: PENDIENTE DOCUMENTADO, no omitido.
 *
 * El encargo pedía cubrirlos «SOLO si existen pantallas y endpoints reales en apps/web y apps/backoffice». Endpoints SÍ existen:
 *   - `POST /turnos/permutas` (solicitarPermuta, CU-62) y `POST /grupos/{g}/cupos/{c}/traspasos` (traspasarCupo, CU-64)  → servicios/grupos
 *   - `POST /billetera/retiros` y `POST /billetera/retiros/{orden}/aprobacion` (aprobarRetiro, RETIRO_APROBAR, 409 al segundo aprobador) → servicios/nucleo-financiero
 * Pantallas NO existen ni en apps/web ni en apps/backoffice (solo en apps/movil: pantalla_permuta.dart, pantalla_retiro.dart, pantalla_retirar.dart,
 * que no se pueden ejecutar en esta máquina: Flutter no está instalado). Por eso no hay escenario de UI de dos usuarios para F5.
 *
 * Este archivo NO es un «skip»: ejecuta una comprobación real que FALLA el día que aparezca una pantalla o una llamada de esos endpoints en las
 * apps web/backoffice, para obligar a escribir el escenario de aceptación/rechazo que hoy no se puede escribir.
 */
const RAIZ = path.resolve(__dirname, '../../../..')
const APPS = ['apps/web/src', 'apps/backoffice/src']
const PATRON_RUTA = /\b(permuta|traspaso|cesi[oó]n|oferta-de-turno|mercado|retiro|retirar|aprobacion-de-retiro)/i
const PATRON_ENDPOINT = /(turnos\/permutas|\/traspasos|billetera\/retiros)/

function archivos(dir: string): string[] {
  return readdirSync(dir).flatMap((n) => {
    const p = path.join(dir, n)
    if (statSync(p).isDirectory()) return n === 'node_modules' ? [] : archivos(p)
    return /\.ts$/.test(n) && !/\.spec\.ts$/.test(n) ? [p] : []
  })
}

test.describe('F5 · pendiente documentado (sin pantallas de mercado de turnos ni de retiro en web/backoffice)', () => {
  for (const app of APPS) {
    test(`${app}: no hay rutas de pantalla ni llamadas a los endpoints de permuta, cesión o retiro`, () => {
      const todos = archivos(path.join(RAIZ, app))
      const rutas = todos
        .filter((f) => /(\.routes\.ts|app\.routes\.ts)$/.test(f))
        .flatMap((f) => readFileSync(f, 'utf8').split('\n').map((l, i) => ({ f, i: i + 1, l })))
        .filter(({ l }) => /path:\s*['"]/.test(l) && PATRON_RUTA.test(l))
      const llamadas = todos
        .flatMap((f) => readFileSync(f, 'utf8').split('\n').map((l, i) => ({ f, i: i + 1, l })))
        .filter(({ l }) => PATRON_ENDPOINT.test(l) && /(http|gateway|\$\{)/i.test(l))
      const hallazgos = [...rutas, ...llamadas].map(({ f, i, l }) => `${path.relative(RAIZ, f)}:${i}: ${l.trim().slice(0, 120)}`)
      expect(hallazgos, 'Apareció una pantalla o una llamada: escribí el escenario de aceptación/rechazo de dos usuarios para F5 y quitá este pendiente').toEqual([])
    })
  }

  test('queda registrado qué falta para poder cubrir F5', () => {
    test.info().annotations.push(
      { type: 'PENDIENTE', description: 'Mercado de turnos: aceptar/rechazar una permuta o cesión necesita pantallas en apps/web (participante con sesión) — hoy solo existen en apps/movil.' },
      { type: 'PENDIENTE', description: 'Retiro con doble aprobación (aprobador A aprueba, B rechaza): endpoint aprobarRetiro existe (RETIRO_APROBAR); el rol TESORERIA ya tiene RETIRO_APROBAR (sql/60_semillas/10-roles-y-permisos.sql:91), pero falta la pantalla de aprobación en apps/backoffice y el mapa de secciones (nucleo/secciones.ts) no la contempla.' },
      { type: 'DECISION', description: 'Quién prioriza esas pantallas es una decisión de producto (dueño de backoffice / app del participante).' },
    )
  })
})
