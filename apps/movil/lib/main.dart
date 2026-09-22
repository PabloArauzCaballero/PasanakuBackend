import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'dominio/cliente.dart' show resultadoGateway;
import 'pantallas/arranque/pantalla_configuracion_invalida.dart';

void main() {
  // H3.S3.M2 / H5.S3.M2: si la configuración del gateway es inválida (sin los
  // `--dart-define` correctos en release, o un host fuera de la lista compilada), se
  // arma la pantalla de bloqueo y NUNCA `AppAportaYa` — así ningún provider que
  // dependa de `dioProvider` (`dominio/cliente.dart`) llega a leerse, y no se crea
  // ningún cliente HTTP ni sale ninguna petición.
  final child = resultadoGateway.valida
      ? AppAportaYa()
      : const PantallaConfiguracionInvalida();
  runApp(
    ProviderScope(
      // Nada de reintentos automáticos silenciosos (planes/10b §2): un proveedor que
      // falla queda en error y la persona reintenta a la vista. Riverpod 3 reintentaría
      // solo, con retroceso, y en una pantalla de dinero eso esconde el error.
      retry: (retryCount, error) => null,
      child: child,
    ),
  );
}
