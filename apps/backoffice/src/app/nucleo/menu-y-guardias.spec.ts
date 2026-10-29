import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { Router, provideRouter, type Routes } from '@angular/router'
import { beforeAll, beforeEach, describe, expect, it } from 'vitest'
import { routes } from '../app.routes'
import { GATEWAY } from './gateway'
import { MENU } from './menu'
import { cumple, entradasVisibles, hojas } from './navegacion'
import { PERMISOS_POR_SECCION } from './secciones'
import { Sesion } from './sesion'

/**
 * **El menú no promete lo que la ruta no cumple (F1).** Con el router y las rutas reales —las mismas guardias que corren
 * en el navegador— y una matriz de sesiones, cada pestaña que el menú muestra tiene que abrir sin rebote, y cada una que
 * oculta tiene que rebotar. Así `menu.ts` (que copia las guardias como dato) no puede divergir de los `*.routes.ts`.
 * Sin componente raíz con `router-outlet`: se navega sin montar pantallas, solo se resuelven rutas y guardias.
 */
const pantallas = MENU.flatMap((g) => g.entradas).flatMap((e) => (e.pantallas ?? []).flatMap(hojas))
const codigos = [
  ...new Set([
    ...pantallas.flatMap((h) => (h.requiere ?? []).flatMap((r) => [...(r.permisos ?? []), ...(r.roles ?? [])])),
    ...Object.values(PERMISOS_POR_SECCION).flat(),
  ]),
].filter((c) => !c.startsWith('ver:'))

type Caso = { nombre: string; permisos: string[]; rol: string }
const casos: Caso[] = [
  { nombre: 'todos · PLATAFORMA', permisos: codigos, rol: 'PLATAFORMA' },
  { nombre: 'todos · oficial', permisos: codigos, rol: 'oficial' },
  { nombre: 'ninguno', permisos: [], rol: 'oficial' },
  // Cada código solo, como permiso de un rol cualquiera y como el rol mismo (el token trae los dos).
  ...codigos.flatMap((c) => [
    { nombre: `${c} · oficial`, permisos: [c], rol: 'oficial' },
    { nombre: `${c} · rol ${c}`, permisos: [c], rol: c },
  ]),
  { nombre: 'ACCESOS_ADMINISTRAR · SEGURIDAD', permisos: ['ACCESOS_ADMINISTRAR'], rol: 'SEGURIDAD' },
]

/**
 * Carga una vez, antes de la matriz, cada pantalla perezosa (`loadComponent`) y cada rama (`loadChildren`), en
 * profundidad. Sin esto el primer caso pagaba el `import()` en frío de todas las rutas —4270 ms solo, más de 5 s dentro
 * de la suite completa— y agotaba el plazo; los demás las encontraban en la caché de módulos (≈300 ms). No cambia qué
 * se navega ni qué se afirma: saca el costo de carga del caso que mide.
 */
async function precargar(rutas: Routes): Promise<void> {
  for (const r of rutas) {
    if (r.loadComponent) await r.loadComponent()
    if (r.children) await precargar(r.children)
    if (r.loadChildren) {
      const hijas = await r.loadChildren()
      if (Array.isArray(hijas)) await precargar(hijas)
    }
  }
}

/** Plazo propio de la carga única: crece con cada pantalla nueva y no tiene por qué competir con el de un caso. */
const PLAZO_DE_PRECARGA_MS = 60_000

const enRuta = (url: string, ruta: string) => {
  const u = url.split(/[?#]/)[0]!
  return u === ruta || u.startsWith(ruta + '/')
}

describe('menú ⇔ guardias de las rutas', () => {
  let router: Router
  let sesion: Sesion
  beforeAll(() => precargar(routes), PLAZO_DE_PRECARGA_MS)
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: '/api/v1' }],
    })
    router = TestBed.inject(Router)
    sesion = TestBed.inject(Sesion)
  })

  it(`hay ${pantallas.length} pantallas en el menú y ${casos.length} sesiones en la matriz`, () => {
    expect(pantallas.length).toBeGreaterThan(30)
    expect(casos.length).toBeGreaterThan(40)
  })

  it.each(casos)('$nombre: lo visible abre sin rebote y lo oculto rebota', async ({ permisos, rol }) => {
    sesion.abrir('token', permisos, rol)
    const visibles = new Set(entradasVisibles(sesion).flatMap((g) => g.entradas).map((e) => e.destino))
    const errores: string[] = []
    for (const h of pantallas) {
      await router.navigateByUrl('/tablero')
      await router.navigateByUrl(h.ruta)
      const abre = enRuta(router.url, h.ruta)
      const muestra = cumple(h.requiere, sesion)
      if (abre !== muestra) errores.push(`${h.ruta}: menú ${muestra ? 'muestra' : 'oculta'}, la ruta ${abre ? 'abre' : `rebota a ${router.url}`}`)
    }
    // Y el destino de cada entrada visible abre ahí mismo (el clic del menú no rebota).
    for (const destino of visibles) {
      await router.navigateByUrl('/tablero')
      await router.navigateByUrl(destino)
      if (!enRuta(router.url, destino)) errores.push(`entrada → ${destino} rebota a ${router.url}`)
    }
    expect(errores).toEqual([])
  })
})
