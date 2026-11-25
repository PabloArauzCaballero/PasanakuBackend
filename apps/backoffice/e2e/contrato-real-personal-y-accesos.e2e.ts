import { expect, test } from '@playwright/test'
import { iniciarSesionDePrueba, navegarSinRecargar } from './apoyo/sesion-e2e'

/**
 * H6.S1.M5 · «Personal y accesos» contra el simulado del contrato REAL de identidad (#516 en `dev`), sin `page.route`:
 * el gateway del E2E responde con los ejemplos de `packages/simulado/ejemplos/identidad` (Prism sobre `identidad.yaml`).
 * Es el contrato real servido por un simulador, no el servicio real: no prueba reglas del servidor.
 */
const PERMISOS = ['USUARIOS_INTERNOS_GESTIONAR', 'ACCESOS_ADMINISTRAR']

test('revisión de accesos: el informe del contrato real se pinta y la consola queda limpia', async ({ page }) => {
  const errores: string[] = []
  page.on('console', (m) => {
    if (m.type() === 'error') errores.push(m.text())
  })
  await iniciarSesionDePrueba(page, PERMISOS)
  await navegarSinRecargar(page, '/sistemas/accesos')
  await expect(page.getByRole('heading', { level: 1, name: /Revisión de accesos/ })).toBeVisible()
  await expect(page.getByRole('button', { name: /Revocar/ }).first()).toBeVisible()
  expect(errores).toEqual([])
})

test('usuarios internos: el listado del contrato real (cursor con limite) se pinta y la consola queda limpia', async ({ page }) => {
  const errores: string[] = []
  const pedidos: string[] = []
  page.on('console', (m) => {
    if (m.type() === 'error') errores.push(m.text())
  })
  page.on('request', (r) => {
    if (/\/usuarios-internos\?/.test(r.url())) pedidos.push(r.url())
  })
  await iniciarSesionDePrueba(page, PERMISOS)
  await navegarSinRecargar(page, '/sistemas/usuarios-internos')
  await expect(page.getByRole('heading', { level: 1, name: /Usuarios internos/ })).toBeVisible()
  await expect(page.locator('summary[aria-label^="Más acciones para"]').first()).toBeVisible()
  expect(pedidos.some((u) => /limite=50/.test(u))).toBe(true)
  expect(errores).toEqual([])
})
