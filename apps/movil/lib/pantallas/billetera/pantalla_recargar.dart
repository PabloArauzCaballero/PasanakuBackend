import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/organismos/aviso_de_conexion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu10_recargar_saldo.dart';
import '../../dominio/error_de_operacion_de_dinero.dart';
import '../../dominio/proteccion_de_pantalla_dinero.dart';
import '../../proveedores/conexion.dart';
import 'textos.dart';

const _medios = {
  'QR': 'QR',
  'TARJETA': 'Tarjeta',
  'TRANSFERENCIA': 'Transferencia',
};

/// CU-10 — recargar saldo. El importe se escribe con `CampoMonto` (nunca `double`);
/// el botón queda `cargando` mientras la orden viaja, así que un doble toque en mala
/// señal no puede crear dos órdenes: la MISMA clave de idempotencia viaja en las dos
/// (guardada en almacén seguro), y mientras `state.isLoading` el notifier ignora
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
    final conexion = ref.watch(hayConexionProvider);
    final redConfirmada = conexion.permiteOperar;
    void enviar() {
      if (!ref.read(hayConexionProvider).permiteOperar || _monto == null) {
        return;
      }
      notifier.enviar(cuentaId: widget.cuentaId, monto: _monto!, medio: _medio);
    }

    return Scaffold(
      appBar: AppBar(title: const Text(TextosBilletera.recargar)),
      body: SafeArea(
        child: CustomScrollView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          slivers: [
            SliverPadding(
              padding: const EdgeInsets.all(Espacio.s4),
              sliver: SliverFillRemaining(
                hasScrollBody: false,
                child: orden != null
                    ? _Confirmacion(orden: orden)
                    : Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          if (!redConfirmada) ...[
                            AvisoDeConexion(
                              estado: conexion,
                              detalleSinConexion:
                                  TextosBilletera.operacionSinConexion,
                              reintentar: () =>
                                  ref.invalidate(hayConexionProvider),
                            ),
                            const SizedBox(height: Espacio.s4),
                          ],
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
                              reintentar:
                                  !redConfirmada ||
                                      estado.error
                                          is ErrorDeOperacionPendiente ||
                                      _monto == null
                                  ? null
                                  : enviar,
                            ),
                          const Spacer(),
                          Boton(
                            texto: TextosBilletera.recargar,
                            variante: BotonVariante.primario,
                            expandido: true,
                            cargando: estado.isLoading,
                            onPressed:
                                !redConfirmada ||
                                    _monto == null ||
                                    estado.error is ErrorDeOperacionPendiente
                                ? null
                                : enviar,
                          ),
                        ],
                      ),
              ),
            ),
          ],
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
