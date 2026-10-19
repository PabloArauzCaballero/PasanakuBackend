# Plan de implementación del backoffice financiero de Pasanaku

Fecha: 2026-10-15. Estado: plan, sin cambios de implementación. Dos carriles de ejecución independientes: **A, dominio y contratos**; **B, experiencia Angular**. Integración por contratos versionados y puertas de aceptación. El objetivo es operar dinero, cumplimiento y cambios de política con trazabilidad y controles claros; la aprobación humana de una política se distingue de la decisión automática de cada caso.

**Decisiones de ejecución confirmadas:** los carriles trabajarán en computadoras independientes sobre el estado local conservado y conciliado con `origin/dev`. Los límites y la matriz de excepciones todavía no están aprobados: se implementan como parámetros con comportamientos sensibles cerrados hasta su validación. «Firmar aprobación» significa por ahora intención explícita, MFA reciente y evidencia auditable; una firma digital certificada queda como decisión aparte de Legal. El punto de partida compartido y las ramas están documentados en `COORDINACION.md`.

## 1. Base comprobada y alcance

Fuentes locales: `docs/Flujo funcional · usuario administrador.md`, `docs/Flujo de pantallas · backoffice administrador.md`, `docs/Cumplimiento.md`, `docs/Arquitectura/ADR-049 Doble aprobación de retiros.md`, contratos OpenAPI de `servicios/*/src/main/resources/openapi/`, `sql/60_semillas/10-roles-y-permisos.sql`, `apps/backoffice/src/app/rutas/`, `apps/backoffice/src/app/nucleo/secciones.ts` y `packages/ui/src/`. Los documentos funcionales son intención de producto, no evidencia de una pantalla operativa. Cada historia exige cotejar modelo, endpoint, autorización y pantalla real.

Ya existen doce roles de operador, permisos sembrados, shell filtrado por permisos, rutas Angular de operación, cumplimiento, admisiones, sistemas, contabilidad y publicidad, componentes UI, bandeja de reclamos, expedientes de verificación y doble aprobación de retiros con segregación y actualización condicional. No se deben reescribir esas piezas por copiar Atlas.

Brechas observadas: el tablero no tiene indicadores; operación carece de rutas para conciliación, cierre, desembolsos, políticas, roles, tarifario, reportes, fondeo y mensajería; cumplimiento tiene fichas de alertas, casos y actas sin bandejas completas y algunos contenedores construyen datos de ejemplo; la respuesta de reclamo no se envía porque falta contrato; retiros exige escribir un UUID porque no hay listado; contabilidad redirige a un UUID de demostración por falta de listado/período vigente; varios permisos exigidos por CU no están catalogados; hay huecos de modelo enumerados en §9 del flujo de pantallas. No usar datos simulados en producción ni sustituirlos por números inventados.

Atlas aporta un patrón de gobierno: preparar versión, bloquear si falla validación, revisar por etapas, registrar quién decidió, publicar por separado y conservar auditoría. En Atlas la confirmación de aprobación registrada no equivale por sí sola a firma criptográfica y su idempotencia/concurrencia requiere endurecimiento; no trasplantar esa semántica como garantía financiera. React/Next y Angular comparten flujos y contratos conceptuales, no componentes ejecutables. Reutilizar el diseño de interacción y el código nativo de Pasanaku.

## 2. Decisiones de producto que fijan ambos carriles

