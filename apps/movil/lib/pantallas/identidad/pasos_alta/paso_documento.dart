import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_diseno/moleculas/referencia_de_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/errores.dart';
import '../dominio/estado_alta.dart';
import '../dominio/validaciones.dart';
import '../textos.dart';
import '../textos_del_alta.dart';
import 'bloque_de_formulario.dart';
import 'campos_del_carnet.dart';
import 'departamentos.dart';
import 'formateadores_del_alta.dart';
import 'paso_capturas.dart';
import 'tarjeta_de_confianza.dart';

/// Fase «Tu identidad»: las capturas y los datos legibles del documento van juntos.
class PasoDocumento extends ConsumerStatefulWidget {
  const PasoDocumento({super.key});

  @override
  ConsumerState<PasoDocumento> createState() => _PasoDocumentoState();
}

class _PasoDocumentoState extends ConsumerState<PasoDocumento> {
  late final _numero = TextEditingController(
    text: ref.read(altaProvider).datos.numeroDocumento,
  );
  final _focoNumero = FocusNode();
  final _focoLugar = FocusNode();
  final _focoVence = FocusNode();
  String? _lugar;
  DateTime? _vence;
  var _intentado = false;

  @override
  void dispose() {
    _numero.dispose();
    _focoNumero.dispose();
    _focoLugar.dispose();
    _focoVence.dispose();
    super.dispose();
  }

  void _sincronizar() {
    final actual = ref.read(altaProvider).datos;
    ref
        .read(altaProvider.notifier)
        .actualizarDatos(
          actual.copiarCon(
            numeroDocumento: _numero.text,
            lugarExpedicion: _lugar,
            fechaExpiracion: _vence,
          ),
        );
    setState(() {});
  }

  Map<String, String?> get _errores => {
    'documento': _intentado ? errorDocumento(_numero.text) : null,
    'lugar': _intentado ? errorLugarExpedicion(_lugar) : null,
    'vence': _intentado ? errorVencimientoDocumento(_vence) : null,
  };

  void _continuar() {
    setState(() {
      _intentado = true;
      _lugar ??= ref.read(altaProvider).datos.lugarExpedicion;
      _vence ??= ref.read(altaProvider).datos.fechaExpiracion;
    });
    _sincronizar();
    final errores = _errores;
    if (errores['documento'] != null) {
      _focoNumero.requestFocus();
      return;
    }
    if (errores['lugar'] != null) {
      _focoLugar.requestFocus();
      return;
    }
    if (errores['vence'] != null) {
      _focoVence.requestFocus();
      return;
    }
    ref.read(altaProvider.notifier).siguiente();
  }

  Widget _datosDelDocumento() {
    final hoy = DateTime.now();
    _lugar ??= ref.read(altaProvider).datos.lugarExpedicion;
    _vence ??= ref.read(altaProvider).datos.fechaExpiracion;
    final alta = ref.watch(altaProvider);
    final errorRemoto = alta.campoError == CampoDeErrorApi.fechaExpiracion
        ? alta.error
        : null;
    final campoNumero = Campo(
      etiqueta: TextosIdentidad.numeroDocumento,
      controlador: _numero,
      foco: _focoNumero,
      icono: Icons.badge_outlined,
      ejemplo: TextosDelAlta.ejemploNumeroDocumento,
      ayuda: TextosIdentidad.documentoAyuda,
      lineasDeAyuda: 8,
      error: _errores['documento'],
      tipoDeTeclado: TextInputType.number,
      formateadores: formateadoresDelNumeroDeCi,
      onChanged: (_) => _sincronizar(),
    );
    final campoExpedicion = CampoDeSeleccion<String>(
      etiqueta: TextosIdentidad.lugarExpedicion,
      valor: _lugar,
      textoVacio: TextosIdentidad.lugarExpedicionVacio,
      ayuda: TextosIdentidad.lugarExpedicionAyuda,
      error: _errores['lugar'],
      foco: _focoLugar,
      opciones: [
        for (final departamento in departamentosDelCarnet)
          (
            valor: departamento.sigla,
            texto: departamento.nombre,
            detalle: null,
          ),
      ],
      onElegida: (lugar) {
        _lugar = lugar;
        _sincronizar();
      },
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        BloqueDeFormulario(
          icono: Icons.badge_outlined,
          titulo: 'Tu carnet',
          hijos: [
            CamposDelCarnet(numero: campoNumero, expedicion: campoExpedicion),
            const SizedBox(height: Espacio.s3),
            CampoDeFecha(
              etiqueta: TextosIdentidad.fechaExpiracion,
              valor: _vence,
              foco: _focoVence,
              icono: Icons.event_available_outlined,
              primera: DateTime(hoy.year, hoy.month, hoy.day),
              ultima: DateTime(hoy.year + 30, hoy.month, hoy.day),
              inicial: DateTime(hoy.year + 1, hoy.month, hoy.day),
              ayuda: TextosIdentidad.fechaExpiracionAyuda,
              error: errorRemoto ?? _errores['vence'],
              onElegida: (fecha) {
                _vence = fecha;
                _sincronizar();
              },
            ),
          ],
        ),
        if (errorRemoto != null) ...[
          const SizedBox(height: Espacio.s2),
          ReferenciaDeError(trazaId: alta.trazaId),
        ],
      ],
    );
  }

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.all(Espacio.s4),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        PasoCapturas(
          onContinuar: _continuar,
          contenidoAdicional: _datosDelDocumento(),
        ),
        const SizedBox(height: Espacio.s4),
        const TarjetaDeConfianza(),
      ],
    ),
  );
}
