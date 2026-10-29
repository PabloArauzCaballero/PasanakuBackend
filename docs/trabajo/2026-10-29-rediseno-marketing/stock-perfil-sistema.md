# Stock — flujos `perfil` + `sistema` (rediseño con skills de marketing)

> **AVANCE: 8 / 8 pantallas revisadas — 7 con cambios, 1 sin cambios a propósito (ayuda).**

- Fecha: 2026-10-29 · Rama: `claude/marketing-perfil-sistema` (desde `origin/dev`, con el
  merge de #296 que pasó estas pantallas a `PantallaConCabecera`).
- Skills leídas: `copywriting`, `copy-editing`, `cro`, `onboarding`, `signup`,
  `marketing-psychology`, `product-marketing` (+ `.agents/product-marketing.md`),
  `ux-writing-microcopy`, `pasanaku-diseno`, `pasanaku-app-movil-ux`,
  `pasanaku-ui-componentes`.
- Diagnóstico (`copywriting`): situación **B**. Ya había voz propia (voseo, «plata», sin
  culpa), así que se restauró y se ajustó lo que fallaba; no se reescribió por gusto.
- Prueba social: **no hay datos reales** (`.agents/product-marketing.md` §Proof Points), así
  que no se agregó ninguna. Tampoco «gratis», plazos ni garantías.

## Pantallas

| Pantalla | Propósito | Acción principal | Problemas encontrados | Qué cambié | Skill | Qué NO cambié y por qué |
|---|---|---|---|---|---|---|
| perfil (`/identidad/perfil`) | Ver el estado de la cuenta y llegar a sus trámites | «Completar verificación» (si falta algo) | Jerga «Verificación de identidad (KYC)» y «expediente»; el error era un botón con la oración entera adentro («No pudimos consultar el estado. Reintentar»); la lista de lo que falta con mayúsculas en medio de la oración; rechazo remitía a «el motivo informado», que la pantalla no muestra; pistas del lector y literales sueltos en el widget | Título «Tu identidad»; error = `Alerta` (qué pasó + qué hacer) + botón «Reintentar»; «Para revisar tu identidad falta: foto de perfil izquierdo, … y fecha de vencimiento del documento.»; CTA «Completar verificación» (coincide con el título de la pantalla a la que lleva); rechazo: «Comunicate con soporte…»; textos a `textos_del_perfil.dart` | ux-writing-microcopy §3, copy-editing (claridad), cro (CTA) | Orden de secciones y fila «Ayuda» (la prueba de navegación la busca por texto y no había mejora medible); correo como `Campo` deshabilitado (contrato de su prueba) |
| verificacion (`/identidad/perfil/verificacion`) | Mandar las fotos o la fecha que faltan | «Tomar foto» / «Guardar fecha» | «Solo lo que falta» sin verbo; consejo genérico «Tomá la foto real con buena luz» igual para ambos lados; «Tu documento / Vencimiento del documento» repetido | «Mandá lo que falta» + quién decide y dónde se ve; consejo por lado tomado del alta («Girá la cabeza a tu izquierda, no el teléfono.»); «Tu carnet» / «Fecha de vencimiento»; motivo del botón apagado más concreto | copywriting (especificidad), signup (expectativas claras) | Estructura y lógica (archivo en 200 líneas, se tocó solo `foto_pendiente_kyc.dart` y textos) |
| dispositivos | Decidir si se confía en el teléfono | «Ir al inicio» | «Continuar» no decía adónde; el título «Este dispositivo» no planteaba la decisión | Título como pregunta «¿Este teléfono es solo tuyo?» + qué hacer si es compartido; CTA con su destino real (`/billetera/inicio`) | ux-writing-microcopy §2, marketing-psychology (Hick: una decisión) | La etiqueta «Confiar en este dispositivo por 30 días» (vive en `textos.dart`, de otro agente) — ver riesgo R2 |
| verificacion-profunda | Declaración PEP | «Confirmar respuesta» | `SegmentedButton` de Material crudo en lugar del átomo del sistema; «Continuar» no hace avanzar a ningún lado | `GrupoRadio` de `diseno_flutter`, sin opción premarcada; pregunta en `cuerpoFuerte`; CTA «Confirmar respuesta» | pasanaku-ui-componentes §1, brief regla 3 (sin casillas premarcadas) | Título de la cabecera y explicación (ya eran claras) |
| contrasena | Cambiar la contraseña | «Guardar contraseña nueva» | «Guardar» a secas | CTA con verbo + objeto; reglas al principio más legibles («necesita 8 caracteres o más…») | signup (requisitos por adelantado), copywriting (CTA) | Validación y campos — ver riesgo R1 |
| baja | Dar de baja la cuenta | «Dar de baja mi cuenta» → hoja «Confirmar baja» / «Cancelar» | El aviso era una sola oración larga en naranja; el botón de la pantalla y el de la hoja decían lo mismo | Consecuencia como título del aviso («Tu cuenta queda inhabilitada») y la condición como detalle; botón que nombra la acción; la hoja conserva «Cancelar» al lado. Sin culpa, sin ocultar ni la salida ni la baja | ux-writing-microcopy §6, brief regla 3 | Comportamiento de la hoja — ver riesgo R3 |
| notificaciones | Lista de avisos de la cuenta | (ninguna: es una lista) | «Estás al día» afirma algo que la bandeja no sabe; «Acá vas a ver todo lo que pase…, aunque se te escape una notificación» promete un feed que hoy no está conectado (el proveedor arranca vacío y nada lo llena) | «Todavía no tenés avisos» + «Esta lista junta los avisos sobre tu cuenta.»; «ahora / hace N min / hace N h» a `textos.dart` | onboarding (vacío que orienta), brief regla 2 | No se agregó botón al vacío: no hay una acción real que corresponda |
| ayuda | Tutoriales | «Comenzar» en el recomendado | Muchos controles antes del contenido (buscador, tema, 4 filtros) | **Nada** | copywriting («leave-it-alone test») | El texto ya es claro y honesto («Ningún tutorial mueve tu plata…»). Subir el recomendado sobre el buscador haría saltar el campo al escribir (el recomendado se oculta al filtrar); sacar filtros cambia comportamiento probado |

## Riesgos y decisiones pendientes (DECISION_REQUIRED, a confirmar con Pablo)

- **R1 — Contraseña no llega al servidor.** `ContrasenaNotifier.actualizar` solo guarda en
  memoria (`dominio/estado_cuenta.dart`); el endpoint no existe. El botón dice «Guardar
  contraseña nueva» y no hay confirmación posterior. No se agregó ningún «Tu contraseña se
  cambió».
- **R2 — «Confiar en este dispositivo por 30 días» no se envía.** `elegirConfianza` solo
  cambia estado local; nadie lo manda. El texto (de `textos.dart`) promete algo no
  implementado. No se agregó la promesa de «no te pedimos el código».
- **R3 — La baja no hace nada.** «Confirmar baja» en la hoja solo la cierra
  (`onConfirmar: () => Navigator.of(context).pop()`); `BajaNotifier.confirmarBaja` lanza
  `UnimplementedError`. El aviso «Tu cuenta queda inhabilitada» describe CU-09, no lo que
  pasa hoy. Hace falta decidir: conectar el endpoint o decir que la baja se pide a soporte.
- **R4 — Bandeja vacía siempre.** No hay CU que la llene; por eso el vacío ya no promete.
- **Propuesta al sistema de diseño:** `GrupoDeFilas`/`FilaDeAccionDePerfil` (filas de ajustes
  en una tarjeta) son genéricas y podrían ser una molécula de `diseno_flutter` si aparece un
  segundo uso.

## Verificación (después del merge de `origin/dev` con #296)

```text
dart analyze --fatal-infos lib test                → No issues found!
python3 scripts/verificar_frontend.py movil        → TODO OK
flutter test test/unidad … test/navegacion         → 15:14 +1116 -9: Some tests failed.
  (las 9 son «Pixel test failed» de goldens del perfil: h8_perfil_oscuro (1),
   h10_perfil_visual (7), h8_break_ui «perfil · correo de 150 caracteres» (1);
   regeneradas con --update-goldens y miradas)
flutter test h8_perfil_oscuro + h10_perfil_visual  → 00:07 +8: All tests passed!
flutter test h8_break_ui --plain-name «correo de 150 caracteres» → 00:03 +2: All tests passed!
capturas FLUJOS=perfil,sistema FASE=despues-marketing → 02:48 +240: All tests passed!
```

Prueba con texto esperado actualizado: `test/widget/pantalla_baja_test.dart` (el botón de la
pantalla pasó de «Confirmar baja» a «Dar de baja mi cuenta»; la hoja sigue diciendo
«Confirmar baja» y «Cancelar»).

No cubierto: la suite completa no se volvió a correr entera después de regenerar los goldens
(solo los tres archivos regenerados); sin prueba en dispositivo real.
