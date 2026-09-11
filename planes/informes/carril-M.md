---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril M — shell móvil (Flutter)"
ola: F1
fase: F2
mundo: Flutter
modulo: apps/movil/lib/{navegacion,proveedores,infraestructura}
rama: pablo/feature/carril-M-shell-movil
estado: en curso
---

# Carril M — shell móvil

**Fase** F2 · **Mundo** Flutter · **Casos de uso** ninguno de negocio (este carril
construye el shell) · **Puesto** P3 · Legion

> Ficha del carril: `planes/18` → `F2 · Shell móvil`. Bloque de la maqueta:
> `planes/22` §2.2. Este informe es nuevo, para F2; el informe `carril-F0-M.md`
> (andamiaje) sigue siendo válido para lo que entregó antes.

## Qué está hecho, con la evidencia que lo prueba

| Entregable | Evidencia | Estado |
| --- | --- | :-: |
| `StatefulShellRoute` con tab bar (`BarraPestanas` de `packages/diseno_flutter`) | `lib/navegacion/shell.dart` + `lib/navegacion/rutas.dart`; `flutter analyze` limpio | ✅ |
| Enchufe por dominio intacto (una entrada por directorio, apuntando a un `rutas.dart` vacío) | `test/unidad/enchufe_de_rutas_test.dart` — 2 pruebas en verde, sin editar | ✅ |
| Proveedores: sesión (ya existía), tema, conexión, idempotencia (ya existía), biometría, dispositivo | `lib/proveedores/{tema,conexion,biometria,dispositivo}.dart` | ✅ |
| Los siete puertos declarados con interfaz en `dominio/puertos/` | `biometria.dart` · `avisos_push.dart` · `camara.dart` · `haptica.dart` · `proteccion_pantalla.dart` (+ `almacen_seguro.dart`, `conectividad.dart` ya existentes) | ✅ |
| Adaptador Android de cada puerto | `infraestructura/android/{biometria,avisos_push,camara,haptica,proteccion_pantalla,conectividad}_android.dart` | ✅ (2 con hueco declarado abajo) |
| Biometría y protección de pantalla con receptor **nativo** de verdad | `MainActivity.kt` (`BiometricPrompt`, `FLAG_SECURE`) + `androidx.biometric:1.1.0` en `build.gradle.kts` | ✅ |
| Bandeja de notificaciones `/notificaciones/bandeja` | `pantallas/notificaciones/{dominio,proveedor_bandeja,pantalla_de_bandeja,rutas,textos}.dart` | ✅ |
| `dominio/validacion.dart` | validadores de teléfono, carnet y monto, sin Flutter ni red | ✅ |
| Deep link de esquema propio (`aportaya://…`) | `navegacion/enlaces_profundos.dart` + intent-filter en `AndroidManifest.xml`; 2 pruebas de widget (app cerrada / app abierta) | ✅ |
| Verificación de contrato al iniciar (`GET /version`) | `dominio/verificacion_contrato.dart`, consumido en `app.dart` (franja no bloqueante) | ✅ |
| Shorebird con canal por entorno | — | 🟡 hueco declarado |

### Comandos y su salida real

```
$ yarn workspace @aportaya/movil build
...
Built with build_runner/aot in 0s; wrote 0 outputs.
(éxito, sin errores)

$ yarn workspace @aportaya/movil lint
Formatted 62 files (0 changed) in 0.04s
=== apps/movil (Flutter) ===
  OK · sin red en vista
  OK · sin literal de diseño
  OK · sin formato de dinero
  OK · sin plataforma en vista
  OK · sin print
  OK · ningún archivo de más de 200 líneas
  OK · el shell tiene navegacion/rutas.dart
  OK · el shell tiene dominio/cliente.dart
  OK · el shell tiene proveedores/sesion.dart
TODO OK

$ yarn workspace @aportaya/movil typecheck
Analyzing movil...
No issues found!

$ dart format --output=none --set-exit-if-changed apps/movil/lib apps/movil/test
Formatted 62 files (0 changed)   # sin cambios pendientes

$ yarn workspace @aportaya/movil test:front
... 31 pruebas, 0 falladas — All tests passed!
(con --concurrency=1: la corrida por omisión de Flutter pierde archivos bajo carga
 en esta máquina — ver «Huecos» abajo. Repetida con --concurrency=1 corre las 31
 pruebas de test/unidad, test/widget, test/contrato y test/goldens sin fallar)

$ yarn workspace @aportaya/movil test:a11y
... 2 pruebas, 0 falladas — All tests passed!

$ grep -r "Platform.is" apps/movil/lib/ | grep -v "/infraestructura/"
(vacío)

$ grep -rn "SharedPreferences" apps/movil/lib/
apps/movil/lib/proveedores/sesion.dart:14:/// ... nunca en `SharedPreferences`.
(única aparición: un comentario que dice explícitamente que NO se usa; no hay
 import de shared_preferences ni construcción de la clase — verificado también
 por la prueba automática `gate_del_shell_test.dart`)
```

