# Reporte — App móvil: portada orientada a conversión (tanda 1)

> **AVANCE: 7 / 7 — 100 %.** Tanda 1 (portada → alta) cerrada. Dos pruebas de «enlace del grupo» y 16 goldens de billetera/saldo fallan **también en `dev` sin este cambio** (ver Evidencia).

- Fecha: 2026-10-28 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/movil-rediseno-marketing`
- Peldaño: **TESTED** + prueba visual en web (Flutter compilado a web, perfil, contra el simulado). No `VERIFIED` en dispositivo: no hay teléfono ni emulador en este entorno.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M3a | «Crear mi cuenta» abre el alta directo; el tour queda en «¿Cómo funciona?» | `flutter test test/widget/portada_test.dart` | 12/12 PASS |
| H1.M3b | Bloque «Para abrir tu cuenta vas a necesitar» en la portada | idem | PASS (prueba nueva) |
| H1.M3c | Goldens de la portada regenerados con Flutter 3.44.8 (la del CI) | `flutter test test/goldens/h10_portada_visual_test.dart test/goldens/h8_portada_mfa_oscuro_test.dart` | 8/8 PASS |
| H1.M3d | Suites completas sin regresiones nuevas | `flutter test` front · a11y · goldens | front 795 PASS / 2 FAIL previos · a11y PASS · goldens 258 PASS / 16 FAIL previos |
| H1.M4 | Capturas antes/después | Playwright sobre `flutter build web --profile` | `evidencia/portada-antes-despues.png` |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Sacar «Confirmar contraseña» del paso 1 del alta | TODO (DECISION_REQUIRED) | Confirmación de Pablo |
| Tandas siguientes: alta paso a paso, primer grupo, resto de la app | TODO | — |
| Fallas previas de `pantallas_enlace_del_grupo_test` y goldens de billetera/saldo | TODO | Fuera de alcance; fallan en `HEAD~1` igual |

## Evidencia
```text
dart analyze --fatal-infos → No issues found!
python3 scripts/verificar_frontend.py movil → TODO OK
portada_test → +12: All tests passed!
goldens portada (3.44.8, línea base sin cambio) → +8: All tests passed!
front (3.44.8) → +795 -2 (pantallas_enlace_del_grupo_test: 2)
  mismas 2 + 2 de saldo fallan en HEAD~1 (sin este cambio): +11 -4
a11y → +6: All tests passed!
goldens → +258 -16 (h10_billetera ×12, h10_texto_200, h8_break_ui, pantalla_de_saldo ×2: ninguno de la portada)
```

## No cubierto
- Dispositivo real Android/iOS: se vio la misma app compilada a web.
- Integración (`patrol`), no corrida.

## Desvíos del plan
- Se instaló además Flutter 3.44.8 porque los goldens del CI se generan con esa versión.

## Riesgos residuales
- Con letra al 130 % en pantallas chicas, el bloque nuevo queda debajo del pliegue (la portada ya desplazaba).

## Decisiones y ambigüedades
- Ver PLAN, sección Decisiones. Los textos no prometen tiempo exacto ni usan testimonios (no hay datos medidos).
