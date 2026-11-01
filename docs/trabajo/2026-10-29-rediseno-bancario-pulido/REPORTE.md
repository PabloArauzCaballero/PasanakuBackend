# Reporte — Rediseño bancario, pulido: campos, cabecera y animaciones

> **AVANCE: 6 / 8 — 75 %.** Campos con relleno suave y anillo de foco, cabecera grande que se compacta (en 3 pantallas) y saldo que se oculta con desenfoque. Queda: migrar las otras 19 pantallas a la cabecera nueva y el MP4 de la entrada de Inicio.

- Fecha: 2026-10-29 · Rama: `claude/rediseno-bancario-pulido` · Plan de origen: `docs/trabajo/2026-10-21-rediseno-bancario/PLAN.md` (H4.S1.M8, H4.S2.M6, H5.S2.M1, H5.S2.M7)
- Peldaño: **TESTED** + prueba visual (goldens, arnés y MP4 con Flutter 3.44.8 y fuentes reales). No VERIFIED en dispositivo.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| P.M1 | Campos (`Campo`, `CampoOTP`, tema de campos) con relleno suave (`surface2`); deshabilitado fundido con el fondo | `flutter test test/widget/campo_relleno_y_halo_test.dart` (diseno) | +3 PASS; el mutante «relleno blanco» falla |
| P.M2 | Foco con línea de marca de 2 px y anillo (`BordeConHalo`, token `halo`); con error, anillo de error. Se pinta desde el borde: abraza la caja, no la ayuda | idem + golden `atomos_campos_*` | PASS · `evidencia/campos-*.png` |
| P.M3 | `PantallaConCabecera`: título grande que, al desplazar, se recorta bajo la fila y deja el título chico junto a volver | `flutter test test/widget/pantalla_con_cabecera_test.dart` | +4 PASS (compacta; el contenido no queda debajo, medido; un solo encabezado para el lector; título largo al 200 % sin recorte — el mutante «alto fijo» falla) |
| P.M4 | Turno, verificar sorteo y transparencia dejan el `AppBar` de fábrica | arnés `FASE=pulido FLUJOS=grupos` | +510 PASS · `evidencia/cabecera-en-pantallas.png`, MP4 en `…/evidencia/movimiento/cabecera/` |
| P.M5 | Ocultar el saldo con desenfoque y fundido; con movimiento reducido, inmediato | `flutter test test/widget/encabezado_de_saldo_desenfoque_test.dart` | +4 PASS (sin `transitionBuilder` falla) · MP4 normal y reducido en `…/movimiento/saldo-oculto/` |
| P.M6 | Gate, análisis y regresión | `dart analyze --fatal-infos` · `verificar_frontend.py movil/diseno` · `flutter test` diseno (unidad, widget, goldens, a11y) y app | No issues · TODO OK · diseno +176 · app +1124 |

## A medias
### P.M7 — Cabecera nueva en el resto de las pantallas (H4.S2.M6)
- Qué anda: el componente, sus pruebas y su MP4; tres pantallas migradas.
- Qué no anda: 19 pantallas siguen con el `AppBar` de fábrica (lista: `grep -rln "AppBar(" apps/movil/lib`).
- Qué falta exactamente: migrarlas por franjas (identidad, billetera, el resto de pasanaku), con capturas por franja. Las que tienen el cuerpo dentro de `EstadoDePantalla` o un scroll propio se migran igual que transparencia.
- Dónde quedó: `packages/diseno_flutter/lib/organismos/pantalla_con_cabecera.dart`; compila; pruebas en verde.

### P.M8 — Entrada escalonada de Inicio (H5.S2.M1)
- Qué anda: desde 0922bade las filas del feed entran con 30 ms de paso.
- Qué no anda: el plan pedía «héroe → acciones → secciones». El héroe y las secciones no se escalonan, por decisión fijada en `pantalla_de_saldo_test` («la cifra y las secciones no esperan al escalonado del feed»).
- Qué falta exactamente: el MP4 y los 3 cuadros del DoD.
- Dónde quedó: sin cambios de código (el intento de escalonar las secciones se revirtió, ver Desvíos).

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| ¿Quitar «Confirmar contraseña» del paso 1 del alta? | DECISION_REQUIRED | Pablo |
| Resto de H4/H5 del plan (rodillo de cifra al refrescar, hero compartido, hoja interrumpible, etc.) | TODO | Próximas tandas |

## Evidencia
- `evidencia/campos-catalogo-antes-despues.png` (catálogo, antes/después, claro y oscuro), `evidencia/campos-foco-y-error.png`, `evidencia/campos-en-pantallas-antes-despues.png`.
- `evidencia/cabecera-cuadros.png` (0, 15, 30, 45 y 120 px), `evidencia/cabecera-en-pantallas.png`; MP4 en `docs/trabajo/2026-10-21-rediseno-bancario/evidencia/movimiento/cabecera/`.
- `evidencia/saldo-oculto-cuadros.png`; MP4 en `docs/trabajo/2026-10-21-rediseno-bancario/evidencia/movimiento/saldo-oculto/`.
- Los MP4 salen de cuadros densos (cada 16 ms / cada 3 px) del mismo arnés; los cuadros densos no se versionan.

## No cubierto
- Dispositivo real: rendimiento del desenfoque en gama baja y sensación del gesto de la cabecera.
- `test/capturas` de `diseno_flutter` (14 casos) **ya fallaba en `dev` antes de este trabajo** (verificado con el árbol limpio); no está en el comando del paquete y no se tocó.

## Desvíos del plan
- Primer intento de la entrada de Inicio: envolví las secciones en `AparicionEscalonada`. La prueba existente `pantalla_de_saldo_test` lo rechaza a propósito, y la guía mide el héroe apenas existe (`motor.dart:114`), así que un héroe en movimiento dejaría el resaltado corrido. Se revirtió; la prueba manda.
- Dos defectos encontrados en las propias capturas, antes de cerrar:
  1. La cabecera superponía el título grande con el chico a mitad del recorrido; ahora el grande se recorta en su franja.
  2. Al ocultar el saldo, la cifra que se iba mostraba importes falsos (Bs 0,00, Bs 945,88). Causa: el filtro entraba y salía del árbol, se recreaba la cifra y su rodillo arrancaba de cero. Ahora el filtro queda siempre y se apaga con `enabled`; hay prueba de regresión.

## Decisiones
- El relleno suave reusa `surface2` y el anillo el token `halo`, que existían y no usaba ningún campo; no se tocó `tokens.json`, que también alimenta la web.
- `PantallaConCabecera` usa `NestedScrollView`: las pantallas conservan su propio scroll y se conectan sin reescribirse. Se midió que el contenido no queda debajo de la cabecera fijada.
