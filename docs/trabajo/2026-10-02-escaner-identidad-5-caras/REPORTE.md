# Reporte — Escáner de identidad (5 capturas, como Atlas) + botones vivos y transiciones (H8)

> **AVANCE: 22 / 23 — 95,7 %.** Rojo abierto: H8.S3.M5 (goldens de `diseno_flutter`) BLOQUEADO hasta regenerarlos en el runner Linux del CI. Hitos A MEDIAS: H5 (faltan los tests unitarios de `SubidaNotifier` / `VerificacionNotifier`) y H8 (por H8.S3.M5). H6 opcional sin empezar.

- Fecha: 2026-10-02 · Plan: [PLAN.md](./PLAN.md) · Rama: `justin/feature/escaner-identidad-atlas` (worktree `PasanakuBackend-escaner`, sobre `origin/test`), sin commitear.
- Peldaño de evidencia alcanzado:
  - Flujo alta → 5 fotos → backoffice → aprobar → app: **VERIFIED** (runtime real: emulador Android + 10 contenedores + backoffice con Playwright, persistencia comprobada con SQL, consola y red revisadas).
  - Botones vivos y transiciones (H8): **TESTED** + prueba visual en claro y oscuro. Los goldens quedan BLOQUEADOS.
  - Peldaño del trabajo (el más bajo de sus áreas): **TESTED**. No es REGRESSION_VERIFIED por los goldens y por los caminos no cubiertos (ver abajo).

## Completado

| ID | Qué se logró (observable) | Comando de verificación | Resultado |
|---|---|---|---|
| H1.S1.M1–M3 | `verificacion_kyc` tiene `url_perfil_izquierdo` / `url_perfil_derecho` desde el modelo `.puml` | `generar_ddl.py` · `verificar_boveda.py` · `bd:reset` | PASS — `evidencia/h1-s1-m1-m2-ddl-boveda.txt`, `h1-s1-m3-bd-reset.txt` |
| H2.S1.M1–M7 | Contrato e identidad aceptan 5 caras; `GET /usuarios/{id}/verificacion` público sin datos personales | `./gradlew :servicios:identidad:test webTest integrationTest --continue` | PASS — BUILD SUCCESSFUL in 2m 27s (`evidencia/h2-tests.txt`) |
| H3 / H3.S1.M1 | El backoffice tipa 5 caras y la cola muestra los expedientes `EN_REVISION` (doble del cliente con los valores del contrato) | backoffice `typecheck`, `lint`, `test:front`, `test:a11y` | PASS — exit 0 · lint OK · 354/354 · 34/34 (`evidencia/h3-s1-m1-estados-gate.txt`) |
| H4 | Paso de capturas de 5 caras en Flutter (cámara, calidad, consejos, carnet de prueba) | `flutter analyze` · `flutter test …` · `verificar_frontend.py movil` | PASS — sin issues · 263 passed / 2 failed preexistentes de Windows · gate sin fallas (`evidencia/cierre-movil-regresion.txt`) |
| H5.S1.M1 | **Bug corregido:** las 5 fotos ahora se suben después de crear la cuenta | `flutter test test/unidad/seguimiento_del_alta_test.dart` + corrida en el emulador + SQL | PASS — 3/3 · 5 × `POST …/documentos` 201 · usuario nuevo con 5 urls (`evidencia/h5-s1-m1-*.txt`) |
| H7 | E2E de punta a punta con el stack real | emulador + Playwright + SQL | PASS — `APROBADA`, `revisada_por` y 5 fotos en base; la app muestra «Identidad verificada» (`evidencia/h7-e2e-aprobacion.txt`, `h7-bo-0*.png`, `h8-14/15*.png`) |
| H8.S1.M1 | PromptManager al día; espejo de skills sin deriva | `git status -sb` · `comm` | PASS (ver PLAN) |
| H8.S2.M1–M2 | Transición de página lateral con fundido, sin zoom; transición de marca sobria de 420 ms | test de transición | PASS — `evidencia/h8-diseno-flutter.txt` |
| H8.S3.M1–M4 | Tokens `Movimiento`; `Boton`/`BotonIcono`/`BotonFlotante` vivos (degradado, sombra teñida, resorte, brillo, respeta reducir movimiento); primitiva Angular | tests de widget · gate del backoffice | PASS — `evidencia/h8-diseno-flutter.txt`, `h3-h8-backoffice-gate.txt` |
| H8.S4.M1 | Flujo de capturas premium: riel de 5 caras, tarjeta con marco de encuadre, sin Anterior/Siguiente | capturas del emulador inspeccionadas | PASS — `h8-05-capturas-boton-fuera.png`, `h8-06-capturas-completas.png` |
| H8.S4.M2 | La sombra de «Tomar foto» ya no aparece cortada | `flutter analyze` · gate de diseño · captura antes/después | PASS — `h8-04-capturas-vacio.png` (cortada) vs `h8-05-…` (completa) |
| H8.S5.M1 | Prueba visual en claro y oscuro (app y backoffice) | capturas inspeccionadas | PASS — `h8-01`, `h8-12-subida-1` (botón en carga), `h8-16/17-*-oscuro`, `h7-bo-01-cola-dark.png` |

## A medias

