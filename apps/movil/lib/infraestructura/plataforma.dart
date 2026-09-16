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
import 'camara_del_sistema.dart';
import 'camara_prestada.dart';
import 'ios/haptica_ios.dart';

/// **El ÚNICO lugar donde se pregunta por la plataforma** (ADR-036). `Platform.is*`
/// en cualquier otro archivo de `lib/` fuera de `infraestructura/` es un rechazo; lo
/// verifica `grep -r "Platform.is" lib/` en el gate de este carril.
///
/// Android es la implementación real de los siete puertos. iOS tiene hoy el pase de
/// `AlmacenSeguro` y `Haptica` (ninguno de los dos usa una API exclusiva de
/// plataforma para las demás): `Biometria`, `AvisosPush`, `Camara` y
/// `ProteccionPantalla` en iOS son el hueco declarado del pase de paridad
/// (ADR-036 · «Android primero, iOS por pase»), y hoy caen al mismo `MethodChannel`
/// que Android sin un receptor nativo iOS — devuelven el valor seguro por omisión.
AlmacenSeguro almacenSeguroDeLaPlataforma() =>
    !kIsWeb && Platform.isIOS ? AlmacenSeguroIos() : AlmacenSeguroAndroid();

Conectividad conectividadDeLaPlataforma() => ConectividadAndroid();

Haptica hapticaDeLaPlataforma() =>
    !kIsWeb && Platform.isIOS ? HapticaIos() : HapticaAndroid();

Biometria biometriaDeLaPlataforma() => BiometriaAndroid();

AvisosPush avisosPushDeLaPlataforma() => AvisosPushAndroid();

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

ProteccionPantalla proteccionPantallaDeLaPlataforma() =>
    ProteccionPantallaAndroid();

/// `IOS` o `ANDROID`, con los nombres que usa el contrato de identidad. Está acá y no
/// en el caso de uso porque `Platform.is*` fuera de `infraestructura/` es un rechazo
/// del gate (ADR-036).
String nombreDePlataforma() =>
    kIsWeb ? 'WEB' : (Platform.isIOS ? 'IOS' : 'ANDROID');
