import type { ErrorParaVista } from '@aportaya/ui/error-de-envio/error-de-envio'

/**
 * Los rechazos del gobierno de accesos (identidad, CU-08 y el step-up de CU-04), en lenguaje humano y con el campo al
 * que pertenecen (regla 95.3): el servidor responde `AP-CU08-xx` y la interfaz nunca muestra el texto crudo. Un código
 * que no se conoce cae al mensaje que el interceptor ya tradujo por estado.
 */
export type CampoDeGobierno = 'rol' | 'ambito' | 'motivo' | 'codigo' | 'vigencia' | 'fechaNacimiento'
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
  'AP-CU08-13': { mensaje: 'La fecha de nacimiento tiene que ser real: no puede faltar ni ser futura.', campo: 'fechaNacimiento' },
  'AP-CU08-12': { mensaje: 'No pudimos seguir el listado desde donde estaba. Volvé a la primera página.' },
  // CU-135 (GU-16): roles personalizados
  'AP-CU135-01': { mensaje: 'El código va en mayúsculas, sin espacios, de 3 a 30 caracteres, y no puede empezar con SERVICIO_. El nombre es obligatorio.', campo: 'rol' },
  'AP-CU135-02': { mensaje: 'Ya hay un rol con ese código. Elegí otro.', campo: 'rol' },
  'AP-CU135-03': { mensaje: 'Un rol necesita al menos un permiso.', campo: 'rol' },
  'AP-CU135-04': { mensaje: 'Alguno de los permisos ya no existe en el catálogo. Actualizá la pantalla y volvé a elegir.', campo: 'rol' },
  'AP-CU135-05': { mensaje: 'Esos permisos juntos permiten autorizar y ejecutar la misma operación (segregación de funciones). Sacá uno de los dos.', campo: 'rol' },
  'AP-CU135-06': { mensaje: 'Los roles del sistema no se editan ni se retiran.' },
  'AP-CU135-07': { mensaje: 'Ese rol ya no existe o está retirado. Actualizá la pantalla.' },
  'AP-CU135-08': { mensaje: 'Escribí el motivo (hasta 160 caracteres): queda en la auditoría.', campo: 'motivo' },
  'AP-CU135-09': { mensaje: 'Falta confirmar con tu segundo factor, o el código ya se usó o venció. Ingresá uno nuevo.', campo: 'codigo' },
  'AP-CU135-10': { mensaje: 'Este rol todavía tiene personas asignadas. Revocá primero sus asignaciones y volvé a retirarlo.' },
  'AP-CU135-11': { mensaje: 'Esa solicitud ya no existe. Actualizá la lista.' },
  'AP-CU135-12': { mensaje: 'Lo propuso tu usuario: lo tiene que resolver otra persona.' },
  'AP-CU135-13': { mensaje: 'Una persona que ya tiene este rol quedaría con permisos incompatibles. Revocale antes el rol que choca.', campo: 'rol' },
  'AP-CU135-14': { mensaje: 'Otra persona ya resolvió esta solicitud. Actualizá la lista.' },
  // CU-136 (GU-18): cuentas de servicio
  'AP-CU136-01': { mensaje: 'El nombre es obligatorio (hasta 80 caracteres).', campo: 'rol' },
  'AP-CU136-02': { mensaje: 'Escribí el motivo (hasta 160 caracteres): queda en la auditoría.', campo: 'motivo' },
  'AP-CU136-03': { mensaje: 'El vencimiento tiene que ser futuro y no pasar de la vigencia máxima.', campo: 'vigencia' },
  'AP-CU136-04': { mensaje: 'Falta confirmar con tu segundo factor, o el código ya se usó o venció. Ingresá uno nuevo.', campo: 'codigo' },
  'AP-CU136-05': { mensaje: 'Esa cuenta no porta una identidad de servicio vigente (SERVICIO_*).', campo: 'rol' },
  'AP-CU136-06': { mensaje: 'Esa cuenta ya está registrada.', campo: 'rol' },
  'AP-CU136-07': { mensaje: 'Esa cuenta de servicio ya no existe. Actualizá la lista.' },
  'AP-CU136-08': { mensaje: 'Esa cuenta ya está revocada: para volver a usarla hay que registrar otra.' },
  'AP-CU136-09': { mensaje: 'Nadie gobierna su propia cuenta. Lo tiene que hacer otra persona.' },
  'AP-CU136-10': { mensaje: 'Esa cuenta no tiene credencial que rotar.' },
  // CU-134 (lote G): actos sobre un participante y sus medidas
  'AP-CU134-01': { mensaje: 'Escribí por qué lo necesitás (al menos 10 caracteres): queda en el registro de accesos.', campo: 'motivo' },
  'AP-CU134-02': { mensaje: 'Nadie actúa sobre su propia cuenta de participante. Lo tiene que hacer otra persona.' },
  'AP-CU134-03': { mensaje: 'Falta confirmar con tu segundo factor, o el código ya se usó o venció. Ingresá uno nuevo.', campo: 'codigo' },
  'AP-CU134-04': { mensaje: 'Esa causal no está en el catálogo: la mora, una orden de autoridad o una sospecha de lavado van por su propio caso.' },
  'AP-CU134-05': { mensaje: 'Esta causal no admite medida inmediata: primero se escucha el descargo. Desmarcá «aplicar ya».' },
  'AP-CU134-06': { mensaje: 'Ya hay un expediente abierto por esa causal sobre esta cuenta: agregá la evidencia ahí.' },
  'AP-CU134-07': { mensaje: 'Todavía corre el plazo de descargo: no se decide sin escuchar a la persona.' },
  'AP-CU134-08': { mensaje: 'Quien propuso o decidió la medida no puede ser quien la resuelve. Lo tiene que hacer otra persona.' },
  'AP-CU134-09': { mensaje: 'Esa apelación ya no está pendiente. Actualizá la ficha.' },
  'AP-CU134-10': { mensaje: 'Ese expediente ya está cerrado o prescribió. Actualizá la ficha.' },
  'AP-CU134-12': { mensaje: 'Esta medida no llegó a firme: no retiene ninguna entrega.' },
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
