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
    actualizar: 'Actualizar saldo',
    saldoDe: 'Cuenta',
    otraCuenta: 'Consultar otra cuenta',
    cuentaEtiqueta: 'Identificador de la cuenta de billetera',
    cuentaAyuda: 'Tiene la forma 8-4-4-4-12. Lo trae el reclamo o la ficha del titular.',
    cuentaError: 'Ese identificador no tiene el formato de una cuenta de billetera.',
    consultar: 'Consultar saldo',
  },
  solicitudesEscaladas: {
    titulo: 'Solicitudes de ingreso escaladas',
    proposito: 'El organizador dejó vencer las 48 horas para resolver una solicitud de ingreso: le toca a operación.',
    sinContrato: 'Todavía no hay una cola de solicitudes escaladas.',
    sinContratoExplicacion:
      'El servicio de grupos no publica la operación que lista las solicitudes vencidas, así que esta pantalla no muestra datos de ejemplo ni botones que no envían nada. Las solicitudes se resuelven en Admisiones, una por una, con su identificador (lo trae el aviso de vencimiento).',
    abrirTitulo: 'Resolver una solicitud en Admisiones',
    solicitudEtiqueta: 'Identificador de la solicitud',
    solicitudAyuda: 'Tiene la forma 8-4-4-4-12 (por ejemplo, 9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10).',
    solicitudError: 'Ese identificador no tiene el formato de una solicitud de ingreso.',
    abrir: 'Abrir en Admisiones',
    sinAdmisiones: 'Resolver solicitudes de ingreso es de quien tiene acceso a Admisiones. Pedí ese acceso o derivá la solicitud.',
  },
} as const
