import type { ExplicacionDeModulo } from './tipos'

/**
 * Explicaciones para el módulo Sistemas e Infraestructura de AportaYa.
 * Mantiene la máxima exhaustividad técnica para ingenieros y auditoría,
 * traduciendo simultáneamente el impacto real a un lenguaje humano y comprensible.
 */
export const moduloSistemasExplicacion: ExplicacionDeModulo = {
  modulo: 'Sistemas',
  prefijos: ['/sistemas'],
  sistemas:
    'Monitorea y gestiona la infraestructura distribuida de AportaYa: 10 microservicios en Spring Boot / Java 21, base de datos PostgreSQL con RLS (Row Level Security), cluster de mensajería Kafka con patrón Outbox transaccional, balanceador NGINX y plataforma Coolify en servidores endurecidos. Controla el acceso mediante RBAC estricto y cuida los contratos OpenAPI de extremo a extremo.',
  negocio:
    'Es la sala de máquinas de AportaYa. Aquí los ingenieros de sistemas vigilan que los servidores estén siempre encendidos, que la base de datos no tenga cuellos de botella y que las conexiones con los bancos funcionen sin interrupciones. Gracias a este módulo, la aplicación móvil de los usuarios responde al instante, no pierde ningún dato y resiste cualquier corte técnico.',
  vistas: {
    '/sistemas': {
      sistemas:
        'Centro de observabilidad que sintetiza el estado operativo de los 10 servicios, salud de las bases de datos, mensajes pendientes en el outbox y estado de los proveedores externos. Detecta caídas de dependencias en menos de 5 segundos.',
      negocio:
        'El panel de control técnico principal. Le dice a los ingenieros si todos los motores de la plataforma están marchando bien, si algún servicio necesita más memoria o si una conexión externa está demorando más de lo normal.',
    },
    '/sistemas/servicios': {
      sistemas:
        'Inventario y métricas en vivo de los microservicios (`identidad`, `billetera`, `pasanaku`, `contabilidad`, `notificaciones`, `auditoria`, etc.). Vigila el cumplimiento de los SLOs (latencia p95 < 250ms, disponibilidad 99.9%, tasa de errores 5xx < 0.1%).',
      negocio:
        'Garantiza la velocidad y confiabilidad de cada función de la app: que registrarse tarde pocos segundos, que enviar un mensaje no demore y que consultar el saldo sea instantáneo. Si un servicio se degrada, se detecta antes de que los usuarios lo noten.',
    },
    '/sistemas/despliegues': {
      sistemas:
        'Control de versiones de software en ejecución, commits de Git desplegados, variables de entorno activas e interruptores de características (feature flags) para habilitar o deshabilitar funcionalidades sin necesidad de reiniciar servidores.',
      negocio:
        'Gestión de actualizaciones seguras. Permite incorporar mejoras continuas a la aplicación sin cortes de servicio y apagar temporalmente una función específica si presenta alguna falla, sin afectar al resto del sistema.',
    },
    '/sistemas/base-de-datos': {
      sistemas:
        'Muestra el estado del motor PostgreSQL: conexiones activas del pool HikariCP, transacciones lentas, bloqueos entre procesos (deadlocks), espacio en disco de tablas e índices y estado de las migraciones de esquema versionadas con Flyway.',
      negocio:
        'La salud del lugar donde se almacena toda la historia de AportaYa. Verifica que las consultas a la base de datos se ejecuten de forma limpia y veloz, impidiendo que el sistema se tranque o se quede sin memoria.',
    },
    '/sistemas/respaldos': {
      sistemas:
        'Administra y verifica la política de copias de seguridad (backups): respaldos completos diarios, copias diferenciales y archivado de WAL (Write-Ahead Logging) para recuperación en el tiempo (PITR) con cifrado AES-256 y ensayos periódicos de restauración.',
      negocio:
        'El seguro de vida de los datos de las personas. Comprueba que existan copias de respaldo automáticas todos los días, guardadas en ubicaciones seguras e inviolables, listas para recuperar toda la información en caso de un desastre mayor.',
    },
    '/sistemas/proveedores': {
      sistemas:
        'Supervisa la comunicación con proveedores externos: pasarelas de pago QR (ej. BancoSol, BCP, Síntesis), servidores de mensajería WhatsApp/SMS y servicios de correo. Monitorea circuit breakers que cortan llamadas cuando el proveedor externo colapsa.',
      negocio:
        'El estado de los puentes hacia el mundo exterior. Si una empresa de telefonía o un banco asociado tiene problemas en su red, este módulo lo detecta y redirige el tráfico por una ruta alternativa para que nadie se quede sin recibir sus notificaciones o dinero.',
    },
    '/sistemas/outbox': {
      sistemas:
        'Vigila la tabla transaccional `mensajes_outbox` y la cola de mensajes descartados (Dead Letter Queue - DLQ). Asegura que ningún evento de dominio se pierda entre la base de datos y el broker Kafka, permitiendo reintentos controlados con retroceso exponencial.',
      negocio:
        'La garantía de que las promesas del sistema se cumplen siempre. Por ejemplo: si se cobra una cuota, el sistema garantiza al 100% que el comprobante se enviará por mensaje, incluso si la red de mensajería estuvo caída unos minutos al momento del cobro.',
    },
    '/sistemas/webhooks': {
      sistemas:
        'Bandeja de recepción de notificaciones de pasarelas de pago externas. Verifica la firma criptográfica HMAC en los headers de cada petición HTTP entrante para descartar ataques de suplantación y procesa la confirmación de cobros con clave de idempotencia.',
      negocio:
        'El receptor seguro de confirmaciones de pago. Cuando un participante paga su cuota escaneando un código QR desde su banco, el banco avisa a este punto: el sistema comprueba que el aviso sea auténtico y acredita el dinero en la billetera al instante.',
    },
    '/sistemas/accesos': {
      sistemas:
        'Auditoría en tiempo real de sesiones de operadores del backoffice: dirección IP de origen, huella del navegador, método de autenticación MFA (segundo factor) y revocación forzosa de tokens JWT comprometidos.',
      negocio:
        'Seguridad de acceso para el personal. Controla quién entra al panel de administración, desde qué lugar y a qué hora, bloqueando accesos no reconocidos y protegiendo la información sensible contra accesos no autorizados.',
    },
    '/sistemas/incidentes': {
      sistemas:
        'Gestión técnica de incidentes de disponibilidad o degradación: tiempos de detección (MTTD), tiempos de resolución (MTTR), enlaces a logs correlacionados con Trace ID y redacción del análisis de causa raíz (post-mortem).',
      negocio:
        'El protocolo de emergencia técnica. Cuando ocurre cualquier avería, organiza al equipo de ingenieros para resolverla con rapidez y documentar con honestidad qué ocurrió, cómo se solucionó y qué candados se pusieron para que no vuelva a pasar.',
    },
    '/sistemas/usuarios-internos': {
      sistemas:
        'Administración de operadores internos del backoffice. Permite crear usuarios, asignar departamentos, definir contraseñas seguras con Argon2id y activar/desactivar cuentas con motivo auditable.',
      negocio:
        'El registro de empleados y colaboradores que operan el sistema. Permite saber con precisión qué personas del equipo tienen permiso para trabajar en el backoffice y suspender de inmediato el acceso a quien ya no pertenezca a la empresa.',
    },
    '/sistemas/usuarios-internos/[id]': {
      sistemas:
        'Ficha detallada del operador interno: roles asignados, historial de inicios de sesión, dispositivos utilizados y bitácora de acciones ejecutadas en la plataforma.',
      negocio:
        'Perfil individual del operador. Muestra qué responsabilidades tiene asignadas y su registro histórico de trabajo, garantizando transparencia y responsabilidad en el uso de las herramientas de administración.',
    },
    '/sistemas/roles-internos': {
      sistemas:
        'Definición de roles según la matriz RBAC de segregación de funciones: roles financieros (`TESORERIA`, `CONTABILIDAD`), de cumplimiento (`OFICIAL_CUMPLIMIENTO`), de soporte (`OPERADOR_SOPORTE`) y de plataforma (`ADMIN_PLATAFORMA`, `ADMIN_SISTEMAS`).',
      negocio:
        'La división de poderes en la empresa. Asegura que nadie tenga superpoderes absolutos: quien aprueba pagos no puede crear cuentas ficticias, y quien audita no puede mover fondos. Esta segregación protege a la empresa y a los usuarios contra fraudes internos.',
    },
    '/sistemas/permisos-internos': {
      sistemas:
        'Inventario atómico de capacidades del sistema (e.g. `RETIRO_APROBAR`, `BILLETERA_VER_TERCEROS`, `AUDITORIA_LEER`). Cada endpoint del gateway y cada acción crítica en el backend valida la presencia explícita de estos permisos en el token JWT.',
      negocio:
        'El catálogo de llaves del sistema. Cada permiso es como una llave específica para una puerta concreta (ver un expediente, autorizar un retiro, descargar un reporte). Permite armar roles justos y con el mínimo acceso necesario para trabajar.',
    },
    '/sistemas/operaciones': {
      sistemas:
        'Catálogo exhaustivo de endpoints de toda la plataforma: método HTTP, ruta, servicio propietario, nivel de riesgo (alto, medio, bajo), clasificación de datos personales (PII) que toca, DTOs de entrada/salida y reglas de idempotencia.',
      negocio:
        'El inventario oficial de todo lo que el sistema sabe hacer. Permite a cualquier miembro del equipo, auditor o regulador conocer qué acciones existen en el software, qué datos tocan y qué candados de seguridad tienen incorporados.',
    },
    '/sistemas/operaciones/[id]': {
      sistemas:
        'Ficha técnica detallada de una operación específica: esquema JSON del payload, validaciones de campos requeridos, códigos de error catalogados (AP-CUxx-xx), tablas SQL que modifica y contratos de prueba asociados.',
      negocio:
        'El manual de instrucciones técnico de una función específica. Explica qué información requiere para ejecutarse, cómo valida que los datos sean correctos y qué respuesta entrega al usuario o a la app móvil.',
    },
    '/sistemas/flujos': {
      sistemas:
        'Grafo interactivo de dependencias y flujos del sistema: conecta interfaces de usuario, operaciones REST, servicios de aplicación, eventos asíncronos y tablas de base de datos. Señala escrituras desprotegidas o posibles derivas de permisos.',
      negocio:
        'El mapa visual de cómo viaja la información dentro de AportaYa. Muestra qué camino recorre un clic del usuario en su teléfono hasta convertirse en un movimiento de dinero registrado en la base de datos.',
    },
    '/sistemas/bitacora': {
      sistemas:
        'Registro inmutable de auditoría forense (`bitacora_eventos`). Almacena eventos con identificador de correlación (Trace ID), operador responsable, IP, recurso modificado, valores anteriores y nuevos en formato JSONB con firma de integridad.',
      negocio:
        'La caja negra de AportaYa. Como en los aviones, cada acción relevante que ocurre en el sistema queda grabada en un registro que nadie puede borrar ni editar. Si surge una duda o investigación, la bitácora dice con exactitud qué pasó, cuándo y quién lo hizo.',
    },
    '/sistemas/catalogos-operativos': {
      sistemas:
        'Mantenimiento de parámetros y catálogos de negocio versionados: tipos de documento de identidad válidos, monedas soportadas (BOB, USD), entidades bancarias autorizadas y estados de ciclo de vida.',
      negocio:
        'Las tablas de referencia del sistema. Centraliza las listas estándar (como la lista de bancos del país o tipos de carnet de identidad) para que toda la plataforma use los mismos términos y no existan confusiones.',
    },
    '/sistemas/catalogos-operativos/[codigo]': {
      sistemas:
        'Detalle y edición controlada de un catálogo específico. Valida que los cambios no rompan claves foráneas en tablas activas y registra el cambio con versión incremental.',
      negocio:
        'Ficha de administración de una lista de referencia. Permite actualizar opciones (por ejemplo, dar de alta un nuevo banco que habilitó cobros por QR) sin necesidad de modificar el código fuente del programa.',
    },
  },
}
