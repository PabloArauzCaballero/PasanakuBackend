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
import 'pasos_alta/paso_captura.dart';
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
      PasoAlta.contrato => _PasoContrato(
        enviando: estado.enviando,
        error: estado.error,
        onIrAlContrato: () async {
          final aceptado = await context.push<bool>('/identidad/contrato');
          if (aceptado != true || !context.mounted) return;

          // **Acá se crea la cuenta de verdad.** `POST /usuarios`, con los tres ids de
          // contrato que devolvió el servidor y la contraseña del paso 2. Antes este
          // método existía y no lo llamaba nadie: el alta terminaba sin que el backend
          // se enterara, y la app mandaba a la billetera de una cuenta inexistente.
          await notifier.enviarAlServidor();
          if (!context.mounted) return;
          // Si falló, se queda acá y lo dice: el contrato ya está aceptado y volver a
          // tocar «Continuar» reintenta el mismo alta, con la misma clave de
          // idempotencia, así que no crea dos personas.
          if (ref.read(altaProvider).error != null) return;

          // **Al login, no a la app.** Terminar el alta no es tener sesión: se pidió
          // abrir una cuenta y todavía no se entró a ninguna. Antes esto iba a la
          // bienvenida, que está dentro del shell, así que el alta desembocaba en la
          // billetera con la barra de pestañas puesta y sin token — y la primera
          // pantalla que se veía era «Tu sesión venció. Volvé a ingresar.»
          //
          // El asistente se vacía al salir: si no, volver a «Crear mi cuenta» retoma
          // el formulario anterior a mitad de camino, con los datos de otra persona.
          notifier.reiniciar();
          ref.read(contratoProvider.notifier).reiniciar();
          final retorno = retornoDeInvitacion(
            GoRouterState.of(context).uri.queryParameters['volver'],
          );
          context.go(
            retorno == null
                ? '/ingreso?alta=lista'
                : '/ingreso?alta=lista&volver=${Uri.encodeComponent(retorno)}',
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
