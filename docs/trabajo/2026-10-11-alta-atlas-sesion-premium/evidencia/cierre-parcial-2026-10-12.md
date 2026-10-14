# Verificación de cierre parcial — 2026-10-12

## Suites de Flutter que pasan

```text
flutter test test/unidad/textos_de_captura_test.dart test/unidad/privacidad_capturas_test.dart test/unidad/subida_del_expediente_test.dart test/widget/hoja_antes_de_escanear_test.dart test/widget/escaner_del_carnet_test.dart test/widget/pantalla_camara_de_respaldo_test.dart test/widget/paso_perfil_transaccional_test.dart --reporter expanded
00:04 +32: All tests passed!
EXIT_CODE=0
```

```text
flutter test test/unidad/ciclo_de_pose_test.dart test/widget/miniaturas_de_vida_test.dart test/widget/aviso_permiso_prueba_de_vida_test.dart --reporter compact
00:02 +16: All tests passed!
EXIT_CODE=0
```

```text
flutter test test/widget/prueba_de_vida_test.dart --reporter expanded
00:07 +7 -1: Some tests failed.
Failing test: las tres poses se capturan solas y el flujo devuelve las fotos
Falla: esperaba "FOTO 2 DE 3" después de la primera pose automática.
EXIT_CODE=1
```

```text
dart analyze
Analyzing movil...
34 issues found.
EXIT_CODE=0
```

El analizador solo informó lints/info (incluidos avisos preexistentes de const/deprecaciones); no reportó errores. No se ejecutó un build Android/iOS ni prueba con cámara física.

## Historial de la suite completa

```text
flutter test --reporter compact
503 passed, 36 failed
EXIT_CODE=1
```

Esta corrida integral fue anterior a las correcciones de imports/API, disposición del selector, limpieza del test widget y sincronización de la captura de cámara. No se repitió después de los últimos cambios; por lo tanto no se presenta como estado final de regresión.

## Cambios de verificación hechos durante el cierre

- `FilaDeSeleccion` reservaba un alto fijo de 48 dp aunque el contenido suma padding adicional; la hoja del selector ahora reserva el alto compuesto y el offset inicial coincide.
- Las pruebas de perfil seleccionan filas por semántica, y el flujo perfil/transaccional pasó 5 casos en la suite agrupada.
- La prueba de captura de cámara de respaldo espera la lectura real de la imagen fuera del fake-async; sus tres casos pasaron en la suite agrupada.
- `CicloDePose` conserva la última instrucción/progreso del detector entre ticks sin veredicto.
- Se inyectó el reloj de prueba en el controlador de vida; la transición automática integrada todavía falla y permanece abierta.
- El test de miniaturas libera su `SemanticsHandle` explícitamente; los 16 casos de ciclo/miniaturas/permisos pasaron.

## Gate de formato / whitespace

```text
git diff --check -- apps/movil packages/diseno_flutter docs/trabajo/2026-10-11-alta-atlas-sesion-premium/PLAN.md
warning: in the working copy of 'apps/movil/lib/pantallas/identidad/dominio/estado_alta.dart', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'apps/movil/lib/pantallas/identidad/pantalla_prueba_de_vida.dart', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'packages/diseno_flutter/lib/atomos/campo_de_seleccion.dart', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'packages/diseno_flutter/lib/atomos/lista_con_buscador.dart', LF will be replaced by CRLF the next time Git touches it
```

El comando terminó con código 0; los avisos son normalización de finales de línea de Windows, no whitespace inválido.

## Re-verificación del flujo de vida — 2026-10-12

```text
flutter test test/widget/prueba_de_vida_test.dart --reporter expanded
00:05 +9: All tests passed!
EXIT_CODE=0
```

La suite comprueba tres poses automáticas con cámara falsa, avance al siguiente cuadro, botón «Continuar» y mapa `Map<CaraDelCarril, Captura>` devuelto. El validador de imagen se sustituye en ese recorrido para evitar que el I/O real compita con el reloj falso; otra prueba conserva el rechazo de una imagen real temporal. Se mockea el canal de plataforma para hápticos en el test de navegación. La carpeta temporal se elimina tras desmontar la pantalla y vaciar la caché de imágenes de Flutter.

