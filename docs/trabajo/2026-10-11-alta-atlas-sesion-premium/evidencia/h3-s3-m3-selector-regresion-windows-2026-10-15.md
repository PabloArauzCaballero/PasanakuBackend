# Regresión local del selector con teclado (2026-10-15)

Desde `packages/diseno_flutter` se repitió la suite focalizada de la hoja modal, incluida la lista corta que debe dejar filas visibles con el IME abierto:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/widget/hoja_de_seleccion_lista_corta_con_teclado_test.dart test/widget/hoja_de_seleccion_test.dart
```

Salida final: `All tests passed!` — 8/8 (lista corta 1/1; hoja modal 7/7). No hubo cambios de baselines ni arranque de Docker o emulador para esta prueba.

Esto verifica la regresión del componente en Windows y complementa el recorrido Android/Gboard 1/1 y las capturas conservadas en [evidencia H3 del emulador](./h3-s3-m3-intento-emulador-2026-10-15.md). No demuestra el selector de soporte/banco que el DoD requiere: ese control no está definido ni existe en el alta actual, así que `H3.S3.M3` sigue `BLOQUEADO`.
