---
name: kyc-identity-verification
description: Gate de diseño de la verificación de identidad — niveles de verificación atados a lo que habilitan, documento de identidad y prueba de vida, verificación de teléfono y correo con tokens de un solo uso, estados del expediente KYC y su revalidación, unicidad de persona sin duplicados ni falsos positivos, minimización y retención de los datos y las imágenes del documento, y el manejo honesto del rechazo y la revisión manual. Usar al construir o tocar el alta de usuarios, la verificación de documento, los estados de KYC, o cualquier regla que condicione una acción a estar verificado.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Verificación de identidad (KYC)

En un pasanaku, saber quién es la otra persona **es el producto**. Si cualquiera puede crear
tres cuentas y tomar tres cupos sin intención de pagar, el fondo de garantía se vacía y la
reputación no significa nada.

Servicio dueño: `identidad` (M1). Entidades: `documento_identidad`, `verificacion_kyc`,
`referencia_personal`, `token_verificacion`. Cumplimiento: `aml-sanctions-screening`.

## 1. Niveles atados a lo que habilitan

No hay "verificado" y "no verificado": hay **niveles**, y cada uno habilita cosas concretas.

| Nivel | Requisitos | Habilita |
|---|---|---|
| 0 — Registrado | Correo o teléfono verificado | Explorar, ser invitado |
| 1 — Contactable | Teléfono verificado + perfil completo | Postularse a un grupo |
| 2 — Identificado | Documento validado + prueba de vida | **Tomar un cupo**, aportar |
| 3 — Cobrable | Nivel 2 + cuenta bancaria verificada | **Recibir una entrega** |

- El nivel se verifica **en el servidor, en el momento de la acción** (`authz-access-control`),
  no escondiendo botones.
- El nivel mínimo por acción es **configuración versionada**, no constantes desparramadas.
- Subir de nivel es un evento de dominio; bajar también (documento vencido, revisión abierta).

## 2. Documento y prueba de vida

- Tipos de documento en **catálogo cerrado** (`terminology-value-sets`), con su formato y su
  validación. Bolivia: CI con su formato y sus casos de complemento.
- **Unicidad del documento**: un documento identifica a **una** cuenta. Índice único sobre la
  forma normalizada (sin puntos, sin espacios, con el complemento tratado de forma consistente).
  Sin normalización, `1234567 LP` y `1234567-LP` son dos personas para el sistema y una para el
  mundo.
- Prueba de vida y comparación con el documento: contra un proveedor, con su resultado y su
  puntaje guardados, **no la decisión sola**. Cuando alguien reclame, hace falta saber con qué
  evidencia se decidió.
- **Las imágenes del documento y los datos biométricos son el dato más sensible del sistema**
  (`data-privacy-financial` §1): cifradas, con acceso auditado, retención declarada y borradas o
  anonimizadas cuando vence la obligación de conservarlas.
- Prohibido usar la imagen del documento como avatar, miniatura o adjunto reutilizable.

## 3. Verificación de teléfono y correo

- Con `token_verificacion` (M1): OTP o enlace firmado, **hash con pepper**, nunca el valor
  plano (regla 90.2.9).
- `politica_token` por propósito: vigencia, intentos máximos, reenvío con backoff.
- `intento_validacion_token` registra cada intento: es lo que detecta fuerza bruta.
- **Enumeración**: el mensaje no revela si el teléfono ya existe. "Si el número está registrado,
  te enviamos un código" (`auth-session-pentest`).
- El teléfono verificado es el canal de WhatsApp para pagos y avisos: cambiarlo exige
  reverificación **y** notificación al canal anterior.

## 4. Estados del expediente

```
INICIADO → EN_REVISION_AUTOMATICA → { APROBADO | RECHAZADO | REVISION_MANUAL }
                                           │              └─▶ { APROBADO | RECHAZADO }
                                           └─▶ VENCIDO → revalidación
```

- `REVISION_MANUAL` es un estado **de primera clase**, con cola, dueño y tiempo objetivo. El
  proveedor automático se equivoca, y una persona real queda esperando.
