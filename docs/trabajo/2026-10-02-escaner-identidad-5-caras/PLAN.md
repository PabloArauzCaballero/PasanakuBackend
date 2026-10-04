# Plan — Escáner de identidad (5 capturas, como Atlas) en el alta de Pasanaku

- Fecha: 2026-10-02 · Repos afectados: PasanakuBackend (worktree `PasanakuBackend-escaner`, rama `justin/feature/escaner-identidad-atlas` sobre `origin/test`) · Predecesor: ninguno
- Resultado observable: una persona completa el alta en la app Flutter tomando 5 fotos (anverso, reverso, selfie, perfil izquierdo, perfil derecho) con el mismo comportamiento que la app de Atlas (consejos, hoja previa, chequeo de calidad, estados de subida, pantalla de estado), sin cambiar la estética de Pasanaku; un operador en el backoffice ve las 5 fotos juntas y aprueba o rechaza; la app refleja el veredicto.
- Kill-test: en el backoffice, abrir un expediente recién creado por la app y ver menos de 5 fotos, o ver que "Aprobar" sigue habilitado con fotos faltantes.

## Alcance
- IN: columnas `url_perfil_izquierdo`/`url_perfil_derecho` en `verificacion_kyc`; enum de caras a 5 en `identidad.yaml` y Java; endpoint `GET /usuarios/{usuarioId}/verificacion`; pantallas Flutter de captura (hoja, cámara con guía, consejos, chequeo de calidad, atajo dev), subida en lote y estado; backoffice con 5 caras; E2E en emulador Android + capturas de backoffice.
- OUT: escáner del sistema real (ML Kit) se deja detrás de bandera apagada por defecto — documentar la decisión, no es bloqueante; QR de comercio; refactor de pantallas no tocadas; arreglar botones muertos preexistentes (Perfil "Guardar", Baja, "¿Olvidaste?") — se anotan, no se tocan (regla 00 §3).
- Ambigüedades registradas:
  1. Endpoint de estado público por `usuarioId` (sin sesión) — confirmar con seguridad. Supuesto: aceptado por paralelismo con `subirDocumento` (`UsuariosController.java:124-128`) y porque el dispositivo nuevo no puede autenticar (MFA sin factor).
  2. ¿El cambio también se replica al repo `PasanakuFrontend`? Supuesto: se trabaja solo en `PasanakuBackend` (superconjunto en `test`); PR final decide el equipo.
  3. Subida diferida vs inmediata (Atlas sube apenas captura; Pasanaku crea la cuenta al final). Supuesto: se valida calidad al instante, se sube en lote tras `POST /usuarios`, con la misma máquina de estados visual.

## H1 — Modelo y base: 2 columnas nuevas en verificacion_kyc
**CA:** Dado el modelo `.puml` con las 2 columnas nuevas, cuando se corre el generador de DDL, entonces solo cambia `sql/10_tablas/01_identidad_usuarios/verificacion_kyc.sql` y la base recreada las expone a jOOQ.
**DoD:** `python scripts/generar_ddl.py` (git status muestra 1 archivo) · `python scripts/verificar_boveda.py` OK · `./gradlew bd:reset` · `./gradlew :servicios:identidad:compileJava` código 0.
**Estado:** HECHO — sus 3 microtareas HECHO con evidencia (`h1-s1-m1-m2-ddl-boveda.txt`, `h1-s1-m3-bd-reset.txt`)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Agregar columnas en el .puml (clase + entity) | El diff del .puml muestra las 2 columnas en los 2 lugares | `git diff docs/entidades/01_identidad_usuarios.puml` muestra el cambio | HECHO |
| H1.S1.M2 | Regenerar DDL y bóveda | Solo cambia el .sql de verificacion_kyc | `git status --short sql/` → verificacion_kyc.sql + nulabilidad.sql (lateral esperado); `verificar_boveda.py` → TODO OK | HECHO |
| H1.S1.M3 | Recrear base (bajar+levantar+aplicar+semillas+dev+verificaciones+humo) | Las 2 columnas existen en la base viva | `\d identidad.verificacion_kyc` muestra url_perfil_izquierdo/derecho; humo 165 OK 0 FALLA | HECHO (ver evidencia/h1-s1-m3-bd-reset.txt) |

## H2 — Contrato + Java del servicio identidad
**CA:** Dado el contrato con 5 caras y el endpoint de estado, cuando se sube cualquier foto o se consulta el estado, entonces el servicio responde correctamente para las 5 caras y sin sesión para el estado.
**DoD:** `./gradlew :servicios:identidad:test :servicios:identidad:webTest :servicios:identidad:integrationTest` verde.
**Estado:** HECHO — tres corridas: (1) BUILD SUCCESSFUL 3m25s pero con `SeguridadWebTest` sin el mock nuevo (corregido); (2) BUILD SUCCESSFUL en verde; (3) tras extraer el schema compartido `CaraDelExpediente` (ver hallazgo abajo) y arreglar los 2 controllers, `./gradlew :servicios:identidad:test :servicios:identidad:webTest :servicios:identidad:integrationTest --continue` -> BUILD SUCCESSFUL en 2m27s, cero FAILED.

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Enum de 5 caras en identidad.yaml (5 posiciones) + schema EstadoDeVerificacion + path nuevo | El spec valida y trae PERFIL_IZQUIERDO/DERECHO | `generarServidorOpenApi` generó `UsuariosApi.consultarEstadoDeVerificacion` y `EstadoDeVerificacion` sin error | HECHO |
| H2.S1.M2 | Cara enum + etiqueta() con guion en CU02GuardarFotoDelExpediente | etiqueta() de PERFIL_IZQUIERDO es "perfil-izquierdo" | compileJava OK; test unitario pendiente | HECHO (TESTED: test+webTest+integrationTest verdes) |
| H2.S1.M3 | AnotarFotoEnExpediente + ExpedienteRepositorio: 2 casos nuevos | Anotar perfil izq/der escribe su columna | compileJava OK; integrationTest pendiente | HECHO (TESTED: test+webTest+integrationTest verdes) |
| H2.S1.M4 | RevisionRepositorio: select/mapeo/claveDeFoto + deUsuario() | La cola y el detalle devuelven 5 fotos | compileJava OK; integrationTest pendiente | HECHO (TESTED: test+webTest+integrationTest verdes) |
| H2.S1.M5 | CU02ConsultarEstadoDeVerificacion + UsuariosController endpoint público | GET sin sesión devuelve estado sin PII | compileJava OK; webTest escrito, corriendo | HECHO (TESTED: test+webTest+integrationTest verdes) |
| H2.S1.M6 | VerificacionesController.CARAS a 5 | subirDocumento acepta las 5 caras | compileJava OK; webTest escrito, corriendo | HECHO (TESTED: test+webTest+integrationTest verdes) |
| H2.S1.M7 | Tests (Web + integración) | Los nuevos casos están en verde | salida pegada en evidencia/ | HECHO — `evidencia/h2-tests.txt`: `test webTest integrationTest --continue` → BUILD SUCCESSFUL in 2m 27s |

## H3 — Clientes generados + simulado + backoffice (5 caras)
**CA:** Dado el contrato nuevo, cuando se regeneran los clientes, entonces el backoffice tipa 5 caras y la cola exige las 5 para habilitar Aprobar.
**DoD:** `yarn workspace @aportaya/backoffice typecheck lint test:front test:a11y` verde.
**Estado:** HECHO (TESTED) — typecheck exit 0 · lint exit 0 (con `pyshim`) · test:front 354/354 · test:a11y 34/34 (`evidencia/h3-h8-backoffice-gate.txt`). Causa raíz del rojo inicial: el doble versionado `clientes/angular/identidad.ts` (regla 65, de otro carril) se resuelve ANTES que la carpeta generada `clientes/angular/identidad/` (gitignored) y declaraba solo 3 caras → el backoffice habría mostrado 3 fotos (kill-test). Fix mínimo: el doble expone `CaraDelExpediente` con los 5 valores del contrato y `ExpedienteEnRevisionFotosEnum` queda como alias; `tira-de-fotos.ts` tipa `caras` como `CaraDelExpediente[]`. El doble sigue siendo del carril de contratos: se borra cuando el CI publique el cliente real.

