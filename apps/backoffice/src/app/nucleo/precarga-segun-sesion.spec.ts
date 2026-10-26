import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, type Route } from '@angular/router'
import { firstValueFrom, of, toArray, defaultIfEmpty, Subject } from 'rxjs'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { requierePermiso, requiereSesion } from './permisos'
import { PrecargaSegunSesion } from './precarga-segun-sesion'
import { Sesion } from './sesion'

/** F2 · se precarga en reposo solo lo que las guardias de la ruta dejarían abrir a esta sesión. */
describe('PrecargaSegunSesion', () => {
  let estrategia: PrecargaSegunSesion
  let sesion: Sesion
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
    estrategia = TestBed.inject(PrecargaSegunSesion)
    sesion = TestBed.inject(Sesion)
  })
  const correr = (route: Route) => {
    const load = vi.fn(() => of('cargado'))
    return { load, valores: firstValueFrom(estrategia.preload(route, load).pipe(toArray(), defaultIfEmpty([]))) }
  }

  it('correcto: una ruta cuya guardia deja pasar se carga (sesión abierta + permiso de sección)', async () => {
    sesion.abrir('t', ['RETIRO_APROBAR'], 'oficial')
    const { load, valores } = correr({ path: 'operacion', canMatch: [requiereSesion(), requierePermiso('ver:operacion')], loadChildren: () => [] })
    expect(await valores).toEqual(['cargado'])
    expect(load).toHaveBeenCalledOnce()
  })

  it('límite: sin guardias se carga; una guardia que no decide todavía (sesión restaurando) la deja esperando sin cargar', async () => {
    sesion.abrir('t', [], 'oficial')
    expect(await correr({ path: 'ayuda', loadChildren: () => [] }).valores).toEqual(['cargado'])
    const pendiente = new Subject<boolean>()
    const { load } = correr({ path: 'x', canMatch: [() => pendiente], loadChildren: () => [] })
    await Promise.resolve()
    expect(load).not.toHaveBeenCalled()
  })

  it('inválido: lo que la guardia corta no se descarga (sin permiso, sin sesión, o una guardia que falla)', async () => {
    sesion.abrir('t', ['RECLAMO_VER'], 'oficial')
    const sinPermiso = correr({ path: 'sistemas', canMatch: [requierePermiso('ver:sistemas')], loadChildren: () => [] })
    expect(await sinPermiso.valores).toEqual([])
    expect(sinPermiso.load).not.toHaveBeenCalled()
    const rota = correr({ path: 'y', canMatch: [() => { throw new Error('guardia rota') }], loadChildren: () => [] })
    expect(await rota.valores).toEqual([])
    sesion.cerrar()
    const sinSesion = correr({ path: '', canMatch: [requiereSesion()], loadChildren: () => [] })
    expect(await sinSesion.valores).toEqual([])
    expect(sinSesion.load).not.toHaveBeenCalled()
  })

  it('límite: de a dos por vez; la tercera arranca cuando llega una de las anteriores', async () => {
    sesion.abrir('t', [], 'oficial')
    const llegadas = [new Subject<string>(), new Subject<string>(), new Subject<string>()]
    const loads = llegadas.map((s) => vi.fn(() => s.asObservable()))
    loads.forEach((load, i) => estrategia.preload({ path: `r${i}`, loadChildren: () => [] }, load).subscribe())
    await vi.waitFor(() => expect(loads[1]).toHaveBeenCalled())
    expect(loads.map((l) => l.mock.calls.length)).toEqual([1, 1, 0])
    llegadas[0]!.next('listo')
    await vi.waitFor(() => expect(loads[2]).toHaveBeenCalled())
  })
})