- `RECHAZADO` guarda **motivo en catálogo cerrado** y admite reintento con condiciones. Un
  rechazo sin motivo utilizable deja al usuario sin saber qué hacer y al soporte sin respuesta.
- `VENCIDO`: el KYC caduca (documento vencido, política de revalidación). Vencer **baja el
  nivel** y bloquea lo que ese nivel habilitaba, con aviso previo, no de golpe el día del cobro.
- Todas las transiciones con actor, motivo y timestamp, append-only
  (`audit-trail-history`).

## 5. Duplicados y multicuenta

- Señales: mismo documento normalizado, mismo teléfono, mismo dispositivo
  (`dispositivo`), misma cuenta bancaria de destino en beneficiarios distintos.
- La coincidencia **abre una revisión**, no un bloqueo automático: hay casos legítimos
  (familiares que comparten dispositivo, cuenta de un tercero autorizado).
- Bloquear a alguien por un falso positivo de duplicado es tan caro como dejar pasar a un
  duplicado real: **las dos direcciones tienen costo**, y la decisión la toma una persona con el
  expediente delante.

## 6. Mínimo necesario

- Pedí **solo** lo que el nivel requiere. Pedir dirección y referencias personales a alguien que
  solo quiere explorar aumenta el abandono y la superficie de datos.
- `referencia_personal` (contacto de respaldo) tiene finalidad declarada: se usa **solo** para lo
  que el consentimiento dice (`consent-management`). **No** es una lista para cobrar
  (`collections-delinquency` §7).
- La verificación se pide **cuando hace falta**, no toda al registrarse: el usuario entiende por
  qué le piden el documento cuando está por tomar un cupo.

## 7. Soporte y accesibilidad

- El flujo tiene que funcionar con documentos gastados, cámaras malas y conexiones lentas: es
  Bolivia, no un laboratorio. Reintentos, ayuda contextual y una vía de revisión manual.
- Mensajes claros de qué salió mal y qué hacer (`ux-writing-microcopy`), sin exponer los
  criterios del antifraude.
- Tiempo objetivo de resolución publicado y medido. Un KYC que tarda tres días sin explicación
  pierde al usuario justo cuando decidió entrar.

## Anti-patrones

- Un booleano `verificado` sin niveles.
- Nivel verificado solo en el frontend.
- Documento sin normalizar ni unicidad.
- Guardar la decisión del proveedor sin la evidencia ni el puntaje.
- Imágenes de documentos sin cifrar, o accesibles por URL adivinable.
- Mensajes que revelan si un teléfono ya está registrado.
- Rechazo sin motivo utilizable ni camino de reintento.
- KYC vencido que bloquea el día del cobro sin aviso previo.
- Bloqueo automático por sospecha de duplicado.
- Usar las referencias personales para cobrar.
- Pedir todos los datos al registrarse.

## Checklist

- [ ] Niveles definidos y atados a acciones concretas, verificados en el servidor.
- [ ] Tipos de documento en catálogo; documento normalizado y único.
- [ ] Evidencia y puntaje del proveedor guardados, no solo la decisión.
- [ ] Imágenes y biometría cifradas, con acceso auditado y retención declarada.
- [ ] Tokens con hash y pepper, política por propósito e intentos registrados.
- [ ] Sin enumeración de usuarios en ningún mensaje.
- [ ] `REVISION_MANUAL` con cola, dueño y tiempo objetivo.
- [ ] Motivos de rechazo en catálogo cerrado, con camino de reintento.
- [ ] Vencimiento con aviso previo y baja de nivel controlada.
- [ ] Duplicados abren revisión, no bloqueo automático.
- [ ] Datos pedidos al mínimo y en el momento en que hacen falta.

## Evidencia / DoD

1. Salida del intento de tomar un cupo con nivel insuficiente: rechazado en el servidor.
2. Salida de alta con documento duplicado: rechazada o derivada a revisión, con el motivo.
3. Registro del expediente KYC con sus transiciones, actor y evidencia.
4. Prueba de que las imágenes no son accesibles sin autorización.
5. Prueba de no enumeración en el flujo de verificación de teléfono.
