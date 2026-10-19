import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { describe, expect, it } from 'vitest'
import { axe } from 'vitest-axe'
import { GATEWAY } from '../nucleo/gateway'
import { AvisosInternosServicio } from '../nucleo/avisos-internos'
import { AvisoInternoTipoEnum, type AvisoInterno } from 'clientes/angular/notificaciones'
import { CampanaDeNotificaciones } from './campana-de-notificaciones'

describe('CampanaDeNotificaciones · accesibilidad', () => {
  it('sin violaciones WCAG serias en estado cerrado y abierto', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'https://gateway.test/api/v1' },
      ],
    })
    const http = TestBed.inject(HttpTestingController)
    const servicio = TestBed.inject(AvisosInternosServicio)
    const fixture = TestBed.createComponent(CampanaDeNotificaciones)
    fixture.detectChanges()

    // 1. Cerrado
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()

    // 2. Abierto con avisos
    const aviso: AvisoInterno = {
      id: '11111111-1111-4111-8111-111111111111',
      titulo: 'Alerta crítica de seguridad',
      mensaje: 'Revisión obligatoria de permisos asignados',
      tipo: AvisoInternoTipoEnum.Alerta,
      creadoEn: '2026-10-19T10:00:00Z',
    }
    servicio.cargar().subscribe()
    http.expectOne((r) => r.url.includes('/notificaciones/avisos-internos')).flush({
      totalNoLeidos: 1,
      elementos: [aviso],
    })
    await fixture.whenStable()
    fixture.detectChanges()

    ;(fixture.nativeElement.querySelector('.disparador') as HTMLButtonElement).click()
    http.expectOne((r) => r.url.includes('/notificaciones/avisos-internos')).flush({
      totalNoLeidos: 1,
      elementos: [aviso],
    })
    await fixture.whenStable()
    fixture.detectChanges()

    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
