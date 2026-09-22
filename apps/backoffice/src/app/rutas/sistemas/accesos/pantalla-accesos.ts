import { ChangeDetectionStrategy, Component } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { Acceso, accesosSimulados } from '../dominio/datos-simulados'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-accesos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas" />
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; } h1 { margin-bottom: var(--s4); }`,
})
export class PantallaAccesos {
  protected readonly t = textosSistemas.accesos
  protected readonly filas = accesosSimulados
  protected readonly columnas: Columna<Acceso>[] = [
    { clave: 'persona', titulo: 'Persona', ordenable: true },
    { clave: 'rol', titulo: 'Rol' },
    { clave: 'ambito', titulo: 'Ámbito' },
    { clave: 'otorgadoEl', titulo: 'Otorgado el' },
  ]
}
