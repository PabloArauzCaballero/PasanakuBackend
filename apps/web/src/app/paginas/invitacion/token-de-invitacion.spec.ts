import { describe, expect, it } from 'vitest'
import { leerEnlace, retirarSecretoDeLaUrl, SecretoDeInvitacionEnMemoria } from './token-de-invitacion'

const TOKEN = 'a1'.repeat(32) // 64 hex, sintético

describe('token de invitación · lectura del enlace', () => {
  it('lee el secreto del fragmento (#t=…) y no lo marca como expuesto', () => {
    expect(leerEnlace(`#t=${TOKEN}`, '')).toEqual({ estado: 'listo', token: TOKEN, enConsulta: false })
  })
  it('acepta mayúsculas y espacios alrededor, y lo normaliza', () => {
    expect(leerEnlace(`#t=${TOKEN.toUpperCase()}`, '')).toMatchObject({ estado: 'listo', token: TOKEN })
  })
  it('un secreto en la consulta (?token=) se acepta pero se informa como expuesto', () => {
    expect(leerEnlace('', `?token=${TOKEN}`)).toEqual({ estado: 'listo', token: TOKEN, enConsulta: true })
  })
  it('sin secreto: sin-token', () => {
    expect(leerEnlace('', '')).toEqual({ estado: 'sin-token' })
    expect(leerEnlace('#otra=cosa', '?x=1')).toEqual({ estado: 'sin-token' })
  })
  it.each(['', 'abc', 'g'.repeat(64), 'a'.repeat(63), 'a'.repeat(65), '<script>alert(1)</script>'])('un valor sin forma de secreto (%j) es inválido', (valor) => {
    expect(leerEnlace(`#t=${encodeURIComponent(valor)}`, '').estado).toBe('invalido')
  })
})

describe('retirar el secreto de la URL', () => {
  it('deja solo la ruta, sin fragmento ni consulta, y reemplaza la entrada del historial en vez de agregar una', () => {
    window.history.replaceState(null, '', `/invitacion/g/i?token=${TOKEN}#t=${TOKEN}`)
    const antes = window.history.length
    retirarSecretoDeLaUrl(window)
    expect(window.location.pathname).toBe('/invitacion/g/i')
    expect(window.location.search).toBe('')
    expect(window.location.hash).toBe('')
    expect(window.location.href).not.toContain(TOKEN)
    expect(window.history.length).toBe(antes)
  })
})

describe('secreto en memoria', () => {
  it('se entrega una sola vez y después ya no está', () => {
    const memoria = new SecretoDeInvitacionEnMemoria()
    memoria.guardar(TOKEN)
    expect(memoria.hay()).toBe(true)
    expect(memoria.tomar()).toBe(TOKEN)
    expect(memoria.tomar()).toBeNull()
    expect(memoria.hay()).toBe(false)
  })
})
