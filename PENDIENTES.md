# Pendientes — alta Atlas y sesión premium

Actualizado: 2026-10-14. Rama de trabajo: `codex/alta-atlas-sesion-premium-2026-10-11`.

## Estado

- Avance oficial del plan: **197/213 tareas HECHO (92,5 %)**; 7 `A MEDIAS`, 9 `BLOQUEADO`.
- Plan y evidencias: `docs/trabajo/2026-10-11-alta-atlas-sesion-premium/PLAN.md`.
- Aprobación de cuenta: sigue siendo manual. La cuenta `PENDIENTE_VERIFICACION` puede iniciar sesión bajo las condiciones documentadas; no se aprueba ni se confía el dispositivo automáticamente.
- Pruebas móviles: continuar con AVD/emulador. No usar teléfono físico. Ensayos que requieran rostros reales quedan pendientes.

## Tareas pendientes

### A medias (7)

| ID | Pendiente para cerrar |
|---|---|
| `H1.S4.M1` | La suite actual pasa 14/14, pero no se conservó el rojo previo de la prueba primero; completar evidencia TDD según `evidencia/h1-s4-m1-vigilante.md`. |
| `H2.S1.M1` | Recorrido completo de alta/login/errores/expiración contra backend acordado, con tabla de error real y capturas. El login de cuenta pendiente y el caso de contraseña inválida tienen evidencia parcial; falta recorrido Flutter completo y entorno/backend disponible. |
| `H4.S5.M1` | Completar/verificar la prueba primero de estructura del alta y conservar salida roja previa; ver `evidencia/h4-s5-m1-verificacion-2026-10-13.md`. |
| `H6.S3.M8` | El perfil AVD midió cuadros por encima de 16 ms (20/135 en la última corrida). Perfilar/optimizar y cumplir umbral en entorno representativo; la medición AVD no basta para cerrar. |
| `H10.S1.M6` | Resolver diferencias de goldens: Windows Flutter 3.44.8 dio 79/268; falta cotejo canónico macOS y revisión, sin regenerar baselines a ciegas. |
| `H10.S1.M7` | Flutter 3.47.5 Windows pasó 132/132; 3.44.8 pasó 124/132. Validar en runner canónico macOS/3.44.8; no actualizar referencias hasta confirmar. |
| `H10.S2.M1` | AVD verificó que sin sesión se muestra login ante deep-link/intent Android. Falta validar push real con backend/FCM; esta tarea no requiere teléfono físico. |

### Bloqueadas (9)

| ID | Bloqueo y cómo reanudar |
|---|---|
| `H3.S3.M3` | Capturas con teclado abierto en tres selects; no existe select de soporte inventariado. Seguir con AVD para los selects existentes y aclarar/implementar el de soporte solo si corresponde al producto. |
| `H6.S2.M3` | Revisión de umbrales requiere 10 altas con rostros/personas reales. Mantener pendiente hasta disponer de participantes y consentimiento; no sustituirlos por imágenes sintéticas. |
| `H8.S4.M4` | “Grupos/detalle” no tiene pantalla/ruta/contrato local; decidir alcance de producto antes de completar la inspección oscura de esa superficie. Las 20 superficies existentes sí están inspeccionadas. |
| `H10.S2.M2` | Recorrido de alta integral depende de backend/storage y cuenta de prueba. Ejecutar en AVD cuando estén disponibles; las pruebas con rostros reales siguen pendientes. |
| `H10.S2.M3` | Recorrido de sesión durante 20 minutos depende de backend y cuenta de ensayo; hacer en AVD cuando estén disponibles. |
| `H10.S2.M4` | Flujo de candado de sesión depende de backend/sesión de ensayo; ejecutar en AVD cuando estén disponibles. |
| `H10.S2.M5` | Cierre y cambio limpio de cuenta requiere backend y dos cuentas de ensayo; ejecutar en AVD cuando estén disponibles. |
| `H10.S2.M6` | Selectores con teclado requieren completar el recorrido de alta en AVD con IME Android y datos de backend. |
| `H10.S3.M1` | Falta la matriz visual de grupos/detalle porque esa pantalla no tiene ruta/contrato local; resolver alcance antes de producir capturas. Las 228 celdas de 20 superficies existentes ya se inspeccionaron. |

