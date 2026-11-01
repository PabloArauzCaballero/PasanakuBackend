import { Routes } from '@angular/router'
import { aterrizaje } from '../../nucleo/permisos'
import { requiereAlguno } from './compartido/acceso'
import { ENTRADAS_DE_CUMPLIMIENTO } from './compartido/navegacion'

/** La guardia de una bandeja: los códigos de su entrada en `ENTRADAS_DE_CUMPLIMIENTO`, la misma lista que usa el menú. */
const guardiaDe = (ruta: string) => requiereAlguno(...(ENTRADAS_DE_CUMPLIMIENTO.find((e) => e.ruta === `/cumplimiento/${ruta}`)?.codigos ?? ['__sin_entrada__']))

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
 * Desde 2026-10-26 los códigos de cada bandeja salen de `ENTRADAS_DE_CUMPLIMIENTO` (`compartido/navegacion.ts`), la
 * misma lista con la que el shell dibuja las pestañas: la pestaña se ve si y solo si la guardia deja entrar
 * (`nucleo/menu-y-guardias.spec.ts`). `verificaciones` pasó a exigir `VERIFICACION_RESOLVER` (antes bastaba el permiso
 * de sección): es la cola de decisión humana de identidad. La raíz aterriza en la primera bandeja que la sesión abre.
 *
 * muestran indisponibilidad honesta: el contrato no publica esas consultas (o, en reclamos,
 * la atención vive en `operacion/reclamos`), y antes dibujaban datos de ejemplo.
 */
export const rutasCumplimiento: Routes = [
  { path: '', pathMatch: 'full', redirectTo: aterrizaje('cumplimiento', 'verificaciones') },
  {
    path: 'verificaciones',
    // `full`: si no, su `canMatch` corre por prefijo sobre `verificaciones/:id` y la ficha caería en la bandeja.
    pathMatch: 'full',
    canMatch: [guardiaDe('verificaciones')],
    loadComponent: () => import('./verificaciones/pantalla-de-expedientes').then((m) => m.PantallaDeExpedientes),
    title: 'Verificación de identidad · AportaYa',
  },
  {
    path: 'verificaciones/:verificacionId',
    canMatch: [guardiaDe('verificaciones')],
    loadComponent: () => import('./verificaciones/ficha-de-expediente').then((m) => m.FichaDeExpediente),
    title: 'Expediente de identidad · AportaYa',
  },
  {
    path: 'alertas',
    // `full`: si no, su `canMatch` corre por prefijo sobre `alertas/:id` y redirige antes de probar la ruta del detalle.
    pathMatch: 'full',
    canMatch: [guardiaDe('alertas')],
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
    // `full`: si no, su `canMatch` corre por prefijo sobre `casos/:id` y redirige antes de probar la ruta del detalle.
    pathMatch: 'full',
    canMatch: [guardiaDe('casos')],
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
    canMatch: [guardiaDe('uif/reportes')],
    loadComponent: () => import('./uif/bandeja-de-reportes').then((m) => m.BandejaDeReportes),
    title: 'Reportes a la UIF · AportaYa',
  },
  {
    path: 'uif/requerimientos',
    canMatch: [guardiaDe('uif/requerimientos')],
    loadComponent: () => import('./uif/bandeja-de-requerimientos').then((m) => m.BandejaDeRequerimientos),
    title: 'Requerimientos de autoridad · AportaYa',
  },
  {
    path: 'continuidad',
    canMatch: [guardiaDe('continuidad')],
    loadComponent: () => import('./colas/pantalla-de-continuidad').then((m) => m.PantallaDeContinuidad),
    title: 'Pruebas de continuidad · AportaYa',
  },
  {
    path: 'actas',
    // `full`: si no, su `canMatch` corre por prefijo sobre `actas/:id` y redirige antes de probar la ruta del detalle.
    pathMatch: 'full',
    canMatch: [guardiaDe('actas')],
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
    canMatch: [guardiaDe('riesgos/eventos')],
    loadComponent: () => import('./colas/pantalla-de-eventos').then((m) => m.PantallaDeEventos),
    title: 'Eventos de riesgo operativo · AportaYa',
  },
  {
    path: 'seguridad/incidentes',
    canMatch: [guardiaDe('seguridad/incidentes')],
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
