import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

export 'app_real.dart'
    show localeDeAportaYa, localesDeAportaYa, delegadosDeLocalizacionDeAportaYa;

import 'dominio/cierre_de_sesion.dart';
import 'dominio/vigilante_de_sesion.dart';
import 'dominio/tope_de_sesion.dart';
import 'navegacion/rutas.dart';
import 'pantallas/soporte/arranque_segun_capacidades.dart';
import 'proveedores/bloqueo.dart';
import 'proveedores/sesion.dart';
import 'app_real.dart';
import 'package:aportaya_diseno/moviles/apertura_de_marca.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

class AppAportaYa extends ConsumerStatefulWidget {
  /// [inicial] nulo significa «la que diga el enrutador». Antes esta clase repetía su
  /// propia ruta de arranque, y cuando la del enrutador cambió a la portada, esta
  /// copia siguió mandando a la billetera: la app abría adentro igual. Un valor por
  /// defecto en dos lugares es un valor por defecto que va a divergir.
  const AppAportaYa({super.key, this.inicial});

  final String? inicial;

  @override
  ConsumerState<AppAportaYa> createState() => _AppAportaYaState();
}

class _AppAportaYaState extends ConsumerState<AppAportaYa> {
  GoRouter? _enrutador;
  late final Sesion _sesion;
  late final Vigilante _vigilante;
  late final TopeDeSesion _tope;
  late final AppLifecycleListener _cicloDeVida;
  Timer? _timerDeInactividad;
  Timer? _timerDelTope;
  bool _estuvoEnSegundoPlano = false;

  @override
  void initState() {
    super.initState();
    _sesion = ref.read(sesionProvider);
    final ahora = ref.read(relojDeSesionProvider);
    final bloqueo = ref.read(bloqueoProvider);
    _vigilante = Vigilante(
      ahora: ahora,
      bloquear: bloqueo.bloquear,
      bloqueada: () => bloqueo.bloqueada,
    );
    _tope = TopeDeSesion(
      sesion: _sesion,
      ahora: ahora,
      cerrar: ({required motivo}) =>
          ref.read(cierreDeSesionProvider).ejecutar(motivo: motivo),
    );
    _sesion.addListener(_alCambiarSesion);
    _cicloDeVida = AppLifecycleListener(onStateChange: _alCambiarEstado);
    _timerDeInactividad = Timer.periodic(
      revisionDeInactividad,
      (_) => _vigilante.revisar(),
    );
    _timerDelTope = Timer.periodic(
      const Duration(minutes: 1),
      (_) => unawaited(_tope.revisar()),
    );
    _hidratarSesion();
  }

  Future<void> _hidratarSesion() async {
    await _sesion.hidratar();
    if (!mounted) return;
    if (_sesion.activa) {
      // La sesión se cubre antes de crear el router: no hay un primer frame
      // privado sin candado tras un arranque en frío.
      ref.read(bloqueoProvider).bloquearAlArrancar();
      await _tope.revisar();
      if (!mounted) return;
    }
    setState(() {
      _enrutador = crearEnrutador(
        inicial: widget.inicial ?? '/portada',
        sesion: _sesion,
      );
    });
  }

  void _alCambiarEstado(AppLifecycleState estado) {
    _vigilante.cambioDeEstado(estado);
    if (estado == AppLifecycleState.hidden ||
        estado == AppLifecycleState.paused) {
      _estuvoEnSegundoPlano = true;
      return;
    }
    if (estado == AppLifecycleState.resumed) {
      final reintentarBiometria = _estuvoEnSegundoPlano;
      _estuvoEnSegundoPlano = false;
      unawaited(_alReanudar(reintentarBiometria));
    }
  }

  Future<void> _alReanudar(bool reintentarBiometria) async {
    await _tope.revisar();
    if (!mounted || !reintentarBiometria || !_sesion.activa) return;
    final bloqueo = ref.read(bloqueoProvider);
    if (bloqueo.bloqueada) bloqueo.nuevoEpisodioBiometrico();
  }

  void _alCambiarSesion() {
    if (_sesion.estado == EstadoDeSesion.anonima) {
      ref.read(bloqueoProvider).olvidar();
    }
  }

  @override
  void dispose() {
    _timerDeInactividad?.cancel();
    _timerDelTope?.cancel();
    _cicloDeVida.dispose();
    _sesion.removeListener(_alCambiarSesion);
    _enrutador?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final enrutador = _enrutador;
    if (enrutador == null) {
      // No se construye ninguna ruta privada hasta terminar la hidratación.
      return Listener(
        onPointerDown: (_) => _vigilante.interaccion(),
        child: MaterialApp(
          title: 'AportaYa',
          locale: localeDeAportaYa,
          supportedLocales: localesDeAportaYa,
          localizationsDelegates: delegadosDeLocalizacionDeAportaYa,
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(body: AperturaDeMarca(alTerminar: () {})),
        ),
      );
    }
    // H2.S2.M3 (madre H6.S2.M3): en un release de iOS sin almacén seguro ni
    // protección de pantalla soportados, `builder` de acá abajo NUNCA se llama --
    // ni `verificacionContratoProvider` ni el cliente HTTP que crea se tocan.
    return Listener(
      onPointerDown: (_) => _vigilante.interaccion(),
      child: ArranqueSegunCapacidades(
        builder: (context) => AppAportaYaReal(enrutador: enrutador, ref: ref),
      ),
    );
  }
}
