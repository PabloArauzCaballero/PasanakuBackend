# Plan — Guías de captura sobrias e identidad visual institucional (app móvil)

- Fecha: 2026-10-15 · Repos afectados: Pasanaku (monorepo) · Rama: `pablo/feat/visual-bancario-2026-10-15` (desde `origin/test` 51982f75) · Worktree: `.claude/worktrees/visual-bancario`
- Predecesor: `2026-10-11-alta-atlas-sesion-premium` (introdujo silueta de cara, guía de encuadre y carrusel de capturas)
- Resultado observable: quien abre la app ve una identidad de banco (neutros fríos, un solo color de acción verde institucional, tipografía sobria, botones planos) y, en el alta, guías de cámara limpias: un óvalo único para el rostro (sin orejas, sin cruz, sin esquinas encima) con indicación de giro en los perfiles, y un contorno de carnet ID-1 completo.
- Kill-test: regenerar el golden `h10-billetera-principal-claro-412` y `silueta_claro_izquierda`; si aparece naranja en un botón, un labio/halo bajo el botón, Bricolage, orejas o nariz dibujada, NO está hecho.

## Pedido literal (2026-10-15)
1) «Las pantallas de guía están horribles: las caras y la orquestación de carnet están pésimas, corregilo.»
2) «Corregí sustancialmente los visuales de la app… imagen institucional de un banco serio… parece de un videojuego… transversalmente.»

## Hallazgos (fase 1, con evidencia)
- Silueta de rostro: `apps/movil/lib/pantallas/identidad/pasos_alta/trazos_de_cara.dart` dibuja huevo con orejas + cruz punteada (frente) y perfil con nariz y oreja rellena. Golden `test/widget/goldens/silueta_claro_*.png`.
- Doble guía: `pantalla_prueba_de_vida.dart:171-176` apila `SiluetaDeCara` **y** `GuiaDeEncuadre(esDocumento:false)` (cuatro esquinas).
- Carnet: `guia_de_encuadre.dart` sólo pinta cuatro esquinas sueltas; `tarjeta_de_captura.dart:70` usa degradé + ícono en círculo con sombra; `paso_capturas.dart` combina riel de 5 círculos + carrusel + `BarraDePuntos` (dos indicadores de lo mismo).
- «Videojuego»: `atomos/superficie_viva.dart` (degradé, halo del color del botón, brillo que cruza 3 veces), `boton.dart:51-55` primario y **peligro** en naranja `accent` (o700); fondo `crema`; títulos en `Bricolage` 700/800 (grotesca expresiva).
- Fuente de verdad del color/tipo: `docs/Views/Sistema-Diseno/estilos.css` → `packages/tokens/tokens.json` (test `tokens-contra-boveda.spec.ts` exige igualdad) → `generado/tokens.{css,dart}` y `packages/diseno_flutter/lib/tokens/tokens.dart`.
- Skills leídas: `pasanaku-diseno`, `improve-ui`, `color-systems` (completas); el resto de la lista de `AGENTS.md` NO se leyó entera (ver REPORTE, desvíos).

## Alcance
- IN: tokens (bóveda + tokens.json + regenerados), `Boton`/`SuperficieViva`, guías de captura (silueta, encuadre, tarjeta, riel, paso de capturas, prueba de vida), sus tests/goldens.
- OUT: ilustraciones del tour (siguen con o500; se anota), logotipo vectorial (cambia sólo de color por rol), portales Angular (heredan los tokens; no se verifican visualmente en este trabajo), comportamiento de captura/ML Kit.
- Ambigüedades registradas:
  - A1 «transversalmente»: supuesto = toda la app móvil vía tokens compartidos; como los tokens también alimentan web/backoffice, ellos heredan el color. Confirmar con Pablo si los portales deben quedar con la paleta anterior.
  - A2 Identidad: supuesto = se conserva el verde de marca (g-scale sin cambios) y el naranja deja de ser color de acción (queda para datos de turnos e ilustraciones). Confirmar con Pablo.
  - A3 «Peligro» deja de ser naranja y pasa a rojo sólido existente (`rojoSolido`). Confirmar.

