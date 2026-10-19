# Plan — El backoffice de AportaYa con la anatomía del portal interno de Atlas

- Fecha: 2026-10-18 · Repos afectados: monorepo Pasanaku (`packages/ui`, `apps/backoffice`, `servicios/auditoria`) · Predecesores: `2026-10-17-acceso-por-correo-y-backoffice` (H6 a medias), `2026-10-18-catalogo-de-metadata` (entregado y rechazado por Pablo: «sigue igual pero con mejores visuales»)
- Referencia: `~/Documents/GitHub/atlas/AtlasAdminPortal` (Next.js + Tailwind, 129 rutas). Nuestro stack: Angular 22 + `@aportaya/ui` + tokens. **No se cambia de stack ni se agregan librerías de UI**: se reproduce la anatomía con los componentes de la casa.
- Resultado observable: un operador abre cualquier pantalla del backoffice y encuentra la misma anatomía que en Atlas — cabecera con ícono, eyebrow, título, descripción y acciones; nota de la vista plegable; barra de filtros en tarjeta con búsqueda y selectores; tabla en tarjeta con cabecera «Registros · n», encabezado fijo y paginación completa; estados de carga (esqueleto), vacío (ícono + título + texto) y error (título + código de seguimiento + reintentar); fichas con pestañas y secciones clave–valor; formularios en tarjetas con confirmación; barra lateral oscura con grupos plegables, buscador global, campana y menú de usuario.
- Kill-test: poner lado a lado `backoffice.aportaya…/tablero/catalogo-de-datos/pago` y `AtlasAdminPortal/internal/data-catalog/tables/:id`: si la ficha nuestra no tiene pestañas «Vista general · Columnas · Operaciones · Gobierno», ni secciones clave–valor, ni botón «Editar ficha» que lleve a un formulario con confirmación, no está hecho.

## Pedido literal (2026-10-18, Pablo)
> «Hagamos un plan ultra detallado … necesito que te fijes detalladamente lo que tiene admin portal y lo hagas ultra detallado porque lo que estoy viendo es una mierda.»
> (antes) «Sigue igual pero con mejores visuales: falta la catalogación de la metadata, los elementos que están activos tal como está en el admin portal.»

## Hallazgos (fase 1) — qué tiene Atlas y qué tenemos

### 1. Sistema visual (Atlas `tailwind.config.ts` + `globals.css` → nuestros tokens)
| Pieza | Atlas | AportaYa (hoy) | Decisión |
|---|---|---|---|
| Lienzo / superficie / línea | `#f5f5f7` / `#fff` / `#e4e4e7` | `--bg #F4F6F5` / `--surface` / `--border` | Reusar nuestros tokens (misma función) |
| Acento | verde azulado `#006a61` (uno solo; «el color es señal») | `--brand #1C5A3A` | Reusar `--brand`; **ningún literal** (`verificar_frontend.py`) |
| Tonos de señal | success / warning / critical / info / **pii** `#6b3fa0` / sensitive | `--ok --aviso --err --info`; **no hay pii** | Agregar rol `pii` y `pii-bg` en `packages/tokens` (claro y oscuro) |
| Tipografía | Inter; títulos 30/38 bold en el acento; eyebrow 11px uppercase tracking .16em; tabla 11px uppercase | Instrument; `--t-titulo-1` 26px; `--t-etiqueta` 12px | Reusar roles; agregar `--t-eyebrow` y `--t-cabecera-tabla` |
| Sombras | subtle / card / card-hover / glow | `--sombra-1/2/3` | Reusar |
| Radios | 8 / 12 / 16 (xl 2xl) | `--r-sm/md/lg/xl` | Reusar |
| Movimiento | fade-in de la vista, shimmer del esqueleto, scale-in de diálogos, drawer-in | nada en el backoffice | Agregar con `prefers-reduced-motion` (95.5.1) |
| Modo oscuro | no tiene | sí (`data-theme`) | Todo lo nuevo en los dos temas (95.5.4) |

### 2. Shell (Atlas `app-shell.tsx`, `internal-shell/*` → nuestro `layout/shell-financiero.ts`)
| Pieza | Atlas | Nuestro | Falta |
|---|---|---|---|
| Barra lateral | oscura, 268px, logo + «ATLAS / Portal interno», **grupos plegables** con ícono y rótulo uppercase, ítem activo resaltado, 12 grupos / ~70 ítems, filtrados por permiso | oscura, 16rem, marca, 5 grupos planos, 10 ítems | Grupos plegables con estado recordado; ítems anidados (subrutas); íconos por ítem |
| Cabecera superior | sticky «glass», hamburguesa en móvil, **migas de pan** + título de la vista, **buscador global** (combobox con recientes y resultados), **campana de notificaciones**, menú de usuario | solo el rol y el botón «Cómo funciona esta pantalla» | Migas, buscador global (sobre el catálogo de pantallas y datos que ya tenemos: tablas, casos, grupos), campana (sin backend de notificaciones: se declara como vacío honesto o se omite), menú de usuario con perfil y salir |
| Contenido | `main` con fade-in por navegación; «Explicador de la vista» plegable (negocio + sistemas) arriba de cada pantalla | `banda-de-proposito` fija (una franja verde siempre abierta) | Reemplazar la banda por el explicador plegable (la banda abierta «pinta de aviso la franja más ancha de la pantalla», Atlas lo quitó por eso) |
| Botón de asistente flotante | sí (AssistFab) | no | **OUT** (sin backend de asistente) |

