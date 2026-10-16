/** Los textos del dominio operación, en voz de marca. Un archivo por dominio. */
export const textosOperacion = {
  billetera: {
    titulo: 'Billetera del titular',
    proposito: 'Ver el saldo real de una cuenta para atender una consulta o un reclamo, sin tocar nada.',
    cargando: 'Cargando el saldo de la cuenta',
    alcance: 'Este es solo el saldo de la billetera personal. El dinero de un grupo (pozos y aportes) y lo invertido no forman parte de lo gastable: lo comprometido en pozos se muestra aparte, y la posición de inversiones vive en su propia pantalla.',
    incoherente: 'No mostramos este saldo porque sus cifras no se pueden explicar (importes fuera de contrato, negativos o en monedas distintas). Es un problema de datos, no de la cuenta: reportalo con el código de seguimiento.',
    alCorteDe: 'Al corte de',
    enCero: 'La cuenta está en cero. No es un error de consulta: el titular todavía no recargó, o retiró todo.',
  },
  solicitudesEscaladas: {
    titulo: 'Solicitudes de ingreso escaladas',
    proposito: 'El organizador dejó vencer las 48 horas para resolver: la solicitud pasa a operación con el puntaje descompuesto.',
    cargando: 'Cargando la cola de solicitudes escaladas',
    sinSolicitudes: 'No hay solicitudes escaladas pendientes.',
    plazoOrganizador: 'Vencía para el organizador',
    escaladaEl: 'Escalada el',
    aFavor: 'A favor',
    enContra: 'En contra',
    aceptar: 'Aceptar solicitud',
    rechazar: 'Rechazar solicitud',
    motivoDeRechazo: 'Motivo del rechazo (obligatorio)',
    confirmarAceptar: (nombre: string, grupo: string) => `Vas a dar el cupo a ${nombre} en el grupo ${grupo}. Recién ahí se genera su primera obligación.`,
    confirmarRechazar: (nombre: string, grupo: string) => `Vas a rechazar a ${nombre} en el grupo ${grupo}. El motivo escrito viaja tal cual al postulante.`,
    faltaMotivo: 'Escribí el motivo antes de confirmar el rechazo.',
    sinEnviar: 'Esta decisión todavía NO se envió: el servicio de grupos no publica la operación para resolver desde esta cola. Para resolver la solicitud usá Admisiones.',
  },
} as const
