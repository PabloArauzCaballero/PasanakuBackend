import 'dart:async';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/puertos/camara_en_vivo.dart';
import '../../infraestructura/plataforma.dart';
import '../../proveedores/ajustes_del_sistema.dart';
import '../../proveedores/haptica.dart';
import 'controlador_prueba_de_vida.dart';
import 'dominio/juez_de_pose.dart';
import 'dominio/ciclo_de_pose.dart';
import 'pasos_alta/aviso_de_permiso_camara.dart';
import 'pasos_alta/barra_superior_de_vida.dart';
import 'pasos_alta/cuenta_atras_grande.dart';
import 'pasos_alta/miniaturas_de_vida.dart';
import 'pasos_alta/pie_de_prueba_de_vida.dart';
import 'pasos_alta/silueta_de_cara.dart';
import 'textos_de_captura.dart';

class PantallaDePruebaDeVida extends ConsumerStatefulWidget {
  const PantallaDePruebaDeVida({
    super.key,
    required this.poses,
    this.crearCamara = camaraEnVivoDeLaPlataforma,
    this.ahora,
    this.validarFoto,
  });

  final List<PoseDeVida> poses;
  final CamaraEnVivo Function() crearCamara;
  @visibleForTesting
  final DateTime Function()? ahora;
  @visibleForTesting
  final Future<Object> Function(String ruta)? validarFoto;

  @override
  ConsumerState<PantallaDePruebaDeVida> createState() =>
      _PantallaDePruebaDeVidaState();
}

class _PantallaDePruebaDeVidaState extends ConsumerState<PantallaDePruebaDeVida>
    with TickerProviderStateMixin {
  late final AnimationController _controladorDestello;
  late final Animation<double> _destello;
  late final ControladorPruebaDeVida _prueba;

  @override
  void initState() {
    super.initState();
    _controladorDestello = AnimationController(
      vsync: this,
      duration: Movimiento.destelloDeCaptura,
    );
    _destello = TweenSequence<double>([
      TweenSequenceItem(tween: Tween(begin: 0, end: 0.85), weight: 16),
      TweenSequenceItem(tween: Tween(begin: 0.85, end: 0), weight: 84),
    ]).animate(_controladorDestello);
    _prueba = ControladorPruebaDeVida(
      poses: widget.poses,
      crearCamara: widget.crearCamara,
      ahora: widget.ahora,
      validarFoto: widget.validarFoto,
      alCapturarConExito: () => ref.read(hapticaProvider).exito(),
      alDisparar: () {
        final tiempo = duracion(context, Movimiento.destelloDeCaptura);
        _controladorDestello.duration = tiempo;
        if (tiempo == Duration.zero) {
          _controladorDestello.value = 0;
          return;
        }
        unawaited(_controladorDestello.forward(from: 0));
      },
    );
    unawaited(_prueba.inicializar());
  }

  void _terminar() => Navigator.of(context).pop(_prueba.resultado);

  Future<void> _continuar() async {
    if (mounted) _terminar();
  }

  @override
  void dispose() {
    _prueba.dispose();
    _controladorDestello.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Theme(
    data: temaDesde(Tokens.oscuro, Brightness.dark),
    child: AnimatedBuilder(
      animation: _prueba,
      builder: (context, _) {
        final pose = _prueba.pose;
        final instruccion = TextosDeCaptura.instruccionDePose(pose);
        return Scaffold(
          backgroundColor: Paleta.ink,
          body: SafeArea(
            child: Column(
              children: [
                BarraSuperiorDeVida(
                  indice: _prueba.indice + 1,
                  total: widget.poses.length,
                  titulo: TextosDeCaptura.tituloDePose(pose),
                  instruccion: instruccion,
                  onSalir: _terminar,
                ),
                Expanded(child: _visor(pose)),
                MiniaturasDeVida(
                  poses: widget.poses,
                  capturas: _prueba.capturas,
                  rechazadas: _prueba.rechazadas,
                  onRepetir: _prueba.repetir,
                ),
                PieDePruebaDeVida(
                  pista: _pistaActual(),
                  progreso: _prueba.estado.progreso,
                  rechazo: _prueba.rechazo,
                  manual: _prueba.camaraLista,
                  tomando: _prueba.tomando,
                  tomada: _prueba.fotoTomada,
                  finalizada: _prueba.finalizada,
                  onTomarFoto: _prueba.tomarAhora,
                  onContinuar: _continuar,
                  onCancelar: _terminar,
                ),
              ],
            ),
          ),
        );
      },
    ),
  );

  String? _pistaActual() {
    if (_prueba.finalizada || _prueba.fotoTomada) return null;
    if (_prueba.estado.fase == FaseDePose.cuentaAtras) {
      return TextosDeCaptura.instruccionDuranteCuenta(_prueba.pose);
    }
    return _prueba.estado.pista ?? TextosDeCaptura.preparando;
  }

  Widget _visor(PoseDeVida pose) {
    if (_prueba.permiso == EstadoDePermiso.denegado ||
        _prueba.permiso == EstadoDePermiso.denegadoParaSiempre) {
      final bloqueado = _prueba.permiso == EstadoDePermiso.denegadoParaSiempre;
      return AvisoDePermisoCamara(
        soloPruebaDeVida: true,
        bloqueadoParaSiempre: bloqueado,
        onAccion: bloqueado
            ? () => ref.read(ajustesDelSistemaProvider).abrir()
            : _prueba.reintentarPermiso,
      );
    }
    if (!_prueba.camaraLista) {
      return const Center(
        child: CircularProgressIndicator(color: Paleta.white),
      );
    }
    return Stack(
      fit: StackFit.expand,
      children: [
        RepaintBoundary(child: _prueba.vista),
        SiluetaDeCara(
          pose: pose,
          progreso: _prueba.estado.progreso,
          tomada: _prueba.fotoTomada,
        ),
        CuentaAtrasGrande(
          segundos: _prueba.estado.fase == FaseDePose.cuentaAtras
              ? _prueba.estado.segundos
              : null,
        ),
        AnimatedBuilder(
          key: const ValueKey('destello-de-captura'),
          animation: _destello,
          builder: (context, _) => IgnorePointer(
            child: ColoredBox(
              color: Paleta.white.withValues(alpha: _destello.value),
            ),
          ),
        ),
      ],
    );
  }
}
