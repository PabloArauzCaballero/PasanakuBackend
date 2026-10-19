import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router, UrlTree, type Route, type UrlSegment } from '@angular/router'
import { describe, expect, it } from 'vitest'
import { Sesion } from '../../nucleo/sesion'
import { rutasConfiguracion } from './configuracion.routes'
import { PantallaDeConfiguracion } from './pantalla-de-configuracion'

function preparar(permisos: string[]) {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
  TestBed.inject(Sesion).abrir('t', permisos, 'BACKOFFICE', 'op-1')
}

function decide(path: string): boolean | string {
  const ruta = rutasConfiguracion.find((r) => r.path === path) as Route
  const guardia = ruta.canMatch![0] as (r: Route, s: UrlSegment[]) => boolean | UrlTree
  const resultado = TestBed.runInInjectionContext(() => guardia(ruta, []))
  return resultado instanceof UrlTree ? TestBed.inject(Router).serializeUrl(resultado) : resultado
}

describe('rutasConfiguracion · cada listado exige el permiso de su contrato', () => {
  it('correcto: tarifario abre con TARIFARIO_PUBLICAR o con AUDITORIA_LEER (es un O)', () => {
    preparar(['TARIFARIO_PUBLICAR'])
    expect(decide('tarifario')).toBe(true)
    TestBed.resetTestingModule()
    preparar(['AUDITORIA_LEER'])
    expect(decide('tarifario')).toBe(true)
    expect(decide('mensajeria')).toBe(true)
    expect(decide('reportes')).toBe(true)
  })

  it('límite: TARIFARIO_PUBLICAR solo no abre mensajería ni reportes', () => {
    preparar(['TARIFARIO_PUBLICAR'])
    expect(decide('mensajeria')).toBe('/tablero')
    expect(decide('reportes')).toBe('/tablero')
  })

  it('inválido: sin permiso, ninguna ruta monta: va al tablero', () => {
    preparar(['RECLAMO_VER'])
    for (const p of ['tarifario', 'mensajeria', 'reportes']) expect(decide(p)).toBe('/tablero')
  })

  it('toda ruta con pantalla de datos tiene canMatch', () => {
    expect(rutasConfiguracion.filter((r) => r.path !== '').every((r) => (r.canMatch ?? []).length > 0)).toBe(true)
  })
})

describe('PantallaDeConfiguracion · entrada filtrada por permiso', () => {
  const secciones = (permisos: string[]) => {
    preparar(permisos)
    const fixture = TestBed.createComponent(PantallaDeConfiguracion)
    fixture.detectChanges()
    const raiz = fixture.nativeElement as HTMLElement
    return { raiz, rutas: Array.from(raiz.querySelectorAll('[data-seccion]')).map((a) => a.getAttribute('data-seccion')) }
  }

  it('AUDITORIA_LEER ve las tres', () => expect(secciones(['AUDITORIA_LEER']).rutas).toEqual(['tarifario', 'mensajeria', 'reportes']))
  it('TARIFARIO_PUBLICAR ve solo el tarifario', () => expect(secciones(['TARIFARIO_PUBLICAR']).rutas).toEqual(['tarifario']))
  it('sin permiso, el vacío lo explica', () => expect(secciones([]).raiz.textContent).toContain('Tu rol no tiene acceso a ninguna parte de la configuración.'))
})
