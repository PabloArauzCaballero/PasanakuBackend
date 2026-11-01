import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import type { UsuarioInternoResumen } from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import type { Acceso, PuertoAccesos } from './puertos'
import { SIN_DATO } from './salud-de-servicios'

/** `usuario.estado` en palabras (los valores del CHECK de `sql/10_tablas/01_identidad_usuarios/usuario.sql`); uno nuevo se muestra tal cual. */
const ESTADO: Readonly<Record<string, string>> = {
  ACTIVO: 'Activo',
  BAJA_VOLUNTARIA: 'Baja voluntaria',
  BLOQUEADO_SEGURIDAD: 'Bloqueado por seguridad',
  ELIMINADO: 'Eliminado',
  INCOMPLETO_KYC: 'Identidad incompleta',
  PENDIENTE_VERIFICACION: 'Pendiente de verificación',
  PRE_REGISTRO: 'Prerregistro',
  SUSPENDIDO: 'Suspendido',
}

/**
 * Revisión de accesos con los datos REALES de identidad: `GET /usuarios-internos` (`listarUsuariosInternos`), una fila
 * por persona y rol — revisar accesos es revisar cada par, no cada persona. Se muestra el estado de la cuenta y si
 * tiene segundo factor, que es lo que una revisión mira primero. El correo y el teléfono no se usan: la vista no los
 * necesita (regla 96.5.3). Una persona sin roles sale igual, como «sin rol»: un usuario interno sin rol también se
 * revisa. Cuándo se otorgó cada rol no lo publica el contrato: queda en «—».
 */
export function filasDeAccesos(usuarios: readonly UsuarioInternoResumen[]): Acceso[] {
  return usuarios.flatMap((u) => {
    const persona = `${u.nombres} ${u.apellidos}`.trim()
    const comun = {
      persona,
      ambito: u.departamento || SIN_DATO,
      otorgadoEl: SIN_DATO,
      estado: u.bloqueadoHasta ? 'Bloqueado temporalmente' : (ESTADO[u.estado] ?? u.estado),
      segundoFactor: u.tieneSegundoFactor ? 'Sí' : 'No',
    }
    const roles = u.roles.length ? u.roles : ['sin rol']
    return roles.map((rol) => ({ id: `${u.id}:${rol}`, rol, ...comun }))
  })
}

/** El puerto de accesos contra el gateway. Un error llega traducido por el interceptor (mensaje y trazaId). */
export function puertoAccesos(): PuertoAccesos {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return { obtener: async () => filasDeAccesos(await firstValueFrom(http.get<UsuarioInternoResumen[]>(`${gateway}/usuarios-internos`))) }
}
