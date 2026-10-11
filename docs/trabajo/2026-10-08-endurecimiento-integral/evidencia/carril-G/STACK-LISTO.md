# Stack minimo real, levantado y reutilizable (carril G)

Fecha: 2026-10-10. Lo levanto `bash despliegue/ensayos/integracion/correr.sh dinero` con `INTEGRACION_MANTENER=1`, sobre las
imagenes `aportaya/<servicio>:test` reconstruidas en esta vuelta (con el relevo del outbox arreglado y los defectos de abajo).

| Que | Valor |
|---|---|
| Gateway (todo entra por aca) | `http://127.0.0.1:18086/api/v1` |
| PostgreSQL (admin, solo lectura para conciliar) | `127.0.0.1:5446` (PgBouncer `6446`) |
| Servicios arriba | identidad, grupos, aportes, entregas, nucleo-financiero, garantia, cumplimiento, organizador, notificaciones + gateway, Postgres, PgBouncer, Redis, Kafka, MinIO |
| Proyecto compose / contenedores | `aportaya-d3-*` (infra: `aportaya-d3-infra-*`) |
| Kafka | topicos `aportaya.*` ya creados (243); el broker NO los autocrea |
| Perfil | `local`: el segundo factor de desarrollo acepta `000000` para roles con MFA (ADMIN_PLATAFORMA, TESORERIA, RESPONSABLE_RIESGOS...) |

## Credenciales de prueba (fuera del repo)

No hay usuarios ni claves fijas en el repo. El entorno sintetico (pimienta, claves de servicio, clave de la base) vive en
`%TEMP%\aportaya-d3\entorno.env` (generado por `arranque_completo.sh`, nunca commiteado). Para tener personas con sesion:

```bash
export D3_PUERTO_PG=5446 D3_PUERTO_PGBOUNCER=6446 D3_PUERTO_GATEWAY=18086 PYTHONUTF8=1
python despliegue/ensayos/integracion/sembrar_aceptacion.py     # imprime JSON {grupoId, organizador, participante, operador, ...}
```

Cada corrida crea personas NUEVAS (telefono `+5917xxxxxxx` aleatorio, clave aleatoria con hash Argon2id, `@ejemplo.invalid`) y un grupo
con un cupo libre; la clave sale solo por stdout. Login: `POST /api/v1/sesiones` con `{telefonoE164, credencial, huellaDispositivo, plataforma}`;
si responde `AP-CU04-03` se repite con `factor: {tipo: "OTP"|"TOTP", valor: "000000"}`. Limite del gateway: 5 req/s en `/sesiones` (429).
Para dinero: `despliegue/ensayos/integracion/mundo.py` (`armar_elenco`, `sembrar_grupo`, `con_billeteras`, `fondear_por_libro`).

## Reglas para quien lo reutilice

- Un solo uso a la vez de los puertos 5446/6446/18086 (los de G). Bajar: `bash despliegue/ensayos/bajar_entorno.sh`.
- Recorridos ya verificados contra este stack: `cd despliegue/ensayos/integracion && python recorridos.py [h8m3 h8m4 h9m5 h14m1 h14m2 h14m3 reclamos desembolso]`.
- Saldo inicial sintetico de billeteras: < USD 1.000 (~6.900 BOB) por persona: por encima el libro exige declaracion UIF (`ck_operelev_declaracion`).
- Tipo de cambio: lo siembra `mundo.tipo_de_cambio_sintetico()` (serie MANUAL de `sql/61_dev`); sin el, toda transaccion del libro se rechaza (R-UIF-04).
- Sin `notificaciones` no se emiten invitaciones (grupos deniega por omision el contacto «suprimido»): esta en el stack.
- No esta: transparencia, tarifas, inversiones, erp, publicidad, auditoria.

## Estado al dejarlo

Ultima corrida completa contra este mismo stack: 121 PASS, 2 FAIL (defectos abiertos, abajo en el informe), 5 GAP declarados;
cero `fallo no previsto` en nucleo, grupos, aportes, entregas y garantia (`integracion-dinero.log` / `.json` en esta carpeta).
Libres Windows con el stack arriba: ~2,7 GB; `aceptacion.sh real` (Chromium + ng serve) NO entra bajo el guardian con el stack arriba.