1. **Decisión automática por defecto.** Una política vigente y aprobada evalúa cada solicitud con datos verificables. Resultado: `APROBADA_AUTOMATICA`, `RECHAZADA_AUTOMATICA` o `REVISION_REQUERIDA` con código de razón, versión de política, entradas referenciadas, hora, correlación y evidencia. No llamar «firmada» a una decisión de máquina.
2. **Revisión humana por excepción.** Datos insuficientes, conflicto, umbral, señal de fraude, alerta normativa, error del motor o regla sin vigencia producen una cola con plazo y responsable. Determinar explícitamente por flujo si el fallo bloquea, retiene o permite continuar; los movimientos de dinero sensibles fallan cerrados. No resolver incertidumbre con una aprobación silenciosa.
3. **Gobierno humano de políticas.** Autor redacta → validación técnica y funcional → simulación con casos históricos/sintéticos → revisión independiente de Riesgos → revisión de Cumplimiento cuando corresponda → aprobación autorizada con segundo factor → publicación separada → monitoreo → reversión. Autor, aprobador y publicador deben ser identidades distintas cuando el riesgo lo requiera. La revisión de una política no crea aprobación humana por transacción.
4. **OCR posterior.** Implementar interfaz de evidencia documental con origen, hash, confianza, versión de extractor, consentimiento/finalidad y revisión; al inicio, usar campos capturados/validación documental humana donde la norma o riesgo lo exija. Un extractor OCR futuro cambia el proveedor de datos, no la API de decisiones ni la historia del expediente. Nunca marcar como verificado un dato solo porque un OCR lo leyó.
5. **Firma de aprobación bien definida.** Para aprobación operativa, guardar intención explícita, actor, rol, alcance exacto, versión/estado previo, resultado, fecha, factor reciente, razón y vínculo de evidencia. Si se exige firma digital con efecto jurídico, definir proveedor, certificados, validación, sellado de tiempo y conservación con Legal; no presentar MFA o un botón como firma digital.
6. **Acceso por permiso y propósito.** Menú y pestaña se muestran por permisos, pero cada endpoint revalida alcance, recurso, separación de funciones, segundo factor y motivo de consulta. La identidad técnica del motor tiene permisos mínimos propios y no usa un usuario humano ficticio.
7. **Terminología y estado visibles.** Separar `borrador`, `validado`, `pendiente de aprobación`, `aprobado`, `publicado`, `suspendido`, `revocado`; para casos, `en evaluación`, `resuelto automáticamente`, `retenido`, `en revisión`, `resuelto por operador`, `ejecutado`, `revertido`. Cada estado tiene evento, actor y acción siguiente.

## 3. Modelo de acceso y pestañas objetivo

Los nombres siguientes son perfiles funcionales. Mantener los roles existentes; dividir permisos incompatibles dentro de un rol antes de crear identidades nuevas. Los permisos precisos se deciden y siembran en A1, nunca se infieren de la pestaña.

| Perfil existente | Inicio y pestañas visibles | Acciones permitidas / límite |
| --- | --- | --- |
| `ADMIN_PLATAFORMA` | Tablero operativo, Configuración, Accesos, Catálogos, Tarifario, Integraciones, Reportes, Ayuda | Administra configuración y roles dentro de SoD; no decide por sí solo alertas, paga ni se autoaprueba. |
| `OFICIAL_CUMPLIMIENTO` | Tablero de cumplimiento, Verificaciones, Alertas, Casos, UIF, Políticas sujetas a cumplimiento, Reportes | Decide/evalúa conforme a atribuciones, aprueba reporte cuando corresponda; no edita evidencia ni aprueba su propia preparación. |
| `ANALISTA_CUMPLIMIENTO` | Verificaciones, Alertas, Casos asignados, Evidencia | Investiga, prepara y escala; envío/regla crítica requiere rol aprobador. |
| `AUDITOR_INTERNO` | Tablero de control, Auditoría, Evidencia, Reportes, Hallazgos | Lectura, exportación justificada, hallazgos; sin ejecución ni mutación de operaciones. |
| `RESPONSABLE_RIESGOS` | Riesgos, Políticas, Simulaciones, Excepciones, Incidentes, Reportes | Revisa/aprueba metodología según matriz, no publica su propia propuesta. |
| `RESPONSABLE_SEGURIDAD` | Seguridad, Accesos, Incidentes, Sesiones, Continuidad, Auditoría | Recuperación dual, investigación y contención; sin revelar secretos ni autoaprobar credenciales. |
| `SOPORTE` | Reclamos, Solicitudes, Consulta acotada, Ayuda | Atiende y documenta; datos financieros/sensibles por finalidad y máscara. |
| `PUNTO_RECLAMO` | Reclamos, Apelaciones, Plazos, Evidencia | Gestiona segunda instancia con separación de quien resolvió primero. |
| `TESORERIA` | Posiciones, Conciliación, Cierre, Retiros excepcionales, Desembolsos, Fondeo | Opera y aprueba según límites; no autoriza y ejecuta la misma operación. |
| `CONTABILIDAD` | Períodos, Asientos/conciliación, Presupuesto, Compras, Cobros/Pagos, Activos, Estados | Cierre y registros con controles de período y SoD. |
| `OPERADOR_PUBLICIDAD` | Anunciantes, Campañas, Desempeño, Liquidación | Gestiona relaciones y campañas, sin aprobar contenido propio. |
| `MODERADOR_CONTENIDO` | Cola de moderación, Evidencia, Apelaciones | Aprueba/rechaza material, sin cambiar presupuesto o liquidación. |

