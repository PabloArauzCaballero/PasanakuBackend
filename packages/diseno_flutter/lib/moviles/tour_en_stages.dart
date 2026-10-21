import 'package:flutter/material.dart';

import '../atomos/boton.dart';
import '../atomos/boton_variante.dart';
import '../tokens/tokens.dart';

/// Un paso del recorrido o walkthrough por etapas.
class EtapaDelTour {
  const EtapaDelTour({
    required this.etiqueta,
    required this.titulo,
    required this.subtitulo,
    required this.descripcion,
    required this.icono,
    required this.colorAcento,
    required this.destacados,
  });

  final String etiqueta;
  final String titulo;
  final String subtitulo;
  final String descripcion;
  final IconData icono;
  final Color colorAcento;
  final List<String> destacados;
}

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

  static const List<EtapaDelTour> etapasPorDefecto = [
    EtapaDelTour(
      etiqueta: 'ETAPA 1 · LA MESA DE JUEGO',
      titulo: '1 Punto por cada Bs en Juego',
      subtitulo: 'Tu dinero aportado es reputación ganada',
      descripcion:
          'En cada ronda, cada boliviano que colocas en la mesa se traduce en un punto directo de reputación. Cumplir tus compromisos suma puntos auditados y eleva tu jerarquía en la comunidad.',
      icono: Icons.savings_outlined,
      colorAcento: Color(0xFF10B981),
      destacados: [
        '1 punto por cada Bs aportado en la mesa',
        'Bonos de +50 pts por anticipar tus aportes',
        '+500 pts al cerrar ciclos completos sin demoras',
      ],
    ),
    EtapaDelTour(
      etiqueta: 'ETAPA 2 · CALIFICACIÓN DE RIESGO',
      titulo: 'Riesgo Bajo A1: Tu Récord Limpio',
      subtitulo: 'La máxima confianza sin garantías asfixiantes',
      descripcion:
          'Mantener cero impagos y puntualidad impecable te otorga la calificación A1. Es tu pasaporte financiero: te abre la puerta a grupos de mayor capital y prioridad para elegir turnos.',
      icono: Icons.shield_outlined,
      colorAcento: Color(0xFF0284C7),
      destacados: [
        'Calificación objetiva basada en hechos fácticos',
        'Acceso preferencial a grupos con montos elevados',
        'Cero días de retraso como estandarte de honor',
      ],
    ),
    EtapaDelTour(
      etiqueta: 'ETAPA 3 · TROFEOS Y LOGROS',
      titulo: 'Insignias de Maestría y Prestigio',
      subtitulo: 'Reconocimiento tangible a tu trayectoria',
      descripcion:
          'Alcanza hitos como Pagador Puntual, Sin Mora 12 Meses y Mesa de Honor. Cada logro desbloquea trofeos exclusivos tipo GymSheet que acreditan tu veteranía comunitaria.',
      icono: Icons.emoji_events_outlined,
      colorAcento: Color(0xFFF59E0B),
      destacados: [
        'Trofeos permanentes con celebraciones animadas',
        'Rangos de honor: Bronce, Plata, Oro y Diamante',
        'Certificaciones verificables ante terceros',
      ],
    ),
    EtapaDelTour(
      etiqueta: 'ETAPA 4 · PRIVACIDAD Y TRANSPARENCIA',
      titulo: 'Transparencia Total sin Fugas de Datos',
      subtitulo: 'Auditoría estricta con confidencialidad absoluta',
      descripcion:
          'Conoce con quién compartes la mesa: consulta ciclos terminados, aportes totales y días de demora de cada integrante, mientras teléfonos y correos quedan blindados bajo estricto enmascaramiento.',
      icono: Icons.lock_person_outlined,
      colorAcento: Color(0xFF6B3FA0),
      destacados: [
        'Teléfono y correo protegidos y enmascarados',
        'Cifras contables contrastadas con el mayor contable',
        'Confianza fundamentada sin invadir la intimidad',
      ],
    ),
  ];

  static Future<void> mostrar(
    BuildContext context, {
    List<EtapaDelTour> etapas = etapasPorDefecto,
    VoidCallback? onCompletar,
  }) =>
      showModalBottomSheet<void>(
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
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.90,
      ),
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(Radios.xl)),
        border: Border.all(color: t.border),
      ),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Barra superior con botón cerrar y título
            Padding(
              padding: const EdgeInsets.fromLTRB(Espacio.s4, Espacio.s3, Espacio.s4, Espacio.s1),
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
            Flexible(
              child: PageView.builder(
                controller: _pageController,
                itemCount: widget.etapas.length,
                onPageChanged: (i) => setState(() => _paginaActual = i),
                itemBuilder: (context, index) {
                  final etapa = widget.etapas[index];
                  return SingleChildScrollView(
                    padding: const EdgeInsets.symmetric(horizontal: Espacio.s5),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const SizedBox(height: Espacio.s2),
                      // Icono con capas concéntricas
                      Stack(
                        alignment: Alignment.center,
                        children: [
                          Container(
                            width: 100,
                            height: 100,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: etapa.colorAcento.withValues(alpha: 0.12),
                            ),
                          ),
                          Container(
                            width: 76,
                            height: 76,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: etapa.colorAcento.withValues(alpha: 0.22),
                              border: Border.all(
                                color: etapa.colorAcento.withValues(alpha: 0.4),
                                width: 2,
                              ),
                            ),
                            child: Center(
                              child: Icon(
                                etapa.icono,
                                size: 36,
                                color: etapa.colorAcento,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: Espacio.s4),

                      // Etiqueta superior
                      Text(
                        etapa.etiqueta,
                        style: Tipo.etiqueta.copyWith(
                          color: etapa.colorAcento,
                          fontWeight: FontWeight.w800,
                          letterSpacing: 1.5,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: Espacio.s2),

                      // Título
                      Text(
                        etapa.titulo,
                        style: Tipo.titulo1.copyWith(
                          color: t.text,
                          fontWeight: FontWeight.w800,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: Espacio.s1),

                      // Subtítulo
                      Text(
                        etapa.subtitulo,
                        style: Tipo.cuerpo.copyWith(
                          color: t.brand,
                          fontWeight: FontWeight.w600,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: Espacio.s3),

                      // Descripción
                      Text(
                        etapa.descripcion,
                        style: Tipo.cuerpoChico.copyWith(
                          color: t.text2,
                          height: 1.4,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: Espacio.s4),

                      // Puntos destacados con checks
                      Container(
                        padding: const EdgeInsets.all(Espacio.s3),
                        decoration: BoxDecoration(
                          color: t.surface2,
                          borderRadius: BorderRadius.circular(Radios.md),
                          border: Border.all(color: t.border),
                        ),
                        child: Column(
                          children: [
                            for (final d in etapa.destacados) ...[
                              Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Icon(
                                    Icons.check_circle_rounded,
                                    size: 16,
                                    color: etapa.colorAcento,
                                  ),
                                  const SizedBox(width: Espacio.s2),
                                  Expanded(
                                    child: Text(
                                      d,
                                      style: Tipo.etiqueta.copyWith(
                                        color: t.text,
                                        fontWeight: FontWeight.w500,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              if (d != etapa.destacados.last)
                                const SizedBox(height: Espacio.s2),
                            ],
                          ],
                        ),
                      ),
                      const SizedBox(height: Espacio.s4),
                    ],
                  ),
                );
              },
            ),
          ),

          // Puntos indicadores y botones de navegación
          Padding(
            padding: const EdgeInsets.fromLTRB(
              Espacio.s5,
              Espacio.s2,
              Espacio.s5,
              Espacio.s5,
            ),
            child: Column(
              children: [
                // Dots con expansión animada
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: List.generate(widget.etapas.length, (i) {
                    final activa = i == _paginaActual;
                    return AnimatedContainer(
                      duration: const Duration(milliseconds: 250),
                      margin: const EdgeInsets.symmetric(horizontal: 4),
                      height: 6,
                      width: activa ? 24 : 6,
                      decoration: BoxDecoration(
                        color: activa
                            ? widget.etapas[_paginaActual].colorAcento
                            : t.border,
                        borderRadius: BorderRadius.circular(Radios.pill),
                      ),
                    );
                  }),
                ),
                const SizedBox(height: Espacio.s4),

                // Botones de acción
                Row(
                  children: [
                    if (_paginaActual > 0) ...[
                      Expanded(
                        child: Boton(
                          texto: 'Atrás',
                          variante: BotonVariante.secundario,
                          onPressed: _irAAtras,
                        ),
                      ),
                      const SizedBox(width: Espacio.s3),
                    ],
                    Expanded(
                      flex: 2,
                      child: Boton(
                        texto: esUltima ? '¡Entendido, a jugar!' : 'Siguiente etapa',
                        variante: BotonVariante.primario,
                        onPressed: _irASiguiente,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    ),
  );
  }
}
