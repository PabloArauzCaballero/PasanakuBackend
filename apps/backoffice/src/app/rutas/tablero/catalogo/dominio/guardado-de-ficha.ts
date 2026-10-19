import { HttpClient } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import type { EntradaFichaDeGobierno, FichaDeGobierno } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../../nucleo/gateway'

/** CU-130 · `PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/gobierno` (`guardarFichaDeGobierno`). */
@Injectable({ providedIn: 'root' })
export class GuardadoDeFicha {
  private readonly http = inject(HttpClient)
  private readonly gateway = inject(GATEWAY)

  guardar(esquema: string, tabla: string, entrada: EntradaFichaDeGobierno): Promise<FichaDeGobierno> {
    return firstValueFrom(this.http.put<FichaDeGobierno>(`${this.gateway}/indicadores/catalogo-de-datos/${esquema}/${tabla}/gobierno`, entrada))
  }
}
