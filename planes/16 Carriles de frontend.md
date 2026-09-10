---
tags:
  - moc
  - plan
  - frontend
  - carriles
titulo: "Carriles de frontend — varias máquinas en paralelo (Flutter y Angular)"
fecha: 2026-09-09
---

# Carriles de frontend

> Espejo de [[07 Carriles de trabajo concurrente]], para los tres productos de
> interfaz. Misma regla de oro: **un carril = un producto o un dominio de pantallas =
> un directorio = una rama = una máquina = un chat**, con propiedad exclusiva de
> archivos para que el conflicto de merge sea imposible por diseño.

> [!important] Stack: Flutter y Angular ([[ADR-044 Frontend en Angular y Flutter]])
> Este documento se reescribió el 2026-09-09 con el cambio de stack. Cuatro cosas
> cambian respecto de la versión anterior: **(1)** ya no hay enrutamiento por archivos;
> lo reemplaza **un enchufe por dominio** que el shell deja escrito y congela (§5);
> **(2)** el sistema de diseño se parte en **dos carriles paralelos**, `F1-W` y `F1-M`,
> porque son dos bases de código sobre un mismo `tokens.json` generado; **(3)** el
> servidor simulado es **Prism**, uno para Dart y TypeScript; **(4)** hay un **tramo de
> transición `TF`** (§12) que rehace los andamiajes. Lo que no cambia: quién posee qué
> directorio, la sincronía con el backend y la regla cero.

> [!important] Tres correcciones desde el plan secuencial, que siguen vigentes
> [[17 Plan de acción secuencial · coordinación de cinco máquinas]] manda en **quién,
> en qué máquina y en qué tramo**. **La Ola F0 se parte en un paso troncal y tres
> andamiajes concurrentes** (delta 2), **la rama es `<usuario>/feature/carril-<id>`**
> (delta 3), y **la unidad de planificación es el tramo, no la ola** (delta 4).

---

## 1 · Por qué esto se paraleliza

| Razón | Consecuencia |
| --- | --- |
| **Un enchufe por dominio** en los tres productos (`rutas.dart` en Flutter, `<dominio>.routes.ts` en Angular), escrito por el shell **antes** de que exista ningún carril de pantallas | Una pantalla nueva es **un archivo nuevo más una línea en el archivo de rutas de su propio dominio**. El shell no se toca. No existe un `routes.ts` central que todos editen |
| **El contrato OpenAPI existe antes que la implementación** | El frontend nunca espera al backend: programa contra el contrato y Prism |
| **`packages/tokens` se congela al cerrar F0.T**, y `packages/ui` y `packages/diseno_flutter` al cerrar F1 | Los carriles **componen**, no diseñan. Nadie edita tokens ni átomos |
| **Dos mundos que no comparten código** | Un carril de Flutter y uno de Angular no pueden pisarse ni queriendo: ni un archivo en común |

> **La consecuencia práctica:** el frontend no espera a que el backend termine un caso
> de uso. Espera a que **escriba su contrato**, que es lo primero que hace.

---

## 2 · Mapa de olas

### Ola F0 · Troncal + tres andamiajes + dos sistemas de diseño

| Carril | Fase | Puesto | Posee | Mundo |
| --- | :-: | :-: | --- | --- |
| **F0.T** | F0 | P1 | `packages/tokens/**` · `packages/simulado/**` · las tareas `generarCliente*` de Gradle · `package.json` y `turbo.json` de raíz · el job `frontend` del CI | ambos |
| **F0-M** | F0 | P3 | andamiaje Flutter (`apps/movil/**`) | Flutter |
| **F0-B** | F0 | P4 | andamiaje Angular del backoffice (`apps/backoffice/**`) | Angular |
| **F0-W** | F0 | P5 | andamiaje Angular SSR del sitio (`apps/web/**`) | Angular |
| **F1-W** | F1 | P4 | `packages/ui/**` — la biblioteca Angular entera, más `/catalogo` en `apps/web` | Angular |
| **F1-M** | F1 | P3 | `packages/diseno_flutter/**` — el paquete Dart entero, más su Widgetbook | Flutter |

`F0.T` va **primero** y bloquea a los tres andamiajes. Los tres andamiajes bloquean a
`F1-W` y `F1-M`, que corren **a la vez**. **Las dos bibliotecas quedan congeladas al
cerrar F1**: a partir de ahí, un átomo nuevo se pide por micro-PR.

### Ola F1 · 3 carriles — los tres shells

