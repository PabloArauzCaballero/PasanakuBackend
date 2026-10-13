import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';

/// Elección explícita cuando el control local recomienda repetir una foto.
class OpcionesDeCapturaRechazada extends StatelessWidget {
  const OpcionesDeCapturaRechazada({
    super.key,
    required this.cara,
    required this.motivo,
    required this.onRepetir,
    required this.onUsarEstaFoto,
  });

  final CaraDelCarril cara;
  final String motivo;
  final VoidCallback onRepetir;
  final VoidCallback onUsarEstaFoto;

  @override
  Widget build(BuildContext context) => Column(
    mainAxisSize: MainAxisSize.min,
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      const Text('Repetí la foto', style: Tipo.titulo3),
      const SizedBox(height: Espacio.s2),
      Alerta(titulo: motivo, tono: Tono.aviso),
      const SizedBox(height: Espacio.s3),
      Boton(
        texto: textoParaRepetir(cara),
        variante: BotonVariante.secundario,
        icono: Icons.refresh_rounded,
        expandido: true,
        onPressed: onRepetir,
      ),
      const SizedBox(height: Espacio.s2),
      Boton(
        texto: 'Usar esta foto igual',
        variante: BotonVariante.primario,
        expandido: true,
        onPressed: onUsarEstaFoto,
      ),
    ],
  );

  static String textoParaRepetir(CaraDelCarril cara) => switch (cara) {
    CaraDelCarril.anverso => 'Repetir foto del anverso',
    CaraDelCarril.reverso => 'Repetir foto del reverso',
    CaraDelCarril.selfie => 'Repetir foto de frente',
    CaraDelCarril.perfilIzquierdo => 'Repetir foto del perfil izquierdo',
    CaraDelCarril.perfilDerecho => 'Repetir foto del perfil derecho',
  };
}
