import 'package:aportaya_diseno/errores.dart';

import '../../dominio/errores.dart';

/// Los 422 nuevos de iniciar y de crear con etapa mínima (`grupos.yaml`, 2026-10-30),
/// en voseo y con qué hacer. El catálogo general (`dominio/errores.dart`) todavía no los
/// tiene; acá se traducen para las dos pantallas que los provocan.
const Map<String, String> _porCodigo = {
  // iniciarGrupo
  'AP-CU69-16':
      'Todavía no se llega al 75 % de cupos ocupados. Sumá a más gente e '
      'intentá de nuevo.',
  'AP-CU69-17': 'Este grupo ya se inició: ya no entra nadie más.',
  'AP-CU69-19':
      'Hay personas con lugar reservado que todavía no firmaron el reglamento. '
      'Cuando firmen, vas a poder iniciar.',
  'AP-CU69-07': 'Solo quien organiza el grupo puede iniciarlo.',
  'AP-CU69-12': 'El grupo todavía no está abierto para recibir gente.',
  // crearGrupo
  'AP-CU20-07':
      'Tu etapa todavía no permite un grupo de este tamaño o de este aporte. '
      'Probá con un aporte menor.',
  'AP-CU20-08': 'Esa etapa no se puede pedir: elegí la tuya o una más baja.',
};

/// El texto para la persona, o `null` si el código no es de este lote (entonces vale
/// el mensaje general de `ErrorPresentable`).
String? mensajeDeEtapaOInicio(Object error) =>
    error is ErrorDeApi ? _porCodigo[error.codigo] : null;

/// El rechazo de iniciar el grupo ya traducido, con la traza para soporte.
({String mensaje, String? traza}) rechazoAlIniciar(Object error) {
  final traza = error is ErrorPresentable ? error.trazaId : null;
  final mensaje =
      mensajeDeEtapaOInicio(error) ??
      (error is ErrorPresentable
          ? error.mensaje
          : 'No pudimos iniciar el grupo. Probá de nuevo en un momento.');
  return (mensaje: mensaje, traza: traza);
}
