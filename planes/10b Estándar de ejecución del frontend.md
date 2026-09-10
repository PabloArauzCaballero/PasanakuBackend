---
tags:
  - moc
  - plan
  - frontend
  - estandar
titulo: "Estándar de ejecución del frontend"
fecha: 2026-09-09
aplica_a: las 15 fases del frontend, sin excepción · Flutter y Angular
---

# Estándar de ejecución del frontend

> **Anexo de [[00b Estándar de ejecución · código limpio, pruebas y calidad]].**
> Todo lo que dice el estándar general —regla cero, KISS, nombres, funciones,
> errores, revisión por riesgo, definición de terminado— **también rige acá**. Este
> documento agrega solo lo que es propio de la interfaz, y lo dice **para los dos
> mundos**: cuando una regla se implementa distinto en Dart y en TypeScript, están las
> dos formas una al lado de la otra.

Fuentes que hace operativas: [[Prompt de frontend]] · skills `disenar-frontend`,
`movil-flutter`, `web-angular`, `web-backoffice`, `arquitectura-atomica`,
`glosario-dominio`, `errores-api`, `dinero-decimal`, `contratos-api`.

---

## 1 · Regla cero aplicada a la interfaz

**No se inventa una pantalla.** Cada caso de uso trae su sección **Interfaz** con lo
que la app y el backoffice deben mostrar. Se lee y se implementa eso.

| Situación | Qué se hace |
| --- | --- |
| El CU dice qué pantalla es | Se implementa **eso**, con esas palabras |
| El CU dice «sin pantalla» | **No se hace pantalla.** Que exista una tabla no significa que tenga UI |
| Falta un texto, un estado o un campo | Se busca en el CU, en `glosario-dominio` y en la identidad de marca. Si no está y es crítico, **se pregunta** |
| Falta un ícono, una animación, un matiz visual | Se decide y se **declara el supuesto** |
| La maqueta de F0.0 lo muestra de una manera y el CU de otra | Manda el CU; la divergencia se declara en el informe |

**No se inventan datos de ejemplo con apariencia real.** Nada de nombres de personas
verosímiles, cédulas, montos que parezcan reales o capturas que simulen una cuenta
existente. Los fixtures salen de `packages/simulado/ejemplos/` o son obviamente
ficticios.

---

## 2 · Los cuatro estados, sin excepción

Toda pantalla o sección que dependa de datos remotos implementa **los cuatro**, y en
flujos con efecto suma dos más:

| Estado | Qué debe verse | Flutter | Angular |
| --- | --- | --- | --- |
| **Cargando** | Indicación clara, **sin salto de layout** | `AsyncValue.loading` → `Esqueleto` | `recurso.isLoading()` → `<ap-esqueleto>` |
| **Vacío** | Por qué no hay nada **y qué hacer** | `EstadoVacio(motivo:, accion:)` | `<ap-estado-vacio>` |
| **Error** | Qué pasó en lenguaje humano **y un botón de reintento** | `AsyncValue.error` → `EstadoError(reintentar:)` | `recurso.error()` → `<ap-estado-error (reintentar)>` |
| **Éxito** | El dato, con formato y unidad explícita | `AsyncValue.data` | `recurso.value()` |
| **Enviando** | Botón deshabilitado con progreso | `Boton(estado: BotonEstado.cargando)` | `<ap-boton [cargando]="true">` |
| **Sin conexión** | Último estado conocido, operaciones bloqueadas | `proveedorConexion` + `EstadoError.sinConexion` | `servicioConexion` + `variante="sinConexion"` |

> **El camino es uno solo.** En Flutter, `EstadoDePantalla<T>` recibe el `AsyncValue<T>`
> y el predicado de vacío, y es el único widget que sabe pintar los cuatro. En Angular,
> la directiva `*apEstadoDePantalla` hace lo mismo sobre un `ResourceRef`. Una pantalla
> que pinta los estados «a mano» con `if` es una pantalla que se va a olvidar uno.

**El contrato, igual en los dos mundos** (lo construye F1; el carril solo lo llama):

