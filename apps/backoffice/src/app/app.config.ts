import { ApplicationConfig, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core'
import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { provideRouter, withComponentInputBinding, withPreloading } from '@angular/router'
import { routes } from './app.routes'
import { inicializarSesion } from './nucleo/auth-bootstrap'
import { PrecargaSegunSesion } from './nucleo/precarga-segun-sesion'
import { INTERCEPTORES_DE_RED } from './nucleo/interceptores'
import { GATEWAY, gatewayPorDefecto } from './nucleo/gateway'
import { proveerTutoriales } from './rutas/ayuda/catalogo/proveer'

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    // Los query params son entradas del componente: el estado de una tabla vive en la URL. En reposo se precarga el
    // código de las secciones que la sesión puede abrir (y solo esas): el clic del menú ya no espera la descarga.
    provideRouter(routes, withComponentInputBinding(), withPreloading(PrecargaSegunSesion)),
    // Ningún componente inyecta HttpClient: la red pasa por estos interceptores y por dominio/.
    provideHttpClient(withInterceptors(INTERCEPTORES_DE_RED)),
    { provide: GATEWAY, useValue: gatewayPorDefecto() },
    // Antes de montar cualquier ruta: intenta restaurar la sesión con la cookie de refresco.
    provideAppInitializer(inicializarSesion()),
    // El motor de tutoriales: catálogo, rutas válidas, almacén de progreso y bitácora.
    ...proveerTutoriales(),
  ],
}
