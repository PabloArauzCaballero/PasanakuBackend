---
tags:
  - arquitectura
  - adr
  - dinero
  - seguridad
  - registro-de-decisiones
titulo: "ADR-052 — Decisiones adoptadas por delegación del dueño"
estado: aceptada
fecha: 2026-10-11
---

# ADR-052 — Decisiones adoptadas por delegación del dueño

> Registro de ocho decisiones que el endurecimiento integral dejó abiertas y que el dueño del proyecto
> delegó expresamente: «lo más recomendado, pero cerralo». Cada una lleva contexto, decisión,
> alternativas descartadas, consecuencias y **quién la ratifica**.

> **Constancia de aceptación (veraz).** Aceptado por delegación expresa del dueño del proyecto (Pablo),
> 2026-10-11; **pendiente de ratificación formal por Producto/Finanzas/Legal** (y Seguridad,
> Cumplimiento y Tesorería donde se indica). No contiene firmas ni nombres de otras personas, no aprueba
> ninguna tarifa ni parámetro regulatorio y **no habilita dinero real**. Las áreas citadas son las que el
> PLAN §12 propone como dueñas; ninguna fue designada ni consultada. Hermano de
> [[ADR-051 Garantía del pozo, comisión y autogestión|ADR-051]], que cierra D-02, D-03, D-04, D-12, D-15 y D-16.

## Contexto

Entre la primera y la quinta vuelta del endurecimiento quedaron decisiones que el código **ya expone con
un interruptor, un doble o una advertencia**, pero que ningún documento fijaba. Las fuentes:
`evidencia/carril-B3/DECISIONES-Y-ABIERTOS.md` (§5 y §6), `evidencia/carril-G/` (el GAP del precio de la
oferta), `evidencia/carril-I/INFORME.md` (CU-18 y billeteras), el apartado «Carril K» de `REPORTE.md` (erp),
`evidencia/carril-L2/INFORME.md` (billetera inexistente de las cuentas de TEST) y `CU-20` (supuesto A1).
Cuando el repositorio no decide nada, la regla 00 §1.7 manda registrar la ambigüedad y no resolverla por
conveniencia; el dueño pidió que no quedaran abiertas, así que se adopta **la opción más conservadora
compatible con lo que el repositorio ya decide** y se deja a la vista quién debe ratificarla.

Resumen:

| # | Decisión | Valor adoptado | Estado del código al 2026-10-11 |
|---|---|---|---|
| DD-1 | Precio de venta de un turno | Vendedor cobra `precio − cargos`; la plataforma ingresa `cargos`; cada deducción con su línea | `entregas` ya lo calcula; `nucleo-financiero` pagaba el precio completo: la alineación está **en curso en otro carril, sin versionar** |
| DD-2 | Admisión humana obligatoria | **Producción `true`; TEST `false`** | Interruptor existe; el compose de entorno trae `false` por omisión |
| DD-3 | Sustitución automática de administrador | **`false`**: manual por backoffice | Interruptor existe, apagado |
| DD-4 | CU-18, verificación de titularidad | `SERVICIO_ENTREGAS` recibe **solo sí/no**, con **límite de consultas** | Sí/no implementado; **el límite no** |
| DD-5 | Apertura del grupo a inscripciones | Automática al confirmarse el precio congelado | Implementada |
| DD-6 | Forma de pago QR | **Fuera** del contrato de pago a proveedor | Contrato y CHECK alineados (ver DD-6) |
| DD-7 | erp: autorizante y aprobador | Salen **del token**, nunca del cuerpo | Implementado en el árbol de trabajo |
| DD-8 | Apertura de billeteras | Las abre `nucleo-financiero` **al consumir el evento** correspondiente | **En curso en otro carril, sin versionar** (Carril Q); sin ese código no hay billeteras |

## Decisión

### DD-1 · Precio de venta de un turno

**Contexto.** Una oferta lleva `precio` y `cargos` (`ck_oferta_turno_cargos`: cargos ≤ precio). `entregas`
declara que el vendedor cobra `precio − cargos` (`ReglasDeOferta.paraElVendedor`) y que los cargos son de la
plataforma; la ficha contractual lo llama «ambigüedad A9» (`evidencia/carril-O2/H9-S1-M1-ficha-contractual-cesion.md` §2).
La corrida real de la cuarta vuelta lo dejó como GAP (`evidencia/carril-G/integracion-dinero.log`):
`nucleo-financiero` le pagó al vendedor Bs 5.500 y a la plataforma Bs 0, mientras `entregas` declaraba
Bs 5.450 y Bs 50. `OfertaParaPago` es mínima a propósito (id, vendedor, precio, estado) y una prueba lo exige.

