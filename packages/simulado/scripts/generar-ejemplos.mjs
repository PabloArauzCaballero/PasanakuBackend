#!/usr/bin/env node
// Produce ejemplos/<servicio>/<operationId>.json desde el esquema de cada operacion.
//
// Un archivo por caso de uso, un escenario por clave. Se GENERA la primera vez y
// despues se conserva: los valores de negocio (nombres, importes, codigos de error)
// se editan a mano, la forma no. Si el contrato cambia y el ejemplo deja de validar,
// la prueba de contrato lo dice — no este script.
//
//   yarn workspace @aportaya/simulado ejemplos          (solo crea los que faltan)
//   yarn workspace @aportaya/simulado ejemplos --forzar (rehace todos)
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs'
import { registerHooks } from 'node:module'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

registerHooks({
  resolve(especificador, contexto, siguiente) {
    if (especificador.startsWith('.') && !/\.[cm]?[jt]s$/.test(especificador)) {
      const candidato = new URL(`${especificador}.ts`, contexto.parentURL)
      if (existsSync(candidato)) return { url: candidato.href, shortCircuit: true }
    }
    return siguiente(especificador, contexto)
  },
})
const { muestraDe } = await import('../src/muestra.ts')
const { resolver } = await import('../src/referencias.ts')
const { construirCatalogo, mensajeDeRegla } = await import('../src/catalogo.ts')

const AQUI = dirname(fileURLToPath(import.meta.url))
const GENERADO = join(AQUI, '..', 'generado')
const EJEMPLOS = join(AQUI, '..', 'ejemplos')
const forzar = process.argv.includes('--forzar')
const METODOS = ['get', 'post', 'put', 'patch', 'delete']

// El catalogo sale del YAML de cada contrato (comentarios `# AP-CUnn-nn`) y de docs/CasosDeUso.
const origen = existsSync(join(GENERADO, 'ORIGEN.txt')) ? readFileSync(join(GENERADO, 'ORIGEN.txt'), 'utf8').trim() : null
const contratos = readdirSync(GENERADO).filter((a) => a.endsWith('.json')).map((a) => {
  const servicio = a.replace(/\.json$/, '')
  return { servicio, yaml: readFileSync(join(GENERADO, `${servicio}.yaml`), 'utf8'), documento: JSON.parse(readFileSync(join(GENERADO, a), 'utf8')) }
})
const carpetaCU = [origen && join(origen, 'docs/CasosDeUso'), join(AQUI, '../../../docs/CasosDeUso')].find((c) => c && existsSync(c))
const catalogo = construirCatalogo(contratos, carpetaCU)
// Prioridad: codigos que el contrato menciona junto a la operacion; si no hay, los del servicio.
function errorDeCatalogo(servicio, operacion, estado, forma) {
  const propios = catalogo.porOperacion.get(`${servicio}/${operacion}`) ?? []
  const delServicio = catalogo.porServicio.get(servicio) ?? []
  const elegido = [...propios, ...delServicio].find((e) => catalogo.global.has(e.codigo)) ?? null
  if (!elegido) return null
  const mensaje = mensajeDeRegla(elegido.nombre ?? catalogo.global.get(elegido.codigo), elegido.codigo)
  // RFC 7807 (transparencia): tipo/titulo/estado/codigo/detalle
  if (forma === 'problema') return { tipo: 'about:blank', titulo: mensaje, estado, codigo: elegido.codigo, detalle: null }
  return { codigo: elegido.codigo, mensaje, detalle: {}, trazaId: `traza-${operacion}-rechazo` }
}
/** Forma del cuerpo de error que declara el contrato para ese estado. */
function formaDeError(documento, operacion, estado) {
  const declarada = resolver(documento, operacion.responses?.[String(estado)] ?? {})
  const medios = Object.keys(declarada?.content ?? {})
  return medios.length > 0 && !medios.includes('application/json') && medios.some((m) => m.includes('problem')) ? 'problema' : 'error'
}

