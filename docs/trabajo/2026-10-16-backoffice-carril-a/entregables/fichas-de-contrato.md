# Fichas de contrato para el carril B — backoffice

> Una ficha por operación publicada en este carril (COORDINACION.md, «Interfaz de coordinación
> obligatoria»). La fuente de verdad es el YAML citado; si difiere de esta ficha, gana el YAML.
> Convenciones comunes (errores, cursor, importes, zona, `X-Request-Id`): `convenciones-contrato.md`.
> **Los ejemplos son SINTÉTICOS**, hechos a mano para mostrar la forma: ningún id ni importe es real.
> El cliente Angular se genera en CI (`./gradlew generateOpenApiClients`): no se escriben tipos a mano.

Notas transversales para B:

- Todas las listas devuelven `{ items, proximo… , proximoId }`; con `proximoId = null` no hay más
  páginas. El cursor se reenvía tal cual (`despuesDe… = proximo…`, `despuesDeId = proximoId`).
- Decidí por `codigo` (`AP-…`), no por el estado HTTP (A-5 del plan).
- Toda acción manda `Idempotency-Key` (UUID). Para reintentar tras perder la respuesta, **misma**
  clave y mismo cuerpo.

---

### 1. `listarRetirosEnRevision` — `GET /api/v1/billetera/retiros` (nucleo-financiero, CU-11)

- Permiso: `RETIRO_APROBAR`. Pantalla: `operacion/retiros` (reemplaza el ingreso manual de UUID).
- Filtros: `limite` 1–100 (25), cursor `despuesDeFecha` + `despuesDeId`. Orden: `solicitadaEn`, `id`.
- Errores: 400 límite/UUID inválido · 403 · 422 `AP-CU11-14` cursor a medias.
- Fila sin destino ni datos de la persona; `puedeDecidir=false` en la orden que pidió quien consulta.

```json
{ "items": [ { "ordenRetiroId": "00000000-0000-4000-8000-0000000000a1",
  "cuentaBilleteraId": "00000000-0000-4000-8000-0000000000a2",
  "solicitadaPor": "00000000-0000-4000-8000-0000000000a3",
  "solicitado": { "monto": "6000.00", "moneda": "BOB" }, "costo": { "monto": "5.00", "moneda": "BOB" },
  "neto": { "monto": "5995.00", "moneda": "BOB" }, "mfaVerificado": true,
  "solicitadaEn": "2026-10-16T14:30:00Z", "ventanaEnfriamientoHasta": null, "puedeDecidir": true } ],
  "proximoFecha": null, "proximoId": null }
```

### 2. `aprobarRetiro` — `POST /api/v1/billetera/retiros/{ordenId}/aprobacion` (nucleo-financiero, CU-11)

- Permiso: `RETIRO_APROBAR`; el solicitante no puede decidir (`AP-CU11-10`).
- Cuerpo: `{ "desenlace": "AUTORIZADA" | "RECHAZADA", "motivo"?: string ≤500 }`.
- Idempotencia: misma clave + misma decisión → **mismo recibo 200** (sin segunda transición).
  Misma clave con otra decisión u otra orden → `AP-CU11-13`. Otra clave sobre orden ya resuelta →
  `AP-CU11-11` (hoy 422, el YAML dice 409 — A-5).
- Estados de la orden: `EN_REVISION` → `AUTORIZADA` | `RECHAZADA` (libera la retención).

### 3. `listarBandejaDeReclamos` — `GET /api/v1/reclamos` (cumplimiento, CU-52)

- Permiso: `RECLAMO_VER`. Pantalla: `operacion/reclamos`.
- Filtros: `estados` (CSV de `INGRESADO,EN_ANALISIS,RESPONDIDO,CERRADO,ELEVADO`; por omisión
  abiertos), `limite`, cursor `despuesDeLimite` + `despuesDeId`. Orden: límite vigente (prórroga si
  la hay), `id`. **Cambio para B:** antes se suponía una lista sin paginar (`cu52-reclamos.ts:10-17`).
- Errores: 422 `AP-CU52-07` filtro o cursor inválido.
- `montoReclamado` es cadena **sin moneda** (A-7): mostrar "moneda no registrada", no "Bs".
- `vencido` = sin respuesta y límite guardado ya pasado. No hay `responsableNombre` (dato de identidad).

### 4. `consultarReclamo` — `GET /api/v1/reclamos/{reclamoId}` (cumplimiento, CU-52)

- Permiso: `RECLAMO_ATENDER` (el texto lo ve solo quien atiende). 422 `AP-CU52-05` inexistente.
- Devuelve `{ reclamo: <fila de bandeja>, descripcion, resultado?, respuesta?, fechaRespuesta?, devolucionId? }`.

