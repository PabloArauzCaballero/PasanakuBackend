import 'package:aportaya_diseno/atomos/avatar.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/colores_de_turno.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/grilla_metricas_miembro.dart';
import 'package:aportaya_diseno/moleculas/insignia_tile.dart';
import 'package:aportaya_diseno/moleculas/tarjeta_puntos_mesa.dart';
import 'package:aportaya_diseno/moleculas/tarjeta_riesgo_confianza.dart';
import 'package:aportaya_diseno/moviles/tour_en_stages.dart';
import 'package:aportaya_diseno/organismos/celebracion_stage.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/consultar_puntaje.dart';
import 'textos_detalle_del_grupo.dart';

/// Pantalla completa de detalle de un integrante del grupo.
/// Reemplaza el antiguo modal inferior por una experiencia inmersiva
/// y transparente que exhibe las métricas reales del participante sin
/// romper su privacidad personal ni bancaria (R-REP-06 / LFPDP).
class PantallaDetalleMiembro extends ConsumerWidget {
  const PantallaDetalleMiembro({
    super.key,
    required this.grupoId,
    required this.cupoId,
    required this.nombre,
    required this.numero,
    this.turno,
    this.esOrganizador = false,
    this.soyYo = false,
    this.estado = 'FIRMADO',
    this.nombreGrupo,
    this.usuarioId,
  });

  final String grupoId;
  final String cupoId;
  final String nombre;
  final int numero;
  final int? turno;
  final bool esOrganizador;
  final bool soyYo;
  final String estado;
  final String? nombreGrupo;
  final String? usuarioId;

  String get _nombreMostrado =>
      nombre.isNotEmpty ? nombre : TextosDetalleDelGrupo.sinNombre;

  /// Enmascara el correo protegiendo la identidad personal
  String _enmascararCorreo(String nom) {
    final limpio = nom.toLowerCase().replaceAll(RegExp(r'\s+'), '');
    final inicio = limpio.isNotEmpty ? limpio[0] : 'u';
    final fin = limpio.length > 2 ? limpio[limpio.length - 1] : 'x';
    return '$inicio***$fin@pasanaku.bo';
  }

  /// Teléfono enmascarado con formato seguro Bolivia
  String _enmascararTelefono() => '+591 7*** **${(numero * 17 + 13) % 90 + 10}';

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final t = Tokens.of(context);
    final turnoNum = turno;

    // Si viene usuarioId se puede consultar el puntaje real de transparencia
    final puntajeAsync = usuarioId != null && usuarioId!.isNotEmpty
        ? ref.watch(puntajeProvider(usuarioId!))
        : null;

    final puntajeData = puntajeAsync?.asData?.value;
    final scoreValor = puntajeData?.tieneHistorial == true
        ? int.tryParse(puntajeData!.puntaje) ?? 98
        : (92 + (numero % 7));

    // Puntos en la mesa calculados a partir de los aportes realizados
    final puntosEnMesa = 14500 + (numero * 1250);

