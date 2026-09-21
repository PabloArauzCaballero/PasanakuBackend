import type { TutorialDefinicion } from './tipos'

/**
 * **El catálogo se valida, no se confía.** Un tutorial mal escrito no se descubre
 * cuando alguien lo abre: se descubre acá, y la prueba `catalogo.spec.ts` falla el
 * build. Es texto puro, sin DOM y sin Angular, para poder correrlo también en un
 * script.
 */
export type CodigoDeProblema =
  | 'id-duplicado'
  | 'tutorial-vacio'
  | 'paso-duplicado'
  | 'paso-sin-objetivo'
  | 'orden-incorrecto'
  | 'ruta-inexistente'
  | 'requisito-inexistente'
  | 'siguiente-inexistente'
  | 'ciclo'
  | 'permiso-incompatible'

export interface ProblemaDeCatalogo {
  readonly codigo: CodigoDeProblema
  readonly tutorialId: string
  readonly detalle: string
}

/** Las rutas que el backoffice sabe montar. Se le pasan para no adivinarlas. */
export type RutasConocidas = readonly string[]

/**
 * Revisa el catálogo entero y devuelve TODO lo que está mal, no lo primero.
 * Una lista vacía es un catálogo sano.
 */
export function validarCatalogo(tutoriales: readonly TutorialDefinicion[], rutas: RutasConocidas = []): ProblemaDeCatalogo[] {
  const problemas: ProblemaDeCatalogo[] = []
  const vistos = new Set<string>()
  for (const t of tutoriales) {
    if (vistos.has(t.id)) problemas.push({ codigo: 'id-duplicado', tutorialId: t.id, detalle: `el id "${t.id}" está declarado dos veces` })
    vistos.add(t.id)
    problemas.push(...validarUno(t, rutas))
  }
  problemas.push(...validarReferencias(tutoriales, vistos))
  problemas.push(...validarCiclos(tutoriales))
  return problemas
}

function validarUno(t: TutorialDefinicion, rutas: RutasConocidas): ProblemaDeCatalogo[] {
  const problemas: ProblemaDeCatalogo[] = []
  const en = (codigo: CodigoDeProblema, detalle: string) => problemas.push({ codigo, tutorialId: t.id, detalle })
  if (t.pasos.length === 0) en('tutorial-vacio', 'un tutorial sin pasos no enseña nada')
  if (t.ruta !== undefined && !conoce(rutas, t.ruta)) en('ruta-inexistente', `la ruta "${t.ruta}" no existe en el backoffice`)

  const idsDePaso = new Set<string>()
  let ordenAnterior = -Infinity
  for (const p of t.pasos) {
    if (idsDePaso.has(p.id)) en('paso-duplicado', `el paso "${p.id}" está dos veces`)
    idsDePaso.add(p.id)
    if (p.objetivo === undefined && p.accion?.tipo !== 'navegar') en('paso-sin-objetivo', `el paso "${p.id}" no resalta nada ni pide navegar`)
    if (p.ruta !== undefined && !conoce(rutas, p.ruta)) en('ruta-inexistente', `el paso "${p.id}" apunta a "${p.ruta}", que no existe`)
    if (p.orden !== undefined) {
      if (p.orden <= ordenAnterior) en('orden-incorrecto', `el paso "${p.id}" declara orden ${p.orden} después de ${ordenAnterior}`)
      ordenAnterior = p.orden
    }
    if (p.permiso !== undefined && t.permisos !== undefined && t.permisos.length > 0 && !t.permisos.includes(p.permiso)) {
      en('permiso-incompatible', `el paso "${p.id}" pide "${p.permiso}", que el tutorial no exige: nadie que lo vea podrá hacerlo`)
    }
  }
  return problemas
}

function validarReferencias(tutoriales: readonly TutorialDefinicion[], ids: ReadonlySet<string>): ProblemaDeCatalogo[] {
  const problemas: ProblemaDeCatalogo[] = []
  for (const t of tutoriales) {
    for (const r of t.requisitos ?? []) {
      if (!ids.has(r)) problemas.push({ codigo: 'requisito-inexistente', tutorialId: t.id, detalle: `el requisito "${r}" no existe` })
    }
    if (t.siguiente !== undefined && !ids.has(t.siguiente)) {
      problemas.push({ codigo: 'siguiente-inexistente', tutorialId: t.id, detalle: `el siguiente "${t.siguiente}" no existe` })
    }
  }
  return problemas
}

/**
 * Ciclos por requisitos: `A` exige `B` y `B` exige `A` deja a las dos tarjetas
 * diciendo «antes hacé la otra» para siempre. Recorrido en profundidad con marca de
 * camino; el primer regreso a un nodo del camino es el ciclo.
 */
function validarCiclos(tutoriales: readonly TutorialDefinicion[]): ProblemaDeCatalogo[] {
  const porId = new Map(tutoriales.map((t) => [t.id, t]))
  const problemas: ProblemaDeCatalogo[] = []
  const resueltos = new Set<string>()
  const camino = new Set<string>()

  const recorrer = (id: string): void => {
    if (resueltos.has(id)) return
    if (camino.has(id)) {
      problemas.push({ codigo: 'ciclo', tutorialId: id, detalle: `"${id}" es requisito de sí mismo a través de ${[...camino].join(' → ')}` })
      return
    }
    camino.add(id)
    for (const r of porId.get(id)?.requisitos ?? []) if (porId.has(r)) recorrer(r)
    camino.delete(id)
    resueltos.add(id)
  }

  for (const t of tutoriales) recorrer(t.id)
  return problemas
}

/** Una ruta conocida vale como prefijo: `/operacion/billetera` la cubre `/operacion`. */
function conoce(rutas: RutasConocidas, ruta: string): boolean {
  if (rutas.length === 0) return true
  const limpia = ruta.split('?')[0] ?? ruta
  return rutas.some((r) => limpia === r || limpia.startsWith(`${r}/`))
}
