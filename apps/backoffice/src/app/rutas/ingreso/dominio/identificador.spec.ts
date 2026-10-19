import { describe, expect, it } from 'vitest'
import { leerIdentificador } from './identificador'

describe('leerIdentificador · correo o teléfono en un solo campo', () => {
  it('con arroba es un correo, sin espacios de los bordes', () => {
    expect(leerIdentificador('  ana.operadora@aportaya.bo ')).toEqual({ ok: true, valor: { correo: 'ana.operadora@aportaya.bo' } })
  })

  it('8 dígitos son un teléfono boliviano', () => {
    expect(leerIdentificador('71234567')).toEqual({ ok: true, valor: { telefonoE164: '+59171234567' } })
  })

  it('acepta el teléfono con +591, espacios o guiones', () => {
    expect(leerIdentificador('+591 712-34567')).toEqual({ ok: true, valor: { telefonoE164: '+59171234567' } })
  })

  it('vacío pide el dato sin decir cuál falta', () => {
    expect(leerIdentificador('   ')).toEqual({ ok: false, error: 'Escribí tu correo o tu teléfono.' })
  })

  it('un correo incompleto no se manda', () => {
    expect(leerIdentificador('ana@aportaya').ok).toBe(false)
    expect(leerIdentificador('ana @aportaya.bo').ok).toBe(false)
  })

  it('un teléfono que no tiene 8 dígitos no se manda', () => {
    expect(leerIdentificador('7123456')).toEqual({ ok: false, error: 'El teléfono son 8 dígitos, sin el +591.' })
    expect(leerIdentificador('712345678')).toEqual({ ok: false, error: 'El teléfono son 8 dígitos, sin el +591.' })
  })
})
