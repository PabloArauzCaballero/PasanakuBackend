import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/cu21_cobrar_aporte.dart';
import 'textos.dart';

const _canales = {
  'BILLETERA_MOVIL': 'Billetera',
  'QR_INTEROPERABLE': 'QR',
  'TRANSFERENCIA_BANCARIA': 'Transferencia',
  'TARJETA': 'Tarjeta',
};

/// CU-21 · aportar. El monto lo fija el reglamento del grupo (no se inventa acá);
/// esta pantalla lo confirma y elige el canal, con la MISMA disciplina de doble
/// toque que billetera (F4): botón bloqueado con la clave de idempotencia.
class PantallaAportar extends ConsumerStatefulWidget {
  const PantallaAportar({
    super.key,
    required this.obligacionId,
    required this.montoDelPeriodo,
  });
  final String obligacionId;
  final String montoDelPeriodo;

  @override
  ConsumerState<PantallaAportar> createState() => _PantallaAportarState();
}

class _PantallaAportarState extends ConsumerState<PantallaAportar> {
  String _canal = 'BILLETERA_MOVIL';
  final _referencia = TextEditingController();

  @override
  void dispose() {
    _referencia.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(cobrarAporteProvider(widget.obligacionId));
    final resultado = estado.value;
    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloAportar)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: resultado != null
              ? Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Text(TextosPasanaku.aporteConfirmado),
                    Text(
                      'Estado de la obligación: ${resultado.estadoObligacion.name}',
                    ),
                    Text(
                      resultado.esNuevo
                          ? 'Pago nuevo.'
                          : 'Mismo pago de un intento anterior (misma clave).',
                    ),
                  ],
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    CampoMonto(
                      etiqueta: TextosPasanaku.tituloAportar,
                      valorInicial: widget.montoDelPeriodo,
                      onChanged: (_) {},
                    ),
                    const SizedBox(height: Espacio.s4),
                    SelectorSegmentado<String>(
                      opciones: _canales,
                      valor: _canal,
                      onChanged: (v) => setState(() => _canal = v),
                    ),
                    const SizedBox(height: Espacio.s4),
                    Campo(
                      etiqueta: 'Referencia del pago',
                      controlador: _referencia,
                      onChanged: (_) => setState(() {}),
                    ),
                    const SizedBox(height: Espacio.s4),
                    if (estado.hasError)
                      EstadoError(
                        error: estado.error!,
                        reintentar: () => ref.invalidate(
                          cobrarAporteProvider(widget.obligacionId),
                        ),
                      ),
                    Boton(
                      texto: TextosPasanaku.aportarBoton,
                      variante: BotonVariante.primario,
                      cargando: estado.isLoading,
                      onPressed: _referencia.text.trim().isEmpty
                          ? null
                          : () => ref
                                .read(
                                  cobrarAporteProvider(
                                    widget.obligacionId,
                                  ).notifier,
                                )
                                .enviar(
                                  monto: widget.montoDelPeriodo,
                                  canal: _canal,
                                  referenciaProveedor: _referencia.text.trim(),
                                ),
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}
