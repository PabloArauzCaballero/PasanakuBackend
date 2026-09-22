import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { Sesion } from '../sesion'
import { CARGADORES_DE_TUTORIALES, RegistroDeTutoriales, RUTAS_DEL_PRODUCTO } from '@aportaya/tutoriales/registro'
import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'
import { identidadDelOperador } from '../../rutas/ayuda/catalogo/proveer'

const paso = (id: string, permiso?: string) => ({ id, titulo: id, descripcion: 'd', objetivo: 'x', permiso })

const CATALOGO: TutorialDefinicion[] = [
  { id: 'abierto', version: '1', titulo: 'Para todos', descripcion: 'd', categoria: 'General', ruta: '/tablero', dificultad: 'inicial', pasos: [paso('a')] },
  {
    id: 'solo-operacion',
    version: '1',
    titulo: 'Billetera',
    descripcion: 'd',
    categoria: 'Operación',
    ruta: '/operacion',
    permisos: ['ver:operacion'],
    dificultad: 'inicial',
    pasos: [paso('a'), paso('avanzado', 'ver:operacion')],
  },
  {
    id: 'solo-sistemas',
    version: '1',
    titulo: 'Servicios',
    descripcion: 'd',
    categoria: 'Sistemas',
    ruta: '/sistemas',
    permisos: ['ver:sistemas'],
    dificultad: 'avanzado',
    pasos: [paso('a')],
  },
  { id: 'billetera-detalle', version: '1', titulo: 'Detalle', descripcion: 'd', categoria: 'Operación', ruta: '/operacion/billetera', permisos: ['ver:operacion'], dificultad: 'inicial', pasos: [paso('a')] },
]

async function montar(permisos: readonly string[]): Promise<RegistroDeTutoriales> {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      identidadDelOperador(),
      { provide: CARGADORES_DE_TUTORIALES, useValue: () => Promise.resolve(CATALOGO), multi: true },
      { provide: RUTAS_DEL_PRODUCTO, useValue: ['/tablero', '/operacion', '/sistemas'] },
    ],
  })
  TestBed.inject(Sesion).abrir('token', permisos, 'rol')
  const registro = TestBed.inject(RegistroDeTutoriales)
  await registro.cargar()
  return registro
}

describe('RegistroDeTutoriales', () => {
  it('sin permisos de módulo solo se ven los tutoriales abiertos', async () => {
    expect((await montar([])).disponibles().map((t) => t.id)).toEqual(['abierto'])
  })

  it('con el permiso del módulo aparece su tutorial, y no el de los otros', async () => {
    const ids = (await montar(['ver:operacion'])).disponibles().map((t) => t.id)
    expect(ids).toContain('solo-operacion')
    expect(ids).not.toContain('solo-sistemas')
  })

  it('los pasos también se filtran por permiso', async () => {
    const conPermiso = (await montar(['ver:operacion'])).buscar('solo-operacion')
    expect(conPermiso?.pasos.map((p) => p.id)).toEqual(['a', 'avanzado'])
  })

  it('el tutorial de una ruta es el de la coincidencia más específica', async () => {
    const registro = await montar(['ver:operacion'])
    expect(registro.paraLaRuta('/operacion/billetera/abc')?.id).toBe('billetera-detalle')
    expect(registro.paraLaRuta('/operacion/reclamos')?.id).toBe('solo-operacion')
  })

  it('una ruta sin tutorial no inventa uno', async () => {
    expect((await montar([])).paraLaRuta('/contabilidad')).toBeUndefined()
  })

  it('los parámetros de consulta no confunden la búsqueda por ruta', async () => {
    expect((await montar([])).paraLaRuta('/tablero?q=algo')?.id).toBe('abierto')
  })

  it('las categorías salen de lo disponible, no del catálogo entero', async () => {
    expect((await montar([])).categorias()).toEqual(['General'])
  })

  it('el catálogo de prueba está sano: el registro no reporta problemas', async () => {
    expect((await montar(['ver:operacion', 'ver:sistemas'])).problemas()).toEqual([])
  })
})
