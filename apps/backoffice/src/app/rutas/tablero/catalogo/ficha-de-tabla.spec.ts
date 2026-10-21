import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, withComponentInputBinding } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { CREDENCIAL, OBLIGACION, resumen } from './catalogo-sintetico'
import { FichaDeTabla } from './ficha-de-tabla'

const BASE = 'http://gw/api/v1/indicadores/catalogo-de-datos'
const ficha = (esquema: string, tabla: string) => `${BASE}/tablas/${esquema}/${tabla}`

/** La ficha de una tabla: cabecera con insignias, pestañas en la URL, secciones clave–valor y relaciones navegables. */
describe('FichaDeTabla', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        // El interceptor real traduce el error: así se ve el 404 tal como lo ve el operador.
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: 'tablero/catalogo-de-datos/:esquema/:tabla', component: FichaDeTabla }], withComponentInputBinding()),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  async function abrir(url: string, respuesta: (r: ReturnType<HttpTestingController['expectOne']>) => void, permisos: string[] = ['TABLERO_SISTEMAS_VER']) {
    TestBed.inject(Sesion).abrir('token', permisos, 'ADMIN_PLATAFORMA')
    const arnes = await RouterTestingHarness.create(url)
    TestBed.inject(ApplicationRef).tick()
    const [, , esquema, tabla] = url.split('?')[0]!.split('/').filter(Boolean)
    http.expectOne(`${BASE}/resumen`).flush(resumen())
    respuesta(http.expectOne(ficha(esquema!, tabla!)))
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    return arnes.routeNativeElement as HTMLElement
  }

  it('correcto: cabecera con esquema.tabla, propósito e insignias; la vista general trae las tres preguntas, la regla y la clasificación', async () => {
    const el = await abrir('/tablero/catalogo-de-datos/aportes/obligacion_aporte', (r) => r.flush(OBLIGACION))
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('aportes.obligacion_aporte')
    const chips = Array.from(el.querySelectorAll('ap-cabecera-de-pagina ap-chip-estado')).map((c) => c.textContent?.trim())
    expect(chips).toEqual(['Detectada', 'Activa'])
    const t = el.textContent ?? ''
    expect(t).toContain('Es el eje del sistema financiero')
    expect(t).toContain('Sin ella no hay deuda formal')
    expect(t).toContain('ck_obligacion_saldo')
    expect(el.querySelector('[role="tablist"]')?.querySelector('[aria-selected="true"]')?.textContent).toContain('Vista general')
    expect(el.querySelector('ap-cabecera-de-pagina ap-boton'), 'sin permiso de edición no hay «Editar ficha»').toBeNull()
  })

  it('correcto: con permiso aparece «Editar ficha»; la pestaña Relaciones (en la URL) lleva a la ficha de la otra tabla', async () => {
    const el = await abrir('/tablero/catalogo-de-datos/aportes/obligacion_aporte?vista=relaciones', (r) => r.flush(OBLIGACION), ['TABLERO_SISTEMAS_VER', 'CATALOGO_DATOS_EDITAR'])
    expect(el.querySelector('ap-cabecera-de-pagina ap-boton')?.textContent).toContain('Editar ficha')
    expect(el.querySelector('a[href="/tablero/catalogo-de-datos/grupos/grupo"]')?.textContent).toContain('grupos.grupo')
    expect(el.querySelector('a[href="/tablero/catalogo-de-datos/aportes/pago"]')?.textContent).toContain('aportes.pago')
    const t = el.textContent ?? ''
    expect(t).toContain('Muchos a uno')
    expect(t).toContain('Impide borrar la referida')
    expect(t).toContain('fk_obligacion_aporte_grupo_id')
  })

  it('límite: una tabla sin relaciones lo dice en vez de dejar la lista vacía; una vista desconocida cae en la general', async () => {
    const el = await abrir('/tablero/catalogo-de-datos/identidad/credencial_acceso?vista=otra', (r) => r.flush(CREDENCIAL))
    expect(el.querySelector('[aria-selected="true"]')?.textContent).toContain('Vista general')
    expect(el.textContent).toContain('Compartido (ningún servicio es dueño)')
  })

  it('inválido: una tabla que no está (404) lo dice sin ofrecer reintentar en vano', async () => {
    const el = await abrir('/tablero/catalogo-de-datos/aportes/tabla_que_no_existe', (r) => r.flush(null, { status: 404, statusText: 'Not Found' }))
    expect(el.textContent).toContain('No encontramos lo que buscabas')
    expect(el.textContent).not.toContain('Volver a intentar')
  })

  it('error: un 500 no muestra el error crudo y ofrece volver a intentar', async () => {
    const el = await abrir('/tablero/catalogo-de-datos/aportes/obligacion_aporte', (r) => r.flush({ detalle: 'NullPointer en X' }, { status: 500, statusText: 'Error' }))
    expect(el.textContent).not.toContain('NullPointer')
    expect(el.textContent).toContain('Volver a intentar')
  })
})
