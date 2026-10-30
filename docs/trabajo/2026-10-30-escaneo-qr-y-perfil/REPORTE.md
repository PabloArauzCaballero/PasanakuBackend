# Reporte — Pantalla de escaneo de QR y pantalla de perfil, mejor terminadas

> **AVANCE: 9 / 9 — 100,0 %.** Mergeado en `test` (#351) y `dev` (#352); build 202610301910 subido a TestFlight.

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/escanear-y-perfil-20261030` (desde `origin/test` `edc5f310`)
- Peldaño de evidencia alcanzado: `TESTED` + prueba visual (capturas inspeccionadas a 390 dp claro/oscuro y goldens 360/412/600 dp y tres viewports). Sin corrida en dispositivo físico. 

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `MarcoDeEscaneo`: velo con hueco redondeado, cuatro esquinas que se cierran al abrir (quietas con movimiento reducido), rojas si el QR no es invitación, pista «Centrá el QR dentro del marco» | captura `evidencia/capturas/despues/escanear_*.png` | inspeccionada |
| H1.S1.M2 | Visor cuadrado (máx. 360) + enlace manual en tarjeta «¿No podés escanear?» | `flutter test test/widget/pantalla_escanear_test.dart test/widget/acceso_escanear_test.dart` | `+11: All tests passed!` |
| H1.S1.M3 | Goldens de escaneo (denegado, 3 viewports × 2 temas) regenerados e inspeccionados | `flutter test test/goldens/pantalla_escanear_golden_test.dart` | `+6: All tests passed!` |
| H2.S1.M1 | Cabecera verde del perfil (patrón de `EncabezadoDeSaldo`): avatar con aro e insignia de cámara (tocar abre lo mismo que el botón), nombre, botón de foto con ícono | `flutter test test/widget/pantalla_perfil_foto_test.dart` | en verde (ver M2) |
| H2.S1.M2 | Sección «Tu cuenta» con verificación y correo; más aire entre secciones | `flutter test test/widget/pantalla_perfil_{correo,foto,cerrar_sesion}_test.dart` | `+10: All tests passed!` |
| H2.S1.M3 | Goldens de perfil regenerados (solo los de perfil) e inspeccionados, incluido correo de 150 caracteres al 200 % | `flutter test test/goldens/h10_perfil_visual_test.dart test/goldens/h8_perfil_oscuro_test.dart` · `--plain-name "perfil ·" test/goldens/h8_break_ui_test.dart` | `+8` y `+1: All tests passed!` |
| H3.S1.M1 | Regresión completa comparada contra `origin/test` limpio | `flutter test` (con y sin el cambio, `git stash`) | **0 fallas nuevas** (`evidencia/fallas-nuevas.txt` vacío) |
| H4.S1.M1 | PR #351 a `test` y gemelo #352 a `dev` (cherry-pick), mergeados; en `dev`: analyze limpio, 36 dirigidos en verde, suite con 338 fallas **todas ya presentes en `test` limpio** | `gh pr view 351/352 --json state` | `MERGED` / `MERGED` |
| H4.S1.M2 | Build **202610301910** de `origin/test` `5a73604a` (incluye #351) subido a App Store Connect, apuntando a la API de TEST | `bash scripts/testflight_ios.sh --solo-compilar` · `xcodebuild -exportArchive … -authenticationKey*` | `Upload succeeded` · `EXPORT SUCCEEDED` ([evidencia/testflight-subida.txt](./evidencia/testflight-subida.txt)) |

## A medias
ninguna

## Pendiente
ninguna

## Evidencia
```text
$ flutter analyze lib/pantallas
No issues found!
$ flutter test <8 archivos dirigidos de escaneo y perfil>
00:02 +35: All tests passed!
$ flutter test --plain-name "perfil ·" test/goldens/h8_break_ui_test.dart
00:00 +1: All tests passed!
$ flutter test   (suite completa)
con el cambio: 338 fallas · origin/test limpio: 353 fallas · nuevas: 0
```
- `evidencia/suite-completa-{con-cambio,linea-base}.log`, `fallas-{con-cambio,linea-base,nuevas}.txt`
- `evidencia/capturas/{antes,despues}/{escanear,perfil}_{claro,oscuro}.png` (cuenta sintética «Ana Mamani Quispe», `ana@correo.test`; inventada)

## No cubierto
- El procesado del build en App Store Connect lo hace Apple (unos minutos); no se instaló ni se abrió el build en un teléfono.
- No se probó en un teléfono real: la cámara de verdad bajo el marco, el toque sobre el avatar, el cambio a rojo con un QR ajeno (cubierto por código, no capturado).
- El estado de verificación con sesión real (la captura muestra el texto sin sesión).

## Desvíos del plan
- `AvatarConAro` quedó en su archivo (`avatar_con_aro.dart`) para respetar el tope de 200 líneas de `yarn lint`.
- La sombra de la cabecera es la de tarjeta (`Elevacion.tarjeta`), no la de héroe: en claro la de héroe se veía como un bloque gris debajo.
- Se generaron en el worktree los clientes Dart y los tokens (no versionados) para poder compilar; los barriles de `clientes/dart/{aportes,notificaciones,tarifas}` y un `registro.tsv` que escribe un test se restauraron y no van en el commit.

## Riesgos residuales
- El IPA se compiló con los barriles de `clientes/dart` regenerados desde los contratos de test (lo mismo que hace una máquina limpia), no con los versionados, que están atrasados.
- **`origin/test` ya tiene 353 tests en rojo** (capturas de billetera, alta y otras pantallas) y `yarn lint` en rojo por `apps/movil/lib/dominio/errores.dart` (203 líneas). Ninguno es de este cambio; quedan para quien sea dueño de esas pantallas.
- Los barriles generados de `clientes/dart` versionados en test están atrasados respecto de los contratos (faltan exports de `proveedor_de_pago_resumen`, `webhook_recibido`, `evento_de_entrega_recibido`, `proveedor_de_mensajeria_resumen`, `costo_de_proveedor`).

## Decisiones y ambigüedades
- «Mucho mejor» sin estilo pedido → se siguió el sistema existente: tokens `camara*` del visor de identidad y la cabecera verde del saldo. Confirmar con Pablo sobre las capturas.
- Sin animación en bucle (línea de barrido): la cámara ya se mueve y la pantalla tiene que asentarse.
- El campo «Correo» deshabilitado se mantuvo: el test de correo (2026-10-19) lo exige como contrato.
- El mensaje de otra sesión (pasanaku-1f) sobre la regla nueva del sorteo CU-60 no aplica a este alcance.
