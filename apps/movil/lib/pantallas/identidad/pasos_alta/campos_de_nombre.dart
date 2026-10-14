import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos.dart';
import '../textos_del_alta.dart';

class CamposDeNombre extends StatelessWidget {
  const CamposDeNombre({
    super.key,
    required this.controladores,
    required this.focos,
    required this.errores,
    required this.onCambio,
  });

  final List<TextEditingController> controladores;
  final List<FocusNode> focos;
  final Map<String, String?> errores;
  final ValueChanged<String> onCambio;

  Widget _campo(int indice, {double alturaMinimaEtiqueta = 36}) {
    final (clave, etiqueta, autofill, ejemplo) = _definicion(indice);
    return Campo(
      etiqueta: etiqueta,
      controlador: controladores[indice],
      foco: focos[indice],
      icono: Icons.person_outline_rounded,
      error: errores[clave],
      ejemplo: ejemplo,
      tipoDeTeclado: TextInputType.name,
      capitalizacion: TextCapitalization.words,
      sugerenciasDeAutocompletado: autofill,
      alturaMinimaEtiqueta: alturaMinimaEtiqueta,
      accionDeTeclado: TextInputAction.next,
      onSubmitted: (_) => focos[indice + 1].requestFocus(),
      onChanged: (_) => onCambio(clave),
    );
  }

  Widget _fila(double ancho, int primero, int segundo) {
    // PasoDatos deja 16 dp de margen por lado; el ancho interior correspondiente
    // al breakpoint Atlas de 400 dp es, por eso, 368 dp.
    final compacto = ancho < 368;
    if (compacto) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _campo(primero),
          const SizedBox(height: Espacio.s3),
          _campo(segundo),
        ],
      );
    }

    // El rótulo «Segundo nombre (opcional)» ocupa dos líneas; reserva ambas
    // antes de pintar las cajas para mantener la fila alineada.
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(child: _campo(primero, alturaMinimaEtiqueta: 45)),
        const SizedBox(width: Espacio.s3),
        Expanded(child: _campo(segundo, alturaMinimaEtiqueta: 45)),
      ],
    );
  }

  (String, String, Iterable<String>, String) _definicion(int indice) =>
      switch (indice) {
        0 => (
          'primerNombre',
          TextosIdentidad.primerNombre,
          [AutofillHints.givenName],
          TextosDelAlta.ejemploPrimerNombre,
        ),
        1 => (
          'segundoNombre',
          TextosIdentidad.segundoNombre,
          [AutofillHints.middleName],
          TextosDelAlta.ejemploSegundoNombre,
        ),
        2 => (
          'apellidoPaterno',
          TextosIdentidad.apellidoPaterno,
          [AutofillHints.familyName],
          TextosDelAlta.ejemploApellidoPaterno,
        ),
        _ => (
          'apellidoMaterno',
          TextosIdentidad.apellidoMaterno,
          [AutofillHints.familyName],
          TextosDelAlta.ejemploApellidoMaterno,
        ),
      };

  @override
  Widget build(BuildContext context) => LayoutBuilder(
    builder: (context, limites) => Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _fila(limites.maxWidth, 0, 1),
        const SizedBox(height: Espacio.s3),
        _fila(limites.maxWidth, 2, 3),
      ],
    ),
  );
}
