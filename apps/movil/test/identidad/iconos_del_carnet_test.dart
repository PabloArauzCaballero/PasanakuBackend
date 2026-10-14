import 'package:aportaya_movil/pantallas/identidad/pasos_alta/riel_de_capturas.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('anverso y reverso usan iconos de carnet, no de tarjeta bancaria', () {
    expect(iconoDeCara(CaraDelCarril.anverso), Icons.badge_outlined);
    expect(iconoDeCara(CaraDelCarril.reverso), Icons.flip_to_back_rounded);
  });
}
