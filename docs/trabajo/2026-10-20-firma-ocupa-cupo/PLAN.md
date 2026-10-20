# Plan — Firmar el reglamento siempre deja a quien firma con un cupo

- Fecha: 2026-10-20 · Repos afectados: PasanakuBackend (servicio `grupos`) · Predecesor: 4e842cd2 (firma del reglamento)
- Resultado observable: el organizador de un grupo creado antes del 2026-10-20 12:29 (sin cupo reservado) firma el reglamento y aparece en la lista de integrantes con un cupo, «Firmó», y el contador sube.
- Kill-test: un participante ACEPTADO_PENDIENTE_FIRMA sin fila en `grupos.cupo` firma → si después no tiene cupo OCUPADO, no está hecho.

## Hallazgos (fase 1)
- `CreacionRepositorio.guardarAlta` reserva el cupo 1 al organizador recién desde 4e842cd2 (2026-10-20 12:29). Antes creaba al organizador `ACEPTADO_PENDIENTE_FIRMA` **sin cupo**.
- `CU69FirmarReglamento.firmar` pasa a ACTIVO y solo hace `UPDATE cupo ... WHERE participante_id = ? AND estado = 'RESERVADO'`: sin cupo reservado no ocupa ninguno.
- Caso real (captura del dueño, grupo «Cumpleaños de leo»): quien mira es el organizador (ve «Invitar a alguien a mi grupo»); el cupo 1 lo ocupa otra persona que entró por enlace (`CU69Enlace`, ACTIVO + OCUPADO); «1 de 10 cupos ocupados»; el organizador ya firmó y quedó ACTIVO sin cupo, sin botón para firmar.
- `ConsultarDetalleDelGrupo.debeFirmar` solo es verdadero en ACEPTADO_PENDIENTE_FIRMA.

## Alcance
- IN: `CU69FirmarReglamento` (asignar el primer cupo LIBRE a quien firma y no tiene cupo; reparar al ACTIVO sin cupo), `ConsultarDetalleDelGrupo` (`debeFirmar` también para el ACTIVO sin cupo con el grupo recibiendo integrantes), test de integración `CU69FirmaYSorteoTest`.
- OUT: la app (ya muestra «Firmar» cuando `debeFirmar` y ya traduce AP-CU69-01), datos de TEST (se reparan cuando el organizador vuelve a firmar). Del contrato solo cambia el comentario de rechazos de `firmarReglamentoDelGrupo` (+AP-CU69-01) y su colección Postman regenerada.
- Ambigüedades registradas: ninguna — la decisión del dueño (2026-10-20) es que el organizador también aporta y cobra con un cupo.

## H1 — Quien firma queda con un cupo
**CA:** Dado un integrante sin cupo en un grupo que recibe integrantes, cuando firma el reglamento, entonces queda ACTIVO con el primer cupo libre OCUPADO y `cupos_ocupados` lo cuenta.
**DoD:** `gradlew :servicios:grupos:integrationTest --tests bo.aportaya.grupos.CU69FirmaYSorteoTest` en verde (PostgreSQL real, Testcontainers) + regresión del servicio `grupos`.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Pendiente sin cupo firma → toma el primer cupo LIBRE | cupo OCUPADO + contador 2 de 3 | test `elOrganizadorSinCupoAlFirmarOcupaElPrimerLibre` | HECHO |
| H1.S1.M2 | ACTIVO sin cupo (ya firmó) ve «Firmar» y al firmar recupera su cupo sin duplicar la firma | `debeFirmar` true antes, false después; ninguna aceptación nueva | test `quienYaFirmoSinCupoLoRecupera` | HECHO |
| H1.S1.M3 | Sin cupos libres, firmar sin cupo se rechaza y no cambia nada | AP-CU69-01, sigue pendiente | test `sinCupoLibreNoFirma` | HECHO |
| H1.S1.M4 | Regresión: los 5 tests existentes de firma y sorteo siguen en verde | 0 fallos | misma corrida | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Dos firmas simultáneas sin cupo toman el mismo cupo | doble asignación | la fila del grupo ya se toma `FOR UPDATE` al empezar; el cupo se toma `FOR UPDATE` |
| Docker no disponible para Testcontainers | no se puede correr el DoD | se declara BLOQUEADO con la salida |
