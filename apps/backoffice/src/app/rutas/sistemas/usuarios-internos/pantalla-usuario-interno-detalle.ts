import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { ActivatedRoute, RouterLink } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import {
  EntradaActualizarUsuarioInternoEstadoEnum,
  type UsuarioInternoDetalle,
} from 'clientes/angular/identidad'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import {
  actualizadorUsuarioInterno,
  desbloqueadorUsuarioInterno,
  usuarioInterno,
} from '../dominio/usuarios-internos'
import { textosSistemas } from '../textos'
import { ModalAsignarRoles } from './modal-asignar-roles'

@Component({
  selector: 'ap-pantalla-usuario-interno-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, ChipEstado, EstadoDePantalla, Boton, FormsModule, RouterLink, ModalAsignarRoles],
  template: `
    <section class="pantalla">
      <div class="navegacion-superior">
        <a routerLink="/sistemas/usuarios-internos" class="enlace-volver">← Volver a usuarios internos</a>
      </div>

      <ap-estado-de-pantalla [recurso]="recurso" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="recurso.reload()">
        @if (recurso.hasValue() && recurso.value(); as u) {
          <header class="cabecera-usuario">
            <div>
              <ap-cabecera-de-pagina [titulo]="u.nombres + ' ' + u.apellidos" [descripcion]="u.cargo + ' · ' + u.departamento" icono="candado" />
              <p class="correo">{{ u.correo }} · Tel: {{ u.telefono }}</p>
            </div>
            <div class="acciones-cabecera">
              <ap-chip-estado [tono]="u.estado === 'ACTIVO' ? 'ok' : 'aviso'">{{ u.estado }}</ap-chip-estado>
              <ap-boton variante="secundario" (pulsado)="toggleEstado(u)">
                {{ u.estado === 'ACTIVO' ? 'Suspender cuenta' : 'Reactivar cuenta' }}
              </ap-boton>
            </div>
          </header>

          @if (u.bloqueadoHasta) {
            <div class="tarjeta-bloqueo" role="alert">
              <div>
                <strong>Cuenta bloqueada temporalmente por seguridad</strong>
                <p class="subtexto">Motivo: {{ u.motivoBloqueo || 'Múltiples intentos fallidos' }}</p>
              </div>
              <ap-boton variante="primario" (pulsado)="desbloquear(u.id)">Desbloquear ahora</ap-boton>
            </div>
          }

          <div class="cuadricula">
            <article class="tarjeta">
              <h2>Datos de organización</h2>
              <dl>
                <div><dt>Organización</dt><dd>AportaYa</dd></div>
                <div><dt>Código público</dt><dd>{{ u.codigoPublico }}</dd></div>
                <div><dt>Departamento</dt><dd>{{ u.departamento }}</dd></div>
                <div><dt>Cargo</dt><dd>{{ u.cargo }}</dd></div>
                <div><dt>Segundo factor</dt><dd>{{ u.tieneSegundoFactor ? 'Configurado' : 'Pendiente' }}</dd></div>
                <div><dt>Debe cambiar contraseña</dt><dd>{{ u.debeCambiarContrasena ? 'Sí' : 'No' }}</dd></div>
              </dl>
            </article>

            <article class="tarjeta">
              <div class="cabecera-seccion">
                <h2>Roles asignados (CU-08)</h2>
                <ap-boton variante="secundario" (pulsado)="mostrarModalRoles.set(true)">Modificar roles</ap-boton>
              </div>
              <div class="lista-chips">
                @for (r of u.roles; track r) {
                  <span class="chip-rol">{{ r }}</span>
                }
              </div>
            </article>
          </div>

          <article class="tarjeta">
            <h2>Permisos efectivos calculados</h2>
            <p class="subtexto">Permisos resultantes de la suma de los roles vigentes del operador:</p>
            <div class="lista-permisos">
              @for (p of u.permisosEfectivos; track p) {
                <span class="chip-permiso">{{ p }}</span>
              }
            </div>
          </article>

          @if (mostrarModalRoles()) {
            <ap-modal-asignar-roles
              [usuarioId]="u.id"
              [rolesActuales]="u.roles"
              (cerrar)="mostrarModalRoles.set(false)"
              (guardado)="onRolesActualizados()"
            />
          }
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { padding: 0; max-width: 68rem; display: flex; flex-direction: column; gap: var(--s4); }
    .navegacion-superior { margin-bottom: var(--s2); }
    .enlace-volver { color: var(--text-2); text-decoration: none; font-size: .875rem; }
    .enlace-volver:hover { color: var(--text); }
    .cabecera-usuario { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--s4); }
    .correo { margin: var(--s1) 0 0; color: var(--text-2); font-size: .875rem; }
    .acciones-cabecera { display: flex; gap: var(--s2); align-items: center; }
    .tarjeta-bloqueo { display: flex; justify-content: space-between; align-items: center; padding: var(--s3); background: var(--surface-aviso, var(--surface-2)); border: var(--borde-fino) solid var(--borde-aviso, var(--border)); border-radius: var(--r-md); }
    .cuadricula { display: grid; grid-template-columns: 1fr 1fr; gap: var(--s4); }
    .tarjeta { background: var(--surface); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); padding: var(--s4); display: flex; flex-direction: column; gap: var(--s3); }
    .cabecera-seccion { display: flex; justify-content: space-between; align-items: center; }
    h2 { margin: 0; font-size: 1.05rem; font-weight: 600; color: var(--text); }
    dl { display: grid; grid-template-columns: auto 1fr; gap: var(--s2) var(--s4); margin: 0; font-size: .875rem; }
    dt { color: var(--text-2); font-weight: 500; }
    dd { margin: 0; color: var(--text); }
    .lista-chips, .lista-permisos { display: flex; gap: var(--s1); flex-wrap: wrap; }
    .chip-rol { padding: var(--s1); background: var(--surface-2); border: var(--borde-fino) solid var(--border); border-radius: var(--r-sm); font-size: .8125rem; }
    .chip-permiso { padding: var(--s1); background: var(--surface-2); border-radius: var(--r-sm); font-size: .75rem; font-family: var(--fuente-mono, monospace); }
    .subtexto { margin: 0; font-size: .8125rem; color: var(--text-3); }
  `,
})
export class PantallaUsuarioInternoDetalle {
  private readonly route = inject(ActivatedRoute)
  private readonly ejecutarActualizacion = actualizadorUsuarioInterno()
  private readonly ejecutarDesbloqueo = desbloqueadorUsuarioInterno()

