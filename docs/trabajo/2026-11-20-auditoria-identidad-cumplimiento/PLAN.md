# Plan — Auditoría de solo lectura: identidad-cumplimiento (L4)

- Fecha: 2026-11-20 · Repos afectados: PasanakuBackend (solo esta carpeta) · Predecesor: metaprompt
  `docs/trabajo/2026-11-20-metaprompt-auditoria/METAPROMPT_AUDITORIA_PASANAKU.md` (rama `claude/nice-fermat-essf9e`)
- Resultado observable: Pablo abre `AUDITORIA-identidad-cumplimiento.md` en la rama
  `auditoria/identidad-cumplimiento-2026-11-20` y encuentra hallazgos con ubicación y evidencia,
  inventarios de aprobaciones y respaldo, matriz de contratos y límites declarados.
- Kill-test: un hallazgo S1/S2 cuya `ruta:línea` no dice lo que el informe afirma; o `git status`
  mostrando archivos fuera de esta carpeta.

## Alcance
- IN: `servicios/identidad`, `servicios/cumplimiento`, tablas `sql/10_tablas/01_identidad_usuarios` y
  `12_cumplimiento_asfi` con sus claves/índices/reglas/semillas, pactos y clientes que los tocan,
  pantallas de identidad/KYC (móvil, web) y de cumplimiento (backoffice), y el circuito de retiros
  con doble aprobación (ADR-049) en sus puntos de contacto.
- OUT: modificar código, SQL, contratos, pruebas o docs del repo; auditar el interior de otros módulos.
- Ambigüedades registradas: A-1 a A-4 del metaprompt (se registran, no se resuelven; responsable Pablo,
  Cumplimiento para lo regulatorio).

## H1 — Informe de auditoría del módulo identidad-cumplimiento
**CA:** Dado el commit auditado de `origin/test`, cuando una persona lee el informe, entonces cada
hallazgo tiene ficha completa, los inventarios y la matriz existen, y los límites están declarados.
**DoD:** `git status --short` solo muestra esta carpeta · commit subido a la rama de entrega.
**Estado:** HECHO (entregable escrito y subido; el peldaño de evidencia de los hallazgos es DISCOVERED, ver REPORTE)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.M0 | Fase 0 — preparación (rama, commit, lecturas base) | commit anotado en el informe | `git rev-parse origin/test` → 07fc6971… | HECHO |
| H1.M1 | Fase 1 — huella y mapa de conexiones | sección 3 escrita | `grep -c '^## 3' AUDITORIA-*.md` → 1 (`evidencia/dod-plan.txt`) | HECHO |
| H1.M2 | Fase 2 — lectura completa con anexo de cobertura | anexo con una fila por archivo | filas del anexo §12 → 566 (`evidencia/dod-plan.txt`) | HECHO |
| H1.M3 | Fase 3 — inventarios de aprobación y respaldo | sección 4 a/b/c escrita | `grep -c '^| APR-'` → 17; `^| RSP-` → 3 | HECHO |
| H1.M4 | Fase 4 — vista de conjunto | hallazgos DUP/REESCRITURA/FRAGIL evaluados | §8 con 32 clases, ninguna vacía | HECHO |
| H1.M5 | Fase 5 — conexiones, contratos y verificadores | matriz + salidas de verificadores en `evidencia/` | `ls evidencia/` → 23 archivos; Gradle y yarn NO COMPROBADO por entorno (§11) | HECHO |
| H1.M6 | Fase 6 — estándares y normativa | cada hallazgo con estándares | `grep -c '^- Estándares y normas:'` → 176 de 176 fichas | HECHO |
| H1.M7 | Fase 7 — refutación | S1/S2 re-abiertos | 33 «Refutación (fase 7)» + 39 «Línea corregida»; chequeo de 677 referencias → 0 fuera de rango | HECHO |
| H1.M8 | Entrega — REPORTE, commit y push | push OK | `git push -u origin auditoria/identidad-cumplimiento-2026-11-20` → `b7218c97..59987f2b  auditoria/identidad-cumplimiento-2026-11-20 -> auditoria/identidad-cumplimiento-2026-11-20` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin Gradle/Flutter/Node funcional en la nube | verificadores no corren | se pega el error literal y se marca NO COMPROBADO |
| Volumen (~46k líneas Java + SQL + front) | cobertura incompleta | subagentes de solo lectura en lotes disjuntos; anexo declara lo no leído |
