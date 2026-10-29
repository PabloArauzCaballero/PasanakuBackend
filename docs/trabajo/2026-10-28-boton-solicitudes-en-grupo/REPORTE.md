# Reporte — «Ver quién pidió unirse» pasa de «Invitar» a la pantalla del grupo

> **AVANCE: 4 / 4 — 100 %.**

- Fecha: 2026-10-28 · Plan: [PLAN.md](./PLAN.md) · Rama: `richard/fix/boton-solicitudes-en-grupo` (desde `origin/dev`)
- Peldaño de evidencia alcanzado: `TESTED` (tests de widget y APK compilado). El hito queda en A MEDIAS hasta que el usuario lo pruebe en su celular contra TEST.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | «Invitar» ya no tiene el botón ni su callback | `grep -rn "alVerSolicitudes" apps/movil/lib` | PASS, sin salida — `evidencia/m1-grep.txt` |
| H1.S1.M2 | El botón está en `seccion_del_grupo.dart`, debajo de «Invitar a alguien a mi grupo», con la misma condición (`soyOrganizador && !completo`) | `flutter analyze lib/pantallas/pasanaku …` | PASS, `No issues found!` — `evidencia/m2-analyze.txt` |
| H1.S1.M3 | Test nuevo: el botón va debajo de invitar y abre `/pasanaku/grupos/{id}/solicitudes`; no aparece con el grupo lleno ni para quien no organiza | `flutter test test/pasanaku` | PASS, 90/90 — `evidencia/m3-m4-tests-y-apk.txt` |
| H1.S1.M4 | APK release contra TEST (mismos `--dart-define` que `apk-de-prueba.yml`) | `flutter build apk --release …` | PASS, `app-release.apk (110.0MB)` — `evidencia/m3-m4-tests-y-apk.txt` |

## A medias
### H1 — Verificación en el dispositivo
- Qué anda: tests de widget en verde y APK compilado.
- Qué no anda: nada observado.
- Qué falta exactamente: que el usuario instale el APK, entre como organizador a un grupo con cupos libres y confirme que ve el botón debajo de «Invitar» y que «Invitar» solo muestra QR y enlace.
- Dónde quedó: rama `richard/fix/boton-solicitudes-en-grupo`; compila.

## Pendiente
ninguna

## Evidencia
Ver `evidencia/`: `m1-grep.txt`, `m2-analyze.txt`, `m3-m4-tests-y-apk.txt`.

## No cubierto
- Prueba manual en dispositivo real contra TEST (la hace el usuario).
- Capturas por tema claro/oscuro: el botón reutiliza `Boton` con `BotonVariante.secundario`, igual que antes; no hay goldens de estas pantallas.

## Desvíos del plan
- Para compilar localmente hubo que generar tokens y clientes Dart (`generateOpenApiClients` y `apps/movil/scripts/generar-clientes.sh`, los mismos pasos de la CI). Lo que esos pasos cambiaron en archivos versionados de `clientes/` (fin de línea) no se incluyó en el commit.
- Flutter local 3.47.8; el repo fija 3.44.8 (la CI usa 3.44.8).

## Riesgos residuales
- Con el grupo lleno el organizador no ve el botón (decisión del usuario): las solicitudes que quedaron pendientes no se ven hasta que se libere un cupo.

## Decisiones y ambigüedades
- Botón oculto junto con «Invitar» cuando el grupo está lleno — decidido por el usuario el 2026-10-28.
- Prefijo de rama `richard/` y PR hacia `dev` — pedido del usuario.
