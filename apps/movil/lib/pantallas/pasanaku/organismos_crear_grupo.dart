import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/pregunta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'grilla_de_opciones.dart';
import 'pantalla_crear_grupo.dart';
import 'textos.dart';

const periodicidades = {
  'SEMANAL': (nombre: 'Semanal', alAno: '52 turnos al año'),
  'QUINCENAL': (nombre: 'Quincenal', alAno: '24 al año'),
  'MENSUAL': (nombre: 'Mensual', alAno: '12 al año'),
  'BIMENSUAL': (nombre: 'Bimensual', alAno: '6 al año'),
};

const modalidades = {
  'SORTEO_ALEATORIO': (
    nombre: 'Sorteo al azar',
    detalle: 'Nadie elige: el orden sale sorteado y verificable',
  ),
  'ORDEN_DE_INGRESO': (
    nombre: 'Orden de ingreso',
    detalle: 'Cobra primero quien entró primero',
  ),
  'ACUERDO_MANUAL': (
    nombre: 'Lo acuerda el grupo',
    detalle: 'Ustedes fijan el orden entre todos',
  ),
};

/// El formulario, como conversación: una pregunta por bloque, las opciones
/// excluyentes como tarjetas elegibles, y la acción anclada abajo para que no haya
/// que desplazarse hasta el final para descubrir que existe.
class FormularioCrearGrupo extends StatelessWidget {
  const FormularioCrearGrupo({
    super.key,
    required this.estado,
    required this.resultado,
    required this.onEnviar,
    required this.controles,
  });
  final AsyncValue<dynamic> estado;
  final dynamic resultado;
  final Future<void> Function() onEnviar;
  final PantallaCrearGrupoControles controles;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    if (resultado != null) {
      return Padding(
        padding: const EdgeInsets.all(Espacio.s4),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              TextosPasanaku.grupoCreado,
              style: Tipo.titulo2.copyWith(color: t.text),
            ),
            const SizedBox(height: Espacio.s2),
            Text(
              'Fondo por período: ${resultado.fondoPorPeriodo}',
              style: Tipo.cuerpo.copyWith(color: t.text2),
            ),
          ],
        ),
      );
    }
    return StatefulBuilder(
      builder: (context, setState) {
        return Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(
                  Espacio.s4,
                  Espacio.s3,
                  Espacio.s4,
                  Espacio.s5,
                ),
                children: [
                  Pregunta(
                    etiqueta: TextosPasanaku.nombreDelGrupo,
                    control: Campo(
                      etiqueta: '',
                      controlador: controles.nombre,
                      onChanged: (_) => setState(() {}),
                    ),
                  ),
                  const SizedBox(height: Espacio.s5),
                  Pregunta(
                    etiqueta: TextosPasanaku.montoAporte,
                    ayuda: _ayudaDelFondo(controles.monto),
                    control: CampoMonto(
                      etiqueta: '',
                      onChanged: (v) => setState(() => controles.monto = v),
                    ),
                  ),
                  const SizedBox(height: Espacio.s5),
                  Pregunta(
                    etiqueta: TextosPasanaku.periodicidad,
                    control: GrillaDeOpciones(
                      opciones: [
                        for (final e in periodicidades.entries)
                          (
                            clave: e.key,
                            titulo: e.value.nombre,
                            detalle: e.value.alAno,
                          ),
                      ],
                      elegida: controles.periodicidad,
                      onElegir: (v) =>
                          setState(() => controles.periodicidad = v),
                    ),
                  ),
                  const SizedBox(height: Espacio.s5),
                  Pregunta(
                    etiqueta: TextosPasanaku.modalidadTurnos,
                    control: GrillaDeOpciones(
                      columnas: 1,
                      opciones: [
                        for (final e in modalidades.entries)
                          (
                            clave: e.key,
                            titulo: e.value.nombre,
                            detalle: e.value.detalle,
                          ),
                      ],
                      elegida: controles.modalidad,
                      onElegir: (v) => setState(() => controles.modalidad = v),
                    ),
                  ),
                  const SizedBox(height: Espacio.s5),
                  Casilla(
                    etiqueta: TextosPasanaku.permitePermutas,
                    valor: controles.permitePermutas,
                    onChanged: (v) =>
                        setState(() => controles.permitePermutas = v),
                  ),
                ],
              ),
            ),
            BarraDeAccion(
              hijo: Boton(
                texto: TextosPasanaku.crearGrupoBoton,
                variante: BotonVariante.primario,
                expandido: true,
                cargando: estado.isLoading,
                onPressed: controles.completo
                    ? () {
                        controles.fechaDeInicio ??= DateTime.now().add(
                          const Duration(days: 7),
                        );
                        onEnviar();
                      }
                    : null,
              ),
            ),
          ],
        );
      },
    );
  }

  /// La ayuda calcula la consecuencia en vez de repetir el formato: es lo que la
  /// persona está tratando de averiguar cuando escribe el monto.
  String? _ayudaDelFondo(String? monto) {
    if (monto == null || monto.isEmpty) return null;
    try {
      final fondo = formatearMonto(
        monto: multiplicarMonto(monto: monto, veces: cuposPorDefecto),
        moneda: 'BOB',
      );
      return 'Con $cuposPorDefecto personas, cada turno junta $fondo';
    } on FormatException {
      // Todavía está escribiendo: no hay consecuencia que adelantar.
      return null;
    }
  }
}
