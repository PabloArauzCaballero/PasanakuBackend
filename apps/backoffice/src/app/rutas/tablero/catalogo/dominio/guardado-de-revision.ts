import { HttpClient } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import type { EntradaRevisionDeColumna, RevisionDeColumna } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../../nucleo/gateway'

/** CU-131 · `PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/columnas/{columna}/revision` (`revisarColumna`). */
@Injectable({ providedIn: 'root' })
export class GuardadoDeRevision {
  private readonly http = inject(HttpClient)
  private readonly gateway = inject(GATEWAY)

  guardar(esquema: string, tabla: string, columna: string, entrada: EntradaRevisionDeColumna): Promise<RevisionDeColumna> {
    return firstValueFrom(
      this.http.put<RevisionDeColumna>(`${this.gateway}/indicadores/catalogo-de-datos/${esquema}/${tabla}/columnas/${columna}/revision`, entrada),
    )
  }
}
