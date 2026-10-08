import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_sesion.dart';
import 'textos_mfa.dart';
import '../../navegacion/retorno_de_invitacion.dart';

const _anchoMaximoMfa = 560.0;

/// CU-04, paso 2: el segundo factor. Seis dígitos, el mismo átomo del celular en
/// el alta — es a propósito, es el mismo patrón (`CampoOTP`).
///
/// Antes esta pantalla avanzaba con cualquier código: guardaba los seis dígitos en
/// memoria y navegaba. Ahora los manda al servidor y **solo avanza si el servidor los
/// acepta**; si no, el error se muestra acá y el campo queda listo para reintentar.
class PantallaDeMfa extends ConsumerWidget {
  const PantallaDeMfa({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(sesionIdentidadProvider);
    if (estado.paso != PasoSesion.mfa) {
      return _MfaSinDesafio(
        sesionAbierta: estado.paso == PasoSesion.autenticada,
      );
    }
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          children: [
            Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: _anchoMaximoMfa),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const CabeceraDeSeccion(titulo: TextosMfa.titulo),
                    const SizedBox(height: Espacio.s5),
                    Text(
                      TextosMfa.codigoDeSeisDigitos,
                      style: Tipo.cuerpo.copyWith(
                        color: Tokens.of(context).text,
                      ),
                    ),
                    const SizedBox(height: Espacio.s2),
                    IgnorePointer(
                      ignoring: estado.enviando,
                      child: CampoOTP(
                        error: estado.error,
                        onCompleto: (codigo) async {
                          final ok = await ref
                              .read(sesionIdentidadProvider.notifier)
                              .completarMfa(codigo);
                          if (!ok || !context.mounted) return;
                          final retorno = retornoDeInvitacion(
                            GoRouterState.of(
                              context,
                            ).uri.queryParameters['volver'],
                          );
                          context.go(retorno ?? '/billetera/inicio');
                        },
                      ),
                    ),
                    if (estado.enviando) ...[
                      const SizedBox(height: Espacio.s4),
                      Text(
                        TextosMfa.verificando,
                        style: Tipo.cuerpoChico.copyWith(
                          color: Tokens.of(context).text2,
                        ),
                      ),
                      const SizedBox(height: Espacio.s2),
                      const LinearProgressIndicator(
                        semanticsLabel: TextosMfa.verificando,
                      ),
                    ],
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

class _MfaSinDesafio extends StatelessWidget {
  const _MfaSinDesafio({required this.sesionAbierta});

  final bool sesionAbierta;

  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      bottom: false,
      child: Column(
        children: [
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(Espacio.s4),
              children: [
                Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(
                      maxWidth: _anchoMaximoMfa,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const CabeceraDeSeccion(titulo: TextosMfa.titulo),
                        const SizedBox(height: Espacio.s4),
                        Alerta(
                          titulo: TextosMfa.sinDesafio,
                          detalle: sesionAbierta
                              ? TextosMfa.yaAutenticadaDetalle
                              : TextosMfa.sinDesafioDetalle,
                          tono: Tono.info,
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          BarraDeAccion(
            hijo: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: _anchoMaximoMfa),
                child: Boton(
                  texto: sesionAbierta
                      ? TextosMfa.irAlInicio
                      : TextosMfa.volverAIngresar,
                  variante: BotonVariante.primario,
                  expandido: true,
                  onPressed: () => context.go(
                    sesionAbierta ? '/billetera/inicio' : '/ingreso',
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    ),
  );
}
