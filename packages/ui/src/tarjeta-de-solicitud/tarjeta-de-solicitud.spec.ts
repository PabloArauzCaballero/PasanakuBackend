import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it, vi } from 'vitest'
import { TarjetaDeSolicitud } from './tarjeta-de-solicitud'

describe('ap-tarjeta-de-solicitud', () => {
  it('rechazar exige motivo', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(TarjetaDeSolicitud)
    fixture.componentRef.setInput('nombre', 'Carlos')
    fixture.componentRef.setInput('detalle', 'Pide el cupo 7')
    fixture.componentRef.setInput('cuandoIso', '2026-09-10T15:00:00Z')
    const rechazado = vi.fn()
    fixture.componentInstance.rechazar.subscribe(rechazado)
    fixture.detectChanges()
    const boton = (texto: string) => [...(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)].find((b) => b.textContent?.includes(texto))!
    boton('Rechazar').click()
    await fixture.whenStable()
    boton('Rechazar con este motivo').click()
    await fixture.whenStable()
    expect(rechazado).not.toHaveBeenCalled()
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('motivo')
    fixture.componentInstance.motivo.set('El cupo ya está cubierto')
    boton('Rechazar con este motivo').click()
    await fixture.whenStable()
    expect(rechazado).toHaveBeenCalledWith('El cupo ya está cubierto')
  })
})
