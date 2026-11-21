# Plan — F0 Preparación del plan de correcciones (hito H0)

- Fecha: 2026-11-21 · Repo: `PasanakuBackend` · Base: `origin/dev` 850e0f51 · Predecesor: plan de correcciones
  (`docs/trabajo/2026-11-21-plan-correcciones/PLAN.md`, H0) y `DECISIONES.md`
- Resultado observable: cualquier sesión que abre `dev` encuentra el plan de correcciones y las decisiones de Pablo,
  ADR vigentes que coinciden con esas decisiones (ADR-055, ADR-056, ADR-058), un catálogo de flujos sin respaldo ni
  aprobaciones, ningún candado `blocker_gate` trabado por trabajos viejos y una línea base de verificadores contra la
  que medir cada fase.
- Kill-test: (1) `blocker_gate.py` sigue bloqueando un turno sin trabajo activo; (2) `grep -n "recuperar-respaldo\|cubrirConRespaldo" docs/catalogo-de-flujos/*.yaml`
  devuelve algo; (3) `verificar_boveda.py` falla por un ADR nuevo; (4) no existe la tabla PASS/FAIL de la línea base.

## Alcance

- IN: copiar las dos carpetas del plan (solo documentación); los 9 `PLAN.md` viejos que traban `blocker_gate`
  (solo su fila de estado); ADR-055, ADR-056, ADR-058 nuevos; banners de reemplazo en ADR-049, ADR-051, ADR-052
  (DD-2) y ADR-054; índice `docs/Arquitectura/_Arquitectura.md`; `docs/Cumplimiento.md` (estado de R-SEG-04 y
  R-ORG-06); `docs/catalogo-de-flujos/*.yaml` y su `matriz.md` regenerada; esta carpeta de trabajo con su evidencia.
- OUT: todo código (`servicios/`, `plataforma/`, `apps/`, `scripts/`), `sql/`, `docs/Restricciones.md` (fuente del
  DDL), `docs/entidades/`, semillas, CU de `docs/CasosDeUso`. F0.M2 (Docker en la máquina de Pablo): solo se escribe
  el procedimiento. Lo roto fuera de alcance se anota en el `REPORTE.md`.
- Ramas: un PR por microtarea hacia `dev`. Rama base del carril `correcciones/f0-preparacion-2026-11-21`; las
  demás con sufijo (`-m1`, `-adr055`, …) porque cada PR necesita su rama.
- Ambigüedades registradas:
  - A1. El inicio automático: DECISIONES dice «al completarse los cupos»; ADR-054 D3/D4 agregaba el 75 % con fecha
    límite y cancelación. No está decidido si el 75 % sobrevive → `DECISION_REQUIRED` (Pablo), se deja vigente
    marcado «a confirmar» en ADR-055.
  - A2. ADR-054 D7 «disolución automática por mora generalizada» vs tanda 7 «disolución votada». `DECISION_REQUIRED`
    (Pablo); ADR-055 registra la duda sin resolverla.
  - A3. ADR-049: P-09 dice «reemplace a ADR-049»; la instrucción del carril dice «reemplazado parcialmente». Supuesto:
    parcial, porque la máquina de estados `AUTORIZADA/EN_REVISION` y el `UPDATE` condicionado sobreviven para los
    retiros retenidos por alerta. Confirmar con Pablo en el PR.
  - A4. Comisión: el anexo L5 dice «fija»; DECISIONES tanda 7 dice «porcentaje único». Manda la tanda 7.

## H0 — Preparación

**CA:** Dado `dev`, cuando una sesión del plan arranca, entonces lee el plan desde `dev`, los ADR coinciden con
DECISIONES, ningún candado viejo la traba y existe la línea base.
**DoD:** cada PR con su salida pegada; `verificar_boveda.py` y `validar_flujos.py` sin fallas nuevas.
**Estado:** A MEDIAS — las 11 microtareas del carril tienen su DoD; el hito no cierra hasta que Pablo mergee los PR, se ejecute F0.M2 en su máquina y la línea base sume `integrationTest` y Flutter

### H0.S1 — Plan visible y candado destrabado

**CA:** Dado `dev` con los PR mergeados, cuando se abre `docs/trabajo/`, entonces están el plan y las decisiones, y
`blocker_gate` no bloquea.
**DoD:** `ls` de las carpetas + `blocker_gate.py --self-test` + corrida real.
**Estado:** HECHO (sus microtareas tienen el DoD ejecutado; PR sin mergear)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H0.S1.M1 | Copiar `2026-11-20-auditoria-consolidado/` y `2026-11-21-plan-correcciones/` tal cual desde `origin/claude/nice-fermat-essf9e` | `diff -r` entre la rama del plan y la mía no muestra diferencias | `diff -r /tmp/plan/docs/trabajo/<carpeta> docs/trabajo/<carpeta>` → sin salida | HECHO — PR #400 · `evidencia` en el PR |
| H0.S1.M2 | F0.M1: pasar a `DESCARTADO` las 9 microtareas viejas con el motivo de P-20 | La corrida real del hook sale 0 sin mensaje | `python3 .claude/hooks/blocker_gate.py --self-test` + corrida con evento → exit 0 | HECHO — PR #401 · `evidencia/F0.M1-blocker-gate.txt` |

### H0.S2 — ADR alineados con DECISIONES

