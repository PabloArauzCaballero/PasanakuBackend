# Avisos a las sesiones concurrentes — 2026-10-30

Enviados con `SendMessage`, todos `success:true`:

| Sesión | msg_id | Contenido |
|---|---|---|
| pasanaku-58 | 7c1027e3-5709-4184-a87d-58b9fdb09880 | Regla del sorteo (≥ 75 % y primera cuota) |
| pasanaku-ab | fb597cbe-5880-44b5-b464-53f543849236 | ídem |
| pasanaku-c1 | 31188ebe-8af2-4a5e-9a99-be4e087888a0 | ídem |
| pasanaku-20 | 86520e14-0ff0-4e92-9ba9-eea9d34a3e63 | ídem |
| pasanaku-c1 | 9a6d76c2-d9bf-49ab-bd3c-b1004f9e5ec8 | Respuesta: adopto su definición (inicio al 75 % achica el grupo); AP-CU60-01 intacto; única precondición nueva = primer aporte; ambigüedad de dónde nace la primera obligación |

Respuesta recibida de `pasanaku-c1`: el 75 % está en su alcance (`docs/trabajo/2026-10-30-arenas-e-inicio-75/PLAN.md`,
rama `claude/arenas-y-inicio-75-20261030`); decisión de Pablo tomada hoy: el organizador inicia con
≥ `ceil(0,75 × cupos)` ocupados y el grupo se achica a los ocupados; su carril C edita
`pantalla_del_grupo.dart` / `seccion_del_grupo.dart`; no toca `CU60Sortear.java` ni `puedeSortear`.
