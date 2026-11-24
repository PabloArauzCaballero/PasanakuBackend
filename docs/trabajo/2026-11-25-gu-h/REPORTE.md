# Reporte — Lote H: pendientes de la app móvil

> **AVANCE: 0 / 7 — 0,0 %.** (ninguna microtarea tiene aún su DoD ejecutado; el trabajo está en `WRITTEN`, ver «A medias»)

- Fecha: 2026-11-25 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-h-pendientes-app` (PR contra `dev`)
- Peldaño de evidencia alcanzado: `WRITTEN` en todas las áreas. Nada compilado ni testeado todavía.

## Completado
ninguna

## A medias
### H1.S1.M1–M3 — CONFIG-001 (firma de release)
- Qué anda: escrito `build.gradle.kts` (sin TODO de plantilla ni firma debug; firma de `APORTAYA_*` desde entorno o `-P`; tarea `verificarFirmaDeRelease` colgada de `preReleaseBuild`), `apk-de-prueba.yml` pasa la keystore estable por esas variables (YAML válido, NO ejecutado), y `docs/operacion/firma-android-release.md` con los pasos de Pablo.
- Qué no anda / sin verificar: no hay Android SDK en este entorno (dl.google.com bloqueado por el proxy), así que `assembleDebug` y `assembleRelease` sin secretos no se pudieron correr. Veredicto `BLOCKED` local.
- Qué falta exactamente: verificar en la CI del PR o en una máquina con SDK que `flutter build apk --release` sin variables falla con «Build de release SIN firma…» y que debug sigue igual.
- Dónde quedó: commit `fix(movil): el release de Android exige la firma…`, pusheado.

### H1.S2.M1 — RESPALDO-003 (tour)
- Qué anda: `textos.dart` reescrito según CU-23/CU-22 (sin cifras, sin «el turno se entrega igual», «la empresa no pone plata»); test `test/identidad/textos_del_tour_test.dart` escrito.
- Qué falta: correr el test; regenerar los goldens de la lámina 4 (`h8_tour_oscuro_test`, `h10_tour_visual_test`).
- Dónde quedó: sin commitear al escribir esto.

### H1.S3.M1 — APROB-001
- Decisión: **conservar** `AP-CU20-02` y `AP-CU69-10` en `errores.dart`: el servidor los sigue emitiendo (`CU20EtapaMinimaTest`, `InvitacionesAdmisionHumanaWebTest`, contrato `grupos.yaml`; `humana-obligatoria` default `true` en `application.yml:78`). El retiro es server-side (F2.L3.M10 / F2.L2.M06) y no está hecho. Test `test/unidad/errores_de_aprobacion_humana_test.dart` escrito (ata cliente y contrato del servidor). Sin correr.

### H1.S4.M1 — grupos/detalle
- Hallazgo: la pantalla, la ruta (`/pasanaku/grupos/:grupoId`, `rutas.dart:85`) y el contrato existen y resuelven cargando/error/datos vía `EstadoDePantalla`; la nota de PENDIENTES estaba desactualizada. Falta la matriz visual: `test/goldens/h11_grupos_detalle_visual_test.dart` escrito, capturas por generar y mirar. `PENDIENTES.md` ya dice «resuelto» pero se confirma solo tras mirar las capturas.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S5.M1 | TODO | gates móviles, PR, CI, aviso a la sesión principal |

## Evidencia
ninguna todavía

## No cubierto
Todo lo anterior: nada ejecutado.

## Desvíos del plan
Los PLAN/REPORTE de `2026-11-23-continuar-traspaso` y `TRASPASO.md` no existen en el repo; se trabajó con `PENDIENTES.md` y los anexos L1–L6.

## Riesgos residuales
Los goldens de la lámina 4 cambian y deben regenerarse en Linux (el job de CI es Linux). La ilustración `arte_fondo_de_garantia.dart` (paraguas sobre la cuota) no se tocó.

## Decisiones y ambigüedades
- Pasos exclusivos de Pablo: crear la keystore y los 4 secretos `ANDROID_RELEASE_*` (ver `docs/operacion/firma-android-release.md`). No se hizo ni se disparó nada.
- `hostDeLaAppWeb` obligatorio queda fuera (otra mitad de F4.L6.M22); Pablo puede pedirlo.
- Texto del tour: describe el objetivo de CU-23 (cobertura aún «pendiente de implementar»); Pablo puede suavizarlo.
