import type { ErrorParaVista } from '@aportaya/ui/error-de-envio/error-de-envio'

/**
 * Los rechazos del gobierno de accesos (identidad, CU-08 y el step-up de CU-04), en lenguaje humano y con el campo al
 * que pertenecen (regla 95.3): el servidor responde `AP-CU08-xx` y la interfaz nunca muestra el texto crudo. Un código
 * que no se conoce cae al mensaje que el interceptor ya tradujo por estado.
 */
export type CampoDeGobierno = 'rol' | 'ambito' | 'motivo' | 'codigo' | 'vigencia'
export type ErrorDeGobierno = ErrorParaVista & { codigo?: string; campo?: CampoDeGobierno }

const POR_CODIGO: Readonly<Record<string, { mensaje: string; campo?: CampoDeGobierno }>> = {
  'AP-CU08-01': { mensaje: 'Ese rol o esa asignación ya no existe. Actualizá la pantalla y volvé a elegir.', campo: 'rol' },
  'AP-CU08-02': { mensaje: 'El ámbito GRUPO necesita el identificador del grupo; los demás ámbitos no lo llevan.', campo: 'ambito' },
  'AP-CU08-03': { mensaje: 'Ese rol es incompatible con otro que la persona ya tiene (segregación de funciones). Elegí otro o revocá primero el que choca.', campo: 'rol' },
  'AP-CU08-04': { mensaje: 'Nadie puede cambiarse sus propios accesos ni darse de baja. Lo tiene que hacer otra persona con permiso.' },
  'AP-CU08-05': { mensaje: 'La persona todavía no tiene segundo factor confirmado: no podrá entrar hasta configurarlo.' },
  'AP-CU08-06': { mensaje: 'Esto dejaría a la plataforma sin ningún administrador de accesos. Primero asigná ese rol a otra persona.' },
  'AP-CU08-08': { mensaje: 'Ese rol no se otorga a personal interno (es de participantes, grupos, anunciantes o servicios). Elegí otro.', campo: 'rol' },
  'AP-CU08-09': { mensaje: 'Falta confirmar con tu segundo factor, o el código ya se usó o venció. Ingresá uno nuevo.', campo: 'codigo' },
  'AP-CU08-10': { mensaje: 'La cuenta de la persona no está habilitada (suspendida, bloqueada o dada de baja). Reactivala antes de este cambio.' },
  'AP-CU08-11': { mensaje: 'Escribí el motivo (mínimo 10 caracteres): queda en la auditoría.', campo: 'motivo' },
  'AP-CU08-12': { mensaje: 'No pudimos seguir el listado desde donde estaba. Volvé a la primera página.' },
  'AP-CU04-04': { mensaje: 'Ese código no sirve: revisalo en tu aplicación de autenticación o esperá al siguiente.', campo: 'codigo' },
  'AP-CU04-06': { mensaje: 'Tu segundo factor no está configurado. Configuralo antes de hacer cambios de acceso.', campo: 'codigo' },
  'AP-CU04-08': { mensaje: 'El código ya se usó o venció. Ingresá uno nuevo de tu aplicación.', campo: 'codigo' },
}

/** Lo que trae el rechazo ya pasado por el interceptor de errores (`ErrorTraducido`), o un error cualquiera. */
export function traducirErrorDeGobierno(e: unknown, generico: string): ErrorDeGobierno {
  const origen = e instanceof Error && e.cause !== undefined && !('mensaje' in e) ? e.cause : e
  const o = (origen && typeof origen === 'object' ? origen : {}) as { mensaje?: unknown; trazaId?: unknown; codigo?: unknown; estado?: unknown; sinConexion?: unknown; tiempoAgotado?: unknown }
  const codigo = typeof o.codigo === 'string' ? o.codigo : undefined
  const conocido = codigo ? POR_CODIGO[codigo] : undefined
  return {
    mensaje: conocido?.mensaje ?? (typeof o.mensaje === 'string' ? o.mensaje : generico),
    trazaId: typeof o.trazaId === 'string' ? o.trazaId : undefined,
    estado: typeof o.estado === 'number' ? o.estado : undefined,
    sinConexion: o.sinConexion === true,
    tiempoAgotado: o.tiempoAgotado === true,
    codigo,
    campo: conocido?.campo,
  }
}

/** ¿El servidor respondió y rechazó? Si no, no se sabe si la operación quedó registrada y la evidencia del segundo factor puede reusarse. */
export function esRechazoDefinitivo(e: ErrorDeGobierno): boolean {
  const estado = e.estado ?? 0
  return estado >= 400 && estado < 500 && ![408, 425, 429].includes(estado) && !e.sinConexion && !e.tiempoAgotado
}
