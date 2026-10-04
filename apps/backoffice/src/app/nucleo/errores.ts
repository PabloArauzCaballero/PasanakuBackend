/**
 * Traducción de `AP-CU<NN>-<nn>` a lenguaje humano. Un `403` responde «no tenés acceso»
 * y nada más: detallarlo confirma que el recurso existe.
 */
export type ErrorTraducido = { mensaje: string; trazaId?: string; codigo?: string; estado?: number; sinConexion?: boolean }

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
