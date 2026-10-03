# Reporte — Escáner de identidad (5 capturas, como Atlas) + botones vivos y transiciones (H8)

> **AVANCE: 45 / 48 — 93,8 %.** Rojo abierto: H8.S3.M5 (goldens de `diseno_flutter` y de la app) BLOQUEADO — las referencias son de **macOS** y esta máquina es Windows. H10.S3.M1 TODO: la renovación de sesión está implementada y probada (165 tests de identidad en verde), pero falta verla en runtime porque el stack Docker compartido lo está usando la sesión de campania. H8.S3.M6 DESCARTADO (premisa equivocada, ver Desvíos). H6 opcional sin empezar.

- Fecha: 2026-10-02 (cierre actualizado 2026-10-03, commit base `a28f3256`) · Plan: [PLAN.md](./PLAN.md) · Rama: `justin/feature/escaner-identidad-atlas` (worktree `PasanakuBackend-escaner`, sobre `origin/test`), sin commitear.
- Peldaño de evidencia alcanzado:
  - Flujo alta → 5 fotos → backoffice → aprobar → app: **VERIFIED** (runtime real: emulador Android + 10 contenedores + backoffice con Playwright, persistencia comprobada con SQL, consola y red revisadas).
  - Botones vivos y transiciones (H8): **TESTED** + prueba visual en claro y oscuro. Los goldens quedan BLOQUEADOS.
  - Subida y estado (H5): **VERIFIED** en runtime (dos altas completas) + caminos de error cubiertos por tests unitarios con chequeo de mutación.
  - Backoffice probado como operador (H9): **VERIFIED** — 58/58 + 19/19 visual contra el stack real recreado desde esta rama, con el bundle limpio y la imagen de identidad corregida.
  - Peldaño del trabajo (el más bajo de sus áreas): **TESTED**. No es REGRESSION_VERIFIED por los goldens (macOS) y por lo listado en No cubierto.

## Completado

| ID | Qué se logró (observable) | Comando de verificación | Resultado |
|---|---|---|---|
| H1.S1.M1–M3 | `verificacion_kyc` tiene `url_perfil_izquierdo` / `url_perfil_derecho` desde el modelo `.puml` | `generar_ddl.py` · `verificar_boveda.py` · `bd:reset` | PASS — `evidencia/h1-s1-m1-m2-ddl-boveda.txt`, `h1-s1-m3-bd-reset.txt` |
| H2.S1.M1–M7 | Contrato e identidad aceptan 5 caras; `GET /usuarios/{id}/verificacion` público sin datos personales | `./gradlew :servicios:identidad:test webTest integrationTest --continue` | PASS — BUILD SUCCESSFUL in 2m 27s (`evidencia/h2-tests.txt`) |
| H3 / H3.S1.M1 | El backoffice tipa 5 caras y la cola muestra los expedientes `EN_REVISION` (doble del cliente con los valores del contrato) | backoffice `typecheck`, `lint`, `test:front`, `test:a11y` | PASS — exit 0 · lint OK · 354/354 · 34/34 (`evidencia/h3-s1-m1-estados-gate.txt`) |
| H4 | Paso de capturas de 5 caras en Flutter (cámara, calidad, consejos, carnet de prueba) | `flutter analyze` · `flutter test …` · `verificar_frontend.py movil` | PASS — sin issues · 263 passed / 2 failed preexistentes de Windows · gate sin fallas (`evidencia/cierre-movil-regresion.txt`) |
| H5.S2.M1–M2 | La máquina de estados de la subida (lenta a los 10 s, cancelar, fallo que no frena a las demás, reintentar con la misma clave, repetir con clave rotada) y el sondeo del estado (cada 2 s, corta en estado final, tope de 20, error) tienen tests | `flutter test test/unidad/subida_del_expediente_test.dart test/unidad/estado_de_verificacion_test.dart` | PASS — 13/13; con la alerta a 30 s o sin tope, fallan los tests que corresponden (`evidencia/h5-s2-notifiers-test.txt`) |
| H5.S2.M3 | La pantalla de subida tiene título («Enviando tus documentos» / «Tus documentos están enviados») | `flutter analyze` · gate · emulador | PASS — `h8-18-subida-titulo-10.png`; segunda alta con 5 urls en base |
| H5.S1.M1 | **Bug corregido:** las 5 fotos ahora se suben después de crear la cuenta | `flutter test test/unidad/seguimiento_del_alta_test.dart` + corrida en el emulador + SQL | PASS — 3/3 · 5 × `POST …/documentos` 201 · usuario nuevo con 5 urls (`evidencia/h5-s1-m1-*.txt`) |
| H7 | E2E de punta a punta con el stack real | emulador + Playwright + SQL | PASS — `APROBADA`, `revisada_por` y 5 fotos en base; la app muestra «Identidad verificada» (`evidencia/h7-e2e-aprobacion.txt`, `h7-bo-0*.png`, `h8-14/15*.png`) |
| H8.S1.M1 | PromptManager al día; espejo de skills sin deriva | `git status -sb` · `comm` | PASS (ver PLAN) |
| H8.S2.M1–M2 | Transición de página lateral con fundido, sin zoom; transición de marca sobria de 420 ms | test de transición | PASS — `evidencia/h8-diseno-flutter.txt` |
| H8.S3.M1–M4 | Tokens `Movimiento`; `Boton`/`BotonIcono`/`BotonFlotante` vivos (degradado, sombra teñida, resorte, brillo, respeta reducir movimiento); primitiva Angular | tests de widget · gate del backoffice | PASS — `evidencia/h8-diseno-flutter.txt`, `h3-h8-backoffice-gate.txt` |
| H8.S4.M1 | Flujo de capturas premium: riel de 5 caras, tarjeta con marco de encuadre, sin Anterior/Siguiente | capturas del emulador inspeccionadas | PASS — `h8-05-capturas-boton-fuera.png`, `h8-06-capturas-completas.png` |
| H8.S4.M2 | La sombra de «Tomar foto» ya no aparece cortada | `flutter analyze` · gate de diseño · captura antes/después | PASS — `h8-04-capturas-vacio.png` (cortada) vs `h8-05-…` (completa) |
| H8.S5.M1 | Prueba visual en claro y oscuro (app y backoffice) | capturas inspeccionadas | PASS — `h8-01`, `h8-12-subida-1` (botón en carga), `h8-16/17-*-oscuro`, `h7-bo-01-cola-dark.png` |

