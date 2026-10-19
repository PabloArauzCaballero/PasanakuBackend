import 'dart:async';

import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../navegacion/acceso_a_rutas.dart';
import '../../proveedores/sesion.dart';
import 'dominio/capturas_del_expediente.dart';
import 'dominio/seguimiento_del_alta.dart';
import 'dominio/subida_del_expediente.dart';
import 'fila_de_subida.dart';
import 'pasos_alta/sacar_captura.dart';
import 'textos_de_subida.dart';

/// La subida en lote de las cinco fotos ya validadas, con la misma máquina de
/// estados de Atlas: "Subiendo…", "está tardando" a los 10 s, y "Reintentar" o
/// "Repetir la foto" ante un fallo.
class PantallaDeSubidaDelExpediente extends ConsumerStatefulWidget {
  const PantallaDeSubidaDelExpediente({super.key, required this.usuarioId});
  final String usuarioId;

  @override
  ConsumerState<PantallaDeSubidaDelExpediente> createState() =>
      _PantallaDeSubidaDelExpedienteState();
}

class _PantallaDeSubidaDelExpedienteState
    extends ConsumerState<PantallaDeSubidaDelExpediente> {
  late final SubidaNotifier _notificadorDeSubida;
  late final SeguimientoDelAlta _seguimientoDelAlta;
  var _estadoDeSubida = const EstadoDeLaSubida();

  @override
  void initState() {
    super.initState();
    _notificadorDeSubida = ref.read(subidaProvider.notifier);
    _seguimientoDelAlta = ref.read(seguimientoDelAltaProvider.notifier);
    _estadoDeSubida = ref.read(subidaProvider);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _subir();
    });
  }

  Future<void> _subir() async {
    final capturas = _capturas(ref.read(seguimientoDelAltaProvider));
    await ref
        .read(subidaProvider.notifier)
        .subirPendientes(
          usuarioId: widget.usuarioId,
          capturas: capturas,
          tokenDeAlta: ref.read(seguimientoDelAltaProvider)?.tokenDeAlta,
        );
    await _borrarSiCompleta();
  }

  Future<void> _borrarSiCompleta() async {
    if (_estadoDeSubida.completa) {
      await _seguimientoDelAlta.liberarCapturas();
    }
  }

  Future<void> _reintentar(CaraDelCarril cara) async {
    final captura = _capturas(
      ref.read(seguimientoDelAltaProvider),
    ).porCara[cara];
    if (captura == null) return;
    await ref
        .read(subidaProvider.notifier)
        .reintentar(
          usuarioId: widget.usuarioId,
          cara: cara,
          ruta: captura.ruta,
          tokenDeAlta: ref.read(seguimientoDelAltaProvider)?.tokenDeAlta,
        );
    await _borrarSiCompleta();
  }

  Future<void> _repetir(CaraDelCarril cara) async {
    ref.read(subidaProvider.notifier).rotarClaveParaRepetir(cara);
    final lograda = await sacarCaptura(context, cara, listas: {cara});
    final nueva = lograda is Map<CaraDelCarril, Captura> ? lograda[cara] : null;
    if (!mounted || nueva == null) return;
    await ref
        .read(seguimientoDelAltaProvider.notifier)
        .reemplazarCaptura(cara, nueva);
    await ref
        .read(subidaProvider.notifier)
        .reintentar(
          usuarioId: widget.usuarioId,
          cara: cara,
          ruta: nueva.ruta,
          tokenDeAlta: ref.read(seguimientoDelAltaProvider)?.tokenDeAlta,
        );
    await _borrarSiCompleta();
  }

  @override
  void dispose() {
    if (_estadoDeSubida.subiendoAlguna) {
      _notificadorDeSubida.cancelarPendientes();
    }
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_seguimientoDelAlta.liberarCapturas());
    });
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(subidaProvider, (_, siguiente) => _estadoDeSubida = siguiente);
    final subida = ref.watch(subidaProvider);
    final capturas = _capturas(ref.watch(seguimientoDelAltaProvider));
    final escalaTexto = MediaQuery.textScalerOf(context).scale(1);
    final textoGrande = escalaTexto >= 1.8;
    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        toolbarHeight: textoGrande ? 112 : null,
        title: Text(
          textoGrande
              ? subida.completa
                    ? 'Tus documentos\nestán enviados'
                    : 'Enviando tus\ndocumentos'
              : subida.completa
              ? TextosDeSubida.tituloEnviados
              : TextosDeSubida.tituloSubiendo,
          maxLines: textoGrande ? 2 : 1,
          overflow: textoGrande ? TextOverflow.visible : TextOverflow.ellipsis,
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: ListView(
                  children: [
                    for (final cara in CapturasDelExpediente.orden)
                      FilaDeSubida(
                        cara: cara,
                        estado: subida.porCara[cara],
                        ruta: capturas.porCara[cara]?.ruta,
                        onCancelar: () =>
                            ref.read(subidaProvider.notifier).cancelar(cara),
                        onReintentar: () => _reintentar(cara),
                        onRepetir: () => _repetir(cara),
                      ),
                  ],
                ),
              ),
              // Una foto que no sube no frena el alta (`subida_del_expediente.dart`
              // original): la cuenta ya existe y el backoffice ve el expediente
              // incompleto, que es recuperable. "Continuar" solo espera a que no
              // quede ninguna subida EN CURSO, no a que las cinco hayan salido bien.
              Boton(
                texto: TextosDeSubida.continuar,
                variante: BotonVariante.primario,
                expandido: true,
                onPressed: subida.subiendoAlguna
                    ? null
                    // Con sesión (la verificación se hizo desde adentro de la
                    // app) el estado es el de la sesión, que lleva de vuelta a
                    // la app; en el registro, el del alta.
                    : () => context.go(
                        ref.read(sesionProvider).estado == EstadoDeSesion.activa
                            ? rutaDeVerificacionDeSesion
                            : '/registro/estado?usuario=${widget.usuarioId}',
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Sin seguimiento (p. ej. la app se reabrió en esta ruta) no hay nada que subir.
CapturasDelExpediente _capturas(AltaEnSeguimiento? seguimiento) =>
    seguimiento?.capturas ?? const CapturasDelExpediente();
