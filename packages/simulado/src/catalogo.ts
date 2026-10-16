import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'

/**
 * El catálogo de errores de negocio que el simulado puede devolver, derivado de las
 * mismas fuentes que lee el equipo de backend y nunca escrito a mano aquí:
 *
 * - los comentarios `# AP-CUnn-nn NOMBRE` junto a cada operación del contrato OpenAPI
 *   (el contrato declara allí qué reglas puede rechazar esa operación), y
 * - las tablas `NOMBRE: 'AP-CUnn-nn'` de `docs/CasosDeUso/*.md`.
 *
 * Un ejemplo de rechazo cuyo código no está en este catálogo es un error inventado: la
 * pantalla se prueba contra algo que el backend no dice.
 */
export type EntradaDelCatalogo = { codigo: string; nombre?: string }

export type Catalogo = {
  /** Todos los códigos conocidos → nombre de la regla. */
  global: Map<string, string | undefined>
  /** `servicio/operationId` → códigos que el contrato menciona junto a esa operación. */
  porOperacion: Map<string, EntradaDelCatalogo[]>
  /** `servicio` → códigos que su contrato menciona en cualquier parte (nivel servicio, menos preciso). */
  porServicio: Map<string, EntradaDelCatalogo[]>
}

const CODIGO = /AP-CU\d+-\d{2}/g
const METODOS = ['get', 'post', 'put', 'patch', 'delete']

function codigosDeLinea(linea: string): EntradaDelCatalogo[] {
  const salida: EntradaDelCatalogo[] = []
  const rx = /(AP-CU\d+-\d{2})\s+([A-Z][A-Z0-9_]{2,})?/g
  for (const m of linea.matchAll(rx)) salida.push({ codigo: m[1] ?? m[0], nombre: m[2] })
  return salida
}

/**
 * Asigna a cada `(ruta, método)` los códigos que su contrato menciona. Los comentarios
 * de indentación ≤ 2 que preceden a una ruta pertenecen a esa ruta; los demás, a la
 * operación en curso.
 */
export function codigosPorRutaYMetodo(yaml: string): Map<string, EntradaDelCatalogo[]> {
  const salida = new Map<string, EntradaDelCatalogo[]>()
  let enPaths = false
  let ruta: string | null = null
  let metodo: string | null = null
  let previos: EntradaDelCatalogo[] = []
  const agregar = (clave: string, entradas: EntradaDelCatalogo[]) => {
    if (entradas.length === 0) return
    salida.set(clave, [...(salida.get(clave) ?? []), ...entradas])
  }
  for (const linea of yaml.split(/\r?\n/)) {
    if (/^paths:/.test(linea)) { enPaths = true; continue }
    if (enPaths && /^\S/.test(linea) && !linea.startsWith('#')) { enPaths = false; ruta = null; metodo = null }
    if (!enPaths) continue
    const indent = linea.length - linea.trimStart().length
    const texto = linea.trim()
    if (texto.startsWith('#')) {
      const codigos = codigosDeLinea(texto)
      if (indent <= 2 && codigos.length > 0) previos.push(...codigos)
      else if (ruta && metodo) agregar(`${ruta} ${metodo}`, codigos)
      continue
    }
    const r = /^ {2}(\/\S*):\s*$/.exec(linea)
    if (r) {
      ruta = r[1] ?? null
      metodo = null
      // los comentarios previos valen para todos los métodos de la ruta
      for (const m of METODOS) agregar(`${ruta} ${m}`, previos)
      previos = []
      continue
    }
    const m = /^ {4}(get|post|put|patch|delete):\s*$/.exec(linea)
    if (m) { metodo = m[1] ?? null; continue }
    if (ruta && metodo && texto.includes('AP-CU')) agregar(`${ruta} ${metodo}`, codigosDeLinea(texto))
  }
  return salida
}

/** `contratos` es la carpeta que contiene `servicios/`; `docs` la que contiene `docs/CasosDeUso`. */
export function construirCatalogo(
  contratos: { servicio: string; yaml: string; documento: { paths: Record<string, Record<string, { operationId?: string }>> } }[],
  carpetaDeCasosDeUso?: string,
): Catalogo {
  const global = new Map<string, string | undefined>()
  const porOperacion = new Map<string, EntradaDelCatalogo[]>()
  const porServicio = new Map<string, EntradaDelCatalogo[]>()
  const registrar = (e: EntradaDelCatalogo) => {
    if (!global.has(e.codigo) || (!global.get(e.codigo) && e.nombre)) global.set(e.codigo, e.nombre)
  }
  for (const { servicio, yaml, documento } of contratos) {
    for (const m of yaml.matchAll(CODIGO)) registrar({ codigo: m[0] })
    const delServicio: EntradaDelCatalogo[] = []
    for (const linea of yaml.split(/\r?\n/)) {
      for (const e of codigosDeLinea(linea)) if (!delServicio.some((x) => x.codigo === e.codigo)) delServicio.push(e)
    }
    porServicio.set(servicio, delServicio)
    const porRuta = codigosPorRutaYMetodo(yaml)
    for (const [clave, entradas] of porRuta) {
      entradas.forEach(registrar)
      const [ruta = '', metodo = ''] = clave.split(' ')
      const id = documento.paths?.[ruta]?.[metodo]?.operationId
      if (id) porOperacion.set(`${servicio}/${id}`, entradas)
    }
  }
  if (carpetaDeCasosDeUso && existsSync(carpetaDeCasosDeUso)) {
    for (const archivo of readdirSync(carpetaDeCasosDeUso).filter((a) => a.endsWith('.md'))) {
      const texto = readFileSync(join(carpetaDeCasosDeUso, archivo), 'utf8')
      for (const m of texto.matchAll(/([A-Z][A-Z0-9_]{2,}):\s*'(AP-CU\d+-\d{2})'/g)) registrar({ codigo: m[2] ?? m[0], nombre: m[1] })
    }
  }
  // `operationId: x   # CU-30` declara a qué caso de uso pertenece la operación: sus errores son
  // los `AP-CU30-nn` del catálogo, aunque el contrato no los repita junto a la operación.
  for (const { servicio, yaml } of contratos) {
    for (const m of yaml.matchAll(/operationId:\s*(\w+)[^\n#]*#\s*CU-(\d+)/g)) {
      const prefijo = `AP-CU${Number(m[2])}-`
      const delCaso = [...global].filter(([codigo]) => codigo.startsWith(prefijo)).map(([codigo, nombre]) => ({ codigo, nombre }))
      const clave = `${servicio}/${m[1]}`
      const propios = porOperacion.get(clave) ?? []
      const faltan = delCaso.filter((e) => !propios.some((p) => p.codigo === e.codigo))
      if (propios.length + faltan.length > 0) porOperacion.set(clave, [...propios, ...faltan])
    }
  }
  // el nombre de la regla puede venir de otra fuente (docs); se completa al final
  for (const entradas of [...porOperacion.values(), ...porServicio.values()]) {
    for (const e of entradas) e.nombre ??= global.get(e.codigo)
  }
  return { global, porOperacion, porServicio }
}

/** «SIN_CUPOS_LIBRES» → «Sin cupos libres». */
export function mensajeDeRegla(nombre: string | undefined, codigo: string): string {
  if (!nombre) return `La operación fue rechazada por la regla ${codigo}.`
  const frase = nombre.toLowerCase().replace(/_/g, ' ')
  return frase.charAt(0).toUpperCase() + frase.slice(1)
}
