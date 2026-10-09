---
tags:
  - moc
  - plan
  - carriles
  - coordinacion
titulo: "Plan de acción secuencial — coordinación de cinco máquinas"
fecha: 2026-08-16
alcance: backend + frontend, ejecutados a la vez sobre el parque real de máquinas
---

# Plan de acción secuencial

> **Qué agrega este documento.** [[07 Carriles de trabajo concurrente]] dice cómo se
> reparte el **backend** y [[16 Carriles de frontend]] cómo se reparte el
> **frontend**. Ninguno de los dos sabe que el otro existe al momento de contar
> máquinas. Este documento los **fusiona en una sola secuencia**, la ata al **parque
> real de hardware** y define el **ritual de coordinación**: qué pasa el lunes, qué
> pasa al cerrar un tramo y quién decide qué.

> [!important] Jerarquía
> Cuando este documento y los otros dos difieran, **manda este** en todo lo que sea
> *quién, en qué máquina y en qué orden*. Los otros dos siguen mandando en *qué se
> construye y qué archivos posee cada carril*. Los deltas están explícitos en §4:
> ninguno se aplica en silencio.

> [!warning] El parque lo opera **una sola persona**
> Cinco máquinas, un operador. Eso no cambia el reparto de archivos ni la secuencia
> —cambia **dónde está el cuello de botella**: no en la máquina, en la atención.
> Tres consecuencias que atraviesan todo el documento:
>
> 1. **No se camina entre cinco laptops.** Un asiento (el Mac) y cuatro obreros por
>    SSH. §2.
> 2. **Máximo dos carriles en primer plano.** Los otros tres corren sobre
>    especificación cerrada. §10.
> 3. **Esto no va cinco veces más rápido, va entre dos y dos y media.** Lo que
>    paraleliza gratis es el tiempo de máquina, no el de decisión. §10.

---

## 1 · Los cinco defectos que este plan corrige

| # | Defecto | Dónde estaba | Cómo se corrige |
| :-: | --- | --- | --- |
| 1 | **El pico real es de 8 carriles, no de 5.** Backend Ola 2 (5 carriles) corre a la vez que frontend Ola F1 (3). Cada documento contaba solo su mitad | [[07 Carriles de trabajo concurrente]] §3 · [[16 Carriles de frontend]] §2 | La unidad de planificación deja de ser la **ola** y pasa a ser el **tramo** (§5): cinco casillas fijas, siempre llenas, que pueden mezclar carriles de dos olas |
| 2 | **Las máquinas figuran como intercambiables** | ambos | §2 y §3: cinco **puestos** con máquina asignada y capacidad declarada. Solo el Mac compila iOS; solo la Legion tiene emulador Android acelerado; solo Ubuntu tiene Docker nativo |
| 3 | **Dos convenciones de rama incompatibles** | 07 §8 usa `<usuario>/feature/…`, 16 §8 usa `carril/f…` | Manda `git-flujo`: `<usuario>/feature/carril-<id>`. Delta 3 |
| 4 | **La Ola 0 deja cuatro máquinas paradas**, y la Ola F0 no puede arrancar porque su gate pide el cliente generado, que nace en la Fase 2 | [[01 Fase 0 · Cimientos del repositorio]] · [[11 Fases F0 y F1 · Cimientos y sistema de diseño]] gate de entrada | Deltas 1 y 2: los tres contratos OpenAPI base con CU-01 y sus clientes generados se adelantan al cierre de la Fase 0, y la Ola F0 se parte en tres andamiajes concurrentes |
| 5 | **Doce casos de uso no tienen carril.** `13_contabilidad_erp` (CU-100–106) y `14_publicidad_campanas` (CU-110–114) existen en `docs/entidades/` y en `docs/CasosDeUso/`, pero **ninguna ola, ninguna fase y ningún carril los nombra** | los tres documentos de plan, que siguen contando **87** casos de uso cuando son **99** | Fases **18** y **19** de backend y **F13** y **F14** de frontend — 18 en **T5** (adelantada: condición de licencia), 19 en T9, F13 en T9 y F14 en T10 (§5). Y una tarea explícita: **escribir los cuatro documentos de fase que faltan** (§11) |

### El número que hay que tener en la cabeza

**36 unidades de fase** (21 de backend, 15 de frontend) sobre **5 puestos** son
**siete fases por máquina, en serie**. No hay reparto que baje eso. Todo lo que este
plan puede hacer —y hace— es que ninguna máquina esté parada esperando a otra, y que
ninguna termine trabajo que después se tira por un conflicto.

> [!important] Decisión tomada el 2026-08-18 — la fase 18 se adelanta a T5
> CU-100 cita Ley 393 (libros y conservación) y Código de Comercio. **El dueño
> decidió tratar los libros contables formales como condición de la licencia**: la
> fase 18 (ERP) corre en **T5** y desplaza a 3C a T6. La cascada completa —F8 a
> T7–T8, 5B a T9, F14 al tramo nuevo T10— está en §5 y en
> [[20 Saneamiento del plan · huecos de la migración a microservicios]] §9.
> La publicidad (fase 19) sigue sin bloquear el lanzamiento y va a T9.

---

## 2 · El parque de máquinas

### Lo que hay

| Máquina | Especificación | Sistema | Supuesto declarado |
| --- | --- | --- | --- |
| **Mac M5** | Apple Silicon M5 | macOS | La más rápida del parque |
| **Ubuntu** | Core i5 · 16 GB DDR4 | Ubuntu | Docker **nativo**, sin máquina virtual |
| **Legion** | Lenovo Legion (gama gaming) | Windows | ≥16 GB y GPU discreta ⇒ emulador Android acelerado |
| **Dell A** | Inspiron · 16 GB | Windows | Gráficos integrados |
| **Dell B** | Inspiron · 16 GB | Windows | Gráficos integrados |

> **Confirmado.** Las tres son Windows: Docker corre por WSL2 en las tres y aplica la
> regla de disco de §7 sin excepción. El único Docker nativo del parque es el de
> Ubuntu, y eso lo vuelve la máquina de integración y de medición.
>
> **Confirmado también: el parque lo opera una sola persona** (§10).

