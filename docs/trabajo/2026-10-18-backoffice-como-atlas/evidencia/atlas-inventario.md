# Inventario del portal AtlasAdminPortal

Repo: `/Users/pablo/Documents/GitHub/atlas/AtlasAdminPortal`. Hay 129 archivos `page.tsx`: 126 bajo `src/app/internal`, más `src/app/page.tsx`, `src/app/legal/page.tsx` y `src/app/legal/[documentCode]/page.tsx`.

**Cómo lo saqué:** para cada página seguí todas sus importaciones de `src/features` y extraje las props y textos de PageHeader, BusinessContextNote, PermissionGate/RoleGate, MetricCard, `header:` de columnas, FilterBar, las Badges, los Button/diálogos y EmptyState/ErrorState. Los endpoints salen de encadenar hook → función de servicio → URL. Escribí este markdown a mano a partir de esa extracción.

**Hay tres huecos:**
- **Endpoints de escritura incompletos.** Algunas mutaciones que viven en componentes hijos no aparecen. En Soporte las añadí a mano.
- **Columnas de tablas hermanas juntas.** Cuando una pantalla tiene varias tablas (pestañas o secciones), sus cabeceras van en orden de aparición, separadas por `/` cuando se ve claro de qué tabla son.
- **Gate de página.** Si no se nombra un gate, la página no tiene PermissionGate ni RoleGate propio; solo el menú la oculta y el backend devuelve 403.

**Patrones comunes a casi todas las pantallas:**
- La carga se muestra con `LoadingSkeleton`.
- Los errores se muestran con `ErrorState`, que lleva un texto "No se pudo…", el requestId y un botón para reintentar.
- Las tablas son `DataTable`, paginadas en el servidor, con `emptyTitle`/`emptyDescription`.
- Los filtros son `FilterBar`: búsqueda más selects, con opción "Todos" y botón para limpiar.

---

## 0. Layout global (shell)

**`src/app/internal/layout.tsx`** monta, en este orden:
- `AppProviders`
- `RouteProgress`, la barra de progreso de navegación
- `ScreenTracker`, que añade el origen (la pantalla) a cada llamada al backend
- `TutorialProvider`, con los recorridos guiados de QA
- `InternalProtectedShell`, que contiene `AppShell`

**`AppShell`** (`src/shared/components/layout/app-shell.tsx`) contiene:
- `AmbientBackground`, cuya variante depende de la ruta
- `AppSidebar`, de 268px
- `AppTopbar`
- `<main key={pathname}>`, que tiene un fade-in en cada navegación y lleva `ViewExplainer` encima del contenido
- `AssistFab`, el botón flotante "Asistente" ("Asistente de Atlas")

**`ViewExplainer`** es un panel plegable por vista con dos bloques: "Explicación de negocio" y "Cómo funciona".

**Topbar** (`internal-shell/app-topbar.tsx`), con estilo glass y sticky, de 64px de alto:
- Hamburguesa "Abrir navegación", solo por debajo de lg.
- `Breadcrumbs`, con etiquetas sacadas de `breadcrumb-labels`.
- Línea de ambiente: `LiveDot` verde + "Ambiente: {NEXT env label, por defecto 'local'}" + " · {host del API o 'servicio local'}". El host se oculta en móvil. **No hay un badge de entorno aparte, solo esta línea de texto.**
- `GlobalSearchBox` (md+):
  - Placeholder "Buscar en el portal…", aria-label "Buscador global".
  - Debounce de 350ms y lista de sugerencias ("Resultados de búsqueda").
  - Enter sin selección lleva a `/internal/search?q=`.
  - En móvil se sustituye por un icono de lupa que enlaza a `/internal/search`.
- `NotificationBell`:
  - aria-label "Notificaciones".
  - Badge de no leídas (muestra "9+" por encima de 9) y anima cuando sube el número.
  - El dropdown trae las últimas 6.
  - Acciones "Marcar todas" y "Ver todas" (enlaza a `/internal/my-notifications`).
  - Endpoints: GET `/internal-users/me/notifications/unread-count` y GET `/internal-users/me/notifications`.
- **No hay menú de usuario en el topbar.** Usuario y logout están en el pie del sidebar.

**Sidebar** (`internal-shell/app-sidebar.tsx`, `nav-config.ts`, `nav-groups*.ts`, `nav-items-*.ts`):
- Cabecera: logo "A" sobre gradiente, "ATLAS" / "Portal interno" y una X "Cerrar navegación" solo en móvil.
- Funciona como drawer en móvil, con velo que cierra al tocar fuera.
- Un ítem se ve si `hasAnyPermission(item.permissions)` y `hasAnyRole(item.roles)`. Una lista vacía significa sin restricción.
- Un grupo sin ítems visibles se oculta.
- Los grupos son plegables (chevron) y se abren solos si contienen la ruta activa.
- El ítem activo lleva una barra teal a la izquierda.
- Pie: avatar con la inicial, nombre completo, email y botón "Cerrar sesión" (icono LogOut).

**Roles referenciados en el menú:**
- **INTERNAL_PORTAL:** internal_operator, risk_analyst, compliance_analyst, admin, platform_admin, system_admin, qa_engineer, devops, readonly_auditor.
- **OPERATIONS_CASE:** internal_operator, risk_analyst, compliance_analyst, admin, platform_admin.
- **WORK_QUEUE:** OPERATIONS_CASE + fraud_analyst.
- **PARTNER_OPS:** internal_operator, risk_analyst, admin, platform_admin (lista truncada en la lectura).
- **CREDIT_OPS:** internal_operator, risk_analyst, admin, platform_admin (lista truncada en la lectura).
- **LOAN_READ:** internal_operator, risk_analyst, admin, platform_admin.
- **LOAN_RATING:** internal_operator, risk_analyst, compliance_analyst, admin, platform_admin.
- **LOAN_PORTFOLIO:** LOAN_READ ∪ LOAN_RATING.
- **LOAN_MONEY:** internal_operator, admin, platform_admin.
- **LOAN_WRITE_OFF:** admin, platform_admin.
- **PAYMENT_CLAIMS:** internal_operator, risk_analyst, compliance_analyst, admin, platform_admin.
- **CAMPAIGN_READ:** internal_operator, admin, platform_admin, system.
- **CAMPAIGN_OPERATE:** admin, platform_admin.
- **SUPPORT_ADMIN:** admin, platform_admin.
- **SUPPORT_KNOWLEDGE:** internal_operator, compliance_analyst, risk_analyst, admin, platform_admin.
- **RUNTIME_JOB:** admin, platform_admin, system.
- **DECISION_ARTIFACT:** internal_operator, risk_analyst, admin, platform_admin.

**Orden del sidebar** (etiqueta — ruta — icono lucide — qué lo oculta):

1. **Ítems sueltos arriba**
   - Inicio — `/internal` — Home — nada
   - Mis notificaciones — `/internal/my-notifications` — Bell — nada
2. **Sistemas** (Activity)
   - Operaciones del sistema — `/systems/endpoints` — Activity — `systems.endpoints.read`
   - Mapa de rutas — `/flows` — Waypoints — `systems.flows.read`
   - Trabajo pendiente — `/flows/pending-work` — Hourglass — `systems.flows.read`
   - Deriva de permisos — `/flows/rbac-drift` — ShieldAlert — `systems.flows.read`
   - Revisión de análisis de flujos — `/flows/review` — ClipboardCheck — `systems.flows.read`
   - Compuerta de documentación — `/flows/gate` — BadgeCheck — `systems.flows.read`
   - Herramientas — `/systems/tools` — Wrench — `systems.tools.read` o `systems.tools.health.read`
   - Salud de la red — `/systems/network-health` — Network — `systems.network.read`
   - Artefactos del motor — `/systems/decision-engine/artifacts` — Boxes — `systems.decisionEngine.artifacts.read`
   - Revisión del catálogo — `/review-queue` — SlidersHorizontal — `systems.reviewQueue.read`
3. **Procesos** (Workflow)
   - Procesos — `/procesos` — Workflow — `workflows.read`
4. **Catálogo y metadatos** (Database)
   - Catálogo de datos — `/data-catalog/tables` — Database — `catalog.data.read`
   - Dominios y glosario — `/business-metadata/domains` — BookOpen — `businessMetadata.read`
   - Definiciones del motor — `/business-metadata/definitions` — BookOpen — `operations.definitions.read`
   - Catálogos operativos — `/operations/catalogs` — Database — `operations.catalogs.read`
5. **Linaje** (GitBranch)
   - Linaje — `/lineage` — GitBranch — `lineage.read`
6. **Gobierno y calidad** (ShieldCheck)
   - Consentimientos del cliente — `/settings/consent-documents` — FileText — `governance.policies.read`
   - Contrato de comercios — `/settings/partner-contracts` — FileSignature — rol INTERNAL_PORTAL
   - Contenido de la app — `/settings/app-content` — FileText — `governance.policies.read`
   - Políticas de notificación — `/settings/notification-policies` — FileText — `governance.policies.read`
   - Motor de decisiones — `/settings/decision-artifacts` — ShieldCheck — rol DECISION_ARTIFACT
   - Gobierno de datos — `/governance` — ShieldCheck — `governance.data.read`
   - Solicitudes de privacidad — `/governance/privacy-requests` — ShieldCheck — `privacy.requests.read`
   - Políticas gobierno — `/governance/policies` — ShieldCheck — `governance.policies.read`
   - Política riesgo — `/risk-policy/current` — ShieldCheck — `operations.riskPolicy.read`
   - Incidencias de calidad — `/data-quality/issues` — ClipboardCheck — `dataQuality.issues.read`
   - Reglas calidad — `/data-quality/rules` — ClipboardCheck — `dataQuality.rules.read`
7. **Reportes** (BarChart3)
   - Reportes — `/reports` — BarChart3 — `reporting.read`
   - Preparación de salida — `/release-readiness` — ClipboardCheck — `reporting.read`
8. **QA** (TestTube2)
   - Laboratorio QA — `/qa/lab` — TestTube2 — `systems.endpoints.read`
   - Aprender el laboratorio QA — `/qa/aprender` — GraduationCap — `systems.endpoints.read`
   - Baterías de prueba — `/qa/suites` — ClipboardCheck — `systems.qa.read`
   - Ejecuciones QA — `/qa/runs` — FileClock — `systems.qa.read`
   - Carga QA — `/qa/stress` — Gauge — `systems.stress.read`
9. **Crédito** (Landmark)
   - Productos de crédito — `/operations/credit/products` — Landmark — rol CREDIT_OPS
10. **Operaciones** (ListChecks)
    - Contactos sin verificar — `/operations/pending-contacts` — MailCheck — rol OPERATIONS_CASE
    - Cola de trabajo — `/operations/work-queue` — ShieldAlert — rol WORK_QUEUE
    - Soporte — `/support` — LifeBuoy — nada
    - Base de conocimiento — `/support/knowledge` — BookOpenText — rol SUPPORT_KNOWLEDGE
    - Agentes de soporte — `/support/agents` — UserCog — rol SUPPORT_ADMIN
    - Archivos — `/files` — FolderTree — `expedientes.leer`
    - Vistas del negocio — `/views` — Table2 — rol INTERNAL_PORTAL
    - Usuarios de comercio — `/merchant-users` — Store — `merchant.users.read` y rol INTERNAL_PORTAL
    - Expedientes de comercio — `/operations/partners` — Stamp — rol PARTNER_OPS
    - Calificación de cartera — `/operations/portfolio` — Gauge — rol LOAN_RATING
    - Préstamos — `/operations/loans` — Landmark — rol LOAN_PORTFOLIO
    - Avisos de pago — `/operations/payment-claims` — HandCoins — rol PAYMENT_CLAIMS
    - Eventos de dominio — `/events` — Radio — rol INTERNAL_PORTAL
    - Procesos automáticos — `/jobs` — ListChecks — rol INTERNAL_PORTAL ∪ RUNTIME_JOB
    - Mensajería interna — `/notifications` — MessageSquare — `notifications.messages.read`
    - Campañas — `/notifications/campaigns` — Megaphone — rol CAMPAIGN_READ
