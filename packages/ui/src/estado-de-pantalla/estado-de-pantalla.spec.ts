import { ChangeDetectionStrategy, Component, provideZonelessChangeDetection, signal } from '@angular/core'
import type { ResourceRef } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { EstadoDePantalla, type ErrorTraducido, type MotivoVacio } from './estado-de-pantalla'

/** Fake mínimo de `ResourceRef`: solo la superficie que el host realmente lee (H1.S1.M4). */
function recursoFalso(config: { cargando?: boolean; error?: unknown; valor?: unknown; status?: string }): ResourceRef<unknown> {
  return {
    isLoading: () => !!config.cargando,
    error: () => config.error,
    hasValue: () => config.valor !== undefined,
    value: () => config.valor,
    status: () => config.status ?? (config.cargando ? 'loading' : config.error ? 'error' : config.valor !== undefined ? 'resolved' : 'idle'),
  } as unknown as ResourceRef<unknown>
}

@Component({
  selector: 'ap-anfitrion-sin-plantillas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla],
  template: `
    <ap-estado-de-pantalla [recurso]="recurso()" [vacio]="esVacio" mensajeVacio="No hay nada acá." [motivoVacio]="motivo()" etiquetaDeCarga="Cargando la cosa" (reintentar)="reintentos.set(reintentos() + 1)">
      <p class="listo">Contenido real</p>
    </ap-estado-de-pantalla>
  `,
})
class AnfitrionSinPlantillas {
  readonly recurso = signal(recursoFalso({ cargando: true }))
  readonly motivo = signal<MotivoVacio>('sinDatos')
  readonly reintentos = signal(0)
  protected esVacio = (dato: unknown) => dato === 'vacio'
}

@Component({
  selector: 'ap-anfitrion-con-plantillas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla],
  template: `
    <ap-estado-de-pantalla [recurso]="recurso()" [vacio]="esVacio" mensajeVacio="mensaje por defecto, no debería verse" (reintentar)="reintentos.set(reintentos() + 1)">
      <ng-template #plantillaVacio let-motivo>
        <button type="button" class="siguiente-paso">{{ motivo === 'porFiltro' ? 'Quitar filtros' : 'Acción propia' }}</button>
      </ng-template>
      <ng-template #plantillaError let-error>
        <p class="error-tipado">{{ error.mensaje }} (estado {{ error.estado }})</p>
      </ng-template>
      <p class="listo">Contenido real</p>
    </ap-estado-de-pantalla>
  `,
})
class AnfitrionConPlantillas {
  readonly recurso = signal(recursoFalso({ cargando: true }))
  readonly reintentos = signal(0)
  protected esVacio = (dato: unknown) => dato === 'vacio'
}

function montar<T>(tipo: new () => T) {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  const fixture = TestBed.createComponent(tipo)
  fixture.detectChanges()
  return fixture
}

