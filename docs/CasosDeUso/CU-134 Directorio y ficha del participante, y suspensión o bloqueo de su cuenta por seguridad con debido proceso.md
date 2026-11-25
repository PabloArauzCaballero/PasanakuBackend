---
tags:
  - caso-uso
  - modulo/01-identidad-usuarios-y-seguridad
  - modulo/09-auditoria-reportes-y-cumplimiento
codigo: CU-134
criticidad: alta
estado_documento: APROBADO — decisión de Pablo del 2026-11-24 («Aprobado todo lo de G ya mismo»), con D-3 y D-11 resueltas por Pablo el mismo día
actores: [Soporte, Analista de fraude, Responsable de seguridad, Segunda instancia, Participante, Sistema]
normas: [Debido proceso, protección de datos personales, ASFI Seguridad de la Información, ASFI Consumidor Financiero]
---

# CU-134 — Directorio y ficha del participante, y suspensión o bloqueo de su cuenta por seguridad con debido proceso

> **Objetivo.** Que un operador pueda encontrar a un participante y ver **solo lo que
> necesita** de su ficha, dejando rastro de cada lectura; y que suspender o bloquear su
> cuenta por seguridad siga siempre el mismo debido proceso —causal cerrada, expediente
> con evidencia, notificación probada, descargo, decisión motivada, apelación resuelta por
> otra persona— sin tocar el dinero propio de la persona ni perjudicar a sus grupos.

> [!success] Aprobado
> Nació de la decisión P-11 del inventario de gobierno de usuarios («primero un CU nuevo en la
> bóveda que Pablo aprueba; recién después se implementa»). **Pablo lo aprobó el 2026-11-24**
> («Aprobado todo lo de G ya mismo», comunicado por la sesión principal del trabajo): las
> decisiones D-1 a D-15 quedan con la opción recomendada en el borrador, listadas en
> *Decisiones aprobadas*. Lo marcado **APROBADO** era propuesta en el borrador. Los números
> de plazo (D-3) y la retención de la entrega (D-11) los fijó Pablo aparte, el mismo día, **sin
> fuente normativa**: son valores de plataforma editables sin tocar código. Pablo también fijó el destino de la entrega retenida y el plazo de resolución de la apelación el 2026-11-25 (ver *Decisiones aprobadas*).

## Qué ya existe y no se duplica

Este caso **no** reemplaza a ninguno de estos; los invoca o se aparta de ellos.

| Situación | Dónde vive | Qué hace este caso |
| --- | --- | --- |
| Rechazo del login de una cuenta `SUSPENDIDO` o `BLOQUEADO_SEGURIDAD` (AP-CU04-02) y bloqueo por intentos fallidos | [[CU-04 Autenticar con MFA y registrar dispositivo]] · [[bloqueo_cuenta]] | Escribe el estado; el rechazo lo sigue haciendo CU-04 |
| Mora: restricción proporcional (`NO_UNIRSE`, `NO_CREAR_GRUPO`, `LIMITE_MONTO`…) | [[CU-25 Declarar el incumplimiento con descargo y evidencia]] · [[CU-27 Restringir al deudor e incluirlo en la lista interna]] | **Fuera.** La mora nunca suspende la cuenta: se restringe por CU-27 |
| Bloqueo de saldo por orden de autoridad | [[CU-17 Bloquear saldo por orden de autoridad]] · [[CU-45 Atender un requerimiento de autoridad]] | **Fuera.** La ficha solo lo muestra como «bloqueo de autoridad vigente», sin el contenido del oficio |
| Sospecha de lavado o financiamiento del terrorismo | [[CU-44 De alerta de monitoreo a reporte de operación sospechosa]] | **Fuera, y con deber de reserva**: este caso nunca se usa con esa causal ni muestra al operador de soporte que existe un caso |
| Incidente de seguridad que afecta muchas cuentas | [[CU-55 Gestionar un incidente de seguridad]] | El incidente puede disparar este caso cuenta por cuenta, con su `incidente_seguridad` como evidencia |
| Sanción al participante por incumplimiento en un grupo | [[sancion]] · [[apelacion_sancion]] (módulo 08) | No se reusa: `sancion.registro_id` es obligatorio y apunta a un incumplimiento |
| Sanción al organizador | [[CU-93 Sancionar al organizador y resolver su apelación]] | Si la persona además organiza, su rol de organizador va por CU-93; los grupos nunca pagan la medida |
| Reclamo del participante contra la medida | [[CU-52 Atender un reclamo en plazo]] | Vía paralela; si prospera, se revierte con compensación |
| Derechos sobre sus datos (acceso, rectificación, supresión) | [[CU-07 Ejercer derechos sobre datos personales]] | La ficha no permite editar datos personales: la rectificación va por CU-07 |
| Roles y accesos del personal interno | [[CU-08 Asignar y revocar roles de operador]] | Este caso es sobre **participantes**, no sobre operadores |

## Actores y disparador

- **Directorio y ficha:** cualquier operador con el permiso de lectura de participantes
  (soporte, cumplimiento, seguridad). Disparador: un ticket, un reclamo, una alerta o un
  expediente que exige ver a la persona.
