import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { RouterLink } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { UsuarioInternoResumen } from 'clientes/angular/identidad'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { usuariosInternos } from '../dominio/usuarios-internos'
import { textosSistemas } from '../textos'
import { ModalAltaUsuarioInterno } from './modal-alta-usuario-interno'

@Component({
  selector: 'ap-pantalla-usuarios-internos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, TablaDeDatos, ChipEstado, EstadoDePantalla, Boton, FormsModule, RouterLink, ModalAltaUsuarioInterno],
  template: `
    <section class="pantalla">
      <header class="encabezado-pantalla">
        <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="candado" />
        <ap-boton variante="primario" (pulsado)="mostrarAlta.set(true)">
          + {{ t.nuevoUsuario }}
        </ap-boton>
      </header>

      <div class="filtros" role="group" [attr.aria-label]="t.filtrarEstado">
        <div class="estados">
          @for (e of estadosFiltro; track e) {
            <ap-boton [variante]="estadoFiltro() === e ? 'primario' : 'fantasma'" [presionado]="estadoFiltro() === e" (pulsado)="estadoFiltro.set(e)">
              {{ e }}
            </ap-boton>
          }
        </div>

        <label class="busqueda">
          <input type="search" [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" [placeholder]="t.buscar" [attr.aria-label]="t.buscar" />
        </label>
      </div>

      <ap-estado-de-pantalla [recurso]="recurso" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="recurso.reload()">
        @if (recurso.hasValue() && recurso.value(); as filas) {
          <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
            <ng-template #celda let-fila let-columna="columna">
              @if (columna.clave === 'correo') {
                <a [routerLink]="['/sistemas/usuarios-internos', fila.id]" class="enlace-usuario">
                  <strong>{{ fila.correo }}</strong>
                </a>
              } @else if (columna.clave === 'nombres') {
                {{ fila.nombres }} {{ fila.apellidos }}
              } @else if (columna.clave === 'roles') {
                <div class="lista-roles">
                  @for (r of fila.roles ?? []; track r) {
                    <span class="chip-rol">{{ r }}</span>
                  }
                </div>
              } @else if (columna.clave === 'estado') {
                <ap-chip-estado [tono]="fila.estado === 'ACTIVO' ? 'ok' : 'aviso'">
                  {{ fila.estado }}
                </ap-chip-estado>
              } @else if (columna.clave === 'tieneSegundoFactor') {
                <ap-chip-estado [tono]="fila.tieneSegundoFactor ? 'ok' : 'neutro'">
                  {{ fila.tieneSegundoFactor ? 'Configurado' : 'Pendiente' }}
                </ap-chip-estado>
              } @else {
                {{ fila[columna.clave] }}
              }
            </ng-template>
          </ap-tabla-de-datos>
        }
      </ap-estado-de-pantalla>

      @if (mostrarAlta()) {
        <ap-modal-alta-usuario-interno (cerrar)="mostrarAlta.set(false)" (guardado)="onUsuarioCreado()" />
      }
    </section>
  `,
  styles: `
    .pantalla { padding: 0; max-width: 68rem; display: flex; flex-direction: column; gap: var(--s4); }
    .encabezado-pantalla { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--s3); }
    .filtros { display: flex; justify-content: space-between; align-items: center; gap: var(--s3); flex-wrap: wrap; }
    .estados { display: flex; gap: var(--s2); }
    .busqueda { min-width: 18rem; }
    .busqueda input { width: 100%; min-height: 2.5rem; padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); color: var(--text); font: inherit; }
    .enlace-usuario { color: var(--color-primario); text-decoration: none; }
    .enlace-usuario:hover { text-decoration: underline; }
    .lista-roles { display: flex; gap: var(--s1); flex-wrap: wrap; }
    .chip-rol { font-size: .75rem; padding: var(--s1); border-radius: var(--r-sm); background: var(--surface-2); border: var(--borde-fino) solid var(--border); }
  `,
})
export class PantallaUsuariosInternos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.usuariosInternos
  protected readonly estadosFiltro = ['TODOS', 'ACTIVO', 'SUSPENDIDO']
  protected readonly estadoFiltro = signal('TODOS')
  protected readonly rolFiltro = signal('')
  protected readonly busqueda = signal('')
  protected readonly mostrarAlta = signal(false)

  protected readonly recurso = usuariosInternos({
    estado: this.estadoFiltro,
    rol: this.rolFiltro,
    busqueda: this.busqueda,
  })

  protected readonly esVacio = (filas: UsuarioInternoResumen[]) => filas.length === 0

  protected readonly columnas: Columna<UsuarioInternoResumen>[] = [
    { clave: 'correo', titulo: 'Correo / Usuario', ordenable: true },
    { clave: 'nombres', titulo: 'Nombre' },
    { clave: 'departamento', titulo: 'Departamento' },
    { clave: 'cargo', titulo: 'Cargo' },
    { clave: 'roles', titulo: 'Roles' },
    { clave: 'tieneSegundoFactor', titulo: 'Segundo factor' },
    { clave: 'estado', titulo: 'Estado' },
  ]

  protected onUsuarioCreado(): void {
    this.mostrarAlta.set(false)
    this.recurso.reload()
  }
}
