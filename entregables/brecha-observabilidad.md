# Brecha de observabilidad — propuesta, no contrato (Q-J2)

> **Estado al 2026-10-29.** Siete de las nueve pantallas ya leen contratos reales; solo **Despliegues** y
> **Respaldos** siguen sin fuente. El texto original de este documento queda abajo como historia.

| Pantalla | Contrato real | Fuente |
|---|---|---|
| Servicios y SLO | `GET /indicadores/operacion/servicios` (auditoria) | readiness medido cada minuto |
| Base de datos | `GET /indicadores/catalogo-de-datos/resumen` + salud (auditoria) | catálogo del modelo |
| Outbox | `GET /indicadores/operacion/outbox` (auditoria) | métricas del relevo en Prometheus |
| Incidentes | `GET /indicadores/operacion/incidentes` (auditoria) | `auditoria.incidente_operativo` |
| Webhooks | `GET /pagos/webhooks-recibidos` (aportes) + `GET /notificaciones/webhooks-recibidos` | `webhook_pasarela`, `evento_entrega_mensaje` |
| Proveedores | `GET /pagos/proveedores`, `GET /notificaciones/proveedores`, `GET /tarifas/costos-de-proveedor` | `proveedor_pago`, `proveedor_mensajeria`, `costo_proveedor_operacion` |
| Revisión de accesos | `GET /usuarios-internos` (identidad) | `usuario_interno` y roles |
| **Despliegues** | — | **ninguna**: el despliegue de TEST es manual (`despliegue/TEST.md`) y no deja registro |
| **Respaldos** | — | **ninguna**: `pg_dump` previo a cada despliegue en `/opt/aportaya/respaldos/` (`despliegue/TEST.md:505`), sin registro |

### Propuesta para Despliegues y Respaldos (DECISION_REQUIRED · Pablo)

Una sola pieza resuelve las dos: **un registro de operaciones de plataforma en `auditoria`**, que escribe el
procedimiento de despliegue y que la pantalla lee.

1. Modelo (por `scripts/modelo.py`, nunca DDL a mano): `auditoria.despliegue_registrado` (servicio, version/sha,
   desplegado_en, desplegado_por, resultado) y `auditoria.respaldo_registrado` (origen, archivo, tamano_bytes,
   tomado_en, verificado_con_pg_restore, restauracion_probada_en).
2. Quién escribe: el paso «Etapa 2 · respaldo» y el cierre del despliegue de `despliegue/TEST.md`, con un `psql`
   del rol de despliegue. Es la única parte que conoce el sha y el archivo del respaldo.
3. Contratos: `GET /indicadores/operacion/despliegues` y `GET /indicadores/operacion/respaldos` (auditoria,
   `TABLERO_SISTEMAS_VER`), iguales en forma a Incidentes.
4. Interruptores: no hay modelo de banderas en ningún servicio. La doble confirmación del lado servidor que pide
   la pantalla es un caso de uso nuevo con su debido proceso, y queda fuera de esta propuesta.

Lo que hay que decidir: si el registro lo escribe el script de despliegue (propuesta) o Coolify, y si
«restauración probada» se carga a mano o la escribe `despliegue/ensayos/restauracion/ensayo_restauracion.sh`.

---

Las nueve pantallas de `apps/backoffice/src/app/rutas/sistemas/` esperan hoy la
siguiente forma de datos. **Ninguna de estas nueve secciones es un contrato real**: no
existe un OpenAPI de "indicadores"/"tablero"/"plataforma" — se buscó en
`servicios/erp/src/main/resources/openapi/erp.yaml` y en `docs/CasosDeUso/CU-98*.md` y
ninguno expone esto. Esto es lo que un futuro servicio de "observabilidad" o
"plataforma" tendría que publicar, propuesto por quien mantiene el backoffice de
sistemas hoy — no pedido ni aprobado por el carril dueño de ese servicio.

### Servicios y SLO

`GET /plataforma/v1/servicios` → `EstadoServicio[]` (id, nombre, estado
operativo/degradado/caído, disponibilidad 30 días, presupuesto de error restante,
última interrupción). Candidato natural: cada servicio ya emite métricas a quien sea
que agregue SLO — este endpoint solo las expondría en un formato de lista.

### Despliegues e interruptores

`GET /plataforma/v1/despliegues` → `Despliegue[]` (servicio, versión, cuándo, quién,
resultado). `GET /plataforma/v1/interruptores` y `POST .../interruptores/:id/solicitar`
+ `.../confirmar` → el flujo de doble confirmación para los que tocan dinero necesita
persistir QUIÉN pidió el cambio del lado servidor (hoy la pantalla lo simula en el
navegador con un campo de texto, que no es una barrera real).

### Base y migraciones

`GET /plataforma/v1/migraciones` → `Migracion[]` (nombre, aplicada el, estado). Podría
derivarse de la tabla de migraciones que ya mantiene cada servicio (Flyway/Liquibase),
agregada por un colector.

### Respaldos

`GET /plataforma/v1/respaldos` → `Respaldo[]` (origen, tomado el, última restauración
probada). Depende de que el proceso de respaldo real registre cuándo se **probó** una
restauración, no solo cuándo se tomó el respaldo — hoy ese dato no se registra en
ningún lado que se haya encontrado.

### Proveedores

`GET /plataforma/v1/proveedores` → `Proveedor[]` (nombre, categoría, costo real del
último período, moneda). Cruza con facturación real de cada proveedor externo — dato
financiero, no solo operativo; quien lo publique probablemente sea contabilidad, no
plataforma.

### Outbox y descartados

`GET /plataforma/v1/outbox` y `GET /plataforma/v1/outbox/descartados` →
`MensajeOutbox[]` / `MensajeDescartado[]`. Si el outbox real vive en la base de cada
servicio (patrón transaccional outbox), este endpoint es un colector de solo lectura
sobre esas tablas — no debería escribir nada.

### Webhooks

`GET /plataforma/v1/webhooks` → `Webhook[]` (dirección entrante/saliente, origen,
último estado, última entrega). Depende de que cada integración registre sus intentos
en un lugar consultable; hoy cada proveedor (pasarela QR, SMS/WhatsApp) probablemente
solo loguea, no persiste un estado consultable.

### Accesos

`GET /plataforma/v1/accesos` → `Acceso[]` (persona, rol, ámbito, otorgado el). Este es
el más sensible: si existe, tiene que ser de solo lectura y auditado — es lista de
quién puede ver qué, no debería inventarse sin que seguridad lo revise primero.

### Incidentes

`GET /plataforma/v1/incidentes` → `Incidente[]` (título, severidad, estado, abierto
el). Podría integrarse con la herramienta de guardia que use el equipo (si hay una) en
vez de ser un sistema nuevo — no se investigó si existe una, queda fuera de este
carril.

---

**Nota:** mientras esto no exista, las nueve pantallas usan
`AdaptadorFuenteNoDisponible` (producción) o `@aportaya/simulado/backoffice-sistemas`
(demo/desarrollo, con el banner de datos de ejemplo) — nunca datos inventados sin
avisar. Ver `apps/backoffice/src/app/rutas/sistemas/dominio/puertos.ts`.
