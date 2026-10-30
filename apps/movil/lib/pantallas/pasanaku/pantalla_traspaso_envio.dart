part of 'pantalla_traspaso.dart';

/// Lo que la pantalla le pide al servidor: la votación del grupo (CU-63) y, con ella
/// aprobada, el traspaso (CU-64).
mixin _EnvioDelTraspaso on ConsumerState<PantallaTraspaso> {
  final _motivo = TextEditingController();
  var _confirmando = false;
  String? _elegido;

  CupoDelGrupo get _cupo => (
    grupoId: widget.grupoId,
    cupoId: widget.cupoId,
    usuarioEntranteId: _elegido,
  );

  @override
  void dispose() {
    _motivo.dispose();
    super.dispose();
  }

  String? _detalleDe(VotacionDelCupo? votacion) => switch (votacion) {
    VotacionAbierta(:final cierraEn) => TextosDelTraspaso.acuerdoAbierto(
      cierraEn,
    ),
    null => TextosDelTraspaso.acuerdoRevisando,
    _ => null,
  };

  /// Pide al grupo que vote el traspaso de ESTE cupo a quien se eligió.
  void _pedirVotacion(IntegranteDelGrupo mio, IntegranteDelGrupo? entrante) =>
      ref
          .read(pedirVotacionProvider.notifier)
          .pedir(
            _cupo,
            TextosDelTraspaso.descripcionDeVotacion(
              mio.numero,
              entrante?.nombre ?? TextosDelTraspaso.sinNombre,
              _motivo.text.trim(),
            ),
          );

  /// Un toque abre UNA hoja: mientras está abierta (o se envía), la acción no responde.
  Future<void> _confirmarYEnviar(
    IntegranteDelGrupo mio,
    IntegranteDelGrupo entrante,
  ) async {
    if (_confirmando) return;
    setState(() => _confirmando = true);
    final motivo = _motivo.text.trim();
    final si = await confirmarTraspaso(
      context,
      ResumenDelTraspaso(mio: mio, entrante: entrante, motivo: motivo),
    );
    if (!mounted) return;
    setState(() => _confirmando = false);
    if (!si) return;
    await ref.read(traspasarCupoProvider.notifier).enviar((
      grupoId: widget.grupoId,
      cupoId: widget.cupoId,
      usuarioEntranteId: entrante.usuarioId!,
      motivo: motivo,
      acuerdoId: switch (ref.read(votacionDelCupoProvider(_cupo)).value) {
        VotacionAprobada(:final acuerdoId) => acuerdoId,
        _ => widget.acuerdoId,
      },
    ));
  }
}
