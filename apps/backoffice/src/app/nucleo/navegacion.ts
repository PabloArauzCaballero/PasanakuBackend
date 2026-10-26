import type { Miga } from '@aportaya/ui/migas/migas'
import { MENU, type Credenciales, type Entrada, type Grupo, type Pantalla, type Requisito } from './menu'

/**
 * Las preguntas que el shell le hace al árbol del menú (`menu.ts`): qué ve esta sesión, a dónde lleva cada entrada,
 * a qué sección y pestaña pertenece una URL, y sus migas. Funciones puras: la sesión entra como argumento.
 */

/** ¿La sesión pasa **toda** la cadena? Cada eslabón se cumple con uno de sus permisos o con uno de sus roles. */
export function cumple(requiere: readonly Requisito[] | undefined, c: Credenciales): boolean {
  const rol = c.rol()
  return (requiere ?? []).every((r) => (r.permisos ?? []).some((x) => c.puede(x)) || (rol !== null && rol !== '' && (r.roles ?? []).includes(rol)))
}

/** Las pantallas reales de una pestaña: ella misma, o las de su grupo. */
export function hojas(p: Pantalla): readonly Pantalla[] {
  return p.grupo ?? [p]
}

/** Las pestañas de una entrada que la sesión puede abrir; un grupo queda con las suyas, y sin ninguna, desaparece. */
export function pantallasVisibles(e: Entrada, c: Credenciales): Pantalla[] {
  return (e.pantallas ?? []).flatMap((p) => {
    if (!p.grupo) return cumple(p.requiere, c) ? [p] : []
    const grupo = p.grupo.filter((h) => cumple(h.requiere, c))
    return grupo.length ? [{ ...p, ruta: grupo[0]!.ruta, grupo }] : []
  })
}

/** A dónde lleva la entrada para esta sesión: la primera pantalla que puede abrir. Sin ninguna, la entrada no se dibuja. */
export function destinoDe(e: Entrada, c: Credenciales): string | undefined {
  if (e.pantallas) return pantallasVisibles(e, c)[0]?.ruta
  return cumple(e.requiere, c) ? e.destino : undefined
}

/** Los grupos del menú con las entradas que la sesión puede abrir, cada una con su destino ya resuelto. */
export function entradasVisibles(c: Credenciales, menu: readonly Grupo[] = MENU): (Grupo & { entradas: (Entrada & { destino: string })[] })[] {
  return menu
    .map((g) => ({ ...g, entradas: g.entradas.flatMap((e) => (destinoDe(e, c) ? [{ ...e, destino: destinoDe(e, c)! }] : [])) }))
    .filter((g) => g.entradas.length > 0)
}

const coincide = (url: string, ruta: string) => url === ruta || url.startsWith(ruta + '/')
const sinConsulta = (url: string) => url.split(/[?#]/)[0]!

/** La entrada dueña de una URL: la de prefijo o pestaña más larga que la contiene (`/operacion/custodia` es Contabilidad). */
export function entradaDe(url: string, menu: readonly Grupo[] = MENU): Entrada | undefined {
  const ruta = sinConsulta(url)
  let mejor: { e: Entrada; largo: number } | undefined
  for (const e of menu.flatMap((g) => g.entradas)) {
    const candidatas = [...e.prefijos, ...(e.pantallas ?? []).flatMap((p) => hojas(p).map((h) => h.ruta))]
    for (const r of candidatas) if (coincide(ruta, r) && (!mejor || r.length > mejor.largo)) mejor = { e, largo: r.length }
  }
  return mejor?.e
}

/** La pestaña (y, si es un grupo, la subpestaña) que contiene la URL, por la ruta más larga. */
export function pantallaActiva(e: Entrada, url: string): { pantalla: Pantalla; hoja: Pantalla } | undefined {
  const ruta = sinConsulta(url)
  let mejor: { pantalla: Pantalla; hoja: Pantalla } | undefined
  for (const p of e.pantallas ?? []) for (const h of hojas(p)) if (coincide(ruta, h.ruta) && (!mejor || h.ruta.length > mejor.hoja.ruta.length)) mejor = { pantalla: p, hoja: h }
  return mejor
}

/**
 * Las migas de una URL: Inicio › Sección › [Grupo ›] Pantalla. Una ficha (`/cumplimiento/casos/8f1`) agrega el título
 * de la página. Con `credenciales`, el enlace de la sección va a la primera pestaña que esa sesión puede abrir (el de
 * una pestaña ajena rebotaría); sin ellas, a la primera pestaña del árbol. En el inicio no hay migas.
 */
export function migasDe(url: string, tituloDePagina: string, credenciales?: Credenciales): Miga[] {
  const ruta = sinConsulta(url)
  const entrada = entradaDe(ruta)
  if (!entrada || entrada.ruta === 'tablero') return []
  const titulo = tituloDePagina.replace(/\s*·\s*(Sistemas\s*·\s*)?AportaYa\s*$/, '').trim()
  const destino = (credenciales ? destinoDe(entrada, credenciales) : undefined) ?? entrada.pantallas?.[0]?.ruta ?? entrada.destino ?? entrada.prefijos[0]!
  const migas: Miga[] = [{ texto: 'Inicio', ruta: '/tablero' }, { texto: entrada.texto, ruta: destino }]
  const activa = pantallaActiva(entrada, ruta)
  if (!activa) return [...migas, { texto: titulo || entrada.texto }]
  if (activa.pantalla.grupo) migas.push({ texto: activa.pantalla.texto, ruta: activa.pantalla.grupo[0]!.ruta })
  if (ruta === activa.hoja.ruta) return [...migas, { texto: activa.hoja.texto }]
  return [...migas, { texto: activa.hoja.texto, ruta: activa.hoja.ruta }, { texto: titulo || activa.hoja.texto }]
}
