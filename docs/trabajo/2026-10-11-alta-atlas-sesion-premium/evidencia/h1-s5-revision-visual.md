# Revisión visual H1.S5 (2026-10-12)

Capturas hechas con Flutter widget tests, `disableAnimations: true` para medir el estado final sin depender del tiempo de entrada, y fuentes Instrument/Bricolage cargadas desde `FontManifest.json` (la fuente sintética Ahem de Flutter no se considera evidencia visual).

| Captura | Viewport | Tema | Revisión |
|---|---:|---|---|
| `h1-candado-claro-360.png` | 360×780 | Claro | Marca centrada, título/detalle legibles, acciones completas y separadas; sin recorte. |
| `h1-candado-oscuro-360.png` | 360×780 | Oscuro | Contraste claro, acción biométrica primaria y reingreso distinguibles; sin recorte. |
| `h1-candado-claro-412.png` | 412×850 | Claro | Composición respirada y dos acciones visibles; sin desbordes. |
| `h1-candado-oscuro-412.png` | 412×850 | Oscuro | Colores semánticos conservados en tema oscuro; texto legible. |
| `h1-candado-claro-tablet.png` | 600×960 | Claro | Contenido acotado a 420 dp y centrado; no se estira a todo el ancho. |
| `h1-candado-oscuro-desktop.png` | 1000×900 | Oscuro | Panel centrado y acotado; composición estable en ancho amplio. |
| `h1-ingreso-caducada.png` | 412×850 | Claro | Aviso ámbar con icono y copy de duración; campos y acciones no quedan tapados. |
| `h1-ingreso-cerrada.png` | 412×850 | Claro | Aviso informativo azul, distinto del aviso de vencimiento; copy sin culpabilizar. |

También verifiqué en `capa_de_bloqueo_animacion_test.dart` entrada 220 ms, salida 160 ms y modo de movimiento reducido. Las capturas son del estado final y no representan el fotograma intermedio de la animación. La matriz no cubre hardware biométrico real ni tamaños menores a 360 dp.