11. **Esquema de datos** (Database)
    - Versiones de esquema — `/schema/versions` — Database — nada
    - Historial de cambios del esquema — `/schema/change-log` — History — nada
12. **Proveedores externos** (Plug)
    - Datos del cliente — `/external-data` — DatabaseZap — rol INTERNAL_PORTAL
    - Catálogo y salud — `/external-providers` — Plug — nada
    - Auditorías — `/external-providers/audits` — ShieldAlert — nada
    - Solicitudes — `/external-providers/requests` — Siren — nada
13. **Seguridad y auditoría** (LockKeyhole)
    - Seguridad sesión — `/security/session` — LockKeyhole — nada
    - Registro del sistema — `/audit` — ShieldCheck — `audit.events.read`
14. **Administración** (Settings)
    - Usuarios internos — `/settings/users` — Users — `internal.users.read`
    - Roles internos — `/settings/roles` — ShieldCheck — `internal.roles.read`
    - Permisos internos — `/settings/permissions` — ShieldCheck — `internal.permissions.read`
    - Actualizar inventario — `/settings/catalog-sync` — Settings — cualquiera de `systems.endpoints.discover`, `systems.endpoints.catalogSeedRefresh`, `systems.tools.inferRequirements`
    - Perfil — `/settings/profile` — UserCircle — nada

Todas las rutas del sidebar llevan delante `/internal`.

---

## 1. Inicio

### `/internal` — Centro interno ATLAS (eyebrow "Inicio")
- Desc: "Cómo está la plataforma ahora: catálogo, revisiones pendientes, pruebas, tráfico y, si tienes permiso, la salud de las herramientas." No tiene BusinessContextNote.
- Gate: `systems.dashboard.read`. Las partes de salud requieren `systems.tools.health.read`, incluido `CriticalToolsBanner`.
- Métricas: MetricCards dinámicas, una por cada clave de `dashboard.counts` (label = clave humanizada). La sección "Postura del catálogo" es un `dl` dinámico sobre `posture`.
- Secciones: "Postura del catálogo", "Accesos rápidos", "Tráfico y latencia" (gráfico de serie temporal más tabla), "Herramientas".
  - Accesos rápidos: Ver operaciones, Ver catálogo de datos, Ver linaje, Ver gobierno, Ver dominios, Preparación de salida, Ver baterías de prueba, Ver auditoría.
- Tabla de tráfico: Método, Ruta, Peticiones, Latencia prom., p95, Tasa de error, Última vez. Búsqueda "Buscar por ruta o método…"; filtro Método.
- Tabla de salud de herramientas: Herramienta, Estado en vivo, Catálogo, Crítica, Chequeo, Qué dice. Búsqueda "Buscar por herramienta, código o mensaje…"; filtros Estado en vivo, Criticidad.
- Badges: MethodBadge, ToolLiveBadge, StatusBadge, Badge.
- Acciones: botón "Actualizar" en el header.
- Estados: "Sin tráfico registrado en esta ventana" / "Ninguna ruta coincide…"; "No hay herramientas con chequeo de salud."; "No se pudo cargar el resumen."
- API: GET `/systems/dashboard`, GET `/systems/reports/traffic-latency`, GET `/systems/reports/traffic-latency-timeseries`, GET `/systems/health/tools`.
- Redirecciones: `/` lleva a `/internal`; `/systems/dashboard` lleva a `/internal`.

### `/internal/my-notifications` — Mis notificaciones (eyebrow "Autoservicio")
- Nota: a diferencia de "Mensajería interna" (todos los mensajes de la organización, requiere permiso de administración), aquí solo salen las notificaciones dirigidas al usuario. Sin gate.
- Columnas: Fecha, Canal, Categoría, Notificación, Estado.
- Filtros: búsqueda "Buscar en el título o el texto…"; Estado, Canal.
- Badges: NotificationChannelBadge, NotificationCategoryBadge, NotificationStatusBadge.
- Acciones: "Marcar todas como leídas"; marcar leída por fila.
- Vacío: "No tenés notificaciones."
- API: GET `/internal-users/me/notifications`, GET `…/unread-count`, POST `…/{id}/read`, POST `…/read-all`.

### `/internal/search` — Búsqueda global (eyebrow "Búsqueda")
- Desc: busca en el catálogo técnico: operaciones, tablas, reglas de calidad, etc. Sin gate.
- Métricas: Total, más un conteo por tipo.
- Pestañas (DetailTabs): una por tipo de resultado.
- Columnas: Tipo, Resultado, Detalle, Método, Riesgo, Estado, Datos personales, Abrir.
- Badges: MethodBadge, RiskBadge, StatusBadge, PiiBadge.
- Vacíos: "Escribe una búsqueda desde la barra superior." / "Sin resultados de este tipo".
- API: GET `/internal/search`.

---

## 2. Sistemas

### `/systems/endpoints` — Catálogo de operaciones
- Desc: "Las operaciones de todos los sistemas de Atlas, tal como están registradas." Gate: `systems.endpoints.read`.
- Columnas: Bloque, Método, Ruta, Módulo, Servicio, Pruebas, Riesgo, Datos personales, Pide sesión, Se prueba aquí, Prueba de carga, Revisión, Estado, Actualizado.
- Filtros: búsqueda "Buscar por ruta, módulo, propósito o nombre…"; Bloque, Riesgo, Revisión.
- Badges: BlockBadge, MethodBadge, ModuleBadge, RiskBadge, PiiBadge, ReviewStatusBadge, StatusBadge.
- Acciones: botón de exportación "Descargar JSON". Enlaces de fila al detalle y a `/qa/lab?endpointId=`.
- Vacío: "Ninguna ruta coincide con la búsqueda o los filtros."
- API: GET `/systems/endpoints`.

### `/systems/endpoints/[endpointId]` — título = nombre de ruta o handler
- Gates: `systems.endpoints.read`; la prueba de carga requiere `systems.stress.execute`; la prueba funcional requiere `systems.endpoints.execute`.
- Pestañas: Resumen, Contratos, Tablas impactadas, Campos impactados, Herramientas, Pruebas QA. Dentro de Pruebas QA: Prueba funcional y Prueba de carga.
- Columnas por tabla:
  - Tablas: Tabla, Operación, Impacto, Principal, Auditoría, Revisión.
  - Campos: Campo, Requerido, Sensible, Validación.
  - Herramientas: Herramienta, Uso, Requerida, Necesita simulador.
  - Contrato: Carga, Tipo / valor, Descripción (búsqueda "Buscar por campo, tipo o descripción…").
  - Resultados QA: Comprobación, Obtenido, Esperado, Resultado.
- Acciones: Probar operación; Ver en el mapa de rutas; Previsualizar carga / Lanzar carga real; Previsualizar petición / Enviar petición real; Volver a los valores por defecto; Descargar el registro; copiar logs; Usar el ejemplo; generar casos.
  - Diálogos de confirmación: "Confirmar previsualización" / "Confirmar carga|envío".
- Vacíos: "NO IMPACTA NINGUNA TABLA", "NO IMPACTA NINGÚN CAMPO", "NO REQUIERE HERRAMIENTAS", "Contrato pendiente.", "La prueba no evaluó ninguna comprobación."
- API: GET `/systems/endpoints/{id}`, GET `/systems/impact/by-endpoint/{id}`, más los endpoints de ejecución de QA.

### `/flows` — Mapa de rutas (eyebrow "Sistemas")
- Desc: "Una fila por operación de la plataforma, sacada del código: qué puede hacer cada usuario, con qué autorización, y qué le falta…"
- Gates: `systems.flows.read`; la acción de verificar requiere `systems.flows.analyze`.
- Métricas: Flujos, Críticos, Rotos, Desactualizados, Escrituras públicas, Críticos sin pruebas.
- Tabla de flujos: Ruta, Flujo, Sistema, Módulo, Riesgo, Autorización, Quién la llama, Señales.
  - Búsqueda "Buscar por nombre, ruta o módulo…".
  - Filtros: Sistema, Módulo, Riesgo, Verificación, Al día, Cliente, Pruebas, Hallazgos.
- Tabla "Hallazgos": Severidad, Tipo, Dónde, Detalle.
  - Búsqueda "Buscar por ruta, módulo o detalle…"; filtros Severidad, Tipo.
- Badges: MethodBadge, BlockBadge, RiskBadge, SeverityBadge, Badge.
- Acciones: "Verificar con corridas" / "Verificando…" en el header; "Reintentar resumen". Los enlaces de fila llevan a `/flows/graph?flow=` y `?systemCode=&module=`. El drawer de detalle de flujo tiene enlace a endpoints.
- Vacíos: "No se encontraron flujos para los filtros seleccionados", "Sin hallazgos abiertos", "El mapa de flujos no está cargado en este entorno".
- API: GET `/systems/flows`, POST `/systems/flows/verify`, GET `/systems/flows/findings`, GET `/systems/flows/{id}`.

### `/flows/graph` — "Diagrama de la operación" o "Diagrama del módulo {módulo}"
- Gate: `systems.flows.read`. No está en el sidebar.
- Diagrama con búsqueda "Buscar un recuadro y centrarlo…".
- Acciones: "Mostrar roles" / "Ocultar roles", "Volver a la tabla". Drawer de detalle de operación.
- Badges: Method, Block, Risk, Severity.
- Errores: "No se pudo cargar el diagrama.", "No se pudo calcular el layout."
- API: GET `/systems/flows/{id}/graph`, GET `/systems/flows/graph`.
- Redirección: `/flows/business` lleva a `/procesos` (conserva `?flow=`).

### `/flows/pending-work` — Trabajo pendiente
- Desc: lo que cada flujo deja encolado al responder, si alguien lo recoge y qué eventos acaban en un aviso. Gate: `systems.flows.read`.
- Métricas: Diagnóstico, Pendientes, Fallidos, Última pasada del consumidor.
- Tabla "Flujos que encolan": Flujo, Eventos, Pendientes, Fallidos, Pendiente desde, Último procesado, Consumidor, Desenlace.
  - Búsqueda "Buscar por método, ruta o evento…"; filtros Situación, Desenlace.
- Tabla "Eventos de dominio: quién los consume": Evento, Agregado, Procesados, Con mensaje, Mensajes, Salieron, Registrado, Último.
  - Búsqueda "Buscar por evento o agregado…"; filtro Registro.
- Vacíos: "Ningún flujo encoló trabajo…", "Sin eventos de dominio en la ventana".
- API: GET `/systems/flows/pending-work`.

### `/flows/rbac-drift` — Deriva de permisos
- Desc: dónde el menú y la operación no piden lo mismo, y qué permisos se exigen sin existir. Gate: `systems.flows.read`.
- Métricas: Averías que dejan fuera a alguien, Permisos que la base no tiene, Menús que piden otro permiso, Llamadas sin guarda.
- Columnas: Qué pasa, Pantalla, Operación, Qué hacer.
- Filtros: búsqueda "Buscar por pantalla, operación o permiso…"; Qué pasa (severidad), Cliente. La fila enlaza a `/flows?flow=`.
- API: GET `/systems/flows/rbac-drift`.

### `/flows/review` — Revisión de análisis de flujos
- Desc: flujos de riesgo alto cuyo análisis no se puede dar por bueno solo; aprobar un flujo es aprobar ese código.
- Gates: `systems.flows.read`; decidir requiere `systems.flows.review`.
- Columnas: (checkbox de selección), Riesgo, Flujo, Por qué, Estado, Decisión.
- Filtros: búsqueda "Buscar por nombre, ruta o módulo…"; Estado de revisión.
- Acciones: por fila "Ver flujo" y aprobar/rechazar. Barra masiva: "Aprobar seleccionados", "Rechazar seleccionados", "Limpiar selección". Drawer de flujo.
- Vacío: "Nada que revisar con este estado".
- API: GET `/systems/flows/review-queue`, PATCH `/systems/flows/{id}/review`.