### 3. Componentes compartidos (Atlas `shared/components` → `packages/ui`)
| Atlas | Qué hace | Nuestro equivalente | Estado |
|---|---|---|---|
| `PageHeader` | ícono en loseta `accentSoft`, eyebrow, título en acento, descripción, carril de acciones (scroll en móvil) | `<h1>` suelto | **Crear `ap-cabecera-de-pagina`** |
| `SectionHeader` | h2 con ícono + descripción | h2 suelto | **Crear `ap-cabecera-de-seccion`** |
| `BusinessContextNote` | «Notas de la vista» plegable, bombilla, chevron | `ap-banda-de-proposito` (fija) | **Crear `ap-nota-de-vista`**; retirar la banda de las 41 pantallas |
| `ViewExplainer` | explicación de módulo y vista (negocio / sistemas) desde un registro central | tutoriales (`@aportaya/tutoriales`) | Integrar: el explicador abre el tutorial de la pantalla |
| `FilterBar` | tarjeta blanca con buscador (debounce 350 ms, tooltip) + selectores compactos con opción «Todos» descrita + «Limpiar» | `ap-barra-de-filtros` (chips sueltos + buscador) | **Rehacer `ap-barra-de-filtros`**: tarjeta, selectores, limpiar; estado en la URL (ya lo tiene) |
| `DataTable` | tarjeta con cabecera «REGISTROS · n» y botón copiar; `thead` fijo 11px uppercase; filas con hover; columna fijada a la derecha (acciones); alto máx 560 con scroll; vacío con `EmptyState` | `ap-tabla-de-datos` (colapsa a tarjetas en móvil, ordena) | **Rehacer `ap-tabla-de-datos`** conservando el colapso móvil y el orden |
| `Pagination` | «Página x de y · n registros», filas por página (10/20/50), ir a página, Anterior/Siguiente | `ap-paginacion` (Anterior / Página x de y / Siguiente) | **Ampliar `ap-paginacion`** |
| `Badge` + `StatusBadge/ReviewStatusBadge/PiiBadge/RiskBadge/BlockBadge/ModuleBadge` | tono + punto (vivo en success) o ícono; 7 tonos | `ap-chip-estado` (5 tonos, punto o ícono) | **Ampliar `ap-chip-estado`**: tonos `pii` y `apagado`, punto vivo opcional; crear las insignias semánticas en el backoffice (`insignia-de-estado`, `-de-revision`, `-de-datos-personales`) |
| `MetricCard` | ícono en loseta con tono, rótulo uppercase, cifra 2xl tabular, pista; hover eleva | `ap-tarjeta-kpi` (rótulo + valor + variación) | **Ampliar `ap-tarjeta-kpi`**: ícono, tono, pista |
| `LoadingSkeleton` | n filas con shimmer (solo transform) | `ap-estado-de-pantalla` (3 barras) | **Crear `ap-esqueleto-de-filas`** |
| `EmptyState` | ícono en loseta, título, texto, acción; borde punteado | `ap-estado-vacio` | Alinear anatomía (ícono + borde punteado) |
| `ErrorState` | rojo, ícono, título, texto, **código de referencia**, Reintentar | `ap-estado-de-pantalla` rama error | Alinear (ya muestra `trazaId`); extraer `ap-estado-de-error` |
| `ForbiddenState` | «Acceso restringido» | `ap-estado-de-bandeja` denegado | Alinear |
| `DetailTabs` / `UrlTabs` | pestañas segmentadas (fondo soft, activa blanca); `UrlTabs` con `?vista=` y teclado | `ap-pestanas`, `ap-selector-segmentado` | **Ampliar `ap-pestanas`**: estado en la URL, teclado (flechas/Inicio/Fin) |
| `KeyValueSection` / `KeyValueGrid` | tarjeta con cabecera gris + lista `dt/dd` cebra, rótulo uppercase, valor `mono` copiable | `dl` suelto | **Crear `ap-seccion-clave-valor`** (+ `ap-codigo-copiable`) |
| `Card / CardHeader / CardContent` | tarjeta 2xl, borde, sombra card, `interactive` eleva | `ap-tarjeta` | Alinear (variante interactiva) |
| `DrawerPanel` | cajón derecho max-w-xl, cabecera fija, cierra con Escape/velo, foco atrapado | no hay | **Crear `ap-cajon-lateral`** sobre `ap-dialogo` |
| `ConfirmDialog` | ámbar, frase de confirmación escrita opcional, carga | `ap-dialogo` | **Crear `ap-dialogo-de-confirmacion`** |
| `Button` | primary (ink), secondary, ghost, danger; `isLoading` con texto; eleva al hover | `ap-boton` (primario/secundario…) | Alinear variantes `fantasma` y `peligro`; estado cargando con texto |
| `Input` / `OptionSelect` / `FieldLabel` + `FieldTooltip` | campo 44px, radio xl, foco con anillo; **cada campo con tooltip de ayuda** (`check-field-help` lo exige) | `ap-campo`, `ap-lista-de-opciones` (cumplimiento) | Mover `ap-lista-de-opciones` y `ap-area-de-texto` a `packages/ui`; agregar tooltip de ayuda |
| `Tooltip` | por hover y foco, posicionado | no hay | **Crear `ap-tooltip`** |
| `CopyButton` / `BotonCopiarTabla` | copia valor / copia tabla como texto | no hay | Crear `ap-boton-copiar` |
| `ExportDownloadButton` | descarga CSV del listado | no hay | OUT salvo donde ya exista exportación (CU-58) |
| Breadcrumbs | `Inicio › Sección › Vista` | no hay | **Crear `ap-migas`** en el shell |

### 4. Catálogo de datos — lo entregado vs Atlas, pieza por pieza
Atlas (`features/data-catalog/*`):
- **Lista** `/data-catalog/tables`: `PageHeader` (ícono Database, «Catálogo de datos», descripción, acción Exportar) · `BusinessContextNote` · `FilterBar` (buscador «Buscar tabla, esquema, entidad, modelo, módulo o responsable…» con tooltip; filtros **Bloque (con conteo por bloque)** y **Revisión**; Limpiar) · `DataTable` con columnas **Bloque · Schema · Tabla (enlace mono) · Entidad · Módulo · Responsable · Datos personales · Financiera · Riesgo · Crítica para auditoría · Retención · Revisión · Estado** · paginación en el servidor.
- **Ficha** `/tables/:id`: `PageHeader` (eyebrow «Catálogo de datos», título `schema.tabla`, descripción = propósito; acciones: **«Editar metadatos»** + insignias PII / Revisión / Estado) · **pestañas «Vista general · Columnas · Operaciones · Gobierno operativo»**:
  - Vista general: `KeyValueSection` ×4 — *Identificación técnica* (Schema, Tabla, Modelo, Detección) · *Contexto de negocio* (Nombre de negocio, Módulo, Responsable, Propósito) · *Clasificación* (Datos personales, Financiera, Riesgo, Legal) · *Responsabilidad* (Auditoría crítica, Retención, Estado de revisión).
  - Columnas: `MetricCard` ×5 (Columnas, Datos personales, Uso ML, Descritas, Aprobadas) · `FilterBar` (buscador; Datos personales, Uso ML, Revisión) · `DataTable` (Columna · Tipo [insignia] · Descripción · Nombre de negocio · Datos personales · Uso ML · Nulos · Validación · Revisión · **Acciones → «Revisar»** abre `DrawerPanel` con Decisión, Nivel de confianza, Motivo).
  - Operaciones: tabla de endpoints que usan la tabla.
  - Gobierno operativo: `KeyValueSection` ×3 — *Clasificación de datos* · *Auditoría* · *Responsabilidad y vigencia* (Responsable, Retención, Estado review, Fuente detección, Confianza).
- **Edición** `/tables/:id/metadata` (página aparte): `PageHeader` (eyebrow «Formulario de catálogo», «Configurar tabla», botón Volver) · tarjeta **«Ficha de negocio»** (Responsable, Política de retención, Estado, Revisión, Propósito; cada campo con tooltip) · tarjeta **«Clasificación de los datos»** (7 interruptores) · `ErrorState` de validación · **`ConfirmDialog`** «Guardar ficha de la tabla — Se actualizará la ficha de X (n campos)».

