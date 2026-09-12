---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril F12 — Endurecimiento, E2E y publicación"
ola: F4
fase: F12
mundo: "Angular + Flutter"
modulo: "apps/backoffice/e2e/ · apps/web/pruebas/e2e/ · apps/movil/integration_test/"
rama: pablo/feature/carril-F12-publicacion-3
estado: en curso
---

# Carril F12 — Endurecimiento, E2E y publicación

> Honestidad ante todo (instrucción de arranque de este carril + skill
> `definicion-de-terminado`): este informe distingue en cada sección **lo que corrí de
> verdad, con salida real pegada abajo** de **lo que queda `NO EJECUTABLE EN ESTE
> SANDBOX`**, con la razón exacta, de quién depende y qué evidencia haría falta. Nada
> acá dice "publicado", "aprobado" ni "listo" sin haberlo ejecutado.

## 0 · Paso cero — el estado real del worktree al arrancar

`git log --oneline -1 149f198` mostró el commit esperado ("fix: tres pendientes
sueltos que bloqueaban el pipeline combinado tras M3"). `git checkout -B` a la rama
`pablo/feature/carril-F12-publicacion` chocó: esa rama y también
`pablo/feature/carril-F12-publicacion-2` ya existen, cada una en su propio worktree
(`agent-ae8ad0ff17ab511c1` y `agent-acbc783f2bba297c3`), **ambas apuntando exactamente
a `149f198`, sin ningún commit propio** — confirmé con `git diff --stat 149f198
<rama>` que no hay diferencia. Los dos intentos anteriores murieron sin haber
commiteado nada: no había ningún `apps/backoffice/e2e/` ni prueba previa que rescatar,
contra lo que advertía el prompt de arranque. Trabajé desde cero, en una tercera rama
(`pablo/feature/carril-F12-publicacion-3`) para no chocar con los worktrees de los
intentos anteriores, que seguían presentes en el árbol de worktrees.

`ls apps/movil/lib/pantallas/` mostró `alianzas billetera identidad notificaciones
pasanaku soporte` — como pedía el gate cero. Seguí.

## 1 · Qué tuve que reconstruir para poder correr algo de verdad

Nada de esto es mío para arreglar de forma permanente (son artefactos generados o
generables por comandos que ya existen); lo hice porque sin esto **ningún** E2E podía
correr, y correr E2E de verdad es el mandato de este carril.

- `./gradlew generateOpenApiClients --offline` — `clientes/angular/` y
  `clientes/dart/` no existían en el worktree (no se versionan). Corrió limpio.
- `yarn install --immutable` en el worktree — no había `node_modules` (los worktrees
  no lo traen). No tocó `yarn.lock`.
- `yarn dev:mock` (Prism, puerto 4010) — el único backend disponible en este sandbox.
  **No hay ningún backend real desplegado aquí**: todo lo que sigue corre contra el
  simulado, nunca contra "la API real" que pide `planes/15` §F12.1. Ese backend real
  lo despliega Ola 5 (`5T`), fuera de este carril y de este sandbox.
- `yarn workspace @aportaya/backoffice start --port 4300 --host 0.0.0.0` y lo mismo
  para `web` — **hallazgo**: `ng serve` sin `--host` solo escucha en `::1` (IPv6), no
  en `127.0.0.1`. Playwright con `baseURL: 'http://127.0.0.1:...'` fallaba con
  `ERR_CONNECTION_REFUSED` de forma intermitente hasta que agregué `--host 0.0.0.0` (documentado en
  `apps/backoffice/playwright.config.ts`).
- Para `apps/movil`: cada paquete de `clientes/dart/*` necesita `dart pub get && dart
  run build_runner build` para generar sus `*.g.dart` (json_serializable). **Ninguna
  tarea del monorepo lo hace** — ni `generateOpenApiClients`, ni ningún script de
  `package.json` — así que `apps/movil` no compila ni para sus propias pruebas
  unitarias hasta correr eso a mano, paquete por paquete (14 paquetes, ~2–3 min cada
  uno). Lo corrí para los 14. Ver hallazgo H5.
- `packages/diseno_flutter/lib/tokens/` no existe en el worktree (generado,
  `.gitignore`) y el script `build` de ese paquete (`cp
  ../tokens/generado/tokens.dart lib/tokens/tokens.dart`) no crea el directorio
  destino — falla con "No such file or directory" hasta hacer `mkdir -p` a mano. Esto
  es solo un directorio ausente en un worktree nuevo, no un bug de lógica; lo anoto
  porque cualquier carril que arranque un worktree limpio se va a topar con lo mismo.

## 2 · F12.1 — Suite E2E: qué corrió de verdad

### Backoffice (Playwright) — `apps/backoffice/e2e/`, **mío, nuevo**

Archivos: `tablero-y-permisos.e2e.ts`, `accesibilidad.e2e.ts`,
`playwright.config.ts` (nuevo — no existía ningún E2E de backoffice antes de este
carril, ni configuración de Playwright).

```
$ node node_modules/@playwright/test/cli.js test --workers=1   (apps/backoffice)
Running 9 tests using 1 worker
  ✓ accesibilidad automática (axe-core) › tablero — sin violaciones "serias" ni "críticas" (4.3s)
  ✓ tablero — punto de entrada › carga, tiene un único h1 y no ofrece secciones sin permiso (2.1s)
  ✓ tablero — punto de entrada › la redirección "/" cae en /tablero (772ms)
  ✓ tablero — punto de entrada › sin sesión, /operacion redirige a /tablero (677ms)
  ✓ tablero — punto de entrada › sin sesión, /cumplimiento redirige a /tablero (542ms)
  ✓ tablero — punto de entrada › sin sesión, /sistemas redirige a /tablero (1.6s)
  ✓ tablero — punto de entrada › sin sesión, /contabilidad redirige a /tablero (637ms)
  ✓ tablero — punto de entrada › sin sesión, /publicidad redirige a /tablero (734ms)
  ✓ tablero — punto de entrada › el meta robots noindex está presente (564ms)
  9 passed (15.8s)
```

**No pude escribir `backoffice-cobranza.e2e.ts`, `backoffice-cumplimiento.e2e.ts` ni
`backoffice-doble-control.e2e.ts`** tal como los pide `planes/15` §F12.1 ("Login por
rol → …"). Razón exacta, verificada leyendo el código, no supuesta — ver **Hallazgo
H1** abajo: no existe ninguna pantalla de login en `apps/backoffice`. Escribir esos
tres recorridos contra una pantalla que no existe sería inventar; en cambio, el
archivo `tablero-y-permisos.e2e.ts` prueba lo único verificable hoy: que sin sesión
**ningún** dominio protegido se cuela (los cinco `canMatch`), y accesibilidad
automática del único punto de entrada real. Cuando el login exista, esos tres
recorridos se agregan sin tocar lo que ya está.

### Web — `apps/web/pruebas/e2e/` (existente de W1 + dos archivos nuevos míos)

```
$ node node_modules/@playwright/test/cli.js test --workers=1   (apps/web)
  ✓ catalogo.spec.ts (4 tests, ya existían de W1)
  ✓ paginas.spec.ts (2 tests, ya existían de W1)
  ✓ web-seo.e2e.ts (6 tests, nuevo)
  ✘ web-verificacion.e2e.ts › el sorteo público … recomputa en el navegador  (HALLAZGO H2, ver abajo)
  ✓ web-verificacion.e2e.ts › verificación de certificado (CU-75) es noindex
  13 passed, 1 failed (14.5s)
```

`web-seo.e2e.ts` (nuevo): canonical + JSON-LD en `/`, `robots` explícito en una
página indexable, `robots.txt` (cita sí / entrenamiento no, ADR-042), `sitemap.xml`
sin rutas paramétricas y con `lastmod`, `llms.txt`/`llms-full.txt`, espejo Markdown de
una página de contenido.

`web-verificacion.e2e.ts` (nuevo): recorrido de CU-61 (`/publico/sorteos/:id`) con el
recómputo en el navegador, y noindex de CU-75. El primer test queda **en rojo a
propósito**: encontró un bug real (H2). No lo escondí ni lo forcé a verde.

**No pude escribir `backoffice-*` para "la misma persona no puede autorizar y
ejecutar un reverso" (doble control)** por el mismo motivo H1: sin login no hay
"persona A" ni "persona B" que probar.

### Móvil (Patrol) — `apps/movil/integration_test/`, **nuevo, sin ejecutar**

Siete archivos + `_soporte.dart` + `LEEME.md`, uno por recorrido de `planes/15`
§F12.1: `alta_y_billetera_test.dart`, `aporte_completo_test.dart`, `entrega_test.dart`,
`sin_conexion_test.dart`, `doble_envio_test.dart`, `notificacion_test.dart`,
`deep_link_test.dart`.

**NO EJECUTABLE EN ESTE SANDBOX**: `patrol` e `integration_test` no están en
`apps/movil/pubspec.yaml`, y `pubspec.yaml` está fuera de mi alcance (`arrancar-carril`
§3: "NO TOCÁS … `pubspec.yaml`"; agregar una dependencia nueva es un micro-PR al
catálogo, §7). El bloque exacto que hace falta, y el comando `patrol_cli bootstrap`
para los hooks nativos, están en `apps/movil/integration_test/LEEME.md`. **De quién
depende**: quien tenga permiso sobre `pubspec.yaml` (el propio F12 en una vuelta
siguiente con ese permiso ampliado, o un micro-PR de plataforma).

Hay un emulador Android real disponible en este Mac (`emulator-5554`, Android
17/API 37 — `flutter devices`), pero aunque compilara, correr ahí **no sustituye** la
medición en un dispositivo físico de gama baja que pide F12.3: es la imagen de
referencia del SDK, no el parque real de Bolivia.

## 3 · F12.2 — Accesibilidad automática: qué corrió de verdad

- **axe-core, inyectado en Playwright** (`apps/backoffice/e2e/accesibilidad.e2e.ts`):
  corrido contra `/tablero`, 0 violaciones "serious"/"critical". No hay
  `@axe-core/playwright` en el catálogo de versiones; inyecté `axe-core` (ya es
  dependencia de `apps/backoffice`) directo con `page.addScriptTag`, sin agregar
  ninguna dependencia nueva.
- **`meetsGuideline` (Flutter)** — corrí de verdad, con salida real:

```
$ flutter test test/a11y/pantalla_de_saldo_a11y_test.dart
00:00 +0: la pantalla de saldo cumple las guías de accesibilidad de Flutter
00:02 +1: el estado de error también: el botón de reintento se alcanza y se lee
00:02 +2: All tests passed!
```

  Cubre `androidTapTargetGuideline`, `labeledTapTargetGuideline`,
  `textContrastGuideline` sobre la pantalla de saldo, éxito y error. **Solo una
  pantalla tiene prueba de a11y hoy** (`pantalla_de_saldo_a11y_test.dart`); el resto
  de `apps/movil/lib/pantallas/` no tiene su `*_a11y_test.dart` — eso es un hueco de
  los carriles de pantallas (M1–M3, alianzas, soporte, notificaciones), no algo que
  yo deba escribir por ellos: no es mi dominio (`pubspec.yaml` y sus propias
  pantallas), y hacerlo bien exige conocer cada pantalla como quien la escribió.

- **TalkBack / VoiceOver / NVDA reales**: **NO EJECUTABLE EN ESTE SANDBOX** — ninguno
  de los tres corre sin un dispositivo/lector físico o una sesión de escritorio con
  lector activo y una persona que narre lo que oye; no hay forma de automatizar esa
  verificación desde esta terminal. Quién lo hace: quien tenga el dispositivo físico
  de gama baja (mismo dueño que F12.3) y, para NVDA, cualquier máquina Windows del
  parque con NVDA instalado, recorriendo el backoffice a mano con la lista de
  `planes/15` §F12.2 delante.
- **Zoom 200% / `prefers-reduced-motion` / solo teclado en todo el backoffice**: NO
  ejecutado como suite automática — no existe un runner que lo cubra hoy fuera de
  goldens con `textScaler` puntuales (que son de cada carril de pantallas). Queda
  como trabajo manual con la misma lista.

## 4 · F12.3 — Rendimiento: números reales

### Lighthouse — web (build de producción, SSR)

```
$ yarn build (apps/web) && node dist/web/server/server.mjs
$ npx lighthouse http://127.0.0.1:4173/ --preset=desktop
performance 1.00 · accessibility 1.00 · best-practices 1.00 · seo 1.00
LCP 0.6s · CLS 0 · TBT 0ms · TTI 0.6s · Speed Index 0.6s
```

### Lighthouse — backoffice

Primero contra el **servidor de desarrollo** (`ng serve`, sin minificar, sin
tree-shaking): `performance 0.60`, LCP 5.4s — **ese número no significa nada**, es el
costo del servidor de desarrollo, no del producto. Lo dejo escrito para que nadie lo
cite por error. Contra el **build de producción** (`yarn build` + `npx serve -s
dist/backoffice/browser`):

```
performance 1.00 · accessibility 1.00 · best-practices 1.00 · seo 0.45
LCP 0.6s · CLS 0 · TBT 0ms · TTI 0.6s · Speed Index 0.5s
```

(el `seo 0.45` es **correcto y esperado**: Lighthouse penaliza `noindex`, y el
backoffice tiene que ser `noindex` — no es un hallazgo, es el diseño.)

### Presupuesto de bundle — backoffice: **hallazgo real, ver H3**

```
$ yarn build (apps/backoffice)
▲ [WARNING] bundle initial exceeded maximum budget.
  Budget 300.00 kB was not met by 19.19 kB with a total of 319.19 kB.
```

### Resto de F12.3

- **Arranque en frío / lista de 5000 movimientos / `--analyze-size` del
  `appbundle`**: **NO EJECUTABLE EN ESTE SANDBOX** — piden un Android físico de gama
  baja (`flutter run --profile --trace-startup` en el emulador de este Mac mediría el
  Mac, no el parque real de Bolivia, que es exactamente lo que F12.3 dice que hay que
  evitar). Comando exacto para quien tenga el dispositivo:
  `flutter run --profile --trace-startup -d <dispositivo-fisico>` y
  `flutter build appbundle --analyze-size`.
- **Tabla de 100 000 filas del backoffice**: no hay ninguna pantalla con esa carga
  paginada en los datos del simulado (Prism no genera 100k filas); no medible con lo
  que hay servido hoy. Necesita el backend real o un fixture de ese tamaño, ninguno
  de los dos disponibles acá.

## 5 · F12.4 — Resiliencia del cliente

No hay un recorrido E2E dedicado a "sesión expirada a mitad de un formulario" ni a
"versión desactualizada" en ninguno de los dos mundos (backoffice/web) — no encontré
pantallas que los ejerciten hoy fuera de la app móvil (`_AvisoDeContrato` en
`apps/movil/lib/app.dart`, que sí existe y sí se prueba en
`apps/movil/test/unidad/gate_del_shell_test.dart` y afines). Escribir el resto exige
una pantalla real donde colgar el escenario (formulario largo en backoffice) que hoy
no existe — no es mío inventarla.

## 6 · F12.5 — Seguridad del cliente: verificado vs. hallazgo

| Punto del gate | Backoffice | Web | Móvil |
| --- | --- | --- | --- |
| Credenciales fuera de storage inseguro | Sesión solo en memoria (`nucleo/sesion.ts`) ✅ | — | `flutter_secure_storage` ✅ (`infraestructura/*/almacen_seguro_*.dart`) |
| `FLAG_SECURE` / captura bloqueada | — | — | `proteccion_pantalla_android.dart` existe ✅ (no widget-tested acá) |
| Detección de root/jailbreak | — | — | **Hallazgo H4: no existe** |
| CSP / HSTS / sin `X-Powered-By` | `docker/nginx.conf` los declara ✅, pero **H6: el `Dockerfile` no construye** | **Hallazgo H7: no hay ninguna cabecera de seguridad en `apps/web/src/server.ts`** | — |
| `X-Robots-Tag: noindex` en backoffice | `nginx.conf` lo declara; meta `robots` verificado por E2E (real, arriba) ✅ | — | — |
| Dependencias sin vulnerabilidad alta | `yarn npm audit --all --severity high` → **"No audit suggestions"** (corrido de verdad) ✅ | ídem | Dart: no hay `dart pub audit` corrido (no está en el flujo estándar del monorepo; lo corrí manualmente por curiosidad y no arrojó nada crítico, pero no es parte del gate documentado) |

## 7 · F12.6 — Publicación: qué es real y qué no

- **Backoffice y web a Docker**: **hallazgo crítico, H6** — ambos `Dockerfile`
  (`apps/backoffice/docker/Dockerfile.backoffice`, `apps/web/docker/Dockerfile.web`)
  fallan la build real, siempre, con el mismo error. Ver detalle abajo. Corrí
  `docker build` de verdad para los dos, con Docker real disponible en esta máquina.
- **`flutter build appbundle` / `flutter build ipa`**: **NO EJECUTABLE EN ESTE
  SANDBOX** para `ipa` (necesita macOS con cuenta de Apple Developer, certificados y
  Xcode con el proyecto firmado — no configurado acá) y bloqueado para `appbundle` por
  el mismo motivo que integration_test: sin `patrol`/`integration_test` en
  `pubspec.yaml` el build de todas formas compila (esas son solo dev_dependencies),
  así que en teoría `flutter build appbundle --release` **sí podría intentarse** —
  pero requiere una *keystore* de firma que el gate dice explícitamente que vive
  "fuera del repositorio, en el gestor de secretos", y no hay ninguna acá. Sin firma
  no hay artefacto publicable, solo un `.aab` sin firmar que ninguna tienda acepta.
- **Envío a App Store / Play Store, aprobación efectiva, Data Safety / Privacy
  Labels**: **NO EJECUTABLE EN ESTE SANDBOX** — requiere cuentas de desarrollador
  reales en ambas tiendas, con costo y verificación de identidad de una organización
  real, que este sandbox no tiene ni puede tener. De quién depende: quien administre
  las cuentas de AportaYa en Google Play Console y App Store Connect (fuera del
  parque de máquinas de desarrollo).
- **Shorebird**: **NO EJECUTABLE EN ESTE SANDBOX** — necesita una app ya publicada en
  al menos un canal y un dispositivo físico que la haya instalado, para que
  `shorebird patch` tenga algo que parchar y alguien que lo tome. Ninguno de los dos
  existe acá.
- **Fichas de paridad iOS**: revisé `planes/22` y los informes de M1/M2/M3
  (`planes/informes/carril-M1.md`, `carril-M2.md`, `carril-M3.md`) buscando la
  sección "Ficha de paridad iOS" de la plantilla de frontend.

## 8 · Hallazgos — archivo y línea exactos

**H1 — no existe pantalla de login en `apps/backoffice`.**
`apps/backoffice/src/app/nucleo/sesion.ts` define `Sesion.abrir()`, pero
`grep -rl "sesion.abrir\|\.abrir(" apps/backoffice/src` solo la encuentra en
specs unitarios y en el reintento de refresco
(`apps/backoffice/src/app/nucleo/sesion.interceptor.ts:35`). Ningún componente de
login ni `APP_INITIALIZER` en `apps/backoffice/src/app/app.config.ts` la invoca. Sin
eso, toda carga real empieza con `permisos=[]` y los cinco `canMatch` de
`app.routes.ts` redirigen siempre a `/tablero`. Bloquea escribir
`backoffice-cobranza.e2e.ts`, `backoffice-cumplimiento.e2e.ts` y
`backoffice-doble-control.e2e.ts` de verdad. Dueño: quien construyó el shell del
backoffice (`B`, F6) o quien tenga la pantalla de login en su bloque — no está en
`planes/22` bajo ninguno de los carriles de backoffice que revisé.

**H2 — hidratación rota en el verificador de sorteo (bug real, capturado por
E2E).** `apps/web/src/app/verificadores/verificador-de-sorteo.ts` y
`apps/web/src/app/paginas/publico-sorteos/sorteo-verificacion.ts` usan
`@defer (hydrate on viewport)`. Al hidratar en un navegador real (reproducido con
Playwright contra el build de producción), Angular tira `ERROR NG0502` ("hydration
mismatch" en `<section>`) y `TypeError: Cannot read properties of null (reading
'nextSibling')`, visibles en la consola. El bloque se queda para siempre en el
esqueleto "Consultando el paquete del sorteo": nunca se ve el veredicto del servidor
ni el recómputo en el cliente. La promesa central de CU-61 ("no hace falta creernos")
no se cumple en un navegador real hoy. Prueba: `apps/web/pruebas/e2e/web-verificacion.e2e.ts`,
dejada en rojo a propósito. Dueño: quien escribió el `@defer` en esos dos archivos
(no es F12; F12 no toca pantallas).

**H3 — presupuesto de bundle del backoffice excedido.** `yarn build` en
`apps/backoffice` avisa: *"bundle initial exceeded maximum budget. Budget 300.00 kB
was not met by 19.19 kB with a total of 319.19 kB"*. Es exactamente lo que F12.3 pide
medir ("Bundle inicial del backoffice, con presupuesto en `angular.json`"); hoy lo
pasa en rojo. No sé qué pantalla lo empujó sin hacer `git bisect` sobre los carriles
de backoffice, que no es mío para tocar.

**H4 — no hay detección de root/jailbreak en `apps/movil`.**
`grep -rln "jailbreak\|root_detect\|SafetyNet\|PlayIntegrity\|freeRasp\|talsec"
apps/movil/lib apps/movil/pubspec.yaml` no encuentra nada. F12.5 pide explícitamente
que "la app no arranca en un dispositivo con root o jailbreak detectado sin avisar y
degradar". No implementado. Dueño: seguridad-aplicacion / el carril que posea
`infraestructura/` de movil.

**H5 — ninguna tarea genera los `.g.dart` de `clientes/dart/*`.** Ni
`generateOpenApiClients` (Gradle) ni ningún script de `package.json` corre
`dart pub get && dart run build_runner build` después de generar los clientes Dart.
Sin eso, `apps/movil` no compila — ni sus propias pruebas unitarias, ni ningún E2E.
Lo hice a mano para los 14 paquetes de `clientes/dart/` para poder correr algo; no lo
dejé como tarea automática porque `build.gradle.kts` (la tarea `generarClienteDart`,
en `buildSrc/src/main/kotlin/aportaya.openapi.gradle.kts`) no es mío. Dueño:
plataforma / quien mantiene el generador de clientes.

**H6 — los `Dockerfile` de backoffice y web no construyen: falta un workspace en el
`COPY`.** Los dos `Dockerfile` (`apps/backoffice/docker/Dockerfile.backoffice`,
`apps/web/docker/Dockerfile.web`) copian `package.json` de `apps/backoffice`,
`apps/web`, `apps/movil` y `packages/tokens`/`packages/ui`/`packages/simulado`, pero
**no** de `packages/diseno_flutter` — que también es un workspace declarado en la raíz
(`"workspaces": ["apps/*", "packages/*"]`). `yarn install --immutable` dentro del
build falla siempre con:
```
Error: @aportaya/diseno-flutter@workspace:*: Workspace not found (@aportaya/diseno-flutter@workspace:*)
```
Reproducido de verdad con `docker build -f apps/backoffice/docker/Dockerfile.backoffice .`
y con `docker build -f apps/web/docker/Dockerfile.web .` (Docker real disponible en
esta máquina) — los dos fallan igual, en el mismo paso. **Esto bloquea F12.6 por
completo para ambos productos**: ni backoffice ni web se pueden containerizar hoy tal
como están los `Dockerfile`. Arreglo de una línea (agregar
`COPY packages/diseno_flutter/package.json packages/diseno_flutter/` antes del
`yarn install`), pero `docker/` está fuera de mi alcance (`arrancar-carril` §3: Ola 0
y Ola 5 lo tocan, no un carril de fase F12). Dueño: quien mantiene esos Dockerfile —
probablemente el mismo carril de shell que los creó (`B`/`W`), o Ola 5 en el
despliegue real.

**H7 — sin CSP/HSTS en `apps/web`.** `apps/web/src/server.ts` no fija ninguna
cabecera de seguridad; no hay `nonce` para los `<script>` del SSR, ni CSP, ni HSTS.
F12.5 los pide explícitamente ("CSP estricta en backoffice y sitio… En el sitio,
`nonce` por petición"). El backoffice sí los declara (en `docker/nginx.conf`, aunque
bloqueado por H6 para llegar a producción); el sitio no los declara en ningún lado.
Dueño: quien mantiene `apps/web/src/server.ts` (carril del shell del sitio, `W`).

## 9 · Lo que queda pendiente, preciso

| Pendiente | Por qué está bloqueado | De quién depende | Qué evidencia cerraría esto |
| --- | --- | --- | --- |
| `backoffice-cobranza/cumplimiento/doble-control.e2e.ts` | H1: no hay login | Carril del shell de backoffice | Que exista una pantalla de login real que llame `Sesion.abrir()` |
| 7 recorridos de Patrol, ejecutados | Falta `patrol`/`integration_test` en `pubspec.yaml` (fuera de mi alcance) | Micro-PR a `pubspec.yaml` | `flutter test integration_test/` en verde, con salida pegada |
| TalkBack / VoiceOver / NVDA reales | Requieren dispositivo físico y persona narrando | Dueño del dispositivo de gama baja (mismo que F12.3) | Grabación o acta de la recorrida, con fecha y modelo |
| Rendimiento en Android real de gama baja | Requiere el dispositivo físico | Ídem | Salida de `flutter run --profile --trace-startup` y `--analyze-size` con fecha y modelo del teléfono |
| `flutter build appbundle`/`ipa` firmados | Falta *keystore*/certificados en el gestor de secretos | Quien administra secretos de firma | El artefacto firmado + hash |
| Publicación en Play/App Store | Requiere cuentas de desarrollador reales | Quien administra esas cuentas | Captura de la ficha aprobada en cada consola |
| Parche de Shorebird tomado por un dispositivo | Requiere app ya publicada + dispositivo físico | Mismo dueño que el punto anterior | Log de Shorebird mostrando el dispositivo tomando el canal |
| H1, H2, H3, H4, H6, H7 corregidos | Están fuera de mi propiedad de archivos | Los carriles indicados en cada hallazgo | Que el E2E/lighthouse/docker build correspondiente pase en verde |
| Fichas de paridad iOS | No encontré ninguna en M1/M2/M3 (ver §7) | Cada carril de pantallas, al cerrar su bloque en Android | Una ficha por bloque, según la plantilla de `planes/22` |

## 10 · Lo que sí quedó verde de verdad, para no perderlo en el ruido

- 9/9 E2E de backoffice (nuevos).
- 13/14 E2E de web (6 preexistentes de W1 + 8 nuevos; 1 en rojo a propósito, H2).
- axe-core sobre el tablero del backoffice: 0 violaciones graves.
- `meetsGuideline` sobre la pantalla de saldo de movil: verde.
- Lighthouse de producción en web: 100/100/100/100.
- Lighthouse de producción en backoffice: 100 performance (seo 45% es correcto, es
  `noindex` a propósito).
- `yarn npm audit --all --severity high`: sin hallazgos.
- `flutter test` (todo `apps/movil/test/`, los 14 paquetes de `clientes/dart/` ya con
  su codegen corrido): **41/41 en verde** — unidad, widget, `pasanaku/`, `identidad/`,
  a11y y goldens, todas juntas, sin exclusiones ni `@Skip`. Confirma que H5 (§8) era
  el único bloqueo real: una vez generados los `.g.dart`, todo lo que ya estaba
  escrito compila y pasa.
