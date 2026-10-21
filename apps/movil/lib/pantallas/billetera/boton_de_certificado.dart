import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moviles/mostrar_snackbar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/compartir.dart';
import 'textos.dart';

/// Comparte el certificado de saldo del período como texto, con la hoja de compartir del
/// sistema. Lleva solo período y saldo al cierre: ni cuenta, ni nombre, ni movimientos.
class BotonDeCertificado extends ConsumerWidget {
  const BotonDeCertificado({
    super.key,
    required this.periodo,
    required this.moneda,
    required this.saldo,
  });
  final String periodo;
  final String moneda;
  final String saldo;

  @override
  Widget build(BuildContext context, WidgetRef ref) => Boton(
    texto: TextosBilletera.compartirCertificado,
    icono: IconosDeMarca.compartir,
    variante: BotonVariante.secundario,
    expandido: true,
    onPressed: () async {
      final abierto = await ref
          .read(compartirProvider)
          .conOtraApp(
            texto: TextosBilletera.certificado(periodo, moneda, saldo),
          );
      if (abierto || !context.mounted) return;
      mostrarSnackbar(context, TextosBilletera.noSePudoCompartir);
    },
  );
}