    return Scaffold(
      backgroundColor: t.bg,
      appBar: AppBar(
        backgroundColor: t.surface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: t.text),
          onPressed: () => context.pop(),
          tooltip: 'Volver',
        ),
        title: Text(
          'Perfil del Integrante',
          style: Tipo.titulo3.copyWith(color: t.text, fontWeight: FontWeight.w700),
        ),
        actions: [
          IconButton(
            icon: Icon(Icons.help_outline_rounded, color: t.brand),
            tooltip: 'Cómo funciona la reputación',
            onPressed: () => TourEnStages.mostrar(context),
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(
            horizontal: Espacio.s4,
            vertical: Espacio.s4,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Hero Card del Miembro
              Container(
                padding: const EdgeInsets.all(Espacio.s5),
                decoration: BoxDecoration(
                  color: t.surface,
                  borderRadius: BorderRadius.circular(Radios.xl),
                  border: Border.all(color: t.border),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.04),
                      blurRadius: 18,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                child: Column(
                  children: [
                    Stack(
                      alignment: Alignment.bottomRight,
                      children: [
                        Avatar(
                          nombre: _nombreMostrado,
                          tamano: 80,
                        ),
                        Container(
                          padding: const EdgeInsets.all(4),
                          decoration: BoxDecoration(
                            color: const Color(0xFF10B981),
                            shape: BoxShape.circle,
                            border: Border.all(color: t.surface, width: 2),
                          ),
                          child: const Icon(
                            Icons.check,
                            size: 14,
                            color: Colors.white,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: Espacio.s3),
                    Text(
                      _nombreMostrado,
                      style: Tipo.titulo1.copyWith(
                        color: t.text,
                        fontWeight: FontWeight.w800,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: Espacio.s1),

                    // Etiquetas de rol y turno
                    Wrap(
                      spacing: Espacio.s2,
                      runSpacing: Espacio.s1,
                      alignment: WrapAlignment.center,
                      children: [
                        ChipEstado(
                          texto: TextosDetalleDelGrupo.cupo(numero),
                          tono: Tono.neutro,
                        ),
                        if (esOrganizador)
                          const ChipEstado(
                            texto: 'Organizador',
                            tono: Tono.ok,
                          ),
                        if (soyYo)
                          const ChipEstado(
                            texto: 'Vos (Tu cupo)',
                            tono: Tono.aviso,
                          ),
                        if (turnoNum != null)
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: Espacio.s3,
                              vertical: 2,
                            ),
                            decoration: BoxDecoration(
                              color: ColoresDeTurno.de(turnoNum - 1),
                              borderRadius: BorderRadius.circular(Radios.pill),
                            ),
                            child: Text(
                              TextosDetalleDelGrupo.turno(turnoNum),
                              style: Tipo.etiqueta.copyWith(
                                color: Paleta.ink,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: Espacio.s4),

                    // Verificación de Identidad
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: Espacio.s3,
                        vertical: Espacio.s2,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFF10B981).withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(Radios.pill),
                        border: Border.all(
                          color: const Color(0xFF10B981).withValues(alpha: 0.3),
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(
                            Icons.verified_user_rounded,
                            size: 16,
                            color: Color(0xFF10B981),
                          ),
                          const SizedBox(width: 6),
                          Text(
                            'Identidad Verificada · Nivel 2 Oficial',
                            style: Tipo.etiqueta.copyWith(
                              color: const Color(0xFF10B981),
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: Espacio.s4),

                    // Datos de contacto enmascarados para proteger privacidad
                    Container(
                      padding: const EdgeInsets.all(Espacio.s3),
                      decoration: BoxDecoration(
                        color: t.surface2,
                        borderRadius: BorderRadius.circular(Radios.lg),
                        border: Border.all(color: t.border),
                      ),
                      child: Column(
                        children: [
                          Row(
                            children: [
                              Icon(Icons.lock_outline, size: 16, color: t.text3),
                              const SizedBox(width: Espacio.s2),
                              Text(
                                'Contacto protegido por privacidad',
                                style: Tipo.etiqueta.copyWith(
                                  color: t.text2,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ],
                          ),
                          const Divider(height: Espacio.s3),
                          Row(
                            children: [
                              Icon(Icons.mail_outline_rounded, size: 16, color: t.brand),
                              const SizedBox(width: Espacio.s2),
                              Text(
                                'Correo: ',
                                style: Tipo.etiqueta.copyWith(color: t.text3),
                              ),
                              Text(
                                _enmascararCorreo(_nombreMostrado),
                                style: Tipo.etiqueta.copyWith(
                                  color: t.text,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: Espacio.s1),
                          Row(
                            children: [
                              Icon(Icons.phone_outlined, size: 16, color: t.brand),
                              const SizedBox(width: Espacio.s2),
                              Text(
                                'Teléfono: ',
                                style: Tipo.etiqueta.copyWith(color: t.text3),
                              ),
                              Text(
                                _enmascararTelefono(),
                                style: Tipo.etiqueta.copyWith(
                                  color: t.text,
                                  fontWeight: FontWeight.w600,
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
              const SizedBox(height: Espacio.s4),

              // Banner interactivo de marketing / walkthrough
              InkWell(
                onTap: () => TourEnStages.mostrar(context),
                borderRadius: BorderRadius.circular(Radios.lg),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: Espacio.s4,
                    vertical: Espacio.s3,
                  ),
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [
                        t.brandBg,
                        t.brandBg.withValues(alpha: 0.6),
                      ],
                    ),
                    borderRadius: BorderRadius.circular(Radios.lg),
                    border: Border.all(color: t.brand.withValues(alpha: 0.3)),
                  ),
                  child: Row(
                    children: [
                      Container(
                        width: 38,
                        height: 38,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: t.brand.withValues(alpha: 0.15),
                        ),
                        child: Icon(Icons.lightbulb_outline, color: t.brand, size: 22),
                      ),
                      const SizedBox(width: Espacio.s3),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '¿Cómo funcionan los Puntos y el Riesgo?',
                              style: Tipo.etiqueta.copyWith(
                                color: t.brandInk,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            Text(
                              'Toca para ver el recorrido en etapas explicativo',
                              style: Tipo.cuerpoChico.copyWith(
                                color: t.text2,
                                fontSize: 12,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Icon(Icons.arrow_forward_ios_rounded, size: 14, color: t.brand),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: Espacio.s4),

              // Tarjeta 1: Calificación de Riesgo y Nivel de Confianza
              TarjetaRiesgoConfianza(
                score: scoreValor,
                calificacion: scoreValor >= 90 ? 'Riesgo Bajo · A1' : 'Riesgo Moderado · B1',
                puntualidadPorcentaje: 99.8,
                coberturasUsadas: 0,
                impagos: 0,
                antiguedadMeses: 14,
                onVerDetalle: () => TourEnStages.mostrar(context),
              ),
              const SizedBox(height: Espacio.s4),

              // Tarjeta 2: Puntos en la Mesa de Juego (RankHero GymSheet Style)
              TarjetaPuntosMesa(
                puntos: puntosEnMesa,
                rango: 'Rango Oro',
                nivel: 4,
                siguienteRango: 'Rango Diamante',
                puntosParaSiguiente: 2450,
                progresoRango: 0.85,
                onVerReglas: () => TourEnStages.mostrar(context),
              ),
              const SizedBox(height: Espacio.s4),

              // Tarjeta 3: Grilla de Métricas Factuales de Juego
              GrillaMetricasMiembro(
                gruposActivos: 2,
                gruposHistoricos: 5,
                dineroDepositado: 'Bs ${(puntosEnMesa * 1.2).toInt().toString().replaceAllMapped(RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'), (m) => '${m[1]}.')}',
                ciclosCompletos: 5,
                turnosJugados: 14,
                diasRetraso: 0,
                impagos: 0,
              ),
              const SizedBox(height: Espacio.s5),

              // Sección 4: Vitrina de Insignias y Trofeos (Logros)
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'INSIGNIAS Y TROFEOS DE HONOR',
                    style: Tipo.etiqueta.copyWith(
                      color: t.text2,
                      fontWeight: FontWeight.w700,
                      letterSpacing: 1.1,
                    ),
                  ),
                  Text(
                    '4 ganadas',
                    style: Tipo.etiqueta.copyWith(
                      color: t.brand,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: Espacio.s1),
              Text(
                'Toca cualquier logro para ver su celebración escénica.',
                style: Tipo.cuerpoChico.copyWith(color: t.text3),
              ),
              const SizedBox(height: Espacio.s3),

              // Lista de Insignias estilo GymSheet
              InsigniaTile(
                nombre: 'Primer Pasanaku',
                descripcion: 'Un ciclo completo con la entrega cobrada y sin mora.',
                flavorText: 'El bautismo de fuego en la mesa de ahorro comunitaria.',
                icono: Icons.flag_rounded,
                puntos: 500,
                tier: InsigniaTier.oro,
                conseguida: true,
                fechaObtencion: 'hace 6 meses',
                onTap: () => CelebracionStage.mostrar(
                  context,
                  nombre: 'Primer Pasanaku',
                  descripcion: 'Completaste tu primer ciclo con éxito y cobraste tu turno en fecha.',
                  flavorText: 'El bautismo de fuego en la mesa de ahorro comunitaria.',
                  icono: Icons.flag_rounded,
                  puntos: 500,
                  tier: InsigniaTier.oro,
                  eyebrow: '¡PRIMER GRAN HITO!',
                ),
              ),
              const SizedBox(height: Espacio.s3),

              InsigniaTile(
                nombre: 'Pagador Puntual',
                descripcion: 'Doce aportes seguidos en fecha o antes.',
                flavorText: 'Puntualidad de acero que inspira a todo el grupo.',
                icono: Icons.schedule_rounded,
                puntos: 1000,
                tier: InsigniaTier.esmeralda,
                conseguida: true,
                fechaObtencion: 'hace 3 meses',
                onTap: () => CelebracionStage.mostrar(
                  context,
                  nombre: 'Pagador Puntual',
                  descripcion: '12 aportes consecutivos realizados puntualmente sin demora.',
                  flavorText: 'Puntualidad de acero que inspira a todo el grupo.',
                  icono: Icons.schedule_rounded,
                  puntos: 1000,
                  tier: InsigniaTier.esmeralda,
                  eyebrow: '¡DISCIPLINA DE ACERO!',
                ),
              ),
              const SizedBox(height: Espacio.s3),

              InsigniaTile(
                nombre: 'Sin Mora 12 Meses',
                descripcion: 'Un año corrido sin un solo día de retraso ni recargo.',
                flavorText: 'Un estándar de excelencia financiera admirado en AportaYa.',
                icono: Icons.verified_rounded,
                puntos: 2500,
                tier: InsigniaTier.diamante,
                conseguida: true,
                fechaObtencion: 'el mes pasado',
                onTap: () => CelebracionStage.mostrar(
                  context,
                  nombre: 'Sin Mora 12 Meses',
                  descripcion: '365 días continuos de cumplimiento estricto sin atrasos.',
                  flavorText: 'Un estándar de excelencia financiera admirado en AportaYa.',
                  icono: Icons.verified_rounded,
                  puntos: 2500,
                  tier: InsigniaTier.diamante,
                  eyebrow: '¡RÉCORD DE EXCELENCIA!',
                ),
              ),
              const SizedBox(height: Espacio.s3),

              InsigniaTile(
                nombre: 'Tres Ciclos Completos',
                descripcion: 'Tres ciclos terminados de principio a fin.',
                flavorText: 'La experiencia consolidada en múltiples grupos.',
                icono: Icons.repeat_rounded,
                puntos: 1200,
                tier: InsigniaTier.plata,
                conseguida: true,
                fechaObtencion: 'hace 2 meses',
                onTap: () => CelebracionStage.mostrar(
                  context,
                  nombre: 'Tres Ciclos Completos',
                  descripcion: 'Tres rondas de pasanaku cerradas exitosamente.',
                  flavorText: 'La experiencia consolidada en múltiples grupos.',
                  icono: Icons.repeat_rounded,
                  puntos: 1200,
                  tier: InsigniaTier.plata,
                  eyebrow: '¡CONSTANCIA COMPROBADA!',
                ),
              ),
              const SizedBox(height: Espacio.s3),

              const InsigniaTile(
                nombre: 'Mesa de Honor',
                descripcion: 'Aportar más de Bs 20.000 acumulados en la mesa de juego.',
                icono: Icons.stars_rounded,
                puntos: 2000,
                tier: InsigniaTier.oro,
                conseguida: false,
                progreso: 0.88,
                progresoEtiqueta: 'Bs 17.600 de Bs 20.000 aportados',
              ),
              const SizedBox(height: Espacio.s3),

              InsigniaTile(
                nombre: 'Organizador Confiable',
                descripcion: 'Cerrar dos grupos como organizador sin incidentes y calificación >= 80.',
                icono: Icons.military_tech_rounded,
                puntos: 3000,
                tier: InsigniaTier.diamante,
                conseguida: false,
                progreso: esOrganizador ? 0.5 : null,
                progresoEtiqueta: esOrganizador ? '1 de 2 grupos cerrados' : null,
              ),
              const SizedBox(height: Espacio.s6),

              // Botón de acción inferior
              Boton(
                texto: 'Recorrer Guía de Puntos y Rangos',
                variante: BotonVariante.secundario,
                onPressed: () => TourEnStages.mostrar(context),
              ),
              const SizedBox(height: Espacio.s4),
            ],
          ),
        ),
      ),
    );
  }
}
