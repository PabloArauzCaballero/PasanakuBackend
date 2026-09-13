import 'package:aportaya_diseno/ilustraciones/cuentas_a_la_vista.dart';
import 'package:aportaya_diseno/ilustraciones/lienzo.dart';
import 'package:aportaya_diseno/ilustraciones/plata_en_custodia.dart';
import 'package:aportaya_diseno/ilustraciones/rueda_de_gente.dart';
import 'package:aportaya_diseno/ilustraciones/sorteo_limpio.dart';
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
          Center(child: ilustracion),
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
/// Cada una tiene su propio dibujo, hecho para ella: la rueda de gente, el cuaderno
/// abierto con la lupa, la ruleta con su sello de verificación, y el banco con la
/// plata aparte de la empresa. Son vectores —nítidos a cualquier tamaño, con los
/// colores del tema— y no imágenes importadas.
const List<Widget> laminasDelTour = [
  Lamina(
    ilustracion: Ilustracion(
      pintor: RuedaDeGente.new,
      etiqueta: 'Ocho personas alrededor de una rueda, con el pozo en el centro',
    ),
    titulo: TextosIdentidad.tour1Titulo,
    texto: TextosIdentidad.tour1Texto,
  ),
  Lamina(
    ilustracion: Ilustracion(
      pintor: CuentasALaVista.new,
      etiqueta: 'Una lista de quién puso y quién cobró, con una lupa encima',
    ),
    titulo: TextosIdentidad.tour2Titulo,
    texto: TextosIdentidad.tour2Texto,
  ),
  Lamina(
    ilustracion: Ilustracion(
      pintor: SorteoLimpio.new,
      etiqueta: 'Una ruleta con la aguja en el turno sorteado y un sello de verificación',
    ),
    titulo: TextosIdentidad.tour3Titulo,
    texto: TextosIdentidad.tour3Texto,
  ),
  Lamina(
    ilustracion: Ilustracion(
      pintor: PlataEnCustodia.new,
      etiqueta: 'Un banco con tu plata, separado del edificio vacío de la empresa',
    ),
    titulo: TextosIdentidad.tour4Titulo,
    texto: TextosIdentidad.tour4Texto,
  ),
];
