import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { RolCatalogo } from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { GW_DE_PRUEBA, probarListadoHttp } from '../compartido/pruebas-http.apoyo'
import { estabilizar, pulsar, texto } from '../compartido/pruebas.apoyo'
import { PantallaRolesInternos } from './pantalla-roles-internos'

const ROLES_EJEMPLO: RolCatalogo[] = [
  {
    id: 'r-1',
    codigo: 'RISK_ANALYST',
    nombre: 'Analista de Riesgo',
    ambito: 'PLATAFORMA',
    departamento: 'RIESGOS',
    descripcion: 'Monitoreo de score y alertas de morosidad',
    esSistema: true,
    permisos: ['ALERTAS_RIESGO_LEER', 'SCORE_CONSULTAR'],
  },
  {
    id: 'r-2',
    codigo: 'OPERATIONS_MANAGER',
    nombre: 'Gerente de Operaciones',
    ambito: 'PLATAFORMA',
    departamento: 'OPERACIONES',
    descripcion: 'Supervisión de conciliación y cierres diarios',
    esSistema: true,
    permisos: ['CIERRES_DIARIOS_GESTIONAR'],
  },
]

describe('PantallaRolesInternos', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('renderiza la lista de roles por departamento', async () => {
    const fixture = TestBed.createComponent(PantallaRolesInternos)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/roles').flush(ROLES_EJEMPLO)
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('RISK_ANALYST')
    expect(texto).toContain('Analista de Riesgo')
    expect(texto).toContain('OPERATIONS_MANAGER')
    expect(texto).toContain('Gerente de Operaciones')
  })
})

describe('PantallaRolesInternos · listado del kit en los tres niveles', () => {
  probarListadoHttp<RolCatalogo>({
    componente: PantallaRolesInternos,
    url: `${GW_DE_PRUEBA}/roles`,
    filas: (n) => Array.from({ length: n }, (_, i) => ({ id: `r-${i + 1}`, codigo: i === 3 ? 'TREASURY_LEAD' : `ROL_${i + 1}`, nombre: `Rol ${i + 1}`, ambito: 'INTERNO', departamento: i % 5 === 0 ? 'AUDITORIA' : 'OPERACIONES', descripcion: 'Rol sintético', esSistema: i % 2 === 0, permisos: ['ALERTAS_RIESGO_LEER'] })),
    textoVacio: 'No hay roles internos configurados.',
    busqueda: { termino: 'treasury', esperadas: 1 },
    filtro: { etiqueta: 'Departamento', valor: 'AUDITORIA', esperadas: 5 },
  })

  it('Ver permisos abre el cajón con los permisos del rol; no hay Editar ni Eliminar (el contrato no los tiene)', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW_DE_PRUEBA }] })
    const http = TestBed.inject(HttpTestingController)
    const f = TestBed.createComponent(PantallaRolesInternos)
    f.detectChanges()
    http.expectOne(`${GW_DE_PRUEBA}/roles`).flush(ROLES_EJEMPLO)
    const el = await estabilizar(f)
    expect(Array.from(el.querySelectorAll('tbody button')).map((b) => texto(b))).toEqual(['Ver permisos', 'Ver permisos'])
    pulsar(el.querySelector('tbody tr')!, 'Ver permisos')
    await estabilizar(f)
    expect(texto(el.querySelector('ap-cajon-lateral'))).toContain(ROLES_EJEMPLO[0]!.permisos[0]!)
  })
})
