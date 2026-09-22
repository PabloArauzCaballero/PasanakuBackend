# 65 — Aislar y simular para no bloquearse

Fija qué hacer cuando el siguiente paso depende de un colaborador que todavía no entregó lo suyo:
una persona que no corrió su prueba, un servicio caído, un repositorio inaccesible, una entidad
rota que no es tuya arreglar. **"No tengo el insumo" no es una razón para detenerse.** Es una razón
para aislar esa dependencia detrás de su contrato real y seguir trabajando contra un doble
declarado, en tres niveles: correcto, límite, e inválido.

> ## ⛔ Esto es OBLIGATORIO, no una alternativa disponible
>
> **Ante cualquier bloqueo, simular los tres niveles del contrato no es una opción: es lo que hay
> que hacer.** No existe "quedó `BLOQUEADO`" ni "quedó `DESCARTADO` porque dependía de otro" como
> cierre válido de una microtarea mientras el contrato de lo que falta se pueda escribir. Si se
> puede nombrar el contrato, se puede simular; y si se puede simular, **se termina el trabajo**.
>
> El objetivo del turno es **cerrarlo entero y de forma independiente**. Un turno que entrega la
> mitad porque esperaba a alguien no entregó la mitad: entregó un problema de coordinación
> disfrazado de avance.
>
> Lo único que sigue siendo legítimo dejar sin cerrar es una **decisión de negocio** (qué debe
> hacer el sistema) o una **acción destructiva sobre algo compartido**. Todo lo demás —que el
> servicio de otro no exista, que su prueba no haya corrido, que su módulo esté roto— **se simula
> y se cierra**, declarando que se cerró contra un doble.

Esta regla es la continuación operativa de la regla 60: ahí se nombra la racionalización
("esto no es mío, lo espero"); acá se fija qué se hace en su lugar.

## 1. Cuándo aplica

Cuando el trabajo está genuinamente bloqueado por algo que **no podés resolver vos mismo ahora**:

- Una persona tiene que correr su propia prueba, entregar su propio análisis, o tomar una decisión
  de negocio, y todavía no lo hizo.
- Un servicio, repositorio o API externa no responde, y no es tuyo arreglarlo.
- Una pieza de código ajena a tu alcance está rota (un bug real, en un módulo que no es el tuyo) y
  arreglarla de raíz excede el cambio mínimo de tu tarea actual.

**No aplica** cuando el bloqueo es tuyo y se puede destrabar de verdad con el esfuerzo del momento
(un servicio que hay que prender, una dependencia que hay que instalar, un comando mal armado). Ahí
rige la regla 60: se intenta destrabar antes de declarar `BLOQUEADO`. Esta regla es para lo que
queda **después** de haber intentado eso y seguir sin el insumo.

## 2. Qué se hace: aislar detrás del contrato, simular en tres niveles

1. **Identificá el contrato real** de lo que falta: la interfaz, el endpoint, el formato de dato,
   la firma de la función — lo que tu código consume, no cómo está implementado del otro lado.
2. **Construí un doble** (fake, stub o mock, según corresponda) que cumpla ese contrato, declarado
   como tal en el código y en el reporte. El doble nunca se presenta como el colaborador real.
3. **Ejercitalo en los tres niveles del contrato** — obligatorio los tres, no sólo el feliz.
   "Del contrato" significa: los casos salen de lo que la interfaz promete, no de lo que sería
   cómodo probar:
   - **Correcto**: la entrada/respuesta válida esperada — lo mínimo para poder seguir programando
     contra el contrato.
   - **Límite**: los bordes del contrato (ver `edge-case-data-catalog` y la sección de valores
     límite de `test-case-design-techniques`) — lo que separa "funciona" de "funciona sólo en el
     caso fácil".
   - **Inválido**: lo que el contrato debería rechazar o manejar como error — para no descubrir en
     producción que tu código asumía que el otro lado nunca falla.
