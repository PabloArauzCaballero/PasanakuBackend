import '../../../dominio/cliente.dart';
import '../../../dominio/errores.dart';

/// Nunca expone el mensaje crudo del servicio ni ofrece reintento inútil.
class FalloConsultaInvitacion {
  const FalloConsultaInvitacion({
    this.mensaje,
    this.requiereIngreso = false,
    this.recuperable = true,
  });

  final String? mensaje;
  final bool requiereIngreso;
  final bool recuperable;

  factory FalloConsultaInvitacion.desde(Object error) {
    final causa = errorDeDominio(error);
    if (causa is ErrorDeApi) {
      if (causa.estado == 401) {
        return const FalloConsultaInvitacion(requiereIngreso: true);
      }
      if (causa.codigo == 'AP-CU69-05') {
        return const FalloConsultaInvitacion(
          mensaje: 'Esta invitación ya no es válida. Pedí un enlace nuevo.',
          recuperable: false,
        );
      }
      if (causa.estado == 403) {
        return const FalloConsultaInvitacion(
          mensaje:
              'No tenés acceso a esta invitación. Pedí ayuda a quien te invitó.',
          recuperable: false,
        );
      }
    }
    return const FalloConsultaInvitacion(
      mensaje: 'No pudimos verificar la invitación. Probá de nuevo.',
    );
  }
}