| Carril | Fase | Producto | Directorio propio · **y se congela al cerrar** |
| --- | :-: | --- | --- |
| **M** | F2 | app móvil | `apps/movil/lib/{navegacion,proveedores,infraestructura}/` · `lib/dominio/{cliente,errores,validacion}.dart` · `lib/dominio/puertos/` · `lib/pantallas/notificaciones/` |
| **B** | F6 | backoffice (P3, en serie tras M) | `apps/backoffice/src/app/{nucleo,layout}/` · `app.routes.ts` · `app.config.ts` · `rutas/tablero/` |
| **W** | F9 | sitio público | `apps/web/src/app/{nucleo,layout,paginas,verificadores}/` · `app.routes*.ts` · `contenido/**` · `scripts/contenido.mjs` |

### Ola F2 · 5 carriles — máxima concurrencia

| Carril | Fase | Directorio propio | CU |
| --- | :-: | --- | --- |
| **M1** | F3 | `apps/movil/lib/pantallas/identidad/` | 01–09, 40, 46 |
| **M2** | F4 | `apps/movil/lib/pantallas/{billetera,alianzas}/` | 10–19, 30–33, 57 + D-17, D-19, D-21, D-22 |
| **B1** | F7 | `apps/backoffice/src/app/rutas/operacion/` | 26 CU de operación |
| **W1** | F10 | `apps/web/src/app/seo/` (incluido `ServicioMeta`) | SEO |
| **W2** | F11 | `apps/web/src/app/geo/` + `public/robots.txt` + la parte GEO de `scripts/contenido.mjs` | GEO |

> **W1 y W2 conviven sin pisarse** porque tocan cosas distintas: SEO escribe
> `ServicioMeta` y el JSON-LD en `seo/`; GEO escribe `robots.txt`, `llms.txt`, el
> generador de espejos `.md` y la guía de redacción. El único punto de contacto es el
> `<head>`, y ahí **manda `ServicioMeta` de W1**: W2 le pasa lo suyo por el `data` de la
> ruta (`alternateMarkdown`), no edita el servicio.

### Ola F3 · 3 carriles

| Carril | Fase | Directorio propio | CU |
| --- | :-: | --- | --- |
| **M3** | F5 | `apps/movil/lib/pantallas/{pasanaku,soporte}/` | 20–29, 52, 53, 59–76 + D-15…D-18, D-20 |
| **B2** | F8 | `apps/backoffice/src/app/rutas/cumplimiento/` | 38 CU de cumplimiento y gobierno |
| **B5** | F8.D | `apps/backoffice/src/app/rutas/sistemas/` + `layout/sistemas/` | Backoffice de sistemas (delta D-2) |

> **B5 es un producto aparte que comparte el shell.** Plataforma y seguridad tienen su
> propio menú, su propio layout de sección y sus propios roles (`PLATAFORMA`,
> `SEGURIDAD`). Colisiona con cero archivos de los otros carriles porque vive en su
> propio directorio de rutas y en su propio layout, que F6 deja creado vacío.

### Ola F4 · 3 carriles — publicación y los carriles nuevos

| Carril | Fase | Alcance |
| --- | :-: | --- |
| **T** | F12 | E2E, accesibilidad, rendimiento, seguridad, publicación (`apps/*/e2e/`, `apps/movil/integration_test/`) |
| **B3** | F13 | `apps/backoffice/src/app/rutas/contabilidad/` — ERP (CU-100–106) |
| **B4** | F14 | `apps/backoffice/src/app/rutas/publicidad/` — publicidad (CU-110–114) |

### Resumen

```
Ola F0 ──► 1 troncal (F0.T) → 3 andamiajes en paralelo → 2 sistemas de diseño en paralelo
Ola F1 ──► 3 máquinas
Ola F2 ──► 5 máquinas   ← pico
Ola F3 ──► 3 máquinas
Ola F4 ──► 3 máquinas
```

**20 carriles en total** — eran 18 contados a mano: F0 suma el paso troncal `F0.T` y
F1 se parte en dos ([[18 Fichas de carril · las 38 unidades de trabajo]]).

### 2.6 · Las pantallas de cada carril móvil

El recorrido del participante ([[Flujo funcional · recorrido del usuario]]) está desglosado
pantalla por pantalla en [[Flujo de pantallas · app del participante]] — cada pantalla trae su
ruta, los organismos que compone, los cuatro estados y su endpoint. El reparto por carril es
directo: una pantalla = un archivo en `pantallas/<dominio>/` + una entrada en el `rutas.dart`
**de ese dominio**.

| Carril | Fase | Pantallas que posee (`apps/movil/lib/pantallas/…`) |
| :-: | :-: | --- |
| **M** | F2 | `navegacion/` (shell con tab bar, deep links, guardias) · `notificaciones/bandeja` |
| **M1** | F3 | `identidad/`: bienvenida · registro · verificación básica · contrato · sesión · mfa · dispositivo · **verificación profunda** · perfil · contraseña · baja |
| **M2** | F4 | `billetera/`: inicio (saldo) · recargar · retirar · cuenta-bancaria · extracto · pagar-aporte (saga) · confirmación · **aportes/:id/no-puedo-pagar** · **entregas/mi-turno/cobro** · `alianzas/`: **mis-vales** · **vales/:id/uso** |
| **M3** | F5 | `pasanaku/`: unirse · postular · **solicitudes/:id** · **grupo/:codigo/solicitudes** · grupo/:codigo · **grupo/:codigo/mi-turno** · **grupo/:codigo/ofertas** · **grupo/:codigo/ofertas/nueva** · **organizador** · **nivel** · **crear** 🔒 · sorteo · reputación · reseñar · `soporte/`: **ayuda** · **reclamos/nuevo** |

