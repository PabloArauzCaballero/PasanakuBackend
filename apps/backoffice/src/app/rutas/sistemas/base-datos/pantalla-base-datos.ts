import { ChangeDetectionStrategy, Component, computed, inject, resource } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { PUERTO_BASE_DE_DATOS, type Migracion } from '../dominio/puertos'
import { textosSistemas } from '../textos'

const ESTADO: Record<Migracion['estado'], string> = { aplicada: 'Aplicada', pendiente: 'Pendiente' }

/** Base y migraciones: qué está aplicado y qué falta, de solo lectura (las migraciones las corre el pipeline, no esta vista). */
@Component({
  selector: 'ap-pantalla-base-datos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito" icono="baseDeDatos">
        <ap-boton acciones icono="recarga" [deshabilitado]="migraciones.isLoading()" (pulsado)="migraciones.reload()">Actualizar</ap-boton>
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de migraciones" [cifras]="cifras()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por nombre de la migración, estado o fecha de aplicación." [exportables]="exportables" nombreDeRegistros="migraciones" [textoVacio]="t.vacio" explicacionVacio="Cuando el pipeline registre una migración, aparece acá con su estado." (reintentar)="migraciones.reload()">
        <ng-template #celda let-f let-c="columna">
          @switch (c.clave) {
            @case ('estado') { <ap-chip-estado [tono]="f.estado === 'aplicada' ? 'ok' : 'aviso'">{{ estadoDe(f) }}</ap-chip-estado> }
            @case ('aplicadaEl') { {{ f.aplicadaEl ?? 'Sin aplicar' }} }
            @default { {{ f[c.clave] }} }
          }
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `:host { display: block; min-width: 0; } .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; } /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }`,
})
export class PantallaBaseDeDatos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.baseDeDatos
  private readonly puerto = inject(PUERTO_BASE_DE_DATOS)
  protected readonly migraciones = resource({ loader: () => this.puerto.obtener() })
  protected readonly carga = cargaDe(this.migraciones)
  private readonly filas = filasDe<Migracion>(this.migraciones)
  protected readonly listado = crearListado<Migracion>({
    filas: this.filas,
    camposDeBusqueda: ['nombre', (m) => ESTADO[m.estado], 'aplicadaEl'],
    filtros: [{ nombre: 'estado', etiqueta: 'Estado', valor: (m) => m.estado, texto: (v) => ESTADO[v as Migracion['estado']] ?? v }],
    porPagina: 10,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Migraciones', valor: contar(f), icono: 'baseDeDatos' },
      { etiqueta: 'Aplicadas', valor: contar(f, (m) => m.estado === 'aplicada'), icono: 'verificado', tono: 'ok' },
      { etiqueta: 'Pendientes', valor: contar(f, (m) => m.estado === 'pendiente'), icono: 'reloj', tono: 'aviso' },
    ]
  })
  protected readonly columnas: Columna<Migracion>[] = [
    { clave: 'nombre', titulo: 'Migración', ordenable: true, mono: true },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
    { clave: 'aplicadaEl', titulo: 'Aplicada el', ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<Migracion>[] = [
    { clave: 'nombre', titulo: 'Migración' },
    { clave: 'estado', titulo: 'Estado', valor: (m) => ESTADO[m.estado] },
    { clave: 'aplicadaEl', titulo: 'Aplicada el', valor: (m) => m.aplicadaEl ?? 'Sin aplicar' },
  ]
  protected estadoDe(m: Migracion): string {
    return ESTADO[m.estado]
  }
}
