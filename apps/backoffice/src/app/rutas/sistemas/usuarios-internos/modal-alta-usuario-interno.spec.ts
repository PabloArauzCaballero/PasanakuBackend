import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { estabilizar, pulsar, texto } from '../compartido/pruebas.apoyo'
import { EVIDENCIA_DE_PRUEBA, escribirCodigoDePrueba, verificarPasoDeSeguridad } from './paso-de-seguridad.pruebas'
import { ModalAltaUsuarioInterno } from './modal-alta-usuario-interno'

const GW = 'http://gw/api/v1'
const ROLES = [
  { id: 'r1', codigo: 'SECURITY_ADMIN', nombre: 'Administración de seguridad', ambito: 'INTERNO', departamento: 'TECNOLOGIA', descripcion: null, esSistema: true, permisos: [] },
  { id: 'r2', codigo: 'AUDITOR', nombre: 'Auditor', ambito: 'INTERNO', departamento: 'AUDITORIA', descripcion: null, esSistema: true, permisos: [] },
]
function escribir(raiz: Element, etiqueta: string, valor: string): void {
  const label = Array.from(raiz.querySelectorAll('label')).find((l) => texto(l).startsWith(etiqueta))!
  const control = (raiz.ownerDocument.getElementById(label.htmlFor) ?? label.querySelector('input, textarea')) as HTMLInputElement | HTMLTextAreaElement
  control.value = valor
  control.dispatchEvent(new Event('input'))
}

describe('ModalAltaUsuarioInterno · POST /usuarios-internos', () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }] })
    http = TestBed.inject(HttpTestingController)
  })
  afterEach(() => http.verify())

  async function montar() {
    const f = TestBed.createComponent(ModalAltaUsuarioInterno)
    f.detectChanges()
    http.expectOne(`${GW}/roles?asignablesAPersonalInterno=true`).flush(ROLES)
    const el = await estabilizar(f)
    return { f, el }
  }
  async function completar(f: Parameters<typeof estabilizar>[0], el: HTMLElement, correo = 'nueva.persona@aportaya.test') {
    escribir(el, 'Nombres', 'Ana')
    escribir(el, 'Apellidos', 'Prueba')
    escribir(el, 'Correo institucional', correo)
    escribir(el, 'Teléfono móvil', '+59170000001')
    escribir(el, 'Fecha de nacimiento', '1988-03-21')
    escribir(el, 'Cargo', 'Analista')
    escribir(el, 'Por qué se da de alta', 'Ingreso aprobado por RRHH')
    escribirCodigoDePrueba(el)
    await estabilizar(f)
  }

  it('aceptado: el rol inicial es el primero que trae el catálogo y el POST lleva lo escrito', async () => {
    const { f, el } = await montar()
    expect((Array.from(el.querySelectorAll('select')).at(-1) as HTMLSelectElement).value).toBe('SECURITY_ADMIN')
    await completar(f, el)
    pulsar(el, 'Crear usuario')
    verificarPasoDeSeguridad(http, GW)
    const req = http.expectOne(`${GW}/usuarios-internos`)
    expect(req.request.method).toBe('POST')
    expect(req.request.body).toEqual({ nombres: 'Ana', apellidos: 'Prueba', correo: 'nueva.persona@aportaya.test', telefono: '+59170000001', fechaNacimiento: '1988-03-21', departamento: 'OPERACIONES', cargo: 'Analista', rolCodigo: 'SECURITY_ADMIN', motivo: 'Ingreso aprobado por RRHH', evidenciaMfa: EVIDENCIA_DE_PRUEBA })
    let guardado = false
    f.componentInstance.guardado.subscribe(() => (guardado = true))
    req.flush({})
    expect(guardado).toBe(true)
  })

  it('límite: sin fecha de nacimiento no deja crear y una futura marca el campo (GU-17)', async () => {
    const { f, el } = await montar()
    await completar(f, el)
    const crear = () => Array.from(el.querySelectorAll('button')).find((b) => texto(b) === 'Crear usuario') as HTMLButtonElement
    expect(crear().disabled).toBe(false)
    escribir(el, 'Fecha de nacimiento', '')
    await estabilizar(f)
    expect(crear().disabled).toBe(true)
    escribir(el, 'Fecha de nacimiento', '2999-01-01')
    await estabilizar(f)
    expect(texto(el)).toContain('La fecha de nacimiento no puede ser futura.')
    expect(crear().disabled).toBe(true)
  })

  it('límite: un correo mal escrito marca el campo y no deja crear', async () => {
    const { f, el } = await montar()
    await completar(f, el, 'sin-arroba')
    expect(texto(el)).toContain('Escribí un correo válido')
    expect((Array.from(el.querySelectorAll('button')).find((b) => texto(b) === 'Crear usuario') as HTMLButtonElement).disabled).toBe(true)
  })

  it('error: un 422 deja lo escrito, muestra el mensaje traducido en el motivo y solicita un código nuevo', async () => {
    const { f, el } = await montar()
    await completar(f, el)
    pulsar(el, 'Crear usuario')
    verificarPasoDeSeguridad(http, GW)
    http.expectOne(`${GW}/usuarios-internos`).flush({ codigo: 'AP-CU08-11', mensaje: 'ConstraintViolation correo_uk' }, { status: 422, statusText: 'Unprocessable' })
    await estabilizar(f)
    expect(texto(el.querySelector('ap-area-de-texto [role=alert]'))).toContain('Escribí el motivo')
    expect(texto(el)).not.toContain('ConstraintViolation')
    expect((el.querySelector('input[type=email]') as HTMLInputElement).value).toBe('nueva.persona@aportaya.test')
    expect((Array.from(el.querySelectorAll('button')).find((b) => texto(b) === 'Crear usuario') as HTMLButtonElement).disabled).toBe(true)
  })
})
