# Reporte — La ruleta del sorteo, con ceremonia

> **AVANCE: 5 / 5 — 100 %.** El sorteo de turnos (CU-60) se pone en escena con una ruleta nueva. El orden lo sigue decidiendo el servidor (semilla sellada); la ruleta solo lo muestra.

- Fecha: 2026-10-29 · Rama: `claude/sorteo-ruleta`
- Peldaño: **TESTED** + prueba visual (video de la ceremonia completa a 2×, claro y oscuro). No VERIFIED en dispositivo.

## Completado
| ID | Qué se logró | Verificación |
|---|---|---|
| R1 | Ruleta nueva: gajos con luz radial por persona, clavijas, aro verde con 24 focos que corren, vidrio quieto, centro con el puesto en juego, aguja de gota con aleta | capturas |
| R2 | `OrquestaDelSorteo`: amague, giro largo que frena (después, cortos), la aguja golpea cada clavija con háptico, el gajo ganador brilla, pausa entre puestos y cierre con los focos parpadeando. Un solo `Ticker` que se apaga quieto | `detalle_del_grupo_test` 12/12 (incluye movimiento reducido y rechazo del servidor) |
| R3 | Cartel por puesto que entra con resorte; si sos vos: «¡Te tocó el N.º turno!» y al terminar queda el tuyo | capturas |
| R4 | El orden de cobro usa el color del gajo de cada persona (antes, el de la posición: no coincidía con la ruleta) y cada fila entra al salir | capturas |
| R5 | Gate y regresión | `dart analyze` sin issues · `verificar_frontend.py movil` TODO OK · app +1129 |

## A medias
Ninguna.

## Pendiente
Ninguno.

## Evidencia
- `evidencia/ruleta-claro.mp4`, `evidencia/ruleta-oscuro.mp4`: la ceremonia entera (24 s, 824×1800, 24 cuadros/s). Se generan con `test/capturas/movimiento/ruleta_del_sorteo_test.dart`.
- `evidencia/cuadros-{claro,oscuro}.png`: momentos clave. `evidencia/primera-version-claro.png`: la primera versión, donde se vieron los colores cruzados y las sombras pesadas que se corrigieron.

## No cubierto
- Dispositivo real (rendimiento del desenfoque de los focos en gama baja, sensación del háptico).
- Con muchos integrantes (más de 12) los nombres se achican; no se filmó ese caso.

## Decisiones
- Movimiento reducido: no gira; el orden aparece entero (como antes).
- Los focos solo se mueven durante el giro y el cierre: quieta, la pantalla no pide cuadros.
