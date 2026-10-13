# H8.S4.M5 / H10.S3.M3 — rúbrica final posterior a los ajustes

Fecha: 2026-10-13. Comparación posterior a [la rúbrica previa](./h8-rubrica-antes.md). Se ajustaron MFA, encabezado/riel de capturas, bienvenida y, en una revisión complementaria, dos acciones existentes de Perfil pasaron a filas táctiles. No se añadieron términos legales, opciones de perfil ni montos de bono. No se tocaron datos reales ni hardware.

## Veredicto

**Gate de severidad: PASA (0 críticos, 0 mayores en las superficies disponibles).** No equivale a aprobación visual universal: quedan observaciones menores/moderadas y limitaciones de contraste por instancia, cámara/dispositivo, desktop y grupos/detalle. La unidad grupos/detalle no tiene ruta/contrato local.

Escala 0–2 por criterio, orden J/G/E/S/C/D/X/P (jerarquía, grilla, espaciado, sistema, contraste visual, densidad, estados, pulido). Son las puntuaciones anteriores para pantallas sin cambios y puntuación revisada para las tres tratadas. `C` es observación visual; los tests de roles cubren pares de tokens, no cada instancia en pantalla.

| Superficie | Puntaje /16 | Cambio/hallazgo después |
|---|---:|---|
| Portada | 15 | Sin cambios; no hallazgo crítico/mayor. |
| Tour | 16 | Sin cambios; no hallazgo crítico/mayor. |
| Ingreso | 16 | Sin cambios; no hallazgo crítico/mayor. |
| MFA | 14 (2/2/1/2/2/1/2/2) | Mayor resuelto: ahora explica qué código ingresar y ofrece orientación a soporte sin inventar destino, canal ni reenvío; ayuda anclada abajo. [claro](./h10-visual/h10-mfa-principal-claro-412-130.png) / [oscuro](./h10-visual/h10-mfa-principal-oscuro-412-130.png). Principal/carga/error revisados en 360/412/600 dp, ambos temas. |
| Alta — cuenta | 11 | Sin cambios; permanece observación moderada: ayuda/placeholders oscuros aparentan tenue y requieren ratio por instancia. |
| Alta — correo | 15 | Sin cambios; no hallazgo crítico/mayor en estado inicial. |
| Alta — documento | 13 (2/2/2/2/1/1/1/2) | Mayor resuelto: título semántico de lámina vecina ya no aparece cortado; rótulos del riel más separados y pueden ocupar dos líneas. [claro](./h10-visual/h10-alta-documento-inicial-claro-412-130.png) / [oscuro](./h10-visual/h10-alta-documento-inicial-oscuro-412-130.png). Se revisaron 360/412/600 dp, ambos temas. |
| Alta — datos | 13 | Sin cambios; permanece observación moderada de contraste de ayuda/placeholders oscuros, sin medición individual. |
| Alta — actividad | 15 | Sin cambios; no hallazgo crítico/mayor en el estado capturado. |
| Alta — contrato | 10 | Observación menor, no mayor: el paso es puente y su CTA abre `PantallaDeContrato`, donde el texto completo debe leerse y se exigen tres consentimientos antes de aceptar. |
| Cámara | 12 | Sin cambios; sin prueba de cámara física. |
| Prueba de vida | 13 | Sin cambios; fixture sintético, no prueba física. |
| Subida | 14 | Sin cambios; estado capturado legible. |
| Verificación | 14 | Sin cambios; estados capturados legibles. |
| Bienvenida | 15 (2/2/2/2/2/1/2/2) | Mayor resuelto: panel centrado, desplazable y con ancho máximo 480 dp; no se inventó contenido/bono. [claro](./h10-visual/h10-bienvenida-principal-claro-412-130.png) / [oscuro](./h10-visual/h10-bienvenida-principal-oscuro-412-130.png). 360/412/600 dp, ambos temas. |
| Candado | 12 | Sin cambios; solo estado inicial en la inspección seleccionada, queda límite de dispositivo. |
| Billetera | 15 | Sin cambios; skeleton algo sutil, observación menor. |
| Grupos — mi estado | 15 | Sin cambios; detalle de cuatro estados sigue en índice H10. |
| Perfil | 12 | Revisión complementaria: las acciones existentes de contraseña y guía ahora son filas con hundido (véase `evidencia/h8-s3-m9.md`). J/G/E/S/C permanecen en 2; D=1, X=0 porque no hay inspección visual de estados de carga/error y P=1. Las acciones de cierre se prueban funcionalmente. No se inventaron ajustes adicionales. |
| Ayuda | 14 | Sin cambios; catálogo claro; estado de artículo/navegación no evaluado por captura estática. |

### Comparación de hallazgos mayores