**Decisión.**
1. El comprador paga **el precio publicado**, sin recargo adicional.
2. El vendedor cobra **`precio − cargos`**; la plataforma ingresa **`cargos`** (`PLATAFORMA_INGRESOS`). El
   vendedor absorbe los cargos (A9).
3. **Cada deducción tiene su línea**: el pago al vendedor, el ingreso de la plataforma y, si aplica, el
   impuesto, son asientos distintos con tipo, importe y referencia a la oferta (regla 91.3.4). No existe un
   «neto» sin sus líneas.
4. Los cargos los fija `tarifas` (H11); el servicio de ofertas **no calcula tarifas**. Su valor sigue siendo
   **sintético** (`apto_produccion = false`) hasta que Dirección comercial/tarifas lo fije.
5. La plataforma no es parte ni garante del cumplimiento del turno; solo registra la cesión y mueve fondos.
   El organizador **no** cobra ni recibe nada de esta venta (RN-18).

**Alternativas descartadas.**
- *El vendedor cobra el precio completo y los cargos se le cobran aparte al comprador:* el comprador pagaría
  más que el precio anunciado; contradice «precio publicado».
- *Cargos en cero hasta que Finanzas decida:* oculta la contradicción en vez de resolverla y deja a
  `entregas` y `nucleo-financiero` con dos lecturas.
- *Un «neto» único sin líneas:* prohibido por la regla 91.3.4.

**Consecuencias.**
- `nucleo-financiero` debe pagar `precio − cargos` al vendedor y acreditar `cargos` a la plataforma **en la
  misma operación idempotente** (una clave, dos líneas, cuadre al centavo).
- `OfertaParaPago` debe llevar `cargos`: es un **campo aditivo versionado** (regla 98.1.3). La prueba que hoy
  exige el contrato mínimo se cambia **por esta decisión, de forma explícita**; no se debilita en silencio.
- Los GAP `H9.S1.M5-cargos` del script de integración (`despliegue/ensayos/integracion/`) se cierran cuando
  ambos servicios coincidan.
- Tratamiento tributario del precio y de los cargos (L5 de la ficha) **sigue abierto**: Fiscal.
- Riesgo residual: sin tope de precio (L8), un precio fuera de mercado entre miembros no se limita.

**Ratifica.** Dirección comercial/tarifas, con Finanzas y el área tributaria.

### DD-2 · Admisión humana obligatoria

**Contexto.** Dos puertas heredadas dejaban entrar sin backoffice: aceptar una invitación por enlace creaba
al participante `ACTIVO`, y `decidirSolicitudDeIngreso` dejaba decidir al organizador. La regla confirmada 4
del dueño dice que el organizador **propone** y backoffice **conserva la autoridad final**. El interruptor es
`aportaya.grupos.admision.humana-obligatoria` (`ADMISION_HUMANA_OBLIGATORIA`), con valor `true` por omisión
en `servicios/grupos/src/main/resources/application.yml`.

**Decisión.** **Producción: `true`. TEST: `false`** (el ambiente de TEST conserva su flujo para los
recorridos A/B). Con `true`, aceptar por enlace responde 422 `AP-CU69-10` (el camino es el canje, que deja la
solicitud pendiente de backoffice) y la decisión directa del organizador responde 422 `AP-CU68-15`.

**Alternativas descartadas.**
- *`false` en producción:* anula la regla 4; el organizador admitiría gente sin control de backoffice.
- *`true` también en TEST:* rompe los recorridos A/B de TEST mientras el flujo de aceptación por backoffice
  no esté disponible de punta a punta (`evidencia/carril-L3`).

**Consecuencias.**
- Hay que **fijar `ADMISION_HUMANA_OBLIGATORIA=true` explícitamente en Coolify de producción**:
  `scripts/generar_compose.py` genera el compose desplegado con `${ADMISION_HUMANA_OBLIGATORIA:-false}`
  (para conservar TEST), de modo que un despliegue que olvide la variable **admite sin humano**. Es un
  punto obligatorio de la lista de salida a producción (H15). Recomendado (cambio de script, fuera de este
  ADR): invertir el valor por omisión del entorno desplegado a `true` y que TEST fije `false`.
