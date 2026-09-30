#!/usr/bin/env node
// Escribe generado/prism/todos.yaml: los catorce contratos fusionados en UNO
// para servirlos en un puerto, con los ejemplos de ejemplos/<servicio>/*.json
// inyectados como `examples:` con nombre, que el simulado sirve con
// `Prefer: example=<escenario>`. Los componentes se prefijan por servicio para que dos
// `Error` no choquen. Un rechazo de 4xx entra como ejemplo de su codigo; `intermitente`
// entra como 503 declarado en la copia (no en el contrato: el contrato no promete que el
// gateway se caiga, el simulado si lo reproduce).
import { mkdirSync, readFileSync, readdirSync, writeFileSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { stringify } from 'yaml'

const AQUI = dirname(fileURLToPath(import.meta.url))
const GENERADO = join(AQUI, '..', 'generado')
const EJEMPLOS = join(AQUI, '..', 'ejemplos')
const PRISM = join(GENERADO, 'prism')
mkdirSync(PRISM, { recursive: true })

const ERROR_503 = { type: 'object', properties: { codigo: { type: 'string' }, mensaje: { type: 'string' }, trazaId: { type: 'string' } } }
let inyectados = 0

/** Prefija todo `#/components/<tipo>/<Nombre>` con el servicio, para fusionar sin choques. */
function prefijarRefs(nodo, servicio) {
  if (Array.isArray(nodo)) return nodo.map((n) => prefijarRefs(n, servicio))
  if (nodo && typeof nodo === 'object') {
    const salida = {}
    for (const [k, v] of Object.entries(nodo)) {
      salida[k] = k === '$ref' && typeof v === 'string'
        ? v.replace(/^#\/components\/(\w+)\//, `#/components/$1/${servicio}__`)
        : prefijarRefs(v, servicio)
    }
    return salida
  }
  return nodo
}

const fusionado = { openapi: '3.1.0', info: { title: 'AportaYa · simulado', version: '0' }, servers: [], paths: {}, components: {} }
for (const archivo of readdirSync(GENERADO).filter((a) => a.endsWith('.json'))) {
  const servicio = archivo.replace(/\.json$/, '')
  const documento = JSON.parse(readFileSync(join(GENERADO, archivo), 'utf8'))
  // El prefijo del gateway se toma de `servers` en cada contrato.
  for (const operaciones of Object.values(documento.paths ?? {})) {
    for (const operacion of Object.values(operaciones)) {
      if (!operacion?.operationId) continue
      const ruta = join(EJEMPLOS, servicio, `${operacion.operationId}.json`)
      if (!existsSync(ruta)) continue
      const { escenarios } = JSON.parse(readFileSync(ruta, 'utf8'))
      for (const [nombre, ejemplo] of Object.entries(escenarios)) {
        const codigo = String(ejemplo.estado)
        operacion.responses ??= {}
        if (!operacion.responses[codigo]) {
          operacion.responses[codigo] = { description: `Simulado · ${nombre}`, content: { 'application/json': { schema: codigo === '503' ? ERROR_503 : {} } } }
        }
        const respuesta = operacion.responses[codigo]
        if (respuesta.$ref) {
          // Una respuesta compartida por $ref no admite ejemplos propios: se copia inline.
          const destino = respuesta.$ref.replace('#/components/responses/', '')
          operacion.responses[codigo] = JSON.parse(JSON.stringify(documento.components.responses[destino]))
        }
        const contenido = (operacion.responses[codigo].content ??= { 'application/json': {} })
        const json = (contenido['application/json'] ??= {})
        json.examples ??= {}
        json.examples[nombre] = { value: ejemplo.cuerpo }
        inyectados += 1
      }
    }
  }
  const conPrefijo = prefijarRefs(documento, servicio)
  // El prefijo del gateway (/api/v1) se escribe en cada ruta, y asi los tres clientes
  // apuntan a http://localhost:4010/api/v1
  // igual que a produccion (una sola base URL, planes/10 §3).
  const prefijo = (conPrefijo.servers?.[0]?.url ?? '').replace(/\/$/, '')
  for (const [ruta, ops] of Object.entries(conPrefijo.paths ?? {})) {
    const completa = `${prefijo}${ruta}`
    if (fusionado.paths[completa]) throw new Error(`la ruta ${completa} esta en dos contratos`)
    fusionado.paths[completa] = ops
  }
  for (const [tipo, entradas] of Object.entries(conPrefijo.components ?? {})) {
    fusionado.components[tipo] ??= {}
    for (const [nombre, def] of Object.entries(entradas)) fusionado.components[tipo][`${servicio}__${nombre}`] = def
  }
}
delete fusionado.servers
writeFileSync(join(PRISM, 'todos.yaml'), stringify(fusionado), 'utf8')
console.log(`simulado: ${inyectados} ejemplos inyectados · ${Object.keys(fusionado.paths).length} rutas en generado/prism/todos.yaml`)
