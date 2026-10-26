import type { HttpTestingController } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { asentar, boton, configurar, montar, texto } from '../comun/bandeja.apoyo'
import { ColaDeSolicitudesEscaladas } from './cola-de-solicitudes-escaladas'

/**
 * D-15 · **sin contrato**: ni `grupos.yaml` ni el simulado publican una cola de solicitudes escaladas (antes se pedía
 * `GET /grupos/solicitudes-escaladas` → 404 y los botones Aceptar/Rechazar no enviaban nada). Requisito vigente: la
 * pantalla no pide ningún endpoint inexistente, lo dice, y lleva al camino real (Admisiones, por identificador).
 */
const ID = '9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10'

describe('ColaDeSolicitudesEscaladas · sin contrato de cola (D-15)', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))

  it('aceptado: no hace ningún pedido y dice que la cola no existe, sin datos de ejemplo', async () => {
    const f = await montar(ColaDeSolicitudesEscaladas, ['SOLICITUD_INGRESO_VER'])
    http.verify()
    expect(texto(f)).toContain('Todavía no hay una cola de solicitudes escaladas.')
    expect(boton(f, 'Aceptar solicitud')).toBeUndefined()
    expect(boton(f, 'Rechazar solicitud')).toBeUndefined()
  })

  it('aceptado: con acceso a Admisiones, un identificador válido abre /admisiones/:id (en minúsculas y sin espacios)', async () => {
    const f = await montar(ColaDeSolicitudesEscaladas, ['SOLICITUD_INGRESO_VER', 'ADMIN_PLATAFORMA'])
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    f.componentInstance['solicitud'].set(`  ${ID.toUpperCase()} `)
    boton(f, 'Abrir en Admisiones')!.click()
    await asentar(f)
    expect(navegar).toHaveBeenCalledWith(['/admisiones', ID])
  })

  it('límite: sin acceso a Admisiones no se ofrece el formulario y se explica a quién le toca', async () => {
    const f = await montar(ColaDeSolicitudesEscaladas, ['SOLICITUD_INGRESO_VER'])
    expect(boton(f, 'Abrir en Admisiones')).toBeUndefined()
    expect(texto(f)).toContain('Resolver solicitudes de ingreso es de quien tiene acceso a Admisiones.')
  })

  it('error: un identificador mal formado se marca en el campo, conserva lo escrito y no navega', async () => {
    const f = await montar(ColaDeSolicitudesEscaladas, ['SOLICITUD_INGRESO_VER', 'ADMIN_PLATAFORMA'])
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    f.componentInstance['solicitud'].set('s-1')
    boton(f, 'Abrir en Admisiones')!.click()
    await asentar(f)
    expect(texto(f)).toContain('Ese identificador no tiene el formato de una solicitud de ingreso.')
    expect(f.componentInstance['solicitud']()).toBe('s-1')
    expect(navegar).not.toHaveBeenCalled()
  })
})