  protected readonly t = textosSistemas.usuariosInternos
  protected readonly id = signal<string | null>(this.route.snapshot.paramMap.get('id'))
  protected readonly recurso = usuarioInterno(this.id)
  protected readonly mostrarModalRoles = signal(false)

  protected readonly esVacio = (u: UsuarioInternoDetalle | undefined) => !u

  protected toggleEstado(u: UsuarioInternoDetalle): void {
    const nuevoEstado =
      u.estado === 'ACTIVO'
        ? EntradaActualizarUsuarioInternoEstadoEnum.Suspendido
        : EntradaActualizarUsuarioInternoEstadoEnum.Activo
    const motivo = prompt(`Ingresá el motivo para cambiar el estado a ${nuevoEstado}:`)
    if (!motivo || !motivo.trim()) return

    this.ejecutarActualizacion(u.id, { estado: nuevoEstado, motivo: motivo.trim() }).subscribe(() => {
      this.recurso.reload()
    })
  }

  protected desbloquear(id: string): void {
    const motivo = prompt('Ingresá el motivo del desbloqueo:')
    if (!motivo || !motivo.trim()) return

    this.ejecutarDesbloqueo(id, motivo.trim()).subscribe(() => {
      this.recurso.reload()
    })
  }

  protected onRolesActualizados(): void {
    this.mostrarModalRoles.set(false)
    this.recurso.reload()
  }
}
