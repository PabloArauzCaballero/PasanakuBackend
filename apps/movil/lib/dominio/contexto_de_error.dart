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
}

/// El texto de un rechazo que depende de dónde ocurrió, o `null` si no hay uno propio.
String? mensajePorContexto(int? estado, ContextoErrorApi contexto) {
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
