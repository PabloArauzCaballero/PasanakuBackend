import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Campo } from '@aportaya/ui/campo/campo'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { CodigoCopiable } from '@aportaya/ui/codigo-copiable/codigo-copiable'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import type { ErrorDeGobierno } from '../dominio/errores-de-gobierno'
import {
  accionesDeCuentasDeServicio,
  cuentasDeServicio,
  type CuentaDeServicio,
  type SalidaDeRotacion,
} from '../dominio/gobierno-de-roles'
import { DialogoDeActo } from '../usuarios-internos/dialogo-de-acto'

const PROPOSITO = 'Las identidades con las que un servicio le habla a otro: quién las registró, cuándo vencen, cuándo cambió su clave, y rotarlas o revocarlas con segundo factor.'

/**
 * Cuentas de servicio (GU-18, CU-136). La credencial nueva se muestra UNA vez, en un aviso que se descarta al cerrarlo:
 * esta pantalla no la guarda en el almacenamiento del navegador ni en la URL, y el listado nunca la trae.
 */
@Component({
  selector: 'ap-pantalla-cuentas-de-servicio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, Campo, ChipEstado, CodigoCopiable, DialogoDeActo, EstadoDePantalla, Fecha, TablaDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas · Personal y accesos" titulo="Cuentas de servicio" [descripcion]="proposito">
        <ap-boton acciones variante="primario" (pulsado)="abrirRegistro()">Registrar cuenta</ap-boton>
        <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="recurso.isLoading()" (pulsado)="recurso.reload()">Actualizar</ap-boton>
      </ap-cabecera-de-pagina>

      @if (entregada(); as e) {
        <ap-alerta tono="aviso" titulo="Copiá la credencial ahora: no se vuelve a mostrar">
          <ap-codigo-copiable [valor]="e.credencial" nombre="credencial de la cuenta de servicio" />
          <p class="sub">Vence el <ap-fecha [valor]="e.venceEn" />. Se cerraron {{ e.sesionesCerradas }} {{ e.sesionesCerradas === 1 ? 'sesión' : 'sesiones' }}. El servicio tiene que recibir esta clave antes de su próximo ingreso.</p>
          <ap-boton variante="secundario" (pulsado)="entregada.set(null)">Ya la guardé</ap-boton>
        </ap-alerta>
      }
      @if (aviso(); as a) { <ap-alerta tono="ok" titulo="Listo">{{ a }}</ap-alerta> }

      <ap-estado-de-pantalla [recurso]="recurso" [vacio]="estaVacio" mensajeVacio="Todavía no hay cuentas de servicio registradas. Registrá una cuenta ya aprovisionada para ponerle nombre y vencimiento, y poder rotar su clave." accionVacio="Registrar cuenta" etiquetaDeCarga="Cargando cuentas de servicio" (reintentar)="recurso.reload()" (actuarEnVacio)="abrirRegistro()">
        <ap-tabla-de-datos titulo="Cuentas de servicio" [columnas]="columnas" [filas]="cuentas()" [totalDeRegistros]="cuentas().length" [cabecera]="true" textoRegistros="Cuentas" textoVacio="Sin cuentas.">
          <ng-template #celda let-c let-col="columna">
            @switch (col.clave) {
              @case ('estado') { <ap-chip-estado [tono]="c.estado === 'ACTIVA' ? 'ok' : 'neutro'">{{ c.estado === 'ACTIVA' ? 'Activa' : 'Revocada' }}</ap-chip-estado> }
              @case ('venceEn') { <ap-fecha [valor]="c.venceEn" /> }
              @case ('credencialRotadaEn') { <ap-fecha [valor]="c.credencialRotadaEn" /> }
              @case ('rolCodigo') { <code>{{ c.rolCodigo }}</code> }
              @case ('id') {
                @if (c.estado === 'ACTIVA') {
                  <div class="acciones">
                    <ap-boton variante="secundario" tamano="sm" (pulsado)="paraRotar.set(c)">Rotar clave</ap-boton>
                    <ap-boton variante="peligro" tamano="sm" (pulsado)="paraRevocar.set(c)">Revocar</ap-boton>
                  </div>
                } @else { <span class="sub">Sin acciones: se registra otra cuenta.</span> }
              }
              @default { {{ c[col.clave] }} }
            }
          </ng-template>
        </ap-tabla-de-datos>
      </ap-estado-de-pantalla>

      @if (registrando()) {
        <ap-dialogo-de-acto titulo="Registrar cuenta de servicio" textoDeConfirmar="Registrar" [valido]="registroValido()" [maximoDeMotivo]="160" efecto="La cuenta queda bajo gobierno con su nombre y vencimiento. Tiene que existir y portar un rol SERVICIO_*." [accion]="registrar" [(error)]="errorDelRegistro" [abierto]="true" (abiertoChange)="!$event && registrando.set(false)" (completado)="alTerminar('La cuenta quedó registrada.')">
          <div class="campos">
            <ap-campo etiqueta="Identificador de la cuenta (usuario)" [(valor)]="usuarioId" [error]="errorDe('rol')" />
            <ap-campo etiqueta="Nombre" [(valor)]="nombre" ayuda="Para reconocerla: por ejemplo, «Servicio de entregas»." />
            <ap-campo etiqueta="Vence el" tipo="date" [(valor)]="vence" [error]="errorDe('vigencia')" ayuda="Dentro de la vigencia máxima configurada." />
          </div>
        </ap-dialogo-de-acto>
      }
      @if (paraRotar(); as c) {
        <ap-dialogo-de-acto [titulo]="'Rotar la clave de ' + c.nombre" textoDeConfirmar="Rotar clave" [maximoDeMotivo]="160" efecto="Se genera una clave nueva que verás una sola vez. La anterior deja de servir y se cierran las sesiones de la cuenta." [accion]="rotar(c)" [abierto]="true" (abiertoChange)="!$event && paraRotar.set(null)" (completado)="alRotar($event)" />
      }
      @if (paraRevocar(); as c) {
        <ap-dialogo-de-acto [titulo]="'Revocar ' + c.nombre" textoDeConfirmar="Revocar cuenta" [destructivo]="true" [maximoDeMotivo]="160" efecto="La cuenta pierde su rol, su clave vence, se suspende al usuario y se cierran sus sesiones. No se puede reactivar: se registra otra." [accion]="revocar(c)" [abierto]="true" (abiertoChange)="!$event && paraRevocar.set(null)" (completado)="alTerminar('La cuenta quedó revocada.')" />
      }
    </section>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .campos { display: grid; gap: var(--s3); }
    .acciones { display: flex; flex-wrap: nowrap; gap: var(--s2); }
    .sub { margin: var(--s2) 0; font: var(--t-cuerpo-chico); color: var(--text-2); }
    code { font-family: var(--f-mono); font: var(--t-ayuda); overflow-wrap: anywhere; }
  `,
})
export class PantallaCuentasDeServicio {
  constructor() {
    inject(ExplicadorDeVista).fijar(PROPOSITO)
  }

  protected readonly proposito = PROPOSITO
  private readonly acciones = accionesDeCuentasDeServicio()
  protected readonly recurso = cuentasDeServicio()
  protected readonly cuentas = computed(() => (this.recurso.hasValue() ? this.recurso.value() : []))
  protected readonly estaVacio = (l: CuentaDeServicio[]) => l.length === 0
  protected readonly aviso = signal<string | null>(null)
  protected readonly entregada = signal<SalidaDeRotacion | null>(null)
  protected readonly registrando = signal(false)
  protected readonly paraRotar = signal<CuentaDeServicio | null>(null)
  protected readonly paraRevocar = signal<CuentaDeServicio | null>(null)
  protected readonly usuarioId = signal('')
  protected readonly nombre = signal('')
  protected readonly vence = signal('')
  protected readonly errorDelRegistro = signal<ErrorDeGobierno | null>(null)
  protected readonly registroValido = computed(
    () => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(this.usuarioId().trim()) && this.nombre().trim().length > 0 && this.vence() !== '',
  )
  protected readonly columnas: Columna<CuentaDeServicio>[] = [
    { clave: 'nombre', titulo: 'Cuenta', ordenable: true },
    { clave: 'rolCodigo', titulo: 'Rol de servicio' },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'venceEn', titulo: 'Vence' },
    { clave: 'credencialRotadaEn', titulo: 'Última rotación' },
    { clave: 'id', titulo: 'Acciones' },
  ]

  protected errorDe(campo: 'rol' | 'vigencia'): string | undefined {
    const e = this.errorDelRegistro()
    return e?.campo === campo ? e.mensaje : undefined
  }
  protected abrirRegistro(): void {
    this.usuarioId.set('')
    this.nombre.set('')
    this.vence.set('')
    this.errorDelRegistro.set(null)
    this.registrando.set(true)
  }
  protected readonly registrar = (evidenciaMfa: string, motivo: string) =>
    this.acciones.registrar({ usuarioId: this.usuarioId().trim(), nombre: this.nombre().trim(), venceEn: `${this.vence()}T23:59:59Z`, motivo, evidenciaMfa })
  protected rotar(c: CuentaDeServicio) {
    return (evidenciaMfa: string, motivo: string) => this.acciones.rotar(c.id, { motivo, evidenciaMfa })
  }
  protected revocar(c: CuentaDeServicio) {
    return (evidenciaMfa: string, motivo: string) => this.acciones.revocar(c.id, { motivo, evidenciaMfa })
  }
  protected alRotar(salida: unknown): void {
    this.entregada.set(salida as SalidaDeRotacion)
    this.aviso.set(null)
    this.recurso.reload()
  }
  protected alTerminar(mensaje: string): void {
    this.aviso.set(mensaje)
    this.recurso.reload()
  }
}
