import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/cu20_crear_grupo.dart';
import 'organismos_crear_grupo.dart';
import 'textos.dart';

/// El contrato que `organismos_crear_grupo.dart` necesita del estado del formulario,
/// sin exponer el `State` completo (arquitectura atómica: la pantalla es dueña del
/// estado, el organismo solo lo lee y lo cambia por acá).
abstract class PantallaCrearGrupoControles {
  TextEditingController get nombre;
  abstract String? monto;
  abstract String periodicidad;
  abstract String modalidad;
  abstract DateTime? fechaDeInicio;
  abstract bool permitePermutas;
  bool get completo;
}

/// CU-20 · crear grupo. Antes del formulario se chequea si quien organiza está
/// habilitado (D-16): si no, la pantalla explica por qué en vez de dejar completar
/// un formulario que el backend va a rechazar igual — el mismo patrón que D-20 usa
/// para el mercado.
class PantallaCrearGrupo extends ConsumerStatefulWidget {
  const PantallaCrearGrupo({super.key, this.organizadorId});

  /// Nulo cuando el grupo lo administra la plataforma (RN-18): no hace falta
  /// habilitación de organizador.
  final String? organizadorId;

  @override
  ConsumerState<PantallaCrearGrupo> createState() => _PantallaCrearGrupoState();
}

class _PantallaCrearGrupoState extends ConsumerState<PantallaCrearGrupo>
    implements PantallaCrearGrupoControles {
  @override
  final nombre = TextEditingController();
  @override
  String? monto;
  @override
  String periodicidad = 'MENSUAL';
  @override
  String modalidad = 'SORTEO_ALEATORIO';
  final int _cupos = 10;
  final int _diaCobro = 5;
  @override
  DateTime? fechaDeInicio;
  @override
  bool permitePermutas = false;

  @override
  void dispose() {
    nombre.dispose();
    super.dispose();
  }

  @override
  bool get completo =>
      nombre.text.trim().length >= 3 && monto != null && fechaDeInicio != null;

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(crearGrupoProvider);
    final resultado = estado.value;

    if (widget.organizadorId != null) {
      final habilitacion = ref.watch(
        habilitacionOrganizadorProvider(widget.organizadorId!),
      );
      return Scaffold(
        appBar: AppBar(title: const Text(TextosPasanaku.tituloCrearGrupo)),
        body: EstadoDePantalla(
          valor: habilitacion,
          etiquetaDeCarga: TextosPasanaku.cargando,
          mensajeVacio: TextosPasanaku.requisitosAyuda,
          reintentar: () => ref.invalidate(
            habilitacionOrganizadorProvider(widget.organizadorId!),
          ),
          exito: (h) {
            if (!h.habilitado) {
              return NoHabilitado(nivel: h.nivel);
            }
            return FormularioCrearGrupo(
              estado: estado,
              resultado: resultado,
              onEnviar: _enviar,
              controles: this,
            );
          },
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloCrearGrupo)),
      body: FormularioCrearGrupo(
        estado: estado,
        resultado: resultado,
        onEnviar: _enviar,
        controles: this,
      ),
    );
  }

  Future<void> _enviar() async {
    if (!completo) return;
    await ref
        .read(crearGrupoProvider.notifier)
        .enviar(
          nombre: nombre.text.trim(),
          montoAporte: monto!,
          periodicidad: EntradaGrupoPeriodicidadEnum.values.byName(
            periodicidad,
          ),
          cupos: _cupos,
          diaCobro: _diaCobro,
          modalidadTurnos: EntradaGrupoModalidadTurnosEnum.values.byName(
            modalidad,
          ),
          fechaDeInicio: fechaDeInicio!,
          organizadorId: widget.organizadorId,
          permitePermutaDeTurnos: permitePermutas,
        );
  }
}
