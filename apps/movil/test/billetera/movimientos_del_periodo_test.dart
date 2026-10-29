import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_movil/pantallas/billetera/movimientos_del_periodo.dart';
import 'package:flutter_test/flutter_test.dart';

MovimientoBilletera _mov(
  String id,
  DateTime cuando, {
  bool credito = true,
}) => MovimientoBilletera(
  id: id,
  sentido: credito
      ? MovimientoBilleteraSentidoEnum.CREDITO
      : MovimientoBilleteraSentidoEnum.DEBITO,
  monto: Dinero(monto: '10.00', moneda: DineroMonedaEnum.BOB),
  saldoDisponiblePosterior: Dinero(monto: '0.00', moneda: DineroMonedaEnum.BOB),
  saldoRetenidoPosterior: Dinero(monto: '0.00', moneda: DineroMonedaEnum.BOB),
  glosa: 'g$id',
  registradoEn: cuando,
);

void main() {
  final desde = DateTime(2026, 9, 1);
  final hasta = DateTime(2026, 9, 30);
  final todos = [
    _mov('a', DateTime(2026, 9, 1, 0, 0)), // borde: primer instante
    _mov('b', DateTime(2026, 9, 30, 23, 59)), // borde: último minuto
    _mov('c', DateTime(2026, 8, 31, 23, 59)), // fuera: un minuto antes
    _mov('d', DateTime(2026, 10, 1, 0, 0)), // fuera: primer instante después
    _mov('e', DateTime(2026, 9, 15, 9), credito: false),
    _mov('f', DateTime(2026, 9, 15, 18)),
  ];

  test('correcto: agrupa por día, del más nuevo al más viejo', () {
    final dias = movimientosDelPeriodo(todos, desde: desde, hasta: hasta);
    expect([for (final d in dias) d.dia.day], [30, 15, 1]);
    expect([for (final m in dias[1].filas) m.id], ['f', 'e']);
  });

  test('límite: incluye ambos días extremos y deja afuera los vecinos', () {
    final ids = [
      for (final d in movimientosDelPeriodo(todos, desde: desde, hasta: hasta))
        for (final m in d.filas) m.id,
    ];
    expect(ids, containsAll(['a', 'b']));
    expect(ids, isNot(contains('c')));
    expect(ids, isNot(contains('d')));
  });

  test('filtros: entradas y salidas se excluyen entre sí', () {
    List<String> ids(FiltroDelExtracto f) => [
      for (final d in movimientosDelPeriodo(
        todos,
        desde: desde,
        hasta: hasta,
        filtro: f,
      ))
        for (final m in d.filas) m.id,
    ];
    expect(ids(FiltroDelExtracto.salidas), ['e']);
    expect(ids(FiltroDelExtracto.entradas), isNot(contains('e')));
    expect(ids(FiltroDelExtracto.entradas).length, 3);
  });

  test('inválido: sin movimientos o período sin nada devuelve lista vacía', () {
    expect(movimientosDelPeriodo([], desde: desde, hasta: hasta), isEmpty);
    expect(
      movimientosDelPeriodo(
        todos,
        desde: DateTime(2025, 1, 1),
        hasta: DateTime(2025, 1, 31),
      ),
      isEmpty,
    );
  });

  test(
    'la lectura pudo quedar corta solo si llegó al tope y no alcanza el inicio',
    () {
      expect(
        lecturaPudoQuedarCorta(todos, limite: 6, desde: DateTime(2026, 8, 1)),
        isTrue,
        reason: 'el más viejo (31 ago) es posterior al 1 ago → corta',
      );
      expect(
        lecturaPudoQuedarCorta(todos, limite: 100, desde: desde),
        isFalse,
        reason: 'no llegó al tope: el servidor mandó todo',
      );
      expect(
        lecturaPudoQuedarCorta(todos, limite: 6, desde: DateTime(2026, 9, 5)),
        isFalse,
        reason: 'ya llegó más atrás que el inicio del período',
      );
    },
  );
}
