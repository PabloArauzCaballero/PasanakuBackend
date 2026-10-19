import 'dart:convert';

import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class _AlmacenEnMemoria implements AlmacenSeguro {
  final datos = <String, String>{};

  @override
  Future<String?> leer(String clave) async => datos[clave];

  @override
  Future<void> guardar(String clave, String valor) async =>
      datos[clave] = valor;

  @override
  Future<void> borrar(String clave) async => datos.remove(clave);
}

class _AlmacenQueFalla implements AlmacenSeguro {
  @override
  Future<String?> leer(String clave) => Future.error(StateError('keystore'));

  @override
  Future<void> guardar(String clave, String valor) =>
      Future.error(StateError('keystore'));

  @override
  Future<void> borrar(String clave) => Future.error(StateError('keystore'));
}

void main() {
  final ahora = DateTime.utc(2026, 10, 12, 12);

  ProviderContainer crear(
    _AlmacenEnMemoria almacen,
    DateTime Function() reloj,
  ) => ProviderContainer(
    overrides: [
      almacenSeguroProvider.overrideWithValue(almacen),
      relojDeSesionProvider.overrideWithValue(reloj),
    ],
  );

  test('recién creada queda desconocida', () {
    final container = crear(_AlmacenEnMemoria(), () => ahora);
    expect(container.read(sesionProvider).estado, EstadoDeSesion.desconocida);
    container.dispose();
  });

  test('hidratar almacén vacío la deja anónima y notifica una vez', () async {
    final container = crear(_AlmacenEnMemoria(), () => ahora);
    final sesion = container.read(sesionProvider);
    var notificaciones = 0;
    sesion.addListener(() => notificaciones++);

    await sesion.hidratar();

    expect(sesion.estado, EstadoDeSesion.anonima);
    expect(notificaciones, 1);
    container.dispose();
  });

  test('hidrata tokens con inicio vigente como activa', () async {
    final almacen = _AlmacenEnMemoria()
      ..datos['sesion.acceso'] = 'access'
      ..datos['sesion.refresco'] = 'refresh'
      ..datos['sesion.inicio-con-credencial'] = ahora
          .subtract(const Duration(hours: 1))
          .millisecondsSinceEpoch
          .toString();
    final container = crear(almacen, () => ahora);

    await container.read(sesionProvider).hidratar();

    expect(container.read(sesionProvider).estado, EstadoDeSesion.activa);
    container.dispose();
  });

  test(
    'un acceso sin refresco conserva sesión activa mientras el JWT siga vigente',
    () async {
      final almacen = _AlmacenEnMemoria()
        ..datos['sesion.acceso'] = 'access'
        ..datos['sesion.inicio-con-credencial'] = ahora
            .subtract(const Duration(minutes: 5))
            .millisecondsSinceEpoch
            .toString();
      final container = crear(almacen, () => ahora);

      await container.read(sesionProvider).hidratar();

      expect(container.read(sesionProvider).estado, EstadoDeSesion.activa);
      expect(await container.read(sesionProvider).tokenDeRefresco(), isNull);
      container.dispose();
    },
  );

  test(
    'tokens sin inicio conocido se invalidan con motivo de caducidad',
    () async {
      final almacen = _AlmacenEnMemoria()
        ..datos['sesion.acceso'] = 'access'
        ..datos['sesion.refresco'] = 'refresh';
      final container = crear(almacen, () => ahora);
      final sesion = container.read(sesionProvider);

      await sesion.hidratar();

      expect(sesion.estado, EstadoDeSesion.anonima);
      expect(sesion.tomarMotivoDeSalida(), MotivoDeSalida.caducada);
      expect(await sesion.tokenDeAcceso(), isNull);
      container.dispose();
    },
  );

  test('abrir persiste tokens e inicio y notifica la transición', () async {
    final almacen = _AlmacenEnMemoria();
    final container = crear(almacen, () => ahora);
    final sesion = container.read(sesionProvider);
    var notificaciones = 0;
    sesion.addListener(() => notificaciones++);

    await sesion.abrir(acceso: 'access', refresco: 'refresh');

    expect(sesion.estado, EstadoDeSesion.activa);
    expect(await sesion.inicioConCredencial(), ahora);
    expect(notificaciones, 1);
    container.dispose();
  });

  test('solo trata permisos explícitos del token como señal de navegación', () {
    String token(Map<String, Object?> carga) {
      String parte(Object valor) =>
          base64Url.encode(utf8.encode(jsonEncode(valor))).replaceAll('=', '');
      return '${parte({'alg': 'none'})}.${parte(carga)}.';
    }

    expect(
      billeteraPermitidaDelToken(
        token({
          'sub': 'usuario-sintetico',
          'permisos': ['BILLETERA_VER'],
        }),
      ),
      isTrue,
    );
    expect(
      billeteraPermitidaDelToken(
        token({'sub': 'usuario-sintetico', 'permisos': <String>[]}),
      ),
      isFalse,
    );
    expect(
      billeteraPermitidaDelToken(token({'sub': 'usuario-sintetico'})),
      isNull,
    );
    expect(billeteraPermitidaDelToken('token-malformado'), isNull);
  });

  test(
    'si faltan permisos pero hay sub, permite consultar el estado propio',
    () async {
      String parte(Object valor) =>
          base64Url.encode(utf8.encode(jsonEncode(valor))).replaceAll('=', '');
      final token =
          '${parte({'alg': 'none'})}.${parte({'sub': 'usuario-sintetico'})}.';
      final sesion = Sesion(_AlmacenEnMemoria(), () => ahora);

      await sesion.abrir(acceso: token);

      expect(sesion.requiereVerificacion, isTrue);
      expect(sesion.usuarioIdActual, 'usuario-sintetico');
      sesion.dispose();
    },
  );

  test('la sesión mantiene id y puerta UX según el permiso explícito', () async {
    String token(List<String> permisos) {
      String parte(Object valor) =>
          base64Url.encode(utf8.encode(jsonEncode(valor))).replaceAll('=', '');
      return '${parte({'alg': 'none'})}.${parte({'sub': 'usuario-sintetico', 'permisos': permisos})}.';
    }

    final sesion = Sesion(_AlmacenEnMemoria(), () => ahora);
    await sesion.abrir(acceso: token([]));
    expect(sesion.usuarioIdActual, 'usuario-sintetico');
    expect(sesion.requiereVerificacion, isTrue);

    await sesion.renovar(
      acceso: token(['BILLETERA_VER']),
      refresco: 'refresco-sintetico',
    );
    expect(sesion.requiereVerificacion, isFalse);

    await sesion.cerrar();
    expect(sesion.usuarioIdActual, isNull);
    expect(sesion.requiereVerificacion, isFalse);
    sesion.dispose();
  });

  test('renovar rota tokens sin cambiar inicio ni notificar estado', () async {
    final almacen = _AlmacenEnMemoria();
    final container = crear(almacen, () => ahora);
    final sesion = container.read(sesionProvider);
    await sesion.abrir(acceso: 'access', refresco: 'refresh');
    final inicio = await sesion.inicioConCredencial();
    var notificaciones = 0;
    sesion.addListener(() => notificaciones++);

    await sesion.renovar(acceso: 'access-2', refresco: 'refresh-2');

    expect(await sesion.tokenDeAcceso(), 'access-2');
    expect(await sesion.tokenDeRefresco(), 'refresh-2');
    expect(await sesion.inicioConCredencial(), inicio);
    expect(notificaciones, 0);
    container.dispose();
  });

  test(
    'cerrar borra credenciales, conserva huella y entrega motivo una vez',
    () async {
      final almacen = _AlmacenEnMemoria()
        ..datos['dispositivo.huella'] = 'device';
      final container = crear(almacen, () => ahora);
      final sesion = container.read(sesionProvider);
      await sesion.abrir(acceso: 'access', refresco: 'refresh');

      await sesion.cerrar(motivo: MotivoDeSalida.caducada);

      expect(sesion.estado, EstadoDeSesion.anonima);
      expect(await sesion.tokenDeAcceso(), isNull);
      expect(await sesion.tokenDeRefresco(), isNull);
      expect(await sesion.inicioConCredencial(), isNull);
      expect(await sesion.leerHuella(), 'device');
      expect(sesion.tomarMotivoDeSalida(), MotivoDeSalida.caducada);
      expect(sesion.tomarMotivoDeSalida(), isNull);
      container.dispose();
    },
  );

  test(
    'si falla la lectura del almacén hidratar falla de forma segura',
    () async {
      final sesion = Sesion(_AlmacenQueFalla(), () => ahora);

      await sesion.hidratar();

      expect(sesion.estado, EstadoDeSesion.anonima);
    },
  );

  test('la generación aumenta al abrir y al cerrar', () async {
    final container = crear(_AlmacenEnMemoria(), () => ahora);
    final valores = <int>[];
    final listener = container.listen<int>(
      generacionDeSesionProvider,
      (_, siguiente) => valores.add(siguiente),
      fireImmediately: true,
    );
    final sesion = container.read(sesionProvider);

    await sesion.abrir(acceso: 'access', refresco: 'refresh');
    await Future<void>.delayed(Duration.zero);
    await sesion.cerrar();
    await Future<void>.delayed(Duration.zero);

    expect(valores, [0, 1, 2]);
    listener.close();
    container.dispose();
  });
}
