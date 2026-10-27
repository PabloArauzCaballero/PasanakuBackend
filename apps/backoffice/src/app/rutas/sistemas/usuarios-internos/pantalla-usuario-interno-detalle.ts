import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Migas } from '@aportaya/ui/migas/migas'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import type { UsuarioInternoDetalle } from 'clientes/angular/identidad'
import { usuarioInterno } from '../dominio/usuarios-internos'
import { textosSistemas } from '../textos'
import { GestorDeUsuarioInterno } from './gestor-de-usuario-interno'
import { ModalAsignarRoles } from './modal-asignar-roles'

/**
 * Ficha de un usuario interno (GET `/usuarios-internos/{id}`) en **una** tarjeta a todo el ancho: datos, roles y
 * permisos efectivos. Las acciones de la cabecera son las que el contrato tiene: Editar, Roles, Suspender/Reactivar y
 * Desbloquear (si está bloqueado). Antes Suspender y Desbloquear usaban `prompt()` y no mostraban nada si el servidor
 * rechazaba; ahora pasan por `ap-gestor-de-usuario-interno`, que deja el error a la vista.
 */
@Component({
  selector: 'ap-pantalla-usuario-interno-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, ChipEstado, EstadoDePantalla, GestorDeUsuarioInterno, Migas, ModalAsignarRoles, SeccionClaveValor],
  template: `
    <section class="pantalla">
      <ap-migas [migas]="[{ texto: 'Usuarios internos', ruta: '/sistemas/usuarios-internos' }, { texto: titulo() }]" />
      <ap-estado-de-pantalla [recurso]="recurso" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="recurso.reload()">
        @if (recurso.hasValue() && recurso.value(); as u) {
          <ap-cabecera-de-pagina [eyebrow]="t.detalle" [titulo]="u.nombres + ' ' + u.apellidos" [descripcion]="u.cargo + ' · ' + u.departamento" icono="persona">
            <ap-boton acciones icono="editar" (pulsado)="gestor().editar(u)">{{ t.editarUsuario }}</ap-boton>
            <ap-boton acciones icono="escudo" (pulsado)="mostrarModalRoles.set(true)">Modificar roles</ap-boton>
            <ap-boton acciones [icono]="u.estado === 'ACTIVO' ? 'pausa' : 'reproducir'" [variante]="u.estado === 'ACTIVO' ? 'peligro' : 'secundario'" (pulsado)="gestor().cambiarEstado(u)">{{ u.estado === 'ACTIVO' ? 'Suspender cuenta' : 'Reactivar cuenta' }}</ap-boton>
          </ap-cabecera-de-pagina>

          @if (u.bloqueadoHasta) {
            <ap-alerta tono="aviso" titulo="Cuenta bloqueada temporalmente por seguridad">
              Motivo: {{ u.motivoBloqueo || 'Múltiples intentos fallidos' }} · hasta {{ u.bloqueadoHasta }}.
              <ap-boton variante="primario" icono="llave" tamano="sm" (pulsado)="gestor().desbloquear(u)">Desbloquear ahora</ap-boton>
            </ap-alerta>
          }

          <article class="ficha">
            <div class="estados">
              <ap-chip-estado [tono]="u.estado === 'ACTIVO' ? 'ok' : 'aviso'">{{ u.estado }}</ap-chip-estado>
              <ap-chip-estado [tono]="u.tieneSegundoFactor ? 'ok' : 'error'">Segundo factor {{ u.tieneSegundoFactor ? 'configurado' : 'pendiente' }}</ap-chip-estado>
            </div>
            <ap-seccion-clave-valor titulo="Datos de organización" [items]="datos()" />
            <section aria-labelledby="titulo-roles">
              <h2 id="titulo-roles">{{ t.rolesAsignados }} (CU-08)</h2>
              <p class="chips">@for (r of u.roles; track r) { <code>{{ r }}</code> } @empty { <span class="sub">Sin roles asignados.</span> }</p>
            </section>
            <section aria-labelledby="titulo-permisos">
              <h2 id="titulo-permisos">{{ t.permisosEfectivos }}</h2>
              <p class="sub">La suma de los permisos de los roles vigentes ({{ u.permisosEfectivos.length }}).</p>
              <p class="chips">@for (p of u.permisosEfectivos; track p) { <code>{{ p }}</code> }</p>
            </section>
          </article>

          @if (mostrarModalRoles()) {
            <ap-modal-asignar-roles [usuarioId]="u.id" [rolesActuales]="u.roles" (cerrar)="mostrarModalRoles.set(false)" (guardado)="onRolesActualizados()" />
          }
        }
      </ap-estado-de-pantalla>
      <ap-gestor-de-usuario-interno (actualizado)="recurso.reload()" />
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
  `,
})
export class PantallaUsuarioInternoDetalle {
  private readonly route = inject(ActivatedRoute)
  protected readonly gestor = viewChild.required(GestorDeUsuarioInterno)
  protected readonly t = textosSistemas.usuariosInternos
  protected readonly id = signal<string | null>(this.route.snapshot.paramMap.get('id'))
  protected readonly recurso = usuarioInterno(this.id)
  protected readonly mostrarModalRoles = signal(false)
  protected readonly esVacio = (u: UsuarioInternoDetalle | undefined) => !u

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

  protected onRolesActualizados(): void {
    this.mostrarModalRoles.set(false)
    this.recurso.reload()
  }
}
