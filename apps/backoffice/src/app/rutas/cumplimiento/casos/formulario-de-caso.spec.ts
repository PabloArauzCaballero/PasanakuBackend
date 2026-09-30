import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { FormularioDeCaso } from './formulario-de-caso'

const AQUI = dirname(fileURLToPath(import.meta.url))

/**
 * Kill-test del carril (PR6-SmartPresentational): "buscar en el componente de
 * presentación una dependencia que llegue —directa o transitivamente— a un cliente HTTP
 * de negocio, a la sesión o al almacenamiento. Si aparece una, esto NO está hecho."
 *
 * Se resuelve leyendo el propio archivo fuente, no adivinando: si algún día alguien
 * agrega `inject(ServicioBorrador)` o `inject(HttpClient)` acá, este test lo detecta sin
 * tener que enumerar cada posible import prohibido de antemano.
 */
/** Saca comentarios de bloque y de línea: el archivo EXPLICA en prosa qué no tiene, y esa
 * prosa nombra las mismas palabras prohibidas — sin esto, el test se falsea a sí mismo. */
function sinComentarios(codigo: string): string {
  return codigo.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '')
}

describe('FormularioDeCaso · dependencias prohibidas', () => {
  it('no importa ServicioBorrador, HttpClient, Sesion ni nada de nucleo/ (negocio/sesión/almacenamiento)', () => {
    const codigo = sinComentarios(readFileSync(resolve(AQUI, 'formulario-de-caso.ts'), 'utf-8'))
    const prohibidos = ['ServicioBorrador', 'HttpClient', 'HttpResource', 'httpResource', "from '../../../nucleo", 'Sesion']
    for (const nombre of prohibidos) {
      expect(codigo, `formulario-de-caso.ts no debe referenciar "${nombre}" fuera de sus comentarios`).not.toContain(nombre)
    }
  })

  it('no inyecta nada más que sus propios inputs/outputs — no tiene ningún inject()', () => {
    const codigo = sinComentarios(readFileSync(resolve(AQUI, 'formulario-de-caso.ts'), 'utf-8'))
    expect(codigo).not.toMatch(/\binject\(/)
  })
})

describe('FormularioDeCaso · contrato de vista', () => {
  function montar(props: Partial<Record<'causalElegida' | 'narrativa' | 'borradorRecuperado' | 'puedeConfirmar', unknown>> = {}) {
    TestBed.resetTestingModule()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(FormularioDeCaso)
    fixture.componentRef.setInput('etapas', [])
    fixture.componentRef.setInput('etapaActual', 0)
    fixture.componentRef.setInput('causales', [{ valor: 'X', texto: 'X' }])
    fixture.componentRef.setInput('causalElegida', props.causalElegida ?? null)
    fixture.componentRef.setInput('narrativa', props.narrativa ?? '')
    fixture.componentRef.setInput('borradorRecuperado', props.borradorRecuperado ?? false)
    fixture.componentRef.setInput('puedeConfirmar', props.puedeConfirmar ?? false)
    fixture.componentRef.setInput('textoCausal', 'Causal')
    fixture.componentRef.setInput('textoSinCausal', 'Elegí una causal')
    fixture.componentRef.setInput('textoBorradorRecuperado', 'Se recuperó un borrador')
    fixture.componentRef.setInput('textoConfirmar', 'Confirmar')
    fixture.detectChanges()
    return fixture
  }

  it('emite seEligioCausal cuando cambia la elección — no muta nada, solo pide', () => {
    const fixture = montar()
    let recibido: string | null | undefined
    fixture.componentInstance.seEligioCausal.subscribe((v) => (recibido = v))
    const radio = fixture.nativeElement.querySelector('input[type="radio"]') as HTMLInputElement
    radio.click()
    fixture.detectChanges()
    expect(recibido).toBe('X')
  })

  it('emite seEscribioNarrativa al escribir, con el valor tecleado', () => {
    const fixture = montar()
    let recibido: string | undefined
    fixture.componentInstance.seEscribioNarrativa.subscribe((v) => (recibido = v))
    const area = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement
    area.value = 'texto nuevo'
    area.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    expect(recibido).toBe('texto nuevo')
  })

  it('emite seQuiereConfirmar al pulsar el botón — nunca afirma un éxito que la UI no puede conocer', () => {
    const fixture = montar({ puedeConfirmar: true })
    let veces = 0
    fixture.componentInstance.seQuiereConfirmar.subscribe(() => veces++)
    const boton = fixture.nativeElement.querySelector('ap-boton button') as HTMLButtonElement
    boton.click()
    expect(veces).toBe(1)
  })

  it('el botón queda deshabilitado cuando puedeConfirmar es false, sin volver a evaluar la regla', () => {
    const fixture = montar({ puedeConfirmar: false })
    const boton = fixture.nativeElement.querySelector('ap-boton button') as HTMLButtonElement
    expect(boton.disabled).toBe(true)
    expect(fixture.nativeElement.textContent).toContain('Elegí una causal')
  })

  it('no muta los arreglos que recibe (etapas, causales) al interactuar', () => {
    const etapas = [{ nombre: 'Causal', descripcion: 'd' }]
    const causales = [{ valor: 'X', texto: 'X' }]
    const etapasOriginal = JSON.stringify(etapas)
    const causalesOriginal = JSON.stringify(causales)

    TestBed.resetTestingModule()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(FormularioDeCaso)
    fixture.componentRef.setInput('etapas', etapas)
    fixture.componentRef.setInput('etapaActual', 0)
    fixture.componentRef.setInput('causales', causales)
    fixture.componentRef.setInput('causalElegida', null)
    fixture.componentRef.setInput('narrativa', '')
    fixture.componentRef.setInput('borradorRecuperado', false)
    fixture.componentRef.setInput('puedeConfirmar', false)
    fixture.componentRef.setInput('textoCausal', 'Causal')
    fixture.componentRef.setInput('textoSinCausal', 'Elegí una causal')
    fixture.componentRef.setInput('textoBorradorRecuperado', 'Se recuperó un borrador')
    fixture.componentRef.setInput('textoConfirmar', 'Confirmar')
    fixture.detectChanges()

    const radio = fixture.nativeElement.querySelector('input[type="radio"]') as HTMLInputElement
    radio.click()
    fixture.detectChanges()
    const area = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement
    area.value = 'x'
    area.dispatchEvent(new Event('input'))
    fixture.detectChanges()

    expect(JSON.stringify(etapas)).toBe(etapasOriginal)
    expect(JSON.stringify(causales)).toBe(causalesOriginal)
  })

  it('muestra el aviso de borrador recuperado solo cuando el input lo indica', () => {
    const sinAviso = montar({ borradorRecuperado: false })
    expect(sinAviso.nativeElement.textContent).not.toContain('Se recuperó un borrador')
    const conAviso = montar({ borradorRecuperado: true })
    expect(conAviso.nativeElement.textContent).toContain('Se recuperó un borrador')
  })
})
