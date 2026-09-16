import { ChangeDetectionStrategy, Component } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Icono } from '../../../layout/icono'
import { Telefono } from './telefono'

@Component({
  selector: 'ap-hero',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono, RouterLink, Telefono],
  host: { style: 'display: contents' },
  template: `
    <section class="hero grano" aria-labelledby="titulo-hero">
      <div class="contenedor hero-in">
        <div class="entrada">
          <div class="eyebrow-marca">
            <span class="ic"><ap-icono nombre="check" [tamano]="12" [grosor]="3" /></span>
            <span>Billetera de pasanaku · Bolivia</span>
          </div>
          <h1 id="titulo-hero">El pasanaku,<br />en tu bolsillo<span class="ya">.</span></h1>
          <p class="bajada">
            Armá tu grupo, aportá desde el celular y recibí tu turno completo. Sin cuadernos que se pierden,
            sin planillas de WhatsApp y sin que alguien tenga que “guardar la plata”.
          </p>
          <div class="acciones">
            <a class="boton boton--principal" routerLink="/descargar">Crear mi grupo</a>
            <a class="boton boton--fantasma" routerLink="/como-funciona">Ver cómo funciona</a>
          </div>
          <div class="confianza">
            <span class="pastilla"><ap-icono nombre="subir" [tamano]="15" [grosor]="2.2" />Recargá y retirá cuando quieras</span>
            <span class="pastilla"><ap-icono nombre="check" [tamano]="15" [grosor]="2.2" />Cada movimiento, comprobable</span>
            <span class="pastilla"><ap-icono nombre="escudo" [tamano]="15" [grosor]="2.2" />Fondo de garantía</span>
          </div>
        </div>
        <ap-telefono />
      </div>
    </section>
    <section class="marco-normativo" aria-label="Marco normativo">
      <div class="contenedor">
        <p class="mn-rotulo">Diseñado contra</p>
        <div class="mn-lista">
          @for (n of normas; track n.sigla) {
            <div class="mn-item"><span class="mn-sigla">{{ n.sigla }}</span><span class="mn-norma">{{ n.norma }}</span></div>
          }
        </div>
      </div>
    </section>
  `,
})
export class Hero {
  protected readonly normas = [
    { sigla: 'ASFI', norma: 'Reglamento ETF · Res. 540/2025' },
    { sigla: 'UIF', norma: 'Instructivo de billetera móvil' },
    { sigla: 'BCB', norma: 'RD 079/2022' },
    { sigla: 'SIN', norma: 'Facturación electrónica' },
    { sigla: 'ISO/IEC 27001', norma: 'Seguridad de la información' },
    { sigla: 'ISO 22301', norma: 'Continuidad del negocio' },
  ]
}
