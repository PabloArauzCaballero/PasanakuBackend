import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'laminas_del_tour.dart';
import 'textos.dart';

/// **Lo primero que ve alguien que abre AportaYa**: el tour de cuatro láminas que
/// declara D-8 de la maqueta, saltable.
///
/// Antes la app arrancaba dentro de la billetera, con un saldo ya puesto, y quien la
/// abría por primera vez caía en el tablero de una cuenta que no era suya. Después
/// fue una sola pantalla que decía todo junto. Ahora es lo que la maqueta pedía: una
/// idea por lámina, pasando con el dedo.
///
/// **Las dos acciones están siempre abajo, fuera del carrusel.** Quien ya sabe qué es
/// AportaYa no tiene que deslizar cuatro veces para poder entrar: el tour se cuenta
/// para quien lo quiere, no se cobra como peaje.
class PantallaDePortada extends StatefulWidget {
  const PantallaDePortada({super.key});

  @override
  State<PantallaDePortada> createState() => _PantallaDePortadaState();
}

class _PantallaDePortadaState extends State<PantallaDePortada> {
  final _paginas = PageController();
  int _actual = 0;

  @override
  void dispose() {
    _paginas.dispose();
    super.dispose();
  }

  void _saltar(int total) => _paginas.animateToPage(
    total - 1,
    duration: const Duration(milliseconds: 320),
    curve: Curves.easeOutCubic,
  );

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    const laminas = laminasDelTour;
    final enLaUltima = _actual == laminas.length - 1;
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            _Encabezado(
              t: t,
              mostrarSaltar: !enLaUltima,
              onSaltar: () => _saltar(laminas.length),
            ),
            Expanded(
              child: PageView(
                controller: _paginas,
                onPageChanged: (i) => setState(() => _actual = i),
                children: laminas,
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: Espacio.s4),
              child: BarraDePuntos(total: laminas.length, actual: _actual),
            ),
            const _Salidas(),
          ],
        ),
      ),
    );
  }
}

class _Encabezado extends StatelessWidget {
  const _Encabezado({
    required this.t,
    required this.mostrarSaltar,
    required this.onSaltar,
  });

  final Tokens t;
  final bool mostrarSaltar;
  final VoidCallback onSaltar;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(Espacio.s4, Espacio.s3, Espacio.s2, 0),
    child: Row(
      children: [
        const Marca(tamano: 30),
        const SizedBox(width: Espacio.s2),
        Text.rich(
          TextSpan(
            children: [
              TextSpan(text: 'Aporta', style: TextStyle(color: t.text)),
              TextSpan(text: 'Ya', style: TextStyle(color: t.accentTexto)),
            ],
          ),
          style: Tipo.titulo2,
        ),
        const Spacer(),
        // Desaparece en la última lámina, no se deshabilita: un botón apagado en una
        // esquina es ruido que nadie puede usar.
        if (mostrarSaltar)
          TextButton(
            onPressed: onSaltar,
            child: const Text(TextosIdentidad.portadaSaltar),
          ),
      ],
    ),
  );
}

/// Las dos salidas, ancladas abajo: se ven sin desplazarse y sin terminar el tour,
/// que es lo que hay que poder hacer en la primera pantalla de una app.
class _Salidas extends StatelessWidget {
  const _Salidas();

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
            onPressed: () => context.push('/registro'),
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
