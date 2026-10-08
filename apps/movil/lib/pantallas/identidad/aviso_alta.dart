import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

class AvisoDeAlta extends StatelessWidget {
  const AvisoDeAlta({super.key, required this.fotosPendientes});

  final bool fotosPendientes;

  @override
  Widget build(BuildContext context) => Semantics(
    liveRegion: fotosPendientes,
    child: Alerta(
      tono: fotosPendientes ? Tono.aviso : Tono.ok,
      titulo: fotosPendientes
          ? TextosIdentidad.altaFotosPendientesTitulo
          : TextosIdentidad.altaListaTitulo,
      detalle: fotosPendientes
          ? TextosIdentidad.altaFotosPendientesDetalle
          : TextosIdentidad.altaListaDetalle,
    ),
  );
}
