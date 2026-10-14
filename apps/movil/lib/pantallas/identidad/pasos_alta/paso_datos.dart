import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/estado_alta.dart';
import '../dominio/validaciones.dart';
import '../textos.dart';
import 'bloque_de_formulario.dart';
import 'campos_de_nombre.dart';
import 'paso_cotejo.dart';
import 'pie_del_paso_datos.dart';
import 'tarjeta_de_confianza.dart';

/// Fase «Tus datos»: identidad personal más cotejo con la información declarada.
class PasoDatos extends ConsumerStatefulWidget {
  const PasoDatos({super.key});

  @override
  ConsumerState<PasoDatos> createState() => _PasoDatosState();
}

class _PasoDatosState extends ConsumerState<PasoDatos> {
  late final _nombres = [
    _campo(ref.read(altaProvider).datos.primerNombre),
    _campo(ref.read(altaProvider).datos.segundoNombre),
    _campo(ref.read(altaProvider).datos.apellidoPaterno),
    _campo(ref.read(altaProvider).datos.apellidoMaterno),
  ];
  final _focos = List.generate(5, (_) => FocusNode());
  final _focoFecha = FocusNode();
  DateTime? _fechaNacimiento;
  final _tocados = <String>{};
  var _intentado = false;

  TextEditingController _campo(String valor) =>
      TextEditingController(text: valor);

  @override
  void dispose() {
    for (final controller in _nombres) {
      controller.dispose();
    }
    for (final foco in [..._focos, _focoFecha]) {
      foco.dispose();
    }
    super.dispose();
  }

  void _sincronizar(String campo) {
    _tocados.add(campo);
    final actual = ref.read(altaProvider).datos;
    ref
        .read(altaProvider.notifier)
        .actualizarDatos(
          actual.copiarCon(
            primerNombre: _nombres[0].text,
            segundoNombre: _nombres[1].text,
            apellidoPaterno: _nombres[2].text,
            apellidoMaterno: _nombres[3].text,
            fechaNacimiento: _fechaNacimiento,
          ),
        );
    setState(() {});
  }

  Map<String, String?> get _errores {
    String? error(String campo, String? Function() validar) =>
        _intentado || _tocados.contains(campo) ? validar() : null;
    return {
      'primerNombre': error(
        'primerNombre',
        () => errorNombres(_nombres[0].text, _nombres[1].text),
      ),
      'segundoNombre': error(
        'segundoNombre',
        () => errorCampoDeNombre(_nombres[1].text),
      ),
      'apellidoPaterno': error(
        'apellidoPaterno',
        () => errorApellidos(_nombres[2].text, _nombres[3].text),
      ),
      'apellidoMaterno': error(
        'apellidoMaterno',
        () => errorCampoDeNombre(_nombres[3].text),
      ),
      'fecha': error('fecha', () => errorFechaNacimiento(_fechaNacimiento)),
    };
  }

  void _continuar() {
    setState(() => _intentado = true);
    final errores = _errores;
    final primerError = errores.entries.firstWhere(
      (entrada) => entrada.value != null,
      orElse: () => const MapEntry('', null),
    );
    if (primerError.value != null) {
      final foco = switch (primerError.key) {
        'segundoNombre' => _focos[1],
        'apellidoPaterno' => _focos[2],
        'apellidoMaterno' => _focos[3],
        'fecha' => _focoFecha,
        _ => _focos[0],
      };
      foco.requestFocus();
      return;
    }
    ref.read(altaProvider.notifier).siguiente();
  }

  @override
  Widget build(BuildContext context) {
    _fechaNacimiento ??= ref.read(altaProvider).datos.fechaNacimiento;
    final hoy = DateTime.now();
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        Espacio.s4,
        Espacio.s4,
        Espacio.s4,
        Espacio.s6,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          BloqueDeFormulario(
            icono: Icons.person_outline_rounded,
            titulo: 'Quién sos',
            hijos: [
              CamposDeNombre(
                controladores: _nombres,
                focos: _focos,
                errores: _errores,
                onCambio: _sincronizar,
              ),
              const SizedBox(height: Espacio.s3),
              CampoDeFecha(
                etiqueta: TextosIdentidad.fechaNacimiento,
                valor: _fechaNacimiento,
                foco: _focoFecha,
                ultima: DateTime(hoy.year - 18, hoy.month, hoy.day),
                inicial: DateTime(hoy.year - 25, hoy.month, hoy.day),
                ayuda: TextosIdentidad.fechaNacimientoAyuda,
                error: _errores['fecha'],
                onElegida: (fecha) {
                  _fechaNacimiento = fecha;
                  _sincronizar('fecha');
                },
              ),
            ],
          ),
          const SizedBox(height: Espacio.s4),
          const PasoCotejo(soloResumen: true),
          const SizedBox(height: Espacio.s4),
          PieDelPasoDatos(onContinuar: _continuar),
          const SizedBox(height: Espacio.s4),
          const TarjetaDeConfianza(),
        ],
      ),
    );
  }
}