### Lo que cada una puede y no puede

| Capacidad | Mac M5 | Ubuntu | Legion | Dell A | Dell B |
| --- | :-: | :-: | :-: | :-: | :-: |
| Compilar iOS · simulador · Xcode · App Store | **única** | ✗ | ✗ | ✗ | ✗ |
| Emulador Android acelerado · Patrol | lento | ✗ | **sí** | ✗ | ✗ |
| Docker nativo · Testcontainers sin VM | VM | **sí** | WSL2 | WSL2 | WSL2 |
| `docker compose` completo + `test:e2e` | sí | **sí** | ajustado | ✗ | ✗ |
| Angular CLI · Prism · Playwright (sin Docker) | sí | sí | sí | **sí** | **sí** |
| `flutter run` sobre Android físico de gama baja | sí | sí | sí | **sí** | **sí** |
| Compilar iOS con Flutter (Xcode) | **única** | ✗ | ✗ | ✗ | ✗ |

### La topología: un asiento y cuatro obreros

El parque lo opera **una sola persona**. No se camina entre cinco laptops: se opera
desde una y las otras cuatro corren sin pantalla.

| Rol físico | Máquina | Cómo se accede | Nota |
| --- | --- | --- | --- |
| **Asiento** | Mac M5 | teclado y pantalla reales | Es donde vive tu atención. Todo lo que exige decidir pasa por acá |
| **Obrero** | Ubuntu | SSH + `tmux`, **sin escritorio** | Sin GUI quedan 2–3 GB más para Docker. Es el obrero más capaz |
| **Obrero** | Legion | SSH + `tmux` · pantalla **solo** para el emulador Android | El emulador es lo único que no se opera a ciegas |
| **Obrero** | Dell A | SSH + `tmux` | Carriles sin Docker pesado |
| **Obrero** | Dell B | SSH + `tmux` | Carriles sin Docker pesado |

**Lo que hay que montar antes del tramo T0:**

- **Red privada entre las cinco** (Tailscale o equivalente) con SSH por clave. Si las
  cinco están siempre en la misma LAN, alcanza con SSH por clave y direcciones fijas.
- **`tmux` en cada obrero**, una sesión con nombre por puesto: `tmux new -s P2`. Sin
  eso, cerrar la tapa del laptop mata la corrida de pruebas de dos horas.
- **En las tres Windows: SSH que caiga directo en WSL2**, no en PowerShell. El clon
  vive **dentro** del sistema de archivos de WSL2, nunca en `/mnt/c` (§7).
- **`git` como único canal entre máquinas.** Nada de carpetas compartidas ni de copiar
  archivos por red: si algo tiene que viajar de una máquina a otra, viaja por un
  commit. Es lo que hace que el reparto de propiedad de archivos signifique algo.

**Las tres consecuencias que ordenan todo el plan:**

1. **Los carriles de frontend no necesitan Docker.** Trabajan contra Prism
   ([[16 Carriles de frontend]] §8). Son el trabajo natural de las Dell.
2. **Los carriles de backend necesitan Postgres real** por Testcontainers
   ([[ADR-008 Pruebas]]). Son el trabajo natural de Ubuntu, Legion y Mac.
3. **El Mac es insustituible en un solo punto: iOS.** Por eso no se lo gasta en lo
   que otra máquina puede hacer una vez que la app móvil arranca (tramo T4).

---

## 3 · Los cinco puestos

> **La regla que reemplaza a «un carril = una máquina».**
> ```
> un puesto = una máquina = un clon = un chat = una identidad, para todo el proyecto
> ```
> El **carril rota**; el **puesto no**. Una máquina que cambia de rol cada ola pierde
> su caché de Docker, su base sembrada, su contexto de chat y su especialidad.

| Puesto | Máquina | Especialidad | Por qué esa máquina |
| :-: | --- | --- | --- |
| **P1 · Troncal y móvil** | **Mac M5** | Ola 0 completa → toda la app móvil → publicación | La más rápida va donde nadie la puede reemplazar: primero al troncal, que bloquea a las otras cuatro; después a iOS, que no corre en ninguna otra |
| **P2 · Núcleo de dinero** | **Ubuntu** | contable → billetera → aportes → entregas → convergencia | Docker nativo: es el carril que más pruebas de integración corre. Además **el mismo hilo mental** recorre toda la cadena del dinero, sin cambiar de máquina |
| **P3 · Dominio del pasanaku** | **Legion** | sistema de diseño → grupos → transparencia → garantía → backoffice de operación → E2E Android | Segunda en potencia, y única con emulador acelerado para cerrar el ciclo móvil |
| **P4 · Identidad y cumplimiento** | **Dell A** | habilitación → auditoría → entregas parcial → cumplimiento → backoffice de cumplimiento | Mucha regla y mucho CRUD, carga de Docker moderada |
| **P5 · Periferia y web** | **Dell B** | notificaciones → tarifas → organizador → sitio público → SEO → GEO | Los carriles que menos infraestructura piden. Angular y Prism no necesitan Docker |

**El identificador de un carril pasa a ser `<puesto>·<carril>`**, por ejemplo
`P2·2A`. El informe de carril arranca declarando puesto y máquina, para que un gate
verde sea rastreable a un hardware concreto.

---

## 4 · Los cinco deltas al plan vigente

Nada de esto se aplica en silencio. Cada delta es un cambio explícito a
[[07 Carriles de trabajo concurrente]] o a [[16 Carriles de frontend]].

