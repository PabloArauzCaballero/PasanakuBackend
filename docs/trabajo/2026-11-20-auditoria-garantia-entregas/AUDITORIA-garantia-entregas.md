# AUDITORÍA — módulo garantia-entregas (solo lectura)

> Documento en construcción: las secciones 1–4 y 6–12 se completan al cierre. Los hallazgos de la
> sección 5 se catalogan a medida que se verifican (indicación de Pablo, 2026-11-20).

## 5. Hallazgos

Convención: `garantia-entregas-<CLASE>-<NNN>`. Severidad S1–S4 según §10 del metaprompt. Cada
ubicación fue reabierta por el agente principal antes de entrar (70.4.8). Peldaño de todo lo de esta
sección: `DISCOVERED` (lectura), salvo que se diga otra cosa: no se ejecutó la app ni los servicios.

<!-- TANDA 1: frontends, clientes, simulado, postman (verificada) -->

### garantia-entregas-RESPALDO-010 — La web pública y el tour de la app prometen que el fondo cubre al que no paga
- Clase / Severidad / Confianza: RESPALDO (y DOC) / S2 / confirmado
- Ubicaciones: `apps/web/src/app/paginas/inicio/secciones/cierre.ts:71`, `apps/web/src/app/paginas/inicio/secciones/producto.ts:75`, `apps/web/src/app/paginas/inicio/secciones/transparencia.ts:65`, `apps/web/src/app/app.routes.ts:117` (JSON-LD de preguntas frecuentes, indexable), `apps/web/src/app/paginas/inicio/secciones/hero.ts:31`, `apps/movil/lib/pantallas/identidad/textos.dart:42-46`, `apps/movil/lib/pantallas/identidad/laminas_del_tour.dart:45-46`
- Evidencia: `cierre.ts:71` «…el fondo de garantía cubre la entrega para que quien tenía el turno cobre igual…»; `producto.ts:75` «Si alguien no aporta, el fondo cubre la entrega de quien tenía el turno»; `app.routes.ts:117` «Un fondo de garantía cubre esa falta para que el resto del grupo no se vea afectado»; `textos.dart:45` «Hay un fondo de garantía que cubre la cuota para que el turno se entregue igual».
- Cómo se comprobó: `grep -n 'cubre\|cubri'` sobre los archivos citados; líneas reabiertas.
- Impacto: promesa comercial pública de pozo completo que R-NUEVA-2 prohíbe; riesgo de reclamo del consumidor cuando el turno se entregue incompleto. La de `app.routes.ts` además queda indexada por buscadores.
- Causa de fondo: los textos de marketing se escribieron contra ADR-051 (regla 7, «la empresa aporta el faltante») y no tienen dueño que los re-valide cuando cambia la regla.
- Solución de fondo: reescribir los textos para que digan qué pasa de verdad (se entrega lo cobrado; la deuda queda a nombre de quien no pagó), una vez que Pablo decida A-2 y si existe o no un fondo comunitario (A-3). No inventar la regla en el texto.
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: apps/web (L6 para el shell), apps/movil tour (L4/L6); garantía como concepto.
- Estándares y normas: ISO/IEC 25010 adecuación funcional (corrección); reglas 91.5/91 (promesa de fondos), 95.
- Esfuerzo estimado: chico (textos) tras la decisión de negocio

### garantia-entregas-APROB-010 — Traspaso del cupo por votación del grupo sigue con ruta viva en la app
- Clase / Severidad / Confianza: APROB (y REESCRITURA con la subasta) / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart:14-22`, `apps/movil/lib/pantallas/pasanaku/textos_del_traspaso.dart:66-77`, `apps/movil/lib/pantallas/pasanaku/dominio/subasta_del_cupo.dart:9-10`
- Evidencia: ruta `'/pasanaku/grupos/:grupoId/cupos/:cupoId/traspaso'` → `PantallaTraspaso`; textos «El grupo aprobó el traspaso en una votación», «Esperando la votación», «Cuando se apruebe, volvés acá a traspasar». La subasta (entregas, CU-136) se declara su reemplazo «sin votación».
- Cómo se comprobó: lectura de las líneas citadas.
- Impacto: dos caminos para ceder el turno; uno exige aprobación del grupo (contra R-NUEVA-1). La persona puede quedar esperando una votación que el flujo nuevo no prevé.
- Causa de fondo: la subasta se agregó sin dar de baja la pantalla vieja.
- Solución de fondo: decidir cuál camino queda (Pablo) y retirar el otro con su ruta, textos y endpoint.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: grupos (votación/acuerdo, lote L3), entregas (subasta).
- Estándares y normas: ISO/IEC 25010 mantenibilidad; regla 00 §1.1, 60 (reescritura).
- Esfuerzo estimado: medio

### garantia-entregas-UX-A11Y-010 — «Se levanta pagando $monto» sin forma de pagar desde la pantalla
- Clase / Severidad / Confianza: UX-A11Y (y APROB) / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/textos.dart:146-148`, `apps/movil/lib/pantallas/pasanaku/pantalla_mi_estado.dart` (bloque de restricción), `servicios/garantia/.../web/CobranzaController.java` (`levantarRestriccion`, permiso `GRUPO_ADMINISTRAR`)
- Evidencia: `'Se levanta pagando $monto.'`; la pantalla no ofrece orden de cobro QR; en el backend levantar la restricción es un acto humano con motivo escrito.
- Cómo se comprobó: lectura de textos y de la pantalla; endpoint en `garantia.yaml` (`levantarRestriccion`).
- Impacto: la pantalla promete un efecto automático que hoy depende de una persona; aviso no accionable (95.2.3).
- Causa de fondo: el levantamiento no está atado al evento de pago confirmado.
- Solución de fondo: levantar la restricción por consumo del evento de aporte cobrado (webhook firmado) y ofrecer el botón de pago QR.
- Relación con el flujo nuevo: APROB / FLUJO-QR
- Conexiones y módulos afectados: aportes (orden QR), garantia.
- Estándares y normas: ISO/IEC 25010 usabilidad; reglas 95.2, R-NUEVA-1.
- Esfuerzo estimado: medio

### garantia-entregas-DINERO-010 — `RestriccionVigente.montoQueLaLevanta` es un importe sin moneda y la app inventa BOB
- Clase / Severidad / Confianza: DINERO (y CONTRATO) / S2 / confirmado
- Ubicaciones: `servicios/garantia/src/main/resources/openapi/garantia.yaml:689-696`, `apps/movil/lib/pantallas/pasanaku/pantalla_mi_estado.dart:91-93`
- Evidencia: `montoQueLaLevanta: { type: string, pattern: '^-?\d+\.\d{2}$' }` (sin `Dinero`); en la app: `// La restricción no trae moneda y la app opera solo en bolivianos` → `_importe(r.montoQueLaLevanta, 'BOB')`.
- Cómo se comprobó: lectura del contrato y de la pantalla.
- Impacto: viola 91.1.2 («prohibido un importe sin moneda»); el patrón admite negativos; si un grupo opera en USD (el contrato `Dinero` admite `[BOB, USD]`) la persona ve un monto en la moneda equivocada.
- Causa de fondo: el esquema se escribió con un string suelto en lugar de reusar `Dinero`; el front parcheó la presentación.
- Solución de fondo: `montoQueLaLevanta: $ref Dinero` (cambio de contrato versionado) y quitar el literal `'BOB'` del front.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: garantia → apps/movil; cliente Dart generado.
- Estándares y normas: ISO/IEC 25010 corrección funcional; ISO 4217; regla 91.1.2, 98.1.3.
- Esfuerzo estimado: chico