> **Las once pantallas en negrita las suman los deltas D-15 a D-22** de
> [[20 Maqueta de referencia · deltas del frontend]]. Tres cosas que conviene ver antes de
> repartir:
>
> 1. **`alianzas/` es carpeta de M2**, no de M3. Un vale es un descuento sobre plata que
>    la persona ya tiene; no es un asunto del grupo.
> 2. **`soporte/` es carpeta de M3.** El reclamo nace casi siempre de algo del pasanaku, y
>    el plazo de CU-52 se prueba junto al resto de los plazos del grupo.
> 3. **`grupo/:codigo/solicitudes` es la primera pantalla de M3 que solo ve un rol.** El
>    guard no es del shell —no depende del nivel de verificación— sino de `soyOrg` en ese
>    grupo, así que lo compone M3 en su `rutas.dart` con `AP-CU68-07` y su explicación.

> **El gate básica/profunda es del shell, no de cada pantalla.** `proveedorSesion` de **M**
> expone el nivel de verificación; las acciones de nivel profundo (crear grupo, habilitarse
> como organizador) las pinta M3 **deshabilitadas con motivo**. Así el gate se prueba una vez,
> en el shell, y no se repite en cada pantalla.

### 2.7 · Las pantallas de cada carril de backoffice

El recorrido del administrador ([[Flujo funcional · usuario administrador]]) está desglosado en
[[Flujo de pantallas · backoffice administrador]]. El backoffice es Angular con **carga
perezosa por dominio** (una ruta = un componente en `rutas/<dominio>/`, registrado en el
`<dominio>.routes.ts` de ese dominio) y el organismo `TablaDeDatos` como pieza de trabajo.

| Carril | Fase | Rutas que posee (`apps/backoffice/src/app/rutas/…`) |
| :-: | :-: | --- |
| **B** | F6 | shell: `nucleo/`, `layout/`, `app.routes.ts`, `rutas/tablero/`, **`operacion/estado`** (estado de plataforma) |
| **B1** | F7 | `operacion/`: conciliación · cierre-diario · desembolsos (autorizar/ejecutar) · reclamos · **solicitudes-escaladas** (D-15) · **políticas-resolución** · roles · tarifario · reportes · **fondeo (QR)** · **mensajería** |
| **B2** | F8 | `cumplimiento/`: **verificaciones como expediente** · **organizadores** (D-16) · alertas · casos/ROS · uif · gobierno |
| **B5** | F8.D | `sistemas/` + `layout/sistemas/`: estado de servicios · slo · despliegues · base-de-datos · respaldos · proveedores · outbox · webhooks · **accesos** · **incidentes** |
| **B3** | F13 | `contabilidad/`: período · presupuesto · compras/CxP · cobros · activos · estados |
| **B4** | F14 | `publicidad/`: partners · anunciantes · campañas (aprobar) · moderación · liquidación |

> **La segregación de funciones se pinta en pantalla:** desembolsos, compras/CxP y campañas
> muestran solo el lado (autorizar **o** ejecutar / gestionar **o** aprobar) que el rol del
> operador permite — nunca los dos botones. El guard `canMatch` de cada ruta monta cada una
> **según el permiso del token**, así que la navegación misma refleja el `roles-y-accesos`
> del backend.

---

## 3 · Sincronía con el backend

Las olas de frontend van **una detrás** de las de backend. Cada carril de frontend
consume los **contratos** de la ola anterior de backend, no su implementación.

| Carril de frontend | Contratos que necesita | Los escribe |
| --- | --- | --- |
| M1 · F3 identidad | CU-01…09, 40, 46 | backend Olas 1A y 1C |
| M2 · F4 billetera | CU-10…19, 30…33, 57 | backend Olas 2A y 2B |
| B1 · F7 operación | los 26 de operación | backend Olas 2 y 3 |
| M3 · F5 pasanaku | CU-20…29, 59…76 | backend Olas 2C, 3 y 4 |
| B2 · F8 cumplimiento | los 38 | backend Olas 2D, 2E y 3C |
| B5 · F8.D sistemas | ninguno de negocio: consume `/erp/*` e `/indicadores/*` | backend Ola 3 (plataforma) |
| W · F9 sitio | CU-30, 34, 61, 72, 73, 75 | backend Olas 2B y 3B |

