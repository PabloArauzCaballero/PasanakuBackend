---
tags:
  - moc
  - plan
  - carriles
titulo: "Fichas de carril — las 38 unidades de trabajo, una por una"
fecha: 2026-08-16
alcance: los 21 carriles de backend y los 20 de frontend (Flutter y Angular, ADR-044)
---

# Fichas de carril

> **Qué es este documento.** [[17 Plan de acción secuencial · coordinación de cinco máquinas]]
> dice **en qué orden** y **en qué máquina**. Este dice **qué es exactamente cada
> carril**: sus casos de uso con nombre, qué necesita ya fusionado antes de empezar,
> qué entrega y quién lo está esperando, qué archivos posee, cuál es su gate propio y
> **dónde se rompe**.
>
> Una ficha se lee **entera** antes de abrir el carril, y se pega junto al prompt de
> arranque de [[07 Carriles de trabajo concurrente]] §9 o [[16 Carriles de frontend]] §9.

## 36 fases, 41 carriles

La diferencia son el paso troncal y los tres andamiajes de la fase F0 (delta 2, más el
troncal `F0.T` que suma [[ADR-044 Frontend en Angular y Flutter]]), la fase F1 partida
en dos mundos (`F1-W` Angular, `F1-M` Flutter) y el backoffice de sistemas (`B5`, delta
D-2). Todo lo demás es un carril por fase. El título del archivo conserva «38» porque
está enlazado desde toda la bóveda; el número vigente es este.

| | Fases | Carriles |
| --- | :-: | :-: |
| Backend | 21 (0–17, más 10a/10b separadas, más 18 y 19) | 21 |
| Frontend | 15 (F0–F14) | 20 |
| **Total** | **36** | **41** |

---

## Cómo se lee una ficha

| Campo | Qué contesta |
| --- | --- |
| **Necesita en `dev`** | Qué tiene que estar **fusionado** antes de abrir el carril. Si algo de acá falta, el gate de entrada está **bloqueado** y no se empieza |
| **Entrega, y quién espera** | Qué produce este carril que otro carril **no puede inventar**. Es la lista que hay que tener escrita antes de declarar el carril terminado |
| **Excepción de propiedad** | Solo aparece cuando el carril posee algo fuera de la fórmula de abajo |
| **Gate propio** | Lo que se verifica **además** del gate de salida de su fase |
| **Gate que suma D-nn** | Lo que agregó un delta de la maqueta después de escrita la ficha. Se verifica igual que el gate propio |
| **Dónde se rompe** | El modo de falla característico. No es un riesgo genérico: es lo que efectivamente sale mal en ese carril |

> [!important] Ocho carriles tienen gate agregado por los deltas D-15 a D-22
> Los primeros catorce deltas de [[20 Maqueta de referencia · deltas del frontend]] eran
> de presentación: la maqueta desglosaba más, pero el backend ya resolvía lo mismo. **Los
> ocho nuevos no.** Tres de ellos cambian una transacción, un contrato o una respuesta, y
> si un carril de backend se entera cuando el frontend llega a componer, hay que rehacer
> las dos puntas.
>
> | Carril | Deltas | Qué le cambia |
> | :-: | --- | --- |
> | **`2C`** grupos y turnos | D-15 · D-20 · D-22 | El canje **no ocupa cupo**; la oferta de permuta y su veredicto de riesgo; aportes devuelve el ciclo completo |
> | **`2E`** organizador | D-16 | El contrato devuelve **cada requisito con su umbral**, no un veredicto |
> | **`2B`** tarifas | D-19 | El descuento por nivel es **concepto de tarifa**, no cálculo del cliente |
> | **`4B`** garantía | D-17 | Aviso anticipado de dificultad, y el expediente **legible por su titular** |
> | **`5B`** publicidad | D-21 | El vale, con sus cuatro reglas de canje |
> | **`F4`** móvil billetera | D-12 · D-17 · D-19 · D-21 · D-22 | Calendario del ciclo completo, vales, desglose del cobro |
> | **`F5`** móvil pasanaku | D-15…D-18 · D-20 | Solicitud de ingreso, habilitación, mora, soporte, mercado |
> | **`F7`** backoffice operación | D-15 · D-16 · D-18 | Habilitaciones, solicitudes escaladas, reclamos con puerta real |
>
> **Al tomar cualquiera de esos ocho, se lee el delta antes que la ficha.**

### La fórmula de propiedad — no se repite en cada ficha

**Todo carril de backend posee, y nadie más toca:**

```
servicios/<su-servicio>/**          TODO, sin excepción:
    build.gradle.kts                  sus dependencias
    src/main/resources/
      application.yml                 su configuración — no hay archivo compartido
      openapi/<servicio>.yaml         su contrato, solo los CU de su lista
    src/main/java/…/{dominio,infraestructura,aplicacion,web,trabajos}/
    src/test/java/…/                  incluidos sus fixtures
    descriptor.yml                    réplicas, recursos, sondas → genera el k8s
    README.md
despliegue/compose/<servicio>.yml   solo si necesita un auxiliar
planes/informes/carril-P<N>.md
```

> **Esa lista es el cambio entero de la arquitectura de servicios.** Antes un carril
> poseía un directorio *dentro* de una aplicación compartida, y la aplicación tenía
> piezas que todos tocaban. Ahora posee un desplegable completo: **no hay ningún
> archivo fuera de ahí que necesite tocar para entregar un caso de uso**.

**Todo carril de frontend posee, y nadie más toca:**

```
<su directorio de pantallas o rutas>/**        incluidos su rutas.dart / <dominio>.routes.ts, su dominio/, su textos.*, sus pruebas y goldens
packages/simulado/ejemplos/<servicio>/CU-NN.json   solo los CU que crea primero; los demás los consume
planes/informes/carril-P<N>.md                     desde _plantilla-frontend.md
```

**Y ninguno toca nunca:** `sql/`, `docs/`, `scripts/`, `plataforma/`,
`gradle/libs.versions.toml`, `settings.gradle.kts`, `despliegue/Dockerfile`,
`despliegue/k8s/`, `clientes/angular/` y `clientes/dart/` (generados), `packages/tokens/`,
`packages/ui/`, `packages/diseno_flutter/`, los shells (`navegacion/`, `proveedores/`,
`infraestructura/`, `nucleo/`, `layout/`, `app.routes.ts`), `pubspec.*`, `angular.json`,
`.github/`, `.claude/skills/` — **ni el `openapi/` de otro servicio**, que se lee para
generar su cliente pero jamás se edita. Para todo eso está el micro-PR.

### La escala de tamaño

Cruza casos de uso con entidades del esquema del servicio. No es una estimación de horas: es cuánta
superficie tiene el carril.

| | Significa |
| :-: | --- |
| ●○○○○ | 1–4 CU · un carril de una sentada |
| ●●○○○ | 5–7 CU |
| ●●●○○ | 8–10 CU |
| ●●●●○ | 11–15 CU · se parte en bloques dentro del mismo carril |
| ●●●●● | 16+ CU · el carril domina su tramo entero |

---

## Índice

