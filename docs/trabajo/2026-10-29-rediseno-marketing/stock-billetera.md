# Stock — flujo `billetera` (rediseño con skills de marketing)

> **AVANCE: 5 / 5 pantallas** con cambios escritos, pruebas dirigidas y suite móvil en verde,
> y capturas «después» revisadas a 360 y 412 en claro y oscuro. La revisión independiente
> (`NO_SELF_APPROVAL`) queda para la sesión principal.

- Fecha: 2026-10-29 · Rama: `claude/marketing-billetera` (desde `origin/dev`)
- Skills leídas completas: `copywriting`, `copy-editing`, `cro`, `onboarding`, `signup`,
  `marketing-psychology`, `product-marketing` (+ `.agents/product-marketing.md`),
  `ux-writing-microcopy`, `pasanaku-diseno`, `pasanaku-app-movil-ux`, `pasanaku-ui-componentes`.
- Archivos: `apps/movil/lib/pantallas/billetera/` (sin tocar `resolver_cuenta.dart`),
  `comprobante_de_retiro.dart` nuevo, `test/billetera/comprobante_de_retiro_test.dart` nuevo.
  `alianzas/` sin cambios (sus rutas siguen vacías; no hay pantalla que rediseñar).
- Diagnóstico de copy (skill `copywriting`): **B**, hay voz real (voseo, «plata», regla 91.3 en
  los comentarios). Se restauró y se ajustó, no se reemplazó.
- Nada inventado: sin cifras, testimonios, sellos, «gratis» ni plazos. Todo número nuevo en
  pantalla (monto a acreditar, neto y costo del retiro, saldo después de transferir) sale de la
  respuesta del servidor (`SalidaRecarga.acreditara`, `SalidaRetiro.montoNeto/costoRetiro`,
  `SalidaTransferencia.saldoDespues`).

| Pantalla | Propósito | Acción principal | Problemas encontrados | Qué cambié | Skill aplicada | Qué NO se cambió y por qué |
|---|---|---|---|---|---|---|
| **inicio** (`/billetera/inicio`) | Ver cuánta plata tengo y operar | Aportar (destacada), Recargar, Retirar | El enlace «Todos» de Movimientos abría el **extracto** (CTA que no dice adónde va). Vacío de pasanakus: el título «Armá tu primer pasanaku» empujaba a crear, pero la acción principal es **unirse** (mensaje y CTA desalineados). Vacío de movimientos genérico. Error de movimientos sin salida clara y con `TextButton` crudo fuera del sistema. | «Todos» → **«Ver extracto»**. Título del vacío → **«Sumate a tu primer pasanaku»** y mensaje que orienta: «Si te invitaron, entrá con su enlace o QR.» (la pantalla de unirse acepta enlace y QR). Vacío de movimientos: «Tu primera recarga va a aparecer acá.» Error: «No pudimos cargar tus movimientos. Probá de nuevo en un momento.» + `Boton` variante enlace. | `cro` (CTA con destino concreto, una acción principal), `onboarding` (vacío que orienta a la primera acción), `ux-writing-microcopy` (error accionable), `pasanaku-ui-componentes` (primitiva existente) | Sin prueba social (no hay dato real). Sin nombre en el saludo (la pantalla no lo tiene a mano sin otra llamada). `custodia` intacto (decisión documentada: sin nombre de banco). El mensaje del vacío se acortó a propósito: la versión larga empujaba la lista a 200 % y rompía `h8_break_ui_test` (ver reporte). |
| **recargar** (`/billetera/recargar`) | Pedir una recarga de saldo | Recargar | El selector de medio no decía qué se elige. Nada explicaba, **antes** de tocar, que el saldo no cambia al instante. Confirmación: una oración larga como título y sin el importe. | Etiqueta **«Cómo vas a pagar»** con `Pregunta`. `Alerta` informativa: «El saldo no cambia al instante · Primero se crea la orden. La plata aparece en tu billetera cuando el banco confirma el pago.» Confirmación partida en título «Tu recarga quedó en camino.» + detalle «Te avisamos cuando el banco la confirme.» + **Monto a acreditar** que devuelve el servidor. | `cro` (objeción «¿cuándo tengo la plata?» antes del CTA), `copy-editing` (barrido 7, riesgo cero sin prometer plazos), `ux-writing-microcopy` | CTA sigue «Recargar» (verbo de la pantalla; un CTA con monto exigiría formatear fuera de `Monto`). Atajos Bs 50/100/200/500 intactos (comodidad, no regla). «Te avisamos» se conserva: lo respalda la plantilla aprobada `RECARGA_ACREDITADA` (`sql/60_semillas/15-eventos-y-plantillas.sql`). Sin plazo de acreditación (no hay fuente). |
| **retirar** (`/billetera/retirar`) | Sacar plata a una cuenta bancaria | Retirar | Nada avisaba que el monto queda retenido. El resultado se pintaba **verde y con «listo»** aunque el retiro solo está instruido (contradecía su propio comentario «Instruido no es llegó»). El costo del retiro no se mostraba. | `Alerta` previa: «El monto queda retenido · Lo separamos de tu saldo disponible hasta que el banco confirme el pago a tu cuenta.» Resultado en `ComprobanteDeRetiro` (archivo nuevo): tono **info** con ícono de espera, título «Tu retiro quedó instruido.» + detalle «El saldo retenido se libera cuando el banco confirma el pago.» (mismo sentido que antes), **Monto neto** y **Costo del retiro** del servidor, pasos sin cambios (paso en curso «En camino al banco»). | `marketing-psychology` (transparencia de costo, sin anclaje ni pérdida), `copy-editing` (barrido «Prove it»), regla 91.3 | El destino sigue como texto libre (hueco declarado CU-18). Campos, validación, MFA de 6 dígitos y clave de idempotencia intactos. |
| **transferir** (`/billetera/transferir`) | Pasar saldo a otra billetera | Transferir | Botón apagado **sin decir por qué** (regla de la casa). El campo decía «Alias» aun con «Grupo» elegido (el servidor espera el **id del grupo**, `ResolverDestino`). Selector sin etiqueta. Confirmación sin dato. | `motivoDeBloqueo` en orden: monto → destino → concepto. Etiqueta del selector «A quién le transferís». Campo dinámico: «Alias de quien recibe» / «Identificador del grupo». Confirmación «Transferencia hecha» + «Tu saldo después de transferir» con `saldoDespues` del servidor. | `signup`/`cro` (fricción: decir qué falta), `ux-writing-microcopy` (etiqueta que nombra el dato real) | Sigue sin enlace desde ninguna vista (ambigüedad A8 del rediseño, no se expone sin decisión). Contrato y validación intactos. |
| **extracto** (`/billetera/extracto`) | Ver saldo al cierre y movimientos de un período | Generar extracto | El CTA estaba arriba y el vacío que lo explica, en el medio, **sin acción**. Al cargar decía «Cargando tu saldo». Un período sin movimientos mostraba «Cuando recargues saldo…» (texto del inicio, fuera de contexto). | Sin período, el botón primario **«Generar extracto» va dentro del vacío** (una sola acción, junto a su explicación); con período, «Elegir otro período» arriba en secundario. Mensaje del vacío con el valor concreto: «Elegí las fechas y te mostramos tu saldo al cierre, cada movimiento y un certificado de saldo para compartir.» Carga: «Generando tu extracto». Período vacío: «No tuviste movimientos en esas fechas. Probá con otro período.» Error del detalle con `Boton` del sistema. | `onboarding` (vacío = primera acción), `cro` (CTA junto al argumento), `ux-writing-microcopy` (vacío por filtro ≠ vacío por nuevo) | El ancla de la guía `billetera.extracto` sigue en el botón en los dos estados. Se mantiene `showDateRangePicker` (cambiarlo es comportamiento). **No** se abre con el mes en curso, aunque la pestaña «Movimientos» llega acá (ver propuesta en el reporte). |

