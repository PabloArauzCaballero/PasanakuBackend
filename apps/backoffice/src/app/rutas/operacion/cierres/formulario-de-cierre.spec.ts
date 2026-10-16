import type { HttpTestingController } from '@angular/common/http/testing'
import type { ComponentFixture } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { configurar, GW, montar, texto } from '../comun/bandeja.apoyo'
import { hoyEnLaPaz } from './dominio/cu51-cierres-diarios'
import { FormularioDeCierre } from './formulario-de-cierre'

/** **UI contra HTTP simulado** de `ejecutarCierreDiario` (CU-51). Datos del ejemplo generado del contrato (sintéticos). */
const URL = `${GW}/contabilidad/cierres-diarios`
const NUEVO = { ...(ejemploDe('nucleo-financiero', 'ejecutarCierreDiario', 'vacio').cuerpo as object), fecha: '2026-10-14', yaExistia: false, cuadrado: false, motivoDelDescuadre: 'Sin conciliación de custodia del día' }
const EXISTENTE = ejemploDe('nucleo-financiero', 'ejecutarCierreDiario', 'ok').cuerpo as { cierreId: string }

describe('FormularioDeCierre · [UI contra HTTP simulado] CU-51', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))
  async function abrir(): Promise<ComponentFixture<FormularioDeCierre>> {
    const f = await montar(FormularioDeCierre, ['CONTABILIDAD'])
    f.componentInstance.hoy = () => '2026-10-16'
    return f
  }
  function pedir(f: ComponentFixture<FormularioDeCierre>, fecha: string) {
    f.componentInstance['fecha'].set(fecha)
    f.componentInstance['revisar']()
    f.detectChanges()
  }
  const confirmar = (f: ComponentFixture<FormularioDeCierre>) => f.componentInstance['enviar'](f.componentInstance['pendiente']()!)

  it('correcto: confirma con la consecuencia, manda solo { fecha } con Idempotency-Key y muestra el comprobante con el 201', async () => {
    const f = await abrir()
    pedir(f, '2026-10-14')
    http.expectNone(URL)
    expect(texto(f)).toContain('Vas a cerrar el día 14 oct 2026')
    confirmar(f)
    f.detectChanges()
    confirmar(f)
    const req = http.expectOne(URL)
    expect(req.request.body).toEqual({ fecha: '2026-10-14' })
    expect(req.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(texto(f)).not.toContain('Día cerrado')
    req.flush(NUEVO, { status: 201, statusText: 'Created' })
    f.detectChanges()
    expect(texto(f)).toContain('Día cerrado')
    expect(texto(f)).toContain('Con observaciones')
    expect(texto(f)).toContain('Sin conciliación de custodia del día')
  })

  it('límite: un día ya cerrado (200, yaExistia) lo dice sin dar a entender que se cerró ahora', async () => {
    const f = await abrir()
    pedir(f, '2026-10-15')
    confirmar(f)
    http.expectOne(URL).flush(EXISTENTE)
    f.detectChanges()
    expect(texto(f)).toContain('Ese día ya estaba cerrado')
    expect(texto(f)).toContain(EXISTENTE.cierreId)
  })

  it('límite: hoy o un día futuro en La Paz no se pide; «hoy» se calcula en UTC−4', async () => {
    const f = await abrir()
    pedir(f, '2026-10-16')
    expect(f.componentInstance['pendiente']()).toBeNull()
    expect(texto(f)).toContain('Elegí un día ya terminado')
    http.expectNone(URL)
    expect(hoyEnLaPaz(new Date('2026-10-17T03:59:00Z'))).toBe('2026-10-16')
    expect(hoyEnLaPaz(new Date('2026-10-17T04:00:00Z'))).toBe('2026-10-17')
  })

  it('inválido: 422 AP-CU51-08 se explica sin comprobante; 403 dice «sin acceso»; sin respuesta queda «sin confirmar»', async () => {
    const f = await abrir()
    pedir(f, '2026-10-14')
    confirmar(f)
    http.expectOne(URL).flush({ codigo: 'AP-CU51-08', mensaje: 'crudo', trazaId: 't' }, { status: 422, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('Aportes no informó el recaudo')
    expect(texto(f)).not.toContain('Día cerrado')
    pedir(f, '2026-10-14')
    confirmar(f)
    http.expectOne(URL).flush({ codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, { status: 403, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('No tenés acceso a esto.')
    pedir(f, '2026-10-14')
    confirmar(f)
    http.expectOne(URL).error(new ProgressEvent('error'), { status: 0 })
    f.detectChanges()
    expect(texto(f)).toContain('No pudimos confirmar si el día quedó cerrado')
  })
})
