import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { restaurarSesion } from './auth-bootstrap'
import { RefrescoDeSesion } from './refresco-de-sesion'
import { Sesion } from './sesion'

/** Ruta interna verdadera: nunca una URL externa que el `volverA` pudiera traer. */
function rutaInternaOTablero(destino: string | null): string {
  return destino && destino.startsWith('/') && !destino.startsWith('//') ? destino : '/tablero'
}

/**
 * Se monta en `/arranque` mientras `Sesion.estado()` es `RESTORING` (arranque normal o un
 * reintento) o `ERROR` (el refresco falló por algo que no es una cookie inválida). Nunca
 * hay login silencioso: mientras el estado es desconocido, esta pantalla es lo único que
 * se ve.
 */
@Component({
  selector: 'ap-restaurando-sesion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton],
  template: `
    @if (esError()) {
      <main>
        <section class="tarjeta" role="alert" aria-live="assertive">
          <h1>No pudimos confirmar tu sesión</h1>
          <p>El servicio de identidad no respondió a tiempo. Tus datos no se perdieron.</p>
          <ap-boton variante="primario" [cargando]="reintentando()" [deshabilitado]="reintentando()" (pulsado)="reintentar()">
            Reintentar
          </ap-boton>
        </section>
      </main>
    } @else {
      <main>
        <section class="tarjeta" aria-busy="true" aria-live="polite">
          <p>Restaurando tu sesión…</p>
        </section>
      </main>
    }
  `,
  styles: `
    main { min-height: 100dvh; display: grid; place-items: center; padding: var(--s5); background: var(--g100); }
    .tarjeta { width: 100%; max-width: 26rem; padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); text-align: center; }
    h1 { margin: 0 0 var(--s3); font-size: 1.2rem; color: var(--text); }
    p { margin: 0 0 var(--s4); color: var(--text-2); }
  `,
})
export class RestaurandoSesion {
  private readonly sesion = inject(Sesion)
  private readonly refresco = inject(RefrescoDeSesion)
  private readonly router = inject(Router)
  private readonly route = inject(ActivatedRoute)

  protected readonly esError = computed(() => this.sesion.estado() === 'ERROR')
  protected readonly reintentando = computed(() => this.sesion.estado() === 'RESTORING')

  protected async reintentar(): Promise<void> {
    await restaurarSesion(this.sesion, this.refresco)
    const volverA = this.route.snapshot.queryParamMap.get('volverA')
    const estado = this.sesion.estado()
    if (estado === 'AUTHENTICATED') {
      void this.router.navigateByUrl(rutaInternaOTablero(volverA))
    } else if (estado === 'ANONYMOUS') {
      void this.router.navigate(['/ingreso'], { queryParams: volverA ? { volverA } : {} })
    }
    // Si sigue en ERROR, esta misma pantalla ya lo muestra: nada más que hacer.
  }
}
