import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';
import 'textos_bienvenida.dart';

const _anchoMaximoBienvenida = 560.0;

/// Esta ruta heredada puede abrirse sin un alta previa. No afirma que exista una
/// cuenta ni ofrece entrar a Billetera/Perfil antes de autenticar. El alta real
/// cierra en `/ingreso?alta=lista`, donde se muestra su resultado comprobado.
class PantallaDeBienvenida extends StatelessWidget {
  const PantallaDeBienvenida({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      bottom: false,
      child: Column(
        children: [
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(Espacio.s4),
              children: [
                Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(
                      maxWidth: _anchoMaximoBienvenida,
                    ),
                    child: const Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        CabeceraDeSeccion(
                          titulo: TextosIdentidad.tituloBienvenida,
                        ),
                        SizedBox(height: Espacio.s4),
                        Alerta(
                          titulo: TextosBienvenida.aviso,
                          detalle: TextosBienvenida.detalle,
                          tono: Tono.info,
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          BarraDeAccion(
            hijo: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(
                  maxWidth: _anchoMaximoBienvenida,
                ),
                child: Boton(
                  texto: TextosBienvenida.irAIngresar,
                  variante: BotonVariante.primario,
                  expandido: true,
                  onPressed: () => context.go('/ingreso'),
                ),
              ),
            ),
          ),
        ],
      ),
    ),
  );
}
