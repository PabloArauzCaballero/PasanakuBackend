import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { Invitacion } from './invitacion'
import { SecretoDeInvitacionEnMemoria } from './token-de-invitacion'

const TOKEN = 'b2'.repeat(32) // 64 hex, sintético

describe('Invitacion · enlace de invitación (H12.S1.M4 · el secreto sale de la URL)', () => {
  beforeEach(() => {
    window.localStorage.clear()
    window.sessionStorage.clear()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
  })

  async function abrir(urlConSecreto: string): Promise<ComponentFixture<Invitacion>> {
    window.history.replaceState(null, '', urlConSecreto)
    const f = TestBed.createComponent(Invitacion)
    f.detectChanges()
    await f.whenStable()
    f.detectChanges()
    return f
  }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

  it('con secreto válido: lo retira de la URL, lo guarda solo en memoria y aclara que no entra al grupo', async () => {
    const f = await abrir(`/invitacion/g1/i1#t=${TOKEN}`)
    expect(window.location.href).not.toContain(TOKEN)
    expect(window.location.hash).toBe('')
    expect(TestBed.inject(SecretoDeInvitacionEnMemoria).hay()).toBe(true)
    expect(texto(f)).toContain('Tu invitación está lista')
    expect(texto(f)).toContain('no entrás automáticamente')
    expect(texto(f)).not.toContain(TOKEN)
  })

  it('el secreto no queda en almacenamiento accesible por script ni en cookies', async () => {
    await abrir(`/invitacion/g1/i1#t=${TOKEN}`)
    expect(window.localStorage.length).toBe(0)
    expect(window.sessionStorage.length).toBe(0)
    expect(document.cookie).not.toContain(TOKEN)
  })

  it('con el secreto en la consulta: lo retira igual y avisa que viajó por un lugar poco seguro', async () => {
    const f = await abrir(`/invitacion/g1/i1?token=${TOKEN}`)
    expect(window.location.href).not.toContain(TOKEN)
    expect(window.location.search).toBe('')
    expect(texto(f)).toContain('poco seguro')
  })

  it('con un código inválido: lo retira, no guarda nada y lo dice como alerta', async () => {
    const f = await abrir('/invitacion/g1/i1#t=esto-no-es-un-secreto')
    expect(window.location.hash).toBe('')
    expect(TestBed.inject(SecretoDeInvitacionEnMemoria).hay()).toBe(false)
    expect((f.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain('no tiene un código de invitación válido')
  })

  it('sin código: lo dice como alerta y no toca la URL', async () => {
    const f = await abrir('/invitacion/g1/i1')
    expect(window.location.pathname).toBe('/invitacion/g1/i1')
    expect((f.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain('no trae un código')
  })
})
