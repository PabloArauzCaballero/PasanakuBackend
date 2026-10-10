import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import Ajv2020 from 'ajv/dist/2020'
import addFormats from 'ajv-formats'
import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { firstValueFrom } from 'rxjs'
import { beforeEach, describe, expect, it } from 'vitest'
import { crearCierreDePeriodo } from '../rutas/contabilidad/dominio/cu100-periodos'
import { crearPagoDeFactura } from '../rutas/contabilidad/dominio/cu103-facturas'
import { crearCobro } from '../rutas/contabilidad/dominio/cu104-cobros'
import { crearDepreciacion } from '../rutas/contabilidad/dominio/cu105-activos'
import { crearGeneracion } from '../rutas/contabilidad/dominio/cu106-estados-financieros'
import { crearResolver } from '../rutas/admisiones/dominio/cu68-admision'
import { crearAprobarRetiro } from '../rutas/operacion/retiros/dominio/cu11-aprobacion-de-retiro'
import { GATEWAY } from './gateway'

/**
 * **Entrada ESTRICTA del servidor (H14.S1.M4 · H12.S1.M1).** Los contratos declaran `additionalProperties: false`: un campo de más es 400,
 * no se ignora (regla 96.2). Esta prueba toma los cuerpos que arma la capa de dominio del backoffice —los que NO vienen tipados desde el
 * cliente generado— y los valida contra el esquema de petición del contrato real (`packages/simulado/generado`, derivado de los OpenAPI).
 *
 * Los literales de entrada son los mismos que las pruebas de cada pantalla fijan con `toEqual` sobre `request.body` (ficha de cobro, ficha de
 * factura, formulario de resolución y la pantalla de retiros): la cadena es «la pantalla produce este cuerpo» (su prueba) + «este cuerpo
 * cumple el contrato» (esta). Las operaciones de publicidad e identidad usan tipos del cliente generado (`clientes/angular/*`), cuyo
 * chequeo de propiedades de más lo hace el compilador.
 *
 * No cubierto: `extraccion/*`, `sesion/refrescar` y `identidad/tutoriales/*` (no tienen contrato en este repositorio: ver
 * `evidencia/carril-F4/2-rutas-del-frontend-vs-contratos.md`).
 */
const GENERADO = join(__dirname, '../../../../../packages/simulado/generado')
const GW = 'http://gw/api/v1'
const ajv = new Ajv2020({ strict: false, allErrors: true })
addFormats(ajv)

type Documento = { paths: Record<string, Record<string, { requestBody?: { content?: Record<string, { schema?: unknown }> } }>>; components: unknown }
const documentos = new Map<string, Documento>()
function documento(servicio: string): Documento {
  let d = documentos.get(servicio)
  if (!d) {
    d = JSON.parse(readFileSync(join(GENERADO, `${servicio}.json`), 'utf8')) as Documento
    documentos.set(servicio, d)
  }
  return d
}

/** Valida `cuerpo` contra el esquema de petición de `metodo ruta` del contrato. Devuelve los errores, vacío si cumple. */
function errores(servicio: string, metodo: string, ruta: string, cuerpo: unknown): string[] {
  const d = documento(servicio)
  const esquema = d.paths[ruta]?.[metodo]?.requestBody?.content?.['application/json']?.schema
  if (!esquema) return [`el contrato no declara cuerpo para ${metodo.toUpperCase()} ${ruta}`]
  const validar = ajv.compile({ ...(esquema as object), components: d.components })
  return validar(cuerpo) ? [] : [ajv.errorsText(validar.errors)]
}

