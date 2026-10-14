import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/estado_alta.dart';
import '../textos.dart';
import '../textos_del_alta.dart';
import 'bloque_de_formulario.dart';
import 'campo_de_actividad.dart';
import 'campo_de_detalle.dart';
import 'catalogo_del_perfil.dart';
import 'tarjeta_de_confianza.dart';

/// Paso de perfil transaccional declarado: origen, actividad y monto estimado.
class PasoActividad extends ConsumerStatefulWidget {
  const PasoActividad({super.key});

  @override
  ConsumerState<PasoActividad> createState() => _PasoActividadState();
}

class _PasoActividadState extends ConsumerState<PasoActividad> {
  // En `initState` y no `late final ... = ref.read(...)`: un `late` sin tocar se
  // inicializa al primer uso, que puede ser `dispose()` (el detalle nunca mostrado),
  // y ahí el `ref` ya no sirve y el paso revienta al salir.
  late final TextEditingController _detalle;
  late final TextEditingController _cual;
  late final TextEditingController _monto;

  String? _origen;
  String? _actividad;

  /// Los errores aparecen al intentar continuar, no mientras se completa: retar a
  /// alguien por un campo que todavía no llegó a tocar es ruido.
  var _intentado = false;

  @override
  void initState() {
    super.initState();
    final estado = ref.read(altaProvider);
    _detalle = TextEditingController(text: estado.detalleDelOrigen);
    _cual = TextEditingController(text: estado.detalleDeLaActividad);
    _monto = TextEditingController(
      text: estado.montoMensualEstimado?.round().toString() ?? '',
    );
    String? elegido(String codigo) => codigo.isEmpty ? null : codigo;
    _origen = elegido(estado.origenDeFondos);
    _actividad = elegido(estado.actividadEconomica);
  }

  @override
  void dispose() {
    _detalle.dispose();
    _cual.dispose();
    _monto.dispose();
    super.dispose();
  }

  bool get _pideDetalle => _origen == origenOtro;

  String? get _errorOrigen =>
      _origen == null ? TextosDelAlta.origenFalta : null;

  bool get _pideCual => _actividad == actividadOtra;

  String? get _errorDetalle => _pideDetalle && !CampoDeDetalle.alcanza(_detalle)
      ? TextosDelAlta.origenDetalleFalta
      : null;

  String? get _errorActividad => _actividad == null
      ? TextosDelAlta.actividadFalta
      : _pideCual && !CampoDeDetalle.alcanza(_cual)
      ? TextosDelAlta.actividadDetalleFalta
      : null;

  /// El monto es opcional; si se informa, debe ser mayor que cero.
  String? get _errorMonto {
    final texto = _monto.text.trim();
    if (texto.isEmpty) return null;
    final valor = double.tryParse(texto);
    if (valor == null || valor <= 0) return TextosDelAlta.montoInvalido;
    return null;
  }

  bool get _valido => [
    _errorOrigen,
    _errorDetalle,
    _errorActividad,
    _errorMonto,
  ].every((e) => e == null);

  void _continuar() {
    setState(() => _intentado = true);
    if (!_valido) return;
    ref
        .read(altaProvider.notifier)
        .actualizarPerfilTransaccional(
          origen: _origen!,
          detalleDelOrigen: _pideDetalle ? _detalle.text.trim() : '',
          actividad: _actividad!,
          detalleDeLaActividad: _pideCual ? _cual.text.trim() : '',
          monto: double.tryParse(_monto.text.trim()),
        );
    ref.read(altaProvider.notifier).siguiente();
  }

  /// Solo muestra el error cuando ya se intentó seguir.
  String? _visible(String? error) => _intentado ? error : null;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          BloqueDeFormulario(
            icono: Icons.work_outline_rounded,
            titulo: 'Tu actividad',
            hijos: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  CampoDeSeleccion<String>(
                    etiqueta: TextosDelAlta.origenDeFondos,
                    icono: Icons.savings_outlined,
                    ayuda: TextosDelAlta.origenDeFondosAyuda,
                    textoVacio: TextosDelAlta.elegirOpcion,
                    valor: _origen,
                    error: _visible(_errorOrigen),
                    exito: _origen != null,
                    opciones: [
                      for (final o in origenesDeFondos)
                        (valor: o.codigo, texto: o.texto, detalle: null),
                    ],
                    onElegida: (v) => setState(() => _origen = v),
                  ),
                  if (_pideDetalle)
                    CampoDeDetalle(
                      etiqueta: TextosDelAlta.origenDetalle,
                      ayuda: TextosDelAlta.origenDetalleAyuda,
                      controlador: _detalle,
                      error: _visible(_errorDetalle),
                      onCambio: () => setState(() {}),
                    ),
                  const SizedBox(height: Espacio.s3),
                  CampoDeActividad(
                    valor: _actividad,
                    cual: _cual,
                    error: _visible(_errorActividad),
                    onElegida: (v) => setState(() => _actividad = v),
                    onCambio: () => setState(() {}),
                  ),
                  const SizedBox(height: Espacio.s3),
                  Campo(
                    etiqueta: TextosDelAlta.montoMensualEstimado,
                    controlador: _monto,
                    prefijo: 'Bs',
                    ayuda: TextosDelAlta.montoMensualAyuda,
                    lineasDeAyuda: 5,
                    error: _visible(_errorMonto),
                    tipoDeTeclado: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    formateadores: [
                      FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                      LengthLimitingTextInputFormatter(9),
                    ],
                    onChanged: (_) => setState(() {}),
                  ),
                  const SizedBox(height: Espacio.s5),
                  Boton(
                    texto: TextosIdentidad.continuar,
                    variante: BotonVariante.primario,
                    expandido: true,
                    // Siempre tocable: si falta algo, el toque muestra qué falta. Un botón
                    // apagado que no dice por qué deja a alguien mirando la pantalla.
                    onPressed: _continuar,
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: Espacio.s4),
          const TarjetaDeConfianza(),
        ],
      ),
    );
  }
}
