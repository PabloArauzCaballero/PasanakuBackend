import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core'
import { Router } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Icono } from '@aportaya/ui/icono/icono'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { CargaDeListado } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { mensajeDeError } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { accionesDeAccesos, cursorDespuesDe, TAMANO_DE_PAGINA_DE_USUARIOS, type CursorDeUsuarios, type UsuarioInternoDeLista } from '../dominio/gobierno-de-accesos'
import { PERMISOS_DE_GESTION_DE_PERSONAL } from '../personal-y-accesos'
import { textosSistemas } from '../textos'
import { GestorDeUsuarioInterno } from './gestor-de-usuario-interno'
import { ModalAltaUsuarioInterno } from './modal-alta-usuario-interno'
import { ModalAsignarRol } from './modal-asignar-rol'
import { Exportador } from '../../../nucleo/exportador'

type Usuario = UsuarioInternoDeLista
const ESTADO: Record<string, string> = { ACTIVO: 'Activo', SUSPENDIDO: 'Suspendido', BAJA_LABORAL: 'Baja laboral', BAJA_VOLUNTARIA: 'Baja voluntaria', BLOQUEADO_SEGURIDAD: 'Bloqueado por seguridad' }
const TONO: Record<string, 'ok' | 'aviso' | 'neutro' | 'error'> = { ACTIVO: 'ok', SUSPENDIDO: 'aviso', BLOQUEADO_SEGURIDAD: 'error' }
/** Hay un bloqueo de acceso vigente: con fecha de fin, o indefinido (`bloqueadoHasta` nulo y `motivoBloqueo` informado, GU-10). */
const estaBloqueado = (u: Usuario) => !!u.bloqueadoHasta || !!u.motivoBloqueo
const estaDeBaja = (u: Usuario) => u.estado === 'BAJA_LABORAL'
const estadoDe = (u: Usuario) => ESTADO[u.estado] ?? u.estado
const segundoFactor = (u: Usuario) => (u.tieneSegundoFactor ? 'Configurado' : 'Pendiente')

/**
 * Usuarios internos (`GET /usuarios-internos`, **paginado por cursor**, GU-17: `limite` 1–100, `antesDe` + `antesDeId`
 * de la última fila). Se piden de a 50 y «Cargar más» pide la siguiente; la búsqueda y los filtros trabajan sobre lo ya
 * cargado y la pantalla lo dice. Cada fila tiene las operaciones del contrato: Ver (ficha) y, a quien gestiona, Asignar
 * rol, Suspender/Reactivar, Desbloquear, Dar de baja y Editar. Nada de esto se dibuja a quien solo lee.
 */
