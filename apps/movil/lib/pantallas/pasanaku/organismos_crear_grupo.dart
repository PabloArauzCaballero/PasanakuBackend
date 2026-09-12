import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/atomos/grupo_radio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'pantalla_crear_grupo.dart';
import 'textos.dart';

const periodicidades = {
  'SEMANAL': 'Semanal',
  'QUINCENAL': 'Quincenal',
  'MENSUAL': 'Mensual',
  'BIMENSUAL': 'Bimensual',
};

const modalidades = {
  'SORTEO_ALEATORIO': 'Sorteo al azar',
  'ORDEN_DE_INGRESO': 'Orden de ingreso',
  'ACUERDO_MANUAL': 'Lo acuerda el grupo',
};

/// Piezas de `PantallaCrearGrupo` separadas en su propio archivo (arquitectura
/// atómica: la pantalla queda como composición, no como un formulario de 250+
/// líneas en un solo archivo).
class NoHabilitado extends StatelessWidget {
  const NoHabilitado({super.key, required this.nivel});
  final String nivel;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            'Todavía no podés organizar un grupo',
            style: texto.titleLarge?.copyWith(color: t.text),
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            'Tu nivel actual es $nivel. ${TextosPasanaku.capacitacionVencidaAviso}',
            style: texto.bodyMedium?.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            TextosPasanaku.requisitosAyuda,
            style: texto.bodySmall?.copyWith(color: t.text3),
          ),
        ],
      ),
    );
  }
}

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
      return Padding(
        padding: const EdgeInsets.all(Espacio.s4),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(TextosPasanaku.grupoCreado),
            const SizedBox(height: Espacio.s2),
            Text('Fondo por período: ${resultado.fondoPorPeriodo}'),
          ],
        ),
      );
    }
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(Espacio.s4),
        child: StatefulBuilder(
          builder: (context, setState) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Campo(
                  etiqueta: TextosPasanaku.nombreDelGrupo,
                  controlador: controles.nombre,
                  onChanged: (_) => setState(() {}),
                ),
                const SizedBox(height: Espacio.s4),
                CampoMonto(
                  etiqueta: TextosPasanaku.montoAporte,
                  onChanged: (v) => setState(() => controles.monto = v),
                ),
                const SizedBox(height: Espacio.s4),
                const Text(TextosPasanaku.periodicidad),
                GrupoRadio<String>(
                  opciones: periodicidades,
                  valor: controles.periodicidad,
                  onChanged: (v) =>
                      setState(() => controles.periodicidad = v ?? 'MENSUAL'),
                ),
                const SizedBox(height: Espacio.s4),
                const Text(TextosPasanaku.modalidadTurnos),
                GrupoRadio<String>(
                  opciones: modalidades,
                  valor: controles.modalidad,
                  onChanged: (v) => setState(
                    () => controles.modalidad = v ?? 'SORTEO_ALEATORIO',
                  ),
                ),
                const SizedBox(height: Espacio.s4),
                Casilla(
                  etiqueta: TextosPasanaku.permitePermutas,
                  valor: controles.permitePermutas,
                  onChanged: (v) =>
                      setState(() => controles.permitePermutas = v),
                ),
                const SizedBox(height: Espacio.s4),
                Boton(
                  texto: TextosPasanaku.crearGrupoBoton,
                  variante: BotonVariante.primario,
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
              ],
            );
          },
        ),
      ),
    );
  }
}
