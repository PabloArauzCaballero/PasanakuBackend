import { ChangeDetectionStrategy, Component } from '@angular/core'

/**
 * Lo que ve una visita cuando `CONFIGURACION_GATEWAY` (ver `gateway.ts`) llegó
 * inválida: nunca la pantalla en blanco, nunca una petición a un host inválido, y
 * nunca la URL rechazada en pantalla. Mismo criterio que `apps/backoffice` (no
 * compartido por package: `packages/ui` no es de este carril).
 */
@Component({
  selector: 'ap-configuracion-invalida',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main role="alert" aria-labelledby="ci-titulo">
      <h1 id="ci-titulo">AportaYa no está disponible ahora mismo</h1>
      <p>Este sitio no pudo arrancar correctamente. No se hizo ninguna petición a ningún servidor.</p>
      <p>Probá de nuevo en unos minutos. Si seguís viendo esto, es un problema de nuestro lado, no de tu conexión.</p>
    </main>
  `,
  styles: `
    :host { display: block; min-height: 100dvh; display: grid; place-items: center; padding: var(--s5); }
    main { max-width: 32rem; text-align: center; }
    h1 { margin-bottom: var(--s4); }
    p { color: var(--text-2); margin: 0 0 var(--s3); }
  `,
})
export class ConfiguracionInvalida {}