| Hallazgo previo | Resultado | Evidencia de regresión |
|---|---|---|
| MFA: pantalla vacía y sin orientación | Resuelto a nivel de UI: instrucción y salida de ayuda visibles; la lógica de autenticación no se cambió. | `flutter test --no-pub --concurrency=1 --reporter compact test/goldens/h10_mfa_visual_test.dart` → incluido abajo, 18/18. Deep links: 5/5. |
| Alta/documento: título vecino cortado y riel apretado | Resuelto a nivel de presentación manteniendo el peek lateral; tarjeta vecina conserva espacio para alinear contenido, pero el encabezado no se pinta ni anuncia. | `flutter test --no-pub --concurrency=1 --reporter expanded test/widget/paso_capturas_responsive_test.dart` → 1/1; matriz dorada H10 de documentos → 6/6. Sin excepción de layout a 412 dp/130 %. |
| Bienvenida: panel pequeño pegado arriba con gran espacio muerto | Resuelto a nivel de composición: panel centrado, scroll seguro y máximo 480 dp. | H10 bienvenida → 6/6; aserción de centrado en 360/412/600 dp y ambos temas. |
| Contrato: densidad baja en pantalla puente | Reclasificado de observación visual mayor a menor. El puente navega a la pantalla del contrato; no se duplicó contenido legal ni se cambió el flujo. | `pantalla_contrato.dart` presenta `textoDelContrato`; `pantalla_contrato_test.dart` verifica lectura y consentimientos antes de habilitar aceptación. |

### Revisión complementaria de Perfil — 2026-10-13

La observación previa «hay acciones y no filas» queda resuelta para las dos acciones de navegación existentes. Las capturas claro/oscuro a 360/412/600 dp y la fila presionada a 412 dp se inspeccionaron; el resumen de inspección está en [H8.S3.M9](./h8-s3-m9.md). La nota no eleva el puntaje (12/16) porque la cobertura visual sigue limitada al estado inicial: sin captura de error/carga de esta pantalla, `X=0`. No se detectó un hallazgo bloqueante o mayor en la revisión complementaria, por lo que el gate de severidad documentado arriba se conserva. Esta rúbrica no sustituye una prueba real con lector de pantalla.

## Comandos y resultados

Desde `apps/movil`:

```text
flutter test --no-pub --concurrency=1 --reporter expanded test/widget/deep_link_test.dart
PASS: 5/5 (incluye que la pantalla MFA muestre instrucción y ayuda sin el shell).

flutter test --no-pub --concurrency=1 --reporter expanded test/widget/paso_capturas_responsive_test.dart
00:00 +1: All tests passed!

flutter test --no-pub --concurrency=1 --reporter compact --update-goldens test/goldens/h10_mfa_visual_test.dart test/goldens/h10_alta_documento_visual_test.dart test/goldens/h10_bienvenida_visual_test.dart
00:09 +30: All tests passed! (capturas actualizadas tras inspección de renders actuales; solo estas tres superficies)

flutter test --no-pub --concurrency=1 --reporter compact test/goldens/h10_mfa_visual_test.dart test/goldens/h10_alta_documento_visual_test.dart test/goldens/h10_bienvenida_visual_test.dart
00:08 +30: All tests passed!

dart analyze --fatal-infos lib/pantallas/identidad/pantalla_mfa.dart lib/pantallas/identidad/textos.dart lib/pantallas/identidad/pantalla_bienvenida.dart lib/pantallas/identidad/pasos_alta/paso_capturas.dart lib/pantallas/identidad/pasos_alta/tarjeta_de_captura.dart lib/pantallas/identidad/pasos_alta/riel_de_capturas.dart test/widget/deep_link_test.dart test/widget/paso_capturas_responsive_test.dart test/goldens/h10_mfa_visual_test.dart test/goldens/h10_alta_documento_visual_test.dart test/goldens/h10_bienvenida_visual_test.dart
No issues found!

python ..\..\scripts\verificar_frontend.py movil
TODO OK (11/11)
```

Se inspeccionaron visualmente las 30 capturas H10 regeneradas (18 MFA, 6 documento, 6 bienvenida) en sus anchos/temas/estados. El dark/360 de MFA reutiliza los nombres H8 existentes porque así está configurado el test; ambos PNG reflejan el render actualizado. No se regeneraron otras baselines.

## Observaciones y límites que quedan

- Contraste: el test `node --test pruebas/roles-contraste.test.mjs pruebas/texto-de-ejemplo-contraste.test.mjs` había pasado 3/3 antes de estos cambios. No calcula cada label/placeholder visible; cuenta/datos oscuros requieren medición de cada par.
- No cubiertos: cámara/haptics/biometría en hardware, desktop nativo, grupos/detalle sin ruta local, y backend real. La evidencia sintética no los sustituye.
- El resto de las 228 celdas H10 no cambió; su registro e inspección individual permanece en [INDICE](./h10-visual/INDICE.md) y [LEIDO](./h10-visual/LEIDO.md). La corrida global de goldens aún no se declara verde; la verificación de este cierre está limitada a los tres suites indicados.
- No se afirma aprobación AA global ni finalización del plan completo.
