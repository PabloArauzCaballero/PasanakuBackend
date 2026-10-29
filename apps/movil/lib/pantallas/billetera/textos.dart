/// Los textos del dominio billetera, en voz de marca. Un archivo por dominio: no hay
/// archivo global de textos (conflicto nº 3 de planes/16).
abstract final class TextosBilletera {
  static const titulo = 'Tu billetera';
  static const disponible = 'Disponible';
  static const enPasanakus = 'En pasanakus';
  static const tusPasanakus = 'Tus pasanakus';
  // Estados de una operación de dinero: dónde está, sin decir que ya pasó (regla 91.3).
  static const montoARecargar = 'Monto a recargar';
  static const montoARetirar = 'Monto a retirar';
  static const montoATransferir = 'Monto a transferir';
  static const pasoPedida = 'Pedida';
  static const pasoConfirmaElBanco = 'Confirma el banco';
  static const pasoAcreditada = 'Acreditada';
  static const pasoInstruido = 'Instruido';
  static const pasoEnCamino = 'En camino al banco';
  static const pasoAcreditadoEnTuCuenta = 'En tu cuenta';
  static const tituloSinPasanakus = 'Armá tu primer pasanaku';
  static const crearUnGrupo = 'Crear un grupo';
  static const entrarConEnlace = 'Entrar con un enlace o QR';
  static const verTodos = 'Ver todos';
  static const todos = 'Todos';
  static const sinPasanakus =
      'Todavía no estás en ningún pasanaku. Cuando entres a uno, lo vas a ver acá.';
  static const cargandoPasanakus = 'Cargando tus pasanakus';
  static const avisoFirmar = 'Te falta firmar el reglamento';
  static const avisoAtrasado = 'Tenés un aporte atrasado';
  static const avisoSuspendido = 'Tu participación está suspendida';
  static const verMisGrupos = 'Ver mis grupos';
  static const unirmeAUnGrupo = 'Unirme a un grupo';
  static String todoAlDia(int n) => n == 1
      ? 'Estás en 1 grupo · todo al día'
      : 'Estás en $n grupos · todo al día';
  static const cargandoSaldo = 'Cargando tu saldo';
  static const buscandoBilletera = 'Buscando tu billetera';
  static const billeteraInactiva =
      'Tu billetera todavía no está activa. Cuando se active, tu saldo aparece acá.';
  static const revisarDeNuevo = 'Volver a revisar';
  static const saldoDisponible = 'Saldo disponible';
  static const saldoRetenido = 'Retenido';
  static const nadaTrabado = 'Nada trabado';
  // Sin nombre de banco: ninguna fuente del repositorio (docs/, contratos, semillas) dice en qué
  // entidad queda la cuenta de custodia —solo «una entidad supervisada por ASFI»—; el «Banco Unión»
  // venía de datos de ejemplo de la maqueta. Cuando producto confirme la entidad, se agrega acá.
  static const custodia = 'En custodia, separado del dinero de la empresa';

  static const verAportesPendientes = 'Ver aportes pendientes';
  static const movimientos = 'Movimientos';
  static const sinMovimientos =
      'Todavía no tenés movimientos. Cuando recargues saldo van a aparecer acá.';
  static const alCorteDe = 'Actualizado';
  static const cargandoMovimientos = 'Cargando movimientos';
  static const errorMovimientos = 'No pudimos cargar tus movimientos.';
  static const reintentar = 'Reintentar';
  static const recargar = 'Recargar';
  static const aportar = 'Aportar';
  static const retirar = 'Retirar';
  static const vales = 'Vales';
  static const valesNoDisponible =
      'Los vales de alianzas todavía no están disponibles en la app.';
  static const transferir = 'Transferir';
  static const extracto = 'Extracto';
  static const monto = 'Monto';
  static const recargaPendiente =
      'Tu recarga quedó en camino. Te avisamos cuando el banco la confirme.';
  static const retiroInstruido =
      'Tu retiro quedó instruido. El saldo retenido se libera cuando el banco confirma el pago.';
  static const transferenciaHecha = 'Transferencia hecha';
  static const cuentaDestino = 'Cuenta de destino';
  static const alias = 'Alias';
  static const concepto = 'Concepto';
  static const enviarConMfa = 'Confirmar con tu segundo factor';
  static const factorMfa = 'Código de tu segundo factor';
  static const motivoRecarga = 'Escribí cuánto querés recargar.';
  static const ayudaCuentaDestino = 'La cuenta donde vas a recibir la plata.';
  static const ayudaFactorMfa = 'Son 6 números.';
  static String? motivoRetiro({
    required bool hayMonto,
    required bool hayCuenta,
    required bool factorCompleto,
  }) => !hayMonto
      ? 'Escribí cuánto querés retirar.'
      : !hayCuenta
      ? 'Indicá la cuenta de destino.'
      : !factorCompleto
      ? 'Escribí los 6 números de tu segundo factor.'
      : null;
  static const desde = 'Desde';
  static const hasta = 'Hasta';
  static const generarExtracto = 'Generar extracto';
  static const sinExtracto = 'Todavía no generaste ningún extracto.';
  static const elegiUnPeriodo = 'Elegí un período';
  static const sinExtractoDetalle =
      'Generá un extracto para ver tu saldo al cierre y cuántos movimientos tuviste en esas fechas.';
  static const elegirOtroPeriodo = 'Elegir otro período';
  static const periodo = 'Período';
  static const filtroTodos = 'Todos';
  static const filtroEntradas = 'Entradas';
  static const filtroSalidas = 'Salidas';
  static const detalleDelPeriodo = 'Detalle del período';
  static const sinMovimientosConEseFiltro =
      'No hay movimientos de este tipo en el período. Probá con «Todos».';
  static const lecturaCorta =
      'Mostramos tus últimos 100 movimientos; los más viejos del período pueden no aparecer.';
  static const compartirCertificado = 'Compartir certificado de saldo';
  static const noSePudoCompartir =
      'No pudimos abrir otra app para compartirlo.';
  static String certificado(String periodo, String moneda, String saldo) =>
      'Certificado de saldo AportaYa\nPeríodo: $periodo\nSaldo al cierre: $moneda $saldo';
  static const saldoFinalDelPeriodo = 'Saldo al cierre del período';
  static const movimientosDelPeriodo = 'Movimientos en el período';
}
