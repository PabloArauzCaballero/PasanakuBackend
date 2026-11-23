/// Textos de los errores que el backend cataloga desde 2026-10-15: el dato que ya
/// tiene cuenta en CU-01 y las fallas de la base sin regla (`AP-BD-0n`). Aparte de
/// `errores.dart` para que cada archivo siga legible de un vistazo.
library;

/// `AP-CU01-03` es uno solo («la cuenta ya existe») y el backend dice en
/// `detalle.campo` CUÁL dato ya tiene cuenta. Sin esto, un correo repetido se leía
/// como «ya hay una cuenta con ese celular o ese documento» y la persona no tenía
/// cómo corregirlo (2026-10-15).
const Map<String, String> cuentaYaExistePorCampo = {
  'telefono': 'Ese celular ya tiene una cuenta. Si es tuyo, iniciá sesión.',
  'documento':
      'Ese documento ya tiene una cuenta. Si es tuyo, iniciá sesión o recuperá el acceso.',
  'correo':
      'Ese correo ya tiene una cuenta. Volvé atrás y usá otro, o iniciá sesión si es tuyo.',
};

/// Lo que rechaza la base sin una regla de negocio detrás (`AP-BD-0n`, ver
/// `FallaDeLaBase` en `comun-web`). Son códigos estables: el cliente distingue un
/// duplicado de una caída, que antes salían iguales como 500.
const Map<String, String> fallasDeLaBase = {
  'AP-BD-01': 'Ya hay un registro con esos datos. Revisalos antes de seguir.',
  'AP-BD-02':
      'Algo de lo que elegiste ya no existe o cambió. Actualizá y probá de nuevo.',
  'AP-BD-03': 'Uno de los datos no es válido. Revisalos antes de seguir.',
  'AP-BD-04': 'Se cruza con otro registro vigente. Revisá las fechas.',
  'AP-BD-05': 'Alguien cambió esto al mismo tiempo. Probá de nuevo.',
  'AP-BD-06': 'Tardó más de lo normal. Probá de nuevo en un momento.',
};

/// `detalle.campo` del cuerpo de error, si viene y es texto.
String? campoDelDetalle(Object? detalle) => switch (detalle) {
  {'campo': final String campo} => campo,
  _ => null,
};

/// CU-21 · pagar la cuota con saldo (F1.L1.M11). El monto y el canal los fija el
/// servidor: ninguno de estos textos le pide a la persona que los corrija.
const Map<String, String> pagoDeCuotaConSaldo = {
  'AP-CU21-01':
      'Esta cuota ya no se puede pagar: puede que ya esté pagada. Revisá tu estado.',
  'AP-CU21-02':
      'El monto de la cuota cambió. Volvé a abrir la cuota para ver lo que queda pendiente.',
  'AP-CU21-03':
      'No te alcanza el saldo para esta cuota. Recargá tu billetera e intentá de nuevo.',
  'AP-CU21-04': 'El período de esta cuota ya cerró y no admite pagos.',
  'AP-CU21-06': 'Esta cuota no es tuya.',
  'AP-CU21-07':
      'No pudimos confirmar la cuota con el grupo. Esperá un momento y probá de nuevo.',
};
