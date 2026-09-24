---
tags:
  - arquitectura
  - adr
  - frontend
  - producto
titulo: "ADR-044 — Frontend en Angular y Flutter"
estado: aceptada
fecha: 2026-09-09
supera: [ADR-004]
enmienda: [ADR-041]
---

# ADR-044 — Frontend en Angular y Flutter

> Reemplaza el stack de interfaz que fijó [[ADR-004 Frontend]] (Expo / React Native
> para la app, React + Vite para el backoffice) y el de [[ADR-041 Sitio público · el tercer producto]]
> (Astro con islas de React) por **dos tecnologías, una por familia de producto**:
> **Flutter** para la app del participante y **Angular** para las dos superficies web.
> Lo que ADR-041 decidió sobre *qué* es el sitio público —tercer producto, estático por
> omisión, verificación sin sesión, `noindex` donde hay datos de terceros— **sigue
> vigente**; cambia solo con qué se construye. [[ADR-036 Android primero]] no cambia.

## Contexto

Los tres productos siguen siendo los mismos y con los mismos requisitos:

| Producto | Usuario | Contexto real de uso | Lo que le exige la bóveda |
| --- | --- | --- | --- |
| **App del participante** | participante y organizador | Android de gama baja, datos intermitentes, en la calle | cámara para QR (CU-21), biometría y dispositivo de confianza (CU-04), push, listas largas de movimientos (CU-14), correcciones sin pasar por tienda (CU-05) |
| **Backoffice** | cumplimiento, soporte, contabilidad, riesgos, plataforma | escritorio, jornada completa, pantallas densas | tablas paginadas del servidor con estado en la URL, exportación auditada (CU-58), doble control visible, expedientes con evidencia |
| **Sitio público** | cualquiera, auditores, terceros | navegador, sin cuenta | tarifario (CU-34), contrato (CU-05), verificación de sorteo, cadena y certificado (CU-61, 73, 75) sin sesión; indexable salvo donde hay datos de terceros |

Lo que se construyó con el stack anterior es **el piso, no las pantallas**: los tres
andamiajes de F0, los tokens y el primer corte de átomos de F1
(`planes/informes/carril-F0-*.md`, `carril-F1.md`). Son unas 5.000 líneas sin contar
lo generado, y ninguna pantalla de negocio. Cambiar de stack hoy cuesta un tramo;
cambiarlo con treinta pantallas escritas costaría el proyecto entero de frontend.

Tres cosas empujan el cambio:

1. **Rendimiento en gama baja sin virtualización artesanal.** ADR-004 ya anotaba que
   «React Native rinde peor que Flutter en listas muy largas» y lo compensaba con
   virtualización y paginación desde el primer día. Flutter dibuja con su propio motor
   y sus listas perezosas son el caso por defecto, no una mitigación.
2. **Un frontend web con marco de trabajo completo y opinado.** El backoffice tiene más
   de sesenta pantallas densas con formularios, tablas, roles y guardias de ruta.
   Angular trae inyección de dependencias, enrutador con guardias y carga perezosa,
   formularios tipados, interceptores HTTP, CDK de accesibilidad y virtualización, y
   renderizado híbrido de servidor **en el mismo marco**: una sola manera de hacer cada
   cosa en vez de una lista de librerías elegidas una por una.
3. **Dos productos web con una sola tecnología.** Con Astro y React + Vite había dos
   maneras de escribir un componente web, y `packages/ui` tenía que renderizar en dos
   entornos. Con Angular en el backoffice **y** en el sitio, `packages/ui` es una sola
   biblioteca de Angular que los dos consumen tal cual.

El costo que se asume es el que ADR-004 había evitado: **un tercer lenguaje** (Dart) y
por lo tanto **dos clientes generados** desde el mismo contrato. Se paga con generación,
no con escritura a mano: los dos clientes salen de la misma tarea de Gradle que ya
genera el servidor.

## Decisión

