import 'package:aportaya_diseno/organismos/panel_bienvenida.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_alta.dart';
import 'textos.dart';

/// D-9 de la maqueta: bono de bienvenida y estado de cuenta nueva, al cerrar el
/// alta de seis pasos. El monto del bono es catálogo del backend (no se inventa un
/// número acá); mientras no haya cliente de identidad, se declara el hueco y se
/// muestra un texto neutro en su lugar.
class PantallaDeBienvenida extends StatelessWidget {
  const PantallaDeBienvenida({super.key, this.datos = const DatosPersonales()});

  final DatosPersonales datos;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Scaffold(
      body: SafeArea(
        // Arriba, no centrado: centrado dejaba media pantalla vacía sobre el panel y
        // los pasos lejos del pulgar se leían como un aviso, no como el próximo paso.
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // El título de la pantalla, sin barra de fábrica: una línea arriba.
            Padding(
              padding: const EdgeInsets.all(Espacio.s4),
              child: Text(
                TextosIdentidad.tituloBienvenida,
                style: Tipo.cuerpoChico.copyWith(color: t.text2),
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(Espacio.s4),
                child: Align(
                  alignment: Alignment.topCenter,
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 480),
                    child: PanelBienvenida(
                      nombre: datos.nombres.isEmpty
                          ? TextosIdentidad.bienvenidaSinNombre
                          : datos.nombres,
                      // Hueco: el monto del bono lo define el catálogo del backend
                      // (`servicios/identidad` o promociones); no está en la bóveda de
                      // pantallas todavía. Va `null` —la frase del bono no se muestra— en
                      // vez de un `'—'`, que no es un importe del contrato `Dinero` y hacía
                      // que la pantalla entera reventara al construirse.
                      bono: null,
                      moneda: 'BOB',
                      pasos: [
                        (
                          texto: TextosIdentidad.bienvenidaIrABilletera,
                          hecho: false,
                          onTap: () => context.go('/billetera/inicio'),
                        ),
                        (
                          texto: TextosIdentidad.bienvenidaCompletarPerfil,
                          hecho: false,
                          onTap: () => context.push('/identidad/perfil'),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
