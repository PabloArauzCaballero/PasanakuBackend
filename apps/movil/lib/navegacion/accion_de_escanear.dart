import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// El acceso a escanear una invitación, junto a la campana en la cabecera del inicio.
class AccionDeEscanear extends StatelessWidget {
  const AccionDeEscanear({super.key});

  @override
  Widget build(BuildContext context) => BotonDeCabecera(
    icono: Icons.qr_code_scanner,
    etiqueta: 'Escanear invitación',
    onTap: () => context.push('/pasanaku/escanear'),
  );
}