## Piezas declaradas por nivel

| Pieza | Nivel | Dónde vive | Estado |
| --- | --- | --- | :-: |
| `ShellPrincipal` | organismo (compone `BarraPestanas` del paquete) | `navegacion/shell.dart` | ✅ |
| `Guardias` | átomo de dominio (funciones de redirect) | `navegacion/guardias.dart` | ✅ |
| `rutaInternaDesde` | átomo de dominio (traducción de URI) | `navegacion/enlaces_profundos.dart` | ✅ |
| `PantallaDeBandeja` | pantalla, compone `ItemDeNotificacion` del paquete | `pantallas/notificaciones/pantalla_de_bandeja.dart` | ✅ |
| `BandejaNotificaciones` | molécula de estado (Riverpod `Notifier`) | `pantallas/notificaciones/proveedor_bandeja.dart` | ✅ |
| Interfaces de los 7 puertos | átomo de dominio (contratos, sin implementación) | `dominio/puertos/*.dart` | ✅ |
| Adaptadores Android | infraestructura | `infraestructura/android/*.dart` | ✅ |

## Supuestos declarados

Regla cero: ninguno silencioso.

1. **Tab bar: «Inicio» y «Movimientos» comparten la misma rama de `StatefulShellRoute`.**
   La maqueta (`planes/22` §2.2) fija 4 destinos — Inicio · Grupos · Movimientos ·
   Perfil — pero los dos primeros son ambos del dominio `billetera`
   (`/billetera/inicio` y `/billetera/extracto`), y ese dominio tiene **un solo**
   `rutas.dart` que no edito. `StatefulShellRoute.indexedStack` da una pila propia
   por *rama*, no por destino visible de la barra: implementé 3 ramas
   (`billetera`, `pasanaku`, `identidad`) y 4 destinos visibles, donde «Movimientos»
   navega *dentro* de la rama de `billetera` en vez de cambiar de rama. Documentado
   en el docstring de `ShellPrincipal`. Si el carril M2 prefiere una pila separada
   para el extracto, es una conversación de diseño de navegación, no un bug de este
   shell.
2. **Marcadores `_AunNoDisponible` en `/pasanaku` e `/identidad`.** Los dominios
   `pasanaku/rutas.dart` e `identidad/rutas.dart` están vacíos (los llenan M3 y M1
   en F5 y F3). Para que la tab bar no lleve a una pantalla en blanco *hoy*, el
   shell declara una ruta placeholder en esos prefijos exactos (`/pasanaku`,
   `/identidad`, sin sub-ruta). **Advertencia para M1 y M3:** no declaren una ruta
   bare `/pasanaku` o `/identidad` — usen una sub-ruta (`/pasanaku/inicio`,
   `/identidad/perfil`, etc.); si necesitan que la raíz del dominio resuelva a una
   pantalla, redirijan desde dentro de su propio `rutas.dart`. Con eso, el
   placeholder deja de matchear solo porque `go_router` prioriza rutas más
   específicas — sin que el shell cambie.
3. **`AvisosPush` en Android queda con el canal declarado pero sin receptor nativo.**
   El registro FCM real pide el SDK de Firebase (plugin de Gradle a nivel de
   proyecto + `google-services.json`), que no existe en este andamiaje y que decidir
   agregar es una decisión de proveedor (`notificaciones-consentimiento`,
   ADR-035), no una implementación de carril de frontend. El puerto, el adaptador y
   el canal `MethodChannel` están declarados y listos: `token()` devuelve `null` sin
   romper nada, porque `/notificaciones/bandeja` es la fuente de verdad y el push es
   solo un aviso de que hay algo nuevo (regla ya vigente antes de este supuesto).
