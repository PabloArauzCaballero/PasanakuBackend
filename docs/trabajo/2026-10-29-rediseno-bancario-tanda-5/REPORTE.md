# Reporte — Rediseño bancario, tanda 5: completar la verificación

> **AVANCE: 4 / 4 — 100 %.** «Completá tu verificación» pasa al lenguaje del rediseño, con sus estados de carga y error del sistema de diseño. Análisis, gate, pruebas y capturas en verde.

- Fecha: 2026-10-29 · Rama: `claude/rediseno-bancario-tanda-5` · Plan de origen: `docs/trabajo/2026-10-21-rediseno-bancario/PLAN.md` (H6)
- Peldaño: **TESTED** + prueba visual con el arnés de capturas (Flutter 3.44.8). No VERIFIED en dispositivo.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| T5.M1 | Título sin jerga («Completá tu verificación», antes «Completar verificación KYC»), entrada con `IntroDePantalla`, secciones «Fotos que faltan» y «Tu documento» en tarjetas | `flutter test` app | +1124 PASS |
| T5.M2 | Cada foto pendiente tiene un botón con nombre («Tomar foto»), no un ícono solo; foto y botón se leen juntos con lector de pantalla | idem | PASS |
| T5.M3 | Carga y error con `EstadoDePantalla` (antes un enlace suelto «No pudimos cargar tu expediente. Reintentar»); revisión cerrada con `EstadoVacio`; «todo enviado» como alerta; el botón apagado dice qué falta; el error de guardado aparece junto al botón | idem | PASS |
| T5.M4 | Gate, análisis y capturas | `dart analyze --fatal-infos lib test` · `verificar_frontend.py movil` · arnés `FASE=tanda5 FLUJOS=perfil` | No issues · TODO OK · +180 PASS · `evidencia/antes-despues-{claro,oscuro}.png`, `evidencia/peor-caso-360.png` |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H4/H5 (campos con relleno suave, cabecera que se compacta, motion) | TODO | — |
| ¿Quitar «Confirmar contraseña» del paso 1 del alta? | DECISION_REQUIRED | Pablo |

## No cubierto
- Dispositivo real y la cámara: la subida de fotos no se ejercitó (el arnés no tiene cámara).

## Desvíos del plan
- `test/identidad/pantalla_completar_kyc_errores_test.dart` falló tras el cambio. Causa reproducida y medida: la lista construye solo lo visible, y con la entrada y las secciones «Guardar fecha» quedó debajo del pliegue (viewport de prueba 800×600); al aparecer la alerta, la referencia quedó en y=616. La prueba ahora se desplaza hasta el botón y hasta la referencia como lo haría la persona; las aserciones son las mismas. En el producto, el error se movió junto al botón para que la persona lo vea al tocarlo.

## Decisiones
- Sin componentes nuevos en `diseno_flutter`. `FotosPendientesKyc` vive junto a la pantalla (la pantalla quedaba en 209 líneas, por encima del límite de 200).
- El consejo de la foto se mantiene igual («Tomá la foto real con buena luz»): no se agregan pautas que no estén definidas.
