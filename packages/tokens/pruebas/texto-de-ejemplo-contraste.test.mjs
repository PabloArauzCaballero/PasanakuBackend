import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const tokens = JSON.parse(readFileSync(new URL('../tokens.json', import.meta.url), 'utf8'))

function luminancia(hex) {
  const canales = hex.match(/[0-9a-f]{2}/gi).map((canal) => parseInt(canal, 16) / 255)
  const lineales = canales.map((canal) =>
    canal <= 0.04045 ? canal / 12.92 : ((canal + 0.055) / 1.055) ** 2.4,
  )
  return lineales[0] * 0.2126 + lineales[1] * 0.7152 + lineales[2] * 0.0722
}

function colorDeTema(tema, rol) {
  const valor = tokens.roles[tema][rol]
  return tokens.primitivas.color[valor] ?? valor
}

test('textoDeEjemplo tiene contraste de al menos 3:1 con el campo en ambos temas', () => {
  for (const tema of ['claro', 'oscuro']) {
    assert.ok(tokens.roles[tema].textoDeEjemplo, `${tema}: falta el rol`)
    const ejemplo = colorDeTema(tema, 'textoDeEjemplo')
    const campo = colorDeTema(tema, 'field')
    const l1 = luminancia(ejemplo)
    const l2 = luminancia(campo)
    const ratio = (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05)
    assert.ok(ratio >= 3, `${tema}: ${ejemplo} sobre ${campo} = ${ratio.toFixed(2)}:1`)
  }
})
