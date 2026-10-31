# Plan — El sorteo exige la primera cuota pagada (R-GRP-20, lado `aportes` + precondición en `grupos`)

- Fecha: 2026-10-30 · Repos afectados: Pasanaku (`servicios/aportes`, `servicios/grupos`) · Rama: `claude/sorteo-primera-cuota-20261030` desde `origin/test` · Worktree: `Pasanaku-wt-sorteo`
- Predecesor: `docs/trabajo/2026-10-30-regla-sorteo-75-y-comision-por-turno/` (la regla en docs) · Carril hermano: `pasanaku-c1`, `docs/trabajo/2026-10-30-arenas-e-inicio-75/` (`iniciarGrupo`, emisión del evento, `puedeSortear`, app)
- Resultado observable: el organizador de un grupo recién iniciado que intenta comprometer el sorteo con algún cupo sin la cuota 1 pagada recibe `422 AP-CU60-11` con la lista de cupos pendientes; cuando todos pagaron, el compromiso procede como hoy. Y al iniciarse un grupo, `aportes` abre sola la obligación de la cuota 1 de cada cupo ocupado, una vez, aunque el evento llegue repetido.
- Kill-test: `./gradlew :servicios:grupos:integrationTest --tests 'bo.aportaya.grupos.CU60Test'` en verde **sin** el caso «cupo sin cuota 1 → AP-CU60-11», o `aportes` sin consumidor del tema `aportaya.grupos.grupo_iniciado`.

## Alcance
- IN (`aportes`): consumidor Kafka de `grupos.grupo_iniciado` + caso de uso idempotente que abre la obligación de la cuota 1 por cupo reutilizando `RegistroDelCalendario`; endpoint `GET /aportes/grupos/{grupoId}/primera-cuota`; contrato en `aportes.yaml`; tests de CU, de consumidor y web.
- IN (`grupos`): puerto nuevo `dominio/puertos/PrimerAporteDelCupo.java` + adaptador `infraestructura/clientes/PrimerAportePorHttp.java`; precondiciones `AP-CU60-11` (cupos pendientes) y `AP-CU60-12` (no se pudo confirmar con `aportes`) en `aplicacion/CU60Sortear.java`; extensión de `AP-CU60-05` a la versión vigente del reglamento mediante una consulta en clase propia; tests `CU60Test`/`BaseDeCU60` y de los adaptadores.
- OUT: `CU69IniciarGrupo`, `SorteoRepositorio`, `HechosDeOtrosServicios`, `HechosPorHttp`, `ConsultarDetalleDelGrupo.puedeSortear`, `grupos.yaml`, cualquier `.dart` (todo de `pasanaku-c1`). Abrir el período 1 (`PROGRAMADO → ABIERTO`) y la transición a `EN_CURSO`: no existen hoy y no son de este trabajo; se registran. Compose de despliegue (`generar_compose.py`, `docker-compose.coolify.yml`): el flag nuevo nace con `matchIfMissing = true`, no exige variable.
- Ambigüedades registradas:
  1. **«Acreditada»** → obligación `PAGADO` del período 1 (la obligación no tiene estado `ACREDITADO`; el pago sí). Supuesto tomado; confirmar: Pablo.
  2. **Cuál es «el período 1» desde `aportes`** sin leer `grupos.periodo` (regla 98.1.1): la obligación `APORTE_PERIODICO` no anulada con menor `fecha_vencimiento` del grupo. Supuesto tomado; alternativa: que `grupos` pase el `periodoId`.
  3. **Quién puede consultar la primera cuota**: `@Permiso("BILLETERA_VER")`, el mismo del precedente `contarMorosos` (ORGANIZADOR y PARTICIPANTE lo tienen); el chequeo de pertenencia al grupo lo hace `grupos`, que es quien llama con el token del usuario. Riesgo registrado (ver abajo).
  4. **Extender AP-CU60-05 a la versión vigente**: pedido de Pablo vía c1. Depende de que `aceptacion_reglamento.reglamento_id` apunte a una fila de `reglamento_grupo` con `vigente_hasta IS NULL`; si los fixtures actuales no lo cumplen, se ajustan los fixtures, no la regla.

## Contrato del evento (lo emite `grupos` por outbox en la transacción de `iniciarGrupo`; lo define este plan)

- `tipo`: `grupos.grupo_iniciado` · `agregado`: `grupo` · `agregadoId`: `grupoId` → tema `aportaya.grupos.grupo_iniciado`, clave `grupoId`.
- Carga (todo lo que `aportes` necesita, sin volver a preguntar; importes como cadena de 2 decimales):

