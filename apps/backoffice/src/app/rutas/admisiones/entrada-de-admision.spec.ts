import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY, GW } from '../../nucleo/gateway'
import type { SolicitudDeAdmision } from './dominio/cu68-admision'
import { EntradaDeAdmision } from './entrada-de-admision'

const SOLICITUDES_FIXTURE: SolicitudDeAdmision[] = [
  {
    solicitudId: '9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10',
    usuarioId: 'usr-1',
    postulante: 'Marcelo Rojas',
    grupoId: 'grp-1',
    grupoCodigo: 'PSK-0042',
    cuposSolicitados: 2,
    mensaje: 'Solicito incorporarme al grupo',
    puntaje: '820',
    estado: 'PENDIENTE',
    fechaSolicitud: '2026-09-10T10:00:00-04:00',
  },
  {
    solicitudId: '8a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
    usuarioId: 'usr-2',
    postulante: 'Elena Quispe',
    grupoId: 'grp-2',
    grupoCodigo: 'PSK-0088',
    cuposSolicitados: 1,
    mensaje: 'Participación en ciclo anterior',
    puntaje: '910',
    estado: 'ACEPTADA',
    fechaSolicitud: '2026-09-08T14:30:00-04:00',
  },
  {
    solicitudId: '7f6e5d4c-3b2a-1f0e-9d8c-7b6a5f4e3d2c',
    usuarioId: 'usr-3',
    postulante: 'Jorge Vargas',
    grupoId: 'grp-1',
    grupoCodigo: 'PSK-0042',
    cuposSolicitados: 1,
    mensaje: 'Sin antecedentes de mora',
    puntaje: '650',
    estado: 'RECHAZADA',
    fechaSolicitud: '2026-09-05T09:15:00-04:00',
  },
]

describe('EntradaDeAdmision · Bandeja de solicitudes de ingreso', () => {
  let http: HttpTestingController
  let router: Router

  beforeEach(() => {
    TestBed.resetTestingModule()
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([
          { path: 'admisiones', component: EntradaDeAdmision },
          { path: 'admisiones/:solicitudId', component: EntradaDeAdmision },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    http = TestBed.inject(HttpTestingController)
    router = TestBed.inject(Router)
  })

  afterEach(() => http.verify())

  it('renderiza la tabla con las solicitudes, columnas y datos formateados', async () => {
    const fixture = TestBed.createComponent(EntradaDeAdmision)
    fixture.detectChanges()

    const req = http.expectOne(`${GW}/grupos/solicitudes`)
    req.flush(SOLICITUDES_FIXTURE)
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('Marcelo Rojas')
    expect(texto).toContain('PSK-0042')
    expect(texto).toContain('PENDIENTE')
    expect(texto).toContain('Elena Quispe')
    expect(texto).toContain('PSK-0088')
    expect(texto).toContain('ACEPTADA')
  })

  it('filtra solicitudes por término de búsqueda y por estado mediante queryParams', async () => {
    const fixture = TestBed.createComponent(EntradaDeAdmision)
    fixture.detectChanges()

    http.expectOne(`${GW}/grupos/solicitudes`).flush(SOLICITUDES_FIXTURE)
    await fixture.whenStable()
    fixture.detectChanges()

    const comp = fixture.componentInstance
    expect(comp['encontradas']()).toHaveLength(3)

    // Navegamos con filtro de búsqueda en la URL
    await router.navigate(['/admisiones'], { queryParams: { q: 'elena' } })
    await fixture.whenStable()
    fixture.detectChanges()

    const encontrados = comp['encontradas']()
    expect(encontrados).toHaveLength(1)
    expect(encontrados[0]?.postulante).toBe('Elena Quispe')

    // Probamos filtro por estado
    await router.navigate(['/admisiones'], { queryParams: { estado: 'PENDIENTE' } })
    await fixture.whenStable()
    fixture.detectChanges()

    const pendientes = comp['encontradas']()
    expect(pendientes).toHaveLength(1)
    expect(pendientes[0]?.postulante).toBe('Marcelo Rojas')
  })

  it('muestra estado vacío si el backend devuelve una lista sin solicitudes', async () => {
    const fixture = TestBed.createComponent(EntradaDeAdmision)
    fixture.detectChanges()

    http.expectOne(`${GW}/grupos/solicitudes`).flush([])
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('No hay solicitudes de ingreso')
  })

  it('si el servidor falla (500): muestra el error accionable con «Volver a intentar», no lanza al pintar y la apertura directa sigue funcionando', async () => {
    const fixture = TestBed.createComponent(EntradaDeAdmision)
    fixture.detectChanges()

    // Regresión: leer `value()` de un recurso en error lanza ResourceValueError dentro del cálculo y aborta el
    // render; el operador veía la pantalla congelada y la validación del identificador no se pintaba.
    http.expectOne(`${GW}/grupos/solicitudes`).flush({ codigo: 'AP-ERROR' }, { status: 500, statusText: 'Internal Server Error' })
    await fixture.whenStable()
    expect(() => fixture.detectChanges()).not.toThrow()

    const el = fixture.nativeElement as HTMLElement
    expect(el.querySelector('[role="alert"]')).not.toBeNull()
    expect(Array.from(el.querySelectorAll('button')).some((b) => /volver a intentar/i.test(b.textContent ?? ''))).toBe(true)

    // La apertura directa por identificador no depende de la lista y se pinta aun con la lista caída.
    const comp = fixture.componentInstance
    comp.id.set('no-es-un-uuid')
    comp['abrir']()
    fixture.detectChanges()
    expect(el.textContent).toContain('Ese identificador no tiene el formato de una solicitud.')
  })

  it('permite abrir directamente una solicitud por UUID válido', async () => {
    const fixture = TestBed.createComponent(EntradaDeAdmision)
    fixture.detectChanges()

    http.expectOne(`${GW}/grupos/solicitudes`).flush(SOLICITUDES_FIXTURE)
    await fixture.whenStable()
    fixture.detectChanges()

    const comp = fixture.componentInstance
    comp.id.set('no-es-un-uuid')
    comp['abrir']()
    expect(comp.error()).toBe('Ese identificador no tiene el formato de una solicitud.')

    const uuidValido = '9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10'
    comp.id.set(uuidValido)
    comp['abrir']()
    expect(comp.error()).toBe('')
  })
})
