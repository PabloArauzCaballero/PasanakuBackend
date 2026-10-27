import { provideZonelessChangeDetection, type InjectionToken, type Type } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { PantallaAccesos } from '../accesos/pantalla-accesos'
import { PantallaBaseDeDatos } from '../base-datos/pantalla-base-datos'
import { PantallaDespliegues } from '../despliegues/pantalla-despliegues'
import { PUERTO_ACCESOS, PUERTO_BASE_DE_DATOS, PUERTO_DESPLIEGUES, PUERTO_INCIDENTES, PUERTO_PROVEEDORES, PUERTO_WEBHOOKS } from '../dominio/puertos'
import { PantallaIncidentes } from '../incidentes/pantalla-incidentes'
import { PantallaProveedores } from '../proveedores/pantalla-proveedores'
import { PantallaWebhooks } from '../webhooks/pantalla-webhooks'
import { estabilizar } from './pruebas.apoyo'

/** Un doble del puerto por vista, con dos filas sintéticas: lo justo para que axe vea tabla, filtros y paginación. */
const CASOS: [string, Type<unknown>, InjectionToken<unknown>, unknown][] = [
  ['Base y migraciones', PantallaBaseDeDatos, PUERTO_BASE_DE_DATOS, [{ id: 'm1', nombre: 'V1__base', aplicadaEl: '2026-10-01', estado: 'aplicada' }, { id: 'm2', nombre: 'V2__cupos', aplicadaEl: null, estado: 'pendiente' }]],
  ['Proveedores', PantallaProveedores, PUERTO_PROVEEDORES, [{ id: 'p1', nombre: 'Nube', categoria: 'Infraestructura', costoRealUltimoPeriodo: '100.00', moneda: 'USD' }]],
  ['Webhooks', PantallaWebhooks, PUERTO_WEBHOOKS, [{ id: 'w1', direccion: 'entrante', origen: 'pasarela', ultimoEstado: 'error', ultimaEntregaEl: '2026-10-25' }]],
  ['Accesos', PantallaAccesos, PUERTO_ACCESOS, [{ id: 'a1', persona: 'Persona Prueba', rol: 'PLATAFORMA', ambito: 'sistemas', otorgadoEl: '2026-09-01' }]],
  ['Incidentes', PantallaIncidentes, PUERTO_INCIDENTES, [{ id: 'i1', titulo: 'Latencia alta', severidad: 'sev2', estado: 'abierto', abiertoEl: '2026-10-20' }]],
  ['Despliegues', PantallaDespliegues, PUERTO_DESPLIEGUES, { despliegues: [{ id: 'd1', servicio: 'aportes', version: '1.0.0', desplegadoEl: '2026-10-25', desplegadoPor: 'ci', estado: 'exitoso' }], interruptores: [{ id: 'x1', nombre: 'Retiros extraordinarios', tocaDinero: true, activo: false }] }],
]

describe('Vistas de puerto de sistemas · accesibilidad', () => {
  for (const [nombre, componente, puerto, datos] of CASOS) {
    it(`${nombre}: sin violaciones serias con datos`, async () => {
      TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), { provide: puerto, useValue: { obtener: () => Promise.resolve(datos) } }] })
      const el = await estabilizar(TestBed.createComponent(componente))
      expect(await axe(el)).toHaveNoViolations()
    })
  }
})
