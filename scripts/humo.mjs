#!/usr/bin/env node
// Humo honesto (PR13, H4.S1.M1): antes, `package.json` corría cada colección con
// `|| true`, así que una colección rota nunca hacía fallar el comando — el kill-test
// del encargo lo demuestra: romper una obligatoria a propósito seguía dando exit 0.
//
// Clasificación obligatoria/informativa: el repo NO trae hoy ninguna marca por
// colección (se buscó en `postman/humo/*.json`, sin resultado — ver
// `entregables/PR13-carril.md`, ambigüedad registrada). Por omisión, **toda**
// colección es obligatoria (denegar por omisión, mismo criterio que el resto del
// proyecto: "sin marca, no se asume permisivo"). `INFORMATIVAS` abajo es la única
// lista de excepciones, y hoy está vacía a propósito: nadie decidió todavía que un
// servicio pueda fallar su humo sin bloquear.
import { readdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import newman from 'newman'

const AQUI = path.dirname(fileURLToPath(import.meta.url))
const RAIZ = path.resolve(AQUI, '..')
const DIR_COLECCIONES = path.join(RAIZ, 'postman/humo')
const ENTORNO = path.join(RAIZ, 'postman/entornos/local.postman_environment.json')

/** Excepciones explícitas: colecciones cuyo fallo avisa pero no bloquea. Vacío por omisión. */
export const INFORMATIVAS = new Set([])

export function clasificar(nombreDeArchivo) {
  return INFORMATIVAS.has(nombreDeArchivo) ? 'informativa' : 'obligatoria'
}

export function listarColecciones(dir = DIR_COLECCIONES) {
  return readdirSync(dir)
    .filter((f) => f.endsWith('.postman_collection.json'))
    .sort()
}

/** Corre una colección con la API de Node de newman (no el CLI: menos parseo de texto, misma info). */
export function correrColeccion(nombreDeArchivo, { dir = DIR_COLECCIONES, entorno = ENTORNO } = {}) {
  return new Promise((resolver, rechazar) => {
    newman.run(
      {
        collection: path.join(dir, nombreDeArchivo),
        environment: entorno,
        reporters: ['cli'],
        color: 'off',
      },
      (error, resumen) => {
        if (error) return rechazar(error)
        const fallas = resumen?.run?.failures?.length ?? 0
        resolver({ nombreDeArchivo, fallas, ok: fallas === 0 })
      },
    )
  })
}

/**
 * Combina los resultados de todas las colecciones en un único veredicto: falla si
 * (y solo si) alguna colección **obligatoria** tuvo fallas. Función pura, separada de
 * I/O — es lo que prueba `humo.spec.mjs` con los tres niveles del encargo sin llamar a
 * newman de verdad.
 */
export function resultadoFinal(resultados) {
  const rotas = resultados.filter((r) => !r.ok)
  const obligatoriasRotas = rotas.filter((r) => clasificar(r.nombreDeArchivo) === 'obligatoria')
  const informativasRotas = rotas.filter((r) => clasificar(r.nombreDeArchivo) === 'informativa')
  return {
    exitoso: obligatoriasRotas.length === 0,
    obligatoriasRotas: obligatoriasRotas.map((r) => r.nombreDeArchivo),
    informativasRotas: informativasRotas.map((r) => r.nombreDeArchivo),
  }
}

async function principal() {
  const colecciones = listarColecciones()
  const resultados = []
  for (const nombreDeArchivo of colecciones) {
    console.log(`\n▶ ${nombreDeArchivo} (${clasificar(nombreDeArchivo)})`)
    resultados.push(await correrColeccion(nombreDeArchivo))
  }
  const { exitoso, obligatoriasRotas, informativasRotas } = resultadoFinal(resultados)
  if (informativasRotas.length > 0) {
    console.warn(`\n⚠ Informativas con fallas (no bloquean): ${informativasRotas.join(', ')}`)
  }
  if (!exitoso) {
    console.error(`\n✖ Humo obligatorio roto: ${obligatoriasRotas.join(', ')}`)
    process.exitCode = 1
    return
  }
  console.log('\n✓ Todas las colecciones obligatorias en verde.')
}

const esEjecutadoDirectamente = process.argv[1] === fileURLToPath(import.meta.url)
if (esEjecutadoDirectamente) {
  principal().catch((error) => {
    console.error(error)
    process.exitCode = 1
  })
}
