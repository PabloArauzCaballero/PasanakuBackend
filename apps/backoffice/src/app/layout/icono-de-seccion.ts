import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

/** Trazos de 24×24, uno por sección del menú. Mismo grosor y remate que `ap-icono`. */
/** Trazos de 24×24, uno por sección y sub-opción del menú. Mismo grosor y remate que `ap-icono`. */
const TRAZOS = {
  // Secciones principales y utilidades
  tablero: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z',
  operacion: 'M3 12h4l3-8 4 16 3-8h4',
  cumplimiento: 'M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6zM9 12l2 2 4-4',
  admisiones: 'M15 19c0-3-2.7-5-6-5s-6 2-6 5M9 11a4 4 0 100-8 4 4 0 000 8zM19 8v6M16 11h6',
  sistemas: 'M4 4h16v6H4zM4 14h16v6H4zM8 7h.01M8 17h.01',
  contabilidad: 'M5 4h12a2 2 0 012 2v14H7a2 2 0 01-2-2zM5 18a2 2 0 012-2h12M9 8h6M9 11h4',
  publicidad: 'M4 10v4h3l6 4V6L7 10zM16 9a4 4 0 010 6M19 6a8 8 0 010 12',
  configuracion: 'M4 7h9M17 7h3M4 17h3M11 17h9M15 5v4M9 15v4',
  catalogo: 'M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3',
  ayuda: 'M12 21a9 9 0 100-18 9 9 0 000 18zM9.5 9.5a2.5 2.5 0 014.6 1.3c0 1.7-2.1 2-2.1 3.7M12 17.5h.01',
  salir: 'M15 4h3a2 2 0 012 2v12a2 2 0 01-2 2h-3M10 16l-4-4 4-4M6 12h10',

  // Operación: subrutas
  estado: 'M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5',
  retiros: 'M12 19V5M5 12l7-7 7 7M4 21h16',
  reclamos: 'M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2zM12 8v3M12 14h.01',
  solicitudesEscaladas: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM14 2v6h6M12 17v-6M9 14l3-3 3 3',
  custodia: 'M5 4h14a2 2 0 012 2v12a2 2 0 01-2 2H5a2 2 0 01-2-2V6a2 2 0 012-2zm7 5a3 3 0 100 6 3 3 0 000-6zm0 2a1 1 0 110 2 1 1 0 010-2z',
  cierresDiarios: 'M19 4H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2V6a2 2 0 00-2-2zM16 2v4M8 2v4M3 10h18M9 16l2 2 4-4',
  reversos: 'M3 12a9 9 0 102.6-6.4L2 9M2 4v5h5',
  desembolsos: 'M20 7H4a2 2 0 00-2 2v10a2 2 0 002 2h16a2 2 0 002-2V9a2 2 0 00-2-2zM16 7V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v2M12 12v4M9 14h6',

  // Cumplimiento: subrutas
  verificaciones: 'M3 5h18a2 2 0 012 2v10a2 2 0 01-2 2H3a2 2 0 01-2-2V7a2 2 0 012-2zM7 11a2 2 0 100-4 2 2 0 000 4zm7-2h4M13 13h5',
  alertas: 'M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0',
  casos: 'M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2zM11 14a2.5 2.5 0 100-5 2.5 2.5 0 000 5zm2 2l2 2',
  uifReportes: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM14 2v6h6M8 13h8M8 17h5',
  uifRequerimientos: 'M14 13l-4 4M8 7l4 4-2 2-4-4zM15 14l2 2-3 3-2-2zM2 20h8',
  continuidad: 'M21.5 2v6h-6M21.34 15.57a10 10 0 11-.57-8.38l6.67-1.19',
  actas: 'M16 4h2a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2h2M9 2h6a1 1 0 011 1v2a1 1 0 01-1 1H9a1 1 0 01-1-1V3a1 1 0 011-1zm0 10l2 2 4-4',
  riesgosEventos: 'M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0zM12 9v4M12 17h.01',
  seguridadIncidentes: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10zM12 8v4M12 16h.01',

  // Sistemas: subrutas
  servicios: 'M18 10h-1.26A8 8 0 109 20h9a5 5 0 000-10z',
  despliegues: 'M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 00-2.91-.09zM12 15l-3-3a22 22 0 012-3.95A12.88 12.88 0 0122 2c0 2.72-.78 7.5-6.05 11z',
  baseDeDatos: 'M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3',
  respaldos: 'M12 15V3m0 12l-4-4m4 4l4-4M2 17l.62 2.48A2 2 0 004.56 21h14.88a2 2 0 001.94-1.52L22 17',
  proveedores: 'M12 2v6M8 2v6M16 2v6M6 8h12a2 2 0 012 2v3a6 6 0 01-6 6v3h-4v-3a6 6 0 01-6-6v-3a2 2 0 012-2z',
  outbox: 'M22 12h-6l-2 3h-4l-2-3H2v7a2 2 0 002 2h16a2 2 0 002-2v-7zM5 12l3-6h8l3 6',
  webhooks: 'M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71',
  accesos: 'M21 2l-2 2m-1.5 1.5L14 9l-3-3 2-2-4-4-6 6a5.66 5.66 0 000 8l4 4a5.66 5.66 0 008 0l6-6z',
  incidentes: 'M8.5 14.5A2.5 2.5 0 0011 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 11-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 002.5 2.5z',
  usuariosInternos: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zm14 10v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75',
  rolesInternos: 'M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z',
  permisosInternos: 'M12 15a3 3 0 100-6 3 3 0 000 6zm0 0v3M12 3a9 9 0 100 18 9 9 0 000-18z',
  bitacora: 'M4 17l6-6-6-6M12 19h8M2 3h20a1 1 0 011 1v16a1 1 0 01-1 1H2a1 1 0 01-1-1V4a1 1 0 011-1z',
  catalogosOperativos: 'M4 19.5A2.5 2.5 0 016.5 17H20M4 4.5A2.5 2.5 0 016.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15z',
  operaciones: 'M22 12h-4l-3 9L9 3l-3 9H2',
  flujos: 'M6 3v12M18 9a3 3 0 100-6 3 3 0 000 6zM6 21a3 3 0 100-6 3 3 0 000 6zm0-6a9 9 0 019-9h3',

  // Finanzas: subrutas
  periodos: 'M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 012 2v14a2 2 0 01-2 2H5a2 2 0 01-2-2V6a2 2 0 012-2zM12 14v4M10 16h4',
  presupuesto: 'M21.21 15.89A10 10 0 118 2.83M22 12A10 10 0 0012 2v10z',
  compras: 'M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4zM3 6h18M16 10a4 4 0 01-8 0',
  cobros: 'M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6',
  anunciantes: 'M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2zM9 22V12h6v10',
  campanas: 'M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1zM4 22v-7',
  moderacion: 'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8zM12 9a3 3 0 100 6 3 3 0 000-6zM15 15l2 2',
  desempeno: 'M23 6l-9.5 9.5-5-5L1 18M17 6h6v6',
  liquidacion: 'M12 3v18M3 8l9-5 9 5M6 13l-3 6h6l-3-6zm12 0l-3 6h6l-3-6z',

  // Plataforma: subrutas
  tarifario: 'M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82zM7 7h.01',
  mensajeria: 'M21 11.5a8.38 8.38 0 01-.9 3.8 8.5 8.5 0 01-7.6 4.7 8.38 8.38 0 01-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 01-.9-3.8 8.5 8.5 0 014.7-7.6 8.38 8.38 0 013.8-.9h.5a8.48 8.48 0 018 8v.5z',
  reportes: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM14 2v6h6M8 17v-3M12 17v-6M16 17v-1',
} as const

export type Seccion = keyof typeof TRAZOS

/** El ícono de una sección o pantalla del menú. Siempre decorativo: el texto va al lado. */
@Component({
  selector: 'ap-icono-de-seccion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true' },
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="trazo()" /></svg>`,
  styles: `
    :host { display: inline-flex; width: 1.25rem; height: 1.25rem; flex: none; vertical-align: middle; }
    svg { width: 100%; height: 100%; }
  `,
})
export class IconoDeSeccion {
  readonly nombre = input.required<Seccion>()
  protected readonly trazo = computed(() => TRAZOS[this.nombre()] ?? TRAZOS.tablero)
}