## Pruebas cuyo texto esperado se actualizó

- `test/widget/pantalla_recargar_test.dart`: la confirmación pasó de una oración a título +
  detalle; se espera «Tu recarga quedó en camino.» y «Te avisamos cuando el banco la confirme.»
  por separado. Las aserciones de una sola petición y la misma clave de idempotencia no cambiaron.

## Pruebas nuevas

- `test/billetera/comprobante_de_retiro_test.dart` (datos sintéticos): correcto (texto, neto,
  costo, paso «En camino», claro y oscuro a 360), límite (costo cero se muestra), inválido
  (`motivoTransferencia` nombra lo primero que falta).

## Propuestas (fuera de alcance, requieren decisión)

1. La pestaña «Movimientos» abre el extracto vacío pidiendo fechas: abrir con el mes en curso
   ahorraría un paso, pero dispara una consulta al entrar (cambio de comportamiento).
2. Las confirmaciones de recarga, retiro y transferencia no tienen botón de salida propio
   (solo el volver de la cabecera). Un «Volver a la billetera» sería la acción única de esa
   pantalla; es navegación nueva, no se agregó.
3. La recarga crea la orden pero la app no muestra cómo pagarla (QR o datos de transferencia
   de `SalidaRecarga`); vale revisar con producto qué devuelve el proveedor.

## Verificación (última línea literal)

```text
dart analyze --fatal-infos lib test                      → No issues found!
python3 scripts/verificar_frontend.py movil              → TODO OK
flutter test test/unidad … test/billetera test/navegacion → 12:21 +1129: All tests passed!
capturas FASE=despues-marketing FLUJOS=billetera         → 01:30 +150: All tests passed!
```

- Goldens regenerados (solo diferencia de píxeles por «Ver extracto»): 8 de
  `h10_billetera_visual_test` (principal y vacío, claro y oscuro, 412 y 600). Revisados a ojo.
- `h8_break_ui_test` «billetera · 40 movimientos · 200 %» falló con una primera versión más
  larga del vacío de pasanakus (la lista no llegaba a las filas en la pasada oscura). Se
  confirmó contra la base (pasa sin los cambios) y contra el texto viejo; se acortó el copy,
  no la prueba. La fragilidad de esa prueba ante el largo del texto queda anotada.
- No cubierto: los estados de confirmación de retiro y transferencia no están en el arnés de
  capturas (solo en prueba de widget); sin dispositivo real ni lector de pantalla.