Lo entregado el 2026-10-18 (`rutas/tablero/catalogo/*`): lista con 5 KPI planos, 4 `<select>` nativos sueltos, tabla con chips; ficha con `h1` + `dl` + formulario de gobierno incrustado + tabla de columnas + listas de relaciones. **Sin** cabecera de página, sin nota plegable, sin barra de filtros en tarjeta, sin tarjeta de tabla con conteo ni encabezado fijo, sin paginación completa, sin esqueleto, sin pestañas, sin secciones clave–valor, sin página de edición ni confirmación, sin revisión por columna, sin tooltips. Eso es «sigue igual pero con mejores visuales»: mismo esqueleto, otro color.

### 5. Nuestras 41 pantallas (`apps/backoffice/src/app/rutas`, 66 rutas) — todas con la anatomía vieja
`banda-de-proposito` + `h1` + contenido. Por sección: tablero (1 + 3 por rol + catálogo 2) · operación (10) · cumplimiento (14) · admisiones (2) · sistemas (10) · contabilidad (6) · publicidad (6) · configuración (4) · ayuda (1). Se migran **todas** a la anatomía nueva (H4), sin cambiar su contrato ni su lógica.

### 6. Inventario completo de Atlas (anexo)
`evidencia/atlas-inventario.md` — las 129 rutas con columnas, filtros, acciones, estados y endpoints, por grupo del menú. **Pendiente del agente de relevamiento**; el plan se corrige si revela anatomías que no están arriba.


### 7. Los módulos del admin portal, uno por uno (menú de Atlas → AportaYa)
Fuente: `internal-shell/nav-groups-*.ts` (12 grupos). «Tenemos» = ya existe una pantalla nuestra con esa función; «Adaptar» = existe el dato pero no la pantalla; «Nuevo» = no hay nada; «No aplica» = es del negocio de crédito/comercios de Atlas.

| Grupo Atlas | Ítem Atlas | AportaYa hoy | Decisión | Hito |
|---|---|---|---|---|
| Inicio | Centro interno (cifras, postura del catálogo, tráfico, accesos rápidos) | `tablero` por rol | Rehacer con la anatomía (cifras con ícono/tono, tarjetas «Postura», accesos rápidos) | H4 |
| Catálogo y metadatos | Catálogo de datos (lista, ficha 4 pestañas, metadata, revisión por columna) | `tablero/catalogo-de-datos` (entregado 10-18, insuficiente) | Rehacer igual a Atlas | H3 |
| | Dominios y glosario | nada | **Nuevo**: glosario de términos del pasanaku (aporte, turno, cupo, bolsa, garantía…) con dominio, definición, dueño, sinónimos y tablas relacionadas; fuente: `docs/Modelos` y `docs/entidades/*.md` | H5 |
| | Definiciones del motor | nada (sin motor de decisión) | No aplica | — |
| | Catálogos operativos (versiones, elementos activos/inactivos) | `catalogo.*` (19 tablas: feriados, motivos, tarifas, umbrales…) sin pantalla | **Nuevo, máximo alcance (Pablo 10-18)**: lista de catálogos con versión vigente, dueño y estado; ficha con elementos, atributos y alias, conmutador Activo/Inactivo; propuesta de versión → aprobación → publicación con vigencia | H5 |
| Linaje | Linaje e impacto | relaciones del catálogo | **Adaptar**: grafo de relaciones por tabla (ya viene `relaciones` en el catálogo) e impacto (qué tablas dependen) | H5 |
| Gobierno y calidad | Consentimientos del cliente | `consentimiento` (tabla, CU-07) | **Adaptar**: versiones de documentos de consentimiento y quién aceptó cuál | H5 |
| | Contrato de comercios | — | No aplica | — |
| | Contenido de la app | nada | **Nuevo**: textos y avisos publicables en la app (requiere contrato nuevo) | H5 (último) |
| | Políticas de notificación | `configuracion/mensajeria` | Tenemos → migrar anatomía | H4 |
| | Motor de decisiones | — | No aplica | — |
| | Gobierno de datos (PII por tabla, políticas) | sensibilidad por columna en el catálogo | **Adaptar**: vista «Datos personales» = tablas y columnas PERSONAL/SECRETO con responsable y retención | H5 |
| | Solicitudes de privacidad | CU-07 `solicitud_datos_personales` | **Adaptar**: bandeja de solicitudes ARCO con plazo y estado | H5 |
| | Políticas de gobierno | `politica_retencion`, `umbral_operativo` | **Adaptar**: lista de políticas vigentes con vigencia y base legal | H5 |
| | Política de riesgo | `regla_cumplimiento` (CU-48) | Tenemos (`cumplimiento`) → migrar | H4 |
| | Incidencias y reglas de calidad | nada | **Nuevo** (bajo): reglas de calidad = restricciones del modelo (`docs/Restricciones.md`, 173) y su verificación diaria | H5 (último) |
| Reportes | Reportes dinámicos | `configuracion/reportes` (CU-58) | Tenemos → migrar | H4 |
| | Preparación de salida (release readiness) | `sistemas/despliegues` | Tenemos → migrar | H4 |
| QA | Laboratorio, baterías, ejecuciones, carga | nada | No aplica (QA de Atlas es su propio producto); nuestro CI cubre | — |
| Crédito | Productos de crédito | — | No aplica | — |
| Operaciones | Contactos sin verificar | `cumplimiento/verificaciones` | Tenemos → migrar | H4 |
| | Cola de trabajo | `operacion` (bandejas) | Tenemos → migrar como «Cola de trabajo» unificada | H4 |
| | Archivos (expedientes) | KYC en MinIO (`verificacion_kyc`) | **Adaptar**: expediente por persona con sus documentos y quién lo vio | H5 |
| | Vistas del negocio | nada | **Adaptar**: consultas de solo lectura gobernadas (grupos, aportes, entregas) con permiso y huella | H5 |
| | Usuarios de comercio, expedientes de comercio, calificación de cartera, préstamos | — | No aplica | — |
| | Eventos de dominio | `sistemas/outbox` | Tenemos → migrar | H4 |
| | Procesos automáticos (jobs) | `tarea_automatizada` (CU-96) sin pantalla completa | **Adaptar**: lista de tareas con última ejecución, próxima, estado y reintento | H5 |
| | Mensajería interna, Campañas | `publicidad/campanas`, `configuracion/mensajeria` | Tenemos → migrar | H4 |
| | Avisos de pago | `operacion/reclamos` y `cumplimiento/reclamos` | Tenemos → migrar | H4 |
| | Soporte, base de conocimiento, agentes | `ticket_soporte` sin pantalla; `ayuda` | **Adaptar**: bandeja de tickets + base de conocimiento = la ayuda actual | H5 |
| Procesos | Procesos (definición e instancias) | `regla_automatizacion` (CU-95) | **Adaptar**: procesos del pasanaku (ciclo del grupo, cobro, entrega) con sus instancias | H5 (último) |
| Sistemas | Operaciones del sistema (endpoints) | `tablero/sistemas` endpoints del día | Tenemos → migrar a «Operaciones del sistema» con ficha por endpoint | H4 |
| | Mapa de rutas, trabajo pendiente, deriva de permisos, revisión de flujos, compuerta | `tablero/catalogo-de-flujos` | **Adaptar** solo «Deriva de permisos» (permisos declarados vs usados: ya lo calcula `secciones.ts`) | H5 |
| | Herramientas (salud) | `sistemas/proveedores`, `servicios` | Tenemos → migrar | H4 |
| | Salud de la red | `sistemas/servicios` | Tenemos → migrar | H4 |
| | Artefactos del motor | — | No aplica | — |
| | Revisión del catálogo (cola) | nada | **Adaptar**: cola de tablas/columnas en NECESITA_REVISION | H3 |
| Esquema de datos | Versiones de esquema, historial de cambios | `sql/` generado y `git` | **Adaptar**: versión del esquema desplegado (hash de `aplicar.sql`), tablas por módulo, cambios por commit | H5 |
| Proveedores externos | Datos del cliente, catálogo y salud, auditorías, solicitudes | `sistemas/proveedores`, `sistemas/webhooks` | Tenemos → migrar (catálogo y salud + webhooks como «solicitudes») | H4 |
| Seguridad y auditoría | Seguridad de sesión | `sistemas/accesos` (sesiones, factores) | Tenemos → migrar | H4 |
| | Registro del sistema (auditoría) | `bitacora_evento` (CU-14/58) sin pantalla | **Nuevo**: bandeja de la bitácora con filtros (entidad, acción, actor, fecha) y detalle con valor anterior/nuevo | H5 |
| Administración | Usuarios internos (lista, alta, ficha, editar/desactivar, roles, desbloqueo) | `sistemas/accesos` (solo lista) | **Nuevo**: módulo completo (CU-08 asignar/revocar roles ya existe en identidad) | H6 |
| | Roles internos | `rol` (18) | **Nuevo**: lista con estado, permisos y descripción; ficha con permisos efectivos | H6 |
| | Permisos internos | `permiso` (41) | **Nuevo**: lista por módulo, acción, riesgo, exige motivo | H6 |
| | Actualizar inventario | — | No aplica (`catalogo_de_datos.py` corre en el build) | — |
| | Perfil | nada | **Nuevo**: perfil del operador (nombre, rol, segundo factor, sesiones) | H6 |

