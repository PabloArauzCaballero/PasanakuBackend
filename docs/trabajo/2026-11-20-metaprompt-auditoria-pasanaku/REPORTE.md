> **AVANCE: 2 / 2 — 100,0 %.**

# Reporte — Metaprompt de auditoría profunda adaptado a Pasanaku

- Fecha: 2026-11-20 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/feat/backoffice-como-atlas-h0-h1-2026-10-18` (solo documentación, sin commit)
- Peldaño de evidencia alcanzado: `VERIFIED` para el documento (kill-test y existencia de rutas ejecutados). El metaprompt NO se ejecutó contra un módulo real: no hay evidencia de que un agente lo siga bien.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `METAPROMPT_AUDITORIA_PASANAKU.md` sin términos del producto anterior | `grep -ciE "alovida\|paciente\|HIPAA\|ANVISA\|LGPD\|FHIR\|PHI" METAPROMPT_AUDITORIA_PASANAKU.md` | `0` |
| H1.S1.M2 | Rutas, scripts, ADR y clases citadas existen | bucle `test -e` sobre ~45 rutas + `grep -l RespaldoPorHttp` | solo imprimió `fin`; sin `FALTA` |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Ratificar A-1 a A-4 | DECISION_REQUIRED | Pablo (A-1 a A-4) y Cumplimiento (A-1, KYC/UIF/retiros) |

## Evidencia
Salidas de los comandos arriba, en esta sesión. No se guardó carpeta `evidencia/`.

## No cubierto
- No se corrió el metaprompt sobre ningún módulo.
- No se ejecutó ningún script de `scripts/` citado; solo se comprobó que existen. El metaprompt instruye leer qué hace cada uno antes de correrlo.
- No se verificó `scripts/auditoria/` más allá de que el directorio existe.

## Desvíos del plan
Ninguno.

## Riesgos residuales
- Metaprompt largo: un agente puede resumir y perder reglas. Mitigado pidiendo volcar al archivo en cada tanda.
- Los topes de líneas (300 en servicios, 200 en front) vienen de la memoria del equipo, no se citan como número en el metaprompt.

## Decisiones y ambigüedades
- Escrito en el repo (versionado, regla 20), no en Downloads; el original no se tocó.
- Rama a auditar: `origin/test` (memoria del equipo: el VPS sigue esa rama).
- A-1: «nada de aprobación» ¿incluye KYC, UIF/ASFI y doble aprobación de retiros (ADR-049)? Supuesto: no se asume; DECISIÓN REQUERIDA. → Pablo / Cumplimiento.
- A-2: qué pasa con la entrega cuando falta plata. Decisión de negocio, no inventada. → Pablo.
- A-3: el aval entre participantes no cuenta como «el sistema paga». → Pablo.
- A-4: mora y recargo sin cobertura. → Pablo.
- «Basta con el QR» se leyó como pago confirmado por webhook firmado (regla 91.3). → Pablo.
- Contradicción registrada: ADR-051 regla 7 («la empresa aporta el faltante») y `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml` chocan con R-NUEVA-2. No se editaron; los ADR se corrigen cuando Pablo decida.
