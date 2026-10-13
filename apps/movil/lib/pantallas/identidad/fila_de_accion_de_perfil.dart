import 'package:aportaya_diseno/atomos/hundido_al_tocar.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Acción de navegación del perfil con semántica de botón y respuesta táctil.
class FilaDeAccionDePerfil extends StatelessWidget {
  const FilaDeAccionDePerfil({
    super.key,
    required this.claveSemantica,
    required this.texto,
    required this.pista,
    required this.onPressed,
  });

  final Key claveSemantica;
  final String texto;
  final String pista;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final forma = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(Radios.md),
      side: BorderSide(color: t.border, width: Borde.fino),
    );
    return Semantics(
      key: claveSemantica,
      button: true,
      label: texto,
      hint: pista,
      onTap: onPressed,
      child: ExcludeSemantics(
        child: HundidoAlTocar(
          child: Material(
            color: t.surface,
            shape: forma,
            child: InkWell(
              onTap: onPressed,
              borderRadius: BorderRadius.circular(Radios.md),
              child: ConstrainedBox(
                constraints: const BoxConstraints(minHeight: Tactil.minimo),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: Espacio.s4,
                    vertical: Espacio.s3,
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          texto,
                          style: Tipo.cuerpo.copyWith(color: t.text),
                        ),
                      ),
                      const SizedBox(width: Espacio.s3),
                      Icon(Icons.chevron_right, color: t.text3),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
