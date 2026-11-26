import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { UsuarioInternoDetalle } from 'clientes/angular/identidad'
import { Sesion } from '../../../nucleo/sesion'
import { EVIDENCIA_DE_PRUEBA, escribirCodigoDePrueba, verificarPasoDeSeguridad } from './paso-de-seguridad.pruebas'
import { GATEWAY } from '../../../nucleo/gateway'
import { estabilizar, pulsar } from '../compartido/pruebas.apoyo'
import { PantallaUsuarioInternoDetalle } from './pantalla-usuario-interno-detalle'

const USUARIO_DETALLE: UsuarioInternoDetalle = {
  id: 'u-1',
  usuarioId: 'usr-1',
  codigoPublico: 'USR-001',
  nombres: 'Carlos',
  apellidos: 'Mamani',
  correo: 'carlos.mamani@aportaya.bo',
  telefono: '+59171234567',
  departamento: 'RIESGOS',
  cargo: 'Analista de Riesgo',
  roles: ['RISK_ANALYST'],
  permisosEfectivos: ['ALERTAS_RIESGO_LEER', 'SCORE_CONSULTAR'],
  estado: 'ACTIVO',
  tieneSegundoFactor: true,
  debeCambiarContrasena: false,
}

describe('PantallaUsuarioInternoDetalle', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'u-1' } } },
        },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('renderiza la ficha con datos de organización, roles y permisos efectivos', async () => {
    const fixture = TestBed.createComponent(PantallaUsuarioInternoDetalle)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/usuarios-internos/u-1').flush(USUARIO_DETALLE)
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('Carlos Mamani')
    expect(texto).toContain('Analista de Riesgo')
    expect(texto).toContain('USR-001')
    expect(texto).toContain('RISK_ANALYST')
    expect(texto).toContain('ALERTAS_RIESGO_LEER')
  })
})

describe('PantallaUsuarioInternoDetalle · acciones sin prompt()', () => {
  it('Suspender abre el diálogo con motivo y manda PATCH estado=SUSPENDIDO; nunca usa window.prompt', async () => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }, { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'u-1' } } } }],
    })
    const http = TestBed.inject(HttpTestingController)
    TestBed.inject(Sesion).abrir('token', ['USUARIOS_INTERNOS_GESTIONAR'], 'OPERADOR')
    const prompt = vi.spyOn(window, 'prompt')
    const f = TestBed.createComponent(PantallaUsuarioInternoDetalle)
    f.detectChanges()
    http.expectOne('http://gw/api/v1/usuarios-internos/u-1').flush(USUARIO_DETALLE)
    const el = await estabilizar(f)
    pulsar(el, 'Suspender cuenta')
    await estabilizar(f)
    const d = el.querySelector('dialog[open]')!
    const area = d.querySelector('textarea') as HTMLTextAreaElement
    area.value = 'Licencia prolongada'
    area.dispatchEvent(new Event('input'))
    escribirCodigoDePrueba(d)
    await estabilizar(f)
    pulsar(d, 'Suspender')
    verificarPasoDeSeguridad(http, 'http://gw/api/v1')
    const req = http.expectOne('http://gw/api/v1/usuarios-internos/u-1')
    expect(req.request.method).toBe('PATCH')
    expect(req.request.body).toEqual({ estado: 'SUSPENDIDO', motivo: 'Licencia prolongada', evidenciaMfa: EVIDENCIA_DE_PRUEBA })
    req.flush(USUARIO_DETALLE)
    f.detectChanges()
    http.expectOne('http://gw/api/v1/usuarios-internos/u-1').flush(USUARIO_DETALLE)
    await estabilizar(f)
    expect(prompt).not.toHaveBeenCalled()
    prompt.mockRestore()
    http.verify()
  })
})

describe('PantallaUsuarioInternoDetalle · operador sin dato de fecha (GU-17)', () => {
  const SIN_DATO: UsuarioInternoDetalle = { ...USUARIO_DETALLE, fechaNacimientoPendiente: true }

  async function montar(permisos: string[], detalle: UsuarioInternoDetalle) {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }, { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'u-1' } } } }],
    })
    const http = TestBed.inject(HttpTestingController)
    TestBed.inject(Sesion).abrir('token', permisos, 'OPERADOR')
    const f = TestBed.createComponent(PantallaUsuarioInternoDetalle)
    f.detectChanges()
    http.expectOne('http://gw/api/v1/usuarios-internos/u-1').flush(detalle)
    const el = await estabilizar(f)
    return { f, el, http }
  }

  it('avisa que falta la fecha y ofrece completarla solo a quien gestiona', async () => {
    const { el } = await montar(['USUARIOS_INTERNOS_GESTIONAR'], SIN_DATO)
    expect(el.textContent).toContain('Falta la fecha de nacimiento')
    expect(Array.from(el.querySelectorAll('button')).some((b) => (b.textContent ?? '').includes('Completar la fecha'))).toBe(true)
  })

  it('sin permiso de gestión el aviso se ve pero no hay acción para completarla', async () => {
    const sinPermiso = await montar([], SIN_DATO)
    expect(sinPermiso.el.textContent).toContain('Falta la fecha de nacimiento')
    expect(Array.from(sinPermiso.el.querySelectorAll('button')).some((b) => (b.textContent ?? '').includes('Completar la fecha'))).toBe(false)
  })

  it('con la fecha cargada no muestra el aviso', async () => {
    const { el } = await montar(['USUARIOS_INTERNOS_GESTIONAR'], USUARIO_DETALLE)
    expect(el.textContent).not.toContain('Falta la fecha de nacimiento')
  })

  it('Completar la fecha manda PATCH con la fecha, el motivo y el segundo factor', async () => {
    const { f, el, http } = await montar(['USUARIOS_INTERNOS_GESTIONAR'], SIN_DATO)
    pulsar(el, 'Completar la fecha')
    await estabilizar(f)
    const d = el.querySelector('dialog[open]')!
    const fecha = Array.from(d.querySelectorAll('input')).find((i) => i.type === 'date') as HTMLInputElement
    fecha.value = '1985-06-15'
    fecha.dispatchEvent(new Event('input'))
    const area = d.querySelector('textarea') as HTMLTextAreaElement
    area.value = 'Dato verificado con su documento'
    area.dispatchEvent(new Event('input'))
    escribirCodigoDePrueba(d)
    await estabilizar(f)
    pulsar(d, 'Guardar la fecha')
    verificarPasoDeSeguridad(http, 'http://gw/api/v1')
    const req = http.expectOne('http://gw/api/v1/usuarios-internos/u-1')
    expect(req.request.method).toBe('PATCH')
    expect(req.request.body).toEqual({ fechaNacimiento: '1985-06-15', motivo: 'Dato verificado con su documento', evidenciaMfa: EVIDENCIA_DE_PRUEBA })
    req.flush({ ...USUARIO_DETALLE })
    f.detectChanges()
    http.expectOne('http://gw/api/v1/usuarios-internos/u-1').flush({ ...USUARIO_DETALLE })
    await estabilizar(f)
    expect(el.textContent).not.toContain('Falta la fecha de nacimiento')
  })
})
