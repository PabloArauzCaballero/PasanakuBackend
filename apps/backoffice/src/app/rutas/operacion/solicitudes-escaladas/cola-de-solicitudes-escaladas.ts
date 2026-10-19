import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { RelojDePlazo } from '@aportaya/ui/reloj-de-plazo/reloj-de-plazo'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Campo } from '@aportaya/ui/campo/campo'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { cargadorDeSolicitudesEscaladas, type SolicitudEscalada } from '../dominio/d15-solicitudes-escaladas'
import { textosOperacion } from '../textos'

type Accion = 'ACEPTAR' | 'RECHAZAR'

const COLUMNAS: ColumnaVirtual<SolicitudEscalada>[] = [
  { clave: 'usuarioNombre', titulo: 'Postulante', ordenable: true },
  { clave: 'grupoCodigo', titulo: 'Grupo' },
  { clave: 'factores', titulo: 'Puntaje descompuesto', ancho: '2' },
  { clave: 'fechaLimiteOrganizador', titulo: 'Vencía para el organizador', ordenable: true },
  { clave: 'solicitudId', titulo: 'Acciones' },
]

/**
 * D-15 · La cola de solicitudes de ingreso que el organizador dejó vencer (48 horas).
 */
@Component({
  selector: 'ap-cola-de-solicitudes-escaladas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, Campo, Dialogo, RelojDePlazo, TablaDeDatosVirtualizada],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="grupo" />
      @if (sinEnviar()) { <ap-alerta tono="aviso">{{ t.sinEnviar }}</ap-alerta> }
      <ap-tabla-de-datos-virtualizada [titulo]="t.titulo" [columnas]="COLUMNAS" [cargador]="cargador" [ordenPermitido]="['fechaLimiteOrganizador', 'usuarioNombre']" [identidad]="identidad">
        <ng-template #celda let-s let-columna="columna">
          @switch (columna.clave) {
            @case ('factores') {
              <ul class="factores">
                @for (f of s.factores; track f.motivo) {
                  <li [class.favor]="f.aFavor" [class.contra]="!f.aFavor">{{ f.aFavor ? t.aFavor : t.enContra }}: {{ f.motivo }}</li>
                }
              </ul>
            }
            @case ('fechaLimiteOrganizador') {
              <ap-reloj-de-plazo [etiqueta]="t.plazoOrganizador" [venceIso]="s.fechaLimiteOrganizador" [ahoraIso]="ahoraIso" />
            }
            @case ('solicitudId') {
              @if (puedeResolver()) {
                <div class="acciones">
                  <ap-boton (pulsado)="abrir(s, 'ACEPTAR')">{{ t.aceptar }}</ap-boton>
                  <ap-boton variante="fantasma" (pulsado)="abrir(s, 'RECHAZAR')">{{ t.rechazar }}</ap-boton>
                </div>
              }
            }
            @default {
              {{ s[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos-virtualizada>
    </section>

    @if (seleccionada(); as s) {
      <ap-dialogo
        [titulo]="accion() === 'ACEPTAR' ? t.aceptar : t.rechazar"
        [textoDeConfirmar]="accion() === 'ACEPTAR' ? t.aceptar : t.rechazar"
        [destructivo]="accion() === 'RECHAZAR'"
        [abierto]="dialogoAbierto()"
        (abiertoChange)="dialogoAbierto.set($event)"
        (confirmar)="confirmar()"
        (cancelar)="cerrar()"
      >
        <p>{{ accion() === 'ACEPTAR' ? t.confirmarAceptar(s.usuarioNombre, s.grupoCodigo) : t.confirmarRechazar(s.usuarioNombre, s.grupoCodigo) }}</p>
        @if (accion() === 'RECHAZAR') {
          <ap-campo [etiqueta]="t.motivoDeRechazo" [valor]="motivo()" (valorChange)="motivo.set($event)" [error]="motivoVacio() ? t.faltaMotivo : undefined" />
        }
      </ap-dialogo>
    }
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 68rem; display: grid; gap: var(--s4); }
    .factores { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--s1); font-size: .9em; white-space: normal; }
    .factores .favor { color: var(--ok); }
    .factores .contra { color: var(--err); }
    .acciones { display: flex; gap: var(--s2); }
  `,
})
export class ColaDeSolicitudesEscaladas {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  private readonly sesion = inject(Sesion)
  protected readonly t = textosOperacion.solicitudesEscaladas
  protected readonly ahoraIso = new Date().toISOString()
  protected readonly COLUMNAS = COLUMNAS
  protected readonly identidad = (s: SolicitudEscalada) => s.solicitudId
  protected readonly cargador = cargadorDeSolicitudesEscaladas()
  protected readonly puedeResolver = computed(() => this.sesion.puede('SOLICITUD_INGRESO_RESOLVER'))

  protected readonly seleccionada = signal<SolicitudEscalada | null>(null)
  protected readonly accion = signal<Accion>('ACEPTAR')
  protected readonly dialogoAbierto = signal(false)
  protected readonly motivo = signal('')
  protected readonly intentoConfirmar = signal(false)
  /** La confirmación no manda nada (falta la operación en el servicio): se dice, en vez de cerrar el diálogo como si hubiera funcionado. */
  protected readonly sinEnviar = signal(false)
  protected readonly motivoVacio = computed(() => this.intentoConfirmar() && this.accion() === 'RECHAZAR' && this.motivo().trim().length === 0)

  abrir(s: SolicitudEscalada, accion: Accion): void {
    this.seleccionada.set(s)
    this.accion.set(accion)
    this.motivo.set('')
    this.intentoConfirmar.set(false)
    this.dialogoAbierto.set(true)
  }

  cerrar(): void {
    this.dialogoAbierto.set(false)
    this.seleccionada.set(null)
  }

  confirmar(): void {
    if (this.accion() === 'RECHAZAR') {
      this.intentoConfirmar.set(true)
      if (this.motivo().trim().length === 0) return
    }
    // El envío real queda pendiente del contrato de resolución (ver el supuesto
    // declarado en dominio/d15-solicitudes-escaladas.ts). Mientras tanto se avisa que NO se envió.
    this.cerrar()
    this.sinEnviar.set(true)
  }
}
