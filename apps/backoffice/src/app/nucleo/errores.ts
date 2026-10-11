/**
 * Traducción de `AP-CU<NN>-<nn>` a lenguaje humano. Un `403` responde «no tenés acceso»
 * y nada más: detallarlo confirma que el recurso existe.
 */
export type ErrorTraducido = { mensaje: string; trazaId?: string; codigo?: string; estado?: number; sinConexion?: boolean; tiempoAgotado?: boolean }

const CATALOGO: Readonly<Record<string, string>> = {
  // CU-01 flujo 4b · los controles del servidor al decidir un expediente
  // (identidad.yaml, POST /identidad/verificaciones/{id}/decision).
  'AP-CU01-08': 'Ese expediente ya fue resuelto por otra persona. Actualizá la cola.',
  'AP-CU01-09': 'Faltan fotos. No se aprueba un expediente que no se puede mirar entero.',
  'AP-CU01-10': 'El documento no tiene fecha de vencimiento: no se puede comprobar que esté vigente.',
  'AP-CU01-11': 'El documento está vencido. No se aprueba con un documento vencido.',
  'AP-CU04-01': 'El teléfono o la contraseña no coinciden.',
  'AP-CU04-02': 'La cuenta está bloqueada por intentos fallidos. Esperá o pedí que la desbloqueen.',
  'AP-CU04-03': 'Ingresá el código de verificación para terminar de entrar.',
  // El backend usa el mismo código para un código equivocado y uno vencido (CU04Autenticar).
  'AP-CU04-04': 'Ese código no sirve: revisalo o pedí uno nuevo.',
  'AP-CU04-05': 'Demasiados intentos. Esperá unos minutos antes de volver a probar.',
  'AP-CU04-07': 'Ese segundo factor no está permitido para operadores.',
  'AP-CU04-06': 'Tu segundo factor no está enrolado. Pedí el enrolamiento a otra persona con permiso.',
  // CU-68 · admisión y sustitución de administrador. Algunos números los reutilizan operaciones distintas del
  // servidor (06, 08, 09 y 10): el texto cubre ambos sentidos y dice qué hacer en cada uno.
  'AP-CU68-01': 'La persona tiene una restricción vigente y no puede entrar a un grupo hasta que se levante.',
  'AP-CU68-02': 'La persona necesita elevar su nivel de verificación antes de entrar a este grupo.',
  'AP-CU68-03': 'La reputación de la persona todavía no alcanza para este grupo.',
  'AP-CU68-04': 'El grupo ya no tiene cupos libres. Revisá el estado del grupo antes de decidir.',
  'AP-CU68-05': 'No hay un criterio de emparejamiento vigente o el grupo concentra demasiado riesgo. Pedí a Cumplimiento que lo revise.',
  'AP-CU68-06': 'La solicitud cambió, ya fue resuelta o no tenés permiso para decidirla. Actualizá el expediente y revisá la última decisión.',
  'AP-CU68-07': 'La propuesta venció o ya no existe. Actualizá el expediente y pedí una propuesta nueva al administrador.',
  'AP-CU68-08': 'No encontramos esa solicitud o tu rol no puede hacer esta operación. Revisá el identificador y tus permisos.',
  'AP-CU68-09': 'Esa solicitud ya fue resuelta, o la clave de la operación pertenece a otra decisión. Actualizá el expediente antes de repetir.',
  'AP-CU68-10': 'Falta el motivo de la decisión, o el grupo no tiene exactamente un administrador vigente. Completá el motivo y revisá el grupo.',
  'AP-CU68-11': 'La persona que quedaría como administradora no es participante activa del grupo. Elegí a otra.',
  'AP-CU68-12': 'Esa persona ya es la administradora del grupo: no hay nada que sustituir.',
  'AP-CU68-13': 'Quien decide la sustitución no puede ser quien queda como administrador. Lo tiene que resolver otra persona.',
  'AP-CU68-14': 'La persona elegida no está habilitada como organizadora. Habilitala antes de designarla.',
  'AP-CU68-15': 'Este grupo exige admisión humana: la decisión del organizador es una propuesta y la resuelve backoffice.',
  'AP-CU69-05': 'Esa invitación ya no es válida.',
  'AP-CU11-10': 'No podés resolver un retiro que pediste vos. Lo tiene que resolver otra persona con permiso.',
  'AP-CU14-02': 'Quien autoriza un reverso no puede ejecutarlo. Es otra persona.',
  'AP-CU52-03': 'Un reclamo favorable no se cierra sin registrar la reparación.',
}

export function mensajeDe(codigo: string | undefined, estado: number): string {
  if (codigo && CATALOGO[codigo]) return CATALOGO[codigo]
  switch (estado) {
    case 401: return 'Tu sesión venció. Volvé a ingresar.'
    case 403: return 'No tenés acceso a esto.'
    case 404: return 'No encontramos lo que buscabas.'
    case 409: return 'Esta operación ya se hizo o cambió mientras tanto. Revisá el estado antes de repetirla.'
    case 0: return 'No hay conexión. Te mostramos lo último que vimos; para operar hace falta señal.'
    default: return 'Algo salió mal de nuestro lado. Probá de nuevo en un momento.'
  }
}

/** Vencido el plazo de espera: para una escritura NO se sabe si llegó, y el texto lo dice. */
export function mensajeDeTiempoAgotado(escritura: boolean): string {
  return escritura
    ? 'La respuesta tardó demasiado y no sabemos si la operación quedó registrada. Antes de repetirla, revisá su estado: si la repetís igual, no se duplica.'
    : 'La respuesta tardó demasiado. Probá de nuevo en un momento.'
}
