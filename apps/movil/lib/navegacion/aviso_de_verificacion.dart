import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../pantallas/identidad/dominio/kyc_del_titular.dart';
import '../pantallas/identidad/dominio/verificacion_diferida.dart';
import '../pantallas/identidad/textos_de_verificacion.dart';
import '../proveedores/sesion.dart';
import 'acceso_a_rutas.dart';

/// Mientras la identidad está en revisión, la persona usa la app igual y este aviso,
/// arriba de todas las pestañas, le dice qué falta. Tocarlo abre el estado de la
/// verificación (donde se activa la cuenta cuando la aprueban). Sin revisión
/// pendiente, devuelve [hijo] tal cual.
class AvisoDeVerificacion extends ConsumerWidget {
  const AvisoDeVerificacion({super.key, required this.hijo});
  final Widget hijo;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final sesion = ref.watch(sesionProvider);
    return ListenableBuilder(
      listenable: sesion,
      builder: (context, _) {
        final usuarioId = sesion.usuarioIdActual;
        if (!sesion.requiereVerificacion || usuarioId == null) return hijo;
        // Sin carnet enviado (la persona saltó la verificación al registrarse) el
        // aviso invita a verificarse; no dice «estamos verificando» algo que no llegó.
        final sinVerificar =
            ref.watch(estadoDeCuentaProvider).asData?.value ==
            EstadoDeCuenta.sinVerificar;
        final estado = ref
            .watch(kycDelTitularProvider(usuarioId))
            .asData
            ?.value
            .estado;
        final (texto, tono) = sinVerificar
            ? (TextosDeVerificacion.avisoSinVerificar, Tono.info)
            : switch (estado) {
                EstadoDeVerificacionEstadoEnum.APROBADA => (
                  TextosDeVerificacion.avisoAprobada,
                  Tono.ok,
                ),
                EstadoDeVerificacionEstadoEnum.RECHAZADA => (
                  TextosDeVerificacion.avisoRechazada,
                  Tono.aviso,
                ),
                _ => (TextosDeVerificacion.avisoVerificando, Tono.info),
              };
        return Column(
          children: [
            SafeArea(
              bottom: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(
                  Espacio.s4,
                  Espacio.s2,
                  Espacio.s4,
                  0,
                ),
                child: Semantics(
                  button: true,
                  child: InkWell(
                    key: const ValueKey('aviso-de-verificacion'),
                    borderRadius: BorderRadius.circular(Radios.md),
                    onTap: () => context.push(
                      sinVerificar
                          ? rutaDeVerificarIdentidad
                          : rutaDeVerificacionDeSesion,
                    ),
                    child: Alerta(titulo: texto, tono: tono),
                  ),
                ),
              ),
            ),
            Expanded(
              child: MediaQuery.removePadding(
                context: context,
                removeTop: true,
                child: hijo,
              ),
            ),
          ],
        );
      },
    );
  }
}
