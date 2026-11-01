# Reporte — Rediseño bancario, tanda 3: verificación, reclamo y avisos

> **AVANCE: 6 / 6 — 100 %.** Verificar sorteo, transparencia, reclamo pendiente y bandeja de avisos pasan al lenguaje del rediseño; análisis, gate, pruebas y goldens en verde.

- Fecha: 2026-10-29 · Rama: `claude/rediseno-bancario-tanda-3` · Plan de origen: `docs/trabajo/2026-10-21-rediseno-bancario/PLAN.md` (H6)
- Peldaño: **TESTED** + prueba visual con el arnés de capturas (Flutter 3.44.8, fuentes reales). No VERIFIED en dispositivo.
- Skills: `pasanaku-diseno`, `pasanaku-ui-componentes`, `ux-writing-microcopy`, `sorteo-transparencia`.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| T3.M1 | Molécula `FilaDeDato` (nombre arriba, valor seleccionable con cifras tabulares) con su prueba; una huella de 64 caracteres no desborda a 360 dp con texto al 200 % | `flutter test test/widget/fila_de_dato_test.dart` | +2 PASS |
| T3.M2 | Verificar sorteo: entrada, resultado en `ChipEstado`, «Datos para reproducirlo» en tarjeta con una fila por dato (antes: «Hash: 9f2c…» corrido) | `flutter test test/pasanaku` | PASS (dentro de +1106) |
| T3.M3 | Transparencia: entrada, resultado con ícono, la falla dice qué parte de la cadena se rompió («El enlace con el bloque anterior») en vez del enum `HASH_ANTERIOR` | idem | PASS |
| T3.M4 | Reclamo pendiente con entrada y tarjeta; bandeja de avisos vacía con `EstadoVacio` («Estás al día») | idem | PASS |
| T3.M5 | Gate, análisis y pruebas | `dart analyze --fatal-infos` · `verificar_frontend.py movil/diseno` · `flutter test` (app y diseno) | No issues · TODO OK · app +1106 · diseno +151 |
| T3.M6 | Capturas antes/después, claro y oscuro | arnés (`FASE=tanda3 FLUJOS=grupos,sistema`) | +570 PASS · `evidencia/antes-despues.png`, `evidencia/oscuro.png` |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Resto de H6 (turno, completar KYC, recargar/retirar) | TODO | Próxima tanda |
| H4/H5 del plan (campos con relleno suave, cabecera que se compacta, motion) | TODO | — |
| ¿Quitar «Confirmar contraseña» del paso 1 del alta? | DECISION_REQUIRED | Pablo |

## Evidencia
- `evidencia/antes-despues.png`, `evidencia/oscuro.png`: hojas comparativas del arnés.
- `evidencia/perfil-goldens.png`: goldens de perfil regenerados (ver Desvíos).

## No cubierto
- Dispositivo real. Los textos no afirman nada que la pantalla no compruebe: «Nadie eligió el orden» se cambió por «Comprobá el sorteo vos mismo», y transparencia describe la cadena de bloques sin prometer más.

## Desvíos del plan
- `test/goldens/h8_perfil_oscuro_test.dart` y los goldens de perfil se rompieron en `dev` por la foto de perfil agregada en otro trabajo (#236), que empujó «Cerrar sesión» debajo del pliegue. Se corrigió la prueba con un desplazamiento acotado a la lista de perfil y se regeneraron los goldens con Flutter 3.44.8 (la versión de CI).

## Decisiones
- `FilaDeDato` se agregó a `diseno_flutter`: el patrón nombre/valor seleccionable se repite en verificación y transparencia y no había equivalente.
- Las capturas del arnés no se versionan; solo las hojas comparativas.