4. **`Camara` en Android: canal declarado, sin captura real.** `F2` la declara «sin
   usuario todavía» por instrucción explícita (el primero es F3, CU-01). El
   `MethodChannel` existe y el lado Kotlin responde `null` — cuando F3 lo necesite,
   se implementa `registerForActivityResult` con `MediaStore.ACTION_IMAGE_CAPTURE`
   sin tocar la interfaz del puerto.
5. **iOS: pase de paridad solo en `AlmacenSeguro` (ya existía) y `Haptica`.**
   `Biometria`, `AvisosPush`, `Camara` y `ProteccionPantalla` no tienen implementación
   iOS propia todavía — caen al mismo canal que Android sin receptor nativo iOS
   (ADR-036, «Android primero, iOS por pase»). Es un hueco de F2 para el pase, no
   para este cierre.
6. **`dominio.dart` de notificaciones vive en `pantallas/notificaciones/`, no en
   `dominio/notificaciones.dart`.** `planes/22` §2.2 nombra `dominio/notificaciones.dart`
   de pasada, pero la lista de "poseés en exclusiva" de este carril dice
   `lib/pantallas/notificaciones/` como directorio completo — sin mencionar
   `dominio/notificaciones.dart` en la lista de `dominio/{cliente,errores,validacion}`.
   Interpreté que el modelo y la lista cerrada de eventos sin aviso son parte de la
   bandeja, no del núcleo de dominio compartido, y los dejé junto a la pantalla que
   los usa.
7. **`versionDeContratoCompilada = '1'`.** No hay forma automática de leer una
   versión del cliente Dart generado (`clientes/dart`, generado y sin dueño de
   carril): la constante se sube a mano en cada micro-PR de contrato. Documentado en
   el propio archivo.
8. **`Prism`/escenario adverso.** El plan menciona `Prefer: example=adverso` contra
   Prism para el escenario de red caída de la maqueta; este carril no levantó Prism
   (no hace falta para el shell: las pruebas de `EstadoDePantalla`/`EstadoError` usan
   un `AsyncValue.error` fabricado). Queda para quien primero necesite correr contra
   Prism de verdad.

## Huecos encontrados, no completados con una suposición

| Hueco | Dónde | De quién / cuándo |
| --- | --- | --- |
| **`clientes/dart` no existía en el checkout** y `packages/diseno_flutter/lib/tokens/tokens.dart` tampoco (son generados, `.gitignore`d, y no había corrido el build una sola vez en esta máquina) | ambiente | Lo generé yo mismo con los comandos de build declarados (`./gradlew generateOpenApiClients`, `dart run build_runner build` en `clientes/dart/nucleo-financiero`, `cp` del `tokens.dart` generado por `packages/tokens`) — son pasos de construcción, no ediciones a paquetes congelados. Cualquier carril que arranque de cero necesita correrlos también; **no está en `arrancar-carril` §5 explícitamente para Flutter** — declarar como hueco del troncal |
| **Shorebird** | — | No configurado: pide una cuenta/proyecto en la consola de Shorebird y un `shorebird.yaml` con `app_id` real, que no existe en este entorno. Fabricar uno falso sería peor que declarar el hueco. Queda para quien tenga la cuenta — no es una decisión técnica de este carril |
| **`yarn turbo run test:front test:a11y --filter=@aportaya/movil` en paralelo** pierde archivos de prueba silenciosamente (menos pruebas reportadas que las que existen, sin marcar falla) y además colisiona con `flutter` (`Waiting for another flutter command to release the startup lock`) y con la compilación de assets nativos de `objective_c` en esta Mac | ambiente / turbo | Cada script (`lint`, `typecheck`, `test:front`, `test:a11y`) corrido **por separado** vía `yarn workspace @aportaya/movil <script>` es determinístico y está pegado arriba con su salida real. `flutter test` con `--concurrency=1` también da el conteo completo y correcto (31/31). No es un defecto del shell; es una limitación de correr varios `flutter` a la vez en esta máquina — declarado para quien configure el CI |
| **`AvisosPush` sin receptor FCM real** (supuesto 3) | `infraestructura/android/avisos_push_android.dart` | Quien decida el proveedor de push (ADR-035) y provisione el proyecto Firebase |
| **`Camara` sin captura real** (supuesto 4) | `infraestructura/android/camara_android.dart` | Carril `M1`, F3, CU-01 (primer usuario real del puerto) |
| **Pase de paridad iOS** de `Biometria`, `AvisosPush`, `Camara`, `ProteccionPantalla` (supuesto 5) | `infraestructura/ios/` | Cuando el proyecto entre al pase de iOS (ADR-036) |
| **Prism no se levantó** en esta máquina (supuesto 8) | — | Quien primero necesite el escenario adverso real contra el mock server |

