import 'package:flutter/material.dart';

import '../atomos/entrada_de_estado.dart';
import '../atomos/tono.dart';
import '../tokens/tokens.dart';

part 'estado_de_tramite_pasos.dart';

/// Un trámite que no depende de la persona (una verificación que revisa alguien, un pago que
/// confirma el banco): dónde está, qué sigue y cuál es su número de caso (ADR-053).
///
/// Antes eran un ícono y dos líneas arriba a la izquierda sobre una hoja vacía. Acá el estado va
/// centrado, el círculo de color lo anuncia y los [pasos] dicen en qué parte del camino
/// está, sin inventar tiempos: solo qué pasó y qué falta.
class EstadoDeTramite extends StatelessWidget {
  const EstadoDeTramite({
    super.key,
    required this.icono,
    required this.tono,
    required this.titulo,
    this.detalle,
    this.pasos = const [],
    this.pasoActual = 0,
    this.fallo = false,
    this.caso,
  });

  final IconData icono;
  final Tono tono;
  final String titulo;
  final String? detalle;

  /// Las etapas del camino, en orden («Recibido», «En revisión», «Listo»).
  final List<String> pasos;

  /// Índice de la etapa en curso. Las anteriores van hechas; con [fallo], la actual es la que
  /// no se pudo completar. Un índice igual a `pasos.length` marca todo como hecho.
  final int pasoActual;
  final bool fallo;

  /// «Caso a11ce001»: lo que se cita a soporte.
  final String? caso;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final (fondo, frente) = tono.coloresDe(t);
    return EntradaDeEstado(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ExcludeSemantics(
            child: DecoratedBox(
              decoration: BoxDecoration(color: fondo, shape: BoxShape.circle),
              child: SizedBox.square(
                dimension: Tactil.minimo * 2,
                child: Icon(
                  icono,
                  size: Espacio.s7 - Espacio.s2,
                  color: frente,
                ),
              ),
            ),
          ),
          const SizedBox(height: Espacio.s5),
          Text(
            titulo,
            textAlign: TextAlign.center,
            style: Tipo.titulo1.copyWith(color: t.text),
          ),
          if (detalle != null && detalle!.isNotEmpty) ...[
            const SizedBox(height: Espacio.s2),
            Text(
              detalle!,
              textAlign: TextAlign.center,
              style: Tipo.cuerpo.copyWith(color: t.text2),
            ),
          ],
          if (pasos.isNotEmpty) ...[
            const SizedBox(height: Espacio.s5),
            _Pasos(
              pasos: pasos,
              actual: pasoActual,
              fallo: fallo,
              tokens: t,
              frente: frente,
            ),
          ],
          if (caso != null) ...[
            const SizedBox(height: Espacio.s5),
            DecoratedBox(
              decoration: BoxDecoration(
                color: t.surface2,
                borderRadius: BorderRadius.circular(Radios.pill),
              ),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: Espacio.s3,
                  vertical: Espacio.s1 + Espacio.s1 / 2,
                ),
                child: Text(
                  caso!,
                  style: Tipo.cuerpoChico.copyWith(color: t.text2),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
