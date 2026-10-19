# Atlas · roles internos y permisos (fuente: AtlasBackend/src/modules/internal-users, leído 2026-10-18)

## Roles (`internal-rbac.roles.ts`)

| Código | Nombre | Departamento | Descripción |
|---|---|---|---|
| `SUPER_ADMIN` | Superadministrador ATLAS | SYSTEMS | Acceso total al backend interno; reservado a bootstrap y emergencia auditada. |
| `SYSTEMS_ADMIN` | Administrador de sistemas | SYSTEMS | Administra configuración operativa, catálogo técnico, QA, jobs y observabilidad. |
| `INTERNAL_IDENTITY_ADMIN` | Administrador de identidad interna | SYSTEMS | Crea usuarios, asigna roles y gestiona bloqueos internos. |
| `OPERATIONS_MANAGER` | Jefatura de operaciones | OPERATIONS | Gestiona colas operativas, asignaciones y revisión de casos. |
| `OPERATIONS_ANALYST` | Analista de operaciones | OPERATIONS | Consulta y atiende casos operativos asignados. |
| `RISK_MANAGER` | Jefatura de riesgo | RISK | Revisa scoring, políticas de riesgo y overrides controlados. |
| `RISK_ANALYST` | Analista de riesgo | RISK | Consulta expedientes, scoring y explicabilidad de riesgo. |
| `FRAUD_ANALYST` | Analista de fraude | RISK | Investiga señales, watchlists, casos de fraude y vínculos sospechosos. |
| `COMPLIANCE_MANAGER` | Jefatura de cumplimiento | COMPLIANCE | Gobierna KYC, consentimientos, privacidad, auditoría y requerimientos regulatorios. |
| `COMPLIANCE_ANALYST` | Analista de cumplimiento | COMPLIANCE | Consulta y revisa KYC, consentimientos, privacidad y evidencias. |
| `COLLECTIONS_MANAGER` | Jefatura de cobranza | COLLECTIONS | Gestiona mora, estrategias de cobranza y asignación de cartera. |
| `COLLECTIONS_AGENT` | Agente de cobranza | COLLECTIONS | Atiende recordatorios, promesas de pago y acciones de cobranza asignadas. |
| `FINANCE_MANAGER` | Jefatura financiera | FINANCE | Consulta y administra conciliación, liquidaciones y reportes financieros internos. |
| `MERCHANT_OPERATIONS` | Operaciones de comercios | OPERATIONS | Gestiona onboarding, soporte y operación diaria de comercios. |
| `DATA_GOVERNANCE_MANAGER` | Gobierno de datos | SYSTEMS | Administra catálogo, metadata de negocio, sensibilidad y ownership. |
| `DATA_QUALITY_ANALYST` | Analista de calidad de datos | SYSTEMS | Revisa reglas, incidencias y resultados de calidad de datos. |
| `QA_ENGINEER` | QA interno | SYSTEMS | Ejecuta suites controladas, revisa endpoints y reportes de pruebas. |
| `AUDITOR_READONLY` | Auditor solo lectura | AUDIT | Consulta auditoría, catálogos y reportes sin capacidad de mutación. |
| `SUPPORT_AGENT` | Soporte interno | SUPPORT | Consulta información mínima para soporte a clientes y comercios. |
| `EXECUTIVE_READONLY` | Ejecutivo solo lectura | EXECUTIVE | Consulta dashboards ejecutivos y métricas sin datos mutables. |

## Permisos (catálogo completo)

