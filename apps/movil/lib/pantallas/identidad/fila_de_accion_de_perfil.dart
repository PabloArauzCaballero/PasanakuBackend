import 'package:aportaya_diseno/atomos/hundido_al_tocar.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Una fila de ajustes del perfil: ícono en círculo, texto y flecha. Va **dentro** de un
/// [GrupoDeFilas] (ADR-053): el borde y el fondo los pone el grupo, no cada fila.
/// Con semántica de botón y respuesta táctil.
class FilaDeAccionDePerfil extends StatelessWidget {
  const FilaDeAccionDePerfil({
    super.key,
    required this.claveSemantica,
    required this.texto,
    required this.pista,
    required this.onPressed,
    required this.icono,
    this.destructiva = false,
  });

  final Key claveSemantica;
  final String texto;
  final String pista;
  final VoidCallback onPressed;
  final IconData icono;

  /// Lo que no se deshace (dar de baja): texto e ícono en rojo y sin flecha, para que no se
  /// confunda con una fila común.
  final bool destructiva;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final color = destructiva ? t.errTexto : t.text;
    return Semantics(
      key: claveSemantica,
      button: true,
      label: texto,
      hint: pista,
      onTap: onPressed,
      child: ExcludeSemantics(
        child: HundidoAlTocar(
          child: InkWell(
            onTap: onPressed,
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
                        color: destructiva ? t.errBg : t.surface2,
                        shape: BoxShape.circle,
                      ),
                      child: SizedBox.square(
                        dimension: Tactil.minimo - Espacio.s2,
                        child: Icon(icono, size: Espacio.s5, color: color),
                      ),
                    ),
                    const SizedBox(width: Espacio.s3),
                    Expanded(
                      child: Text(
                        texto,
                        style: Tipo.cuerpo.copyWith(color: color),
                      ),
                    ),
                    if (!destructiva) ...[
                      const SizedBox(width: Espacio.s3),
                      Icon(IconosDeMarca.seguir, color: t.text3),
                    ],
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

/// Varias filas en una sola tarjeta, separadas por una línea fina, como los ajustes de las
/// apps bancarias. Una fila sola también se ve mejor así que como tarjeta suelta.
class GrupoDeFilas extends StatelessWidget {
  const GrupoDeFilas({super.key, required this.filas});

  final List<Widget> filas;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Tarjeta(
      relleno: false,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(Radios.lg),
        child: Column(
          children: [
            for (final (i, fila) in filas.indexed) ...[
              if (i > 0)
                Divider(
                  height: Borde.fino,
                  thickness: Borde.fino,
                  indent: Espacio.s4 + Tactil.minimo - Espacio.s2 + Espacio.s3,
                  color: t.border,
                ),
              fila,
            ],
          ],
        ),
      ),
    );
  }
}
