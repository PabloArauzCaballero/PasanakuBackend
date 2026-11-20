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
