import { ChangeDetectionStrategy, Component } from '@angular/core'
import { RouterLink } from '@angular/router'

/** Lo que hay debajo, las preguntas y el llamado final. */
@Component({
  selector: 'ap-cierre',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  host: { style: 'display: contents' },
  template: `
    <section class="seccion seccion--oscura grano" aria-labelledby="titulo-debajo">
      <div class="orbe orbe--verde orbe--oscura-verde" aria-hidden="true"></div>
      <div class="orbe orbe--naranja orbe--oscura-naranja" aria-hidden="true"></div>
      <div class="contenedor sobre-orbes">
        <div class="cabecera cabecera--centrada">
          <span class="etiqueta">Lo que hay debajo</span>
          <h2 id="titulo-debajo">Esto no se diseñó a ojo.</h2>
          <p>Antes de la primera pantalla se escribió la especificación completa: cada flujo, cada regla y cada dato, con las restricciones que hacen imposible violarlas desde la base.</p>
        </div>
        <dl class="rejilla rejilla--4">
          @for (c of cifras; track c.clave) {
            <div class="cifra cifra--oscura"><dt class="k">{{ c.clave }}</dt><dd class="v">{{ c.valor }}</dd></div>
          }
        </dl>
      </div>
    </section>

    <section class="seccion seccion--tenue" id="preguntas" aria-labelledby="titulo-preguntas">
      <div class="contenedor contenedor--angosto">
        <div class="cabecera cabecera--centrada">
          <span class="etiqueta">Preguntas frecuentes</span>
          <h2 id="titulo-preguntas">Lo que todos preguntan primero.</h2>
        </div>
        @for (p of preguntas; track p.q) {
          <details class="pregunta">
            <summary>{{ p.q }}</summary>
            <div class="respuesta"><p>{{ p.a }}</p></div>
          </details>
        }
        <p class="ver-mas"><a routerLink="/preguntas">Ver todas las preguntas</a></p>
      </div>
    </section>

    <section class="seccion" id="empezar" aria-labelledby="titulo-empezar">
      <div class="contenedor">
        <div class="banda">
          <h2 id="titulo-empezar">¿Armamos tu primer pasanaku?</h2>
          <p>Dejanos tu contacto y te avisamos apenas abramos los primeros grupos. Sin compromiso y sin letra chica.</p>
          <div class="acciones">
            <a class="boton boton--principal" href="mailto:hola@aportaya.bo?subject=Quiero%20crear%20mi%20grupo">Quiero mi cupo</a>
            <a class="boton boton--claro" href="mailto:hola@aportaya.bo?subject=Consulta%20sobre%20AportaYa">Hablar con el equipo</a>
          </div>
          <p class="nota">Juntos ahorramos mejor.</p>
        </div>
      </div>
    </section>
  `,
})
export class Cierre {
  // Medidas del repositorio el 2026-09-16, no cifras de marketing: docs/CasosDeUso, sql/10_tablas,
  // sql/40_reglas/restricciones.sql y servicios/.
  protected readonly cifras = [
    { valor: '99', clave: 'casos de uso especificados' },
    { valor: '305', clave: 'tablas en el modelo' },
    { valor: '181', clave: 'reglas de negocio en la base' },
    { valor: '14', clave: 'servicios independientes' },
  ]
  protected readonly preguntas = [
    { q: '¿Qué es un pasanaku?', a: 'Es el ahorro rotativo de toda la vida: un grupo aporta la misma cantidad cada período y en cada vuelta el fondo completo va para uno, hasta que a todos les tocó.' },
    { q: '¿Puedo sacar mi plata cuando quiera?', a: 'Tu saldo disponible es tuyo y lo retirás cuando quieras a tu cuenta bancaria. Lo que ya aportaste a un grupo es del fondo de ese período y le corresponde a quien tiene el turno.' },
    { q: '¿Qué pasa si alguien no aporta?', a: 'Primero se le avisa y se le da plazo. Si igual no aporta, el fondo de garantía cubre la entrega para que quien tenía el turno cobre igual, y la deuda queda a nombre de quien incumplió.' },
    { q: '¿Está autorizado por ASFI?', a: 'Todavía no, y no lo vamos a disimular. Operar con dinero real exige la autorización previa de ASFI. Hasta tenerla, AportaYa no custodia fondos del público.' },
  ]
}
