---
name: service-to-service-security
description: Seguridad entre servicios con confianza cero — por qué la red interna no es un límite de confianza, autenticación de servicio (mTLS o token de servicio con alcance acotado), propagación de la identidad del usuario sin cabeceras creídas a ciegas, autorización resuelta en cada servicio, principio de menor privilegio en credenciales de base y de broker, rotación de secretos, y verificación de webhooks entrantes de la pasarela de pagos. Usar al conectar dos servicios, al exponer un endpoint interno, al recibir un webhook externo, al configurar credenciales de base o de cola, y al revisar si alguien confía en que "el gateway ya validó".
allowed-tools: Read Grep Glob Bash
effort: high
---

# Seguridad entre servicios

"Está en la red interna" dejó de ser un control el día que un contenedor comprometido, una
dependencia maliciosa o un bucket mal configurado pueden hablarle a tus servicios. En Pasanaku,
donde un endpoint interno puede **ordenar un desembolso**, la red no es un límite de confianza.

La regla dura está en **98.6**.

## 1. Dos identidades distintas, siempre

| Identidad | Qué responde | Cómo viaja |
|---|---|---|
| **Del servicio** | ¿Qué proceso me está llamando? | mTLS, o token de servicio firmado y de vida corta |
| **Del usuario** | ¿En nombre de quién actúa? | Token del usuario propagado, o token de intercambio con el sujeto adentro |

Confundirlas es el error de diseño más común: un servicio que se autentica correctamente **no
tiene por eso permiso sobre el recurso de un usuario**. `entregas` puede llamar a `garantia`, y
aun así el desembolso del cupo 44 requiere que el actor tenga derecho sobre ese cupo.

```http
# ✅
Authorization: Bearer <token del usuario, verificable>
X-Service-Token: <token del servicio llamante, alcance: garantia:leer-deuda>

# ❌ identidad inyectada y creída: cualquiera que alcance el puerto es cualquier usuario
X-User-Id: usr_91
```

## 2. Autenticación de servicio

Dos opciones válidas; elegí una y aplicala en todos:

- **mTLS**: cada servicio con su certificado, emitido por una CA interna, con rotación
  automatizada. Autentica el canal, no la operación.
- **Token de servicio**: JWT de vida corta emitido por el proveedor de identidad, con `aud` del
  servicio destino y **alcance acotado a lo que necesita** (`pagos:emitir-orden`, no `pagos:*`).

En los dos casos:

1. El destino **valida**: firma, emisor, audiencia, expiración y alcance. Validar solo la firma
   deja pasar un token emitido para otro servicio.
2. Vida corta (minutos), rotación automática, sin secretos largos en variables de entorno que
   nadie revisa (`environment-secrets-config`).
3. Un servicio comprometido no puede usar su token para hacer todo: el alcance limita el daño.

## 3. La autorización no se delega

Cada servicio, en cada endpoint que lee o muta algo de alguien:

1. Resuelve **quién es el actor** (del token propagado, verificado por él mismo).
2. Resuelve **si puede** sobre **ese recurso concreto**: rol + propiedad + relación
   (`authz-access-control`).
3. Devuelve respuesta mínima: solo los campos que ese actor necesita (regla 90.1.8).

> [!important] Un servicio que confía en que el gateway ya autorizó tiene una vulnerabilidad de
> autorización, aunque el gateway esté bien configurado. La configuración del borde cambia; la
> del servicio es la que protege el dato.

Test obligatorio: **llamada directa al servicio interno, salteándose el gateway**, con token de
otro usuario. Tiene que dar 403 (`api-pentest`, `api-testing`).

## 4. Menor privilegio en todo lo que se conecta

| Credencial | Regla |
|---|---|
| Base de datos | Usuario propio por servicio, solo sobre su esquema. Sin `SELECT` sobre esquemas ajenos (regla 98.1.1). `UPDATE`/`DELETE` **revocados** en tablas append-only (regla 91.2.5) |
| Broker | Permiso de publicar solo en sus tópicos y consumir solo sus colas |
| Almacenamiento de archivos | Prefijo propio, sin listar el bucket entero |
| Proveedor de pagos | Credenciales de producción solo en producción; nunca en la máquina de nadie |

