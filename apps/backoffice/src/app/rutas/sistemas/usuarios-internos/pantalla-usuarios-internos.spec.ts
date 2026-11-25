import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { UsuarioInternoResumen } from 'clientes/angular/identidad'
import { GATEWAY, GW } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { estabilizar, pulsar, texto } from '../compartido/pruebas.apoyo'
import { PantallaUsuariosInternos } from './pantalla-usuarios-internos'

const USUARIOS_EJEMPLO: UsuarioInternoResumen[] = [
  {
    id: 'u-1',
    usuarioId: 'usr-1',
    codigoPublico: 'USR-001',
    nombres: 'Carlos',
    apellidos: 'Mamani',
    correo: 'carlos.mamani@aportaya.bo',
    telefono: '+59171234567',
    departamento: 'RIESGOS',
    cargo: 'Analista de Riesgo',
    roles: ['RISK_ANALYST'],
    estado: 'ACTIVO',
    tieneSegundoFactor: true,
    debeCambiarContrasena: false,
  },
]

describe('PantallaUsuariosInternos', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('renderiza la lista de usuarios internos y sus datos clave', async () => {
    const fixture = TestBed.createComponent(PantallaUsuariosInternos)
    fixture.detectChanges()

    const req = http.expectOne(`${GW}/usuarios-internos?limite=50`)
    req.flush(USUARIOS_EJEMPLO)
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('carlos.mamani@aportaya.bo')
    expect(texto).toContain('Carlos')
    expect(texto).toContain('Mamani')
    expect(texto).toContain('RIESGOS')
    expect(texto).toContain('Analista de Riesgo')
    expect(texto).toContain('RISK_ANALYST')
  })

  it('muestra estado vacío si el backend devuelve lista vacía', async () => {
    const fixture = TestBed.createComponent(PantallaUsuariosInternos)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/usuarios-internos?limite=50').flush([])
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('No se encontraron usuarios internos registrados')
  })

  it('carga una página adicional con ambos componentes del cursor y conserva las filas previas', async () => {
    const f = TestBed.createComponent(PantallaUsuariosInternos)
    f.detectChanges()
    const creadoEn = '2026-11-25T14:59:59Z'
    const primero = USUARIOS_EJEMPLO[0]!
    http.expectOne(`${GW}/usuarios-internos?limite=50`).flush(Array.from({ length: 50 }, (_, i) => ({ ...primero, id: `personal-${i}`, creadoEn })))
    const vista = await estabilizar(f)
    pulsar(vista, 'Cargar más usuarios')
    const siguiente = http.expectOne((r) => r.url === `${GW}/usuarios-internos` && r.params.get('antesDe') === creadoEn && r.params.get('antesDeId') === 'personal-49' && r.params.get('limite') === '50')
    siguiente.flush([{ ...primero, id: 'personal-50', creadoEn }])
    await estabilizar(f)
    expect(texto(vista.querySelector('ap-cifras-de-sistemas'))).toContain('Usuarios internos 51')
    expect(texto(vista)).not.toContain('Cargar más usuarios')
  })

  it('distingue la baja laboral del bloqueo sin fecha, aun con acceso de solo lectura', async () => {
    TestBed.inject(Sesion).abrir('token', ['USUARIOS_INTERNOS_LEER'], 'OPERADOR')
    const f = TestBed.createComponent(PantallaUsuariosInternos)
    f.detectChanges()
    const primero = USUARIOS_EJEMPLO[0]!
    http.expectOne(`${GW}/usuarios-internos?limite=50`).flush([
      { ...primero, estado: 'BAJA_LABORAL' },
      { ...primero, id: 'u-2', bloqueadoHasta: null, motivoBloqueo: 'ORDEN_ADMIN' },
    ])
    const vista = await estabilizar(f)
    expect(texto(vista)).toContain('Baja laboral')
    expect(texto(vista)).toContain('Bloqueado sin fecha de fin')
    expect(vista.querySelector('details.mas')).toBeNull()
  })
})
