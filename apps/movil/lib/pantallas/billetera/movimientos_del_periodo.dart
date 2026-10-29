import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';

/// Qué movimientos del período se muestran.
enum FiltroDelExtracto { todos, entradas, salidas }

/// Los movimientos de un día (de calendario local), del más nuevo al más viejo.
typedef DiaDeMovimientos = ({DateTime dia, List<MovimientoBilletera> filas});

DateTime _soloDia(DateTime d) => DateTime(d.year, d.month, d.day);

/// Recorta los movimientos al período elegido (ambos días incluidos), aplica el filtro y los
/// agrupa por día. Solo ordena y agrupa lo que el servidor mandó: no calcula importes.
List<DiaDeMovimientos> movimientosDelPeriodo(
  Iterable<MovimientoBilletera> movimientos, {
  required DateTime desde,
  required DateTime hasta,
  FiltroDelExtracto filtro = FiltroDelExtracto.todos,
}) {
  final inicio = _soloDia(desde.toLocal());
  final fin = _soloDia(hasta.toLocal()).add(const Duration(days: 1));
  final elegidos = movimientos.where((m) {
    final cuando = m.registradoEn.toLocal();
    if (cuando.isBefore(inicio) || !cuando.isBefore(fin)) return false;
    final credito = m.sentido == MovimientoBilleteraSentidoEnum.CREDITO;
    return switch (filtro) {
      FiltroDelExtracto.todos => true,
      FiltroDelExtracto.entradas => credito,
      FiltroDelExtracto.salidas => !credito,
    };
  }).toList()..sort((a, b) => b.registradoEn.compareTo(a.registradoEn));
  final porDia = <DateTime, List<MovimientoBilletera>>{};
  for (final m in elegidos) {
    porDia.putIfAbsent(_soloDia(m.registradoEn.toLocal()), () => []).add(m);
  }
  return [for (final e in porDia.entries) (dia: e.key, filas: e.value)];
}

/// `true` si la lectura pudo haberse quedado corta: llegó al tope pedido y el más viejo
/// recibido es posterior al inicio del período.
bool lecturaPudoQuedarCorta(
  List<MovimientoBilletera> movimientos, {
  required int limite,
  required DateTime desde,
}) {
  if (movimientos.length < limite) return false;
  final masViejo = movimientos
      .map((m) => m.registradoEn)
      .reduce((a, b) => a.isBefore(b) ? a : b);
  return masViejo.toLocal().isAfter(_soloDia(desde.toLocal()));
}
