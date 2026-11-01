import { describe } from 'vitest'
import { probarVistaDePuerto } from '../compartido/pruebas.apoyo'
import { PUERTO_RESPALDOS, type Respaldo } from '../dominio/puertos'
import { PantallaRespaldos } from './pantalla-respaldos'

/** Doble del puerto (regla 65): filas sintéticas y deterministas, nunca la fuente real. */
describe('PantallaRespaldos · listado del kit en los tres niveles', () => {
  probarVistaDePuerto<Respaldo>({
    componente: PantallaRespaldos,
    puerto: PUERTO_RESPALDOS,
    filas: (n) => Array.from({ length: n }, (_, i) => ({ id: `r-${i + 1}`, origen: i % 2 ? 'postgres-principal' : 'objetos-adjuntos', tomadoEl: '2026-10-25', ultimaRestauracionProbadaEl: i % 4 === 0 ? null : new Date().toISOString().slice(0, 10) })),
    textoVacio: 'No hay respaldos registrados.',
    busqueda: { termino: 'adjuntos', esperadas: 13 },
    filtro: { etiqueta: 'Restauración', valor: 'vencida', esperadas: 7 },
  })
})
