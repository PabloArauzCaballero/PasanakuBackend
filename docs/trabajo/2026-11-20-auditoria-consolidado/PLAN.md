# Plan — Consolidado de las auditorías L1–L6 (paso 7 del metaprompt)

- Fecha: 2026-11-21 · Repos: PasanakuBackend (solo docs) · Predecesor: las seis ramas `auditoria/*-2026-11-20`
- Resultado observable: Pablo lee en un solo archivo los totales, los S1, los inventarios de aprobación y respaldo unificados y las preguntas que hay que responder antes del plan de correcciones.
- Kill-test: un S1 de algún informe que no aparezca en `CONSOLIDADO.md`, o totales que no coincidan con la tabla «Total» de cada informe.

## Alcance
- IN: leer las secciones 2, 4 y las fichas S1 de los seis informes; escribir `CONSOLIDADO.md`.
- OUT: re-verificar en el código cada hallazgo (lo hicieron las sesiones); proponer correcciones (se hace después, con las respuestas de Pablo).

## H1 — Consolidado
**CA:** Dado los seis informes, cuando Pablo abre `CONSOLIDADO.md`, entonces encuentra los 32 S1, los totales que cuadran y las preguntas abiertas.
**DoD:** totales cruzados contra la fila «Total» de cada informe (comando en REPORTE.md).
**Estado:** HECHO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Extraer totales por severidad | coinciden con cada informe | `grep '**Total**'` sobre la sección 2 | HECHO |
| H1.S1.M2 | Listar todos los S1 | 32 títulos con su ID | `awk` de fichas S1 | HECHO |
| H1.S1.M3 | Unificar inventarios y ambigüedades A-1..A-4 | cada fila cita su informe | lectura de la sección 4 de los seis | HECHO |
| H1.S1.M4 | Redactar preguntas para el plan | ninguna respuesta asumida | revisión del texto | HECHO |
