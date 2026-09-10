import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core'
import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { provideRouter, withComponentInputBinding } from '@angular/router'
import { routes } from './app.routes'
import { erroresInterceptor } from './nucleo/errores.interceptor'
import { idempotenciaInterceptor } from './nucleo/idempotencia.interceptor'
import { sesionInterceptor } from './nucleo/sesion.interceptor'
import { trazaInterceptor } from './nucleo/traza.interceptor'
import { GATEWAY, gatewayPorDefecto } from './nucleo/gateway'

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    // Los query params son entradas del componente: el estado de una tabla vive en la URL.
    provideRouter(routes, withComponentInputBinding()),
    // Ningún componente inyecta HttpClient: la red pasa por estos interceptores y por dominio/.
    provideHttpClient(withInterceptors([trazaInterceptor, sesionInterceptor, idempotenciaInterceptor, erroresInterceptor])),
    { provide: GATEWAY, useValue: gatewayPorDefecto() },
  ],
}
