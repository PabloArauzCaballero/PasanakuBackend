import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router, UrlTree, type Route } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { Sesion } from '../../../nucleo/sesion'
import { rutasCumplimiento } from '../cumplimiento.routes'
import { requiereAlguno, tieneAlguno } from './acceso'
import { ENTRADAS_DE_CUMPLIMIENTO } from './navegacion'

/** Corre los `canMatch` de una ruta en contexto de inyección: `true` si monta. */
function monta(ruta: Route): boolean {
  return (ruta.canMatch ?? []).every((guarda) => TestBed.runInInjectionContext(() => (guarda as () => boolean | UrlTree)()) === true)
}
const ruta = (path: string) => rutasCumplimiento.find((r) => r.path === path) as Route

describe('acceso · espejo de GuardiaDePermiso (rol o permiso)', () => {
  let sesion: Sesion
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
    sesion = TestBed.inject(Sesion)
  })

  it('un código alcanza si es el ROL de la sesión o si está entre sus permisos', () => {
    sesion.abrir('t', ['AUDITORIA_LEER'], 'RESPONSABLE_RIESGOS')
    expect(tieneAlguno(sesion, ['RESPONSABLE_RIESGOS'])).toBe(true)
    expect(tieneAlguno(sesion, ['AUDITORIA_LEER'])).toBe(true)
    expect(tieneAlguno(sesion, ['OFICIAL_CUMPLIMIENTO', 'CUMPLIMIENTO_CASOS'])).toBe(false)
  })

  it('sin ninguno de los códigos, la ruta no monta y se vuelve al tablero', () => {
    sesion.abrir('t', ['CONTABILIDAD_ERP_CERRAR'], 'CONTABILIDAD')
    const resultado = TestBed.runInInjectionContext(() => requiereAlguno('AUDITORIA_LEER')({} as never, [], {} as never))
    expect(resultado).toEqual(TestBed.inject(Router).parseUrl('/tablero'))
  })

  it('cada bandeja nueva pide exactamente los códigos de su operación en cumplimiento.yaml', () => {
    const casos: [string, string[], string][] = [
      ['alertas', ['CUMPLIMIENTO_ALERTAS'], 'OPERADOR'],
      ['casos', ['CUMPLIMIENTO_CASOS'], 'OPERADOR'],
      ['uif/reportes', ['CUMPLIMIENTO_REPORTAR'], 'OPERADOR'],
      ['uif/reportes', ['AUDITORIA_LEER'], 'AUDITOR_INTERNO'],
      ['uif/requerimientos', [], 'OFICIAL_CUMPLIMIENTO'],
      ['continuidad', [], 'RESPONSABLE_RIESGOS'],
      ['actas', ['AUDITORIA_LEER'], 'AUDITOR_INTERNO'],
      ['riesgos/eventos', [], 'RESPONSABLE_RIESGOS'],
      ['seguridad/incidentes', [], 'RESPONSABLE_SEGURIDAD'],
    ]
    for (const [path, permisos, rol] of casos) {
      sesion.abrir('t', permisos, rol)
      expect(monta(ruta(path)), `${path} con ${rol}/${permisos}`).toBe(true)
      sesion.abrir('t', ['VERIFICACION_RESOLVER'], 'OPERADOR')
      expect(monta(ruta(path)), `${path} sin sus códigos`).toBe(false)
    }
  })

  it('la navegación de la sección ofrece solo lo que la sesión puede abrir', () => {
    sesion.abrir('t', ['AUDITORIA_LEER'], 'RESPONSABLE_SEGURIDAD')
    const visibles = ENTRADAS_DE_CUMPLIMIENTO.filter((e) => tieneAlguno(sesion, e.codigos)).map((e) => e.texto)
    expect(visibles).not.toContain('Alertas')
    expect(visibles).not.toContain('Casos')
    expect(visibles).toContain('Incidentes')
  })
})