**Patrón de pantalla:** título con función y propósito, estado y hora de actualización, filtros en URL, tabla paginada con orden seguro, contadores provenientes del servidor, SLA visible, ficha con resumen/evidencia/decisiones/historial, acciones contextuales, confirmación que muestra consecuencia exacta, reautenticación cuando aplique, recibo con ID y siguiente estado. Proveer carga, vacío, error, denegado, expirado y desactualizado; montos con moneda, zona horaria Bolivia y precisión; datos sensibles enmascarados y revelación con motivo. El tablero debe responder “qué requiere atención ahora”, sin mezclar métricas de universos o períodos distintos.

## 4. Contrato de trabajo entre carriles

- **A es dueño** de esquema, reglas, seguridad de servidor, OpenAPI, datos, decisiones, auditoría y adaptadores. **B es dueño** de rutas, componentes, estados, interacción, accesibilidad y textos. Ambos pueden empezar de inmediato.
- Antes de cada módulo, A publica una ficha de contrato: recurso, campos y nulabilidad, estados/transiciones, permisos, filtros/orden/paginación, códigos de error, versión/ETag, idempotencia, correlación, eventos, ejemplo sanitizado. B diseña contra ese contrato o un adaptador tipado local; no inventa un endpoint ni presenta el adaptador como producción.
- Compatible significa que B consume un cliente generado/validado contra OpenAPI y A no cambia significado de un campo sin versionar. Un campo ausente se muestra como “no disponible”; cero solo representa cero real.
- Gates compartidos: G0 inventario y decisiones de modelo; G1 contrato; G2 recorrido completo con datos reales; G3 controles financieros/seguridad; G4 operación y despliegue. Cada módulo puede pasar gates por separado.

## 5. Carril A — dominio, decisiones y controles (independiente)

### A0. Descubrimiento y diseño de contratos (P0)

- Mapear cada pantalla deseada a CU, entidad, servicio, OpenAPI, permiso y dueño; anotar `existe`, `parcial`, `ausente`, `bloqueado por norma`. Reconciliar los siete huecos del §9 del flujo de pantallas con el esquema actual, en particular denuncia, política de reclamos, tokens de operador, aprobación de recuperación y 24 permisos faltantes.
- Congelar vocabulario de estados, montos/moneda, hora, ID de correlación, errores y eventos. Elegir qué acciones requieren dos personas y cuáles quedan automáticas según política aprobada.
- Entregables: matriz de trazabilidad versionada, ADR de motor de decisiones y matriz de autorización con recursos/acciones/MFA/SoD. Aceptación: ninguna historia P0 sin fuente de verdad ni permiso asignado.

### A1. Identidad, permisos y segregación (P0)

- Normalizar catálogo de permisos por recurso y acción; migraciones/seeders idempotentes. Revisar asignación a doce roles, acceso por ámbito, altas/bajas, revisión periódica y recertificación. Revocación efectiva de sesiones y tokens al cambiar privilegios.
- Completar recuperación/reinscripción con solicitante, aprobador distinto, tiempo de enfriamiento parametrizado, doble control, auditoría; cerrar el hueco de persistencia descrito en el flujo. Proteger consultas sensibles con finalidad y factor reciente; evitar filtración por búsquedas.
- Aceptación: permiso denegado en API aunque se fabrique la URL; autor no aprueba; revocación surte efecto; cada lectura sensible queda atribuible sin guardar secretos.

### A2. Núcleo de decisión automática (P0)

