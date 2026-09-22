---
name: movil-flutter
description: "Construir la app móvil de AportaYa con Flutter y Dart: composición atómica con widgets, Riverpod y AsyncValue para los cuatro estados, go_router con un enchufe de rutas por dominio, dio sobre el cliente generado clientes/dart, puertos para cámara, biometría, almacén seguro y push, red intermitente, Shorebird para parches y Android primero. Úsala al crear o modificar cualquier pantalla, widget o proveedor de apps/movil o de packages/diseno_flutter."
---

# App móvil con Flutter

La app del participante: aportar, pagar por QR, ver turno, cobrar la bolsa, ver
saldo. Contexto real: Android de gama baja, datos móviles intermitentes, en la calle.

El **diseño visual** (tokens, átomos, componentes móviles) lo manda la skill
`disenar-frontend`; esta skill manda la **estructura y el comportamiento**. La decisión
de stack es [[ADR-044 Frontend en Angular y Flutter]]; el orden de plataformas,
[[ADR-036 Android primero]].

## Estructura

```
apps/movil/
├── pubspec.yaml · .fvmrc · package.json      el package.json es un envoltorio: lint/test:front/test:a11y para turbo
├── lib/
│   ├── main.dart · app.dart                  ProviderScope, tema desde tokens, router
│   ├── navegacion/                           go_router: shell con tab bar, deep links, guardias · UN ENCHUFE POR DOMINIO — se congela en F2
│   ├── proveedores/                          sesion · tema · conexion · idempotencia · biometria · dispositivo (Riverpod)
│   ├── dominio/
│   │   ├── cliente.dart                      dio + interceptores — la ÚNICA salida a la red
│   │   ├── errores.dart                      AP-CU<NN>-<nn> → voz de marca
│   │   ├── validacion.dart                   validadores generados del contrato
│   │   ├── cuNN_<verbo>.dart                 un archivo por caso de uso, sobre clientes/dart
│   │   └── puertos/                          Biometria · AvisosPush · Camara · AlmacenSeguro · Haptica · Conectividad · ProteccionPantalla
│   ├── infraestructura/{android,ios}/        adaptadores — el pase de iOS vive acá
│   ├── organismos/                           solo los que dependen de una API nativa (MarcoDeCamara, LectorQR)
│   └── pantallas/<dominio>/                  identidad · billetera · alianzas · pasanaku · soporte · notificaciones
│       ├── rutas.dart                        `List<RouteBase> rutasIdentidad` — el enchufe que el shell importa
│       ├── textos.dart                       los textos de este dominio, en voz de marca
│       └── <pantalla>.dart                   compone organismos; sin lógica
├── test/{unidad,widget,a11y,contrato,goldens}/
└── integration_test/                         Patrol

packages/diseno_flutter/                      paquete Dart `aportaya_diseno`: tema, átomos, moléculas, organismos, piezas móviles, Widgetbook
packages/tokens/generado/tokens.dart          GENERADO desde tokens.json — el único archivo con literales de diseño
clientes/dart/<servicio>/                     GENERADO desde el OpenAPI — no se edita
```

**Un archivo, un widget**, con el nombre del widget. Un `build()` de más de 80 líneas
está mezclando niveles: son varios widgets sin extraer.

## Composición: los cuatro niveles en Flutter

| Nivel | Qué es | Depende de | Nunca |
| --- | --- | --- | --- |
| **Átomo** | `StatelessWidget` con `Semantics`, estilos de `Tokens.of(context)` | `tokens.dart` | conocer una regla de negocio; usar `Colors.*` |
| **Molécula** | un widget con una responsabilidad, o un `@riverpod` que envuelve **un** recurso | átomos, `dominio/` | orquestar la pantalla, navegar |
| **Organismo** | sección autónoma: formulario, lista, panel | moléculas, átomos, `dominio/` | `dio` directo |
| **Pantalla** | `ConsumerWidget` que compone organismos y lee la ruta | organismos | cálculos, reglas, `Platform.is*` |

Ningún widget hace red: la red vive en `dominio/`, y las pantallas la consumen por
proveedores de Riverpod.

## Rutas: el enchufe por dominio

`go_router` no descubre rutas por archivos. Para que un carril agregue pantallas sin
editar el shell, **F2 deja escrito** en `navegacion/rutas.dart` una lista por dominio y
**se congela**:

```dart
final rutas = [...rutasIdentidad, ...rutasBilletera, ...rutasAlianzas, ...rutasPasanaku, ...rutasSoporte, ...rutasNotificaciones];
```