**Si un contrato no existe todavía, el carril de frontend no lo inventa.** Lo pide al
carril de backend y trabaja en otra pantalla mientras tanto (regla cero). Y cuando el
contrato llega, **regenera los dos clientes** (`./gradlew generateOpenApiClients`): el
Dart y el Angular salen del mismo comando.

---

## 4 · Propiedad de archivos

### Lo que un carril posee en exclusiva

| Ruta | Nota |
| --- | --- |
| Su directorio de pantallas o rutas | Todo lo de adentro: sus páginas, **su `rutas.dart` / `<dominio>.routes.ts`**, su `dominio/` (los proveedores o recursos por CU sobre el cliente generado), su `textos.*`, sus pruebas y sus goldens |
| Sus ejemplos del contrato | `packages/simulado/ejemplos/<servicio>/CU-NN.json` — organizados **por servicio**. El ejemplo de un CU lo crea el **primer carril** que lo necesita; el segundo lo consume (barrido 17: un CU no tiene dos archivos de ejemplos) |
| `planes/informes/carril-<id>.md` | Su informe, desde `planes/informes/_plantilla-frontend.md` (tabla de pantallas con columna Maqueta, comparación por escenario, ficha de paridad iOS) |

> **Los clientes de API no aparecen acá porque no tienen dueño.** `clientes/angular` y
> `clientes/dart` son **generados** desde los `openapi/*.yaml` del backend: no se editan
> a mano, los regenera quien corre `generateOpenApiClients` y por eso el solape de CU
> **no es un conflicto**.

### Lo que ningún carril toca (solo lectura)

| Ruta | Quién la cambia |
| --- | --- |
| **`packages/tokens/**`** | F0.T. **Congelado al cerrar F0.** Un token nuevo = micro-PR con la prueba contra la bóveda |
| **`packages/ui/**`** · **`packages/diseno_flutter/**`** | F1-W · F1-M. **Congelados al cerrar F1.** Un átomo nuevo = micro-PR |
| El `openapi/` de cada servicio | Los carriles de **backend** |
| `apps/movil/lib/{navegacion,proveedores,infraestructura}/` · `lib/dominio/{cliente,errores,validacion}.dart` | Carril M (F2), luego congelado |
| `apps/backoffice/src/app/{nucleo,layout}/` · `app.routes.ts` · `app.config.ts` | Carril B (F6), luego congelado. **Excepción:** `layout/sistemas/` es de B5 |
| `apps/web/src/app/{nucleo,layout}/` · `app.routes*.ts` · `scripts/contenido.mjs` | Carril W (F9), luego congelado. **Excepción:** la parte GEO del script es de W2 |
| `apps/web/src/app/seo/` | Carril W1 (F10) |
| `package.json`, `yarn.lock`, `pubspec.yaml`, `pubspec.lock`, `angular.json`, `turbo.json`, `docker/`, `.github/` | Micro-PR |
| **`.claude/skills/**`** | **Micro-PR.** Las 66 skills son de todos ([[19 Contrato de carril · conflicto cero, skills y calidad verificada]] §1) |

---

## 5 · Los siete puntos de conflicto, y cómo se eliminan

Se implementan en la **Ola F0** y en los shells.

| # | Conflicto | Solución |
| :-: | --- | --- |
| 1 | Un registro central de rutas | **Un enchufe por dominio.** El shell escribe `rutas.dart` / `app.routes.ts` **una vez**, con una entrada por directorio de dominio que apunta a un archivo de rutas **vacío** dentro de ese directorio. El carril llena el suyo. Prueba del gate: agregar una pantalla vacía no toca nada fuera del directorio |
| 2 | Barril `packages/ui/index.ts` con 80 exports | Sin barril. Angular: **una entrada secundaria por componente** (`@aportaya/ui/boton`). Flutter: **un archivo por componente**, importado por ruta (`package:aportaya_diseno/atomos/boton.dart`), sin `library` que exporte todo |
| 3 | Un archivo global de traducciones o textos | **Un `textos.dart` / `textos.ts` por dominio de pantallas**, en el directorio del carril |
| 4 | Catálogo con un registro central | Angular: `/catalogo` **descubre** las entradas secundarias por *glob*. Flutter: Widgetbook con **un archivo por componente** anotado (`@UseCase`), descubierto por generación |
| 5 | `yarn.lock` / `pubspec.lock`: dos carriles agregan dependencias | Todas se instalan en la Ola F0. **Ninguna en rama de carril**. Presupuesto de [[19 Contrato de carril · conflicto cero, skills y calidad verificada]] §6: cero dependencias nuevas |
| 6 | W1 (SEO) y W2 (GEO) editando el `<head>` | `ServicioMeta` es de **W1**; W2 le pasa lo suyo por el `data` de la ruta |
| 7 | Dos carriles agregando el mismo puerto nativo (`Camara`, `Biometria`) | Los puertos son del shell (F2) y se declaran **todos** en `dominio/puertos/` antes de la Ola F2, aunque su primer usuario llegue después. Un puerto nuevo = micro-PR al shell |

