import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/atomos/palabra_de_marca.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:aportaya_diseno/moviles/fondo_de_marca.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'garantias_de_portada.dart';
import 'textos.dart';

/// **La bienvenida**: lo primero que ve alguien que abre AportaYa, tal como la
/// maqueta (`identidad/bienvenida`).
///
/// Centrada, con aire y con una sola idea: la marca, qué es esto en dos líneas, y las
/// dos salidas abajo. El tour no vive acá —se abre con «Crear mi cuenta», que es
/// cuando alguien quiere saber más—; quien ya tiene cuenta entra sin pasar por cuatro
/// láminas que no necesita.
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
                        escalon: const Duration(milliseconds: 70),
                        duracionPorHijo: const Duration(milliseconds: 420),
                        children: [
                          Center(child: _Lockup(t: t)),
                          const SizedBox(height: Espacio.s6),
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
                              constraints: const BoxConstraints(maxWidth: 320),
                              child: Text(
                                TextosIdentidad.bienvenidaTexto,
                                textAlign: TextAlign.center,
                                style: Tipo.cuerpo.copyWith(color: t.text2),
                              ),
                            ),
                          ),
                          const SizedBox(height: Espacio.s6),
                          const GarantiasDePortada(),
                        ],
                      ),
                    ),
                  ),
                ),
                const _Salidas(),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// La marca sobre su sello verde y, debajo, la palabra. El sello con el isotipo es el
/// `#ay-marca` de la maqueta —el isotipo pensado para ir sobre relleno de marca—; la
/// palabra debajo es lo que lo vuelve un logotipo y no un cuadrado verde.
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
          color: t.verdeSolido,
          borderRadius: BorderRadius.circular(Radios.xl),
          boxShadow: [t.sombra3],
        ),
        child: Center(
          child: Marca(tamano: 54, colorDelTrazo: t.sobreVerdeSolido),
        ),
      ),
      const SizedBox(height: Espacio.s4),
      PalabraDeMarca(ancho: 148, color: t.brandInk),
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
          onPressed: () => context.push('/tour'),
        ),
        const SizedBox(height: Espacio.s2),
        Boton(
          texto: TextosIdentidad.portadaYaTengoCuenta,
          variante: BotonVariante.fantasma,
          expandido: true,
          onPressed: () => context.push('/ingreso'),
        ),
      ],
    ),
  );
}
