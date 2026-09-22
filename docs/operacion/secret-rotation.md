# Runbook — Rotación de secretos

## Síntomas (por qué se rota)

- Sospecha o confirmación de exposición: un secreto apareció en un log, un
  repositorio público, o una captura pegada donde no debía (regla 90.2/90.3
  — esto NO se resuelve borrando del historial, se rota).
- Rotación programada (política del equipo, sin fecha fija hoy —
  `DECISION_REQUIRED`).
- Cambio de responsable con acceso a un secreto que se va del equipo.

## Inventario — qué secretos existen hoy

| Secreto | Variable | Quién lo usa | Efecto de rotarlo |
|---|---|---|---|
| Clave de firma JWT | `JWT_CLAVE_FIRMA` | Solo `identidad` (ADR-024: un único emisor, los otros trece validan contra su JWKS, no firman) | Toda sesión activa firmada con la clave vieja deja de validar — logout forzado global |
| Pimienta de hashing | `SEGURIDAD_PIMIENTA` | Todos los que guardan `token_verificacion` (regla 90.2.9: hash con *pepper*, nunca el valor plano) | Los hashes existentes, calculados con la pimienta vieja, dejan de verificar — invalida OTP/tokens de un solo uso en vuelo |
| Credenciales de Postgres | `POSTGRES_PASSWORD` (dev: `pasanaku`, hardcodeado — `despliegue/compose/base.yml`) | Todos los servicios, vía PgBouncer | En dev no aplica (es un valor de desarrollo, no un secreto real); en producción, la rotación depende del proveedor gestionado — no hay un mecanismo propio de este repo todavía |
| Credenciales de MinIO | `MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD` (dev: `aportaya`/`aportaya-local`) | El servidor de archivos | Mismo caso: valor de desarrollo, sin mecanismo de rotación propio para producción |

## Diagnóstico previo

1. **¿Qué tan expuesto estuvo el secreto?** Determina la urgencia — un
   secreto en un log interno no es lo mismo que uno en un commit público.
2. **¿Hay sesiones/tokens activos que dependan de él ahora mismo?** Rotar
   `JWT_CLAVE_FIRMA` en horario pico fuerza logout de todo el mundo
   simultáneamente — coordinar la ventana.

## Acciones seguras — el orden importa

1. **Generar el valor nuevo** fuera de este repo (nunca un valor de ejemplo
   commiteado — regla 90.3.1).
2. **Cargarlo en el gestor de secretos del entorno** (Coolify u otro — el
   mecanismo real de producción todavía no está documentado en este repo,
   `DECISION_REQUIRED` para quien lo opere).
3. **Reiniciar los servicios que lo consumen**, uno por uno si el secreto lo
   permite (`JWT_CLAVE_FIRMA` es de `identidad` solamente: un solo reinicio,
   sin ventana de doble clave porque hay un único emisor).
4. **Confirmar que el valor viejo ya no está en ningún lugar vivo**: ni en
   variables de entorno de un contenedor que quedó corriendo, ni en un
   `.env` local.

## Recuperación (si la rotación salió mal)

- Si el nuevo secreto no arranca el servicio (config inválida): volver al
  valor anterior temporalmente, diagnosticar en un entorno de prueba, no en
  producción.
- Un secreto rotado **nunca se revierte por comodidad** si ya hubo sospecha
  de exposición (regla 90.3.5: se rota, no se vuelve al valor expuesto).

## Validación

```bash
# El servicio arranca con el secreto nuevo
docker logs --since 2m aportaya-<servicio> | grep -i "Started Aplicacion"

# Falla explícita si el secreto FALTA (regla 90.3.4: sin default silencioso)
# — probar en un entorno de prueba, nunca en produccion:
docker run --rm -e SPRING_PROFILES_ACTIVE=production <imagen-del-servicio>
# tiene que rechazar el arranque, no levantar con un valor vacío
```

## Escalamiento

- Exposición confirmada de `JWT_CLAVE_FIRMA` o `SEGURIDAD_PIMIENTA` en un
  medio público: es un incidente de seguridad, no una rotación de rutina —
  escalar de inmediato, no esperar la ventana programada.
- Rotación de credenciales de base de datos en producción: depende del
  mecanismo del proveedor gestionado, que este repo todavía no documenta —
  escalar a quien opere esa infraestructura (`DECISION_REQUIRED`).

## Ver también

[[branch-protection]] · [[postgres-down]]