### `/flows/gate` — Compuerta de documentación
- Desc: antes de certificar: flujos críticos verificados, sin escrituras desprotegidas ni deriva grave, cola de revisión… Gate: `systems.flows.read`.
- Métricas: Resultado, Comprobaciones que fallan, Sin medir, Evaluada.
- Columnas: Comprobación, Qué comprueba, Cantidad, Estado (CheckBadge).
- Filtros: búsqueda "Buscar por código o por lo que comprueba…"; Estado.
- Vacío: "La compuerta no devolvió comprobaciones." Error: "No se pudo evaluar la compuerta."
- API: GET `/systems/flows/documentation-gate`.

### `/systems/tools` — Herramientas internas (eyebrow "Herramientas")
- Desc: servicios técnicos de los que depende la plataforma; no muestra variables de entorno ni secretos.
- Gate: `systems.tools.read` o `systems.tools.health.read`. Cada pestaña se ve según su permiso.
- Pestañas: Catálogo, Salud (`?tab=salud`).
- Pestaña Catálogo: Código, Nombre, Tipo, Proveedor, Crítica, Credenciales, Sandbox, Estado.
  - Búsqueda "Buscar por código, nombre, proveedor o tipo…"; filtro Estado.
- Pestaña Salud: Herramienta, Estado en vivo, Catálogo, Crítica, Chequeo, Qué dice.
  - Filtros Estado en vivo, Criticidad; botón "Actualizar".
- Badges: StatusBadge, ToolLiveBadge.
- Vacíos: "Ninguna herramienta cumple estos filtros", "No hay herramientas con chequeo de salud."
- API: GET `/systems/tools`, GET `/systems/health/tools`.
- Redirección: `/systems/tools/health` lleva a `?tab=salud`.

### `/systems/tools/[toolId]` — título = nombre de la herramienta
- Gate: `systems.tools.read`. Ficha más notas de gobierno; StatusBadge.
- Error: "No se pudo cargar herramienta."
- API: GET `/systems/tools/{id}`.

### `/systems/network-health` — Salud de la red
- Desc: si los tres sistemas (núcleo, motor de decisiones, ERP) responden y entregaron su lista de rutas y tablas.
- Nota: "Salud de herramientas" mira piezas sueltas; esta pantalla mira si Atlas está completo. Gate: `systems.network.read`.
- Métricas (estado del servidor): RAM disponible, Disco, Carga (15 min), Caché de build, Copia de bases.
- Tabla de sistemas: Sistema, Estado en vivo, Operaciones, Tablas, Catálogo, Si falta.
  - Búsqueda "Buscar por sistema, propósito o mensaje…"; filtros Estado en vivo, Catálogo.
- Tabla de hosts: Aplicación, Principal, Respaldo, Memoria.
- Badges: HealthBadge.
- Acciones: "Actualizar catálogos" (federate), "Actualizar". Las filas enlazan a endpoints y tablas filtrados por `?block=`.
- Vacío: "No hay sistemas en la red."
- API: GET `/systems/health/network`, POST `/systems/blocks/federate`, GET `/systems/monitor/host`.

### `/systems/decision-engine/artifacts` — Artefactos activos del motor
- Desc: todo lo que el motor tiene publicado y en uso.
- Nota: "¿Qué política decidió este crédito?" — cada fila es un despliegue activo. Gate: `systems.decisionEngine.artifacts.read`.
- Columnas: Artefacto, Tipo, Versión desplegada, Ambiente, Despliegue, Tráfico (traffic split), Vigente desde, Desplegado por, Equipo.
- Filtros: búsqueda "Buscar por código, nombre o equipo…"; Ambiente.
- Acción: "Actualizar".
- API: GET `/systems/decision-engine/artifacts`.

### `/review-queue` — Revisión del catálogo (eyebrow "Sistemas")
- Desc: confirma o descarta lo que detectó el escáner (rutas, tablas, columnas, impactos, herramientas); cada decisión guarda su motivo.
- Nota: el catálogo se llena escaneando código y puede equivocarse; no es la revisión de flujos.
- Gates: `systems.reviewQueue.read`; resolver requiere `systems.reviewQueue.resolve`.
- Una sección por familia, cada una con su tabla:
  - Rutas: Método, Ruta, Módulo, Riesgo, Responsable.
  - Columnas: Columna, Nombre de negocio, Tipo, Datos personales.
  - Impactos: Impacto, Operación, Tabla, Nivel.
  - Campos: Campo, Sensible.
  - Herramientas: Requirement, Tool, Uso, Requerida.
  - Todas añaden Revisión y Acciones.
- Filtros: búsqueda "Buscar ruta, tabla, columna, campo o herramienta…"; Tipo, Estado revisión.
- Badges: PiiBadge, ReviewStatusBadge, RiskBadge, MethodBadge, ModuleBadge.
- Acciones: "Actualizar". Diálogo de decisión con motivo (placeholder "Ej.: la columna guarda el número de carnet…"): Aprobar, Marcar revisión, Rechazar, Cancelar.
- Vacío: "Nada que revisar aquí" / "Nada coincide con la búsqueda".
- API: GET `/systems/review-queue`, más el POST de decisión.

---

## 3. Procesos

### `/procesos` — Procesos de Atlas (eyebrow "Procesos")
- Desc: cada proceso de negocio de principio a fin: para qué existe, quién lo empieza y lo cierra, qué pasa si falla, si cada paso humano tiene pantalla.
- Gate: `workflows.read`.
- Métricas: Procesos, Documentados, Totalmente cableados, Pasos sin pantalla.
- Columnas: Proceso, Tipo, Prioridad, Dueño, Dónde ocurre, Etapas, Operaciones, Documentación, Pantallas, Abrir.
- Filtros: búsqueda "Buscar por nombre o número de proceso…"; Tipo, Bloque, Prioridad, Estado.
- Enlaces: detalle y `?tab=casos`.
- Vacío: "Ningún proceso cumple estos filtros" / "Todavía no hay procesos declarados".
- API: GET `/internal/processes`.

### `/procesos/[code]` — título = nombre del proceso
- Gates: `workflows.read`; los enlaces al mapa requieren `systems.flows.read`.
- Pestañas: Resumen, Pasos y flujos, Casos en curso, Documentación y cableado.
- Métricas: Pasos, Con flujo en el mapa, Críticos, Verificados.
- Secciones: "El proceso en cinco preguntas", "Etapas y pasos", "Documentación", "Versión y fuentes", "Avance del caso".
- Tabla de pasos: Etapa, Quién actúa, Pantalla, Paso, Cableado, Riesgo y prueba, Cómo se hace, Bloque, Quién la llama, Mapa de rutas.
  - Búsqueda "Buscar por etapa, paso, descripción o ruta…"; filtro Estado.
- Tabla de documentación: Comprobación, Estado, Qué se mira.
- Tabla de casos: Caso, Situación, Avance (búsqueda "Buscar un caso por su código o identificador…").
- Badges: WiringBadge, Method, Risk, Status, Block, Severity.
- Vacíos: "Este proceso no existe", "Este proceso no declara etapas.", "No hay casos con estos filtros", "Abre un caso para ver su avance".
- API: GET `/internal/processes/{code}`, `/wiring`, `/instances`, progreso de instancia.
- Redirección: `/procesos/[code]/instancias` lleva a la pestaña de casos (conserva `?caso=`).

---

## 4. Catálogo y metadatos

### `/data-catalog/tables` — Catálogo de datos
- Desc: "Las tablas de datos de los tres bloques de Atlas: núcleo, motor de decisión y ERP."
- Nota: cada fila es una tabla real; sirve para que soporte, auditoría y el equipo técnico sepan qué significa, quién responde y si tiene datos sensibles.
- Gate: `catalog.data.read`.
- Columnas: Bloque, Schema, Tabla, Entidad, Módulo, Responsable, Datos personales, Financiera, Riesgo, Crítica para auditoría, Retención, Revisión, Estado.
- Filtros: búsqueda "Buscar tabla, esquema, entidad, modelo, módulo o responsable…"; Bloque, Revisión.
- Exportación: "Descargar JSON". La fila enlaza al detalle.
- API: GET `/systems/blocks`, GET `/systems/data-entities`.
- Redirecciones: `/exports` lleva aquí; `/exports/[id]` lleva al destino del export.

### `/data-catalog/tables/[entityId]` — "{schema}.{tabla}" (eyebrow "Catálogo de datos")
- Gates: `catalog.data.read`; revisar columnas requiere `systems.reviewQueue.resolve`.
- Pestañas: Vista general, Columnas, Operaciones, Gobierno operativo.
- Métricas: Columnas, Datos personales, Uso ML, Descritas, Aprobadas.
- Tabla de columnas: Columna, Tipo, Descripción, Nullable, Datos personales, Uso ML, Validación, Nombre de negocio, Nulos, Revisión, Acciones.
  - Búsqueda "Buscar columna, tipo o descripción..."; filtros Datos personales, Uso ML, Revisión.
- Tabla de operaciones: Operación, Afectación, Impacto, Principal, Auditoría, Revisión.
- Badges: AffectationBadge, PiiBadge, ReviewStatus, Risk.
- Acciones: "Editar metadatos" (lleva a `/metadata`); "Revisar" por columna, que abre el diálogo "Revisar columna X" con "Registrar revisión".
- Vacíos: "Todavía no se sabe qué operaciones usan esta tabla.", "Catálogo de columnas pendiente."
- API: GET `/systems/data-entities/{id}`, GET `/systems/impact/by-table/{schema}/{table}`.

### `/data-catalog/tables/[entityId]/metadata` — Configurar tabla (eyebrow "Formulario de catálogo")
- Desc: edita la ficha (propósito, responsable, retención, estado, clasificación); es documentación.
- Gate: `catalog.data.manage` y `systems.dataEntities.updateMetadata`.
- Formulario con secciones "Ficha de negocio" y "Clasificación de los datos" (campos booleanos).
- Acciones: "Volver al detalle", "Guardar ficha" (confirmación "Guardar ficha de la tabla").
- API: PATCH `/systems/data-entities/{id}/metadata`.

### `/business-metadata/domains` — Dominios y glosario (eyebrow "Metadata de negocio")
- Nota: Atlas se divide en dominios (onboarding, riesgo, cobranza, cumplimiento…); responde "¿qué parte del negocio toca esta ruta o tabla?".
- Gate: `businessMetadata.read`.
- Pestañas (UrlTabs `?tab`): Dominios, Términos.
- Pestaña Dominios:
  - Métricas: Dominios, Operaciones, Tablas, Baterías de prueba.
  - Tabla "Tablas sin dominio": Módulo, Tablas sin dominio (búsqueda "Buscar por módulo…").
  - Tabla "Resumen por dominio": Dominio, Revisión, Descripción, Operaciones, Tablas, Baterías de prueba, Datos personales, Críticos, En revisión, Módulos, Ver.
  - Búsqueda "Buscar dominio, nombre, descripción o módulo…"; filtros Revisión, Datos personales.
- Pestaña Términos:
  - Métricas: Términos que cumplen el filtro, Tablas en el glosario, Campos en el glosario, Dominios con términos.
  - Columnas: Término, Clave, Tipo, Dominio, Dueño, Estado, Definición, Columnas, Actualizado.
  - Búsqueda "Buscar término, clave, definición, dominio o dueño…"; filtros Tipo, Dominio.
- API: GET `/systems/domains/overview`, GET `/internal/business-metadata/glossary`, GET `…/glossary/facets`.
- Redirección: `/business-metadata/glossary` lleva a `?tab=terminos`.

