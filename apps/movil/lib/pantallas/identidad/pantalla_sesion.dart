import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_contrasena.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
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

  /// El formato mínimo que el contrato acepta: la contraseña no baja de ocho. Es
  /// ayuda para no gastar un viaje al servidor, no la decisión — la de verdad la
  /// toma el servidor.
  bool get _valido =>
      errorTelefono(_telefono.text) == null && _contrasena.text.length >= 8;

  Future<void> _ingresar() async {
    final n = ref.read(sesionIdentidadProvider.notifier);
    n.actualizarCredenciales(
      telefono: _telefono.text.trim(),
      contrasena: _contrasena.text,
    );
    final avanzo = await n.enviarCredenciales();
    if (!avanzo || !mounted) return;
    // A dónde ir lo dijo el servidor, no esta pantalla.
    final paso = ref.read(sesionIdentidadProvider).paso;
    if (paso == PasoSesion.mfa) {
      context.push('/identidad/mfa');
    } else {
      context.go('/billetera/inicio');
    }
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(sesionIdentidadProvider);
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: ListView(
            children: [
              const CabeceraDeSeccion(titulo: TextosIdentidad.tituloSesion),
              const SizedBox(height: Espacio.s4),
              if (estado.error != null) ...[
                Alerta(tono: Tono.error, titulo: estado.error!),
                const SizedBox(height: Espacio.s4),
              ],
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
                cargando: estado.enviando,
                onPressed: _valido ? _ingresar : null,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
