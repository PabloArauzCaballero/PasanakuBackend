import 'package:aportaya_diseno/errores.dart';

import '../../dominio/errores.dart';

/// Los 422 de `firmarReglamentoDelGrupo` (`grupos.yaml`, 2026-10-30) dichos para quien
/// firma. El catálogo general (`dominio/errores.dart`) traduce `AP-CU69-05` como la
/// invitación vencida (`unirsePorEnlace`); en la firma el mismo código es
/// `REGLAMENTO_CAMBIO`, así que acá manda este texto.
const reglamentoCambio = 'AP-CU69-05';

const Map<String, String> _porCodigo = {
  reglamentoCambio:
      'El reglamento cambió mientras lo leías. Te mostramos la versión '
      'vigente: leela y volvé a firmar.',
  'AP-CU69-13':
      'No tenés nada que firmar en este grupo: tu firma ya está registrada.',
};

/// ¿El servidor dice que el texto firmado ya no es el vigente?
bool esReglamentoCambiado(Object error) =>
    error is ErrorDeApi && error.codigo == reglamentoCambio;

/// El texto para la persona cuando la firma no se pudo guardar.
String mensajeDeFirma(Object error) {
  if (error is ErrorDeApi) {
    final propio = _porCodigo[error.codigo];
    if (propio != null) return propio;
  }
  return error is ErrorPresentable
      ? error.mensaje
      : 'No pudimos guardar tu firma. Probá de nuevo.';
}