## Errores y riesgos conocidos

- CI canónico GitHub Actions, corrida `38627145597` (SHA `cc833a1234ce645d1d12c4f499d5b3d1ba3f4caa`), terminó `failure`: fallaron prerrequisitos (deriva de 4 colecciones Postman, Gitleaks reportó 25 hallazgos `generic-api-key` en fixtures/documentación aún por adjudicar, verificadores de bóveda/base, formato y reporter OSV). Jobs móviles/macOS quedaron omitidos. No copiar secretos a logs ni aprobar estas detecciones sin revisar contexto.
- Goldens: resultados dependen del SDK/plataforma; conservar baselines y comparar en el entorno canónico antes de cambios.
- El backend local necesita PostgreSQL/Kafka/almacenamiento y secretos de ensayo. MinIO no estaba disponible (Quay 401 / Hub no obtuvo imagen); no reemplazar por una imagen no aprobada.
- La suite completa CU04 sobre DB persistente tuvo fallas por colisión de teléfonos semilla (`uq_usuario_telefono_e164`); la regresión dirigida reciente pasó 2/2 con teléfono aleatorio. No limpiar ni borrar la DB/volúmenes para “arreglarla”.
- Si se levantan Docker, AVD o servicios Java para pruebas, vigilar RAM y apagarlos al terminar (límite indicado: 14,9 GB usados). AVD/ADB y Docker estaban apagados al guardar el progreso anterior; verificar antes de continuar.
- PR #61 (`codex/...` → `dev`) ya existe y estaba marcado `CONFLICTING`; PR #60 (`dev` → `test`) también reportaba conflicto. Actualizar y revisar ambos estados; no hacer merge forzado.

## Instrucciones exactas para retomar

1. Abrir PowerShell y ejecutar:

   ```powershell
   Set-Location 'C:\Users\leonardo\Repositories\pasanaku\PasanakuBackend'
   git status --short
   git branch --show-current
   Get-CimInstance Win32_OperatingSystem | Select-Object @{N='LibreGB';E={[math]::Round($_.FreePhysicalMemory/1MB,2)}}, @{N='TotalGB';E={[math]::Round($_.TotalVisibleMemorySize/1MB,2)}}
   ```

2. Confirmar que la rama es `codex/alta-atlas-sesion-premium-2026-10-11`, revisar `PROGRESO.md` y `git status`; preservar cambios existentes. No ejecutar `clean`, `reset`, `checkout --`, ni borrar datos/volúmenes.
3. Consultar el plan vigente:

   ```powershell
   python .claude/hooks/plan_status.py --path docs/trabajo/2026-10-11-alta-atlas-sesion-premium/PLAN.md
   ```

4. Antes de pruebas de backend, verificar RAM y estado de Docker. Usar un solo runner Flutter/Dart a la vez, AVD en lugar de teléfono, y apagar emulador/ADB/servicios temporales al acabar.
5. Atacar primero los `A MEDIAS` que no dependen de participantes reales ni definición de producto; documentar cada bloqueo sin inventar resultados. Al final volver a ejecutar el contador y actualizar `PROGRESO.md`/evidencias.
6. Revisar PR #61 contra `dev` y el PR que se abra hacia `test`; corregir conflictos de forma normal y esperar CI. No marcar el trabajo terminado hasta que CI y los pendientes restantes estén resueltos o aceptados explícitamente.

## Enlaces locales

- Progreso de reanudación: `PROGRESO.md`.
- Plan detallado: `docs/trabajo/2026-10-11-alta-atlas-sesion-premium/PLAN.md`.
- Evidencias: `docs/trabajo/2026-10-11-alta-atlas-sesion-premium/evidencia/`.