function estados(operacion, cumple) {
  return Object.keys(operacion.responses ?? {}).map(Number).filter((c) => Number.isFinite(c) && cumple(c)).sort((a, b) => a - b)
}
function cuerpo(documento, operacion, estado, modo) {
  const declarada = resolver(documento, operacion.responses?.[String(estado)] ?? {})
  const esquema = declarada?.content?.['application/json']?.schema
  if (!esquema) return null
  return muestraDe(documento, esquema, `${operacion.operationId}.${estado}`, modo)
}

let creados = 0
let alineados = 0
/** Un rechazo cuyo codigo no esta en el catalogo de la operacion se reemplaza por uno que si; el resto del archivo (editado a mano) no se toca. */
function alinearRechazo(destino, servicio, operationId, documento, operacion) {
  const archivo = JSON.parse(readFileSync(destino, 'utf8'))
  const rechazo = archivo.escenarios?.rechazo
  if (!rechazo) return
  const propios = (catalogo.porOperacion.get(`${servicio}/${operationId}`) ?? []).map((e) => e.codigo)
  const actual = rechazo.cuerpo?.codigo
  const vigente = typeof actual === 'string' && (propios.length > 0 ? propios.includes(actual) : catalogo.global.has(actual))
  if (vigente) return
  const nuevo = errorDeCatalogo(servicio, operationId, rechazo.estado, formaDeError(documento, operacion, rechazo.estado))
  if (!nuevo) return
  rechazo.cuerpo = nuevo
  writeFileSync(destino, `${JSON.stringify(archivo, null, 2)}\n`, 'utf8')
  alineados += 1
}
let conservados = 0
for (const archivo of readdirSync(GENERADO).filter((a) => a.endsWith('.json'))) {
  const servicio = archivo.replace(/\.json$/, '')
  const documento = JSON.parse(readFileSync(join(GENERADO, archivo), 'utf8'))
  mkdirSync(join(EJEMPLOS, servicio), { recursive: true })
  for (const [ruta, operaciones] of Object.entries(documento.paths ?? {})) {
    for (const metodo of METODOS) {
      const operacion = operaciones[metodo]
      if (!operacion?.operationId) continue
      const destino = join(EJEMPLOS, servicio, `${operacion.operationId}.json`)
      if (existsSync(destino) && !forzar) {
        conservados += 1
        alinearRechazo(destino, servicio, operacion.operationId, documento, operacion)
        continue
      }
      const exito = estados(operacion, (c) => c >= 200 && c < 300)[0]
      const rechazo = estados(operacion, (c) => c >= 400 && c !== 401 && c !== 403)[0] ?? estados(operacion, (c) => c >= 400)[0]
      if (exito === undefined) continue
      const escenarios = {
        ok: { estado: exito, cuerpo: cuerpo(documento, operacion, exito, 'representativo') },
        vacio: { estado: exito, cuerpo: cuerpo(documento, operacion, exito, 'minimo') },
        adverso: { estado: exito, cuerpo: cuerpo(documento, operacion, exito, 'representativo') },
      }
      if (metodo !== 'get') {
        escenarios.aceptado = { estado: 202, cuerpo: cuerpo(documento, operacion, exito, 'representativo') }
        escenarios.intermitente = { estado: 503, cuerpo: { codigo: 'AP-GW-503', mensaje: 'El servicio no respondió', trazaId: 'traza-intermitente' }, luego: 'ok' }
      }
      if (rechazo !== undefined) {
        escenarios.rechazo = { estado: rechazo, cuerpo: errorDeCatalogo(servicio, operacion.operationId, rechazo, formaDeError(documento, operacion, rechazo)) ?? cuerpo(documento, operacion, rechazo, 'representativo') }
      }
      writeFileSync(destino, `${JSON.stringify({ servicio, operacion: operacion.operationId, metodo, ruta, escenarios }, null, 2)}\n`, 'utf8')
      creados += 1
    }
  }
}
console.log(`ejemplos: ${creados} creados · ${conservados} conservados (editados a mano, no se pisan) · ${alineados} rechazos alineados al catalogo de errores`)