### 8. Tipos de usuario: los 20 roles internos de Atlas (`evidencia/atlas-roles-y-permisos.md`)
Atlas define 20 roles con departamento y descripción (SUPER_ADMIN, SYSTEMS_ADMIN, INTERNAL_IDENTITY_ADMIN, OPERATIONS_MANAGER/ANALYST, RISK_MANAGER/ANALYST, FRAUD_ANALYST, COMPLIANCE_MANAGER/ANALYST, COLLECTIONS_MANAGER/AGENT, FINANCE_MANAGER, MERCHANT_OPERATIONS, DATA_GOVERNANCE_MANAGER, DATA_QUALITY_ANALYST, QA_ENGINEER, AUDITOR_READONLY, SUPPORT_AGENT, EXECUTIVE_READONLY) y 68 permisos con **nivel de riesgo** y **«exige motivo»**. Nosotros tenemos 18 roles y 41 permisos sin riesgo ni motivo. **Hipótesis:** estos son «los tipos de usuario que habíamos dicho con sus opciones ultra detalladas». Se registra como ambigüedad 4 y se planifica H6 sobre esta base, adaptando los roles al pasanaku (sin crédito ni comercios).

## Alcance
- IN: tokens nuevos (pii, eyebrow, cabecera de tabla); componentes de `packages/ui` listados en §3; shell (grupos plegables, migas, buscador global, menú de usuario); catálogo de datos completo como Atlas (lista, ficha con pestañas, edición en página aparte con confirmación, revisión por columna con su persistencia); migración de las 41 pantallas a la anatomía nueva; prueba visual de cada pantalla en 3 anchos × 2 temas; despliegue en TEST.
- OUT: asistente flotante (sin backend); exportación CSV donde no existe contrato; los módulos de Atlas marcados «no aplica» en §7 (crédito, comercios, cobranza, motor de decisión, QA de Atlas); app móvil y web pública.
- Ambigüedades registradas y **resueltas por Pablo el 2026-10-18** («1. exacto adaptado a pasanaku. 2. los catálogos de nosotros, ambos máximo alcance. 3. implementala toda. 4. obvio»):
  1. «Elementos activos» = **las dos cosas**: estado por tabla del catálogo de datos (H3) **y** catálogos operativos de AportaYa con cada elemento activo/inactivo (H5.S2), con el máximo alcance.
  2. Campana de notificaciones: **se implementa completa**, con backend: avisos internos para operadores en el servicio `notificaciones` (contrato nuevo), no una fachada. Pasa a H5.S0.
  3. Revisión por columna: **sí**, con tabla y endpoint nuevos (H3.S1).
  4. Tipos de usuario = **los 20 roles internos de Atlas adaptados al pasanaku** (§8). H6 desbloqueado.

## Decisiones
- Un componente nuevo entra a `packages/ui` solo si no existe uno equivalente (95.1.1); lo que ya existe se amplía, no se duplica.
- Los componentes se escriben primero contra el catálogo de datos (H3) y recién después se migra el resto (H4): así el patrón se prueba en una pantalla completa antes de replicarlo 40 veces.
- Cada pantalla migrada conserva sus specs; se agregan los de la anatomía (cabecera, nota, estados) una sola vez en `packages/ui`.
- Toda animación respeta `prefers-reduced-motion`; todo componente nuevo funciona en claro y oscuro.

