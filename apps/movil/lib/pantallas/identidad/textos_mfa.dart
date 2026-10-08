/// Copy del desafío de segundo factor y de sus rutas sin un desafío pendiente.
class TextosMfa {
  const TextosMfa._();

  static const titulo = 'Verificación en dos pasos';
  static const sinDesafio = 'No hay un código pendiente';
  static const sinDesafioDetalle =
      'Ingresá con tu teléfono y contraseña para iniciar la verificación.';
  static const yaAutenticadaDetalle =
      'Tu sesión ya está abierta. Podés continuar al inicio.';
  static const volverAIngresar = 'Ir a ingresar';
  static const irAlInicio = 'Ir al inicio';
  static const verificando = 'Verificando código';
  static const codigoDeSeisDigitos = 'Código de 6 dígitos';
}