---

## 6 · Micro-PR al troncal

Igual que en el backend ([[07 Carriles de trabajo concurrente]] §6). El caso más común
acá: **un carril necesita un átomo que su biblioteca de diseño no tiene.**

```
1  ¿Existe algo parecido? Reusar antes que crear
2  ¿Lo van a usar dos productos o dos pantallas? → sube a packages/ui o packages/diseno_flutter por micro-PR
3  ¿Es de un solo dominio? → vive en el directorio del carril, sin micro-PR
4  rama troncal/<carril>-<pieza> · con su prueba, su golden o captura, y su entrada en el catálogo
5  PR [MICRO] → revisión prioritaria · todos rebasan
```

> **Regla del tercer uso.** No se sube a la biblioteca al segundo uso: al tercero. Y
> **nunca se duplica** un átomo — dos `Monto` con formatos distintos es cómo dos
> pantallas empiezan a mostrar el mismo saldo diferente.

> **Un átomo que existe en un mundo y no en el otro no es una asimetría: es un pedido.**
> Si B1 necesita un `ChipDeVencimiento` y lo sube a `packages/ui`, se abre el mismo
> micro-PR en `packages/diseno_flutter` **si alguna pantalla móvil lo va a usar**; si no,
> queda anotado en el catálogo como «solo web», con el motivo.

---

## 7 · Puntos de sincronización entre olas

- [ ] Todos los carriles del tramo fusionaron a `dev` (`git-flujo`: los PR apuntan a
      `dev`; `dev → main` solo cuando el tramo cierra entero y en verde)
- [ ] `dev` pasa el CI completo, incluidos `test:a11y` y Lighthouse CI
- [ ] Cada carril ejecutó su gate y lo registró en su informe
- [ ] Micro-PR pendientes, fusionados
- [ ] Cada máquina hace `git pull` y **regenera los clientes** (`./gradlew generateOpenApiClients`);
      nadie regenera tokens ni toca las bibliotecas de diseño: están congeladas
- [ ] **Revisión visual conjunta**: una máquina abre `/catalogo` (Angular) y Widgetbook
      (Flutter) lado a lado, en claro y oscuro, más las pantallas nuevas **contra la
      maqueta**. Es la única forma de detectar que dos carriles —o dos mundos— resolvieron
      lo mismo de dos maneras, o distinto de como se aprobó

---

## 8 · Montar una máquina nueva

```bash
git clone <repo> && cd Pasanaku
git checkout -b <usuario>/feature/carril-<id> origin/dev    # git-flujo · delta 3

yarn install --immutable          # sin yarn add
./gradlew generateOpenApiClients  # clientes/angular y clientes/dart — requiere Java 21

# las skills llegaron completas y la sesión las ve  (19 §1)
ls .claude/skills | grep -v README | wc -l        # 66
python3 scripts/verificar_boveda.py               # "índice de skills completo"

yarn dev:mock                     # Prism en :4010 — no necesita el backend levantado

# según el carril:
fvm install && fvm flutter pub get                # solo carriles Flutter — la versión está en .fvmrc
yarn dev:movil                    # flutter run contra Prism — requiere Android físico o emulador
yarn dev:backoffice               # ng serve
yarn dev:web                      # ng serve con SSR

yarn lint && yarn typecheck && yarn test:front && yarn test:a11y
```

**Los carriles de frontend no necesitan el backend corriendo.** Trabajan contra Prism.
La API real aparece en los puntos de sincronización y en la Fase F12.

Los carriles móviles necesitan además: Flutter con `fvm`, Android Studio (solo para el
SDK; el emulador **solo en P3**) y un dispositivo físico — **preferentemente de gama
baja**, que es el parque real en Bolivia. Las Dell corren `flutter run -d <dispositivo>`
por USB, nunca el emulador ([[17 Plan de acción secuencial · coordinación de cinco máquinas]] §7).

---

## 9 · Prompt de arranque de un carril

> La skill `arrancar-carril` es la **primera acción** al abrir el chat; este prompt es lo
> que se le pega después, con los huecos llenos.

