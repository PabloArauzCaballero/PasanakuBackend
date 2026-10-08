import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/organismos/aviso_de_conexion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/conexion.dart';
import 'confirmacion_aporte.dart';
import 'dominio/cu21_cobrar_aporte.dart';
import 'formulario_aporte.dart';
import 'revision_aporte.dart';
import 'textos.dart';

const _anchoMaximoAporte = 560.0;

/// CU-21 · aporte con monto fijo, revisión e idempotencia contra doble cobro.
class PantallaAportar extends ConsumerStatefulWidget {
  const PantallaAportar({
    super.key,
    required this.obligacionId,
    required this.montoDelPeriodo,
  });

  final String obligacionId;
  final String montoDelPeriodo;

  @override
  ConsumerState<PantallaAportar> createState() => _PantallaAportarState();
}

class _PantallaAportarState extends ConsumerState<PantallaAportar> {
  String _canal = 'BILLETERA_MOVIL';
  final _referencia = TextEditingController();
  bool _intentoEnviar = false;
  bool _revisando = false;
  bool _envioIniciado = false;

  @override
  void dispose() {
    _referencia.dispose();
    super.dispose();
  }

  void _irARevision() {
    final referencia = _referencia.text.trim();
    if (referencia.isEmpty) {
      setState(() => _intentoEnviar = true);
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() => _revisando = true);
  }

  void _enviar() {
    if (!_revisando ||
        !ref.read(hayConexionProvider).permiteOperar ||
        ref.read(cobrarAporteProvider(widget.obligacionId)).isLoading) {
      return;
    }
    setState(() => _envioIniciado = true);
    ref
        .read(cobrarAporteProvider(widget.obligacionId).notifier)
        .enviar(
          monto: widget.montoDelPeriodo,
          canal: _canal,
          referenciaProveedor: _referencia.text.trim(),
        );
  }

  @override
  Widget build(BuildContext context) {
    if (!montoDeAporteValido(widget.montoDelPeriodo)) {
      return Scaffold(
        appBar: AppBar(
          title: const _TituloAporte(titulo: TextosPasanaku.tituloAportar),
        ),
        body: const SafeArea(
          child: Center(
            child: SingleChildScrollView(
              child: EstadoVacio(mensaje: ErrorDeMontoNoVerificable.texto),
            ),
          ),
        ),
      );
    }
    final estado = ref.watch(cobrarAporteProvider(widget.obligacionId));
    final resultado = estado.value;
    final conexion = ref.watch(hayConexionProvider);
    final operacionPausada = !conexion.permiteOperar;
    final avisoConexion = operacionPausada
        ? AvisoDeConexion(
            estado: conexion,
            detalleSinConexion: TextosPasanaku.aporteSinConexion,
            reintentar: () => ref.invalidate(hayConexionProvider),
          )
        : null;
    return Scaffold(
      appBar: AppBar(
        title: _TituloAporte(
          titulo: _revisando && resultado == null
              ? TextosPasanaku.tituloRevisionAporte
              : TextosPasanaku.tituloAportar,
        ),
      ),
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, limites) => Center(
            child: SizedBox(
              width: limites.maxWidth > _anchoMaximoAporte
                  ? _anchoMaximoAporte
                  : limites.maxWidth,
              height: limites.maxHeight,
              child: resultado != null
                  ? ConfirmacionAporte(
                      monto: widget.montoDelPeriodo,
                      esNuevo: resultado.esNuevo,
                    )
                  : _revisando
                  ? RevisionAporte(
                      monto: widget.montoDelPeriodo,
                      medio: canalesDeAporte[_canal]!,
                      referencia: _referencia.text.trim(),
                      cargando: estado.isLoading,
                      envioIniciado: _envioIniciado,
                      error: estado.hasError ? estado.error : null,
                      operacionPausada: operacionPausada,
                      avisoConexion: avisoConexion,
                      onConfirmar: _enviar,
                      onEditar: _envioIniciado
                          ? null
                          : () => setState(() => _revisando = false),
                    )
                  : FormularioAporte(
                      monto: widget.montoDelPeriodo,
                      canal: _canal,
                      referencia: _referencia,
                      intentoEnviar: _intentoEnviar,
                      estado: estado,
                      avisoConexion: avisoConexion,
                      onCanalChanged: (valor) => setState(() => _canal = valor),
                      onReferenciaChanged: () => setState(() {}),
                      onRevisar: _irARevision,
                    ),
            ),
          ),
        ),
      ),
    );
  }
}

class _TituloAporte extends StatelessWidget {
  const _TituloAporte({required this.titulo});

  final String titulo;

  @override
  Widget build(BuildContext context) => Align(
    alignment: Alignment.center,
    child: ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: _anchoMaximoAporte),
      child: SizedBox(width: double.infinity, child: Text(titulo)),
    ),
  );
}
