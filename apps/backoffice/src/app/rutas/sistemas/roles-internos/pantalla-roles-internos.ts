import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { CajonLateral } from '@aportaya/ui/cajon-lateral/cajon-lateral'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { RolCatalogo } from 'clientes/angular/identidad'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { catalogoRoles } from '../dominio/usuarios-internos'
import { textosSistemas } from '../textos'

const origenDe = (r: RolCatalogo) => (r.esSistema ? 'Sistema' : 'Personalizado')

/**
 * Catálogo de roles internos (`GET /roles`). Es de solo lectura en el contrato —no hay alta, edición ni baja de
 * roles—, así que la única acción de fila es **Ver**, que abre en un cajón los permisos que suma el rol.
 */
@Component({
  selector: 'ap-pantalla-roles-internos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Boton, CabeceraDePagina, CajonLateral, ChipEstado, CifrasDeSistemas, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas · Identidad" [titulo]="t.titulo" [descripcion]="t.proposito">
        @if (carga().tipo !== 'sin-fuente') { <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="recurso.isLoading()" (pulsado)="recurso.reload()">Actualizar</ap-boton> }
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de roles" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por código, nombre, descripción o un permiso que tenga el rol." [exportables]="exportables" nombreDeRegistros="roles" [textoVacio]="t.vacio" explicacionVacio="Los roles internos los define el servicio de identidad; cuando existan, aparecen acá." (reintentar)="recurso.reload()">
        <ng-template #celda let-r let-c="columna">
          @switch (c.clave) {
            @case ('codigo') { <code>{{ r.codigo }}</code> }
            @case ('esSistema') { <ap-chip-estado [tono]="r.esSistema ? 'neutro' : 'ok'">{{ origen(r) }}</ap-chip-estado> }
            @case ('permisos') { {{ r.permisos.length }} {{ r.permisos.length === 1 ? 'permiso' : 'permisos' }} }
            @default { {{ r[c.clave] ?? '—' }} }
          }
        </ng-template>
        <ng-template #acciones let-r>
          <ap-acciones-de-fila [fila]="r.nombre" [ver]="true" textoVer="Ver permisos" (verPulsado)="ver(r)" />
        </ng-template>
      </ap-listado>

      <ap-cajon-lateral [titulo]="seleccionado() ? 'Permisos de ' + seleccionado()!.nombre : 'Permisos del rol'" [(abierto)]="cajonAbierto">
        @if (seleccionado(); as r) {
          <p class="sub">{{ r.descripcion || 'Sin descripción.' }} · {{ r.ambito }}</p>
          <ul class="permisos">
            @for (p of r.permisos; track p) { <li><code>{{ p }}</code></li> } @empty { <li class="sub">Este rol no suma permisos.</li> }
          </ul>
        }
        <ap-boton acciones variante="fantasma" icono="cerrar" (pulsado)="cajonAbierto.set(false)">Cerrar</ap-boton>
      </ap-cajon-lateral>
    </section>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    /* La tabla se desplaza dentro de su marco: las acciones van en una sola línea en vez de apilarse y estirar la fila. */
    ap-acciones-de-fila { flex-wrap: nowrap; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }
    code { padding: 0 var(--s1); border-radius: var(--r-sm); background: var(--surface-2); font: var(--t-ayuda); font-family: var(--f-mono); color: var(--text-2); overflow-wrap: anywhere; }
    .sub { margin: 0 0 var(--s3); font: var(--t-cuerpo-chico); color: var(--text-2); }
    .permisos { display: grid; gap: var(--s1); margin: 0; padding: 0; list-style: none; }
  `,
})
export class PantallaRolesInternos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.rolesInternos
  protected readonly recurso = catalogoRoles()
  protected readonly carga = cargaDe(this.recurso)
  private readonly filas = filasDe<RolCatalogo>(this.recurso)
  protected readonly seleccionado = signal<RolCatalogo | null>(null)
  protected readonly cajonAbierto = signal(false)
  protected readonly origen = origenDe
  protected readonly listado = crearListado<RolCatalogo>({
    filas: this.filas,
    camposDeBusqueda: ['codigo', 'nombre', 'descripcion', 'departamento', (r) => r.permisos.join(' ')],
    filtros: [
      { nombre: 'departamento', etiqueta: 'Departamento', valor: (r) => r.departamento ?? 'Sin departamento' },
      { nombre: 'ambito', etiqueta: 'Ámbito', valor: (r) => r.ambito },
      { nombre: 'origen', etiqueta: 'Origen', valor: origenDe },
    ],
    porPagina: 10,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Roles', valor: contar(f), icono: 'escudo' },
      { etiqueta: 'Del sistema', valor: contar(f, (r) => r.esSistema), icono: 'candado' },
      { etiqueta: 'Personalizados', valor: contar(f, (r) => !r.esSistema), icono: 'engranaje' },
      { etiqueta: 'Departamentos', valor: String(new Set(f.map((r) => r.departamento ?? '')).size), icono: 'carpeta' },
    ]
  })
  protected readonly columnas: Columna<RolCatalogo>[] = [
    { clave: 'codigo', titulo: 'Código de rol', ordenable: true },
    { clave: 'nombre', titulo: 'Nombre', ordenable: true },
    { clave: 'departamento', titulo: 'Departamento', ordenable: true },
    { clave: 'descripcion', titulo: 'Descripción' },
    { clave: 'esSistema', titulo: 'Origen' },
    { clave: 'permisos', titulo: 'Permisos', numerica: true },
  ]
  protected readonly exportables: ColumnaExportable<RolCatalogo>[] = [
    { clave: 'codigo', titulo: 'Código de rol' },
    { clave: 'nombre', titulo: 'Nombre' },
    { clave: 'ambito', titulo: 'Ámbito' },
    { clave: 'departamento', titulo: 'Departamento' },
    { clave: 'descripcion', titulo: 'Descripción' },
    { clave: 'esSistema', titulo: 'Origen', valor: origenDe },
    { clave: 'permisos', titulo: 'Permisos', valor: (r) => r.permisos.join(' | ') },
  ]

  protected ver(r: RolCatalogo): void {
    this.seleccionado.set(r)
    this.cajonAbierto.set(true)
  }
}
