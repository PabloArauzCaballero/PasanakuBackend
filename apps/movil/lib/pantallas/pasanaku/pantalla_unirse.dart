import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cliente.dart';
import '../../proveedores/idempotencia.dart';
import '../../proveedores/sesion.dart';
import 'dominio/cu69_unirse_por_enlace.dart';
import 'tarjetas_de_unirse.dart';
import 'textos_invitacion.dart';
import 'vistas_de_unirse.dart';

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
  bool _cargando = true;
  bool _necesitaIngreso = false;
  bool _aceptaReglamento = false;
  bool _enviando = false;
  String? _error;
  Map<String, dynamic>? _detalle;
  String? _grupoAceptado;

  EnlaceDeInvitacion? get _enlace => EnlaceDeInvitacion.leer(widget.codigo);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _consultar());
  }

  Future<void> _consultar() async {
    final enlace = _enlace;
    if (enlace == null) {
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
      final detalle = await enlace.consultar(ref.read(dioProvider));
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _detalle = detalle;
      });
    } on Object catch (e) {
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _error = textoDelRechazoDeInvitacion(
          e,
          'No pudimos verificar la invitación. Revisá el enlace o probá de nuevo.',
        );
      });
    }
  }

  Future<void> _aceptar() async {
    if (_enviando ||
        !_aceptaReglamento ||
        _detalle == null ||
        _enlace == null) {
      return;
    }
    setState(() => _enviando = true);
    final enlace = _enlace!;
    final formulario = 'cu69-aceptar-${enlace.tokenId}';
    final clave = ref.read(idempotenciaProvider.notifier).claveDe(formulario);
    try {
      final grupoId = await enlace.aceptar(
        ref.read(dioProvider),
        hashReglamento: _detalle!['hashReglamento'],
        claveDeIdempotencia: clave,
      );
      ref.read(idempotenciaProvider.notifier).cerrar(formulario);
      if (!mounted) return;
      setState(() {
        _grupoAceptado = grupoId;
        _detalle = null;
        _error = null;
      });
    } on Object catch (e) {
      if (!mounted) return;
      setState(
        () => _error = textoDelRechazoDeInvitacion(
          e,
          'No pudimos completar tu ingreso. Probá de nuevo.',
        ),
      );
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
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
              child: CabeceraDeSeccion(titulo: TextosInvitacion.tituloUnirse),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(Espacio.s4),
                children: [
                  if (_cargando) const VerificandoInvitacion(),
                  if (_necesitaIngreso) IngresoRequerido(retorno: retorno),
                  if (detalle != null) ...[
                    TarjetaDelGrupo(detalle: detalle),
                    const SizedBox(height: Espacio.s4),
                    ReglamentoDelGrupo(
                      reglamento: detalle['reglamento'] as String? ?? '',
                      acepta: _aceptaReglamento,
                      alCambiar: (v) => setState(() => _aceptaReglamento = v),
                    ),
                  ],
                  if (_grupoAceptado != null) const IngresoAceptado(),
                  if (_error != null)
                    ErrorDeInvitacion(
                      mensaje: _error!,
                      alReintentar: _detalle == null && !_necesitaIngreso
                          ? _consultar
                          : null,
                    ),
                ],
              ),
            ),
            if (detalle != null)
              BarraDeAccion(
                hijo: Boton(
                  texto: TextosInvitacion.unirmeAlGrupo,
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
