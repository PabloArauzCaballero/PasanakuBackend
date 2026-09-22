#!/usr/bin/env node
// Emite vectores/monto.json: 5.000 importes deterministas y su formato esperado.
// El Monto de Angular y el de Flutter pasan los mismos vectores; si difieren en un
// centavo, el sistema muestra el mismo saldo distinto en dos pantallas.
import { existsSync, mkdirSync, writeFileSync } from 'node:fs'
import { registerHooks } from 'node:module'
import { resolve } from 'node:path'
import { RAIZ } from './comun.mjs'

registerHooks({
  resolve(especificador, contexto, siguiente) {
    if (especificador.startsWith('.') && !/\.[cm]?[jt]s$/.test(especificador)) {
      const candidato = new URL(`${especificador}.ts`, contexto.parentURL)
      if (existsSync(candidato)) return { url: candidato.href, shortCircuit: true }
    }
    return siguiente(especificador, contexto)
  },
})
const { formatearMonto } = await import('../dinero/formatear.ts')

// El mismo generador determinista que la prueba de propiedad de F1.
let semilla = 20260828
const siguiente = () => {
  semilla = (semilla * 1103515245 + 12345) % 2147483648
  return semilla / 2147483648
}
const casos = [
  { monto: '0.00', moneda: 'BOB' },
  { monto: '1240.00', moneda: 'BOB' },
  { monto: '48750.00', moneda: 'BOB' },
  { monto: '1234567.89', moneda: 'BOB' },
  { monto: '-1240.50', moneda: 'BOB' },
  { monto: '10.00', moneda: 'USD' },
]
for (let i = 0; i < 5000; i += 1) {
  const digitos = 1 + Math.floor(siguiente() * 18)
  let enteros = String(1 + Math.floor(siguiente() * 9))
  for (let d = 1; d < digitos; d += 1) enteros += String(Math.floor(siguiente() * 10))
  const centavos = String(Math.floor(siguiente() * 100)).padStart(2, '0')
  const signo = siguiente() < 0.2 ? '-' : ''
  casos.push({ monto: `${signo}${enteros}.${centavos}`, moneda: 'BOB' })
}
const vectores = casos.map((c) => ({ ...c, esperado: formatearMonto(c) }))
mkdirSync(resolve(RAIZ, 'vectores'), { recursive: true })
writeFileSync(resolve(RAIZ, 'vectores/monto.json'), `${JSON.stringify(vectores)}\n`, 'utf8')
process.stdout.write(`vectores/monto.json · ${vectores.length} casos\n`)
