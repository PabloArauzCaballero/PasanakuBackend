import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { Catalogo } from './catalogo'

const SRC = join(__dirname, '..')
const PIEZAS = readdirSync(SRC, { withFileTypes: true })
  .filter((d) => d.isDirectory() && !['catalogo', 'tono', 'tipo-de-movimiento'].includes(d.name))
  .map((d) => d.name)

/** El catálogo es la prueba de que existe cada pieza: una pieza sin muestra no está terminada. */
describe('catálogo de @aportaya/ui', () => {
  it('monta todas las piezas, y cada carpeta tiene su archivo con el mismo nombre', () => {
    for (const pieza of PIEZAS) {
      expect(readdirSync(join(SRC, pieza)), `${pieza} sin ${pieza}.ts`).toContain(`${pieza}.ts`)
    }
    const catalogo = ['catalogo-atomos.ts', 'catalogo-moleculas.ts', 'catalogo-organismos.ts'].map((f) => readFileSync(join(__dirname, f), 'utf8')).join('\n')
    for (const pieza of PIEZAS) {
      if (pieza === 'estado-de-pantalla' || pieza === 'toast' || pieza === 'icono') continue
      expect(catalogo, `${pieza} no está en el catálogo`).toContain(`<ap-${pieza}`)
    }
  })

  it('las piezas de la maqueta (plan 22 §6, columna Angular) existen con ese nombre', () => {
    for (const p of ['selector-segmentado', 'codigo-qr', 'cuenta-enmascarada', 'fecha', 'barra-de-pasos', 'fila-de-cotejo', 'chips-de-filtro', 'resumen-de-periodo', 'fila-de-movimiento', 'estado-vacio', 'riel-de-turnos', 'reloj-de-plazo', 'escalera-de-etapas', 'medidor-de-rango', 'desglose-de-cobro', 'seccion-de-expediente', 'lista-de-requisitos', 'tarjeta-de-solicitud', 'vale', 'tarjeta-de-oferta', 'panel-de-factores', 'verificador-de-sorteo', 'banda-de-proposito', 'estado-de-pantalla']) {
      expect(PIEZAS, `falta ${p}`).toContain(p)
    }
  })

  it('se renderiza con las cuatro reglas de la maqueta a la vista', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(Catalogo)
    fixture.detectChanges()
    await fixture.whenStable()
    const html = fixture.nativeElement as HTMLElement
    // Regla 2: movimientos agrupados por día con neto y saldo.
    expect(html.querySelector('ap-lista-de-movimientos')?.textContent).toContain('Hoy')
    expect(html.querySelector('ap-lista-de-movimientos')?.textContent).toContain('Neto')
    // Regla 3: el elegido del selector segmentado es un radio marcado.
    expect(html.querySelector('ap-selector-segmentado [aria-checked="true"]')?.textContent?.trim()).toBe('Lista')
    // Regla 1: el ícono del movimiento se llama por lo que pasó.
    expect(html.querySelector('ap-fila-de-movimiento ap-icono')?.getAttribute('aria-label')).toBe('Aporte')
  })
})