### `/business-metadata/glossary/[termId]` — título = nombre del término (eyebrow "Glosario")
- Gate: `businessMetadata.read`.
- Secciones: Definición; Relaciones (Tipo, Origen, Destino tipo, Destino); Auditoría (Acción, Actor, Fecha).
- Vacíos: "Sin relaciones registradas.", "Sin eventos de auditoría registrados."
- API: GET `/internal/business-metadata/terms/{id}`.

### `/business-metadata/definitions` — Definiciones del motor (eyebrow "Motor de decisión")
- Desc: vocabulario del motor: eventos, observaciones, atributos e indicadores calculados.
- Nota: antes de usar una señal, tiene que estar definida.
- Gate: `operations.definitions.read`.
- Métricas: Eventos, Observaciones, Atributos, Features.
- Columnas: Tipo, Código, Nombre, Familia/alcance, Dato, Riesgo, Flags, Dominio, Dueño, Tablas, Revisión, Activo.
- Filtros: búsqueda "Buscar por código o nombre…"; Tipo, Estado.
- Vacío: "No hay definiciones para los filtros aplicados."
- API: GET `/operations/definitions`.

### `/business-metadata/definitions/package` — Publicar paquete de definiciones
- Desc: crea o actualiza en lote eventos, observaciones, atributos e indicadores de un dominio.
- Gate: `businessMetadata.read`.
- Editor JSON con secciones "Paquete" y "Qué se va a enviar".
- Acciones: "Ver definiciones", "Validar paquete", "Restaurar plantilla", "Publicar paquete" (con confirmación).
- API: POST `/operations/definitions/package`.

### `/operations/catalogs` — Catálogos operativos (eyebrow "Catálogos")
- Desc: listas de valores de las reglas: versión, dueño y estado de aprobación.
- Nota: si un catálogo tiene versión sin aprobar o sin dueño, una regla puede fallar.
- Gate: `operations.catalogs.read`.
- Métricas: Catálogos, Activos, Publicados, Sin versión. Panel de staging: Pendientes, Aprobados, Rechazados, Sugeridos por IA.
- Tabla "Inventario": Código, Catálogo, Dominio, Dueño, Activo, Versión, Estado, Acciones.
  - Búsqueda "Buscar por código, nombre, dominio o dueño…"; filtros Estado versión, Activo.
- Staging: Ítem propuesto, Propuesta, Aviso.
  - Búsqueda "Buscar por código, nombre o n.º del ítem…"; filtro Propuesta.
- Acciones:
  - "Nueva versión" abre el diálogo "Nueva versión de X". Dentro: Agregar elemento, Quitar elemento, Agregar alias, Agregar mapeo, Quitar mapeo, Crear borrador.
  - "Ingerir" abre "Ingerir valores a X", con "Ingerir lote".
  - Staging: "Aprobar seleccionados (n)", "Rechazar seleccionados (n)", "Ver todo el catálogo / Ver sólo esta ingesta".
- API: GET `/operations/catalogs`, POST `/operations/catalogs/{code}/versions`, POST `/operations/catalog-ingestions`, GET `/operations/catalog-staging-items`, POST `…/decision-batch`.

### `/operations/catalogs/[catalogCode]/versions/[versionId]` — "Versión {code}"
- Nota: borrador → aprobación → publicación; solo la versión publicada alimenta reglas.
- Gates: `operations.catalogs.read`; aprobar requiere el rol SUPER_ADMIN.
- Métricas: Items, Alias, Mapeos de riesgo, Estado.
- Secciones: "Ficha de la versión", "Elementos de la versión", "Atributos".
- Columnas:
  - Ítems: Código, Nombre, Tipo, Confianza, Alias, Mapeos de riesgo, Activo (búsqueda "Buscar por código, nombre o tipo del elemento…"; filtro Activo).
  - Alias: Valor, Normalizado.
  - Mapeos: Dimensión, Banda, Puntos, Motivo, Uso, Vigencia, Explicación.
- Acciones: "Volver a catálogos", "Enviar a aprobación" (diálogo), diálogo de decisión aprobar/rechazar, drawer "Elemento X".
- API: GET `/operations/catalogs/{c}/versions/{v}`, POST `…/submit-for-approval`, POST `…/decision`.

---

## 5. Linaje

### `/lineage` — Linaje (eyebrow "Linaje")
- Desc: qué rutas leen o escriben cada tabla y cómo se relacionan las tablas.
- Nota: antes de cambiar o borrar una ruta o tabla, saber qué más se rompe.
- Gate: `lineage.read`.
- Pestañas (UrlTabs `?vista`): Grafo, Nodos, Relaciones e impacto, Mapa por dominio.
- Métricas: Dominios, Rutas, Tablas, Tablas con datos personales, Aristas, Operación → tabla, Tabla → tabla, Críticas o altas, Nodos en el grafo, Generado.
- Grafo: interruptor Grafo/Tabla y drawer de nodo. Tabla de nodos: Nodo, Tipo, Dominio, Criticidad, Estado; filtro Tipo de nodo.
- Impacto: Origen, Destino, Detalle, Relación, Operación, Severidad, Descripción.
  - Búsqueda "Buscar origen, destino, operación, tabla o descripción…"; filtros Relación, Severidad, Módulo.
- Nodos-tablas: Tabla, Financiera, Riesgo, Legal, Datos personales (búsqueda "Buscar tabla, esquema, entidad, módulo, responsable o ruta…").
- Nodos-rutas: Operación, Impacto, Carga (búsqueda "Buscar tabla, esquema, ruta, nombre de ruta o módulo…").
- Mapa por dominio: igual que la tabla de dominios.
- API: GET `/internal/lineage`, `/internal/lineage/impact`, `/systems/domains/overview`, `/systems/data-entities`, `/systems/endpoints`.
- Redirecciones: `/lineage/impact` lleva a `?vista=impacto`; `/lineage/official` lleva a `?vista=grafo`.

### `/lineage/nodes/[nodeId]` — etiqueta del nodo
- Desc: "Qué le llega, qué sale de él y sus metadatos." Gate: `lineage.read`.
- Columnas: Nodo, Tipo, Dominio, Criticidad, Estado, Origen, Destino, Detalle, Relación, Operación, Severidad, Descripción. Enlaces a otros nodos.
- API: GET `/internal/lineage/nodes/{id}`.

### `/lineage/table/[schemaName]/[tableName]` — "{schema}.{tabla}" (eyebrow "Linaje por tabla")
- Gate: `lineage.read`.
- Secciones: "Operaciones que afectan esta tabla", "Detalle técnico del linaje".
- Columnas: Operación, Impacto, Principal, Transaccional, Rollback, Cliente, Financiero, Riesgo, Legal, Auditoría, Revisión.
- Vacío: "No hay impactos registrados para esta tabla."
- API: GET `/systems/impact/by-table/{s}/{t}`.

---

## 6. Gobierno y calidad

### `/settings/consent-documents` — Documentos de consentimiento (eyebrow "Gobierno y calidad")
- Desc: lo que el cliente acepta al registrarse; se edita aquí y llega a la app sin desplegar. Editar requiere `governance.policies.manage`.
- Métricas: Documentos, Vigentes, Borradores, Retirados.
- Columnas: Documento, Versión, Idioma, Estado, Vigencia, Resumen, Texto, Acciones.
- Filtros: búsqueda "Buscar por código, título o resumen…"; Estado.
- Acciones: "Editar texto" abre un editor con Guardar y Cancelar.
- Vacío: "Todavía no hay documentos publicados".
- API: GET `/operations/consent-documents`, PATCH `/operations/consent-documents/{id}`.

### `/settings/partner-contracts` — Contrato de afiliación (eyebrow "Comercios")
- Desc: el texto bajo el que opera un comercio sin contrato propio; se publica por versiones.
- Nota: verificar que un comercio existe no es tener algo firmado.
- Gates: RoleGate INTERNAL_PORTAL; las acciones requieren `governance.policies.manage`.
- Métricas: Contrato vigente, Versiones publicadas, En vigor desde.
- Columnas: Contrato, Versión, Estado, En vigor desde, Texto, Acciones.
- Filtros: búsqueda "Buscar por código o nombre del contrato…"; Estado.
- Acciones: "Publicar una versión" (formulario con Publicar y Cancelar); "Ver texto" (diálogo); "Marcar como vigente" (diálogo "Marcar como contrato vigente").
- API: base `/operations/partner-contract-templates` (GET, POST, PATCH `/{id}/default`).

### `/settings/app-content` — Contenido de la app (eyebrow "Gobierno y calidad")
- Desc: todo lo que el cliente lee y no es un dato suyo; llega a la app sin publicar versión. Las acciones requieren APP_CONTENT_MANAGE.
- Hay un selector de pantalla y una vista previa en forma de teléfono.
- Métricas: "Piezas en {pantalla}", Visibles en la app, Ocultas.
- Columnas: Pieza, Orden, Texto, Botón, Estado, Acciones.
- Filtros: búsqueda "Buscar por título, clave o texto…"; Visibilidad.
- Acciones: "Nueva pieza en …", Publicar, Editar, Guardar, Cancelar, "Añadir punto".
- API: GET y PUT `/operations/app-content`.

### `/settings/notification-policies` — Políticas de notificación
- Desc: qué avisos manda Atlas, cómo se llaman en la app y cuáles son irrenunciables. Gate: `governance.policies.manage`.
- Métricas: Políticas, Irrenunciables, Activas, Inactivas.
- Columnas: Código, Nombre, Categoría, Canal, Obligatoria, Activa, Acciones.
- Filtros: búsqueda "Buscar por código, nombre, categoría o explicación…"; Categoría, Canal, Obligatoria, Estado.
- Acciones: Editar, Guardar, Cancelar.
- API: GET y PUT `/operations/notification-policies`.

### `/settings/decision-artifacts` — Motor de decisiones (eyebrow "Configuración")
- Desc: las decisiones que Atlas delega en el motor: qué política resuelve cada una, quién la llama y en qué punto. Sin gate de página (solo el rol del menú).
- Columnas: Decisión, Artefacto que la resuelve, En el motor, Versión, Flujo de trabajo, Origen.
- Filtros: búsqueda "Buscar por decisión, artefacto o flujo de trabajo…"; Origen, En el motor.
- Vacío: "Atlas no delega ninguna decisión en el motor."
- API: GET `/internal/decision-artifacts`.

### `/settings/decision-artifacts/[decisionType]` — título = nombre de la decisión
- Formulario de enlace con "Guardar".
- Tabla de consumidores: Operación, Para qué la llama (búsqueda "Buscar por método, ruta o para qué se llama…").
- Vacío: "Ninguna operación dispara esta decisión."
- API: GET y POST `/internal/decision-artifacts`.

### `/governance` — Gobierno de datos
- Desc: cuántas tablas y rutas manejan datos personales, financieros o de riesgo y cuántas siguen sin revisar.
- Nota: obligaciones legales y de seguridad.
- Gate: `governance.data.read`.
- Pestañas (`?tab`): Resumen, Datos personales.
- Resumen:
  - Métricas: Tablas con datos personales, Tablas financieras, Tablas de riesgo, Pendientes de revisión.
  - Secciones: "Clasificación de tablas", "Riesgo operativo por ruta", "Registros de gobierno disponibles" (enlaces a catálogo, review-queue, audit).
- Datos personales:
  - Métricas: Con datos personales, Destructivos, Alto/crítico.
  - Tablas: Tabla, Dominio, Datos personales, Legal, Dispositivo, Ubicación, Retención, Estado. Rutas: Ruta, Riesgo, Campos personales, Pide sesión.
  - Búsqueda "Buscar tabla, esquema, entidad, módulo, responsable o ruta…".
- API: GET `/systems/catalog/summary`, `/systems/data-entities`, `/systems/endpoints`.
- Redirección: `/governance/pii` lleva a `?tab=datos-personales`.

