import path from 'node:path'

/**
 * Entorno de la suite de aceptación con DOS usuarios reales (A = participante en el sitio web, B = operador de backoffice).
 *
 * La MISMA suite corre en dos modos; solo cambian estas variables:
 *   ACEPTACION_MODO = mock | real   (por defecto `mock`)
 *   BASE_API        = origen de la API  (mock: el doble con estado de `doble-con-estado.ts`; real: el gateway del stack que levanta D)
 *   BASE_WEB        = origen de apps/web
 *   BASE_BO         = origen de apps/backoffice (su `ng serve`/build debe proxyear `/api/v1` a BASE_API: ver `proxy.aceptacion.mjs`)
 *
 * Modo `mock` = VERIFIED_FUNCTIONAL_ONLY (el backend es un doble). Modo `real` = contra servicios reales.
 */
export type Modo = 'mock' | 'real'

const PUERTO_BASE = Number(process.env['PUERTO_BASE'] ?? 5445)

export const MODO: Modo = process.env['ACEPTACION_MODO'] === 'real' ? 'real' : 'mock'
export const BASE_API = (process.env['BASE_API'] ?? `http://127.0.0.1:${PUERTO_BASE + 1}`).replace(/\/$/, '')
export const BASE_BO = (process.env['BASE_BO'] ?? `http://127.0.0.1:${PUERTO_BASE + 2}`).replace(/\/$/, '')
export const BASE_WEB = (process.env['BASE_WEB'] ?? `http://127.0.0.1:${PUERTO_BASE + 3}`).replace(/\/$/, '')
export const API = `${BASE_API}/api/v1`

export const EVIDENCIA = process.env['EVIDENCIA_DIR'] ?? path.resolve(__dirname, '../../../../../docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-E3')
export const CAPTURAS = path.join(EVIDENCIA, 'capturas', MODO)

export const VIEWPORTS = {
  movil: { width: 360, height: 740 },
  tablet: { width: 768, height: 1024 },
  escritorio: { width: 1280, height: 900 },
} as const
export type Viewport = keyof typeof VIEWPORTS
export const TEMAS = ['light', 'dark'] as const
export type Tema = (typeof TEMAS)[number]

/** Etiqueta de peldaño que la suite escribe en su informe. */
export const ETIQUETA = MODO === 'mock' ? 'VERIFIED_FUNCTIONAL_ONLY (doble con estado)' : 'VERIFIED_FUNCTIONAL_ONLY (stack real; falta inspección visual por persona)'
