import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core'
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http'
import { provideRouter, withComponentInputBinding } from '@angular/router'
import { provideClientHydration, withEventReplay, withIncrementalHydration } from '@angular/platform-browser'
import { routes } from './app.routes'
import { erroresInterceptor } from './nucleo/errores.interceptor'
import { trazaInterceptor } from './nucleo/traza.interceptor'
import { GATEWAY, gatewayPorDefecto } from './nucleo/gateway'

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    // Hidratación incremental: los verificadores se hidratan cuando se ven; el contenido no.
    provideClientHydration(withEventReplay(), withIncrementalHydration()),
    provideHttpClient(withFetch(), withInterceptors([trazaInterceptor, erroresInterceptor])),
    { provide: GATEWAY, useValue: gatewayPorDefecto() },
  ],
}
