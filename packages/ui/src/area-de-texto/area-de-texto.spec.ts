import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { AreaDeTexto } from './area-de-texto'
import { Pestanas } from '../pestanas/pestanas'
import { Campo } from '../campo/campo'

describe('AreaDeTexto, Campo con tooltip y Pestanas segmentadas', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  it('área: etiqueta vinculada, contador y ayuda por aria-describedby; el error la reemplaza', () => {
    const fixture = TestBed.createComponent(AreaDeTexto)
    fixture.componentRef.setInput('etiqueta', 'Nota de revisión')
    fixture.componentRef.setInput('maximo', 500)
    fixture.componentRef.setInput('ayuda', 'Por qué se decidió así.')
    fixture.componentRef.setInput('tooltip', 'Queda en la bitácora.')
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    const area = el.querySelector('textarea') as HTMLTextAreaElement
    expect(el.querySelector('label')?.getAttribute('for')).toBe(area.id)
    expect(area.getAttribute('aria-describedby')).toBe(`${area.id}-ayuda ${area.id}-contador`)
    expect(el.querySelector('.ayuda-boton')?.getAttribute('aria-label')).toBe('Ayuda sobre Nota de revisión')
    area.value = ' hola '
    area.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    expect(el.querySelector('.contador')?.textContent).toBe('4 / 500')
    fixture.componentRef.setInput('error', 'Explicá el rechazo.')
    fixture.detectChanges()
    expect(area.getAttribute('aria-invalid')).toBe('true')
    expect(area.getAttribute('aria-describedby')).toBe(`${area.id}-error ${area.id}-contador`)
  })

  it('campo: el tooltip aparece junto a la etiqueta con nombre accesible, sin cambiar el campo', () => {
    const fixture = TestBed.createComponent(Campo)
    fixture.componentRef.setInput('etiqueta', 'Responsable')
    fixture.componentRef.setInput('tooltip', 'Equipo o persona que responde.')
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    expect(el.querySelector('.ayuda-boton')?.getAttribute('aria-label')).toBe('Ayuda sobre Responsable')
    expect(el.querySelector('[role="tooltip"]')?.textContent).toBe('Equipo o persona que responde.')
    expect(el.querySelector('label')?.getAttribute('for')).toBe(el.querySelector('input')?.id)
  })

  it('pestañas: Inicio y Fin saltan a la primera y a la última; la variante segmentada se marca en el host', () => {
    const fixture = TestBed.createComponent(Pestanas)
    fixture.componentRef.setInput('etiqueta', 'Vista')
    fixture.componentRef.setInput('estilo', 'segmentada')
    fixture.componentRef.setInput('pestanas', [{ valor: 'general', texto: 'Vista general' }, { valor: 'columnas', texto: 'Columnas' }, { valor: 'gobierno', texto: 'Gobierno' }])
    fixture.componentInstance.elegida.set('columnas')
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    expect(el.classList.contains('segmentada')).toBe(true)
    const botones = el.querySelectorAll('button')
    botones[1]!.dispatchEvent(new KeyboardEvent('keydown', { key: 'End', bubbles: true }))
    expect(fixture.componentInstance.elegida()).toBe('gobierno')
    botones[2]!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Home', bubbles: true }))
    expect(fixture.componentInstance.elegida()).toBe('general')
  })
})
