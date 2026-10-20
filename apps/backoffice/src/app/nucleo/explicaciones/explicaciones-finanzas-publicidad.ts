import type { ExplicacionDeModulo } from './tipos'

/**
 * Explicaciones para los módulos de Finanzas (Contabilidad ERP) y Publicidad de AportaYa.
 * Diseñado con rigor contable-financiero y redacción transparente y cercana para el usuario común.
 */
export const moduloContabilidadExplicacion: ExplicacionDeModulo = {
  modulo: 'Contabilidad ERP',
  prefijos: ['/contabilidad'],
  sistemas:
    'Sistema de contabilidad de partida doble integrado (CU-100 a CU-106). Cada movimiento financiero de la plataforma genera automáticamente asientos contables balanceados (débitos = créditos) con cuentas del plan contable oficial. Gestiona ejercicios fiscales, centros de costo, devengamiento de comisiones, impuestos de ley y generación de estados financieros auditables.',
  negocio:
    'La gestión financiera transparente de la empresa. Garantiza que los números de AportaYa sean exactos, auditables y claros como el agua: qué dinero ingresa por comisiones de servicio, qué gastos operativos se pagan y que los impuestos se liquiden con puntualidad y honestidad ante el Servicio de Impuestos Nacionales.',
  vistas: {
    '/contabilidad': {
      sistemas:
        'Control de períodos contables (CU-100): consulta `GET /erp/periodos/vigente` y `GET /erp/ejercicios`. Permite abrir meses fiscales, bloquear movimientos retroactivos y ejecutar el cierre definitivo con rol `CONTABILIDAD_ERP_CERRAR`.',
      negocio:
        'El calendario contable del negocio. Organiza las cuentas mes a mes, asegurando que una vez cerrado un mes nadie pueda alterar las cifras pasadas y que los balances reflejen la realidad financiera exacta de la empresa.',
    },
    '/contabilidad/periodo/[ejercicioId]': {
      sistemas:
        'Detalle de un ejercicio fiscal histórico o futuro: lista los períodos mensuales, su estado (ABIERTO, BLOQUEADO, CERRADO) y el balance de comprobación preliminar.',
      negocio:
        'Revisión histórica de un año fiscal concreto. Permite consultar cómo cerró la contabilidad en años anteriores para preparar auditorías externas o declaraciones tributarias oficiales.',
    },
    '/contabilidad/presupuesto': {
      sistemas:
        'Control presupuestario por centro de costo (CU-101): compara el presupuesto asignado contra lo devengado y pagado en tiempo real. Exige permiso `CONTABILIDAD_ERP_PRESUPUESTO` y alerta sobre desvíos mayores al 5%.',
      negocio:
        'El plan de gastos e inversiones de la empresa. Ayuda a cuidar los recursos económicos de AportaYa, verificando que cada área (tecnología, soporte, marketing, legal) gaste únicamente lo planificado y aprobado por la dirección.',
    },
    '/contabilidad/compras': {
      sistemas:
        'Gestión de cuentas por pagar y facturas de proveedores (CU-103). Valida la validez fiscal de la factura, la recepción a satisfacción del servicio o bien, y programa la orden de pago bancario con aprobación de dos firmas.',
      negocio:
        'El pago justo y ordenado a nuestros proveedores (servidores en la nube, telefonía, seguridad, servicios legales). Comprueba que lo que nos cobran sea correcto antes de desembolsar el pago.',
    },
    '/contabilidad/cobros': {
      sistemas:
        'Gestión de cuentas por cobrar y facturación electrónica oficial (CU-104). Devenga las comisiones por uso de plataforma, emite la factura con código de control tributario QR y concilia los abonos recibidos.',
      negocio:
        'La facturación formal de nuestros servicios. Asegura que por cada comisión cobrada por el uso de la billetera o la administración de pasanaku, el usuario reciba su factura electrónica oficial y válida para impuestos.',
    },
    '/contabilidad/activos/[periodoVigenteId]': {
      sistemas:
        'Inventario y depreciación de activos fijos (CU-105): equipos de computación, servidores, mobiliario y licencias de software. Calcula la depreciación mensual según la tabla de vida útil oficial y genera los asientos contables respectivos.',
      negocio:
        'El registro del patrimonio material de la empresa. Cuida los equipos y herramientas que el equipo utiliza a diario, registrando su desgaste en el tiempo conforme a las normas contables.',
    },
    '/contabilidad/estados/[periodoId]': {
      sistemas:
        'Generación de Estados Financieros oficiales (CU-106): Balance General, Estado de Resultados (Pérdidas y Ganancias) y Flujo de Efectivo. Permite exportar libros oficiales en PDF y Excel con hash de integridad para la auditoría externa.',
      negocio:
        'Los estados financieros que demuestran la solidez de la empresa ante bancos, socios y el Estado. Refleja que AportaYa es una compañía financieramente sana, solvente y digna de la confianza de miles de familias.',
    },
  },
}