### `/governance/privacy-requests` — Solicitudes de privacidad (eyebrow "Gobierno de datos")
- Desc: pedidos para ver, corregir, llevarse, limitar o borrar datos; 15 días naturales de plazo.
- Nota: tomar una solicitud te hace responsable; cerrarla exige escribir cómo se resolvió.
- Gate: `privacy.requests.read`.
- Métricas: Abiertas, Vencidas, Plazo legal.
- Columnas: Solicitud, Cliente, Recibida, Plazo, Responsable, Motor, Estado.
- Filtros: búsqueda "Código de solicitud o de cliente…"; Estado, Derecho, Plazo.
- Badges: PrivacyStatusBadge, EngineDecisionBadge.
- Vacío: "No hay solicitudes para estos filtros."
- API: GET `/operations/privacy/data-subject-requests`.

### `/governance/privacy-requests/[requestId]` — título = tipo de derecho
- Gates: lectura de privacidad; transiciones con PRIVACY_MANAGE.
- Ficha con sección "Historial"; los valores cifrados muestran un error si el sobre no se puede leer.
- Diálogo de transición (tomar, atender, rechazar) con resolución y Cancelar.
- API: GET `…/data-subject-requests/{id}`, POST `…/{id}/transition`.

### `/governance/policies` — Políticas de gobierno
- Desc: propósitos, retenciones, clasificaciones, campos sensibles y reglas de calidad en una lista. Gate: `governance.policies.read`.
- Métricas: Políticas, Campos sensibles, Consentimiento explícito, Clases protegidas.
- Columnas: Tipo, Código, Nombre, Alcance, Control, Estado.
- Filtros: búsqueda "Buscar por código, nombre o alcance…"; Tipo. La fila enlaza al detalle.
- API: GET `/operations/data-governance/policies/search`.

### `/governance/policies/[policyId]` — título = nombre de la política
- Gate: `governance.policies.read`.
- Secciones: "Alcance de datos" (Tipo, Activo); "Configuración operativa"; controles (Control, Estado, Regla); acciones (Acción, Operación, Activa); aprobaciones (Aprobación, Motivo); auditoría (Actor, Fecha, Comentario).
- Acción: "Ver configuración".
- API: GET `/internal/governance/policies/{id}`.

### `/governance/policies/[policyId]/configure` — Configuración de la política
- Gate: `governance.policies.manage`. Resumen de append only, borrado, retención, masking, auditoría y aprobaciones.
- Acción: "Volver al detalle".

### `/governance/policies/package` — Publicar paquete de gobernanza
- Gate: `governance.policies.read`. Editor de paquete (propósitos, retenciones, proveedores, clasificaciones, campos sensibles, reglas de calidad).
- Acciones: "Ver políticas", "Validar paquete", "Restaurar plantilla", "Publicar paquete".
- API: POST `/operations/data-governance/policy-package`.

### `/risk-policy/current` — Política de riesgo actual
- Desc: la política LOCAL vigente; es la red de seguridad si el Motor no responde. Gate: `operations.riskPolicy.read`.
- Métricas: Modelos, Rulesets, Reglas, Señales.
- Tabla de rulesets: Ruleset, Estado, Desde, Hasta, Reglas (búsqueda "Buscar versión de ruleset…").
- Tabla "Reglas de política": Regla, Código, Dimensión, Tipo, Severidad, Acción, Hard stop, Estado.
  - Búsqueda "Buscar por regla, código o ruleset…"; filtros Dimensión, Severidad, Hard stop.
- API: GET `/operations/risk-policy/current`.

### `/risk-policy/ruleset-versions/new` — "Las versiones de política se crean en el Motor"
- Gate: `operations.riskPolicy.read`. Página informativa con el botón "Ver la política local vigente" y enlace a decision-artifacts.

### `/data-quality/issues` — Incidencias de calidad (eyebrow "Calidad de datos")
- Desc: registros que no cumplen una regla; reconócelos, corrígelos o descártalos, siempre con motivo.
- Gates: `dataQuality.issues.read`; resolver requiere `dataQuality.issues.resolve`.
- Métricas: Pendientes, Sin revisar, Reconocidas sin corregir, Cerradas.
- Tabla "Bandeja de calidad": Incidencia, Estado, Severidad, Tabla, Registro, Detectada, Notas, Acciones.
- Filtros: búsqueda "Buscar por tabla, código de regla o notas…"; Estado, Severidad.
- Acciones: por fila Resolver o Cerrar; diálogo de resolución (Reconocer, corregir, descartar, con motivo y notas; "Procesando…", Cancelar).
- Vacíos: "No hay incidencias de calidad registradas." / "No hay incidencias para los filtros actuales."
- API: GET `/operations/data-quality/issues`, POST `…/{id}/resolve`.
- Redirección: `/alerts` lleva aquí.

### `/data-quality/rules` — Reglas de calidad
- Gate: `dataQuality.rules.read`.
- Métricas: Reglas, Activas, Críticas, Incidencias pendientes.
- Tabla "Inventario de reglas": Regla, Código, Objetivo, Tipo, Severidad, Estado, Definición actualizada, Pendientes.
- Filtros: búsqueda "Buscar por código, nombre, tabla o campo…"; Severidad, Estado.
- Exportación: "Descargar JSON".
- API: GET `/internal/data-quality/rules`.

### `/data-quality/rules/[ruleId]` — Detalle de regla
- Gate: `dataQuality.rules.read`. Secciones "Acción esperada" y "Ejecución" (tarjeta de ejecución).
- API: GET `/internal/data-quality/rules/{id}`.

---

## 7. Reportes

### `/reports` — Reportería dinámica
- Desc: los informes que Atlas sabe calcular; están en el código del servidor y se calculan en vivo. Gate: `reporting.read`.
- Métricas: Informes, Activos, Críticos o altos.
- Tabla "Definiciones disponibles": Reporte, Código, Dominio, Dueño, Criticidad, Fuente, Estado, Actualizado.
- Filtros: búsqueda "Buscar informe, clave, descripción, dominio, dueño o fuente…"; Dominio, Estado.
- API: GET `/internal/reports`.

### `/reports/[reportId]` — título = nombre del informe
- Gates: `reporting.read`; calcular requiere `reporting.execute`.
- Secciones: "Qué calcula" (Apartado, Qué muestra; búsqueda "Buscar por apartado o descripción…") y "Calcular el informe" (parámetros, botón "Calcular" / "Calculando…").
- API: GET `/internal/reports/{id}`, POST `…/run`.

### `/release-readiness` — Preparación para publicar (eyebrow "Preparación")
- Incluye además la vista "Preparación para salir" (eyebrow "Reporterías"). Gate: `reporting.read`.
- Métricas:
  - Release: Puntuación de preparación, Estado, Bloqueantes, Advertencias.
  - Reportería: Cobertura de tablas, Cobertura de operaciones, Cobertura de pruebas, Baterías de prueba activas, Tablas financieras o de riesgo, Operaciones disponibles, Tablas disponibles.
- Secciones: Checklist de release (Comprobación, Estado, Detalle; búsqueda "Buscar por comprobación o detalle…"; filtro Estado), Acciones recomendadas, Metadatos listos para reportes, Sistemas listos para reportes, Fuentes candidatas, Señales (Señal, Cobertura, Estado; búsqueda "Buscar por señal…"), Accesos rápidos.
- API: GET `/internal/release-readiness`, GET `/systems/catalog/summary`.
- Redirección: `/reports/readiness` lleva aquí.

---

## 8. QA

Todas las pantallas de QA llevan botones de recorrido guiado (TutorialProvider): Llévame ahí, Seguir con «…», Dejarlo aquí, Entendido, Atrás, Omitir paso, Siguiente/Finalizar, Ver tutorial.

### `/qa/lab` — Laboratorio de testing (eyebrow "Consola de pruebas")
- Gates: `systems.endpoints.read`; ejecutar requiere `systems.endpoints.execute`; carga requiere `systems.stress.execute`.
- Pestañas: Prueba unitaria (subpestañas Funcional, Carga), Recorrido (encadenado), Árbol de decisión. Van en la URL como `?tab=&sub=`.
- Secciones: Seleccionar operación (Método, Ruta, Módulo, Riesgo, Estado, Acción; búsqueda "Buscar ruta, módulo o acción..."), Prueba funcional, Prueba de carga, Proveedores externos (simulador), Recorridos precargados, Campañas, Editor de pasos · diagnóstico, Árbol de decisión del recorrido.
- Tablas de la corrida: Persona, Pasos, Documento, Detalle, N.º, Paso, HTTP, Latencia, Arquetipo, Categoría del caso, Ingreso mensual, Nacimiento, Correo y teléfono, Motivo, Personas, Aprobaron, Fallaron, Omitidos, No aplica, Sin conclusión, Cancelados, Hito, Paso que falló, Intento, Respuesta, Código de la petición, Causa raíz, Campaña, Recorridos, Reparto de personas, Gravedad.
  - Búsquedas: por persona, por diario, por campaña, por código.
  - Filtros: Resultado, Estado, Desenlace, Gravedad.
- Acciones: Previsualizar / Enviar petición real; Previsualizar carga / Lanzar carga real; Previsualizar (simulación); Ejecutar recorrido real; Cancelar corrida; Generar caso; Generar y cargar; Cargar pasos (.json); Agregar paso; Ver pasos; Ver datos; Descargar el registro; Copiar; Comprobar; Terminar prueba.
  - Diálogos: "Confirmar simulación/recorrido", "Cancelar la corrida".
- API: `/systems/qa/journey-templates`, `/systems/qa/capabilities`, POST `/systems/qa/runs/preflight`, POST/GET `/systems/qa/runs`, `/systems/qa/runs/{id}` (con `/cancel`, `/events`, `/evidence`, `/personas`, `/personas/{k}/steps`, `/timeline`), `/systems/qa/campaigns`, `/workflows…`, `/operations/workflows/{c}/consistency`.

### `/qa/aprender` — Aprender el laboratorio QA
- Pestañas: Recorridos, Guía de referencia (`?tab=guia`); la guía tiene permiso propio.
- Recorridos: Recorrido, Tutoriales, Orden, Acciones (búsqueda "Buscar recorrido…").
- Tutoriales: Tutorial, Pantalla, Nivel, Duración, Estado (búsqueda "Buscar tutorial…"; filtros Pantalla, Nivel, Estado).
- Tablas de la guía: Escenario, Qué cambia, Credencial, Ambiente, Dial, Rango…
- Acciones: "Abrir el lab", "Empezar recorrido", "Reiniciar", "Simular corrida".
- API: GET del progreso de tutoriales del usuario.
- Redirección: `/qa/guia` lleva a `?tab=guia`.

### `/qa/suites` — Baterías de prueba registradas
- Nota: suites pre-registradas para comprobar flujos críticos de punta a punta. Gates: `systems.qa.read`; crear requiere `systems.qa.execute`.
- Columnas: Código, Nombre, Módulo, Tipo, Ambientes, Activa, Datos de ejemplo, Segura en producción, Destructiva.
- Filtros: búsqueda "Buscar por código, nombre o módulo…"; Tipo de suite.
- Acciones: "Nueva suite" (diálogo "Nueva suite de QA": Crear suite / Guardar cambios), "Abrir QA Live Lab", "Crear primera suite".
- Vacío: "Todavía no hay suites de prueba".
- API: GET/POST `/systems/test-suites`.

### `/qa/suites/[suiteId]` — título = nombre de la suite
- Gates: `systems.qa.read`; ejecutar requiere `systems.qa.execute`.
- Pestañas: Resumen, Pasos, Configuración, Ejecución.
- Columnas de pasos: #, Nombre, Método, Ruta, Ruta del catálogo, Datos de entrada, Continúa, Cleanup, Orden, Acción.
- Acciones: Editar batería; Agregar paso / Editar paso (diálogo); flechas para reordenar; "Ejecutar suite" (confirmación "Confirmar ejecución de pruebas"); "Actualizar run".
- API: GET/PATCH `/systems/test-suites/{id}`, POST/PATCH `…/steps`, POST `…/steps/reorder`, POST `…/run`.

