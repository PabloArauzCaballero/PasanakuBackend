# H2.S1.M1 — reproducción de acceso a billetera en AVD (2026-10-15)

## Resultado observado

En el AVD `Samsung_A06-L`, tras ingresar a la app debug con la cuenta existente autorizada por el usuario, se llegó a `Tu billetera` y se mostró **«No tenés acceso a esto»** junto con **«Volver a intentar»**, el mismo texto que aparece en las capturas del teléfono físico.

- Cuenta: identificador omitido parcialmente (`67****16`). Contraseña no guardada, copiada a órdenes ni escrita en evidencia.
- APK: compilado localmente desde la rama de trabajo, `debug`; `applicationId=bo.aportaya.aportaya_movil`.
- Backend compilado en el APK: `https://api.aportaya.161.97.85.216.sslip.io/api/v1` (el host de la sonda registrada en `h2-capturas-no-acceso-telefono-real-2026-10-15.md`). La sonda al endpoint público `/actuator/health` había devuelto `UP`.
- Traza visible en la UI: `dc9563f5-e681-3d11-b0d6-3b2f50cb0644`.
- Comprobación UI adicional: `uiautomator` mostró `Tu billetera`, `No tenés acceso a esto` y `Volver a intentar`; también estaban las pestañas autenticadas `Inicio` y `Movimientos`.

## Comandos no sensibles usados

Desde `apps/movil`:

```powershell
flutter build apk --debug --target=lib/main.dart --dart-define=API=https://api.aportaya.161.97.85.216.sslip.io/api/v1 --dart-define=HOSTS_PERMITIDOS=api.aportaya.161.97.85.216.sslip.io --no-pub
```

Instalación sobre AVD `Samsung_A06-L` (arrancado con 512 MiB):

```powershell
adb -s emulator-5554 install -r apps/movil/build/app/outputs/flutter-apk/app-debug.apk
```

La navegación y entrada se realizaron mediante la UI del emulador; no se colocó la contraseña en la línea de comandos.

## Límite de lo que demuestra

La reproducción confirma el texto y la superficie de billetera para esta cuenta, en esta build y contra ese host. No se capturó status/cuerpo HTTP de la petición ni se correlacionó la traza con logs del backend. El código local mapea HTTP 403 a este texto (`apps/movil/lib/dominio/errores.dart`), así que 403 es la explicación consistente con el contrato del cliente, pero no es una medición directa de la respuesta de esta ejecución. No se confirmó el permiso `BILLETERA_VER` de la cuenta. Tampoco se verificó Extracto ni creación de grupo, ni se pudo probar que el host desplegara el mismo SHA que atendió las capturas físicas.

No se conservó una captura de pantalla de la sesión para no guardar datos privados. Tras la observación se ejecutó `adb shell pm clear bo.aportaya.aportaya_movil` (`Success`), se borraron los archivos temporales creados y se apagó el emulador. Última medición: 11,57 GiB usados / 3,76 GiB libres; ADB sin dispositivos, sin `emulator` ni `qemu` activos.
