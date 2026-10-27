import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

export type NombreDeIcono = keyof typeof TRAZOS

/** Trazos de 24×24 (stroke), uno por nombre. El ícono dice **qué pasó**, nunca si sube o baja. */
const TRAZOS = {
  aporte: 'M12 3v12m0 0-4-4m4 4 4-4M4 21h16',
  entrega: 'M3 8h18v11H3zM3 12h18M8 8V5h8v3',
  recarga: 'M4 12a8 8 0 1 0 2.3-5.7M4 4v5h5',
  retiro: 'M12 21V9m0 0-4 4m4-4 4 4M4 3h16',
  mora: 'M12 8v5m0 3h.01M12 3 2 20h20z',
  comision: 'M7 7h10M7 12h10M7 17h6',
  reloj: 'M12 7v5l3 2M12 21a9 9 0 1 1 0-18 9 9 0 0 1 0 18z',
  verificado: 'M9 12l2 2 4-4M12 21a9 9 0 1 1 0-18 9 9 0 0 1 0 18z',
  alerta: 'M12 8v5m0 3h.01M12 21a9 9 0 1 1 0-18 9 9 0 0 1 0 18z',
  cerrar: 'M6 6l12 12M18 6 6 18',
  buscar: 'M21 21l-4.3-4.3M17 11a6 6 0 1 1-12 0 6 6 0 0 1 12 0z',
  ojo: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zm10 3a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
  ojoCerrado: 'M3 3l18 18M10 6a10 10 0 0 1 12 6 12 12 0 0 1-3 3.5M6.5 6.5A12 12 0 0 0 2 12s4 7 10 7a9 9 0 0 0 3.5-.7',
  copiar: 'M9 9h10v12H9zM5 15V3h10',
  qr: 'M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h2v2h-2zM18 14h2v2h-2zM14 18h2v2h-2zM18 18h2v2h-2z',
  chevronDerecha: 'M9 6l6 6-6 6',
  chevronIzquierda: 'M15 6l-6 6 6 6',
  chevronAbajo: 'M6 9l6 6 6-6',
  mas: 'M12 5v14M5 12h14',
  menos: 'M5 12h14',
  estrella: 'M12 3l2.8 5.9 6.4.8-4.7 4.4 1.2 6.4L12 17.4 6.3 20.5l1.2-6.4L2.8 9.7l6.4-.8z',
  campana: 'M6 16V11a6 6 0 0 1 12 0v5l2 2H4zM10 21h4',
  calendario: 'M4 6h16v14H4zM4 10h16M8 3v4M16 3v4',
  persona: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21a8 8 0 0 1 16 0',
  // Anatomía del backoffice (cabecera de página, nota de vista, estados, shell).
  bombilla: 'M9 18h6M10 21h4M8 13a5 5 0 1 1 8 0c-.8.8-1.3 1.6-1.5 2.5h-5C9.3 14.6 8.8 13.8 8 13z',
  libro: 'M4 5a2 2 0 0 1 2-2h14v16H6a2 2 0 0 0-2 2zM4 19V5M8 7h8M8 11h6',
  baseDeDatos: 'M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3',
  advertencia: 'M12 9v4m0 3h.01M10.3 4.5 2.6 18a2 2 0 0 0 1.7 3h15.4a2 2 0 0 0 1.7-3L13.7 4.5a2 2 0 0 0-3.4 0z',
  sinResultados: 'M17 11a6 6 0 1 1-12 0 6 6 0 0 1 12 0zM21 21l-4.3-4.3M9 9l4 4m0-4-4 4',
  prohibido: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM5.6 5.6l12.8 12.8',
  menu: 'M4 7h16M4 12h16M4 17h16',
  salir: 'M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 16l-4-4 4-4M6 12h10',
  flechaIzquierda: 'M19 12H5m0 0 6 6m-6-6 6-6',
  casa: 'M3 11l9-8 9 8M5 10v10h5v-6h4v6h5V10',
  grupo: 'M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM17 11a3 3 0 1 0 0-6M3 20a6 6 0 0 1 12 0M15 14a6 6 0 0 1 6 6',
  candado: 'M6 11h12v10H6zM8 11V7a4 4 0 0 1 8 0v4',
  filtro: 'M3 5h18l-7 8v6l-4 2v-8z',
  info: 'M12 11v5m0-8h.01M12 21a9 9 0 1 1 0-18 9 9 0 0 1 0 18z',
  // Catálogo de datos: las columnas de una tabla y la clave que une dos tablas.
  columnas: 'M4 4h16v16H4zM9.5 4v16M14.5 4v16',
  relacion: 'M9 15 15 9M10 6.5l1.5-1.5a4 4 0 0 1 5.7 5.7L15.7 12.2M14 17.5 12.5 19a4 4 0 0 1-5.7-5.7L8.3 11.8',
  // Acciones de listado y de fila (2026-10-26): ícono + nombre en cada botón.
  editar: 'M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z',
  eliminar: 'M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v6M14 11v6',
  descargar: 'M12 3v12m0 0-4-4m4 4 4-4M4 17v3h16v-3',
  subir: 'M12 15V3m0 0-4 4m4-4 4 4M4 17v3h16v-3',
  archivo: 'M14 3H6v18h12V7zM14 3v4h4',
  archivoCsv: 'M14 3H6v18h12V7zM14 3v4h4M9 12h6M9 16h6',
  archivoJson: 'M10 11c-1 0-1.5.5-1.5 1.5v.5c0 .7-.4 1-1 1 .6 0 1 .3 1 1v.5c0 1 .5 1.5 1.5 1.5M14 11c1 0 1.5.5 1.5 1.5v.5c0 .7.4 1 1 1-.6 0-1 .3-1 1v.5c0 1-.5 1.5-1.5 1.5M14 3H6v18h12V7zM14 3v4h4',
  guardar: 'M5 3h11l3 3v15H5zM8 3v5h7V3M8 21v-7h8v7',
  aprobar: 'M5 12l5 5L20 7',
  rechazar: 'M15 9l-6 6m0-6 6 6M12 21a9 9 0 1 1 0-18 9 9 0 0 1 0 18z',
  enviar: 'M22 2 11 13M22 2l-7 20-4-9-9-4z',
  imprimir: 'M6 9V3h12v6M6 18H4v-7h16v7h-2M6 14h12v7H6z',
  limpiar: 'M3 5h12l-5 6v6l-3 2v-8zM16 14l5 5m0-5-5 5',
  ordenar: 'M7 4v16m0 0-3-3m3 3 3-3M17 20V4m0 0-3 3m3-3 3 3',
  lista: 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01',
  tabla: 'M3 4h18v16H3zM3 10h18M3 15h18M9 4v16',
  billetera: 'M3 7h16a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM3 7l12-4v4M17 13h.01',
  banco: 'M3 10h18L12 4zM5 10v8M9 10v8M15 10v8M19 10v8M3 20h18',
  documento: 'M14 3H6v18h12V7zM14 3v4h4M9 13h6M9 17h4',
  carpeta: 'M3 6h6l2 2h10v11H3z',
  etiqueta: 'M3 12V3h9l9 9-9 9zM7.5 7.5h.01',
  llave: 'M14 10a4 4 0 1 0-1.2-2.8L3 17v4h4v-2h2v-2h2l1.8-1.8M16 7h.01',
  escudo: 'M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z',
  servidor: 'M4 4h16v6H4zM4 14h16v6H4zM8 7h.01M8 17h.01',
  nube: 'M7 18a4 4 0 0 1-.5-8A6 6 0 0 1 18 9a4.5 4.5 0 0 1-.5 9z',
  enlace: 'M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1',
  correo: 'M3 5h18v14H3zM3 6l9 7 9-7',
  telefono: 'M5 3h4l2 5-3 2a11 11 0 0 0 6 6l2-3 5 2v4a2 2 0 0 1-2 2A17 17 0 0 1 3 5a2 2 0 0 1 2-2z',
  mensaje: 'M4 4h16v12H8l-4 4z',
  grafico: 'M4 20V10M10 20V4M16 20v-7M2 20h20',
  tendencia: 'M3 17l6-6 4 4 8-8M15 7h6v6',
  moneda: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM15 9.5c-.5-1-1.6-1.5-3-1.5-1.7 0-3 .8-3 2s1.3 1.7 3 2 3 .8 3 2-1.3 2-3 2c-1.4 0-2.5-.5-3-1.5M12 6v2M12 16v2',
  recibo: 'M5 3h14v18l-3-2-2 2-2-2-2 2-2-2-3 2zM9 8h6M9 12h6M9 16h3',
  historial: 'M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5M12 7v5l3 2',
  personaMas: 'M10 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM2 21a8 8 0 0 1 13-6.2M19 14v6M16 17h6',
  engranaje: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1',
  deshacer: 'M9 14 4 9l5-5M4 9h11a5 5 0 0 1 0 10h-3',
  flechaDerecha: 'M5 12h14m0 0-6-6m6 6-6 6',
  externo: 'M14 4h6v6M20 4l-9 9M18 14v6H4V6h6',
  puntos: 'M5 12h.01M12 12h.01M19 12h.01',
  pausa: 'M8 5v14M16 5v14',
  reproducir: 'M7 4l13 8-13 8z',
  bandeja: 'M3 13h5l2 3h4l2-3h5M5 5h14l2 8v6H3v-6z',
  tarjeta: 'M3 6h18v12H3zM3 10h18M7 15h3',
  ubicacion: 'M12 21s-7-6-7-11a7 7 0 1 1 14 0c0 5-7 11-7 11zM12 12a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
  actividad: 'M3 12h4l3-8 4 16 3-8h4',
  bandera: 'M5 21V4h11l-2 4 2 4H5',
  rayo: 'M13 2 4 14h7l-1 8 9-12h-7z',
  imagen: 'M3 5h18v14H3zM3 16l5-5 4 4 3-3 6 6M15 9h.01',
  paquete: 'M12 3l8 4.5v9L12 21l-8-4.5v-9zM12 12l8-4.5M12 12v9M12 12 4 7.5',
  portapapeles: 'M9 4h6v3H9zM9 5H6v16h12V5h-3M9 12h6M9 16h4',
  megafono: 'M4 10v4h3l6 4V6L7 10zM16 9a4 4 0 0 1 0 6',
  balanza: 'M12 3v18M7 21h10M5 7h14M5 7l-3 7a3 3 0 0 0 6 0zM19 7l-3 7a3 3 0 0 0 6 0z',
  interruptor: 'M8 7h8a5 5 0 0 1 0 10H8A5 5 0 0 1 8 7zM16 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
} as const

/**
 * Un ícono de trazo que hereda el color del texto. Es decorativo por omisión
 * (`aria-hidden`); con `etiqueta` pasa a ser una imagen con nombre.
 */
@Component({
  selector: 'ap-icono',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.role]': 'etiqueta() ? "img" : null',
    '[attr.aria-label]': 'etiqueta() ?? null',
    '[attr.aria-hidden]': 'etiqueta() ? null : "true"',
    '[style.--tamano]': 'tamanoCss()',
  },
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="trazo()" /></svg>`,
  styles: `
    :host { display: inline-flex; width: var(--tamano); height: var(--tamano); flex: none; vertical-align: middle; }
    svg { width: 100%; height: 100%; }
  `,
})
export class Icono {
  readonly nombre = input.required<NombreDeIcono>()
  readonly etiqueta = input<string>()
  /** `s4` (16), `s5` (24) o `s6` (32): escalas del sistema, nunca un número. */
  readonly tamano = input<'s4' | 'S5' | 's5' | 's6'>('s5')
  readonly trazo = computed(() => TRAZOS[this.nombre()])
  readonly tamanoCss = computed(() => `var(--${this.tamano().toLowerCase()})`)
}
