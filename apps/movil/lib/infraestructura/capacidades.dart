import 'dart:io';

import 'package:flutter/foundation.dart' show kIsWeb;

/// Cuánto puede la app confiar en un puerto **en la plataforma que corre ahora**.
///
/// Tres grados, no un booleano: un booleano solo puede decir "sí" o "no", y hay
/// puertos donde la respuesta correcta es "a medias" (por ejemplo, un aviso push que
/// nunca deja de funcionar del todo porque la bandeja del servidor es la fuente de
/// verdad — ADR-035 — pero tampoco puede avisar en la pantalla bloqueada).
enum GradoDeSoporte {
  /// El adaptador real de la plataforma, sin recortes.
  soportado,

  /// Funciona, pero con una limitación conocida y documentada (no bloquea la app).
  degradado,

  /// No hay ningún adaptador nativo detrás. La UI tiene que deshabilitar la acción
  /// con su motivo, nunca dejar que se llame a un canal que no existe.
  noSoportado,
}

/// El mapa de soporte de los **siete puertos** fijados en la Ola F2 (ADR-036),
/// resuelto una sola vez por `capacidadesDeLaPlataforma()` — igual que
/// `nombreDePlataforma()`, es lectura de plataforma y por eso vive acá y no en
/// `dominio/`: `Platform.is*` fuera de `infraestructura/` es un rechazo del gate.
///
/// **Por qué esto y no un booleano por puerto en cada proveedor.** Antes de esto,
/// `infraestructura/plataforma.dart` resolvía CUATRO de los siete puertos
/// (`Conectividad`, `Biometria`, `AvisosPush`, `ProteccionPantalla`) al mismo
/// adaptador de Android sin importar la plataforma real: en iOS abrían un
/// `MethodChannel` (`bo.aportaya/biometria`, etc.) que ningún receptor nativo de iOS
/// atiende. `MissingPluginException` se atrapaba y el puerto devolvía su valor
/// seguro por *accidente* de manejo de errores, no porque alguien hubiera decidido
/// que iOS no lo soporta. Este mapa hace esa decisión explícita, consultable y
/// proyectable en package:test — sin él, "no lo soporta" y "el canal nativo no está
/// registrado todavía" se ven exactamente igual.
class Capacidades {
  const Capacidades({
    required this.almacenSeguro,
    required this.conectividad,
    required this.biometria,
    required this.avisosPush,
    required this.camara,
    required this.proteccionPantalla,
    required this.haptica,
  });

  final GradoDeSoporte almacenSeguro;
  final GradoDeSoporte conectividad;
  final GradoDeSoporte biometria;
  final GradoDeSoporte avisosPush;
  final GradoDeSoporte camara;
  final GradoDeSoporte proteccionPantalla;
  final GradoDeSoporte haptica;

  /// Los DOS puertos que protegen dinero y datos personales en el dispositivo
  /// (H2.S2.M3, madre H6.S2.M3): sin almacén seguro no hay dónde guardar el token
  /// sin que quede legible; sin protección de pantalla, una captura o una grabación
  /// se lleva el saldo. Si cualquiera de los dos NO está soportado en un release,
  /// la app no puede arrancar con normalidad.
  bool get seguridadCriticaSoportada =>
      almacenSeguro == GradoDeSoporte.soportado &&
      proteccionPantalla != GradoDeSoporte.noSoportado;

  /// Para pintar el mapa completo en un log de diagnóstico o en la pantalla de
  /// soporte (H2.S2.M4 cita esto en el ADR).
  Map<String, GradoDeSoporte> get porPuerto => {
    'AlmacenSeguro': almacenSeguro,
    'Conectividad': conectividad,
    'Biometria': biometria,
    'AvisosPush': avisosPush,
    'Camara': camara,
    'ProteccionPantalla': proteccionPantalla,
    'Haptica': haptica,
  };
}

/// Android es la implementación de referencia: los siete puertos declaran soporte
/// PLENO (H2.S1.M1). El hueco de `AvisosPush` (sin SDK de Firebase todavía, ver
/// `infraestructura/android/avisos_push_android.dart`) es un hueco de PRODUCTO
/// —falta decidir el proveedor, ADR-035— no de PLATAFORMA: el canal está declarado y
/// listo, así que sigue siendo "soportado" en el sentido de este mapa.
const _capacidadesAndroid = Capacidades(
  almacenSeguro: GradoDeSoporte.soportado,
  conectividad: GradoDeSoporte.soportado,
  biometria: GradoDeSoporte.soportado,
  avisosPush: GradoDeSoporte.soportado,
  camara: GradoDeSoporte.soportado,
  proteccionPantalla: GradoDeSoporte.soportado,
  haptica: GradoDeSoporte.soportado,
);

/// iOS, con lo que existe HOY (ver la tabla en `docs/Arquitectura/ADR-036...md`):
///
/// | Puerto             | Grado       | Por qué |
/// |---                 |---          |---|
/// | AlmacenSeguro       | soportado   | `AlmacenSeguroIos` usa Keychain vía `flutter_secure_storage`, ya multiplataforma. |
/// | Conectividad        | soportado   | `connectivity_plus` es un plugin federado con implementación iOS real (ver `entregables/decision-conectividad.md`); la sonda al gateway es Dart puro. |
/// | Biometria           | noSoportado | Sin `local_auth` (ni ningún paquete que resuelva `LocalAuthentication`) en `pubspec.yaml`: agregarlo sin decidirlo es una dependencia nueva sin justificar (regla 90.4.2, H2.S1.M5). Deuda declarada. |
/// | AvisosPush          | noSoportado | El canal de Android no tiene receptor APNs; sin él, "soportado" sería fingir. `/notificaciones/bandeja` sigue siendo la fuente de verdad (ADR-035), así que la app funciona igual. |
/// | Camara              | soportado   | `image_picker` es multiplataforma real; no hay ningún `MethodChannel` propio detrás. |
/// | ProteccionPantalla  | noSoportado | `FLAG_SECURE` es una API de Android; iOS no tiene un equivalente por `MethodChannel` implementado hoy (bloquear captura en iOS pide una extensión nativa que no existe en este carril). Es CRÍTICA (`seguridadCriticaSoportada`). |
/// | Haptica             | soportado   | `HapticaIos` usa `HapticFeedback` del SDK de Flutter, no un canal propio. |
const _capacidadesIos = Capacidades(
  almacenSeguro: GradoDeSoporte.soportado,
  conectividad: GradoDeSoporte.soportado,
  biometria: GradoDeSoporte.noSoportado,
  avisosPush: GradoDeSoporte.noSoportado,
  camara: GradoDeSoporte.soportado,
  proteccionPantalla: GradoDeSoporte.noSoportado,
  haptica: GradoDeSoporte.soportado,
);

/// El único lugar (junto con `plataforma.dart`) donde se pregunta `Platform.is*`
/// para decidir capacidades. Inyectable por plataforma forzada en pruebas mediante
/// [paraPruebas]; en la app real siempre se llama sin argumentos.
Capacidades capacidadesDeLaPlataforma({bool? forzarIOSParaPruebas}) {
  final esIOS = forzarIOSParaPruebas ?? (!kIsWeb && Platform.isIOS);
  return esIOS ? _capacidadesIos : _capacidadesAndroid;
}