- **Suspensión o bloqueo:** propone el **analista de fraude** o el **responsable de
  seguridad**; decide una persona **distinta** de quien propuso; la apelación la resuelve
  una **tercera** instancia (*APROBADO*, ver D-5).
- **Disparadores de la medida:** hallazgo de seguridad sobre la cuenta (toma de cuenta,
  dispositivo anómalo), fraude con evidencia, abuso contra otros participantes, incidente
  de seguridad ([[CU-55 Gestionar un incidente de seguridad]]).
- **Participante:** recibe la notificación, presenta descargo y, si quiere, apelación.

## Precondiciones

1. El operador tiene sesión de backoffice con segundo factor confirmado (`R-SEG-10`) y el
   permiso que la acción exige (tabla de permisos más abajo). La interfaz no es barrera:
   el servicio niega por omisión.
2. El operador **no** es el participante sobre el que actúa (`R-SEG-04`, no actuar sobre
   sí mismo). Un operador puede ser también participante en la app.
3. Para leer la ficha hay **justificación** escrita de al menos 10 caracteres
   (`ck_acceso_justificacion` de [[registro_acceso_datos]]) y, si existe, el ticket o
   expediente que la motiva.
4. Para proponer una medida existe **causal del catálogo cerrado** (paso 6) y al menos
   una evidencia.
5. Existe una política vigente **anterior al hecho** que fija plazos de descargo,
   apelación y prescripción para estas medidas (*APROBADO*, D-3): [[politica_medida_cuenta]],
   la política de plataforma de `identidad`, sembrada con los valores de D-3.

## Flujo principal

### A · Directorio y ficha

1. **Buscar.** El operador busca por `usuario.codigo_publico` o por teléfono exacto, o
   filtra por `estado` y `nivel_kyc`. El listado es **paginado por cursor** con orden
   estable (`fecha_registro`, `id`) y devuelve por fila solo: código público, nombre
   abreviado («Juan P.»), `estado`, `nivel_kyc`, fecha de registro y marcas booleanas
   (tiene restricción vigente, tiene bloqueo vigente, tiene medida de seguridad en curso).
   Cada búsqueda escribe en [[registro_acceso_datos]] una fila por persona devuelta, con
   `operacion='BUSQUEDA'` y la justificación; sin resultados no se leyó el dato de nadie. El término buscado viaja en el cuerpo, nunca en
   la URL (regla 90.2.2).
2. **Abrir la ficha.** Devuelve los datos **mínimos y enmascarados**:
   - identidad: nombre completo, código público, `estado`, `nivel_kyc`, fecha de registro,
     último acceso;
   - contacto **enmascarado**: teléfono `+591 7•••••12`, correo `j•••@g•••.com`; el
     documento de identidad solo como tipo y últimos dígitos;
   - participación: grupos activos con su estado y **si tiene obligaciones vencidas** (sí/no
     y cantidad), no los importes;
   - medidas vigentes: restricciones de [[restriccion_usuario]], bloqueos de
     [[bloqueo_cuenta]], medidas de este caso con su línea de tiempo, y «bloqueo de autoridad
     vigente» sin contenido del oficio;
   - **no** muestra: saldo, movimientos, cuentas bancarias, reputación detallada, alertas o
     casos de cumplimiento.
   La apertura escribe [[registro_acceso_datos]] con `operacion='LECTURA'` en la **misma
   transacción** que lee (si no se puede registrar, no se muestra).
3. **Revelar un dato** (teléfono o correo; el documento solo se muestra por tipo y últimos dígitos) exige el permiso `DATOS_SENSIBLES_LEER`
   (existe en la semilla), **segundo factor en el momento** (`R-SEG-12`) y una justificación
   propia. Se revela **un campo por vez**, deja su propia fila en [[registro_acceso_datos]]
   y el valor no se guarda en el navegador ni en logs.
4. **Ver datos financieros** (saldo, movimientos, cuentas bancarias) no es parte de la
   ficha: lo hace quien tenga `BILLETERA_VER_TERCEROS` en sus pantallas propias, con su
   propio rastro.
5. **Base legal del tratamiento** (*APROBADO*, ver D-9): Bolivia no tiene todavía ley
   integral de protección de datos ([[Cumplimiento]] §5); la base es la ejecución del
   contrato de adhesión aceptado ([[CU-05 Aceptar contrato de adhesión y tarifario]]), la
   obligación regulatoria de seguridad de la información y la prevención del fraude. La
   ficha muestra al operador la finalidad que declaró y no permite exportar.

### B · Suspensión o bloqueo de la cuenta por seguridad