### H9 — Backoffice probado como operador humano (2026-10-03)

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H9.S1.M1–M6, H9.S2.M1 | Un operador entra (con mensajes correctos ante contraseña o código equivocados), filtra la cola, mira las 5 fotos, rechaza con motivo y aprueba; la app lee el veredicto; todo con teclado; sin desborde en 390/768/1440 px, en claro y oscuro | `node bo-humano.cjs` (Playwright, stack real) | PASS — 58/58 + 19/19 visual (`evidencia/h9-bo-humano-corrida.txt`, capturas `h9-0*.png`) |
| H9.S3.M1 | El motivo es de cada expediente (antes, lo escrito en una tarjeta aparecía en todas y habilitaba «Rechazar» en cualquiera) | spec `decision-de-expediente.spec.ts` + bo-humano | PASS — 5/5; con el defecto reintroducido fallan 2 |
| H9.S3.M2 | Si la decisión falla, el aviso aparece en esa tarjeta (antes se callaba) | spec + 503 simulado | PASS |
| H9.S3.M3 | El filtro activo se anuncia con `aria-pressed` (antes solo por color) | bo-humano | PASS |
| H9.S3.M4 | Código equivocado: «Ese código no sirve…» (antes «El código venció») | bo-humano | PASS |
| H9.S3.M5 | Móvil sin scroll horizontal (antes 39 px) | bo-humano visual | PASS |
| H9.S3.M6 | **Bug de seguridad en identidad corregido:** el bloqueo contaba cada ingreso normal con segundo factor como fallido y no se reiniciaba con un ingreso correcto; un operador que entraba seguido quedaba bloqueado 30 min al primer error | `./gradlew :servicios:identidad:test webTest integrationTest` + runtime | PASS — antes del fix 2 tests fallan, después CU04 13/13; en runtime, tras 8 ingresos un tipeo equivocado no bloquea (`evidencia/h9-s3-m6-*.txt`) |
| H9.S3.M7 | Los botones fantasma/enlace deshabilitados se ven deshabilitados (regresión de H8) | medición Playwright | PASS (`evidencia/h9-s3-m7-m8-contraste.txt`) |
| H9.S3.M8 | Menú activo legible en oscuro: 2,86:1 → 9,02:1 | medición Playwright | PASS |

### H10 — Renovación de sesión del backoffice (2026-10-03)

