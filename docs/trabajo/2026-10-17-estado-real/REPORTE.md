# Reporte — «Arquitectura y estado» con la salud REAL de cada servicio

> **AVANCE: 5 / 6 — 83,3 %.** Falta H1.S3.M1 (PRs a dev y test con CI en verde).

- Fecha: 2026-10-17 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/fix/estado-real-2026-10-17`
- Peldaño: backend `TESTED` (sonda contra un servidor HTTP real en loopback); frontend `VERIFIED_FUNCTIONAL_ONLY` +
  prueba visual (E2E con la respuesta de auditoria como DOBLE declarado). Falta verlo contra los servicios reales.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Contrato `GET /indicadores/operacion/servicios` (`consultarSaludDeServicios`) | `./gradlew :servicios:auditoria:compileJava` | PASS |
| H1.S1.M2 | `SondaDeSalud`: readiness en paralelo, timeout 2 s; 200→ARRIBA, otro→DEGRADADO, sin respuesta→CAIDO; 10 s de memoria | `test --tests *SondaDeSaludTest *SaludDeServiciosTest` | PASS 4/4 + 1/1 |
| H1.S1.M3 | Permiso TABLERO_SISTEMAS_VER o AUDITORIA_LEER; 403 con otro tablero sin tocar la sonda; 401 sin sesión | `webTest --tests *SaludDeServiciosWebTest *SeguridadWebTest` | PASS 3/3, 6/6 |
| H1.S2.M1 | La pantalla pinta lo que contestó cada servicio; «consultando», «sin permiso», «sin dato» nunca dicen «arriba» | `ng test …/pantalla-de-estado.spec.ts` | PASS 7/7 |
| H1.S2.M2 | Prueba visual: 3 viewports × 2 temas con datos; móvil y escritorio en los estados sin dato | `playwright test e2e/operacion-estado.e2e.ts` | PASS 8/8, `evidencia/capturas/` |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S3.M1 | TODO | Abrir PRs a dev y test y esperar el CI |

## Evidencia
- `evidencia/H1-S1-backend.txt` — suites y casos del backend.
- `evidencia/H1-S2-front.txt` — specs, a11y preexistente, eslint, Postman y los 4 hallazgos corregidos.
- `evidencia/H1-S2-e2e-operacion-estado.txt` — corrida del E2E (8 passed).
- `evidencia/capturas/` — 12 capturas (datos en 6 combinaciones; sin permiso, error y consultando en móvil claro y escritorio oscuro).

## No cubierto
- **La sonda contra los servicios reales en la red de Docker** (nombres `identidad:8080`…): probada contra un servidor
  HTTP real en loopback, no contra el compose. El kill-test del plan (apagar un servicio y verlo «no responde») queda
  para la pila D3 del trabajo de tableros o para TEST tras el despliegue.
- Réplicas EN MARCHA: no hay orquestador que consultar; la columna dice «exigidas (producción)».
- `pantalla-de-estado.a11y.spec.ts` vence por plazo (preexistente, mismo resultado sin el cambio).

## Desvíos del plan
- Las columnas se reordenaron (salud junto al servicio) tras ver en la captura que quedaban cortadas.
- Docker Desktop se cerró con la máquina saturada (load 245) a mitad del trabajo; se reabrió y se volvió a arrancar
  `aportaya-postgres` (de otra sesión) tal como estaba, sin cambios.

## Riesgos residuales
- Cada consulta (como mucho una cada 10 s) hace 14 GET internos. Si un servicio cuelga, la pantalla espera hasta 2 s.
- Si el nombre de un servicio en el compose cambia, sale «no responde» (la lista vive en `application.yml`).

## Gate de seguridad (regla 90.6)
| Amenaza | Control | Test | Resultado | Riesgo residual |
|---|---|---|---|---|
| Ver el estado interno sin permiso | `@Permiso TABLERO_SISTEMAS_VER` / `AUDITORIA_LEER` en `IndicadoresController.consultarSaludDeServicios` | `SaludDeServiciosWebTest.otroTablero` (403, la sonda no se llama); `SeguridadWebTest` (401) | PASS | — |
| Filtrar detalle de dependencias de otro servicio | La sonda descarta el cuerpo (`BodyHandlers.discarding()`): solo estado HTTP y tiempo | `SondaDeSaludTest` | PASS | — |
| Amplificación (cada visita = 14 llamadas) | 10 s de memoria + timeout por llamada | `SaludDeServiciosTest.vigencia` | PASS | Hasta 2 s de espera con un servicio colgado |
| Mostrar el error crudo del backend | La pantalla no muestra el mensaje: textos propios | E2E «error (500)» busca «detalle interno» → 0 | PASS | — |

## Decisiones y ambigüedades
- A1: quién ve la salud → TABLERO_SISTEMAS_VER o AUDITORIA_LEER; la pantalla sigue abierta y sin permiso lo dice.
  Confirmar con Pablo.
