import type { ErrorTraducido } from '../../../nucleo/errores'

/**
 * Los rechazos de `cumplimiento.yaml` que esta sección distingue. `nucleo/errores.ts` todavía
 * no los tiene en su CATALOGO (pedido en el reporte del lote L2): sin esto, un 422 de regla
 * de negocio se mostraría como «algo salió mal de nuestro lado», que es falso y no dice qué
 * hacer. Cuando el CATALOGO los incorpore, este mapa se borra y queda `error.mensaje`.
 */
const FILTRO_INVALIDO = 'El filtro de la lista no es válido. Quitá los filtros y volvé a cargar.'

export const MENSAJES_DE_CUMPLIMIENTO: Readonly<Record<string, string>> = {
  'AP-CU43-01': 'No hay catálogo activo con ese código, o el período todavía no se generó. Revisá el período y el código.',
  'AP-CU43-02': 'Quien aprueba no puede ser quien generó el reporte. Lo tiene que aprobar otra persona.',
  'AP-CU43-03': 'El reporte ya no está en estado Generado: no se puede aprobar ni registrar su envío otra vez. Actualizá la lista.',
  'AP-CU43-04': 'El reporte está fuera de plazo: se registra igual y se abre un hallazgo.',
  'AP-CU43-05': FILTRO_INVALIDO,
  'AP-CU44-01': 'Falta la conclusión: no se cierra un caso sin justificarlo.',
  'AP-CU44-02': 'La revisión tiene que ser independiente: quien analizó el caso no puede decidirlo. Lo decide otra persona.',
  'AP-CU44-03': 'Para reportar, el reporte de operación sospechosa tiene que estar radicado. Revisá su identificador.',
  'AP-CU44-04': 'Se pasó el plazo de investigación de este caso.',
  'AP-CU44-05': FILTRO_INVALIDO,
  'AP-CU44-06': 'Ese caso no existe. Actualizá la bandeja.',
  'AP-CU44-07': 'Este caso ya tiene otra decisión. Actualizá la bandeja antes de repetir.',
  'AP-CU44-08': 'Alguna alerta no existe, es de otro titular o ya está en otro caso. Revisá la selección.',
  'AP-CU45-01': 'Ese número de oficio ya fue registrado.',
  'AP-CU45-02': 'Sin el oficio y su hash no se actúa. Cargá el enlace al documento y su hash.',
  'AP-CU45-03': 'El alcance es ambiguo: pedí una aclaración a la autoridad antes de entregar información.',
  'AP-CU45-04': 'El plazo de respuesta ya venció: se responde igual y se abre un hallazgo.',
  'AP-CU45-05': FILTRO_INVALIDO,
  'AP-CU54-04': FILTRO_INVALIDO,
  'AP-CU55-04': FILTRO_INVALIDO,
  'AP-CU56-01': 'Una prueba exitosa exige el acta del comité que la reporta.',
  'AP-CU56-02': 'El RTO obtenido supera el comprometido en el plan.',
  'AP-CU56-03': 'Ese proceso crítico no tiene plan de continuidad.',
  'AP-CU56-04': FILTRO_INVALIDO,
  'AP-CU94-07': FILTRO_INVALIDO,
}

/** Lo que la vista muestra: el error ya traducido por el interceptor, con el texto propio si el código es de acá. */
export function traducirError(error: unknown): ErrorTraducido {
  const e = (error ?? {}) as Partial<ErrorTraducido>
  const base: ErrorTraducido = { ...e, mensaje: e.mensaje ?? 'Algo salió mal de nuestro lado. Probá de nuevo en un momento.' }
  const propio = e.codigo ? MENSAJES_DE_CUMPLIMIENTO[e.codigo] : undefined
  return propio ? { ...base, mensaje: propio } : base
}
