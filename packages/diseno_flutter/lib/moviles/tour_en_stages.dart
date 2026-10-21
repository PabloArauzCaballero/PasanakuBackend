import 'package:flutter/material.dart';

import '../atomos/boton.dart';
import '../atomos/boton_variante.dart';
import '../tokens/tokens.dart';

part 'tour_en_stages_etapas.dart';
part 'tour_en_stages_pagina.dart';

/// Walkthrough interactivo en etapas (Stages) con diseño ultra-estético
/// y psicología de marketing enfocada en progreso, estatus y confianza comunitaria.
class TourEnStages extends StatefulWidget {
  const TourEnStages({
    super.key,
    this.etapas = etapasPorDefecto,
    this.onCompletar,
  });

  final List<EtapaDelTour> etapas;
  final VoidCallback? onCompletar;

  static const List<EtapaDelTour> etapasPorDefecto = _etapasPorDefecto;

  static Future<void> mostrar(
    BuildContext context, {
    List<EtapaDelTour> etapas = etapasPorDefecto,
    VoidCallback? onCompletar,
  }) => showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    backgroundColor: Colors.transparent,
    builder: (c) => TourEnStages(
      etapas: etapas,
      onCompletar: () {
        Navigator.of(c).pop();
        onCompletar?.call();
      },
    ),
  );

  @override
  State<TourEnStages> createState() => _TourEnStagesState();
}

class _TourEnStagesState extends State<TourEnStages> {
  late final PageController _pageController;
  int _paginaActual = 0;

  @override
  void initState() {
    super.initState();
    _pageController = PageController();
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _irASiguiente() {
    if (_paginaActual < widget.etapas.length - 1) {
      _pageController.nextPage(
        duration: const Duration(milliseconds: 320),
        curve: Curves.easeOutCubic,
      );
    } else {
      widget.onCompletar?.call();
    }
  }

  void _irAAtras() {
    if (_paginaActual > 0) {
      _pageController.previousPage(
        duration: const Duration(milliseconds: 320),
        curve: Curves.easeOutCubic,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final esUltima = _paginaActual == widget.etapas.length - 1;

    return Container(
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(Radios.xl),
        ),
        border: Border.all(color: t.border),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Barra superior con botón cerrar y título
          Padding(
            padding: const EdgeInsets.fromLTRB(
              Espacio.s4,
              Espacio.s3,
              Espacio.s4,
              Espacio.s1,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: Espacio.s2,
                        vertical: 3,
                      ),
                      decoration: BoxDecoration(
                        color: t.surface2,
                        borderRadius: BorderRadius.circular(Radios.sm),
                        border: Border.all(color: t.border),
                      ),
                      child: Text(
                        'Paso ${_paginaActual + 1} de ${widget.etapas.length}',
                        style: Tipo.etiqueta.copyWith(
                          color: t.text2,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  ],
                ),
                TextButton(
                  onPressed: widget.onCompletar,
                  child: Text(
                    esUltima ? 'Cerrar' : 'Saltar tour',
                    style: Tipo.etiqueta.copyWith(color: t.text3),
                  ),
                ),
              ],
            ),
          ),

          // Contenedor deslizable de etapas
          SizedBox(
            height: 480,
            child: PageView.builder(
              controller: _pageController,
              itemCount: widget.etapas.length,
              onPageChanged: (i) => setState(() => _paginaActual = i),
              itemBuilder: (context, index) {
                final etapa = widget.etapas[index];
                return _paginaDeEtapa(etapa, t);
              },
            ),
          ),

          // Puntos indicadores y botones de navegación
          _navegacionDelTour(
            etapas: widget.etapas,
            paginaActual: _paginaActual,
            t: t,
            esUltima: esUltima,
            irAAtras: _irAAtras,
            irASiguiente: _irASiguiente,
          ),
        ],
      ),
    );
  }
}
