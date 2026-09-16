import { ChangeDetectionStrategy, Component } from '@angular/core'
import { Icono } from '../../../layout/icono'

/** La transparencia verificable: qué se promete y cómo se ve un historial sellado. */
@Component({
  selector: 'ap-transparencia-inicio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { style: 'display: contents' },
  template: `
    <section class="seccion seccion--tenue" id="transparencia" aria-labelledby="titulo-transparencia">
      <div class="contenedor dupla">
        <div>
          <span class="etiqueta">Transparencia verificable</span>
          <h2 id="titulo-transparencia">Comprobalo. No hace falta que nos creas.</h2>
          <p class="texto-guia">En el pasanaku de cuaderno, todo depende de que una persona anote bien y no se le pierda la hoja. Acá cada período queda registrado, a la vista de todos y sin que nadie —nosotros incluidos— pueda cambiarlo después.</p>
          <ul class="lista-marcas">
            @for (p of promesas; track p.titulo) {
              <li><span class="tic"><ap-icono nombre="check" [tamano]="12" [grosor]="3" /></span><span><b>{{ p.titulo }}</b> {{ p.texto }}</span></li>
            }
          </ul>
        </div>
        <div>
          <div class="historial">
            <div class="historial-cab"><span class="tt">Historial de Las Comadres</span><span class="sub">Bs 250 por período · 10 cupos</span></div>
            @for (p of periodos; track p.mes) {
              <div class="periodo">
                <span class="mes">{{ p.mes }}</span>
                <span class="estado"><span class="insignia insignia--ok"><span class="punto"></span>Sellado</span></span>
                <span class="detalle">Aportaron <b>{{ p.aportaron }} de 10</b>{{ p.nota }} · recibió <b>{{ p.recibio }}</b> <span class="monto monto--sm"><span class="moneda">Bs</span>2.500,00</span></span>
                <span class="cupos" role="img" [attr.aria-label]="p.aportaron + ' de 10 cupos aportaron'">
                  @for (c of diez; track $index) { <i [class.falta]="$index >= p.aportaron"></i> }
                </span>
              </div>
            }
            <div class="historial-pie">
              <p class="garantia"><ap-icono nombre="escudoCheck" [tamano]="17" [grosor]="2.2" /><span>Una vez sellado un período, <b>nadie lo puede modificar</b> — ni el organizador, ni nosotros. Y no tenés que creernos: podés comprobarlo por tu cuenta.</span></p>
              <details class="tecnico">
                <summary>Ver la comprobación técnica</summary>
                <div class="cuerpo">
                  <div class="fila"><span class="k">agosto</span><span class="v">e8a0d5c3…31f7</span></div>
                  <div class="fila"><span class="k">viene de</span><span class="v">c204af71…5be8</span></div>
                  <div class="fila"><span class="k">julio</span><span class="v">c204af71…5be8</span></div>
                  <div class="fila"><span class="k">viene de</span><span class="v">7f3c9e2b…a91d</span></div>
                </div>
                <p class="nota">Cada período se sella con una huella que incluye la del período anterior. Cambiar un solo dato de junio rompe la huella de julio y la de agosto, y el sistema señala exactamente dónde.</p>
              </details>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class TransparenciaInicio {
  protected readonly diez = Array.from({ length: 10 })
  protected readonly promesas = [
    { titulo: 'El sorteo no se puede arreglar.', texto: 'El orden de los turnos queda fijado antes de conocerse, y después se publica cómo salió. Un solo sorteo por grupo.' },
    { titulo: 'Lo que ya pasó, no se toca.', texto: 'Cuando un período cierra, queda sellado. Modificar un dato viejo rompe el sello de todos los períodos siguientes.' },
    { titulo: 'Si algo no cuadra, te lo decimos.', texto: 'El sistema revisa la historia del grupo todos los días y avisa exactamente qué período falla.' },
    { titulo: 'Podés comprobarlo sin nosotros.', texto: 'Publicamos cómo se hace la cuenta y te llevás el historial completo en un archivo.' },
  ]
  protected readonly periodos = [
    { mes: 'Junio', aportaron: 10, nota: '', recibio: 'Rocío G.' },
    { mes: 'Julio', aportaron: 9, nota: ' · el fondo de garantía cubrió el que faltó', recibio: 'Juan C.' },
    { mes: 'Agosto', aportaron: 10, nota: '', recibio: 'Marisol C.' },
  ]
}
