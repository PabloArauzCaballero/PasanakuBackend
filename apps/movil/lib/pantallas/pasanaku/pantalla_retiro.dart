import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/cu65_solicitar_retiro.dart';
import 'textos.dart';

/// CU-65 · solicitar el retiro. La posición se muestra ANTES de confirmar y con el
/// texto exacto que llega — nunca se recalcula del lado del cliente.
class PantallaRetiro extends ConsumerStatefulWidget {
  const PantallaRetiro({
    super.key,
    required this.grupoId,
    required this.participanteId,
  });
  final String grupoId;
  final String participanteId;

  @override
  ConsumerState<PantallaRetiro> createState() => _PantallaRetiroState();
}

class _PantallaRetiroState extends ConsumerState<PantallaRetiro> {
  final _motivo = TextEditingController();
  bool _aceptaPlan = false;

  @override
  void dispose() {
    _motivo.dispose();
    super.dispose();
  }

  String _textoPosicion(String posicion) => switch (posicion) {
    'ACREEDORA' => TextosPasanaku.posicionAcreedora,
    'DEUDORA' => TextosPasanaku.posicionDeudora,
    _ => TextosPasanaku.posicionNeutra,
  };

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(solicitarRetiroProvider);
    final resultado = estado.value;
    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloRetiro)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: resultado != null
              ? Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    ChipEstado(
                      texto: resultado.posicion.name,
                      tono: resultado.posicion.name == 'DEUDORA'
                          ? Tono.aviso
                          : Tono.ok,
                    ),
                    const SizedBox(height: Espacio.s3),
                    Text(_textoPosicion(resultado.posicion.name)),
                  ],
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Campo(
                      etiqueta: TextosPasanaku.motivoRetiro,
                      controlador: _motivo,
                      lineas: 3,
                      onChanged: (_) => setState(() {}),
                    ),
                    const SizedBox(height: Espacio.s3),
                    Casilla(
                      etiqueta: TextosPasanaku.aceptoPlanDePago,
                      valor: _aceptaPlan,
                      onChanged: (v) => setState(() => _aceptaPlan = v),
                    ),
                    const SizedBox(height: Espacio.s4),
                    if (estado.hasError)
                      EstadoError(
                        error: estado.error!,
                        reintentar: () =>
                            ref.invalidate(solicitarRetiroProvider),
                      ),
                    Boton(
                      texto: TextosPasanaku.pedirRetiro,
                      variante: BotonVariante.peligro,
                      cargando: estado.isLoading,
                      onPressed: _motivo.text.trim().length < 10
                          ? null
                          : () => ref
                                .read(solicitarRetiroProvider.notifier)
                                .enviar(
                                  grupoId: widget.grupoId,
                                  participanteId: widget.participanteId,
                                  motivo: _motivo.text.trim(),
                                  aceptaPlanDePago: _aceptaPlan,
                                ),
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}
