import 'dart:convert';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/app_real.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../comun.dart';
import 'adaptador_por_contrato.dart';
import 'inventario.dart';

/// Matriz de capturas del rediseño bancario (PLAN.md §0.1): 3 viewports × 2 temas × estados.
/// Se corre con `flutter test test/capturas/rediseno --update-goldens --dart-define=FASE=antes|despues`
/// y escribe en `docs/trabajo/2026-10-21-rediseno-bancario/evidencia/<fase>/<flujo>/`.
/// NO son goldens de comparación: son evidencia para mirar.
const fase = String.fromEnvironment('FASE', defaultValue: 'antes');

/// Solo los flujos pedidos (`--dart-define=FLUJOS=billetera,grupos`); vacío = todos.
const flujosPedidos = String.fromEnvironment('FLUJOS');

const viewportsDeCaptura = {
  '360': Size(360, 800),
  '412': Size(412, 915),
  '600': Size(600, 960),
};

const temasDeCaptura = {'claro': Brightness.light, 'oscuro': Brightness.dark};

const _salida =
    '../../../../../docs/trabajo/2026-10-21-rediseno-bancario/evidencia';

bool flujoPedido(String flujo) =>
    flujosPedidos.isEmpty || flujosPedidos.split(',').contains(flujo);

String rutaDeCaptura(
  PantallaDeCaptura p,
  EstadoDeCaptura estado,
  String viewport,
  String tema,
) =>
    '$_salida/$fase/${p.flujo}/${p.nombre}__${estado.name}__${viewport}__$tema.png';

/// Monta la app con su enrutador real en [p.ruta], con un Dio que contesta por contrato.
/// Devuelve el adaptador (para el registro) y una función que saca la pantalla como lo
/// haría la persona —navegando— antes de desmontar la app entera. `salir` devuelve las
/// excepciones que la pantalla tira al irse: van al registro como hallazgos.
Future<(AdaptadorPorContrato, Future<List<String>> Function())> montarRuta(
  WidgetTester t,
  PantallaDeCaptura p,
  EstadoDeCaptura estado,
  Size viewport,
  Brightness brillo,
) async {
  t.view.physicalSize = viewport;
  t.view.devicePixelRatio = 1;
  addTearDown(t.view.reset);
  _simularPermisos(estado);

  final sesion = p.sesion == SesionDeCaptura.activa
      ? await _sesionConUsuario()
      : await sesionAnonimaDePrueba();
  final adaptador = AdaptadorPorContrato(estado);
  final dio = Dio(BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'))
    ..httpClientAdapter = adaptador;
  instalarInterceptores(dio, sesion);
  // Una ruta que se abre con `extra` arranca en la casa de su sesión y navega con él.
  final enrutador = crearEnrutador(
    inicial: p.extra == null
        ? p.ruta
        : (p.sesion == SesionDeCaptura.activa
              ? '/billetera/inicio'
              : '/portada'),
    sesion: sesion,
  );

  await t.pumpWidget(
    ProviderScope(
      retry: (r, e) => null,
      overrides: [
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        sesionProvider.overrideWithValue(sesion),
        dioProvider.overrideWithValue(dio),
      ],
      child: MaterialApp.router(
        debugShowCheckedModeBanner: false,
        locale: localeDeAportaYa,
        supportedLocales: localesDeAportaYa,
        localizationsDelegates: delegadosDeLocalizacionDeAportaYa,
        theme: temaDesde(Tokens.claro, Brightness.light),
        darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
        themeMode: brillo == Brightness.dark ? ThemeMode.dark : ThemeMode.light,
        routerConfig: enrutador,
      ),
    ),
  );
  if (p.extra != null) {
    await t.pump();
    enrutador.go(p.ruta, extra: p.extra);
  }
  await _asentarAcotado(t, estado);
  Future<List<String>> salir() async {
    final halladas = <String>[];
    Future<void> paso(Future<void> Function() accion) async {
      try {
        await accion();
      } catch (e) {
        halladas.add('$e'.split('\n').first);
      }
      final e = t.takeException();
      if (e != null) halladas.add('$e'.split('\n').first);
    }

    if (!p.saleDesmontando) {
      await paso(() async {
        enrutador.go('/portada');
        await t.pump();
      });
      await paso(() => t.pump(const Duration(milliseconds: 500)));
    }
    await paso(() => t.pumpWidget(const SizedBox.shrink()));
    await paso(() => t.pump(const Duration(seconds: 2)));
    return halladas;
  }

  return (adaptador, salir);
}

/// En `flutter test` no hay plugins nativos: el permiso del sistema se contesta según el
/// estado (datos → concedido, vacío → denegado, error → bloqueado para siempre) y la
/// cámara no lista dispositivos. Así quedan fotografiados los tres estados de permiso.
void _simularPermisos(EstadoDeCaptura estado) {
  // Índices de `PermissionStatus`: denied 0, granted 1, permanentlyDenied 4.
  final permiso = switch (estado) {
    EstadoDeCaptura.vacio => 0,
    EstadoDeCaptura.error => 4,
    _ => 1,
  };
  final mensajero =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  const permisos = MethodChannel('flutter.baseflow.com/permissions/methods');
  mensajero.setMockMethodCallHandler(permisos, (llamada) async {
    return switch (llamada.method) {
      'requestPermissions' => {
        for (final p in llamada.arguments as List<Object?>) p: permiso,
      },
      'checkPermissionStatus' => permiso,
      'shouldShowRequestPermissionRationale' => false,
      _ => null,
    };
  });
  const camara = MethodChannel('plugins.flutter.io/camera');
  mensajero.setMockMethodCallHandler(camara, (llamada) async => <Object>[]);
  addTearDown(() {
    mensajero.setMockMethodCallHandler(permisos, null);
    mensajero.setMockMethodCallHandler(camara, null);
  });
}

/// Sesión con un JWT SINTÉTICO (sin firma): `sub` y el permiso de billetera, como el de
/// una persona verificada. Las pantallas que leen `usuarioIdActual` tienen con qué.
Future<Sesion> _sesionConUsuario() async {
  String parte(Map<String, Object> m) =>
      base64Url.encode(utf8.encode(jsonEncode(m))).replaceAll('=', '');
  final acceso = [
    parte({'alg': 'none', 'typ': 'JWT'}),
    parte({
      'sub': usuarioDeCaptura,
      'permisos': ['BILLETERA_VER'],
      'exp': 1893456000,
    }),
    'firma-sintetica',
  ].join('.');
  final sesion = Sesion(
    AlmacenEnMemoria(),
    () => DateTime.utc(2026, 10, 11, 12),
  );
  await sesion.abrir(acceso: acceso, refresco: 'refresco-sintetico');
  return sesion;
}

/// Como `asentar`, pero con tope: una pantalla con un pulso o un esqueleto en bucle nunca
/// «se asienta», y la captura igual tiene que salir.
Future<void> _asentarAcotado(WidgetTester t, EstadoDeCaptura estado) async {
  await t.pump();
  await t.pump(const Duration(milliseconds: 250));
  if (estado == EstadoDeCaptura.cargando) return;
  for (var i = 0; i < 30 && t.binding.hasScheduledFrame; i++) {
    await t.pump(const Duration(milliseconds: 100));
  }
}
