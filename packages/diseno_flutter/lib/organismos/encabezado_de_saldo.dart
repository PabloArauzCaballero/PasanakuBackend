import 'dart:ui' show ImageFilter;

import 'package:flutter/material.dart';

import '../atomos/iconos_de_marca.dart';
import '../atomos/monto.dart';
import '../atomos/monto_que_sube.dart';
import '../atomos/movimiento.dart';
import '../tokens/tokens.dart';

part 'encabezado_de_saldo_piezas.dart';

/// El saldo, como **tarjeta héroe** verde de la pantalla de inicio (ADR-053).
///
/// Hasta el rediseño bancario era texto suelto en tinta: se le había sacado la tarjeta
/// para que la superficie señalara solo lo que vence. La persona usuaria pidió el
/// lenguaje de sus apps bancarias, donde el saldo es el objeto protagonista. Lo que vence
/// sigue teniendo su propia tarjeta, con el único naranja de la pantalla.
///
/// El saldo se puede ocultar (en la calle, alguien mira por encima del hombro): el ojo
/// lo tapa con puntos y el lector anuncia «Saldo oculto».
class EncabezadoDeSaldo extends StatefulWidget {
  const EncabezadoDeSaldo({
    super.key,
    required this.etiqueta,
    required this.monto,
    required this.moneda,
    this.etiquetaHablada,
    this.cifras = const [],
    this.distintivo,
    this.acciones,
  });

  final String etiqueta;

  /// Lo que anuncia un lector de pantalla cuando conviene que diga más que lo que se
  /// ve («Saldo disponible» entero, aunque en pantalla diga «Disponible»).
  final String? etiquetaHablada;
  final String monto;
  final String moneda;

  /// Cifras secundarias, cada una en su chip: «Retenido Bs 150,00».
  final List<({String etiqueta, String monto})> cifras;

  /// La garantía que protege el saldo, entera y visible, con un escudo debajo de las
  /// cifras («En custodia, separado del dinero de la empresa»). No se acorta a un chip:
  /// es la frase que sostiene la confianza, y la tiene que poder leer cualquiera.
  final String? distintivo;

  /// Va debajo de la tarjeta (las acciones circulares viven sobre el fondo).
  final Widget? acciones;

  @override
  State<EncabezadoDeSaldo> createState() => _EncabezadoDeSaldoState();
}

class _EncabezadoDeSaldoState extends State<EncabezadoDeSaldo> {
  bool _oculto = false;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final estiloCifra = Tipo.cifraSaldo.copyWith(color: t.heroTexto);
    final tarjeta = DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(Radios.xxl),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [t.heroInicio, t.heroFin],
        ),
        boxShadow: Elevacion.heroe(t),
      ),
      child: DecoratedBox(
        // El brillo de arriba a la derecha: la luz que hace que el verde no sea plano.
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(Radios.xxl),
          gradient: RadialGradient(
            center: Alignment.topRight,
            radius: 1.1,
            colors: [t.heroBrillo, t.heroBrillo.withValues(alpha: 0)],
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(
            Espacio.s5,
            Espacio.s4,
            Espacio.s3,
            Espacio.s5,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: ExcludeSemantics(
                      child: Text(
                        widget.etiqueta,
                        style: Tipo.cuerpoChico.copyWith(color: t.heroTexto2),
                      ),
                    ),
                  ),
                  _BotonDeOcultar(
                    oculto: _oculto,
                    alTocar: () => setState(() => _oculto = !_oculto),
                  ),
                ],
              ),
              Padding(
                padding: const EdgeInsets.only(right: Espacio.s3),
                child: AnimatedSwitcher(
                  duration: duracion(context, Movimiento.ocultarSaldo),
                  transitionBuilder: _desenfoqueYFundido,
                  layoutBuilder: _alineadasALaIzquierda,
                  child: _oculto
                      ? Semantics(
                          key: const ValueKey('oculto'),
                          label: 'Saldo oculto',
                          child: ExcludeSemantics(
                            child: Text('Bs ••••••', style: estiloCifra),
                          ),
                        )
                      : FittedBox(
                          // Partir `9.999.999,99` entre grupos cambia cómo se lee el
                          // saldo: un importe largo se achica hasta caber en una línea.
                          key: const ValueKey('visible'),
                          fit: BoxFit.scaleDown,
                          alignment: Alignment.centerLeft,
                          child: MontoQueSube(
                            monto: widget.monto,
                            moneda: widget.moneda,
                            etiqueta: widget.etiquetaHablada ?? widget.etiqueta,
                            estilo: estiloCifra,
                          ),
                        ),
                ),
              ),
              if (widget.cifras.isNotEmpty) ...[
                const SizedBox(height: Espacio.s4),
                Wrap(
                  spacing: Espacio.s2,
                  runSpacing: Espacio.s2,
                  children: [
                    for (final c in widget.cifras)
                      _Chip(
                        hijo: _oculto
                            ? Semantics(
                                label: '${c.etiqueta} oculto',
                                child: ExcludeSemantics(
                                  child: Text('${c.etiqueta} ••••'),
                                ),
                              )
                            // Wrap y no Row: con texto grande, la cifra baja de
                            // renglón en vez de salirse del chip.
                            : Wrap(
                                crossAxisAlignment: WrapCrossAlignment.center,
                                children: [
                                  // [Monto] ya anuncia «Retenido: Bs 150,00».
                                  ExcludeSemantics(
                                    child: Text('${c.etiqueta} '),
                                  ),
                                  Monto(
                                    monto: c.monto,
                                    moneda: widget.moneda,
                                    etiqueta: c.etiqueta,
                                    estilo: Tipo.cuerpoChico.copyWith(
                                      color: t.heroTexto,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                ],
                              ),
                      ),
                  ],
                ),
              ],
              if (widget.distintivo != null) ...[
                const SizedBox(height: Espacio.s4),
                _Garantia(texto: widget.distintivo!),
              ],
            ],
          ),
        ),
      ),
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        tarjeta,
        if (widget.acciones != null) ...[
          const SizedBox(height: Espacio.s5),
          widget.acciones!,
        ],
      ],
    );
  }
}
