# Plan — iOS sin ML Kit: detección de rostro con Vision de Apple

- Fecha: 2026-10-30 · Repos: Pasanaku (`apps/movil`) · Rama: `claude/ios-vision-sin-mlkit-20261030` (base `origin/test`)
- Decisión de Pablo (2026-10-30, por AskUserQuestion): «Vision de Apple en iOS» para cerrar el rojo del CI de simulador iOS.
- Resultado observable: el job «App — navegación en simulador iOS (macOS)» compila la app para el simulador arm64; en el iPhone la prueba de vida sigue detectando la cara y el giro con Vision; en Android no cambia nada (sigue ML Kit).
- Kill-test: `xcodebuild build-for-testing -sdk iphonesimulator` sigue con `ld: framework 'Pods_Runner' not found`, o `flutter build ipa` falla.

## Hallazgos
- Causa del rojo (diagnóstico del agente, reproducido): los pods binarios de ML Kit no traen slice arm64 de simulador → `EXCLUDED_ARCHS[sdk=iphonesimulator*]=arm64` en Pods-Runner → Xcode 26 descarta la dependencia → `ld: framework 'Pods_Runner' not found`. Introducido en b498086f (google_mlkit_face_detection ^0.15.1).
- Único uso: `lib/infraestructura/lector_de_rostro_mlkit.dart`, creado en `camara_en_vivo_plugin.dart:90`.
- El dominio usa de la lectura: rostros, centro, ancho, giro, inclinación (solo en valor absoluto), ojosAbiertos (nullable: `null` no bloquea, `juez_de_pose.dart:100`).
- Vision (`VNDetectFaceRectanglesRequest`, revisión 3, iOS 15+) da caja normalizada, `yaw` y `pitch`; no da ojos abiertos → `null` en iOS.

## Alcance
- IN: copia local de google_mlkit_face_detection 0.15.1 y google_mlkit_commons 0.13.0 solo con Android (MIT) vía `dependency_overrides`; lector Vision en Swift + canal; selección por plataforma en Dart; Podfile/Podfile.lock.
- OUT: lógica del juez de pose, Android, umbrales.
- Ambigüedad: sentido del `yaw` de Vision frente al cuadro espejado de la cámara delantera de iOS. Supuesto: mismo sentido que el EulerY de ML Kit en iOS (se invierte, como hoy). **A confirmar en un iPhone real** (Pablo). Si sale al revés, la pantalla ofrece la foto manual.

## H1 — iOS con Vision
**CA:** Dado un iPhone, cuando hace la prueba de vida, entonces se detecta la cara con Vision y el build de simulador arm64 enlaza.
**DoD:** build simulador `TEST BUILD SUCCEEDED` · `flutter build ipa` OK · tests Dart del lector en verde · `flutter analyze` limpio · regresión sin fallos nuevos.
**Estado:** HECHO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Plugins ML Kit solo Android (copia local + overrides) | `pod install` ya no instala MLKit* | `grep -c MLKit ios/Podfile.lock` → 0 | HECHO (0) |
| H1.S1.M2 | Lector Vision en Swift + canal `bo.aportaya/lector_de_rostro` | compila | build simulador | HECHO |
| H1.S1.M3 | Lector Dart para iOS + selección por plataforma | mapea la respuesta del canal a `LecturaDeRostro` | test unitario con canal simulado (correcto, sin cara, error) | HECHO (3/3) |
| H1.S1.M4 | Build simulador arm64 e ipa | ambos compilan | `xcodebuild build-for-testing` · `flutter build ipa` | HECHO (simulador OK; ipa 202610310124) |
| H1.S1.M5 | Regresión + PRs test/dev | sin fallos nuevos | `flutter test` vs línea base | HECHO (#372/#373 MERGED) |
