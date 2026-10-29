# Stock — flujo `grupos` (pasada de marketing)

> **AVANCE: 11 / 18 pantallas con cambios · 18 / 18 auditadas.**
> Las 7 restantes se auditaron con capturas y se dejaron como estaban (motivo en su fila).

- Fecha: 2026-10-29 · Rama: `claude/marketing-grupos` (desde `origin/dev` 9da0d154)
- Alcance: `apps/movil/lib/pantallas/pasanaku/`, sus pruebas y goldens. Sin tocar
  `diseno_flutter`, identidad, billetera, notificaciones ni soporte.
- Skills leídas completas antes de tocar código: `copywriting`, `copy-editing`, `cro`,
  `onboarding`, `signup`, `marketing-psychology`, `product-marketing`
  (+ `.agents/product-marketing.md`), `ux-writing-microcopy`, `pasanaku-diseno`,
  `pasanaku-app-movil-ux`, `pasanaku-ui-componentes`, `referrals`.
- Prioridad por impacto en conversión: crear → invitar → unirse/pedir → aportar → resto.

## Plan (regla 20, versión corta)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| M1 | Copy y jerarquía de crear/invitar/unirse/pedir/aportar | Cada pantalla tiene entrada que dice qué pasa, CTA con verbo y motivo del botón apagado | `flutter test test/pasanaku test/widget` en verde | HECHO |
| M2 | Quitar promesas de avisos que el backend no manda | Ningún texto de este flujo dice «te avisamos»/«le avisamos» | `grep -rn "avisamos" lib/pantallas/pasanaku` sin resultados en textos usados | HECHO |
| M3 | Gate y suite completa | analyze, `verificar_frontend.py movil` y suite del brief en verde | salidas literales en el reporte final | ver reporte |
| M4 | Capturas «después» miradas | 360/412, claro/oscuro de las pantallas cambiadas revisadas | copiadas a `scratchpad/marketing/despues/grupos/` | ver reporte |

## Hallazgo que motivó M2 (con fuente)

`grupos.ingreso_solicitado` y `grupos.ingreso_aceptado` se emiten
(`servicios/grupos/.../CU68Postular.java:78`, `OrganizadorDecideIngreso.java`) pero **ningún
servicio los consume** (`grep` sin resultados en `servicios/notificaciones/src/main`). Por eso
«Listo, le avisamos al organizador», «Te avisamos» y «te avisamos apenas responda» prometían un
aviso que no existe (brief §2). Ahora dicen lo que sí pasa: el pedido queda en «Quién pidió
unirse» y, aceptado, el grupo aparece en «Grupos».

## Pantallas