```dart
// Flutter · packages/diseno_flutter/lib/organismos/estado_de_pantalla.dart
EstadoDePantalla<List<Movimiento>>(
  valor: ref.watch(movimientosProvider),          // AsyncValue<T>
  vacio: (lista) => lista.isEmpty,                // qué es «vacío» para esta pantalla
  mensajeVacio: textos.sinMovimientos,            // por qué y qué hacer — nunca «no hay datos»
  motivoVacio: MotivoVacio.sinDatos,              // sinDatos · porFiltro · porPermiso: se ven distinto
  reintentar: () => ref.invalidate(movimientosProvider),
  esqueleto: const EsqueletoDeLista(filas: 6),    // del mismo tamaño que el contenido: sin salto
  exito: (lista) => ListaMovimientos(lista),
)
```

```html
<!-- Angular · @aportaya/ui/estado-de-pantalla -->
<ng-container *apEstadoDePantalla="movimientos; vacio: esVacio; mensajeVacio: textos.sinMovimientos;
                                    motivoVacio: 'porFiltro'; esqueleto: esqueletoDeTabla; let lista">
  <ap-tabla-de-datos [filas]="lista" />
</ng-container>
```

Lo que garantiza el organismo, y por eso no se reimplementa: el esqueleto ocupa el
mismo alto que el contenido; el error muestra el mensaje **ya traducido** por el
catálogo y un botón de reintento que llama al `reload`; sin conexión pinta la variante
`sinConexion` con el último dato y la marca de cuándo se actualizó; y los tres motivos
de vacío tienen texto y acción distintos (**«no hay» ≠ «el filtro no devolvió» ≠ «no
tenés acceso»**).

> **En el backoffice el estado vacío importa el doble:** «no hay alertas» y «el filtro
> no devolvió nada» son mensajes **distintos** y se distinguen. Y una pantalla vacía
> **por permiso** lo dice explícitamente —«no tenés acceso a esto»—, nunca una tabla
> vacía que parece un error de datos (`web-backoffice`).

> **Un `EstadoVacio` que dice «No hay datos» no cumple.** Tiene que decir por qué está
> vacío y qué puede hacer la persona: *«Todavía no tenés movimientos. Cuando recargues
> saldo van a aparecer acá.»*

### El `202` no es éxito

Una operación aceptada pero no confirmada se muestra como **pendiente**, con su estado
actualizándose. Mostrarla como exitosa es decirle a la persona que su plata se movió
cuando todavía no se sabe (`movil-flutter`, `errores-api`).

### Red intermitente — las cuatro reglas

| Regla | Cómo se ve |
| --- | --- |
| **Nada de reintentos automáticos silenciosos** sobre operaciones con efecto | Reintento **manual y visible** en cada pantalla de dinero. En `dio` y en `HttpClient`, el interceptor de reintento **solo** aplica a `GET` |
| Caché de lectura con **marca de cuándo se actualizó** | La app abre mostrando el último estado conocido, fechado. Riverpod `keepAlive` + persistencia en disco cifrada solo para lecturas |
| Timeouts cortos, **mensajes concretos** | ✅ «No pudimos confirmar tu aporte, revisá el estado antes de volver a pagar» · ❌ «Error de red» |
| Compatibilidad de contrato **verificada al iniciar** | La app compara la versión del contrato que trae compilada con la que publica el gateway; si es incompatible, pide actualizar **en vez de fallar a mitad de un pago** |

### Permiso nativo denegado

Si la persona deniega la cámara, la app **explica qué se pierde y ofrece una
alternativa** —ingresar el código del QR a mano—, en vez de quedarse en una pantalla
muerta. El permiso se pide **después** de explicar para qué, nunca al abrir la app
(`movil-flutter`).

### Notificaciones

**El contenido de una notificación no revela montos ni datos personales.** Aparece en
la pantalla de bloqueo de un teléfono que puede estar sobre una mesa. La notificación
es el aviso; **la bandeja de la app es la fuente** ([[ADR-035 Canales por defecto]]).

---

## 3 · Diseño: los tokens son la única fuente

De la skill `disenar-frontend`, que manda sobre cualquier criterio estético:

> **Verde = estructura. Naranja = acción.** El naranja `#E5852B` se reserva para el
> **único** llamado a la acción principal de cada pantalla. Nunca dos naranjas
> compitiendo.

