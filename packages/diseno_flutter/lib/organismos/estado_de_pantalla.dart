import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../organismos/estado_error.dart';
import '../organismos/estado_vacio.dart';
import '../organismos/motivo_vacio.dart';
import '../tokens/tokens.dart';

/// El único widget que sabe pintar los cuatro estados (más sin conexión).
///
/// `AsyncValue` ya es cargando / error / dato; lo que no sabe es qué es «vacío» ni
/// cómo se ve un error en voz de marca. Pintar los estados con `switch` en cada
/// pantalla es cómo se olvida uno.
class EstadoDePantalla<T> extends StatelessWidget {
  const EstadoDePantalla({
    super.key,
    required this.valor,
    required this.exito,
    required this.mensajeVacio,
    required this.reintentar,
    this.vacio,
    this.motivoVacio = MotivoVacio.sinDatos,
    this.esqueleto,
    this.etiquetaDeCarga = 'Cargando',
  });

  final AsyncValue<T> valor;
  final Widget Function(T dato) exito;
  final bool Function(T dato)? vacio;
  final String mensajeVacio;
  final MotivoVacio motivoVacio;
  final VoidCallback reintentar;
  final Widget? esqueleto;
  final String etiquetaDeCarga;

  @override
  Widget build(BuildContext context) {
    return valor.when(
      skipLoadingOnRefresh: false,
      loading: () => Semantics(
        label: etiquetaDeCarga,
        liveRegion: true,
        child: esqueleto ?? const _Esqueleto(),
      ),
      error: (error, _) => EstadoError(error: error, reintentar: reintentar),
      data: (dato) {
        if (vacio?.call(dato) ?? false) {
          return EstadoVacio(mensaje: mensajeVacio, motivo: motivoVacio);
        }
        return exito(dato);
      },
    );
  }
}

class _Esqueleto extends StatelessWidget {
  const _Esqueleto();

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    Widget barra(double ancho) => Container(
      width: ancho,
      height: Espacio.s5,
      margin: const EdgeInsets.only(bottom: Espacio.s3),
      decoration: BoxDecoration(
        color: t.surface2,
        borderRadius: BorderRadius.circular(Radios.sm),
      ),
    );
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [barra(160), barra(240), barra(120)],
      ),
    );
  }
}
