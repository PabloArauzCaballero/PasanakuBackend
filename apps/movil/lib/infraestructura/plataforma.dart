import 'dart:io';

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
    Platform.isIOS ? AlmacenSeguroIos() : AlmacenSeguroAndroid();

Conectividad conectividadDeLaPlataforma() => ConectividadAndroid();

Haptica hapticaDeLaPlataforma() =>
    Platform.isIOS ? HapticaIos() : HapticaAndroid();

Biometria biometriaDeLaPlataforma() => BiometriaAndroid();

AvisosPush avisosPushDeLaPlataforma() => AvisosPushAndroid();

/// La cámara es la MISMA en las dos plataformas: la del sistema, con su permiso.
/// No hay nada exclusivo de Android ni de iOS que justifique dos adaptadores, y el
/// que había —un `MethodChannel` a un plugin que nunca existió— devolvía `null`
/// siempre, así que no se podía sacar una foto en ninguna de las dos.
Camara camaraDeLaPlataforma() => CamaraDelSistema();

ProteccionPantalla proteccionPantallaDeLaPlataforma() =>
    ProteccionPantallaAndroid();

/// `IOS` o `ANDROID`, con los nombres que usa el contrato de identidad. Está acá y no
/// en el caso de uso porque `Platform.is*` fuera de `infraestructura/` es un rechazo
/// del gate (ADR-036).
String nombreDePlataforma() => Platform.isIOS ? 'IOS' : 'ANDROID';