## H0 — Sistema de diseño del backoffice (tokens y documento)
**CA:** Dado `packages/tokens`, cuando se generan los tokens, entonces existen los roles `pii`, `pii-bg`, `pii-texto`, `--t-eyebrow`, `--t-cabecera-tabla` en claro y oscuro, y `docs/trabajo/…/evidencia/sistema-visual.md` deja escrita la equivalencia Atlas → AportaYa de §1.
**DoD:** `yarn workspace @aportaya/tokens build` exit 0 · `verificar_frontend.py` TODO OK · contraste ≥ 4,5:1 medido para `pii` sobre `pii-bg` (claro y oscuro).
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H0.S1.M1 | Roles `pii*` y tipografías `eyebrow` / `cabecera-tabla` en `tokens.json` (claro y oscuro) | `tokens.css` los contiene en los 2 temas | `yarn workspace @aportaya/tokens build` + `grep -c pii tokens.css` → 6 | HECHO — bóveda `estilos.css` + `tokens.json`; `tokens.css` con `--pii`, `--pii-bg`, `--pii-texto` en claro y oscuro, `--t-eyebrow`, `--t-cabecera-tabla`; tokens 15/15 (`tokens-contra-boveda` y `roles-contraste` ampliados con pii) |
| H0.S1.M2 | Contraste de los pares nuevos | ratio ≥ 4,5 en claro y oscuro | script en `evidencia/H0-contraste-pii.txt` | HECHO — claro 8,05:1 / oscuro 7,44:1 (texto); `--pii` sobre superficie 7,38 / 5,66 |
| H0.S1.M3 | `evidencia/sistema-visual.md`: tabla de equivalencias y anatomía de página (cabecera → nota → filtros → tabla → paginación; ficha → cabecera → pestañas → secciones) | El documento existe con las 2 anatomías dibujadas en ASCII | revisión | HECHO — `evidencia/sistema-visual.md` (color, tipografía, espacio, listado, ficha, shell) |

## H1 — Componentes compartidos en `packages/ui`
**CA:** Dado `packages/ui`, cuando se corre su suite, entonces cada componente de §3 marcado «Crear/Rehacer/Ampliar» existe con spec, funciona en claro y oscuro, con teclado, y sin literales de diseño.
**DoD:** `yarn --cwd packages/ui test:front` verde · `verificar_frontend.py` TODO OK · captura de cada componente en claro/oscuro en `evidencia/H1/`.
**Estado:** HECHO — packages/ui 171/171 (37 archivos, axe en claro y oscuro), lint, reglas; capturas `evidencia/H1/catalogo-backoffice-*`

### H1.S1 — Estructura de página
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `ap-cabecera-de-pagina`: ícono en loseta, eyebrow, título, descripción, carril de acciones (scroll horizontal en móvil) | Spec: renderiza las 5 partes; sin desborde a 360 px | `ng test` del paquete + captura | HECHO — `cabecera-de-pagina.spec.ts` 3/3 (captura en H3) |
| H1.S1.M2 | `ap-cabecera-de-seccion`: h2 con ícono y descripción | Spec | idem | HECHO — en el mismo spec |
| H1.S1.M3 | `ap-nota-de-vista`: plegable, `aria-expanded`, chevron, recuerda estado por sesión | Spec: abre/cierra con teclado; cerrado no es tarjeta | idem | HECHO — `nota-de-vista.spec.ts` 4/4 (incluye almacenamiento roto) |
| H1.S1.M4 | `ap-migas`: lista `nav[aria-label=Breadcrumb]` desde las rutas | Spec: `Inicio › Sistemas › Catálogo de datos` | idem | HECHO — `migas.spec.ts` 3/3 |
| H1.S1.M5 | `ap-tooltip`: por hover y foco, Escape cierra, `aria-describedby` | Spec de teclado | idem | HECHO — `tooltip.spec.ts` 2/2 (Escape descarta hasta salir y volver) |

### H1.S2 — Listados
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | `ap-barra-de-filtros` v2: tarjeta, buscador con debounce 350 ms y tooltip, selectores con «Todos», botón Limpiar; estado en la URL | Spec: debounce 350 ms, Limpiar solo con filtros, borrar avisa al instante, destruir no emite | `ng test` + captura | HECHO — `barra-de-filtros.spec.ts` 4/4 (presentacional; la URL la maneja el backoffice) |
| H1.S2.M2 | `ap-tabla-de-datos` v2: tarjeta con «Registros · n» y copiar, `thead` fijo, hover de fila, columna de acciones fijada a la derecha, alto máximo con scroll; conserva colapso móvil y orden | Spec: las 3 piezas nuevas + los specs viejos siguen verdes | idem | HECHO — `cabecera`, `totalDeRegistros`, `altoMaximo`, `fijaDerecha`, `mono`, copiar; specs viejos 11/11; backoffice 839/839 |
| H1.S2.M3 | `ap-paginacion` v2: «Página x de y · n registros», filas por página, ir a página, Anterior/Siguiente | Spec: ir a página fuera de rango se acota | idem | HECHO — `paginacion.spec.ts` 4/4 |
| H1.S2.M4 | `ap-esqueleto` forma `filas`: n filas con brillo por `transform`; sin movimiento con `prefers-reduced-motion`; `estado-de-pantalla` lo usa | Spec + captura | idem | HECHO — en `lista-de-opciones.spec.ts` |
| H1.S2.M5 | `ap-estado-vacio` (ícono por motivo, borde punteado) / `ap-estado-de-error` (nuevo; `estado-de-pantalla` lo usa) / denegado = `estado-vacio` motivo `porPermiso` con ícono prohibido | Spec | idem | HECHO — `estado-de-error.spec.ts` |
| H1.S2.M6 | `ap-chip-estado`: tonos `pii` y `apagado`, punto `vivo` opcional | Spec | idem | HECHO — tonos en `tono.ts`; muestra en el catálogo (axe OK en claro y oscuro) |
| H1.S2.M7 | `ap-tarjeta-kpi` v2: ícono con tono, pista, hover eleva | Spec | idem | HECHO — `estado-de-error.spec.ts` (kpi) |

### H1.S3 — Fichas y formularios
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | `ap-pestanas` v2: estado en `?vista=`, teclado (flechas, Inicio, Fin), estilo segmentado | Spec de teclado | `ng test` + captura | HECHO — `estilo="segmentada"`, Inicio/Fin; la URL la enlaza el consumidor (`area-de-texto.spec.ts`) |
| H1.S3.M2 | `ap-seccion-clave-valor` + `ap-codigo-copiable`: cabecera gris, lista cebra, rótulo uppercase, valor mono copiable, tono por fila | Spec: copia al portapapeles con aviso accesible | idem | HECHO — `seccion-clave-valor.spec.ts` 2/2 + `ap-codigo-copiable` |
| H1.S3.M3 | `ap-cajon-lateral`: derecho, cabecera fija, Escape y velo cierran, foco atrapado y restaurado | Spec de foco (95.4.2) | idem | HECHO — `cajon-lateral.spec.ts` 2/2 (cerrar, Escape, foco de vuelta) |
| H1.S3.M4 | `ap-dialogo-de-confirmacion`: ámbar, frase escrita opcional, botón cargando | Spec: no confirma hasta escribir la frase | idem | HECHO — `dialogo-de-confirmacion.spec.ts` 3/3; `ap-dialogo` ganó `confirmarDeshabilitado` |
| H1.S3.M5 | `ap-lista-de-opciones` en `packages/ui` con tooltip de ayuda (hecho en H1.S2); mover `ap-area-de-texto` y que `cumplimiento/compartido` reexporte; `ap-campo` con tooltip | Specs de cumplimiento siguen verdes | `test:front` del backoffice | HECHO — `ap-area-de-texto` y `ap-lista-de-opciones` en `packages/ui` con tooltip; `ap-campo` con tooltip; `cumplimiento/compartido/campos.ts` reexporta; backoffice 839/839, lint y reglas OK |
| H1.S3.M6 | `ap-boton`: variantes `fantasma` y `peligro`; `cargando` con texto | Spec | `ng test` | DESCARTADO — ya existían las dos variantes y el estado cargando con girador + texto (`boton.ts`) |

