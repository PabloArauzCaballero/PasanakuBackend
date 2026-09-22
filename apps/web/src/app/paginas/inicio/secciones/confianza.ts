import { ChangeDetectionStrategy, Component } from '@angular/core'
import { Icono } from '../../../layout/icono'
import { NombreDeTrazo } from '../../../layout/trazos'

/** Cuando algo sale mal, y la seguridad: por qué se puede confiar la plata. */
@Component({
  selector: 'ap-confianza',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { style: 'display: contents' },
  template: `
    <section class="seccion" aria-labelledby="titulo-mal">
      <div class="contenedor">
        <div class="cabecera">
          <span class="etiqueta">Cuando algo sale mal</span>
          <h2 id="titulo-mal">Nadie queda afuera sin poder decir lo suyo.</h2>
          <p>En un pasanaku, el problema no es el software: es qué pasa cuando alguien no aporta. Está resuelto con procedimiento, no con improvisación.</p>
        </div>
        <ol class="mini-flujo" aria-label="El procedimiento, en orden">
          @for (paso of procedimiento; track paso; let ultimo = $last) {
            <li class="mf-paso" [class.mf-paso--final]="ultimo">{{ paso }}</li>
            @if (!ultimo) { <li class="mf-flecha" aria-hidden="true"><ap-icono nombre="flecha" [tamano]="15" /></li> }
          }
        </ol>
        <div class="rejilla rejilla--3">
          @for (c of casos; track c.titulo) {
            <article class="tarjeta">
              <span class="insignia" [class]="'insignia insignia--' + c.tono">{{ c.insignia }}</span>
              <h3>{{ c.titulo }}</h3>
              <p>{{ c.texto }}</p>
            </article>
          }
        </div>
      </div>
    </section>

    <section class="seccion seccion--tenue" id="seguridad" aria-labelledby="titulo-seguridad">
      <div class="contenedor">
        <div class="cabecera">
          <span class="etiqueta">Seguridad y cumplimiento</span>
          <h2 id="titulo-seguridad">Cercanos al hablar, impecables con la plata.</h2>
          <p>Manejar dinero de terceros tiene reglas. Las tomamos como requisito de diseño, no como un trámite posterior.</p>
        </div>
        <div class="rejilla rejilla--4">
          @for (s of seguridad; track s.titulo) {
            <article class="tarjeta">
              <div class="icono"><ap-icono [nombre]="s.icono" [tamano]="22" /></div>
              <h3>{{ s.titulo }}</h3>
              <p>{{ s.texto }}</p>
            </article>
          }
        </div>
        <div class="aviso" role="note">
          <span class="signo"><ap-icono nombre="alerta" [tamano]="17" [grosor]="2.2" /></span>
          <div>
            <div class="t">AportaYa está en construcción</div>
            <div class="m">Operar con dinero electrónico del público requiere autorización previa de ASFI. Hasta obtenerla, la plataforma no custodia fondos de terceros y lo que ves acá es el producto en desarrollo, no un servicio financiero disponible.</div>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class Confianza {
  protected readonly procedimiento = ['Aviso y plazo', 'Descargo', 'Decisión motivada', 'Apelación']
  protected readonly casos = [
    { insignia: 'Mora', tono: 'aviso', titulo: 'Primero el recordatorio', texto: 'Antes de declarar nada, el sistema avisa y da plazo. Los días son hábiles y se calculan una sola vez, al inicio: no se recalculan para que te queden peor.' },
    { insignia: 'Descargo', tono: 'neutral', titulo: 'Tu versión cuenta', texto: 'Un incumplimiento es un expediente, no una banderita roja: causal escrita, notificación probada, descargo con evidencia y decisión motivada. Y una apelación, resuelta por otra persona.' },
    { insignia: 'Reputación', tono: 'ok', titulo: 'Un historial que se explica', texto: 'Tu puntaje se calcula con criterios publicados y se puede reproducir. Si mejorás, sube; si prescribe, deja de pesar. Nada de castigos por pronóstico.' },
  ]
  protected readonly seguridad: { icono: NombreDeTrazo; titulo: string; texto: string }[] = [
    { icono: 'escudoCheck', titulo: 'Doble factor', texto: 'Segundo factor y dispositivo de confianza para entrar y para cada operación sensible. El PIN nunca se guarda en claro.' },
    { icono: 'candadoPunto', titulo: 'Cada dato, su dueño', texto: 'La base decide qué puede ver cada sesión. Si un permiso falla, la consulta no devuelve de más: no devuelve nada.' },
    { icono: 'documento', titulo: 'Contra la norma', texto: 'Diseñado contra el reglamento de ASFI, el instructivo de la UIF, la normativa del BCB y la facturación del SIN, requisito por requisito.' },
    { icono: 'reloj', titulo: 'Respaldos probados', texto: 'Respaldo cifrado con recuperación a un punto en el tiempo, y ensayo de restauración: un backup que nunca se restauró no es un backup.' },
  ]
}
