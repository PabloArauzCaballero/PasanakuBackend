# Relocalización de referencias fuera de rango — AUDITORIA-identidad-cumplimiento.md

Verificación de solo lectura (2026-11-20). Método: ficha `### <ID>` del informe → identificador/literal citado → `grep -n` / `sed -n` sobre el archivo real. No se modificó ningún archivo del repo ni se corrió ningún build.
Rutas abreviadas: `CUMP/` = `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/`; `IDEN/` = `servicios/identidad/src/main/java/bo/aportaya/identidad/`; `NF/` = `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/`; `BO/` = `apps/backoffice/src/app/rutas/cumplimiento/`.
Nota: las semillas `sql/60_semillas/*.sql` están generadas desde `seeders/minimos/*.json`; los números citados tampoco cuadran con los JSON, así que se relocalizan en el `.sql`.

## Filas de `fuera-de-rango.txt` (35)

| ID | ruta | citado | veredicto | rango correcto | evidencia / qué se buscó | ¿la afirmación central cae? |
|---|---|---|---|---|---|---|
| IDCU-HUECO-040 | sql/60_semillas/11-contratos-de-adhesion.sql | 62-67 | RELOCALIZADO | 5-8 | 4 contratos con estado `'BORRADOR'` (`('CTO-BILLETERA', 1, 'BILLETERA', 'BORRADOR', ...)`) | No |
| IDCU-REG-046 | sql/60_semillas/08-gobierno-y-licencia.sql | 42-58 | RELOCALIZADO (con matiz) | 27-34 (matriz) y 37-43 (tipologías) | matriz: `'Instructivo EIF — enfoque basado en riesgo', current_date`; tipologías: `..., TRUE, now()`. Matiz: `activa` existe solo en `regla_monitoreo_lft` (la matriz no tiene esa columna); `aprobada_por` no se inserta en ninguna de las dos → queda NULL (columna confirmada en `sql/10_tablas/12_cumplimiento_asfi/*.sql`); las tipologías usan `now()`, no `current_date` | No (el detalle "matriz activa TRUE" no aplica, el resto se sostiene) |
| IDCU-SEG-059 | NF/infraestructura/SegundoFactorStepUp.java | 139-169 | RELOCALIZADO | 85-122 | valida `exp`, `iss` (:92), `aud` (:95-96), `proposito` (:102), `acr` (:105), `jti` (:109-122); grep `monto\|destino` sin coincidencias de validación | No |
| IDCU-APROB-060 | NF/infraestructura/BandejaDeRetirosRepositorio.java | 138-148 | RELOCALIZADO | 33-43 (select) y 63-73 (record `EnRevision`) | selecciona cuenta, solicitante, montos, moneda, `mfa_verificado`, fechas; javadoc :63 "sin destino ni datos de la persona" | No |
| IDCU-DUP-INT-063 | sql/60_semillas/08-gobierno-y-licencia.sql | 52-58 | RELOCALIZADO | 39 | `('ENTRADA_SALIDA_INMEDIATA', ... "minutos": 120 ..., 500, 1, 'ALTA', 'RETENER_OPERACION', ...)` | No |
| IDCU-DEBIDO-PROCESO-066 | sql/60_semillas/08-gobierno-y-licencia.sql | 54 | RELOCALIZADO | 39 | `accion_automatica` = `'RETENER_OPERACION'` en ENTRADA_SALIDA_INMEDIATA | No |
| IDCU-DATOS-072 | sql/60_semillas/11-contratos-de-adhesion.sql | 62-67 | RELOCALIZADO | 5-8 | `'https://pasanaku.bo/legal/...pdf', repeat('0', 64)` | No |
| IDCU-DATOS-072 | sql/60_semillas/13-politicas-de-token.sql | 72-74,89-90 | RELOCALIZADO | 6 y 23 | `('VERIFICACION_CORREO', 600, 6, ...)` (OTP) y `('VERIFICACION_CORREO', 86400, 32, ...)` (enlace) | No |
| IDCU-SEG-086 | IDEN/aplicacion/AnotarFotoEnExpediente.java | 91-106 | RELOCALIZADO | 32-47 | `ejecutar(...)` despacha `expedientes.anotarAnverso/.../anotarPerfilDerecho` sin chequear estado; el UPDATE sin precondición está en `ExpedienteRepositorio.java:49-51,64-71` (cita de la ficha válida) | No |
| IDCU-SEG-093 | IDEN/aplicacion/EmitirTokenDeInvitacion.java | 44-236 | RELOCALIZADO | 44-148 (`ejecutar`) | valida solo `clave/grupoId/telefono/canal` (:45-51); grep `administr\|propiet\|organizador` sin resultados → no hay chequeo de propiedad de `grupoId` | No |
| IDCU-DUP-INT-103 | IDEN/aplicacion/CU08AsignarRol.java | 248-300 | RELOCALIZADO | 51-102 | ámbito :59-64 (`ambito.completoCon(...)`), permisos resultantes con vigentes :67-68, MFA :98-99 (`faltaMfa = ... GLOBAL ... factorActivo`) | No |
| IDCU-RENDIMIENTO-104 | IDEN/infraestructura/RolesYPermisosRepositorio.java | 177-201 | RELOCALIZADO | 28-52 (y 54-82) | `listarRoles`: `.fetch()` sin límite (:33) y `accesos.permisosDelRol(dsl, rolId)` por fila (:38, N+1); `listarPermisos` sin límite (:66) | No |
| IDCU-FLUJO-FALTANTE-117 | CUMP/aplicacion/CU42RegistrarRog.java | 322-407 | RELOCALIZADO | 33-137 (clase; `registrar` :51) | `grep -rln CU42RegistrarRog servicios/cumplimiento/src/main` → solo su propio archivo | No |
| IDCU-FLUJO-FALTANTE-118 | CUMP/aplicacion/CU45AtenderRequerimiento.java | 370-385,391-400 | RELOCALIZADO | 104-120 y 128-137 | payload de `cumplimiento.requerimiento_recibido` con `exigeBloqueo` y `justificacionDeAcceso`, sin `usuarioAfectadoId` (:110-119); `anotarBloqueo` :128 sin llamador en main (solo `RequerimientoRepositorio.java:59`, que es el método del repositorio) | No |
| IDCU-SEG-119 | CUMP/aplicacion/CU02ElevarDiligencia.java | 258-266,278 | RELOCALIZADO | 124-133 y 145 | `if (esPep && entrada.segundaRevisionPor().isEmpty())` (:126); `diligencias.completar(dsl, diligenciaId, entrada.aprobadaPor(), entrada.segundaRevisionPor(), ahora)` (:133); `Optional.of(entrada.aprobadaPor())` (:145) | No |
| IDCU-SEG-119 | CUMP/aplicacion/CU02ElevarDiligencia.java | 282-287 | RELOCALIZADO | 149-154 | `limites.vigentesPara(dsl, destino.name(), ...)` → `TopeNuevo` | No |
| IDCU-SEG-120 | CUMP/aplicacion/CU44DecidirComoRevisor.java | 347-350 | RELOCALIZADO | 75-77 | `if (ctx.usuarioId().equals(caso.analistaId())) throw ... (44, 2)` | No |
| IDCU-FLUJO-FALTANTE-123 | CUMP/aplicacion/CU06RevisarConocimiento.java | 403-421 | RELOCALIZADO | 184-200 | `vencerYPedirLimitacion(...)` emite `cumplimiento.revision_vencida` | No |
| IDCU-FLUJO-FALTANTE-123 | CUMP/aplicacion/CU02ElevarDiligencia.java | 289-299 | RELOCALIZADO | 156-166 | `outbox.emitir(... "cumplimiento.diligencia_elevada" ...)` | No |
| IDCU-FLUJO-FALTANTE-123 | CUMP/aplicacion/CU03DeclararPep.java | 492-529 | RELOCALIZADO | 146-156 y 171-201 | `cumplimiento.pep_declarado` (:149); `alConfirmarseCoincidencia` (:172) emite `cumplimiento.caso_lft_abierto` (:195); grep de `alConfirmarseCoincidencia` en main → sin llamadores | No |
| IDCU-FLUJO-FALTANTE-123 | CUMP/aplicacion/CU06RevisarConocimiento.java | 415-416 | RELOCALIZADO | 195-196 | `UUID.fromString(ctx.traza().id()), Map.of("cantidad", Integer.toString(vencidas))` (traza como agregado, solo cantidad) | No |
| IDCU-DEFECTO-126 | CUMP/aplicacion/CU45AtenderRequerimiento.java | 416-423 | RELOCALIZADO | 149-165 | `gobierno.abrirHallazgo(dsl, "OFI-" + entrada.numeroOficio().substring(0, Math.min(16, ...)) ...)` | No |
| IDCU-DATOS-127 | CUMP/ConfiguracionDeCumplimiento.java | 38-107 | RELOCALIZADO | 34-101 | `new PeriodicidadDeRevision(6, 12, 24)` :48; `"CRITICA", Duration.ofDays(5) ... "BAJA", Duration.ofDays(45)` :69-72; 24/48/72/120 h :80-84; `Escala(15, 8)` :90; `Set.of("TESORERIA", "OPERACIONES", "COMERCIAL")` :101. Nota: `cortesDeSeveridad` no está aquí sino en `CU54EscalarPlanesVencidos.java:123-129` (ya citado en la ficha) | No |
| IDCU-DATOS-127 | CUMP/aplicacion/CU45AtenderRequerimiento.java | 430 | RELOCALIZADO | 165 | `ahora.toLocalDate().plusDays(15)` | No |
| IDCU-HUECO-129 | CUMP/aplicacion/CU06RevisarConocimiento.java | 403-421 | RELOCALIZADO | 184-200 | `vencerYPedirLimitacion`; grep en `src/main` → solo su declaración, sin llamador | No |
| IDCU-DEFECTO-131 | CUMP/aplicacion/CU02ElevarDiligencia.java | 309 | RELOCALIZADO | 176 | `diligencias.elevarTipo(dsl, usuarioId, destino.name());` (en `abrirOElevar`) | No |
| IDCU-DEFECTO-131 | CUMP/aplicacion/CU03DeclararPep.java | 459-460 | RELOCALIZADO | 132-133 | `diligencias.elevarTipo(dsl, entrada.usuarioId(), DILIGENCIA_REFORZADA);` | No |
| IDCU-CONTRATO-132 | CUMP/aplicacion/CU02ElevarDiligencia.java | 315-321 | RELOCALIZADO | 182-188 | record `EntradaDiligencia(ClaveIdempotencia clave, ...)` :183; grep `clave` → única aparición, nunca se usa | No |
| IDCU-DEBIDO-PROCESO-140 | CUMP/aplicacion/CU02ElevarDiligencia.java | 233-237 | RELOCALIZADO | 100-103 | `"Hay una coincidencia de lista sin resolver: el nivel no sube hasta que se cierre."` | No |
| IDCU-DEBIDO-PROCESO-140 | CUMP/aplicacion/CU03DeclararPep.java | 509-526 | RELOCALIZADO | 182-199 | `"Coincidencia de lista confirmada para " + nombreCoincidente` (:188); evento `caso_lft_abierto` con `usuarioId` (:195-198) | No |
| IDCU-RENDIMIENTO-143 | CUMP/aplicacion/CU06RevisarConocimiento.java | 336-346 | RELOCALIZADO | 116-127 | `if (desvio.ameritaAlerta()) { ... revisiones.abrirAlerta(...)` en cada corrida | No |
| IDCU-DATOS-144 | BO/textos.ts | 163 | RELOCALIZADO | 60 | `etapas: ['Causal', 'Notificación', 'Plazo', 'Descargo', 'Decisión motivada', 'Apelación']` | No |
| IDCU-DATOS-144 | BO/textos-lft.ts | 111,148 | RELOCALIZADO | 23 y 60 | `'... Al titular no se le avisa nada (deber de reserva).'`; `'... Nunca se notifica al titular.'` | No |
| IDCU-HUECO-156 | BO/textos-lft.ts | 100,112-113 | RELOCALIZADO | 12 y 24-25 | `elegirAlertas: 'Marcá las alertas de un mismo titular...'`; `analista: 'Identificador del analista asignado'`, `analistaAyuda: 'El UUID de la persona...'` | No |
| IDCU-DUP-INT-164 | BO/textos-lft.ts | 105-106,131-138 | RELOCALIZADO | 17-18 y 43-50 | `estadosDeAlerta`, `severidades`; `estadosDeCaso`, `prioridades`, `decisiones` como `Record<string,string>` a mano | No |

