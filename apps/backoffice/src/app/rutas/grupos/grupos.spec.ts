import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting, type TestRequest } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../nucleo/errores.interceptor'
import { GATEWAY } from '../../nucleo/gateway'
import { DetalleDeGrupo } from './detalle-de-grupo'
import type { DetalleDeGrupoDeBackoffice, GrupoDeBackoffice } from './dominio/consulta-de-grupos'
import { ListadoDeGrupos } from './listado-de-grupos'

/** **Prueba de UI contra HTTP simulado**: las pantallas y su cliente, NO la integración con `grupos`. Datos sintéticos. */
const GW = 'http://gw/api/v1'
const URL = `${GW}/grupos/backoffice`
const ID = 'c9900000-0000-4000-8000-000000000001'
const grupo = (cambios: Partial<GrupoDeBackoffice> = {}): GrupoDeBackoffice => ({
  grupoId: ID,
  codigo: 'GR-PRUEBA1',
  nombre: 'Pasanaku sintético',
  estado: 'EN_CURSO',
  montoAporte: '500.00',
  moneda: 'BOB',
  periodicidad: 'MENSUAL',
  cuposTotales: 10,
  cuposOcupados: 7,
  fechaInicio: '2026-11-01',
  ...cambios,
})

describe('Grupos del backoffice · [UI contra HTTP simulado]', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([{ path: 'grupos', component: ListadoDeGrupos }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''
  const pedidoListado = (): TestRequest => http.expectOne((r) => r.url === URL)
  async function listado(cuerpo: GrupoDeBackoffice[]) {
    const f = TestBed.createComponent(ListadoDeGrupos)
    f.detectChanges()
    pedidoListado().flush(cuerpo)
    await f.whenStable()
    f.detectChanges()
    return f
  }

  describe('listado', () => {
    it('cargando: avisa que está trayendo los grupos', () => {
      const f = TestBed.createComponent(ListadoDeGrupos)
      f.detectChanges()
      expect((f.nativeElement as HTMLElement).querySelector('[role="status"]')?.getAttribute('aria-label')).toContain('Cargando')
    })

    it('con datos: estado legible, ocupación y aporte formateado desde el decimal en texto', async () => {
      const f = await listado([grupo()])
      expect(texto(f)).toContain('Pasanaku sintético')
      expect(texto(f)).toContain('En curso')
      expect(texto(f)).not.toContain('EN_CURSO')
      expect(texto(f)).toContain('7 / 10')
      expect(texto(f)).toContain('500')
    })

    it('vacío: dice que no hay grupos con ese estado', async () => {
      const f = await listado([])
      expect(texto(f)).toContain('No hay grupos con este estado')
    })

    it('búsqueda: filtra por nombre o código en el cliente, y sin coincidencias orienta', async () => {
      await TestBed.inject(Router).navigateByUrl('/grupos?q=prueba1')
      const f = await listado([grupo(), grupo({ grupoId: 'otro', codigo: 'GR-OTRO', nombre: 'Otro' })])
      expect(texto(f)).toContain('GR-PRUEBA1')
      expect(texto(f)).not.toContain('GR-OTRO')
      f.destroy()
      await TestBed.inject(Router).navigateByUrl('/grupos?q=nada')
      const g = await listado([grupo()])
      expect(texto(g)).toContain('Ningún grupo coincide')
    })

    it('filtro: un estado del esquema viaja al servidor; uno desconocido no se manda', async () => {
      await TestBed.inject(Router).navigateByUrl('/grupos?estado=SUSPENDIDO')
      const f = TestBed.createComponent(ListadoDeGrupos)
      f.detectChanges()
      expect(pedidoListado().request.params.get('estado')).toBe('SUSPENDIDO')
      f.destroy()
      await TestBed.inject(Router).navigateByUrl('/grupos?estado=INVENTADO')
      const g = TestBed.createComponent(ListadoDeGrupos)
      g.detectChanges()
      expect(pedidoListado().request.params.has('estado')).toBe(false)
    })

    it('error: mensaje accionable, sin el texto crudo del backend', async () => {
      const f = TestBed.createComponent(ListadoDeGrupos)
      f.detectChanges()
      pedidoListado().flush({ codigo: 'AP-SEG-01', mensaje: 'crudo del backend', trazaId: 't' }, { status: 403, statusText: 'x' })
      await f.whenStable()
      f.detectChanges()
      expect(texto(f)).not.toContain('crudo del backend')
      expect(texto(f)).toContain('Volver a intentar')
    })
  })

  describe('detalle', () => {
    async function detalle(cuerpo: DetalleDeGrupoDeBackoffice | null, error?: { codigo: string }) {
      const f = TestBed.createComponent(DetalleDeGrupo)
      f.componentRef.setInput('grupoId', ID)
      f.detectChanges()
      const r = http.expectOne(`${URL}/${ID}`)
      if (error) r.flush({ ...error, mensaje: 'crudo del backend', trazaId: 't' }, { status: 422, statusText: 'x' })
      else r.flush(cuerpo)
      await f.whenStable()
      f.detectChanges()
      return f
    }

    it('con datos: el nombre del grupo como título, sus datos y cada cupo con alias, libre y quién organiza', async () => {
      const f = await detalle({
        grupo: grupo(),
        cupos: [
          { numero: 1, estado: 'OCUPADO', alias: 'Ana', estadoParticipante: 'ACTIVO', esOrganizador: true },
          { numero: 2, estado: 'LIBRE', alias: null, estadoParticipante: null, esOrganizador: false },
        ],
      })
      expect((f.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain('Pasanaku sintético')
      expect(texto(f)).toContain('GR-PRUEBA1')
      expect(texto(f)).toContain('Mensual')
      expect(texto(f)).toContain('Ana')
      expect(texto(f)).toContain('Organiza')
      expect(texto(f)).toContain('Libre')
    })

    it('vacío: un grupo sin cupos lo dice', async () => {
      const f = await detalle({ grupo: grupo({ fechaInicio: undefined }), cupos: [] })
      expect(texto(f)).toContain('todavía no tiene cupos')
      expect(texto(f)).toContain('Sin fecha')
    })

    it('error: un grupo inexistente (AP-CU64-01) no muestra el texto crudo', async () => {
      const f = await detalle(null, { codigo: 'AP-CU64-01' })
      expect(texto(f)).not.toContain('crudo del backend')
      expect(texto(f)).toContain('Volver a intentar')
    })
  })
})
