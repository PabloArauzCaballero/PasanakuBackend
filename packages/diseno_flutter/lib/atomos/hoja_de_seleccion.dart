import 'dart:async';

import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import '../moviles/alcance_haptica.dart';
import 'campo_busqueda.dart';
import 'fila_de_seleccion.dart';
import 'filtrar_opciones_de_hoja.dart';
import 'movimiento.dart';

typedef OpcionDeHoja = ({String texto, String? detalle});

Future<int?> mostrarHojaDeSeleccion(
  BuildContext context, {
  required String titulo,
  required List<OpcionDeHoja> opciones,
  int? elegida,
  String? ayuda,
  TextInputType teclado = TextInputType.text,
}) => showModalBottomSheet<int>(
  context: context,
  isScrollControlled: true,
  useSafeArea: true,
  showDragHandle: true,
  builder: (_) => _HojaDeSeleccion(
    titulo: titulo,
    opciones: opciones,
    elegida: elegida,
    ayuda: ayuda,
    teclado: teclado,
  ),
);

class _HojaDeSeleccion extends StatefulWidget {
  const _HojaDeSeleccion({
    required this.titulo,
    required this.opciones,
    required this.elegida,
    required this.ayuda,
    required this.teclado,
  });
  final String titulo;
  final List<OpcionDeHoja> opciones;
  final int? elegida;
  final String? ayuda;
  final TextInputType teclado;

  @override
  State<_HojaDeSeleccion> createState() => _HojaDeSeleccionState();
}

class _HojaDeSeleccionState extends State<_HojaDeSeleccion> {
  final _busqueda = TextEditingController();
  final _focoTitulo = FocusNode(debugLabel: 'Título del selector');
  late final ScrollController _lista;

  @override
  void initState() {
    super.initState();
    _lista = ScrollController(
      initialScrollOffset: (widget.elegida ?? 0) * Tactil.minimo,
    );
    _busqueda.addListener(_cambio);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _focoTitulo.requestFocus();
    });
  }

  void _cambio() => setState(() {});

  @override
  void dispose() {
    _busqueda.removeListener(_cambio);
    _busqueda.dispose();
    _focoTitulo.dispose();
    _lista.dispose();
    super.dispose();
  }

  List<int> get _visibles =>
      filtrarIndicesDeOpcionesDeHoja(widget.opciones, _busqueda.text);

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final media = MediaQuery.of(context);
    final teclado = media.viewInsets.bottom;
    final disponible = media.size.height - media.padding.top - teclado;
    final visibles = _visibles;
    final duracionAnimacion = duracion(context, Movimiento.micro);
    return AnimatedPadding(
      // El route no reajusta el contenido a los viewInsets en todos los hosts.
      padding: EdgeInsets.only(bottom: teclado),
      duration: duracion(context, Movimiento.entrada),
      curve: Movimiento.llega,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxHeight: disponible * .92),
        child: Column(
          mainAxisSize: teclado > 0 ? MainAxisSize.max : MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
              child: Focus(
                focusNode: _focoTitulo,
                child: Semantics(
                  header: true,
                  child: Text(
                    widget.titulo,
                    style: Tipo.titulo3.copyWith(color: t.text),
                  ),
                ),
              ),
            ),
            if (widget.ayuda case final ayuda?)
              Padding(
                padding: const EdgeInsets.fromLTRB(
                  Espacio.s4,
                  Espacio.s2,
                  Espacio.s4,
                  0,
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.info_outline_rounded, size: 16, color: t.text3),
                    const SizedBox(width: Espacio.s2),
                    Expanded(
                      child: Text(
                        ayuda,
                        style: Tipo.ayuda.copyWith(color: t.text2),
                      ),
                    ),
                  ],
                ),
              ),
            Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: Espacio.s4,
                vertical: Espacio.s1,
              ),
              child: CampoBusqueda(
                controlador: _busqueda,
                etiqueta: 'Buscar en ${widget.titulo.toLowerCase()}',
                teclado: widget.teclado,
                sinAutocorreccion: true,
                capitalizacion: TextCapitalization.none,
                accionDeTeclado: TextInputAction.search,
              ),
            ),
            Flexible(
              child: AnimatedSwitcher(
                duration: duracionAnimacion,
                switchInCurve: Movimiento.llega,
                switchOutCurve: Movimiento.sale,
                child: visibles.isEmpty
                    ? Padding(
                        key: const ValueKey('vacío'),
                        padding: const EdgeInsets.all(Espacio.s5),
                        child: Text(
                          'Nada coincide con «${_busqueda.text.trim()}».',
                          textAlign: TextAlign.center,
                          style: Tipo.cuerpo.copyWith(color: t.text3),
                        ),
                      )
                    : ListView.builder(
                        key: const ValueKey('opciones'),
                        controller: _lista,
                        // El alto visible y el área táctil coinciden con el mínimo
                        // del sistema; el padding interno ya forma parte de esa fila.
                        itemExtent: Tactil.minimo,
                        keyboardDismissBehavior:
                            ScrollViewKeyboardDismissBehavior.manual,
                        itemCount: visibles.length,
                        itemBuilder: (context, n) {
                          final i = visibles[n];
                          final opcion = widget.opciones[i];
                          return FilaDeSeleccion(
                            texto: opcion.texto,
                            detalle: opcion.detalle,
                            elegida: i == widget.elegida,
                            consulta: _busqueda.text,
                            onTap: () {
                              unawaited(
                                AlcanceHaptica.maybeOf(context)?.seleccion(),
                              );
                              Navigator.of(context).pop(i);
                            },
                          );
                        },
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