| Pantalla (harness) | Propósito | Acción principal | Problemas encontrados | Qué cambié | Skill | Qué NO cambié y por qué |
|---|---|---|---|---|---|---|
| **nuevo** (crear grupo) | Organizar un pasanaku | Crear grupo | Abría directo en un campo sin decir qué se arma ni qué pasa después; «Crear grupo» apagado sin motivo; «Nadie elige: el orden sale sorteado y verificable» promete en una pantalla que no verifica | Entrada `IntroDePantalla` «Armá el pasanaku con tu gente» + qué sigue (enlace y vos decidís); `motivoDeBloqueo` con lo que falta; modalidad sorteo: «El orden sale de un sorteo que el grupo puede comprobar» | copywriting, cro, onboarding, ux-writing | Campos, validación (`completo`), defaults (10 cupos, día 5, inicio +7 días) y envío intactos. `_RechazoAlCrear` se movió a `rechazo_al_crear.dart` solo por el tope de 200 líneas |
| nuevo → **grupo creado** | Confirmar y empujar a invitar | Invitar a alguien a mi grupo | La cifra que la persona vino a armar (fondo por período) era una nota al pie en `text3`; «Está congelando el precio» es jerga | «Cada turno junta» + `Monto` en `Tipo.cifra`; «Estamos dejando fija la tarifa del grupo…» (es lo que hace `tarifas` al consumir `grupos.grupo_creado`); línea «Ahora sumá a tu gente…» antes del CTA | copywriting (so what), onboarding (peak-end, próximo paso) | Texto del botón y ruta de invitar, iguales |
| **invitar** (enlace del grupo) | Compartir QR/enlace | Compartir QR por WhatsApp | El trato («quien lo abre pide, vos decidís») solo aparecía cuando no había enlace; el vacío pegaba título y detalle en una sola frase larga | `IntroDePantalla` fija arriba: «Sumá gente a tu pasanaku · Compartí el QR o el enlace. Quien lo abra te pide unirse y vos decidís a quién aceptar»; vacío con `titulo` propio y detalle corto | referrals (mecanismo de compartir, sin incentivos), cro | Sin recompensas ni contadores (no existen en el código). Botones, confirmaciones de anular/regenerar y mensajes compartidos intactos. Marca «Aporteo» en los textos de compartir sin tocar (decisión A5 abierta) |
| **invitar-numero** | Invitación personal a un celular | Crear la invitación | Abría en un campo; botón «Invitar» genérico y apagado sin motivo (en realidad crea la invitación y después la mandás vos) | Entrada «Invitá a una persona · La invitación sirve solo para ese celular. Se la mandás vos…»; CTA «Crear la invitación»; motivo «Escribí los 8 dígitos del celular» | ux-writing (el botón nombra lo que pasa), signup (fricción) | Validación `+591` + 8 dígitos y canal, intactos |
| **unirse** (invitación personal) | Aceptar reglamento y entrar | Unirme al grupo | «Unirme al grupo» apagado sin decir que falta marcar el reglamento | `motivoDeBloqueo`: «Marcá que leíste el reglamento para unirte» | cro (fricción), pasanaku-diseno §4 | Casilla sin premarcar (sin patrones oscuros), idempotencia y hash del reglamento intactos |
| **enlace-de-grupo** (`/g/:codigo`) | Pedir unirse con el enlace | Pedir unirme | «Listo, le avisamos al organizador»: aviso inexistente (ver M2). El aviso de datos no decía que el organizador decide | Éxito: «Listo, tu pedido está en la lista del organizador · Lo ve en «Quién pidió unirse»…»; aviso previo con título «Qué pasa al pedir unirte» | copywriting (claims supportable), marketing-psychology (ansiedad/regret, sin presión) | En el harness la sesión es anónima y el guardián manda al ingreso: la captura muestra el ingreso (no es pantalla mía). Verificado con `pantallas_enlace_del_grupo_test` |
| **pedir-cupo** | Postular a un grupo (CU-68) | Pedir mi cupo | Abría con una alerta azul («Tu cupo queda en espera») antes de decir qué se pide: se leía como problema. Pasos «Te avisamos» y «te avisamos apenas responda»: avisos inexistentes. «Un mensaje para quien decide» era vago | `IntroDePantalla` «Pedí tu lugar en el grupo · El organizador ve tu pedido y decide…»; pasos: «Pedido enviado · Decide el organizador · Lo ves en tus grupos»; «En cuánto: no hay plazo fijo. Si te acepta, el grupo aparece en tu lista»; «Un mensaje para el organizador (opcional)» | onboarding (expectativas), ux-writing | El botón sigue diciendo «Pedir mi cupo» (D-15: nunca «Unirme») |
| **aportar** | Pagar el aporte del período | Aportar Bs X | CTA «Aportar» sin la cifra; apagado sin motivo; «Referencia del pago» sin explicar qué es; éxito «Aporte registrado.» seco | CTA «Aportar Bs 4,44» (`formatearMonto`); motivo «Escribí la referencia del pago para poder aportar»; ayuda «El número de operación de tu comprobante»; éxito «Tu aporte quedó registrado» con el chip de estado real | copywriting (CTA = verbo + qué), cro | No dice «acreditado» ni promete tiempos (regla 91.3): el chip sigue mostrando el estado que devuelve el servidor. Naranja de urgencia, idempotencia y canales intactos |
| **mis grupos** (pestaña, fuera del harness) | Ver mis grupos y entrar a uno nuevo | Unirse / Organizar | Vacío mudo sobre qué hacer; las dos puertas de entrada eran botones `fantasma` al pie, se leían como enlaces | Vacío: «…Unite con el QR o el enlace que te mandó el organizador, u organizá uno con tu gente»; los dos botones pasan a `secundario` | onboarding (empty states), cro (jerarquía) | Orden, claves y topes («al tope» apaga y explica) intactos |
| **solicitudes** | Aceptar o rechazar pedidos | Aceptar | Vacío repetía el título («Nadie pidió unirse todavía» / «Todavía nadie pidió unirse») | Vacío: «Cuando alguien abra el enlace o el QR del grupo y pida unirse, aparece acá para que decidas» | ux-writing | Propuesta (no hecha, agrega navegación): botón «Compartir el enlace» en el vacío |
| **mi-puntaje** | Ver mi reputación | — | «Lo calcula el servicio de transparencia» es lenguaje de sistema | «Lo calculamos a partir de tu historial en AportaYa» | ux-writing (lenguaje del usuario) | El nivel se muestra como «En construccion» (sin tilde, sale del enum): no se tocó la conversión; anotado |
| **mi-estado** | Estado del aporte en un grupo | Invitar a alguien a mi grupo | No hay CTA de aportar aunque haya «Por aportar este período» (la pantalla no tiene `obligacionId`); en el estado vacío del harness el chip dice «Con deuda vigente» con Bs 0,00 | Solo hereda el cambio de `AccionesDeGrupo` (que con grupo pinta el mismo botón primario) | — | Agregar «Aportar» exige un dato que la pantalla no recibe: es cambio de comportamiento. Propuesto en el reporte |
| **retiro** | Pedir el retiro | Solicitar el retiro | Copy ya neutralizado en la pasada anterior (`TextosDeTramitesDelGrupo`) | Nada | — | Trámite delicado con dinero: sin cambios de copy sin necesidad |
| **permuta** | Proponer cambio de turno | Proponer el cambio | Bien resuelto (entrada + motivo del botón) | Nada | — | — |
| **transparencia** | Verificar la cadena del grupo | Verificar la cadena | Correcto y sobrio | Nada | — | No se agregaron promesas de inviolabilidad |
| **sorteo-turno** | Ver el turno y cómo se sorteó | Comprobar el sorteo | Neutralizado a propósito («Comprobá el sorteo vos mismo») | Nada | — | Brief: no reintroducir «nadie puede elegir el orden» |
| **sorteo-verificar** | Reproducir el sorteo | Reproducir el sorteo | `verificacionAyuda` dice «nadie, ni la plataforma, pudo elegir el resultado después de conocerlo»: es la única pantalla que sí reproduce y compara el orden | Nada | — | Se dejó porque la pantalla misma lo verifica (excepción del brief) |
| **reclamo-nuevo** / **escanear** | Reclamo / leer QR | Enviar reclamo / Usar este enlace | Sin problemas de conversión a la vista | Nada | — | — |

