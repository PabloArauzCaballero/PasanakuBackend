import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu13_cuenta_propia.dart';
import 'textos.dart';

/// Entrega a [construir] el id de la cuenta de la persona.
///
/// Si la ruta ya trae `?cuenta=` (la pantalla se abrió desde otra que la conoce) se usa tal cual y
/// no se pregunta nada. Si no, se pregunta al servidor `GET /billetera/cuentas/propia`: la app
/// nunca supone una cuenta (antes consultaba un id fijo y, contra el servidor real, leía como falla
/// del servidor lo que era «todavía no tenés billetera activa»).
///
/// Los estados: cargando, billetera todavía no activa (no es una falla: se dice y se puede volver a
/// revisar), y error accionable con reintento y código de seguimiento.
class ResolverCuenta extends ConsumerWidget {
  const ResolverCuenta({super.key, this.cuentaId, required this.construir});

  final String? cuentaId;
  final Widget Function(String cuentaId) construir;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final conocida = cuentaId;
    if (conocida != null && conocida.isNotEmpty) return construir(conocida);
    final valor = ref.watch(cuentaPropiaProvider);
    void revisar() => ref.invalidate(cuentaPropiaProvider);
    if (valor.hasValue && valor.value == null) {
      return EstadoVacio(
        mensaje: TextosBilletera.billeteraInactiva,
        accion: Boton(
          texto: TextosBilletera.revisarDeNuevo,
          variante: BotonVariante.fantasma,
          onPressed: revisar,
        ),
      );
    }
    return EstadoDePantalla<CuentaPropia?>(
      valor: valor,
      etiquetaDeCarga: TextosBilletera.buscandoBilletera,
      mensajeVacio: TextosBilletera.billeteraInactiva,
      reintentar: revisar,
      exito: (cuenta) => construir(cuenta!.cuentaId),
    );
  }
}

/// Lo mismo para las pantallas interiores (recargar, retirar, transferir, extracto): mientras la
/// cuenta no está resuelta, un marco con el título y el estado; resuelta, la pantalla real.
class PantallaConCuenta extends StatelessWidget {
  const PantallaConCuenta({
    super.key,
    required this.titulo,
    required this.cuentaId,
    required this.construir,
  });

  final String titulo;
  final String? cuentaId;
  final Widget Function(String cuentaId) construir;

  @override
  Widget build(BuildContext context) {
    if (cuentaId != null && cuentaId!.isNotEmpty) return construir(cuentaId!);
    return Scaffold(
      appBar: AppBar(title: Text(titulo)),
      body: SafeArea(
        child: ResolverCuenta(construir: construir),
      ),
    );
  }
}
