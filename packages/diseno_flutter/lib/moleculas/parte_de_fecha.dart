import 'package:flutter/material.dart';

import '../atomos/campo_de_seleccion.dart';

/// Los meses como se dicen, para el select del medio de [CampoDeFecha].
const mesesDelAnio = [
  'Enero',
  'Febrero',
  'Marzo',
  'Abril',
  'Mayo',
  'Junio',
  'Julio',
  'Agosto',
  'Septiembre',
  'Octubre',
  'Noviembre',
  'Diciembre',
];

/// Días que tiene un mes: el día 0 del mes siguiente es el último de este.
int diasDelMes(int anio, int mes) => DateTime(anio, mes + 1, 0).day;

/// Una de las tres partes de [CampoDeFecha] (Día, Mes o Año): un select sin
/// etiqueta propia —la etiqueta es la de la fecha entera— que ocupa su parte de la
/// fila. Día y año pasan las doce opciones y se eligen con buscador numérico.
class ParteDeFecha extends StatelessWidget {
  const ParteDeFecha({
    super.key,
    required this.vacio,
    required this.flex,
    required this.valor,
    required this.opciones,
    required this.onElegida,
    this.marca,
    this.completa = false,
    this.numerico = true,
    this.foco,
  });

  /// Lo que dice antes de elegir: «Día», «Mes» o «Año».
  final String vacio;
  final int flex;
  final int? valor;
  final List<({int valor, String texto})> opciones;
  final ValueChanged<int> onElegida;

  /// `''` pinta el borde de error sin texto: el mensaje va una vez, debajo de la fila.
  final String? marca;
  final bool completa;
  final bool numerico;
  final FocusNode? foco;

  @override
  Widget build(BuildContext context) => Expanded(
    flex: flex,
    child: CampoDeSeleccion<int>(
      etiqueta: '',
      textoVacio: vacio,
      foco: foco,
      valor: valor,
      error: marca,
      exito: completa,
      tecladoDeBusqueda: numerico ? TextInputType.number : TextInputType.text,
      opciones: opciones,
      onElegida: onElegida,
    ),
  );
}