| # | Delta | Documento que cambia | Por qué |
| :-: | --- | --- | --- |
| **1** | **los tres contratos OpenAPI base se adelantan al cierre de la Fase 0 y sus clientes generados se publican en `dev`**, en vez de nacer en la Fase 2 | [[01 Fase 0 · Cimientos del repositorio]] §0.6 | Es un archivo de contrato por servicio cuya forma ya fija el [[00c Recetario · implementar un caso de uso]], más los clientes `clientes/angular` y `clientes/dart` que se generan de él (ADR-044). Desbloquea la Ola F0 **un tramo entero antes**, y el cliente generado existe igual para todos los carriles |
| **2** | **La Ola F0 se parte en un paso troncal y tres andamiajes concurrentes**: móvil, backoffice y web, un puesto cada uno | [[16 Carriles de frontend]] §2 | Son **tres directorios nuevos**: colisión cero. Lo compartido —tokens, generadores, Prism, CI— lo hace P1 primero, como `F0.T`. **Corregido por ADR-044:** F1 ya **se puede partir** en `F1-W` (Angular) y `F1-M` (Flutter), porque son dos bases de código sobre un mismo `tokens.json` generado |
| **3** | **Una sola convención de rama**: `<usuario>/feature/carril-<id>` | [[16 Carriles de frontend]] §8, que decía `carril/f<ola>-<id>` | Manda `git-flujo`. Dos convenciones conviviendo son dos filtros de CI y dos formas de perder un PR |
| **4** | **La unidad de planificación es el tramo, no la ola** | [[07 Carriles de trabajo concurrente]] §3 y §7 | Un tramo puede mezclar carriles de dos olas mientras las dependencias estén fusionadas en `dev`. El §10 de 07 ya lo permitía para un carril adelantado; acá se vuelve la regla. El punto de sincronización sigue existiendo: pasa a ser **por tramo** |
| **5** | **La deuda de tramo se declara, no se arrastra** | nuevo | Cuando un tramo tiene más carriles listos que casillas, los que no entran se anotan como **deuda** en el informe, con el tramo donde se pagan. Un carril que "quedó pendiente" sin fecha es un carril que no se hace |

---

## 5 · El plan secuencial · once tramos, más el de transición

**Cómo se lee.** Cada fila es un tramo. Cada tramo llena las cinco casillas. Un tramo
cierra con el ritual de §6, y recién entonces empieza el siguiente. Los tramos **no
son semanas**: duran lo que dure su carril más lento.

### T0 · Cimientos · **bloqueante**

| Puesto | Atención | Trabajo | Entregable |
| :-: | :-: | --- | --- |
| **P1** Mac | **primer plano** | **Backend Fase 0** — monorepo, Docker, ADR de desviación, lint, corredores de JUnit, esqueletos, CI · **+ Delta 1**: los tres contratos OpenAPI base (con `CU-01`) y sus clientes generados, publicados en `dev` | Gate de salida de la Fase 0, ejecutado |
| **P2** Ubuntu | segundo plano | **Verificación independiente del gate de P1** sobre su propia máquina, y afinado de Postgres para desarrollo | El gate pasa en **dos** máquinas, no en una |
| **P3** Legion | segundo plano | Preparación de carril: leer los CU de **transparencia (`3B`) y garantía (`4B`)** —los dos que la ficha le asigna—, declarar las piezas por nivel | `informes/carril-P3.md` con la declaración |
| **P4** Dell A | segundo plano | Preparación de carril: CU de habilitación y auditoría | `informes/carril-P4.md` |
| **P5** Dell B | segundo plano | `landing/` + preparación: CU de notificaciones y tarifas | `informes/carril-P5.md` |

> **Por qué P2 duplica el gate.** «Funciona en mi máquina» sobre los cimientos cuesta
> las veinte fases siguientes. Es la única duplicación deliberada del plan, y con un
> solo operador cuesta casi nada: es correr los mismos comandos por SSH en otra
> máquina.
>
> **Por qué los otros tres no escriben código.** No hay dónde: el monorepo no existe.
> Pero la declaración de piezas y las cinco preguntas de frontera transaccional del
> [[00c Recetario · implementar un caso de uso]] son trabajo real, obligatorio, y su
> archivo es **propiedad exclusiva del carril**: colisión cero. **Las tres
> declaraciones se revisan en bloque al cerrar T0**, no una por una a medida que
> salen: revisar en bloque es lo que evita tres cambios de contexto.

### T1 · Núcleo transversal ∥ andamiajes de frontend

| Puesto | Trabajo |
| :-: | --- |
| **P1** Mac | **Backend Fases 1 y 2** — carriles **`T1` → `T2`** en serie: capa de datos generada, después `comun/`, worker y outbox. **Congela `packages/datos/src/entidades/`** |
| **P2** Ubuntu | Acompaña el troncal: segunda ejecución de los gates de las Fases 1 y 2, convenciones de `pruebas/fixtures/`, script de reseteo rápido de base |
| **P3** Legion | **F0 · andamiaje móvil** *(Delta 2)* — **ejecutado con Expo; se rehace en Flutter en TF** |
| **P4** Dell A | **F0 · andamiaje backoffice** *(Delta 2)* — **ejecutado con React + Vite; se rehace en Angular en TF** |
| **P5** Dell B | **F0 · andamiaje web** *(Delta 2)* — **ejecutado con Astro; se rehace en Angular SSR en TF** |

**Cierra con:** gate de la Fase 2 **y** gate de salida F0. Desde acá, el esquema y las
entidades no se regeneran más: cualquier cambio de modelo para todo el proyecto
([[07 Carriles de trabajo concurrente]] §11).

### T2 · Ola 1 de backend ∥ sistema de diseño

| Puesto | Carril | Fase | Alcance |
| :-: | :-: | :-: | --- |
| **P1** Mac | 1A | 3 | `identidad` · CU-01, 04, 05, 08, 09 |
| **P2** Ubuntu | 1B | 5 | `nucleo-financiero` (libro contable) · CU-24 |
| **P3** Legion | — | **F1** | **`packages/ui`** — tokens y primer corte de átomos, **ejecutado con React; se rehace en TF en dos carriles paralelos, uno Angular y otro Flutter**. Los tokens y la prueba contra la bóveda sobreviven tal cual |
| **P4** Dell A | 1C | 4 | `cumplimiento` (parcial) · CU-02, 03, 06, 40, 46 |
| **P5** Dell B | 1D | 12 | `notificaciones` · CU-80–83 |

**Hito:** al cerrar T2, **CU-24 punta a punta** valida el stack entero (Spring +
jOOQ + RLS + outbox) antes de que T3 abra tres servicios sobre él
([[20 Saneamiento del plan · huecos de la migración a microservicios]] §5).