| Regla | Valor | Flutter | Angular |
| --- | --- | --- | --- |
| Fuente | `packages/tokens/tokens.json` | `tokens.dart` generado → `ThemeExtension<Tokens>` | `tokens.css` generado → `var(--…)` |
| Espaciado | Múltiplos de 4: `s1 4` … `s7 48` | `Tokens.of(context).s4` | `var(--s4)` |
| Radio | `r-sm 8` · `r-md 12` · `r-lg 16` · `r-xl 24` · `r-pill 999` | `Tokens.of(context).rMd` | `var(--r-md)` |
| Tipografía | `Poppins` display/cifras · `Inter` cuerpo/UI | `TextTheme` del tema, nunca `TextStyle(fontFamily:)` | `var(--font-d)` · `var(--font-b)` |
| **Dinero** | `tabular-nums`, prefijo `Bs`, coma decimal → **`Bs 1.240,00`** | widget `Monto` con `FontFeature.tabularFigures()` | componente `ap-monto` con `font-variant-numeric: tabular-nums` |
| Táctil móvil | Área mínima **48×48 dp** (Flutter) · 44 px (web móvil) | `androidTapTargetGuideline` en la prueba | `min-height: var(--tactil)` |
| Tema | Claro y oscuro **redefinen solo tokens** | `ThemeMode.system` + los dos `ThemeData` desde tokens | `prefers-color-scheme` + `data-theme` |

**Cero literales.** Un `#fff`, un `padding: 13px`, un `Color(0xFF...)`, un
`EdgeInsets.all(13)` o un `fontFamily:` dentro de un componente es un fallo de lint, no
una preferencia.

**Dos implementaciones, una revisión.** El átomo `Boton` de Flutter y el `ap-boton` de
Angular se revisan **lado a lado** en el punto de sincronización: el golden de uno
contra la captura del otro. Lo que difiera se corrige en el que se apartó de
`docs/Views/Sistema-Diseno/`.

---

## 4 · La voz de la interfaz

De la identidad de marca: **cercana al hablar, impecable con el dinero.**

| Sí | No |
| --- | --- |
| «Listo, tu aporte de Bs 250 quedó guardado.» | «Transacción procesada exitosamente.» |
| «Te toca en 2 turnos. Te avisamos.» | «Posición en cola: 3.» |
| «Confirmar aporte de Bs 250» | «Aceptar» |
| «No pudimos cobrar. Revisá tu saldo e intentá de nuevo.» | «Error 409.» |
| «Tu grupo», «tu turno», «tu aporte» | «El usuario», «la entidad» |

**El vocabulario es el de `glosario-dominio`**: grupo, cupo, turno, período, aporte,
entrega. Nunca `member`, `payment`, `round`. Si el modelo dice `obligacion_aporte`, la
pantalla dice «aporte», no «cuota pendiente de pago».

**Los textos viven en un archivo por dominio de pantallas**, en el directorio del
carril: `textos.dart` en Flutter, `textos.ts` en Angular. No hay un archivo global de
traducciones (conflicto nº 3 de [[16 Carriles de frontend]] §5). El idioma es uno
(`es-BO`); si algún día hay otro, se usa `intl` (Flutter) y `@angular/localize`, y
ese día lleva ADR.

**Los errores se traducen.** El usuario ve el mensaje del código `AP-CU<NN>-<nn>` en
lenguaje llano; nunca el código, nunca el texto de PostgreSQL, nunca «error
inesperado» a secas. El catálogo de traducciones es **uno por producto**, versionado,
y lo consume el interceptor de errores; ninguna pantalla traduce por su cuenta.

---

## 5 · Dinero en el cliente

| Regla | Por qué |
| --- | --- |
| **El cliente no calcula importes.** Muestra lo que el servidor devolvió | Invariante 5 del backend: la exactitud es cumplimiento |
| Todo importe pasa por el átomo **`Monto`** | Un solo formateador por mundo, y **los dos pasan los mismos vectores** de `packages/tokens/vectores/monto.json` |
| Los importes llegan y viajan como ***string*** | `numeric` se lee como cadena de punta a punta; el cliente generado los tipa como `String`, no como `double` |
| Prohibido `toFixed`, `Number()`, `parseFloat`, `double.parse`, `toStringAsFixed` sobre importes | Regla de lint en los dos mundos |
| Una comisión se muestra como **línea con nombre**, nunca como descuento anónimo | CU-31, transparencia ASFI |
| El costo total **con impuestos** se ve **antes** de confirmar | CU-30, `R-CON-07` |

