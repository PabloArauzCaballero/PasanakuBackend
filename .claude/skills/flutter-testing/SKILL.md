---
name: flutter-testing
description: Pruebas de la app Flutter — unit tests de lógica, widget tests (`testWidgets`, `WidgetTester`, finders, `pump`/`pumpAndSettle`), golden tests con `matchesGoldenFile`, dobles de dependencias e integración con el paquete `integration_test`. Usar al escribir o arreglar cualquier test de la app móvil, al decidir qué probar en qué nivel, o al proteger el tema y componentes contra regresiones. Es un gate: sin evidencia de corrida no se declara probado.
allowed-tools: Read Grep Glob Bash Edit Write
effort: high
---

# Pruebas — Flutter

Tres niveles, del más barato al más caro: **unit** (lógica pura), **widget** (un widget o pantalla
en un entorno de prueba) e **integración** (la app real sobre un dispositivo/emulador). Cubrí la
mayor parte abajo. Complementa a `unit-testing` (FIRST, dobles, un comportamiento por test).

## 1. Unit — lógica y estado

- Notifiers, casos de uso, mapeos y utilidades se prueban sin `WidgetTester`. Con Riverpod, un
  `ProviderContainer` con `overrides` (ver `flutter-state-architecture`).
- Deterministas: reloj y aleatoriedad inyectados; nada de `DateTime.now()` real dentro de la lógica.

## 2. Widget test

```dart
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('muestra el título y el mensaje', (tester) async {
    await tester.pumpWidget(const MaterialApp(
      home: Scaffold(body: MyCard(title: 'Aporte', message: 'Hoy 15:00')),
    ));

    expect(find.text('Aporte'), findsOneWidget);
    expect(find.text('Hoy 15:00'), findsOneWidget);

    await tester.tap(find.byIcon(Icons.close));
    await tester.pump(); // un frame; usa pumpAndSettle() para esperar animaciones
    expect(find.byType(MyCard), findsNothing);
  });
}
```

- Finders orientados a lo que ve el usuario: `find.text`, `find.byIcon`, `find.bySemanticsLabel`;
  `find.byKey` para casos puntuales. Evitá acoplar el test a la estructura interna del árbol.
- Matchers: `findsOneWidget`, `findsNothing`, `findsWidgets`, `findsNWidgets(n)`.
- `pump()` avanza un frame; `pumpAndSettle()` espera a que terminen las animaciones. No uses
  esperas por tiempo arbitrarias.
- Envolvé el widget bajo prueba con lo que necesite (`MaterialApp`, `ProviderScope` con overrides,
  `Localizations`) — solo eso, no toda la app.

## 3. Golden tests

- `matchesGoldenFile` compara un render contra una imagen de referencia. Útil para el tema y
  componentes del design system.

```dart
await expectLater(find.byType(ContributionCard), matchesGoldenFile('goldens/contribution_card.png'));
```

- Generá/actualizá las referencias con `flutter test --update-goldens`, y **revisá el diff** antes
  de aceptar (misma disciplina que `visual-regression-testing`). Estabilizá fuentes y datos; el
  render de fuentes puede variar entre entornos, así que fijá el runner (idealmente CI).

## 4. Dobles

- Inyectá dependencias por provider/constructor y reemplazalas por fakes/mocks en el test. Preferí
  **fakes** propios para repositorios; mocks solo para verificar interacciones. Ver `unit-testing`.

## 5. Integración

- El paquete `integration_test` corre la app real sobre dispositivo/emulador para flujos críticos
  (login, calendarior). Es lento: pocos, sobre los caminos que más importan. Datos sintéticos, nunca
  datos reales (ver `test-data-management`, `data-privacy-financial`).

## Qué probar en cada nivel
| Nivel | Cubre | Ejemplos |
|---|---|---|
| Unit | Lógica, estado, mapeos | notifier, validaciones, `copyWith`, contraste del tema |
| Widget | Render e interacción de un widget/pantalla | estados carga/vacío/error, tap, formulario |
| Golden | Apariencia estable | componentes del design system, tema claro/oscuro |
| Integración | Flujo E2E en la app real | login → calendario → registrar aporte |

## Evidencia / DoD
No declares "probado" sin pegar la salida literal:
- `flutter test` (o el subconjunto afectado) con el resumen de suites/tests **en verde**.
- Para golden: confirmación de que el diff fue revisado, no solo `--update-goldens` a ciegas.
- Para integración: comando y resultado sobre qué dispositivo/emulador.
- Declará qué quedó **sin cubrir**. Ver `evidence-and-verification`.

## Checklist
- [ ] Lógica en unit; UI en widget; apariencia en golden; flujo crítico en integración.
- [ ] Finders orientados al usuario; sin esperas por tiempo arbitrarias.
- [ ] Golden con datos/fuentes estables y diff revisado antes de aceptar.
- [ ] Dependencias inyectadas y reemplazadas por dobles; datos sintéticos.
- [ ] Salida literal de la corrida pegada; "no cubierto" declarado.
