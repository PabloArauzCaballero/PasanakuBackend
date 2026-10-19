import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { RolCatalogo } from 'clientes/angular/identidad'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { catalogoRoles } from '../dominio/usuarios-internos'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-roles-internos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, TablaDeDatos, ChipEstado, EstadoDePantalla, Boton, FormsModule],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="candado" />

      <div class="filtros" role="group" aria-label="Filtrar por departamento">
        <ap-boton [variante]="deptoFiltro() === 'TODOS' ? 'primario' : 'fantasma'" (pulsado)="deptoFiltro.set('TODOS')">
          Todos los departamentos
        </ap-boton>
        @for (d of departamentos(); track d) {
          <ap-boton [variante]="deptoFiltro() === d ? 'primario' : 'fantasma'" (pulsado)="deptoFiltro.set(d)">
            {{ d }}
          </ap-boton>
        }
      </div>

      <ap-estado-de-pantalla [recurso]="recurso" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="recurso.reload()">
        @if (rolesFiltrados(); as filas) {
          <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
            <ng-template #celda let-fila let-columna="columna">
              @if (columna.clave === 'codigo') {
                <code>{{ fila.codigo }}</code>
              } @else if (columna.clave === 'esSistema') {
                <ap-chip-estado [tono]="fila.esSistema ? 'neutro' : 'ok'">
                  {{ fila.esSistema ? 'Sistema' : 'Personalizado' }}
                </ap-chip-estado>
              } @else if (columna.clave === 'permisos') {
                <span class="conteo-permisos">{{ fila.permisos?.length ?? 0 }} permisos</span>
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
    .filtros { display: flex; gap: var(--s2); flex-wrap: wrap; }
    code { font-family: var(--fuente-mono, monospace); font-size: .8125rem; color: var(--color-primario); }
    .conteo-permisos { font-size: .8125rem; color: var(--text-2); }
  `,
})
export class PantallaRolesInternos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.rolesInternos
  protected readonly recurso = catalogoRoles()
  protected readonly deptoFiltro = signal('TODOS')

  protected readonly departamentos = computed(() => {
    const list = this.recurso.value() ?? []
    const set = new Set<string>()
    for (const r of list) {
      if (r.departamento) set.add(r.departamento)
    }
    return Array.from(set).sort()
  })

  protected readonly rolesFiltrados = computed(() => {
    const list = this.recurso.value() ?? []
    const d = this.deptoFiltro()
    if (d === 'TODOS') return list
    return list.filter((r) => r.departamento === d)
  })

  protected readonly esVacio = (filas: RolCatalogo[] | undefined) => !filas || filas.length === 0

  protected readonly columnas: Columna<RolCatalogo>[] = [
    { clave: 'codigo', titulo: 'Código de rol', ordenable: true },
    { clave: 'nombre', titulo: 'Nombre' },
    { clave: 'departamento', titulo: 'Departamento' },
    { clave: 'descripcion', titulo: 'Descripción' },
    { clave: 'esSistema', titulo: 'Origen' },
    { clave: 'permisos', titulo: 'Permisos' },
  ]
}
