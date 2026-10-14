import 'dart:io';

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';
import '../dominio/juez_de_pose.dart';

class MiniaturasDeVida extends StatelessWidget {
  const MiniaturasDeVida({
    super.key,
    required this.poses,
    required this.capturas,
    required this.rechazadas,
    required this.onRepetir,
  });

  final List<PoseDeVida> poses;
  final Map<CaraDelCarril, Captura> capturas;
  final Set<CaraDelCarril> rechazadas;
  final ValueChanged<PoseDeVida> onRepetir;

  @override
  Widget build(BuildContext context) => RepaintBoundary(
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
      child: Row(
        children: [
          for (final pose in poses)
            Expanded(
              child: _Miniatura(
                pose: pose,
                captura: capturas[pose.cara],
                rechazada: rechazadas.contains(pose.cara),
                onRepetir: () => onRepetir(pose),
              ),
            ),
        ],
      ),
    ),
  );
}

class _Miniatura extends StatelessWidget {
  const _Miniatura({
    required this.pose,
    required this.captura,
    required this.rechazada,
    required this.onRepetir,
  });

  final PoseDeVida pose;
  final Captura? captura;
  final bool rechazada;
  final VoidCallback onRepetir;

  @override
  Widget build(BuildContext context) {
    final titulo = switch (pose) {
      PoseDeVida.frente => 'Frente',
      PoseDeVida.izquierda => 'Perfil izquierdo',
      PoseDeVida.derecha => 'Perfil derecho',
    };
    final estado = rechazada
        ? 'Repetir'
        : captura == null
        ? 'Pendiente'
        : 'Lista';
    final color = rechazada
        ? Paleta.err
        : captura == null
        ? Paleta.white
        : Paleta.ok;
    final contenido = Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(Radios.sm),
          child: SizedBox(
            width: 76,
            height: 76,
            child: captura == null
                ? ColoredBox(
                    color: Paleta.white.withValues(alpha: 0.1),
                    child: Icon(
                      Icons.account_circle_outlined,
                      color: color,
                      size: 34,
                    ),
                  )
                : Image.file(
                    File(captura!.ruta),
                    cacheWidth: 228,
                    fit: BoxFit.cover,
                    errorBuilder: (_, _, _) => const Icon(
                      Icons.account_circle_outlined,
                      color: Paleta.white,
                    ),
                  ),
          ),
        ),
        const SizedBox(height: Espacio.s1),
        Text(
          estado,
          style: Tipo.etiqueta.copyWith(color: color),
          textAlign: TextAlign.center,
        ),
      ],
    );
    final semantica = Semantics(
      container: true,
      button: rechazada,
      label: '$titulo: $estado',
      onTap: rechazada ? onRepetir : null,
      child: ExcludeSemantics(child: contenido),
    );
    if (!rechazada) return semantica;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onRepetir,
      child: semantica,
    );
  }
}
