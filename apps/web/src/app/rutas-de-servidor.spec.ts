import { RenderMode } from '@angular/ssr'
import { describe, expect, it } from 'vitest'
import { serverRoutes } from './app.routes.server'

/**
 * **Estático por omisión** (ADR-041): SSR se declara ruta por ruta, y esta prueba
 * enumera cuáles. Crece solo si alguien lo cambia a propósito.
 *
 * `planes/14` F9.3 pide seis rutas `Server`. Acá son cinco: `contrato-de-adhesion` y
 * `legal/estado-regulatorio` no tienen hoy un contrato público de lectura en
 * `servicios/cumplimiento` (ver la nota en `app.routes.server.ts` y el informe del
 * carril W), así que se sirven `Prerender` en vez de inventar una respuesta del
 * backend. Se agrega `plazos` (CU-59, ya scaffoldeada por F0-W) porque depende de un
 * servicio en vivo igual que las demás.
 */
describe('rutas de servidor', () => {
  it('cinco rutas van en servidor, documentado por qué no son las seis del plan; el resto se prerenderiza', () => {
    const servidor = serverRoutes.filter((r) => r.renderMode === RenderMode.Server).map((r) => r.path)
    expect(servidor).toEqual(['plazos', 'tarifas', 'verificar/:codigo', 'publico/grupos/:codigo', 'publico/sorteos/:id'])
    expect(serverRoutes.at(-1)).toMatchObject({ path: '**', renderMode: RenderMode.Prerender })
  })

  it('las rutas publicas de verificacion van noindex por X-Robots-Tag ademas de la meta', () => {
    for (const ruta of ['verificar/:codigo', 'publico/grupos/:codigo', 'publico/sorteos/:id']) {
      const r = serverRoutes.find((r) => r.path === ruta)
      expect(r?.headers?.['X-Robots-Tag'], `${ruta} sin X-Robots-Tag`).toBe('noindex, nofollow')
    }
  })

  it('ninguna ruta de servidor deja cachear datos vivos sin decirlo', () => {
    for (const r of serverRoutes.filter((r) => r.renderMode === RenderMode.Server)) {
      expect(r.headers?.['Cache-Control'], `${r.path} sin Cache-Control`).toBeDefined()
    }
  })
})
