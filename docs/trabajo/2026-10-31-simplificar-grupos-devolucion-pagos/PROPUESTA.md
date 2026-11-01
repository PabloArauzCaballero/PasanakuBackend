# Propuesta — Pasanaku sin administrador: grupos de confianza, devolución automática y pago en un toque

> **En una línea.** Nadie «administra» un grupo. La persona pone tres números y comparte un enlace (o pide
> que la emparejen); desde ahí el sistema admite, inicia, sortea, cobra, entrega y devuelve con reglas
> deterministas. El rol de organizador humano queda solo para quien quiera ser organizador profesional,
> fuera del camino principal de la app.

- Fecha: 2026-10-31 · Autor: sesión de Claude a pedido de Pablo · Evidencia: [`evidencia/hallazgos.md`](./evidencia/hallazgos.md)
- Peldaño: `DISCOVERED`. Esto es una propuesta de producto: nada está implementado ni probado.
- Convención: **Hecho** = lo leí en el repo, con ruta. **Hipótesis** = lo que creo y hay que verificar.
  **DECISION_REQUIRED** = lo decide Pablo (o quien él diga), no esta sesión.

---

## 1 · Diagnóstico: por qué hoy es complejo

### 1.1 Alta de grupo — 12 pasos, 4 personas distintas deciden

Hecho (detalle y rutas en `hallazgos.md` §A). Para que una familia de 10 arranque un pasanaku hoy:

| # | Quién | Qué hace | Quién decide |
|---|---|---|---|
| 1 | cada uno (×10) | registro + foto del carnet + rostro | **una persona de backoffice aprueba** el KYC |
| 2 | cada uno (×10) | acepta contrato de adhesión | — |
| 3 | el que arma | acepta el contrato de **organizador Aprendiz** | — |
| 4 | el que arma | llena **7 campos obligatorios** (nombre, monto, periodicidad, cupos, día de cobro, modalidad de turnos, fecha de inicio) | — |
| 5 | sistema | espera a que `tarifas` congele el precio | — |
| 6 | el que arma | comparte el enlace | — |
| 7 | cada invitado (×9) | abre el enlace y **pide cupo** | — |
| 8 | organizador | **acepta cada pedido** (×9); en producción su aceptación es solo una propuesta y **backoffice** la resuelve (`admision.humana-obligatoria=true`) | organizador + backoffice |
| 9 | cada uno (×10) | **firma el reglamento** | — |
| 10 | organizador | **inicia** el grupo (100 % o ≥ 75 %) | organizador |
| 11 | cada uno (×10) | recarga saldo y paga la **primera cuota** | — |
| 12 | sistema | sorteo | — |

Tres de esos pasos son decisiones humanas que, en un grupo de gente que ya se conoce, no deciden nada:
aceptar al primo (8), apretar «iniciar» (10) y haber firmado un contrato de organizador (3) para un
grupo donde el «organizador» no custodia ni cobra nada (RN-18). Y la causa raíz es una sola decisión:
**ADR-051 D-12** (2026-10-08) prohibió los grupos sin organizador humano, como opción conservadora
«sujeta a ratificación» de Producto. El modelo, el contrato OpenAPI y el trigger de base **todavía
admiten `organizador_id = NULL`** («grupo autogestionado»); solo dos `if` en `MapeoDeAltaDeGrupo` y
`CU20CrearGrupo` lo rechazan.

### 1.2 Devolución — cinco caminos, todos con una persona en el medio

Hecho (`hallazgos.md` §C):

| Quiero… | Hoy exige |
|---|---|
| sacar mi saldo al banco | registrar cuenta, **verificar titularidad** (micro-depósito, proveedor o comprobante que revisa soporte), **esperar el enfriamiento**, MFA, y que **Tesorería** ejecute |
| irme del grupo | pedirlo con motivo; si me deben, **cobro al cierre del ciclo**; si debo, **plan de pago**; el grupo aprueba si el reglamento lo dice |
| que me devuelvan porque el grupo nunca arrancó | **pedir el retiro yo** (CU-65 2a); no pasa solo |
| que se disuelva el grupo | **acuerdo con quórum**, prelación de 5 niveles |
| que me devuelvan un pago duplicado | que soporte lo pida y **un supervisor lo apruebe** |

### 1.3 Pago — dos pasos y ninguna pasarela real

