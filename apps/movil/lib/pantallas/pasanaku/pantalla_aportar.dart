import 'dart:async';

import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'aporte_registrado.dart';
import 'dominio/cu21_pagar_cuota_con_saldo.dart';
import 'tarjeta_del_monto_del_periodo.dart';
import '../../proveedores/haptica.dart';
import 'textos.dart';
import 'textos_de_conversion.dart';

/// CU-21 · aportar con el saldo de la billetera (F1.L1.M11, P-06). La app no elige canal,
/// no escribe referencia y no dicta el monto: el servidor cobra el pendiente de la
/// obligación. La pantalla lo muestra como dato y confirma, con el botón bloqueado
/// mientras viaja la clave de idempotencia (sin doble cobro).
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
  @override
  Widget build(BuildContext context) {
    final cobrar = pagarCuotaConSaldoProvider(widget.obligacionId);
    ref.listen(cobrar, (anterior, siguiente) {
      if (anterior?.value == null && siguiente.value != null) {
        unawaited(ref.read(hapticaProvider).exito().catchError((Object _) {}));
      }
    });
    final estado = ref.watch(cobrar);
    final resultado = estado.value;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const CabeceraDeSeccion(titulo: TextosPasanaku.tituloAportar),
              Expanded(
                child: resultado != null
                    ? Center(child: AporteRegistrado(resultado: resultado))
                    : SingleChildScrollView(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            const SizedBox(height: Espacio.s4),
                            TarjetaDelMontoDelPeriodo(
                              monto: widget.montoDelPeriodo,
                            ),
                            const SizedBox(height: Espacio.s3),
                            Text(
                              TextosPasanaku.seCobraDelSaldo,
                              style: Tipo.cuerpoChico.copyWith(
                                color: Tokens.of(context).text2,
                              ),
                            ),
                            const SizedBox(height: Espacio.s4),
                            if (estado.hasError)
                              EstadoError(
                                error: estado.error!,
                                reintentar: () => ref.invalidate(
                                  pagarCuotaConSaldoProvider(
                                    widget.obligacionId,
                                  ),
                                ),
                              ),
                          ],
                        ),
                      ),
              ),
              if (resultado == null)
                Padding(
                  padding: const EdgeInsets.only(
                    top: Espacio.s3,
                    bottom: Espacio.s4,
                  ),
                  // «Aportar» es pagar algo que vence: la única acción en naranja (A2).
                  // El verbo con la cifra: se sabe qué se paga antes de tocar.
                  child: Boton(
                    texto: TextosDeConversion.aportarCon(
                      formatearMonto(
                        monto: widget.montoDelPeriodo,
                        moneda: 'BOB',
                      ),
                    ),
                    variante: BotonVariante.urgencia,
                    expandido: true,
                    cargando: estado.isLoading,
                    onPressed: () => ref
                        .read(
                          pagarCuotaConSaldoProvider(
                            widget.obligacionId,
                          ).notifier,
                        )
                        .pagar(),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
