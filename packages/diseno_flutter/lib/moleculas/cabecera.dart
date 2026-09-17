import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// La cabecera de una pantalla de nivel superior: un saludo chico y el título grande,
/// alineados a la izquierda, con las acciones a la derecha.
///
/// **Una cabecera por pantalla.** Antes el shell dibujaba una barra con «AportaYa» y
/// cada pantalla ponía otro título debajo: dos cabeceras para una sola pantalla, y el
/// nombre de la app ocupando el lugar más valioso en cada vista.
class CabeceraDeInicio extends StatelessWidget {
  const CabeceraDeInicio({
    super.key,
    required this.titulo,
    this.saludo,
    this.acciones = const [],
  });

  final String titulo;
  final String? saludo;
  final List<Widget> acciones;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: Espacio.s2, bottom: Espacio.s1),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: MergeSemantics(
              child: Semantics(
                header: true,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (saludo != null)
                      Text(
                        saludo!,
                        style: Tipo.cuerpoChico.copyWith(color: t.text2),
                      ),
                    Text(titulo, style: Tipo.titulo1.copyWith(color: t.text)),
                  ],
                ),
              ),
            ),
          ),
          ...acciones,
        ],
      ),
    );
  }
}

/// La cabecera de una pantalla interior: volver, título, y una acción opcional.
/// Reemplaza al `AppBar` con título centrado, que empujaba el contenido hacia abajo y
/// recortaba los títulos largos.
class CabeceraDeSeccion extends StatelessWidget {
  const CabeceraDeSeccion({
    super.key,
    required this.titulo,
    this.alVolver,
    this.accion,
  });

  final String titulo;
  final VoidCallback? alVolver;
  final Widget? accion;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: Espacio.s2, bottom: Espacio.s1),
      child: Row(
        children: [
          if (alVolver != null || Navigator.of(context).canPop())
            Padding(
              padding: const EdgeInsets.only(right: Espacio.s2),
              child: BotonDeCabecera(
                icono: Icons.arrow_back,
                etiqueta: 'Volver',
                onTap: alVolver ?? () => Navigator.of(context).maybePop(),
              ),
            ),
          Expanded(
            child: Semantics(
              header: true,
              child: Text(
                titulo,
                style: Tipo.titulo2.copyWith(color: t.text),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ),
          if (accion != null) ...[const SizedBox(width: Espacio.s2), accion!],
        ],
      ),
    );
  }
}

/// El botón cuadrado de una cabecera: área táctil completa, forma de campo, y un punto
/// de aviso opcional en la esquina.
class BotonDeCabecera extends StatelessWidget {
  const BotonDeCabecera({
    super.key,
    required this.icono,
    required this.etiqueta,
    required this.onTap,
    this.conAviso = false,
  });

  final IconData icono;
  final String etiqueta;
  final VoidCallback onTap;
  final bool conAviso;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      button: true,
      label: etiqueta,
      child: Material(
        color: t.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(Radios.md),
          side: BorderSide(color: t.border, width: Borde.fino),
        ),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(Radios.md),
          child: SizedBox.square(
            dimension: Tactil.minimo,
            child: Stack(
              alignment: Alignment.center,
              children: [
                Icon(icono, size: 20, color: t.text),
                if (conAviso)
                  Positioned(
                    top: Espacio.s2 + 1,
                    right: Espacio.s2 + 1,
                    child: Container(
                      width: Espacio.s2,
                      height: Espacio.s2,
                      decoration: BoxDecoration(
                        color: t.accent,
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: t.surface,
                          width: Borde.desfase,
                        ),
                      ),
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
