import { existsSync, readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import type { Escenario } from './escenarios'

export type EjemploDeEscenario = {
  estado: number
  cuerpo: unknown
  /** Solo en `intermitente`: el escenario con el que responde la segunda llamada con la misma clave. */
  luego?: Escenario
}

export type ArchivoDeEjemplos = {
  servicio: string
  operacion: string
  metodo: string
  ruta: string
  escenarios: Partial<Record<Escenario, EjemploDeEscenario>>
}

/**
 * La raíz del repositorio se busca hacia arriba desde el directorio de trabajo, no
 * desde `import.meta.url`: los corredores de Angular empaquetan este módulo y la URL
 * deja de apuntar a `packages/simulado`. Es el mismo criterio que usa `test/comun.dart`.
 */
function raizDelRepositorio(): string {
  let dir = process.cwd()
  while (!existsSync(resolve(dir, 'settings.gradle.kts'))) {
    const padre = dirname(dir)
    if (padre === dir) throw new Error('no encuentro la raíz del repositorio (settings.gradle.kts)')
    dir = padre
  }
  return dir
}
const RAIZ = resolve(raizDelRepositorio(), 'packages/simulado')

/**
 * El ejemplo de una operación y un escenario, leído del mismo archivo que Prism sirve.
 * Lo consumen las pruebas de Angular (por import) y las de Flutter (por ruta, el mismo
 * JSON): un CU, un archivo, dos lenguajes.
 */
export function ejemploDe(servicio: string, operacion: string, escenario: Escenario): EjemploDeEscenario {
  const archivo = resolve(RAIZ, 'ejemplos', servicio, `${operacion}.json`)
  const contenido = JSON.parse(readFileSync(archivo, 'utf8')) as ArchivoDeEjemplos
  const ejemplo = contenido.escenarios[escenario]
  if (!ejemplo) throw new Error(`${servicio}/${operacion} no tiene el escenario ${escenario}`)
  return ejemplo
}
