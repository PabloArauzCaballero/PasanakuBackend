# Reporte — Auditoría de solo lectura L1: aportes + núcleo financiero

> **AVANCE: 1 / 9 — 11,1 %.** (reporte parcial de turno; la auditoría sigue en curso en esta misma sesión)

- Fecha: 2026-11-20 · Plan: [PLAN.md](./PLAN.md) · Rama: `auditoria/aportes-nucleo-financiero-2026-11-20` (desde `origin/test` @ `07fc69719287b0dadd152d0293a054948df69ead`; `origin/dev` @ `850e0f51` sin diferencias en `servicios/aportes` ni `servicios/nucleo-financiero`)
- Peldaño de evidencia alcanzado: `DISCOVERED` (lectura de código) + verificadores Python ejecutados. Gradle, yarn y Flutter: `BLOCKED` por entorno.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Rama creada desde `origin/test`, historial completo | `git rev-parse --is-shallow-repository` | PASS → `false` (tras `git fetch --unshallow`) |

## A medias
### H1.S1.M2 / M3 — Huella, mapa y lectura con cobertura
- Qué anda: huella medida (aportes main ≈6,2k líneas, nucleo-financiero main ≈15,8k, tests ≈30,8k); lectura repartida en 4 subagentes de solo lectura con lotes disjuntos (CLAUDE.md autoriza hasta 4).
- Qué no anda: el informe `AUDITORIA-aportes-nucleo-financiero.md` todavía no está escrito.
- Qué falta exactamente: integrar los cuatro informes, verificar yo cada hallazgo (70.4.8) y escribir las secciones 3 y 12.
- Dónde quedó: solo `PLAN.md` y este reporte en la carpeta; nada fuera de ella.
### H1.S1.M6 — Verificadores
- Qué anda: corrieron con exit 0 `verificar_contratos_limites`, `validar_flujos` (AVISO H8-01: `instruir-pago` sin cablear), `verificar_criterios`, `verificar_seguridad` (3 avisos de `DSL.field(variable)` en nucleo), `inventario_endpoints --check`, `matriz_de_pruebas --servicio …`, `verificar_boveda`, `verificar_carriles`, `verificar_frontend`, `verificar_pruebas_web`, `auditar_backend` («0/393 pruebas verificadas»), `listar_topicos`. `verificar_rutas_frontend` salió con exit 1 por una lista de conocidas desactualizada (`GET /grupos/solicitudes-escaladas`, no es de este módulo).
- Qué no anda: `./gradlew :servicios:aportes:test …` → `BUILD FAILED`: `Could not resolve org.postgresql:postgresql:42.7.12 … Received status code 429 from server: Too Many Requests` (Maven Central). No hay daemon de Docker (`failed to connect to the docker API`) para correr `integrationTest`. `yarn typecheck` → el proyecto pide yarn 4.18.0 por Corepack y hay 1.22.22 global, sin `node_modules`.
- Qué falta exactamente: nada ejecutable sin arreglar el entorno, y el metaprompt prohíbe hacerlo; queda NO COMPROBADO.
- Dónde quedó: salidas en el scratchpad de la sesión; se pegan recortadas en el informe.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S1.M4 | TODO | inventarios APROB/RESPALDO, tras integrar los subagentes |
| H1.S1.M5 | TODO | vista de conjunto |
| H1.S1.M7 | TODO | estándares por hallazgo |
| H1.S1.M8 | TODO | refutación de S1/S2 |
| H1.S1.M9 | TODO | commit y push finales |

## Evidencia
```text
$ ./gradlew --no-daemon :servicios:aportes:test :servicios:aportes:webTest :servicios:nucleo-financiero:test :servicios:nucleo-financiero:webTest --continue
> Could not resolve org.postgresql:postgresql:42.7.12. … Received status code 429 from server: Too Many Requests
BUILD FAILED in 38s
```

## No cubierto
Toda ejecución de pruebas Java/Flutter/Angular del módulo (ver arriba). Evidencia 91.6 y 98.8: NO COMPROBADA.

## Desvíos del plan
Ninguno todavía.

## Riesgos residuales
Hallazgos preliminares de severidad S1, ya verificados leyendo el código y pendientes de la refutación de la Fase 7: el aporte queda acreditado por declaración del cliente, sin webhook (`CU21CobrarAporte.java:103-168`, `PagoRepositorio.java:42`); el webhook no cambia el estado del pago (`CU100RecibirWebhookPasarela.java:165`).

## Decisiones y ambigüedades
A-1 a A-4 del metaprompt: se registran, no se resuelven (Pablo; Cumplimiento para lo regulatorio). El candado de la regla 65 señala microtareas BLOQUEADO/EN CURSO en planes **de otros trabajos** (`2026-10-03-…`, `2026-10-14-…`, `2026-10-16-…`, `2026-10-27-…`, `2026-10-28-…`, `2026-10-30-…`). Quedan fuera del alcance de esta auditoría de solo lectura: no se tocan, y se informan a Pablo.