describe('entrada estricta · los cuerpos de la capa de dominio cumplen el contrato (sin campos de más)', () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  /** Dispara la operación, captura el cuerpo que salió por la red y responde vacío. */
  async function cuerpoDe(url: string, enviar: () => Promise<unknown>): Promise<unknown> {
    const pendiente = enviar().catch(() => undefined)
    const r = http.expectOne((req) => req.url === `${GW}${url}`)
    const cuerpo = r.request.body
    r.flush({})
    await pendiente
    return cuerpo
  }

  it('erp · cierre de período: { glosa }', async () => {
    const cierre = TestBed.runInInjectionContext(() => crearCierreDePeriodo())
    const c = await cuerpoDe('/erp/periodos/p1/cierre', () => firstValueFrom(cierre('p1', 'Cierre de octubre')))
    expect(errores('erp', 'post', '/erp/periodos/{periodoId}/cierre', c)).toEqual([])
  })

  it('erp · pago de factura de proveedor: DIVERGENCIA DECLARADA — no manda `autorizadoPor`, que el contrato exige (required)', async () => {
    // Decisión de seguridad documentada en `cu103-facturas.ts` (EntradaPago): el «quién autorizó» sale de la sesión, no del cliente. Contra el
    // backend real eso es un 400 por `required` HASTA que erp derive `autorizadoPor` del token y quite el campo del contrato. Esta prueba fija
    // la divergencia EXACTA y falla el día que se resuelva en cualquiera de los dos sentidos, para obligar a actualizarla.
    const pagar = TestBed.runInInjectionContext(() => crearPagoDeFactura())
    const c = await cuerpoDe('/erp/facturas-de-proveedor/f1/pagos', () => firstValueFrom(pagar('f1', { monto: '100.00', moneda: 'BOB', formaPago: 'TRANSFERENCIA' })))
    const e = errores('erp', 'post', '/erp/facturas-de-proveedor/{facturaId}/pagos', c)
    expect(e).toHaveLength(1)
    expect(e[0]).toContain("required property 'autorizadoPor'")
    expect(e[0]).not.toContain('additional')
  })

  it('erp · cobro de una cuenta por cobrar: { monto, formaCobro }', async () => {
    const cobrar = TestBed.runInInjectionContext(() => crearCobro())
    const c = await cuerpoDe('/erp/cuentas-por-cobrar/c1/cobros', () => firstValueFrom(cobrar('c1', { monto: '100.00', formaCobro: 'TRANSFERENCIA' })))
    expect(errores('erp', 'post', '/erp/cuentas-por-cobrar/{cuentaId}/cobros', c)).toEqual([])
  })

  it('erp · depreciación de un activo: { periodoContableId }', async () => {
    const depreciar = TestBed.runInInjectionContext(() => crearDepreciacion())
    const c = await cuerpoDe('/erp/activos/a1/depreciaciones', () => firstValueFrom(depreciar('a1', '5a2c1f0e-3b7d-4c11-9e55-0a1b2c3d4e5f')))
    expect(errores('erp', 'post', '/erp/activos/{activoId}/depreciaciones', c)).toEqual([])
  })

  it('erp · estados financieros: { tipo }', async () => {
    const generar = TestBed.runInInjectionContext(() => crearGeneracion())
    const c = await cuerpoDe('/erp/periodos/p1/estados-financieros', () => firstValueFrom(generar('p1', 'BALANCE_GENERAL')))
    expect(errores('erp', 'post', '/erp/periodos/{periodoId}/estados-financieros', c)).toEqual([])
  })

  it('grupos · resolución de admisión: { decision, motivo, revisionEsperada, propuestaId }', async () => {
    const resolver = TestBed.runInInjectionContext(() => crearResolver())
    const entrada = { decision: 'ACEPTAR' as const, motivo: 'Evidencia suficiente.', revisionEsperada: 1, propuestaId: '5a2c1f0e-3b7d-4c11-9e55-0a1b2c3d4e5f' }
    const c = await cuerpoDe('/grupos/solicitudes/s1/resoluciones', () => firstValueFrom(resolver('s1', entrada, '5a2c1f0e-3b7d-4c11-9e55-0a1b2c3d4e50')))
    expect(errores('grupos', 'post', '/grupos/solicitudes/{solicitudId}/resoluciones', c)).toEqual([])
  })

  it('nucleo-financiero · aprobación de retiro: { desenlace } y { desenlace, motivo }', async () => {
    const aprobar = TestBed.runInInjectionContext(() => crearAprobarRetiro())
    for (const entrada of [{ desenlace: 'AUTORIZADA' as const }, { desenlace: 'RECHAZADA' as const, motivo: 'Titular no coincide.' }]) {
      const c = await cuerpoDe('/billetera/retiros/o1/aprobacion', () => firstValueFrom(aprobar('o1', entrada)))
      expect(errores('nucleo-financiero', 'post', '/billetera/retiros/{ordenId}/aprobacion', c)).toEqual([])
    }
  })

  it('el validador detecta un campo de más (la prueba no es vacía)', () => {
    expect(errores('nucleo-financiero', 'post', '/billetera/retiros/{ordenId}/aprobacion', { desenlace: 'AUTORIZADA', ordenId: 'x' })).not.toEqual([])
    expect(errores('erp', 'post', '/erp/periodos/{periodoId}/cierre', { glosa: 'x', periodoId: 'p1' })).not.toEqual([])
  })
})
