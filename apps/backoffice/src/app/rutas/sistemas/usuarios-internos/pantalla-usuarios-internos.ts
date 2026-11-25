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
import { ESTADO, TONO, estaBloqueado, estaDeBaja, estadoDe, segundoFactor } from './estado-del-usuario'
import { GestorDeUsuarioInterno } from './gestor-de-usuario-interno'
import { ModalAltaUsuarioInterno } from './modal-alta-usuario-interno'
import { ModalAsignarRol } from './modal-asignar-rol'
import { Exportador } from '../../../nucleo/exportador'

type Usuario = UsuarioInternoDeLista

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
  templateUrl: './pantalla-usuarios-internos.html',
  host: { '(document:click)': 'cerrarMenus($event)', '(document:keydown.escape)': 'cerrarMenus()', '(window:resize)': 'cerrarMenus()', '(window:scroll)': 'cerrarMenus()' },
  styleUrl: './pantalla-usuarios-internos.css',
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

  protected readonly identidad = (u: Usuario) => u.id
  protected readonly listado = crearListado<Usuario>({
    filas: this.cargadas,
    camposDeBusqueda: ['nombres', 'apellidos', 'correo', 'codigoPublico', 'departamento', 'cargo', (u) => u.roles.join(' ')],
    filtros: [
      { nombre: 'estado', etiqueta: 'Estado', valor: (u) => u.estado, texto: (v) => ESTADO[v] ?? v },
      { nombre: 'departamento', etiqueta: 'Departamento', valor: (u) => u.departamento },
      { nombre: 'segundoFactor', etiqueta: 'Segundo factor', valor: segundoFactor },
    ],
    porPagina: 10,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.cargadas()
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
    this.cargandoMas.set(false)
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
