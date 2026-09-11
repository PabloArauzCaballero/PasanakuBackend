---
ruta: /legal/estado-regulatorio
titulo: "Estado regulatorio"
bajada: "El estado real del trámite de licencia de AportaYa ante la ASFI, con la fecha de la última actualización."
descripcion: "Estado regulatorio de AportaYa: solicitud de licencia en trámite ante la ASFI (Res. ASFI/540/2025)."
indexable: true
actualizado: 2026-09-09
---

## Estado actual

**Solicitud de licencia en trámite ante la ASFI.** AportaYa no opera hoy como entidad financiera autorizada. El marco aplicable es el Reglamento para Empresas de Tecnología Financiera (Resolución ASFI/540/2025 del 3 de julio de 2025), que crea la figura de certificado de adecuación y, después, licencia de funcionamiento.

## Qué significa esto para vos

- No somos una entidad financiera regulada todavía.
- No prometemos rendimientos ni garantías propias de una entidad autorizada.
- El pasanaku que digitalizamos funciona con las mismas reglas que un pasanaku tradicional: ahorro rotativo entre un grupo, sin intermediación financiera regulada.

## Cuándo cambia esta página

Esta página se actualiza el mismo día que haya una resolución de ASFI sobre el certificado de adecuación o la licencia. Hasta entonces, el estado que ves acá es el único correcto.

> **Nota técnica de este carril (F9):** el contrato para publicar este estado con su versión y hash firmado (equivalente a `documento_publicado`) todavía no existe como endpoint público en `servicios/cumplimiento` — se buscó en `openapi/cumplimiento.yaml` y solo existe `GET /licencia/alcance`, que es interno (CU-46, exige sesión). Esta página se publica hoy como contenido estático versionado en este repositorio (Markdown con `actualizado`), no como `RenderMode.Server` contra el backend. **Pedido al carril de backend dueño de `servicios/cumplimiento`:** publicar un endpoint público de solo lectura sobre `licencia_regulatoria` (estado, número de resolución si existe, fecha) para que esta página pueda pasar a `Server` con la fuente real, como pide `planes/14`.
