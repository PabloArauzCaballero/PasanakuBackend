import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../dominio/cliente.dart';
import '../../navegacion/retorno_de_invitacion.dart';
import '../../proveedores/idempotencia.dart';
import '../../proveedores/sesion.dart';
import 'accion_error_invitacion.dart';
import 'acceso_para_invitacion.dart';
import 'contenido_de_invitacion.dart';
import 'detalle_de_invitacion.dart';
import 'dominio/detalle_visible_de_invitacion.dart';
import 'dominio/fallo_consulta_invitacion.dart';
import 'dominio/respuesta_aceptacion_invitacion.dart';

/// Entrada pública; el servidor valida al destinatario antes de mostrar datos.
class PantallaUnirse extends ConsumerStatefulWidget {
  const PantallaUnirse({super.key, required this.codigo});
  final String codigo;

  @override
  ConsumerState<PantallaUnirse> createState() => _PantallaUnirseState();
}

class _PantallaUnirseState extends ConsumerState<PantallaUnirse> {
  bool _cargando = true;
  bool _necesitaIngreso = false;
  bool _aceptaReglamento = false;
  bool _enviando = false;
  bool _consultaEnVuelo = false;
  bool _enlaceNoRecuperable = false;
  String? _error, _grupoAceptado;
  DetalleVisibleDeInvitacion? _detalle;

  RegExpMatch? get _partes => formatoCodigoInvitacion.firstMatch(widget.codigo);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _consultar());
  }

  Future<void> _consultar() async {
    if (_consultaEnVuelo) return;
    _consultaEnVuelo = true;
    try {
      final partes = _partes;
      if (partes == null) {
        setState(() {
          _cargando = false;
          _error =
              'El enlace de invitación no es válido. Pedí uno nuevo a quien te invitó.';
        });
        return;
      }
      setState(() {
        _cargando = true;
        _error = null;
        _enlaceNoRecuperable = false;
      });
      String? acceso;
      try {
        acceso = await ref.read(sesionProvider).tokenDeAcceso();
      } on Object {
        acceso = null;
      }
      if (!mounted) return;
      if (acceso == null || acceso.isEmpty) {
        setState(() {
          _cargando = false;
          _necesitaIngreso = true;
        });
        return;
      }
      try {
        final respuesta = await ref
            .read(dioProvider)
            .post<Map<String, dynamic>>(
              '/grupos/invitaciones/enlace/consultar',
              data: {'tokenId': partes.group(1), 'token': partes.group(2)},
            );
        final detalle = DetalleVisibleDeInvitacion.desde(respuesta.data);
        if (!mounted) return;
        setState(() {
          _cargando = false;
          _detalle = detalle;
          _error = null;
        });
      } on Object catch (error) {
        if (!mounted) return;
        final fallo = FalloConsultaInvitacion.desde(error);
        setState(() {
          _cargando = false;
          _error = fallo.mensaje;
          _necesitaIngreso = fallo.requiereIngreso;
          _enlaceNoRecuperable = !fallo.recuperable;
        });
      }
    } finally {
      _consultaEnVuelo = false;
    }
  }

  Future<void> _aceptar() async {
    if (_enviando ||
        !_aceptaReglamento ||
        _detalle == null ||
        _partes == null) {
      return;
    }
    setState(() {
      _enviando = true;
      _error = null;
    });
    final partes = _partes!;
    final formulario = 'cu69-aceptar-${partes.group(1)}';
    final clave = ref.read(idempotenciaProvider.notifier).claveDe(formulario);
    try {
      final respuesta = await ref
          .read(dioProvider)
          .post<Map<String, dynamic>>(
            '/grupos/invitaciones/enlace/aceptar',
            data: {
              'tokenId': partes.group(1),
              'token': partes.group(2),
              'hashReglamento': _detalle!.hashReglamento,
              'aceptaReglamento': true,
            },
            options: Options(headers: {'Idempotency-Key': clave}),
          );
      final grupoAceptado = grupoDeAceptacionConfirmada(respuesta.data);
      ref.read(idempotenciaProvider.notifier).cerrar(formulario);
      if (!mounted) return;
      setState(() {
        _grupoAceptado = grupoAceptado;
        _detalle = null;
        _error = null;
      });
    } on Object {
      if (!mounted) return;
      setState(
        () => _error =
            'No pudimos confirmar tu ingreso. Podés volver a intentarlo sin duplicarlo.',
      );
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Invitación al grupo')),
      body: SafeArea(
        child: ContenidoDeInvitacion(
          children: [
            if (_cargando) const Center(child: CircularProgressIndicator()),
            if (_necesitaIngreso) AccesoParaInvitacion(codigo: widget.codigo),
            if (_detalle case final detalle?)
              DetalleDeInvitacion(
                detalle: detalle,
                aceptaReglamento: _aceptaReglamento,
                enviando: _enviando,
                alCambiarAceptacion: (valor) =>
                    setState(() => _aceptaReglamento = valor ?? false),
                alAceptar: _aceptar,
              ),
            if (_grupoAceptado != null) ...[
              const Text('Ya formás parte del grupo.'),
              TextButton(
                onPressed: () => context.go('/portada'),
                child: const Text('Ir a portada'),
              ),
            ],
            if (_error != null) ...[
              Text(
                _error!,
                style: TextStyle(color: Tokens.of(context).errTexto),
              ),
              if (_detalle == null && !_necesitaIngreso)
                AccionErrorInvitacion(
                  recuperable: _partes != null && !_enlaceNoRecuperable,
                  onPressed: _partes == null || _enlaceNoRecuperable
                      ? () => context.go('/portada')
                      : _consultar,
                ),
            ],
          ],
        ),
      ),
    );
  }
}
