/** Los textos del catálogo de datos, en voz de marca. */
export const textosCatalogo = {
  titulo: 'Catálogo de datos',
  proposito:
    'Cada tabla de la plataforma con por qué existe, qué servicio la escribe y cómo se relaciona con las demás. Es la estructura, nunca los datos de las personas.',
  busqueda: 'Buscar tabla, columna o concepto',
  filtroEsquemas: 'Esquema',
  cargando: 'Consultando el catálogo',
  vacio: 'Ninguna tabla coincide con la búsqueda. Probá con otra palabra o quitá el filtro de esquema.',
  columnasLista: { tabla: 'Tabla', esquema: 'Esquema', servicio: 'Servicio dueño', columnas: 'Columnas', queEs: 'Qué es' },
  resultados: (n: number, total: number) => (n === total ? `${total} tablas` : `${n} de ${total} tablas`),
  ficha: {
    volver: 'Volver al catálogo',
    noExiste: (tabla: string) => `La tabla «${tabla}» no está en el catálogo. Puede que se haya renombrado: buscala en la lista.`,
    queEs: 'Qué es',
    negocio: 'Para qué sirve',
    porQue: 'Por qué existe',
    sistema: 'A nivel de sistema',
    notaDeSistema: 'Nota de la ficha',
    appendOnly: 'Solo se agregan filas: nunca se editan ni se borran.',
    reglas: 'Reglas nombradas',
    columnas: 'Columnas',
    relaciones: 'Relaciones',
    salientes: 'Esta tabla apunta a',
    entrantes: 'Apuntan a esta tabla',
    sinRelaciones: 'Ninguna',
    columnasTabla: { nombre: 'Columna', tipo: 'Tipo', negocio: 'Negocio', sistema: 'Sistema', sensibilidad: 'Sensibilidad' },
  },
}
