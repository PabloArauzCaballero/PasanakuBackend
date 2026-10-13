# H2.S1 — reproducción de errores de alta/ingreso (2026-10-12)

Resultado: no reproducido. No se obtuvo una respuesta de aplicación/backend que permita observar un código HTTP de negocio. Esto es una limitación del entorno, no evidencia de que los errores de producto no existan.

| Superficie/acción | Texto visto | Código real | HTTP | Clasificación | Hipótesis |
|---|---|---|---:|---|---|
| Arranque de la reproducción | No se pudo iniciar el recorrido funcional: no consta la URL del backend de Pablo; el fallback local no está escuchando y no hay dispositivo móvil. | — | — | `ENVIRONMENT` | Nueva: falta URL/backend y dispositivo para reproducir; no asignable honestamente a E1–E7. |

Comprobaciones realizadas:

- `rg -n "API|Prism|4010|backend|gateway" .../NOTAS.md apps/movil/scripts packages/simulado` no encontró una URL de backend de Pablo; `packages/simulado` confirma `:4010` como mock local.
- `Get-NetTCPConnection -State Listen ...` filtrado a puertos 4010, 5433 y 4011 no devolvió listeners.
- `Test-Path packages/simulado/node_modules` y `Test-Path node_modules` devolvieron `False`; no se arrancó una instalación de dependencias porque no están disponibles en el checkout.
- `flutter devices` devolvió únicamente `Windows (desktop)` y `Chrome (web)`; no hay Android/iOS conectado ni emulador iniciado.

## H2.S1.M2 — auditoría de clasificación

La tabla contiene 1 fila observable y las 1/1 filas están clasificadas (`ENVIRONMENT`) y ligadas a una hipótesis nueva, no atribuible a E1–E7 sin evidencia. Esto satisface el CA literal de M2 para los registros actuales. No convierte la falta de entorno en PASS ni afirma que se hayan clasificado errores funcionales que no se pudieron observar.

No se ejecutó el alta o login contra un backend distinto al indicado por el plan ni se inventaron textos/códigos. No se generaron capturas porque no apareció ningún error funcional. Por instrucción del usuario no se pidió aclaración ni captura externa; H2.S1.M1 y M3 permanecen parciales hasta disponer del endpoint/dispositivo o una captura de referencia.
