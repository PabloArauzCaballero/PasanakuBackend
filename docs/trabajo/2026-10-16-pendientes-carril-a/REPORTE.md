> **AVANCE: 22 / 24 — 91,7 %.** CI de #110: Trivy, imagen, formato, contratos y lint en verde; quedan rojos CU-04 (#109) y goldens móviles desactualizados (decisión visual). Lo que falta no es código: el CI de los PR #109 y #110 (H4.S1.M8, EN CURSO) y,
> por consecuencia, que el CI de `dev` quede verde (H3.S1.M2, A MEDIAS). Hasta que esos dos PR pasen y se mergeen, el
> CI de `dev`/`test` sigue rojo.

# Reporte — Pendientes del carril A del backoffice y auditoría del CI

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md) · Ramas: `codex/pendientes-carril-a-20261016` (PR #97, mergeado),
  `codex/cu04-codigo-por-correo-test-20261016` (PR #109), `codex/ci-trivy-spring-openssl-20261016` (PR #110)
- Peldaño de evidencia: **TESTED** (pruebas dirigidas y regresión local). No es **VERIFIED**: no se ejercitó ningún
  servicio levantado, y el CI de #109 y #110 todavía no terminó.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Clase = archivo en las tres matrices web renombradas en #89 (defecto propio) | `grep -c "^class <Nombre> "` | 1/1/1 · `evidencia/H1-01` |
| H1.S1.M2 | Formato Java de `identidad` | `:servicios:identidad:spotlessCheck test webTest` | PASS · `evidencia/H1-03` |
| H1.S1.M3–M4 | Formato Dart de `diseno_flutter` y `movil` (Flutter 3.44.8) | `dart format --set-exit-if-changed` | exit 0 · `evidencia/H1-02` |
| H1.S1.M5 | `testBarrido` de identidad (dos archivos ≥ 300 ya en `dev`) | `testBarrido` + conteo de `@Test` | 12 = 12 · `evidencia/H1-04` |
| H2.S1.M1 | **Seguridad:** `actualizarFechaExpiracionDocumento` declarada en `PoliticaDeAcceso` y probada | `contractTest` de plataforma + `UsuariosFechaExpiracionWebTest` | matriz 8/8 · 3/3 · `evidencia/H2-01` |
| H2.S1.M2 | **Privacidad:** el teléfono ya no va al MDC (CU-04 y CU-01) | `grep` de `marcarCasoDeUso(...getTelefono…)` | vacío · `evidencia/H2-02` |
| H2.S1.M3 | YAML de `crearCampana` = permiso del código | `grep` + Postman | PASS · `evidencia/H2-03` |
| H2.S1.M4–M5 | CU-47/CU-48: el aprobador sale de la sesión; las pruebas exigen `aprobada_por = sesión` | `integrationTest --tests '*CU47*' '*CU48*'` | 32/32 · `evidencia/H2-04` |
| H2.S1.M6 | `Traza.marcarCasoDeUso(cu, referencia)` | `:plataforma:comun-web:test` | 11/0 · `evidencia/H2-06` |
| H3.S1.M1 | Regresión de los servicios tocados y gates | gradle + `verificar_*` | PASS · `evidencia/H3-01` |
| H3.S1.M3 | Los mismos cambios en `test` | `git diff origin/dev origin/test` | árbol idéntico |
| H3.S1.M4 | Espejo `test` = `origin/test` | `git diff --quiet` | b225a5b0, sin force |
| H4.S1.M1 | Auditoría backend completa de `dev` | gradle de los 15 servicios + plataforma | 3.721 pruebas, 1 falla (abajo) |
| H4.S1.M2–M7 | Arreglos locales de los rojos del CI (ver H4 en el plan) | `yarn lint`, `yarn typecheck`, simulado, identidad | exit 0 en todos · PR #109, #110 |

## A medias

### H3.S1.M2 — CI de `dev` en verde
- Qué anda: #97 mergeado (839c03bc); todo lo que #97 tocaba pasa en local y en los jobs de backend del CI.
- Qué no anda: el CI de `dev` sigue rojo por causas ajenas a #97 que aparecieron con los merges del día: la prueba de
  CU-04, Postman de identidad, Trivy (Spring 6.2.19 y openssl) y `f1` (lint frontend). Además, por `f1` en rojo, `f2`
  (pruebas frontend) no corrió hoy en ninguna rama.
- Qué falta exactamente: que el CI de #109 y #110 pase y se mergeen; después, revisar el CI de `dev` y el de `test`.
- Dónde quedó: PR #109 y #110 abiertos, con su rama empujada; todo compila.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H4.S1.M8 · CI de #109 y #110 | A MEDIAS: `f2` corrió por primera vez hoy y falla por goldens móviles desactualizados en Linux (anverso-360 31,6 %, capturas_* ~1 %, pautas_publicas_360 4,7 %, cabecera_fase_2_360 0,8 %, ilustracion_de_pauta 0,1 %): vienen de los cambios visuales del día (pautas de foto, identidad institucional). Regenerarlos es aceptar el cambio visual: lo decide quien los hizo, revisando la captura | que terminen los jobs; si `h4_ci_icono_visual_test` falla en Linux, decidir si el golden se regenera (cambio visual intencional) o si el cambio visual es un error |
| Bóveda CU-04: códigos `-08`, `-09` y `-10` | `DECISION_REQUIRED` | lo decide el dueño de CU-04 (`-08` tiene dos sentidos en el código y `-10` solo vive en el YAML); quedó anotado también en el reporte de otra sesión |
| Migrar a Spring Boot 4 (CVE-2026-47884/47890) | `DECISION_REQUIRED` | la excepción vence el 2026-11-11 en OSV y en Trivy |
| `dev` del espejo del frontend | `DECISION_REQUIRED` | ~467 archivos distintos con PR propios; pisarlo es destructivo |
| Permisos S-8, D-15, finalidades, motor de decisiones, monedas del modelo, A7 | `DECISION_REQUIRED` | ver el reporte del carril A |

## Evidencia

```text
# Auditoría de dev @ d63e4fef (worktree limpio, sin caché)
aportes 49·41·105·2 · auditoria 16·34·46·2 · cumplimiento 14·87·332·2 · entregas 40·60·111·2·9 · erp 5·52·117·2
garantia 35·46·154·2 · grupos 70·89·228·2 · identidad 35·123·151(1 falla)·2 · inversiones 36·33·147·2·23
notificaciones 5·36·66·2 · nucleo-financiero 81·68·476·2·18 · organizador 15·37·110·2 · publicidad 5·21·59·2
tarifas 7·34·106·2 · transparencia 7·30·120·2 · plataforma 102/0          TOTAL 3.721 pruebas, 1 falla
falla: CU04CodigoPorCorreoTest — esperaba AP-CU04-03, 278dc8ca devuelve AP-CU04-10 → PR #109
inversiones contractTest: "Cannot run program python" (ENVIRONMENT de esta Mac) → 23/23 con shim python→python3

# Frontend (rama de #110)
yarn lint → exit 0 · yarn typecheck → exit 0 · simulado vitest → 307/307 · yarn test:a11y → 14/14
yarn turbo run test:front --concurrency=1 → 11/12: falla h4_ci_icono_visual_test
  (golden anverso-360.png, 31,63 %, 91.081 px; corrido en macOS; el veredicto es del CI en Linux)

# openssl en la imagen runtime fijada (linux/amd64)
instalada: 3.0.13-0ubuntu3.15 · Candidate: 3.0.13-0ubuntu3.16 · tras install: 3.0.13-0ubuntu3.16
```

Archivos: `evidencia/H1-01…H1-04`, `H2-01…H2-06`, `H3-01`.

## No cubierto

- El job «App — goldens (Linux)» y `f2` del CI no se reprodujeron en local (macOS): los goldens solo se juzgan en Linux.
- No se levantó ningún servicio ni se probó por el gateway (el perfil `base` falla por la imagen de MinIO).
- No se construyó la imagen Docker completa en local: se verificó el paquete de openssl sobre el mismo digest.

## Desvíos del plan

1. H1.S1.M5 (límite de 300 líneas en identidad) se agregó: estaba rojo en `dev` y bloqueaba el job de formato.
2. Mientras se trabajaba, #98 (otra sesión) partió los mismos archivos de otra forma. Quien mergeó #97 resolvió el
   conflicto quedándose con esta versión de las pruebas y con el `FilaDeExpediente` de #98. El árbol resultante pasa.
3. H4 completo se agregó a pedido del dueño («asegurate de que el CI esté en orden y sin regresiones»).
4. Los clientes Dart/Angular se generan **de a un servicio por invocación de Gradle**: generados juntos, cruzan salidas
   entre servicios (en `tarifas` aparecieron modelos de publicidad, en `garantia` modelos de aportes). Esto explica
   también los «clientes contaminados» del carril A.

## Riesgos residuales

| Riesgo | Impacto | Qué hacer |
|---|---|---|
| Generar clientes de varios servicios en la misma invocación de Gradle cruza las salidas | clientes falsos en local y typecheck en rojo sin motivo | arreglar la tarea `generarCliente*` (salidas o carpeta temporal compartida); mientras tanto, generar de a uno |
| `f2` no corrió hoy: puede haber más rojos del frontend en Linux | aparecen al pasar #110 | mirar el CI de #110 |
| Excepciones de Spring 6.2.19 vencen el 2026-11-11 | el CI vuelve a bloquear | decidir la migración a Spring Boot 4 antes de esa fecha |

## Decisiones y ambigüedades

| # | Decisión / supuesto | Confirmar con |
|---|---|---|
| P-1 | `CU04CodigoPorCorreoTest` se actualizó a `-10` siguiendo la intención de 278dc8ca y el YAML | dueño de CU-04 |
| P-2 | Se espejó en Trivy la excepción que H17 ya había decidido para OSV, con la misma fecha y el mismo resguardo | Seguridad |
| P-3 | Ejemplos del simulado con datos sintéticos (`+59170000000`, `persona@ejemplo.invalid`) | — |
| P-4 | `Traza` conserva la clave `usuarioId` del MDC para no romper tableros; solo cambia el nombre del parámetro | dueño de plataforma |
