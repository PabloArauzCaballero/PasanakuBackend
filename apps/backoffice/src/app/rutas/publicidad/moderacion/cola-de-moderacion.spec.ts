import { HttpTestingController } from '@angular/common/http/testing'
import { describe, expect, it } from 'vitest'
import { configurarListado, esperar, GW, montarListado, textoDe } from '../../contabilidad/listado/listado.apoyo'
import { pagina, pieza } from '../publicidad.apoyo'
import { ColaDeModeracion } from './cola-de-moderacion'

/**
 * H1.S3.M3 · CU-112 · La cola sobre `listarPiezasCreativas` por cursor. Moderación por identidad separada: sin
 * `PUBLICIDAD_MODERAR` no hay botones; R-PUB-05 (quien subió no modera) lo decide el servidor con `AP-CU112-03`.
 */
const URL = `${GW}/publicidad/piezas-creativas?limite=50&estadoModeracion=PENDIENTE`
const REVISION = `${GW}/publicidad/piezas-creativas/p-1/revision`

/** Botones de la FILA (el diálogo abierto repite el texto en su botón de confirmar). */
function botones(f: { nativeElement: HTMLElement }, texto: string): HTMLButtonElement[] {
  return [...f.nativeElement.querySelectorAll('[role="cell"] ap-boton button')].filter((b) => (b as HTMLElement).textContent?.includes(texto)) as HTMLButtonElement[]
}

async function abrirYConfirmar(http: HttpTestingController, f: Awaited<ReturnType<typeof montarListado<ColaDeModeracion>>>, accion: string) {
  botones(f, accion)[0]!.click()
  await esperar(f)
  const confirmar = [...document.querySelectorAll('ap-dialogo button')].find((b) => (b as HTMLElement).textContent?.trim() === accion) as HTMLButtonElement
  confirmar.click()
  await esperar(f)
  return http
}

describe('ColaDeModeracion · CU-112 por cursor', () => {
  it('correcto · la pieza se ve (enlace al recurso, fecha) y con el permiso se ofrece aprobar y rechazar', async () => {
    const http = configurarListado(['PUBLICIDAD_MODERAR'])
    const f = await montarListado(http, ColaDeModeracion, URL, { cuerpo: pagina([pieza()]) })
    expect(textoDe(f)).toContain('Banner de aniversario')
    expect(f.nativeElement.querySelector('a.ver')?.getAttribute('href')).toBe('https://cdn.aportaya.test/p-1.png')
    expect(f.nativeElement.querySelector('a.ver')?.getAttribute('rel')).toBe('noopener noreferrer')
    expect(botones(f, 'Aprobar pieza')).toHaveLength(1)
    expect(botones(f, 'Rechazar pieza')).toHaveLength(1)
  })

  it('identidad separada · sin PUBLICIDAD_MODERAR no se dibuja ni aprobar ni rechazar', async () => {
    const http = configurarListado([])
    const f = await montarListado(http, ColaDeModeracion, URL, { cuerpo: pagina([pieza()]) })
    expect(botones(f, 'Aprobar pieza')).toHaveLength(0)
    expect(botones(f, 'Rechazar pieza')).toHaveLength(0)
  })

  it('aprobar: la fila muestra «Aprobada» solo después del 2xx', async () => {
    const http = configurarListado(['PUBLICIDAD_MODERAR'])
    const f = await montarListado(http, ColaDeModeracion, URL, { cuerpo: pagina([pieza()]) })
    await abrirYConfirmar(http, f, 'Aprobar pieza')
    const req = http.expectOne(REVISION)
    expect(req.request.body).toEqual({ decision: 'APROBADA' })
    expect(f.nativeElement.querySelector('[role="cell"] ap-chip-estado:not([tono="info"])')).toBeNull()
    req.flush({ revisionId: 'r-1', estadoModeracion: 'APROBADA' }, { status: 201, statusText: 'Created' })
    await esperar(f)
    expect(textoDe(f)).toContain('Aprobada')
    expect(botones(f, 'Aprobar pieza')).toHaveLength(0)
  })

  it('inválido · 422 AP-CU112-03 (quien subió la pieza intenta moderarla): lo dice y la pieza NO queda resuelta', async () => {
    const http = configurarListado(['PUBLICIDAD_MODERAR'])
    const f = await montarListado(http, ColaDeModeracion, URL, { cuerpo: pagina([pieza()]) })
    await abrirYConfirmar(http, f, 'Aprobar pieza')
    http.expectOne(REVISION).flush({ codigo: 'AP-CU112-03', mensaje: 'interno', trazaId: 'tz-3' }, { status: 422, statusText: 'x' })
    await esperar(f)
    expect(document.body.textContent).toContain('Quien subió esta pieza no puede moderarla (R-PUB-05)')
    expect(document.body.textContent).not.toContain('interno')
    expect(botones(f, 'Aprobar pieza')).toHaveLength(1)
  })

  it('límite · rechazar sin motivo no envía nada', async () => {
    const http = configurarListado(['PUBLICIDAD_MODERAR'])
    const f = await montarListado(http, ColaDeModeracion, URL, { cuerpo: pagina([pieza()]) })
    await abrirYConfirmar(http, f, 'Rechazar pieza')
    expect(http.match(REVISION)).toHaveLength(0)
    expect(document.body.textContent).toContain('El rechazo exige un motivo escrito.')
  })

  it('límite · cola vacía lo dice', async () => {
    const http = configurarListado(['PUBLICIDAD_MODERAR'])
    const f = await montarListado(http, ColaDeModeracion, URL, { cuerpo: pagina([]) })
    expect(textoDe(f)).toContain('No hay piezas pendientes de moderación.')
  })

  it('inválido · 403 en la cola: alerta traducida', async () => {
    const http = configurarListado(['PUBLICIDAD_MODERAR'])
    const f = await montarListado(http, ColaDeModeracion, URL, { status: 403, cuerpo: { mensaje: 'interno' } })
    expect(f.nativeElement.querySelector('ap-estado-de-listado [role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
  })
})
