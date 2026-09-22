import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { ATRIBUTO, LocalizadorDeObjetivo } from '@aportaya/tutoriales/objetivo'

function montar(): LocalizadorDeObjetivo {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  return TestBed.inject(LocalizadorDeObjetivo)
}

const sembrar = (id: string): HTMLElement => {
  const div = document.createElement('div')
  div.setAttribute(ATRIBUTO, id)
  document.body.appendChild(div)
  return div
}

describe('LocalizadorDeObjetivo', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('encuentra un elemento que ya está', () => {
    const esperado = sembrar('boton-crear')
    expect(montar().buscar('boton-crear')).toBe(esperado)
  })

  it('no confunde un id con otro que lo contiene', () => {
    sembrar('boton-crear-campana')
    expect(montar().buscar('boton-crear')).toBeNull()
  })

  it('espera a un elemento que llega después, como el que aparece tras una petición', async () => {
    const localizador = montar()
    const promesa = localizador.esperar('fila-tardia', 1000)
    setTimeout(() => sembrar('fila-tardia'), 10)
    expect(await promesa).not.toBeNull()
  })

  it('un objetivo que nunca aparece devuelve null y NO lanza: el tutorial decide qué hacer', async () => {
    const localizador = montar()
    await expect(localizador.esperar('nunca-existe', 20)).resolves.toBeNull()
  })

  it('un elemento sin tamaño no tiene recuadro: no se resalta lo que no se ve', () => {
    const localizador = montar()
    expect(localizador.recuadroDe(sembrar('invisible'))).toBeNull()
  })
})
