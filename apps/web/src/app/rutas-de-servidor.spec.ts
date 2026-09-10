import { RenderMode } from '@angular/ssr'
import { describe, expect, it } from 'vitest'
import { serverRoutes } from './app.routes.server'

/**
 * **Estático por omisión** (ADR-041): SSR se declara ruta por ruta, y esta prueba
 * enumera cuáles. Crece solo si alguien lo cambia a propósito.
 */
describe('rutas de servidor', () => {
  it('hoy exactamente una ruta va en servidor; el resto se prerenderiza', () => {
    const servidor = serverRoutes.filter((r) => r.renderMode === RenderMode.Server).map((r) => r.path)
    expect(servidor).toEqual(['plazos'])
    expect(serverRoutes.at(-1)).toMatchObject({ path: '**', renderMode: RenderMode.Prerender })
  })

  it('ninguna ruta de servidor deja cachear datos vivos sin decirlo', () => {
    for (const r of serverRoutes.filter((r) => r.renderMode === RenderMode.Server)) {
      expect(r.headers?.['Cache-Control'], `${r.path} sin Cache-Control`).toBeDefined()
    }
  })
})