- Crear contrato único de `evaluar`, `obtener decisión`, `historial` y `reprocesar` con clave de idempotencia; estado inmutable de intento, snapshot o referencias verificables de entradas, versión/hash de política, reglas disparadas, motivos legibles, resultado, actor `MOTOR`, tiempos, traza y causalidad con operación resultante.
- Evaluación determinista para misma versión/entrada; límites vigentes parametrizados, no cableados; integridad de fuentes, expiración de evidencia y conflictos. Distinguir rechazo de imposibilidad técnica. Operación monetaria y decisión se coordinan con transacción/outbox para evitar doble ejecución; reintentos reciben el mismo resultado.
- Adaptador documental preparado para OCR: extracción no confiable → normalización → validación → procedencia/confianza → decisión o revisión; sin dataset OCR como dependencia inicial. Datos externos sin respuesta producen política de fallback explícita.
- Aceptación: cada resultado reproduce política/entradas/motivos, reintento no duplica efecto, error técnico no aparece como rechazo al cliente, no se afirma verificación documental inexistente.

### A3. Gobierno y “firma” de políticas (P0)

- Versiones inmutables con borrador, diff, dependencias, compilación/validación, pruebas de regresión, simulación de impacto (volumen, aprobados, rechazados, excepciones por segmento), fecha de vigencia y plan de reversión.
- Flujo ordenado de revisores según riesgo; bloquear autor como aprobador, misma identidad en pasos incompatibles y doble envío concurrente. `UPDATE` condicionado por versión/estado dentro de transacción; idempotencia real de aprobación/rechazo. Comentario y evidencia obligatorios según decisión; registrar factor reciente, propósito y consecuencia.
- Publicar aparte de aprobar; despliegue gradual, pausa y rollback a versión aprobada, con auditoría de qué versión resolvió cada caso. Reportar cambios de política que alteran tasa de decisión o monto expuesto.
- Aceptación: no se activa una política sin gates y aprobaciones; dos clics o dos revisores concurrentes generan una sola transición; un rollback preserva historial, no lo reescribe.

### A4. Colas y operaciones financieras (P0)

- Exponer listados reales con paginación/filtros y SLA para retiros, desembolsos, conciliación, cierres y excepciones. Reusar ADR-049 para retiros, completando idempotencia de respuesta y bandeja. Definir umbrales/SoD por tipo de movimiento en datos versionados.
- Conciliación de saldos y movimientos contra proveedor/custodia: cortes, diferencias, partidas pendientes, conciliador, evidencia, bloqueo de cierre si hay diferencias críticas. Cierre diario con calendario, períodos, lotes, reporte y reapertura controlada. Desembolso automático bajo política, cola solo por excepciones, comprobante y reverso por movimiento inverso.
- Aceptación: ningún débito/crédito duplicado, cada diferencia tiene estado/propietario, cierre reproduce sus cifras y no ocurre con bloqueos activos, transacciones irreversibles tienen recibo y segundo control donde aplique.

### A5. Cumplimiento, reclamos y riesgo (P0/P1)

- Completar endpoints de bandejas y fichas para verificaciones, alertas, casos, actas, reportes UIF y requerimientos; acceso mínimo a evidencia, retención y cadena de custodia. El motor propone clasificaciones conforme a reglas; reporte regulatorio y decisiones legalmente reservadas pasan por responsable autorizado. No automatizar envío externo sin norma y autorización definidas.
- Implementar respuesta/reapertura/apelación de reclamos, relojes de plazo, política de resolución automática versionada y cola humana. Registrar categoría, motivo, consentimiento, comunicación y segunda instancia; modelar denuncia con debido proceso si se habilita.
- Riesgo operacional, incidentes, planes de acción y continuidad: severidad, responsables, plazos, evidencia, estado y escalamiento. Aceptación: cada expediente tiene línea temporal completa y nadie puede borrar un hecho para cambiar el resultado.

### A6. Contabilidad, configuración y canales (P1)

