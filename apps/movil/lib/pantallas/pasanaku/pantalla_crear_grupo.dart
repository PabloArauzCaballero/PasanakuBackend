import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'contrato_de_organizador.dart';
import 'dominio/cu20_crear_grupo.dart';
import 'dominio/habilitacion_basica.dart';
import 'no_habilitado.dart';
import 'organismos_crear_grupo.dart';
import 'textos.dart';

/// Cuántas personas se asumen al crear el grupo mientras el formulario no pregunte la
/// cantidad. Está acá arriba, y no escondido en el estado, porque la ayuda del monto lo
/// usa para adelantar cuánto junta cada turno: el número que se muestra y el que se
/// envía tienen que ser el mismo.
const cuposPorDefecto = 10;

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
  final int _cupos = cuposPorDefecto;
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

  /// La fecha de inicio NO es requisito para habilitar el botón: el formulario no la
  /// pregunta y se completa al enviar (hoy + 7 días, `organismos_crear_grupo.dart`). Exigirla
  /// acá dejaba «Crear grupo» apagado para siempre (hallazgo del carril H en emulador).
  @override
  bool get completo =>
      nombre.text.trim().length >= 3 && monto != null && _hayImporte(monto!);

  /// Carril H3: «00,00» era un monto válido para el campo y dejaba «Crear grupo» encendido. Un aporte en
  /// cero no es un aporte: hace falta al menos un dígito distinto de cero.
  static bool _hayImporte(String monto) => RegExp(r'[1-9]').hasMatch(monto);

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(crearGrupoProvider);
    final resultado = estado.value;

    if (widget.organizadorId != null) {
      final habilitacion = ref.watch(
        habilitacionOrganizadorProvider(widget.organizadorId!),
      );
      return _ConCabecera(
        hijo: EstadoDePantalla(
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

    // Sin `organizadorId` en la ruta: es la persona la que quiere organizar. Si todavía
    // no es organizadora, primero ve y acepta el contrato Aprendiz (CU-90 vía básica).
    final basica = ref.watch(habilitacionBasicaProvider);
    return _ConCabecera(
      hijo: EstadoDePantalla(
        valor: basica,
        etiquetaDeCarga: TextosPasanaku.cargando,
        mensajeVacio: TextosPasanaku.requisitosAyuda,
        reintentar: () => ref.invalidate(habilitacionBasicaProvider),
        exito: (h) {
          if (!h.habilitado) {
            return ContratoDeOrganizadorVista(
              contrato: h,
              onAceptado: () => ref.invalidate(habilitacionBasicaProvider),
            );
          }
          _organizadorPropio = h.organizadorId;
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

  /// El `organizadorId` de quien crea, cuando no vino en la ruta.
  String? _organizadorPropio;

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
          organizadorId: widget.organizadorId ?? _organizadorPropio,
          permitePermutaDeTurnos: permitePermutas,
        );
  }
}

/// Cabecera propia de la pantalla, en vez del `AppBar` con título centrado: la acción
/// de la pantalla queda anclada abajo, así que el `Scaffold` no lleva barra arriba.
class _ConCabecera extends StatelessWidget {
  const _ConCabecera({required this.hijo});

  final Widget hijo;

  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      bottom: false,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: Espacio.s4),
            child: CabeceraDeSeccion(titulo: TextosPasanaku.tituloCrearGrupo),
          ),
          // El margen lateral lo pone cada parte, no este contenedor: la barra de
          // acción tiene que llegar a los dos bordes de la pantalla para leerse como
          // el piso de la vista, y no como un panel flotando con fondo a los costados.
          Expanded(child: hijo),
        ],
      ),
    ),
  );
}
