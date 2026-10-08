import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos.dart';

/// El número y el departamento son un dato compuesto, pero necesitan ancho propio
/// en pantallas estrechas y con letra grande para conservar etiquetas y ayudas.
class DocumentoYLugar extends StatelessWidget {
  const DocumentoYLugar({
    super.key,
    required this.controlador,
    required this.focoNumero,
    required this.focoLugar,
    required this.lugar,
    required this.opciones,
    required this.errorNumero,
    required this.errorLugar,
    required this.exitoNumero,
    required this.exitoLugar,
    required this.onNumero,
    required this.onLugar,
  });

  final TextEditingController controlador;
  final FocusNode focoNumero;
  final FocusNode focoLugar;
  final String? lugar;
  final List<({String valor, String texto})> opciones;
  final String? errorNumero;
  final String? errorLugar;
  final bool exitoNumero;
  final bool exitoLugar;
  final ValueChanged<String> onNumero;
  final ValueChanged<String> onLugar;

  @override
  Widget build(BuildContext context) => LayoutBuilder(
    builder: (context, constraints) {
      final numero = Campo(
        etiqueta: TextosIdentidad.numeroDocumento,
        controlador: controlador,
        foco: focoNumero,
        icono: Icons.credit_card_outlined,
        ayuda: TextosIdentidad.documentoAyuda,
        error: errorNumero,
        exito: exitoNumero,
        onChanged: onNumero,
      );
      final expedido = CampoDeSeleccion<String>(
        etiqueta: TextosIdentidad.lugarExpedicion,
        valor: lugar,
        textoVacio: TextosIdentidad.lugarExpedicionVacio,
        ayuda: TextosIdentidad.lugarExpedicionAyuda,
        error: errorLugar,
        exito: exitoLugar,
        foco: focoLugar,
        opciones: opciones,
        onElegida: onLugar,
      );
      final apilar =
          constraints.maxWidth < 400 ||
          MediaQuery.textScalerOf(context).scale(1) >= 1.5;
      if (apilar) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            numero,
            const SizedBox(height: Espacio.s3),
            expedido,
          ],
        );
      }
      return Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(flex: 3, child: numero),
          const SizedBox(width: Espacio.s3),
          Expanded(flex: 2, child: expedido),
        ],
      );
    },
  );
}
