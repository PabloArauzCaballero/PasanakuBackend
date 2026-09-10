import { ChangeDetectionStrategy, Component, signal } from '@angular/core'
import { Acordeon } from '../acordeon/acordeon'
import { Alerta } from '../alerta/alerta'
import { BarraDePasos } from '../barra-de-pasos/barra-de-pasos'
import { ChipsDeFiltro } from '../chips-de-filtro/chips-de-filtro'
import { DesgloseDeCobro } from '../desglose-de-cobro/desglose-de-cobro'
import { EscaleraDeEtapas } from '../escalera-de-etapas/escalera-de-etapas'
import { EstadoVacio } from '../estado-vacio/estado-vacio'
import { FilaDeCotejo } from '../fila-de-cotejo/fila-de-cotejo'
import { FilaDeMovimiento } from '../fila-de-movimiento/fila-de-movimiento'
import { ItemPasanaku } from '../item-pasanaku/item-pasanaku'
import { MedidorDeRango } from '../medidor-de-rango/medidor-de-rango'
import { Paginacion } from '../paginacion/paginacion'
import { Pestanas } from '../pestanas/pestanas'
import { RelojDePlazo } from '../reloj-de-plazo/reloj-de-plazo'
import { ResumenDePeriodo } from '../resumen-de-periodo/resumen-de-periodo'
import { RielDeTurnos } from '../riel-de-turnos/riel-de-turnos'
import { Tarjeta } from '../tarjeta/tarjeta'
import { TarjetaKPI } from '../tarjeta-kpi/tarjeta-kpi'
import { TarjetaSaldo } from '../tarjeta-saldo/tarjeta-saldo'
import { SeccionDeCatalogo } from './seccion'
import { ETAPAS, FILTROS, HOY, MOVIMIENTOS, TURNOS } from './muestras'

