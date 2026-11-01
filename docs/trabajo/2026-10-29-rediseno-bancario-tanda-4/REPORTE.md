# Reporte — Rediseño bancario, tanda 4: turno, recargar y retirar

> **AVANCE: 4 / 4 — 100 %.** Turno, recargar y retirar pasan al lenguaje del rediseño; el turno deja de afirmar una verificación que no hacía. Análisis, gate, pruebas y goldens en verde.

- Fecha: 2026-10-29 · Rama: `claude/rediseno-bancario-tanda-4` · Plan de origen: `docs/trabajo/2026-10-21-rediseno-bancario/PLAN.md` (H6)
- Peldaño: **TESTED** + prueba visual con el arnés de capturas (Flutter 3.44.8). No VERIFIED en dispositivo.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| T4.M1 | Turno: riel y pasos del sorteo en tarjetas, explicación del compromiso y la semilla, botón «Comprobar el sorteo» a lo ancho (antes «Verificar afuera»). **Ya no muestra «El orden reproducido coincide con el guardado»**: la pantalla no reproduce nada; lo afirmaba solo porque la semilla estaba revelada. La comprobación real sigue en la verificación pública. | `flutter test` app | +1120 PASS |
| T4.M2 | Retirar: cuenta y código en una tarjeta con ayuda por campo; el botón apagado dice qué falta (monto, cuenta o los 6 números) | idem | PASS |
| T4.M3 | Recargar: el botón apagado dice «Escribí cuánto querés recargar.» | idem | PASS |
| T4.M4 | Gate, análisis y capturas | `dart analyze --fatal-infos lib` · `verificar_frontend.py movil` · arnés `FASE=tanda4 FLUJOS=grupos,billetera` | No issues · TODO OK · +660 PASS · `evidencia/antes-despues-{claro,oscuro}.png` (arriba antes, abajo después) |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Completar KYC | TODO | Próxima tanda |
| H4/H5 (campos con relleno suave, cabecera que se compacta, motion) | TODO | — |
| ¿Quitar «Confirmar contraseña» del paso 1 del alta? | DECISION_REQUIRED | Pablo |

## No cubierto
- Dispositivo real. «Cuenta de destino» sigue siendo texto libre: el selector de cuentas verificadas (CU-18) no existe; la ayuda no promete verificación.

## Decisiones
- Sin componentes nuevos: se reusan `Tarjeta`, `Campo.ayuda` y `Boton.motivoDeBloqueo`.
