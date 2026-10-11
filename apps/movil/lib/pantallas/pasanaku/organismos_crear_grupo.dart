import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/errores.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/pregunta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'grilla_de_opciones.dart';
import 'grupo_creado.dart';
import 'opciones_de_grupo.dart';
import 'pantalla_crear_grupo.dart';
import 'textos.dart';

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
    if (resultado != null) {
      return GrupoCreado(
        grupoId: '${resultado.grupoId}',
        fondoPorPeriodo: resultado.fondoPorPeriodo as String,
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
            // Carril H3: con el servidor TEST real el rechazo (p. ej. 422 AP-CU20-04) no se veía:
            // el botón dejaba de girar y no pasaba nada. Se dice arriba de la acción, siempre visible.
            if (estado.hasError && !estado.isLoading)
              Padding(
                padding: const EdgeInsets.fromLTRB(
                  Espacio.s4,
                  Espacio.s2,
                  Espacio.s4,
                  0,
                ),
                child: _RechazoAlCrear(error: estado.error!),
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

/// El rechazo de crear grupo, en lenguaje humano y con la traza para soporte.
class _RechazoAlCrear extends StatelessWidget {
  const _RechazoAlCrear({required this.error});
  final Object error;

  @override
  Widget build(BuildContext context) {
    final presentable = error is ErrorPresentable
        ? error as ErrorPresentable
        : null;
    final traza = presentable?.trazaId;
    return Alerta(
      titulo:
          presentable?.mensaje ??
          'No pudimos crear el grupo. Probá de nuevo en un momento.',
      detalle: traza == null ? null : 'Código de seguimiento: $traza',
      tono: Tono.error,
    );
  }
}
