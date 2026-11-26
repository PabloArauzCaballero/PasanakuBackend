import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';
import type { DatoRevelado } from 'clientes/angular/identidad/model/datoRevelado';
import type { EntradaMedidaDeCuenta } from 'clientes/angular/identidad/model/entradaMedidaDeCuenta';
import type { ExpedienteDeMedida } from 'clientes/angular/identidad/model/expedienteDeMedida';
import type { Observable } from 'rxjs';
import { GATEWAY } from '../../../nucleo/gateway';
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor';

export type CampoRevelable = 'TELEFONO' | 'CORREO';

/**
 * CU-134 · los actos sobre un participante y sus medidas (`identidad.yaml`). Revelar, decidir, resolver la apelación y
 * liberar la entrega llevan la evidencia del segundo factor (`evidenciaMfa`, R-SEG-12) que obtiene `DialogoDeActo`;
 * proponer no la lleva. Todos mandan `Idempotency-Key`: reintentar no duplica. Nada viaja en la URL salvo los ids.
 */
export function accionesDeParticipantes() {
  const http = inject(HttpClient);
  const gateway = inject(GATEWAY);
  const enviar = <T>(ruta: string, cuerpo: unknown): Observable<T> =>
    http.post<T>(`${gateway}${ruta}`, cuerpo, { context: contextoIdempotente() });
  const medida = (id: string) => `/identidad/medidas-cuenta/${encodeURIComponent(id)}`;
  return {
    revelar: (
      usuarioId: string,
      campo: CampoRevelable,
      justificacion: string,
      evidenciaMfa: string,
    ) =>
      enviar<DatoRevelado>(
        `/identidad/participantes/${encodeURIComponent(usuarioId)}/revelaciones`,
        {
          campo,
          justificacion,
          evidenciaMfa,
        },
      ),
    decidir: (
      expedienteId: string,
      resultado: 'CONFIRMADA' | 'DESESTIMADA',
      fundamento: string,
      evidenciaMfa: string,
    ) =>
      enviar<ExpedienteDeMedida>(`${medida(expedienteId)}/decision`, {
        resultado,
        fundamento,
        evidenciaMfa,
      }),
    resolver: (
      expedienteId: string,
      resultado: 'ACEPTADA' | 'RECHAZADA',
      resolucion: string,
      evidenciaMfa: string,
    ) =>
      enviar<ExpedienteDeMedida>(`${medida(expedienteId)}/apelacion/resolucion`, {
        resultado,
        resolucion,
        evidenciaMfa,
      }),
    liberar: (expedienteId: string, fundamento: string, evidenciaMfa: string) =>
      enviar<ExpedienteDeMedida>(`${medida(expedienteId)}/liberacion-de-entrega`, {
        fundamento,
        evidenciaMfa,
      }),
    proponer: (usuarioId: string, entrada: EntradaMedidaDeCuenta) =>
      enviar<ExpedienteDeMedida>(
        `/identidad/participantes/${encodeURIComponent(usuarioId)}/medidas`,
        entrada,
      ),
  };
}

/** Qué actos admite cada etapa del expediente; el servidor vuelve a validar cada uno (regla 95.6.1). */
export const puedeDecidir = (estado: string): boolean => estado === 'EN_DESCARGO';
export const puedeResolver = (estado: string): boolean => estado === 'EN_APELACION';
/** A-1: lo que llegó a firme retiene la entrega hasta la liberación humana, aunque ya esté cumplida. */
export const puedeLiberar = (estado: string): boolean =>
  estado === 'FIRME' || estado === 'CUMPLIDA';
