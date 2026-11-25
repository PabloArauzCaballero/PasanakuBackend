import { describe, expect, it } from 'vitest'
import { estabilizar, pulsar, texto } from '../compartido/pruebas.apoyo'
import { EVIDENCIA_DE_PRUEBA, escribirCodigoDePrueba, verificarPasoDeSeguridad } from '../usuarios-internos/paso-de-seguridad.pruebas'
import { GW_ACCESOS, montarAccesos, responderPagina, URL_INFORME, URL_REVOCAR } from './pantalla-accesos.pruebas'

async function abrirRevocacion() {
  const { http, fixture } = montarAccesos()
  responderPagina(http, 'VIVOS')
  const vista = await estabilizar(fixture)
  pulsar(vista, 'Revocar AUDITOR de Persona de prueba')
  await estabilizar(fixture)
  const dialogo = vista.querySelector('dialog[open]')!
  return { http, fixture, vista, dialogo }
}

function escribirMotivo(dialogo: Element, valor: string): void {
  const motivo = dialogo.querySelector<HTMLTextAreaElement>('textarea')!
  motivo.value = valor
  motivo.dispatchEvent(new Event('input'))
}

describe('Revisión de accesos · informe de CU-08', () => {
  it('pagina en el servidor y reinicia la página al elegir otra sección', async () => {
    const { http, fixture } = montarAccesos()
    responderPagina(http, 'VIVOS', 1, 26)
    const vista = await estabilizar(fixture)
    expect(texto(vista)).toContain('Página 1 de 2')
    expect(texto(vista)).toContain('Persona de prueba')
    pulsar(vista, 'Siguiente')
    fixture.detectChanges()
    responderPagina(http, 'VIVOS', 2, 26)
    await estabilizar(fixture)
    expect(texto(vista)).toContain('Página 2 de 2')
    const secciones = vista.querySelector<HTMLSelectElement>('ap-lista-de-opciones select')!
    secciones.value = 'POR_VENCER'
    secciones.dispatchEvent(new Event('change'))
    fixture.detectChanges()
    responderPagina(http, 'POR_VENCER', 1, 0)
    await estabilizar(fixture)
    expect(texto(vista)).toContain('No hay accesos que venzan en los próximos 7 días')
    http.verify()
  })

  it('muestra un error accionable y permite reintentar sin exponer el rechazo crudo', async () => {
    const { http, fixture } = montarAccesos()
    http.expectOne(`${URL_INFORME}?seccion=VIVOS&pagina=1&limite=25`).flush({ mensaje: 'mensaje interno' }, { status: 500, statusText: 'Error' })
    const vista = await estabilizar(fixture)
    expect(texto(vista)).not.toContain('mensaje interno')
    pulsar(vista, 'Volver a intentar')
    fixture.detectChanges()
    responderPagina(http, 'VIVOS', 1, 0)
    await estabilizar(fixture)
    expect(texto(vista)).toContain('No hay accesos vivos')
    http.verify()
  })

  it('permite ver el informe a quien lee y oculta la revocación', async () => {
    const { http, fixture } = montarAccesos(['USUARIOS_INTERNOS_LEER'])
    responderPagina(http, 'VIVOS')
    const vista = await estabilizar(fixture)
    expect(texto(vista)).toContain('Persona de prueba')
    expect(texto(vista)).not.toContain('Revocar')
    http.verify()
  })
})

describe('Revisión de accesos · revocación de CU-08', () => {
  it('exige motivo y segundo factor, respeta la casilla y actualiza el informe', async () => {
    const { http, fixture, vista, dialogo } = await abrirRevocacion()
    const confirmar = Array.from(dialogo.querySelectorAll('button')).find((b) => texto(b) === 'Revocar acceso')!
    escribirMotivo(dialogo, 'corto')
    escribirCodigoDePrueba(dialogo)
    await estabilizar(fixture)
    expect(confirmar.disabled).toBe(true)
    const cerrarSesiones = dialogo.querySelector<HTMLInputElement>('input[type=checkbox]')!
    expect(cerrarSesiones.checked).toBe(true)
    cerrarSesiones.click()
    escribirMotivo(dialogo, 'Cambio de responsabilidad')
    await estabilizar(fixture)
    confirmar.click()
    verificarPasoDeSeguridad(http, GW_ACCESOS)
    const revocacion = http.expectOne(URL_REVOCAR)
    expect(revocacion.request.method).toBe('POST')
    expect(revocacion.request.body).toEqual({ motivoRevocacion: 'Cambio de responsabilidad', cerrarSesiones: false, evidenciaMfa: EVIDENCIA_DE_PRUEBA })
    revocacion.flush({ asignacionId: 'asignacion-1', usuarioId: 'usuario-1', revocadaEn: '2026-11-25T15:00:00Z', sesionesCerradas: 0, yaEstabaRevocada: false })
    fixture.detectChanges()
    responderPagina(http, 'VIVOS', 1, 0)
    await estabilizar(fixture)
    expect(texto(vista)).toContain('Se revocó el acceso')
    expect(vista.querySelector('dialog[open]')).toBeNull()
    http.verify()
  })

  it('traduce la protección del último administrador y conserva el motivo', async () => {
    const { http, fixture, vista, dialogo } = await abrirRevocacion()
    escribirMotivo(dialogo, 'Cambio de responsabilidad')
    escribirCodigoDePrueba(dialogo)
    await estabilizar(fixture)
    pulsar(dialogo, 'Revocar acceso')
    verificarPasoDeSeguridad(http, GW_ACCESOS)
    http.expectOne(URL_REVOCAR).flush({ codigo: 'AP-CU08-06', mensaje: 'crudo' }, { status: 422, statusText: 'Rechazado' })
    await estabilizar(fixture)
    expect(texto(vista)).toContain('sin ningún administrador de accesos')
    expect(dialogo.querySelector('textarea')?.value).toBe('Cambio de responsabilidad')
    http.verify()
  })
})