### TF · Transición de stack del frontend — **se inserta donde el proyecto está hoy**

> [[ADR-044 Frontend en Angular y Flutter]], 2026-09-09. Los andamiajes de F0 y el
> primer corte de F1 se hicieron con Expo, React + Vite y Astro. **Se rehacen en
> Flutter y Angular antes de la primera pantalla de negocio**, que es el momento más
> barato. Detalle en [[16 Carriles de frontend]] §12 y en las fichas `F0.T`, `F0-M`,
> `F0-B`, `F0-W`, `F1-W`, `F1-M` de [[18 Fichas de carril · las 38 unidades de trabajo]].

| Paso | Puesto | Carril | Alcance |
| :-: | :-: | :-: | --- |
| **TF.0** | **P1** Mac | **F0.T** | `packages/tokens` (tokens.json → css + dart, vectores de `Monto`), `generarClienteAngular` y `generarClienteDart` en Gradle, borrar `clientes/typescript`, Prism con ejemplos por CU, tareas de raíz, job `frontend` del CI. **Bloquea TF.1** |
| **TF.1** | **P3** Legion | **F0-M** | Andamiaje Flutter con enchufe por dominio, `custom_lint`, `PantallaDeSaldo` contra Prism |
| **TF.1** | **P4** Dell A | **F0-B** | Andamiaje Angular del backoffice, interceptores, `PantallaDeBilletera`, `noindex` |
| **TF.1** | **P5** Dell B | **F0-W** | Andamiaje Angular SSR, `rutas-de-servidor.spec.ts`, `robots.txt` generado, `Dockerfile.web` |
| **TF.2** | **P3** Legion | **F1-M** | `packages/diseno_flutter` completo, goldens, Widgetbook |
| **TF.2** | **P4** Dell A | **F1-W** | `packages/ui` (Angular) completo, `/catalogo`, capturas |
| **TF.2** | **P5** Dell B | — | Toma el primer carril de backend de la deuda declarada, o **2B** si todavía no cerró |
| **TF.3** | **P1** Mac | — | **Revisión visual conjunta** golden ↔ captura · congelar `packages/ui` y `packages/diseno_flutter` · cerrar el tramo |
| todo TF | **P2** Ubuntu | — | **No toca frontend.** Sigue con el carril de backend que tenga en curso |

**En serie dentro del tramo:** en P3, `F0-M → F1-M`; en P4, `F0-B → F1-W`. El andamiaje
cierra antes de que su sistema de diseño empiece.

**Cierra con:** gate de salida F0 **y** los dos gates de F1 ejecutados, la revisión
visual conjunta registrada, y el barrido «ninguna referencia a Expo, React, Vite, Astro,
MSW ni Maestro en `apps/`, `packages/` ni `package.json`» en verde.

**Costo declarado.** Los carriles de backend que P3, P4 y P5 tenían asignados en el
tramo en que TF se inserta **se corren un tramo**, y se anotan como deuda declarada
(delta 5). P1 y P2 no se detienen. Es un tramo de tres puestos, pagado una sola vez.

### T3 · Ola 2 de backend ∥ los shells

| Puesto | Carril | Fase | Alcance |
| :-: | :-: | :-: | --- |
| **P1** Mac | 2C | 8 | `grupos` · CU-20, 59, 60, 62–65, 68, 69 |
| **P2** Ubuntu | 2A | 6 | `nucleo-financiero` (billetera) · CU-10–17, 50, 57 |
| **P3** Legion | — | **F2 → F6** | shell móvil (Flutter), después shell backoffice (Angular): los dos son chicos y van en serie en la misma máquina |
| **P4** Dell A | 2D | 15 | `auditoria` · CU-07, 54, 55, 58, 98 |
| **P5** Dell B | 2B | 7 | `tarifas` · CU-30–36 |

**Deuda declarada:** carril 2E (`organizador`, fase 14) → se paga en T4 · fase F9
(sitio público) → se paga en T5.
**Hito:** al cerrar T3 se puede validar el stack de punta a punta con **CU-31**.

### T4 · El Mac se va a móvil y no vuelve

| Puesto | Carril | Fase | Alcance |
| :-: | :-: | :-: | --- |
| **P1** Mac | — | **F3** | **móvil · identidad** — CU-01–09, 40, 46. Desde acá P1 es móvil y nada más |
| **P2** Ubuntu | 3A | 9 | `aportes` · CU-19, 21, 51, 99 |
| **P3** Legion | 3B | 13 | `transparencia` · CU-61, 70–76, 97 |
| **P4** Dell A | 3D | 10a | `entregas` (parcial) · CU-18 |
| **P5** Dell B | 2E | 14 | `organizador` · CU-90–93, 95, 96 *(deuda de T3)* |

**Deuda declarada:** carril 3C (cumplimiento ASFI, fase 16) → T5.

> **Por qué el Mac cambia de lado justo acá.** Porque a partir de este punto existe
> todo lo que la app móvil necesita para no inventar nada: `packages/ui` congelado
> (T2), el shell (T3) y los contratos de identidad (T2). Y porque si el móvil arranca
> más tarde, la publicación en tiendas —que solo el Mac puede hacer— queda al final
> de una fila de seis fases.

### T5 · Ola 4 de backend ∥ billetera móvil ∥ ERP adelantado

| Puesto | Carril | Fase | Alcance |
| :-: | :-: | :-: | --- |
| **P1** Mac | — | **F4** | móvil · billetera — CU-10–19, 30–33, 57 · **+ pase de iOS del bloque de identidad (F3)** al abrir el tramo |
| **P2** Ubuntu | 4A | 10b | `entregas` · CU-22, 28 |
| **P3** Legion | 4B | 11 | `garantia` · CU-23, 25–27, 29, 66, 67 |
| **P4** Dell A | **5A** | **18** | **`erp` · CU-100–106** *(adelantado: condición de licencia — decisión del 2026-08-18)* |
| **P5** Dell B | — | **F9** | sitio público (Angular SSR) *(deuda de T3)* |

**Deuda declarada:** carril 3C (cumplimiento ASFI, fase 16) → **T6**.