export const moduloPublicidadExplicacion: ExplicacionDeModulo = {
  modulo: 'Publicidad y Alianzas',
  prefijos: ['/publicidad'],
  sistemas:
    'Motor de publicidad contextual y patrocinios éticos (CU-110 a CU-114). Permite a comercios aliados y marcas verificadas ofrecer promociones y descuentos relevantes a los participantes del pasanaku sin vender ni exponer datos personales (cero tracking invasivo). Gestiona anunciantes, aprobación de piezas creativas, medición de impresiones y liquidación financiera.',
  negocio:
    'Alianzas comerciales que benefician al usuario. En lugar de publicidad molesta o invasiva, conecta a los ahorristas con comercios locales (tiendas de construcción, librerías, electrodomésticos, farmacias) que ofrecen descuentos exclusivos para quienes cobran su pasanaku, generando ingresos extra para mantener bajas las comisiones de la app.',
  vistas: {
    '/publicidad': {
      sistemas:
        'Panel general de publicidad: sintetiza anunciantes activos, campañas vigentes, piezas en cola de moderación y métricas agregadas de rendimiento consumiendo el servicio comercial.',
      negocio:
        'Resumen de las alianzas y promociones vigentes en la plataforma. Permite supervisar qué marcas están participando y verificar que las ofertas activas aporten valor real a la comunidad.',
    },
    '/publicidad/anunciantes': {
      sistemas:
        'Catálogo de anunciantes y comercios aliados (CU-110). Valida Número de Identificación Tributaria (NIT), personería jurídica, categoría comercial y vigencia del contrato de patrocinio.',
      negocio:
        'El directorio de empresas de confianza autorizadas a publicar ofertas en AportaYa. Nos aseguramos de que solo comercios serios, registrados y con excelente reputación puedan ofrecer productos a nuestros usuarios.',
    },
    '/publicidad/campanas': {
      sistemas:
        'Gestión de campañas publicitarias (CU-111): presupuesto asignado, segmentación comunitaria no invasiva por ciudad o categoría de pasanaku, fechas de inicio/fin y topes de impresiones diarias.',
      negocio:
        'La planificación de promociones y beneficios especiales. Define cuándo y en qué regiones se muestran las ofertas (por ejemplo, descuentos en útiles escolares en enero o en materiales de construcción para quienes ahorran para mejorar su hogar).',
    },
    '/publicidad/campanas/[campanaId]/aprobacion': {
      sistemas:
        'Panel de aprobación de campaña publicitaria: segrega funciones exigiendo permiso `PUBLICIDAD_APROBAR_CAMPANA`. Permite evaluar el arte, el texto promocional y el enlace de destino antes de habilitar la campaña.',
      negocio:
        'El control de calidad final de cada anuncio. Un supervisor revisa que la oferta sea verídica, que el descuento sea real y que no contenga información engañosa antes de que aparezca en la pantalla de la gente.',
    },
    '/publicidad/moderacion': {
      sistemas:
        'Cola de moderación ética y regulatoria de piezas creativas (CU-112). Examina imágenes, textos y formatos para garantizar que no violen normas de protección al consumidor, no promuevan juegos de azar ni productos nocivos.',
      negocio:
        'La protección de la comunidad contra contenido inadecuado. Bloquea de raíz cualquier anuncio dudoso, préstamos informales o contenido que no respete los valores de respeto y protección familiar de AportaYa.',
    },
    '/publicidad/desempeno': {
      sistemas:
        'Métricas de rendimiento publicitario (CU-113): total de impresiones servidas, clics efectivos, tasa de interacción (CTR) y conversiones verificadas, garantizando la privacidad absoluta de los identificadores de usuario.',
      negocio:
        'Informe de resultados para los comercios aliados. Muestra cuántas personas vieron la promoción y cuántas aprovecharon los descuentos, demostrando a los comercios el valor de su alianza con la comunidad de Pasanaku.',
    },
    '/publicidad/liquidacion': {
      sistemas:
        'Liquidación y facturación a anunciantes (CU-114). Cruza las impresiones y clics validados contra el tarifario contratado, genera la liquidación comercial y emite la factura fiscal para cobro bancario.',
      negocio:
        'El cobro transparente a las empresas patrocinadoras. Garantiza que los anunciantes paguen exactamente por los resultados obtenidos, generando ingresos legítimos para financiar la seguridad y tecnología de la plataforma.',
    },
  },
}