## Lo que queda abierto, y de quién es

| Qué | De quién | Cuándo |
| --- | --- | --- |
| Llenar `pantallas/identidad/rutas.dart` | M1 | F3 |
| Llenar `pantallas/billetera/rutas.dart` y `pantallas/alianzas/rutas.dart` (incluido `/billetera/extracto`, que ya tiene su lugar reservado en la tab bar) | M2 | F4 |
| Llenar `pantallas/pasanaku/rutas.dart` y `pantallas/soporte/rutas.dart` | M3 | F5 |
| Shorebird de verdad (cuenta + `shorebird.yaml`) | quien tenga la cuenta | antes de la primera release |
| Receptor FCM nativo para `AvisosPush` | quien decida el proveedor de push | cuando se active push real |
| `MediaStore.ACTION_IMAGE_CAPTURE` para `Camara` | M1 | F3, CU-01 |

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | :-: |
| Sesión | Sobrevive a cerrar y reabrir la app | `test/unidad/sesion_persiste_test.dart` — 2/2 verde (ProviderContainer nuevo, mismo almacén) | ✅ |
| Seguridad | Nada sensible en `SharedPreferences` | grep real pegado arriba + `test/unidad/gate_del_shell_test.dart` | ✅ |
| Resiliencia | Red caída ⇒ estado, no pantalla en blanco | `test/widget/conexion_test.dart` — `EstadoError` con ícono sin-conexión, mensaje y botón de reintento | ✅ |
| Navegación | Deep link abre la pantalla correcta, app cerrada y abierta | `test/widget/deep_link_test.dart` — 2/2 verde | ✅ |
| Plataforma | `Platform.is` solo en `infraestructura/` | grep real pegado arriba, vacío | ✅ |
| Calidad | `flutter analyze` limpio | pegado arriba, `No issues found!` | ✅ |
| Calidad | `dart format` sin cambios | pegado arriba, `0 changed` | ✅ |
| Entrega | `yarn workspace @aportaya/movil build/lint/typecheck/test:front/test:a11y` | pegado arriba, todo verde | ✅ |
| Arquitectura | Enchufe por dominio intacto | `enchufe_de_rutas_test.dart` 2/2, sin editar `navegacion/rutas.dart` al agregar pantalla | ✅ |
| Shorebird | Parche en canal `desarrollo` | — | 🟡 hueco declarado |

### Frases prohibidas sin evidencia

No se dice «está listo». Lo que hay: **33 pruebas en verde entre `test:front` y
`test:a11y` (31 + 2), `flutter analyze` sin hallazgos, `dart format` sin cambios
pendientes, los siete puertos con interfaz y adaptador Android — dos de ellos
(`Biometria` con `BiometricPrompt`, `ProteccionPantalla` con `FLAG_SECURE`) con
receptor nativo Kotlin de verdad y no un stub —, el grep de `Platform.is` vacío
fuera de `infraestructura/`, el grep de `SharedPreferences` sin un uso real, y dos
huecos declarados por escrito (Shorebird, FCM) en vez de fabricados.**

## Ver también

`planes/12 Fases F2 a F5 · App móvil` · `planes/18 Fichas de carril` (ficha `F2`) ·
`planes/22 Mapa de la maqueta` §2.2 · `planes/informes/carril-F0-M.md` (andamiaje
previo, sigue vigente) · skill `movil-flutter`
