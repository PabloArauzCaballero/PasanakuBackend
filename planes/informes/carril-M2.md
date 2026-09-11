---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril M2 — billetera y alianzas (Flutter)"
ola: 2
fase: 4
mundo: Flutter
modulo: apps/movil/lib/pantallas/billetera | apps/movil/lib/pantallas/alianzas
rama: pablo/feature/carril-M2-billetera
estado: en curso
---

# Carril M2 — billetera y alianzas

**Fase** F4 · **Mundo** Flutter · **Casos de uso** CU-10–19, 30–33, 57 · **Puesto** M2

> Este informe lo escribe solo este carril. Se corta el trabajo en un punto seguro
> (nada roto, todo lo entregado compila, analiza y pasa sus pruebas) porque el
> alcance completo de la ficha —ocho casos de uso de dinero más el calendario de
> ciclo y los vales de alianzas— no entra en una sola sesión sin fabricar resultados.
> Lo que sigue es exacto sobre qué se hizo y qué no.

## Qué quedó hecho

| CU | Pantalla | Ruta | Dominio | Prueba |
| --- | --- | --- | --- | --- |
| CU-10 Recargar saldo | `pantalla_recargar.dart` | `/billetera/recargar` | `cu10_recargar_saldo.dart` | `test/widget/pantalla_recargar_test.dart` (doble toque) |
| CU-11 Retirar saldo | `pantalla_retirar.dart` | `/billetera/retirar` | `cu11_retirar_saldo.dart` | cubierta por `flutter analyze` + `test:front`; sin widget test propio (ver Huecos) |
| CU-12 Transferir saldo | `pantalla_transferir.dart` | `/billetera/transferir` | `cu12_transferir_saldo.dart` | ídem |
| CU-13 Consultar saldo | `pantalla_de_saldo.dart` (ya existía de F0/M1) | `/billetera/inicio` | `cu13_consultar_saldo.dart` | ya tenía 8 pruebas; se le agregó el botón real hacia `/billetera/recargar` |
| CU-15 Emitir extracto | `pantalla_extracto.dart` | `/billetera/extracto` | `cu15_emitir_extracto.dart` | cubierta por análisis estático; sin widget test propio |

Además: `dominio/proteccion_de_pantalla_dinero.dart` — un mixin
`ConProteccionDePantalla` que activa/desactiva el puerto `ProteccionPantalla` (ya
declarado por M1) al entrar y salir de las cuatro pantallas nuevas que muestran
saldo o mueven dinero. Se aplicó en `pantalla_recargar`, `pantalla_retirar`,
`pantalla_transferir` y `pantalla_extracto`.

## Decisión de infraestructura corregida (no es de este carril, pero bloqueaba)

