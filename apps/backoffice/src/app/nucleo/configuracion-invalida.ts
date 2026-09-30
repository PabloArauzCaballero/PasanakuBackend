import { ChangeDetectionStrategy, Component } from '@angular/core'

/**
 * Lo que ve un operador cuando `CONFIGURACION_GATEWAY` (ver `gateway.ts`) llegó
 * inválida: nunca la pantalla en blanco, nunca una petición a un host inválido, y
 * nunca la URL rechazada en pantalla — mostrarla no ayuda a quien opera (no puede
 * arreglar el despliegue desde el navegador) y sí ayuda a quien está mirando por
 * encima del hombro. El mensaje es accionable: a quién avisar, no qué URL falló.
 */
@Component({
  selector: 'ap-configuracion-invalida',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main aria-labelledby="ci-titulo">
      <!-- "alert" no es un rol permitido en <main> (axe: aria-allowed-role) — <main> se
           queda como landmark simple y el div interno lleva la semántica de alerta. -->
      <div role="alert">
        <h1 id="ci-titulo">El backoffice no puede arrancar</h1>
        <p>La configuración de este despliegue no es válida. No se hizo ninguna petición a ningún servidor.</p>
        <p>Avisá a la persona de guardia de infraestructura (Leo, plataforma) con la hora y el ambiente. No hay nada para intentar desde este navegador.</p>
      </div>
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