> **El pase de iOS va por bloque y en P1**, que es la única máquina con Xcode
> ([[ADR-036 Android primero]]). Cada bloque de pantallas cierra en Android en su
> tramo y entra al pase **al abrir el tramo siguiente**, antes de la primera pantalla
> nueva: F3 se pasa al abrir T5, F4 al abrir T6, y los bloques de F5 a medida que
> cierran en T6 y T7. Así ninguna pantalla llega a F12 sin su ficha de paridad, y el
> Mac no acumula un pase de cuarenta pantallas al final.

### T6 · Convergencia de backend ∥ frontend a fondo

| Puesto | Carril | Fase | Alcance |
| :-: | :-: | :-: | --- |
| **P1** Mac | — | **F5** | móvil · pasanaku — CU-20–29, 52, 53, 59–76. **Es la fase más grande del frontend**: se extiende a T7 · **+ pase de iOS del bloque de billetera (F4)** al abrir el tramo |
| **P2** Ubuntu | T | **17** | E2E, rendimiento, resiliencia, ensayo de restauración, seguridad, despliegue |
| **P3** Legion | — | **F7** | backoffice · operación (26 CU, Angular) |
| **P4** Dell A | **3C** | **16** | `cumplimiento` · CU-41–45, 47–49, 52, 53, 56, 94 *(deuda de T5)* |
| **P5** Dell B | — | **F10** | SEO — `<Meta>`, JSON-LD, sitemap |

**Hito:** al cerrar T6, **todos los casos de uso de backend salvo publicidad están
implementados** (94 de 99) y `PasanakuCompletoE2ETest.java` tiene que pasar. La
fase 17 cierra su E2E **después** de que 3C fusiona, dentro del mismo tramo.

> **Por qué la fase 17 va a Ubuntu.** Docker nativo: el `compose` completo, la prueba
> de carga y el ensayo de restauración se miden sin la capa de virtualización que
> falsea los números en macOS y en WSL2.

### T7 · Cierre del frontend

| Puesto | Trabajo |
| :-: | --- |
| **P1** Mac | **F5** continúa hasta cerrar · **pase de iOS de cada bloque de F5** a medida que cierra (grupo, turno, aporte, entrega, transparencia, reclamo) |
| **P2** Ubuntu | Corrección de lo que aparezca en la fase 17 · **autorización a desplegar el backend** |
| **P3** Legion | **F7 → B5**: F7 continúa y, al cerrar, B5 backoffice de sistemas · arma el E2E móvil con **Patrol sobre emulador Android** |
| **P4** Dell A | **F8** — backoffice · cumplimiento y gobierno (38 CU). Arranca acá con los contratos de 3C ya en `dev`; **se extiende a T8** |
| **P5** Dell B | **F11** — GEO: `robots.txt`, `llms.txt`, espejos `.md`, primera medición en los cuatro motores |

### T8 · Ola F4 · publicación

| Puesto | Carril | Fase | Alcance |
| :-: | :-: | :-: | --- |
| **P1** Mac | — | **F12** | E2E, accesibilidad, rendimiento, seguridad · **`flutter build ipa` y envío a App Store; `appbundle` a Play; Shorebird en `produccion`**. El `ipa` no es transferible. **Riesgo declarado:** su cierre y la publicación ocurren al final del tramo, después de que F8 fusiona |
| **P2** Ubuntu | T | 17 (cola) | Despliegue del backend en el entorno **`ensayo`** y real, sondas, respaldos, ensayo de restauración |
| **P3** Legion | — | E2E móvil | **Patrol contra `ensayo`** — el ciclo móvil completo que F12 necesita, en emulador y en Android físico |
| **P4** Dell A | — | F8 (cola) | backoffice · cumplimiento y gobierno, hasta cerrar |
| **P5** Dell B | — | F11 (cola) | Publicación del sitio · primera medición GEO en los cuatro motores |

### T9 · Los módulos de cola — publicidad y backoffice ERP

| Puesto | Carril | Fase | Alcance |
| :-: | :-: | :-: | --- |
| **P1** Mac | — | F12 (cola) | Corrección de hallazgos de revisión de tienda · actualizaciones OTA |
| **P2** Ubuntu | — | operación | Guardia de producción, respaldos, indicadores |
| **P3** Legion | — | **F13** | backoffice · contabilidad ERP *(fase nueva)* |
| **P4** Dell A | **5B** | **19** | **14 publicidad y campañas** · CU-110–114 *(carril nuevo — defecto 5)* |
| **P5** Dell B | — | — | Segunda medición GEO · verificación de indexación · corrección de contenido |

**Hito:** al cerrar T9, **los 99 casos de uso están implementados.**

### T10 · Cierre — el backoffice de publicidad

| Puesto | Carril | Fase | Alcance |
| :-: | :-: | :-: | --- |
| **P1** Mac | — | — | OTA y correcciones de tienda que sigan llegando |
| **P2** Ubuntu | — | operación | Guardia de producción, respaldos, indicadores |
| **P3** Legion | — | F13 (cola) | Cierre de F13 si quedó deuda |
| **P4** Dell A | — | **F14** | backoffice · publicidad y campañas *(fase nueva)* |
| **P5** Dell B | — | — | Tercera medición GEO · corrección de contenido |

### Resumen visual