Hecho (`hallazgos.md` §D): el recorrido es **recargar** (QR → webhook → saldo) y después **aportar**
(saldo → saga). El QR directo a la cuenta del grupo existe en el contrato de CU-21 y en la app
(`QR_INTEROPERABLE`), pero la persona sigue viendo «billetera», «saldo», «recargar». Y el bloqueo de
fondo: **no hay proveedor real** — el seed apunta a `example.bo` y TEST corre `ClienteProveedorSimulado`.

---

## 2 · Principio rector

**El sistema opera; las personas solo aportan y cobran.** Es exactamente lo que el modelo original
llamó «organizador digital» en modo `AUTONOMO` (`docs/entidades/07_organizador_automatizacion.md`) y lo
que ADR-051 apagó por prudencia. La propuesta es **reabrirlo como decisión explícita de Producto**, en un
ADR nuevo que supersede D-12 parcialmente, con estas condiciones que lo hacen seguro:

1. El organizador humano **nunca** custodió ni cobró (RN-18, regla 91.5): quitarlo no quita un control
   de dinero, quita un cuello de botella.
2. Todo lo que mueve plata sigue igual: cuenta del grupo titular «grupo», tarifa congelada, aporte
   acreditado solo por webhook firmado, entrega del pozo completo por saga, mayor append-only,
   idempotencia. **Ninguna regla de la 91 cambia.**
3. El KYC de cada participante sigue siendo la barrera de entrada (`R-UIF-09`). Lo que desaparece es
   la **debida diligencia reforzada del administrador**, porque no hay administrador.
4. La «responsabilidad identificable» que ADR-051 pedía la asume la plataforma, que es quien de hecho ya
   ejecuta cada operación.

Riesgo que ADR-051 señaló y sigue vigente: R-ORG-06 («las acciones que mueven plata ajena exigen
confirmación humana») está escrita para las **reglas configurables** de CU-95, no para los casos de uso
centrales (CU-21, CU-22 ya los ejecuta el sistema). **Hipótesis**: el ADR nuevo tiene que decir
explícitamente que el flujo central del grupo de confianza no es una «regla de automatización» en el
sentido de R-ORG-06. Confirmar con Cumplimiento.

---

## 3 · Propuesta 1 — Grupos: dos puertas, ninguna manual

### 3.1 Puerta A · «Armar uno con los míos» (grupo de confianza)

**Lo que hace la persona** (3 pantallas, 1 minuto):

1. Tres números: **cuánto pone cada uno**, **cada cuánto**, **cuántos son**. La app muestra en vivo
   «cada uno recibe Bs X cuando le toque» y «termina en N meses».
2. Toca «Compartir» → WhatsApp / SMS / copiar (ya existe).
3. Listo. La pantalla del grupo muestra «3 de 10 ya están adentro» y se actualiza sola.

**Lo que ya no hace nadie:**

