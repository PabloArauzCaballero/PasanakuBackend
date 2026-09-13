import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'colores_de_turno.dart';
import 'pintor_de_rueda.dart';

/// Un pasanaku es una rueda de personas que se turnan el pozo: este widget **es** esa
/// rueda. Un segmento por turno; los ya cobrados van pintados con el color de su
/// persona, los que faltan quedan en línea, y el turno propio lleva un punto.
///
/// Es el mismo objeto a 36 px en una lista y a 236 px en la pantalla del grupo, así que
/// la persona reconoce su rueda antes de leer el nombre. Reemplaza a la barra de
/// progreso genérica: una barra dice «cuánto falta», la rueda dice además «de quién es
/// cada tramo y dónde estoy yo».
class Rueda extends StatelessWidget {
  const Rueda({
    super.key,
    required this.turnos,
    required this.cobrados,
    this.miTurno,
    this.turnoActual,
    this.diametro = 36,
    this.etiqueta,
  }) : assert(turnos > 0, 'una rueda sin turnos no es una rueda');

  /// Cuántas personas dan la vuelta.
  final int turnos;

  /// Cuántos turnos ya cobraron, contados desde el primero.
  final int cobrados;

  /// El turno de quien mira (base 0), si está en esta rueda.
  final int? miTurno;

  /// El turno que se está cobrando ahora (base 0): se pinta a medio tono.
  final int? turnoActual;

  final double diametro;

  /// Qué anuncia un lector de pantalla. Si se omite, se arma una frase con los datos.
  final String? etiqueta;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: etiqueta ?? _frase,
      excludeSemantics: true,
      child: SizedBox.square(
        dimension: diametro,
        child: CustomPaint(
          painter: PintorDeRueda(
            turnos: turnos,
            cobrados: cobrados,
            miTurno: miTurno,
            turnoActual: turnoActual,
            colorDeTurno: ColoresDeTurno.de,
            colorPendiente: t.border,
            colorDelPunto: t.text,
            colorDelBorde: t.bg,
          ),
        ),
      ),
    );
  }

  String get _frase {
    final vuelta = 'Rueda de $turnos turnos, $cobrados ya cobrados';
    return miTurno == null ? '$vuelta.' : '$vuelta. Tu turno es el ${miTurno! + 1}.';
  }
}
