import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../dominio/cliente.dart';
import '../../proveedores/idempotencia.dart';
import '../../proveedores/sesion.dart';
import 'textos.dart';

/// Entrada pública del enlace. Los datos del grupo llegan solo tras comprobar en
/// el servidor que el teléfono autenticado es el destinatario de la invitación.
///
/// Diseño (hallazgo del carril H): antes era texto plano pegado al borde, con el importe
/// crudo («100.00 BOB … MENSUAL») y el botón principal casi invisible mientras estaba
/// apagado. Ahora usa la cabecera, las tarjetas, la casilla y la barra de acción del
/// sistema de diseño, igual que «Organizar un grupo».
class PantallaUnirse extends ConsumerStatefulWidget {
  const PantallaUnirse({super.key, required this.codigo});
  final String codigo;

  @override
  ConsumerState<PantallaUnirse> createState() => _PantallaUnirseState();
}

class _PantallaUnirseState extends ConsumerState<PantallaUnirse> {
  static final _formato = RegExp(
    r'^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\.([0-9a-f]{64})$',
  );

  static const _cadaCuanto = {
    'SEMANAL': 'por semana',
    'QUINCENAL': 'cada quincena',
    'MENSUAL': 'por mes',
    'BIMENSUAL': 'cada dos meses',
  };

  bool _cargando = true;
  bool _necesitaIngreso = false;
  bool _aceptaReglamento = false;
  bool _enviando = false;
  String? _error;
  Map<String, dynamic>? _detalle;
  String? _grupoAceptado;

  RegExpMatch? get _partes => _formato.firstMatch(widget.codigo);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _consultar());
  }

  Future<void> _consultar() async {
    final partes = _partes;
    if (partes == null) {
      setState(() {
        _cargando = false;
        _error = 'El enlace de invitación no es válido.';
      });
      return;
    }
    setState(() {
      _cargando = true;
      _error = null;
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
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _detalle = respuesta.data;
      });
    } on Object {
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _error =
            'No pudimos verificar la invitación. Revisá el enlace o probá de nuevo.';
      });
    }
  }

  Future<void> _aceptar() async {
    if (_enviando ||
        !_aceptaReglamento ||
        _detalle == null ||
        _partes == null) {
      return;
    }
    setState(() => _enviando = true);
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
              'hashReglamento': _detalle!['hashReglamento'],
              'aceptaReglamento': true,
            },
            options: Options(headers: {'Idempotency-Key': clave}),
          );
      ref.read(idempotenciaProvider.notifier).cerrar(formulario);
      if (!mounted) return;
      setState(() {
        _grupoAceptado = respuesta.data?['grupoId'] as String?;
        _detalle = null;
        _error = null;
      });
    } on Object {
      if (!mounted) return;
      setState(
        () => _error = 'No pudimos completar tu ingreso. Probá de nuevo.',
      );
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  /// «Bs 100,00»; si el servidor mandara un importe fuera del contrato, se muestra tal
  /// cual en vez de romper la pantalla de una invitación.
  String _importe(Map<String, dynamic> d) {
    final monto = '${d['montoAporte']}';
    final moneda = '${d['moneda']}';
    try {
      return formatearMonto(monto: monto, moneda: moneda);
    } on FormatException {
      return '$monto $moneda';
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final retorno = Uri.encodeComponent('/pasanaku/unirse/${widget.codigo}');
    final detalle = _detalle;
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: Espacio.s4),
              child: CabeceraDeSeccion(titulo: TextosPasanaku.tituloUnirse),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(Espacio.s4),
                children: [
                  if (_cargando)
                    Padding(
                      padding: const EdgeInsets.all(Espacio.s6),
                      child: Column(
                        children: [
                          const CircularProgressIndicator(),
                          const SizedBox(height: Espacio.s3),
                          Text(
                            TextosPasanaku.verificandoInvitacion,
                            style: Tipo.cuerpo.copyWith(color: t.text2),
                          ),
                        ],
                      ),
                    ),
                  if (_necesitaIngreso)
                    Tarjeta(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Text(
                            TextosPasanaku.ingresaParaVerGrupo,
                            style: Tipo.cuerpo.copyWith(color: t.text),
                          ),
                          const SizedBox(height: Espacio.s4),
                          Boton(
                            texto: TextosPasanaku.ingresar,
                            variante: BotonVariante.primario,
                            expandido: true,
                            onPressed: () =>
                                context.go('/ingreso?volver=$retorno'),
                          ),
                          const SizedBox(height: Espacio.s2),
                          Boton(
                            texto: TextosPasanaku.crearCuenta,
                            variante: BotonVariante.fantasma,
                            expandido: true,
                            onPressed: () =>
                                context.go('/registro?volver=$retorno'),
                          ),
                        ],
                      ),
                    ),
                  if (detalle != null) ...[
                    Tarjeta(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            detalle['nombre'] as String? ?? '',
                            style: Tipo.titulo2.copyWith(color: t.text),
                          ),
                          const SizedBox(height: Espacio.s2),
                          Text(
                            _importe(detalle),
                            style: Tipo.titulo1.copyWith(color: t.brandTexto),
                          ),
                          Text(
                            _cadaCuanto[detalle['periodicidad']] ??
                                '${detalle['periodicidad']}'.toLowerCase(),
                            style: Tipo.cuerpoChico.copyWith(color: t.text3),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: Espacio.s4),
                    Tarjeta(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            TextosPasanaku.reglamentoDelGrupo,
                            style: Tipo.etiqueta.copyWith(color: t.text2),
                          ),
                          const SizedBox(height: Espacio.s2),
                          Text(
                            detalle['reglamento'] as String? ?? '',
                            style: Tipo.cuerpo.copyWith(color: t.text),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: Espacio.s3),
                    Casilla(
                      etiqueta: TextosPasanaku.aceptoReglamento,
                      valor: _aceptaReglamento,
                      onChanged: (v) => setState(() => _aceptaReglamento = v),
                    ),
                  ],
                  if (_grupoAceptado != null)
                    Tarjeta(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.all(Espacio.s2),
                                decoration: BoxDecoration(
                                  color: t.okBg,
                                  shape: BoxShape.circle,
                                ),
                                child: Icon(
                                  Icons.check_rounded,
                                  color: t.okTexto,
                                ),
                              ),
                              const SizedBox(width: Espacio.s3),
                              Expanded(
                                child: Text(
                                  TextosPasanaku.yaFormasParte,
                                  style: Tipo.titulo3.copyWith(color: t.text),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: Espacio.s4),
                          Boton(
                            texto: TextosPasanaku.verMisGrupos,
                            variante: BotonVariante.primario,
                            expandido: true,
                            onPressed: () => context.go('/pasanaku/mi-estado'),
                          ),
                        ],
                      ),
                    ),
                  if (_error != null) ...[
                    const SizedBox(height: Espacio.s4),
                    Alerta(tono: Tono.error, titulo: _error!),
                    if (_detalle == null && !_necesitaIngreso) ...[
                      const SizedBox(height: Espacio.s3),
                      Boton(
                        texto: TextosPasanaku.reintentar,
                        variante: BotonVariante.fantasma,
                        expandido: true,
                        onPressed: _consultar,
                      ),
                    ],
                  ],
                ],
              ),
            ),
            if (detalle != null)
              BarraDeAccion(
                hijo: Boton(
                  texto: TextosPasanaku.unirmeAlGrupo,
                  variante: BotonVariante.primario,
                  expandido: true,
                  cargando: _enviando,
                  onPressed: _aceptaReglamento && !_enviando ? _aceptar : null,
                ),
              ),
          ],
        ),
      ),
    );
  }
}
