import 'package:aportaya_diseno/atomos/rueda.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// Una lámina del tour: una ilustración, un título y un párrafo. Una idea por lámina
/// y nada más — si hay que explicar dos cosas, son dos láminas.
class Lamina extends StatelessWidget {
  const Lamina({
    super.key,
    required this.ilustracion,
    required this.titulo,
    required this.texto,
  });

  final Widget ilustracion;
  final String titulo;
  final String texto;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(height: 150, child: Center(child: ilustracion)),
          const SizedBox(height: Espacio.s6),
          Semantics(
            header: true,
            child: Text(titulo, style: Tipo.titulo1.copyWith(color: t.text)),
          ),
          const SizedBox(height: Espacio.s3),
          Text(texto, style: Tipo.cuerpo.copyWith(color: t.text2)),
        ],
      ),
    );
  }
}

/// Las cuatro láminas, en el orden en que alguien se hace las preguntas: **qué es
/// esto**, **qué voy a ver**, **cómo se decide quién cobra primero** y **dónde está
/// mi plata**. La última es la que más pesa y por eso va al final, cuando ya hay
/// suficiente contexto para que signifique algo.
///
/// La rueda protagoniza las tres primeras y va cambiando de estado: vacía, con turnos
/// cobrados, y con el turno propio marcado. No es decoración repetida — es la misma
/// rueda contando su historia.
List<Widget> laminasDelTour(Tokens t) => [
  const Lamina(
    ilustracion: Rueda(
      turnos: 10,
      cobrados: 0,
      diametro: 136,
      etiqueta: 'Una rueda de diez turnos, recién empezada',
    ),
    titulo: TextosIdentidad.tour1Titulo,
    texto: TextosIdentidad.tour1Texto,
  ),
  const Lamina(
    ilustracion: Rueda(
      turnos: 10,
      cobrados: 4,
      miTurno: 6,
      diametro: 136,
      etiqueta: 'La misma rueda: cuatro ya cobraron, tu turno es el sexto',
    ),
    titulo: TextosIdentidad.tour2Titulo,
    texto: TextosIdentidad.tour2Texto,
  ),
  const Lamina(
    ilustracion: Rueda(
      turnos: 10,
      cobrados: 4,
      miTurno: 6,
      turnoActual: 4,
      diametro: 136,
      etiqueta: 'El turno que se está cobrando ahora, sorteado',
    ),
    titulo: TextosIdentidad.tour3Titulo,
    texto: TextosIdentidad.tour3Texto,
  ),
  Lamina(
    ilustracion: Icon(
      Icons.account_balance_outlined,
      size: 104,
      color: t.brandTexto,
    ),
    titulo: TextosIdentidad.tour4Titulo,
    texto: TextosIdentidad.tour4Texto,
  ),
];
