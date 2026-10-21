import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const tokens = JSON.parse(readFileSync(new URL('../tokens.json', import.meta.url), 'utf8'))

function rgba(valor) {
  if (valor.startsWith('#')) {
    let hex = valor.slice(1)
    if (hex.length === 3) hex = [...hex].map((canal) => canal + canal).join('')
    return [0, 2, 4].map((indice) => parseInt(hex.slice(indice, indice + 2), 16)).concat(1)
  }
  const coincidencia = valor.match(/rgba?\(([^)]+)\)/)
  assert.ok(coincidencia, `color no reconocido: ${valor}`)
  const canales = coincidencia[1].split(',').map(Number)
  return [canales[0], canales[1], canales[2], canales[3] ?? 1]
}

// La capa del backoffice redefine algunos roles sobre el tema base: lo que no redefine, lo hereda.
const BASE = { backofficeClaro: 'claro', backofficeOscuro: 'oscuro' }

function resolver(tema, rol) {
  const valor = tokens.roles[tema][rol] ?? tokens.roles[BASE[tema]]?.[rol]
  return tokens.primitivas.color[valor] ?? valor
}

function sobre(frente, fondo) {
  const a = rgba(frente)
  const b = rgba(fondo)
  const alfa = a[3] + b[3] * (1 - a[3])
  return `#${a.slice(0, 3).map((canal, i) => Math.round((canal * a[3] + b[i] * b[3] * (1 - a[3])) / alfa).toString(16).padStart(2, '0')).join('')}`
}

function luminancia(color) {
  const canales = rgba(color).slice(0, 3).map((canal) => canal / 255).map((canal) =>
    canal <= 0.04045 ? canal / 12.92 : ((canal + 0.055) / 1.055) ** 2.4,
  )
  return canales[0] * 0.2126 + canales[1] * 0.7152 + canales[2] * 0.0722
}

function contraste(frente, fondo) {
  const a = luminancia(frente)
  const b = luminancia(fondo)
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05)
}

function fondoRenderizado(tema, rol) {
  const fondo = resolver(tema, rol)
  return rgba(fondo)[3] < 1 ? sobre(fondo, resolver(tema, 'surface')) : fondo
}

const TEMAS = ['claro', 'oscuro', 'backofficeClaro', 'backofficeOscuro']

test('roles de texto y estado superan 4.5:1 en ambos temas (y en la capa del backoffice)', () => {
  for (const tema of TEMAS) {
    for (const texto of ['text', 'text2', 'text3']) {
      for (const fondo of ['surface', 'bg']) {
        const ratio = contraste(resolver(tema, texto), resolver(tema, fondo))
        assert.ok(ratio >= 4.5, `${tema}: ${texto} sobre ${fondo} = ${ratio.toFixed(2)}:1`)
      }
    }
    for (const [texto, fondo] of [
      ['brandInk', 'brandBg'],
      ['brandTexto', 'brandBg'],
      ['text2', 'surface2'],
      ['sobreVerdeSolido', 'brand'],
      // ColorScheme.error/onError usa rojoSolido; `err` semántico es 4.38:1 con blanco.
      ['sobreRojoSolido', 'rojoSolido'],
      ['okTexto', 'okBg'],
      ['avisoTexto', 'warnBg'],
      ['errTexto', 'errBg'],
      ['infoTexto', 'infoBg'],
      ['piiTexto', 'piiBg'],
    ]) {
      const ratio = contraste(resolver(tema, texto), fondoRenderizado(tema, fondo))
      assert.ok(ratio >= 4.5, `${tema}: ${texto} sobre ${fondo} = ${ratio.toFixed(2)}:1`)
    }
    for (const texto of ['brandTexto', 'accentTexto']) {
      for (const fondo of ['surface', 'bg']) {
        const ratio = contraste(resolver(tema, texto), resolver(tema, fondo))
        assert.ok(ratio >= 4.5, `${tema}: ${texto} sobre ${fondo} = ${ratio.toFixed(2)}:1`)
      }
    }
    const accionDeSnackbar = contraste(resolver(tema, 'bg'), resolver(tema, 'text'))
    assert.ok(accionDeSnackbar >= 4.5, `${tema}: acción de snackbar bg sobre text = ${accionDeSnackbar.toFixed(2)}:1`)
  }
})

test('controles de acción y estado superan sus umbrales en ambos temas (y en la capa del backoffice)', () => {
  for (const tema of TEMAS) {
    for (const [texto, fondo] of [
      ['accentInk', 'accent'],
      ['sobreVerdeSolido', 'verdeSolido'],
      ['sobreRojoSolido', 'rojoSolido'],
    ]) {
      const ratio = contraste(resolver(tema, texto), resolver(tema, fondo))
      assert.ok(ratio >= 4.5, `${tema}: ${texto} sobre ${fondo} = ${ratio.toFixed(2)}:1`)
    }
    for (const control of ['accent', 'verdeSolido', 'rojoSolido']) {
      const fondos = control === 'rojoSolido' ? ['surface', 'bg'] : ['surface', 'surface2', 'bg', 'brandBg']
      for (const fondo of fondos) {
        const ratio = contraste(resolver(tema, control), resolver(tema, fondo))
        assert.ok(ratio >= 3, `${tema}: control ${control} sobre ${fondo} = ${ratio.toFixed(2)}:1`)
      }
    }
    const ratioDeBorde = contraste(resolver(tema, 'brand'), resolver(tema, 'surface'))
    assert.ok(ratioDeBorde >= 3, `${tema}: brand sobre surface = ${ratioDeBorde.toFixed(2)}:1`)
  }
})
