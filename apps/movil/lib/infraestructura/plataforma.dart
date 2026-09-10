import 'dart:io';

import '../dominio/puertos/almacen_seguro.dart';
import '../infraestructura/android/almacen_seguro_android.dart';
import '../infraestructura/ios/almacen_seguro_ios.dart';

/// El ÚNICO lugar donde se pregunta por la plataforma. `Platform.is*` en una vista
/// es un rechazo (ADR-036); el barrido `verificar_frontend.py` lo verifica.
AlmacenSeguro almacenSeguroDeLaPlataforma() =>
    Platform.isIOS ? AlmacenSeguroIos() : AlmacenSeguroAndroid();
