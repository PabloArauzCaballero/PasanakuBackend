import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { afterEach, describe, expect, it } from 'vitest'
import { BannerDatosDeEjemplo } from '../banner-datos-de-ejemplo/banner-datos-de-ejemplo'
import { PantallaDespliegues } from '../despliegues/pantalla-despliegues'
import { detectarModoFuentesDeSistemas, provideFuentesDeSistemas } from './proveedor-fuentes'

/**
 * H1.S4.M5 (carril B · L4) — los datos de ejemplo de `sistemas/` solo existen en desarrollo y demo; en producción la
 * pantalla dice «no disponible» y no hay banner de ejemplo porque no hay ejemplo. Completa a `proveedor-fuentes*.spec.ts`
 * y `sin-simulado-en-pantallas.spec.ts` con lo que no cubrían: la configuración de build y la pantalla montada.
 */
type Configuraciones = Record<string, { fileReplacements?: { replace: string; with: string }[] }>
const ANGULAR_JSON = join(dirname(fileURLToPath(import.meta.url)), '../../../../../angular.json')
const build = JSON.parse(readFileSync(ANGULAR_JSON, 'utf-8')).projects.backoffice.architect.build as { defaultConfiguration: string; configurations: Configuraciones }
const reemplazaProveedor = (nombre: string) =>
  (build.configurations[nombre]?.fileReplacements ?? []).some((r) => r.replace.endsWith('rutas/sistemas/dominio/proveedor-fuentes.ts') && r.with.endsWith('proveedor-fuentes.demo.ts'))

describe('build · el proveedor con datos de ejemplo no entra a producción', () => {
  it('correcto: `production` es la configuración por omisión y no reemplaza ningún archivo', () => {
    expect(build.defaultConfiguration).toBe('production')
    expect(build.configurations['production']?.fileReplacements ?? []).toEqual([])
  })

  it('límite: solo `development` y `demo` cambian el proveedor seguro por el de ejemplo', () => {
    const conEjemplo = Object.keys(build.configurations).filter(reemplazaProveedor).sort()
    expect(conEjemplo).toEqual(['demo', 'development'])
  })
})

describe('detectarModoFuentesDeSistemas · solo la etiqueta exacta enciende el modo demo', () => {
  afterEach(() => document.querySelectorAll('meta[name="aportaya-demo"]').forEach((m) => m.remove()))
  const conEtiqueta = (valor: string) => {
    const meta = document.createElement('meta')
    meta.name = 'aportaya-demo'
    meta.content = valor
    document.head.appendChild(meta)
  }

  it('correcto: sin etiqueta es producción', () => expect(detectarModoFuentesDeSistemas()).toBe('produccion'))
  it('límite: «true» exacto es demo', () => {
    conEtiqueta('true')
    expect(detectarModoFuentesDeSistemas()).toBe('demo')
  })
  it.each(['TRUE', '1', 'si', ''])('inválido: «%s» no enciende el modo demo', (valor) => {
    conEtiqueta(valor)
    expect(detectarModoFuentesDeSistemas()).toBe('produccion')
  })
})

// Despliegues todavía no tiene contrato real: es la que ejercita la indisponibilidad honesta. Servicios ya lee de
// auditoria (`salud-de-servicios.spec.ts`).
@Component({ imports: [BannerDatosDeEjemplo, PantallaDespliegues], template: `<ap-banner-datos-de-ejemplo /><ap-pantalla-despliegues />` })
class SeccionComoEnProduccion {}

describe('pantalla de sistemas con el proveedor de producción · indisponibilidad honesta', () => {
  it('aun con la etiqueta demo encendida: sin banner de ejemplo, con el estado «todavía no hay datos para mostrar» y sin cifras de ejemplo', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), ...provideFuentesDeSistemas('demo')] })
    const fixture = TestBed.createComponent(SeccionComoEnProduccion)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    const raiz = fixture.nativeElement as HTMLElement
    expect(raiz.textContent).not.toContain('Datos de ejemplo.')
    expect(raiz.querySelector('ap-estado-vacio')?.textContent).toContain('todavía no hay datos para mostrar')
    expect(raiz.textContent).not.toMatch(/\d+(\.\d+)?%/)
  })
})