### `/qa/runs` — Corridas de las suites QA
- Gate: `systems.qa.read`.
- Columnas: Corrida, Suite, Ambiente, Estado, Duración, Inicio, Fin, Creado.
- Filtros: búsqueda "Buscar por suite o n.º de corrida…"; Estado, Ambiente.
- Acción: "Abrir QA Live Lab".
- API: GET `/systems/test-runs`.

### `/qa/runs/[runId]` — "Ejecución QA {estado}"
- Tabla "Pasos ejecutados": Ejecución del paso, Paso, Estado, HTTP, Duración, Error, Creado.
- API: GET `/systems/test-runs/{id}`.

### `/qa/stress` — Perfiles de carga (eyebrow "Carga QA")
- Gates: `systems.stress.read`; crear requiere `systems.stress.execute`.
- Perfiles: Perfil, Nombre, Ruta, Peticiones/s, Duración, Concurrencia, Aprobación, Estado.
  - Búsqueda "Buscar perfil o ruta…"; filtro Estado.
- "Matriz de cobertura": Módulo, Requiere prueba de carga, Perfil habilitado, Perfiles.
- Acciones: "Nuevo perfil" (diálogo "Nuevo perfil de carga"), "Abrir el laboratorio QA".
- API: GET/POST `/systems/stress-profiles`, GET `/systems/stress-matrix`.

### `/qa/stress/[profileId]` — título = nombre del perfil
- Acciones: "Editar perfil"; "Encolar simulación / Encolar carga real" (confirmación). Enlaces al endpoint y a `/qa/stress/runs`.
- API: GET `/systems/stress-profiles/{id}`, POST `…/queue-run`.

### `/qa/stress/runs` — Historial de corridas de carga
- Gate: `systems.stress.read`.
- Columnas: Corrida, Proceso, Estado, Creado, Inicio, Fin, Actor, Acciones.
- Filtros: búsqueda "Buscar por código de perfil o n.º de corrida…"; Estado, Ambiente.
- Acción: "Ver datos" (diálogo "Detalle de la corrida de carga").
- API: GET `/systems/stress-runs`.

---

## 9. Crédito

### `/operations/credit/products` — Productos de crédito
- Desc: el catálogo ofrecido a clientes (montos, plazos, tasa, revisión humana). Nota: la lista enseña borradores, activos, suspendidos y retirados.
- Gate: RoleGate CREDIT_OPS.
- Columnas: Producto, Estado, Monto, Plazo, Tasa anual, Revisión, Vigencia, Acciones.
- Filtros: búsqueda "Buscar por nombre, código, moneda o descripción…"; Estado.
- Acciones: "Nuevo producto" (formulario "Nuevo producto de crédito": Crear en borrador, Activar ahora, Dejar en borrador); diálogo de cambio de estado con motivo y "Confirmar: …".
- API: GET/POST `/operations/credit/products`, PATCH `…/{id}/status`.

### `/operations/credit/applications/[applicationId]` — "Solicitud {code}"
- Gate: RoleGate CREDIT_OPS.
- Secciones: "Aceptación del negocio" ("Aceptar la operación" / "Declinar la operación"), "Decisión humana".
- Historial: Cuándo, Evento, Cambio de estado, Quién, Motivo, Notas (búsqueda "Buscar por evento, estado, quién, motivo o notas…").
- Badges: ApplicationStatusBadge, AcceptanceBadge.
- API: GET `/operations/credit/applications/{id}`, POST `…/business-acceptance`, POST `…/decision`.

### `/operations/credit/applications/from-case` — "Caso {code}"
- Pantalla de resolución que lleva a la solicitud. Vacíos: "Este caso no apunta a una solicitud de crédito.", "No encontramos la solicitud …".
- API: GET `/customers/{id}/credit-applications`.

---

## 10. Operaciones

### `/operations/pending-contacts` — Contactos sin verificar
- Nota: sin correo verificado no puede recuperar el PIN; reenviar usa el mismo canal que la app. Sin gate de página.
- Métricas: Contactos pendientes (toda la cola), Correos, Teléfonos.
- Columnas: Cliente, Contacto sin verificar, Estado del cliente, Declarado, Registrado.
- Filtros: búsqueda "Código de cliente, dominio o últimos 4…"; Tipo de contacto.
- Acción por fila: "Reenviar" / "Reenviar SMS". Enlaza a la investigación del cliente.
- API: GET `/operations/customers/pending-contact-verification`, POST `/customer-onboarding/{id}/contact-verification/request`.

### `/operations/work-queue` — Cola de trabajo
- Desc: casos de revisión manual y de fraude en una sola cola. Gate: RoleGate WORK_QUEUE.
- Pestañas: Todas, Revisión manual, Fraude.
- Métricas: "{pestaña activa}", Revisión manual con estos filtros, Fraude con estos filtros.
- Columnas: Tipo, Caso, Cliente, Prioridad, Estado, Motivo, Abierto, Acción.
- Filtros: búsqueda "Código de cliente o de caso…"; Estado, Prioridad.
- Badges: WorkItemTypeBadge, SeverityBadge, StatusBadge.
- Acción: "Decidir" abre el diálogo "Decidir caso de revisión manual/fraude · #id" (código de motivo, "Registrar decisión"); enlace al expediente.
- API: GET `/operations/work-queue`, POST `/operations/manual-review-cases/{id}/decision`, POST `/operations/fraud-cases/{id}/decision`.
- Redirecciones: `/operations/manual-review-cases` y `/operations/fraud-cases` llevan aquí con la pestaña correspondiente.

### `/operations/customers/[customerId]/investigation-summary` — "Investigación del cliente #id"
- Desc: perfil, contactos, consentimientos, última evaluación de riesgo y casos abiertos.
- Nota: cribado de listas restrictivas, habilitación y recálculo de riesgo; los casos se deciden en la cola.
- Las secciones se ocultan según el rol: LOAN_*, COMPLIANCE_SCREENING/CLEAR, CREDIT_OPS.
- Tablas:
  - Contactos: Tipo, Valor (últimos 4), Estado, Acciones.
  - Consentimientos: Finalidad, Otorgado, Revocado.
  - Casos: Caso, Código, Prioridad, Abierto, Gravedad.
  - Calificación: Fecha, Categoría, Venía de, Motivo.
  - Solicitudes: Solicitud, Importe, Plazo, Decidida, Comercio.
  - Préstamos: Préstamo, Capital prestado, Capital pendiente, Días de atraso, Desembolsado.
  - Expediente: Nombre, Origen, Tamaño, Modificado.
- Acciones:
  - Recalcular riesgo (confirmación).
  - Registrar decisión de habilitación.
  - Ejecutar cribado, Descartar coincidencias.
  - Recalcular desde la bitácora.
  - Reenviar código.
  - Registrar decisión de identidad.
  - Informe de gasto (PDF).
  - Desembolsar (diálogo).
  - Recalcular línea, Quitar ajuste.
  - Cambiar tarjeta / Confirmar cambio.
- API: GET `/operations/customers/{id}/investigation-summary`, POST `/customers/{id}/risk-assessments`, POST `…/eligibility/decision`, `…/compliance/screening`, `…/compliance/clear-matches`, `…/identity-verification/decision`, GET `…/behavior-summary`, `/customers/{id}/credit-applications`, `/customers/{id}/loans`, `…/credit-rating(-history)`, `/customers/{id}/credit-line`, POST `…/credit-line/recalculate`, POST `/credit-applications/{id}/disbursement`.

### `/operations/customers/[customerId]/audit` — "Auditoría del cliente #id"
- Desc: historial unificado de 8 fuentes. Nota: solo lectura. Gate: `audit.events.read`.
- Métricas: Eventos cargados, Fuentes cubiertas.
- "Feed de auditoría": Fecha, Fuente, Evento, Actor, Objetivo. Acción "Cargar más".
- API: GET `/operations/audit/customer/{id}/feed`.

### `/operations/sessions/[sessionId]/investigation-summary` — "Investigación de la sesión #id"
- Nota: solo lectura; el GPS nunca entrega latitud ni longitud.
- Métricas: Señales encendidas, Riesgo del dispositivo, Autenticaciones fallidas, Permisos denegados.
- Pestañas: Resumen, Dispositivo, Red e IP, SIM, Autenticación, Permisos, Riesgo del dispositivo, Ubicación, Acciones, Observaciones, Auditoría.
- Columnas por pestaña: Capturado, VPN, Proxy, Tor, País, Ciudad, Score de reputación, Operadora, Tipo de SIM, SIMs, Teléfono (últimos 4), Ocurrió, Tipo de evento, Resultado, Permiso, Otorgado, Respondido, Versión de app, Rooteado, Emulador, Reason code, Coordenadas, Precisión (m), Evento, Pantalla, Código, Valor booleano, Acción, Tipo de actor.
- Badges: SignalBadge, BooleanBadge.
- API: GET `/operations/sessions/{id}/investigation-summary`.

### `/operations/risk-assessments/[riskAssessmentRunId]` — "Evaluación de riesgo #id"
- Gate: `operations.riskPolicy.read`.
- Métricas: Score total, Score de fraude, Score de identidad, Decidido.
- Explicación: Impacto, Código, Qué mide (búsqueda "Buscar por código, descripción o impacto…").
- "Traza de auditoría", con pestañas Corrida, Resultado, Reglas, Contribuciones, Snapshot.
  - Reglas: Regla, Reason code, Dimensión, Severidad, Acción, Bloqueante, Disparada.
  - Contribuciones: Feature, Bin / atributo, Puntos, WoE, Valor crudo.
- API: GET `/operations/risk-assessments/{id}`, GET `…/explanation`.

### `/support` — Bandeja de casos (eyebrow "Soporte")
- Nota: cada fila es un caso real; si no estás en la mesa de soporte se te avisa (SupportAccessState, 403).
- Métricas: Casos en esta vista/página, P1 y P2, Sin agente.
- Panel de mesa:
  - "En espera": Conversación, Tipo, Expediente, Esperando desde, Estado, Abierta.
  - "Mías": búsqueda "Buscar por código, n.º de conversación o de expediente…"; filtros Asignación, Canal, Estado.
  - Acción "Atender".
- Casos: Caso, Asunto, Prioridad, Sensibilidad, Agente, Abierto, Acción.
  - Búsqueda "Buscar por número de caso, asunto o código de cliente…".
  - Filtros: Vista, Prioridad, Cola, Tipo, Motivo, Resolución, Causa raíz.
- Acciones: paginación Anterior/Siguiente; "Revisar plazos ahora" (solo SUPPORT_ADMIN, confirmación "¿Revisar los plazos ahora?").
- Badges: PrioridadBadge, EstadoCasoBadge, SensibilidadBadge.
- API: GET `/internal/support/cases`, `/queues`, `/codes`, `/categories`, `/desk/queue`, `/desk/mine`, POST `/internal/support/desk/sla/sweep`, POST `/desk/channels/{id}/claim`.

### `/support/cases/[caseId]` — número de caso
- Panel de chat (placeholder "Escribe tu respuesta…", "Enviar").
- Acciones: "Atender esta conversación", "Comprobar" (integridad), "Vincular", "Clasificar" (diálogo), "Reclamar", "Resolver" (diálogo), Cancelar.
- API: GET `/internal/support/cases/{id}`, `/timeline`, `/desk/channels/{id}/integrity`, POST `…/claim`, `…/links`, `…/resolve`, GET/POST `/support/channels/{id}/messages`.

### `/support/knowledge` — Base de conocimiento
- Nota: un artículo publicado no se edita; se redacta otra versión. Gate: RoleGate SUPPORT_KNOWLEDGE.
- Cola de versiones: Versión, Estado, Redactada por, Último cambio, Acción.
- Artículos: Artículo, Quién lo lee, Útil / no útil, Próxima revisión.
  - Búsqueda "Buscar por clave o título…"; filtros Estado, Quién lo lee.
