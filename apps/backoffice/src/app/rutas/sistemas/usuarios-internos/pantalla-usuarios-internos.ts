import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core'
import { Router } from '@angular/router'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Icono } from '@aportaya/ui/icono/icono'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { UsuarioInternoResumen } from 'clientes/angular/identidad'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { usuariosInternos } from '../dominio/usuarios-internos'
import { textosSistemas } from '../textos'
import { GestorDeUsuarioInterno } from './gestor-de-usuario-interno'
import { ModalAltaUsuarioInterno } from './modal-alta-usuario-interno'
import { ModalAsignarRoles } from './modal-asignar-roles'
import { Exportador } from '../../../nucleo/exportador'

type Usuario = UsuarioInternoResumen
const ESTADO: Record<string, string> = { ACTIVO: 'Activo', SUSPENDIDO: 'Suspendido' }
const estadoDe = (u: Usuario) => ESTADO[u.estado] ?? u.estado
const segundoFactor = (u: Usuario) => (u.tieneSegundoFactor ? 'Configurado' : 'Pendiente')

/**
 * Usuarios internos (`GET /usuarios-internos`, sin paginar en el contrato: la colección entera se filtra y pagina en
 * el navegador). Cada fila tiene las operaciones que el contrato sí tiene: Ver (ficha), Editar (PATCH datos), Roles
 * (PUT roles), Suspender/Reactivar (PATCH estado) y Desbloquear (POST) si está bloqueado. No hay DELETE: no se dibuja
 * «Eliminar».
 */
