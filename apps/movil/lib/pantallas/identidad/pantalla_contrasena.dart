import 'package:aportaya_diseno/atomos/campo_contrasena.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/intro_de_pantalla.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';

import 'dominio/estado_cuenta.dart';
import 'dominio/validaciones.dart';
import 'textos.dart';
import 'textos_de_tramites.dart';

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
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      body: PantallaConCabecera(
        titulo: TextosIdentidad.tituloContrasenaNueva,
        cuerpo: SafeArea(
          top: false,
          // Sin desplazamiento propio, la cabecera no se compactaría y el botón
          // quedaría bajo el pliegue en una pantalla chica.
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(Espacio.s4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const IntroDePantalla(
                  icono: IconosDeMarca.contrasena,
                  titulo: TextosDeTramites.contrasenaTitulo,
                  explicacion: TextosDeTramites.contrasenaExplicacion,
                ),
                Tarjeta(
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
                    ],
                  ),
                ),
                const SizedBox(height: Espacio.s5),
                Boton(
                  texto: TextosDeTramites.contrasenaGuardar,
                  motivoDeBloqueo: valido
                      ? null
                      : TextosDeTramites.contrasenaBloqueo,
                  variante: BotonVariante.primario,
                  expandido: true,
                  onPressed: valido
                      ? () => ref
                            .read(contrasenaProvider.notifier)
                            .actualizar(
                              actual: _actual.text,
                              nueva: _nueva.text,
                            )
                      : null,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