- Acciones: "Crear artículo" (diálogo "Crear un artículo de ayuda": Crear y redactar), "Nueva versión", "Guardar borrador", diálogo de transición (revisar, aprobar, publicar).
- API: `/admin/support/knowledge/versions`, `…/versions/{id}`, `/admin/support/knowledge/articles`.

### `/support/agents` — Agentes de la mesa
- Nota: quien tiene rol de soporte o administración entra solo; aquí se habilita a otros. Sin gate de página.
- Columnas: Persona, Estado, Nivel, Presencia, Ocupación.
- Filtros: búsqueda "Buscar por nombre o correo…"; Estado, Nivel.
- Acciones: "Habilitar" (selector de usuario interno, capacidad), "Quitar de la mesa".
- API: GET/POST/DELETE `/internal/support/desk/agents`, GET `/internal/support/queues`.

### `/files` — Archivos (eyebrow "Operaciones")
- Desc: un expediente por cliente y por comercio. Gate: `expedientes.leer`.
- Columnas: Expediente, Estado, Manifiesto, Archivos, Tamaño, Enviado, Abierto.
- Filtros: búsqueda "Buscar por código de cliente, nombre o NIT del comercio…"; Tipo, Estado.
- Vacío: "Ningún expediente coincide."
- API: GET `/expedientes`.

### `/files/[expedienteId]` — código de cliente o "Expediente id"
- Explorador de archivos: Nombre, Origen, Tamaño, Modificado (búsqueda "Buscar en todo el expediente…").
- Panel de detalle con pestañas: Vista previa, Detalles, Quién lo ve, Historial.
- Acciones: Añadir archivos, Nueva carpeta, Ver/Ocultar la papelera, Vaciar la papelera, Restaurar, Renombrar, A la papelera (diálogo "Mover a la papelera"), Gestionar el acceso, Dar acceso, Quitar, Descargar.
- Vacío: "Esta carpeta está vacía."
- API: `/expedientes/{id}` (con `/nodos`, `/carpetas`, `/papelera`, `/actividad`, `/contactos`, `/nodos/{n}/contenido`, `/concesiones`, `/restaurar`).
- `/files/cliente/[customerId]` resuelve el expediente del cliente (GET `/expedientes/por-sujeto/customer/{id}`). Vacío: "Este cliente todavía no tiene expediente."

### `/views` — Vistas del negocio (eyebrow "Lectura gobernada")
- Gate: RoleGate INTERNAL_PORTAL.
- Las vistas se eligen con botones-chip; cada una con su búsqueda "Buscar por {campo}…" y sus filtros:
  - Clientes: Estado, Banda de riesgo.
  - Decisiones de riesgo: Estado, Banda, Decisión.
  - Cola operativa: Tipo, Estado, Prioridad, Severidad.
  - Salud de proveedores: Salud, Estado del proveedor.
  - Entrega de notificaciones: Estado, Canal, Categoría.
  - Cobertura de operaciones: Módulo, Riesgo, Revisión.
  - Eventos de auditoría: Tipo de evento, Tipo de actor, Tipo de objetivo.
- Métricas: Registros, Columnas. Las columnas son dinámicas.
- API: GET `/internal/views/{view}`, `/facets`.

### `/merchant-users` — Usuarios de comercio (eyebrow "Identidad del comercio")
- Nota: el ERP pide el acceso y esta consola lo concede; los datos no se editan al aprobar.
- Gates: RoleGate INTERNAL_PORTAL; las acciones requieren `merchant.users.manage`.
- Métricas: Por atender, Identidades, Activas, Suspendidas o dadas de baja.
- Cola de altas: Comercio, Persona, Pedido, Decisión (búsqueda "Buscar por correo, nombre, cuenta o sucursal…"; Conceder/Rechazar).
- Identidades: Usuario, Estado, Debe cambiar clave, Último acceso, Acciones (búsqueda "Buscar por correo, nombre o código…"; filtro Estado).
- Diálogo de cambio de estado con motivo ("Cambiar").
- API: GET `/merchant/users`, PATCH `…/{id}/status`, GET `…/provisioning-requests`, POST `…/approve` y `…/reject`.

### `/operations/partners` — Expedientes de comercio (eyebrow "Onboarding de comercios")
- Nota: la verificación la resuelve el Motor; aquí llega lo que exigió criterio humano.
- Gates: RoleGate PARTNER_OPS; acciones con `partner.qr.review` y `partner.kyb.request`.
- Métricas: Esperando decisión, Con esta búsqueda, El más antiguo, QR pendientes de activar, Del negocio, De cuenta bancaria.
- Cola: Comercio, Tipo, Estado, Enviado, Decidió, Expediente, Acciones (búsqueda "Nombre del comercio o NIT…"; "Revisar").
- QR: Sucursal, Entidad, Cuenta, Huella del archivo, Subido (búsqueda "Comercio, NIT, sucursal, cuenta o n.º de QR…"; filtro Tipo de QR; Activar QR / Rechazar o Revocar QR).
- Drawer del expediente con Dirección, Ciudad, Terminal, Serie, Nombre de fachada; acciones Aprobar, Rechazar, Cerrar sin decidir.
- API: GET `/operations/partners/queue`, `/qr-codes/pending`, POST `…/qr-codes/{q}/review`, POST `/operations/partners/{id}/decision`, `…/kyb-review`, `/partner-onboarding/{id}/status`.

### `/operations/portfolio` — Calificación de cartera (eyebrow "Operación de cartera")
- Desc: categoría de riesgo y previsión por deuda; corre sola cada 6 h. Gate: RoleGate LOAN_RATING.
- Métricas: Créditos calificados, Exposición, Previsión, Esperando entrega, Reintentando, Agotados, Entregados.
- Escala: Categoría, Significado, Días de atraso, Previsión, Cómo se lee.
- Resumen: Créditos, Exposición.
- Desenlaces agotados: Crédito, Ejecución, Ventana, Desenlace, Intentos, Último error, Observado (búsqueda "Número de préstamo…").
- Acciones: "Recalificar la cartera" (diálogo "Recalificar toda la cartera"), "Calificar" un préstamo.
- API: GET `/operations/credit-rating/portfolio-summary`, `/operations/rating-scale`, `/operations/loans/outcome-status`, `/outcome-backlog`, POST `/operations/credit-rating/sweep`, `…/loans/{id}/rate`.

### `/operations/loans` — Préstamos (eyebrow "Cartera")
- Nota: desembolsar se hace desde la ficha del cliente; cobros, reversos y castigos desde la ficha del préstamo. Gate: RoleGate LOAN_PORTFOLIO.
- Accesos rápidos por número de préstamo o de cliente ("Abrir", "Abrir ficha").
- Columnas: Préstamo, Cliente, Estado, Capital prestado, Capital pendiente, Días de atraso, Desembolsado.
- Filtros: búsqueda "Código de préstamo o de cliente…"; Estado, Tramo de mora.
- API: GET `/operations/loans`.

### `/operations/loans/[loanId]` — "Préstamo {code}"
- Gates: LOAN_READ; acciones con LOAN_MONEY y LOAN_WRITE_OFF.
- Métricas: Capital pendiente, Pendiente con interés y mora, Días de atraso.
- Cuotas: Cuota, Vence, Capital, Interés, Mora, Pendiente, Estado, Días de atraso.
- Cobros: Cobro, Recibido, Importe, Medio, Referencia.
- Historial: Cuándo, Qué pasó, Notas.
- Calificación: Fecha, Categoría, Venía de, Motivo.
- Acciones: "Registrar cobro" (diálogo), "Reversar", "Castigar" (drawer de motivo y confirmación).
- API: GET `/loans/{id}`, POST `…/payments`, POST `…/payments/{p}/reversal`, POST `…/write-off`, `/operations/loans/{id}/rating(-history)`.

### `/operations/payment-claims` — Avisos de pago
- Desc: comprobantes que envían los clientes y verifica el comercio; aquí solo se supervisan. Gate: RoleGate PAYMENT_CLAIMS.
- Métricas: Pendientes en la cola, "Atrasados (más de N h)", En esta consulta.
- Columnas: Aviso, Estado, Espera, Importe, Comercio, Cliente, Préstamo, Enviado, Decidido.
- Filtros: búsqueda "Código de aviso, de cliente o comercio…"; Estado, Antigüedad; botón "Ver sólo los atrasados".
- API: GET `/operations/payment-claims`.

### `/events` — Cola de eventos (eyebrow "Eventos de dominio")
- Gates: RoleGate INTERNAL_PORTAL; las acciones requieren RUNTIME_JOB.
- Métricas: Total con este filtro, Fallidos con esta búsqueda, Pendientes con esta búsqueda, Definiciones del catálogo.
- Columnas: Evento, Estado, Intentos, Disponible, Procesado, Último error, Acciones.
- Filtros: búsqueda "Código, agregado o correlación…"; Estado, Código del catálogo.
- Acciones: "Publicar evento" (diálogo "— Elige un evento —"), Reintentar o Cancelar por fila (confirmación).
- API: GET `/operations/events`, `/catalog`, POST `/operations/events`, `…/{id}/retry`, `…/{id}/cancel`.

### `/jobs` — Procesos automáticos
- Nota: todas las tareas arrancan en ensayo; la ejecución real exige confirmación.
- Pestañas: Historial (INTERNAL_PORTAL), Ejecutar ahora (RUNTIME_JOB, `?tab=ejecutar`); si no hay ninguna, ForbiddenState.
- Métricas: Corridas, Completadas, En ejecución, Fallidas.
- Historial: Proceso, Origen, Estado, Duración, Creado, Acciones (búsqueda "Buscar por código del proceso…"; filtros Estado, Origen; "Ver").
- Ejecutar: Job, Para qué sirve, Impacto, Ensayo, Parámetros (búsqueda "Buscar tarea por nombre, código o para qué sirve…"; filtros Impacto, Ensayo; "Ejecutar…" abre un diálogo con "Ejecutar ensayo" / "Ejecutar en real").
- API: GET `/internal/jobs`, POST `/operations/jobs/{code}`.
- Redirección: `/operations/runtime-jobs` lleva a `?tab=ejecutar`.

### `/jobs/[jobId]` — nombre del job
- RoleGate INTERNAL_PORTAL. Entrada, salida y error de la corrida.
- API: GET `/internal/jobs/{id}`.

### `/notifications` — Notificaciones (eyebrow "Mensajería interna")
- Desc: mensajes por in-app, push, email, SMS y WhatsApp con historial.
- Gates: `notifications.messages.read`; plantillas con `notifications.templates.manage`; envío con `notifications.messages.manage`.
- Pestañas: Mensajes, Plantillas, Preferencias, Enviar notificación.
- Mensajes: Fecha, Canal, Destinatario, Plantilla, Categoría, Título / asunto, Estado, Correlación, Detalle.
  - Búsqueda "Buscar por correlation ID, plantilla o título…"; filtros Canal, Estado, Destinatario.
  - Diálogo "Mensaje #id": Reintentar entrega / Cancelar mensaje.
- Plantillas: Código, Canal, Idioma, Versión, Categoría, Título y texto, Estado, Acciones.
  - Filtros Canal, Activa; "Nueva plantilla" y Editar.
- Preferencias: ID de cliente con "Cargar preferencias"; Evento, Canal, Obligatorio; "Agregar preferencia".
- Enviar notificación: formulario "Enviar notificación personalizada" con confirmación.
- API: `/operations/notifications/templates`, `/preferences/{id}`, `/messages`, `…/retry`, `…/cancel`, POST `/broadcast`.

