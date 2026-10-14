# H8.S2.M1 — tokens de movimiento Flutter

- Fecha: 2026-10-12.
- Rama: `codex/alta-atlas-sesion-premium-2026-10-11`.
- Decisión de stack: `packages/diseno_flutter/lib/atomos/movimiento.dart` es la fuente canónica de duración, curva y resorte para Flutter. `MovimientoDeInterfaz` no existe en los tokens Flutter de este checkout; ese nombre pertenece a la referencia de otro stack y no se duplicó.
- Cambio: se añadieron nombres semánticos para las duraciones estéticas que aún estaban escritas en pantallas: entrada/escalón de portada, salida de candado, destello de captura y cambio del contador; la silueta reutiliza `Movimiento.micro`. Los valores se conservaron para no anticipar los cambios visuales de H8.S3.M2 ni alterar la secuencia H6 de captura.
- Ámbito: no se tocaron los tiempos que expresan reglas/cadencias de dominio ni los `Duration(seconds: ...)` de espera/timeout.

## Verificación de literales

Comando: `rg -n "Duration\(milliseconds:" apps/movil/lib/pantallas`

Salida restante — todos son dominio o temporización operativa, no estética:

```text
apps/movil/lib/pantallas/identidad/controlador_prueba_de_vida.dart:78:      const Duration(milliseconds: 100),
apps/movil/lib/pantallas/identidad/dominio/ciclo_de_pose.dart:4:const respiroDePose = Duration(milliseconds: 1600);
apps/movil/lib/pantallas/identidad/dominio/ciclo_de_pose.dart:10:const avisoDeFotoTomada = Duration(milliseconds: 1100);
apps/movil/lib/pantallas/identidad/dominio/cu02_subir_foto.dart:40:      Duration(milliseconds: 45000 + (bytes / 100 * 1000).round());
```

## Pruebas dirigidas

`dart format lib/pantallas/identidad/pantalla_portada.dart lib/pantallas/identidad/capa_de_bloqueo.dart lib/pantallas/identidad/pantalla_prueba_de_vida.dart lib/pantallas/identidad/pasos_alta/cuenta_atras_grande.dart lib/pantallas/identidad/pasos_alta/silueta_de_cara.dart`

```text
Formatted 5 files (0 changed).
```

`flutter test --concurrency=1 --reporter expanded test/widget/portada_test.dart test/widget/prueba_de_vida_test.dart test/widget/silueta_de_cara_test.dart` (desde `apps/movil`)

```text
00:11 +23: All tests passed!
```

La suite confirma el flujo de portada y prueba de vida, además del comportamiento visual de la silueta y el caso de movimiento reducido. No se ejecutó build Android/Gradle.

Skills aplicadas: `frontend-motion`, `flutter-development`, `flutter-theming`, `frontend-accessibility`, `accessibility-testing`, `flutter-state-architecture`, `frontend-performance`, `pasanaku-ui-componentes` y `pasanaku-movimiento`.
