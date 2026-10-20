import type { ExplicacionDeModulo } from './tipos'

/**
 * Explicaciones para el módulo Inicio y Tableros Ejecutivos de AportaYa.
 * Diseñado con redacción clara y empática para el usuario común, y precisión técnica en sistemas.
 */
export const moduloTableroExplicacion: ExplicacionDeModulo = {
  modulo: 'Inicio y Tableros',
  prefijos: ['/tablero'],
  sistemas:
    'Agrega métricas en tiempo real desde la réplica de lectura de PostgreSQL y series temporales operativas. Los indicadores se calculan mediante consultas agregadas que no bloquean las transacciones activas de la billetera ni del motor de pasanaku. Cada indicador cuenta con una meta prefijada y un responsable técnico.',
  negocio:
    'Es la cabina de control principal de AportaYa: permite que los directores, operadores y reguladores vean en segundos la salud de toda la plataforma. Responde a preguntas vitales como: ¿cuánta plata se está moviendo hoy?, ¿hay grupos con atrasos?, ¿el sistema está respondiendo rápido? y ¿el dinero de la gente está seguro y respaldado al 100%?',
  vistas: {
    '/tablero': {
      sistemas:
        'Consulta el resumen ejecutivo consumiendo `GET /tablero/resumen` y `GET /auditoria/eventos/recientes`. Cruza los eventos de outbox y el estado de los microservicios sin impactar la base primaria. Permite detectar desvíos operativos antes de que se conviertan en incidentes.',
      negocio:
        'Esta es la pantalla de bienvenida y pulso del negocio. Muestra de un vistazo los números clave del día: el dinero recaudado en aportes, las entregas de dinero a los ganadores de turno, los reclamos que requieren atención inmediata y si hay alguna alerta que deba atenderse con urgencia para que ningún usuario quede esperando.',
    },
    '/tablero/financiero': {
      sistemas:
        'Calcula el balance de liquidez en tiempo real cruzando la tabla `asientos_contables` y `cuentas_bancarias_custodia`. Verifica el cumplimiento de la ecuación fundamental de encaje bancario: Saldo en Custodia >= Dinero Electrónico Emitido. Si el ratio cae por debajo de 1.00, se dispara una alerta de severidad crítica.',
      negocio:
        'Garantiza que por cada peso virtual que un participante tiene en su aplicación, exista un peso real resguardado en el banco. Esta vista permite a tesorería y finanzas vigilar que no haya descuadres, que la mora en los grupos esté bajo control y que el fondo de garantía comunitario cuente con respaldo suficiente para cubrir cualquier falta.',
    },
    '/tablero/sistemas': {
      sistemas:
        'Monitorea en vivo los Objetivos de Nivel de Servicio (SLO) de cada microservicio: latencia percentil 95 (p95 < 250ms), tasa de éxito HTTP (disponibilidad >= 99.9%), uso de CPU/memoria y saturación del pool de conexiones HikariCP. Se nutre de métricas del actuador de Spring Boot y del gateway NGINX.',
      negocio:
        'Asegura que la plataforma nunca se caiga ni se vuelva lenta. Si los servidores sufren sobrecarga o una conexión con un banco falla, este tablero lo avisa al instante para que el equipo de soporte lo solucione antes de que una familia note problemas al enviar un pago o recibir su turno.',
    },
    '/tablero/comercial': {
      sistemas:
        'Agrupa métricas de adopción: número de grupos de pasanaku activos, volumen mensual transaccionado (TPV), retención de organizadores y tasa de completitud de ciclos. Procesa proyecciones a partir de la tabla `grupos` y `participaciones` aplicando filtros de período.',
      negocio:
        'Mide el impacto social y comercial de AportaYa en la comunidad: cuántas personas están ahorrando juntas, cuántos grupos terminaron con éxito sin ninguna mora y qué organizadores tienen la mejor reputación. Ayuda al equipo comercial a premiar el buen cumplimiento y abrir nuevos programas de ahorro.',
    },
    '/tablero/catalogo-de-datos': {
      sistemas:
        'Inspecciona el diccionario de datos de PostgreSQL: tablas, columnas, restricciones de integridad referencial (claves foráneas), índices y políticas de seguridad a nivel de fila (RLS). Cruza el esquema físico con los contratos documentados en OpenAPI y los diagramas PlantUML de la bóveda.',
      negocio:
        'Es el mapa de toda la información que guarda la plataforma. Garantiza que sepamos con exactitud dónde se guarda cada dato, qué significa en la vida real (por ejemplo: la cédula de identidad, el monto de un aporte o el comprobante de pago) y que ninguna tabla quede huérfana o sin un propósito claro.',
    },
    '/tablero/catalogo-de-datos/[tabla]': {
      sistemas:
        'Muestra la radiografía técnica de una tabla específica: tipos de datos exactos (e.g. `NUMERIC(14,2)` para dinero, `TIMESTAMPTZ` para fechas), restricciones CHECK, índices para optimizar búsquedas y las operaciones del sistema que leen o escriben sobre ella.',
      negocio:
        'Explica qué representa esta tabla para el negocio (por ejemplo: `asientos_contables` representa la historia inmutable de cada centavo que entra y sale). Permite a auditores y desarrolladores entender el modelo sin necesidad de descifrar código complejo.',
    },
    '/tablero/catalogo-de-datos/[tabla]/ficha': {
      sistemas:
        'Permite a los administradores autorizados clasificar la sensibilidad de los datos de una tabla (e.g. datos públicos, confidenciales o datos personales regulados como el CI y teléfono) y actualizar su definición de gobierno mediante una transacción protegida.',
      negocio:
        'Controla la privacidad y protección de los datos de nuestros usuarios. Define quién tiene derecho a ver cada dato y bajo qué justificación legal o de soporte, cumpliendo con las normas de protección al consumidor financiero.',
    },
  },
}
