import { ChangeDetectionStrategy, Component } from '@angular/core'
import { Icono } from '../../../layout/icono'
import { NombreDeTrazo } from '../../../layout/trazos'

type Bloque = { icono: NombreDeTrazo; titulo: string; texto: string; naranja?: boolean }

/** Cómo funciona y la billetera: los dos bloques que explican el producto. */
@Component({
  selector: 'ap-producto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { style: 'display: contents' },
  template: `
    <section class="seccion seccion--tenue" id="como-funciona" aria-labelledby="titulo-como">
      <div class="contenedor">
        <div class="cabecera">
          <span class="etiqueta">Cómo funciona</span>
          <h2 id="titulo-como">El pasanaku de siempre, con las cuentas claras.</h2>
          <p>Un grupo, un monto y un turno para cada uno. Lo que cambia es que ahora todo queda registrado: quién aportó, cuándo, y a quién le tocó.</p>
        </div>
        <ol class="rejilla rejilla--4 flujo">
          @for (paso of pasos; track paso.titulo; let i = $index) {
            <li class="paso">
              <div class="paso-marca">
                <div class="icono"><ap-icono [nombre]="paso.icono" [tamano]="21" /></div>
                <span class="paso-num" aria-hidden="true">{{ i + 1 }}</span>
              </div>
              <h3>{{ paso.titulo }}</h3>
              <p>{{ paso.texto }}</p>
            </li>
          }
        </ol>
      </div>
    </section>

    <section class="seccion" id="billetera" aria-labelledby="titulo-billetera">
      <div class="contenedor">
        <div class="cabecera">
          <span class="etiqueta">La billetera</span>
          <h2 id="titulo-billetera">Tu plata, a tu alcance.</h2>
          <p>No es una app para anotar quién debe. Es una billetera: el dinero entra, se mueve y sale, y cada paso deja rastro.</p>
        </div>
        <div class="rejilla rejilla--3">
          @for (b of billetera; track b.titulo) {
            <article class="tarjeta">
              <div class="icono" [class.icono--naranja]="b.naranja"><ap-icono [nombre]="b.icono" [tamano]="22" /></div>
              <h3>{{ b.titulo }}</h3>
              <p>{{ b.texto }}</p>
            </article>
          }
        </div>
      </div>
    </section>
  `,
})
export class Producto {
  protected readonly pasos: Bloque[] = [
    { icono: 'grupo', titulo: 'Armá el grupo', texto: 'Elegís el monto del aporte, cada cuánto se aporta y cuántos cupos hay. El reglamento queda escrito y todos lo aceptan antes de que entre el primer boliviano.' },
    { icono: 'dado', titulo: 'Se sortea el orden', texto: 'Publicamos el hash de la semilla antes del sorteo y la semilla después. Cualquiera puede recomputar el orden y comprobar que salió así.' },
    { icono: 'calendario', titulo: 'Aportá cada período', texto: 'Desde tu saldo, con QR o en efectivo en un punto de atención. Te avisamos antes de que venza, no después.' },
    { icono: 'regalo', titulo: 'Recibí tu entrega', texto: 'Cuando te toca, el fondo del período completo entra a tu billetera. De ahí lo retirás a tu cuenta cuando quieras.' },
  ]
  protected readonly billetera: Bloque[] = [
    { icono: 'billetera', titulo: 'Recargá y retirá', texto: 'Recargás por QR, transferencia o efectivo, y retirás a tu cuenta bancaria cuando quieras. Retirar es un derecho, no un favor.' },
    { icono: 'qr', titulo: 'Aportá con QR', texto: 'Escaneás y aportás en segundos. El pago se concilia contra el extracto del banco, no contra una captura de pantalla en el grupo.', naranja: true },
    { icono: 'mensaje', titulo: 'Te avisamos a tiempo', texto: 'Recordatorio antes del vencimiento y confirmación cuando el aporte entra, por WhatsApp o notificación. Vos elegís qué te llega.' },
    { icono: 'candado', titulo: 'Custodia separada', texto: 'El dinero de los participantes no se mezcla con el de la plataforma. El encaje se verifica todos los días: si no cuadra, salta.' },
    { icono: 'escudoCheck', titulo: 'Fondo de garantía', texto: 'Si alguien no aporta, el fondo cubre la entrega de quien tenía el turno. El grupo no se cae por uno, y la deuda se gestiona aparte.' },
    { icono: 'engranaje', titulo: 'Organizador automático', texto: 'Los cobros, los recordatorios y el cierre de cada período los hace el sistema. Si preferís un organizador de carne y hueso, no cobra comisión.' },
  ]
}
