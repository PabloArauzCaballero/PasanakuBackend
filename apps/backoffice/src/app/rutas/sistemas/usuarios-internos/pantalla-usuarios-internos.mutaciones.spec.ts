import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed, type ComponentFixture } from '@angular/core/testing'
import { Router, provideRouter } from '@angular/router'
import type { UsuarioInternoResumen } from 'clientes/angular/identidad'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { conteoDeExportacion, elegir, estabilizar, filasVisibles, listadoDe, pulsar, resumenDePaginas, texto } from '../compartido/pruebas.apoyo'
import { PantallaUsuariosInternos } from './pantalla-usuarios-internos'

const GW = 'http://gw/api/v1'
const LISTA = `${GW}/usuarios-internos`
/** Usuarios sintéticos y deterministas (regla 97.6): ningún dato real de persona. */
function usuarios(n: number): UsuarioInternoResumen[] {
  return Array.from({ length: n }, (_, i) => ({
    id: `u-${i + 1}`, usuarioId: `usr-${i + 1}`, codigoPublico: `USR-${String(i + 1).padStart(3, '0')}`,
    nombres: i === 4 ? 'José' : `Nombre${i + 1}`, apellidos: 'Prueba', correo: `op${i + 1}@aportaya.test`, telefono: '+59170000000',
    departamento: i % 2 ? 'RIESGOS' : 'TECNOLOGIA', cargo: 'Analista', estado: i % 5 === 0 ? 'SUSPENDIDO' : 'ACTIVO',
    debeCambiarContrasena: false, tieneSegundoFactor: i % 3 !== 0, ...(i === 1 ? { bloqueadoHasta: '2026-10-27T10:00:00Z' } : {}), roles: ['RISK_ANALYST'],
  }))
}
const dialogoAbierto = (el: HTMLElement) => el.querySelector('dialog[open]') as HTMLElement
function escribir(raiz: Element, etiqueta: string, valor: string): void {
  const label = Array.from(raiz.querySelectorAll('label')).find((l) => texto(l).startsWith(etiqueta))
  const control = (label?.htmlFor ? raiz.ownerDocument.getElementById(label.htmlFor) : label?.querySelector('input, textarea')) as HTMLInputElement | HTMLTextAreaElement | null
  if (!control) throw new Error(`No hay campo «${etiqueta}»`)
  control.value = valor
  control.dispatchEvent(new Event('input'))
}