## H1 — Guías de captura sobrias
**CA:** Dado el alta en el paso de fotos, cuando la persona ve el carrusel y abre la cámara, entonces ve un único óvalo de rostro (perfiles con flechas de giro hacia el lado pedido) y un contorno de carnet completo con esquinas redondeadas, sin degradés ni esquinas sueltas superpuestas.
**DoD:** goldens de silueta y capturas regenerados y mirados · `flutter test test/widget/silueta_de_cara_test.dart` y tests de capturas en verde.
**Estado:** A MEDIAS — microtareas HECHO; falta verificación en dispositivo real (cámara y ML Kit) y confirmar A1–A3

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Óvalo único + chevrons de giro (`trazos_de_cara`, `silueta_de_cara`) | golden sin orejas/nariz/cruz | `flutter test test/widget/silueta_de_cara_test.dart --update-goldens` + inspección PNG | HECHO |
| H1.S1.M2 | Quitar las esquinas de la prueba de vida | `pantalla_prueba_de_vida` no monta `GuiaDeEncuadre` | `grep GuiaDeEncuadre pantalla_prueba_de_vida.dart` → vacío + golden h10 prueba de vida | HECHO |
| H1.S2.M1 | Encuadre de carnet = contorno ID-1 completo con velo exterior en la cámara | golden cámara muestra rectángulo redondeado completo | `flutter test test/goldens/h10_camara_visual_test.dart --update-goldens` + inspección | HECHO |
| H1.S2.M2 | Tarjeta de captura: esquema de carnet / óvalo, superficie plana | sin degradé ni ícono en círculo | golden `capturas_marco_*` regenerado + inspección | HECHO |
| H1.S2.M3 | Riel de capturas sobrio (casillas redondeadas sin sombra). **Corregido en el camino:** `BarraDePuntos` se conserva (contrato a11y testeado: 48 dp, «Lámina n de 5») | riel sin círculos con sombra | `flutter test test/widget` (capturas) verde | HECHO |

## H2 — Identidad institucional transversal
**CA:** Dado cualquier pantalla de la app, cuando se renderiza, entonces el fondo es neutro frío, las acciones primarias son verde institucional plano, los títulos usan la familia de texto en semibold y ningún botón tiene halo, degradé ni brillo.
**DoD:** `yarn workspace` tokens `test:front` verde · goldens h10 regenerados y mirados (claro/oscuro, 360/412/600) · `dart analyze` sin issues.
**Estado:** A MEDIAS — microtareas HECHO; falta verificación en dispositivo real (cámara y ML Kit) y confirmar A1–A3

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Bóveda + tokens.json: bg neutro, acento = verde, tipografía display = Instrument 600, sombras sobrias | test bóveda verde | `npx vitest run` en `packages/tokens` → PASS | HECHO |
| H2.S1.M2 | Regenerar tokens.css/tokens.dart | `generados-coinciden.spec` verde | `node scripts/a-css.mjs && node scripts/a-dart.mjs` + vitest | HECHO |
| H2.S2.M1 | `SuperficieViva` plana: sin degradé, halo ni brillo; presión sutil | golden de botón plano | goldens `diseno_flutter` regenerados + inspección | HECHO |
| H2.S2.M2 | `BotonVariante.peligro` en rojo sólido | perfil: «Dar de baja» rojo | golden perfil h10 + inspección | HECHO |
| H2.S2.M3 | (agregada) Ilustraciones del tour sin naranja: `Paleta.o500` → `g700` | tour sin distintivo naranja | goldens h10/h8 del tour + inspección | HECHO |
| H2.S2.M4 | (agregada) Anillo del foco de tutorial en `g300` fijo: con el acento verde profundo se perdía en el velo | `tutoriales_a11y_test` verde | `flutter test test/a11y/tutoriales_a11y_test.dart` | HECHO |

## H3 — Verificación y regresión
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Analyze + format | 0 issues | `dart analyze --fatal-infos` en movil y diseno | HECHO |
| H3.S1.M2 | Suites de movil y diseno | verdes salvo fallos preexistentes documentados | `flutter test` (serial) | HECHO |
| H3.S1.M3 | Hoja antes/después mirada | capturas en `evidencia/` | PNG en `evidencia/` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Cambiar tokens compartidos cambia web/backoffice | Portales con otro color sin revisar | Registrado en A1; no se verifica visualmente acá |
| Goldens de otros carriles cambian en masa | Diff grande | Se regeneran y se miran las principales; se declara lo no mirado |
| Tests que afirman la geometría vieja del perfil | Rojo | Se actualizan al nuevo contrato de diseño con igual fuerza (espejo izquierda/derecha) |
