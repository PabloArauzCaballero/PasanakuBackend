import 'dart:async';

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/puertos/camara_en_vivo.dart';
import '../../infraestructura/plataforma.dart';
import '../../proveedores/ajustes_del_sistema.dart';
import 'dominio/captura_desde_archivo.dart';
import 'dominio/capturas_del_expediente.dart';
import 'dominio/juez_de_pose.dart';
import 'pasos_alta/aviso_de_permiso_camara.dart';
import 'pasos_alta/cabecera_de_camara.dart';
import 'pasos_alta/guia_de_encuadre.dart';
import 'pasos_alta/pie_de_prueba_de_vida.dart';
import 'textos_de_captura.dart';

/// La prueba de vida automática: frente, perfil izquierdo y perfil derecho seguidos,
/// con la cámara delantera. **La foto se saca sola** cuando el detector ve la pose
/// sostenida ([JuezDePose]). Sin detector, o tras [plazoAutomatico] sin lograrla,
/// aparece «Tomar foto» de respaldo (sin dejar de detectar). Devuelve las capturas
/// logradas por cara —aunque se cancele a mitad— o `null` si no salió ninguna.
class PantallaDePruebaDeVida extends ConsumerStatefulWidget {
  const PantallaDePruebaDeVida({
    super.key,
    required this.poses,
    this.crearCamara = camaraEnVivoDeLaPlataforma,
  });

  final List<PoseDeVida> poses;

  /// Las pruebas pasan un doble; la app, la cámara real.
  final CamaraEnVivo Function() crearCamara;

  static const plazoAutomatico = Duration(seconds: 20);

  @override
  ConsumerState<PantallaDePruebaDeVida> createState() =>
      _PantallaDePruebaDeVidaState();
}

class _PantallaDePruebaDeVidaState
    extends ConsumerState<PantallaDePruebaDeVida> {
  late final CamaraEnVivo _camara = widget.crearCamara();
  final _capturas = <CaraDelCarril, Captura>{};
  EstadoDePermiso? _permiso;
  var _lista = false;
  var _indice = 0;
  late JuezDePose _juez = JuezDePose(_pose);
  Veredicto? _veredicto;
  var _tomando = false;
  var _manual = false;
  String? _rechazo;
  Timer? _plazo;

  PoseDeVida get _pose => widget.poses[_indice];

  @override
  void initState() {
    super.initState();
    _inicializar();
  }

  Future<void> _inicializar() async {
    final permiso = await _camara.pedirPermiso();
    if (!mounted) return;
    if (permiso == EstadoDePermiso.concedido) {
      await _camara.iniciar(frontal: true, conDeteccion: true);
    }
    if (!mounted) return;
    setState(() {
      _permiso = permiso;
      _lista = permiso == EstadoDePermiso.concedido;
    });
    if (_lista) await _empezarPose();
  }

  Future<void> _empezarPose() async {
    _juez = JuezDePose(_pose);
    _plazo?.cancel();
    _plazo = Timer(PantallaDePruebaDeVida.plazoAutomatico, () {
      if (mounted) setState(() => _manual = true);
    });
    setState(() {
      _veredicto = null;
      _manual = false;
    });
    final conDetector = await _camara.escucharRostros(_alLeer);
    if (mounted && !conDetector) setState(() => _manual = true);
  }

  void _alLeer(LecturaDeRostro? lectura) {
    if (!mounted || _tomando) return;
    final veredicto = _juez.evaluar(lectura);
    setState(() => _veredicto = veredicto);
    if (veredicto.listo) _capturar();
  }

  Future<void> _capturar() async {
    if (_tomando) return;
    setState(() {
      _tomando = true;
      _rechazo = null;
    });
    _plazo?.cancel();
    Object resultado;
    try {
      await _camara.dejarDeEscuchar();
      final ruta = await _camara.tomarFoto();
      resultado = await capturaDesdeArchivo(ruta, recortadaAlCarnet: false);
    } on Object {
      resultado = TextosDeCaptura.fotoFallida;
    }
    if (!mounted) return;
    if (resultado is! Captura) {
      setState(() {
        _tomando = false;
        _rechazo = resultado as String;
      });
      await _empezarPose();
      return;
    }
    _capturas[_pose.cara] = resultado;
    unawaited(HapticFeedback.mediumImpact());
    if (_indice == widget.poses.length - 1) return _terminar();
    setState(() {
      _indice++;
      _tomando = false;
    });
    await _empezarPose();
  }

  void _terminar() => Navigator.of(
    context,
  ).pop(_capturas.isEmpty ? null : Map<CaraDelCarril, Captura>.of(_capturas));

  @override
  void dispose() {
    _plazo?.cancel();
    _camara.liberar();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Paleta.ink,
      body: SafeArea(
        child: Column(
          children: [
            CabeceraDeCamara(
              titulo:
                  '${tituloDeCaptura(_pose.cara)} · '
                  '${TextosDeCaptura.deTres(_indice + 1, widget.poses.length)}',
              pista: pistaDeCaptura(_pose.cara),
              onVolver: _terminar,
            ),
            Expanded(child: _visor()),
            PieDePruebaDeVida(
              pista: _lista
                  ? (_veredicto?.pista ?? TextosDeCaptura.preparando)
                  : null,
              progreso: _veredicto?.progreso ?? 0,
              rechazo: _rechazo,
              manual: _manual && _lista,
              tomando: _tomando,
              onTomarFoto: _capturar,
              onCancelar: _terminar,
            ),
          ],
        ),
      ),
    );
  }

  Widget _visor() {
    if (_permiso == EstadoDePermiso.denegado ||
        _permiso == EstadoDePermiso.denegadoParaSiempre) {
      return AvisoDePermisoCamara(
        bloqueadoParaSiempre: _permiso == EstadoDePermiso.denegadoParaSiempre,
        onAccion: _permiso == EstadoDePermiso.denegadoParaSiempre
            ? () => ref.read(ajustesDelSistemaProvider).abrir()
            : _inicializar,
      );
    }
    if (!_lista) {
      return const Center(
        child: CircularProgressIndicator(color: Paleta.white),
      );
    }
    return Stack(
      fit: StackFit.expand,
      children: [_camara.vista(), const GuiaDeEncuadre(esDocumento: false)],
    );
  }
}
