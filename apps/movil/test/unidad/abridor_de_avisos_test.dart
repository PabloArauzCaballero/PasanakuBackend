import 'dart:async';

import 'package:aportaya_movil/navegacion/abridor_de_avisos.dart';
import 'package:flutter_test/flutter_test.dart';

const _uuid = '3f2a9c1e-8b47-4d21-9a66-0c5e7d1b2a34';
final _hash = 'ab' * 32;

void main() {
  late StreamController<String> toques;
  late List<String> abiertas;
  late DateTime reloj;
  late AbridorDeAvisos abridor;

  setUp(() {
    toques = StreamController<String>();
    abiertas = [];
    reloj = DateTime.utc(2026, 9, 30, 12);
    abridor = AbridorDeAvisos(
      toques: toques.stream,
      abrir: abiertas.add,
      ahora: () => reloj,
    )..iniciar();
  });

  tearDown(() async {
    await abridor.detener();
    await toques.close();
  });

  Future<void> tocar(String texto) async {
    toques.add(texto);
    await Future<void>.delayed(Duration.zero);
  }

  test('correcto: un toque con destino válido abre esa ruta', () async {
    await tocar('aportaya://billetera');
    expect(abiertas, ['/billetera/inicio']);
  });

  test('correcto: la invitación válida abre unirse', () async {
    await tocar('aportaya://unirse/$_uuid.$_hash');
    expect(abiertas, ['/pasanaku/unirse/$_uuid.$_hash']);
  });

  test(
    'límite: el mismo toque dos veces dentro de la ventana abre una vez',
    () async {
      await tocar('aportaya://billetera');
      reloj = reloj.add(const Duration(milliseconds: 500));
      await tocar('/billetera/inicio');
      expect(abiertas, ['/billetera/inicio']);
    },
  );

  test('límite: pasada la ventana el mismo destino vuelve a abrirse', () async {
    await tocar('aportaya://billetera');
    reloj = reloj.add(const Duration(seconds: 3));
    await tocar('aportaya://billetera');
    expect(abiertas, hasLength(2));
  });

  test('límite: destinos distintos seguidos abren ambos', () async {
    await tocar('aportaya://billetera');
    await tocar('aportaya://notificaciones');
    expect(abiertas, ['/billetera/inicio', '/notificaciones/bandeja']);
  });

  test('inválido: un destino externo o ajeno no abre nada', () async {
    await tocar('https://evil.com');
    await tocar('//evil.com');
    await tocar('/identidad/baja');
    await tocar('Tu aporte fue acreditado');
    expect(abiertas, isEmpty);
  });

  test('inválido: un error del flujo no mata la escucha', () async {
    toques.addError(StateError('falló el canal'));
    await Future<void>.delayed(Duration.zero);
    await tocar('aportaya://billetera');
    expect(abiertas, ['/billetera/inicio']);
  });

  test('límite: tras detener no se abre nada más', () async {
    await abridor.detener();
    await tocar('aportaya://billetera');
    expect(abiertas, isEmpty);
  });
}