```text
flutter analyze
34 issues found. (ran in 9.3s)
EXIT_CODE=0
```

Los 34 resultados son infos/lints (const, API deprecadas y observaciones de estilo); no hubo errores de análisis. La ejecución no verifica cámara física ni rendimiento de dispositivo.

```text
python .claude/hooks/plan_status.py --path docs/trabajo/2026-10-11-alta-atlas-sesion-premium/PLAN.md
Avance: 124/213 microtareas HECHO (58.2%)
Estados: TODO=63  HECHO=124  A MEDIAS=24  BLOQUEADO=2
```

### Re-verificación final del ciclo sin detector

```text
flutter test test/widget/prueba_de_vida_test.dart --reporter expanded
00:05 +10: All tests passed!
EXIT_CODE=0
```

La décima prueba avanza 50 tics de 100 ms con una cámara sin detector y comprueba una captura automática seguida por el paso a la pose 2. El reloj falso se avanza por tics, porque un único salto grande bombea menos callbacks periódicos.

```text
python .claude/hooks/plan_status.py --path docs/trabajo/2026-10-11-alta-atlas-sesion-premium/PLAN.md
Avance: 124/213 microtareas HECHO (58.2%)
Estados: TODO=63  HECHO=124  A MEDIAS=24  BLOQUEADO=2
```

### Intento adicional: directorio vacío tras devolver una captura parcial

```text
flutter test --reporter expanded --plain-name "salir antes de terminar devuelve capturas parciales" test/widget/prueba_de_vida_test.dart
PathAccessException: Cannot delete file .../selfie.png
OS Error: el proceso no tiene acceso al archivo porque está siendo utilizado por otro proceso (errno = 32)
EXIT_CODE=1
```

La captura parcial sí vuelve en el mapa; el borrado adicional, ejecutado mientras sigue vivo el runner Flutter de Windows, no pudo eliminar el archivo abierto. `eliminarArchivosLocales` absorbe el `FileSystemException`; el temporal se liberó al terminar el runner. Se dejó la suite original intacta y H6.S3.M9 permanece A MEDIAS: confirmar el comportamiento en Android y decidir si hace falta desvincular miniaturas antes de borrar.

### Re-verificación serial relacionada con vida y subida

```text
flutter test --reporter compact test/widget/prueba_de_vida_test.dart test/widget/pantalla_subida_del_expediente_test.dart test/unidad/subida_del_expediente_test.dart
00:09 +22: All tests passed!
EXIT_CODE=0
```

## Estado final y gate del reporte

```text
python .claude/hooks/plan_status.py --path docs/trabajo/2026-10-11-alta-atlas-sesion-premium/PLAN.md
Avance: 124/213 microtareas HECHO (58.2%)
Estados: TODO=63  HECHO=124  A MEDIAS=24  BLOQUEADO=2
REPORTE.md: completo
```

```text
python .claude/hooks/report_gate.py --self-test
report_gate self-test: 14 PASS, 0 FAIL
```

## Navegación del carrusel de capturas

```text
flutter test test/widget/paso_capturas_test.dart --reporter expanded
00:03 +3: All tests passed!
EXIT_CODE=0
```

El round-trip usa un router de prueba: tocar selfie envía las tres poses de vida pendientes; al devolver el mapa de capturas, la tarjeta registra 3/5 y el carrusel avanza a la primera pendiente. Los otros casos cubren el carrusel y las pautas. Se corrigió la prueba para disponer explícitamente su `SemanticsHandle` antes de que Flutter verifique el fin del test. Junto con `posesDesde` en `prueba_de_vida_test.dart`, quedan cubiertas selección desde la cara tocada y repetición exclusiva de una cara ya capturada.

## Retorno de foto rechazada del escáner

```text
flutter test test/widget/escaner_del_carnet_test.dart --reporter expanded
00:04 +5: All tests passed!
EXIT_CODE=0
```

