# 90 — Seguridad y datos sensibles

Marco de referencia: **OWASP Top 10:2025** y **OWASP ASVS 5.0**, adaptados al alcance real del
cambio. Para APIs, además **OWASP API Security Top 10:2023**.

Esta regla no es una lista de buenas intenciones: **cada punto es condición de corrección**.
Un cambio que la incumple está mal, no "mejorable". La frase "la seguridad no es parte de este
ticket" está explícitamente prohibida (ver regla 60).

## 90.1 Controles obligatorios en cambios sensibles

Un cambio es sensible si toca autenticación, autorización, datos de personas, dinero, archivos,
o entrada proveniente del usuario. En ese caso son obligatorios:

1. **Autenticación resuelta en el servidor.** Nunca inferida del cliente.
2. **Autorización por rol Y por propiedad del recurso.** El rol correcto sobre el recurso ajeno
   sigue siendo acceso indebido.
3. **Chequeo a nivel de objeto en cada endpoint** que lee o muta un recurso identificado
   (IDOR/BOLA). Cambiar un identificador en la URL no puede devolver datos de otro.
4. **Chequeo a nivel de función** (BFLA): que el endpoint exista no significa que cualquiera
   pueda invocarlo.
5. **Validación de toda entrada en el servidor**, con lista blanca de campos. Prohibido aceptar
   campos no declarados (mass assignment).
6. **Uploads con tipo real y tamaño validados** en servidor, almacenamiento fuera de la aplicación
   y acceso autorizado. Ver `file-uploads-media`.
7. **Prohibido confiar en campos ocultos, deshabilitados o calculados en el frontend.**
8. **Respuesta mínima**: no devolver más datos personales de los que esa vista necesita.
9. **Transiciones de estado validadas en el backend**, con la precondición en la escritura.
   Ver `state-machines-workflows`.
10. **Acciones sensibles idempotentes** cuando el cliente puede reintentar. Ver `concurrency-and-locking`.
11. **Links de acción por correo o chat con token de propósito único y vencimiento**, y validación
    de que el actor del token es el actor correcto.
12. **Consultas parametrizadas u ORM seguro.** Prohibida la concatenación de SQL con entrada de
    usuario, incluido el `ORDER BY`, que se resuelve por lista blanca.
13. **Errores sin stacktrace, sin SQL y sin secretos** hacia el cliente. Ver `error-handling-contract`.
14. **Rate limiting** en endpoints de autenticación, búsqueda, envío de mensajes y cualquier
    operación costosa.

## 90.2 Datos personales y financieros — prohibiciones duras

Pasanaku administra **dinero de terceros** y los datos con los que se identifica a quien lo pone.
La categoría especial acá son los **datos financieros y de identidad**: documento, cuenta
bancaria, QR de cobro, importes atribuibles a una persona, historial de mora. Estas prohibiciones
no admiten excepción operativa:

1. **Prohibido escribir datos personales o financieros en logs, trazas, métricas, mensajes de
   error, reportes de errores o analytics.** Se loguean identificadores, no contenidos: `cupo_id`
   sí, número de cuenta no; `pago_id` sí, payload del QR no.
2. **Prohibido pasar datos identificatorios o financieros en la URL** (path o query): número de
   documento, cuenta bancaria, importe atribuible, token de pago. Quedan en historiales, proxies
   y logs de acceso — y en microservicios, también en los logs del gateway y de cada salto.
3. **Prohibido incluir datos de participantes en el reporte de un trabajo** (regla 40). Si una
   salida los contenía, se enmascara y se declara que se enmascaró.
4. **Prohibido enviar datos reales a servicios de terceros** no acordados, incluidos túneles de
   desarrollo, servicios de captura, herramientas de depuración y modelos externos. Un QR de
   producción pegado en un chat es un incidente.
5. **Prohibido copiar datos de producción a entornos de desarrollo o prueba.** Si se necesita
   volumen o realismo, se generan datos sintéticos. Ver `synthetic-test-data-generation`.
