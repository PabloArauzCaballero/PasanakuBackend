# Plan — Lote H: pendientes de la app móvil (CONFIG-001, RESPALDO-003, APROB-001, grupos/detalle)

- Fecha: 2026-11-25 · Repos afectados: monorepo PasanakuBackend (`apps/movil`, `.github/workflows`, `docs/`) · Predecesor: lote G (`2026-11-24-gu-g`)
- Rama: `claude/gu-h-pendientes-app` → PR contra `dev`. Pablo aprobó lo que dependía de él (2026-11-24, «Aprobado todo»): donde no hay propuesta se aplica la opción conservadora y la decisión queda escrita en el REPORTE.
- Resultado observable: (1) un `assembleRelease` sin secretos de firma falla con un mensaje claro y nunca firma con la clave debug; (2) el tour de la app no promete del fondo del grupo más de lo que dice el CU-23; (3) el mapa de errores del cliente refleja lo que el servidor emite hoy; (4) grupos/detalle resuelto según el anexo L3.
- Kill-test: `assembleRelease` sin variables termina en éxito; o el tour todavía dice «el grupo no se frena».
- Nota de descubrimiento: `docs/trabajo/2026-11-23-continuar-traspaso/{PLAN,REPORTE}.md` y `2026-11-24-gobierno-de-usuarios/TRASPASO.md` no existen en `origin/dev` ni en la rama; se trabajó con `PENDIENTES.md` y los anexos L1–L6 del plan de correcciones.

## Alcance
- IN: `apps/movil/android/app/build.gradle.kts`; `.github/workflows/apk-de-prueba.yml` (solo lectura de secretos, NO se dispara); `docs/` con los pasos de firma; `apps/movil/lib/pantallas/identidad/textos.dart` y su test; `apps/movil/lib/dominio/errores.dart` y su test; grupos/detalle según decisión.
- OUT: crear la keystore o los secretos; `hostDeLaAppWeb` obligatorio (parte de F4.L6.M22, no pedida); retiro server-side de admisión humana (F2.L3.M10, otro lote); la ilustración `arte_fondo_de_garantia.dart` (paquete `diseno_flutter`, fuera de este lote: se anota).
- Ambigüedades: ver REPORTE.

## H1 — Cuatro pendientes de la app cerrados
**CA:** Dado el estado de `dev`, cuando se aplican los cuatro ítems, entonces cada uno tiene su decisión escrita, su test y los gates móviles en verde.
**DoD:** `flutter analyze` sin issues · `flutter test` de las carpetas afectadas · `dart format` · CI del PR en verde.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Firma de release desde variables de entorno / gradle properties; sin ellas falla con mensaje claro; quitar TODO de plantilla y firma debug | `assembleRelease` sin secretos falla con el mensaje; `assembleDebug` sigue igual | gradle sin secretos → exit≠0 con el mensaje | A MEDIAS |
| H1.S1.M2 | `apk-de-prueba.yml` pasa la keystore estable por las variables nuevas (sin ejecutarlo) | el workflow sigue siendo YAML válido y lee solo secretos existentes | `python -c yaml.safe_load` | HECHO |
| H1.S1.M3 | Doc con los pasos de Pablo (keytool, nombres de secretos, workflow) | existe `docs/` con pasos exactos | lectura | HECHO |
| H1.S2.M1 | Texto del tour sobre el fondo ajustado a CU-23/CU-22, con fuente | sin cifras/coberturas sin fuente; no promete «el grupo no se frena» | test de textos en verde | HECHO |
| H1.S3.M1 | `AP-CU20-02` y `AP-CU69-10`: decidir contra el servidor real | decisión con grep del servidor; test de errores | `flutter test test/.../errores*` | HECHO |
| H1.S4.M1 | grupos/detalle según PENDIENTES/ANEXO-L3 | decisión escrita; si hay ruta, 4 estados + tests | tests + prueba visual | A MEDIAS |
| H1.S5.M1 | Gates móviles, PR, CI, aviso a la sesión principal | CI verde | salida pegada | TODO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin Android SDK local | no se puede ejecutar gradle | instalar SDK; si no se puede, verificar en CI del PR (BLOQUEADO declarado) |
| `apk-de-prueba.yml` dependía de la firma debug | romper el APK de prueba | pasar la keystore estable por las mismas variables |
