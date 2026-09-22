/**
 * Traducción de `AP-CU<NN>-<nn>` a lenguaje humano. Un `403` responde «no tenés acceso»
 * y nada más: detallarlo confirma que el recurso existe.
 */
export type ErrorTraducido = { mensaje: string; trazaId?: string; codigo?: string; estado?: number; sinConexion?: boolean }

const CATALOGO: Readonly<Record<string, string>> = {
  'AP-CU04-01': 'El teléfono o la contraseña no coinciden.',
  'AP-CU04-02': 'La cuenta está bloqueada por intentos fallidos. Esperá o pedí que la desbloqueen.',
  'AP-CU04-03': 'Ingresá el código de verificación para terminar de entrar.',
  'AP-CU04-04': 'El código venció. Pedí uno nuevo.',
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