## H2 — Shell del backoffice
**CA:** Dado un operador con sesión, cuando abre el backoffice, entonces ve la barra lateral con grupos plegables e íconos, la cabecera fija con migas, buscador global, (campana) y menú de usuario; en el celular, hamburguesa y cajón.
**DoD:** specs del shell verdes · capturas 3 anchos × 2 temas · e2e del shell.
**Estado:** A MEDIAS — hecho todo salvo la campana (va con H5.S0, backend real) y el retiro de las bandas pantalla por pantalla (H4)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Menú con grupos plegables y subrutas (`nucleo/secciones.ts` → árbol: Inicio · Operación[…] · Control[Cumplimiento, Admisiones] · Datos[Catálogo de datos, Catálogo de flujos] · Sistemas[servicios, despliegues, base de datos, respaldos, proveedores, outbox, webhooks, accesos, incidentes] · Finanzas[Contabilidad…, Publicidad…] · Plataforma[Configuración…, Ayuda]); estado abierto recordado por sesión; grupo abierto si contiene la ruta activa | Spec: el grupo de la ruta activa arranca abierto | `ng test layout/**` + capturas | HECHO — `nucleo/menu.ts` (árbol, `migasDe`) + `layout/menu-lateral.ts`; `menu.spec.ts` 4/4, `menu-lateral.spec.ts` 3/3. Se retiró el sub-menú propio de Sistemas (`layout/sistemas/shell-sistemas.ts`): duplicaba al principal y era ilegible en oscuro (`--g100` de fondo) |
| H2.S1.M2 | Cabecera fija: migas + título de la vista; menú de usuario (nombre/rol, Perfil → `/configuracion`, Salir) | Spec | idem | HECHO — `shell-financiero.ts` + `menu-de-usuario.ts`; spec del shell 4/4 |
| H2.S1.M3 | Buscador global (`⌘K`): combobox sobre un índice local de pantallas + tablas del catálogo; recientes en `sessionStorage` | Spec: teclado ↑↓ Enter Esc; abre la ruta | idem | HECHO — `nucleo/indice-de-busqueda.ts` + `layout/buscador-global.ts`; `buscador-global.spec.ts` 3/3 |
| H2.S1.M4 | Campana de avisos internos: lista no leídos, marcar leído, enlace al destino; consume H5.S0 | Spec + e2e | idem | TODO — se construye junto con H5.S0 (backend real), no como fachada |
| H2.S1.M5 | Explicador de la vista en el shell (`nucleo/explicador-de-vista.ts` + `ap-nota-de-vista`); cada pantalla fija su texto al migrar (H4) y pierde la banda | Spec: la banda ya no existe en ninguna ruta | `grep -rc banda-de-proposito rutas` → 0 al cerrar H4 | A MEDIAS — el shell ya muestra la nota; las 48 bandas se retiran pantalla por pantalla en H4 |
| H2.S1.M6 | Cajón móvil con foco atrapado; `main` con fade-in (reducido con `prefers-reduced-motion`) | e2e móvil sin desborde | `playwright` shell | HECHO — `e2e/shell.e2e.ts` 3/3: capturas 3 anchos × 2 temas (`evidencia/H2/h2-shell-outbox-*`), menú de usuario, buscador con ⌘K y teclado, cajón del celular; sin desborde y contraste axe OK |

## H3 — Catálogo de datos exactamente como Atlas
**CA:** Dado un operador con `TABLERO_SISTEMAS_VER`, cuando abre Catálogo de datos, entonces ve lista, ficha con 4 pestañas y edición en página aparte con confirmación, con las piezas enumeradas en §4; con `CATALOGO_DATOS_EDITAR` guarda ficha y revisa columnas.
**DoD:** specs + e2e con capturas · `verificar_criterios` del CU-130 y del CU nuevo de revisión por columna · en TEST, kill-test.
**Estado:** TODO

### H3.S1 — Backend que falta
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Entidad `revision_columna` (esquema, tabla, columna, decisión, confianza, motivo, revisado_por, en) en el PlantUML + CU-131 en la bóveda | `generar_ddl.py` y `verificar_boveda.py` OK | comandos | HECHO — `revision_columna` (UQ esquema+tabla+columna, CK decisión/confianza), CU-131 en la bóveda (TODO OK), catálogo regenerado (339 tablas) |
| H3.S1.M2 | `PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/columnas/{columna}/revision` + `GET` incluye `revision` por columna (aditivo); bitácora | `CU131Test` + web test | `integrationTest`, `webTest`, `verificar_criterios` | HECHO — `evidencia/H3-S1-auditoria.txt`: test 34, barrido 2, web 54, integración 60 (CU131Test 5/5 contra PostgreSQL), criterios sin divergencias; Postman y clientes regenerados |
| H3.S1.M3 | Conteo por esquema/módulo para el filtro «Bloque (n)» y total por estado para las cifras → ya viene todo el catálogo; se calcula en el cliente. **No hace falta endpoint.** | — | — | DESCARTADO (el catálogo viaja entero) |

### H3.S2 — Lista
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S2.M1 | Cabecera de página (ícono base de datos, «Catálogo de datos», descripción) + nota de la vista | Captura = anatomía Atlas | e2e + captura | TODO |
| H3.S2.M2 | Cifras: Tablas · Activas · Sin revisar · Con datos personales · Hacia el retiro, con ícono y tono | Spec de métricas (ya existe) + captura | idem | TODO |
| H3.S2.M3 | Barra de filtros v2: buscador con tooltip; **Esquema (n)** · Módulo (n) · Estado · Revisión; Limpiar | Spec: conteos por opción | idem | TODO |
| H3.S2.M4 | Tabla v2: Esquema · Tabla (mono, enlace) · Módulo (insignia) · Responsable · Datos personales (insignia pii) · Financiera · Crítica · Retención (mono) · Revisión (insignia) · Estado (insignia); paginación v2 | Spec de columnas + captura 3×2 | idem | TODO |

