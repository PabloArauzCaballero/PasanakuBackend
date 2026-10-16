import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { ErrorDeEnvio, type ErrorParaVista } from './error-de-envio'

function montar(error: ErrorParaVista | null) {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  const f = TestBed.createComponent(ErrorDeEnvio)
  f.componentRef.setInput('error', error)
  f.detectChanges()
  return f.nativeElement as HTMLElement
}

describe('ap-error-de-envio', () => {
  it('sin error no pinta nada', () => {
    expect(montar(null).querySelector('ap-alerta')).toBeNull()
  })

  it('un rechazo del servidor dice que NO se completó, con el código de seguimiento, y no promete nada sobre repetir', () => {
    const el = montar({ mensaje: 'El cobro no puede exceder el saldo.', estado: 422, trazaId: 't-1' })
    expect(el.textContent).toContain('No se pudo completar')
    expect(el.textContent).toContain('Código de seguimiento: t-1')
    expect(el.textContent).not.toContain('no se duplica')
    expect(el.querySelector('[role="alert"]')).not.toBeNull()
  })

  it('sin conexión, plazo vencido o 5xx: «no pudimos confirmar» y repetir lo mismo no duplica', () => {
    for (const e of [{ mensaje: 'x', estado: 0, sinConexion: true }, { mensaje: 'x', estado: 0, tiempoAgotado: true }, { mensaje: 'x', estado: 503 }]) {
      const el = montar(e)
      expect(el.textContent).toContain('No pudimos confirmar si se registró')
      expect(el.textContent).toContain('no se duplica')
    }
  })

  it('accesibilidad: sin violaciones en los dos casos', async () => {
    for (const e of [{ mensaje: 'Rechazado', estado: 422 }, { mensaje: 'Sin red', estado: 0, sinConexion: true, trazaId: 'abc' }]) {
      expect(await axe(montar(e), { rules: { 'color-contrast': { enabled: false } } })).toHaveNoViolations()
    }
  })
})
