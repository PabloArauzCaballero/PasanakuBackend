import { describe, expect, it } from 'vitest'
import { ContenedorDeActa } from '../actas/contenedor-de-acta'
import { ContenedorDeAlerta } from '../alertas/contenedor-de-alerta'
import { ContenedorDeReclamo } from '../reclamos/contenedor-de-reclamo'
import { montar, textoDe } from './pruebas.apoyo'

/** Plan B4: ningún contenedor dibuja datos de ejemplo como reales; sin contrato, se dice. */
describe('contenedores sin contrato · indisponibilidad honesta', () => {
  it('CU-97: la alerta de riesgo temprano no inventa hechos ni un grupo de ejemplo, y lleva a la bandeja real', () => {
    const { fixture, http } = montar(ContenedorDeAlerta, [], 'OPERADOR', { alertaId: 'x' })
    const texto = textoDe(fixture)
    expect(texto).toContain('todavía no está disponible')
    expect(texto).not.toContain('Aporte tardío')
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/cumplimiento/alertas')
    http.verify()
  })

  it('CU-94: el acta no arma asistentes de ejemplo y lleva a la lista de actas', () => {
    const { fixture, http } = montar(ContenedorDeActa, [], 'OPERADOR', { actaId: 'x' })
    expect(textoDe(fixture)).toContain('solo la lista de actas')
    expect(textoDe(fixture)).not.toContain('Cerrar acta')
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/cumplimiento/actas')
    http.verify()
  })

  it('CU-52: el reclamo no muestra un plazo inventado ni un botón que no envía; lleva a Operación', () => {
    const { fixture, http } = montar(ContenedorDeReclamo, [], 'OPERADOR', { reclamoId: 'x' })
    expect(textoDe(fixture)).not.toContain('Enviar respuesta')
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/operacion/reclamos')
    http.verify()
  })
})
