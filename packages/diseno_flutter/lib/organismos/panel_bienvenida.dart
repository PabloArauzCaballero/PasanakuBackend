import 'package:flutter/material.dart';

import '../atomos/hundido_al_tocar.dart';
import '../atomos/iconos_de_marca.dart';
import '../atomos/monto.dart';
import '../moleculas/tarjeta.dart';
import '../tokens/tokens.dart';

/// La portada de una cuenta nueva: la bienvenida y los pasos siguientes (D-9, ADR-053).
///
/// [bono] en `null` significa «todavía no se sabe cuánto es», y entonces la frase del
/// bono no se muestra. Antes se le pasaba `'—'` para decir eso mismo y el panel
/// reventaba: `Monto` exige un importe del contrato `Dinero` y un guión no lo es. Una
/// pantalla que no puede decir la cifra tiene que callarse, no romperse.
///
/// La bienvenida es una tarjeta verde como la del saldo (el mismo objeto protagonista de la
/// app) y debajo, una lista de pasos numerados: un paso hecho se marca con tilde, no se tacha.
class PanelBienvenida extends StatelessWidget {
  const PanelBienvenida({
    super.key,
    required this.nombre,
    required this.bono,
    required this.moneda,
    required this.pasos,
  });
  final String nombre;
  final String? bono;
  final String moneda;
  final List<({String texto, bool hecho, VoidCallback onTap})> pasos;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        DecoratedBox(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(Radios.xxl),
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [t.heroInicio, t.heroFin],
            ),
            boxShadow: Elevacion.heroe(t),
          ),
          child: Padding(
            padding: const EdgeInsets.all(Espacio.s5),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Bienvenida, $nombre',
                  style: Tipo.tituloPantalla.copyWith(color: t.heroTexto),
                ),
                if (bono != null) ...[
                  const SizedBox(height: Espacio.s2),
                  Wrap(
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      Text(
                        'Te acreditamos ',
                        style: Tipo.cuerpo.copyWith(color: t.heroTexto2),
                      ),
                      Monto(
                        monto: bono!,
                        moneda: moneda,
                        etiqueta: 'Bono de bienvenida',
                        estilo: Tipo.cuerpoFuerte.copyWith(color: t.heroTexto),
                      ),
                      Text(
                        ' de bienvenida.',
                        style: Tipo.cuerpo.copyWith(color: t.heroTexto2),
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: Espacio.s5),
        Tarjeta(
          relleno: false,
          child: ClipRRect(
            borderRadius: BorderRadius.circular(Radios.lg),
            child: Column(
              children: [
                for (final (i, p) in pasos.indexed) ...[
                  if (i > 0) Divider(height: Borde.fino, color: t.border),
                  _Paso(numero: i + 1, paso: p, tokens: t),
                ],
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _Paso extends StatelessWidget {
  const _Paso({required this.numero, required this.paso, required this.tokens});

  final int numero;
  final ({String texto, bool hecho, VoidCallback onTap}) paso;
  final Tokens tokens;

  @override
  Widget build(BuildContext context) {
    final t = tokens;
    return Semantics(
      button: !paso.hecho,
      label: '${paso.texto}${paso.hecho ? ', hecho' : ''}',
      child: ExcludeSemantics(
        child: HundidoAlTocar(
          activo: !paso.hecho,
          child: InkWell(
            onTap: paso.hecho ? null : paso.onTap,
            child: ConstrainedBox(
              constraints: const BoxConstraints(minHeight: Tactil.minimo),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: Espacio.s4,
                  vertical: Espacio.s3,
                ),
                child: Row(
                  children: [
                    DecoratedBox(
                      decoration: BoxDecoration(
                        color: paso.hecho ? t.okBg : t.brandBg,
                        shape: BoxShape.circle,
                      ),
                      child: SizedBox.square(
                        dimension: Tactil.minimo - Espacio.s2,
                        child: Center(
                          child: paso.hecho
                              ? Icon(
                                  IconosDeMarca.listo,
                                  size: Espacio.s5,
                                  color: t.okTexto,
                                )
                              : Text(
                                  '$numero',
                                  style: Tipo.cuerpoFuerte.copyWith(
                                    color: t.brandTexto,
                                  ),
                                ),
                        ),
                      ),
                    ),
                    const SizedBox(width: Espacio.s3),
                    Expanded(
                      child: Text(
                        paso.texto,
                        style: Tipo.cuerpo.copyWith(color: t.text),
                      ),
                    ),
                    if (!paso.hecho) Icon(IconosDeMarca.seguir, color: t.text3),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
