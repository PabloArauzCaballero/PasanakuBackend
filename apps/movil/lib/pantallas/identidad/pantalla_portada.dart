import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/atomos/palabra_de_marca.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:aportaya_diseno/moviles/fondo_de_marca.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'garantias_de_portada.dart';
import 'lo_que_necesitas.dart';
import 'textos.dart';

/// **La bienvenida**: lo primero que ve alguien que abre AportaYa, tal como la
/// maqueta (`identidad/bienvenida`).
///
/// Centrada, con aire y con una sola idea: la marca, qué es esto en dos líneas, y las
/// dos puertas abajo. «Crear mi cuenta» lleva directo al alta: quien ya decidió no
/// tiene que pasar cuatro láminas para empezar. El tour queda en «¿Cómo funciona?»,
/// para quien quiere entender antes. Quien ya tiene cuenta entra desde su puerta.
///
/// Debajo de las garantías dice qué hay que tener a mano (`LoQueNecesitas`): el
/// carnet, el celular y el correo, y buena luz. Así nadie descubre a mitad del alta
/// que le falta el carnet.
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
    return FondoDeMarca(
      hijo: Scaffold(
        backgroundColor: Colors.transparent,
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: Espacio.s5),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(
                  child: Center(
                    child: SingleChildScrollView(
                      child: AparicionEscalonada(
                        escalon: Movimiento.escalonDePortada,
                        duracionPorHijo: Movimiento.entradaDePortada,
                        children: [
                          Center(
                            key: const ValueKey('portada-marca'),
                            child: _Lockup(t: t),
                          ),
                          Column(
                            key: const ValueKey('portada-titular'),
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
                              Center(
                                child: ConstrainedBox(
                                  constraints: const BoxConstraints(
                                    maxWidth: 320,
                                  ),
                                  child: Text(
                                    TextosIdentidad.bienvenidaTexto,
                                    textAlign: TextAlign.center,
                                    style: Tipo.cuerpo.copyWith(color: t.text2),
                                  ),
                                ),
                              ),
                              const SizedBox(height: Espacio.s4),
                            ],
                          ),
                          const KeyedSubtree(
                            key: ValueKey('portada-garantias'),
                            child: Column(
                              children: [
                                GarantiasDePortada(),
                                SizedBox(height: Espacio.s4),
                                LoQueNecesitas(),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                AparicionEscalonada(
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
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// La marca sobre su sello de acción y, debajo, la palabra. Mantiene la forma del
/// lockup original sin sumar el verde de marca como segundo acento de la pantalla.
class _Lockup extends StatelessWidget {
  const _Lockup({required this.t});
  final Tokens t;

  @override
  Widget build(BuildContext context) => Column(
    mainAxisSize: MainAxisSize.min,
    children: [
      Container(
        width: 84,
        height: 84,
        decoration: BoxDecoration(
          color: t.accent,
          borderRadius: BorderRadius.circular(Radios.xl),
          boxShadow: [t.sombra3],
        ),
        child: Center(child: Marca(tamano: 54, colorDelTrazo: t.accentInk)),
      ),
      const SizedBox(height: Espacio.s4),
      PalabraDeMarca(ancho: 148, color: t.text),
    ],
  );
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
          onPressed: () => context.push('/registro'),
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
