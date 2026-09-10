---
tags:
  - plan
  - fase
  - frontend
titulo: "Fase F12 — Endurecimiento, accesibilidad, E2E y publicación"
fase: F12
depende_de: [F2, F3, F4, F5, F6, F7, F8, F9, F10, F11]
habilita: []
---

# Fase F12 — Endurecimiento, accesibilidad, E2E y publicación

> **Se ejecuta en:** Ola F4 · carril T (convergencia, máquina única).
> Ver [[16 Carriles de frontend]].

> [!important] Antes de escribir la primera línea
> [[10b Estándar de ejecución del frontend]] aplica. **Nada de pantallas nuevas en
> esta fase**: si aparece un caso de uso sin interfaz, pertenece a su fase.

> **Objetivo.** Que los tres productos aguanten uso real —Android de gama baja, red
> intermitente, lector de pantalla, jornada completa— y que estén publicados: la app
> en las tiendas, el backoffice y el sitio en producción.

## Gate de entrada

- [ ] Fases F2 a F11 cerradas con sus gates ejecutados
- [ ] El backend en Ola 5, con la API real disponible en un entorno de ensayo

---

## F12.1 · Suite E2E

### Web — Playwright + Chromium contra la API real

| Recorrido | Qué ejercita |
| --- | --- |
| `backoffice-cobranza.e2e.ts` | Login por rol → estado de cobranza → reclamo con vencimiento → cierre |
| `backoffice-cumplimiento.e2e.ts` | Alerta → caso → decisión → reporte remitido |
| `backoffice-doble-control.e2e.ts` | **La misma persona no puede autorizar y ejecutar** un reverso |
| `web-publico.e2e.ts` | Home → tarifas → reclamos, con metadatos y JSON-LD verificados |
| `web-verificacion.e2e.ts` | Verificar certificado, cadena y sorteo, **recomputando en el cliente** |
| `web-seo.e2e.ts` | Canonical, hreflang, sitemap, `robots.txt`, `.md` espejo, `noindex` donde corresponde |

### Móvil — Patrol sobre `integration_test`, en dispositivo real y en emulador

| Recorrido | Qué ejercita |
| --- | --- |
| `integration_test/alta_y_billetera_test.dart` | Registro con cámara → contrato → recarga → movimientos. **Patrol acepta el diálogo nativo de permiso de cámara** |
| `integration_test/aporte_completo_test.dart` | Entrar al grupo → *Mi aporte* → pagar con saldo → ver el movimiento |
| `integration_test/entrega_test.dart` | *Cobrar mi turno* → deducciones → neto → línea de tiempo |
| `integration_test/sin_conexion_test.dart` | **Patrol apaga la red del dispositivo**: último estado visible, operaciones bloqueadas |
| `integration_test/doble_envio_test.dart` | Dos toques rápidos en cada operación de dinero ⇒ **un** efecto, misma `Idempotency-Key` |
| `integration_test/notificacion_test.dart` | Una notificación push abre la pantalla correcta y no muestra el monto en la bandeja del sistema |
| `integration_test/deep_link_test.dart` | `aportaya://unirse/{codigo}` con la app cerrada y abierta |

> **Por qué Patrol y no solo `integration_test`.** `integration_test` no puede tocar lo
> que está fuera del árbol de Flutter: el diálogo de permiso, el panel de
> notificaciones, el modo avión. Patrol sí, y eso es exactamente lo que estas corridas
> necesitan probar. Corre en **P3 (emulador acelerado)** y en un **Android físico de
> gama baja** antes de publicar.

**Entregable F12.1:** las trece corridas en verde contra la API real: las seis de
Playwright en CI, las siete de Patrol en P3 y en dispositivo físico, con evidencia.

---

## F12.2 · Accesibilidad — auditoría completa

`vitest-axe`, `@axe-core/playwright` y las guías de accesibilidad de Flutter
(`meetsGuideline`) ya corren desde F1. Acá se audita lo que las herramientas no ven:

| Control | Cómo se verifica |
| --- | --- |
| **Lector de pantalla** | TalkBack en Android y VoiceOver en iOS, recorriendo alta, aporte y retiro completos |
| **Solo teclado** | Backoffice entero, sin mouse, incluidos tabla virtualizada, filtros y modales con `cdkTrapFocus` |
| **Lector en escritorio** | NVDA sobre el backoffice: la tabla anuncia fila, columna y celda; los errores de formulario se anuncian por `LiveAnnouncer` |
| **Contraste** | AA ≥ 4.5:1 verificado en claro y oscuro, pieza por pieza |
| **Tipografía ampliada** | 200 % sin romper ni cortar contenido (`textScaler` 2.0 en goldens; zoom 200 % en Playwright) |
| **Área táctil** | ≥ 48 dp en toda la app (`androidTapTargetGuideline` en todas las pantallas, no solo en átomos) |
| **Movimiento** | `prefers-reduced-motion` respetado |
| **Errores anunciados** | `aria-live` en cada validación de formulario |

