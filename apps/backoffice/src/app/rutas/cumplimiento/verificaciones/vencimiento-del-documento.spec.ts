import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { ExpedienteEnRevisionEstadoEnum, type ExpedienteEnRevision } from 'clientes/angular/identidad'
import { VencimientoDelDocumento } from './vencimiento-del-documento'

const BASE = {
  verificacionId: 'v-1',
  usuarioId: '9f2c1e4a-0000-4000-8000-000000000001',
  nombreCompleto: 'Marcelo Rojas',
  documento: 'CI 4821993 SC',
  estado: ExpedienteEnRevisionEstadoEnum.EnRevision,
  iniciadaEn: '2026-09-17T10:00:00-04:00',
  fotos: [],
} as unknown as ExpedienteEnRevision

function pintar(extra: Partial<ExpedienteEnRevision>): HTMLElement {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  const fixture = TestBed.createComponent(VencimientoDelDocumento)
  fixture.componentRef.setInput('expediente', { ...BASE, ...extra })
  fixture.detectChanges()
  return (fixture.nativeElement as HTMLElement).querySelector('p')!
}

describe('VencimientoDelDocumento', () => {
  it('muestra el mismo día que trae el contrato, sin correrlo por la zona', () => {
    const p = pintar({ fechaExpiracionDocumento: '2027-05-10', documentoVigente: true })
    expect(p.textContent?.replace(/\s+/g, ' ').trim()).toBe('Vence el 10 may 2027')
    expect(p.classList.contains('marcado')).toBe(false)
  })

  it('vencido se marca y se dice', () => {
    const p = pintar({ fechaExpiracionDocumento: '2026-09-23', documentoVigente: false })
    expect(p.textContent?.replace(/\s+/g, ' ').trim()).toBe('Vence el 23 sep 2026 · Documento vencido')
    expect(p.classList.contains('marcado')).toBe(true)
  })

  it('sin fecha se marca: no hay forma de comprobar la vigencia', () => {
    const p = pintar({ documentoVigente: false })
    expect(p.textContent?.trim()).toBe('Sin fecha de vencimiento')
    expect(p.classList.contains('marcado')).toBe(true)
  })
})
