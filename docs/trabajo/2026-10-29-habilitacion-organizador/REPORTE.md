# Reporte — Habilitar a un organizador desde el backoffice (CU-90, segundo paso)

> **AVANCE: 4 / 4 — 100,0 %.** (sin ejercitar en TEST: ver Pendiente)

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/habilitacion-organizador-20261029`
- Peldaño de evidencia: backend `TESTED` (web + integración del módulo) · frontend `TESTED` contra HTTP simulado.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `consultarHabilitacion` con `@PermisoAlternativo({"ADMIN_PLATAFORMA"})`; admin 200, participante sin GRUPO_CREAR 403, GRUPO_CREAR sigue 200 | `./gradlew :servicios:organizador:webTest` | 43/43 |
| H1.S2.M1 | Respuesta tipada como `SalidaHabilitacion`; HABILITADO → aviso; si no, estado + faltantes en las palabras del servidor; 422 traducido | `ng test` habilitación | PASS |
| H1.S2.M2 | Enlace «Ver la habilitación del organizador» desde cada decisión con `organizadorId` | `ng test` postulaciones | 13/13 |
| H1.S3.M1 | Lint, typecheck, regresión completa, formato, barrido e integración del módulo | ver Evidencia | todo en verde |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Ejercicio en TEST | TODO | Merge + despliegue; en TEST hay 2 organizadores HABILITADO y 0 postulaciones |

## Evidencia
```text
backoffice: Test Files 185 passed (185) · Tests 1315 passed (1315) · yarn lint EXIT 0 · typecheck EXIT 0
organizador: webTest 43/43 · integrationTest 118/118 · spotlessCheck + testBarrido EXIT 0
```

## No cubierto
Integración real backoffice ↔ organizador; prueba visual.

## Desvíos del plan
- **Regresión atrapada antes del PR:** con el permiso y su comentario, `OrganizadoresController.java` pasó de 299 a 303 líneas y
  `testBarrido` (≤ 300) lo rechazó. Se movió `consultarHabilitacion` a `RutasDeHabilitacion` (mismo tema, inyección por setter,
  patrón de `grupos/web/RutasDelGrupo.java`): el controlador queda en 275.
- El spec de la pantalla respondía al POST con la forma de `Habilitacion`; el contrato dice `SalidaHabilitacion`. Se alineó el
  doble con el contrato (era un TEST_BUG: el doble no cumplía el pacto).

## Riesgos residuales
- **Anotado, no tocado:** `ConsultarHabilitacion.ejecutar` no verifica pertenencia del organizador consultado: cualquiera con
  `GRUPO_CREAR` lee nivel y límites de cualquier organizador (posible BOLA). Requiere decisión: ¿es dato público entre organizadores?

## Decisiones y ambigüedades
- Seguridad (90.6): amenaza = abrir la lectura a quien no decide; control = `ADMIN_PLATAFORMA` solo como alternativa, pruebas
  negativas (participante sin GRUPO_CREAR → 403) en verde.
