import { nombreSegunCantidad, singularDe } from './singular'

describe('singularDe · el nombre de un registro en singular', () => {
  it('aceptado: los regulares pierden la «s» final', () => {
    expect(singularDe('alertas')).toBe('alerta')
    expect(singularDe('anunciantes')).toBe('anunciante')
    expect(singularDe('mensajes')).toBe('mensaje')
  })

  it('límite: los irregulares recuperan su forma, tilde incluida', () => {
    expect(singularDe('conciliaciones')).toBe('conciliación')
    expect(singularDe('órdenes')).toBe('orden')
    expect(singularDe('roles')).toBe('rol')
    expect(singularDe('eventos (aprox.)')).toBe('evento (aprox.)')
  })

  it('inválido: un nombre que no termina en «s» queda igual', () => {
    expect(singularDe('personal')).toBe('personal')
    expect(singularDe('')).toBe('')
  })

  it('nombreSegunCantidad: singular solo con uno; cero y varios van en plural', () => {
    expect(nombreSegunCantidad(1, 'piezas')).toBe('pieza')
    expect(nombreSegunCantidad(0, 'piezas')).toBe('piezas')
    expect(nombreSegunCantidad(2, 'piezas')).toBe('piezas')
  })
})
