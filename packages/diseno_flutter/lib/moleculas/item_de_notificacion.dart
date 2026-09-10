import 'package:flutter/material.dart';

import '../atomos/punto.dart';
import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// Un aviso de la bandeja: ícono por tono, título, detalle, cuándo, leído o no.
class ItemDeNotificacion extends StatelessWidget {
  const ItemDeNotificacion({
    super.key,
    required this.titulo,
    required this.detalle,
    required this.cuando,
    this.tono = Tono.info,
    this.leida = false,
    this.onTap,
  });
  final String titulo;
  final String detalle;
  final String cuando;
  final Tono tono;
  final bool leida;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final (fondo, frente) = tono.coloresDe(t);
    return Semantics(
      button: onTap != null,
      label: '${leida ? '' : 'Sin leer. '}$titulo. $detalle. $cuando',
      excludeSemantics: true,
      child: InkWell(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.all(Espacio.s4),
          color: leida ? Colors.transparent : t.surface2,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CircleAvatar(
                radius: Espacio.s5,
                backgroundColor: fondo,
                child: Icon(
                  Icons.notifications_outlined,
                  color: frente,
                  size: Espacio.s5,
                ),
              ),
              const SizedBox(width: Espacio.s3),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      titulo,
                      style: texto.titleSmall?.copyWith(
                        color: t.text,
                        fontWeight: leida ? FontWeight.w500 : FontWeight.w700,
                      ),
                    ),
                    Text(
                      detalle,
                      style: texto.bodyMedium?.copyWith(color: t.text2),
                    ),
                    Text(
                      cuando,
                      style: texto.bodySmall?.copyWith(color: t.text3),
                    ),
                  ],
                ),
              ),
              if (!leida)
                const Padding(
                  padding: EdgeInsets.only(top: Espacio.s2),
                  child: Punto(),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
