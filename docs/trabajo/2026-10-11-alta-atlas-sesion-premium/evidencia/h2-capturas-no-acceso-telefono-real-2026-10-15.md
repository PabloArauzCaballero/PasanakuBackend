# H2 — Revisión de capturas «No tenés acceso a esto»

Fecha de revisión: 2026-10-15. Fuente: las seis imágenes que el usuario dejó en `evidencia/telefono-real-2026-10-15/`. Se abrieron y revisaron visualmente en esta sesión. Las imágenes se conservan en su carpeta original; este informe no las copia.

## Observación por captura

| Archivo | Pantalla/acción visible | Observación directa | Código de seguimiento visible |
|---|---|---|---|
| `WhatsApp Image 2026-10-15 at 00.27.07.jpeg` | Movimientos → Extracto | Error «No tenés acceso a esto» y botón «Volver a intentar». | `68ed99f3-738a-e2bb-f408-d11d24b6de5b` |
| `WhatsApp Image 2026-10-15 at 00.27.07 (1).jpeg` | Inicio → Tu billetera | Mismo texto y mismo código que Extracto. | `68ed99f3-738a-e2bb-f408-d11d24b6de5b` |
| `WhatsApp Image 2026-10-15 at 00.27.07 (2).jpeg` | Formulario «Organizar un grupo» | Formulario con datos ingresados; aviso «No tenés acceso a esto» aparece antes de tocar «Crear grupo». | `e7523034-2172-81d9-7d90-d591825ab3c0` |
| `WhatsApp Image 2026-10-15 at 00.27.08.jpeg` | Inicio → Tu billetera, reintento posterior | El mismo error visible con otro código. | `ea568f92-2db4-a50c-a323-f33855a09b5c` |
| `WhatsApp Image 2026-10-15 at 00.27.08 (1).jpeg` | Tutorial, paso 2 de 3 | La guía dice que el elemento de esta parte aún no está disponible y ofrece reintentar o salir. | No aparece un código legible en esta captura. |
| `WhatsApp Image 2026-10-15 at 00.27.08 (2).jpeg` | Tutorial, paso 1 de 3, sobre Inicio | Se ve el error de billetera detrás de la guía. | `b753daee-b52a-e4ae-db83-53cdd8a9dfac` |

## Qué permite concluir la evidencia

- La app presenta al menos dos superficies de billetera inaccesibles (Inicio y Extracto) y el intento de crear grupo también muestra falta de acceso. Hay cuatro códigos de seguimiento distintos visibles en el conjunto.
- La imagen no muestra el status HTTP, el cuerpo JSON ni una respuesta de servidor; por eso no identifica por sí sola el permiso denegado ni demuestra una caída del backend. El mapeador móvil traduce HTTP 403 al texto exacto «No tenés acceso a esto» (`apps/movil/lib/dominio/errores.dart:95`); la asignación concreta de 403 a cada código debe confirmarse con red/logs correlacionados, que no están disponibles desde este equipo.
- La causa de billetera hallada en el código es consistente con el síntoma: los endpoints exigen `BILLETERA_VER`, y el rol `PARTICIPANTE` otorga ese permiso (`sql/60_semillas/10-roles-y-permisos.sql`). La aprobación manual ahora asigna `PARTICIPANTE` globalmente para aprobaciones nuevas; ese cambio pasó el PR #70 y se integró en `test`. No se verificó el rol de la cuenta de las capturas ni se hizo una lectura autenticada tras el despliegue. Cuentas aprobadas antes de ese cambio no reciben la asignación automáticamente.
- Crear grupos es una autorización separada: `GruposController.crearGrupo` exige `GRUPO_CREAR`, que el catálogo concede al rol `ORGANIZADOR`. No se debe quitar ese control ni conceder `ORGANIZADOR` como parte de la aprobación de identidad.
- El tutorial muestra como no disponible un objetivo y, en otra imagen, queda superpuesto a la superficie con error de billetera. Es compatible con un destino que no llegó a cargarse, pero las capturas no prueban la dependencia causal ni una falla propia del tutorial.

## Sonda pública de conectividad (2026-10-15)

Comandos ejecutados después de revisar las imágenes:

```text
curl.exe -sS -m 12 -i https://api.aportaya.161.97.85.216.sslip.io/actuator/health
HTTP/1.1 200 OK
{"status":"UP","groups":["liveness","readiness"]}

curl.exe -sS -m 12 -o NUL -w 'GET /api/v1/sesiones sin token: %{http_code}\n' https://api.aportaya.161.97.85.216.sslip.io/api/v1/sesiones
GET /api/v1/sesiones sin token: 401
```

Demuestra que el host responde y que la ruta de sesión rechaza una petición sin token como corresponde; no identifica el SHA de la imagen servida ni reproduce las llamadas autenticadas de las capturas.

## Veredicto y pendiente

```text
VEREDICTO:   BLOCKED (reproducción parcial; síntoma observado, causa HTTP por código de seguimiento sin confirmar)
Peldaño:     DISCOVERED
Superficie:  Mobile — capturas del teléfono aportadas por el usuario
Comando:     inspección visual de las seis imágenes listadas arriba
Salida:      cuatro códigos de seguimiento; errores de billetera, creación de grupo y objetivo de tutorial no disponible
Demuestra:   qué pantallas muestran el mensaje y qué códigos se pueden buscar en logs
No cubierto: status/cuerpo HTTP por código, logs del backend, rol real de la cuenta, prueba de billetera tras renovar sesión, ni reproducción Flutter/backend en AVD
```

Siguiente paso seguro para cerrar H2.S1.M1: disponer de logs de TEST correlacionables por esos códigos y de una cuenta de ensayo autorizada en AVD; confirmar la asignación `PARTICIPANTE`, iniciar sesión de nuevo y observar Inicio/Extracto; después probar la creación únicamente con una cuenta con rol `ORGANIZADOR`. No se incluyen aquí datos personales ni se afirma que la cuenta existente haya quedado corregida.
