#!/usr/bin/env node
// Emite generado/tokens.dart desde tokens.json. Es el UNICO archivo Dart con
// literales de diseno; packages/diseno_flutter lo copia en su build y apps/movil lo
// consume via Tokens.of(context). No se versiona ni se edita.
import { mkdirSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { RAIZ, ROLES, TIPOS, resolverRol, tokens } from './comun.mjs'

const { color, espacio, radio, fuente, tactil, borde } = tokens.primitivas

/** `#1C5A3A` → `Color(0xFF1C5A3A)`; `rgba(31,157,87,.14)` → `Color.fromRGBO(31, 157, 87, 0.14)`. */
function aColorDart(valor) {
  const hex = /^#([0-9a-f]{6})$/i.exec(valor)
  if (hex) return `Color(0xFF${hex[1].toUpperCase()})`
  const rgba = /^rgba\((\d+),(\d+),(\d+),(\.?\d*\.?\d+)\)$/.exec(valor.replace(/\s/g, ''))
  if (rgba) return `Color.fromRGBO(${rgba[1]}, ${rgba[2]}, ${rgba[3]}, ${Number(rgba[4])})`
  throw new Error(`no se como convertir a Color: ${valor}`)
}

/** `0 4px 16px rgba(16,35,26,.08)` → BoxShadow. Solo sombras simples: es lo que hay. */
function aSombraDart(valor) {
  const m = /^(-?\d+)(?:px)? (-?\d+)px (\d+)px (rgba\([^)]+\))$/.exec(valor)
  if (!m) throw new Error(`no se como convertir a BoxShadow: ${valor}`)
  return `BoxShadow(offset: Offset(${m[1]}, ${m[2]}), blurRadius: ${m[3]}, color: ${aColorDart(m[4])})`
}

/** El tracking va en em en la bóveda; Flutter lo quiere en píxeles del tamaño. */
const aLetterSpacing = (t) => Number((t.track * t.tamano).toFixed(3))

const estilosDeTipo = TIPOS.map(([nombre, t]) => {
  const familia = t.familia === 'display' ? 'Fuente.display' : 'Fuente.cuerpo'
  const partes = [
    `fontFamily: ${familia}`,
    `fontSize: ${t.tamano}`,
    `fontWeight: FontWeight.w${t.peso}`,
    `height: ${t.alto}`,
    `letterSpacing: ${aLetterSpacing(t)}`,
  ]
  return `  static const ${nombre} = TextStyle(\n    ${partes.join(',\n    ')},\n  );`
}).join('\n\n')

const esSombra = (rol) => rol.startsWith('sombra')
const campos = ROLES.map((rol) => `  final ${esSombra(rol) ? 'BoxShadow' : 'Color'} ${rol};`).join('\n')
const parametros = ROLES.map((rol) => `    required this.${rol},`).join('\n')
const tema = (nombre) =>
  ROLES.map((rol) => {
    const v = resolverRol(tokens.roles[nombre][rol])
    return `    ${rol}: ${esSombra(rol) ? aSombraDart(v) : aColorDart(v)},`
  }).join('\n')
const lerp = ROLES.map((rol) =>
  esSombra(rol)
    ? `      ${rol}: BoxShadow.lerp(${rol}, otro.${rol}, t)!,`
    : `      ${rol}: Color.lerp(${rol}, otro.${rol}, t)!,`,
).join('\n')

const dart = `// GENERADO por packages/tokens/scripts/a-dart.mjs desde tokens.json — no editar a mano.
// Es el único archivo Dart del proyecto con literales de diseño (invariante 3).
// ignore_for_file: public_member_api_docs

import 'package:flutter/material.dart';

/// Paleta cruda. Un widget no la pide: pide un rol de [Tokens].
abstract final class Paleta {
${Object.entries(color).map(([k, v]) => `  static const ${k} = ${aColorDart(v)};`).join('\n')}
}

/// Escalas del sistema. Múltiplos de 4; un valor fuera de acá es un valor inventado.
abstract final class Espacio {
${Object.entries(espacio).map(([k, v]) => `  static const double ${k} = ${v};`).join('\n')}
}

abstract final class Radios {
${Object.entries(radio).map(([k, v]) => `  static const double ${k} = ${v};`).join('\n')}
}

abstract final class Borde {
${Object.entries(borde).map(([k, v]) => `  static const double ${k} = ${v};`).join('\n')}
}

abstract final class Fuente {
  static const display = '${fuente.displayFamilia}';
  static const cuerpo = '${fuente.cuerpoFamilia}';
}

/// Un estilo por uso, no un tamaño suelto. Una pantalla pide \`Tipo.titulo1\`; si le
/// hace falta un tamaño que no está acá, falta un rol en la bóveda, no un número.
abstract final class Tipo {
${estilosDeTipo}
}

abstract final class Tactil {
  /// Área táctil mínima en dp. 48 es la pauta de Android; por debajo se queda afuera
  /// quien no tiene pulso firme, que en una billetera es mucha gente.
  static const double minimo = ${tactil.minimoMovil};
}

/// Los roles del tema: lo que un widget pide. El tema oscuro redefine solo esto.
class Tokens extends ThemeExtension<Tokens> {
  const Tokens({
${parametros}
  });

${campos}

  static const claro = Tokens(
${tema('claro')}
  );

  static const oscuro = Tokens(
${tema('oscuro')}
  );

  static Tokens of(BuildContext context) => Theme.of(context).extension<Tokens>()!;

  @override
  Tokens copyWith() => this;

  @override
  Tokens lerp(ThemeExtension<Tokens>? other, double t) {
    if (other is! Tokens) return this;
    final otro = other;
    return Tokens(
${lerp}
    );
  }
}
`
mkdirSync(resolve(RAIZ, 'generado'), { recursive: true })
writeFileSync(resolve(RAIZ, 'generado/tokens.dart'), dart, 'utf8')
process.stdout.write(`generado/tokens.dart · ${dart.split('\n').length} lineas\n`)