**Flutter para `apps/movil`; Angular para `apps/backoffice` y `apps/web`; una fuente
única de tokens que se genera para los dos mundos; un solo servidor simulado derivado
del contrato que los tres consumen; y dos clientes generados —Angular y Dart— desde los
mismos `openapi/*.yaml`.**

| Capa | Móvil | Web (backoffice y sitio) |
| --- | --- | --- |
| Marco | **Flutter** estable, Dart 3 · Material 3 como base, tema propio desde tokens | **Angular** (≥ 21), *standalone*, *zoneless*, señales |
| Navegación | `go_router` con rutas tipadas; **un enchufe por dominio** que el shell deja listo | Angular Router con `loadChildren` por dominio; **un enchufe por dominio** en `app.routes.ts` |
| Estado de servidor | Riverpod: `AsyncValue` **es** cargando / error / dato; el vacío lo decide la vista | `resource()` / `httpResource()` de Angular; una directiva `EstadoDePantalla` que exige los cuatro estados |
| Formularios | `flutter_form_builder` con validadores derivados del contrato | *Signal Forms* de Angular con validadores derivados del contrato |
| Cliente de API | **generado** `dart-dio` → `clientes/dart/<servicio>` | **generado** `typescript-angular` → `clientes/angular/<servicio>` |
| Diseño | `packages/diseno_flutter` (paquete Dart `aportaya_diseno`) | `packages/ui` (biblioteca Angular `@aportaya/ui`, una entrada secundaria por componente) |
| Tokens | `packages/tokens/tokens.json` → `tokens.dart` **generado** | el mismo `tokens.json` → `tokens.css` **generado** |
| Servidor simulado | **Scalar** sobre los contratos, con ejemplos por CU y por escenario (`packages/simulado`) | el mismo Scalar |
| Nativo | `local_auth`, `flutter_secure_storage`, `mobile_scanner`, `firebase_messaging`, `app_links`, `connectivity_plus`, detrás de **puertos** (ADR-036) | Angular CDK: *overlay*, *a11y*, *scrolling* (virtualización), *table* |
| Correcciones sin tienda | **Shorebird** (*code push* de la capa Dart) | despliegue de contenedor |
| Sitio público | — | `@angular/ssr` con **rutas híbridas**: *prerender* por omisión, servidor solo en verificación, **hidratación incremental** para los verificadores |
| Pruebas | `flutter_test` por nivel · *goldens* en claro y oscuro · guías de accesibilidad de Flutter · **Patrol** para E2E en dispositivo | **Vitest** + Angular Testing Library · `vitest-axe` · **Playwright** con `@axe-core/playwright` · Lighthouse CI |
| Lint | `flutter_lints` + reglas propias con `custom_lint` | `angular-eslint` con las reglas de plantilla de accesibilidad como error + reglas propias |
| Empaquetado | `flutter build appbundle` / `ipa`; Shorebird para parches | Docker: backoffice **estático** tras NGINX; sitio con proceso Node de SSR tras NGINX |

### Las reglas que no cambian

Todo lo que ADR-004 fijó como **regla de uso** sigue en pie, palabra por palabra:
ningún importe se formatea a mano en la vista; ninguna regla de negocio vive solo en el
cliente; toda operación de dinero envía clave de idempotencia; la app asume red
intermitente; los colores salen de los tokens. Y siguen vigentes los diez invariantes de
`planes/10 Plan maestro del frontend.md`. **Cambia la herramienta; no cambia lo que se
exige.**

### Lo que se genera y lo que se escribe una sola vez