```
          P1 Mac         P2 Ubuntu      P3 Legion     P4 Dell A     P5 Dell B
T0   ┃  BE F0+contratos  verifica       preparación   preparación   landing
T1   ┃  BE F1+F2         acompaña       FE-F0 móvil   FE-F0 backof  FE-F0 web      (stack anterior)
T2   ┃  1A identidad     1B contable    FE-F1 ui      1C habilit.   1D notif.      (stack anterior)
TF   ┃  FE-F0.T →ADR     2A/4A siguen   F0-M → F1-M   F0-B → F1-W   F0-W          ◆ transición Flutter/Angular
T3   ┃  2C grupos        2A billetera   FE-F2 móvil   FE-F6 backof  2B tarifas
T4   ┃  FE-F3 móvil ▼    3A aportes     3B transp.    3D entregas   2E organiz.
T5   ┃  FE-F4 móvil      4A entregas    4B garantía   5A ERP ★▲     FE-F9 web
T6   ┃  FE-F5 móvil      BE fase 17     FE-F7 backof  3C cumplim.   FE-F10 SEO
T7   ┃  FE-F5 (sigue)    despliegue     FE-F7 + E2E   FE-F8 backof  FE-F11 GEO
T8   ┃  FE-F12 tiendas   ensayo+prod    E2E Patrol    FE-F8 (cola)  publicación
T9   ┃  correcciones     operación      FE-F13 ERP ★  5B publi. ★   medición GEO
T10  ┃  OTA              operación      FE-F13 cola   FE-F14 pub ★  medición GEO
          ▲ desde T4 el Mac es móvil y nada más
          ★ carriles nuevos · defecto 5   ▲ 5A adelantado: condición de licencia
          ◆ TF se inserta en el punto en que el proyecto está al 2026-09-09: el backend sigue su ola; los tres
            puestos de frontend rehacen F0 y F1 y los carriles de backend que tenían se corren un tramo
```

---

## 6 · El ritual de coordinación

Sin esto, cinco máquinas son cinco proyectos.

### Dos roles con nombre

| Rol | Quién | Qué hace |
| --- | --- | --- |
| **Guardián del troncal** | **P1** hasta cerrar T1 · **P2** desde T2 | Único que fusiona a `dev`. Revisa los micro-PR **el mismo día**. Decide qué sube a `packages/dominio` o a `packages/ui` |
| **Integrador de tramo** | **P2** siempre | Al cerrar cada tramo corre la suite completa contra `dev` ya fusionado, en Ubuntu. Un carril verde aislado y rojo integrado es información: casi siempre es un átomo duplicado |

### Apertura de tramo

1. El guardián publica en `planes/informe.md` la fila del tramo: puesto → carril.
2. Cada puesto: `git fetch && git rebase origin/dev`. **Nadie regenera nada.**
3. Cada puesto abre su rama `<usuario>/feature/carril-<id>` desde `dev`.
4. Cada puesto pega en su chat el prompt de arranque (§9 de
   [[07 Carriles de trabajo concurrente]] o de [[16 Carriles de frontend]]) con el
   encabezado de puesto de §9 de este documento.
5. Cada puesto **declara sus piezas por nivel y espera el visto bueno** antes de
   escribir el primer archivo.

### Todos los días, en cada puesto

```bash
git fetch && git rebase origin/dev     # primero: absorber los micro-PR de ayer
./gradlew build --offline              # por si un micro-PR tocó el catálogo de versiones
./gradlew spotlessCheck check test
# al terminar el día: una línea en planes/informes/carril-<id>.md
```

Una línea de informe por día: qué CU tocó, qué prueba corrió, con qué resultado, y
qué lo bloquea. **Nunca «avancé en el backend».**

### Cierre de tramo

- [ ] Los cinco puestos fusionaron a `dev`
- [ ] `dev` pasa el CI completo
- [ ] Cada puesto ejecutó el gate de su fase y **pegó la salida del comando** en su informe
- [ ] Los micro-PR pendientes están fusionados
- [ ] **P2 corrió la suite completa contra `dev` integrado**
- [ ] La **deuda del tramo** está anotada con el tramo donde se paga
- [ ] `planes/informe.md` actualizado: olas, carriles, CU implementados, hitos, riesgos
- [ ] `dev → main` solo si el tramo cierra entero y en verde

### Micro-PR

Sin cambios respecto de [[07 Carriles de trabajo concurrente]] §6, con dos precisiones
de coordinación:

- **Lo revisa el guardián, el mismo día.** Un micro-PR que duerme dos días bloquea a
  un puesto o —peor— lo empuja a duplicar el átomo.
- **El puesto no espera.** Sigue con lo que no depende de eso mientras se revisa.

---

## 7 · Reglas de máquina

### El presupuesto de 16 GB

En una máquina de 16 GB, un carril de backend en marcha ya consume:

| Pieza | Aproximado |
| --- | --- |
| Postgres por Testcontainers | ~1 GB |
| **Kafka en compose** | ~1 GB |
| **El servicio del carril, en la JVM** | ~0,5 GB |
| Gradle (demonio + compilación) | ~2–3 GB |
| Editor con indexado de Java sobre el monorepo | ~2–3 GB |
| Navegador | ~2 GB |
| Windows + WSL2, o macOS | ~4 GB |

**No entra nada más, y esta cuenta es más ajustada que la anterior:** la JVM y Kafka
suman donde antes había un proceso de Node. De ahí, **cinco** reglas duras:

1. **En una Dell: o el emulador Android, o Docker. Nunca los dos.** El emulador pide
   4–6 GB. Para probar en móvil, las Dell usan **`flutter run -d <dispositivo>` sobre un
   Android físico de gama baja** por USB — que además es el parque real en Bolivia.
2. **`TESTCONTAINERS_REUSE_ENABLE=true`** y **un** contenedor de Postgres por
   máquina, no uno por suite. Levantar Postgres por corrida de prueba es la mitad del
   tiempo perdido de un carril de backend.
3. **El `docker compose` completo y el `e2eTest` corren en P2 (Ubuntu) y en CI, no en
   cada máquina.** Ya lo decía [[07 Carriles de trabajo concurrente]] §8; acá tiene
   nombre de máquina.
4. **Un carril levanta el perfil `base` y SU servicio. Nunca los quince.** Es la regla
   que hace vivible la arquitectura de servicios en 16 GB
   ([[ADR-025 Empaquetado y despliegue de los servicios]]). Contra los otros trece se
   programa por su OpenAPI y se prueba con dobles.
5. **Un solo demonio de Gradle por máquina**, con `org.gradle.jvmargs=-Xmx2g` fijado
   en `gradle.properties`. Sin tope, el demonio se come la memoria que Kafka y
   Postgres necesitan, y la máquina empieza a intercambiar sin avisar.

### Por máquina