El test de «Usar esta foto igual» ahora espera el resultado de `sacarCaptura` y comprueba que el mapa devuelto conserva la captura del anverso elegida; otro caso verifica que repetir vuelve a escanear y devuelve la nueva captura.

## Pautas públicas de captura

```text
flutter test test/widget/pautas_publicas_test.dart --reporter expanded
00:01 +2: All tests passed!
EXIT_CODE=0
```

El recorrido anónimo llega a `/registro/pautas` sin shell y verifica las siete parejas bien/mal con sus ilustraciones. La captura de pantalla pedida en el plan sigue pendiente de emulador/dispositivo.

## Goldens de la silueta de vida

```text
flutter test test/widget/silueta_de_cara_test.dart --reporter expanded
00:00 +5: All tests passed!
EXIT_CODE=0
```

Incluye seis imágenes de 300×400 (frente/izquierda/derecha en claro y oscuro). Se revisaron visualmente; el banner de depuración que aparecía en la primera generación se quitó antes de generar las referencias finales. El análisis abarca simetría, contorno, velo, guías y estado animado. El test Node `generados-coinciden.spec.ts` no se pudo ejecutar porque `vitest` no está instalado/disponible en el entorno; la prueba Flutter sí lee los tokens generados y comprueba ambos temas.

## H5.S1.M2 — Sin salida manual para capturar el carnet

```text
git grep -n -i -E 'escribirAMano|a mano' -- apps/movil/lib
sin coincidencias (exit 1)

flutter test --reporter expanded test/widget/tarjeta_de_capturas_test.dart
00:00 +2: All tests passed!
EXIT_CODE=0
```

El texto/ramas de «Escribir a mano» no existen en la app. Los dos estados de permiso conservan «Elegir de la galería» y su botón de recuperación, sin ofrecer continuar sin foto.

## H9.S1.M6 — Espejo de skills

```text
Comparación recursiva de rutas y SHA-256 entre .claude/skills y .agents/skills
PASS: 289 archivos; rutas y SHA-256 idénticos
```

## Checkpoint — H9.S1.M1/M3/M5

- Hecho: verificación factual de `pasanaku-diseno` y `pasanaku-movimiento`; lista §95.8 actualizada con D2/D4. Se corrigió una afirmación falsa sobre `MovimientoDeInterfaz`.
- Evidencia: `h9-verificacion-de-skills.md`; `git diff --check` y contador del plan.
- Skills: `skill-authoring`, `anti-hallucination-guard`, `pasanaku-diseno` (leídas completas antes del cambio).
- Ahora: H9.S1.M4 cotejada; H9.S1.M2 depende de cierre H1.
- Bloqueo: dispositivo físico fuera del alcance; no afecta estas comprobaciones estáticas.
- Estado: HECHO para M1/M3/M4/M5; M2 sigue TODO.
- Peldaño: VERIFIED para M1/M3/M4/M5.

## H5.S2.M5 — Navegación a las pautas desde el carrusel

```text
flutter test --reporter expanded test/widget/paso_capturas_test.dart
00:03 +3: All tests passed!
EXIT_CODE=0
```

El caso «el botón de pautas abre el contenido disponible» verifica el título, recomendaciones, estados Correcto/Incorrecto, selección de «Foto de frente», título correspondiente y desplazamiento hasta «Sin accesorios».

## Checkpoint — H5.S2.M1/M3/M4

- Hecho: la prueba cubre cinco láminas, insignia 0→1, avance al reverso, insignia exitosa a 5/5; cada punto tiene 48×48 dp, navega a su lámina y los completos usan `t.ok`.
- Evidencia: `flutter test --reporter expanded test/widget/paso_capturas_test.dart` → 3/3 PASS; la prueba lee el color renderizado de los cinco puntos tras completar las cinco capturas.
- También: `flutter test --reporter expanded test/widget/tarjeta_de_captura_ampliada_test.dart` → 1/1 PASS; decode cache 720 px a 360 dp/DPR 2, `gaplessPlayback` y hueco visible con archivo inexistente.
- Skills: `flutter-testing`, `frontend-accessibility`, `mobile-ux-design`, `pasanaku-diseno`, `pasanaku-ui-componentes`.
- Ahora: H5.S2.M2 (diseño responsive con viewportFraction/Hero) sigue pendiente de rediseño/capturas.
- Bloqueo: capturas de hardware no disponibles; no fueron requisito de M3.
- Estado: HECHO para M1/M3/M4.
- Peldaño: TESTED para M1/M3/M4.

