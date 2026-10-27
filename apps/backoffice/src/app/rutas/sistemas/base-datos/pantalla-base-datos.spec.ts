import { describe } from 'vitest'
import { probarVistaDePuerto } from '../compartido/pruebas.apoyo'
import { PUERTO_BASE_DE_DATOS, type Migracion } from '../dominio/puertos'
import { PantallaBaseDeDatos } from './pantalla-base-datos'

/** Doble del puerto (regla 65): filas sintéticas y deterministas, nunca la fuente real. */
describe('PantallaBaseDeDatos · listado del kit en los tres niveles', () => {
  probarVistaDePuerto<Migracion>({
    componente: PantallaBaseDeDatos,
    puerto: PUERTO_BASE_DE_DATOS,
    filas: (n) => Array.from({ length: n }, (_, i) => ({ id: `m-${i + 1}`, nombre: i === 7 ? 'V0008__indice_cupos' : `V${String(i + 1).padStart(4, '0')}__cambio`, aplicadaEl: i % 5 === 0 ? null : '2026-10-01', estado: i % 5 === 0 ? 'pendiente' : 'aplicada' })),
    textoVacio: 'No hay migraciones registradas.',
    busqueda: { termino: 'indice', esperadas: 1 },
    filtro: { etiqueta: 'Estado', valor: 'pendiente', esperadas: 5 },
  })
})
