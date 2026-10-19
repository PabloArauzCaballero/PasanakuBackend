part of 'capa_de_bloqueo.dart';

class _VistaDeBloqueo extends StatelessWidget {
  const _VistaDeBloqueo({
    required this.hijo,
    required this.conservarCapa,
    required this.motivo,
    required this.opacidad,
    required this.escala,
    required this.biometriaDisponible,
    required this.onIngresarDeNuevo,
    this.onDesbloquear,
  });

  final Widget hijo;
  final bool conservarCapa;
  final MotivoDeBloqueo motivo;
  final Animation<double> opacidad;
  final Animation<double> escala;
  final bool biometriaDisponible;
  final VoidCallback onIngresarDeNuevo;
  final VoidCallback? onDesbloquear;

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        ExcludeSemantics(child: AbsorbPointer(child: hijo)),
        if (conservarCapa)
          Positioned.fill(
            child: FadeTransition(
              key: const ValueKey('capa-bloqueo-fundido'),
              opacity: opacidad,
              child: _ContenidoDeBloqueo(
                motivo: motivo,
                escala: escala,
                biometriaDisponible: biometriaDisponible,
                onDesbloquear: onDesbloquear,
                onIngresarDeNuevo: onIngresarDeNuevo,
              ),
            ),
          ),
      ],
    );
  }
}

class _ContenidoDeBloqueo extends StatelessWidget {
  const _ContenidoDeBloqueo({
    required this.motivo,
    required this.escala,
    required this.biometriaDisponible,
    required this.onIngresarDeNuevo,
    this.onDesbloquear,
  });

  final MotivoDeBloqueo motivo;
  final Animation<double> escala;
  final bool biometriaDisponible;
  final VoidCallback onIngresarDeNuevo;
  final VoidCallback? onDesbloquear;

  @override
  Widget build(BuildContext context) {
    final tokens = Tokens.of(context);
    return FondoDeMarca(
      hijo: Scaffold(
        backgroundColor: Colors.transparent,
        body: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(
                horizontal: Espacio.s5,
                vertical: Espacio.s6,
              ),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 420),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Center(
                      child: ScaleTransition(
                        key: const ValueKey('capa-bloqueo-escala'),
                        scale: escala,
                        child: const Marca(tamano: 88),
                      ),
                    ),
                    const SizedBox(height: Espacio.s6),
                    Semantics(
                      header: true,
                      child: Text(
                        'Hola de nuevo',
                        textAlign: TextAlign.center,
                        style: Tipo.titulo1.copyWith(color: tokens.text),
                      ),
                    ),
                    const SizedBox(height: Espacio.s3),
                    Text(
                      _detalleDelBloqueo(motivo),
                      textAlign: TextAlign.center,
                      style: Tipo.cuerpo.copyWith(color: tokens.text2),
                    ),
                    const SizedBox(height: Espacio.s6),
                    if (biometriaDisponible)
                      Boton(
                        texto: 'Entrar con huella o PIN',
                        variante: BotonVariante.primario,
                        expandido: true,
                        onPressed: onDesbloquear,
                      ),
                    const SizedBox(height: Espacio.s2),
                    Boton(
                      texto: 'Entrar con mi contraseña',
                      variante: BotonVariante.fantasma,
                      expandido: true,
                      onPressed: onIngresarDeNuevo,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

String _detalleDelBloqueo(MotivoDeBloqueo motivo) => switch (motivo) {
  MotivoDeBloqueo.arranque =>
    'Por tu seguridad, confirmá que sos vos para ver tu cuenta.',
  MotivoDeBloqueo.segundoPlano =>
    'Saliste de la app un rato. Confirmá que sos vos para seguir.',
  MotivoDeBloqueo.inactividad =>
    'Pasaron 5 minutos sin uso. Confirmá que sos vos para seguir.',
};