6. **Prohibido restaurar un backup de producción en staging sin anonimizar.**
7. **Todo acceso a datos financieros de una persona deja rastro auditable**, incluidas las
   lecturas. La bitácora de lectura es separada de la de escritura. Ver `audit-trail-history`.
8. **Antes de exponer datos de una persona** en búsquedas, grupos públicos, emparejamiento o
   perfiles de reputación, se verifica el consentimiento en el servidor. Ver `consent-management`
   y `group-discovery-matching`.
9. **Nunca se persiste ni se loguea el valor plano de un token, OTP o credencial**: solo su hash
   con *pepper* (modelo M1, `token_verificacion`).
10. **Datos de tarjeta (PAN, CVV, banda) no entran al sistema.** Si algún día hay tarjeta, es
    contra un proveedor tokenizado y el alcance PCI-DSS se declara explícitamente antes de
    escribir la primera línea.
11. El detalle operativo está en `data-privacy-financial`, que es **gate obligatorio** en todo PR
    que toque datos de personas o importes, aunque nadie lo haya pedido.

## 90.3 Secretos y configuración

1. **Prohibido commitear secretos**, en cualquier forma: claves, tokens, cadenas de conexión,
   certificados, credenciales de prueba que sirvan en algún entorno real.
2. **Prohibido incluir secretos en el bundle del frontend.** Todo lo que llega al navegador es público.
3. **Prohibido dejar secretos en capas de imagen o en argumentos de build.**
4. **La configuración se valida al arrancar** y el servicio **falla si falta o es inválida**.
   Prohibido arrancar con un valor por defecto silencioso para un secreto.
5. Un secreto expuesto **se rota**, no se borra del historial y se da por resuelto.
6. Ver `environment-secrets-config` y `github-security-features`.

## 90.4 Cadena de suministro

1. **Lockfile commiteado** siempre.
2. **Toda dependencia nueva se justifica** antes de agregarse: qué resuelve, tamaño, mantenimiento,
   licencia y superficie que abre. Ver `dependency-management`.
3. **Las alertas de vulnerabilidad se triagean**, no se acumulan.
4. En CI son obligatorias las etapas de **escaneo de secretos y de dependencias**.
   Ver `code-quality-gates` y `ci-cd-pipeline`.

## 90.5 Áreas de máxima sensibilidad

Cuando el cambio toca alguna de estas, el gate de seguridad es **obligatorio y explícito**, con su
sección en el reporte:

- Aportes, órdenes de cobro, pagos QR y conciliación bancaria
- Entregas de fondo, deducciones y cuentas bancarias de beneficiario
- Fondo de garantía, coberturas, avales y ejecución de deuda
- Contabilidad de doble partida y cierre de periodo
- Sorteo de turnos y cualquier cosa que decida el orden de cobro
- Incumplimiento, sanciones, reputación y su debido proceso
- Identidad, KYC, listas de cumplimiento y reportes a la autoridad
- Autenticación, roles, permisos y tokens de propósito único
- Notificaciones que contienen enlaces de pago
- Cualquier superficie pública no autenticada
- Cualquier llamada **entre servicios** que mueva o autorice dinero (ver regla 98)

## 90.6 Evidencia obligatoria

Un cambio sensible no se cierra sin, en el reporte del trabajo:

1. **Amenaza considerada** — qué podría salir mal.
2. **Control encontrado o agregado** — dónde vive, con archivo y línea.
3. **Test que lo demuestra** — especialmente el test de autorización negativo.
4. **Resultado** — salida literal.
5. **Riesgo residual** — lo que queda sin cubrir y por qué.

## 90.7 Skills relacionadas

`security-guardrails` · `data-privacy-financial` · `authz-access-control` · `authn-identity` ·
`multi-tenancy` · `consent-management` · `kyc-identity-verification` ·
`aml-sanctions-screening` · `money-movement-safety` · `audit-trail-history` ·
`error-handling-contract` · `file-uploads-media` · `environment-secrets-config` ·
`secure-code-review` · `threat-modeling` · `security-testing` · `dependency-management` ·
`service-to-service-security`