### 5. `responderReclamo` — `POST /api/v1/reclamos/{reclamoId}/respuesta` (cumplimiento, CU-52)

- Permiso: `RECLAMO_ATENDER`. Cuerpo: `{ "resultado": "FAVORABLE|DESFAVORABLE|PARCIAL|DESISTIDO",
  "respuesta": string 1–4000, "devolucionId"?: uuid }` (sin campos de más: 400).
- `FAVORABLE` con monto exige `devolucionId` (CU-33): `AP-CU52-03`. Fuera de plazo se acepta y abre hallazgo.
- Idempotencia por estado: mismo responsable + misma respuesta → mismo recibo, sin segundo evento;
  otra respuesta sobre un cerrado → `AP-CU52-06`. Estado resultante: `CERRADO`.
- Reemplaza el diálogo que simulaba enviar (`bandeja-de-reclamos.ts:150`).

### 6. `listarEjerciciosFiscales` — `GET /api/v1/erp/ejercicios` (erp, CU-100)

- Permiso: `CONTABILIDAD_ERP_REPORTES` **o** cualquier otro `CONTABILIDAD_ERP_*` (los mismos que
  abren la sección en `secciones.ts:38-47`).
- Filtros: `limite` 1–20 (5), cursor `antesDeAnio`. Orden: año descendente; cada ejercicio con sus
  períodos (mes, estado `ABIERTO|CERRADO`, fechas `date`).

### 7. `consultarPeriodoVigente` — `GET /api/v1/erp/periodos/vigente` (erp, CU-100)

- Mismos permisos. Devuelve el período que contiene hoy en America/La_Paz, con su estado aunque esté
  `CERRADO`. Sin período para hoy → 422 `AP-CU100-04`: la pantalla lo dice; no elige otro.
- **Reemplaza** la redirección a `periodo/11111111-1111-4111-8111-111111111111`
  (`contabilidad.routes.ts:28`).

```json
{ "periodoId": "00000000-0000-4000-8000-0000000000c1", "ejercicioId": "00000000-0000-4000-8000-0000000000c2",
  "anio": 2026, "mes": 10, "estado": "ABIERTO", "fechaInicio": "2026-10-01", "fechaFin": "2026-10-31" }
```

### 8. `listarBandejaDeAlertas` — `GET /api/v1/cumplimiento/alertas` (cumplimiento, CU-44)

- Permiso: `CUMPLIMIENTO_ALERTAS` (oficial y analista). Pantalla: `cumplimiento/alertas`.
- Filtros: `estados` (`ABIERTA,EN_ANALISIS,ESCALADA,DESCARTADA`; por omisión abiertas),
  `severidades` (`CRITICA,ALTA,MEDIA,BAJA`), `limite`, cursor `despuesDeFecha` + `despuesDeId`.
- Sin persona, monto ni detalle (deber de reserva, A-8). 422 `AP-CU44-05`.

### 9. `listarBandejaDeCasos` — `GET /api/v1/cumplimiento/casos` (cumplimiento, CU-44)

- Permiso: `CUMPLIMIENTO_CASOS` (solo `OFICIAL_CUMPLIMIENTO`). Orden: plazo guardado, `id`; cursor
  `despuesDePlazo` + `despuesDeId`. Sin persona, resumen ni hallazgos. `vencido` contra el plazo guardado.

### 10. `abrirCasoLft` — `POST /api/v1/cumplimiento/casos` (cumplimiento, CU-44)

- Permiso: `CUMPLIMIENTO_CASOS`. Cuerpo: `{ "alertaIds": [uuid, 1–50], "analistaId": uuid, "resumen": 1–500 }`.
- 201 `{ casoId, estado, plazoLimite }`. Reintento con las mismas alertas y analista → el mismo caso.
  Alertas inexistentes, de titulares distintos o ya en otro caso → `AP-CU44-08`.

### 11. `decidirCasoLft` — `POST /api/v1/cumplimiento/casos/{casoId}/decision` (cumplimiento, CU-44)

- Permiso: `CUMPLIMIENTO_CASOS`. Cuerpo: `{ "decision": "DESCARTAR|MONITOREO_REFORZADO|REPORTAR|TERMINAR_RELACION",
  "conclusion": 20–2000, "reporteSospechosoId"?: uuid }`. **No** se manda revisor ni analista (400 si se manda).
- El revisor es la sesión y debe ser distinto del analista del caso (`AP-CU44-02`). `REPORTAR` sin ROS →
  `AP-CU44-03`. Inexistente `AP-CU44-06`. Otra decisión sobre un decidido `AP-CU44-07`; la misma
  decisión del mismo oficial repetida → mismo resultado.
- Nunca se notifica al titular: la UI no debe ofrecer "avisar al cliente" en este flujo.
