# Diagnóstico — capturas «antes» (412 × 915, claro), origin/test 7ab476c5

Fuente: `flutter test test/capturas/rediseno/capturas_test.dart --update-goldens --dart-define=FASE=2026-10-30-antes --name "412 · claro"` → 229 PNG, exit 0.
Capturas en `docs/trabajo/2026-10-21-rediseno-bancario/evidencia/2026-10-30-antes/` (no se commitean: son 229 archivos).

## Defectos reales (de la app), por prioridad
| # | Pantalla | Defecto | Skill |
|---|---|---|---|
| 1 | Cambiar turno (permuta), Retirarme del grupo, Cambiar contraseña, Hacer un reclamo, Verificación adicional | El botón principal queda a mitad de pantalla, con media pantalla vacía debajo y fuera de la zona del pulgar; otras pantallas del mismo flujo (pedir cupo, invitar, unirse) lo anclan abajo con `BarraDeAccion`. Inconsistente. | cro (CTA visible), mobile-ux-design |
| 2 | Bienvenida tras el alta | Media pantalla vacía arriba, el panel flota al centro; solo ofrece «Ver mi billetera» y «Completar mi perfil», ninguno lleva al momento de valor (estar en un pasanaku). | onboarding (aha moment), visual-hierarchy |
| 3 | Tu billetera | «Hola» suelto sin nombre: se lee como dato faltante. | copywriting |
| 4 | Transparencia del grupo (vacío) | «Este grupo todavía no tiene bloques sellados»: jerga técnica, no dice qué va a aparecer ni cuándo. | copywriting, ux-writing-microcopy |

## Artefactos del arnés (NO son defectos de la app)
«nombre», «montoAporte moneda», «reglamento», «puntaje», «Niveldeconfianza», «correo», «hashEsperado…», «glosa»: son los valores de ejemplo de los contratos OpenAPI que contesta `adaptador_por_contrato.dart`. Con datos reales no aparecen. La cámara negra de «Escanear» es el arnés sin cámara.

## Revisado y sin cambio
Portada, tour, ingreso, MFA, registro, pautas, contrato, estados de trámite, recargar/retirar/transferir (CTA ya anclado), extracto, crear grupo, pedir cupo, invitar, solicitudes, sorteo, perfil, ayuda, notificaciones. El botón deshabilitado «contorno pálido» es deliberado (`packages/diseno_flutter/lib/atomos/boton.dart:66`), no se toca.
