import { describe, expect, it } from 'vitest'
import { CREDENCIAL, OBLIGACION, fila } from './catalogo-sintetico'
import { SIN_FILTROS, clasificacionDe, filasDeRelaciones, filasDeTablas, normalizar, urlDeFicha, urlDeTablas } from './dominio'
import { filasDeColumnas } from './revision'

describe('urlDeTablas · la página que se pide al servidor', () => {
  it('correcto: sin filtros pide la página y el límite, nada más', () => {
    expect(urlDeTablas('http://gw', SIN_FILTROS, 1, 20)).toBe('http://gw/indicadores/catalogo-de-datos/tablas?pagina=1&limite=20')
  })

  it('límite: solo los filtros con valor, en orden fijo; la búsqueda recortada a 80 y sin espacios a los lados', () => {
    const url = urlDeTablas('http://gw', { ...SIN_FILTROS, q: `  ${'a'.repeat(90)} `, revision: 'APROBADA', esquema: 'aportes' }, 3, 50)
    expect(url).toBe(`http://gw/indicadores/catalogo-de-datos/tablas?q=${'a'.repeat(80)}&esquema=aportes&revision=APROBADA&pagina=3&limite=50`)
  })

  it('inválido: una página 0 o negativa se pide como la primera; la ficha escapa lo que no es un nombre', () => {
    expect(urlDeTablas('http://gw', SIN_FILTROS, -4, 20)).toContain('pagina=1')
    expect(urlDeFicha('http://gw', 'aportes', 'pago/../x')).toBe('http://gw/indicadores/catalogo-de-datos/tablas/aportes/pago%2F..%2Fx')
  })
})

describe('normalizar', () => {
  it('correcto: sin acentos ni mayúsculas', () => {
    expect(normalizar('  PeRÍOdo ')).toBe('periodo')
  })
})

describe('filas · la tabla recibe texto', () => {
  it('correcto: una tabla de un esquema compartido dice «Compartido»; la sensibilidad va en palabras', () => {
    const [obligacion, credencial] = filasDeTablas([fila(OBLIGACION), fila(CREDENCIAL)])
    expect(obligacion).toMatchObject({ id: 'aportes.obligacion_aporte', sensible: 'Financiera', tonoSensible: 'info', columnas: '3', estado: 'Activa' })
    expect(credencial).toMatchObject({ servicio: 'Compartido', sensible: 'Ninguna', responsable: 'Sin asignar' })
  })

  it('correcto: la columna de convención lo aclara, la que no tiene nota lo dice y la sensibilidad va en palabras', () => {
    const [id, monto, grupo] = filasDeColumnas(OBLIGACION)
    expect(id!.negocio).toBe('Identificador estable')
    expect(monto).toMatchObject({ negocio: 'Lo que se pactó', sensibilidad: 'Dato financiero' })
    expect(grupo!.negocio).toBe('Sin nota de negocio')
  })
})

describe('filasDeRelaciones · lo que la base hace, en palabras', () => {
  it('correcto: cardinalidad, obligatoriedad y acción al borrar se leen sin saber SQL', () => {
    const [saliente] = filasDeRelaciones(OBLIGACION.relaciones.salientes)
    expect(saliente).toMatchObject({ cardinalidad: 'Muchos a uno', obligatoria: 'Obligatoria', alBorrar: 'Impide borrar la referida', restriccion: 'fk_obligacion_aporte_grupo_id' })
    const [entrante] = filasDeRelaciones(OBLIGACION.relaciones.entrantes)
    expect(entrante).toMatchObject({ cardinalidad: 'Uno a uno', obligatoria: 'Opcional', alBorrar: 'Queda vacía al borrar la referida' })
  })

  it('límite: una relación de un servidor viejo (sin los datos nuevos) muestra guiones, no «undefined»', () => {
    const [vieja] = filasDeRelaciones([{ columna: 'grupo_id', tabla: 'grupo', esquema: 'grupos' }])
    expect(vieja).toMatchObject({ cardinalidad: '—', alBorrar: '—', obligatoria: '—', restriccion: '—' })
  })
})

describe('clasificacionDe', () => {
  it('correcto: usa la del servidor; sin ella, cuenta las columnas', () => {
    expect(clasificacionDe(OBLIGACION).financieros).toBe(1)
    expect(clasificacionDe({ ...OBLIGACION, clasificacion: undefined })).toEqual({ personales: 0, financieros: 1, secretos: 0, derivados: 0 })
  })
})
