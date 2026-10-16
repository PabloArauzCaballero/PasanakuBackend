import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { EntradaRevisionDecisionEnum } from 'clientes/angular/publicidad/model/entradaRevision'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { MODERADOR_ES_EL_ANUNCIANTE, moderarPieza, puedeUsarseEnUnAnuncio, type PiezaCreativa } from './cu112-moderacion'

const PENDIENTE: Pick<PiezaCreativa, 'estadoModeracion'> = { estadoModeracion: 'PENDIENTE' }
const APROBADA: Pick<PiezaCreativa, 'estadoModeracion'> = { estadoModeracion: 'APROBADA' }
const RECHAZADA: Pick<PiezaCreativa, 'estadoModeracion'> = { estadoModeracion: 'RECHAZADA' }

const RUTAS = join(__dirname, '..')

function archivosDe(carpeta: string): string[] {
  return readdirSync(carpeta).flatMap((e) => {
    const ruta = join(carpeta, e)
    return statSync(ruta).isDirectory() ? archivosDe(ruta) : [ruta]
  })
}

describe('CU-112 · gate: la moderación es previa a la entrega', () => {
  it('R-PUB-04: solo una pieza APROBADA puede usarse en un anuncio', () => {
    expect(puedeUsarseEnUnAnuncio(PENDIENTE)).toBe(false)
    expect(puedeUsarseEnUnAnuncio(RECHAZADA)).toBe(false)
    expect(puedeUsarseEnUnAnuncio(APROBADA)).toBe(true)
  })

  it('R-PUB-05: quien subió la pieza no puede moderarla — el contrato no dice quién la subió, lo rechaza el servidor (AP-CU112-03) y el error llega con su código', async () => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting()],
    })
    const http = TestBed.inject(HttpTestingController)
    const intento = moderarPieza(TestBed.inject(HttpClient), 'http://gw/api/v1', 'p-1', { decision: EntradaRevisionDecisionEnum.Aprobada }).catch((e: ErrorTraducido) => e)
    const req = http.expectOne('http://gw/api/v1/publicidad/piezas-creativas/p-1/revision')
    req.flush({ codigo: MODERADOR_ES_EL_ANUNCIANTE, mensaje: 'x', trazaId: 'tz' }, { status: 422, statusText: 'x' })
    expect(await intento).toMatchObject({ estado: 422, codigo: 'AP-CU112-03' })
  })

  it('no existe, en todo el carril, ninguna ruta ni componente que programe un anuncio saltando la moderación', () => {
    const archivos = archivosDe(RUTAS)
    const contenido = archivos.map((a) => readFileSync(a, 'utf8')).join('\n')
    // El único lugar que puede mover una pieza fuera de PENDIENTE es `moderarPieza` (este dominio).
    const llamadasAPost = contenido.match(/\/publicidad\/(anuncios|piezas-creativas\/[^'"`]*\/revision)/g) ?? []
    for (const llamada of llamadasAPost) {
      expect(llamada.includes('/revision')).toBe(true)
    }
    // No hay ninguna ruta cargada en publicidad.routes.ts hacia una pantalla de "programar anuncio":
    // el alcance del carril (ficha F14) es anunciantes, campañas, moderación, desempeño y liquidación.
    const rutas = readFileSync(join(RUTAS, 'publicidad.routes.ts'), 'utf8')
    expect(rutas).not.toMatch(/anuncios/)
  })
})
