# Plan — Detalle del grupo, firma del reglamento y sorteo con ruleta

- Fecha: 2026-10-20 · Repos: PasanakuBackend (grupos, apps/movil) · Predecesor: `docs/trabajo/2026-10-20-unirse-por-qr-y-enlace/`
- Resultado observable: dentro de un grupo se ve el resumen (monto, periodicidad, cupos X/N, estado) y una tarjeta por cupo con nombre, estado y turno; quien está pendiente firma el reglamento desde ahí; cuando todos los cupos están firmados el organizador toca «Iniciar sorteo», ve la ruleta girar y queda el orden de cobro en las tarjetas.
- Kill-test: un grupo de 3 cupos con organizador + 2 aceptados que firman no llega a «Iniciar sorteo», o el sorteo devuelve AP-CU60-09.

## Hallazgos (2026-10-20)
- Nadie pasa a un participante de `ACEPTADO_PENDIENTE_FIRMA` a `ACTIVO` salvo el canje por número (`CU69Enlace.aceptar`): por enlace o como organizador se queda en «Falta firmar» para siempre.
- El organizador nace participante SIN cupo (`CreacionRepositorio.guardarAlta`).
- Nadie crea `grupos.periodo`: `CU60Sortear.revelar` exige un periodo por cupo (AP-CU60-09).
- Nadie pasa el grupo a `CONFORMADO`.
- `participante.alias VARCHAR(60)` existe y no se usa: guarda el nombre visible.
- La ruleta solo existe en `docs/Views/AportaYa-Maqueta.html`; los proveedores de CU-60 en la app no tienen pantalla.

## Decisiones del dueño (2026-10-20)
- El organizador ocupa el cupo 1 y entra al sorteo.
- En las tarjetas todos ven nombre, estado y turno; nadie ve teléfono ni correo de los demás.

## Ambigüedades (supuesto · a quién confirmar)
- Calendario: el periodo n empieza en `fecha_inicio + (n-1)·paso` (SEMANAL 7 d, QUINCENAL 15 d, MENSUAL 1 mes, BIMENSUAL 2 meses); vence el último día del paso; gracia = `dias_gracia`; entrega al día siguiente de la gracia. Se crea al conformarse el grupo. · producto.
- El sorteo se revela en el acto (sin fecha comprometida a futuro): `CU60Sortear.revelar` lo permite. · producto.

## Alcance
- IN: firmar reglamento (endpoint + app); organizador en el cupo 1 con su nombre; nombre en el participante al aprobar; grupo CONFORMADO y calendario al completar firmas; `GET /grupos/{id}/detalle`; pantalla del grupo con tarjetas; sorteo con ruleta.
- OUT: canje por número (no guarda nombre); aportes y cobros; repetir sorteo; producción.

## H1 — Servidor
**CA:** Dado un grupo de N cupos, cuando el organizador y N-1 aceptados firman, entonces el grupo queda CONFORMADO con N periodos, el detalle lo dice y el sorteo crea N turnos.
**DoD:** `:servicios:grupos:webTest` y `:servicios:grupos:test` verdes; `integrationTest` de grupos en GitHub (flujo temporal); bóveda y criterios OK.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | Organizador ocupa el cupo 1 (RESERVADO) con su nombre en `alias` | al crear, cupos ocupados = 1 | `CU20Test` (CI) | TODO |
| H1.M2 | Aprobar una solicitud copia el nombre al `alias` | el participante nace con nombre | `CU68`/`CU69EnlaceDeGrupoTest` (CI) | TODO |
| H1.M3 | Firmar el reglamento (AP-CU69-05/13): ACTIVO + cupo OCUPADO; con todos, CONFORMADO + calendario | firma idempotente; última firma conforma | `CU69FirmaTest` (CI) + webTest | TODO |
| H1.M4 | `GET /grupos/{id}/detalle` solo para integrantes | ajeno → 422; tarjetas con nombre/estado/turno | webTest + `DetalleDelGrupoTest` (CI) | TODO |
| H1.M5 | Sorteo de punta a punta con el calendario generado | comprometer + revelar crea N turnos | `CU69FirmaTest` (CI) | TODO |

## H2 — App
**CA:** Dentro del grupo se ven resumen y tarjetas; «Firmar el reglamento» si me toca; «Iniciar sorteo» para el organizador con el grupo completo; la ruleta gira y muestra el orden.
**DoD:** `flutter analyze` sin problemas; tests de pantalla (correcto, límite, inválido); goldens regenerados en Linux; APK.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.M1 | Detalle del grupo: resumen + tarjetas + hoja de información | N tarjetas, libres incluidas | test de pantalla | TODO |
| H2.M2 | Firmar el reglamento | firma y refresca | test de pantalla | TODO |
| H2.M3 | Iniciar sorteo + ruleta + orden | comprometer→revelar→ruleta→orden | test de pantalla | TODO |
| H2.M4 | APK | APK en Descargas | `flutter build apk` exit 0 | TODO |
