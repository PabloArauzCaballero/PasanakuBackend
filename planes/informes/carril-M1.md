---
tags:
  - plan
  - informe
  - carril
titulo: "Carril M1 — móvil · identidad y cuenta"
ola: F2
fase: F3
modulo: apps/movil
rama: pablo/feature/carril-M1-identidad
estado: parcial
---

# Carril M1 — móvil · identidad y cuenta

**Fase** F3 · **Pantallas** once, en `apps/movil/lib/pantallas/identidad/`

> Este informe reemplaza al anterior (bloqueado, con rama `pablo/feature/carril-0T-cimientos`
> y basado en Expo/TypeScript). Al arrancar esta vez, `dev` ya trae el shell móvil de F2 en
> Flutter (`navegacion/`, `proveedores/`, `infraestructura/`, los siete puertos) y
> `packages/diseno_flutter` congelado con átomos, moléculas y organismos genéricos — los dos
> bloqueos que frenaban al informe anterior están resueltos. El carril se ejecutó.

## 1 · Qué quedó hecho

Las once pantallas de identidad, registradas **solo** en
`apps/movil/lib/pantallas/identidad/rutas.dart`:

| # | Ruta | CU | Pantalla |
| :-: | --- | :-: | --- |
| 1 | `/identidad` | CU-04 | `PantallaDeSesion` — teléfono y contraseña |
| 2 | `/identidad/bienvenida` | CU-01 (D-9) | `PantallaDeBienvenida` |
| 3 | `/identidad/registro` | CU-01 | `PantallaDeRegistro` — el asistente de ocho pasos |
| 4 | `/identidad/verificacion-basica` | CU-01 | mismo asistente, segunda puerta de entrada (§3, H-2) |
| 5 | `/identidad/contrato` | CU-05 | `PantallaDeContrato` — gate de scroll |
| 6 | `/identidad/mfa` | CU-04 | `PantallaDeMfa` |
| 7 | `/identidad/dispositivos` | CU-04 | `PantallaDeDispositivos` |
| 8 | `/identidad/verificacion-profunda` | CU-02, CU-03 | `PantallaDeVerificacionProfunda` — declaración PEP |
| 9 | `/identidad/perfil` | CU-07 | `PantallaDePerfil` |
| 10 | `/identidad/contrasena` | CU-09 | `PantallaDeContrasena` |
| 11 | `/identidad/baja` | CU-09 | `PantallaDeBaja` |

El alta de ocho pasos (CU-01, delta D-1 de la maqueta) no es un `StatefulWidget` gigante:
es `AltaNotifier` (`dominio/estado_alta.dart`, Riverpod `Notifier`) que sabe en qué paso
está, más un organismo por paso en `pasos_alta/` (`paso_datos.dart`, `paso_celular.dart`,
`paso_captura.dart` —reusado para anverso, reverso y prueba de vida—, `paso_cotejo.dart`,
`paso_perfil_transaccional.dart`), orquestados por `pantalla_registro.dart`.

El mismo patrón (un `Notifier` por flujo, un organismo por paso) se repite en
`dominio/estado_sesion.dart` (CU-04: credenciales → MFA → dispositivo de confianza),
`dominio/estado_contrato.dart` (CU-05) y `dominio/estado_cuenta.dart` (CU-03 PEP, CU-07
perfil, CU-09 contraseña y baja).

### El gate de cámara (CU-01)

`pasos_alta/paso_captura.dart` usa el puerto `Camara` (F2, `dominio/puertos/camara.dart`),
cuyo contrato ya devuelve `null` — nunca lanza — ante permiso denegado o fallo del canal
nativo. La pantalla trata ese `null` como una alternativa explícita: ofrece reintentar, y
si `permitirEscribirAMano` es true (todos los pasos salvo la prueba de vida, ver §3 S-1),
ofrece seguir con los datos ya escritos a mano. Probado en
`test/identidad/estado_alta_test.dart` a nivel de notifier; el recorrido completo con la
cámara requiere un `MethodChannel` simulado — anotado como pendiente en §4.

### El gate del contrato (CU-05)