| Máquina | Regla propia |
| --- | --- |
| **Mac M5** | Es **arm64**. Si una imagen no tiene `arm64`, corre emulada: **un gate corrido bajo emulación no se declara verde** — se repite en P2. Único punto de publicación iOS: si el Mac cae, la publicación se detiene (riesgo declarado en §8) |
| **Ubuntu** | Docker nativo ⇒ es la **máquina de integración y de medición**. Los números de rendimiento de la fase 17 salen de acá y de ningún otro lado |
| **Legion** | Emulador Android acelerado ⇒ **Patrol corre acá**. Si es Windows: el clon del repositorio va **dentro del sistema de archivos de WSL2**, nunca en `/mnt/c` — los volúmenes montados sobre NTFS destruyen el tiempo de compilación de Gradle y de Testcontainers |
| **Dell A y B** | Carriles sin Docker pesado por diseño. Misma regla de WSL2 que la Legion. Un Android físico de gama baja por máquina |

---

## 8 · Cuando algo se desvía

| Situación | Qué se hace |
| --- | --- |
| **Un puesto termina antes** | Toma el primer carril de la **deuda declarada** (§5), no uno del tramo siguiente — salvo que sus dependencias ya estén en `dev` |
| **Un puesto se atrasa y traba el cierre del tramo** | El tramo **no se abre a medias**. Se corta el alcance del carril atrasado por CU completos, se declara lo que falta como deuda con tramo asignado, y se cierra |
| **Cae una máquina** | El puesto se reasigna al que tenga la especialidad más cercana y el tramo pierde una casilla. **Excepción: si cae el Mac, la fase F12 (tiendas) se detiene** — no hay reasignación posible. Es el único punto único de falla del parque |
| **`dev` queda rojo tras fusionar un tramo** | No se abre el siguiente hasta arreglarlo. Lo arregla P2 (integrador) con el puesto que lo rompió |
| **Dos puestos necesitan el mismo átomo** | Gana el que abrió el micro-PR primero; el segundo lo consume ya fusionado. **Nunca se duplica** |
| **Un carril de frontend necesita un contrato que no existe** | No lo inventa. Lo pide al puesto de backend que lo posee y trabaja en otra pantalla. Regla cero |
| **Aparece un cambio de modelo** | **Para todo el parque.** Se hace en el troncal, se regenera, se verifica la bóveda, se fusiona, y recién ahí las cinco máquinas rebasan |

---

## 9 · Encabezado de puesto

Se pega **antes** de invocar `/arrancar-carril` ([[07 Carriles de trabajo concurrente]] §10).
Son los datos que la skill no puede deducir sola: cuál de las cinco máquinas es y qué
rol adicional lleva en este tramo.

```text
PUESTO
  Identificador:  P<N>
  Máquina:        <Mac M5 | Ubuntu i5 | Legion | Dell A | Dell B>
  Especialidad:   <la de §3>
  Tramo actual:   T<N> de 10
  Rol adicional:  <ninguno | guardián del troncal | integrador de tramo>

  Presupuesto de esta máquina: <la fila propia de §7>

  Leo, además de lo que pida la skill:
    planes/17 §5 — solo mi fila del tramo
    planes/17 §6 — el ritual de coordinación

CIERRO EL TRAMO CUANDO
  Mi gate de fase está ejecutado, con la salida del comando pegada en mi informe,
  y mi PR pasa el CI. No antes, y no por declaración.
```

Y a continuación:

```text
/arrancar-carril

Carril <ID>, ola <N>. Servicio: <nombre>. CU: <lista>.
```

> **El encabezado de puesto y la skill no se solapan.** El encabezado dice *qué
> máquina soy*; la skill dice *qué carril tomo y cómo se trabaja*. Repetir lo segundo
> acá es lo que hacía que cinco máquinas arrancaran distinto.

---

## 10 · Un solo operador

El parque es de cinco máquinas y de **una persona**. Todo lo anterior sigue siendo
válido; lo que cambia es dónde está el límite.

### Primer plano y segundo plano

Con cinco carriles y una sola atención, la restricción deja de ser la máquina y pasa
a ser tu cabeza. Por eso cada carril está, en cada momento, en uno de dos estados:

| Estado | Cuántos | Qué es | Qué necesita de vos |
| --- | :-: | --- | --- |
| **Primer plano** | **máximo 2** | El carril donde hay que **decidir**: un CU ambiguo, una pieza nueva, una frontera transaccional, una decisión de diseño | Tu atención continua |
| **Segundo plano** | los otros 3 | Especificación cerrada y visto bueno dado. Corre pruebas, instala, compila, o avanza sobre una declaración de piezas ya aprobada | Una visita por vuelta (abajo) |

**La regla:** un carril **entra** en primer plano cuando pide una decisión, y **sale**
cuando la decisión está escrita —en el CU, en el informe, o como supuesto declarado.
Ningún carril se queda en primer plano «por las dudas»: eso es exactamente lo que
convierte cinco carriles en uno.

### El asiento por tramo

Qué carril ocupa tu atención en cada tramo. Los que no figuran corren en segundo plano.

| Tramo | Primer plano | Por qué ese |
| :-: | --- | --- |
| **T0** | P1 · Fase 0 | Los cimientos no se delegan: cada decisión de acá se paga veinte fases |
| **T1** | P1 · Fases 1 y 2 | `comun/` y la frontera transaccional son la plantilla de los 99 CU |
| **T2** | P1 · 1A identidad · **y** P3 · F1 sistema de diseño | El primer módulo real y el único carril con decisiones estéticas irreversibles |
| **T3** | P2 · 2A billetera · **y** P1 · 2C grupos | Billetera es el carril con más fronteras transaccionales del proyecto |
| **T4** | P1 · F3 móvil · **y** P2 · 3A aportes | La primera pantalla real de producto y el cobro con QR |
| **T5** | P1 · F4 billetera móvil · **y** P4 · 5A ERP | Dinero en pantalla, y el ERP adelantado que condiciona la licencia |
| **T6** | P2 · fase 17 · **y** P4 · 3C cumplimiento | Rendimiento y despliegue se deciden mirando números; cumplimiento define lo que el supervisor ve |
| **T7** | P1 · F5 pasanaku móvil | La fase más grande del frontend, y la que define el producto |
| **T8** | P1 · F12 tiendas | Trámite de revisión: no se delega ni se automatiza |
| **T9** | P3 · F13 y P4 · 5B | Módulos de cola con documento de fase recién escrito (§11) |
| **T10** | P4 · F14 | El último backoffice; lo demás es operación |