### garantia-entregas-DINERO-011 — Subasta y puja envían siempre moneda BOB aunque la subasta esté en otra moneda
- Clase / Severidad / Confianza: DINERO / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/dominio/subasta_del_cupo.dart:82`, `:125`; `apps/movil/lib/pantallas/pasanaku/formulario_de_subasta.dart:116`; `apps/movil/lib/pantallas/pasanaku/formulario_de_puja.dart:72`
- Evidencia: `Dinero(monto: …, moneda: DineroMonedaEnum.BOB)`; la puja muestra `subasta.montoMinimo.moneda` pero envía BOB.
- Cómo se comprobó: lectura (subagente) y reapertura de `subasta_del_cupo.dart`.
- Impacto: en un grupo USD la persona ve USD y se envía BOB; el servicio debería rechazar por moneda distinta (si no lo hace es S1 — no comprobado).
- Causa de fondo: moneda constante en el cliente en lugar de tomarla del agregado.
- Solución de fondo: tomar la moneda de `montoMinimo`/`montoAporte`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: entregas (subasta CU-136).
- Estándares y normas: ISO/IEC 25010 corrección; regla 91.1.2.
- Esfuerzo estimado: chico

### garantia-entregas-UX-A11Y-011 — Estados de puja: «ya volvió a tu billetera» mostrado para SUPERADA; GANADORA/RECHAZADA/PENDIENTE sin pintar
- Clase / Severidad / Confianza: UX-A11Y (y DINERO) / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/pantalla_subasta.dart:64-68`, `apps/movil/lib/pantallas/pasanaku/textos_de_la_subasta.dart:65`, enum en `servicios/entregas/src/main/resources/openapi/entregas.yaml:691`
- Evidencia: SUPERADA y LIBERADA comparten «Te superaron: tu saldo ya volvió a tu billetera».
- Cómo se comprobó: lectura (subagente), ubicación reabierta.
- Impacto: afirma un movimiento de dinero no confirmado; estados del contrato sin representación.
- Causa de fondo: mapeo de estados incompleto en la vista.
- Solución de fondo: un texto por estado; «ya volvió» solo con LIBERADA.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: entregas (subasta).
- Estándares y normas: ISO/IEC 25010 corrección y usabilidad; regla 95.2, 98.4.1.
- Esfuerzo estimado: chico

### garantia-entregas-UX-A11Y-012 — Errores del servidor de monto en subasta/puja no se asocian al campo
- Clase / Severidad / Confianza: UX-A11Y / S3 / probable
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/formulario_de_puja.dart:88-95`, `apps/movil/lib/pantallas/pasanaku/formulario_de_subasta.dart:180-189`
- Evidencia: rechazos AP-CU136-03/04/07 (del monto) se pintan como `Alerta` sobre el botón; `CampoMonto` tiene parámetro `error` sin usar.
- Cómo se comprobó: lectura del subagente; no reabierto línea a línea por el agente principal (por eso «probable»).
- Impacto: incumple 95.3.2; lectores de pantalla no vinculan el error al campo. Lo escrito sí se preserva y el doble envío sí está bloqueado.
- Causa de fondo: mapeo de error genérico.
- Solución de fondo: mapear códigos de monto a `CampoMonto.error`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: entregas.
- Estándares y normas: WCAG 2.2 (3.3.1); regla 95.3.
- Esfuerzo estimado: chico

### garantia-entregas-CONTRATO-010 — La app lee solo la primera página de `listarSubastasDeCupo`
- Clase / Severidad / Confianza: CONTRATO / S4 / probable
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/dominio/subasta_del_cupo.dart:13-24`; `entregas.yaml:515` («Paginado, con orden estable»)
- Evidencia: el provider no usa el cursor.
- Cómo se comprobó: lectura del subagente.
- Impacto: subastas fuera de la primera página invisibles.
- Causa de fondo / Solución de fondo: falta paginación en el provider / consumir el cursor.
- Relación con el flujo nuevo: ninguna · Conexiones: entregas · Estándares: ISO/IEC 25010 completitud; 96.6 · Esfuerzo: chico

### garantia-entregas-PRUEBAS-010 — Mocks de `packages/simulado` sin escenario adverso real y con códigos de error ajenos
- Clase / Severidad / Confianza: PRUEBAS (y CONTRATO) / S3 / probable
- Ubicaciones: `packages/simulado/ejemplos/garantia/*.json`, `packages/simulado/ejemplos/entregas/*.json` (41 archivos); ejemplos: `listarSubastasDeCupo` usa AP-CU64-01; `listarDesembolsos` usa AP-CU28-01 (el contrato declara AP-CU28-07); `consultarRestriccion.json` sin `rechazo` y `nivel: "nivel"`.
- Evidencia: comparación por script del subagente (claves de primer nivel y códigos de estado): escenario `adverso` idéntico a `ok` en las 41; 34 mocks emiten 202/503 no declarados.
- Cómo se comprobó: script del subagente; no re-ejecutado por el agente principal.
- Impacto: las pantallas desarrolladas contra el simulador nunca ejercitan error real (regla 65 exige tres niveles).
- Causa de fondo: mocks generados por plantilla.
- Solución de fondo: generar los mocks desde el OpenAPI con los códigos de error de cada operación.
- Relación con el flujo nuevo: ninguna · Conexiones: packages/simulado (L6) · Estándares: regla 65, 80 · Esfuerzo: medio

### garantia-entregas-DOC-010 — Backoffice y catálogos de diseño todavía hablan de cobertura del fondo y «fondo completo»
- Clase / Severidad / Confianza: DOC (y RESPALDO) / S3 / confirmado por el subagente, muestreado
- Ubicaciones: `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-tablero.ts:25`, `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-operacion.ts:36-38,73`, `apps/backoffice/src/app/rutas/tablero/dominio/catalogo-de-tableros.ts:31-33`, `packages/ui/src/catalogo/muestras.ts:30`, `packages/diseno_flutter/lib/catalogo/muestras.dart:606`
- Evidencia: «Impagos cubiertos», «Recuperación de lo cubierto», «Cobertura del fondo»; «el fondo cubre la diferencia».
- Impacto: indicadores y ayudas que presuponen R-NUEVA-2 incumplida.
- Solución de fondo: retirar/renombrar tras decidir A-3.
- Relación con el flujo nuevo: RESPALDO · Conexiones: backoffice (L6), transparencia · Estándares: regla 00 §6 · Esfuerzo: chico

### garantia-entregas-UX-A11Y-013 — Valores literales de tipografía en la bandeja de desembolsos
- Clase / Severidad / Confianza: UX-A11Y / S4 / probable
- Ubicaciones: `apps/backoffice/src/app/rutas/operacion/desembolsos/bandeja-de-desembolsos.ts:67`, `apps/backoffice/src/app/rutas/operacion/comun/importe-sin-moneda.ts:16`
- Evidencia: `max-width: 10ch`, `font-size: .92em`, `font-size: .875rem`.
- Impacto/Solución: incumple 95.1.5; usar tokens de `packages/tokens`. · Relación: ninguna · Esfuerzo: chico

