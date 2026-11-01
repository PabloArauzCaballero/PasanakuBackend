# Plan — «Ver quién pidió unirse» pasa de «Invitar» a la pantalla del grupo

- Fecha: 2026-10-28 · Repos afectados: Pasanaku (`apps/movil`) · Rama: `richard/fix/boton-solicitudes-en-grupo` (desde `origin/dev`) · Predecesor: ninguno
- Resultado observable: el organizador ve «Ver quién pidió unirse» en la pantalla de su grupo, justo debajo de «Invitar a alguien a mi grupo»; la pantalla «Invitar» ya no muestra ese botón y solo deja el QR y el enlace.
- Kill-test: `grep -n "verSolicitudes" apps/movil/lib/pantallas/pasanaku/vista_enlace_del_grupo.dart` devuelve algo, o el test del grupo no encuentra el botón debajo de «Invitar».

## Alcance
- IN: `vista_enlace_del_grupo.dart` (quitar el botón y su callback), `pantalla_enlace_del_grupo.dart` (quitar el callback y el import de `go_router` que queda sin uso), `seccion_del_grupo.dart` (agregar el botón debajo de «Invitar», misma condición), `test/pasanaku/detalle_del_grupo_test.dart` (aserciones del botón).
- OUT: QR, enlace, compartir, copiar, generar otro y anular de «Invitar» (pedido explícito del usuario: no tocarlos); la pantalla de solicitudes y su ruta; backend.
- Ambigüedades registradas: con el grupo lleno «Invitar» se oculta. Decisión del usuario (2026-10-28): el botón de solicitudes se oculta junto con «Invitar» (`soyOrganizador && !completo`).

## H1 — El botón vive en la pantalla del grupo
**CA:** Dado el organizador de un grupo con cupos libres, cuando abre el grupo, entonces ve «Ver quién pidió unirse» debajo de «Invitar a alguien a mi grupo» y al tocarlo llega a `/pasanaku/grupos/{id}/solicitudes`; un participante que no organiza no lo ve; con el grupo lleno no se ve.
**DoD:** microtareas en HECHO + tests de `test/pasanaku` en verde + APK de prueba generado.
**Estado:** A MEDIAS — falta la prueba del usuario en su celular

### H1.S1 — Mover el botón
**CA:** igual que H1.
**DoD:** analyzer limpio y tests dirigidos en verde.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Quitar el botón de la vista de «Invitar» | `verSolicitudes` no aparece en la vista ni `alVerSolicitudes` en la pantalla | `grep -rn "alVerSolicitudes" apps/movil/lib` → sin salida | HECHO |
| H1.S1.M2 | Agregar el botón debajo de «Invitar» en el grupo | Analyzer limpio en los tres archivos | `flutter analyze lib/pantallas/pasanaku` → `No issues found!` | HECHO |
| H1.S1.M3 | Tests: aparece para el organizador y navega; no aparece para participante ni con grupo lleno | Tests en verde | `flutter test test/pasanaku/detalle_del_grupo_test.dart test/pasanaku/pantallas_enlace_del_grupo_test.dart` → `All tests passed!` | HECHO |
| H1.S1.M4 | APK de prueba para el celular del usuario | El APK existe | `flutter build apk` → `Built build/app/outputs/flutter-apk/...apk` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Goldens de «Invitar» o del grupo cambian | CI de goldens en rojo | Correr los tests de goldens de pasanaku y declarar el resultado |
| El APK apunta a un entorno distinto al que usa el usuario | Prueba no representativa | Usar los mismos `--dart-define` del workflow `apk-de-prueba.yml` |
