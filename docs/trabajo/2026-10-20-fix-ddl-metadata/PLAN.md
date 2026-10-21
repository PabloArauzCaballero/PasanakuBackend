# Plan — Las semillas regeneradas no borran los comentarios de metadata

- Fecha: 2026-10-20 · Repos afectados: PasanakuBackend · Predecesor: commit 2489cacf (agregó `24-metadata-comentarios.sql`)
- Resultado observable: el job «Bóveda, seguridad, carriles y despliegue coherentes» deja de fallar en «Lo derivado no puede divergir de su fuente».
- Kill-test: correr `generar_ddl.py` y `generar_semillas.py` (en ese orden, como el CI) y mirar `git diff -- sql/`; si hay diferencias, no está hecho.

## Alcance
- IN: `scripts/generar_semillas.py` (llamar a `inyectar_metadata.generar()` después de regenerar las semillas).
- OUT: el contenido de la metadata, `inyectar_metadata.py`, el resto de los checks de CI que fallan en `dev` («Formato…», «Frontend…»).
- Ambigüedades registradas: ninguna.

## Hallazgos
- `generar_semillas.procesar()` hace `shutil.rmtree(sql/60_semillas)` y regenera desde `seeders/`.
- `sql/60_semillas/24-metadata-comentarios.sql` y su `\ir` en `sembrar.sql` los escribe `scripts/inyectar_metadata.py`, que ninguna corrida del CI ejecuta → el CI los borra y marca divergencia (4165 líneas).
- Correr `inyectar_metadata.py` después de los generadores reproduce byte a byte lo commiteado.

## H1 — `sql/` regenerado sin divergencia
**CA:** Dado `dev` limpio, cuando se corren los pasos del CI, entonces `git diff -- sql/` queda vacío.
**DoD:** secuencia del job de CI en local, todos exit 0, y `git diff --quiet -- sql/`.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `generar_semillas.main()` llama a `inyectar_metadata.generar()` al final | `sql/` sin diferencias tras `generar_ddl.py` + `generar_semillas.py` | `git diff --quiet -- sql/` → «SIN DIVERGENCIA» | HECHO |
| H1.S1.M2 | Segunda corrida idempotente | segunda corrida no cambia nada | `git diff --quiet -- sql/ scripts/metadata_enriquecida.json` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| `inyectar_metadata` deja de reproducir lo commiteado | CI vuelve a fallar | el mismo gate de CI lo detecta |
