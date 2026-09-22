import { ChangeDetectionStrategy, Component, signal } from '@angular/core'
import { Avatar } from '../avatar/avatar'
import { Boton } from '../boton/boton'
import { Campo } from '../campo/campo'
import { CampoBusqueda } from '../campo-busqueda/campo-busqueda'
import { CampoContrasena } from '../campo-contrasena/campo-contrasena'
import { CampoMonto } from '../campo-monto/campo-monto'
import { CampoOTP } from '../campo-otp/campo-otp'
import { Casilla } from '../casilla/casilla'
import { ChipElegible } from '../chip-elegible/chip-elegible'
import { ChipEstado } from '../chip-estado/chip-estado'
import { CodigoQR } from '../codigo-qr/codigo-qr'
import { CuentaEnmascarada } from '../cuenta-enmascarada/cuenta-enmascarada'
import { Esqueleto } from '../esqueleto/esqueleto'
import { Estrellas } from '../estrellas/estrellas'
import { Fecha } from '../fecha/fecha'
import { Girador } from '../girador/girador'
import { GrupoRadio } from '../grupo-radio/grupo-radio'
import { Interruptor } from '../interruptor/interruptor'
import { Monto } from '../monto/monto'
import { Progreso } from '../progreso/progreso'
import { Punto } from '../punto/punto'
import { SelectorSegmentado } from '../selector-segmentado/selector-segmentado'
import { TONOS } from '../tono/tono'
import { SeccionDeCatalogo } from './seccion'
import { HOY } from './muestras'

/** Los átomos, cada uno con sus variantes y estados. Es datos: no tiene lógica propia. */
@Component({
  selector: 'ap-catalogo-atomos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar, Boton, Campo, CampoBusqueda, CampoContrasena, CampoMonto, CampoOTP, Casilla, ChipElegible, ChipEstado, CodigoQR, CuentaEnmascarada, Esqueleto, Estrellas, Fecha, Girador, GrupoRadio, Interruptor, Monto, Progreso, Punto, SelectorSegmentado, SeccionDeCatalogo],
  template: `
    <ap-seccion-de-catalogo nombre="Botones" ancla="botones">
      <ap-boton variante="primario">Confirmar aporte de Bs 250</ap-boton>
      <ap-boton variante="secundario">Ver el grupo</ap-boton>
      <ap-boton variante="fantasma">Cancelar</ap-boton>
      <ap-boton variante="peligro">Salir del grupo</ap-boton>
      <ap-boton variante="enlace">Ver el reglamento</ap-boton>
      <ap-boton variante="primario" [cargando]="true">Procesando</ap-boton>
      <ap-boton variante="secundario" [deshabilitado]="true">No disponible</ap-boton>
      <ap-boton variante="secundario" tamano="sm">Chico</ap-boton>
      <ap-boton variante="secundario" tamano="lg">Grande</ap-boton>
    </ap-seccion-de-catalogo>
    <ap-seccion-de-catalogo nombre="Campos" ancla="campos">
      <ap-campo etiqueta="Nombre completo" marcador="Como en tu carnet" ayuda="Tal cual figura en el documento." />
      <ap-campo etiqueta="Correo" tipo="email" valor="ana@" error="Ese correo no está completo" />
      <ap-campo etiqueta="Teléfono" tipo="tel" valor="71234567" [exito]="true" />
      <ap-campo etiqueta="Deshabilitado" valor="No se puede editar" [deshabilitado]="true" />
      <ap-campo-monto [(valor)]="monto" ayuda="Se debita de tu saldo." />
      <ap-campo-monto valor="12,3,4" etiqueta="Monto mal escrito" />
      <ap-campo-busqueda [(valor)]="busqueda" />
      <ap-campo-contrasena />
      <ap-campo-otp />
      <ap-campo-otp valor="12" error="El código venció. Pedí otro." />
    </ap-seccion-de-catalogo>
    <ap-seccion-de-catalogo nombre="Selección" ancla="seleccion">
      <ap-casilla [marcada]="true">Acepto el reglamento del grupo</ap-casilla>
      <ap-casilla>Sin marcar</ap-casilla>
      <ap-interruptor [activo]="true">Avisos por WhatsApp</ap-interruptor>
      <ap-interruptor>Apagado</ap-interruptor>
      <ap-grupo-radio etiqueta="Cómo pagás" [opciones]="[{ valor: 'saldo', texto: 'Con mi saldo' }, { valor: 'qr', texto: 'Con QR', detalle: 'Desde tu banco' }]" elegido="saldo" />
      <ap-selector-segmentado etiqueta="Vista" [segmentos]="[{ valor: 'lista', texto: 'Lista' }, { valor: 'calendario', texto: 'Calendario' }]" elegido="lista" />
      <ap-chip-elegible [elegido]="true" icono="aporte">Aportes</ap-chip-elegible>
      <ap-chip-elegible icono="entrega">Entregas</ap-chip-elegible>
      <ap-estrellas [valor]="4" />
      <ap-estrellas [valor]="3" [editable]="true" etiqueta="Calificá al organizador" />
    </ap-seccion-de-catalogo>
    <ap-seccion-de-catalogo nombre="Indicadores" ancla="indicadores">
      @for (t of tonos; track t) { <ap-chip-estado [tono]="t">{{ t }}</ap-chip-estado> }
      <ap-chip-estado tono="ok" icono="verificado">Al día</ap-chip-estado>
      <ap-avatar nombre="Ana Flores" tamano="sm" /><ap-avatar nombre="Ana Flores" /><ap-avatar nombre="Ana Flores" tamano="lg" /><ap-avatar nombre="Ana Flores" tamano="xl" />
      <ap-girador /><ap-progreso [valor]="0.6" etiqueta="Avance del grupo" style="width: calc(var(--s7) * 4)" /><ap-esqueleto style="width: calc(var(--s7) * 4)" /><ap-punto etiqueta="Hay novedades" />
      <ap-monto monto="1240.00" moneda="BOB" etiqueta="Saldo" /><ap-monto monto="-12.50" moneda="BOB" etiqueta="Mora" class="negativo" />
      <ap-cuenta-enmascarada numero="4321 0000 1234" banco="Banco Unión" />
      <ap-fecha [iso]="hoy" /><ap-fecha [iso]="hoy" [conHora]="false" />
      <ap-codigo-qr contenido="https://aportaya.bo/v/ABC123" etiqueta="Código para pagar el aporte" />
    </ap-seccion-de-catalogo>
  `,
})
export class CatalogoAtomos {
  readonly tonos = TONOS
  readonly hoy = HOY
  readonly monto = signal('1.240,00')
  readonly busqueda = signal('vecinas')
}
