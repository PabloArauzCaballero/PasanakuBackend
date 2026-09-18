import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'cu01_registrar.dart';
import 'estado_alta.dart';
import 'estado_contrato.dart';
import 'estado_sesion.dart' show mensajeDeError;
import 'subida_del_expediente.dart';

/// El cierre del alta: `POST /usuarios` con todo junto.
///
/// Vive aparte de [AltaNotifier] porque son dos cosas distintas. El notifier sabe en
/// qué paso está el formulario; esto sabe cómo se cierra —qué se manda, en qué orden,
/// y qué pasa si una foto falla—. Juntarlos hacía de `estado_alta.dart` un archivo que
/// crecía por los dos lados a la vez.
///
/// **Los ocho pasos juntan datos y recién al final se crea la persona.** Antes de este
/// llamado no existe nada del lado del servidor, que es lo que permite abandonar el
/// alta a mitad sin dejar una cuenta a medio hacer.
Future<ResultadoDelAlta> enviarElAlta(Ref ref, EstadoAlta estado) async {
  final d = estado.datos;
  final nacimiento = d.fechaNacimiento;
  if (nacimiento == null) {
    return const ResultadoDelAlta(error: 'Falta tu fecha de nacimiento.');
  }
  final contratos = ref.read(contratoProvider).aceptados;
  if (contratos.isEmpty) {
    // No es una validación de forma: sin los ids de los contratos vigentes el
    // servidor rechaza el alta, y decirlo acá evita un viaje y un error genérico.
    return const ResultadoDelAlta(
      error: 'Falta aceptar el contrato para abrir tu cuenta.',
    );
  }
  try {
    final r = await ref
        .read(registroProvider)
        .crear(
          telefonoE164: d.telefono,
          nombres: d.nombres,
          apellidos: d.apellidos,
          fechaNacimiento: nacimiento,
          tipoDocumento: d.tipoDocumento,
          numeroDocumento: d.numeroDocumento,
          lugarExpedicion: d.lugarExpedicion,
          correo: d.correo,
          canalVerificacion: d.canalVerificacion,
          contrasena: estado.contrasena,
          contratosAceptados: contratos,
        );
    // Las tres fotos se sacaron antes de que la persona existiera: recién ahora hay un
    // `usuarioId` al que atarlas. Si una falla, el alta NO se deshace — la cuenta ya
    // está creada y quien revisa en el backoffice ve el expediente incompleto, que es
    // recuperable; perder el alta entera no lo es.
    if (r.usuarioId != null) {
      await subirFotosDelExpediente(ref, estado, r.usuarioId!);
    }
    return ResultadoDelAlta(cuentaBilleteraId: r.cuentaBilleteraId);
  } on Object catch (e) {
    return ResultadoDelAlta(error: mensajeDeError(e));
  }
}

/// Lo que devuelve el cierre: el id de la billetera que el backend abrió, o el error
/// que hay que mostrarle a la persona. Nunca los dos.
class ResultadoDelAlta {
  const ResultadoDelAlta({this.cuentaBilleteraId, this.error});

  /// `null` si el alta quedó pendiente de verificación y todavía no hay billetera.
  final String? cuentaBilleteraId;
  final String? error;
}