6. **Causal cerrada** (*APROBADO*, ver D-1). El catálogo es
   un concepto codificado, no texto libre:

   | Causal | Qué la prueba | ¿Admite medida cautelar inmediata? |
   | --- | --- | --- |
   | `CUENTA_COMPROMETIDA` | [[intento_autenticacion]], [[dispositivo]], [[sesion]] anómalos, aviso del propio titular | Sí: protege al titular |
   | `FRAUDE_CON_EVIDENCIA` | suplantación de identidad en [[verificacion_kyc]], comprobantes falsos, uso de medios de pago ajenos | Sí |
   | `ABUSO_CONTRA_PARTICIPANTES` | denuncias procedentes, amenazas o acoso documentados | **No**: primero descargo |
   | `CUENTA_DUPLICADA_O_DE_TERCERO` | la misma persona con otra cuenta, o cuenta usada por otra persona, contra el contrato de adhesión | **No**: primero descargo |

   Quedan **fuera** de este catálogo: mora (CU-27), orden de autoridad (CU-17), sospecha
   LGI/FT (CU-44, deber de reserva). Intentar usarlas responde `CAUSAL_FUERA_DE_CATALOGO`.

7. **Medidas** (*APROBADO*, ver D-2), sobre los estados que **ya existen** en
   `ck_usuario_estado`:
   - **Bloqueo cautelar** → `usuario.estado = 'BLOQUEADO_SEGURIDAD'` y fila en
     [[bloqueo_cuenta]] con `motivo='FRAUDE'` (o el motivo que se apruebe para cuenta
     comprometida). Solo para causales que lo admiten, y siempre **seguido** del
     procedimiento: el descargo corre igual y puede revertirlo.
   - **Suspensión temporal** → `usuario.estado = 'SUSPENDIDO'` con fin de vigencia guardado.
   - **No existe inhabilitación definitiva** (D-2 aprobada): una cuenta que no debe seguir
     operando se cierra por [[CU-16 Cerrar billetera y devolver saldo]], nunca reteniendo el saldo.

8. **Abrir el expediente.** Se crea [[expediente_medida_cuenta]] (*APROBADO*, D-4) con `usuario_id`,
   `causal`, `medida`, `cautelar`, `motivo`, `propuesta_por`, `clave_idempotencia` y estado
   `PROPUESTA`. Las evidencias se guardan **inmutables**, distinguiendo las que aporta el
   sistema de las que aporta el afectado (mismo criterio que `R-GAR-02`). Todo **en la misma
   transacción** con su `evento_dominio` (`R-AUD-04`).
9. **Cautelar.** Si la causal lo admite y quien propone marca `cautelar`, en la misma
   transacción: se cambia `usuario.estado`, se crea [[bloqueo_cuenta]], se revocan sus
   [[sesion]] vivas y sus refrescos (`R-SEG-09`), y el expediente pasa a `CAUTELAR`.
10. **Notificar.** Se envía por [[CU-80 Despachar una notificación]] la causal, la evidencia
    que la persona puede ver, la medida, **cómo presentar descargo** y el plazo. **El plazo
    corre desde la entrega probada** con [[evento_entrega_mensaje]], no desde el envío; si
    la persona no tiene canal verificado activo, el plazo **no corre** hasta acreditar el
    aviso por otra vía. La fecha límite se calcula en días hábiles
    ([[CU-59 Mantener el calendario de días no hábiles]]) **al notificar y se guarda**; nunca
    se recalcula.
11. **Descargo.** El participante presenta su argumento y evidencias desde la app. Con
    `SUSPENDIDO` la app **no** se le cierra del todo (ver paso 15 y D-6): puede entrar en
    modo restringido para ver la medida y defenderse. Con `BLOQUEADO_SEGURIDAD` por cuenta
    comprometida, el descargo entra por un canal verificado alternativo
    (*APROBADO*, ver D-6), porque la sesión podría ser del atacante.
12. **Decisión motivada.** Vencido el plazo o presentado el descargo, decide una persona
    **distinta** de quien propuso (`R-SEG-04`). Escribe el fundamento, que responde al
    descargo. Resultado:
    - `CONFIRMADA` → el expediente pasa a `FIRME` recién al vencer el plazo de apelación sin
      apelación, o al rechazarse la apelación; mientras tanto la medida rige si era cautelar,
      y si no lo era **se aplica recién al quedar firme** (*APROBADO*, ver D-7);
    - `DESESTIMADA` → si había cautelar, se revierte (paso 16).
    No existe decisión por silencio en contra del afectado: si nadie decide antes de
    `prescribe_en_dias`, el expediente se cierra `PRESCRITO` y no se reabre por el mismo hecho.
13. **Apelación.** Una sola por expediente (mismo criterio que `R-ORG-05`), dentro del plazo
    guardado. La resuelve una tercera persona, que **no** propuso ni decidió (`R-SEG-04`).
    El plazo de resolución es de **10 días hábiles desde la recepción acreditada de la apelación**.
    Se calcula con CU-59 y se guarda al recibirla. Si la instancia no resuelve en ese plazo,
    **se resuelve a favor del apelante** y se revierte la medida de este expediente.
14. **Efectos firmes.** Con el expediente `FIRME`: el estado queda con su fin de vigencia,
    se registra un [[evento_reputacion]] **solo** para `FRAUDE_CON_EVIDENCIA` y
    `ABUSO_CONTRA_PARTICIPANTES` (*APROBADO*, ver D-8; nunca para cuenta comprometida, que
    es víctima, no autor), uno por hecho (`R-REP-01`), por [[CU-70 Registrar un evento de reputación]].
