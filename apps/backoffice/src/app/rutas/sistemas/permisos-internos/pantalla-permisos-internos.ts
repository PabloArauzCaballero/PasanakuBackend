import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { PermisoCatalogo } from 'clientes/angular/identidad'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { catalogoPermisos } from '../dominio/usuarios-internos'
import { textosSistemas } from '../textos'

const RIESGOS = ['BAJO', 'MEDIO', 'ALTO', 'CRITICO'] as const
const TONO: Record<string, Tono> = { CRITICO: 'error', ALTO: 'aviso', MEDIO: 'neutro', BAJO: 'ok' }
const RIESGO: Record<string, string> = { BAJO: 'Bajo', MEDIO: 'Medio', ALTO: 'Alto', CRITICO: 'Crítico' }
const riesgoDe = (p: PermisoCatalogo) => RIESGO[p.nivelRiesgo] ?? p.nivelRiesgo
const motivoDe = (p: PermisoCatalogo) => (p.exigeMotivo ? 'Exige motivo' : 'Estándar')
const mfaDe = (p: PermisoCatalogo) => (p.requiereMfa ? 'Requiere MFA' : 'No')

/** Matriz de permisos (`GET /permisos`), de solo lectura en el contrato: sin acciones de fila. */
@Component({
  selector: 'ap-pantalla-permisos-internos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas · Identidad" [titulo]="t.titulo" [descripcion]="t.proposito">
        @if (carga().tipo !== 'sin-fuente') { <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="recurso.isLoading()" (pulsado)="recurso.reload()">Actualizar</ap-boton> }
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de permisos" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por código, recurso, acción o descripción del permiso." [exportables]="exportables" nombreDeRegistros="permisos" [textoVacio]="t.vacio" explicacionVacio="Los permisos los define el servicio de identidad; cuando existan, aparecen acá." (reintentar)="recurso.reload()">
        <ng-template #celda let-p let-c="columna">
          @switch (c.clave) {
            @case ('codigo') { <code>{{ p.codigo }}</code> }
            @case ('nivelRiesgo') { <ap-chip-estado [tono]="tono(p)">{{ riesgo(p) }}</ap-chip-estado> }
            @case ('exigeMotivo') { <ap-chip-estado [tono]="p.exigeMotivo ? 'aviso' : 'neutro'">{{ motivo(p) }}</ap-chip-estado> }
            @case ('requiereMfa') { <ap-chip-estado [tono]="p.requiereMfa ? 'aviso' : 'neutro'">{{ mfa(p) }}</ap-chip-estado> }
            @default { {{ p[c.clave] }} }
          }
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }
    code { padding: 0 var(--s1); border-radius: var(--r-sm); background: var(--surface-2); font: var(--t-ayuda); font-family: var(--f-mono); color: var(--text-2); overflow-wrap: anywhere; }
  `,
})
export class PantallaPermisosInternos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.permisosInternos
  protected readonly recurso = catalogoPermisos()
  protected readonly carga = cargaDe(this.recurso)
  private readonly filas = filasDe<PermisoCatalogo>(this.recurso)
  protected readonly motivo = motivoDe
  protected readonly mfa = mfaDe
  protected readonly listado = crearListado<PermisoCatalogo>({
    filas: this.filas,
    camposDeBusqueda: ['codigo', 'recurso', 'accion', 'descripcion'],
    filtros: [
      { nombre: 'riesgo', etiqueta: 'Riesgo', valor: (p) => p.nivelRiesgo, opciones: RIESGOS.map((r) => ({ valor: r, texto: RIESGO[r]! })) },
      { nombre: 'recurso', etiqueta: 'Recurso', valor: (p) => p.recurso },
      { nombre: 'motivo', etiqueta: 'Motivo', valor: motivoDe },
      { nombre: 'mfa', etiqueta: 'MFA', valor: mfaDe },
    ],
    porPagina: 10,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Permisos', valor: contar(f), icono: 'candado' },
      { etiqueta: 'Riesgo crítico', valor: contar(f, (p) => p.nivelRiesgo === 'CRITICO'), icono: 'alerta', tono: 'error' },
      { etiqueta: 'Exigen motivo', valor: contar(f, (p) => p.exigeMotivo), icono: 'documento', tono: 'aviso' },
      { etiqueta: 'Requieren MFA', valor: contar(f, (p) => p.requiereMfa), icono: 'llave', tono: 'info' },
    ]
  })
  protected readonly columnas: Columna<PermisoCatalogo>[] = [
    { clave: 'codigo', titulo: 'Permiso', ordenable: true },
    { clave: 'recurso', titulo: 'Recurso', ordenable: true },
    { clave: 'accion', titulo: 'Acción', ordenable: true },
    { clave: 'nivelRiesgo', titulo: 'Riesgo', ordenable: true },
    { clave: 'exigeMotivo', titulo: 'Motivo' },
    { clave: 'requiereMfa', titulo: 'MFA' },
    { clave: 'descripcion', titulo: 'Descripción' },
  ]
  protected readonly exportables: ColumnaExportable<PermisoCatalogo>[] = [
    { clave: 'codigo', titulo: 'Permiso' },
    { clave: 'recurso', titulo: 'Recurso' },
    { clave: 'accion', titulo: 'Acción' },
    { clave: 'nivelRiesgo', titulo: 'Riesgo', valor: riesgoDe },
    { clave: 'exigeMotivo', titulo: 'Motivo', valor: motivoDe },
    { clave: 'requiereMfa', titulo: 'MFA', valor: mfaDe },
    { clave: 'descripcion', titulo: 'Descripción' },
  ]
  protected readonly riesgo = riesgoDe
  protected tono(p: PermisoCatalogo): Tono {
    return TONO[p.nivelRiesgo] ?? 'neutro'
  }
}
