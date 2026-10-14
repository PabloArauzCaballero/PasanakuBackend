import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/subida_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/fila_de_subida.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('la fila lenta comunica el retraso y permite cancelar', (
    tester,
  ) async {
    var cancelaciones = 0;
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          body: FilaDeSubida(
            cara: CaraDelCarril.anverso,
            estado: const SubidaDeUnaCara(estado: EstadoDeUnaSubida.lenta),
            ruta: '/privado/no-se-muestra.jpg',
            onCancelar: () => cancelaciones++,
            onReintentar: () {},
            onRepetir: () {},
          ),
        ),
      ),
    );

    expect(
      find.text(
        'Está tardando más de lo normal. Podés esperar o cancelar y volver a intentar.',
      ),
      findsOneWidget,
    );
    expect(find.text('Subiendo el anverso…'), findsOneWidget);
    expect(find.text('/privado/no-se-muestra.jpg'), findsNothing);
    await tester.tap(find.text('Cancelar la subida'));
    expect(cancelaciones, 1);
  });

  testWidgets('la fila fallida ofrece reintento y repetir la cara', (
    tester,
  ) async {
    var reintentos = 0;
    var repeticiones = 0;
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          body: FilaDeSubida(
            cara: CaraDelCarril.perfilDerecho,
            estado: const SubidaDeUnaCara(
              estado: EstadoDeUnaSubida.fallida,
              error: 'No se pudo enviar la foto.',
              trazaId: 'traza-1',
            ),
            ruta: null,
            onCancelar: () {},
            onReintentar: () => reintentos++,
            onRepetir: () => repeticiones++,
          ),
        ),
      ),
    );

    expect(find.text('No se pudo enviar la foto.'), findsOneWidget);
    expect(find.text('Referencia: traza-1'), findsOneWidget);
    await tester.tap(find.text('Reintentar'));
    await tester.tap(find.text('Repetir foto del perfil derecho'));
    expect(reintentos, 1);
    expect(repeticiones, 1);
  });
}