### garantia-entregas-SEG-010 — `usuarioId` de la restricción tomado de la query de la ruta móvil
- Clase / Severidad / Confianza: SEG / S4 / probable
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart:64-72`; control en servidor `servicios/garantia/.../web/CobranzaController.java:47-50`
- Evidencia: `/pasanaku/mi-estado?usuario=…&participante=…`; el servidor exige propio usuario o `GRUPO_ADMINISTRAR`, así que no hay BOLA.
- Impacto: bajo (servidor protege); identificadores en URL innecesarios.
- Solución de fondo: resolver el usuario por sesión. · Estándares: OWASP API1 (mitigado), 90.2.2 · Esfuerzo: chico

<!-- TANDA 2: servicios Java garantia y entregas (S1/S2 reabiertos por el agente principal) -->

### garantia-entregas-RESPALDO-001 — Cada fondeo con faltante dispara automáticamente la cobertura con dinero de la empresa
- Clase / Severidad / Confianza: RESPALDO (y DINERO) / S1 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22EntregarPozoCompleto.java:67-85,100`; `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/RespaldoPorHttp.java:56`; `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java:128-129`; `servicios/garantia/src/main/java/bo/aportaya/garantia/web/GarantiaController.java:172-175`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirConRespaldo.java:215`; `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/AsientoPropuesto.java:47-48`; consumidor `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/consumo/ConsumidorDeAsientosKafka.java` (tópico `aportaya.garantia.cobertura_empresarial_aplicada`)
- Evidencia: `var cobertura = respaldo.cubrir(e.grupoId(), e.periodoId(), e.turnoId(), faltante)` … `cubiertoEmpresa = cobertura.cubierto();` → `RespaldoPorHttp` hace POST a `"/garantia/respaldo/coberturas"` → `AsientoPropuesto.aplicacion(...)` = `par("COBERTURA", coberturaId, Cuenta.POZO_GRUPO, Cuenta.RESERVA_RESPALDO, monto)`; nucleo-financiero escucha ese tópico y registra el asiento.
- Cómo se comprobó: lectura de los cinco archivos; `grep -rhoE 'aportaya\.garantia\.[a-z_]+'` muestra que el tópico tiene consumidor en nucleo-financiero.
- Impacto: la empresa paga al beneficiario lo que no pagaron otros participantes. Es exactamente lo que R-NUEVA-2 prohíbe, en el camino vivo `POST /entregas/pozo-completo`.
- Causa de fondo: ADR-051 (D-02/D-03/D-15, «pozo completo con respaldo empresarial») implementado en entregas, garantía y nucleo-financiero; nadie lo retiró al cambiar la regla.
- Solución de fondo: ADR nueva que deje sin efecto las decisiones de respaldo de ADR-051; retirar el puerto `RespaldoEmpresarial` y `RespaldoPorHttp`, deprecar `/garantia/respaldo/coberturas` con versión y fecha (98.1.3) y conservar el histórico append-only. Qué se hace con el faltante depende de A-2 (no se decide acá).
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: entregas → garantía (HTTP) → nucleo-financiero (Kafka, mayor); aportes (recaudo); lote L1 por el consumidor del asiento.
- Estándares y normas: ISO/IEC 25010 adecuación funcional (corrección); reglas 91, 98.1.3; R-NUEVA-2.
- Esfuerzo estimado: grande

### garantia-entregas-RESPALDO-002 — Reserva, ampliación, liberación y reversa de respaldo empresarial siguen expuestas y mueven caja de la empresa
- Clase / Severidad / Confianza: RESPALDO / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/GarantiaController.java:153-180`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23ReservarRespaldo.java:143,217,268`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23ReversarCobertura.java:102`; `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/AsientoPropuesto.java:42-63`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/ContingenciaDeCaja.java`
- Evidencia: `@Permiso("ADMIN_PLATAFORMA") … reservarRespaldo`, `ampliarRespaldo`, `liberarRespaldo`, `reversarCoberturaRespaldo`; `reserva(...) = par("AJUSTE", reservaId, Cuenta.RESERVA_RESPALDO, Cuenta.CAJA_EMPRESA, monto)`. Los cuatro tópicos `respaldo_*` los consume nucleo-financiero.
- Cómo se comprobó: lectura del controlador y de `AsientoPropuesto`; búsqueda de tópicos.
- Impacto: el mecanismo que alimenta RESPALDO-001 sigue operable a mano y escribe en el mayor. Además la contingencia escala a Tesorería para «reponer el respaldo».
- Causa de fondo: la misma que RESPALDO-001.
- Solución de fondo: retirar junto con RESPALDO-001; contra-asiento de liberación para reservas vigentes; tablas `*_respaldo` a solo lectura histórica (A-3).
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: garantía → nucleo-financiero (6 tópicos).
- Estándares y normas: ISO/IEC 25010 corrección; regla 91; R-NUEVA-2.
- Esfuerzo estimado: grande

### garantia-entregas-SEG-001 — `recuperarRespaldo` traslada dinero del pozo a la caja de la empresa con un pago que afirma el cliente
- Clase / Severidad / Confianza: SEG (y DINERO, RESPALDO) / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/RespaldoWeb.java:152-157`; `servicios/garantia/src/main/java/bo/aportaya/garantia/web/GarantiaController.java:185-188`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23RecuperarRespaldo.java:68-142`; `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/AsientoPropuesto.java:57-58`
- Evidencia: `new CU23RecuperarRespaldo.EntradaRecuperacion(cuerpo.getPagoId(), cuerpo.getObligacionId(), MapeoDeGarantia.dinero(cuerpo.getPago()))`; el caso de uso no consulta aportes ni ningún webhook: usa `e.pago()` y emite `recuperacion(...) = par("COBERTURA", id, Cuenta.CAJA_EMPRESA, Cuenta.POZO_GRUPO, monto)`.
- Cómo se comprobó: lectura completa de `CU23RecuperarRespaldo.recuperar`; el tope es lo cubierto en la línea, no un pago verificado.
- Impacto: un `ADMIN_PLATAFORMA` puede declarar un `pagoId` y un importe inventados y mover saldo del pozo del grupo a la caja de la empresa en el mayor. Viola 91.3.1 (no es pago hasta que la pasarela confirme) y R-NUEVA-1.
- Causa de fondo: la recuperación se diseñó como acto administrativo, no como consumo del evento de pago confirmado.
- Solución de fondo: eliminar el endpoint junto con el respaldo. Si sobrevive alguna imputación, que sea solo por consumo del evento de aporte cobrado, con el importe afirmado por aportes.
- Relación con el flujo nuevo: RESPALDO / FLUJO-QR
- Conexiones y módulos afectados: garantía → nucleo-financiero; aportes (debería ser la fuente).
- Estándares y normas: ISO/IEC 25010 seguridad (integridad); CWE-345; OWASP API Security Top 10 (API6/2023 flujos sensibles); reglas 91.3.1, 90.1.7.
- Esfuerzo estimado: chico (retirar) / medio (rehacer por evento)

### garantia-entregas-SEG-002 — Los endpoints `GRUPO_ADMINISTRAR` de garantía no comprueban que el organizador sea de ese grupo (BOLA)
- Clase / Severidad / Confianza: SEG / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/GarantiaController.java:63-137` (devolución, reemplazo, disolución); `servicios/garantia/src/main/java/bo/aportaya/garantia/web/IncumplimientosController.java` (declarar, cubrir `:143-153`, aval, restricción, reemplazo, resolver descargo); `servicios/garantia/src/main/java/bo/aportaya/garantia/web/CobranzaController.java` (`levantarRestriccion`); guardia `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/GuardiaDePermiso.java` (`codigo.equals(contexto.rol()) || sesion.permisos().contains(codigo)`); RLS `sql/40_reglas/restricciones.sql:1437-1462`
- Evidencia: la guardia solo mira el permiso; `grep -rniE 'organizador|esMiembro|pertenece'` en `servicios/garantia/src/main` no encuentra ninguna comprobación de relación con el grupo (la única comprobación de propiedad es `CobranzaController.java:49` y `CU25DeclararIncumplimiento.java:162`). Las tablas sin `usuario_id` (fondo_garantia, disolucion_anticipada, reemplazo_participante, cobertura_*) no reciben política RLS en `fn_seg_aplicar_rls`.
- Cómo se comprobó: lectura de la guardia, del recorrido RLS y búsqueda de chequeos de relación.
- Impacto: cualquier persona con rol ORGANIZADOR (la semilla le da `GRUPO_ADMINISTRAR`, `sql/60_semillas/10-roles-y-permisos.sql:152`) puede disolver, devolver el fondo, reemplazar participantes, ejecutar avales o cubrir incumplimientos de un grupo ajeno.
- Causa de fondo: autorización solo por rol; falta la dimensión «propiedad/relación» (90.1.2).
- Solución de fondo: política de autorización por relación (organizador del grupo resuelto contra grupos o por claim firmado con alcance), aplicada en la escritura, y matriz negativa de pruebas.
- Relación con el flujo nuevo: APROB (son pasos humanos)
- Conexiones y módulos afectados: grupos (fuente de la relación), plataforma/comun-web.
- Estándares y normas: ISO/IEC 25010 seguridad; OWASP API1:2023 (BOLA); CWE-639; reglas 90.1.2-3, 96.5.2.
- Esfuerzo estimado: medio

