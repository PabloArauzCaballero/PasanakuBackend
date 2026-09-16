import { ChangeDetectionStrategy, Component } from '@angular/core'
import { Cierre } from './secciones/cierre'
import { Confianza } from './secciones/confianza'
import { Hero } from './secciones/hero'
import { Producto } from './secciones/producto'
import { TransparenciaInicio } from './secciones/transparencia'

/**
 * El inicio del sitio: la estética y el recorrido de la landing, sobre los tokens y las
 * fuentes de la app. Se prerenderiza y no carga ningún cliente de API. Todo el contenido
 * está en el HTML desde el servidor: nada aparece recién al hacer scroll.
 */
@Component({
  selector: 'ap-inicio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Hero, Producto, TransparenciaInicio, Confianza, Cierre],
  template: `
    <main id="contenido">
      <ap-hero />
      <ap-producto />
      <ap-transparencia-inicio />
      <ap-confianza />
      <ap-cierre />
    </main>
  `,
})
export class Inicio {}