describe('EstadoDePantalla · las cuatro ramas reales (H2.S1.M1)', () => {
  it('cargando: role=status y aria-live=polite explícito (H2.S2.M3, brecha #4 de contrato-view-state.md cerrada)', () => {
    const fixture = montar(AnfitrionSinPlantillas)
    const nodo = fixture.nativeElement.querySelector('[role="status"]') as HTMLElement
    expect(nodo).not.toBeNull()
    expect(nodo.getAttribute('aria-live')).toBe('polite')
    expect(nodo.getAttribute('aria-label')).toBe('Cargando la cosa')
  })

  it('error: role=alert, aria-live=assertive, mensaje traducido y trazaId solo si el contrato lo trae — nunca el error crudo', () => {
    const fixture = montar(AnfitrionSinPlantillas)
    ;(fixture.componentInstance as AnfitrionSinPlantillas).recurso.set(recursoFalso({ error: { mensaje: 'Traducido', trazaId: 'abc-123', estado: 500 } as ErrorTraducido }))
    fixture.detectChanges()
    const seccion = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement
    expect(seccion.getAttribute('aria-live')).toBe('assertive')
    expect(seccion.textContent).toContain('Traducido')
    expect(seccion.textContent).toContain('Código de seguimiento: abc-123')
    const boton = seccion.querySelector('button') as HTMLButtonElement
    expect(boton.textContent?.trim()).toBe('Volver a intentar')
  })

  it('error sin trazaId (el contrato no lo trae): no se inventa un "Código de seguimiento"', () => {
    const fixture = montar(AnfitrionSinPlantillas)
    ;(fixture.componentInstance as AnfitrionSinPlantillas).recurso.set(recursoFalso({ error: { mensaje: 'Traducido' } as ErrorTraducido }))
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector('.traza')).toBeNull()
  })

  it('vacío: role=status, aria-live=polite, orienta con mensajeVacio cuando no hay plantilla', () => {
    const fixture = montar(AnfitrionSinPlantillas)
    ;(fixture.componentInstance as AnfitrionSinPlantillas).recurso.set(recursoFalso({ valor: 'vacio' }))
    fixture.detectChanges()
    const seccion = fixture.nativeElement.querySelector('.vacio') as HTMLElement
    expect(seccion.getAttribute('role')).toBe('status')
    expect(seccion.getAttribute('aria-live')).toBe('polite')
    expect(seccion.textContent).toContain('No hay nada acá.')
  })

  it('listo: contenido proyectado, sin ninguna de las otras tres ramas en el DOM', () => {
    const fixture = montar(AnfitrionSinPlantillas)
    ;(fixture.componentInstance as AnfitrionSinPlantillas).recurso.set(recursoFalso({ valor: 'con-dato' }))
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector('.listo')?.textContent).toBe('Contenido real')
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull()
    expect(fixture.nativeElement.querySelector('.vacio')).toBeNull()
    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeNull()
  })

  it('reintentar emite hacia el consumidor; el host no decide política de reintento (H2.S1.M3)', () => {
    const fixture = montar(AnfitrionSinPlantillas)
    ;(fixture.componentInstance as AnfitrionSinPlantillas).recurso.set(recursoFalso({ error: { mensaje: 'x' } }))
    fixture.detectChanges()
    ;(fixture.nativeElement.querySelector('[role="alert"] button') as HTMLButtonElement).click()
    fixture.detectChanges()
    expect((fixture.componentInstance as AnfitrionSinPlantillas).reintentos()).toBe(1)
  })
})

describe('EstadoDePantalla · plantilla con contexto tipado por consumidor (H2.S1.M1/M2)', () => {
  it('vacío con plantilla propia: la acción de siguiente paso la aporta el consumidor, con el motivo tipado como contexto', () => {
    const fixture = montar(AnfitrionConPlantillas)
    ;(fixture.componentInstance as AnfitrionConPlantillas).recurso.set(recursoFalso({ valor: 'vacio' }))
    fixture.detectChanges()
    const boton = fixture.nativeElement.querySelector('.siguiente-paso') as HTMLButtonElement
    expect(boton).not.toBeNull()
    expect(boton.textContent).toBe('Acción propia')
    // el mensaje por defecto ("mensaje por defecto...") no debe aparecer: la plantilla reemplaza, no convive
    expect(fixture.nativeElement.textContent).not.toContain('mensaje por defecto')
  })

  it('error con plantilla propia: recibe el ErrorTraducido completo (mensaje + estado), nunca el error crudo del backend', () => {
    const fixture = montar(AnfitrionConPlantillas)
    ;(fixture.componentInstance as AnfitrionConPlantillas).recurso.set(recursoFalso({ error: { mensaje: 'Traducido', estado: 404 } as ErrorTraducido }))
    fixture.detectChanges()
    const parrafo = fixture.nativeElement.querySelector('.error-tipado') as HTMLElement
    expect(parrafo.textContent).toContain('Traducido')
    expect(parrafo.textContent).toContain('404')
  })
})

describe('EstadoDePantalla · el host no traduce transporte ni decide autorización (H2.S1.M3)', () => {
  it('el archivo fuente no importa HttpClient, Router ni nada que decida sesión/endpoints', () => {
    const fuente = readFileSync(join(__dirname, 'estado-de-pantalla.ts'), 'utf8')
    expect(fuente).not.toMatch(/HttpClient|@angular\/common\/http|@angular\/router|Auth|Sesion|Session/i)
  })
})
