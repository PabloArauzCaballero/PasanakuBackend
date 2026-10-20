import { Component, provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { ExplicadorDeVistaComponent } from './explicador-de-vista.component'
import { ExplicadorDeVista } from './explicador-de-vista'

@Component({ template: '' })
class DummyComponent {}

describe('ExplicadorDeVistaComponent', () => {
  let router: Router
  let servicio: ExplicadorDeVista

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([{ path: '**', component: DummyComponent }]),
        ExplicadorDeVista,
      ],
    })
    router = TestBed.inject(Router)
    servicio = TestBed.inject(ExplicadorDeVista)
  })

  function montar(): { fixture: ComponentFixture<ExplicadorDeVistaComponent>; el: HTMLElement } {
    const fixture = TestBed.createComponent(ExplicadorDeVistaComponent)
    fixture.detectChanges()
    return { fixture, el: fixture.nativeElement as HTMLElement }
  }

  it('se renderiza plegado inicialmente y muestra el título del módulo', async () => {
    await router.navigateByUrl('/operacion/custodia')
    const { el } = montar()

    const boton = el.querySelector('.barra-gatillo') as HTMLButtonElement
    expect(boton).not.toBeNull()
    expect(boton.getAttribute('aria-expanded')).toBe('false')
    expect(el.textContent).toContain('Operación')
    expect(el.querySelector('.panel-detallado')).toBeNull()
  })

  it('al hacer clic se despliega y muestra los bloques de Negocio y Sistemas con explicaciones completas', async () => {
    await router.navigateByUrl('/operacion/custodia')
    const { fixture, el } = montar()

    const boton = el.querySelector('.barra-gatillo') as HTMLButtonElement
    boton.click()
    fixture.detectChanges()

    const panel = el.querySelector('.panel-detallado')
    expect(panel).not.toBeNull()

    const negocio = el.querySelector('.bloque-negocio')
    expect(negocio?.textContent).toContain('Explicación de negocio')
    expect(negocio?.textContent).toContain('prueba máxima de honestidad')

    const sistemas = el.querySelector('.bloque-sistemas')
    expect(sistemas?.textContent).toContain('Cómo funciona (Sistemas)')
    expect(sistemas?.textContent).toContain('CU-50')
  })

  it('se colapsa al navegar a una nueva ruta', async () => {
    await router.navigateByUrl('/tablero')
    const { fixture, el } = montar()

    const boton = el.querySelector('.barra-gatillo') as HTMLButtonElement
    boton.click()
    fixture.detectChanges()
    expect(el.querySelector('.panel-detallado')).not.toBeNull()

    await router.navigateByUrl('/operacion/retiros')
    fixture.detectChanges()
    expect(el.querySelector('.panel-detallado')).toBeNull()
  })

  it('muestra la nota operativa adicional si se fijó una', async () => {
    await router.navigateByUrl('/operacion/custodia')
    servicio.fijar('Atención especial por cierre de mes.')
    const { fixture, el } = montar()

    const boton = el.querySelector('.barra-gatillo') as HTMLButtonElement
    boton.click()
    fixture.detectChanges()

    const nota = el.querySelector('.nota-adicional')
    expect(nota?.textContent).toContain('Nota operativa:')
    expect(nota?.textContent).toContain('Atención especial por cierre de mes.')
  })
})