### `/notifications/campaigns` — Campañas
- Nota: se arman en el ERP; aquí se siguen y se frenan. Gate: RoleGate CAMPAIGN_READ.
- Pestañas: Campañas, Segmentos.
- Campañas: Campaña, Estado, Canales, Avance, Ventana, Ficha (búsqueda "Buscar por nombre…"; filtro Estado; "Ver archivados / Ver activos").
- Segmentos: Segmento, A quién incluye, Tamaño, Calculado.
- API: GET `/operations/notifications/campaigns`, `/audience-segments`.

### `/notifications/campaigns/[campaignId]` — nombre de la campaña
- Métricas por canal: Avisos, Entregados, Por salir, Fallidos, Anulados, Leídos.
- Mensajes: Cliente, Turno, Resultado… (búsqueda "Buscar por cliente…"; filtros Estado, Canal).
- Acciones (CAMPAIGN_OPERATE): Pausar, Reanudar, Cancelar campaña, Volver.
- API: GET `…/campaigns/{id}`, `/messages`, POST `/pause`, `/resume`, `/cancel`.

---

## 11. Esquema de datos

### `/schema/versions` — Versiones de esquema
- Desc: catálogo DDL de solo lectura. Nota: el DDL real sale por migraciones; proponer solo registra.
- Gates: proponer requiere `governance.schema.propose`; aprobar requiere el permiso de aprobación; hay una sección solo para SUPER_ADMIN.
- Versiones: Versión, Tablas, Columnas, Relaciones, Notas, Creada (búsqueda "Buscar versión por código o notas…").
- Formularios: Área, Formulario, Propósito, Permiso.
- Acciones: "Change log"; "Proponer tabla" (diálogo "Proponer tabla nueva": Agregar columna, Agregar relación, Registrar propuesta).
- API: GET `/operations/schema/versions`, POST `/operations/schema/tables`.
- Redirección: `/forms` lleva aquí.

### `/schema/versions/[versionId]` — código de versión
- Métricas: Esquemas, Tablas, Columnas, Relaciones. Selector de esquema.
- Tablas: Tabla, Tipo, Solo añade filas, Por organización, Columnas, Relaciones, Descripción (búsqueda "Buscar tabla por nombre o descripción…").
- API: `…/versions/{id}`, `/schemas`, `/operations/schema/tables`.

### `/schema/tables/[tableId]` — nombre de la tabla
- Métricas: Columnas, Columnas con datos personales, Relaciones, Indexadas.
- Tablas: Columna, Propiedades, relaciones. Acción "Volver al inventario".
- API: GET `/operations/schema/tables/{id}`.

### `/schema/change-log` — Historial de cambios propuestos
- Nota: aprobar o rechazar exige permiso; quien propone no puede aprobar.
- Columnas: Cambio, Tipo, Entidad, Estado, Solicitante, Creado, Acción.
- Filtros: búsqueda "Buscar por tabla propuesta o tipo de cambio…"; Estado.
- Acción: "Revisar" abre un diálogo con Aprobar y Rechazar. Badge: ApprovalStatusBadge.
- API: GET `/operations/schema/change-log`, PATCH `…/{id}/approve`.

---

## 12. Proveedores externos

Ninguna de estas páginas tiene gate propio, salvo `/external-data`.

### `/external-data` — Datos externos del cliente (eyebrow "Evidencia externa")
- RoleGate INTERNAL_PORTAL. Campo de cliente con "Abrir cliente".
- Secciones: Salud de proveedores; 1 · Consentimientos ("Registrar consentimiento", "Revocar"); 2 · Consultar a un proveedor ("Ver qué costaría", "Consultar"); 3 · Con qué se decidió; Confianza digital ("Comprobar ahora"); Facebook ("Generar enlace de conexión").
- API: `/external-data/providers/health`, `/consents…`, `/requests/preview`, `/requests`, `/users/{id}/{dataset}`, `/digital-trust/…`, `/social/facebook/…`.

### `/external-providers` — Proveedores externos
- Nota: "Gestionar" abre modo, estado, corte de emergencia, políticas de costo y prueba real.
- Métricas: Responden, Llamadas, Éxito, Peor latencia p95, Costo estimado.
- Catálogo: Proveedor, Tipo de proveedor, Cómo se le llama, Salud, Autenticación, Política, Acciones.
  - Búsqueda "Buscar por código, nombre, categoría o descripción…"; filtros Tipo de proveedor, Cómo se le llama, Salud, Costoso.
- Actividad: Proveedor, Modo, Latencia reciente, Llamadas, Éxito, Latencia p95, Última llamada, Último error (filtros Modo, Último error).
- Drawer "Gestionar", con pestañas Resumen, Modo y tipo, Autenticación, Costos, Probar:
  - Modo y tipo: Guardar modo y tipo; Apagar de emergencia.
  - Costos: Consulta, Estado, Nivel de costo, Reglas, Costo por consulta, Máx./día usuario, Máx./día global; Guardar política.
  - Autenticación: Forzar renovación del acceso; Revocar credencial; Rotar credencial.
  - Probar: Ejecutar prueba; Llamar ahora / Simular.
- Enlaces del header: Auditorías, Solicitudes.
- API: `/admin/external-providers/dashboard`, `/health`, `/auth-state`, `/{code}/runtime`, `/kill-switch`, `/cost-policy`, `/credentials/{rotate|revoke|invalidate-token}`, `/test`.

### `/external-providers/audits` — Auditorías y diagnóstico
- Pestañas (8): Configuración, ¿Listo para producción?, Preparación, Cumplimiento, Consumo, Consultas repetidas, Purga por antigüedad, Datos sensibles.
- Métricas: Solicitudes revisadas, Repeticiones encontradas, Puntuación, Candidatas a purga, Más antiguas que, Respuestas revisadas, Hallazgos, Pedidas, Ejecutadas, Frenadas, Desde caché, Costo estimado, Costo real, Nota, Críticos, Altos, Medios.
- Columnas: Solicitud, Cliente, Cuándo se pidió, Cómo acabó, Qué se haría, Gravedad, Proveedor, Qué pasa, Qué hacer, Qué pasó, Veces, Clave encontrada, Respuesta, Llamadas, Éxito, Fallos, Bloqueadas, Límite, Credencial, p95, Costo, Salud, Sirve simulado, Sirve producción, Bloqueos, Políticas, Fallos recientes, Qué le falta.
- API: `/admin/external-providers/{idempotency-audit, retention/preview, sanitization-audit, production-gate, sla, usage, quality-audit, readiness}`.

### `/external-providers/requests` — Solicitudes a proveedores
- Pestañas: Listado, Aprobar, Reintentar, Recalcular indicadores, Vista previa de política.
- Listado: ID, Cuándo, Proveedor, Qué se pidió, Cliente, Cómo acabó, Aprobación, Tardó.
  - Búsqueda "Buscar por ID de solicitud, referencia del proveedor, tipo o error…"; filtros Proveedor, Cómo acabó, Aprobación.
- Acciones: Aprobar solicitud, Reintentar solicitud, Recalcular indicadores, Previsualizar política.
- API: `/admin/external-providers/requests`, `…/{id}/approve|retry|rebuild-features`, POST `/policy/preview`.

---

## 13. Seguridad y auditoría

### `/security/session` — Seguridad de sesión
- Datos tomados de la sesión en el cliente, sin API.
- Métricas: Permisos, Roles, Alertas, Bloqueos.
- Columnas: Comprobación, Estado, Qué significa (búsqueda "Buscar por comprobación o descripción…"; filtro Estado).

### `/audit` — Registro y auditoría del sistema
- Nota: reconstruir qué pasó (pago rechazado, decisión incorrecta…). Gate: `audit.events.read`.
- Pestañas: Registro del sistema, Auditoría de acciones.
- Registro técnico: Capturado, Tipo, Servicio, Líneas, Bytes, Contenido (búsqueda "Buscar en el contenido del log…"; filtro Tipo; "Cargar archivo de registro", "Refrescar").
- Acciones: Fecha, Código de referencia, Método, Ruta, Módulo, Quién, Resultado, Duración, Riesgo, Datos personales. Enlaza a `/audit/request/{id}`.
- API: GET `/systems/logs/mongo`, `/systems/action-logs`, `/filter-catalog`.

### `/audit/request/[requestId]` — "Solicitud {id}"
- Columnas: Fecha, Método, Ruta, Módulo, Acción, HTTP, Duración, Riesgo, Datos personales.
- API: GET `/systems/action-logs/by-request/{id}`.

---

## 14. Administración

### `/settings/users` — Usuarios internos
- Nota: cada persona con acceso puede ver datos de clientes, aprobar decisiones o tocar configuración crítica.
- Gates: `internal.users.read`; "Nuevo usuario" requiere `internal.users.manage`.
- Columnas: Usuario, Nombre, Departamento, Cargo, Roles, Segundo factor, Debe cambiar contraseña, Estado.
- Filtros: búsqueda "Buscar por correo, nombre, departamento, cargo o rol…"; Estado, Rol.
- API: GET `/internal/users`, `/internal/roles`.

### `/settings/users/new` — Nuevo usuario interno
- Nota: la contraseña inicial es temporal y aleatoria, el motivo queda auditado y se fuerza el cambio. Gate: `internal.users.manage`.
- Formulario "Crear usuario interno" con email y motivo, y botón "Crear usuario". Al terminar: "Usuario creado" e "Ir a la ficha del usuario".
- API: POST `/internal/auth/signup`.

### `/settings/users/[internalUserId]` — nombre completo
- Gates: `internal.users.read`; editar requiere `internal.users.manage`.
- Secciones: Permisos efectivos; Cuenta bloqueada por intentos fallidos ("Desbloquear" con motivo); Roles y permisos ("Guardar roles y permisos"); Editar / desactivar usuario ("Guardar cambios" / "Guardar y cambiar estado").
- API: GET/PATCH `/internal/users/{id}`, POST `/unlock`, PATCH `/roles`.

### `/settings/roles` — Roles internos
- Gate: `internal.roles.read`.
- Columnas: Rol, Estado, Permisos, Descripción (búsqueda "Buscar por código, nombre o descripción del rol…").
- API: GET `/internal/roles`.

### `/settings/permissions` — Permisos internos
- Gate: `internal.permissions.read`.
- Columnas: Permiso, Módulo, Acción, Estado, Descripción (búsqueda "Buscar por permiso, módulo, acción o descripción…"; filtro Módulo).
- API: GET `/internal/permissions`.

### `/settings/catalog-sync` — Actualizar inventario (eyebrow "Sincronización")
- Desc: no borra datos ni toca secretos; cada acción pide confirmación.
- Tarjetas de acción, cada una con su permiso y botón "Ejecutar" (confirmación "Confirmar sincronización").
- API: POST `/systems/endpoints/discover`, `/systems/endpoints/catalog-seed/refresh`, `/systems/tools/infer-requirements`, `/systems/data-entities/infer-impacts`.

### `/settings/profile` — Perfil interno
- Secciones: Roles, Permisos, Cambiar contraseña ("Enviarme el código" y luego "Confirmar contraseña nueva").

---

## 15. Fuera del shell

- **`/internal/login`**
  - Panel de marca: "Sistemas, QA y gobierno de datos en un solo lugar…".
  - Formulario: "Acceso interno ATLAS" / "Bienvenido de vuelta"; campos Organización, Correo interno, Contraseña; botón "Entrar al portal interno".
  - Segundo paso de PIN: "Verifica tu acceso" (código de 6 dígitos, "Confirmar y entrar", "Usar otra cuenta").
  - Enlace a recuperar acceso.
- **`/internal/recuperar-acceso`** — "Recuperar el acceso": Organización y Correo interno, botón "Enviarme el código". Después Código del correo y Contraseña nueva, botón "Cambiar mi contraseña", y finalmente "Ya puedes entrar" con "Ir al acceso".
- **`/legal`** — índice público "Documentos legales" (renderizado en servidor; si el backend falla muestra un aviso). **`/legal/[documentCode]`** muestra el documento con `legal-document-view`.