import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'campo_busqueda.dart';

/// La hoja que abre un [CampoDeSeleccion] con muchas opciones: un buscador arriba y
/// la lista filtrada debajo.
///
/// Un menú desplegable de dieciocho renglones largos tapa la pantalla entera y obliga
/// a leerlos todos para encontrar el propio. Escribiendo tres letras —«com», «salud»,
/// «1987»— la lista se reduce a lo que importa.
///
/// Devuelve el índice elegido en [textos], o `null` si se cerró sin elegir.
Future<int?> mostrarListaConBuscador(
  BuildContext context, {
  required String titulo,
  required List<String> textos,
  int? elegido,
  TextInputType teclado = TextInputType.text,
}) => showModalBottomSheet<int>(
  context: context,
  isScrollControlled: true,
  useSafeArea: true,
  showDragHandle: true,
  builder: (_) => _ListaConBuscador(
    titulo: titulo,
    textos: textos,
    elegido: elegido,
    teclado: teclado,
  ),
);

/// Sin tildes ni mayúsculas: quien busca «educacion» tiene que encontrar «Educación».
String normalizarParaBuscar(String texto) {
  const con = 'áéíóúüñÁÉÍÓÚÜÑ';
  const sin = 'aeiouunAEIOUUN';
  final b = StringBuffer();
  for (final c in texto.split('')) {
    final i = con.indexOf(c);
    b.write(i < 0 ? c : sin[i]);
  }
  return b.toString().toLowerCase().trim();
}

class _ListaConBuscador extends StatefulWidget {
  const _ListaConBuscador({
    required this.titulo,
    required this.textos,
    required this.elegido,
    required this.teclado,
  });

  final String titulo;
  final List<String> textos;
  final int? elegido;
  final TextInputType teclado;

  @override
  State<_ListaConBuscador> createState() => _ListaConBuscadorState();
}

class _ListaConBuscadorState extends State<_ListaConBuscador> {
  final _busqueda = TextEditingController();

  @override
  void initState() {
    super.initState();
    _busqueda.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _busqueda.dispose();
    super.dispose();
  }

  List<int> get _visibles {
    final q = normalizarParaBuscar(_busqueda.text);
    return [
      for (var i = 0; i < widget.textos.length; i++)
        if (q.isEmpty || normalizarParaBuscar(widget.textos[i]).contains(q)) i,
    ];
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final visibles = _visibles;
    final alto = MediaQuery.sizeOf(context).height * 0.75;
    return Padding(
      // Sube con el teclado: si no, el buscador queda tapado al escribir.
      padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
      child: SizedBox(
        height: alto,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
              child: Text(
                widget.titulo,
                style: Tipo.titulo3.copyWith(color: t.text),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(Espacio.s4),
              child: CampoBusqueda(
                controlador: _busqueda,
                etiqueta: 'Buscar',
                teclado: widget.teclado,
              ),
            ),
            Expanded(
              child: visibles.isEmpty
                  ? Padding(
                      padding: const EdgeInsets.all(Espacio.s5),
                      child: Text(
                        'No hay opciones con «${_busqueda.text.trim()}».',
                        textAlign: TextAlign.center,
                        style: Tipo.cuerpo.copyWith(color: t.text3),
                      ),
                    )
                  : ListView.builder(
                      itemCount: visibles.length,
                      itemBuilder: (context, n) {
                        final i = visibles[n];
                        final esElegido = i == widget.elegido;
                        return ListTile(
                          minTileHeight: Tactil.minimo,
                          title: Text(
                            widget.textos[i],
                            style: Tipo.cuerpo.copyWith(
                              color: esElegido ? t.brandTexto : t.text,
                            ),
                          ),
                          trailing: esElegido
                              ? Icon(Icons.check_rounded, color: t.brandTexto)
                              : null,
                          selected: esElegido,
                          onTap: () => Navigator.of(context).pop(i),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