- Proveer listado de ejercicios/período vigente; asientos, cierres, presupuesto, compras, cuentas por pagar/cobrar, activos y estados con corte y versión. Conciliar subledger de billeteras contra mayor y custodio; exportaciones verificables y sin fórmulas/números de ejemplo.
- Tarifario con simulación, vigencia/preaviso y publicación; QR/cuenta recaudadora y proveedores con control dual; mensajería con plantillas, enrutamiento, intentos, estado de entrega y consentimiento. Reportería con definición, programación, permisos de datos, exportación y auditoría.
- Publicidad: anunciantes, campañas, moderación independiente, desempeño y liquidación, con separación contable del dinero de usuarios.

### A7. Observabilidad y preparación operativa (P0 transversal)

- Eventos estructurados y correlación extremo a extremo; métricas de latencia, decisiones por motivo, excepción, revisión vencida, idempotencia, outbox, conciliación, error de proveedor, seguridad y acceso. Alertas con responsable/runbook y panel de salud real.
- Gestión de secretos, cifrado, backups/restauración, continuidad, entornos, migraciones reversibles y retención. Auditoría de acciones y lecturas sensibles, evidencia exportable con control de integridad; supervisión de políticas, terceros y cambios.
- Aceptación: un incidente puede reconstruirse desde ID de operación hasta política/actor/eventos, restauración y reversión tienen procedimiento y evidencia.

## 6. Carril B — experiencia y pantallas Angular (independiente)

### B0. Sistema de diseño y arquitectura de información (P0)

- Inventariar `packages/ui` y conservar tabla virtualizada, filtros URL, banda de propósito, estado de pantalla, estados vacíos, chips, reloj SLA, diálogos, montos, máscaras y skeletons. Formalizar tokens de tipografía, espaciamiento, densidad, color semántico, foco, contraste y breakpoints; variantes compactas para tablas financieras.
- Revisar shell: menú por permiso efectivo, subtabs por función, breadcrumb, búsqueda global con autorización, selector de período/ámbito cuando aplique, acciones globales mínimas. En pantalla estrecha, conservar estado/acción y mover columnas secundarias a detalle. Contenido en español claro y sin “firma digital” cuando solo hay aprobación autenticada.
- Aceptación: mismo significado visual de estado y riesgo en todos los módulos; navegación por teclado/lector y foco al cambiar estado; no hay acciones muertas.

### B1. Patrones operativos reutilizables (P0)

- Crear contenedor de bandeja tipado con filtros persistentes, columnas configuradas por permiso, paginación servidor, selección segura, estado vacío/error/carga y vínculo profundo. Crear ficha de expediente con resumen, línea temporal, documentos, motivo, decisiones e historial; no precargar fotografías o datos sensibles.
- Crear panel de confirmación para decisiones humanas: identidad/recurso/importe/versión, efecto exacto, razón y evidencia requerida, factor reciente, `Idempotency-Key`, estado de envío, recibo o conflicto por versión. Para decisión automática, mostrar tarjeta explicativa de política/motivos y botón de revisión solo si corresponde.
- Adaptador de API generado/tipado, manejo central de 401/403/409/412/422/429/5xx y recarga tras conflicto; recuperación de borrador local sin asumir que hubo commit. Aceptación: doble clic no produce dobles efectos; la UI no dice “aprobado” hasta recibir confirmación del servidor.

### B2. Tablero y trabajo diario (P0)

- Reemplazar el tablero de enlaces por proyecciones reales filtradas por rol: asuntos vencidos/próximos, excepciones monetarias, conciliación, alertas, reclamos, salud de integraciones. Cada KPI tiene período, última actualización, definición, drilldown y estado sin datos. Mantener accesos actuales como atajos.
- Separar “requiere mi acción” de “monitoreo” y “tendencia”. No mostrar totales de usuarios o dinero fuera de ámbito autorizado. Aceptación: desde un indicador se llega a la lista filtrada que explica su número.

### B3. Operación y Tesorería (P0)

