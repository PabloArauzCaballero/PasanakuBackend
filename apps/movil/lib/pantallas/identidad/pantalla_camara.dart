import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/puertos/camara_en_vivo.dart';
import '../../infraestructura/plataforma.dart';
import '../../proveedores/ajustes_del_sistema.dart';
import 'dominio/captura_desde_archivo.dart';
import 'dominio/capturas_del_expediente.dart';
import 'dominio/camara_identidad.dart';
import 'pasos_alta/aviso_de_permiso_camara.dart';
import 'pasos_alta/encabezado_y_consejos_de_camara.dart';
import 'pasos_alta/elegir_captura_de_galeria.dart';
import 'pasos_alta/guia_de_encuadre.dart';
import 'pasos_alta/opciones_de_captura_rechazada.dart';
import 'textos_de_captura.dart';

/// La cámara en vivo del escáner, comportamiento de Atlas: cabecera con la pista
/// del paso, visor con guía de encuadre y consejos plegables, pie con "Tomar
/// foto" y "Cancelar". Devuelve la [Captura] aceptada, o `null` si se canceló.
class PantallaDeCamara extends ConsumerStatefulWidget {
  const PantallaDeCamara({super.key, required this.cara, this.camara});
  final CaraDelCarril cara;
  @visibleForTesting
  final CamaraEnVivo? camara;

  @override
  ConsumerState<PantallaDeCamara> createState() => _PantallaDeCamaraState();
}

class _PantallaDeCamaraState extends ConsumerState<PantallaDeCamara> {
  late final CamaraEnVivo _camara =
      widget.camara ?? camaraEnVivoDeLaPlataforma();
  EstadoDePermiso? _permiso;
  bool _lista = false;
  bool _tomando = false;
  String? _rechazo;
  Captura? _capturaRechazada;

  @override
  void initState() {
    super.initState();
    _inicializar();
  }

  Future<void> _inicializar() async {
    final permiso = await _camara.pedirPermiso();
    if (!mounted) return;
    if (permiso == EstadoDePermiso.concedido) {
      await _camara.iniciar(frontal: !widget.cara.esDocumento);
    }
    if (!mounted) return;
    setState(() {
      _permiso = permiso;
      _lista = permiso == EstadoDePermiso.concedido;
    });
  }

  @override
  void dispose() {
    _camara.liberar();
    super.dispose();
  }

  Future<void> _tomarFoto() async {
    setState(() {
      _tomando = true;
      _rechazo = null;
    });
    final ruta = await _camara.tomarFoto();
    final evaluacion = await evaluarCapturaDesdeArchivo(
      ruta,
      recortadaAlCarnet: false,
    );
    if (!mounted) return;
    if (evaluacion.rechazo != null && widget.cara.esDocumento) {
      setState(() {
        _tomando = false;
        _rechazo = evaluacion.rechazo;
        _capturaRechazada = evaluacion.captura;
      });
      return;
    }
    if (evaluacion.rechazo != null) {
      setState(() {
        _tomando = false;
        _rechazo = evaluacion.rechazo;
      });
      return;
    }
    Navigator.of(context).pop(evaluacion.captura);
  }

  void _usarCapturaRechazada() {
    final captura = _capturaRechazada;
    if (captura != null) Navigator.of(context).pop(captura);
  }

  Future<void> _elegirDeLasFotos() async {
    final captura = await elegirCapturaDeGaleria(
      context,
      ref.read(camaraProvider),
    );
    if (mounted && captura != null) {
      Navigator.of(context).pop({widget.cara: captura});
    }
  }

  @override
  Widget build(BuildContext context) {
    return Theme(
      data: temaDesde(Tokens.oscuro, Brightness.dark),
      child: Scaffold(
        backgroundColor: Paleta.ink,
        body: SafeArea(
          child: Stack(
            fit: StackFit.expand,
            children: [
              _visor(),
              if (_lista) ...[
                GuiaDeEncuadre(
                  esDocumento: widget.cara.esDocumento,
                  velo: Tokens.of(context).camaraVelo,
                ),
              ],
              Align(
                alignment: Alignment.topCenter,
                child: EncabezadoYConsejosDeCamara(
                  cara: widget.cara,
                  lista: _lista,
                  onVolver: () => Navigator.of(context).pop(),
                ),
              ),
              Align(alignment: Alignment.bottomCenter, child: _pie()),
            ],
          ),
        ),
      ),
    );
  }

  Widget _pie() => Padding(
    padding: const EdgeInsets.all(Espacio.s4),
    child: Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (_rechazo != null && _capturaRechazada != null) ...[
          OpcionesDeCapturaRechazada(
            cara: widget.cara,
            motivo: _rechazo!,
            onRepetir: _tomarFoto,
            onUsarEstaFoto: _usarCapturaRechazada,
          ),
          const SizedBox(height: Espacio.s3),
        ] else if (_rechazo != null) ...[
          Alerta(titulo: _rechazo!, tono: Tono.aviso),
          const SizedBox(height: Espacio.s3),
        ],
        Boton(
          texto: TextosDeCaptura.tomarFoto,
          variante: BotonVariante.primario,
          expandido: true,
          cargando: _tomando,
          onPressed: _lista && !_tomando ? _tomarFoto : null,
        ),
        const SizedBox(height: Espacio.s2),
        Boton(
          texto: TextosDeCaptura.cancelar,
          variante: BotonVariante.enlace,
          expandido: true,
          onPressed: () => Navigator.of(context).pop(),
        ),
      ],
    ),
  );

  Widget _visor() {
    if (_permiso == EstadoDePermiso.denegado ||
        _permiso == EstadoDePermiso.denegadoParaSiempre) {
      return AvisoDePermisoCamara(
        bloqueadoParaSiempre: _permiso == EstadoDePermiso.denegadoParaSiempre,
        onAccion: _permiso == EstadoDePermiso.denegadoParaSiempre
            ? () => ref.read(ajustesDelSistemaProvider).abrir()
            : _inicializar,
        onElegirDeLasFotos: _elegirDeLasFotos,
      );
    }
    if (!_lista) {
      return const Center(
        child: CircularProgressIndicator(color: Paleta.white),
      );
    }
    return Stack(fit: StackFit.expand, children: [_camara.vista()]);
  }
}
