# Reporte — Ingreso con correo, sin segundo factor para el usuario final, login del backoffice estilo Atlas y rediseño visual del backoffice

> **AVANCE: 13 / 14 — 92,9 %.** Hitos H4 y H6 A MEDIAS. H5 (tipos de usuario) queda `BLOQUEADO` por decisión de negocio: la lista detallada no está en ningún lado accesible. Riesgo abierto: 2 goldens Linux del ingreso móvil van a fallar en el CI hasta regenerarlos en Linux.

- Fecha: 2026-10-17 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/feat/backoffice-login-y-acceso-2026-10-17` (desde `origin/test` 8a0131c6), subida a `origin`.
- Peldaño de evidencia alcanzado:
  - Backend `identidad`: `TESTED` (tests dirigidos contra PostgreSQL real + web test del contrato). Regresión completa del servicio: ver «Evidencia».
  - Backoffice: `VERIFIED_FUNCTIONAL_ONLY` + prueba visual (capturas miradas en 3 anchos × 2 temas). La regresión e2e (157/157) corrió contra el **mock Prism**, no contra el backend real.
  - App móvil: `TESTED` (tests de widget y goldens macOS). No se probó en un teléfono.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `POST /sesiones` acepta `correo` o `telefonoE164` (exactamente uno; si no, 400 AP-VAL-02) | `./gradlew :servicios:identidad:webTest --tests '*SesionesControllerWebTest'` | PASS 18/18 · `evidencia/H1-sesiones-web.txt` |
| H1.S1.M2 | `UsuarioRepositorio.porCorreo`: sin importar mayúsculas; si dos cuentas comparten el correo salvo mayúsculas, no identifica a nadie | `./gradlew :servicios:identidad:integrationTest --tests '*CU04Test'` | PASS 18/18 · `evidencia/H1-H2-cu04-integracion.txt` |
| H1.S1.M3 | CU-04 entra por correo; un correo inexistente responde igual que una contraseña mala (AP-CU04-01) | idem | PASS (`ingresoConCorreo`, `correoInexistenteOAmbiguoEsCredencialInvalida`) |
| H1.S1.M4 | Clientes Angular y Dart con `correo` | `./gradlew :servicios:identidad:generarClientes` | PASS · `evidencia/H1-S1-M4-clientes.txt` |
| H2.S1.M1 | Especificación CU-04 con el requisito nuevo | `grep -n "teléfono nuevo" docs/CasosDeUso/CU-04…` | PASS · `evidencia/H2-S1-M1-especificacion.txt` |
| H2.S1.M2 | El participante entra sin código desde cualquier teléfono; las operaciones con `requiere_mfa` lo siguen pidiendo; el operador, siempre | `integrationTest --tests '*CU04Test' --tests '*CU04CodigoPorCorreoTest'` | PASS 18/18 + 5/5 |
| H3.S1.M1 | Backoffice: un solo campo «Correo o teléfono» (`leerIdentificador`) | `npx ng test --include='src/app/rutas/ingreso/**/*.spec.ts'` | PASS 6/6 · `evidencia/H3-S1-M1-ingreso-specs.txt` |
| H3.S1.M2 | Login del backoffice con la anatomía del portal interno de Atlas (panel de marca + tarjeta «Bienvenido de vuelta»), con el símbolo real de la marca | capturas | PASS · `evidencia/H6/despues/ingreso-*` (celular, tablet, escritorio × claro/oscuro + error por campo) |
| H4.S1.M1 | App: celular por omisión (teclado numérico, +591) y enlace «Entrar con mi correo» | `flutter test` del ingreso + goldens macOS | PASS 14/14 · `evidencia/H4-movil.txt` |
| H6.S1.M1 | Auditoría visual de 9 rutas (antes) | capturas | `evidencia/H6/antes/` |
| H6.S1.M2 | `@aportaya/ui` Boton: plano, sin degradé, sin brillo animado, sin sombra teñida | `yarn --cwd packages/ui test:front` | PASS 125/125 · `evidencia/H6-S1-M2-boton.txt` |
| H6.S1.M3 | Shell: barra lateral oscura con el símbolo, menú agrupado con íconos, rol legible; en el celular, una fila que se desliza | specs + capturas | PASS · `evidencia/H6-S1-M3-M4-shell.txt`, `evidencia/H6/despues/` |
| H6.S1.M4 | «Salir»: `DELETE /sesion` y vuelta a `/ingreso`; si la red falla, igual sale | `npx ng test --include='src/app/layout/**/*.spec.ts'` | PASS 14/14 |

## A medias
Todas las microtareas están HECHO. Hay dos **hitos** que quedan a medias:

### H4 — App móvil: ingreso con correo
- Qué anda: el ingreso acepta celular (por omisión) o correo (enlace «Entrar con mi correo»); 14/14 tests dirigidos y los goldens macOS del ingreso regenerados y mirados.
- Qué no anda: los 2 goldens Linux de `motivo_de_salida_en_ingreso_test` (`h1-ingreso-*.png`) no reflejan el enlace nuevo; el job `test:front` del CI (Linux) va a fallar en ellos.
- Qué falta exactamente: regenerar esos 2 PNG en Linux/amd64 con Flutter 3.44.8 (como en 342664c4) y probar el ingreso en un teléfono.
- Dónde quedó: `apps/movil/lib/pantallas/identidad/{campos_de_sesion,pantalla_sesion}.dart`, `dominio/{cu04_autenticar,estado_sesion}.dart`; rama `pablo/feat/backoffice-login-y-acceso-2026-10-17`; compila (`flutter analyze`: sin problemas).

### H6 — Rediseño visual del backoffice
- Qué anda: el ingreso estilo Atlas, el shell (barra lateral con la marca, menú agrupado con íconos, rol legible, Salir) y el botón compartido plano e institucional, en claro y oscuro, en celular, tablet y escritorio.
- Qué no anda: el contenido de cada pantalla (tablas, tarjetas, formularios) mantiene su diseño anterior; solo hereda los colores institucionales.
- Qué falta exactamente: una pasada pantalla por pantalla —operación, cumplimiento, contabilidad, publicidad y sistemas—, cada una con capturas antes/después.
- Dónde quedó: `apps/backoffice/src/app/layout/*`, `apps/backoffice/src/app/rutas/ingreso/*`, `packages/ui/src/boton/boton.ts`; compila, typecheck y lint en 0.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H5.S1.M1 — tipos de usuario «que habíamos dicho» | BLOQUEADO (`DECISION_REQUIRED`) | Que Pablo vuelva a pasar la lista. No está en el repo, ni en `docs/`, ni en `Prompts/`, ni en las conversaciones de esta máquina (posiblemente se pasó desde otra de sus máquinas). Hoy existen 18 roles en `seeders/minimos/10-roles-y-permisos.json`. |

## Evidencia
```text
$ TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :servicios:identidad:integrationTest --tests "*CU04Test" --tests "*CU04CodigoPorCorreoTest"
BUILD SUCCESSFUL in 22s
testsuite CU04CodigoPorCorreoTest tests="5" failures="0" errors="0"
testsuite CU04Test tests="18" failures="0" errors="0"

$ ./gradlew :servicios:identidad:webTest --tests "*SesionesControllerWebTest"
BUILD SUCCESSFUL · tests="18" failures="0"

$ yarn --cwd apps/backoffice test:front
 Test Files  125 passed (125)
      Tests  832 passed (832)

$ npx playwright test --workers=1        (apps/backoffice, contra el mock Prism)
  157 passed (2.7m)

$ yarn --cwd packages/ui test:front
 Test Files  23 passed (23)
      Tests  125 passed (125)

$ flutter test test/unidad test/widget test/contrato test/identidad test/pasanaku   (apps/movil)
+641 -8   ← los 8 fallos son goldens comparados en Linux y fallan IGUAL sin estos cambios (línea base medida con stash)
```
Regresión completa de `identidad` (`test`, `webTest`, `integrationTest`): ver `evidencia/H1-H2-regresion-identidad.txt`.

## No cubierto
- **Backend real del backoffice:** los e2e y las capturas de pantallas internas corrieron contra el mock Prism, con un doble de sesión. No se entró al backoffice desplegado en TEST.
- **Teléfono real:** el ingreso con correo de la app no se probó en un dispositivo.
- **Goldens Linux del ingreso móvil** (`docs/trabajo/2026-10-11-alta-atlas-sesion-premium/evidencia/h1-ingreso-*.png`, de `motivo_de_salida_en_ingreso_test`): no se regeneraron porque hace falta un Linux con Flutter 3.44.8, y la imagen `ghcr.io/cirruslabs/flutter:3.44.8` no existe con ese tag. El job `test:front` del CI (Linux) va a fallar en esos 2 hasta que se regeneren.
- **Rediseño del contenido de cada pantalla interna:** se rediseñaron el ingreso, el shell (lateral + cabecera) y el botón compartido, y todas las pantallas heredan los tokens institucionales. Tablas, tarjetas y formularios de cada pantalla **no** se rediseñaron uno por uno.
- **Aviso de «nuevo ingreso»** por correo al entrar desde un teléfono nuevo: sigue saliendo el evento `identidad.sesion_iniciada`, pero no se verificó que alguien lo notifique.

## Desvíos del plan
- **H6.S1.M2** decía «specs del botón», pero el botón no tiene spec propio: se corrió la regresión completa de `@aportaya/ui`.
- **H6.S1.M4** (Salir) se agregó durante la ejecución: en el backoffice no había forma visible de cerrar sesión.
- Se descartaron las capturas tomadas con tokens locales viejos (naranja y crema); las vigentes son las de `evidencia/H6/despues/`.
- El login del backoffice usa el símbolo real de la marca (`landing/assets/img/simbolo.svg`) en lugar del monograma «A» del primer intento.

## Riesgos residuales
- **Sin segundo factor al entrar:** quien obtiene la contraseña de un participante ve su cuenta. Mitigación vigente: retiro y cambio de cuenta siguen pidiendo step-up (`requiere_mfa`), y el bloqueo por intentos sigue activo.
- **`lower(correo)` sin índice funcional:** el ingreso por correo hace un escaneo secuencial de `identidad.usuario`. Hoy la tabla es chica; con volumen hace falta `CREATE INDEX … ON identidad.usuario (lower(correo))` por el mecanismo del esquema.
- **Tokens generados no versionados:** `packages/tokens/generado/tokens.css` y los clientes generados son locales. Una copia vieja pintaba el backoffice de naranja y crema, y Gradle daba los clientes por al día cuando no lo estaban (61 errores de typecheck que se resolvieron con `generarClientes --rerun-tasks`). Lo que ve TEST depende de que la imagen se reconstruya.
- **CI:** los 2 goldens Linux del ingreso móvil (ver «No cubierto»).

## Decisiones y ambigüedades
- **A1 · «sin segundo factor»** — supuesto: se quita al **iniciar sesión** para el participante en cualquier teléfono y se mantiene en operaciones con `permiso.requiere_mfa` (retiro, cambio de cuenta), que es lo recomendado y cuida la experiencia. El operador lo sigue teniendo siempre (R-SEG-10). **Confirmar con Pablo.**
- **A2 · correo sin verificar como identificador** — se acepta, porque la contraseña es el secreto. Si dos cuentas comparten el correo salvo mayúsculas, se responde «credencial inválida». **Confirmar con Pablo.**
- **A3 · tipos de usuario** — `DECISION_REQUIRED`, ver «Pendiente».
- **Código por correo (2026-10-16):** con la regla nueva solo sale en la operación sensible, no al entrar. Sus 5 tests pasaron a ejercitar ese camino; no se borró ninguno.
- **Rol legible:** se muestra el código del rol convertido a texto (`ADMIN_PLATAFORMA` → «Administración de plataforma») en vez de copiar al frontend el catálogo de nombres de las semillas (sería un catálogo paralelo).
- **App móvil:** se mantuvo el celular por omisión, con su teclado numérico, y el correo quedó detrás de un enlace, para no castigar a la mayoría que entra con el celular.
