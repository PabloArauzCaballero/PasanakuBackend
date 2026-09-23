import { httpResource, provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { Component, provideZonelessChangeDetection, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { catchError, throwError } from 'rxjs'
import { describe, expect, it } from 'vitest'
import { EstadoDePantalla, RAMA_DE } from './estado-de-pantalla'

type Dato = { valor: number }

const URL_BASE = 'http://gw/api/v1/dato'

/**
 * El mismo interceptor que usa `apps/backoffice` (`nucleo/errores.interceptor.ts`), copiado
 * acá en miniatura para no importar cruzando de `packages/ui` a una app — `packages/ui` no
 * depende de ninguna app, y esto reproduce exactamente la forma de `ErrorTraducido` que las
 * apps reales adjuntan, sin inventar un doble con otra forma.
 */
function interceptorDePrueba() {
  return (req: import('@angular/common/http').HttpRequest<unknown>, next: import('@angular/common/http').HttpHandlerFn) =>
    next(req).pipe(
      catchError((error: unknown) => {
        const e = error as { status?: number }
        return throwError(() => ({
          mensaje: e.status === 404 ? 'No encontramos lo que buscabas.' : e.status === 0 ? 'No hay conexión. Te mostramos lo último que vimos; para operar hace falta señal.' : 'Algo salió mal de nuestro lado. Probá de nuevo en un momento.',
          estado: e.status,
          sinConexion: e.status === 0,
          trazaId: e.status === 0 ? undefined : 'traza-1',
        }))
      }),
    )
}

function crearAnfitrion() {
  @Component({
    selector: 'ap-anfitrion-estado',
    imports: [EstadoDePantalla],
    template: `
      <ap-estado-de-pantalla
        [recurso]="recurso"
        [vacio]="esVacio"
        mensajeVacio="No hay nada acá."
        [accionVacio]="accionVacio()"
        (actuarEnVacio)="actuaciones.set(actuaciones() + 1)"
        (reintentar)="reintentos.set(reintentos() + 1)"
      >
        @if (recurso.hasValue()) { <p class="contenido">valor: {{ recurso.value().valor }}</p> }
      </ap-estado-de-pantalla>
    `,
  })
  class Anfitrion {
    readonly disparado = signal(false)
    readonly esVacio = (d: Dato) => d.valor === 0
    readonly accionVacio = signal<string | undefined>(undefined)
    readonly actuaciones = signal(0)
    readonly reintentos = signal(0)
    readonly recurso = httpResource<Dato>(() => (this.disparado() ? { url: URL_BASE } : undefined))
  }

  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([interceptorDePrueba()])),
      provideHttpClientTesting(),
    ],
  })
  const fixture = TestBed.createComponent(Anfitrion)
  const http = TestBed.inject(HttpTestingController)
  fixture.detectChanges()
  return { fixture, http }
}