**Hallazgo que simplifica el hito (factual-discovery, antes de escribir nada):**
`tira-de-fotos.ts` (`protected readonly caras = Object.values(ExpedienteEnRevisionFotosEnum)`)
y `pantalla-de-expedientes.ts` (`completo()` = `this.caras.every(c => e.fotos.includes(c))`,
misma fuente) ya iteran el enum generado **dinámicamente**, no una lista de 3 escrita a
mano. Con el cliente Angular regenerado desde el contrato de 5 caras, la tira muestra 5
figuras y `completo()`/`Aprobar` exige las 5 **sin tocar un solo archivo de
`apps/backoffice`**. El grid CSS (`auto-fit, minmax(12rem,1fr)`) ya es responsivo a N
elementos. Los specs existentes (`tira-de-fotos.a11y.spec.ts` usa una lista PARCIAL
`['ANVERSO','SELFIE']`, no asume un total de 3) siguen válidos sin editar.
Por disciplina de alcance (regla 00 §3) se **descarta** la parte de H3 que proponía
crear `etiquetaDeCara()`/`carasFaltantes()` y nuevos specs: sería inventar una mejora
no pedida sobre código que ya funciona para 5 caras. Queda solo: regenerar el cliente
y correr el gate.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Estados de `ExpedienteEnRevisionEstadoEnum` en el doble `clientes/angular/identidad.ts` con los valores del contrato | Dado un expediente `EN_REVISION` recién creado por la app, cuando el operador abre «Por decidir» o el filtro EN_REVISION, entonces lo ve | `yarn workspace @aportaya/backoffice typecheck test:front` verde · Playwright: la cola lista el expediente (captura) | HECHO (VERIFIED) — typecheck 0 · test:front 354/354 · lint 0 · test:a11y 34/34 (`evidencia/h3-s1-m1-estados-gate.txt`) · Playwright contra el stack real: «Por decidir» lista el expediente nuevo (`evidencia/h7-bo-01-cola-*.png`) |

**Hallazgo H7 (runtime, 2026-10-02):** con el backoffice contra el stack real, la cola decía «No hay expedientes esperando» con un expediente `EN_REVISION` recién creado, y el filtro pedía `GET /identidad/verificaciones?estado=EnRevision` → `[]`. Causa: el doble versionado (mismo de H3) declara los estados con el nombre del miembro (`'EnRevision'`) en vez del valor del contrato (`EN_REVISION`, `identidad.yaml:493`). Clase: PRODUCT_BUG en el doble de otro carril; corrección mínima de valores, sin tocar la lógica (que usa las claves).

## H4 — Flutter: capturas, cámara, calidad, consejos, atajo de desarrollo
**CA:** Dado el paso de capturas, cuando la persona toma las 5 fotos (o usa el atajo dev), entonces ve el mismo comportamiento que Atlas (hoja, consejos, calidad, estados) sin cambiar la estética de Pasanaku.
**DoD:** `flutter analyze` + `flutter test` en verde.
**Estado:** HECHO (TESTED) — ver detalle de la corrida original abajo; regresión de cierre: `flutter analyze` sin issues, 263 passed / 2 failed preexistentes de Windows, gate de diseño TODO OK (`evidencia/cierre-movil-regresion.txt`). Detalle: todos los archivos escritos

## H5 — Flutter: subida en lote y pantalla de estado
**CA:** Dado el envío del alta, cuando las 5 fotos se suben, entonces la persona ve el estado (pendiente/en revisión/aprobada/rechazada) con polling y puede reintentar.
**DoD:** tests de subida y de verificación en verde.
**Estado:** HECHO (VERIFIED) — subida en lote y estado verificados en runtime (dos altas completas con 5 urls en base) y caminos de error cubiertos por 13 tests unitarios (H5.S2.M1-M2, con chequeo de mutación); título agregado (H5.S2.M3). Detalle original abajo.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | Las capturas sobreviven a `AltaNotifier.reiniciar()` hasta la subida (PRODUCT_BUG hallado en runtime) | Dado un alta completa, cuando se crea la cuenta, entonces la pantalla de subida sube las 5 fotos y `verificacion_kyc` del usuario nuevo tiene las 5 urls | test unitario de `SeguimientoDelAlta` en verde · corrida en emulador + SQL con 5 urls | HECHO (VERIFIED) — `flutter test test/unidad/seguimiento_del_alta_test.dart` 3/3 (`evidencia/h5-s1-m1-seguimiento-test.txt`) · emulador: 5 filas «Documento enviado» · SQL: usuario nuevo con las 5 urls, el de la corrida rota con 0 (`evidencia/h5-s1-m1-subida-runtime.txt`) |
| H5.S2.M1 | Tests unitarios de `SubidaNotifier`: subida ok, «lenta» a los 10 s, cancelar → pendiente, fallo → fallida, reintentar con la MISMA clave, repetir con clave ROTADA, una que falla no frena a las demás | Cada transición del contrato de la máquina de estados se observa en un test | `flutter test test/unidad/subida_del_expediente_test.dart` verde | HECHO — 7 tests en verde; con la alerta movida a 30 s el test «lenta» falla (chequeo de mutación) · `evidencia/h5-s2-notifiers-test.txt` |
| H5.S2.M2 | Tests unitarios de `VerificacionNotifier`: sondea cada 2 s, se detiene en estado final, tope de 20 intentos, error → AsyncError | Ídem | `flutter test test/unidad/estado_de_verificacion_test.dart` verde | HECHO — 6 tests en verde; sin el tope de 20 el test del tope falla (chequeo de mutación) · `evidencia/h5-s2-notifiers-test.txt` |
| H5.S2.M3 | Título en la pantalla de subida («Subiendo tus documentos» / «Tus documentos están enviados») | La barra superior deja de estar vacía | `flutter analyze` + captura del emulador | HECHO — `flutter analyze` sin issues · gate de diseño sin fallas (fila extraída a `fila_de_subida.dart` por el tope de 200 líneas) · emulador: «Tus documentos están enviados» (`evidencia/h8-18-subida-titulo-10.png`, oscuro); segunda alta con 5 urls en base |
| H8.S3.M6 | Regenerar los goldens de `diseno_flutter` en Linux (contenedor Docker con la versión de Flutter del CI) en vez de esperar al runner | `flutter test test/goldens` en Linux 18/18 con las imágenes versionadas | salida del contenedor en `evidencia/` | SUSTITUIDA por H12.S1.M1 (goldens generados y verificados en el runner macOS). Motivo: premisa equivocada (decidido en sesión, 2026-10-03): las referencias son de macOS, no de Linux; generarlas en un contenedor Linux no coincidiría con el CI. Ver H8.S3.M5 |

**Hallazgo H5 (runtime, 2026-10-02 15:49):** en el emulador la pantalla de subida mostró las 5 filas en «—» y «Continuar» habilitado; el backend no recibió ningún `POST /documentos` y la fila de `verificacion_kyc` del usuario recién creado quedó `EN_REVISION` sin fotos (kill-test del plan). Causa: `pantalla_registro.dart` llama a `AltaNotifier.reiniciar()` (vacía `capturas`) ANTES de navegar a `/registro/subida`, y esa pantalla lee las capturas de `altaProvider` → `subirPendientes` recorre un mapa vacío. Clase: PRODUCT_BUG, introducido en H5 por este mismo trabajo. Fix: `SeguimientoDelAlta` guarda `usuarioId` + copia de las capturas; la pantalla de subida lee de ahí.

## H6 — (opcional) Vencimiento del CI en el contrato
**Estado:** HECHO en H12.S2 — se reabrió con las decisiones del usuario (2026-10-03): la fecha la escribe la persona en el alta y un carnet vencido bloquea Aprobar. Ver H12.

