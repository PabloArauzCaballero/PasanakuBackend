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
import 'catalogo_del_perfil.dart';

/// Paso 7 de 8 — perfil transaccional declarado (CU-01 flujo 7): origen de fondos,
/// actividad económica y monto mensual estimado.
///
/// **Los dos primeros se eligen de una lista, no se escriben.** Eran campos de texto
/// vacíos: quien llegaba acá tenía que adivinar qué se esperaba —¿«sueldo»?,
/// ¿«asalariado»?, ¿«mi trabajo»?— y del otro lado quedaban tantas respuestas
/// distintas como personas. El perfil declarado existe para poder compararlo contra
/// lo que después se opera; con texto libre no hay nada que comparar. Las listas están
/// en `catalogo_del_perfil.dart` y usan los códigos que ya acepta la base.
///
/// «Otro» abre un campo para escribirlo: una lista cerrada sin salida obliga a mentir.
class PasoPerfilTransaccional extends ConsumerStatefulWidget {
  const PasoPerfilTransaccional({super.key});

  @override
  ConsumerState<PasoPerfilTransaccional> createState() =>
      _PasoPerfilTransaccionalState();
}

class _PasoPerfilTransaccionalState
    extends ConsumerState<PasoPerfilTransaccional> {
  // Se crean en `initState` y no con `late final ... = ref.read(...)`: un `late`
  // que todavía no se tocó se inicializa la primera vez que alguien lo lee, y la
  // primera vez puede ser `dispose()` —cuando el campo de detalle nunca se mostró
  // porque el origen no fue «Otro»—. Ahí el `ref` ya no se puede usar y el paso
  // revienta al salir.
  late final TextEditingController _detalle;
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
    _monto = TextEditingController(
      text: estado.montoMensualEstimado?.round().toString() ?? '',
    );
    _origen = estado.origenDeFondos.isEmpty ? null : estado.origenDeFondos;
    _actividad = estado.actividadEconomica.isEmpty
        ? null
        : estado.actividadEconomica;
  }

  @override
  void dispose() {
    _detalle.dispose();
    _monto.dispose();
    super.dispose();
  }

  bool get _pideDetalle => _origen == origenOtro;

  String? get _errorOrigen =>
      _origen == null ? TextosDelAlta.origenFalta : null;

  String? get _errorDetalle => _pideDetalle && _detalle.text.trim().length < 3
      ? TextosDelAlta.origenDetalleFalta
      : null;

  String? get _errorActividad =>
      _actividad == null ? TextosDelAlta.actividadFalta : null;

  /// El monto es opcional —quien no sabe cuánto va a mover no tiene que inventarlo—,
  /// pero si escribe algo tiene que ser un número mayor que cero: un perfil declarado
  /// en Bs 0 no dice nada y después dispara desvío con la primera recarga.
  String? get _errorMonto {
    final texto = _monto.text.trim();
    if (texto.isEmpty) return null;
    final valor = double.tryParse(texto);
    if (valor == null || valor <= 0) return TextosDelAlta.montoInvalido;
    return null;
  }

  bool get _valido =>
      _errorOrigen == null &&
      _errorDetalle == null &&
      _errorActividad == null &&
      _errorMonto == null;

  void _continuar() {
    setState(() => _intentado = true);
    if (!_valido) return;
    ref
        .read(altaProvider.notifier)
        .actualizarPerfilTransaccional(
          origen: _origen!,
          detalleDelOrigen: _pideDetalle ? _detalle.text.trim() : '',
          actividad: _actividad!,
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
                (valor: o.codigo, texto: o.texto),
            ],
            onElegida: (v) => setState(() => _origen = v),
          ),
          if (_pideDetalle) ...[
            const SizedBox(height: Espacio.s3),
            Campo(
              etiqueta: TextosDelAlta.origenDetalle,
              controlador: _detalle,
              icono: Icons.edit_outlined,
              ayuda: TextosDelAlta.origenDetalleAyuda,
              error: _visible(_errorDetalle),
              onChanged: (_) => setState(() {}),
            ),
          ],
          const SizedBox(height: Espacio.s3),
          CampoDeSeleccion<String>(
            etiqueta: TextosDelAlta.actividadEconomica,
            icono: Icons.work_outline,
            ayuda: TextosDelAlta.actividadEconomicaAyuda,
            textoVacio: TextosDelAlta.elegirOpcion,
            valor: _actividad,
            error: _visible(_errorActividad),
            exito: _actividad != null,
            opciones: [
              for (final a in actividadesEconomicas)
                (valor: a.codigo, texto: a.texto),
            ],
            onElegida: (v) => setState(() => _actividad = v),
          ),
          const SizedBox(height: Espacio.s3),
          Campo(
            etiqueta: TextosDelAlta.montoMensualEstimado,
            controlador: _monto,
            prefijo: 'Bs',
            ayuda: TextosDelAlta.montoMensualAyuda,
            error: _visible(_errorMonto),
            tipoDeTeclado: const TextInputType.numberWithOptions(decimal: true),
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
    );
  }
}