describe('EstadoDePantalla · ramas contra ResourceStatus real (no una unión inventada)', () => {
  it('idle: sin petición todavía, se comporta como vacío en vez de quedar en blanco', () => {
    const { fixture } = crearAnfitrion()
    expect(fixture.componentInstance.recurso.status()).toBe('idle')
    expect(fixture.nativeElement.querySelector('[role="status"]')?.textContent).toContain('No hay nada acá.')
  })

  it('loading: primera carga, esqueleto con etiqueta', () => {
    const { fixture, http } = crearAnfitrion()
    fixture.componentInstance.disparado.set(true)
    fixture.detectChanges()
    expect(fixture.componentInstance.recurso.status()).toBe('loading')
    expect(fixture.nativeElement.querySelector('.esqueleto')).not.toBeNull()
    http.expectOne(URL_BASE).flush({ valor: 5 })
  })

  it('resolved: contenido proyectado, sin esqueleto ni error', async () => {
    const { fixture, http } = crearAnfitrion()
    fixture.componentInstance.disparado.set(true)
    fixture.detectChanges()
    http.expectOne(URL_BASE).flush({ valor: 5 })
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelector('.contenido')?.textContent).toContain('valor: 5')
    expect(fixture.nativeElement.querySelector('.esqueleto')).toBeNull()
  })

  it('vacío (dato con valor 0 según el predicado): usa EstadoVacio, no un <p> suelto', async () => {
    const { fixture, http } = crearAnfitrion()
    fixture.componentInstance.disparado.set(true)
    fixture.detectChanges()
    http.expectOne(URL_BASE).flush({ valor: 0 })
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelector('ap-estado-vacio')).not.toBeNull()
    expect(fixture.nativeElement.querySelector('.titulo')?.textContent).toBe('No hay nada acá.')
    // Sin `accionVacio` y con el motivo por omisión ('sinDatos'), EstadoVacio no deduce ningún
    // botón: la apariencia de los 22 consumidores reales que no pasan acción no cambia.
    expect(fixture.nativeElement.querySelector('ap-boton')).toBeNull()
  })

  it('vacío con acción de siguiente paso: aparece el botón y actuar() se propaga', async () => {
    const { fixture, http } = crearAnfitrion()
    fixture.componentInstance.accionVacio.set('Crear el primero')
    fixture.componentInstance.disparado.set(true)
    fixture.detectChanges()
    http.expectOne(URL_BASE).flush({ valor: 0 })
    await fixture.whenStable()
    const boton = fixture.nativeElement.querySelector('ap-boton button') as HTMLButtonElement
    expect(boton.textContent?.trim()).toBe('Crear el primero')
    boton.click()
    expect(fixture.componentInstance.actuaciones()).toBe(1)
  })

  it('error: el mensaje NO se reescribe (viene tal cual del interceptor de cada app)', async () => {
    const { fixture, http } = crearAnfitrion()
    fixture.componentInstance.disparado.set(true)
    fixture.detectChanges()
    http.expectOne(URL_BASE).error(new ProgressEvent('error'), { status: 0 })
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('No hay conexión. Te mostramos lo último que vimos; para operar hace falta señal.')
    expect(fixture.nativeElement.querySelector('button')?.textContent?.trim()).toBe('Volver a intentar')
  })

  it('error 404: oculta "Volver a intentar" (reintentar la misma petición no arregla un recurso que no existe)', async () => {
    const { fixture, http } = crearAnfitrion()
    fixture.componentInstance.disparado.set(true)
    fixture.detectChanges()
    http.expectOne(URL_BASE).flush({ mensaje: 'tecnico' }, { status: 404, statusText: 'Not Found' })
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('No encontramos lo que buscabas.')
    expect(fixture.nativeElement.querySelector('button')).toBeNull()
  })

  it('reintentar: el botón de error emite el output, la traducción del error no es cosa del host', async () => {
    const { fixture, http } = crearAnfitrion()
    fixture.componentInstance.disparado.set(true)
    fixture.detectChanges()
    http.expectOne(URL_BASE).flush({ mensaje: 'tecnico' }, { status: 500, statusText: 'Internal Server Error' })
    await fixture.whenStable()
    ;(fixture.nativeElement.querySelector('button') as HTMLButtonElement).click()
    expect(fixture.componentInstance.reintentos()).toBe(1)
  })

  it('reloading (obsoleto): con un valor ya visible, NO se tapa con el esqueleto — se marca aria-busy', async () => {
    const { fixture, http } = crearAnfitrion()
    fixture.componentInstance.disparado.set(true)
    fixture.detectChanges()
    http.expectOne(URL_BASE).flush({ valor: 5 })
    await fixture.whenStable()

    fixture.componentInstance.recurso.reload()
    fixture.detectChanges()

    expect(fixture.componentInstance.recurso.status()).toBe('reloading')
    expect(fixture.nativeElement.querySelector('.esqueleto')).toBeNull()
    expect(fixture.nativeElement.querySelector('.contenido')?.textContent).toContain('valor: 5')
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull()

    http.expectOne(URL_BASE).flush({ valor: 5 })
    await fixture.whenStable()
  })

  it('local: un `.set()` directo (sin recarga) se pinta como contenido resuelto', async () => {
    const { fixture, http } = crearAnfitrion()
    fixture.componentInstance.disparado.set(true)
    fixture.detectChanges()
    http.expectOne(URL_BASE).flush({ valor: 5 })
    await fixture.whenStable()

    fixture.componentInstance.recurso.set({ valor: 9 })
    fixture.detectChanges()

    expect(fixture.componentInstance.recurso.status()).toBe('local')
    expect(fixture.nativeElement.querySelector('.contenido')?.textContent).toContain('valor: 9')
  })

  it('exhaustividad: los seis valores reales de ResourceStatus están todos contemplados (no una lista de diez inventada)', () => {
    // `RAMA_DE` está tipado `Record<ResourceStatus, …>`: sacarle una clave o que Angular agregue
    // una nueva a `ResourceStatus` sin actualizar acá rompe `yarn typecheck`, no silenciosamente
    // en producción. Esta aserción es el testigo en tiempo de ejecución de esa garantía de tipos.
    expect(Object.keys(RAMA_DE).sort()).toEqual(['error', 'idle', 'loading', 'local', 'reloading', 'resolved'])
  })

  it('el host no depende de sesión ni de router: no decide autorización ni navega', () => {
    // Si `EstadoDePantalla` necesitara Router o un proveedor de sesión, `TestBed.createComponent`
    // de este spec (que solo provee HttpClient de prueba) fallaría al montar. No falla.
    const { fixture } = crearAnfitrion()
    expect(fixture.componentInstance).toBeTruthy()
  })
})
