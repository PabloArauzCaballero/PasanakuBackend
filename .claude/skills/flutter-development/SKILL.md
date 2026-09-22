---
name: flutter-development
description: Estándar de la casa para escribir Flutter/Dart — composición de widgets, stateless vs stateful, uso de `const` y rendimiento de rebuilds, keys, layout (Row/Column/Flex/Expanded/Slivers), estructura por feature, null safety y lints. Usar al crear o revisar cualquier widget, pantalla o servicio de la app móvil, al diagnosticar rebuilds de más o jank, o al decidir cómo partir un widget grande. El estado se maneja en `flutter-state-architecture` y el tema en `flutter-theming`.
---

# Flutter — estándar de la casa

La UI es una función del estado, construida por **composición** de widgets pequeños. Preferí
componer widgets antes que extender clases; un método `build` es una descripción declarativa,
no un procedimiento.

## 1. Composición sobre herencia

- Widgets chicos y con una responsabilidad; se combinan, no se heredan. Un `build` que pasa de
  la pantalla es señal de extraer sub-widgets.
- Extraé a un **widget** propio (clase), no a un método `_buildX()` que devuelve `Widget`: la
  clase permite `const`, tiene su propio contexto y no fuerza rebuilds del padre.

```dart
// ❌ método helper: rebuild sube al padre, no puede ser const
Widget _buildAvatar() => CircleAvatar(child: Text(initials));

// ✅ widget propio, const donde se pueda
class Avatar extends StatelessWidget {
  const Avatar({super.key, required this.initials});
  final String initials;
  @override
  Widget build(BuildContext context) => CircleAvatar(child: Text(initials));
}
```

## 2. Stateless vs stateful

- `StatelessWidget` por defecto. Usá `StatefulWidget` solo para estado efímero y local del
  widget (animaciones, foco, expandido/colapsado, controllers).
- El estado de negocio/datos NO vive en `State`: va en la capa de estado (ver `flutter-state-architecture`).
- Liberá recursos en `dispose()` (controllers, focus nodes, subscriptions). Fugarlos es un bug.

## 3. `const` y rendimiento de rebuilds

- Marcá `const` todo constructor de widget que no dependa de datos variables. Un subárbol `const`
  no se reconstruye. Activá el lint `prefer_const_constructors`.
- El costo no es el `build`, es reconstruir subárboles que no cambiaron. Aislá lo que cambia:
  subí el estado lo mínimo y dejá el resto `const`.
- No hagas trabajo caro en `build` (I/O, parseo, `MediaQuery` mal usado): `build` puede correr
  muchas veces por segundo.

## 4. Keys

- Usá `Key` cuando reordenás/insertás/borrás elementos de una lista de widgets con estado, para
  que Flutter preserve el estado correcto. `ValueKey(id)` con un id estable del dominio.
- No pongas keys "por las dudas": una key incorrecta rompe la preservación de estado.

## 5. Layout

- `Row`/`Column` + `Expanded`/`Flexible` para repartir espacio; `SizedBox` para espaciar (no
  `Container` vacío). `Padding` para márgenes internos.
- Listas largas: `ListView.builder`/`GridView.builder` (perezosas), nunca `ListView(children: [...])`
  con cientos de hijos. Para efectos de scroll complejos, `CustomScrollView` + slivers.
- Errores clásicos: `Column` con hijo que quiere altura infinita (envolver en `Expanded`);
  `Row` que desborda (`Flexible`/`Wrap`); `unbounded height/width` (leé el mensaje, dice el eje).

## 6. Estructura por feature

- Carpetas por feature (`features/calendario/…`), no por tipo. Dentro: `presentation/`, `application/`
  (estado/casos de uso), `data/` (repos, DTOs). Compartido en `core/`/`shared/`.
- Un widget = un archivo cuando crece. Nombres descriptivos, sin sufijos redundantes.
- Aplicá `native-code-patterns`: mirá 2–3 features existentes y copiá su forma antes de crear una nueva.

## 7. Dart / null safety / lints

- Null safety estricto: evitá `!` para callar al compilador; modelá la ausencia con tipos y
  `?.`/`??`. Preferí `late final` a nullable mutable cuando la inicialización es diferida y segura.
- Clases de datos inmutables (`final` en los campos, constructores `const`); considerá
  `copyWith` para variaciones. (En este stack la inmutabilidad la refuerza `flutter-state-architecture`.)
- Activá `flutter_lints`/`package:lints` y tratá los warnings como errores en CI. `dart format` obligatorio.
- `async`/`await` con manejo de errores; no dejes futures sin await ni excepciones sin capturar.

## Checklist
- [ ] Widgets extraídos como clases `const`, no como métodos `_buildX`.
- [ ] `StatefulWidget` solo para estado efímero; `dispose()` libera todo lo que abrió.
- [ ] `const` aplicado donde el subárbol no depende de datos variables.
- [ ] Listas largas con `.builder`; ningún desborde ni "unbounded" sin resolver.
- [ ] Estructura por feature; nada de `any` implícito ni `!` para silenciar nulos.
- [ ] `dart format` y `flutter analyze` limpios; lints como error en CI.