- El rechazo por la puerta 2 es un cierre con instrucción, no una redirección silenciosa; una redirección
  real exigiría una versión nueva del contrato (`SalidaAceptacionInvitacion.participanteId` obligatorio).
- Riesgo residual: la regla es una configuración, no un invariante de base.

**Ratifica.** Negocio y Cumplimiento.

### DD-3 · Sustitución automática de administrador

**Contexto.** Al revocarse la habilitación de un organizador (`organizador.habilitacion_revocada`), `grupos`
emite `grupos.sustitucion_requerida` por grupo vivo. El modo automático elige al participante `ACTIVO` más
antiguo que `organizador` confirma habilitado; esa regla de elección es **sintética**, no una decisión de
negocio. El consumidor Kafka (`ConsumidorDeRevocacionesKafka`) está apagado por omisión y no hay prueba
contra un broker real (`carril-B3/DECISIONES-Y-ABIERTOS.md` §6).

**Decisión.** **`SUSTITUCION_AUTOMATICA_ADMIN=false`**: la sustitución es **siempre un acto humano** de
backoffice (`POST /grupos/{id}/administrador/sustitucion`). Retirar al saliente del grupo es otro acto (CU-65).

**Alternativas descartadas.**
- *Automática:* confía la administración de dinero ajeno a una regla no aprobada («el más antiguo»).
- *Híbrida (propone el sistema, confirma una persona):* es válida a futuro, pero requiere decidir la regla
  de elección, que hoy no existe.

**Consecuencias.**
- Mientras `SUSTITUCION_CONSUMO_HABILITADO` siga en `false`, **una revocación no genera automáticamente la
  tarea**: backoffice debe detectar los grupos del organizador revocado por su propio listado. Hay que
  encender el consumo cuando operaciones confirme la configuración del consumidor, sin encender el modo
  automático.
- Un grupo puede quedar sin administrador activo entre la revocación y la sustitución. No se fija un SLA
  contractual (regla 00 §2): lo define Operaciones.

**Ratifica.** Negocio y Cumplimiento.

### DD-4 · CU-18: `SERVICIO_ENTREGAS` verifica titularidad solo con sí/no

**Contexto.** `entregas` necesita saber si la cuenta de destino pertenece al titular y **no puede leer la base
de identidad** (ADR-017). `identidad` admite además del permiso de datos sensibles la identidad de servicio
`SERVICIO_ENTREGAS` (`UsuariosController.verificarTitularidad`, `@PermisoAlternativo("SERVICIO_ENTREGAS")`);
el hallazgo del carril I fue que una credencial de servicio comprometida sería un **oráculo sí/no de
identidad** sin límite de consultas.

**Decisión.**
1. `SERVICIO_ENTREGAS` recibe **únicamente un sí/no**: ningún nombre, documento ni dato del titular sale de
   `identidad`.
2. Hay un **límite de consultas** por servicio llamante y por usuario consultado en una ventana de tiempo;
   superado el límite, la consulta se rechaza y se registra.
3. **Toda consulta deja rastro auditable** (quién la hizo, sobre qué usuario, resultado), sin el número de
   cuenta en claro (regla 90.2.1 y 90.2.7).
4. Sin credencial de servicio o ante cualquier fallo (403, 500, cuerpo ilegible, caída) la respuesta es **«no»**
   y no se registra la cuenta (falla cerrado), como ya hace `TitularPorHttp`.

Las cifras del límite **no se inventan**: las fija Seguridad y viven en configuración.

**Alternativas descartadas.**
- *Devolver nombre y documento al servicio:* viola respuesta mínima (regla 90.1.8).
- *Que `entregas` compare contra una copia de los datos:* duplica PII en otro servicio (ADR-017).
- *Sin límite de consultas:* deja el oráculo abierto.
- *Reusar el límite del gateway (ADR-050):* ese control opera en el borde; **no se asume** que cubra el
  tráfico servicio a servicio.

**Consecuencias.**
- **El límite de consultas todavía no está implementado** (`carril-I/INFORME.md`, riesgo residual): la
  decisión queda adoptada y la implementación es trabajo de `identidad`.
- La verificación sigue sujeta a que `identidad` entregue credenciales de servicio fuera del perfil `local`
  (`evidencia/carril-C3/credenciales-de-servicio.md`).

**Ratifica.** Seguridad, con los dueños de `identidad` y `entregas`.

