import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/referencia_de_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'dominio/capturas_del_expediente.dart';
import 'dominio/subida_del_expediente.dart';
import 'textos_de_subida.dart';

/// Una fila de la pantalla de subida: el nombre de la cara y su estado — en curso
/// (con «Cancelar»), enviada, o fallida (con «Reintentar» y «Repetir la foto»).
class FilaDeSubida extends StatelessWidget {
  const FilaDeSubida({
    super.key,
    required this.cara,
    required this.estado,
    required this.ruta,
    required this.onCancelar,
    required this.onReintentar,
    required this.onRepetir,
  });

  final CaraDelCarril cara;
  final SubidaDeUnaCara? estado;
  final String? ruta;
  final VoidCallback onCancelar;
  final VoidCallback onReintentar;
  final VoidCallback onRepetir;

  @override
  Widget build(BuildContext context) {
    final valor = estado?.estado ?? EstadoDeUnaSubida.pendiente;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: Espacio.s2),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            tituloDeCaptura(cara),
            style: Theme.of(context).textTheme.titleSmall,
          ),
          const SizedBox(height: Espacio.s1),
          switch (valor) {
            EstadoDeUnaSubida.pendiente => const Text('—'),
            EstadoDeUnaSubida.subiendo => _conDetalle(
              TextosDeSubida.subiendo(cara),
              TextosDeSubida.detalle,
              onCancelar,
            ),
            EstadoDeUnaSubida.lenta => _conDetalle(
              TextosDeSubida.subiendo(cara),
              TextosDeSubida.lenta,
              onCancelar,
            ),
            EstadoDeUnaSubida.subida => const Alerta(
              titulo: TextosDeSubida.todoListo,
              tono: Tono.ok,
            ),
            EstadoDeUnaSubida.fallida => Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Alerta(titulo: estado?.error ?? '', tono: Tono.error),
                ReferenciaDeError(trazaId: estado?.trazaId),
                const SizedBox(height: Espacio.s2),
                Wrap(
                  spacing: Espacio.s1,
                  runSpacing: Espacio.s1,
                  children: [
                    TextButton(
                      onPressed: onReintentar,
                      child: const Text(TextosDeSubida.reintentar),
                    ),
                    TextButton(
                      onPressed: onRepetir,
                      child: Text(TextosDeSubida.repetirFotoDe(cara)),
                    ),
                  ],
                ),
              ],
            ),
          },
        ],
      ),
    );
  }

  Widget _conDetalle(String titulo, String detalle, VoidCallback onCancelar) =>
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Semantics(
            label: 'Subida en progreso',
            child: const LinearProgressIndicator(),
          ),
          const SizedBox(height: Espacio.s2),
          Text(titulo),
          Text(detalle, style: Tipo.ayuda),
          TextButton(
            onPressed: onCancelar,
            child: const Text(TextosDeSubida.cancelarLaSubida),
          ),
        ],
      );
}
