# Reporte — Ilustraciones HD de las pautas de foto (carnet y rostro)

> **AVANCE: 3 / 3 — 100 %.**

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/feat/backoffice-como-atlas-h0-h1-2026-10-18` (sin commit)
- Peldaño de evidencia alcanzado: `TESTED` + prueba visual del golden en 360 dp, tema claro. Sin corrida en dispositivo.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Carnet con sombra sobre la mesa, cartulina con degradé, ondas y roseta de seguridad, encabezado, chip, foto con la persona, datos con rótulo, firma, holograma y zona de lectura mecánica | `flutter test --update-goldens test/widget/ilustracion_de_pauta_golden_test.dart` | PASS; PNG inspeccionado (1× y vista 3×) |
| H1.S1.M2 | Muñeco con pelo, orejas, cuello con sombra, remera con cuello en V, ojos, cejas, nariz, sonrisa; gorra y lentes con brillo; contraluz con borde de luz | mismo comando | PASS; PNG inspeccionado |
| H1.S1.M3 | Tests de pautas y análisis | `flutter test test/widget/pautas_publicas_test.dart test/widget/ilustracion_de_pauta_golden_test.dart` · `flutter test test/widget/pautas_publicas_golden_test.dart` · `flutter analyze lib/pantallas/identidad/ilustraciones` | `All tests passed!` · `No issues found!` (`evidencia/`) |

## A medias
ninguna

## Pendiente
ninguna

## Evidencia
```text
$ flutter analyze lib/pantallas/identidad/ilustraciones
No issues found! (ran in 2.2s)
$ flutter test test/widget/pautas_publicas_test.dart test/widget/ilustracion_de_pauta_golden_test.dart
00:02 +3: All tests passed!
$ flutter test test/widget/pautas_publicas_golden_test.dart   (antes de actualizar)
Golden "goldens/pautas_publicas_360.png": Pixel test failed, 34.46%, 99239px diff detected.   ← esperado: cambió la ilustración
$ flutter test test/widget/pautas_publicas_golden_test.dart   (después)
00:00 +1: All tests passed!
```
Archivos: `evidencia/analyze.txt`, `evidencia/pautas-publicas.txt`, `evidencia/pautas-publicas-golden*.txt`, `evidencia/h10-sin-mi-cambio.txt`.

## No cubierto
- No se vio en dispositivo real (iPhone/Android) ni en tema oscuro. La ilustración usa colores fijos (no depende del tema), pero no se capturó.
- No se capturó en 412 / 600 dp.
- `hoja_antes_de_escanear.dart` usa la escena `carnetClaro`; no tiene golden propio.

## Desvíos del plan
- Se agregó `ilustraciones/persona_de_ejemplo.dart` (nuevo) para que el selfie y la foto del carnet compartan la misma persona.
- Para correr `pautas_publicas_test` se regeneraron los clientes Dart (`./gradlew generateOpenApiClients` + `apps/movil/scripts/generar-clientes.sh`). Son archivos generados y fuera de git. Antes no compilaban porque faltaban modelos (`ContactoEnGrupo`, `PerfilDeReputacionEnGrupo`, etc.).

## Riesgos residuales
- `test/goldens/h10_camara_visual_test.dart` tiene 6 goldens en rojo, y **ya estaban en rojo sin este cambio**: se guardaron solo los archivos de este trabajo con stash, se corrió el test, falló igual y después se restauraron los archivos. Ese test no dibuja `IlustracionDePauta`. Falla `ENVIRONMENT`/desactualizado, no se tocó (fuera de alcance).

## Decisiones y ambigüedades
- «Ultra HD» = dibujo vectorial con mucho más detalle (nítido a cualquier densidad), no fotos/bitmaps. Confirmar con Pablo.
- El commit `c0207532` pedía un busto «sin rasgos»; ahora el muñeco tiene rasgos simples y genéricos. Confirmar con Pablo.
- El carnet es genérico: no imita el diseño de ningún documento real (ni escudo ni bandera oficial).
- Solo colores de `Paleta` (tokens); la piel usa la escala `o100–o400`.

## Continuación (2026-10-30)
Este trabajo quedó sin commit en la rama vieja; se llevó a `test` (#360) y `dev` (#361), partido a ≤ 200 líneas, y salió en TestFlight 202610302027. Ver [../2026-10-30-test-sano-e-ilustraciones/REPORTE.md](../2026-10-30-test-sano-e-ilustraciones/REPORTE.md).
