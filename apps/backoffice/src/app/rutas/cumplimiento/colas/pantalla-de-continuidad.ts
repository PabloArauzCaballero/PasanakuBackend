import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { PruebaDeContinuidadEnLista, SalidaPruebaDeContinuidad } from 'clientes/angular/cumplimiento'
import { Sesion } from '../../../nucleo/sesion'
import { tieneAlguno } from '../compartido/acceso'
import { crearBandeja } from '../compartido/bandeja-por-cursor'
import type { Recibo } from '../dominio/cu43-reportes'
import { cargadorDePruebas } from '../dominio/cu56-continuidad'
import { textosColas } from '../textos-colas'
import { camposDeCelda, ColaDeLectura, type Celda } from './cola-de-lectura'
import { RegistrarPrueba } from './registrar-prueba'

const T = textosColas.continuidad
const COLUMNAS: Columna<PruebaDeContinuidadEnLista>[] = [
  { clave: 'fecha', titulo: T.columnas.fecha },
  { clave: 'tipo', titulo: T.columnas.tipo },
  { clave: 'rtoMinutos', titulo: T.columnas.rtoMinutos, numerica: true },
  { clave: 'rpoMinutos', titulo: T.columnas.rpoMinutos, numerica: true },
  { clave: 'resultado', titulo: T.columnas.resultado },
  { clave: 'actaComiteId', titulo: T.columnas.actaComiteId },
]
const celda = (p: PruebaDeContinuidadEnLista, clave: string): Celda => {
  switch (clave) {
    case 'fecha': return formatearDia(p.fecha)
    case 'tipo': return textosColas.tiposDePrueba[p.tipo] ?? p.tipo
    case 'resultado': return textosColas.resultados[p.resultado] ?? p.resultado
    case 'actaComiteId': return p.actaComiteId ? T.conActa : T.sinActa
    default: return String(p[clave as keyof PruebaDeContinuidadEnLista] ?? '')
  }
}

/**
 * CU-56 · pruebas de continuidad (`listarPruebasDeContinuidad`). Lectura con RESPONSABLE_RIESGOS o AUDITORIA_LEER;
 * registrar (`registrarPruebaDeContinuidad`) solo el rol RESPONSABLE_RIESGOS. Una prueba registrada es evidencia ante
 * el regulador: no se edita ni se elimina (el contrato no tiene esas operaciones).
 */
@Component({
  selector: 'ap-pantalla-de-continuidad',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ColaDeLectura, Boton, Alerta, RegistrarPrueba],
  template: `
    <ap-cola-de-lectura [titulo]="t.titulo" [proposito]="t.proposito" icono="actividad" [tabla]="t.tabla" [vacio]="t.vacio" [columnas]="COLUMNAS" [bandeja]="bandeja" [identidad]="identidad" [celda]="celda" nombreDeRegistros="pruebas" ayudaDeBusqueda="Busca por fecha, tipo, RTO, RPO, resultado o acta en la página cargada: el servidor no admite búsqueda ni filtros.">
      @if (puedeRegistrar()) { <ap-boton acciones variante="primario" icono="mas" (pulsado)="registrando.set(true)">{{ t.registrar }}</ap-boton> }
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
    </ap-cola-de-lectura>
    @if (puedeRegistrar()) { <ap-registrar-prueba [(abierto)]="registrando" (registro)="alRegistrar($event)" /> }
  `,
})
export class PantallaDeContinuidad {
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly celda = celda
  protected readonly bandeja = crearBandeja<PruebaDeContinuidadEnLista>({ cargador: cargadorDePruebas(), camposDeBusqueda: camposDeCelda(COLUMNAS, celda) })
  protected readonly identidad = (p: PruebaDeContinuidadEnLista) => p.pruebaId
  protected readonly puedeRegistrar = computed(() => tieneAlguno(this.sesion, ['RESPONSABLE_RIESGOS']))
  protected readonly registrando = signal(false)
  protected readonly recibo = signal<string | null>(null)

  protected alRegistrar({ salida, yaExistia }: Recibo<SalidaPruebaDeContinuidad>): void {
    const resultado = textosColas.resultados[salida.resultado] ?? salida.resultado
    this.recibo.set(textosColas.registro.recibo(resultado, !!salida.planAccionId, yaExistia))
    void this.bandeja.recargar()
  }
}
