import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Boton } from '@aportaya/ui/boton/boton';
import { Campo } from '@aportaya/ui/campo/campo';
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado';
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla';
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos';
import type { EntradaBusquedaParticipantes } from 'clientes/angular/identidad/model/entradaBusquedaParticipantes';
import type { PaginaDeParticipantes } from 'clientes/angular/identidad/model/paginaDeParticipantes';
import type { ParticipanteResumen } from 'clientes/angular/identidad/model/participanteResumen';
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista';
import { MarcoDeCumplimiento } from '../compartido/marco';
import { directorioDe, directorioVacio, justificacionValida } from '../dominio/cu134-participantes';
import { textosParticipantes } from '../textos-participantes';

const T = textosParticipantes;
const COLUMNAS: Columna<ParticipanteResumen>[] = [
  { clave: 'codigoPublico', titulo: T.columnas.codigoPublico },
  { clave: 'nombreAbreviado', titulo: T.columnas.nombreAbreviado },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'nivelKyc', titulo: T.columnas.nivelKyc },
  { clave: 'tieneMedidaEnCurso', titulo: T.columnas.marcas },
];

/**
 * CU-134 · directorio de participantes. Datos mínimos (código, nombre abreviado, estado y marcas), sin contacto: el
 * contacto enmascarado está en la ficha. Buscar exige justificación; sin ella no sale ninguna petición. El permiso lo
 * vuelve a decidir el servidor (`PARTICIPANTES_LEER`).
 */
@Component({
  selector: 'ap-directorio-de-participantes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MarcoDeCumplimiento,
    Campo,
    Boton,
    EstadoDePantalla,
    TablaDeDatos,
    ChipEstado,
    RouterLink,
  ],
  template: `
    <ap-marco-de-cumplimiento [titulo]="t.titulo" [proposito]="t.proposito" icono="persona">
      <form class="busqueda" (submit)="$event.preventDefault(); buscar()">
        <ap-campo [etiqueta]="t.codigoPublico" [(valor)]="codigo" autocompletar="off" />
        <ap-campo [etiqueta]="t.telefono" tipo="tel" [(valor)]="telefono" autocompletar="off" />
        <ap-campo
          class="ancho"
          [etiqueta]="t.justificacion"
          [ayuda]="t.ayudaJustificacion"
          [(valor)]="justificacion"
          [error]="errorJustificacion()"
        />
        <ap-boton
          variante="primario"
          tipo="submit"
          icono="buscar"
          [cargando]="directorio.isLoading()"
          >{{ t.buscar }}</ap-boton
        >
      </form>
      @if (consulta()) {
        <ap-estado-de-pantalla
          [recurso]="directorio"
          [vacio]="vacio"
          [mensajeVacio]="t.vacio"
          motivoVacio="porFiltro"
          [etiquetaDeCarga]="t.cargando"
          (reintentar)="directorio.reload()"
        >
          @if (directorio.hasValue() && directorio.value(); as pagina) {
            <ap-tabla-de-datos
              [titulo]="t.tabla"
              [columnas]="COLUMNAS"
              [filas]="pagina.filas"
              [identidad]="identidad"
            >
              <ng-template #celda let-p let-columna="columna">
                @switch (columna.clave) {
                  @case ('codigoPublico') {
                    <a
                      class="enlace"
                      [routerLink]="['/cumplimiento/participantes', p.usuarioId]"
                      [attr.aria-label]="t.abrir + ' ' + p.codigoPublico"
                      >{{ p.codigoPublico }}</a
                    >
                  }
                  @case ('estado') {
                    <ap-chip-estado [tono]="p.estado === 'ACTIVO' ? 'ok' : 'aviso'">{{
                      estado(p.estado)
                    }}</ap-chip-estado>
                  }
                  @case ('tieneMedidaEnCurso') {
                    {{ marcas(p) }}
                  }
                  @default {
                    {{ p[columna.clave] }}
                  }
                }
              </ng-template>
            </ap-tabla-de-datos>
          }
        </ap-estado-de-pantalla>
      } @else {
        <p class="indicacion">{{ t.sinBuscar }}</p>
      }
    </ap-marco-de-cumplimiento>
  `,
  styles: `
    .enlace { color: var(--brand-texto); font-weight: 600; text-decoration: none; }
    .enlace:hover { text-decoration: underline; }
    .enlace:focus-visible { outline: var(--borde-foco) solid var(--brand); outline-offset: var(--borde-desfase); }
    .busqueda {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
      gap: var(--s3);
      align-items: end;
    }
    .ancho {
      grid-column: 1 / -1;
    }
    .indicacion {
      margin: 0;
      color: var(--text-2);
    }
  `,
})
export class DirectorioDeParticipantes {
  constructor() {
    inject(ExplicadorDeVista).fijar(T.proposito);
  }
  protected readonly t = T;
  protected readonly COLUMNAS = COLUMNAS;
  protected readonly codigo = signal('');
  protected readonly telefono = signal('');
  protected readonly justificacion = signal('');
  protected readonly errorJustificacion = signal<string | undefined>(undefined);
  protected readonly consulta = signal<EntradaBusquedaParticipantes | null>(null);
  protected readonly directorio = directorioDe(this.consulta);
  protected readonly vacio = (p: PaginaDeParticipantes) => directorioVacio(p);
  protected readonly identidad = (p: ParticipanteResumen) => p.usuarioId;
  protected readonly estado = (codigo: string) => T.estados[codigo] ?? codigo;

  protected marcas(p: ParticipanteResumen): string {
    const lista = [
      p.tieneMedidaEnCurso ? T.marcas.medida : null,
      p.tieneBloqueo ? T.marcas.bloqueo : null,
      p.tieneRestriccion ? T.marcas.restriccion : null,
    ].filter((m) => m !== null);
    return lista.length ? lista.join(' · ') : T.marcas.ninguna;
  }

  protected buscar(): void {
    if (!justificacionValida(this.justificacion())) {
      this.errorJustificacion.set(T.errorJustificacion);
      return;
    }
    this.errorJustificacion.set(undefined);
    const cuerpo: EntradaBusquedaParticipantes = {
      justificacion: this.justificacion().trim(),
      limite: 20,
    };
    if (this.codigo().trim()) cuerpo.codigoPublico = this.codigo().trim();
    if (this.telefono().trim()) cuerpo.telefonoE164 = this.telefono().trim();
    this.consulta.set(cuerpo);
  }
}