> **Esto es una billetera para gente que quizá nunca usó una.** La accesibilidad acá
> no es cumplimiento normativo: es si el producto sirve o no.

---

## F12.3 · Rendimiento en el dispositivo real

| Métrica | Objetivo | Dónde | Cómo se mide |
| --- | :-: | --- | --- |
| Arranque en frío, Android gama baja | < 3 s | app | `flutter run --profile --trace-startup` |
| Lista de 5 000 movimientos | 60 fps al desplazar, cero *jank* | app | `flutter run --profile` + DevTools *timeline* |
| Tamaño del `appbundle` | medido y con presupuesto (`--analyze-size`) | app | `flutter build appbundle --analyze-size` |
| Tabla de 100 000 filas paginadas | < 100 ms al filtrar | backoffice | Playwright con `performance.now()` |
| *Bundle* inicial del backoffice | con presupuesto en `angular.json` | backoffice | `ng build` rompe si lo pasa |
| LCP / INP / CLS del sitio | § F10.4 | web | Lighthouse CI |
| JS inicial del sitio | ≤ 150 KB comprimidos | web | Lighthouse CI + `budgets` |

**Se mide en un dispositivo real de gama baja**, no en el emulador. El parque de
Android en Bolivia es lo que define si esto funciona. Los números van al informe del
carril con la fecha y el modelo del teléfono.

## F12.4 · Resiliencia del cliente

- **Red intermitente**: reintento manual claro, estado visible, sin datos perdidos.
- **Sesión expirada a mitad de un formulario**: se recupera el formulario tras
  reautenticar.
- **API caída**: mensaje en voz de marca con reintento, nunca una pantalla en blanco.
- **Respuesta lenta**: el botón muestra progreso y **no permite un segundo envío**.
- **Versión desactualizada**: la app detecta incompatibilidad de contrato y pide
  actualizar, en vez de fallar de forma rara.

---

## F12.5 · Seguridad del cliente

- Credenciales solo en `flutter_secure_storage` (app) y en memoria + cookie `HttpOnly`
  (backoffice); nada en `SharedPreferences` ni en `localStorage`.
- Captura de pantalla bloqueada en vistas con saldo y datos personales (`FLAG_SECURE`).
- La app no arranca en un dispositivo con *root* o *jailbreak* detectado sin avisar y
  degradar: operaciones de dinero bloqueadas con motivo (`flutter_jailbreak_detection`).
- Ningún token, PIN, documento ni número de cuenta en logs, trazas o capturas.
- CSP estricta en backoffice y sitio; sin `unsafe-inline`. En el sitio, `nonce` por
  petición para los `<script>` que emite el SSR de Angular.
- El paquete de la app no contiene `clientes/dart` de servicios que la app no consume ni
  URLs de entornos que no sean el de destino (`--dart-define` por sabor).
- Backoffice: `X-Robots-Tag: noindex`, HSTS, `X-Content-Type-Options`, sin
  `X-Powered-By`.
- Dependencias escaneadas; ninguna vulnerabilidad alta sin justificación.

---

## F12.6 · Publicación

| Producto | Cómo se publica | Qué hay que preparar |
| --- | --- | --- |
| **`apps/movil`** | `flutter build appbundle` → Google Play (P3 o P1) · `flutter build ipa` → App Store (**solo P1**) · **Shorebird** para parches de la capa Dart, con un canal por entorno | Fichas en español boliviano, capturas, **política de privacidad enlazada**, clasificación por edad, declaración de datos recolectados (*Data Safety* y *Privacy Nutrition Labels*). Firmas: *keystore* de Android y certificados de Apple **fuera del repositorio**, en el gestor de secretos |
| **`apps/backoffice`** | `ng build` → Docker (NGINX, estático) | Sin puerto público; acceso por red interna o VPN; `X-Robots-Tag`, CSP, HSTS |
| **`apps/web`** | `ng build` con SSR → Docker (Node tras NGINX) | Dominio, TLS, cabeceras, caché de NGINX para las rutas `Server`, `robots.txt`, `sitemap.xml`, verificación en Search Console y Bing |

