# Reporte — Pautas de foto: ejemplos de carnet y de rostro, consejos y aviso antes de escanear

> **AVANCE: 6 / 6 — 100,0 %.** El hito queda A MEDIAS: falta verlo en el teléfono. La regresión no tiene rojos nuevos; `escaner_del_carnet_test` es intermitente y también falla en `origin/test` (ver Evidencia).

- Fecha: 2026-10-15 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/feat/pautas-de-foto-2026-10-15` (desde `origin/test` dbfbf35b)
- Peldaño de evidencia: `TESTED` + prueba visual por goldens (claro y oscuro, 360 y 412 dp). No `VERIFIED`: no se abrió en un dispositivo.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Carnet con proporción ID-1 y radio reales sobre una mesa oscura. Los defectos se dibujan como en una foto: oscuridad, franja de reflejo con degradé, desenfoque real (`ImageFilter.blur`) y perspectiva real (`Matrix4` con profundidad) | `flutter test test/widget/ilustracion_de_pauta_golden_test.dart --update-goldens` | PASS · `test/widget/goldens/ilustracion_de_pauta_escenas.png` mirado |
| H1.S1.M2 | Rostro: busto neutro sin rasgos, dentro del mismo óvalo y con el mismo velo que la cámara. Ladeado se sale del óvalo; con accesorios lleva lentes oscuros y gorra; el contraluz tiene el fondo quemado y la persona oscura. Desaparecen la cabeza verde con ojos de punto y el arco de pelo | ídem | PASS · ídem |
| H1.S1.M3 | Cada ejemplo lleva un marco verde (correcto) o rojo (incorrecto), además del ícono y la palabra | `flutter test test/widget/pautas_publicas_test.dart test/widget/pautas_publicas_golden_test.dart --update-goldens` | PASS 3/3 · `pautas_publicas_360.png` mirado |
| H1.S2.M1 | Consejos de cámara: un ícono por consejo (luz, sin flash, nitidez, bordes; rostro, sin accesorios, luz) en lugar de cuatro tildes grises iguales | `flutter test test/widget/roles_color_acento_test.dart test/widget/pantalla_camara_de_respaldo_test.dart` | PASS 11/11 |
| H1.S2.M2 | «Antes de escanear» muestra arriba el ejemplo del carnet correcto | `flutter test test/widget/hoja_antes_de_escanear_test.dart` | PASS 1/1 · `evidencia/hoja-antes-de-escanear-*.png` |
| H1.S3.M1 | Análisis estático y regresión de la app | `dart analyze --fatal-infos` · `flutter test --reporter expanded` | `No issues found!` · rojos = los 7 preexistentes + 1 intermitente preexistente |

## A medias
### H1 — hito
- Qué anda: todo lo de la tabla, demostrado en goldens.
- Qué no anda: nada atribuible a este trabajo.
- Qué falta exactamente: abrir las pautas, la cámara y el aviso en un teléfono; que Pablo confirme A1.
- Dónde quedó: rama `pablo/feat/pautas-de-foto-2026-10-15`; compila (`dart analyze` limpio).

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Prueba en dispositivo | TODO | Un APK o TestFlight de esta rama |

## Evidencia
```text
$ flutter test --reporter expanded   (apps/movil) → evidencia/h1s3m1-suite-movil.txt, evidencia/fallos.lst
$ comm -13 <fallos-final.lst de 2026-10-15-visual-bancario> fallos.lst
test/widget/escaner_del_carnet_test.dart: un carnet escaneado queda como captura del anverso [E]
# Clase: TEST_BUG intermitente y preexistente. No importa ningún archivo tocado (sacar_captura.dart).
# Con mis cambios: 1 corrida PASS y 1 FAIL. En origin/test, sin mis cambios: 3 PASS y 1 FAIL
# (evidencia/escaner-en-base-origin-test.txt). Depende de una espera real de 300 ms para decodificar un PNG.
```
Capturas: `evidencia/hoja-antes-de-escanear-{claro,oscuro}-412.png`, `consejos-camara-{claro,oscuro}-412.png`, `pautas-rostro-{claro,oscuro}-412.png`. Las renderizó una prueba descartable que no cargaba las fuentes, así que el texto y los íconos salen como bloques; sirven para ver la composición. Las ilustraciones salen completas.

## No cubierto
- Dispositivo real.
- Pantallas de 600 dp de las pautas (dos columnas): el diseño existente no se tocó, solo el contenido del dibujo.

## Desvíos del plan
- `roles_color_acento_test` buscaba el ícono `check_circle` de los consejos. Ahora busca `wb_sunny_outlined`, el primer consejo del carnet, con la misma aserción de color `text2`.

## Riesgos residuales
- `ios/Podfile.lock` versionado está desactualizado: le faltan los pods de ML Kit y cada `flutter build ios` lo reescribe (también `project.pbxproj`). No se commiteó: queda fuera de alcance.
- `escaner_del_carnet_test` es intermitente (ver Evidencia): conviene esperar por condición en vez de 300 ms fijos.
- Las ilustraciones usan colores fijos de la paleta, iguales en los dos temas, porque representan una foto, como el visor de la cámara.

## Decisiones y ambigüedades
- A1: «horripilante» no dice qué parte. Supuesto: las tres piezas de guía de foto (pautas, consejos de cámara y aviso antes de escanear). **Confirmar con Pablo en el teléfono.**
