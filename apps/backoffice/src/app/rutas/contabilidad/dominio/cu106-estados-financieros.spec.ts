import { HttpHeaders, HttpResponse } from '@angular/common/http'
import { describe, expect, it } from 'vitest'
import { ejemploDe } from '@aportaya/simulado'
import {
  armarDescarga,
  CABECERA_DE_HASH,
  DocumentoDiscrepante,
  esProvisorio,
  nombreDeArchivo,
  type EstadoFinancieroGenerado,
} from './cu106-estados-financieros'

/**
 * **El gate propio del carril B3, punto 2.** El estado financiero se descarga CON SU
 * HASH, y el hash es el que generó el backend: sale del ejemplo real del contrato
 * (`packages/simulado/ejemplos/erp/generarEstadoFinanciero.json`, el mismo archivo que
 * sirve Prism en `yarn dev:mock`), no de un literal escrito en la prueba, y el cliente no
 * lo recalcula en ningún punto.
 */
const GENERADO = ejemploDe('erp', 'generarEstadoFinanciero', 'ok').cuerpo as EstadoFinancieroGenerado

function respuesta(hashDeLaCabecera: string | null): HttpResponse<Blob> {
  return new HttpResponse<Blob>({
    body: new Blob(['documento sellado por el backend']),
    headers: hashDeLaCabecera === null ? new HttpHeaders() : new HttpHeaders({ [CABECERA_DE_HASH]: hashDeLaCabecera }),
  })
}

describe('CU-106 · el hash del estado financiero viene del backend', () => {
  it('el ejemplo del contrato trae hashContenido: es el dato del que depende todo el gate', () => {
    expect(GENERADO.hashContenido).toBeTruthy()
  })

  it('la descarga lleva EXACTAMENTE el hash que devolvió el backend, sin recalcularlo', () => {
    const descarga = armarDescarga(GENERADO, respuesta(GENERADO.hashContenido), 'marzo 2026')
    expect(descarga.hashContenido).toBe(GENERADO.hashContenido)
    expect(descarga.verificado).toBe(true)
  })

  it('si el documento que llega no es el del estado generado, no se entrega el archivo', () => {
    expect(() => armarDescarga(GENERADO, respuesta('otro-hash-distinto'), 'marzo 2026')).toThrow(DocumentoDiscrepante)
  })

  it('sin la cabecera del backend no se finge la verificación: queda marcada como no verificada', () => {
    const descarga = armarDescarga(GENERADO, respuesta(null), 'marzo 2026')
    expect(descarga.verificado).toBe(false)
    expect(descarga.hashContenido).toBe(GENERADO.hashContenido)
  })

  it('el nombre del archivo lleva el hash del backend, para que el documento se reconozca fuera del sistema', () => {
    expect(nombreDeArchivo(GENERADO, 'marzo 2026')).toBe(`balance-general-marzo-2026-${GENERADO.hashContenido.slice(0, 8)}.pdf`)
  })
})

describe('CU-106 · provisorio contra definitivo (flujo alternativo 2a)', () => {
  it('un período todavía abierto solo da un estado provisorio', () => {
    expect(esProvisorio(false)).toBe(true)
  })

  it('un período cerrado da el definitivo', () => {
    expect(esProvisorio(true)).toBe(false)
  })
})
