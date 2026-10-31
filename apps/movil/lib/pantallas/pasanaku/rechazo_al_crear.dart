import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/errores.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:flutter/material.dart';

import 'errores_de_etapa_e_inicio.dart';

/// El rechazo de crear grupo, en lenguaje humano y con la traza para soporte.
class RechazoAlCrear extends StatelessWidget {
  const RechazoAlCrear({super.key, required this.error});
  final Object error;

  @override
  Widget build(BuildContext context) {
    final presentable = error is ErrorPresentable
        ? error as ErrorPresentable
        : null;
    final traza = presentable?.trazaId;
    return Alerta(
      // La etapa mínima y sus desbloqueos (AP-CU20-07/08) dicen qué cambiar.
      titulo:
          mensajeDeEtapaOInicio(error) ??
          presentable?.mensaje ??
          'No pudimos crear el grupo. Probá de nuevo en un momento.',
      detalle: traza == null ? null : 'Código de seguimiento: $traza',
      tono: Tono.error,
    );
  }
}