Diseño de **ADR-010** sobre el modelo que ya estaba en la base (`token_verificacion` tipo `REFRESCO` con familia, trigger R-SEG-09, `sesion.refresco_familia_id`). No se inventó nada: faltaban la política sembrada, el contrato y el código.

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H10.S1.M1 | Política `REFRESCO_SESION` en `seeders/minimos/13-politicas-de-token.json` (12 h, cookie httpOnly) | `generar_semillas.py` ×2 | PASS — SQL +5 líneas, idéntico en la segunda corrida |
| H10.S1.M2 | `POST /sesion/refrescar` en `identidad.yaml` (público por cookie; 200 `{acceso, rol, permisos}`; 401 `AP-SES-01`) | `generarServidorOpenApi` | PASS |
| H10.S2.M1 | `RenovarSesion`: emitir (familia nueva, SHA-256) y renovar (precondición en el UPDATE, rotación, gracia de 10 s para dos pestañas, reuso → el trigger revoca familia y sesión) | `integrationTest` `CU04RenovacionTest` | PASS — 7/7; con el UPDATE de reuso quitado o la gracia en 0 fallan los tests que corresponden |
| H10.S2.M2 | Ingreso `WEB` con cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/sesion`; la app no recibe cookie; 401 borra la cookie | `webTest` | PASS — suite identidad: test 5, webTest 55, integrationTest 105, 0 fallos (`evidencia/h10-identidad-tests.txt`) |

## A medias

### H10.S3.M1 — Renovación de sesión en runtime
- **Qué anda:** el código y el contrato, demostrados por 165 tests de identidad (PostgreSQL real para la rotación y el reuso).
- **Qué no está verificado:** que en el navegador real el backoffice, al recargar, siga dentro (cookie a través de nginx → gateway → identidad).
- **Qué falta exactamente:** cuando campania libere el stack: `bd:reset` desde esta rama (siembra `REFRESCO_SESION`), reconstruir `aportaya/identidad:local` con `--build-arg BD_URL_ADMIN=jdbc:postgresql://<ip de postgres>:5432/pasanaku`, levantar identidad, y con Playwright entrar → recargar → seguir en `/cumplimiento`; reusar un refresh viejo → 401 y sesión revocada (SQL).
- **Dónde quedó:** commiteado en esta rama; compila y los tests pasan.


### H8 — Extensión de diseño
- **Qué anda:** todo menos H8.S3.M5 (ver Completado).
- **Qué no anda:** las referencias golden de `packages/diseno_flutter/test/goldens/imagenes/` (18) y de `apps/movil/test/goldens/imagenes/` (9: transición de ingreso, saldo, escanear) siguen siendo las de la piel vieja; el job `app-goldens` del CI (`macos-latest`) va a fallar hasta regenerarlas.
- **Qué falta exactamente:** en una Mac: `yarn workspace @aportaya/diseno-flutter test:goldens --update-goldens` y `yarn workspace @aportaya/movil test:goldens --update-goldens`, revisar las imágenes y versionarlas.
- **Dónde quedó:** sin cambios en ningún `test/goldens/` (las regeneraciones de prueba en Windows se restauraron).

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H8.S3.M5 | BLOQUEADO | Regenerar los goldens en **macOS** (las referencias del repo son de macOS y el CI las compara en `macos-latest`, `ci.yml:373`). Dueño: quien tenga Mac / quien mantenga `diseno_flutter`. simulacion-65 con Windows como doble: ACEPTADO 18/18 y 9/9 tras regenerar, LIMITE 18/18 y 9/9 en segunda corrida (determinista); INVALIDO 15/18 y 9/9 fallan contra las referencias, pero ese nivel no distingue el cambio porque en Windows ya fallaban sin tocar nada. Evidencia: `evidencia/h8-s3-m5-goldens-simulacion.txt` |
| H8.S3.M6 | DESCARTADO | Generarlos en un contenedor Linux: premisa equivocada (no coincidirían con macOS). Decidido en sesión, 2026-10-03. |
| H6 | TODO (opcional) | Vencimiento del CI en el contrato: no se empezó. |

## Evidencia

```text
$ flutter test test/unidad test/widget test/contrato test/identidad test/pasanaku     (cierre)
00:45 +263 -2: Some tests failed.
test/unidad/enchufe_de_rutas_test.dart … [E]      ← preexistente, '/' fijo vs rutas de Windows
test/unidad/gate_del_shell_test.dart … [E]        ← preexistente, idem

$ psql (usuario sintético, id truncado) — tras aprobar en el backoffice
 usuario  |  estado  | revisada_por | resuelta_en | anverso | reverso | selfie | perfil_izq | perfil_der
 83bba82f | APROBADA | t            | t           | t       | t       | t      | t          | t

$ psql — antes / después del fix H5.S1.M1
 a5738afe… | EN_REVISION | f | f | f | f | f      ← corrida con el bug: cuenta creada, 0 fotos
 83bba82f… | EN_REVISION | t | t | t | t | t      ← corrida con el fix

$ Playwright (backoffice contra el stack real)
IMAGENES ANVERSO 1600x1009, REVERSO 1600x1009, SELFIE 1200x1600, PERFIL_IZQUIERDO 1200x1600, PERFIL_DERECHO 1200x1600
APROBAR habilitado: true
MUTACION 200 POST /api/v1/identidad/verificaciones/<id>/decision
```

