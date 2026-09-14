import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/estado_alta.dart';
import '../dominio/validaciones.dart';
import '../textos.dart';
import 'campos_del_alta.dart';

/// Paso 1 de 8 — datos personales (CU-01). Un organismo puro: lee y escribe en el
/// notifier, no llama a la red.
///
/// **«Continuar» siempre se puede tocar.** Antes el botón nacía apagado y se
/// encendía solo con el formulario entero válido, sin decir nunca qué faltaba: quien
/// escribía su celular como `71000090` veía un botón muerto y no tenía forma de
/// saber por qué. Ahora el toque siempre hace algo — o avanza, o muestra los errores
/// y lleva el foco al primer campo que falla.
///
/// Los errores no aparecen mientras se escribe por primera vez: un formulario que te
/// grita «escribí al menos dos letras» cuando llevás una sola letra escrita está
/// retando a alguien que todavía no terminó.
class PasoDatos extends ConsumerStatefulWidget {
  const PasoDatos({super.key});

  @override
  ConsumerState<PasoDatos> createState() => _PasoDatosState();
}

class _PasoDatosState extends ConsumerState<PasoDatos> {
  static const _prefijoBolivia = '+591';

  late final _nombres = _campo(ref.read(altaProvider).datos.nombres);
  late final _apellidos = _campo(ref.read(altaProvider).datos.apellidos);
  late final _telefono = _campo(
    ref.read(altaProvider).datos.telefono.replaceFirst(_prefijoBolivia, ''),
  );
  late final _documento = _campo(ref.read(altaProvider).datos.numeroDocumento);
  late final _correo = _campo(ref.read(altaProvider).datos.correo);

  final _focos = List.generate(5, (_) => FocusNode());
  final _focoFecha = FocusNode();
  final _focoLugar = FocusNode();
  DateTime? _fechaNacimiento;
  String? _lugarExpedicion;
  String? _canal;
  final _tocados = <String>{};
  var _intentado = false;

  TextEditingController _campo(String valor) =>
      TextEditingController(text: valor);

  @override
  void dispose() {
    for (final c in [_nombres, _apellidos, _telefono, _documento, _correo]) {
      c.dispose();
    }
    for (final f in [..._focos, _focoFecha, _focoLugar]) {
      f.dispose();
    }
    super.dispose();
  }

  String get _telefonoCompleto =>
      _telefono.text.isEmpty ? '' : '$_prefijoBolivia${_telefono.text}';

  void _sincronizar(String cual) {
    _tocados.add(cual);
    setState(() {
      ref
          .read(altaProvider.notifier)
          .actualizarDatos(
            ref
                .read(altaProvider)
                .datos
                .copiarCon(
                  nombres: _nombres.text,
                  apellidos: _apellidos.text,
                  telefono: _telefonoCompleto,
                  numeroDocumento: _documento.text,
                  correo: _correo.text,
                  canalVerificacion: _canal,
                  lugarExpedicion: _lugarExpedicion,
                  fechaNacimiento: _fechaNacimiento,
                ),
          );
    });
  }

  /// El error de un campo, o `null` mientras no lo hayan tocado ni se haya intentado
  /// continuar.
  String? _error(String cual, String? Function() calcular) =>
      _tocados.contains(cual) || _intentado ? calcular() : null;

  Map<String, String?> get _errores => {
    'nombres': _error('nombres', () => errorNombre(_nombres.text)),
    'apellidos': _error('apellidos', () => errorNombre(_apellidos.text)),
    'telefono': _error('telefono', () => errorTelefono(_telefonoCompleto)),
    'documento': _error('documento', () => errorDocumento(_documento.text)),
    // El correo se exige siempre: una billetera manda extractos y comprobantes, y
    // sin correo no tienen adonde ir. Ademas es lo que habilita elegirlo como canal.
    'correo': _error('correo', () => errorCorreo(_correo.text)),
    'lugar': _error('lugar', () => errorLugarExpedicion(_lugarExpedicion)),
    'fecha': _error('fecha', () => errorFechaNacimiento(_fechaNacimiento)),
  };

  void _continuar() {
    setState(() => _intentado = true);
    final fallan = _errores.entries.where((e) => e.value != null).toList();
    if (fallan.isEmpty) {
      ref.read(altaProvider.notifier).siguiente();
      return;
    }
    // Al primer campo que falla, para no dejar a nadie buscando cuál era.
    const orden = [
      'nombres',
      'apellidos',
      'telefono',
      'documento',
      'correo',
      'lugar',
      'fecha',
    ];
    final primero = orden.indexOf(fallan.first.key);
    (switch (primero) {
      5 => _focoLugar,
      6 => _focoFecha,
      _ => _focos[primero],
    }).requestFocus();
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    _fechaNacimiento ??= ref.read(altaProvider).datos.fechaNacimiento;
    _lugarExpedicion ??= ref.read(altaProvider).datos.lugarExpedicion;
    _canal ??= ref.read(altaProvider).datos.canalVerificacion;
    final errores = _errores;
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
          CamposDelAlta(
            controladores: [
              _nombres,
              _apellidos,
              _telefono,
              _documento,
              _correo,
            ],
            focos: _focos,
            focoFecha: _focoFecha,
            focoLugar: _focoLugar,
            errores: errores,
            tocados: _tocados,
            prefijo: _prefijoBolivia,
            fecha: _fechaNacimiento,
            lugar: _lugarExpedicion,
            onCambio: _sincronizar,
            canal: _canal!,
            onCanal: (c) {
              _canal = c;
              _sincronizar('canal');
            },
            onLugar: (l) {
              _lugarExpedicion = l;
              _sincronizar('lugar');
            },
            onFecha: (f) {
              _fechaNacimiento = f;
              _sincronizar('fecha');
            },
          ),
          const SizedBox(height: Espacio.s5),
          Boton(
            texto: TextosIdentidad.continuar,
            icono: Icons.arrow_forward,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: _continuar,
          ),
          const SizedBox(height: Espacio.s3),
          Text(
            TextosIdentidad.registroPie,
            textAlign: TextAlign.center,
            style: Tipo.ayuda.copyWith(color: t.text3),
          ),
        ],
      ),
    );
  }
}
