import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { axe } from 'vitest-axe';
import { beforeEach, describe, expect, it } from 'vitest';
import { GATEWAY } from '../../../nucleo/gateway';
import { erroresInterceptor } from '../../../nucleo/errores.interceptor';
import { DirectorioDeParticipantes } from './directorio-de-participantes';
import { FichaDeParticipante } from './ficha-de-participante';

// Datos sintéticos: ningún dato real de personas.
const USUARIO = '33333333-3333-4333-8333-333333333333';
const BUSQUEDA = 'http://gw/api/v1/identidad/participantes/busquedas';
const FICHA = `http://gw/api/v1/identidad/participantes/${USUARIO}/ficha`;
const FILA = {
  usuarioId: USUARIO,
  codigoPublico: 'AP-SINT-01',
  nombreAbreviado: 'Persona S.',
  estado: 'ACTIVO',
  nivelKyc: 'BASICO',
  fechaRegistro: '2026-01-10T12:00:00Z',
  tieneRestriccion: false,
  tieneBloqueo: false,
  tieneMedidaEnCurso: true,
};
const MOTIVO = 'Ticket SOP-0001 de prueba';

function escribir(raiz: HTMLElement, etiqueta: string, valor: string): void {
  const campo = [...raiz.querySelectorAll('label')].find((l) => l.textContent?.includes(etiqueta));
  const input = raiz.querySelector<HTMLInputElement>(`#${campo?.getAttribute('for')}`)!;
  input.value = valor;
  input.dispatchEvent(new Event('input'));
}

const enviar = (raiz: HTMLElement) =>
  raiz.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));

describe('CU-134 · Participantes', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('sin justificación no consulta y señala el campo', async () => {
    const f = TestBed.createComponent(DirectorioDeParticipantes);
    await f.whenStable();
    enviar(f.nativeElement);
    TestBed.tick();
    http.expectNone(BUSQUEDA);
    expect(f.nativeElement.textContent).toContain('al menos 10 caracteres');
  });

  it('con datos: pinta la fila con su marca y enlaza a la ficha; manda la justificación en el cuerpo', async () => {
    const f = TestBed.createComponent(DirectorioDeParticipantes);
    await f.whenStable();
    escribir(f.nativeElement, 'Por qué consultas', MOTIVO);
    enviar(f.nativeElement);
    TestBed.tick();
    const req = http.expectOne(BUSQUEDA);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.justificacion).toBe(MOTIVO);
    req.flush({ filas: [FILA] });
    await f.whenStable();
    expect(f.nativeElement.textContent).toContain('AP-SINT-01');
    expect(
      f.nativeElement.querySelector(`a[href="/cumplimiento/participantes/${USUARIO}"]`),
    ).not.toBeNull();
    expect(await axe(f.nativeElement)).toHaveNoViolations();
  });

  it('vacío: orienta en vez de una tabla muda', async () => {
    const f = TestBed.createComponent(DirectorioDeParticipantes);
    await f.whenStable();
    escribir(f.nativeElement, 'Por qué consultas', MOTIVO);
    enviar(f.nativeElement);
    TestBed.tick();
    http.expectOne(BUSQUEDA).flush({ filas: [] });
    await f.whenStable();
    expect(f.nativeElement.textContent).toContain('Ningún participante coincide');
  });

  it('error: no muestra el error crudo y ofrece reintentar', async () => {
    const f = TestBed.createComponent(DirectorioDeParticipantes);
    await f.whenStable();
    escribir(f.nativeElement, 'Por qué consultas', MOTIVO);
    enviar(f.nativeElement);
    TestBed.tick();
    http
      .expectOne(BUSQUEDA)
      .flush({ detail: 'SQLSTATE secreto' }, { status: 500, statusText: 'Error' });
    await f.whenStable();
    expect(f.nativeElement.textContent).not.toContain('SQLSTATE');
    expect(
      [...f.nativeElement.querySelectorAll('button')].some((b: HTMLButtonElement) =>
        /volver a intentar/i.test(b.textContent ?? ''),
      ),
    ).toBe(true);
  });

  it('ficha: pide la justificación antes de abrir y luego muestra contacto enmascarado y la línea de tiempo', async () => {
    const f = TestBed.createComponent(FichaDeParticipante);
    f.componentRef.setInput('usuarioId', USUARIO);
    TestBed.tick();
    http.expectNone(FICHA);
    escribir(f.nativeElement, 'Por qué consultas', MOTIVO);
    enviar(f.nativeElement);
    TestBed.tick();
    const req = http.expectOne(FICHA);
    expect(req.request.body).toEqual({ justificacion: MOTIVO });
    req.flush({
      ...FILA,
      nombreCompleto: 'Persona Sintética',
      telefonoEnmascarado: '+591 7•••••12',
      correoEnmascarado: 'p•••@ejemplo.test',
      medidas: [
        {
          expedienteId: 'e1',
          usuarioId: USUARIO,
          causal: 'CUENTA_COMPROMETIDA',
          medida: 'SUSPENSION_TEMPORAL',
          cautelar: true,
          estado: 'EN_DESCARGO',
          estadoCuenta: 'SUSPENDIDA',
          propuestaEn: '2026-11-01T10:00:00Z',
          fechaLimiteDescargo: '2026-11-08T23:59:59Z',
          prescribeEn: '2027-11-01T10:00:00Z',
          lineaDeTiempo: [{ estado: 'PROPUESTA', ocurridoEn: '2026-11-01T10:00:00Z' }],
        },
      ],
    });
    await f.whenStable();
    const texto = f.nativeElement.textContent;
    expect(texto).toContain('+591 7•••••12');
    expect(texto).toContain('Cuenta comprometida');
    expect(texto).toContain('Descargo hasta');
    expect(await axe(f.nativeElement)).toHaveNoViolations();
  });

  it('ficha inexistente (404): dice que no la encontramos', async () => {
    const f = TestBed.createComponent(FichaDeParticipante);
    f.componentRef.setInput('usuarioId', USUARIO);
    await f.whenStable();
    escribir(f.nativeElement, 'Por qué consultas', MOTIVO);
    enviar(f.nativeElement);
    TestBed.tick();
    http
      .expectOne(FICHA)
      .flush({ codigo: 'AP-CU134-01' }, { status: 404, statusText: 'Not Found' });
    await f.whenStable();
    expect(f.nativeElement.textContent).not.toContain('AP-CU134-01');
  });
});
