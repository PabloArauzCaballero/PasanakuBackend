---
ruta: /contrato-de-adhesion
titulo: "Contrato de adhesión"
bajada: "Las condiciones que aceptás al abrir tu billetera, con su versión y fecha de vigencia."
descripcion: "Contrato de adhesión de la cuenta de billetera de AportaYa: condiciones vigentes, con versión y fecha."
indexable: true
actualizado: 2026-09-09
---

## Qué es este documento

El contrato de adhesión son las condiciones que aceptás al abrir tu cuenta de billetera en AportaYa: qué incluye el servicio, qué comisiones se cobran y cómo se resuelven los reclamos.

## Versiones anteriores

Cuando el contrato cambia, la versión anterior queda publicada y accesible: nunca se edita en el lugar, se sustituye por una versión nueva con su propia fecha de vigencia.

> **Nota técnica de este carril (F9):** CU-05 (Aceptar contrato de adhesión y tarifario) define `documento_publicado` con `hash_documento`, `url_publica` y `vigente_desde`, pero **no encontramos un endpoint público de lectura** para ese documento en `openapi/cumplimiento.yaml` (solo existe `POST /cumplimiento/contratos/{contratoId}/aceptaciones`, que registra una aceptación, no publica el texto). Esta página se publica hoy como marcador de posición estático, `RenderMode.Prerender`, en vez de `Server` con el texto real y su hash — sería peor mostrar un contrato con un hash inventado que no mostrar el hash. **Pedido al carril de backend dueño de `servicios/cumplimiento`:** un `GET` público sobre `documento_publicado` filtrado por `tipo='CONTRATO_ADHESION'` con el texto vigente, su hash y su fecha, para que esta página pase a `Server` como pide `planes/14`.