15. **Lo que la medida nunca alcanza** (principio ya escrito en CU-27 §3, extendido aquí):
    la persona **siempre** puede ver su saldo, pagar lo que debe, recibir lo que le
    corresponde a una cuenta verificada propia y reclamar. Cómo se compatibiliza con el
    rechazo total de login de CU-04 es la decisión D-6.
16. **Fin de la medida.** Al vencer la vigencia (trabajo diario), al desestimarse, al
    prosperar la apelación o al recuperarse la cuenta comprometida: `usuario.estado` vuelve
    al estado previo guardado en el expediente, [[bloqueo_cuenta]] recibe `liberada_en` y
    `liberada_por`, y si la medida fue indebida se registra un evento de reputación
    **compensatorio** (no se borra el original) y, si hubo perjuicio económico medible,
    entra como [[evento_riesgo_operativo]] ([[CU-54 Registrar un evento de riesgo operativo]]).
    **Ninguna fila del expediente se borra** (`R-AUD-01`).

### C · Efectos sobre grupos y dinero en curso

17. **Los grupos no pagan la medida.** Ningún aporte de los demás se retiene, ningún
    sorteo se repite y ningún turno ajeno se mueve por la medida.
18. **Obligaciones y lugar en el grupo** (D-10 y D-12 aprobadas): las obligaciones de aporte
    pendientes **siguen exigibles** y la persona puede pagarlas durante la medida (coherente
    con CU-27 §3); **no se la reemplaza** en el grupo por esta medida
    ([[CU-66 Reemplazar a un participante moroso]] sigue siendo solo para mora).
    **Entrega durante la medida (D-11 aprobada):** si el turno del participante cae mientras
    tiene una medida vigente, su entrega **se retiene en custodia** sin perderse ni moverse a
    otro: no se le acredita hasta resolver. Se **libera** a su cuenta verificada si la medida
    se levanta, se desestima, se revierte o vence, siempre que no exista una restricción independiente
    vigente. Si la medida **queda firme**, la custodia sigue segregada a favor del titular solo
    mientras la medida esté vigente. La firmeza no cambia el beneficiario ni extingue su derecho.
    Si falta una cuenta de destino verificada, se conserva la custodia a favor del titular y se
    deriva a revisión. Toda liberación exige conciliación e idempotencia. La retención
    la ejecuta el servicio `entregas` al consultar a `identidad` si el beneficiario tiene medida
    vigente; el contrato entre servicios y su idempotencia se definen al implementar (reglas 91
    y 98), sin cambiar la liquidación más de lo necesario.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | El operador busca sin justificación o con menos de 10 caracteres | Se rechaza con `JUSTIFICACION_INSUFICIENTE`; no se ejecuta la búsqueda |
| 2a | No se puede escribir el registro de lectura | No se muestra la ficha: sin rastro no hay lectura (`R-SEG-02`) |
| 2b | El operador abre su propia ficha de participante | Se rechaza con `ACTUA_SOBRE_SI_MISMO`; su cuenta la ve en la app |
| 3a | Revela un dato sin segundo factor reciente | Se rechaza con `SEGUNDO_FACTOR_REQUERIDO` (`R-SEG-12`) |
| 6a | Se intenta suspender por mora, por orden de autoridad o por sospecha LGI/FT | Se rechaza con `CAUSAL_FUERA_DE_CATALOGO` y se indica el caso que corresponde, sin mencionar cumplimiento al operador de soporte |
| 8a | Mismo pedido reenviado con la misma `clave_idempotencia` | Devuelve el mismo expediente, no abre otro |
| 8b | Ya hay un expediente abierto por la misma causal sobre la cuenta | Se rechaza con `EXPEDIENTE_ABIERTO`; la evidencia nueva se agrega al existente |
| 9a | Se pide cautelar para una causal que no la admite | Se rechaza con `CAUTELAR_NO_ADMITIDA` |
| 10a | La persona no tiene canal verificado activo | El plazo queda sin correr y el expediente queda `NOTIFICACION_PENDIENTE`; el cautelar, si lo hubo, sigue |
| 12a | Se intenta decidir antes de vencer el plazo de descargo sin descargo presentado | Se rechaza con `PLAZO_DESCARGO_ABIERTO` |
| 12b | Decide la misma persona que propuso | Se rechaza con `MISMA_PERSONA` (`R-SEG-04`) |
| 13a | Segunda apelación sobre el mismo expediente | Se rechaza con `APELACION_DUPLICADA` |
| 13b | La instancia no resuelve en su plazo | La apelación se resuelve a favor del apelante y la medida se revierte (paso 16) |
| — | Venció `prescribe_en_dias` sin decisión | Se cierra `PRESCRITO`; un intento de decidir responde `EXPEDIENTE_PRESCRITO` |
| — | Hay un reclamo en curso contra la medida ([[CU-52 Atender un reclamo en plazo]]) | El expediente no queda firme mientras el reclamo esté en revisión (mismo criterio que AP-CU27-07 y AP-CU93-07): `RECLAMO_EN_CURSO` |
| — | La persona es también organizador | La medida de cuenta no reasigna grupos por sí sola; si corresponde, se abre [[CU-93 Sancionar al organizador y resolver su apelación]] aparte |
| — | La cuenta tiene un caso LGI/FT abierto | El operador de este caso no lo ve; si cumplimiento necesita congelar, lo hace por CU-44 sin avisar al titular |

