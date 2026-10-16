import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import type { PruebaDeContinuidadEnLista, SalidaPruebaDeContinuidad } from 'clientes/angular/cumplimiento'
import { Sesion } from '../../../nucleo/sesion'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { tieneAlguno } from '../compartido/acceso'
import type { Recibo } from '../dominio/cu43-reportes'
import { cargadorDePruebas } from '../dominio/cu56-continuidad'
import { textosColas } from '../textos-colas'
import { ColaDeLectura, type Celda } from './cola-de-lectura'
import { RegistrarPrueba } from './registrar-prueba'

const T = textosColas.continuidad
const COLUMNAS: ColumnaVirtual<PruebaDeContinuidadEnLista>[] = [
  { clave: 'fecha', titulo: T.columnas.fecha },
  { clave: 'tipo', titulo: T.columnas.tipo },
  { clave: 'rtoMinutos', titulo: T.columnas.rtoMinutos, numerica: true },
  { clave: 'rpoMinutos', titulo: T.columnas.rpoMinutos, numerica: true },
  { clave: 'resultado', titulo: T.columnas.resultado },
  { clave: 'actaComiteId', titulo: T.columnas.actaComiteId },
]

/**
 * CU-56 · pruebas de continuidad (`listarPruebasDeContinuidad`). Lectura con RESPONSABLE_RIESGOS o
 * AUDITORIA_LEER; registrar (`registrarPruebaDeContinuidad`) solo el rol RESPONSABLE_RIESGOS.
 */
@Component({
  selector: 'ap-pantalla-de-continuidad',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ColaDeLectura, Boton, Alerta, RegistrarPrueba],
  template: `
    <ap-cola-de-lectura [titulo]="t.titulo" [proposito]="t.proposito" [tabla]="t.tabla" [vacio]="t.vacio" [columnas]="COLUMNAS" [cargador]="cargador" [identidad]="identidad" [celda]="celda">
      @if (puedeRegistrar()) { <ap-boton acciones variante="primario" (pulsado)="registrando.set(true)">{{ t.registrar }}</ap-boton> }
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
    </ap-cola-de-lectura>
    @if (puedeRegistrar()) { <ap-registrar-prueba [(abierto)]="registrando" (registro)="alRegistrar($event)" /> }
  `,
})
export class PantallaDeContinuidad {
  private readonly sesion = inject(Sesion)
  private readonly cola = viewChild.required(ColaDeLectura)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly cargador = cargadorDePruebas()
  protected readonly identidad = (p: PruebaDeContinuidadEnLista) => p.pruebaId
  protected readonly puedeRegistrar = computed(() => tieneAlguno(this.sesion, ['RESPONSABLE_RIESGOS']))
  protected readonly registrando = signal(false)
  protected readonly recibo = signal<string | null>(null)
  protected readonly celda = (p: PruebaDeContinuidadEnLista, clave: string): Celda => {
    switch (clave) {
      case 'fecha': return formatearDia(p.fecha)
      case 'tipo': return textosColas.tiposDePrueba[p.tipo] ?? p.tipo
      case 'resultado': return textosColas.resultados[p.resultado] ?? p.resultado
      case 'actaComiteId': return p.actaComiteId ? T.conActa : T.sinActa
      default: return String(p[clave as keyof PruebaDeContinuidadEnLista] ?? '')
    }
  }

  protected alRegistrar({ salida, yaExistia }: Recibo<SalidaPruebaDeContinuidad>): void {
    const resultado = textosColas.resultados[salida.resultado] ?? salida.resultado
    this.recibo.set(textosColas.registro.recibo(resultado, !!salida.planAccionId, yaExistia))
    this.cola().recargar()
  }
}
