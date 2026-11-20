# Plan — Metaprompt de auditoría profunda adaptado a Pasanaku

- Fecha: 2026-11-20 · Repos afectados: Pasanaku (solo documentación) · Predecesor: `/Users/pablo/Downloads/METAPROMPT_AUDITORIA_MODULOS.md` (versión ALOVIDA)
- Resultado observable: existe un metaprompt para auditar un módulo de Pasanaku que usa su estructura real y audita contra el flujo nuevo (sin aprobaciones, solo QR; el sistema no cubre a quien no paga).
- Kill-test: `grep -ciE "alovida|paciente|HIPAA|ANVISA|LGPD|FHIR" METAPROMPT_AUDITORIA_PASANAKU.md` devuelve 0.

## Alcance
- IN: `docs/trabajo/2026-11-20-metaprompt-auditoria-pasanaku/` (PLAN, metaprompt, REPORTE).
- OUT: todo código, SQL, contratos y ADR del repo. El archivo de Downloads no se toca.
- Ambigüedades registradas:
  1. «Ya no se requiere aprobación de nada»: ¿incluye KYC, cumplimiento UIF/ASFI y la doble aprobación de retiros (ADR-049)? Supuesto: NO se asume; el metaprompt las lista como DECISIÓN REQUERIDA. Confirmar con Pablo.
  2. «Basta con el QR»: Supuesto: QR pagado y confirmado por webhook verificado de la pasarela (regla 91.3), no la palabra del usuario. Confirmar con Pablo.
  3. «El sistema no paga por quien no paga»: no dice qué pasa con la entrega cuando falta plata. Es decisión de negocio (regla 65 §3). Supuesto: el auditor no la inventa; la lista como DECISIÓN REQUERIDA.

## H1 — Metaprompt Pasanaku
**CA:** Dado un auditor con el archivo, cuando lo ejecuta sobre un módulo, entonces recibe rutas reales, el flujo nuevo, las reglas de la casa y un entregable único con inventarios de aprobaciones y de respaldo.
**DoD:** kill-test en 0; rutas citadas existen (`ls`).
**Estado:** HECHO (ver REPORTE)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Redactar el metaprompt | Sin términos de ALOVIDA | grep = 0 | HECHO |
| H1.S1.M2 | Verificar rutas citadas | Todas existen | script de `test -e` | HECHO |
