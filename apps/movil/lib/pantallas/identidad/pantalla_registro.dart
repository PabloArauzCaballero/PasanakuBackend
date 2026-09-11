import 'package:aportaya_diseno/moleculas/barra_de_pasos.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_alta.dart';
import 'pasos_alta/paso_captura.dart';
import 'pasos_alta/paso_celular.dart';
import 'pasos_alta/paso_cotejo.dart';
import 'pasos_alta/paso_datos.dart';
import 'pasos_alta/paso_perfil_transaccional.dart';
import 'textos.dart';

/// CU-01, el alta en ocho pasos (D-1 de la maqueta). Un solo `Notifier`
/// (`AltaNotifier`) sabe en qué paso está; esta pantalla solo elige qué organismo
/// mostrar. **No es un `StatefulWidget` de 600 líneas** — cada paso es su propio
/// archivo en `pasos_alta/`.
class PantallaDeRegistro extends ConsumerWidget {
  const PantallaDeRegistro({super.key});

  static const _nombreDePaso = {
    PasoAlta.datos: TextosIdentidad.pasoDatos,
    PasoAlta.celular: TextosIdentidad.pasoCelular,
    PasoAlta.anverso: TextosIdentidad.pasoAnverso,
    PasoAlta.reverso: TextosIdentidad.pasoReverso,
    PasoAlta.pruebaDeVida: TextosIdentidad.pasoPruebaDeVida,
    PasoAlta.cotejo: TextosIdentidad.pasoCotejo,
    PasoAlta.perfilTransaccional: TextosIdentidad.pasoPerfil,
    PasoAlta.contrato: TextosIdentidad.pasoContrato,
  };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(altaProvider);
    final notifier = ref.read(altaProvider.notifier);

    Widget cuerpo() => switch (estado.paso) {
      PasoAlta.datos => const PasoDatos(),
      PasoAlta.celular => const PasoCelular(),
      PasoAlta.anverso => PasoCaptura(
        titulo: TextosIdentidad.capturarAnverso,
        esDocumento: true,
        rutaActual: estado.rutaAnverso,
        onCapturado: notifier.capturarAnverso,
        onContinuar: notifier.siguiente,
      ),
      PasoAlta.reverso => PasoCaptura(
        titulo: TextosIdentidad.capturarReverso,
        esDocumento: true,
        rutaActual: estado.rutaReverso,
        onCapturado: notifier.capturarReverso,
        onContinuar: notifier.siguiente,
      ),
      PasoAlta.pruebaDeVida => PasoCaptura(
        titulo: TextosIdentidad.capturarSelfie,
        esDocumento: false,
        rutaActual: estado.rutaSelfie,
        onCapturado: notifier.capturarSelfie,
        onContinuar: notifier.siguiente,
        // La prueba de vida no tiene alternativa manual: sin ella no hay forma
        // de cotejar rostro contra documento (supuesto declarado en el informe).
        permitirEscribirAMano: false,
      ),
      PasoAlta.cotejo => const PasoCotejo(),
      PasoAlta.perfilTransaccional => const PasoPerfilTransaccional(),
      PasoAlta.contrato => _PasoContrato(
        onIrAlContrato: () async {
          final aceptado = await context.push<bool>('/identidad/contrato');
          if (aceptado == true && context.mounted) {
            context.go('/identidad/bienvenida');
          }
        },
      ),
    };

    return Scaffold(
      appBar: AppBar(
        leading: estado.numeroDePaso > 1
            ? BackButton(onPressed: notifier.atras)
            : null,
      ),
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: BarraDePasos(
                total: estado.totalDePasos,
                actual: estado.numeroDePaso,
                nombre: _nombreDePaso[estado.paso]!,
              ),
            ),
            Expanded(child: SingleChildScrollView(child: cuerpo())),
          ],
        ),
      ),
    );
  }
}

class _PasoContrato extends StatelessWidget {
  const _PasoContrato({required this.onIrAlContrato});
  final VoidCallback onIrAlContrato;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Último paso: leer y aceptar el contrato de adhesión y el '
            'tarifario vigente.',
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: onIrAlContrato,
            child: const Text(TextosIdentidad.continuar),
          ),
        ],
      ),
    );
  }
}