```text
Sos el carril <ID> de la ola F<N> del frontend de AportaYa.
Mundo: <Flutter | Angular>.

ANTES DE ESCRIBIR NADA, leé en este orden:
  planes/00b Estándar de ejecución · código limpio, pruebas y calidad.md
  planes/10b Estándar de ejecución del frontend.md          ← cómo se escribe la UI, en los dos mundos
  planes/10 Plan maestro del frontend.md                    ← invariantes y stack
  planes/16 Carriles de frontend.md                         ← qué archivos podés tocar
  planes/18 Fichas de carril · las 38 unidades de trabajo.md   ← tu ficha, entera
  planes/<documento de tu fase>.md
  planes/22 Mapa de la maqueta · pantalla, carril y mundo.md   ← tus pantallas: ruta, organismos, delta
  planes/20 Maqueta de referencia · deltas del frontend.md      ← los deltas de tu carril, enteros
  docs/Views/AportaYa-Maqueta.html  (tus pantallas, en escenario optimista Y adverso)
  docs/CasosDeUso/CU-<NN> *.md  (todos los de tu carril — la sección INTERFAZ manda)
  .claude/skills/disenar-frontend/SKILL.md  +  .claude/skills/arquitectura-atomica/SKILL.md  +  docs/Views/Sistema-Diseno/
  .claude/skills/<movil-flutter | web-angular>/SKILL.md      ← la skill de tu mundo
  .claude/skills/web-backoffice/SKILL.md                     ← solo carriles B*

TU ALCANCE
  Fase:        F<N>
  Producto:    <movil | backoffice | web>
  Casos de uso: <lista>
  Rama:        <usuario>/feature/carril-<id>          (PR hacia dev)

POSEÉS EN EXCLUSIVA
  <tu directorio de pantallas o rutas>     (incluidos tu rutas.dart / <dominio>.routes.ts, tu dominio/ por CU, tu textos.*, tus pruebas y goldens)
  packages/simulado/ejemplos/<servicio>/   (el ejemplo de un CU lo crea el primer carril; el segundo lo consume)
  planes/informes/carril-<id>.md              (desde planes/informes/_plantilla-frontend.md)

NO TOCÁS (solo lectura)
  packages/tokens/  packages/ui/  packages/diseno_flutter/  clientes/angular/  clientes/dart/
  los shells: apps/movil/lib/{navegacion,proveedores,infraestructura}/ · apps/backoffice/src/app/{nucleo,layout}/ · app.routes.ts · apps/web/src/app/seo/
  package.json  yarn.lock  pubspec.yaml  pubspec.lock  angular.json  docker/  .github/
  ¿Necesitás un átomo, un token o un puerto nuevo? Micro-PR. NO lo crees en tu rama.

REGLAS QUE NO SE NEGOCIAN
  - Regla cero: la pantalla sale de la sección Interfaz del CU. No se inventa.
    Si falta algo crítico, PARÁS Y PREGUNTÁS. Si no, declarás el supuesto.
  - La maqueta manda sobre el cómo: cada pantalla se compara contra la suya en los dos
    escenarios, y el golden o la captura va AL LADO de la captura de la maqueta en el PR.
  - Las piezas de planes/22 §6 se usan del paquete de tu mundo; no se reimplementan en tu directorio.
  - Los cuatro estados en toda pantalla con datos, vía EstadoDePantalla.
  - Cero literales de diseño. Todo desde tokens (tokens.dart / var(--…)).
  - Ninguna llamada de red en un widget o componente. Ningún tipo reescrito a mano.
  - Ningún importe formateado fuera del átomo Monto.
  - Doble envío bloqueado en toda operación de dinero, con la misma clave.
  - Un solo botón naranja por pantalla.
  - Accesibilidad bloqueante: teclado, foco, contraste AA, semántica (web) · meetsGuideline (móvil).
  - Flutter: Platform.is* solo en infraestructura/. Android primero; iOS por pase (ADR-036).
  - Un golden o una captura no se actualiza sin mirar la diferencia.
  - No declarás nada terminado sin haber ejecutado el comando.

TERMINÁS CUANDO
  El gate de salida de tu fase está ejecutado, con evidencia en
  planes/informes/carril-<id>.md, y tu PR pasa el CI.

Empezá listando los componentes que vas a crear, por nivel, y esperá mi visto bueno.
```

---

## 10 · Cuando dos carriles se pisan igual

| Síntoma | Causa | Qué se hace |
| --- | --- | --- |
| Dos componentes casi iguales con nombres distintos | Ninguno abrió micro-PR | Se unifica en la biblioteca de diseño y se borran los dos. **Prioridad alta** |
| El mismo saldo se ve distinto en dos pantallas | Alguien formateó fuera de `Monto` | Se revierte. Es rechazo sin discusión |
| El mismo componente se ve distinto en la app y en el backoffice | Uno de los dos se apartó del catálogo | Se corrige el que se apartó de `docs/Views/Sistema-Diseno/`; se registra en la revisión visual conjunta |
| Un carril necesita un endpoint que no existe | El contrato no está escrito | Se pide al carril de backend. **No se inventa el contrato** |
| Conflicto en `<head>` entre W1 y W2 | W2 editó `ServicioMeta` | Se revierte: W2 pasa `data` |
| Un carril editó `app.routes.ts` o `navegacion/rutas.dart` | Quiso registrar una pantalla en el shell | Se revierte: la pantalla se registra en el archivo de rutas **de su dominio** |
| Dos pantallas resuelven el mismo estado vacío distinto | Falta revisión visual conjunta | Se unifica en el punto de sincronización |
| `pubspec.lock` o `yarn.lock` con diff en una rama de carril | Alguien agregó una dependencia | Se revierte; si hace falta, micro-PR con justificación |

