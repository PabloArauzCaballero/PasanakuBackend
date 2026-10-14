# Verificación de la integración de origin/dev (carril M, quinta vuelta)

Rama: `justin/merge/dev-en-cierre` (desde `origin/justin/feature/cierre-endurecimiento-2026-10-08` 334f2e44), merge de `origin/dev` 7b467d8e.
Base común: 59370bbd (origin/test). Puerto Postgres desechable 5449. Todo por `heavy.sh` y de a uno.

| Qué | Comando | Resultado |
|---|---|---|
| Esquema | `PYTHONUTF8=1 python scripts/generar_ddl.py` + `generar_semillas.py` | 337 tablas, 1500 filas; sin diff contra lo fusionado |
| Compose | `generar_compose.py` y `--coolify` | solo cambia `inversiones` (límites y pool); `verificar_limites_compose.py` OK |
| Clientes | `./gradlew generarClientes` | OK (Angular y Dart de los 15 servicios) |
| Compilación | `compileJava compileTestJava` de todo el repo | OK tras reparar `BaseDeCU01` |
| identidad | `test` 20 · `webTest` 96 · `integrationTest` 135 · `testBarrido` 2 · contract/saga sin pruebas | 253 pruebas, 0 fallas, 0 omitidas (XML) |
| spotless | `spotlessApply` / `spotlessCheck` identidad | OK |
| Postman | `generar_postman.py` | regenerado |
| Simulado | `tsc`, `eslint`, build de ejemplos, `vitest` | 261/261 |
| ui | `tsc`, `ng lint`, `test:front` 125, `test:a11y` 9 | verde |
| backoffice | `tsc` app y spec, `ng lint` | verde (tras retirar el doble `identidad.ts`) |
| web | `tsc`, `ng lint` | verde |
