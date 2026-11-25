import type { HttpTestingController } from '@angular/common/http/testing'
import { expect } from 'vitest'

export const EVIDENCIA_DE_PRUEBA = 'e'.repeat(40)

export function escribirCodigoDePrueba(dialogo: Element, codigo = '123456'): void {
  const campo = dialogo.querySelector<HTMLInputElement>('ap-campo-otp input')!
  campo.value = codigo
  campo.dispatchEvent(new Event('input'))
}

export function verificarPasoDeSeguridad(http: HttpTestingController, gateway: string): void {
  const desafio = http.expectOne(`${gateway}/sesiones/desafios`)
  expect(desafio.request.body).toEqual({ proposito: 'ADMIN' })
  desafio.flush({ desafioId: 'd1', expiraEn: '2026-11-25T10:05:00Z' })
  const verificacion = http.expectOne(`${gateway}/sesiones/desafios/d1/verificacion`)
  expect(verificacion.request.body).toEqual({ factor: { tipo: 'TOTP', valor: '123456' } })
  verificacion.flush({ evidencia: EVIDENCIA_DE_PRUEBA, expiraEn: '2026-11-25T10:05:00Z', jti: 'j1' })
}
