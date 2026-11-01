# Plan — Habilitar a un organizador desde el backoffice (CU-90, segundo paso)

- Fecha: 2026-10-29 · Repos: monorepo (servicios/organizador, apps/backoffice) · Predecesor: backlog ítem 1; `2026-10-29-postulaciones-organizador`
- Rama: `claude/habilitacion-organizador-20261029` (desde `origin/test`)
- Resultado observable: desde el expediente de una postulación aprobada, el operador abre la habilitación del organizador,
  ve su estado y lo habilita; si le faltan requisitos, la pantalla dice cuáles.
- Kill-test: un ADMIN_PLATAFORMA abre `/cumplimiento/organizadores/{id}/habilitacion` y no ve el botón → no está hecho.

## Hallazgos
- `consultarHabilitacion` exige `GRUPO_CREAR` (`OrganizadoresController.java:135`); ADMIN_PLATAFORMA no lo tiene → el GET da 403
  y el botón (que depende del GET, `pantalla-de-habilitacion.ts:26`) nunca aparece. `habilitarOrganizador` sí es ADMIN_PLATAFORMA (`:153`).
- `habilitarOrganizador` devuelve `SalidaHabilitacion {organizadorId, estado, faltantes[]}`; el front la tipaba como `Habilitacion`
  y no mostraba los faltantes.
- La pantalla solo se abre por UUID; `DecisionDeHabilitacion.organizadorId` existe en el expediente de postulaciones.
- Fuera de alcance (anotado): `ConsultarHabilitacion.ejecutar` no verifica pertenencia del organizador consultado (posible BOLA
  para cualquiera con GRUPO_CREAR). No se toca.

## Alcance
- IN: `@PermisoAlternativo({"ADMIN_PLATAFORMA"})` en `consultarHabilitacion` + doc del contrato; tipo y faltantes en el front;
  enlace desde el expediente de la postulación.
- OUT: cambiar la lógica de `ConsultarHabilitacion`; el BOLA anotado.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Permiso alternativo + web test | admin 200; participante sin GRUPO_CREAR 403; GRUPO_CREAR sigue 200 | `webTest` organizador | HECHO |
| H1.S2.M1 | Tipo `SalidaHabilitacion` + faltantes + mensaje | correcto (HABILITADO → aviso) / límite (faltantes listados) / inválido (422 traducido) | spec de la pantalla | HECHO |
| H1.S2.M2 | Enlace desde el expediente de la postulación | decisión con organizadorId → enlace | spec de postulaciones | HECHO |
| H1.S3.M1 | Lint + typecheck + regresión completa + barrido/spotless | sin rojos nuevos | suites | HECHO |