Verificable con una consulta, no con confianza: la salida de los permisos efectivos va en el
reporte del cambio que los toca.

## 5. Superficie interna mínima

- Un servicio expone hacia adentro **solo lo que otro necesita**, no su CRUD completo.
- Endpoints administrativos y de depuración: **nunca** accesibles desde el borde, autenticados
  igual, y auditados.
- `/health` y `/metrics` no devuelven configuración, versiones de dependencias ni nombres de host
  internos a quien no corresponde.
- Nada de "endpoint sin auth porque es interno". Si existe, alguien lo va a llamar desde afuera.

## 6. Webhooks entrantes: el borde más peligroso

El webhook de la pasarela **acredita dinero**. Es el endpoint más atacado del sistema.

1. **Firma verificada, siempre**, con la clave del proveedor y comparación de tiempo constante.
   Sin firma válida: `401`, sin procesar nada, sin filtrar en el mensaje por qué falló.
2. **Ventana de tiempo** sobre el timestamp firmado, para cortar reenvíos viejos (replay).
3. **Idempotencia por el id del proveedor**: el mismo evento dos veces acredita una vez
   (regla 91.1.4).
4. **No confíes en el cuerpo**: confirmá el estado consultando la API del proveedor antes de
   acreditar, cuando el monto o el estado lo justifiquen.
5. Lista blanca de IPs si el proveedor la publica, **como control adicional**, nunca como el
   único.
6. Responder rápido y procesar asíncrono (outbox/cola): un webhook que tarda hace que el
   proveedor reintente y multiplique el trabajo.
7. El cuerpo del webhook **no se loguea entero**: lleva datos financieros (regla 90.2).

## 7. Rotación y compromiso

- Rotación programada de certificados, tokens de servicio y credenciales de base, **automatizada
  y ensayada**. Una rotación que nadie probó falla el día que hay un incidente.
- Un secreto expuesto se **rota**, no se borra del historial y se da por resuelto
  (regla 90.3.5).
- Procedimiento escrito para revocar el acceso de un servicio comprometido sin tirar el resto
  (`incident-response-postmortem`).

## Anti-patrones

- "Está en la red interna, no hace falta autenticar".
- `X-User-Id` inyectado por el gateway y creído por el servicio.
- Un solo token compartido por todos los servicios, sin alcance y sin vencimiento.
- Validar la firma del token pero no la audiencia ni el alcance.
- Un único usuario de base para todos los servicios.
- Endpoint interno sin autenticación "porque no está publicado".
- Webhook sin verificación de firma en desarrollo "para probar más rápido".
- Loguear el cuerpo completo del webhook.
- Secretos en la imagen, en los argumentos de build o en el compose commiteado.

## Checklist

- [ ] Toda llamada entre servicios está autenticada (mTLS o token de servicio con alcance).
- [ ] El destino valida firma, emisor, audiencia, expiración y alcance.
- [ ] La identidad del usuario viaja verificable; ninguna cabecera de identidad se cree a ciegas.
- [ ] Cada servicio autoriza por su cuenta sobre el recurso concreto.
- [ ] Probada la llamada directa al servicio interno salteando el gateway: 403.
- [ ] Credenciales de base, broker y storage con menor privilegio, verificadas por consulta.
- [ ] Superficie interna mínima; nada de CRUD ajeno expuesto.
- [ ] Webhooks: firma, ventana temporal, idempotencia y procesamiento asíncrono.
- [ ] Rotación de secretos automatizada y ensayada.

## Evidencia / DoD

1. Salida de la llamada directa al endpoint interno sin token y con token ajeno: 401 / 403.
2. Salida de la consulta de permisos de base por servicio.
3. Salida del webhook con firma inválida (rechazado) y con firma válida repetida (efecto único).
4. Registro de la rotación ensayada, con el tiempo que tomó.
5. Amenaza considerada, control agregado y riesgo residual (regla 90.6).
