import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/atomos/rueda.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'portada_promesa.dart';
import 'textos.dart';

/// **Lo primero que ve alguien que abre AportaYa.**
///
/// Antes la app arrancaba dentro de la billetera, con un saldo ya puesto: quien la
/// abría por primera vez caía en el tablero de una cuenta que no era suya, sin saber
/// qué era esto ni cómo entrar. Esta pantalla es la puerta: dice qué es un pasanaku
/// acá, por qué las cuentas quedan claras, y ofrece las dos únicas salidas posibles
/// —crear cuenta o ingresar—.
///
/// La rueda es el héroe, no un ícono decorativo: es el objeto del que habla el
/// producto, y quien ya hizo un pasanaku la reconoce antes de leer una palabra.
class PantallaDePortada extends StatelessWidget {
  const PantallaDePortada({super.key});

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(
                  Espacio.s4,
                  Espacio.s3,
                  Espacio.s4,
                  Espacio.s3,
                ),
                children: [
                  Row(
                    children: [
                      const Marca(tamano: 34),
                      const SizedBox(width: Espacio.s2),
                      Text.rich(
                        TextSpan(
                          children: [
                            TextSpan(
                              text: 'Aporta',
                              style: TextStyle(color: t.text),
                            ),
                            TextSpan(
                              text: 'Ya',
                              style: TextStyle(color: t.accentTexto),
                            ),
                          ],
                        ),
                        style: Tipo.titulo2,
                      ),
                    ],
                  ),
                  const SizedBox(height: Espacio.s5),
                  // El tamaño lo manda el alto del teléfono, no el gusto: la portada
                  // tiene que entrar entera sin desplazarse, y con la rueda más grande
                  // la tercera promesa —la de la custodia, la que responde «¿y mi
                  // plata?»— quedaba cortada a media frase contra la barra de
                  // acciones. `portada_test.dart` lo mide en un teléfono chico.
                  const Center(
                    child: Rueda(
                      turnos: 10,
                      cobrados: 4,
                      miTurno: 6,
                      turnoActual: 4,
                      diametro: 104,
                      etiqueta: TextosIdentidad.portadaRuedaTitulo,
                    ),
                  ),
                  const SizedBox(height: Espacio.s5),
                  Semantics(
                    header: true,
                    child: Text(
                      TextosIdentidad.portadaTitular,
                      style: Tipo.titulo1.copyWith(color: t.text),
                    ),
                  ),
                  const SizedBox(height: Espacio.s3),
                  Text(
                    TextosIdentidad.portadaBajada,
                    style: Tipo.cuerpo.copyWith(color: t.text2),
                  ),
                  const SizedBox(height: Espacio.s5),
                  const PortadaPromesas(),
                ],
              ),
            ),
            _Salidas(),
          ],
        ),
      ),
    );
  }
}

/// Las dos salidas, ancladas abajo: se ven sin desplazarse, que es lo que hay que
/// poder hacer en la primera pantalla de una app.
class _Salidas extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Container(
      decoration: BoxDecoration(
        color: t.surface,
        border: Border(top: BorderSide(color: t.border, width: Borde.fino)),
      ),
      padding: const EdgeInsets.fromLTRB(
        Espacio.s4,
        Espacio.s3,
        Espacio.s4,
        Espacio.s3,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Boton(
            texto: TextosIdentidad.portadaCrearCuenta,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => context.push('/identidad/registro'),
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
}