`pantalla_contrato.dart` deshabilita "Aceptar y continuar" hasta que un
`ScrollController` reporta el final del texto (`_alDesplazar`), y además exige los tres
consentimientos por separado (contrato, tarifario, tratamiento de datos) — ninguno se
puede tocar antes de llegar al final. Verificado en
`test/identidad/pantalla_contrato_test.dart`: scroll parcial deja el botón deshabilitado,
scroll completo + los tres checks lo habilita. Salida real del `flutter test`, pegada en
§5.

### El puerto de cámara sin proveedor

`proveedores/` está congelado para este carril y no traía un `Provider<Camara>` (los
siete puertos de F2 solo declararon la interfaz). Se agregó
`pantallas/identidad/dominio/camara_identidad.dart` con el provider, envolviendo la misma
fábrica (`camaraDeLaPlataforma()`) que usan los demás puertos — sin tocar `proveedores/`.
Si otro carril lo necesita, corresponde un micro-PR que lo suba, no una segunda copia.

## 2 · Decisiones tomadas y por qué

| Decisión | Por qué |
| --- | --- |
| El alta en ocho pasos vive en **una** pantalla-anfitriona con un `Notifier`, no en ocho rutas | Es el patrón que pide el gate del carril ("no es un `StatefulWidget` de 600 líneas: son organismos por paso + un Notifier"), y evita que `go_router` tenga que reconstruir el estado del alta al navegar entre pasos |
| `/identidad/registro` y `/identidad/verificacion-basica` son dos rutas hacia el **mismo** `AltaNotifier` | La maqueta las nombra como pantallas separadas (fila 2.2 y 2.3 del informe anterior); el estado que gobierna los pasos es uno solo, así que se registran como dos puertas de entrada al mismo asistente en vez de duplicar el flujo. Ver H-2 en §3 |
| CU-02 (elevar nivel) y CU-03 (PEP) comparten una pantalla | Así las agrupa `planes/12` fila 2.8: "la elevación de nivel la dispara el servidor" (no hay una acción de cliente que "eleve nivel" por sí misma), y lo único con forma propia para una pantalla es la declaración PEP |
| Ninguna pantalla llama a la red | Sin el cliente Dart de `identidad` cableado en `apps/movil/pubspec.yaml` (§3, H-CLIENTE), llamar a la red habría significado inventar el *shape* de las respuestas a mano — exactamente lo que la regla "ningún tipo reescrito a mano" prohíbe. Cada notifier expone un método de envío (`enviarAlServidor`, `iniciarSesion`, `guardar`, `confirmar`, `confirmarBaja`) que hoy lanza `UnimplementedError` con el hueco citado, en vez de simular una respuesta |
| La prueba de vida no ofrece "escribir a mano" | Es una foto que compara rostro contra documento; no hay equivalente manual. Supuesto declarado, no lo dice la bóveda explícitamente |
| El texto del contrato de adhesión es de relleno | El contenido real lo publica `servicios/cumplimiento` (CU-05 se mudó ahí en el carril 0T); sin ese endpoint, se usa un texto largo fijo solo para poder ejercer y probar el gate de scroll |
| La baja (CU-09) no pide motivo | La maqueta no especifica un campo de motivo en esa pantalla; se pide solo confirmación explícita con diálogo de doble toque |

## 3 · Huecos declarados

