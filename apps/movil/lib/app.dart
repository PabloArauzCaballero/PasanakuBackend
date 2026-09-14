import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/verificacion_contrato.dart';
import 'navegacion/rutas.dart';
import 'proveedores/sesion.dart';
import 'package:aportaya_diseno/moviles/apertura_de_marca.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

class AppAportaYa extends ConsumerWidget {
  /// [inicial] nulo significa «la que diga el enrutador». Antes esta clase repetía su
  /// propia ruta de arranque, y cuando la del enrutador cambió a la portada, esta
  /// copia siguió mandando a la billetera: la app abría adentro igual. Un valor por
  /// defecto en dos lugares es un valor por defecto que va a divergir.
  AppAportaYa({super.key, String? inicial})
    : _enrutador = inicial == null
          ? crearEnrutador()
          : crearEnrutador(inicial: inicial);

  final GoRouter _enrutador;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // Se dispara al construir el árbol, sin bloquear el primer frame: si el
    // gateway está caído al iniciar, el arranque no se cuelga esperándolo.
    ref.watch(verificacionContratoProvider);
    return MaterialApp.router(
      title: 'AportaYa',
      theme: temaDesde(Tokens.claro, Brightness.light),
      darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
      themeMode: ThemeMode.system,
      routerConfig: _enrutador,
      debugShowCheckedModeBanner: false,
      builder: (context, child) => _ConApertura(
        enrutador: _enrutador,
        child: _AvisoDeContrato(child: child),
      ),
    );
  }
}

/// Una franja angosta arriba, no un diálogo que tapa la pantalla: el aviso de
/// «actualizá la app» no puede impedir seguir usando lo que ya funciona.
class _AvisoDeContrato extends ConsumerWidget {
  const _AvisoDeContrato({required this.child});
  final Widget? child;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(verificacionContratoProvider).asData?.value;
    if (estado != EstadoContrato.desactualizado || child == null) {
      return child ?? const SizedBox.shrink();
    }
    return Column(
      children: [
        Material(
          color: Theme.of(context).colorScheme.errorContainer,
          child: SafeArea(
            bottom: false,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Text(
                'Hay una versión nueva de AportaYa. Actualizala para seguir '
                'operando sin problemas.',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ),
          ),
        ),
        Expanded(child: child!),
      ],
    );
  }
}

/// La apertura de marca por encima de la app, una sola vez por arranque.
///
/// Va acá arriba y no como una ruta: así la app **ya está armada y pidiendo el saldo**
/// detrás de la animación, en vez de empezar a cargar recién cuando la apertura
/// termina. La animación tapa el arranque; no lo demora.
/// También es el momento en que se decide **a dónde entrar**: mientras la marca se
/// acerca, se lee el almacén seguro. Si hay una sesión guardada, la app abre en la
/// billetera; si no, se queda en la portada. Es el único lugar donde esa pregunta se
/// hace, y se hace una sola vez por arranque: preguntarla en cada navegación sería
/// tocar el llavero del teléfono a cada paso.
class _ConApertura extends ConsumerStatefulWidget {
  const _ConApertura({required this.child, required this.enrutador});
  final Widget child;
  final GoRouter enrutador;

  @override
  ConsumerState<_ConApertura> createState() => _ConAperturaState();
}

class _ConAperturaState extends ConsumerState<_ConApertura> {
  bool _mostrando = true;
  Timer? _respaldo;

  @override
  void initState() {
    super.initState();
    _decidirDondeEntrar();
    // El cinturón de seguridad: si la animación no llamara a su final —un ticker que
    // no arranca, un frame que no llega—, a los dos segundos la apertura se va igual.
    // Nadie se queda mirando un logo sin poder entrar a su plata.
    _respaldo = Timer(const Duration(seconds: 2), _ocultar);
  }

  Future<void> _decidirDondeEntrar() async {
    // Si leer el llavero falla —teléfono con el almacén bloqueado, un permiso raro—,
    // lo seguro es la portada: se pide ingresar de nuevo. Nunca al revés.
    String? token;
    try {
      token = await ref.read(sesionProvider).tokenDeAcceso();
    } on Object {
      token = null;
    }
    if (!mounted || token == null || token.isEmpty) return;
    if (widget.enrutador.state.uri.toString() == '/portada') {
      widget.enrutador.go('/billetera/inicio');
    }
  }

  void _ocultar() {
    _respaldo?.cancel();
    if (!mounted || !_mostrando) return;
    // Con «reducir movimiento», la apertura pide cerrarse mientras se está
    // construyendo el árbol, y ahí `setState` sobre un ancestro es un error de
    // Flutter. Se difiere al frame siguiente, que además es cuando corresponde.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || !_mostrando) return;
      setState(() => _mostrando = false);
    });
  }

  @override
  void dispose() {
    _respaldo?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Stack(
    children: [
      widget.child,
      if (_mostrando)
        Positioned.fill(child: AperturaDeMarca(alTerminar: _ocultar)),
    ],
  );
}