## Postcondiciones

- Toda lectura de la ficha y toda revelación de un dato tienen su fila en
  [[registro_acceso_datos]] con justificación.
- Ninguna cuenta queda suspendida o bloqueada por este caso sin expediente, causal del
  catálogo, evidencia y notificación intentada; ninguna queda **firme** sin descargo
  posible, decisión motivada de otra persona y plazo de apelación vencido o resuelto.
- El dinero propio de la persona y el de sus grupos no se mueve por esta medida.

## Contrato · `openapi/identidad.yaml`

> *Contrato aprobado.* Todavía no está en `identidad.yaml`; se escribe al implementarse. Los importes no aparecen: la ficha no devuelve dinero.

```ts
export const BusquedaCU134 = z.object({
  codigoPublico: z.string().max(12).optional(),
  telefonoE164:  z.string().max(20).optional(),
  estado: z.enum(['ACTIVO','BAJA_VOLUNTARIA','BLOQUEADO_SEGURIDAD','ELIMINADO',
                  'INCOMPLETO_KYC','PENDIENTE_VERIFICACION','PRE_REGISTRO','SUSPENDIDO']).optional(),
  justificacion: z.string().min(10).max(300),
  ticketSoporteId: z.string().max(30).nullable(),
  cursor: z.string().nullable(),
  limite: z.number().int().min(1).max(50),
}).strict()   // POST /identidad/participantes/busquedas

export const AperturaFichaCU134 = z.object({
  justificacion: z.string().min(10).max(300),
  ticketSoporteId: z.string().max(30).nullable(),
}).strict()   // POST /identidad/participantes/{usuarioId}/ficha

export const RevelacionCU134 = z.object({
  campo: z.enum(['TELEFONO','CORREO']),   // el documento va cifrado: solo tipo y ultimos digitos
  justificacion: z.string().min(10).max(300),
}).strict()   // POST /identidad/participantes/{usuarioId}/revelaciones  (step-up)

export const EntradaMedidaCU134 = z.object({
  causal: z.enum(['CUENTA_COMPROMETIDA','FRAUDE_CON_EVIDENCIA',
                  'ABUSO_CONTRA_PARTICIPANTES','CUENTA_DUPLICADA_O_DE_TERCERO']),
  medida: z.enum(['BLOQUEO_CAUTELAR','SUSPENSION_TEMPORAL']),
  cautelar: z.boolean(),
  motivo: z.string().min(30).max(500),
  evidencias: z.array(z.object({
    descripcion: z.string().max(300), referenciaId: z.string().uuid().nullable(),
  })).min(1).max(20),
  vigenteHasta: z.string().datetime().nullable(),
}).strict()   // la clave de idempotencia viaja en la cabecera Idempotency-Key   // POST /identidad/participantes/{usuarioId}/medidas

export const DecisionCU134 = z.object({
  resultado: z.enum(['CONFIRMADA','DESESTIMADA']),
  fundamento: z.string().min(50).max(4000),
}).strict()   // POST /identidad/medidas-cuenta/{expedienteId}/decision

export const DescargoCU134 = z.object({
  argumento: z.string().min(20).max(4000),
  evidencias: z.array(z.object({ descripcion: z.string().max(300) })).max(10),
}).strict()   // POST /identidad/medidas-cuenta/{expedienteId}/descargo   (app, titular)

export const ApelacionCU134 = DescargoCU134   // POST /identidad/medidas-cuenta/{expedienteId}/apelacion

export const SalidaMedidaCU134 = z.object({
  expedienteId: z.string().uuid(),
  estado: z.enum(['PROPUESTA','CAUTELAR','NOTIFICACION_PENDIENTE','EN_DESCARGO','DECIDIDA',
                  'EN_APELACION','FIRME','DESESTIMADA','REVOCADA','CUMPLIDA','PRESCRITO']),
  estadoCuenta: z.enum(['ACTIVO','SUSPENDIDO','BLOQUEADO_SEGURIDAD']),
  fechaLimiteDescargo: z.string().datetime().nullable(),
  fechaLimiteApelacion: z.string().datetime().nullable(),
  apelacionId: z.string().uuid().nullable(),
}).strict()

export const ErroresCU134 = {
  JUSTIFICACION_INSUFICIENTE: 'AP-CU134-01',
  ACTUA_SOBRE_SI_MISMO:       'AP-CU134-02',
  SEGUNDO_FACTOR_REQUERIDO:   'AP-CU134-03',
  CAUSAL_FUERA_DE_CATALOGO:   'AP-CU134-04',
  CAUTELAR_NO_ADMITIDA:       'AP-CU134-05',
  EXPEDIENTE_ABIERTO:         'AP-CU134-06',
  PLAZO_DESCARGO_ABIERTO:     'AP-CU134-07',
  MISMA_PERSONA:              'AP-CU134-08',
  APELACION_DUPLICADA:        'AP-CU134-09',
  EXPEDIENTE_PRESCRITO:       'AP-CU134-10',
  RECLAMO_EN_CURSO:           'AP-CU134-11',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `JUSTIFICACION_INSUFICIENTE` | Búsqueda, ficha o revelación sin justificación de al menos 10 caracteres (`ck_acceso_justificacion`, `R-SEG-02`) |
| `ACTUA_SOBRE_SI_MISMO` | El operador lee, propone, decide o resuelve sobre su propia cuenta de participante (`R-SEG-04`) |
| `SEGUNDO_FACTOR_REQUERIDO` | Revelar un dato o decidir una medida sin segundo factor reciente (`R-SEG-12`) |
| `CAUSAL_FUERA_DE_CATALOGO` | La causal no está en el catálogo cerrado (mora, autoridad o LGI/FT van por otro caso) |
| `CAUTELAR_NO_ADMITIDA` | Se pide medida cautelar para una causal que exige descargo previo |
| `EXPEDIENTE_ABIERTO` | Ya hay un expediente abierto por la misma causal sobre la cuenta |
| `PLAZO_DESCARGO_ABIERTO` | Se intenta decidir antes de vencer el plazo de descargo y sin descargo presentado |
| `MISMA_PERSONA` | Decide quien propuso, o resuelve la apelación quien propuso o decidió (`R-SEG-04`) |
| `APELACION_DUPLICADA` | Ya existe una apelación para ese expediente |
| `EXPEDIENTE_PRESCRITO` | Venció `prescribe_en_dias` sin decisión: no se reabre por el mismo hecho |
| `RECLAMO_EN_CURSO` | Hay un reclamo en revisión contra la medida: no queda firme mientras tanto (regla 91.4) |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `enmascarar(campo, valor)` | Teléfono, correo y documento enmascarados; puro |
| Átomo | `causalAdmiteCautelar(causal)` | Lee el catálogo cerrado; puro |
| Átomo | `plazosDeMedida(politica, notificadoEn, calendario)` | Descargo, apelación y prescripción en días hábiles; puro |
| Molécula | `ParticipantesRepositorio` | Directorio, ficha y rastro en [[registro_acceso_datos]] en la transaccion de la lectura |
| Molécula | `MedidasDeCuentaRepositorio` | Expediente, evidencia e historial append-only, apelacion, estado de cuenta, bloqueo y calendario habil |
| Molécula | `AccesosRepositorio.cerrarSesionesDe` | Compartido con [[CU-04 Autenticar con MFA y registrar dispositivo]] y CU-08 |
| Organismo | `CU134ConsultarParticipante` | Transacción: busqueda, ficha y revelacion con su rastro; medida vigente para `entregas` |
| Organismo | `CU134GestionarMedida` | Transacción: proponer, notificar, descargo, decidir, apelar, resolver y el trabajo diario de vencimientos |
| Página | `POST /identidad/participantes/busquedas` · `POST /identidad/participantes/{id}/ficha` · `POST /identidad/participantes/{id}/medidas` · `POST /identidad/medidas-cuenta/{id}/decision` | Traduce y delega |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| — (las lecturas van a [[registro_acceso_datos]], no al outbox) | — | `PARTICIPANTES_LEER` (*APROBADO*, permiso nuevo) |
| — (consulta servicio a servicio de `entregas`, D-11) | — | `PARTICIPANTES_MEDIDA_VIGENTE_LEER` (*APROBADO*, permiso nuevo de mínimo privilegio: solo la credencial `SERVICIO_ENTREGAS`; no abre directorio ni ficha) |
| — | Revelación de un campo | `DATOS_SENSIBLES_LEER` (existe) + segundo factor |
| `cuenta_participante.medida_propuesta` | Notificación con causal, evidencia visible y cómo defenderse | `PARTICIPANTES_MEDIDA_PROPONER` (*APROBADO*) |
| `cuenta_participante.bloqueo_cautelar` | Revocación de sesiones y aviso al titular | `PARTICIPANTES_MEDIDA_PROPONER` |
| `cuenta_participante.medida_decidida` | Aviso con el fundamento y el plazo de apelación | `PARTICIPANTES_MEDIDA_DECIDIR` (*APROBADO*) |
| `cuenta_participante.medida_firme` | Evento de reputación cuando la causal lo prevé | — |
| `cuenta_participante.medida_revocada` | Restitución, reputación compensatoria y riesgo operativo si hubo perjuicio | `PARTICIPANTES_APELACION_RESOLVER` (*APROBADO*) |
| — | Trabajo diario: vence descargos, apelaciones y vigencias; prescribe; resuelve a favor del apelante si la instancia no respondió | — |

Segregación (*APROBADO*, ver D-5): proponer, decidir y resolver la apelación son tres
permisos que **una misma persona no ejerce sobre el mismo expediente**, verificado en la
base, no solo en la aplicación. Asignación sugerida de roles existentes: `SOPORTE` →
leer; `FRAUD_ANALYST` → proponer; `RESPONSABLE_SEGURIDAD` → decidir; comité de gobierno
([[CU-94 Elevar una decisión al comité de gobierno]]) u `OFICIAL_CUMPLIMIENTO` → apelación.

## Interfaz

- **App:** con la cuenta suspendida, una pantalla de estado que dice **qué** pasó, **por
  qué** (la causal en lenguaje claro), **hasta cuándo**, y los botones para presentar
  descargo, apelar, pagar y reclamar. Nunca «no disponible» seco.
- **Backoffice:** nueva entrada «Participantes» con directorio en `TablaDeDatos`
  (cuatro estados), ficha enmascarada con botón «Revelar» por campo, y expediente de
  medida con línea de tiempo, plazos en semáforo y la bandeja de decisión y de apelación,
  cada una visible solo para su permiso (regla de pantalla 0.4: un operador nunca ve los
  dos botones).

## Restricciones aplicables

`R-SEG-02` · `R-SEG-03` · `R-SEG-04` · `R-SEG-09` · `R-SEG-10` · `R-SEG-12` · `R-SEG-13` ·
`R-SEG-14` · `R-SEG-15` · `R-AUD-01` · `R-AUD-04` · `R-REP-01`

`R-SEG-13`: proponente, decisor y resolutor de la apelación son tres personas distintas y
ninguna es el afectado (CHECK y trigger). `R-SEG-14`: un expediente abierto por cuenta y causal
(UNIQUE parcial). `R-SEG-15`: no hay decisión sin fundamento ni firmeza sin decisión confirmada.
Una apelación por expediente la garantiza el UNIQUE de `apelacion_medida_cuenta`; la evidencia
y el historial son append-only (`R-AUD-01`).

## Evidencia que deja

[[expediente_medida_cuenta]] · [[evidencia_medida_cuenta]] · [[historial_estado_medida_cuenta]] ·
[[apelacion_medida_cuenta]] · [[politica_medida_cuenta]] · [[registro_acceso_datos]] ·
[[bloqueo_cuenta]] · [[sesion]] · [[evento_entrega_mensaje]] · [[evento_reputacion]] ·
[[evento_riesgo_operativo]] · [[bitacora_evento]] · `evento_dominio`

## Datos sensibles

| Dato | Dónde aparece | Tratamiento |
| --- | --- | --- |
| Teléfono, correo, documento | Ficha | Enmascarados; revelación por campo con segundo factor, justificación y rastro |
| Nombre | Directorio | Abreviado («Juan P.»); completo solo en la ficha |
| Término de búsqueda | Búsqueda | En el cuerpo, nunca en la URL ni en logs (regla 90.2.2) |
| Causal y evidencias | Expediente | Visibles para el titular y para quien tiene el permiso; nunca en logs ni en el evento de dominio (el evento lleva identificadores, no contenido) |
| Existencia de un caso LGI/FT | — | Nunca se muestra en este caso (deber de reserva, CU-44) |
| Saldo, movimientos, cuentas | — | No forman parte de la ficha |

## Criterios de aceptación

```gherkin
Dado un operador con permiso de lectura de participantes
Cuando abre la ficha de un participante con una justificación válida
Entonces ve el teléfono y el correo enmascarados
Y existe una fila en registro_acceso_datos con operacion LECTURA y su justificación