| Código | Módulo | Recurso | Acción | Riesgo | Exige motivo | Descripción |
|---|---|---|---|---|---|---|
| `privacy.requests.read` | privacy | requests | read | MEDIUM | — | Ver la cola de solicitudes de derechos del titular, su plazo legal y su historial. |
| `privacy.requests.manage` | privacy | requests | manage | HIGH | sí | Tomar, completar o rechazar una solicitud de derechos del titular. Cerrarla exige motivo; no borra datos: la supresión sigue siendo manual. |
| `auth.internal.me.read` | auth | internal_session | read | LOW | — | Consultar perfil, roles y permisos efectivos propios. |
| `systems.dashboard.read` | systems | dashboard | read | — | — | Consultar dashboard técnico del portal interno. |
| `systems.endpoints.read` | systems | endpoint_catalog | read | — | — | Consultar catálogo de endpoints. |
| `systems.endpoints.execute` | systems | endpoint_catalog | execute | HIGH | sí | Ejecutar endpoint controlado desde QA. |
| `workflows.read` | workflows | process_catalog | read | — | — | Consultar Procesos: la ficha de cada proceso de negocio, sus etapas, su cableado y sus instancias en curso. |
| `systems.flows.read` | systems | flow_catalog | read | — | — | Consultar Flujos: el mapa derivado del código de pantallas, endpoints, permisos y hallazgos. |
| `systems.flows.analyze` | systems | flow_catalog | analyze | — | — | Cargar o regenerar el catálogo de Flujos desde el artefacto derivado. |
| `systems.flows.review` | systems | flow_catalog | review | — | — | Confirmar o descartar hallazgos y conexiones inferidas de Flujos. |
| `systems.endpoints.discover` | systems | endpoint_catalog | discover | HIGH | sí | Descubrir endpoints y actualizar revisión. |
| `systems.endpoints.catalogSeedRefresh` | systems | endpoint_catalog | seed_refresh | HIGH | sí | Refrescar catálogo técnico desde seed. |
| `systems.dataEntities.read` | systems | data_entity | read | — | — | Consultar entidades y tablas catalogadas. |
| `systems.dataEntities.updateMetadata` | systems | data_entity | update_metadata | HIGH | sí | Actualizar metadata técnica. |
| `systems.reviewQueue.read` | systems | review_queue | read | — | — | Consultar cola de revisión. |
| `systems.reviewQueue.resolve` | systems | review_queue | resolve | HIGH | sí | Resolver elementos de revisión. |
| `systems.tools.read` | systems | tool_catalog | read | — | — | Consultar herramientas internas. |
| `systems.network.read` | systems | network_health | read | — | — | Consultar la salud de la RED de bloques del ecosistema. |
| `systems.network.federate` | systems | block_catalog | federate | HIGH | sí | Refederar el catálogo de un bloque del ecosistema. |
| `systems.decisionEngine.artifacts.read` | systems | decision_artifact | read | — | — | Consultar los artefactos ACTIVOS del motor de decisión. |
| `systems.tools.health.read` | systems | tool_health | read | — | — | Consultar salud de herramientas internas. |
| `systems.tools.inferRequirements` | systems | tool_requirements | infer | HIGH | sí | Inferir requisitos de herramientas. |
| `systems.qa.read` | systems | qa_suite | read | — | — | Consultar suites y corridas QA. |
| `systems.qa.execute` | systems | qa_run | execute | HIGH | sí | Ejecutar suites QA controladas. |
| `systems.stress.read` | systems | stress_profile | read | — | — | Consultar perfiles y corridas stress. |
| `systems.stress.execute` | systems | stress_run | execute | CRITICAL | sí | Ejecutar stress controlado. |
| `credit.loan_disbursement.override_rate` | credit | loan_disbursement | override_rate | HIGH | sí | Fijar al desembolsar una tasa distinta de la que decidió el Motor o la del producto. Sigue clampeada al rango del producto y al tope de usura. |
| `expedientes.leer` | expedientes | expediente | read | MEDIUM | — | Ver el árbol de un expediente, abrir sus archivos y su actividad. |
| `expedientes.escribir` | expedientes | expediente | write | HIGH | — | Subir, crear carpetas, renombrar, mover y enviar a la papelera. |
| `expedientes.compartir` | expedientes | expediente | share | HIGH | sí | Conceder y revocar acceso a carpetas y archivos de un expediente. |
| `expedientes.administrar` | expedientes | expediente | admin | CRITICAL | sí | Purgar la papelera, congelar y conceder administración de un expediente. |
| `expedientes.pii.revelar` | expedientes | expediente_pii | reveal | CRITICAL | sí | Ver sin enmascarar los contactos y referencias del expediente. |
| `catalog.data.read` | catalog | data_catalog | read | — | — | Consultar catálogo de datos. |
| `catalog.data.manage` | catalog | data_catalog | manage | HIGH | sí | Administrar catálogo de datos. |
| `businessMetadata.read` | business_metadata | business_term | read | — | — | Consultar metadata de negocio. |
| `businessMetadata.manage` | business_metadata | business_term | manage | HIGH | sí | Administrar metadata de negocio. |
| `governance.data.read` | governance | data_governance | read | — | — | Consultar gobierno de datos. |
| `governance.data.manage` | governance | data_governance | manage | HIGH | sí | Administrar gobierno de datos. |
| `governance.policies.read` | governance | policy | read | — | — | Consultar políticas de gobierno. |
| `governance.policies.manage` | governance | policy | manage | HIGH | sí | Administrar políticas de gobierno. |
| `governance.schema.propose` | governance | schema_change | propose | HIGH | sí | Proponer un cambio de esquema (queda pendiente de aprobación por otra persona). |
| `governance.schema.approve` | governance | schema_change | approve | CRITICAL | sí | Aprobar o rechazar un cambio de esquema propuesto por otra persona. |
| `dataQuality.issues.read` | data_quality | quality_issue | read | — | — | Consultar incidencias de calidad. |
| `dataQuality.issues.resolve` | data_quality | quality_issue | resolve | HIGH | sí | Resolver incidencias de calidad. |
| `dataQuality.rules.read` | data_quality | quality_rule | read | — | — | Consultar reglas de calidad. |
| `dataQuality.rules.manage` | data_quality | quality_rule | manage | HIGH | sí | Administrar reglas de calidad. |
| `operations.catalogs.read` | operations | catalog | read | — | — | Consultar catálogos operativos. |
| `operations.definitions.read` | operations | definition | read | — | — | Consultar definiciones operativas. |
| `operations.riskPolicy.read` | operations | risk_policy | read | — | — | Consultar política de riesgo vigente. |
| `reporting.read` | reporting | report | read | — | — | Consultar reportes dinámicos. |
| `reporting.execute` | reporting | report | execute | HIGH | sí | Ejecutar reportes dinámicos. |
| `reporting.manage` | reporting | report | manage | HIGH | sí | Administrar reportes dinámicos. |
| `lineage.read` | lineage | lineage_graph | read | — | — | Consultar lineage e impacto. |
| `audit.events.read` | audit | audit_event | read | HIGH | — | Consultar eventos de auditoría. |
| `audit.events.detail` | audit | audit_event | detail | HIGH | — | Consultar detalle de auditoría sensible. |
| `notifications.messages.read` | notifications | notification_message | read | — | — | Consultar mensajes de notificación (in-app/push/email/sms/whatsapp) y su historial de entrega. |
| `notifications.messages.manage` | notifications | notification_message | manage | HIGH | sí | Reintentar o cancelar mensajes de notificación pendientes/fallidos. |
| `notifications.templates.read` | notifications | notification_template | read | — | — | Consultar plantillas de notificación. |
| `notifications.templates.manage` | notifications | notification_template | manage | HIGH | sí | Crear y editar plantillas de notificación. |
| `internal.users.read` | internal | internal_user | read | — | — | Consultar usuarios internos. |
| `merchant.users.read` | merchant | merchant_user | read | — | — | Consultar identidades de usuarios de comercios afiliados. |
| `merchant.users.manage` | merchant | merchant_user | manage | HIGH | — | Conceder o rechazar el acceso pedido por el ERP, y activar, suspender o dar de baja el de un usuario de comercio. |
| `merchant.users.request` | merchant | merchant_user | request | — | — | Encolar una petición de alta de identidad de comercio. Lo usa el ERP, no el portal interno. |
| `partner.kyb.request` | merchant | partner_profile | request | — | — | Pedir al Motor la verificación del expediente de un comercio, y enlazar el expediente con su cuenta del ERP. Lo usa el ERP, no el portal interno. |
| `partner.qr.review` | merchant | partner_qr_code | review | — | — | Aprobar o rechazar el QR de cobro de un comercio. Es lo que decide a qué cuenta transfieren los clientes: hasta que alguien lo aprueba, la app no lo enseña. |
| `internal.users.manage` | internal | internal_user | manage | CRITICAL | sí | Crear, editar, suspender y gestionar usuarios internos. |
| `internal.roles.read` | internal | internal_role | read | — | — | Consultar roles internos. |
| `internal.roles.manage` | internal | internal_role | manage | CRITICAL | sí | Administrar roles internos. |
| `internal.permissions.read` | internal | internal_permission | read | — | — | Consultar permisos internos. |

