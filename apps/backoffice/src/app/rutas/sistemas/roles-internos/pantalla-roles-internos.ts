import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core'
import { httpResource } from '@angular/common/http'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { CajonLateral } from '@aportaya/ui/cajon-lateral/cajon-lateral'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { PermisoCatalogo, RolCatalogo } from 'clientes/angular/identidad'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { rolesAsignables } from '../dominio/gobierno-de-accesos'
import { accionesDeRoles, RIESGO_DE_CUATRO_OJOS, type SalidaDeRol } from '../dominio/gobierno-de-roles'
import type { ErrorDeGobierno } from '../dominio/errores-de-gobierno'
import { GATEWAY } from '../../../nucleo/gateway'
import { textosSistemas } from '../textos'
import { DialogoDeActo } from '../compartido/dialogo-de-acto'
import { COLUMNAS, EXPORTABLES, origenDe } from './columnas-de-roles'
import { FormularioDeRol } from './formulario-de-rol'
import { PanelDeSolicitudesDeRol } from './panel-de-solicitudes-de-rol'

/**
 * Catálogo de roles asignables al personal interno. Es de solo lectura en el contrato —no hay alta, edición ni baja de
 * roles—, así que la única acción de fila es **Ver**, que abre en un cajón los permisos que suma el rol.
 */
