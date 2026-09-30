# Gate de promoción `dev` → `main`

> Cada casillero marcado enlaza a su `evidencia/` o su `carriles/`. Un
> casillero marcado sin enlace no cuenta (regla 30) — `H5.S2.M2` lo verifica
> con `grep`. Los checks de los otros cuatro carriles quedan sin marcar
> hasta que ellos mismos los marquen: no se les inventa el cierre desde este
> carril.

## Documentos requeridos (H5.S2.M1)

| Documento | Existe | Dueño |
|---|---|---|
| ADR-046…049 | [ ] No existen todavía | Richard / Justin / Leo / Marcelo (según el tema) |
| ADR-050 (rate limiting) | [x] [[ADR-050 Rate limiting distribuido en el borde]] — evidencia/H3-S1-M4-redis-caido-fail-closed.txt | Pablo |
| `endpoints.md` (inventario) | [ ] No existe | Marcelo (PR4, seguridad transversal) |
| `security-matrix.md` | [ ] No existe | Marcelo |
| `financial-invariants.md` | [ ] No existe | Justin (núcleo financiero) |
| `proveedores.md` | [ ] No existe | Pablo (H1.S1, cuando enlace los contratos `step-up-jwt`/`evento-kafka` de Richard/Leo) — pendiente porque esos contratos tampoco existen todavía |
| `dependencias.md` | [ ] No existe (parcialmente cubierto por `.github/osv-scanner.toml` + `evidencia/H2-S4-M1-trivy-*`, que sí son míos) | Pablo, parcial |
| `mutation-testing.md` | [ ] No existe | Fuera del alcance de los cinco carriles de esta noche, según el plan madre |

## Los 17 checks del metaprompt §85 + los de este reparto

### CI y operación (Pablo — este carril)

- [x] `spotlessCheck` en verde — corregido, PR #1 sin mergear todavía: [evidencia/H1-S2-M1-spotless.txt](evidencia/H1-S2-M1-spotless.txt)
- [x] SCA real (OSV-Scanner) corriendo en CI — evidencia/H2-S2-M2-osv-scan-ci-real.txt (corrida real, job `dependencias`)
- [ ] SCA sin HIGH/CRITICAL sin excepción — **bloqueado**: encontró vulnerabilidades reales, ver hallazgo F-04 abajo
- [x] SBOM CycloneDX por módulo — [evidencia/H2-S3-M1-cyclonedx.txt](evidencia/H2-S3-M1-cyclonedx.txt)
- [x] Trivy real sobre la imagen — [evidencia/H2-S4-M1-trivy-gateway-table.txt](evidencia/H2-S4-M1-trivy-gateway-table.txt)
- [ ] Trivy sin CRITICAL/HIGH sin excepción — **bloqueado**, mismo motivo que SCA (hallazgo F-04)
- [x] Imagen por digest, no por tag — `grep -c "@sha256" despliegue/Dockerfile` → 2, ver evidencia/H2-S4-M1-trivy-gateway-table.txt (misma imagen escaneada)
- [x] CODEOWNERS válido — [evidencia/H2-S5-M1-codeowners-errors.txt](evidencia/H2-S5-M1-codeowners-errors.txt) → `{"errors":[]}`
- [ ] Ruleset mínimo aplicado — **bloqueado, `DECISION_REQUIRED`**: un comando de Pablo, ver `carriles/PR5-ci-operacion.md` §Bloqueado
- [x] Rate limiting real con Redis, fail-closed — [[ADR-050 Rate limiting distribuido en el borde]], [evidencia/H3-S1-M4-redis-caido-fail-closed.txt](evidencia/H3-S1-M4-redis-caido-fail-closed.txt)
- [x] CORS por perfil, fail-closed en producción — [evidencia/H3-S2-cors.txt](evidencia/H3-S2-cors.txt)
- [x] `/actuator` bloqueado en el borde — verificado en [evidencia/H3-S2-cors.txt](evidencia/H3-S2-cors.txt) (contexto) y el commit de H3.S3
- [x] Carga medida con k6 (6 escenarios) — evidencia/H4-k6-*.txt (6 corridas, `transferencia.js` con baseline ×3)
- [x] Backup/restore ejecutado — [evidencia/H5-restore.txt](evidencia/H5-restore.txt)
- [x] 4 runbooks (outbox, kafka, postgres, secretos) — `docs/operacion/`, citados desde carriles/PR5-ci-operacion.md
- [ ] `verificarProduccion` (tarea Gradle) — **no empezado** (H2.S6)
- [ ] E2E financiero en CI — **no empezado** (H2.S6)

### De los otros cuatro carriles

Ningún check propio: `carriles/PR1-identidad.md`, `PR2-nucleo-financiero.md`,
`PR3-plataforma-infra.md`, `PR4-seguridad.md` no existen todavía (hallazgo
F-01, `carriles/PR5-ci-operacion.md`). Sus checks quedan `[ ]` hasta que
publiquen su propia bitácora.

## Hallazgos que bloquean `READY`

| ID | Qué | Dónde |
|---|---|---|
| F-04 | Trivy encontró CRITICAL reales sin parche (netty-handler, bcprov-jdk18on, spring-security-web) | `carriles/PR5-ci-operacion.md` |
| F-01 | Los otros cuatro carriles sin bitácora publicada | `carriles/PR5-ci-operacion.md` |
| H2.S5.M3 | Ruleset mínimo listo, sin aplicar — `DECISION_REQUIRED` | `carriles/PR5-ci-operacion.md` |
| H2.S6 | E2E financiero y `verificarProduccion` sin empezar | Este documento |

## Veredicto de este carril (no del sistema completo — eso es `FINAL_REPORT.md`)

El carril PR5 (CI y operación) llega a **`WRITTEN`/`TESTED` en la mayoría de
sus hitos, `VERIFIED` donde hay evidencia de ejecución real contra el
gateway** (H3, H4) — nunca `READY` para el sistema completo, porque `READY`
exige que TODOS los carriles estén cerrados (regla 30.6) y los otros cuatro
ni empezaron.

## Ver también

[[baseline]] · [[ADR-050 Rate limiting distribuido en el borde]]
