import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:aportaya_movil/infraestructura/capacidades.dart';
import 'package:aportaya_movil/infraestructura/ios/avisos_push_ios.dart';
import 'package:aportaya_movil/infraestructura/ios/biometria_ios.dart';
import 'package:aportaya_movil/infraestructura/ios/proteccion_pantalla_ios.dart';

/// H2.S1.M3 (madre H6.S1.M3) -- catorce casos: los SIETE puertos, por las DOS
/// plataformas, con la plataforma FORZADA (`forzarIOSParaPruebas`) en vez de confiar
/// en el sistema operativo real donde corre el test runner (que nunca es iOS).
///
/// También cubre H2.S1.M2: los adaptadores "no soportados en iOS" no abren ningún
/// `MethodChannel` -- se lo verifica con un `MethodChannelMock` que hace fallar la
/// prueba si alguien invoca el canal.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group(
    'capacidadesDeLaPlataforma -- catorce casos (7 puertos x 2 plataformas)',
    () {
      final android = capacidadesDeLaPlataforma(forzarIOSParaPruebas: false);
      final ios = capacidadesDeLaPlataforma(forzarIOSParaPruebas: true);

      test('Android declara soporte PLENO en los siete puertos', () {
        expect(android.almacenSeguro, GradoDeSoporte.soportado);
        expect(android.conectividad, GradoDeSoporte.soportado);
        expect(android.biometria, GradoDeSoporte.soportado);
        expect(android.avisosPush, GradoDeSoporte.soportado);
        expect(android.camara, GradoDeSoporte.soportado);
        expect(android.proteccionPantalla, GradoDeSoporte.soportado);
        expect(android.haptica, GradoDeSoporte.soportado);
      });

      test(
        'iOS: AlmacenSeguro soportado (Keychain vía flutter_secure_storage)',
        () {
          expect(ios.almacenSeguro, GradoDeSoporte.soportado);
        },
      );

      test(
        'iOS: Conectividad soportada (connectivity_plus es multiplataforma real)',
        () {
          expect(ios.conectividad, GradoDeSoporte.soportado);
        },
      );

      test('iOS: Biometria NO soportada (sin local_auth en pubspec.yaml)', () {
        expect(ios.biometria, GradoDeSoporte.noSoportado);
      });

      test(
        'iOS: AvisosPush NO soportado (el canal de Android habla FCM, no APNs)',
        () {
          expect(ios.avisosPush, GradoDeSoporte.noSoportado);
        },
      );

      test('iOS: Camara soportada (image_picker es multiplataforma real)', () {
        expect(ios.camara, GradoDeSoporte.soportado);
      });

      test(
        'iOS: ProteccionPantalla NO soportada (FLAG_SECURE es de Android)',
        () {
          expect(ios.proteccionPantalla, GradoDeSoporte.noSoportado);
        },
      );

      test('iOS: Haptica soportada (HapticFeedback es del SDK de Flutter)', () {
        expect(ios.haptica, GradoDeSoporte.soportado);
      });
    },
  );

  group('seguridadCriticaSoportada', () {
    test('Android: soportada (almacén y protección de pantalla plenos)', () {
      expect(
        capacidadesDeLaPlataforma(
          forzarIOSParaPruebas: false,
        ).seguridadCriticaSoportada,
        isTrue,
      );
    });

    test('iOS: NO soportada hoy (protección de pantalla no soportada)', () {
      expect(
        capacidadesDeLaPlataforma(
          forzarIOSParaPruebas: true,
        ).seguridadCriticaSoportada,
        isFalse,
      );
    });
  });

  group('los adaptadores "no soportado en iOS" nunca abren un canal nativo', () {
    setUp(() {
      // Cualquier invocación a CUALQUIER canal durante este grupo hace fallar la
      // prueba: es la forma de comprobar que "no soportado" es una decisión, no un
      // `MissingPluginException` atrapado por accidente.
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
            const MethodChannel('bo.aportaya/biometria'),
            (call) =>
                fail('BiometriaIos invocó un canal nativo: ${call.method}'),
          );
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
            const MethodChannel('bo.aportaya/avisos_push'),
            (call) =>
                fail('AvisosPushIos invocó un canal nativo: ${call.method}'),
          );
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
            const MethodChannel('bo.aportaya/proteccion_pantalla'),
            (call) => fail(
              'ProteccionPantallaIos invocó un canal nativo: ${call.method}',
            ),
          );
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
            const MethodChannel('bo.aportaya/biometria'),
            null,
          );
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
            const MethodChannel('bo.aportaya/avisos_push'),
            null,
          );
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
            const MethodChannel('bo.aportaya/proteccion_pantalla'),
            null,
          );
    });

    test(
      'BiometriaIos.disponible()/confirmar() devuelven el valor seguro sin canal',
      () async {
        const biometria = BiometriaIos();
        expect(await biometria.disponible(), isFalse);
        expect(await biometria.confirmar('confirmar envío'), isFalse);
      },
    );

    test(
      'AvisosPushIos.token() es null sin canal, y toques no emite nada',
      () async {
        final avisos = AvisosPushIos();
        expect(await avisos.token(), isNull);
        final recibidos = <String>[];
        final suscripcion = avisos.toques.listen(recibidos.add);
        await Future<void>.delayed(const Duration(milliseconds: 50));
        await suscripcion.cancel();
        expect(recibidos, isEmpty);
      },
    );

    test(
      'ProteccionPantallaIos.activar()/desactivar() son no-ops sin canal',
      () async {
        const proteccion = ProteccionPantallaIos();
        await proteccion.activar();
        await proteccion.desactivar();
        // Si llegó hasta acá sin que `fail()` del handler se disparara, no hubo canal.
      },
    );
  });
}