## Filas agregadas por el coordinador

| ID | ruta | citado | veredicto | rango correcto | evidencia / qué se buscó | ¿la afirmación central cae? |
|---|---|---|---|---|---|---|
| IDCU-DINERO-135 | CUMP/aplicacion/CU40EvaluarLimites.java | 529 | RELOCALIZADO | 97 | `public record EntradaLimite(UUID cuentaBilleteraId, String concepto, BigDecimal monto) {}` (sin moneda) | No |
| IDCU-DINERO-135 | CUMP/aplicacion/CU40EvaluarLimites.java | 511 | RELOCALIZADO | 79 | `"monto", entrada.monto().toPlainString(),` en `cumplimiento.limites_evaluados`, sin moneda | No |
| IDCU-HUECO-124 | servicios/cumplimiento/src/main/resources/application.yml | 34 | CITA CORRECTA (en rango; el archivo tiene 90 líneas) | 34 (bajo `publicacion:` :32) | `base-url: ${BASE_URL_PUBLICA:https://aportaya.bo}`; usado en `CU43RemitirReportes.java:59,247` (`baseUrlDeArchivos + "/reportes/" + codigo + "-" + periodo`) | No |
| IDCU-CLEAN-161 | BO/textos.ts | 124-125 | RELOCALIZADO | 21-22 | `estados: { PENDIENTE: ..., EN_REVISION: ..., APROBADA: ..., RECHAZADA: ... }` y `caras: {...}` en `verificaciones`; grep `t.estados`/`t.caras` en los componentes que usan `textosCumplimiento.verificaciones` (`tira-de-fotos.ts`, `decision-de-expediente.ts`, `vencimiento-del-documento.ts`) → sin usos | No |
| IDCU-DUP-INT-164 | BO/textos.ts | 124-125 | RELOCALIZADO | 21-22 | mismo diccionario `estados` que duplica `ESTADOS` de `BO/dominio/cu02-presentacion.ts:13-30` (esa cita sí es correcta) | No |
| IDCU-PRUEBAS-175 | BO/compartido/pruebas.apoyo.ts | 14-18 | CITA CORRECTA (en rango; 73 líneas) | 14-18 | `contra HttpTestingController (doble del servidor, declarado como tal ...)`. Ojo: existe también `apps/backoffice/src/app/rutas/sistemas/compartido/pruebas.apoyo.ts`; la del módulo cumplimiento es la correcta | No |

