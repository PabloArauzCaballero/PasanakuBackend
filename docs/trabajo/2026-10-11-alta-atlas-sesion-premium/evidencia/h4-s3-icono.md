# H4.S3.M5 — decisión visual del icono de CI

Fecha: 2026-10-12. Captura del widget real `PasoDocumento`, con las fuentes de la app, viewport 360×800 y DPR 1. La prueba nueva monta tanto el carril de anverso como los datos del carnet, captura la superficie y comprueba ausencia del icono de tarjeta bancaria.

Comando desde `apps/movil`:

```text
flutter test --concurrency=1 --reporter expanded test/identidad/h4_ci_icono_visual_test.dart --timeout 60s
00:01 +1: All tests passed!
EXIT_CODE=0
```

| Captura | Observación |
|---|---|
| [anverso-360.png](./h4-s3-icono/anverso-360.png) | El icono del anverso dibuja una credencial con silueta de persona y líneas de datos; está acompañado por «Frente», «Anverso del carnet» y el texto de encuadre del carnet. |
| [numero-ci-360.png](./h4-s3-icono/numero-ci-360.png) | El campo «Número de CI» repite la credencial con silueta y renglones; no muestra banda, chip ni el patrón de tarjeta bancaria. |

**Decisión:** conservar `Icons.badge_outlined`. En estas capturas se reconoce como documento de identidad, así que no se cumple la condición para dibujar un `CustomPainter` propio. No se añade otra variante decorativa.