**Shorebird no es un atajo para lo nativo.** Un parche que toca un *plugin* con código
Android o iOS **no se publica por Shorebird**: pasa por tienda. La regla se verifica en
el CI: `shorebird patch` corre solo si el diff no toca `android/`, `ios/` ni
`pubspec.lock` con *plugins* nativos.

### App Store Optimization

Es la contraparte del SEO para la app, y no la cubre la Fase F10:

- Título y subtítulo con «pasanaku» y «billetera», que es como la gente busca.
- Descripción con las diez preguntas de §F11.5 respondidas.
- Capturas que muestran **pantallas reales**, nunca maquetas con datos inventados.
- Palabras clave en español boliviano.
- **La ficha no promete lo que la licencia no permite todavía** (regla 2 de F9–F11).

---

## Gate de salida de la Fase F12 — y del frontend

### Funcionalidad
- [ ] Toda pantalla que la sección **Interfaz** de un CU exige, existe
- [ ] Las trece corridas E2E en verde contra la API real (seis Playwright, siete Patrol)
- [ ] Los cuatro estados en cada pantalla con datos

### Los diez invariantes del frontend, uno por uno
- [ ] 1 · Ninguna llamada de red fuera de `dominio/` y `nucleo/` (lint en los dos mundos)
- [ ] 2 · Ningún tipo reescrito; todos los ejemplos validan contra su esquema OpenAPI
- [ ] 3 · Cero literales de diseño fuera de tokens (lint)
- [ ] 4 · Los cuatro estados, probados
- [ ] 5 · Ningún importe formateado fuera de `Monto` (lint)
- [ ] 6 · Doble envío bloqueado en **toda** operación de dinero
- [ ] 7 · Ninguna regla de negocio que viva solo en el cliente
- [ ] 8 · Ningún componente sobre el límite sin justificación
- [ ] 9 · `/verificar/`, `/publico/`, `/catalogo` y el backoffice: **`noindex` verificado con `curl`**
- [ ] 10 · Ninguna afirmación regulatoria que no sea cierta hoy

### Accesibilidad y rendimiento
- [ ] Lector de pantalla: alta, aporte y retiro completos en Android e iOS
- [ ] Backoffice completo solo con teclado, y con NVDA
- [ ] Contraste AA en claro y oscuro
- [ ] Rendimiento medido **en dispositivo real de gama baja**
- [ ] CWV del sitio en verde

### SEO y GEO
- [ ] Metadatos, canonical, hreflang y JSON-LD válidos en cada página indexable
- [ ] `sitemap.xml` sin rutas `noindex`, con `lastmod` real
- [ ] `robots.txt` conforme a ADR-042
- [ ] `llms.txt`, `llms-full.txt` y los `.md` espejo, generados en el build
- [ ] Primera medición de las diez preguntas en los cuatro motores, registrada

### Publicación
- [ ] App aprobada en ambas tiendas, con *Data Safety* y *Privacy Labels* completos
- [ ] Un parche de Shorebird publicado en `produccion` y tomado por un dispositivo real
- [ ] Todas las pantallas con su ficha de paridad iOS cerrada ([[ADR-036 Android primero]])
- [ ] Backoffice desplegado **sin puerto público**
- [ ] Sitio desplegado con TLS, cabeceras y verificación en Search Console

---

## Lo que este plan deja pendiente

| Pendiente | Por qué | Cuándo |
| --- | --- | --- |
| **Cambiar `Organization` → `FinancialService`** y actualizar `/legal/estado-regulatorio` | Depende de la resolución de ASFI | El día que se otorgue la licencia |
| Contenido educativo continuo (blog) | El GEO se sostiene con contenido fresco; una publicación no es un proyecto | Operación continua |
| Segunda y siguientes mediciones de GEO | La primera es línea de base | Mensual |
| Internacionalización más allá de `es-BO` | No hay otro mercado hoy | Si se expande |
| Modo sin conexión con cola de operaciones | Encolar dinero duplica aportes; requiere diseño propio | Solo con ADR que lo resuelva |
| Flutter Web para el catálogo de `diseno_flutter` | Widgetbook en dispositivo alcanza; publicarlo en web es comodidad, no requisito | Si la revisión visual lo pide |

## Ver también

[[00c Recetario · implementar un caso de uso]] · [[16 Carriles de frontend]] · [[10 Plan maestro del frontend]] · [[14 Fases F9 a F11 · Sitio público, SEO y GEO]] · [[06 Fase 17 · Endurecimiento, E2E y despliegue]] · [[ADR-044 Frontend en Angular y Flutter]]
