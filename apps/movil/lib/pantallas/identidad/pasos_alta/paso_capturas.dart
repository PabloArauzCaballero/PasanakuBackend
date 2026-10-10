import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/capturas_del_expediente.dart';
import '../dominio/carnet_de_prueba.dart';
import '../dominio/estado_alta.dart';
import '../textos_de_captura.dart';
import 'hoja_antes_de_escanear.dart';
import 'pie_de_capturas.dart';
import 'riel_de_capturas.dart';
import 'sacar_captura.dart';
import 'tarjeta_de_captura.dart';

/// El paso de capturas del alta (CU-02), comportamiento de Atlas: carrusel de
/// cinco fotos, salto automático a la siguiente pendiente, y "Continuar" con el
/// motivo de qué falta cuando está deshabilitado. Arriba, el riel de las cinco caras
/// (`RielDeCapturas`) dice dónde estás y qué falta, y lleva a cualquiera.
class PasoCapturas extends ConsumerStatefulWidget {
  const PasoCapturas({super.key, required this.onContinuar});
  final VoidCallback onContinuar;

  @override
  ConsumerState<PasoCapturas> createState() => _PasoCapturasState();
}

class _PasoCapturasState extends ConsumerState<PasoCapturas> {
  late final PageController _controlador;
  late int _pagina;
  final _hojaMostrada = <CaraDelCarril>{};

  @override
  void initState() {
    super.initState();
    final pendiente = ref.read(altaProvider).capturas.siguientePendiente;
    final inicial = CapturasDelExpediente.orden.indexOf(
      pendiente ?? CapturasDelExpediente.orden.first,
    );
    _pagina = inicial < 0 ? 0 : inicial;
    _controlador = PageController(initialPage: _pagina);
  }

  @override
  void dispose() {
    _controlador.dispose();
    super.dispose();
  }

  void _saltarA(CaraDelCarril cara) {
    final i = CapturasDelExpediente.orden.indexOf(cara);
    if (MediaQuery.disableAnimationsOf(context)) {
      _controlador.jumpToPage(i);
      return;
    }
    _controlador.animateToPage(
      i,
      duration: Movimiento.pagina,
      curve: Movimiento.pareja,
    );
  }

  Future<void> _tomarFoto(CaraDelCarril cara) async {
    // La hoja de consejos, una vez por cara del carnet; el escáner, siempre.
    if (cara.esDocumento && _hojaMostrada.add(cara)) {
      if (!await mostrarHojaAntesDeEscanear(context)) return;
    }
    if (!mounted) return;
    final listas = ref.read(altaProvider).capturas.porCara.keys.toSet();
    final resultado = await sacarCaptura(context, cara, listas: listas);
    if (!mounted || resultado == null) return;
    if (resultado == 'prueba') {
      await _usarCarnetDePrueba();
      return;
    }
    if (resultado is Map<CaraDelCarril, Captura>) {
      final notifier = ref.read(altaProvider.notifier);
      resultado.forEach(notifier.registrarCaptura);
      final siguiente = ref.read(altaProvider).capturas.siguientePendiente;
      if (siguiente != null) _saltarA(siguiente);
    }
  }

  Future<void> _usarCarnetDePrueba() async {
    final capturas = await generarCapturasDePrueba();
    final notifier = ref.read(altaProvider.notifier);
    for (final cara in CapturasDelExpediente.orden) {
      final captura = capturas.porCara[cara];
      if (captura != null) notifier.registrarCaptura(cara, captura);
    }
    final datos = ref.read(altaProvider).datos;
    final leidos = leerCarnetDePrueba(
      numeroEscrito: datos.numeroDocumento,
      lugarEscrito: datos.lugarExpedicion ?? '',
    );
    notifier.actualizarDatos(
      datos.copiarCon(
        numeroDocumento: leidos.numeroDocumento,
        lugarExpedicion: leidos.lugarExpedicion,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final capturas = ref.watch(altaProvider.select((e) => e.capturas));
    const orden = CapturasDelExpediente.orden;
    final visible = orden[_pagina];
    final visibleLista = capturas.porCara.containsKey(visible);
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            TextosDeCaptura.contador(capturas.porCara.length),
            style: Tipo.titulo2.copyWith(color: t.text),
          ),
          const SizedBox(height: Espacio.s3),
          RielDeCapturas(
            actual: orden[_pagina],
            listas: capturas.porCara.keys.toSet(),
            onElegir: _saltarA,
          ),
          const SizedBox(height: Espacio.s4),
          SizedBox(
            height: 360,
            child: PageView.builder(
              controller: _controlador,
              onPageChanged: (i) => setState(() => _pagina = i),
              itemCount: orden.length,
              itemBuilder: (context, i) {
                final cara = orden[i];
                return TarjetaDeCaptura(
                  cara: cara,
                  captura: capturas.porCara[cara],
                );
              },
            ),
          ),
          const SizedBox(height: Espacio.s3),
          // Fuera del `PageView` a propósito: ahí adentro el borde de la página
          // recortaba la sombra del botón. Actúa sobre la cara que se ve.
          Boton(
            texto: visibleLista
                ? TextosDeCaptura.repetirFoto
                : TextosDeCaptura.tomarFoto,
            icono: visibleLista
                ? Icons.refresh_rounded
                : Icons.photo_camera_rounded,
            variante: visibleLista
                ? BotonVariante.fantasma
                : BotonVariante.primario,
            expandido: true,
            onPressed: () => _tomarFoto(visible),
          ),
          const SizedBox(height: Espacio.s5),
          PieDeCapturas(
            capturas: capturas,
            onEnviar: widget.onContinuar,
            onUsarCarnetDePrueba: _usarCarnetDePrueba,
          ),
        ],
      ),
    );
  }
}