## Checkpoint — H10.S1.M2

- Hecho: formateados únicamente los siete archivos que el verificador señaló; el barrido completo ya no detecta diferencias.
- Evidencia: `h10-formato-2026-10-12.txt`.
- Skills: `flutter-testing` y `evidence-and-verification`; operación mecánica de formato, sin cambio funcional.
- Ahora: seguir con H10 en serie; otros gates y la regresión integral permanecen pendientes.
- Bloqueo: ninguno para este DoD.
- Estado: HECHO.
- Peldaño: VERIFIED.

## Checkpoint — H10.S1.M3

- Hecho: quitados los dos literales de diseño reportados en la app móvil sin reducir el contador de 88 dp: se añadió `Tipo.cuentaAtras` a la fuente JSON y a su espejo CSS, se regeneró el Dart y la cuenta usa ese estilo; el visor toma `ColorScheme.scrim`.
- Refactor: `app.dart` queda por debajo de 200 líneas y delega el `MaterialApp.router`; el borrado temporal reutiliza `eliminarRutaLocal`; la página de transición de marca vive en su helper local.
- Verificado: el gate `python ../../scripts/verificar_frontend.py movil` termina `TODO OK`; `dart analyze --fatal-infos` pasa para los seis archivos afectados. El cotejo directo JSON/CSS cubre 13 roles tipográficos y pasa.
- Regresiones: `prueba_de_vida_test.dart` 10/10, `deep_link_test.dart` 5/5, `capa_de_bloqueo_test.dart` 7/7 y `portada_test.dart` 8/8. `guardian_de_sesion_test.dart` falla en su segundo caso por el timer de tutorial de 50 ms pendiente tras desmontar; se anota sin atribuirlo a estos cambios.
- La prueba Vitest de bóveda no pudo iniciar: el workspace no tiene `vitest` instalado (`ERR_MODULE_NOT_FOUND`); `npx` no resolvió `vitest/config`. No se cambió el manifiesto ni el lockfile.
- Skills completas aplicadas antes de los cambios: `flutter-theming`, `frontend-design-system`, `color-systems`, `typography-systems`, `flutter-testing`, `flutter-development`, `flutter-state-architecture`, `native-code-patterns` y `frontend-navigation-ia`.
- Estado: HECHO para el DoD de M3.

## Cierre — H10.S1.M1

```text
cd apps/movil
dart analyze --fatal-infos
Analyzing movil...
No issues found!
EXIT_CODE=0
```

Las 32 infos fueron corregidas sin supresiones: inicialización del ciclo de pose sin cambiar su API, `const`, imports redundantes, comentarios de biblioteca, transformaciones no deprecadas y colección de flags semánticos actual. Regresiones en serie: `ciclo_de_pose_test.dart`, `silueta_de_cara_test.dart`, `miniaturas_de_vida_test.dart` y `componentes_de_fase_test.dart` → 22/22 PASS; `dart format --output=none --set-exit-if-changed lib test` → 383 archivos, 0 cambios. Estado: HECHO.

## Cierre — H10.S1.M4

```text
cd apps/movil
flutter test --concurrency=1 --reporter expanded test/unidad test/widget test/contrato test/identidad test/pasanaku --timeout 60s
566 passed; 1 failed
EXIT_CODE=1
Único fallo: test/unidad/guardian_de_sesion_test.dart — «un 401 cierra la sesión y lleva al ingreso»
```

La falla coincide con la línea base H0 (`h0-test-front-post-clientes.txt`): queda un timer de 50 ms de búsqueda de ancla del tutorial al desmontar el árbol. Los desajustes nuevos de copy y fixture que aparecieron en la primera repetición se corrigieron; la segunda corrida completa no conserva ninguno. Los tests dirigidos de captura, expediente, texto español, datos, autolanzamiento, pautas, navegación y avisos de ingreso pasaron. Por criterio de H10.S1.M4 (fallas contenidas en H0), estado: HECHO.