- Agregar rutas de conciliación, cierre, desembolsos automáticos/excepciones, retiro en revisión, movimientos/reversos, reclamos, políticas de reclamo, roles, tarifario, reportes, fondeo y mensajería. La bandeja de retiros sustituye el ingreso manual de UUID, conservando ficha por ID para enlaces profundos.
- Conciliación: cifras enfrentadas por corte, diferencia destacada, desglose y evidencia. Cierre: prerequisitos, bloqueos, vista previa, confirmación y comprobante. Desembolso: política aplicada, estado de intento, excepción y trazabilidad. Reclamo: respuesta real, plazo y apelación; eliminar diálogos que simulan enviar.
- Aceptación: lista → ficha → decisión/acción → recibo → historial, sin datos de demostración en producción ni promesas de endpoint inexistente.

### B4. Cumplimiento, Riesgos y Seguridad (P0/P1)

- Completar bandejas de verificación, alertas, casos, actas, UIF/requerimientos y organizadores; tarjetas de evidencia con procedencia, vigencia y acceso por finalidad. Diferenciar recomendación automática, decisión humana y envío externo. Dos columnas de comparación documental donde sea útil; carga diferida de imagen sensible.
- Pantallas de políticas/simulaciones/aprobaciones y excepciones: diff legible, matriz de impacto, comentarios, orden de firmas, siguiente aprobador, versión vigente y rollback; trazabilidad sin exponer datos innecesarios. Seguridad: sesiones, recuperaciones, factor, incidentes y continuidad con control dual.
- Aceptación: los contenedores de ejemplo quedan conectados a contratos reales o muestran indisponibilidad honesta; ninguna acción aparece si falla permiso o separación de funciones.

### B5. Contabilidad, configuración, publicidad y auditoría (P1)

- Sustituir redirección a UUID de demo por selector/listado del período real. Crear recorridos completos de compra, cobro, pago, activos, cierre y estados; origen de cifras y exportación visible. Tarifario, proveedores, QR, plantillas, reportes y roles con vista previa de cambios y vigencia.
- Completar publicidad con aprobación de campaña y moderación por identidad separada. Crear explorador de auditoría de solo lectura: filtros actor/recurso/intervalo/evento, timeline, exportación controlada, hashes/IDs de evidencia y enlaces a expedientes.
- Aceptación: ninguna pantalla de producción presenta datos ficticios; todo exportable indica corte, filtros, autor y permisos.

### B6. Calidad de experiencia transversal (P0)

- Semántica de color además de texto/icono; errores accionables sin revelar datos, validación de formularios, confirmación de cambios sensibles, estados de espera y prevención de pérdida de borrador. Proteger portapapeles, descarga y caché de datos sensibles según finalidad.
- Verificar diseño en escritorio y tablet, teclado, lector, zoom, idioma, fechas/moneda y tablas grandes. Microcopy explica por qué una decisión es automática, por qué está retenida y qué acción sigue; no promete certeza OCR.

## 7. Orden propuesto y gates

| Ola | Carril A puede entregar | Carril B puede entregar | Gate conjunto |
| --- | --- | --- | --- |
| 0 — inventario | A0, matriz de permisos, vocabulario y contratos P0 | B0, inventario UI, mapa de rutas/estados | G0: decisiones abiertas registradas, dueño y fecha; sin endpoints asumidos. |
| 1 — núcleo | A1–A3: identidad, motor, gobierno | B1–B2: patrones, tablero con adaptadores tipados | G1: OpenAPI + ejemplos + permisos + errores versionados. |
| 2 — dinero | A4: colas, conciliación, cierre, desembolso | B3: recorridos de operación | G2: operación real de extremo a extremo y recibo auditable. |
| 3 — control | A5 y A7: cumplimiento, reclamos, observabilidad | B4 y auditoría de B5 | G3: SoD, accesos, reintentos, contingencia, evidencia. |
| 4 — cobertura | A6: ERP/configuración/canales/publicidad | resto de B5 y B6 | G4: cada módulo sin demo, soporte operativo y documentación. |

El desarrollo de cada carril es independiente hasta G1: B trabaja con fixtures contractuales **etiquetados solo para desarrollo**, A implementa endpoints sin depender de componentes. Priorizar un corte vertical real por módulo al integrar. No usar una fecha ni porcentaje de progreso sin estimación del equipo; medir historias cerradas por gate y evidencia.

## 8. Criterios de aceptación por flujo crítico

