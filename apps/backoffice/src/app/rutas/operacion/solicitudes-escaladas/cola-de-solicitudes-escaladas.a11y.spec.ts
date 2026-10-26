import type { HttpTestingController } from '@angular/common/http/testing'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { configurar, montar } from '../comun/bandeja.apoyo'
import { ColaDeSolicitudesEscaladas } from './cola-de-solicitudes-escaladas'

describe('ColaDeSolicitudesEscaladas · accesibilidad', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))

  it('sin violaciones con el aviso de «sin contrato» y el formulario para abrir en Admisiones', async () => {
    const f = await montar(ColaDeSolicitudesEscaladas, ['SOLICITUD_INGRESO_VER', 'ADMIN_PLATAFORMA'])
    expect(await axe(f.nativeElement)).toHaveNoViolations()
    http.verify()
  })

  it('sin violaciones con el identificador inválido marcado', async () => {
    const f = await montar(ColaDeSolicitudesEscaladas, ['SOLICITUD_INGRESO_VER', 'ADMIN_PLATAFORMA'])
    f.componentInstance['abrir']()
    f.detectChanges()
    expect(await axe(f.nativeElement)).toHaveNoViolations()
  })
})
