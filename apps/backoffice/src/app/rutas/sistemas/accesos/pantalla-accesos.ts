import { ChangeDetectionStrategy, Component, computed, inject, resource } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { PUERTO_ACCESOS, type Acceso } from '../dominio/puertos'
import { textosSistemas } from '../textos'

/**
 * Quién tiene acceso a qué. Solo lectura: revocar un acceso no tiene contrato en esta fuente; los roles de una
 * persona interna se cambian desde «Usuarios internos» (PUT `/usuarios-internos/{id}/roles`).
 */
@Component({
  selector: 'ap-pantalla-accesos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, CifrasDeSistemas, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito" icono="llave">
        <ap-boton acciones icono="recarga" [deshabilitado]="accesos.isLoading()" (pulsado)="accesos.reload()">Actualizar</ap-boton>
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de accesos" [cifras]="cifras()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por persona, rol, ámbito o fecha de otorgamiento." nombreDeRegistros="accesos" [textoVacio]="t.vacio" explicacionVacio="Cuando se otorgue un acceso, aparece acá con su rol y su ámbito." (reintentar)="accesos.reload()" />
    </section>
  `,
  styles: `.pantalla { display: grid; gap: var(--s5); min-width: 0; }`,
})
export class PantallaAccesos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.accesos
  private readonly puerto = inject(PUERTO_ACCESOS)
  protected readonly accesos = resource({ loader: () => this.puerto.obtener() })
  protected readonly carga = cargaDe(this.accesos)
  private readonly filas = filasDe<Acceso>(this.accesos)
  protected readonly listado = crearListado<Acceso>({
    filas: this.filas,
    camposDeBusqueda: ['persona', 'rol', 'ambito', 'otorgadoEl'],
    filtros: [
      { nombre: 'rol', etiqueta: 'Rol', valor: (a) => a.rol },
      { nombre: 'ambito', etiqueta: 'Ámbito', valor: (a) => a.ambito },
    ],
    porPagina: 10,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Accesos', valor: contar(f), icono: 'llave' },
      { etiqueta: 'Personas', valor: String(new Set(f.map((a) => a.persona)).size), icono: 'persona' },
      { etiqueta: 'Roles distintos', valor: String(new Set(f.map((a) => a.rol)).size), icono: 'escudo' },
    ]
  })
  protected readonly columnas: Columna<Acceso>[] = [
    { clave: 'persona', titulo: 'Persona', ordenable: true },
    { clave: 'rol', titulo: 'Rol', ordenable: true },
    { clave: 'ambito', titulo: 'Ámbito', ordenable: true },
    { clave: 'otorgadoEl', titulo: 'Otorgado el', ordenable: true },
  ]
}