### H3.S3 — Ficha
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S3.M1 | Cabecera: eyebrow «Catálogo de datos», título `esquema.tabla`, descripción = qué es; acciones: «Editar ficha» (solo con permiso) + insignias PII/Revisión/Estado | Spec + captura | e2e | TODO |
| H3.S3.M2 | Pestañas «Vista general · Columnas · Relaciones · Gobierno» en `?vista=` | Spec de URL | idem | TODO |
| H3.S3.M3 | Vista general: 4 secciones clave–valor (Identificación técnica: Esquema, Tabla, Servicio dueño, Append-only · Contexto de negocio: Módulo, Responsable, Qué es, Para qué sirve, Por qué existe · Clasificación: Datos personales, Financiera, Secretos · Responsabilidad: Crítica, Retención, Revisión, Revisado por, Actualizado) | Captura | idem | TODO |
| H3.S3.M4 | Columnas: cifras (Columnas, Datos personales, Financieras, Descritas, Aprobadas) + filtros (buscador, Sensibilidad, Revisión) + tabla (Columna mono · Tipo insignia · Negocio · Sistema · Sensibilidad insignia · Nulos · Revisión · Acciones «Revisar») | Spec + captura | idem | TODO |
| H3.S3.M5 | Cajón «Revisar columna X»: Decisión, Confianza, Motivo (obligatorio al rechazar) → PUT de H3.S1.M2 | Spec: rechazar sin motivo no envía | idem | TODO |
| H3.S3.M6 | Relaciones: dos tablas (apunta a / apuntan a) con enlace; Gobierno: 3 secciones clave–valor | Captura | idem | TODO |

### H3.S4 — Edición
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S4.M1 | Ruta `catalogo-de-datos/:tabla/ficha`: cabecera (eyebrow «Formulario de catálogo», «Configurar tabla», Volver) + tarjeta «Ficha de negocio» (Responsable, Retención, Estado, Revisión, Nota) con tooltip por campo + tarjeta «Crítica para auditoría» | Spec + captura | e2e | TODO |
| H3.S4.M2 | Diálogo de confirmación «Guardar ficha de la tabla — Se actualizará la ficha de X (n campos)»; PUT solo con lo que cambió; AP-CU130-01 explicado y borrador conservado | Spec (los 4 de `seccion-de-gobierno.spec` migran acá) | idem | TODO |
| H3.S4.M3 | Retirar el formulario incrustado de la ficha | `grep seccion-de-gobierno` → 0 | — | TODO |

## H4 — Migrar las 41 pantallas a la anatomía nueva
**CA:** Dada cualquier ruta del backoffice, cuando se abre, entonces usa cabecera de página, nota de vista, barra de filtros v2 / tabla v2 / paginación v2 en los listados, pestañas + secciones clave–valor en las fichas, tarjetas con confirmación en los formularios; sin `banda-de-proposito` ni `h1` sueltos.
**DoD:** por pantalla: spec verde + captura 3×2 mirada; al cierre: `test:front` 100 %, e2e completo, `verificar_frontend.py`.
**Estado:** TODO

Una microtarea por pantalla (41), con el mismo CA/DoD; se abren de a una en el orden del menú. Se lista acá para que ninguna quede afuera:

| ID | Pantalla (ruta) | Tipo | Estado |
|---|---|---|---|
| H4.S1.M1 | `tablero` (inicio por rol) | tablero: cifras + accesos rápidos + «postura» | TODO |
| H4.S1.M2–M4 | `tablero/financiero`, `/sistemas`, `/comercial` | tablero | TODO |
| H4.S1.M5 | `tablero/catalogo-de-flujos` (si existe) | lista | TODO |
| H4.S2.M1–M10 | operación: `operacion` (bandejas), `billetera/:id`, `reclamos`, `solicitudes-escaladas`, `retiros`, `retiros/:id`, `custodia`, `cierres-diarios`, `reversos`, `desembolsos`, `estado` | lista / ficha / formulario | TODO |
| H4.S3.M1–M14 | cumplimiento: `verificaciones`, `alertas`, `alertas/:id`, `casos`, `casos/:id`, `uif/reportes`, `uif/requerimientos`, `continuidad`, `actas`, `actas/:id`, `riesgos/eventos`, `seguridad/incidentes`, `reclamos/:id`, `organizadores/:id/habilitacion` | lista / ficha / formulario | TODO |
| H4.S4.M1–M2 | admisiones, `admisiones/:id` | lista / ficha | TODO |
| H4.S5.M1–M10 | sistemas: inicio, `servicios`, `despliegues`, `base-de-datos`, `respaldos`, `proveedores`, `outbox`, `webhooks`, `accesos`, `incidentes` | lista | TODO |
| H4.S6.M1–M6 | contabilidad: inicio, `periodo/:id`, `presupuesto`, `compras`, `cobros`, `activos/:id`, `estados/:id` | lista / ficha | TODO |
| H4.S7.M1–M6 | publicidad: `anunciantes`, `campanas`, `campanas/:id/aprobacion`, `moderacion`, `desempeno`, `liquidacion` | lista / ficha | TODO |
| H4.S8.M1–M4 | configuración: inicio, `tarifario`, `mensajeria`, `reportes` | formulario / lista | TODO |
| H4.S9.M1 | ayuda | índice | TODO |


## H5 — Módulos del admin portal que no tenemos (§7, «Nuevo» y «Adaptar»)
**CA:** Dado el menú del backoffice, cuando un operador con permiso lo recorre, entonces encuentra cada módulo de §7 marcado Nuevo/Adaptar, con la anatomía de H1 y conectado a datos reales (contrato OpenAPI del servicio dueño), nunca a un mock.
**DoD:** por módulo: contrato + CU en la bóveda + tests del servicio + specs + e2e con capturas; `verificar_criterios` sin divergencias.
**Estado:** TODO

