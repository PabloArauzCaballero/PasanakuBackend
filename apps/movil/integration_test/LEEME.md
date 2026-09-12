# `integration_test/` — F12.1, andamiaje sin ejecutar

**Bloqueo real, no de este carril**: correr cualquiera de estos archivos requiere dos
paquetes que hoy no están en `apps/movil/pubspec.yaml` — `integration_test` (viene con
el SDK de Flutter) y `patrol` (paquete externo). `pubspec.yaml` está fuera de mi
alcance (`arrancar-carril` §3: "NO TOCÁS … pubspec.yaml pubspec.lock"; agregar una
dependencia nueva es un micro-PR al catálogo, §7, no una decisión de este carril).

**Lo que hace falta, exacto, para que esto compile y corra:**

```yaml
# apps/movil/pubspec.yaml, bajo dev_dependencies:
  integration_test:
    sdk: flutter
  patrol: ^3.15.0   # o la versión vigente en pub.dev al momento del micro-PR
```

y en la raíz del proyecto Flutter, generar `patrol.yaml` con
`dart run patrol_cli:main bootstrap` (agrega hooks nativos en `android/` e `ios/` que
tampoco son míos: `arrancar-carril` no me da propiedad sobre esas carpetas).

**Qué SÍ hice**: los siete archivos de este directorio son el recorrido completo — con
las claves (`Key(...)`), rutas (`go_router`) y pasos reales que existen hoy en
`apps/movil/lib/` — pero usan `patrolTest(...)` y `$` (el binding de Patrol) que no
resuelven sin el paquete. Compilarlos hoy da `Target of URI doesn't exist: 'package:patrol/patrol.dart'`.
Quien haga el micro-PR de pubspec puede correrlos tal cual, o soy yo en una vuelta
siguiente si me dan permiso sobre `pubspec.yaml` puntualmente para esto.

**Comandos que correrían, una vez resuelto lo anterior:**

```bash
flutter test integration_test/alta_y_billetera_test.dart -d emulator-5554   # P3, emulador acelerado
flutter test integration_test/                                              # todo el directorio
patrol test --target integration_test/sin_conexion_test.dart -d <dispositivo-fisico-gama-baja>
```

**Lo que sí corrí en este sandbox**: hay un emulador Android real disponible
(`emulator-5554`, Android 17/API 37, ver `flutter devices`). No es "gama baja" — es la
imagen de referencia del SDK, sin las limitaciones de CPU/RAM del parque real de
Bolivia que pide F12.3 — así que aunque compilara, correr ahí NO sustituye la medición
en dispositivo físico de gama baja que pide el gate. Ver `planes/informes/carril-F12.md`.
