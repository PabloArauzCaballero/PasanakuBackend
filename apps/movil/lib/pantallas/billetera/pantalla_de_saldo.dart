import 'dart:async';

import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/esqueleto.dart';
import 'package:aportaya_diseno/atomos/esqueleto_brillante.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu13_consultar_saldo.dart';
import '../../navegacion/accion_de_avisos.dart';
import '../../navegacion/accion_de_ayuda.dart';
import '../soporte/autolanzador_de_guia.dart';
import 'cuerpo_de_saldo.dart';
import 'resolver_cuenta.dart';
import 'textos.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import '../../proveedores/haptica.dart';

/// El inicio de la billetera. Sin `AppBar`: la cabecera es parte de la pantalla, con
/// el saludo encima del título, y la campana de avisos como única acción.
class PantallaDeSaldo extends ConsumerWidget {
  const PantallaDeSaldo({super.key, this.cuentaId});

  /// La cuenta a mostrar; `null` = la de la persona, que se le pregunta al servidor.
  final String? cuentaId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return AutolanzadorDeGuia(
      hijo: Scaffold(
        body: SafeArea(
          bottom: false,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Sin «Hola» suelto: sin el nombre de la persona se leía como un dato
                // que faltaba cargar.
                const CabeceraDeInicio(
                  titulo: TextosBilletera.titulo,
                  acciones: [AccionDeAyuda(), AccionDeAvisos()],
                ),
                Expanded(
                  child: ResolverCuenta(
                    cuentaId: cuentaId,
                    construir: (id) => _SaldoDeLaCuenta(cuentaId: id),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// El saldo de una cuenta ya conocida: cargando, éxito, cuenta en cero y error con reintento.
class _SaldoDeLaCuenta extends ConsumerWidget {
  const _SaldoDeLaCuenta({required this.cuentaId});
  final String cuentaId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final saldo = ref.watch(saldoProvider(cuentaId));
    return RefreshIndicator(
      onRefresh: () async {
        unawaited(
          ref.read(hapticaProvider).seleccion().catchError((Object _) {}),
        );
        ref.invalidate(saldoProvider(cuentaId));
        ref.invalidate(movimientosBilleteraProvider(cuentaId));
      },
      child: EstadoDePantalla<SaldoBilletera>(
        valor: saldo,
        etiquetaDeCarga: TextosBilletera.cargandoSaldo,
        omitirSkeletonAlActualizar: true,
        esqueleto: const EsqueletoBrillante(
          child: Padding(
            padding: EdgeInsets.all(Espacio.s4),
            child: Esqueleto(filas: 5, alto: Espacio.s5),
          ),
        ),
        // Una cuenta en cero también muestra su saldo y sus acciones: es justo cuando
        // hace falta «Recargar», y la guía señala esa cifra en su primer paso.
        mensajeVacio: TextosBilletera.sinMovimientos,
        reintentar: () {
          ref.invalidate(saldoProvider(cuentaId));
          ref.invalidate(movimientosBilleteraProvider(cuentaId));
        },
        exito: (s) => CuerpoDeSaldo(
          saldo: s,
          movimientos: ref.watch(movimientosBilleteraProvider(cuentaId)),
          reintentarMovimientos: () =>
              ref.invalidate(movimientosBilleteraProvider(cuentaId)),
        ),
      ),
    );
  }
}
