import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import type { PaginaDeAccesos, SeccionDeAccesos } from '../dominio/gobierno-de-accesos'
import { PantallaAccesos } from './pantalla-accesos'

export const GW_ACCESOS = 'http://gw/api/v1'
export const URL_INFORME = `${GW_ACCESOS}/usuarios-internos/accesos/informe`
export const URL_REVOCAR = `${GW_ACCESOS}/usuarios-internos/asignaciones/asignacion-1/revocacion`
export function paginaDePrueba(seccion: SeccionDeAccesos, pagina = 1, total = 1): PaginaDeAccesos {
  return {
    seccion, pagina, total, limite: 25, totalPaginas: Math.ceil(total / 25), generadoEn: '2026-11-25T15:00:00Z',
    items: total ? [{
      asignacionId: 'asignacion-1', usuarioId: 'usuario-1', nombre: 'Persona de prueba', estadoCuenta: 'ACTIVO', rolCodigo: 'AUDITOR',
      ambito: 'GLOBAL', otorgadaPor: 'usuario-2', otorgadaEn: '2026-11-24T15:00:00Z', vigenteHasta: '2026-12-01T15:00:00Z', totpConfirmado: false,
    }] : [],
  }
}

export function montarAccesos(permisos: string[] = ['ACCESOS_ADMINISTRAR']) {
  TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW_ACCESOS }] })
  TestBed.inject(Sesion).abrir('token', permisos, 'OPERADOR')
  const http = TestBed.inject(HttpTestingController)
  const fixture = TestBed.createComponent(PantallaAccesos)
  fixture.detectChanges()
  return { http, fixture }
}

export function responderPagina(http: HttpTestingController, seccion: SeccionDeAccesos, pagina = 1, total = 1): void {
  http.expectOne(`${URL_INFORME}?seccion=${seccion}&pagina=${pagina}&limite=25`).flush(paginaDePrueba(seccion, pagina, total))
}
