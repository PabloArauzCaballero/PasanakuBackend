# Reporte — Detalle del grupo, firma del reglamento y sorteo con ruleta

> **AVANCE: 8 / 9 — 88,9 %.**

- Fecha: 2026-10-20 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/detalle-firma-sorteo-20261020`
- Peldaño: `TESTED` (servidor con PostgreSQL real en GitHub; app con pruebas de pantalla). Falta verlo en el teléfono.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | El organizador ocupa el cupo 1 con su nombre | `CU20Test` (GitHub, run 39457949551) | PASS (3 cupos libres de 4) |
| H1.M2 | Aprobar una solicitud guarda el nombre en el participante | `:servicios:grupos:integrationTest` | PASS |
| H1.M3 | Firmar: ACTIVO + cupo OCUPADO; la última firma conforma y arma el calendario | `CU69FirmaYSorteoTest` | PASS |
| H1.M4 | `GET /grupos/{id}/detalle` solo para integrantes, sin teléfono ni correo | `GruposDetalleYFirmaWebTest` + `CU69FirmaYSorteoTest` | webTest 102/102 · PASS |
| H1.M5 | Sorteo de punta a punta con el calendario generado | `CU69FirmaYSorteoTest.sorteoDePuntaAPunta` | PASS (3 turnos 1..3) |
| H2.M1 | Pantalla del grupo con resumen y tarjetas | `flutter test test/pasanaku/detalle_del_grupo_test.dart` | 11/11 |
| H2.M2 | Firmar el reglamento | idem | PASS |
| H2.M3 | Ruleta + orden de cobro (con y sin movimiento) | idem | PASS |

## A medias
### H2.M4 — APK y prueba en el teléfono
- Qué anda: código y pruebas de pantalla.
- Qué falta: construir el APK y verlo con cuentas reales en TEST.
- Dónde quedó: rama `claude/detalle-firma-sorteo-20261020`.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Grupo a EN_CURSO tras el sorteo (CU-60 paso 6) | TODO | decidir con producto cuándo se abre el primer periodo |

## Evidencia
- GitHub Actions run 39457949551 «Pruebas grupos e identidad (temporal)»: `:servicios:grupos:test`, `webTest` e `integrationTest` en verde.
- Local: `:servicios:grupos:webTest` 102/102; `flutter analyze` «No issues found»; `flutter test test/pasanaku/` +76; `verificar_boveda.py` TODO OK; `verificar_criterios.py` sin divergencias; `verificar_frontend.py` sin FALLA.

## No cubierto
- La ruleta y las tarjetas en un teléfono real (las pruebas usan la fuente de prueba).
- El canje por número no guarda el nombre: esos integrantes aparecen como «Integrante».

## Desvíos del plan
- `IntegrantesDelGrupo` entra por setter en `RutasDelGrupo`: `GruposController` estaba al tope de 300 líneas.
- `GruposControllerWebTest` pasaba las 300 líneas (ya venía de #157): sus dobles sin uso pasaron a `@MockitoBean(types=…)`.
- `HechosCaidaTest` falla en Windows y pasa en Linux (GitHub): ENVIRONMENT, no tocado.

## Riesgos residuales
- El calendario (fechas de cada periodo) es un supuesto registrado en el plan, a confirmar con producto.
- Los grupos creados antes de este cambio tienen al organizador sin cupo.

## Decisiones y ambigüedades
- Del dueño: el organizador ocupa el cupo 1; en las tarjetas todos ven nombre, estado y turno, nadie ve teléfono ni correo.
- Supuestos: calendario por periodicidad desde la fecha de inicio (o hoy); revelado en el acto.
