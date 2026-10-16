---
name: atlas-app-movil-ux
description: UX de la app del cliente de Atlas (Expo + React Native + expo-router) — áreas táctiles y `hitSlop`, safe areas, teclado, hápticos, permisos del sistema, y las diferencias que aparecen cuando la MISMA app se sirve en el navegador. Usar al diseñar o revisar cualquier pantalla de la app, al portar algo a la web, o cuando algo «en el celular se siente incómodo».
---

# UX de la app del cliente

Sustituye a la guía genérica de UX móvil, que estaba escrita para **Flutter** (widgets, temas de
Material). La app de Atlas es **Expo 57 · React Native 0.86 · expo-router · Reanimated 4**, y la
misma base se sirve además en el navegador con react-native-web. Eso cambia casi todas las
respuestas concretas.

## 1. Una app, dos destinos

Las 42 pantallas son las mismas en el teléfono y en el navegador. El aspecto de escritorio se añade
por fuera (`src/web/`), nunca editando una pantalla, y **a 390 px la web tiene que ser idéntica al
teléfono**. Al escribir una pantalla hay que asumir que va a correr en los dos sitios.

**Cinco cosas que sólo rompen en web y que `tsc` no ve** (medidas, no teóricas):

1. `expo-secure-store` **lanza** en web → hay un `token-storage.web.ts`.
2. El `entering` de Reanimated apila los bloques → animar por valor compartido.
3. `play()` de audio sin gesto llega como error de página → mirar `navigator.userActivation`.
4. `expo-file-system` es una cáscara que devuelve vacío **sin error** → `device/archivos.ts` con
   fetch de `blob:`/`data:`.
5. El datetimepicker no existe → `<input type=date>`.

Y la sexta, que muerde igual: **`hitSlop` no existe en react-native-web** (0.21.2). Los controles
pequeños medían lo dibujado —8×8 px los puntos del carrusel, 20×20 el ojo del PIN—, por debajo de los
24 px de WCAG 2.2 (2.5.8). Solución: `toqueWeb()` / `data-toque`, un `::before` absoluto con `inset`
negativo (ver `src/ui/hit-slop.ts`). **No** relleno + margen negativo: eso desplazaba el dibujo 12 px
en cada cabecera.

## 2. Áreas táctiles

- `touch.minSize` es **48** (el mayor entre los 44 de iOS y los 48 de Material, para no tener dos
  criterios según plataforma). `touch.minSpacing` 8.
- Para un control pequeño pegado a un texto —el ⓘ de un campo, el ojo del PIN, «Listo» de una hoja,
  un chip— se agranda el área sin mover el dibujo: `hitSlop` en el teléfono y `toqueWeb()` en la web,
  con una de las medidas del catálogo de `TOQUES`.
- `e2e-web/responsive.mjs` mide esto por ancho y falla si algo visible queda por debajo de 24×24.

## 3. Safe areas, teclado y gestos

- `Screen` ya resuelve los márgenes del sistema con `react-native-safe-area-context`; el pie suma
  `insets.bottom`. Una pantalla que se dibuja «a pelo» acaba con el botón bajo la barra de gestos.
- El campo con foco tiene que quedar por encima del teclado. En formularios largos, el error se
  pinta arriba y el botón está abajo: hay que **llevar la vista hasta él** (`useScrollToError`).
- Volver atrás: el botón redondo de `ScreenHeader` (`onBack="auto"`) es el que sabe volver al paso
  anterior. Tres pantallas se escribieron su propio encabezado y el precio fue quedarse SIN botón de
  volver: sólo salía quien conociera el gesto del sistema.

## 4. Hápticos

`Button` los da según `haptic`: `light` por defecto, `success` para confirmar algo que salió bien,
`warning` para lo que no. No van en web (no existen) y no se ponen en cualquier sitio: un háptico en
cada toque deja de significar nada.

## 5. Permisos del sistema

- Se **consultan** en el alta, no se piden de golpe: lo que viaja es el estado real del dispositivo.
- El canal de avisos de Android se crea **al arrancar**, no al conceder el permiso: una notificación
  dirigida a un canal inexistente se descarta en silencio, y el permiso puede venir concedido de una
  sesión anterior.
- Cámara y GPS exigen **HTTPS**: en el TEST de Contabo, que va por HTTP plano, la cámara no abre. Eso
  no es un fallo de la pantalla; es el contexto seguro.
- Sólo 9 de las 42 pantallas tocan hardware (cámara en `escanear` e `identidad`, GPS en `domicilio`,
  agenda en `referencias`, archivos en `extracto-bancario`, galería en `pago`/`pagar`/`soporte`,
  push en `preferencias-avisos`). Las demás deben poder correr sin nada de eso.

## 6. Estados y espera

- Toda vista que trae datos: vacío, cargando, error y sin permiso (`frontend-ux-states`,
  `EmptyState`/`ErrorState`/`Skeleton`).
- Un botón apagado dice por qué, y lo dice **como instrucción**, no como alarma: el motivo va debajo,
  en tono secundario, no en ámbar con triángulo. Una advertencia que sale antes de que la persona
  pueda equivocarse deja de significar «revisa esto».
- Nada de textos en pasado sobre algo que aún no ocurrió. La pantalla de verificación decía «Te
  enviamos un código» sin haber pedido ninguno, y el alta se quedaba parada esperando un SMS que no
  existía (queda registrado en la memoria del proyecto, no en una skill).

## 7. Cómo se prueba

- `npm test` (incluye `check:field-help`: ningún campo sin «qué poner»).
- Componente montado con `@testing-library/react-native` — corre en el entorno nativo, así que cubre
  el teléfono de verdad.
- Recorridos con Maestro en el simulador (`animate={false}` en listas, o los toques fallan).
- En la web: `e2e-web/humo.mjs` (42 rutas) y `e2e-web/responsive.mjs` (13 anchos).
- Compilar para Android necesita `ANDROID_HOME` y JDK 21, y `adb reverse` para hablar con el backend
  local.

## Checklist

- [ ] La pantalla funciona en el teléfono **y** en el navegador; a 390 px se ven iguales.
- [ ] Todo lo tocable llega a 48, o lleva `hitSlop` **y** `toqueWeb()`.
- [ ] Márgenes del sistema y teclado resueltos por `Screen`, no a mano.
- [ ] Vacío, cargando, error y sin permiso existen y los vi.
- [ ] Si toca hardware, degrada con sentido donde no lo hay (web, HTTP, permiso denegado).