/** Las moléculas con datos de muestra fijos. */
@Component({
  selector: 'ap-catalogo-moleculas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Acordeon, Alerta, BarraDePasos, ChipsDeFiltro, DesgloseDeCobro, EscaleraDeEtapas, EstadoVacio, FilaDeCotejo, FilaDeMovimiento, ItemPasanaku, MedidorDeRango, Paginacion, Pestanas, RelojDePlazo, ResumenDePeriodo, RielDeTurnos, Tarjeta, TarjetaKPI, TarjetaSaldo, SeccionDeCatalogo],
  template: `
    <ap-seccion-de-catalogo nombre="Tarjetas" ancla="tarjetas">
      <ap-tarjeta titulo="Con título">Contenido de la tarjeta.</ap-tarjeta>
      <ap-tarjeta-kpi etiqueta="Grupos activos" valor="128" variacion="12 más que la semana pasada" />
      <ap-tarjeta-kpi etiqueta="Recaudado hoy" valor="48200.00" moneda="BOB" />
      <ap-tarjeta-saldo saldo="1240.00" moneda="BOB" retenido="250.00" style="width: 100%; max-width: calc(var(--s7) * 8)" />
      <ap-tarjeta-saldo etiqueta="Saldo disponible (sin plata)" saldo="0.00" moneda="BOB" style="width: 100%; max-width: calc(var(--s7) * 8)" />
    </ap-seccion-de-catalogo>
    <ap-seccion-de-catalogo nombre="Listas" ancla="listas">
      <div role="list" style="width: 100%; max-width: calc(var(--s7) * 9)">
        <ap-item-pasanaku nombre="Las Vecinas" aporte="250.00" moneda="BOB" [turnoActual]="4" [turnos]="12" estado="alDia" />
        <ap-item-pasanaku nombre="Taller Sur" aporte="500.00" moneda="BOB" [turnoActual]="7" [turnos]="8" estado="atrasado" />
      </div>
      <div style="width: 100%; max-width: calc(var(--s7) * 9)">
        @for (m of movimientos; track m.id) { <ap-fila-de-movimiento [m]="m" /> }
      </div>
      <ap-resumen-de-periodo periodo="Septiembre 2026" entradas="2900.00" salidas="262.50" neto="2637.50" moneda="BOB" style="width: 100%; max-width: calc(var(--s7) * 9)" />
      <ap-chips-de-filtro [filtros]="filtros" [(elegidos)]="elegidos" />
      <ap-estado-vacio titulo="Sin movimientos" motivo="porFiltro" />
      <ap-estado-vacio titulo="No podés ver los desembolsos" motivo="porPermiso" explicacion="Pedile acceso a Contabilidad." />
      <ap-estado-vacio titulo="Todavía no tenés grupos" accionPropia="Crear mi primer grupo" />
    </ap-seccion-de-catalogo>
    <ap-seccion-de-catalogo nombre="Avisos y navegación" ancla="avisos">
      <ap-alerta tono="ok" titulo="Listo">Tu aporte de Bs 250 quedó guardado.</ap-alerta>
      <ap-alerta tono="aviso">Tu turno vence mañana.</ap-alerta>
      <ap-alerta tono="error" titulo="No se pudo cobrar">Tu saldo no alcanza. Recargá y volvé a intentar.</ap-alerta>
      <ap-alerta tono="info">Los plazos se cuentan en días hábiles.</ap-alerta>
      <ap-pestanas etiqueta="Secciones" [pestanas]="[{ valor: 'grupos', texto: 'Grupos', cuenta: 3 }, { valor: 'aportes', texto: 'Aportes' }]" [(elegida)]="pestana" />
      <div id="panel-grupos" role="tabpanel" aria-labelledby="pestana-grupos" [hidden]="pestana() !== 'grupos'">Tres grupos.</div>
      <div id="panel-aportes" role="tabpanel" aria-labelledby="pestana-aportes" [hidden]="pestana() !== 'aportes'">Los aportes.</div>
      <ap-paginacion [totalDeFilas]="140" [pagina]="2" />
      <ap-acordeon titulo="¿Qué pasa si no pago?" [abierto]="true">Se aplica la escalera de etapas del reglamento.</ap-acordeon>
      <ap-barra-de-pasos [pasos]="['Teléfono', 'Código', 'Documento', 'Selfie', 'Cotejo', 'Cuenta', 'Reglas', 'Listo']" [actual]="3" style="width: 100%; max-width: calc(var(--s7) * 8)" />
    </ap-seccion-de-catalogo>
    <ap-seccion-de-catalogo nombre="Del negocio" ancla="negocio">
      <div role="table" aria-label="Cotejo" style="width: 100%; max-width: calc(var(--s7) * 10)">
        <ap-fila-de-cotejo campo="Nombre" declarado="José Pérez" documento="JOSÉ PÉREZ" />
        <ap-fila-de-cotejo campo="Documento" declarado="1234567 LP" documento="1234567 SC" />
      </div>
      <ap-riel-de-turnos [turnos]="turnos" style="width: 100%" />
      <ap-reloj-de-plazo etiqueta="Plazo para responder el reclamo" venceIso="2026-09-13T15:00:00Z" [ahoraIso]="hoy" norma="5 días hábiles, reglamento art. 12" />
      <ap-reloj-de-plazo etiqueta="Descargo" venceIso="2026-09-09T15:00:00Z" [ahoraIso]="hoy" />
      <ap-escalera-de-etapas [etapas]="etapas" [actual]="1" />
      <ap-medidor-de-rango etiqueta="Riesgo del cambio" [valor]="72" style="width: 100%; max-width: calc(var(--s7) * 6)" />
      <ap-desglose-de-cobro [lineas]="[{ concepto: 'Entrega del turno', monto: '2400.00' }, { concepto: 'Comisión', monto: '-24.00', detalle: '1 % según tarifa vigente' }]" total="2376.00" moneda="BOB" style="width: 100%; max-width: calc(var(--s7) * 7)" />
    </ap-seccion-de-catalogo>
  `,
})
export class CatalogoMoleculas {
  readonly hoy = HOY
  readonly movimientos = MOVIMIENTOS
  readonly filtros = FILTROS
  readonly turnos = TURNOS
  readonly etapas = ETAPAS
  readonly elegidos = signal(['aporte'])
  readonly pestana = signal('grupos')
}
