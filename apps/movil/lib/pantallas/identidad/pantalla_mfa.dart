import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_sesion.dart';
import 'textos.dart';
import 'textos_del_alta.dart';
import '../../navegacion/retorno_de_invitacion.dart';

/// CU-04, paso 2: el segundo factor. Seis dígitos, el mismo átomo del celular en
/// el alta — es a propósito, es el mismo patrón (`CampoOTP`).
///
/// Antes esta pantalla avanzaba con cualquier código: guardaba los seis dígitos en
/// memoria y navegaba. Ahora los manda al servidor y **solo avanza si el servidor los
/// acepta**; si no, el error se muestra acá y el campo queda listo para reintentar.
///
/// Dice para qué se pide el código y tiene un botón «Confirmar» (carril H3: antes eran seis
/// casillas sueltas, sin explicación ni acción, y no quedaba claro qué se esperaba). Al completar
/// las seis casillas el código se envía solo, como siempre; el botón sirve para reintentar el mismo
/// código tras un rechazo o cuando la persona prefiere confirmar a mano.
class PantallaDeMfa extends ConsumerStatefulWidget {
  const PantallaDeMfa({super.key});

  @override
  ConsumerState<PantallaDeMfa> createState() => _PantallaDeMfaState();
}

class _PantallaDeMfaState extends ConsumerState<PantallaDeMfa> {
  String _codigo = '';
  String? _avisoReenvio;

  Future<void> _confirmar(String codigo) async {
    final ok = await ref
        .read(sesionIdentidadProvider.notifier)
        .completarMfa(codigo);
    if (!ok || !mounted) return;
    final retorno = retornoDeInvitacion(
      GoRouterState.of(context).uri.queryParameters['volver'],
    );
    context.go(retorno ?? '/billetera/inicio');
  }

  Future<void> _reenviar() async {
    setState(() => _avisoReenvio = null);
    final ok = await ref
        .read(sesionIdentidadProvider.notifier)
        .reenviarCodigoMfa();
    if (!mounted) return;
    final envioConfirmado = ref.read(sesionIdentidadProvider).correoMfaEnviado;
    setState(() {
      _avisoReenvio = !ok
          ? null
          : envioConfirmado
          ? 'El correo de verificacion fue aceptado para envio. Revisa Recibidos y Spam.'
          : 'El servidor no confirmo un envio al correo. Revisa el metodo configurado o contacta a soporte.';
    });
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(sesionIdentidadProvider);
    final t = Tokens.of(context);
    return Scaffold(
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
                children: [
                  const CabeceraDeSeccion(titulo: TextosIdentidad.tituloMfa),
                  const SizedBox(height: Espacio.s3),
                  Text(
                    TextosIdentidad.explicacionMfa,
                    style: Tipo.cuerpo.copyWith(color: t.text2),
                  ),
                  const SizedBox(height: Espacio.s5),
                  CampoOTP(
                    etiqueta: TextosIdentidad.etiquetaCodigoMfa,
                    ejemplo: TextosDelAlta.ejemploCodigo,
                    error: estado.error,
                    habilitado: !estado.enviando,
                    onCambio: (c) => setState(() => _codigo = c),
                    onCompleto: _confirmar,
                  ),
                  const SizedBox(height: Espacio.s4),
                  Text(
                    TextosIdentidad.mfaSinCodigoAyuda,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                  const SizedBox(height: Espacio.s2),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: TextButton(
                      onPressed: estado.enviando ? null : _reenviar,
                      child: const Text('Reenviar código por correo'),
                    ),
                  ),
                  if (_avisoReenvio != null)
                    Text(
                      _avisoReenvio!,
                      style: Tipo.cuerpo.copyWith(color: t.text2),
                    ),
                ],
              ),
            ),
            BarraDeAccion(
              hijo: Boton(
                // Mientras se verifica, el botón dice qué está pasando (0922bade; se
                // había perdido en el merge 6f88a950).
                texto: estado.enviando
                    ? TextosIdentidad.verificandoCodigo
                    : TextosIdentidad.confirmarMfa,
                variante: BotonVariante.primario,
                expandido: true,
                cargando: estado.enviando,
                onPressed: _codigo.length == 6 && !estado.enviando
                    ? () => _confirmar(_codigo)
                    : null,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
