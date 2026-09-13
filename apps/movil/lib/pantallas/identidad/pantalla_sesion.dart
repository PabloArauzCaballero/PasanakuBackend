import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_contrasena.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_sesion.dart';
import 'dominio/validaciones.dart';
import 'textos.dart';

/// CU-04, paso 1: teléfono y contraseña. El servidor decide si además pide MFA —
/// esta pantalla nunca asume que no hace falta (invariante 7).
class PantallaDeSesion extends ConsumerStatefulWidget {
  const PantallaDeSesion({super.key});

  @override
  ConsumerState<PantallaDeSesion> createState() => _PantallaDeSesionState();
}

class _PantallaDeSesionState extends ConsumerState<PantallaDeSesion> {
  final _telefono = TextEditingController();
  final _contrasena = TextEditingController();

  @override
  void dispose() {
    _telefono.dispose();
    _contrasena.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final valido =
        errorTelefono(_telefono.text) == null && _contrasena.text.isNotEmpty;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const CabeceraDeSeccion(titulo: TextosIdentidad.tituloSesion),
              const SizedBox(height: Espacio.s4),
              Campo(
                etiqueta: TextosIdentidad.telefono,
                controlador: _telefono,
                tipoDeTeclado: TextInputType.phone,
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: Espacio.s3),
              CampoContrasena(
                etiqueta: TextosIdentidad.contrasena,
                controlador: _contrasena,
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: Espacio.s5),
              Boton(
                texto: TextosIdentidad.ingresar,
                variante: BotonVariante.primario,
                expandido: true,
                onPressed: valido
                    ? () {
                        final n = ref.read(sesionIdentidadProvider.notifier);
                        n.actualizarCredenciales(
                          telefono: _telefono.text,
                          contrasena: _contrasena.text,
                        );
                        n.enviarCredenciales();
                        context.go('/identidad/mfa');
                      }
                    : null,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