| Artefacto | Fuente única | Se genera para |
| --- | --- | --- |
| Tokens de diseño | `packages/tokens/tokens.json` (verificado contra `docs/Views/Sistema-Diseno/estilos.css`) | `tokens.css` (Angular) · `tokens.dart` (Flutter) |
| Formato de dinero | `packages/tokens/vectores/monto.json` — casos entrada → salida | la prueba TS y la prueba Dart consumen el mismo JSON; los dos `Monto` tienen que coincidir byte a byte |
| Cliente de API | `servicios/*/src/main/resources/openapi/*.yaml` | `clientes/angular/` · `clientes/dart/` · el servidor Spring |
| Ejemplos de respuesta | `packages/simulado/ejemplos/<servicio>/CU-NN.json`, derivados del esquema | Scalar en desarrollo · fixtures de Vitest · fixtures de `flutter_test` |
| Átomos del sorteo y la cadena | Java (`plataforma/`) con **vectores dorados** | `packages/dominio-cliente` en TypeScript, para los verificadores del sitio |

> **Dos implementaciones del sistema de diseño, una especificación.** Lo que impide que
> diverjan no es la revisión: es que los tokens sean generados de un solo archivo, que
> `Monto` comparta vectores, y que la revisión visual conjunta compare los *goldens* de
> Flutter con las capturas de Playwright del catálogo Angular, pieza por pieza.

## Motivo

**Porque el usuario de la app está en un Android de gama baja**, y es el motivo que ya
sostenía ADR-004. Flutter compila a código nativo y dibuja sin puente de JavaScript; lo
que en React Native era una lista de mitigaciones acá es el comportamiento por defecto.

**Porque el backoffice es grande y denso, y un marco completo escala mejor que una
composición de librerías.** Con sesenta pantallas escritas por cinco máquinas y un solo
revisor, que exista *una* manera de declarar una ruta con guardia, *una* de inyectar un
servicio, *una* de interceptar un `401` y *una* de escribir un formulario tipado vale
más que la libertad de elegir. Angular es opinado a propósito, y eso es lo que un
equipo chico con muchas manos necesita.

**Porque un solo marco web reduce el sistema de diseño a una biblioteca.** `packages/ui`
tenía dos renderizadores (`/web` y `/nativo`) y el sitio no podía compartir componentes
con el backoffice sin traer React Native al navegador. Ahora el sitio y el backoffice
consumen exactamente la misma biblioteca de Angular; la app tiene la suya en Dart, con
los mismos tokens generados.

**Porque las correcciones sin pasar por tienda siguen siendo posibles.** El argumento más
fuerte de ADR-004 a favor de Expo era EAS Update. Shorebird cumple el mismo papel para
la capa Dart: un texto obligatorio, un tarifario o un mensaje de cumplimiento se corrige
en horas. Lo que toca código nativo sigue pasando por tienda, exactamente igual que antes.

**Porque el costo del cambio es el más bajo que va a tener.** No hay pantallas de negocio
escritas. Lo que existe se porta o se descarta en un tramo, y lo que vale —tokens
verificados contra la bóveda, la prueba de propiedad de `Monto`, el simulado derivado del
contrato, las pruebas de «una pantalla nueva no toca ningún registro compartido»— se
conserva como criterio de aceptación del stack nuevo.

## Alternativas descartadas

| Alternativa | Por qué no |
| --- | --- |
| **Seguir con Expo + React + Astro** | Funciona, y ADR-004 lo justificó bien. Se descarta por lo que cuesta sostenerlo con cinco máquinas: tres maneras de escribir un componente, un sistema de diseño con dos renderizadores, y mitigaciones de rendimiento que en Flutter no hacen falta |
| **Flutter para los tres productos** (Flutter Web) | El sitio público necesita HTML indexable y liviano; Flutter Web dibuja en *canvas* y pesa megabytes. Para el backoffice sería viable, pero deja el sitio afuera y obliga a un tercer stack igual |
| **Angular para móvil** (Ionic / Capacitor) | Una billetera necesita ser app, con biometría, cámara y push confiables: es el argumento de ADR-004 contra la PWA, y sigue en pie. Capacitor los expone, pero el rendimiento en gama baja es el de una vista web |
| **Kotlin Multiplatform + Compose** | Excelente en Android, inmaduro en iOS y sin ecosistema comparable de paquetes probados para KYC, QR y biometría |
| **Astro para el sitio, Angular solo en el backoffice** | Mantiene dos maneras de escribir web y deja `packages/ui` sin poder ser una sola biblioteca. Angular con *prerender* por omisión e hidratación incremental cubre lo que Astro daba, con un costo de JavaScript inicial mayor que se acota con presupuesto en CI |
| **React Native + Angular** | Elimina el tercer lenguaje pero conserva el puente de JavaScript en gama baja y dos sistemas de componentes en TypeScript que no comparten nada más que los tipos |