## IDCU-DINERO-135 — revisión del resto de referencias de la ficha

| ref citada | veredicto | evidencia |
|---|---|---|
| `dominio/EvaluacionDeLimites.java:24-49` | CORRECTA | `evaluar(List<Tope> topes, BigDecimal monto)` :24, sin moneda |
| `CU52AtenderReclamo.java:251-258` | CORRECTA | record `EntradaReclamo(..., BigDecimal montoReclamado, ...)` (montoReclamado :256) |
| `ConsultarBandejaDeReclamos.java:95` | CORRECTA | `/** ... la moneda no esta modelada (A-7). */` |
| `CU53ElevarReclamo.java:162-163` | CORRECTA | `EntradaResolucion(UUID instanciaId, String resolucion, BigDecimal montoResarcido, ...)` :163 |
| `CU56EjecutarPruebaDeContinuidad.java:133` | CORRECTA | `payload.put("perdidaBruta", entrada.perdidaCuantificada());` sin moneda |
| `ConsultarTableroDeCumplimiento.java:55` | CORRECTA | `BigDecimal montoTotal,` en un record sin campo moneda (la `moneda` de :82 pertenece a otro record) |
| `CU41RegistrarPcc01.java:64-74,257` | CORRECTA | `entrada.tipoDeCambio()` del llamador (:66-73), `entrada.monto().multiply(tipoDeCambio)` (:74); `BigDecimal tipoDeCambio` en la entrada (:257) |

Conclusión DINERO-135: solo las dos referencias a `CU40EvaluarLimites.java` estaban mal (529→97, 511→79); el resto de la ficha es exacto y la afirmación central se sostiene.

## Resumen
- 35 filas originales: 35 RELOCALIZADO, 0 CONTENIDO NO ENCONTRADO.
- 6 filas agregadas: 4 RELOCALIZADO, 2 CITA CORRECTA (HUECO-124 `application.yml:34`, PRUEBAS-175 `pruebas.apoyo.ts:14-18`).
- Afirmaciones centrales que caen: ninguna. Detalles menores inexactos: REG-046 (la matriz no tiene `activa`; las tipologías usan `now()`), DATOS-127 (`cortesDeSeveridad` vive en CU54, no en la configuración).
