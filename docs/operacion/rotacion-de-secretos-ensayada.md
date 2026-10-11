# Runbook — Rotación de secretos, ensayada

Complementa [[secret-rotation]] (el procedimiento general) con lo que ese documento no tenía:
**el inventario completo de lo que el despliegue inyecta, qué clase de rotación admite cada secreto,
y la mecánica ensayada** con material sintético (`despliegue/ensayos/rotacion/ensayo_rotacion.py`).
H13.S1.M3 · E-H13-03.

Reglas que mandan: 90.3 (un secreto expuesto se **rota**, no se borra del historial; la configuración se
valida al arrancar) y 90.2.9 (nunca el valor plano de un token o credencial en un log).

## Síntomas

- Un secreto apareció en un log, una captura, un repositorio o un ticket (regla 90.2/90.3).
- Salió del equipo alguien con acceso al almacén de secretos de Coolify.
- Rotación programada: **la cadencia es decisión del responsable de seguridad** (hoy sin fecha fija).

## Inventario

Cada fila es una variable que `scripts/generar_compose.py` inyecta en el compose desplegado. La prueba
`RotacionDeSecretosContratoTest` falla si aparece un secreto nuevo sin fila acá.

| Secreto | Quién lo usa | Clase | Soporte hoy en el código | Costo de rotarlo |
|---|---|---|---|---|
| `JWT_CLAVE_FIRMA` | `identidad` firma; los otros 14 validan contra su JWKS | Asimétrica con JWKS | **Ventana de dos claves**: el JWKS publica la vigente y, si está, `JWT_CLAVE_FIRMA_ANTERIOR` (solo se publica, nunca firma). Probado en `RotacionDeClavesDeFirmaTest` | Con la ventana: ninguna sesión se invalida (el token vive 15 min). Sin ella (poner solo la nueva): logout global (ensayo: 200 de 200) |
| `JWT_CLAVE_FIRMA_ANTERIOR` | `identidad` (solo la publica en el JWKS) | Asimétrica con JWKS | Opcional, vacía fuera de una rotación; no puede traer el mismo `kid` que la vigente ni usarse sin vigente (el servicio no arranca) | Ninguno: es la mitad de la ventana. Se vacía pasados 15 minutos de la rotación |
| `INVITACIONES_CLAVE` | `identidad` (HMAC de invitaciones) | HMAC | Una variable, una clave; **default vacío** en `application.yml:39` (se valida al primer uso, no al arrancar) | Invitaciones emitidas y no consumidas dejan de valer |
| `QR_INTERNO_CLAVE` | `nucleo-financiero` (QR internos, ≥ 32 bytes) | HMAC | Una clave; falla al arrancar si falta | QR vigentes dejan de valer; se reemiten |
| `WEBHOOK_SECRETO` | `notificaciones` (firma de acuses del proveedor) | HMAC | Una clave; falla al arrancar si falta | Acuses en vuelo firmados con la vieja se rechazan hasta coordinar con el proveedor |
| `CERTIFICADOS_CLAVE_FIRMA` | `transparencia` (certificados verificables) | Firma | Una clave | Los certificados ya emitidos **no se re-firman**: hay que conservar la clave pública vieja para verificarlos |
| `SEGURIDAD_PIMIENTA` | `identidad`, `entregas` (hash con pimienta de tokens y códigos) | Pimienta | Una clave | No se puede re-hashear sin el valor: migración perezosa (ver abajo); sin ella invalida OTP y tokens en vuelo |
| `BD_CLAVE` | los 15 servicios, vía PgBouncer | Credencial de base | Gestor de la base | Reinicio coordinado; depende del proveedor gestionado (`DECISION_REQUIRED`) |
| `ARCHIVOS_CLAVE` | `identidad` y otros que suben a MinIO | Credencial de almacenamiento | Gestor de MinIO | Reinicio del cliente; rotar usuario y clave en MinIO primero |
| `GMAIL_CLIENT_SECRET` | `identidad` (correo de verificación) | Credencial externa | Default vacío en `application.yml:61` | La rota el proveedor; el servicio sigue sin enviar correo hasta cargar la nueva |
| `GMAIL_REFRESH_TOKEN` | `identidad` | Credencial externa | Default vacío en `application.yml:62` | Idem; requiere reautorizar la cuenta de envío |
| `INVERSIONES_ALIADO_API_KEY`, `INVERSIONES_ALIADO_FIRMA_SECRETO`, `INVERSIONES_LIBRO_CREDENCIAL_SERVICIO` | `inversiones` | Credencial externa / de servicio | Vacías: el aliado está **deshabilitado** (DR-INV-01) | No aplica mientras no haya aliado contratado |
| `POSTGRES_PASSWORD`, `MINIO_ROOT_PASSWORD` | infraestructura | Infra | Valores de desarrollo en `despliegue/compose/base.yml`; en producción los pone el proveedor | Fuera del repo |

## Diagnóstico previo