## Consecuencias

**A favor**

- Un solo marco web y un solo marco móvil; una sola biblioteca de diseño por mundo.
- Rendimiento en gama baja sin mitigaciones artesanales.
- Enrutador, DI, formularios, interceptores, accesibilidad y SSR vienen del marco: menos
  decisiones por carril y menos lugares donde dos carriles resuelven lo mismo distinto.
- El sistema de diseño de Flutter y el de Angular **pueden construirse en paralelo**:
  son dos bases de código sobre un mismo `tokens.json` generado.

**En contra, y hay que asumirlo**

- **Tres lenguajes en el repositorio** (Java, TypeScript, Dart). Lo que cruza la frontera
  se genera; lo que no se puede generar —los átomos del sorteo para el sitio— ya tenía
  vectores dorados y sigue igual.
- **El sitio público carga más JavaScript que con Astro.** Angular con *prerender* e
  hidratación incremental deja las páginas de contenido en HTML completo y difiere la
  hidratación de los verificadores, pero el runtime del marco viaja. Se acota con un
  presupuesto bloqueante en CI (`planes/19` §6) y con `@defer` en todo lo que no sea el
  primer pintado.
- **Dos implementaciones del sistema de diseño.** El riesgo de divergencia se mitiga con
  tokens generados, vectores compartidos y revisión visual conjunta; no desaparece.
- **Un tramo de transición** en el que los tres andamiajes y el sistema de diseño se
  rehacen. Está planificado como tramo `TF` en `planes/17` y documentado en
  `planes/16` §12.
- Shorebird es un servicio externo con contrato; su caída no rompe la app —sigue
  corriendo la versión instalada—, pero sí la capacidad de parchear en horas. Queda
  registrado en `proveedores-externos` con su conmutación: publicar por tienda.

## Cómo se verifica

- [ ] `yarn dev:movil`, `yarn dev:backoffice` y `yarn dev:web` levantan contra
      `yarn dev:mock` (Prism) con una pantalla real y sus cuatro estados cada uno.
- [ ] `./gradlew generateOpenApiClients` produce `clientes/angular/` y `clientes/dart/`
      desde los mismos contratos; el CI regenera y falla si hay diferencia.
- [ ] `packages/tokens` genera `tokens.css` y `tokens.dart`, y la prueba
      `tokens-contra-boveda` sigue comparando contra `estilos.css`.
- [ ] Los dos `Monto` pasan los mismos vectores de `packages/tokens/vectores/monto.json`.
- [ ] Ninguna pantalla ni componente importa `dio`, `HttpClient` ni `fetch` fuera de
      `dominio/` y `nucleo/` (lint en los dos mundos).
- [ ] `grep -r "Platform.is" apps/movil/lib` no devuelve nada fuera de `infraestructura/`.
- [ ] El sitio público: páginas de contenido *prerender*, `/verificar/*` y `/publico/*`
      en servidor con `X-Robots-Tag: noindex`, verificado con `curl`.

## Ver también

[[ADR-004 Frontend]] · [[ADR-036 Android primero]] ·
[[ADR-041 Sitio público · el tercer producto]] · [[ADR-042 Política de rastreadores de IA]] ·
[[ADR-020 Contratos OpenAPI primero]] · [[ADR-033 Puertos y adaptadores]] ·
[[ADR-019 Dinero con BigDecimal]] · `planes/10 Plan maestro del frontend.md` ·
`movil-flutter` · `web-angular` · `web-backoffice` · `disenar-frontend`
