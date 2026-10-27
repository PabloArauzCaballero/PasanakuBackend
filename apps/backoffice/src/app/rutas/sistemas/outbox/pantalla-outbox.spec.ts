import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { estabilizar, filasVisibles, probarVistaDePuerto, texto } from '../compartido/pruebas.apoyo'
import { PUERTO_OUTBOX, type MensajeOutbox, type OutboxYDescartados } from '../dominio/puertos'
import { PantallaOutbox } from './pantalla-outbox'

const DESCARTADO = { id: 'x-1', tipo: 'AporteAcreditado', motivo: 'Firma del consumidor inválida tras 5 reintentos', descartadoEl: '2026-10-24' }

/** Doble del puerto (regla 65): filas sintéticas y deterministas. */
describe('PantallaOutbox · bandeja de salida en los tres niveles', () => {
  probarVistaDePuerto<MensajeOutbox>({
    componente: PantallaOutbox,
    puerto: PUERTO_OUTBOX,
    respuesta: (mensajes): OutboxYDescartados => ({ mensajes, descartados: [] }),
    filas: (n) => Array.from({ length: n }, (_, i) => ({ id: `o-${i + 1}`, tipo: i === 2 ? 'EntregaEjecutada' : 'AporteAcreditado', creadoEl: '2026-10-25', estado: i % 4 === 0 ? 'pendiente' : 'entregado' })),
    textoVacio: 'No hay mensajes en la bandeja de salida.',
    busqueda: { termino: 'entrega ejecutada', esperadas: 1 },
    filtro: { etiqueta: 'Estado', valor: 'pendiente', esperadas: 7 },
  })
})

describe('PantallaOutbox · GATE de descartados', () => {
  it('el motivo de cada descartado se ve en la tabla y ninguna fila ofrece editar ni eliminar (append-only)', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), { provide: PUERTO_OUTBOX, useValue: { obtener: () => Promise.resolve({ mensajes: [], descartados: [DESCARTADO] }) } }] })
    const el = await estabilizar(TestBed.createComponent(PantallaOutbox))
    expect(filasVisibles(el, 1)).toBe(1)
    expect(texto(el.querySelectorAll('ap-listado')[1])).toContain('Firma del consumidor inválida')
    expect(Array.from(el.querySelectorAll('button')).some((b) => /Eliminar|Editar/.test(texto(b)))).toBe(false)
    expect(texto(el)).toContain('No hay mensajes en la bandeja de salida.')
  })
})
