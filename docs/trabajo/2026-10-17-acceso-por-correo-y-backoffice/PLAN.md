# Plan — Ingreso con correo, sin segundo factor para el usuario final, login del backoffice estilo Atlas y rediseño visual del backoffice

- Fecha: 2026-10-17 · Repos afectados: Pasanaku (monorepo) · Rama: `pablo/feat/backoffice-login-y-acceso-2026-10-17` (desde `origin/test` 8a0131c6)
- Predecesor: `2026-10-16-codigo-acceso-por-correo`, `2026-10-15-visual-bancario` (rediseño de la app móvil; el backoffice quedó OUT)
- Resultado observable:
  1. Una persona entra a la app o al backoffice escribiendo su **correo o su teléfono** y la contraseña.
  2. El **usuario final** (participante) entra con contraseña desde cualquier teléfono, sin código de segundo factor. Retiro y cambio de cuenta siguen pidiéndolo (step-up). El operador del backoffice lo sigue teniendo siempre (R-SEG-10).
  3. El login del backoffice tiene la anatomía del portal interno de Atlas (panel de marca + tarjeta), en claro y oscuro.
  4. El resto del backoffice deja de verse igual que antes: rediseño visual transversal (H6).
- Kill-test: `POST /sesiones` con `{correo, credencial}` de un participante desde una huella nueva devuelve 401/422 o `requiereFactorAdicional=true` → NO está hecho.

## Pedido literal (2026-10-17, Pablo)
1. «Sigue exactamente igual los visuales.» → aclaración: «backoffice totalmente».
2. «Necesitamos esos tipos de usuarios… con sus opciones ultra detalladas como te pasé.» → «los que habíamos dicho».
3. «Que se pueda logear también con correo. Que el portal backoffice tenga un login muchísimo más estético, fijate como está en atlas.»
4. «El usuario final no quiero que tenga segundo factor.» → «lo más recomendado pero toma en cuenta la experiencia del usuario».

## Hallazgos (fase 1)
- Login backoffice: `apps/backoffice/src/app/rutas/ingreso/pantalla-de-ingreso.ts` — tarjeta sola centrada, solo teléfono (+591), TOTP.
- Contrato: `servicios/identidad/src/main/resources/openapi/identidad.yaml:890` `EntradaAutenticacion` exige `telefonoE164`; no hay `correo`.
- CU-04 busca por teléfono: `CU04Autenticar.java` `usuarios.porTelefono(...)`; el intento ya guarda `identificador_usado` genérico (`AccesoRepositorio.registrarIntento`).
- `usuario.correo` es `UQ, NULL` (`sql/30_indices/01_identidad_usuarios.sql:13` `uq_usuario_correo`), sin normalizar (comparación exacta).
- Segundo factor: `dominio/ExigeSegundoFactor.java` → `!dispositivoConfiable || permisoExigeMfa` para no operadores. Criterio 1 de `docs/CasosDeUso/CU-04 …md:169` lo exige en dispositivo desconocido; test `CU04Test.criterio1` y `cuentaActivaSinFactorNoOmiteMfaEnDispositivoNuevo`.
- Step-up de operaciones sensibles: `CU04StepUp.java` (independiente del login) — no se toca.
- Referencia Atlas: `atlas/AtlasAdminPortal/src/features/auth/login-page.tsx` y `login-hero.tsx`.
- Roles existentes: 18 en `seeders/minimos/10-roles-y-permisos.json`. La lista «ultra detallada» de tipos de usuario **no está** en el repo, ni en `docs/`, ni en `Prompts/`, ni en las transcripciones locales.

## Alcance
- IN: contrato y CU-04 de `identidad`; `ExigeSegundoFactor` y su especificación; clientes generados de identidad; login del backoffice; login de la app móvil; rediseño visual del backoffice (H6).
- OUT: step-up (`CU04StepUp`), recuperación de contraseña, esquema de base (no hace falta columna nueva), tipos de usuario (H5, decisión de negocio).
- Ambigüedades registradas:
  - A1 «sin segundo factor» — supuesto: se elimina al **iniciar sesión** para no operadores en cualquier dispositivo; se mantiene en operaciones con `permiso.requiere_mfa` (retiro, cambio de cuenta). Motivo: recomendado y es la práctica de banca móvil (fricción solo donde se mueve plata). Confirmar con Pablo.
  - A2 Correo sin verificar como identificador — supuesto: se acepta (la contraseña es el secreto; el correo solo identifica). Si dos cuentas difieren solo en mayúsculas, el correo no identifica a nadie y se responde «credencial inválida». Confirmar con Pablo.
  - A3 «Tipos de usuario … los que habíamos dicho» — la lista no está en ningún lado accesible. `DECISION_REQUIRED`: Pablo la vuelve a pasar.