1. **¿Qué tan expuesto estuvo?** Un log interno no es un commit público: determina si se rota ya o en la
   ventana.
2. **¿Cuál es el costo de la clase?** La columna de arriba. `JWT_CLAVE_FIRMA` en hora pico desloguea a
   todos a la vez: coordinar.
3. **¿Está el valor viejo en algún lugar vivo?** Variables de entorno de un contenedor que sigue corriendo,
   un `.env` local, un volcado de configuración.

## Acciones seguras

1. **Generar el valor nuevo fuera del repositorio** (`openssl rand -base64 48`; para RSA, un par nuevo con `kid` nuevo).
   Nunca un ejemplo commiteado.
2. **Cargarlo en el almacén de secretos del entorno** (Coolify). El compose desplegado solo trae `${VARIABLE}`:
   rotar es cambiar la variable, no editar el repositorio (el ensayo lo comprueba: ningún valor de desarrollo
   está en `docker-compose.coolify.yml`).
3. **Reiniciar por clase**:
   - *Con ventana* (JWT, HMAC, pimienta): publicar la nueva **junto a** la vieja, firmar solo con la nueva,
     esperar a que venza lo emitido con la vieja (el TTL más largo), retirar la vieja. Para el JWT: cargar la
     clave vigente actual en `JWT_CLAVE_FIRMA_ANTERIOR`, la nueva (con `kid` distinto) en `JWT_CLAVE_FIRMA`,
     reiniciar `identidad`; a los 15 minutos vaciar `JWT_CLAVE_FIRMA_ANTERIOR` y reiniciar.
   - *Sin ventana* (HMAC de invitaciones, QR interno, webhook: una sola clave hoy): reiniciar de golpe y aceptar
     el costo de la tabla.
4. **Confirmar que el valor viejo no está en ningún lugar vivo** (punto 3 del diagnóstico).
5. Si hubo exposición: **no volver al valor expuesto** aunque la rotación falle (90.3.5).

## Recuperación

- Si el servicio no arranca con el secreto nuevo (configuración inválida): corregir el valor **nuevo**, no
  restaurar el expuesto. El servicio debe fallar al arrancar si falta un secreto (90.3.4); los que tienen
  default vacío (tabla) son una brecha: arrancan sin él y fallan recién al usarlo.
- Pimienta: si se retiró antes de tiempo, los usuarios no migrados deben pasar por el flujo de recuperación.

## Validación

```bash
python despliegue/ensayos/rotacion/ensayo_rotacion.py     # ensaya la mecánica con material sintético
docker logs --since 2m aportaya-<servicio> | grep -i "Started Aplicacion"    # el servicio arrancó con el valor nuevo
```

### Lo que el ensayo demostró (salida literal en `evidencia/carril-S2/rotacion-ensayo.log`)

- **JWT, una clave**: rotar invalida 200 de 200 sesiones del ensayo. **Con ventana de solapamiento**: 0
  invalidadas, la clave nueva valida desde el primer segundo, y al retirar la vieja lo firmado con ella deja
  de valer (la clave filtrada no sirve de nada).
- **HMAC**: con la ventana `[nueva, vieja]` todo lo emitido con la vieja sigue válido y lo nuevo se firma solo
  con la nueva; retirada la vieja, una firma forjada con la clave filtrada no pasa.
- **Pimienta**: la huella guardada no se convierte a la pimienta nueva sin el valor original; con migración
  perezosa se sabe cuántos faltan (la vieja no se retira hasta que sean cero).
- **Higiene**: ningún valor de desarrollo aparece en el compose desplegado.

## Escalamiento

- Exposición confirmada de `JWT_CLAVE_FIRMA`, `SEGURIDAD_PIMIENTA` o una credencial de base en un medio
  público: incidente de seguridad (RESPONSABLE_SEGURIDAD), no rotación de rutina.
- **Decisiones pendientes** (de Seguridad y de quien opere la infraestructura): cadencia de rotación;
  si se invierte en solapamiento de claves en `identidad` (JWKS con dos claves) y listas de claves HMAC;
  mecanismo de rotación de la base en producción.

## Hallazgos que salieron de armar esto (dueños en la evidencia del carril)

1. ~~`JWKS` de `identidad` publica una sola clave~~ **Resuelto**: ventana de dos claves
   (`JWT_CLAVE_FIRMA_ANTERIOR`), con prueba `RotacionDeClavesDeFirmaTest`.
2. `INVITACIONES_CLAVE`, `JWT_CLAVE_FIRMA` y las credenciales externas tienen **default vacío** en
   `application.yml`: el servicio arranca sin el secreto (90.3.4). El barrido `SecretosSinDefaultBarridoTest`
   fija la lista actual y falla ante cualquier secreto nuevo con default.
3. El inventario de [[secret-rotation]] listaba 4 secretos; el despliegue inyecta 10 más.

## Ver también

[[secret-rotation]] · [[branch-protection]] · [[postgres-down]]
