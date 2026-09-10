import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';

void main() {
  runApp(
    ProviderScope(
      // Nada de reintentos automáticos silenciosos (planes/10b §2): un proveedor que
      // falla queda en error y la persona reintenta a la vista. Riverpod 3 reintentaría
      // solo, con retroceso, y en una pantalla de dinero eso esconde el error.
      retry: (retryCount, error) => null,
      child: AppAportaYa(),
    ),
  );
}
