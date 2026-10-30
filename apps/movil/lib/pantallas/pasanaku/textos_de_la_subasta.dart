/// Los textos de la subasta del cupo (CU-64 por subasta), en voseo.
abstract final class TextosDeLaSubasta {
  static const subastarMiCupo = 'Subastar mi cupo';
  static const accesoDetalle =
      'Quien más ofrezca se queda con tu lugar y te paga por él';
  static const cuposEnSubasta = 'Cupos en subasta';
  static const verTodos = 'Ver todos';
  static const ningunoAhora =
      'Ahora no hay cupos en subasta. Cuando alguien ponga el suyo, lo vas a ver acá.';
  static const cargando = 'Cargando los cupos en subasta';
  static const cargandoSubasta = 'Cargando la subasta';

  // Abrir
  static const comoFunciona = 'Cómo funciona';
  static const reglas = [
    'Ponés un precio base y cuánto dura la subasta.',
    'A todos les llega tu cupo y ofrecen con su saldo, que queda retenido.',
    'Al cierre gana la oferta más alta: el cupo y su turno pasan a esa persona.',
    'Te pagamos la oferta ganadora en tu billetera. Tus aportes vencidos siguen siendo tuyos.',
  ];
  static const precioBase = 'Precio base';
  static const ayudaBase = 'Lo mínimo que aceptás por tu cupo';
  static const duracion = 'Cuánto dura';
  static const horas = {24: '24 h', 48: '48 h', 72: '72 h'};
  static const ponerEnSubasta = 'Poner en subasta';
  static const escribiElPrecio = 'Escribí el precio base para continuar.';
  static const confirmarTitulo = '¿Ponés tu cupo en subasta?';
  static const confirmar = 'Sí, subastar';
  static String confirmarDetalle(String base, int horas) =>
      'Precio base $base, durante $horas horas. Mientras nadie ofrezca podés '
      'retirarlo; con ofertas, la subasta sigue hasta el cierre.';
  static const retirar = 'Retirar de la subasta';

  // Estado de una subasta
  static const ofertaMasAlta = 'Oferta más alta';
  static const sinOfertas = 'Todavía nadie ofreció';
  static const aporte = 'Aporte';
  static const cierra = 'Cierra';
  static String ofertas(int n) => n == 1 ? '1 oferta' : '$n ofertas';
  static const estados = {
    'ABIERTA': 'Abierta',
    'ADJUDICADA': 'Cerrada · pasando el cupo',
    'LIQUIDANDO': 'Cerrada · pagando',
    'VENDIDA': 'Vendida',
    'DESIERTA': 'Cerró sin ofertas',
    'CANCELADA': 'Retirada',
    'FALLIDA': 'No se pudo completar',
  };

  // Pujar
  static const tuOferta = 'Tu oferta';
  static String minimo(String m) => 'Mínimo $m';
  static const aceptoReglamento = 'Si gano, entro al grupo con sus reglas';
  static const detalleReglamento =
      'Seguís pagando los aportes del cupo y cobrás en su turno.';
  static const ofrecer = 'Ofrecer';
  static const faltaMonto = 'Escribí cuánto ofrecés para continuar.';
  static const faltaAceptar = 'Aceptá las reglas del grupo para ofrecer.';
  static const ofrecerTitulo = '¿Confirmás tu oferta?';
  static String ofrecerDetalle(String monto) =>
      'Retenemos $monto de tu saldo. Si alguien ofrece más, te lo devolvemos al '
      'instante; si ganás, se le paga a quien sale cuando el cupo ya sea tuyo.';
  static const siOfrecer = 'Sí, ofrecer';
  static const ganando = 'Tu oferta es la más alta';
  static const teSuperaron = 'Te superaron: tu saldo ya volvió a tu billetera';
  static const pendiente =
      'La billetera no respondió. Tocá «Ofrecer» de nuevo: no se retiene dos veces.';
  static const esTuCupo = 'Es tu cupo: seguís la subasta desde acá.';
}
