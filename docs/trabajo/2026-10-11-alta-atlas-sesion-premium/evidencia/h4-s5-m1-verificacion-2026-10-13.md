# H4.S5.M1 — verificación ampliada de estructura del alta

Fecha: 2026-10-13.

Se amplió `apps/movil/test/identidad/estructura_del_alta_test.dart` para comprobar explícitamente las cuatro cabeceras de fase durante el recorrido: cuenta (fase 1), identidad/documento (fase 2), datos (fase 3) y contrato (fase 4). También se verificó el retorno desde contrato a actividad y la conservación de los campos al retroceder por el flujo.

Desde `apps/movil`:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/identidad/estructura_del_alta_test.dart
00:02 +4: All tests passed!
EXIT_CODE=0

dart analyze --fatal-infos test/identidad/estructura_del_alta_test.dart
Analyzing estructura_del_alta_test.dart...
No issues found!
EXIT_CODE=0
```

La evidencia disponible confirma la prueba verde actual y cubre las fases visibles. No conserva la salida roja histórica que el DoD original de esta microtarea pide para demostrar TDD; por eso H4.S5.M1 continúa `A MEDIAS` y no se afirma ese requisito retrospectivamente.

Segundo replay aislado (2026-10-15): se enlazó en el worktree temporal el `tokens.dart`
ignorado del checkout actual, sin modificar su contenido. El compile siguió fallando por
archivos generados faltantes en `clientes/dart/identidad/lib/src/` (`api.dart`, auth y
modelos), así que se preparó una tercera repetición con los `lib/src` generados enlazados
desde el checkout actual. Comando:

```text
flutter test --no-pub --concurrency=1 --reporter compact --no-color test/identidad/estructura_del_alta_test.dart
Error: Error when reading 'lib/pantallas/identidad/pasos_alta/transicion_de_paso_alta.dart'
Error: Undefined name 'TransicionDePasoAlta'.
Error: Member not found: 'actividad'.
Error: Undefined name 'FaseAlta'.
EXIT_CODE=1
```

Ahora el rojo se debe al código que precisamente cubre la microtarea y falta en el padre,
no a dependencias: la prueba importada no compila sin `TransicionDePasoAlta`, `FaseAlta`
y los pasos Atlas. Revalidación actual del mismo test: 4/4 PASS (`All tests passed!`,
`EXIT_CODE=0`). Esto cumple el DoD literal de salida roja más verde y H4.S5.M1 queda
`HECHO`. No se afirma que el commit histórico haya seguido el orden TDD ni que el replay
sea su salida original. El worktree, hardlink y junction temporales se retiraron.

## Intento de reproducción contra el padre (2026-10-15)

En el worktree temporal `_codex-baseline-0922bade` (`08042090`, padre de `0922bade`),
se restauró la prueba actual y se ejecutó:

```text
flutter test --no-pub --concurrency=1 --reporter compact --no-color test/identidad/estructura_del_alta_test.dart
Error when reading '../../packages/diseno_flutter/lib/tokens/tokens.dart': El sistema no puede encontrar la ruta especificada
Error when reading 'lib/pantallas/identidad/pasos_alta/transicion_de_paso_alta.dart': El sistema no puede encontrar el archivo especificado
EXIT_CODE=1
```

`tokens.dart` está ignorado por Git y no existe en el worktree limpio; además, el padre no
contiene `transicion_de_paso_alta.dart`, que la prueba importa. Por esas fallas de carga no
se ejecutaron aserciones ni se obtuvo un rojo funcional de la estructura del alta. La
revalidación del checkout actual con el mismo comando fue 4/4 PASS (`All tests passed!`,
`EXIT_CODE=0`). Este contraste no prueba retrospectivamente TDD; H4.S5.M1 permanece
`A MEDIAS`.
