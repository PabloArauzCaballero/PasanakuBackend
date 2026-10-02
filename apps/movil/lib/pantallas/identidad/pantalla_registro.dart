import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/barra_de_pasos.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_alta.dart';
import 'dominio/estado_contrato.dart';
import 'dominio/seguimiento_del_alta.dart';
import 'pasos_alta/paso_capturas.dart';
import 'pasos_alta/paso_contrasena.dart';
import 'pasos_alta/paso_celular.dart';
import 'pasos_alta/paso_cotejo.dart';
import 'pasos_alta/paso_datos.dart';
import 'pasos_alta/paso_perfil_transaccional.dart';
import 'textos.dart';
import 'textos_del_alta.dart';
import '../../navegacion/retorno_de_invitacion.dart';

/// CU-01, el alta en ocho pasos (D-1 de la maqueta). Un solo `Notifier`
/// (`AltaNotifier`) sabe en qué paso está; esta pantalla solo elige qué organismo
/// mostrar. **No es un `StatefulWidget` de 600 líneas** — cada paso es su propio
/// archivo en `pasos_alta/`.
class PantallaDeRegistro extends ConsumerWidget {
  const PantallaDeRegistro({super.key});

  static const _nombreDePaso = {
    PasoAlta.datos: TextosIdentidad.pasoDatos,
    PasoAlta.contrasena: TextosIdentidad.pasoContrasena,
    PasoAlta.celular: TextosIdentidad.pasoCelular,
    PasoAlta.capturas: TextosIdentidad.pasoCapturas,
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
      PasoAlta.contrasena => const PasoContrasena(),
      PasoAlta.celular => const PasoCelular(),
      PasoAlta.capturas => PasoCapturas(onContinuar: notifier.siguiente),
      PasoAlta.cotejo => const PasoCotejo(),
      PasoAlta.perfilTransaccional => const PasoPerfilTransaccional(),
      PasoAlta.contrato => _PasoContrato(
        enviando: estado.enviando,
        error: estado.error,
        onIrAlContrato: () async {
          final aceptado = await context.push<bool>('/identidad/contrato');
          if (aceptado != true || !context.mounted) return;

          // Crea la cuenta con el contrato aceptado y la contraseña del paso 2.
          final resultado = await notifier.enviarAlServidor();
          if (!context.mounted) return;
          // Si falló, reintentar conserva la misma clave de idempotencia.
          final usuarioId = resultado.usuarioId;
          if (resultado.error != null || usuarioId == null) return;

          // El alta no abre sesión: las cinco fotos se suben con este `usuarioId`
          // antes de llegar al login, igual que Atlas sube apenas captura — acá se
          // sube en lote porque la persona no existe hasta este punto.
          // Las capturas se copian ANTES de `reiniciar()`, que las vacía.
          ref
              .read(seguimientoDelAltaProvider.notifier)
              .fijar(usuarioId, ref.read(altaProvider).capturas);
          notifier.reiniciar();
          ref.read(contratoProvider.notifier).reiniciar();
          final retorno = retornoDeInvitacion(
            GoRouterState.of(context).uri.queryParameters['volver'],
          );
          context.go(
            retorno == null
                ? '/registro/subida?usuario=$usuarioId'
                : '/registro/subida?usuario=$usuarioId&volver=${Uri.encodeComponent(retorno)}',
          );
        },
      ),
    };

    final t = Tokens.of(context);
    return Scaffold(
      appBar: AppBar(
        // Sin título, la pantalla arrancaba con una flecha suelta sobre una barra de
        // pasos: quien llega acá desde «Crear mi cuenta» no leía en ningún lado que
        // está creando una cuenta.
        title: const Text(TextosIdentidad.registroTitulo),
        leading: estado.numeroDePaso > 1
            ? BackButton(onPressed: notifier.atras)
            : null,
      ),
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  BarraDePasos(
                    total: estado.totalDePasos,
                    actual: estado.numeroDePaso,
                    nombre: _nombreDePaso[estado.paso]!,
                  ),
                  // La advertencia va en el primer paso y no en todos: es sobre estos
                  // campos, y repetirla ocho veces la vuelve ruido.
                  if (estado.paso == PasoAlta.datos)
                    Padding(
                      padding: const EdgeInsets.only(top: Espacio.s3),
                      child: Text(
                        TextosIdentidad.registroIntro,
                        style: Tipo.cuerpoChico.copyWith(color: t.text2),
                      ),
                    ),
                ],
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
  const _PasoContrato({
    required this.onIrAlContrato,
    required this.enviando,
    required this.error,
  });
  final VoidCallback onIrAlContrato;
  final bool enviando;

  /// Lo que devolvió `POST /usuarios` si falló. Se muestra acá y no en un cartel que
  /// se va solo: el alta quedó a un toque de completarse y hay que poder reintentarla.
  final String? error;

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
          if (error != null) ...[
            const SizedBox(height: Espacio.s4),
            Alerta(tono: Tono.error, titulo: error!),
          ],
          const SizedBox(height: Espacio.s4),
          Boton(
            texto: enviando
                ? TextosDelAlta.altaEnviando
                : TextosIdentidad.continuar,
            variante: BotonVariante.primario,
            expandido: true,
            cargando: enviando,
            onPressed: enviando ? null : onIrAlContrato,
          ),
        ],
      ),
    );
  }
}
