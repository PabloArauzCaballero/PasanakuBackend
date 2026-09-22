import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_tamano.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/progreso.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../../dominio/tutoriales/modelo.dart';
import 'textos.dart';
import 'vista_de_tutoriales.dart';

/// Un tutorial en la lista: qué enseña, cuánto lleva y qué botón corresponde.
class TarjetaDeTutorial extends StatelessWidget {
  const TarjetaDeTutorial({
    super.key,
    required this.fila,
    required this.alComenzar,
    required this.alContinuar,
    required this.alReiniciar,
  });

  final TutorialEnLista fila;
  final VoidCallback alComenzar;
  final VoidCallback alContinuar;
  final VoidCallback alReiniciar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final tutorial = fila.tutorial;
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(
                  tutorial.titulo,
                  style: Tipo.titulo3.copyWith(color: t.text),
                ),
              ),
              const SizedBox(width: Espacio.s2),
              // El estado se dice con palabra, no solo con color.
              ChipEstado(
                texto: _textoDeEstado(fila.estado),
                tono: _tonoDe(fila.estado),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s1),
          Text(
            _meta(tutorial),
            style: Tipo.cuerpoChico.copyWith(color: t.text3),
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            tutorial.descripcion,
            style: Tipo.cuerpo.copyWith(color: t.text2),
          ),
          if (fila.requisitosPendientes.isNotEmpty) ...[
            const SizedBox(height: Espacio.s2),
            Text(
              '${TextosSoporte.requisitos}: ${fila.requisitosPendientes.map((r) => r.titulo).join(', ')}',
              style: Tipo.cuerpoChico.copyWith(color: t.avisoTexto),
            ),
          ],
          if (fila.fraccion > 0 &&
              fila.estado != EstadoDeProgreso.completado) ...[
            const SizedBox(height: Espacio.s3),
            Progreso(
              valor: fila.fraccion,
              etiqueta: 'Avance de ${tutorial.titulo}',
            ),
          ],
          const SizedBox(height: Espacio.s3),
          Row(
            children: [
              Expanded(
                child: Boton(
                  texto: fila.continuable
                      ? TextosSoporte.continuar
                      : fila.estado == EstadoDeProgreso.completado
                      ? TextosSoporte.repetir
                      : TextosSoporte.comenzar,
                  variante: BotonVariante.primario,
                  onPressed: fila.continuable ? alContinuar : alComenzar,
                ),
              ),
              if (fila.progreso != null) ...[
                const SizedBox(width: Espacio.s2),
                Boton(
                  texto: TextosSoporte.reiniciar,
                  variante: BotonVariante.fantasma,
                  tamano: BotonTamano.sm,
                  onPressed: alReiniciar,
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }

  String _meta(TutorialDefinicion t) {
    final partes = [
      t.categoria,
      _textoDeDificultad(t.dificultad),
      TextosSoporte.pasos(t.pasos.length),
      if (t.minutos != null) TextosSoporte.minutos(t.minutos!),
      if (t.obligatorio) TextosSoporte.obligatorio,
    ];
    return partes.join(' · ');
  }
}

String _textoDeEstado(EstadoDeProgreso estado) => switch (estado) {
  EstadoDeProgreso.pendiente => 'Pendiente',
  EstadoDeProgreso.enProgreso => 'En progreso',
  EstadoDeProgreso.completado => 'Completado',
  EstadoDeProgreso.omitido => 'A medias',
};

String _textoDeDificultad(Dificultad d) => switch (d) {
  Dificultad.inicial => 'Inicial',
  Dificultad.intermedio => 'Intermedio',
  Dificultad.avanzado => 'Avanzado',
};

Tono _tonoDe(EstadoDeProgreso estado) => switch (estado) {
  EstadoDeProgreso.completado => Tono.ok,
  EstadoDeProgreso.enProgreso => Tono.info,
  EstadoDeProgreso.omitido => Tono.aviso,
  EstadoDeProgreso.pendiente => Tono.neutro,
};
