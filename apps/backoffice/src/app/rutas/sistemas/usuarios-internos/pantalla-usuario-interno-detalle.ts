import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core'
import { ActivatedRoute, RouterLink } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Migas } from '@aportaya/ui/migas/migas'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import { Sesion } from '../../../nucleo/sesion'
import type { SalidaAsignacionDeRol, UsuarioInternoFicha } from '../dominio/gobierno-de-accesos'
import { usuarioInterno } from '../dominio/usuarios-internos'
import { PERMISOS_DE_GESTION_DE_PERSONAL } from '../personal-y-accesos'
import { textosSistemas } from '../textos'
import { GestorDeUsuarioInterno } from './gestor-de-usuario-interno'
import { ModalAsignarRol } from './modal-asignar-rol'

/**
 * Ficha de un usuario interno (GET `/usuarios-internos/{id}`) en **una** tarjeta a todo el ancho: datos, roles y
 * permisos efectivos. Las acciones de la cabecera son las que el contrato tiene: Editar, Roles, Suspender/Reactivar y
 * Desbloquear (si está bloqueado). Antes Suspender y Desbloquear usaban `prompt()` y no mostraban nada si el servidor
 * rechazaba; ahora pasan por `ap-gestor-de-usuario-interno`, que deja el error a la vista.
 */
@Component({
  selector: 'ap-pantalla-usuario-interno-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, ChipEstado, EstadoDePantalla, GestorDeUsuarioInterno, Migas, ModalAsignarRol, RouterLink, SeccionClaveValor],
  template: `
    <section class="pantalla">
      <ap-migas [migas]="[{ texto: 'Usuarios internos', ruta: '/sistemas/usuarios-internos' }, { texto: titulo() }]" />
      <ap-estado-de-pantalla [recurso]="recurso" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="recurso.reload()">
        @if (recurso.hasValue() && recurso.value(); as u) {
          <ap-cabecera-de-pagina [eyebrow]="t.detalle" [titulo]="u.nombres + ' ' + u.apellidos" [descripcion]="u.cargo + ' · ' + u.departamento">
            @if (gestiona() && !deBaja(u)) {
              <ng-container acciones>
              <ap-boton icono="editar" (pulsado)="gestor().editar(u)">{{ t.editarUsuario }}</ap-boton>
              @if (u.estado === 'ACTIVO') { <ap-boton icono="escudo" (pulsado)="mostrarModalRol.set(true)">Asignar rol</ap-boton> }
              <ap-boton [icono]="u.estado === 'ACTIVO' ? 'pausa' : 'reproducir'" [variante]="u.estado === 'ACTIVO' ? 'peligro' : 'secundario'" (pulsado)="gestor().cambiarEstado(u)">{{ u.estado === 'ACTIVO' ? 'Suspender cuenta' : 'Reactivar cuenta' }}</ap-boton>
              <ap-boton icono="candado" variante="peligro" (pulsado)="gestor().darDeBaja(u)">Dar de baja</ap-boton>
              </ng-container>
            }
          </ap-cabecera-de-pagina>

          @if (aviso(); as texto) { <ap-alerta tono="ok" titulo="Listo">{{ texto }}</ap-alerta> }
          @if (deBaja(u)) {
            <ap-alerta tono="neutro" titulo="Baja laboral">La cuenta está dada de baja: sin roles, sin sesiones y sin acceso. Se conserva el historial de quién le otorgó qué.</ap-alerta>
          }
          @if (u.bloqueadoHasta || u.motivoBloqueo) {
            <ap-alerta tono="aviso" [titulo]="u.bloqueadoHasta ? 'Cuenta bloqueada por seguridad' : 'Cuenta bloqueada sin fecha de fin'">
              Motivo: {{ u.motivoBloqueo || 'Múltiples intentos fallidos' }}@if (u.bloqueadoHasta) { · hasta {{ u.bloqueadoHasta }} } @else { · el bloqueo no vence solo: hay que desbloquearla }.
              @if (gestiona() && !deBaja(u)) { <ap-boton variante="primario" icono="llave" tamano="sm" (pulsado)="gestor().desbloquear(u)">Desbloquear ahora</ap-boton> }
            </ap-alerta>
          }

          <article class="ficha">
            <div class="estados">
              <ap-chip-estado [tono]="u.estado === 'ACTIVO' ? 'ok' : u.estado === 'BAJA_LABORAL' ? 'neutro' : 'aviso'">{{ etiquetaDeEstado(u.estado) }}</ap-chip-estado>
              <ap-chip-estado [tono]="u.tieneSegundoFactor ? 'ok' : 'error'">Segundo factor {{ u.tieneSegundoFactor ? 'configurado' : 'pendiente' }}</ap-chip-estado>
            </div>
            <ap-seccion-clave-valor titulo="Datos de organización" [items]="datos()" />
            <section aria-labelledby="titulo-roles">
              <h2 id="titulo-roles">{{ t.rolesAsignados }}</h2>
              <p class="chips">@for (r of u.roles; track r) { <code>{{ r }}</code> } @empty { <span class="sub">Sin roles asignados.</span> }</p>
              <p class="sub">Para revocar un rol puntual, andá a <a routerLink="/sistemas/accesos">Revisión de accesos</a>.</p>
            </section>
            <section aria-labelledby="titulo-permisos">
              <h2 id="titulo-permisos">{{ t.permisosEfectivos }}</h2>
              <p class="sub">La suma de los permisos de los roles vigentes ({{ u.permisosEfectivos.length }}).</p>
              <p class="chips">@for (p of u.permisosEfectivos; track p) { <code>{{ p }}</code> }</p>
            </section>
          </article>

          @if (mostrarModalRol()) {
            <ap-modal-asignar-rol [usuarioId]="u.id" (cerrar)="mostrarModalRol.set(false)" (guardado)="onRolAsignado($event)" />
          }
        }
      </ap-estado-de-pantalla>
      <ap-gestor-de-usuario-interno (actualizado)="recurso.reload()" (resumen)="aviso.set($event)" />
    </section>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    .ficha { display: grid; gap: var(--s5); padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); min-width: 0; }
    .estados { display: flex; flex-wrap: wrap; gap: var(--s2); }
    h2 { margin: 0 0 var(--s2); font: var(--t-titulo-3); color: var(--text); }
    .chips { display: flex; flex-wrap: wrap; gap: var(--s1); margin: 0; }
    code { padding: var(--s1) var(--s2); border-radius: var(--r-sm); background: var(--surface-2); font: var(--t-ayuda); font-family: var(--f-mono); color: var(--text-2); overflow-wrap: anywhere; }
    .sub { margin: 0 0 var(--s2); font: var(--t-ayuda); color: var(--text-3); }
    ap-alerta ap-boton { display: inline-block; margin-top: var(--s2); }
    a { color: var(--link, var(--brand)); }
  `,
})
export class PantallaUsuarioInternoDetalle {
  private readonly route = inject(ActivatedRoute)
  protected readonly gestor = viewChild.required(GestorDeUsuarioInterno)
  protected readonly t = textosSistemas.usuariosInternos
  protected readonly id = signal<string | null>(this.route.snapshot.paramMap.get('id'))
  protected readonly recurso = usuarioInterno(this.id)
  protected readonly mostrarModalRol = signal(false)
  protected readonly aviso = signal<string | null>(null)
  private readonly sesion = inject(Sesion)
  protected readonly gestiona = computed(() => PERMISOS_DE_GESTION_DE_PERSONAL.some((p) => this.sesion.permisos().includes(p)))
  protected readonly deBaja = (u: UsuarioInternoFicha) => u.estado === 'BAJA_LABORAL'
  protected readonly esVacio = (u: UsuarioInternoFicha | undefined) => !u
  protected readonly etiquetaDeEstado = (estado: string) => ({ ACTIVO: 'Activo', SUSPENDIDO: 'Suspendido', BAJA_LABORAL: 'Baja laboral' })[estado] ?? estado