### DD-5 · Apertura automática del grupo a inscripciones

**Contexto.** `CU-20` paso 7: `tarifas` consume `grupos.grupo_creado`, congela el precio y responde
`tarifas.tarifa_congelada`; `grupos` pasa el grupo de `BORRADOR` a `ABIERTO_A_INSCRIPCION`. El **supuesto A1**
dejaba abierto si la apertura también debía esperar la cuenta del grupo en `nucleo-financiero`, que todavía
no la abre ningún consumidor.

**Decisión.** El grupo **se abre solo, al confirmarse el precio congelado**. No hay paso humano
(regla confirmada 3: sin segunda aprobación del grupo). **La cuenta del grupo no es condición de apertura;
sí lo es de cobro**: cobrar a un participante exige que la cuenta exista.

**Alternativas descartadas.**
- *Apertura manual por backoffice:* es la segunda aprobación humana que la regla 3 prohíbe.
- *Esperar también la cuenta del grupo:* con el consumidor de `nucleo-financiero` ausente (DD-8), ningún
  grupo abriría nunca.

**Consecuencias.**
- Un grupo puede recibir invitados antes de tener cuenta; el cobro sigue bloqueado hasta que exista.
- Sin tarifario vigente el evento se rechaza sin marcarse consumido y se reintenta: el grupo queda en
  `BORRADOR`, nunca con un precio inventado. Una respuesta repetida no cambia el estado ni escribe otro
  historial.
- `APERTURA_CONSUMO_HABILITADO` debe estar en `true` (es el valor por omisión); apagado, ningún grupo nuevo
  recibe invitados.

**Ratifica.** Producto, con el dueño de `nucleo-financiero` (por la segunda condición futura).

### DD-6 · La forma de pago QR queda fuera del contrato de pago a proveedor

**Contexto.** `pago_a_proveedor` admite `TRANSFERENCIA`, `CHEQUE` y `EFECTIVO`
(`ck_pago_a_proveedor_forma_pago`, `sql/10_tablas/13_contabilidad_erp/pago_a_proveedor.sql`). El contrato
admitía además `QR`, que la restricción rechaza: un cliente que enviara `QR` recibía un fallo de base en vez
de un 400 del contrato (`REPORTE.md`, Carril K).

**Decisión.** **`QR` no es una forma de pago a proveedor.** El contrato de `EntradaPago` lista solo las
tres formas que admite la restricción. El QR pertenece al **cobro** (`EntradaCobro`, `cuenta_por_cobrar`),
no al pago.

**Alternativas descartadas.**
- *Agregar `QR` al CHECK:* nadie decidió pagar proveedores por QR; sería ampliar la superficie de pago con
  dinero sin decisión.
- *Dejar el contrato más ancho que la restricción:* convierte un error del cliente en un fallo del servidor.

**Consecuencias.**
- Un `QR` en un pago a proveedor se rechaza **en el contrato** (400), no en la base.
- Si Tesorería quiere pagar por QR, es una decisión nueva que cambia el CHECK, el contrato y `CU-103` juntos.
- Estado: `CU-103` (flujo y contrato) y `erp.yaml` ya lo reflejan en el árbol de trabajo; la verificación de
  ejecución es la de `PagoDeFacturaWebTest` y `CU103RechazosTest`.

**Ratifica.** Tesorería y Producto.

### DD-7 · erp: el autorizante y el aprobador salen del token

**Contexto.** `pagarFacturaDeProveedor` exigía `autorizadoPor` en el cuerpo y la aprobación de la factura
aceptaba `aprobadaPor` dictado por el cliente: cualquiera podía declararse otra persona y, con ello, esquivar
la segregación de funciones `R-SEG-04` (quien aprueba la factura no puede ser quien la paga).

**Decisión.**
1. `autorizado_por` del pago es **el sujeto autenticado** (`ctx.usuarioId`); el contrato no declara
   `autorizadoPor`.
2. `aprobada_por` de la factura es **el sujeto autenticado** al aprobar (`aprobar: true`); el contrato no
   declara `aprobadaPor`.
3. Si cualquiera de los dos llega en el cuerpo, se rechaza con **400** (`additionalProperties: false`).
4. `R-SEG-04` no cambia: aprobador ≠ pagador.

**Alternativas descartadas.**
- *El cliente dicta quién autoriza:* permite suplantar y vacía la segregación de funciones.
- *Aceptar el campo pero ignorarlo:* oculta el error en lugar de rechazarlo (regla 96.2.2).

