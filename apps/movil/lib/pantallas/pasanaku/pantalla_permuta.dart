import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/cu62_solicitar_permuta.dart';
import 'textos.dart';

/// CU-62 · proponer una permuta de turnos. Nace `PENDIENTE`: esta pantalla nunca la
/// muestra como confirmada hasta que la contraparte acepte.
class PantallaPermuta extends ConsumerStatefulWidget {
  const PantallaPermuta({
    super.key,
    required this.turnoOrigenId,
    required this.turnoDestinoId,
    required this.contraparteId,
  });
  final String turnoOrigenId;
  final String turnoDestinoId;
  final String contraparteId;

  @override
  ConsumerState<PantallaPermuta> createState() => _PantallaPermutaState();
}

class _PantallaPermutaState extends ConsumerState<PantallaPermuta> {
  final _motivo = TextEditingController();

  @override
  void dispose() {
    _motivo.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(solicitarPermutaProvider);
    final resultado = estado.value;
    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloPermuta)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: resultado != null
              ? Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Text(TextosPasanaku.permutaPendiente),
                    Text('Estado: ${resultado.estado}'),
                  ],
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Campo(
                      etiqueta: TextosPasanaku.motivoPermuta,
                      controlador: _motivo,
                      lineas: 3,
                      onChanged: (_) => setState(() {}),
                    ),
                    const SizedBox(height: Espacio.s4),
                    if (estado.hasError)
                      EstadoError(
                        error: estado.error!,
                        reintentar: () =>
                            ref.invalidate(solicitarPermutaProvider),
                      ),
                    Boton(
                      texto: TextosPasanaku.pedirPermuta,
                      variante: BotonVariante.primario,
                      cargando: estado.isLoading,
                      onPressed: _motivo.text.trim().length < 10
                          ? null
                          : () => ref
                                .read(solicitarPermutaProvider.notifier)
                                .enviar(
                                  turnoOrigenId: widget.turnoOrigenId,
                                  turnoDestinoId: widget.turnoDestinoId,
                                  contraparteId: widget.contraparteId,
                                  motivo: _motivo.text.trim(),
                                ),
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}
