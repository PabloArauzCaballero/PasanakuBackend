import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';

/// **La bienvenida**: lo primero que ve alguien que abre AportaYa, tal como la
/// maqueta (`identidad/bienvenida`).
///
/// Centrada, con aire y con una sola idea: la marca en su sello verde, qué es esto en
/// dos líneas, y las dos salidas abajo. El tour no vive acá —se abre con «Crear mi
/// cuenta», que es cuando alguien quiere saber más—; quien ya tiene cuenta entra sin
/// pasar por cuatro láminas que no necesita.
class PantallaDePortada extends StatelessWidget {
  const PantallaDePortada({super.key});

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Scaffold(
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
                        Center(child: _Sello(t: t)),
                        const SizedBox(height: Espacio.s5),
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
                      ],
                    ),
                  ),
                ),
              ),
              Padding(
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
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// La marca sobre su sello verde sólido, con los trazos claros: el `#ay-marca` de la
/// maqueta, que es el isotipo pensado para ir sobre relleno de marca.
class _Sello extends StatelessWidget {
  const _Sello({required this.t});
  final Tokens t;

  @override
  Widget build(BuildContext context) => Container(
    width: 72,
    height: 72,
    decoration: BoxDecoration(
      color: t.verdeSolido,
      borderRadius: BorderRadius.circular(Radios.lg + Espacio.s1),
      boxShadow: [t.sombra2],
    ),
    child: Center(
      child: Marca(tamano: 48, colorDelTrazo: t.sobreVerdeSolido),
    ),
  );
}
