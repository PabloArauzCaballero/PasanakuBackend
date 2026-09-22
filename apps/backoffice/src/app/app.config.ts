import { ApplicationConfig, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core'
import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { provideRouter, withComponentInputBinding } from '@angular/router'
import { routes } from './app.routes'
import { inicializarSesion } from './nucleo/auth-bootstrap'
import { erroresInterceptor } from './nucleo/errores.interceptor'
import { idempotenciaInterceptor } from './nucleo/idempotencia.interceptor'
import { registroDeAccesoInterceptor } from './nucleo/registro-de-acceso.interceptor'
import { sesionInterceptor } from './nucleo/sesion.interceptor'
import { trazaInterceptor } from './nucleo/traza.interceptor'
import { GATEWAY, gatewayPorDefecto } from './nucleo/gateway'
import { proveerTutoriales } from './rutas/ayuda/catalogo/proveer'

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    // Los query params son entradas del componente: el estado de una tabla vive en la URL.
    provideRouter(routes, withComponentInputBinding()),
    // Ningún componente inyecta HttpClient: la red pasa por estos interceptores y por dominio/.
    provideHttpClient(
      withInterceptors([trazaInterceptor, sesionInterceptor, idempotenciaInterceptor, registroDeAccesoInterceptor, erroresInterceptor]),
    ),
    { provide: GATEWAY, useValue: gatewayPorDefecto() },
    // Antes de montar cualquier ruta: intenta restaurar la sesión con la cookie de refresco.
    provideAppInitializer(inicializarSesion()),
    // El motor de tutoriales: catálogo, rutas válidas, almacén de progreso y bitácora.
    ...proveerTutoriales(),
  ],
}
