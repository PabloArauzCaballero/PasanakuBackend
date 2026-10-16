import { Routes } from '@angular/router'
import { requierePermiso } from '../../nucleo/permisos'
import { requiereAlguno } from './compartido/acceso'

/**
 * Las rutas del dominio cumplimiento. **Este archivo lo posee el carril B (lote L2)**; el shell
 * solo lo carga por `loadChildren`. Cada pantalla tiene su propio id en la URL, para poder
 * pegar el enlace en un expediente (mismo patrón que `operacion`).
 *
 * Cada bandeja lleva en `canMatch` los MISMOS códigos que exige su operación en
 * `cumplimiento.yaml` (y `GuardiaDePermiso` del servidor: un código alcanza si es el rol o un
 * permiso de la sesión). El rol oculta, no protege: el servidor vuelve a decidir.
 *
 * `alertas/:alertaId` (CU-97), `actas/:actaId` (detalle de CU-94) y `reclamos/:reclamoId`
 * muestran indisponibilidad honesta: el contrato no publica esas consultas (o, en reclamos,
 * la atención vive en `operacion/reclamos`), y antes dibujaban datos de ejemplo.
 */
export const rutasCumplimiento: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'verificaciones' },
  {
    path: 'verificaciones',
    loadComponent: () => import('./verificaciones/pantalla-de-expedientes').then((m) => m.PantallaDeExpedientes),
    title: 'Verificación de identidad · AportaYa',
  },
  {
    path: 'alertas',
    canMatch: [requierePermiso('CUMPLIMIENTO_ALERTAS')],
    loadComponent: () => import('./alertas/bandeja-de-alertas').then((m) => m.BandejaDeAlertas),
    title: 'Alertas de monitoreo · AportaYa',
  },
  {
    path: 'alertas/:alertaId',
    loadComponent: () => import('./alertas/contenedor-de-alerta').then((m) => m.ContenedorDeAlerta),
    title: 'Alertas de riesgo temprano · AportaYa',
  },
  {
    path: 'casos',
    canMatch: [requierePermiso('CUMPLIMIENTO_CASOS')],
    loadComponent: () => import('./casos/bandeja-de-casos').then((m) => m.BandejaDeCasos),
    title: 'Casos de investigación · AportaYa',
  },
  {
    path: 'casos/:casoId',
    loadComponent: () => import('./casos/pantalla-de-caso').then((m) => m.PantallaDeCaso),
    title: 'Caso de cumplimiento · AportaYa',
  },
  {
    path: 'uif/reportes',
    canMatch: [requiereAlguno('CUMPLIMIENTO_REPORTAR', 'AUDITORIA_LEER')],
    loadComponent: () => import('./uif/bandeja-de-reportes').then((m) => m.BandejaDeReportes),
    title: 'Reportes a la UIF · AportaYa',
  },
  {
    path: 'uif/requerimientos',
    canMatch: [requiereAlguno('OFICIAL_CUMPLIMIENTO', 'AUDITORIA_LEER')],
    loadComponent: () => import('./uif/bandeja-de-requerimientos').then((m) => m.BandejaDeRequerimientos),
    title: 'Requerimientos de autoridad · AportaYa',
  },
  {
    path: 'continuidad',
    canMatch: [requiereAlguno('RESPONSABLE_RIESGOS', 'AUDITORIA_LEER')],
    loadComponent: () => import('./colas/pantalla-de-continuidad').then((m) => m.PantallaDeContinuidad),
    title: 'Pruebas de continuidad · AportaYa',
  },
  {
    path: 'actas',
    canMatch: [requiereAlguno('OFICIAL_CUMPLIMIENTO', 'AUDITORIA_LEER')],
    loadComponent: () => import('./colas/pantalla-de-actas').then((m) => m.PantallaDeActas),
    title: 'Actas de comité · AportaYa',
  },
  {
    path: 'actas/:actaId',
    loadComponent: () => import('./actas/contenedor-de-acta').then((m) => m.ContenedorDeActa),
    title: 'Acta de comité · AportaYa',
  },
  {
    path: 'riesgos/eventos',
    canMatch: [requiereAlguno('RESPONSABLE_RIESGOS', 'AUDITORIA_LEER')],
    loadComponent: () => import('./colas/pantalla-de-eventos').then((m) => m.PantallaDeEventos),
    title: 'Eventos de riesgo operativo · AportaYa',
  },
  {
    path: 'seguridad/incidentes',
    canMatch: [requiereAlguno('RESPONSABLE_SEGURIDAD', 'AUDITORIA_LEER')],
    loadComponent: () => import('./colas/pantalla-de-incidentes').then((m) => m.PantallaDeIncidentes),
    title: 'Incidentes de seguridad · AportaYa',
  },
  {
    path: 'reclamos/:reclamoId',
    loadComponent: () => import('./reclamos/contenedor-de-reclamo').then((m) => m.ContenedorDeReclamo),
    title: 'Reclamo del consumidor · AportaYa',
  },
  {
    path: 'organizadores/:organizadorId/habilitacion',
    loadComponent: () => import('./organizadores/pantalla-de-habilitacion').then((m) => m.PantallaDeHabilitacionOrganizador),
    title: 'Habilitación de organizador · AportaYa',
  },
]
