# Cableado pendiente en `app.config.ts` (Richard)

`app.config.ts` no es de este carril (`justin/frontend/config`) — este documento entrega
la línea exacta a cambiar, probada con `TestBed`, sin tocar el archivo.

## `apps/backoffice/src/app/app.config.ts`

**Ya funciona sin cambios**: `gatewayPorDefecto()` (misma firma de siempre) ahora usa
`validarConfiguracion` internamente y nunca cae a `localhost` — la línea actual

```ts
{ provide: GATEWAY, useValue: gatewayPorDefecto() },
```

ya deja de tener el hallazgo original. **No hace falta que Richard toque nada para que
el kill-test 2 pase.**

Lo que sigue es opcional, para la parte que si depende de `app.config.ts`: mostrar
`ConfiguracionInvalida` en vez del shell cuando la configuración es inválida (hoy, con
la etiqueta faltante, `GATEWAY` queda en `''` y las pantallas mostrarían errores de red
uno por uno — funciona, pero no es la pantalla de bloqueo explícita que pide H3.S2).
Para eso:

1. Reemplazar la línea de arriba por:

   ```ts
   import { isDevMode } from '@angular/core'
   import { CONFIGURACION_GATEWAY, provideGateway } from './nucleo/gateway'
   // …
   ...provideGateway(isDevMode() ? 'desarrollo' : 'produccion'),
   ```

2. En `app.ts` (el componente raíz, tampoco de este carril), inyectar
   `CONFIGURACION_GATEWAY` y renderizar `<ap-configuracion-invalida />`
   (`apps/backoffice/src/app/nucleo/configuracion-invalida.ts`) en vez de
   `<router-outlet />` cuando `!configuracion.valida`. Esto es lo que hace que ninguna
   petición salga con una configuración inválida — hoy cada dominio la evita por su
   cuenta (URLs vacías fallan solas), pero no hay una pantalla de bloqueo explícita
   hasta que se haga este paso.

## `apps/web/src/app/app.config.ts`

Mismo patrón, mismos dos pasos, con `apps/web/src/app/nucleo/gateway.ts` y
`apps/web/src/app/nucleo/configuracion-invalida.ts`. La única diferencia es que en SSR
conviene pasar el modo explícito por variable de entorno en vez de `isDevMode()` puro
(`APORTAYA_MODO=produccion` en el proceso de Node), para que servidor y navegador
decidan lo mismo sin depender de cómo Angular detecta el modo en cada plataforma.

## `sistemas.routes.ts` — ya cableado, no requiere nada de Richard

`provideFuentesDeSistemas(detectarModoFuentesDeSistemas())` ya está en los
`providers` de la ruta padre de `sistemas` (`apps/backoffice/src/app/rutas/sistemas/
sistemas.routes.ts`) — no depende de `app.config.ts` porque el router de Angular
admite providers por ruta desde 15.2. Nada que Richard tenga que hacer acá.

## Cómo activar el modo demo (dato, no red)

Etiqueta `<meta name="aportaya-demo" content="true">` en el HTML del backoffice (SPA
sin servidor propio: solo la etiqueta, igual mecanismo que `aportaya-gateway`, ver
`detectarModoFuentesDeSistemas()` en `apps/backoffice/src/app/rutas/sistemas/dominio/
proveedor-fuentes.compartido.ts`). Quién la carga en cada despliegue es infra (Q-J4).
