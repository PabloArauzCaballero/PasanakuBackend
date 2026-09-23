import type { Page } from '@playwright/test'
import { iniciarSesionDePrueba } from './apoyo/sesion-e2e'

/** Abre la sesión real de CU-04 sin permisos de sección para los tutoriales públicos. */
export async function sesionDeOperador(page: Page): Promise<void> {
  await iniciarSesionDePrueba(page, [])
}
