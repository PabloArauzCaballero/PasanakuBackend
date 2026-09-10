import '../moleculas/tipo_de_movimiento.dart';

/// Un movimiento con su fecha, para agrupar.
typedef Movimiento = ({
  String fechaIso,
  TipoDeMovimiento tipo,
  String concepto,
  String monto,
  String? saldoCorrido,
  bool pendiente,
});
