import 'package:aportaya_diseno/atomos/hundido_al_tocar.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/capturas_del_expediente.dart';
import '../textos_de_captura.dart';

/// Las cinco caras en una fila: dónde estás, qué ya está y qué falta, de un vistazo.
///
/// Reemplaza a «Anterior» y «Siguiente» sueltos debajo del carrusel, que obligaban a
/// recorrer foto por foto para saber cuál faltaba. Cada cara es tocable y lleva al
/// cuadro; la actual tiene anillo, la lista un tilde verde, la pendiente queda tenue.
/// Lo que está y lo que falta no depende solo del color: cambia el ícono.
class RielDeCapturas extends StatelessWidget {
  const RielDeCapturas({
    super.key,
    required this.actual,
    required this.listas,
    required this.onElegir,
  });

  final CaraDelCarril actual;
  final Set<CaraDelCarril> listas;
  final ValueChanged<CaraDelCarril> onElegir;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        for (final cara in CapturasDelExpediente.orden)
          Expanded(
            child: _Cara(
              cara: cara,
              actual: cara == actual,
              lista: listas.contains(cara),
              onTap: () => onElegir(cara),
            ),
          ),
      ],
    );
  }
}

IconData iconoDeCara(CaraDelCarril cara) => switch (cara) {
  CaraDelCarril.anverso => Icons.badge_outlined,
  CaraDelCarril.reverso => Icons.credit_card_outlined,
  CaraDelCarril.selfie => Icons.face_outlined,
  CaraDelCarril.perfilIzquierdo => Icons.turn_left,
  CaraDelCarril.perfilDerecho => Icons.turn_right,
};

class _Cara extends StatelessWidget {
  const _Cara({
    required this.cara,
    required this.actual,
    required this.lista,
    required this.onTap,
  });

  final CaraDelCarril cara;
  final bool actual;
  final bool lista;
  final VoidCallback onTap;

  static const _lado = Tactil.minimo - Espacio.s1;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final corto = TextosDeCaptura.corto(cara.valorApi);
    final fondo = lista ? t.brand : (actual ? t.brandBg : t.surface);
    final frente = lista
        ? t.sobreVerdeSolido
        : (actual ? t.brandTexto : t.text3);
    return Semantics(
      button: true,
      selected: actual,
      label: TextosDeCaptura.irA(corto, lista),
      excludeSemantics: true,
      child: HundidoAlTocar(
        child: InkResponse(
          onTap: onTap,
          radius: Tactil.minimo / 2 + Espacio.s2,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: Espacio.s1),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AnimatedContainer(
                  duration: Movimiento.entrada,
                  curve: Movimiento.llega,
                  width: _lado,
                  height: _lado,
                  decoration: BoxDecoration(
                    color: fondo,
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: actual ? t.brand : t.border,
                      width: actual ? Borde.foco - Borde.fino : Borde.fino,
                    ),
                    boxShadow: actual || lista ? [t.sombra1, t.sombra2] : null,
                  ),
                  child: AnimatedSwitcher(
                    duration: Movimiento.entrada,
                    switchInCurve: Movimiento.llega,
                    transitionBuilder: (hijo, a) =>
                        FadeTransition(opacity: a, child: hijo),
                    child: Icon(
                      lista ? Icons.check_rounded : iconoDeCara(cara),
                      key: ValueKey(lista),
                      size: Espacio.s5 - Espacio.s1,
                      color: frente,
                    ),
                  ),
                ),
                const SizedBox(height: Espacio.s1 + Borde.desfase),
                Text(
                  corto,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Tipo.ayuda.copyWith(
                    color: actual ? t.text : t.text3,
                    fontWeight: actual ? FontWeight.w600 : null,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