**Consecuencias.**
- La segregación se vuelve verificable: el rastro de auditoría nombra a quien realmente actuó.
- Los clientes (backoffice) dejan de enviar esos campos; el frontend ya se adaptó (`REPORTE.md`, Carril K).
- La idempotencia del pago (`R-CTB-11`, cabecera `Idempotency-Key` obligatoria) acompaña esta decisión en
  `CU-103`; su estado de implementación se verifica en `servicios/erp/src/test/…/CU103IdempotenciaTest`.

**Ratifica.** Contabilidad y Seguridad.

### DD-8 · Las billeteras se abren al evento correspondiente

**Contexto.** `CU-01` describe la apertura de la billetera como un paso del mismo alta, pero el código de
`identidad` es explícito: crea al usuario en una transacción local (`PENDIENTE_VERIFICACION`, no opera) y
emite `identidad.usuario_registrado`; la billetera y la cuenta contable son de `nucleo-financiero`, y
`svc_identidad` no puede escribir en ese esquema (ADR-017, invariante 11). `Flujo funcional · recorrido del
usuario` nombraba un evento distinto (`identidad.usuario_creado`). Para el grupo, `CU-20` crea la cuenta en el alta (paso 3)
y emite `GRUPO_CREADO` (paso 6), mientras el flujo funcional hablaba de un `grupos.grupo_activado` que **nunca se
emite**. **En la última versión versionada ningún consumidor abre ninguna de las dos** (`carril-I/INFORME.md`; la cuenta A de TEST responde `AP-CU13-01 «No tenés una billetera
activa en BOB»` en `carril-L2/INFORME.md`).

**Decisión.**
1. **La billetera de un usuario** (`tipo = USUARIO`) la abre `nucleo-financiero` **al consumir
   `identidad.usuario_registrado`**, en su propio esquema y transacción, con saldos en cero. No puede quedar
   operativa para un usuario `PENDIENTE_VERIFICACION`; desde qué evento pasa a operar lo fija el contrato del
   productor al implementarlo.
2. **La cuenta del grupo** (`tipo = GRUPO`, titular el grupo y **nunca el organizador**, `R-GRP-04`) la abre
   `nucleo-financiero` **al consumir `grupos.grupo_creado`**, el evento que ya se emite en el alta (`CU-20`
   paso 6) y que `tarifas` ya consume. Se descarta `grupos.grupo_activado`: nadie lo emite.
3. Ningún servicio abre billeteras llamando a otro de forma síncrona, ni las crea «al primer uso».
4. Consumidor idempotente (la entrega es at-least-once): el segundo evento no crea otra billetera
   (`R-BIL-04`: una cuenta activa por usuario, moneda y tipo).

**Alternativas descartadas.**
- *`identidad` abre la billetera:* viola ADR-017 y la propiedad de datos.
- *Llamada HTTP síncrona en el alta:* cadena síncrona en el camino del usuario y el alta quedaría atada a la
  disponibilidad de `nucleo-financiero` (regla 98.2.5).
- *Abrir al primer uso:* carrera entre dos operaciones, estado de nivel de debida diligencia ambiguo.
- *Sembrarlas a mano:* no escala y deja datos sin trazabilidad.

**Consecuencias.**
- **Implementación en curso en otro carril (Carril Q, archivos sin versionar al 2026-10-11) y sin verificar
  contra Kafka real.** Hasta que `nucleo-financiero` consuma esos eventos no hay billeteras reales: los
  consumidores de dinero (`aporte_cobrado`, recargas) rechazan por billetera inexistente y los recorridos
  reales con dinero no pueden cerrarse. Es dependencia dura del trabajo con dinero real.
- Se corrige `CU-01` (el paso 9-10 deja de decir que `identidad` abre la billetera) y el nombre del evento
  en el flujo funcional.
- La cuenta del grupo y el congelamiento del precio nacen del mismo `grupo_creado`, pero en consumidores
  distintos: un grupo puede estar abierto a inscripciones antes de que su cuenta exista. Es el supuesto A1,
  ahora decidido (DD-5): cobrar exige la cuenta.

**Ratifica.** Producto y el dueño de `nucleo-financiero`; Cumplimiento para la condición de operatividad de
la billetera de usuario.

## Motivo

