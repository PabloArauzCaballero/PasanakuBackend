import { MODO, BASE_API } from './entorno'
import { servirDoble } from './doble-con-estado'

/** Modo mock: levanta el doble con estado en el puerto de BASE_API y lo baja al terminar. En modo real no levanta nada. */
export default async function globalSetup(): Promise<() => Promise<void>> {
  if (MODO !== 'mock' || process.env['ACEPTACION_DOBLE_EXTERNO']) return async () => {}
  const { servidor } = await servirDoble(Number(new URL(BASE_API).port))
  return async () => {
    await new Promise<void>((ok) => servidor.close(() => ok()))
  }
}
