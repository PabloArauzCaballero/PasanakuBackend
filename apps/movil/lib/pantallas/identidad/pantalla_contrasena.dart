import 'package:aportaya_diseno/atomos/campo_contrasena.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/estado_cuenta.dart';
import 'dominio/validaciones.dart';
import 'textos.dart';

/// CU-09 — cambiar credenciales.
class PantallaDeContrasena extends ConsumerStatefulWidget {
  const PantallaDeContrasena({super.key});

  @override
  ConsumerState<PantallaDeContrasena> createState() =>
      _PantallaDeContrasenaState();
}

class _PantallaDeContrasenaState extends ConsumerState<PantallaDeContrasena> {
  final _actual = TextEditingController();
  final _nueva = TextEditingController();

  @override
  void dispose() {
    _actual.dispose();
    _nueva.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final valido =
        _actual.text.isNotEmpty && errorContrasena(_nueva.text) == null;
    return Scaffold(
      appBar: AppBar(title: const Text(TextosIdentidad.tituloContrasenaNueva)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              CampoContrasena(
                etiqueta: TextosIdentidad.contrasenaActual,
                controlador: _actual,
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: Espacio.s3),
              CampoContrasena(
                etiqueta: TextosIdentidad.contrasenaNueva,
                controlador: _nueva,
                fortaleza: fortalezaContrasena(_nueva.text),
                error: _nueva.text.isEmpty
                    ? null
                    : errorContrasena(_nueva.text),
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: Espacio.s5),
              Boton(
                texto: TextosIdentidad.guardar,
                variante: BotonVariante.primario,
                expandido: true,
                onPressed: valido
                    ? () => ref
                          .read(contrasenaProvider.notifier)
                          .actualizar(actual: _actual.text, nueva: _nueva.text)
                    : null,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
