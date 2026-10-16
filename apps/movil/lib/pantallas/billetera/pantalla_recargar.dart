import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu10_recargar_saldo.dart';
import '../../dominio/proteccion_de_pantalla_dinero.dart';
import 'textos.dart';

const _medios = {
  'QR': 'QR',
  'TARJETA': 'Tarjeta',
  'TRANSFERENCIA': 'Transferencia',
};

/// CU-10 — recargar saldo. El importe se escribe con `CampoMonto` (nunca `double`);
/// el botón queda `cargando` mientras la orden viaja, así que un doble toque en mala
/// señal no puede crear dos órdenes: la MISMA clave de idempotencia viaja en las dos
/// (`proveedores/idempotencia.dart`), y mientras `state.isLoading` el notifier ignora
/// una segunda llamada aunque el botón, por algún motivo, se tocara igual.
class PantallaRecargar extends ConsumerStatefulWidget {
  const PantallaRecargar({super.key, required this.cuentaId});
  final String cuentaId;

  @override
  ConsumerState<PantallaRecargar> createState() => _PantallaRecargarState();
}

class _PantallaRecargarState extends ConsumerState<PantallaRecargar>
    with ConProteccionDePantalla<PantallaRecargar> {
  String? _monto;
  String _medio = 'QR';

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(recargarSaldoProvider);
    final notifier = ref.read(recargarSaldoProvider.notifier);
    final orden = estado.value;
    return Scaffold(
      appBar: AppBar(title: const Text(TextosBilletera.recargar)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: orden != null
              ? _Confirmacion(orden: orden)
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    SelectorSegmentado<String>(
                      opciones: _medios,
                      valor: _medio,
                      onChanged: (v) => setState(() => _medio = v),
                    ),
                    const SizedBox(height: Espacio.s4),
                    CampoMonto(
                      etiqueta: TextosBilletera.monto,
                      onChanged: (v) => setState(() => _monto = v),
                    ),
                    const SizedBox(height: Espacio.s4),
                    if (estado.hasError)
                      EstadoError(
                        error: estado.error!,
                        reintentar: () => ref.invalidate(recargarSaldoProvider),
                      ),
                    const Spacer(),
                    Boton(
                      texto: TextosBilletera.recargar,
                      variante: BotonVariante.primario,
                      expandido: true,
                      cargando: estado.isLoading,
                      onPressed: _monto == null
                          ? null
                          : () => notifier.enviar(
                              cuentaId: widget.cuentaId,
                              monto: _monto!,
                              medio: _medio,
                            ),
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}

class _Confirmacion extends StatelessWidget {
  const _Confirmacion({required this.orden});
  final SalidaRecarga orden;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const Icon(Icons.hourglass_top, size: Espacio.s7),
        const SizedBox(height: Espacio.s3),
        Text(
          TextosBilletera.recargaPendiente,
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.titleMedium,
        ),
      ],
    );
  }
}
