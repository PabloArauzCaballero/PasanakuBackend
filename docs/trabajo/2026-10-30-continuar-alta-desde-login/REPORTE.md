# Reporte — Continuar un registro a medias desde el login

> **AVANCE: 5 / 5 — 100,0 %.** Build 202610301458 en TestFlight con «Continuar mi registro». PRs #337 (test) y #338 (dev) mergeados por Pablo con el CI todavía corriendo.

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Ramas: `claude/continuar-alta-desde-login-20261030` (#337), `claude/continuar-alta-desde-login-dev-20261030` (#338)
- Peldaño de evidencia: **TESTED** (tests dirigidos con el servidor simulado + regresión sin fallos nuevos + capturas claro/oscuro). No `VERIFIED`: no se probó contra el servicio `identidad` real ni en un teléfono.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Login: «¿Dejaste tu registro a medias? Continuar mi registro» → `/registro?continuar=1` | `flutter test test/identidad/continuar_alta_test.dart` | PASS · [tests-dirigidos.txt](./evidencia/tests-dirigidos.txt) |
| H1.S1.M2 | Paso 1 en modo continuar: aviso + pide código | ídem | PASS · capturas `evidencia/login-*.png`, `paso1-continuar-*.png` |
| H1.S1.M3 | Al confirmar el código vuelve al paso pendiente con lo cargado; borrador vacío y código rechazado se manejan | ídem (3 niveles) | PASS |
| H1.S1.M5 | Build 202610301458 subido; PRs #337 y #338 mergeados | `gh pr view 337/338 --json state` | MERGED · `Upload succeeded` |
| H1.S1.M4 | Regresión sin fallos nuevos frente a `origin/test` | `flutter test` unidad…goldens, árbol limpio vs cambios | PASS · [regresion.txt](./evidencia/regresion.txt) |

## A medias
ninguna

## Pendiente
ninguna

## Evidencia
```text
$ flutter test test/identidad/continuar_alta_test.dart
00:00 +6: All tests passed!
$ regresión (árbol limpio vs cambios): fallos 283 → 282, nuevos: ninguno
$ docker (ubuntu:24.04 amd64, Flutter 3.44.8) flutter test test/identidad/motivo_de_salida_en_ingreso_test.dart
00:02 +3: All tests passed!
$ xcodebuild -exportArchive … (build 202610301458)
Progress 100%: Upload succeeded.
** EXPORT SUCCEEDED **
```

## No cubierto
- Servicio `identidad` real: el servidor se simuló con el adaptador de Dio (doble declarado en el test). Falta probar contra TEST que `confirmarVerificacionCorreo` devuelva el borrador.
- No se abrió el build en un teléfono.
- El caso «cuenta ya creada con KYC incompleto» (`usuarioIncompleto` en el backend) no se ejercitó.

## Desvíos del plan
- **Hipótesis refutada:** creí que un código ya confirmado guardado en el teléfono se saltaba la confirmación y perdía el borrador. El test mostró que al escribir el correo `actualizarDatos` olvida lo guardado (`notifier_alta.dart:21-26`), así que el borrador ya volvía entrando por «Crear mi cuenta». Lo que faltaba era solo la entrada desde el login y el aviso.
- Se regeneraron 2 goldens del ingreso en Linux (fuera del plan original) porque bloqueaban el CI. Se agregó al trabajo, no se hizo «de paso».

## Riesgos residuales
- Los PRs se mergearon con 3 checks pendientes (goldens Linux, navegación iOS y release iOS). 32 goldens del ingreso, la billetera y la bienvenida se regeneraron en Linux y pasaron en el contenedor; `h8_prueba_vida_oscura` falla en Linux y no es de este trabajo (no se tocó).
- La contraseña se elige de nuevo al retomar (el borrador no la guarda). Es a propósito, pero puede sorprender.
- Otros goldens locales fallan en Mac y pasan en Linux. Regenerar goldens en Mac los rompe en el CI.

## Decisiones y ambigüedades
- Enlace siempre visible en el login, no solo cuando hay registro guardado en el teléfono: el borrador vive en el servidor y se puede retomar desde otro teléfono. A confirmar con Pablo.
- Volver a elegir contraseña al retomar: supuesto aceptable porque la cuenta todavía no existe. A confirmar con Pablo.
