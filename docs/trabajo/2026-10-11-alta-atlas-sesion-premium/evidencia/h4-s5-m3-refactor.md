# H4.S5.M3 — reorganización del flujo de alta

## Cambio

- `PasoAlta` ahora declara, en orden: `cuenta`, `correo`, `documento`, `datos`, `actividad`, `contrato`.
- `cuenta` agrupa teléfono, correo, canal informativo y clave/repetición; `correo` conserva la confirmación por código.
- `documento` reúne las cinco capturas con CI, expedición y vencimiento.
- `datos` reúne nombres, apellidos, fecha de nacimiento y el cotejo existente; `actividad` conserva origen, actividad económica y monto.
- Los widgets sincronizan los datos editables con `EstadoAlta`, de modo que atrás reconstruye sus controladores desde el estado.
- El error remoto de contraseña vuelve a `cuenta`; el de vencimiento vuelve a `documento`.
- No se cambió `enviarElAlta`, el contrato de `POST /usuarios`, ni el traspaso/subida posterior de capturas.

## Verificación dirigida

Ejecutado serialmente con `--concurrency=1`:

- `test/identidad/estructura_del_alta_test.dart` — 4/4 PASS: secuencia/fases, cabecera, campos distribuidos y conservación al retroceder.
- `test/identidad/paso_contrasena_test.dart` — 5/5 PASS (ahora cubre el paso `cuenta`).
- `test/widget/paso_datos_test.dart` — 2/2 PASS.
- `test/identidad/estado_alta_test.dart` — 5/5 PASS; incluye errores remotos de clave → `cuenta` y vencimiento → `documento`, y confirma el `POST` existente.
- `test/widget/paso_capturas_test.dart` — 3/3 PASS.
- `test/widget/hoja_antes_de_escanear_test.dart` — 1/1 PASS.
- `test/identidad/proteccion_alta_ingreso_test.dart` — 2/2 PASS, tras ajustar el avance esperado al nuevo paso `documento` en la fase 2.
- `test/identidad/error_alta_desplazamiento_test.dart` — 1/1 PASS.
- `flutter test --concurrency=1 --reporter expanded test/identidad` — 53/53 PASS, reejecutado después de extraer los bloques de contacto y clave para mantener `paso_cuenta.dart` bajo 200 líneas.
- `git grep -n "PasoAlta.celular" -- apps/movil/lib apps/movil/test` — 0 coincidencias.
- `flutter test --concurrency=1 --reporter expanded test/widget/paso_perfil_transaccional_test.dart` — 5/5 PASS.
- `flutter test --concurrency=1 --reporter expanded test/widget/hoja_de_seleccion_test.dart` (desde `packages/diseno_flutter`) — 6/6 PASS.

## Corrección durante la validación

La primera ejecución de `paso_perfil_transaccional_test.dart` detectó un overflow vertical de 6 px cuando una fila del selector tenía dos líneas: el padding vertical reducía el contenido disponible por debajo de la altura táctil de 48 dp. Se quitó ese padding interno manteniendo el mínimo táctil; las cinco pruebas del paso y las seis pruebas de la hoja de selección pasan ahora.

`enviarElAlta` continúa llamando al mismo `Registro.crear` con los mismos campos, contratos aceptados e idempotencia; el cambio de error solo conserva `campoError`/`trazaId` para dirigir la respuesta al paso correcto. La salida de `git diff` de `cu01_registrar.dart` contiene únicamente el ajuste de comentario sobre el número de pasos.

La suite integral de la app no se ejecutó por la restricción de RAM; sí se ejecutaron serialmente las pruebas específicas del flujo y el análisis focalizado. No se hizo QA visual en emulador/dispositivo.

La extracción visual quedó en `bloque_contacto_de_cuenta.dart` (76 líneas) y `bloque_clave_de_cuenta.dart` (99 líneas); `paso_cuenta.dart` queda en 192 líneas. `python scripts/verificar_frontend.py movil` terminó `TODO OK` y `dart analyze --fatal-infos` de la app terminó `No issues found!`.
