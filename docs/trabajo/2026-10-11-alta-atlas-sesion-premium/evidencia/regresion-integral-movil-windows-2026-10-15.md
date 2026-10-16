# Revalidación integral móvil en Windows (2026-10-15)

## Entorno y comando

- Windows, Flutter 3.47.5, Dart 3.13.4.
- Desde `apps/movil`:

```text
flutter test --no-pub --concurrency=1 --reporter json test/unidad test/widget test/contrato test/identidad test/pasanaku --timeout 60s
```

Resultado de la corrida posterior al ajuste de la expectativa MFA: **627 PASS, 5 FAIL, salida 1**. No se usó Docker ni AVD; la RAM se mantuvo entre 9,75 y 10,70 GiB usados de 15,32 GiB.

## Fallos exactos

Los cinco tests fallidos terminan en comparaciones de imágenes golden; no falló ninguna aserción de autorización, sesión o contrato:

| Test | Golden / resultado observado |
|---|---|
| `test/widget/paso_capturas_test.dart` — cinco láminas, avance tras guardar y puntos tocables | `capturas_peek_360.png`: 35,71 % de píxeles distintos; el test se detiene en esa comparación antes de sus aserciones posteriores. |
| `test/widget/paso_capturas_test.dart` — el marco conserva las dos elevaciones tokenizadas | Comparaciones `capturas_marco_claro_360.png` y `capturas_marco_oscuro_360.png`; la clara reporta 91,05 % de diferencia. |
| `test/identidad/h4_ci_icono_visual_test.dart` — compara visualmente el carnet con tarjeta de pago | `anverso-360.png`: 40,65 % de píxeles distintos. |
| `test/identidad/motivo_de_salida_en_ingreso_test.dart` — sesión caducada | `h1-ingreso-caducada.png`: 1,38 % de píxeles distintos. |
| `test/identidad/motivo_de_salida_en_ingreso_test.dart` — sesión cerrada | `h1-ingreso-cerrada.png`: comparación pixelaria fallida. |

Se conservaron los baselines: no se ejecutó `--update-goldens`. Por ser comparaciones visuales de host Windows, no se atribuyen automáticamente a un defecto funcional ni se declaran correctas: la clasificación canónica sigue pendiente en el runner macOS/Flutter 3.44.8 indicado por H10.S1.M6–M7. Los tests de texto/estado que alcanzan sus comparaciones no muestran discrepancia; el primer test de capturas no alcanza sus assertions posteriores en esta corrida.

## Defecto de prueba corregido

El test `test/widget/deep_link_test.dart` esperaba `TextosIdentidad.mfaIndicacion`, que no es el texto que renderiza `PantallaDeMfa`; la pantalla usa `TextosIdentidad.explicacionMfa`. Se actualizó únicamente la expectativa para validar el texto real y vigente de la UI. La suite aislada `flutter test --no-pub --concurrency=1 --reporter expanded test/widget/deep_link_test.dart` pasó **5/5** después del cambio. No se cambió el texto de producto ni la navegación MFA.

## Verificaciones relacionadas

- Suite focalizada de sesión/diagnóstico/navegación/estado KYC: **23/23 PASS** (`evidencia/regresion-sesion-diagnostico-android-2026-10-15.md`).
- Hoja de selección del paquete `diseno_flutter`, incluida la lista corta bajo teclado: **8/8 PASS** (`evidencia/h3-s3-m3-selector-regresion-windows-2026-10-15.md`).
- `dart format --output=none --set-exit-if-changed` para 19 archivos Dart modificados: **0 cambios**; `dart analyze --fatal-infos` sobre esos mismos 19: **No issues found!**.
- El contador del plan permanece **199/213 (93,4 %), 5 A MEDIAS y 9 BLOQUEADO**; ningún DoD incompleto cambió a HECHO por estas verificaciones.

## Clasificación y revalidación de goldens — 2026-10-15

- `paso_capturas_test.dart`: los tres diffs grandes eran baselines obsoletos respecto del cambio intencional de `553bd786` (marco/fondo/icono neutralizados y estilos actuales del paquete de diseño). Se inspeccionaron imágenes anterior/actual; se actualizaron solo los tres PNG de test. La corrida de actualización y la corrida normal posterior pasaron **4/4**.
- `h4_ci_icono_visual_test.dart`: su comparación apuntaba a la captura histórica en `docs/`, anterior al mismo rediseño, y la diferencia era 40,65 %. No se sobrescribió evidencia histórica: el test ahora compara los dos PNG de `test/identidad/goldens/`. Se generaron con la salida actual tras revisar el marco neutro y el formulario responsive; el test normal pasa **1/1**.
- Suite completa repetida después de sincronizar estos baselines: **630/632 PASS**, salida 1. Los únicos dos restantes son los goldens de sesión caducada/cerrada (cada uno 1,38 % distinto); las aserciones de texto/tono pasan. No se actualizaron: quedan pendientes de contrastar en el entorno canónico Flutter 3.44.8/macOS. Por ello H10.S1.M6 continúa A MEDIAS y no se afirma regresión funcional ni éxito integral.
