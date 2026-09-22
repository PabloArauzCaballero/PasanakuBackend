/**
 * CU-94 · Elevar una decisión al comité de gobierno (skill `gobierno-comites`).
 *
 * Quórum no es contar cabezas: el comité tiene `quorumMinimo` Y `composicionRequerida`.
 * Un acta NO SE CIERRA sin las dos cosas — es la regla de la base
 * (`AP-CU94-01 SIN_QUORUM`, `AP-CU94-02 COMPOSICION_INCOMPLETA`) reflejada acá para que
 * el botón de cerrar quede deshabilitado antes de golpear al servidor.
 *
 * **Contrato pendiente:** `cumplimiento.yaml` declara los códigos de error de CU-94
 * pero no una ruta HTTP (comité, sesión y acta no tienen operación en el contrato
 * real). Se declara como hueco — el dominio queda listo para conectarse.
 */
export type VotoNominal = { integranteId: string; rol: string; voto: 'A_FAVOR' | 'EN_CONTRA' | 'ABSTENCION'; motivoAbstencion?: string }

export type ActaDeComite = {
  id: string
  comiteId: string
  quorumMinimo: number
  composicionRequerida: string[]
  asistentes: VotoNominal[]
}

export function tieneQuorum(acta: ActaDeComite): boolean {
  return acta.asistentes.length >= acta.quorumMinimo
}

export function composicionCompleta(acta: ActaDeComite): boolean {
  const rolesPresentes = new Set(acta.asistentes.map((a) => a.rol))
  return acta.composicionRequerida.every((rol) => rolesPresentes.has(rol))
}

/** Toda abstención necesita su motivo escrito (`gobierno-comites`: "un acta sin disidencias registradas es un acta incompleta"). */
export function abstencionesConMotivo(acta: ActaDeComite): boolean {
  return acta.asistentes.filter((a) => a.voto === 'ABSTENCION').every((a) => !!a.motivoAbstencion?.trim())
}

export function puedeCerrarActa(acta: ActaDeComite): boolean {
  return tieneQuorum(acta) && composicionCompleta(acta) && abstencionesConMotivo(acta)
}

export function motivoDeBloqueo(acta: ActaDeComite): string | null {
  if (!tieneQuorum(acta)) return 'sinQuorum'
  if (!composicionCompleta(acta)) return 'composicionIncompleta'
  if (!abstencionesConMotivo(acta)) return 'abstencionSinMotivo'
  return null
}
