import type { UsuarioInternoDeLista } from '../dominio/gobierno-de-accesos'

export const ESTADO: Record<string, string> = { ACTIVO: 'Activo', SUSPENDIDO: 'Suspendido', BAJA_LABORAL: 'Baja laboral', BAJA_VOLUNTARIA: 'Baja voluntaria', BLOQUEADO_SEGURIDAD: 'Bloqueado por seguridad' }
export const TONO: Record<string, 'ok' | 'aviso' | 'neutro' | 'error'> = { ACTIVO: 'ok', SUSPENDIDO: 'aviso', BLOQUEADO_SEGURIDAD: 'error' }
/** Hay un bloqueo de acceso vigente: con fecha de fin, o indefinido (`bloqueadoHasta` nulo y `motivoBloqueo` informado, GU-10). */
export const estaBloqueado = (u: UsuarioInternoDeLista) => !!u.bloqueadoHasta || !!u.motivoBloqueo
export const estaDeBaja = (u: UsuarioInternoDeLista) => u.estado === 'BAJA_LABORAL'
export const estadoDe = (u: UsuarioInternoDeLista) => ESTADO[u.estado] ?? u.estado
export const segundoFactor = (u: UsuarioInternoDeLista) => (u.tieneSegundoFactor ? 'Configurado' : 'Pendiente')