## H1 — Ingreso con correo (backend)
**CA:** Dado un usuario con correo, cuando manda `{correo, credencial}` a `POST /sesiones`, entonces entra igual que con teléfono; con un correo inexistente recibe exactamente el mismo error que con contraseña mala.
**DoD:** `./gradlew :servicios:identidad:test --tests '*CU04*'` en verde + web test del contrato.
**Estado:** HECHO — regresión de identidad 330/330 (`evidencia/H1-H2-regresion-identidad.txt`)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Contrato: `EntradaAutenticacion` acepta `correo` o `telefonoE164` (uno de los dos) | El YAML valida y el controlador rechaza ninguno/ambos con 422 | `./gradlew :servicios:identidad:webTest --tests '*Sesiones*'` | HECHO — `evidencia/H1-sesiones-web.txt` (18/18) |
| H1.S1.M2 | `UsuarioRepositorio.porCorreo` sin distinguir mayúsculas, solo si identifica a una sola cuenta | Test de integración: encuentra; mayúsculas; ambiguo → vacío | `./gradlew :servicios:identidad:integrationTest --tests '*CU04*'` | HECHO — `evidencia/H1-H2-cu04-integracion.txt` |
| H1.S1.M3 | `CU04Autenticar` resuelve por el identificador presentado | Login por correo PASS; correo inexistente = credencial inválida | idem | HECHO — idem (`ingresoConCorreo`, `correoInexistenteOAmbiguoEsCredencialInvalida`) |
| H1.S1.M4 | Regenerar clientes Angular y Dart de identidad | Generador exit 0, `correo` en el modelo | `./gradlew :servicios:identidad:generarClientes` | HECHO — `evidencia/H1-S1-M4-clientes.txt` |

## H2 — Sin segundo factor al iniciar sesión para el usuario final
**CA:** Dado un participante con o sin factor enrolado, cuando entra desde un teléfono nuevo, entonces obtiene sesión sin código; dado un operador, se le sigue exigiendo siempre.
**DoD:** `CU04Test` en verde con criterios actualizados; especificación CU-04 actualizada.
**Estado:** HECHO — regresión de identidad 330/330 (`evidencia/H1-H2-regresion-identidad.txt`)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Especificación CU-04: criterio 1 y flujo 3 cambian por pedido de Pablo (2026-10-17) | El `.md` dice el requisito nuevo | `grep -n "teléfono nuevo" "docs/CasosDeUso/CU-04 …md"` | HECHO — `evidencia/H2-S1-M1-especificacion.txt` |
| H2.S1.M2 | `ExigeSegundoFactor`: no operador → solo si la operación exige MFA | Participante en huella nueva entra; operador sigue exigido | `./gradlew :servicios:identidad:integrationTest --tests '*CU04*'` | HECHO — `evidencia/H1-H2-cu04-integracion.txt` (CU04Test 18/18, CU04CodigoPorCorreoTest 5/5) |

## H3 — Login del backoffice estilo Atlas
**CA:** Dado un operador en `/ingreso`, ve panel de marca + tarjeta «Bienvenido de vuelta», puede escribir correo o teléfono, y el paso del código sigue funcionando; en móvil el panel se oculta y la tarjeta ocupa el ancho.
**DoD:** specs del backoffice en verde · capturas 390/768/1440 en claro y oscuro, miradas.
**Estado:** HECHO (VERIFIED_FUNCTIONAL_ONLY contra el mock Prism; e2e 157/157)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Identificador único: correo o teléfono, con su validación | Spec: correo → `correo`; 8 dígitos → `telefonoE164` | `npx ng test --include='src/app/rutas/ingreso/**/*.spec.ts'` | HECHO — `evidencia/H3-S1-M1-ingreso-specs.txt` (6/6; typecheck, ng lint y verificar_frontend en 0) |
| H3.S1.M2 | Layout partido estilo Atlas con tokens, claro/oscuro, movimiento reducido | Capturas en 3 viewports × 2 temas | capturas en `evidencia/H3/` | HECHO — `evidencia/H6/despues/` (3 anchos × 2 temas + error por campo), miradas |

