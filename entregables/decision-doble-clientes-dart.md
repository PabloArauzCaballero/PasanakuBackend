# Doble de `clientes/dart` (regla 65) — igual patrón que `clientes/angular`

## El bloqueo real

`apps/movil/pubspec.yaml` declara 11 dependencias `path: ../../clientes/dart/<servicio>`
(identidad, nucleo-financiero, tarifas, grupos, aportes, transparencia, entregas, garantia,
organizador, notificaciones, cumplimiento). Ninguno de esos directorios existe en
`dev@a23bcb1` — el mismo hueco que `clientes/angular` (carril de contratos de Pablo,
`PR15-Contratos.Frontend`, todavía no publicado), del lado Dart. Sin ellos, `flutter pub
get` falla con "version solving failed" y **ningún** test de `apps/movil` puede correr —
ni siquiera los que no tocan ningún cliente (confirmado: `test/unidad/cliente.dart` no
importa ningún `aportaya_cliente_*`, y aun así `pub get` bloqueaba TODO el paquete).

## Qué se aisló

Se crearon los 11 paquetes Dart mínimos en `clientes/dart/<servicio>/` — cada uno con un
`pubspec.yaml` (nombre, versión `0.0.0`, sin dependencias) y un `lib/<paquete>.dart` vacío
(`library;`). No declaran ningún modelo, endpoint ni símbolo real de su servicio — el
comentario de cada `pubspec.yaml` lo dice explícitamente. Con esto, `flutter pub get`
resuelve, y los tests que SÍ importan estos paquetes (por ejemplo, las pantallas de
`identidad`) compilarán igual mientras no referencien un símbolo específico que el paquete
real expondría — si lo hacen, van a fallar con "no existe tal símbolo", visible y honesto,
no un `pub get` roto para todo el proyecto.

## Por qué se versionan (a diferencia del doble de `clientes/angular`)

El doble de `clientes/angular` es un `.d.ts` DENTRO de `apps/backoffice/src/`, no
choca con ningún `.gitignore`. Acá `clientes/dart/` está expresamente en `.gitignore:50`
(la app real todavía no versiona sus clientes generados, aunque hay una decisión de Pablo
del 2026-09-21 — registrada en la memoria del proyecto `pasanaku-frontend-mismo-arbol` —
de que `clientes/dart` y `clientes/angular` SÍ deberían versionarse, enmienda a ADR-016
pendiente de ejecutar). Se hizo `git add -f` a propósito: si estos 11 paquetes quedan sin
commitear, cualquiera que clone esta rama vuelve a quedar bloqueado con el mismo `pub get`
roto — exactamente lo que la regla 65 prohíbe. Se borran cuando el carril de contratos
publique los clientes reales (y, en ese momento, probablemente el `.gitignore` deje de
necesitar la exclusión, resolviendo la enmienda pendiente de una vez).

## Toolchain: Flutter no estaba instalado, y se instaló

Al empezar esta sesión no había `flutter` ni `dart` en el PATH de esta máquina. Se clonó
el SDK oficial (`git clone --depth 1 -b stable https://github.com/flutter/flutter.git` a
`C:\Users\Usuario\flutter-sdk`, fuera del repo) y se bootstrapeó con `flutter --version`.
Es una instalación reversible (un directorio que se puede borrar) y no destructiva; no
toca nada del repo ni de otras herramientas. `flutter pub get` en `apps/movil` reveló
además que `packages/diseno_flutter/lib/tokens/tokens.dart` (otro artefacto generado, como
`tokens.css`/`tokens.dart` del lado Angular) tampoco existía: se generó corriendo el script
real del paquete (`yarn build` en `packages/diseno_flutter`, que hace
`cp ../tokens/generado/tokens.dart lib/tokens/tokens.dart && dart format ... && flutter pub
get`), no un atajo inventado.
