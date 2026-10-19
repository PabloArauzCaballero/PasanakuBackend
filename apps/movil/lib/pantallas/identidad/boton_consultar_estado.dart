import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../navegacion/acceso_a_rutas.dart';
import 'dominio/consultar_estado_de_cuenta.dart';
import 'textos_de_verificacion.dart';

/// «Consultar estado» (2026-10-19): mientras la identidad está en revisión, la persona
/// pregunta desde Inicio, Grupos o una operación si ya la aprobaron. Si la aprobaron,
/// la cuenta se activa ahí mismo y se llama a [alActivar]; si no, se le dice que
/// sigue en revisión. Nunca la deja esperando en una pantalla.
class BotonConsultarEstado extends ConsumerStatefulWidget {
  const BotonConsultarEstado({
    super.key,
    this.alActivar,
    this.variante = BotonVariante.primario,
    this.expandido = false,
  });

  final VoidCallback? alActivar;
  final BotonVariante variante;
  final bool expandido;

  @override
  ConsumerState<BotonConsultarEstado> createState() =>
      _BotonConsultarEstadoState();
}

class _BotonConsultarEstadoState extends ConsumerState<BotonConsultarEstado> {
  bool _consultando = false;

  Future<void> _consultar() async {
    if (_consultando) return;
    setState(() => _consultando = true);
    final avisos = ScaffoldMessenger.maybeOf(context);
    void avisar(String texto) =>
        avisos?.showSnackBar(SnackBar(content: Text(texto)));
    try {
      final resultado = await ref.read(consultarEstadoDeCuentaProvider)();
      if (!mounted) return;
      switch (resultado) {
        case ResultadoDeConsulta.activada:
          avisar(TextosDeVerificacion.cuentaActivada);
          widget.alActivar?.call();
        case ResultadoDeConsulta.aprobadaSinActivar:
          avisar(TextosDeVerificacion.noSePudoActivar);
        case ResultadoDeConsulta.enRevision:
          avisar(TextosDeVerificacion.sigueEnRevision);
        case ResultadoDeConsulta.sinVerificar:
          context.push(rutaDeVerificarIdentidad);
        case ResultadoDeConsulta.otro:
          avisar(TextosDeVerificacion.requeridaNoDisponible);
      }
    } on Object {
      if (mounted) avisar(TextosDeVerificacion.noSePudoConsultar);
    } finally {
      if (mounted) setState(() => _consultando = false);
    }
  }

  @override
  Widget build(BuildContext context) => Boton(
    key: const ValueKey('consultar-estado'),
    texto: _consultando
        ? TextosDeVerificacion.consultando
        : TextosDeVerificacion.consultarEstado,
    variante: widget.variante,
    expandido: widget.expandido,
    cargando: _consultando,
    onPressed: _consultar,
  );
}