---

## 11 · Lo que **no** se paraleliza

- **F0.T.** Tokens, generadores y Prism son el piso de todo; si se parte, cada
  andamiaje inventa el suyo.
- **Un cambio de tokens.** Para todo, se hace en troncal, se regeneran los dos
  artefactos, se revisan los dos catálogos en claro y oscuro, y recién ahí los carriles
  rebasan.
- **La Ola F4.** Accesibilidad, rendimiento y publicación se miden sobre el producto
  entero.
- **El pase de iOS.** Lo hace una máquina (P1) por bloque de pantallas, con las fichas
  de paridad de ADR-036; no se reparte.

---

## 12 · Transición: de Expo / React / Astro a Flutter / Angular

> Lo que existía al 2026-09-09 y qué se hace con cada cosa. Es el alcance del tramo
> **`TF`** de [[17 Plan de acción secuencial · coordinación de cinco máquinas]] §5.

### 12.1 · Qué se conserva, qué se porta, qué se borra

| Lo que había | Decisión | Dónde queda |
| --- | :-: | --- |
| `packages/ui/src/tokens/{paleta,temas}.ts` + `tokens-contra-boveda.spec.ts` | **se conserva** | se mueve a `packages/tokens/`; el JSON se extrae de ahí |
| `packages/ui/src/dinero/formatear.ts` + prueba de propiedad | **se conserva** | `packages/ui/monto/formatear.ts`; la prueba emite `vectores/monto.json` para Dart |
| `packages/ui/scripts/tokens-a-css.mjs` | **se conserva** | `packages/tokens/scripts/a-css.mjs`; se le suma `a-dart.mjs` |
| `packages/ui/src/web/*.tsx` (20 átomos React) | **se porta** | cada `.spec.tsx` se reescribe como `.spec.ts` de Angular Testing Library **antes** que el componente: la prueba vieja es el criterio de aceptación del átomo nuevo |
| `packages/ui/src/nativo/*.tsx` | **se porta** | a `packages/diseno_flutter/lib/atomos/`, con el mismo criterio |
| `packages/simulado/scripts/contratos-a-json.mjs` · `src/{escenarios,muestra,contratos,tipos}.ts` | **se conserva** | mismo paquete; alimentan `generar-ejemplos.mjs` |
| `packages/simulado/src/manejadores.ts` (MSW) | **se borra** | lo reemplaza `prism.yml` + `ejemplos/` |
| `apps/movil/**` (Expo) | **se borra** | el andamiaje Flutter es nuevo. Sobreviven como especificación: las 5 pruebas de `PantallaDeSaldo`, `enrutamiento-por-archivos.spec.ts` (reescrita como «una pantalla nueva no toca el shell») y las decisiones del informe `carril-F0-M` |
| `apps/backoffice/**` (React + Vite) | **se borra** | ídem: sobreviven las 6 pruebas de `PantallaDeBilletera` y las decisiones de `carril-F0-B` (token en memoria, `403` opaco, `staleTime: 0`) |
| `apps/web/**` (Astro) | **se borra** | ídem: sobreviven `robots.spec.ts`, `paginas-por-archivo.spec.ts` (ahora `rutas-de-servidor.spec.ts`) y las decisiones de `carril-F0-W` |
| `clientes/typescript/` (`typescript-fetch`) | **se borra** | lo reemplazan `clientes/angular/` y `clientes/dart/` |
| `turbo.json`, `package.json` de raíz, `.yarnrc.yml` | **se conserva** | se agregan las tareas nuevas; `nodeLinker: node-modules` ya no hace falta por Metro, pero se deja: Angular CLI también lo prefiere |
| `planes/informes/carril-F0-*.md`, `carril-F1.md`, `carril-M1.md` | **se conserva** | son historia. Cada carril rehecho abre **una sección nueva** «Rehecho en Flutter / Angular · TF» en el mismo informe, no un archivo nuevo |
| ADR-004, ADR-041 | **se conserva** | ADR-004 marcado *superada por ADR-044*; ADR-041 con nota de enmienda en el *con qué* |
| `landing/` | **no se toca** | es la página estática de marca en GitHub Pages; no es `apps/web` |

### 12.2 · El orden del tramo TF

