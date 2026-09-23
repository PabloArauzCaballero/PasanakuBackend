import 'dart:io';

import 'package:flutter/foundation.dart' show kIsWeb;

import '../dominio/puertos/almacen_seguro.dart';
import '../dominio/puertos/avisos_push.dart';
import '../dominio/puertos/biometria.dart';
import '../dominio/puertos/camara.dart';
import '../dominio/puertos/conectividad.dart';
import '../dominio/puertos/haptica.dart';
import '../dominio/puertos/proteccion_pantalla.dart';
import 'android/almacen_seguro_android.dart';
import 'android/avisos_push_android.dart';
import 'android/biometria_android.dart';
import 'android/conectividad_android.dart';
import 'android/haptica_android.dart';
import 'android/proteccion_pantalla_android.dart';
import 'ios/almacen_seguro_ios.dart';
import 'ios/avisos_push_ios.dart';
import 'ios/biometria_ios.dart';
import 'camara_del_sistema.dart';
import 'camara_prestada.dart';
import 'ios/haptica_ios.dart';
import 'ios/proteccion_pantalla_ios.dart';

/// **El ÚNICO lugar donde se pregunta por la plataforma** (ADR-036, enmendado en
/// H2.S2.M4 de PR13-Ci.Frontend). `Platform.is*` en cualquier otro archivo de `lib/`
/// fuera de `infraestructura/` es un rechazo; lo verifica `grep -r "Platform.is"
/// lib/` en el gate de este carril.
///
/// **Historia del hallazgo (H2, madre H6):** hasta este carril, `Biometria`,
/// `AvisosPush` y `ProteccionPantalla` devolvían SIEMPRE el adaptador de Android acá
/// abajo, sin mirar la plataforma. En iOS eso abre un `MethodChannel` sin ningún
/// receptor nativo iOS detrás (`bo.aportaya/biometria`, `.../avisos_push`,
/// `.../proteccion_pantalla`): "funcionaba" solo porque cada adaptador atrapa
/// `MissingPluginException` y devuelve su valor seguro -- un accidente de manejo de
/// errores, no una decisión. Ahora cada puerto resuelve explícitamente por
/// plataforma, y el grado de soporte que le corresponde a cada uno está declarado y
/// es consultable en `capacidades.dart` -- ver la tabla completa ahí y en
/// `docs/Arquitectura/ADR-036...md`.
AlmacenSeguro almacenSeguroDeLaPlataforma() =>
    !kIsWeb && Platform.isIOS ? AlmacenSeguroIos() : AlmacenSeguroAndroid();

/// **Un solo adaptador para las dos plataformas** (H2.S1.M4): `connectivity_plus`
/// es un plugin FEDERADO con implementación iOS real (no un canal propio sin
/// receptor) y la sonda al gateway (`Dio`) es Dart puro, sin nada exclusivo de
/// Android. Ver `entregables/decision-conectividad.md` para la cita de la versión
/// instalada. Mantiene el nombre de archivo/clase `ConectividadAndroid` a propósito
/// -- renombrarlo no es parte de este carril y solo agregaría ruido a un diff que ya
/// toca bastante -- pero la resolución de acá abajo ya NO depende de la plataforma.
Conectividad conectividadDeLaPlataforma() => ConectividadAndroid();

Haptica hapticaDeLaPlataforma() =>
    !kIsWeb && Platform.isIOS ? HapticaIos() : HapticaAndroid();

/// **No soportado en iOS** (`capacidades.dart`): sin `local_auth` en
/// `pubspec.yaml`, no hay cómo resolver `LocalAuthentication` sin agregar una
/// dependencia nueva sin justificar (regla 90.4.2). `BiometriaIos` no abre ningún
/// canal nativo.
Biometria biometriaDeLaPlataforma() =>
    !kIsWeb && Platform.isIOS ? const BiometriaIos() : BiometriaAndroid();

/// **No soportado en iOS** (`capacidades.dart`): el canal de Android habla con FCM,
/// que no resuelve APNs. `AvisosPushIos` no abre ningún canal nativo; `/notificaciones
/// /bandeja` sigue siendo la fuente de verdad (ADR-035), así que la app funciona
/// igual sin push.
AvisosPush avisosPushDeLaPlataforma() =>
    !kIsWeb && Platform.isIOS ? AvisosPushIos() : AvisosPushAndroid();

/// La cámara es la MISMA en las dos plataformas: la del sistema, con su permiso.
/// No hay nada exclusivo de Android ni de iOS que justifique dos adaptadores, y el
/// que había —un `MethodChannel` a un plugin que nunca existió— devolvía `null`
/// siempre, así que no se podía sacar una foto en ninguna de las dos.
/// Con `--dart-define=CAMARA_DEV=<url>` la foto la saca la camara del Mac a traves
/// de `scripts/camara_del_mac.py`. Es la unica forma de recorrer el alta en el
/// simulador de iOS, que no tiene camara ni puede usar la del Mac. Sin la bandera
/// —o sea, en cualquier build que no sea de desarrollo— esta rama no existe.
const _camaraDeDesarrollo = String.fromEnvironment('CAMARA_DEV');

Camara camaraDeLaPlataforma() => _camaraDeDesarrollo.isEmpty
    ? CamaraDelSistema()
    : CamaraPrestada(_camaraDeDesarrollo, CamaraDelSistema());

/// **No soportado en iOS** (`capacidades.dart`, CRÍTICA de seguridad):
/// `FLAG_SECURE` es de la API de Android; no hay un equivalente nativo registrado
/// para iOS en este carril. `ProteccionPantallaIos` no abre ningún canal nativo, y
/// `app.dart` bloquea el arranque en release de iOS mientras esto siga así
/// (H2.S2.M3) en vez de dejar una pantalla de saldo sin protección de captura.
ProteccionPantalla proteccionPantallaDeLaPlataforma() =>
    !kIsWeb && Platform.isIOS
    ? const ProteccionPantallaIos()
    : ProteccionPantallaAndroid();

/// `IOS` o `ANDROID`, con los nombres que usa el contrato de identidad. Está acá y no
/// en el caso de uso porque `Platform.is*` fuera de `infraestructura/` es un rechazo
/// del gate (ADR-036).
String nombreDePlataforma() =>
    kIsWeb ? 'WEB' : (Platform.isIOS ? 'IOS' : 'ANDROID');
