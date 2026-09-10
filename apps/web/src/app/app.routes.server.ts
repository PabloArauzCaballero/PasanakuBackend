import { RenderMode, ServerRoute } from '@angular/ssr'

/**
 * **Estático por omisión** (ADR-041). Solo las rutas que dependen de un servicio en vivo
 * van en servidor, y `rutas-de-servidor.spec.ts` enumera cuáles: si mañana hay media
 * docena, la decisión dejó de cumplirse en silencio.
 */
export const serverRoutes: ServerRoute[] = [
  { path: 'plazos', renderMode: RenderMode.Server, headers: { 'Cache-Control': 'no-store' } },
  { path: '**', renderMode: RenderMode.Prerender },
]
