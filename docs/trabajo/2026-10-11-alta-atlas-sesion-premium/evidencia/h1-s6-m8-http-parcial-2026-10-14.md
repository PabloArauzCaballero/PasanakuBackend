# H1.S6.M8 — ciclo de renovación extremo a extremo con sesión sintética

Fecha: 2026-10-14 (UTC). Entorno local aislado; no se usaron datos ni servicios de producción.

## Recorrido y resultado

Se ejecutó `identidad` con `bootRun` en `127.0.0.1:8085`, conectado a un clúster PostgreSQL temporal en `127.0.0.1:5433` (heap de aplicación limitado a 768 MiB y un solo worker Gradle). Docker y el emulador no se iniciaron. Una cuenta y dispositivo sintéticos realizaron login HTTP 200. Con la clave RSA efímera de prueba que publicó la propia instancia en JWKS se firmó un access JWT válido con `exp` pasado; no se guardó esa clave ni ningún token en el repositorio o en archivos.

La prueba optativa integra el interceptor de producción `instalarInterceptores` y Dio real por HTTP con el servicio real, usando el access vencido y el refresh emitido por el login. Ejecución:

```powershell
$env:H1_S6_M8_BASE_URL='http://127.0.0.1:8085'
$env:H1_S6_M8_USER_ID='<UUID sintético>'
$env:H1_S6_M8_ACCESS_EXPIRADO='<JWT sintético vencido>'
$env:H1_S6_M8_REFRESH='<refresh sintético>'
flutter test test/e2e_local/cliente_refresco_backend_test.dart --reporter expanded --timeout 60s
```

Resultado literal:

```text
00:00 +0: loading .../cliente_refresco_backend_test.dart
00:00 +0: access vencido renueva contra identidad y reintenta el recurso protegido
H1_S6_M8_EXPIRED_ACCESS_STATUS=401
H1_S6_M8_CLIENT_RETRY_STATUS=200
H1_S6_M8_LOGOUT_STATUS=204
H1_S6_M8_REFRESH_AFTER_LOGOUT_STATUS=401
00:00 +1: All tests passed!
```

El test también comprueba que el token y refresh se rotan, que el recurso protegido corresponde al usuario sintético, y que el refresh recién rotado falla tras `DELETE /sesion`. `dart format` terminó sin cambios y `dart analyze --fatal-infos test/e2e_local/cliente_refresco_backend_test.dart` dio `No issues found!`. Al detener identidad se consultó PostgreSQL: `revocada_en is not null = true`, motivo `cierre solicitado`. Luego se apagó el clúster temporal con `pg_ctl stop -m fast`; los datos del clúster se conservaron.

## Alcance y límites

El recorrido del criterio está cubierto con el cliente/interceptor real, transporte HTTP, servicio identidad y PostgreSQL aislado. El login se hizo directamente contra el endpoint del servicio; no se automatizó la interfaz móvil. No se incluyeron Gateway ni Kafka: el broker no estaba disponible y el servicio registró fallos de conexión a `127.0.0.1:9092`; esto no impidió el ciclo de autenticación probado. Docker Compose era opcional según el criterio y no fue necesario. No se afirma una validación Android/iOS ni E2E visual de la aplicación.
