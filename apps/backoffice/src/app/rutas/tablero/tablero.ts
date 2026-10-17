import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Sesion } from '../../nucleo/sesion'
import { BANDEJAS, lectorDeBandejas, type DefinicionDeBandeja } from './dominio/bandejas'
import { TarjetaDeBandeja, type EstadoDeTarjeta } from './tarjeta-de-bandeja'
import { textosTablero } from './textos'

type Acceso = { ruta: string; texto: string; permiso: string; descripcion: string }

/**
 * H1.S4.M1 (B2) · El punto de entrada tras iniciar sesión, en tres bloques:
 * 1. **Requiere tu acción**: una tarjeta por bandeja real que el permiso de la sesión habilita (sin permiso, la tarjeta no
 *    existe: ni se pide). Dice «hay pendientes» / «al día» con la hora de la consulta; nunca un total (AMB-B3).
 * 2. **Monitoreo**: los indicadores sin proyección publicada se declaran «no disponible», con el motivo.
 * 3. **Accesos**: los atajos de siempre por sección (`data-tutorial-id="tablero-accesos"` lo usan los tutoriales).
 * El rol oculta, no protege: cada bandeja vuelve a exigir su permiso en el servidor, y un 403 se muestra como error.
 */
@Component({
  selector: 'ap-tablero',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, ChipEstado, RouterLink, TarjetaDeBandeja],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <section class="pantalla">
      <h1>{{ t.titulo }}</h1>

      <section class="bloque" aria-labelledby="tablero-accion">
        <div class="cabecera">
          <h2 id="tablero-accion">{{ t.accion.titulo }}</h2>
          @if (visibles().length > 0) {
            <ap-boton [cargando]="consultando()" (pulsado)="actualizar()">{{ t.accion.actualizar }}</ap-boton>
          }
        </div>
        <p class="explicacion">{{ t.accion.explicacion }}</p>
        @if (visibles().length === 0) {
          <p role="status" class="vacio">{{ t.accion.sinBandejas }}</p>
        } @else {
          <ul class="grilla" data-tutorial-id="tablero-accion">
            @for (b of visibles(); track b.id) {
              <li><ap-tarjeta-de-bandeja [bandeja]="b" [estado]="estados()[b.id] ?? cargando" /></li>
            }
          </ul>
        }
      </section>

      <section class="bloque" aria-labelledby="tablero-monitoreo">
        <h2 id="tablero-monitoreo">{{ t.monitoreo.titulo }}</h2>
        <p class="explicacion">{{ t.monitoreo.explicacion }}</p>
        <ul class="grilla">
          @for (i of t.monitoreo.indicadores; track i.id) {
            <li class="indicador" [attr.data-indicador]="i.id">
              <h3>{{ i.titulo }}</h3>
              <ap-chip-estado tono="neutro">{{ t.monitoreo.noDisponible }}</ap-chip-estado>
              <p>{{ i.motivo }}</p>
            </li>
          }
        </ul>
      </section>

      <section class="bloque" aria-labelledby="tablero-accesos-titulo">
        <h2 id="tablero-accesos-titulo">{{ t.accesos.titulo }}</h2>
        <ul class="grilla accesos" data-tutorial-id="tablero-accesos">
          @for (a of accesos; track a.ruta) {
            @if (sesion.puede(a.permiso)) {
              <li>
                <a [routerLink]="'/' + a.ruta" [attr.data-tutorial-id]="'tablero-' + a.ruta">
                  <span class="texto">{{ a.texto }}</span>
                  <span class="descripcion">{{ a.descripcion }}</span>
                </a>
              </li>
            }
          }
        </ul>
      </section>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; gap: var(--s5); }
    h1, h2, h3 { margin: 0; }
    h2 { font-size: 1.15em; }
    h3 { font-size: 1em; }
    .bloque { display: grid; gap: var(--s3); }
    .cabecera { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--s3); }
    .explicacion, .vacio, .indicador p { margin: 0; color: var(--text-2); }
    .grilla { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr)); gap: var(--s4); }
    .indicador { display: grid; gap: var(--s2); align-content: start; padding: var(--s4); border: var(--borde-fino) dashed var(--border); border-radius: var(--r-lg); background: var(--surface); }
    .accesos a { display: block; min-height: var(--area-tactil); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); text-decoration: none; }
    .accesos a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .texto { display: block; font-weight: 600; color: var(--text); }
    .descripcion { display: block; margin-top: var(--s1); color: var(--text-2); font-size: .9em; }
  `,
})
export class Tablero {
  protected readonly sesion = inject(Sesion)
  protected readonly t = textosTablero
  protected readonly cargando: EstadoDeTarjeta = { tipo: 'cargando' }
  private readonly leer = lectorDeBandejas()

  /** Las bandejas que el permiso de la sesión habilita. Sin permiso, la tarjeta no se muestra ni se consulta. */
  protected readonly visibles = computed(() => BANDEJAS.filter((b) => b.permisos.some((p) => this.sesion.puede(p))))
  protected readonly estados = signal<Partial<Record<DefinicionDeBandeja['id'], EstadoDeTarjeta>>>({})
  protected readonly consultando = computed(() => Object.values(this.estados()).some((e) => e?.tipo === 'cargando'))

  protected readonly accesos: Acceso[] = [
    { ruta: 'tablero/financiero', texto: 'Tablero financiero', permiso: 'TABLERO_FINANCIERO_VER', descripcion: 'Dinero entrante y saliente, mora, fondo y turnos.' },
    { ruta: 'tablero/sistemas', texto: 'Tablero de sistemas', permiso: 'TABLERO_SISTEMAS_VER', descripcion: 'Tráfico, errores, latencia y endpoints del día.' },
    { ruta: 'tablero/catalogo-de-datos', texto: 'Catálogo de datos', permiso: 'TABLERO_SISTEMAS_VER', descripcion: 'Cada tabla con por qué existe, su servicio y sus relaciones.' },
    { ruta: 'tablero/comercial', texto: 'Tablero comercial', permiso: 'TABLERO_COMERCIAL_VER', descripcion: 'Altas, activos, embudo, grupos y retención.' },
    { ruta: 'operacion', texto: 'Operación', permiso: 'ver:operacion', descripcion: 'Billetera, cobranza, entregas y cierre.' },
    { ruta: 'operacion/estado', texto: 'Arquitectura y estado', permiso: 'ver:operacion', descripcion: 'Nivel de criticidad y réplicas por servicio.' },
    { ruta: 'operacion/retiros', texto: 'Aprobación de retiros', permiso: 'RETIRO_APROBAR', descripcion: 'Segunda firma de un retiro: autorizar o rechazar.' },
    { ruta: 'cumplimiento', texto: 'Cumplimiento', permiso: 'ver:cumplimiento', descripcion: 'UIF, alertas, casos y gobierno.' },
    { ruta: 'admisiones', texto: 'Solicitudes de ingreso', permiso: 'ver:admisiones', descripcion: 'Resolver quién entra a un grupo.' },
    { ruta: 'contabilidad', texto: 'Contabilidad', permiso: 'ver:contabilidad', descripcion: 'Períodos, presupuestos y estados financieros.' },
    { ruta: 'publicidad', texto: 'Publicidad', permiso: 'ver:publicidad', descripcion: 'Campañas y ofertas.' },
    { ruta: 'configuracion', texto: 'Configuración', permiso: 'ver:configuracion', descripcion: 'Tarifario, mensajería y reportes, de solo lectura.' },
    { ruta: 'sistemas', texto: 'Sistemas', permiso: 'ver:sistemas', descripcion: 'Salud de servicios, despliegues y respaldos.' },
  ]

  constructor() {
    this.actualizar()
  }

  /** Vuelve a consultar cada bandeja visible; cada tarjeta resuelve por su cuenta (una caída no tapa a las demás). */
  protected actualizar(): void {
    for (const bandeja of this.visibles()) {
      this.fijar(bandeja.id, this.cargando)
      void this.leer(bandeja).then((lectura) => this.fijar(bandeja.id, { ...lectura, consultadaEn: new Date().toISOString() }))
    }
  }

  private fijar(id: DefinicionDeBandeja['id'], estado: EstadoDeTarjeta): void {
    this.estados.update((actual) => ({ ...actual, [id]: estado }))
  }
}
