# Reporte — Lote H: pendientes de la app móvil

> **AVANCE: 4 / 7 — 57,1 %.** (A MEDIAS cuentan como no hechas: H1.S1.M1 y H1.S4.M1; H1.S5.M1 pendiente de CI.)

- Fecha: 2026-11-25 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-h-pendientes-app` → PR a `dev`
- Peldaño de evidencia: tour y errores `TESTED` (tests dirigidos); firma de release `WRITTEN`; grupos/detalle `DISCOVERED`.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M2 | `apk-de-prueba.yml` pasa su keystore estable por las variables `APORTAYA_*` (no se ejecutó ni disparó; solo corre en push a `test`) | `python3 -c "import yaml;yaml.safe_load(open('.github/workflows/apk-de-prueba.yml'))"` | PASS (`yaml ok`) |
| H1.S1.M3 | `docs/operacion/firma-android-release.md` con keytool, 4 secretos con nombre y el paso de workflow | lectura | existe |
| H1.S2.M1 | Tour: lámina 4 reescrita desde CU-23/CU-22, sin cifras; goldens de la lámina 4 regenerados y la captura 360/oscuro/130 % mirada (sin overflow) | `flutter test test/identidad/textos_del_tour_test.dart test/unidad/errores_de_aprobacion_humana_test.dart` y `flutter test test/goldens/h8_tour_oscuro_test.dart test/goldens/h10_tour_visual_test.dart` | PASS (`All tests passed!`, salidas en `evidencia/`) |
| H1.S3.M1 | `AP-CU20-02`/`AP-CU69-10` conservados: el servidor los emite; test que ata cliente y contrato | idem | PASS |

## A medias
### H1.S1.M1 — CONFIG-001 firma de release
- Qué anda: `build.gradle.kts` sin TODO de plantilla ni firma debug; firma desde `APORTAYA_KEYSTORE_FILE/_PASSWORD/KEY_ALIAS/KEY_PASSWORD` (entorno o `-P`); `verificarFirmaDeRelease` (de `preReleaseBuild`) aborta con «Build de release SIN firma: faltan …».
- Qué no anda / sin verificar: no hay Android SDK (dl.google.com bloqueado por el proxy), no se corrió `assembleDebug` ni `assembleRelease`. `BLOCKED` local.
- Qué falta: correr `flutter build apk --release` sin variables (debe fallar con el mensaje) y `--debug` (debe seguir igual) en una máquina con SDK o en CI.
- Dónde quedó: commit en la rama, pusheado.

### H1.S4.M1 — grupos/detalle
- Qué anda: se verificó que SÍ existen pantalla (`PantallaDelGrupo`), ruta (`rutas.dart:85`) y contrato; resuelve cargando/error/datos con `EstadoDePantalla`; hay tests (`test/pasanaku/detalle_del_grupo_test.dart`). Los pendientes `H8.S4.M4`/`H10.S3.M1` estaban desactualizados; `PENDIENTES.md` corregido.
- Qué no anda: no hay matriz de capturas de esa pantalla.
- Qué falta: generar los clientes Dart (`gradle generateOpenApiClients`, falla aquí por 429 de Maven Central) y producir 3 anchos × 2 temas × {datos, cargando, error} mirándolos.
- Dónde quedó: sin código nuevo (se descartó un golden sin poder generar su baseline).

## CI de #521
- Rojo en `5828baae`, job «Frontend — Flutter y Angular»: el lint de `apps/movil` falló por «ningún archivo de más de 200 líneas» (`textos.dart` 202). Causa: mi comentario de fuente. Corregido en `4c3b0b48` (ahora 200 líneas; test del tour sigue en verde). El PR #521 se mergeó a `dev` (282a1522) con la CI sin rojos al cierre (suite del commit `7de86687` completada sin fallos según GitHub).
- Estado final: MERGEADO. Lo que sigue sin verificar y pasa a quien retome: verificar `assembleRelease` sin secretos (en CI o con SDK) y la matriz de capturas de grupos/detalle.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S5.M1 | TODO | CI verde del PR y aviso a la sesión principal |

## Evidencia
`evidencia/tests-dirigidos.txt` (7 tests, `All tests passed!`) · `evidencia/goldens-update.txt` (29 tests, `All tests passed!`).

## No cubierto
`assembleRelease`/`assembleDebug`; `flutter analyze` completo y suite completa (clientes Dart no generados: 137 issues de imports de `clientes/dart/*/lib/src`; sobre los archivos tocados: sin issues); capturas de grupos/detalle; la ilustración `arte_fondo_de_garantia.dart` (paraguas sobre la cuota) no se tocó.

## Desvíos del plan
Se descartó el golden `h11_grupos_detalle_visual_test` (no se pudo generar su baseline). Los PLAN/REPORTE de `2026-11-23-continuar-traspaso` y `TRASPASO.md` no existen en el repo.

## Riesgos residuales
Si el workflow de producción no existe aún, el release firmado depende de que Pablo cree la keystore y los secretos.

## Decisiones y ambigüedades (para que Pablo corrija si quiere)
- **Solo Pablo:** crear la keystore y los secretos `ANDROID_RELEASE_KEYSTORE_BASE64`, `_KEYSTORE_PASSWORD`, `_KEY_ALIAS`, `_KEY_PASSWORD` (pasos en `docs/operacion/firma-android-release.md`).
- RESPALDO-003: texto describe el objetivo de CU-23 (cobertura aún «pendiente de implementar»); si se prefiere, se puede quitar la promesa de cobertura. Fuente: `docs/CasosDeUso/CU-23…md`, `CU-22…md`.
- APROB-001: se conservan ambos códigos mientras el servidor los emite (`application.yml:78`, tests de grupos); se retiran con F2.L3.M10.
- `hostDeLaAppWeb` obligatorio en release: fuera de alcance.
