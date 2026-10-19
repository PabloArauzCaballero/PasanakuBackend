import 'dart:async';

import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/referencia_de_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../dominio/errores.dart';
import '../../proveedores/sesion.dart';
import 'dominio/estado_alta.dart';
import 'dominio/estado_sesion.dart' show mensajeDeError, trazaDeError;
import 'dominio/seguimiento_del_alta.dart';
import 'dominio/verificacion_diferida.dart';
import 'pasos_alta/paso_documento.dart';
import 'pasos_alta/paso_perfil_transaccional.dart';
import 'pasos_alta/transicion_de_paso_alta.dart';
import 'textos_de_verificacion.dart';

/// La verificación de identidad desde adentro de la app, para quien creó su cuenta
/// sin hacerla (2026-10-17). Reusa los pasos del registro —carnet y fotos, después la
/// actividad— y al final envía el carnet (`POST /usuarios/{id}/verificacion`) y pasa
/// a la subida de las fotos, la misma pantalla del registro.
class PantallaVerificarIdentidad extends ConsumerStatefulWidget {
  const PantallaVerificarIdentidad({super.key});

  @override
  ConsumerState<PantallaVerificarIdentidad> createState() =>
      _PantallaVerificarIdentidadState();
}

class _PantallaVerificarIdentidadState
    extends ConsumerState<PantallaVerificarIdentidad> {
  late final AltaNotifier _alta;
  var _capturasTransferidas = false;
  var _enviando = false;
  String? _error;
  String? _trazaId;

  @override
  void initState() {
    super.initState();
    _alta = ref.read(altaProvider.notifier);
    WidgetsBinding.instance.addPostFrameCallback(
      (_) => _alta.empezarSoloVerificacion(),
    );
  }

  @override
  void dispose() {
    // Si se va sin enviar, las fotos sacadas no quedan en el teléfono.
    if (!_capturasTransferidas) unawaited(_alta.abandonar());
    super.dispose();
  }

  Future<void> _enviar() async {
    final usuarioId = ref.read(sesionProvider).usuarioIdActual;
    if (usuarioId == null || _enviando) return;
    final estado = ref.read(altaProvider);
    final d = estado.datos;
    setState(() {
      _enviando = true;
      _error = null;
      _trazaId = null;
    });
    try {
      await ref
          .read(iniciarVerificacionProvider)
          .ejecutar(
            usuarioId: usuarioId,
            tipo: d.tipoDocumento,
            numero: d.numeroDocumento,
            lugarExpedicion: d.lugarExpedicion,
            fechaExpiracion: d.fechaExpiracion,
          );
      if (!mounted) return;
      ref
          .read(seguimientoDelAltaProvider.notifier)
          .fijar(usuarioId, estado.capturas);
      _capturasTransferidas = true;
      _alta.reiniciar();
      ref.invalidate(estadoDeCuentaProvider);
      context.go('/registro/subida?usuario=$usuarioId');
    } on Object catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e is ErrorDeApi && e.codigo == 'AP-CU01-03'
            ? 'Ese carnet ya está registrado en otra cuenta. Si es tuyo, '
                  'escribinos desde Ayuda.'
            : mensajeDeError(e);
        _trazaId = trazaDeError(e);
      });
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(altaProvider);
    final enModo = estado.modo == ModoDelAlta.soloVerificacion;
    final cuerpo = !enModo
        ? const SizedBox.shrink()
        : switch (estado.paso) {
            PasoAlta.actividad => const PasoActividad(),
            PasoAlta.contrato => _Envio(
              enviando: _enviando,
              error: _error,
              trazaId: _trazaId,
              onEnviar: _enviar,
            ),
            _ => const PasoDocumento(),
          };
    return Scaffold(
      appBar: AppBar(
        title: const Text(TextosDeVerificacion.verificarTitulo),
        leading: enModo && estado.paso != PasoAlta.documento
            ? BackButton(onPressed: _alta.atras)
            : const CloseButton(),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          child: TransicionDePasoAlta(
            child: KeyedSubtree(key: ValueKey(estado.paso), child: cuerpo),
          ),
        ),
      ),
    );
  }
}

class _Envio extends StatelessWidget {
  const _Envio({
    required this.enviando,
    required this.error,
    required this.trazaId,
    required this.onEnviar,
  });
  final bool enviando;
  final String? error;
  final String? trazaId;
  final VoidCallback onEnviar;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.all(Espacio.s4),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(TextosDeVerificacion.enviarVerificacionDetalle),
        if (error != null) ...[
          const SizedBox(height: Espacio.s3),
          Alerta(tono: Tono.error, titulo: error!),
          ReferenciaDeError(trazaId: trazaId),
        ],
        const SizedBox(height: Espacio.s4),
        Boton(
          key: const ValueKey('enviar-verificacion'),
          texto: enviando
              ? TextosDeVerificacion.enviandoVerificacion
              : TextosDeVerificacion.enviarVerificacion,
          variante: BotonVariante.primario,
          expandido: true,
          cargando: enviando,
          onPressed: enviando ? null : onEnviar,
        ),
      ],
    ),
  );
}
