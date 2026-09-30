import 'dart:async';

import '../../dominio/puertos/avisos_push.dart';

/// **No soportado en iOS, explícito** (H2.S1.M2, madre H6.S1.M2).
///
/// El canal de avisos push de Android (`bo.aportaya/avisos_push`) habla con un SDK
/// de FCM que tampoco resuelve APNs: usarlo en iOS abriría un canal sin receptor
/// nativo. `token()` devuelve `null` -- el mismo valor "sin registrar" que ya declara
/// el puerto para "sin Google Play Services, por ejemplo" -- así que ninguna pantalla
/// tiene que distinguir "iOS" de "Android sin Play Services": las dos son "no hay
/// push, la bandeja alcanza" (ADR-035). `toques` nunca emite: no hay ningún canal
/// nativo detrás que pueda entregar un toque.
class AvisosPushIos implements AvisosPush {
  AvisosPushIos();

  final _toques = StreamController<String>.broadcast();

  @override
  Future<String?> token() async => null;

  @override
  Stream<String> get toques => _toques.stream;
}
