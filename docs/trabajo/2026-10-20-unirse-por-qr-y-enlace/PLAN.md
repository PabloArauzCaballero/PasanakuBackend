# Plan — Unirse a un grupo con la imagen del QR o con el enlace

- Fecha: 2026-10-20 · Repos afectados: PasanakuBackend (identidad, grupos, apps/movil) · Predecesor: `docs/trabajo/2026-10-19-enlace-de-grupo-qr/`
- Resultado observable: el organizador invita solo con dos opciones —QR (imagen que se manda a WhatsApp) y enlace—; quien recibe abre «Grupos → Unirse a un grupo», sube la imagen del QR desde su galería o pega el enlace, la app valida contra el servidor que el grupo existe y manda la solicitud; el organizador ve una tarjeta por persona con nombre, teléfono y correo y la acepta o la rechaza.
- Kill-test: crear un grupo nuevo en TEST, compartir el QR como imagen, subir esa imagen con otra cuenta en «Unirse a un grupo» y no ver la tarjeta del grupo con «Pedir unirme».

## Hallazgos (2026-10-20, TEST)
- «Crear enlace del grupo» sí creaba enlaces (5 filas en `grupos.enlace_grupo`), pero cada uno nacía inválido: `CU69EnlaceDeGrupo.vigente` exige grupo `CONFORMADO`/`ABIERTO_A_INSCRIPCION` y los dos grupos estaban en `BORRADOR`.
- Causa: faltaba el tópico `aportaya.tarifas.tarifa_congelada` (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`); `tarifas` no podía avisar a `grupos` y ningún grupo se abría. Arreglado corriendo `despliegue/host/crear-topicos.sh` (paso documentado en `despliegue/TEST.md`): 257/257 tópicos, los dos grupos pasaron a `ABIERTO_A_INSCRIPCION`.
- Ya existe: `rutaDeInvitacionDesdeQr` (`lib/navegacion/lectura_de_qr.dart`) que acepta el enlace https propio y devuelve `/g/<código>`; `PantallaUnirseAlGrupo` (`/g/:codigo`) que consulta el servidor y pide unirse; `CodigoQR` (qr_flutter); `mobile_scanner` 7.4.1 con `analyzeImage(path)`; `image_picker` 1.2.3; canal nativo `bo.aportaya/compartir`.

## Alcance
- IN: botón «Unirse a un grupo» en Grupos; pantalla para subir la imagen del QR o pegar el enlace; invitar con dos opciones (compartir QR como imagen, compartir/copiar enlace) y enlace creado al entrar; tarjeta de solicitud con nombre, teléfono y correo (backend + app).
- OUT: iOS (sin canal nativo de compartir); producción; invitar a un número (queda la ruta, sin botón en la pantalla de invitar).
- Ambigüedades registradas:
  - El organizador ve nombre completo, teléfono y correo de quien pide (pedido del dueño 2026-10-20: «su nombre y teléfono o correo»). Reemplaza el supuesto anterior «Rosa Q.». Confirmar con el dueño si el correo también debe verse.
  - Quien pide acepta compartir esos datos al tocar «Pedir unirme»: se le avisa en la pantalla.

## H1 — El organizador ve quién pide unirse con nombre, teléfono y correo
**CA:** Dado un pedido por enlace, cuando el organizador abre «Quién pidió unirse», entonces ve el nombre completo, el teléfono y el correo de esa persona.
**DoD:** `gradlew :servicios:identidad:webTest :servicios:grupos:webTest` en verde · deriva del modelo sin pendientes.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | identidad: `MiPerfil` devuelve `nombreCompleto` y `telefono` | el perfil propio trae los dos campos | `gradlew :servicios:identidad:webTest` verde | TODO |
| H1.M2 | modelo: `solicitud_ingreso.telefono_contacto`, `correo_contacto`; `nombre_visible` guarda el nombre completo | DDL regenerado, sin deriva | `python scripts/generar_ddl.py` + `verificar_boveda.py` OK | TODO |
| H1.M3 | grupos: guardar contacto al pedir por enlace y devolverlo en la lista | la lista trae `telefono` y `correo` | `gradlew :servicios:grupos:webTest` verde | TODO |

## H2 — App: invitar con QR o enlace, y unirse subiendo el QR o pegando el enlace
**CA:** Dado un grupo abierto, cuando el organizador entra a Invitar, entonces ve el QR y el enlace con «Compartir QR» y «Compartir enlace»; y cuando otra persona sube esa imagen o pega ese enlace en «Unirse a un grupo», entonces ve el grupo y puede pedir unirse.
**DoD:** `flutter analyze` sin errores nuevos · `flutter test` de las pantallas tocadas en verde · APK v12 construido.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.M1 | Botón «Unirse a un grupo» debajo de «Organizar un grupo nuevo» | el botón lleva a `/pasanaku/unirse-a-un-grupo` | test de widget | TODO |
| H2.M2 | Pantalla «Unirse a un grupo»: subir imagen del QR (galería) o pegar enlace; valida y abre `/g/<código>` | QR válido/enlace válido navegan; imagen sin QR o enlace ajeno muestran error | test de widget con doble del lector (correcto, límite, inválido) | TODO |
| H2.M3 | Invitar: enlace creado al entrar; «Compartir QR» (imagen PNG por intent) y «Compartir enlace»/«Copiar»; sin «invitar a un número» | se ven solo las dos opciones | test de widget + `flutter build apk` | TODO |
| H2.M4 | Solicitudes: tarjeta con nombre, teléfono y correo | la tarjeta muestra los tres | test de widget | TODO |
| H2.M5 | APK v12 | APK en Descargas | `flutter build apk` exit 0 | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| WhatsApp recomprime la imagen | el QR no se lee | QR con nivel de corrección M, fondo blanco y margen; probar `analyzeImage` con la misma imagen |
| Pantalla con FLAG_SECURE | no afecta: se comparte un PNG generado, no una captura | — |
| CI rojo previo en `test` (testBarrido, R-BIL-01, f5) | no es de este trabajo | se anota |
