import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'banda_de_marca.dart';
import 'garantias_de_portada.dart';
import 'textos.dart';

/// **La bienvenida**: lo primero que ve alguien que abre AportaYa, tal como la
/// maqueta (`identidad/bienvenida`).
///
/// Centrada, con aire y con una sola idea: la marca, qué es esto en dos líneas, y las
/// dos puertas abajo. «Crear mi cuenta» lleva al tour antes del alta; «¿Cómo
/// funciona?» permite abrirlo sin iniciar el registro. Quien ya tiene cuenta entra
/// desde su puerta directa, sin pasar por cuatro láminas que no necesita.
///
/// Sobre la maqueta suma dos cosas, y las dos por el mismo motivo —tres elementos
/// sueltos sobre un papel liso se ven vacíos, no tranquilos—: el aura de marca de
/// fondo y la fila de garantías, que dice en tres palabras lo que el tour cuenta en
/// cuatro láminas, para quien no lo va a mirar.
class PantallaDePortada extends StatelessWidget {
  const PantallaDePortada({super.key});

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    const lateral = EdgeInsets.symmetric(horizontal: Espacio.s5);
    return Scaffold(
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Expanded(
            // Arriba, no centrado: la banda de marca tiene que tocar el borde superior.
            child: Align(
              alignment: Alignment.topCenter,
              child: SingleChildScrollView(
                child: AparicionEscalonada(
                  escalon: Movimiento.escalonDePortada,
                  duracionPorHijo: Movimiento.entradaDePortada,
                  children: [
                    // La banda de marca va a sangre, detrás de la barra de estado (ADR-053).
                    KeyedSubtree(
                      key: const ValueKey('portada-marca'),
                      child: BandaDeMarca(t: t),
                    ),
                    Padding(
                      key: const ValueKey('portada-titular'),
                      padding: lateral,
                      child: Column(
                        children: [
                          const SizedBox(height: Espacio.s4),
                          Semantics(
                            header: true,
                            child: Text(
                              TextosIdentidad.bienvenidaTitulo,
                              textAlign: TextAlign.center,
                              style: Tipo.titulo1.copyWith(color: t.text),
                            ),
                          ),
                          const SizedBox(height: Espacio.s3),
                          ConstrainedBox(
                            constraints: const BoxConstraints(maxWidth: 320),
                            child: Text(
                              TextosIdentidad.bienvenidaTexto,
                              textAlign: TextAlign.center,
                              style: Tipo.cuerpo.copyWith(color: t.text2),
                            ),
                          ),
                          const SizedBox(height: Espacio.s4),
                        ],
                      ),
                    ),
                    const Padding(
                      key: ValueKey('portada-garantias'),
                      padding: lateral,
                      child: GarantiasDePortada(),
                    ),
                  ],
                ),
              ),
            ),
          ),
          SafeArea(
            top: false,
            child: Padding(
              padding: lateral,
              child: AparicionEscalonada(
                escalon: Duration.zero,
                duracionPorHijo: Movimiento.entradaDePortada,
                demoraInicial: Movimiento.escalonDePortada * 3,
                children: const [
                  KeyedSubtree(
                    key: ValueKey('portada-botones'),
                    child: _Salidas(),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Salidas extends StatelessWidget {
  const _Salidas();

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: Espacio.s4),
    child: Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Boton(
          texto: TextosIdentidad.portadaCrearCuenta,
          variante: BotonVariante.primario,
          expandido: true,
          onPressed: () => context.push('/tour'),
        ),
        const SizedBox(height: Espacio.s2),
        Boton(
          texto: TextosIdentidad.portadaYaTengoCuenta,
          variante: BotonVariante.fantasma,
          expandido: true,
          onPressed: () => context.push('/ingreso'),
        ),
        const SizedBox(height: Espacio.s1),
        TextButton(
          onPressed: () => context.push('/tour'),
          child: const Text(TextosIdentidad.portadaComoFunciona),
        ),
      ],
    ),
  );
}
