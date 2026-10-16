import 'package:aportaya_movil/navegacion/enlaces_profundos.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('aportaya://unirse/ABC123 abre la ruta de unirse a un pasanaku', () {
    expect(
      rutaInternaDesde(Uri.parse('aportaya://unirse/ABC123')),
      '/pasanaku/unirse/ABC123',
    );
  });

  test('aportaya://billetera sin resto va al inicio de billetera', () {
    expect(
      rutaInternaDesde(Uri.parse('aportaya://billetera')),
      '/billetera/inicio',
    );
  });

  test('un esquema distinto de aportaya no se traduce', () {
    expect(rutaInternaDesde(Uri.parse('https://aportaya.bo/unirse/X')), isNull);
  });

  test('un host sin traducción conocida no se traduce', () {
    expect(rutaInternaDesde(Uri.parse('aportaya://desconocido/x')), isNull);
  });
}
