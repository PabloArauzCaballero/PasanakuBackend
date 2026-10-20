# Reporte — Límites de grupos, secciones en «Grupos» y privacidad de las solicitudes

> **AVANCE: 6 / 7 — 85,7 %.**

- Fecha: 2026-10-20 · Rama: `claude/limites-y-privacidad-20261020`
- Peldaño: `TESTED` (servidor con PostgreSQL real en GitHub, run 39481235333; app con pruebas de pantalla).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| M1 | Solicitudes: solo nombre completo (servidor no devuelve ni guarda teléfono/correo; app no los muestra) | webTest + `flutter test test/pasanaku/` | 103/103 · +78 |
| M2 | Tope de 4 participaciones (AP-CU69-15) al pedir, aceptar y canjear | `CU69LimiteDeParticipacionesTest` (GitHub) | PASS |
| M3 | `GET /grupos/mis-limites` | `GruposDetalleYFirmaWebTest.misLimites` | PASS |
| M4 | «Grupos» en dos secciones con contador y botones apagados al tope | `pantalla_mis_grupos_test.dart` | PASS |
| M5 | Orden de cobro: «Tú» en rojo para quien mira; nombre completo para los demás | `detalle_del_grupo_test.dart` | PASS |
| M6 | Nombre de la solicitud cuando el integrante no tiene nombre guardado | integrationTest (GitHub) | PASS |

## A medias
### M7 — APK y prueba en el teléfono
- Qué falta: construir el APK y verlo con cuentas reales.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Admisión por backoffice (AdmisionRepositorio) sin el tope | TODO | aplicar el mismo control al reservar por expediente |

## Evidencia
- GitHub run 39481235333: `:servicios:grupos:test`, `webTest`, `integrationTest` en verde.
- Local: webTest grupos 103/103; `flutter analyze` sin problemas; `flutter test test/pasanaku/` +78; bóveda TODO OK; criterios sin divergencias; reglas de frontend sin FALLA.

## No cubierto
- La vista en un teléfono real.

## Desvíos del plan
- Ninguno.

## Riesgos residuales
- Las columnas `telefono_contacto` y `correo_contacto` quedan en el modelo sin uso.

## Decisiones y ambigüedades
- Del dueño (2026-10-20): solicitudes solo con nombre completo; tope de participación 4; dos secciones con contador; «Tú» en rojo en el orden de cobro.
- Supuesto: el tope de 4 cuenta solo los grupos donde se participa SIN organizarlos (los organizados tienen su tope de 2). Confirmar con el dueño.
