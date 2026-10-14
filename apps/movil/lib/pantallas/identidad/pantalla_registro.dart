import 'dart:async';

import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/referencia_de_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_alta.dart';
import 'dominio/estado_contrato.dart';
import 'dominio/comportamiento_de_pantalla_del_alta.dart';
import 'dominio/seguimiento_del_alta.dart';
import 'paso_contrato_de_alta.dart';
import 'pasos_alta/cabecera_de_fase.dart';
import 'pasos_alta/paso_cuenta.dart';
import 'pasos_alta/paso_correo.dart';
import 'pasos_alta/paso_documento.dart';
import 'pasos_alta/paso_datos.dart';
import 'pasos_alta/paso_perfil_transaccional.dart';
import 'pasos_alta/transicion_de_paso_alta.dart';
import 'mostrar_celebracion_de_alta.dart';
import 'textos.dart';
import '../../navegacion/retorno_de_invitacion.dart';
import '../../proveedores/haptica.dart';

/// CU-01, el alta en seis pasos y cuatro fases (D-1 de la maqueta). Un solo `Notifier`
/// (`AltaNotifier`) sabe en qué paso está; esta pantalla solo elige qué organismo
/// mostrar. **No es un `StatefulWidget` de 600 líneas** — cada paso es su propio
/// archivo en `pasos_alta/`.
class PantallaDeRegistro extends ConsumerStatefulWidget {
  const PantallaDeRegistro({super.key});

  @override
  ConsumerState<PantallaDeRegistro> createState() => _PantallaDeRegistroState();
}

class _PantallaDeRegistroState extends ConsumerState<PantallaDeRegistro>
    with ComportamientoDePantallaDelAlta<PantallaDeRegistro> {
  var _capturasTransferidas = false;
  late final AltaNotifier _alta;

  @override
  void initState() {
    super.initState();
    _alta = ref.read(altaProvider.notifier);
  }

  @override
  void dispose() {
    if (!_capturasTransferidas) {
      unawaited(_alta.abandonar());
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(altaProvider);
    final notifier = ref.read(altaProvider.notifier);
    ref.listen(altaProvider, alCambiarEstadoDelAlta);

    Widget cuerpo() => switch (estado.paso) {
      PasoAlta.cuenta => const PasoCuenta(),
      PasoAlta.correo => const PasoCorreo(),
      PasoAlta.documento => const PasoDocumento(),
      PasoAlta.datos => const PasoDatos(),
      PasoAlta.actividad => const PasoActividad(),
      PasoAlta.contrato => PasoContratoDeAlta(
        enviando: estado.enviando,
        onIrAlContrato: () async {
          final aceptado = await context.push<bool>('/identidad/contrato');
          if (aceptado != true || !context.mounted) return;

          // Crea la cuenta con el contrato aceptado y la contraseña del paso 2.
          final resultado = await notifier.enviarAlServidor();
          if (!context.mounted) return;
          // Si falló, reintentar conserva la misma clave de idempotencia.
          final usuarioId = resultado.usuarioId;
          if (resultado.error != null || usuarioId == null) return;
          unawaited(
            ref.read(hapticaProvider).exito().catchError((Object _) {}),
          );
          await mostrarCelebracionDeAlta(context);
          if (!context.mounted) return;

          // El alta no abre sesión: las cinco fotos se suben con este `usuarioId`
          // antes de llegar al login, igual que Atlas sube apenas captura — acá se
          // sube en lote porque la persona no existe hasta este punto.
          // Las capturas se copian ANTES de `reiniciar()`, que las vacía.
          ref
              .read(seguimientoDelAltaProvider.notifier)
              .fijar(
                usuarioId,
                ref.read(altaProvider).capturas,
                tokenDeAlta: resultado.tokenDeAlta,
              );
          _capturasTransferidas = true;
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
    final errorArriba = estado.error ?? estado.errorCodigo;
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
                  CabeceraDeFase(
                    numero: estado.numeroDeFase,
                    total: estado.totalDeFases,
                    titulo: estado.paso.fase.titulo,
                  ),
                  // La advertencia va en el primer paso y no en todos: es sobre estos
                  // campos, y repetirla ocho veces la vuelve ruido.
                  if (estado.paso == PasoAlta.cuenta)
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
            Expanded(
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (errorArriba != null)
                      Padding(
                        key: alertaErrorKey,
                        padding: const EdgeInsets.fromLTRB(
                          Espacio.s4,
                          Espacio.s4,
                          Espacio.s4,
                          0,
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            Alerta(tono: Tono.error, titulo: errorArriba),
                            ReferenciaDeError(trazaId: estado.trazaId),
                          ],
                        ),
                      ),
                    TransicionDePasoAlta(
                      child: KeyedSubtree(
                        key: ValueKey(estado.paso),
                        child: cuerpo(),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
