# Reporte — Datos que no se actualizan entre pantallas, teclado colgado y foto propia en el grupo

> **AVANCE: 3 / 3 — 100 %.** El hito queda `A MEDIAS`: falta la prueba visual en el iPhone.

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/datos-entre-pantallas-20261030` (desde `origin/test` `ebe0ceb0`)
- Peldaño de evidencia alcanzado: `TESTED` (tests de widget dirigidos + regresión comparada). En el dispositivo: `UNKNOWN` hasta un build nuevo.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Al tocar una pestaña se suelta el foco (se cierra el teclado) y se vuelven a pedir saldo, movimientos, mis grupos, detalle del grupo, estado del participante y foto | `flutter test test/widget/datos_entre_pantallas_test.dart` | PASS 4/4 — `evidencia/test-dirigido.txt`. Con el arreglo desactivado el test falla (`Expected: <2> Actual: <1>`), así que discrimina |
| H1.S1.M2 | Firmar el reglamento invalida también `misGruposProvider` («Tus pasanakus» y «Mis grupos» dejan de decir «Falta firmar») | `flutter analyze` + suite completa | `No issues found!` — `evidencia/analyze.txt`; 0 fallas nuevas en la suite |
| H1.S2.M1 | La fila propia («Tú») de Integrantes y el detalle del miembro muestran la foto de perfil subida; sin foto o para otra persona, siguen las iniciales | `flutter test test/widget/datos_entre_pantallas_test.dart` | PASS (3 casos de avatar) |

## A medias
### H1 — prueba visual en el dispositivo
- Qué anda: los tres arreglos, demostrados con tests de widget y sin regresiones.
- Qué no anda: no se observó en el iPhone. Las capturas de Pablo son del build anterior.
- Qué falta exactamente: un build de TestFlight con este commit y repetir dos cosas: (1) entrar a Aportar, tocar el monto y tocar «Inicio»: el teclado tiene que cerrarse; (2) abrir un grupo y ver la foto en la fila «Tú».
- Dónde quedó: rama `claude/datos-entre-pantallas-20261030`; compila (`flutter analyze` limpio).

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Foto de OTROS integrantes | fuera de alcance (no planificada) | Contrato nuevo en identidad para leer la foto de otra persona + verificar consentimiento (regla 90.2.8). Decisión de Pablo |

## Evidencia
```text
$ flutter test test/widget/datos_entre_pantallas_test.dart test/pasanaku/acceso_al_traspaso_test.dart
00:02 +6: All tests passed!

$ flutter analyze
No issues found! (ran in 22.7s)

Suite completa apps/movil (--concurrency=1, reporter json)
base origin/test ebe0ceb0: 2570 tests, 353 fallas
con cambios:              2574 tests, 353 fallas
fallas nuevas: 0 · fallas que desaparecen: 0

$ python3 ../../scripts/verificar_frontend.py movil
TODO OK
```
- `evidencia/test-dirigido.txt`, `evidencia/analyze.txt`, `evidencia/regresion-comparada.txt`, `evidencia/suite-completa-cola.txt`, `evidencia/lint.txt`.

## No cubierto
- Ninguna prueba en el dispositivo real (ver A medias).
- El refresco de «Movimientos»: comparte rama con Inicio (`/billetera/extracto`), así que tocarla también refresca, pero no hay test específico.
- Volver con «atrás» dentro de la misma pestaña (sin tocar la barra) no refresca por este mecanismo. Solo firmar tiene invalidación explícita.

## Desvíos del plan
- Los tests de M1 y S2.M1 quedaron en un solo archivo (`datos_entre_pantallas_test.dart`) en vez de dos. El plan se corrigió.
- El worktree necesitó generar `tokens.dart` y los clientes Dart (`./gradlew generateOpenApiClients`, igual que `scripts/testflight_ios.sh`), porque no se versionan.

## Riesgos residuales
- Cada toque de pestaña dispara hasta 6 pedidos GET. Riverpod conserva el valor anterior mientras recarga, así que no hay parpadeo, pero sí más tráfico.
- La suite ya traía 353 fallas preexistentes: 348 en goldens/capturas y 5 en `identidad/componentes_de_fase`, `identidad/motivo_de_salida_en_ingreso` y `widget/paso_capturas`. Tapan regresiones visuales reales.
- `lib/pantallas/identidad/pasos_alta/paso_cuenta.dart` no pasa `dart format` en `origin/test`. Es preexistente y no se tocó.

## Decisiones y ambigüedades
- «Datos que no se actualizan entre pantallas» no nombraba cuáles. Supuesto: saldo, grupos/firma y foto, que es lo que se ve en las capturas. **Confirmar con Pablo** si vio otro dato desactualizado.
- Se refresca al cambiar de pestaña, en un solo lugar (`lib/navegacion/refrescar_al_cambiar_de_pestana.dart`), en vez de agregar invalidaciones en cada pantalla que muta. Es la causa común: el `indexedStack` mantiene vivas las pantallas de atrás.
- No hay datos personales en la evidencia. La foto de los tests es un PNG sintético de 1×1.
