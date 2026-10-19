import { ChangeDetectionStrategy, Component, inject, resource } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { PUERTO_ACCESOS, type Acceso } from '../dominio/puertos'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-accesos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, TablaDeDatos, EstadoDePantalla],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="candado" />
      <ap-estado-de-pantalla [recurso]="accesos" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="accesos.reload()">
        @if (accesos.hasValue() && accesos.value(); as filas) {
          <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas" />
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `.pantalla { padding: 0; max-width: 60rem; display: flex; flex-direction: column; gap: var(--s4); }`,
})
export class PantallaAccesos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.accesos
  private readonly puerto = inject(PUERTO_ACCESOS)
  protected readonly accesos = resource({ loader: () => this.puerto.obtener() })
  protected readonly esVacio = (filas: Acceso[]) => filas.length === 0
  protected readonly columnas: Columna<Acceso>[] = [
    { clave: 'persona', titulo: 'Persona', ordenable: true },
    { clave: 'rol', titulo: 'Rol' },
    { clave: 'ambito', titulo: 'Ámbito' },
    { clave: 'otorgadoEl', titulo: 'Otorgado el' },
  ]
}
