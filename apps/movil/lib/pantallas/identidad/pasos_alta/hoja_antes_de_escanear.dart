import 'package:aportaya_diseno/moviles/hoja_de_confirmacion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos_de_captura.dart';

/// "Antes de escanear" de Atlas: se muestra una vez por visita al paso, antes del
/// anverso o el reverso del carnet. "Abrir el escáner" solo actúa si la hoja
/// terminó de cerrarse con esa opción — cualquier otro cierre es "ahora no".
Future<bool> mostrarHojaAntesDeEscanear(BuildContext context) async {
  final abrir = await HojaDeConfirmacion.mostrar<bool>(
    context,
    hoja: const _HojaAntesDeEscanear(),
  );
  return abrir ?? false;
}

class _HojaAntesDeEscanear extends StatelessWidget {
  const _HojaAntesDeEscanear();

  static const _iconos = [
    Icons.table_restaurant_outlined,
    Icons.wb_sunny_outlined,
    Icons.flare_outlined,
  ];

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    const requisitos = TextosDeCaptura.antesDeEscanearRequisitos;
    return HojaDeConfirmacion(
      titulo: TextosDeCaptura.antesDeEscanearTitulo,
      // Tres requisitos, uno por fila y con su ícono: se leen de un vistazo. En una
      // sola línea con puntos medios se leían como una frase y se salteaban.
      detalle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (var i = 0; i < requisitos.length; i++)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: Espacio.s1),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(Espacio.s2),
                    decoration: BoxDecoration(
                      color: t.brandBg,
                      borderRadius: BorderRadius.circular(Radios.sm),
                    ),
                    child: Icon(
                      _iconos[i],
                      size: Espacio.s5 - Espacio.s1,
                      color: t.brandTexto,
                    ),
                  ),
                  const SizedBox(width: Espacio.s3),
                  Expanded(
                    child: Text(
                      requisitos[i],
                      style: Tipo.cuerpoFuerte.copyWith(color: t.text),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
      textoConfirmar: TextosDeCaptura.abrirElEscaner,
      onConfirmar: () => Navigator.of(context).pop(true),
      onCancelar: () => Navigator.of(context).pop(false),
    );
  }
}
