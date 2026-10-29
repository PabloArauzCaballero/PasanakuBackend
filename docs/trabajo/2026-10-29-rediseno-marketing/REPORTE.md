# Reporte — Rediseño de la app con las skills de marketing

> **AVANCE: 4 / 4 flujos — 100 %.** 46 pantallas auditadas; entrada/alta, billetera, grupos y perfil/avisos/ayuda rediseñados en copy, jerarquía y acción principal. En `dev` y `test`.

- Fecha: 2026-10-29 · PR: #297 (perfil/avisos/ayuda), #298 (billetera), #299 (grupos), #300 (entrada/alta) → `test` en #302.
- Skills: `copywriting`, `copy-editing`, `cro`, `onboarding`, `signup`, `marketing-psychology`, `product-marketing`, `ux-writing-microcopy` (de coreyhaines31/marketingskills) junto con `pasanaku-diseno`, `pasanaku-app-movil-ux`, `pasanaku-ui-componentes`.
- Peldaño: **TESTED** + prueba visual con el arnés de capturas. No VERIFIED en dispositivo.

## Cómo se hizo
1. **Stock «antes»**: el arnés `test/capturas/rediseno` fotografió las 46 pantallas del inventario sobre `dev` (1344 capturas: estados × 360/412/600 × claro/oscuro).
2. Cuatro agentes en paralelo, uno por flujo, con archivos disjuntos y un brief común con prohibiciones duras: nada inventado (cifras, testimonios, sellos), nada que el código no haga, sin patrones oscuros, comportamiento intacto.
3. Cada rama se mergeó con el `dev` del momento y la sesión principal corrió la suite completa antes de cada PR (app: +1125 a +1129 en verde, `dart analyze` sin issues, `verificar_frontend.py movil` TODO OK).
4. **Stock «después»**: las mismas 1344 capturas sobre el `dev` final.

## Completado
| Flujo | Inventario | Qué cambió (lo principal) |
|---|---|---|
| Entrada + alta | `stock-entrada-alta.md` | Portada: se quitó «el carnet hace falta para abrir la cuenta» (falso desde el 17/10) y «con la plata segura» (no comprobable). MFA con tildes y voseo; solo dice que mandó el correo si el servidor lo confirmó. |
| Billetera | `stock-billetera.md` | Recargar y retirar avisan que el saldo no cambia al instante; el retiro instruido ya no se pinta como «listo»; montos del servidor; «Ver extracto». |
| Grupos | `stock-grupos.md` | Se quitaron tres «te avisamos» que nadie envía; crear, invitar y unirse con entrada y motivo del botón apagado; «Aportar Bs X». |
| Perfil, avisos, ayuda | `stock-perfil-sistema.md` | Sin «KYC»; PEP con `GrupoRadio` sin preselección; baja sin culpa; vacío de avisos honesto. |

## A medias
Ninguna.

## Pendiente
| Tema | Estado | Quién decide |
|---|---|---|
| Baja: «Confirmar baja» solo cierra la hoja (`confirmarBaja` lanza `UnimplementedError`) | DECISION_REQUIRED | Pablo / producto |
| Contraseña: «Guardar» no llega a ningún servicio | DECISION_REQUIRED | Pablo / producto |
| «Confiar en este dispositivo 30 días» no se envía | DECISION_REQUIRED | Pablo / producto |
| Recarga: se crea la orden pero no se muestra cómo pagarla | DECISION_REQUIRED | Pablo / producto |
| Admisión humana: los textos dicen «decide el organizador» y resuelve el backoffice | DECISION_REQUIRED | Negocio |
| Nombre visible «Aporteo» / «AportaYa» (A5) | DECISION_REQUIRED | Pablo |
| «Confirmar contraseña» en el paso 1 del alta | DECISION_REQUIRED | Pablo |

## Evidencia
`evidencia/<flujo>-<n>-<tema>.png`: arriba «antes», abajo «después», estado «datos» a 412 dp, en claro y oscuro (18 hojas). Las 2688 capturas sueltas no se versionan.

## No cubierto
- Dispositivo real, lector de pantalla y cámara con permiso concedido.
- Pantallas de éxito de recarga, retiro, transferencia, «Grupo creado» y «Pedir unirme»: solo pruebas de widget (no están en el arnés).

## Decisiones
- Prueba social, «gratis» y urgencia: omitidas a propósito — no hay dato real que las respalde.
- Donde una prueba existente exigía algo (largo de texto, colores neutros), se cambió el código y no la prueba.