### garantia-entregas-SEG-003 — El organizador cubre un incumplimiento con días de mora y aprobador que manda en el cuerpo
- Clase / Severidad / Confianza: SEG (y APROB, DEBIDO-PROCESO, DINERO) / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/IncumplimientosController.java:143-153`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirIncumplimiento.java:71-127`
- Evidencia: `MapeoDeGarantia.dinero(cuerpo.getMontoSolicitado()), cuerpo.getDiasMora(), cuerpo.getAprobadaPor()`; `boolean quedaPendiente = calculo.exigeAprobacionManual() && entrada.aprobadaPor() == null;` Ninguna línea de 71-125 comprueba el estado del expediente (firme) ni el vencimiento del plazo de descargo.
- Cómo se comprobó: lectura del controlador y del caso de uso.
- Impacto: cualquier UUID en `aprobadaPor` salta el umbral de aprobación manual; `diasMora` lo afirma quien pide la cobertura; se cubre con el fondo de los participantes un expediente recién notificado. Viola 90.1.7, 91.4.1 y RN-18 (el organizador dispone del fondo).
- Causa de fondo: datos que deciden dinero aceptados del cliente.
- Solución de fondo: leer mora y deuda de aportes; exigir expediente firme en la escritura; decidir con Pablo si la cobertura con fondo mutual existe con R-NUEVA-2 (A-3).
- Relación con el flujo nuevo: APROB / RESPALDO (fondo mutual, A-3)
- Conexiones y módulos afectados: aportes (mora), grupos.
- Estándares y normas: ISO/IEC 25010 seguridad (integridad); CWE-807; reglas 90.1.7, 91.4, 91.5.
- Esfuerzo estimado: medio

### garantia-entregas-SEG-004 — La devolución del fondo se decide con `grupoCerrado` y `deudasVivas` del cuerpo
- Clase / Severidad / Confianza: SEG (y DINERO) / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/GarantiaController.java:63-69`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU29DevolverFondo.java:60-70,150`
- Evidencia: `new CU29DevolverFondo.EntradaDevolucion(grupoId, Boolean.TRUE.equals(cuerpo.getGrupoCerrado()), cuerpo.getDeudasVivas())`; `if (!entrada.grupoCerrado())` / `if (entrada.deudasVivas() > 0)`.
- Cómo se comprobó: lectura.
- Impacto: combinado con SEG-002, un organizador reparte el fondo de garantía de un grupo en curso mandando `grupoCerrado=true, deudasVivas=0`.
- Causa de fondo: precondiciones de negocio afirmadas por el cliente en lugar de leídas de su dueño.
- Solución de fondo: preguntar a grupos el estado del grupo y contar deudas vivas en el propio esquema, dentro de la transacción.
- Relación con el flujo nuevo: APROB (paso manual del organizador)
- Conexiones y módulos afectados: grupos.
- Estándares y normas: ISO/IEC 25010 seguridad; CWE-807; regla 90.1.7, 96.3.2.
- Esfuerzo estimado: chico

