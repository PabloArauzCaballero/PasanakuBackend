import 'dart:async';
import 'package:flutter/material.dart';

import '../../dominio/puertos/camara_en_vivo.dart';
import 'dominio/captura_desde_archivo.dart';
import 'dominio/capturas_del_expediente.dart';
import 'dominio/ciclo_de_pose.dart';
import 'dominio/juez_de_pose.dart';
import 'textos_de_captura.dart';

class ControladorPruebaDeVida extends ChangeNotifier {
  ControladorPruebaDeVida({
    required this.poses,
    required CamaraEnVivo Function() crearCamara,
    required this.alDisparar,
    this.alCapturarConExito,
    DateTime Function()? ahora,
    Future<Object> Function(String ruta)? validarFoto,
  }) : _camara = crearCamara(),
       _ahora = ahora ?? DateTime.now,
       _validarFoto =
           validarFoto ??
           ((ruta) => capturaDesdeArchivo(ruta, recortadaAlCarnet: false));

  final List<PoseDeVida> poses;
  final VoidCallback alDisparar;
  final Future<void> Function()? alCapturarConExito;
  final DateTime Function() _ahora;
  final Future<Object> Function(String ruta) _validarFoto;
  final CamaraEnVivo _camara;
  final capturas = <CaraDelCarril, Captura>{};
  final rechazadas = <CaraDelCarril>{};
  EstadoDePermiso? permiso;
  EstadoDePose estado = const EstadoDePose(fase: FaseDePose.respiro);
  String? rechazo;
  CicloDePose? _ciclo;
  JuezDePose? _juez;
  Timer? _ticker;
  var indice = 0;
  var camaraLista = false;
  var tomando = false;
  var fotoTomada = false;
  var finalizada = false;
  var _cerrado = false;

  PoseDeVida get pose => poses[indice];
  Widget get vista => _camara.vista();

  Future<void> inicializar() async {
    permiso = await _camara.pedirPermiso();
    if (_cerrado) return;
    if (permiso == EstadoDePermiso.concedido) {
      await _camara.iniciar(frontal: true, conDeteccion: true);
      camaraLista = true;
    }
    notifyListeners();
    if (camaraLista) await _empezarPose();
  }

  Future<void> reintentarPermiso() => inicializar();

  Future<void> _empezarPose() async {
    _ciclo = CicloDePose(inicio: _ahora(), hayDetector: true);
    _juez = JuezDePose(pose);
    _iniciarTicker();
    estado = const EstadoDePose(fase: FaseDePose.respiro);
    fotoTomada = false;
    rechazo = null;
    notifyListeners();
    final hayDetector = await _camara.escucharRostros(_alLeer);
    if (_cerrado) return;
    _ciclo?.hayDetector = hayDetector;
    avanzar();
  }

  void _iniciarTicker() {
    _ticker?.cancel();
    _ticker = Timer.periodic(
      const Duration(milliseconds: 100),
      (_) => avanzar(),
    );
  }

  void _alLeer(LecturaDeRostro? lectura) {
    if (_cerrado || tomando || fotoTomada || finalizada) return;
    avanzar(veredicto: _juez?.evaluar(lectura));
  }

  void avanzar({Veredicto? veredicto}) {
    if (_cerrado || tomando || fotoTomada || finalizada) return;
    final nuevo = _ciclo?.avanzar(_ahora(), veredicto: veredicto);
    if (nuevo == null) return;
    _publicar(nuevo);
  }

  void tomarAhora() {
    final nuevo = _ciclo?.tomarAhora(_ahora());
    if (nuevo != null) _publicar(nuevo);
  }

  void _publicar(EstadoDePose nuevo) {
    final cambio =
        nuevo.fase != estado.fase ||
        nuevo.segundos != estado.segundos ||
        nuevo.pista != estado.pista ||
        nuevo.progreso != estado.progreso ||
        nuevo.disparar;
    estado = nuevo;
    if (cambio) notifyListeners();
    if (nuevo.disparar) unawaited(_capturar());
  }

  Future<void> _capturar() async {
    if (_cerrado || tomando || finalizada) return;
    tomando = true;
    _ticker?.cancel();
    notifyListeners();
    String? ruta;
    Object resultado;
    try {
      await _camara.dejarDeEscuchar();
      alDisparar();
      ruta = await _camara.tomarFoto();
      resultado = await _validarFoto(ruta);
    } on Object {
      resultado = TextosDeCaptura.fotoFallida;
    }
    if (_cerrado) {
      if (resultado is Captura) await eliminarRutaLocal(resultado.ruta);
      if (resultado is! Captura && ruta != null) {
        await eliminarRutaLocal(ruta);
      }
      return;
    }
    if (resultado is! Captura) {
      if (ruta != null) await eliminarRutaLocal(ruta);
      _ciclo?.reiniciar();
      rechazadas.add(pose.cara);
      tomando = false;
      rechazo = resultado as String;
      estado = const EstadoDePose(fase: FaseDePose.buscando);
      notifyListeners();
      await _reanudarPose();
      return;
    }
    final haptica = alCapturarConExito;
    if (haptica != null) {
      unawaited(haptica().catchError((Object _) {}));
    }
    capturas[pose.cara] = resultado;
    rechazadas.remove(pose.cara);
    tomando = false;
    fotoTomada = true;
    rechazo = null;
    estado = const EstadoDePose(fase: FaseDePose.tomada, progreso: 1);
    notifyListeners();
    await Future<void>.delayed(avisoDeFotoTomada);
    if (_cerrado) return;
    if (indice == poses.length - 1) {
      finalizada = true;
      notifyListeners();
      return;
    }
    indice++;
    await _empezarPose();
  }

  Future<void> _reanudarPose() async {
    _ciclo = CicloDePose(inicio: _ahora(), hayDetector: true);
    _juez = JuezDePose(pose);
    _iniciarTicker();
    final hayDetector = await _camara.escucharRostros(_alLeer);
    if (_cerrado) return;
    _ciclo?.hayDetector = hayDetector;
  }

  void repetir(PoseDeVida pose) {
    if (pose != this.pose || !rechazadas.contains(pose.cara)) return;
    _ciclo?.reiniciar();
    _juez = JuezDePose(pose);
    rechazadas.remove(pose.cara);
    rechazo = null;
    notifyListeners();
    avanzar();
  }

  Map<CaraDelCarril, Captura>? get resultado =>
      capturas.isEmpty ? null : Map<CaraDelCarril, Captura>.of(capturas);

  @override
  void dispose() {
    _cerrado = true;
    _ticker?.cancel();
    unawaited(_camara.liberar());
    super.dispose();
  }
}
