import 'package:aportaya_diseno/moviles/hoja_de_confirmacion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// "Antes de escanear" de Atlas: se muestra una vez por visita al paso, antes del
/// anverso o el reverso del carnet. "Abrir el escáner" solo actúa si la hoja
/// terminó de cerrarse con esa opción — cualquier otro cierre es "ahora no".
Future<bool> mostrarHojaAntesDeEscanear(BuildContext context) async {
  Future<void>? cierreCompleto;
  final abrir = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (hoja) {
      cierreCompleto = ModalRoute.of(hoja)!.completed;
      return const _HojaAntesDeEscanear();
    },
  );
  if (abrir == true) await cierreCompleto;
  return abrir ?? false;
}

class _HojaAntesDeEscanear extends StatelessWidget {
  const _HojaAntesDeEscanear();

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final enIos = Theme.of(context).platform == TargetPlatform.iOS;
    return HojaDeConfirmacion(
      titulo: 'Antes de escanear',
      detalle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Poné el carnet sobre una mesa lisa y de color oscuro, con buena '
            'luz y sin reflejos. El recuadro lo encuentra solo y dispara '
            'cuando lo tiene entero.',
            style: Tipo.cuerpo.copyWith(color: t.text),
          ),
          if (enIos) ...[
            const SizedBox(height: Espacio.s3),
            Text(
              'Si arriba ves «Filtros», dejá «Color» o «Foto». En gris o en '
              'blanco y negro se pierden detalles que necesitamos para '
              'verificar tu carnet.',
              style: Tipo.cuerpo.copyWith(color: t.text),
            ),
          ],
        ],
      ),
      textoConfirmar: 'Abrir el escáner',
      onConfirmar: () => Navigator.of(context).pop(true),
      onCancelar: () => Navigator.of(context).pop(false),
    );
  }
}
