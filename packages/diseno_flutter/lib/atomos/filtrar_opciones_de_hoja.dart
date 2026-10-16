import 'normalizar_para_buscar.dart';

/// Conserva los índices originales de las opciones que coinciden por texto o detalle.
List<int> filtrarIndicesDeOpcionesDeHoja(
  List<({String texto, String? detalle})> opciones,
  String consulta,
) {
  final q = normalizarParaBuscar(consulta);
  return [
    for (var i = 0; i < opciones.length; i++)
      if (q.isEmpty ||
          normalizarParaBuscar(opciones[i].texto).contains(q) ||
          normalizarParaBuscar(opciones[i].detalle ?? '').contains(q))
        i,
  ];
}