## H4 — App móvil: ingreso con correo
**CA:** En la pantalla de ingreso de la app se puede escribir correo o teléfono.
**DoD:** `flutter test` de identidad en verde.
**Estado:** A MEDIAS — anda: ingreso por correo o celular, 14/14 tests y goldens macOS. No anda: los 2 goldens Linux de `motivo_de_salida_en_ingreso_test` van a fallar en el CI. Falta: regenerarlos en Linux con Flutter 3.44.8 y probar en un teléfono.

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Campo identificador en el ingreso de la app | Test: correo → `correo`; teléfono → `telefonoE164` | `flutter test` de ingreso + goldens macOS + `test:front` contra línea base | HECHO — `evidencia/H4-movil.txt` (14/14; goldens del ingreso regenerados y mirados; test:front +641 −8 = línea base). Diseño: celular por omisión con teclado numérico y enlace «Entrar con mi correo» |

## H5 — Tipos de usuario
**Estado:** BLOQUEADO — `DECISION_REQUIRED`: la lista detallada no está en el repo ni en las transcripciones locales. Destraba: Pablo la vuelve a pasar. No se inventa (regla 00 §1).

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | Roles y permisos de los tipos de usuario «que habíamos dicho» en `seeders/minimos` | Cada tipo con sus opciones, tal como Pablo los definió | `python3 scripts/generar_semillas.py` + segunda corrida idempotente | BLOQUEADO — `DECISION_REQUIRED` (decisión de negocio, regla 65 §1): falta la lista; no se simula porque lo que falta es el QUÉ, no un contrato |

## H6 — Rediseño visual del backoffice
**CA:** Dado cualquier ruta del backoffice, se ve con la misma familia visual que el login nuevo (shell, tablas, tarjetas), en claro y oscuro.
**DoD:** capturas antes/después por ruta principal · specs y lint en verde.
**Estado:** A MEDIAS — anda: ingreso, shell (lateral, cabecera, Salir) y botón compartido con la identidad institucional, en claro y oscuro y en 3 anchos. No anda: tablas, tarjetas y formularios de cada pantalla siguen con su diseño (solo heredan los tokens). Falta: una pasada pantalla por pantalla (operación, cumplimiento, contabilidad, publicidad, sistemas).

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Auditoría visual con capturas de las rutas principales (antes) | Lista de hallazgos con captura | `evidencia/H6/antes/` | HECHO — 9 rutas contra el mock Prism; hallazgos: marca en texto plano, menú sin íconos ni grupos, rol como código crudo (`ADMIN_PLATAFORMA`), sin salida visible |
| H6.S1.M2 | Botón de `@aportaya/ui` con la identidad institucional ya decidida para la app (A2 de `2026-10-15-visual-bancario`): relleno plano, sin degradé, sin brillo animado, sin sombra teñida | Captura del ingreso con el botón plano | `yarn --cwd packages/ui test:front` (el botón no tiene spec propio: regresión del paquete) + captura | HECHO — 125/125 + `evidencia/H6/antes` vs `evidencia/H6/despues` |
| H6.S1.M3 | Shell: barra lateral oscura con el símbolo real de la marca (`landing/assets/img/simbolo.svg`), menú con íconos y agrupado, cabecera con el rol legible (sin código crudo) | Capturas después en claro y oscuro, 3 anchos | `evidencia/H6/despues/` + lint + specs del shell | HECHO — `evidencia/H6-S1-M3-M4-shell.txt`; capturas en `evidencia/H6/despues/` (9 rutas claro/oscuro, celular y tablet), miradas |
| H6.S1.M4 | «Salir» en el shell: `DELETE /sesion` (contrato `cerrarSesion`) y vuelta a `/ingreso`; si la red falla, igual se cierra en el navegador | Spec: llama al endpoint y cierra la sesión local | `npx ng test --include='src/app/layout/**/*.spec.ts'` | HECHO — 14/14 (incluye «si el servidor falla, igual sale») |

## Hallazgo durante la ejecución (H3)
- `packages/tokens/generado/tokens.css` no se versiona. Una copia local vieja pintaba el backoffice naranja y crema; regenerado (`yarn workspace @aportaya/tokens build`) sale verde institucional `#164A30` y fondo `#F4F6F5`. El Dockerfile del backoffice lo regenera al construir, así que lo que ve TEST depende de que la imagen se haya reconstruido.
- El `Boton` Angular conserva degradé, brillo animado y sombra teñida: es el «videojuego» que la app ya dejó. Se agrega H6.S1.M2.

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Robo de contraseña = acceso sin segundo factor | Ver datos de la cuenta | Retiro y cambio de cuenta siguen con step-up; bloqueo por intentos sigue |
| `lower(correo)` sin índice funcional | Escaneo secuencial en el ingreso | Tabla chica hoy; registrar como deuda |
