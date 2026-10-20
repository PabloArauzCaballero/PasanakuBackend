# H6.S3.M8 — comprobaciones widget y estructura (2026-10-16)

Este control complementa las mediciones profile de AVD de `h6-s3-m8-profile-android-2026-10-14.md`; no las reemplaza ni cierra el umbral de rendimiento en un dispositivo Android de gama media.

## Resultado

- `flutter test test/widget/prueba_de_vida_test.dart test/widget/silueta_de_cara_test.dart test/widget/pie_de_prueba_de_vida_test.dart`: PASS, 19 tests, 0 fallos.
- Inspección del trayecto de cámara: `RepaintBoundary` en el preview (`pantalla_prueba_de_vida.dart`), silueta (`pasos_alta/silueta_de_cara.dart`) y contador (`pasos_alta/cuenta_atras_grande.dart`). `rg BackdropFilter` no encuentra coincidencias en esos archivos.
- No hubo cambios de código de rendimiento en esta comprobación. Las mediciones guardadas del AVD son variables y superan 16 ms; no bastan para atribuir el costo a un componente ni representan un teléfono de gama media.

H6.S3.M8 sigue `A MEDIAS` hasta medir el flujo en un Android de gama media y demostrar el criterio del plan.