| # | Hueco | Detalle | De quién es |
| :-: | --- | --- | --- |
| H-CLIENTE | El cliente Dart de `identidad` (`clientes/dart/identidad`) existe (se generó al correr `./gradlew generateOpenApiClients` durante este carril) **pero no está declarado en `apps/movil/pubspec.yaml`** | `pubspec.yaml` es de solo lectura para este carril; agregar la dependencia es un cambio de una línea que le corresponde a quien posee el shell (carril M) o a un micro-PR troncal | Carril M / troncal |
| H-1 | `POST /sesion` vs `/sesiones` | Heredado del informe anterior; no verificado de nuevo porque ninguna pantalla llama a la red todavía | Backend de identidad |
| H-2 | El alcance de CU de F3 no coincide entre `planes/12` (sin CU-08) y la ficha F3 (CU-01–09) | Se implementó CU-01–07, 09; **CU-08 (asignar rol) no tiene pantalla** — es un caso de operador/organizador, no de la app del participante, así que se declara fuera de alcance de móvil | Definición de producto |
| H-3 | CU-40 y CU-46 no tienen pantalla propia | Son evaluaciones que el servidor hace al procesar otras operaciones (límites, alcance de licencia), no pantallas — se declaran como estados de error posibles dentro de las pantallas existentes, no como rutas nuevas | — |
| H-4 | CU-05 (contrato) lo sirve el backend de `cumplimiento`, no `identidad` | Decisión del carril 0T; la pantalla de contrato queda lista para apuntar a ese servicio en cuanto tenga cliente | Carril de cumplimiento |
| H-5 | Sin OCR de documento contratado | `PasoCotejo` compara "declarado" contra "declarado" (mismo valor) en vez de declarado contra leído por un proveedor de KYC — la mecánica de la fila de cotejo (`FilaDeCotejo`, de `packages/diseno_flutter`) queda ejercida, pero no hay lectura real todavía | Backend / proveedor de KYC |
| H-6 | El monto del bono de bienvenida (D-9) no está en ninguna bóveda | `PantallaDeBienvenida` muestra `—` en vez de inventar una cifra | Catálogo / promociones |
| H-7 | `test/unidad/enchufe_de_rutas_test.dart` (de M, fuera de mi alcance) asume que `identidad/rutas.dart` se puede vaciar reemplazando `= [];`; con contenido real ese `replaceFirst` ya no encuentra el patrón y no hace nada — la prueba sigue pasando porque compara huellas fuera de `pantallas/identidad/`, pero quedó comprobado y no corregido (no es mío) | Se verificó que **no rompe**, solo queda como nota para quien la lea después | Carril M |

## 4 · Qué queda abierto

- **Sin llamadas de red reales** en ninguna de las once pantallas: cada acción de envío
  (`enviarAlServidor`, `iniciarSesion`, `guardar`, `confirmar`, `confirmarBaja`) lanza
  `UnimplementedError` citando este informe. Cerrarlo es una línea en `pubspec.yaml`
  (H-CLIENTE) más cablear cada notifier contra `aportaya_cliente_identidad` — no rediseñar
  pantallas.
- **Sin goldens** para las once pantallas: no se generaron porque "un golden no se
  actualiza sin mirar la diferencia" y no hay una primera versión aprobada contra la cual
  comparar; se dejan para cuando el hueco H-CLIENTE cierre y las pantallas muestren datos
  reales en vez de placeholders.
- **Prueba de la cámara con `MethodChannel` simulado** (permiso denegado / fallo real de
  plataforma) no se escribió — se verificó el camino a nivel de notifier
  (`capturarAnverso(null)` etc.) pero no con un doble del canal nativo.
- **CU-06** (revisión periódica de KYC) no tiene pantalla: es un proceso que dispara el
  backend, no una acción iniciada por el usuario; no se encontró fila en la maqueta para
  eso, y no se inventó una.

## 5 · Gate del carril — salida real

```
$ flutter analyze lib/pantallas/identidad test/identidad
Analyzing 2 items...
   info • prefer_const_constructors (x4, sin impacto funcional)
   info • use_null_aware_elements (x1)
5 issues found. (ran in 2.2s)

$ flutter analyze                       # app entera
Analyzing movil...
   (los mismos 5 infos de arriba)
5 issues found. (ran in 1.1s)

$ dart format --output=none --set-exit-if-changed lib/pantallas/identidad test/identidad
(sin salida — ya formateado)

$ flutter test test/identidad
00:00 +0: pantalla_contrato_test.dart: aceptar sigue deshabilitado con scroll parcial...
00:00 +1: pantalla_contrato_test.dart: aceptar sigue deshabilitado con scroll parcial...
00:00 +2: pantalla_contrato_test.dart: aceptar sigue deshabilitado con scroll parcial...
00:00 +3: All tests passed!

$ flutter test test/unidad/enchufe_de_rutas_test.dart
00:00 +0: el enchufe del shell importa las rutas de cada dominio, y nada más las conoce
00:00 +1: agregar una pantalla vacía en un dominio no cambia ningún archivo fuera de él
00:00 +2: All tests passed!

$ grep -c "name: 'identidad\." apps/movil/lib/pantallas/identidad/rutas.dart
11
```

