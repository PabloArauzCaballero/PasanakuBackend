import { Component, provideZonelessChangeDetection, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { AccionesDeFila } from '../acciones-de-fila/acciones-de-fila'
import type { Columna } from '../tabla-de-datos/tabla-de-datos'
import { crearListado } from './crear-listado'
import { ListadoDeDatos, type CargaDeListado } from './listado'

type Fila = { id: string; nombre: string; estado: string }

@Component({
  imports: [ListadoDeDatos, AccionesDeFila],
  template: `
    <ap-listado titulo="Personas" [columnas]="columnas" [listado]="listado" [carga]="carga()" (reintentar)="reintentos = reintentos + 1">
      <ng-template #acciones let-f><ap-acciones-de-fila [fila]="f.nombre" [editar]="true" [eliminar]="true" (editarPulsado)="editadas.push(f.id)" (eliminarConfirmado)="eliminadas.push(f.id)" /></ng-template>
    </ap-listado>
  `,
})
class Anfitrion {
  readonly datos = signal<readonly Fila[]>([])
  readonly carga = signal<CargaDeListado>({ tipo: 'lista' })
  readonly columnas: Columna<Fila>[] = [{ clave: 'id', titulo: 'Código' }, { clave: 'nombre', titulo: 'Nombre', ordenable: true }, { clave: 'estado', titulo: 'Estado' }]
  readonly listado = crearListado<Fila>({ filas: this.datos, camposDeBusqueda: ['id', 'nombre'], filtros: [{ nombre: 'estado', etiqueta: 'Estado', valor: (f) => f.estado }], porPagina: 10 })
  reintentos = 0
  editadas: string[] = []
  eliminadas: string[] = []
}

describe('ap-listado', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  function montar(n: number, carga: CargaDeListado = { tipo: 'lista' }) {
    const f = TestBed.createComponent(Anfitrion)
    f.componentInstance.datos.set(Array.from({ length: n }, (_, i) => ({ id: `P-${i + 1}`, nombre: `Persona ${i + 1}`, estado: i % 2 ? 'ACTIVO' : 'BAJA' })))
    f.componentInstance.carga.set(carga)
    f.detectChanges()
    return { f, el: f.nativeElement as HTMLElement, a: f.componentInstance }
  }
  const texto = (el: Element | null) => el?.textContent?.replace(/\s+/g, ' ').trim()

  it('aceptado: barra de filtros, exportar, tabla de la página visible y paginación completa', () => {
    const { el } = montar(25)
    expect(el.querySelector('ap-barra-de-filtros input[type=search]')).not.toBeNull()
    expect(el.querySelectorAll('ap-barra-de-filtros select').length).toBe(1)
    expect(texto(el.querySelector('ap-exportar-listado'))).toContain('Descargar CSV')
    expect(texto(el.querySelector('ap-exportar-listado'))).toContain('Descargar JSON')
    expect(el.querySelectorAll('tbody tr').length).toBe(10)
    expect(texto(el.querySelector('ap-paginacion .resumen'))).toBe('Página 1 de 3 · 25 registros')
    expect(el.querySelector('ap-paginacion select')).not.toBeNull()
    expect(el.querySelector('ap-paginacion input[type=number]')).not.toBeNull()
  })

  it('aceptado: Siguiente avanza y la acción Editar de la fila dice qué fila es', () => {
    const { f, el, a } = montar(25)
    ;(Array.from(el.querySelectorAll('ap-paginacion .pasos button')).at(1) as HTMLButtonElement).click()
    f.detectChanges()
    expect(texto(el.querySelector('tbody tr td'))).toContain('P-11')
    const editar = el.querySelector('tbody ap-acciones-de-fila button[aria-label^="Editar"]') as HTMLButtonElement
    expect(editar.getAttribute('aria-label')).toBe('Editar Persona 11')
    expect(texto(editar)).toBe('Editar')
    expect(editar.querySelector('svg')).not.toBeNull()
    editar.click()
    expect(a.editadas).toEqual(['P-11'])
  })

  it('aceptado: Eliminar no emite hasta confirmar en el diálogo', () => {
    const { f, el, a } = montar(3)
    ;(el.querySelector('tbody ap-acciones-de-fila button[aria-label^="Eliminar"]') as HTMLButtonElement).click()
    f.detectChanges()
    expect(a.eliminadas).toEqual([])
    const acciones = f.debugElement.query((d) => d.componentInstance instanceof AccionesDeFila).componentInstance as AccionesDeFila
    expect(acciones.preguntando()).toBe(true)
    acciones.confirmado()
    expect(a.eliminadas).toEqual(['P-1'])
  })

  it('aceptado: la columna de acciones va al final y no sale en la exportación', () => {
    const { f, el } = montar(2)
    expect(Array.from(el.querySelectorAll('thead th')).map((t) => texto(t))).toEqual(['Código', 'Nombre', 'Estado', 'Acciones'])
    const listado = f.debugElement.query((d) => d.componentInstance instanceof ListadoDeDatos).componentInstance as ListadoDeDatos<Fila>
    expect(listado.columnasExportables().map((c) => c.clave)).toEqual(['id', 'nombre', 'estado'])
  })

  it('límite: sin datos muestra el vacío sin tabla ni paginación', () => {
    const { el } = montar(0)
    expect(el.querySelector('ap-estado-vacio')).not.toBeNull()
    expect(el.querySelector('table')).toBeNull()
    expect(el.querySelector('ap-paginacion')).toBeNull()
  })

  it('límite: un filtro sin coincidencias ofrece «Quitar filtros» y lo cumple', () => {
    const { f, el, a } = montar(5)
    a.listado.buscar('no existe')
    f.detectChanges()
    const vacio = el.querySelector('ap-estado-vacio')!
    expect(texto(vacio)).toContain('Ningún registro coincide')
    ;(vacio.querySelector('button') as HTMLButtonElement).click()
    f.detectChanges()
    expect(el.querySelectorAll('tbody tr').length).toBe(5)
  })

  it('límite: cargando muestra el esqueleto y deshabilita la descarga', () => {
    const { el } = montar(5, { tipo: 'cargando' })
    expect(el.querySelector('ap-esqueleto')).not.toBeNull()
    expect((el.querySelector('ap-exportar-listado button') as HTMLButtonElement).disabled).toBe(true)
  })

  it('error: muestra el mensaje con su identificador y el reintento avisa', () => {
    const { f, el, a } = montar(5, { tipo: 'error', mensaje: 'El servicio no respondió.', idPeticion: 'req-123' })
    expect(texto(el.querySelector('ap-estado-de-error'))).toContain('El servicio no respondió.')
    expect(texto(el.querySelector('ap-estado-de-error'))).toContain('req-123')
    ;(el.querySelector('ap-estado-de-error button') as HTMLButtonElement).click()
    f.detectChanges()
    expect(a.reintentos).toBe(1)
    expect(el.querySelector('table')).toBeNull()
  })
})