describe('PantallaUsuariosInternos · listado y operaciones del contrato (identidad.yaml)', () => {
  let http: HttpTestingController
  let f: ComponentFixture<PantallaUsuariosInternos>
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }] })
    http = TestBed.inject(HttpTestingController)
    f = TestBed.createComponent(PantallaUsuariosInternos)
  })
  afterEach(() => http.verify())
  async function cargar(n: number) {
    f.detectChanges()
    http.expectOne(LISTA).flush(usuarios(n))
    return estabilizar(f)
  }

  it('aceptado: 25 usuarios a 10 por página; buscar «jose» y filtrar por estado; cifras arriba', async () => {
    const el = await cargar(25)
    expect(filasVisibles(el)).toBe(10)
    expect(resumenDePaginas(el)).toContain('Página 1 de 3')
    expect(texto(el.querySelector('ap-cifras-de-sistemas'))).toContain('Suspendidos 5')
    listadoDe(f).buscar('jose')
    await estabilizar(f)
    expect(conteoDeExportacion(el)).toBe('1 fila')
    listadoDe(f).limpiar()
    elegir(el, 'Estado', 'SUSPENDIDO')
    await estabilizar(f)
    expect(listadoDe(f).total()).toBe(5)
  })

  it('aceptado: cada fila trae Ver, Editar, Roles y Suspender/Reactivar con ícono + nombre, Desbloquear solo si está bloqueado, y ningún Eliminar', async () => {
    const el = await cargar(3)
    const fila2 = el.querySelectorAll('tbody tr')[1]!
    expect(Array.from(fila2.querySelectorAll('button')).map((b) => texto(b))).toEqual(['Roles', 'Suspender', 'Desbloquear', 'Ver', 'Editar'])
    expect(fila2.querySelectorAll('button ap-icono').length).toBe(5)
    expect(texto(el.querySelectorAll('tbody tr')[0]!)).toContain('Reactivar')
    expect(Array.from(el.querySelectorAll('button')).some((b) => texto(b) === 'Eliminar')).toBe(false)
  })

  it('aceptado: Ver navega a la ficha del usuario', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    const el = await cargar(2)
    pulsar(el.querySelectorAll('tbody tr')[1]!, 'Ver')
    expect(navegar).toHaveBeenCalledWith(['/sistemas/usuarios-internos', 'u-2'])
  })

  it('aceptado: Editar manda PATCH con departamento, cargo y motivo, cierra y recarga la lista', async () => {
    const el = await cargar(2)
    pulsar(el.querySelectorAll('tbody tr')[1]!, 'Editar')
    await estabilizar(f)
    const d = dialogoAbierto(el)
    expect(texto(d.querySelector('h2'))).toBe('Editar datos del usuario interno')
    escribir(d, 'Cargo', 'Jefa de riesgos')
    escribir(d, 'Motivo', 'Ascenso aprobado por RRHH')
    await estabilizar(f)
    pulsar(d, 'Guardar cambios')
    const req = http.expectOne(`${GW}/usuarios-internos/u-2`)
    expect(req.request.method).toBe('PATCH')
    expect(req.request.body).toEqual({ departamento: 'RIESGOS', cargo: 'Jefa de riesgos', motivo: 'Ascenso aprobado por RRHH' })
    req.flush({})
    f.detectChanges()
    http.expectOne(LISTA).flush(usuarios(2))
    await estabilizar(f)
    expect(dialogoAbierto(el)).toBeNull()
  })

  it('error: si el PATCH falla, el diálogo queda abierto con lo escrito, el error traducido y el botón libre', async () => {
    const el = await cargar(2)
    pulsar(el.querySelectorAll('tbody tr')[1]!, 'Suspender')
    await estabilizar(f)
    const d = dialogoAbierto(el)
    expect(texto(d.querySelector('h2'))).toBe('Suspender usuario interno')
    escribir(d, 'Motivo', 'Baja por fin de contrato')
    await estabilizar(f)
    pulsar(d, 'Suspender')
    const req = http.expectOne(`${GW}/usuarios-internos/u-2`)
    expect(req.request.body).toEqual({ estado: 'SUSPENDIDO', motivo: 'Baja por fin de contrato' })
    req.flush({ codigo: 'X' }, { status: 500, statusText: 'Error' })
    await estabilizar(f)
    expect(dialogoAbierto(el)).not.toBeNull()
    expect((d.querySelector('textarea') as HTMLTextAreaElement).value).toBe('Baja por fin de contrato')
    expect(texto(d.querySelector('ap-error-de-envio'))).toContain('No se pudo')
    const confirmar = Array.from(d.querySelectorAll('button')).find((b) => texto(b) === 'Suspender') as HTMLButtonElement
    expect(confirmar.disabled).toBe(false)
  })

  it('límite: confirmar está deshabilitado con un motivo de menos de 5 caracteres', async () => {
    const el = await cargar(2)
    pulsar(el.querySelectorAll('tbody tr')[1]!, 'Desbloquear')
    await estabilizar(f)
    const d = dialogoAbierto(el)
    escribir(d, 'Motivo', 'ok')
    await estabilizar(f)
    expect((Array.from(d.querySelectorAll('button')).find((b) => texto(b) === 'Desbloquear') as HTMLButtonElement).disabled).toBe(true)
    escribir(d, 'Motivo', 'Identidad verificada por teléfono')
    await estabilizar(f)
    pulsar(d, 'Desbloquear')
    const req = http.expectOne(`${GW}/usuarios-internos/u-2/desbloquear`)
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({ motivo: 'Identidad verificada por teléfono' })
    req.flush({})
    f.detectChanges()
    http.expectOne(LISTA).flush(usuarios(2))
    await estabilizar(f)
    expect(dialogoAbierto(el)).toBeNull()
  })

  it('aceptado: Roles abre el diálogo con los roles vigentes y manda PUT con la selección y el motivo', async () => {
    const el = await cargar(2)
    pulsar(el.querySelectorAll('tbody tr')[1]!, 'Roles')
    f.detectChanges()
    http.expectOne(`${GW}/roles`).flush([
      { id: 'r1', codigo: 'RISK_ANALYST', nombre: 'Analista de riesgo', ambito: 'INTERNO', departamento: 'RIESGOS', descripcion: null, esSistema: true, permisos: [] },
      { id: 'r2', codigo: 'AUDITOR', nombre: 'Auditor', ambito: 'INTERNO', departamento: 'AUDITORIA', descripcion: null, esSistema: true, permisos: [] },
    ])
    await estabilizar(f)
    const d = dialogoAbierto(el)
    const casillas = d.querySelectorAll('input[type=checkbox]') as NodeListOf<HTMLInputElement>
    expect(casillas[0]!.checked).toBe(true)
    casillas[1]!.click()
    escribir(d, 'Motivo formal', 'Rota a auditoría por seis meses')
    await estabilizar(f)
    pulsar(d, 'Confirmar roles')
    const req = http.expectOne(`${GW}/usuarios-internos/u-2/roles`)
    expect(req.request.method).toBe('PUT')
    expect(req.request.body).toEqual({ roles: ['RISK_ANALYST', 'AUDITOR'], motivo: 'Rota a auditoría por seis meses' })
    req.flush({})
    f.detectChanges()
    http.expectOne(LISTA).flush(usuarios(2))
    await estabilizar(f)
    expect(dialogoAbierto(el)).toBeNull()
  })

  it('error: un 403 al listar muestra «no tenés acceso» traducido y «Volver a intentar» vuelve a pedir', async () => {
    f.detectChanges()
    http.expectOne(LISTA).flush({ mensaje: 'stacktrace crudo' }, { status: 403, statusText: 'Forbidden' })
    const el = await estabilizar(f)
    expect(el.querySelector('ap-estado-de-error')).not.toBeNull()
    expect(texto(el)).not.toContain('stacktrace crudo')
    pulsar(el, 'Volver a intentar')
    f.detectChanges()
    http.expectOne(LISTA).flush([])
    await estabilizar(f)
    expect(texto(el)).toContain('No se encontraron usuarios internos registrados')
    expect(texto(el)).toContain('Dar de alta')
  })
})