  protected readonly titulo = computed(() => {
    const u = this.recurso.hasValue() ? this.recurso.value() : undefined
    return u ? `${u.nombres} ${u.apellidos}` : this.t.detalle
  })
  protected readonly datos = computed<ClaveValor[]>(() => {
    const u = this.recurso.hasValue() ? this.recurso.value() : undefined
    if (!u) return []
    return [
      { etiqueta: 'Código público', valor: u.codigoPublico, mono: true },
      { etiqueta: 'Correo', valor: u.correo },
      { etiqueta: 'Teléfono', valor: u.telefono },
      { etiqueta: 'Departamento', valor: u.departamento },
      { etiqueta: 'Cargo', valor: u.cargo },
      { etiqueta: 'Debe cambiar contraseña', valor: u.debeCambiarContrasena },
      { etiqueta: 'Alta', valor: u.creadoEn },
      { etiqueta: 'Última actualización', valor: u.actualizadoEn },
    ]
  })

  protected onRolAsignado(r: SalidaAsignacionDeRol): void {
    this.mostrarModalRol.set(false)
    this.aviso.set(r.yaExistia ? `Ya tenía el rol ${r.rolCodigo} con ese ámbito: no se cambió nada.` : `Rol ${r.rolCodigo} asignado.${r.requiereMfa ? ' Todavía no tiene segundo factor confirmado: no podrá entrar hasta configurarlo.' : ''}`)
    this.recurso.reload()
  }
}
