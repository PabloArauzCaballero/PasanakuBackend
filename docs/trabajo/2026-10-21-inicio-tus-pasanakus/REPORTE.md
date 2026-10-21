# Reporte — «Tus pasanakus» en el inicio con datos reales

> **AVANCE: 2 / 3 — 66,7 %.** La sección del inicio está hecha y probada en local; falta llevarla a `test` y generar el APK (H1.S2.M1).

- Fecha: 2026-10-21 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/inicio-tus-pasanakus-20261021`
- Peldaño de evidencia: `TESTED` (pruebas de widget con el cliente HTTP simulado + capturas miradas). No `VERIFIED`: no se probó en un teléfono contra TEST.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `ResumenDePasanakus` reemplaza el texto fijo del inicio: un aviso por grupo que pide algo (falta firmar, aporte atrasado, suspendido) que abre ese grupo; «Estás en N grupos · todo al día» + «Ver mis grupos»; sin grupos, «Unirme a un grupo» / «Crear un grupo»; cargando y error con reintento propios. La navegación al grupo se extrajo a `abrir_mi_grupo.dart` y la usan Grupos y el inicio | `flutter test test/billetera/resumen_de_pasanakus_test.dart` | PASS 5/5 |
| H1.S1.M2 | Regresión | `flutter test test/billetera test/pasanaku test/a11y` · `flutter test test/widget` | PASS 100/100 · widget: todas salvo comparaciones de imagen en Windows (ver No cubierto) |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S2.M1 | TODO | PR a `dev`, llevar a `test`, APK |

## Evidencia
Capturas en [evidencia/](./evidencia/): avisos, al día y sin grupos, a 360 dp, claro y oscuro (datos sintéticos).

## No cubierto
- Las comparaciones de imagen (`test/goldens/h8_break_ui_test.dart`, `pautas_publicas_golden_test.dart`, `paso_capturas_test.dart`) fallan en Windows en pantallas que este cambio no toca (alta, perfil, capturas): es la diferencia conocida entre Windows y Linux. Las tres de billetera **sí** cambian con esta sección (ahora muestran «Tus pasanakus» real). Se regeneran solo en Linux (memoria del proyecto); la prueba ya simula «sin grupos» para que la imagen sea estable.
- El CI no corre las pruebas de la app mientras el job de Frontend se corte antes, en el formato de `diseno_flutter` (de Pablo).

## Desvíos del plan
- `test/widget/pantalla_de_saldo_test.dart` y `test/goldens/h8_break_ui_test.dart`: se les agregó la respuesta de `/grupos/participaciones`, porque el inicio ahora la pide. Sin ella, el reintento de la sección confundía la aserción del reintento del saldo.

## Riesgos residuales
- «Próximo aporte» (fecha y monto) no está: necesita el calendario de cada grupo. Segundo paso.
- Los estados que no piden nada (activo, en proceso) cuentan para «Estás en N grupos».

## Decisiones y ambigüedades
- Del dueño (2026-10-21): el inicio no repite la pestaña Grupos; resume lo que pide atención.
