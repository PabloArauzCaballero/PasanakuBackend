# Reporte — Enlace de grupo con QR que abre la app

> **AVANCE: 11 / 15 — 73,3 %.**

- Fecha: 2026-10-19 · Plan: [PLAN.md](./PLAN.md) · Ramas: `claude/enlace-de-grupo-qr-20261019` (PR #152, backend) y `claude/app-enlace-de-grupo-qr-20261019` (PR #153, app)
- Peldaño: `TESTED` (backend con pruebas contra PostgreSQL real; app con pruebas de pantalla y capturas). Falta desplegar y probar en teléfonos.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | `MiPerfil.nombreVisible` («Rosa Q.») | `gradlew :servicios:identidad:webTest` | 144/144 |
| H2.M1/M2 | CU-69 con la sección del enlace y sus criterios; tabla `enlace_grupo` y columnas en `solicitud_ingreso` | `verificar_boveda.py`, `verificar_criterios.py`, `generar_ddl.py` | TODO OK · exit 0 · «Sin pendientes» |
| H3.M1–M4 | Contrato y casos de uso: generar/anular (administrador), consultar/pedir (participante), autorización negativa | `webTest` grupos + integración en PostgreSQL | 95/95 web; 51/51 integración (`evidencia/backend-pruebas.txt`) |
| H4.M1–M4 | App: invitar con QR y WhatsApp, solicitudes, `/g/<código>`, enlaces https y QR | `flutter test …` | +759 (7 rojas solo píxeles de Windows); nuevas 14 |

## A medias
### H5.M1 — App Links
- Qué anda: `intent-filter` https con `autoVerify`, `assetlinks.json`, nginx (`nginx -t` OK con la imagen del despliegue).
- Qué no anda / falta: comprobar tras desplegar que TEST sirve `/.well-known/assetlinks.json` y que Android marca el dominio como verificado (`adb shell pm get-app-links bo.aportaya.aportaya_movil`).
- Dónde quedó: `apps/movil/android/app/src/main/AndroidManifest.xml`, `apps/movil/docker/`.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H6.M1 | TODO | CI del #152/#153, merge a `test`, despliegue |
| H6.M2 | TODO | APK v11 |
| H6.M3 | TODO | Prueba en dos teléfonos reales (organizador e invitado) |

## Evidencia
Ver `evidencia/backend-pruebas.txt`. Las pruebas con base real se corrieron con copias temporales de las clases `CU*Test` apuntando a un PostgreSQL local (no hay Docker en la máquina); las copias se borraron.

## No cubierto
- App Links verificados de punta a punta (después del despliegue).
- Con `ADMISION_HUMANA_OBLIGATORIA=true` (producción) la aprobación del organizador queda como propuesta para backoffice: la pantalla «Quién pidió unirse» mostraría el error del servidor. En TEST está en `false`.
- iOS: no hay Universal Links; el enlace abre la app web.

## Desvíos del plan
- Primera corrida contra PostgreSQL: 7 fallas reales (fecha ligada como texto en SQL plano) → `CAST(? AS timestamptz)`.
- El SQL generado divergía (se generó antes de la ficha del modelo) → regenerado.
- `consultarMiPerfil.json` del simulador estaba viejo desde #126 (`estadoDeCuenta`) → corregido.
- Formato (spotless) aplicado a código de #145 en identidad, notificaciones y auditoría: estaba rojo en `test` y frenaba los jobs con base real. Solo formato.
- La colección Postman regenerada incluye la del #148.

## Riesgos residuales
- `testBarrido` sigue rojo en `test` por dos archivos de #145 de más de 300 líneas (`CatalogosOperativosRepositorio` 570, `UsuariosInternosRepositorio` 374); y la prueba de humo `R-BIL-01`. No se tocaron: son de otro trabajo.
- `assetlinks.json` tiene la huella de la clave debug de la máquina que firma los APK de TEST; un APK firmado con otra clave abre el enlace en el navegador.
- El código del enlace solo lo tiene el teléfono del organizador que lo creó (el servidor guarda el hash): desde otro teléfono hay que crear uno nuevo, lo que anula el anterior.

## Decisiones y ambigüedades
- Del dueño (2026-10-19): el organizador aprueba; el enlace sirve hasta llenarse o 7 días.
- Supuesto: el organizador ve «primer nombre + inicial del apellido». Confirmar con el usuario.
- Sin la app instalada, el enlace abre la app web en la misma pantalla.
