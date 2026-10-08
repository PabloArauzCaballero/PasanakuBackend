import 'dart:async';

import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/barra_de_pasos.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'cerrar_alta.dart';
import 'dominio/estado_alta.dart';
import 'dominio/notifier_alta.dart';
import 'pasos_alta/paso_captura.dart';
import 'pasos_alta/paso_contrasena.dart';
import 'pasos_alta/paso_celular.dart';
import 'pasos_alta/paso_contrato.dart';
import 'pasos_alta/paso_cotejo.dart';
import 'pasos_alta/paso_datos.dart';
import 'pasos_alta/paso_perfil_transaccional.dart';
import 'textos.dart';

/// CU-01, el alta en ocho pasos (D-1 de la maqueta). Un solo `Notifier`
/// (`AltaNotifier`) sabe en qué paso está; esta pantalla solo elige qué organismo
/// mostrar. **No es un `StatefulWidget` de 600 líneas** — cada paso es su propio
/// archivo en `pasos_alta/`.
class PantallaDeRegistro extends ConsumerStatefulWidget {
  const PantallaDeRegistro({super.key});

  @override
  ConsumerState<PantallaDeRegistro> createState() => _PantallaDeRegistroState();
}

class _PantallaDeRegistroState extends ConsumerState<PantallaDeRegistro> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        unawaited(ref.read(altaProvider.notifier).restaurarBorrador());
      }
    });
  }

  static const _nombreDePaso = {
    PasoAlta.datos: TextosIdentidad.pasoDatos,
    PasoAlta.contrasena: TextosIdentidad.pasoContrasena,
    PasoAlta.celular: TextosIdentidad.pasoCelular,
    PasoAlta.anverso: TextosIdentidad.pasoAnverso,
    PasoAlta.reverso: TextosIdentidad.pasoReverso,
    PasoAlta.pruebaDeVida: TextosIdentidad.pasoPruebaDeVida,
    PasoAlta.cotejo: TextosIdentidad.pasoCotejo,
    PasoAlta.perfilTransaccional: TextosIdentidad.pasoPerfil,
    PasoAlta.contrato: TextosIdentidad.pasoContrato,
  };

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(altaProvider);
    final notifier = ref.read(altaProvider.notifier);

    if (estado.restaurando) {
      return Scaffold(
        appBar: AppBar(title: const Text(TextosIdentidad.registroTitulo)),
        body: const SafeArea(
          child: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircularProgressIndicator(),
                SizedBox(height: Espacio.s3),
                Text(TextosIdentidad.registroRecuperando),
              ],
            ),
          ),
        ),
      );
    }

    Widget cuerpo() => switch (estado.paso) {
      PasoAlta.datos => const PasoDatos(),
      PasoAlta.contrasena => const PasoContrasena(),
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
      PasoAlta.contrato => PasoContrato(
        enviando: estado.enviando,
        error: estado.error,
        onIrAlContrato: () => unawaited(cerrarAlta(context, ref)),
      ),
    };

    final t = Tokens.of(context);
    return PopScope<void>(
      canPop: estado.paso == PasoAlta.datos,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) notifier.atras();
      },
      child: Scaffold(
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
          child: SingleChildScrollView(
            key: ValueKey(estado.paso),
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
                if (estado.resumido || estado.errorBorrador != null)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(
                      Espacio.s4,
                      Espacio.s2,
                      Espacio.s4,
                      0,
                    ),
                    child: Alerta(
                      tono: estado.errorBorrador != null
                          ? Tono.aviso
                          : Tono.info,
                      titulo: switch (estado.errorBorrador) {
                        'recuperar' => TextosIdentidad.registroNoRecuperado,
                        'guardar' => TextosIdentidad.registroNoGuardado,
                        'borrar' => TextosIdentidad.registroBorradorNoBorrado,
                        _ => TextosIdentidad.registroReanudado,
                      },
                    ),
                  ),
                cuerpo(),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