```
TF.0  P1   F0.T ─ tokens, generadores, Prism, tareas de raíz, CI     ← bloquea todo
TF.1  P3   F0-M (Flutter)   ∥   P4   F0-B (Angular)   ∥   P5   F0-W (Angular SSR)
TF.2  P3   F1-M (diseno_flutter)   ∥   P4   F1-W (@aportaya/ui + /catalogo)
TF.3  revisión visual conjunta · congelar packages/ui y packages/diseno_flutter · cerrar el tramo
```

**Los carriles de backend de P3, P4 y P5 se corren un tramo.** Está escrito en
[[17 Plan de acción secuencial · coordinación de cinco máquinas]] §5; acá solo se
declara el costo: un tramo de tres puestos. Es el precio de cambiar de stack **antes**
de la primera pantalla de negocio, que es el momento más barato que va a tener.

### 12.2b · Lo ejecutado el 2026-09-09 y el 2026-09-10 (TF.0 a TF.2)

| Paso | Estado | Evidencia |
| :-: | :-: | --- |
| TF.0 · `F0.T` | ✅ | `packages/tokens` (13 pruebas), clientes Angular y Dart de los 14 servicios, `packages/simulado` con 151 ejemplos y Prism respondiendo los seis escenarios, tareas de raíz, job `frontend` del CI |
| TF.1 · `F0-M` | ✅ | Flutter con enchufe por dominio, `PantallaDeSaldo` con sus cuatro estados: 22 pruebas (unidad, widget, contrato, a11y, 2 goldens), `flutter analyze` limpio, barrido en verde |
| TF.1 · `F0-B` | ✅ | Angular zoneless con enchufe por dominio, `PantallaDeBilletera`: 15 pruebas (4 estados + 403 + reintento, axe en 3 estados, enchufe, contrato), `noindex`, imagen NGINX |
| TF.1 · `F0-W` | ✅ | Angular SSR híbrido, `/` prerenderizada y `/plazos` en servidor con `CalculadoraDePlazo`: 11 pruebas (robots, rutas de servidor, páginas, calculadora), contenido en Markdown procesado en el build |
| TF.2 · `F1-M` | ✅ | `packages/diseno_flutter`: 94 archivos, una pieza por archivo en `atomos/`, `moleculas/`, `organismos/`, `moviles/`; Widgetbook en `lib/catalogo/`; 44 pruebas (unidad, widget, a11y con `meetsGuideline`, 14 goldens claro/oscuro); `flutter analyze --fatal-infos` limpio; barrido `diseno` en verde (cero `Colors.`/`Color(`/`EdgeInsets` numérico, ningún archivo > 200 líneas) |
| TF.2 · `F1-W` | ✅ | `packages/ui`: 60 carpetas, una pieza por carpeta, todas las de [[22 Mapa de la maqueta · pantalla, carril y mundo]] §6 columna Angular; `/catalogo` en `apps/web` prerenderizado y `noindex` (meta + robots); 37 unitarias + 3 de axe sobre el catálogo entero en claro y oscuro; 6 e2e de Playwright con capturas en `apps/web/capturas/` y control de área táctil ≥ 44 px; barrido `ui` en verde |
| TF.3 | 🟡 | Revisión lado a lado hecha sobre las capturas de `/catalogo` y los goldens de Flutter (detalle en `carril-F1.md` §Rehecho). Los goldens usan la fuente Ahem: comparan geometría y color, no tipografía. Falta la mirada humana y el congelamiento formal |

Detalle en `planes/informes/carril-F0-M.md`, `carril-F0-B.md`, `carril-F0-W.md` y `carril-F1.md`, sección «Rehecho».

### 12.3 · Gate del tramo TF

- [x] Gate de salida F0 y gate de salida F1 (para F1-W **y** F1-M), ejecutados (2026-09-10; el contraste pieza por pieza queda medido por captura, no por axe: jsdom no lo calcula)
- [ ] Ninguna referencia a Expo, React, Vite, Astro, MSW ni Maestro en `apps/`, `packages/`
      ni en `package.json` (`grep` en el CI, como barrido)
- [ ] `clientes/typescript/` no existe; `clientes/angular/` y `clientes/dart/` regenerados sin diff
- [x] Los informes de F0-M, F0-B, F0-W y F1 tienen su sección «Rehecho»
- [x] `docs/Arquitectura/Estructura del repositorio.md` actualizado
- [ ] La revisión visual conjunta ejecutada y registrada

## Ver también

[[19 Contrato de carril · conflicto cero, skills y calidad verificada]] · [[18 Fichas de carril · las 38 unidades de trabajo]] · [[17 Plan de acción secuencial · coordinación de cinco máquinas]] · [[22 Mapa de la maqueta · pantalla, carril y mundo]] · [[10 Plan maestro del frontend]] · [[10b Estándar de ejecución del frontend]] · [[07 Carriles de trabajo concurrente]] · [[ADR-044 Frontend en Angular y Flutter]] · [[informe]] · [[disenar-frontend]]