4. **Seguí trabajando** tu parte contra el doble: escribir, verificar, iterar.
5. **Registrá explícitamente** qué quedó verificado contra el doble y qué sigue pendiente de
   verificar contra el colaborador real cuando exista. Un doble no sustituye la integración real
   (regla 80.5.10): sustituye la espera, no la verificación final.

## 3. Ejemplos aplicados a Pasanaku

En un sistema de microservicios esto pasa **todos los días**: tu servicio necesita a otro que
todavía no existe, o al que le falta el endpoint.

- **`entregas` necesita la deuda exigible y `garantia` todavía no la expone.** El contrato es la
  operación `deudaExigible(cupoId) → { monto, moneda, lineas[] }`. Se construye un stub que la
  cumple y se ejercita en los tres niveles: **correcto** (deuda con dos líneas), **límite** (deuda
  cero; deuda igual a la bolsa bruta, que deja neto cero), **inválido** (el servicio responde 500
  y tarda más que el timeout). La liquidación se termina, se verifica, y se declara que quedó
  contra un doble (`service-contracts-versioning` para el pacto).
- **El webhook de la pasarela no se puede probar contra el sandbox del proveedor.** El contrato es
  el payload firmado. Doble local: **correcto** (firma válida, monto igual al de la orden),
  **límite** (el mismo evento dos veces; evento con timestamp al borde de la ventana),
  **inválido** (firma inválida, monto distinto, orden inexistente). Esto es lo que exige
  `payments-qr-integration` §8, y no depende de nadie.
- **Falta la decisión de negocio de qué pasa si falta plata para la entrega.** Esto **sí** queda
  abierto: es una decisión de negocio (`DECISION_REQUIRED`), no un bloqueo de ejecución. Se
  registra como ambigüedad con a quién confirmársela, y se implementan las ramas que sí están
  definidas.

La diferencia entre los dos primeros y el tercero es la línea de esta regla: **si el contrato se
puede nombrar, se simula y se cierra; si lo que falta es la decisión de qué debe hacer el sistema,
se registra y se escala.**

## 4. Prohibiciones

1. **Prohibido presentar el resultado de un doble como si fuera el resultado del colaborador
   real.** Una liquidación calculada contra el stub de `garantia` no es "la entrega verificada";
   una prueba que corriste vos no es la prueba oficial de quien es dueño de ese servicio.
2. **Prohibido simular sólo el camino feliz** y declarar el mecanismo probado. Sin el nivel límite
   e inválido, lo que existe es un `WRITTEN` optimista, no una verificación (regla 30).
3. **Prohibido dejar el doble como reemplazo permanente** sin registrar qué falta verificar contra
   lo real. El doble destraba el trabajo de hoy; no cierra el hito.
4. **Prohibido cerrar una microtarea como `BLOQUEADO` o `DESCARTADO` alegando que depende de
   otro, sin haber simulado antes los tres niveles de su contrato.** Es la contracara de esta
   regla: si el contrato se puede nombrar, el bloqueo es de coordinación, no de ejecución, y la
   microtarea se cierra contra el doble. Sólo una decisión de negocio sin tomar, o una acción
   destructiva sobre algo compartido, justifican no cerrarla.
5. **Prohibido usar esta regla para justificar tocar código fuera de tu alcance** (regla 00, §3):
   aislar el problema NO es arreglarlo. Si aislarlo requiere un cambio, que sea el mínimo posible y
   declarado, nunca la corrección de fondo del módulo ajeno.

## 5. Relación con las skills

El oficio de diseñar los tres niveles está en `test-case-design-techniques` (partición de
equivalencia, valores límite, positivos y negativos) y `synthetic-test-data-generation` §4
("válidos e inválidos a propósito"). Ninguna de las dos cubre el paso previo que esta regla fija:
decidir aislar y simular en el momento en que aparece el bloqueo, en vez de detenerse a esperarlo.
Relacionadas: `rationalization-guard` (regla 60), `service-contracts-versioning` y
`microservices-testing` (el doble se construye contra el pacto, no contra lo que uno imagina),
`root-cause-debugging` (para el bug real que queda registrado, no arreglado).