Cada carril llena **su** `pantallas/<dominio>/rutas.dart` con `go_router_builder`
(rutas tipadas). Un guard propio de un dominio (`soyOrg` en un grupo) se compone en ese
`rutas.dart`, no en el shell. **Prueba del gate:** agregar una pantalla vacía no toca
nada fuera del directorio del dominio.

Deep links (`aportaya://unirse/{codigo}`) llegan por `app_links` y se resuelven en el
shell; la pantalla destino no sabe si vino de un link o de un toque.

## Los estados no son opcionales

`AsyncValue<T>` de Riverpod **ya es** cargando / error / dato. Lo que no sabe es qué es
«vacío» ni cómo se ve el error en voz de marca; eso lo resuelve **un solo widget**:

```dart
EstadoDePantalla<List<Movimiento>>(
  valor: ref.watch(movimientosProvider),
  vacio: (lista) => lista.isEmpty,
  mensajeVacio: textos.sinMovimientos,        // dice por qué y qué hacer
  reintentar: () => ref.invalidate(movimientosProvider),
  exito: (lista) => ListaMovimientos(lista),
)
```

Pintar los estados con `switch` a mano en cada pantalla es cómo se olvida uno.

| Estado | Qué se ve |
| --- | --- |
| Cargando | `Esqueleto` del mismo tamaño que el contenido; sin saltos |
| Vacío | Por qué no hay nada y qué hacer |
| Error | Qué pasó en lenguaje humano + reintento |
| Enviando | `Boton(estado: cargando)`, deshabilitado |
| Sin conexión | `EstadoError.sinConexion`: último estado conocido, operaciones bloqueadas |

Una operación aceptada pero no confirmada (`202`) se muestra como **pendiente**, nunca
como éxito.

## Dinero y doble envío

- Toda operación con efecto envía **`Idempotency-Key`** generada al abrir el formulario
  (`proveedorIdempotencia(formularioId)`) y **reutilizada** en el reintento. El
  interceptor de `dio` la adjunta; la pantalla no la ve.
- Los importes se muestran con el átomo `Monto`; **el cliente nunca recalcula**. Los
  importes llegan como `String` del cliente generado y viajan como `String`.
- Prohibido `NumberFormat`, `toStringAsFixed`, `double.parse` sobre importes (lint).
- Confirmación explícita antes de cualquier operación irreversible, con el monto y el
  destinatario a la vista, en una `HojaDeConfirmacion`.
- La biometría **confirma, no autentica**: `local_auth` desbloquea el envío; el
  servidor autentica.

## Capacidades nativas — detrás de un puerto

| Necesidad | Puerto | Adaptador Android | Caso de uso |
| --- | --- | --- | --- |
| Escanear QR | `Camara` | `mobile_scanner`; el permiso se pide **después** de explicar para qué | CU-21 |
| Guardar credenciales | `AlmacenSeguro` | `flutter_secure_storage` (Keystore); nunca `SharedPreferences` | CU-04 |
| Biometría | `Biometria` | `local_auth` | CU-04 |
| Dispositivo de confianza | `Dispositivo` | identificador **generado** y guardado en el almacén seguro; nunca IMEI ni `androidId` | CU-04 |
| Notificaciones | `AvisosPush` | `firebase_messaging`; el contenido no revela montos ni datos personales | Módulo 05 |
| Bandeja de avisos | — | Lee `bandeja_entrada`, que es la fuente; el push es solo el aviso ([[ADR-035 Canales por defecto]]) | Módulo 05 |
| Conectividad | `Conectividad` | `connectivity_plus` **más una sonda al gateway** | todos |
| Captura de pantalla | `ProteccionPantalla` | `screen_protector` (`FLAG_SECURE`), activado por ruta | vistas con saldo |
| Subir un archivo | — | `multipart` al servicio dueño; vuelve una **clave de objeto** ([[ADR-034 Almacenamiento de archivos]]) | CU-02, CU-22 |

**Los siete puertos se declaran en F2 aunque su primer usuario llegue en F4.** Un puerto
nuevo a mitad de una ola es un micro-PR al shell que todos rebasan.

`Platform.isIOS` / `Platform.isAndroid` **no aparece en una vista**: lo que difiere por
plataforma vive en `infraestructura/`. Se verifica con `grep` en el gate.

Si se deniega un permiso, la app explica qué se pierde y ofrece una alternativa
(ingresar el código del QR a mano), en vez de quedarse en una pantalla muerta.

## Red intermitente