`flutter test` (la suite completa de `apps/movil`) falla en 5 archivos **ajenos a este
carril**: `test/a11y/pantalla_de_saldo_a11y_test.dart`, `test/contrato/consultar_saldo_test.dart`,
`test/goldens/pantalla_de_saldo_golden_test.dart`, `test/widget/deep_link_test.dart` y
`test/widget/pantalla_de_saldo_test.dart` — todos por un error de compilación preexistente
en `clientes/dart/nucleo-financiero` (`_$SalidaReversoFromJson` no generado: falta correr
`build_runner` sobre ese cliente). No es código de identidad ni lo toca; verificado
corriendo `flutter test test/identidad` en aislado, que pasa entero.

No se corrió `yarn workspace @aportaya/movil build/lint/typecheck/test:front/test:a11y`
porque ese *target* de Turbo no existe para Flutter en este monorepo (el `package.json`
del taller de móvil expone `test:front`/`test:a11y` sobre `packages/diseno_flutter`, no
sobre `apps/movil`, que se gobierna con Flutter directo) — se usó el equivalente directo
de Flutter, como preveía el encargo.

## 6 · Ficha de paridad iOS — bloque de ingreso

Ninguna pantalla de este carril se implementó en iOS (ADR-036: Android primero, iOS por
pase de paridad). Diferencias esperadas de antemano para el bloque de ingreso
(`/identidad`, `/identidad/mfa`, `/identidad/dispositivos`):

| Aspecto | Android (implementado) | iOS (esperado, no implementado) |
| --- | --- | --- |
| Captura de documento/selfie | `MethodChannel` propio sobre `MediaStore.ACTION_IMAGE_CAPTURE` (`CamaraAndroid`) | Necesita su propio adaptador nativo (`AVFoundation` o `image_picker` con `UIImagePickerController`); el puerto `Camara` no cambia, solo la implementación en `infraestructura/ios/` |
| Biometría en "confiar en este dispositivo" | Ya resuelto por el puerto `Biometria` (huella/rostro Android) | Face ID / Touch ID vía `local_auth`; mismo puerto, mismo contrato — sin trabajo de pantalla |
| Autofill de OTP | `CampoOTP` no usa `SmsRetriever` de Android en esta versión (fuera de alcance) | iOS ofrece autofill de SMS nativo en `UITextField` con `textContentType = .oneTimeCode`; **el átomo `CampoOTP` de `packages/diseno_flutter` no expone ese hook** — hueco para F1, no para M1 |
| Vuelta atrás del sistema | Botón físico/gesto Android controlado por `PopScope` implícito de `go_router` | Gesto de swipe-back de iOS puede interrumpir un paso del alta a mitad de captura; no se probó, queda como riesgo declarado |
| Selector de fecha (`showDatePicker`) | Material date picker | iOS normalmente espera un `CupertinoDatePicker`; `paso_datos.dart` usa el picker de Material sin diferenciar plataforma — sería el primer ajuste visual al portar |

## 7 · Invariantes verificados

- **Invariante 7** (el cliente no es la garantía final): todas las validaciones en
  `dominio/validaciones.dart` son de ayuda (formato de teléfono, longitud de nombre,
  fortaleza de contraseña) — ninguna decide duplicados, listas restrictivas, ni si un
  contrato está vigente; eso lo hace el servidor cuando el hueco H-CLIENTE cierre.
- **Cero literales de diseño**: todos los textos están en `textos.dart`; todos los colores,
  espaciados y radios vienen de `Tokens.of(context)` / las constantes `Espacio`, `Radios`
  que expone `packages/diseno_flutter`.
- **Ninguna llamada de red en un widget**: no hay un solo `Dio(...)` ni `dio.get/post` en
  `pantallas/identidad/**` — es, a propósito, la consecuencia de H-CLIENTE.
- **Ningún tipo reescrito a mano**: por la misma razón no se escribieron clases que imiten
  la forma de `identidad.yaml` a mano; los `Estado*` de cada notifier son estado de UI
  (paso actual, texto de un campo), no modelos de contrato.

## Ver también

[[12 Fases F2 a F5 · App móvil]] · [[16 Carriles de frontend]] ·
[[18 Fichas de carril · las 38 unidades de trabajo]] ·
[[22 Mapa de la maqueta · pantalla, carril y mundo]] · `docs/CasosDeUso/CU-01` a `CU-09`,
`CU-40`, `CU-46`