Orden (valor para Pablo primero, backend más barato primero):
| ID | Módulo | Servicio dueño / contrato | Qué se construye | Estado |
|---|---|---|---|---|
| H5.S0 | Avisos internos para operadores (la campana) | `notificaciones`: `aviso_interno` (destinatario rol/usuario, título, texto, enlace, leído_en) + `GET /avisos-internos`, `POST …/{id}/leido`; los casos de uso existentes emiten avisos (alerta nueva, caso asignado, ficha a revisar, despliegue fallido) | campana + bandeja «Mis avisos» | TODO |
| H5.S1 | Registro del sistema (bitácora) | `auditoria`: `GET /auditoria/bitacora` paginado por cursor (entidad, acción, actor, rango) + detalle | lista + cajón de detalle con valor anterior/nuevo (JSON) | TODO |
| H5.S2 | Catálogos operativos | `tarifas`/`catalogo`: `GET /catalogos`, `GET /catalogos/{codigo}/elementos`, `PATCH …/elementos/{id}` (activo) con bitácora | lista de catálogos (código, nombre, dueño, elementos, vigente desde) + ficha con elementos y conmutador Activo/Inactivo | TODO |
| H5.S3 | Revisión del catálogo (cola) | ya existe (CU-130/131) | bandeja de pendientes con acción «Revisar» | TODO |
| H5.S4 | Gobierno de datos: datos personales y políticas | catálogo + `politica_retencion` | dos vistas de solo lectura | TODO |
| H5.S5 | Solicitudes de privacidad | `auditoria` CU-07 (existe) | bandeja + ficha + resolver | TODO |
| H5.S6 | Procesos automáticos | `organizador` CU-96 (existe) | lista + ficha + reintentar | TODO |
| H5.S7 | Expedientes (archivos) | `cumplimiento` KYC (existe, CU-02/05) | expediente por persona + registro de lectura (R-SEG-02) | TODO |
| H5.S8 | Soporte: tickets + base de conocimiento | `auditoria` `ticket_soporte` (CU-52) + `ayuda` | bandeja + ficha; la ayuda pasa a «Base de conocimiento» | TODO |
| H5.S9 | Linaje e impacto | catálogo (relaciones) | grafo por tabla + impacto | TODO |
| H5.S10 | Esquema de datos: versión e historial | `auditoria` (hash de `aplicar.sql` en el catálogo) | versión desplegada + cambios por commit (desde git en el build) | TODO |
| H5.S11 | Vistas del negocio | `transparencia`/`grupos` (contratos existentes de solo lectura) | consultas gobernadas con huella | TODO |
| H5.S12 | Consentimientos | `identidad` `consentimiento` | versiones + aceptaciones | TODO |
| H5.S13 | Deriva de permisos | backoffice (local) | declarados vs usados | TODO |
| H5.S14 | Glosario y dominios · Contenido de la app · Reglas de calidad · Procesos | contratos nuevos | se abren al cerrar lo anterior | TODO |
Cada subtarea se descompone en microtareas (contrato · CU · servicio · tests · pantalla · e2e · capturas) al abrirse, con el mismo formato de H3.

## H6 — Tipos de usuario: usuarios, roles y permisos internos como Atlas
**CA:** Dado un administrador de identidad, cuando abre Administración, entonces ve Usuarios internos (lista con Usuario, Nombre, Departamento, Cargo, Roles, Segundo factor, Debe cambiar contraseña, Estado; alta con contraseña provisional enviada al correo; ficha con Organización, Código, Departamento, Cargo, Segundo factor, Bloqueo, Roles, Permisos efectivos; editar/desactivar con motivo; roles con motivo; desbloqueo con motivo), Roles internos (Rol, Estado, Permisos, Descripción, departamento) y Permisos internos (Permiso, Módulo, Acción, Riesgo, Exige motivo, Estado, Descripción); y Perfil.
**DoD:** CU-08 ampliado + CU nuevos en la bóveda · `identidad` tests · specs · e2e · capturas · en TEST.
**Estado:** TODO (Pablo confirmó el 2026-10-18: los roles de Atlas adaptados al pasanaku).

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Modelo: `permiso.nivel_riesgo` (BAJO/MEDIO/ALTO) y `permiso.exige_motivo`; `rol.departamento`, `rol.descripcion`; `usuario_interno.departamento`, `cargo`, `debe_cambiar_contrasena` (lo que falte) en el PlantUML | DDL y bóveda OK | `generar_ddl.py`, `verificar_boveda.py` | TODO |
| H6.S1.M2 | Lista de roles adaptada al pasanaku a partir de los 20 de Atlas (sin crédito/comercios; + organizador, tesorería, publicidad) con sus permisos, en `seeders/minimos` | Semillas idempotentes | `generar_semillas.py` ×2 | TODO |
| H6.S1.M3 | `identidad`: `GET /usuarios-internos` (filtros estado, rol, búsqueda), `POST` alta con contraseña provisional por correo, `GET/PATCH /{id}` con motivo, `PUT /{id}/roles` con motivo (CU-08), `POST /{id}/desbloquear` con motivo; `GET /roles`, `GET /permisos` | tests + web tests + autorización negativa | `identidad` suite | TODO |
| H6.S2.M1–M6 | Pantallas: usuarios (lista, alta, ficha, editar, roles, desbloqueo), roles, permisos, perfil | specs + e2e + capturas | backoffice suite | TODO |

## H7 — Verificación y entrega
**CA:** Todo lo anterior en verde, mirado y en TEST.
**DoD:** `test:front` backoffice y `packages/ui` · e2e completo (un worker) · `test:a11y` del backoffice (comparar con línea base: hoy vence por plazo) · capturas de las 41 pantallas en 3×2 miradas · PR a `test` y `dev` · reconstrucción en el host · captura en vivo · `REPORTE.md`.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H7.S1.M1 | Regresión completa y gates | todo verde | salidas en `evidencia/` | TODO |
| H7.S1.M2 | Capturas 41 × 3 × 2 miradas | ninguna desborda; contraste axe OK | e2e `capturas-f2` | TODO |
| H7.S1.M3 | PRs #test y #dev mergeados; TEST reconstruido; verificación en vivo con el admin (Pablo o acceso a credenciales) | kill-test PASS | captura en vivo | TODO |

## Orden de ejecución y estimación honesta
H0 → H1 (los componentes son la base de todo) → H2 → H3 (primera pantalla completa; **aquí Pablo revisa antes de seguir**) → H6 (tipos de usuario: pantallas de usuarios/roles/permisos, que son las que más se parecen a Atlas) → H4 por secciones → H5 (módulos nuevos, en el orden de la tabla) → H7. Son ~130 microtareas; H1, H3 y H5 son dos tercios del trabajo. **Checkpoint con Pablo al cerrar H3**: si la ficha del catálogo no convence, se corrige el patrón antes de replicarlo en 41 pantallas.

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Ambigüedad 1 (catálogos operativos) | Trabajo en la dirección equivocada | Pregunta explícita en la entrega del plan; H6 reservado |
| `tabla-de-datos` v2 rompe las 20 pantallas que la usan | Regresión amplia | Mantener la API de entradas; specs viejos como red; migrar en H4 de a una |
| Archivos de más de 200 líneas (`verificar_frontend.py`) | Bloquea CI | Componentes partidos en plantilla + estilos + dominio |
| Revisión por columna exige tabla nueva en TEST | Imagen `esquema` y despliegue | Mismo camino que CU-130 (verificar `aportaya/esquema:test` antes del push) |
| Credenciales del admin de TEST | No puedo hacer el kill-test en vivo | Pablo lo hace o habilita el acceso |