- Reintento manual visible en cada pantalla de dinero; el interceptor de reintento
  de `dio` **solo** aplica a `GET`.
- Caché de lectura (`keepAlive` + persistencia cifrada solo para lecturas) para que la
  app abra mostrando el último estado conocido, con marca de cuándo se actualizó.
- Timeouts cortos y mensajes concretos: «no pudimos confirmar tu aporte, revisá el
  estado antes de volver a pagar» es correcto; «error de red» no.
- La app verifica compatibilidad de contrato con el gateway al iniciar; si es
  incompatible, pide actualizar en vez de fallar a mitad de un pago.

## Rendimiento en gama baja

- Listas **perezosas y paginadas** desde el primer día: `ListView.builder` o
  `SliverList` con paginación del servidor. Nunca un `Column` con cien hijos.
- `const` en todo widget que pueda serlo; `select` en Riverpod para no reconstruir de más.
- Imágenes con `cacheWidth`; nada que no se vea se descarga.
- Sin animaciones que bloqueen en pantallas de dinero; `MediaQuery.disableAnimations`
  respetado.
- Se mide con `flutter run --profile` en un **dispositivo real de gama baja**, no en el
  emulador.

## Actualizaciones

- **Shorebird** para la capa Dart: un texto obligatorio, un tarifario o un mensaje de
  cumplimiento se corrige en horas, con un canal por entorno.
- Cualquier cambio que toque código nativo (un *plugin* con Android/iOS, un SDK de
  KYC) requiere build y revisión de tienda: se planifica por versión. El CI no publica
  un parche si el diff toca `android/`, `ios/` o un *plugin* nativo.

## Pruebas

| Nivel | Herramienta | Archivo |
| --- | --- | --- |
| Átomo | `flutter_test` + **golden en claro y oscuro**, `textScaler` 2.0 | `test/widget/atomos/` · `test/goldens/` |
| Molécula | `flutter_test` | `test/widget/moleculas/` |
| Organismo | `flutter_test` + `http_mock_adapter` con los ejemplos de `packages/simulado` + `ProviderContainer` con *overrides* | `test/widget/organismos/` |
| Contrato | el ejemplo valida contra el esquema del OpenAPI | `test/contrato/` |
| Accesibilidad | `meetsGuideline(textContrastGuideline)` · `androidTapTargetGuideline` · `labeledTapTargetGuideline` | `test/a11y/` |
| E2E | **Patrol** en `integration_test/`: permisos nativos, modo avión, notificaciones | `integration_test/` |

Las cinco pruebas obligatorias de toda pantalla con datos: cuatro estados · doble envío
· sin conexión · accesibilidad · contrato. Un golden **no se actualiza sin mirar la
diferencia**, y va en un commit propio con la imagen en el PR.

## Lint (`custom_lint`)

`sin_red_en_vista` · `tipos_del_contrato` · `sin_literal_de_diseno` (`Color(`, `Colors.`,
`EdgeInsets` numérico, `fontSize:`, `fontFamily:`) · `sin_formato_de_dinero` ·
`capas_front` · `tamano_widget` · `sin_print` · `sin_plataforma_en_vista` · `semantics_en_tocables`.

## Accesibilidad

`Semantics(button: true, label: …)` en todo átomo tocable; área táctil ≥ 48 dp; contraste
AA; `textScaler` hasta 2.0 sin romper; TalkBack recorriendo alta, aporte y retiro en
F12. Una billetera la usa gente de todas las edades; esto no es opcional.

## Antipatrones

- `dio` o `http` dentro de un widget.
- `Colors.green`, `Color(0xFF…)`, `EdgeInsets.all(13)` fuera de `tokens.dart` y del tema.
- Un `MaterialApp(routes: {…})` a mano: es el registro central que el enchufe evita.
- `Platform.isIOS` en una pantalla.
- Reglas de negocio que solo existen en el cliente.
- Recalcular importes para mostrarlos; un importe tipado como `double`.
- Guardar el token en `SharedPreferences`.
- Un `StatefulWidget` de 600 líneas con los ocho pasos del alta adentro.
- `--update-goldens` a ciegas.

## Ver también

`disenar-frontend` · `web-angular` · `errores-api` · `glosario-dominio` · `arquitectura-atomica` ·
`contratos-api` · `dinero-decimal` · `idempotencia-reintentos` · `seguridad-aplicacion` ·
[[ADR-044 Frontend en Angular y Flutter]] · [[ADR-036 Android primero]] ·
[[Flujo de pantallas · app del participante]] · `docs/Arquitectura/Prompts/Prompt de frontend.md`
