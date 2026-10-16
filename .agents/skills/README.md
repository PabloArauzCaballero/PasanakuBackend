# Skills de AportaYa

Cada carpeta es una skill: instrucciones de **cómo se hace el trabajo acá**, no
documentación del producto. Se invocan solas cuando la tarea coincide con su
descripción, o a mano con `/<nombre>`.

La bóveda (`docs/`) dice **qué** hay que construir. Las skills dicen **cómo**.

## Método y arquitectura

| Skill | Cuándo |
| --- | --- |
| `arrancar-carril` | **Primera acción al abrir el chat de un carril**, antes de todo |
| `frontera-transaccional` | Paso 0 de todo caso de uso: qué va todo-junto-o-nada |
| `arquitectura-atomica` | Antes del primer archivo de cualquier funcionalidad |
| `implementar-desde-boveda` | Al empezar a programar un caso de uso |
| `plan-por-fases` | Cuando el alcance abarque varios módulos o infraestructura |
| `decisiones-adr` | Al elegir una librería o cambiar algo caro de revertir |
| `codigo-limpio` | Al escribir o revisar cualquier código |
| `revision-codigo` | Al revisar un PR |
| `debido-proceso` | Cuando una decisión termine con alguien perdiendo algo |
| `definicion-de-terminado` | Antes de decir que algo está listo |
| `entorno-monorepo` | Al mover paquetes, dependencias o scripts |
| `git-flujo` | Antes de commitear y al abrir el PR |
| `glosario-dominio` | Al nombrar cualquier cosa |

## Especificación: la bóveda

| Skill | Cuándo |
| --- | --- |
| `boveda-modelo` | Al tocar `docs/entidades/*.puml` o regenerar la bóveda |
| `caso-de-uso` | Al escribir o cambiar un caso de uso |
| `restriccion` | Cuando una regla deba ser imposible de violar |
| `norma-nueva` | Cuando aparezca una resolución, circular o umbral nuevo |
| `semillas-catalogos` | Al cambiar un valor de catálogo en `seeders/` |

## Construcción

| Skill | Cuándo |
| --- | --- |
| `contratos-api` | Antes de implementar cualquier endpoint |
| `back-spring` | Al escribir el backend |
| `datos-jooq` | Al escribir consultas y repositorios |
| `servicios-y-sagas` | Al llamar a otro servicio, consumir un evento o cruzar una operación |
| `dinero-decimal` | Cada vez que aparezca un importe |
| `trabajos-outbox` | Al disparar efectos fuera de la transacción |
| `errores-api` | Al devolver o traducir un error |
| `idempotencia-reintentos` | En todo endpoint con efecto y todo webhook |
| `seguridad-aplicacion` | **Antes de escribir cualquier endpoint, consulta, adaptador, Dockerfile o pantalla**, y al revisar un PR |
| `autenticacion-jwt` | Al crear un endpoint o tocar login, refresh y permisos |
| `roles-y-accesos` | Al decidir qué permiso exige algo, o dar de alta a un operador |
| `seguridad-sesion-rls` | En toda consulta con políticas de fila |
| `lecturas-proyecciones` | Listados pesados, extractos, vistas y réplica de lectura |
| `extraccion-de-datos` | Al crear un reporte o exportar algo con datos personales |
| `motor-de-reglas` | Al escribir una regla de cumplimiento, antifraude o automatización |
| `automatizacion-tareas` | Al escribir un trabajo programado o un motor de tareas |
| `pruebas-cu` | Al implementar cualquier caso de uso |

## Interfaz

| Skill | Cuándo |
| --- | --- |
| `disenar-frontend` | Al crear o modificar cualquier pantalla |
| `movil-flutter` | Al trabajar en la app (Flutter) o en `packages/diseno_flutter` |
| `web-angular` | Al trabajar en el backoffice, el sitio público o `packages/ui` (Angular) |
| `web-backoffice` | El comportamiento del backoffice: tablas, expedientes, plazos, permisos |

## Dominio

| Skill | Cuándo |
| --- | --- |
| `kyc-onboarding` | Alta, verificación, niveles, contrato de adhesión |
| `contabilidad-partida-doble` | Cualquier flujo que mueva dinero |
| `qr-pagos` | Cobro con QR, pasarelas y conciliación bancaria |
| `desembolsos-payouts` | Cualquier salida de dinero: retiro, entrega, devolución |
| `reembolsos-disputas` | Devolver un cobro o responder un contracargo |
| `proveedores-externos` | Al integrar, enrutar o dar de baja un proveedor |
| `facturacion-sin` | Comisiones, tarifario, impuestos y factura |
| `gobernanza-grupo` | Ciclo del grupo, cupos, turnos y acuerdos |
| `emparejamiento-ingreso` | Postulación, emparejamiento, invitaciones y referencias |
| `organizador-habilitacion` | Habilitar, medir, sancionar o dar de baja a un organizador |
| `sorteo-transparencia` | Sorteo verificable, cadena de bloques y reputación |
| `reputacion-social` | Insignias, reseñas y certificados de reputación |
| `garantia-mora-cobranza` | Mora, fondo de garantía, sanciones y cobranza |
| `alertas-riesgo-temprano` | Scoring, métricas de grupo y alertas antes del incumplimiento |
| `notificaciones-consentimiento` | Cualquier aviso al usuario |

## Cumplimiento y control

| Skill | Cuándo |
| --- | --- |
| `cumplimiento-uif` | Umbrales, debida diligencia, monitoreo y reportes de sospecha |
| `reportes-regulatorios` | Cualquier remisión periódica con plazo y acuse |
| `reclamos-consumidor` | Circuito de reclamos y transparencia de información |
| `observabilidad` | Rastro, indicadores, incidentes y eventos de riesgo |
| `gobierno-comites` | Comités, actas, riesgo de producto y oficial de cumplimiento |
| `indicadores-tablero` | Al crear un KPI o armar un tablero |
| `plazos-habiles` | Cada vez que aparezca 'X días hábiles' |

## Operación y entrega

