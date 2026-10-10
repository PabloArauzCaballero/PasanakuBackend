import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:flutter/material.dart';

import '../textos_del_alta.dart';
import 'campo_de_detalle.dart';
import 'catalogo_del_perfil.dart';

/// «Actividad económica» del paso 7 y, si se eligió «Otra», el «¿Cuál?» debajo.
///
/// Son dieciocho opciones más «Otra»: pasan el umbral de [CampoDeSeleccion] y se
/// eligen con buscador. El error de «¿Cuál?» se marca en ese campo, no en la lista.
class CampoDeActividad extends StatelessWidget {
  const CampoDeActividad({
    super.key,
    required this.valor,
    required this.cual,
    required this.error,
    required this.onElegida,
    required this.onCambio,
  });

  final String? valor;

  /// Lo que se escribe en «¿Cuál?».
  final TextEditingController cual;

  /// Ya filtrado por «¿se intentó seguir?».
  final String? error;
  final ValueChanged<String> onElegida;
  final VoidCallback onCambio;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      CampoDeSeleccion<String>(
        etiqueta: TextosDelAlta.actividadEconomica,
        icono: Icons.work_outline,
        ayuda: TextosDelAlta.actividadEconomicaAyuda,
        textoVacio: TextosDelAlta.elegirOpcion,
        valor: valor,
        error: valor == null ? error : null,
        exito: valor != null,
        opciones: [
          for (final a in actividadesEconomicas)
            (valor: a.codigo, texto: a.texto),
        ],
        onElegida: onElegida,
      ),
      if (valor == actividadOtra)
        CampoDeDetalle(
          etiqueta: TextosDelAlta.actividadDetalle,
          ayuda: TextosDelAlta.actividadDetalleAyuda,
          controlador: cual,
          error: error,
          onCambio: onCambio,
        ),
    ],
  );
}