Al arrancar, `flutter analyze` fallaba con 220 errores en **todo** `apps/movil` —
incluidas las pantallas de identidad que M1 ya había entregado— porque
`packages/diseno_flutter/lib/tokens/tokens.dart` no existía: es un artefacto que
genera `yarn workspace @aportaya/tokens build` y copia
`yarn workspace @aportaya/diseno-flutter build`, y nadie los había corrido en este
worktree. No es un archivo mío ni de M1: es el paso de build documentado en la nota
de `package.json` (*"los tres `dev:*` generan antes los tokens...  sin ellos nada
arranca con tema"*). Se corrió una vez:

```
yarn install --immutable
yarn workspace @aportaya/tokens build
mkdir -p packages/diseno_flutter/lib/tokens
yarn workspace @aportaya/diseno-flutter build
```

Con eso `flutter analyze` bajó de 220 a los 3 propios de este carril (ya
corregidos). También hizo falta `dart run build_runner build` dentro de
`clientes/dart/nucleo-financiero` (los `.g.dart` de `json_serializable` tampoco
estaban generados; sin ellos ningún `fromJson`/`toJson` compilaba, incluida la
pantalla de saldo de F0). Ninguno de estos comandos tocó un archivo fuera de
`clientes/dart/`, `packages/tokens/generado/` o `packages/diseno_flutter/lib/tokens/`
— todos artefactos generados y gitignorados; no aparecen en `git status`.

**Hueco para quien arranque el siguiente carril:** si esto se repite, correr esa
secuencia antes de reportar "main está roto".

## Supuestos declarados

1. **CU-10, medio de recarga**: la maqueta y el CU mencionan QR, tarjeta y
   transferencia; el contrato generado (`solicitarRecarga`) no devuelve payload de
   QR en `SalidaRecarga` (solo `ordenRecargaId`, `estado`, `expiraEn`,
   `acreditara`), a diferencia del bloque `ts` desactualizado dentro del propio CU-10
   (que sí trae `qr`). Se siguió el **contrato real** (`openapi` → cliente generado),
   no el bloque de ejemplo del documento: la pantalla muestra un estado "pendiente"
   genérico, sin dibujar un `CodigoQR`. Si el backend agrega el payload del QR, la
   pantalla se actualiza sin tocar el dominio de otra pantalla.
2. **CU-11, `instrumentoDestinoId`**: sin CU-18 construido (ver Huecos), el destino
   se pide como texto libre en vez de un selector de cuentas bancarias verificadas.
   Documentado en el propio archivo (`pantalla_retirar.dart`).
3. **`ProteccionPantalla` por ruta**: el puerto dice en su comentario que
   "`go_router` lo activa y desactiva por ruta al entrar y salir", pero no hay
   ninguna wiring de eso en `navegacion/` (congelado) ni en ningún otro archivo del
   repo — se comprobó con `grep -rn "ProteccionPantalla"` sobre todo `lib/`. Se
   asumió que, a falta de ese mecanismo central, cada pantalla se protege a sí
   misma (mixin `ConProteccionDePantalla`), que es lo que permite mi alcance sin
   tocar `navegacion/`. Si M1 o un carril de shell agrega el mecanismo central más
   adelante, este mixin queda redundante y se puede borrar sin romper nada (activar
   dos veces el mismo protector no es un error, según la forma del puerto).
4. **Cuenta por omisión**: se mantuvo la constante de UUID de ejemplo
   (`11111111-…`) que ya traía `rutas.dart` de F0/M1, para no inventar un mecanismo
   de "cuenta activa" que no está en ningún CU de este carril.
5. **`EntradaRetiro.factorMfa`**: se pide como texto de 6 dígitos porque no hay
   puerto de biometría expuesto para retiro en el CU (el puerto `biometria.dart` de
   M1 es para desbloqueo de sesión, no está conectado a esta operación); si el
   backend espera un código de otro origen (p. ej. push MFA), este campo cambia sin
   tocar el resto de la pantalla.

## Huecos pendientes — no se inventaron

| # | Qué falta | Por qué está bloqueado | CU afectados |
| --- | --- | --- | --- |
| 1 | **CU-30/31/32/33 (comisiones/tarifas)** completos: cotizar antes de confirmar, devengar, factura, nota de crédito | `apps/movil/pubspec.yaml` está congelado para este carril y **no declara** `aportaya_cliente_tarifas` (sí existe generado en `clientes/dart/tarifas` desde `yarn workspace @aportaya/tokens`… en realidad desde `generateOpenApiClients`, pero el paquete Flutter no lo importa). Sin esa dependencia no hay tipo `SalidaCotizacion` disponible en `apps/movil`, y agregarla a mano viola la regla de no tocar `pubspec.yaml`. **Se necesita un micro-PR** (`chore/troncal-M2-cliente-tarifas`) que agregue la dependencia de path, igual que ya existe para `aportaya_cliente_nucleofinanciero`. Sin eso, el gate propio *"la comisión se muestra antes de confirmar (CU-30)"* no se puede cumplir con un tipo generado real, y no se va a fabricar un tipo a mano (invariante "ningún tipo reescrito a mano"). |
| 2 | **Lectura de QR** (`mobile_scanner`, puerto `camara.dart` ya declarado por M1) | `mobile_scanner` no está en `pubspec.yaml`; mismo bloqueo que el punto 1 — micro-PR necesario. Afecta a CU-12 (pagar/transferir por QR) y a CU-21/57-adyacentes de alianzas (vale). |
| 3 | **D-21 vale con QR rotativo, estado, origen y condiciones; doble canje AP-VAL-03** | Depende del punto 2 (lectura de QR) y de un caso de uso de "vales" que no aparece en CU-10–19/30–33/57 (el catálogo lo mapea a otro módulo, probablemente `garantia-mora-cobranza` o `gobernanza-grupo`); no se encontró un `docs/CasosDeUso/CU-*.md` con el contenido de "vale" dentro del rango asignado a este carril. **Se declara como hueco de mapeo**: `planes/22 §2.3` debería decir qué CU exacto cubre el vale; no se adivinó. |
| 4 | **D-22 (grupo de 12 cupos → 12 cuotas navegables, 4+contador con 11 abiertas), D-19 (cobro de turno con bolsa/comisión/descuento/neto), D-17 (cuota exigible con "No voy a poder pagar")** | Estas pantallas viven en el dominio de **grupos y aportes** (`gobernanza-grupo`, `garantia-mora-cobranza`), no en `billetera` ni `alianzas` según `planes/16 §2` tal como se pudo verificar rápido; no se construyeron porque quedan fuera de los CU-10–19/30–33/57 asignados y de las carpetas que este carril posee en exclusiva. Si el mapa de `planes/22` los asigna efectivamente a M2, es un hueco de alcance a resolver con quien arma la ficha, no algo para decidir unilateralmente dado que toca dinero de terceros (el fondo de garantía). |
| 5 | **CU-14 Reversar transacción, CU-16 Cerrar billetera, CU-17 Bloquear por autoridad, CU-18 Registrar cuenta bancaria, CU-19 Reembolsar/disputa** | No se llegó por tiempo. Los clientes Dart ya existen (`reversarTransaccion`, `solicitarCierreBilletera`, `bloquearPorAutoridad`) y los ejemplos de contrato también, así que el siguiente tramo puede seguir el mismo patrón usado en CU-10/11/12 sin más descubrimiento. CU-17 (bloqueo por autoridad) probablemente no lleva pantalla de usuario — es una acción del backoffice/autoridad, no del participante — declarado como supuesto a confirmar, no como pantalla faltante. |
| 6 | **CU-57 Operar un punto de atención y arquear el efectivo** | **Obsoleto**: `docs/CasosDeUso/CU-10 Recargar saldo.md` documenta que [[ADR-039 Sin efectivo]] retiró el efectivo del producto el 20 de agosto de 2026 y that CU-57 "quedó obsoleto con su número reservado". No se construye nada; no es un hueco, es alcance que ya no existe. |
| 7 | **Goldens de `SelectorSegmentado` en claro/oscuro (D-12)** | Se usó el átomo (ya construido por diseño con relleno de marca en el elegido) en `pantalla_recargar` y `pantalla_transferir`, pero no se agregó un golden test dedicado a esa franja de la pantalla — el golden existente (`pantalla_de_saldo_golden_test.dart`) es de la pantalla de F0, no de las nuevas. Pendiente. |
| 8 | **`test:a11y`** solo cubre `pantalla_de_saldo` (heredado de F0/M1); las cuatro pantallas nuevas no tienen prueba de accesibilidad propia. `test:a11y` pasa (2/2) porque no hay pruebas nuevas que fallen, no porque las pantallas nuevas estén verificadas. |
| 9 | **Widget tests de CU-11, CU-12, CU-15** | Solo CU-10 tiene un widget test (el exigido por el gate de doble toque). CU-11 y CU-12 comparten exactamente el mismo patrón de `Boton(cargando:)` + `idempotenciaProvider`, así que el riesgo es bajo, pero no está probado con un test propio todavía. |

## Ejemplos del contrato

Ya existían en `packages/simulado/ejemplos/nucleo-financiero/` (generados junto con
el `openapi/nucleo-financiero.yaml` antes de este carril): `solicitarRecarga.json`,
`solicitarRetiro.json`, `transferirSaldo.json`, `emitirExtracto.json`. No se creó
ninguno nuevo; se consumieron tal cual desde `test/widget/pantalla_recargar_test.dart`
y desde `test/comun.dart` (`ejemplo(...)`).

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | --- |
| Arquitectura | `flutter analyze` sin ningún issue | `Analyzing movil... No issues found! (ran in 0.8s)` (corrido dos veces, la última tras el mixin de protección) | ✅ |
| Formato | `dart format --set-exit-if-changed lib test` | `Formatted 96 files (0 changed) in 0.06 seconds` | ✅ |
| Cero literales de diseño | `python3 scripts/verificar_frontend.py movil` | `sin literal de diseño`, `sin formato de dinero`, `sin red en vista`, `sin plataforma en vista`, `sin print`, `ningún archivo >200 líneas` — 9/9 OK, `TODO OK` | ✅ |
| Dinero — idempotencia + botón bloqueado | `flutter test test/widget/pantalla_recargar_test.dart` | `doble toque en la recarga: una sola petición, la misma clave de idempotencia` → **1 llamada**, **1 clave**, verificado con un interceptor de Dio que cuenta peticiones y clave `Idempotency-Key` real en dos toques consecutivos sin esperar entre medio | ✅ (CU-10; CU-11/CU-12 comparten el mismo mecanismo sin test propio — hueco 9) |
| Dinero — átomo `Monto` | `grep` negativo | `python3 scripts/verificar_frontend.py movil` → `sin formato de dinero` en verde (regex `NumberFormat\|toStringAsFixed\|double.parse\|num.parse` sobre todo `lib/`) | ✅ |
| Comisión antes de confirmar (CU-30) | — | **No implementado** — hueco 1 (pubspec congelado sin cliente de tarifas) | ⬜ bloqueado |
| QR con `mobile_scanner` | — | **No implementado** — hueco 2 (paquete no está en pubspec) | ⬜ bloqueado |
| `screen_protector` en rutas con saldo | Mixin `ConProteccionDePantalla` en las 4 pantallas nuevas + prueba de que no revienta con Riverpod (widget test de CU-10 pasa con el mixin activo) | Código en `dominio/proteccion_de_pantalla_dinero.dart`; `pantalla_de_saldo.dart` (la de F0) queda sin protección — es `ConsumerWidget` sin estado, no se convirtió para no arriesgar los goldens existentes sin revisar el diff a ojo (regla de goldens) | ⚠️ parcial |
| D-22 / D-19 / D-21 / D-17 | — | **No implementados** — huecos 3 y 4 (fuera de alcance de dominio o de CU no localizado) | ⬜ bloqueado |
| D-12 segmentado en los dos temas | Átomo usado; sin golden dedicado | hueco 7 | ⚠️ parcial |
| Entrega — `yarn workspace @aportaya/movil typecheck` | `dart analyze --fatal-infos` | `Analyzing movil... No issues found!` | ✅ |
| Entrega — `yarn workspace @aportaya/movil lint` | `dart format` + `verificar_frontend.py` | ambos en verde (ver arriba) | ✅ |
| Entrega — `yarn workspace @aportaya/movil test:front` | `flutter test test/unidad test/widget test/contrato test/goldens` | **32/32 pruebas en verde** (`00:03 +32: All tests passed!`) | ✅ |
| Entrega — `yarn workspace @aportaya/movil test:a11y` | `flutter test test/a11y` | **2/2** — solo cubre la pantalla heredada de F0 (hueco 8) | ⚠️ cobertura parcial |

## Salidas reales pegadas

```
$ yarn workspace @aportaya/movil typecheck
Analyzing movil...
No issues found!

$ yarn workspace @aportaya/movil lint
Formatted 95 files (0 changed) in 0.06 seconds.
=== apps/movil (Flutter) ===
  OK    · sin red en vista
  OK    · sin literal de diseño
  OK    · sin formato de dinero
  OK    · sin plataforma en vista
  OK    · sin print
  OK    · ningún archivo de más de 200 líneas
  OK    · el shell tiene navegacion/rutas.dart
  OK    · el shell tiene dominio/cliente.dart
  OK    · el shell tiene proveedores/sesion.dart
TODO OK

$ yarn workspace @aportaya/movil test:front
...
00:03 +30: pantalla de saldo · éxito · claro
00:03 +31: pantalla de saldo · éxito · oscuro
00:03 +32: All tests passed!

$ yarn workspace @aportaya/movil test:a11y
00:00 +0: la pantalla de saldo cumple las guías de accesibilidad de Flutter
00:00 +1: el estado de error también: el botón de reintento se alcanza y se lee
00:00 +2: All tests passed!
```

(Salida completa de cada comando disponible en la sesión que generó este informe;
se pega el resumen por espacio, no se omite ningún resultado adverso.)

## Micro-PR abiertos al troncal — pendientes de abrir

| Rama propuesta | Qué agrega | Mundo | Estado |
| --- | --- | :-: | :-: |
| `pablo/chore/troncal-M2-cliente-tarifas` | `aportaya_cliente_tarifas` como dependencia de `apps/movil/pubspec.yaml` (mismo patrón que `aportaya_cliente_nucleofinanciero`) | Flutter | **no abierto** — declarado en Huecos #1, requiere permiso fuera de este carril |
| `pablo/chore/troncal-M2-qr-y-proteccion` | `mobile_scanner` y (si falta) `screen_protector` en `pubspec.yaml` | Flutter | **no abierto** — declarado en Huecos #2 |

## Bloqueos

- Sin el micro-PR de `aportaya_cliente_tarifas`, CU-30 a CU-33 no se pueden
  implementar con tipos generados reales.
- Sin el micro-PR de `mobile_scanner`, la lectura de QR (CU-12 por QR, y el vale de
  D-21) no se puede implementar sin reinventar el puerto de cámara ya declarado.
- El mapa de `planes/22 §2.3` no deja claro si D-17/D-19/D-21/D-22 son de este
  carril o de `gobernanza-grupo`/`garantia-mora-cobranza`; se necesita esa
  aclaración antes de tocar esas cuatro pantallas.

## Ver también

`planes/16 Carriles de frontend.md` · `planes/18 Fichas de carril.md` (ficha F4) ·
`planes/12 Fases F2 a F5 · App móvil.md` · `docs/CasosDeUso/CU-10 Recargar saldo.md`
(nota de ADR-039 sobre CU-57)
