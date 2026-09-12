import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { ColaDeModeracion } from './cola-de-moderacion'
import type { PiezaCreativa } from '../dominio/cu112-moderacion'

const URL = 'http://gw/api/v1/publicidad/piezas-creativas?estadoModeracion=PENDIENTE'
const PIEZA: PiezaCreativa = {
  piezaCreativaId: 'p-1',
  anuncianteId: 'a-1',
  anuncianteNombre: 'Ferretería del Sur',
  subidaPor: 'op-9',
  titulo: 'Banner de aniversario',
  texto: null,
  urlRecurso: 'https://cdn.aportaya.test/p-1.png',
  tipoRecurso: 'IMAGEN',
  estadoModeracion: 'PENDIENTE',
}

describe('ColaDeModeracion · accesibilidad', () => {
  let http: HttpTestingController
  beforeEach(() => {
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('sin violaciones serias con la cola cargada', async () => {
    TestBed.inject(Sesion).abrir('t', ['PUBLICIDAD_MODERAR'], 'operador')
    const fixture = TestBed.createComponent(ColaDeModeracion)
    fixture.detectChanges()
    http.expectOne(URL).flush([PIEZA])
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
