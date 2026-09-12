import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Campo } from '@aportaya/ui/campo/campo'
import { CampoMonto, aCadenaDelContrato } from '@aportaya/ui/campo-monto/campo-monto'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Monto } from '@aportaya/ui/monto/monto'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { accionesDeAnunciante, cargadorDeAnunciantes, type Anunciante } from '../dominio/cu110-anunciantes'
import { textosPublicidad } from '../textos'
import { EntradaAnuncianteMonedaEnum, EntradaAnuncianteTipoEnum } from 'clientes/angular/publicidad/model/entradaAnunciante'

const COLUMNAS: ColumnaVirtual<Anunciante>[] = [
  { clave: 'razonSocialFacturacion', titulo: 'Razón social', ordenable: true },
  { clave: 'tipo', titulo: 'Tipo' },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'limiteGastoMensual', titulo: 'Límite mensual', numerica: true },
]

/**
 * CU-110 · Anunciantes. Postular socio comercial, verificarlo y dar de alta el
 * anunciante sobre la `TablaDeDatosVirtualizada` real del shell. Todo importe pasa por
 * `ap-monto` (átomo `@aportaya/ui/monto`): acá no hay formateo manual de plata.
 */
@Component({
  selector: 'ap-pantalla-de-anunciantes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, Dialogo, Campo, CampoMonto, ChipEstado, Monto, TablaDeDatosVirtualizada],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <header>
        <h1>{{ t.titulo }}</h1>
        @if (puedeGestionar()) {
          <ap-boton (pulsado)="abrir()">{{ t.nuevoAnunciante }}</ap-boton>
        }
      </header>
      <ap-tabla-de-datos-virtualizada [titulo]="t.titulo" [columnas]="COLUMNAS" [cargador]="cargador" [ordenPermitido]="['razonSocialFacturacion']" [identidad]="identidad">
        <ng-template #celda let-a let-columna="columna">
          @switch (columna.clave) {
            @case ('tipo') {
              {{ a.tipo === 'ORGANIZADOR' ? 'Organizador' : 'Socio comercial' }}
            }
            @case ('estado') {
              <ap-chip-estado [tono]="a.estado === 'ACTIVA' ? 'ok' : 'neutro'">{{ a.estado === 'ACTIVA' ? 'Activa' : 'Suspendida' }}</ap-chip-estado>
            }
            @case ('limiteGastoMensual') {
              @if (a.limiteGastoMensual) {
                <ap-monto [monto]="a.limiteGastoMensual" [moneda]="a.moneda" />
              } @else {
                Sin límite
              }
            }
            @default {
              {{ a[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos-virtualizada>
    </main>

    <ap-dialogo [titulo]="t.nuevoAnunciante" [textoDeConfirmar]="t.guardar" [abierto]="dialogoAbierto()" (abiertoChange)="dialogoAbierto.set($event)" (confirmar)="guardar()" (cancelar)="cerrar()" [cargando]="guardando()">
      <div class="formulario">
        <ap-campo [etiqueta]="t.razonSocial" [(valor)]="razonSocial" [error]="intentoGuardar() && !razonSocial() ? 'Obligatorio' : undefined" />
        <ap-campo-monto [etiqueta]="t.limiteGasto" [(valor)]="limite" />
      </div>
    </ap-dialogo>
  `,
  styles: `
    main { padding: var(--s5); max-width: 68rem; display: grid; gap: var(--s4); }
    header { display: flex; align-items: center; justify-content: space-between; gap: var(--s3); }
    h1 { margin: 0; }
    .formulario { display: grid; gap: var(--s3); }
  `,
})
export class PantallaDeAnunciantes {
  private readonly sesion = inject(Sesion)
  private readonly acciones = accionesDeAnunciante()
  protected readonly t = textosPublicidad.anunciantes
  protected readonly COLUMNAS = COLUMNAS
  protected readonly identidad = (a: Anunciante) => a.anuncianteId
  protected readonly cargador = cargadorDeAnunciantes()
  protected readonly puedeGestionar = () => this.sesion.puede('PUBLICIDAD_ANUNCIANTES')

  protected readonly dialogoAbierto = signal(false)
  protected readonly guardando = signal(false)
  protected readonly intentoGuardar = signal(false)
  protected readonly razonSocial = signal('')
  protected readonly limite = signal('')

  abrir(): void {
    this.razonSocial.set('')
    this.limite.set('')
    this.intentoGuardar.set(false)
    this.dialogoAbierto.set(true)
  }

  cerrar(): void {
    this.dialogoAbierto.set(false)
  }

  async guardar(): Promise<void> {
    this.intentoGuardar.set(true)
    if (!this.razonSocial().trim()) return
    this.guardando.set(true)
    try {
      const limiteContrato = this.limite() ? aCadenaDelContrato(this.limite()) : null
      await this.acciones.darDeAlta({
        tipo: EntradaAnuncianteTipoEnum.SocioComercial,
        razonSocialFacturacion: this.razonSocial(),
        moneda: EntradaAnuncianteMonedaEnum.Bob,
        ...(limiteContrato ? { limiteGastoMensual: limiteContrato } : {}),
      })
      this.cerrar()
    } finally {
      this.guardando.set(false)
    }
  }
}
