import { ChangeDetectionStrategy, Component, signal } from '@angular/core'
import { BandaDeProposito } from '../banda-de-proposito/banda-de-proposito'
import { Boton } from '../boton/boton'
import { Dialogo } from '../dialogo/dialogo'
import { FocoDeTutorial, type RecuadroResaltado } from '../foco-de-tutorial/foco-de-tutorial'
import { GloboDeTutorial } from '../globo-de-tutorial/globo-de-tutorial'
import { ListaDeMovimientos } from '../lista-de-movimientos/lista-de-movimientos'
import { ListaDeRequisitos } from '../lista-de-requisitos/lista-de-requisitos'
import { PanelDeFactores } from '../panel-de-factores/panel-de-factores'
import { SeccionDeExpediente } from '../seccion-de-expediente/seccion-de-expediente'
import { Columna, TablaDeDatos } from '../tabla-de-datos/tabla-de-datos'
import { TarjetaDeOferta } from '../tarjeta-de-oferta/tarjeta-de-oferta'
import { TarjetaDeSolicitud } from '../tarjeta-de-solicitud/tarjeta-de-solicitud'
import { Vale } from '../vale/vale'
import { VerificadorDeSorteo } from '../verificador-de-sorteo/verificador-de-sorteo'
import { SeccionDeCatalogo } from './seccion'
import { FACTORES, FILAS_DE_TABLA, FilaDeTabla, HOY, MOVIMIENTOS, PASOS_DE_SORTEO, REQUISITOS } from './muestras'

/** Los organismos con datos de muestra fijos. */
@Component({
  selector: 'ap-catalogo-organismos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, Dialogo, FocoDeTutorial, GloboDeTutorial, ListaDeMovimientos, ListaDeRequisitos, PanelDeFactores, SeccionDeExpediente, TablaDeDatos, TarjetaDeOferta, TarjetaDeSolicitud, Vale, VerificadorDeSorteo, SeccionDeCatalogo],
  template: `
    <ap-seccion-de-catalogo nombre="Organismos" ancla="organismos">
      <ap-banda-de-proposito texto="Acá se ve la plata de una persona y se decide si un movimiento raro se congela o se deja pasar." style="width: 100%" />
      <ap-tabla-de-datos titulo="Grupos" [columnas]="columnas" [filas]="filas" [seleccionable]="true" [elegidas]="['g2']" style="width: 100%; max-width: calc(var(--s7) * 12)" />
      <ap-lista-de-movimientos [movimientos]="movimientos" moneda="BOB" [hoyIso]="hoy" style="width: 100%; max-width: calc(var(--s7) * 9)" />
      <ap-seccion-de-expediente titulo="Verificación de identidad" actor="Ana Flores (cumplimiento)" [actualizadoIso]="hoy" [datos]="[{ nombre: 'Documento', valor: '1234567 LP' }, { nombre: 'Vence', valor: '2030-01-01T00:00:00Z', esFecha: true }, { nombre: 'Resultado', valor: 'Coincide' }]" style="width: 100%; max-width: calc(var(--s7) * 10)" />
      <ap-lista-de-requisitos [requisitos]="requisitos" style="width: 100%; max-width: calc(var(--s7) * 9)" />
      <ap-tarjeta-de-solicitud nombre="Carlos Choque" detalle="Pide el cupo 7 de Las Vecinas" [cuandoIso]="hoy" style="width: 100%; max-width: calc(var(--s7) * 8)" />
      <ap-vale partner="Farmacia Sur" beneficio="20 % en medicamentos" codigo="AY-7K2M-93" venceIso="2026-10-01T00:00:00Z" condiciones="Un uso por persona. No acumulable." />
      <ap-vale partner="Farmacia Sur" beneficio="20 % en medicamentos" codigo="AY-7K2M-93" venceIso="2026-08-01T00:00:00Z" estado="vencido" />
      <ap-tarjeta-de-oferta nombre="Rosa Mamani" [turnoQueDa]="2" [turnoQuePide]="6" costo="80.00" moneda="BOB" riesgo="medio" style="width: 100%; max-width: calc(var(--s7) * 7)" />
      <ap-panel-de-factores veredicto="Se aprueba con límite de Bs 500" [factores]="factores" style="width: 100%; max-width: calc(var(--s7) * 9)" />
      <ap-verificador-de-sorteo [pasos]="pasos" [coincide]="true" [conReproducir]="true" enlacePublico="/verificar/sorteo/abc" style="width: 100%; max-width: calc(var(--s7) * 9)" />
      <ap-boton variante="secundario" (pulsado)="dialogo.set(true)">Abrir un diálogo</ap-boton>
      <ap-dialogo titulo="¿Confirmás el aporte?" textoDeConfirmar="Confirmar aporte de Bs 250" [(abierto)]="dialogo">Se debita de tu saldo ahora mismo.</ap-dialogo>

      <ap-boton variante="secundario" (pulsado)="tutorial.set(!tutorial())">{{ tutorial() ? 'Cerrar' : 'Ver' }} el paso de un tutorial</ap-boton>
      @if (tutorial()) {
        <ap-foco-de-tutorial [recuadro]="recuadro" [bloquea]="false" />
        <ap-globo-de-tutorial
          titulo="Este es el menú"
          descripcion="Cada entrada abre un dominio completo. Se vuelve al tablero desde cualquier lado."
          [indice]="0"
          [total]="4"
          [recuadro]="recuadro"
          posicion="abajo"
          [ventana]="ventana"
          (cerrar)="tutorial.set(false)"
          (omitir)="tutorial.set(false)"
          (avanzar)="tutorial.set(false)"
        />
      }
    </ap-seccion-de-catalogo>
  `,
})
export class CatalogoOrganismos {
  readonly tutorial = signal(false)
  /** Un recuadro de mentira, para mostrar el velo y el globo sin motor detrás. */
  readonly recuadro: RecuadroResaltado = { x: 40, y: 120, ancho: 220, alto: 48 }
  /** El catálogo se prerrenderiza en el sitio: sin ventana, se asume una de escritorio. */
  readonly ventana = typeof window === 'undefined' ? { ancho: 1280, alto: 900 } : { ancho: window.innerWidth, alto: window.innerHeight }
  readonly hoy = HOY
  readonly movimientos = MOVIMIENTOS
  readonly requisitos = REQUISITOS
  readonly factores = FACTORES
  readonly pasos = PASOS_DE_SORTEO
  readonly filas = FILAS_DE_TABLA
  readonly columnas: Columna<FilaDeTabla>[] = [
    { clave: 'grupo', titulo: 'Grupo', ordenable: true },
    { clave: 'cupos', titulo: 'Cupos', numerica: true, ordenable: true },
    { clave: 'aporte', titulo: 'Aporte (Bs)', numerica: true },
    { clave: 'estado', titulo: 'Estado' },
  ]
  readonly dialogo = signal(false)
}
