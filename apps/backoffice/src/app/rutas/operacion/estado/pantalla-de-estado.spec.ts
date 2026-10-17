import { ApplicationRef, Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { Router, provideRouter } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { describe, expect, it } from 'vitest'
import { Sesion } from '../../../nucleo/sesion'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaDeEstado } from './pantalla-de-estado'

@Component({ selector: 'ap-anfitrion', imports: [PantallaDeEstado], template: `<ap-pantalla-de-estado />` })
class Anfitrion {}

/**
 * `operacion/estado`: prueba que el estado de la tabla (filtro de nivel, en la URL)
 * sobrevive a "recargar" — acá, a volver a crear el arnés de ruta con la misma URL — y
 * que el botón de exportar respeta el permiso del rol (CU-58).
 */
describe('PantallaDeEstado · estado en la URL y exportación con permiso', () => {
  function montarProviders() {
    return [
      provideZonelessChangeDetection(),
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([{ path: '**', component: Anfitrion }]),
      { provide: GATEWAY, useValue: 'http://gw/api/v1' },
    ]
  }

  const URL = 'http://gw/api/v1/indicadores/operacion/servicios'
  const SANOS = { consultadoEn: '2026-10-17T15:04:05Z', servicios: [] }

  /** Dispara el efecto del recurso (`whenStable` esperaría a la petición pendiente, que el test es quien responde). */
  function disparar(arnes: RouterTestingHarness) {
    arnes.fixture.detectChanges()
    TestBed.inject(ApplicationRef).tick()
  }

  /** Monta en `url`, responde la salud con `cuerpo` y espera. */
  async function montarRespondiendo(url: string, cuerpo: object = SANOS) {
    const arnes = await RouterTestingHarness.create(url)
    disparar(arnes)
    TestBed.inject(HttpTestingController).expectOne(URL).flush(cuerpo)
    await arnes.fixture.whenStable()
    return arnes
  }

  it('sin el permiso de exportación, el botón no aparece', async () => {
    TestBed.configureTestingModule({ providers: montarProviders() })
    TestBed.inject(Sesion).abrir('t', [], 'consulta')
    const arnes = await montarRespondiendo('/')
    const botones = Array.from(arnes.routeNativeElement?.querySelectorAll('.cabecera button') ?? [])
    expect(botones).toHaveLength(0)
  })

  it('con el permiso, el botón de exportar aparece', async () => {
    TestBed.configureTestingModule({ providers: montarProviders() })
    TestBed.inject(Sesion).abrir('t', ['exportar:estado-plataforma'], 'oficial')
    const arnes = await montarRespondiendo('/')
    expect(arnes.routeNativeElement?.querySelector('.cabecera button')?.textContent).toContain('Exportar')
  })

  it('pegar la URL con un filtro de nivel ya aplicado muestra solo esa página filtrada', async () => {
    TestBed.configureTestingModule({ providers: montarProviders() })
    TestBed.inject(Sesion).abrir('t', [], 'oficial')
    const arnes = await montarRespondiendo('/?filtro=N1')
    const router = TestBed.inject(Router)
    expect(router.url).toContain('filtro=N1')
    const texto = arnes.routeNativeElement?.textContent ?? ''
    expect(texto).toContain('identidad')
    expect(texto).not.toContain('publicidad')
  })

  async function montar() {
    TestBed.configureTestingModule({ providers: montarProviders() })
    TestBed.inject(Sesion).abrir('t', ['TABLERO_SISTEMAS_VER'], 'oficial')
    const arnes = await RouterTestingHarness.create('/')
    disparar(arnes)
    return { arnes, http: TestBed.inject(HttpTestingController) }
  }
  const fila = (arnes: RouterTestingHarness, servicio: string) =>
    Array.from(arnes.routeNativeElement?.querySelectorAll('tr') ?? []).find((tr) => tr.textContent?.includes(servicio))?.textContent ?? ''

  it('la salud es la que contestó cada servicio: uno caído dice «no responde», no «arriba»', async () => {
    const { arnes, http } = await montar()
    http.expectOne(URL).flush({
      consultadoEn: '2026-10-17T15:04:05Z',
      servicios: [
        { servicio: 'identidad', estado: 'ARRIBA', latenciaMs: 12 },
        { servicio: 'aportes', estado: 'DEGRADADO', latenciaMs: 40 },
        { servicio: 'erp', estado: 'CAIDO' },
      ],
    })
    await arnes.fixture.whenStable()
    expect(fila(arnes, 'identidad')).toContain('arriba')
    expect(fila(arnes, 'identidad')).toContain('12 ms')
    expect(fila(arnes, 'aportes')).toContain('con problemas')
    expect(fila(arnes, 'erp')).toContain('no responde')
    // Un servicio que la respuesta no trae no se rellena con «arriba».
    expect(fila(arnes, 'tarifas')).toContain('sin dato')
    expect(arnes.routeNativeElement?.textContent).toContain('Salud consultada a las 11:04:05')
  })

  it('mientras pregunta, ninguna fila dice «arriba»', async () => {
    const { arnes, http } = await montar()
    arnes.fixture.detectChanges()
    expect(arnes.routeNativeElement?.textContent).toContain('Preguntando a cada servicio')
    expect(fila(arnes, 'identidad')).not.toContain('arriba')
    http.expectOne(URL)
  })

  it('sin permiso (403): lo dice, y la tabla queda con la arquitectura sin estado', async () => {
    const { arnes, http } = await montar()
    http.expectOne(URL).flush({ codigo: 'AP-403' }, { status: 403, statusText: 'Forbidden' })
    await arnes.fixture.whenStable()
    expect(arnes.routeNativeElement?.textContent).toContain('Tu rol no puede ver la salud')
    expect(fila(arnes, 'identidad')).toContain('sin permiso')
    expect(fila(arnes, 'identidad')).not.toContain('arriba')
  })

  it('si la consulta falla (500): «sin dato» y se puede volver a consultar', async () => {
    const { arnes, http } = await montar()
    http.expectOne(URL).flush({ codigo: 'AP-500' }, { status: 500, statusText: 'Error' })
    await arnes.fixture.whenStable()
    expect(arnes.routeNativeElement?.textContent).toContain('No se pudo consultar la salud')
    expect(fila(arnes, 'identidad')).toContain('sin dato')
    const reintentar = Array.from(arnes.routeNativeElement?.querySelectorAll('button') ?? []).find((b) => b.textContent?.includes('Volver a consultar'))
    reintentar?.click()
    disparar(arnes)
    http.expectOne(URL)
  })
})
