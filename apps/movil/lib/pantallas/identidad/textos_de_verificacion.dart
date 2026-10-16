/// Los textos de la pantalla de estado de la verificación (CU-02), uno a uno
/// iguales a los de Atlas: pendiente, en revisión, aprobada o rechazada.
class TextosDeVerificacion {
  TextosDeVerificacion._();

  static const pendienteTitulo = 'Estamos revisando tu documento';
  static const pendienteDetalle = 'No cierres la app.';
  static const enRevisionTitulo = 'Lo está revisando una persona';
  static const enRevisionDetalle = 'No tenemos un plazo fijo.';
  static const cuentaEnVerificacionTitulo =
      'Tu cuenta está en proceso de verificación';
  static const cuentaEnVerificacionDetalle =
      'La revisión de tu identidad sigue en curso. Tu sesión permanece activa; '
      'el acceso a las funciones protegidas se habilitará si tu identidad es aprobada.';
  static const accesoAunNoHabilitado =
      'Tu identidad figura verificada, pero el acceso todavía no se actualizó. '
      'Cierra sesión y vuelve a ingresar para renovar el acceso.';
  static const aprobadaTitulo = 'Identidad verificada';
  static const rechazadaTitulo = 'No pudimos validar tu documento';
  static const intentarDeNuevo = 'Intentar de nuevo';
  static const seguirConElRegistro = 'Seguir con el registro';
  static const entrar = 'Entrar';
  static const actualizarEstado = 'Actualizar estado';
  static const salirDeCuenta = 'Salir de esta cuenta';
  static const actualizarAcceso = 'Cerrar sesión para actualizar el acceso';
  static String caso(String verificacionId) =>
      'Caso ${verificacionId.length > 8 ? verificacionId.substring(0, 8) : verificacionId}';
}

/// Los motivos de rechazo que el motor devuelve, traducidos — mismo catálogo que
/// Atlas.
String motivoDeRechazo(String? codigo) => switch (codigo) {
  'DOCUMENTO_NO_VALIDO' => 'El documento no se pudo validar.',
  'IDENTIDAD_NO_COINCIDE' => 'La cara no coincide con el documento.',
  'SOSPECHA_DE_FRAUDE' => 'El documento quedó marcado para revisión.',
  'REQUIERE_REVISION' => 'Necesita una revisión manual.',
  null => 'No pudimos confirmar el motivo.',
  _ => codigo,
};