@Component({
  selector: 'ap-pantalla-usuarios-internos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, GestorDeUsuarioInterno, Icono, ListadoDeDatos, ModalAltaUsuarioInterno, ModalAsignarRoles],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas · Identidad" [titulo]="t.titulo" [descripcion]="t.proposito" icono="persona">
        @if (carga().tipo !== 'sin-fuente') { <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="recurso.isLoading()" (pulsado)="recurso.reload()">Actualizar</ap-boton> }
        <ap-boton acciones variante="primario" icono="personaMas" (pulsado)="mostrarAlta.set(true)">{{ t.nuevoUsuario }}</ap-boton>
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de usuarios internos" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" [identidad]="identidad" ayudaDeBusqueda="Busca por nombre, correo, código, departamento, cargo o rol." [exportables]="exportables" [exportable]="exportador.puedeExportar('usuarios-internos')" nombreDeRegistros="usuarios" [textoVacio]="t.vacio" explicacionVacio="Dá de alta al primer operador interno con su rol inicial." accionVacio="Dar de alta" (primeraAccion)="mostrarAlta.set(true)" (reintentar)="recurso.reload()">
        <ng-template #celda let-u let-c="columna">
          @switch (c.clave) {
            @case ('nombres') { <strong>{{ u.nombres }} {{ u.apellidos }}</strong><span class="sub">{{ u.correo }}</span> }
            @case ('roles') { <span class="roles">@for (r of u.roles; track r) { <code>{{ r }}</code> }</span> }
            @case ('estado') {
              <ap-chip-estado [tono]="u.estado === 'ACTIVO' ? 'ok' : 'aviso'">{{ estado(u) }}</ap-chip-estado>
              @if (u.bloqueadoHasta) { <ap-chip-estado tono="error">Bloqueado</ap-chip-estado> }
            }
            @case ('tieneSegundoFactor') { <ap-chip-estado [tono]="u.tieneSegundoFactor ? 'ok' : 'neutro'">{{ factor(u) }}</ap-chip-estado> }
            @default { {{ u[c.clave] }} }
          }
        </ng-template>
        <ng-template #acciones let-u>
          <ap-acciones-de-fila [fila]="u.nombres + ' ' + u.apellidos" [ver]="true" (verPulsado)="ver(u)">
            <!-- Cinco acciones en la fila la estiraban a cuatro líneas: Ver queda a la vista y el resto va en «Más». -->
            <details class="mas" (toggle)="ubicarMenu($event)">
              <summary [attr.aria-label]="'Más acciones para ' + u.nombres + ' ' + u.apellidos"><ap-icono nombre="puntos" tamano="s4" />Más</summary>
              <div class="menu">
                <button type="button" apAccionDeFila [attr.aria-label]="'Asignar roles a ' + u.nombres + ' ' + u.apellidos" (click)="rolesDe.set(u)"><ap-icono nombre="escudo" tamano="s4" />Roles</button>
                <button type="button" apAccionDeFila [attr.aria-label]="(u.estado === 'ACTIVO' ? 'Suspender a ' : 'Reactivar a ') + u.nombres + ' ' + u.apellidos" (click)="gestor().cambiarEstado(u)"><ap-icono [nombre]="u.estado === 'ACTIVO' ? 'pausa' : 'reproducir'" tamano="s4" />{{ u.estado === 'ACTIVO' ? 'Suspender' : 'Reactivar' }}</button>
                @if (u.bloqueadoHasta) {
                  <button type="button" apAccionDeFila [attr.aria-label]="'Desbloquear a ' + u.nombres + ' ' + u.apellidos" (click)="gestor().desbloquear(u)"><ap-icono nombre="llave" tamano="s4" />Desbloquear</button>
                }
                <button type="button" apAccionDeFila [attr.aria-label]="'Editar ' + u.nombres + ' ' + u.apellidos" (click)="gestor().editar(u)"><ap-icono nombre="editar" tamano="s4" />Editar</button>
              </div>
            </details>
          </ap-acciones-de-fila>
        </ng-template>
      </ap-listado>

      <ap-gestor-de-usuario-interno (actualizado)="recurso.reload()" />
      @if (mostrarAlta()) {
        <ap-modal-alta-usuario-interno (cerrar)="mostrarAlta.set(false)" (guardado)="onUsuarioCreado()" />
      }
      @if (rolesDe(); as u) {
        <ap-modal-asignar-roles [usuarioId]="u.id" [rolesActuales]="u.roles" (cerrar)="rolesDe.set(null)" (guardado)="onRolesGuardados()" />
      }
    </section>
  `,
  host: { '(document:click)': 'cerrarMenus($event)', '(document:keydown.escape)': 'cerrarMenus()', '(window:resize)': 'cerrarMenus()', '(window:scroll)': 'cerrarMenus()' },
  styles: `
    :host { display: block; min-width: 0; }
    /* «Más»: menú propio de la fila. Se ubica fijo junto a su botón (y hacia arriba si abajo no entra): dentro de la
       tabla lo recortaría su marco desplazable. */
    .mas { position: relative; }
    .mas summary { display: inline-flex; align-items: center; gap: var(--s1); min-height: var(--area-tactil); padding: 0 var(--s2); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); color: var(--text); font: var(--t-cuerpo-chico); font-weight: 600; list-style: none; cursor: pointer; }
    .mas summary::-webkit-details-marker { display: none; }
    .mas summary:hover, .mas[open] summary { background: var(--surface-2); }
    .mas summary:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .menu { position: fixed; z-index: 20; display: grid; gap: var(--s1); min-width: calc(var(--s7) * 4); padding: var(--s2); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); box-shadow: var(--sombra-3); }
    .menu [apAccionDeFila] { justify-content: flex-start; width: 100%; border-color: transparent; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }
    .sub { display: block; font: var(--t-ayuda); color: var(--text-3); overflow-wrap: anywhere; }
    .roles { display: flex; flex-wrap: wrap; gap: var(--s1); }
    code { padding: 0 var(--s1); border-radius: var(--r-sm); background: var(--surface-2); font: var(--t-ayuda); font-family: var(--f-mono); color: var(--text-2); }
  `,
})
export class PantallaUsuariosInternos {
  /** Correos y datos del personal: la descarga en el navegador solo con `exportar:usuarios-internos` (regla 90.2). */
  protected readonly exportador = inject(Exportador)
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  private readonly router = inject(Router)
  protected readonly gestor = viewChild.required(GestorDeUsuarioInterno)
  protected readonly t = textosSistemas.usuariosInternos
  protected readonly mostrarAlta = signal(false)
  protected readonly rolesDe = signal<Usuario | null>(null)
  // El contrato acepta estado/rol/busqueda en la consulta, pero devuelve la colección sin paginar: se pide entera y
  // el kit busca y filtra en el navegador, así el CSV/JSON baja exactamente lo que se ve filtrado.
  protected readonly recurso = usuariosInternos({ estado: signal(''), rol: signal(''), busqueda: signal('') })
  protected readonly carga = cargaDe(this.recurso)
  private readonly filas = filasDe<Usuario>(this.recurso)
  protected readonly identidad = (u: Usuario) => u.id
  protected readonly listado = crearListado<Usuario>({
    filas: this.filas,
    camposDeBusqueda: ['nombres', 'apellidos', 'correo', 'codigoPublico', 'departamento', 'cargo', (u) => u.roles.join(' ')],
    filtros: [
      { nombre: 'estado', etiqueta: 'Estado', valor: (u) => u.estado, texto: (v) => ESTADO[v] ?? v },
      { nombre: 'departamento', etiqueta: 'Departamento', valor: (u) => u.departamento },
      { nombre: 'segundoFactor', etiqueta: 'Segundo factor', valor: segundoFactor },
    ],
    porPagina: 10,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Usuarios internos', valor: contar(f), icono: 'grupo' },
      { etiqueta: 'Activos', valor: contar(f, (u) => u.estado === 'ACTIVO'), icono: 'verificado', tono: 'ok' },
      { etiqueta: 'Suspendidos', valor: contar(f, (u) => u.estado === 'SUSPENDIDO'), icono: 'pausa', tono: 'aviso' },
      { etiqueta: 'Sin segundo factor', valor: contar(f, (u) => !u.tieneSegundoFactor), icono: 'candado', tono: 'error' },
    ]
  })
  protected readonly columnas: Columna<Usuario>[] = [
    { clave: 'nombres', titulo: 'Persona', ordenable: true },
    { clave: 'departamento', titulo: 'Departamento', ordenable: true },
    { clave: 'cargo', titulo: 'Cargo', ordenable: true },
    { clave: 'roles', titulo: 'Roles' },
    { clave: 'tieneSegundoFactor', titulo: 'Segundo factor' },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<Usuario>[] = [
    { clave: 'codigoPublico', titulo: 'Código' },
    { clave: 'nombres', titulo: 'Nombre', valor: (u) => `${u.nombres} ${u.apellidos}` },
    { clave: 'correo', titulo: 'Correo' },
    { clave: 'departamento', titulo: 'Departamento' },
    { clave: 'cargo', titulo: 'Cargo' },
    { clave: 'roles', titulo: 'Roles', valor: (u) => u.roles.join(' | ') },
    { clave: 'tieneSegundoFactor', titulo: 'Segundo factor', valor: segundoFactor },
    { clave: 'estado', titulo: 'Estado', valor: estadoDe },
  ]
  protected readonly estado = estadoDe
  protected readonly factor = segundoFactor

  private readonly elemento = inject(ElementRef<HTMLElement>)

  /** Un clic fuera, Escape o elegir una acción cierra el menú «Más» abierto; abrir uno cierra los demás. */
  protected cerrarMenus(evento?: Event): void {
    const destino = evento?.target instanceof Element ? evento.target : null
    for (const d of this.elemento.nativeElement.querySelectorAll('details.mas[open]')) {
      if (!destino || !d.contains(destino) || destino.closest('.menu')) d.removeAttribute('open')
    }
  }

  /** Al abrir, el menú se pone debajo de «Más», alineado a su borde derecho; si no entra abajo, arriba. */
  protected ubicarMenu(evento: Event): void {
    const d = evento.target as HTMLDetailsElement
    const menu = d.querySelector<HTMLElement>('.menu')
    if (!d.open || !menu) return
    const boton = d.querySelector('summary')!.getBoundingClientRect()
    const separacion = 4
    const alto = menu.offsetHeight
    const arriba = boton.bottom + separacion + alto > window.innerHeight
    menu.style.top = `${arriba ? boton.top - separacion - alto : boton.bottom + separacion}px`
    menu.style.left = `${Math.max(separacion, boton.right - menu.offsetWidth)}px`
  }

  protected ver(u: Usuario): void {
    void this.router.navigate(['/sistemas/usuarios-internos', u.id])
  }

  protected onUsuarioCreado(): void {
    this.mostrarAlta.set(false)
    this.recurso.reload()
  }

  protected onRolesGuardados(): void {
    this.rolesDe.set(null)
    this.recurso.reload()
  }
}
