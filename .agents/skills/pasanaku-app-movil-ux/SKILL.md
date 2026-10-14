---
name: pasanaku-app-movil-ux
description: UX de la app móvil de AportaYa (Flutter) — áreas táctiles, safe areas, teclado (hojas y formularios que no quedan tapados), hápticos, permisos del sistema, cámara, sesión bancaria (sin sesión al ingreso, candado de 60 s / 5 min, tope de 8 h) y la versión web de la misma app. Usar al diseñar o revisar cualquier pantalla de apps/movil, cuando algo «en el celular se siente incómodo», o al tocar navegación, sesión o hardware. Traduce al stack real la skill de Atlas `atlas-app-movil-ux`.
---

# UX de la app móvil de AportaYa

La app es **Flutter** (Android, iOS y, como demostración, web dentro de `ComoEnUnTelefono`). Las
respuestas concretas son de Flutter; los principios vienen de Atlas y de la banca móvil.

## 1. Sesión: lo primero que se rompe y lo que más duele

Pablo lo dijo «mil veces»: **sin sesión, al ingreso, a la fuerza.** Y «la sesión, como es una app
bancaria, debe durar el estándar».

- **Una sola fuente de verdad**: `Sesion` (`proveedores/sesion.dart`), observable. El enrutador la
  escucha (`refreshListenable`) y decide con **una** función pura (`decidirRuta`). Ninguna pantalla
  implementa su propio guard de acceso, y ninguna navega al ingreso a mano tras un cierre: lo hace
  el guardián. Una pantalla sí puede leer tokens para una operación concreta.
- **Clasificación de rutas cerrada**: `soloSinSesion` (portada, tour, ingreso, alta), `publica`
  (pasos del alta antes de existir la cuenta, segundo factor), `privada` (todo lo demás, por
  omisión). La prueba mantiene un inventario explícito de rutas y además verifica que una ruta
  sintética sin clasificar cae en privada; no recorre automáticamente el árbol registrado.
- **Duración estándar**: acceso de 15 min renovado en silencio con refresco **rotado**; tope absoluto
  de **8 h** desde el último ingreso con contraseña; el servidor revoca la familia si alguien reusa un
  refresco.
- **El candado** (la «vista que habíamos dicho»): se tapa la app al abrir en frío, al volver tras
  **más de 60 s** en segundo plano y tras **5 min** sin toques. Una máquina de estados pura
  (`Vigilante`), probada con reloj falso, decide cuándo cubrir. Se intenta biometría si está
  disponible; si no, la vista ofrece «Ingresar de nuevo». El candado **no** cierra la sesión; el
  tope sí.
- **Cerrar sesión**: intenta revocar en el servidor con plazo (5 s, ≤ 2 intentos) y después borrar
  los tokens locales; un fallo de red no impide el cierre local, pero un fallo del almacén seguro
  puede impedir borrar tokens y cambiar a anónimo. Los providers de cuenta observan
  `generacionDeSesionProvider` y se reinician para no reutilizar datos de la persona anterior.
- **Motivo de salida**: si la sesión se cerró sin pedirlo, el ingreso lo dice una vez («Por seguridad,
  tu sesión dura 8 horas…» / «Tu sesión se cerró…»).

## 2. Áreas táctiles

- `Tactil.minimo` = **48** (el mayor entre 44 de iOS y 48 de Material: un solo criterio).
- Un control pequeño pegado a un texto (el ⓘ de un campo, el ojo de la contraseña, «Listo» de una hoja)
  agranda su **área**, no su dibujo: `SizedBox(width: 48, height: 48, child: Center(child: dibujo))`.
  Nunca márgenes negativos (en Atlas desplazaban el dibujo 12 px).
- Separación mínima entre objetivos: `Espacio.s2`.

## 3. Safe areas, teclado y gestos

- `SafeArea`/`MediaQuery.paddingOf(context)` siempre; un pie de pantalla suma el inferior. La cámara a
  pantalla completa **no** lleva `SafeArea` arriba: lo llevan sus barras.
- **El teclado nunca tapa lo que se está usando.** Formularios: `Scaffold(resizeToAvoidBottomInset:
  true)` (por omisión) + `Scrollable.ensureVisible` al enfocar el campo y al mostrar un error. Hojas:
  el alto se calcula **restando** `MediaQuery.viewInsetsOf(context).bottom` y la hoja sube con
  `AnimatedPadding` (ver `hoja_de_seleccion.dart`). Probar con el teclado **real** del sistema, no
  solo con `viewInsets` simulado (`mobile-native`).