Dado un operador que busca participantes sin justificación
Cuando envía la búsqueda
Entonces se rechaza con JUSTIFICACION_INSUFICIENTE y no se devuelve ninguna fila

Dado un operador sin segundo factor reciente
Cuando pide revelar el teléfono de un participante
Entonces se rechaza con SEGUNDO_FACTOR_REQUERIDO

Dado un operador que también es participante
Cuando intenta abrir su propia ficha o proponer una medida sobre su cuenta
Entonces se rechaza con ACTUA_SOBRE_SI_MISMO

Dado un participante con deuda vencida
Cuando un analista intenta suspender su cuenta por mora
Entonces se rechaza con CAUSAL_FUERA_DE_CATALOGO y se indica el caso de restricción por mora

Dada una propuesta de suspensión por abuso contra participantes
Cuando se la marca como cautelar
Entonces se rechaza con CAUTELAR_NO_ADMITIDA

Dada una cuenta comprometida con evidencia de sesiones anómalas
Cuando el analista propone un bloqueo cautelar
Entonces la cuenta queda BLOQUEADO_SEGURIDAD, sus sesiones quedan revocadas y el expediente queda CAUTELAR
Y al acreditarse la notificación se guarda la fecha límite de descargo y el expediente pasa a EN_DESCARGO