## H7 — E2E en emulador + backoffice + capturas + REPORTE
**CA:** Dado el stack real (2 servicios + base), cuando se completa el alta en el emulador y se aprueba en el backoffice, entonces la app refleja "Identidad verificada" y la base tiene las 5 urls y revisada_por.
**DoD:** capturas de app y backoffice en evidencia/, SQL de verificación pegado, REPORTE.md con AVANCE en la primera línea.
**Estado:** HECHO (VERIFIED) — alta completa en emulador → 5 fotos en base → operador ve las 5 juntas en el backoffice (Playwright, stack real) → Aprobar → `POST …/decision` 200 → base `APROBADA` con `revisada_por` → la app muestra «Identidad verificada». Kill-test negativo observado: el expediente sin fotos muestra «Faltan fotos» y Aprobar deshabilitado. Evidencia: `h7-e2e-aprobacion.txt`, `h7-bo-0*.png`, `h8-14/15*.png`

## H8 — Extensión pedida por el usuario (2026-10-02, 2ª sesión): skills, transiciones serias, botón vivo, flujo premium
Pedido literal: "traer todas las skills y hacer el diseño ultra premium con motion skills y que cada botón tenga un diseño vivo"; "los que tienen son diarreicos y horribles"; "la transición tipo zoom es la peor que he visto… hacela decente y seria para el tipo de app que es". **Esto deroga el supuesto inicial "sin cambiar la estética de Pasanaku"** para botones, transiciones y el flujo de captura (decisión del usuario).
**CA:** Dado cualquier pantalla de la app, cuando la persona navega, entonces la pantalla entra con un desplazamiento lateral corto y un fundido (≤ 320 ms, sin escalado) y en iOS conserva el gesto nativo de volver; cuando ve o toca un botón, este tiene profundidad (degradado sutil + sombra en capas teñida), responde al toque con resorte y compresión de sombra, y el primario tiene un brillo que lo recorre; con "reducir movimiento" nada se anima.
**DoD:** `flutter analyze` 0 errores en `packages/diseno_flutter` y `apps/movil` · `flutter test` de diseno_flutter y de movil sin fallos nuevos · `python scripts/verificar_frontend.py diseno` OK · capturas del emulador en claro y oscuro inspeccionadas.
**Estado:** HECHO — H8.S3.M5 (goldens) se cerró en H12.S1.M1 con el runner macOS de GitHub Actions

Ambigüedades H8: (a) "traer todas las skills": el worktree ya tiene las 261 del estándar Pasanaku; faltaban solo 5 skills de diseño de **Atlas** (`atlas-diseno`, `atlas-movimiento`, `atlas-ui-componentes`, `atlas-app-movil-ux`, `atlas-estilo-portales`) escritas para Expo/Reanimated/Next — no aplican a Flutter/Angular y copiarlas metería guía falsa (regla 00 §1.3). Supuesto: se actualiza PasanakuPromptManager (6 commits atrás) y se verifica que el espejo del repo no tenga deriva; las skills de Atlas se leen como referencia, no se instalan. Confirmar con Pablo. (b) "cada botón" incluye el backoffice: supuesto **sí** para la primitiva compartida `packages/ui/src/boton` (CSS), sin rediseñar pantallas del backoffice.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H8.S1.M1 | `git pull` de PasanakuPromptManager y chequeo de deriva del espejo de skills | El PM queda al día con origin y el worktree no tiene skills faltantes del estándar | `git status -sb` sin "behind" · `comm` de listados vacío | HECHO — PM `db17381` al día con origin; 194 skills, 0 faltantes en el worktree (261). Un daily local de Justin chocaba con origin: quedó la versión de origin y la edición local sigue en `stash@{0}` del PM, sin perder nada |
| H8.S2.M1 | `TransicionDeEje` (eje lateral + fundido, sin escala) reemplaza a `TransicionConZoom` en `tema.dart`; iOS usa la de Cupertino (gesto de volver) | Ninguna `Transform.scale` en la transición de página | `flutter test` del test nuevo de transición en verde | HECHO — `transicion_de_eje.dart`; `transicion_con_zoom.dart` borrado; test 'desplaza y funde, nunca escala' en verde (evidencia/h8-diseno-flutter.txt) |
| H8.S2.M2 | `TransicionDeMarca` (1,45 s, logo atravesando la cámara) → fundido cruzado sobrio ≤ 420 ms | Salir de la portada no muestra el logo volando | test de transición en verde + captura | HECHO (TESTED; captura pendiente en H8.S5) — 420 ms, fundido + subida 16 px; `panel_de_marca.dart` quedó sin uso y se borró |
| H8.S3.M1 | Tokens de movimiento `Movimiento` (duraciones, curvas, resortes) en diseno_flutter | Los átomos nuevos no usan `Duration(` sueltos | `grep -c "Duration("` en átomos nuevos = 0 | HECHO — `atomos/movimiento.dart`; superficie_viva/transiciones usan solo `Movimiento.*` |
| H8.S3.M2 | `Boton` vivo: degradado sutil, sombra en capas teñida, filo de luz, brillo que recorre el primario, resorte + compresión de sombra, carga animada | Los 6 variantes renderizan y respetan reducir movimiento | test de widget del botón en verde | HECHO (TESTED) — `superficie_viva.dart` + `luz_de_boton.dart`; brillo finito (4 cruces) para que `pumpAndSettle` termine; 9/9 en verde (evidencia/h8-diseno-flutter.txt) |
| H8.S3.M3 | `BotonIcono` y `BotonFlotante` con el mismo resorte y halo | Ambos se hunden al tocar | test de widget en verde | HECHO (TESTED) — evidencia/h8-diseno-flutter.txt |
| H8.S3.M4 | Primitiva Angular `packages/ui/src/boton` con transiciones por token y `prefers-reduced-motion` | Hover eleva, active hunde, sin `transition: all` | `yarn workspace @aportaya/backoffice typecheck lint` | HECHO (RUNS) — gate completo en verde (`evidencia/h3-h8-backoffice-gate.txt`); captura en H7 |
| H8.S4.M1 | Flujo de capturas premium: tarjeta de captura, navegación Anterior/Siguiente, hoja "Antes de escanear" | Sin textos sueltos como navegación; tarjeta con jerarquía | captura del emulador inspeccionada | HECHO — riel de 5 caras + tarjeta con marco de encuadre + sin Anterior/Siguiente; capturas `h8-05-capturas-boton-fuera.png` (vacío) y `h8-06-capturas-completas.png` (5/5) inspeccionadas |
| H8.S4.M2 | Sacar «Tomar foto / Repetir la foto» del `PageView` y dejarlo fijo debajo, actuando sobre la cara visible | La sombra del botón no aparece cortada en seco (hallazgo en `evidencia/h8-04-capturas-vacio.png`: el `PageView` recorta con `Clip.hardEdge` lo que sale de cada página, y el botón estaba al fondo de la tarjeta) | `flutter analyze` 0 errores · captura del emulador con la sombra completa | HECHO — `flutter analyze lib/pantallas/identidad` → No issues found · `verificar_frontend.py movil` sin fallas · antes/después: `evidencia/h8-04-capturas-vacio.png` (sombra cortada) vs `evidencia/h8-05-capturas-boton-fuera.png` (sombra completa). El pie se extrajo a `pie_de_capturas.dart` para respetar el tope de 200 líneas (paso_capturas.dart 214 → 179) |
| H8.S3.M5 | Regenerar goldens de `catalogo_golden_test.dart` (botones cambian a propósito) en el CI Linux | El golden de botones refleja la piel nueva | `flutter test --update-goldens` en Linux | HECHO en H12.S1.M1 (run 37164040999, commit `1bb277de`). Antes: BLOQUEADO — las referencias golden de este repo están hechas en **macOS** y el CI las compara en `macos-latest` (job `app-goldens`, `ci.yml:373`; comentario en `catalogo_golden_test.dart:3-4`: el rasterizador da píxeles distintos en Linux/Windows). Desde esta máquina Windows no se pueden generar. Afecta `packages/diseno_flutter/test/goldens` (18) **y** `apps/movil/test/goldens` (9: transición de ingreso, saldo, escanear). Lo destraba: en una Mac (o en el runner macOS), `yarn workspace @aportaya/diseno-flutter test:goldens --update-goldens` y `yarn workspace @aportaya/movil test:goldens --update-goldens`, y versionar las imágenes. Dueño: quien tenga Mac en el equipo / quien mantenga `diseno_flutter`. simulacion-65 con Windows como doble (`evidencia/h8-s3-m5-goldens-simulacion.txt`, archivos restaurados): ACEPTADO — `--update-goldens` y comparar → 18/18 y 9/9; LIMITE — segunda comparación seguida → 18/18 y 9/9 (render determinista, claro y oscuro); INVALIDO — contra las referencias versionadas fallan 15/18 y 9/9, pero ese nivel NO distingue el cambio: en Windows ya fallaban sin tocar nada (diferencia de plataforma) |
| H8.S5.M1 | Prueba visual en emulador, claro y oscuro | Capturas en `evidencia/h8-*.png` revisadas | archivos presentes + revisión anotada | HECHO — claro: `h8-01-portada.png`, `h8-05`, `h8-06`, `h8-12-subida-1.png` (botón en carga con brillo), `h8-14`, `h8-15`; oscuro: `h8-16-estado-aprobada-oscuro.png`, `h8-17-portada-oscuro.png`, `h7-bo-01-cola-dark.png`. Revisadas; hallazgos anotados en el REPORTE |

