# H10.S2.M1–M6 — recorridos Android bloqueados por recursos locales

Fecha: 2026-10-13. No se ejecutaron los recorridos ni se inventaron capturas, videos o logs.

## Evidencia del entorno

- `flutter devices`: solo `Windows (desktop)` y `Chrome (web)` conectados; no hay Android conectado.
- `adb devices -l`: muestra únicamente `List of devices attached`, sin dispositivos.
- `flutter emulators`: existen `Pixel_2` y `Samsung_A06-L`, pero cada AVD tiene `hw.ramSize=2048 MB`.
- Memoria al verificar: **954 MB libres de 15.691 MB**. No inicié un AVD de 2 GB para respetar el límite de RAM solicitado y evitar presión/OOM.

## Tareas afectadas

| ID | Requisito real | Bloqueo actual |
|---|---|---|
| H10.S2.M1 | Android, instalación limpia y redirección/login | No hay dispositivo Android disponible para instalar y observar la app. |
| H10.S2.M2 | Alta completa, correo, cámara, prueba de vida, contrato y subida; ≥25 capturas | Requiere Android/cámara y servicios/cuenta de prueba; no se inició con el entorno actual. |
| H10.S2.M3 | Sesión viva 20 min y evidencia de `/sesion/refrescar` 200 | Sin dispositivo ejecutando la app ni backend de prueba conectado. |
| H10.S2.M4 | Candado por salida, retorno y 5 min de inactividad | Requiere ciclo de vida de Android real/emulado; no se sustituyó con una prueba widget. |
| H10.S2.M5 | Logout y cambio entre dos cuentas sin filtración | Requiere ejecución Android y dos cuentas de prueba/backend. |
| H10.S2.M6 | Selector con teclado virtual Android y ≥3 filas visibles | Requiere teclado/ventana Android; la geometría simulada no demuestra el comportamiento del IME real. |

## Reanudación

Conectar un Android o liberar RAM suficiente para iniciar un AVD respetando el presupuesto local; confirmar backend y cuentas sintéticas de prueba. Ejecutar cada recorrido de `PLAN.md`, conservar las capturas/videos solicitados, filtrar `flutter logs` y revisar la red. Entonces reemplazar estos bloqueos con evidencia de ejecución; hasta ese momento H10.S2.M1–M6 permanecen `BLOQUEADO`.

## Reintento del AVD con límite explícito de RAM — 2026-10-13

- Se apagó primero el PostgreSQL temporal; el puerto 5433 quedó cerrado. No había otros emuladores ni dispositivo ADB.
- AVD `Samsung_A06-L` tiene `hw.ramSize=2048 MB`. Se lanzó sin ventana y sin restaurar snapshot, primero con `-memory 1024`; la RAM física libre descendió de 1.517 MB a 278 MB antes de completar el arranque. Se envió `adb emu kill` al cruzar el umbral de 450 MB.
- Segundo intento con `-memory 768` y umbral de corte 550 MB: descendió a 336 MB antes de completar el arranque y se detuvo. Después se confirmó `adb devices` vacío, cero procesos emulator/QEMU y 1.709 MB libres.

Resultado: no se obtuvo Android ejecutable sin cruzar el límite de memoria acordado; H10.S2.M1–M6 siguen `BLOQUEADO`. No se inició Docker ni se dejó ningún servicio/emulador en segundo plano.

## Revalidación parcial y precondiciones — 2026-10-14

- H10.S2.M1: con un build debug válido y Samsung_A06-L disponible, se probó instalación/almacenamiento limpios, deep link frío a `aportaya://billetera` e intent caliente `aportaya://notificaciones`. Ambos mostraron el formulario de ingreso en el árbol UI, sin billetera ni bandeja. El resultado parcial y la evidencia se registran en [H10.S2.M1 AVD](./h10-s2-m1-avd-2026-10-14.md); sigue A MEDIAS por falta de una notificación FCM real verificable, no por requerir teléfono físico (el usuario indicó expresamente usar AVD).
- H10.S2.M6 no puede alcanzar de forma fiel “Expedido en” ni “Actividad económica” sin superar antes la confirmación de correo: `PantallaDeRegistro` presenta `PasoAlta.cuenta`, luego `PasoAlta.correo`, y solo después `PasoAlta.documento`/`PasoAlta.actividad`; `AltaNotifier.solicitarCodigoCorreo` y `confirmarCodigoCorreo` llaman a `verificacionCorreoProvider`, que valida el OTP en servidor. La API de prueba `10.0.2.2:4010` no estaba disponible en la revalidación. Un OTP inventado o un bypass del notifier no representaría el recorrido de producto pedido. M6 continúa bloqueada por backend OTP y falta de verificación del IME en AVD; no se requiere teléfono físico.
- El AVD, ADB y Gradle se apagaron después de la revalidación de M1; no se dejó ningún recurso virtual encendido.

## Reintento AVD y PostgreSQL local — 2026-10-15

- Estado inicial: Docker Desktop ya estaba activo; RAM 13,20/15,32 GiB, ADB sin dispositivos y sin contenedores en ejecución. El contenedor existente `aportaya-postgres` y su volumen local `aportaya-datos-pg` se conservaron; solo se inició el servicio `postgres` (no Kafka ni el stack completo). Salud `healthy`, `pg_isready` aceptó conexiones y el uso del contenedor fue 65,51 MiB. Se verificó el inventario de esquemas con una consulta de solo lectura; no se ejecutaron migraciones ni cambios de datos. Luego se detuvo con `docker compose ... stop postgres`.
- Se intentó iniciar `Samsung_A06-L` con `-memory 768 -no-window -no-audio -no-snapshot-load -no-boot-anim -gpu swiftshader_indirect`. ADB lo detectó como `emulator-5554`, pero `sys.boot_completed` no llegó a `1`. La RAM subió hasta 15,08/15,32 GiB, por debajo pero muy cerca del máximo acordado de 15,1 GiB; se envió `adb emu kill` y se detuvo el proceso. No se instaló ni ejecutó la aplicación, por lo que no cuenta como recorrido PASS.
- Estado tras el reintento: `adb devices` vacío, ningún proceso QEMU/emulator, PostgreSQL detenido y ningún contenedor activo. Docker Desktop se apagó al dejar de necesitarlo; tras la suite de pruebas, la última memoria medida fue 10,38/15,32 GiB. H10.S2.M1–M6 siguen BLOQUEADO salvo la evidencia parcial ya documentada para M1; este intento no reemplaza ni amplía esa prueba.
