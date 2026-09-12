# `integration_test/` — micro-PR aplicado, ejecución real bloqueada por el JDK del sandbox

**Actualización posterior a F12 (troncal, mismo día).** El micro-PR que este archivo
pedía ya se hizo: `integration_test` y `patrol: ^3.15.0` están en
`apps/movil/pubspec.yaml`, `flutter pub get` resuelve limpio y `flutter analyze
--fatal-infos` da **"No issues found!"** sobre los siete archivos — dos bugs reales que
solo aparecían al compilar contra el paquete real se corrigieron en el mismo micro-PR:
imports faltantes de `flutter_test` y `flutter/material.dart` en varios archivos, y en
`notificacion_test.dart` la API real de Patrol 3.20.0 es `tap(Selector)` y
`getNativeViews` devuelve `Future<List<NativeView>>` (hay que `await`), no
`openNotification(index:)` como decía el borrador original.

**Lo que queda bloqueado, ahora sí de infraestructura real, no de permiso sobre
archivos**: `patrol bootstrap` no existe en `patrol_cli` 4.x (el comando se eliminó);
con la versión compatible con `patrol` 3.20.0 según la tabla oficial (`patrol_cli`
3.11.0), `patrol test` sí arranca — compila la app, levanta Gradle — pero el Gradle
interno de esa versión de `patrol_cli` falla con:

```
BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_'
Unsupported class file major version 70
```

Es JDK 26 (instalado en esta Mac) contra una herramienta que no lo soporta todavía
(major version 70 = Java 26; el Gradle que trae `patrol_cli` 3.11.0 espera JDK 17 o 21,
como el propio `patrol doctor` advierte: *"You are using Java 26.0.2 which can cause
issues on Android"*). No es un bug del proyecto: es una versión de JDK del sistema más
nueva que la herramienta de terceros soporta. Bajar el JDK por defecto de todo el Mac
para esto es un cambio de infraestructura ancho, fuera de lo que corresponde arreglar
acá — lo resuelve quien mantenga el parque de máquinas, instalando un JDK 17/21 y
apuntando `patrol_cli` a él (`JAVA_HOME` específico para esta herramienta, sin tocar el
JDK global).

**Comandos que correrían, con un JDK 17 o 21 disponible:**

```bash
export JAVA_HOME=<ruta a un JDK 17 o 21>
dart pub global activate patrol_cli 3.11.0
patrol test --target integration_test/sin_conexion_test.dart -d <dispositivo>
```

Hay un emulador Android real disponible en este Mac (`Pixel_4`, API 37) y funcionando
—se usó para confirmar el bloqueo real de JDK, no uno inventado— pero, aunque
compilara, correr ahí **no sustituye** la medición en un dispositivo físico de gama
baja que pide F12.3: es la imagen de referencia del SDK, no el parque real de Bolivia.
Ver `planes/informes/carril-F12.md`.
