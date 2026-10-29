import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { SalidaSaludDeServicios } from 'clientes/angular/auditoria'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { filasConSalud, horaDeConsulta, recursoSaludDeServicios, type FilaDeEstado } from '../../operacion/estado/dominio/estado-del-sistema'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { textosSistemas } from '../textos'

/**
 * Servicios y SLO · la salud REAL de cada servicio: `auditoria` pregunta el readiness de los catorce
 * (`GET /indicadores/operacion/servicios`, `consultarSaludDeServicios`) y esta pantalla lo pinta al lado del nivel de
 * criticidad de ADR-037. Antes leía `PUERTO_SERVICIOS`, que en producción siempre rechazaba («fuente no disponible»).
 * Disponibilidad de 30 días y presupuesto de error no se muestran: ningún contrato los publica todavía
 * (`entregables/brecha-observabilidad.md`), y una columna vacía para los catorce no dice nada.
 */
@Component({
  selector: 'ap-pantalla-servicios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina data-tutorial-id="sistemas-servicios" eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito" icono="servidor">
        <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="salud.isLoading()" (pulsado)="salud.reload()">Volver a consultar</ap-boton>
      </ap-cabecera-de-pagina>
      @if (consultadoA(); as hora) { <p class="consulta" role="status">Consultado a las {{ hora }} (hora de La Paz).</p> }
      <ap-cifras-de-sistemas etiqueta="Resumen de servicios" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado data-tutorial-id="sistemas-servicios-estado" [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por nombre del servicio, su salud o su nivel." [exportables]="exportables" nombreDeRegistros="servicios" [textoVacio]="t.vacio" explicacionVacio="Ningún servicio coincide con la búsqueda o el filtro." (reintentar)="salud.reload()">
        <ng-template #celda let-f let-c="columna">
          @if (c.clave === 'salud') { <ap-chip-estado [tono]="f.tono">{{ f.salud }}</ap-chip-estado> } @else { {{ f[c.clave] }} }
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `:host { display: block; min-width: 0; } .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; } .consulta { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); } /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }`,
})
export class PantallaServicios {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.servicios
  protected readonly salud = recursoSaludDeServicios()
  protected readonly carga = cargaDe(this.salud)
  private readonly filas = filasDe<FilaDeEstado, SalidaSaludDeServicios>(this.salud, (v) => filasConSalud(v, undefined))
  protected readonly consultadoA = computed(() => (this.salud.hasValue() && this.salud.value() ? horaDeConsulta(this.salud.value()!.consultadoEn) : null))
  protected readonly listado = crearListado<FilaDeEstado>({
    filas: this.filas,
    camposDeBusqueda: ['servicio', 'salud', 'nivel'],
    filtros: [
      { nombre: 'salud', etiqueta: 'Salud', valor: (s) => s.salud },
      { nombre: 'nivel', etiqueta: 'Nivel', valor: (s) => s.nivel },
    ],
    porPagina: 20,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Servicios', valor: contar(f), icono: 'servidor' },
      { etiqueta: 'Arriba', valor: contar(f, (s) => s.tono === 'ok'), icono: 'verificado', tono: 'ok' },
      { etiqueta: 'Con problemas', valor: contar(f, (s) => s.tono === 'aviso'), icono: 'advertencia', tono: 'aviso' },
      { etiqueta: 'No responden', valor: contar(f, (s) => s.tono === 'error'), icono: 'alerta', tono: 'error' },
    ]
  })
  protected readonly columnas: Columna<FilaDeEstado>[] = [
    { clave: 'servicio', titulo: 'Servicio', ordenable: true, mono: true },
    { clave: 'salud', titulo: 'Salud ahora', ordenable: true },
    { clave: 'latencia', titulo: 'Tardó', numerica: true },
    { clave: 'nivel', titulo: 'Nivel', ordenable: true },
    { clave: 'replicas', titulo: 'Réplicas exigidas (producción)', ordenable: true, numerica: true },
    { clave: 'porQue', titulo: 'Por qué ese nivel' },
  ]
  protected readonly exportables: ColumnaExportable<FilaDeEstado>[] = [
    { clave: 'servicio', titulo: 'Servicio' },
    { clave: 'salud', titulo: 'Salud ahora' },
    { clave: 'latencia', titulo: 'Tardó' },
    { clave: 'nivel', titulo: 'Nivel' },
    { clave: 'replicas', titulo: 'Réplicas exigidas (producción)' },
    { clave: 'porQue', titulo: 'Por qué ese nivel' },
  ]
}