## Propuestas para `packages/diseno_flutter` o para otra pasada (no hechas)

1. `mi-estado`: botón «Aportar Bs X» cuando hay monto por aportar (necesita el `obligacionId`
   en `EstadoDelParticipante`).
2. `solicitudes` vacío: acción «Compartir el enlace del grupo» (ruta ya existe).
3. `PantallaEnlaceDelGrupo` dice en su doc que «desde acá se ve quién pidió unirse», pero no
   hay enlace a `solicitudes`; `TextosEnlaceDelGrupo.invitarAUnNumero` existe y nadie lo usa:
   la ruta `/invitar/numero` no tiene entrada en la UI.
4. Nivel de confianza legible («En construcción») con un mapa de los valores del contrato.

## Ambigüedades

- **A5 (abierta, no resuelta acá):** el nombre visible es «Aporteo» bajo el ícono y en los
  mensajes de compartir, y «AportaYa» en textos internos. Este flujo mezcla los dos
  (`escanearRechazo` dice AportaYa; `porQrDetalle` dice Aporteo). Confirmar con Pablo.
- Con `aportaya.grupos.admision.humana-obligatoria` encendido (producción), la decisión del
  organizador la resuelve backoffice. Los textos siguen diciendo «decide el organizador», como
  antes; confirmar con negocio si hay que nombrar la revisión.
