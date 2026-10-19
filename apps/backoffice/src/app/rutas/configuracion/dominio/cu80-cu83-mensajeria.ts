import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { EnvioEnLista } from 'clientes/angular/notificaciones/model/envioEnLista'
import type { PlantillaEnLista } from 'clientes/angular/notificaciones/model/plantillaEnLista'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { CURSOR_DESCENDENTE, cargadorDeListado } from './listado'

export type { EnvioEnLista, PlantillaEnLista }

/**
 * CU-80 · `listarPlantillas` (`notificaciones.yaml`, `GET /notificaciones/plantillas`): una fila por versión, SIN asunto,
 * cuerpo ni botones. CU-83 · `listarEnvios` (`GET /notificaciones/envios`): auditoría de entrega SIN destinatario,
 * contenido ni identificador del proveedor (regla 90.2). Ambas exigen AUDITORIA_LEER y paginan (`antesDe`, `antesDeId`).
 */
export const cargarPlantillas = (http: HttpClient, gateway: string): CargadorDePagina<PlantillaEnLista> =>
  cargadorDeListado<PlantillaEnLista>(http, `${gateway}/notificaciones/plantillas`, CURSOR_DESCENDENTE)

export const cargarEnvios = (http: HttpClient, gateway: string): CargadorDePagina<EnvioEnLista> =>
  cargadorDeListado<EnvioEnLista>(http, `${gateway}/notificaciones/envios`, CURSOR_DESCENDENTE)

export const cargadorDePlantillas = (): CargadorDePagina<PlantillaEnLista> => cargarPlantillas(inject(HttpClient), inject(GATEWAY))
export const cargadorDeEnvios = (): CargadorDePagina<EnvioEnLista> => cargarEnvios(inject(HttpClient), inject(GATEWAY))
