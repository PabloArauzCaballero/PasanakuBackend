import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { PermisoCatalogo } from 'clientes/angular/identidad'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { catalogoPermisos } from '../dominio/usuarios-internos'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-permisos-internos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, TablaDeDatos, ChipEstado, EstadoDePantalla, Boton, FormsModule],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="candado" />

      <div class="filtros" role="group" aria-label="Filtrar por nivel de riesgo">
        <div class="niveles">
          @for (n of nivelesRiesgo; track n) {
            <ap-boton [variante]="nivelFiltro() === n ? 'primario' : 'fantasma'" (pulsado)="nivelFiltro.set(n)">
              {{ n }}
            </ap-boton>
          }
        </div>

        <label class="busqueda">
          <input type="search" [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" placeholder="Buscar por recurso o acción..." aria-label="Buscar permisos" />
        </label>
      </div>

      <ap-estado-de-pantalla [recurso]="recurso" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="recurso.reload()">
        @if (permisosFiltrados(); as filas) {
          <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
            <ng-template #celda let-fila let-columna="columna">
              @if (columna.clave === 'codigo') {
                <code>{{ fila.codigo }}</code>
              } @else if (columna.clave === 'nivelRiesgo') {
                <ap-chip-estado [tono]="tonoRiesgo(fila.nivelRiesgo)">
                  {{ fila.nivelRiesgo }}
                </ap-chip-estado>
              } @else if (columna.clave === 'exigeMotivo') {
                <ap-chip-estado [tono]="fila.exigeMotivo ? 'aviso' : 'neutro'">
                  {{ fila.exigeMotivo ? 'Exige motivo' : 'Estándar' }}
                </ap-chip-estado>
              } @else if (columna.clave === 'requiereMfa') {
                <ap-chip-estado [tono]="fila.requiereMfa ? 'aviso' : 'neutro'">
                  {{ fila.requiereMfa ? 'Requiere MFA' : 'No' }}
                </ap-chip-estado>
              } @else {
                {{ fila[columna.clave] }}
              }
            </ng-template>
          </ap-tabla-de-datos>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { padding: 0; max-width: 68rem; display: flex; flex-direction: column; gap: var(--s4); }
    .filtros { display: flex; justify-content: space-between; align-items: center; gap: var(--s3); flex-wrap: wrap; }
    .niveles { display: flex; gap: var(--s2); flex-wrap: wrap; }
    .busqueda { min-width: 18rem; }
    .busqueda input { width: 100%; min-height: 2.5rem; padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); color: var(--text); font: inherit; }
    code { font-family: var(--fuente-mono, monospace); font-size: .8125rem; color: var(--color-primario); }
  `,
})
export class PantallaPermisosInternos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.permisosInternos
  protected readonly recurso = catalogoPermisos()
  protected readonly nivelesRiesgo = ['TODOS', 'BAJO', 'MEDIO', 'ALTO', 'CRITICO']
  protected readonly nivelFiltro = signal('TODOS')
  protected readonly busqueda = signal('')

  protected tonoRiesgo(nivel?: string): 'ok' | 'neutro' | 'aviso' | 'error' {
    if (nivel === 'CRITICO') return 'error'
    if (nivel === 'ALTO') return 'aviso'
    if (nivel === 'MEDIO') return 'neutro'
    return 'ok'
  }

  protected readonly permisosFiltrados = computed(() => {
    const list = this.recurso.value() ?? []
    const nf = this.nivelFiltro()
    const b = this.busqueda().trim().toLowerCase()

    return list.filter((p) => {
      const coincideNivel = nf === 'TODOS' || p.nivelRiesgo === nf
      const coincideBusqueda =
        !b ||
        p.codigo?.toLowerCase().includes(b) ||
        p.recurso?.toLowerCase().includes(b) ||
        p.accion?.toLowerCase().includes(b) ||
        p.descripcion?.toLowerCase().includes(b)
      return coincideNivel && coincideBusqueda
    })
  })

  protected readonly esVacio = (filas: PermisoCatalogo[] | undefined) => !filas || filas.length === 0

  protected readonly columnas: Columna<PermisoCatalogo>[] = [
    { clave: 'codigo', titulo: 'Permiso', ordenable: true },
    { clave: 'recurso', titulo: 'Recurso' },
    { clave: 'accion', titulo: 'Acción' },
    { clave: 'nivelRiesgo', titulo: 'Riesgo' },
    { clave: 'exigeMotivo', titulo: 'Motivo' },
    { clave: 'requiereMfa', titulo: 'MFA' },
    { clave: 'descripcion', titulo: 'Descripción' },
  ]
}