```json
{
  "grupoId": "uuid",
  "moneda": "BOB",
  "cuposTotales": 5,
  "iniciadoEn": "2026-10-30T15:00:00Z",
  "periodo": { "periodoId": "uuid", "numero": 1, "fechaInicio": "2026-11-01", "fechaLimitePago": "2026-11-30", "fechaFinGracia": "2026-12-03", "fechaEntregaPrevista": "2026-12-04" },
  "cupos": [ { "cupoId": "uuid", "numero": 1, "participanteId": "uuid", "montoAporte": "500.00" } ]
}
```

## H1 — `aportes` abre la cuota 1 al enterarse del inicio
**CA:** Dado un evento `grupos.grupo_iniciado` con N cupos, cuando llega (una o varias veces), entonces existen exactamente N `obligacion_aporte` `APORTE_PERIODICO`/`PENDIENTE` del período 1, y la segunda entrega no crea nada ni emite nada.
**DoD:** `./gradlew :servicios:aportes:integrationTest --tests 'bo.aportaya.aportes.CU21PrimeraCuotaTest'` en verde con los casos: correcto (N obligaciones), duplicado (0 nuevas), inválido (payload sin cupos / montos no positivos → rechazado sin efecto).
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `aplicacion/CU21AbrirPrimeraCuota.java`: dedup por `Consumidos` + `RegistroDelCalendario.registrar` con el período armado desde la carga | Evento → N obligaciones; repetido → 0 | `CU21PrimeraCuotaTest` 8/8 (correcto, mismo evento, re-emisión con otro id) · `evidencia/aportes-resultados-xml.txt` | HECHO |
| H1.S1.M2 | `consumo/ConsumidorDeGrupoIniciadoKafka.java` con flag `aportaya.grupo-iniciado.consumo-habilitado` y parseo del sobre (event_id, correlation_id) | Cabecera o campo faltante → excepción, sin consumir | `ConsumidorDeGrupoIniciadoKafkaTest` 3/3 · `evidencia/aportes-resultados-xml.txt` | HECHO |
| H1.S1.M3 | Carga inválida (sin cupos, monto ≤ 0, período ≠ 1) se rechaza sin tocar la base | 0 obligaciones | casos inválidos del `CU21PrimeraCuotaTest` (3) · `evidencia/aportes-resultados-xml.txt` | HECHO |

## H2 — `aportes` responde quién pagó la cuota 1
**CA:** Dado un grupo con obligaciones del período 1, cuando `grupos` consulta `GET /aportes/grupos/{grupoId}/primera-cuota`, entonces recibe `cuposPagados` (obligación `PAGADO`) y `cuposPendientes`; sin obligaciones responde listas vacías y `periodoId` nulo.
**DoD:** contrato en `aportes.yaml` con `additionalProperties: false`; `./gradlew :servicios:aportes:webTest --tests '*PrimeraCuotaWebTest'` y caso de consulta en `CU21PrimeraCuotaTest` en verde.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | `aportes.yaml`: operación `consultarPrimeraCuota` + schema `PrimeraCuota` | El servidor generado compila | `compileJava` exit 0 · `evidencia/aportes-suite-2.txt` (BUILD SUCCESSFUL) | HECHO |
| H2.S1.M2 | Repositorio + `aplicacion/ConsultarPrimeraCuota.java` + método en `AportesController` con `@Permiso("BILLETERA_VER")`; el record del resultado vive en `dominio/PrimeraCuotaDelGrupo` (la regla de arquitectura prohíbe web → infraestructura) | Pagados/pendientes correctos tras un cobro | `CU21PrimeraCuotaTest` «consulta tras un cobro» y «sin obligaciones» · `ArquitecturaTest` 5/5 | HECHO |
| H2.S1.M3 | `PrimeraCuotaWebTest`: 200 con sesión y permiso; 401 sin token; 403 sin permiso; 400 id inválido | Matriz negativa de autorización (regla 80.2.3) | `webTest` 5/5 · `evidencia/aportes-resultados-xml.txt` | HECHO |

