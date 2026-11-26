import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import type { ExpedienteDeMedida } from 'clientes/angular/identidad/model/expedienteDeMedida';
import { erroresInterceptor } from '../../../nucleo/errores.interceptor';
import { GATEWAY } from '../../../nucleo/gateway';
import { idempotenciaInterceptor } from '../../../nucleo/idempotencia.interceptor';
import { texto } from '../../sistemas/compartido/pruebas.apoyo';

/** Apoyo de las pruebas de los actos de CU-134: mismos interceptores que la app (idempotencia y errores). */
// Datos sintéticos (regla 97.6): ningún dato real de persona.
export const GW = 'http://gw/api/v1';
export const USUARIO = '33333333-3333-4333-8333-333333333333';
export const EXP = 'e1';
export const FUNDAMENTO =
  'La evidencia del sistema y el descargo se ponderaron y sostienen la medida propuesta.';
export const dialogo = (el: HTMLElement) => el.querySelector('dialog[open]') as HTMLElement;
export const medida = (estado: string): ExpedienteDeMedida =>
  ({
    expedienteId: EXP,
    usuarioId: USUARIO,
    causal: 'CUENTA_COMPROMETIDA',
    medida: 'SUSPENSION_TEMPORAL',
    cautelar: true,
    estado,
    estadoCuenta: 'SUSPENDIDO',
    propuestaEn: '2026-11-01T10:00:00Z',
    prescribeEn: '2027-11-01T10:00:00Z',
    lineaDeTiempo: [],
  }) as unknown as ExpedienteDeMedida;

export function escribir(raiz: Element, etiqueta: string, valor: string): void {
  const label = Array.from(raiz.querySelectorAll('label')).find((l) =>
    texto(l).startsWith(etiqueta),
  );
  const control = (
    label?.htmlFor
      ? raiz.ownerDocument.getElementById(label.htmlFor)
      : label?.querySelector('input, textarea')
  ) as HTMLInputElement | HTMLTextAreaElement | null;
  if (!control) throw new Error(`No hay campo «${etiqueta}»`);
  control.value = valor;
  control.dispatchEvent(new Event('input'));
}

export function configurarPruebas(): HttpTestingController {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: GW },
    ],
  });
  return TestBed.inject(HttpTestingController);
}