| Skill | Cuándo |
| --- | --- |
| `ci-calidad` | Al configurar el CI o cuando un gate bloquee el merge |
| `resiliencia-rendimiento` | Al integrar un proveedor, dimensionar pools o medir |
| `despliegue-contenedores` | Docker, NGINX, Kubernetes y manifiestos |
| `respaldos-restauracion` | Respaldos, punto en el tiempo y ensayo de restauración |
| `documentacion-entregables` | Al documentar, agregar un endpoint o preparar la entrega |

## Reglas comunes a todas

1. **La bóveda manda.** Si el código y la especificación divergen, se corrige el
   código —o se corrige la bóveda primero, y en el mismo PR.
2. **Ninguna cifra regulatoria ni comercial en el código.** Van a catálogo, con
   vigencia y cita.
3. **Denegar por omisión.** Falta el límite, la licencia o la política: se rechaza.
4. **Nada se edita.** La corrección es un registro nuevo que compensa al anterior.
5. **La garantía vive en la base.** La aplicación valida para dar buen mensaje.

## Estándar de la casa (heredadas de PasanakuPromptManager)

Estas skills vienen del estándar cross-repo de la empresa (`.claude/rules/` + `.claude/skills/` del repo `PasanakuPromptManager`), instaladas junto con `.claude/hooks/` y `.claude/rules/` para este encargo (regla 10, fase "instalación del estándar"). Se suman a las skills propias de AportaYa de arriba, no las reemplazan.

