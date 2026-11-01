part of 'tour_en_stages.dart';

/// Las cuatro etapas del recorrido por defecto.
const List<EtapaDelTour> _etapasPorDefecto = [
  EtapaDelTour(
    etiqueta: 'ETAPA 1 · LA MESA DE JUEGO',
    titulo: '1 Punto por cada Bs en Juego',
    subtitulo: 'Tu dinero aportado es reputación ganada',
    descripcion:
        'En cada ronda, cada boliviano que colocas en la mesa se traduce en un punto directo de reputación. Cumplir tus compromisos suma puntos auditados y eleva tu jerarquía en la comunidad.',
    icono: Icons.savings_outlined,
    colorAcento: Paleta.ok,
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
    colorAcento: Paleta.info,
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
    colorAcento: Paleta.warn,
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
    colorAcento: Paleta.pii,
    destacados: [
      'Teléfono y correo protegidos y enmascarados',
      'Cifras contables contrastadas con el mayor contable',
      'Confianza fundamentada sin invadir la intimidad',
    ],
  ),
];
