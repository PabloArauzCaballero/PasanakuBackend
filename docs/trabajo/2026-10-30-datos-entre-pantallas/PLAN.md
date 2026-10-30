# Plan — Datos que no se actualizan entre pantallas, teclado colgado y foto propia en el grupo

- Fecha: 2026-10-30 · Repos afectados: monorepo Pasanaku (`apps/movil`) · Predecesor: `2026-10-28-foto-de-perfil`
- Rama: `claude/datos-entre-pantallas-20261030` (desde `origin/test` `ebe0ceb0`, worktree `../Pasanaku-wt-datos`)
- Pedido de Pablo 2026-10-30, con dos capturas del iPhone: «no se ve la imagen que ya había subido, hay datos que no se actualizan entre pantallas». Captura 1: Inicio con el teclado numérico abierto encima. Captura 2: en «Integrantes» del grupo, la fila propia («Tú») muestra iniciales «LL» en vez de la foto.
- Resultado observable: al cambiar de pestaña, Inicio/Grupos/Perfil vuelven a pedir saldo, grupos y foto; el teclado se cierra; en la lista de integrantes la fila propia muestra la foto subida.
- Kill-test: firmar en un grupo, tocar «Inicio» y que «Tus pasanakus» siga diciendo «Falta firmar»; o tocar Inicio desde Aportar y que el teclado quede abierto.

## Hechos (descubrimiento)
- El shell es `StatefulShellRoute.indexedStack` (`lib/navegacion/rutas.dart:49`): cada rama queda montada por detrás. Los `FutureProvider.autoDispose` de saldo (`dominio/cu13_consultar_saldo.dart:12,28`), grupos (`pasanaku/dominio/mis_grupos.dart:45`), detalle (`pasanaku/dominio/detalle_del_grupo.dart:11`) y foto (`identidad/dominio/foto_de_perfil.dart`) siguen escuchados → **nunca se vuelven a pedir** al volver a la pestaña.
- El cambio de pestaña (`lib/navegacion/shell.dart` `onChanged`) hace `context.go` sin soltar el foco; no hay ningún `unfocus` en `lib/` → el `CampoMontoGrande` de la rama que queda detrás conserva el foco y el teclado de iOS sigue abierto.
- `pantalla_firmar_reglamento.dart:48` invalida solo `detalleDelGrupoProvider`, no `misGruposProvider` (que alimenta «Tus pasanakus» y «Mis grupos»).
- `tarjeta_de_integrante.dart:58` y `pantalla_detalle_miembro.dart:127` dibujan `Avatar(nombre:)` sin imagen, aunque el integrante sea `soyYo`.
- El backend solo expone la foto **propia** (`GET /usuarios/yo/foto-de-perfil`); no hay contrato para la foto de otra persona.

## Alcance
- IN: soltar foco + refrescar datos compartidos al cambiar de pestaña; invalidar `misGruposProvider` al firmar; foto propia en la tarjeta de integrante y en el detalle del miembro.
- OUT: foto de OTRAS personas (exige contrato nuevo en identidad + consentimiento, regla 90.2.8); cambios en los arreglos sin commitear de la rama `pablo/feat/backoffice-como-atlas-h0-h1-2026-10-18`.
- Ambigüedad: «datos que no se actualizan» no dice cuáles. Supuesto: saldo, grupos/firma y foto (lo que muestran las capturas). Confirmar con Pablo si vio otro dato desactualizado.

## H1 — Lo que cambia en una pantalla se ve en las otras
**CA:** Dado que firmo, aporto o subo foto, cuando cambio de pestaña, entonces veo el dato nuevo y el teclado cerrado; mi fila en el grupo muestra mi foto.
**DoD:** tests de widget dirigidos en verde + `flutter analyze` sin errores nuevos + suite de la app.
**Estado:** A MEDIAS — microtareas 3/3 HECHO; falta la prueba visual en el iPhone (build nuevo)

### H1.S1 — Navegación y frescura
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Al cambiar de pestaña: `unfocus` + invalidar saldo, movimientos, grupos, detalle, estado del participante y foto | El teclado se cierra y los proveedores se vuelven a pedir | `flutter test test/widget/datos_entre_pantallas_test.dart` verde | HECHO |
| H1.S1.M2 | Firmar invalida también `misGruposProvider` | Tras firmar, mis grupos se piden de nuevo | `flutter analyze` sin errores nuevos + suite | HECHO |
### H1.S2 — Foto propia en el grupo
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Tarjeta de integrante y detalle del miembro usan la foto si `soyYo` | Con foto → imagen; sin foto o ajeno → iniciales | `flutter test test/widget/datos_entre_pantallas_test.dart` verde | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Refrescar en cada toque de pestaña suma pedidos | Algo más de tráfico | Riverpod 3 conserva el valor previo mientras recarga: no hay parpadeo de «cargando» |
| La prueba en el iPhone exige build nuevo a TestFlight | La verificación visual real queda para Pablo | Se declara `VERIFIED_FUNCTIONAL_ONLY` hasta tener captura del dispositivo |
