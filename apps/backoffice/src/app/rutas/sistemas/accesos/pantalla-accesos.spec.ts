import { describe } from 'vitest'
import { probarVistaDePuerto } from '../compartido/pruebas.apoyo'
import { PUERTO_ACCESOS, type Acceso } from '../dominio/puertos'
import { PantallaAccesos } from './pantalla-accesos'

/** Doble del puerto (regla 65): filas sintéticas y deterministas, nunca la fuente real. */
describe('PantallaAccesos · listado del kit en los tres niveles', () => {
  probarVistaDePuerto<Acceso>({
    componente: PantallaAccesos,
    puerto: PUERTO_ACCESOS,
    filas: (n) => Array.from({ length: n }, (_, i) => ({ id: `a-${i + 1}`, persona: i === 9 ? 'José Quispe' : `Persona ${i + 1}`, rol: i % 3 ? 'PLATAFORMA' : 'SEGURIDAD', ambito: 'sistemas', otorgadoEl: '—', estado: 'Activo', segundoFactor: i % 4 ? 'Sí' : 'No' })),
    textoVacio: 'No hay accesos registrados.',
    busqueda: { termino: 'jose', esperadas: 1 },
    filtro: { etiqueta: 'Rol', valor: 'SEGURIDAD', esperadas: 9 },
  })
})