| Flujo | Casos obligatorios para declarar completo |
| --- | --- |
| Decisión automática | misma entrada/versión = mismo resultado; entrada faltante = excepción; política suspendida = bloqueo definido; reintento y evento duplicado sin doble movimiento; razones y versión visibles. |
| Aprobar política | autor distinto; orden de revisores; comentario/evidencia; factor reciente; dos aprobaciones simultáneas; publicación separada; reversión; decisión histórica sigue apuntando a versión anterior. |
| Retiro/desembolso | umbral vigente, SoD, saldo/custodia, proveedor lento o duplicado, estado después de timeout, recibo, reverso compensatorio. |
| Conciliación/cierre | diferencia, datos tardíos, corte reproducible, bloqueo/reapertura, asiento y exportación coincidentes. |
| Verificación/alerta/ROS | acceso por finalidad, documento vencido, evidencia incompleta, escalamiento, revisión independiente, confidencialidad y trazabilidad de envío. |
| Reclamo/apelación | plazo, respuesta enviada, política automática, excepción, segunda instancia independiente, evidencia de comunicación. |
| Acceso/auditoría | permiso revocado, sesión vencida, intento directo por URL/API, dato enmascarado, exportación autorizada y lectura auditada. |

## 9. Reciclaje, exclusiones y decisiones pendientes

**Reciclar local:** componentes `packages/ui`, `tabla-de-datos-virtualizada`, filtros en URL, `secciones.ts`, guardias, sesiones, rutas, clientes y CU/OpenAPI existentes; restricciones SQL y ADR-049. **Adaptar de Atlas:** estructura de gobierno de versiones, review queue, confirmation dialog, timeline, lineage, diff, simulación, paneles de QA, estado de jobs y estados claros. **No copiar literal:** componentes React en Angular, esquema de permisos de otro producto, cadena Atlas de aprobaciones como firma jurídica, idempotencia aparente o datos demo.

Decisiones que requieren dueño de negocio/Legal antes del despliegue: clasificación regulatoria aplicable a Pasanaku; límites, plazos y preavisos vigentes; qué decisiones pueden automatizarse por producto; qué requiere revisión humana o firma digital formal; retención y acceso a evidencia; matriz de incompatibilidades; facultades para publicación de políticas y envío regulatorio. El equipo técnico puede desarrollar con valores parametrizados y flags cerrados; no inventar parámetros normativos. La Recopilación ASFI se actualiza, por lo que Legal debe validar su versión aplicable antes de lanzar.

## 10. Definición de terminado del backoffice

Cada módulo tiene trazabilidad CU → entidad → permiso → contrato → pantalla → evento → evidencia; no datos falsos; denegación en servidor; estados de UI completos; acción repetida segura; reconciliación y auditoría consultables; accesibilidad y rendimiento aceptables con volúmenes reales; responsable operativo, alerta y runbook; migración y reversión documentadas. El backoffice entero se considera listo solo cuando los flujos P0 están integrados y los P1 habilitados por producto cumplen el mismo estándar. La aptitud regulatoria final exige revisión legal y operativa independiente.

## Referencias externas de criterio

- Atlas Decision Engine Backend, gobierno de artefactos: https://github.com/PabloArauzCaballero/AtlasDecisionEngineBackend/blob/432dc3f7142501f75325f5f49655af9ee458961c/src/modules/governance/governance.service.ts
- Atlas Decision Engine Frontend, confirmación: https://github.com/PabloArauzCaballero/AtlasDecisionEngineFrontend/blob/d47e436a99d00343f2e251c77c8390862e4e7719/src/features/governance/DecisionConfirmDialog.tsx
- Atlas Admin Portal, referencia de organización de módulos: https://github.com/PabloArauzCaballero/AtlasAdminPortal
- Atlas Backend, distinción entre decisión de motor y operador: https://github.com/PabloArauzCaballero/AtlasBackend/blob/ea03efdfbe65c91086858bf87e0d194e3c2fcd36/src/modules/partner-onboarding/application/partner-verification.service.ts
- ASFI, Recopilación de Normas para Servicios Financieros: https://www.asfi.gob.bo/node/401
- NIST, Cybersecurity Framework 2.0: https://www.nist.gov/publications/nist-cybersecurity-framework-csf-20
