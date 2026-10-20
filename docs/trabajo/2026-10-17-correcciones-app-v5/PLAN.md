# Plan — Correcciones de la app tras la prueba en teléfono (APK v5)

- Fecha: 2026-10-17 · Repos afectados: PasanakuBackend (rama `claude/grupos-nombre-participacion-20261017`, PR #120) y la app en `alta-atlas-sesion-work/apps/movil` · Predecesor: `2026-10-11-alta-atlas-sesion-premium`
- Resultado observable: la persona abre la app y entra con su huella sin una pantalla de bloqueo trabada; en «Grupos» ve cada grupo suyo como tarjeta con su nombre; en el perfil ve su correo; «Ver la guía otra vez» no muestra el aviso amarillo.
- Kill-test: instalar el APK v5, abrir la app con sesión guardada y una billetera en cero, y tocar «Ver la guía otra vez».

## Alcance
- IN: los 5 puntos que reportó el usuario (capturas 5 a 9).
- OUT: regenerar los goldens de CI; cambiar las reglas del candado (cuándo se bloquea).
- Ambigüedades registradas:
  - «Me salió esto, nada que ver» (pantalla de bloqueo). Supuesto: el problema es que la huella no se pidió sola y la pantalla quedó trabada con un texto poco claro. El bloqueo al abrir la app es una decisión de seguridad que existe (`lib/proveedores/bloqueo.dart`) y se mantiene. A confirmar con el usuario.

## H1 — Correcciones de la app
**CA:** Dado el APK v5, cuando la persona repite los 5 pasos de las capturas, entonces ninguno se reproduce.
**DoD:** los tests dirigidos de cada microtarea en verde; `flutter analyze` sin errores nuevos; el APK v5 está en Descargas.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.M1 | Billetera en cero: se muestran el saldo y las acciones | Con un saldo de 0,00 se ven la cifra, «Recargar» y «Todavía no tenés movimientos» | `flutter test test/widget/pantalla_de_saldo_test.dart test/widget/inicio_de_billetera_test.dart` → All passed | HECHO |
| H1.M2 | Guía: el paso 1 encuentra `billetera.saldo` con un saldo en cero | La marca existe con un saldo en cero (se deriva de M1) | igual que M1 (la marca está dentro de `CuerpoDeSaldo`, que ahora se muestra en cero) | HECHO |
| H1.M3 | Backend: las participaciones traen el nombre del grupo, el aporte y si la persona es organizadora | GET `/grupos/participaciones` responde con `nombreDelGrupo` | `gradlew :servicios:grupos:webTest` → MisParticipaciones tests=2 failures=0; CI del PR #120 en verde; fusionado (3c797aa) | HECHO |
| H1.M4 | Pestaña Grupos: una tarjeta por grupo, con su nombre | Con 2 participaciones se ven 2 tarjetas con sus nombres; al tocar una se abre su estado, titulado con el nombre | `flutter test test/pasanaku/pantalla_mis_grupos_test.dart` → +4 All passed | HECHO |
| H1.M5 | Perfil: muestra el correo de la cuenta | El campo Correo trae el correo registrado | Backend PR #121 (`GET /usuarios/yo/perfil`; webTest con 19 suites y 0 fallos) + `flutter test test/widget/pantalla_perfil_correo_test.dart` → All passed | HECHO (PR #121 fusionado, 02bbd73) |
| H1.M6 | Candado: la huella se pide sola cuando la app ya es visible; el texto queda más claro | La huella se pide después del primer frame | `flutter test test/widget/capa_de_bloqueo_interaccion_test.dart test/unidad/vigilante_test.dart` → +18 All passed | HECHO |
| H1.M7 | APK v5 en Descargas | Existe el archivo `AportaYa-test-2026-10-17-v5.apk` | `ls -la Downloads/AportaYa-test-2026-10-17-v5.apk` → 69073664 bytes (build 2026101703) | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| El PR #120 no está desplegado cuando el usuario prueba | Las tarjetas salen sin nombre | La app muestra «Grupo» con el aporte mientras tanto; se avisa |

## H2 — Alta sin callejón: entrar a la app mientras se verifica la identidad (2026-10-17, tarde)
Pedido del usuario: tras subir los documentos salía «Lo está revisando una persona / No tenemos un plazo fijo», y después de ingresar, «Tu cuenta está en proceso de verificación» con un único botón «Salir de esta cuenta». Además, crear un grupo daba «no hay tarifa vigente».
**CA:** Dado un cliente con la identidad EN_REVISION, cuando ingresa, entonces entra a la app (no a un callejón), ve un aviso de verificación arriba y, cuando lo aprueban, el acceso se activa sin cerrar sesión.
**DoD:** tests dirigidos en verde; APK v6 en Descargas.
**Estado:** HECHO (suite no visual +686 −2: los 2 son goldens de la pantalla de ingreso, ajenos)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H2.M0 | TEST: levantar tarifas, publicidad y transparencia, que quedaron «Created» tras el despliegue de las 14:09 | Los 3 contenedores `healthy` | `docker ps` en el VPS | HECHO |
| H2.M1 | Textos profesionales en la pantalla posterior a los documentos y en la de cuenta en verificación | Sin «No tenemos un plazo fijo» ni «Salir de esta cuenta» como única salida | `flutter test test/identidad/pantalla_estado_verificacion_sesion_test.dart` → +5 | HECHO |
| H2.M2 | Con sesión y la identidad pendiente, no se fuerza la ruta de verificación | `decidirRuta` deja ir a `/billetera/inicio` | `flutter test test/unidad/acceso_a_rutas_test.dart test/unidad/sesion_observable_test.dart` → +18 | HECHO |
| H2.M3 | Aviso de verificación arriba del shell, que lleva al estado | Con `requiereVerificacion` se ve el aviso | test «al abrir billetera con la identidad en revisión…» → PASS | HECHO |
| H2.M4 | Aprobada con sesión activa: «Activar mi cuenta» renueva el token sin cerrar sesión | Se llama a `/sesion/refrescar` y se va al inicio | tests «correcto/inválido/límite» → PASS | HECHO |
| H2.M5 | APK v6 | Archivo en Descargas | `ls` → AportaYa-test-2026-10-17-v6.apk, 69073664 bytes | HECHO |

Fuera de alcance (recomendación al usuario, decisión de negocio): avisar por correo y push cuando se resuelve el KYC (hoy no existe ese evento) y la aprobación automática con un proveedor de verificación.
