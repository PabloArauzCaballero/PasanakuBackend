import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'dominio/bloques_del_contrato.dart';
import 'texto_del_contrato.dart';

/// El contrato pintado como un documento: cada cláusula con su número arriba y su
/// nombre como título, los incisos con sangría colgante, el tarifario en su propio
/// recuadro y el pie de versión en voz baja.
///
/// Solo tokens: los colores salen de [Tokens] y la tipografía de [Tipo], así que el
/// tema claro y el oscuro salen solos.
/// Unos 75 caracteres de cuerpo: más ancho, en una tableta o en la web, el ojo pierde
/// la línea siguiente al volver. Lo usa la pantalla para todo el cuerpo, no solo el texto.
const double anchoDeLectura = 680;

class CuerpoDelContrato extends StatelessWidget {
  const CuerpoDelContrato({super.key});

  static final _bloques = bloquesDelContrato(textoDelContrato);

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final hijos = <Widget>[];
    var tarifario = <Widget>[];

    void cerrarTarifario() {
      if (tarifario.isEmpty) return;
      hijos.add(
        Container(
          margin: const EdgeInsets.only(top: Espacio.s5),
          padding: const EdgeInsets.all(Espacio.s4),
          decoration: BoxDecoration(
            color: t.surface,
            borderRadius: BorderRadius.circular(Radios.lg),
            border: Border.all(color: t.border, width: Borde.fino),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: tarifario,
          ),
        ),
      );
      tarifario = [];
    }

    for (final b in _bloques) {
      final pieza = _pieza(b, t);
      if (b.enTarifario) {
        tarifario.add(pieza);
      } else {
        cerrarTarifario();
        hijos.add(pieza);
      }
    }
    cerrarTarifario();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: hijos,
    );
  }

  Widget _pieza(BloqueDelContrato b, Tokens t) => switch (b.tipo) {
    TipoDeBloque.titulo => Padding(
      padding: const EdgeInsets.only(bottom: Espacio.s3),
      child: Semantics(
        header: true,
        child: Text(
          enTipoOracion(b.texto),
          style: Tipo.titulo2.copyWith(color: t.text),
        ),
      ),
    ),
    TipoDeBloque.seccion => Padding(
      padding: EdgeInsets.only(
        top: b.enTarifario ? 0 : Espacio.s6,
        bottom: Espacio.s2,
      ),
      child: Semantics(
        header: true,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (b.ordinal != null)
              Padding(
                padding: const EdgeInsets.only(bottom: Espacio.s1),
                child: Text(
                  'CLÁUSULA ${b.ordinal}',
                  style: Tipo.etiqueta.copyWith(color: t.brandTexto),
                ),
              ),
            Text(
              enTipoOracion(b.texto),
              style: Tipo.titulo3.copyWith(color: t.text),
            ),
          ],
        ),
      ),
    ),
    TipoDeBloque.parrafo => Padding(
      padding: const EdgeInsets.only(bottom: Espacio.s3),
      child: _conTermino(b.texto, t),
    ),
    TipoDeBloque.introduccion => Padding(
      padding: const EdgeInsets.only(bottom: Espacio.s2),
      child: Text(b.texto, style: Tipo.cuerpoFuerte.copyWith(color: t.text)),
    ),
    TipoDeBloque.item => Padding(
      padding: const EdgeInsets.only(left: Espacio.s2, bottom: Espacio.s2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: Espacio.s5,
            child: b.marca == '·'
                ? Padding(
                    // Centra el punto en la primera línea: alto de línea del
                    // cuerpo (15 × 1,5) menos el punto, a la mitad.
                    padding: const EdgeInsets.only(top: Espacio.s2),
                    child: Container(
                      width: Espacio.s2,
                      height: Espacio.s2,
                      decoration: BoxDecoration(
                        color: t.brand,
                        shape: BoxShape.circle,
                      ),
                    ),
                  )
                : Text(
                    b.marca!,
                    style: Tipo.cuerpoFuerte.copyWith(color: t.brandTexto),
                  ),
          ),
          Expanded(
            child: Text(b.texto, style: Tipo.cuerpo.copyWith(color: t.text2)),
          ),
        ],
      ),
    ),
    TipoDeBloque.pie => Padding(
      padding: const EdgeInsets.only(top: Espacio.s4),
      child: Text(b.texto, style: Tipo.cuerpoChico.copyWith(color: t.text3)),
    ),
  };
}

/// «Billetera: el instrumento…» — el término definido va en negrita. Solo cuando lo
/// que precede a los dos puntos es corto y no es una oración: un párrafo que cita
/// algo con dos puntos en el medio queda como está.
Widget _conTermino(String texto, Tokens t) {
  final i = texto.indexOf(': ');
  final cuerpo = Tipo.cuerpo.copyWith(color: t.text2);
  if (i <= 0 || i > 40 || texto.substring(0, i).contains('.')) {
    return Text(texto, style: cuerpo);
  }
  return Text.rich(
    TextSpan(
      style: cuerpo,
      children: [
        TextSpan(
          text: texto.substring(0, i + 1),
          style: Tipo.cuerpoFuerte.copyWith(color: t.text),
        ),
        TextSpan(text: texto.substring(i + 1)),
      ],
    ),
  );
}
