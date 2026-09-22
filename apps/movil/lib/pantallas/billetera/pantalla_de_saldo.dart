import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu13_consultar_saldo.dart';
import '../../navegacion/accion_de_avisos.dart';
import 'cuerpo_de_saldo.dart';
import 'textos.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

/// El inicio de la billetera. Sin `AppBar`: la cabecera es parte de la pantalla, con
/// el saludo encima del título, y la campana de avisos como única acción.
class PantallaDeSaldo extends ConsumerWidget {
  const PantallaDeSaldo({super.key, required this.cuentaId});
  final String cuentaId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final saldo = ref.watch(saldoProvider(cuentaId));
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const CabeceraDeInicio(
                saludo: 'Hola',
                titulo: TextosBilletera.titulo,
                acciones: [AccionDeAvisos()],
              ),
              Expanded(
                child: EstadoDePantalla<SaldoBilletera>(
                  valor: saldo,
                  etiquetaDeCarga: TextosBilletera.cargandoSaldo,
                  vacio: saldoEnCero,
                  mensajeVacio: TextosBilletera.sinMovimientos,
                  reintentar: () => ref.invalidate(saldoProvider(cuentaId)),
                  exito: (s) => CuerpoDeSaldo(saldo: s),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
