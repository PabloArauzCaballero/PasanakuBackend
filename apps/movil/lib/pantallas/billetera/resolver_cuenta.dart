import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../dominio/cu13_cuenta_propia.dart';
import '../../dominio/errores.dart';
import '../identidad/dominio/kyc_del_titular.dart';
import '../identidad/boton_consultar_estado.dart';
import '../identidad/textos_de_verificacion.dart';
import '../../proveedores/sesion.dart';
import 'recargable.dart';
import 'textos.dart';

const _codigoPermisoDenegado = 'AP-SEG-01';

/// Entrega a [construir] el id de la cuenta de la persona.
///
/// Si la ruta ya trae `?cuenta=` (la pantalla se abrió desde otra que la conoce) se usa tal cual y
/// no se pregunta nada. Si no, se pregunta al servidor `GET /billetera/cuentas/propia`: la app
/// nunca supone una cuenta (antes consultaba un id fijo y, contra el servidor real, leía como falla
/// del servidor lo que era «todavía no tenés billetera activa»).
///
/// Los estados: cargando, billetera todavía no activa (no es una falla: se dice y se puede volver a
/// revisar), y error accionable con reintento y código de seguimiento.
class ResolverCuenta extends ConsumerWidget {
  const ResolverCuenta({super.key, this.cuentaId, required this.construir});

  final String? cuentaId;
  final Widget Function(String cuentaId) construir;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final conocida = cuentaId;
    if (conocida != null && conocida.isNotEmpty) return construir(conocida);
    final valor = ref.watch(cuentaPropiaProvider);
    void revisar() => ref.invalidate(cuentaPropiaProvider);
    final usuarioId = ref.read(sesionProvider).usuarioIdActual;
    // Deslizar hacia abajo vuelve a preguntar todo: la cuenta y, si está en revisión,
    // el estado de la verificación (2026-10-19).
    Future<void> recargar() async {
      if (usuarioId != null) ref.invalidate(kycDelTitularProvider(usuarioId));
      ref.invalidate(cuentaPropiaProvider);
      try {
        await ref.read(cuentaPropiaProvider.future);
      } on Object {
        // El estado de error ya lo pinta la pantalla.
      }
    }

    Widget recargable(Widget hijo) =>
        Recargable(alRecargar: recargar, hijo: hijo);
    final error = valor.hasError ? valor.error : null;
    if (error is ErrorDeApi &&
        error.estado == 403 &&
        error.codigo == _codigoPermisoDenegado) {
      if (usuarioId != null) {
        final verificacion = ref.watch(kycDelTitularProvider(usuarioId));
        return recargable(
          verificacion.when(
            loading: () => const Center(
              child: CircularProgressIndicator(
                semanticsLabel: 'Consultando tu verificación',
              ),
            ),
            error: (_, _) => EstadoVacio(
              mensaje: 'No pudimos confirmar el estado de tu verificación.',
              accion: Boton(
                texto: TextosBilletera.revisarDeNuevo,
                variante: BotonVariante.fantasma,
                onPressed: () {
                  ref.invalidate(kycDelTitularProvider(usuarioId));
                  revisar();
                },
              ),
            ),
            data: (estado) => _estadoSinAcceso(context, ref, estado, revisar),
          ),
        );
      }
    }
    if (valor.hasValue && valor.value == null) {
      return recargable(
        EstadoVacio(
          mensaje: TextosBilletera.billeteraInactiva,
          accion: Boton(
            texto: TextosBilletera.revisarDeNuevo,
            variante: BotonVariante.fantasma,
            onPressed: revisar,
          ),
        ),
      );
    }
    final estado = EstadoDePantalla<CuentaPropia?>(
      valor: valor,
      etiquetaDeCarga: TextosBilletera.buscandoBilletera,
      etiquetaDeTraza: 'Código de seguimiento',
      mensajeVacio: TextosBilletera.billeteraInactiva,
      reintentar: revisar,
      exito: (cuenta) => construir(cuenta!.cuentaId),
    );
    // Con la cuenta resuelta, la pantalla de adentro maneja su propio gesto.
    return valor.hasValue ? estado : recargable(estado);
  }
}

Widget _estadoSinAcceso(
  BuildContext context,
  WidgetRef ref,
  EstadoDeVerificacion estado,
  VoidCallback revisar,
) {
  final (mensaje, accion) = switch (estado.estado) {
    // «Completar verificación» solo si de verdad falta algo del expediente. Con todo
    // enviado, lo único que se puede hacer es preguntar si ya lo aprobaron
    // (2026-10-19: el botón llevaba a un formulario sin nada para completar).
    EstadoDeVerificacionEstadoEnum.PENDIENTE ||
    EstadoDeVerificacionEstadoEnum.EN_REVISION => (
      '${TextosDeVerificacion.cuentaEnVerificacionTitulo}. '
          '${TextosDeVerificacion.cuentaEnVerificacionDetalle}',
      pendientesDeKyc(estado).isEmpty
          ? BotonConsultarEstado(alActivar: revisar)
          : Boton(
              texto: 'Completar verificación',
              variante: BotonVariante.primario,
              onPressed: () => context.push('/identidad/perfil/verificacion'),
            ),
    ),
    // Aprobada pero el acceso de la sesión todavía no lo sabe: se activa ahí mismo,
    // sin cerrar la sesión.
    EstadoDeVerificacionEstadoEnum.APROBADA => (
      TextosDeVerificacion.aprobadaConsultar,
      BotonConsultarEstado(alActivar: revisar),
    ),
    EstadoDeVerificacionEstadoEnum.RECHAZADA => (
      TextosDeVerificacion.rechazadaTitulo,
      Boton(
        texto: TextosBilletera.revisarDeNuevo,
        variante: BotonVariante.fantasma,
        onPressed: revisar,
      ),
    ),
  };
  return EstadoVacio(mensaje: mensaje, accion: accion);
}

/// Lo mismo para las pantallas interiores (recargar, retirar, transferir, extracto): mientras la
/// cuenta no está resuelta, un marco con el título y el estado; resuelta, la pantalla real.
class PantallaConCuenta extends StatelessWidget {
  const PantallaConCuenta({
    super.key,
    required this.titulo,
    required this.cuentaId,
    required this.construir,
  });

  final String titulo;
  final String? cuentaId;
  final Widget Function(String cuentaId) construir;

  @override
  Widget build(BuildContext context) {
    if (cuentaId != null && cuentaId!.isNotEmpty) return construir(cuentaId!);
    return Scaffold(
      appBar: AppBar(title: Text(titulo)),
      body: SafeArea(child: ResolverCuenta(construir: construir)),
    );
  }
}