@Component({
  selector: 'ap-pantalla-roles-internos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Alerta, Boton, CabeceraDePagina, CajonLateral, ChipEstado, CifrasDeSistemas, DialogoDeActo, FormularioDeRol, ListadoDeDatos, PanelDeSolicitudesDeRol],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas · Identidad" [titulo]="t.titulo" [descripcion]="t.proposito">
        <ap-boton acciones variante="primario" (pulsado)="abrirCrear()">Nuevo rol</ap-boton>
        @if (carga().tipo !== 'sin-fuente') { <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="recurso.isLoading()" (pulsado)="recurso.reload()">Actualizar</ap-boton> }
      </ap-cabecera-de-pagina>
      @if (aviso(); as a) { <ap-alerta tono="ok" titulo="Listo">{{ a }}</ap-alerta> }
      <ap-panel-de-solicitudes-de-rol (resuelta)="alResolverSolicitud($event)" />
      <ap-cifras-de-sistemas etiqueta="Resumen de roles" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por código, nombre, descripción o un permiso que tenga el rol." [exportables]="exportables" nombreDeRegistros="roles" [textoVacio]="t.vacio" explicacionVacio="Todavía no hay roles. Creá uno con «Nuevo rol»; los del sistema los define identidad." (reintentar)="recurso.reload()">
        <ng-template #celda let-r let-c="columna">
          @switch (c.clave) {
            @case ('codigo') { <code>{{ r.codigo }}</code> }
            @case ('esSistema') { <ap-chip-estado [tono]="r.esSistema ? 'neutro' : 'ok'">{{ origen(r) }}</ap-chip-estado> }
            @case ('permisos') { {{ r.permisos.length }} {{ r.permisos.length === 1 ? 'permiso' : 'permisos' }} }
            @default { {{ r[c.clave] ?? '—' }} }
          }
        </ng-template>
        <ng-template #acciones let-r>
          <ap-acciones-de-fila [fila]="r.nombre" [ver]="true" textoVer="Ver permisos" [editar]="!r.esSistema" textoEditar="Editar rol" (verPulsado)="ver(r)" (editarPulsado)="abrirEditar(r)" />
        </ng-template>
      </ap-listado>

      <ap-cajon-lateral [titulo]="seleccionado() ? 'Permisos de ' + seleccionado()!.nombre : 'Permisos del rol'" [(abierto)]="cajonAbierto">
        @if (seleccionado(); as r) {
          <p class="sub">{{ r.descripcion || 'Sin descripción.' }} · {{ r.ambito }}</p>
          <ul class="permisos">
            @for (p of r.permisos; track p) { <li><code>{{ p }}</code></li> } @empty { <li class="sub">Este rol no suma permisos.</li> }
          </ul>
        }
        @if (seleccionado(); as r) {
          @if (r.esSistema) { <p class="sub">Es un rol del sistema: no se edita ni se retira.</p> }
          @else { <ap-boton acciones variante="peligro" (pulsado)="abrirRetirar(r)">Retirar rol</ap-boton> }
        }
        <ap-boton acciones variante="fantasma" icono="cerrar" (pulsado)="cajonAbierto.set(false)">Cerrar</ap-boton>
      </ap-cajon-lateral>

      @if (formulario(); as modo) {
        <ap-dialogo-de-acto [titulo]="modo === 'crear' ? 'Nuevo rol personalizado' : 'Editar ' + (editando()?.nombre ?? 'rol')" textoDeConfirmar="Guardar rol" [valido]="formularioValido()" [maximoDeMotivo]="160" [efecto]="efectoDelFormulario()" [accion]="guardar" [(error)]="errorDelFormulario" [abierto]="true" (abiertoChange)="!$event && formulario.set(null)" (completado)="alGuardar($event)">
          <ap-formulario-de-rol [modo]="modo" [catalogo]="catalogoDePermisos()" [error]="errorDelFormulario()" [(codigo)]="codigo" [(nombre)]="nombre" [(descripcion)]="descripcion" [(seleccion)]="seleccion" />
        </ap-dialogo-de-acto>
      }
      @if (paraRetirar(); as r) {
        <ap-dialogo-de-acto [titulo]="'Retirar ' + r.nombre" textoDeConfirmar="Retirar rol" [destructivo]="true" [maximoDeMotivo]="160" efecto="El rol deja de ofrecerse y no se puede asignar más. Si todavía tiene personas asignadas, primero hay que revocarlas." [accion]="retirar" [abierto]="true" (abiertoChange)="!$event && paraRetirar.set(null)" (completado)="alGuardar($event)">
          <p>{{ r.nombre }} · <code>{{ r.codigo }}</code></p>
        </ap-dialogo-de-acto>
      }
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
  protected readonly recurso = rolesAsignables()
  protected readonly carga = cargaDe(this.recurso)
  private readonly filas = filasDe<RolCatalogo>(this.recurso)
  private readonly acciones = accionesDeRoles()
  private readonly gateway = inject(GATEWAY)
  /** El catálogo de permisos se pide recién al abrir el formulario: la pantalla sola no lo necesita. */
  private readonly permisos = httpResource<PermisoCatalogo[]>(() => (this.formulario() ? `${this.gateway}/permisos` : undefined))
  private readonly panel = viewChild(PanelDeSolicitudesDeRol)
  protected readonly catalogoDePermisos = computed<PermisoCatalogo[]>(() => (this.permisos.hasValue() ? this.permisos.value() : []))
  protected readonly formulario = signal<'crear' | 'editar' | null>(null)
  protected readonly editando = signal<RolCatalogo | null>(null)
  protected readonly paraRetirar = signal<RolCatalogo | null>(null)
  protected readonly errorDelFormulario = signal<ErrorDeGobierno | null>(null)
  protected readonly aviso = signal<string | null>(null)
  protected readonly codigo = signal('')
  protected readonly nombre = signal('')
  protected readonly descripcion = signal('')
  protected readonly seleccion = signal<string[]>([])
  protected readonly formularioValido = computed(() => {
    const codigoOk = this.formulario() === 'editar' || /^[A-Z][A-Z0-9_]{2,29}$/.test(this.codigo()) && !this.codigo().startsWith('SERVICIO_')
    return codigoOk && this.nombre().trim().length > 0 && this.seleccion().length > 0
  })
  protected readonly efectoDelFormulario = computed(() => {
    const riesgo = this.catalogoDePermisos().filter((p) => this.seleccion().includes(p.codigo) && RIESGO_DE_CUATRO_OJOS.includes(p.nivelRiesgo))
    return riesgo.length > 0
      ? 'Incluye permisos de riesgo alto o crítico: no se aplica ahora, queda pendiente hasta que otra persona lo apruebe.'
      : 'El cambio se aplica al confirmar y queda en la auditoría.'
  })
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
  protected readonly columnas = COLUMNAS
  protected readonly exportables = EXPORTABLES

  protected abrirCrear(): void {
    this.reiniciar('', '', '', [])
    this.editando.set(null)
    this.formulario.set('crear')
  }
  protected abrirEditar(r: RolCatalogo): void {
    this.reiniciar(r.codigo, r.nombre, r.descripcion ?? '', [...r.permisos])
    this.editando.set(r)
    this.formulario.set('editar')
  }
  protected abrirRetirar(r: RolCatalogo): void {
    this.cajonAbierto.set(false)
    this.paraRetirar.set(r)
  }
  private reiniciar(codigo: string, nombre: string, descripcion: string, seleccion: string[]): void {
    this.codigo.set(codigo)
    this.nombre.set(nombre)
    this.descripcion.set(descripcion)
    this.seleccion.set(seleccion)
    this.errorDelFormulario.set(null)
  }
  protected readonly guardar = (evidenciaMfa: string, motivo: string) => {
    const comun = { nombre: this.nombre().trim(), descripcion: this.descripcion().trim() || null, permisos: this.seleccion(), motivo, evidenciaMfa }
    const rol = this.editando()
    return rol ? this.acciones.editar(rol.id, comun) : this.acciones.crear({ ...comun, codigo: this.codigo() })
  }
  protected readonly retirar = (evidenciaMfa: string, motivo: string) => this.acciones.retirar(this.paraRetirar()!.id, { motivo, evidenciaMfa })
  protected alGuardar(salida: unknown): void {
    const r = salida as SalidaDeRol
    this.aviso.set(r.estado === 'PENDIENTE_DE_APROBACION' ? 'Quedó pendiente: otra persona tiene que aprobarlo antes de que se aplique.' : 'El cambio quedó aplicado y registrado en la auditoría.')
    this.recurso.reload()
    this.panel()?.recargar()
  }
  protected alResolverSolicitud(salida: SalidaDeRol): void {
    this.aviso.set(salida.estado === 'RECHAZADA' ? 'La solicitud se rechazó: no cambió ningún rol.' : 'La solicitud se aprobó y el cambio quedó aplicado.')
    this.recurso.reload()
  }

  protected ver(r: RolCatalogo): void {
    this.seleccionado.set(r)
    this.cajonAbierto.set(true)
  }
}
