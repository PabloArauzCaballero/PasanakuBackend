import type { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { Sesion } from '../../../nucleo/sesion';
import { estabilizar, pulsar, texto } from '../../sistemas/compartido/pruebas.apoyo';
import {
  EVIDENCIA_DE_PRUEBA,
  escribirCodigoDePrueba,
  verificarPasoDeSeguridad,
} from '../../sistemas/usuarios-internos/paso-de-seguridad.pruebas';
import { configurarPruebas, GW, USUARIO, dialogo, escribir } from './acciones.pruebas';
import { ProponerMedida } from './proponer-medida';
import { RevelarDato } from './revelar-dato';

describe('CU-134 · revelar un dato y proponer una medida', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    http = configurarPruebas();
  });
  afterEach(() => http.verify());

  it('revelar: con DATOS_SENSIBLES_LEER pide el dato con justificación y evidencia y lo muestra', async () => {
    TestBed.inject(Sesion).abrir('t', ['DATOS_SENSIBLES_LEER'], 'r');
    const f = TestBed.createComponent(RevelarDato);
    f.componentRef.setInput('usuarioId', USUARIO);
    const el = await estabilizar(f);
    expect(el.textContent).not.toContain('Revelar correo');
    pulsar(el, 'Revelar teléfono');
    await estabilizar(f);
    const d = dialogo(el);
    escribir(d, 'Por qué necesitás verlo', 'Ticket SOP-0001 de prueba');
    escribirCodigoDePrueba(d);
    await estabilizar(f);
    pulsar(d, 'Revelar');
    verificarPasoDeSeguridad(http, GW);
    const req = http.expectOne(`${GW}/identidad/participantes/${USUARIO}/revelaciones`);
    expect(req.request.body).toEqual({
      campo: 'TELEFONO',
      justificacion: 'Ticket SOP-0001 de prueba',
      evidenciaMfa: EVIDENCIA_DE_PRUEBA,
    });
    req.flush({ campo: 'TELEFONO', valor: '+59170000001' });
    await estabilizar(f);
    expect(texto(el.querySelector('ap-alerta'))).toContain('+59170000001');
  });

  it('revelar: sin DATOS_SENSIBLES_LEER no hay botón', async () => {
    TestBed.inject(Sesion).abrir('t', ['PARTICIPANTES_LEER'], 'r');
    const f = TestBed.createComponent(RevelarDato);
    f.componentRef.setInput('usuarioId', USUARIO);
    expect((await estabilizar(f)).querySelectorAll('ap-boton').length).toBe(0);
  });

  it('proponer: valida antes de enviar, manda el cuerpo del contrato y conserva lo escrito ante un rechazo de causal', async () => {
    const f = TestBed.createComponent(ProponerMedida);
    f.componentRef.setInput('usuarioId', USUARIO);
    f.componentRef.setInput('abierto', true);
    const el = await estabilizar(f);
    const d = dialogo(el);
    pulsar(d, 'Proponer');
    await estabilizar(f);
    expect(texto(d)).toContain('Elegí la causal');
    expect(texto(d)).toContain('al menos 30 caracteres');
    const radio = Array.from(d.querySelectorAll<HTMLInputElement>('input[type="radio"]')).find(
      (r) => r.value === 'FRAUDE_CON_EVIDENCIA',
    )!;
    radio.click();
    escribir(d, 'Motivo', 'Movimientos inusuales confirmados por el equipo de riesgo.');
    escribir(d, 'Evidencia', 'Ticket RIE-0001 sintético');
    await estabilizar(f);
    pulsar(d, 'Proponer');
    const req = http.expectOne(`${GW}/identidad/participantes/${USUARIO}/medidas`);
    expect(req.request.body).toEqual({
      causal: 'FRAUDE_CON_EVIDENCIA',
      medida: 'SUSPENSION_TEMPORAL',
      cautelar: false,
      motivo: 'Movimientos inusuales confirmados por el equipo de riesgo.',
      evidencias: [{ descripcion: 'Ticket RIE-0001 sintético' }],
    });
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    req.flush(
      { codigo: 'AP-CU134-04', mensaje: 'x' },
      { status: 422, statusText: 'Unprocessable' },
    );
    await estabilizar(f);
    expect(dialogo(el)).not.toBeNull();
    expect((d.querySelector('textarea') as HTMLTextAreaElement).value).toBe(
      'Movimientos inusuales confirmados por el equipo de riesgo.',
    );
    expect(Array.from(d.querySelectorAll('[role="alert"]')).map(texto).join(' ')).toContain(
      'Esa causal no está en el catálogo',
    );
  });
});
