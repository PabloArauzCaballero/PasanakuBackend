// Lo que los dos generadores comparten: leer tokens.json y resolver roles a valores.
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

export const RAIZ = resolve(dirname(fileURLToPath(import.meta.url)), '..')
export const tokens = JSON.parse(readFileSync(resolve(RAIZ, 'tokens.json'), 'utf8'))

/** Un rol que nombra una primitiva se resuelve a su valor; lo demas queda tal cual. */
export function resolverRol(valor) {
  const color = tokens.primitivas.color[valor]
  return color ?? valor
}

/** `brandTexto` → `brand-texto`, `surface2` → `surface-2`. */
export const aKebab = (nombre) =>
  nombre.replace(/[A-Z]/g, (l) => `-${l.toLowerCase()}`).replace(/([a-z])(\d)/g, '$1-$2')

export const ROLES = Object.keys(tokens.roles.claro)

/** Los roles tipográficos, sin las claves de comentario (`//...`) del JSON. */
export const TIPOS = Object.entries(tokens.primitivas.tipo).filter(([k]) => !k.startsWith('//'))

/** `display` → `--font-d`, `cuerpo` → `--font-b`: cómo nombra el CSS cada familia. */
export const varDeFamilia = (familia) => `var(--font-${familia === 'display' ? 'd' : 'b'})`

/** El `font` abreviado de CSS para un rol: `peso tamaño/alto familia`. */
export const aFontCss = (t) => `${t.peso} ${t.tamano}px/${t.alto} ${varDeFamilia(t.familia)}`