@Component({
  selector: 'ap-pantalla-usuarios-internos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Alerta, Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, GestorDeUsuarioInterno, Icono, ListadoDeDatos, ModalAltaUsuarioInterno, ModalAsignarRol],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas · Identidad" [titulo]="t.titulo" [descripcion]="t.proposito">
        @if (carga().tipo !== 'sin-fuente') { <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="cargando() || cargandoMas()" (pulsado)="recargar()">Actualizar</ap-boton> }
        @if (gestiona()) { <ap-boton acciones variante="primario" icono="personaMas" (pulsado)="mostrarAlta.set(true)">{{ t.nuevoUsuario }}</ap-boton> }
      </ap-cabecera-de-pagina>
      @if (aviso(); as texto) { <ap-alerta tono="ok" titulo="Listo">{{ texto }}</ap-alerta> }
      <ap-cifras-de-sistemas etiqueta="Resumen de usuarios internos" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" [identidad]="identidad" ayudaDeBusqueda="Busca por nombre, correo, código, departamento, cargo o rol entre los usuarios ya cargados." [exportables]="exportables" [exportable]="exportador.puedeExportar('usuarios-internos')" nombreDeRegistros="usuarios" [textoVacio]="t.vacio" explicacionVacio="Dá de alta al primer operador interno con su rol inicial." [accionVacio]="gestiona() ? 'Dar de alta' : undefined" (primeraAccion)="mostrarAlta.set(true)" (reintentar)="recargar()">
        <ng-template #celda let-u let-c="columna">
          @switch (c.clave) {
            @case ('nombres') { <strong>{{ u.nombres }} {{ u.apellidos }}</strong><span class="sub">{{ u.correo }}</span> }
            @case ('roles') { <span class="roles">@for (r of u.roles; track r) { <code>{{ r }}</code> }</span> }
            @case ('estado') {
              <span class="chips"><ap-chip-estado [tono]="tono(u)">{{ estado(u) }}</ap-chip-estado>@if (bloqueado(u)) { <ap-chip-estado tono="error">{{ u.bloqueadoHasta ? 'Bloqueado' : 'Bloqueado sin fecha de fin' }}</ap-chip-estado> }</span>
            }
            @case ('tieneSegundoFactor') { <ap-chip-estado [tono]="u.tieneSegundoFactor ? 'ok' : 'neutro'">{{ factor(u) }}</ap-chip-estado> }
            @default { {{ u[c.clave] }} }
          }
        </ng-template>
        <ng-template #acciones let-u>
          <ap-acciones-de-fila [fila]="u.nombres + ' ' + u.apellidos" [ver]="true" (verPulsado)="ver(u)">
            <!-- Cinco acciones en la fila la estiraban a cuatro líneas: Ver queda a la vista y el resto va en «Más». -->
            @if (gestiona() && !deBaja(u)) {
            <details class="mas" (toggle)="ubicarMenu($event)">
              <summary [attr.aria-label]="'Más acciones para ' + u.nombres + ' ' + u.apellidos"><ap-icono nombre="puntos" tamano="s4" />Más</summary>
              <div class="menu">
                @if (u.estado === 'ACTIVO') {
                  <button type="button" apAccionDeFila [attr.aria-label]="'Asignar un rol a ' + u.nombres + ' ' + u.apellidos" (click)="rolDe.set(u)"><ap-icono nombre="escudo" tamano="s4" />Asignar rol</button>
                }
                <button type="button" apAccionDeFila [attr.aria-label]="(u.estado === 'ACTIVO' ? 'Suspender a ' : 'Reactivar a ') + u.nombres + ' ' + u.apellidos" (click)="gestor().cambiarEstado(u)"><ap-icono [nombre]="u.estado === 'ACTIVO' ? 'pausa' : 'reproducir'" tamano="s4" />{{ u.estado === 'ACTIVO' ? 'Suspender' : 'Reactivar' }}</button>
                @if (bloqueado(u)) {
                  <button type="button" apAccionDeFila [attr.aria-label]="'Desbloquear a ' + u.nombres + ' ' + u.apellidos" (click)="gestor().desbloquear(u)"><ap-icono nombre="llave" tamano="s4" />Desbloquear</button>
                }
                <button type="button" apAccionDeFila [attr.aria-label]="'Editar ' + u.nombres + ' ' + u.apellidos" (click)="gestor().editar(u)"><ap-icono nombre="editar" tamano="s4" />Editar</button>
                <button type="button" apAccionDeFila [attr.aria-label]="'Dar de baja a ' + u.nombres + ' ' + u.apellidos" (click)="gestor().darDeBaja(u)"><ap-icono nombre="candado" tamano="s4" />Dar de baja</button>
              </div>
            </details>
            }
          </ap-acciones-de-fila>
        </ng-template>
      </ap-listado>

      @if (hayMas() && carga().tipo === 'lista') {
        <div class="mas-filas">
          <ap-boton variante="secundario" icono="recarga" [deshabilitado]="cargandoMas()" (pulsado)="cargarMas()">{{ cargandoMas() ? 'Cargando…' : 'Cargar más usuarios' }}</ap-boton>
          @if (errorAlCargarMas(); as e) { <p class="error" role="alert">{{ e }}</p> }
          <p class="sub" role="status">Se muestran los primeros {{ cargadas().length }}; hay más. La búsqueda y los filtros trabajan sobre los ya cargados.</p>
        </div>
      }
      <ap-gestor-de-usuario-interno (actualizado)="recargar()" (resumen)="aviso.set($event)" />
      @if (mostrarAlta()) {
        <ap-modal-alta-usuario-interno (cerrar)="mostrarAlta.set(false)" (guardado)="onUsuarioCreado()" />
      }
      @if (rolDe(); as u) {
        <ap-modal-asignar-rol [usuarioId]="u.id" (cerrar)="rolDe.set(null)" (guardado)="onRolAsignado($event)" />
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
    .sub { display: block; font: var(--t-ayuda); color: var(--text-3); overflow-wrap: break-word; }
    .chips { display: inline-flex; flex-wrap: wrap; gap: var(--s1); }
    .mas-filas { display: grid; justify-items: start; gap: var(--s2); }
    .mas-filas .error { margin: 0; font: var(--t-cuerpo-chico); color: var(--err-texto); }
    .mas-filas .sub { margin: 0; }
    .roles { display: flex; flex-wrap: wrap; gap: var(--s1); }
    code { padding: 0 var(--s1); border-radius: var(--r-sm); background: var(--surface-2); font: var(--t-ayuda); font-family: var(--f-mono); color: var(--text-2); }
  `,
})
export class PantallaUsuariosInternos {
  /** Correos y datos del personal: la descarga en el navegador solo con `exportar:usuarios-internos` (regla 90.2). */
  protected readonly exportador = inject(Exportador)
  private readonly router = inject(Router)
  protected readonly gestor = viewChild.required(GestorDeUsuarioInterno)
  protected readonly t = textosSistemas.usuariosInternos
  protected readonly mostrarAlta = signal(false)
  protected readonly rolDe = signal<Usuario | null>(null)
  protected readonly aviso = signal<string | null>(null)
  private readonly sesion = inject(Sesion)
  /** Dar de alta, suspender, desbloquear, baja y asignar: solo a quien el servidor se lo permite (`USUARIOS_INTERNOS_GESTIONAR` o `ACCESOS_ADMINISTRAR`). */
  protected readonly gestiona = computed(() => PERMISOS_DE_GESTION_DE_PERSONAL.some((p) => this.sesion.permisos().includes(p)))
  private readonly acciones = accionesDeAccesos()
  protected readonly cargadas = signal<readonly Usuario[]>([])
  protected readonly cargando = signal(true)
  protected readonly cargandoMas = signal(false)
  protected readonly hayMas = signal(false)
  protected readonly errorAlCargar = signal<{ mensaje: string; trazaId?: string } | null>(null)
  protected readonly errorAlCargarMas = signal<string | null>(null)
  private generacion = 0
  protected readonly carga = computed<CargaDeListado>(() => {
    const e = this.errorAlCargar()
    if (e) return { tipo: 'error', mensaje: e.mensaje, idPeticion: e.trazaId }
    return this.cargando() ? { tipo: 'cargando' } : { tipo: 'lista' }
  })
  private readonly filas = this.cargadas

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
      { etiqueta: this.hayMas() ? 'Usuarios cargados' : 'Usuarios internos', valor: contar(f), icono: 'grupo' },
      { etiqueta: 'Activos', valor: contar(f, (u) => u.estado === 'ACTIVO'), icono: 'verificado', tono: 'ok' },
      { etiqueta: 'Suspendidos o de baja', valor: contar(f, (u) => u.estado === 'SUSPENDIDO' || u.estado === 'BAJA_LABORAL'), icono: 'pausa', tono: 'aviso' },
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
  protected readonly tono = (u: Usuario) => TONO[u.estado] ?? 'neutro'
  protected readonly bloqueado = estaBloqueado
  protected readonly deBaja = estaDeBaja
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

  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
    this.recargar()
  }

  /** Vuelve a la primera página. Con filas a la vista no parpadea a esqueleto: las reemplaza al llegar. */
  protected recargar(): void {
    const mia = ++this.generacion
    this.cargando.set(this.cargadas().length === 0)
    this.errorAlCargar.set(null)
    this.errorAlCargarMas.set(null)
    this.acciones.paginaDeUsuarios(null).subscribe({
      next: (pagina) => {
        if (mia !== this.generacion) return
        this.cargadas.set(pagina)
        this.hayMas.set(pagina.length >= TAMANO_DE_PAGINA_DE_USUARIOS && cursorDespuesDe(pagina) !== null)
        this.cargando.set(false)
      },
      error: (e: unknown) => {
        if (mia !== this.generacion) return
        this.errorAlCargar.set(mensajeDeError(e, 'No pudimos cargar los usuarios internos. Probá de nuevo.'))
        this.cargando.set(false)
      },
    })
  }

  protected cargarMas(): void {
    const cursor: CursorDeUsuarios | null = cursorDespuesDe(this.cargadas())
    if (!cursor || this.cargandoMas()) return
    const mia = this.generacion
    this.cargandoMas.set(true)
    this.errorAlCargarMas.set(null)
    this.acciones.paginaDeUsuarios(cursor).subscribe({
      next: (pagina) => {
        if (mia !== this.generacion) return
        const vistos = new Set(this.cargadas().map((u) => u.id))
        this.cargadas.set([...this.cargadas(), ...pagina.filter((u) => !vistos.has(u.id))])
        this.hayMas.set(pagina.length >= TAMANO_DE_PAGINA_DE_USUARIOS && cursorDespuesDe(pagina) !== null)
        this.cargandoMas.set(false)
      },
      error: (e: unknown) => {
        if (mia !== this.generacion) return
        this.errorAlCargarMas.set(mensajeDeError(e, 'No pudimos cargar más usuarios. Probá de nuevo.').mensaje)
        this.cargandoMas.set(false)
      },
    })
  }

  protected onUsuarioCreado(): void {
    this.mostrarAlta.set(false)
    this.aviso.set('Usuario creado. Tiene una contraseña provisional y debe configurar su segundo factor al ingresar.')
    this.recargar()
  }

  protected onRolAsignado(r: { yaExistia: boolean; requiereMfa: boolean; rolCodigo: string }): void {
    this.rolDe.set(null)
    this.aviso.set(r.yaExistia ? `Esa persona ya tenía el rol ${r.rolCodigo} con ese ámbito: no se cambió nada.` : `Rol ${r.rolCodigo} asignado.${r.requiereMfa ? ' Todavía no tiene segundo factor confirmado: no podrá entrar hasta configurarlo.' : ''}`)
    this.recargar()
  }
}