### garantia-entregas-DEBIDO-PROCESO-001 — El aval se ejecuta y la deuda se subroga sin expediente firme y antes de que el avalista pague
- Clase / Severidad / Confianza: DEBIDO-PROCESO (y DINERO) / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU26EjecutarAval.java:98-106`; tópico `garantia.aval_ejecutado` sin consumidor
- Evidencia: `gestion.subrogar(dsl, coberturaId, deuda.id(), "FONDO_GARANTIA", "AVALISTA", aEjecutar, ahora); gestion.marcarDeudaSubrogada(dsl, deuda.id(), deuda.version());` en el mismo paso que notifica (`ahora.plus(plazoDeRespuesta)`), sin consultar estado del expediente; el retorno de `marcarDeudaSubrogada` se ignora. `grep` de `aval_ejecutado` fuera de garantía: sin resultados.
- Cómo se comprobó: lectura; búsqueda de consumidores.
- Impacto: el patrimonio del avalista queda comprometido sin firmeza ni plazo de descargo cumplido (91.4.1/91.4.4), y la deuda cambia de acreedor sin que nadie haya pagado; nadie cobra al avalista.
- Causa de fondo: subrogación modelada como efecto de la notificación, no del pago.
- Solución de fondo: precondición de estado firme en la escritura; subrogar solo al consumir el pago confirmado del avalista (webhook firmado).
- Relación con el flujo nuevo: FLUJO-QR (el pago del avalista) — el aval no es R-NUEVA-2 (A-3)
- Conexiones y módulos afectados: aportes/nucleo-financiero (pago del avalista), reputación.
- Estándares y normas: ISO/IEC 25010 corrección; reglas 91.4, 98.3.
- Esfuerzo estimado: medio

### garantia-entregas-SEG-005 — La acreditación del desembolso la tipea una persona: `exitoso=true` marca ACREDITADA y manda el asiento al mayor
- Clase / Severidad / Confianza: SEG (y DINERO, APROB) / S1 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/web/DesembolsosController.java:105-106`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:150-196`; consumidor `ConsumidorDeAsientosKafka.java` (tópico `aportaya.entregas.desembolso_acreditado`)
- Evidencia: `@Permiso("ENTREGA_EJECUTAR") … anotarRespuestaDesembolso`; `if (entrada.exitoso()) { … desembolsos.acreditar(dsl, orden.id(), entrada.referenciaProveedor(), ahora); … "asiento", new AsientoDeEntrega(orden.entregaId(), orden.monto()).comoCarga()`. No hay firma del proveedor ni adaptador al proveedor en el servicio.
- Cómo se comprobó: lectura; búsqueda del consumidor del tópico.
- Impacto: una persona de Tesorería puede declarar pagada una transferencia que no salió; el mayor registra la salida del pozo. El comentario del consumidor dice «El desembolso que el proveedor CONFIRMO», que no es lo que pasa.
- Causa de fondo: falta la integración con el proveedor de desembolso; se reemplazó por un formulario humano.
- Solución de fondo: webhook del proveedor con firma verificada e idempotente por `referenciaProveedor`; retirar el endpoint humano o dejarlo solo para conciliación con doble control y sin asiento.
- Relación con el flujo nuevo: APROB / FLUJO-QR (simétrico del lado de salida)
- Conexiones y módulos afectados: nucleo-financiero (mayor).
- Estándares y normas: ISO/IEC 25010 seguridad (integridad, no repudio); CWE-345; reglas 91.3, 91.1.4.
- Esfuerzo estimado: grande

### garantia-entregas-DINERO-001 — `ejecutarEntrega` marca ENTREGADA con un monto libre y puede coexistir con una orden de desembolso en vuelo
- Clase / Severidad / Confianza: DINERO (y APROB) / S1 / confirmado (lectura; no ejecutado)
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java:113-114`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22LiquidarEntrega.java:161-190`; `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/EntregaRepositorio.java:129-143`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:123,173-178`
- Evidencia: `marcarEntregada(...)` hace `.set("monto_efectivamente_entregado", entregado.monto())` con `estado IN ('AUTORIZADA','EN_PROCESO_DESEMBOLSO')`, sin comparar con el neto; `emitir` solo cambia la orden (`cambiarEstado(dsl, ordenId, List.of("CREADA"), "ENVIADA_A_PROVEEDOR")`), la entrega sigue AUTORIZADA; al acreditar, `entregas.cambiarEstado(...)` se llama sin mirar su resultado y el asiento se emite igual.
- Cómo se comprobó: lectura de las cuatro ubicaciones.
- Impacto: una entrega puede quedar ENTREGADA a mano (incluso por un monto distinto, o con método `EFECTIVO_ORGANIZADOR`) y, además, acreditarse la orden y asentarse en el mayor: doble entrega o entrega sin dinero real.
- Causa de fondo: dos caminos de cierre de la entrega (legacy manual y CU-28) sin máquina de estados única.
- Solución de fondo: eliminar `ejecutar`; `emitir` lleva la entrega a EN_PROCESO_DESEMBOLSO con precondición; fallar si `cambiarEstado` devuelve false.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: nucleo-financiero.
- Estándares y normas: ISO/IEC 25010 corrección e integridad; reglas 91.1, 96.3.1-2.
- Esfuerzo estimado: medio

### garantia-entregas-SEG-006 — El desembolso no comprueba que la cuenta destino sea del beneficiario
- Clase / Severidad / Confianza: SEG (y DINERO) / S1 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:91-118`
- Evidencia: se valida `cuenta.estaVerificada()` y `bloqueadaHasta`, y se emite `desembolsos.emitir(dsl, entrada.entregaId(), entrada.proveedorId(), entrada.cuentaDestinoId(), entrega.neto(), …)`; no hay comparación entre el titular de la cuenta y el beneficiario de la entrega (ni con el titular de una cesión), ni de moneda.
- Cómo se comprobó: lectura del bloque completo.
- Impacto: Tesorería puede dirigir el pozo de un turno a cualquier cuenta verificada de cualquier usuario. La RLS no ayuda porque Tesorería es rol privilegiado.
- Causa de fondo: la entrega guarda `beneficiario_participante_id` y la cuenta `usuario_id`, sin vínculo verificable en este servicio.
- Solución de fondo: resolver el usuario del beneficiario (o del cesionario) y exigir coincidencia en la escritura y por trigger.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: grupos (participante→usuario), identidad.
- Estándares y normas: ISO/IEC 25010 seguridad; OWASP API1:2023; CWE-639; reglas 91.3.3, 90.1.3.
- Esfuerzo estimado: medio

### garantia-entregas-SEG-007 — El cliente puede enviar su propio `numeroCifrado`, distinto del número que se enmascara y deduplica
- Clase / Severidad / Confianza: SEG / S1 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU18RegistrarCuentaDestino.java:72,101-108`
- Evidencia: `if (entrada.numeroCifrado() != null && … (entrada.numeroCifrado().startsWith("cifrado:") || entrada.numeroCifrado().startsWith(CifradorSimetrico.PREFIJO))) { numeroCifradoReal = entrada.numeroCifrado(); } else { numeroCifradoReal = CifradorSimetrico.cifrar(entrada.numeroEnClaro(), pimienta); }` — y el hash/máscara salen de `numeroEnClaro` (`:72`).
- Cómo se comprobó: lectura.
- Impacto: la cuenta que se muestra y se verifica no es necesariamente la que se paga (el cifrado guardado puede ser de otra cuenta). Además la misma `pimienta` se usa como clave de cifrado y como pepper de hash (reutilización de clave).
- Causa de fondo: atajo para pruebas («cifrado:») que quedó en el código de producción.
- Solución de fondo: cifrar siempre en servidor con una clave dedicada y rotada; rechazar `numeroCifrado` del cliente.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: plataforma (`CifradorSimetrico`).
- Estándares y normas: ISO/IEC 25010 seguridad; CWE-807, CWE-323; reglas 90.2.9, 91.3.3.
- Esfuerzo estimado: chico

### garantia-entregas-SEG-008 — «Cuenta verificada» es autodeclarada por su dueño con un método de texto libre
- Clase / Severidad / Confianza: SEG / S2 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU18RegistrarCuentaDestino.java:147-176`
- Evidencia: `verificar(UUID cuentaId, String metodo, ContextoSesion ctx)` solo exige `cuenta.usuarioId().equals(ctx.usuarioId())` y llama `cuentas.verificar(dsl, cuentaId, metodo, ahora, …)`; no hay contraste con el banco.
- Cómo se comprobó: lectura.
- Impacto: la condición 91.3.3 («desembolso contra cuenta verificada») queda vacía; el enfriamiento sí corre.
- Causa de fondo: la verificación real (microdepósito/banco) no está implementada; se dejó un paso declarativo.
- Solución de fondo: verificación por proveedor (microdepósito o consulta de titularidad) con resultado firmado.
- Relación con el flujo nuevo: ninguna · Conexiones: identidad (titularidad) · Estándares: ISO/IEC 25010 seguridad; regla 91.3.3 · Esfuerzo: grande

### garantia-entregas-DINERO-002 — `saldoRetenido` lo afirma quien pide el desembolso
- Clase / Severidad / Confianza: DINERO / S2 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:106-111,263`
- Evidencia: `if (!entrada.saldoRetenido()) { throw … "El saldo de la entrega no esta retenido" }` — el booleano viene de la entrada; no hay retención consultada en nucleo-financiero.
- Impacto: el control AP-CU28-04 no controla nada; se puede ordenar pago sin saldo retenido.
- Solución de fondo: crear o consultar la retención en nucleo-financiero dentro del caso de uso.
- Relación con el flujo nuevo: ninguna · Conexiones: nucleo-financiero · Estándares: regla 90.1.7, 91 · Esfuerzo: medio

### garantia-entregas-DINERO-003 — `POST /entregas` (liquidación legacy) sigue vivo, confía en bruto/recaudado del cliente, ignora la clave de idempotencia y admite EFECTIVO_ORGANIZADOR
- Clase / Severidad / Confianza: DINERO (y REESCRITURA, HUECO) / S1 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java:64-80`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22LiquidarEntrega.java:51-108`; `servicios/entregas/src/main/resources/openapi/entregas.yaml:897`; `sql/10_tablas/04_entregas_fondo/entrega_fondo.sql:31`
- Evidencia: `liquidarEntrega(UUID idempotencyKey, EntradaLiquidacion cuerpo)` → `cu22.liquidar(... MapeoDeEntregas.dinero(cuerpo.getBruto()), MapeoDeEntregas.dinero(cuerpo.getRecaudado()), … cuerpo.getMetodoDesembolso().getValue() …)` sin pasar `idempotencyKey`; enum `[BILLETERA_MOVIL, COMPENSACION_INTERNA, EFECTIVO_ORGANIZADOR, …]` y el CHECK de la base lo admite, mientras `entregas.yaml:787` dice «sin EFECTIVO_ORGANIZADOR a proposito, el organizador no es caja (RN-18)».
- Cómo se comprobó: lectura del controlador, contrato y DDL.
- Impacto: Tesorería puede crear una entrega con un pozo afirmado por ella, sin pasar por aportes; método en efectivo vía organizador contradice RN-18/91.5. Clon divergente del fondeo (`/entregas/pozo-completo`): dos reglas para lo mismo.
- Causa de fondo: el fondeo nuevo se agregó sin retirar la liquidación vieja (REESCRITURA).
- Solución de fondo: retirar `POST /entregas` o hacerlo delegar en el fondeo; sacar `EFECTIVO_ORGANIZADOR` del contrato y del modelo (`docs/entidades`, no del SQL generado).
- Relación con el flujo nuevo: ninguna directa (riesgo de dinero)
- Conexiones y módulos afectados: aportes (debería afirmar el recaudado).
- Estándares y normas: ISO/IEC 25010 corrección y mantenibilidad; reglas 91.1.4, 91.5, 90.1.7.
- Esfuerzo estimado: medio

### garantia-entregas-APROB-001 — La entrega del turno avanza por cinco actos humanos: fondear, autorizar (cuatro ojos), emitir, ejecutar y anotar respuesta
- Clase / Severidad / Confianza: APROB / S2 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java:99-100,113-114,128-129`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22LiquidarEntrega.java:119-149,169`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:84-89`; `servicios/entregas/src/main/java/bo/aportaya/entregas/web/DesembolsosController.java:78-79,105-106`; `sql/40_reglas/restricciones.sql` (`ck_entrega_segregacion`, R-SEG-04); `docs/catalogo-de-flujos/entrega-del-turno.yaml:41-55`
- Evidencia: `@Permiso("ENTREGA_AUTORIZAR") autorizarEntrega`; `if (!"AUTORIZADA".equals(entrega.estado())) throw … "todavia no se desembolsa."`; comentario «La autorizacion es la firma de que alguien reviso que corresponde pagarle a esa persona».
- Cómo se comprobó: lectura.
- Impacto: aunque todos paguen por QR, la entrega no avanza sin RESPONSABLE_RIESGOS y TESORERIA. Bloquea R-NUEVA-1.
- Causa de fondo: segregación de funciones R-SEG-04 aplicada al pago del pozo.
- Solución de fondo: transición automática fondeado → autorizado → orden emitida por eventos; decidir con Cumplimiento si R-SEG-04 es obligación legal (A-1) antes de quitarla.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: aportes (evento de cobro), nucleo-financiero.
- Estándares y normas: ISO/IEC 25010 adecuación funcional; R-NUEVA-1; norma a identificar por Cumplimiento para R-SEG-04.
- Esfuerzo estimado: grande

### garantia-entregas-APROB-002 — Aprobación manual por umbral en la cobertura del fondo mutual, con estado SOLICITADA sin salida
- Clase / Severidad / Confianza: APROB (y HUECO) / S2 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirIncumplimiento.java:126-140`; `sql/10_tablas/08_garantia_incumplimiento/politica_cobertura.sql:15`; `sql/60_semillas/02-politicas.sql:27-28`
- Evidencia: `String estado = quedaPendiente ? "SOLICITADA" : "APLICADA";` y evento `garantia.cobertura_requiere_aprobacion`; `grep` del tópico fuera de garantía: sin consumidor; ningún endpoint aplica una cobertura SOLICITADA.
- Impacto: un estado muerto y un paso humano (contra R-NUEVA-1). Combinado con SEG-003 el umbral es eludible.
- Solución de fondo: decidir A-3 (¿existe la cobertura del fondo mutual?) y retirar el paso manual.
- Relación con el flujo nuevo: APROB · Conexiones: ninguna · Estándares: ISO/IEC 25010 completitud; R-NUEVA-1 · Esfuerzo: chico

### garantia-entregas-HUECO-001 — Cobertura del fondo, ejecución de aval y devolución del fondo no mueven dinero: sus eventos no tienen consumidor
- Clase / Severidad / Confianza: HUECO (y DINERO, TRAZA) / S2 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirIncumplimiento.java` (evento `incumplimiento_cubierto`), `CU26EjecutarAval.java` (`aval_ejecutado`), `CU29DevolverFondo.java` (`fondo_devuelto`); `FondoRepositorio.java` (`moverSaldo`)
- Evidencia: `grep -rnE 'garantia\.(incumplimiento_cubierto|aval_ejecutado|fondo_devuelto)'` fuera de `servicios/garantia`: sin resultados; el `@KafkaListener` de nucleo-financiero solo escucha los seis tópicos de respaldo y `desembolso_acreditado`.
- Impacto: el saldo cacheado de `fondo_garantia` baja pero no hay asiento ni movimiento de billetera: el fondo se desincroniza del mayor y quedan deudas por coberturas que nunca se pagaron.
- Causa de fondo: el saldo del fondo vive en garantía (caché) en vez de en el mayor (91.1.5).
- Solución de fondo: cada movimiento del fondo propone su asiento y lo registra nucleo-financiero; los saldos se leen del mayor.
- Relación con el flujo nuevo: RESPALDO (fondo mutual, A-3)
- Conexiones y módulos afectados: nucleo-financiero (lote L1).
- Estándares y normas: ISO/IEC 25010 corrección; reglas 91.1.5, 98.3.
- Esfuerzo estimado: grande

### garantia-entregas-DATOS-001 — RLS de `registro_incumplimiento`/`deuda_participante` deja al imputado y al organizador sin ver el expediente; las pruebas corren con el rol dueño
- Clase / Severidad / Confianza: DEFECTO (y SEG, DEBIDO-PROCESO) / S2 / probable
- Ubicaciones: `sql/40_reglas/restricciones.sql:1437-1438` (`cond := 'fn_seg_rol_privilegiado() OR fn_seg_es_sistema()'` para tablas con `usuario_id` fuera de `visibles_por_titular`); `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU25DeclararIncumplimiento.java:157-163`; `servicios/garantia/src/test/java/bo/aportaya/garantia/BaseDeGarantia.java:73-75`
- Evidencia: rama «denegar por omisión» del recorrido RLS; el descargo busca el expediente con el contexto del imputado.
- Cómo se comprobó: lectura; falta la comprobación `SET ROLE svc_garantia` + contexto PARTICIPANTE contra una base con `sql/aplicar.sql` (no ejecutada: sin base en la nube).
- Impacto: si la hipótesis se confirma, el participante no puede presentar descargo («Ese expediente no existe») y `consultarRestriccion` devuelve `vigente=false` (falla abierta).
- Causa de fondo: lista `visibles_por_titular` no incluye las tablas de garantía; pruebas de integración con rol que no pasa por RLS.
- Solución de fondo: decidir visibilidad por titular en `docs/Restricciones.md`; pruebas de integración con el rol real de la aplicación.
- Relación con el flujo nuevo: ninguna · Conexiones: plataforma/comun-datos · Estándares: ISO/IEC 25010 corrección; regla 80.2.4, 91.4 · Esfuerzo: medio

### garantia-entregas-DINERO-004 — Importes sin moneda o con moneda fija BOB en garantía
- Clase / Severidad / Confianza: DINERO (y DATOS) / S2 / confirmado (esquema) · probable (código)
- Ubicaciones: tablas sin columna `moneda` junto al importe: `movimiento_fondo`, `abono_recuperacion`, `cobertura_incumplimiento`, `politica_cobertura` (topes absolutos), `acuerdo_quita`, `castigo_deuda`, `devolucion_fondo`, `disolucion_anticipada`, `ejecucion_aval`, `aval_participante`, `gestion_cobranza`, `promesa_pago`, `accion_cobranza`, `sancion`, `lista_restriccion_interna`, `reemplazo_participante`, `liquidacion_participante`, `historial_*`, `registro_incumplimiento` (todas en `sql/10_tablas/08_garantia_incumplimiento/`) y `entregas.deduccion_entrega`; código: `ExpedienteRepositorio.java`, `GestionRepositorio.java`, `IndicadoresDeGarantia.java` (moneda fija BOB, según el subagente); contrato `garantia.yaml:689-696` (ver DINERO-010)
- Evidencia: matriz por tabla de la sección 6-bis; `grep -L moneda` sobre los archivos de la carpeta.
- Impacto: viola 91.1.2; un grupo en USD se registra y relee como BOB; las sumas mezclan monedas.
- Causa de fondo: modelo de datos anterior a la regla 91.
- Solución de fondo: agregar moneda en el modelo (`docs/entidades/*.puml` → DDL generado), expandir y contraer.
- Relación con el flujo nuevo: ninguna · Conexiones: nucleo-financiero · Estándares: ISO 4217; regla 91.1.2, 97.1 · Esfuerzo: grande

### garantia-entregas-DINERO-005 — Saldos del fondo cacheados y sumas ad-hoc en lugar del mayor
- Clase / Severidad / Confianza: DINERO / S2 / probable
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/FondoRepositorio.java` (`moverSaldo`, `consumido`), `GestionRepositorio.java` (`deudaViva`), `IndicadoresDeGarantiaRepositorio.java`; `sql/10_tablas/08_garantia_incumplimiento/fondo_garantia.sql`; sin control de cuadre en `sql/50_verificacion/verificaciones.sql` (sí lo hay para la reserva, `:118-134`)
- Evidencia: UPDATE de `saldo_disponible`/`saldo_comprometido`; SUM sobre tablas operativas para indicadores.
- Impacto: dos fuentes de verdad para el mismo saldo (91.1.5); el tablero de transparencia puede mostrar un saldo que el mayor no respalda (agravado por HUECO-001).
- Solución de fondo: saldo leído del mayor; verificación de cuadre en `50_verificacion`.
- Relación con el flujo nuevo: ninguna · Conexiones: nucleo-financiero, transparencia · Estándares: regla 91.1.5 · Esfuerzo: grande

### garantia-entregas-DINERO-006 — El mismo faltante se puede cubrir dos veces con `turnoId` distintos del mismo periodo
- Clase / Severidad / Confianza: DINERO / S2 / sospecha (moot si se retira RESPALDO-001)
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirFaltanteDelCorte.java` (usa `pedido.turnoId()` sin validarlo contra el recaudo); índice `uq_cobertura_respaldo_turno_viva` (`sql/30_indices/08_garantia_incumplimiento.sql`)
- Evidencia: la unicidad es por turno y por clave, no por periodo (reporte del subagente; el agente principal confirmó la validación de periodo-grupo en `:50` pero no la ausencia de validación del turno).
- Comprobación pendiente: prueba de integración con dos turnos para el mismo periodo.
- Impacto/Solución: doble aplicación de reserva / validar turno contra grupos y unicidad por periodo. · Relación: RESPALDO · Estándares: 91.1.4 · Esfuerzo: chico

### garantia-entregas-RESILIENCIA-001 — Recargo por mora que no encuentra fondo se reintenta 10 s y se descarta sin DLT ni reproceso
- Clase / Severidad / Confianza: RESILIENCIA / S2 / probable
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/IngresoAlFondo.java` (lanza si el grupo no tiene fondo y comenta «el reproceso lo aplica»); `servicios/garantia/src/main/java/bo/aportaya/garantia/trabajos/` (solo `.gitkeep`); manejador de errores en plataforma (`ConfiguracionMensajeria.java`)
- Evidencia: `ls servicios/garantia/src/main/java/bo/aportaya/garantia/trabajos` → solo `.gitkeep`; no hay reproceso.
- Impacto: el recargo queda en la billetera y el fondo no lo ve; mensaje perdido en silencio (98.3.4, 96.7.4).
- Solución de fondo: DLT con alerta y reproceso idempotente. · Conexiones: nucleo-financiero, plataforma/comun-mensajeria · Estándares: 96.7, 98.3 · Esfuerzo: medio

### garantia-entregas-SEG-009 — Las llamadas entre servicios propagan el token del usuario final (cobertura empresarial ejecutada con la identidad de Tesorería)
- Clase / Severidad / Confianza: SEG / S2 / probable
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/ClienteConCircuito.java:72,81`; `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/ClienteConCircuito.java:81` (clon, ver DUP-INT-001)
- Evidencia: propagación del `Authorization` entrante; solo algunos adaptadores de entregas usan la credencial `SERVICIO_ENTREGAS`.
- Impacto: viola 98.6.3; el permiso que exige el servicio llamado es el del humano (`ENTREGA_EJECUTAR`), no uno de servicio con alcance; con cumplimiento, el ORGANIZADOR recibe 403 y el aval/restricción se frena.
- Solución de fondo: token de servicio con alcance acotado por operación. · Conexiones: aportes, cumplimiento, garantía · Estándares: OWASP API2:2023; regla 98.6 · Esfuerzo: medio

### garantia-entregas-DUP-INT-001 — `ClienteConCircuito` y `ConfiguracionDeCircuito` copiados en garantía y entregas
- Clase / Severidad / Confianza: DUP-X / S3 / confirmado (mismo nombre y tamaño: 128 y 28 líneas en ambos)
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/ClienteConCircuito.java`, `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/ClienteConCircuito.java`, y las dos `ConfiguracionDeCircuito.java`
- Evidencia: `wc -l` 128/128 y 28/28.
- Impacto: un arreglo de resiliencia o del token (SEG-009) hay que hacerlo dos veces; el código transversal está en `plataforma/comun-web`.
- Solución de fondo: una sola versión en `plataforma/comun-web`. · Relación: ninguna · Estándares: ISO/IEC 25010 mantenibilidad · Esfuerzo: chico

### garantia-entregas-DEFECTO-001 — `ejecutarReemplazo` responde 500 por `MapeoDeGarantia.dinero(null)`
- Clase / Severidad / Confianza: DEFECTO / S3 / probable
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU66ReemplazarParticipante.java` (`new SalidaReemplazo(reemplazoId, "EJECUTADO", null, null, false)`), `servicios/garantia/src/main/java/bo/aportaya/garantia/web/MapeoDeReemplazo.java`, `MapeoDeGarantia.java`
- Evidencia/Comprobación pendiente: reporte del subagente; el test web mockea montos no nulos. Falta ejecutar el endpoint.
- Impacto/Solución: 500 en una operación de grupo / mapear nulos. · Relación: APROB · Esfuerzo: chico

### garantia-entregas-DINERO-007 — `Dinero` del contrato admite negativos y la disolución/reemplazo usan cifras del cliente
- Clase / Severidad / Confianza: DINERO / S2 / probable
- Ubicaciones: `garantia.yaml` y `entregas.yaml` (`pattern: '^-?\d+\.\d{2}$'`); `CU66ReemplazarParticipante.java` (`deudaTotal.menos(asumida)`); `CU67DisolverGrupo.java` (masa, totales y posiciones del cuerpo)
- Impacto: un `deudaQueAsumeElEntrante` negativo infla la deuda retenida; la disolución reparte con totales no cotejados contra aportes/entregas.
- Solución de fondo: `pattern` sin signo para importes de entrada; leer totales de sus dueños. · Estándares: 91.2.2, 90.1.7 · Esfuerzo: medio

### garantia-entregas-DINERO-008 — Residuo de la devolución del fondo sin asignar
- Clase / Severidad / Confianza: DINERO / S3 / probable
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/DevolucionDelFondo.java`, `CU29DevolverFondo.java`
- Evidencia: el reparto topa al último aportante en lo aportado; si el saldo supera lo aportado (recargos) el excedente no se asigna ni se registra.
- Impacto/Solución: viola 91.6.5 («nunca se pierde: se asigna») / declarar regla de residuo (decisión de negocio). · Esfuerzo: chico

### garantia-entregas-RESILIENCIA-002 — `CierreDeSubastas` programado sin ShedLock y sagas de mercado sin recuperación de pasos colgados
- Clase / Severidad / Confianza: RESILIENCIA / S2 / probable
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/trabajos/CierreDeSubastas.java:34`; `SubastaDelCupo.java` (retener → fijar puja); `ComprarOfertaDeTurno.java` (`vencer`, `abortarColgadas` sin programar)
- Evidencia: `@Scheduled(` sin `@SchedulerLock` (la dependencia shedlock está en `build.gradle.kts`).
- Impacto: con varias réplicas, cierres concurrentes; plata de postores retenida si el proceso cae entre retener y fijar.
- Solución de fondo: ShedLock y job de recuperación de PENDIENTES con token de servicio. · Estándares: 96.7.4 · Esfuerzo: medio

### garantia-entregas-OBSERV-001 — Logs con beneficiario y montos atribuibles
- Clase / Severidad / Confianza: OBSERV (y SEG) / S3 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22LiquidarEntrega.java:105,148,189`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:138`
- Evidencia: `BITACORA.info("CU-22 · Entrega liquidada: entrega={}, grupo={}, beneficiario={}, neto={}", …)`; `"… orden={}, entrega={}, monto={}"`.
- Impacto: importe atribuible a una persona en logs (90.2.1). No se comprobó si el enmascarador de PII de la configuración de logs lo cubre (no corre en la nube).
- Solución de fondo: loguear solo identificadores. · Estándares: regla 90.2.1 · Esfuerzo: chico

### garantia-entregas-CONTRATO-001 — Clave `Idempotency-Key` exigida y descartada en `declararIncumplimiento` y `liquidarEntrega`
- Clase / Severidad / Confianza: CONTRATO (y DINERO) / S3 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/IncumplimientosController.java:70`; `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java:65`
- Evidencia: el parámetro `idempotencyKey` no se pasa al caso de uso.
- Impacto: el contrato promete idempotencia por clave y deduplica por otra cosa (obligación o turno); `cubrirIncumplimiento`, `ejecutarAval`, `devolverFondo`, `disolucion` no llevan clave (91.1.4).
- Solución de fondo: propagar y persistir la clave con `UNIQUE`. · Estándares: 91.1.4 · Esfuerzo: chico

### garantia-entregas-DEBIDO-PROCESO-002 — Sin ruta de apelación en garantía; restricción del deudor sin exigir firmeza
- Clase / Severidad / Confianza: DEBIDO-PROCESO / S2 / probable
- Ubicaciones: `servicios/garantia/src/main/resources/openapi/garantia.yaml` (no hay operación de apelación; la tabla `apelacion_sancion` existe); `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU27RestringirDeudor.java`
- Evidencia: `grep -n apelaci` sobre el openapi de garantía sin operaciones.
- Impacto: 91.4.3 exige que la ruta de apelación exista en el código.
- Solución de fondo: implementar apelación (o ubicar el servicio dueño) y precondición de firmeza en restricción. · Relación: R-NUEVA-1 no elimina el debido proceso · Esfuerzo: medio

### garantia-entregas-PRUEBAS-001 — Pruebas que validan el flujo viejo (respaldo y aprobaciones) y faltan matrices negativas
- Clase / Severidad / Confianza: PRUEBAS / S3 / confirmado (inventario de `@DisplayName`)
- Ubicaciones: garantía: `CU131*`, `CU133ContingenciaTest`, `RespaldoCoberturaWebTest`, `RespaldoControllerWebTest`, `CU23Test` (criterio1 cubre en NOTIFICADO), `CoberturaWebTest`, `CU66Test`; entregas: `CU133PozoCompletoTest`, `CU133PozoCompletoRechazosTest`, `CU133PozoCompletoConcurrenciaTest`, `CU133EntregaConsistenteTest`, `AdaptadoresDelPozoTest`, `RespaldoCaidaTest`, `FondeoDelPozoWebTest`; `SeguridadWebTest` en ambos (solo 401/403 por rol)
- Evidencia: `python3 scripts/matriz_de_pruebas.py --servicio entregas` → «Sin prueba web (5): … /entregas/subastas …» (ver `evidencia/05-verificadores.txt`).
- Impacto: al retirar el respaldo las suites fallarán por diseño; la matriz negativa de BOLA (otro organizador, otro grupo, otro usuario) no existe, por eso SEG-002 y SEG-006 no los detecta ninguna prueba.
- Solución de fondo: reescribir las pruebas contra R-NUEVA-1/2 cuando se decida A-2/A-3; agregar matriz negativa por endpoint (80.2.3).
- Relación con el flujo nuevo: APROB / RESPALDO · Estándares: regla 80.2.3, 80.5 · Esfuerzo: grande

### garantia-entregas-DOC-001 — README y comentarios dicen que nadie consume el asiento, y sí se consume
- Clase / Severidad / Confianza: DOC / S4 / probable
- Ubicaciones: `servicios/garantia/README.md:70`; `servicios/garantia/src/test/java/bo/aportaya/garantia/MayorDeDoble.java`; `servicios/entregas/src/test/java/bo/aportaya/entregas/CU133EntregaConsistenteTest.java`
- Impacto/Solución: lector engañado sobre el efecto en el mayor / corregir. · Esfuerzo: chico
