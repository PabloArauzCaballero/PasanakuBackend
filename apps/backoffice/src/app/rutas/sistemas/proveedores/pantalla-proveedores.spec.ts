import { describe } from 'vitest'
import { probarVistaDePuerto } from '../compartido/pruebas.apoyo'
import { PUERTO_PROVEEDORES, type Proveedor } from '../dominio/puertos'
import { PantallaProveedores } from './pantalla-proveedores'

/** Doble del puerto (regla 65): filas sintéticas y deterministas, nunca la fuente real. */
describe('PantallaProveedores · listado del kit en los tres niveles', () => {
  probarVistaDePuerto<Proveedor>({
    componente: PantallaProveedores,
    puerto: PUERTO_PROVEEDORES,
    filas: (n) => Array.from({ length: n }, (_, i) => ({ id: `p-${i + 1}`, nombre: i === 2 ? 'Mensajería SMS Andina' : `Proveedor ${i + 1}`, categoria: i % 2 ? 'Nube' : 'Mensajería', costoRealUltimoPeriodo: '1250.50', moneda: i % 5 === 0 ? 'USD' : 'BOB' })),
    textoVacio: 'No hay proveedores registrados.',
    busqueda: { termino: 'andina', esperadas: 1 },
    filtro: { etiqueta: 'Moneda', valor: 'USD', esperadas: 5 },
  })
})
