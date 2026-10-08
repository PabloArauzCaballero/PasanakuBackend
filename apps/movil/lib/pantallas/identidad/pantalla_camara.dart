import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/foundation.dart' show kReleaseMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/puertos/camara_en_vivo.dart';
import '../../infraestructura/plataforma.dart';
import '../../proveedores/ajustes_del_sistema.dart';
import 'dominio/captura_desde_archivo.dart';
import 'dominio/capturas_del_expediente.dart';
import 'pasos_alta/aviso_de_permiso_camara.dart';
import 'pasos_alta/cabecera_de_camara.dart';
import 'pasos_alta/consejos_para_la_foto.dart';
import 'pasos_alta/guia_de_encuadre.dart';
import 'textos_de_captura.dart';

/// La cámara en vivo del escáner, comportamiento de Atlas: cabecera con la pista
/// del paso, visor con guía de encuadre y consejos plegables, pie con "Tomar
/// foto" y "Cancelar". Devuelve la [Captura] aceptada, o `null` si se canceló.
class PantallaDeCamara extends ConsumerStatefulWidget {
  const PantallaDeCamara({super.key, required this.cara});
  final CaraDelCarril cara;

  @override
  ConsumerState<PantallaDeCamara> createState() => _PantallaDeCamaraState();
}

class _PantallaDeCamaraState extends ConsumerState<PantallaDeCamara> {
  final CamaraEnVivo _camara = camaraEnVivoDeLaPlataforma();
  EstadoDePermiso? _permiso;
  bool _lista = false;
  bool _tomando = false;
  String? _rechazo;

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
    final resultado = await capturaDesdeArchivo(ruta, recortadaAlCarnet: false);
    if (!mounted) return;
    if (resultado is String) {
      setState(() {
        _tomando = false;
        _rechazo = resultado;
      });
      return;
    }
    Navigator.of(context).pop(resultado);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Paleta.ink,
      body: SafeArea(
        child: Column(
          children: [
            CabeceraDeCamara(
              titulo: tituloDeCaptura(widget.cara),
              pista: pistaDeCaptura(widget.cara),
              onVolver: () => Navigator.of(context).pop(),
            ),
            Expanded(child: _visor()),
            _pie(),
          ],
        ),
      ),
    );
  }

  Widget _pie() => Padding(
    padding: const EdgeInsets.all(Espacio.s4),
    child: Column(
      children: [
        if (_rechazo != null) ...[
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
        if (!kReleaseMode) ...[
          const SizedBox(height: Espacio.s2),
          Boton(
            texto: TextosDeCaptura.usarCarnetDePrueba,
            variante: BotonVariante.fantasma,
            expandido: true,
            onPressed: () => Navigator.of(context).pop('prueba'),
          ),
        ],
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
      );
    }
    if (!_lista) {
      return const Center(
        child: CircularProgressIndicator(color: Paleta.white),
      );
    }
    return Stack(
      fit: StackFit.expand,
      children: [
        _camara.vista(),
        GuiaDeEncuadre(esDocumento: widget.cara.esDocumento),
        Positioned(
          left: Espacio.s3,
          right: Espacio.s3,
          bottom: Espacio.s3,
          child: ConsejosParaLaFoto(cara: widget.cara),
        ),
      ],
    );
  }
}