| Hoy | Propuesta | Por qué se puede |
|---|---|---|
| Contrato de organizador Aprendiz | No existe el rol en este modo. El que arma es **anfitrión**: participante común con el único poder de **anular el enlace** | RN-18: nunca custodió nada |
| Nombre, día de cobro, modalidad, fecha de inicio, permutas | Se derivan: nombre «Pasanaku de <anfitrión>» editable después; modalidad **siempre sorteo** (ya es la que hace verificable el orden, CU-60); día de cobro = día del primer cobro; permutas = según plantilla de reglamento | Los 3 números «definen el negocio completo» (`02_grupos_turnos.md:28-33`) |
| Pedir cupo → aprobar → firmar | **El enlace es la admisión**: abrirlo con KYC vigente y firmar el reglamento ocupa el cupo, en un solo toque. Nadie aprueba a nadie | En un grupo de confianza el filtro es social: el anfitrión eligió a quién mandarle el enlace. `CU69EnlaceDeGrupo` hoy crea solicitud PENDIENTE «decisión del dueño»; sería una decisión nueva **por tipo de grupo**, no global |
| «Iniciar» a mano | **Inicio automático**: al llenarse, o en la fecha límite con ≥ `ceil(0,75 × cupos)` achicando el grupo (regla ya mergeada en #364/#365); si no llega al 75 %, **se cancela y devuelve solo** (§4.3) | Reusa `CU69IniciarGrupo` con un disparador cron en vez de un botón |
| Sorteo convocado por el organizador | **Sorteo automático** en cuanto todas las primeras cuotas están acreditadas (R-GRP-20), con fecha de revelación fija (p. ej. 24 h después del compromiso, para que quien quiera aporte entropía) | CU-60 ya dice «Actor principal: el sistema» |
| Admisión humana de backoffice | **No aplica** al grupo de confianza. Sigue aplicando a la puerta B si Cumplimiento lo exige | Interruptor pasa de global a **por tipo de grupo** |

**Lo que no cambia** (y es lo que hace que «use absolutamente todo igual», como pidió Pablo): cuenta de
billetera del grupo, tarifa congelada con hash, reglamento firmado con hash, obligaciones por cupo,
acreditación solo por webhook, sorteo commit-reveal verificable, entrega del pozo completo con cargos
línea por línea, transparencia encadenada, reputación, mora e incumplimiento con debido proceso, UIF.

**Límites del grupo de confianza**: monto por aporte y bolsa por turno acotados por el catálogo
`limite_operativo_billetera` del **nivel de KYC del participante más bajo del grupo** (ya existe, estado
`PROVISIONAL`). No se inventa ningún número nuevo (regla 97.5.4). Si alguien con nivel más bajo quiere
entrar a un grupo que supera su límite, el sistema se lo dice y le ofrece subir de nivel (CU-02), como
hoy hace `AP-CU10-01`.

### 3.2 El algoritmo: las reglas que reemplazan al organizador

Todas deterministas, todas con su CU ya escrito. Lo nuevo es el **disparador** (evento o cron del
organizador digital, CU-96) en vez de un botón humano.

| Evento | Regla | Reusa |
|---|---|---|
| Alguien abre el enlace con KYC vigente y sin restricción | Nace `ACEPTADO_PENDIENTE_FIRMA` con cupo reservado; al firmar queda `ACTIVO` | `CU69EnlaceDeGrupo` + `CU69FirmarReglamento` (saltando `CU68AceptarIngreso`) |
| Último cupo firmado **o** fecha límite con ≥ 75 % | `CONFORMADO` achicado; nace la obligación del período 1 por cupo | `CU69IniciarGrupo` (origin/test) |
| Fecha límite con < 75 % | `CANCELADO`; devolución íntegra automática de lo acreditado (§4.3) | estado `CANCELADO` ya existe en `EstadoGrupo` (`02_grupos_turnos.puml:55`); falta la transición |
| Todas las primeras cuotas `ACREDITADO` | Compromiso del sorteo; revelación a las 24 h | `CU60Sortear` |
| Apertura de período | Obligaciones, recordatorios, enlace de pago | CU-21 flujo 1, CU-81, CU-99 flujo 5 |
| Vencimiento + gracia | Recargo como obligación nueva; umbral → incumplimiento con descargo | CU-21 flujo 6, CU-25 |
| Cierre de período con pozo completo (o cubierto por la reserva) | Entrega al turno, cargos línea por línea | `CU22EntregarPozoCompleto` + `CU23CubrirConRespaldo` |
| Último período entregado | `FINALIZADO`; reputación; reseñas habilitadas | CU-70/71/76 |
| Mora generalizada según umbral del reglamento | Disolución automática con la prelación simplificada (§4.4) | CU-67 con causal automática (ya previsto: «causal automática prevista en el reglamento») |

Lo único que **sigue siendo humano** es lo que la regla 91.4 exige que lo sea: sancionar, ejecutar un
aval, castigar una deuda. Eso no lo hacía el organizador tampoco.

### 3.3 Puerta B · «Que me armen uno» (desconocidos, por algoritmo)

Esto ya está diseñado en **CU-68 flujo 5-7** y no tiene código. La persona dice qué busca (monto,
periodicidad, cuándo preferiría cobrar); el motor arma una `propuesta_grupo` con `puntaje_cohesion` y
`riesgo_estimado`, cada postulante acepta o no, y al llegar a las aceptaciones necesarias **se
materializa como grupo sin organizador** (hoy CU-68 §6 dice que materializa «por CU-20», que exige
organizador: con el ADR nuevo deja de hacerlo). Reusa los pesos versionados de `criterio_emparejamiento`,
`score_riesgo_incumplimiento` y `max_morosos_por_grupo`.

Aquí sí conviene **mantener la admisión humana de backoffice como interruptor** (ADR-052 DD-2) hasta que
Cumplimiento diga lo contrario: desconocidos + plata es la zona de riesgo. Es un interruptor por tipo
de grupo, no una decisión de esta propuesta.

### 3.4 Qué pasa con el organizador habilitado (CU-90/91/92)

No se borra: se **saca del camino principal de la app**. Queda como «organizador profesional» habilitado
desde backoffice para quien arme muchos grupos (el segmento que ADR-051 tenía en mente). En la app,
«Organizar un grupo» deja de ser la puerta: las puertas son «Armar uno con los míos» y «Que me armen uno».

### 3.5 Inventario de lo que habría que tocar (para dimensionar, no para hacerlo acá)

| Pieza | Cambio | Dónde |
|---|---|---|
| ADR nuevo | Supersede D-12 parcialmente: grupo de confianza sin organizador; R-ORG-06 no aplica al flujo central; límites por catálogo | `docs/Arquitectura/` |
| Tipo de grupo | Decidir: reusar `es_autogestionado` (ya en el puml y en la tabla) o un `tipo` (`CONFIANZA` / `ABIERTO` / `ORGANIZADO`). **Recomendación: `tipo`**, porque la puerta B también es sin organizador y hay que distinguirla por la admisión | `docs/entidades/02_grupos_turnos.puml` → DDL → jOOQ (dirección única, regla 97.1) |
| Alta | Aceptar `organizadorId = null` cuando `tipo = CONFIANZA`; derivar los campos; el anfitrión ocupa el cupo 1 como hoy el organizador | `MapeoDeAltaDeGrupo.java:38-44`, `CU20CrearGrupo.java:88-90`, `grupos.yaml` `EntradaGrupo` (hacer opcionales nombre, diaCobro, modalidad, fechaDeInicio **solo** para este tipo; contrato versionado, regla 98.1.3) |
| Enlace = admisión | Si el grupo es `CONFIANZA`, canjear el enlace crea `ACEPTADO_PENDIENTE_FIRMA` directo | `CU69EnlaceDeGrupo.java` |
| Inicio automático | Tarea cron del organizador digital que invoca `CU69IniciarGrupo` en la fecha límite, y cancelación con devolución si < 75 % | `servicios/organizador` (CU-96) → `servicios/grupos`; evento nuevo `grupo.cancelado_sin_inicio` |
| Sorteo automático | Consumidor del evento «todas las primeras cuotas acreditadas» que llama a `comprometer`; revelación programada | `servicios/grupos` + `aportes` (evento) |
| Reglamento plantilla | Un `reglamento_grupo` generado de los 3 números con la plantilla «confianza» (gracia, recargo, permutas, quórum, causal de disolución automática) | `servicios/grupos`; texto legal → DECISION_REQUIRED |
| App | Flujo de 3 campos; «Compartir» como paso 2; pantalla del grupo con «N de M adentro» y sin botones de administrar; quitar «Pedir cupo» y «Solicitudes» para este tipo | `apps/movil/lib/pantallas/pasanaku/` (leer antes las skills D1/D2/D4 que exige `AGENTS.md`) |
| Backoffice | Lista de grupos de confianza con sus eventos automáticos (auditoría), sin acciones de admisión | `apps/backoffice` |

---

## 4 · Propuesta 2 — Devolución: automática, por donde vino, sin pedirla

### 4.1 Caso A · Sacar mi saldo al banco (CU-11 + CU-18)

- **Hoy**: cuenta registrada a mano + verificación de titularidad + enfriamiento + MFA + Tesorería.
- **Propuesta**: **«Devolver por donde vino»**. La cuenta de origen del último pago QR acreditado del
  titular queda registrada como instrumento **verificado por consulta al proveedor** (método que CU-18
  ya lista) si el proveedor devuelve nombre/documento del pagador y coinciden con el titular. Sin
  micro-depósito ni comprobante. El enfriamiento se conserva **solo** para cuentas agregadas a mano, que
  es donde vive el fraude de toma de cuenta. Ejecución automática por el proveedor (ACH) bajo el tope de
  segundo aprobador del catálogo; Tesorería solo mira lo que supera el tope.
- **Hipótesis a verificar**: que el proveedor de QR interoperable devuelva en el webhook la cuenta y el
  titular del pagador. Si no lo devuelve, este caso no se simplifica y se dice.
- **Qué toca**: `CU100RecibirWebhookPasarela` (guardar instrumento de origen), `CU18` (método
  `CONSULTA_AL_PROVEEDOR` automático), política de enfriamiento por método, `CU28` automático bajo tope.
- **Decide**: Cumplimiento (titularidad por proveedor) + Tesorería (tope de ejecución automática).

### 4.2 Caso B · Irme de un grupo en curso (CU-65)

- **Hoy**: motivo, posición, plan de pago negociado, aprobación del grupo según reglamento.
- **Propuesta**: la plantilla de reglamento del grupo de confianza fija la regla y la app solo la
  muestra: **si no cobré**, mi parte vuelve **al cierre del ciclo** a mi billetera y de ahí por 4.1 (esto
  ya es así; lo que cambia es que no hay nadie que apruebe); **si ya cobré**, el plan de pago es
  **automático e igual al calendario original** (sigo debiendo las mismas cuotas en las mismas fechas),
  sin negociación. Retirarse no exige acuerdo del grupo en este tipo.
- **Qué toca**: `CU65Retirarse` (sin paso de aprobación cuando `tipo = CONFIANZA`; plan = calendario).
- **Decide**: Pablo (regla del reglamento plantilla).

### 4.3 Caso C · El grupo nunca arrancó

- **Hoy**: cada uno tiene que pedir el retiro (CU-65 2a).
- **Propuesta**: en la fecha límite con < 75 %, el grupo pasa a `CANCELADO` y **cada aporte acreditado
  vuelve íntegro a la billetera del que lo puso, automáticamente, en la misma transacción**, con asiento
  y evento `grupo.cancelado_sin_inicio`. Sin comisión (no hubo servicio). Después, 4.1.
- **Qué toca**: transición nueva `ABIERTO_A_INSCRIPCION → CANCELADO` (el estado ya existe), devolución
  en `aportes`/`nucleo-financiero` por evento (saga de compensación S1 ya existe para el reverso).
- **Decide**: Pablo (fecha límite por defecto: p. ej. la fecha del primer cobro elegida; sin número
  inventado, es un parámetro del reglamento plantilla).

### 4.4 Caso D · Disolver el grupo

- **Hoy**: acuerdo con quórum, prelación de 5 niveles que incluye reponer el fondo mutual.
- **Propuesta**: con la reserva empresarial como único respaldo (ADR-051 D-15) y sin fondo mutual en el
  grupo de confianza, la prelación queda en **dos** líneas: deudas exigibles de cada uno contra su propio
  saldo → devolución a quienes no cobraron, a prorrata. Disparador **automático por causal del
  reglamento** (mora generalizada: umbral de la plantilla) además del acuerdo.
- **Qué toca**: `CU67DisolverGrupo` (prelación por tipo), regla cron (CU-96).
- **Decide**: Pablo (umbral de mora generalizada) + Finanzas (la reserva absorbe o no lo cubierto).

### 4.5 Caso E · Pago duplicado o de más

- **Hoy**: soporte + supervisor + proveedor.
- **Propuesta**: lo que la conciliación detecta como **duplicado inequívoco** (misma `orden_cobro`, dos
  `pago` acreditados) se reembolsa **solo**, por donde vino, bajo el tope del catálogo; el «pago de más»
  sigue como saldo a favor (ya es así). Lo ambiguo sigue siendo de soporte.
- **Qué toca**: `CU19ReembolsarPago` con motivo `DUPLICADO` auto-aprobado bajo tope.
- **Decide**: Cumplimiento (`R-SEG-04` exige aprobador ≠ solicitante; habría que declarar que el
  sistema es el solicitante y la regla el aprobador, o mantenerlo manual).

---

## 5 · Propuesta 3 — Pago en un toque

1. **La billetera desaparece del camino del aporte.** «Pagar mi cuota» genera un QR por el **importe
   exacto de la obligación, directo a la cuenta del grupo** (CU-21 medio `QR`, ya en contrato y app). Si
   hay saldo, se usa primero y el QR sale por la diferencia. Recargar queda como acción secundaria.
2. **Débito programado opt-in**: «cobrame solo cada vez» = retención el día de la autorización y
   ejecución en la fecha (CU-21 flujo 4 → CU-13). Ya diseñado; sin código verificado.
3. **El recordatorio trae el pago**: enlace de pago rápido (CU-99 flujo 5) en el mensaje de CU-81, que
   abre la app en el QR de esa cuota.
4. **La acreditación sigue siendo solo por webhook firmado** (regla 91.3.1). Es lo que hace que el pago
   sea automático **y** seguro: nadie «aprueba» un comprobante.
5. **Una sola cosa en la pantalla de recarga**: QR. `pantalla_recargar.dart` hoy ofrece también
   `TARJETA` y `TRANSFERENCIA`, en contra de `Flujo de pantallas` §4.2. Anotado, no se arregla acá.
6. **El bloqueo real es la pasarela.** Nada de lo anterior cobra un boliviano hasta que haya un
   proveedor de QR interoperable real contratado y pasado por CU-99 (evaluación de tercero, sandbox con
   webhook duplicado y timeout, credenciales en el gestor de secretos). Eso es una **decisión de
   negocio y un contrato**, no código. No nombro proveedores: sería inventar.
7. **Comisión**: la recomendación del 2 % por turno (piso Bs 10, techo Bs 300) sigue pendiente de
   decisión (`ANALISIS-COMISION.md`). En el grupo de confianza se cobra igual: es el servicio de la
   plataforma, no del organizador.

---

## 6 · Lo que esta propuesta no toca

RN-18 (nadie cobra por administrar, nadie custodia salvo la plataforma) · regla 91 completa · KYC básico
como barrera de entrada y su aprobación (hoy manual: es la fricción más grande que queda y **no es de
producto, es de Cumplimiento**; se anota) · mayor append-only · idempotencia · debido proceso de
sanciones · UIF.

---

## 7 · Decisiones de Pablo (DECISION_REQUIRED)

| ID | Decisión | Recomendación de esta sesión |
|---|---|---|
| D1 | Reabrir el grupo sin organizador humano (supersede ADR-051 D-12) para el tipo «confianza» | **Sí**, con ADR nuevo y las cuatro condiciones de §2 |
| D2 | El enlace es la admisión en el grupo de confianza (nadie aprueba) | **Sí**; el anfitrión puede anular el enlace |
| D3 | Inicio y sorteo automáticos (fecha límite + 75 %, R-GRP-20) | **Sí**; la fecha límite es el primer día de cobro elegido |
| D4 | Si no llega al 75 %, cancelar y devolver solo | **Sí**, íntegro, sin comisión |
| D5 | Puerta B (emparejamiento por algoritmo) también sin organizador, con admisión humana por interruptor | **Sí**, interruptor encendido hasta que Cumplimiento lo apague |
| D6 | Organizador habilitado queda solo para «profesionales», fuera del camino principal | **Sí** |
| D7 | Reglamento plantilla del grupo de confianza: gracia, recargo, permutas, retiro sin aprobación, disolución automática por mora | Parámetros **por catálogo**, texto legal lo entrega Legal |
| D8 | Devolver «por donde vino» con titularidad verificada por el proveedor | **Sí si** el proveedor devuelve titular; si no, no se promete |
| D9 | Modelado: `tipo` de grupo nuevo vs reusar `es_autogestionado` | **`tipo`** (`CONFIANZA` / `ABIERTO` / `ORGANIZADO`) |
| D10 | Orden de los carriles de implementación | §8 |

---

## 8 · Orden sugerido de implementación

Sin estimaciones de tiempo (no las puedo justificar). Cada paso es un carril con su `PLAN.md`.

1. **ADR** que supersede D-12 y fija el tipo de grupo, los límites por catálogo y la lectura de R-ORG-06.
2. **Modelo → DDL → jOOQ**: `grupo.tipo`; transición `CANCELADO`; `reglamento` plantilla. Verificación de
   deriva (regla 97.2).
3. **`grupos`**: alta `CONFIANZA` sin organizador y con derivados; enlace = admisión; inicio automático y
   cancelación con devolución; sorteo automático. Contrato versionado.
4. **App**: las tres pantallas de la puerta A; pantalla del grupo sin administración. Capturas en tres
   viewports y dos temas (regla 95.7).
5. **Devolución** 4.3 (cancelación) y 4.2 (retiro sin aprobación): son los que no dependen del proveedor.
6. **Pagos**: «Pagar mi cuota» con QR exacto; recordatorio con enlace. En paralelo, la **decisión
   comercial** de la pasarela, que es el camino crítico de todo lo que cobra de verdad.
7. **Puerta B** (motor de propuestas, CU-68 §5-7): es lo más nuevo en código y lo menos urgente para
   «jugar con los míos».
8. Devolución 4.1, 4.4, 4.5: dependen del proveedor y de Cumplimiento.

---

## 9 · Ambigüedades e hipótesis declaradas

1. Interpretación de «no manual, automático, en base a un algoritmo» (PLAN §Ambigüedades 1).
2. Qué «devolución» duele más (PLAN §Ambigüedades 2): se trataron las cinco.
3. R-ORG-06 y el flujo central del grupo de confianza (§2): hipótesis, confirmar con Cumplimiento.
4. El proveedor de QR devuelve titular del pagador (§4.1): hipótesis, confirmar con el proveedor cuando
   exista.
5. Débito programado (CU-13) y enlace de pago (CU-99 §5): leídos en los CU, **no verificados en código**.
6. Los límites del catálogo son `PROVISIONAL`: cualquier tope que esta propuesta use hereda esa marca.
