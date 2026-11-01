import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Ayuda, junto a la campana en la cabecera del inicio (ADR-053, A6): salió de la barra de
/// pestañas, y escanear una invitación pasó al botón del medio de la barra.
class AccionDeAyuda extends StatelessWidget {
  const AccionDeAyuda({super.key});

  @override
  Widget build(BuildContext context) => BotonDeCabecera(
    icono: IconosDeMarca.ayuda,
    etiqueta: 'Ayuda',
    onTap: () => context.go('/soporte/ayuda'),
  );
}
