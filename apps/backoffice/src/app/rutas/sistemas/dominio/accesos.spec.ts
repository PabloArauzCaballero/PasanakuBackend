import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import type { UsuarioInternoResumen } from 'clientes/angular/identidad'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { filasDeAccesos, puertoAccesos } from './accesos'
import { SIN_DATO } from './salud-de-servicios'

const GW = 'https://gateway.test/api/v1'

// Sintético: la forma de `UsuarioInternoResumen` (identidad.yaml); ninguna persona real.
const persona = (n: number, roles: string[], extra: Partial<UsuarioInternoResumen> = {}): UsuarioInternoResumen => ({
  id: `00000000-0000-4000-8000-00000000000${n}`,
  usuarioId: `10000000-0000-4000-8000-00000000000${n}`,
  nombres: `Persona${n}`,
  apellidos: 'Prueba',
  correo: `persona${n}@ejemplo.test`,
  departamento: 'Plataforma',
  cargo: 'Operador',
  estado: 'ACTIVO',
  debeCambiarContrasena: false,
  tieneSegundoFactor: true,
  roles,
  ...extra,
})

describe('accesos · Revisión de accesos contra GET /usuarios-internos', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }],
    })
  })

  it('correcto: una fila por persona y rol, con ámbito, estado y segundo factor; nunca el correo', () => {
    const filas = filasDeAccesos([persona(1, ['ADMIN_PLATAFORMA', 'RESPONSABLE_SEGURIDAD']), persona(2, ['TESORERIA'], { tieneSegundoFactor: false })])
    expect(filas.map((f) => [f.persona, f.rol, f.segundoFactor])).toEqual([
      ['Persona1 Prueba', 'ADMIN_PLATAFORMA', 'Sí'],
      ['Persona1 Prueba', 'RESPONSABLE_SEGURIDAD', 'Sí'],
      ['Persona2 Prueba', 'TESORERIA', 'No'],
    ])
    expect(filas[0]).toMatchObject({ ambito: 'Plataforma', estado: 'Activo', otorgadoEl: SIN_DATO })
    expect(new Set(filas.map((f) => f.id)).size).toBe(3)
    expect(JSON.stringify(filas)).not.toContain('@ejemplo.test')
  })

  it('límite: sin roles sale igual como «sin rol»; bloqueado temporalmente gana al estado; un estado nuevo se muestra tal cual', () => {
    const filas = filasDeAccesos([
      persona(1, []),
      persona(2, ['X'], { bloqueadoHasta: '2026-11-01T00:00:00Z' }),
      persona(3, ['X'], { estado: 'NUEVO_ESTADO', departamento: '' }),
    ])
    expect(filas.map((f) => [f.rol, f.estado, f.ambito])).toEqual([
      ['sin rol', 'Activo', 'Plataforma'],
      ['X', 'Bloqueado temporalmente', 'Plataforma'],
      ['X', 'NUEVO_ESTADO', SIN_DATO],
    ])
    expect(filasDeAccesos([])).toEqual([])
  })

  it('correcto: el puerto pide /usuarios-internos al gateway y devuelve las filas', async () => {
    const puerto = TestBed.runInInjectionContext(() => puertoAccesos())
    const promesa = puerto.obtener()
    TestBed.inject(HttpTestingController).expectOne(`${GW}/usuarios-internos`).flush([persona(1, ['ADMIN_PLATAFORMA'])])
    expect((await promesa).map((f) => f.rol)).toEqual(['ADMIN_PLATAFORMA'])
  })

  it('inválido: con 403 o 500 el puerto rechaza (la pantalla pinta el error), nunca devuelve filas inventadas', async () => {
    for (const status of [403, 500]) {
      const puerto = TestBed.runInInjectionContext(() => puertoAccesos())
      const promesa = puerto.obtener()
      TestBed.inject(HttpTestingController).expectOne(`${GW}/usuarios-internos`).flush({}, { status, statusText: 'x' })
      await expect(promesa).rejects.toBeTruthy()
    }
  })
})
