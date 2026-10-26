import { describe, expect, it } from 'vitest'
import { boton, buscar, configurar, elegir, esperar, GW, montar, pagina, pieza, texto } from '../publicidad.apoyo'
import { ColaDeModeracion } from './cola-de-moderacion'

/**
 * H1.S3.M3 · CU-112 · La cola sobre `listarPiezasCreativas` por lotes. Moderación por identidad separada: sin
 * `PUBLICIDAD_MODERAR` no hay botones; R-PUB-05 (quien subió no modera) lo decide el servidor con `AP-CU112-03`.
 */
const URL = `${GW}/publicidad/piezas-creativas?limite=100&estadoModeracion=PENDIENTE`
const REVISION = `${GW}/publicidad/piezas-creativas/p-1/revision`

async function confirmar(accion: string, f: Parameters<typeof esperar>[0]): Promise<void> {
  boton(document.querySelector('ap-dialogo-de-moderacion')!, accion)!.click()
  await esperar(f)
}

describe('ColaDeModeracion · CU-112', () => {
  it('aceptado · la pieza se ve con «Ver pieza» (otra pestaña, sin opener) y con el permiso se ofrece aprobar y rechazar', async () => {
    const http = configurar(['PUBLICIDAD_MODERAR'])
    const f = await montar(ColaDeModeracion)
    http.expectOne(URL).flush(pagina([pieza()]))
    await esperar(f)
    expect(texto(f)).toContain('Banner de aniversario')
    const ver = f.nativeElement.querySelector('a[apAccionDeFila]') as HTMLAnchorElement
    expect(ver.getAttribute('href')).toBe('https://cdn.aportaya.test/p-1.png')
    expect(ver.getAttribute('rel')).toBe('noopener noreferrer')
    expect(boton(f.nativeElement, 'Aprobar pieza Banner de aniversario')).toBeDefined()
    expect(boton(f.nativeElement, 'Rechazar pieza Banner de aniversario')).toBeDefined()
  })

  it('aceptado · aprobar: la fila muestra «Aprobada» solo después del 2xx y deja de ofrecer la decisión', async () => {
    const http = configurar(['PUBLICIDAD_MODERAR'])
    const f = await montar(ColaDeModeracion)
    http.expectOne(URL).flush(pagina([pieza()]))
    await esperar(f)
    boton(f.nativeElement, 'Aprobar pieza Banner de aniversario')!.click()
    await esperar(f)
    await confirmar('Aprobar pieza', f)
    const req = http.expectOne(REVISION)
    expect(req.request.body).toEqual({ decision: 'APROBADA' })
    expect(f.nativeElement.querySelector('ap-tabla-de-datos')?.textContent).not.toContain('Aprobada')
    req.flush({ piezaCreativaId: 'p-1', estadoModeracion: 'APROBADA' }, { status: 201, statusText: 'Created' })
    await esperar(f)
    expect(f.nativeElement.querySelector('ap-tabla-de-datos')?.textContent).toContain('Aprobada')
    expect(boton(f.nativeElement, 'Aprobar pieza Banner de aniversario')).toBeUndefined()
  })

  it('aceptado · el estado consulta al servidor (lo ya resuelto) y la búsqueda cubre el texto de la pieza', async () => {
    const http = configurar(['PUBLICIDAD_MODERAR'])
    const f = await montar(ColaDeModeracion)
    http.expectOne(URL).flush(pagina([pieza(), pieza({ piezaCreativaId: 'p-2', titulo: 'Otra', texto: 'Promoción de octubre' })]))
    await esperar(f)
    await buscar(f, 'octubre')
    expect(texto(f)).toContain('1 de 2 piezas')
    await buscar(f, '')
    await elegir(f, 'Estado de moderación', 'RECHAZADA')
    http.expectOne(`${GW}/publicidad/piezas-creativas?limite=100&estadoModeracion=RECHAZADA`).flush(pagina([pieza({ estadoModeracion: 'RECHAZADA' })]))
    await esperar(f)
    expect(boton(f.nativeElement.querySelector('ap-tabla-de-datos')!, /^Aprobar pieza/)).toBeUndefined()
  })

  it('límite · sin PUBLICIDAD_MODERAR no se dibuja ni aprobar ni rechazar', async () => {
    const http = configurar([])
    const f = await montar(ColaDeModeracion)
    http.expectOne(URL).flush(pagina([pieza()]))
    await esperar(f)
    expect(boton(f.nativeElement.querySelector('ap-tabla-de-datos')!, /^Aprobar pieza/)).toBeUndefined()
    expect(boton(f.nativeElement.querySelector('ap-tabla-de-datos')!, /^Rechazar pieza/)).toBeUndefined()
  })

  it('límite · rechazar sin motivo no envía nada; cola vacía lo dice', async () => {
    const http = configurar(['PUBLICIDAD_MODERAR'])
    const f = await montar(ColaDeModeracion)
    http.expectOne(URL).flush(pagina([pieza()]))
    await esperar(f)
    boton(f.nativeElement, 'Rechazar pieza Banner de aniversario')!.click()
    await esperar(f)
    await confirmar('Rechazar pieza', f)
    expect(http.match(REVISION)).toHaveLength(0)
    expect(document.body.textContent).toContain('El rechazo exige un motivo escrito.')
    f.destroy()
    const g = await montar(ColaDeModeracion)
    http.expectOne(URL).flush(pagina([]))
    await esperar(g)
    expect(texto(g)).toContain('No hay piezas en este estado.')
  })

  it('error · 422 AP-CU112-03 (quien subió la pieza intenta moderarla): lo dice y la pieza NO queda resuelta', async () => {
    const http = configurar(['PUBLICIDAD_MODERAR'])
    const f = await montar(ColaDeModeracion)
    http.expectOne(URL).flush(pagina([pieza()]))
    await esperar(f)
    boton(f.nativeElement, 'Aprobar pieza Banner de aniversario')!.click()
    await esperar(f)
    await confirmar('Aprobar pieza', f)
    http.expectOne(REVISION).flush({ codigo: 'AP-CU112-03', mensaje: 'interno', trazaId: 'tz-3' }, { status: 422, statusText: 'x' })
    await esperar(f)
    expect(document.body.textContent).toContain('Quien subió esta pieza no puede moderarla (R-PUB-05)')
    expect(document.body.textContent).not.toContain('interno')
    expect(boton(f.nativeElement, 'Aprobar pieza Banner de aniversario')).toBeDefined()
  })

  it('error · 403 en la cola: alerta traducida y «Volver a intentar» repite el pedido', async () => {
    const http = configurar(['PUBLICIDAD_MODERAR'])
    const f = await montar(ColaDeModeracion)
    http.expectOne(URL).flush({ mensaje: 'interno' }, { status: 403, statusText: 'x' })
    await esperar(f)
    expect(f.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
    boton(f.nativeElement, /Volver a intentar|Reintentar/)!.click()
    await esperar(f)
    expect(http.match(URL)).toHaveLength(1)
  })
})
