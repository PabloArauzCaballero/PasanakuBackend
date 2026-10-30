/// Algunos códigos CU-01 se reutilizan por contrato en operaciones distintas.
/// El endpoint permite traducirlos sin mostrar a la persona un error de otro flujo.
enum ContextoErrorApi {
  general,
  registro,
  verificacionCorreo,

  /// `POST /grupos`: el 403 significa que la cuenta todavía no es organizador.
  crearGrupo,

  /// `GET /usuarios/{id}/verificacion`: el 403 llega mientras la cuenta recién
  /// creada todavía no tiene sesión.
  estadoDeVerificacion,

  /// `POST /recargas-de-prueba/{ordenId}/pago`: el 422 es una orden que no se pudo
  /// confirmar (vencida, ya cerrada o sin QR de prueba).
  pagoDeRecargaDePrueba,
}

/// El texto de un rechazo que depende de dónde ocurrió, o `null` si no hay uno propio.
String? mensajePorContexto(int? estado, ContextoErrorApi contexto) {
  if (contexto == ContextoErrorApi.pagoDeRecargaDePrueba && estado == 422) {
    return 'No pudimos confirmar esta recarga. '
        'Si pasó mucho tiempo, empezá una recarga nueva.';
  }
  if (estado != 403) return null;
  return switch (contexto) {
    ContextoErrorApi.crearGrupo =>
      'Para crear un grupo necesitás estar habilitado como organizador. '
          'Mientras tanto, podés sumarte a un grupo con una invitación.',
    ContextoErrorApi.estadoDeVerificacion =>
      'Tu cuenta está en verificación. Ingresá con tu celular y tu '
          'contraseña para ver cómo avanza.',
    _ => null,
  };
}
