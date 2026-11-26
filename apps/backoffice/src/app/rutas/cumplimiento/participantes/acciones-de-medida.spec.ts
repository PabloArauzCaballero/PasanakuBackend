import type { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import { Sesion } from '../../../nucleo/sesion';
import { estabilizar, pulsar, texto } from '../../sistemas/compartido/pruebas.apoyo';
import {
  EVIDENCIA_DE_PRUEBA,
  escribirCodigoDePrueba,
  verificarPasoDeSeguridad,
} from '../../sistemas/usuarios-internos/paso-de-seguridad.pruebas';
import {
  configurarPruebas,
  GW,
  EXP,
  FUNDAMENTO,
  dialogo,
  medida,
  escribir,
} from './acciones.pruebas';
import { AccionesDeMedida } from './acciones-de-medida';

describe('CU-134 · actos sobre una medida, con segundo factor', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    http = configurarPruebas();
  });
  afterEach(() => http.verify());

  async function acciones(estado: string, permisos: string[]) {
    TestBed.inject(Sesion).abrir('t', permisos, 'r');
    const f = TestBed.createComponent(AccionesDeMedida);
    f.componentRef.setInput('medida', medida(estado));
    let cambios = 0;
    f.componentInstance.cambio.subscribe(() => cambios++);
    return { f, el: await estabilizar(f), cambios: () => cambios };
  }

  it('sin el permiso de decidir no ofrece ningún acto, aunque la etapa lo admita', async () => {
    const { el } = await acciones('EN_DESCARGO', ['PARTICIPANTES_LEER']);
    expect(el.querySelectorAll('ap-boton').length).toBe(0);
  });

  it('confirmar: segundo factor, fundamento y la decisión con la evidencia; avisa el cambio', async () => {
    const { f, el, cambios } = await acciones('EN_DESCARGO', ['PARTICIPANTES_MEDIDA_DECIDIR']);
    expect(Array.from(el.querySelectorAll('ap-boton')).map(texto)).toEqual([
      'Confirmar medida',
      'Desestimar medida',
    ]);
    expect(await axe(el)).toHaveNoViolations();
    pulsar(el, 'Confirmar medida');
    await estabilizar(f);
    const d = dialogo(el);
    expect(texto(d)).toContain('corre el plazo de apelación');
    escribir(d, 'Fundamento de la decisión', FUNDAMENTO);
    escribirCodigoDePrueba(d);
    await estabilizar(f);
    pulsar(d, 'Confirmar medida');
    verificarPasoDeSeguridad(http, GW);
    const req = http.expectOne(`${GW}/identidad/medidas-cuenta/${EXP}/decision`);
    expect(req.request.body).toEqual({
      resultado: 'CONFIRMADA',
      fundamento: FUNDAMENTO,
      evidenciaMfa: EVIDENCIA_DE_PRUEBA,
    });
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    req.flush(medida('DECIDIDA'));
    await estabilizar(f);
    expect(cambios()).toBe(1);
    expect(dialogo(el)).toBeNull();
  });

  it('un fundamento corto no habilita confirmar y no se pide nada', async () => {
    const { f, el } = await acciones('EN_DESCARGO', ['PARTICIPANTES_MEDIDA_DECIDIR']);
    pulsar(el, 'Desestimar medida');
    await estabilizar(f);
    const d = dialogo(el);
    escribir(d, 'Fundamento de la decisión', 'corto');
    escribirCodigoDePrueba(d);
    await estabilizar(f);
    const confirmar = Array.from(d.querySelectorAll('button')).find(
      (b) => texto(b) === 'Desestimar medida',
    ) as HTMLButtonElement;
    expect(confirmar.disabled).toBe(true);
  });

  it('apelación: aceptar manda la resolución con la evidencia; un rechazo del servidor va al campo del código', async () => {
    const { f, el } = await acciones('EN_APELACION', ['PARTICIPANTES_APELACION_RESOLVER']);
    pulsar(el, 'Aceptar apelación');
    await estabilizar(f);
    const d = dialogo(el);
    escribir(d, 'Resolución', FUNDAMENTO);
    escribirCodigoDePrueba(d);
    await estabilizar(f);
    pulsar(d, 'Aceptar apelación');
    verificarPasoDeSeguridad(http, GW);
    const req = http.expectOne(`${GW}/identidad/medidas-cuenta/${EXP}/apelacion/resolucion`);
    expect(req.request.body).toEqual({
      resultado: 'ACEPTADA',
      resolucion: FUNDAMENTO,
      evidenciaMfa: EVIDENCIA_DE_PRUEBA,
    });
    req.flush(
      { codigo: 'AP-CU134-03', mensaje: 'x' },
      { status: 422, statusText: 'Unprocessable' },
    );
    await estabilizar(f);
    expect(dialogo(el)).not.toBeNull();
    expect(texto(d.querySelector('ap-campo-otp'))).toContain('segundo factor');
  });

  it('A-1: con la medida cumplida ofrece liberar la entrega y manda el fundamento', async () => {
    const { f, el } = await acciones('CUMPLIDA', ['PARTICIPANTES_MEDIDA_DECIDIR']);
    expect(Array.from(el.querySelectorAll('ap-boton')).map(texto)).toEqual(['Liberar entrega']);
    pulsar(el, 'Liberar entrega');
    await estabilizar(f);
    const d = dialogo(el);
    escribir(d, 'Fundamento de la liberación', 'Medida cumplida y sin deuda exigible');
    escribirCodigoDePrueba(d);
    await estabilizar(f);
    pulsar(d, 'Liberar entrega');
    verificarPasoDeSeguridad(http, GW);
    const req = http.expectOne(`${GW}/identidad/medidas-cuenta/${EXP}/liberacion-de-entrega`);
    expect(req.request.body).toEqual({
      fundamento: 'Medida cumplida y sin deuda exigible',
      evidenciaMfa: EVIDENCIA_DE_PRUEBA,
    });
    req.flush(medida('CUMPLIDA'));
  });
});
