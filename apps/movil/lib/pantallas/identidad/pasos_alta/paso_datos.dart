import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/estado_alta.dart';
import '../dominio/validaciones.dart';
import '../textos.dart';

/// Paso 1 de 8 — datos personales (CU-01). Un organismo puro: lee y escribe en el
/// notifier, no llama a la red.
class PasoDatos extends ConsumerStatefulWidget {
  const PasoDatos({super.key});

  @override
  ConsumerState<PasoDatos> createState() => _PasoDatosState();
}

class _PasoDatosState extends ConsumerState<PasoDatos> {
  late final _nombres = TextEditingController(
    text: ref.read(altaProvider).datos.nombres,
  );
  late final _apellidos = TextEditingController(
    text: ref.read(altaProvider).datos.apellidos,
  );
  late final _telefono = TextEditingController(
    text: ref.read(altaProvider).datos.telefono,
  );
  late final _documento = TextEditingController(
    text: ref.read(altaProvider).datos.numeroDocumento,
  );
  DateTime? _fechaNacimiento;

  @override
  void dispose() {
    _nombres.dispose();
    _apellidos.dispose();
    _telefono.dispose();
    _documento.dispose();
    super.dispose();
  }

  void _sincronizar() {
    ref
        .read(altaProvider.notifier)
        .actualizarDatos(
          ref
              .read(altaProvider)
              .datos
              .copiarCon(
                nombres: _nombres.text,
                apellidos: _apellidos.text,
                telefono: _telefono.text,
                numeroDocumento: _documento.text,
                fechaNacimiento: _fechaNacimiento,
              ),
        );
  }

  bool get _valido =>
      errorNombre(_nombres.text) == null &&
      errorNombre(_apellidos.text) == null &&
      errorTelefono(_telefono.text) == null &&
      errorDocumento(_documento.text) == null &&
      errorFechaNacimiento(_fechaNacimiento) == null;

  @override
  Widget build(BuildContext context) {
    _fechaNacimiento ??= ref.read(altaProvider).datos.fechaNacimiento;
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Campo(
            etiqueta: TextosIdentidad.nombres,
            controlador: _nombres,
            onChanged: (_) => setState(_sincronizar),
          ),
          const SizedBox(height: Espacio.s3),
          Campo(
            etiqueta: TextosIdentidad.apellidos,
            controlador: _apellidos,
            onChanged: (_) => setState(_sincronizar),
          ),
          const SizedBox(height: Espacio.s3),
          Campo(
            etiqueta: TextosIdentidad.telefono,
            controlador: _telefono,
            tipoDeTeclado: TextInputType.phone,
            onChanged: (_) => setState(_sincronizar),
          ),
          const SizedBox(height: Espacio.s3),
          Campo(
            etiqueta: TextosIdentidad.numeroDocumento,
            controlador: _documento,
            onChanged: (_) => setState(_sincronizar),
          ),
          const SizedBox(height: Espacio.s3),
          OutlinedButton(
            onPressed: () async {
              final elegida = await showDatePicker(
                context: context,
                initialDate: DateTime(2000),
                firstDate: DateTime(1900),
                lastDate: DateTime.now(),
              );
              if (elegida != null) {
                setState(() {
                  _fechaNacimiento = elegida;
                  _sincronizar();
                });
              }
            },
            child: Text(
              _fechaNacimiento == null
                  ? TextosIdentidad.fechaNacimiento
                  : '${TextosIdentidad.fechaNacimiento}: '
                        '${_fechaNacimiento!.day}/${_fechaNacimiento!.month}/${_fechaNacimiento!.year}',
            ),
          ),
          const SizedBox(height: Espacio.s5),
          Boton(
            texto: TextosIdentidad.continuar,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: _valido
                ? () => ref.read(altaProvider.notifier).siguiente()
                : null,
          ),
        ],
      ),
    );
  }
}
