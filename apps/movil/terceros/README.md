# Plugins de terceros, copiados

| Paquete | Versión publicada | Licencia | Qué se cambió |
|---|---|---|---|
| `google_mlkit_face_detection` | 0.15.1 (pub.dev) | MIT (`LICENSE`) | sin `ios/` ni `example/`; `pubspec.yaml` declara solo `android` |
| `google_mlkit_commons` | 0.13.0 (pub.dev) | MIT (`LICENSE`) | ídem |

Por qué: los pods binarios de ML Kit no tienen slice arm64 para el simulador de iOS y
rompían `xcodebuild -sdk iphonesimulator` (`ld: framework 'Pods_Runner' not found`).
En iOS la detección de rostro la hace Vision de Apple (`ios/Runner/LectorDeRostroVision.swift`).
El código Dart y Android es el publicado, sin cambios. Para actualizar: copiar la versión
nueva de `~/.pub-cache/hosted/pub.dev/`, borrar `ios/` y `example/`, y quitar `ios:` de
`flutter.plugin.platforms`.