> **Si el generador de Dart mapea un importe a `double`, el generador está mal
> configurado**, no la regla. Los importes del contrato llevan `format: decimal` y
> `type: string`; `dart-dio` los deja como `String`. Es lo primero que verifica la
> prueba de contrato de `Monto`.

---

## 5b · Tablas del backoffice — el organismo central

Casi todo el backoffice es una tabla con filtros. Reglas fijas de `web-backoffice`:

| Aspecto | Regla | En Angular |
| --- | --- | --- |
| Paginación | **Del servidor, siempre.** Nada de traer todo y filtrar en el cliente | `httpResource` con `pagina` y `tamano` como señales de entrada |
| Orden y filtros | **Por lista blanca** acordada con la API | El tipo del campo de orden es una unión literal generada del contrato |
| **Estado en la URL** | Un oficial tiene que poder **pegar el enlace de lo que está mirando en un expediente** | `withComponentInputBinding()`: los *query params* son entradas del componente |
| Virtualización | Para listas largas; el DOM no crece sin límite | `cdk-virtual-scroll-viewport` con altura fija de fila |
| Columnas de dinero | **Alineadas a la derecha**, con el átomo `Monto`, **moneda visible** | `<ap-monto>` en una celda `.numerica` |
| Fechas | **Con zona horaria explícita**; nada ambiguo en un expediente | átomo `ap-fecha` con `America/La_Paz` visible |
| Exportación | Del servidor, con los mismos filtros, y **queda registrada: quién exportó qué y cuándo** | `Exportador` llama al endpoint de CU-58; nunca `Blob` desde el cliente |
| Selección múltiple | **Solo si existe una acción masiva real**, con confirmación que **enumera lo afectado** | `SelectionModel` del CDK |

### Lo que distingue al backoffice de un CRUD

- **El plazo se muestra siempre**, y el vencimiento se destaca **antes** de vencer.
- **La evidencia se ve**: toda pantalla de expediente muestra la bitácora —quién hizo
  qué, cuándo, con qué resultado.
- **Nada se edita.** Corregir es registrar un movimiento o una decisión nueva, con
  motivo obligatorio. **Si la interfaz sugiere «editar», está mintiendo sobre el
  modelo.**
- **Motivo obligatorio** en toda acción con consecuencia: bloquear saldo, rechazar,
  elevar, devolver comisión.
- **Doble confirmación** en lo irreversible, enumerando exactamente qué va a pasar.
- La **segregación de funciones se respeta en la interfaz**: quien registra no aprueba.
  Si la interfaz permite ambas cosas al mismo usuario, **es un defecto**.
- Las pantallas de conciliación y reportes **leen de la réplica**: son consultas pesadas.

---

## 6 · Formularios y efectos

| Regla | Flutter | Angular |
| --- | --- | --- |
| Validación con **los tipos generados** y validadores derivados del contrato | `dominio/validacion.dart` (generado de `minLength`, `pattern`, `required`) | `validarConContrato(esquema)` como validador de Signal Forms |
| Error **por campo, en el campo**, diciendo cómo corregir | `CampoDeFormulario(error:)` | `<ap-campo [error]>` con `aria-describedby` |
| **Clave de idempotencia**: se genera al abrir el formulario y se reenvía igual en el reintento; **con forma UUID**, que es lo que el contrato declara | `proveedorIdempotencia(formularioId)` | `claveDeIdempotencia()` en el componente, viaja por `HttpContextToken` |
| El botón que envía **se deshabilita y muestra progreso** | `Boton(estado: cargando)` mientras la mutación está `AsyncLoading` | `[cargando]="mutacion.enCurso()"` |
| Deshabilitar el botón **no es** la validación: se explica qué falta | resumen de faltantes bajo el botón | ídem |
| Confirmación explícita para lo irreversible, diciendo **qué** y **por cuánto** | `HojaDeConfirmacion` (bottom sheet) | `DialogoDeConfirmacion` (CDK overlay) |
| **Los formularios largos guardan borrador local** (reportes, expedientes) | — (la app no tiene formularios largos) | borrador cifrado en `IndexedDB`, sin datos personales en claro |

