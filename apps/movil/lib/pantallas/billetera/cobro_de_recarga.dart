import 'dart:async';

import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/codigo_q_r.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/organismos/estado_de_tramite.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu10_pago_de_prueba.dart';
import '../../proveedores/haptica.dart';
import 'saldo_de_la_recarga.dart';
import 'textos.dart';

/// La orden de recarga con su QR dinámico: lo que la persona escanea desde su banco, cuánto
/// se acredita y su saldo. Con un QR de PRUEBA (solo TEST) ofrece «Simular pago»; al
/// acreditarse, el saldo cuenta del valor anterior al nuevo.
///
/// El saldo va siempre en la misma posición y con la misma clave: así es el MISMO
/// `MontoQueSube` antes y después del pago, y sube desde donde estaba en vez de rearmarse.
class CobroDeRecarga extends ConsumerWidget {
  const CobroDeRecarga({
    super.key,
    required this.orden,
    required this.cuentaId,
  });
  final SalidaRecarga orden;
  final String cuentaId;

  void _pagar(WidgetRef ref) => ref
      .read(pagarRecargaDePruebaProvider.notifier)
      .pagar(cuentaId: cuentaId, ordenId: orden.ordenRecargaId);

  bool get _dePrueba =>
      orden.cobroQr?.origen == CobroQrRecargaOrigenEnum.PRUEBA;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.listen(pagarRecargaDePruebaProvider, (anterior, siguiente) {
      if (anterior?.value == null && siguiente.value != null) {
        unawaited(ref.read(hapticaProvider).exito().catchError((Object _) {}));
      }
    });
    final pago = ref.watch(pagarRecargaDePruebaProvider);
    final acreditada = pago.value;
    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(vertical: Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (acreditada != null)
            const EstadoDeTramite(
              icono: IconosDeMarca.tramiteListo,
              tono: Tono.ok,
              titulo: TextosBilletera.recargaAcreditada,
              detalle: TextosBilletera.recargaAcreditadaDetalle,
              pasos: _pasos,
              pasoActual: 3,
            )
          else
            _Qr(orden: orden, dePrueba: _dePrueba),
          const SizedBox(height: Espacio.s5),
          SaldoDeLaRecarga(
            key: const ValueKey('saldo-de-la-recarga'),
            cuentaId: cuentaId,
            saldoDespues: acreditada?.saldoDespues,
          ),
          const SizedBox(height: Espacio.s5),
          // Con error, el reintento ES simular el pago otra vez: una sola acción, no dos botones.
          if (pago.hasError)
            EstadoError(error: pago.error!, reintentar: () => _pagar(ref))
          else if (_dePrueba && acreditada == null)
            Boton(
              texto: TextosBilletera.simularPago,
              variante: BotonVariante.primario,
              expandido: true,
              cargando: pago.isLoading,
              onPressed: () => _pagar(ref),
            ),
        ],
      ),
    );
  }
}

const _pasos = [
  TextosBilletera.pasoPedida,
  TextosBilletera.pasoConfirmaElBanco,
  TextosBilletera.pasoAcreditada,
];

/// El QR, lo que se acredita y, si es de prueba, el sello que lo dice.
class _Qr extends StatelessWidget {
  const _Qr({required this.orden, required this.dePrueba});
  final SalidaRecarga orden;
  final bool dePrueba;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          TextosBilletera.pagaConElQr,
          style: Tipo.titulo2.copyWith(color: t.text),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: Espacio.s2),
        Text(
          TextosBilletera.pagaConElQrDetalle,
          style: Tipo.cuerpo.copyWith(color: t.text2),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: Espacio.s4),
        Center(
          child: CodigoQR(
            contenido: orden.cobroQr!.contenido,
            etiqueta: TextosBilletera.qrDeLaRecarga,
          ),
        ),
        const SizedBox(height: Espacio.s4),
        Text(
          TextosBilletera.montoAAcreditar,
          style: Tipo.etiqueta.copyWith(color: t.text2),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: Espacio.s1),
        Center(
          // Lo calcula el servidor (puede descontar el costo del proveedor): no se recalcula.
          child: Monto(
            monto: orden.acreditara.monto,
            moneda: orden.acreditara.moneda.value,
            etiqueta: TextosBilletera.montoAAcreditar,
            estilo: Tipo.cifra.copyWith(color: t.text),
          ),
        ),
        if (dePrueba) ...[
          const SizedBox(height: Espacio.s4),
          const Alerta(
            tono: Tono.aviso,
            titulo: TextosBilletera.qrDePrueba,
            detalle: TextosBilletera.qrDePruebaDetalle,
          ),
        ],
      ],
    );
  }
}
