# Plan — Continuar un registro a medias desde el login

- Fecha: 2026-10-30 · Repos: Pasanaku (`apps/movil`) · Rama: `claude/continuar-alta-desde-login-20261030` (base `origin/test`)
- Predecesor: `docs/trabajo/2026-10-30-pantallas-y-testflight/` (cerrado).
- Resultado observable: quien dejó el alta a medias toca en el login «Continuar mi registro», escribe el mismo correo, confirma un código y la app lo lleva al paso donde quedó con lo que ya había cargado.
- Kill-test: desde el login no hay forma de llegar al alta en modo continuar, o tras confirmar el código la app no salta al paso pendiente.

## Pedido literal
«necesito que si deje un formulario de creacion pendiente pueda acceder a continuar el mismo desde el login».

## Hallazgos
- El servidor ya guarda el borrador (`guardarBorradorOnboarding`) y lo devuelve al confirmar **cualquier** código de correo del mismo correo (`servicios/identidad/.../infraestructura/BorradorOnboardingRepositorio.java:41-70`).
- La app lo restaura en `AltaNotifier.confirmarCodigoCorreo` (`apps/movil/lib/pantallas/identidad/dominio/notifier_alta.dart:66-69`), que salta al `pasoPendiente`.
- ~~Defecto: una verificación ya confirmada en el teléfono saltaría la confirmación~~ **Refutado por test**: al escribir el correo, `AltaNotifier.actualizarDatos` olvida lo guardado (`dominio/notifier_alta.dart:21-26`), así que siempre se pide un código nuevo y el borrador vuelve. El mecanismo de retomar ya funcionaba entrando por «Crear mi cuenta»; lo que faltaba es la entrada desde el login y decirle a la persona qué hacer.
- El login (`sesion_pie.dart`) solo ofrece «Crear mi cuenta».
- No hay operación GET del borrador en el contrato: recuperarlo exige confirmar un código.

## Alcance
- IN: enlace en el login; `/registro?continuar=1`; en ese modo el paso 1 avisa qué hacer y pide código nuevo (`nueva: true`).
- OUT: backend y contrato; cambiar el comportamiento del alta normal; recuperar la contraseña (no se guarda en el borrador, a propósito).
- Ambigüedades: la contraseña se vuelve a elegir al retomar (el borrador no la guarda). Supuesto: aceptable porque la cuenta todavía no existe. Confirmar con Pablo.

## H1 — Continuar el alta desde el login
**CA:** Dado un borrador en el servidor, cuando la persona toca «Continuar mi registro» en el login y confirma el código de su correo, entonces el alta queda en el paso pendiente con los datos guardados.
**DoD:** tests de widget dirigidos en verde · `flutter analyze` limpio · verificador de 200 líneas OK · regresión sin fallos nuevos.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Enlace «Continuar mi registro» en el login → `/registro?continuar=1` | tocarlo navega a esa ruta | test widget `continuar_alta_test.dart` | HECHO |
| H1.S1.M2 | Modo continuar en el paso 1: aviso + código nuevo | con `continuar`, `solicitar` se llama con `nueva: true` y se ve el aviso | test widget | HECHO |
| H1.S1.M3 | Restauración tras confirmar salta al paso pendiente | confirmar con borrador `pasoPendiente: datos` deja el alta en `datos` | test (doble del servidor: correcto, borrador vacío, error) | HECHO |
| H1.S1.M4 | Regresión | sin fallos nuevos vs árbol limpio | `flutter test …` | HECHO (`evidencia/regresion.txt`) |
| H1.S1.M5 | PRs a test y dev + TestFlight | build aceptado | `xcodebuild -exportArchive` → `Upload succeeded` | EN CURSO |