Índice de `evidencia/`: `h1-*` (base), `h2-tests.txt`, `h3-*` (gates del backoffice), `h4-*` (Flutter), `h5-s1-m1-*` (fix de la subida), `h7-*` (builds, E2E, capturas del backoffice), `h8-*` (diseño, capturas de la app), `h8-s3-m5-goldens-simulacion.txt`, `cierre-movil-regresion.txt`.

## No cubierto

- Caminos de error de la subida (lenta, cancelar, reintentar, repetir) y del sondeo: cubiertos por tests unitarios con un doble del puerto; **en runtime** solo se ejerció el camino feliz.
- Rechazo desde el backoffice y su reflejo en la app («Rechazar» con motivo): no se ejercitó; solo se aprobó.
- La cámara real del emulador y el chequeo de calidad con fotos reales: las 5 capturas del E2E salieron del **carnet de prueba sintético** (atajo de desarrollo).
- iOS (gesto nativo de volver con `TransicionDeEje`): sin simulador en esta máquina.
- Viewport tablet del backoffice y la app en tablet: solo se miró móvil (1080×2400) y escritorio (1440×900).
- Lector de pantalla en las pantallas nuevas: solo los tests automáticos de a11y del backoffice.
- Regla 98.8 (caída del servicio dependido, traza con `correlationId`): no se ejercitó apagar identidad durante la subida.

## Desvíos del plan

- **2026-10-03 (H9):** otra sesión (worktree `PasanakuBackend-campania`) recreó el stack Docker compartido desde su rama y reconstruyó `aportaya/identidad:local` encima de la imagen de este trabajo. Por decisión del usuario se esperó a que terminara (su `REPORTE.md` escrito y 45 min sin cambios). Recién ahí se recreó la base desde esta rama (`bd:reset`, humo 165 OK; `bajar`/`levantar` se excluyeron por la carrera conocida de `minio-bucket`) y se reconstruyó la imagen (mismo id `cc418e08`: build reproducible).
- Entorno: `bd/build.gradle.kts` invoca `python3` a secas, y el `ProcessBuilder` de Java en Windows solo busca `.exe`. Se creó `C:/Users/DELL/tools/py3venv` (venv con `--system-site-packages`, `python3.exe` = lanzador del venv). Es infraestructura de la máquina, no un cambio al repo.
- El arnés de Playwright bloqueó dos veces la cuenta de operador de dev (5 fallidos en 15 min); eso destapó H9.S3.M6. Los casos negativos del ingreso quedaron detrás de `NEGATIVOS=1`.
- `TaskStop` no mata el `node` de `ng serve`: dos veces quedó un huérfano en el 4200, cerrado a mano.
- Una corrida intermedia del backoffice (45 PASS) no cuenta como evidencia: el servidor había fallado al recompilar (mezcla de mayúsculas `Dell`/`DELL` en la ruta) y no se puede afirmar qué bundle sirvió. Se reinició limpio y se repitió todo.

- **2026-10-03:** se agregaron H5.S2.M1–M3 (tests de la subida y del sondeo, título) y H8.S3.M6 (goldens en Linux), que se descartó al leer `ci.yml:366-373`: las referencias son de macOS.