Total: 69 permisos.

## Permisos por rol (`ROLE_PERMISSION_CODES`)

- **INTERNAL_IDENTITY_ADMIN** (8): `auth.internal.me.read`, `internal.users.read`, `internal.users.manage`, `internal.roles.read`, `internal.roles.manage`, `internal.permissions.read`, `audit.events.read`, `audit.events.detail`
- **OPERATIONS_MANAGER** (17): `auth.internal.me.read`, `workflows.read`, `expedientes.leer`, `expedientes.escribir`, `expedientes.compartir`, `systems.dashboard.read`, `operations.catalogs.read`, `operations.definitions.read`, `operations.riskPolicy.read`, `catalog.data.read`, `reporting.read`, `notifications.messages.read`, `notifications.templates.read`, `merchant.users.request`, `merchant.users.read`, `partner.kyb.request`, `credit.loan_disbursement.override_rate`
- **OPERATIONS_ANALYST** (7): `auth.internal.me.read`, `workflows.read`, `expedientes.leer`, `expedientes.escribir`, `operations.catalogs.read`, `operations.definitions.read`, `catalog.data.read`
- **RISK_MANAGER** (9): `auth.internal.me.read`, `workflows.read`, `expedientes.leer`, `expedientes.escribir`, `expedientes.compartir`, `operations.riskPolicy.read`, `catalog.data.read`, `reporting.read`, `audit.events.read`
- **RISK_ANALYST** (6): `auth.internal.me.read`, `workflows.read`, `expedientes.leer`, `expedientes.escribir`, `operations.riskPolicy.read`, `catalog.data.read`
- **FRAUD_ANALYST** (8): `auth.internal.me.read`, `workflows.read`, `expedientes.leer`, `expedientes.escribir`, `expedientes.pii.revelar`, `operations.catalogs.read`, `catalog.data.read`, `audit.events.read`
- **COMPLIANCE_MANAGER** (12): `auth.internal.me.read`, `workflows.read`, `expedientes.leer`, `expedientes.compartir`, `expedientes.pii.revelar`, `governance.data.read`, `governance.policies.read`, `audit.events.read`, `audit.events.detail`, `reporting.read`, `privacy.requests.read`, `privacy.requests.manage`
- **COMPLIANCE_ANALYST** (7): `auth.internal.me.read`, `workflows.read`, `expedientes.leer`, `governance.data.read`, `governance.policies.read`, `audit.events.read`, `privacy.requests.read`
- **COLLECTIONS_MANAGER** (6): `auth.internal.me.read`, `workflows.read`, `expedientes.leer`, `operations.catalogs.read`, `operations.definitions.read`, `reporting.read`
- **COLLECTIONS_AGENT** (4): `auth.internal.me.read`, `workflows.read`, `operations.catalogs.read`, `operations.definitions.read`
- **FINANCE_MANAGER** (5): `auth.internal.me.read`, `workflows.read`, `reporting.read`, `reporting.execute`, `audit.events.read`
- **MERCHANT_OPERATIONS** (7): `auth.internal.me.read`, `workflows.read`, `operations.catalogs.read`, `operations.definitions.read`, `merchant.users.read`, `merchant.users.manage`, `partner.qr.review`
- **DATA_GOVERNANCE_MANAGER** (13): `auth.internal.me.read`, `workflows.read`, `catalog.`, `businessMetadata.`, `governance.`, `dataQuality.`, `systems.dataEntities.read`, `systems.dataEntities.updateMetadata`, `systems.flows.read`, `systems.flows.analyze`, `systems.flows.review`, `lineage.read`, `audit.events.read`
- **DATA_QUALITY_ANALYST** (7): `auth.internal.me.read`, `workflows.read`, `catalog.data.read`, `dataQuality.issues.read`, `dataQuality.issues.resolve`, `dataQuality.rules.read`, `dataQuality.rules.manage`
- **QA_ENGINEER** (9): `auth.internal.me.read`, `workflows.read`, `systems.endpoints.read`, `systems.endpoints.execute`, `systems.qa.read`, `systems.qa.execute`, `systems.stress.read`, `systems.flows.read`, `catalog.data.read`
- **AUDITOR_READONLY** (1): `auth.internal.me.read`
- **SUPPORT_AGENT** (3): `auth.internal.me.read`, `operations.catalogs.read`, `operations.definitions.read`
- **EXECUTIVE_READONLY** (5): `auth.internal.me.read`, `workflows.read`, `systems.dashboard.read`, `reporting.read`, `catalog.data.read`
