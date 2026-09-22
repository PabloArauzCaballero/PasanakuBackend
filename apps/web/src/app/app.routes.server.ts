import { RenderMode, ServerRoute } from '@angular/ssr'

/**
 * **Estático por omisión** (ADR-041). Solo las rutas que dependen de un servicio en
 * vivo van en servidor, y `rutas-de-servidor.spec.ts` enumera cuáles.
 *
 * `planes/14` F9.3 pide seis rutas `Server`: `verificar/:codigo`,
 * `publico/grupos/:codigo`, `publico/sorteos/:id`, `tarifas`,
 * `contrato-de-adhesion` y `legal/estado-regulatorio`. **Acá van cinco**, y la
 * diferencia está documentada (también en `planes/informes/carril-W.md`):
 *
 * - `contrato-de-adhesion` y `legal/estado-regulatorio` NO tienen hoy un contrato
 *   público de lectura en `servicios/cumplimiento` (se buscó en
 *   `openapi/cumplimiento.yaml`: no existe un `GET` de `documento_publicado` ni de
 *   `licencia_regulatoria` sin sesión). Publicarlas como `Server` contra un
 *   endpoint que no existe habría significado inventar la respuesta — se prefirió
 *   `Prerender` con contenido versionado y la nota del hueco dentro de cada página.
 * - En cambio se agrega `plazos` (CU-59), que **ya** estaba servida por F0-W antes
 *   de este carril y depende de un servicio en vivo (`/grupos/calendario/calcular`):
 *   quitarla de `Server` la habría roto.
 *
 * Con esos dos cambios, el conteo real es cinco, no seis — no por descuido, por un
 * contrato de backend que todavía no existe.
 */
export const serverRoutes: ServerRoute[] = [
  { path: 'plazos', renderMode: RenderMode.Server, headers: { 'Cache-Control': 'no-store' } },
  { path: 'tarifas', renderMode: RenderMode.Server, headers: { 'Cache-Control': 'public, s-maxage=600' } },
  { path: 'verificar/:codigo', renderMode: RenderMode.Server, headers: { 'Cache-Control': 'no-store', 'X-Robots-Tag': 'noindex, nofollow' } },
  { path: 'publico/grupos/:codigo', renderMode: RenderMode.Server, headers: { 'Cache-Control': 'no-store', 'X-Robots-Tag': 'noindex, nofollow' } },
  { path: 'publico/sorteos/:id', renderMode: RenderMode.Server, headers: { 'Cache-Control': 'no-store', 'X-Robots-Tag': 'noindex, nofollow' } },
  { path: '**', renderMode: RenderMode.Prerender },
]
