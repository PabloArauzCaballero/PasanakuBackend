import 'package:flutter/material.dart';

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
    TipoDeMovimiento.aporte => Icons.groups_outlined,
    TipoDeMovimiento.recargaQR => Icons.qr_code_2,
    TipoDeMovimiento.recargaTransferencia => Icons.account_balance_outlined,
    TipoDeMovimiento.retiro => Icons.outbox_outlined,
    TipoDeMovimiento.entrega => Icons.card_giftcard_outlined,
    TipoDeMovimiento.comision => Icons.receipt_long_outlined,
    TipoDeMovimiento.reverso => Icons.undo,
    TipoDeMovimiento.debitoRechazado => Icons.block_outlined,
    TipoDeMovimiento.bono => Icons.celebration_outlined,
    TipoDeMovimiento.transferencia => Icons.swap_horiz,
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
