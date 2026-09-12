import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../dominio/cu13_consultar_saldo.dart';
import '../../pantallas/billetera/tarjeta_de_saldo.dart';
import '../../pantallas/billetera/textos.dart';
import 'package:aportaya_diseno/moleculas/acciones_rapidas.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

/// Pantalla de inicio de la billetera (`docs/Views/AportaYa-Maqueta.html`, función
/// `pintarSaldo`): la fila de accesos rápidos y la tarjeta de saldo (`tarjeta_de_saldo.dart`).
///
/// **Supuesto declarado — «Vales».** La maqueta muestra un cuarto acceso rápido,
/// «Vales», que carril-M2.md (§ huecos 2 y 3) ya declaró bloqueado: ni
/// `mobile_scanner` está en `pubspec.yaml` ni existe un CU de «vale» localizado en el
/// rango de este carril. El acceso se muestra deshabilitado con su motivo, no se
/// inventa una pantalla.
class PantallaDeSaldo extends ConsumerWidget {
  const PantallaDeSaldo({super.key, required this.cuentaId});

  final String cuentaId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final saldo = ref.watch(saldoProvider(cuentaId));
    return Scaffold(
      appBar: AppBar(title: const Text(TextosBilletera.titulo)),
      body: EstadoDePantalla<SaldoBilletera>(
        valor: saldo,
        etiquetaDeCarga: TextosBilletera.cargandoSaldo,
        vacio: saldoEnCero,
        mensajeVacio: TextosBilletera.sinMovimientos,
        reintentar: () => ref.invalidate(saldoProvider(cuentaId)),
        exito: (s) => _CuerpoSaldo(saldo: s),
      ),
    );
  }
}

class _CuerpoSaldo extends StatelessWidget {
  const _CuerpoSaldo({required this.saldo});
  final SaldoBilletera saldo;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(Espacio.s4),
      children: [
        AccionesRapidas(
          acciones: [
            (
              texto: TextosBilletera.recargar,
              icono: Icons.file_download_outlined,
              onTap: () => context.pushNamed(
                'billetera.recargar',
                queryParameters: {'cuenta': saldo.cuentaId},
              ),
            ),
            (
              texto: TextosBilletera.aportar,
              icono: Icons.file_upload_outlined,
              onTap: () => context.pushNamed('pasanaku.miEstado'),
            ),
            (
              texto: TextosBilletera.retirar,
              icono: Icons.account_balance_outlined,
              onTap: () => context.pushNamed(
                'billetera.retirar',
                queryParameters: {'cuenta': saldo.cuentaId},
              ),
            ),
            (
              texto: TextosBilletera.vales,
              icono: Icons.confirmation_number_outlined,
              onTap: () => ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text(TextosBilletera.valesNoDisponible),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: Espacio.s4),
        TarjetaDeSaldo(saldo: saldo),
      ],
    );
  }
}