**Nunca se regenera la clave al reintentar.** Es la prueba de doble envío: dos toques,
una llamada, la misma cabecera `Idempotency-Key`.

---

## 7 · Accesibilidad — bloqueante, no deseable

| Control | Umbral | Flutter | Angular |
| --- | --- | --- | --- |
| Contraste | **AA ≥ 4.5:1** en texto; 3:1 en componentes | `meetsGuideline(textContrastGuideline)` | `vitest-axe` · `@axe-core/playwright` |
| Foco | Visible siempre; nunca `outline: none` sin reemplazo | `FocusNode` con decoración del tema | `:focus-visible` con `outline 3px var(--g300)` |
| Teclado | Todo el flujo, sin trampas de foco | teclado físico en tablet probado en F12 | `cdkTrapFocus` en modales; `LiveAnnouncer` para errores |
| Semántica | Un botón es un botón | `Semantics(button: true, label:)` en todo átomo tocable | `<button>`; nunca `div (click)` — regla de plantilla como error |
| Etiquetas | Todo control con etiqueta asociada; error anunciado | `labeledTapTargetGuideline` | `aria-describedby`, `aria-live="polite"` |
| Tipografía ampliada | 200 % sin romperse | `MediaQuery.textScaler` hasta 2.0 en golden | zoom 200 % en Playwright |
| Movimiento | `prefers-reduced-motion` respetado | `MediaQuery.disableAnimations` | `@media (prefers-reduced-motion)` |
| Móvil | Área táctil ≥ 48 dp; sin desplazamiento horizontal | `androidTapTargetGuideline` | — |
| Lector de pantalla | Alta, aporte y retiro completos | TalkBack y VoiceOver (F12) | NVDA sobre el backoffice (F12) |

---

## 8 · Rendimiento

- **Listas largas: perezosas y paginadas desde el primer día.** En Flutter,
  `ListView.builder` / `SliverList` con paginación del servidor; en Angular,
  `cdk-virtual-scroll-viewport`. Nunca un `Column` con mil hijos ni un `*ngFor` sin
  virtualizar sobre una lista paginada.
- Caché de servidor con invalidación explícita. Nada de recargar todo tras cada acción.
- Imágenes dimensionadas y en formato moderno; en Flutter, `cacheWidth`; en Angular,
  `NgOptimizedImage`.
- **`const` donde se pueda** en Flutter; **`OnPush` implícito** (zoneless) en Angular:
  nada de `setState` global ni de `detectChanges()` a mano.
- **Se optimiza con medición**, no por intuición: `flutter run --profile` en un Android
  de gama baja; Lighthouse CI en el sitio.
- La app asume **red intermitente**: reintento manual claro y estado visible.

---

## 9 · Seguridad en el cliente

| Regla | Flutter | Angular |
| --- | --- | --- |
| Credenciales en almacenamiento seguro | `flutter_secure_storage` (Keystore / Keychain); jamás `SharedPreferences` | token solo en memoria; refresh en cookie `HttpOnly`; jamás `localStorage` |
| Ningún dato sensible en la URL, en el estado global ni en un log | `go_router` con ids opacos; sin PII en `extra` | ids opacos en rutas; sin PII en *query params* |
| Los permisos se muestran u ocultan **por comodidad**; se verifican en el servidor | guardias de `go_router` | `canMatch` funcionales |
| Número de cuenta **enmascarado** (últimos cuatro) | átomo `CuentaEnmascarada` | ídem |
| Ningún token, PIN ni documento en una traza | interceptor que redacta cabeceras | ídem |
| **Sin capturas de pantalla** en vistas con saldo y datos personales | `screen_protector` (`FLAG_SECURE` en Android) por ruta | — |
| CSP estricta, sin `unsafe-inline` | — | cabeceras en NGINX; `nonce` para SSR del sitio |
| Dispositivo de confianza | identificador estable en el almacén seguro; nunca el IMEI | — |

---

## 10 · Antes de abrir el PR