Dado un expediente con el plazo de descargo abierto y sin descargo
Cuando se intenta decidir
Entonces se rechaza con PLAZO_DESCARGO_ABIERTO

Dado un expediente propuesto por un analista
Cuando el mismo analista intenta decidirlo
Entonces se rechaza con MISMA_PERSONA

Dada una propuesta de medida reenviada con la misma clave de idempotencia
Cuando llega por segunda vez
Entonces devuelve el mismo expedienteId y no se crea otro expediente

Dada una apelación que una tercera persona acepta
Cuando la resuelve
Entonces la medida queda REVOCADA, la cuenta vuelve a su estado previo
Y se emite identidad.medida_cuenta_revocada sin borrar el historial del expediente

Dada una apelación recibida y pendiente de resolución
Cuando vencen los diez días hábiles sin decisión
Entonces la apelación queda ACEPTADA_POR_VENCIMIENTO y la medida se revierte
Y se emite una sola vez identidad.medida_cuenta_revocada

Dada una cuenta suspendida con aportes pendientes en un grupo
Cuando se aplica la suspensión
Entonces ninguna obligación, entrega ni cupo del grupo cambia por la medida
```

## Decisiones aprobadas

Aprobadas por Pablo el 2026-11-24 («Aprobado todo lo de G ya mismo»): en cada una rige la
opción que el borrador recomendaba. Fuente: esta aprobación, comunicada por la sesión
principal del trabajo de gobierno de usuarios.

| ID | Tema | Decisión aprobada |
| --- | --- | --- |
| — | Número del caso | **CU-134** (CU-133 y CU-136 se usan como prefijo de pruebas en `servicios/entregas`) |
| D-1 | Causales | Catálogo cerrado de cuatro: `CUENTA_COMPROMETIDA`, `FRAUDE_CON_EVIDENCIA`, `ABUSO_CONTRA_PARTICIPANTES`, `CUENTA_DUPLICADA_O_DE_TERCERO` |
| D-2 | Medidas | Bloqueo cautelar y suspensión temporal. **No** hay inhabilitación definitiva ni amonestación |
| D-3 | Plazos | Viven en [[politica_medida_cuenta]], política de plataforma propia de `identidad` (no en `politica_sancion`, que es de `garantia`: regla 98.1): **descargo 5 días hábiles, apelación 5 días hábiles, prescripción del expediente 12 meses, duración máxima de la suspensión 90 días**. Valor de plataforma aprobado por Pablo el 2026-11-24, **sin fuente normativa**; editable sin tocar código |
| D-4 | Expediente | Tablas nuevas en `identidad`: [[expediente_medida_cuenta]], [[evidencia_medida_cuenta]] y [[historial_estado_medida_cuenta]] (append-only) y [[apelacion_medida_cuenta]] |
| D-5 | Quién actúa | Permisos nuevos `PARTICIPANTES_MEDIDA_PROPONER`, `PARTICIPANTES_MEDIDA_DECIDIR` y `PARTICIPANTES_APELACION_RESOLVER`, asignados de inicio a `FRAUD_ANALYST`, `RESPONSABLE_SEGURIDAD` y el comité de gobierno respectivamente; tres personas distintas por expediente |
| D-6 | Defensa con cuenta suspendida | Modo restringido: con `SUSPENDIDO` entra solo a ver la medida, presentar descargo y apelación, pagar y reclamar. Con cuenta comprometida, canal verificado alternativo |
| D-7 | Sin cautelar | La suspensión rige recién al quedar firme |
| D-8 | Reputación | Solo una medida firme por `FRAUDE_CON_EVIDENCIA` o `ABUSO_CONTRA_PARTICIPANTES` produce evento de reputación |
| D-9 | Base legal | Contrato de adhesión + seguridad de la información + prevención del fraude ([[Cumplimiento]] §5) |
| D-10 | Aportes pendientes | Siguen exigibles y la persona puede pagarlos |
| D-11 | Entrega durante la medida | **Se retiene en custodia hasta resolver** (decisión de Pablo del 2026-11-24): si queda firme se conserva a favor del titular solo mientras la medida esté vigente; se libera al terminar, previa verificación de otras restricciones y de la cuenta destino |
| D-12 | Reemplazo en el grupo | Nunca por esta medida |
| D-13 | Inhabilitación con saldo o deuda | No aplica: D-2 no incluye inhabilitación definitiva |
| D-14 | Registro de lectura | Se agregan `FICHA_PARTICIPANTE` y `CORREO` a `registro_acceso_datos.tipo_dato` |
| D-15 | Motivo de bloqueo | Se agrega `CUENTA_COMPROMETIDA` al CHECK de [[bloqueo_cuenta]] |

## Ambigüedades abiertas

| ID | Ambigüedad | Supuesto mientras tanto | A quién confirmar |
| --- | --- | --- | --- |
| A-1 | **Resuelta por Pablo (2026-11-25)**: custodia a favor del titular mientras siga vigente la medida | Al terminar, liberar a cuenta verificada solo si no hay otra restricción vigente | — |
| A-2 | ~~Unidad de la prescripción~~ — **resuelta**: [[politica_medida_cuenta]] guarda `prescribe_en_meses` y `duracion_maxima_suspension_dias` en columnas propias | — | — |
| A-3 | **Resuelta por Pablo (2026-11-25)**: la segunda instancia dispone de 10 días hábiles desde la recepción acreditada | Si vence el plazo sin resolución, se acepta la apelación y se revierte la medida | — |

## Ver también

[[CU-07 Ejercer derechos sobre datos personales]] · [[CU-17 Bloquear saldo por orden de autoridad]] · [[CU-27 Restringir al deudor e incluirlo en la lista interna]] · [[CU-44 De alerta de monitoreo a reporte de operación sospechosa]] · [[CU-52 Atender un reclamo en plazo]] · [[CU-55 Gestionar un incidente de seguridad]] · [[CU-93 Sancionar al organizador y resolver su apelación]] · [[Seguridad]] · [[Restricciones]]