### Qué paraleliza gratis y qué no

| Paraleliza gratis · la máquina trabaja sola | **No** paraleliza · te consume a vos |
| --- | --- |
| `./gradlew build`, `docker pull`, `generateJooq` | Decidir la frontera transaccional de un CU |
| Suites de pruebas, Testcontainers, E2E | Revisar una declaración de piezas |
| Sesiones de agente sobre especificación cerrada | Verificar un gate: leer la salida real del comando |
| Lectura de CU y redacción de la declaración | Resolver un criterio que dos carriles resolvieron distinto |
| Medición GEO, Lighthouse, escaneos | Aceptar o rechazar un micro-PR |

> **La consecuencia práctica:** los carriles **lentos** van a los obreros y los
> **ambiguos** al asiento. Un carril que tarda mucho pero no pregunta nada es el
> candidato perfecto para un obrero; uno que pregunta cada veinte minutos hay que
> tenerlo delante.

### La vuelta — reemplaza al standup

Dos o tres veces por día, **en orden fijo P1 → P5**, cuatro pasos por puesto:

```bash
ssh P<N>                          # o: tmux attach -t P<N>
# 1. leer la última línea de planes/informes/carril-P<N>.md
# 2. desbloquear lo que pida decisión, o anotarlo para el bloque de primer plano
# 3. relanzar lo que quedó parado
# 4. escribir una línea de informe
```

**El orden fijo es lo importante, no la frecuencia.** Sin orden fijo, un carril de
segundo plano se queda tres días sin visita y produce trabajo que hay que tirar
entero.

### Los dos roles son dos sombreros

Guardián del troncal e integrador de tramo (§6) son la misma persona: vos. Nombrarlos
igual sirve, y mucho: convierte «reviso cuando me acuerde» en «reviso en un momento
definido, con una lista». Un rol que no tiene momento no se ejerce.

### La expectativa honesta

Cinco máquinas con un operador **no son cinco veces más rápido**. El paralelismo real
está en el **tiempo de máquina** —instalaciones, pruebas, builds, sesiones sobre
especificación cerrada—, no en el **tiempo de decisión**, que sigue siendo uno solo y
en serie.

**Esperá entre 2× y 2,5×** respecto de trabajar en una sola máquina. Y ese factor se
pierde entero si los carriles de segundo plano quedan sin visita: rehacer una fase
cuesta más que las cuatro que corrieron en paralelo mientras tanto.

Lo que sí ganás además, y no es menor: **aislamiento**. Cinco clones, cinco bases,
cinco ramas. Un carril que rompe su base de datos, su `node_modules` o su rama **no
toca a los otros cuatro**. Con una sola máquina, cada experimento arriesga todo.

### El riesgo propio de esta configuración

Un carril de segundo plano que corre mucho tiempo sin visita produce trabajo que hay
que tirar entero. El control ya está en el plan y con un solo operador pasa a ser **lo
único** que separa avanzar de rehacer:

> **La declaración de piezas por nivel, antes de escribir el primer archivo, con visto
> bueno explícito.** Con cinco personas eso es higiene. Con una persona y cinco
> carriles, es el plan entero.

---

## 11 · Lo primero que hay que hacer

En este orden, y nada de esto es opcional:

1. **Montar la topología de §2**: red privada, SSH por clave, `tmux` por puesto, WSL2
   en las tres Windows. Sin esto no hay cinco máquinas: hay cinco laptops.
2. **Verificar las 66 skills en cada máquina** y cargar las obligatorias de cada
   carril — [[19 Contrato de carril · conflicto cero, skills y calidad verificada]] §1
   y §2. Un carril que trabaja de memoria inventa lo que las skills evitan.
3. **Escribir los cuatro documentos de fase que faltan** — fase 18
   (`13_contabilidad_erp`, CU-100–106), fase 19 (`14_publicidad_campanas`,
   CU-110–114), **F13 y F14** (sus backoffice), con el formato de los demás y
   absorbiendo lo útil de `docs/implementation/plan-de-implementacion.md`
   ([[20 Saneamiento del plan · huecos de la migración a microservicios]] §9).
   **La fase 18 es lo primero: corre en T5.**
4. ~~Resolver la pregunta de cumplimiento abierta~~ **Resuelta el 2026-08-18**: los
   libros contables formales se tratan como condición de la licencia; la fase 18
   corre en T5 y la cascada está aplicada en §5.
5. **Corregir el conteo de casos de uso**: los planes dicen 87, son **99**.
6. **Aplicar los deltas 1, 2 y 3** a [[01 Fase 0 · Cimientos del repositorio]] y a
   [[16 Carriles de frontend]]. Un plan con dos convenciones de rama no arranca.
7. **Crear los cinco informes** `informes/carril-P<N>.md` desde `informes/_plantilla.md`.
8. **Aceptar que la Ola 0 se alarga.** Los generadores, las doce pruebas de barrido y
   el verificador de criterios de
   [[19 Contrato de carril · conflicto cero, skills y calidad verificada]] se
   construyen en `T0` y `T2`, antes del primer caso de uso. Se paga una vez y se cobra
   38 veces; la alternativa —confiar en que cada carril se acuerde— sale más cara y se
   descubre tarde.
9. **Abrir el tramo T0**: P1 arranca la Fase 0 en primer plano; P2, P3, P4 y P5
   arrancan su preparación en segundo plano. Nadie espera.

## Ver también

[[19 Contrato de carril · conflicto cero, skills y calidad verificada]] · [[18 Fichas de carril · las 38 unidades de trabajo]] · [[00 Plan maestro]] · [[07 Carriles de trabajo concurrente]] · [[16 Carriles de frontend]] ·
[[10 Plan maestro del frontend]] · [[01 Fase 0 · Cimientos del repositorio]] ·
[[11 Fases F0 y F1 · Cimientos y sistema de diseño]] · [[informe]] · [[git-flujo]] ·
[[plan-por-fases]]