## H3 — `grupos` no compromete el sorteo sin la cuota 1 de todos
**CA:** Dado un grupo `CONFORMADO` con un cupo sin cuota 1 pagada, cuando el organizador compromete el sorteo, entonces recibe `422 AP-CU60-11` con `cuposPendientes`; si `aportes` no responde, `422 AP-CU60-12`; con todos pagados, el compromiso procede.
**DoD:** `./gradlew :servicios:grupos:integrationTest --tests 'bo.aportaya.grupos.CU60*'` en verde incluidos los tres casos nuevos; adaptador probado con WireMock/doble como `HechosPorHttpTest` y `HechosCaidaTest`.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Puerto `dominio/puertos/PrimerAporteDelCupo.java` (`Optional<Set<UUID>> cuposConPrimeraCuotaPagada(UUID grupoId)`) + `infraestructura/clientes/PrimerAportePorHttp.java` con `ClienteConCircuito` sobre `aportaya.servicios.aportes` | GET correcto → set; 4xx/caída → `empty()` | `PrimerAportePorHttpTest` 6/6 (correcto, sin obligaciones, cuerpo incompleto, demora, 4xx, 5xx) · `evidencia/grupos-resultados-xml.txt` | HECHO |
| H3.S1.M2 | `CU60Sortear.comprometer`: consulta al puerto **antes** de `datos.conContexto`; dentro, pendientes = plantel ocupado − pagados → `AP-CU60-11` con `detalle.cuposPendientes` (números de cupo); `empty()` → `AP-CU60-12` | Los tres casos del CA | `CU60Test` «rechazaSinPrimeraCuota» (11, detalle `[2]`) y «rechazaSinRespuestaDeAportes» (12) · `evidencia/grupos-suite-2.txt` | HECHO |
| H3.S1.M3 | `BaseDeCU60` y los tests existentes de CU-60 reciben un doble del puerto que responde «todos pagados» para no cambiar su semántica | Suite CU-60 previa sigue en verde | `CU60Test` 16/16 + `CU60SnapshotTest` 10/10 · `evidencia/grupos-resultados-xml.txt` | HECHO |
| H3.S1.M4 | `AP-CU60-05` mira la versión vigente: consulta nueva en `infraestructura/AceptacionVigenteRepositorio.java` (cupos ocupados sin aceptación de un `reglamento_grupo` con `vigente_hasta IS NULL`) reemplaza a `sorteos.todosAceptaronElReglamento` en `CU60Sortear` | Participante con aceptación de una versión vieja → `AP-CU60-05` | `CU60Test` «aceptaFirmaDeLaVersionVigente» (procede) y «rechazaFirmaDeVersionAnterior» (05, `cuposSinFirma [1,2,3]`). Hallazgo: la fixtura `cuposOcupados` no asigna participante, así que la verificación vieja pasaba en vacío; los casos nuevos usan `participantesConCupo` + reglamento + aceptaciones sembrados | HECHO |

## H4 — Documentación y entrega
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | CU-60 (`docs/CasosDeUso`): errores `AP-CU60-11/12` y `05` por versión vigente; README de `aportes` (consumidor nuevo, endpoint) | Los códigos del código están en el doc | `grep -c "AP-CU60-1[12]" "docs/CasosDeUso/CU-60 Sortear los turnos.md"` → 4 | HECHO |
| H4.S1.M2 | Contrato del evento enviado a `pasanaku-c1` y confirmado | c1 emite exactamente ese payload | msg `524fbb45…` + confirmación de c1 · `evidencia/coordinacion-c1.md` | HECHO |
| H4.S1.M3 | `REPORTE.md` con evidencia por microtarea | — | `REPORTE.md` en disco (regla 40) | HECHO |

## Estado de los hitos
H1 HECHO · H2 HECHO · H3 HECHO (regresión completa de `grupos` 513/513) · H4 HECHO. Las regresiones de módulo de ambos servicios en verde con `spotlessCheck` (`evidencia/grupos-regresion-2.txt`, `evidencia/aportes-regresion-3.txt`).

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| jOOQ se genera contra una base viva en `127.0.0.1:5433`, hoy ocupada por `pg-arenas` (de c1) | Compilar mi worktree lee su esquema; si difiere, el código generado difiere | Solo lectura; si falla, levanto Postgres propio en otro puerto con `BD_URL_ADMIN` |
| `aportes.yaml` lo toca también c1 (no debería: su contrato es `grupos.yaml`) | Conflicto de merge | Avisado; mi cambio es aditivo |
| Autorización del endpoint por `BILLETERA_VER` sin chequeo de pertenencia en `aportes` | Un usuario podría consultar cupos pagados de un grupo ajeno (solo UUIDs) | Mismo riesgo que `contarMorosos`; registrado para `authz` |
| Nadie abre el período 1 (`PROGRAMADO`) ni pasa el grupo a `EN_CURSO` | El cobro de la cuota 1 (CU-21) puede exigir período `ABIERTO` | Fuera de alcance; se registra para c1/Pablo en el reporte |
| `RegistroDelCalendario` puede ser `@Transactional` y exigir sesión | Anidar dentro de mi transacción | Leer su firma antes de escribir (M1) |
