/** Los textos del tablero, en voz de marca. */
export const textosTablero = {
  titulo: 'Tablero',
  proposito: 'Qué requiere tu acción hoy, según tu rol, y a qué sección podés entrar.',
  accion: {
    titulo: 'Requiere tu acción',
    explicacion: 'Miramos la primera fila de cada bandeja que tu rol puede atender. Decimos si hay algo pendiente; la cantidad la ves en la bandeja.',
    actualizar: 'Actualizar',
    sinBandejas: 'Tu rol no atiende ninguna bandeja con pendientes. Usá los accesos de abajo para entrar a tu sección.',
    cargando: 'Consultando la bandeja',
    pendientes: 'Hay pendientes',
    alDia: 'Al día',
    error: 'No pudimos consultarla',
    abrir: 'Abrir bandeja',
    consultadaEl: 'Consultada',
    traza: 'Código de seguimiento',
  },
  monitoreo: {
    titulo: 'Monitoreo',
    explicacion: 'Indicadores de la plataforma. Solo mostramos los que un servicio publica; el resto lo decimos.',
    noDisponible: 'Indicador no disponible',
    indicadores: [
      { id: 'integraciones', titulo: 'Salud de integraciones', motivo: 'Ningún servicio publica todavía una proyección del estado de los proveedores externos.' },
      { id: 'conciliacion', titulo: 'Conciliación del día', motivo: 'No hay proyección de la conciliación diaria: el resultado se ve en la bandeja de operación.' },
    ],
  },
  accesos: { titulo: 'Accesos' },
  porRol: {
    financiero: {
      titulo: 'Tablero financiero',
      proposito: 'Lo que entró, lo que salió, la mora y el fondo de garantía, calculado desde el mayor y cada servicio dueño.',
    },
    sistemas: {
      titulo: 'Tablero de sistemas',
      proposito: 'Cuánto se usó la plataforma, cuánto falló y cuánto tardó, desde OpenTelemetry.',
    },
    comercial: {
      titulo: 'Tablero comercial',
      proposito: 'Cuántas personas llegan, cuántas se quedan y cuánto dinero comprometen los grupos.',
    },
    granularidad: 'Ver por',
    segmentos: [
      { valor: 'dia', texto: 'Día' },
      { valor: 'mes', texto: 'Mes' },
      { valor: 'anio', texto: 'Año' },
    ],
    campo: { dia: 'Día', mes: 'Mes', anio: 'Año' },
    cargando: 'Consultando los indicadores',
    vacio:
      'Todavía no hay indicadores de este período. Se calculan cada día de madrugada para el día anterior, y el mes y el año recién al terminar: elegí un período cerrado.',
    enCurso: 'Elegiste un período que todavía no terminó: sus indicadores se publican cuando cierre.',
    provisorio: 'Algún indicador de este período es provisorio: puede cambiar.',
    sinDatos: 'Sin datos en el período',
    sinDatosExplicacion: 'No hubo operaciones que medir, o el servicio dueño todavía no lo publicó.',
    suprimido: 'Oculto por privacidad',
    suprimidoExplicacion: (minimo?: number) =>
      minimo ? `Hay menos de ${minimo} casos: mostrarlo identificaría a las personas.` : 'Hay muy pocos casos para mostrarlo sin identificar a las personas.',
    casos: (n: number) => (n === 1 ? 'Sobre 1 caso' : `Sobre ${n} casos`),
    embudo: {
      titulo: 'Embudo de alta',
      explicacion: 'Cuántas personas dieron cada paso en el período. Cada paso cuenta sus propios hechos: no es la misma cohorte.',
    },
    calculo: 'Cada número sale del servicio dueño del dato y declara su fórmula en el catálogo de indicadores.',
    endpoints: {
      titulo: 'Endpoints de hoy',
      explicacion: 'Desde la medianoche hasta ahora: cuántas veces se llamó cada ruta, cuántas fallaron y cuánto tardó el 95 % de las llamadas.',
      cargando: 'Consultando el tráfico de hoy',
      vacio: 'Todavía nadie llamó a la plataforma hoy.',
      sinFuente: 'No pudimos leer el tráfico: el colector de métricas no respondió. Esto no significa que no haya habido llamadas.',
    },
  },
}
