import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'dominio/cara_del_carril.dart';
import 'texto_completar_kyc.dart';

/// Las fotos de perfil que faltan, juntas en una tarjeta.
class FotosPendientesKyc extends StatelessWidget {
  const FotosPendientesKyc({
    super.key,
    required this.caras,
    required this.subiendo,
    required this.onCapturar,
  });

  final List<CaraDelCarril> caras;
  final Set<CaraDelCarril> subiendo;
  final ValueChanged<CaraDelCarril> onCapturar;

  @override
  Widget build(BuildContext context) => Tarjeta(
    child: Column(
      children: [
        for (final cara in caras)
          FotoPendienteKyc(
            titulo: cara == CaraDelCarril.perfilIzquierdo
                ? TextoCompletarKyc.perfilIzquierdo
                : TextoCompletarKyc.perfilDerecho,
            cargando: subiendo.contains(cara),
            onPressed: () => onCapturar(cara),
          ),
      ],
    ),
  );
}

/// Una foto que falta en el expediente: qué foto es y un botón con nombre para tomarla
/// (antes, un ícono de cámara solo; ADR-053).
class FotoPendienteKyc extends StatelessWidget {
  const FotoPendienteKyc({
    super.key,
    required this.titulo,
    required this.cargando,
    required this.onPressed,
  });

  final String titulo;
  final bool cargando;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: Espacio.s2),
      // Una sola parada del lector: qué foto es y el botón que la toma.
      child: MergeSemantics(
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(titulo, style: Tipo.cuerpo.copyWith(color: t.text)),
                  const SizedBox(height: Espacio.s1),
                  Text(
                    TextoCompletarKyc.consejoFoto,
                    style: Tipo.cuerpoChico.copyWith(color: t.text2),
                  ),
                ],
              ),
            ),
            const SizedBox(width: Espacio.s3),
            Boton(
              texto: TextoCompletarKyc.tomarFoto,
              variante: BotonVariante.secundario,
              icono: Icons.photo_camera_outlined,
              cargando: cargando,
              onPressed: cargando ? null : onPressed,
            ),
          ],
        ),
      ),
    );
  }
}
