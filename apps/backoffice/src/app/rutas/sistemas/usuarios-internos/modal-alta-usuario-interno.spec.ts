import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { estabilizar, pulsar, texto } from '../compartido/pruebas.apoyo'
import { ModalAltaUsuarioInterno } from './modal-alta-usuario-interno'

const GW = 'http://gw/api/v1'
const ROLES = [
  { id: 'r1', codigo: 'SECURITY_ADMIN', nombre: 'Administración de seguridad', ambito: 'INTERNO', departamento: 'TECNOLOGIA', descripcion: null, esSistema: true, permisos: [] },
  { id: 'r2', codigo: 'AUDITOR', nombre: 'Auditor', ambito: 'INTERNO', departamento: 'AUDITORIA', descripcion: null, esSistema: true, permisos: [] },
]
function escribir(raiz: Element, etiqueta: string, valor: string): void {
  const label = Array.from(raiz.querySelectorAll('label')).find((l) => texto(l).startsWith(etiqueta))!
  const control = (raiz.ownerDocument.getElementById(label.htmlFor) ?? label.querySelector('input')) as HTMLInputElement
  control.value = valor
  control.dispatchEvent(new Event('input'))
}

describe('ModalAltaUsuarioInterno · POST /usuarios-internos', () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }] })
    http = TestBed.inject(HttpTestingController)
  })
  afterEach(() => http.verify())

  async function montar() {
    const f = TestBed.createComponent(ModalAltaUsuarioInterno)
    f.detectChanges()
    http.expectOne(`${GW}/roles`).flush(ROLES)
    const el = await estabilizar(f)
    return { f, el }
  }
  async function completar(f: Parameters<typeof estabilizar>[0], el: HTMLElement, correo = 'nueva.persona@aportaya.test') {
    escribir(el, 'Nombres', 'Ana')
    escribir(el, 'Apellidos', 'Prueba')
    escribir(el, 'Correo institucional', correo)
    escribir(el, 'Teléfono móvil', '+59170000001')
    escribir(el, 'Cargo', 'Analista')
    await estabilizar(f)
  }

  it('aceptado: el rol inicial es el primero que trae el catálogo y el POST lleva lo escrito', async () => {
    const { f, el } = await montar()
    expect((Array.from(el.querySelectorAll('select')).at(-1) as HTMLSelectElement).value).toBe('SECURITY_ADMIN')
    await completar(f, el)
    pulsar(el, 'Crear usuario')
    const req = http.expectOne(`${GW}/usuarios-internos`)
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({ nombres: 'Ana', apellidos: 'Prueba', correo: 'nueva.persona@aportaya.test', telefono: '+59170000001', departamento: 'OPERACIONES', cargo: 'Analista', rolCodigo: 'SECURITY_ADMIN' })
    let guardado = false
    f.componentInstance.guardado.subscribe(() => (guardado = true))
    req.flush({})
    expect(guardado).toBe(true)
  })

  it('límite: un correo mal escrito marca el campo y no deja crear', async () => {
    const { f, el } = await montar()
    await completar(f, el, 'sin-arroba')
    expect(texto(el)).toContain('Escribí un correo válido')
    expect((Array.from(el.querySelectorAll('button')).find((b) => texto(b) === 'Crear usuario') as HTMLButtonElement).disabled).toBe(true)
  })

  it('error: un 422 deja lo escrito, muestra el mensaje traducido (no el crudo) y libera el botón', async () => {
    const { f, el } = await montar()
    await completar(f, el)
    pulsar(el, 'Crear usuario')
    http.expectOne(`${GW}/usuarios-internos`).flush({ codigo: 'AP-X', mensaje: 'ConstraintViolation correo_uk' }, { status: 422, statusText: 'Unprocessable' })
    await estabilizar(f)
    expect(texto(el.querySelector('ap-error-de-envio'))).toContain('No se pudo dar de alta')
    expect(texto(el)).not.toContain('ConstraintViolation')
    expect((el.querySelector('input[type=email]') as HTMLInputElement).value).toBe('nueva.persona@aportaya.test')
    expect((Array.from(el.querySelectorAll('button')).find((b) => texto(b) === 'Crear usuario') as HTMLButtonElement).disabled).toBe(false)
  })
})
