import { afterNextRender, ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { CabeceraDePagina } from '../../layout/cabecera-de-pagina'
import { EnlaceALaApp } from '../../layout/enlace-a-la-app'
import { Icono } from '../../layout/icono'
import { leerEnlace, retirarSecretoDeLaUrl, SecretoDeInvitacionEnMemoria, type LecturaDeEnlace } from './token-de-invitacion'

type Vista = 'leyendo' | LecturaDeEnlace['estado']

/**
 * La puerta de un enlace de invitación (`/invitacion/<grupo>/<invitación>#t=<secreto>`).
 *
 * Lo que hace, en orden y apenas carga: lee el secreto del enlace, **lo retira de la barra de
 * direcciones y del historial** y lo deja solo en memoria. Aceptar la invitación NO hace entrar a
 * nadie al grupo: pide lugar, y la admisión la resuelve una persona de AportaYa.
 *
 * Es estática (se prerenderiza sin datos) y no se indexa: la ruta está en `RUTAS_NO_INDEXABLES`.
 * El canje contra el servidor exige la sesión de quien recibe la invitación, y este sitio no tiene
 * sesión de participantes: sigue en la app. Acá no se simula.
 */
@Component({
  selector: 'ap-invitacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pagina' },
  imports: [RouterLink, CabeceraDePagina, EnlaceALaApp, Icono],
  template: `
    <main id="contenido">
      <ap-cabecera-de-pagina etiqueta="Invitación" titulo="Te invitaron a un grupo de AportaYa" [bajada]="bajada()" />
      <div class="contenedor contenedor--angosto cuerpo-pagina">
        @switch (vista()) {
          @case ('leyendo') {
            <p class="texto-guia" role="status">Preparando tu invitación…</p>
          }
          @case ('listo') {
            <p class="texto-guia" role="status">Tu invitación está lista. Sacamos el código secreto de la dirección para que no quede a la vista ni en el historial.</p>
            <article class="prosa">
              <h2>Qué pasa si la aceptás</h2>
              <ul>
                <li>Pedís lugar en el grupo: no entrás automáticamente.</li>
                <li>Una persona del equipo de AportaYa revisa tu solicitud y te avisa.</li>
                <li>Para seguir, ingresá con el teléfono al que te mandaron la invitación.</li>
              </ul>
            </article>
            @if (enConsulta()) {
              <div class="aviso" role="note">
                <span class="signo"><ap-icono nombre="alerta" [tamano]="17" [grosor]="2.2" /></span>
                <div>
                  <div class="t">El enlace traía el código en un lugar poco seguro</div>
                  <div class="m">Quedó fuera de la dirección, pero pedile a quien te invitó que te mande uno nuevo.</div>
                </div>
              </div>
            }
            <div class="acciones acciones--descarga"><ap-enlace-a-la-app /></div>
          }
          @case ('invalido') {
            <div class="aviso" role="alert">
              <span class="signo"><ap-icono nombre="alerta" [tamano]="17" [grosor]="2.2" /></span>
              <div>
                <div class="t">Este enlace no tiene un código de invitación válido</div>
                <div class="m">Pedile a quien te invitó que te lo mande de nuevo.</div>
              </div>
            </div>
            <p class="texto-guia"><a routerLink="/">Ir al inicio</a></p>
          }
          @default {
            <div class="aviso" role="alert">
              <span class="signo"><ap-icono nombre="alerta" [tamano]="17" [grosor]="2.2" /></span>
              <div>
                <div class="t">Este enlace no trae un código de invitación</div>
                <div class="m">Pedile a quien te invitó que te lo mande de nuevo.</div>
              </div>
            </div>
            <p class="texto-guia"><a routerLink="/">Ir al inicio</a></p>
          }
        }
      </div>
    </main>
  `,
})
export class Invitacion {
  private readonly secreto = inject(SecretoDeInvitacionEnMemoria)
  protected readonly vista = signal<Vista>('leyendo')
  protected readonly enConsulta = signal(false)
  protected readonly bajada = signal('Pedís lugar en el grupo; una persona revisa tu solicitud antes de que entres.')

  constructor() {
    // Solo en el navegador y después del primer pintado: el servidor no ve el fragmento y no tiene nada que retirar.
    afterNextRender(() => {
      const lectura = leerEnlace(window.location.hash, window.location.search)
      if (lectura.estado !== 'sin-token') {
        retirarSecretoDeLaUrl(window)
        this.enConsulta.set(lectura.enConsulta)
      }
      if (lectura.estado === 'listo') this.secreto.guardar(lectura.token)
      this.vista.set(lectura.estado)
    })
  }
}
