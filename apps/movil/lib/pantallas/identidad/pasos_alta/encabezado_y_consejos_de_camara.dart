import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/cabecera_de_camara.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/consejos_para_la_foto.dart';

class EncabezadoYConsejosDeCamara extends StatelessWidget {
  const EncabezadoYConsejosDeCamara({
    required this.cara,
    required this.lista,
    required this.onVolver,
    super.key,
  });

  final CaraDelCarril cara;
  final bool lista;
  final VoidCallback onVolver;

  @override
  Widget build(BuildContext context) => Column(
    mainAxisSize: MainAxisSize.min,
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      CabeceraDeCamara(
        titulo: tituloDeCaptura(cara),
        pista: pistaDeCaptura(cara),
        onVolver: onVolver,
      ),
      if (lista)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s3),
          child: ConsejosParaLaFoto(cara: cara),
        ),
    ],
  );
}
