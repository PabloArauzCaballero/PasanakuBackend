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
                    error: estado.error,
                    onCambio: (c) => setState(() => _codigo = c),
                    onCompleto: _confirmar,
                  ),
                ],
              ),
            ),
            BarraDeAccion(
              hijo: Boton(
                texto: TextosIdentidad.confirmarMfa,
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