**CA:** Dado `docs/Arquitectura/`, cuando una sesión busca la regla vigente de aprobaciones, garantía o proveedores,
entonces encuentra ADR-055, ADR-056 y ADR-058, y los ADR viejos dicen qué quedó reemplazado.
**DoD:** `python3 scripts/verificar_boveda.py` sin fallas nuevas en la sección ARQUITECTURA + grep de banners.
**Estado:** HECHO (sus microtareas tienen el DoD ejecutado; PR sin mergear)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H0.S2.M1 | F0.M3 ADR-055 «Sin aprobaciones en el circuito» (+ F0.L2.M02, F0.L3.M01, F0.L4.M01, F0.L4.M02): banners en ADR-049, ADR-052 DD-2 y ADR-054; estado de R-SEG-04 y R-ORG-06 en `Cumplimiento.md` | ADR-049 y ADR-054 tienen la línea «Reemplazado parcialmente por ADR-055» | `grep -n "Reemplazado parcialmente por ADR-055" docs/Arquitectura/ADR-049* docs/Arquitectura/ADR-054*` → 2 líneas · `verificar_boveda.py` ARQUITECTURA OK | HECHO — PR #402 · `evidencia/F0.M3-adr055.txt` |
| H0.S2.M2 | F0.M4 ADR-056 «Tu garantía vuelve y la puntualidad vale plata» (+ F0.L2.M01 parte documental, F0.L5.M01 parte documental); ADR-051 `superada por ADR-056` | ADR-051 declara `estado: superada por ADR-056` | `grep -n "^estado" docs/Arquitectura/ADR-051*` → `superada por ADR-056` · `verificar_boveda.py` ARQUITECTURA OK | HECHO — PR #403 · `evidencia/F0.M4-adr056.txt` |
| H0.S2.M3 | PX0.M01 ADR-058 «Integración con proveedores externos» con RM-01..RM-20 | El ADR enumera RM-01 a RM-20 | `grep -c "RM-[0-2][0-9]" ADR-058` ≥ 20 · `verificar_boveda.py` ARQUITECTURA OK | HECHO — PR #404 · `evidencia/PX0.M01-adr058.txt` |

### H0.S3 — Catálogo de flujos

**CA:** Dado el catálogo, cuando se lo valida, entonces no nombra respaldo ni aprobaciones eliminadas y pasa.
**DoD:** `python3 scripts/validar_flujos.py` → «Sin fallas.»
**Estado:** HECHO (sus microtareas tienen el DoD ejecutado; PR sin mergear)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H0.S3.M1 | F0.M5: sacar del catálogo vivo los pasos de respaldo, recargo al fondo y aprobación humana; flujo objetivo del modelo A en `objetivo/` (no validado contra código porque todavía no existe) | Ningún `.yaml` vivo nombra `cubrirConRespaldo`, `recuperarRespaldo`, `liberarRespaldo` ni `aprobarRetiro` | `python3 scripts/validar_flujos.py` → «Sin fallas.» + `grep` vacío | HECHO — PR #405 · `evidencia/F0.M5-flujos.txt`. Desvío: el flujo objetivo va en `objetivo-modelo-A.md` (no en `objetivo/*.yaml`) para no inventar `operationId` |

### H0.S4 — Línea base y entorno

**CA:** Dado `dev`, cuando una fase quiere medir regresión, entonces tiene la salida de cada verificador y de cada
suite por servicio.
**DoD:** archivos en `evidencia/linea-base/` + tabla PASS/FAIL/NO COMPROBADO en el `REPORTE.md`.
**Estado:** HECHO (sus microtareas tienen el DoD ejecutado; PR sin mergear)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H0.S4.M1 | F0.M6: verificadores de solo lectura (`scripts/verificar_*.py` y afines), uno por uno, salida guardada | Cada verificador tiene su archivo de salida con código de salida | `ls evidencia/linea-base/verificadores/` | HECHO — 20 salidas: 19 rc=0, `verificar_rutas_frontend` rc=1 (registrado, no arreglado) |
| H0.S4.M2 | F0.M6: `./gradlew :servicios:<s>:test` de a uno contra la base Postgres local de EJECUCION.md | Cada servicio tiene su salida y su veredicto | `ls evidencia/linea-base/gradle/` | HECHO — 15/15 rc=0, 559 pruebas, 0 fallidas (`_conteo-junit.txt`) |
| H0.S4.M3 | F0.M6: `yarn lint` y `yarn typecheck` si hay dependencias | Salida guardada o motivo de NO COMPROBADO | `ls evidencia/linea-base/yarn/` | HECHO — lint 8/10 y typecheck 11/14; los fallos son los 2 paquetes Flutter (exit 127, sin Flutter) → NO COMPROBADO |
| H0.S4.M4 | F0.M2: procedimiento Docker para la máquina de Pablo escrito en el `REPORTE.md` (no se ejecuta por instrucción) | El procedimiento nombra el compose, los servicios y los healthchecks reales del repo | Lectura del `REPORTE.md` §F0.M2 | HECHO (documento; la ejecución queda para la máquina de Pablo) |

### H0.S5 — Anexos

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H0.S5.M1 | Clasificar las 18 `F0.Lx.Mnn` de los anexos: documentales (se cubren en H0.S2/H0.S3) y de código (lista para la próxima tanda) | Las 18 aparecen en el `REPORTE.md` con destino | `grep -c "F0.L" REPORTE.md` ≥ 18 | HECHO |

## Riesgos y bloqueos previstos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Los PR documentales se pisan entre sí (índice de ADR) | Conflicto de merge | Cada PR toca líneas distintas del índice; se avisa en el PR el orden sugerido de merge |
| Maven Central 429 | Línea base incompleta | Un reintento; si persiste, NO COMPROBADO con la salida |
| Pruebas con Testcontainers sin Docker | Suites en rojo por entorno | Clasificar `ENVIRONMENT` (regla 80.4), nunca PASS |