### H5 — Subida en lote y pantalla de estado
- **Qué anda:** después de corregir H5.S1.M1, la subida en lote funciona en runtime: 5 × `POST /api/v1/usuarios/<id>/documentos` → 201, y las 5 urls quedan en `verificacion_kyc` / `documento_identidad`. La pantalla de estado hace polling y pasa de «Lo está revisando una persona» a «Identidad verificada» tras la aprobación.
- **Qué no anda / no está probado:** no existen tests unitarios de `SubidaNotifier` (aviso «está tardando» a los 10 s, cancelar, reintentar con la misma clave de idempotencia, repetir con la clave rotada) ni de `VerificacionNotifier`. Esos caminos no se ejercitaron en runtime: todas las subidas salieron bien al primer intento. La pantalla de subida no tiene título; arriba queda una barra vacía (`h8-12-subida-8.png`).
- **Qué falta exactamente:** (1) `test/unidad/subida_del_expediente_test.dart` con `http_mock_adapter`: 201, demora mayor a 10 s → `lenta`, cancelar → `pendiente`, 5xx → `fallida` + reintentar con la misma clave, repetir → clave nueva; (2) lo mismo para el polling de `estado_de_verificacion.dart`; (3) un título en `PantallaDeSubidaDelExpediente` (texto en `textos_de_subida.dart`).
- **Dónde quedó:** `apps/movil/lib/pantallas/identidad/{dominio/subida_del_expediente.dart, dominio/estado_de_verificacion.dart, pantalla_subida_del_expediente.dart}`. Compila y analyze no reporta issues.

### H8 — Extensión de diseño
- **Qué anda:** todo menos H8.S3.M5 (ver Completado).
- **Qué no anda:** las referencias golden de `packages/diseno_flutter/test/goldens/imagenes/` siguen siendo las de la piel vieja, así que en el CI esos goldens van a fallar hasta regenerarlos.
- **Qué falta exactamente:** en el runner Linux, `cd packages/diseno_flutter && flutter test --update-goldens test/goldens`, y versionar las imágenes.
- **Dónde quedó:** sin cambios en `test/goldens/` (la regeneración de prueba en Windows se restauró).

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H8.S3.M5 | BLOQUEADO | Regenerar los goldens en el runner Linux del CI (dueño: quien mantenga `diseno_flutter`). Simulado con Windows como doble en los tres niveles (simulacion-65): INVALIDO 15/18 fallan contra las referencias viejas (el cambio se detecta), ACEPTADO 18/18 tras regenerar, LIMITE 18/18 en una segunda corrida (render determinista). Evidencia: `evidencia/h8-s3-m5-goldens-simulacion.txt` |
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

- Caminos de error de la subida (lenta, cancelar, reintentar, repetir) y del polling: solo se ejerció el camino feliz.
- Rechazo desde el backoffice y su reflejo en la app («Rechazar» con motivo): no se ejercitó; solo se aprobó.
- La cámara real del emulador y el chequeo de calidad con fotos reales: las 5 capturas del E2E salieron del **carnet de prueba sintético** (atajo de desarrollo).
- iOS (gesto nativo de volver con `TransicionDeEje`): sin simulador en esta máquina.
- Viewport tablet del backoffice y la app en tablet: solo se miró móvil (1080×2400) y escritorio (1440×900).
- Lector de pantalla en las pantallas nuevas: solo los tests automáticos de a11y del backoffice.
- Regla 98.8 (caída del servicio dependido, traza con `correlationId`): no se ejercitó apagar identidad durante la subida.

## Desvíos del plan

- **H5.S1.M1 (nuevo, PRODUCT_BUG propio):** `pantalla_registro.dart` vaciaba el estado del alta (`reiniciar()`) antes de navegar a la subida, y la pantalla de subida leía las capturas de ahí. La cuenta se creaba sin ninguna foto: es exactamente el kill-test del plan. Fix: `SeguimientoDelAlta` guarda `usuarioId` + una copia de las capturas, y la subida lee de ahí. Lo encontró el E2E en runtime, no los tests.
- **H3.S1.M1 (nuevo):** el doble `clientes/angular/identidad.ts` (de otro carril, PR #18) declaraba los estados con el nombre del miembro (`'EnRevision'`) y no con el valor del contrato (`EN_REVISION`). La cola del backoffice nunca mostraba expedientes por decidir. Se corrigieron solo los valores.
- **H8.S4.M2 (nuevo):** el botón «Tomar foto» salió del `PageView` (que recortaba su sombra); el pie se extrajo a `pie_de_capturas.dart` para respetar el tope de 200 líneas.
- Concurrencia (regla 70.1.4): el `typecheck` y los tests del backoffice corrieron con `ng serve` en modo watch levantado (ocioso, sin recompilar). No se vieron efectos.
- Para el E2E se corrió `CLAVE_DEV=… scripts/clave_dev.py`, el procedimiento documentado en el README, que pone una contraseña de desarrollo a las 11 cuentas de prueba **de la base local**.
- El arnés de Playwright inyecta `<meta name="aportaya-gateway" content="http://localhost:4200/api/v1">` solo en el navegador de prueba, porque el `index.html` de desarrollo no lo trae. El proxy temporal vive en el scratchpad, no en el repo.
- Se borraron 10 volcados `apps/movil/ui*.xml` que dejó la sesión anterior y se restauró `packages/tokens/vectores/monto.json`, que solo tenía cambios de fin de línea del build.

## Riesgos residuales

- Los goldens de `diseno_flutter` van a fallar en el CI hasta regenerarlos en Linux.
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
- Backoffice en oscuro: el ítem activo del menú lateral es una pastilla clara con texto verde claro, de bajo contraste (`h7-bo-01-cola-dark.png`).
- 2 tests del móvil fallan en Windows por comparar rutas con `/` fijo.

## Procesos que quedan corriendo

- Docker Desktop y los 10 contenedores `aportaya-*`: se dejan levantados a propósito, porque son el stack local de desarrollo.
- El servidor del backoffice (`ng serve`, puerto 4200) y el emulador Android se cierran al terminar esta sesión.
