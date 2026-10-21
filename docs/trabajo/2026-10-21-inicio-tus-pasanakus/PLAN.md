# Plan — «Tus pasanakus» en el inicio con datos reales

- Fecha: 2026-10-21 · Repo: PasanakuBackend (`apps/movil`) · Rama: `claude/inicio-tus-pasanakus-20261021` (desde `origin/dev`)
- Resultado observable: en el inicio (billetera), «Tus pasanakus» deja de decir siempre «Todavía no estás en ningún pasanaku». Muestra un aviso por cada grupo que pide algo (falta firmar, aporte atrasado, suspendido), y tocarlo abre ese grupo. Si todo está al día, una línea «Estás en N grupos · todo al día» con «Ver mis grupos». Si no está en ninguno, el mensaje con atajos para unirse o crear.
- Kill-test: con un grupo «Falta firmar», el inicio sigue diciendo «Todavía no estás en ningún pasanaku».

## Hallazgos
- `apps/movil/lib/pantallas/billetera/cuerpo_de_saldo.dart:57-63`: la sección es un texto fijo (`TextosBilletera.sinPasanakus`); nunca consulta los grupos.
- Los grupos del usuario: `misGruposProvider` (`pantallas/pasanaku/dominio/mis_grupos.dart`, `GET /grupos/participaciones`), con `estado` de la participación.
- Abrir un grupo: `pantalla_mis_grupos.dart:76-90` (`/pasanaku/grupos/{id}` con participante, usuario y nombre). Pestaña Grupos: `/pasanaku`. Unirse: `/pasanaku/unirse-a-un-grupo`. Crear: `/pasanaku/grupos/nuevo`.

## Alcance
- IN: la sección del inicio (widget nuevo con sus estados), textos, la navegación al grupo reusada, pruebas de widget, capturas.
- OUT: «próximo aporte» con fecha y monto (necesita el calendario de cada grupo: segundo paso); backend; la pestaña Grupos.
- Decisión del dueño (2026-10-21): el inicio no repite la lista de Grupos; resume lo que pide atención.

## H1 — El inicio muestra lo que pide atención de tus grupos
**CA:** Dado un usuario con un grupo «Falta firmar» y otro activo, cuando abre el inicio, entonces ve un aviso «<grupo> · Te falta firmar el reglamento» que abre ese grupo, y no ve «Todavía no estás en ningún pasanaku».
**DoD:** `flutter test` dirigido + `test/billetera` + a11y en verde · `verificar_frontend.py movil` sin fallas nuevas · capturas 360/412/600 claro/oscuro miradas.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Widget `ResumenDePasanakus` (avisos / al día / ninguno / cargando / error) en el inicio, reusando `misGruposProvider` y la navegación al grupo | los cinco estados se ven y el aviso abre el grupo | `flutter test test/billetera/resumen_de_pasanakus_test.dart` | HECHO (5/5) |
| H1.S1.M2 | Regresión del inicio y de Grupos | sin fallas | `flutter test test/billetera test/pasanaku test/a11y` | HECHO (100/100) |
| H1.S2.M1 | PR a `dev`, llevar a `test` y APK | APK en Descargas | `gh pr checks` + `gh run download` | TODO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Si `/grupos/participaciones` falla, el inicio no puede romperse | saldo y movimientos dejan de verse | la sección resuelve su propio error, compacto, con reintentar |
