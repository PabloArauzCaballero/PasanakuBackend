import 'package:aportaya_diseno/atomos/colores_de_turno.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos_detalle_del_grupo.dart';

/// Una fila del orden de cobro: el nombre completo, si es quien mira y su gajo en la
/// ruleta (de ahí sale el color: cada persona conserva el de su gajo).
typedef PuestoDeCobro = ({String nombre, bool soyYo, int gajo});

/// El orden de cobro que salió del sorteo (2026-10-20): una fila por turno, con el
/// color de su gajo en la ruleta, el mismo con el que se lo vio salir. La fila propia dice «Tú» en rojo (pedido
/// del dueño); las demás, el nombre completo. Así cada uno ve el mismo orden, pero se
/// encuentra a sí mismo de un vistazo.
class OrdenDeCobro extends StatelessWidget {
  const OrdenDeCobro({super.key, required this.puestos});

  /// En el orden de cobro: el primero cobra primero.
  final List<PuestoDeCobro> puestos;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          TextosDetalleDelGrupo.ordenDeCobro,
          style: Tipo.titulo3.copyWith(color: t.text),
        ),
        const SizedBox(height: Espacio.s2),
        // Cada fila entra una vez, cuando sale su puesto: sube y aparece. Con la clave por
        // puesto, las que ya estaban no se vuelven a animar.
        for (var k = 0; k < puestos.length; k++)
          TweenAnimationBuilder<double>(
            key: ValueKey(k),
            tween: Tween(begin: 0, end: 1),
            duration: duracion(context, Movimiento.entrada),
            curve: Movimiento.llega,
            builder: (context, v, fila) => Opacity(
              opacity: v,
              child: Transform.translate(
                offset: Offset(0, Espacio.s3 * (1 - v)),
                child: fila,
              ),
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: Espacio.s1),
              child: Row(
                children: [
                  Container(
                    width: Espacio.s3,
                    height: Espacio.s3,
                    decoration: BoxDecoration(
                      color: ColoresDeTurno.de(puestos[k].gajo),
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: Espacio.s2),
                  Expanded(
                    child: Text(
                      TextosDetalleDelGrupo.posicion(
                        k + 1,
                        puestos[k].soyYo
                            ? TextosDetalleDelGrupo.tu
                            : puestos[k].nombre,
                      ),
                      style: puestos[k].soyYo
                          ? Tipo.titulo3.copyWith(color: t.errTexto)
                          : Tipo.cuerpo.copyWith(color: t.text),
                    ),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}
