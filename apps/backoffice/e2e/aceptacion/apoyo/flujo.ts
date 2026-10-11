import { expect, type Page } from '@playwright/test'
import type { ClienteApi, DecisionApi } from './api'
import type { Mundo } from './escenario'
import { irA } from './escenario'

export type Clientes = { organizador: ClienteApi; a: ClienteApi; a2: ClienteApi; b: ClienteApi; b2: ClienteApi; sinPermiso: ClienteApi }
export type Solicitud = { grupoId: string; invitacionId: string; token: string; solicitudId: string; propuesta: DecisionApi | null }

/** Camino previo común a F2/F3/F4: el organizador invita a A, A canjea (pide ingreso) y, si se pide, el organizador propone. */
export async function prepararSolicitud(
  api: Clientes,
  mundo: Mundo,
  propone: { decision: 'ACEPTAR' | 'RECHAZAR'; motivo: string } | null,
): Promise<Solicitud> {
  const inv = await api.organizador.invitar(mundo.grupoId, mundo.participante.telefono)
  expect(inv.estado, `invitar: ${inv.texto.slice(0, 200)}`).toBe(201)
  const canje = await api.a.canjear(mundo.grupoId, inv.cuerpo.invitacionId, inv.cuerpo.token)
  expect(canje.estado, `canjear: ${canje.texto.slice(0, 200)}`).toBe(200)
  expect(canje.cuerpo.estado).toBe('PENDIENTE_BACKOFFICE')
  let propuesta: DecisionApi | null = null
  if (propone) {
    const p = await api.organizador.proponer(canje.cuerpo.solicitudId, propone.decision, propone.motivo, 0)
    expect(p.estado, `proponer: ${p.texto.slice(0, 200)}`).toBe(200)
    propuesta = p.cuerpo
  }
  return { grupoId: mundo.grupoId, invitacionId: inv.cuerpo.invitacionId, token: inv.cuerpo.token, solicitudId: canje.cuerpo.solicitudId, propuesta }
}

/** Desde cualquier pantalla del backoffice: «Solicitudes de ingreso» → pegar el identificador → «Abrir solicitud». */
export async function abrirExpediente(page: Page, solicitudId: string): Promise<void> {
  await irA(page, '/admisiones')
  await expect(page.getByRole('heading', { name: 'Abrir una solicitud' })).toBeVisible()
  await page.getByLabel(/Identificador de la solicitud/).fill(solicitudId)
  await page.getByRole('button', { name: 'Abrir solicitud' }).click()
  await expect(page).toHaveURL(new RegExp(`/admisiones/${solicitudId}$`))
}

/** Completa el formulario de resolución (sin enviarlo). */
export async function llenarResolucion(page: Page, decision: 'Aceptar' | 'Rechazar', motivo: string): Promise<void> {
  await page.getByLabel(decision, { exact: true }).check({ force: true })
  await page.getByLabel(/Motivo de la resolución/).fill(motivo)
}

export const resolver = (page: Page) => page.getByRole('button', { name: 'Resolver solicitud' })

export function unica<T>(xs: T[], que: string): T {
  expect(xs, que).toHaveLength(1)
  return xs[0] as T
}
