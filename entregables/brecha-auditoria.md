# Brecha de auditoría: `POST /extraccion/accesos` no existe

## H4.S1.M1 — Qué exige el backend, y qué no existe

**Amenaza:** el frontend hoy afirma implícitamente (comentario "no garantiza" en
`registro-de-acceso.interceptor.ts`, versión anterior) que un acceso a un expediente con
datos de personas quedó auditado. Si el backend no tiene dónde recibir ese registro, la
auditoría de lectura simplemente no existe, y el comentario en el código era una promesa
falsa.

**Verificado contra el contrato real**, no supuesto (regla 00 — evidencia mínima §7):

- Servicio dueño de auditoría: `servicios/auditoria` (Java/Spring Boot, contract-first).
- Su contrato real: `servicios/auditoria/src/main/resources/openapi/auditoria.yaml`.
- Endpoints que ese contrato define hoy (`grep -n "^  /" auditoria.yaml`):
  - `/auditoria/derechos` (CU-07, ejercicio de derechos del titular)
  - `/reportes/{definicionId}/ejecuciones`
  - `/reportes/exportaciones/{exportacionId}/descargas`
  - `/indicadores`
- Los controladores reales del servicio (`web/DerechosController.java`,
  `IndicadoresController.java`, `ReportesController.java`) implementan una interfaz
  generada (`AuditoriaApi`) desde ese mismo YAML — no hay una cuarta ruta escondida.
- `grep -rn "extraccion" servicios packages` → 0 resultados en todo el monorepo.

**Conclusión: `POST /extraccion/accesos` NO existe en ningún contrato real.** No es una
hipótesis (regla 00 §1.2 prohíbe convertir probabilidad en hecho) — se buscó y no está.

**Control esperado del lado servidor** (para cuando el endpoint exista): un evento
`AccesoADatoSensible` con `actorId`, `recurso`, `id` del recurso, `correlacionId` y
`timestamp`, persistido en una bitácora de LECTURA separada de la de escritura (regla
90.2.7, `audit-trail-history`) — el servicio dueño natural es `servicios/auditoria`, junto
a `/auditoria/derechos`.

## H4.S1.M2 — `rutaId` y datos de personas en la URL

`grep -rn "ACCESO_A_DATOS" apps/backoffice/src --include=*.ts` (fuera de
`nucleo/registro-de-acceso.interceptor.ts`, que solo lo declara) → **0 resultados**:
ninguna pantalla del backoffice usa hoy `ACCESO_A_DATOS` para marcar una lectura. No hay
tabla de pantallas que poblar todavía — es un mecanismo declarado sin consumidores. Cuando
una pantalla lo use, el `rutaId` que manda el interceptor ya sale sin `?query` (se recorta
en `registroDeAccesoInterceptor`, `rutaSinQuery`), así que un futuro filtro de búsqueda con
documento en la URL no se filtra al registro de auditoría por este camino. Sigue siendo
responsabilidad de cada pantalla no poner un documento o cuenta en el *path* mismo (regla
90.2.2) — eso esta brecha no lo puede verificar sin consumidores reales.

## H4.S1.M3 — El frontend no es fuente de auditoría

**Decisión:** el frontend deja de afirmar (en comentario o en comportamiento) que un
acceso quedó auditado. Se eliminó la palabra "garantiza" del archivo. La garantía real solo
puede vivir en el servidor (regla 90.2.7): un log de acceso que depende de que el
navegador mande un `POST` que puede no salir (pestaña cerrada, sin red, adblocker) nunca es
una garantía, es una mejor-esfuerzo.

**Alternativas consideradas:**
1. **(Elegida)** El interceptor manda el registro *best-effort* cuando el backend lo
   soporte, detrás de una bandera (`REGISTRO_DE_ACCESO_HABILITADO`, apagada hoy); si falla,
   se reporta (consola + aviso en pantalla vía `Avisos`, `packages/ui/src/toast/toast.ts` —
   servicio ya existente, reusado en vez de crear uno nuevo). Nunca bloquea la lectura.
2. **Descartada:** que el propio backend de dominio (el servicio que sirve el expediente)
   registre el acceso en la misma transacción de lectura, sin depender del cliente. Es la
   solución correcta a mediano plazo, pero es un cambio de otro servicio, fuera del
   alcance de este carril (frontend puro) — se registra como recomendación, no se
   implementa acá.

Qué garantía se elimina: que "toda lectura de un expediente queda auditada" — hoy NINGUNA
lectura queda auditada del lado del backoffice, con o sin el interceptor, porque el
contrato no existe. La bandera apagada lo hace explícito en vez de fingir que sí.
