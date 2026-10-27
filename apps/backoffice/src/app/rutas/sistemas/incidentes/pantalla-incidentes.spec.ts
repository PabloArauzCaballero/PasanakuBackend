import { describe } from 'vitest'
import { probarVistaDePuerto } from '../compartido/pruebas.apoyo'
import { PUERTO_INCIDENTES, type Incidente } from '../dominio/puertos'
import { PantallaIncidentes } from './pantalla-incidentes'

/** Doble del puerto (regla 65): filas sintéticas y deterministas, nunca la fuente real. */
describe('PantallaIncidentes · listado del kit en los tres niveles', () => {
  probarVistaDePuerto<Incidente>({
    componente: PantallaIncidentes,
    puerto: PUERTO_INCIDENTES,
    filas: (n) => Array.from({ length: n }, (_, i) => ({ id: `i-${i + 1}`, titulo: i === 6 ? 'Caída de la pasarela QR' : `Incidente ${i + 1}`, severidad: (['sev1', 'sev2', 'sev3'] as const)[i % 3]!, estado: i % 2 ? 'cerrado' : 'abierto', abiertoEl: '2026-10-20' })),
    textoVacio: 'No hay incidentes registrados.',
    busqueda: { termino: 'pasarela', esperadas: 1 },
    filtro: { etiqueta: 'Severidad', valor: 'sev1', esperadas: 9 },
  })
})
