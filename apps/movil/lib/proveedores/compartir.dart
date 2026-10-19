import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/compartir.dart';
import '../infraestructura/android/compartir_android.dart';

/// Un solo adaptador: en Android abre las apps con intents; en iOS y web el canal no
/// existe, devuelve `false` y la pantalla ofrece copiar el enlace.
final compartirProvider = Provider<Compartir>((_) => CompartirAndroid());