El dueño pidió cerrar, y la regla de la casa pide que cerrar no sea inventar. Por eso cada decisión
adopta, entre las opciones posibles, **la que menos expone** (dinero del participante, datos de identidad,
autoridad de backoffice) y **la que el código ya sostiene**: interruptores existentes (DD-2, DD-3, DD-5),
reglas de contrato ya escritas (DD-6, DD-7), o una arquitectura que ya prohíbe la alternativa (DD-4, DD-8).
Donde el repositorio no tiene la cifra (cargos, límite de consultas), se decide **la forma** y la cifra
queda sintética y a cargo de su dueño.

## Alternativas descartadas

Se detallan por decisión, dentro de cada bloque. Las comunes a todas:

- **Dejarlas abiertas hasta tener a Producto, Finanzas y Legal:** es lo que había; los despachos entre
  servicios seguían divergiendo (DD-1) o con interruptores sin valor de producción (DD-2, DD-3).
- **Cambiar el código dentro de este ADR:** el carril es de documentación; la implementación pertenece a
  cada servicio y se rastrea en las consecuencias.

## Consecuencias

**Implementación pendiente** (no la hace este ADR):

| Decisión | Qué falta | Dueño |
|---|---|---|
| DD-1 | `nucleo-financiero` paga `precio − cargos` y acredita `cargos`; `OfertaParaPago` con `cargos` (aditivo, versionado). En curso en otro carril (sin versionar al 2026-10-11); falta verificarlo con ambos servicios | `nucleo-financiero`, `entregas` |
| DD-2 | Fijar `true` en producción; recomendado invertir el valor por omisión del compose desplegado | Operaciones, `scripts/generar_compose.py` |
| DD-3 | Encender el consumo cuando se confirme el consumidor; nunca el modo automático | Operaciones, `grupos` |
| DD-4 | Límite de consultas y rastro de cada consulta | `identidad` |
| DD-8 | Terminar y verificar contra Kafka real los consumidores de `usuario_registrado` y `grupo_creado` (en curso, Carril Q) | `nucleo-financiero` |

**Lo que este registro NO puede cerrar con un documento:** cualquier cifra, calificación o autorización
que depende de una persona, una norma o un tercero (cargos y tarifas, tratamiento tributario, tope por
operación, capital de reserva, aliado e inversión, titularidad y custodia, límite de consultas). Queda
declarado en cada bloque con su dueño.

## Cómo se verifica

- Documental: `python scripts/verificar_boveda.py` y `python scripts/verificar_criterios.py` con RC 0
  (evidencia en `evidencia/carril-W/`).
- DD-2: `GruposAdmisionHumanaWebTest`, `GruposSolicitudesWebTest`, `InvitacionesAdmisionHumanaWebTest`
  (ambos modos, ambas puertas).
- DD-3: `CU68SustitucionAutomaticaTest` (modo manual por omisión, idempotencia, falla cerrado).
- DD-5: pruebas de la coreografía de apertura de `grupos` y `tarifas` (casos 7a y 7b de `CU-20`).
- DD-6 y DD-7: `PagoDeFacturaWebTest`, `CU103Test`, `CU103RechazosTest`, `CU103IdempotenciaTest`
  (`servicios/erp`).
- DD-1, DD-4 y DD-8: **sin prueba que los cierre todavía**; son decisiones sin código completo. No se
  ejecutó ninguna prueba del backend para aceptar este ADR.

## Ratificación pendiente

| Decisión | Quién ratifica |
|---|---|
| DD-1 | Dirección comercial/tarifas + Finanzas + área tributaria |
| DD-2, DD-3 | Negocio y Cumplimiento |
| DD-4 | Seguridad (con `identidad` y `entregas`) |
| DD-5 | Producto + dueño de `nucleo-financiero` |
| DD-6 | Tesorería + Producto |
| DD-7 | Contabilidad + Seguridad |
| DD-8 | Producto + dueño de `nucleo-financiero` + Cumplimiento |

## Referencias

- [[ADR-051 Garantía del pozo, comisión y autogestión|ADR-051]] · [[ADR-017 Propiedad de datos por servicio|ADR-017]] ·
  [[ADR-050 Rate limiting distribuido en el borde|ADR-050]] · [[ADR-049 Doble aprobación de retiros|ADR-049]].
- [[CU-01 Registro y apertura de billetera]] · [[CU-18 Registrar y verificar una cuenta bancaria de destino]] ·
  [[CU-20 Crear grupo y congelar tarifario]] · [[CU-103 Registrar y pagar una factura de proveedor]].
