import 'dart:async';

import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu10_pago_de_prueba.dart';
import '../../proveedores/haptica.dart';
import '../../proveedores/pagos_de_prueba.dart';
import 'acciones_del_cobro.dart';
import 'pasos_para_pagar.dart';
import 'recarga_acreditada.dart';
import 'saldo_de_la_recarga.dart';
import 'tarjeta_del_qr.dart';
import 'textos.dart';

/// La orden de recarga con su QR dinámico. Arriba, la tarjeta protagonista (el estado del pago,
/// cuánto se acredita y el QR); al lado o debajo, cómo pagar, el saldo como dato secundario y la
/// única acción del momento. Con un QR de PRUEBA (solo TEST) ofrece «Simular pago»; al
/// acreditarse, la tarjeta confirma y el saldo cuenta del valor anterior al nuevo.
///
/// El saldo va siempre en la misma columna y con la misma clave: así es el MISMO `MontoQueSube`
/// antes y después del pago, y sube desde donde estaba en vez de rearmarse.
class CobroDeRecarga extends ConsumerWidget {
  const CobroDeRecarga({
    super.key,
    required this.orden,
    required this.cuentaId,
  });
  final SalidaRecarga orden;
  final String cuentaId;

  /// Desde este ancho, la tarjeta y el resto van en dos columnas: en una tableta, una sola
  /// columna estirada deja el QR flotando en el medio y los textos en renglones larguísimos.
  static const anchoDeDosColumnas = Espacio.s7 * 14;

  /// El ancho máximo de todo el contenido, para que en pantallas grandes no se desparrame.
  static const anchoMaximo = Espacio.s7 * 20;

  void _pagar(WidgetRef ref) => ref
      .read(pagarRecargaDePruebaProvider.notifier)
      .pagar(cuentaId: cuentaId, ordenId: orden.ordenRecargaId);

  bool _dePrueba(WidgetRef ref) =>
      ref.watch(pagosDePruebaProvider) &&
      orden.cobroQr?.origen == CobroQrRecargaOrigenEnum.PRUEBA;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dePrueba = _dePrueba(ref);
    ref.listen(pagarRecargaDePruebaProvider, (anterior, siguiente) {
      if (anterior?.value == null && siguiente.value != null) {
        unawaited(ref.read(hapticaProvider).exito().catchError((Object _) {}));
      }
    });
    final pago = ref.watch(pagarRecargaDePruebaProvider);
    final acreditada = pago.value;
    final principal = acreditada != null
        ? RecargaAcreditada(orden: orden)
        : TarjetaDelQr(orden: orden, dePrueba: dePrueba, fallo: pago.hasError);
    final simulable = dePrueba && acreditada == null && !pago.hasError;
    final hayAccion = AccionesDelCobro.hayAccion(
      acreditada: acreditada != null,
      simulable: simulable,
    );
    final accion = AccionesDelCobro(
      acreditada: acreditada != null,
      simulable: simulable,
      pagando: pago.isLoading,
      pagar: () => _pagar(ref),
    );
    return LayoutBuilder(
      builder: (context, limites) {
        final dosColumnas = limites.maxWidth >= anchoDeDosColumnas;
        // Con error, el aviso es lo primero que se lee (arriba de todo en una columna, arriba de
        // la columna de al lado en dos), y su botón es el reintento.
        final aviso = pago.hasError
            ? Tarjeta(
                relleno: false,
                child: EstadoError(
                  error: pago.error!,
                  reintentar: () => _pagar(ref),
                ),
              )
            : null;
        final secundario = Column(
          key: const ValueKey('columna-secundaria'),
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (dosColumnas && aviso != null) ...[
              aviso,
              const SizedBox(height: Espacio.s5),
            ],
            if (acreditada == null) ...[
              const PasosParaPagar(),
              const SizedBox(height: Espacio.s5),
            ],
            SaldoDeLaRecarga(
              key: const ValueKey('saldo-de-la-recarga'),
              cuentaId: cuentaId,
              saldoDespues: acreditada?.saldoDespues,
            ),
            if (acreditada == null && !dePrueba && !pago.hasError) ...[
              const SizedBox(height: Espacio.s3),
              Text(
                TextosBilletera.recargaPendienteDetalle,
                style: Tipo.cuerpoChico.copyWith(
                  color: Tokens.of(context).text2,
                ),
                textAlign: TextAlign.center,
              ),
            ],
            // En dos columnas la acción cierra la columna; en una, va anclada abajo.
            if (dosColumnas && hayAccion) ...[
              const SizedBox(height: Espacio.s5),
              accion,
            ],
          ],
        );
        final contenido = SingleChildScrollView(
          padding: const EdgeInsets.symmetric(vertical: Espacio.s4),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: anchoMaximo),
              child: dosColumnas
                  ? Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(child: principal),
                        const SizedBox(width: Espacio.s5),
                        Expanded(child: secundario),
                      ],
                    )
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        if (aviso != null) ...[
                          aviso,
                          const SizedBox(height: Espacio.s5),
                        ],
                        principal,
                        const SizedBox(height: Espacio.s5),
                        secundario,
                      ],
                    ),
            ),
          ),
        );
        // La misma forma que el botón «Recargar» del paso anterior: fuera del scroll, abajo. El
        // contenido queda siempre en el mismo lugar del árbol, haya pie o no, para que el saldo
        // no se rearme al pasar de un estado a otro.
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(child: contenido),
            if (!dosColumnas && hayAccion)
              Padding(
                padding: const EdgeInsets.only(
                  top: Espacio.s3,
                  bottom: Espacio.s4,
                ),
                child: accion,
              ),
          ],
        );
      },
    );
  }
}
