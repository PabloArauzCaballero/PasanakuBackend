import 'package:flutter/widgets.dart';

import '../atomos/iconos_de_marca.dart';

/// Qué pasó, no si el número sube o baja: cada tipo lleva su ícono (regla 1 de la maqueta).
enum TipoDeMovimiento {
  aporte,
  recargaQR,
  recargaTransferencia,
  retiro,
  entrega,
  comision,
  reverso,
  debitoRechazado,
  bono,
  transferencia,
}

extension TipoDeMovimientoIcono on TipoDeMovimiento {
  IconData get icono => switch (this) {
    TipoDeMovimiento.aporte => IconosDeMarca.aportar,
    TipoDeMovimiento.recargaQR => IconosDeMarca.qr,
    TipoDeMovimiento.recargaTransferencia => IconosDeMarca.ingresoDesdeBanco,
    TipoDeMovimiento.retiro => IconosDeMarca.salidaAlBanco,
    TipoDeMovimiento.entrega => IconosDeMarca.entregaDelFondo,
    TipoDeMovimiento.comision => IconosDeMarca.comision,
    TipoDeMovimiento.reverso => IconosDeMarca.reverso,
    TipoDeMovimiento.debitoRechazado => IconosDeMarca.rechazado,
    TipoDeMovimiento.bono => IconosDeMarca.bono,
    TipoDeMovimiento.transferencia => IconosDeMarca.movimientos,
  };

  String get nombre => switch (this) {
    TipoDeMovimiento.aporte => 'Aporte',
    TipoDeMovimiento.recargaQR => 'Recarga por QR',
    TipoDeMovimiento.recargaTransferencia => 'Recarga por transferencia',
    TipoDeMovimiento.retiro => 'Retiro',
    TipoDeMovimiento.entrega => 'Entrega del fondo',
    TipoDeMovimiento.comision => 'Comisión',
    TipoDeMovimiento.reverso => 'Reverso',
    TipoDeMovimiento.debitoRechazado => 'Débito rechazado',
    TipoDeMovimiento.bono => 'Bono',
    TipoDeMovimiento.transferencia => 'Transferencia',
  };
}
