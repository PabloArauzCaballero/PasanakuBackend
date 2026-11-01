import { describe } from 'vitest'
import { probarVistaDePuerto } from '../compartido/pruebas.apoyo'
import { PUERTO_WEBHOOKS, type Webhook } from '../dominio/puertos'
import { PantallaWebhooks } from './pantalla-webhooks'

/** Doble del puerto (regla 65): filas sintéticas y deterministas, nunca la fuente real. */
describe('PantallaWebhooks · listado del kit en los tres niveles', () => {
  probarVistaDePuerto<Webhook>({
    componente: PantallaWebhooks,
    puerto: PUERTO_WEBHOOKS,
    filas: (n) => Array.from({ length: n }, (_, i) => ({ id: `w-${i + 1}`, direccion: i % 2 ? 'saliente' : 'entrante', origen: i === 4 ? 'pasarela-bnb' : `origen-${i + 1}`, ultimoEstado: i % 6 === 0 ? 'error' : 'ok', ultimaEntregaEl: '2026-10-25 10:00' })),
    textoVacio: 'No hay webhooks configurados.',
    busqueda: { termino: 'bnb', esperadas: 1 },
    filtro: { etiqueta: 'Último estado', valor: 'error', esperadas: 5 },
  })
})
