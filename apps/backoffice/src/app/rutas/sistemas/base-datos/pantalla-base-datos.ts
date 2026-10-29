import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { recursoSaludDeServicios } from '../../operacion/estado/dominio/estado-del-sistema'
import { cargaDe } from '../compartido/carga'
import { CifrasDeSistemas, type Cifra } from '../compartido/cifras'
import { filasDeEsquemas, recursoResumenDeLaBase, type FilaDeEsquema } from '../dominio/base-de-datos'
import { textosSistemas } from '../textos'

/**
 * Base de datos · cada esquema real con su servicio dueño, sus cifras y si el dueño alcanza la base ahora. Antes leía
 * `PUERTO_BASE_DE_DATOS` (migraciones), que en producción siempre rechazaba: el sistema no tiene migraciones versionadas
 * que listar (ver `dominio/base-de-datos.ts`). El detalle de cada tabla vive en el Catálogo de datos, enlazado por esquema.
 */
@Component({
  selector: 'ap-pantalla-base-datos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, ListadoDeDatos, RouterLink],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito" icono="baseDeDatos">
        <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="resumen.isLoading() || salud.isLoading()" (pulsado)="recargar()">Volver a consultar</ap-boton>
      </ap-cabecera-de-pagina>
      <p class="nota">{{ t.nota }} <a class="enlace" routerLink="/tablero/catalogo-de-datos">Ver cada tabla en el Catálogo de datos</a>.</p>
      <ap-cifras-de-sistemas etiqueta="Resumen de la base" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por esquema, servicio dueño o lo que guarda." [exportables]="exportables" nombreDeRegistros="esquemas" [textoVacio]="t.vacio" explicacionVacio="Ningún esquema coincide con la búsqueda o el filtro." (reintentar)="recargar()">
        <ng-template #celda let-f let-c="columna">
          @switch (c.clave) {
            @case ('esquema') { <a class="enlace-mono" [routerLink]="'/tablero/catalogo-de-datos'" [queryParams]="{ esquema: f.esquema }" [attr.aria-label]="'Ver las tablas del esquema ' + f.esquema + ' en el Catálogo de datos'">{{ f.esquema }}</a> }
            @case ('conexion') { <ap-chip-estado [tono]="f.tono">{{ f.conexion }}</ap-chip-estado> }
            @default { {{ f[c.clave] }} }
          }
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `:host { display: block; min-width: 0; } .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; } .nota { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); } .enlace { color: var(--brand-texto); font-weight: 600; } .enlace-mono { color: var(--brand-texto); font-weight: 600; text-decoration: none; } .enlace-mono:hover, .enlace:hover { text-decoration: underline; } .enlace-mono:focus-visible, .enlace:focus-visible { outline: var(--borde-foco) solid var(--brand); outline-offset: var(--borde-desfase); } /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }`,
})
export class PantallaBaseDeDatos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.baseDeDatos
  protected readonly resumen = recursoResumenDeLaBase()
  /** La conexión es complemento: si la salud falla, el esquema se ve igual y la columna dice «sin dato». */
  protected readonly salud = recursoSaludDeServicios()
  protected readonly carga = cargaDe(this.resumen)
  private readonly filas = computed<FilaDeEsquema[]>(() => {
    if (!this.resumen.hasValue() || !this.resumen.value()) return []
    const salud = this.salud.hasValue() ? this.salud.value() : undefined
    return filasDeEsquemas(this.resumen.value()!, salud)
  })
  protected readonly listado = crearListado<FilaDeEsquema>({
    filas: this.filas,
    camposDeBusqueda: ['esquema', 'servicio', 'queEs'],
    filtros: [{ nombre: 'conexion', etiqueta: 'Conexión', valor: (f) => f.conexion }],
    porPagina: 20,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const r = this.resumen.hasValue() ? this.resumen.value() : undefined
    const n = (v: number | undefined) => (v === undefined ? '—' : v.toLocaleString('es-BO'))
    return [
      { etiqueta: 'Esquemas', valor: n(r?.esquemas.length), icono: 'baseDeDatos' },
      { etiqueta: 'Tablas', valor: n(r?.tablas), icono: 'tabla' },
      { etiqueta: 'Columnas', valor: n(r?.columnas), icono: 'lista' },
      { etiqueta: 'Relaciones', valor: n(r?.relaciones), icono: 'enlace' },
    ]
  })
  protected readonly columnas: Columna<FilaDeEsquema>[] = [
    { clave: 'esquema', titulo: 'Esquema', ordenable: true, mono: true },
    { clave: 'servicio', titulo: 'Servicio dueño', ordenable: true },
    { clave: 'conexion', titulo: 'Conexión ahora', ordenable: true },
    { clave: 'tablas', titulo: 'Tablas', ordenable: true, numerica: true },
    { clave: 'columnas', titulo: 'Columnas', ordenable: true, numerica: true },
    { clave: 'relaciones', titulo: 'Relaciones', numerica: true },
    { clave: 'appendOnly', titulo: 'Solo agregar', numerica: true },
    { clave: 'conDatosPersonales', titulo: 'Con datos personales', numerica: true },
    { clave: 'queEs', titulo: 'Qué guarda' },
  ]
  protected readonly exportables: ColumnaExportable<FilaDeEsquema>[] = [
    { clave: 'esquema', titulo: 'Esquema' },
    { clave: 'servicio', titulo: 'Servicio dueño' },
    { clave: 'conexion', titulo: 'Conexión ahora' },
    { clave: 'tablas', titulo: 'Tablas' },
    { clave: 'columnas', titulo: 'Columnas' },
    { clave: 'relaciones', titulo: 'Relaciones' },
    { clave: 'appendOnly', titulo: 'Solo agregar' },
    { clave: 'conDatosPersonales', titulo: 'Con datos personales' },
    { clave: 'queEs', titulo: 'Qué guarda' },
  ]
  protected recargar(): void {
    this.resumen.reload()
    this.salud.reload()
  }
}
