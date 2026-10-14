# H2 — Inicio de sesión con aprobación manual pendiente

Fecha: 2026-10-14. Validación de caso de uso contra PostgreSQL local persistente. No se usó teléfono físico ni se necesitó un rostro.

## Resultado

Se ajustó CU-04 y se añadieron dos regresiones. Una crea un usuario de ensayo con credencial válida, lo deja en `PENDIENTE_VERIFICACION` / `NINGUNO` y autentica desde un dispositivo nuevo, sin factor aún enrolado. Verifica que:

- el resultado de autenticación sea exitoso;
- se cree una sesión;
- el dispositivo quede no confiable;
- el estado de la cuenta continúe siendo `PENDIENTE_VERIFICACION`.

La segunda prueba confirma que un usuario `ACTIVO` sin factor en dispositivo nuevo continúa recibiendo `AP-CU04-03` y no obtiene sesión. La excepción se limita a cuenta pendiente, no operadora, sin factor enrolado y autenticación no sensible. Si ya hay factor, MFA conserva su política existente. No se altera el estado ni se marca aprobada la cuenta.

Comando ejecutado: `.\gradlew --no-daemon --max-workers=1 "-Dorg.gradle.jvmargs=-Xmx512m -XX:MaxMetaspaceSize=256m" :servicios:identidad:integrationTest --tests 'bo.aportaya.identidad.CU04Test.cuentaPendientePuedeIniciarSesionSinAutoaprobarse' --tests 'bo.aportaya.identidad.CU04Test.cuentaActivaSinFactorNoOmiteMfaEnDispositivoNuevo'`.

Resultado: **BUILD SUCCESSFUL**, 2/2 tests.

## Límites y estado

Esto prueba la regla de backend y no equivale al recorrido Flutter completo de H2.S1.M1. Se conserva MFA para cuentas activas y para operadores; también para cuentas pendientes que ya tengan un factor enrolado o que pidan una operación sensible. CU-04 no cambia estado ni asigna roles. No demuestra alta, gateway, envío de notificaciones, caducidad de sesión ni autorización de operaciones financieras.

Una ejecución exploratoria de toda `CU04Test` en la base persistente completó 14 casos, pero 9 chocaron con números de teléfono fijos ya existentes (`uq_usuario_telefono_e164`). No se limpiaron tablas ni se borró ningún registro. El test añadido usa un teléfono aleatorio para ser repetible sobre esta base. Los registros de ensayo creados durante estas ejecuciones se conservaron.

No se cambió la lógica de aprobación ni se marcó aprobada ninguna cuenta. H2.S1.M1 permanece `A MEDIAS`; el contador global no cambia por esta comprobación parcial. Para las validaciones móviles se usará AVD/emulador. Quedan pendientes los ensayos que requieren rostro real.