- **H5.S1.M1 (nuevo, PRODUCT_BUG propio):** `pantalla_registro.dart` vaciaba el estado del alta (`reiniciar()`) antes de navegar a la subida, y la pantalla de subida leía las capturas de ahí. La cuenta se creaba sin ninguna foto: es exactamente el kill-test del plan. Fix: `SeguimientoDelAlta` guarda `usuarioId` + una copia de las capturas, y la subida lee de ahí. Lo encontró el E2E en runtime, no los tests.
- **H3.S1.M1 (nuevo):** el doble `clientes/angular/identidad.ts` (de otro carril, PR #18) declaraba los estados con el nombre del miembro (`'EnRevision'`) y no con el valor del contrato (`EN_REVISION`). La cola del backoffice nunca mostraba expedientes por decidir. Se corrigieron solo los valores.
- **H8.S4.M2 (nuevo):** el botón «Tomar foto» salió del `PageView` (que recortaba su sombra); el pie se extrajo a `pie_de_capturas.dart` para respetar el tope de 200 líneas.
- Concurrencia (regla 70.1.4): el `typecheck` y los tests del backoffice corrieron con `ng serve` en modo watch levantado (ocioso, sin recompilar). No se vieron efectos.
- Para el E2E se corrió `CLAVE_DEV=… scripts/clave_dev.py`, el procedimiento documentado en el README, que pone una contraseña de desarrollo a las 11 cuentas de prueba **de la base local**.
- El arnés de Playwright inyecta `<meta name="aportaya-gateway" content="http://localhost:4200/api/v1">` solo en el navegador de prueba, porque el `index.html` de desarrollo no lo trae. El proxy temporal vive en el scratchpad, no en el repo.
- Se borraron 10 volcados `apps/movil/ui*.xml` que dejó la sesión anterior y se restauró `packages/tokens/vectores/monto.json`, que solo tenía cambios de fin de línea del build.

## Riesgos residuales

- Renovación de sesión (H10): probada con tests, falta verla en el navegador real. En producción la cookie exige HTTPS (`Secure`); en desarrollo funciona porque Chrome trata `localhost` como seguro. Si el backoffice y la API se sirven en dominios distintos, `SameSite=Strict` y `Path` deben revisarse con infraestructura.
- `packages/ui/src/monto/monto.spec.ts` vence a los 5000 ms también en la base `22c5e687` (preexistente, no tocado).

- Los goldens de `diseno_flutter` y de la app van a fallar en el job `app-goldens` (macOS) hasta regenerarlos en una Mac.
- Endpoint público `GET /usuarios/{id}/verificacion` (regla 90.6):
  - **Amenaza:** consultar el estado KYC de otra persona sabiendo su UUID.
  - **Control:** `@Publico` declarado en `UsuariosController.java:193`; la respuesta (`EstadoDeVerificacion`, `identidad.yaml:524-527`) lleva `verificacionId`, `estado`, `motivoRechazo` y las caras presentes, sin nombre, documento ni urls; el gateway aplica rate limit a `/api/v1/usuarios/**` (`rutas.yml`, 5/s, ráfaga 10).
  - **Test:** `UsuariosControllerWebTest` «GET /usuarios/{id}/verificacion — publica, sin datos personales» y `SeguridadWebTest`, en verde (`h2-tests.txt`).
  - **Riesgo residual:** quien tenga el UUID ve el estado y el motivo de rechazo. Queda pendiente de confirmar con seguridad (ambigüedad 1).
- El carnet de prueba fija el documento `1234567`: el atajo de desarrollo funciona **una sola vez por base** (la segunda alta choca con `AP-CU01-03`).

## Decisiones y ambigüedades

Supuestos tomados, cada uno con a quién confirmarlo:

1. Endpoint de estado público por `usuarioId`: aceptado por paralelismo con `subirDocumento`. Confirmar con seguridad.
2. Cambio solo en `PasanakuBackend`, no replicado en `PasanakuFrontend`. Lo decide el equipo en el PR.
3. Subida en lote después de `POST /usuarios`, en vez de inmediata como Atlas. Confirmar con producto.
4. «Traer todas las skills»: se interpretó como poner al día el PromptManager y el espejo. Las 5 skills de Atlas (Expo/Next) no se instalaron. Confirmar con Pablo.
5. «Cada botón» incluye el backoffice solo vía la primitiva `packages/ui/src/boton`.

Hallazgos preexistentes **fuera de alcance**, anotados y no tocados:

- `configuracion.dart`: `10.0.2.2` no está en el set de loopback, aunque el comentario de `cliente.dart` dice que sí (se rodeó con `HOSTS_PERMITIDOS`).
- Selector de fecha del alta: se eligió el 15 de octubre y el campo muestra «14 oct 2001» (probable corrimiento de zona horaria).
- El backend usa `AP-CU01-03` tanto para teléfono como para documento duplicado, y la app siempre dice «Ese celular ya tiene una cuenta».
- El `index.html` del backoffice no trae el meta `aportaya-gateway`: en desarrollo solo funciona contra Prism.
- La app móvil traduce `AP-CU04-04` como «Demasiados intentos…» (`apps/movil/lib/dominio/errores.dart:12`), igual de inexacto que el backoffice antes de H9.S3.M4.
- 2 tests del móvil fallan en Windows por comparar rutas con `/` fijo.

## Procesos que quedan corriendo

- Docker Desktop y los 10 contenedores `aportaya-*`: se dejan levantados a propósito, porque son el stack local de desarrollo.
- El servidor del backoffice y el emulador Android se cierran al terminar la sesión.