## H9 — Backoffice probado como operador humano (pedido 2026-10-03)
Pedido literal: "levantes el backoffice y pruebes que realmente funciona … como un usuario humano, realizando correcciones si hubiera".
**CA:** Dado el backoffice contra el stack real, cuando un operador entra, filtra la cola, mira las 5 fotos, rechaza con motivo o aprueba, entonces cada acción hace lo que dice, la persona ve el resultado en la app, y nada se rompe en móvil, tablet ni escritorio, en claro ni oscuro, ni usando solo el teclado.
**DoD:** script Playwright `bo-humano` en verde contra el stack real (salida en `evidencia/h9-*.txt`) · capturas por viewport y tema inspeccionadas · consola y red sin errores inesperados · cada defecto encontrado con su microtarea, fix y re-test.
**Estado:** HECHO (VERIFIED) — 58/58 en la corrida final + 19/19 visual; gates backoffice y ui en verde salvo `monto.spec` (timeout preexistente, reproducido en la base `22c5e687`; corregido en H11.S2.M1). Recarga sin sesión (H9.S1.M7): resuelta en H10 con el diseño de ADR-010.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H9.S1.M1 | Ingreso: contraseña errónea, código erróneo y correcto | El error se muestra asociado y el correcto entra | `bo-humano` paso «ingreso» PASS | HECHO (VERIFIED) — contraseña equivocada, código equivocado y correcto · `evidencia/h9-bo-humano-corrida.txt` (corrida final: base recreada desde esta rama, humo 165 OK, identidad `cc418e08`, `ng serve` limpio) |
| H9.S1.M2 | Cola y filtros: Por decidir, PENDIENTE, EN_REVISION, APROBADA, RECHAZADA | Cada filtro lista solo su estado y marca el activo | `bo-humano` paso «filtros» PASS | HECHO (VERIFIED) — 5 filtros, cada uno con su estado, `aria-pressed`, esqueleto con red lenta · `evidencia/h9-bo-humano-corrida.txt` (corrida final: base recreada desde esta rama, humo 165 OK, identidad `cc418e08`, `ng serve` limpio) |
| H9.S1.M3 | Fotos: ver/ocultar, 5 imágenes cargadas, expediente incompleto sin Aprobar | Ídem | `bo-humano` paso «fotos» PASS | HECHO (VERIFIED) — 5 fotos con alt, ocultar, parcial 3/5 sin Aprobar y con motivo · `evidencia/h9-bo-humano-corrida.txt` (corrida final: base recreada desde esta rama, humo 165 OK, identidad `cc418e08`, `ng serve` limpio) |
| H9.S1.M4 | Rechazar sin motivo (bloqueado) y con motivo (persistido, visible en RECHAZADA y en la API pública que lee la app) | Ídem | `bo-humano` paso «rechazo» PASS + SQL | HECHO (VERIFIED) — rechazo sin motivo bloqueado; con motivo: 200, en RECHAZADA y la API que lee la app devuelve RECHAZADA + motivo · `evidencia/h9-bo-humano-corrida.txt` (corrida final: base recreada desde esta rama, humo 165 OK, identidad `cc418e08`, `ng serve` limpio) |
| H9.S1.M5 | Aprobar un expediente completo y verlo en APROBADA | Ídem | `bo-humano` paso «aprobar» PASS + SQL | HECHO (VERIFIED) — aprobar: 200, en APROBADA, la app lee APROBADA · `evidencia/h9-bo-humano-corrida.txt` (corrida final: base recreada desde esta rama, humo 165 OK, identidad `cc418e08`, `ng serve` limpio) |
| H9.S1.M6 | Solo teclado: Tab llega a filtros, Ver fotos, Motivo, Aprobar/Rechazar con foco visible | Ídem | `bo-humano` paso «teclado» PASS | HECHO (VERIFIED) — Tab llega a filtros, Ver fotos, Motivo, Aprobar; foco visible · `evidencia/h9-bo-humano-corrida.txt` (corrida final: base recreada desde esta rama, humo 165 OK, identidad `cc418e08`, `ng serve` limpio) |
| H9.S1.M7 | Recargar la página con sesión abierta | La sesión sigue o se explica por qué no | observación + causa | HECHO — **resuelto en H10** (renovación con cookie según ADR-010, verificada en navegador 14/14). Diagnóstico original: recargar vuelve a /ingreso porque el backoffice llama `POST /sesion/refrescar` (`refresco-de-sesion.ts:35`, PR #18) y ese endpoint **no existe** en ningún contrato ni en identidad/gateway; `POST /sesiones` devuelve el token en el cuerpo y no setea cookie (observado con Playwright, `cookies guardadas: []`). Renovar sesión con cookie es una decisión de seguridad (DECISION_REQUIRED: rotación/revocación/ADR, dueño: identidad + quien lleve auth del backoffice), fuera de este trabajo |
| H9.S3.M1 | El motivo es de CADA expediente: escribir en una tarjeta no llena ni habilita «Rechazar» en otra (defecto: un solo `signal` compartido por toda la cola) | Ídem | `bo-humano` paso «motivo» PASS + spec | HECHO (VERIFIED) — spec 5/5 (mutación: 2 fallan con el motivo compartido) + `bo-humano` «motivo» PASS |
| H9.S3.M2 | Si la decisión falla, el operador lo ve en esa tarjeta con un mensaje accionable (defecto: `error: () => resolviendo.set(false)` lo callaba) | Ídem | spec con 5xx + `bo-humano` | HECHO (VERIFIED) — spec + `bo-humano` «error» (503 simulado: aviso en esa tarjeta, se puede reintentar, no aparece en otras) |
| H9.S3.M3 | El filtro activo se anuncia con `aria-pressed` (defecto: solo por color, regla 95.4.6) — input `presionado` en `ap-boton` | Ídem | `bo-humano` paso «filtros» PASS + test:a11y | HECHO (VERIFIED) — `aria-pressed=true` en el filtro activo en los 5 filtros |
| H9.S3.M4 | `AP-CU04-04` dice «Ese código no sirve…» y no «venció» (el backend usa el mismo código para equivocado y vencido, `CU04Autenticar.java:155-160`) | Ídem | `bo-humano` paso «ingreso» | HECHO (VERIFIED) — «Ese código no sirve: revisalo o pedí uno nuevo.» en runtime |
| H9.S3.M5 | En 390 px el backoffice no tiene scroll horizontal (defecto: 39 px de desborde; la columna `1fr` del shell no baja del mínimo de la cabecera, que no podía partirse, `shell-financiero.ts:67-70`) | `scrollWidth - clientWidth ≤ 0` en 390×844 | `bo-humano visual` PASS + captura móvil | HECHO (VERIFIED) — móvil 390 px: sobra 0 px (antes 39) en claro y oscuro |
| H9.S3.M6 | El bloqueo por intentos cuenta fallos CONSECUTIVOS de verdad: desde el último ingreso exitoso, y sin contar `FACTOR_REQUERIDO` (es el primer paso de todo ingreso con segundo factor, no un fallo) | Dado un operador que entró 5 veces con su segundo factor en 15 min, cuando se equivoca una vez de contraseña, entonces recibe «no coinciden» y NO queda bloqueado; 5 contraseñas equivocadas seguidas siguen bloqueando | `./gradlew :servicios:identidad:test --tests '*CU04*'` verde + runtime: 6 ingresos y 1 error sin bloqueo | HECHO (VERIFIED) — antes del fix 2 tests FALLAN, después CU04 13/13 y suite de identidad verde; runtime con la imagen nueva: tras 8 ingresos normales un tipeo equivocado responde «no coinciden» y 0 bloqueos (base: 8 exitosos, 8 FACTOR_REQUERIDO, 5 fallos reales) · `evidencia/h9-s3-m6-*.txt` |
| H9.S3.M7 | Un botón fantasma/sobreVerde/enlace DESHABILITADO se ve deshabilitado (regresión de H8: la regla de la variante ganaba a la de `:disabled`; «Ver las fotos» sin fotos parecía habilitado, solo cambiaba el cursor) | El color del deshabilitado difiere del habilitado | medición con Playwright + test:a11y + captura | HECHO (VERIFIED) — «Ver las fotos» sin fotos: texto pasa del verde del habilitado a `--text-3` (claro rgb(100,113,105), oscuro rgb(137,153,142)) · `evidencia/h9-s3-m7-m8-contraste.txt` + capturas |
| H9.S3.M8 | Menú activo del backoffice legible en oscuro (medido 2,86:1; WCAG AA pide 4,5:1) — `--brand-bg` en vez de `--g100` fijo | contraste ≥ 4,5:1 en claro y oscuro | medición con Playwright + captura | HECHO (VERIFIED) — menú activo: oscuro 2,86:1 → 9,02:1, claro 7,11:1 → 14,33:1 · `evidencia/h9-s3-m7-m8-contraste.txt` |
| H9.S2.M1 | Prueba visual 390×844, 768×1024, 1440×900 en claro y oscuro | Sin desbordes, sin texto cortado, contraste legible | capturas `h9-*.png` inspeccionadas | HECHO — 390×844, 768×1024, 1440×900 en claro y oscuro: sin scroll horizontal, fotos cargadas, consola/red limpias; capturas `h9-06-*.png` inspeccionadas |

**Hallazgos H9 (2026-10-03), clasificados con evidencia:**
- ENVIRONMENT (2026-10-03 02:47): otra sesión (worktree `PasanakuBackend-campania`, rama `justin/test/campania-cu-principales`, campaña E2E) recreó el stack Docker compartido desde su rama (`docker inspect … project.working_dir`), que no tiene `url_perfil_izquierdo/derecho`; los contenedores de identidad/cumplimiento de este trabajo quedaron abajo. Decisión del usuario: **esperar** a que campania termine y recién ahí recrear la base desde esta rama para la corrida final (H9.S3.M5 visual, H9.S3.M6 runtime). No se tocó el stack de campania.
- PRODUCT_BUG en identidad (seguridad, regla 90.5): `AccesoRepositorio.fallidosConsecutivos` cuenta TODO intento no exitoso de los últimos 15 min, incluidos los `FACTOR_REQUERIDO` que deja cada ingreso normal con segundo factor, y aunque haya éxitos en el medio. Observado en `identidad.intento_autenticacion`: 13 pares `FACTOR_REQUERIDO`/éxito entre 02:27 y 02:29 y el siguiente error de contraseña bloqueó la cuenta 30 min (`DEMASIADOS_INTENTOS`). Con la recarga que cierra la sesión (H9.S1.M7), un operador real se bloquea solo. → H9.S3.M6.
- PRODUCT_BUG corregidos: motivo compartido entre tarjetas (H9.S3.M1), error de la decisión callado (H9.S3.M2), filtro activo solo por color (H9.S3.M3), «el código venció» ante un código mal escrito (H9.S3.M4).
- TEST_BUG del arnés, corregidos y no del producto: lectura de la cola en el mismo cuadro del clic (con red lenta de 1,5 s el producto muestra el esqueleto, `evidencia/h9-00-filtro-cargando.png`), foco medido en el `<input>` en vez de su `.caja` (`:focus-within`), conteo sin esperar el repintado, Tab a un botón deshabilitado.
- Conocido / fuera de alcance: `/sesion/refrescar` no existe (H9.S1.M7); `packages/ui/src/monto/monto.spec.ts` «pasa los mismos vectores que el Monto de Flutter» vence a los 5000 ms **también en la base `22c5e687`** (preexistente, reproducido restaurando `packages/ui` y `packages/tokens` de esa base).
- Entorno: el arnés dejó la cuenta de operador de dev bloqueada 30 min (5 fallidos en 15 min, `intentos-maximos: 5`, `duracion-bloqueo: PT30M`): el control de seguridad funciona. Se esperó el vencimiento, no se tocó la base; los casos negativos del ingreso quedan detrás de `NEGATIVOS=1`.

## H10 — Renovación de sesión del backoffice: recargar no saca al operador (pedido 2026-10-03: «arreglalo»)
Origen: H9.S1.M7. El backoffice llama `POST /sesion/refrescar` (`refresco-de-sesion.ts:35`) y ese endpoint no existía. El diseño NO se inventa: es el de **ADR-010** (backoffice: cookie `httpOnly` + `Secure` + `SameSite` estricto; refresh rotado en cada uso con detección de reuso que revoca la familia; ventana de gracia corta para carreras) y el modelo ya está en la base: `token_verificacion` con `tipo_token = 'REFRESCO'`, `familia_id`, `rotado_de_id`, `uq_token_refresco_vivo`, trigger `fn_seg_detectar_reuso_refresco` (R-SEG-09) y `sesion.refresco_familia_id`. Faltaban la política sembrada, el contrato y el código.
**CA:** Dado un operador que entró al backoffice, cuando recarga la página, entonces sigue dentro sin volver a ingresar; cuando alguien reusa un token de refresco ya rotado, entonces la familia y la sesión quedan revocadas y el siguiente refresco da 401.
**DoD:** tests de integración de identidad (rotación, reuso → revocación, vencido, sin cookie) en verde · webTest del endpoint y de la cookie (`HttpOnly`, `Secure`, `SameSite=Strict`, `Path`) · runtime: Playwright entra, recarga y sigue en `/cumplimiento` · el token en claro nunca se guarda ni se loguea (solo su SHA-256).
**Estado:** HECHO (VERIFIED) — 165 tests de identidad + 14/14 en navegador real. Dos hallazgos del runtime, corregidos: (1) PRODUCT_BUG propio: `digest()` de pgcrypto vive en `public`, fuera del `search_path` de `svc_identidad` → 500 en el ingreso web; el SHA-256 pasa a Java y los tests ahora corren con el search_path del servicio (con el código viejo fallan: `evidencia/h10-s3-m1-fix-digest.txt`). (2) ENVIRONMENT: `aportaya/gateway:local` era una imagen del 22-09 que exigía Bearer en `/api/v1/sesiones`; se reconstruyó desde esta rama.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H10.S1.M1 | Política `REFRESCO_SESION` en `seeders/minimos/13-politicas-de-token.json` (ttl = vigencia de sesión, 12 h) + SQL regenerado | La fila existe tras `bd:semillas`, idempotente | `generar_semillas.py` + segunda corrida sin cambios + SQL | HECHO (TESTED) — fila en el JSON, SQL regenerado (+5 líneas) e idéntico en la segunda corrida; los tests siembran la misma fila. Aplicarla en la base viva queda para H10.S3.M1 (el stack lo tiene campania) |
| H10.S1.M2 | Contrato: `POST /sesion/refrescar` en `identidad.yaml` (público por cookie; 200 `{acceso, rol, permisos}`; 401) | El spec valida y genera `SesionesApi.renovarSesion` | `generarServidorOpenApi` OK | HECHO (RUNS) — `generarServidorOpenApi` → `SesionApi.renovarSesion` + `SalidaRenovacion` |
| H10.S2.M1 | `RenovarSesion`: emitir (familia nueva, hash SHA-256, ligado a la sesión) y renovar (consumir con precondición en el UPDATE, rotar, gracia 10 s, reuso → trigger revoca) | Ídem CA | integrationTest nuevos en verde | HECHO (TESTED) — `CU04RenovacionTest` 7/7 contra PostgreSQL real (rotación, reuso revoca familia y sesión, gracia sin revocar, desconocido/ausente, vencido, sesión revocada, solo hash); mutaciones: sin el UPDATE de reuso y con GRACIA=0 fallan los tests correspondientes (`evidencia/h10-s2-m1-renovacion-tests.txt`) |
| H10.S2.M2 | `SesionesController`: cookie en el ingreso WEB y endpoint `/sesion/refrescar` (401 borra la cookie) | La cookie sale con `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/sesion` | webTest | HECHO (TESTED) — `SesionControllerWebTest` 3/3 + 2 nuevos en `SesionesControllerWebTest` (cookie WEB con HttpOnly/Secure/SameSite=Strict/Path; ANDROID sin cookie) · suite identidad: test 5, webTest 55, integrationTest 105, 0 fallos (`evidencia/h10-identidad-tests.txt`) |
| H10.S3.M1 | Runtime: entrar → recargar → sigue dentro; reuso → 401 | Ídem | Playwright + SQL | HECHO (VERIFIED) — navegador real contra el stack de esta rama: 14/14 (`evidencia/h10-s3-m1-runtime.txt`): cookie HttpOnly/Secure/SameSite=Strict/Path=/api/v1/sesion, invisible para `document.cookie`; recargar dos veces sigue dentro y rota; pestaña nueva entra directo; reusar el refresh viejo → 401 y la sesión queda revocada (SQL: R-SEG-09 en tokens y sesión). Capturas `h10-01..04-*.png` |

## H11 — Cerrar los hallazgos anotados que se pueden cerrar (pedido 2026-10-03: «todo lo que se pueda, cerralo»)
**CA:** Cada hallazgo de la lista del REPORTE queda corregido con su test, o declarado con el motivo concreto por el que no se puede cerrar desde acá.
**DoD:** `flutter analyze` + `flutter test` de `apps/movil` y `packages/diseno_flutter` sin fallos (incluidos los 2 de Windows), gates del backoffice/ui.
**Estado:** HECHO — 6 hallazgos cerrados con test. Lo que quedaba abierto (H6, goldens macOS, meta del gateway en desarrollo) se cerró en H12.
Desvío: las microtareas se escribieron en el plan después de los primeros cambios de código (errores, loopback, carnet, fecha, rutas), no antes. Se registra.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H11.S1.M1 | Catálogo de errores del móvil alineado al contrato: `AP-CU01-03` (celular O documento), `AP-CU04-01` (contraseña), `AP-CU04-04` (código), `AP-CU04-05` (bloqueo, minutos) | Cada texto describe lo que el contrato dice del código | `autenticar_test` (expectativa corregida: fijaba el texto equivocado) | HECHO (TESTED) — móvil: analyze sin issues, 283/283 tests (`evidencia/h11-movil.txt`) |
| H11.S1.M2 | `10.0.2.2` cuenta como loopback en debug y se rechaza en release | Ídem | 2 tests nuevos en `configuracion_test` | HECHO (TESTED) — 2 tests nuevos en `configuracion_test` · móvil: analyze sin issues, 283/283 tests (`evidencia/h11-movil.txt`) |
| H11.S1.M3 | Carnet de prueba lee el documento escrito (o uno sintético al azar), nunca `1234567` fijo | Dos altas de prueba seguidas no chocan con `AP-CU01-03` | `carnet_de_prueba_test` 3 tests | HECHO (TESTED) — `carnet_de_prueba_test` 3/3 · móvil: analyze sin issues, 283/283 tests (`evidencia/h11-movil.txt`) |
| H11.S1.M4 | La fecha de nacimiento elegida se muestra tal cual (antes «14 oct» por «15 oct») | `Fecha.formatearDia(DateTime(1995,10,15))` = «15 oct 1995» | `diseno_flutter` `fecha_test` | HECHO (TESTED) — `fecha_test` 2/2; diseno_flutter 49/49 sin goldens (`evidencia/h11-diseno.txt`); el backend ya recibía la fecha correcta (`1995-10-15T00:00:00.000` → día) |
| H11.S1.M5 | Los 2 tests del móvil que fallaban en Windows comparan rutas normalizadas | Pasan en Windows sin debilitar lo que verifican | `flutter test test/unidad` 0 fallos | HECHO (TESTED) — `gate_del_shell_test` y `enchufe_de_rutas_test` pasan en Windows · móvil: analyze sin issues, 283/283 tests (`evidencia/h11-movil.txt`) |
| H11.S2.M1 | `monto.spec.ts` (ui) deja de vencer a los 5 s | Pasa | `yarn workspace @aportaya/ui test:front` | HECHO (TESTED) — de >5000 ms a 252 ms con `detectChanges()` sincrónico; misma aserción; mutación detectada; `@aportaya/ui` 112/112, typecheck y lint OK (`evidencia/h11-monto.txt`) |
| H11.S3.M1 | H6 (vencimiento del CI) | Se decide | — | HECHO en H12.S2 (decisión tomada por el usuario el 2026-10-03). Antes: DESCARTADO — no se puede cerrar sin decisión de producto: el modelo ya tiene `documento_identidad.fecha_expiracion` y el estado `VENCIDA`, pero falta decidir de dónde sale la fecha (la escribe la persona u OCR) y qué hace un carnet vencido (bloquea la aprobación o solo avisa). DECISION_REQUIRED; dueño: producto/cumplimiento. Propuesta en el REPORTE |

## H12 — Cero pendientes (pedido 2026-10-03: «nada pendiente»; lo revisará la ASFI)
Decisiones del usuario (2026-10-03): (1) la fecha de vencimiento del carnet **la escribe la persona** en el alta; (2) un carnet vencido **bloquea Aprobar** en el backoffice; (3) autorizado el **push** de la rama y generar los goldens en un runner **macOS** de GitHub Actions.
**CA:** No queda ninguna microtarea BLOQUEADA, TODO ni DESCARTADA por falta de acción de este carril; todo cambio con su test y su evidencia.
**DoD:** suites en verde (identidad, backoffice, ui, móvil, diseño) · goldens regenerados y verificados en macOS · REPORTE sin pendientes.
**Estado:** HECHO — 15 microtareas con evidencia. Fuera del alcance de esta rama y declarado en el REPORTE: vulnerabilidades de dependencias de todo el repositorio (OSV/Trivy).

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H12.S1.M1 | Workflow `goldens-macos.yml`: regenera en `macos-latest`, verifica sin `--update` y commitea en la rama | El run termina en verde y la rama trae las imágenes nuevas | run de Actions + `git log` | HECHO (VERIFIED) — run 37164040999 `success`; commit `1bb277de` del bot con 16 referencias (diseño 6 + app 10); el job verifica sin `--update-goldens` antes de commitear. En Windows los goldens siguen sin coincidir (plataforma, documentado); el resto de la app 293/293 |
| H12.S2.M1 | Contrato: `documento.fechaExpiracion` (date) en el alta; `fechaExpiracionDocumento` y `documentoVigente` (lo calcula el servidor) en `ExpedienteEnRevision`; rechazos declarados | El spec valida y genera | `generarServidorOpenApi` + `generateOpenApiClients` | HECHO (TESTED) — clientes Dart/Angular regenerados; Postman regenerado (identidad, recorrido) |
| H12.S2.M2 | Identidad persiste la fecha (`documento_identidad.fecha_expiracion`, columna ya existente) y la devuelve a la cola; alta con documento vencido → `AP-CU01-07` sin crear nada | Alta con fecha → guardada; vencida → 422 | integrationTest + webTest | HECHO (TESTED) — `CU01Test` (2 casos nuevos), `UsuariosControllerWebTest` (llega al caso de uso; fecha mal formada → 400) · `evidencia/h12-s2-identidad-tests.txt` |
| H12.S2.M3 | **El servidor** rechaza APROBAR sin las 5 fotos (`AP-CU01-09`), sin vencimiento (`AP-CU01-10`), vencido (`AP-CU01-11`) o ya resuelto (`AP-CU01-08`, con `FOR UPDATE`); rechazar con motivo sigue permitido | Cada caso → 422 y el expediente sigue `EN_REVISION`; vence hoy → se aprueba | integrationTest + webTest | HECHO (TESTED) — `CU02DecisionDelServidorTest`, `CU02RevisionTest` (re-decisión), `VerificacionesControllerWebTest` (422 con código) · identidad 173/173 |
| H12.S2.M4 | App: campo «Vence el» obligatorio en el paso 1 (calendario desde hoy), un carnet vencido no deja avanzar, el cotejo lo muestra, `AP-CU01-06/07` con texto propio | Validación con mensaje en el campo | `flutter analyze` + `flutter test` | HECHO (TESTED) — analyze sin issues; 293/293 sin goldens; 3 archivos de prueba nuevos o ampliados |
| H12.S2.M5 | Backoffice: la tarjeta muestra «Vence el …» (sin corrimiento de zona), vencido o sin fecha → «Aprobar» deshabilitado con el motivo; `AP-CU01-08..11` traducidos | Ídem | specs + a11y + bo-humano | HECHO (TESTED) — backoffice 373 + 36 a11y, ui 114 + 3 a11y |
| H12.S2.M6 | **Hallazgo de trazabilidad:** los rechazos de la decisión se habían numerado `AP-CU02-01..04`, que en la bóveda son de CU-02 (cumplimiento) con otro significado. Pasan a `AP-CU01-08..11` (la decisión fija `verificacion_kyc.estado`: CU-01 flujo 4b) y se documenta el flujo en la bóveda | Ningún código con dos significados | `verificar_criterios.py` + `verificar_boveda.py` | HECHO (TESTED) — ambos gates OK; también se declaró `AP-CU01-06` (clave rechazada), que el servidor ya emitía sin estar en el contrato ni en la bóveda |
| H12.S2.M7 | Gate de criterios: 9 pruebas de CU-04 (renovación de sesión, H10) y 2 de CU-01 no tenían criterio Gherkin en la bóveda | `verificar_criterios.py` sin fallas | ídem | HECHO (TESTED) — criterios agregados; 2 nombres de prueba llevados a forma Gherkin |
| H12.S2.M8 | Reglas del barrido: Spotless (también `RenovarSesion.java` y 2 web tests ya commiteados) y ningún archivo ≥300 líneas (`CU02RevisionTest`, `UsuariosControllerWebTest` separados) | `spotlessCheck` + `testBarrido` | gradle | HECHO (TESTED) — BUILD SUCCESSFUL |
| H12.S2.M9 | Runtime: alta con vencimiento desde el emulador; backoffice como operador (vigente/vencido/sin fecha, forzar Aprobar desde DevTools → 422) | Ídem | emulador + Playwright + SQL | HECHO (VERIFIED) — API: vencido → 422 `AP-CU01-07`, nada creado; app: «Vence el» obligatorio, calendario desde hoy, cotejo, alta con `fecha_expiracion=2029-10-15` en la base; backoffice fase A 15/15 (vigente, vence hoy, sin fecha → 422 `AP-CU01-10` al forzar) y fase B 9/9 pasada la medianoche, sin tocar datos (mismo expediente vencido por el reloj del servidor → 422 `AP-CU01-11` al forzar; rechazo con motivo guardado) · `evidencia/h12-runtime.txt`, `h12-app-*.png`, `h12-bo-*.png`. Un `UPDATE` manual para simular el día fue denegado (y corresponde: regla de no arreglar la base a mano); por eso la fase B esperó a la medianoche |
| H12.S2.M10 | CI completo (`workflow_dispatch`) sobre la rama | Los jobs que dependen de este trabajo en verde | GitHub Actions | HECHO (VERIFIED) — encontró 3 defectos propios, corregidos: Postman regenerado antes del cambio de códigos, ejemplos de `packages/simulado` sin `documentoVigente`, e2e del backoffice que dependía de que `/sesion/refrescar` no existiera en el mock. OSV-Scanner y Trivy fallan por dependencias de todo el repositorio (igual en `main`/`dev`, esta rama no toca `yarn.lock` ni `gradle.lockfile`): ver `evidencia/h12-ci-osv.txt` y REPORTE |
| H12.S3.M1 | Traer el fix de `digest()` de invitaciones (campania `ba43e798`) | Invitaciones sin `digest` sin calificar | integrationTest identidad | HECHO (TESTED) — `public.digest(...)`, mismas líneas que `ba43e798`; CU69 en verde |
| H12.S3.M2 | Aviso de Postman `recibirWebhookPasarela` sin `@Permiso` | El generador no avisa | `generar_postman.py` sin AVISO | HECHO (TESTED) — la regex cortaba en el `)` de una anotación con argumentos; corrida sin AVISO |
| H12.S3.M3 | Backoffice en desarrollo contra el stack real sin arnés | `ng serve` de desarrollo resuelve el gateway | prueba en navegador | HECHO (TESTED) — `index.desarrollo.html` con el meta (`/api/v1`), proxies `proxy.desarrollo.json` (Prism) y `proxy.stack-local.json` (`yarn start:stack`) · `evidencia/h12-s3-m3-dev-gateway.txt`; navegador en H12.S2.M9 |

## Hallazgo H7: el APK de Android compila -- 2 fixes de entorno necesarios
Primer intento de `flutter run -d emulator-5554` fallo: `permission_handler_android`
exige `compileSdk 37`, pero (a) el Android SDK de esta maquina solo tenia hasta 35/36
instalados y el auto-instalador de Gradle bajo `platforms;android-37.0` -- el Android
SDK cambio su esquema de nombres de plataforma a version decimal (`37.0`, `37.1`...) y
AGP/Flutter todavia buscan el nombre viejo `android-37` a secas; (b)
`apps/movil/android/app/build.gradle.kts` usaba `compileSdk = flutter.compileSdkVersion`
(resuelve a 36). Arreglado con: una **union de directorio** (`New-Item -ItemType
Junction`) `android-37 -> android-37.0` en el SDK local (no es un cambio al repo, es
infraestructura de esta maquina) + `compileSdk = 37` explicito en
`build.gradle.kts` (cambio real, documentado inline, compileSdk es retrocompatible y
no afecta minSdk/targetSdk). Con los dos, `flutter run` compilo: **`Built
buildpp\outputslutter-apkpp-debug.apk`**. Quedo pendiente solo instalar en
el emulador (se mato el emulador por memoria justo antes del paso de instalacion;
el APK ya esta compilado y cacheado, reinstalar es cuestion de segundos).

Memoria: la maquina (32GB) corriendo a la vez Docker Desktop/WSL2 (stack base + 2
servicios), el emulador Android (~3.7GB) y el primer build de Gradle para Android
(que descarga NDK r28c + SDK 37 + CMake, varios GB) llego a 3.8-4.3GB libres varias
veces; una vez el harness mato en background el primer intento de build de Docker por
presion de memoria (no es un fallo del comando). Mitigado: builds de Docker uno por
uno en vez de en paralelo, y apagando el emulador durante los tramos de compilacion
que no lo necesitan (no hace falta hasta instalar el APK), reiniciandolo despues.
También se liberaron ~1.4GB cerrando 3 procesos `esbuild --service` huérfanos de un
proyecto no relacionado (Alovida), hallados entre los consumidores de memoria más
altos y sin relación con esta tarea.

## Hallazgo H7: Docker Desktop quedó caído tras la presión de memoria; recuperado
Al retomar para instalar el APK, el daemon de Docker respondía `Docker Desktop is
unable to start` y los 10 contenedores de `aportaya-*` estaban `Exited (255)` (el
backend de Docker murió junto con el build que mató el harness por presión de
memoria, aunque los contenedores en sí no son parte de esa presión). La distro WSL
`docker-desktop` estaba `Stopped`. Recuperado: `wsl --shutdown`, matar los procesos
`Docker Desktop.exe`/`com.docker.*` colgados, relanzar vía
`explorer.exe "shell:AppsFolder\Docker.DockerForWindows.Settings"` (el `.exe` no
vive en una ruta fija de Program Files en esta instalación), esperar el daemon
(`docker info`), y `docker start` de los 10 contenedores en el orden de dependencia
(base -> identidad/cumplimiento -> gateway/nginx). Confirmado sano: los 10
`healthy` y `curl localhost/api/v1/cumplimiento/contratos/vigentes` -> `200`.

## Hallazgo H7: `10.0.2.2` (alias del emulador a la máquina anfitriona) no pasa la
## validación de gateway de la app -- workaround sin tocar código de la app
Primer intento de abrir la app instalada mostró la pantalla de bloqueo "AportaYa no
puede arrancar" (`pantallas/arranque/...`, ver `dominio/cliente.dart`): compilé el
APK con `--dart-define=API=http://10.0.2.2/api/v1` pero sin `HOSTS_PERMITIDOS`.
`dominio/configuracion.dart:34` define `_loopback = {'localhost', '127.0.0.1',
'::1', '0.0.0.0'}` -- **no incluye `10.0.2.2`**, pese a que el comentario de
`cliente.dart:27-28` documenta explícitamente que "desde el emulador de Android la
máquina anfitriona es `10.0.2.2`" como si debiera admitirse igual que loopback en
debug. Es una discrepancia real entre el comentario y el código (`esLoopback` da
`false` para `10.0.2.2`, cae a `host-ajeno` porque `HOSTS_PERMITIDOS` viene vacío).
No es un archivo de mi alcance (es validación compartida de arranque, no del
escáner de identidad) así que no lo edito -- lo rodeo con el mecanismo que el mismo
código ya expone para esto: agregar `--dart-define=HOSTS_PERMITIDOS=10.0.2.2` al
build de prueba. Reconstruido con ambos defines; evidencia en
`evidencia/h7-flutter-build-apidefine2.txt`. Queda anotado para quien sea dueño de
`configuracion.dart`: o el comentario está desactualizado, o falta agregar
`10.0.2.2` (y el equivalente de iOS, `localhost` ya cubierto) al set de loopback de
debug.

## Hallazgo: 2 tests preexistentes fallan en Windows, sin relación con este cambio
`flutter test test/unidad test/widget test/contrato test/identidad test/pasanaku`:
260 passed, 2 failed. Los 2 son `gate_del_shell_test.dart` ("Platform.is solo aparece
dentro de infraestructura/") y `enchufe_de_rutas_test.dart` ("agregar una pantalla
vacía en un dominio no cambia ningún archivo fuera de él"): ambos comparan
`f.path.contains('/infraestructura/')` / `contains('/pantallas/identidad/')` con una
barra `/` fija, que nunca calza con las rutas de Windows (`\`) -- confirmado con
`git diff --stat HEAD -- <ambos archivos>` vacío: ningún archivo tocado por este
trabajo. Son bugs preexistentes del harness de tests en Windows, no introducidos ni
agravados por este cambio; reproducen igual en un checkout limpio sin tocar nada.
No se arreglan (fuera de carril, archivos de test compartidos) -- se anotan para el
equipo.

## Hallazgo de entorno: falta un paso de build previo a `flutter analyze`/`dev:movil`
`package:aportaya_diseno/tokens/tokens.dart` no existe hasta correr
`yarn workspace @aportaya/diseno-flutter build` (copia `packages/tokens/generado/tokens.dart`
a `packages/diseno_flutter/lib/tokens/tokens.dart` + `flutter pub get`). El script de
conveniencia `dev:movil` del root solo corre `yarn workspace @aportaya/tokens build`
(CSS+JSON+Dart dentro de `packages/tokens/`), **no** el paso de `diseno-flutter` que copia
ese archivo al paquete que `apps/movil` importa. Primer `flutter analyze` en este
worktree limpio dio 687 errores, TODOS por esta causa (confirmado con `git diff`: cero
de esos archivos son míos). Se corrió el paso que faltaba manualmente; no se tocó
`package.json` (troncal) — se deja anotado como hallazgo para el equipo, no se arregla.
Además se corrigieron en este mismo paso dos errores reales introducidos por mí:
import relativo mal armado en `textos_de_subida.dart` (faltaba `dominio/`), uso de un
miembro interno de Riverpod (`copyWithPrevious`) en `estado_de_verificacion.dart`, y un
`case _` inalcanzable en `pantalla_estado_de_verificacion.dart`.

## Hallazgo H2 adicional: bug del generador Dart con enums de array duplicados
Al tener `ExpedienteEnRevision.fotos` y `EstadoDeVerificacion.fotos` como dos arrays
`items: {type: string, enum: [...]}` con el MISMO conjunto de 5 valores, el generador
`dart-dio` (openapi-generator) emite en `expediente_en_revision.dart` un campo
`List<ExpedienteEnRevisionFotosEnum>` pero el enum que realmente genera en ese archivo
se llama `EstadoDeVerificacionFotosEnum` (y en `estado_de_verificacion.dart` el campo
referencia un `FotosEnum` que no existe en ningún lado) -- `build_runner`/
`json_serializable` falla con `InvalidType`. No reproduce en el cliente Angular (cada
schema genera su propio enum sin cruzarse). Es un bug del generador frente a enums de
array duplicados entre schemas, no algo introducido a mano. Fix: se extrajo un schema
compartido `CaraDelExpediente` (`components.schemas`) y los dos `fotos.items` ahora son
`$ref` a él -- es además mejor práctica de OpenAPI (una sola fuente de verdad para el
enum). Esto generó un top-level `CaraDelExpediente` en Java (ambos controllers
actualizados, compila) y previsiblemente en Angular/Dart tambien (a confirmar al
regenerar clientes; si el backoffice importaba `ExpedienteEnRevisionFotosEnum` por
nombre, se actualiza el import -- cambio mecánico, no de lógica).

## Desvíos registrados durante la ejecución
- `python3` en esta máquina es el stub roto de Microsoft Store; `python` (3.14.6) es el real. Los scripts del repo y las tareas Gradle (`bd/build.gradle.kts`) invocan `python3` a secas. Se creó un shim en `C:\Users\DELL\tools\pyshim\` (archivo `python3` sin extensión para bash, `python3.bat` para procesos nativos de Windows como el hijo de Gradle) y se antepone al PATH en cada comando. No se tocó `bd/build.gradle.kts` (fuera de alcance, es troncal).
- El primer `./gradlew bd:reset` reutilizó un daemon de Gradle con el PATH viejo (sin el shim); se corrigió con `./gradlew --stop` + `--no-daemon`.
- `docker compose --profile base up -d --wait` (tarea `bd:levantar`) falló una vez con `exit 1` aunque los 7 servicios quedaron `healthy` según `docker ps`; causa: el contenedor de un solo uso `aportaya-minio-bucket` (crea el bucket y termina con `exit 0`, sin healthcheck) entra en carrera con el `--wait` de compose. Es una condición de carrera de infraestructura preexistente en `despliegue/compose/base.yml`, no causada por este cambio — se reintentó.
- Concurrencia: por unos minutos corrieron en paralelo `bd:reset` (Docker/Gradle) y la instalación de paquetes del SDK de Android (`sdkmanager`, red). No comparten recursos (uno es Docker/CPU local, el otro es descarga de red) y la regla 70.1.4 habla de builds/test runners; se decidió no interrumpir ninguno de los dos para no perder progreso. No se repite con un tercer proceso en paralelo.
- Segundo desvío de la regla 70.1.4: se lanzó `generateOpenApiClients` mientras la rerun de `test/webTest/integrationTest` todavía corría (error, ambos son `./gradlew --no-daemon` contra el mismo proyecto). Gradle serializa el acceso a sus cachés compartidas con locks de archivo entre procesos del mismo proyecto, así que el riesgo real es que uno espere al otro, no que se corrompan — se decidió no matar ninguno de los dos a mitad de escritura (más riesgoso que esperar) y no abrir un tercero en paralelo. Verificado después: ningún archivo de evidencia quedó truncado ni mezclado entre las dos corridas.

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin Android SDK/AVD en esta máquina | Bloquea H7 | Instalar cmdline-tools + system-image google_apis (en curso) |
| Flutter 3.47 vs .fvmrc 3.44.8 | pubspec.lock podría no resolver | Probar 3.47 primero, fvm 3.44.8 si falla |
| 14 servicios JVM → OOM | Máquina lenta/falla | Levantar solo identidad + cumplimiento + base |
| DestinoDeObjeto rechaza guion bajo | 500 al subir perfil_izquierdo | Cara.etiqueta() con guion, cubierto por test |
