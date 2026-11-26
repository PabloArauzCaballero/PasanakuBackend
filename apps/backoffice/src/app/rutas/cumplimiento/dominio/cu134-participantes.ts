import { httpResource } from '@angular/common/http';
import { inject, Signal } from '@angular/core';
import type { EntradaBusquedaParticipantes } from 'clientes/angular/identidad/model/entradaBusquedaParticipantes';
import type { FichaDeParticipante } from 'clientes/angular/identidad/model/fichaDeParticipante';
import type { PaginaDeParticipantes } from 'clientes/angular/identidad/model/paginaDeParticipantes';
import { GATEWAY } from '../../../nucleo/gateway';

/**
 * CU-134 · directorio y ficha del participante (`identidad.yaml`, tag Participantes). Las dos lecturas son POST: el
 * término y la justificación viajan en el cuerpo, nunca en la URL (regla 90.2.2), y cada una deja su fila en
 * `registro_acceso_datos` del lado del servidor. Sin justificación no se pide nada: el recurso queda inactivo.
 */
export function directorioDe(consulta: Signal<EntradaBusquedaParticipantes | null>) {
  const gateway = inject(GATEWAY);
  return httpResource<PaginaDeParticipantes>(() => {
    const cuerpo = consulta();
    return cuerpo
      ? { url: `${gateway}/identidad/participantes/busquedas`, method: 'POST', body: cuerpo }
      : undefined;
  });
}

export function fichaDe(usuarioId: Signal<string>, justificacion: Signal<string | null>) {
  const gateway = inject(GATEWAY);
  return httpResource<FichaDeParticipante>(() => {
    const motivo = justificacion();
    return motivo
      ? {
          url: `${gateway}/identidad/participantes/${encodeURIComponent(usuarioId())}/ficha`,
          method: 'POST',
          body: { justificacion: motivo },
        }
      : undefined;
  });
}

/** La justificación mínima que exige el servidor (`ck_acceso_justificacion`, AP-CU134-01). */
export const justificacionValida = (texto: string): boolean => texto.trim().length >= 10;

export const directorioVacio = (p: PaginaDeParticipantes): boolean => p.filas.length === 0;