| Skill | Cuándo |
| --- | --- |
| `accessibility-testing` | Usar al agregar tests de a11y a un componente o flujo, al configurar la etapa de accesibilidad del CI, o antes de declarar accesible una pantalla. |
| `accounting-double-entry` | Usar al modelar o tocar cualquier registro contable, un asiento, un movimiento de saldo, un módulo de activos/pasivos, o antes de cerrar un cambio que mueva dinero. |
| `agent-orchestration` | Usar antes de lanzar un subagente, cuando haya que recorrer muchos archivos y no quieras ese material en tu contexto, al repartir un trabajo grande en lotes paralelos, o cuando un trabajo delegado volvió incompleto, duplicado o con archivos pisados. |
| `agent-resource-control` | Usar antes de lanzar un build, una suite, un E2E, un servidor de desarrollo o un subagente; cuando la máquina se pone lenta o hay fallos intermitentes; y al cerrar un turno para limpiar lo que quedó corriendo. |
| `aml-sanctions-screening` | Usar al construir o tocar cribado de listas, alertas de cumplimiento, reglas de monitoreo, reportes regulatorios, o cualquier decisión de bloquear una cuenta u operación por sospecha. |
| `angular-development` | Usar al crear o revisar cualquier componente, servicio, ruta, guard o interceptor, al migrar código con NgModules, `@Input` o `*ngIf`, o cuando una vista no se actualiza. |
| `angular-forms` | Usar al crear o revisar cualquier formulario, al mapear un 422 de la API a la UI, al construir un formulario desde una definición o al decidir entre reactive forms y Signal Forms. |
| `angular-signals-state` | Usar al decidir dónde vive un dato, al traer datos async a una vista, al escribir un store, o cuando un `effect` setea otro signal. |
| `angular-ssr-hydration` | Usar al crear un componente o servicio que toque APIs de navegador, al integrar una librería de DOM, al configurar rutas del servidor, o ante un hydration mismatch o un parpadeo al cargar. |
| `angular-testing` | Usar al escribir o arreglar cualquier spec de Angular, al migrar desde Jasmine/Karma, o cuando un test solo pasa con `detectChanges` a ciegas. |
| `anti-hallucination-guard` | Usar antes de introducir cualquier pieza nueva, al interpretar un requisito dudoso, al usar una API que no acabás de ver, y al redactar afirmaciones sobre el sistema. |
| `api-gateway-bff` | Usar al exponer un endpoint hacia afuera, al armar la pantalla que necesita datos de varios servicios, al agregar autenticación o rate limiting en el borde, o cuando el gateway empieza a tener reglas de negocio. |
| `api-openapi-docs` | Usar al escribir o revisar un spec OpenAPI, documentar un endpoint nuevo, o antes de publicar/versionar una API pública o interna. |
| `api-pentest` | Usar al evaluar una API NestJS antes de un release, tras cambiar guards, roles o DTOs, o al auditar un servicio que expone datos de participantes o de otros tenants. |
| `api-testing` | Usar al crear o modificar cualquier endpoint, al cambiar un guard, un DTO o una regla de permisos, y antes de declarar probado un backend cuyo único respaldo son unitarios con el ORM mockeado. |
| `astro-development` | Usar al construir o modificar la landing/sitio de marketing, al agregar una isla interactiva, al modelar contenido en colecciones, o al decidir qué va en Astro y qué en la app Angular. |
| `async-messaging-events` | Usar al publicar un evento, escribir un consumidor, sacar trabajo del request (emails, notificaciones, integraciones), coordinar un flujo que cruza módulos o servicios, o diagnosticar mensajes duplicados, perdidos o fuera de orden — incluido el aviso que se duplica o nunca sale cuando falla el guardado (outbox). |
| `atomic-design-components` | Usar antes de crear cualquier componente de UI, al revisar un PR que agrega uno, al ubicar algo como átomo/molécula/organismo, o al detectar dos componentes que hacen casi lo mismo. |
| `audit-trail-history` | Usar al diseñar una tabla de auditoría, al agregar un endpoint que lee o muta datos financieros, financieros o de permisos, al implementar "ver historial de cambios", o al revisar si una acción sensible deja rastro. |
| `auth-session-pentest` | Usar al evaluar login, registro, reset de contraseña, emisión y refresh de tokens o cierre de sesión, y tras cualquier cambio en el flujo de identidad. |
| `authn-identity` | Usar al diseñar o revisar login, registro, reset de contraseña, emisión de tokens, cierre de sesión o cualquier flujo que decida QUIÉN es el actor. |
| `authz-access-control` | Usar al crear o revisar cualquier endpoint, query de listado, export, job o acción por link que lea o mute datos de alguien, y antes de cerrar un cambio que toque roles o permisos. |
| `backend-development` | Usar al diseñar un servicio nuevo, un endpoint, un job asíncrono, al encontrar un controller con reglas de negocio o consultas a la base adentro, o al revisar si una API está lista para producción. |
| `backend-observability` | Usar al instrumentar un servicio nuevo, al investigar un incidente sin suficiente visibilidad, o al definir qué alertar y qué mostrar en un dashboard. |
| `background-jobs-scheduling` | Usar al escribir un cron, un worker, un proceso por lotes, un recordatorio de aporte o un reproceso masivo, y al diagnosticar un job que corrió dos veces, no corrió o quedó colgado. |
| `backup-restore-dr` | Usar al diseñar o revisar la estrategia de backup de un servicio, antes de una operación destructiva o un cambio de esquema riesgoso, al preparar un simulacro, o al afirmar que "hay backups". |
| `bug-reporting-standard` | Usar al abrir un issue de bug, al documentar un fallo hallado en QA, al triar el backlog de defectos, o al convertir un "no anda" en algo que otra persona pueda reproducir y arreglar. |
| `caching-strategy` | Usar antes de agregar cualquier cache, al revisar un endpoint lento que "se arregla cacheando", al diseñar catálogos o directorios públicos de alto tráfico, y al diagnosticar datos viejos, respuestas de otro usuario o memoria que crece. |
| `ci-cd-pipeline` | Usar al diseñar el pipeline de un repo o servicio nuevo, al ordenar o acelerar las etapas, o al decidir qué bloquea un merge y qué un deploy. |
| `claude-md-authoring` | Usar al crear o editar el CLAUDE. |
| `clean-code` | Usar al escribir, revisar o refactorizar cualquier código de producción, para nombrar variables/funciones/clases, dividir funciones largas, eliminar duplicación o decidir si un comentario sobra. |
| `code-complexity-metrics` | Usar al elegir qué refactorizar primero, al configurar reglas de complejidad en el linter, al revisar un archivo que "da miedo tocar", o al justificar con datos por qué un módulo necesita trabajo. |
| `code-efficiency` | Usar al revisar un endpoint lento, un loop sobre una colección grande, una query que crece con el dataset, o antes de agregar cache/paralelismo. |
| `code-quality-audit` | Usar al evaluar la dinero de un repo heredado, antes de decidir refactorizar vs reescribir, al hacer due diligence técnica, o en una revisión de calidad trimestral. |
| `code-quality-gates` | Usar al armar o endurecer el pipeline de un repo, al definir qué condiciones bloquean un PR, al agregar una métrica nueva al gate, o cuando la rama de integración se rompe seguido. |
| `code-review-standard` | Usar al revisar cualquier PR, al preparar un PR para pedir review, al decidir si un comentario debe frenar el merge, o al definir la política de revisión de un repo. |
| `collections-delinquency` | Usar al modelar o tocar mora, gestión de cobranza, incumplimiento, deuda, sanción, restricción de usuario o apelación, y antes de automatizar cualquier consecuencia negativa para un participante. |
| `color-systems` | Usar al construir la paleta de un producto nuevo, al agregar modo oscuro, al elegir un color de estado o acento, o al revisar por qué una interfaz "se ve genérica" o falla contraste. |
| `component-architecture-solid` | Usar al diseñar un componente, al revisar uno que creció con demasiados inputs o un `@switch` de negocio, o al decidir si partirlo, extenderlo por projection o inyectarle una dependencia. |
| `concurrency-and-locking` | Usar al diseñar o revisar una mutación que lee-y-luego-escribe, un endpoint que el cliente reintenta, un worker de cola, un contador, o ante datos duplicados o pisados de forma intermitente. |
| `consent-management` | Usar al construir o revisar cualquier flujo que comparta, busque, muestre o exporte datos de un participante a otro actor, al tocar directorios o búsquedas que expongan personas, y antes de dar por terminado un endpoint de lectura financiera. |
| `content-moderation-abuse` | Usar al construir o revisar cualquier superficie donde un usuario publica contenido que otros ven, al agregar reportar/bloquear/ocultar, o al diagnosticar spam, acoso o contenido que no debería estar visible. |
| `context-thrift` | Usar al arrancar cualquier tarea en un repo grande o multi-repo, antes de abrir un archivo largo, un log, un diff extenso o documentación externa, y cuando la sesión se alarga y hay que pasar hallazgos a notas durables en vez de seguir acumulando dumps en el chat. |
| `contribution-calendar` | Usar al generar o tocar el calendario de un grupo, al calcular un vencimiento, al programar recordatorios, al mover un turno, o ante un problema de fechas, husos horarios o mora disparada un día antes. |
| `coolify-databases-backups` | Usar al crear o tocar una base en Coolify, al programar o auditar backups, antes de un upgrade y en cada simulacro de restore. |
| `coolify-deployment` | Usar al crear o reconfigurar un recurso en Coolify, al conectar un repo, o cuando el deploy queda verde pero el dominio no responde. |
| `coolify-operations` | Usar al operar o diagnosticar la plataforma, automatizar deploys, dar acceso a alguien, o cuando un dominio deja de responder. |
| `css-architecture` | Usar al montar la base de estilos de un producto, al decidir dónde vive un estilo (global, capa, componente), al pelear con especificidad o `::ng-deep`, o al revisar CSS inconsistente o difícil de sobrescribir. |
| `dashboard-data-ui` | Usar al diseñar un dashboard, un panel analítico, una vista de reportes o cualquier pantalla con métricas, gráficos y tablas juntos, y al revisar por qué un tablero "abruma" o no responde ninguna pregunta. |
| `data-modeling-plantuml` | Usar al crear o modificar un diagrama ER o de clases, al agregar una tabla, columna o relación al modelo, al revisar un cambio de modelo antes de escribir código, o cuando un generador falla o adivina por un diagrama ambiguo. |
| `data-privacy-financial` | Usar en todo PR que toque datos de una persona o un importe atribuible, al agregar un log o una métrica, al exponer un endpoint, al armar datos de prueba, o antes de exportar cualquier cosa. |
| `data-quality-validation` | Usar después de cargar seeds o un dataset, tras aplicar un parche de esquema o un backfill, al validar un restore, cuando "los números no cuadran", y antes de declarar una carga o migración de datos como correcta. |
| `database-design` | Usar al diseñar un esquema nuevo, agregar una tabla o índice, escribir una migración, o revisar por qué una query es lenta o inconsistente. |
| `dead-code-duplication` | Usar al limpiar un módulo, al bajar el tamaño del bundle o del árbol de dependencias, después de quitar una feature, al configurar la etapa de duplicación del gate, o cuando "no sé si esto se usa todavía". |
| `dependency-management` | Usar antes de agregar cualquier dependencia nueva, al actualizar versiones, ante una alerta de vulnerabilidad, al configurar workspaces, o cuando un build falla "solo en CI" o "solo en mi máquina". |
| `deployment-verification-smoke` | Usar después de cualquier deploy a staging o producción, antes de anunciar "está en producción", tras un rollback, y al cerrar un carril cuyo criterio es "desplegado". |
| `disbursement-payouts` | Usar al modelar o tocar entrega de fondo, deducciones, cuenta bancaria del beneficiario, orden o intento de desembolso, confirmación o incidencia, y antes de cerrar cualquier cambio por el que salga plata del sistema. |
| `dispute-resolution` | Usar al modelar o tocar disputas, descargos, apelaciones, incidencias de entrega o cualquier flujo donde un participante cuestione una decisión del sistema. |
| `distributed-data-integrity` | Usar al diseñar un flujo que reparte una invariante de dinero entre servicios, al escribir un chequeo de cuadre, cuando un total no coincide entre dos pantallas, o al investigar una diferencia contable. |
| `distributed-tracing-correlation` | Usar al crear un servicio o un consumidor, al instrumentar un flujo de dinero, cuando un incidente obliga a cruzar logs por timestamp, o al revisar que un cambio no rompa la traza. |
| `docker-local-stack` | Usar al armar o arreglar el `docker-compose. |
| `dockerfile-production` | Usar al escribir o revisar un Dockerfile, al pasar de Nixpacks a Dockerfile, cuando la imagen pesa o el build es lento, o cuando el contenedor no se apaga limpio. |
| `e2e-failure-triage` | Usar apenas un test se pone rojo, cuando un test pasa "a veces", antes de tocar un spec que falla, o cuando alguien propone reintentar hasta que pase. |
| `e2e-playwright` | Usar al crear o modificar cualquier spec de Playwright, al cerrar un slice con UI ejecutable, al reproducir un bug de interfaz o antes de declarar verificado un flujo de usuario. |
| `edge-case-data-catalog` | Usar al diseñar casos de un endpoint, formulario o función, al escribir tests negativos, o al preguntarse "¿qué le meto para romperlo?". |
| `environment-secrets-config` | Usar al agregar una variable de entorno, al leer config en código, al preparar el deploy de un servicio, al revisar un `. |
| `error-handling-contract` | Usar al crear una excepción de dominio, decidir qué status devuelve un caso, escribir o revisar un exception filter, documentar errores en OpenAPI, o cuando el front no distingue dos fallos. |
| `eventual-consistency-read-models` | Usar al replicar datos de otro servicio, al construir un panel o listado que cruza servicios, cuando un usuario reporta que hizo algo y no lo ve, o al decidir si un número puede leerse de una réplica. |
| `evidence-and-verification` | Usar SIEMPRE antes de decir "listo", "funciona", "arreglado", "implementado" o "probado"; al cerrar un slice, un carril o un bug; y al redactar cualquier reporte de estado. |
| `exploratory-testing` | Usar al recibir una feature nueva o poco entendida, al buscar lo que los tests scripted no cubren, antes de un release para "sacudir" el sistema, o cuando algo "se siente raro" pero no hay un caso que lo capture. |
| `factual-discovery` | Usar al arrancar cualquier carril, feature o bug, antes de planificar o editar, y cada vez que estés por asumir cómo funciona algo en lugar de confirmarlo. |
| `file-uploads-media` | Usar al crear o revisar cualquier endpoint que reciba un archivo (foto de perfil, documento, adjunto de ficha, comprobante), al servir un archivo subido, o al diseñar dónde y cómo se guardan los medios. |
| `financial-close-reporting` | Usar al construir el cierre, un reporte financiero, un export, un dashboard con cifras de dinero, o al investigar por qué dos reportes del mismo periodo dan distinto. |
| `finish-your-turn` | Usar antes de terminar cualquier respuesta que entrega código, un fix, un carril o una investigación; cuando estás por escribir "el siguiente paso sería…", "quedaría pendiente…", "podrías probar…" o dejar un TODO; y para decidir cuándo SÍ corresponde parar y devolver con un bloqueo documentado. |
| `flutter-development` | Usar al crear o revisar cualquier widget, pantalla o servicio de la app móvil, al diagnosticar rebuilds de más o jank, o al decidir cómo partir un widget grande. |
| `flutter-state-architecture` | Usar al decidir dónde vive un dato, al traer datos asíncronos a una pantalla, al escribir un notifier/provider o un repositorio, o cuando la lógica de negocio se está filtrando dentro de los widgets. |
| `flutter-testing` | Usar al escribir o arreglar cualquier test de la app móvil, al decidir qué probar en qué nivel, o al proteger el tema y componentes contra regresiones. |
| `flutter-theming` | Usar al definir o revisar el tema de la app, al agregar un color o un estilo de texto, al implementar modo oscuro, o al alinear la app móvil con la identidad visual del producto web. |
| `frontend-accessibility` | Usar al construir o revisar cualquier componente, formulario, modal, navegación o página antes de darla por terminada, y como auditoría dedicada con lector de pantalla y axe cuando el cambio toca interacción o estructura del DOM. |
| `frontend-beautiful-ui` | Usar al pulir una pantalla ya estructurada, o para diagnosticar por qué una UI correcta funcionalmente "se ve genérica" o "hecha por IA". |
| `frontend-data-access` | Usar al crear el cliente de un endpoint nuevo, al registrar un interceptor, al tipar la respuesta de la API, o cuando un componente traga el error del backend. |
| `frontend-data-tables` | Usar al construir cualquier listado tabular (calendario, solicitudes, participantes, movimientos), al hacerlo responsivo, o al revisar una tabla lenta, ilegible o inutilizable en móvil. |
| `frontend-design-system` | Usar al crear el sistema de tokens de un producto nuevo, al agregar un tema o modo oscuro, al diseñar la API de un componente reutilizable, o al revisar por qué los estilos de un producto son inconsistentes entre pantallas. |
| `frontend-error-monitoring` | Usar al montar el reporte de errores de la app, al decidir qué se captura y qué se manda, al correlacionar un error del front con su traza en el backend, o al revisar por qué los errores de producción "no se ven". |
| `frontend-forms-ux` | Usar al diseñar o revisar cualquier formulario (alta de usuario/profesional, calendario, cotización), cuando un formulario "frustra" o pierde datos, o al decidir cómo mostrar los errores de validación y del servidor. |
| `frontend-i18n-l10n` | Usar al agregar cualquier texto visible, al preparar el producto para un segundo idioma o región, al formatear fechas/montos, o al revisar por qué una traducción quedó rota o antinatural. |
| `frontend-motion` | Usar al animar entradas/salidas, transiciones de ruta, listas, gestos, animación ligada al scroll o cualquier microinteracción — para decidir duración, easing y herramienta sin romper accesibilidad, SSR ni performance. |
| `frontend-navigation-ia` | Usar al diseñar la estructura de navegación de un producto o sección, al agregar una ruta o un ítem de menú, al armar el nav de un rol nuevo (organizador, participante, admin), o al revisar por qué los usuarios "no encuentran" una función. |
| `frontend-performance` | Usar al construir cualquier pantalla con datos, medios o interacción pesada, antes de mergear un cambio que toque el bundle o el árbol de render, o al investigar por qué una pantalla "se siente lenta". |
| `frontend-responsive-layout` | Usar al maquetar cualquier pantalla o componente nuevo, al adaptar uno existente a más anchos, o al revisar por qué algo se rompe entre mobile y desktop. |
| `frontend-security` | Usar al renderizar HTML dinámico, al integrar contenido de terceros, al decidir dónde vive el token de sesión, o al revisar cualquier vista que muestre datos de participantes. |
| `frontend-ui-design` | Usar al arrancar una pantalla o componente nuevo, al definir tokens de layout, o al revisar si una interfaz existente comunica jerarquía y es usable antes de pulir el detalle visual. |
| `frontend-ux-states` | Usar al construir cualquier pantalla o componente que dependa de una llamada a red, al revisar un flujo antes de darlo por terminado, o cuando un bug reportado es "la pantalla queda rara" en algún caso límite. |
| `git-workflow-multirepo` | Usar antes del primer commit de un carril, al coordinar cambios que tocan más de un repo, o al decidir cómo integrar una rama sin romper a los demás repos. |
| `github-actions-ci` | Usar al crear o modificar un workflow, al endurecer uno existente, o cuando una corrida falla y hay que diagnosticarla. |
| `github-branch-protection-rulesets` | Usar al configurar un repo nuevo, al endurecer la rama de integración o la de producción, al escribir o depurar un CODEOWNERS, al decidir quién puede saltear una regla, o cuando un PR queda bloqueado por un check que nunca reporta. |
| `github-cli-automation` | Usar al abrir o consultar un PR, diagnosticar una corrida fallida, automatizar una tarea repetida sobre varios repos, o antes de que un agente ejecute cualquier comando `gh` que escriba. |
| `github-issues-projects` | Usar al crear plantillas de issues, al definir o limpiar labels, al armar el tablero de un equipo o de un carril, al hacer triage del backlog, o al partir un carril grande en issues rastreables entre varios repos. |
| `github-multirepo-coordination` | Usar al abrir un carril que toca más de un repo, al ordenar los merges, o al romper un contrato que otros consumen. |
| `github-pull-requests` | Usar al abrir, describir, actualizar o mergear un PR, al partir un cambio grande en varios, o al revisar si un PR está listo para pedir review. |
| `github-releases-versioning` | Usar al cortar una versión, al definir el esquema de versiones de un repo nuevo, al configurar las categorías de las release notes, al publicar un release candidate, al sacar un hotfix sobre producción, o al decidir si un cambio es major, minor o patch. |
| `github-repo-standards` | Usar al crear un repo nuevo, al normalizar uno existente, al agregar una plantilla, o al decidir qué se comparte a nivel organización. |
| `github-security-features` | Usar al crear un repo, al auditar la postura de seguridad de uno existente, al emitir o rotar un token, al dar acceso a una persona o bot, o cuando aparece una alerta de secreto o de dependencia. |
| `group-discovery-matching` | Usar al construir la búsqueda o el listado público de grupos, el flujo de postulación, el motor de emparejamiento, la propuesta de grupo, o la búsqueda de un reemplazo. |
| `guarantee-fund-workflows` | Usar al modelar o tocar fondo, cobertura, subrogación, aval, recuperación, castigo o devolución, y antes de cerrar cualquier cambio que haga que el fondo pague por alguien. |
| `hooks-and-guardrails` | Usar al convertir una regla que "el agente a veces se salta" en una barrera determinista, al escribir o revisar un script en . |
| `iconography-imagery` | Usar al agregar un icono a un botón o menú, al elegir o incorporar un set de iconos, al insertar imágenes o ilustraciones, al diseñar un empty state, o al revisar por qué la UI se ve dispareja o pesada. |
| `incident-response-postmortem` | Usar cuando un servicio se cae o degrada, ante una posible brecha de datos, al coordinar la respuesta en caliente, y al escribir el postmortem una vez resuelto. |
| `integrity-testing` | Usar al tocar el esquema de datos, una migración, un flujo de pagos/inventario/reservas, cualquier operación concurrente, o un límite entre servicios con contrato explícito. |
| `kyc-identity-verification` | Usar al construir o tocar el alta de usuarios, la verificación de documento, los estados de KYC, o cualquier regla que condicione una acción a estar verificado. |
| `lane-authoring` | Usar al abrir un carril nuevo, al recibir un pedido grande y no saber por dónde cortarlo en entregas cerrables de punta a punta, o al revisar si un carril está bien definido antes de arrancar el trabajo. |
| `maps-geolocation` | Usar al modelar una entidad con ubicación, implementar "lugares/entidades financieros cercanos", buscar por radio, ordenar por distancia, o al elegir y consumir un proveedor de mapas y geocodificación. |
| `microservices-architecture` | Usar antes de crear un servicio, antes de mover una responsabilidad de un servicio a otro, cuando dos servicios siempre se despliegan juntos, cuando alguien propone leer la tabla de otro, o al ubicar dónde vive una funcionalidad nueva. |
| `microservices-deployment` | Usar antes de desplegar un servicio, al planificar un cambio que toca dos, al escribir una migración, o al diseñar el rollback de un release. |
| `microservices-testing` | Usar al definir qué tests escribir para un servicio o un flujo que cruza servicios, cuando la suite E2E es lenta e intermitente, o al verificar un cambio que toca dos o más servicios. |
| `mikroorm-patterns` | Usar al escribir o revisar un repositorio, caso de uso transaccional, entidad, job o test que toque el EntityManager, o ante datos viejos, flushes inesperados o errores de contexto global. |
| `milestone-planning` | Usar antes de escribir la primera línea de código, al armar el PLAN. |
| `mobile-offline-sync` | Usar al construir cualquier flujo que deba funcionar sin conexión o tolerar cortes, al diseñar la caché local de una pantalla, al sincronizar cambios hechos offline, o cuando "si se corta el internet se pierde lo que cargó". |
| `mobile-release-security` | Usar al configurar el almacenamiento de credenciales, antes de un release a las tiendas, al agregar un permiso o un SDK de terceros, y al revisar qué datos salen del dispositivo. |
| `mobile-ux-design` | Usar al diseñar o revisar cualquier pantalla, flujo o componente de la app nativa, al portar una vista web a móvil, o cuando algo "en el celular se siente incómodo" (se tapa con el teclado, cuesta tocar, no respeta el notch). |
| `model-driven-schema` | Usar al agregar o quitar tablas, columnas, claves, índices o constraints; al tocar un generador; al ver síntomas de deriva (tabla ausente, columna ausente, nulabilidad divergente, conteos que no cuadran); antes de cualquier tentación de ALTER manual; y al decidir entre este enfoque y migraciones versionadas. |
| `money-movement-safety` | Usar antes de escribir, revisar o cerrar cualquier código que calcule, prometa, acredite, deduzca, desembolse, condone o reverse un importe. |
| `multi-tenancy` | Usar al crear una tabla, endpoint, job, caché o índice de búsqueda en un sistema multi-organización, y antes de cerrar cualquier cambio que toque datos con dueño organizacional. |
| `native-code-patterns` | Usar antes de crear o editar un controller, servicio, entidad ORM, DTO, componente, cliente de API, test o script — localizar 2–3 ejemplos cercanos del mismo tipo y copiar su forma (naming, estructura, errores, imports, densidad de comentarios). |
| `nestjs-development` | Usar al crear un módulo, controller, provider o pipe en NestJS, cuando un endpoint acepta un body sin validar o campos de más, o al revisar por qué un guard/interceptor no se ejecuta en el orden esperado. |
| `notifications-delivery` | Usar al agregar un recordatorio, un correo transaccional o una notificación push; al elegir por qué canal sale un evento; o al diagnosticar notificaciones que llegan a quien optó por no recibirlas o por el canal equivocado. |
| `outcome-first` | Usar al recibir cualquier tarea, carril, slice o bug, antes de planificar o explorar, cuando se dio algo por terminado y nadie se pone de acuerdo en si está hecho, y al redactar un reporte de avance o de cierre. |
| `payment-reconciliation` | Usar al construir o tocar la importación de extractos, el motor de emparejamiento, la gestión de excepciones o el cierre de conciliación, y al investigar un pago que figura en el banco y no en el sistema, o al revés. |
| `payments-qr-integration` | Usar al integrar o tocar un proveedor de pagos, al emitir un QR o un enlace, al escribir o revisar el webhook, o al diagnosticar un pago que el usuario dice haber hecho y el sistema no ve. |
| `pentest-methodology` | Usar al planificar un ejercicio de seguridad de un servicio o feature, antes de ejecutar cualquier prueba activa, y para encuadrar todo el trabajo de las demás skills de assessment. |
| `pentest-recon-mapping` | Usar al arrancar una evaluación de seguridad, al auditar qué publica un servicio nuevo, o antes de exponer algo a internet. |
| `pentest-reporting-remediation` | Usar al cerrar una evaluación de seguridad, al documentar un hallazgo aislado de una revisión, al priorizar la cola de correcciones, y al decidir formalmente aceptar un riesgo. |
| `performance-load-testing` | Usar antes de lanzar un endpoint o flujo de alto tráfico, al fijar un SLO de latencia, tras un incidente de lentitud, o para comprobar que una optimización realmente mejoró algo. |
| `postgresql-advanced` | Usar al diagnosticar una query lenta, diseñar un índice o búsqueda, aplicar DDL sobre tablas con tráfico, aislar tenants o garantizar una invariante en la base. |
| `progress-reporting` | Usar al ejecutar un carril, una feature full-stack, una migración, un refactor o una investigación de varias fases; cuando el trabajo cruza varios repos; y al retomar una tarea que quedó a medias. |
| `prompt-engineering` | Usar al redactar o revisar un system prompt, el cuerpo de una skill, el prompt de un subagente o de un hook de tipo prompt, o al diagnosticar por qué un prompt da resultados vagos, sobre-dispara herramientas o ignora una instrucción. |
| `prompt-evals` | Usar antes de mergear cualquier edición a una description o al cuerpo de una skill, al agregar una skill que compite con otra, al cambiar de modelo, o cuando alguien dice "lo mejoré" sin casos que lo respalden. |
| `prompt-governance-versioning` | Usar al crear, editar, renombrar, deprecar o borrar una skill; al cambiar una `description`; al mover una skill entre áreas; y antes de publicar cualquier cambio que altere cómo se comporta una skill existente. |
| `python-tooling-standards` | Usar al crear o modificar cualquier script Python del repo, un generador, un cargador de datos o un hook, y al revisar por qué dos corridas dan salidas distintas. |
| `qa-evidence-reporting` | Usar al cerrar cualquier trabajo con QA, al escribir el reporte final de un carril, al adjuntar resultados a un PR o issue, o cuando alguien dice "ya lo probé" sin evidencia que lo respalde. |
| `qa-orchestration` | Usar al diseñar cómo se prueba un release completo, al armar o depurar un pipeline de CI, o al coordinar varios agentes/personas que hacen QA sobre el mismo cambio. |
| `qa-strategy` | Usar al planificar cómo se prueba una feature o un release, al definir qué va en cada nivel de test, o al decidir si algo está "listo para probar" o "probado de verdad". |
| `rationalization-guard` | Usar cuando estés por saltear discovery, testing, verificación o alcance; cuando tu propio texto contenga "debería", "seguramente" o "casi"; y antes de cualquier cierre. |
| `realtime-websockets` | Usar al agregar notificaciones en vivo, un chat, presencia, o actualización en vivo de calendario/tablero; al escribir o revisar un gateway; o al diagnosticar sockets que no autorizan, se caen al escalar a varias instancias o se saturan. |
| `refactoring-safely` | Usar al limpiar código heredado, al extraer o renombrar un símbolo, función o módulo en código, al pagar deuda técnica, al preparar un módulo para una feature nueva, o cuando sentís que "primero habría que ordenar esto". |
| `regression-suite-management` | Usar al decidir qué regresión corre para un cambio, al armar las etapas de CI, cuando la suite se vuelve lenta o roja crónica, o al cuarentenar o eliminar un test. |
| `regulatory-compliance-mapping` | Usar al arrancar un producto o módulo que trate datos personales o financieros, al recibir un requisito "de cumplimiento", al preparar una auditoría o evaluación de impacto, y al responder "¿esto cumple?" sin inventar la respuesta. |
| `release-and-rollback` | Usar al planificar cómo sale una versión a producción, al meter un cambio de base junto con código, o al decidir entre rollback y fix-forward. |
| `requirements-and-acceptance` | Usar al recibir cualquier requisito, historia o bug; al detectar una frase ambigua en el backlog; al decidir si algo está listo para implementar; y al definir qué evidencia probará que un criterio se cumplió. |
| `resilience-patterns` | Usar al integrar cualquier dependencia externa o inter-servicio, al diagnosticar una caída en cascada o un pool agotado, al elegir valores de timeout y reintento, y antes de declarar que un flujo aguanta la caída de un dependido. |
| `root-cause-debugging` | Usar cuando falla un test, un build, un E2E, un endpoint o un flujo; ante un bug reportado; y ante cualquier fallo intermitente. |
| `rosca-group-lifecycle` | Usar al modelar o tocar grupo, cupo, participante, turno, periodo, reglamento, acuerdo, permuta, ingreso, retiro o disolución, y al revisar cualquier endpoint que cambie quién cobra cuándo. |
| `saga-distributed-transactions` | Usar al diseñar o tocar cualquier flujo que cambie estado en dos o más servicios, especialmente si mueve plata, y al diagnosticar una operación que quedó a medio camino. |
| `scope-discipline` | Usar al arrancar cualquier slice o fix, cuando encontrás algo roto o feo fuera de tu tarea, cuando sentís ganas de "aprovechar", antes de tocar un archivo no previsto y al revisar tu diff antes de entregar. |
| `search-and-filtering` | Usar al construir un endpoint de listado o buscador, al agregar filtros u ordenamiento, al implementar autocompletado o "buscar por nombre", o al diagnosticar una búsqueda lenta, con resultados inestables entre páginas o que filtra datos que no debería. |
| `secure-code-review` | Usar al revisar un PR que toca autenticación, autorización, datos sensibles o entrada de usuario, al auditar un módulo antes de un release, y como parte de caja blanca de una evaluación de seguridad. |
| `security-guardrails` | Usar como gate de revisión antes de mergear cualquier endpoint, migración, integración de terceros o script automatizado que toque datos reales. |
| `security-testing` | Usar al agregar un endpoint sensible, al cambiar guards, roles o DTOs, al configurar la etapa de seguridad del CI, y antes de un release; solo contra entornos propios y autorizados. |
| `seed-data-catalogs` | Usar al crear o actualizar un catálogo (regiones, ciudades, especialidades, bancos, instituciones, monedas, entregas), al escribir un seeder o generador de datos, al corregir un dato cargado, o cuando un requisito pide "datos reales". |
| `seo-public-pages` | Usar al crear o revisar cualquier página pública que deba aparecer en buscadores o verse bien al compartirse, y para decidir qué se prerenderiza y qué se sirve on-demand. |
| `server-hardening` | Usar al provisionar o auditar un server, después de un incidente, al abrir cualquier puerto, y antes de alojar datos financieros. |
| `service-communication-patterns` | Usar antes de agregar una llamada entre servicios, al diseñar un flujo que cruza dos o más, cuando un endpoint lento resulta ser tres llamadas encadenadas, o al decidir si publicar un evento o llamar una API. |
| `service-contracts-versioning` | Usar al crear o modificar cualquier endpoint interno, evento de integración o payload que consuma otro servicio, al agregar o quitar un campo, y antes de mergear un cambio que otro equipo consume. |
| `service-to-service-security` | Usar al conectar dos servicios, al exponer un endpoint interno, al recibir un webhook externo, al configurar credenciales de base o de cola, y al revisar si alguien confía en que "el gateway ya validó". |
| `skill-authoring` | Usar al crear una skill nueva, al editar la description de una existente, al partir una skill que creció demasiado, o al revisar por qué una skill no se activa o se activa cuando no debe. |
| `skills-router` | Usar al empezar cualquier trabajo para saber qué cargar, cuando no sabés si existe una skill para lo que estás por hacer, al revisar si te salteaste un gate antes de cerrar, o al incorporar a alguien al repo. |
| `smart-dumb-components` | Usar al crear cualquier componente, al partir uno que trae datos y pinta a la vez, cuando un componente no se puede reusar en otra pantalla ni testear sin levantar media app, o al decidir si algo debe inyectar un servicio. |
| `solid-principles` | Usar al diseñar una clase o módulo nuevo, al revisar si una jerarquía de herencia o una interfaz gigante necesita partirse, o al decidir entre herencia y composición. |
| `state-machines-workflows` | Usar al agregar un estado o acción a una entidad, al exponer un endpoint que cambia estado, al revisar un `PATCH` que acepta `status`, o ante un registro en estado imposible. |
| `static-analysis-linting` | Usar al montar o migrar la config de lint de un repo, al agregar o desactivar una regla, al integrar el lint al pre-commit y al CI, o cuando el lint tira mil warnings que nadie mira. |
| `subagent-design` | Usar al crear o revisar un archivo en . |
| `synthetic-test-data-generation` | Usar al construir factories o fixtures, al poblar un entorno de prueba o demo, al necesitar volumen para un test de carga, o cuando los tests fallan por datos poco realistas o irrepetibles. |
| `technical-debt-management` | Usar al tomar un atajo consciente, al encontrar deuda mientras trabajás en otra cosa, al planificar capacidad de un sprint, o al justificar por qué un módulo necesita tiempo de refactor. |
| `technical-docs-and-adr` | Usar al tomar una decisión de arquitectura no trivial, al escribir el README de un módulo o repo, al preparar un runbook de operación o incidente, o al decidir qué merece documentarse y qué se explica solo con el código. |
| `terminology-value-sets` | Usar antes de crear un enum, un catálogo, un `select` de opciones fijas o un `switch` sobre un tipo, y al sembrar o traducir catálogos. |
| `test-case-design-techniques` | Usar al planificar qué probar de una feature, endpoint o formulario, al convertir criterios de aceptación en casos concretos, o cuando "probé algo" pero no hay forma de saber qué quedó sin cubrir. |
| `test-data-management` | Usar al escribir cualquier test de integración, API o E2E, al diseñar fixtures o factories, cuando un test falla "solo a veces" o "solo después de otro", y antes de copiar cualquier dato hacia un entorno de prueba. |
| `test-plan-authoring` | Usar al arrancar el QA de un carril, antes de escribir el primer test, al preparar un release, o cuando nadie sabe qué se va a probar ni cuándo se considera terminado. |
| `threat-modeling` | Usar al diseñar una feature nueva o sensible, al revisar un diseño antes de codear, y como insumo de la evaluación de seguridad. |
| `transparency-reputation` | Usar al construir el panel de transparencia, el score, un certificado, un badge, o cualquier pantalla que muestre el comportamiento de un participante a otros. |
| `typescript-standards` | Usar al escribir o revisar código TypeScript (API NestJS o web Angular), configurar un `tsconfig`, tipar un DTO o respuesta externa, o cuando aparece un `any`, un `as` o un `!` para callar al compilador. |
| `typography-systems` | Usar al definir la escala tipográfica de un producto, al maquetar texto largo o tablas numéricas, al elegir o cargar una fuente, o al revisar por qué un texto se ve apretado, ilegible o "de plantilla". |
| `uat-acceptance-signoff` | Usar al cerrar una feature o carril de cara al negocio, antes de habilitar algo en producción, al coordinar la validación con el responsable funcional/financiero, o cuando "está terminado" técnicamente pero nadie del negocio lo aprobó. |
| `ui-quality-review` | Usar como pasada final antes de dar una pantalla por terminada, al revisar un PR que agrega o cambia UI, o para diagnosticar por qué una interfaz "correcta" se ve barata o templada. |
| `unit-testing` | Usar al escribir o revisar cualquier test unitario nuevo, o al decidir si un mock es la herramienta correcta para un caso. |
| `ux-clarity-usability` | Usar al diseñar o revisar cualquier flujo o pantalla, cuando un usuario "no sabe qué hacer" o "se pierde", y antes de dar por terminada una vista con la que alguien va a operar. |
| `ux-writing-microcopy` | Usar al DECIDIR qué dice un botón, label, mensaje de error, estado vacío, confirmación o notificación, al traducir jerga técnica del backend a lenguaje humano, o cuando un flujo confunde por lo que dice y no por cómo se ve. |
| `vertical-slicing` | Usar al planificar la implementación de un carril, al ver un plan organizado por capas horizontales ("primero todo el backend"), o cuando un cambio creció tanto que no se puede probar hasta el final. |
| `visual-hierarchy-composition` | Usar al componer una pantalla nueva, al diagnosticar una interfaz donde "todo compite" o "no sé dónde mirar", y al revisar si el orden visual coincide con el orden de importancia. |
| `visual-proof` | Usar después de cualquier cambio de layout, espaciado, modal, overlay, menú, tabla, formulario, responsive, tema o animación, y antes de declarar verificada una pantalla. |
| `visual-regression-testing` | Usar al proteger componentes del design system o pantallas estables contra cambios visuales no intencionales, al tocar tokens o CSS global, o cuando una baseline falla. |
| `web-app-pentest` | Usar al evaluar la seguridad de la web Angular/Astro antes de un release, tras cambios en autenticación o formularios, o como guía de una revisión manual dirigida. |
| `windows-dev-environment` | Usar al escribir un script o comando que otro correrá en Windows, al configurar la máquina de un dev nuevo, cuando algo "anda en Linux y falla en Windows", o al escribir un hook que debe correr en Windows. |
| `work-report-md` | Usar al cerrar cualquier trabajo, al terminar una sesión con algo incompleto, o al retomar trabajo ajeno. |
