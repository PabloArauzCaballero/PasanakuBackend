# Coordinación con la sesión `pasanaku-c1` — 2026-10-30

Reparto acordado por `SendMessage` (todos `success:true`):

| Lado | Qué |
|---|---|
| `pasanaku-1f` (este trabajo) | `aportes`: consumidor de `grupos.grupo_iniciado`, `CU21AbrirPrimeraCuota`, `GET /aportes/grupos/{grupoId}/primera-cuota`, contrato y tests. `grupos`: puerto `PrimerAporteDelCupo`, adaptador `PrimerAportePorHttp`, `AceptacionVigenteRepositorio`, precondiciones `AP-CU60-11/12` y `AP-CU60-05` por versión vigente en `CU60Sortear`, tests. |
| `pasanaku-c1` | `CU69IniciarGrupo` (inicio al ≥ 75 %, achica el grupo, publica reglamento N+1), **emisión** del evento `grupos.grupo_iniciado` por outbox en esa transacción, `puedeSortear` en `ConsultarDetalleDelGrupo` usando mi puerto, `grupos.yaml`, app Flutter (botón, re-firma). Rama `claude/arenas-y-inicio-75-20261030` @ `3e94c14a` publicada. |

Contrato del evento enviado (msg `524fbb45…`) y **confirmado por c1** («lo dejé registrado en mi PLAN.md (H3) tal cual»):
tipo `grupos.grupo_iniciado`, agregado `grupo`, carga `{ grupoId, moneda, cuposTotales, iniciadoEn, periodo{periodoId, numero:1, fechaInicio, fechaLimitePago, fechaFinGracia, fechaEntregaPrevista}, cupos[{cupoId, numero, participanteId, montoAporte}] }`.

Decisiones de Pablo recibidas vía c1 (AskUserQuestion de c1, 2026-10-30):
- La primera cuota nace al iniciar el grupo; el sorteo se habilita cuando todas están acreditadas por webhook verificado.
- Al iniciar se publica la versión N+1 del reglamento y cada ocupante debe aceptarla antes del sorteo.

Pendiente del lado de c1 (no de este trabajo): emitir el evento; abrir el período 1 (`PROGRAMADO → ABIERTO`) si el cobro de la cuota 1 lo exige (c1 dijo que lo verifica y, si cambia una regla de negocio, se lo pregunta a Pablo).
