import '../../dominio/puertos/biometria.dart';

/// **No soportado en iOS, explícito** (H2.S1.M2, madre H6.S1.M2).
///
/// Antes de esto, `biometriaDeLaPlataforma()` devolvía `BiometriaAndroid` también en
/// iOS: un `MethodChannel('bo.aportaya/biometria')` sin ningún receptor nativo de
/// iOS, que solo "andaba" porque `MissingPluginException` se atrapaba y devolvía el
/// valor seguro (`false`) por manejo de errores, no por decisión. Este adaptador NO
/// abre ningún canal — lo verifica el gate del carril con un `MethodChannel` de
/// prueba que falla si algo lo invoca (`test/unidad/capacidades_test.dart`).
///
/// Falta `local_auth` (o equivalente) en `pubspec.yaml` para resolver esto de
/// verdad: no se agrega sin decidirlo (regla 90.4.2) — ver
/// `entregables/decision-conectividad.md` §Biometría para la nota completa y la
/// deuda declarada.
class BiometriaIos implements Biometria {
  const BiometriaIos();

  @override
  Future<bool> disponible() async => false;

  @override
  Future<bool> confirmar(String motivo) async => false;
}