- [ ] Componentes declarados **por nivel** en la descripción del PR
- [ ] Los cuatro estados existen en cada pantalla con datos, **vía `EstadoDePantalla`**
- [ ] Cero literales de diseño fuera de `packages/tokens`
- [ ] Ninguna llamada de red dentro de un componente o widget
- [ ] Los tipos vienen del cliente generado; los ejemplos validan contra su esquema
- [ ] Un solo botón naranja por pantalla
- [ ] Dinero con `tabular-nums` y formato `Bs 1.240,00`, por `Monto`
- [ ] Teclado, foco y contraste verificados (web) · guías de accesibilidad de Flutter en verde (móvil)
- [ ] Claro y oscuro probados: goldens (Flutter) o capturas (Angular) actualizados **y revisados**
- [ ] Prueba de doble envío en todo flujo con efecto
- [ ] Copy en voz de marca y vocabulario del `glosario-dominio`, en `textos.*` del dominio
- [ ] Ningún componente pasa de ~150 líneas (una pantalla de 400 son varios organismos
      sin extraer)
- [ ] `yarn lint && yarn typecheck && yarn test:front && yarn test:a11y` en verde
- [ ] En Flutter: `grep -r "Platform.is" lib/` vacío fuera de `infraestructura/`

---

## 11 · Revisión de frontend — orden por riesgo

```
1. ¿Qué caso de uso implementa? ¿Coincide con su sección Interfaz?
2. Dinero: formato, cálculo en el cliente, comisión visible
3. Efectos: idempotencia, doble envío, confirmación
4. Los cuatro estados
5. Accesibilidad: teclado, foco, contraste, semántica
6. Composición: niveles y dirección de dependencia
7. Tokens y voz de marca
8. Estilo → lo resolvió la herramienta (dart format · prettier)
```

### Se rechaza sin discusión

| Hallazgo | Por qué |
| --- | --- |
| `HttpClient`, `fetch` o `dio` dentro de un componente | Invariante 1 |
| Un importe calculado o formateado a mano | La exactitud es cumplimiento |
| Un hex, un `px`, un `Color(0x…)` o un `EdgeInsets.all(13)` suelto | El sistema de diseño deja de existir al segundo |
| Una pantalla de datos sin sus cuatro estados | La mitad de la interfaz no existe |
| Los cuatro estados pintados con `if` en vez de `EstadoDePantalla` | Se va a olvidar uno en la pantalla siguiente |
| Un flujo con efecto sin bloqueo de doble envío | Duplica dinero |
| Un tipo reescrito a mano que ya está en el cliente generado | Diverge en la cuarta semana |
| `div (click)` · `GestureDetector` sin `Semantics` | Inaccesible |
| Un ejemplo que no valida contra su esquema OpenAPI | La pantalla ya está rota y no lo sabe |
| Traer diez mil filas para filtrar en el navegador | La tabla se pagina en el servidor |
| Un botón «editar» sobre algo append-only | La interfaz miente sobre el modelo |
| Acción masiva sin confirmación que enumere lo afectado | Irreversible a ciegas |
| Exportar sin registrar quién exportó | `R-SEG-02`, CU-58 |
| Una notificación que muestra el monto | Aparece en la pantalla de bloqueo |
| Un `202` mostrado como éxito | Dice que la plata se movió cuando no se sabe |
| Reutilizar el layout de la app para pantallas densas | El backoffice no es la app estirada |
| Una regla de negocio que solo vive en el cliente | La garantía está en el lugar equivocado |
| Datos de ejemplo con apariencia de reales | Regla cero |
| `Platform.isIOS` en una vista | ADR-036: eso va en el adaptador |
| Un `routes.ts` / `rutas.dart` compartido editado por un carril de pantallas | Conflicto nº 1 de [[16 Carriles de frontend]] |
| Un golden actualizado sin revisar la diferencia | «Actualizar goldens» a ciegas aprueba cualquier regresión visual |

## Ver también

[[00b Estándar de ejecución · código limpio, pruebas y calidad]] · [[00c Recetario · implementar un caso de uso]] · [[10 Plan maestro del frontend]] · [[16 Carriles de frontend]] · [[Prompt de frontend]] · [[ADR-044 Frontend en Angular y Flutter]]