| Ficha | Puesto | Tramo | Fase | Qué es | Tamaño |
| :-: | :-: | :-: | :-: | --- | :-: |
| [`T0`](#t0--cimientos-del-repositorio) | P1 | T0 | 0 | Cimientos del repositorio | ●●●○○ |
| [`T1`](#t1--capa-de-datos) | P1 | T1 | 1 | Capa de datos generada | ●●○○○ |
| [`T2`](#t2--núcleo-transversal) | P1 | T1 | 2 | Núcleo transversal | ●●●●○ |
| [`1A`](#1a--identidad-y-usuarios) | P1 | T2 | 3 | Identidad y usuarios | ●●●○○ |
| [`1B`](#1b--contabilidad-de-partida-doble) | P2 | T2 | 5 | Contabilidad de partida doble | ●○○○○ |
| [`1C`](#1c--habilitación-y-límites) | P4 | T2 | 4 | Habilitación y límites | ●●○○○ |
| [`1D`](#1d--notificaciones) | P5 | T2 | 12 | Notificaciones | ●●○○○ |
| [`2A`](#2a--billetera-y-custodia) | P2 | T3 | 6 | Billetera y custodia | ●●●●○ |
| [`2B`](#2b--tarifas-comisiones-y-facturación) | P5 | T3 | 7 | Tarifas, comisiones y facturación | ●●●○○ |
| [`2C`](#2c--grupos-y-turnos) | P1 | T3 | 8 | Grupos y turnos | ●●●●○ |
| [`2D`](#2d--auditoría-y-reportes) | P4 | T3 | 15 | Auditoría y reportes | ●●○○○ |
| [`2E`](#2e--organizador-y-automatización) | P5 | T4 | 14 | Organizador y automatización | ●●●○○ |
| [`3A`](#3a--aportes-y-pagos-con-qr) | P2 | T4 | 9 | Aportes y pagos con QR | ●●○○○ |
| [`3B`](#3b--transparencia-y-reputación) | P3 | T4 | 13 | Transparencia y reputación | ●●●○○ |
| [`3C`](#3c--cumplimiento-asfi-y-uif) | P4 | T6 | 16 | Cumplimiento ASFI y UIF | ●●●●● |
| [`3D`](#3d--cuenta-bancaria-de-destino) | P4 | T4 | 10a | Cuenta bancaria de destino | ●○○○○ |
| [`4A`](#4a--entrega-del-fondo) | P2 | T5 | 10b | Entrega del fondo | ●○○○○ |
| [`4B`](#4b--garantía-e-incumplimiento) | P3 | T5 | 11 | Garantía e incumplimiento | ●●●○○ |
| [`5T`](#5t--convergencia-y-despliegue) | P2 | T6–T8 | 17 | Convergencia y despliegue | ●●●●○ |
| [`5A`](#5a--contabilidad-erp-carril-nuevo) | P4 | T5 | 18 | Contabilidad ERP ★ | ●●●○○ |
| [`5B`](#5b--publicidad-y-campañas-carril-nuevo) | P4 | T9 | 19 | Publicidad y campañas ★ | ●●○○○ |
| [`F0.T`](#f0t--troncal-del-frontend) | P1 | TF | F0 | Troncal del frontend: tokens, clientes, Prism | ●●○○○ |
| [`F0-M`](#f0-m--andamiaje-móvil-flutter) | P3 | TF | F0 | Andamiaje móvil (Flutter) | ●●○○○ |
| [`F0-B`](#f0-b--andamiaje-backoffice-angular) | P4 | TF | F0 | Andamiaje backoffice (Angular) | ●●○○○ |
| [`F0-W`](#f0-w--andamiaje-web-angular-ssr) | P5 | TF | F0 | Andamiaje web (Angular SSR) | ●●○○○ |
| [`F1-W`](#f1-w--sistema-de-diseño--angular) | P4 | TF | F1 | Sistema de diseño · Angular | ●●●●○ |
| [`F1-M`](#f1-m--sistema-de-diseño--flutter) | P3 | TF | F1 | Sistema de diseño · Flutter | ●●●●○ |
| [`F2`](#f2--shell-móvil) | P3 | T3 | F2 | Shell móvil | ●●○○○ |
| [`F6`](#f6--shell-backoffice) | P3 | T3 | F6 | Shell backoffice | ●●○○○ |
| [`F3`](#f3--móvil--identidad) | P1 | T4 | F3 | Móvil · identidad | ●●●○○ |
| [`F9`](#f9--sitio-público) | P5 | T5 | F9 | Sitio público | ●●●○○ |
| [`F4`](#f4--móvil--billetera) | P1 | T5 | F4 | Móvil · billetera | ●●●●○ |
| [`F5`](#f5--móvil--pasanaku) | P1 | T6–T7 | F5 | Móvil · pasanaku | ●●●●● |
| [`F7`](#f7--backoffice--operación) | P3 | T6–T7 | F7 | Backoffice · operación | ●●●●● |
| [`F8`](#f8--backoffice--cumplimiento) | P4 | T7–T8 | F8 | Backoffice · cumplimiento | ●●●●● |
| [`B5`](#b5--backoffice-de-sistemas) | P3 | T7 | F8.D | Backoffice de sistemas | ●●●○○ |
| [`F10`](#f10--seo) | P5 | T6 | F10 | SEO | ●●○○○ |
| [`F11`](#f11--geo) | P5 | T7 | F11 | GEO | ●●○○○ |
| [`F12`](#f12--publicación) | P1 | T8 | F12 | Publicación | ●●●○○ |
| [`F13`](#f13--backoffice--contabilidad-erp-carril-nuevo) | P3 | T9 | F13 | Backoffice · ERP ★ | ●●○○○ |
| [`F14`](#f14--backoffice--publicidad-carril-nuevo) | P4 | T10 | F14 | Backoffice · publicidad ★ | ●●○○○ |

★ carriles nuevos: cubren los 12 CU que ningún plan nombraba
([[17 Plan de acción secuencial · coordinación de cinco máquinas]] defecto 5).

---

# Parte A · Los 21 carriles de backend

## Ola 0 · Troncal — P1 · Mac M5

> Los tres carriles de la Ola 0 son **el mismo puesto en serie**. Bloquean todo lo
> demás: mientras corren, los otros cuatro puestos están en preparación (T0) o en los
> andamiajes de frontend (T1).

### `T0` · Cimientos del repositorio

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P1** · Mac M5 · **T0** · fase 0 |
| **Documento** | [[01 Fase 0 · Cimientos del repositorio]] |
| **Alcance** | monorepo Gradle, Docker, esquemas y roles, análisis estático, los cinco corredores de JUnit, descubrimiento de servicios por barrido de `settings.gradle.kts` (no hay `apps/api` ni `apps/worker`: no existen), CI, **y los tres contratos OpenAPI base con `CU-01` más su cliente `clientes/typescript` generado** (delta 1) |
| **Tamaño** | ●●●○○ · cero CU de negocio, pero **habilita las 20 fases restantes** |

**Necesita en `dev`.** Nada de código. Sí de la bóveda: `sql/aplicar.sql` corre limpio
sobre base vacía y `verificar_boveda.py` está en verde.

**Entrega, y quién espera.**

| Entrega | Lo espera |
| --- | --- |
| Monorepo yarn, `tsconfig.base.json`, alias `@aportaya/*` | **todos** |
| Las reglas propias de análisis estático (ArchUnit) y los cinco corredores de JUnit | **todos** |
| `docker-compose.yml` con Postgres, PgBouncer, Kafka y NGINX | **todos los de backend** |
| **Todas** las dependencias declaradas en el catálogo de versiones de una vez | **todos** — nadie corre una dependencia agregada al catálogo |
| Los tres contratos OpenAPI base | `F0-M`, `F0-B`, `F0-W` (T1) |

**Excepción de propiedad.** Posee **el repositorio entero**. Es el único carril del que
eso es cierto.

**Gate propio.** El gate de salida de la fase 0, ejecutado **dos veces**: en P1 y por
SSH en P2 (§5 T0 de [[17 Plan de acción secuencial · coordinación de cinco máquinas]]).

**Dónde se rompe.** Por olvidar una dependencia en el catálogo de versiones. Cada una dependencia agregada al catálogo desde una rama
de carril es un conflicto de `yarn.lock` con las otras cuatro máquinas. Se instala
ahora todo lo que las 20 fases van a necesitar, aunque parezca prematuro.

---

### `T1` · Capa de datos

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P1** · Mac M5 · **T1** · fase 1 |
| **Documento** | [[02 Fases 1 y 2 · Capa de datos y núcleo transversal]] |
| **Alcance** | `plataforma/comun-datos`: clases jOOQ **generadas** desde la base viva por `EntityGenerator`, `DineroType`, configuración de PgBouncer en modo *transaction* |
| **Tamaño** | ●●○○○ · 305 tablas generadas, no escritas a mano |

**Necesita en `dev`.** `T0` cerrado. La base con las 305 tablas aplicadas y los 20
catálogos mínimos sembrados.

**Entrega, y quién espera.** Las clases de jOOQ por esquema — **lo esperan los 18
carriles de backend siguientes**, y **queda congelado al cerrar**: desde acá nadie
regenera nada.

**Gate propio.** `./gradlew generateJooq compileJava`` en verde. Es la
verificación de que las entidades siguen a la base y no al revés (§4 R1+R2 del
[[00 Plan maestro]]).

**Dónde se rompe.** Cuando alguien edita una entidad a mano para «arreglar» un tipo.
A partir de ahí la bóveda y el código son dos verdades, y el `git diff --exit-code`
del CI empieza a fallar sin que nadie entienda por qué.

---

### `T2` · Núcleo transversal

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P1** · Mac M5 · **T1** · fase 2 |
| **Documento** | [[02 Fases 1 y 2 · Capa de datos y núcleo transversal]] |
| **Alcance** | `plataforma/comun-web/`: `conContexto`, contexto de sesión para RLS por `SET LOCAL`, catálogo de errores, idempotencia, el relevo del outbox y el planificador con ShedLock, **descubrimiento de servicios por barrido de Gradle** |
| **Tamaño** | ●●●●○ · define la forma de los 99 casos de uso |

**Necesita en `dev`.** `T1` cerrado, con las entidades congeladas.

**Entrega, y quién espera.**

| Entrega | Por qué es bloqueante |
| --- | --- |
| `conContexto` | **Una transacción por caso de uso.** Ningún carril la reimplementa |
| Contexto de RLS (`app.usuario_id`, `app.rol`) por `SET LOCAL` | Sin contexto no hay política de fila: todo carril lo usa igual |
| Catálogo `constraint_name → AP-CU<NN>-<nn>` | Traduce el error de PostgreSQL a mensaje útil |
| `respuesta_idempotente` y su envoltorio | Todo endpoint con efecto |
| Outbox + relevo a Kafka corriendo | `1D` arranca directo sobre esto en T2 |
| **Descubrimiento de servicios por barrido de `settings.gradle.kts`** | Elimina el conflicto nº 1: no hay `app.module.ts` que registrar |

**Gate propio.** Las diez pruebas de `CU-00` (el pipeline transversal), y una prueba
que agrega un módulo vacío y comprueba que **se descubre solo**, sin editar nada.

**Dónde se rompe.** Si la frontera transaccional queda ambigua acá, los 99 casos de
uso la copian ambigua. Es el carril donde más caro sale improvisar y donde el
[[00c Recetario · implementar un caso de uso]] es literal, no orientativo.

---

## Ola 1 · T2 — cuatro carriles a la vez

### `1A` · Identidad y usuarios

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P1** · Mac M5 · **T2** · fase 3 |
| **Módulo** | `01_identidad_usuarios` — 25 entidades |
| **Servicio** | `identidad` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `identidad` · `svc_identidad` — no lee ningún otro esquema |
| **Documento** | [[03 Fases 3 a 7 · Identidad, habilitación y núcleo de dinero]] |
| **Tamaño** | ●●●○○ · 5 CU · es el carril que estrena el recetario |

**Casos de uso.**

| CU | Nombre | Nota |
| :-: | --- | --- |
| 01 | Registro y apertura de billetera | El contrato ya existe desde `T0`: acá se implementa |
| 04 | Autenticar con MFA y registrar dispositivo | Argon2id, dispositivo de confianza |
| 05 | Aceptar contrato de adhesión y tarifario | `R-CON-06` · hash del documento aceptado |
| 08 | Asignar y revocar roles de operador | **Revocar cierra sesiones activas** |
| 09 | Cambiar credenciales y solicitar la baja | La baja no borra: cierra |

**Necesita en `dev`.** `T2` completo. Nada más: las 25 tablas ya existen y se siembran
fixtures propios.

**Entrega, y quién espera.**

| Entrega | Lo espera |
| --- | --- |
| Contratos CU-01, 04, 05, 08, 09 | `F3` móvil identidad (P1, T4) |
| Sesión con contexto de RLS poblado | **todos** los carriles siguientes |
| `usuario` y `rol` utilizables como fixture | `1C`, `2D`, `3C` |

**Gate propio.** Prueba negativa de permisos por cada rol, y prueba de que **revocar un
rol invalida las sesiones abiertas** — no solo las futuras.

**Dónde se rompe.** En la baja. CU-09 no borra al usuario: cierra la cuenta y conserva
la evidencia que el regulador exige. Un `DELETE` acá rompe la conservación de Ley 393
y se lleva puesto el histórico contable.

---

### `1B` · Contabilidad de partida doble

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P2** · Ubuntu · **T2** · fase 5 |
| **Módulo** | parte contable de `03_aportes_pagos_qr` — `cuenta_contable`, `asiento_contable`, `movimiento_contable`, `cierre_diario` |
| **Servicio** | `nucleo-financiero` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `nucleo_financiero` · `svc_nucleo_financiero` — no lee ningún otro esquema |
| **Documento** | [[03 Fases 3 a 7 · Identidad, habilitación y núcleo de dinero]] |
| **Tamaño** | ●○○○○ · **1 CU**, y aun así es de los carriles más importantes |

**Casos de uso.** CU-24 · Registrar el asiento contable de una operación.

> [!warning] El módulo 03 de la bóveda se reparte entre **dos** servicios de código
> `servicios/nucleo-financiero` (la parte contable, este carril `1B`) y
> `servicios/aportes` (`3A`, tramo T4). La frontera es exacta: **`1B` posee las cuatro
> entidades contables** de la lista de arriba, que viven en `nucleo-financiero`; `3A`
> posee las otras 19 en `aportes`. Sin esa partición, dos carriles de dos olas
> distintas compartirían servicio y el reparto de propiedad de archivos dejaría de
> significar algo.

**Necesita en `dev`.** `T2` completo. **No** necesita billetera: el asiento es anterior
y más general que la billetera.

**Entrega, y quién espera.** `registrarAsiento(...)` con partida doble balanceada. Lo
esperan **`2A` billetera, `2B` tarifas, `3A` aportes, `4A` entregas, `4B` garantía y
`5A` ERP** — es decir, todo lo que mueva dinero en el resto del proyecto.

**Gate propio.** Una propiedad, no un ejemplo: **para todo asiento, la suma de debe
menos la suma de haber es exactamente cero**, probada con `fast-check` sobre importes
generados. Más: rechazo probado de `UPDATE` sobre `asiento_contable` (append-only).

**Dónde se rompe.** Si `registrarAsiento` acepta un asiento desbalanceado «por ahora».
Cinco carriles lo consumen; el descuadre aparece meses después en un cierre diario y
no hay forma de saber cuál de los cinco lo introdujo.

---

### `1C` · Habilitación y límites

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P4** · Dell A · **T2** · fase 4 |
| **Módulo** | `12_cumplimiento_asfi` (parcial) + límites de `10_billetera_custodia` |
| **Servicio** | `nucleo-financiero` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `nucleo_financiero` · `svc_nucleo_financiero` — no lee ningún otro esquema |
| **Documento** | [[03 Fases 3 a 7 · Identidad, habilitación y núcleo de dinero]] |
| **Tamaño** | ●●○○○ · 5 CU |

**Casos de uso.**

| CU | Nombre |
| :-: | --- |
| 02 | Elevar nivel de debida diligencia |
| 03 | Declaración PEP y beneficiario final |
| 06 | Revisión periódica de conocimiento del cliente |
| 40 | Evaluar límites antes de una operación |
| 46 | Verificar el alcance de la licencia |

**Necesita en `dev`.** `T2` completo · contratos de `1A` (mismo tramo: se programa
contra el contrato, no contra la implementación).

**Entrega, y quién espera.** `evaluarLimites(...)` y `verificarAlcanceLicencia(...)` —
los esperan **`2A` billetera** (T3, toda operación pasa por ahí) y **`3C`
cumplimiento** (T5).

**Gate propio.** CU-46 con la licencia en `EN_TRAMITE`: **toda operación fuera del
alcance vigente se rechaza**, y el rechazo cita la resolución. Es la prueba que evita
que el producto opere algo que todavía no tiene autorizado.

**Dónde se rompe.** Cableando un umbral. Todo número de este carril sale del catálogo
sembrado, con vigencia y cita normativa (`semillas-catalogos`). Un `if (monto > 10000)`
en el código es un defecto de cumplimiento, no de estilo.

---

### `1D` · Notificaciones

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P5** · Dell B · **T2** · fase 12 |
| **Módulo** | `05_notificaciones` — 15 entidades |
| **Servicio** | `notificaciones` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `notificaciones` · `svc_notificaciones` — no lee ningún otro esquema |
| **Documento** | [[05 Fases 12 a 16 · Plataforma, reputación y cumplimiento]] |
| **Tamaño** | ●●○○○ · 4 CU |

**Casos de uso.** CU-80 despachar una notificación · CU-81 programar recordatorios de
aporte · CU-82 procesar una respuesta entrante · CU-83 enrutar el envío por proveedor
de mensajería.

**Necesita en `dev`.** `T2` completo — en particular **el outbox funcionando**. Nada
más: este carril **solo consume eventos**.

**Entrega, y quién espera.** El despacho real de avisos. Lo esperan **todos los
carriles de T3 en adelante**, y por eso va tan temprano: terminarlo en la Ola 1
**elimina los *stubs* de aviso de los diez carriles siguientes**.

**Gate propio.** Consentimiento y supresión probados: un usuario que optó por no
recibir **no recibe**, salvo los avisos que la norma obliga. Y el tope de mensajes por
ventana, probado.

**Dónde se rompe.** Notificando dentro de la transacción. El aviso se **encola** en el
outbox y se despacha después del `COMMIT`. Un envío sincrónico ata la transacción de
dinero a la disponibilidad de un proveedor de SMS.

---

## Ola 2 · T3 y T4 — el núcleo del dinero

### `2A` · Billetera y custodia

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P2** · Ubuntu · **T3** · fase 6 |
| **Módulo** | `10_billetera_custodia` — 26 entidades |
| **Servicio** | `nucleo-financiero` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `nucleo_financiero` · `svc_nucleo_financiero` — no lee ningún otro esquema |
| **Documento** | [[03 Fases 3 a 7 · Identidad, habilitación y núcleo de dinero]] |
| **Tamaño** | ●●●●○ · 10 CU · **el carril con más fronteras transaccionales del proyecto** |
| **Atención** | **primer plano** en T3 |

**Casos de uso.**

| CU | Nombre | Lo delicado |
| :-: | --- | --- |
| 10 | Recargar saldo | Idempotencia con la pasarela |
| 11 | Retirar saldo | Sale dinero: doble control |
| 12 | Transferir saldo entre billeteras | Dos billeteras, un asiento |
| 13 | Retener y liberar saldo | La retención no es un débito |
| 14 | Reversar una transacción | **Por reverso, nunca por `UPDATE`** |
| 15 | Emitir extracto y certificado de saldo | Lectura sobre réplica |
| 16 | Cerrar billetera y devolver saldo | No cierra con saldo retenido |
| 17 | Bloquear saldo por orden de autoridad | Prevalece sobre todo lo demás |
| 50 | Conciliar la custodia y verificar el encaje | El descuadre abre evento de riesgo |
| 57 | Operar un punto de atención y arquear el efectivo | Un arqueo por punto y fecha |

**Necesita en `dev`.** `T2` · **`1B` `registrarAsiento`** (bloqueante de verdad) ·
`1C` límites y licencia · contratos de `1A`.

**Entrega, y quién espera.** Contratos CU-10…17, 50, 57 → **`F4` móvil billetera** (P1,
T5) y **`F7` backoffice operación** (P3, T6). Y el libro de billetera, que esperan
`3A`, `4A`, `4B` y `5A`.

**Gate propio.**

- **El saldo nunca se escribe.** Se deriva del libro. Prueba: reconstruir el saldo
  desde los movimientos y comparar, sobre un caso con reverso y retención.
- **Custodia y encaje cuadran** después de una secuencia de 500 operaciones generadas.
- Doble envío de cada operación con la misma clave de idempotencia devuelve **la misma
  respuesta**, no dos movimientos.
- Concurrencia: dos retiros simultáneos sobre el mismo saldo no dejan saldo negativo.

**Dónde se rompe.** En la retención. `retencion_saldo` no es un débito: es saldo que
sigue siendo del titular pero no está disponible. Tratarla como débito descuadra la
custodia contra el encaje, y el descuadre aparece recién en CU-50, lejos de la causa.

---

### `2B` · Tarifas, comisiones y facturación

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P5** · Dell B · **T3** · fase 7 |
| **Módulo** | `11_tarifas_comisiones` — 27 entidades |
| **Servicio** | `tarifas` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `tarifas` · `svc_tarifas` — no lee ningún otro esquema |
| **Documento** | [[03 Fases 3 a 7 · Identidad, habilitación y núcleo de dinero]] |
| **Tamaño** | ●●●○○ · 7 CU |

**Casos de uso.** CU-30 cotizar la comisión antes de operar · CU-31 devengar y cobrar
la comisión · CU-32 emitir factura electrónica · CU-33 devolver comisión y emitir nota
de crédito · CU-34 publicar un tarifario nuevo con preaviso · CU-35 cerrar la
liquidación mensual de ingresos · CU-36 segmentar comercialmente y aplicar precio
diferenciado.

**Necesita en `dev`.** `T2` · **`1B` `registrarAsiento`** · contratos de `2A` (mismo
tramo: se programa contra el contrato).

**Entrega, y quién espera.**

| Entrega | Lo espera |
| --- | --- |
| **Cotización congelada** | `2C` (CU-20 congela el tarifario al crear el grupo) |
| Devengo, cargo y factura | `3A`, `4A`, `5A` ERP, `5B` publicidad |
| Contrato CU-34 (tarifario público) | **`F9` sitio público** — es obligación ASFI de transparencia |

**Gate propio.** **CU-31 de punta a punta es el hito de validación del stack**
([[informe]]): cotizar → devengar → cobrar → asentar → facturar, en una transacción,
con el importe exacto en `DECIMAL(14,2)` y sin un solo `number` en el camino.

**Gate que suma D-19** ([[20 Maqueta de referencia · deltas del frontend]]). El descuento
de comisión por nivel de reputación **es un concepto de tarifa con su regla**, no un
ajuste que calcula la app:

- Se expresa como fila de `regla_tarifa` sobre `COM_ENTREGA`, con el nivel como condición
  y su vigencia. Cambiar el porcentaje es un seeder, no un despliegue.
- **La cotización devuelve el desglose**: bruto, descuento aplicado con su motivo, y neto.
  `F4` pinta esas líneas; no las deriva.
- **El piso y el techo siguen mandando.** Sobre una bolsa chica la comisión toca el piso
  de Bs 10 y el descuento vale poco: eso es correcto y se muestra tal cual. Redondear para
  que el beneficio luzca mejor es falsear el tarifario.
- **Un descuento nunca se convierte en acreditación.** Se cobra menos; no se emite saldo.
  Emitir saldo obliga a respaldarlo en `cuenta_custodia` (ver D-19), y ahí deja de ser una
  promoción comercial para pasar a ser emisión de dinero electrónico.

**Dónde se rompe.** Al recotizar. El precio se **congela** al cotizar y se cobra el
congelado, aunque el tarifario haya cambiado entre medio. Recotizar al cobrar es cómo
un usuario paga una comisión distinta de la que aceptó — y CU-34 exige preaviso
justamente para que eso no pase.

---

### `2C` · Grupos y turnos

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P1** · Mac M5 · **T3** · fase 8 |
| **Módulo** | `02_grupos_turnos` — 22 entidades |
| **Servicio** | `grupos` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `grupos` · `svc_grupos` — no lee ningún otro esquema |
| **Documento** | [[04 Fases 8 a 11 · Circuito del pasanaku]] |
| **Tamaño** | ●●●●○ · 11 CU · **es el pasanaku** |
| **Atención** | **primer plano** en T3 |

**Casos de uso.**

| CU | Nombre | Lo delicado |
| :-: | --- | --- |
| 20 | Crear grupo y congelar tarifario | Congela contra `2B` |
| 59 | Mantener el calendario de días no hábiles | **Base de todos los plazos** |
| 60 | Sortear los turnos | Semilla verificable, no `random()` |
| 62 | Permutar turnos entre participantes | Acuerdo de ambas partes |
| 63 | Proponer y votar un acuerdo | Quórum y voto nominal |
| 64 | Traspasar un cupo | Cambia quién debe |
| 65 | Retirarse de un grupo | Deja un cupo caído |
| 68 | Postular a un grupo y ser emparejado | Pesos versionados |
| 69 | Invitar a un contacto y registrar sus referencias | Token de un solo uso |

**Necesita en `dev`.** `T2` · contratos de `1A` y de `2B` (mismo tramo).

**Entrega, y quién espera.** El grupo, el turno y el calendario. Los esperan **`3A`
aportes, `3B` transparencia, `4A` entregas, `4B` garantía**, y en frontend **`F5`
móvil pasanaku** (P1, T6).

**Gate propio.** CU-60: el sorteo se **reproduce** desde su semilla guardada y da el
mismo resultado. Es lo que después verifica públicamente CU-61 sin sesión, y si no es
reproducible, la transparencia del producto es decorativa.

**Gate que suman los deltas nuevos.** Estos tres **no son de frontend**: cambian la
transacción y el contrato, y llegar tarde a ellos obliga a rehacer `F5`.

| Delta | Qué cambia acá |
| :-: | --- |
| **D-15** | El canje de la invitación (CU-69) **consume el token pero no ocupa cupo**: crea `solicitud_ingreso` en `PENDIENTE`. El cupo lo ocupa la resolución, y `ck_solicitud_ingreso_resuelta` exige `revisada_por` y `fecha_resolucion`. **Prueba obligatoria:** un `UPDATE` que cierre la solicitud sin esos dos campos tiene que ser rechazado **por la base**. Endpoints nuevos: `POST /grupos/{cod}/solicitudes-ingreso` y `POST …/{id}/resolucion`, los dos idempotentes |
| **D-20** | La oferta de permuta (CU-62) expone **los once estados**, y la aceptación devuelve el **veredicto de riesgo con sus factores y umbrales**, no solo un booleano. `EN_VALIDACION` ocurre **después** de `ACEPTADA`; el dinero se mueve solo en `EJECUTADA`. Tope de compensación y tope de permutas por ciclo son **dato de catálogo**, no constantes |
| **D-22** | `GET /aportes/obligaciones` devuelve **el ciclo completo** con las futuras marcadas. Derivarlo en el cliente sería recalcular lo que el período ya fijó al abrirse |

**Dónde se rompe.** En el calendario. CU-59 alimenta cada plazo del sistema, y los
plazos **se guardan al inicio y no se recalculan nunca** (`plazos-habiles`). Un feriado
cargado tarde no debe mover un vencimiento ya calculado — y si lo mueve, tiene que ser
solo a favor del cliente.

---

### `2D` · Auditoría y reportes

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P4** · Dell A · **T3** · fase 15 |
| **Módulo** | `09_auditoria_reportes` — 19 entidades |
| **Servicio** | `auditoria` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `auditoria` · `svc_auditoria` — no lee ningún otro esquema |
| **Documento** | [[05 Fases 12 a 16 · Plataforma, reputación y cumplimiento]] |
| **Tamaño** | ●●○○○ · 5 CU |

**Casos de uso.** CU-07 ejercer derechos sobre datos personales · CU-54 registrar un
evento de riesgo operativo · CU-55 gestionar un incidente de seguridad · CU-58 definir,
programar y exportar un reporte · CU-98 publicar el tablero de indicadores.

**Necesita en `dev`.** `T2` (bitácora y trazas) · contratos de `1A`.

**Entrega, y quién espera.** El motor de reportes con **ejecución bajo la sesión del
solicitante y RLS vigente**, hash del resultado y exportación que caduca. Lo esperan
**`3C` cumplimiento** (T5) y **`F8` backoffice cumplimiento** (P4, T6).

**Gate propio.** Un reporte ejecutado por un usuario **no devuelve filas que su RLS le
oculta**. Es la prueba de que la extracción no es una puerta trasera alrededor de las
políticas de fila.

**Dónde se rompe.** Exportando sin vencimiento ni tope de descargas. Un CSV con datos
personales y una URL eterna es exactamente el hallazgo que un supervisor busca, y el
control ya está especificado en `extraccion-de-datos`.

---

### `2E` · Organizador y automatización

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P5** · Dell B · **T4** · fase 14 *(deuda declarada de T3)* |
| **Módulo** | `07_organizador_automatizacion` — 12 entidades |
| **Servicio** | `organizador` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `organizador` · `svc_organizador` — no lee ningún otro esquema |
| **Documento** | [[05 Fases 12 a 16 · Plataforma, reputación y cumplimiento]] |
| **Tamaño** | ●●●○○ · 6 CU |

**Casos de uso.** CU-90 postular a organizador y habilitarse · CU-91 firmar y rescindir
el contrato de organizador · CU-92 evaluar el desempeño · CU-93 sancionar y resolver su
apelación · CU-95 definir una regla de automatización · CU-96 programar y ejecutar una
tarea automatizada.

**Necesita en `dev`.** `T2` (planificador) · `1A` identidad · `1D` notificaciones ·
contratos de `2C` grupos.

**Entrega, y quién espera.** El **motor de reglas** (expresión compilada, umbral que
apunta al catálogo, catálogo cerrado de acciones) y el ejecutor de tareas. Los esperan
`3C` cumplimiento (reglas de monitoreo) y `F8`.

**Gate propio.** Dos, y ninguno es negociable:

- **Simulación obligatoria antes de activar** una regla: una regla nueva no puede
  producir efectos sin haber corrido contra datos históricos primero.
- **Exactamente una vez entre réplicas**: la misma tarea programada, con dos workers
  levantados, se ejecuta una sola vez (`SKIP LOCKED`).

**Gate que suma D-16** ([[20 Maqueta de referencia · deltas del frontend]]). La
habilitación deja de ser un veredicto y pasa a ser una lista que se puede mostrar:

- **El contrato devuelve cada requisito evaluado con su código, su umbral y el valor del
  usuario**, no solo `aprobada: true`. `F5` pinta cumplidos y faltantes con esos datos, y
  si el contrato manda solo el veredicto, la app tendría que cablear los umbrales —que es
  exactamente lo que `semillas-catalogos` prohíbe.
- **El puntaje se congela al postular** (`puntaje_reputacion_al_solicitar`) y la
  evaluación usa los requisitos **vigentes a esa fecha**, no los de hoy. Prueba: cambiar
  un umbral entre la postulación y la resolución no cambia el resultado.
- **El rechazo lleva motivo y fecha desde la que se puede volver a postular.** Un rechazo
  sin camino de vuelta es una expulsión encubierta.
- **La suspensión por capacitación vencida no toca los grupos vigentes.** Prueba: un
  organizador suspendido no puede crear grupos y **sigue administrando** los que tiene.

**Dónde se rompe.** Permitiendo expresiones arbitrarias en las reglas. El motor evalúa
expresiones **compiladas contra un catálogo cerrado**; si acepta código, una regla de
negocio se convierte en ejecución remota.

---

## Ola 3 · T4 y T5

### `3A` · Aportes y pagos con QR

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P2** · Ubuntu · **T4** · fase 9 |
| **Módulo** | `03_aportes_pagos_qr` — 19 entidades *(las 4 contables son de `1B`)* |
| **Servicio** | `aportes` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `aportes` · `svc_aportes` — no lee ningún otro esquema |
| **Documento** | [[04 Fases 8 a 11 · Circuito del pasanaku]] |
| **Tamaño** | ●●○○○ · 4 CU, pero de altísima criticidad |
| **Atención** | **primer plano** en T4 |

**Casos de uso.** CU-19 reembolsar un pago y atender una disputa · CU-21 **cobrar el
aporte del período** · CU-51 ejecutar el cierre diario · CU-99 dar de alta un proveedor
de pago y enrutar el cobro.

**Necesita en `dev`.** `1B` asiento · `2A` billetera · `2B` tarifas · `2C` grupos. **Es
el carril con más dependencias del proyecto**, y por eso va en T4 y no antes.

**Entrega, y quién espera.** El cobro real del pasanaku. Lo esperan `4A` entregas, `4B`
garantía y **`F5` móvil pasanaku**.

**Gate propio.**

- **Webhook duplicado y fuera de orden**: la pasarela avisa dos veces y en desorden; el
  resultado tiene que ser el mismo pago, una sola vez.
- **Conciliación**: un pago que aparece en el extracto bancario y no en el sistema abre
  una excepción de conciliación, no se ignora.
- El cierre diario (CU-51) corre **exactamente una vez** aunque haya dos réplicas.

**Dónde se rompe.** Confiando en el orden de los webhooks. Llegan duplicados, fuera de
orden y tarde. Todo el diseño de `idempotencia-reintentos` existe para este carril.

---

### `3B` · Transparencia y reputación

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P3** · Legion · **T4** · fase 13 |
| **Módulo** | `06_transparencia_reputacion` — 16 entidades |
| **Servicio** | `transparencia` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `transparencia` · `svc_transparencia` — no lee ningún otro esquema |
| **Documento** | [[05 Fases 12 a 16 · Plataforma, reputación y cumplimiento]] |
| **Tamaño** | ●●●○○ · 9 CU |

**Casos de uso.** CU-61 verificar públicamente el sorteo · CU-70 registrar un evento de
reputación · CU-71 recalcular el puntaje · CU-72 sellar el bloque de transparencia ·
CU-73 verificar la cadena de transparencia · CU-74 otorgar y revocar una insignia ·
CU-75 emitir un certificado de reputación verificable · CU-76 reseñar a un participante
y moderar la reseña · CU-97 anticipar el riesgo con alertas tempranas.

**Necesita en `dev`.** `2C` grupos (sorteo y turnos) · contratos de `3A`.

**Entrega, y quién espera.** **Las cuatro rutas públicas sin sesión** — CU-61, CU-72,
CU-73, CU-75 —, que son la razón por la que existe `apps/web`. Las espera **`F9` sitio
público** (P5, T5).

**Gate propio.** La cadena de transparencia se **verifica desde afuera**: un tercero,
sin sesión, con solo el código público, comprueba que el bloque no fue alterado. Si eso
no pasa, CU-73 es una promesa y no un mecanismo.

**Dónde se rompe.** Indexando. `/verificar/*` y `/publico/*` llevan datos de personas:
van `noindex, nofollow` (invariante 9 del [[10 Plan maestro del frontend]]). Este carril
produce las rutas; `F10` SEO tiene que **no** indexarlas. Es un punto de contacto entre
dos carriles de puestos distintos: se escribe en la ficha de los dos.

---

### `3C` · Cumplimiento ASFI y UIF

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P4** · Dell A · **T6** · fase 16 *(deuda declarada de T4)* — *cascada de la decisión del 2026-08-18: la fase 18 se adelantó a T5* |
| **Módulo** | `12_cumplimiento_asfi` — **47 entidades, el módulo más grande de la bóveda** |
| **Servicio** | `cumplimiento` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `cumplimiento` · `svc_cumplimiento` — no lee ningún otro esquema |
| **Documento** | [[05 Fases 12 a 16 · Plataforma, reputación y cumplimiento]] |
| **Tamaño** | ●●●●● · 14 CU |

**Casos de uso.** CU-41 umbral y formulario PCC-01 · CU-42 umbral y ROG · CU-43 remitir
reportes mensuales a la UIF · CU-44 de alerta de monitoreo a ROS · CU-45 atender un
requerimiento de autoridad · CU-47 evaluar el riesgo del producto antes de lanzarlo ·
CU-48 calibrar reglas y triar alertas · CU-49 designar al oficial de cumplimiento y
capacitar · CU-52 atender un reclamo en plazo · CU-53 elevar un reclamo a segunda
instancia · CU-56 ejecutar una prueba de continuidad · CU-94 elevar una decisión al
comité de gobierno.

**Necesita en `dev`.** `1A` · `1C` habilitación · `2A` billetera (los umbrales miran
operaciones) · `2D` auditoría · `2E` motor de reglas.

**Entrega, y quién espera.** Todo el circuito regulatorio. Lo espera **`F8` backoffice
cumplimiento** (P4, T6) — **mismo puesto**, lo cual es deliberado: el que implementó la
regla es el que dibuja su pantalla.

**Gate propio.**

- **Ventana acumulada** de PCC-01 y ROG: tres operaciones que individualmente no llegan
  al umbral pero juntas sí, dentro de la ventana, **disparan**.
- **Plazos guardados, no recalculados**: el vencimiento de un reclamo se calcula al
  abrirlo y se guarda. Recalcularlo al consultar es cómo un plazo legal se mueve solo.
- **Debido proceso completo** en toda decisión que perjudique: causal escrita,
  notificación probada, descargo, decisión motivada, apelación resuelta **por otro**.

**Dónde se rompe.** Por tamaño. 14 CU y 47 entidades no entran en una sentada: se parte
en bloques —umbrales, reportes, reclamos, gobierno— y **cada bloque cierra con sus
pruebas** antes de abrir el siguiente. Un carril de este tamaño abierto entero es un
carril que no cierra nunca.

---

### `3D` · Cuenta bancaria de destino

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P4** · Dell A · **T4** · fase 10a |
| **Módulo** | `04_entregas_fondo` (parcial) — 10 entidades |
| **Servicio** | `entregas` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `entregas` · `svc_entregas` — no lee ningún otro esquema |
| **Documento** | [[04 Fases 8 a 11 · Circuito del pasanaku]] |
| **Tamaño** | ●○○○○ · 1 CU |

**Casos de uso.** CU-18 · Registrar y verificar una cuenta bancaria de destino.

**Necesita en `dev`.** `1A` identidad · `2A` billetera.

**Entrega, y quién espera.** La cuenta bancaria verificada y **cifrada**. La espera
**`4A` entrega del fondo** (P2, T5) — sin esto no hay a dónde mandar la plata.

**Gate propio.** La cuenta se guarda cifrada y **no se muestra completa** en ninguna
pantalla ni en ningún log. Y la verificación es real: una cuenta no verificada no puede
recibir un desembolso.

**Dónde se rompe.** Es el carril más chico del proyecto y por eso el candidato a que se
le baje la guardia. Es también el único punto donde el sistema le entrega dinero a un
número de cuenta que escribió un usuario.

---

## Ola 4 · T5

### `4A` · Entrega del fondo

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P2** · Ubuntu · **T5** · fase 10b |
| **Módulo** | `04_entregas_fondo` — 10 entidades |
| **Servicio** | `entregas` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `entregas` · `svc_entregas` — no lee ningún otro esquema |
| **Documento** | [[04 Fases 8 a 11 · Circuito del pasanaku]] |
| **Tamaño** | ●○○○○ · 2 CU · **cierra el circuito del pasanaku** |

**Casos de uso.** CU-22 liquidar y entregar el fondo · CU-28 emitir la orden de
desembolso y ejecutar el intento.

**Necesita en `dev`.** `3D` (CU-18) · `2A` billetera · `2C` grupos · `3A` aportes ·
`1B` asiento.

**Entrega, y quién espera.** La salida de dinero. La espera `F5` móvil pasanaku y el
hito **«el pasanaku funciona»**: al cerrar T5 tiene que pasar
`PasanakuCompletoE2ETest.java`.

**Gate propio.** Orden de desembolso **idempotente**: reintentar el mismo desembolso no
manda la plata dos veces. Cada intento se clasifica por tipo de error, y el historial de
estados queda completo para cuando el beneficiario diga que no le llegó.

**Dónde se rompe.** Reintentando sin clave. Un desembolso duplicado no se corrige con
un `UPDATE`: se corrige con un reverso, un asiento y una conversación incómoda.

---

### `4B` · Garantía e incumplimiento

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P3** · Legion · **T5** · fase 11 |
| **Módulo** | `08_garantia_incumplimiento` — **33 entidades** |
| **Servicio** | `garantia` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `garantia` · `svc_garantia` — no lee ningún otro esquema |
| **Documento** | [[04 Fases 8 a 11 · Circuito del pasanaku]] |
| **Tamaño** | ●●●○○ · 7 CU |

**Casos de uso.** CU-23 cubrir un incumplimiento con el fondo · CU-25 declarar el
incumplimiento con descargo y evidencia · CU-26 ejecutar el aval y subrogar la deuda ·
CU-27 restringir al deudor e incluirlo en la lista interna · CU-29 devolver los aportes
del fondo de garantía · CU-66 reemplazar a un participante moroso · CU-67 disolver el
grupo anticipadamente.

**Necesita en `dev`.** `2C` grupos · `3A` aportes · `2A` billetera · `1B` asiento ·
calendario de CU-59.

**Entrega, y quién espera.** El manejo del incumplimiento. Lo espera `F5` móvil y `F7`
backoffice operación.

**Gate propio.** **Debido proceso, probado paso por paso**: causal escrita, notificación
con prueba de entrega, plazo **guardado al inicio**, descargo con evidencia, decisión
motivada, apelación única resuelta por otra persona, prescripción, y reversión con
compensación si la apelación prospera. Cada uno de esos pasos es una prueba.

**Gate que suma D-17** ([[20 Maqueta de referencia · deltas del frontend]]). Todo este
aparato tenía superficie de operador y **ninguna de cliente**: el que debe veía un recargo
creciendo y nada más. El carril expone lo que la app necesita para mostrarlo:

- **Aviso anticipado de dificultad** como operación propia (`POST /aportes/obligaciones/
  avisos-de-dificultad`). No condona nada —el importe no cambia— pero **mueve la etapa de
  cobranza** y frena los recordatorios automáticos hasta que se resuelva. Prueba: tras el
  aviso, el motor de notificaciones respeta la etapa nueva y su tope de contactos.
- **La escalera se consulta, no se cablea.** Las seis etapas con sus canales, frecuencia y
  `max_contactos_por_semana` salen de `estrategia_cobranza`. La app las pinta.
- **Los dos plazos de 5 días hábiles viajan guardados**, con su fecha de vencimiento ya
  calculada. La app no suma días hábiles: no tiene el calendario de feriados y no debe
  tenerlo.
- **El expediente es legible por su titular**, no solo por el operador. Con las mismas
  piezas de evidencia y su hash.

**Dónde se rompe.** Sancionando antes de notificar, o dejando que quien decide sea
quien resuelve la apelación. Es el carril donde el sistema le saca algo a una persona:
todo lo que falte en el debido proceso vuelve como reclamo con la razón del otro lado.

---

## Ola 5 y los carriles nuevos

### `5T` · Convergencia y despliegue

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P2** · Ubuntu · **T6 → T8** · fase 17 |
| **Documento** | [[06 Fase 17 · Endurecimiento, E2E y despliegue]] |
| **Tamaño** | ●●●●○ · sin CU nuevos: mide el sistema entero |
| **Atención** | **primer plano** en T6 |

**Necesita en `dev`.** Los 87 CU del núcleo implementados (cierre de T5).

**Alcance.** E2E completo, rendimiento con medición, resiliencia (timeouts, reintentos
con jitter, circuit breaker, backpressure), **ensayo de restauración**, endurecimiento
de seguridad y despliegue real.

**Gate propio.** El de `definicion-de-terminado`, sin gate crítico en rojo. Más:
**RPO y RTO medidos en un ensayo de restauración real**, no declarados.

**Dónde se rompe.** Midiendo en la máquina equivocada. Este carril vive en Ubuntu
**porque Docker es nativo**: los números de latencia y throughput sacados bajo la VM de
macOS o bajo WSL2 no son los del sistema, son los de la capa de virtualización.

---

### `5A` · Contabilidad ERP · **carril nuevo**

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P4** · Dell A · **T5** · fase 18 — *adelantado por condición de licencia, decisión del 2026-08-18* |
| **Módulo** | `13_contabilidad_erp` — 18 entidades |
| **Servicio** | `erp` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `erp` · `svc_erp` — no lee ningún otro esquema |
| **Documento** | El servicio `servicios/erp/` ya está andamiado; el documento de fase está pendiente de redactar (§11 de [[17 Plan de acción secuencial · coordinación de cinco máquinas]]) |
| **Tamaño** | ●●●○○ · 7 CU |

**Casos de uso.** CU-100 abrir y cerrar el período contable · CU-101 presupuestar por
centro de costo · CU-102 dar de alta un tercero comercial y su orden de compra · CU-103
registrar y pagar una factura de proveedor · CU-104 cobrar una cuenta por cobrar ·
CU-105 depreciar un activo fijo · CU-106 generar el estado financiero del período.

**Necesita en `dev`.** `1B` asiento (es la base de todo este módulo) · `2B` facturación
· `4A` desembolsos.

**Entrega, y quién espera.** Los libros contables formales. Los espera **`F13`
backoffice contabilidad** (P3, T9) — mismo puesto.

**Gate propio.** **Un período cerrado no admite asientos retroactivos**, probado con
rechazo de restricción, no con validación en el servicio. Y el estado financiero se
**reproduce**: generarlo dos veces sobre los mismos datos da byte por byte lo mismo.

> [!warning] Decisión de cumplimiento abierta
> CU-100 cita **Ley 393 (libros y conservación)** y el Código de Comercio. Si llevar
> libros formales es **condición de la licencia** y no una obligación que empieza con
> la operación, **este carril se adelanta a T5** y desplaza a `3C`. Es una decisión de
> cumplimiento, no técnica, y hay que tomarla antes de T5.

**Dónde se rompe.** Duplicando el asiento. `1B` ya sabe registrar asientos; este módulo
**usa** `registrarAsiento`, no escribe el suyo. Dos motores de asiento en el mismo
sistema es cómo el balance del ERP deja de coincidir con el libro de la billetera.

---

### `5B` · Publicidad y campañas · **carril nuevo**

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P4** · Dell A · **T9** · fase 19 — *cascada de la decisión del 2026-08-18* |
| **Módulo** | `14_publicidad_campanas` — 14 entidades |
| **Servicio** | `publicidad` — un desplegable, propiedad exclusiva del carril |
| **Esquema · rol** | `publicidad` · `svc_publicidad` — no lee ningún otro esquema |
| **Documento** | El servicio `servicios/publicidad/` ya está andamiado; el documento de fase está pendiente de redactar (§11 de [[17 Plan de acción secuencial · coordinación de cinco máquinas]]) |
| **Tamaño** | ●●○○○ · 5 CU |

**Casos de uso.** CU-110 dar de alta un anunciante y su cuenta publicitaria · CU-111
crear y aprobar una campaña · CU-112 moderar una pieza creativa · CU-113 entregar un
anuncio y medir su desempeño · CU-114 liquidar y facturar el gasto publicitario.

**Necesita en `dev`.** `1A` identidad · `2B` tarifas y facturación · `1B` asiento.

**Entrega, y quién espera.** La plataforma de anuncios. La espera **`F14` backoffice
publicidad** (P4, T9) — mismo puesto.

**Gate propio.** La liquidación de CU-114 **cuadra contra los eventos de entrega** de
CU-113: no se factura una impresión que no se registró. Y la moderación de CU-112 es
previa a la entrega, no posterior.

**Gate que suma D-21** ([[20 Maqueta de referencia · deltas del frontend]]). El vale es lo
que este carril le presta al producto núcleo, y trae cuatro reglas propias:

| Regla | Por qué |
| --- | --- |
| **El descuento lo asume el comercio en su margen** | No toca `cuenta_custodia`, y por eso puede ser mucho más grande que cualquier bono que la plataforma pudiera pagar (ver D-19). Un vale **nunca** se convierte en acreditación de saldo |
| **El beneficio se congela en el canje** | Si la campaña baja del 8 % al 6 %, el vale ya usado valió lo de ese día — misma regla que el tarifario congelado por grupo |
| **El presupuesto corta la emisión, nunca el canje** | Un vale en manos de alguien es una obligación asumida |
| **Protección contra doble canje** | Token firmado, QR rotativo e idempotencia. Es la garantía que hace que un comercio acepte poner el descuento; sin ella, no hay alianza |

Y RN-18 sigue en pie: la comisión sobre una venta atribuida la cobra **la plataforma**,
nunca el organizador del grupo.

**Dónde se rompe.** Mezclando el dinero del anunciante con el de los participantes. La
cuenta publicitaria es un tercero comercial, no una billetera de pasanaku: si comparten
libro, la custodia y el encaje dejan de cuadrar y el problema aparece en CU-50.

---

# Parte B · Los 20 carriles de frontend

> **Stack: Flutter y Angular** ([[ADR-044 Frontend en Angular y Flutter]]). Las fichas
> de esta parte se reescribieron el 2026-09-09. Los carriles de F0 y F1 se **rehacen** en
> el tramo `TF` ([[17 Plan de acción secuencial · coordinación de cinco máquinas]] §5);
> sus informes anteriores quedan como historia y reciben una sección «Rehecho».

> **Los carriles de frontend no necesitan el backend corriendo.** Trabajan contra
> **Prism**, con ejemplos derivados del contrato OpenAPI ([[16 Carriles de frontend]] §8).
> Lo que sí necesitan es que **el contrato exista**: si no está escrito, el carril **no lo
> inventa** — lo pide al carril de backend que lo posee y trabaja en otra pantalla.
>
> **Los seis gates comunes** se dan por incluidos en cada ficha y no se repiten: los
> cuatro estados en toda pantalla con datos, vía `EstadoDePantalla` · cero literales de
> diseño fuera de `packages/tokens` · ningún importe formateado fuera del átomo `Monto` ·
> doble envío bloqueado con la misma clave en toda operación de dinero · un solo botón
> naranja por pantalla · accesibilidad AA bloqueante (teclado, foco, contraste, semántica
> en web; `meetsGuideline` en móvil).
>
> **Y un séptimo, para todo carril de pantallas:** cada pantalla se compara contra la suya
> en [[AportaYa-Maqueta]] en los dos escenarios, con la ruta, los organismos y el delta que
> le asigna [[22 Mapa de la maqueta · pantalla, carril y mundo]]; el golden o la captura va
> al lado de la captura de la maqueta en el PR.

## Tramo TF · Troncal, tres andamiajes y dos sistemas de diseño

> **Delta 2, corregido por ADR-044.** La fase F0 tiene ahora **un paso troncal** que
> bloquea a **tres andamiajes concurrentes**, y F1 se parte en **dos carriles** que corren
> a la vez porque son dos bases de código sin un archivo en común.

### `F0.T` · Troncal del frontend

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P1** · Mac M5 · **TF.0** · fase F0 |
| **Documento** | [[11 Fases F0 y F1 · Cimientos y sistema de diseño]] §F0.T |
| **Posee** | `packages/tokens/**` · `packages/simulado/**` · `buildSrc/…/aportaya.openapi.gradle.kts` (las tareas `generarClienteAngular` y `generarClienteDart`) · `package.json` y `turbo.json` de raíz · el job `frontend` de `.github/workflows/ci.yml` |
| **Tamaño** | ●●○○○ |
| **Atención** | **primer plano** en TF.0 |

**Necesita en `dev`.** `T0` cerrado · ADR-044 indexado · los contratos OpenAPI que ya existen.

**Entrega, y quién espera.** `tokens.json` con sus dos generados (`tokens.css`,
`tokens.dart`) y la prueba contra la bóveda; `vectores/monto.json`; los dos clientes
generados desde el mismo Gradle; Prism con un archivo de ejemplos por CU; las tareas de
raíz y el CI. **Lo esperan los tres andamiajes**, y por eso va primero y solo.

**Gate propio.** `yarn workspace @aportaya/tokens build` produce los dos generados **con
las mismas claves** · `./gradlew generateOpenApiClients` produce `clientes/angular` y
`clientes/dart` y **los importes son `string` en los dos** · `yarn dev:mock` responde los
tres escenarios por cabecera `Prefer` y **rechaza** un ejemplo que no valide contra el
esquema · el CI corre el job `frontend` en verde sobre un monorepo sin apps todavía.

**Dónde se rompe.** Dejando `typescript-fetch` «por si acaso». Dos clientes TypeScript
son dos fuentes de tipos, y alguien va a importar del equivocado. Se borra
`clientes/typescript/` en este mismo carril.

---

### `F0-M` · Andamiaje móvil (Flutter)

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P3** · Legion · **TF.1** · fase F0 |
| **Documento** | [[11 Fases F0 y F1 · Cimientos y sistema de diseño]] · skill `movil-flutter` |
| **Posee** | `apps/movil/**` (Flutter) |
| **Tamaño** | ●●○○○ |

**Necesita en `dev`.** `F0.T` cerrado.

**Entrega, y quién espera.** Flutter con `fvm`, `go_router` con **un enchufe por
dominio** en `navegacion/rutas.dart` (seis listas vacías: identidad, billetera, alianzas,
pasanaku, soporte, notificaciones), Riverpod, `dio` sobre `clientes/dart` con los
interceptores, el `package.json` envoltorio para `turbo`, `custom_lint` con las nueve
reglas, y `PantallaDeSaldo` con sus cuatro estados contra Prism. Lo esperan `F1-M` y
todos los carriles de pantalla móviles.

**Gate propio.** `yarn dev:movil` abre en un Android físico contra Prism con **una
pantalla real y sus cuatro estados** · `flutter test` corre unidad, widget, contrato, a11y
y **un golden en claro y oscuro** · y la prueba que importa: **agregar una pantalla vacía
a `pantallas/identidad/` no toca ningún archivo fuera de ese directorio** · `grep -r
"Platform.is" lib/` vacío fuera de `infraestructura/`.

**Dónde se rompe.** Escribiendo un `MaterialApp` con `routes:` a mano «para arrancar
rápido». Ese mapa es el registro central que el enchufe por dominio existe para evitar,
y si nace acá, F2 lo hereda y los tres carriles de pantalla lo editan. Y el segundo modo
de falla: dejar que el generador de Dart mapee los importes a `double`. Se verifica en
la primera regeneración, no en F4.

---

### `F0-B` · Andamiaje backoffice (Angular)

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P4** · Dell A · **TF.1** · fase F0 |
| **Documento** | [[11 Fases F0 y F1 · Cimientos y sistema de diseño]] · skills `web-angular`, `web-backoffice` |
| **Posee** | `apps/backoffice/**` |
| **Tamaño** | ●●○○○ |

**Necesita en `dev`.** `F0.T` cerrado.

**Entrega, y quién espera.** Angular *standalone* y *zoneless*, `app.routes.ts` con **un
enchufe por dominio** (cinco `loadChildren` a archivos de rutas vacíos), `nucleo/` con los
interceptores sobre `clientes/angular`, Vitest + Angular Testing Library + `vitest-axe`,
`angular-eslint` con las nueve reglas, `Dockerfile.backoffice` (NGINX estático), y
`PantallaDeBilletera` con `cuentaId` en la URL y sus seis estados. Lo esperan `F1-W`,
`F6`, `F7`, `F8`, `F13`, `F14`.

**Gate propio.** `yarn dev:backoffice` con la pantalla contra Prism · **`noindex` desde
el primer día** en la meta y en el NGINX de la imagen · el token del operador **no
aparece en `localStorage`** (prueba de Playwright que lo busca) · agregar una ruta vacía a
`rutas/operacion/` **no toca `app.routes.ts`**.

**Dónde se rompe.** Montando el andamiaje con Angular Material «porque ya trae tabla».
El sistema de diseño es propio y la `TablaDeDatos` es del carril `F6` sobre el CDK.
Material metería un segundo sistema de tokens que después nadie saca.

---

### `F0-W` · Andamiaje web (Angular SSR)

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P5** · Dell B · **TF.1** · fase F0 |
| **Documento** | [[11 Fases F0 y F1 · Cimientos y sistema de diseño]] · [[ADR-041 Sitio público · el tercer producto]] · skill `web-angular` |
| **Posee** | `apps/web/**` |
| **Tamaño** | ●●○○○ |

**Necesita en `dev`.** `F0.T` cerrado.

**Entrega, y quién espera.** Angular con `@angular/ssr` y `app.routes.server.ts`
(`Prerender` por omisión), `scripts/contenido.mjs` con su primer Markdown, `robots.txt`
generado con prueba, `Dockerfile.web` (Node tras NGINX, sin root), y `/plazos` en
`RenderMode.Server` con sus cuatro estados contra Prism. Lo esperan `F9`, `F10`, `F11`.

**Gate propio.** `ng build` separa lo prerenderizado de lo servido y **`rutas-de-servidor.spec.ts`
enumera las rutas `Server` y exige que sea exactamente una** · `docker build` corre como
`node` y sirve la portada, `robots.txt` y `/plazos` · el `budget` de `angular.json` está
fijado en **150 KB comprimidos** y el build lo respeta con la app vacía.

**Dónde se rompe.** Poniendo el sitio entero en `RenderMode.Server` «por las dudas».
`Prerender` por omisión es la única ventaja del producto (ADR-041); todo lo demás
estático es lo que hace el sitio rápido e indexable. Y el segundo: olvidar
`withIncrementalHydration()`. Sin eso los verificadores hidratan la página entera y el
presupuesto de JS se rompe en F9.

---

### `F1-W` · Sistema de diseño · Angular

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P4** · Dell A · **TF.2** · fase F1 |
| **Posee** | **`packages/ui/**`** · la ruta `/catalogo` de `apps/web` |
| **Documento** | [[11 Fases F0 y F1 · Cimientos y sistema de diseño]] · skill `disenar-frontend` |
| **Tamaño** | ●●●●○ |
| **Atención** | **primer plano** en TF.2, compartido con `F1-M` |

**Necesita en `dev`.** Los tres andamiajes cerrados · `packages/tokens` congelado.

**Entrega, y quién espera.** La biblioteca Angular `@aportaya/ui` con **una entrada
secundaria por componente**: tokens consumidos por `var(--…)`, átomos, moléculas,
organismos (incluidos `EstadoDePantalla`, `TablaDeDatos` sobre el CDK, `SeccionDeExpediente`)
y el catálogo vivo en `/catalogo`. **La esperan `F6`, `F7`, `F8`, `F9`, `F13`, `F14`**, y
**se congela al cerrar**: desde T3, un átomo nuevo se pide por micro-PR.

**Gate propio.** Tema claro y oscuro completos · `test:a11y` en verde sobre el catálogo
entero · **`Monto` pasa los 5.000 vectores de `packages/tokens/vectores/monto.json`** ·
ningún hex ni `px` fuera de `packages/tokens`, verificado por lint · las 20 pruebas de los
átomos React anteriores, **reescritas como criterio de aceptación**, en verde · **las
piezas Angular de [[22 Mapa de la maqueta · pantalla, carril y mundo]] §6 existen con ese
nombre**, cada una con su caso en `/catalogo` comparado contra la maqueta.

**Dónde se rompe.** Inventando. **Acá no se diseña**: se implementa lo que
`docs/Views/Sistema-Diseno/` ya definió. Y un modo de falla nuevo: **resolver algo
distinto que `F1-M`**. Un `ChipEstado` con cinco variantes en Angular y cuatro en Flutter
es el mismo defecto que dos `Monto`. Se detecta en la revisión visual conjunta de TF.3, y
por eso los dos carriles comparten primer plano.

---

### `F1-M` · Sistema de diseño · Flutter

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P3** · Legion · **TF.2** · fase F1 |
| **Posee** | **`packages/diseno_flutter/**`** (paquete Dart `aportaya_diseno`, con su Widgetbook) |
| **Documento** | [[11 Fases F0 y F1 · Cimientos y sistema de diseño]] · skills `disenar-frontend`, `movil-flutter` |
| **Tamaño** | ●●●●○ |
| **Atención** | **primer plano** en TF.2, compartido con `F1-W` |

**Necesita en `dev`.** `F0-M` cerrado · `packages/tokens` congelado.

**Entrega, y quién espera.** El paquete Dart con el tema construido desde `tokens.dart`
(`ThemeExtension`), átomos, moléculas, organismos (incluido `EstadoDePantalla<T>` sobre
`AsyncValue`), las piezas móviles (tab bar, bottom sheet, teclado numérico, PIN/OTP) y el
Widgetbook con un caso de uso por componente. **La esperan `F2`, `F3`, `F4`, `F5`**, y
**se congela al cerrar**.

**Gate propio.** Goldens en claro y oscuro de cada átomo, con `textScaler` 2.0 ·
`meetsGuideline` (contraste, área táctil, etiquetas) en verde sobre todo el paquete ·
**`Monto` pasa los mismos 5.000 vectores que el de Angular** · ningún `Color(`, `Colors.`
ni `EdgeInsets` numérico fuera de `tokens.dart`, verificado por `custom_lint` · ningún
átomo tocable sin `Semantics` · **las piezas Flutter de
[[22 Mapa de la maqueta · pantalla, carril y mundo]] §6 existen con ese nombre**, cada una
con su caso en Widgetbook comparado contra la maqueta.

**Dónde se rompe.** Usando `Colors.green` o `ThemeData.light()` «mientras tanto». Un
color de Material dentro del paquete de diseño se propaga a toda la app. Y **actualizando
goldens sin mirar**: `--update-goldens` aprueba cualquier regresión visual si nadie abre
la diferencia. La regla es que el golden cambia en un commit propio, con la imagen en el PR.

---

## Ola F1 · T3 y T5 — los shells

### `F2` · Shell móvil

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P3** · Legion · **T3** · fase F2 |
| **Posee** | `apps/movil/lib/{navegacion,proveedores,infraestructura}/` · `lib/dominio/{cliente,errores,validacion}.dart` · `lib/dominio/puertos/` · `lib/pantallas/notificaciones/` — **y se congela al cerrar** |
| **Documento** | [[12 Fases F2 a F5 · App móvil]] · skill `movil-flutter` |
| **Tamaño** | ●●○○○ |

**Necesita en `dev`.** `F1-M` congelado.

**Entrega, y quién espera.** El shell con tab bar (`StatefulShellRoute`), los enchufes
por dominio ya conectados, los proveedores (sesión, tema, conexión, idempotencia,
biometría, dispositivo), **todos los puertos declarados** —`Biometria`, `AvisosPush`,
`Camara`, `AlmacenSeguro`, `Haptica`, `Conectividad`, `ProteccionPantalla`— con su
adaptador Android, la bandeja de notificaciones, Shorebird con canal por entorno y la
verificación de contrato al iniciar. Lo esperan `F3` (T4), `F4` (T5) y `F5` (T6).

**Gate propio.** La sesión sobrevive a cerrar y reabrir la app · **nada sensible en
`SharedPreferences`**: va en `flutter_secure_storage` · con la red caída la app muestra
estado, no una pantalla en blanco · un *deep link* abre la pantalla correcta con la app
cerrada · `grep -r "Platform.is" lib/` vacío fuera de `infraestructura/` · un parche de
Shorebird se toma en `desarrollo`.

**Dónde se rompe.** Dejando el shell abierto, o declarando un puerto solo cuando llega su
primer usuario. Se congela al cerrar T3 porque tres carriles de pantallas van a componer
sobre él, y un puerto que aparece en T5 es un micro-PR al shell que los tres tienen que
rebasar. **Los siete puertos se declaran acá aunque `Camara` no tenga usuario hasta F3.**

---

### `F6` · Shell backoffice

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P3** · Legion · **T3** · fase F6, en serie después de `F2` |
| **Posee** | `apps/backoffice/src/app/{nucleo,layout}/` · `app.routes.ts` · `app.config.ts` · `rutas/tablero/` · `rutas/operacion/estado` — **y se congela al cerrar** |
| **Documento** | [[13 Fases F6 a F8 · Backoffice]] · skills `web-angular`, `web-backoffice` |
| **Tamaño** | ●●○○○ |

**Necesita en `dev`.** `F1-W` congelado · `F0-B`.

**Entrega, y quién espera.** Los dos layouts (financiero y de sistemas, este último
vacío), `ServicioSesion` con rol y permisos, los interceptores con registro de acceso,
`BarraDeFiltros` con estado en la URL, `Exportador` por CU-58, `PanelDeEvidencia`, el
acceso administrativo de ADR-038, y **la `TablaDeDatos` virtualizada con el CDK**, que es
el organismo que usan las 64 pantallas de `F7` y `F8`.

**Gate propio.** La tabla rinde con 100.000 filas paginadas del servidor sin trabar el
hilo principal · navegación completa por teclado, fila a fila · exportación que respeta el
permiso del usuario · el estado de la tabla sobrevive a recargar y a pegar la URL ·
agregar una ruta vacía a `rutas/cumplimiento/` **no toca `app.routes.ts`** · `dist/` no
contiene clientes de servicios que la ruta cargada no usa.

**Dónde se rompe.** Haciendo la tabla a medida de la primera pantalla que la use. La
usan 64 pantallas de dos carriles distintos: si nace acoplada a un caso, `F7` y `F8`
terminan con dos tablas. Y el segundo: un `layout/sistemas/` que no existe en T3, con lo
cual `B5` tiene que editar `layout/` en T7, que está congelado.

---

### `F9` · Sitio público

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P5** · Dell B · **T5** · fase F9 *(deuda declarada de T3)* |
| **Posee** | `apps/web/src/app/{nucleo,layout,paginas,verificadores}/` · `app.routes*.ts` · `contenido/**` · `scripts/contenido.mjs` · `packages/dominio-cliente/**` |
| **Documento** | [[14 Fases F9 a F11 · Sitio público, SEO y GEO]] · skill `web-angular` |
| **Tamaño** | ●●●○○ |

**Casos de uso.** CU-30 y **CU-34 tarifario público con preaviso** · **CU-61
verificación pública del sorteo** · **CU-72 y CU-73 cadena de transparencia** ·
**CU-75 certificado de reputación verificable**.

**Necesita en `dev`.** `F1-W` · `F0-W` · contratos de **`2B`** (tarifario) y **`3B`**
(transparencia y certificados) · los **vectores dorados** generados por la prueba Java de
los átomos del sorteo.

**Entrega, y quién espera.** Las quince rutas con `RenderMode` declarado, el contenido en
Markdown procesado por el script, los cuatro verificadores con hidratación incremental,
y `packages/dominio-cliente` con `barajarDeterminista`, `hashDeBloque` y compañía
probados contra los vectores. Lo esperan `F10` y `F11`.

**Gate propio.**

- Las cuatro rutas públicas responden **sin sesión** y se verifican desde afuera.
- **`/verificar/*` y `/publico/*` van `noindex, nofollow`** en meta **y** en
  `X-Robots-Tag` por `headers` de la ruta — invariante 9.
- `rutas-de-servidor.spec.ts` enumera **exactamente seis** rutas `Server`.
- Las páginas de contenido **no cargan ningún cliente de API** (verificado en `dist/`).
- **No se publica ninguna afirmación regulatoria que no sea cierta hoy** (invariante 10).
- Con el gateway caído, `/verificar/x` responde `200` con `EstadoError`, no `503`.

**Dónde se rompe.** En la tensión entre SEO y protección de datos. Cuando se pelean,
**gana la protección**. Y en el presupuesto: un verificador fuera de `@defer` hidrata la
página entera y rompe los 150 KB. Es el punto de contacto con `F10`, escrito en las dos
fichas.

---

## Ola F2 y F3 · T4 a T7 — las pantallas

### `F3` · Móvil · identidad

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P1** · Mac M5 · **T4** · fase F3 |
| **Posee** | `apps/movil/lib/pantallas/identidad/` |
| **Documento** | [[12 Fases F2 a F5 · App móvil]] |
| **Tamaño** | ●●●○○ · CU-01…09, 40, 46 |
| **Atención** | **primer plano** en T4 |

**Necesita en `dev`.** `F2` shell congelado · contratos de **`1A`** y **`1C`** (T2).

**Entrega, y quién espera.** El alta real: registro en ocho pasos, verificación
documental con `MarcoDeCamara` sobre el puerto `Camara`, MFA, dispositivo de confianza,
contrato de adhesión, declaración PEP. Lo espera `F4` — sin sesión no hay billetera.

**Gate propio.** El flujo completo de alta en un **Android de gama baja** · la cámara
funciona con poca luz o falla con un mensaje útil, y **con permiso denegado ofrece la
alternativa** · el contrato de adhesión se muestra **entero** antes de aceptar · las
once pantallas registradas **solo** en `pantallas/identidad/rutas.dart` · **ficha de
paridad iOS** del bloque de ingreso escrita (ADR-036).

**Dónde se rompe.** Validando de más en el cliente. **El cliente nunca es la garantía**
(invariante 7): valida para ayudar, y el servidor protege. Y en Flutter hay una trampa
propia: meter la lógica de los ocho pasos en un `StatefulWidget` de 600 líneas. Son ocho
organismos y un `Notifier` de Riverpod que sabe en qué paso está.

---

### `F4` · Móvil · billetera

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P1** · Mac M5 · **T5** · fase F4 |
| **Posee** | `apps/movil/lib/pantallas/{billetera,alianzas}/` |
| **Tamaño** | ●●●●○ · CU-10…19, 30…33, 57 |
| **Atención** | **primer plano** en T5 |

**Necesita en `dev`.** `F3` · contratos de **`2A`** billetera y **`2B`** tarifas (T3).

**Entrega, y quién espera.** Recarga, retiro, transferencia, extracto, QR, comisiones
a la vista, el calendario del ciclo, los vales. Lo espera `F5`.

**Gate propio.** **Toda operación de dinero envía clave de idempotencia y bloquea el
botón**, probado con doble toque real y con Patrol · el importe se muestra **siempre** por
`Monto` · la comisión se muestra **antes** de confirmar (CU-30), nunca después · lectura
del QR con `mobile_scanner` tras el puerto `Camara`, en gama baja · la lista de 5.000
movimientos sin *jank* en `--profile` · `screen_protector` activo en las rutas con saldo.

**Gate que suman los deltas nuevos** ([[20 Maqueta de referencia · deltas del frontend]]):

| Delta | Lo que hay que poder mostrar | Cómo se verifica |
| :-: | --- | --- |
| **D-22** | El calendario cubre **el ciclo completo** de cada grupo | Un grupo de 12 cupos muestra 12 cuotas y se navega de la primera a la última |
| **D-22** | La **lista no se deja invadir** por lo que todavía no se debe | Con 11 cuotas abiertas, la lista muestra 4 tarjetas y una línea que dice cuántas faltan |
| **D-12** | El segmentado **se ve elegido en los dos temas** | Golden en claro y en oscuro con el segmento activo legible |
| **D-12** | Cada estado del calendario lleva **relleno y borde** | Los cuatro estados se distinguen a 36 dp, en golden |
| **D-19** | El cobro del turno muestra **bolsa, comisión, descuento por nivel y neto**, del contrato de `2B` | El cliente **no calcula** el descuento: lo recibe |
| **D-21** | El vale muestra QR **rotativo**, estado, origen y condiciones | Doble canje rechazado con `AP-VAL-03` |
| **D-17** | La cuota exigible ofrece ***No voy a poder pagar*** | Lleva a la pantalla de salidas, con el costo de cada una |

**Dónde se rompe.** En el doble toque. Es la pantalla donde el usuario, con red lenta,
toca dos veces. Si el botón no se bloquea con la misma clave, el backend absorbe el
duplicado —está diseñado para eso— pero el usuario ve dos movimientos y deja de confiar.

---

### `F5` · Móvil · pasanaku

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P1** · Mac M5 · **T6 → T7** · fase F5 |
| **Posee** | `apps/movil/lib/pantallas/{pasanaku,soporte}/` |
| **Tamaño** | ●●●●● · CU-20–29, 52, 53, 59–76 · **la fase más grande del frontend** |
| **Atención** | **primer plano** en T6 y T7 |

**Necesita en `dev`.** `F4` · contratos de **`2C`** grupos, **`3A`** aportes, **`3B`**
transparencia, **`4A`** entregas y **`4B`** garantía. Por eso empieza en T6.

**Entrega, y quién espera.** El producto: crear y unirse a un grupo, ver el turno,
aportar, recibir el fondo, ver la transparencia, reclamar. Lo espera `F12`.

**Gate propio.** Además de los seis comunes: **el turno y el sorteo se ven verificables
desde la app** (enlace a la verificación pública de CU-61) · el reclamo (CU-52) muestra
**el plazo guardado**, no uno recalculado · la mora se comunica **en hechos, no en
probabilidades** (skill `alertas-riesgo-temprano`) · cada bloque cierra con su ficha de
paridad iOS.

**Gate que suman los deltas nuevos** ([[20 Maqueta de referencia · deltas del frontend]]):

| Delta | Lo que hay que poder mostrar | Cómo se verifica |
| :-: | --- | --- |
| **D-15** | Canjear la invitación **no ocupa cupo**: el botón dice *Pedir mi cupo* | Después del canje, `GET /grupos?participante=` **no** trae el grupo |
| **D-15** | *Tu pedido de cupo* dice **quién decide, en cuánto, qué ve de vos y las tres salidas** | Las dos columnas de privacidad están, y el plazo no se recalcula al volver |
| **D-15** | La cola del organizador trae el puntaje **descompuesto**, y **rechazar exige motivo** | Confirmar un rechazo en blanco es imposible desde la interfaz |
| **D-16** | *Organizar un grupo* muestra los 14 requisitos como **cumplidos y faltantes** | Los umbrales salen del contrato de `2E`, **no del código de la app** |
| **D-16** | **Capacitación vencida suspende pero no quita los grupos vigentes** | Está en la pantalla, no en un instructivo |
| **D-17** | El participante ve **su propio expediente** | Los plazos vienen guardados; la app no los calcula |
| **D-18** | El reclamo entrega **número correlativo y fecha límite concreta** | Y dice que la segunda instancia y la ASFI siguen disponibles |
| **D-20** | El mercado marca el **tope del 5 %** antes de pisarlo | Publicar por encima del tope se bloquea con `AP-CU62-05` |
| **D-20** | Por debajo del puntaje mínimo la pantalla **no se abre y explica por qué** | Con enlace a *Tu nivel*, no un «no disponible» |

**Dónde se rompe.** Por tamaño, igual que `3C`. Se parte en bloques —grupo, turno,
aporte, entrega, transparencia, reclamo— y cada bloque cierra con sus pruebas y su ficha
de paridad. Y hay una trampa propia: es la pantalla donde se muestra el riesgo de un
participante. **Un mensaje que castiga por pronóstico** es un defecto de producto, no
una función.

---

### `F7` · Backoffice · operación

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P3** · Legion · **T6 → T7** · fase F7 |
| **Posee** | `apps/backoffice/src/app/rutas/operacion/` |
| **Documento** | [[13 Fases F6 a F8 · Backoffice]] |
| **Tamaño** | ●●●●● · 26 CU de operación |

**Necesita en `dev`.** `F6` shell · contratos de las olas 2 y 3 de backend.

**Entrega, y quién espera.** Las pantallas de soporte, contabilidad y riesgos:
conciliación, descuadres, reversos, incidencias de desembolso, arqueos, solicitudes
escaladas, reclamos.

**Gate propio.** **Segregación de funciones visible**: quien registra no es quien
aprueba, y el `canMatch` de cada ruta monta solo el lado que el rol permite · toda
acción con efecto pide confirmación con el dato concreto delante · toda bandeja con plazo
ordena por vencimiento · las 26 rutas registradas **solo** en `operacion.routes.ts`, con
carga perezosa verificada en `dist/`.

**Gate que suman los deltas nuevos:** D-15 (cola de solicitudes escaladas), D-18
(reclamos con puerta real y plazo guardado); D-16 vive en `F8.C`, no acá.

**Dónde se rompe.** Dando a los operadores más de lo que su rol permite «porque es
interno». El backoffice es donde una fuga de permisos no se nota hasta la auditoría. Y
en Angular: un componente de 400 líneas con la tabla, los filtros y el formulario de
reverso adentro. Son un organismo de `@aportaya/ui` y dos propios.

---

### `F8` · Backoffice · cumplimiento

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P4** · Dell A · **T7 → T8** · fase F8 |
| **Posee** | `apps/backoffice/src/app/rutas/cumplimiento/` |
| **Tamaño** | ●●●●● · 38 CU de cumplimiento y gobierno |

**Necesita en `dev`.** `F6` shell · contratos de **`2D`** auditoría, **`2E`**
organizador y **`3C`** cumplimiento.

> **Mismo puesto que `3C` y `2D`, a propósito.** El que implementó la regla es el que
> dibuja su pantalla.

**Entrega, y quién espera.** Alertas, casos, PCC-01, ROG, ROS, requerimientos de
autoridad, reclamos con sus plazos, actas de comité, habilitación de organizadores,
verificaciones como expediente.

**Gate propio.** **El plazo que se muestra es el guardado**, con su fecha de inicio
visible · toda decisión que perjudique muestra **el estado del debido proceso** · el acta
de comité registra **voto nominal y abstención**, y no se cierra sin quórum · **rechazar
u observar sin causal del catálogo es imposible** · los formularios largos (ROS, actas)
**guardan borrador** y lo recuperan tras una sesión caída.

**Dónde se rompe.** Mostrando una probabilidad como si fuera un hecho. Una alerta de
riesgo es una **razón para acompañar**, no una condena: la pantalla tiene que dejar
claro qué es un hecho registrado y qué es una estimación del modelo.

---

### `B5` · Backoffice de sistemas

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P3** · Legion · **T7** · fase F8.D |
| **Posee** | `apps/backoffice/src/app/rutas/sistemas/` · `layout/sistemas/` (que F6 dejó vacío) |
| **Documento** | [[13 Fases F6 a F8 · Backoffice]] §F8.D · [[20 Maqueta de referencia · deltas del frontend]] D-2 |
| **Tamaño** | ●●●○○ |

**Necesita en `dev`.** `F6` shell · `/erp/*` e `/indicadores/*` de la plataforma (Ola 3).

**Entrega, y quién espera.** Estado de servicios, SLO con presupuesto de error,
despliegues e interruptores, base y migraciones, respaldos con la última restauración
probada, proveedores con costo real, outbox y descartados, webhooks, más *Accesos* e
*Incidentes* que se mudan de `F7` y `F8`. Lo espera `F12`.

**Gate propio.** Un rol financiero **no ve** este backoffice **y** sus endpoints
responden `403` · restauración probada hace más de 30 días ⇒ marcada como vencida · un
interruptor que toca dinero exige dos personas, y la interfaz lo impide · la cola de
descartados es visible con motivo por mensaje.

**Dónde se rompe.** Compartiendo el menú con el backoffice financiero «para no duplicar».
Son dos productos con dos usuarios; mezclarlos es lo que termina dándole a un operador
financiero permisos sobre la base de datos.

---

### `F10` · SEO

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P5** · Dell B · **T6** · fase F10 |
| **Posee** | `apps/web/src/app/seo/` — **incluido `ServicioMeta`** y el JSON-LD — y la parte de sitemap de `scripts/contenido.mjs` |
| **Documento** | [[14 Fases F9 a F11 · Sitio público, SEO y GEO]] |
| **Tamaño** | ●●○○○ |

**Necesita en `dev`.** `F9` sitio público.

**Entrega, y quién espera.** `ServicioMeta`, JSON-LD, sitemap, canónicas, Lighthouse CI
configurado y bloqueante. Lo espera `F11`, que **le pasa lo suyo por el `data` de la
ruta** y no edita el servicio.

**Gate propio.** Lighthouse CI bloqueante con CWV en verde · **el sitemap no incluye
ninguna ruta `/verificar/*` ni `/publico/*`**, verificado por prueba · JSON-LD válido sin
`Review`, `AggregateRating` ni `FinancialService` · `budgets` de `angular.json` bajo 150 KB
comprimidos con las quince páginas.

**Dónde se rompe.** Indexando lo que no se puede indexar. **Cuando el SEO y la protección
de datos se pelean, gana la protección** — y acá es donde se pelean.

---

### `F11` · GEO

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P5** · Dell B · **T7** · fase F11 |
| **Posee** | `apps/web/src/app/geo/` · `public/robots.txt` · la parte de espejos `.md` y `llms.txt` de `scripts/contenido.mjs` |
| **Tamaño** | ●●○○○ |

**Necesita en `dev`.** `F10` — `ServicioMeta` es de `F10`.

**Entrega, y quién espera.** `robots.txt` conforme a ADR-042, `llms.txt`,
`llms-full.txt`, los espejos `.md` generados por el script y la guía de redacción. Y la
**primera medición en los cuatro motores**, que es un hito del [[informe]].

**Gate propio.** Los espejos `.md` se generan solos desde `contenido/`, no se escriben a
mano · `llms.txt` no expone ninguna ruta con datos de terceros · la medición queda
registrada con fecha · el contenido de las diez preguntas está **prerenderizado**, nunca
dentro de un `@defer`.

**Dónde se rompe.** Editando `ServicioMeta`. Es de `F10`; `F11` le pasa
`alternateMarkdown` por el `data` de la ruta. Es el conflicto nº 6 de
[[16 Carriles de frontend]] y la reversión es automática.

---

## Ola F4 y los carriles nuevos · T8 y T9

### `F12` · Publicación

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P1** · Mac M5 · **T8** · fase F12 |
| **Documento** | [[15 Fase F12 · Endurecimiento, E2E y publicación]] |
| **Posee** | `apps/backoffice/e2e/` · `apps/web/e2e/` · `apps/movil/integration_test/` · las fichas de tienda |
| **Tamaño** | ●●●○○ |
| **Atención** | **primer plano** en T8 |

**Necesita en `dev`.** `F5`, `F7`, `F8`, `B5`, `F9`, `F10`, `F11` cerrados · el backend
desplegado por `5T`.

**Entrega.** E2E (Playwright en web y backoffice, **Patrol en Android desde P3** y en
dispositivo físico), accesibilidad con lector, rendimiento medido en gama baja,
seguridad, **`flutter build appbundle` e `ipa`, Shorebird en `produccion`, y envío a App
Store y Play**.

**Gate propio.** El de `definicion-de-terminado` sin gate crítico en rojo, más la
aprobación efectiva en **ambas** tiendas, un parche de Shorebird tomado por un dispositivo
real, y todas las fichas de paridad iOS cerradas.

**Dónde se rompe.** Es el **único punto único de falla del parque**: si el Mac cae, la
publicación en iOS se detiene y no hay reasignación posible. El E2E de Android corre en
P3 en paralelo y el `appbundle` puede salir de P3, pero el `ipa` no.

---

### `F13` · Backoffice · contabilidad ERP · **carril nuevo**

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P3** · Legion · **T9** · fase F13 |
| **Posee** | `apps/backoffice/src/app/rutas/contabilidad/` |
| **Documento** | [[13 Fases F6 a F8 · Backoffice]] §F13–F14; el documento de fase completo se escribe al abrir el carril |
| **Tamaño** | ●●○○○ · CU-100–106 |

**Necesita en `dev`.** `F6` shell · contratos de **`5A`** (T8).

**Entrega.** Períodos contables, presupuestos por centro de costo, órdenes de compra,
facturas de proveedor, activos fijos, estados financieros.

**Gate propio.** **Un período cerrado se ve cerrado**: la pantalla no ofrece asentar en
él. Y el estado financiero se descarga con su hash, para que sea el mismo documento que
generó el backend.

**Dónde se rompe.** Reimplementando el formato de importes o de fechas contables. `Monto`
es el único formateador, también acá.

---

### `F14` · Backoffice · publicidad · **carril nuevo**

| | |
| --- | --- |
| **Puesto · tramo · fase** | **P4** · Dell A · **T10** · fase F14 |
| **Posee** | `apps/backoffice/src/app/rutas/publicidad/` |
| **Documento** | [[13 Fases F6 a F8 · Backoffice]] §F13–F14; el documento de fase completo se escribe al abrir el carril |
| **Tamaño** | ●●○○○ · CU-110–114 |

**Necesita en `dev`.** `F6` shell · contratos de **`5B`** (T9).

**Entrega.** Anunciantes, campañas, moderación de piezas creativas, desempeño,
liquidación.

**Gate propio.** La cola de moderación (CU-112) es **previa** a la entrega: no existe la
pantalla que publica una pieza sin moderar. Y el desempeño mostrado cuadra con lo
facturado en CU-114.

**Dónde se rompe.** Mostrando métricas de desempeño que no coinciden con la liquidación.
Si el anunciante ve un número en el panel y otro en la factura, la discusión no es
técnica y no la gana el sistema.

---

## Ver también

[[19 Contrato de carril · conflicto cero, skills y calidad verificada]] · [[17 Plan de acción secuencial · coordinación de cinco máquinas]] · [[07 Carriles de trabajo concurrente]] ·
[[16 Carriles de frontend]] · [[00 Plan maestro]] · [[10 Plan maestro del frontend]] ·
[[00c Recetario · implementar un caso de uso]] · [[informe]]
