import { ChangeDetectionStrategy, Component } from '@angular/core'
import { Icono } from '../../../layout/icono'

/**
 * La pantalla de inicio de la billetera, dibujada en HTML: la misma que muestra la app.
 * Es una ilustración (`role="img"` con su descripción): los montos son de ejemplo y
 * ningún lector de pantalla los lee como datos.
 */
@Component({
  selector: 'ap-telefono',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { class: 'figura' },
  template: `
    <div class="orbe orbe--verde orbe--hero-verde" aria-hidden="true"></div>
    <div class="orbe orbe--naranja orbe--hero-naranja" aria-hidden="true"></div>
    <div class="telefono" role="img" aria-label="Pantalla de inicio de AportaYa: saldo disponible de 1.240 bolivianos, el grupo Las Comadres al día y los últimos movimientos.">
      <div class="flotante flotante--turno" aria-hidden="true">
        <span class="ic ic--ok"><ap-icono nombre="check" [tamano]="15" [grosor]="2.6" /></span>
        <span class="txt"><span class="tt">Aporte confirmado</span><span class="st">Las Comadres · hoy</span></span>
      </div>
      <div class="flotante flotante--cadena" aria-hidden="true">
        <span class="ic ic--naranja"><ap-icono nombre="cadena" [tamano]="15" [grosor]="2.2" /></span>
        <span class="txt"><span class="tt">Cadena íntegra</span><span class="st">12 bloques verificados</span></span>
      </div>
      <div class="pantalla" aria-hidden="true">
        <div class="cabecera-verde">
          <div class="estado-barra"><span class="hora">9:41</span></div>
          <div class="saludo">Hola, Marisol</div>
          <div class="rotulo">Saldo disponible</div>
          <div class="monto saldo"><span class="moneda">Bs</span>1.240,00</div>
          <div class="acciones-rapidas">
            <span class="b b--accion"><ap-icono nombre="arriba" [tamano]="15" [grosor]="2.4" />Recargar</span>
            <span class="b b--fantasma"><ap-icono nombre="abajo" [tamano]="15" [grosor]="2.4" />Retirar</span>
          </div>
        </div>
        <div class="cuerpo-app">
          <div class="grupo-app">
            <span class="avatar avatar--verde">LC</span>
            <span class="info">
              <span class="nm">Las Comadres</span>
              <span class="sub">Turno 4 de 10 · aportás el 15</span>
              <span class="progreso"><i class="progreso--40"></i></span>
            </span>
            <span class="insignia insignia--ok"><span class="punto"></span>Al día</span>
          </div>
          <div class="rotulo-lista">Últimos movimientos</div>
          @for (m of movimientos; track m.nombre) {
            <div class="movimiento">
              <span class="ic" [class.ic--sale]="!m.entra" [class.ic--entra]="m.entra">
                <ap-icono [nombre]="m.entra ? 'entra' : 'sale'" [tamano]="14" [grosor]="2.6" />
              </span>
              <span class="txt"><span class="tn">{{ m.nombre }}</span><span class="td">{{ m.cuando }}</span></span>
              <span class="monto" [class.monto--entra]="m.entra"><span class="moneda">Bs</span>{{ m.importe }}</span>
            </div>
          }
        </div>
        <div class="tabbar">
          <span class="on"><ap-icono nombre="casa" [tamano]="19" [grosor]="1.9" />Inicio</span>
          <span><ap-icono nombre="personas" [tamano]="19" [grosor]="1.9" />Grupos</span>
          <span><ap-icono nombre="flechas" [tamano]="19" [grosor]="1.9" />Movimientos</span>
          <span><ap-icono nombre="persona" [tamano]="19" [grosor]="1.9" />Perfil</span>
        </div>
      </div>
    </div>
  `,
})
export class Telefono {
  // Importes de EJEMPLO ya escritos como texto de ilustración: no se formatea dinero acá.
  protected readonly movimientos = [
    { nombre: 'Aporte · Las Comadres', cuando: 'Hoy, 09:12', importe: '250,00', entra: false },
    { nombre: 'Recarga con QR', cuando: 'Ayer, 18:40', importe: '500,00', entra: true },
    { nombre: 'Entrega recibida', cuando: '3 de julio', importe: '2.500,00', entra: true },
  ]
}