## Cierre — H3.S1.M1 · H10.S1.M6/M7/M8

`packages/diseno_flutter/test/widget/hoja_de_seleccion_test.dart` → 6/6 PASS después de ajustar las filas al mínimo táctil de 48 dp; el caso 360×640 con teclado de 300 px confirma tres filas enteras. M1 queda HECHO. El dispositivo real pendiente de M3 no se sustituye por esta geometría simulada.

`apps/movil/test/goldens`: 7 casos PASS, 8 FAIL (solo saldo claro/oscuro y seis capturas del escáner en tres tamaños y dos temas). Los diffs inspeccionados mantienen el mismo contenido y distribución; el repositorio advierte que los baselines existentes fueron generados en macOS y el rasterizador actual es Windows. No regeneré esos ocho baselines desde una plataforma no canónica. La transición sí se corrigió a fundido opaco sin solapamiento, 300 ms (`Movimiento.pagina`), con reduced-motion instantáneo; cuatro cuadros se regeneraron, se inspeccionaron visualmente con las fuentes correctas y el test quedó verde.

`packages/diseno_flutter`: 82 PASS, 13 FAIL, todos goldens del catálogo; la falla funcional de tres filas con teclado quedó resuelta. H10.S1.M7 queda A MEDIAS hasta regenerar/validar en el entorno canónico. H10.S1.M6 también A MEDIAS por los ocho diffs del rasterizador.

`cd packages/diseno_flutter; dart analyze --fatal-infos` (2026-10-12) → `No issues found!`, EXIT_CODE=0. `python .claude/hooks/plan_status.py --self-test` → 31 PASS, 0 FAIL.

H10.S1.M8 queda BLOQUEADO: `evidencia/h1-s6-backend-bloqueado.md` registra PostgreSQL:5433 rechazando conexión y Docker inactivo; el análisis de recursos observó ~1,82 GiB libres frente a heap Gradle de 3 GiB. No inicié Gradle para no agotar RAM ni alterar servicios/contendedores externos.

## Cierre — H10.S1.M5

`flutter test --concurrency=1 --reporter expanded test/a11y --timeout 60s` → 4/4 PASS. La suite móvil de accesibilidad queda HECHO.

## Cierre — H4.S5.M6/M8

```text
cd apps/movil
flutter test --concurrency=1 --reporter expanded --timeout 60s test/identidad/error_alta_desplazamiento_test.dart test/identidad/proteccion_alta_ingreso_test.dart test/widget/paso_contrato_de_alta_test.dart
00:05 +5: All tests passed!
EXIT_CODE=0
```

El test M6 recorre `PasoAlta.values` (siete pasos) y exige que el error y la referencia queden visibles en la franja superior tras desplazarse al final de cada paso. La iteración encontró dos alertas en contrato; se eliminó el duplicado del organismo y la presentación común de `PantallaDeRegistro` queda como fuente única. M8 verifica con un puerto falso el alta a través de cuenta/identidad/datos y el ingreso, incluyendo activar y liberar protección. Skills consultadas para este cambio: `flutter-testing`, `test-case-design-techniques`, `requirements-and-acceptance`, `evidence-and-verification`, `frontend-ux-states`, `pasanaku-app-movil-ux`.

## Estados visibles de cada fila de subida

```text
flutter test --reporter expanded test/widget/pantalla_subida_del_expediente_test.dart
00:00 +2: All tests passed!
EXIT_CODE=0
```

La prueba de `FilaDeSubida` verifica el copy y la cancelación de la fila lenta, oculta la ruta local, y comprueba error/referencia junto a las acciones «Reintentar» y «Repetir foto del perfil derecho». El test del controlador ya cubre el adaptador falso y el umbral exacto de 10 s. Se conserva H5.S3.M4 como A MEDIAS porque falta montar la pantalla completa con esos estados y capturarla en emulador/dispositivo.
