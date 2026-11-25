import { describe, expect, it } from 'vitest'
import { axe } from 'vitest-axe'
import { estabilizar, pulsar } from '../compartido/pruebas.apoyo'
import { montarAccesos, responderPagina } from './pantalla-accesos.pruebas'

describe('Revisión de accesos · accesibilidad', () => {
  it('el informe y la confirmación de revocación cumplen axe', async () => {
    const { http, fixture } = montarAccesos()
    responderPagina(http, 'VIVOS')
    const vista = await estabilizar(fixture)
    expect(await axe(vista)).toHaveNoViolations()
    pulsar(vista, 'Revocar AUDITOR de Persona de prueba')
    await estabilizar(fixture)
    expect(await axe(vista)).toHaveNoViolations()
    http.verify()
  })
})