- Android: `windowSoftInputMode="adjustResize"` en `AndroidManifest.xml`.
- **Volver atrás**: toda pantalla tiene su botón de volver o de cerrar; el gesto del sistema
  (`PopScope`) hace lo mismo que el botón. El candado bloquea el atrás.
- El error del servidor se pinta arriba y el botón está abajo: **llevar la vista hasta el error**.

## 4. Hápticos (política deseada; aplicación aún no centralizada)

`selectionClick` al elegir algo (opción de una hoja, segmento, página), `lightImpact` al tocar el botón
primario, `mediumImpact` al confirmar algo que salió bien (foto tomada, cuenta creada, pago),
`heavyImpact` solo para un error que bloquea dinero. **Nunca** en cada tecla ni en cada desplazamiento:
un háptico en todo deja de significar algo. `proveedores/haptica.dart` expone un puerto, pero no
impone esta política: aún hay llamadas directas a `HapticFeedback` en pantallas/controladores y el
proveedor actual implementa `toqueLigero` como `selectionClick`. En web el adaptador no hace nada.

## 5. Permisos y hardware

- Se piden **en el momento de usarlos**, con una pantalla previa que explica para qué («Necesitamos
  tu cámara · Solo se usa para tu prueba de vida»).
- Tres estados distintos: concedido, denegado (ofrecer volver a pedir), **bloqueado para siempre**
  (mandar a Ajustes: el sistema no vuelve a preguntar).
- Cámara: solo por el puerto `CamaraEnVivo`; liberar siempre al salir. Detector de rostro: puede no
  existir (web, emulador sin servicios) ⇒ la pantalla **degrada** a cuenta atrás, nunca se traba.
- Cámara y detector necesitan dispositivo real para verificarse: el emulador no alcanza.

## 6. Estados y espera

- Toda vista con datos: **cargando (esqueleto), vacío (que orienta), error (accionable, con
  referencia), sin permiso, sin conexión** (`frontend-ux-states`).
- Un botón apagado dice **por qué**, debajo, como instrucción.
- Nada en pasado sobre lo que no ocurrió («Te enviamos un código» antes de enviarlo).
- La prueba de vida, las subidas y los escáneres deben ofrecer las salidas que admita cada operación:
  cancelar o reintentar cuando corresponda; sin detector, la prueba de vida ofrece «Tomar ahora».
  La API actual de captura no ofrece «Usar esta foto igual» después de un rechazo local; no prometas
  esa acción si no está implementada.

## 7. La misma app en web

`flutter run -d chrome` sirve para revisar diseño, pero: sin biometría, sin detector de rostro, sin
cámara en HTTP, sin hápticos, almacén seguro emulado. Toda pieza de hardware tiene que **degradar con
sentido** ahí. Las capturas de verificación se toman en un dispositivo o emulador.

## 8. Cómo se prueba

- Unidad: lógica pura (guardián, vigilante, ciclo de pose, validaciones) con relojes falsos.
- Widget: `testWidgets` con `ProviderScope(overrides: …)`, almacén en memoria, cámara falsa,
  `tester.view` para tamaños y teclado.
- A11y: `test/a11y` (semántica, tamaños de toque con `meetsGuideline(androidTapTargetGuideline)`,
  contraste con `textContrastGuideline`).
- E2E: `patrol` en dispositivo (opcional).
- Visual: capturas a 360, 412 y 600 dp, claro y oscuro (`visual-proof`).

## Checklist

- [ ] Sin sesión no se ve nada privado; mantener el inventario explícito de rutas alineado con el router y probar también el fallback de rutas desconocidas.
- [ ] Todo lo tocable llega a 48 dp.
- [ ] El teclado no tapa ni el campo enfocado ni la lista de una hoja; probado con teclado real.
- [ ] Auditar los usos hápticos contra la política deseada; el puerto actual no centraliza ni impone todas las llamadas.
- [ ] Si toca hardware, degrada con sentido (web, sin permiso, sin detector).
- [ ] Vacío, cargando, error y sin permiso existen y los vi.
